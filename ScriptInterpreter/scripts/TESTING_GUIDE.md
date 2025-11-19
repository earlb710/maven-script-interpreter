# Circular Import Detection Testing Guide

This guide explains how to test the circular import detection feature in the EBS scripting language.

## Test Files Overview

### Automated Test Suite

1. **test_circular_imports_comprehensive.ebs**
   - Comprehensive test script that can be run from within the EBS console
   - Tests valid imports, multiple non-circular imports
   - Reports pass/fail status for each test
   - Provides guidance on manual tests that must be run separately

2. **test_circular_imports.sh**
   - Shell script that automates running all test scenarios
   - Builds the project, runs tests, and reports results
   - Tests both success cases (valid imports) and failure cases (circular imports)
   - Color-coded output for easy reading

### Individual Test Scripts

1. **test_valid_import.ebs**
   - Tests that normal imports work correctly
   - Imports utils.ebs and verifies functions work
   - Should succeed

2. **test_multiple_import.ebs**
   - Tests importing the same file multiple times (non-circular)
   - Should succeed

3. **test_circular_direct.ebs**
   - Tests direct circular import: A→B→A
   - Uses circular_direct_a.ebs and circular_direct_b.ebs
   - Should fail with "Circular import detected" error

4. **test_circular.ebs**
   - Tests indirect circular import: A→B→C→A
   - Uses circular_a.ebs, circular_b.ebs, and circular_c.ebs
   - Should fail with "Circular import detected" error

### Supporting Files

- **circular_a.ebs, circular_b.ebs, circular_c.ebs** - Files forming A→B→C→A cycle
- **circular_direct_a.ebs, circular_direct_b.ebs** - Files forming A→B→A cycle
- **utils.ebs** - Utility functions used by valid import tests

## Running the Tests

### Method 1: Automated Shell Script (Recommended)

Run the automated test suite from the ScriptInterpreter directory:

```bash
cd ScriptInterpreter
scripts/test_circular_imports.sh
```

This will:
1. Build the project
2. Run all test cases
3. Report pass/fail for each test
4. Display a summary with color-coded results

**Expected Output:**
```
==========================================
Circular Import Detection Test Runner
==========================================

Building project...
Build successful

==========================================
Running Test Suite
==========================================

Running: Test 1: Valid Import
[PASS] Test 1: Valid Import

Running: Test 2: Multiple Non-Circular Imports
[PASS] Test 2: Multiple Non-Circular Imports

Running: Test 3: Direct Circular Import (A->B->A)
[PASS] Test 3: Direct Circular Import (A->B->A) - Failed with expected error

Running: Test 4: Indirect Circular Import (A->B->C->A)
[PASS] Test 4: Indirect Circular Import (A->B->C->A) - Failed with expected error

Running: Test 5: Comprehensive Test Suite
[PASS] Test 5: Comprehensive Test Suite

==========================================
Test Summary
==========================================
Total Tests: 5
Passed: 5
Failed: 0

✓ All tests passed!
```

### Method 2: EBS Console (Interactive)

1. Start the EBS interactive console:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. In the console, run the comprehensive test suite:
   ```
   /run scripts/test_circular_imports_comprehensive.ebs
   ```

3. To test circular import detection (these will cause errors, which is expected):
   ```
   /run scripts/test_circular_direct.ebs
   ```
   Expected: Error with "Circular import detected: circular_direct_a.ebs -> circular_direct_b.ebs -> circular_direct_a.ebs"

   ```
   /run scripts/test_circular.ebs
   ```
   Expected: Error with "Circular import detected" showing the full A→B→C→A chain

### Method 3: Command Line (Individual Tests)

Run individual tests from the command line:

```bash
cd ScriptInterpreter

# Test valid import (should succeed)
java -cp target/classes com.eb.script.Run scripts/test_valid_import.ebs

# Test circular import (should fail with error)
java -cp target/classes com.eb.script.Run scripts/test_circular_direct.ebs
```

## Understanding Test Results

### Success Cases (Should Pass)
- **Valid imports**: Script runs without error, imported functions work correctly
- **Multiple imports**: Same file can be imported multiple times if not circular

### Failure Cases (Should Show Error)
- **Circular imports**: Script fails with clear error message showing the circular chain
- Error format: `Runtime error on line X : Circular import detected: file_a.ebs -> file_b.ebs -> file_a.ebs`

## Test Coverage

The test suite covers:

1. ✅ **Valid Import**: Normal import functionality still works
2. ✅ **Multiple Non-Circular Imports**: Same file imported twice (allowed)
3. ✅ **Direct Circular Import**: A→B→A pattern detected
4. ✅ **Indirect Circular Import**: A→B→C→A pattern detected
5. ✅ **Path Normalization**: Different path representations treated as same file
6. ✅ **Error Messages**: Clear, helpful error messages showing import chain
7. ✅ **Stack Cleanup**: Import stack properly cleaned up on errors

## Troubleshooting

### Build Errors
If the automated test script fails to build:
```bash
cd ScriptInterpreter
mvn clean compile
```

### JavaFX Dependency Issues
If running from command line shows JavaFX errors, use:
```bash
mvn javafx:run
```
Then run tests from the console instead.

### Test Script Not Found
Ensure you're running from the ScriptInterpreter directory:
```bash
cd /path/to/maven-script-interpreter/ScriptInterpreter
```

## Continuous Integration

To integrate these tests into CI/CD:

```yaml
# Example GitHub Actions workflow step
- name: Run Circular Import Tests
  run: |
    cd ScriptInterpreter
    ./scripts/test_circular_imports.sh
```

## Adding New Tests

To add a new test case:

1. Create test files in `ScriptInterpreter/scripts/`
2. Add test case to `test_circular_imports.sh` using `run_success_test` or `run_failure_test`
3. Document the test in this file
4. Run the automated suite to verify

## Questions?

For more information about the circular import detection implementation, see:
- `CIRCULAR_IMPORT_IMPLEMENTATION.md` - Technical implementation details
- `CIRCULAR_IMPORT_TESTS.md` - Test case descriptions
