# Screen Item Status Properties

## Overview

**DEPRECATED NOTICE**: The `source` property has been removed from this feature. Only the **status** property remains for tracking automatic change detection for screen variables.

The `stateful` property (which controls whether changes mark the screen as changed) has been **moved from the item level to the variable level**. See `STATEFUL_PROPERTY.md` for details on the updated stateful property.

## Problem Statement (Updated)

For screen items, we need to:
1. ~~Add a `source` property with values "data" or "display"~~ **REMOVED**
2. ~~When `source=="data"`, keep the original data values~~ **REMOVED**
3. Track current values for variables alongside their original values
4. Add a `status` property that is "changed" if current value != original value, else "clean"

## Implementation Details

### Core Classes Modified

#### 1. DisplayItem.java
Added status property:
```java
// Status of the item: "clean" (unchanged) or "changed" (modified from original)
public String status = "clean";
```

**Removed:**
```java
// Source property has been removed as of version 1.0.2.3
```

#### 2. AreaItem.java
**Removed:**
```java
// Source property has been removed as of version 1.0.2.3
```

#### 3. Var.java
Enhanced to track original values, compute status, and control stateful behavior:

**New Fields:**
```java
// Original value (captured when screen is created or explicitly set)
private Object originalValue;

// Whether changes to this variable should mark the screen as changed (default: true)
private Boolean stateful = true;
```

**New Methods:**
```java
// Check if the current value has changed from the original value
public boolean hasChanged()

// Get the status: "changed" if value != original, "clean" otherwise
public String getStatus()

// Reset the original value to the current value (marking as clean)
public void resetOriginalValue()

// Get/set whether changes to this variable mark screen as changed
public Boolean getStateful()
public void setStateful(Boolean stateful)
```

**Constructor Update:**
```java
public Var(String name, DataType type, Object defaultValue) {
    // ...
    this.originalValue = defaultValue;  // Set original value to default initially
}
```

### Builtin Functions

The following builtin functions are **DEPRECATED and REMOVED**:

#### ~~1. screen.getItemSource(screenName, itemName)~~ **REMOVED**
**Removed in version 1.0.2.3** - Use stateful property instead

#### ~~2. screen.setItemSource(screenName, itemName, source)~~ **REMOVED**
**Removed in version 1.0.2.3** - Use `scr.setVarStateful()` instead

#### 3. screen.getItemStatus(screenName, itemName)
**Returns:** String - "clean" or "changed"

Gets the status of a screen item by comparing current value to original value.

**Example:**
```ebs
var status = call screen.getItemStatus("myScreen", "firstName");
if status = "changed" then
    print "First name has been modified";
```

#### 4. screen.resetItemOriginalValue(screenName, itemName)
**Returns:** Boolean - true on success

Resets the original value to the current value, marking the item as "clean".

**Example:**
```ebs
// After saving changes, reset to mark as clean
call screen.resetItemOriginalValue("myScreen", "firstName");
```

#### 5. screen.checkChanged(screenName)
**Returns:** Boolean - true if any item has changed

Checks if any item in the screen has changed from its original value.

**Example:**
```ebs
var hasChanges = call screen.checkChanged("myScreen");
if hasChanges then
    print "Screen has unsaved changes";
```

#### 6. screen.checkError(screenName)
**Returns:** Boolean - true if screen has error status

Checks if the screen has an error status.

**Example:**
```ebs
var hasError = call screen.checkError("myScreen");
if hasError then
    print "Screen has errors";
```

#### 7. screen.revert(screenName)
**Returns:** Boolean - true on success

Reverts all items in the screen to their original values.

**Example:**
```ebs
// User clicks cancel button
call screen.revert("myScreen");
print "All changes reverted";
```

#### 8. screen.clear(screenName)
**Returns:** Boolean - true on success

Clears all items in the screen to their default values (usually blank/null).

**Example:**
```ebs
// User clicks clear/reset button
call screen.clear("myScreen");
print "All fields cleared to defaults";
```

### JSON Schema Updates

Updated `display-metadata.json` to include the new properties:

```json
{
  "source": {
    "type": "string",
    "description": "Source of the value: 'data' (original data value) or 'display' (formatted display value)",
    "enum": ["data", "display"],
    "default": "data"
  },
  "status": {
    "type": "string",
    "description": "Status of the item: 'clean' (unchanged from original) or 'changed' (modified from original)",
    "enum": ["clean", "changed"],
    "default": "clean"
  }
}
```

## Use Cases

### 1. Form Dirty Tracking
Track which fields have been modified before submitting:

```ebs
screen userForm = {
    "vars": [
        {"name": "firstName", "type": "string", "default": "John"},
        {"name": "lastName", "type": "string", "default": "Doe"},
        {"name": "email", "type": "string", "default": "john@example.com"}
    ]
};

// User modifies firstName
userForm.firstName = "Jane";

// Check what changed
var fnStatus = call screen.getItemStatus("userForm", "firstName");
var lnStatus = call screen.getItemStatus("userForm", "lastName");
var emStatus = call screen.getItemStatus("userForm", "email");

if fnStatus = "changed" then
    print "First name was modified";
if lnStatus = "clean" then
    print "Last name unchanged";
```

### 2. Save Confirmation
Only prompt to save if there are changes:

```ebs
hasChanges() return bool {
    var fn = call screen.getItemStatus("myForm", "firstName");
    var ln = call screen.getItemStatus("myForm", "lastName");
    
    if fn = "changed" or ln = "changed" then
        return true;
    return false;
}

// When user tries to close
if call hasChanges() then
    print "You have unsaved changes. Save before closing?";
```

### 3. Reset to Original Values
Restore all fields to their original state:

