# Test Demonstration - Import Conflict Detection

## Test 1: Valid Imports (No Conflicts)
**Script**: `test_valid_imports.ebs`
```
import "helper_math.ebs";
import "helper_string.ebs";

var sum: int = call add(10, 5);
var text: string = call concat("Hello", " World");
```

**Output**:
```
Loading math helpers...
Math helpers loaded
Loading string helpers...
String helpers loaded
=== Testing Valid Imports ===

Testing imported functions...
add(10, 5) = 15
multiply(3, 4) = 12
concat result: Hello World

SUCCESS: Valid imports work correctly!
=== Test Completed ===
```
✅ **Result**: Script executes successfully when function names are unique.

---

## Test 2: Duplicate Function in Two Imports
**Script**: `test_simple_duplicate.ebs`
```
import "helper_duplicate1.ebs";  // defines testFunc(x) { return x * 2; }
import "helper_duplicate2.ebs";  // defines testFunc(x) { return x * 3; }
```

**Output**:
```
Loading helper_duplicate1...
helper_duplicate1 loaded
Loading helper_duplicate2...
helper_duplicate2 loaded
Error: Runtime error on line 10 : Function 'testfunc' is already declared 
       in helper_duplicate1.ebs and cannot be overwritten by import from 
       helper_duplicate2.ebs
```
❌ **Result**: Error thrown, clearly identifying the conflict source.

---

## Test 3: Function in Current Script, Then Import
**Script**: `test_duplicate_in_current.ebs`
```
myFunc(x: int) return int {
    return x * 10;
}

import "helper_with_myfunc.ebs";  // also defines myFunc(x)
```

**Output**:
```
Loading helper_with_myfunc...
helper_with_myfunc loaded
Error: Runtime error on line 11 : Function 'myfunc' is already declared 
       in test_duplicate_in_current.ebs and cannot be overwritten by import 
       from helper_with_myfunc.ebs
```
❌ **Result**: Error thrown, preventing import from overwriting current script function.

---

## Test 4: Duplicate Screen in Two Imports
**Script**: `test_duplicate_screen_import.ebs`
```
import "helper_screen1.ebs";  // defines screen testScreen = { ... }
import "helper_screen2.ebs";  // defines screen testScreen = { ... }
```

**Output**:
```
Loading helper_screen1...
helper_screen1 loaded
Loading helper_screen2...
Error: Runtime error on line 24 : Runtime error on line 24 : Screen 
       'testscreen' is already declared in helper_screen1.ebs and 
       cannot be overwritten
```
❌ **Result**: Error thrown for duplicate screen name.

---

## Summary

| Test Scenario | Expected | Actual | Status |
|--------------|----------|--------|--------|
| Valid imports with unique names | Success | Success | ✅ |
| Two imports with same function | Error | Error with clear message | ✅ |
| Current function + import same name | Error | Error with clear message | ✅ |
| Two imports with same screen | Error | Error with clear message | ✅ |
| Current screen + import same name | Error | Error with clear message | ✅ |

All tests pass! The implementation successfully prevents all forms of function and screen name overwrites.
