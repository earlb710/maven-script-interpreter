# Container (Area) Alignment Options

## Overview

Containers (areas) in the EBS screen system support an `alignment` property that controls how child elements are positioned within the container. This property is available for the following container types:

- **HBox**: Horizontal box layout
- **VBox**: Vertical box layout  
- **StackPane**: Stacked layout (children overlay each other)
- **FlowPane**: Wrapping flow layout
- **TilePane**: Uniform tile grid layout

## Property Syntax

The `alignment` property is specified as a string value in the area definition:

```javascript
{
    "name": "myArea",
    "type": "hbox",
    "alignment": "center",
    "spacing": "10",
    "padding": "15",
    "items": [...]
}
```

## Available Alignment Values

### Shorthand Values

These simplified values map to commonly-used positions:

| Value | Maps To | Description |
|-------|---------|-------------|
| `"left"` | `center-left` | Left edge, vertically centered |
| `"right"` | `center-right` | Right edge, vertically centered |
| `"top"` | `top-center` | Top edge, horizontally centered |
| `"bottom"` | `bottom-center` | Bottom edge, horizontally centered |
| `"center"` | `center` | Center both horizontally and vertically |

### Full Position Values

These values specify exact positions within the 9-position grid:

| Value | Position | Description |
|-------|----------|-------------|
| `"top-left"` | ↖️ | Top-left corner |
| `"top-center"` | ⬆️ | Top edge, horizontally centered |
| `"top-right"` | ↗️ | Top-right corner |
| `"center-left"` | ⬅️ | Left edge, vertically centered |
| `"center"` | ⏺️ | Center both horizontally and vertically |
| `"center-right"` | ➡️ | Right edge, vertically centered |
| `"bottom-left"` | ↙️ | Bottom-left corner |
| `"bottom-center"` | ⬇️ | Bottom edge, horizontally centered |
| `"bottom-right"` | ↘️ | Bottom-right corner |

### Baseline Alignment Values

For text-based layouts, these values align to the text baseline:

| Value | Description |
|-------|-------------|
| `"baseline-left"` | Aligned to text baseline, left |
| `"baseline-center"` | Aligned to text baseline, center |
| `"baseline-right"` | Aligned to text baseline, right |

## Visual Reference

```
┌───────────────────────────────────┐
│  top-left   top-center  top-right │
│                                   │
│ center-left   center   center-right│
│                                   │
│bottom-left bottom-center bottom-right│
└───────────────────────────────────┘
```

## Default Alignments

When no `alignment` property is specified, containers use these defaults:

| Container | Default | Description |
|-----------|---------|-------------|
| **HBox** | `center-left` | Children at left edge, centered vertically |
| **VBox** | `top-center` | Children at top edge, centered horizontally |
| **StackPane** | `center` | Children centered both ways |
| **FlowPane** | `top-left` | Children start at top-left, wrap as needed |
| **TilePane** | `top-left` | Tiles start at top-left in uniform grid |

## Usage Examples

### Example 1: Center-Aligned Form

```javascript
{
    "name": "loginForm",
    "type": "vbox",
    "alignment": "center",
    "spacing": "15",
    "padding": "30",
    "items": [
        {"varRef": "username", "sequence": 1},
        {"varRef": "password", "sequence": 2},
        {"varRef": "loginButton", "sequence": 3}
    ]
}
```

### Example 2: Right-Aligned Button Bar

```javascript
{
    "name": "buttonBar",
    "type": "hbox",
    "alignment": "right",
    "spacing": "10",
    "padding": "10 20",
    "items": [
        {"varRef": "cancelButton", "sequence": 1},
        {"varRef": "okButton", "sequence": 2}
    ]
}
```

### Example 3: Top-Left StackPane Overlay

```javascript
{
    "name": "overlay",
    "type": "stackpane",
    "alignment": "top-left",
    "padding": "20",
    "items": [
        {"varRef": "backgroundImage", "sequence": 1},
        {"varRef": "titleLabel", "sequence": 2}
    ]
}
```

### Example 4: Bottom-Right Positioned Content

```javascript
{
    "name": "footer",
    "type": "hbox",
    "alignment": "bottom-right",
    "spacing": "5",
    "padding": "10",
    "items": [
        {"varRef": "versionLabel", "sequence": 1}
    ]
}
```

## Container-Specific Behavior

### HBox Alignment
- Affects **vertical positioning** of children
- Horizontal positioning follows layout order (left to right)
- Best for mixed-height children

### VBox Alignment  
- Affects **horizontal positioning** of children
- Vertical positioning follows layout order (top to bottom)
- Best for mixed-width children

### StackPane Alignment
- Applies to all stacked children as default
- Individual children can override via item-level settings
- Ideal for overlays and layered content

### FlowPane/TilePane Alignment
- Determines starting position for flow/grid
- Children wrap or tile from alignment point
- Useful for responsive, dynamic layouts

## Programmatic Access

The alignment property can be accessed and modified at runtime using screen builtins:

```javascript
// Get current alignment
var currentAlign = scr.getAreaProperty("myScreen.myArea", "alignment");

// Set new alignment
call scr.setAreaProperty("myScreen.myArea", "alignment", "center-right");
```

## Best Practices

1. **Be Explicit**: While defaults exist, explicitly setting alignment clarifies intent
2. **Visual Consistency**: Use consistent alignment patterns across related areas
3. **Responsive Design**: Consider how alignment affects different window sizes
4. **Test Thoroughly**: Preview alignment changes with different content sizes

## Technical Notes

- The `alignment` property is implemented using JavaFX's `Pos` enum
- Alignment is applied after default styles but before custom styles
- Values are case-insensitive and support both hyphenated and underscore formats
- Invalid alignment values are silently ignored (container uses default)

## Related Properties

The `alignment` property works alongside other layout properties:

- **spacing**: Gap between children
- **padding**: Internal margin around children  
- **style**: CSS style overrides (can include `-fx-alignment`)
- **hgrow/vgrow**: Growth priority for flexible layouts

## See Also

- [AREA_DEFINITION.md](docs/AREA_DEFINITION.md) - Complete area definition documentation
- [AreaDefinition.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaDefinition.java) - Java implementation
- [AreaContainerFactory.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaContainerFactory.java) - Container creation and alignment application
