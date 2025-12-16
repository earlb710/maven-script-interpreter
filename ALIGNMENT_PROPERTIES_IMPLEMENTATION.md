# Alignment Properties Implementation

## Overview
This implementation adds two new alignment properties (`contentAlignment` and `itemAlignment`) to the EBS screen system, providing precise control over both content alignment within controls and item positioning within parent containers.

## Problem Statement
Previously, there was a single `alignment` property that was used for item content alignment. The requirement was to:
1. Keep `alignment` as is for backwards compatibility
2. Add `contentAlignment` to explicitly control content alignment (same as current `alignment`)
3. Add `itemAlignment` to control the alignment of items (and their HBox/VBox wrappers) within parent containers

## Implementation

### New Properties

#### 1. `contentAlignment`
- **Purpose**: Controls text/content alignment within the control itself
- **Usage**: Applied to TextField, TextArea, ComboBox, Spinner, etc.
- **Values**: `"left"`, `"center"`, `"right"` (also accepts shortcuts: `"l"`, `"c"`, `"r"`)
- **CSS Effect**: Sets `-fx-alignment` on the control

**Example:**
```json
{
  "varRef": "userName",
  "display": {"type": "textfield"},
  "contentAlignment": "center"
}
```

#### 2. `itemAlignment`
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

#### 3. `alignment` (Deprecated but retained)
- **Purpose**: Backwards compatibility
- **Behavior**: 
  - Used as fallback for `contentAlignment` if not specified
  - Used as fallback for `itemAlignment` if not specified
- **Recommendation**: New code should use `contentAlignment` and/or `itemAlignment` explicitly

### Files Modified

1. **AreaItem.java**
   - Added `contentAlignment` field
   - Added `itemAlignment` field
   - Kept `alignment` field with deprecation note
   - Updated `toString()` method

2. **BuiltinsScreen.java**
   - Added `contentAlignment` to `setAreaItemProperty()` method
   - Added `itemAlignment` to `setAreaItemProperty()` method
   - Added `contentAlignment` to `getAreaItemProperty()` method
   - Added `itemAlignment` to `getAreaItemProperty()` method

3. **AreaItemFactory.java**
   - Updated content alignment logic to prioritize `contentAlignment`
   - Falls back to `alignment` for backwards compatibility
   - Applies alignment to TextField, TextArea, ComboBox, and Spinner controls

4. **ScreenFactory.java**
   - Added `itemAlignment` support for StackPane containers
   - Added `itemAlignment` support for GridPane containers
   - Added `itemAlignment` support for VBox containers (wraps in HBox)
   - Added `itemAlignment` support for HBox containers (wraps in VBox)
   - Updated property parsing to recognize `contentAlignment` and `itemAlignment`
   - Updated valid properties list to include snake_case variants

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

## Backwards Compatibility

The implementation maintains full backwards compatibility:

1. **Existing scripts continue to work**: Scripts using `alignment` will continue to function as before
2. **Graceful fallback**: If `contentAlignment` is not specified, the system falls back to `alignment`
3. **No breaking changes**: All existing functionality is preserved

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