```ebs
resetForm(formName) {
    var items = call screen.getItemList(formName);
    var i = 0;
    while i < items.length {
        var itemName = items[i];
        var status = call screen.getItemStatus(formName, itemName);
        
        if status = "changed" then
            // Reset would go here - implementation depends on requirements
            print "Item " + itemName + " was changed";
        
        i = i + 1;
    }
}
```

### 4. Data Source Selection
Switch between data and display representations:

```ebs
// For a date field, might want to show formatted vs raw value
call screen.setItemSource("myForm", "birthDate", "display");
// Now shows formatted date like "January 15, 1990"

call screen.setItemSource("myForm", "birthDate", "data");
// Now shows raw date like "1990-01-15"
```

### 5. Audit Trail
Track what changed for audit purposes:

```ebs
getChangedFields(formName) return array {
    var changedList = [];
    var items = call screen.getItemList(formName);
    
    var i = 0;
    while i < items.length {
        var itemName = items[i];
        var status = call screen.getItemStatus(formName, itemName);
        
        if status = "changed" then
            call array.push(changedList, itemName);
        
        i = i + 1;
    }
    
    return changedList;
}

// Usage
var changes = call getChangedFields("userForm");
print "Modified fields: " + changes.length;
```

## Complete Example

```ebs
// Create a contact form
screen contactForm = {
    "title": "Contact Information",
    "width": 600,
    "height": 400,
    "vars": [
        {
            "name": "name",
            "type": "string",
            "default": "John Doe",
            "display": {
                "type": "textfield",
                "labelText": "Name:",
                "promptHelp": "Enter full name"
            }
        },
        {
            "name": "email",
            "type": "string",
            "default": "john@example.com",
            "display": {
                "type": "textfield",
                "labelText": "Email:",
                "promptHelp": "Enter email address"
            }
        },
        {
            "name": "phone",
            "type": "string",
            "default": "555-1234",
            "display": {
                "type": "textfield",
                "labelText": "Phone:",
                "promptHelp": "Enter phone number"
            }
        }
    ]
};

// Initial status check
print "=== Initial Status ===";
var nameStatus = call screen.getItemStatus("contactForm", "name");
print "Name status: " + nameStatus;  // Output: clean

// Simulate user editing the form
print "";
print "=== User Edits Name ===";
contactForm.name = "Jane Smith";
nameStatus = call screen.getItemStatus("contactForm", "name");
print "Name status after edit: " + nameStatus;  // Output: changed

// Check if form has changes
print "";
print "=== Check All Fields ===";
var emailStatus = call screen.getItemStatus("contactForm", "email");
var phoneStatus = call screen.getItemStatus("contactForm", "phone");
print "Email status: " + emailStatus;  // Output: clean
print "Phone status: " + phoneStatus;  // Output: clean

// Determine if form is dirty
var isDirty = false;
if nameStatus = "changed" or emailStatus = "changed" or phoneStatus = "changed" then
    isDirty = true;

if isDirty then
    print "Form has unsaved changes!";

// Simulate save operation
print "";
print "=== Save and Reset ===";
print "Saving changes...";
// ... save logic here ...
call screen.resetItemOriginalValue("contactForm", "name");
nameStatus = call screen.getItemStatus("contactForm", "name");
print "Name status after save: " + nameStatus;  // Output: clean

// Test source property
print "";
print "=== Test Source Property ===";
var source = call screen.getItemSource("contactForm", "name");
print "Current source: " + source;  // Output: data

call screen.setItemSource("contactForm", "name", "display");
source = call screen.getItemSource("contactForm", "name");
print "Updated source: " + source;  // Output: display
```

## Design Decisions

1. **Default Values**: Both `source` and `status` default to sensible values:
   - `source` defaults to "data" (use raw data values)
   - `status` defaults to "clean" (no changes)

2. **Automatic Status**: The `status` property is computed automatically by comparing `value` to `originalValue`. It's not manually set by users (except through the builtin functions).

3. **Independent Tracking**: Each variable tracks its own original value and status independently.

4. **Case-Insensitive Lookups**: Screen and item names are case-insensitive for consistency with the rest of the EBS system.

5. **Immutable Original**: The `originalValue` is only changed when:
   - Variable is first created (set to default value)
   - `resetItemOriginalValue()` is explicitly called

## Testing

### Test Files Created

1. **test_screen_source_status.ebs** - Full test script with GUI
2. **test_screen_source_status_nogui.ebs** - Simplified test for validation

### Running Tests

To test the feature manually:
```bash
# In the EBS console, run:
run scripts/test_screen_source_status.ebs
```

The test script validates:
- Getting initial source and status (should be "data" and "clean")
- Setting source to "display"
- Modifying values and checking status becomes "changed"
- Resetting original value back to "clean"
- Reverting changes manually restores "clean" status
- Unchanged variables remain "clean"

## Future Enhancements

Potential improvements for future releases:

1. **Automatic UI Indicators**: Add visual indicators in the UI to show which fields have changed
2. **Batch Operations**: Add functions to get/reset status for all items at once
3. **Change Events**: Add callback hooks when status changes
4. **History Tracking**: Maintain history of value changes, not just original
5. **Validation Integration**: Combine status with validation to show "invalid" state
6. **Display Formatting**: Implement actual display value formatting based on source property

## Related Features

This feature works in conjunction with:
- **Screen Status Feature** (SCREEN_STATUS_FEATURE.md) - For overall screen-level status
- **Screen Edit Menu** - For editing screen properties
- **Variable Sets** - For organizing related variables

## Migration Notes

This is a **non-breaking change**:
- Existing screens work without modification
- Default values ensure backward compatibility
- New properties are optional in screen definitions
- Old code continues to function normally
