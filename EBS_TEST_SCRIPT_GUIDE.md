# EBS Test Script Writing Guide

## Overview

This guide explains how to write effective test scripts for the EBS (Earl Bosch Script) language using the built-in testing framework centered around `debug.assert` and `debug.assertEquals`.

## Test Script Structure

### Basic Template

```javascript
// Test: [Test Suite Name]
// Purpose: [Brief description of what is being tested]

// Enable debug mode to see assertion results
call debug.on();

// Initialize test counters
var totalTests: int = 0;
var passedTests: int = 0;
var failedTests: int = 0;

// ============================================
// Test Suite: [Category Name]
// ============================================

// Test 1: [Test Description]
totalTests = totalTests + 1;
var result1 = call debug.assert(true, "Test 1: Description of what should pass");
if result1 then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// Test 2: [Test Description]
totalTests = totalTests + 1;
var result2 = call debug.assertEquals(expected, actual, "Test 2: Description");
if result2 then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// ============================================
// Test Summary
// ============================================
print "========================================";
print "Test Results Summary";
print "========================================";
print "Total Tests:  " + totalTests;
print "Passed:       " + passedTests;
print "Failed:       " + failedTests;
print "Success Rate: " + ((passedTests * 100) / totalTests) + "%";
print "========================================";

if failedTests > 0 then {
    print "❌ TESTS FAILED";
} else {
    print "✅ ALL TESTS PASSED";
}

call debug.off();
```

## Test Assertion Functions

### 1. debug.assert(condition, message)

Tests a boolean condition and returns `true` if the condition is true, `false` otherwise.

**Parameters:**
- `condition` (bool): The boolean expression to test
- `message` (string, optional): Description of what is being tested

**Returns:** `bool` - `true` if assertion passes, `false` if it fails

**Example:**
```javascript
call debug.on();

// Test arithmetic
var sum = 10 + 5;
var result = call debug.assert(sum == 15, "Addition: 10 + 5 should equal 15");
// Result: true, prints "Assertion SUCCESS"

// Test string comparison
var name = "Alice";
var result2 = call debug.assert(name == "Bob", "Name should be Bob");
// Result: false, prints "Assertion FAILED: Name should be Bob"

call debug.off();
```

### 2. debug.assertEquals(expected, actual, message)

Compares two values for equality (deep comparison for objects/arrays) and returns `true` if they match.

**Parameters:**
- `expected` (any): The expected value
- `actual` (any): The actual value to compare
- `message` (string, optional): Description of what is being tested

**Returns:** `bool` - `true` if values are equal, `false` otherwise

**Example:**
```javascript
call debug.on();

// Test with primitives
var expected = 42;
var actual = 40 + 2;
var result = call debug.assertEquals(expected, actual, "Should equal 42");
// Result: true, prints "Assertion SUCCESS: | expected=42, actual=42"

// Test with strings
var result2 = call debug.assertEquals("hello", "world", "Strings should match");
// Result: false, prints "Assertion FAILED: Strings should match | expected=hello, actual=world"

// Test with JSON objects (deep comparison)
var obj1 = call json.jsonfromstring("{\"name\":\"test\",\"value\":123}");
var obj2 = call json.jsonfromstring("{\"name\":\"test\",\"value\":123}");
var result3 = call debug.assertEquals(obj1, obj2, "Objects should be equal");
// Result: true (deep equality check)

call debug.off();
```

## Test Organization Patterns

### Pattern 1: Simple Test Counter

```javascript
call debug.on();

var passed: int = 0;
var failed: int = 0;

// Test 1
if call debug.assert(10 > 5, "Test 1: Greater than") then {
    passed = passed + 1;
} else {
    failed = failed + 1;
}

// Test 2
if call debug.assert(5 < 10, "Test 2: Less than") then {
    passed = passed + 1;
} else {
    failed = failed + 1;
}

print "Passed: " + passed + ", Failed: " + failed;
call debug.off();
```

### Pattern 2: Test Function Wrapper

```javascript
call debug.on();

var totalTests: int = 0;
var passedTests: int = 0;
var failedTests: int = 0;

runTest(condition: bool, description: string) {
    totalTests = totalTests + 1;
    if call debug.assert(condition, description) then {
        passedTests = passedTests + 1;
    } else {
        failedTests = failedTests + 1;
    }
}

// Run tests
call runTest(10 + 5 == 15, "Addition test");
call runTest(10 - 5 == 5, "Subtraction test");
call runTest(10 * 5 == 50, "Multiplication test");

print "Results: " + passedTests + "/" + totalTests + " passed";
call debug.off();
```

### Pattern 3: Test Suite with Sections

