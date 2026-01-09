# Button Shortcut Property - Visual Guide

This guide provides visual representations of how the button shortcut property works in EBS screen definitions.

## Basic Concept

```
┌─────────────────────────────────────────────────────┐
│  Before: Regular Button (no shortcut)              │
├─────────────────────────────────────────────────────┤
│                                                     │
│         ┌─────────┐                                │
│         │  Save   │  ← Plain button                │
│         └─────────┘                                │
│                                                     │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  After: Button with Shortcut Property              │
├─────────────────────────────────────────────────────┤
│                                                     │
│         ┌─────────┐                                │
│         │  S̲ave   │  ← 'S' is underlined           │
│         └─────────┘                                │
│              ▲                                      │
│              │ Hover shows tooltip:                │
│              └─────────────────┐                   │
│                                │                   │
│         ┌──────────────────────┴─────────────┐    │
│         │  Shortcut: Alt+S                   │    │
│         └────────────────────────────────────┘    │
│                                                     │
│    Pressing Alt+S triggers the button!             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Screen Definition Flow

```
EBS Script Definition
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
{
    "name": "saveButton",
    "display": {
        "type": "button",
        "labelText": "Save",
        "shortcut": "Alt+S",      ← New property!
        "onClick": "call saveData();"
    }
}
        │
        │ 1. Parse JSON
        ▼
┌───────────────────────┐
│ InterpreterScreen     │
│ parseDisplayItem()    │
│                       │
│ Extracts "shortcut"   │
│ from JSON             │
└──────────┬────────────┘
           │
           │ 2. Store in DisplayItem
           ▼
┌───────────────────────┐
│ DisplayItem           │
│                       │
│ shortcut = "Alt+S"    │
└──────────┬────────────┘
           │
           │ 3. Create control
           ▼
┌───────────────────────┐
│ AreaItemFactory       │
│ createItem()          │
│                       │
│ Creates Button        │
│ Applies shortcut      │
└──────────┬────────────┘
           │
           │ 4. Apply shortcut
           ▼
┌───────────────────────┐
│ ButtonShortcutHelper  │
│                       │
│ - Underlines 'S'      │
│ - Adds tooltip        │
│ - Registers listener  │
└──────────┬────────────┘
           │
           │ 5. Render
           ▼
┌───────────────────────┐
│     ┌─────────┐       │
│     │  S̲ave   │       │
│     └─────────┘       │
│                       │
│  JavaFX Button with   │
│  keyboard shortcut    │
└───────────────────────┘
```

## Shortcut Format Parsing

```
Input String: "Alt+S"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1: Split by '+' character
┌─────┬───┬───┐
│ Alt │ + │ S │
└─────┴───┴───┘
   │       │
   │       └─→ Last part = Key
   └─────────→ Other parts = Modifiers

Step 2: Extract modifiers
┌──────────────────────────┐
│ "Alt"  → useAlt = true   │
│ "Ctrl" → useCtrl = true  │
└──────────────────────────┘

Step 3: Parse key code
┌──────────────────────────┐
│ "S" → KeyCode.S          │
│ "1" → KeyCode.DIGIT1     │
│ "Enter" → KeyCode.ENTER  │
└──────────────────────────┘

Step 4: Apply to button
┌───────────────────────────────┐
│ ButtonShortcutHelper.         │
│   addShortcut(button,         │
│               KeyCode.S,      │
│               useAlt=true,    │
│               useCtrl=false)  │
└───────────────────────────────┘
```

## Example: Multiple Buttons

```
Screen with Multiple Shortcuts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────────────────────────────────────────┐
│  File Manager                                     [×]    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Toolbar                                         │    │
│  ├────────────────────────────────────────────────┤    │
│  │                                                 │    │
│  │  ┌─────┐  ┌──────┐  ┌─────┐  ┌──────┐         │    │
│  │  │ N̲ew  │  │ O̲pen  │  │ S̲ave │  │ E̲xit  │         │    │
│  │  └─────┘  └──────┘  └─────┘  └──────┘         │    │
│  │   Ctrl+N   Ctrl+O    Ctrl+S   Alt+X           │    │
│  │                                                 │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘

Each button has:
├─ Underlined character matching the shortcut key
├─ Tooltip showing the full shortcut combination
└─ Keyboard activation via the shortcut
```

## User Interaction

```
User Presses Keyboard Shortcut
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────┐
│ User presses Alt+S                       │
└─────────────┬────────────────────────────┘
              │
              │ JavaFX KeyEvent
              ▼
