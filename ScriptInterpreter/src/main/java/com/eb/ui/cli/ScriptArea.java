package com.eb.ui.cli;

import com.eb.util.MarkupTokenizer;
import com.eb.util.Util;
import java.util.List;
import java.util.Stack;
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
    private final Stack<TextChange> undoStack = new Stack<>();
    private final Stack<TextChange> redoStack = new Stack<>();
    private boolean isUndoingOrRedoing = false;
    private static final int MAX_UNDO_HISTORY = 1000;
    
    // Observable properties for undo/redo state
    private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty redoAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty performingAction = new SimpleBooleanProperty(false);

    public ScriptArea() {
        setParagraphGraphicFactory(LineNumberFactory.get(this)); // initial state ON
        
        // Set up custom undo tracking that only monitors plain text changes
        // This ignores style changes (syntax highlighting, search highlighting, etc.)
        plainTextChanges()
            .filter(change -> !isUndoingOrRedoing && change.getNetLength() != 0)
            .subscribe(this::recordTextChange);
    }
    
    /**
     * Record a text change for undo/redo functionality.
     * Only plain text changes are recorded, style changes are ignored.
     */
    private void recordTextChange(PlainTextChange change) {
        // Clear redo stack when a new change is made
        redoStack.clear();
        redoAvailable.set(false);
        
        // Create a record of this change
        TextChange textChange = new TextChange(
            change.getPosition(),
            change.getRemoved(),
            change.getInserted()
        );
        
        undoStack.push(textChange);
        undoAvailable.set(true);
        
        // Limit undo history size to prevent memory issues
        if (undoStack.size() > MAX_UNDO_HISTORY) {
            undoStack.remove(0);
        }
    }
    
    /**
     * Undo the last text change.
     */
    @Override
    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        
        TextChange change = undoStack.pop();
        isUndoingOrRedoing = true;
        performingAction.set(true);
        
        try {
            // Undo: remove inserted text and restore removed text
            int start = change.position;
            int end = start + change.inserted.length();
            
            replaceText(start, end, change.removed);
            
            // Position cursor at the start of the undone change
            selectRange(start, start);
            
            // Push to redo stack
            redoStack.push(change);
            
            // Update observable properties
            undoAvailable.set(!undoStack.isEmpty());
            redoAvailable.set(true);
        } finally {
            isUndoingOrRedoing = false;
            performingAction.set(false);
        }
    }
    
    /**
     * Redo the last undone text change.
     */
    @Override
    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        
        TextChange change = redoStack.pop();
        isUndoingOrRedoing = true;
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
            undoStack.push(change);
            
            // Update observable properties
            undoAvailable.set(true);
            redoAvailable.set(!redoStack.isEmpty());
        } finally {
            isUndoingOrRedoing = false;
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
            return isUndoingOrRedoing;
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
