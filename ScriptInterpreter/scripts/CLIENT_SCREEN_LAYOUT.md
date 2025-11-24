# Client Screen UI Layout

This document describes the visual layout of the client information screen.

## Window Layout

```
╔══════════════════════════════════════════════════════════════╗
║  Client Information                                    [_][□][X]║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║                                                              ║
║    Client Name:  [________________________]                 ║
║                  Enter client name                          ║
║                                                              ║
║    Surname:      [________________________]                 ║
║                  Enter surname                              ║
║                                                              ║
║    Age:          [  18  ] [▲▼]                             ║
║                                                              ║
║    Client ID:    [________________________]                 ║
║                  Enter client ID                            ║
║                                                              ║
║                                                              ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

**Window Dimensions:** 600px width × 400px height

## Field Descriptions

### 1. Client Name (Text Field)
- **Type:** String input field
- **Control:** Standard text field
- **Label:** "Client Name:"
- **Placeholder:** "Enter client name"
- **Validation:** Mandatory (required field)
- **Access:** `clientScreen.clientName`

### 2. Surname (Text Field)
- **Type:** String input field
- **Control:** Standard text field
- **Label:** "Surname:"
- **Placeholder:** "Enter surname"
- **Validation:** Mandatory (required field)
- **Access:** `clientScreen.surname`

### 3. Age (Spinner)
- **Type:** Integer input control
- **Control:** Spinner with up/down buttons
- **Label:** "Age:"
- **Range:** 0 to 150
- **Default:** 18
- **Validation:** Mandatory (required field)
- **Access:** `clientScreen.age`

### 4. Client ID (Text Field)
- **Type:** String input field
- **Control:** Standard text field
- **Label:** "Client ID:"
- **Placeholder:** "Enter client ID"
- **Validation:** Mandatory (required field)
- **Access:** `clientScreen.clientId`
- **Note:** In demo version, text is auto-converted to uppercase

## Interactive Features

### Field Updates
All fields can be updated in two ways:

1. **User Input (GUI):**
   - Click on any field and type or use controls
   - Spinner allows clicking up/down arrows or typing directly
   - All changes are immediately reflected in the underlying variables

2. **Programmatic Updates (Script):**
   ```javascript
   // Setting values
   clientScreen.clientName = "John";
   clientScreen.surname = "Doe";
   clientScreen.age = 30;
   clientScreen.clientId = "CLT-12345";
   
   // Reading values
   var name = clientScreen.clientName;
   var fullName = clientScreen.clientName + " " + clientScreen.surname;
   ```

### Validation
- **Mandatory Fields:** All fields are marked as mandatory
- **Age Range:** Spinner enforces 0-150 range automatically
- **Real-time Updates:** Changes are immediately synchronized

### Keyboard Navigation
- **Tab:** Move to next field
- **Shift+Tab:** Move to previous field
- **Up/Down Arrows:** Increment/decrement age (when spinner is focused)
- **Enter:** Accept value in current field

## Example Usage Scenarios

### Scenario 1: New Client Entry
```javascript
// Start with empty fields
show screen clientScreen;

// User enters:
// - Client Name: "Alice"
// - Surname: "Johnson"
// - Age: 28
// - Client ID: "CLT-001"

// Read entered values
print clientScreen.clientName;  // Output: Alice
print clientScreen.surname;     // Output: Johnson
print clientScreen.age;         // Output: 28
print clientScreen.clientId;    // Output: CLT-001
```

### Scenario 2: Update Existing Client
```javascript
// Pre-populate with existing data
clientScreen.clientName = "Bob";
clientScreen.surname = "Smith";
clientScreen.age = 45;
clientScreen.clientId = "CLT-999";

// Show screen for editing
show screen clientScreen;

// User modifies age to 46
// Changes are automatically reflected
```

### Scenario 3: Birthday Update
```javascript
// Increment age programmatically
var currentAge = clientScreen.age;
clientScreen.age = currentAge + 1;
print "Happy Birthday! New age: " + clientScreen.age;
```

## Visual Style

- **Labels:** Displayed to the left of input fields
- **Text Fields:** White background with gray border
- **Spinner:** Numeric display with up/down arrow buttons
- **Mandatory Fields:** All fields required before form submission
- **Prompt Text:** Gray italic text shown when field is empty
- **Focus:** Selected field highlighted with blue border

## Technical Notes

The client screen is implemented using the EBS screen definition syntax:
- Each screen runs independently and supports concurrent access
- All field updates are thread-safe
- Layout uses vertical arrangement with label-control pairs

## Related Files

- **Basic Script:** `client_screen.ebs`
- **Enhanced Demo:** `client_screen_demo.ebs`
- **Documentation:** `CLIENT_SCREEN_README.md`
