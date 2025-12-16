# Test Scripts for New Var-Level Builtin Functions

This directory contains comprehensive test scripts for the 7 new var-level builtin functions introduced in the stateful property migration.

## Overview

These functions provide fine-grained control over variable state management in EBS screens:

1. **scr.setVarStateful** - Control whether changes mark screen as dirty
2. **scr.getVarStateful** - Query stateful property state
3. **scr.getVarOriginalValue** - Retrieve pre-modification baseline value
4. **scr.submitVarItem** - Mark current value as new baseline (post-save)
5. **scr.resetVarItem** - Revert to original value (cancel changes)
6. **scr.clearVarItem** - Clear to default value
7. **scr.initVarItem** - Reset to default as new baseline

## Test Scripts

### Comprehensive Test
- **test_all_var_builtin_functions.ebs** - Complete test suite covering all 7 functions with workflow simulation

### Individual Function Tests
- **test_scr_getVarStateful.ebs** - Test getting stateful property
- **test_scr_setVarStateful.ebs** - Test setting stateful property dynamically
- **test_scr_getVarOriginalValue.ebs** - Test retrieving original values
- **test_scr_submitVarItem.ebs** - Test marking values as submitted
- **test_scr_resetVarItem.ebs** - Test reverting to original values
- **test_scr_clearVarItem.ebs** - Test clearing to defaults
- **test_scr_initVarItem.ebs** - Test initializing to default as original

## Running the Tests

All test scripts require the EBS interactive console with JavaFX initialized.

### Option 1: Run in Interactive Console

1. Start the EBS console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. In the console, import any test script:
   ```javascript
   import "scripts/test/test_all_var_builtin_functions.ebs";
   ```
   or
   ```javascript
   import "scripts/test/test_scr_getVarStateful.ebs";
   ```

### Option 2: Run Individual Tests

Each test script is self-contained and can be run independently:

```javascript
// In EBS console
import "scripts/test/test_scr_setVarStateful.ebs";
import "scripts/test/test_scr_resetVarItem.ebs";
// etc.
```

## Test Script Format

Each test script:
- Creates a simple screen with test variables
- Shows the screen (required for screen registration)
- Tests the specific builtin function(s)
- Prints clear output showing expected vs actual results
- Includes comments explaining the use case

## Expected Output

All test scripts print their results to the console with clear labels:
- Initial states
- Actions performed
- Results with expected values
- Success confirmation

## Common Use Cases Demonstrated

### Database Save Workflow
```javascript
// User edits form
testScreen.customerName = "Jane Smith";

// User clicks Save - submit to database
// ... database code ...

// Mark as new baseline
call scr.submitVarItem("testScreen", "customerName");
```

### Cancel Changes
```javascript
// User edits form
testScreen.email = "newemail@example.com";

// User clicks Cancel - revert changes
call scr.resetVarItem("testScreen", "email");
```

### Clear Form
```javascript
// Clear all fields to defaults
call scr.clearVarItem("testScreen", "field1");
call scr.clearVarItem("testScreen", "field2");
```

### Complete Reset
```javascript
// Reset to default and mark as new baseline
call scr.initVarItem("testScreen", "field1");
```

## Validation

Each test script validates:
- Correct syntax according to EBS_SCRIPT_SYNTAX.md
- Proper use of the `call` keyword for builtin functions
- Type annotations where appropriate
- Screen definition with `stateful` property at var level
- Expected behavior of each function

## Troubleshooting

**Screen not found error:**
- Ensure screen is defined before calling builtins
- Ensure `show screen` is called to register the screen

**Toolkit not initialized:**
- Tests must be run in interactive console (mvn javafx:run)
- Cannot run via command-line `java -cp` without JavaFX setup

**Variable not found:**
- Check variable name matches definition (case-insensitive)
- Verify screen name is correct

## Related Documentation

- **STATEFUL_PROPERTY.md** - Complete documentation of stateful property
- **EBS_SCRIPT_SYNTAX.md** - EBS language syntax reference
- **SCREEN_ITEM_SOURCE_STATUS.md** - Status property documentation
