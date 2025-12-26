# Button Icons Quick Reference

## Property

### Add Icon to Button
```javascript
{
    "name": "myButton",
    "display": {
        "type": "button",
        "labelText": "Button Text",
        "icon": "icons/icon-file.png"  // Icon path
    }
}
```

## Common Icon Paths

| Action | Icon Path | Usage |
|--------|-----------|-------|
| **File Operations** | | |
| New file | `icons/script-file.png` | Create new file |
| Open file | `icons/folder-open.png` | Open file dialog |
| Save file | `icons/save.png` | Save current file |
| Config | `icons/config-file.png` | Configuration/settings |
| **Navigation** | | |
| Folder | `icons/folder.png` | Browse folder |
| Project | `icons/project.png` | Project root |
| Favorite | `icons/folder_fav.png` | Favorites/bookmarks |
| **Content Types** | | |
| Script | `icons/script-file.png` | EBS script files |
| Markdown | `icons/markdown-file.png` | Documentation |
| Image | `icons/image-file.png` | Images |
| Text | `icons/text-file.png` | Text files |

## Quick Examples

### File Toolbar

```javascript
screen fileManager = {
    "areas": [
        {
            "name": "toolbar",
            "type": "hbox",
            "spacing": "5"
        }
    ],
    "vars": [
        {
            "name": "newBtn",
            "area": "toolbar",
            "display": {
                "type": "button",
                "labelText": "New",
                "icon": "icons/script-file.png",
                "shortcut": "Ctrl+N"
            }
        },
        {
            "name": "openBtn",
            "area": "toolbar",
            "display": {
                "type": "button",
                "labelText": "Open",
                "icon": "icons/folder-open.png",
                "shortcut": "Ctrl+O"
            }
        },
        {
            "name": "saveBtn",
            "area": "toolbar",
            "display": {
                "type": "button",
                "labelText": "Save",
                "icon": "icons/save.png",
                "shortcut": "Ctrl+S"
            }
        }
    ]
};
```

### Action Buttons

```javascript
// Refresh button with icon
{
    "name": "refreshBtn",
    "display": {
        "type": "button",
        "labelText": "Refresh",
        "icon": "icons/refresh.png",
        "shortcut": "F5"
    }
}

// Settings button with icon
{
    "name": "settingsBtn",
    "display": {
        "type": "button",
        "labelText": "Settings",
        "icon": "icons/settings.png"
    }
}

// Favorite button with icon
{
    "name": "favoriteBtn",
    "display": {
        "type": "button",
        "labelText": "Add to Favorites",
        "icon": "icons/folder_fav.png"
    }
}
```

### Combined with Event Handlers

```javascript
{
    "name": "saveBtn",
    "display": {
        "type": "button",
        "labelText": "Save",
        "icon": "icons/save.png",
        "shortcut": "Ctrl+S",
        "onClick": "call saveFile();"
    }
}
```

## Icon Specifications

- **Size**: Automatically scaled to 16x16 pixels
- **Format**: PNG (recommended for transparency)
- **Location**: `src/main/resources/icons/` for classpath
- **Path format**: `"icons/filename.png"`

## Path Options

### Classpath Resource (Recommended)
```javascript
"icon": "icons/save.png"
```

### File System Path
```javascript
"icon": "/home/user/icons/custom.png"
```

### Relative Path
```javascript
"icon": "./my-icons/icon.png"
```

## Common Patterns

### Create File Type Buttons
```javascript
// Group of file type buttons
var fileTypes = [
    { "name": "scriptBtn", "text": "Script", "icon": "icons/script-file.png" },
    { "name": "configBtn", "text": "Config", "icon": "icons/config-file.png" },
    { "name": "markdownBtn", "text": "Markdown", "icon": "icons/markdown-file.png" }
];
```

### Toolbar with Separators
```javascript
{
    "vars": [
        // File operations
        { "name": "newBtn", "icon": "icons/script-file.png", "text": "New" },
        { "name": "openBtn", "icon": "icons/folder-open.png", "text": "Open" },
        { "name": "sep1", "type": "separator" },
        // Edit operations  
        { "name": "cutBtn", "icon": "icons/cut.png", "text": "Cut" },
        { "name": "copyBtn", "icon": "icons/copy.png", "text": "Copy" }
    ]
}
```

## Features

- ✅ Icons display alongside button text
- ✅ Automatic scaling to 16x16px
- ✅ Works with keyboard shortcuts
- ✅ Works with onClick handlers
- ✅ Graceful fallback if icon missing
- ✅ Supports transparency (PNG alpha)
- ✅ Classpath and file system loading

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Icon not showing | Check path: `"icons/filename.png"` |
| Icon pixelated | Use 16x16px or higher resolution source |
| Path not found | Verify file exists in `src/main/resources/icons/` |
| Button too wide | Adjust area spacing or use shorter text |

## See Also

- [BUTTON_ICONS.md](BUTTON_ICONS.md) - Complete button icon documentation
- [TREEVIEW_ICONS_QUICKREF.md](TREEVIEW_ICONS_QUICKREF.md) - TreeView icon quick reference
- [BUTTON_SHORTCUTS.md](BUTTON_SHORTCUTS.md) - Button keyboard shortcuts
- [test_button_icons.ebs](test_button_icons.ebs) - Working demo script
