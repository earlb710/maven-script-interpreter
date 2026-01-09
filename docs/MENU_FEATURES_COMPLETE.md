# Complete Menu Features Implementation

## Overview
This document summarizes all menu-related features for EBS screen windows, providing comprehensive control over menu bars and menu items.

## Features Implemented

### 1. Screen Property: `showMenu`
Control menu bar visibility at screen creation time.

```ebs
screen myScreen = {
    "name": "myScreen",
    "title": "My Application",
    "width": 800,
    "height": 600,
    "showMenu": false,  // Hide menu bar at creation
    "area": [...]
};
```

### 2. Builtin: `scr.showMenu(screenName?)`
Show the menu bar dynamically at runtime.

```ebs
call scr.showMenu("myScreen");
// Or from within screen event handler:
call scr.showMenu();
```

### 3. Builtin: `scr.hideMenu(screenName?)`
Hide the menu bar dynamically at runtime.

```ebs
call scr.hideMenu("myScreen");
// Or from within screen event handler:
call scr.hideMenu();
```

### 4. Builtin: `scr.addMenu(screenName, parentPath, name, displayName, callback)`
Add custom menu items dynamically with callback execution.

**Parameters:**
- `screenName` (string, required): Name of the target screen
- `parentPath` (string, required): Menu hierarchy using dot notation
  - Examples: `"Edit"`, `"Edit.Format"`, `"Tools.Advanced"`
- `name` (string, required): Internal identifier for the menu item
- `displayName` (string, required): Text displayed to the user
- `callback` (string, required): EBS code to execute when clicked

**Features:**
- Creates parent menus automatically if they don't exist
- Supports nested submenus via dot notation
- Executes callbacks on screen thread via dispatcher
- Returns true on success

### 5. Builtin: `scr.removeMenu(screenName, menuPath)`
Remove menus or menu items dynamically.

**Parameters:**
- `screenName` (string, required): Name of the target screen
- `menuPath` (string, required): Menu path using dot notation
  - Examples: `"Tools"` (removes entire menu), `"Edit.customAction"` (removes specific item)

**Features:**
- Can remove entire top-level menus
- Can remove specific menu items from any level
- Supports nested submenus via dot notation
- Returns true on success

### 6. Builtin: `scr.enableMenu(screenName, menuPath)`
Enable menus or menu items, making them clickable.

**Parameters:**
- `screenName` (string, required): Name of the target screen
- `menuPath` (string, required): Menu path using dot notation
  - Examples: `"Tools"` (enables entire menu), `"Edit.Save"` (enables specific item)

**Features:**
- Can enable entire top-level menus
- Can enable specific menu items at any level
- Supports nested submenus via dot notation
- Returns true on success

### 7. Builtin: `scr.disableMenu(screenName, menuPath)`
Disable menus or menu items, making them unclickable and grayed out.

**Parameters:**
- `screenName` (string, required): Name of the target screen
- `menuPath` (string, required): Menu path using dot notation
  - Examples: `"Tools"` (disables entire menu), `"Edit.Save"` (disables specific item)

**Features:**
- Can disable entire top-level menus
- Can disable specific menu items at any level
- Supports nested submenus via dot notation
- Useful for context-sensitive menu states
- Returns true on success

**Examples:**

```ebs
// Add to existing top-level menu
call scr.addMenu("myScreen", "Edit", "customAction", "Custom Action", 
    "println('Custom action executed!');");

// Create new top-level menu with item
call scr.addMenu("myScreen", "Tools", "myTool", "My Tool", 
    "call myToolFunction();");

// Add to nested submenu (creates hierarchy if needed)
call scr.addMenu("myScreen", "Edit.Format", "uppercase", "Convert to Uppercase", 
    "call convertToUpper();");

// Create deeply nested menu
call scr.addMenu("myScreen", "Tools.Advanced.Debug", "inspect", "Inspect Variables",
    "call debugInspect();");
```

## Complete Usage Example

```ebs
// Create a screen with custom menus
screen customMenuScreen = {
    "name": "customMenuScreen",
    "title": "Custom Menu Demo",
    "width": 800,
    "height": 600,
    "area": [
        {
            "name": "main",
            "type": "vbox",
            "items": [
                {
                    "type": "label",
                    "text": "Demo of custom menu features"
                },
                {
                    "type": "button",
                    "text": "Toggle Menu",
                    "onClick": "if menuVisible then call scr.hideMenu(); else call scr.showMenu();"
                }
            ]
        }
    ]
};

show screen customMenuScreen;

// Add custom File menu with multiple items
call scr.addMenu("custommenuscreen", "File", "new", "New Document", 
    "println('New document');");
call scr.addMenu("custommenuscreen", "File", "open", "Open...", 
    "println('Open dialog');");
call scr.addMenu("custommenuscreen", "File", "save", "Save", 
    "println('Save document');");

// Add Tools menu with nested submenus
call scr.addMenu("custommenuscreen", "Tools", "preferences", "Preferences...", 
    "println('Show preferences');");
call scr.addMenu("custommenuscreen", "Tools.Advanced", "clearCache", "Clear Cache", 
    "println('Cache cleared');");
call scr.addMenu("custommenuscreen", "Tools.Advanced.Debug", "showLog", "Show Debug Log",
    "println('Show log');");

// Add Help menu
call scr.addMenu("custommenuscreen", "Help", "about", "About", 
    "println('About dialog');");
call scr.addMenu("custommenuscreen", "Help", "docs", "Documentation", 
    "println('Open docs');");
```

