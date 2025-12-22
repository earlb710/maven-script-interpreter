# Stateful Property for Screen Variables

## Overview

The `stateful` property is a Boolean flag that can be set on screen variables in screen definitions. It controls whether changes to that variable mark the screen as "changed" (dirty state).

**Important Change**: The `stateful` property is now set at the **variable level** (in the `vars` section), not at the item level. This provides better control over which variables should trigger screen dirty state tracking.

## Syntax

```javascript
screen myScreen = {
    "vars": [
        {
            "name": "myData",
            "type": "string",
            "default": "",
            "stateful": true  // Changes mark screen as CHANGED (default)
        },
        {
            "name": "filter",
            "type": "string",
            "default": "",
            "stateful": false  // Changes do NOT mark screen as CHANGED
        }
    ],
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "items": [
                {
                    "name": "dataField",
                    "varref": "myData"
                },
                {
                    "name": "searchFilter",
                    "varref": "filter"
                }
            ]
        }
    ]
};
```

## Default Behavior

- **Default value**: `true`
- **Backward compatible**: All existing screens continue to work without changes
- When `stateful` is not specified, variables behave as they always have (changes mark screen as CHANGED)

## When to Use `stateful: false`

Use `stateful: false` for variables that represent:

### 1. Search and Filter Fields
```javascript
{
    "name": "searchText",
    "type": "string",
    "default": "",
    "stateful": false,
    "display": {
        "type": "textfield",
        "labelText": "Search:"
    }
}
```
Users don't expect search fields to make a form "dirty".

### 2. Preview Controls
```javascript
{
    "name": "previewColor",
    "type": "string",
    "default": "#ffffff",
    "stateful": false,
    "display": {
        "type": "colorpicker",
        "labelText": "Preview Color:"
    }
}
```
Preview controls shouldn't trigger save prompts.

### 3. Temporary Values
```javascript
{
    "name": "tempValue",
    "type": "int",
    "default": 50,
    "stateful": false,
    "display": {
        "type": "slider",
        "min": 0,
        "max": 100
    }
}
```
Temporary values for UI state or calculations.

### 4. UI Preferences
```javascript
{
    "name": "sortBy",
    "type": "string",
    "default": "Name",
    "stateful": false,
    "display": {
        "type": "combobox",
        "options": ["Name", "Date", "Size"]
    }
}
```
Display preferences shouldn't affect data state.

## What `stateful: false` Does

When an item has `stateful: false`:

1. ✅ **Data binding still works**: The variable is updated normally
2. ✅ **onChange handlers still fire**: Event handlers execute as expected
3. ✅ **Validation still runs**: onValidate handlers work normally
4. ❌ **Screen status NOT changed**: Screen stays CLEAN when this item changes
5. ❌ **Item NOT marked changed**: Debug panel doesn't show change indicator
6. ❌ **No save prompt**: Closing screen won't prompt about unsaved changes

## What `stateful: false` Does NOT Do

- Does NOT disable the control
- Does NOT prevent data updates
- Does NOT skip event handlers
- Does NOT affect other items' change tracking

## Example: Mixed Stateful Items

```javascript
screen customerForm = {
    "title": "Customer Form",
    "vars": [
        {"name": "customerName", "type": "string", "default": ""},
        {"name": "email", "type": "string", "default": ""},
        {"name": "searchFilter", "type": "string", "default": ""},
        {"name": "previewTheme", "type": "string", "default": "light"}
    ],
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "items": [
                {
                    "name": "nameField",
                    "varref": "customerName",
                    "sequence": 1
                    // stateful: true (default) - marks screen as changed
                },
                {
                    "name": "emailField",
                    "varref": "email",
                    "sequence": 2
                    // stateful: true (default) - marks screen as changed
                },
                {
                    "name": "searchBox",
                    "varref": "searchFilter",
                    "sequence": 3,
                    "stateful": false
                    // stateful: false - does NOT mark screen as changed
                },
                {
                    "name": "themeSelector",
                    "varref": "previewTheme",
                    "sequence": 4,
                    "stateful": false,
                    "display": {
                        "type": "combobox",
                        "options": ["light", "dark"]
                    }
                    // stateful: false - theme preview doesn't make form dirty
                }
            ]
        }
    ]
};
```

