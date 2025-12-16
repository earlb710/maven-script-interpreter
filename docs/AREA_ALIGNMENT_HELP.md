# Area Alignment Help Guide

## Quick Reference

The `alignment` property controls how child elements are positioned within container areas (HBox, VBox, StackPane, FlowPane, TilePane).

## Syntax

```javascript
"area": [
    {
        "name": "myArea",
        "type": "hbox",        // or vbox, stackpane, flowpane, tilepane
        "alignment": "center",  // alignment value
        "items": [...]
    }
]
```

## Available Values

### Shorthand (Common Use)

| Value | Effect | Use When |
|-------|--------|----------|
| `"left"` | Left edge, vertically centered | Aligning content to the left |
| `"right"` | Right edge, vertically centered | Right-aligned button bars, menus |
| `"top"` | Top edge, horizontally centered | Top headers |
| `"bottom"` | Bottom edge, horizontally centered | Footers, status bars |
| `"center"` | Center both ways | Centered dialogs, login forms |

### Precise Positions (9-Point Grid)

```
┌─────────────────────────────────┐
│ top-left   top-center  top-right│
│                                  │
│center-left   center  center-right│
│                                  │
│bottom-left bottom-center bottom-right│
└─────────────────────────────────┘
```

| Value | Position |
|-------|----------|
| `"top-left"` | Top-left corner |
| `"top-center"` | Top edge, centered horizontally |
| `"top-right"` | Top-right corner |
| `"center-left"` | Left edge, centered vertically |
| `"center"` | Center both horizontally and vertically |
| `"center-right"` | Right edge, centered vertically |
| `"bottom-left"` | Bottom-left corner |
| `"bottom-center"` | Bottom edge, centered horizontally |
| `"bottom-right"` | Bottom-right corner |

### Baseline (Text Alignment)

| Value | Use For |
|-------|---------|
| `"baseline-left"` | Text-based layouts, left-aligned |
| `"baseline-center"` | Text-based layouts, center-aligned |
| `"baseline-right"` | Text-based layouts, right-aligned |

## Container-Specific Behavior

### HBox (Horizontal Box)
- Children flow **left to right**
- Alignment affects **vertical position** (top, center, bottom)
- Default: `center-left`

```javascript
{
    "name": "horizontalBar",
    "type": "hbox",
    "alignment": "center",  // Centers items vertically within the HBox
    "items": [...]
}
```

### VBox (Vertical Box)
- Children flow **top to bottom**
- Alignment affects **horizontal position** (left, center, right)
- Default: `top-center`

```javascript
{
    "name": "verticalForm",
    "type": "vbox",
    "alignment": "center",  // Centers items horizontally within the VBox
    "items": [...]
}
```

### StackPane
- Children **overlay each other**
- Alignment affects position of all stacked children
- Default: `center`

```javascript
{
    "name": "overlay",
    "type": "stackpane",
    "alignment": "top-left",  // All children positioned at top-left
    "items": [...]
}
```

### FlowPane
- Children **wrap** when they exceed width
- Alignment determines starting position
- Default: `top-left`

### TilePane
- Children arranged in **uniform grid**
- Alignment determines starting position
- Default: `top-left`

## Common Patterns

### Pattern 1: Centered Login Form
```javascript
screen loginScreen = {
    "title": "Login",
    "width": 400,
    "height": 300,
    "vars": [
        {"name": "username", "type": "string", "default": ""},
        {"name": "password", "type": "string", "default": ""},
        {"name": "loginBtn", "type": "string", "default": ""}
    ],
    "area": [
        {
            "name": "loginForm",
            "type": "vbox",
            "alignment": "center",      // Center all fields horizontally
            "spacing": "15",
            "padding": "30",
            "items": [
                {"varRef": "username", "sequence": 1,
                 "display": {"type": "textfield", "labelText": "Username:"}},
                {"varRef": "password", "sequence": 2,
                 "display": {"type": "passwordfield", "labelText": "Password:"}},
                {"varRef": "loginBtn", "sequence": 3,
                 "display": {"type": "button", "labelText": "Login"}}
            ]
        }
    ]
};
```

