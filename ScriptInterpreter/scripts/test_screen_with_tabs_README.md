# Test Screen with Tabs - README

## Overview

This test script (`test_screen_with_tabs.ebs`) demonstrates a comprehensive JavaFX screen implementation featuring:

1. **Master Client Detail Record** - Displayed above the tabs and always visible
2. **Three Tabs** - Each with specific functionality:
   - Tab 1: Contact Information
   - Tab 2: Account Details
   - Tab 3: Summary (consolidated view)
3. **Multi-Column Layouts** - Using GridPane for organized display
4. **Interactive Buttons** - With onClick handlers executing EBS code
5. **Tooltips of Varying Lengths** - Short, medium, and long tooltips on all fields and buttons

## Running the Script

### Using the EBS Console

1. Start the EBS console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. In the console, open the script:
   - Use File → Open or Ctrl+O
   - Navigate to `scripts/test_screen_with_tabs.ebs`

3. Execute the script:
   - Press Ctrl+Enter to run the script
   - The screen will open automatically

### Using Command Line (Alternative)

```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test_screen_with_tabs.ebs
```

Note: This requires JavaFX to be properly configured in the classpath.

## Screen Structure

### Master Client Record (Top Section)

Always visible above the tabs, contains:

- **Client ID** - Text field with short tooltip
- **Client Name** - Text field with medium-length tooltip
- **Status** - ComboBox with long multi-line tooltip

This section is styled with a light gray background and border to distinguish it from the tabbed content.

### Tab 1: Contact Information

Multi-column GridPane layout containing:

**Column 1 (Left):**
- Contact Name (text field)
- Department (combo box)

**Column 2 (Right):**
- Email (text field) - with long tooltip
- Phone (text field)

**Column 3:**
- Extension (text field)
- Preferred Contact (choice box) - with long tooltip

**Buttons:**
- **Update Contact** - Updates tab1Action variable
- **Clear Fields** - Clears all contact fields (with long warning tooltip)

### Tab 2: Account Details

Multi-column GridPane layout containing:

**Column 1 (Left):**
- Account Type (combo box)
- Contract Term (spinner)
- Priority Level (slider)

**Column 2 (Right):**
- Monthly Budget (text field) - with long tooltip
- Auto-Renewal (checkbox) - with long tooltip

**Full Width:**
- Account Notes (text area spanning 2 columns)

**Buttons:**
- **Save Account** - Saves account details
- **Reset to Defaults** - Resets all fields (with long warning tooltip)

### Tab 3: Summary

Contains:

- **Summary Text Area** - Large text area displaying consolidated information
- **Refresh Summary Button** - Generates a formatted summary including:
  - Master record details
  - Contact information from Tab 1
  - Account details from Tab 2
  - Last action performed in each tab

## Features Demonstrated

### 1. Tooltip Variations

The script includes three types of tooltips:

- **Short tooltips** (5-20 characters): "Short tooltip", "Main phone number"
- **Medium tooltips** (30-80 characters): "This is a medium length tooltip that provides more information..."
- **Long tooltips** (100+ characters): Multi-line tooltips with detailed explanations and warnings

### 2. Multi-Column Layout

Uses GridPane with `layoutPos` property to position items:
- Format: `"row,column"` (e.g., `"0,0"` for row 0, column 0)
- Supports column spanning with `colSpan` property
- Items can span multiple columns for wider controls

### 3. Interactive Buttons

Buttons execute EBS code via the `onClick` property:
- Update variables
- Print messages to console
- Clear or reset form fields
- Generate dynamic summaries

### 4. Data Binding

All form fields are bound to variables:
- Changes in the UI update the variables
- Variable updates from code reflect in the UI
- Summary tab displays current values from all tabs

### 5. Tab Navigation

The TabPane allows switching between different content areas:
- Master record remains visible when switching tabs
- Each tab has independent layout and items
- Tab titles are set via `screenName` property

## Variables

The script defines the following variable categories:

### Master Record Variables
- `clientId` (string)
- `clientName` (string)
- `clientStatus` (string)

### Contact Information Variables (Tab 1)
- `contactName` (string)
- `email` (string)
- `phone` (string)
- `extension` (string)
- `department` (string)
- `preferredContact` (string)
- `tab1Action` (string) - Tracks last action

### Account Details Variables (Tab 2)
- `accountType` (string)
- `monthlyBudget` (double)
- `contractTerm` (int)
- `autoRenewal` (bool)
- `priority` (int)
- `accountNotes` (string)
- `tab2Action` (string) - Tracks last action

### Summary Variables (Tab 3)
- `summaryText` (string)

## Expected Behavior

1. **On Script Start:**
   - Variables are initialized with default values
   - Initial values are printed to console
   - Screen opens with master record visible

2. **User Interactions:**
   - Edit any field in the master record or tabs
   - Hover over fields and buttons to see tooltips
   - Click buttons to trigger actions
   - Switch between tabs to view different sections

3. **Summary Tab:**
   - Initially shows placeholder text
   - Click "Refresh Summary" to generate formatted summary
   - Summary includes all current values from all sections

4. **On Screen Close:**
   - All final values are printed to console
   - Script completes successfully

## Layout Configuration

### GridPane Settings
- Horizontal gap: 20px
- Vertical gap: 15px
- Padding: 20px

### Master Record Styling
- Background: Light gray (#f0f0f0)
- Border: 1px solid gray (#cccccc)
- Padding: 10px

### Field Widths
- Standard fields: 250px
- Narrow fields: 150-200px
- Summary text area: 800px × 400px

## Testing Checklist

When testing this script, verify:

- [ ] Master record is always visible above tabs
- [ ] All three tabs are present and selectable
- [ ] Tooltips appear on hover for all fields and buttons
- [ ] Tooltips display correctly (short, medium, and long)
- [ ] Multi-column layout is properly aligned
- [ ] Buttons execute their onClick handlers
- [ ] Console shows button action messages
- [ ] Summary tab displays consolidated data after refresh
- [ ] All field types work correctly (text, combo, checkbox, slider, etc.)
- [ ] Tab navigation works smoothly
- [ ] Screen can be resized
- [ ] All variables retain their values when switching tabs

## Troubleshooting

### Script Won't Load
- Ensure JavaFX is properly installed
- Check that all JSON syntax is valid
- Verify the script path is correct

### Screen Doesn't Display
- Check console for error messages
- Ensure display is available (not headless environment)
- Verify JavaFX runtime is in classpath

### Tooltips Don't Show
- Ensure `tooltip` property is set on items
- Hover over controls (not labels) for tooltips
- Check that JavaFX version supports tooltips

### Buttons Don't Work
- Check console for error messages
- Verify onClick code syntax is correct
- Ensure referenced variables exist

## Notes

- This is a test/demonstration script showing advanced screen features
- The script is self-contained and doesn't require external dependencies
- All EBS code in onClick handlers runs in the interpreter context
- Variable changes are immediately reflected in the UI due to data binding
- The screen uses standard JavaFX controls and layouts

## Author

EBS Test Suite - 2025-11-18

## Related Scripts

- `test_screen_basic.ebs` - Basic screen functionality
- `test_screen_two_columns.ebs` - Two-column layout example
- `test_button_verified.ebs` - Button onClick functionality
- `test_statusbar_tooltip.ebs` - Status bar and tooltip features
