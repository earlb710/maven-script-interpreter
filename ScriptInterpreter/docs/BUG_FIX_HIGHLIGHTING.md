# Bug Fix: Custom Function Highlighting Not Working

## Problem
The custom function highlighting feature was not working at all - no functions were being highlighted in the colors specified (orange for custom, red for undefined).

## Root Cause
The issue was in the `computeEbsHighlighting()` method in `EbsTab.java`. The code was using numbered capture groups (`m.group(1)`) to extract function names from the regex matches:

```java
// BROKEN CODE
if (m.group("HASHCALL") != null) {
    String funcName = m.group(1);  // ❌ Incorrect!
    ...
}
```

The problem is that when using named groups in an alternation pattern like this:

```java
"(?<HASHCALL>#\\s*([A-Za-z_][A-Za-z0-9_]*))"
+ "|(?<FUNCTION>\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\())"
```

The numbered groups (`m.group(1)`, `m.group(2)`, etc.) don't correctly map to the capture groups within each named group. Instead, they map to all capture groups in the entire regex pattern sequentially.

## Solution
Extract the function name directly from the matched text of the named group using string manipulation:

```java
// FIXED CODE
if (m.group("HASHCALL") != null) {
    String matched = m.group("HASHCALL");
    // Extract function name after the # and any whitespace
    String funcName = matched.replaceFirst("^#\\s*", "");
    ...
}
else if (m.group("FUNCTION") != null) {
    String matched = m.group("FUNCTION");
    // Extract function name (everything before whitespace and opening paren)
    String funcName = matched.replaceFirst("\\s*\\($", "").trim();
    ...
}
```

This correctly extracts the function name from whatever pattern matched:
- For `HASHCALL` pattern: Remove `#` and whitespace from the beginning
- For `FUNCTION` pattern: Remove whitespace and opening parenthesis from the end

## Verification
Updated the test (`HighlightingTest.java`) to verify the fix:

```
Extracted custom functions:
  - anotherfunction
  - mycustomfunction
  - calculatesum

Matches found:
  - Function call: myCustomFunction -> funcName: myCustomFunction [CUSTOM]
  - Function call: calculateSum -> funcName: calculateSum [CUSTOM]
  - Function call: myCustomFunction -> funcName: myCustomFunction [CUSTOM]
  - Hash call: #myCustomFunction -> funcName: myCustomFunction [CUSTOM]
  - Function call: undefinedFunction -> funcName: undefinedFunction [UNDEFINED]
  - Hash call: #undefinedFunction -> funcName: undefinedFunction [UNDEFINED]
```

✅ Custom functions are correctly identified
✅ Undefined functions are correctly detected
✅ Both `functionName()` and `#functionName` syntax work

## Expected Behavior
After this fix, the editor will correctly highlight:

1. **Custom functions** (Orange #FFB86C): Functions defined in the current script
   - Example: `myHelper()`, `#calculateSum`

2. **Built-in functions** (Yellow #DCDCAA): EBS runtime functions
   - Example: `string.toUpper()`, `json.parse()`

3. **Undefined functions** (Red #FF5555 with underline): Function calls that don't exist
   - Example: `undefinedFunc()`, `#unknownFunction`

## Commit
Fixed in commit: **9040871**

## Files Changed
- `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsTab.java` - Fixed capture group extraction
- `ScriptInterpreter/src/test/java/com/eb/ui/ebs/HighlightingTest.java` - Updated test to match new logic
