# Keyboard Shortcuts Reference

## Overview

The EBS Console application supports comprehensive keyboard navigation, including menu access, editor shortcuts, and button shortcuts. All buttons in the application have keyboard shortcuts with underlined characters and tooltip hints.

For detailed information about button shortcuts, see [BUTTON_SHORTCUTS.md](BUTTON_SHORTCUTS.md).

## Global Keyboard Shortcuts (Console Application)

Access menus quickly from anywhere in the console application using Alt+ key combinations:

| Shortcut | Menu | Description |
|----------|------|-------------|
| **Alt+F** | File | New file, Open, Save, Exit, Recent files |
| **Alt+E** | Edit | Cut, Copy, Paste, Undo, Redo, Find, Replace |
| **Alt+C** | Config | AI Setup, Database, Mail Server, FTP, Colors |
| **Alt+T** | Tools | Regex Tester, Thread Viewer |
| **Alt+S** | Screens | View and manage open screens |
| **Alt+H** | Help | Syntax Help |

## Screen Keyboard Shortcuts

When working with screens (EBS script windows), use these shortcuts:

| Shortcut | Action | Description |
|----------|--------|-------------|
| **Alt+E** | Focus Editor | Jumps to the first editable field in the screen |
| **Ctrl+D** | Debug Mode | Toggle debug panel (shows variables) |
| **Ctrl+P** | Screenshot | Capture screenshot to file |

## Existing Editor Shortcuts

These shortcuts work in script editor tabs:

### File Operations
| Shortcut | Action |
|----------|--------|
| **Ctrl+N** | New Script File |
| **Ctrl+O** | Open File |
| **Ctrl+S** | Save |
| **Ctrl+Shift+S** | Save As |
| **Ctrl+Q** | Exit Application |
| **Ctrl+W** | Close Tab |
| **Ctrl+1-9** | Open Recent File (1-9) |

### Editing
| Shortcut | Action |
|----------|--------|
| **Ctrl+X** | Cut |
| **Ctrl+C** | Copy |
| **Ctrl+V** | Paste |
| **Ctrl+Z** | Undo |
| **Ctrl+Y** | Redo |
| **Ctrl+F** | Find |
| **Ctrl+H** | Replace |
| **Ctrl+L** | Toggle Line Numbers |

### Advanced Editing
| Shortcut | Action |
|----------|--------|
| **Alt+Up** | Move Line(s) Up |
| **Alt+Down** | Move Line(s) Down |
| **Ctrl+Delete** | Delete Word/Spaces |
| **Tab** | Indent (with selection = indent all lines) |
| **Ctrl+Space** | Show Autocomplete |

### Console
| Shortcut | Action |
|----------|--------|
| **Ctrl+Enter** | Submit/Execute Command |
| **Ctrl+Up** | Previous History |
| **Ctrl+Down** | Next History |

## Project Management
| Shortcut | Action |
|----------|--------|
| **Ctrl+Shift+N** | New Project |
| **Ctrl+Shift+O** | Open Project |

## Button Shortcuts

All buttons in the application now support keyboard shortcuts:
- Look for **underlined letters** in button labels
- Press **Alt + [letter]** to activate the button
- Hover over buttons to see the shortcut in the tooltip

**Common Button Shortcuts:**
- **Alt+S**: Save (in most dialogs)
- **Alt+C**: Close/Cancel (in most dialogs)
- **Alt+O**: OK (in dialogs)
- **Alt+M**: Remove (in configuration dialogs)
- **Alt+A**: Add (in configuration dialogs)
- **Alt+R**: Replace/Refresh/Reset (context-dependent)

For a complete list of button shortcuts, see [BUTTON_SHORTCUTS.md](BUTTON_SHORTCUTS.md).

## Tips

1. **Menu Access**: Press Alt+[letter] to open a menu, then use arrow keys to navigate menu items, Enter to select, or Esc to close.

2. **Button Access**: Press Alt+[letter] to activate buttons. Look for underlined letters in button labels.

3. **Quick Editor Access**: In screens, press Alt+E to quickly jump to the first input field without using the mouse.

4. **Context-Aware**: Alt+E behaves differently in console (opens Edit menu) vs. screens (focuses editor), providing the most useful action in each context.

5. **Combine Shortcuts**: Use Alt+F followed by arrow keys to navigate the File menu entirely with keyboard.

6. **Find in Console**: Press Ctrl+F in the console output area to search through console output.

7. **Tooltip Hints**: Hover over any button to see its keyboard shortcut in the tooltip.

## Screen Editor Focus

The Alt+E shortcut in screens focuses the first editable control, which includes:
- Text fields (TextField)
- Text areas (TextArea)
- Combo boxes (ComboBox)
- Choice boxes (ChoiceBox)
- Date pickers (DatePicker)
- Color pickers (ColorPicker)
- Spinners (Spinner)
- Script areas (ScriptArea)

## Accessibility

All keyboard shortcuts are designed to work with:
- Screen readers
- Keyboard-only navigation
- Standard accessibility tools

No mouse is required to operate the entire application.

## Platform Compatibility

These shortcuts follow standard desktop application conventions:
- **Windows**: Alt key for menu access
- **Linux**: Alt key for menu access
- **macOS**: Alt/Option key for menu access

Note: On some systems, the window manager may intercept Alt+ combinations. Check your system settings if shortcuts don't work as expected.
