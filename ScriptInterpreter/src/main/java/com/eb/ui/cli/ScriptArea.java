package com.eb.ui.cli;

import com.eb.util.MarkupTokenizer;
import com.eb.util.Util;
import java.util.LinkedList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
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

    public ScriptArea() {
        setParagraphGraphicFactory(LineNumberFactory.get(this)); // initial state ON
        
        // Set up custom undo tracking that only monitors plain text changes
        // This ignores style changes (syntax highlighting, search highlighting, etc.)
        plainTextChanges()
            .filter(change -> !performingAction.get() && change.getNetLength() != 0)
            .subscribe(this::recordTextChange);
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
}
