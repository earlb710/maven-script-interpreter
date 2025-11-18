# AreaDefinition Documentation

## Overview

The `AreaDefinition` class and its sub-components provide a comprehensive framework for defining JavaFX UI layouts within the EBS (Earl Bosch Script) screen system. This system allows developers to create structured, responsive user interfaces with precise control over layout, positioning, and styling.

## Table of Contents

- [AreaDefinition Class](#areadefinition-class)
- [AreaType Enum](#areatype-enum)
- [AreaItem Class](#areaitem-class)
- [DisplayMetadata Class](#displaymetadata-class)
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
| `promptHelp` | String | Placeholder text | "Enter username..." |
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

## DisplayMetadata Class

The `DisplayMetadata` class defines the display properties and behavior for UI items. It controls how variables are rendered visually, including their type, validation, styling, and constraints. AreaItems can optionally override these settings or use the metadata defined for their associated variable.

### Properties

| Property | Type | Description | Example Values |
|----------|------|-------------|----------------|
| `itemType` | ItemType | The UI control type (enum) | TEXTFIELD, LABEL, BUTTON |
| `type` | String | Item type as string (for compatibility) | "textfield", "label" |
| `cssClass` | String | CSS class name from ItemType enum | "screen-item-textfield" |
| `mandatory` | boolean | Whether field is required | true, false |
| `caseFormat` | String | Text case transformation | "upper", "lower", "title" |
| `min` | Object | Minimum value constraint | 0, "2024-01-01" |
| `max` | Object | Maximum value constraint | 100, "2024-12-31" |
| `style` | String | CSS style string | "-fx-font-size: 14px;" |
| `screenName` | String | Associated screen name | "LoginScreen" |
| `alignment` | String | Text/content alignment | "left", "center", "right" |
| `pattern` | String | Regex validation pattern | "^[a-zA-Z0-9]+$" |

### ItemType Enum

The `ItemType` enum defines all available UI control types, organized by category:

#### Text Input Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `TEXTFIELD` | Single-line text input | Usernames, short text |
| `TEXTAREA` | Multi-line text input | Comments, descriptions |
| `PASSWORDFIELD` | Masked password input | Passwords, sensitive data |

#### Selection Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `CHECKBOX` | Boolean selection | Accept terms, enable features |
| `RADIOBUTTON` | Single selection from group | Gender, payment method |
| `TOGGLEBUTTON` | Toggle state button | On/off switches |
| `COMBOBOX` | Dropdown selection (editable) | Country, category selection |
| `CHOICEBOX` | Dropdown selection | Simple selections |
| `LISTVIEW` | List of items | Multiple options display |

#### Numeric Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `SPINNER` | Numeric input with increment/decrement | Quantity, age |
| `SLIDER` | Numeric input via slider | Volume, brightness |

#### Date/Time Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `DATEPICKER` | Date selection calendar | Birth date, appointment |

#### Color Control

| Type | Description | Use Case |
|------|-------------|----------|
| `COLORPICKER` | Color selection | Theme customization |

#### Button Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `BUTTON` | Action button | Submit, cancel, save |

#### Display-Only Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `LABEL` | Text label with padding | Form labels, titles |
| `LABELTEXT` | Text label (alias for LABEL) | Display text |
| `TEXT` | Plain text (no padding) | Inline text, messages |
| `HYPERLINK` | Clickable link | URLs, navigation |
| `SEPARATOR` | Visual divider | Section separation |

#### Media/Display Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `IMAGEVIEW` | Image display | Photos, icons |
| `MEDIAVIEW` | Video/audio player | Media playback |
| `WEBVIEW` | Embedded web browser | HTML content |
| `CHART` | Data visualization | Graphs, charts |

#### Progress/Status Controls

| Type | Description | Use Case |
|------|-------------|----------|
| `PROGRESSBAR` | Horizontal progress bar | Download progress |
| `PROGRESSINDICATOR` | Circular progress | Loading spinner |

#### Custom

| Type | Description | Use Case |
|------|-------------|----------|
| `CUSTOM` | Custom/fallback type | Extension point |

### CSS Configuration

- **CSS File**: `/css/screen-items.css`
- **Class Prefix**: `screen-item-`
- **Example Classes**: `screen-item-textfield`, `screen-item-button`, `screen-item-label`

### EBS Script Syntax

DisplayMetadata is typically defined within variable definitions:

```javascript
vars: [{
    name: "username",
    type: "string",
    display: {
        type: "textfield",
        mandatory: true,
        alignment: "left",
        pattern: "^[a-zA-Z0-9_]{3,20}$",
        style: "-fx-font-size: 14px;"
    }
}, {
    name: "age",
    type: "int",
    display: {
        type: "spinner",
        min: 0,
        max: 120
    }
}, {
    name: "welcomeMessage",
    type: "string",
    display: {
        type: "labeltext",
        alignment: "center",
        style: "-fx-font-size: 18px; -fx-font-weight: bold;"
    }
}]
```

### Fallback Mechanism

AreaItems can override DisplayMetadata at the item level. When an AreaItem doesn't specify its own `displayMetadata`, it automatically uses the DisplayMetadata defined for its associated variable (`varRef`):

```java
// Automatic fallback logic
DisplayMetadata metadata = item.displayMetadata;
if (metadata == null && item.varRef != null) {
    metadata = interpreter.getDisplayMetadata(screenName, item.varRef);
}
```

This allows for:
1. **Centralized configuration**: Define common properties once in the variable
2. **Flexible overrides**: Customize specific items when needed
3. **Consistency**: Default behavior promotes uniform UIs

### DisplayMetadata vs AreaItem Properties

Some properties can be specified in both DisplayMetadata and AreaItem. Understanding the relationship:

| Property Category | DisplayMetadata | AreaItem | Notes |
|-------------------|----------------|----------|-------|
| **Control Type** | `itemType`, `type` | `displayMetadata.itemType` | Can be overridden per item |
| **Validation** | `mandatory`, `pattern`, `min`, `max` | - | Defined at variable level |
| **Text Formatting** | `caseFormat`, `alignment` | `alignment` | AreaItem alignment affects layout positioning |
| **Styling** | `style`, `cssClass` | - | Base styling from DisplayMetadata |
| **Colors** | - | `textColor`, `backgroundColor` | Item-specific colors |
| **Behavior** | - | `editable`, `disabled`, `visible` | Runtime state control |
| **UI Text** | - | `promptHelp`, `tooltip` | Item-specific hints |

### Property Inheritance Example

```javascript
screen "UserForm" {
    vars: [{
        name: "email",
        type: "string",
        display: {
            type: "textfield",
            mandatory: true,
            pattern: "^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$",
            alignment: "left"
        }
    }],
    area: [{
        name: "formArea",
        type: "vbox",
        items: [{
            // Uses all DisplayMetadata from "email" variable
            varRef: "email",
            sequence: 1
        }, {
            // Overrides DisplayMetadata - shows as passwordfield instead
            varRef: "email",
            sequence: 2,
            display: {
                type: "passwordfield"  // Override: different control type
            },
            promptHelp: "Confirm email",  // Item-specific property
            backgroundColor: "#f0f0f0"     // Item-specific property
        }]
    }]
}
```

In this example:
- First item: Uses email's TEXTFIELD display with validation pattern
- Second item: Overrides to PASSWORDFIELD but keeps validation, adds prompt text and color

### Best Practices for DisplayMetadata

1. **Define at Variable Level**: Set common display properties in variable definitions
2. **Override Sparingly**: Only override at AreaItem level when necessary
3. **Validation First**: Use `mandatory`, `pattern`, `min`, `max` for data integrity
4. **Consistent Types**: Use appropriate ItemTypes for data types (SPINNER for numbers, DATEPICKER for dates)
5. **Meaningful Patterns**: Provide clear regex patterns with validation messages
6. **Accessibility**: Use `mandatory` flag, `tooltip`, and `promptHelp` for user guidance
7. **Styling Strategy**: Define base styles in DisplayMetadata, item-specific colors in AreaItem

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
            promptHelp: "Enter your username",
            prefWidth: "300"
        }, {
            name: "passwordField",
            varRef: "password",
            sequence: 2,
            promptHelp: "Enter your password",
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
            promptHelp: "Enter data here..."
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
- `promptHelp` or `prompt_help`
- `textColor` or `text_color`
- `backgroundColor` or `background_color`

### Backward Compatibility

The old property name `relativePos` (and `relative_pos`) is still supported for backward compatibility but `layoutPos` is recommended.

---

## Related Components

- **DisplayMetadata**: Defines display properties for variables including ItemType, validation, styling, and constraints (see [DisplayMetadata Class](#displaymetadata-class) above and DisplayMetadata.java)
- **ItemType**: Enum of all 24 available input and display item types (part of DisplayMetadata)
- **AreaDefinition**: Container for areas within screens with AreaType and items
- **AreaType**: Enum of 19 JavaFX container types for layouts
- **AreaItem**: Individual items within areas with layout properties and optional DisplayMetadata override
- **Screen Statement**: Top-level screen definition containing variables and areas
- **Interpreter**: Parses and processes screen definitions, handles DisplayMetadata fallback logic

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
- **v1.2**: Added UI behavior properties (promptHelp, editable, disabled, etc.)
- **v1.3**: Renamed InputItemType to ItemType, added display-only items
- **v1.4**: Renamed relativePos to layoutPos, added comprehensive layout properties

---

## Support

For questions, issues, or contributions related to AreaDefinition:
- See ARCHITECTURE.md for system architecture details
- See README.md for general project information
- Review the Interpreter.java implementation for parsing details
