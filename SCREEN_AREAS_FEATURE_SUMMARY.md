# Screen Areas Feature Summary

## What's New

This feature adds introspection capabilities for screen areas (containers), allowing developers to:
1. **List all areas** in a screen dynamically
2. **Get detailed help** for any container type
3. **Inspect runtime state** of containers via JavaFX properties

## Before and After

### Before
```javascript
// No way to list areas programmatically
// No built-in help for container types
// Limited runtime inspection

// Only option was to remember area names or use scr.getAreaProperty()
call scr.getAreaProperty("myScreen.headerArea", "alignment");
```

### After
```javascript
// List all areas dynamically
var areas = myScreen.area.nodes;
print "Found " + call str.toString(call array.length(areas)) + " areas";

// Get help for any container
print myScreen.headerArea.help;
// Returns detailed documentation about HBox containers

// Inspect runtime state
print myScreen.contentArea.javafx;
// Returns JavaFX properties, dimensions, styles, etc.
```

## Use Cases

### 1. Dynamic Screen Inspection
```javascript
// Discover and inspect all areas at runtime
show screen myApp;
var areas = myApp.area.nodes;

for (i = 0; i < call array.length(areas); i = i + 1) {
    print "Area: " + areas[i];
}
```

**Benefits**:
- Debug complex screen layouts
- Validate screen structure programmatically
- Build diagnostic tools

### 2. Self-Documenting Code
```javascript
// Get help directly in the console during development
show screen myApp;
print myApp.headerArea.help;
```

**Output Example**:
```
═══════════════════════════════════════════════════════════
Container Type: Screen.container
═══════════════════════════════════════════════════════════

Description:
Horizontal box layout that arranges children in a single row 
from left to right. Ideal for buttons, toolbars, and 
horizontal layouts.

Supported Properties:
  • type (string) - Container type name
  • spacing (number) - Gap between children
  • padding (string) - Internal spacing around children
  • alignment (string) - Child alignment position
  [... more properties ...]

Example:
{
    "name": "buttonBar",
    "type": "hbox",
    "alignment": "right",
    "spacing": "10",
    "padding": "10 20",
    "items": [...]
}
═══════════════════════════════════════════════════════════
```

### 3. Runtime Debugging
```javascript
// Check actual container state at runtime
show screen myApp;
print myApp.contentArea.javafx;
```

**Output Example**:
```
JavaFX Container Description:
  Container Type: vbox
  JavaFX Class: VBox
  Width: 780.00
  Height: 540.00
  Min Width: -1.00
  Min Height: -1.00
  Pref Width: -1.00
  Pref Height: -1.00
  Max Width: 1.7976931348623157E308
  Max Height: 1.7976931348623157E308
  X: 10.00
  Y: 10.00
  Padding: 20.0 20.0 20.0 20.0
  Spacing: 15.00
  Alignment: TOP_CENTER
  Visible: true
  Managed: true
  Disabled: false
  Children Count: 5
```

## API Reference

### Property: `.nodes`
**Access Pattern**: `screenName.area.nodes`  
**Returns**: `ArrayDynamic<String>` - List of all area names  
**Example**:
```javascript
var areas = myScreen.area.nodes;  // ["mainArea", "headerArea", "contentArea"]
```

### Property: `.help`
**Access Pattern**: `screenName.areaName.help`  
**Returns**: `String` - Formatted help documentation  
**Example**:
```javascript
var helpText = myScreen.headerArea.help;
print helpText;
```

### Property: `.javafx`
**Access Pattern**: `screenName.areaName.javafx`  
**Returns**: `String` - JavaFX container description  
**Example**:
```javascript
var javaFXInfo = myScreen.contentArea.javafx;
print javaFXInfo;
```

## Supported Container Types

All 16 container types from `AreaDefinition.AreaType` are fully supported:

| Category | Types |
|----------|-------|
| **Layout Panes** (9) | Pane, StackPane, AnchorPane, BorderPane, FlowPane, GridPane, HBox, VBox, TilePane |
| **Containers** (6) | ScrollPane, SplitPane, TabPane, Tab, Accordion, TitledPane |
| **Special** (3) | Group, Region, Canvas |

Each type has:
- ✅ Detailed description
- ✅ Complete property list (common + specific)
- ✅ Usage example
- ✅ Best practices

## Integration with Existing Features

This feature complements existing screen functionality:

