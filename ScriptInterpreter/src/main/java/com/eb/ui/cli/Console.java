package com.eb.ui.cli;

import com.eb.ui.ebs.EbsStyled;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.parser.ParseError;
import com.eb.script.token.ebs.EbsToken;
import com.eb.util.Util;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.model.StyleSpans;

/**
 * Console with styled output and a multiline, styled input area.
 *
 * The input area is a RichTextFX StyleClassedTextArea to allow multiline
 * editing. Default keybindings: • Enter inserts a newline • Ctrl+Enter submits
 * the whole buffer • Up/Down navigate history when the caret is at the
 * first/last line
 *
 * @author Earl Bosch
 */
public final class Console {

    private ScriptArea outputArea;
    private ScriptArea inputArea;
    private final Tab consoleTab;
    // history
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private final Handler handler;
    // autocomplete
    private final AutocompletePopup autocompletePopup;
    
    // Find bar components
    private HBox findBar;
    private TextField findField;
    private CheckBox chkCase, chkWord, chkRegex;
    private Button btnNext, btnPrev, btnClose;
    private Label lblCount;
    private List<int[]> lastMatches = java.util.Collections.emptyList();
    private int currentIndex = -1;
    private boolean suppressFindSearch = false;

    public Console(Handler handler) {
        this.handler = handler;
        this.autocompletePopup = new AutocompletePopup();
        consoleTab = buildConsoleTab();
        this.handler.setUI_outputArea(outputArea);
        hookSystemStreams();
        setupAutocomplete();
    }

    public Tab getConsoleTab() {
        return consoleTab;
    }

    public ScriptArea getOutputArea() {
        return outputArea;
    }

    public void requestFocus() {
        Platform.runLater(() -> inputArea.requestFocus());
    }

