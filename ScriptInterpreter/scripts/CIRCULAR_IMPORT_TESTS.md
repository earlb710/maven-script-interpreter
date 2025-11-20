# Circular Import Detection Tests

This directory contains test scripts for verifying circular import detection in the EBS scripting language.

## Test Cases

### Valid Import Tests (Should Pass)

#### 1. Valid Import (test_valid_import.ebs)
Tests that normal imports work correctly.

**Files:**
- `utils.ebs` - Contains utility functions
- `test_valid_import.ebs` - Imports utils.ebs and uses its functions

**Expected Result:** Success, imported functions work correctly

#### 2. Multiple Non-Circular Imports (test_multiple_import.ebs)
Tests that the same file can be imported multiple times as long as it's not circular.

**Files:**
- `utils.ebs` - Contains utility functions
- `test_multiple_import.ebs` - Imports utils.ebs twice

**Expected Result:** Success, both imports complete without error

#### 3. Multiple Different Imports (test_multiple_different_imports.ebs)
Tests importing multiple different files in the same script.

**Files:**
- `helper_math.ebs` - Math helper functions
- `helper_string.ebs` - String helper functions
- `utils.ebs` - Utility functions
- `test_multiple_different_imports.ebs` - Imports all three files

**Expected Result:** Success, all imports work and functions are accessible

#### 4. Combined Import (test_combined_import.ebs)
Tests importing a file that itself imports other files (diamond dependency pattern).

**Dependency Graph:**
```
test_combined_import.ebs
    └─> helper_combined.ebs
            ├─> helper_math.ebs
            └─> helper_string.ebs
```

**Files:**
- `helper_math.ebs` - Math functions
- `helper_string.ebs` - String functions
- `helper_combined.ebs` - Imports both helper files
- `test_combined_import.ebs` - Imports helper_combined.ebs

**Expected Result:** Success, diamond dependency handled correctly

#### 5. Deep Import Chain (test_deep_import_chain.ebs)
Tests a deep import chain (4 levels) without cycles.

**Dependency Graph:**
```
test_deep_import_chain.ebs
    └─> deep_import_a.ebs
            └─> deep_import_b.ebs
                    └─> deep_import_c.ebs
                            └─> deep_import_d.ebs
```

**Files:**
- `deep_import_a.ebs` through `deep_import_d.ebs` - Chain of imports
- `test_deep_import_chain.ebs` - Starts the import chain

**Expected Result:** Success, deep chain loads completely

### Circular Import Tests (Should Fail with Error)

#### 6. Direct Circular Import (test_circular_direct.ebs)
Tests the simplest circular import: A imports B, and B imports A.

**Dependency Graph:**
```
test_circular_direct.ebs
    └─> circular_direct_a.ebs
            └─> circular_direct_b.ebs
                    └─> circular_direct_a.ebs ❌ CYCLE
```

**Files:**
- `circular_direct_a.ebs` - Imports circular_direct_b.ebs
- `circular_direct_b.ebs` - Imports circular_direct_a.ebs (creates cycle)
- `test_circular_direct.ebs` - Main test script

**Expected Result:** Error message showing circular import chain

#### 7. Indirect Circular Import (test_circular.ebs)
Tests a deeper circular import: A imports B, B imports C, and C imports A.

**Dependency Graph:**
```
test_circular.ebs
    └─> circular_a.ebs
            └─> circular_b.ebs
                    └─> circular_c.ebs
                            └─> circular_a.ebs ❌ CYCLE
```

**Files:**
- `circular_a.ebs` - Imports circular_b.ebs
- `circular_b.ebs` - Imports circular_c.ebs
- `circular_c.ebs` - Imports circular_a.ebs (creates cycle)
- `test_circular.ebs` - Main test script

**Expected Result:** Error message showing circular import chain

#### 8. Self-Import Detection (test_self_import.ebs)
Tests a file importing itself.

**Dependency Graph:**
```
test_self_import.ebs
    └─> self_import.ebs
            └─> self_import.ebs ❌ CYCLE
```

**Files:**
- `self_import.ebs` - Imports itself
- `test_self_import.ebs` - Main test script

**Expected Result:** Error message indicating self-import detected

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
