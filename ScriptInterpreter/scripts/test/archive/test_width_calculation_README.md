# Width Calculation Test Script

## Purpose
This test script (`test_width_calculation.ebs`) demonstrates and verifies the width calculation functionality for input controls in the EBS screen system.

## Controls Tested

### 1. DatePicker
- **Auto-width**: Automatically sized based on date format (typically 15 characters)
- **MaxLength override**: When maxLength is specified, that width is used instead

### 2. ColorPicker
- **Auto-width**: Automatically sized with a compact default (12 characters)
- **MaxLength override**: When maxLength is specified, that width is used instead

### 3. ComboBox
- **Short options (data-driven)**: Width automatically calculated based on longest option
  - Example: ["XS", "S", "M", "L", "XL", "XXL"] → narrow width
- **Long options (data-driven)**: Width automatically calculated to fit longest option
  - Example: ["Human Resources", "Information Technology", ...] → wide width
- **MaxLength override**: Explicit width specification overrides option-based calculation

### 4. ChoiceBox
- **Short options (data-driven)**: Width automatically calculated based on longest option
  - Example: ["Active", "Inactive", "Pending"] → narrow width
- **Long options (data-driven)**: Width automatically calculated to fit longest option
  - Example: ["United States of America", "United Kingdom", ...] → wide width
- **MaxLength override**: Explicit width specification overrides option-based calculation

## How to Run

### Step 1: Start the Application
```bash
cd ScriptInterpreter
mvn javafx:run
```

### Step 2: Load the Test Script
In the console window, type:
```
/open scripts/test_width_calculation.ebs
```

### Step 3: Execute the Script
Press `Ctrl+Enter` to run the script.

### Step 4: Verify the Results
The screen will display with 10 test controls. Observe:
- DatePicker controls have appropriate width for date display
- ColorPicker controls are compact
- ComboBox with short options (Size) is narrow
- ComboBox with long options (Department) is wide enough to show full text
- ChoiceBox with short options (Status) is narrow
- ChoiceBox with long options (Country) is wide enough to show full text
- Controls with maxLength specified use that width regardless of option data

## Expected Behavior

### Data-Driven Width Calculation
When no explicit `maxLength` is specified:
- **ComboBox** and **ChoiceBox** automatically size to fit the longest option
- Width is calculated using JavaFX text measurement for accuracy
- Font size settings are respected in the calculation
- Extra padding (40px) is added for dropdown arrow and borders

### MaxLength Override
When `maxLength` is specified:
- The explicit width is always used
- Option data is ignored for width calculation
- Allows manual control over field width when needed

### Default Fallback
When no maxLength and no options are available:
- DatePicker: 15 characters (default for date format)
- ColorPicker: 12 characters (compact default)
- ComboBox: 20 characters
- ChoiceBox: 20 characters

## Test Cases

| Control | Label | Width Source | Expected Width |
|---------|-------|--------------|----------------|
| DatePicker | Birth Date | Auto (default) | ~15 chars |
| DatePicker | Appointment Date | maxLength=25 | ~25 chars |
| ColorPicker | Favorite Color | Auto (default) | ~12 chars |
| ColorPicker | Accent Color | maxLength=20 | ~20 chars |
| ComboBox | Size | Data (short options) | Narrow (~6 chars) |
| ComboBox | Department | Data (long options) | Wide (~30+ chars) |
| ComboBox | Priority | maxLength=15 | ~15 chars |
| ChoiceBox | Status | Data (short options) | Narrow (~8 chars) |
| ChoiceBox | Country | Data (long options) | Wide (~30+ chars) |
| ChoiceBox | Role | maxLength=12 | ~12 chars |

## Implementation Details

### Code Changes
The width calculation is implemented in `AreaItemFactory.java`:

1. **applyControlSizeAndFont** method:
   - Added DatePicker and ColorPicker to width application logic
   - Width is calculated via `calculateControlWidth` method

2. **calculateControlWidth** method:
   - For ChoiceBox and ComboBox: checks for options and uses longest option for width
   - Creates JavaFX Text node to measure actual text width with current font
   - Adds appropriate padding for control decorations

3. **guessLengthByType** method:
   - Added ColorPicker case with 12-character default

### Width Calculation Priority
1. **Explicit maxLength** (highest priority)
2. **Option data** (for ComboBox/ChoiceBox)
3. **Type-based default** (fallback)

## Notes

- Font size affects width calculation - larger fonts result in wider controls
- Bold and italic styling may affect rendering but width is based on normal font weight
- The padding includes space for borders, dropdown arrows, and internal margins
- Width calculation happens at control creation time
- Changes to options after control creation do not trigger width recalculation

## Related Files

- **Implementation**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaItemFactory.java`
- **Test Script**: `ScriptInterpreter/scripts/test_width_calculation.ebs`
- **Documentation**: `ScriptInterpreter/scripts/test_width_calculation_README.md` (this file)
