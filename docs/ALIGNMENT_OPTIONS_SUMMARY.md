# Alignment Options Available on Containers (Areas)

## Quick Answer

Containers (areas) support an **`alignment`** property with **17 possible values** grouped into three categories:

### 1. Shorthand Values (5)
Simple, commonly-used positions:
- **`left`** - Left edge, vertically centered
- **`right`** - Right edge, vertically centered
- **`top`** - Top edge, horizontally centered
- **`bottom`** - Bottom edge, horizontally centered
- **`center`** - Center both horizontally and vertically

### 2. Full Position Values (9)
Precise positions in a 3×3 grid:
- **`top-left`** - Top-left corner
- **`top-center`** - Top edge, centered horizontally
- **`top-right`** - Top-right corner
- **`center-left`** - Left edge, centered vertically
- **`center`** - Center both ways
- **`center-right`** - Right edge, centered vertically
- **`bottom-left`** - Bottom-left corner
- **`bottom-center`** - Bottom edge, centered horizontally
- **`bottom-right`** - Bottom-right corner

### 3. Baseline Values (3)
For text-based layouts:
- **`baseline-left`** - Text baseline, left aligned
- **`baseline-center`** - Text baseline, center aligned
- **`baseline-right`** - Text baseline, right aligned

## Supported Containers

The `alignment` property works with these container types:
- **HBox** - Horizontal box (affects vertical positioning)
- **VBox** - Vertical box (affects horizontal positioning)
- **StackPane** - Stacked layout (affects all layers)
- **FlowPane** - Wrapping flow (sets starting position)
- **TilePane** - Tile grid (sets starting position)

## Usage Example

```javascript
{
    "name": "buttonBar",
    "type": "hbox",
    "alignment": "right",    // Align buttons to the right
    "spacing": "10",
    "padding": "10",
    "items": [
        {"varRef": "cancelButton", "sequence": 1},
        {"varRef": "okButton", "sequence": 2}
    ]
}
```

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

## Default Alignments (when not specified)

- **HBox**: `center-left`
- **VBox**: `top-center`
- **StackPane**: `center`
- **FlowPane**: `top-left`
- **TilePane**: `top-left`

## Complete Documentation

For detailed documentation, examples, and technical information:
- **Quick Reference**: [CONTAINER_ALIGNMENT_OPTIONS.md](CONTAINER_ALIGNMENT_OPTIONS.md)
- **Complete Guide**: [docs/AREA_DEFINITION.md](docs/AREA_DEFINITION.md) (see "Alignment Options" section)

## Implementation Files

- Property definition: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaDefinition.java`
- Alignment application: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaContainerFactory.java`
- Test script: `test_alignment_simple.ebs`
