// package com.eb.ui.tabs;  // adjust to your project
package com.eb.ui.ebs;

import com.eb.ui.tabs.*;
import com.eb.ui.util.ButtonShortcutHelper;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.builtins.Builtins;
import com.eb.script.interpreter.builtins.BuiltinsFile;
import com.eb.script.file.FileData;
import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import com.eb.ui.cli.AutocompletePopup;
import com.eb.ui.cli.AutocompleteSuggestions;
import com.eb.ui.cli.Handler;
import com.eb.ui.cli.JsonSchemaAutocomplete;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Debugger;
import com.eb.util.Util;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

public class EbsTab extends Tab {

    protected static Charset defaultCharset = StandardCharsets.UTF_8;
    protected final RuntimeContext context;
    protected final TabContext tabContext;
    protected final Handler handler;
    protected final ScriptArea outputArea = new ScriptArea();
    protected final ScriptArea dispArea = new ScriptArea();
    protected String filename;
    protected String ext;
    // --- Dirty tracking ---
    private String baseTitle;        // the clean (no-star) title, usually file name
    private boolean dirty = false;
    private boolean suppressDirty = false; // avoid marking dirty during programmatic loads

    private HBox findBar;
    private ComboBox<String> findField;
    private TextField replaceField;
    private CheckBox chkCase, chkWord, chkRegex;
    private Button btnNext, btnPrev, btnReplace, btnReplaceAll, btnClose;
    private Label lblCount;
    
    // Search history for find field (max 10 items)
    private static final int MAX_SEARCH_HISTORY = 10;
    private final java.util.List<String> searchHistory = new java.util.ArrayList<>();

    private List<int[]> lastMatches = java.util.Collections.emptyList(); // each int[]{start,endExclusive}
    private List<int[]> stalePendingClear = java.util.Collections.emptyList(); // old matches pending clear after text change
    private int currentIndex = -1;
    private boolean suppressFindSearch = false; // avoid automatic search when programmatically setting find field
    private boolean dropdownOpen = false; // tracks if dropdown is currently open
    private int searchOriginPosition = -1; // position where the search was initiated (before any matches selected)
    
    // Minimum character count for find highlighting (at least 2)
    private static final int MIN_FIND_CHARS = 2;
    
    // Timer for debounced editor change re-highlighting (1 second)
    private PauseTransition editorChangeTimer;
    // Flag to indicate if highlights are stale due to editor changes
    private boolean highlightsStale = false;
    
    // autocomplete
    private final AutocompletePopup autocompletePopup;

    public EbsTab(TabContext tabContext) throws IOException {
        this.tabContext = tabContext;
        this.context = new RuntimeContext(tabContext.name, tabContext.path);
        this.handler = new EbsHandler(context);
        this.autocompletePopup = new AutocompletePopup();
        handler.setUI_outputArea(outputArea);
        context.environment.setEcho(false);
        Debugger debug = context.environment.getDebugger();
        debug.setOutputArea(outputArea);
        filename = tabContext.path.getFileName() != null ? tabContext.path.getFileName().toString() : tabContext.path.toString();
        ext = filename.substring(filename.lastIndexOf('.'));
        loadFile(tabContext);
        tabUI();
        
        // Clear undo history after file load and initial syntax highlighting
        // Use Platform.runLater to ensure this runs after any pending style updates
        Platform.runLater(() -> {
            dispArea.getUndoManager().forgetHistory();
        });
        
        // Make outputArea not editable
        outputArea.setEditable(false);

        // After you know the tabContext/path/filename:
        this.baseTitle = (tabContext != null && tabContext.name != null)
                ? tabContext.name : (filename != null ? filename : "untitled");

        setText(baseTitle); // show clean name initially
        setTooltip(new Tooltip(tabContext.path.toString())); // you already do this later; keep it in one place if you prefer

        // When you load the text into dispArea, guard marking dirty
        suppressDirty = true;
        suppressDirty = false;

        // Mark as dirty when user edits (simple listener works fine)
        dispArea.textProperty().addListener((obs, oldV, newV) -> {
            if (!suppressDirty) {
                markDirty();
            }
        });

        dispArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // First check if autocomplete popup wants to handle the event
            if (autocompletePopup.handleKeyEvent(e)) {
                return;
            }
            
            if (e.isControlDown() && e.getCode() == KeyCode.D) {
                // Toggle debug mode for this thread
                com.eb.script.interpreter.screen.ScreenFactory.toggleDebugModeForThread(outputArea);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.L) {
                dispArea.toggleLineNumbers();   // <— turns line numbers on/off
                e.consume();                    // prevent further handling of the keystroke
            } else if (e.getCode() == KeyCode.SPACE && e.isControlDown()) {
                // Autocomplete: Ctrl+Space
                showAutocomplete();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.DELETE) {
                // Ctrl+Delete: delete spaces onward if on space, delete word if on text
                handleCtrlDelete(dispArea);
                e.consume();
            } else if (e.getCode() == KeyCode.TAB && e.isShiftDown() && !e.isControlDown()) {
                // Shift+Tab: unindent/outdent selected lines
                handleShiftTabUnindent(dispArea);
                e.consume();
            } else if (e.getCode() == KeyCode.TAB && !e.isControlDown() && !e.isShiftDown()) {
                // Tab: indent multiple lines if selected, normal behavior for single line
                if (handleTabIndent(dispArea)) {
                    e.consume();
                }
            } else if (e.isControlDown() && e.getCode() == KeyCode.G) {
                // Ctrl+G: go to line number
                handleGoToLine(dispArea);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.SLASH) {
                // Ctrl+/: toggle line comments
                handleToggleLineComments(dispArea);
                e.consume();
            } else if (e.isAltDown() && e.getCode() == KeyCode.UP) {
                // Alt+Up: move line(s) up
                handleMoveLineUp(dispArea);
                e.consume();
            } else if (e.isAltDown() && e.getCode() == KeyCode.DOWN) {
                // Alt+Down: move line(s) down
                handleMoveLineDown(dispArea);
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER && !e.isControlDown() && !e.isShiftDown()) {
                // Enter: auto-indent to match the previous line's indentation
                handleAutoIndent(dispArea);
                e.consume();
            }
        });
        
        // Update autocomplete as user types (like in Console)
        dispArea.addEventHandler(KeyEvent.KEY_TYPED, e -> {
            if (autocompletePopup.isShowing()) {
                // Don't hide popup when typing dot (.)
                if (!".".equals(e.getCharacter())) {
                    // Update the suggestions based on the new text
                    showAutocomplete();
                }
            }
        });
        
        // Hide autocomplete when clicking in the editor (JavaFX Popup doesn't auto-hide on owner clicks)
        dispArea.setOnMouseClicked(e -> {
            if (autocompletePopup.isShowing()) {
                autocompletePopup.hide();
            }
        });
        
        // Setup autocomplete callback
        setupAutocomplete();
        
        // Add cursor position tracking for dispArea
        dispArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            updateCursorPosition(dispArea);
        });
        
        // Also update on focus (when switching to this tab)
        dispArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                updateCursorPosition(dispArea);
            }
        });

        outputArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.D) {
                // Toggle debug mode for this thread
                com.eb.script.interpreter.screen.ScreenFactory.toggleDebugModeForThread(outputArea);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.L) {
                outputArea.toggleLineNumbers();   // <— turns line numbers on/off
                e.consume();                    // prevent further handling of the keystroke
            } else if (e.isControlDown() && e.getCode() == KeyCode.F) {
                // Transfer focus to editor and show find bar
                Platform.runLater(() -> {
                    dispArea.requestFocus();
                    if (findBar.isVisible()) {
                        hideFind();                  // close if open
                    } else {
                        showFind(false);             // open in Find-only mode
                    }
                });
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.H) {
                // Transfer focus to editor and show find/replace bar
                Platform.runLater(() -> {
                    dispArea.requestFocus();
                    showFind(true);
                });
                e.consume();
            }
        });
    }

    private void loadFile(TabContext tabContext) {
        try {
            // Check if this is a new file that doesn't exist yet
            if (!Files.exists(tabContext.path)) {
                // For new files, don't try to read - we'll set content later via initializeAsNewFile
                suppressDirty = true;
                dispArea.replaceText("");
                outputArea.printlnInfo("New file: " + tabContext.path.getFileName());
                suppressDirty = false;
                // Note: Don't clear undo history yet - will be done after syntax highlighting is applied
                return;
            }
            
            FileData ret = BuiltinsFile.readTextFile(tabContext.path.toString());
            tabContext.fileContext = ret.fileContext;
            suppressDirty = true;
            dispArea.replaceText(ret.stringData);
            outputArea.printlnOk(ret.fileContext.path.toString() + " : " + ret.fileContext.size);
            suppressDirty = false;
            // Note: Don't clear undo history yet - will be done after syntax highlighting is applied
        } catch (Exception ex) {
            outputArea.printlnError("load error:" + ex.getMessage());
        }
    }
    
    /**
     * Setup autocomplete callback for when user selects a suggestion.
     */
    private void setupAutocomplete() {
        autocompletePopup.setOnSelect(suggestion -> {
            insertSuggestion(suggestion);
        });
    }
    
    /**
     * Show the autocomplete popup with context-aware suggestions.
     */
    private void showAutocomplete() {
        String text = dispArea.getText();
        int caretPos = dispArea.getCaretPosition();
        
        // For JSON autocomplete, use the full text to maintain context
        // For other cases, just use current line (same as Console)
        String contextText;
        int contextCaretPos;
        
        if (JsonSchemaAutocomplete.looksLikeJson(text, caretPos)) {
            // Use full text for JSON to maintain proper context
            contextText = text;
            contextCaretPos = caretPos;
        } else {
            // For non-JSON, use current line only
            contextText = getCurrentLineText(text, caretPos);
            contextCaretPos = getCaretPositionInCurrentLine(text, caretPos);
        }
        
        List<String> suggestions = AutocompleteSuggestions.getSuggestionsForContext(contextText, contextCaretPos);
        
        if (!suggestions.isEmpty()) {
            autocompletePopup.show(dispArea, suggestions);
        } else {
            // Hide popup if no suggestions available
            autocompletePopup.hide();
        }
    }
    
    /**
     * Get the text of the current line where the caret is positioned.
     */
    private String getCurrentLineText(String text, int caretPos) {
        // Find the start of the current line (last newline before caret)
        int lineStart = text.lastIndexOf('\n', caretPos - 1);
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;
        
        // Find the end of the current line (next newline after caret)
        int lineEnd = text.indexOf('\n', caretPos);
        lineEnd = (lineEnd == -1) ? text.length() : lineEnd;
        
        return text.substring(lineStart, lineEnd);
    }
    
    /**
     * Get the caret position relative to the start of the current line.
     */
    private int getCaretPositionInCurrentLine(String text, int caretPos) {
        // Find the start of the current line
        int lineStart = text.lastIndexOf('\n', caretPos - 1);
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;
        
        return caretPos - lineStart;
    }
    
    /**
     * Insert the selected suggestion at the current cursor position.
     * Replaces the current word being typed with the suggestion.
     */
    private void insertSuggestion(String suggestion) {
        int caretPos = dispArea.getCaretPosition();
        String text = dispArea.getText();
        
        // Find the start of the current word
        int start = caretPos;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '"' || c == '#') {
                start--;
            } else {
                break;
            }
        }
        
        // Check if we're inserting a JSON property key (which needs quotes)
        boolean isJsonKey = JsonSchemaAutocomplete.isSuggestingJsonKeys(text, caretPos);
        
        // Check if the suggestion is a builtin function that needs parameters
        String toInsert = suggestion;
        int finalCaretOffset = suggestion.length();
        
        if (isJsonKey) {
            // For JSON property keys, add quotes if not already present
            boolean hasOpeningQuote = start > 0 && text.charAt(start - 1) == '"';
            boolean hasClosingQuote = caretPos < text.length() && text.charAt(caretPos) == '"';
            
            if (!hasOpeningQuote) {
                toInsert = "\"" + toInsert;
                finalCaretOffset++;
            }
            if (!hasClosingQuote) {
                toInsert = toInsert + "\"";
            } else {
                // If there's already a closing quote, we'll position caret after it
                finalCaretOffset++;
            }
        } else {
            String paramSignature = AutocompleteSuggestions.getBuiltinParameterSignature(suggestion);
            if (paramSignature != null) {
                toInsert = suggestion + paramSignature;
                // Position cursor inside the parentheses after first = sign
                int firstEquals = paramSignature.indexOf("=");
                if (firstEquals >= 0) {
                    finalCaretOffset = suggestion.length() + firstEquals + 1;
                } else {
                    // Move cursor to after the opening paren
                    finalCaretOffset = suggestion.length() + 1;
                }
            }
        }
        
        // Replace the current word with the suggestion
        dispArea.replaceText(start, caretPos, toInsert);
        
        // Move cursor to appropriate position
        dispArea.moveTo(start + finalCaretOffset);
        
        // Request focus back to the editor
        dispArea.requestFocus();
    }
    
