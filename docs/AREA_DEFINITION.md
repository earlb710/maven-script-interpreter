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

#### Core Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Unique identifier for the area |
| `areaType` | AreaType | The JavaFX container type (enum) |
| `type` | String | Area type as string (for compatibility) |
| `cssClass` | String | CSS class name from the AreaType enum |
| `layout` | String | Layout configuration string |
| `style` | String | CSS style string (defaults to areaType's default) |
| `screenName` | String | Associated screen name |
| `displayName` | String | Display name for UI elements (e.g., tab labels) |
| `title` | String | Title for titled containers (e.g., TitledPane, group headers) |

#### Layout Properties

| Property | Type | Description |
|----------|------|-------------|
| `spacing` | String | Spacing between child elements (e.g., "10", "15") |
| `padding` | String | Internal padding around children (e.g., "10", "10 5", "10 5 10 5") |
| `areaBackground` | String | Area background color in hex format (e.g., "#f0f0f0") |

#### Group Border Properties

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `groupBorder` | String | Border style: none, raised, inset, lowered, line | none |
| `groupBorderColor` | String | Border color in hex format (e.g., "#4a9eff") | #808080 |
| `groupBorderWidth` | String | Border width in pixels (e.g., "2" or "2px") | 2px (1px for line) |
| `groupBorderInsets` | String | Border insets (e.g., "5", "5 10", "5 10 5 10") | - |
| `groupBorderRadius` | String | Border corner radius (e.g., "5" or "5px") | 5px |

#### Group Label Properties

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `groupLabelText` | String | Label text displayed on/near the group border | - |
| `groupLabelAlignment` | String | Label alignment: left, center, right | left |
| `groupLabelOffset` | String | Vertical position: top, on, bottom | on |
| `groupLabelColor` | String | Label text color in hex format (e.g., "#4a9eff") | groupBorderColor |
| `groupLabelBackground` | String | Label background color (e.g., "#ffffff") | white |

#### Event Handler Properties

| Property | Type | Description |
|----------|------|-------------|
| `gainFocus` | String | Inline EBS code to execute when focus enters this area |
| `lostFocus` | String | Inline EBS code to execute when focus leaves this area |

#### Multi-Record Properties

| Property | Type | Description |
|----------|------|-------------|
| `numberOfRecords` | Integer | Number of records - items become templates duplicated for each record |
| `recordRef` | String | Array variable name for expanding items (e.g., "clients") |

#### Container Properties

| Property | Type | Description |
|----------|------|-------------|
| `items` | List\<AreaItem\> | List of items contained in this area |
| `childAreas` | List\<AreaDefinition\> | Nested child areas for hierarchical layouts |

### EBS Script Syntax

```javascript
area: [{
    name: "mainArea",
    type: "vbox",
    layout: "fill",
    spacing: "15",
    padding: "20",
    style: "-fx-background-color: #f0f0f0;",
    items: [
        // AreaItem definitions
    ]
}]
```

### Group Border Example

```javascript
area: [{
    name: "personalInfo",
    type: "vbox",
    groupBorder: "line",
    groupBorderColor: "#4a9eff",
    groupBorderWidth: "2",
    groupBorderInsets: "10 15 10 15",
    groupBorderRadius: "8",
    groupLabelText: "Personal Information",
    groupLabelAlignment: "left",
    groupLabelOffset: "on",
    groupLabelColor: "#4a9eff",
    groupLabelBackground: "white",
    areaBackground: "#f8f9fa",
    spacing: "10",
    padding: "15",
    items: [
        { varRef: "firstName", sequence: 1 },
        { varRef: "lastName", sequence: 2 }
    ]
}]
```

### Focus Event Handler Example

```javascript
area: [{
    name: "formSection",
    type: "vbox",
    gainFocus: "println('Focus entered form section');",
    lostFocus: "println('Focus left form section');",
    items: [...]
}]
```

### Multi-Record Area Example

```javascript
area: [{
    name: "clientList",
    type: "vbox",
    numberOfRecords: 5,
    recordRef: "clients",
    items: [
        // Items will be duplicated 5 times with varRef expanded:
        // clients[0].name, clients[1].name, etc.
        { varRef: "name", sequence: 1, display: { labelText: "Client - Name:" } }
    ]
}]
```

### Spacing and Padding Properties

The `spacing` and `padding` properties provide direct control over the layout spacing in area definitions without requiring inline CSS styles.

#### Spacing Property

**Purpose**: Controls the gap between child elements within a container.

**Applies To**: 
- `HBox` - horizontal spacing between children
- `VBox` - vertical spacing between children
- `FlowPane` - horizontal and vertical gaps (hgap and vgap)
- `GridPane` - horizontal and vertical gaps (hgap and vgap)
- `TilePane` - horizontal and vertical gaps (hgap and vgap)

**Format**: Single numeric value (e.g., "10", "15", "20")

**Examples**:
```javascript
// VBox with 20 pixels between each child
{
    name: "listArea",
    type: "vbox",
    spacing: "20",
    items: [...]
}

// GridPane with 15 pixels between rows and columns
{
    name: "formGrid",
    type: "gridpane",
    spacing: "15",
    items: [...]
}
```

#### Padding Property

**Purpose**: Controls the internal space around children within a container, creating a margin between the container's edges and its content.

**Applies To**: All Region types (VBox, HBox, GridPane, BorderPane, etc.)

**Formats**:
- Single value: `"10"` - applies to all sides (top, right, bottom, left)
- Two values: `"10 5"` - first is vertical (top/bottom), second is horizontal (left/right)
- Four values: `"10 5 10 5"` - top, right, bottom, left (clockwise from top)

**Examples**:
```javascript
// Padding of 30 pixels on all sides
{
    name: "mainArea",
    type: "vbox",
    padding: "30",
    items: [...]
}

// Vertical padding 20px, horizontal padding 30px
{
    name: "formArea",
    type: "vbox",
    padding: "20 30",
    items: [...]
}

// Different padding for each side: top=10, right=20, bottom=10, left=20
{
    name: "contentArea",
    type: "hbox",
    padding: "10 20 10 20",
    items: [...]
}
```

#### Spacing vs Padding

| Property | Purpose | Scope | Common Use |
|----------|---------|-------|------------|
| `spacing` | Gap between children | Between elements | Separate form fields, buttons, list items |
| `padding` | Internal margin | Around all children | Create breathing room, offset from container edges |

**Combined Example**:
```javascript
{
    name: "formContainer",
    type: "vbox",
    spacing: "15",           // 15px between each form field
    padding: "25 30 25 30",  // 25px top/bottom, 30px left/right margins
    style: "-fx-background-color: white; -fx-border-color: #cccccc;",
    items: [
        { varRef: "field1", sequence: 1 },
        { varRef: "field2", sequence: 2 },
        { varRef: "field3", sequence: 3 }
    ]
}
```

#### Relationship with Style Property

The `spacing` and `padding` properties are applied **before** the `style` property, allowing the style to override them if needed. However, it's recommended to use `spacing` and `padding` properties for clarity and maintainability, reserving `style` for other CSS properties.

```javascript
// Good: Use properties for spacing and padding
{
    type: "vbox",
    spacing: "15",
    padding: "20",
    style: "-fx-background-color: #f0f0f0;"
}

// Less ideal: Mix in style (harder to read and maintain)
{
    type: "vbox",
    style: "-fx-spacing: 15; -fx-padding: 20; -fx-background-color: #f0f0f0;"
}
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
| `editable` | Boolean | Whether editable | true, false |
| `disabled` | Boolean | Disabled state | true, false |
| `visible` | Boolean | Visibility | true, false |
| `tooltip` | String | Hover tooltip | "Username must be alphanumeric" |
| `textColor` | String | Text color | "#333333", "red" |
| `backgroundColor` | String | Background color | "#FFFFFF", "lightblue" |

### Data Source Properties

| Property | Type | Description | Values |
|----------|------|-------------|--------|
| `source` | String | Source of the value | "data" (original data value), "display" (formatted display value) |

**Note:** `promptHelp` (placeholder text) is a DisplayItem property, not an AreaItem property. Set it in the variable's `display` definition or in the item's `displayItem` override.

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

### Event Handlers

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `onValidate` | String | EBS code to validate item value | "if (age < 18) { return false; } return true;" |

The `onValidate` property allows you to specify inline EBS code that validates the item's value whenever it changes. The validation code:
- Is executed automatically when the control's value changes
- Must return a boolean value (`true` for valid, `false` for invalid)
- Has access to all screen variables
- Causes the control to be marked with a red border when validation fails
- Can be defined at either the variable's display level or the individual item level
- Item-level `onValidate` takes precedence over variable-level `onValidate`

**Example: Email validation**
```javascript
{
    "varRef": "email",
    "sequence": 1,
    "onValidate": "if (string.length(email) > 0) { var hasAt = string.contains(email, '@'); if (not hasAt) { return false; } } return true;"
}
```

**Example: Age range validation**
```javascript
{
    "name": "age",
    "type": "int",
    "display": {
        "type": "textfield",
        "onValidate": "if (age < 18 or age > 100) { return false; } return true;"
    }
}
```

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
| `onValidate` | String | Inline EBS code for validation | "return age >= 18;" |

### Event Handler Properties

#### onValidate

The `onValidate` property defines inline validation logic for a variable's display. When defined at the variable level in the `display` object, this validation will apply to all instances of that variable unless overridden at the item level.

**Format**: String containing EBS code that must return a boolean value
**Trigger**: Executed automatically whenever the control's value changes
**Return Value**: 
- `true` - Value is valid (removes error styling)
- `false` - Value is invalid (applies red border)

**Example: Username length validation**
```javascript
{
    "name": "username",
    "type": "string",
    "display": {
        "type": "textfield",
        "onValidate": "var len = string.length(username); if (len < 3 and len > 0) { return false; } return true;"
    }
}
```

**Example: Numeric range validation**
```javascript
{
    "name": "age",
    "type": "int",
    "display": {
        "type": "spinner",
        "min": 0,
        "max": 150,
        "onValidate": "if (age < 18 or age > 100) { return false; } return true;"
    }
}
```

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

### Example 1: Simple Vertical Layout with Spacing and Padding

```javascript
screen "LoginScreen" {
    vars: [{
        name: "username",
        type: "string",
        display: { 
            type: "textfield", 
            mandatory: true,
            labelText: "Username:"
        }
    }, {
        name: "password",
        type: "string",
        display: { 
            type: "passwordfield", 
            mandatory: true,
            labelText: "Password:"
        }
    }],
    area: [{
        name: "loginForm",
        type: "vbox",
        spacing: "15",
        padding: "20",
        style: "-fx-alignment: center; -fx-background-color: #f5f5f5;",
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

### Example 2: Grid Layout with Spacing

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
        spacing: "10",
        padding: "20",
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
        padding: "10",
        style: "-fx-background-color: #f0f0f0;",
        items: [{
            varRef: "title",
            hgrow: "ALWAYS",
            alignment: "center",
            textColor: "#333333"
        }]
    }, {
        name: "mainContent",
        type: "borderpane",
        padding: "15",
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

### Example 4: Nested Areas with Independent Spacing

```javascript
screen "NestedAreasScreen" {
    vars: [{
        name: "title",
        display: { type: "labeltext" }
    }, {
        name: "field1",
        display: { type: "textfield", labelText: "Field 1:" }
    }, {
        name: "field2",
        display: { type: "textfield", labelText: "Field 2:" }
    }],
    area: [{
        name: "outerContainer",
        type: "vbox",
        spacing: "20",
        padding: "25 30",
        style: "-fx-background-color: #f5f5f5;",
        items: [{
            varRef: "title",
            sequence: 1
        }],
        areas: [{
            name: "innerForm",
            type: "vbox",
            spacing: "12",
            padding: "15",
            style: "-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;",
            items: [{
                varRef: "field1",
                sequence: 1,
                prefWidth: "400"
            }, {
                varRef: "field2",
                sequence: 2,
                prefWidth: "400"
            }]
        }]
    }]
}
```

### Example 5: Display-Only Items with Custom Styling

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
        spacing: "20",
        padding: "30",
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

### 1. Use Spacing and Padding Properties

- **Prefer properties over inline CSS**: Use `spacing` and `padding` properties instead of CSS for clarity
- **Spacing**: Use for consistent gaps between child elements
- **Padding**: Use to create breathing room around content
- **Combine both**: Use together for professional, well-spaced layouts

```javascript
// Good: Clear and maintainable
{
    type: "vbox",
    spacing: "15",
    padding: "20",
    style: "-fx-background-color: white;"
}

// Less ideal: Everything in CSS
{
    type: "vbox",
    style: "-fx-spacing: 15; -fx-padding: 20; -fx-background-color: white;"
}
```

### 2. Layout Selection

- **VBOX/HBOX**: Simple linear layouts, forms
- **GRIDPANE**: Complex forms, tabular data
- **BORDERPANE**: Application-level layouts (header, footer, sidebar)
- **STACKPANE**: Overlays, centered content
- **SCROLLPANE**: Large or dynamic content that may exceed viewport

### 3. Positioning Strategy

- Use `sequence` for ordering items within any container
- Use `layoutPos` for precise positioning in grids and border panes
- Use `alignment` for fine-tuning placement within containers

### 4. Responsive Design

- Set `hgrow` and `vgrow` to "ALWAYS" for items that should expand
- Use `prefWidth`/`prefHeight` for initial sizing
- Set `minWidth`/`minHeight` to prevent items from becoming too small
- Use `maxWidth`/`maxHeight` to cap maximum sizes

### 5. Spacing and Padding Best Practices

- **Use area-level spacing**: Set spacing at the area level for consistent gaps between children
- **Use area-level padding**: Set padding at the area level to create uniform margins
- **Item-level margin**: Use item `margin` property for fine-tuned control of individual items
- **Item-level padding**: Use item `padding` property for internal spacing within specific items
- **Consistent values**: Use consistent spacing values (e.g., 10, 15, 20) throughout your application

### 6. DisplayMetadata Strategy

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
