# Right-Click Package Feature Documentation

## Overview

A new right-click context menu option has been added to the Project Tree View for `.ebs` files. This feature allows users to quickly package EBS scripts into binary `.ebsp` format directly from the file tree.

## Feature Details

### Location
- **Menu Item**: "Package to .ebsp"
- **Context**: Right-click context menu on `.ebs` files in the Project Tree View
- **Position**: Appears after "Run Script" and before the separator for file operations

### Functionality

When a user right-clicks on an `.ebs` file in the project tree and selects "Package to .ebsp":

1. **Progress Dialog**: A modal dialog appears showing "Packaging [filename]" with a "Please wait..." message
2. **Background Processing**: The packaging operation runs in a background thread to avoid blocking the UI
3. **Result Dialog**: After completion, a detailed dialog shows:
   - Success or failure status
   - Input filename
   - Output filename (automatically generated as `filename.ebsp`)
   - Original file size (in human-readable format)
   - Packaged file size (in human-readable format)
   - Size reduction/increase percentage
   - Error details if packaging fails

### Technical Implementation

**File**: `ProjectTreeView.java`

**New Methods Added**:
1. `packageScript(String scriptPath)` - Main packaging method
   - Parses the `.ebs` file using `Parser.parse()`
   - Serializes to `.ebsp` using `RuntimeContextSerializer.serialize()`
   - Displays progress and result dialogs
   - Refreshes the tree view to show the new `.ebsp` file
   
2. `formatFileSize(long size)` - Helper method
   - Formats file sizes in human-readable format (bytes, KB, MB)
   - Shows both formatted size and exact byte count for KB/MB

**Integration Points**:
- Context menu is dynamically created in `setupContextMenu()` method
- Menu item is only shown for files with `.ebs` extension
- Uses existing packaging infrastructure from `com.eb.script.package_tool` package

### Output File Location

The packaged `.ebsp` file is created in the same directory as the source `.ebs` file with the extension changed to `.ebsp`. For example:
- Input: `/path/to/script.ebs`
- Output: `/path/to/script.ebsp`

### Error Handling

The feature handles several error scenarios:
- Parse errors in the EBS script (shows error message with line number)
- File system errors (shows error details)
- I/O exceptions during packaging (shows exception message)

All errors are displayed in user-friendly alert dialogs with detailed information.

## Testing Results

### Manual Test Case

**Test Script**: `/tmp/test_package.ebs`
```ebs
// Simple test script for packaging
var message: string = "Hello from EBS packaged script!";
print(message);

// Math operations
var a: int = 10;
var b: int = 3;
var sum: int = a + b;
var diff: int = a - b;

print("a = " + a);
print("b = " + b);
print("sum = " + sum);
print("diff = " + diff);
print("Packaging test completed!");
```

**Test Results**:
```
Packaging multiple scripts (main: /tmp/test_package.ebs)
Parsing: /tmp/test_package.ebs
Packaging to: /tmp/test_package.ebsp
Original size: 330 bytes
Packaged size: 1380 bytes (1.35 KB)
Size increase: 318.2%
Successfully packaged to: /tmp/test_package.ebsp
```

**File Verification**:
- Input file: 330 bytes
- Output file: 1.4 KB (gzip compressed data)
- File type: Serialized RuntimeContext with GZIP compression

### Expected UI Flow (Description)

Since this is a headless environment, here's what the user experience would look like:

1. **Initial State**: User sees project tree with `.ebs` files
2. **Right-Click**: User right-clicks on `test_package.ebs`
3. **Context Menu**: Menu appears with options including:
   - Run Script
   - **Package to .ebsp** ← New option
   - (separator)
   - Rename File...
   - Copy...
   - Move...
   - (separator)
   - Delete

4. **Select Package**: User clicks "Package to .ebsp"
5. **Progress Dialog**: Modal dialog appears:
   ```
   Packaging Script
   Packaging test_package.ebs
   Please wait...
   ```

6. **Result Dialog** (on success):
   ```
   Packaging Complete
   Successfully packaged script
   
   Package created successfully!
   
   Input file:  test_package.ebs
   Output file: test_package.ebsp
   
   Original size:  330 bytes
   Packaged size: 1.35 KB (1380 bytes)
   
   Size increase: 318.2%
   ```

7. **Tree Refresh**: The project tree automatically refreshes to show the new `.ebsp` file

8. **Result Dialog** (on error):
   ```
   Packaging Failed
   Failed to package script
   
   Error packaging script:
   
   [line 2] Parse error at DATATYPE (string): Expected ';' after variable declaration.
   ```

## Code Changes Summary

### Modified Files
1. `ScriptInterpreter/src/main/java/com/eb/ui/ebs/ProjectTreeView.java`
   - Added "Package to .ebsp" menu item to file context menu (line ~608)
   - Added `packageScript()` method (lines 2471-2565)
   - Added `formatFileSize()` method (lines 2567-2578)

### Dependencies Used
- `com.eb.script.parser.Parser` - For parsing EBS scripts
- `com.eb.script.package_tool.RuntimeContextSerializer` - For serialization
- `java.nio.file.Files` - For file operations
- `javafx.scene.control.Alert` - For dialogs

## Future Enhancements

Potential improvements for this feature:
1. Allow user to specify output filename/location
2. Add option to package multiple files at once
3. Show packaging options (compression level, etc.)
4. Add a "Package All" option for project-level packaging
5. Add keyboard shortcut for packaging (e.g., Ctrl+Shift+P)
6. Show packaging progress bar for large scripts
7. Add history/log of packaged files

## Related Documentation

- `/package` console command in `EbsConsoleHandler.java`
- Packaging tool: `com.eb.script.package_tool.EbsPackager`
- Serialization: `com.eb.script.package_tool.RuntimeContextSerializer`
- User Guide: `PACKAGING_IMPLEMENTATION_SUMMARY.md`
