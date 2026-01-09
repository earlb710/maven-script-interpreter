# While Loop Condition Parsing Fix

## Issue

EBS scripts with complex while loop conditions containing parentheses and logical operators were failing to parse. For example:

```javascript
while (currentX != toX || currentY != toY) && stepCount < maxSteps {
    // loop body
}
```

This would result in the error:
```
Parse error at BOOL_AND (&&): Expected '{' after while condition.
```

## Root Cause

The `whileStatement()` method in `Parser.java` incorrectly handled parenthesized expressions by:

1. Detecting an opening `(` and treating it as a condition wrapper
2. Parsing the expression inside the parentheses
3. Consuming the closing `)`
4. Attempting to find `{` to start the loop body

This approach failed when the actual condition extended beyond the parentheses, such as:
```javascript
(currentX != toX || currentY != toY) && stepCount < maxSteps
```

The parser would stop at the `)`, leaving `&& stepCount < maxSteps` unparsed, and then encounter `&&` when expecting `{`.

## Solution

Applied the same fix that was already implemented for `ifStatement()`:

**Before:**
```java
Expression condition;
if (match(EbsTokenType.LPAREN)) {
    condition = expression();
    consume(EbsTokenType.RPAREN, "Expected ')' after while condition.");
} else {
    condition = expression();
    consumeOptional(EbsTokenType.THEN);
}
```

**After:**
```java
// Parse the full expression, letting the expression parser handle parentheses naturally
Expression condition = expression();

// Optionally consume 'then' keyword if present
consumeOptional(EbsTokenType.THEN);
```

This allows the expression parser to handle parentheses as part of operator precedence, correctly parsing complex conditions.

## What Now Works

The fix enables all of these condition patterns:

1. **Complex parenthesized expressions with logical operators:**
   ```javascript
   while (x != 0 || y != 0) && count < max { ... }
   ```

2. **Multiple parenthesized groups:**
   ```javascript
   while (x < 10 && count < 5) || (y > 5 && count < 3) { ... }
   ```

3. **Simple conditions (no regression):**
   ```javascript
   while count < 10 { ... }
   ```

4. **Conditions with optional 'then' keyword:**
   ```javascript
   while count < 10 then { ... }
   ```

5. **Simple parenthesized conditions:**
   ```javascript
   while (x < 10) { ... }
   ```

6. **Complex nested parentheses:**
   ```javascript
   while ((a == 1 || b == 2) && c < 5) || (a != 0) { ... }
   ```

## Testing

### Test Suite
A comprehensive test suite was added: `TestWhileConditionParsing.java` with 6 test cases covering all the scenarios above.

### Test Results
```
=== Testing While Loop Condition Parsing ===

Test 1: Parenthesized OR condition with AND operator
✓ Test 1 passed - complex condition with parentheses parses correctly

Test 2: Multiple parenthesized groups with OR
✓ Test 2 passed - multiple parenthesized groups parse correctly

Test 3: Simple condition without parentheses
✓ Test 3 passed - simple condition without parentheses still works

Test 4: Condition with optional 'then' keyword
✓ Test 4 passed - 'then' keyword still works

Test 5: Simple parenthesized condition
✓ Test 5 passed - simple parenthesized condition works

Test 6: Complex nested parentheses
✓ Test 6 passed - complex nested parentheses work

=== Test Summary ===
Passed: 6
Failed: 0
```

### Example Script
An example script demonstrating the fix is available at: `test_while_condition_fix.ebs`

## Related Code

### Other Loop Statements
- **for statement**: Uses required parentheses correctly: `for (init; condition; increment)`. No change needed.
- **foreach statement**: Has optional parentheses for structure, not condition. No change needed.
- **do-while statement**: Requires parentheses correctly: `do {...} while (condition);`. No change needed.

### If Statement
The `ifStatement()` method was already fixed with this same approach, which inspired this fix.

## Code Review and Security

- ✅ Code review: No issues found
- ✅ CodeQL security scan: No vulnerabilities detected
- ✅ Build: Successful
- ✅ All tests: Passing

## Files Changed

1. `ScriptInterpreter/src/main/java/com/eb/script/parser/Parser.java`
   - Updated `whileStatement()` method
   - Added explanatory comments

2. `ScriptInterpreter/src/test/java/com/eb/script/test/TestWhileConditionParsing.java`
   - New comprehensive test suite

3. `test_while_condition_fix.ebs`
   - Example script demonstrating the fix

## Version

This fix is included in the next version after 1.0.8.12.