```javascript
call debug.on();

var total: int = 0;
var passed: int = 0;
var failed: int = 0;

print "======================================";
print "Test Suite: String Operations";
print "======================================";

// String Tests
print "\n--- String Conversion Tests ---";
total = total + 1;
if call debug.assertEquals("HELLO", call string.toupper("hello"), "toUpper test") then {
    passed = passed + 1;
} else {
    failed = failed + 1;
}

total = total + 1;
if call debug.assertEquals("hello", call string.tolower("HELLO"), "toLower test") then {
    passed = passed + 1;
} else {
    failed = failed + 1;
}

print "\n--- String Query Tests ---";
total = total + 1;
if call debug.assert(call string.contains("hello world", "world"), "contains test") then {
    passed = passed + 1;
} else {
    failed = failed + 1;
}

// Summary
print "\n======================================";
print "Total: " + total + ", Passed: " + passed + ", Failed: " + failed;
print "======================================";

call debug.off();
```

## Testing Different Language Features

### Testing Variables and Types

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// Integer
tests = tests + 1;
var x: int = 42;
if call debug.assertEquals(42, x, "Integer variable") then { passed = passed + 1; }

// String
tests = tests + 1;
var name: string = "Alice";
if call debug.assertEquals("Alice", name, "String variable") then { passed = passed + 1; }

// Boolean
tests = tests + 1;
var flag: bool = true;
if call debug.assert(flag, "Boolean variable") then { passed = passed + 1; }

// Type conversion
tests = tests + 1;
var num: int = "42";
if call debug.assertEquals(42, num, "String to int conversion") then { passed = passed + 1; }

print "Variable Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing Operators

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// Arithmetic
tests = tests + 1;
if call debug.assertEquals(15, 10 + 5, "Addition") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assertEquals(5, 10 - 5, "Subtraction") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assertEquals(50, 10 * 5, "Multiplication") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assertEquals(2, 10 / 5, "Division") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assertEquals(8, 2 ^ 3, "Exponentiation") then { passed = passed + 1; }

// Comparison
tests = tests + 1;
if call debug.assert(10 > 5, "Greater than") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assert(5 < 10, "Less than") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assert(10 == 10, "Equality") then { passed = passed + 1; }

tests = tests + 1;
if call debug.assert(10 != 5, "Inequality") then { passed = passed + 1; }

print "Operator Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing String Functions

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// toUpper
tests = tests + 1;
if call debug.assertEquals("HELLO", call string.toupper("hello"), "toUpper") then {
    passed = passed + 1;
}

// toLower
tests = tests + 1;
if call debug.assertEquals("world", call string.tolower("WORLD"), "toLower") then {
    passed = passed + 1;
}

// trim
tests = tests + 1;
if call debug.assertEquals("text", call string.trim("  text  "), "trim") then {
    passed = passed + 1;
}

// contains
tests = tests + 1;
if call debug.assert(call string.contains("hello world", "world"), "contains") then {
    passed = passed + 1;
}

// startsWith
tests = tests + 1;
if call debug.assert(call string.startswith("hello", "hel"), "startsWith") then {
    passed = passed + 1;
}

// endsWith
tests = tests + 1;
if call debug.assert(call string.endswith("hello", "lo"), "endsWith") then {
    passed = passed + 1;
}

print "String Function Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing JSON Functions

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// Parse JSON
tests = tests + 1;
var jsonStr = "{\"name\":\"Alice\",\"age\":30}";
var obj = call json.jsonfromstring(jsonStr);
if call debug.assert(obj != null, "JSON parsing") then {
    passed = passed + 1;
}

// Get string value
tests = tests + 1;
var name = call json.getstring(obj, "name", "");
if call debug.assertEquals("Alice", name, "JSON getString") then {
    passed = passed + 1;
}

// Get int value
tests = tests + 1;
var age = call json.getint(obj, "age", 0);
if call debug.assertEquals(30, age, "JSON getInt") then {
    passed = passed + 1;
}

// Set value
tests = tests + 1;
call json.set(obj, "city", "Boston");
var city = call json.getstring(obj, "city", "");
if call debug.assertEquals("Boston", city, "JSON set") then {
    passed = passed + 1;
}

// Remove value
tests = tests + 1;
call json.remove(obj, "age");
var removedAge = call json.getint(obj, "age", -1);
if call debug.assertEquals(-1, removedAge, "JSON remove") then {
    passed = passed + 1;
}

print "JSON Function Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing Arrays

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// Array creation
tests = tests + 1;
var numbers: int[5];
if call debug.assertEquals(5, numbers.length, "Array length") then {
    passed = passed + 1;
}

// Array assignment
tests = tests + 1;
numbers[0] = 10;
numbers[1] = 20;
if call debug.assertEquals(10, numbers[0], "Array element assignment") then {
    passed = passed + 1;
}

// Array literal
tests = tests + 1;
var items = [1, 2, 3, 4, 5];
if call debug.assertEquals(5, items.length, "Array literal length") then {
    passed = passed + 1;
}

