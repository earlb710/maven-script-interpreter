# Alignment Properties Implementation

## Overview
This implementation adds two new alignment properties (`contentAlignment` and `itemAlignment`) to the EBS screen system, providing precise control over both content alignment within controls and item positioning within parent containers.

## Problem Statement
Previously, there was a single `alignment` property that was ambiguous - it controlled item content alignment but the architecture was unclear. The requirement was to:
1. Add `contentAlignment` (in DisplayItem) to explicitly control content/text alignment within controls
2. Add `itemAlignment` (in AreaItem) to control the alignment of items (and their HBox/VBox wrappers) within parent containers
3. Remove the old `alignment` property completely to enforce clean architectural separation

## Implementation

### Properties

#### 1. `contentAlignment` (DisplayItem property)
- **Location**: `DisplayItem` class (display/styling property)
- **Purpose**: Controls text/content alignment within the control itself
- **Usage**: Applied to TextField, TextArea, ComboBox, Spinner, etc.
- **Values**: `"left"`, `"center"`, `"right"` (also accepts shortcuts: `"l"`, `"c"`, `"r"`)
- **Implementation**: Uses `TextField.setAlignment(Pos)` API for TextFields, CSS for other controls

**Example:**
```json
{
  "varRef": "userName",
  "display": {
    "type": "textfield",
    "contentAlignment": "center"
  }
}
```

#### 2. `itemAlignment` (AreaItem property)
- **Location**: `AreaItem` class (layout/positioning property)
- **Purpose**: Controls positioning of the item (and its HBox/VBox wrapper) within the parent container
- **Usage**: Affects how the entire item (including label wrapper) is positioned in its parent
- **Values**: Any JavaFX Pos value (e.g., `"center"`, `"top-left"`, `"center-right"`, `"bottom-center"`)
- **Container-specific behavior**:
  - **StackPane**: Uses `StackPane.setAlignment()`
  - **GridPane**: Uses `GridPane.setHalignment()` and `GridPane.setValignment()`
  - **VBox**: Wraps item in HBox for horizontal alignment
  - **HBox**: Wraps item in VBox for vertical alignment

**Example:**
```json
{
  "varRef": "submitButton",
  "display": {"type": "button", "labelText": "Submit"},
  "itemAlignment": "center-right"
}
```

### Architecture

This implementation enforces clean separation between display and layout concerns:

- **DisplayItem**: Holds display/styling metadata (`contentAlignment`, `textColor`, `fontSize`, etc.)
- **AreaItem**: Holds layout/positioning properties (`itemAlignment`, `margin`, `padding`, `colSpan`, etc.)

The old ambiguous `alignment` property has been **completely removed** to prevent confusion.

### Files Modified

1. **DisplayItem.java**
   - Added `contentAlignment` field (display/styling property)
   - Removed old `alignment` field to prevent confusion
   - Updated `toString()` method

2. **AreaItem.java**
   - Added `itemAlignment` field (layout/positioning property)
   - Removed old `alignment` field completely
   - Updated `toString()` method

3. **BuiltinsScreen.java**
   - Added `contentAlignment` to `setAreaItemProperty()` and `getAreaItemProperty()` methods
   - Added `itemAlignment` to `setAreaItemProperty()` and `getAreaItemProperty()` methods
   - Removed support for deprecated `alignment` property

4. **InterpreterScreen.java**
   - Added parsing for `contentAlignment` from display definitions
   - Added parsing for `itemAlignment` from item definitions
   - Removed parsing for deprecated `alignment` property
   - Support for multiple case variations (camelCase, snake_case, lowercase)

5. **AreaItemFactory.java**
   - Updated content alignment logic to use `contentAlignment` from DisplayItem
   - Uses `TextField.setAlignment(Pos)` API for TextField controls
   - Uses CSS for TextArea and ComboBox controls
   - Removed fallback to deprecated `alignment` property

6. **ScreenFactory.java**
   - Added `itemAlignment` support for StackPane containers
   - Added `itemAlignment` support for GridPane containers  
   - Added `itemAlignment` support for VBox containers (wraps in HBox)
   - Added `itemAlignment` support for HBox containers (wraps in VBox)
   - Updated property parsing to recognize `contentAlignment` and `itemAlignment`
   - Removed support for deprecated `alignment` property

### Container-Specific Implementation

#### StackPane
```java
if (alignmentValue != null && !alignmentValue.isEmpty()) {
    Pos pos = parseAlignment(alignmentValue);
    StackPane.setAlignment(control, pos);
}
```

#### GridPane
```java
if (alignmentValue != null && !alignmentValue.isEmpty()) {
    Pos pos = parseAlignment(alignmentValue);
    GridPane.setHalignment(control, pos.getHpos());
    GridPane.setValignment(control, pos.getVpos());
}
```

