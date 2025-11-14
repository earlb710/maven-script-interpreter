# AreaDefinition Documentation

## Overview

The `AreaDefinition` class and its sub-components provide a comprehensive framework for defining JavaFX UI layouts within the EBS (Earl Bosch Script) screen system. This system allows developers to create structured, responsive user interfaces with precise control over layout, positioning, and styling.

## Table of Contents

- [AreaDefinition Class](#areadefinition-class)
- [AreaType Enum](#areatype-enum)
- [AreaItem Class](#areaitem-class)
- [Usage Examples](#usage-examples)
- [Best Practices](#best-practices)

---

## AreaDefinition Class

The `AreaDefinition` class represents a container area within a screen, defining its type, layout, styling, and the items it contains.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Unique identifier for the area |
| `areaType` | AreaType | The JavaFX container type (enum) |
| `type` | String | Area type as string (for compatibility) |
| `cssClass` | String | CSS class name from the AreaType enum |
| `layout` | String | Layout configuration string |
| `style` | String | CSS style string (defaults to areaType's default) |
| `screenName` | String | Associated screen name |
| `items` | List\<AreaItem\> | List of items contained in this area |

### EBS Script Syntax

```javascript
area: [{
    name: "mainArea",
    type: "vbox",
    layout: "fill",
    style: "-fx-padding: 20; -fx-spacing: 15;",
    items: [
        // AreaItem definitions
    ]
}]
```

---

## AreaType Enum

The `AreaType` enum defines all supported JavaFX container types with their default CSS classes and styles.

### Layout Panes

| Type | Description | Default Alignment | Use Case |
|------|-------------|-------------------|----------|
| `PANE` | Basic pane | None | Absolute positioning |
| `STACKPANE` | Stacks children | Center | Overlaying elements |
| `ANCHORPANE` | Anchored positioning | None | Relative positioning to edges |
| `BORDERPANE` | Five regions (top, bottom, left, right, center) | None | Application layouts |
| `FLOWPANE` | Wraps content | Top-left | Flexible wrapping layouts |
| `GRIDPANE` | Grid-based layout | Top-left | Form layouts, tables |
| `HBOX` | Horizontal box | Center-left | Horizontal arrangements |
| `VBOX` | Vertical box | Top-center | Vertical arrangements |
| `TILEPANE` | Uniform tile grid | Top-left | Image galleries, buttons |

### Container Types

| Type | Description | Use Case |
|------|-------------|----------|
| `SCROLLPANE` | Scrollable content | Large content areas |
| `SPLITPANE` | Resizable divider | Split views |
| `TABPANE` | Tabbed interface | Multi-page content |
| `TAB` | Individual tab | Tab content |
| `ACCORDION` | Collapsible sections | Hierarchical content |
| `TITLEDPANE` | Titled, collapsible pane | Section grouping |

### Special Types

| Type | Description | Use Case |
|------|-------------|----------|
| `GROUP` | Node grouping | Transform grouping |
| `REGION` | Custom region | Custom layouts |
| `CANVAS` | Drawing surface | Graphics, charts |
| `CUSTOM` | Custom container | Fallback/custom types |

### CSS Configuration

- **CSS File**: `/css/screen-areas.css`
- **Class Prefix**: `screen-area-`
- **Example**: `screen-area-vbox`, `screen-area-gridpane`

---

## AreaItem Class

The `AreaItem` class defines individual UI elements within an area, including their positioning, appearance, behavior, and layout properties.

### Core Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Item identifier |
| `sequence` | int | Display order (default: 0) |
| `layoutPos` | String | Layout position (e.g., "0,0" for GridPane, "top" for BorderPane) |
| `varRef` | String | Reference to a screen variable |
| `displayMetadata` | DisplayMetadata | Optional item-specific display metadata |

### UI Behavior Properties

| Property | Type | Description | Example Values |
|----------|------|-------------|----------------|
| `promptText` | String | Placeholder text | "Enter username..." |
| `editable` | Boolean | Whether editable | true, false |
| `disabled` | Boolean | Disabled state | true, false |
| `visible` | Boolean | Visibility | true, false |
| `tooltip` | String | Hover tooltip | "Username must be alphanumeric" |
| `textColor` | String | Text color | "#333333", "red" |
| `backgroundColor` | String | Background color | "#FFFFFF", "lightblue" |

### Layout Properties

#### Grid Layout (GridPane)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `layoutPos` | String | Grid position (row,col) | "0,0", "2,1" |
| `colSpan` | Integer | Column span | 1, 2, 3 |
| `rowSpan` | Integer | Row span | 1, 2, 3 |

#### Growth Priority (HBox/VBox)

| Property | Type | Description | Values |
|----------|------|-------------|--------|
| `hgrow` | String | Horizontal grow priority | "ALWAYS", "SOMETIMES", "NEVER" |
| `vgrow` | String | Vertical grow priority | "ALWAYS", "SOMETIMES", "NEVER" |

#### Spacing

| Property | Type | Description | Examples |
|----------|------|-------------|----------|
| `margin` | String | External spacing | "10", "10 5", "10 5 10 5" |
| `padding` | String | Internal spacing | "10", "10 5", "10 5 10 5" |

#### Size Constraints

| Property | Type | Description | Examples |
|----------|------|-------------|----------|
| `prefWidth` | String | Preferred width | "300", "50%" |
| `prefHeight` | String | Preferred height | "200", "auto" |
| `minWidth` | String | Minimum width | "100", "50" |
| `minHeight` | String | Minimum height | "50", "30" |
| `maxWidth` | String | Maximum width | "500", "100%" |
| `maxHeight` | String | Maximum height | "400", "80%" |

#### Positioning

| Property | Type | Description | Values |
|----------|------|-------------|--------|
| `alignment` | String | Alignment within parent | "center", "top-left", "bottom-right" |

### DisplayMetadata Fallback

If `displayMetadata` is not provided for an AreaItem, it will automatically use the DisplayMetadata associated with its `varRef` variable. This allows for centralized configuration with item-specific overrides when needed.

```java
// Fallback logic
DisplayMetadata metadata = item.displayMetadata;
if (metadata == null && item.varRef != null) {
    metadata = interpreter.getDisplayMetadata(screenName, item.varRef);
}
```

---

## Usage Examples

### Example 1: Simple Vertical Layout

```javascript
screen "LoginScreen" {
    vars: [{
        name: "username",
        type: "string",
        display: { type: "textfield", mandatory: true }
    }, {
        name: "password",
        type: "string",
        display: { type: "passwordfield", mandatory: true }
    }],
    area: [{
        name: "loginForm",
        type: "vbox",
        style: "-fx-padding: 20; -fx-spacing: 15; -fx-alignment: center;",
        items: [{
            name: "usernameField",
            varRef: "username",
            sequence: 1,
            promptText: "Enter your username",
            prefWidth: "300"
        }, {
            name: "passwordField",
            varRef: "password",
            sequence: 2,
            promptText: "Enter your password",
            prefWidth: "300"
        }]
    }]
}
```

### Example 2: Grid Layout with Spanning

```javascript
screen "FormScreen" {
    vars: [{
        name: "firstName",
        display: { type: "textfield" }
    }, {
        name: "lastName",
        display: { type: "textfield" }
    }, {
        name: "address",
        display: { type: "textarea" }
    }],
    area: [{
        name: "formGrid",
        type: "gridpane",
        style: "-fx-hgap: 10; -fx-vgap: 10; -fx-padding: 20;",
        items: [{
            varRef: "firstName",
            layoutPos: "0,0",
            hgrow: "ALWAYS",
            prefWidth: "200"
        }, {
            varRef: "lastName",
            layoutPos: "0,1",
            hgrow: "ALWAYS",
            prefWidth: "200"
        }, {
            varRef: "address",
            layoutPos: "1,0",
            colSpan: 2,
            rowSpan: 2,
            hgrow: "ALWAYS",
            vgrow: "ALWAYS",
            prefHeight: "100"
        }]
    }]
}
```

### Example 3: Complex Layout with Multiple Areas

```javascript
screen "DashboardScreen" {
    vars: [{
        name: "title",
        display: { type: "labeltext", alignment: "center" }
    }, {
        name: "statusMessage",
        display: { type: "text" }
    }, {
        name: "dataInput",
        display: { type: "textfield" }
    }],
    area: [{
        name: "header",
        type: "hbox",
        style: "-fx-padding: 10; -fx-background-color: #f0f0f0;",
        items: [{
            varRef: "title",
            hgrow: "ALWAYS",
            alignment: "center",
            textColor: "#333333"
        }]
    }, {
        name: "mainContent",
        type: "borderpane",
        items: [{
            varRef: "statusMessage",
            layoutPos: "top",
            margin: "10",
            backgroundColor: "#e8f5e9"
        }, {
            varRef: "dataInput",
            layoutPos: "center",
            margin: "20",
            prefWidth: "400",
            promptText: "Enter data here..."
        }]
    }]
}
```

### Example 4: Display-Only Items with Custom Styling

```javascript
screen "InfoScreen" {
    vars: [{
        name: "welcomeText",
        display: { type: "labeltext" }
    }, {
        name: "separator",
        display: { type: "separator" }
    }, {
        name: "infoChart",
        display: { type: "chart" }
    }],
    area: [{
        name: "infoArea",
        type: "vbox",
        style: "-fx-spacing: 20; -fx-padding: 30;",
        items: [{
            varRef: "welcomeText",
            sequence: 1,
            textColor: "#1976d2",
            alignment: "center",
            style: "-fx-font-size: 24px; -fx-font-weight: bold;"
        }, {
            varRef: "separator",
            sequence: 2,
            prefWidth: "100%",
            maxHeight: "2"
        }, {
            varRef: "infoChart",
            sequence: 3,
            prefWidth: "600",
            prefHeight: "400",
            hgrow: "ALWAYS",
            vgrow: "ALWAYS"
        }]
    }]
}
```

---

## Best Practices

### 1. Layout Selection

- **VBOX/HBOX**: Simple linear layouts, forms
- **GRIDPANE**: Complex forms, tabular data
- **BORDERPANE**: Application-level layouts (header, footer, sidebar)
- **STACKPANE**: Overlays, centered content
- **SCROLLPANE**: Large or dynamic content that may exceed viewport

### 2. Positioning Strategy

- Use `sequence` for ordering items within any container
- Use `layoutPos` for precise positioning in grids and border panes
- Use `alignment` for fine-tuning placement within containers

### 3. Responsive Design

- Set `hgrow` and `vgrow` to "ALWAYS" for items that should expand
- Use `prefWidth`/`prefHeight` for initial sizing
- Set `minWidth`/`minHeight` to prevent items from becoming too small
- Use `maxWidth`/`maxHeight` to cap maximum sizes

### 4. Spacing and Margins

- Apply margin to individual items for fine control
- Use padding for internal spacing within items
- Configure container-level spacing in the area's `style` property

### 5. DisplayMetadata Strategy

- Define common display properties in variable definitions
- Override at the AreaItem level only when necessary
- Keep item-specific overrides minimal for maintainability

### 6. Naming Conventions

- Use descriptive names for areas (e.g., "loginForm", "headerArea")
- Name items based on their purpose (e.g., "usernameField", "submitButton")
- Use consistent naming patterns across screens

### 7. Color and Styling

- Use hex colors for consistency (#RRGGBB)
- Named colors are acceptable for readability ("red", "blue")
- Apply styles at the area level for consistency
- Override at item level only for exceptions

### 8. Performance Considerations

- Avoid deeply nested areas when possible
- Use appropriate container types (e.g., FLOWPANE for wrapping vs GRIDPANE)
- Minimize the number of style overrides
- Set visibility to false for items that should be hidden initially

---

## Property Support Matrix

### Case Sensitivity

All properties support both **camelCase** and **snake_case** naming:
- `layoutPos` or `layout_pos`
- `colSpan` or `col_span`
- `promptText` or `prompt_text`
- `textColor` or `text_color`
- `backgroundColor` or `background_color`

### Backward Compatibility

The old property name `relativePos` (and `relative_pos`) is still supported for backward compatibility but `layoutPos` is recommended.

---

## Related Components

- **DisplayMetadata**: Defines display properties for variables (see DisplayMetadata.java)
- **ItemType**: Enum of all available input and display item types
- **Screen Statement**: Top-level screen definition containing areas

---

## Technical Notes

### CSS Class Generation

Each AreaType generates a CSS class in the format: `screen-area-{typename}`

Example:
- VBOX → `screen-area-vbox`
- GRIDPANE → `screen-area-gridpane`

### Default Styles

Each AreaType has sensible default styles that can be overridden:
- Layout panes default to transparent backgrounds
- Spacing defaults vary by container type (10px for HBOX/VBOX, 5px for FLOWPANE, etc.)

### Parsing Order

Items are parsed in sequence and can be re-ordered using the `sequence` property. Lower sequence numbers appear first.

---

## API Reference

For developers working with AreaDefinition programmatically:

```java
// Get AreaType from string
AreaType type = AreaType.fromString("vbox");

// Access properties
String cssClass = type.getCssClass();
String defaultStyle = type.getDefaultStyle();

// Create area definition
AreaDefinition area = new AreaDefinition();
area.name = "myArea";
area.areaType = AreaType.VBOX;

// Add items
AreaItem item = new AreaItem();
item.varRef = "username";
item.layoutPos = "0,0";
area.items.add(item);
```

---

## Version History

- **v1.0**: Initial AreaDefinition implementation with AreaType enum
- **v1.1**: Added AreaItem with display metadata support
- **v1.2**: Added UI behavior properties (promptText, editable, disabled, etc.)
- **v1.3**: Renamed InputItemType to ItemType, added display-only items
- **v1.4**: Renamed relativePos to layoutPos, added comprehensive layout properties

---

## Support

For questions, issues, or contributions related to AreaDefinition:
- See ARCHITECTURE.md for system architecture details
- See README.md for general project information
- Review the Interpreter.java implementation for parsing details
