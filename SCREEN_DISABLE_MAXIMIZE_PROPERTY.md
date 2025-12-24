# Screen Disable Maximize Property

## Overview

This document describes the `disableMaximize` property for EBS screen definitions, which controls whether users can maximize screen windows.

## Feature Description

The `disableMaximize` property is a boolean flag that can be set in screen JSON definitions to prevent users from maximizing the window. When set to `true`, the maximize button will be disabled and any attempts to maximize the window (via button, keyboard shortcut, or programmatic calls) will be prevented.

This is useful for:
- Dialog-style windows that should maintain a fixed size
- Utility windows that don't need to be maximized
- Preventing layout issues that might occur with maximized windows
- Creating a more controlled user interface experience

## Property Details

- **Property Name**: `disableMaximize`
- **Type**: Boolean
- **Default Value**: `false` (window can be maximized by default)
- **Location**: Top-level property in screen JSON definition
- **Compatibility**: Works alongside the `resizable` property

## Usage

### Syntax

In your screen definition JSON, add the `disableMaximize` property at the top level:

```ebs
screen myScreen = {
    "title": "My Screen",
    "width": 800,
    "height": 600,
    "disableMaximize": true,  // Prevents window maximization
    "area": [
        // ... area definitions
    ]
};
```

### Example: Screen WITHOUT Maximize Capability

```ebs
screen dialogScreen = {
    "title": "Settings Dialog",
    "width": 600,
    "height": 400,
    "resizable": true,
    "disableMaximize": true,  // Window can be resized but not maximized
    "vars": [
        {
            "name": "setting1",
            "type": "string",
            "default": "Value 1",
            "display": {
                "type": "textfield",
                "labelText": "Setting 1:"
            }
        },
        {
            "name": "setting2",
            "type": "string",
            "default": "Value 2",
            "display": {
                "type": "textfield",
                "labelText": "Setting 2:"
            }
        }
    ]
};

show screen dialogScreen;
```

### Example: Screen WITH Maximize Capability (Default)

```ebs
screen normalScreen = {
    "title": "Normal Application Window",
    "width": 800,
    "height": 600,
    // disableMaximize is false by default, so window can be maximized
    "vars": [
        {
            "name": "content",
            "type": "string",
            "default": "This window can be maximized normally.",
            "display": {
                "type": "textarea",
                "labelText": "Content:",
                "height": 10
            }
        }
    ]
};

show screen normalScreen;
```

### Example: Combining with Other Window Properties

```ebs
screen fixedSizeDialog = {
    "title": "Fixed Size Dialog",
    "width": 500,
    "height": 300,
    "resizable": false,        // Window cannot be resized
    "disableMaximize": true,   // Window cannot be maximized
    "showMenu": false,         // No menu bar
    "vars": [
        {
            "name": "message",
            "type": "string",
            "default": "This is a fixed-size, non-maximizable dialog.",
            "display": {
                "type": "label",
                "labelText": "Message:"
            }
        }
    ]
};

show screen fixedSizeDialog;
```

## Implementation Details

When `disableMaximize` is set to `true`:

1. The window is initially set to non-maximized state
2. A property listener is attached to the JavaFX Stage's `maximizedProperty()`
3. Any attempt to maximize the window is immediately reverted
4. This works for all maximization methods:
   - Clicking the maximize button in the window title bar
   - Using keyboard shortcuts (e.g., Win+Up on Windows)
   - Programmatic calls to maximize the window
   - Double-clicking the title bar

## Interaction with Other Properties

### With `maximize` Property

If both `maximize: true` and `disableMaximize: true` are set:
- The `disableMaximize` takes precedence
- The window will not be maximized on show
- The window will prevent any maximization attempts

### With `resizable` Property

The properties work independently:
- `resizable: false, disableMaximize: false` - Window cannot be resized but can be maximized
- `resizable: true, disableMaximize: true` - Window can be resized but cannot be maximized
- `resizable: false, disableMaximize: true` - Window cannot be resized or maximized (fully fixed)

## Testing

To test the `disableMaximize` property:

1. Create a screen with `disableMaximize: true`
2. Show the screen
3. Attempt to maximize the window using:
   - The maximize button in the title bar
   - Keyboard shortcuts (varies by OS)
   - Double-clicking the title bar (on some systems)
4. The window should remain in its normal (non-maximized) state

## Troubleshooting

### Window Still Maximizes

If the window can still be maximized despite setting `disableMaximize: true`:

1. Check that the property is spelled correctly (case-insensitive: `disableMaximize` or `disablemaximize`)
2. Verify the property is at the top level of the screen JSON, not nested
3. Ensure the value is a boolean (`true` or `false`), not a string

### Maximize Button Still Visible

Note that `disableMaximize` prevents maximization functionality but does not hide the maximize button in the window title bar. The button remains visible but becomes non-functional. This is a limitation of the JavaFX platform.

## Related Properties

- `resizable` - Controls whether the window can be resized
- `maximize` - Controls whether the window starts in maximized state
- `showMenu` - Controls whether the menu bar is displayed
- `width` and `height` - Set the initial window dimensions

## Version History

- **Version 1.0** (December 2025) - Initial implementation of `disableMaximize` property
