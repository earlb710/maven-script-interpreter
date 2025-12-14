# TreeView Styling Guide

## Overview

The EBS language provides built-in functions to style TreeView items dynamically. You can set bold, italic, and color styling on individual tree nodes to highlight important items, indicate status, or improve visual organization.

## Styling Functions

### 1. `scr.setTreeItemBold(screenName, itemPath, bold)`

Sets bold text styling for a tree item.

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item (e.g., "Root.src.main")
- `bold` (Boolean): true to enable bold, false to disable

**Returns:** Boolean (true on success)

**Example:**
```javascript
// Make folder name bold
call scr.setTreeItemBold("fileExplorer", "Root.src", true);

// Make file name bold
call scr.setTreeItemBold("fileExplorer", "Root.main.ebs", true);

// Remove bold styling
call scr.setTreeItemBold("fileExplorer", "Root.test.ebs", false);
```

### 2. `scr.setTreeItemItalic(screenName, itemPath, italic)`

Sets italic text styling for a tree item.

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item
- `italic` (Boolean): true to enable italic, false to disable

**Returns:** Boolean (true on success)

**Example:**
```javascript
// Make documentation file italic
call scr.setTreeItemItalic("fileExplorer", "Root.docs.README.md", true);

// Make configuration file italic
call scr.setTreeItemItalic("fileExplorer", "Root.config.json", true);

// Remove italic styling
call scr.setTreeItemItalic("fileExplorer", "Root.data.txt", false);
```

### 3. `scr.setTreeItemColor(screenName, itemPath, color)`

Sets text color for a tree item.

**Parameters:**
- `screenName` (String): The name of the screen containing the TreeView
- `itemPath` (String): Dot-notation path to the tree item
- `color` (String): Color value (hex, rgb, or color name)

**Returns:** Boolean (true on success)

**Color Formats:**
- **Hex:** `#RRGGBB` (e.g., `"#ff0000"` for red)
- **RGB:** `rgb(r,g,b)` (e.g., `"rgb(255,0,0)"` for red)
- **RGBA:** `rgba(r,g,b,a)` (e.g., `"rgba(255,0,0,0.5)"` for semi-transparent red)
- **Color names:** `"red"`, `"blue"`, `"green"`, etc.

**Example:**
```javascript
// Set file to red (hex format)
call scr.setTreeItemColor("fileExplorer", "Root.error.log", "#ff0000");

// Set file to blue (rgb format)
call scr.setTreeItemColor("fileExplorer", "Root.main.ebs", "rgb(0,102,204)");

// Set file to green (color name)
call scr.setTreeItemColor("fileExplorer", "Root.success.txt", "green");

// Remove color styling (use null or empty string)
call scr.setTreeItemColor("fileExplorer", "Root.normal.txt", null);
```

## Item Path Notation

Tree item paths use dot notation to navigate the tree hierarchy:

- `"Root"` - The root node
- `"Root.src"` - Child "src" of root
- `"Root.src.main"` - Child "main" of "src"
- `"Root.src.main.Main.java"` - File "Main.java" in the "main" folder

