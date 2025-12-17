# Screen Menu Visibility Property

## Overview

This document describes the `showMenu` property for EBS screen definitions, which controls whether the menu bar is displayed at the top of screen windows.

## Feature Description

The `showMenu` property is a boolean flag that can be set in screen JSON definitions to control the visibility of the menu bar. When set to `false`, the screen will be created without the default Edit menu at the top, providing a cleaner interface for applications that don't require menu functionality.

## Property Details

- **Property Name**: `showMenu`
- **Type**: Boolean
- **Default Value**: `true` (menu is shown by default)
- **Location**: Top-level property in screen JSON definition

## Usage

### Syntax

In your screen definition JSON, add the `showMenu` property at the top level:

```ebs
screen myScreen = {
    "name": "myScreen",
    "title": "My Screen",
    "width": 800,
    "height": 600,
    "showMenu": false,  // Hides the menu bar
    "area": [
        // ... area definitions
    ]
};
```

### Example: Screen WITH Menu (Default)

```ebs
screen screenWithMenu = {
    "name": "screenWithMenu",
    "title": "Screen with Menu Bar",
    "width": 600,
    "height": 400,
    "area": [
        {
            "name": "main",
            "type": "vbox",
            "items": [
                {
                    "type": "label",
                    "text": "This screen shows the menu bar at the top."
                }
            ]
        }
    ]
};

show screen screenWithMenu;
```

This creates a screen with the default Edit menu bar visible.

### Example: Screen WITHOUT Menu

```ebs
screen screenWithoutMenu = {
    "name": "screenWithoutMenu",
    "title": "Screen without Menu Bar",
    "width": 600,
    "height": 400,
    "showMenu": false,
    "area": [
        {
            "name": "main",
            "type": "vbox",
            "items": [
                {
                    "type": "label",
                    "text": "This screen has no menu bar."
                }
            ]
        }
    ]
};

show screen screenWithoutMenu;
```

This creates a screen without the menu bar, providing a cleaner interface.

## Dynamic Menu Control with Builtin Functions

In addition to the `showMenu` property, you can control menu visibility and add custom menu items at runtime using builtin functions:

### scr.showMenu(screenName?)

Shows the menu bar on a screen. If no screen name is provided, operates on the current screen context.

```ebs
// Show menu for a specific screen
call scr.showMenu("myScreen");

// Or from within a screen event handler
call scr.showMenu();
```

### scr.hideMenu(screenName?)

Hides the menu bar on a screen. If no screen name is provided, operates on the current screen context.

```ebs
// Hide menu for a specific screen
call scr.hideMenu("myScreen");

// Or from within a screen event handler
call scr.hideMenu();
```

### scr.addMenu(screenName, parentPath, name, displayName, callback)

Adds a custom menu item to a screen's menu bar dynamically. The parent path uses dot notation to specify the menu hierarchy.

**Parameters:**
- `screenName` - The name of the screen
- `parentPath` - Parent menu path (e.g., "Edit" or "Edit.Format")
- `name` - Internal identifier for the menu item
- `displayName` - Text displayed to the user
- `callback` - EBS code to execute when clicked

```ebs
// Add to existing Edit menu
call scr.addMenu("myScreen", "Edit", "customAction", "Custom Action", 
    "println('Custom action executed');");

// Create new top-level menu with item
call scr.addMenu("myScreen", "Tools", "myTool", "My Tool", 
    "call myToolFunction();");

// Add to nested submenu (creates if needed)
call scr.addMenu("myScreen", "Edit.Format", "uppercase", "Convert to Uppercase", 
    "call convertToUpper();");
```

### scr.removeMenu(screenName, menuPath)

Removes a menu or menu item from a screen's menu bar dynamically.

**Parameters:**
- `screenName` - The name of the screen
- `menuPath` - Menu path using dot notation (e.g., "Tools" or "Edit.customAction")

```ebs
// Remove a menu item
call scr.removeMenu("myScreen", "Edit.customAction");

// Remove an entire top-level menu
call scr.removeMenu("myScreen", "Tools");

// Remove from nested submenu
call scr.removeMenu("myScreen", "Edit.Format.uppercase");
```

