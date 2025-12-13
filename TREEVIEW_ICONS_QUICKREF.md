# TreeView Icons Quick Reference

## Functions

### Set Static Icon (for files/leaves)
```javascript
call scr.setTreeItemIcon(screenName, itemPath, iconPath);
```

### Set State-Based Icons (for folders/branches)
```javascript
call scr.setTreeItemIcons(screenName, itemPath, defaultIcon, openIcon, closedIcon);
```

### Get Current Icon
```javascript
var iconPath = call scr.getTreeItemIcon(screenName, itemPath);
```

## Common Icon Paths

| Type | Icon Path | Usage |
|------|-----------|-------|
| **Folders** | | |
| Regular folder | `icons/folder.png` | Closed folder |
| Open folder | `icons/folder-open.png` | Expanded folder |
| Favorite folder | `icons/folder_fav.png` | Bookmarked/starred folder |
| Linked folder | `icons/folder_ref.png` | External/linked folder |
| **Script Files** | | |
| Regular script | `icons/script-file.png` | Normal .ebs file |
| Main script | `icons/script-file-run.png` | Executable/main .ebs file |
| Missing script | `icons/script-file-missing.png` | File not found (red) |
| **Configuration Files** | | |
| JSON | `icons/config-file.png` | .json files |
| CSS | `icons/css-file.png` | .css files |
| XML | `icons/xml-file.png` | .xml files |
| **Documentation** | | |
| Markdown | `icons/markdown-file.png` | .md files |
| Text | `icons/text-file.png` | .txt files |
| **Code Files** | | |
| Java | `icons/java-file.png` | .java files |
| **Media** | | |
| Image | `icons/image-file.png` | .png, .jpg, .gif files |
| **Other** | | |
| Generic file | `icons/file.png` | Any other file type |
| Git file | `icons/git-file.png` | .gitignore, etc. |
| Project | `icons/project.png` | Project root |

## Item Path Examples

| Tree Structure | Item Path |
|----------------|-----------|
| `Root` | `"Root"` |
| `Root → src` | `"Root.src"` |
| `Root → src → main` | `"Root.src.main"` |
| `Root → src → Main.java` | `"Root.src.Main.java"` |
| `Root → resources → config.json` | `"Root.resources.config.json"` |

## Quick Examples

### Set Folder Icon (changes on expand/collapse)
```javascript
call scr.setTreeItemIcons("myScreen", "Root.src", 
    "icons/folder.png",         // default
    "icons/folder-open.png",    // when open
    "icons/folder.png"          // when closed
);
```

### Set File Icon (static)
```javascript
call scr.setTreeItemIcon("myScreen", "Root.main.ebs", "icons/script-file-run.png");
```

### Set Special Folder (same icon always)
```javascript
call scr.setTreeItemIcons("myScreen", "Root.favorites", 
    "icons/folder_fav.png",
    "icons/folder_fav.png",
    "icons/folder_fav.png"
);
```

### Remove Icon
```javascript
call scr.setTreeItemIcon("myScreen", "Root.file.txt", null);
```

## Common Patterns

### File Type Based Icons
```javascript
// Define icon constants
var ICON_SCRIPT = "icons/script-file.png";
var ICON_MAIN = "icons/script-file-run.png";
var ICON_CONFIG = "icons/config-file.png";
var ICON_CSS = "icons/css-file.png";
var ICON_MARKDOWN = "icons/markdown-file.png";

// Apply based on file extension
if (filename.endsWith(".ebs")) then {
    call scr.setTreeItemIcon(screen, path, ICON_SCRIPT);
} else if (filename.endsWith(".json")) then {
    call scr.setTreeItemIcon(screen, path, ICON_CONFIG);
} else if (filename.endsWith(".css")) then {
    call scr.setTreeItemIcon(screen, path, ICON_CSS);
} else if (filename.endsWith(".md")) then {
    call scr.setTreeItemIcon(screen, path, ICON_MARKDOWN);
} end
```

### Dynamic Icon Update
```javascript
// Function to update icon based on file status
updateFileStatus(screen, path, status) return void {
    if (status == "modified") then {
        call scr.setTreeItemIcon(screen, path, "icons/script-file-modified.png");
    } else if (status == "error") then {
        call scr.setTreeItemIcon(screen, path, "icons/script-file-missing.png");
    } else {
        call scr.setTreeItemIcon(screen, path, "icons/script-file.png");
    } end
}
```

### Folder Type Based Icons
```javascript
// Apply different icons based on folder purpose
setupFolderIcons(screen) return void {
    // Source code folder
    call scr.setTreeItemIcons(screen, "Root.src", 
        "icons/folder.png", "icons/folder-open.png", "icons/folder.png");
    
    // Resources folder
    call scr.setTreeItemIcons(screen, "Root.resources", 
        "icons/folder.png", "icons/folder-open.png", "icons/folder.png");
    
    // Favorites/bookmarks folder
    call scr.setTreeItemIcons(screen, "Root.favorites", 
        "icons/folder_fav.png", "icons/folder_fav.png", "icons/folder_fav.png");
    
    // External/linked folder
    call scr.setTreeItemIcons(screen, "Root.external", 
        "icons/folder_ref.png", "icons/folder_ref.png", "icons/folder_ref.png");
}
```

## Error Handling

```javascript
var success = call scr.setTreeItemIcon("myScreen", "Root.file.txt", "icons/file.png");
if (success == false) then {
    call println("Failed to set icon - check screen name, item path, and icon path");
end
```

## Icon Specifications

- **Size**: Icons are automatically scaled to 16x16 pixels
- **Format**: PNG recommended (supports transparency)
- **Location**: Place in `src/main/resources/icons/` for classpath loading
- **Naming**: Use lowercase with hyphens (e.g., `script-file.png`)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Icon doesn't appear | Check icon path, item path, and screen name |
| Icon is pixelated | Use higher resolution source icon (16x16+) |
| State icons don't change | Use `setTreeItemIcons()` and ensure item has children |
| Wrong item gets icon | Check dot notation path carefully |
| Custom icon not loading | Verify icon file exists in resources or use absolute path |

## See Also

- [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) - Complete documentation with detailed examples
- [docs/EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md) - Full EBS syntax reference
- [ScriptInterpreter/scripts/examples/treeview-icons-demo.ebs](ScriptInterpreter/scripts/examples/treeview-icons-demo.ebs) - Working demo script
