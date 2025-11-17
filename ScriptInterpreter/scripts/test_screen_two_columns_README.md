# Two Column Layout Test Screen

## Description
This test screen demonstrates a two-column layout using GridPane with proper tab navigation order. Items are arranged in two columns, with navigation flowing top-to-bottom in the left column first, then top-to-bottom in the right column.

## Layout Structure

The screen uses a GridPane layout with 2 columns and 5 rows:

```
┌─────────────────────────────────┬─────────────────────────────────┐
│  LEFT COLUMN (Column 0)         │  RIGHT COLUMN (Column 1)        │
├─────────────────────────────────┼─────────────────────────────────┤
│ 1. First Name:    [TextField]   │ 6. Address:       [TextField]   │
│                                  │                                  │
│ 2. Last Name:     [TextField]   │ 7. City:          [TextField]   │
│                                  │                                  │
│ 3. Email:         [TextField]   │ 8. State:         [ComboBox]    │
│                                  │                                  │
│ 4. Phone:         [TextField]   │ 9. Zip Code:      [TextField]   │
│                                  │                                  │
│ 5. Age:           [Spinner]     │ 10. Country:      [ChoiceBox]   │
│                                  │                                  │
└─────────────────────────────────┴─────────────────────────────────┘
```

## Navigation Order (Tab Sequence)

When using the Tab key to navigate between fields, the order is:

1. First Name (row 0, column 0)
2. Last Name (row 1, column 0)
3. Email (row 2, column 0)
4. Phone (row 3, column 0)
5. Age (row 4, column 0)
6. Address (row 0, column 1)
7. City (row 1, column 1)
8. State (row 2, column 1)
9. Zip Code (row 3, column 1)
10. Country (row 4, column 1)

This ensures a natural reading order: **top-to-bottom in the left column first, then top-to-bottom in the right column**.

## Screen Properties

- **Title**: Two Column Layout Test
- **Size**: 800 x 600 pixels
- **Layout**: GridPane with 20px horizontal gap, 15px vertical gap, 30px padding

## Control Types Used

### Left Column
- **TextFields**: First Name, Last Name, Email, Phone (with prompt text)
- **Spinner**: Age (range: 18-100)

### Right Column
- **TextFields**: Address, City, Zip Code (with prompt text)
- **ComboBox**: State (dropdown with options: California, Texas, New York, Florida, Illinois)
- **ChoiceBox**: Country (dropdown with options: USA, Canada, Mexico, UK, Australia)

## How to Run

1. Start the EBS Console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. In the console, load and execute the script:
   ```
   /open scripts/test_screen_two_columns.ebs
   [Ctrl+Enter]
   ```

3. The screen will be created and displayed automatically.

## Testing the Navigation

1. Click on the "First Name" field
2. Press Tab repeatedly
3. Observe that focus moves in this order:
   - First Name → Last Name → Email → Phone → Age (down the left column)
   - Address → City → State → Zip Code → Country (down the right column)

## Implementation Details

### GridPane Layout
The items are positioned using `layoutPos` in the format `"row,column"`:
- Left column items: `layoutPos: "0,0"`, `"1,0"`, `"2,0"`, `"3,0"`, `"4,0"`
- Right column items: `layoutPos: "0,1"`, `"1,1"`, `"2,1"`, `"3,1"`, `"4,1"`

### Tab Order
The `sequence` property controls tab navigation order:
- Left column: sequence 1-5
- Right column: sequence 6-10

This ensures the tab order follows the visual layout: left column top-to-bottom, then right column top-to-bottom.

## Screen Fits Completely

The screen is designed to fit all items on screen without scrolling:
- Window size: 800x600 pixels
- Content area (with padding): approximately 740x540 pixels
- 5 rows with 15px vertical gap + 30px padding: 195px for gaps and padding
- Remaining space for controls: ~345px (adequate for 5 rows of controls)
- Each control has a prefWidth of 300px, fitting comfortably in columns

## Key Features Demonstrated

1. **Two-column layout** using GridPane
2. **Proper tab navigation** with sequence numbers
3. **Mixed control types** (TextField, Spinner, ComboBox, ChoiceBox)
4. **Label text alignment** (all left-aligned)
5. **Prompt text** for user guidance
6. **Data binding** between screen variables and controls
7. **Consistent sizing** with prefWidth for all controls
8. **All content visible** without scrolling (fits on screen)
