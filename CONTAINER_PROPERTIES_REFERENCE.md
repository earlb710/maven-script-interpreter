# Container Properties Quick Reference

This document provides a quick reference for all properties available on each container type in the EBS screen system.

## How to Access Properties

```javascript
// List all areas in a screen
var areas = myScreen.area.nodes;

// Get help for a container
print myScreen.myArea.help;

// Get complete JavaFX runtime details (includes all properties)
print myScreen.myArea.javafx;

// Get all runtime properties
print myScreen.myArea.properties;

// Get hierarchy information
var children = myScreen.myArea.children;  // List of child area names
var parent = myScreen.childArea.parent;   // Parent area name
print myScreen.myArea.tree;               // Tree structure

// Get event handlers
print myScreen.myArea.events;

// Capture snapshot (copies to clipboard)
print myScreen.myArea.snapshot;
```

## Common Properties (All Containers)

These properties are available on **all** container types:

| Property | Type | Description | Example Values |
|----------|------|-------------|----------------|
| `type` | string | Container type name | "hbox", "vbox", "gridpane" |
| `spacing` | number | Gap between children (pixels) | "10", "15", "20" |
| `padding` | string | Internal spacing around children | "10", "10 20", "10 20 10 20" |
| `alignment` | string | Child alignment position | "center", "left", "top-center" |
| `style` | string | Custom CSS styling | "-fx-background-color: #f0f0f0;" |
| `areaBackground` | string | Background color | "#ffffff", "#f0f0f0" |
| `minWidth` | string | Minimum width constraint | "200", "200px" |
| `prefWidth` | string | Preferred width | "400", "400px" |
| `maxWidth` | string | Maximum width | "800", "800px" |
| `minHeight` | string | Minimum height constraint | "100", "100px" |
| `prefHeight` | string | Preferred height | "300", "300px" |
| `maxHeight` | string | Maximum height | "600", "600px" |
| `hgrow` | string | Horizontal growth priority | "ALWAYS", "SOMETIMES", "NEVER" |
| `vgrow` | string | Vertical growth priority | "ALWAYS", "SOMETIMES", "NEVER" |

## Container-Specific Properties

### HBox (Horizontal Box)
**Base Properties**: All common properties  
**Specific Properties**: None  
**Default Alignment**: center-left  
**Use Case**: Horizontal layouts, button bars, toolbars

### VBox (Vertical Box)
**Base Properties**: All common properties  
**Specific Properties**: None  
**Default Alignment**: top-center  
**Use Case**: Vertical layouts, forms, menus

### GridPane
**Base Properties**: All common properties  
**Specific Properties**:
- `hgap` (number) - Horizontal gap between columns
- `vgap` (number) - Vertical gap between rows

**Use Case**: Forms, tables, structured layouts

### StackPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Default Alignment**: center  
**Use Case**: Layered content, overlays, centered content

### BorderPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Regions**: top, bottom, left, right, center  
**Use Case**: Application layouts with headers, footers, sidebars

### FlowPane
**Base Properties**: All common properties  
**Specific Properties**:
- `hgap` (number) - Horizontal gap between items
- `vgap` (number) - Vertical gap between items
- `orientation` (string) - HORIZONTAL or VERTICAL

**Use Case**: Tags, chips, wrapping content

### TilePane
**Base Properties**: All common properties  
**Specific Properties**:
- `hgap` (number) - Horizontal gap between tiles
- `vgap` (number) - Vertical gap between tiles
- `orientation` (string) - HORIZONTAL or VERTICAL

**Use Case**: Image galleries, icon grids, uniform content

### AnchorPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Children positioned using anchor constraints  
**Use Case**: Custom layouts with precise positioning

### Pane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: No automatic positioning  
**Use Case**: Custom layouts with manual positioning

### ScrollPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Automatically adds scrollbars when needed  
**Use Case**: Large content, scrollable areas

### SplitPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Resizable dividers between children  
**Use Case**: Resizable panels, editors, multi-view layouts

### TabPane
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Contains Tab children  
**Use Case**: Multi-page interfaces, settings panels

### Tab
**Base Properties**: Limited (not a Region)  
**Specific Properties**:
- `title` (string) - Tab title text

**Note**: Individual tab within TabPane  
**Use Case**: Individual pages in tabbed interface

### Accordion
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Contains TitledPane children, only one open at a time  
**Use Case**: Settings, navigation menus, grouped content

