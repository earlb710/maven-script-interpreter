# Screen Item Color Properties

## Overview
This document describes all color properties available for screen items in the EBS scripting language. Screen items can have four distinct color aspects: label text color, label background color, item text color, and item background color.

## Problem Statement
For a screen item, there should be four color properties available:
1. **Item background color** - Background color of the item/control
2. **Item text color** - Text color within the item/control
3. **Label background color** - Background color of the label
4. **Label text color** - Text color of the label

## Complete Color Property Matrix

| Property | Description | Supports camelCase | Supports snake_case | Level | Since |
|----------|-------------|-------------------|---------------------|-------|-------|
| **labelColor** | Label text color | ✅ `labelColor` | ✅ `label_color` | DisplayItem | Initial |
| **labelBackgroundColor** | Label background color | ✅ `labelBackgroundColor` | ✅ `label_background_color` | DisplayItem | **NEW** |
| **textColor** | Item text color (preferred) | ✅ `textColor` | ✅ `text_color` | DisplayItem | Recent |
| **itemColor** | Item text color (legacy) | ✅ `itemColor` | ✅ `item_color` | DisplayItem | Initial |
| **backgroundColor** | Item background color | ✅ `backgroundColor` | ✅ `background_color` | AreaItem | Initial |

### Notes
- **textColor** takes precedence over **itemColor** when both are specified
- All properties support both camelCase and snake_case naming conventions
- Properties can be specified at the display metadata level or item level
- Item-level properties override display metadata-level properties

## Where Properties Apply

### Display Metadata Level (DisplayItem)
These properties are specified in the `display` object within the variable definition:
- `labelColor` - Color of the label text
- `labelBackgroundColor` - Background color of the label (NEW)
- `textColor` / `itemColor` - Color of the text within the control
- Applied to all instances of the variable across different areas

### Item Level (AreaItem)
These properties are specified in the `items` array within an area definition:
- `textColor` - Overrides the display metadata text color for this specific item
- `backgroundColor` - Background color of the control for this specific item
- Applied only to the specific item instance in that area

## Usage Examples

### Example 1: Basic Color Properties
```ebs
screen colorDemo = {
    "title": "Color Demo",
    "width": 500,
    "height": 400,
    "sets": [
        {
            "setname": "main",
            "vars": [
                {
                    "name": "username",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "labelText": "Username:",
                        "labelColor": "#FF0000",               // Red label text
                        "labelBackgroundColor": "#FFFF00",     // Yellow label background
                        "textColor": "#0000FF",                // Blue item text
                        "labelPosition": "left"
                    }
                }
            ]
        }
    ],
    "areas": [
        {
            "areaname": "form",
            "containerType": "vbox",
            "items": [
                {
                    "name": "username_item",
                    "varRef": "username",
                    "backgroundColor": "#F0F0F0"            // Light gray item background
                }
            ]
        }
    ]
};
```

### Example 2: Snake_case Naming
```ebs
{
    "name": "email",
    "type": "string",
    "display": {
        "type": "textfield",
        "labelText": "Email:",
        "label_color": "#008000",                  // Green label text
        "label_background_color": "#E0FFE0",       // Light green label background
        "text_color": "#000080",                   // Navy item text
        "labelPosition": "left"
    }
}
```

### Example 3: Item-Level Overrides
```ebs
{
    "areaname": "details",
    "containerType": "vbox",
    "items": [
        {
            "name": "status_field",
            "varRef": "status",
            // Override the display metadata colors at the item level
            "textColor": "#FF0000",               // Red text (override)
            "backgroundColor": "#FFFFE0"          // Light yellow background
        }
    ]
}
```

### Example 4: All Color Properties Together
```ebs
screen fullColorExample = {
    "title": "Full Color Example",
    "width": 600,
    "height": 500,
    "sets": [
        {
            "setname": "form",
            "vars": [
                {
                    "name": "description",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textarea",
                        "labelText": "Description:",
                        "labelColor": "#FFFFFF",               // White label text
                        "labelBackgroundColor": "#0000FF",     // Blue label background
                        "textColor": "#000000",                // Black item text
                        "labelPosition": "top",
                        "height": 5
                    }
                }
            ]
        }
    ],
    "areas": [
        {
            "areaname": "main",
            "containerType": "vbox",
            "items": [
                {
                    "name": "desc_item",
                    "varRef": "description",
                    "backgroundColor": "#FFFACD"           // Lemon chiffon item background
                }
            ]
        }
    ]
};
```

## Color Value Formats

Color values can be specified in the following formats:
- **Hex colors**: `"#FF0000"` (red), `"#00FF00"` (green), `"#0000FF"` (blue)
- **Named colors**: `"red"`, `"blue"`, `"green"`, `"white"`, `"black"`, etc.
- **RGB values**: `"rgb(255, 0, 0)"` for red
- **RGBA values**: `"rgba(255, 0, 0, 0.5)"` for semi-transparent red

## Implementation Details

### Files Modified
1. **DisplayItem.java** - Added `labelBackgroundColor` field
2. **InterpreterScreen.java** - Added parsing for the new property with variable substitution
3. **ScreenFactory.java** - Updated to parse, merge, clone, and apply the new property
4. **AreaItemFactory.java** - Updated to apply label background color to Label controls

### Property Precedence Rules
1. Item-level properties override display metadata-level properties
2. `textColor` takes precedence over `itemColor` when both are specified
3. Properties specified at the item level only affect that specific item instance

## Backward Compatibility

All changes are fully backward compatible:
- Existing screens using `itemColor` continue to work
- Existing screens without the new `labelBackgroundColor` property work as before
- New property is optional - if not specified, default styling applies

## Testing

Two test scripts have been provided:
1. **test_color_parse.ebs** - Verifies that all color properties parse correctly without errors
2. **test_screen_color_properties.ebs** - Comprehensive GUI test showing all color properties in action

## Benefits

1. **Complete Color Control**: All four color aspects are now available for full customization
2. **Consistent API**: All properties follow the same naming conventions and patterns
3. **Flexible Naming**: Both camelCase and snake_case variants supported
4. **Clear Semantics**: Property names clearly indicate what they affect (label vs. item)
5. **Backward Compatible**: No breaking changes to existing code

## Migration Guide

### Adding Label Background Colors to Existing Screens

If you have existing screens and want to add label background colors:

```ebs
// Before: Only label text color
{
    "display": {
        "type": "textfield",
        "labelText": "Name:",
        "labelColor": "#FF0000"
    }
}

// After: Both label text and background colors
{
    "display": {
        "type": "textfield",
        "labelText": "Name:",
        "labelColor": "#FF0000",
        "labelBackgroundColor": "#FFFFE0"    // Add this line
    }
}
```

## Related Documentation

- [TEXTCOLOR_PROPERTY_IMPLEMENTATION.md](TEXTCOLOR_PROPERTY_IMPLEMENTATION.md) - Details on textColor vs itemColor
- [SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md](SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md) - Screen component type system

## Conclusion

With the addition of `labelBackgroundColor`, the EBS screen system now provides complete control over all four color aspects of screen items:
1. ✅ Item background color (`backgroundColor`)
2. ✅ Item text color (`textColor` or `itemColor`)
3. ✅ Label background color (`labelBackgroundColor`)
4. ✅ Label text color (`labelColor`)

This provides developers with the flexibility to create fully customized, visually appealing user interfaces with precise color control.
