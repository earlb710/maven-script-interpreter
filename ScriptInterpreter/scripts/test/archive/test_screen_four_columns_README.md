# Four Column Test Screen - Item Listeners

## Overview

This test screen demonstrates a **4-column GridPane layout** with **item listeners** that show how changes in some input fields can reflect on other fields through button-triggered updates.

## File Location

`ScriptInterpreter/scripts/test_screen_four_columns.ebs`

## Features

### Layout
- **4 columns × 3 rows** GridPane
- **Window size**: 1600 × 700 pixels
- **12 input fields** plus 5 action buttons
- **Navigation**: Top-to-bottom within each column (left to right)

### Item Listener Demonstrations

The screen demonstrates four types of listener behaviors:

1. **Name Calculation**
   - Input fields: `firstName`, `lastName`
   - Output field: `fullName`
   - Trigger: "Update Full Name" button
   - Behavior: Combines first and last name into full name

2. **Numeric Calculation**
   - Input fields: `quantity`, `price`
   - Output field: `total`
   - Trigger: "Calculate Total" button
   - Behavior: Multiplies quantity × price to get total

3. **Category Mirroring**
   - Input field: `category` (ComboBox)
   - Output field: `categoryMirror` (TextField)
   - Trigger: "Sync Category" button
   - Behavior: Copies selected category to mirror field

4. **Status Mirroring**
   - Input field: `status` (ComboBox)
   - Output field: `statusMirror` (TextField)
   - Trigger: "Sync Status" button
   - Behavior: Copies selected status to mirror field

## Screen Layout

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Four Column Layout Test - Item Listeners                    [ _ □ X ]     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   COLUMN 0       COLUMN 1       COLUMN 2       COLUMN 3                    │
│   ────────       ────────       ────────       ────────                    │
│   First Name     Quantity       Category       Status                      │
│   Last Name      Price          Priority       Notes                       │
│   Full Name      Total          Cat Mirror     Status Mirror               │
│                                                                             │
│   [Update Full Name] [Calculate Total] [Sync Cat] [Sync Stat] [Update All]│
└────────────────────────────────────────────────────────────────────────────┘
```

## Field Details

### Column 0 - Name Fields
1. **First Name** (TextField) - Sequence 1, Position 0,0
2. **Last Name** (TextField) - Sequence 2, Position 1,0
3. **Full Name** (TextField) - Sequence 3, Position 2,0 - *Calculated from First + Last*

### Column 1 - Numeric Fields
4. **Quantity** (Spinner 1-100) - Sequence 4, Position 0,1
5. **Price** (Spinner $1-$1000) - Sequence 5, Position 1,1
6. **Total** (TextField) - Sequence 6, Position 2,1 - *Calculated from Quantity × Price*

### Column 2 - Category Fields
7. **Category** (ComboBox) - Sequence 7, Position 0,2
   - Options: Electronics, Books, Clothing, Food, Toys
8. **Priority** (ChoiceBox) - Sequence 8, Position 1,2
   - Options: Low, Medium, High, Urgent
9. **Category Mirror** (TextField) - Sequence 9, Position 2,2 - *Mirrors Category selection*

### Column 3 - Status Fields
10. **Status** (ComboBox) - Sequence 10, Position 0,3
    - Options: Active, Inactive, Pending, Completed
11. **Notes** (TextField) - Sequence 11, Position 1,3
12. **Status Mirror** (TextField) - Sequence 12, Position 2,3 - *Mirrors Status selection*

### Button Area (Bottom)
13. **Update Full Name** (Button) - Combines firstName + lastName → fullName
14. **Calculate Total** (Button) - Multiplies quantity × price → total
15. **Sync Category** (Button) - Copies category → categoryMirror
16. **Sync Status** (Button) - Copies status → statusMirror
17. **Update All** (Button) - Updates all calculated/mirrored fields at once

## Navigation Order

Tab key navigation follows this sequence:

```
Column 0: 1 → 2 → 3
    ↓
Column 1: 4 → 5 → 6
    ↓
Column 2: 7 → 8 → 9
    ↓
Column 3: 10 → 11 → 12
    ↓