See [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md#item-path-notation) for more examples.

## Combining Styles

You can combine multiple styling functions on the same tree item:

```javascript
// Make a file bold, italic, and purple
call scr.setTreeItemBold("fileExplorer", "Root.important.ebs", true);
call scr.setTreeItemItalic("fileExplorer", "Root.important.ebs", true);
call scr.setTreeItemColor("fileExplorer", "Root.important.ebs", "#9900cc");

// Make folder bold and blue
call scr.setTreeItemBold("fileExplorer", "Root.src", true);
call scr.setTreeItemColor("fileExplorer", "Root.src", "#0066cc");
```

## Common Use Cases

### 1. File Status Indication

```javascript
// Color code files by status
statusColorFile(screen, path, status) return void {
    if (status == "modified") then {
        call scr.setTreeItemColor(screen, path, "#ff8800");  // Orange
        call scr.setTreeItemBold(screen, path, true);
    } else if (status == "error") then {
        call scr.setTreeItemColor(screen, path, "#cc0000");  // Red
        call scr.setTreeItemBold(screen, path, true);
    } else if (status == "success") then {
        call scr.setTreeItemColor(screen, path, "#008800");  // Green
    } else {
        call scr.setTreeItemColor(screen, path, null);
        call scr.setTreeItemBold(screen, path, false);
    } end
}
```

### 2. File Type Styling

```javascript
// Style files based on type
styleByFileType(screen, path, filename) return void {
    if (filename.endsWith(".ebs")) then {
        // Script files: bold
        call scr.setTreeItemBold(screen, path, true);
    } else if (filename.endsWith(".md")) then {
        // Documentation: italic and green
        call scr.setTreeItemItalic(screen, path, true);
        call scr.setTreeItemColor(screen, path, "#008800");
    } else if (filename.endsWith(".json") or filename.endsWith(".xml")) then {
        // Config files: orange and italic
        call scr.setTreeItemColor(screen, path, "#ff8800");
        call scr.setTreeItemItalic(screen, path, true);
    } else if (filename.startsWith("test_")) then {
        // Test files: red
        call scr.setTreeItemColor(screen, path, "#cc0000");
    } end
}
```

### 3. Folder Hierarchy Emphasis

```javascript
// Make all folders bold to stand out
styleFolders(screen, folderPaths) return void {
    var i = 0;
    while (i < array.length(folderPaths)) do {
        var folderPath = folderPaths[i];
        call scr.setTreeItemBold(screen, folderPath, true);
        i = i + 1;
    } end
}

// Usage
var folders = ["Root.src", "Root.docs", "Root.tests", "Root.resources"];
call styleFolders("fileExplorer", folders);
```

### 4. Main File Highlighting

```javascript
// Highlight the main entry point
call scr.setTreeItemBold("fileExplorer", "Root.main.ebs", true);
call scr.setTreeItemColor("fileExplorer", "Root.main.ebs", "#0066cc");  // Blue
```

### 5. Priority or Importance Levels

```javascript
// Set priority colors
setPriority(screen, path, priority) return void {
    if (priority == "high") then {
        call scr.setTreeItemBold(screen, path, true);
        call scr.setTreeItemColor(screen, path, "#cc0000");  // Red
    } else if (priority == "medium") then {
        call scr.setTreeItemColor(screen, path, "#ff8800");  // Orange
    } else if (priority == "low") then {
        call scr.setTreeItemColor(screen, path, "#888888");  // Gray
    } end
}
```

## Color Palette Suggestions

### Status Colors
- **Success/Valid:** `#008800` (green)
- **Warning/Modified:** `#ff8800` (orange)
- **Error/Invalid:** `#cc0000` (red)
- **Info/Special:** `#0066cc` (blue)
- **Disabled/Inactive:** `#888888` (gray)

### File Type Colors
- **Source Code:** `#0066cc` (blue)
- **Documentation:** `#008800` (green)
- **Configuration:** `#ff8800` (orange)
- **Tests:** `#cc0000` (red)
- **Data Files:** `#9900cc` (purple)

### Folder Colors
- **Source Folder:** `#0066cc` (blue)
- **Resources:** `#008800` (green)
- **Build Output:** `#888888` (gray)
- **Tests:** `#cc0000` (red)

## Complete Example

Here's a complete example that creates a styled file explorer:

```javascript
// Create screen with TreeView
call scr.create("fileExplorer", "Styled File Explorer", 600, 500);

// Add TreeView control
var treeData = call map.new();
call map.put(treeData, "type", "treeview");
call map.put(treeData, "name", "fileTree");
// ... (set position and size)

// Create tree structure
// ... (add folders and files)

call scr.addControl("fileExplorer", treeData);

// Apply styling
// Make all folders bold
call scr.setTreeItemBold("fileExplorer", "Root.src", true);
call scr.setTreeItemBold("fileExplorer", "Root.docs", true);
call scr.setTreeItemBold("fileExplorer", "Root.tests", true);

// Style main script file (bold + blue)
call scr.setTreeItemBold("fileExplorer", "Root.src.main.ebs", true);
call scr.setTreeItemColor("fileExplorer", "Root.src.main.ebs", "#0066cc");

// Style documentation files (italic + green)
call scr.setTreeItemItalic("fileExplorer", "Root.docs.README.md", true);
call scr.setTreeItemColor("fileExplorer", "Root.docs.README.md", "#008800");

// Style test files (red)
call scr.setTreeItemColor("fileExplorer", "Root.tests.test_main.ebs", "#cc0000");

// Style config file (italic + orange)
call scr.setTreeItemItalic("fileExplorer", "Root.config.json", true);
call scr.setTreeItemColor("fileExplorer", "Root.config.json", "#ff8800");

// Show the screen
call scr.showScreen("fileExplorer");
```

## Dynamic Styling

You can update styles dynamically based on runtime conditions:

```javascript
// Function to update file styling based on validation result
updateFileValidation(screen, path, isValid) return void {
    if (isValid) then {
        call scr.setTreeItemColor(screen, path, "#008800");  // Green
        call scr.setTreeItemBold(screen, path, false);
    } else {
        call scr.setTreeItemColor(screen, path, "#cc0000");  // Red
        call scr.setTreeItemBold(screen, path, true);
    } end
}

// Validate files and update their styling
validateAndStyleFiles(screen, filePaths) return void {
    var i = 0;
    while (i < array.length(filePaths)) do {
        var filePath = filePaths[i];
        var isValid = call validateFile(filePath);  // Your validation logic
        call updateFileValidation(screen, filePath, isValid);
        i = i + 1;
    } end
}
```

## Removing Styles

To remove styling, set the style to its default value:

```javascript
// Remove bold
call scr.setTreeItemBold("fileExplorer", "Root.file.txt", false);

// Remove italic
call scr.setTreeItemItalic("fileExplorer", "Root.file.txt", false);

// Remove color (use null or empty string)
call scr.setTreeItemColor("fileExplorer", "Root.file.txt", null);
```

## Best Practices

1. **Use Consistent Colors:** Define color constants and reuse them
   ```javascript
   var COLOR_SUCCESS = "#008800";
   var COLOR_ERROR = "#cc0000";
   var COLOR_WARNING = "#ff8800";
   ```

2. **Don't Overuse Styling:** Too many colors and styles can be distracting
   - Use bold for important items only
   - Limit your color palette to 3-5 colors
   - Use italic sparingly for special emphasis

3. **Consider Accessibility:**
   - Use sufficient color contrast
   - Don't rely solely on color to convey meaning
   - Combine color with bold or italic for important items

4. **Document Your Color Scheme:**
   - Add comments explaining what each color represents
   - Create a style guide for your application

5. **Test Combinations:**
   - Make sure text is readable with your color choices
   - Test bold + italic combinations
   - Verify colors work with your TreeView background

## Error Handling

All styling functions return boolean values indicating success:

```javascript
var success = call scr.setTreeItemBold("fileExplorer", "Root.file.txt", true);
if (success == false) then {
    call println("Failed to apply bold style - check screen name and item path");
end
```

## Performance Considerations

- Styling is stored in a WeakHashMap, so it doesn't prevent garbage collection
- Styles are applied when tree cells are rendered
- Updating many items at once is efficient
- TreeView automatically refreshes when styles change

## Integration with Icons

Styling works perfectly with TreeView icons:

```javascript
// Set icon AND styling
call scr.setTreeItemIcon("fileExplorer", "Root.main.ebs", "icons/script-file-run.png");
call scr.setTreeItemBold("fileExplorer", "Root.main.ebs", true);
call scr.setTreeItemColor("fileExplorer", "Root.main.ebs", "#0066cc");
```

See [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) for complete icon documentation.

## See Also

- **[TREEVIEW_ICONS.md](TREEVIEW_ICONS.md)** - Complete guide to setting TreeView icons
- **[TREEVIEW_ICONS_QUICKREF.md](TREEVIEW_ICONS_QUICKREF.md)** - Quick reference for TreeView functions
- **[docs/EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md)** - Full EBS syntax reference
- **[treeview-styling-demo.ebs](ScriptInterpreter/scripts/examples/treeview-styling-demo.ebs)** - Working demo script

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Styling doesn't appear | Check screen name and item path are correct |
| Color looks wrong | Verify color format (hex needs #, rgb needs rgb()) |
| TreeView doesn't update | Styles apply automatically; try refreshing the screen |
| Wrong item gets styled | Check dot notation path carefully |
| Styles disappear | Styles are stored per TreeItem instance; recreating tree clears styles |

## Demo Script

See `ScriptInterpreter/scripts/examples/treeview-styling-demo.ebs` for a complete working example demonstrating all styling functions.
