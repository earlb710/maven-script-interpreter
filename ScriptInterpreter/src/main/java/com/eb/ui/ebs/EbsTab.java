// package com.eb.ui.tabs;  // adjust to your project
package com.eb.ui.ebs;

import com.eb.ui.tabs.*;
import com.eb.script.RuntimeContext;
import com.eb.script.file.BuiltinsFile;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

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
    private TextField findField, replaceField;
    private CheckBox chkCase, chkWord, chkRegex;
    private Button btnNext, btnPrev, btnReplace, btnReplaceAll, btnClose;
    private Label lblCount;

    private List<int[]> lastMatches = java.util.Collections.emptyList(); // each int[]{start,endExclusive}
    private int currentIndex = -1;
    
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
            
            if (e.isControlDown() && e.getCode() == KeyCode.L) {
                dispArea.toggleLineNumbers();   // <— turns line numbers on/off
                e.consume();                    // prevent further handling of the keystroke
            } else if (e.getCode() == KeyCode.SPACE && e.isControlDown()) {
                // Autocomplete: Ctrl+Space
                showAutocomplete();
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
            if (e.isControlDown() && e.getCode() == KeyCode.L) {
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

        dispArea.multiPlainChanges().successionEnds(java.time.Duration.ofMillis(120));
    }

    private void loadFile(TabContext tabContext) {
        try {
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

        findField = new TextField();
        findField.setPromptText("Find");
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

        HBox buttons = new HBox(8, runBtn, clearBtn);
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

        // Re-highlight after pauses in typing
        // If you have ReactFX on classpath, this is ideal:
        // dispArea.multiPlainChanges()
        //        .successionEnds(Duration.ofMillis(120))
        //        .subscribe(ch -> applyEbsHighlighting(dispArea.getText()));
        //
        // Fallback: simple text listener (less efficient but OK for small files)
        dispArea.textProperty().addListener((obs, oldV, newV) -> applyEbsHighlighting(newV));
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

        // Re-highlight after changes (simple listener; you can throttle with ReactFX if present)
        dispArea.textProperty().addListener((obs, oldV, newV) -> applyLexerSpans(newV));
    }

    private void applyLexerSpans(String src) {
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
        dispArea.setStyleSpans(0, spans);
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

    // Setup find bar listeners once during initialization
    private void setupFindListeners() {
        // Live search when typing in find field
        findField.textProperty().addListener((obs, o, n) -> {
            Platform.runLater(() -> {
                runSearch();
            });
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
        
        // Button actions
        btnNext.setOnAction(e -> {
            Platform.runLater(() -> {
                gotoNext();
            });
        });
        btnPrev.setOnAction(e -> {
            Platform.runLater(() -> {
                gotoPrev();
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
        
        // Populate find field with current selection
        String selectedText = dispArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            findField.setText(selectedText);
        }
        
        Platform.runLater(() -> findField.requestFocus());
    }

    private void hideFind() {
        clearHighlights();
        findBar.setVisible(false);
        findBar.setManaged(false);
        lastMatches = java.util.Collections.emptyList();
        currentIndex = -1;
    }

    private void runSearch() {
        clearHighlights();
        currentIndex = -1;
        String q = findField.getText();
        if (q == null || q.isEmpty()) {
            lblCount.setText("");
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
     * Get the handler for this tab
     * @return The handler
     */
    public Handler getHandler() {
        return handler;
    }

}