┌───────────────────────────────────────────┐
│ Scene-level Event Filter                  │
│ (registered by ButtonShortcutHelper)      │
│                                            │
│ Checks:                                    │
│  ✓ Alt key pressed?     → Yes             │
│  ✓ Ctrl key pressed?    → No              │
│  ✓ KeyCode == 'S'?      → Yes             │
│  ✓ Button visible?      → Yes             │
│  ✓ Button enabled?      → Yes             │
└─────────────┬──────────────────────────────┘
              │
              │ All checks passed!
              ▼
┌───────────────────────────────────────────┐
│ button.fire()                              │
│                                            │
│ Triggers the onClick handler              │
└─────────────┬──────────────────────────────┘
              │
              ▼
┌───────────────────────────────────────────┐
│ Execute EBS code:                          │
│ "call saveData();"                         │
└────────────────────────────────────────────┘
```

## Visual Feedback Examples

### Dialog with Alt Shortcuts

```
┌───────────────────────────────────────────┐
│  Confirm Action                     [×]   │
├───────────────────────────────────────────┤
│                                           │
│  Are you sure you want to continue?      │
│                                           │
│                                           │
│         ┌──────┐  ┌─────┐  ┌────────┐   │
│         │  Y̲es  │  │  N̲o  │  │ C̲ancel  │   │
│         └──────┘  └─────┘  └────────┘   │
│          Alt+Y     Alt+N     Alt+C       │
│                                           │
└───────────────────────────────────────────┘

User can quickly respond:
- Press Alt+Y to confirm
- Press Alt+N to decline
- Press Alt+C to cancel
```

### Form with Save/Cancel

```
┌────────────────────────────────────────────────┐
│  Edit Settings                           [×]   │
├────────────────────────────────────────────────┤
│                                                │
│  Name:     [John Doe                    ]     │
│                                                │
│  Email:    [john@example.com            ]     │
│                                                │
│  Phone:    [555-0123                    ]     │
│                                                │
│                                                │
│               ┌──────┐    ┌────────┐          │
│               │ S̲ave  │    │ C̲ancel  │          │
│               └──────┘    └────────┘          │
│                Alt+S       Alt+C              │
│                                                │
└────────────────────────────────────────────────┘

After filling the form:
- Press Alt+S to save without reaching for mouse
- Press Alt+C to cancel and close
```

## Tooltip Display

```
When hovering over a button with shortcut:

┌─────────┐
│  S̲ave   │ ←── Button with underlined 'S'
└────┬────┘
     │
     │ (hover)
     ▼
┌──────────────────────┐
│ Shortcut: Alt+S      │ ←── Tooltip appears
└──────────────────────┘

The tooltip shows:
✓ Clear indication this is a shortcut
✓ Modifier keys (Alt, Ctrl)
✓ The actual key to press
```

## Character Matching Priority

```
Button Label: "Save File"
Shortcut: "Alt+S"

Step 1: Search for uppercase 'S'
┌───────────────┐
│ Save File     │
│ ^             │
│ Found 'S'!    │
└───────────────┘

Result: S̲ave File (underlines first S)

───────────────────────────────────────

Button Label: "close"
Shortcut: "Alt+C"

Step 1: Search for uppercase 'C'
┌───────────────┐
│ close         │
│ Not found     │
└───────────────┘

Step 2: Search for lowercase 'c'
┌───────────────┐
│ close         │
│ ^             │
│ Found 'c'!    │
└───────────────┘

Result: c̲lose (underlines first c)

───────────────────────────────────────

Button Label: "OK"
Shortcut: "Alt+S"

Step 1: Search for 'S'
┌───────────────┐
│ OK            │
│ Not found     │
└───────────────┘

Result: OK (no underline, shortcut still works)
```

## Complete Example Screen

```
EBS Script Definition:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