    /**
     * Redirect System.out and System.err to the TextArea.
     */
    public void hookSystemStreams() {
        try {
            System.setOut(new java.io.PrintStream(new StyledTextAreaOutputStream(outputArea, "info"), true, "UTF-8"));
            System.setErr(new java.io.PrintStream(new StyledTextAreaOutputStream(outputArea, "error"), true, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            printlnError(Util.formatExceptionWithOrigin(ex));
        }
    }

    private Tab buildConsoleTab() {
        // ---- Output area ----
        outputArea = new ScriptArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(false);
        outputArea.getStyleClass().add("console-out");

        var outputScroller = new VirtualizedScrollPane<>(outputArea);
        outputScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outputScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        StackPane outputFrame = new StackPane(outputScroller);
        outputFrame.getStyleClass().addAll("console-frame", "bevel-lowered");
        outputFrame.setPadding(new Insets(0));

        // ---- Find bar ----
        findField = new TextField();
        findField.setPromptText("Find");
        
        chkCase = new CheckBox("Aa");
        chkWord = new CheckBox("Word");
        chkRegex = new CheckBox("Regex");
        
        btnPrev = new Button("Prev");
        btnNext = new Button("Next");
        btnClose = new Button("Close");
        lblCount = new Label("");
        
        findBar = new HBox(8, new Label("Find:"),
                findField, chkCase, chkWord, chkRegex,
                btnPrev, btnNext,
                lblCount, btnClose);
        findBar.getStyleClass().add("find-bar");
        findBar.setVisible(false);
        findBar.setManaged(false);
        findBar.setAlignment(Pos.CENTER_LEFT);
        findBar.setFillHeight(false);
        
        setupFindListeners();

        // ---- Multiline input area (RichTextFX) ----
        inputArea = new ScriptArea();
        inputArea.getStyleClass().clear();
        //inputArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 13px; -fx-font-fill: #e6e6e6;");
        inputArea.getStyleClass().add("console-in");
        inputArea.setWrapText(true);
        inputArea.setUseInitialStyleForInsertion(true);
        inputArea.replaceText("");

        // Live syntax highlighting (debounced)
        inputArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(10))
                .subscribe(ignore -> applyInputHighlighting());
        applyInputHighlighting(); // initial

        var inputScroller = new VirtualizedScrollPane<>(inputArea);
        inputScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        inputScroller.setPrefHeight(90); // ~4-5 lines; tweak as needed

        StackPane inputFrame = new StackPane(inputScroller);
        inputFrame.getStyleClass().addAll("console-frame", "bevel-lowered");
        inputFrame.setPadding(new Insets(0));
        HBox.setHgrow(inputFrame, Priority.ALWAYS);

        // Buttons (optional)
        Button btnClear = new Button(" Clear ");
        btnClear.setOnAction(e -> outputArea.clear());
        Button btnSubmit = new Button("Submit");
        btnSubmit.setOnAction(e -> submitInputBuffer());
        Tooltip bt = new Tooltip("[control + enter]");
        bt.setShowDelay(javafx.util.Duration.millis(500));
        btnSubmit.setTooltip(bt);

        HBox bottom = new HBox(1, inputFrame, new Separator(Orientation.VERTICAL), new VBox(1, btnClear, btnSubmit));
        bottom.setPadding(new Insets(3));
        inputScroller.setMaxWidth(Double.MAX_VALUE);
        inputEvents();
        
        // Add cursor position tracking for console input area
        setupCursorTracking(inputArea);
        
        // Layout with find bar above output
        VBox top = new VBox(4, findBar, outputFrame);
        VBox.setVgrow(outputFrame, Priority.ALWAYS);
        
        BorderPane content = new BorderPane(top);
        content.setBottom(bottom);

        Tab t = new Tab("Console", content);
        t.setId("consoleTab");
        t.setClosable(false);                  // cannot close via tab UI
        t.setOnCloseRequest(ev -> ev.consume()); // belt & braces: block programmatic close too
        return t;
    }

    private void inputEvents() {
        // History navigation (only when at start/end of buffer)
// Key handling:
//   • Ctrl+Enter submits
//   • Ctrl+Up / Ctrl+Down navigate history (anywhere in buffer)
//   • Plain Up/Down behave normally (cursor movement)
//   • Ctrl+Space triggers autocomplete
//   • Ctrl+F opens find bar
        inputArea.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            // First check if autocomplete popup wants to handle the event
            if (autocompletePopup.handleKeyEvent(e)) {
                return;
            }

            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                submitInputBuffer();
                e.consume();
                return;
            }

            // Autocomplete: Ctrl+Space
            if (e.getCode() == KeyCode.SPACE && e.isControlDown()) {
                showAutocomplete();
                e.consume();
                return;
            }
            
            // Find: Ctrl+F
            if (e.getCode() == KeyCode.F && e.isControlDown()) {
                if (findBar.isVisible()) {
                    hideFind();
                } else {
                    showFind();
                }
                e.consume();
                return;
            }
            
            // Close find bar: Escape
            if (e.getCode() == KeyCode.ESCAPE && findBar.isVisible()) {
                hideFind();
                e.consume();
                return;
            }

            // History: Ctrl+Up / Ctrl+Down
            if (e.isControlDown() && e.getCode() == KeyCode.UP) {
                if (!history.isEmpty()) {
                    if (historyIndex < 0) {
                        historyIndex = history.size() - 1;
                    } else if (historyIndex > 0) {
                        historyIndex--;
                    }
                    inputArea.clear();
                    inputArea.replaceText(history.get(historyIndex));
                    inputArea.moveTo(inputArea.getLength());
                }
                e.consume();
                return;
            }

            if (e.isControlDown() && e.getCode() == KeyCode.DOWN) {
                if (!history.isEmpty()) {
                    if (historyIndex >= 0 && historyIndex < history.size() - 1) {
                        historyIndex++;
                        inputArea.clear();
                        inputArea.replaceText(history.get(historyIndex));
                    } else {
                        historyIndex = -1;
                        inputArea.clear();
                    }
                    inputArea.moveTo(inputArea.getLength());
                }
                e.consume();
                return;
            }

            // Otherwise: let JavaFX handle normal editing/navigation keys
        });

        // Hide autocomplete on specific key presses
        inputArea.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (autocompletePopup.isShowing()) {
                // Hide popup on: Space (not Ctrl+Space), Enter, Tab, or Backspace
                // Note: Ctrl+Space is used to trigger autocomplete, so don't hide on that
                if ((e.getCode() == KeyCode.SPACE && !e.isControlDown()) ||
                    e.getCode() == KeyCode.ENTER ||
                    e.getCode() == KeyCode.TAB ||
                    e.getCode() == KeyCode.BACK_SPACE) {
                    autocompletePopup.hide();
                }
            }
        });

        // Update autocomplete as user types
        inputArea.addEventHandler(KeyEvent.KEY_TYPED, e -> {
            if (autocompletePopup.isShowing()) {
                // Don't hide popup when typing dot (.)
                if (!".".equals(e.getCharacter())) {
                    // Update the suggestions based on the new text
                    showAutocomplete();
                }
            }
        });
        
        // Hide autocomplete when clicking in the editor (JavaFX Popup doesn't auto-hide on owner clicks)
        inputArea.setOnMouseClicked(e -> {
            if (autocompletePopup.isShowing()) {
                autocompletePopup.hide();
            }
        });
    }