Buttons: 13 → 14 → 15 → 16 → 17
```

This ensures natural reading order: **top-to-bottom within each column, left-to-right across columns**.

## How to Run

### Start the Application
```bash
cd ScriptInterpreter
mvn javafx:run
```

### Load and Execute the Script
In the console:
```
/open scripts/test_screen_four_columns.ebs
```
Then press `Ctrl+Enter` to execute.

## Testing the Item Listeners

### Test 1: Name Calculation
1. Type a value in **First Name** field (e.g., "John")
2. Type a value in **Last Name** field (e.g., "Doe")
3. Click **Update Full Name** button
4. **Observe**: Full Name field updates to "John Doe"

### Test 2: Numeric Calculation
1. Change **Quantity** spinner (e.g., to 5)
2. Change **Price** spinner (e.g., to $20)
3. Click **Calculate Total** button
4. **Observe**: Total field updates to "$100"

### Test 3: Category Mirroring
1. Select a different **Category** from dropdown (e.g., "Books")
2. Click **Sync Category** button
3. **Observe**: Category Mirror field updates to "Books"

### Test 4: Status Mirroring
1. Select a different **Status** from dropdown (e.g., "Pending")
2. Click **Sync Status** button
3. **Observe**: Status Mirror field updates to "Pending"

### Test 5: Update All
1. Make changes to multiple fields
2. Click **Update All** button
3. **Observe**: All calculated and mirrored fields update simultaneously

### Test 6: Tab Navigation
1. Click in the **First Name** field
2. Press **Tab** key repeatedly
3. **Verify**: Focus moves through fields in sequence 1→2→3→...→17

## Technical Implementation

### Data Binding
- All controls are bound to screen variables using the `varRef` property
- Changes to controls automatically update the bound variables
- Button `onClick` handlers execute EBS code to propagate changes

### Listener Pattern
The onClick handlers demonstrate the listener pattern:
```javascript
"onClick": "fourColumnScreen.fullName = fourColumnScreen.firstName + \" \" + fourColumnScreen.lastName;"
```

This shows how:
1. User changes input fields (firstName, lastName)
2. Button click triggers calculation/copy logic
3. Output field (fullName) reflects the changes
4. JavaFX's built-in data binding updates the UI automatically

### Layout Strategy
- **GridPane** with `layoutPos` format: `"row,column"`
- **Sequence numbers** control tab order
- **prefWidth** set to 300px for consistency
- **Horizontal gap**: 20px between columns
- **Vertical gap**: 15px between rows
- **Padding**: 30px around the grid

## Window Size Calculation

```
Width = (4 columns × 300px) + (3 gaps × 20px) + (2 × 30px padding)
      = 1200 + 60 + 60
      = 1320px → rounded to 1600px for comfort

Height = (3 rows × ~80px) + (2 gaps × 15px) + (2 × 30px padding) + button area
       = 240 + 30 + 60 + 100
       = 430px → expanded to 700px for button area and spacing
```

## Key Concepts Demonstrated

1. **4-Column Layout**: Shows how to distribute fields across 4 columns
2. **Item Listeners**: Demonstrates field-to-field updates via onClick handlers
3. **Mixed Control Types**: TextField, Spinner, ComboBox, ChoiceBox, Button
4. **Calculated Fields**: Shows how to compute derived values (fullName, total)
5. **Mirror Fields**: Shows how to reflect selections (categoryMirror, statusMirror)
6. **Two-Way Binding**: Built-in JavaFX binding keeps UI and variables in sync
7. **Tab Navigation**: Proper sequence ensures intuitive navigation flow

## Related Files

- **Script**: `ScriptInterpreter/scripts/test_screen_four_columns.ebs`
- **Visualization**: `ScriptInterpreter/scripts/visualize_four_columns.py`
- **Documentation**: This README

## Comparison with Other Test Screens

| Feature | 2-Column | 3-Column | 4-Column (This) |
|---------|----------|----------|-----------------|
| Window Width | 800px | 1200px | 1600px |
| Fields | 10 | 12 | 12 |
| Columns | 2 | 3 | 4 |
| Rows per Column | 5 | 4 | 3 |
| Item Listeners | No | No | **Yes** ✓ |
| Action Buttons | No | No | **Yes** ✓ |
| Calculated Fields | No | No | **Yes** ✓ |
| Mirror Fields | No | No | **Yes** ✓ |

## Success Criteria

✅ **4-column layout created** - GridPane with 4 columns  
✅ **12 fields properly positioned** - 3 rows × 4 columns  
✅ **Item listeners implemented** - onClick handlers update related fields  
✅ **Navigation flows correctly** - Top-to-bottom in each column  
✅ **Mixed control types** - TextField, Spinner, ComboBox, ChoiceBox, Button  
✅ **Calculated fields work** - Full Name and Total compute from inputs  
✅ **Mirror fields work** - Category and Status selections reflected  
✅ **Documentation complete** - README and visualization provided  

## Notes

- The item listener pattern uses **button-triggered updates** rather than automatic real-time updates
- This approach is more explicit and easier to understand/debug
- For automatic real-time updates, you would need to implement custom change listeners in Java code
- The current approach is suitable for form-based UIs where users complete sections before updating

## Author

EBS Test Suite - 2025-11-17
