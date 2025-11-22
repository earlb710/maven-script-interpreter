# Test Document for New String Builtin Functions

## Overview
This directory contains comprehensive test documentation for the 7 new string builtin functions added to the EBS (Earl Bosch Script) language.

## Test File
**File:** `TEST_STRING_FUNCTIONS.ebs`

This test document validates all functionality of the new string manipulation functions and demonstrates their practical usage.

## Functions Tested

### 1. str.substring(str, beginIndex, endIndex?)
- **Purpose:** Extract substring from a string
- **Parameters:**
  - `str` (string): Source string
  - `beginIndex` (int): Starting index (inclusive)
  - `endIndex` (int, optional): Ending index (exclusive)
- **Returns:** string
- **Tests:** Extraction from start, to end, and middle sections

### 2. str.indexOf(str, searchString, fromIndex?)
- **Purpose:** Find first occurrence of substring
- **Parameters:**
  - `str` (string): String to search in
  - `searchString` (string): String to search for
  - `fromIndex` (int, optional): Position to start searching from
- **Returns:** int (index or -1 if not found)
- **Tests:** Basic search, search from position, not found cases

### 3. str.lastIndexOf(str, searchString, fromIndex?)
- **Purpose:** Find last occurrence of substring
- **Parameters:**
  - `str` (string): String to search in
  - `searchString` (string): String to search for
  - `fromIndex` (int, optional): Position to search backwards from
- **Returns:** int (index or -1 if not found)
- **Tests:** Reverse search, character search, not found cases

### 4. str.charAt(str, index)
- **Purpose:** Get character at specific index
- **Parameters:**
  - `str` (string): Source string
  - `index` (int): Character position (0-based)
- **Returns:** string (single character)
- **Tests:** First char, last char, space character

### 5. str.replaceAll(str, regex, replacement)
- **Purpose:** Replace all occurrences using regex patterns
- **Parameters:**
  - `str` (string): Source string
  - `regex` (string): Regular expression pattern
  - `replacement` (string): Replacement text
- **Returns:** string
- **Tests:** Simple replacement, digit patterns, whitespace normalization

### 6. str.lpad(str, length, padChar)
- **Purpose:** Left-pad string to specified length
- **Parameters:**
  - `str` (string): Source string
  - `length` (int): Desired total length
  - `padChar` (string): Single character to pad with
- **Returns:** string
- **Tests:** Padding with zeros, spaces, various lengths

### 7. str.rpad(str, length, padChar)
- **Purpose:** Right-pad string to specified length
- **Parameters:**
  - `str` (string): Source string
  - `length` (int): Desired total length
  - `padChar` (string): Single character to pad with
- **Returns:** string
- **Tests:** Padding with zeros, spaces, various lengths

## Test Features

### Help Documentation Display
Each function test includes a call to `system.help(functionName)` which displays:
- Function signature with parameter types
- Short description
- Detailed explanation
- Example usage with expected outputs

### Test Cases
For each function, the test document includes:
- Multiple test scenarios covering typical use cases
- Edge cases and boundary conditions
- Assertions to validate expected behavior
- Clear output showing inputs, results, and expectations

### Integration Tests
The document also includes integration tests demonstrating:
- **Email Parsing:** Using indexOf, substring, and lastIndexOf together to parse email addresses
- **Table Formatting:** Using lpad and rpad to create aligned column displays

## Running the Tests

### Command Line
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/TEST_STRING_FUNCTIONS.ebs"
```

### Expected Output
The test document produces formatted output showing:
1. **Test Headers:** Clear section markers for each function
2. **Help Documentation:** Full help text for each function
3. **Test Results:** Pass/fail indicators for each test case
4. **Integration Examples:** Practical demonstrations
5. **Summary:** Overall test results

### Success Criteria
All tests should pass with output showing:
- ✓ marks for each passed test section
- Correct values matching expected outputs
- No error messages or exceptions
- Complete integration test results

## Test Coverage

### Functionality Coverage
- ✅ Basic operations for all 7 functions
- ✅ Optional parameters (endIndex, fromIndex)
- ✅ Edge cases (empty strings, not found, boundaries)
- ✅ Error conditions (null inputs, invalid indices)
- ✅ Combined usage scenarios

### Documentation Coverage
- ✅ Help text display for all functions
- ✅ Parameter descriptions
- ✅ Return type information
- ✅ Usage examples with expected outputs

## Implementation Details

### Java API Equivalence
All functions mirror their Java String API counterparts:
- `str.substring` → `String.substring()`
- `str.indexOf` → `String.indexOf()`
- `str.lastIndexOf` → `String.lastIndexOf()`
- `str.charAt` → `String.charAt()`
- `str.replaceAll` → `String.replaceAll()`
- `str.lpad` → Custom implementation
- `str.rpad` → Custom implementation

### Error Handling
- Null-safe: Functions handle null inputs appropriately
- IndexOutOfBoundsException wrapped in InterpreterError
- PatternSyntaxException for invalid regex patterns
- Validation for padding character (must be single char)

## Use Cases

### Text Processing
- Parsing structured data (emails, URLs, paths)
- Extracting substrings based on delimiters
- Finding and replacing patterns

### Formatting
- Aligning text in columns
- Creating formatted tables
- Padding numbers with leading zeros
- Building visual layouts and progress bars

### String Analysis
- Finding first and last occurrences
- Character-level access
- Pattern matching with regex

## Related Documentation
- `system-lookup.json`: JSON help entries for all functions
- `Builtins.java`: Implementation source code
- `EBS_SCRIPT_SYNTAX.md`: Language syntax reference

## Notes
- The test uses `debug.assertEquals()` for validation
- All test assertions must pass for success
- Help documentation is pulled from `system-lookup.json`
- Test demonstrates both individual and combined function usage