//    private void loadFile(Path path) throws IOException {
//        filename = path.getFileName() != null ? path.getFileName().toString() : path.toString();
//        ext = filename.substring(filename.lastIndexOf('.'));
//        long size = Files.size(path);
//        final long LIMIT = 10 * 1024 * 1024; // 10 MB UI limit
//        byte[] bytes;
//        if (size > LIMIT) {
//            bytes = Files.readAllBytes(path.subpath(0, path.getNameCount())); // placeholder; 
//        } else {
//            bytes = Files.readAllBytes(path);
//        }
//
//        boolean binary = looksBinary(bytes);
//        if (!binary) {
//            Charset use = defaultCharset;
//            text = new String(bytes, use);
//        } else {
//            // Hex/preview for binary
//            StringBuilder sb = new StringBuilder();
//            int col = 0;
//            for (int i = 0; i < Math.min(bytes.length, 1_000_000); i++) {
//                sb.append(String.format("%02X ", bytes[i]));
//                if (++col == 16) {
//                    sb.append('\n');
//                    col = 0;
//                }
//            }
//            if (bytes.length > 1_000_000) {
//                sb.append("\n... (truncated) ...\n");
//            }
//            text = sb.toString();
//        }
//
//    }

    private void tabUI() throws IOException {
        // Decide if we treat as text or binary:

        boolean isEbs = ext.equalsIgnoreCase(".ebs");
        boolean isJson = ext.equalsIgnoreCase(".json");
        boolean isCss = ext.equalsIgnoreCase(".css");
        boolean isHtml = ext.equalsIgnoreCase(".html");
        boolean isMd = ext.equalsIgnoreCase(".md");
        boolean isEditable = isEbs || isJson || isCss || isHtml || isMd;

        if (isEditable) {
            dispArea.setEditable(true);               // enable edits for textual content
            dispArea.getStyleClass().add("editor-ebs");
            
            // Choose appropriate highlighting based on file type
            if (isCss) {
                setupCssHighlighting();
            } else if (isHtml) {
                setupHtmlHighlighting();
            } else if (isJson) {
                setupJsonHighlighting();
            } else if (isMd) {
                setupMdHighlighting();
            } else if (isEbs) {
                setupEbsSyntaxHighlighting();         // Use custom function highlighting for .ebs files
            } else {
                setupLexerHighlighting();             // Use basic lexer for other text files
            }
        } else {
            dispArea.getStyleClass().add("editor-text");
            setupLexerHighlighting();             // optional: you can highlight non-.ebs tokens too
            // binary preview remains non-editable (you already build 'text' hex preview in loadFile)
            dispArea.setEditable(false);
            dispArea.getStyleClass().add("editor-binary");
        }
        outputArea.getStyleClass().add("editor-ebs");

        var dispAreaScroller = new VirtualizedScrollPane<>(dispArea);
        dispAreaScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dispAreaScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        StackPane dispAreaFrame = new StackPane(dispAreaScroller);
        dispAreaFrame.getStyleClass().addAll("bevel-lowered");
        dispArea.setPadding(Insets.EMPTY);
        dispAreaScroller.setPadding(Insets.EMPTY);
        dispAreaFrame.setPadding(Insets.EMPTY);

        var outputAreaScroller = new VirtualizedScrollPane<>(outputArea);
        outputAreaScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outputAreaScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        StackPane outputAreaFrame = new StackPane(outputAreaScroller);
        outputAreaFrame.getStyleClass().addAll("bevel-lowered");
        outputArea.setPadding(Insets.EMPTY);
        outputAreaScroller.setPadding(Insets.EMPTY);
        outputAreaFrame.setPadding(Insets.EMPTY);

        findField = new ComboBox<>();
        findField.setEditable(true);
        findField.setPromptText("Find");
        findField.setPrefWidth(200);
        replaceField = new TextField();
        replaceField.setPromptText("Replace");

        chkCase = new CheckBox("Aa");
        chkWord = new CheckBox("Word");
        chkRegex = new CheckBox("Regex");

        btnPrev = new Button("Prev");
        ButtonShortcutHelper.addAltShortcut(btnPrev, KeyCode.P);
        btnNext = new Button("Next");
        ButtonShortcutHelper.addAltShortcut(btnNext, KeyCode.N);
        btnReplace = new Button("Replace");
        ButtonShortcutHelper.addAltShortcut(btnReplace, KeyCode.R);
        btnReplaceAll = new Button("Replace All");
        ButtonShortcutHelper.addAltShortcut(btnReplaceAll, KeyCode.A);
        btnClose = new Button("Close");
        ButtonShortcutHelper.addAltShortcut(btnClose, KeyCode.C);
        lblCount = new Label("");

        // Group find field with next/prev buttons in a bordered container
        HBox findGroup = new HBox(2, findField, btnNext, btnPrev);
        findGroup.setAlignment(Pos.CENTER_LEFT);
        findGroup.setStyle("-fx-border-color: #999999; -fx-border-width: 1px; -fx-padding: 1px;");

        findBar = new HBox(8, new Label("Find:"),
                findGroup, chkCase, chkWord, chkRegex,
                new Label("Replace:"), replaceField, btnReplace, btnReplaceAll,
                lblCount, btnClose);
        findBar.getStyleClass().add("find-bar");
        findBar.setVisible(false);
        findBar.setManaged(false); // don't reserve height when hidden

        findBar.setAlignment(Pos.CENTER_LEFT);   // center vertically, left-align horizontally
        findBar.setFillHeight(false);

// Place the bar above the editor
        VBox top = new VBox(4, new Label("Code:"), findBar, dispAreaFrame);
        top.setStyle("-fx-padding: 2;");

        // Buttons row (bottom)
        Button runBtn = new Button("Run");
        runBtn.setDefaultButton(true);
        runBtn.setPadding(new Insets(5, 10, 5, 10));
        ButtonShortcutHelper.addAltShortcut(runBtn, KeyCode.U);
        
        // Only enable Run button for .ebs files
        if (!isEbs) {
            runBtn.setDisable(true);
            runBtn.setTooltip(new Tooltip("Run button is only available for .ebs files"));
        } else {
            runBtn.setTooltip(new Tooltip("Run the EBS script"));
        }

        Button clearBtn = new Button("Clear");
        clearBtn.setPadding(new Insets(5, 10, 5, 10));
        clearBtn.setOnAction(e -> outputArea.clear());
        ButtonShortcutHelper.addAltShortcut(clearBtn, KeyCode.L);

        // Show the "start in" directory (script's parent directory)
        Path startInDir = tabContext.path != null ? tabContext.path.getParent() : null;
        String startInText = startInDir != null ? startInDir.toString() : System.getProperty("user.dir");
        Label startInLabel = new Label("Start in: " + startInText);
        startInLabel.getStyleClass().add("start-in-label");
        startInLabel.setMaxWidth(400); // Limit width to prevent layout issues with long paths
        startInLabel.setTooltip(new Tooltip("File operations use relative paths from this directory\n" + startInText));

        // Create button row - add View button for HTML and Markdown files
        HBox buttons;
        if (isHtml || isMd) {
            Button viewBtn = new Button("View");
            viewBtn.setPadding(new Insets(5, 10, 5, 10));
            ButtonShortcutHelper.addAltShortcut(viewBtn, KeyCode.V);
            if (isHtml) {
                viewBtn.setTooltip(new Tooltip("Open HTML in WebView"));
                viewBtn.setOnAction(e -> openHtmlInWebView());
            } else if (isMd) {
                viewBtn.setTooltip(new Tooltip("Convert Markdown to HTML and view in WebView"));
                viewBtn.setOnAction(e -> openMarkdownInWebView());
            }
            buttons = new HBox(8, runBtn, clearBtn, viewBtn, startInLabel);
        } else {
            buttons = new HBox(8, runBtn, clearBtn, startInLabel);
        }
        buttons.setStyle("-fx-padding: 6 4 0 0;");

        VBox bottom = new VBox(2, new Label("Output:"), outputAreaFrame, buttons);
        bottom.setStyle("-fx-padding: 2;");

        VBox.setVgrow(dispAreaFrame, Priority.ALWAYS);
        VBox.setVgrow(outputAreaFrame, Priority.ALWAYS);

        // Split pane vertical
        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(top, bottom);
        split.setDividerPositions(0.6); // 60% code, 40% output by default
        this.setText(tabContext.name);
        this.setContent(split);
        //Tab tab = new Tab(fileName, split);
        setClosable(true);
        setTooltip(new Tooltip(tabContext.path.toString()));
        getStyleClass().add("tab-file");

        // Store path + handle on the tab
        setUserData(tabContext);

        // ✅ When the tab closes, close the file (prefer handle; fall back to path)
        setOnClosed(ev -> {
            try {
                if (tabContext != null) {
                    String handle = tabContext.fileContext.handle;
                    if (!tabContext.fileContext.closed && handle != null && !handle.isBlank()) {
                        handler.submit("/close " + tabContext.fileContext);
                    }
                }
            } catch (Exception ex) {
                handler.submitErrors("file.close on tab close failed: " + ex.getMessage());
            }
        });

        findBar.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.F) {
                Platform.runLater(() -> {
                    hideFind();                  // close if open
                });
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.H) {
                Platform.runLater(() -> {
                    showFind(true);
                });
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && findBar.isVisible()) {
                Platform.runLater(() -> {
                    hideFind();
                });
                e.consume();
            }
        });
        
        // Setup find listeners once during initialization
        setupFindListeners();

        dispArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.F) {
                Platform.runLater(() -> {
                    if (findBar.isVisible()) {
                        hideFind();                  // close if open
                    } else {
                        showFind(false);             // open in Find-only mode
                    }
                });
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.H) {
                Platform.runLater(() -> {
                    showFind(true);
                });
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && findBar.isVisible()) {
                Platform.runLater(() -> {
                    hideFind();
                });
                e.consume();
            }
        });

        // Run implementation (simple path: send to EbsHandler -> Console)
        runBtn.setOnAction(e -> {
            String src = dispArea.getSelectedText().isEmpty() ? dispArea.getText() : dispArea.getSelectedText();
            outputArea.printlnWarn("> Running " + tabContext.path.getFileName() + (dispArea.getSelectedText().isEmpty() ? "" : " (selection)") + "...");
            runBtn.setDisable(true);
            
            // Update status bar from handler (uses main window's status bar)
            if (handler instanceof EbsHandler) {
                StatusBar statusBar = ((EbsHandler) handler).getStatusBar();
                if (statusBar != null) {
                    statusBar.setStatus("Running");
                    statusBar.clearMessage();
                }
            }

            // Offload execution to avoid freezing the UI if scripts are long
            Thread t = new Thread(() -> {
                try {
                    // Clear all previous script setups before running
                    cleanupBeforeRun();
                    
                    // Submit to the current console/handler (prints go to console).
                    handler.submit(src);

                    Platform.runLater(() -> {
                        outputArea.printlnOk("✓ Done.");
                        if (handler instanceof EbsHandler) {
                            StatusBar statusBar = ((EbsHandler) handler).getStatusBar();
                            if (statusBar != null) {
                                statusBar.clearStatus();
                                statusBar.setMessage("Execution completed");
                            }
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        outputArea.printlnError("✗ Error: " + Util.formatExceptionWith2Origin(ex));
                        if (handler instanceof EbsHandler) {
                            StatusBar statusBar = ((EbsHandler) handler).getStatusBar();
                            if (statusBar != null) {
                                statusBar.clearStatus();
                                String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                                String displayMsg = errorMsg.length() > 60 
                                    ? errorMsg.substring(0, 57) + "..." 
                                    : errorMsg;
                                statusBar.setMessage(displayMsg, errorMsg);
                            }
                        }
                    });
                } finally {
                    Platform.runLater(() -> runBtn.setDisable(false));
                }
            }, "ebs-runner");
            t.setDaemon(true);
            t.start();
        });
    }

    private static boolean looksBinary(byte[] sample) {
        if (sample == null) {
            return false;
        }
        int nonText = 0, checked = 0;
        for (byte b : sample) {
            int u = b & 0xFF;
            if (u == 0) {
                return true;
            }                // NUL strongly suggests binary
            if (u < 0x09) {
                nonText++;                    // control range
            }
            checked++;
            if (checked >= 512) {
                break;       // tiny sample
            }
        }
        return nonText > 4;
    }