#### VBox (wraps in HBox for horizontal alignment)
```java
if (alignmentValue != null && !alignmentValue.isEmpty()) {
    Pos pos = parseAlignment(alignmentValue);
    // Extract horizontal alignment
    Pos hAlignment = /* ... based on pos ... */;
    
    HBox alignmentWrapper = new HBox(control);
    alignmentWrapper.setAlignment(hAlignment);
    vbox.getChildren().add(alignmentWrapper);
}
```

#### HBox (wraps in VBox for vertical alignment)
```java
if (alignmentValue != null && !alignmentValue.isEmpty()) {
    Pos pos = parseAlignment(alignmentValue);
    // Extract vertical alignment
    Pos vAlignment = /* ... based on pos ... */;
    
    VBox alignmentWrapper = new VBox(control);
    alignmentWrapper.setAlignment(vAlignment);
    hbox.getChildren().add(alignmentWrapper);
}
```

## Usage Examples

### Example 1: Content Alignment
```json
{
  "sets": [{
    "setname": "main",
    "vars": [
      {"name": "leftText", "datatype": "string", "value": "Left"},
      {"name": "centerText", "datatype": "string", "value": "Center"},
      {"name": "rightText", "datatype": "string", "value": "Right"}
    ]
  }],
  "areas": [{
    "name": "mainArea",
    "type": "vbox",
    "items": [
      {
        "varRef": "leftText",
        "display": {"type": "textfield"},
        "contentAlignment": "left"
      },
      {
        "varRef": "centerText",
        "display": {"type": "textfield"},
        "contentAlignment": "center"
      },
      {
        "varRef": "rightText",
        "display": {"type": "textfield"},
        "contentAlignment": "right"
      }
    ]
  }]
}
```

### Example 2: Item Alignment in GridPane
```json
{
  "areas": [{
    "name": "gridArea",
    "type": "gridpane",
    "items": [
      {
        "varRef": "topLeft",
        "layoutPos": "0,0",
        "display": {"type": "button"},
        "itemAlignment": "top-left"
      },
      {
        "varRef": "center",
        "layoutPos": "1,1",
        "display": {"type": "button"},
        "itemAlignment": "center"
      },
      {
        "varRef": "bottomRight",
        "layoutPos": "2,2",
        "display": {"type": "button"},
        "itemAlignment": "bottom-right"
      }
    ]
  }]
}
```

### Example 3: Runtime Property Changes
```javascript
// Set content alignment via script
call scr.setProperty("myScreen.myField", "contentAlignment", "center");

// Get content alignment
var align: string = scr.getProperty("myScreen.myField", "contentAlignment");

// Set item alignment
call scr.setProperty("myScreen.myButton", "itemAlignment", "center-right");

// Get item alignment
var itemAlign: string = scr.getProperty("myScreen.myButton", "itemAlignment");
```

## Breaking Changes

⚠️ **BREAKING**: This implementation is **NOT backwards compatible**:

1. **Old `alignment` property removed**: Scripts using `alignment` must be updated
2. **No fallback**: The system will not fall back to the old `alignment` property
3. **Migration required**: Existing scripts must be updated to use:
   - `contentAlignment` (in display definition) for text/content alignment
   - `itemAlignment` (at item level) for item positioning in parent containers

### Migration Guide

**Before (old `alignment` property):**
```json
{
  "varRef": "myField",
  "display": {"type": "textfield"},
  "alignment": "center"
}
```

**After (new properties):**
```json
{
  "varRef": "myField",
  "display": {
    "type": "textfield",
    "contentAlignment": "center"
  },
  "itemAlignment": "center"
}
```

## Testing

Test and example scripts have been created to validate the implementation:

1. **ScriptInterpreter/scripts/test/test_alignment.ebs**: Basic test for content alignment with runtime property changes
2. **ScriptInterpreter/scripts/examples/alignment_properties.ebs**: Comprehensive example covering:
   - Content alignment in various controls
   - Item alignment in GridPane
   - Item alignment in VBox
   - Runtime property changes via scr.setProperty/getProperty

## Performance Considerations

The implementation includes an optimization check to prevent double-wrapping:
- The `alignmentApplied` property is set on wrapper containers
- Before wrapping, the system checks if alignment has already been applied
- This prevents redundant wrapper creation

## Future Enhancements

Potential future improvements:
1. Support for more container types (e.g., TilePane, FlowPane)
2. Animation support for alignment changes
3. More granular alignment controls (e.g., separate horizontal and vertical alignment properties)

## Security

CodeQL security analysis passed with **0 alerts**.

## Conclusion

This implementation successfully separates content alignment from container alignment while maintaining backwards compatibility. The new properties provide developers with precise control over both the content within controls and the positioning of items within their parent containers.