## Default Menu Bar

When `showMenu` is true (default), screens include an Edit menu with:
- Cut (Ctrl+X)
- Copy (Ctrl+C)
- Paste (Ctrl+V)
- Undo (Ctrl+Z)
- Redo (Ctrl+Y)
- Close (Ctrl+W)

Custom menu items can be added to the Edit menu or create new menus alongside it.

## Use Cases

### 1. Hide Menu for Clean UI
```ebs
screen kiosk = {
    "showMenu": false,  // No menu bar for public kiosk
    ...
};
```

### 2. Toggle Menu Visibility
```ebs
// Show/hide based on user role
if isAdmin then {
    call scr.showMenu("mainScreen");
} else {
    call scr.hideMenu("mainScreen");
}
```

### 3. Add and Remove Application-Specific Menus
```ebs
// Add custom menus for your application
call scr.addMenu("app", "File.Recent", "file1", "document1.txt", 
    "call openFile('document1.txt');");
call scr.addMenu("app", "Edit.Format", "indent", "Increase Indent",
    "call increaseIndent();");
call scr.addMenu("app", "View.Zoom", "zoomIn", "Zoom In",
    "call zoom(1.25);");

// Remove a menu item
call scr.removeMenu("app", "File.Recent.document1.txt");

// Remove entire submenu
call scr.removeMenu("app", "Edit.Format");
```

### 4. Context-Sensitive Menus with Enable/Disable
```ebs
// Enable/disable menus based on document state
if documentOpen then {
    call scr.enableMenu("editor", "File.Save");
    call scr.enableMenu("editor", "File.Close");
} else {
    call scr.disableMenu("editor", "File.Save");
    call scr.disableMenu("editor", "File.Close");
}

// Disable during processing
call scr.disableMenu("app", "Tools");
// ... perform long operation ...
call scr.enableMenu("app", "Tools");
```

### 5. Dynamic Tool Menus
```ebs
// Build menu from available plugins/tools
foreach tool in availableTools do {
    call scr.addMenu("app", "Tools", tool.id, tool.displayName,
        "call executeTool('" + tool.id + "');");
}
```

## Implementation Details

### Storage
- MenuBar references stored in `ScreenFactory.screenMenuBars` map
- Indexed by screen name (case-insensitive)
- Stored when menu is created with `showMenu: true`

### Event Execution
- Menu callbacks execute on screen thread via `ScreenEventDispatcher`
- Uses `dispatchAsync()` for non-blocking execution
- Errors logged to console output

### Menu Hierarchy
- Parent path parsed using dot notation
- Menus created recursively if they don't exist
- Supports arbitrary nesting depth

## Testing

Test scripts available:
1. `ScriptInterpreter/scripts/test/test_menu_visibility.ebs` - Show/hide functionality
2. `ScriptInterpreter/scripts/test/test_addmenu.ebs` - Custom menu items
3. `ScriptInterpreter/scripts/test/test_removemenu.ebs` - Remove menu items
4. `ScriptInterpreter/scripts/test/test_enabledisablemenu.ebs` - Enable/disable menu items

## Documentation

- **User Guide**: `SCREEN_MENU_PROPERTY.md`
- **Implementation**: `IMPLEMENTATION_SUMMARY_MENU_PROPERTY.md`
- **Feature Summary**: `MENU_CONTROL_FEATURE_SUMMARY.md`
- **Help System**: Integrated in `help-lookup.json`

## API Reference

| Function | Parameters | Return | Description |
|----------|------------|--------|-------------|
| `scr.showMenu` | screenName? | bool | Shows menu bar |
| `scr.hideMenu` | screenName? | bool | Hides menu bar |
| `scr.addMenu` | screenName, parentPath, name, displayName, callback | bool | Adds custom menu item |
| `scr.removeMenu` | screenName, menuPath | bool | Removes menu or menu item |
| `scr.enableMenu` | screenName, menuPath | bool | Enables menu or menu item |
| `scr.disableMenu` | screenName, menuPath | bool | Disables menu or menu item |

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `showMenu` | boolean | true | Controls menu visibility at creation |

## Notes

- Screen name parameter is optional when called from within screen event handlers
- Screen names are case-insensitive
- Menu bar must be shown (via property or `scr.showMenu()`) before adding menu items
- Callback code executes in screen's interpreter context
- Parent menus are created with default styling

## Backward Compatibility

✅ All existing screens work without changes
- Default behavior: `showMenu: true` (menu shown)
- Existing Edit menu remains unchanged
- No breaking changes to API

## Future Enhancements

Potential additions:
- Menu item separators
- Keyboard accelerators for custom items
- Menu item enable/disable
- Menu item icons
- Checkbox/radio menu items
- Remove/update menu items
- Menu item tooltips
