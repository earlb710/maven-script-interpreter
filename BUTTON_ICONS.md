# Button Icon Support

## Overview

Screen button items now support displaying icons alongside button text. This feature follows the same icon loading pattern used by TreeView items, supporting both classpath resources and file system paths.

## Features

- **Static icon property**: Specify an icon path in the button's display metadata
- **Flexible loading**: Load icons from classpath resources (e.g., `icons/*.png`) or file system paths
- **Automatic scaling**: Icons are automatically scaled to 16x16 pixels to fit button size
- **Graceful error handling**: Missing icon files are silently ignored without breaking the button
- **Combined with text**: Icons appear alongside button text for better visual clarity

## Usage

### Basic Example

```javascript
{
    "name": "saveButton",
    "type": "string",
    "area": "toolbarArea",
    "display": {
        "type": "button",
        "labelText": "Save",
        "icon": "icons/save.png",
        "onClick": "call saveFile();"
    }
}
```

### With Keyboard Shortcuts

Icons work seamlessly with keyboard shortcuts:

```javascript
{
    "name": "openButton",
    "type": "string",
    "area": "toolbarArea",
    "display": {
        "type": "button",
        "labelText": "Open",
        "icon": "icons/folder-open.png",
        "shortcut": "Ctrl+O",
        "onClick": "call openFile();"
    }
}
```

### Complete Screen Example

```javascript
screen fileManager = {
    "title": "File Manager",
    "width": 600,
    "height": 400,
    "areas": [
        {
            "name": "toolbarArea",
            "type": "hbox",
            "spacing": "10",
            "padding": "10"
        }
    ],
    "vars": [
        {
            "name": "newButton",
            "type": "string",
            "area": "toolbarArea",
            "display": {
                "type": "button",
                "labelText": "New",
                "icon": "icons/script-file.png",
                "shortcut": "Ctrl+N",
                "onClick": "call createNewFile();"
            }
        },
        {
            "name": "openButton",
            "type": "string",
            "area": "toolbarArea",
            "display": {
                "type": "button",
                "labelText": "Open",
                "icon": "icons/folder-open.png",
                "shortcut": "Ctrl+O",
                "onClick": "call openFile();"
            }
        },
        {
            "name": "saveButton",
            "type": "string",
            "area": "toolbarArea",
            "display": {
                "type": "button",
                "labelText": "Save",
                "icon": "icons/save.png",
                "shortcut": "Ctrl+S",
                "onClick": "call saveFile();"
            }
        }
    ]
};

show screen fileManager;
```

## Icon Path Resolution

Icons are loaded in the following order:

1. **Classpath resources** (recommended): Place icons in `src/main/resources/icons/`
   - Path: `"icons/filename.png"`
   - Example: `"icons/save.png"`

2. **File system paths**: Use absolute or relative file paths
   - Absolute: `"/home/user/icons/save.png"`
   - Relative: `"./my-icons/save.png"`

### Available Icon Resources

The interpreter includes many pre-built icons in the `icons/` directory:

| Category | Icons |
|----------|-------|
| **Files** | `script-file.png`, `config-file.png`, `markdown-file.png`, `image-file.png`, `text-file.png`, `java-file.png` |
| **Folders** | `folder.png`, `folder-open.png`, `folder_fav.png`, `folder_ref.png` |
| **Projects** | `project.png` |

See `TREEVIEW_ICONS_QUICKREF.md` for a complete list of available icons.

## Icon Specifications

- **Size**: Icons are automatically scaled to **16x16 pixels**
- **Format**: PNG recommended (supports transparency)
- **Location**: Place in `src/main/resources/icons/` for classpath loading
- **Naming**: Use lowercase with hyphens (e.g., `save-file.png`)
- **Transparency**: Supported via PNG alpha channel

## Comparison with TreeView Icons

Button icons use the same underlying implementation as TreeView icons:

