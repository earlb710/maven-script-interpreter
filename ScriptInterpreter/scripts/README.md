# EBS Screen Test Scripts

This directory contains comprehensive test scripts for the EBS (Earl Bosch Script) language screen feature. These scripts demonstrate various screen controls, variable management, and advanced features.

## Test Script Overview

### 1. test_screen_basic.ebs
**Purpose:** Tests basic screen functionality  
**Features Tested:**
- Basic screen creation with simple variables
- Variable access (reading default values)
- Variable modification
- Screen show command
- Thread-safe variable storage

**Variables:**
- `userName` (string, textfield)
- `counter` (int, textfield)
- `message` (string, textarea)

**Usage:**
```bash
java -cp target/classes com.eb.script.Run scripts/test_screen_basic.ebs
```

---

### 2. test_screen_text_inputs.ebs
**Purpose:** Tests all text-based input controls  
**Features Tested:**
- Single-line text fields
- Multi-line text areas
- Password fields
- Text validation patterns
- Case formatting (upper/lower)
- Mandatory field constraints

**Control Types:**
- `textfield` - Single-line text input
- `textarea` - Multi-line text input
- `passwordfield` - Password input (masked)

**Special Features:**
- Email validation pattern
- Case formatting (upper/lower)
- Mandatory field marking

---

### 3. test_screen_selection.ebs
**Purpose:** Tests selection-based input controls  
**Features Tested:**
- Checkbox controls
- Radio button controls
- Toggle buttons
- Combo boxes
- Choice boxes
- List views
- Boolean and string value handling

**Control Types:**
- `checkbox` - Boolean toggle
- `radiobutton` - Single selection from group
- `togglebutton` - Toggle state button
- `combobox` - Dropdown with search
- `choicebox` - Simple dropdown
- `listview` - Multi-item list selection

---

### 4. test_screen_numeric.ebs
**Purpose:** Tests numeric input controls  
**Features Tested:**
- Spinner controls with min/max bounds
- Slider controls with ranges
- Integer and double value types
- Boundary condition testing

**Control Types:**
- `spinner` - Numeric input with increment/decrement buttons
- `slider` - Visual range selector

**Variables Tested:**
- Age, quantity (spinner)
- Volume, brightness, rating, temperature (slider)
- Percentage, price (textfield for decimals)
- Boundary testing (min/max values)

---

### 5. test_screen_date_color.ebs
**Purpose:** Tests date and color picker controls  
**Features Tested:**
- Date picker controls
- Color picker controls
- Date range selection
- Color theme management
- Hex color format

**Control Types:**
- `datepicker` - Date selection calendar
- `colorpicker` - Color selection dialog

**Use Cases:**
- Birth date selection
- Appointment scheduling
- Date range selection
- Theme color customization
- UI color scheme management

---

### 6. test_screen_display.ebs
**Purpose:** Tests display-only controls  
**Features Tested:**
- Label controls
- Text controls
- Hyperlink controls
- Separator controls
- Custom styling
- Text alignment
- Dynamic content updates

**Control Types:**
- `label` - Static text label
- `labeltext` - Alternative label style
- `text` - Plain text display
- `hyperlink` - Clickable link
- `separator` - Visual divider

**Styling Examples:**
- Color formatting (green for success, red for errors, orange for warnings)
- Font weight and style
- Text alignment
- Custom CSS styles

---

### 7. test_screen_advanced.ebs
**Purpose:** Tests advanced screen features  
**Features Tested:**
- Multiple screen creation
- Cross-screen variable access
- Independent screen threads
- Show/hide functionality
- Variable interaction between screens
- Thread-safe operations

**Screens Created:**
1. **mainPanel** - Main control panel (600x500)
2. **settingsPanel** - Settings configuration (550x450)
3. **dataDisplay** - Data display panel (500x400)

**Advanced Features:**
- Access variables across different screens
- Modify variables in multiple screens simultaneously
- Calculate derived values from screen variables
- Demonstrate thread-safe concurrent access
- Multiple windows running independently

---

### 8. test_screen_comprehensive.ebs
**Purpose:** Comprehensive test combining all features  
**Features Tested:**
- All control types in a single screen
- Complex variable types (JSON, arrays)
- Thread-safe variable access
- Cross-variable calculations
- Complete workflow testing

**Control Types Included:**
- Text inputs (textfield, textarea, passwordfield)
- Selection controls (checkbox, combobox, choicebox)
- Numeric controls (spinner, slider)
- Date and color pickers
- Display controls (label, text, hyperlink)

**Complex Data:**
- JSON preferences object
- Array of tags
- Calculated values
- Profile completion percentage

---

## Running the Tests

### Interactive Console (JavaFX UI)
1. Start the interactive console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. Use the `/open` command to load a script:
   ```
   /open scripts/test_screen_basic.ebs
   ```

