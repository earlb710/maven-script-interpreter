# Circular Import Detection Tests

This directory contains test scripts for verifying circular import detection in the EBS scripting language.

## Test Cases

### 1. Direct Circular Import (test_circular_direct.ebs)
Tests the simplest circular import: A imports B, and B imports A.

**Files:**
- `circular_direct_a.ebs` - Imports circular_direct_b.ebs
- `circular_direct_b.ebs` - Imports circular_direct_a.ebs (creates cycle)
- `test_circular_direct.ebs` - Main test script

**Expected Result:** Error message showing circular import chain

### 2. Indirect Circular Import (test_circular.ebs)
Tests a deeper circular import: A imports B, B imports C, and C imports A.

**Files:**
- `circular_a.ebs` - Imports circular_b.ebs
- `circular_b.ebs` - Imports circular_c.ebs
- `circular_c.ebs` - Imports circular_a.ebs (creates cycle)
- `test_circular.ebs` - Main test script

**Expected Result:** Error message showing circular import chain

### 3. Valid Import (test_valid_import.ebs)
Tests that normal, non-circular imports still work correctly.

**Files:**
- `utils.ebs` - Contains utility functions
- `test_valid_import.ebs` - Imports utils.ebs and uses its functions

**Expected Result:** Successful execution and correct function results

### 4. Multiple Non-Circular Imports (test_multiple_import.ebs)
Tests that the same file can be imported multiple times as long as it's not circular.

**Files:**
- `utils.ebs` - Contains utility functions
- `test_multiple_import.ebs` - Imports utils.ebs twice

**Expected Result:** Successful execution (both imports complete without error)

## Running Tests

To run these tests in the EBS interactive console:
1. Start the console: `mvn javafx:run`
2. Use the `/run` command followed by the test script path
3. Or use the File menu to open and run the test scripts

## Expected Behavior

### Circular Import Error Message Format
When a circular import is detected, the error message shows the import chain:
```
Runtime error on line X : Circular import detected: file_a.ebs -> file_b.ebs -> file_a.ebs
```

### Implementation Details
- Import tracking uses an import stack to detect when a file tries to import itself (directly or indirectly)
- Paths are normalized to handle different path representations (relative vs absolute)
- The import stack is properly cleaned up even when errors occur
- Multiple imports of the same file are allowed as long as they don't create a circular dependency
