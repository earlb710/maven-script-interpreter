# Sorted Map Implementation Summary

## Overview
This document summarizes the implementation of sorted map functionality in the EBS scripting language. The feature allows users to declare maps that automatically maintain alphabetical order of their keys using Java's TreeMap, while regular maps continue to use LinkedHashMap to preserve insertion order.

## Implementation Details

### 1. Lexer Changes
**File:** `ScriptInterpreter/src/main/java/com/eb/script/token/ebs/EbsTokenType.java`

Added the `SORTED` keyword as a type modifier:
```java
SORTED(PrintStyle.KEYWORD, Category.KEYWORD, "sorted"),
```

### 2. AST Changes
**File:** `ScriptInterpreter/src/main/java/com/eb/script/interpreter/statement/VarStatement.java`

Added `isSortedMap` field to track sorted map declarations:
```java
public final boolean isSortedMap; // Whether this is a sorted map (uses TreeMap instead of LinkedHashMap)
```

Updated all constructors to accept the `isSortedMap` parameter, with a default value of `false` for backward compatibility.

### 3. Parser Changes
**File:** `ScriptInterpreter/src/main/java/com/eb/script/parser/Parser.java`

Modified `varDeclaration()` method to recognize "sorted map" syntax:
- Checks for SORTED token followed by MAP token after the colon in type annotations
- Sets `isSortedMap` flag to true when parsed
- Passes the flag through to VarStatement constructor

### 4. Interpreter Changes
**File:** `ScriptInterpreter/src/main/java/com/eb/script/interpreter/Interpreter.java`

Modified `visitVarStatement()` method to handle sorted and normal maps differently:
- For sorted maps: Creates `java.util.TreeMap` which maintains keys in alphabetical order
- For normal maps: Creates `java.util.LinkedHashMap` which maintains insertion order
- Handles conversion of existing map values to the appropriate type

### 5. Builtin Functions
**File:** `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java`

Added two new builtin functions for map conversion:

1. **`map.toSorted(map)`** - Converts a normal map to a sorted map (TreeMap)
2. **`map.toUnsorted(map)`** - Converts a sorted map to a normal map (LinkedHashMap)

Implemented `dispatchMapBuiltin()` method to handle these functions.

### 6. Documentation Updates
**Files:**
- `docs/EBS_COLLECTIONS_REFERENCE.md`
- `docs/EBS_SCRIPT_SYNTAX.md`

Added comprehensive documentation including:
- Explanation of normal vs sorted maps
- Declaration syntax examples
- Use cases for each type
- Conversion function documentation
- Table of map builtin functions

### 7. Examples and Tests
**Created Files:**
- `ScriptInterpreter/scripts/examples/sorted_map.ebs` - Basic sorted map usage
- `ScriptInterpreter/scripts/examples/map_comparison.ebs` - Comparison between map types
- `ScriptInterpreter/scripts/test/test_sorted_map.ebs` - Test script for sorted maps
- `ScriptInterpreter/src/test/java/com/eb/script/test/TestSortedMap.java` - Java parser test

All parsing tests pass successfully.

## Usage Examples

### Basic Declaration
```javascript
// Normal map (maintains insertion order)
var config: map = {"z": 1, "a": 2, "m": 3};
// Iteration order: z, a, m

// Sorted map (maintains alphabetical order)
var sortedConfig: sorted map = {"z": 1, "a": 2, "m": 3};
// Iteration order: a, m, z
```

### Conversion Between Types
```javascript
var normalMap: map = {"z": 26, "a": 1, "m": 13};
var sortedMap: sorted map = {"z": 26, "a": 1, "m": 13};

// Convert normal to sorted
var toSorted = call map.toSorted(normalMap);

// Convert sorted to normal
var toNormal = call map.toUnsorted(sortedMap);
```

### Empty Maps
```javascript
var emptyNormal: map = {};
var emptySorted: sorted map = {};
```

### Const Maps
```javascript
const constNormal: map = {"key": "value"};
const constSorted: sorted map = {"key": "value"};
```

## Use Cases

### Normal Maps (LinkedHashMap)
- Sequential processing where insertion order matters
- User-defined ordering
- FIFO-style access patterns
- Maintaining insertion history

### Sorted Maps (TreeMap)
- Configuration files (easier to compare and diff)
- Consistent API responses (predictable JSON structure)
- Alphabetically sorted displays
- Deterministic iteration for reproducible results
- Logging and debugging (consistent output)

## Backward Compatibility
All changes are fully backward compatible:
- Existing `map` type declarations continue to work unchanged
- No changes to existing map operations or builtin functions
- Default behavior remains the same (LinkedHashMap for normal maps)
- Only explicit `sorted map` declarations use TreeMap

## Testing
- Parser tests verify correct parsing of sorted map syntax
- Example scripts demonstrate practical usage
- Manual testing confirms TreeMap vs LinkedHashMap behavior
- Code review and security scan completed successfully

## Technical Notes
1. Map keys are always strings (as before)
2. Map values can be any type (as before)
3. Sorted maps use natural string ordering (alphabetical)
4. Conversion functions create new map instances (non-destructive)
5. All map operations (json.get, json.set, etc.) work identically on both types

## Files Modified
1. `ScriptInterpreter/src/main/java/com/eb/script/token/ebs/EbsTokenType.java`
2. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/statement/VarStatement.java`
3. `ScriptInterpreter/src/main/java/com/eb/script/parser/Parser.java`
4. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/Interpreter.java`
5. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java`
6. `docs/EBS_COLLECTIONS_REFERENCE.md`
7. `docs/EBS_SCRIPT_SYNTAX.md`

## Files Created
1. `ScriptInterpreter/scripts/examples/sorted_map.ebs`
2. `ScriptInterpreter/scripts/examples/map_comparison.ebs`
3. `ScriptInterpreter/scripts/test/test_sorted_map.ebs`
4. `ScriptInterpreter/src/test/java/com/eb/script/test/TestSortedMap.java`
5. `SORTED_MAP_IMPLEMENTATION.md` (this file)
