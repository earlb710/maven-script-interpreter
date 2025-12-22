# Custom Function Highlighting - Visual Guide

This document provides a visual explanation of how the custom function highlighting feature works in the EBS script editor.

## Color Coding Reference

| Function Type | Color | CSS Class | Description |
|--------------|-------|-----------|-------------|
| Built-in Functions | Yellow (#DCDCAA) | `tok-builtin` | Functions provided by the EBS runtime (thread.*, string.*, json.*, etc.) |
| Custom Functions | Orange (#FFB86C) | `tok-custom-function` | User-defined functions in the current script |
| Unknown Functions | White (default) | default | Function calls that may be imported from other files |
| Keywords | Cyan (#00FFFF) | `tok-keyword` | Language keywords (if, while, print, etc.) |
| Normal Text | White (#FFFFFF) | `info` | Default text color |

**Note**: Functions that are not found in the current file are shown with default styling (white) since they may be imported from other files via `import` statements. Only builtins and locally-defined functions are highlighted in color.

## Example with Highlighting

```ebs
// Define a custom function
myHelper(x int) return int {     // myHelper: white (definition, not a call)
    return x * 2
}

// Call the custom function
var result int = myHelper(5)     // myHelper: ORANGE (custom function call)
print result                      // print: CYAN (keyword)

// Use hash call syntax
#myHelper                         // myHelper: ORANGE (custom function call via #)

// Call a built-in function
var upper string = string.toUpper("test")  // string.toUpper: YELLOW (built-in)
var data json = json.parse("{}")           // json.parse: YELLOW (built-in)
thread.timerStop("myTimer")                // thread.timerStop: YELLOW (built-in)

// Call a function that may be imported
importedFunc()                    // importedFunc: WHITE (could be from import)
#anotherImported                  // anotherImported: WHITE (could be from import)
```

## Supported Built-in Prefixes

The highlighting recognizes these built-in function prefixes:
- `thread.*` - Threading and timer functions
- `string.*` / `str.*` - String manipulation
- `array.*` - Array operations
- `json.*` - JSON parsing and manipulation
- `file.*` - File I/O operations
- `http.*` - HTTP requests
- `ftp.*` - FTP operations
- `mail.*` - Email operations
- `date.*` - Date/time functions
- `system.*` / `sys.*` - System operations
- `random.*` - Random number generation
- `canvas.*`, `draw.*`, `effect.*`, `style.*`, `transform.*`, `vector.*`, `image.*` - Graphics
- `map.*`, `queue.*` - Data structures
- `crypto.*`, `css.*` - Utilities
- `custom.*`, `ai.*`, `timer.*`, `debug.*`, `echo.*`, `plugin.*`, `classtree.*`, `scr.*` - Misc

## How the Highlighting Updates

1. **As You Type**: The highlighting updates automatically with a 100ms debounce
2. **After Defining a Function**: Once you complete typing a function definition (e.g., `functionName() {`), subsequent calls to that function will be highlighted in orange
3. **Built-in Detection**: Built-in functions are always recognized by their prefix (e.g., `thread.`, `string.`)

## Example Workflow

### Step 1: Start writing code
```ebs
myFunc()  // WHITE (not yet defined, could be imported)
```

### Step 2: Define the function
```ebs
myFunc() {  // Definition complete!
    print "hello"
}

myFunc()  // Now ORANGE (custom function)
```

## Pattern Detection

The feature detects function definitions in these formats:

1. **Simple function**:
   ```ebs
   functionName() {
       // code
   }
   ```

2. **Function with parameters**:
   ```ebs
   functionName(param1 int, param2 string) {
       // code
   }
   ```

3. **Function with return type**:
   ```ebs
   functionName(param int) return string {
       return "result"
   }
   ```

4. **Function keyword syntax**:
   ```ebs
   function functionName {
       // code
   }
   ```

## Benefits Visualization

### Before Custom Highlighting:
- All function calls looked the same (white)
- No way to distinguish custom vs built-in functions
- No indication of which functions are defined locally

### After Custom Highlighting:
- ✅ **Custom functions** stand out in orange (defined in current file)
- ✅ **Built-in functions** are clearly yellow (EBS runtime)
- ✅ **Unknown functions** remain white (may be imported)
- ✅ Better code readability

## Technical Details

- **Case Insensitive**: `myFunc`, `MyFunc`, `MYFUNC` all refer to the same function
- **Scope**: Only functions defined in the current file are highlighted as custom (orange)
- **Imports**: Functions imported from other files are shown with default styling
- **Performance**: Minimal overhead - uses efficient regex patterns with debouncing
- **Keywords Excluded**: Language keywords like `if`, `while`, `print` cannot be used as function names
