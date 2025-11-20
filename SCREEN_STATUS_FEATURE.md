# Screen Status Tracking Feature

## Overview
This implementation adds status tracking to screens in the EBS interpreter, allowing screens to be in one of three states: CLEAN, CHANGED, or ERROR. The status determines whether a confirmation dialog is shown when the user tries to close the screen.

## Status States

### CLEAN
- Screen has no unsaved changes and no errors
- User can close the screen without any confirmation dialog
- This is the default state when a screen is created

### CHANGED
- Screen has unsaved changes that haven't been saved
- When user tries to close the screen, a warning dialog is shown:
  - Title: "Warning - Confirm Close"
  - Message: "Screen has unsaved changes.\n\nAre you sure?"
  - Buttons: YES, NO
  - If YES: Screen closes
  - If NO or dialog closed: Screen remains open

### ERROR
- Screen has encountered an error condition
- When user tries to close the screen, an error dialog is shown:
  - Title: "Error - Confirm Close"
  - Message: "Screen has an error: {error message}\n\nAre you sure?"
  - Buttons: YES, NO
  - If YES: Screen closes
  - If NO or dialog closed: Screen remains open

## Implementation Details

### New Files
1. **ScreenStatus.java** - Enum defining the three status states
   - Located: `com.eb.script.interpreter.screen.ScreenStatus`
   - Methods:
     - `fromString(String)` - Parse status from string
     - `toString()` - Convert to lowercase string

### Modified Files

#### InterpreterContext.java
- Added two new maps to track screen status:
  - `screenStatuses: Map<String, ScreenStatus>` - Tracks status per screen
  - `screenErrorMessages: Map<String, String>` - Stores error messages per screen
  
- Added methods:
  - `getScreenStatus(String screenName)` - Returns status, defaults to CLEAN
  - `setScreenStatus(String screenName, ScreenStatus status)` - Sets status
  - `getScreenErrorMessage(String screenName)` - Returns error message or null
  - `setScreenErrorMessage(String screenName, String message)` - Sets error message and auto-sets ERROR status
  
- Updated `clear()` and `remove()` methods to clean up new maps

#### InterpreterScreen.java
- Modified `visitScreenStatement()` method:
  - Updated `setOnCloseRequest` handler to check screen status
  - For CLEAN status: Closes immediately without confirmation
  - For CHANGED or ERROR status: 
    - Consumes the close event
    - Shows appropriate confirmation dialog
    - Only closes if user confirms with YES button
  
- Added `performScreenClose(String screenName)` helper method:
  - Centralizes all cleanup operations
  - Collects output fields and invokes callbacks
  - Stops screen thread
  - Cleans up resources

#### Builtins.java
Added four new builtin functions for EBS scripts:

1. **screen.setStatus(screenName, status)**
   - Parameters:
     - `screenName: string` - Name of the screen
     - `status: string` - Status value: "clean", "changed", or "error"
   - Returns: `bool` - true on success
   - Example: `call screen.setStatus(screenName="myScreen", status="changed");`

2. **screen.getStatus(screenName)**
   - Parameters:
     - `screenName: string` - Name of the screen
   - Returns: `string` - Current status: "clean", "changed", or "error"
   - Example: `var string status = call screen.getStatus(screenName="myScreen");`

3. **screen.setError(screenName, errorMessage)**
   - Parameters:
     - `screenName: string` - Name of the screen
     - `errorMessage: string` - Error message to display
   - Returns: `bool` - true on success
   - Note: Automatically sets status to ERROR
   - Example: `call screen.setError(screenName="myScreen", errorMessage="Invalid data");`

4. **screen.getError(screenName)**
   - Parameters:
     - `screenName: string` - Name of the screen
   - Returns: `string` - Error message or null if no error
   - Example: `var string error = call screen.getError(screenName="myScreen");`

## Test Script

A test script `test_screen_status.ebs` is provided in the `scripts/` directory that demonstrates:
1. Creating a screen with a combobox to select status type
2. Button to apply the selected status
3. Instructions for testing the confirmation dialogs
4. Interactive testing of all three status states

### Test Steps
1. Run the test script in the EBS console
2. A screen appears with a status selector
3. Select "clean" and click "Set Status" - try closing (should close immediately)
4. Select "changed" and click "Set Status" - try closing (should show warning)
5. Select "error" and click "Set Status" - try closing (should show error with message)

## Usage Example in EBS Scripts

```ebs
// Create a screen
screen myScreen = {
    "title": "My Screen",
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "name",
            "type": "string",
            "default": ""
        }
    ]
};

// Show the screen
screen myScreen show;

// Mark screen as changed when user edits data
call screen.setStatus(screenName="myScreen", status="changed");

// Check status before performing operations
var string status = call screen.getStatus(screenName="myScreen");
if status = "error" then
    print "Screen has errors, cannot save";

// Set error status with message
call screen.setError(screenName="myScreen", errorMessage="Name is required");

// Clear error and set to clean
call screen.setStatus(screenName="myScreen", status="clean");

// Get error message if present
var string error = call screen.getError(screenName="myScreen");
if error != null then
    print "Error: " + error;
```

## Design Decisions

1. **Status is per-screen, not global**: Each screen has its own independent status
2. **CLEAN is the default**: All screens start in CLEAN state
3. **Error message automatically sets ERROR status**: Calling `setError` sets both the message and status
4. **Confirmation dialogs use Platform.runLater**: Ensures dialogs appear on JavaFX thread
5. **Thread-safe storage**: Uses ConcurrentHashMap for status and error message maps
6. **Case-insensitive screen names**: All screen name lookups are normalized to lowercase

## Future Enhancements

Potential improvements for future releases:
1. Add callback hooks when status changes
2. Support custom confirmation dialog messages
3. Add "dirty field" tracking to automatically set CHANGED status
4. Persist status across screen hide/show cycles
5. Add visual indicators in screen UI to show current status