### TitledPane
**Base Properties**: All common properties  
**Specific Properties**:
- `title` (string) - Title text

**Note**: Collapsible with title bar  
**Use Case**: Collapsible sections, expandable content

### GROUP
**Base Properties**: All common properties  
**Specific Properties** (Border styling):
- `groupBorder` (string) - Border style: "none", "line", "raised", "lowered", "inset", "outset"
- `groupBorderColor` (string) - Border color in hex (e.g., "#4a9eff")
- `groupBorderWidth` (string) - Border width in pixels (e.g., "2", "2px")
- `groupBorderRadius` (string) - Border corner radius (e.g., "5", "5px")
- `groupBorderInsets` (string) - Border insets (e.g., "5", "5 10")
- `groupLabelText` (string) - Label text on the border
- `groupLabelAlignment` (string) - Label alignment: "left", "center", "right"
- `groupLabelOffset` (string) - Label vertical offset: "top", "on", "bottom"
- `groupLabelColor` (string) - Label text color in hex
- `groupLabelBackground` (string) - Label background color in hex

**Note**: Logical grouping, rendered as VBox  
**Use Case**: Grouping related controls, bordered sections

### Region
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Base class for custom containers  
**Use Case**: Extending with custom layout logic

### Canvas
**Base Properties**: All common properties  
**Specific Properties**: None  
**Note**: Drawing surface for custom graphics  
**Use Case**: Charts, diagrams, custom visualizations

## Alignment Values Reference

All containers that support `alignment` can use these values:

### Shorthand
- `"left"` → center-left
- `"right"` → center-right
- `"top"` → top-center
- `"bottom"` → bottom-center
- `"center"` → center

### Full Position
- `"top-left"`, `"top-center"`, `"top-right"`
- `"center-left"`, `"center"`, `"center-right"`
- `"bottom-left"`, `"bottom-center"`, `"bottom-right"`

### Baseline (for text)
- `"baseline-left"`, `"baseline-center"`, `"baseline-right"`

## Growth Priority Values

For `hgrow` and `vgrow` properties:
- `"ALWAYS"` - Always grows to fill available space
- `"SOMETIMES"` - Grows if there's extra space
- `"NEVER"` - Never grows (default)

## Padding Format

The `padding` property accepts various formats:
- Single value: `"10"` → 10px all sides
- Two values: `"10 20"` → 10px top/bottom, 20px left/right
- Four values: `"10 20 10 20"` → top, right, bottom, left

## Getting Runtime Information

### Complete JavaFX State (`.javafx`)

Use the `.javafx` property to get complete runtime state (includes all `.properties`):

```javascript
show screen myScreen;
print myScreen.myArea.javafx;
```

This returns:
- Container type and class name
- Current dimensions (width, height)
- Position (X, Y coordinates)
- Size constraints (min/max/pref)
- Padding and spacing values
- Alignment settings
- Style information
- State (visible, managed, disabled)
- Child count (for Pane containers)
- Container-specific properties

### All Properties (`.properties`)

Get all runtime property values:

```javascript
print myScreen.myArea.properties;
```

Returns the same comprehensive information as `.javafx`.

### Hierarchy Information

**Children** - Get immediate child areas:
```javascript
var children = myScreen.myArea.children;
```

**Parent** - Get parent area name:
```javascript
var parent = myScreen.childArea.parent;
```

**Tree** - Get full hierarchy tree:
```javascript
print myScreen.myArea.tree;
```

### Event Handlers (`.events`)

List registered event handlers:
```javascript
print myScreen.myArea.events;
```

Shows:
- Mouse events (click, press, release, enter, exit)
- Key events (press, release, typed)
- Focus tracking

### Visual Snapshot (`.snapshot`)

Capture container as image:
```javascript
print myScreen.myArea.snapshot;
```

- Captures PNG image
- Automatically copies to system clipboard
- Returns confirmation message

## Debug View Integration

The debug panel (Ctrl+D) now shows container JavaFX information in tooltips:
- Hover over items to see parent container properties
- Complete JavaFX state displayed
- Click items to copy information including container details

## See Also

- [SCREEN_AREA_NODES.md](SCREEN_AREA_NODES.md) - Complete guide to area nodes and properties
- [CONTAINER_ALIGNMENT_OPTIONS.md](CONTAINER_ALIGNMENT_OPTIONS.md) - Detailed alignment reference
- [AreaDefinition.java](ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaDefinition.java) - Java implementation
