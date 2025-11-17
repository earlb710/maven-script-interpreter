# N-Column Layout Visual Guide

## Understanding N-Column Layouts

This guide shows how to create screens with any number of columns (2, 3, 4, 5, etc.).

## Visual Examples

### 2 Columns (test_screen_two_columns.ebs)
```
┌──────────────────────────────────────────────────┐
│  Two Column Layout                   [ _ □ X ]   │
├──────────────────────────────────────────────────┤
│                                                   │
│   COLUMN 0          COLUMN 1                     │
│   ────────          ────────                     │
│   1. Field 1        6. Field 6                   │
│   2. Field 2        7. Field 7                   │
│   3. Field 3        8. Field 8                   │
│   4. Field 4        9. Field 9                   │
│   5. Field 5       10. Field 10                  │
│                                                   │
│   Window: 800x600                                │
│   Tab: 1→2→3→4→5→6→7→8→9→10                     │
└──────────────────────────────────────────────────┘
```

### 3 Columns (test_screen_n_columns.ebs)
```
┌────────────────────────────────────────────────────────────────┐
│  Three Column Layout                             [ _ □ X ]     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│   COLUMN 0       COLUMN 1       COLUMN 2                       │
│   ────────       ────────       ────────                       │
│   1. Field 1     5. Field 5     9. Field 9                     │
│   2. Field 2     6. Field 6    10. Field 10                    │
│   3. Field 3     7. Field 7    11. Field 11                    │
│   4. Field 4     8. Field 8    12. Field 12                    │
│                                                                 │
│   Window: 1200x600                                             │
│   Tab: 1→2→3→4→5→6→7→8→9→10→11→12                            │
└────────────────────────────────────────────────────────────────┘
```

### 4 Columns (configurable)
```
┌────────────────────────────────────────────────────────────────────────────┐
│  Four Column Layout                                          [ _ □ X ]     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   COL 0       COL 1       COL 2       COL 3                                │
│   ─────       ─────       ─────       ─────                                │
│   1. Fld 1    4. Fld 4    7. Fld 7   10. Fld 10                            │
│   2. Fld 2    5. Fld 5    8. Fld 8   11. Fld 11                            │
│   3. Fld 3    6. Fld 6    9. Fld 9   12. Fld 12                            │
│                                                                             │
│   Window: 1600x600                                                         │
│   Tab: 1→2→3→4→5→6→7→8→9→10→11→12                                        │
└────────────────────────────────────────────────────────────────────────────┘
```

### 5 Columns (configurable)
```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Five Column Layout                                                    [ _ □ X ]     │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│   COL 0    COL 1    COL 2    COL 3    COL 4                                         │
│   ─────    ─────    ─────    ─────    ─────                                         │
│   1. F1    4. F4    7. F7   10. F10  13. F13                                        │
│   2. F2    5. F5    8. F8   11. F11  14. F14                                        │
│   3. F3    6. F6    9. F9   12. F12  15. F15                                        │
│                                                                                       │
│   Window: 2000x600                                                                   │
│   Tab: 1→2→3→4→5→6→7→8→9→10→11→12→13→14→15                                        │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## Key Patterns

### Navigation Flow
All n-column layouts follow the same pattern:
```
Column 0: Top → Bottom (all items)
    ↓
Column 1: Top → Bottom (all items)
    ↓
Column 2: Top → Bottom (all items)
    ↓
... (continue for all columns)
```

### layoutPos Pattern
For item number `i` (1-indexed), with `n` columns and `r` rows per column:

```
column = floor((i - 1) / r)
row = (i - 1) mod r
layoutPos = "row,column"
```

Example for 3 columns, 4 rows per column:
- Item 1: col=0, row=0 → "0,0"
- Item 2: col=0, row=1 → "1,0"
- Item 3: col=0, row=2 → "2,0"
- Item 4: col=0, row=3 → "3,0"
- Item 5: col=1, row=0 → "0,1"
- Item 6: col=1, row=1 → "1,1"
- ... and so on

### Window Width Calculation
```
windowWidth = (n × 300px) + ((n - 1) × 20px) + 60px
            = (n columns × control width) + (gaps between) + (padding)
```

Examples:
- 2 columns: (2 × 300) + (1 × 20) + 60 = 680px → use 800px
- 3 columns: (3 × 300) + (2 × 20) + 60 = 1000px → use 1200px
- 4 columns: (4 × 300) + (3 × 20) + 60 = 1320px → use 1600px
- 5 columns: (5 × 300) + (4 × 20) + 60 = 1640px → use 2000px

## Quick Reference Table

| Columns | Items | Rows/Col | Window Width | Example File |
|---------|-------|----------|--------------|--------------|
| 2       | 10    | 5        | 800px        | test_screen_two_columns.ebs |
| 3       | 12    | 4        | 1200px       | test_screen_n_columns.ebs |
| 4       | 12    | 3        | 1600px       | (adapt n_columns) |
| 5       | 15    | 3        | 2000px       | (adapt n_columns) |

## Implementation Steps

### 1. Define Variables
Create a `vars` array with all your fields.

### 2. Calculate Layout
```
numColumns = your desired column count
totalItems = number of variables
rowsPerColumn = ceil(totalItems / numColumns)
```

### 3. Create Area Items
For each item (i = 1 to totalItems):
```
column = floor((i - 1) / rowsPerColumn)
row = (i - 1) mod rowsPerColumn
layoutPos = "row,column"
sequence = i
```

### 4. Set Window Size
```
width = (numColumns × 300) + ((numColumns - 1) × 20) + 60 + margin
height = 600 (adjust if needed for row count)
```

## Tips for Success

1. **Keep controls at 300px width** for consistency
2. **Use 20px horizontal gap** for comfortable spacing
3. **Maintain 15px vertical gap** between rows
4. **Add 30px padding** around the grid
5. **Set sequential sequence numbers** for proper tab order
6. **Calculate layoutPos systematically** to avoid placement errors
7. **Test navigation** by pressing Tab to verify order

## Available Test Scripts

1. **test_screen_two_columns.ebs** - Production-ready 2-column example with mixed controls
2. **test_screen_n_columns.ebs** - Flexible 3-column example (easily adaptable)

Modify `test_screen_n_columns.ebs` to create 4-column, 5-column, or any n-column layouts by:
- Adjusting the number of variables
- Recalculating layoutPos values
- Updating window width
- Following the patterns documented in the README
