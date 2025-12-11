# Screen Component Types Test Scripts

This directory contains test scripts for the screen component type system that enables `typeof` introspection, typed variable declarations, and JavaFX component introspection for screen components.

## Overview

The screen component type system provides:
- **typeof operator**: Returns "Screen.Xxx" format for screen component variables
- **Type declarations**: Variables can be declared with `screen.xxx` types (e.g., `var x : screen.textfield`)
- **JavaFX introspection**: Access detailed component information via `.javafx` property
- **Canvas type**: Uses `screen.canvas` (not `screen.canvasview`) for consistency with canvas datatype

## Test Scripts

### 1. test_screen_types_basic.ebs
**Purpose:** Quick validation of basic functionality  
**Description:** Simple test covering the essential features:
- typeof operator returning "Screen.xxx" format
- Variable declarations with screen.xxx types
- Value assignment and access

**Run:** 
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_types_basic.ebs"
```

**Expected Output:**
```
Basic Screen Component Type Test
=================================

1. Testing typeof operator:
   typeof testForm.username = Screen.Textfield
   typeof testForm.notes = Screen.Textarea

2. Testing variable declarations:
   user = admin
   comments = Enter notes here

3. Testing value assignment:
   testForm.username = newuser

✓ Basic test passed!
```

---

### 2. test_screen_component_types.ebs
**Purpose:** Comprehensive test suite  
**Description:** Tests all major features:
- typeof returns correct format for multiple component types
- Variable declarations with various screen.xxx types
- Value access and modification
- Case insensitivity (SCREEN.TEXTAREA, Screen.TextField, screen.button)

**Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_component_types.ebs"
```

**Expected Output:**
```
=== Screen Component Type System Tests ===

TEST 1: typeof returns Screen.xxx format
----------------------------------------
typeof myScreen.clientText = Screen.Textarea
typeof myScreen.nameField = Screen.Textfield
typeof myScreen.saveButton = Screen.Button
typeof myScreen.statusLabel = Screen.Label
typeof myScreen.passwordInput = Screen.Passwordfield

TEST 2: Variable declarations with screen.xxx types
---------------------------------------------------
clientData (screen.textarea) = Sample text content
userName (screen.textfield) = John Doe
buttonText (screen.button) = Save
status (screen.label) = Ready

TEST 3: Variable value access and modification
----------------------------------------------
Original value: myScreen.nameField = John Doe
Modified value: myScreen.nameField = Jane Smith

TEST 4: Case insensitivity
---------------------------
test1 (SCREEN.TEXTAREA) = Test uppercase
test2 (Screen.TextField) = Test mixed case
test3 (screen.Button) = Test lowercase

=== All tests completed successfully! ===
```

---

### 3. test_screen_types_all_components.ebs
**Purpose:** Coverage test for all component types  
**Description:** Validates typeof for all supported JavaFX components:
- Text inputs: textfield, textarea, passwordfield
- Selection controls: checkbox, radiobutton, togglebutton, combobox, choicebox
- Numeric controls: spinner, slider
- Date/time: datepicker
- Buttons: button
- Display: label, labeltext, text, hyperlink

**Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_types_all_components.ebs"
```

**Expected Output:**
```
All Screen Component Types Test
================================

Text Input Components:
----------------------
textfield:     Screen.Textfield
textarea:      Screen.Textarea
passwordfield: Screen.Passwordfield

Selection Components:
---------------------
checkbox:      Screen.Checkbox
radiobutton:   Screen.Radiobutton
togglebutton:  Screen.Togglebutton
combobox:      Screen.Combobox
choicebox:     Screen.Choicebox

[... more component types ...]

✓ All component types test passed!
```

---

### 4. test_javafx_property_access.ebs (NEW)
**Purpose:** Test JavaFX component introspection  
**Description:** Validates the `.javafx` property accessor for detailed component information:
- Access JavaFX Node properties via `screenName.varName.javafx`
- Returns detailed description including type, size, position, style, visibility, etc.
- Works for all screen component types

**Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_javafx_property_access.ebs"
```

**Expected Output:**
```
JavaFX Component Access Test
=============================

Screen created with controls: username (textfield), comments (textarea), submitBtn (button)

TEST 1: typeof operator
-----------------------
typeof testScreen.username = Screen.Textfield
typeof testScreen.comments = Screen.Textarea
typeof testScreen.submitBtn = Screen.Button

TEST 2: .javafx property access
--------------------------------

testScreen.username.javafx:
JavaFX Component Description:
  Type: TextField
  Component Type: Screen.Textfield
  Width: 200.00
  Height: 25.00
  X: 0.00
  Y: 0.00
  Visible: true
  Managed: true
  Disabled: false

testScreen.comments.javafx:
JavaFX Component Description:
  Type: TextArea
  Component Type: Screen.Textarea
  Width: 400.00
  Height: 150.00
  Visible: true
  Managed: true
  Disabled: false

testScreen.submitBtn.javafx:
JavaFX Component Description:
  Type: Button
  Component Type: Screen.Button
  Width: 80.00
  Height: 30.00
  Visible: true
  Disabled: false

✓ JavaFX property access test completed!
```

---

### 5. test_canvas_screen_type.ebs (NEW)
**Purpose:** Test canvas type as Screen.Canvas  
**Description:** Validates the canvas component type simplification:
- typeof returns "Screen.Canvas" (not "Screen.Canvasview")
- Type declarations with `screen.canvas` work
- Case-insensitive support

**Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_canvas_screen_type.ebs"
```

**Expected Output:**
```
Canvas Component Type Test
==========================

TEST 1: typeof returns Screen.Canvas
-------------------------------------
typeof testCanvas.myCanvas = Screen.Canvas

TEST 2: Variable declaration with screen.canvas type
-----------------------------------------------------
canvasVar declared with type screen.canvas: test value

TEST 3: Case insensitivity
---------------------------
canvas1 (screen.canvas) = value1
canvas2 (Screen.Canvas) = value2
canvas3 (SCREEN.CANVAS) = value3

✓ All canvas type tests passed!
```

---

### 6. screen_component_types_example.ebs (in examples/)
**Purpose:** Practical real-world example  
**Description:** Demonstrates a user registration form with:
- Multiple component types (textfield, passwordfield, textarea, checkbox)
- Type introspection using typeof
- Typed variable declarations
- Form data processing
- Simple validation logic

**Run:**
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/examples/screen_component_types_example.ebs"
```

**Expected Output:**
```
Screen Component Types - Practical Example
==========================================

Form Definition Created
-----------------------

Component Type Inspection:
  firstName: Screen.Textfield
  lastName: Screen.Textfield
  email: Screen.Textfield
  password: Screen.Passwordfield
  bio: Screen.Textarea
  agreeTerms: Screen.Checkbox

[... form processing and validation ...]

✓ Example completed successfully!
```

---

## Features Tested

### typeof Operator
All tests validate that `typeof` returns the correct "Screen.Xxx" format:
```ebs
print typeof myScreen.clientText;  // "Screen.Textarea"
print typeof myCanvas.drawing;     // "Screen.Canvas"
```

### Type Declarations
All tests demonstrate variable declarations with screen component types:
```ebs
var data : screen.textarea = myScreen.clientText;
var name : screen.textfield = myScreen.nameField;
var canvas : screen.canvas = myCanvas.drawing;  // Canvas uses screen.canvas
```

### Canvas Type Simplification
Canvas component type uses `screen.canvas` (not `screen.canvasview`):
```ebs
screen myCanvas = {
    "sets": [{
        "setname": "main",
        "vars": [{
            "name": "drawing",
            "type": "canvas",
            "display": {"type": "canvasview"}  // Display type in JSON
        }]
    }]
};

print typeof myCanvas.drawing;  // Returns "Screen.Canvas"
var c : screen.canvas = myCanvas.drawing;  // Type declaration
```

### JavaFX Component Introspection (NEW)
New test validates accessing detailed JavaFX component information:
```ebs
print myScreen.username.javafx;
// Returns comprehensive component details:
//   - JavaFX class type (TextField, Button, etc.)
//   - Screen component type (Screen.Textfield, etc.)
//   - Size (width, height)
//   - Position (x, y)
//   - Style and CSS classes
//   - Visibility and state flags
```

### Case Insensitivity
Tests confirm that all variations work:
```ebs
var v1 : screen.textarea = ...;    // lowercase
var v2 : Screen.TextArea = ...;    // mixed case
var v3 : SCREEN.TEXTAREA = ...;    // uppercase
```

## Supported Component Types

All JavaFX components are supported:

**Text Inputs:** textfield, textarea, passwordfield  
**Selections:** checkbox, radiobutton, togglebutton, combobox, choicebox, listview, tableview, treeview  
**Numeric:** spinner, slider  
**Date/Time:** datepicker  
**Color:** colorpicker  
**Buttons:** button  
**Display:** label, labeltext, text, hyperlink, separator  
**Media:** imageview, canvasview, mediaview, webview, chart  
**Progress:** progressbar, progressindicator  

## Running All Tests

To run all screen component type tests sequentially:

```bash
cd ScriptInterpreter

# Basic test
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_types_basic.ebs"

# Comprehensive test
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_component_types.ebs"

# All components test
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/test/test_screen_types_all_components.ebs"

# Practical example
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/examples/screen_component_types_example.ebs"
```

## Usage in Your Scripts

### Basic Usage
```ebs
// Define screen with components
screen myForm = {
    "sets": [{
        "setname": "user",
        "vars": [
            {
                "name": "username",
                "type": "string",
                "display": {"type": "textfield"}
            }
        ]
    }]
};

// Check component type
print typeof myForm.username;  // "Screen.Textfield"

// Declare typed variable
var user : screen.textfield = myForm.username;
```

### Advanced Usage
```ebs
// Multiple component types
var field : screen.textfield = myScreen.nameField;
var area : screen.textarea = myScreen.bio;
var pwd : screen.passwordfield = myScreen.password;
var btn : screen.button = myScreen.submitBtn;
var chk : screen.checkbox = myScreen.agreeTerms;
```

## Troubleshooting

If tests fail, check:
1. Screen definition includes "display" property with "type"
2. Variable names match between screen definition and access
3. Screen has "sets" array with "vars" defined
4. Component type is spelled correctly (case-insensitive but must be valid)

## See Also

- `SCREEN_COMPONENT_TYPES_IMPLEMENTATION.md` - Full implementation details
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/DisplayItem.java` - Component types enum
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenComponentType.java` - Type representation
