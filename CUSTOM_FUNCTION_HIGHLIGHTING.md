# Custom Function Highlighting Feature

## Overview

The EBS script editor now provides intelligent syntax highlighting that distinguishes between built-in functions, user-defined custom functions, and undefined function calls.

## Feature Details

### Three Types of Function Highlighting

1. **Built-in Functions** (Yellow: #DCDCAA)
   - Functions provided by the EBS runtime
   - Examples: `json.parse()`, `string.toUpper()`, `file.open()`, `array.push()`
   - Styling: Yellow color (tok-builtin style)

2. **Custom User Functions** (Orange: #FFB86C)
   - Functions defined within the current script
   - Detected from function definitions in the code
   - Styling: Orange color (tok-custom-function style)

3. **Undefined Functions** (Red: #FF5555 with underline)
   - Function calls that are neither built-in nor defined in the script
   - Indicates potential errors or typos
   - Styling: Red color with red underline (tok-undefined-function style)

### Function Call Syntax Support

The highlighting works for both EBS function call syntaxes:

1. **Regular calls**: `functionName(arguments)`
2. **Hash calls**: `#functionName` or `# functionName`

### How It Works

1. **Function Detection**: When highlighting code, the editor first scans the entire text to identify all custom function definitions using pattern matching:
   - `functionName() { ... }`
   - `functionName(params) return type { ... }`
   - `function functionName { ... }`

2. **Function Classification**: When a function call is encountered:
   - Check if it's a built-in function (from Builtins registry)
   - Check if it's a custom function (found in the current script)
   - Otherwise, mark as undefined (potential error)

3. **Real-time Updates**: Highlighting updates automatically as you type (with 100ms debounce)

## Example

```ebs
// Define custom functions - these will be tracked
myCustomFunction() {
    print "Hello from myCustomFunction"
}

calculateSum(a int, b int) return int {
    return a + b
}

function anotherFunction {
    print "This is another function"
}

// Custom function calls - highlighted in ORANGE
myCustomFunction()
#myCustomFunction
var result int = calculateSum(5, 10)

// Built-in function calls - highlighted in YELLOW
print "Using builtin: " + string.toUpper("test")
json.parse("{\"key\": \"value\"}")

// Undefined function calls - highlighted in RED with underline (ERROR)
undefinedFunction()
#undefinedFunction
```

## Implementation Details

### Files Modified

1. **console.css**: Added three CSS styles for function highlighting
   - `.tok-custom-function`: Orange color for custom functions
   - `.tok-undefined-function`: Red color with underline for errors

2. **EbsTab.java**: Enhanced highlighting logic
   - `extractCustomFunctions()`: Parses function definitions from text
   - `isKeywordOrType()`: Filters keywords/types from function names
   - `computeEbsHighlighting()`: Applies appropriate styles based on function type
   - Updated regex pattern to capture hash calls (`#functionName`)

3. **HighlightingTest.java**: Test class to validate the logic

### Technical Notes

- Function names are case-insensitive (EBS language convention)
- Keywords and types are excluded from being recognized as function names
- The feature uses regex pattern matching for efficiency
- No runtime overhead - all detection happens during syntax highlighting

## Benefits

1. **Better Code Readability**: Quickly distinguish between different types of functions
2. **Error Detection**: Immediately spot typos or calls to undefined functions
3. **Code Navigation**: Easily identify where custom functions are used
4. **Learning Aid**: Helps users understand which functions are built-in vs custom

## Future Enhancements (Potential)

- Jump to function definition on click
- Show function signature in tooltip on hover
- Highlight function definitions differently from calls
- Support for imported functions from other modules
