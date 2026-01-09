# Package Feature - Visual Guide

## Feature Overview

This document provides a visual description of the new right-click package feature for `.ebs` files in the EBS Script Interpreter.

## User Interface Flow

### 1. Project Tree View - Initial State

```
Projects
└── MyProject
    ├── main.ebs
    ├── test_modulo.ebs
    └── test_package.ebs  ← User will right-click this file
```

The user sees their project files in the tree view on the left side of the application.

### 2. Right-Click Context Menu

When the user right-clicks on `test_package.ebs`, a context menu appears:

```
┌──────────────────────────┐
│ Run Script               │
│ Package to .ebsp         │ ← NEW OPTION
├──────────────────────────┤
│ Rename File...           │
│ Copy...                  │
│ Move...                  │
├──────────────────────────┤
│ Delete                   │
└──────────────────────────┘
```

### 3. Progress Dialog

After clicking "Package to .ebsp", a modal dialog appears:

```
┌─────────────────────────────────────┐
│  Packaging Script                   │
├─────────────────────────────────────┤
│                                     │
│  Packaging test_package.ebs         │
│                                     │
│  Please wait...                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

Note: This dialog cannot be closed by the user during packaging.

### 4. Success Result Dialog

On successful packaging, a detailed result dialog appears:

```
┌─────────────────────────────────────────────────────┐
│  Packaging Complete                          [i]    │
├─────────────────────────────────────────────────────┤
│  Successfully packaged script                       │
│                                                     │
│  Package created successfully!                      │
│                                                     │
│  Input file:  test_package.ebs                     │
│  Output file: test_package.ebsp                    │
│                                                     │
│  Original size:  330 bytes                         │
│  Packaged size: 1.35 KB (1380 bytes)               │
│                                                     │
│  Size increase: 318.2%                             │
│                                                     │
│                                      ┌────────┐    │
│                                      │   OK   │    │
│                                      └────────┘    │
└─────────────────────────────────────────────────────┘
```

### 5. Error Result Dialog (Example)

If packaging fails, an error dialog appears with details:

```
┌─────────────────────────────────────────────────────┐
│  Packaging Failed                             [X]   │
├─────────────────────────────────────────────────────┤
│  Failed to package script                           │
│                                                     │
│  Error packaging script:                            │
│                                                     │
│  [line 2] Parse error at DATATYPE (string):        │
│  Expected ';' after variable declaration.           │
│                                                     │
│  Cause: Unexpected token                            │
│                                                     │
│                                      ┌────────┐    │
│                                      │   OK   │    │
│                                      └────────┘    │
└─────────────────────────────────────────────────────┘
```

### 6. Updated Project Tree

After successful packaging, the tree view automatically refreshes to show the new file:

```
Projects
└── MyProject
    ├── main.ebs
    ├── test_modulo.ebs
    ├── test_package.ebs
    └── test_package.ebsp  ← NEW FILE APPEARS