| Feature | TreeView | Buttons |
|---------|----------|---------|
| **Icon property** | `icon`, `iconOpen`, `iconClosed` | `icon` |
| **State-based** | Yes (open/closed) | No (static only) |
| **Default size** | 16x16px | 16x16px |
| **Automatic scaling** | Yes | Yes |
| **Classpath loading** | Yes | Yes |
| **File system loading** | Yes | Yes |

## Implementation Details

### Code Changes

1. **DisplayItem.java**: Added `icon` field to store icon path
2. **display-metadata.json**: Added `icon` property to JSON schema
3. **AreaItemFactory.java**: 
   - Added `loadIcon()` helper method for icon loading
   - Modified button creation to apply icon via `Button.setGraphic()`
   - Refactored TreeView icon loading to use shared helper
4. **ScreenFactory.java**: Updated metadata merge and clone methods to handle `icon` field

### Icon Loading Process

```
1. Parse display metadata → icon property extracted
2. Button created → labelText applied
3. loadIcon() called → icon image loaded from path
4. ImageView created → scaled to 16x16px
5. Button.setGraphic() → icon displayed alongside text
```

### Error Handling

- Missing icon files are **silently ignored**
- Invalid icon paths do not break button functionality
- Buttons without icons work exactly as before
- No console warnings for missing optional icons

## Testing

A comprehensive test script is provided in `test_button_icons.ebs`:

```bash
# Run the test script
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="../test_button_icons.ebs"
```

The test demonstrates:
- File action buttons (script, config, markdown, image icons)
- Folder action buttons (folder, open folder, project, favorite icons)
- Status updates on button clicks
- Combination of icons with text and event handlers

## Best Practices

1. **Use meaningful icons**: Choose icons that clearly represent the button's action
2. **Consistent sizing**: All button icons are 16x16px - design accordingly
3. **Classpath resources**: Prefer classpath resources over file system paths for portability
4. **PNG format**: Use PNG for best transparency and quality support
5. **Optional property**: Icons are optional - buttons work with or without them
6. **Combine with shortcuts**: Use icons, text, and shortcuts together for best UX

## Examples

### Toolbar with Icons

```javascript
// File operations toolbar
{
    "name": "fileToolbar",
    "type": "hbox",
    "spacing": "5",
    "padding": "5",
    "items": [
        { "button": "New", "icon": "icons/script-file.png", "shortcut": "Ctrl+N" },
        { "button": "Open", "icon": "icons/folder-open.png", "shortcut": "Ctrl+O" },
        { "button": "Save", "icon": "icons/save.png", "shortcut": "Ctrl+S" }
    ]
}
```

### Action Buttons

```javascript
// Common action buttons with icons
{
    "name": "refreshButton",
    "display": {
        "type": "button",
        "labelText": "Refresh",
        "icon": "icons/refresh.png",
        "shortcut": "F5"
    }
},
{
    "name": "settingsButton",
    "display": {
        "type": "button",
        "labelText": "Settings",
        "icon": "icons/settings.png"
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Icon doesn't appear | Check icon path and ensure file exists in resources |
| Icon is pixelated | Use higher resolution source icon (16x16 or larger) |
| Wrong icon displays | Verify icon path matches actual file location |
| Button too wide | Icon + text may increase width - adjust layout spacing |

## Future Enhancements

Potential future improvements:
- Icon-only buttons (no text)
- Custom icon sizes via `iconWidth`/`iconHeight` properties
- State-based icons (hover, pressed, disabled states)
- Animated icons for progress indicators
- Support for SVG icons

## Related Documentation

- [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) - Complete TreeView icon documentation
- [TREEVIEW_ICONS_QUICKREF.md](TREEVIEW_ICONS_QUICKREF.md) - Quick reference for available icons
- [BUTTON_SHORTCUTS.md](BUTTON_SHORTCUTS.md) - Button keyboard shortcut documentation
- [EBS_LANGUAGE_REFERENCE.md](docs/EBS_LANGUAGE_REFERENCE.md) - Full EBS syntax reference

## Version History

- **v1.0** (2025-12-26): Initial implementation of button icon support
  - Added `icon` property to DisplayItem
  - Implemented icon loading and scaling
  - Created test script and documentation
