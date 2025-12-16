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
