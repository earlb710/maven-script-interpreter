// package com.eb.ui.tabs;  // adjust to your project
package com.eb.ui.ebs;

import com.eb.ui.tabs.*;
import com.eb.script.RuntimeContext;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    
    // Minimum character count for find highlighting (more than 2 means at least 3)
    private static final int MIN_FIND_CHARS = 3;
    
    // Timer for debounced editor change re-highlighting (2 seconds)
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
            } else if (e.getCode() == KeyCode.TAB && !e.isControlDown() && !e.isShiftDown()) {
                // Tab: indent multiple lines if selected, normal behavior for single line
                if (handleTabIndent(dispArea)) {
                    e.consume();
                }
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
                return;
            }
            
            FileData ret = BuiltinsFile.readTextFile(tabContext.path.toString());
            tabContext.fileContext = ret.fileContext;
            suppressDirty = true;
            dispArea.replaceText(ret.stringData);
            outputArea.printlnOk(ret.fileContext.path.toString() + " : " + ret.fileContext.size);
            suppressDirty = false;
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

        if (isEbs) {
            dispArea.setEditable(true);               // enable edits for textual content
            dispArea.getStyleClass().add("editor-ebs");
            setupLexerHighlighting();             // <— hook the EbsLexer here
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
        btnNext = new Button("Next");
        btnReplace = new Button("Replace");
        btnReplaceAll = new Button("Replace All");
        btnClose = new Button("Close");
        lblCount = new Label("");

        findBar = new HBox(8, new Label("Find:"),
                findField, chkCase, chkWord, chkRegex,
                btnPrev, btnNext,
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

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> outputArea.clear());

        // Show the "start in" directory (script's parent directory)
        Path startInDir = tabContext.path != null ? tabContext.path.getParent() : null;
        String startInText = startInDir != null ? startInDir.toString() : System.getProperty("user.dir");
        Label startInLabel = new Label("Start in: " + startInText);
        startInLabel.getStyleClass().add("start-in-label");
        startInLabel.setMaxWidth(400); // Limit width to prevent layout issues with long paths
        startInLabel.setTooltip(new Tooltip("File operations use relative paths from this directory\n" + startInText));

        HBox buttons = new HBox(8, runBtn, clearBtn, startInLabel);
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
        StyleSpans<Collection<String>> spans = computeEbsHighlighting(text);
        // Apply to the area
        dispArea.setStyleSpans(0, spans);
    }

    private StyleSpans<Collection<String>> computeEbsHighlighting(String text) {
        Matcher m = EBS_PATTERN.matcher(text);
        int last = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        while (m.find()) {
            // gap (unstyled)
            builder.add(Collections.emptyList(), m.start() - last);

            String styleClass
                    = m.group("COMMENT") != null ? "tok-comment"
                    : m.group("STRING") != null ? "tok-string"
                    : m.group("NUMBER") != null ? "tok-number"
                    : m.group("KEYWORD") != null ? "tok-keyword"
                    : m.group("TYPE") != null ? "tok-type"
                    : m.group("BOOL") != null ? "tok-bool"
                    : m.group("NULL") != null ? "tok-null"
                    : m.group("BUILTIN") != null ? "tok-builtin"
                    : m.group("FUNCTION") != null ? "tok-function" : null;

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
        markDirty();  // Mark as dirty since it's a new unsaved file
    }

    // Setup find bar listeners once during initialization
    private void setupFindListeners() {
        // Initialize the editor change timer (2 second delay)
        editorChangeTimer = new PauseTransition(Duration.seconds(2));
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
        clearHighlights();
        findBar.setVisible(false);
        findBar.setManaged(false);
        lastMatches = java.util.Collections.emptyList();
        currentIndex = -1;
        // Stop any pending timer and reset stale flag
        if (editorChangeTimer != null) {
            editorChangeTimer.stop();
        }
        highlightsStale = false;
        // Reapply syntax highlighting to reset all text styling
        // This ensures any stale find highlights are cleared and text is properly re-highlighted
        applyLexerSpans(dispArea.getText());
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
        
        // Only highlight when there are more than 2 characters (at least 3)
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

        // Find the first match at or after the current position
        // Use selection start if there's a selection, otherwise use caret position
        int searchStartPos;
        if (dispArea.getSelection().getLength() > 0) {
            searchStartPos = dispArea.getSelection().getStart();
        } else {
            searchStartPos = dispArea.getCaretPosition();
        }
        
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

}
