# Screen Item Source and Status Feature - Quick Reference

## Visual Flow

```
Screen Creation
     |
     v
Variables initialized with default values
     |
     v
originalValue = defaultValue
status = "clean"
source = "data"
     |
     v
User modifies field value
     |
     v
value != originalValue
     |
     v
status becomes "changed"
     |
     v
[Option A] User saves changes
     |
     v
Call screen.resetItemOriginalValue()
     |
     v
originalValue = current value
status = "clean"

[Option B] User reverts changes
     |
     v
value = originalValue
     |
     v
status automatically becomes "clean"
```

## Property Behavior

### Source Property
```
┌─────────────────────────────────┐
│   source = "data"               │
│   ↓                             │
│   Use raw variable value        │
│   Example: "1990-01-15"         │
└─────────────────────────────────┘
              OR
┌─────────────────────────────────┐
│   source = "display"            │
│   ↓                             │
│   Use formatted value           │
│   Example: "January 15, 1990"   │
└─────────────────────────────────┘
```

### Status Property
```
┌─────────────────────────────────┐
│   value == originalValue        │
│   ↓                             │
│   status = "clean"              │
│   (No changes)                  │
└─────────────────────────────────┘
              OR
┌─────────────────────────────────┐
│   value != originalValue        │
│   ↓                             │
│   status = "changed"            │
│   (Modified)                    │
└─────────────────────────────────┘
```

## Quick Function Reference

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `screen.getItemSource` | screenName, itemName | String | Get source: "data" or "display" |
| `screen.setItemSource` | screenName, itemName, source | Boolean | Set source property |
| `screen.getItemStatus` | screenName, itemName | String | Get status: "clean" or "changed" |
| `screen.resetItemOriginalValue` | screenName, itemName | Boolean | Reset to clean state |
| `screen.checkChanged` | screenName | Boolean | Check if any item changed |
| `screen.checkError` | screenName | Boolean | Check if screen has error |
| `screen.revert` | screenName | Boolean | Revert all items to original |
| `screen.clear` | screenName | Boolean | Clear all items to defaults |

## Common Patterns

### 1. Check if form is dirty (NEW - using screen.checkChanged)
```ebs
// Simpler approach using new function
var isDirty = call screen.checkChanged("myForm");
if isDirty then
    print "Form has unsaved changes";
```

### 2. Check if form is dirty (original approach)
```ebs
isDirty = false;
var items = call screen.getItemList("myForm");
var i = 0;
while i < items.length {
    var status = call screen.getItemStatus("myForm", items[i]);
    if status = "changed" then
        isDirty = true;
    i = i + 1;
}
```

### 3. Revert all changes (NEW - using screen.revert)
```ebs
// User clicks Cancel button
call screen.revert("myForm");
print "All changes reverted to original values";
```

### 4. Clear form to defaults (NEW - using screen.clear)
```ebs
// User clicks Clear/Reset button
call screen.clear("myForm");
print "All fields cleared to default values";
```

### 5. Reset all fields to clean
```ebs
var items = call screen.getItemList("myForm");
var i = 0;
while i < items.length {
    call screen.resetItemOriginalValue("myForm", items[i]);
    i = i + 1;
}
```

### 6. Get list of changed fields
```ebs
changedFields = [];
var items = call screen.getItemList("myForm");
var i = 0;
while i < items.length {
    var status = call screen.getItemStatus("myForm", items[i]);
    if status = "changed" then
        call array.push(changedFields, items[i]);
    i = i + 1;
}
```

### 7. Conditional save with error checking (NEW)
```ebs
saveForm(formName) {
    // Check for errors first
    var hasError = call screen.checkError(formName);
    if hasError then
        print "Cannot save: form has errors";
        return false;
    
    // Check if there are changes to save
    var hasChanges = call screen.checkChanged(formName);
    if hasChanges then
        // Perform save operation
        print "Saving changes...";
        // Mark all fields as saved
        var items = call screen.getItemList(formName);
        var i = 0;
        while i < items.length {
            call screen.resetItemOriginalValue(formName, items[i]);
            i = i + 1;
        }
        return true;
    else
        print "No changes to save";
        return true;
}
```

### 8. Conditional save based on changes (original)
```ebs
saveForm(formName) {
    var hasChanges = false;
    var items = call screen.getItemList(formName);
    
    var i = 0;
    while i < items.length {
        var status = call screen.getItemStatus(formName, items[i]);
        if status = "changed" then
            hasChanges = true;
        i = i + 1;
    }
    
    if hasChanges then
        // Perform save
        print "Saving changes...";
        // After save, reset all to clean
        i = 0;
        while i < items.length {
            call screen.resetItemOriginalValue(formName, items[i]);
            i = i + 1;
        }
    else
        print "No changes to save";
}
```

## State Diagram

```
                Initial State
                ┌──────────┐
                │  CLEAN   │
                │ value=5  │
                │ orig=5   │
                └────┬─────┘
                     │
         User changes value to 10
                     │
                     ▼
                ┌──────────┐
                │ CHANGED  │
                │ value=10 │
                │ orig=5   │
                └─┬────┬───┘
                  │    │
    User reverts  │    │  User saves
    value to 5    │    │  & resets
                  │    │
                  ▼    ▼
                ┌──────────┐
                │  CLEAN   │
                │ value=?  │
                │ orig=?   │
                └──────────┘
      Left: value=5, orig=5
      Right: value=10, orig=10
```

## Integration with Screen Status

This feature works alongside the existing screen-level status:

```
Screen Level:  screen.getStatus("myScreen")
               Returns: "clean", "changed", or "error"
               Applies to entire screen

Item Level:    screen.getItemStatus("myScreen", "field1")
               Returns: "clean" or "changed"
               Applies to individual field
```

Use both for complete tracking:
- **Screen status**: Overall screen state
- **Item status**: Individual field changes

## File Locations

- **Code**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/`
  - `DisplayItem.java` - Display item properties
  - `AreaItem.java` - Area item properties
  - `Var.java` - Variable with original value tracking

- **Builtins**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/Builtins.java`
  - Lines ~2450-2650: Implementation of 4 new functions

- **Schema**: `ScriptInterpreter/src/main/resources/json/display-metadata.json`
  - Added source and status properties

- **Tests**: `ScriptInterpreter/scripts/`
  - `test_screen_source_status.ebs`
  - `test_screen_source_status_nogui.ebs`

- **Documentation**: `SCREEN_ITEM_SOURCE_STATUS.md`

## Tips

1. **Always reset after save**: Call `resetItemOriginalValue` after successfully saving to mark fields clean
2. **Check before close**: Prompt user if any field has status="changed"
3. **Batch operations**: Process all items in a loop for form-level operations
4. **Source switching**: Use source property for data entry vs display modes
5. **Validation combo**: Combine status tracking with validation for complete form handling
