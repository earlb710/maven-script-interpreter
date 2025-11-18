# JSON Schemas for EBS Screen System

This directory contains JSON Schema definitions for the EBS (Earl Bosch Script) screen system components.

## Schemas

### 1. screen-definition.json
Complete schema for screen definitions including all components.

**Use for**: Validating entire screen definitions (the top-level structure).

**Key properties**:
- `name`: Screen identifier
- `title`: Window title
- `width`, `height`: Window dimensions
- `vars`: Array of variable definitions with display metadata
  - `name`: Variable name (required)
  - `type`: Data type (string, int, float, double, bool, date, etc.)
  - `scope`: Variable scope - "internal" for internal-only variables not displayed in UI, "parameter" for variables that may be displayed/accessed (default: "parameter")
  - `direction`: Parameter direction - "in" for input-only, "out" for output-only, "inout" for bidirectional (default: "inout")
  - `display`: Display metadata for UI rendering (optional)
- `area`: Array of container area definitions

### 2. area-definition.json
Schema for container area definitions (layout containers).

**Use for**: Validating individual container/area definitions within a screen.

**Key properties**:
- `name`: Area identifier
- `type`: Container type (vbox, hbox, gridpane, borderpane, etc.)
- `layout`: Layout configuration
- `style`: JavaFX CSS styling
- `items`: Array of UI items/controls

**Supported container types** (19):
- Layout Panes: pane, stackpane, anchorpane, borderpane, flowpane, gridpane, hbox, vbox, tilepane
- Containers: scrollpane, splitpane, tabpane, tab, accordion, titledpane
- Special: group, region, canvas, custom

### 3. display-metadata.json
Schema for display metadata (UI control types and validation).

**Use for**: Validating display properties for variables and items.

**Key properties**:
- `type`: UI control type (textfield, button, label, etc.)
- `mandatory`: Whether field is required
- `min`, `max`: Value constraints
- `pattern`: Regex validation pattern
- `case`: Text case transformation
- `alignment`: Content alignment
- `style`: JavaFX CSS styling

**Supported control types** (26):
- Text Input: textfield, textarea, passwordfield
- Selection: checkbox, radiobutton, togglebutton, combobox, choicebox, listview
- Numeric: spinner, slider
- Date/Time: datepicker
- Color: colorpicker
- Button: button
- Display: label, labeltext, text, hyperlink, separator
- Media: imageview, mediaview, webview, chart
- Progress: progressbar, progressindicator
- Custom: custom

## Usage

### Validation in Code

```java
// Load schema
InputStream schemaStream = getClass().getResourceAsStream("/json/screen-definition.json");

// Validate JSON against schema
// (Using a JSON Schema validation library like everit-org/json-schema)
```

### IDE Integration

Most modern IDEs support JSON Schema validation:

1. **VS Code**: Add schema reference in your JSON file:
   ```json
   {
     "$schema": "path/to/screen-definition.json",
     "name": "MyScreen",
     ...
   }
   ```

2. **IntelliJ IDEA**: Configure JSON Schema mappings in Settings → Languages & Frameworks → Schemas and DTDs → JSON Schema Mappings

### Validation Tools

- **Online**: [JSONSchemaValidator.net](https://www.jsonschemavalidator.net/)
- **CLI**: Use `ajv-cli` (npm package)
  ```bash
  npm install -g ajv-cli
  ajv validate -s screen-definition.json -d your-screen.json
  ```

## Examples

### Complete Screen Definition

```json
{
  "name": "LoginScreen",
  "title": "Login",
  "width": 400,
  "height": 300,
  "vars": [
    {
      "name": "username",
      "type": "string",
      "scope": "parameter",
      "direction": "in",
      "display": {
        "type": "textfield",
        "mandatory": true
      }
    },
    {
      "name": "password",
      "type": "string",
      "scope": "parameter",
      "direction": "in",
      "display": {
        "type": "passwordfield",
        "mandatory": true
      }
    },
    {
      "name": "sessionToken",
      "type": "string",
      "scope": "internal"
    }
  ],
  "area": [
    {
      "name": "loginForm",
      "type": "vbox",
      "style": "-fx-padding: 20; -fx-spacing: 15;",
      "items": [
        {
          "name": "usernameField",
          "varRef": "username",
          "sequence": 1,
          "promptText": "Enter username",
          "prefWidth": "300"
        },
        {
          "name": "passwordField",
          "varRef": "password",
          "sequence": 2,
          "promptText": "Enter password",
          "prefWidth": "300"
        }
      ]
    }
  ]
}
```

### Area Definition with GridPane

```json
{
  "name": "formGrid",
  "type": "gridpane",
  "style": "-fx-hgap: 10; -fx-vgap: 10; -fx-padding: 20;",
  "items": [
    {
      "varRef": "firstName",
      "layoutPos": "0,0",
      "hgrow": "ALWAYS",
      "prefWidth": "200"
    },
    {
      "varRef": "lastName",
      "layoutPos": "0,1",
      "hgrow": "ALWAYS",
      "prefWidth": "200"
    },
    {
      "varRef": "address",
      "layoutPos": "1,0",
      "colSpan": 2,
      "rowSpan": 2,
      "hgrow": "ALWAYS",
      "vgrow": "ALWAYS"
    }
  ]
}
```

### Display Metadata Examples

```json
{
  "type": "textfield",
  "mandatory": true,
  "pattern": "^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$",
  "alignment": "left"
}
```

```json
{
  "type": "spinner",
  "min": 0,
  "max": 100
}
```

```json
{
  "type": "datepicker",
  "min": "2024-01-01",
  "max": "2024-12-31",
  "mandatory": true
}
```

## Schema Relationships

```
screen-definition.json
├── vars[]
│   └── display → display-metadata.json
└── area[] → area-definition.json
    └── items[]
        └── display → display-metadata.json
```

## Property Name Conventions

The schemas support both naming conventions:
- **camelCase**: `varRef`, `prefWidth`, `layoutPos` (primary)
- **snake_case**: `var_ref`, `pref_width`, `layout_pos` (alternative)

Both are accepted by the ScreenFactory parser.

**Case-Insensitive Key Lookup**: When screen definitions are parsed from JSON, the EBS JSON parser uses case-insensitive mode, which converts all keys to lowercase. This means:
- `varRef`, `VarRef`, `varref` → all normalized to `"varref"`
- `promptText`, `PromptText`, `prompttext` → all normalized to `"prompttext"`
- String values preserve their original casing
- The ScreenFactory parser looks up properties using lowercase keys

## Notes

- All schemas follow JSON Schema Draft 07
- Schemas include detailed descriptions and examples
- Default values are specified where applicable
- Enums provide valid value constraints
- Patterns validate text formats (margins, padding, etc.)

## Related Documentation

- **AREA_DEFINITION.md**: Detailed documentation on area and item definitions
- **ScreenFactory**: JavaFX screen creation from definitions
- **AreaItemFactory**: UI control creation
- **AreaContainerFactory**: Container creation
