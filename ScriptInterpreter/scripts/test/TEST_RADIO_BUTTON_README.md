# Radio Button Selection Test Scripts

## Overview

These test scripts comprehensively validate radio button functionality in the EBS scripting language. Radio buttons are used for exclusive selection within a group of options, similar to multiple-choice questions where only one option can be selected at a time.

## Test Files

### 1. test_radio_button_selection.ebs
**Purpose:** Comprehensive interactive test with GUI display  
**Description:** Tests all radio button features including:
- Multiple radio button groups for different categories
- Reading and modifying radio button values programmatically
- Type checking with `typeof` operator
- Variable declarations with `screen.radiobutton` type
- Visual display of radio buttons in a JavaFX window

**Use Case:** Manual testing and demonstration of radio button functionality

**How to Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Dexec.args="scripts/test/test_radio_button_selection.ebs"
```

Or using the run script:
```bash
cd ScriptInterpreter
./run.sh scripts/test/test_radio_button_selection.ebs
```

### 2. test_radio_button_selection_nogui.ebs
**Purpose:** Automated test without GUI display  
**Description:** Same comprehensive tests as the GUI version, but without attempting to display the screen. Perfect for:
- Automated testing in CI/CD pipelines
- Headless environments
- Quick validation of radio button logic

**Use Case:** Automated testing and validation

**How to Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_radio_button_selection_nogui.ebs"
```

## Test Coverage

Both test scripts validate the following functionality:

### TEST 1: Initial Radio Button Values
- Verifies that radio buttons are initialized with correct default values
- Tests string-based radio buttons (theme selection)
- Tests boolean-based radio buttons (language, notification preferences)

### TEST 2: Radio Button typeof Check
- Confirms that `typeof` returns "Screen.Radiobutton" for radio button variables
- Works for both string and boolean radio button types

### TEST 3: Variable Declarations with Radio Button Types
- Tests declaring variables with `screen.radiobutton` type annotation
- Validates that values can be assigned and read correctly

### TEST 4: Modifying Radio Button Values
- Tests programmatic changes to radio button selections
- Simulates user selection by setting values to true/false or specific strings
- Verifies that changes are reflected in the screen variables

### TEST 5: Reading All Current Selections
- Tests conditional logic based on radio button selections
- Demonstrates how to determine which option is currently selected
- Shows practical usage patterns for radio button groups

### TEST 6: Testing Multiple Selection Patterns
- Tests setting all options to false, then enabling one
- Validates that radio button state can be fully controlled programmatically
- Ensures proper state management for exclusive selections

## Screen Definition Structure

The test screens demonstrate three different radio button groups:

### Theme Preferences (String-based)
```javascript
{
    "name": "theme",
    "type": "string",
    "default": "light",
    "display": {
        "type": "radiobutton",
        "labelText": "Theme: Light",
        "promptHelp": "Select Light Theme"
    }
}
```

### Language Selection (Boolean-based)
```javascript
{
    "name": "langEnglish",
    "type": "bool",
    "default": true,
    "display": {
        "type": "radiobutton",
        "labelText": "English",
        "promptHelp": "Select English"
    }
}
```

### Notification Preferences (Boolean-based)
```javascript
{
    "name": "notifyAll",
    "type": "bool",
    "default": true,
    "display": {
        "type": "radiobutton",
        "labelText": "All Notifications",
        "promptHelp": "Receive all notifications"
    }
}
```

## Key Features Demonstrated

### 1. Radio Button Types
Radio buttons can be defined with either:
- **String type:** Stores the string value of the selected option
- **Boolean type:** Stores true/false for each option (useful for mutually exclusive groups)

### 2. Component Type System
Radio buttons support the screen component type system:
```javascript
var myRadio : screen.radiobutton = screenName.radioVar;
print typeof screenName.radioVar;  // Prints "Screen.Radiobutton"
```

### 3. Display Properties
Radio buttons support these display properties:
- `labelText`: Text label displayed next to the radio button
- `promptHelp`: Tooltip/hint text shown on hover
- `type`: Must be set to "radiobutton"

### 4. Programmatic Control
Radio button values can be:
- Read: `var value = screenName.radioVar;`
- Modified: `screenName.radioVar = newValue;`
- Tested: `if (screenName.radioVar) { ... }`

## Expected Output

