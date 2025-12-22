# Visual Guide: New File and Add File Feature

## Projects Panel - Right-Click Context Menu

### Before (Original)
```
┌──────────────────────────┐
│ Projects                 │
├──────────────────────────┤
│ 📁 My Project            │  <-- Right-click here
│ 📁 Another Project       │
│ 📁 Test Project          │
└──────────────────────────┘

Context Menu (Right-click on a project):
┌─────────────────────┐
│ Remove from List    │
└─────────────────────┘
```

### After (With New Feature)
```
┌──────────────────────────┐
│ Projects                 │
├──────────────────────────┤
│ 📁 My Project            │  <-- Right-click here
│ 📁 Another Project       │
│ 📁 Test Project          │
└──────────────────────────┘

Context Menu (Right-click on a project):
┌─────────────────────┐
│ New File...         │  <-- NEW
│ Add File...         │  <-- NEW
├─────────────────────┤
│ Remove from List    │
└─────────────────────┘
```

## New File Dialog

```
┌──────────────────────────────────────────────────────┐
│ New File                                             │
├──────────────────────────────────────────────────────┤
│                                                      │
│  File Type:  [EBS Script ▼]                         │
│              - EBS Script (.ebs)                     │
│              - JSON (.json)                          │
│              - CSS (.css)                            │
│              - Markdown (.md)                        │
│                                                      │
│  File Name:  [my-script                  ]          │
│              (extension .ebs will be added)          │
│                                                      │
│  Path:       [/home/user/projects/      ] [Browse...]│
│                                                      │
│                              [Create] [Cancel]       │
└──────────────────────────────────────────────────────┘
```

## Add File Dialog (Standard File Chooser)

```
┌──────────────────────────────────────────────────────┐
│ Add Existing File                                    │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Look in: [/home/user/projects/ ▼]                  │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │ 📄 main.ebs                                    │ │
│  │ 📄 config.json                                 │ │
│  │ 📄 styles.css                                  │ │
│  │ 📄 README.md                                   │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  File name: [                           ]           │
│  File type: [EBS Scripts (*.ebs) ▼]                 │
│              - EBS Scripts (*.ebs)                   │
│              - JSON Files (*.json)                   │
│              - CSS Files (*.css)                     │
│              - Markdown Files (*.md)                 │
│              - All Files (*.*)                       │
│                                                      │
│                              [Open] [Cancel]         │
└──────────────────────────────────────────────────────┘
```

## Workflow Example

### Creating a New EBS Script

1. User right-clicks on "My Project" in the Projects panel
2. Selects "New File..." from the context menu
3. NewFileDialog opens with:
   - File Type: "EBS Script" (default)
   - File Name: (empty, ready for input)
   - Path: "/home/user/projects/my-project" (auto-filled)
4. User types "calculator" as File Name
5. User clicks "Create"
6. System creates `/home/user/projects/my-project/calculator.ebs` with:
   ```
   // EBS Script
   // Type your code here
   
   ```
7. File opens in a new tab for editing

### Adding an Existing File

1. User right-clicks on "My Project" in the Projects panel
2. Selects "Add File..." from the context menu
3. File chooser opens at "/home/user/projects/my-project"
4. User navigates and selects "helper.ebs"
5. User clicks "Open"
6. File opens in a new tab for editing
7. File is added to recent files list

## Integration with Existing Features

The new files are seamlessly integrated with existing features:
- Files open in the standard EbsTab editor
- Files are tracked in the file management system
- Recent files list is updated automatically
- Same file handling infrastructure as File → Open command
- Syntax highlighting and editing features work immediately

## Benefits

1. **Convenience**: Quickly create files within project context
2. **Type Safety**: Enforced file extensions and default content
3. **Project Organization**: Files created in correct project directory
4. **User-Friendly**: Intuitive dialogs with sensible defaults
5. **Consistency**: Uses existing file opening infrastructure