// ---------- EBS syntax highlighting ----------
    private static final String[] EBS_KEYWORDS = new String[]{
        "var", "print", "call", "return",
        "if", "then", "else", "while", "do", "foreach", "in", "break", "continue",
        // SQL-ish tokens you support in the language:
        "connect", "use", "cursor", "open", "close", "connection",
        "select", "from", "where", "order", "by", "group", "having"
    };

    private static final String[] EBS_TYPES = new String[]{
        "byte", "int", "integer", "long", "float", "double", "string", "date", "bool", "boolean", "json"
    };

    private static final Pattern EBS_PATTERN = buildEbsPattern();

    private static Pattern buildEbsPattern() {
        // Build keyword alternations: \b(?:var|print|...)\\b
        String kw = "\\b(?:" + String.join("|", EBS_KEYWORDS) + ")\\b";
        String tp = "\\b(?:" + String.join("|", EBS_TYPES) + ")\\b";

        // Tokens:
        String COMMENT = "//[^\\n]*";
        String STRING_DQ = "\"([^\"\\\\]|\\\\.)*\"";
        String STRING_SQ = "'([^'\\\\]|\\\\.)*'";
        String NUMBER = "\\b\\d+(?:_\\d+)*(?:\\.\\d+(?:_\\d+)*)?\\b";
        String BOOL = "\\b(?:true|false)\\b";
        String NULLL = "\\bnull\\b";
        String BUILTIN = "\\b(?:(?:json|file|http)\\.[A-Za-z_][A-Za-z0-9_]*)\\b"; // json.get, http.getjson, file.open, etc.
        String FUNCNAME = "\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()";                 // foo( ... ) -> function-like
        String HASHCALL = "#\\s*([A-Za-z_][A-Za-z0-9_]*)";                         // #functionName or # functionName

        // Use named groups
        String master
                = "(?<COMMENT>" + COMMENT + ")"
                + "|(?<STRING>" + STRING_DQ + "|" + STRING_SQ + ")"
                + "|(?<NUMBER>" + NUMBER + ")"
                + "|(?<KEYWORD>" + kw + ")"
                + "|(?<TYPE>" + tp + ")"
                + "|(?<BOOL>" + BOOL + ")"
                + "|(?<NULL>" + NULLL + ")"
                + "|(?<BUILTIN>" + BUILTIN + ")"
                + "|(?<HASHCALL>" + HASHCALL + ")"
                + "|(?<FUNCTION>" + FUNCNAME + ")";

        return Pattern.compile(master, Pattern.MULTILINE);
    }

    private void setupEbsSyntaxHighlighting() {
        // Initial highlight
        applyEbsHighlighting(dispArea.getText());

        // Re-highlight after pauses in typing using ReactFX's debouncing
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyEbsHighlighting(dispArea.getText()));
    }

    private void applyEbsHighlighting(String text) {
        // When find bar is visible AND there are any active highlights (current or stale),
        // skip all styling during editing. Styling will be reapplied after the timer fires.
        if (findBar != null && findBar.isVisible() && 
            (highlightsStale || !lastMatches.isEmpty() || !stalePendingClear.isEmpty())) {
            return;
        }
        
        StyleSpans<Collection<String>> spans = computeEbsHighlighting(text);
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        // Apply to the area
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            // Reapply bracket highlighting after syntax highlighting to ensure it's visible
            dispArea.highlightMatchingBrackets();
        });
    }

    /**
     * Extract custom function names defined in the text.
     * Looks for function definitions like: functionName(...) { or functionName { or function functionName
     */
    private Set<String> extractCustomFunctions(String text) {
        Set<String> functions = new HashSet<>();
        
        // Pattern to match function definitions:
        // 1. functionName(...) return type { or functionName(...) {
        // 2. functionName { 
        // 3. function functionName
        Pattern funcDefPattern = Pattern.compile(
            "(?:^|\\s)(?:function\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*(?:\\([^)]*\\))?\\s*(?:return\\s+[A-Za-z_][A-Za-z0-9_]*)?\\s*\\{",
            Pattern.MULTILINE
        );
        
        Matcher m = funcDefPattern.matcher(text);
        while (m.find()) {
            String funcName = m.group(1);
            // Exclude keywords and types from being considered as function names
            if (funcName != null && !isKeywordOrType(funcName)) {
                functions.add(funcName.toLowerCase());
            }
        }
        
        return functions;
    }
    
    /**
     * Check if a name is a keyword or type
     */
    private boolean isKeywordOrType(String name) {
        String lowerName = name.toLowerCase();
        for (String kw : EBS_KEYWORDS) {
            if (kw.equals(lowerName)) {
                return true;
            }
        }
        for (String tp : EBS_TYPES) {
            if (tp.equals(lowerName)) {
                return true;
            }
        }
        return false;
    }

    private StyleSpans<Collection<String>> computeEbsHighlighting(String text) {
        // First, extract all custom function definitions from the text
        Set<String> customFunctions = extractCustomFunctions(text);
        
        // Get builtin function names
        Set<String> builtins = Builtins.getBuiltins();
        
        Matcher m = EBS_PATTERN.matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);

            String styleClass = null;
            
            if (m.group("COMMENT") != null) {
                styleClass = "tok-comment";
            } else if (m.group("STRING") != null) {
                styleClass = "tok-string";
            } else if (m.group("NUMBER") != null) {
                styleClass = "tok-number";
            } else if (m.group("KEYWORD") != null) {
                styleClass = "tok-keyword";
            } else if (m.group("TYPE") != null) {
                styleClass = "tok-type";
            } else if (m.group("BOOL") != null) {
                styleClass = "tok-bool";
            } else if (m.group("NULL") != null) {
                styleClass = "tok-null";
            } else if (m.group("BUILTIN") != null) {
                styleClass = "tok-builtin";
            } else if (m.group("HASHCALL") != null) {
                // Handle # function calls
                String matched = m.group("HASHCALL");
                // Extract function name after the # and any whitespace
                String funcName = matched.replaceFirst("^#\\s*", "");
                if (funcName != null && !funcName.isEmpty()) {
                    String lowerName = funcName.toLowerCase();
                    if (builtins.contains(lowerName)) {
                        styleClass = "tok-builtin";
                    } else if (customFunctions.contains(lowerName)) {
                        styleClass = "tok-custom-function";
                    } else {
                        styleClass = "tok-undefined-function";
                    }
                }
            } else if (m.group("FUNCTION") != null) {
                // Handle regular function calls (with parentheses)
                String matched = m.group("FUNCTION");
                // Extract function name (everything before whitespace and opening paren)
                String funcName = matched.replaceFirst("\\s*\\($", "").trim();
                if (funcName != null && !funcName.isEmpty()) {
                    String lowerName = funcName.toLowerCase();
                    if (builtins.contains(lowerName)) {
                        styleClass = "tok-builtin";
                    } else if (customFunctions.contains(lowerName)) {
                        styleClass = "tok-custom-function";
                    } else {
                        styleClass = "tok-undefined-function";
                    }
                }
            }

            builder.add(styleClass == null ? Collections.emptyList()
                    : Collections.singleton(styleClass),
                    m.end() - m.start());

            last = m.end();
        }
        // tail
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }

// ---------- CSS syntax highlighting ----------
    private static Pattern CSS_PATTERN = null;
    
    private static Pattern getCssPattern() {
        if (CSS_PATTERN == null) {
            CSS_PATTERN = buildCssPattern();
        }
        return CSS_PATTERN;
    }
    
    private static Pattern buildCssPattern() {
        String[] CSS_PROPERTIES = new String[]{
            "align-content", "align-items", "align-self", "animation", "animation-delay",
            "animation-direction", "animation-duration", "animation-fill-mode", "animation-iteration-count",
            "animation-name", "animation-play-state", "animation-timing-function", "backface-visibility",
            "background", "background-attachment", "background-clip", "background-color", "background-image",
            "background-origin", "background-position", "background-repeat", "background-size", "border",
            "border-bottom", "border-bottom-color", "border-bottom-left-radius", "border-bottom-right-radius",
            "border-bottom-style", "border-bottom-width", "border-collapse", "border-color", "border-image",
            "border-left", "border-left-color", "border-left-style", "border-left-width", "border-radius",
            "border-right", "border-right-color", "border-right-style", "border-right-width", "border-spacing",
            "border-style", "border-top", "border-top-color", "border-top-left-radius", "border-top-right-radius",
            "border-top-style", "border-top-width", "border-width", "bottom", "box-shadow", "box-sizing",
            "caption-side", "clear", "clip", "color", "column-count", "column-gap", "column-rule",
            "column-rule-color", "column-rule-style", "column-rule-width", "column-width", "columns",
            "content", "counter-increment", "counter-reset", "cursor", "direction", "display",
            "empty-cells", "filter", "flex", "flex-basis", "flex-direction", "flex-flow", "flex-grow",
            "flex-shrink", "flex-wrap", "float", "font", "font-family", "font-size", "font-size-adjust",
            "font-stretch", "font-style", "font-variant", "font-weight", "grid", "grid-area", "grid-auto-columns",
            "grid-auto-flow", "grid-auto-rows", "grid-column", "grid-column-end", "grid-column-gap",
            "grid-column-start", "grid-gap", "grid-row", "grid-row-end", "grid-row-gap", "grid-row-start",
            "grid-template", "grid-template-areas", "grid-template-columns", "grid-template-rows",
            "height", "justify-content", "left", "letter-spacing", "line-height", "list-style",
            "list-style-image", "list-style-position", "list-style-type", "margin", "margin-bottom",
            "margin-left", "margin-right", "margin-top", "max-height", "max-width", "min-height",
            "min-width", "opacity", "order", "outline", "outline-color", "outline-offset", "outline-style",
            "outline-width", "overflow", "overflow-x", "overflow-y", "padding", "padding-bottom",
            "padding-left", "padding-right", "padding-top", "page-break-after", "page-break-before",
            "page-break-inside", "perspective", "perspective-origin", "position", "quotes", "resize",
            "right", "tab-size", "table-layout", "text-align", "text-align-last", "text-decoration",
            "text-decoration-color", "text-decoration-line", "text-decoration-style", "text-indent",
            "text-justify", "text-overflow", "text-shadow", "text-transform", "top", "transform",
            "transform-origin", "transform-style", "transition", "transition-delay", "transition-duration",
            "transition-property", "transition-timing-function", "vertical-align", "visibility",
            "white-space", "width", "word-break", "word-spacing", "word-wrap", "z-index"
        };
        
        // Build property alternations
        String props = "\\b(?:" + String.join("|", CSS_PROPERTIES) + ")\\b";
        
        // CSS Tokens
        String COMMENT = "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/";  // /* ... */
        String SELECTOR = "(?:[.#]?[a-zA-Z_][a-zA-Z0-9_-]*|\\*|\\[[^\\]]+\\]|:[a-zA-Z_-]+(?:\\([^)]*\\))?)";  // .class, #id, tag, *, [attr], :pseudo
        String PROPERTY = props;
        String COLOR = "(?:#[0-9a-fA-F]{3}\\b|#[0-9a-fA-F]{4}\\b|#[0-9a-fA-F]{6}\\b|#[0-9a-fA-F]{8}\\b)";  // #rgb, #rgba, #rrggbb, #rrggbbaa
        String UNIT = "\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|vmin|vmax|cm|mm|in|pt|pc|ex|ch|deg|rad|turn|s|ms)\\b";
        String NUMBER = "\\b\\d+(?:\\.\\d+)?\\b";
        String STRING_DQ = "\"([^\"\\\\]|\\\\.)*\"";
        String STRING_SQ = "'([^'\\\\]|\\\\.)*'";
        String IMPORTANT = "!important\\b";
        String AT_RULE = "@[a-zA-Z_-]+";  // @media, @import, @keyframes, etc.
        
        // Use named groups
        String master =
                "(?<COMMENT>" + COMMENT + ")"
                + "|(?<ATRULE>" + AT_RULE + ")"
                + "|(?<IMPORTANT>" + IMPORTANT + ")"
                + "|(?<PROPERTY>" + PROPERTY + ")"
                + "|(?<COLOR>" + COLOR + ")"
                + "|(?<STRING>" + STRING_DQ + "|" + STRING_SQ + ")"
                + "|(?<UNIT>" + UNIT + ")"
                + "|(?<NUMBER>" + NUMBER + ")"
                + "|(?<SELECTOR>" + SELECTOR + ")";
        
        return Pattern.compile(master, Pattern.MULTILINE);
    }
    
    private void setupCssHighlighting() {
        // Initial highlight
        applyCssHighlighting(dispArea.getText());
        
        // Re-highlight after pauses in typing using ReactFX's debouncing
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyCssHighlighting(dispArea.getText()));
    }
    
    private void applyCssHighlighting(String text) {
        StyleSpans<Collection<String>> spans = computeCssHighlighting(text);
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            dispArea.highlightMatchingBrackets();
        });
    }
    
    private StyleSpans<Collection<String>> computeCssHighlighting(String text) {
        Matcher m = getCssPattern().matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        
        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);
            
            String styleClass =
                    m.group("COMMENT") != null ? "tok-comment"
                    : m.group("ATRULE") != null ? "tok-keyword"
                    : m.group("IMPORTANT") != null ? "tok-keyword"
                    : m.group("PROPERTY") != null ? "tok-builtin"
                    : m.group("COLOR") != null ? "tok-string"
                    : m.group("STRING") != null ? "tok-string"
                    : m.group("UNIT") != null ? "tok-number"
                    : m.group("NUMBER") != null ? "tok-number"
                    : m.group("SELECTOR") != null ? "tok-type" : null;
            
            builder.add(styleClass == null ? Collections.emptyList()
                    : Collections.singleton(styleClass),
                    m.end() - m.start());
            
            last = m.end();
        }
        // tail
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }

