# Screen Area Nodes and Properties

## Overview

Screen areas (containers) now support property access for introspection and help. This feature allows you to list all containers in a screen, get detailed help about container types, and access JavaFX properties at runtime.

## New Properties

### `.nodes` Property

Access all area/container names in a screen:

```javascript
show screen myScreen;
var areaList = myScreen.area.nodes;

// Iterate through all areas
for (i = 0; i < call array.length(areaList); i = i + 1) {
    print "Area: " + areaList[i];
}
```

**Returns**: `ArrayDynamic` containing all area names (strings) defined in the screen.

### `.help` Property

Get detailed help about a specific container type:

```javascript
show screen myScreen;
print myScreen.myArea.help;
```

**Returns**: Formatted help text including:
- Container type description
- Supported properties with explanations
- Usage example with JSON syntax
- Best practices for that container type

### `.javafx` Property

Access complete JavaFX container state at runtime (includes all `.properties`):

```javascript
show screen myScreen;
print myScreen.myArea.javafx;
```

**Returns**: Comprehensive JavaFX description including:
- Container type name (e.g., HBox, VBox, GridPane)
- Dimensions (width, height, min/max/pref sizes)
- Position (X, Y coordinates)
- Spacing and alignment settings
- Style information (CSS styles, style classes)
- Visibility and state (visible, managed, disabled)
- Container-specific properties (e.g., gaps for GridPane, orientation for FlowPane)

### `.properties` Property

Get all runtime property values:

```javascript
show screen myScreen;
print myScreen.myArea.properties;
```

**Returns**: Comprehensive property listing with current values:
- Type and class information
- All size properties (width, height, min/max/pref)
- Position (x, y)
- Padding
- Container-specific properties (spacing, alignment, gaps, orientation)
- Style information
- State (visible, managed, disabled)

### `.children` Property

Get list of immediate child area names:

```javascript
show screen myScreen;
var children = myScreen.myArea.children;

// Iterate through children
for (i = 0; i < call array.length(children); i = i + 1) {
    print "Child: " + children[i];
}
```

**Returns**: `ArrayDynamic` containing names of immediate child areas.

### `.parent` Property

Get the parent area name:

```javascript
show screen myScreen;
var parentName = myScreen.childArea.parent;
print "Parent area: " + parentName;
```

**Returns**: Parent area name as a string, or empty string if this is a root container.

### `.tree` Property

Get hierarchical tree structure:

```javascript
show screen myScreen;
print myScreen.myArea.tree;
```

**Returns**: Formatted tree structure showing the container and all its children:
```
└── myArea (hbox)
    ├── childArea1
    └── childArea2
```

### `.events` Property

List all registered event handlers:

```javascript
show screen myScreen;
print myScreen.myArea.events;
```

**Returns**: List of registered event handlers including:
- Mouse events (onMouseClicked, onMousePressed, onMouseReleased, onMouseEntered, onMouseExited)
- Key events (onKeyPressed, onKeyReleased, onKeyTyped)
- Focus tracking

### `.snapshot` Property

Capture container as image and copy to clipboard:

```javascript
show screen myScreen;
print myScreen.myArea.snapshot;
```

**Returns**: Confirmation message with snapshot details. The PNG image is automatically copied to the system clipboard for easy pasting into other applications.

## Supported Container Types

The following 16 container types are supported with detailed help:

### Layout Panes (9)
1. **PANE** - Basic layout with no automatic positioning
2. **STACKPANE** - Stacked layout with overlaying children
3. **ANCHORPANE** - Flexible layout with anchor-based positioning
4. **BORDERPANE** - Five-region layout (top, bottom, left, right, center)
5. **FLOWPANE** - Wrapping flow layout
6. **GRIDPANE** - Grid-based rows and columns layout
7. **HBOX** - Horizontal box layout (left to right)
8. **VBOX** - Vertical box layout (top to bottom)
9. **TILEPANE** - Uniform-sized tile layout

### Containers (6)
10. **SCROLLPANE** - Scrollable container for large content
11. **SPLITPANE** - Resizable dividers between children
12. **TABPANE** - Tabbed interface container
13. **TAB** - Individual tab within TabPane
14. **ACCORDION** - Collapsible titled panels
15. **TITLEDPANE** - Single collapsible panel with title

### Special (3)
16. **GROUP** - Logical grouping (VBox with spacing)
17. **REGION** - Base class for custom containers
18. **CANVAS** - Drawing surface for custom graphics

## Common Container Properties

All containers support these common properties:

| Property | Type | Description |
|----------|------|-------------|
| `type` | string | Container type name (e.g., "hbox", "vbox") |
| `spacing` | number | Gap between children (pixels) |
| `padding` | string | Internal spacing around children |
| `alignment` | string | Child alignment position |
| `style` | string | Custom CSS styling |
| `areaBackground` | string | Background color (hex format) |
| `minWidth`, `prefWidth`, `maxWidth` | string | Width constraints |
| `minHeight`, `prefHeight`, `maxHeight` | string | Height constraints |
| `hgrow`, `vgrow` | string | Growth priority (ALWAYS, SOMETIMES, NEVER) |