**Behavior:**
- Typing in `customerName` or `email` → Screen becomes CHANGED
- Typing in `searchFilter` → Screen stays CLEAN
- Changing `previewTheme` → Screen stays CLEAN

## Screen Status Values

The screen can be in one of three states:
- `CLEAN`: No changes made
- `CHANGED`: User has made changes (at least one stateful item modified)
- `ERROR`: Screen has encountered a validation error

## Checking Screen Status

Use `scr.getStatus()` to check the current screen status:

```javascript
var status = scr.getStatus("myScreen");
println("Screen status: " + status);  // "clean", "changed", or "error"
```

## Debug Panel

When debug mode is enabled (Ctrl+D), the debug panel shows:
- Screen status at the top
- Changed items marked with an asterisk (*)
- Non-stateful items do NOT show the asterisk when modified

## Implementation Notes

- Property is stored on the `Var` object for each screen variable
- Checked by `ControlListenerFactory.markScreenChanged()` before updating status
- Only affects change tracking; all other functionality remains normal
- Thread-safe: Works correctly with screen event dispatcher threads

## New Builtin Functions

### scr.setVarStateful(screenName, varName, stateful)
Sets whether changes to a variable should mark the screen as changed.

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable
- `stateful` (Boolean): `true` to mark screen as changed, `false` to ignore changes

**Returns:** Boolean - `true` on success

**Example:**
```javascript
// Make the filter variable non-stateful
call scr.setVarStateful("myScreen", "filter", false);
```

### scr.getVarStateful(screenName, varName)
Gets whether changes to a variable mark the screen as changed.

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Boolean - `true` if stateful, `false` otherwise

**Example:**
```javascript
var isStateful = call scr.getVarStateful("myScreen", "filter");
println("Filter is stateful: " + isStateful);
```

### scr.getVarOriginalValue(screenName, varName)
Gets the original value of a variable (before any modifications).

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Any - The original value of the variable

**Example:**
```javascript
var original = call scr.getVarOriginalValue("myScreen", "customerName");
println("Original name: " + original);
```

### scr.submitVarItem(screenName, varName)
Marks the variable as submitted by setting its original value to the current value.
Use this after successfully saving changes to the database.

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Boolean - `true` on success

**Example:**
```javascript
// After saving to database
call scr.submitVarItem("myScreen", "customerName");
```

### scr.resetVarItem(screenName, varName)
Resets the variable to its original value (undoes any changes).

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Boolean - `true` on success

**Example:**
```javascript
// User clicks "Cancel" button
call scr.resetVarItem("myScreen", "customerName");
```

### scr.clearVarItem(screenName, varName)
Clears the variable to its default/empty value.

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Boolean - `true` on success

**Example:**
```javascript
// Clear the search field
call scr.clearVarItem("myScreen", "searchText");
```

### scr.initVarItem(screenName, varName)
Initializes the variable to its default value and sets it as the original value.
This resets the variable to a clean state with its default value.

**Parameters:**
- `screenName` (String): Name of the screen
- `varName` (String): Name of the variable

**Returns:** Boolean - `true` on success

**Example:**
```javascript
// Reset to clean state with default value
call scr.initVarItem("myScreen", "customerName");
```

## Test Script

A complete test script is available at:
`ScriptInterpreter/scripts/test/test_stateful_property.ebs`

Run it to see the feature in action:
```bash
cd ScriptInterpreter
mvn javafx:run
# In console: import "scripts/test/test_stateful_property.ebs";
```

## Related Features

- **Screen Status**: `scr.getStatus()`, `scr.setStatus()`
- **Screen Changes**: `scr.checkChanged()`, `scr.revert()`, `scr.clear()`
- **Debug Panel**: Toggle with Ctrl+D to see changed items
- **Variable Management**: New builtin functions for managing variable state

## Breaking Changes

### Removed Features
The following builtins have been **removed** as they operated on a deprecated `source` property:
- `scr.getItemSource()` - No longer available
- `scr.setItemSource()` - No longer available

The `source` property has been removed from `AreaItem` and `DisplayItem` classes as it was replaced by the `stateful` property.

## Version

Feature updated in version 1.0.2.3 (December 2025)
- Property moved from item level to variable level
- Added 7 new builtin functions for variable management
- Removed deprecated source property and related builtins