### scr.enableMenu(screenName, menuPath)

Enables a menu or menu item, making it clickable.

**Parameters:**
- `screenName` - The name of the screen
- `menuPath` - Menu path using dot notation

```ebs
// Enable a menu item
call scr.enableMenu("myScreen", "Edit.Save");

// Enable an entire menu
call scr.enableMenu("myScreen", "Tools");

// Enable nested item
call scr.enableMenu("myScreen", "Edit.Format.Uppercase");
```

### scr.disableMenu(screenName, menuPath)

Disables a menu or menu item, making it unclickable and grayed out.

**Parameters:**
- `screenName` - The name of the screen
- `menuPath` - Menu path using dot notation

```ebs
// Disable a menu item
call scr.disableMenu("myScreen", "Edit.Save");

// Disable an entire menu
call scr.disableMenu("myScreen", "Tools");

// Context-sensitive disabling
if !documentOpen then {
    call scr.disableMenu("editor", "File.Save");
}
```

### Example: Toggle Menu with Buttons

```ebs
screen interactiveScreen = {
    "name": "interactiveScreen",
    "title": "Interactive Menu Demo",
    "width": 600,
    "height": 400,
    "area": [
        {
            "name": "main",
            "type": "vbox",
            "items": [
                {
                    "type": "button",
                    "text": "Hide Menu",
                    "onClick": "call scr.hideMenu();"
                },
                {
                    "type": "button",
                    "text": "Show Menu",
                    "onClick": "call scr.showMenu();"
                }
            ]
        }
    ]
};

show screen interactiveScreen;
```

## Technical Implementation

### Modified Classes

1. **ScreenDefinition.java**
   - Added `showMenu` boolean field (default: `true`)
   - Added getter `isShowMenu()` and setter `setShowMenu(boolean)`
   - Updated `toString()` to include showMenu property
   - Modified `createNewStage()` to pass showMenu to ScreenFactory

2. **ScreenFactory.java**
   - Added overloaded `createScreen()` method with `showMenu` parameter
   - Modified menu bar creation to be conditional based on `showMenu` flag
   - Updated `createScreenFromDefinition()` to parse `showMenu` from JSON
   - Backward compatible: existing calls default to `showMenu = true`

### Code Changes

The menu bar is now created conditionally:

```java
// Only add menu bar if showMenu is true
if (showMenu) {
    javafx.scene.control.MenuBar menuBar = createScreenMenuBar(stage);
    screenRoot.setTop(menuBar);
}
```

### Backward Compatibility

All existing screen definitions will continue to work as before, with the menu bar shown by default. The `showMenu` property is optional and defaults to `true` if not specified.

## Use Cases

1. **Full-Screen Applications**: Applications that want to maximize screen space
2. **Kiosk Mode**: Screens that should not expose menu functionality to users
3. **Custom UI**: Applications that implement their own menu system
4. **Simplified Interfaces**: Screens that don't need text editing capabilities

## Menu Functionality

The default menu bar in EBS screens includes:
- **Edit Menu**:
  - Cut (Ctrl+X)
  - Copy (Ctrl+C)
  - Paste (Ctrl+V)
  - Undo (Ctrl+Z)
  - Redo (Ctrl+Y)
  - Close (Ctrl+W)

When `showMenu` is set to `false`, all these menu items and keyboard shortcuts are removed from the screen.

## Layout Considerations

When the menu bar is hidden (`showMenu: false`):
- The status bar remains at the bottom of the window
- Content area fills the entire space where the menu bar would have been
- No extra space or padding is added in place of the menu bar

## Testing

To test the feature, create two screens side by side - one with the menu and one without:

```ebs
// Load and run test_menu_visibility.ebs
```

The test script creates two screens for comparison:
1. A screen with the default menu bar visible
2. A screen without the menu bar

## Notes

- The property only affects the top menu bar, not other UI elements
- The status bar at the bottom is always displayed regardless of this setting
- Keyboard shortcuts from the menu (Ctrl+C, Ctrl+V, etc.) are also removed when the menu is hidden
- This property is per-screen, so different screens can have different menu visibility settings