// ---------- HTML syntax highlighting ----------
    private static Pattern HTML_PATTERN = null;
    
    private static Pattern getHtmlPattern() {
        if (HTML_PATTERN == null) {
            HTML_PATTERN = buildHtmlPattern();
        }
        return HTML_PATTERN;
    }
    
    private static Pattern buildHtmlPattern() {
        String[] HTML_TAGS = new String[]{
            "a", "abbr", "address", "area", "article", "aside", "audio", "b", "base", "bdi", "bdo",
            "blockquote", "body", "br", "button", "canvas", "caption", "cite", "code", "col", "colgroup",
            "data", "datalist", "dd", "del", "details", "dfn", "dialog", "div", "dl", "dt", "em", "embed",
            "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6",
            "head", "header", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "label", "legend",
            "li", "link", "main", "map", "mark", "meta", "meter", "nav", "noscript", "object", "ol",
            "optgroup", "option", "output", "p", "param", "picture", "pre", "progress", "q", "rp", "rt",
            "ruby", "s", "samp", "script", "section", "select", "small", "source", "span", "strong",
            "style", "sub", "summary", "sup", "svg", "table", "tbody", "td", "template", "textarea",
            "tfoot", "th", "thead", "time", "title", "tr", "track", "u", "ul", "var", "video", "wbr"
        };
        
        // Build tag alternations
        String tags = "\\b(?:" + String.join("|", HTML_TAGS) + ")\\b";
        
        // HTML Tokens
        String COMMENT = "<!--[^-]*(?:-(?!->)[^-]*)*-->";  // <!-- ... -->
        String DOCTYPE = "<!DOCTYPE[^>]*>";
        String TAG_OPEN = "</?(" + tags + ")";  // <tag or </tag
        String TAG_CLOSE = "/?>";  // > or />
        String ATTRIBUTE = "\\b[a-zA-Z_:][-a-zA-Z0-9_:.]*(?=\\s*=)";  // attribute names
        String STRING_DQ = "\"([^\"\\\\]|\\\\.)*\"";
        String STRING_SQ = "'([^'\\\\]|\\\\.)*'";
        String ENTITY = "&(?:[a-zA-Z][a-zA-Z0-9]*|#[0-9]+|#x[0-9a-fA-F]+);";  // &nbsp; &#123; &#xAB;
        
        // Use named groups
        String master =
                "(?<COMMENT>" + COMMENT + ")"
                + "|(?<DOCTYPE>" + DOCTYPE + ")"
                + "|(?<TAGOPEN>" + TAG_OPEN + ")"
                + "|(?<TAGCLOSE>" + TAG_CLOSE + ")"
                + "|(?<ATTRIBUTE>" + ATTRIBUTE + ")"
                + "|(?<STRING>" + STRING_DQ + "|" + STRING_SQ + ")"
                + "|(?<ENTITY>" + ENTITY + ")";
        
        return Pattern.compile(master, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    }
    
    private void setupHtmlHighlighting() {
        // Initial highlight
        applyHtmlHighlighting(dispArea.getText());
        
        // Re-highlight after pauses in typing using ReactFX's debouncing
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyHtmlHighlighting(dispArea.getText()));
    }
    
    private void applyHtmlHighlighting(String text) {
        StyleSpans<Collection<String>> spans = computeHtmlHighlighting(text);
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            dispArea.highlightMatchingBrackets();
        });
    }
    
    private StyleSpans<Collection<String>> computeHtmlHighlighting(String text) {
        Matcher m = getHtmlPattern().matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        
        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);
            
            String styleClass =
                    m.group("COMMENT") != null ? "tok-comment"
                    : m.group("DOCTYPE") != null ? "tok-keyword"
                    : m.group("TAGOPEN") != null ? "tok-keyword"
                    : m.group("TAGCLOSE") != null ? "tok-keyword"
                    : m.group("ATTRIBUTE") != null ? "tok-builtin"
                    : m.group("STRING") != null ? "tok-string"
                    : m.group("ENTITY") != null ? "tok-type" : null;
            
            builder.add(styleClass == null ? Collections.emptyList()
                    : Collections.singleton(styleClass),
                    m.end() - m.start());
            
            last = m.end();
        }
        // tail
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }

// ---------- JSON syntax highlighting ----------
    private static Pattern JSON_PATTERN = null;
    
    private static Pattern getJsonPattern() {
        if (JSON_PATTERN == null) {
            JSON_PATTERN = buildJsonPattern();
        }
        return JSON_PATTERN;
    }
    
    private static Pattern buildJsonPattern() {
        // JSON Tokens
        String COMMENT_SINGLE = "//[^\\n]*";  // Single-line comment (non-standard but common)
        String COMMENT_MULTI = "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/";  // /* ... */
        String KEY = "\"([^\"\\\\]|\\\\.)*\"\\s*(?=:)";  // "key": (string followed by colon)
        String STRING = "\"([^\"\\\\]|\\\\.)*\"";  // "string"
        String NUMBER = "-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b";  // JSON numbers
        String BOOLEAN = "\\b(?:true|false)\\b";
        String NULL = "\\bnull\\b";
        
        // Use named groups (NOTE: Java regex group names cannot contain underscores)
        String master =
                "(?<COMMENTSINGLE>" + COMMENT_SINGLE + ")"
                + "|(?<COMMENTMULTI>" + COMMENT_MULTI + ")"
                + "|(?<KEY>" + KEY + ")"
                + "|(?<STRING>" + STRING + ")"
                + "|(?<NUMBER>" + NUMBER + ")"
                + "|(?<BOOLEAN>" + BOOLEAN + ")"
                + "|(?<NULL>" + NULL + ")";
        
        return Pattern.compile(master, Pattern.MULTILINE);
    }
    
    private void setupJsonHighlighting() {
        // Initial highlight
        applyJsonHighlighting(dispArea.getText());
        
        // Re-highlight after pauses in typing using ReactFX's debouncing
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyJsonHighlighting(dispArea.getText()));
    }
    
    private void applyJsonHighlighting(String text) {
        StyleSpans<Collection<String>> spans = computeJsonHighlighting(text);
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            dispArea.highlightMatchingBrackets();
        });
    }
    
    private StyleSpans<Collection<String>> computeJsonHighlighting(String text) {
        Matcher m = getJsonPattern().matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        
        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);
            
            String styleClass =
                    m.group("COMMENTSINGLE") != null ? "tok-comment"
                    : m.group("COMMENTMULTI") != null ? "tok-comment"
                    : m.group("KEY") != null ? "tok-builtin"
                    : m.group("STRING") != null ? "tok-string"
                    : m.group("NUMBER") != null ? "tok-number"
                    : m.group("BOOLEAN") != null ? "tok-bool"
                    : m.group("NULL") != null ? "tok-null" : null;
            
            builder.add(styleClass == null ? Collections.emptyList()
                    : Collections.singleton(styleClass),
                    m.end() - m.start());
            
            last = m.end();
        }
        // tail
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }

// ---------- Markdown syntax highlighting ----------
    private static Pattern MD_PATTERN = null;
    
    private static Pattern getMdPattern() {
        if (MD_PATTERN == null) {
            MD_PATTERN = buildMdPattern();
        }
        return MD_PATTERN;
    }
    
    private static Pattern buildMdPattern() {
        // Markdown Tokens (basic highlighting)
        String HEADING = "^#{1,6}\\s+.*$";  // # Heading
        String BOLD = "\\*\\*[^*]+\\*\\*|__[^_]+__";  // **bold** or __bold__
        String ITALIC = "\\*[^*]+\\*|_[^_]+_";  // *italic* or _italic_
        String CODE_BLOCK = "```[\\s\\S]*?```|~~~[\\s\\S]*?~~~";  // ```code```
        String INLINE_CODE = "`[^`]+`";  // `code`
        String LINK = "\\[[^\\]]+\\]\\([^)]+\\)";  // [text](url)
        String LIST = "^\\s*[-*+]\\s+.*$|^\\s*\\d+\\.\\s+.*$";  // - item or 1. item
        String BLOCKQUOTE = "^>\\s+.*$";  // > quote
        
        // Use named groups (order matters - more specific patterns first)
        String master =
                "(?<CODEBLOCK>" + CODE_BLOCK + ")"
                + "|(?<INLINECODE>" + INLINE_CODE + ")"
                + "|(?<BOLD>" + BOLD + ")"
                + "|(?<ITALIC>" + ITALIC + ")"
                + "|(?<LINK>" + LINK + ")"
                + "|(?<HEADING>" + HEADING + ")"
                + "|(?<LIST>" + LIST + ")"
                + "|(?<BLOCKQUOTE>" + BLOCKQUOTE + ")";
        
        return Pattern.compile(master, Pattern.MULTILINE);
    }
    
    private void setupMdHighlighting() {
        // Initial highlight
        applyMdHighlighting(dispArea.getText());
        
        // Re-highlight after pauses in typing using ReactFX's debouncing
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyMdHighlighting(dispArea.getText()));
    }
    
    private void applyMdHighlighting(String text) {
        StyleSpans<Collection<String>> spans = computeMdHighlighting(text);
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            dispArea.highlightMatchingBrackets();
        });
    }
    
    private StyleSpans<Collection<String>> computeMdHighlighting(String text) {
        Matcher m = getMdPattern().matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        
        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);
            
            String styleClass =
                    m.group("CODEBLOCK") != null ? "tok-string"
                    : m.group("HEADING") != null ? "tok-keyword"
                    : m.group("BOLD") != null ? "tok-keyword"
                    : m.group("ITALIC") != null ? "tok-type"
                    : m.group("INLINECODE") != null ? "tok-string"
                    : m.group("LINK") != null ? "tok-builtin"
                    : m.group("LIST") != null ? "tok-keyword"
                    : m.group("BLOCKQUOTE") != null ? "tok-comment" : null;
            
            builder.add(styleClass == null ? Collections.emptyList()
                    : Collections.singleton(styleClass),
                    m.end() - m.start());
            
            last = m.end();
        }
        // tail
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }

