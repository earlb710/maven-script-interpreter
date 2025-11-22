# N-Column Layout Test Screen

## Description
This test screen demonstrates a flexible n-column layout using GridPane. The example shows 3 columns with 4 rows (12 fields total), but the approach can be adapted for any number of columns.

## Key Concept: Supporting N Columns

### Column Layout Strategy
The n-column layout uses a flexible approach where:
- **Columns are numbered**: 0, 1, 2, ..., n-1
- **Items fill columns top-to-bottom**: All items in column 0, then column 1, then column 2, etc.
- **Sequence numbers control tab order**: Items are numbered sequentially to follow column-by-column navigation

### Example Configurations

#### 2 Columns (like test_screen_two_columns.ebs)
```
10 items ÷ 2 columns = 5 rows per column
Column 0: Items 1-5 (layoutPos: "0,0" through "4,0")
Column 1: Items 6-10 (layoutPos: "0,1" through "4,1")
Window width: 800px
```

#### 3 Columns (this example)
```
12 items ÷ 3 columns = 4 rows per column
Column 0: Items 1-4 (layoutPos: "0,0" through "3,0")
Column 1: Items 5-8 (layoutPos: "0,1" through "3,1")
Column 2: Items 9-12 (layoutPos: "0,2" through "3,2")
Window width: 1200px
```

#### 4 Columns
```
12 items ÷ 4 columns = 3 rows per column
Column 0: Items 1-3 (layoutPos: "0,0" through "2,0")
Column 1: Items 4-6 (layoutPos: "0,1" through "2,1")
Column 2: Items 7-9 (layoutPos: "0,2" through "2,2")
Column 3: Items 10-12 (layoutPos: "0,3" through "2,3")
Window width: 1600px
```

## Layout Structure (3 Columns Example)

```
┌─────────────────────────────────────────────────────────────────────┐
│  N-Column Layout Test (3 Columns)                      [ _ □ X ]    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  COLUMN 0           COLUMN 1           COLUMN 2                      │
│  ════════           ════════           ════════                      │
│                                                                       │
│  1. Field 1         5. Field 5         9. Field 9                    │
│  2. Field 2         6. Field 6         10. Field 10                  │
│  3. Field 3         7. Field 7         11. Field 11                  │
│  4. Field 4         8. Field 8         12. Field 12                  │
│                                                                       │
│  Tab Order: 1→2→3→4→5→6→7→8→9→10→11→12                              │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Navigation Order (Tab Sequence)

The tab order follows columns top-to-bottom:

```
Column 0:  1 → 2 → 3 → 4
              ↓
Column 1:  5 → 6 → 7 → 8
              ↓
Column 2:  9 → 10 → 11 → 12
```

## Screen Properties

- **Title**: N-Column Layout Test (3 Columns)
- **Size**: 1200 x 600 pixels
- **Layout**: GridPane with 20px horizontal gap, 15px vertical gap, 30px padding
- **Columns**: 3
- **Rows per column**: 4
- **Total fields**: 12

## How to Create N-Column Layouts

### Step 1: Calculate Layout
```
totalItems = number of fields you need
numColumns = desired number of columns
rowsPerColumn = ceil(totalItems / numColumns)
windowWidth = (numColumns * 300) + ((numColumns - 1) * 20) + 60
```

### Step 2: Assign layoutPos
For each item:
```
itemNumber = 1, 2, 3, ..., totalItems
column = floor((itemNumber - 1) / rowsPerColumn)
row = (itemNumber - 1) mod rowsPerColumn
layoutPos = "row,column"
```

### Step 3: Set Sequence
```
sequence = itemNumber (1-based, sequential)
```

## Example Calculation for 3 Columns

Given: 12 items, 3 columns desired

```
rowsPerColumn = ceil(12 / 3) = 4 rows
windowWidth = (3 * 300) + (2 * 20) + 60 = 1000px (use 1200px for comfort)