screen fileManager = {
    "title": "File Manager",
    "width": 600,
    "height": 400,
    "areas": [
        {
            "name": "toolbar",
            "type": "hbox",
            "spacing": 10,
            "padding": 10,
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

Results in:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────────────────────────────────┐
│  File Manager                             [×]   │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │ ┌─────┐  ┌──────┐  ┌─────┐              │  │
│  │ │ N̲ew  │  │ O̲pen  │  │ S̲ave │              │  │
│  │ └─────┘  └──────┘  └─────┘              │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│    Keyboard shortcuts:                         │
│    • Ctrl+N - Create new file                  │
│    • Ctrl+O - Open existing file               │
│    • Ctrl+S - Save current file                │
│                                                 │
└─────────────────────────────────────────────────┘
```

## Supported Shortcut Combinations

```
┌────────────────────────────────────────────────────┐
│ Modifier Combinations                              │
├────────────────────────────────────────────────────┤
│                                                    │
│  Alt + Key                                         │
│  ┌──────────────────────────────────────────┐    │
│  │ "Alt+S"   → Alt + S key                  │    │
│  │ "Alt+1"   → Alt + 1 key                  │    │
│  │ "Alt+F1"  → Alt + F1 function key        │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  Ctrl + Key                                        │
│  ┌──────────────────────────────────────────┐    │
│  │ "Ctrl+S"  → Ctrl + S key                 │    │
│  │ "Ctrl+N"  → Ctrl + N key                 │    │
│  │ "Ctrl+1"  → Ctrl + 1 key                 │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  Alt + Ctrl + Key                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ "Alt+Ctrl+X" → Alt + Ctrl + X key        │    │
│  │ "Ctrl+Alt+Q" → Same (order doesn't matter)│    │
│  └──────────────────────────────────────────┘    │
│                                                    │
└────────────────────────────────────────────────────┘
```

## Error Handling

```
Invalid Shortcut Examples:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Missing modifier:
   "shortcut": "S"
   
   Result: ⚠ Warning logged, shortcut ignored
           Button still works for mouse clicks

2. Invalid key:
   "shortcut": "Alt+InvalidKey"
   
   Result: ⚠ Warning logged, shortcut ignored
           Button still works for mouse clicks

3. Empty shortcut:
   "shortcut": ""
   
   Result: ⚠ Silently ignored
           Button works normally without shortcut

4. Missing key:
   "shortcut": "Alt+"
   
   Result: ⚠ Warning logged, shortcut ignored
           Button still works for mouse clicks

In all cases, the button remains functional!
Only the shortcut feature is disabled.
```

## Best Practices Visualization

```
✓ GOOD SHORTCUTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

File Operations:
┌─────┐  ┌──────┐  ┌─────┐  ┌───────┐
│ N̲ew  │  │ O̲pen  │  │ S̲ave │  │ P̲rint  │
└─────┘  └──────┘  └─────┘  └───────┘
Ctrl+N   Ctrl+O   Ctrl+S   Ctrl+P
└─→ Standard, memorable shortcuts

Dialog Buttons:
┌──────┐  ┌─────┐  ┌────────┐
│  Y̲es  │  │  N̲o  │  │ C̲ancel  │
└──────┘  └─────┘  └────────┘
 Alt+Y     Alt+N     Alt+C
└─→ First letter, easy to remember

✗ POOR SHORTCUTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Conflicts with system:
┌──────┐
│ Copy │
└──────┘
Ctrl+C  ← Bad! Used for text copy

Conflicts with browser:
┌─────────┐
│ Refresh │
└─────────┘
Ctrl+R  ← Bad! Browser refresh

Non-intuitive:
┌─────┐
│ Save │
└─────┘
Alt+Q  ← Bad! 'Q' not in 'Save'
```

## Summary

```
┌─────────────────────────────────────────────────────┐
│ Button Shortcut Property - At a Glance             │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Property Name:  "shortcut"                        │
│  Location:       Button display object             │
│  Format:         "Modifier+Key"                    │
│  Examples:       "Alt+S", "Ctrl+R", "Alt+Ctrl+X"  │
│                                                     │
│  Visual Effects:                                   │
│   ✓ Underlines matching character                 │
│   ✓ Adds tooltip with shortcut info               │
│   ✓ Enables keyboard activation                   │
│                                                     │
│  Benefits:                                         │
│   ✓ Keyboard-first workflow                       │
│   ✓ Improved accessibility                        │
│   ✓ Power user efficiency                         │
│   ✓ No mouse required                             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## See Also

- [BUTTON_SHORTCUT_PROPERTY.md](BUTTON_SHORTCUT_PROPERTY.md) - Complete documentation
- [BUTTON_SHORTCUT_PROPERTY_IMPLEMENTATION.md](BUTTON_SHORTCUT_PROPERTY_IMPLEMENTATION.md) - Implementation details
- [EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md) - EBS syntax reference
- [test_button_shortcut.ebs](test_button_shortcut.ebs) - Working example