// ---------- Lexer-based highlighter ----------
    private final EbsLexer ebsLexer = new EbsLexer();

    private void setupLexerHighlighting() {
        // Initial pass
        applyLexerSpans(dispArea.getText());

        // Re-highlight after pauses in typing using ReactFX's debouncing
        // This is more efficient than the simple text listener as it batches
        // multiple rapid keystrokes into a single highlighting pass
        dispArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(100))
                .subscribe(ignore -> applyLexerSpans(dispArea.getText()));
    }

    private void applyLexerSpans(String src) {
        // When find bar is visible AND there are any active highlights (current or stale),
        // skip all styling during editing. Styling will be reapplied after the timer fires.
        // We check multiple conditions to catch the first keystroke before highlightsStale is set.
        if (findBar != null && findBar.isVisible() && 
            (highlightsStale || !lastMatches.isEmpty() || !stalePendingClear.isEmpty())) {
            return;
        }
        
        // Dispatch to appropriate highlighter based on file extension
        boolean isCss = ext.equalsIgnoreCase(".css");
        boolean isHtml = ext.equalsIgnoreCase(".html");
        boolean isJson = ext.equalsIgnoreCase(".json");
        boolean isMd = ext.equalsIgnoreCase(".md");
        
        if (isCss) {
            applyCssHighlighting(src);
            return;
        } else if (isHtml) {
            applyHtmlHighlighting(src);
            return;
        } else if (isJson) {
            applyJsonHighlighting(src);
            return;
        } else if (isMd) {
            applyMdHighlighting(src);
            return;
        }
        
        // Default: use EBS lexer for all other files
        List<EbsToken> tokens = ebsLexer.tokenize(src); // returns List<EbsToken> with start/end/style
        // Build spans from token positions
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        int pos = 0;
        for (EbsToken t : tokens) {
            if (t.start <= t.end) {//test empty string
                int start = Math.max(0, t.start);
                int endInclusive = Math.max(start, t.end); // lexer uses inclusive end for JSON slicing in parser
                int tokenLen = endInclusive - start + 1;

                // gap before token: no style
                if (start > pos) {
                    tokenLen = tokenLen + (start - pos);
                    //builder.add(Collections.emptyList(), start - pos);
                }

                // token style (from EbsToken.style → EbsTokenType.getStyle() → LexerType.getStyle() → PrintStyle.styleClass())
                //Collection<String> styles = (t.style != null) ? t.styleList : defaultStyle;
                builder.add(t.styleList, Math.max(0, tokenLen));
                pos = endInclusive + 1;
            }
        }

        // tail gap
        if (pos < src.length()) {
            builder.add(Collections.emptyList(), src.length() - pos);
        }

        StyleSpans<Collection<String>> spans = builder.create();
        
        // Preserve scroll position when applying style spans
        double scrollY = dispArea.getEstimatedScrollY();
        
        dispArea.setStyleSpans(0, spans);
        
        // Restore scroll position after style update
        Platform.runLater(() -> {
            dispArea.scrollYToPixel(scrollY);
            // Reapply bracket highlighting after syntax highlighting to ensure it's visible
            dispArea.highlightMatchingBrackets();
        });
    }

    public String getEditorText() {
        return dispArea.getText();
    }

    // Mark tab as dirty and add '*'
    private void markDirty() {
        if (!dirty) {
            dirty = true;
            setText(baseTitle + "*");
            if (!getStyleClass().contains("tab-changed")) {
                getStyleClass().add("tab-changed");   // <— add style for this tab only
            }
        }
    }

    // Called after a successful Save to disk
    public void markCleanTitle() {
        if (dirty) {
            dirty = false;
            setText(baseTitle);
            getStyleClass().remove("tab-changed");   // <— add style for this tab only
        }
    }

    /**
     * Check if the tab has unsaved changes.
     * @return true if the tab has unsaved changes, false otherwise
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Initialize tab with content for a new (unsaved) file.
     * Marks the tab as dirty to indicate it needs to be saved.
     * @param content Initial content for the file
     */
    public void initializeAsNewFile(String content) {
        suppressDirty = true;
        dispArea.replaceText(content);
        suppressDirty = false;
        // Clear undo history after a delay to ensure syntax highlighting is applied first
        // The multiPlainChanges listener triggers highlighting after 100ms
        PauseTransition clearUndoDelay = new PauseTransition(Duration.millis(200));
        clearUndoDelay.setOnFinished(e -> {
            dispArea.getUndoManager().forgetHistory();
        });
        clearUndoDelay.play();
        markDirty();  // Mark as dirty since it's a new unsaved file
    }

    // Setup find bar listeners once during initialization
    private void setupFindListeners() {
        // Initialize the editor change timer (1 second delay)
        editorChangeTimer = new PauseTransition(Duration.seconds(1));
        editorChangeTimer.setOnFinished(e -> {
            if (findBar.isVisible() && highlightsStale) {
                // Reset stale flag FIRST so applyLexerSpans won't skip
                highlightsStale = false;
                // Now reapply syntax highlighting (was skipped during editing)
                applyLexerSpans(dispArea.getText());
                // Redo the find search on new text, but don't move cursor or change find position
                runSearchHighlightOnly();
            }
        });
        
        // Listen for editor changes to trigger debounced re-highlighting
        dispArea.textProperty().addListener((obs, oldText, newText) -> {
            if (findBar.isVisible()) {
                // When find bar is visible and editing, we need to refresh styling after delay
                if (!lastMatches.isEmpty()) {
                    // Save current matches for clearing later when timer fires
                    stalePendingClear = lastMatches;
                    lastMatches = java.util.Collections.emptyList();
                    currentIndex = -1;
                }
                highlightsStale = true;
                // Reset and start the timer for re-highlighting (syntax + find)
                editorChangeTimer.playFromStart();
            }
        });
        
        // Live search when typing in find field (use the editor's text property for editable combobox)
        findField.getEditor().textProperty().addListener((obs, o, n) -> {
            if (!suppressFindSearch && !dropdownOpen) {
                Platform.runLater(() -> {
                    runSearch();
                });
            }
        });
        
        // Track when dropdown opens
        findField.setOnShowing(e -> {
            dropdownOpen = true;
        });
        
        // Handle dropdown selection when it closes
        findField.setOnHidden(e -> {
            if (dropdownOpen) {
                dropdownOpen = false;
                // Get the currently selected item
                String selected = findField.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    // The selection changed - update editor and search
                    Platform.runLater(() -> {
                        suppressFindSearch = true;
                        findField.getEditor().setText(selected);
                        suppressFindSearch = false;
                        addToSearchHistory(selected);
                        runSearch();
                    });
                }
            }
        });
        
        // Also search when user presses Enter (and add to history)
        findField.setOnAction(e -> {
            if (!suppressFindSearch && !dropdownOpen) {
                Platform.runLater(() -> {
                    // Add to history when user presses Enter
                    String q = findField.getEditor().getText();
                    if (q != null && q.length() >= MIN_FIND_CHARS) {
                        addToSearchHistory(q);
                    }
                    runSearch();
                });
            }
        });
        
        // Re-search when checkboxes change
        chkCase.selectedProperty().addListener((obs, o, n) -> {
            Platform.runLater(() -> {
                runSearch();
            });
        });
        chkWord.selectedProperty().addListener((obs, o, n) -> {
            Platform.runLater(() -> {
                runSearch();
            });
        });
        chkRegex.selectedProperty().addListener((obs, o, n) -> {
            Platform.runLater(() -> {
                runSearch();
            });
        });
        
        // Button actions - next/prev immediately re-run highlighting if stale then navigate
        btnNext.setOnAction(e -> {
            Platform.runLater(() -> {
                // Add to history when user clicks Next
                String q = findField.getEditor().getText();
                if (q != null && q.length() >= MIN_FIND_CHARS) {
                    addToSearchHistory(q);
                }
                refreshHighlightsIfStale();
                gotoNext();
                dispArea.requestFocus();
            });
        });
        btnPrev.setOnAction(e -> {
            Platform.runLater(() -> {
                // Add to history when user clicks Prev
                String q = findField.getEditor().getText();
                if (q != null && q.length() >= MIN_FIND_CHARS) {
                    addToSearchHistory(q);
                }
                refreshHighlightsIfStale();
                gotoPrev();
                dispArea.requestFocus();
            });
        });
        btnReplace.setOnAction(e -> {
            Platform.runLater(() -> {
                replaceOne();
            });
        });
        btnReplaceAll.setOnAction(e -> {
            Platform.runLater(() -> {
                replaceAll();
            });
        });
        btnClose.setOnAction(e -> {
            Platform.runLater(() -> {
                hideFind();
            });
        });
    }

    // Public method to show find bar from menu
    public void showFindFromMenu(boolean withReplace) {
        dispArea.requestFocus();
        if (findBar.isVisible() && !withReplace) {
            hideFind();
        } else {
            showFind(withReplace);
        }
    }
    
    // Public method to toggle line numbers
    public void toggleLineNumbers() {
        dispArea.toggleLineNumbers();
    }

    private void showFind(boolean withReplace) {
        findBar.setVisible(true);
        findBar.setManaged(true);
        replaceField.setDisable(!withReplace);
        btnReplace.setDisable(!withReplace);
        btnReplaceAll.setDisable(!withReplace);
        
        // Capture the search origin position before any search is performed
        // Use selection start if there's a selection, otherwise use caret position
        if (dispArea.getSelection().getLength() > 0) {
            searchOriginPosition = dispArea.getSelection().getStart();
        } else {
            searchOriginPosition = dispArea.getCaretPosition();
        }
        
        // Clear find field and populate with current selection if any
        String selectedText = dispArea.getSelectedText();
        suppressFindSearch = true;
        if (selectedText != null && !selectedText.isEmpty()) {
            findField.getEditor().setText(selectedText);
        } else {
            // Clear the find field when showing find bar with no selection
            findField.getEditor().setText("");
            clearHighlights();
            lastMatches = java.util.Collections.emptyList();
            currentIndex = -1;
            lblCount.setText("");
        }
        suppressFindSearch = false;
        
        // Explicitly run search after populating the field (only if there's text)
        if (!findField.getEditor().getText().isEmpty()) {
            runSearch();
        }
        
        Platform.runLater(() -> findField.requestFocus());
    }

    private void hideFind() {
        findBar.setVisible(false);
        findBar.setManaged(false);
        lastMatches = java.util.Collections.emptyList();
        stalePendingClear = java.util.Collections.emptyList();
        currentIndex = -1;
        searchOriginPosition = -1; // Reset search origin when closing find bar
        // Stop any pending timer and reset stale flag
        if (editorChangeTimer != null) {
            editorChangeTimer.stop();
        }
        highlightsStale = false;
        
        // Reapply syntax highlighting to remove find highlights
        applyLexerSpans(dispArea.getText());
        
        // Note: With custom undo manager, we no longer need to clear undo history
        // The custom undo manager only tracks text changes, not style changes (highlighting)
    }

    private void runSearch() {
        clearHighlights();
        currentIndex = -1;
        String q = findField.getEditor().getText();
        if (q == null || q.isEmpty()) {
            lblCount.setText("");
            lastMatches = java.util.Collections.emptyList();
            return;
        }
        
        // Only highlight when there are at least 2 characters
        if (q.length() < MIN_FIND_CHARS) {
            lblCount.setText("Enter " + MIN_FIND_CHARS + "+ chars to search");
            lastMatches = java.util.Collections.emptyList();
            return;
        }
        
        // Note: Don't add to search history here - it happens on every keystroke
        // and modifying the ComboBox's items list clears the editor text.
        // History is added when user presses Enter, clicks Next/Prev, or selects from dropdown.

        boolean cs = chkCase.isSelected();
        boolean ww = chkWord.isSelected();
        boolean rx = chkRegex.isSelected();

        String text = dispArea.getText();
        ArrayList<int[]> hits = new ArrayList<>();

        try {
            java.util.regex.Pattern pat;
            if (rx) {
                int flags = cs ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                pat = java.util.regex.Pattern.compile(q, flags);
            } else {
                // Escape the literal unless regex is on
                String literal = java.util.regex.Pattern.quote(q);
                String pattern = ww ? "\\b" + literal + "\\b" : literal;
                int flags = cs ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                pat = java.util.regex.Pattern.compile(pattern, flags);
            }
            java.util.regex.Matcher m = pat.matcher(text);
            while (m.find()) {
                int s = m.start();
                int e = m.end(); // exclusive
                hits.add(new int[]{s, e});
            }
        } catch (Exception ex) {
            // invalid regex; show nothing
        }

        lastMatches = hits;
        
        if (hits.isEmpty()) {
            lblCount.setText("0 matches");
            return;
        }

        // Find the first match at or after the search origin position
        // This ensures that as the user types more characters, the search stays
        // at the original location instead of jumping to the next match
        int searchStartPos = searchOriginPosition >= 0 ? searchOriginPosition : dispArea.getCaretPosition();
        
        currentIndex = 0;
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i)[0] >= searchStartPos) {
                currentIndex = i;
                break;
            }
        }
        
        int[] cur = hits.get(currentIndex);
        selectCurrent(cur);
        updateCountLabel();

        // Highlight all matches (optimized via batched style updates in ScriptArea)
        for (int[] r : hits) {
            dispArea.addStyleToRange(r[0], r[1], "find-hit");
        }
        // Emphasize current
        dispArea.addStyleToRange(cur[0], cur[1], "find-current");
    }
    
    /**
     * Run search but only refresh highlights without jumping to a location.
     * Used when the editor content changes and we want to re-highlight after a delay.
     */
    private void runSearchHighlightOnly() {
        clearHighlights();
        String q = findField.getEditor().getText();
        if (q == null || q.isEmpty() || q.length() < MIN_FIND_CHARS) {
            lastMatches = java.util.Collections.emptyList();
            currentIndex = -1;
            lblCount.setText(q == null || q.isEmpty() ? "" : "Enter " + MIN_FIND_CHARS + "+ chars to search");
            return;
        }

        boolean cs = chkCase.isSelected();
        boolean ww = chkWord.isSelected();
        boolean rx = chkRegex.isSelected();

        String text = dispArea.getText();
        ArrayList<int[]> hits = new ArrayList<>();

        try {
            java.util.regex.Pattern pat;
            if (rx) {
                int flags = cs ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                pat = java.util.regex.Pattern.compile(q, flags);
            } else {
                String literal = java.util.regex.Pattern.quote(q);
                String pattern = ww ? "\\b" + literal + "\\b" : literal;
                int flags = cs ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                pat = java.util.regex.Pattern.compile(pattern, flags);
            }
            java.util.regex.Matcher m = pat.matcher(text);
            while (m.find()) {
                hits.add(new int[]{m.start(), m.end()});
            }
        } catch (Exception ex) {
            // invalid regex; show nothing
        }

        lastMatches = hits;
        
        if (hits.isEmpty()) {
            currentIndex = -1;
            lblCount.setText("0 matches");
            return;
        }

        // Highlight all matches without selecting or jumping
        for (int[] r : hits) {
            dispArea.addStyleToRange(r[0], r[1], "find-hit");
        }
        
        // Preserve currentIndex if still valid, otherwise find nearest match to caret
        if (currentIndex < 0 || currentIndex >= hits.size()) {
            // currentIndex invalid, find nearest match to caret position
            int caretPos = dispArea.getCaretPosition();
            currentIndex = 0;
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i)[0] >= caretPos) {
                    currentIndex = i;
                    break;
                }
            }
        }
        
        updateCountLabel();
    }

    private void selectCurrent(int[] r) {
        dispArea.selectRange(r[0], r[1]); // selection shows current
        dispArea.moveTo(r[1]);
        
        // Scroll to center the match in the viewport
        int paragraph = dispArea.getCurrentParagraph();
        int visibleParagraphs = dispArea.getVisibleParagraphs().size();
        int offset = visibleParagraphs / 2;
        int targetParagraph = Math.max(0, paragraph - offset);
        
        dispArea.showParagraphAtTop(targetParagraph);
    }

    private void gotoNext() {
        if (lastMatches.isEmpty()) {
            return;
        }
        clearCurrentEmphasis();
        currentIndex = (currentIndex + 1) % lastMatches.size();
        int[] cur = lastMatches.get(currentIndex);
        selectCurrent(cur);
        updateCountLabel();
        dispArea.addStyleToRange(cur[0], cur[1], "find-current");
        // Update search origin to current match start so subsequent typing searches from this position
        searchOriginPosition = cur[0];
    }

    private void gotoPrev() {
        if (lastMatches.isEmpty()) {
            return;
        }
        clearCurrentEmphasis();
        currentIndex = (currentIndex - 1 + lastMatches.size()) % lastMatches.size();
        int[] cur = lastMatches.get(currentIndex);
        selectCurrent(cur);
        updateCountLabel();
        dispArea.addStyleToRange(cur[0], cur[1], "find-current");
        // Update search origin to current match start so subsequent typing searches from this position
        searchOriginPosition = cur[0];
    }

    // Clear only the 'find-current' class on the previous current match
    private void clearCurrentEmphasis() {
        if (currentIndex >= 0 && currentIndex < lastMatches.size()) {
            int[] prev = lastMatches.get(currentIndex);
            dispArea.removeStyleFromRange(prev[0], prev[1], "find-current");
        }
    }

    // Update the count label to show "current/total matches"
    private void updateCountLabel() {
        if (lastMatches.isEmpty()) {
            lblCount.setText("0 matches");
        } else {
            lblCount.setText((currentIndex + 1) + "/" + lastMatches.size() + " matches");
        }
    }

    private void clearHighlights() {
        // Remove highlight classes from all previously-highlighted ranges
        // (optimized via batched style updates in ScriptArea)
        for (int[] r : lastMatches) {
            dispArea.removeStyleFromRange(r[0], r[1], "find-hit");
            dispArea.removeStyleFromRange(r[0], r[1], "find-current");
        }
        // Also clear any stale highlights that were pending from text changes
        for (int[] r : stalePendingClear) {
            dispArea.removeStyleFromRange(r[0], r[1], "find-hit");
            dispArea.removeStyleFromRange(r[0], r[1], "find-current");
        }
        stalePendingClear = java.util.Collections.emptyList();
    }
    
    /**
     * Add a search term to the history, keeping only the last MAX_SEARCH_HISTORY items.
     * If the term already exists, it's moved to the front.
     */
    private void addToSearchHistory(String term) {
        if (term == null || term.isEmpty()) return;
        
        // Remove if already exists (we'll add it to the front)
        searchHistory.remove(term);
        
        // Add to the front
        searchHistory.add(0, term);
        
        // Keep only the last MAX_SEARCH_HISTORY items
        while (searchHistory.size() > MAX_SEARCH_HISTORY) {
            searchHistory.remove(searchHistory.size() - 1);
        }
        
        // Update ComboBox items (no listeners since we're not using ObservableList)
        suppressFindSearch = true;
        try {
            String currentText = findField.getEditor().getText();
            findField.getItems().setAll(searchHistory);
            findField.getEditor().setText(currentText);
        } finally {
            suppressFindSearch = false;
        }
    }
    
    /**
     * Refresh highlights if they are stale due to editor changes.
     * Stops any pending timer and runs a highlight-only search (no cursor jump).
     * @return true if highlights were refreshed, false if they were not stale
     */
    private boolean refreshHighlightsIfStale() {
        if (highlightsStale) {
            editorChangeTimer.stop();
            runSearchHighlightOnly();
            highlightsStale = false;
            return true;
        }
        return false;
    }

    private void replaceOne() {
        if (lastMatches.isEmpty() || currentIndex < 0) {
            return;
        }
        String repl = replaceField.getText();
        int[] cur = lastMatches.get(currentIndex);
        // Replace current match
        dispArea.replaceText(cur[0], cur[1], repl == null ? "" : repl);
        // Re-run search to recompute matches (positions changed)
        runSearch();
    }

    private void replaceAll() {
        if (lastMatches.isEmpty()) {
            return;
        }
        String repl = replaceField.getText();
        // Replace all from end to start to preserve indices
        for (int i = lastMatches.size() - 1; i >= 0; i--) {
            int[] r = lastMatches.get(i);
            dispArea.replaceText(r[0], r[1], repl == null ? "" : repl);
        }
        runSearch();
    }
    
    /**
     * Update the cursor position in the status bar's custom section
     * @param area The ScriptArea to get cursor position from
     */
    private void updateCursorPosition(ScriptArea area) {
        if (handler instanceof EbsHandler) {
            StatusBar statusBar = ((EbsHandler) handler).getStatusBar();
            if (statusBar != null) {
                // Get current paragraph (row) and column
                int caretPos = area.getCaretPosition();
                int currentParagraph = area.getCurrentParagraph();
                int columnPos = area.getCaretColumn();
                
                // Format as (col,row) - using 1-based indexing for user display
                String position = String.format("(%d,%d)", columnPos + 1, currentParagraph + 1);
                statusBar.setCustom(position);
            }
        }
    }
    
    /**
     * Clean up all previous script setups before running the script.
     * This includes:
     * - Closing all open screens (windows)
     * - Closing database connections
     * - Clearing the environment (variables, call stack, opened files)
     * - Clearing the interpreter context (screen definitions, database connections)
     * - Clearing the runtime context (parsed statements and blocks)
     */
    private void cleanupBeforeRun() {
        try {
            if (handler instanceof EbsHandler) {
                EbsHandler ebsHandler = (EbsHandler) handler;
                
                // Get the interpreter and its context
                com.eb.script.interpreter.Interpreter interpreter = ebsHandler.getInterpreter();
                com.eb.script.interpreter.InterpreterContext interpreterContext = interpreter.getContext();
                
                // Close all open screens on the JavaFX Application Thread
                java.util.concurrent.ConcurrentHashMap<String, Stage> screens = interpreterContext.getScreens();
                if (!screens.isEmpty()) {
                    // Create a list of screen names to avoid concurrent modification
                    List<String> screenNames = new ArrayList<>(screens.keySet());
                    
                    // Use a CountDownLatch to ensure screens are closed before continuing
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    
                    Platform.runLater(() -> {
                        try {
                            for (String screenName : screenNames) {
                                Stage stage = screens.get(screenName);
                                if (stage != null && stage.isShowing()) {
                                    try {
                                        stage.close();
                                    } catch (Exception ex) {
                                        outputArea.printlnWarn("Warning: Error closing screen '" + screenName + "': " + ex.getMessage());
                                    }
                                }
                                
                                // Interrupt and stop the screen thread if it exists
                                Thread thread = interpreterContext.getScreenThreads().get(screenName);
                                if (thread != null && thread.isAlive()) {
                                    try {
                                        thread.interrupt();
                                    } catch (Exception ex) {
                                        // Ignore thread interruption errors
                                    }
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                    
                    // Wait for screen closing to complete (with timeout to prevent indefinite blocking)
                    try {
                        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Close all database connections before clearing
                // Create a copy of the connections to avoid concurrent modification
                java.util.Map<String, com.eb.script.interpreter.db.DbConnection> connections = interpreterContext.getConnections();
                List<com.eb.script.interpreter.db.DbConnection> connectionsList = new ArrayList<>(connections.values());
                for (com.eb.script.interpreter.db.DbConnection conn : connectionsList) {
                    try {
                        if (conn != null) {
                            conn.close();
                        }
                    } catch (Exception ex) {
                        // Ignore errors closing connections - they may already be closed
                    }
                }
                
                // Close all opened files before clearing the environment
                context.environment.closeAllOpenFiles();
                
                // Clear the interpreter context (screens, variables, etc.)
                // This will also clear connections and cursor specs
                interpreterContext.clear();
                
                // Clear the environment (variables, call stack, opened files)
                context.environment.clear();
                
                // Clear the runtime context (parsed statements and blocks)
                context.blocks.clear();
                context.statements = null;
            }
        } catch (Exception ex) {
            // Log the error but don't prevent the script from running
            outputArea.printlnWarn("Warning: Error during cleanup: " + ex.getMessage());
        }
    }
    
    /**
     * Get the handler for this tab
     * @return The handler
     */
    public Handler getHandler() {
        return handler;
    }
    
    /**
     * Handle Ctrl+Delete key press.
     * When cursor is on a space, delete all spaces onward.
     * When cursor is on text, delete the current word.
     * @param area The ScriptArea to operate on
     */
    private void handleCtrlDelete(ScriptArea area) {
        int caretPos = area.getCaretPosition();
        String text = area.getText();
        
        if (caretPos >= text.length()) {
            return; // At end of text, nothing to delete
        }
        
        char currentChar = text.charAt(caretPos);
        int deleteEnd = caretPos;
        
        if (Character.isWhitespace(currentChar)) {
            // Delete all spaces onward
            while (deleteEnd < text.length() && Character.isWhitespace(text.charAt(deleteEnd))) {
                deleteEnd++;
            }
        } else {
            // Delete the word (letters, digits, underscores)
            while (deleteEnd < text.length()) {
                char c = text.charAt(deleteEnd);
                if (Character.isLetterOrDigit(c) || c == '_') {
                    deleteEnd++;
                } else {
                    break;
                }
            }
        }
        
        if (deleteEnd > caretPos) {
            area.replaceText(caretPos, deleteEnd, "");
        }
    }
    
    /**
     * Handle Tab key press for indentation.
     * When multiple lines are selected, indent all of them.
     * When single line or no selection, return false to allow default behavior.
     * @param area The ScriptArea to operate on
     * @return true if event was handled (multiple lines indented), false for default behavior
     */
    private boolean handleTabIndent(ScriptArea area) {
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        
        if (selStart == selEnd) {
            return false; // No selection, use default tab behavior
        }
        
        String text = area.getText();
        
        // Find the start of the line containing selStart
        int lineStart = selStart;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Find the end of the line containing selEnd
        int lineEnd = selEnd;
        // If selection ends exactly at start of a line (not including that line), adjust back
        if (selEnd > 0 && text.charAt(selEnd - 1) == '\n') {
            lineEnd = selEnd - 1;
        }
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        
        // Check if this spans multiple lines
        boolean multipleLines = false;
        for (int i = lineStart; i < lineEnd; i++) {
            if (text.charAt(i) == '\n') {
                multipleLines = true;
                break;
            }
        }
        
        if (!multipleLines) {
            return false; // Single line, use default tab behavior
        }
        
        // Indent all lines in the selection
        String selectedText = text.substring(lineStart, lineEnd);
        String[] lines = selectedText.split("\n", -1);
        StringBuilder indented = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                indented.append('\n');
            }
            // Add tab at the beginning of each line (even empty lines)
            indented.append('\t').append(lines[i]);
        }
        
        // Replace the text
        area.replaceText(lineStart, lineEnd, indented.toString());
        
        // Restore selection to cover the indented text
        int newEnd = lineStart + indented.length();
        area.selectRange(lineStart, newEnd);
        
        return true; // Event handled
    }
    
    /**
     * Handle Enter key press for auto-indentation.
     * When Enter is pressed, insert a newline followed by the same indentation as the current line.
     * Only spaces and tabs are considered as indentation characters.
     * @param area The ScriptArea to operate on
     */
    private void handleAutoIndent(ScriptArea area) {
        int caretPos = area.getCaretPosition();
        String text = area.getText();
        
        // Guard against empty text
        if (text.isEmpty()) {
            area.insertText(caretPos, "\n");
            return;
        }
        
        // Find the start of the current line
        int lineStart = caretPos;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Determine the full indentation of the current line (scan from line start)
        int indentEnd = lineStart;
        while (indentEnd < text.length() && 
               (text.charAt(indentEnd) == ' ' || text.charAt(indentEnd) == '\t')) {
            indentEnd++;
        }
        
        // Extract the indentation characters
        String indentation = text.substring(lineStart, indentEnd);
        
        // Insert newline + indentation at caret position
        // The caret will automatically be positioned after the inserted text
        area.insertText(caretPos, "\n" + indentation);
    }
    
    /**
     * Handle Shift+Tab key press for unindenting/outdenting.
     * Removes one tab or up to 4 spaces from the beginning of each selected line.
     * @param area The ScriptArea to operate on
     */
    private void handleShiftTabUnindent(ScriptArea area) {
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        String text = area.getText();
        
        // Find the start of the line containing selStart
        int lineStart = selStart;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Find the end of the line containing selEnd
        int lineEnd = selEnd;
        // If selection ends exactly at start of a line (not including that line), adjust back
        if (selEnd > 0 && text.charAt(selEnd - 1) == '\n') {
            lineEnd = selEnd - 1;
        }
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        
        // Unindent all lines in the range
        String selectedText = text.substring(lineStart, lineEnd);
        String[] lines = selectedText.split("\n", -1);
        StringBuilder unindented = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                unindented.append('\n');
            }
            String line = lines[i];
            // Remove one tab or up to 4 spaces from the beginning
            if (line.startsWith("\t")) {
                unindented.append(line.substring(1));
            } else {
                // Remove up to 4 leading spaces
                int spacesToRemove = 0;
                for (int j = 0; j < Math.min(4, line.length()); j++) {
                    if (line.charAt(j) == ' ') {
                        spacesToRemove++;
                    } else {
                        break;
                    }
                }
                unindented.append(line.substring(spacesToRemove));
            }
        }
        
        // Replace the text
        area.replaceText(lineStart, lineEnd, unindented.toString());
        
        // Restore selection to cover the unindented text
        int newEnd = lineStart + unindented.length();
        area.selectRange(lineStart, newEnd);
    }
    
    /**
     * Handle Ctrl+G key press for go to line number.
     * Shows a dialog to enter a line number and jumps to that line.
     * @param area The ScriptArea to operate on
     */
    private void handleGoToLine(ScriptArea area) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Go to Line");
        dialog.setHeaderText("Enter line number:");
        dialog.setContentText("Line:");
        
        // Get current line for default value
        int currentLine = area.getCurrentParagraph() + 1;
        dialog.getEditor().setText(String.valueOf(currentLine));
        dialog.getEditor().selectAll();
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(lineStr -> {
            try {
                int lineNumber = Integer.parseInt(lineStr.trim());
                int totalLines = area.getParagraphs().size();
                
                // Validate line number
                if (lineNumber < 1) {
                    lineNumber = 1;
                } else if (lineNumber > totalLines) {
                    lineNumber = totalLines;
                }
                
                // Move to the line (0-indexed)
                int targetParagraph = lineNumber - 1;
                area.moveTo(targetParagraph, 0);
                area.requestFollowCaret();
                area.requestFocus();
            } catch (NumberFormatException ex) {
                // Invalid input, do nothing
            }
        });
    }
    
    /**
     * Handle Ctrl+/ key press for toggling line comments.
     * Adds // at the beginning of uncommented lines or removes it from commented lines.
     * @param area The ScriptArea to operate on
     */
    private void handleToggleLineComments(ScriptArea area) {
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        String text = area.getText();
        
        // Find the start of the line containing selStart
        int lineStart = selStart;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Find the end of the line containing selEnd (or selStart if no selection)
        int lineEnd = selEnd;
        // If selection ends exactly at start of a line (not including that line), adjust back
        if (selStart != selEnd && selEnd > 0 && selEnd < text.length() && text.charAt(selEnd - 1) == '\n') {
            lineEnd = selEnd - 1;
        }
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        
        // Get all lines in the range
        String selectedText = text.substring(lineStart, lineEnd);
        String[] lines = selectedText.split("\n", -1);
        
        // Check if all non-empty lines are commented
        boolean allCommented = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                allCommented = false;
                break;
            }
        }
        
        // Toggle comments
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            String line = lines[i];
            if (allCommented) {
                // Remove comment: find first occurrence of "//" and remove it
                int commentIdx = line.indexOf("//");
                if (commentIdx >= 0) {
                    result.append(line.substring(0, commentIdx));
                    // Skip the // and any single space after it
                    int afterComment = commentIdx + 2;
                    if (afterComment < line.length() && line.charAt(afterComment) == ' ') {
                        afterComment++;
                    }
                    if (afterComment < line.length()) {
                        result.append(line.substring(afterComment));
                    }
                } else {
                    result.append(line);
                }
            } else {
                // Add comment at the beginning (after any leading whitespace)
                int firstNonSpace = 0;
                while (firstNonSpace < line.length() && 
                       (line.charAt(firstNonSpace) == ' ' || line.charAt(firstNonSpace) == '\t')) {
                    firstNonSpace++;
                }
                result.append(line.substring(0, firstNonSpace));
                result.append("// ");
                result.append(line.substring(firstNonSpace));
            }
        }
        
        // Replace the text
        area.replaceText(lineStart, lineEnd, result.toString());
        
        // Restore selection to cover the modified text
        int newEnd = lineStart + result.length();
        area.selectRange(lineStart, newEnd);
    }
    
    /**
     * Handle Alt+Up key press for moving line(s) up.
     * Moves the current line or selected lines up by one line.
     * @param area The ScriptArea to operate on
     */
    private void handleMoveLineUp(ScriptArea area) {
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        String text = area.getText();
        
        // Find the start of the line containing selStart
        int lineStart = selStart;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Can't move up if already at first line
        if (lineStart == 0) {
            return;
        }
        
        // Find the end of the line containing selEnd
        int lineEnd = selEnd;
        // If selection ends exactly at start of a line (not including that line), adjust back
        if (selEnd > 0 && text.charAt(selEnd - 1) == '\n') {
            lineEnd = selEnd - 1;
        }
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        
        // Find the start of the previous line
        int prevLineStart = lineStart - 1; // Skip the newline before current line
        while (prevLineStart > 0 && text.charAt(prevLineStart - 1) != '\n') {
            prevLineStart--;
        }
        
        // Extract the lines
        String prevLine = text.substring(prevLineStart, lineStart); // Includes trailing newline
        String currentLines = text.substring(lineStart, lineEnd);
        
        // Check if currentLines needs a newline at the end
        boolean hasTrailingNewline = lineEnd < text.length() && text.charAt(lineEnd) == '\n';
        
        // Swap the lines
        String replacement = currentLines + (hasTrailingNewline ? "\n" : "") + prevLine;
        
        // Replace the text
        area.replaceText(prevLineStart, lineEnd + (hasTrailingNewline ? 1 : 0), replacement);
        
        // Update selection to follow the moved lines
        int offset = prevLineStart;
        int newSelStart = offset + (selStart - lineStart);
        int newSelEnd = offset + (selEnd - lineStart);
        area.selectRange(newSelStart, newSelEnd);
    }
    
    /**
     * Handle Alt+Down key press for moving line(s) down.
     * Moves the current line or selected lines down by one line.
     * @param area The ScriptArea to operate on
     */
    private void handleMoveLineDown(ScriptArea area) {
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        String text = area.getText();
        
        // Find the start of the line containing selStart
        int lineStart = selStart;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Find the end of the line containing selEnd
        int lineEnd = selEnd;
        // If selection ends exactly at start of a line (not including that line), adjust back
        if (selEnd > 0 && text.charAt(selEnd - 1) == '\n') {
            lineEnd = selEnd - 1;
        }
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        
        // Check if there's a newline after current selection
        boolean hasNewlineAfter = lineEnd < text.length() && text.charAt(lineEnd) == '\n';
        if (hasNewlineAfter) {
            lineEnd++; // Include the newline
        }
        
        // Can't move down if already at last line
        if (lineEnd >= text.length()) {
            return;
        }
        
        // Find the end of the next line
        int nextLineEnd = lineEnd;
        while (nextLineEnd < text.length() && text.charAt(nextLineEnd) != '\n') {
            nextLineEnd++;
        }
        
        // Check if there's a newline after the next line
        boolean nextHasNewline = nextLineEnd < text.length() && text.charAt(nextLineEnd) == '\n';
        if (nextHasNewline) {
            nextLineEnd++; // Include the newline
        }
        
        // Extract the lines
        String currentLines = text.substring(lineStart, lineEnd);
        String nextLine = text.substring(lineEnd, nextLineEnd);
        
        // Swap the lines
        String replacement = nextLine + currentLines;
        
        // Replace the text
        area.replaceText(lineStart, nextLineEnd, replacement);
        
        // Update selection to follow the moved lines
        int offset = lineStart + nextLine.length();
        int newSelStart = offset + (selStart - lineStart);
        int newSelEnd = offset + (selEnd - lineStart);
        area.selectRange(newSelStart, newSelEnd);
    }
    
    /**
     * Open the HTML content in a new WebView window.
     * Creates a new stage with a WebView that displays the current HTML content from the editor.
     * 
     * Note: This method loads the HTML content without sanitization since it's a developer tool
     * for previewing HTML that the user is actively editing. The user is intentionally viewing
     * their own content including any scripts, similar to how other HTML editors work.
     */
    private void openHtmlInWebView() {
        // Get the current HTML content from the editor
        String htmlContent = dispArea.getText();
        
        // Create a new Stage (window) for the WebView
        Stage webViewStage = new Stage();
        webViewStage.setTitle("HTML Preview - " + (filename != null ? filename : "untitled"));
        
        // Create a WebView
        WebView webView = new WebView();
        
        // Create a pin button to keep window always on top
        ToggleButton pinBtn = new ToggleButton("📌 Pin");
        pinBtn.setTooltip(new Tooltip("Keep window always on top"));
        pinBtn.setOnAction(e -> {
            webViewStage.setAlwaysOnTop(pinBtn.isSelected());
        });
        
        // Create auto-refresh toggle button with debounced updates (0.5 second delay)
        ToggleButton autoRefreshBtn = new ToggleButton("🔄 Auto Refresh");
        autoRefreshBtn.setTooltip(new Tooltip("Automatically refresh preview when editor changes"));
        
        // Get base URL for resolving relative paths (images, CSS, JS, etc.)
        // We need to inject a <base> tag into the HTML to set the base URL for relative paths
        String baseUrl = tabContext.path.getParent() != null 
            ? tabContext.path.getParent().toUri().toString() 
            : tabContext.path.toUri().toString();
        
        // Timer for debouncing editor changes
        PauseTransition refreshTimer = new PauseTransition(Duration.millis(500));
        refreshTimer.setOnFinished(e -> {
            // Refresh the WebView with current editor content
            String updatedContent = dispArea.getText();
            // Inject base tag if not present to resolve relative paths
            String contentWithBase = injectBaseTag(updatedContent, baseUrl);
            webView.getEngine().loadContent(contentWithBase, "text/html");
        });
        
        // Listener for editor text changes
        javafx.beans.value.ChangeListener<String> textChangeListener = (obs, oldText, newText) -> {
            if (autoRefreshBtn.isSelected()) {
                // Restart the timer on each change (debouncing)
                refreshTimer.playFromStart();
            }
        };
        
        // Apply custom styling when auto-refresh is toggled
        autoRefreshBtn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Blue background with white text when on
                autoRefreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                // Add the listener when enabled
                dispArea.textProperty().addListener(textChangeListener);
            } else {
                // Reset to default style when off
                autoRefreshBtn.setStyle("");
                // Stop any pending refresh
                refreshTimer.stop();
                // Remove the listener when disabled
                dispArea.textProperty().removeListener(textChangeListener);
            }
        });
        
        // Clean up when window closes
        webViewStage.setOnCloseRequest(e -> {
            refreshTimer.stop();
            dispArea.textProperty().removeListener(textChangeListener);
        });
        
        // Create a toolbar with the pin and auto-refresh buttons
        HBox toolbar = new HBox(5);
        toolbar.setPadding(new Insets(5));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(pinBtn, autoRefreshBtn);
        toolbar.getStyleClass().add("toolbar");
        
        // Create a StatusBar to show URLs when hovering over links
        StatusBar statusBar = new StatusBar();
        
        // Listen to WebEngine's status changed event to display hover URLs
        webView.getEngine().setOnStatusChanged(event -> {
            String status = event.getData();
            if (status != null && !status.isEmpty()) {
                // Show the URL in the status bar message section
                statusBar.setMessage(status);
            } else {
                // Clear the status bar when not hovering over a link
                statusBar.clearMessage();
            }
        });
        
        // Add load state listener for error detection
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.FAILED) {
                statusBar.setMessage("Failed to load HTML content");
            }
        });
        
        // Enable JavaScript console error logging
        webView.getEngine().setOnError(event -> {
            statusBar.setMessage("Error: " + event.getMessage());
        });
        
        // Load the HTML content with injected base tag for resolving relative paths (images, CSS, JS, etc.)
        String contentWithBase = injectBaseTag(htmlContent, baseUrl);
        webView.getEngine().loadContent(contentWithBase, "text/html");
        
        // Create a BorderPane layout with toolbar at top, WebView in center, and StatusBar at bottom
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(webView);
        root.setBottom(statusBar);
        
        // Create a scene with the layout (800x600 is a reasonable default, window is resizable)
        Scene scene = new Scene(root, 800, 600);
        webViewStage.setScene(scene);
        
        // Show the stage
        webViewStage.show();
    }
    
    /**
     * Opens markdown content in a WebView after converting to HTML.
     * Similar to openHtmlInWebView but converts markdown first using file.markdownToHtml builtin.
     */
    private void openMarkdownInWebView() {
        // Get the current markdown content from the editor
        String markdownContent = dispArea.getText();
        
        // Convert markdown to HTML using the builtin function
        String htmlContent;
        try {
            htmlContent = BuiltinsFile.markdownToHtml(context.environment, markdownContent, true);
        } catch (Exception ex) {
            outputArea.appendText("Error converting markdown to HTML: " + ex.getMessage() + "\n");
            return;
        }
        
        // Create a new Stage (window) for the WebView
        Stage webViewStage = new Stage();
        webViewStage.setTitle("Markdown Preview - " + (filename != null ? filename : "untitled"));
        
        // Create a WebView
        WebView webView = new WebView();
        
        // Create a pin button to keep window always on top
        ToggleButton pinBtn = new ToggleButton("📌 Pin");
        pinBtn.setTooltip(new Tooltip("Keep window always on top"));
        pinBtn.setOnAction(e -> {
            webViewStage.setAlwaysOnTop(pinBtn.isSelected());
        });
        
        // Create auto-refresh toggle button with debounced updates (0.5 second delay)
        ToggleButton autoRefreshBtn = new ToggleButton("🔄 Auto Refresh");
        autoRefreshBtn.setTooltip(new Tooltip("Automatically refresh preview when editor changes"));
        
        // Get base URL for resolving relative paths (images, CSS, JS, etc.)
        String baseUrl = tabContext.path.getParent() != null 
            ? tabContext.path.getParent().toUri().toString() 
            : tabContext.path.toUri().toString();
        
        // Timer for debouncing editor changes
        PauseTransition refreshTimer = new PauseTransition(Duration.millis(500));
        refreshTimer.setOnFinished(e -> {
            // Refresh the WebView with current editor content
            String updatedMarkdown = dispArea.getText();
            try {
                String updatedHtml = BuiltinsFile.markdownToHtml(context.environment, updatedMarkdown, true);
                // Inject base tag for resolving relative paths
                String contentWithBase = injectBaseTag(updatedHtml, baseUrl);
                webView.getEngine().loadContent(contentWithBase, "text/html");
            } catch (Exception ex) {
                // If conversion fails, show error in status bar (we'll add this later)
                System.err.println("Error converting markdown: " + ex.getMessage());
            }
        });
        
        // Listener for editor text changes
        javafx.beans.value.ChangeListener<String> textChangeListener = (obs, oldText, newText) -> {
            if (autoRefreshBtn.isSelected()) {
                // Restart the timer on each change (debouncing)
                refreshTimer.playFromStart();
            }
        };
        
        // Apply custom styling when auto-refresh is toggled
        autoRefreshBtn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Blue background with white text when on
                autoRefreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                // Add the listener when enabled
                dispArea.textProperty().addListener(textChangeListener);
            } else {
                // Reset to default style when off
                autoRefreshBtn.setStyle("");
                // Stop any pending refresh
                refreshTimer.stop();
                // Remove the listener when disabled
                dispArea.textProperty().removeListener(textChangeListener);
            }
        });
        
        // Clean up when window closes
        webViewStage.setOnCloseRequest(e -> {
            refreshTimer.stop();
            dispArea.textProperty().removeListener(textChangeListener);
        });
        
        // Create a toolbar with the pin and auto-refresh buttons
        HBox toolbar = new HBox(5);
        toolbar.setPadding(new Insets(5));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(pinBtn, autoRefreshBtn);
        toolbar.getStyleClass().add("toolbar");
        
        // Create a StatusBar to show URLs when hovering over links
        StatusBar statusBar = new StatusBar();
        
        // Listen to WebEngine's status changed event to display hover URLs
        webView.getEngine().setOnStatusChanged(event -> {
            String status = event.getData();
            if (status != null && !status.isEmpty()) {
                // Show the URL in the status bar message section
                statusBar.setMessage(status);
            } else {
                // Clear the status bar when not hovering over a link
                statusBar.clearMessage();
            }
        });
        
        // Add load state listener for error detection
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.FAILED) {
                statusBar.setMessage("Failed to load HTML content");
            }
        });
        
        // Enable JavaScript console error logging
        webView.getEngine().setOnError(event -> {
            statusBar.setMessage("Error: " + event.getMessage());
        });
        
        // Load the converted HTML content with injected base tag
        String contentWithBase = injectBaseTag(htmlContent, baseUrl);
        webView.getEngine().loadContent(contentWithBase, "text/html");
        
        // Create a BorderPane layout with toolbar at top, WebView in center, and StatusBar at bottom
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(webView);
        root.setBottom(statusBar);
        
        // Create a scene with the layout (800x600 is a reasonable default, window is resizable)
        Scene scene = new Scene(root, 800, 600);
        webViewStage.setScene(scene);
        
        // Show the stage
        webViewStage.show();
    }
    
    /**
     * Injects a <base> tag into HTML content to set the base URL for resolving relative paths.
     * If the HTML already has a <head> tag, the base tag is inserted at the beginning.
     * If there's no <head>, one is created with the base tag.
     * 
     * @param htmlContent The original HTML content
     * @param baseUrl The base URL to use for relative path resolution
     * @return HTML content with base tag injected
     */
    private String injectBaseTag(String htmlContent, String baseUrl) {
        String baseTag = "<base href=\"" + baseUrl + "\">";
        
        // Check if there's already a base tag
        if (htmlContent.toLowerCase().contains("<base")) {
            return htmlContent; // Already has a base tag
        }
        
        // Try to insert into existing <head> tag
        String lowerContent = htmlContent.toLowerCase();
        int headIndex = lowerContent.indexOf("<head");
        if (headIndex >= 0) {
            int headCloseIndex = htmlContent.indexOf(">", headIndex);
            if (headCloseIndex >= 0) {
                // Insert base tag right after <head>
                return htmlContent.substring(0, headCloseIndex + 1) + baseTag + htmlContent.substring(headCloseIndex + 1);
            }
        }
        
        // No <head> tag found, try to insert before <body> or at start
        int bodyIndex = lowerContent.indexOf("<body");
        if (bodyIndex >= 0) {
            // Insert <head> with base tag before <body>
            return htmlContent.substring(0, bodyIndex) + "<head>" + baseTag + "</head>" + htmlContent.substring(bodyIndex);
        }
        
        // No structure found, prepend base tag at the beginning
        return baseTag + htmlContent;
    }

}
