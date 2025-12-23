# Custom Function Syntax Highlighting Feature

## Overview
The EBS script editor now provides intelligent syntax highlighting for custom (user-defined) functions with automatic validation against known builtins and imported functions.

## Features

### 1. Custom Function Recognition
- Functions defined in the current file are automatically detected
- Functions from imported files are recursively collected
- Function definitions are recognized in **all three formats per EBS syntax**:
  - **namedBlock**: `functionName { ... }`
  - **namedBlockWithReturn**: `functionName return type { ... }`
  - **namedBlockWithParams**: `functionName(params) [return type] { ... }`
  - All formats support optional `function` keyword: `function functionName ...`

### 2. Syntax Highlighting
- **Known Functions**: Custom functions and builtins are highlighted in yellow (`tok-function` style)
- **Unknown Functions**: Function calls that cannot be resolved show a red underline (`tok-function-error` style)
- Real-time updates as you type or edit imports

### 3. Import Processing
- Import statements are parsed to identify imported files
- Function names from imported files are automatically collected
- Supports recursive imports (imports within imports)
- Relative paths are resolved from the current file's directory

## How It Works

1. **On File Load/Edit**: The editor parses the current file and all imported files
2. **Function Collection**: Extracts all function definitions using three patterns:
   - Pattern 1: `identifier(` - Functions with parameters
   - Pattern 2: `identifier return` - Functions with return type only
   - Pattern 3: `identifier {` - Basic functions without parameters
3. **Token Processing**: During syntax highlighting, identifies function calls (identifier followed by `(`)
4. **Validation**: Checks if each function call matches:
   - A builtin function (from `Builtins.java`)
   - A custom function (defined in current file or imports)
   - A language keyword
5. **Styling**: Applies appropriate CSS class based on validation result

## CSS Styles

### Known Functions
```css
.tok-function {
    -fx-fill: #DCDCAA; /* yellow */
}
```

### Unknown Functions (Error)
```css
.tok-function-error {
    -fx-fill: #DCDCAA; /* same yellow */
    -rtfx-underline-color: red;
    -rtfx-underline-width: 2;
    -rtfx-underline-dash-array: none; /* solid line */
}
```

## Examples

### All Function Definition Formats

```javascript
// Format 1: Basic function (namedBlock)
greet {
    print("Hello!");
}

// Format 2: Function with return type (namedBlockWithReturn)
getValue return int {
    return 42;
}

// Format 3: Function with parameters (namedBlockWithParams)
add(a: int, b: int) return int {
    return a + b;
}

// Format 4: With optional 'function' keyword
function multiply(x: int, y: int) return int {
    return x * y;
}
```

### Function Call Highlighting

```javascript
// Known function calls - highlighted normally
call greet();              // ✅ Yellow
var x = call getValue();   // ✅ Yellow
var sum = call add(5, 3);  // ✅ Yellow

// Unknown function call - red underline
var bad = call unknownFunc(1); // ⚠️ Yellow + Red underline
```

### Import Example

```javascript
// custom-lib.ebs
helper return string {
    return "I'm a helper";
}

// main.ebs
import "custom-lib.ebs";

call helper();        // ✅ Highlighted normally
call notDefined();    // ⚠️ Red underline
```

## Implementation Details

### Key Classes Modified
- **EbsTab.java**: Main editor tab component
  - `extractFunctionNames()`: Parses all three function definition formats
  - `extractImportedFunctions()`: Recursively processes imports
  - `updateKnownFunctions()`: Maintains set of known functions
  - `markUnknownFunctions()`: Post-processes tokens for validation

- **console.css**: Styling
  - Added `tok-function-error` style class

### Performance
- Function extraction runs on each editor change (debounced 100ms)
- Three regex patterns for comprehensive function detection
- Import files are only read when changed
- Set-based lookup for O(1) validation

## Benefits

1. **Immediate Feedback**: Typos in function names are caught immediately
2. **Import Validation**: Ensures imported functions exist
3. **Better Code Navigation**: Easily distinguish custom vs builtin functions
4. **Syntax Compliance**: Correctly handles all EBS function definition formats per `syntax_ebnf.txt`

## Alignment with EBS Syntax Guide

This implementation follows the official EBS syntax as defined in:
- `ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt`
- `docs/EBS_SCRIPT_SYNTAX.md`

All three function definition formats are supported:
```
namedBlock           = identifier "{" ... "}"
namedBlockWithReturn = identifier "return" typeName "{" ... "}"
namedBlockWithParams = identifier "(" ... ")" ["return" typeName] "{" ... "}"
```

## Future Enhancements
- Cache imported file parsing results
- Show function signature on hover
- Jump to function definition
- Auto-complete for custom functions

