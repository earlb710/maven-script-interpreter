# Circular Import Detection Implementation

## Summary

This implementation adds circular import detection to the EBS scripting language's `import` statement. The feature prevents infinite recursion and stack overflow errors that would occur when files import each other in a circular manner.

## Problem Statement

Previously, the EBS import statement had no protection against circular imports. If file A imported file B, and file B imported file A, the interpreter would enter an infinite loop, eventually causing a stack overflow error.

## Solution

### Technical Approach

The solution uses an **import stack** to track which files are currently being imported. When a new import is encountered:

1. The file path is resolved and normalized to its canonical absolute path
2. The import stack is checked for the presence of this path
3. If found, a circular import is detected and an error is thrown with a helpful message
4. If not found, the path is pushed onto the stack, the file is imported, and then popped from the stack

### Key Implementation Details

#### 1. Import Stack in InterpreterContext
```java
private final Deque<String> importStack = new java.util.ArrayDeque<>();
```

- Uses a `Deque` (double-ended queue) as a stack for efficient push/pop operations
- Stored in `InterpreterContext` so it's available throughout the interpreter's execution
- Tracks normalized absolute paths of files currently being imported

#### 2. Path Normalization
```java
Path normalizedPath;
try {
    normalizedPath = importPath.toRealPath();
} catch (IOException e) {
    normalizedPath = importPath.toAbsolutePath().normalize();
}
```

- Uses `toRealPath()` to get the canonical absolute path, resolving symlinks
- Falls back to `toAbsolutePath().normalize()` if `toRealPath()` fails
- This ensures that different path representations (relative vs absolute, with vs without symlinks) are treated as the same file

#### 3. Circular Import Detection
```java
if (context.getImportStack().contains(importPathStr)) {
    // Build circular import chain message
    throw error(stmt.getLine(), "Circular import detected: " + chain);
}
```

- Simple contains check on the stack
- If found, generates a helpful error message showing the import chain
- The error message format: `Circular import detected: file_a.ebs -> file_b.ebs -> file_a.ebs`

#### 4. Stack Management
```java
context.getImportStack().push(importPathStr);
try {
    // Import the file
} finally {
    context.getImportStack().pop();
}
```

- Push before importing, pop after (in finally block)
- The finally block ensures the stack is cleaned up even if an error occurs during import
- This prevents stack pollution that could cause false positives

## Test Cases

### 1. Direct Circular Import (A→B→A)
- Files: `circular_direct_a.ebs`, `circular_direct_b.ebs`
- Test: `test_circular_direct.ebs`
- **Expected**: Error with message showing the circular chain

### 2. Indirect Circular Import (A→B→C→A)
- Files: `circular_a.ebs`, `circular_b.ebs`, `circular_c.ebs`
- Test: `test_circular.ebs`
- **Expected**: Error with message showing the full circular chain

### 3. Valid Import
- Files: `utils.ebs`
- Test: `test_valid_import.ebs`
- **Expected**: Successful execution, imported functions work correctly

### 4. Multiple Non-Circular Imports
- Files: `utils.ebs`
- Test: `test_multiple_import.ebs`
- **Expected**: Successful execution, same file imported twice without error

## Benefits

1. **Prevents Stack Overflow**: Circular imports no longer cause infinite recursion
2. **Clear Error Messages**: Users get helpful messages showing exactly which files form the circular dependency
3. **No False Positives**: Multiple imports of the same file (non-circular) are still allowed
4. **Robust Path Handling**: Different path representations are correctly identified as the same file
5. **Clean Error Recovery**: Import stack is properly cleaned up even when errors occur

## Edge Cases Handled

1. **Symbolic Links**: Resolved via `toRealPath()` so symlinks don't create false negatives
2. **Relative vs Absolute Paths**: Normalized to absolute paths for consistent comparison
3. **Import Failures**: Stack is cleaned up in finally block even if import throws an exception
4. **Multiple Imports**: Same file can be imported multiple times as long as not circular
5. **Deep Circular Chains**: Works correctly for any depth of circular dependency (A→B→C→...→A)

## Performance Impact

- **Minimal**: O(n) check where n is the current import depth (typically very small, < 10)
- **Memory**: O(n) where n is the maximum import depth
- The Deque operations (push/pop/contains) are all efficient

## Future Enhancements (Not Implemented)

1. **Import Caching**: Track already-imported files to avoid re-parsing (would need cache invalidation)
2. **Import Once Semantics**: Only import each file once per script execution
3. **Import Depth Limit**: Set a maximum import depth to prevent excessively deep import chains

## Files Modified

1. `InterpreterContext.java`
   - Added `importStack` field
   - Added `getImportStack()` method

2. `Interpreter.java`
   - Modified `visitImportStatement()` method to check for circular imports
   - Added path normalization
   - Added circular import detection logic
   - Added import stack management with proper cleanup

## Testing Recommendations

When testing this feature:
1. Try direct circular imports (A→B→A)
2. Try indirect circular imports (A→B→C→A)
3. Verify valid imports still work
4. Verify multiple imports of the same file work
5. Test with relative and absolute paths
6. Test with symlinks if applicable
7. Verify error messages are clear and helpful

## Compatibility

This change is **backward compatible**. All existing valid scripts will continue to work. Only scripts that previously caused infinite recursion (which would have failed anyway) will now fail with a clear error message instead.
