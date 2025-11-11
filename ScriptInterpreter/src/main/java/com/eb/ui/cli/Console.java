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
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
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
                .successionEnds(Duration.ofMillis(20))
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
        BorderPane content = new BorderPane(outputFrame);
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
        
        // Get only the current line for tokenization
        String currentLineText = getCurrentLineText(text, caretPos);
        int caretPosInLine = getCaretPositionInCurrentLine(text, caretPos);
        
        List<String> suggestions = AutocompleteSuggestions.getSuggestionsForContext(currentLineText, caretPosInLine);
        
        if (!suggestions.isEmpty()) {
            autocompletePopup.show(inputArea, suggestions);
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
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                wordStart--;
            } else {
                break;
            }
        }
        
        // Replace the current word with the suggestion
        inputArea.replaceText(wordStart, caretPos, suggestion);
        
        // Move caret to end of inserted text
        inputArea.moveTo(wordStart + suggestion.length());
    }
}
