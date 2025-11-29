# onChange Feature Implementation Summary

## Overview

The `onChange` event handler property allows EBS script developers to execute inline code whenever a screen control's value changes. This feature enables reactive programming patterns for screen updates, calculated fields, cascading changes, and dynamic UI behavior.

## What Was Added

### 1. Data Model Changes
- Added `onChange` property to `DisplayItem` class (for variable-level change handlers)
- Added `onChange` property to `AreaItem` class (for item-level change handlers)

### 2. Parsing Support
- Updated `InterpreterScreen.parseAreaDefinition()` to parse `onChange` from area item definitions
- Updated `InterpreterScreen.parseDisplayItem()` to parse `onChange` from display definitions (via DisplayItem)
- Supports `onChange`, `on_change`, and `onchange` naming conventions (case-insensitive)

### 3. Change Handler Execution
- Implemented `DisplayChangeHandler.setupChangeHandler()` method that:
  - Executes the EBS code whenever the control's value changes
  - Runs after the variable value has been updated
  - Runs after validation (if `onValidate` is defined)
  - Has access to all screen variables, including the new value
- Uses `ControlListenerFactory.attachValueChangeListener()` for all input control types:
  - TextField, TextArea, PasswordField
  - ComboBox (both editable and non-editable), ChoiceBox
  - CheckBox, RadioButton, ToggleButton
  - Spinner, Slider
  - DatePicker, ColorPicker

### 4. Return Value Handling
- Unlike `onValidate`, the `onChange` handler does not require a return value
- Any return value from the onChange code is ignored
- The handler runs for its side effects (variable updates, function calls, etc.)

### 5. Documentation
- Comprehensive documentation added to `AREA_DEFINITION.md`:
  - Added to AreaItem Event Handlers section
  - Added to DisplayMetadata Event Handler Properties section
- Comprehensive documentation added to `EBS_SCRIPT_SYNTAX.md`:
  - Added Event Handlers section with onClick, onValidate, and onChange
  - Multiple examples showing common use cases

## How to Use

### Variable-Level onChange
Define the change handler in the variable's display metadata:

```javascript
{
    "name": "quantity",
    "type": "int",
    "default": 1,
    "display": {
        "type": "spinner",
        "min": 1,
        "max": 100,
        "onChange": "total = quantity * unitPrice;"
    }
}
```

### Item-Level onChange
Override or add a change handler at the item level:

```javascript
{
    "varRef": "category",
    "sequence": 1,
    "onChange": "call loadSubcategories(category); call updateDisplay();"
}
```

### Execution Rules
1. Change handler code is executed every time the control's value changes
2. The variable value has already been updated when the handler runs
3. The handler has access to all screen variables
4. Item-level `onChange` takes precedence over variable-level `onChange`
5. No return value is required (any return value is ignored)

### Combining with onValidate
When both handlers are defined:
1. Value change triggers the control listener
2. `onValidate` runs first (if defined) and applies visual validation styling
3. `onChange` runs after validation (regardless of validation result)

```javascript
{
    "name": "email",
    "type": "string",
    "display": {
        "type": "textfield",
        "onValidate": "return string.contains(email, '@');",
        "onChange": "call updateEmailPreview(email);"
    }
}
```

## Common Use Cases

### 1. Calculated Fields
```javascript
// Update total when quantity or price changes
"onChange": "total = quantity * unitPrice;"
```

### 2. Cascading Updates
```javascript
// Load cities when country changes
"onChange": "call loadCitiesForCountry(country);"
```

### 3. Enable/Disable Controls
```javascript
// Enable advanced options when checkbox is checked
"onChange": "call screen.setProperty('myScreen.advancedField', 'disabled', not showAdvanced);"
```

### 4. Show/Hide Sections
```javascript
// Show details section when toggle is on
"onChange": "call screen.setProperty('myScreen.detailsArea', 'visible', showDetails);"
```

### 5. Logging/Debugging
```javascript
// Log all changes
"onChange": "println('Field changed to: ' + myField);"
```

### 6. Conditional Styling
```javascript
// Update styling based on value
"onChange": "if (age < 18) { call screen.setProperty('myScreen.ageField', 'textColor', 'red'); } else { call screen.setProperty('myScreen.ageField', 'textColor', 'black'); }"
```

## Files Changed
1. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaItem.java` - Added `onChange` property
2. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/DisplayItem.java` - Added `onChange` property
3. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/display/DisplayChangeHandler.java` - Added `setupChangeHandler()` method
4. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/display/ControlListenerFactory.java` - Added `attachValueChangeListener()` method
5. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenFactory.java` - Added parsing and handler setup
6. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java` - Added parsing support
7. `docs/AREA_DEFINITION.md` - Added comprehensive documentation
8. `docs/EBS_SCRIPT_SYNTAX.md` - Added Event Handlers section

## Testing Status
- ✅ Build successful with no compilation errors
- ✅ Documentation updated
- ⚠️ Manual GUI testing requires JavaFX environment (not available in CI)

## Example Test Script

```javascript
// Test onChange functionality
screen testOnChange = {
    "title": "onChange Test",
    "width": 600,
    "height": 400,
    "vars": [
        {
            "name": "quantity",
            "type": "int",
            "default": 1,
            "display": {
                "type": "spinner",
                "min": 1,
                "max": 100,
                "labelText": "Quantity:",
                "onChange": "println('Quantity changed to: ' + quantity); total = quantity * 10;"
            }
        },
        {
            "name": "total",
            "type": "int",
            "default": 10,
            "display": {
                "type": "label",
                "labelText": "Total:"
            }
        },
        {
            "name": "showDetails",
            "type": "bool",
            "default": false,
            "display": {
                "type": "checkbox",
                "labelText": "Show Details",
                "onChange": "println('Show details: ' + showDetails);"
            }
        }
    ]
};

show screen testOnChange;
```

To test manually:
```bash
cd ScriptInterpreter
mvn javafx:run
# Then run the test script through the UI
```

## Comparison with onValidate

| Feature | onValidate | onChange |
|---------|------------|----------|
| Purpose | Validate input | React to changes |
| Return value | Required (boolean) | Not required |
| Visual feedback | Red border on failure | None |
| Execution order | First | After validation |
| Use case | Input validation | Calculated fields, cascading updates |

## Next Steps for User
1. Test the feature in the JavaFX UI environment
2. Try changing values in different control types
3. Verify that onChange code has access to all screen variables
4. Test item-level vs variable-level onChange precedence
5. Test combination of onValidate and onChange on the same control
