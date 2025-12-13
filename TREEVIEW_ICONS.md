# TreeView Icons Guide

## Overview

The EBS language provides built-in functions to set icons on TreeView items dynamically. This allows you to customize the appearance of tree nodes with custom icons for files, folders, and other elements in your TreeView controls.

## Icon Functions

### 1. `scr.setTreeItemIcon(screenName, itemPath, iconPath)`

Sets a static icon for a tree item (typically used for leaf nodes like files).

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item (e.g., "Root.src.main")
- `iconPath` (String): Path to the icon image file

**Returns:** Boolean (true on success)

**Example:**
```javascript
// Set icon for a file
call scr.setTreeItemIcon("fileExplorer", "main.ebs", "icons/script-file-run.png");

// Set icon for a configuration file
call scr.setTreeItemIcon("fileExplorer", "config.json", "icons/config-file.png");
```

### 2. `scr.setTreeItemIcons(screenName, itemPath, iconPath, iconOpenPath, iconClosedPath)`

Sets state-based icons for a tree item (typically used for branch nodes like folders that can expand/collapse).

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item
- `iconPath` (String): Default icon path (fallback)
- `iconOpenPath` (String): Icon to display when item is expanded (null to use default)
- `iconClosedPath` (String): Icon to display when item is collapsed (null to use default)

**Returns:** Boolean (true on success)

**Example:**
```javascript
// Set folder icons that change when opened/closed
call scr.setTreeItemIcons("fileExplorer", "src", 
    "icons/folder.png",           // default
    "icons/folder-open.png",      // when expanded
    "icons/folder.png"            // when collapsed
);

// Set special folder with star
call scr.setTreeItemIcons("fileExplorer", "favorites", 
    "icons/folder_fav.png",       // default
    "icons/folder_fav.png",       // when expanded (same)
    "icons/folder_fav.png"        // when collapsed (same)
);
```

### 3. `scr.getTreeItemIcon(screenName, itemPath)`

Gets the current icon path for a tree item.

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item

**Returns:** String (icon path) or null if no icon set

**Example:**
```javascript
var currentIcon = call scr.getTreeItemIcon("fileExplorer", "main.ebs");
call println("Current icon: " + currentIcon);
```

## Item Path Notation

Tree item paths use dot notation to navigate the tree hierarchy:

- `"Root"` - The root node
- `"Root.src"` - Child "src" of root
- `"Root.src.main"` - Child "main" of "src"
- `"Root.src.main.Main.java"` - File "Main.java" in the "main" folder

**Example Tree Structure:**
```
Root
├── src
│   ├── main
│   │   └── Main.java
│   └── test
│       └── Test.java
└── resources
    └── config.json
```

**Paths:**
- Root: `"Root"`
- src folder: `"Root.src"`
- main folder: `"Root.src.main"`
- Main.java file: `"Root.src.main.Main.java"`
- resources folder: `"Root.resources"`
- config.json file: `"Root.resources.config.json"`

## Available Icons

The EBS runtime includes the following built-in icons in the `icons/` directory:

### File Icons
- `icons/file.png` - Generic file
- `icons/script-file.png` - EBS script file (.ebs)
- `icons/script-file-run.png` - Main/executable EBS script
- `icons/script-file-missing.png` - Missing script file (red)
- `icons/config-file.png` - JSON configuration file
- `icons/css-file.png` - CSS stylesheet file
- `icons/markdown-file.png` - Markdown file (.md)
- `icons/xml-file.png` - XML file
- `icons/java-file.png` - Java source file
- `icons/image-file.png` - Image file (png, jpg, gif)
- `icons/git-file.png` - Git file (.gitignore, etc.)
- `icons/text-file.png` - Plain text file

### Folder Icons
- `icons/folder.png` - Regular folder (closed)
- `icons/folder-open.png` - Regular folder (open)
- `icons/folder_fav.png` - Favorite/bookmarked folder
- `icons/folder_ref.png` - Linked/reference folder

### Project Icons
- `icons/project.png` - Project icon

### UI Icons
- `icons/expand-arrow.png` - Expand/collapse arrow
- `icons/tree-node.png` - Generic tree node
- `icons/dome.png` - Dome/special marker
- `icons/help-page.png` - Help page icon
- `icons/info-page.png` - Info page icon
- `icons/normal-page.png` - Normal page icon

## Icon Loading

Icons can be loaded from three sources (in order of priority):

1. **Classpath resources**: `icons/file.png` (from resources directory)
2. **Resources with leading slash**: `/icons/file.png` 
3. **File system paths**: `/absolute/path/to/icon.png`

**Recommended:** Use classpath resources (e.g., `icons/file.png`) for portability.

## Complete Example

Here's a complete example that creates a TreeView and sets custom icons:

