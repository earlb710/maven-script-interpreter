# New File and Add File Feature

## Overview
This feature adds "New File..." and "Add File..." options to the right-click context menu on projects in the Project Tree View.

## User Interface

### Project Context Menu
When right-clicking on a project in the Projects panel, users will now see:
```
┌─────────────────────┐
│ New File...         │
│ Add File...         │
├─────────────────────┤
│ Remove from List    │
└─────────────────────┘
```

### New File Dialog
When selecting "New File...", a dialog appears with:
- **File Type**: Dropdown with options:
  - EBS Script (.ebs)
  - JSON (.json)
  - CSS (.css)
  - Markdown (.md)
- **File Name**: Text field for the filename (extension added automatically)
- **Path**: Text field with the project directory path (with Browse... button)
- **Buttons**: Create, Cancel

### Add File Dialog
When selecting "Add File...", a standard file chooser appears with:
- File type filters for: EBS Scripts, JSON Files, CSS Files, Markdown Files, All Files
- Initial directory set to the project directory

## Behavior

### New File
1. Opens NewFileDialog with project directory as default path
2. User selects file type, enters name, and optionally changes path
3. Creates parent directories if they don't exist
4. Prompts for confirmation if file already exists
5. Creates file with appropriate default content:
   - EBS Script: `// EBS Script\n// Type your code here\n\n`
   - JSON: `{\n  \n}\n`
   - CSS: `/* CSS Styles */\n\n`
   - Markdown: `# Markdown Document\n\n`
6. Opens the new file in a tab for editing

### Add File
1. Opens file chooser dialog starting at project directory
2. User selects an existing file
3. File is added to recent files list
4. File is opened in a tab for editing

## Implementation Details

### Files Modified
- **ProjectTreeView.java**: Added context menu items
- **EbsConsoleHandler.java**: Added `createNewFile()` and `addExistingFile()` methods

### Files Created
- **NewFileDialog.java**: Dialog for creating new files with type selection

### Key Design Decisions
1. **Path Handling**: Extracts project directory from project.json path automatically
2. **File Opening**: Uses existing file infrastructure (FileContext, TabContext, Builtins.callBuiltin)
3. **Extension Handling**: Automatically appends extension based on selected type
4. **Directory Creation**: Creates parent directories if they don't exist
5. **Default Content**: Provides sensible default content for each file type

## Usage Example

1. Right-click on a project in the Projects panel
2. Select "New File..."
3. Choose "EBS Script" from the File Type dropdown
4. Enter "my-script" as the File Name
5. Path defaults to `/path/to/project/`
6. Click "Create"
7. File is created as `/path/to/project/my-script.ebs` and opened in a tab

## Future Enhancements
- Support for additional file types (XML, HTML, etc.)
- Template selection for different file types
- Recent files integration
- File organization within project structure
