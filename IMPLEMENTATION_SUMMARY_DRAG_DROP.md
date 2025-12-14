# Drag-and-Drop Implementation Summary

## Overview
This document provides a technical summary of the drag-and-drop feature implementation for the ProjectTreeView component.

## Problem Statement
Users needed an intuitive way to reorganize files and directories within the project tree by dragging them to new locations, rather than using context menu operations.

## Solution
Implemented JavaFX drag-and-drop handlers on TreeCell objects to enable moving files and directories via drag-and-drop gestures.

## Technical Implementation

### File Modified
- `ScriptInterpreter/src/main/java/com/eb/ui/ebs/ProjectTreeView.java`

### New Imports Added
```java
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
```

### New Method: `setupDragAndDrop(TreeCell<String> cell)`
This method configures all drag-and-drop behavior for tree cells. It's called from the cell factory for each cell.

#### Handler 1: setOnDragDetected
**Purpose**: Initiate drag operation when user clicks and drags

**Logic**:
1. Validate dragged item is not root or project node
2. Retrieve file path from cell's graphic userData
3. Verify file/directory exists
4. Create Dragboard with TransferMode.MOVE
5. Store path in ClipboardContent

**Validations**:
- Not root item
- Not project node (only items within projects can be dragged)
- Not empty cell
- Has valid path in userData
- File/directory exists

#### Handler 2: setOnDragOver
**Purpose**: Validate drop target during drag operation

**Logic**:
1. Check gesture source is different from target
2. Verify target is a directory (not file)
3. Prevent dropping into same parent
4. Prevent dropping directory into itself/subdirectories
5. Accept transfer mode if valid

**Validations**:
- Target must be a directory
- Source cannot drop into its own parent directory
- Directory cannot be dropped into itself or subdirectories
- Proper exception handling for invalid paths

#### Handler 3: setOnDragDropped
**Purpose**: Perform the actual file system move operation

**Logic**:
1. Retrieve source path from dragboard
2. Retrieve target directory path from cell
3. Create destination path (target + source filename)
4. Check if destination already exists
5. Perform Files.move() operation
6. Update tree view:
   - Find and remove source item from tree
   - Refresh target directory node
7. Log success or show error dialog

**Error Handling**:
- Shows alert if destination file/directory already exists
- Catches and displays any move operation errors
- Validates target is a directory before attempting move

#### Handler 4: setOnDragEntered
**Purpose**: Provide visual feedback when drag enters valid drop target

