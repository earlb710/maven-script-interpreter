# TableView Feature Documentation

## Overview

The TableView feature enables screens to display arrays of records in a scrollable table format. This allows users to view multiple records simultaneously, with each record displayed as a row and each field as a column.

## Use Cases

- Displaying database query results
- Showing lists of employees, customers, products, etc.
- Presenting any array of structured data (JSON objects/Maps)
- Viewing multiple records with automatic scrolling

## Feature Details

### Display Type: `tableview`

The `tableview` display type creates a JavaFX TableView control that can display an array of records. Each record should be a JSON object (Map) with fields that correspond to the column definitions.

### Basic Syntax

```javascript
screen myScreen = {
    "title": "My Screen",
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "myData",
            "type": "string",  // The variable type (array binding handled automatically)
            "default": "",
            "display": {
                "type": "tableview",
                "labelText": "Data List:",
                "columns": [
                    {
                        "name": "Column Header",
                        "field": "fieldName",
                        "type": "string",
                        "width": 150,
                        "alignment": "left"
                    }
                    // ... more columns
                ]
            }
        }
    ]
};
```

### Column Definition Properties

Each column in the `columns` array supports the following properties:

| Property | Type | Required | Description | Default |
|----------|------|----------|-------------|---------|
| `name` | string | Yes | The column header text displayed to users | - |
| `field` | string | Yes | The field name in the record (JSON key) to display | - |
| `type` | string | No | Data type: "string", "int", "double", "bool" | "string" |
| `width` | integer | No | Column width in pixels | Auto-sized |
| `alignment` | string | No | Text alignment: "left", "center", "right" | "left" |

### Data Format

The TableView expects an array of records where each record is a JSON object (Map). Example:

```javascript
var employeeData: array[*];
employeeData = [
    {"id": 1, "name": "John Smith", "department": "Engineering", "salary": 85000},
    {"id": 2, "name": "Jane Doe", "department": "Marketing", "salary": 92000},
    {"id": 3, "name": "Bob Johnson", "department": "Sales", "salary": 78000}
];

// Bind to screen variable
myScreen.employees = employeeData;
```

### Column Alignment Examples

- **Left alignment** (default): Use for text fields like names, descriptions
- **Center alignment**: Use for short codes, status indicators
- **Right alignment**: Use for numeric fields like IDs, amounts, quantities

### Complete Example

See `test_screen_tableview.ebs` for a complete working example that demonstrates:
- Creating an array of employee records
- Defining a TableView with 5 columns
- Different column alignments (center, left, right)
- Different column widths
- Binding array data to the table
- Automatic scrollbar when data exceeds visible area

### Features

1. **Automatic Scrolling**: When the number of records exceeds the visible area, a vertical scrollbar automatically appears
2. **Dynamic Updates**: Changing the bound array variable updates the table contents
3. **Column Resizing**: Uses constrained resize policy for automatic column width distribution
4. **Type Safety**: Field values are converted to strings for display
5. **Null Handling**: Null or missing fields display as empty strings

### Technical Details

#### JavaFX Implementation
- Uses `TableView<Map<String, Object>>` internally
- Each row is a Map with field names as keys
- Cell value factories extract field values from the Map
- Columns use `SimpleStringProperty` for data binding

#### Variable Binding
- The TableView binds to a screen variable (e.g., `myScreen.myData`)
- When the variable changes, the table automatically refreshes
- Supports both `List<Map>` and `ArrayDynamic` array types
- Items are cleared and repopulated on each update

### Styling

The TableView uses the CSS class `screen-item-tableview` with default styling:
```css
-fx-border-color: #cccccc; 
-fx-border-width: 1;
```

You can override this with custom styles using the `style` property in the display metadata.

### Limitations

- **Read-Only**: The TableView is currently read-only (no cell editing)
- **String Display**: All field values are converted to strings for display
- **No Selection Handling**: Currently no onClick or selection event handlers
- **No Sorting**: Column sorting is not yet implemented
- **No Filtering**: Data filtering is not yet implemented

### Future Enhancements

Potential future features:
- Editable cells with two-way binding back to the array
- Row selection with onClick event handlers
- Column sorting (ascending/descending)
- Column filtering
- Row styling based on data values
- Cell formatting (number formats, date formats)
- Multi-column sorting
- Row addition/deletion through UI
- Export to CSV functionality

### Related Files

- **DisplayItem.java**: Defines TABLEVIEW ItemType and TableColumn class
- **AreaItemFactory.java**: Creates the TableView control and columns
- **ScreenFactory.java**: Handles data binding between array and TableView
- **InterpreterScreen.java**: Parses column definitions from JSON
- **test_screen_tableview.ebs**: Example script demonstrating the feature

## Migration Guide

If you have existing screens that display lists, you can migrate them to use TableView:

### Before (Using Multiple Fields)
```javascript
"vars": [
    {"name": "employee1Name", "type": "string"},
    {"name": "employee1Dept", "type": "string"},
    {"name": "employee2Name", "type": "string"},
    {"name": "employee2Dept", "type": "string"}
]
```

### After (Using TableView)
```javascript
"vars": [
    {
        "name": "employees",
        "type": "string",
        "display": {
            "type": "tableview",
            "columns": [
                {"name": "Name", "field": "name"},
                {"name": "Department", "field": "department"}
            ]
        }
    }
]

// In script:
myScreen.employees = [
    {"name": "John", "department": "IT"},
    {"name": "Jane", "department": "HR"}
];
```

## Summary

The TableView feature provides a powerful way to display arrays of records in EBS screens, making it easy to present structured data with automatic scrolling, column definitions, and dynamic updates. It's particularly useful for displaying database results, lists of entities, or any collection of structured data.