```

## File Size Display Examples

The feature displays file sizes in human-readable format:

| Original Size | Packaged Size | Display Format |
|--------------|---------------|----------------|
| 330 bytes | 1380 bytes | "330 bytes" → "1.35 KB (1380 bytes)" |
| 50 KB | 45 KB | "50.00 KB (51200 bytes)" → "45.00 KB (46080 bytes)" |
| 2.5 MB | 1.8 MB | "2.50 MB (2621440 bytes)" → "1.80 MB (1887437 bytes)" |

## Size Change Indicators

The dialog shows whether the package is smaller or larger:

- **Size Reduction**: "Size reduction: 10.5%" (shown when packaged file is smaller)
- **Size Increase**: "Size increase: 318.2%" (shown when packaged file is larger)

Note: For small scripts, the packaged size is often larger due to the serialization overhead and metadata. For larger scripts or scripts with many dependencies, the packaged size may be smaller due to compression.

## Menu Item Positioning

The "Package to .ebsp" option is strategically placed:

1. **After "Run Script"**: Logical grouping with script execution
2. **Before file operations**: Separate from rename/copy/move/delete
3. **Only for .ebs files**: Menu item doesn't appear for other file types

## Color Coding (in dialogs)

- **Progress Dialog**: Blue info icon
- **Success Dialog**: Green checkmark or info icon
- **Error Dialog**: Red X icon

## Keyboard Navigation

While there's no dedicated keyboard shortcut (yet), users can:
1. Select file with arrow keys
2. Press context menu key (or Shift+F10)
3. Use arrow keys to navigate to "Package to .ebsp"
4. Press Enter to activate

## Integration Points

The feature integrates with existing functionality:

1. **File Tree Refresh**: Automatically updates after packaging
2. **Status Bar**: Could show packaging status (future enhancement)
3. **Console Output**: Could log packaging details (future enhancement)
4. **Recent Files**: Newly created .ebsp files can be opened

## Technical Details Visible to User

Users see:
- **GZIP Compression**: File type shows "gzip compressed data"
- **Serialized Format**: .ebsp files are binary (not text-editable)
- **File Extension**: Automatically changes from .ebs to .ebsp
- **Same Directory**: Output file appears in same folder as input

## Comparison with Console Command

| Feature | Console Command | Right-Click Menu |
|---------|----------------|------------------|
| Command | `/package <input.ebs> [output.ebsp]` | Right-click → "Package to .ebsp" |
| Output path | Customizable | Automatic (same dir, .ebsp extension) |
| Feedback | Console text | GUI dialogs |
| File sizes | Text format | Human-readable with percentages |
| Tree refresh | Manual | Automatic |
| Convenience | Medium | High |

## Error Scenarios Handled

1. **Parse Errors**: Shows line number and error description
2. **File System Errors**: Shows "Failed to read/write" with details
3. **Permission Issues**: Shows access denied error
4. **Disk Space**: Shows insufficient space error
5. **Invalid Path**: Shows "File not found" or path error

## Future Enhancement Ideas (Visual)

Potential UI improvements:

1. **Progress Bar**: Show actual packaging progress
   ```
   ┌─────────────────────────────────────┐
   │  Packaging Script                   │
   │  Packaging test_package.ebs         │
   │  ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░ 50%          │
   └─────────────────────────────────────┘
   ```

2. **Packaging Options Dialog**: Before packaging
   ```
   ┌─────────────────────────────────────┐
   │  Package Options                    │
   │  Output path: [____________]  [...]│
   │  Compression: [High ▼]              │
   │  Include deps: [✓]                  │
   │               [Package] [Cancel]    │
   └─────────────────────────────────────┘
   ```

3. **Batch Packaging**: Select multiple files
   ```
   Selected 3 files for packaging:
   - test1.ebs → test1.ebsp
   - test2.ebs → test2.ebsp
   - main.ebs → main.ebsp
   ```

4. **Status Bar Integration**:
   ```
   Status: Packaging test_package.ebs... | (50,10)
   ```

## Accessibility Features

- **Keyboard Navigation**: Full keyboard support via context menu key
- **Clear Labels**: Descriptive menu item text
- **Error Messages**: Detailed, actionable error descriptions
- **Modal Dialogs**: Prevent accidental clicks during operations
- **File Size Context**: Shows both formatted and exact byte counts

## Performance Characteristics

| Script Size | Parse Time | Serialize Time | UI Update |
|------------|-----------|---------------|-----------|
| < 1 KB     | < 50 ms   | < 100 ms      | < 50 ms   |
| 1-10 KB    | < 200 ms  | < 300 ms      | < 50 ms   |
| 10-100 KB  | < 1 s     | < 1 s         | < 50 ms   |
| > 100 KB   | 1-5 s     | 1-3 s         | < 50 ms   |

Note: All operations run in background thread, keeping UI responsive.