// Array sorting
tests = tests + 1;
var unsorted = [5, 2, 8, 1, 9];
call array.sort(unsorted, true);
if call debug.assertEquals(1, unsorted[0], "Array sort - first element") then {
    passed = passed + 1;
}

print "Array Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing Control Flow

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// If-then-else
tests = tests + 1;
var x = 10;
var result = "";
if x > 5 then {
    result = "greater";
} else {
    result = "lesser";
}
if call debug.assertEquals("greater", result, "If-then-else") then {
    passed = passed + 1;
}

// While loop
tests = tests + 1;
var count = 0;
var i = 0;
while i < 5 {
    count = count + 1;
    i = i + 1;
}
if call debug.assertEquals(5, count, "While loop") then {
    passed = passed + 1;
}

// Do-while loop
tests = tests + 1;
var doCount = 0;
var j = 0;
do {
    doCount = doCount + 1;
    j = j + 1;
} while (j < 3);
if call debug.assertEquals(3, doCount, "Do-while loop") then {
    passed = passed + 1;
}

print "Control Flow Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

### Testing Functions

```javascript
call debug.on();

var tests: int = 0;
var passed: int = 0;

// Function without parameters
tests = tests + 1;
getMessage return string {
    return "Hello, World!";
}
var msg = call getMessage();
if call debug.assertEquals("Hello, World!", msg, "Function without params") then {
    passed = passed + 1;
}

// Function with parameters
tests = tests + 1;
add(a: int, b: int) return int {
    return a + b;
}
var sum = call add(5, 3);
if call debug.assertEquals(8, sum, "Function with params") then {
    passed = passed + 1;
}

// Function with default parameters
tests = tests + 1;
greet(name: string = "Guest") return string {
    return "Hello, " + name;
}
var greeting1 = call greet();
var greeting2 = call greet("Alice");
if call debug.assertEquals("Hello, Guest", greeting1, "Default param") then {
    passed = passed + 1;
}
tests = tests + 1;
if call debug.assertEquals("Hello, Alice", greeting2, "Override default param") then {
    passed = passed + 1;
}

print "Function Tests: " + passed + "/" + tests + " passed";
call debug.off();
```

## Best Practices

### 1. Always Enable Debug Mode

```javascript
// At the start of your test script
call debug.on();

// Your tests here...

// At the end
call debug.off();
```

Without `debug.on()`, assertion messages won't be printed and you won't see which tests failed.

### 2. Use Descriptive Test Messages

```javascript
// ❌ Bad: Not descriptive
call debug.assert(x == 5, "Test 1");

// ✅ Good: Clear description
call debug.assert(x == 5, "Variable x should equal 5 after initialization");
```

### 3. Track All Test Results

```javascript
var totalTests: int = 0;
var passedTests: int = 0;
var failedTests: int = 0;

// For each test, increment counters
totalTests = totalTests + 1;
if call debug.assert(condition, "Test description") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}
```

### 4. Group Related Tests

```javascript
print "========================================";
print "String Function Tests";
print "========================================";

// Test toupper, tolower, trim, etc.

print "\n========================================";
print "JSON Function Tests";
print "========================================";

// Test JSON functions
```

### 5. Print Summary at the End

```javascript
print "\n========================================";
print "Test Results Summary";
print "========================================";
print "Total Tests:  " + totalTests;
print "Passed:       " + passedTests;
print "Failed:       " + failedTests;
if failedTests == 0 then {
    print "Status:       ✅ ALL PASSED";
} else {
    print "Status:       ❌ SOME FAILED";
}
print "========================================";
```

### 6. Test Edge Cases

```javascript
// Test empty strings
call debug.assert(call string.isempty(""), "Empty string check");

// Test null values
var nullVar = null;
call debug.assert(nullVar == null, "Null comparison");

// Test boundary values
var maxInt = 2147483647;
call debug.assert(maxInt > 0, "Max integer boundary");
```

### 7. Use assertEquals for Complex Types

```javascript
// For JSON objects, use assertEquals for deep comparison
var obj1 = call json.jsonfromstring("{\"a\":1,\"b\":2}");
var obj2 = call json.jsonfromstring("{\"a\":1,\"b\":2}");
call debug.assertEquals(obj1, obj2, "Deep object comparison");
```

## Complete Example Test Script

