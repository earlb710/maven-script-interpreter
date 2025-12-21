# Custom Function Highlighting - Visual Guide

This document provides a visual explanation of how the custom function highlighting feature works in the EBS script editor.

## Color Coding Reference

| Function Type | Color | CSS Class | Description |
|--------------|-------|-----------|-------------|
| Built-in Functions | Yellow (#DCDCAA) | `tok-builtin` | Functions provided by the EBS runtime |
| Custom Functions | Orange (#FFB86C) | `tok-custom-function` | User-defined functions in the current script |
| Undefined Functions | Red (#FF5555) with underline | `tok-undefined-function` | Function calls with no definition (potential errors) |
| Keywords | Cyan (#00FFFF) | `tok-keyword` | Language keywords (if, while, print, etc.) |
| Normal Text | White (#FFFFFF) | `info` | Default text color |

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

// Call an undefined function
undefinedFunc()                   // undefinedFunc: RED with underline (ERROR!)
#anotherUndefined                 // anotherUndefined: RED with underline (ERROR!)
```

## How the Highlighting Updates

1. **As You Type**: The highlighting updates automatically with a 100ms debounce
2. **After Defining a Function**: Once you complete typing a function definition (e.g., `functionName() {`), subsequent calls to that function will be highlighted in orange
3. **Real-time Error Detection**: If you call a function before defining it, it will show as red/underlined until you add the definition

## Example Workflow

### Step 1: Start writing code
```ebs
myFunc()  // RED with underline (undefined)
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
- No indication of undefined function errors

### After Custom Highlighting:
- ✅ **Custom functions** stand out in orange
- ✅ **Built-in functions** are clearly yellow
- ✅ **Errors** are immediately visible in red with underline
- ✅ Better code readability and error detection

## Technical Details

- **Case Insensitive**: `myFunc`, `MyFunc`, `MYFUNC` all refer to the same function
- **Scope**: Only functions defined in the current file are tracked (imports not yet supported)
- **Performance**: Minimal overhead - uses efficient regex patterns with debouncing
- **Keywords Excluded**: Language keywords like `if`, `while`, `print` cannot be used as function names
