# Screen Menu API - Complete Reference

## Overview
Complete API for managing screen menu bars in EBS applications.

## Property-Based Control

### `showMenu` (boolean, default: true)
Controls menu bar visibility at screen creation.

```ebs
screen myScreen = {
    "name": "myScreen",
    "showMenu": false,  // Hide menu at creation
    ...
};
```

## Runtime Control Functions

### `scr.showMenu(screenName?)`
Shows the menu bar on a screen.

**Parameters:**
- `screenName` (string, optional) - Screen name; uses current context if omitted

**Returns:** boolean (true on success)

**Example:**
```ebs
call scr.showMenu("myScreen");
```

### `scr.hideMenu(screenName?)`
Hides the menu bar on a screen.

**Parameters:**
- `screenName` (string, optional) - Screen name; uses current context if omitted

**Returns:** boolean (true on success)

**Example:**
```ebs
call scr.hideMenu("myScreen");
```

### `scr.addMenu(screenName, parentPath, name, displayName, callback)`
Adds a custom menu item to a screen's menu bar.

**Parameters:**
- `screenName` (string, required) - Target screen name
- `parentPath` (string, required) - Parent menu path using dot notation
  - Examples: `"Edit"`, `"Edit.Format"`, `"Tools.Advanced"`
- `name` (string, required) - Internal identifier (not currently used but reserved)
- `displayName` (string, required) - Text shown to user in the menu
- `callback` (string, required) - EBS code to execute when clicked

**Returns:** boolean (true on success)

**Features:**
- Creates parent menus automatically if they don't exist
- Supports arbitrary nesting via dot notation
- Callbacks execute on screen thread via dispatcher

**Examples:**
```ebs
// Add to existing Edit menu
call scr.addMenu("myScreen", "Edit", "action1", "My Action", 
    "println('Action executed');");

// Create new top-level Tools menu
call scr.addMenu("myScreen", "Tools", "tool1", "My Tool", 
    "call myToolFunction();");

// Create nested submenu (auto-creates parents)
call scr.addMenu("myScreen", "Edit.Format", "upper", "Convert to Uppercase",
    "call convertToUpper();");

// Deeply nested menu
call scr.addMenu("myScreen", "Tools.Advanced.Debug", "log", "Show Log",
    "call showDebugLog();");
```

### `scr.removeMenu(screenName, menuPath)`
Removes a menu or menu item from a screen's menu bar.

**Parameters:**
- `screenName` (string, required) - Target screen name
- `menuPath` (string, required) - Menu path using dot notation
  - For top-level menu: `"Tools"` (removes entire menu)
  - For menu item: `"Edit.My Action"` (removes specific item)
  - For nested item: `"Edit.Format.Uppercase"` (removes from submenu)

**Returns:** boolean (true on success)

**Important:** Use the displayName (visible text) in the path, not the internal name.

**Examples:**
```ebs
// Remove a menu item by its displayed name
call scr.removeMenu("myScreen", "Edit.My Action");

// Remove entire top-level menu
call scr.removeMenu("myScreen", "Tools");

// Remove from nested submenu
call scr.removeMenu("myScreen", "Edit.Format.Convert to Uppercase");

// Remove an intermediate submenu (removes all its children too)
call scr.removeMenu("myScreen", "Edit.Format");
```

### `scr.enableMenu(screenName, menuPath)`
Enables a menu or menu item, making it clickable.

**Parameters:**
- `screenName` (string, required) - Target screen name
- `menuPath` (string, required) - Menu path using dot notation
  - For top-level menu: `"Tools"` (enables entire menu)
  - For menu item: `"Edit.Save"` (enables specific item)
  - For nested item: `"Edit.Format.Uppercase"` (enables in submenu)

**Returns:** boolean (true on success)

**Examples:**
```ebs
// Enable a menu item
call scr.enableMenu("myScreen", "Edit.Save");

// Enable entire top-level menu
call scr.enableMenu("myScreen", "Tools");

// Enable nested item
call scr.enableMenu("myScreen", "Edit.Format.Uppercase");

// Enable based on state
if documentModified then {
    call scr.enableMenu("editor", "File.Save");
}
```

### `scr.disableMenu(screenName, menuPath)`
Disables a menu or menu item, making it unclickable and grayed out.

**Parameters:**
- `screenName` (string, required) - Target screen name
- `menuPath` (string, required) - Menu path using dot notation
  - For top-level menu: `"Tools"` (disables entire menu)
  - For menu item: `"Edit.Save"` (disables specific item)
  - For nested item: `"Edit.Format.Uppercase"` (disables in submenu)

**Returns:** boolean (true on success)

**Use Cases:**
- Prevent actions when conditions aren't met
- Show what's available but not currently applicable
- Provide visual feedback about application state
- Disable during long operations to prevent duplicate actions

**Examples:**
```ebs
// Disable a menu item
call scr.disableMenu("myScreen", "Edit.Save");

// Disable entire top-level menu
call scr.disableMenu("myScreen", "Tools");

// Disable nested item
call scr.disableMenu("myScreen", "Edit.Format.Uppercase");

// Context-sensitive disabling
if !documentOpen then {
    call scr.disableMenu("editor", "File.Save");
    call scr.disableMenu("editor", "File.Close");
}

// Disable during processing
call scr.disableMenu("app", "Tools.Process");
// ... perform long operation ...
call scr.enableMenu("app", "Tools.Process");
```