| Feature | Purpose | Integration Point |
|---------|---------|------------------|
| **scr.getAreaProperty()** | Get specific property values | Use `.nodes` to discover areas first |
| **scr.setAreaProperty()** | Modify properties at runtime | Use `.help` to see available properties |
| **Debug Panel (Ctrl+D)** | Visual inspection | Use `.javafx` for programmatic inspection |
| **Screen Definitions** | Define screen structure | Use `.help` for property documentation |

## Performance Notes

- **`.nodes`** - Fast O(1) lookup of pre-registered containers
- **`.help`** - Static text generation, minimal overhead
- **`.javafx`** - Reads current JavaFX state, no performance impact

## Examples by Container Type

### HBox Example
```javascript
screen myApp = {
    "area": [{
        "name": "toolbar",
        "type": "hbox",
        "alignment": "right",
        "spacing": "10",
        "items": [...]
    }]
};
show screen myApp;
print myApp.toolbar.help;  // Get HBox-specific help
```

### GridPane Example
```javascript
screen myApp = {
    "area": [{
        "name": "form",
        "type": "gridpane",
        "spacing": "10",
        "items": [...]
    }]
};
show screen myApp;
print myApp.form.help;     // Get GridPane-specific help
print myApp.form.javafx;   // See current state
```

### Group with Border Example
```javascript
screen myApp = {
    "area": [{
        "name": "settings",
        "type": "group",
        "groupBorder": "line",
        "groupBorderColor": "#4a9eff",
        "groupLabelText": "Settings",
        "items": [...]
    }]
};
show screen myApp;
print myApp.settings.help;  // See Group border properties
```

## Developer Workflow

### Development Phase
1. Define screen with areas
2. Use `.help` to explore available properties
3. Reference documentation for property details
4. Apply properties to areas

### Testing Phase
1. Show screen
2. Use `.nodes` to verify all areas created
3. Use `.javafx` to inspect runtime state
4. Verify layout and styling

### Debugging Phase
1. Use `.nodes` to list all areas
2. Use `.javafx` to check actual vs expected state
3. Use `.help` to verify property usage
4. Adjust definitions as needed

## Code Examples

### Complete Example: Inspection Tool
```javascript
// Simple screen inspection tool
inspectScreen(screenName: string) {
    show screen screenName;
    
    var areas = screenName.area.nodes;
    print "Screen: " + screenName;
    print "Areas: " + call str.toString(call array.length(areas));
    print "";
    
    for (i = 0; i < call array.length(areas); i = i + 1) {
        print "  " + (i + 1) + ". " + areas[i];
    }
}

// Usage
call inspectScreen("myApp");
```

### Complete Example: Help Browser
```javascript
// Browse help for all areas
browseHelp(screenName: string) {
    show screen screenName;
    
    var areas = screenName.area.nodes;
    
    for (i = 0; i < call array.length(areas); i = i + 1) {
        var areaName = areas[i];
        print "==== " + areaName + " ====";
        // Note: Can't dynamically access screenName[areaName].help
        // Would need specific property access per area
    }
}
```

## Limitations

1. **Dynamic Property Access**: Cannot use computed area names
   - ❌ `myScreen[areaName].help` (not supported)
   - ✅ `myScreen.headerArea.help` (direct access only)

2. **Screen Must Be Shown**: `.javafx` requires screen to be displayed
   - Screen definition alone is not enough
   - Use `show screen` before accessing `.javafx`

3. **Read-Only**: These properties are for inspection only
   - Use `scr.setAreaProperty()` to modify properties
   - Use `scr.getAreaProperty()` to get current values

## Migration Guide

No migration needed! This is a new feature that adds capabilities without breaking existing code.

### Existing Code
```javascript
// Still works exactly as before
call scr.getAreaProperty("myScreen.myArea", "alignment");
call scr.setAreaProperty("myScreen.myArea", "alignment", "center");
```

### Enhanced Code
```javascript
// New capabilities added
var areas = myScreen.area.nodes;           // NEW
print myScreen.myArea.help;                 // NEW
print myScreen.myArea.javafx;               // NEW (also works on controls)
```

## See Also

- [SCREEN_AREA_NODES.md](SCREEN_AREA_NODES.md) - Complete guide
- [CONTAINER_PROPERTIES_REFERENCE.md](CONTAINER_PROPERTIES_REFERENCE.md) - Property reference
- [CONTAINER_ALIGNMENT_OPTIONS.md](CONTAINER_ALIGNMENT_OPTIONS.md) - Alignment guide
- [AreaDefinition.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaDefinition.java) - Implementation
