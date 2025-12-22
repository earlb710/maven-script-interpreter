# Button Shortcut Property

## Overview

The `shortcut` property allows you to define keyboard shortcuts for buttons in EBS screen definitions. When specified, the button will respond to the keyboard shortcut in addition to mouse clicks, and the button label will automatically show the shortcut key underlined with a tooltip displaying the full shortcut combination.

## Feature Added

**Date**: December 2024  
**Version**: EBS Script Interpreter  
**Location**: Screen button `display` property

## Usage

### Basic Syntax

Add the `shortcut` property to a button's `display` object:

```javascript
{
    "name": "saveButton",
    "display": {
        "type": "button",
        "labelText": "Save",
        "shortcut": "Alt+S",
        "onClick": "call saveData();"
    }
}
```

### Supported Shortcut Formats

The `shortcut` property accepts strings in the format `Modifier+Key`:

- **Alt+Key**: `"Alt+S"`, `"Alt+L"`, `"Alt+E"`
- **Ctrl+Key**: `"Ctrl+R"`, `"Ctrl+S"`, `"Ctrl+N"`
- **Both modifiers**: `"Alt+Ctrl+X"`, `"Ctrl+Alt+Q"`

### Key Support

- **Letters**: A-Z (e.g., `"Alt+S"`)
- **Digits**: 0-9 (e.g., `"Alt+1"`)
- **Special keys**: Named JavaFX KeyCodes (e.g., `"Alt+Enter"`, `"Alt+F1"`)

## Visual Effects

When a button has a `shortcut` property:

1. **Underlined Character**: The matching character in the button label is automatically underlined
   - Example: "Save" with `"Alt+S"` becomes "S̲ave"

2. **Tooltip Enhancement**: A tooltip is added showing the shortcut combination
   - Example: "Shortcut: Alt+S"

3. **Keyboard Activation**: Pressing the shortcut key combination triggers the button's `onClick` handler

## Examples

### Simple Button with Alt Shortcut

```javascript
{
    "name": "loadButton",
    "display": {
        "type": "button",
        "labelText": "Load",
        "shortcut": "Alt+L",
        "onClick": "call loadFile();"
    }
}
```

**Result**: The "L" in "Load" is underlined, and pressing Alt+L will trigger the load action.

### Button with Ctrl Shortcut

```javascript
{
    "name": "runButton",
    "display": {
        "type": "button",
        "labelText": "Run Script",
        "shortcut": "Ctrl+R",
        "onClick": "call executeScript();"
    }
}
```

**Result**: The "R" in "Run" is underlined, and pressing Ctrl+R will execute the script.

### Complete Screen Example

```javascript
screen myApp = {
    "title": "My Application",
    "width": 600,
    "height": 400,
    "areas": [
        {
            "name": "toolbar",
            "type": "hbox",
            "spacing": 10,
            "items": [
                {
                    "name": "newButton",
                    "display": {
                        "type": "button",
                        "labelText": "New",
                        "shortcut": "Ctrl+N",
                        "onClick": "call createNew();"
                    }
                },
                {
                    "name": "openButton",
                    "display": {
                        "type": "button",
                        "labelText": "Open",
                        "shortcut": "Ctrl+O",
                        "onClick": "call openFile();"
                    }
                },
                {
                    "name": "saveButton",
                    "display": {
                        "type": "button",
                        "labelText": "Save",
                        "shortcut": "Ctrl+S",
                        "onClick": "call saveFile();"
                    }
                }
            ]
        }
    ]
};

show screen myApp;
```

### Dialog with Alt Shortcuts

```javascript
screen confirmDialog = {
    "title": "Confirm Action",
    "width": 400,
    "height": 200,
    "areas": [
        {
            "name": "buttonBar",
            "type": "hbox",
            "spacing": 10,
            "alignment": "center",
            "items": [
                {
                    "name": "yesButton",
                    "display": {
                        "type": "button",
                        "labelText": "Yes",
                        "shortcut": "Alt+Y",
                        "onClick": "confirmed = true; close screen confirmDialog;"
                    }
                },
                {
                    "name": "noButton",
                    "display": {
                        "type": "button",
                        "labelText": "No",
                        "shortcut": "Alt+N",
                        "onClick": "confirmed = false; close screen confirmDialog;"
                    }
                },
                {
                    "name": "cancelButton",
                    "display": {
                        "type": "button",
                        "labelText": "Cancel",
                        "shortcut": "Alt+C",
                        "onClick": "close screen confirmDialog;"
                    }
                }
            ]
        }
    ]
};
```

## Implementation Details

### Under the Hood

The shortcut property is implemented using three components:

1. **DisplayItem.java**: Stores the shortcut string
2. **InterpreterScreen.java**: Parses the shortcut from JSON screen definitions
3. **AreaItemFactory.java**: Applies the shortcut using `ButtonShortcutHelper`