Item 1:  column = floor(0/4) = 0, row = 0 mod 4 = 0 → layoutPos "0,0"
Item 2:  column = floor(1/4) = 0, row = 1 mod 4 = 1 → layoutPos "1,0"
Item 3:  column = floor(2/4) = 0, row = 2 mod 4 = 2 → layoutPos "2,0"
Item 4:  column = floor(3/4) = 0, row = 3 mod 4 = 3 → layoutPos "3,0"
Item 5:  column = floor(4/4) = 1, row = 4 mod 4 = 0 → layoutPos "0,1"
Item 6:  column = floor(5/4) = 1, row = 5 mod 4 = 1 → layoutPos "1,1"
...and so on
```

## How to Run

1. Start the EBS Console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. In the console, load and execute the script:
   ```
   /open scripts/test_screen_n_columns.ebs
   [Ctrl+Enter]
   ```

3. The screen will be created and displayed automatically.

## Testing the Navigation

1. Click on "Field 1"
2. Press Tab repeatedly
3. Observe that focus moves column by column:
   - Field 1 → 2 → 3 → 4 (down column 0)
   - Field 5 → 6 → 7 → 8 (down column 1)
   - Field 9 → 10 → 11 → 12 (down column 2)

## Adapting for Different Column Counts

### For 4 Columns
- Change window width to 1600px
- Adjust items to have 3 rows per column (12 items / 4 columns)
- Update layoutPos accordingly:
  - Column 0: "0,0", "1,0", "2,0"
  - Column 1: "0,1", "1,1", "2,1"
  - Column 2: "0,2", "1,2", "2,2"
  - Column 3: "0,3", "1,3", "2,3"

### For 5 Columns
- Change window width to 2000px
- Adjust items to have 2-3 rows per column (12 items / 5 columns ≈ 2-3 rows)
- Use ceiling division for uneven distribution

## Key Features Demonstrated

1. **Flexible n-column layout** using GridPane
2. **Proper tab navigation** with sequence numbers
3. **Column-by-column navigation** (top-to-bottom in each column)
4. **Scalable approach** for any number of columns
5. **Data binding** between screen variables and controls
6. **Consistent sizing** with prefWidth for all controls

## Comparison with Two-Column Test

| Feature | test_screen_two_columns.ebs | test_screen_n_columns.ebs |
|---------|----------------------------|---------------------------|
| Columns | Fixed at 2 | Configurable (example: 3) |
| Items | 10 fields | 12 fields |
| Width | 800px | 1200px |
| Control Types | Mixed (TextField, Spinner, ComboBox, ChoiceBox) | All TextFields (for simplicity) |
| Purpose | Demonstrate two-column layout with variety | Demonstrate scalable n-column approach |

## Implementation Notes

### Why Column-by-Column Navigation?

The sequence numbers are assigned sequentially (1, 2, 3, ...) following the visual column order. This ensures:
- Natural reading flow (top-to-bottom within columns, left-to-right across columns)
- No confusing zigzag patterns
- Consistent with user expectations

### Window Width Calculation

```
windowWidth = (numColumns × controlWidth) + ((numColumns - 1) × hgap) + (2 × padding)
            = (numColumns × 300) + ((numColumns - 1) × 20) + 60
```

For 3 columns: (3 × 300) + (2 × 20) + 60 = 1000px (we use 1200px for margins)

### layoutPos Format

GridPane uses `"row,column"` format:
- First number is the row (0-indexed)
- Second number is the column (0-indexed)
- Example: `"2,1"` means row 2, column 1

## Advanced: Dynamic N-Column Generation

While this example shows a static 3-column layout, the EBS language could potentially support dynamic column calculation. Here's a conceptual approach:

```
numColumns = 3
totalItems = 12
rowsPerColumn = ceil(totalItems / numColumns)

for i = 1 to totalItems {
    col = floor((i - 1) / rowsPerColumn)
    row = (i - 1) mod rowsPerColumn
    layoutPos = row + "," + col
    // Create item with calculated layoutPos
}
```

Note: Current EBS script syntax may not support this level of dynamic generation. The example script uses explicit definitions.

## Summary

This test demonstrates how to create flexible n-column layouts in EBS screens using GridPane. By understanding the layoutPos and sequence patterns, you can easily adapt the approach for 2, 3, 4, or more columns as needed for your application.
