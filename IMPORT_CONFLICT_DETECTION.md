# Import Conflict Detection - Implementation Summary

## Overview
This implementation adds strict conflict detection for function and screen names when importing scripts in the EBS interpreter. When a name is already declared (in the current script or a previous import), attempting to import or declare another function/screen with the same name will result in a clear error message.

## Problem Statement
Previously, the EBS interpreter allowed:
1. Functions/screens in imported scripts to silently overwrite each other (last import wins)
2. Functions/screens in the current script to overwrite imported ones (or vice versa)
3. No error or warning when name conflicts occurred

## Solution
Implemented strict no-overwrite semantics:
- Track all declared function and screen names with their source files
- Check for conflicts before allowing any new declaration
- Provide clear error messages indicating which file first declared the conflicting name

## Behavior Changes

### Before: Import-to-Import Conflicts (Allowed)
```
import "helper_duplicate1.ebs";  // defines testFunc()
import "helper_duplicate2.ebs";  // defines testFunc() - silently overwrites!
call testFunc(5);  // Calls version from helper_duplicate2.ebs
```
**Result**: Second import silently overwrites the first, no error

### After: Import-to-Import Conflicts (Blocked)
```
import "helper_duplicate1.ebs";  // defines testFunc()
import "helper_duplicate2.ebs";  // defines testFunc() - ERROR!
```
**Result**: `Error: Function 'testfunc' is already declared in helper_duplicate1.ebs and cannot be overwritten by import from helper_duplicate2.ebs`

### Before: Current Script vs Import Conflicts (Allowed)
```
myFunc(x: int) return int {
    return x * 10;
}

import "helper_with_myfunc.ebs";  // defines myFunc() - may overwrite depending on parse order
```
**Result**: Depending on parse order, one version overwrites the other

### After: Current Script vs Import Conflicts (Blocked)
```
myFunc(x: int) return int {
    return x * 10;
}

import "helper_with_myfunc.ebs";  // defines myFunc() - ERROR!
```
**Result**: `Error: Function 'myfunc' is already declared in test_duplicate_in_current.ebs and cannot be overwritten by import from helper_with_myfunc.ebs`

### Screens - Same Behavior
Screen declarations follow the same rules:
```
screen myScreen = { ... };
import "helper_with_myscreen.ebs";  // ERROR if defines myScreen
```

## Implementation Details

### 1. Tracking Maps (InterpreterContext.java)
```java
// Track declared function names to prevent overwrites (functionName -> source file/script name)
private final Map<String, String> declaredFunctions = new ConcurrentHashMap<>();

// Track declared screen names to prevent overwrites (screenName -> source file/script name)
private final Map<String, String> declaredScreens = new ConcurrentHashMap<>();
```

### 2. Function Registration (Interpreter.java - interpret method)
At the start of script execution, register all functions from the current script:
```java
// Register all functions from the current script BEFORE any imports are processed
if (runtime.blocks != null) {
    for (String functionName : runtime.blocks.keySet()) {
        context.getDeclaredFunctions().put(functionName, runtime.name);
    }
}
```

### 3. Import Conflict Detection (Interpreter.java - visitImportStatement)
When importing, check each function for conflicts:
```java
// Check if this function name was already declared
if (context.getDeclaredFunctions().containsKey(functionName)) {
    String existingSource = context.getDeclaredFunctions().get(functionName);
    throw error(stmt.getLine(), 
        "Function '" + functionName + "' is already declared in " + existingSource + 
        " and cannot be overwritten by import from " + stmt.filename);
}
// Register this function as declared from the imported file
context.getDeclaredFunctions().put(functionName, stmt.filename);
```

### 4. Screen Conflict Detection (InterpreterScreen.java - visitScreenStatement)
When declaring a screen, check for conflicts:
```java
// Check if this screen name was already declared
if (context.getDeclaredScreens().containsKey(stmt.name)) {
    String existingSource = context.getDeclaredScreens().get(stmt.name);
    throw interpreter.error(stmt.getLine(), 
        "Screen '" + stmt.name + "' is already declared in " + existingSource + 
        " and cannot be overwritten");
}
// Register this screen as declared
context.getDeclaredScreens().put(stmt.name, sourceName);
```

### 5. Import Context Tracking
Added tracking of current import file to provide accurate error messages:
```java
private String currentImportFile;  // Track which import file is currently being processed

// Set during import execution
currentImportFile = stmt.filename;
try {
    // Execute imported statements
} finally {
    currentImportFile = previousImportFile;
}
```

## Error Messages
All error messages clearly indicate:
1. The conflicting name
2. Which file first declared the name
3. Which file attempted to redeclare it

Examples:
- `Function 'testfunc' is already declared in helper_duplicate1.ebs and cannot be overwritten by import from helper_duplicate2.ebs`
- `Screen 'testscreen' is already declared in helper_screen1.ebs and cannot be overwritten`

## Testing
Comprehensive test scripts were created to verify all scenarios:

### Test Scripts
1. **test_simple_duplicate.ebs** - Tests two imports with same function
2. **test_duplicate_in_current.ebs** - Tests current script function + import
3. **test_duplicate_screen_import.ebs** - Tests two imports with same screen
4. **test_screen_in_current_then_import.ebs** - Tests current script screen + import
5. **test_valid_imports.ebs** - Verifies valid imports still work correctly
6. **test_comprehensive_conflicts.ebs** - Combined test of multiple scenarios

### Test Results
✅ All conflict scenarios properly detected and reported
✅ Valid imports with unique names work correctly
✅ Error messages are clear and informative
✅ No security vulnerabilities introduced (CodeQL scan passed)

## Edge Cases Handled
1. **Case-insensitive matching**: Function and screen names are matched case-insensitively
2. **Import ordering**: Parser moves import statements to the front, so they always execute before current script statements
3. **Nested imports**: Each import is tracked separately to provide accurate error messages
4. **Context restoration**: Import context is properly restored after processing each import

## Backward Compatibility
This is a **breaking change** that adds stricter validation:
- Scripts that previously worked by relying on silent overwrites will now fail with errors
- This is intentional and aligns with the requirement to prevent all overwrites
- Scripts can be fixed by renaming conflicting functions/screens

## Files Modified
1. `InterpreterContext.java` - Added tracking maps and getters
2. `Interpreter.java` - Added registration and conflict detection logic
3. `InterpreterScreen.java` - Added screen conflict detection
4. `Run.java` - Improved error handling to display InterpreterError messages

## Conclusion
The implementation successfully prevents all forms of function and screen name overwrites during imports, providing clear error messages to help developers identify and fix conflicts. The strict no-overwrite semantics ensure that the source of each function and screen is always unambiguous.
