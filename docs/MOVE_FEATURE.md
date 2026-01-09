# Move Feature Implementation

This document describes the "Move" functionality added to the project tree view.

## Changes Made

### Context Menu Updates

#### For Files:
Previously:
```
Rename File...
Copy...
---
Delete
```

Now:
```
Rename File...
Copy...
Move...
---
Delete
```

#### For Directories (Regular):
Previously:
```
New File...
New Directory...
---
Rename Directory...
Delete Directory...
---
Refresh
```

Now:
```
New File...
New Directory...
---
Rename Directory...
Move...
Delete Directory...
---
Refresh
```

#### For Directories (Linked Folders):
Previously:
```
New File...
New Directory...
---
Rename Directory...
---
Remove from Project
---
Refresh
```

Now:
```
New File...
New Directory...
---
Rename Directory...
Move...
---
Remove from Project
---
Refresh
```

## Implementation Details

### moveFile() Method
- Opens a directory chooser dialog to select the destination directory
- Validates that the target file doesn't already exist
- Moves the file using `Files.move()`
- Updates the tree view by removing the file from the current location
- Refreshes the destination directory if it's visible in the tree

### moveDirectory() Method
- Opens a directory chooser dialog to select the destination directory
- Prevents moving a directory into itself or its subdirectories
- Validates that the target directory doesn't already exist
- Moves the directory recursively using `Files.move()`
- Updates the tree view by removing the directory from the current location
- Refreshes the destination directory if it's visible in the tree

## User Experience

1. **File Move**: Right-click on a file → Select "Move..." → Choose destination directory → File is moved
2. **Directory Move**: Right-click on a directory → Select "Move..." → Choose destination directory → Directory is moved

Both operations include proper error handling and user feedback through alert dialogs.
