# Quick Test Reference

## Run All Tests (Automated)
```bash
cd ScriptInterpreter
scripts/test_circular_imports.sh
```

## Run Individual Tests

### Valid Import Test
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test_valid_import.ebs
```
**Expected:** Success, prints "SUCCESS: Valid import works correctly!"

### Multiple Import Test
```bash
java -cp target/classes com.eb.script.Run scripts/test_multiple_import.ebs
```
**Expected:** Success, prints "SUCCESS: Multiple non-circular imports work!"

### Direct Circular Import Test (A→B→A)
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular_direct.ebs
```
**Expected:** Error message: "Circular import detected: circular_direct_a.ebs -> circular_direct_b.ebs -> circular_direct_a.ebs"

### Indirect Circular Import Test (A→B→C→A)
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular.ebs
```
**Expected:** Error message showing the full circular chain

### Comprehensive Test Suite
```bash
java -cp target/classes com.eb.script.Run scripts/test_circular_imports_comprehensive.ebs
```
**Expected:** Success, shows pass/fail for each test case

## Test Files

- `test_valid_import.ebs` - Tests normal imports ✓
- `test_multiple_import.ebs` - Tests multiple non-circular imports ✓
- `test_circular_direct.ebs` - Tests A→B→A detection ✗ (expected)
- `test_circular.ebs` - Tests A→B→C→A detection ✗ (expected)
- `test_circular_imports_comprehensive.ebs` - Automated test suite ✓
- `test_circular_imports.sh` - Shell script runner

✓ = Should succeed
✗ = Should fail with circular import error
