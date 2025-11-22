# Submit Screen Implementation Summary

## Overview
This document describes the implementation of the new `submit screen` statement and the removal of callback functionality from `close screen`.

## Motivation
Previously, the `close screen` statement would invoke callbacks if they were set. This created ambiguity and made it unclear when callbacks would be invoked. The new implementation provides clear separation:
- Use `submit screen` when you want to close the screen AND invoke the callback
- Use `close screen` when you want to close the screen WITHOUT invoking the callback

## Implementation Details

### New Files
1. **ScreenSubmitStatement.java** - Statement class representing the submit screen operation

### Modified Files
1. **EbsTokenType.java** - Added SUBMIT keyword token
2. **Parser.java** - Added parsing logic for "submit screen" syntax
3. **InterpreterScreen.java**:
   - Added `visitScreenSubmitStatement()` method
   - Updated `visitScreenCloseStatement()` to remove callback invocation
4. **StatementVisitor.java** - Added visitScreenSubmitStatement interface method
5. **Interpreter.java** - Added delegation to visitScreenSubmitStatement

## Syntax

### Submit Screen
```javascript
// Submit the current screen (requires callback to be set)
submit screen;

// Submit a specific screen (requires callback to be set)
submit screen myScreen;
```

### Close Screen (Updated Behavior)
```javascript
// Close the current screen (no callback invoked)
close screen;

// Close a specific screen (no callback invoked)
close screen myScreen;
```

### Show Screen with Callback
```javascript
// Show a screen with a callback function
show screen myScreen callback handleSubmit;

// The callback will only be invoked if "submit screen" is used
```

## Behavior

### Submit Screen
- **Purpose**: Close the screen and invoke the callback
- **Requirements**: A callback MUST be set when the screen is shown
- **Error**: Throws an error if no callback is set
- **Actions**:
  1. Validates that a callback is registered
  2. Collects output fields (variables with "out" or "inout" scope)
  3. Invokes the callback with collected data
  4. Closes the screen
  5. Cleans up resources

### Close Screen (Updated)
- **Purpose**: Close the screen without invoking callback
- **Requirements**: None (no callback required)
- **Error**: None related to callbacks
- **Actions**:
  1. Closes the screen
  2. Cleans up resources
  3. Does NOT invoke any callback

## Callback Data Structure

When a callback is invoked via `submit screen`, it receives a JSON object with:
```json
{
  "screenName": "myScreen",
  "event": "closed",
  "timestamp": 1700000000000,
  "fields": [
    {
      "name": "fieldName",
      "type": "string",
      "value": "field value",
      "scope": "out"
    }
  ]
}
```

## Example Usage

```javascript
// Define a callback function
handleFormSubmit(eventData: json) return void {
    print "Form submitted!";
    print "Screen: " + eventData.screenName;
    
    // Process output fields
    var i: int = 0;
    while (i < eventData.fields.length) {
        var field: json = eventData.fields[i];
        print field.name + " = " + field.value;
        i = i + 1;
    }
}

// Create a screen
screen myForm = {
    "title": "User Form",
    "vars": [
        {
            "name": "username",
            "type": "string",
            "scope": "out",
            "display": { "type": "textfield" }
        }
    ],
    "area": [
        {
            "name": "main",
            "type": "vbox",
            "items": [
                { "name": "username", "varRef": "username", "seq": 1 },
                {
                    "name": "submitBtn",
                    "seq": 2,
                    "display": {
                        "type": "button",
                        "labelText": "Submit",
                        "onClick": "submit screen;"
                    }
                },
                {
                    "name": "cancelBtn",
                    "seq": 3,
                    "display": {
                        "type": "button",
                        "labelText": "Cancel",
                        "onClick": "close screen;"
                    }
                }
            ]
        }
    ]
};

// Show the screen with callback
show screen myForm callback handleFormSubmit;
```

## Testing

A comprehensive test script `test_submit_screen.ebs` has been created that covers:
1. Submit with callback (success case)
2. Submit without callback (error case)
3. Close without callback (success case)

## Backward Compatibility

**Breaking Change**: The `close screen` statement no longer invokes callbacks. If your code relied on this behavior, you must:
1. Update `close screen` calls to `submit screen` where callback invocation is desired
2. Ensure callbacks are set when using `submit screen`

## Error Messages

### Submit Screen Errors
- **No callback set**: "Cannot submit screen 'screenName': No callback was specified when the screen was shown. Use 'show screen screenName callback <functionName>;' to set a callback."
- **Screen not shown**: "Screen 'screenName' has not been shown yet."
- **No screen name in context**: "No screen name specified and not executing in a screen context. Use 'submit screen <name>;' to submit a specific screen, or call 'submit screen;' from within screen event handlers (e.g., onClick)."
