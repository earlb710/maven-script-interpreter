# Custom Function Syntax Highlighting Feature

## Overview
The EBS script editor now provides intelligent syntax highlighting for custom (user-defined) functions with automatic validation against known builtins and imported functions.

## Features

### 1. Custom Function Recognition
- Functions defined in the current file are automatically detected
- Functions from imported files are recursively collected
- Function definitions are recognized in multiple formats:
  - `functionName(params) { ... }`
  - `functionName(params) return type { ... }`
  - `function functionName(params) { ... }`

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
2. **Function Collection**: Extracts all function definitions (name and parameters)
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

## Example

### test_function_highlighting.ebs
```javascript
// Define custom function
myFunction(x: int, y: int) return int {
    return x + y;
}

// Known function call - highlighted normally
var result: int = call myFunction(10, 20);

// Unknown function call - red underline
var bad: int = call unknownFunction(1, 2);
```

### test_import_highlighting.ebs
```javascript
import "imported_functions.ebs";

// Function from import - highlighted normally
var result: int = call importedAdd(10, 20);

// Unknown function - red underline
var bad: int = call notDefined();
```

## Implementation Details

### Key Classes Modified
- **EbsTab.java**: Main editor tab component
  - `extractFunctionNames()`: Parses function definitions
  - `extractImportedFunctions()`: Recursively processes imports
  - `updateKnownFunctions()`: Maintains set of known functions
  - `markUnknownFunctions()`: Post-processes tokens for validation

- **console.css**: Styling
  - Added `tok-function-error` style class

### Performance
- Function extraction runs on each editor change (debounced 100ms)
- Import files are only read when changed
- Regex-based parsing for efficiency

## Benefits

1. **Immediate Feedback**: Typos in function names are caught immediately
2. **Import Validation**: Ensures imported functions exist
3. **Better Code Navigation**: Easily distinguish custom vs builtin functions
4. **Refactoring Support**: Renaming functions shows errors instantly

## Future Enhancements
- Cache imported file parsing results
- Show function signature on hover
- Jump to function definition
- Auto-complete for custom functions
