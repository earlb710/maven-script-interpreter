# Variable Scope and Direction Properties

## Overview

This document describes the new `scope` and `direction` properties for screen variable definitions, which provide explicit control over variable visibility and data flow direction.

## Problem Statement

Previously, screen variables lacked explicit indicators for:
1. **Internal variables**: Variables used only for calculations, not displayed in the UI
2. **Parameter direction**: Whether a variable is input-only, output-only, or bidirectional

The implicit "hiddenInd" concept was unclear and needed a better naming convention.

## Solution

Two new optional properties were added to screen variable definitions:

### 1. `scope` Property

Clarifies whether a variable is internal or a parameter.

**Values:**
- `"parameter"` (default): Variable may be displayed in the UI and accessed/modified
- `"internal"`: Variable is not displayed, used only for internal calculations

**Usage:**
```json
{
  "name": "sessionToken",
  "type": "string",
  "scope": "internal",
  "default": "secret-xyz"
}
```

**Validation:**
- Internal variables cannot have `display` metadata (enforced at parse time)
- Invalid scope values will result in an error

### 2. `direction` Property

Indicates the data flow direction for parameter variables.

**Values:**
- `"in"`: Input-only parameter (data flows into the screen)
- `"out"`: Output-only parameter (data flows out of the screen)
- `"inout"` (default): Bidirectional parameter (data flows both ways)

**Usage:**
```json
{
  "name": "userInput",
  "type": "string",
  "scope": "parameter",
  "direction": "in",
  "display": {
    "type": "textfield",
    "labelText": "Enter value:"
  }
}
```

**Note:** The `direction` property is informational and does not currently enforce read/write restrictions.

## Complete Example

```json
screen myApp = {
  "title": "Application Example",
  "width": 600,
  "height": 400,
  "vars": [
    {
      "name": "userName",
      "type": "string",
      "scope": "parameter",
      "direction": "in",
      "default": "",
      "display": {
        "type": "textfield",
        "labelText": "User Name:",
        "mandatory": true
      }
    },
    {
      "name": "status",
      "type": "string",
      "scope": "parameter",
      "direction": "out",
      "default": "",
      "display": {
        "type": "textfield",
        "labelText": "Status:"
      }
    },
    {
      "name": "message",
      "type": "string",
      "scope": "parameter",
      "direction": "inout",
      "default": "",
      "display": {
        "type": "textarea",
        "labelText": "Message:"
      }
    },
    {
      "name": "sessionId",
      "type": "string",
      "scope": "internal",
      "default": "auto-generated-id"
    },
    {
      "name": "internalCounter",
      "type": "int",
      "scope": "internal",
      "default": 0
    }
  ]
};
```

## Implementation Details

### JSON Schema

The schema (`screen-definition.json`) was updated to include:
- `scope` enum with values: `"internal"`, `"parameter"` (default: `"parameter"`)
- `direction` enum with values: `"in"`, `"out"`, `"inout"` (default: `"inout"`)

### Storage

Two new maps were added to `InterpreterContext`:
- `screenVarScopes`: Maps `screenName -> (varName -> scope)`
- `screenVarDirections`: Maps `screenName -> (varName -> direction)`

### Parsing

`InterpreterScreen.java` was updated to:
1. Parse `scope` and `direction` from variable definitions
2. Validate values against allowed options
3. Enforce that internal variables cannot have display metadata
4. Store metadata in context maps
5. Clean up metadata when screens are closed

## Benefits

1. **Clarity**: Explicit properties replace implicit behavior
2. **Documentation**: Variable purpose is self-documenting
3. **Validation**: Parse-time validation prevents configuration errors
4. **Future extensibility**: Direction property enables future enforcement of read/write restrictions
5. **Backward compatibility**: Both properties are optional with sensible defaults

## Migration Guide

Existing screen definitions continue to work without changes:
- Variables without `scope` default to `"parameter"`
- Variables without `direction` default to `"inout"`

To adopt the new properties:
1. Add `"scope": "internal"` to variables that should not be displayed
2. Add appropriate `direction` values to document data flow
3. Remove `display` metadata from internal variables

## Testing

Test scripts are available:
- `scripts/test_var_scope_direction.ebs`: Full test with UI
- `scripts/test_var_scope_no_ui.ebs`: Test without showing UI

## References

- JSON Schema: `src/main/resources/json/screen-definition.json`
- Implementation: `src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java`
- Context: `src/main/java/com/eb/script/interpreter/InterpreterContext.java`
- Documentation: `src/main/resources/json/README.md`
