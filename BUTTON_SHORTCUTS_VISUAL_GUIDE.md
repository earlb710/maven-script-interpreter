# Button Shortcuts - Visual Guide

This guide provides visual examples of how the button keyboard shortcuts feature works.

## How It Looks

### Before Enhancement
```
┌─────────┐
│  Save   │  ← Plain button text
└─────────┘
```

### After Enhancement
```
┌─────────┐
│  Save   │  ← "S" is underlined
└─────────┘
   ⌃
   └─ Press Alt+S to activate

Tooltip shows: "Shortcut: Alt+S"
```

## Visual Examples

### Example 1: Mail Configuration Dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Mail Configuration                                     × │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [Configuration Table]                                       │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌─────────────────┐  ┌────────┐       │
│  │ Add Configuration│  │ Add Gmail Template│  │ Remove │       │
│  └────────────────┘  └─────────────────┘  └────────┘       │
│     Alt+A                Alt+G                Alt+R          │
│                                                              │
│                          ┌──────┐  ┌───────┐               │
│                          │ Save │  │ Close │               │
│                          └──────┘  └───────┘               │
│                           Alt+S     Alt+C                    │
└──────────────────────────────────────────────────────────────┘
```

### Example 2: Script Editor Tab

```
┌──────────────────────────────────────────────────────────────┐
│  script.ebs                                            × │
├──────────────────────────────────────────────────────────────┤
│  Code:                                                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ // Your EBS script here                                │ │
│  │                                                        │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  Output:                                                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────┐  ┌───────┐                                        │
│  │ Run │  │ Clear │                                        │
│  └─────┘  └───────┘                                        │
│   Alt+U    Alt+L                                            │
└──────────────────────────────────────────────────────────────┘
```

### Example 3: Console Window

```
┌──────────────────────────────────────────────────────────────┐
│  EBS Console                                          ─ □ × │
├──────────────────────────────────────────────────────────────┤
│  [Output Area]                                               │
│                                                              │
│                                                              │
│  [Input Area]                                                │
│                                                              │
│  ┌───────┐  ┌───────┐  ┌────────┐                          │
│  │ Clear │  │ Reset │  │ Submit │                          │
│  └───────┘  └───────┘  └────────┘                          │
│   Alt+L      Alt+R      Alt+U                               │
└──────────────────────────────────────────────────────────────┘
```

## Underline Visualization

Here's how the underlined characters appear in different buttons:

| Button Label | Underlined Character | Shortcut |
|--------------|----------------------|----------|
| **S**ave     | S (uppercase)        | Alt+S    |
| **C**lose    | C (uppercase)        | Alt+C    |
| **O**K       | O (uppercase)        | Alt+O    |
| **A**dd      | A (uppercase)        | Alt+A    |
| **R**emove   | R (uppercase)        | Alt+R    |
| **B**rowse   | B (uppercase)        | Alt+B    |
| **E**xport   | E (uppercase)        | Alt+E    |
| **T**est     | T (uppercase)        | Alt+T    |
| R**u**n      | u (lowercase)        | Alt+U    |
| C**l**ear    | l (lowercase)        | Alt+L    |
| **N**ext     | N (uppercase)        | Alt+N    |
| **P**rev     | P (uppercase)        | Alt+P    |
| **V**iew     | V (uppercase)        | Alt+V    |

## Tooltip Display

When you hover over a button with a shortcut, the tooltip shows:

```
┌──────────────────────────────┐
│  Save                        │
│                              │
│  Shortcut: Alt+S             │
└──────────────────────────────┘
```

Or if the button already has a tooltip:

```
┌──────────────────────────────┐
│  Run                         │
│                              │
│  Run the EBS script          │
│  Shortcut: Alt+U             │
└──────────────────────────────┘
```

## Usage Flow

### Opening a Dialog and Using Shortcuts

```
1. User opens Mail Config dialog
   └─> Keyboard: Alt+C (Config menu) → Navigate to Mail

2. Dialog appears with buttons showing underlined letters
   └─> Visual: Save has "S" underlined

3. User wants to save changes
   └─> Keyboard: Press Alt+S
   
4. Button activates as if clicked
   └─> Result: Changes saved and dialog closes
```

### Find and Replace in Editor

```
1. User opens Find bar
   └─> Keyboard: Ctrl+F

2. Find bar shows with buttons
   └─> Visual: "Next", "Prev", "Replace", etc. with underlines

3. User searches for text
   └─> Keyboard: Type search text

4. User navigates matches
   └─> Keyboard: Alt+N (Next) or Alt+P (Prev)

5. User replaces current match
   └─> Keyboard: Alt+R (Replace)

6. User closes find bar
   └─> Keyboard: Alt+C (Close) or Esc
```

## Design Principles

### 1. Consistency
```
Same operations use same shortcuts across dialogs:
- Save → Alt+S
- Close → Alt+C
- OK → Alt+O
- Cancel → Alt+C
- Add → Alt+A
- Remove → Alt+R
```

### 2. Visual Feedback
```
Button Label: Save
             ↓
Underline:    S̲ave
             ↓
Tooltip:     "Shortcut: Alt+S"
             ↓
Action:      Alt+S pressed → Button fires
```

### 3. Accessibility
```
Keyboard Users → Can navigate entire app
Screen Readers → Read underlined text and tooltips
Motor Disabilities → Reduced need for precise clicking
Power Users → Faster workflow with keyboard
```

## Conflict Resolution

When multiple buttons might want the same shortcut:

### Option 1: Use Different Letters
```
"Save" → Alt+S
"Submit" → Alt+U (second letter)
"Close" → Alt+C
```

### Option 2: Context Separation
```
Dialog 1: Alt+S for "Save"
Dialog 2: Alt+S for "Search"
(Okay because they're never visible simultaneously)
```

### Option 3: Use Ctrl Instead of Alt
```
Alt+S → Menu access or dialog buttons
Ctrl+S → File operations
```

## Browser-Like Behavior

The button shortcuts work similar to accesskey in HTML:

```html
<!-- HTML analogy -->
<button accesskey="s">Save</button>

<!-- Becomes in our app -->
Button saveBtn = new Button("Save");
ButtonShortcutHelper.addAltShortcut(saveBtn, KeyCode.S);
```

Both result in:
- Underlined character in button
- Alt+key activates the button
- Tooltip shows the shortcut
