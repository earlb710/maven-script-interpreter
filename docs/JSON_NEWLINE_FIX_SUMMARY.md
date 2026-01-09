# JSON Parser Newline Support - Implementation Summary

## Issue
The JSON parser (`Json.java`) was rejecting screen definitions with inline event handlers that contained actual newlines. Users would get an error: `"Unescaped control character in string"` when writing multi-line onClick, onChange, or onExpand handlers.

## Root Cause
The JSON specification (RFC 8259) requires control characters (0x00-0x1F) including newlines to be escaped in JSON strings. However, for usability in screen definitions with inline event code, allowing unescaped newlines makes the code more readable and maintainable.

## Solution
Modified the JSON parser to be more lenient while maintaining security:

### Changes to `Json.java`
**Location:** `ScriptInterpreter/src/main/java/com/eb/script/json/Json.java`

**Before:**
```java
if (c >= 0 && c < 0x20) {
    throw error("Unescaped control character in string");
}
```

**After:**
```java
// Allow newlines (LF=0x0A, CR=0x0D) and tab (0x09) for multi-line inline code in screen definitions
// Reject other control characters (0x00-0x08, 0x0B-0x0C, 0x0E-0x1F) for safety
if (c >= 0 && c < 0x20 && c != 0x0A && c != 0x0D && c != 0x09) {
    throw error("Unescaped control character in string (char code: 0x" + Integer.toHexString(c) + ")");
}
```

### What's Allowed
- **Newlines (LF)**: 0x0A
- **Carriage Returns (CR)**: 0x0D  
- **Tabs**: 0x09

### What's Still Rejected (for security)
All other control characters: 0x00-0x08, 0x0B-0x0C, 0x0E-0x1F

## Example Usage

### Before (Required Escaping)
```javascript
screen myScreen = {
    "area": [{
        "items": [{
            "display": {
                "onClick": "print \"Line 1\";\\nprint \"Line 2\";\\nprint \"Line 3\";"
            }
        }]
    }]
};
```

### After (Supports Actual Newlines)
```javascript
screen myScreen = {
    "area": [{
        "items": [{
            "display": {
                "onClick": "print \"Line 1\";
print \"Line 2\";
print \"Line 3\";"
            }
        }]
    }]
};
```

## Testing

### Unit Tests
Created `TestJsonMultiline.java` with comprehensive tests:
1. ✅ JSON with escaped newlines (backward compatibility)
2. ✅ JSON with actual embedded newlines (new feature)
3. ✅ Multiple lines in event handler code
4. ✅ Safety check: still rejects other control characters

### Integration Test
Created `test_multiline_event_handlers.ebs` demonstrating:
- Multi-line onClick handlers
- Multi-line onChange handlers
- Real-world screen definition that parses successfully

### Regression Testing
- All existing tests pass (0 failures)
- No impact on existing functionality

### Security Testing
- CodeQL security scan: 0 alerts
- Other control characters still properly rejected

## Benefits

1. **Improved Readability**: Multi-line event handlers are easier to read and maintain
2. **Better Developer Experience**: More natural way to write inline event code
3. **Backward Compatible**: Escaped newlines (\n) still work as before
4. **Security Maintained**: Other dangerous control characters are still rejected
5. **Consistent with EBS Lexer**: The EBS lexer already supports multi-line strings, now JSON parser does too

## Files Modified

1. `ScriptInterpreter/src/main/java/com/eb/script/json/Json.java` - Core fix
2. `ScriptInterpreter/src/test/java/com/eb/script/test/TestJsonMultiline.java` - Unit tests
3. `test_multiline_event_handlers.ebs` - Example/integration test

## Impact

- **Breaking Changes**: None
- **API Changes**: None
- **Performance Impact**: Negligible (one additional condition check per character)
- **Security Impact**: Positive (better error messages for rejected characters)

## Version Info

- **Implemented**: 2025-12-17
- **Java Version**: 21
- **Maven Version**: 3.x