// ---- Input syntax highlighting using Token.style --------------------------------
    /**
     * Re-tokenize input text and apply style spans using Token.style. This
     * assumes: tokens are non-overlapping and sorted by start position. If your
     * tokenizer returns overlapping tokens, resolve before building spans.
     */
    private void applyInputHighlighting() {
        String text = inputArea.getText();
        List<EbsToken> tokens = EbsStyled.tokenizeConsole(text);
        StyleSpans<Collection<String>> styles = EbsStyled.toStyleSpans(tokens, text.length(), 0);
        if (styles != null) {
            inputArea.setStyleSpans(0, styles);
        }
    }

    public void appendStyledText(String text) {
        EbsStyled.appendStyledText(outputArea, text);
    }

    /**
     * Submit the current buffer as lines to the handler, update history, clear
     * input.
     */
    private void submitInputBuffer() {
        String block = inputArea.getText();
        if (block == null) {
            block = "";
        }
        try {
            // de-duplicate last history entry
            if (history.isEmpty() || !history.get(history.size() - 1).trim().equals(block.trim())) {
                history.add(block);
            }
            historyIndex = -1;

            // split into lines for the varargs handler
            String[] lines = block.replace("\r\n", "\n").split("\n", -1);
            handler.submit(lines);
            inputArea.clear();
        } catch (ParseError | InterpreterError ex) {
            printlnWarn("Submitted : " + (block.length() > 80 ? block.substring(0, 78) + "…" : block));
            printlnError(Util.formatExceptionWithOrigin(ex));
        } catch (Exception ex) {
            printlnWarn("Submitted : " + (block.length() > 80 ? block.substring(0, 78) + "…" : block));
            printlnError(Util.formatExceptionWith2Origin(ex));
        }
    }

    public void submit(String... lines) throws Exception {
        handler.submit(lines);
    }

    // --- Output helpers ---
    public void println(String line) {
        outputArea.println(line);
    }

    public void printlnInfo(String s) {
        outputArea.printlnInfo(s);
    }

    public void printlnWarn(String s) {
        outputArea.printlnWarn(s);
    }

    public void printlnError(String s) {
        outputArea.printlnError(s);
    }

    public void printlnOk(String s) {
        outputArea.printlnOk(s);
    }

    void clear() {
        Platform.runLater(() -> {
            outputArea.clear();
            inputArea.clear();
        });
    }

    /**
     * Setup autocomplete callback to insert selected suggestion.
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
        String text = inputArea.getText();
        int caretPos = inputArea.getCaretPosition();
        
        // For JSON autocomplete, use the full text to maintain context
        // For other cases, just use current line
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
            autocompletePopup.show(inputArea, suggestions);
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
     * Insert the selected suggestion at the caret position,
     * replacing the current word being typed.
     */
    private void insertSuggestion(String suggestion) {
        String text = inputArea.getText();
        int caretPos = inputArea.getCaretPosition();
        
        // Find the start of the current word
        int wordStart = caretPos;
        while (wordStart > 0) {
            char c = text.charAt(wordStart - 1);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_'|| c == '/' || c == '"' || c == '#') {
                wordStart--;
            } else {
                break;
            }
        }
        
        // Check if we're inserting a JSON property key (which needs quotes)
        boolean isJsonKey = JsonSchemaAutocomplete.isSuggestingJsonKeys(text, caretPos);
        
        // Check if the suggestion is a builtin and get its parameter signature
        String paramSignature = null;
        if (AutocompleteSuggestions.isBuiltin(suggestion)) {
            paramSignature = AutocompleteSuggestions.getBuiltinParameterSignature(suggestion);
        }
        
        // Build the text to insert
        String insertText = suggestion;
        int finalCaretOffset = suggestion.length();
        
        if (isJsonKey) {
            // For JSON property keys, add quotes if not already present
            boolean hasOpeningQuote = wordStart > 0 && text.charAt(wordStart - 1) == '"';
            boolean hasClosingQuote = caretPos < text.length() && text.charAt(caretPos) == '"';
            
            if (!hasOpeningQuote) {
                insertText = "\"" + insertText;
                finalCaretOffset++;
            }
            if (!hasClosingQuote) {
                insertText = insertText + "\"";
            } else {
                // If there's already a closing quote, we'll position caret after it
                finalCaretOffset++;
            }
        } else if (paramSignature != null) {
            insertText = suggestion + paramSignature;
            // Position caret after the first = sign to allow user to type/replace the default value
            int firstEquals = paramSignature.indexOf("=");
            if (firstEquals >= 0) {
                finalCaretOffset = suggestion.length() + firstEquals + 1;
            }
        }
        
        // Replace the current word with the suggestion (and parameters if applicable)
        inputArea.replaceText(wordStart, caretPos, insertText);
        
        // Move caret to the appropriate position
        inputArea.moveTo(wordStart + finalCaretOffset);
    }
    
    /**
     * Setup cursor position tracking for the given ScriptArea
     * Updates the status bar's custom section with cursor position (col,row)
     */
    private void setupCursorTracking(ScriptArea area) {
        // Add cursor position listener
        area.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            updateCursorPosition(area);
        });
        
        // Also update on focus
        area.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                updateCursorPosition(area);
            }
        });
        
        // Initial update
        updateCursorPosition(area);
    }
    
    /**
     * Update the cursor position in the status bar's custom section
     */
    private void updateCursorPosition(ScriptArea area) {
        if (handler instanceof com.eb.ui.ebs.EbsHandler) {
            com.eb.ui.ebs.StatusBar statusBar = ((com.eb.ui.ebs.EbsHandler) handler).getStatusBar();
            if (statusBar != null) {
                // Get current paragraph (row) and column
                int currentParagraph = area.getCurrentParagraph();
                int columnPos = area.getCaretColumn();
                
                // Format as (col,row) - using 1-based indexing for user display
                String position = String.format("(%d,%d)", columnPos + 1, currentParagraph + 1);
                statusBar.setCustom(position);
            }
        }
    }
    
    // ---- Find bar methods ----
    
    private void setupFindListeners() {
        // Live search when typing in find field
        findField.textProperty().addListener((obs, o, n) -> {
            if (!suppressFindSearch) {
                Platform.runLater(() -> runSearch());
            }
        });
        
        // Re-search when checkboxes change
        chkCase.selectedProperty().addListener((obs, o, n) -> Platform.runLater(() -> runSearch()));
        chkWord.selectedProperty().addListener((obs, o, n) -> Platform.runLater(() -> runSearch()));
        chkRegex.selectedProperty().addListener((obs, o, n) -> Platform.runLater(() -> runSearch()));
        
        // Button actions
        btnNext.setOnAction(e -> Platform.runLater(() -> gotoNext()));
        btnPrev.setOnAction(e -> Platform.runLater(() -> gotoPrev()));
        btnClose.setOnAction(e -> Platform.runLater(() -> hideFind()));
        
        // Handle keyboard events on find bar
        findBar.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideFind();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                gotoNext();
                e.consume();
            }
        });
    }
    
    private void showFind() {
        findBar.setVisible(true);
        findBar.setManaged(true);
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

        String text = outputArea.getText();
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
            lblCount.setText("0 matches");
            return;
        }

        currentIndex = 0;
        int[] cur = hits.get(currentIndex);
        selectCurrent(cur);
        updateCountLabel();

        // Highlight all matches
        for (int[] r : hits) {
            outputArea.addStyleToRange(r[0], r[1], "find-hit");
        }
        // Emphasize current
        outputArea.addStyleToRange(cur[0], cur[1], "find-current");
    }
    
    private void selectCurrent(int[] r) {
        outputArea.selectRange(r[0], r[1]);
        outputArea.moveTo(r[1]);
        
        // Scroll to center the match in the viewport
        int paragraph = outputArea.getCurrentParagraph();
        int visibleParagraphs = outputArea.getVisibleParagraphs().size();
        int offset = visibleParagraphs / 2;
        int targetParagraph = Math.max(0, paragraph - offset);
        
        outputArea.showParagraphAtTop(targetParagraph);
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
        outputArea.addStyleToRange(cur[0], cur[1], "find-current");
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
        outputArea.addStyleToRange(cur[0], cur[1], "find-current");
    }
    
    private void clearCurrentEmphasis() {
        if (currentIndex >= 0 && currentIndex < lastMatches.size()) {
            int[] prev = lastMatches.get(currentIndex);
            outputArea.removeStyleFromRange(prev[0], prev[1], "find-current");
        }
    }
    
    private void updateCountLabel() {
        if (lastMatches.isEmpty()) {
            lblCount.setText("0 matches");
        } else {
            lblCount.setText((currentIndex + 1) + "/" + lastMatches.size() + " matches");
        }
    }
    
    private void clearHighlights() {
        for (int[] r : lastMatches) {
            outputArea.removeStyleFromRange(r[0], r[1], "find-hit");
            outputArea.removeStyleFromRange(r[0], r[1], "find-current");
        }
    }
    
    /**
     * Public method to show find bar from menu
     */
    public void showFindFromMenu() {
        if (findBar.isVisible()) {
            hideFind();
        } else {
            showFind();
        }
    }
}
