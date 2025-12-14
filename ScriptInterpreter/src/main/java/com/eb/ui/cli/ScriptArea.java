package com.eb.ui.cli;

import com.eb.util.MarkupTokenizer;
import com.eb.util.Util;
import java.util.LinkedList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.scene.input.MouseEvent;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.PlainTextChange;

/**
 *
 * @author Earl Bosch
 */
public class ScriptArea extends StyleClassedTextArea {

    private boolean showLineNumbers = true; // default: on
    
    // Custom undo/redo stacks that only track text changes
    // Using LinkedList for efficient removal of oldest entries
    private final LinkedList<TextChange> undoStack = new LinkedList<>();
    private final LinkedList<TextChange> redoStack = new LinkedList<>();
    private static final int MAX_UNDO_HISTORY = 1000;
    
    // Observable properties for undo/redo state
    private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty redoAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty performingAction = new SimpleBooleanProperty(false);
    
    // Change merging state for word-level undo
    private TextChange pendingChange = null;
    private long lastChangeTime = 0;
    private static final long MERGE_TIMEOUT_MS = 500; // Merge changes within 500ms
    
    // Bracket matching state
    private int[] lastHighlightedBrackets = null; // [openPos, closePos] or null if no brackets highlighted
    
    // Quote matching state
    private int[] lastHighlightedQuotes = null; // [openPos, closePos] or null if no quotes highlighted
    
    // Selection drag state - used to suppress bracket highlighting during selection
    private boolean isSelectionDragInProgress = false;