When running the no-GUI test, you should see:

```
===============================================
  Radio Button Selection Test (No GUI)
===============================================

TEST 1: Initial Radio Button Values
------------------------------------
Theme Options:
  theme = light
  themeOption2 = dark
  themeOption3 = auto

Language Options:
  langEnglish = Y
  langSpanish = N
  langFrench = N
  langGerman = N

Notification Options:
  notifyAll = Y
  notifyImportant = N
  notifyNone = N

TEST 2: Radio Button typeof Check
----------------------------------
typeof radioTestScreen.theme = Screen.Radiobutton
typeof radioTestScreen.langEnglish = Screen.Radiobutton
typeof radioTestScreen.notifyAll = Screen.Radiobutton

TEST 3: Variable Declarations with Radio Button Types
------------------------------------------------------
themeRadio (screen.radiobutton) = light
langRadio (screen.radiobutton) = Y
notifyRadio (screen.radiobutton) = Y

TEST 4: Modifying Radio Button Values
--------------------------------------
Changing language selection from English to Spanish...
  langEnglish = N
  langSpanish = Y

Changing notification preference to Important Only...
  notifyAll = N
  notifyImportant = Y

TEST 5: Reading All Current Selections
---------------------------------------
Theme Preferences:
  ✓ Light theme is default

Language Selection:
  ✓ Spanish is selected

Notification Preference:
  ✓ Important notifications only

TEST 6: Testing Multiple Selection Patterns
--------------------------------------------
All language options set to false
French language selected
  langEnglish = N
  langSpanish = N
  langFrench = Y
  langGerman = N

===============================================
  Test Summary
===============================================
✓ Initial values loaded correctly
✓ typeof returns Screen.Radiobutton
✓ Variable declarations with screen.radiobutton work
✓ Radio button values can be modified programmatically
✓ Current selections can be read and evaluated
✓ Multiple selection patterns tested successfully

Radio Button Selection Test COMPLETED!
```

## Important Notes

### Radio Button Grouping
- In EBS, radio buttons in the same "setname" are visually grouped together in the UI
- To create mutually exclusive selections, place related radio buttons in the same set
- Programmatically, you control exclusivity by setting other options to false when enabling one

### Boolean vs String Types
- **Boolean radio buttons:** Each option is a separate variable that can be true/false
  - Better for programmatic control of individual options
  - Easier to determine which option is selected
- **String radio buttons:** Single variable holds the string value of the selected option
  - More compact representation
  - Natural for single-selection scenarios

### Display Properties
- Use `labelText` to provide descriptive text for each radio button
- Use `promptHelp` to provide additional context or instructions
- Radio buttons automatically render with proper spacing and alignment

## Related Documentation

- See `docs/EBS_SCRIPT_SYNTAX.md` for complete EBS syntax reference
- See `SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md` for details on the component type system
- See `test_screen_types_all_components.ebs` for examples of all component types
- See `test_screen_component_types.ebs` for more component type examples

## Use Cases

Radio button selection is ideal for:
- Theme selection (light, dark, auto)
- Language preferences
- Notification settings
- Payment method selection
- Gender selection
- Priority levels
- Any scenario requiring exclusive selection from multiple options

## Testing Strategy

1. **Automated Testing:** Use `test_radio_button_selection_nogui.ebs` in CI/CD pipelines
2. **Manual Testing:** Use `test_radio_button_selection.ebs` for visual verification
3. **Integration Testing:** Test radio buttons within larger forms and workflows
4. **User Acceptance Testing:** Verify radio button behavior matches user expectations

## Troubleshooting

### Issue: Radio buttons don't appear
**Solution:** Ensure the display type is set to "radiobutton" and the screen is properly defined

### Issue: Multiple radio buttons selected
**Solution:** Use boolean type and manually set others to false when selecting one, or use string type with a single variable

### Issue: typeof returns wrong type
**Solution:** Verify the display type is "radiobutton" and the screen has been initialized

### Issue: Cannot modify radio button value
**Solution:** Ensure you're accessing the screen variable correctly: `screenName.varName = value;`

## Conclusion

These test scripts provide comprehensive coverage of radio button functionality in EBS, demonstrating both the technical capabilities and practical usage patterns. Use them as a reference for implementing radio buttons in your own EBS applications.
