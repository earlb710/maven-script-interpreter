# Quick Test Reference

## Run All Tests (Automated)
```bash
cd ScriptInterpreter
scripts/test_circular_imports.sh
```

## Run Individual Tests

### Valid Import Tests (Should Pass ✓)

#### Test 1: Valid Import
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test_valid_import.ebs
```
**Expected:** Success, prints "SUCCESS: Valid import works correctly!"

#### Test 2: Multiple Imports (Same File)
```bash
java -cp target/classes com.eb.script.Run scripts/test_multiple_import.ebs
```
**Expected:** Success, prints "SUCCESS: Multiple non-circular imports work!"

#### Test 3: Multiple Different Imports
```bash
java -cp target/classes com.eb.script.Run scripts/test_multiple_different_imports.ebs
```
**Expected:** Success, imports helper_math, helper_string, and utils

#### Test 4: Combined Import (Diamond Dependency)
```bash
java -cp target/classes com.eb.script.Run scripts/test_combined_import.ebs
```
**Expected:** Success, helper_combined imports helper_math and helper_string

#### Test 5: Deep Import Chain (A->B->C->D)
```bash
java -cp target/classes com.eb.script.Run scripts/test_deep_import_chain.ebs
```
**Expected:** Success, deep chain of 4 imports loads completely

### Circular Import Tests (Should Fail ✗)

#### Test 6: Direct Circular Import (A→B→A)
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular_direct.ebs
```
**Expected:** Error message: "Circular import detected: circular_direct_a.ebs -> circular_direct_b.ebs -> circular_direct_a.ebs"

#### Test 7: Indirect Circular Import (A→B→C→A)
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular.ebs
```
**Expected:** Error message showing the full circular chain

#### Test 8: Self-Import Detection
```bash
java -cp target/classes com.eb.script.Run scripts/test_self_import.ebs
```
**Expected:** Error message: "Circular import detected: self_import.ebs -> self_import.ebs"

### Comprehensive Test Suite
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular_imports_comprehensive.ebs
```
**Expected:** Success, shows pass/fail for each test case

## Test Files Overview

### Helper Files
- `utils.ebs` - Original utility functions (multiplyByTwo, factorial, formatGreeting)
- `helper_math.ebs` - Math functions (add, subtract, multiply)
- `helper_string.ebs` - String functions (concat, repeat)
- `helper_combined.ebs` - Imports both helper_math and helper_string
- `deep_import_a.ebs` through `deep_import_d.ebs` - Chain of imports (no cycle)

### Circular Import Files
- `circular_a.ebs`, `circular_b.ebs`, `circular_c.ebs` - Form A→B→C→A cycle
- `circular_direct_a.ebs`, `circular_direct_b.ebs` - Form A→B→A cycle
- `self_import.ebs` - Imports itself

### Test Scripts
- `test_valid_import.ebs` - Tests normal imports ✓
- `test_multiple_import.ebs` - Tests multiple imports of same file ✓
- `test_multiple_different_imports.ebs` - Tests importing different files ✓
- `test_combined_import.ebs` - Tests diamond dependency ✓
- `test_deep_import_chain.ebs` - Tests deep chain (4 levels) ✓
- `test_circular_direct.ebs` - Tests A→B→A detection ✗
- `test_circular.ebs` - Tests A→B→C→A detection ✗
- `test_self_import.ebs` - Tests self-import detection ✗
- `test_circular_imports_comprehensive.ebs` - Automated test suite ✓
- `test_circular_imports.sh` - Shell script runner

✓ = Should succeed
✗ = Should fail with circular import error