    public ScriptArea() {
        setParagraphGraphicFactory(LineNumberFactory.get(this)); // initial state ON
        
        // Set up custom undo tracking that only monitors plain text changes
        // This ignores style changes (syntax highlighting, search highlighting, etc.)
        plainTextChanges()
            .filter(change -> !performingAction.get() && change.getNetLength() != 0)
            .subscribe(this::recordTextChange);
        
        // Set up bracket matching on caret position changes
        // Skip highlighting if user is dragging to select text
        caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (!isSelectionDragInProgress) {
                highlightMatchingBrackets();
            }
        });
        
        // Track mouse press to detect start of selection drag
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            // Mouse press indicates potential selection start - suppress bracket highlighting
            isSelectionDragInProgress = true;
        });
        
        // Track mouse release to detect end of selection drag
        addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            // Mouse release ends selection drag - re-enable bracket highlighting
            isSelectionDragInProgress = false;
            // Now apply bracket highlighting at the final caret position
            highlightMatchingBrackets();
        });
    }
    
    /**
     * Record a text change for undo/redo functionality.
     * Only plain text changes are recorded, style changes are ignored.
     * Changes are merged together if they happen in quick succession at the same position.
     */
    private void recordTextChange(PlainTextChange change) {
        long currentTime = System.currentTimeMillis();
        
        // Clear redo stack when a new change is made
        redoStack.clear();
        redoAvailable.set(false);
        
        // Try to merge with pending change if possible
        if (pendingChange != null && canMerge(pendingChange, change, currentTime)) {
            // Merge the changes
            pendingChange = mergeChanges(pendingChange, change);
            lastChangeTime = currentTime;
        } else {
            // Commit any pending change before starting a new one
            if (pendingChange != null) {
                commitChange(pendingChange);
            }
            
            // Start a new pending change
            pendingChange = new TextChange(
                change.getPosition(),
                change.getRemoved(),
                change.getInserted()
            );
            lastChangeTime = currentTime;
        }
    }
    
    /**
     * Check if a new change can be merged with the pending change.
     * Changes can be merged if they happen within the timeout period and
     * represent continuous typing (insertions at adjacent positions) or
     * continuous deletion (backspace/delete at same position).
     */
    private boolean canMerge(TextChange pending, PlainTextChange newChange, long currentTime) {
        // Check timeout
        if (currentTime - lastChangeTime > MERGE_TIMEOUT_MS) {
            return false;
        }
        
        String newRemoved = newChange.getRemoved();
        String newInserted = newChange.getInserted();
        int newPos = newChange.getPosition();
        
        // Case 1: Continuous insertion (typing forward)
        // New text is inserted right after the pending text
        if (pending.removed.isEmpty() && newRemoved.isEmpty()) {
            // Check if inserting at the end of previous insertion
            if (newPos == pending.position + pending.inserted.length()) {
                // Don't merge if inserting a space or newline after non-whitespace
                // This creates word boundaries for better undo granularity
                if (!pending.inserted.isEmpty() && !newInserted.isEmpty()) {
                    char lastChar = pending.inserted.charAt(pending.inserted.length() - 1);
                    char newChar = newInserted.charAt(0);
                    // Break on word boundaries (space, newline, punctuation)
                    if (isWordBoundary(lastChar, newChar)) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        // Case 2: Continuous forward deletion (delete key)
        // Deleting at the same position repeatedly
        if (pending.inserted.isEmpty() && newInserted.isEmpty() && newPos == pending.position) {
            return true;
        }
        
        // Case 3: Continuous backward deletion (backspace)
        // Deleting before the previous deletion position
        if (pending.inserted.isEmpty() && newInserted.isEmpty()) {
            if (newPos + newRemoved.length() == pending.position) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if transition from one character to another represents a word boundary.
     */
    private boolean isWordBoundary(char lastChar, char newChar) {
        // Space or newline always creates a boundary
        if (Character.isWhitespace(newChar) || Character.isWhitespace(lastChar)) {
            return true;
        }
        // Transition between alphanumeric and punctuation
        boolean lastAlphaNum = Character.isLetterOrDigit(lastChar) || lastChar == '_';
        boolean newAlphaNum = Character.isLetterOrDigit(newChar) || newChar == '_';
        if (lastAlphaNum != newAlphaNum) {
            return true;
        }
        return false;
    }
    
    /**
     * Merge a new change with the pending change.
     */
    private TextChange mergeChanges(TextChange pending, PlainTextChange newChange) {
        String newRemoved = newChange.getRemoved();
        String newInserted = newChange.getInserted();
        int newPos = newChange.getPosition();
        
        // Continuous insertion
        if (pending.removed.isEmpty() && newRemoved.isEmpty() && 
            newPos == pending.position + pending.inserted.length()) {
            return new TextChange(
                pending.position,
                pending.removed,
                pending.inserted + newInserted
            );
        }
        
        // Forward deletion (delete key)
        if (pending.inserted.isEmpty() && newInserted.isEmpty() && newPos == pending.position) {
            return new TextChange(
                pending.position,
                pending.removed + newRemoved,
                pending.inserted
            );
        }
        
        // Backward deletion (backspace)
        if (pending.inserted.isEmpty() && newInserted.isEmpty() && 
            newPos + newRemoved.length() == pending.position) {
            return new TextChange(
                newPos,
                newRemoved + pending.removed,
                pending.inserted
            );
        }
        
        // Shouldn't reach here if canMerge returned true
        return pending;
    }
    
    /**
     * Commit a change to the undo stack.
     */
    private void commitChange(TextChange change) {
        undoStack.add(change);
        undoAvailable.set(true);
        
        // Limit undo history size to prevent memory issues
        // Remove from the front (oldest) when limit is exceeded
        if (undoStack.size() > MAX_UNDO_HISTORY) {
            undoStack.removeFirst();
        }
    }
    
    /**
     * Undo the last text change.
     */
    @Override
    public void undo() {
        // Commit any pending change before undoing
        if (pendingChange != null) {
            commitChange(pendingChange);
            pendingChange = null;
        }
        
        if (undoStack.isEmpty()) {
            return;
        }
        
        TextChange change = undoStack.removeLast();
        performingAction.set(true);
        
        try {
            // Undo: remove inserted text and restore removed text
            int start = change.position;
            int end = start + change.inserted.length();
            
            replaceText(start, end, change.removed);
            
            // Position cursor at the start of the undone change
            selectRange(start, start);
            
            // Push to redo stack
            redoStack.add(change);
            
            // Update observable properties
            undoAvailable.set(!undoStack.isEmpty());
            redoAvailable.set(true);
        } finally {
            performingAction.set(false);
        }
    }
    
    /**
     * Redo the last undone text change.
     */
    @Override
    public void redo() {
        // Commit any pending change before redoing
        if (pendingChange != null) {
            commitChange(pendingChange);
            pendingChange = null;
        }
        
        if (redoStack.isEmpty()) {
            return;
        }
        
        TextChange change = redoStack.removeLast();
        performingAction.set(true);
        
        try {
            // Redo: remove the text that was restored and insert the original text
            int start = change.position;
            int end = start + change.removed.length();
            
            replaceText(start, end, change.inserted);
            
            // Position cursor at the end of the redone change
            int newPos = start + change.inserted.length();
            selectRange(newPos, newPos);
            
            // Push back to undo stack
            undoStack.add(change);
            
            // Update observable properties
            undoAvailable.set(true);
            redoAvailable.set(!redoStack.isEmpty());
        } finally {
            performingAction.set(false);
        }
    }
    
    /**
     * Get the custom undo manager (for compatibility with existing code that calls forgetHistory()).
     * Returns a wrapper that implements the UndoManager interface.
     */
    @Override
    public org.fxmisc.undo.UndoManager<?> getUndoManager() {
        return new CustomUndoManagerWrapper();
    }
    
    /**
     * Wrapper class to provide UndoManager interface compatibility.
     */
    private class CustomUndoManagerWrapper implements org.fxmisc.undo.UndoManager<Object> {
        @Override
        public boolean undo() {
            if (!undoStack.isEmpty()) {
                ScriptArea.this.undo();
                return true;
            }
            return false;
        }
        
        @Override
        public boolean redo() {
            if (!redoStack.isEmpty()) {
                ScriptArea.this.redo();
                return true;
            }
            return false;
        }
        
        @Override
        public boolean isUndoAvailable() {
            return !undoStack.isEmpty();
        }
        
        @Override
        public org.reactfx.value.Val<Boolean> undoAvailableProperty() {
            return org.reactfx.value.Val.wrap(undoAvailable);
        }
        
        @Override
        public boolean isRedoAvailable() {
            return !redoStack.isEmpty();
        }
        
        @Override
        public org.reactfx.value.Val<Boolean> redoAvailableProperty() {
            return org.reactfx.value.Val.wrap(redoAvailable);
        }
        
        @Override
        public boolean isPerformingAction() {
            return performingAction.get();
        }
        
        @Override
        public javafx.beans.value.ObservableBooleanValue performingActionProperty() {
            return performingAction;
        }
        
        @Override
        public boolean isAtMarkedPosition() {
            return false;
        }
        
        @Override
        public javafx.beans.value.ObservableBooleanValue atMarkedPositionProperty() {
            // Create a constant property that never changes
            return new SimpleBooleanProperty(false);
        }
        
        @Override
        public org.fxmisc.undo.UndoManager.UndoPosition getCurrentPosition() {
            // Return a dummy position as we don't track positions in our simple implementation
            return new org.fxmisc.undo.UndoManager.UndoPosition() {
                @Override
                public void mark() {
                    // No-op
                }
                
                @Override
                public boolean isValid() {
                    return false;
                }
            };
        }
        
        @Override
        public org.reactfx.value.Val<Object> nextUndoProperty() {
            return org.reactfx.value.Val.constant(null);
        }
        
        @Override
        public org.reactfx.value.Val<Object> nextRedoProperty() {
            return org.reactfx.value.Val.constant(null);
        }
        
        @Override
        public void preventMerge() {
            // Not needed for our simple implementation
        }
        
        @Override
        public void forgetHistory() {
            pendingChange = null;  // Clear any pending change
            undoStack.clear();
            redoStack.clear();
            undoAvailable.set(false);
            redoAvailable.set(false);
        }
        
        @Override
        public void mark() {
            // Not needed for our simple implementation
        }
        
        @Override
        public void close() {
            forgetHistory();
        }
    }
    
    /**
     * Simple record of a text change for undo/redo.
     * Using a record provides automatic equals(), hashCode(), and toString() implementations.
     */
    private static record TextChange(int position, String removed, String inserted) {
    }

    /**
     * Toggle line numbers on/off.
     */
    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
        if (show) {
            setParagraphGraphicFactory(LineNumberFactory.get(this));
        } else {
            setParagraphGraphicFactory(null);
        }
    }

    /**
     * Current state for line numbers.
     */
    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    /**
     * Convenience toggle.
     */
    public void toggleLineNumbers() {
        setShowLineNumbers(!showLineNumbers);
    }

    public void print(String line) {
        Util.runOnFx(() -> {
            var segments = MarkupTokenizer.tokenize(line);
            if (segments.isEmpty()) {
                return;
            }
            for (var seg : segments) {
                if (seg.styles == null || seg.styles.isEmpty()) {
                    printStyled(seg.text, "info");
                } else {
                    printStyled(seg.text, seg.styles);
                }
            }
        });
    }

    public void println(String line) {
        Util.runOnFx(() -> {
            var segments = MarkupTokenizer.tokenize(line);
            if (segments.isEmpty()) {
                this.appendText("\n");
                return;
            }
            for (var seg : segments) {
                if (seg.styles == null || seg.styles.isEmpty()) {
                    printStyled(seg.text, "info");
                } else {
                    printStyled(seg.text, seg.styles);
                }
            }
            this.appendText("\n");
        });
    }

    public void printlnInfo(String s) {
        printStyled(s + "\n", "info");
    }

    public void printlnWarn(String s) {
        printStyled(s + "\n", "warn");
    }

    public void printlnError(String s) {
        printStyled(s + "\n", "error");
    }

    public void printlnOk(String s) {
        printStyled(s + "\n", "ok");
    }

    public void printStyled(String text, String... styleClasses) {
        printStyled(text, java.util.Arrays.asList(styleClasses));
    }

    public void printStyled(String text, List<String> styleClasses) {
        Util.runOnFx(() -> {
            int start = this.getLength();
            this.appendText(text);
            int end = start + text.length() + 1; // +1 to include trailing newline added by callers

            if (styleClasses == null || styleClasses.isEmpty()) {
                this.setStyleClass(start, end, "info");
            } else if (styleClasses.size() == 1) {
                String s = styleClasses.get(0);
                if (s.length() == 1) {
                    styleClasses.add("info");
                    this.setStyle(start, end, styleClasses);
                } else {
                    this.setStyleClass(start, end, s);
                }
            } else {
                styleClasses.add(0, "info");
                this.setStyle(start, end, styleClasses); // multi-class API
            }

            this.moveTo(this.getLength());
            this.requestFollowCaret();
        });
    }

    public void addStyleToRange(int start, int endExclusive, String styleClass) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(getLength(), endExclusive);
        
        if (actualStart >= actualEnd) {
            return;
        }
        
        // Use getStyleSpans to get all existing style spans in the range at once,
        // then modify them to add the style class and apply via setStyleSpans.
        // This is more efficient than per-character getStyleOfChar calls.
        org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> existingSpans = 
            getStyleSpans(actualStart, actualEnd);
        
        org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>> builder = 
            new org.fxmisc.richtext.model.StyleSpansBuilder<>();
        
        for (org.fxmisc.richtext.model.StyleSpan<java.util.Collection<String>> span : existingSpans) {
            java.util.Collection<String> existingStyles = span.getStyle();
            java.util.List<String> newStyles = new java.util.ArrayList<>(existingStyles);
            if (!newStyles.contains(styleClass)) {
                newStyles.add(styleClass);
            }
            builder.add(newStyles, span.getLength());
        }
        
        setStyleSpans(actualStart, builder.create());
    }

    public void removeStyleFromRange(int start, int endExclusive, String styleClass) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(getLength(), endExclusive);
        
        if (actualStart >= actualEnd) {
            return;
        }
        
        // Use getStyleSpans to get all existing style spans in the range at once,
        // then modify them to remove the style class and apply via setStyleSpans.
        // This is more efficient than per-character getStyleOfChar calls.
        org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> existingSpans = 
            getStyleSpans(actualStart, actualEnd);
        
        org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>> builder = 
            new org.fxmisc.richtext.model.StyleSpansBuilder<>();
        
        for (org.fxmisc.richtext.model.StyleSpan<java.util.Collection<String>> span : existingSpans) {
            java.util.Collection<String> existingStyles = span.getStyle();
            java.util.List<String> newStyles = new java.util.ArrayList<>(existingStyles);
            newStyles.removeIf(c -> c.equals(styleClass));
            builder.add(newStyles, span.getLength());
        }
        
        setStyleSpans(actualStart, builder.create());
    }
    
    /**
     * Highlight matching brackets and quotes when the caret is on or inside a pair.
     * Supports {}, (), [] brackets and single/double quotes.
     * This method is public to allow external triggering after syntax highlighting updates.
     */
    public void highlightMatchingBrackets() {
        // Clear previous highlights
        clearBracketHighlights();
        clearQuoteHighlights();
        
        String text = getText();
        int caretPos = getCaretPosition();
        
        if (text.isEmpty() || caretPos < 0) {
            return;
        }
        
        // Check if caret is on a bracket or quote
        char charAtCaret = (caretPos < text.length()) ? text.charAt(caretPos) : '\0';
        char charBeforeCaret = (caretPos > 0) ? text.charAt(caretPos - 1) : '\0';
        
        // Check for quotes first (they take precedence when on the character)
        // But skip escaped quotes
        if (isQuote(charAtCaret) && !isEscaped(text, caretPos)) {
            highlightQuotePair(text, caretPos, charAtCaret);
        } else if (isQuote(charBeforeCaret) && !isEscaped(text, caretPos - 1)) {
            // Also check the character before the caret (when cursor is after a quote)
            highlightQuotePair(text, caretPos - 1, charBeforeCaret);
        } else if (isOpenBracket(charAtCaret)) {
            // Try matching bracket at caret position
            highlightBracketPair(text, caretPos, charAtCaret, true);
        } else if (isCloseBracket(charAtCaret)) {
            highlightBracketPair(text, caretPos, charAtCaret, false);
        } else if (isOpenBracket(charBeforeCaret)) {
            // Also check the character before the caret (when cursor is after a bracket)
            highlightBracketPair(text, caretPos - 1, charBeforeCaret, true);
        } else if (isCloseBracket(charBeforeCaret)) {
            highlightBracketPair(text, caretPos - 1, charBeforeCaret, false);
        } else {
            // Caret is not on a bracket or quote - check if it's inside a bracket pair
            highlightEnclosingBrackets(text, caretPos);
        }
    }
    
    /**
     * Check if character is an opening bracket.
     */
    private boolean isOpenBracket(char c) {
        return c == '{' || c == '(' || c == '[';
    }
    
    /**
     * Check if character is a closing bracket.
     */
    private boolean isCloseBracket(char c) {
        return c == '}' || c == ')' || c == ']';
    }
    
    /**
     * Get the matching bracket for a given bracket.
     */
    private char getMatchingBracket(char bracket) {
        return switch (bracket) {
            case '{' -> '}';
            case '}' -> '{';
            case '(' -> ')';
            case ')' -> '(';
            case '[' -> ']';
            case ']' -> '[';
            default -> '\0';
        };
    }
    
    /**
     * Check if two brackets are a matching pair.
     */
    private boolean isMatchingPair(char open, char close) {
        return (open == '{' && close == '}') ||
               (open == '(' && close == ')') ||
               (open == '[' && close == ']');
    }
    
    /**
     * Highlight a bracket pair starting from a specific position.
     * @param text The text to search in
     * @param bracketPos The position of the bracket
     * @param bracket The bracket character
     * @param searchForward True if searching for closing bracket, false for opening
     */
    private void highlightBracketPair(String text, int bracketPos, char bracket, boolean searchForward) {
        int matchPos = searchForward 
            ? findMatchingCloseBracket(text, bracketPos, bracket)
            : findMatchingOpenBracket(text, bracketPos, bracket);
        
        if (matchPos != -1) {
            // Found matching bracket - highlight both with success color
            addStyleToRange(bracketPos, bracketPos + 1, "bracket-match");
            addStyleToRange(matchPos, matchPos + 1, "bracket-match");
            lastHighlightedBrackets = new int[]{
                Math.min(bracketPos, matchPos), 
                Math.max(bracketPos, matchPos)
            };
        } else {
            // No matching bracket - highlight with error color
            addStyleToRange(bracketPos, bracketPos + 1, "bracket-error");
            lastHighlightedBrackets = new int[]{bracketPos, bracketPos};
        }
    }
    
    /**
     * Find the matching closing bracket for an opening bracket.
     * @param text The text to search in
     * @param startPos The position of the opening bracket
     * @param openBracket The opening bracket character
     * @return The position of the matching closing bracket, or -1 if not found
     */
    private int findMatchingCloseBracket(String text, int startPos, char openBracket) {
        char closeBracket = getMatchingBracket(openBracket);
        int depth = 1;
        
        for (int i = startPos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == openBracket) {
                depth++;
            } else if (c == closeBracket) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1; // No matching bracket found
    }
    
    /**
     * Find the matching opening bracket for a closing bracket.
     * @param text The text to search in
     * @param startPos The position of the closing bracket
     * @param closeBracket The closing bracket character
     * @return The position of the matching opening bracket, or -1 if not found
     */
    private int findMatchingOpenBracket(String text, int startPos, char closeBracket) {
        char openBracket = getMatchingBracket(closeBracket);
        int depth = 1;
        
        for (int i = startPos - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == closeBracket) {
                depth++;
            } else if (c == openBracket) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1; // No matching bracket found
    }
    
    /**
     * Highlight the enclosing bracket pair when caret is inside brackets.
     * @param text The text to search in
     * @param caretPos The caret position
     */
    private void highlightEnclosingBrackets(String text, int caretPos) {
        // Find the nearest enclosing bracket pair
        int bestOpenPos = -1;
        int bestClosePos = -1;
        int bestDepth = Integer.MAX_VALUE;
        
        // Try each bracket type
        for (char openBracket : new char[]{'{', '(', '['}) {
            char closeBracket = getMatchingBracket(openBracket);
            
            // Find the closest opening bracket before caret
            for (int i = caretPos - 1; i >= 0; i--) {
                if (text.charAt(i) == openBracket) {
                    // Found an opening bracket, now find its matching close
                    int closePos = findMatchingCloseBracket(text, i, openBracket);
                    if (closePos != -1 && closePos >= caretPos) {
                        // This bracket pair encloses the caret
                        int depth = countNestedBrackets(text, i, closePos);
                        if (depth < bestDepth) {
                            bestDepth = depth;
                            bestOpenPos = i;
                            bestClosePos = closePos;
                        }
                        break; // Found the closest opening bracket of this type
                    }
                }
            }
        }
        
        if (bestOpenPos != -1 && bestClosePos != -1) {
            // Highlight the enclosing bracket pair
            addStyleToRange(bestOpenPos, bestOpenPos + 1, "bracket-match");
            addStyleToRange(bestClosePos, bestClosePos + 1, "bracket-match");
            lastHighlightedBrackets = new int[]{bestOpenPos, bestClosePos};
        }
    }
    
    /**
     * Count the nesting depth of brackets between two positions.
     * @param text The text to search in
     * @param startPos The start position
     * @param endPos The end position
     * @return The nesting depth
     */
    private int countNestedBrackets(String text, int startPos, int endPos) {
        int depth = 0;
        for (int i = startPos + 1; i < endPos; i++) {
            char c = text.charAt(i);
            if (isOpenBracket(c)) {
                depth++;
            }
        }
        return depth;
    }
    
    /**
     * Clear any previously highlighted brackets.
     */
    private void clearBracketHighlights() {
        if (lastHighlightedBrackets != null) {
            int openPos = lastHighlightedBrackets[0];
            int closePos = lastHighlightedBrackets[1];
            
            // Remove both match and error styles
            removeStyleFromRange(openPos, openPos + 1, "bracket-match");
            removeStyleFromRange(openPos, openPos + 1, "bracket-error");
            
            if (closePos != openPos) {
                removeStyleFromRange(closePos, closePos + 1, "bracket-match");
                removeStyleFromRange(closePos, closePos + 1, "bracket-error");
            }
            
            lastHighlightedBrackets = null;
        }
    }
    
    // ========== Quote Matching Methods ==========
    
    /**
     * Check if character is a quote.
     */
    private boolean isQuote(char c) {
        return c == '"' || c == '\'';
    }
    
    /**
     * Highlight a quote pair starting from a specific position.
     * Handles escaped quotes by skipping them.
     * @param text The text to search in
     * @param quotePos The position of the quote
     * @param quote The quote character (single or double)
     */
    private void highlightQuotePair(String text, int quotePos, char quote) {
        int matchPos = findMatchingQuote(text, quotePos, quote);
        
        if (matchPos != -1) {
            // Found matching quote - highlight both with success color
            addStyleToRange(quotePos, quotePos + 1, "quote-match");
            addStyleToRange(matchPos, matchPos + 1, "quote-match");
            lastHighlightedQuotes = new int[]{quotePos, matchPos};
        } else {
            // No matching quote - highlight with error color
            addStyleToRange(quotePos, quotePos + 1, "quote-error");
            lastHighlightedQuotes = new int[]{quotePos, quotePos};
        }
    }
    
    /**
     * Find the matching quote for a quote at a given position.
     * Skips escaped quotes (\' or \").
     * @param text The text to search in
     * @param startPos The position of the opening quote
     * @param quote The quote character to match
     * @return The position of the matching quote, or -1 if not found
     */
    private int findMatchingQuote(String text, int startPos, char quote) {
        // Determine if we're looking for the opening or closing quote
        // by checking if there's a quote before us that's not escaped
        boolean lookingForClose = isOpeningQuote(text, startPos, quote);
        
        if (lookingForClose) {
            // Search forward for closing quote
            for (int i = startPos + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == quote) {
                    // Check if this quote is escaped
                    if (!isEscaped(text, i)) {
                        return i;
                    }
                }
            }
        } else {
            // Search backward for opening quote
            for (int i = startPos - 1; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == quote) {
                    // Check if this quote is escaped
                    if (!isEscaped(text, i)) {
                        return i;
                    }
                }
            }
        }
        
        return -1; // No matching quote found
    }
    
    /**
     * Check if a quote at a given position is escaped.
     * A quote is escaped if it's preceded by an odd number of backslashes.
     * @param text The text to check in
     * @param pos The position of the quote
     * @return true if the quote is escaped, false otherwise
     */
    private boolean isEscaped(String text, int pos) {
        if (pos == 0) {
            return false;
        }
        
        // Count consecutive backslashes before the quote
        int backslashCount = 0;
        int i = pos - 1;
        while (i >= 0 && text.charAt(i) == '\\') {
            backslashCount++;
            i--;
        }
        
        // If there's an odd number of backslashes, the quote is escaped
        return backslashCount % 2 == 1;
    }
    
    /**
     * Determine if a quote at a given position is an opening quote.
     * This is done by counting unescaped quotes of the same type before it.
     * If the count is even, this is an opening quote.
     * @param text The text to check in
     * @param pos The position of the quote
     * @param quote The quote character
     * @return true if this is an opening quote, false if closing
     */
    private boolean isOpeningQuote(String text, int pos, char quote) {
        int count = 0;
        
        // Count unescaped quotes before this position
        for (int i = 0; i < pos; i++) {
            if (text.charAt(i) == quote && !isEscaped(text, i)) {
                count++;
            }
        }
        
        // If even number of quotes before, this is opening; if odd, this is closing
        return count % 2 == 0;
    }
    
    /**
     * Clear any previously highlighted quotes.
     */
    private void clearQuoteHighlights() {
        if (lastHighlightedQuotes != null) {
            int openPos = lastHighlightedQuotes[0];
            int closePos = lastHighlightedQuotes[1];
            
            // Remove both match and error styles
            removeStyleFromRange(openPos, openPos + 1, "quote-match");
            removeStyleFromRange(openPos, openPos + 1, "quote-error");
            
            if (closePos != openPos) {
                removeStyleFromRange(closePos, closePos + 1, "quote-match");
                removeStyleFromRange(closePos, closePos + 1, "quote-error");
            }
            
            lastHighlightedQuotes = null;
        }
    }
}
