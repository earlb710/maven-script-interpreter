# Circular Import Prevention Implementation

## Summary

This implementation adds circular import prevention to the EBS scripting language's `import` statement using a global import cache. Each file is imported only once per interpreter session, which prevents infinite recursion, stack overflow errors, and unnecessary re-execution of imported code.

## Problem Statement

Previously, the EBS import statement had no protection against circular imports. If file A imported file B, and file B imported file A, the interpreter would enter an infinite loop, eventually causing a stack overflow error. Additionally, files could be imported and executed multiple times unnecessarily.

## Solution

### Technical Approach

The solution uses a **global import cache** to track which files have been imported. When a new import is encountered:

1. The file path is resolved and normalized to its canonical absolute path
2. The import cache is checked for the presence of this path
3. If found, the import is skipped (file already loaded)
4. If not found, the path is added to the cache and the file is imported

### Key Implementation Details

#### 1. Import Cache in InterpreterContext
```java
private final Set<String> importedFiles = ConcurrentHashMap.newKeySet();
```

- Uses a thread-safe `Set` to store normalized absolute paths of all imported files
- Stored in `InterpreterContext` so it's available throughout the interpreter's execution
- Global for the entire interpreter session - files are only imported once

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

#### 3. Import Cache Check
```java
if (context.getImportedFiles().contains(importPathStr)) {
    // File already imported, skip re-importing
    return;
}

// Add to cache before processing
context.getImportedFiles().add(importPathStr);
```

- Simple contains check on the cache set
- If found, the import is silently skipped with an optional message
- If not found, the file is added to the cache immediately (before processing) to prevent circular imports
- No stack management needed - files stay in the cache for the entire session

## Test Cases

### 1. Direct Circular Import (A→B→A)
- Files: `circular_direct_a.ebs`, `circular_direct_b.ebs`
- Test: `test_circular_direct.ebs`
- **Expected**: No error, A imports B, B tries to import A but A is already cached so it's skipped

### 2. Indirect Circular Import (A→B→C→A)
- Files: `circular_a.ebs`, `circular_b.ebs`, `circular_c.ebs`
- Test: `test_circular.ebs`
- **Expected**: No error, A imports B, B imports C, C tries to import A but A is already cached so it's skipped

### 3. Valid Import
- Files: `utils.ebs`
- Test: `test_valid_import.ebs`
- **Expected**: Successful execution, imported functions work correctly

### 4. Multiple Imports (Same File)
- Files: `utils.ebs`
- Test: `test_multiple_import.ebs`
- **Expected**: First import succeeds, second import is skipped (already cached)

## Benefits

1. **Prevents Stack Overflow**: Circular imports no longer cause infinite recursion
2. **Prevents Duplicate Execution**: Each file is only loaded and executed once
3. **Automatic Circular Prevention**: Circular imports are naturally prevented by the cache
4. **Efficient**: Simple set lookup, no stack management needed
5. **Robust Path Handling**: Different path representations are correctly identified as the same file
6. **Thread-Safe**: Uses ConcurrentHashMap.newKeySet() for thread safety

## Edge Cases Handled

1. **Symbolic Links**: Resolved via `toRealPath()` so symlinks don't create false negatives
2. **Relative vs Absolute Paths**: Normalized to absolute paths for consistent comparison
3. **Import Failures**: File added to cache before processing to prevent partial imports
4. **Multiple Import Attempts**: Subsequent imports of the same file are silently skipped
5. **Deep Circular Chains**: Works correctly for any depth of circular dependency (A→B→C→...→A)

## Performance Impact

- **Minimal**: O(1) set lookup for each import
- **Memory**: O(n) where n is the number of unique imported files (typically small)
- **Thread-Safe**: ConcurrentHashMap.newKeySet() allows safe concurrent access

## Implementation Approach

This cache-based approach differs from a stack-based detection approach:

**Stack-Based (alternative approach):**
- Tracks currently importing files
- Allows same file to be imported multiple times
- Requires push/pop management and cleanup
- Provides detailed circular import error messages

**Cache-Based (current implementation):**
- Tracks all imported files globally
- Each file imported only once (like Python's import)
- Simpler implementation - no stack management
- Circular imports prevented automatically

## Files Modified

1. `InterpreterContext.java`
   - Added `importedFiles` field (Set<String>)
   - Added `getImportedFiles()` method

2. `Interpreter.java`
   - Modified `visitImportStatement()` method to check import cache
   - Added path normalization
   - Import skipped if file already in cache
   - File added to cache before processing to prevent circular imports

## Testing Recommendations

When testing this feature:
1. Try direct circular imports (A→B→A) - should work, B's import of A is skipped
2. Try indirect circular imports (A→B→C→A) - should work, C's import of A is skipped
3. Verify valid imports still work
4. Verify multiple imports of the same file result in only one execution
5. Test with relative and absolute paths
6. Test with symlinks if applicable
7. Verify that imported functions/blocks remain accessible

## Compatibility

This change **alters behavior** from any previous implementations:
- **Before**: Same file could potentially be imported multiple times
- **After**: Each file is imported only once per interpreter session

This is similar to how most programming languages handle imports (Python, Java, etc.) and is the expected behavior. Scripts that relied on re-importing files to re-execute code will need to be updated to use function calls instead.
