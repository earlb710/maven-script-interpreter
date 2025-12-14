# Slider Value Display Feature - Visual Documentation

## Feature Overview
This document demonstrates the new `showSliderValue` feature for slider controls in the EBS screen system.

## What was added?
A new JSON property `showSliderValue: true/false` that can be added to slider display metadata in the `vars` section of screen definitions.

When `showSliderValue: true`, a label displaying the current slider value is automatically added to the right of the slider control.

## Visual Representation

### Before (Default behavior - showSliderValue not specified or false):
```
Label:          [═══════●═════════════]
```

### After (With showSliderValue: true):
```
Label:          [═══════●═════════════]  50
```

## UI Layout Details

The value label is:
- Positioned to the right of the slider with 10px left padding
- Automatically updated as the slider moves
- Styled to match the slider's font properties (size, color, bold, italic)
- Formatted intelligently:
  - Integer values: `50` (no decimals)
  - Decimal values: `50.5` (one decimal place)
- Set to a minimum width of 50px for consistent appearance

## Example Screen Definitions

### Example 1: Basic slider with value display
```javascript
{
    "name": "volume",
    "type": "int",
    "default": 50,
    "display": {
        "type": "slider",
        "min": 0,
        "max": 100,
        "labelText": "Volume:",
        "showSliderValue": true
    }
}
```

**Renders as:**
```
Volume:         [════════════●════════]  50
```

### Example 2: Styled slider with value display
```javascript
{
    "name": "opacity",
    "type": "int",
    "default": 80,
    "display": {
        "type": "slider",
        "min": 0,
        "max": 100,
        "labelText": "Opacity:",
        "showSliderValue": true,
        "itemFontSize": "16px",
        "itemColor": "#0066cc",
        "itemBold": true
    }
}
```

**Renders as:**
```
Opacity:        [══════════════●══════]  **80**  (in blue, bold, 16px)
```

### Example 3: Slider without value display (backward compatible)
```javascript
{
    "name": "brightness",
    "type": "int",
    "default": 75,
    "display": {
        "type": "slider",
        "min": 0,
        "max": 100,
        "labelText": "Brightness:",
        "showSliderValue": false
    }
}
```

**Renders as:**
```
Brightness:     [═════════════●═══════]
```

### Example 4: Slider with negative values
```javascript
{
    "name": "temperature",
    "type": "int",
    "default": 20,
    "display": {
        "type": "slider",
        "min": -10,
        "max": 50,
        "labelText": "Temperature:",
        "showSliderValue": true
    }
}
```

**Renders as:**
```
Temperature:    [════════════●════════]  20
```
When moved to minimum:
```
Temperature:    [●═══════════════════]  -10
```

## Test Script
A comprehensive test script is available at:
`ScriptInterpreter/scripts/test_slider_value_display.ebs`

This script demonstrates:
1. Slider with value display enabled
2. Slider with value display explicitly disabled
3. Slider with negative value ranges
4. Slider without the property (defaults to false)
5. Slider with custom styling (blue, bold)
6. Slider with custom styling (green, italic)

## Technical Implementation

### Files Modified:
1. **DisplayItem.java** - Added `showSliderValue` field
2. **display-metadata.json** - Added schema definition and example
3. **AreaItemFactory.java** - Creates HBox wrapper with slider + label
4. **ScreenFactory.java** - Handles binding for wrapped sliders

### Component Structure:
When `showSliderValue: true`:
```
HBox (container)
├── Slider (original control)
└── Label (value display)
```

The HBox:
- Has 5px spacing between children
- Uses CENTER_LEFT alignment
- Preserves all properties from the slider for proper binding
- Is transparent to mouse events (pickOnBounds: false) for proper tooltip behavior

### Binding Behavior:
- Slider changes update both the bound variable AND the value label
- Variable changes update both the slider position AND the value label
- All existing slider binding functionality is preserved
- Two-way data binding works seamlessly

## Backward Compatibility
- When `showSliderValue` is not specified, it defaults to `false`
- Existing sliders without this property continue to work as before
- The feature is purely additive with no breaking changes

## Styling Support
The value label inherits styling from the slider's display metadata:
- `itemFontSize` - Controls font size of the value
- `itemColor` or `textColor` - Controls text color of the value (textColor takes precedence when both are specified)
- `itemBold` - Makes the value bold
- `itemItalic` - Makes the value italic

All styling is applied consistently for a cohesive appearance.
