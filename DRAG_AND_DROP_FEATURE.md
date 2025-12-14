# Drag-and-Drop Feature Implementation

This document describes the drag-and-drop functionality added to the project tree view, allowing users to move files and directories by dragging them to new locations.

## Overview

The project tree now supports drag-and-drop operations for files and directories within the project structure. This provides an intuitive way to reorganize project files without using context menu options.

## Features

### Drag Detection
- Users can click and drag any file or directory within a project
- Project nodes themselves cannot be dragged (only their contents)
- The root "Projects" node cannot be dragged
- Visual feedback starts immediately when dragging begins

### Drop Validation
- Files and directories can only be dropped onto directory items (not files)
- Directories cannot be dropped into themselves or their subdirectories
- Items cannot be dropped into their current parent directory (no-op move)
- Invalid drop targets are automatically rejected
- Visual feedback (highlight) shows valid drop targets

### Drop Operation
- The file/directory is moved to the target directory using `Files.move()`
- Duplicate name checking prevents overwriting existing files
- Tree view is automatically updated after successful move:
  - Source item is removed from its old location
  - Target directory is refreshed to show the moved item
- Error handling with user-friendly alert dialogs

## User Experience

### Moving a File
1. Click and hold on a file in the project tree
2. Drag the file over a directory (target directory highlights with blue background)
3. Release the mouse button to drop the file
4. The file is moved to the new directory and the tree updates

### Moving a Directory
1. Click and hold on a directory in the project tree
2. Drag the directory over another directory (target highlights)
3. Release to drop the directory
4. The directory and all its contents are moved to the new location

### Visual Feedback
- **Drag Start**: Cursor changes to indicate drag operation
- **Valid Target**: Directory highlights with light blue background (#e0e0ff)
- **Invalid Target**: No highlight, drop cursor shows operation not allowed
- **Drop Complete**: Tree view updates to reflect the new structure

## Implementation Details

### New Method: `setupDragAndDrop(TreeCell<String> cell)`

This method configures all drag-and-drop handlers for each tree cell:

#### 1. **setOnDragDetected**
- Validates that the item can be dragged (not root, not project, not empty)
- Verifies the file/directory exists
- Initiates drag with `TransferMode.MOVE`
- Stores the item path in the dragboard

#### 2. **setOnDragOver**
- Validates the drop target is a directory
- Prevents dropping into the same parent
- Prevents dropping directory into itself or subdirectories
- Accepts drag if valid, rejects otherwise

#### 3. **setOnDragDropped**
- Performs the actual file system move operation
- Checks for duplicate names in destination
- Updates tree view (removes from source, refreshes target)
- Shows error dialog if move fails
- Returns success/failure status

#### 4. **setOnDragEntered** / **setOnDragExited**
- Provides visual feedback (background color change)
- Highlights valid drop targets during drag
- Removes highlight when drag exits

### Integration

The drag-and-drop setup is integrated into the existing cell factory:
```java
treeView.setCellFactory(tv -> {
    TreeCell<String> cell = new TreeCell<>() {
        // ... existing cell update logic ...
    };
    
    // ... existing mouse click handler ...
    
    // Setup drag-and-drop handlers
    setupDragAndDrop(cell);
    
    return cell;
});
```

## Validation Rules

1. **Cannot drag project nodes**: Only files and directories within projects can be dragged
2. **Cannot drag root**: The "Projects" root node cannot be dragged
3. **Cannot drop on files**: Drop targets must be directories
4. **Cannot drop directory into itself**: Prevents infinite loops and invalid operations
5. **Cannot drop into same parent**: Prevents no-op moves
6. **Must not overwrite**: Duplicate names in destination are rejected with an error message

## Error Handling

All errors are handled gracefully with JavaFX Alert dialogs:
- **Duplicate Name**: "A file or directory with that name already exists in the destination."
- **Move Failure**: "Failed to move: [error message]"
- **Invalid Target**: Operation is rejected silently (no error dialog, just no drop cursor)

## Relationship with Context Menu

The drag-and-drop feature complements the existing context menu "Move..." options:
- **Context Menu Move**: Opens a directory chooser dialog for more control
- **Drag-and-Drop**: Quick, visual way to move within visible tree structure

Both methods perform the same underlying operation (`Files.move()`) and update the tree view in the same way.

## Technical Notes

### JavaFX Drag-and-Drop API
The implementation uses standard JavaFX drag-and-drop API:
- `Dragboard`: Contains data being transferred (file path as string)
- `TransferMode.MOVE`: Indicates a move operation (not copy)
- `ClipboardContent`: Wraps the dragged item's path

### Tree View Updates
After a successful move:
1. Source item is found using `findTreeItemForPath()` and removed
2. Target directory is refreshed using `refreshDirectoryNode()`
3. Both operations ensure the tree accurately reflects the file system

### File System Operations
- Uses `java.nio.file.Files.move()` for atomic move operations
- Verifies paths exist before and after operations
- Normalizes paths for comparison to prevent path traversal issues

## Testing Recommendations

To test the feature:
1. Create a test project with multiple files and directories
2. Try dragging files between directories
3. Try dragging directories into other directories
4. Test invalid operations (drag into file, drag into same parent, etc.)
5. Verify tree updates correctly after each move
6. Test error cases (duplicate names, permission issues)

## Future Enhancements

Potential improvements for future versions:
- Support for multi-select drag (move multiple items at once)
- Drag-and-drop between different projects
- Copy operation with Ctrl key (instead of always moving)
- Undo/redo support for drag-and-drop moves
- Drag-and-drop files from external file manager into project tree
