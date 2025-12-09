# Project Files Feature - Visual Guide

## Tree View Structure

The tree view now displays files under each project with file-type-specific icons:

```
┌──────────────────────────────────────┐
│ Projects                             │
├──────────────────────────────────────┤
│ 📁 My Project                        │  <-- Project (folder icon)
│   📜 main.ebs                        │  <-- EBS Script (scroll icon)
│   📄 config.json                     │  <-- JSON file (document icon)
│   🎨 styles.css                      │  <-- CSS file (palette icon)
│   📖 README.md                       │  <-- Markdown (book icon)
│ 📁 Another Project                   │
│   📜 script.ebs                      │
└──────────────────────────────────────┘
```

## File Type Icons

Each file type has a unique icon for easy identification:

| File Type | Extension | Icon | Unicode |
|-----------|-----------|------|---------|
| EBS Script | .ebs | 📜 | \uD83D\uDCDC |
| JSON | .json | 📄 | \uD83D\uDCC4 |
| CSS | .css | 🎨 | \uD83C\uDFA8 |
| Markdown | .md | 📖 | \uD83D\uDCD6 |
| Project | (folder) | 📁 | \uD83D\uDCC1 |

## project.json Structure

When files are added to a project, they're stored in a "files" array:

```json
{
  "name": "My Project",
  "directory": "/path/to/project",
  "description": "EBS Script Project",
  "version": "1.0.0",
  "css": [
    "console.css"
  ],
  "mainScript": "main.ebs",
  "settings": {
    "autoLoad": true
  },
  "files": [
    "main.ebs",
    "config.json",
    "styles.css",
    "README.md"
  ]
}
```

## Workflow

### Creating a New File

1. Right-click on "My Project" → Select "New File..."
2. Dialog appears:
   - Type: EBS Script (dropdown)
   - Name: calculator
   - Path: /path/to/project
3. Click "Create"
4. Results:
   - File created: `/path/to/project/calculator.ebs`
   - Added to project.json: `"files": ["calculator.ebs"]`
   - Appears in tree view: 📜 calculator.ebs
   - Opens in editor automatically

### Adding an Existing File

1. Right-click on "My Project" → Select "Add File..."
2. File chooser opens at project directory
3. Select existing file (e.g., helper.ebs)
4. Results:
   - Added to project.json: `"files": ["calculator.ebs", "helper.ebs"]`
   - Appears in tree view: 📜 helper.ebs
   - Opens in editor automatically

### Opening a File from Tree View

1. Double-click on 📜 calculator.ebs in tree view
2. File opens in editor tab
3. Added to recent files list

## Features

✅ Files automatically added to project.json
✅ Files displayed in tree view with type-specific icons
✅ Projects auto-expand to show files
✅ Double-click to open files
✅ Tooltips show full file paths
✅ Relative paths used when possible
✅ Duplicate prevention (files only added once)

## Technical Details

- File paths stored relative to project directory
- Supports both ArrayDynamic and standard List for compatibility
- Tree view refreshes automatically when files are added
- Files persist across application sessions (stored in project.json)
