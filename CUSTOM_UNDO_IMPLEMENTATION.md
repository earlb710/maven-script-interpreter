# Custom Undo Manager Implementation

## Overview
This document describes the custom undo manager implementation for the EBS script editor that solves the issue where undo operations were trying to undo syntax highlighting instead of actual text changes.

## Problem Statement
The original undo functionality in the script editor was unusable because:
1. When typing text, syntax highlighting is automatically applied by the lexer
2. RichTextFX's default UndoManager records both text changes AND style changes
3. Pressing Ctrl+Z would try to undo the last change, which was often the highlighting
4. This caused the cursor to jump to the top of the document
5. Same issue occurred with search/find highlighting

## Solution
Implemented a custom undo manager in `ScriptArea.java` that:
- Only tracks plain text changes (additions and deletions)
- Completely ignores style changes (syntax highlighting, search highlighting)
- Maintains proper cursor position after undo/redo operations
- Uses observable properties for UI integration

## Technical Implementation

### Architecture
```
ScriptArea extends StyleClassedTextArea
  ├─ Custom undo/redo stacks (LinkedList<TextChange>)
  ├─ Observable properties (undoAvailable, redoAvailable, performingAction)
  └─ CustomUndoManagerWrapper (compatibility wrapper)
```

### Key Components

#### 1. Text Change Recording
```java
// Subscribe only to plain text changes, ignore style changes
plainTextChanges()
    .filter(change -> !performingAction.get() && change.getNetLength() != 0)
    .subscribe(this::recordTextChange);
```

#### 2. TextChange Record
```java
private static record TextChange(int position, String removed, String inserted) {
}
```
- Stores the position, removed text, and inserted text
- Uses Java record for automatic equals(), hashCode(), and toString()

#### 3. Undo Operation
```java
public void undo() {
    TextChange change = undoStack.removeLast();
    performingAction.set(true);
    try {
        // Remove inserted text and restore removed text
        replaceText(change.position, 
                   change.position + change.inserted.length(), 
                   change.removed);
        // Position cursor at start of undone change
        selectRange(change.position, change.position);
        // Move to redo stack
        redoStack.add(change);
    } finally {
        performingAction.set(false);
    }
}
```

#### 4. Redo Operation
```java
public void redo() {
    TextChange change = redoStack.removeLast();
    performingAction.set(true);
    try {
        // Remove restored text and reinsert original
        replaceText(change.position, 
                   change.position + change.removed.length(), 
                   change.inserted);
        // Position cursor at end of redone change
        selectRange(newPos, newPos);
        // Move back to undo stack
        undoStack.add(change);
    } finally {
        performingAction.set(false);
    }
}
```

### Design Decisions

#### LinkedList vs Stack
- **Decision**: Use `LinkedList<TextChange>` instead of `Stack<TextChange>`
- **Reason**: Need efficient O(1) removal of oldest entries when history limit is reached
- **Alternative**: Stack would require O(n) operation for `remove(0)`

#### Observable Properties
- **Decision**: Use JavaFX `BooleanProperty` for state tracking
- **Reason**: Allows UI components to react to undo/redo availability changes
- **Properties**:
  - `undoAvailable`: True when undo stack is not empty
  - `redoAvailable`: True when redo stack is not empty
  - `performingAction`: True during undo/redo operations

#### History Limit
- **Decision**: Limit undo history to 1000 changes
- **Reason**: Prevent unbounded memory growth
- **Implementation**: Remove oldest entry when limit exceeded

#### Filtering Strategy
- **Decision**: Filter using `plainTextChanges()` instead of `richChanges()`
- **Reason**: `plainTextChanges()` only fires for text modifications, not style changes
- **Additional Filter**: Skip changes when `performingAction` is true to avoid recording undo/redo operations themselves

### Compatibility Layer
The `CustomUndoManagerWrapper` class implements `org.fxmisc.undo.UndoManager<Object>` to maintain compatibility with existing code that calls `getUndoManager()` or `forgetHistory()`.

Key wrapper methods:
- `undo()` / `redo()`: Delegate to ScriptArea's custom methods
- `undoAvailableProperty()` / `redoAvailableProperty()`: Wrap our observable properties
- `forgetHistory()`: Clear both stacks and update properties
- `getCurrentPosition()`: Return dummy position (not used in our implementation)

## Testing

### Manual Testing
See `ScriptInterpreter/scripts/test_undo.ebs` for detailed test procedures:

1. **Basic Undo/Redo**:
   - Type text → observe syntax highlighting applied
   - Press Ctrl+Z → text should be removed, highlighting updates automatically
   - Press Ctrl+Y → text should be restored

2. **Search Highlighting**:
   - Type text in editor
   - Press Ctrl+F and search for text → matches highlighted
   - Type more text
   - Press Ctrl+Z → should undo text, not search highlights

3. **Cursor Position**:
   - Type text at various positions
   - Press Ctrl+Z multiple times
   - Cursor should remain at the correct position (not jump to top)

4. **Multiple Operations**:
   - Type, delete, type more text
   - Press Ctrl+Z multiple times
   - Each undo should revert one logical text change

### Expected Behavior
- ✅ Undo only affects text content
- ✅ Syntax highlighting is ignored by undo
- ✅ Search highlighting is ignored by undo
- ✅ Cursor stays at correct position
- ✅ Redo works correctly
- ✅ History limit prevents memory issues
- ✅ Observable properties update correctly

## Security Review
- **CodeQL Scan**: No security issues found
- **Code Review**: All identified issues addressed

## Performance Considerations
- **Memory**: Limited to ~1000 changes × average change size
- **CPU**: O(1) for undo/redo operations
- **CPU**: O(1) for adding new changes (including history limit enforcement)
- **Subscription**: Event stream filtering is efficient (built into ReactFX)

## Future Enhancements
Possible improvements (not currently needed):
1. Smart change merging (merge consecutive single-character insertions)
2. Configurable history limit
3. Persistent undo history across sessions
4. Change grouping by time intervals

## References
- RichTextFX Documentation: https://github.com/FXMisc/RichTextFX
- ReactFX Event Streams: https://github.com/TomasMikula/ReactFX
- Java Records: https://docs.oracle.com/en/java/javase/21/language/records.html