### Pattern 2: Right-Aligned Action Buttons
```javascript
{
    "name": "buttonBar",
    "type": "hbox",
    "alignment": "right",       // Push buttons to the right
    "spacing": "10",
    "padding": "10 20",
    "items": [
        {"varRef": "cancelBtn", "sequence": 1,
         "display": {"type": "button", "labelText": "Cancel"}},
        {"varRef": "saveBtn", "sequence": 2,
         "display": {"type": "button", "labelText": "Save"}}
    ]
}
```

### Pattern 3: Header/Content/Footer Layout
```javascript
{
    "name": "mainLayout",
    "type": "vbox",
    "spacing": "0",
    "items": [],
    "areas": [
        {
            "name": "header",
            "type": "hbox",
            "alignment": "center",          // Centered header
            "padding": "20",
            "areaBackground": "#2c3e50",
            "items": [...]
        },
        {
            "name": "content",
            "type": "vbox",
            "alignment": "top-left",        // Content starts at top-left
            "padding": "20",
            "items": [...]
        },
        {
            "name": "footer",
            "type": "hbox",
            "alignment": "bottom-right",    // Footer info at bottom-right
            "padding": "10",
            "areaBackground": "#ecf0f1",
            "items": [...]
        }
    ]
}
```

### Pattern 4: Dashboard with Cards
```javascript
{
    "name": "dashboard",
    "type": "flowpane",
    "alignment": "top-center",      // Center cards horizontally, start at top
    "spacing": "20",
    "padding": "30",
    "items": [
        // Cards will wrap and stay centered
        {"varRef": "card1", "sequence": 1},
        {"varRef": "card2", "sequence": 2},
        {"varRef": "card3", "sequence": 3}
    ]
}
```

## Dynamic Alignment Changes

You can change alignment at runtime using builtins:

```javascript
// Get current alignment
var currentAlign = call scr.getAreaProperty("myScreen.buttonBar", "alignment");
print "Current alignment: " + currentAlign;

// Change alignment dynamically
call scr.setAreaProperty("myScreen.buttonBar", "alignment", "left");

// Toggle between alignments
if currentAlign == "left" then
    call scr.setAreaProperty("myScreen.buttonBar", "alignment", "right");
else
    call scr.setAreaProperty("myScreen.buttonBar", "alignment", "left");
end if;
```

## Combining with Other Properties

Alignment works alongside other area properties:

```javascript
{
    "name": "styledArea",
    "type": "vbox",
    "alignment": "center",              // Center children
    "spacing": "15",                    // 15px gap between children
    "padding": "20 30",                 // 20px top/bottom, 30px left/right
    "areaBackground": "#f5f5f5",        // Light gray background
    "style": "-fx-border-color: #ddd; -fx-border-width: 1;",
    "items": [...]
}
```

## Troubleshooting

### Q: My alignment isn't working
**A:** Check these common issues:
1. Verify container type supports alignment (HBox, VBox, StackPane, FlowPane, TilePane)
2. Confirm alignment value spelling (case-insensitive but must match valid values)
3. Check if CSS `style` property overrides alignment with `-fx-alignment`

### Q: Children don't align as expected
**A:** Remember:
- **HBox**: Alignment affects vertical position (not horizontal)
- **VBox**: Alignment affects horizontal position (not vertical)
- Children's natural size affects positioning
- Use `prefWidth`/`prefHeight` on items for consistent sizing

### Q: How do I center items in a GridPane?
**A:** GridPane doesn't support the alignment property. Use:
```javascript
"style": "-fx-alignment: center;"
```

## Default Alignments Reference

When no `alignment` property is specified:

| Container | Default | Effect |
|-----------|---------|--------|
| HBox | `center-left` | Children at left, centered vertically |
| VBox | `top-center` | Children at top, centered horizontally |
| StackPane | `center` | All children centered |
| FlowPane | `top-left` | Start wrapping from top-left |
| TilePane | `top-left` | Grid starts at top-left |

## See Also

- **[CONTAINER_ALIGNMENT_OPTIONS.md](../CONTAINER_ALIGNMENT_OPTIONS.md)** - Complete reference
- **[AREA_DEFINITION.md](AREA_DEFINITION.md)** - Full area documentation
- **[EBS_SCRIPT_SYNTAX.md](EBS_SCRIPT_SYNTAX.md)** - Language syntax reference
