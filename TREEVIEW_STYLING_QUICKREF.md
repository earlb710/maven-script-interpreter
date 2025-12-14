# TreeView Styling Quick Reference

## Functions

### Set Bold
```javascript
call scr.setTreeItemBold(screenName, itemPath, true/false);
```

### Set Italic
```javascript
call scr.setTreeItemItalic(screenName, itemPath, true/false);
```

### Set Color
```javascript
call scr.setTreeItemColor(screenName, itemPath, colorString);
```

## Color Formats

| Format | Example | Usage |
|--------|---------|-------|
| Hex | `"#ff0000"` | Red color |
| RGB | `"rgb(255,0,0)"` | Red color |
| RGBA | `"rgba(255,0,0,0.5)"` | Semi-transparent red |
| Name | `"red"` | Named color |

## Common Colors

| Color | Hex | Usage |
|-------|-----|-------|
| Red | `#cc0000` | Errors, tests, critical items |
| Orange | `#ff8800` | Warnings, modified files, config |
| Green | `#008800` | Success, valid, documentation |
| Blue | `#0066cc` | Info, main files, source code |
| Purple | `#9900cc` | Special, data files |
| Gray | `#888888` | Disabled, inactive, secondary |

## Quick Examples

### Bold Folder
```javascript
call scr.setTreeItemBold("myScreen", "Root.src", true);
```

### Italic File
```javascript
call scr.setTreeItemItalic("myScreen", "Root.README.md", true);
```

### Colored File
```javascript
call scr.setTreeItemColor("myScreen", "Root.error.log", "#cc0000");
```

### Combine All Three
```javascript
call scr.setTreeItemBold("myScreen", "Root.important.ebs", true);
call scr.setTreeItemItalic("myScreen", "Root.important.ebs", true);
call scr.setTreeItemColor("myScreen", "Root.important.ebs", "#9900cc");
```

## Common Patterns

### File Status Colors
```javascript
// Error state
call scr.setTreeItemColor(screen, path, "#cc0000");  // Red
call scr.setTreeItemBold(screen, path, true);

// Warning state
call scr.setTreeItemColor(screen, path, "#ff8800");  // Orange

// Success state
call scr.setTreeItemColor(screen, path, "#008800");  // Green
```

### File Type Styling
```javascript
// Script files - bold
if (filename.endsWith(".ebs")) then {
    call scr.setTreeItemBold(screen, path, true);
end

// Documentation - italic + green
if (filename.endsWith(".md")) then {
    call scr.setTreeItemItalic(screen, path, true);
    call scr.setTreeItemColor(screen, path, "#008800");
end

// Config files - italic + orange
if (filename.endsWith(".json")) then {
    call scr.setTreeItemItalic(screen, path, true);
    call scr.setTreeItemColor(screen, path, "#ff8800");
end

// Test files - red
if (filename.startsWith("test_")) then {
    call scr.setTreeItemColor(screen, path, "#cc0000");
end
```

### Folder Emphasis
```javascript
// Make all folders bold
var folders = ["Root.src", "Root.docs", "Root.tests"];
var i = 0;
while (i < array.length(folders)) do {
    call scr.setTreeItemBold("myScreen", folders[i], true);
    i = i + 1;
} end
```

### Main File Highlight
```javascript
// Bold + blue for main entry point
call scr.setTreeItemBold("myScreen", "Root.main.ebs", true);
call scr.setTreeItemColor("myScreen", "Root.main.ebs", "#0066cc");
```

### Priority Levels
```javascript
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

## Remove Styling

```javascript
// Remove bold
call scr.setTreeItemBold("myScreen", "Root.file.txt", false);

// Remove italic
call scr.setTreeItemItalic("myScreen", "Root.file.txt", false);

// Remove color
call scr.setTreeItemColor("myScreen", "Root.file.txt", null);
```

## Item Path Examples

| Tree Structure | Item Path |
|----------------|-----------|
| `Root` | `"Root"` |
| `Root → src` | `"Root.src"` |
| `Root → src → main.ebs` | `"Root.src.main.ebs"` |
| `Root → docs → README.md` | `"Root.docs.README.md"` |

## Best Practices

✓ **DO:**
- Use consistent color schemes
- Combine colors with bold/italic for importance
- Test color contrast and readability
- Document your color meanings

✗ **DON'T:**
- Overuse colors (limit to 3-5)
- Use color alone to convey meaning
- Use too many bold/italic items
- Ignore accessibility

## Integration with Icons

```javascript
// Icons + styling work together
call scr.setTreeItemIcon("myScreen", "Root.main.ebs", "icons/script-file-run.png");
call scr.setTreeItemBold("myScreen", "Root.main.ebs", true);
call scr.setTreeItemColor("myScreen", "Root.main.ebs", "#0066cc");
```

## Error Handling

```javascript
var success = call scr.setTreeItemBold("myScreen", "Root.file.txt", true);
if (success == false) then {
    call println("Failed - check screen name and item path");
end
```

## See Also

- [TREEVIEW_STYLING.md](TREEVIEW_STYLING.md) - Complete styling documentation
- [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) - Complete icon documentation
- [treeview-styling-demo.ebs](ScriptInterpreter/scripts/examples/treeview-styling-demo.ebs) - Working demo script