3. Press `Ctrl+Enter` to execute the script

### Command-Line Execution
Execute any test script directly:
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test_screen_basic.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_text_inputs.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_selection.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_numeric.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_date_color.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_display.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_advanced.ebs
java -cp target/classes com.eb.script.Run scripts/test_screen_comprehensive.ebs
```

## Screen Control Types Reference

### Input Controls
| Control Type | Data Type | Description | Example Use Case |
|-------------|-----------|-------------|------------------|
| textfield | string | Single-line text input | Username, email, short text |
| textarea | string | Multi-line text input | Description, notes, comments |
| passwordfield | string | Masked text input | Password, PIN |
| checkbox | bool | Boolean toggle | Agree to terms, enable feature |
| radiobutton | string/bool | Single selection | Theme selection, gender |
| togglebutton | bool | Toggle state button | Dark mode, notifications |
| combobox | string | Dropdown with search | Country, category selection |
| choicebox | string | Simple dropdown | Language, status selection |
| listview | string | Multi-item list | Categories, tags, options |
| spinner | int | Numeric stepper | Age, quantity, count |
| slider | int | Range selector | Volume, brightness, rating |
| datepicker | string | Date selection | Birth date, appointment |
| colorpicker | string | Color selection | Theme color, highlight |

### Display Controls
| Control Type | Description | Example Use Case |
|-------------|-------------|------------------|
| label | Static text label | Titles, field labels |
| labeltext | Alternative label | Instructions, info text |
| text | Plain text display | Descriptions, content |
| hyperlink | Clickable link | URLs, documentation links |
| separator | Visual divider | Section separators |

## Screen Syntax

### Basic Screen Definition
```javascript
screen myScreen = {
    "title": "Screen Title",
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "variableName",
            "type": "string",
            "default": "default value",
            "display": {"type": "textfield", "mandatory": true}
        }
    ]
};
```

### Variable Access
```javascript
// Read variable
var value = myScreen.variableName;

// Write variable
myScreen.variableName = "new value";
```

### Screen Commands
```javascript
// Show screen
screen myScreen show;

// Hide screen
screen myScreen hide;
```

## Display Options

### Common Display Properties
- `type` - Control type (textfield, textarea, checkbox, etc.)
- `mandatory` - Mark as required field (true/false)
- `min` - Minimum value for numeric controls
- `max` - Maximum value for numeric controls
- `style` - Custom CSS styling
- `alignment` - Text alignment (left, center, right)
- `pattern` - Regex pattern for validation
- `caseFormat` - Text case formatting (upper, lower, title)

### Styling Example
```javascript
"display": {
    "type": "label",
    "style": "-fx-text-fill: green; -fx-font-weight: bold;"
}
```

## Thread Safety

All screen variables are stored in `ConcurrentHashMap` for thread-safe access:
- Multiple screens run in independent threads
- Variables can be safely accessed and modified from any thread
- No manual synchronization required
- Thread-safe read and write operations

## Best Practices

1. **Always test with different data types** - Test string, int, double, bool, and JSON types
2. **Test boundary conditions** - Verify min/max constraints work correctly
3. **Verify mandatory fields** - Ensure mandatory validation works as expected
4. **Test cross-screen operations** - Verify multiple screens work independently
5. **Test variable modifications** - Ensure thread-safe access works correctly
6. **Use descriptive variable names** - Makes scripts more maintainable
7. **Include print statements** - Helps debug and verify behavior
8. **Test show/hide cycles** - Verify screens can be hidden and shown multiple times

## Troubleshooting

### Screen doesn't appear
- Ensure `screen <name> show;` command is executed
- Check that screen definition is valid JSON
- Verify all mandatory fields have default values

### Variable not updating
- Ensure correct syntax: `screenName.variableName = value;`
- Check that variable name matches definition (case-sensitive)
- Verify data type matches variable declaration

### Control not displaying correctly
- Check that control type is supported
- Verify display type matches variable data type
- Review custom styles for CSS errors

## Additional Resources

- [README.md](../../README.md) - Main project documentation
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - System architecture details
- [syntax_ebnf.txt](../src/main/java/com/eb/script/syntax_ebnf.txt) - Complete language syntax

## Contributing

When adding new test scripts:
1. Follow the naming convention: `test_screen_*.ebs`
2. Include comprehensive comments
3. Test all control types relevant to the feature
4. Update this README with the new test description
5. Ensure scripts run without errors

## Test Results

All test scripts should:
- ✓ Compile without syntax errors
- ✓ Execute without runtime errors
- ✓ Display screens correctly
- ✓ Allow variable access and modification
- ✓ Demonstrate thread-safe operations
- ✓ Print meaningful debug output

---

**Last Updated:** 2025-11-15  
**Test Suite Version:** 1.0  
**EBS Language Version:** 1.0-SNAPSHOT