```javascript
// ============================================
// EBS Test Suite: Core Language Features
// Purpose: Comprehensive test of variables, 
//          operators, strings, and JSON
// ============================================

call debug.on();

var totalTests: int = 0;
var passedTests: int = 0;
var failedTests: int = 0;

print "========================================";
print "Starting EBS Test Suite";
print "========================================";

// ============================================
// Test Section: Variables and Types
// ============================================
print "\n--- Testing Variables and Types ---";

totalTests = totalTests + 1;
var intVar: int = 42;
if call debug.assertEquals(42, intVar, "Integer variable initialization") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
var strVar: string = "Hello";
if call debug.assertEquals("Hello", strVar, "String variable initialization") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
var boolVar: bool = true;
if call debug.assert(boolVar, "Boolean variable initialization") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// ============================================
// Test Section: Arithmetic Operators
// ============================================
print "\n--- Testing Arithmetic Operators ---";

totalTests = totalTests + 1;
if call debug.assertEquals(15, 10 + 5, "Addition operator") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
if call debug.assertEquals(5, 10 - 5, "Subtraction operator") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
if call debug.assertEquals(50, 10 * 5, "Multiplication operator") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
if call debug.assertEquals(2, 10 / 5, "Division operator") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// ============================================
// Test Section: String Functions
// ============================================
print "\n--- Testing String Functions ---";

totalTests = totalTests + 1;
if call debug.assertEquals("HELLO", call string.toupper("hello"), "string.toupper") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
if call debug.assertEquals("world", call string.tolower("WORLD"), "string.tolower") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
if call debug.assertEquals("test", call string.trim("  test  "), "string.trim") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// ============================================
// Test Section: JSON Functions
// ============================================
print "\n--- Testing JSON Functions ---";

totalTests = totalTests + 1;
var jsonStr = "{\"name\":\"Alice\",\"age\":30}";
var jsonObj = call json.jsonfromstring(jsonStr);
if call debug.assert(jsonObj != null, "json.jsonfromstring") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
var name = call json.getstring(jsonObj, "name", "");
if call debug.assertEquals("Alice", name, "json.getstring") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

totalTests = totalTests + 1;
var age = call json.getint(jsonObj, "age", 0);
if call debug.assertEquals(30, age, "json.getint") then {
    passedTests = passedTests + 1;
} else {
    failedTests = failedTests + 1;
}

// ============================================
// Test Summary
// ============================================
print "\n========================================";
print "Test Results Summary";
print "========================================";
print "Total Tests:  " + totalTests;
print "Passed:       " + passedTests;
print "Failed:       " + failedTests;
print "Success Rate: " + ((passedTests * 100) / totalTests) + "%";
print "========================================";

if failedTests > 0 then {
    print "Status: ❌ " + failedTests + " TEST(S) FAILED";
} else {
    print "Status: ✅ ALL TESTS PASSED";
}

call debug.off();
```

## Running Test Scripts

### Command Line

```bash
# From the ScriptInterpreter directory
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="path/to/test_script.ebs"
```

### Interactive Console

1. Open the EBS interactive console
2. Use `/open path/to/test_script.ebs` to load the test
3. Press `Ctrl+Enter` to execute

## Output Example

When running a test script, you'll see output like:

```
========================================
Starting EBS Test Suite
========================================

--- Testing Variables and Types ---
[ASSERT] Assertion SUCCESS
[ASSERT] Assertion SUCCESS
[ASSERT] Assertion SUCCESS

--- Testing Arithmetic Operators ---
[ASSERT] Assertion SUCCESS: | expected=15, actual=15
[ASSERT] Assertion SUCCESS: | expected=5, actual=5
[ASSERT] Assertion SUCCESS: | expected=50, actual=50
[ASSERT] Assertion SUCCESS: | expected=2, actual=2

--- Testing String Functions ---
[ASSERT] Assertion SUCCESS: | expected=HELLO, actual=HELLO
[ASSERT] Assertion SUCCESS: | expected=world, actual=world
[ASSERT] Assertion SUCCESS: | expected=test, actual=test

--- Testing JSON Functions ---
[ASSERT] Assertion SUCCESS
[ASSERT] Assertion SUCCESS: | expected=Alice, actual=Alice
[ASSERT] Assertion SUCCESS: | expected=30, actual=30

========================================
Test Results Summary
========================================
Total Tests:  13
Passed:       13
Failed:       0
Success Rate: 100%
========================================
Status: ✅ ALL TESTS PASSED
```

## Tips for Debugging Failed Tests

1. **Check assertion messages**: The debug output shows exactly what was expected vs. actual
2. **Run debug.on() early**: Ensures all assertion messages are printed
3. **Test incrementally**: Add one test at a time to isolate issues
4. **Use debug.log()**: Add custom debug messages for additional context
   ```javascript
   call debug.log("INFO", "About to test feature X with value: " + value);
   ```
5. **Use debug.vars()**: Inspect all variables at a point in time
   ```javascript
   var allVars = call debug.vars();
   print allVars;
   ```

## Additional Resources

- **EBS_SCRIPT_SYNTAX.md**: Complete language syntax reference
- **syntax_ebnf.txt**: Formal grammar specification
- **ScriptInterpreter/scripts/**: Example test scripts in the repository