## Complete Workflow Example

```ebs
// Create screen with menu
screen editor = {
    "name": "editor",
    "title": "Text Editor",
    "width": 800,
    "height": 600,
    "showMenu": true,  // Show menu (default)
    "area": [...]
};

show screen editor;

// Add custom File menu items
call scr.addMenu("editor", "File", "new", "New File", "call newFile();");
call scr.addMenu("editor", "File", "open", "Open...", "call openDialog();");
call scr.addMenu("editor", "File", "save", "Save", "call saveFile();");

// Add Edit > Format submenu
call scr.addMenu("editor", "Edit.Format", "upper", "Uppercase", 
    "call toUppercase();");
call scr.addMenu("editor", "Edit.Format", "lower", "Lowercase",
    "call toLowercase();");

// Add Tools menu with nested items
call scr.addMenu("editor", "Tools", "prefs", "Preferences...",
    "call showPreferences();");
call scr.addMenu("editor", "Tools.Advanced", "cache", "Clear Cache",
    "call clearCache();");

// Disable Save until document is modified
call scr.disableMenu("editor", "File.Save");

// Enable Save when document is modified
call scr.enableMenu("editor", "File.Save");

// Remove a menu item when no longer needed
call scr.removeMenu("editor", "Edit.Format.Lowercase");

// Remove entire submenu
call scr.removeMenu("editor", "Edit.Format");

// Toggle menu visibility
call scr.hideMenu("editor");  // Hide for full screen
// ... user works ...
call scr.showMenu("editor");  // Show again

// Remove custom menu completely
call scr.removeMenu("editor", "Tools");
```

## Default Menu Items

When `showMenu: true`, screens include a default Edit menu with:
- Cut (Ctrl+X)
- Copy (Ctrl+C)
- Paste (Ctrl+V)
- Undo (Ctrl+Z)
- Redo (Ctrl+Y)
- Close (Ctrl+W)

Custom items can be added to this menu or new menus created alongside it.

## Path Notation

All menu operations use **dot notation** for hierarchical paths:

| Path | Meaning |
|------|---------|
| `"Edit"` | Top-level Edit menu |
| `"Tools"` | Top-level Tools menu |
| `"Edit.Format"` | Format submenu under Edit |
| `"Edit.Format.Uppercase"` | Uppercase item in Edit > Format |
| `"Tools.Advanced.Debug"` | Debug submenu in Tools > Advanced |

**Important:** When adding items, the `parentPath` does not include the item itself. When removing items, the `menuPath` includes the full path to the item.

## Error Handling

All functions return `true` on success. Errors are logged to console output:

- Menu bar not found → Error message
- Parent menu not found → Warning message
- Item not found → Warning message
- No dispatcher → Error when menu item clicked

## Best Practices

1. **Use descriptive displayNames**: Users see these in the menu
2. **Organize logically**: Group related items in submenus
3. **Keep callbacks simple**: Complex operations should call functions
4. **Remove unused items**: Clean up menus when context changes
5. **Test thoroughly**: Verify menu operations on actual screens
6. **Handle errors**: Check console output for issues

## Common Patterns

### Conditional Menus
```ebs
// Add menus based on user role
if isAdmin then {
    call scr.addMenu("app", "Admin", "users", "Manage Users",
        "call manageUsers();");
}
```

### Context-Sensitive Items
```ebs
// Add/remove based on document state
if documentOpen then {
    call scr.addMenu("app", "File", "close", "Close Document",
        "call closeDoc();");
} else {
    call scr.removeMenu("app", "File.Close Document");
}
```

### Recent Files Menu
```ebs
// Build recent files menu dynamically
foreach file in recentFiles do {
    call scr.addMenu("app", "File.Recent", file.id, file.name,
        "call openFile('" + file.path + "');");
}
```

### Plugin Menus
```ebs
// Add plugin menus dynamically
foreach plugin in loadedPlugins do {
    call scr.addMenu("app", "Plugins", plugin.id, plugin.name,
        "call executePlugin('" + plugin.id + "');");
}
```

### Cleanup on Close
```ebs
// Remove all custom menus before closing
call scr.removeMenu("app", "Tools");
call scr.removeMenu("app", "Plugins");
```

## Limitations

- Cannot modify default Edit menu items (Cut, Copy, etc.)
- Cannot add keyboard shortcuts to custom items (future enhancement)
- Cannot add separators dynamically (future enhancement)
- Cannot add checkboxes or radio items (future enhancement)
- Menu bar must be visible before adding items
- Removal uses displayName matching (case-sensitive)

## Test Scripts

1. **test_menu_visibility.ebs** - Show/hide menu bar
2. **test_addmenu.ebs** - Add custom menu items
3. **test_removemenu.ebs** - Remove menus and items

## See Also

- `SCREEN_MENU_PROPERTY.md` - User guide with examples
- `MENU_FEATURES_COMPLETE.md` - Complete feature documentation
- `help-lookup.json` - Integrated help system
