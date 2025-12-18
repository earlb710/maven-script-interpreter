# Button Keyboard Shortcuts

This document describes the button keyboard shortcut enhancement feature implemented in the EBS Console application.

## Overview

All buttons in the EBS Console application now support keyboard shortcuts, allowing users to interact with the application entirely via keyboard without needing to use the mouse. Each button has an underlined character in its label and displays the shortcut key combination in its tooltip.

## Implementation Details

### ButtonShortcutHelper Utility

A new utility class `ButtonShortcutHelper` (`com.eb.ui.util.ButtonShortcutHelper`) provides convenient methods to add keyboard shortcuts to buttons:

```java
// Add Alt+S shortcut to a Save button
Button saveBtn = new Button("Save");
ButtonShortcutHelper.addAltShortcut(saveBtn, KeyCode.S);

// Add Ctrl+R shortcut to a Run button
Button runBtn = new Button("Run");
ButtonShortcutHelper.addCtrlShortcut(runBtn, KeyCode.R);
```

### Features

1. **Automatic Character Underlining**: The utility automatically finds and underlines the matching character in the button label
2. **Tooltip Enhancement**: Adds shortcut information to the button's tooltip (creates one if it doesn't exist)
3. **Smart Activation**: Only activates when the button is visible, managed, and enabled
4. **Event Filtering**: Uses scene-level event filtering to capture shortcuts globally

## Button Shortcuts by Component

### Configuration Dialogs

#### Mail Configuration Dialog
- **Alt+A**: Add Configuration
- **Alt+G**: Add Gmail Template
- **Alt+R**: Remove
- **Alt+S**: Save
- **Alt+C**: Close

#### Database Configuration Dialog
- **Alt+A**: Add Configuration
- **Alt+R**: Remove
- **Alt+O**: Copy Connection String
- **Alt+S**: Save
- **Alt+C**: Close

#### FTP Configuration Dialog
- **Alt+A**: Add Configuration
- **Alt+R**: Remove
- **Alt+S**: Save
- **Alt+C**: Close

#### Safe Directories Dialog
- **Alt+A**: Add Directory
- **Alt+R**: Remove
- **Alt+O**: Copy to Clipboard
- **Alt+B**: Browse
- **Alt+S**: Save
- **Alt+C**: Close

#### AI Chat Model Setup Dialog
- **Alt+S**: Save
- **Alt+T**: Test
- **Alt+C**: Close

### Project Management Dialogs

#### New File Dialog
- **Alt+B**: Browse (select directory)
- **Alt+C**: Create (OK button)

#### New Project Dialog
- **Alt+B**: Browse (select directory)
- **Alt+O**: OK

#### Thread Viewer Dialog
- **Alt+R**: Refresh
- **Alt+T**: Stop Screen Thread
- **Alt+C**: Close

#### Export Configuration Dialog
- **Alt+E**: Export
- **Alt+C**: Cancel

### Main Application UI

#### Console Window
- **Alt+L**: Clear (output)
- **Alt+R**: Reset (console and all screens)
- **Alt+U**: Submit (execute input)

#### Script Editor Tab (EbsTab)

**Find/Replace Bar:**
- **Alt+P**: Prev (previous match)
- **Alt+N**: Next (next match)
- **Alt+R**: Replace (current match)
- **Alt+A**: Replace All
- **Alt+C**: Close (find bar)

**Script Execution:**
- **Alt+U**: Run (execute script)
- **Alt+L**: Clear (output)
- **Alt+V**: View (for HTML/Markdown files)

## Usage Guidelines

### For Users

1. **Look for underlined letters**: Each button shows which character is underlined in its label
2. **Press Alt + the underlined letter**: Hold Alt and press the underlined letter to activate the button
3. **Check tooltips**: Hover over buttons to see the exact shortcut key combination

### For Developers

When adding new buttons to the application:

```java
// Import the helper
import com.eb.ui.util.ButtonShortcutHelper;
import javafx.scene.input.KeyCode;

// Create button
Button myButton = new Button("Execute");

// Add shortcut (Alt+E for the "E" in "Execute")
ButtonShortcutHelper.addAltShortcut(myButton, KeyCode.E);

// Or use Ctrl instead of Alt
ButtonShortcutHelper.addCtrlShortcut(myButton, KeyCode.E);
```

### Best Practices

1. **Choose obvious letters**: Use the first letter of the button text when possible
2. **Avoid conflicts**: Don't use the same Alt+key combination for multiple visible buttons
3. **Use Alt for dialogs**: Use Alt+key for dialog buttons (consistent with menus)
4. **Use Ctrl for actions**: Use Ctrl+key for action buttons if they perform operations
5. **Document in tooltips**: The helper automatically adds tooltip info, but you can add more context

## Accessibility Benefits

- **Keyboard-only navigation**: Complete application control without mouse
- **Screen reader friendly**: Underlined text and tooltips are accessible
- **Motor disability support**: Reduces need for precise mouse movements
- **Power user efficiency**: Faster interaction for experienced users

## Technical Notes

### Character Matching Algorithm

The helper uses this priority for finding matching characters:
1. Uppercase letters first (e.g., "S" in "Save")
2. Lowercase letters next (e.g., "s" in "close")
3. Returns -1 if no match found (button label won't have underline)

### Event Handling

- Shortcuts are registered at the Scene level using event filters
- Only active when button is visible, managed, and enabled
- Events are consumed after button activation to prevent propagation
- Modifier keys (Alt, Ctrl) are checked to ensure correct combination

### Label Rendering

- Original button text is cleared
- A TextFlow graphic is created with Text nodes
- The matching character is marked with `setUnderline(true)`
- This preserves the original visual appearance while adding the underline

## Future Enhancements

Potential improvements for future versions:

1. **Conflict Detection**: Warn developers if multiple buttons have the same shortcut in a scene
2. **Customization**: Allow users to customize keyboard shortcuts
3. **Help Dialog**: Show all available shortcuts in current context
4. **Visual Indicators**: Highlight button when Alt key is pressed
5. **Localization Support**: Handle different keyboard layouts and languages

## See Also

- [KEYBOARD_SHORTCUTS.md](KEYBOARD_SHORTCUTS.md) - Full keyboard shortcuts reference
- [ButtonShortcutHelper.java](ScriptInterpreter/src/main/java/com/eb/ui/util/ButtonShortcutHelper.java) - Source code
