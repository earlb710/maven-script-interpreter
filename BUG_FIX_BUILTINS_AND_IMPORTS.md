# Bug Fix: Builtins and Imported Functions Incorrectly Marked as Undefined

## Problems Reported
1. Custom functions like `call isCheckmate` were marked as red with underline
2. Builtins like `thread.timerStop` were marked as red with underline

## Root Causes

### Problem 1: Incomplete Builtin Pattern
The BUILTIN regex pattern only matched three prefixes:
```java
String BUILTIN = "\\b(?:(?:json|file|http)\\.[A-Za-z_][A-Za-z0-9_]*)\\b";
```

However, the EBS runtime has 30+ builtin prefixes including:
- `thread.*` (threading and timers)
- `string.*` / `str.*` (string operations)
- `array.*` (array operations)
- `canvas.*`, `draw.*`, `effect.*`, `style.*`, `transform.*`, `vector.*`, `image.*` (graphics)
- `map.*`, `queue.*` (data structures)
- `date.*`, `system.*`, `random.*` (utilities)
- And many more...

**Result**: Functions like `thread.timerStop` were not recognized as builtins and fell through to the "undefined" case.

### Problem 2: Import Statement Not Tracked
The highlighting only looks at function definitions in the current file. It doesn't:
- Parse `import` statements
- Load imported files
- Track functions from imported modules

Example from `chess-game.ebs`:
```ebs
import "chess-moves.ebs";  // This is not tracked

// Later in the file:
var result = call isCheckmate(player);  // isCheckmate is defined in chess-moves.ebs
```

The function `isCheckmate` is defined in `chess-moves.ebs` but the highlighter only sees the current file, so it was marked as undefined.

## Solutions

### Solution 1: Expanded Builtin Pattern
Updated the BUILTIN pattern to include all 30+ prefixes:
```java
String BUILTIN = "\\b(?:thread|string|array|json|file|http|ftp|mail|date|system|random|canvas|draw|effect|style|transform|vector|image|map|queue|crypto|css|custom|ai|timer|debug|echo|plugin|classtree|str|sys|scr)\\.[A-Za-z_][A-Za-z0-9_]*\\b";
```

**Result**: All builtins are now correctly recognized and highlighted in yellow.

### Solution 2: Removed "Undefined" Highlighting
Since the highlighter doesn't track imports, marking unknown functions as "undefined" (red with underline) creates false positives. 

**Changed behavior**:
- **Before**: Unknown functions → Red with underline (error)
- **After**: Unknown functions → Default white styling (neutral)

**Rationale**: 
- If a function is a builtin → Highlighted yellow
- If a function is defined in current file → Highlighted orange
- If a function is not found → Default styling (may be imported, no error indication)

This is a more conservative approach that avoids false error indicators while still providing helpful highlighting for known cases.

## Color Scheme Summary

| Function Type | Color | When Applied |
|--------------|-------|--------------|
| Built-in | Yellow (#DCDCAA) | Function matches builtin pattern (prefix.name) |
| Custom | Orange (#FFB86C) | Function defined in current file |
| Unknown | White (default) | Function not found (may be imported) |

## Examples

### Before Fix:
```ebs
thread.timerStop("timer1")    // ❌ RED (incorrectly marked as undefined)
call isCheckmate(player)      // ❌ RED (incorrectly marked as undefined)
```

### After Fix:
```ebs
thread.timerStop("timer1")    // ✅ YELLOW (correctly recognized as builtin)
call isCheckmate(player)      // ✅ WHITE (may be imported, no error)
```

## Commit
Fixed in commit: **72afb1c**

## Future Enhancement Possibility
To fully support import tracking, the highlighter would need to:
1. Parse import statements in the current file
2. Load and parse imported .ebs files
3. Extract function definitions from imported files
4. Merge imported functions with local functions

This would be a significant enhancement but is not implemented in the current version.