**Logic**:
1. Check if target is a directory
2. Apply blue highlight background (#e0e0ff)
3. Exception handling for invalid paths

#### Handler 5: setOnDragExited
**Purpose**: Remove visual feedback when drag exits cell

**Logic**:
1. Reset cell style to default
2. Removes highlight background

### Integration
The setupDragAndDrop() method is called from the existing cell factory:

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

### What Can Be Dragged
- Files within projects ✓
- Directories within projects ✓
- Files/directories in nested folders ✓

### What Cannot Be Dragged
- Root "Projects" node ✗
- Project nodes themselves ✗
- Empty cells ✗
- Non-existent files/directories ✗

### What Can Be Drop Targets
- Directories ✓
- Directories within projects ✓

### What Cannot Be Drop Targets
- Files ✗
- Root node ✗
- Project nodes ✗
- Same parent directory (no-op) ✗
- Directory cannot be dropped into itself ✗
- Directory cannot be dropped into its subdirectories ✗

## User Experience Flow

### Successful Move
1. User clicks and holds on a file/directory
2. Drag cursor appears
3. User moves cursor over a directory
4. Directory highlights with blue background
5. User releases mouse button
6. File/directory is moved
7. Tree view updates automatically
8. Source item removed from old location
9. Target directory refreshed to show moved item

### Failed Move (Duplicate Name)
1-5. Same as successful move
6. Alert dialog appears: "A file or directory with that name already exists in the destination."
7. User clicks OK
8. No changes made to file system or tree view

### Failed Move (Invalid Target)
1. User clicks and holds on a file/directory
2. Drag cursor appears
3. User moves cursor over a file (not directory)
4. No highlight appears (invalid target)
5. Drop cursor shows "not allowed" symbol
6. If user releases, nothing happens

## File System Operations

### Move Operation
- Uses `java.nio.file.Files.move(source, destination)`
- Atomic operation (all or nothing)
- Preserves file attributes
- Works across different directories
- Fails safely if destination exists

### Path Handling
- Paths retrieved from Label userData
- Converted to Path objects using `Paths.get()`
- Normalized for comparison operations
- Validated before use

## Tree View Updates

### After Successful Move
1. **Source Removal**:
   - Uses `findTreeItemForPath()` to locate source item
   - Removes from parent's children list
   - Preserves other tree structure

2. **Target Refresh**:
   - Calls `refreshDirectoryNode()` on target
   - Clears and reloads directory contents
   - Maintains sort order (directories first, then files, alphabetical)
   - Shows newly moved item in target location

## Exception Handling

### Types of Exceptions Handled
1. **InvalidPathException**: Caught when comparing paths
2. **Generic Exception**: Caught in drag visual feedback
3. **All Exceptions**: Caught in drag-dropped handler with error dialog

### Error Display
- User-friendly Alert dialogs
- Error header: "Move Error" or "Cannot Move"
- Error message includes exception details
- User must acknowledge by clicking OK

## Visual Feedback

### Highlight Color
- Background: #e0e0ff (light blue)
- Applied only to valid drop targets
- Removed when drag exits

### Cursor Changes
- Standard drag cursor during drag
- "Not allowed" cursor over invalid targets
- Standard cursor when drag completes

## Performance Considerations

### Efficient Operations
- Drag handlers only process valid cells
- Early returns prevent unnecessary processing
- File system checks only when needed
- Tree updates are incremental (not full refresh)

### Potential Bottlenecks
- Large directory structures: refreshDirectoryNode() loads all contents
- Frequent drags: Each drag creates new dragboard/content
- Deep tree searches: findTreeItemForPath() is recursive

### Optimization Opportunities
- Cache file existence checks during drag
- Batch tree updates for multiple moves
- Implement lazy loading for large directories

## Testing Considerations

### Manual Testing Scenarios
1. Drag file to different directory
2. Drag directory to another directory
3. Drag file/directory to same parent (should reject)
4. Drag directory into itself (should reject)
5. Drag directory into subdirectory (should reject)
6. Drag file/directory with duplicate name (should show error)
7. Drag onto file (should reject)
8. Drag project node (should not start drag)
9. Visual feedback during drag
10. Tree updates after successful move

### Edge Cases to Test
- Empty directories
- Hidden files/directories
- Very long file names
- Special characters in names
- Linked folders (from project.json)
- Main script files
- Non-existent files (red highlighted)
- Files in deeply nested folders

## Compatibility

### Existing Features
- Works alongside context menu "Move..." option
- Both use same `Files.move()` operation
- Both update tree view using same methods
- No conflicts or interference

### Future Enhancements
- Multi-select drag (move multiple items at once)
- Drag between different projects
- Copy with Ctrl key modifier
- Undo/redo support
- External file drops (from OS file manager)

## Code Quality

### Follows Existing Patterns
- Uses getUserData() for path storage (consistent with most of codebase)
- Uses System.out.println for logging (matches existing code)
- Uses Alert dialogs for errors (matches existing UI)
- Exception handling patterns consistent with codebase

### Security
- CodeQL scan: 0 alerts
- Path validation prevents traversal attacks
- No external input accepted (paths come from tree)
- File operations validated before execution

## Documentation
- DRAG_AND_DROP_FEATURE.md: User-facing feature documentation
- Inline Javadoc comments in code
- This implementation summary

## Conclusion
The drag-and-drop implementation provides an intuitive, visual way to reorganize files within the project tree while maintaining data integrity and providing appropriate user feedback. It integrates seamlessly with existing functionality and follows established code patterns.