```javascript
// Create a screen with a TreeView
call scr.create("fileExplorer", "File Explorer", 800, 600);

// Add a TreeView component (assuming it's added to the screen)
// ... TreeView creation code ...

// Set icons for folders
call scr.setTreeItemIcons("fileExplorer", "Root.src", 
    "icons/folder.png", 
    "icons/folder-open.png", 
    "icons/folder.png"
);

call scr.setTreeItemIcons("fileExplorer", "Root.resources", 
    "icons/folder.png", 
    "icons/folder-open.png", 
    "icons/folder.png"
);

// Set icons for specific files
call scr.setTreeItemIcon("fileExplorer", "Root.src.main.ebs", "icons/script-file-run.png");
call scr.setTreeItemIcon("fileExplorer", "Root.src.test.ebs", "icons/script-file.png");
call scr.setTreeItemIcon("fileExplorer", "Root.resources.config.json", "icons/config-file.png");
call scr.setTreeItemIcon("fileExplorer", "Root.resources.styles.css", "icons/css-file.png");

// Set icon for special folders
call scr.setTreeItemIcons("fileExplorer", "Root.favorites", 
    "icons/folder_fav.png", 
    "icons/folder_fav.png", 
    "icons/folder_fav.png"
);

// Show the screen
call scr.showScreen("fileExplorer");
```

## Icon Size

All icons are automatically sized to 16x16 pixels with preserved aspect ratio. You can provide icons of any size, but they will be scaled down to fit the tree view cells.

## Dynamic Icon Updates

You can change icons at runtime based on user actions or file states:

```javascript
// Function to update file icon based on status
updateFileIcon(filename, status) return void {
    if (status == "modified") then {
        call scr.setTreeItemIcon("fileExplorer", filename, "icons/file-modified.png");
    } else if (status == "error") then {
        call scr.setTreeItemIcon("fileExplorer", filename, "icons/script-file-missing.png");
    } else {
        call scr.setTreeItemIcon("fileExplorer", filename, "icons/script-file.png");
    } end
}

// Update icon when file state changes
call updateFileIcon("Root.src.main.ebs", "modified");
```

## Custom Icons

You can use custom icons by:

1. **Adding to resources**: Place your icon in `src/main/resources/icons/` and reference it as `icons/myicon.png`
2. **Using file paths**: Provide an absolute or relative file path to your icon

```javascript
// Using custom icon from resources
call scr.setTreeItemIcon("fileExplorer", "Root.custom.txt", "icons/custom-file.png");

// Using custom icon from file system
call scr.setTreeItemIcon("fileExplorer", "Root.external.txt", "/path/to/custom/icon.png");
```

## Error Handling

If an icon fails to load:
- The tree item will display with no icon (null graphic)
- An error message is printed to stderr
- The operation returns false

```javascript
var success = call scr.setTreeItemIcon("fileExplorer", "Root.file.txt", "icons/missing.png");
if (success == false) then {
    call println("Failed to set icon");
end
```

## Best Practices

1. **Use consistent icon sizes**: 16x16 pixels for best results
2. **Use PNG format**: Supports transparency for better appearance
3. **Use classpath resources**: More portable than file paths
4. **Set folder icons with state**: Use `setTreeItemIcons()` for folders to show open/closed states
5. **Set file icons as static**: Use `setTreeItemIcon()` for files that don't expand
6. **Cache icon paths**: Store icon paths in variables to avoid duplication

```javascript
// Good practice: Define icon constants
var ICON_FOLDER = "icons/folder.png";
var ICON_FOLDER_OPEN = "icons/folder-open.png";
var ICON_SCRIPT = "icons/script-file.png";
var ICON_CONFIG = "icons/config-file.png";

// Use constants for consistency
call scr.setTreeItemIcons("fileExplorer", "Root.src", ICON_FOLDER, ICON_FOLDER_OPEN, ICON_FOLDER);
call scr.setTreeItemIcon("fileExplorer", "Root.main.ebs", ICON_SCRIPT);
```

## ProjectTreeView Integration

The built-in ProjectTreeView (in the EBS IDE) automatically sets icons for files and folders based on their type:

- **EBS scripts**: `script-file.png`
- **Main scripts**: `script-file-run.png` (configured in project.json)
- **Missing files**: `script-file-missing.png` (red text)
- **JSON files**: `config-file.png`
- **CSS files**: `css-file.png`
- **Folders**: `folder.png` / `folder-open.png`
- **Linked folders**: `folder_ref.png` (from project.json directories array)
- **Projects**: `project.png`

You can see this implementation in:
- `ScriptInterpreter/src/main/java/com/eb/ui/ebs/ProjectTreeView.java` (lines 1095-1186)

## Troubleshooting

### Icon doesn't appear
- **Check the path**: Ensure the icon path is correct
- **Verify icon exists**: Check that the icon file is in the resources directory
- **Check item path**: Verify the tree item path is correct using dot notation
- **Check screen name**: Ensure the screen name is correct and lowercase

### Icon appears pixelated
- **Use higher resolution**: Provide icons at 16x16 or higher resolution
- **Use PNG format**: Supports better quality and transparency

### State icons don't change
- **Use setTreeItemIcons()**: Not setTreeItemIcon() for state-based icons
- **Check if item has children**: State icons only work for branch nodes with children
- **Verify paths are different**: Ensure open/closed icon paths are different if you want them to change

## See Also

- `EBS_LANGUAGE_REFERENCE.md` - Complete language reference
- `docs/EBS_SCRIPT_SYNTAX.md` - Screen functions documentation (lines 3946-3970)
- `SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md` - Screen component details
- `ProjectTreeView.java` - Built-in IDE TreeView implementation
