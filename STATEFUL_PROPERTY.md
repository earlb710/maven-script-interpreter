# Stateful Property for Area Items

## Overview

The `stateful` property is a Boolean flag that can be set on area items in screen definitions. It controls whether changes to that item mark the screen as "changed" (dirty state).

## Syntax

```javascript
screen myScreen = {
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "items": [
                {
                    "name": "dataField",
                    "varref": "myData",
                    "stateful": true  // Changes mark screen as CHANGED (default)
                },
                {
                    "name": "searchFilter",
                    "varref": "filter",
                    "stateful": false  // Changes do NOT mark screen as CHANGED
                }
            ]
        }
    ]
};
```

## Default Behavior

- **Default value**: `true`
- **Backward compatible**: All existing screens continue to work without changes
- When `stateful` is not specified, items behave as they always have (changes mark screen as CHANGED)

## When to Use `stateful: false`

Use `stateful: false` for items that represent:

### 1. Search and Filter Fields
```javascript
{
    "name": "searchBox",
    "varref": "searchText",
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
    "name": "colorPreview",
    "varref": "previewColor",
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
    "name": "tempSlider",
    "varref": "tempValue",
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
    "name": "sortOrder",
    "varref": "sortBy",
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

- Property is stored on the JavaFX control's properties map
- Checked by `ControlListenerFactory.markScreenChanged()` before updating status
- Only affects change tracking; all other functionality remains normal
- Thread-safe: Works correctly with screen event dispatcher threads

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
- **Item Properties**: `editable`, `disabled`, `visible`, etc.

## Version

Feature added in version 1.0.2.2 (December 2025)