### Container-Specific Properties

**GridPane, FlowPane, TilePane:**
- `hgap` - Horizontal gap between columns/items
- `vgap` - Vertical gap between rows/items

**FlowPane, TilePane:**
- `orientation` - HORIZONTAL or VERTICAL layout direction

**TitledPane:**
- `title` - Title text for the pane

**GROUP:**
- `groupBorder` - Border style (none, line, raised, lowered, inset, outset)
- `groupBorderColor` - Border color (hex format)
- `groupBorderWidth` - Border width (pixels)
- `groupBorderRadius` - Border corner radius (pixels)
- `groupLabelText` - Label text on the border
- `groupLabelAlignment` - Label alignment (left, center, right)

## Usage Examples

### Example 1: Discover All Areas in a Screen

```javascript
screen myApp = {
    "title": "My Application",
    "width": 800,
    "height": 600,
    "area": [
        {
            "name": "mainArea",
            "type": "borderpane",
            "items": [],
            "areas": [
                {"name": "headerArea", "type": "hbox", "items": []},
                {"name": "contentArea", "type": "vbox", "items": []},
                {"name": "footerArea", "type": "hbox", "items": []}
            ]
        }
    ]
};

show screen myApp;

// List all areas
var areas = myApp.area.nodes;
print "Found " + call str.toString(call array.length(areas)) + " areas:";
for (i = 0; i < call array.length(areas); i = i + 1) {
    print "  " + (i + 1) + ". " + areas[i];
}
```

### Example 2: Get Help for a Container Type

```javascript
show screen myApp;

// Get help for the header area (HBox)
print myApp.headerArea.help;

// Output includes:
// - Description of HBox
// - Supported properties
// - Usage example
```

### Example 3: Inspect Container at Runtime

```javascript
show screen myApp;

// Get current state of header area
print myApp.headerArea.javafx;

// Output includes:
// - Container type (HBox)
// - Dimensions and position
// - Spacing and alignment
// - Style information
// - Child count
```

### Example 4: Dynamic Layout Inspection

```javascript
// Programmatically check container properties
show screen myApp;

var allAreas = myApp.area.nodes;

// Inspect each area
for (i = 0; i < call array.length(allAreas); i = i + 1) {
    var areaName = allAreas[i];
    print "==== Area: " + areaName + " ====";
    
    // Get JavaFX details for this area
    // Note: Cannot dynamically construct property access like myApp[areaName].javafx
    // Would need to hardcode each area name
}
```

## Best Practices

1. **Use `.nodes` for Discovery** - When working with dynamically configured screens, use `.nodes` to discover all available areas
2. **Use `.help` During Development** - Access help text to understand container capabilities and properties
3. **Use `.javafx` for Debugging** - Inspect runtime state when troubleshooting layout issues
4. **Document Custom Layouts** - Reference the help output in your documentation for consistency

## Technical Notes

- Property access is case-insensitive (`.nodes`, `.NODES`, `.Nodes` all work)
- The `.nodes` property returns an empty array if no areas are defined
- The `.help` property returns help text specific to the container's type
- The `.javafx` property requires the screen to be shown (not just defined)
- All three properties work on any named area in the screen definition

## Debug View Integration

The debug panel (opened with Ctrl+D) now includes container information in tooltips:

### Enhanced Tooltips

When hovering over items in the debug panel:
- **Area path** is displayed showing which container the item belongs to
- **Container Info** section shows complete JavaFX properties of the parent container
  - Container type and JavaFX class
  - Current dimensions and size constraints
  - Position, padding, spacing
  - Alignment settings
  - Style information
  - Container state (visible, managed, disabled)
  - Container-specific properties (gaps, orientation, etc.)

### Benefits
- Immediate access to container properties without manual querying
- Quick debugging of layout issues
- Understanding container hierarchy at a glance
- Same information is copied to clipboard when clicking items

### Usage
1. Open debug panel with Ctrl+D
2. Hover over any item in the debug table
3. View area path and container properties in the tooltip
4. Click item to copy all information to clipboard

## Integration with Existing Features

This feature complements existing screen functionality:

- **scr.getAreaProperty()** - Get specific property values
- **scr.setAreaProperty()** - Modify properties at runtime
- **Debug Panel (Ctrl+D)** - Visual inspection with enhanced tooltips
- **Screen Definitions** - Define areas with rich metadata

## See Also

- [CONTAINER_ALIGNMENT_OPTIONS.md](CONTAINER_ALIGNMENT_OPTIONS.md) - Container alignment reference
- [AreaDefinition.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaDefinition.java) - Java implementation
- [ScreenContainerType.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenContainerType.java) - Container type system
- [Interpreter.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/Interpreter.java) - Property access implementation