### ButtonShortcutHelper Integration

The implementation leverages the existing `ButtonShortcutHelper` utility class which provides:
- Automatic character detection and underlining in button text
- Tooltip creation/enhancement with shortcut information
- Scene-level keyboard event handling for reliable shortcut detection
- Smart activation (only works when button is visible, enabled, and managed)

### Parsing Logic

The shortcut parser:
1. Splits the shortcut string by `+` character
2. Extracts modifiers (Alt, Ctrl/Control) from all parts except the last
3. Parses the final part as the key code
4. Supports both named keys (Enter, F1) and single characters (A-Z, 0-9)

## Best Practices

### Shortcut Selection

1. **Use Alt for UI Navigation**: Alt+key shortcuts are traditional for menu and dialog navigation
2. **Use Ctrl for Actions**: Ctrl+key shortcuts are conventional for application actions (Save, Copy, etc.)
3. **Avoid Conflicts**: Don't use shortcuts that conflict with system or browser shortcuts
4. **Choose Memorable Keys**: Pick keys that relate to the button label (S for Save, L for Load)

### Examples of Good Shortcuts

- File operations: `Ctrl+N` (New), `Ctrl+O` (Open), `Ctrl+S` (Save)
- Dialog buttons: `Alt+Y` (Yes), `Alt+N` (No), `Alt+C` (Cancel)
- Actions: `Ctrl+R` (Run), `Ctrl+E` (Export), `Ctrl+P` (Print)

### Shortcuts to Avoid

- `Ctrl+C`, `Ctrl+V`, `Ctrl+X` (copy, paste, cut - used by text fields)
- `Alt+F4` (window close on Windows)
- `Ctrl+W`, `Ctrl+T` (browser tab controls)
- `F5`, `Ctrl+R` in web contexts (browser refresh)

## Accessibility Benefits

Adding keyboard shortcuts to buttons improves accessibility:

1. **Keyboard-Only Navigation**: Users can navigate without a mouse
2. **Screen Reader Support**: Underlined text and tooltips are accessible to screen readers
3. **Motor Disability Support**: Reduces need for precise mouse movements
4. **Power User Efficiency**: Experienced users can work faster with keyboard shortcuts

## Limitations

1. **Button Controls Only**: The `shortcut` property only works with button-type controls
2. **Single Key**: Each shortcut can only use a single key (not key sequences)
3. **No Customization**: Users cannot currently customize shortcuts at runtime
4. **Visibility Required**: Shortcuts only work when the button is visible and enabled

## Troubleshooting

### Shortcut Not Working

**Problem**: Pressing the shortcut key doesn't trigger the button

**Possible Causes**:
1. Button is disabled or not visible
2. Shortcut conflicts with another control or system shortcut
3. Incorrect shortcut format (check for typos)
4. The key character doesn't appear in the button label (underlining won't work)

**Solutions**:
- Verify button is enabled and visible
- Choose a different, non-conflicting shortcut
- Check shortcut syntax: `"Modifier+Key"`
- Use a key that appears in the button text for best visual feedback

### Character Not Underlined

**Problem**: The button label doesn't show an underlined character

**Possible Causes**:
1. The key character doesn't appear in the button label
2. Case mismatch (though the matcher is case-insensitive)

**Solutions**:
- Ensure the button `labelText` contains the shortcut key character
- Example: `"Save"` with `"Alt+S"` works, but `"OK"` with `"Alt+S"` won't underline anything

## Related Features

- [BUTTON_SHORTCUTS.md](BUTTON_SHORTCUTS.md) - Complete keyboard shortcuts documentation
- [ButtonShortcutHelper.java](ScriptInterpreter/src/main/java/com/eb/ui/util/ButtonShortcutHelper.java) - Utility class source
- [EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md) - Complete EBS syntax reference

## Migration from Manual Shortcuts

If you previously used the `ButtonShortcutHelper` class directly in custom code:

**Before** (manual Java code):
```java
Button saveBtn = new Button("Save");
ButtonShortcutHelper.addAltShortcut(saveBtn, KeyCode.S);
```

**After** (declarative EBS):
```javascript
{
    "name": "saveButton",
    "display": {
        "type": "button",
        "labelText": "Save",
        "shortcut": "Alt+S",
        "onClick": "call saveData();"
    }
}
```

## Future Enhancements

Potential improvements for future versions:

1. **User Customization**: Allow users to customize shortcuts via settings
2. **Conflict Detection**: Warn about duplicate shortcuts in a screen
3. **Help Dialog**: Show all available shortcuts in current context
4. **Key Sequences**: Support multi-key sequences like "Ctrl+K, Ctrl+S"
5. **Global Shortcuts**: Screen-independent shortcuts that work app-wide

## See Also

- **Screen/UI Windows** section in [EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md)
- **Button Controls** documentation in [SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md](SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md)
- **ButtonShortcutHelper** source code for implementation details
