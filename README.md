# Maven Script Interpreter

A powerful script interpreter for the EBS (Earl Bosch Script) language, featuring a rich JavaFX-based interactive development environment with syntax highlighting, autocomplete, and integrated database support.

## Features

- **Custom Scripting Language**: Full-featured scripting language with familiar syntax
- **Type Casting**: Explicit type conversions with `int()`, `string()`, `float()`, `double()`, `byte()`, `long()`, `boolean()`, `record()`, `map()`
- **Bitmap Type**: Define named bit fields within a byte (8-bit) for compact storage of flags and small values
- **Intmap Type**: Define named bit fields within an integer (32-bit) for storing larger bit-packed data structures
- **typeof Operator**: Get the type of any variable or expression at runtime (e.g., `typeof a` returns `"string"`)
- **Exception Handling**: Robust error handling with `try-exceptions-when` syntax to catch specific or any errors
- **Interactive Console**: JavaFX-based IDE with rich text editing
- **Syntax Highlighting**: Color-coded syntax for better readability
- **Autocomplete**: Intelligent code completion suggestions for keywords, built-ins, and JSON schemas
- **Configurable Colors**: Customize console colors via JSON configuration file
- **Database Integration**: Built-in SQL support with cursors and connections
- **Type Aliases**: Define reusable type aliases for complex types with the `typeof` keyword
- **Array Support**: Multi-dimensional arrays with type safety
- **JSON Support**: Native JSON parsing with support for both standard quoted keys and JavaScript-style unquoted keys, validation, and schema support
- **File I/O**: Built-in file operations
- **Extensible**: Easy to add custom built-in functions

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.x
- JavaFX 21

### Building the Project

```bash
cd ScriptInterpreter
mvn clean compile
```

### Running the Application

#### Interactive Console (JavaFX UI)

```bash
mvn javafx:run
```

#### Command-Line Script Execution

```bash
java -cp target/classes com.eb.script.Run <script-file.ebs>
```

### Console Configuration

You can customize the console colors by creating a `console.cfg` file in the root directory. The file uses JSON format to define color properties for different text styles.

Example `console.cfg`:
```json
{
  "colors": {
    "info": "#e6e6e6",
    "error": "#ee0000",
    "warn": "#eeee00",
    "ok": "#00ee00",
    "keyword": "#00FFFF",
    "background": "#000000",
    "text": "#e6e6e6"
  }
}
```

See [CONSOLE_CONFIG_GUIDE.md](CONSOLE_CONFIG_GUIDE.md) for complete configuration options and examples.

## Organizing Screen Applications

**Best Practice for Screen Apps with Custom CSS:**

When creating applications with screens and custom styling, organize your files in a dedicated directory:

```
my-screen-app/
├── my-app.ebs          # Main EBS script with screen definitions
├── custom-theme.css    # Custom CSS for your screens
└── README.md           # Optional documentation
```

This organization:
- Keeps related files together (easier to manage and deploy)
- Enables relative CSS paths from the EBS script location
- Simplifies version control and sharing
- Provides clear separation between different screen applications

**Example:**
```javascript
// In my-screen-app/my-app.ebs
screen myScreen = { "title": "My App" };
show screen myScreen;

// Load CSS from same directory using relative path
call css.loadCss("myscreen", "custom-theme.css");
```

See `ScriptInterpreter/scripts/examples/css-screen-demo/` for a complete working example.

## Language Overview

### Basic Syntax

```javascript
// Variable declaration
var name: string = "World";
var count: int = 42;
var items: int[5];  // Fixed-size array
var data: json = {"key": "value"};  // Standard JSON with quoted keys
var config: json = {theme: "dark", volume: 80};  // JavaScript-style unquoted keys (NEW)
var settings: map = {"theme": "dark", "volume": 80};  // Map type

// Control flow
if count > 40 then {
    print "Count is large";
}

// Loops
while count > 0 {
    print count;
    count = count - 1;
}

// Functions
greet(name: string) return string {
    return "Hello, " + name + "!";
}

// Function calls
var message = call greet("World");
print message;

// Arrays
var numbers = [1, 2, 3, 4, 5];
print numbers[0];  // Access elements
print numbers.length;  // Get length

// Type aliases (typeof keyword)
personType typeof record{name: string, age: int};
var person: personType;
person = {"name": "Alice", "age": 30};  // JSON syntax
print person;  // Print the record

// Record literal syntax (cleaner initialization)
posType typeof record { x: int, y: int };
var pos = posType { x: 10, y: 20 };  // No quotes on field names
print pos.x;  // Output: 10

// Database operations
connect db = "jdbc:oracle:thin:@localhost:1521:xe";
use db {
    cursor myCursor = select * from users where age > 18;
    open myCursor();
    
    while call myCursor.hasNext() {
        var row = call myCursor.next();
        print row.name;
    }
    
    close myCursor;
}
close connection db;
```

> **Note on JSON Syntax**: The EBS JSON parser supports both standard JSON with quoted keys (`{"name": "value"}`) and JavaScript-style unquoted keys (`{name: "value"}`). Both formats can be used interchangeably or mixed in the same document. Unquoted keys must be valid identifiers (letters, digits, underscore) and cannot start with a digit. Keys with special characters (hyphens, spaces, etc.) must be quoted.

### Type Casting

The language supports explicit type casting for all data types. Use the format `type(value)` to cast values:

```javascript
// String to number conversions
var strNum: string = "42";
var intVal: int = int(strNum);
var floatVal: float = float("3.14");
var longVal: long = long("999999999");

// Number to string conversions
var numStr: string = string(123);

// Between numeric types
var wholeNum: int = int(3.14);  // 3 (truncation)
var decimalNum: double = double(42);  // 42.0

// Boolean conversions
var flag: bool = boolean("true");
var flagStr: string = string(false);

// Byte conversions
var byteVal: byte = byte(65);

// Casting in expressions
var str1: string = "10";
var str2: string = "20";
var sum: int = int(str1) + int(str2);  // 30

// Casting in conditionals
if int("15") > 10 then {
    print "Condition met";
}
```

**Supported cast types:**
- `int()` or `integer()` - Convert to integer
- `long()` - Convert to long integer
- `float()` - Convert to floating point
- `double()` - Convert to double precision
- `string()` - Convert to string
- `byte()` - Convert to byte
- `boolean()` or `bool()` - Convert to boolean
- `record()` - Cast JSON object to record with type inference
- `map()` - Cast JSON object to map (key-value store)

See `scripts/test/test_type_casting.ebs` for more examples.

### typeof Operator

The `typeof` operator returns the type of a variable or expression as a string:

```javascript
// Simple types
var a: string = "xyz";
print typeof a;  // Output: string

var num: int = 42;
print typeof num;  // Output: int

// Record types - shows full structure
var b: record {x: string, y: int};
b = {"x": "hello", "y": 42};
print typeof b;  // Output: record {x:string, y:int}

// Works with all types
var flag: bool = true;
print typeof flag;  // Output: bool

// Can be used in conditionals
if typeof a == "string" then {
    print "a is a string";
}

// Works with cast expressions
print typeof int("42");  // Output: int
```

**Supported for all types:**
- Primitive types: `string`, `int`, `long`, `float`, `double`, `bool`, `byte`
- Complex types: `array`, `json`, `map`, `date`, `record`
- For records, displays the complete structure with field names and types

See `scripts/test/test_typeof_operator.ebs` for more examples.

### Exception Handling

The language supports robust error handling with `try-exceptions-when` syntax. This allows you to catch specific types of errors or use `ANY_ERROR` to catch all errors:

```javascript
// Basic exception handling
try {
    var result = 10 / 0;  // Will throw MATH_ERROR
} exceptions {
    when MATH_ERROR {
        print "Cannot divide by zero!";
    }
    when ANY_ERROR {
        print "An unexpected error occurred";
    }
}

// Capture error message in a variable
try {
    var data = #file.read("missing.txt");
} exceptions {
    when IO_ERROR(msg) {
        print "File error: " + msg;
    }
}

// Multiple specific handlers
try {
    // Some risky operation
    var result = processData();
} exceptions {
    when DB_ERROR {
        print "Database error occurred";
    }
    when TYPE_ERROR {
        print "Type conversion failed";
    }
    when ANY_ERROR(errorMsg) {
        print "Unexpected error: " + errorMsg;
    }
}
```

**Available Error Types:**
- `ANY_ERROR` - Catches any error (catch-all handler, should typically be last)
- `IO_ERROR` - File I/O operations, streams, paths
- `DB_ERROR` - Database connection and query errors
- `TYPE_ERROR` - Type conversion and casting errors
- `NULL_ERROR` - Null pointer or null value errors
- `INDEX_ERROR` - Array index out of bounds errors
- `MATH_ERROR` - Division by zero, arithmetic errors
- `PARSE_ERROR` - JSON parsing, date parsing errors
- `NETWORK_ERROR` - HTTP and network connection errors
- `NOT_FOUND_ERROR` - Variable or function not found errors
- `ACCESS_ERROR` - Permission or access denied errors
- `VALIDATION_ERROR` - Validation errors

**Key Features:**
- Error handlers are checked in order - the first matching handler is executed
- Use `when ERROR_TYPE(varName)` to capture the error message in a variable
- Multiple handlers can be specified for different error types
- `ANY_ERROR` should typically be placed last as it catches all errors
- Try blocks can be nested for granular error handling

### Raising Exceptions

You can explicitly raise exceptions using the `raise exception` statement. This allows you to signal errors from your own code.

**Standard Exceptions** (from the ErrorType enum) only accept a single message parameter:

```javascript
// Raise standard exceptions with a message
raise exception IO_ERROR("File not found: config.txt");
raise exception VALIDATION_ERROR("Input must be a positive number");
raise exception MATH_ERROR("Cannot calculate square root of negative number");

// Raise without a message (uses default message)
raise exception NULL_ERROR();
```

**Custom Exceptions** can have multiple parameters and are identified by any name not in the standard ErrorType list:

```javascript
// Raise custom exceptions with multiple parameters
raise exception ValidationFailed("username", "must be at least 3 characters");
raise exception OutOfBoundsError(10, 0, 5);  // index, min, max
raise exception BusinessRuleViolation("order", 12345, "insufficient inventory");
```

**Catching Raised Exceptions:**

```javascript
// Catch standard exceptions
try {
    raise exception VALIDATION_ERROR("Invalid input");
} exceptions {
    when VALIDATION_ERROR(msg) {
        print "Validation failed: " + msg;
    }
}

// Catch custom exceptions by name
try {
    raise exception MyCustomError("error details", 42);
} exceptions {
    when MyCustomError(msg) {
        print "Custom error: " + msg;
    }
    when ANY_ERROR(msg) {
        print "Caught by ANY_ERROR: " + msg;
    }
}
```

**Note:** Both standard and custom exception names are matched case-insensitively. Any unrecognized exception type (not in the standard ErrorType list) is treated as a custom exception.

### Map Type

The `map` type provides a flexible key-value store where keys are strings and values can be any type. Maps are backed by JSON objects and are ideal for dynamic data storage.

```javascript
// Declare a map variable
var config: map = {"host": "localhost", "port": 8080, "debug": true};

// Cast JSON to map
var jsonData: json = {"name": "Alice", "age": 30, "city": "NYC"};
var userMap = map(jsonData);

// Access values using json functions
var name = call json.get(userMap, "name");       // "Alice"
var age = call json.getint(userMap, "age");      // 30

// Modify and add values
call json.set(userMap, "city", "Los Angeles");   // Update existing
call json.set(userMap, "country", "USA");        // Add new key

// Nested maps
var nested: json = {"user": {"name": "Bob", "settings": {"theme": "dark"}}};
var nestedMap = map(nested);
```

**Map vs Record vs JSON:**
| Type | Use Case |
|------|----------|
| `map` | Flexible key-value store, no schema required |
| `record` | Type-safe access with predefined field names and types |
| `json` | Any JSON structure including arrays, objects, and primitives |

See `scripts/test/test_map_type.ebs` for more examples.

### String Functions

The language provides comprehensive string manipulation functions. A useful function is `str.charArray()` which returns character codes:

```javascript
// Get character codes from a string
var text: string = "Hello";
var codes = call str.charArray(text);
print codes;  // Output: [72, 101, 108, 108, 111]

// Access individual character codes
print codes[0];  // Output: 72 (character 'H')
print codes[1];  // Output: 101 (character 'e')

// Use with string length
print codes.length;  // Output: 5

// Check ASCII values
var letter: string = "A";
var letterCodes = call str.charArray(letter);
print letterCodes[0];  // Output: 65 (ASCII code for 'A')
```

**Use cases for `str.charArray()`:**
- Character analysis and validation
- Custom encoding/decoding operations
- Character-by-character processing
- ASCII/Unicode value checking
- String transformation algorithms

See `scripts/test/test_str_chararray.ebs` for more examples.

### UI Screens

Create interactive UI windows with thread-safe variables:

```javascript
// Define a screen with variables
screen myWindow = {
    "title": "My Application",
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "counter",
            "type": "int",
            "default": 0
        },
        {
            "name": "message",
            "type": "string",
            "default": "Hello"
        },
        {
            "name": "user",
            "type": "record",
            "default": {"name": "John", "email": "john@example.com"}
        },
        {
            "name": "employees",
            "type": "array.record",
            "default": {"id": 0, "name": "", "salary": 0.0}  // Template for new records
        }
    ]
};

// Access screen variables using screen_name.var_name syntax
var currentCount = myWindow.counter;
print currentCount;  // Prints: 0

// Assign to screen variables
myWindow.counter = 42;
myWindow.message = "Updated!";

// Access record fields
print myWindow.user.name;   // Prints: John
print myWindow.user.email;  // Prints: john@example.com

// Update record values
myWindow.user = {"name": "Jane", "email": "jane@example.com"};

// Array.record variables - default is a template for new records
print myWindow.employees;  // Initially empty: []

// Assign array of records
myWindow.employees = [
    {"id": 1, "name": "Alice", "salary": 75000},
    {"id": 2, "name": "Bob", "salary": 65000}
];

// Access array elements
print myWindow.employees[0].name;  // Prints: Alice
print myWindow.employees.length;   // Prints: 2

// Show the screen (new syntax)
show screen myWindow;

// Hide the screen (new syntax)
hide screen myWindow;

// Show it again
show screen myWindow;
```

**Screen Features:**
- Screens are created but NOT shown automatically - use `show screen <name>;` to display
- Each screen runs in its own dedicated thread
- The thread automatically stops when the screen is closed
- All screen variables are thread-safe (using ConcurrentHashMap)
- Access variables via `screen_name.var_name` syntax (e.g., `myWindow.counter`)
- Assign to variables via `screen_name.var_name = value` (e.g., `myWindow.counter = 10`)
- Variable assignments do not trigger screen display
- Multiple screens can be created and managed independently
- **Supported Variable Types**: `int`, `long`, `float`, `double`, `string`, `bool`, `byte`, `date`, `json`, `record`, and `array.record`
  - Record types allow storing structured data with multiple fields
  - Access record fields: `screenName.varName.fieldName`
  - **Array.record types** store arrays of records with a template for new entries
    - Default value is a single record (template), not an array
    - Variable initializes as empty array and can be populated with records
    - Example: `"type": "array.record", "default": {"id": 0, "name": ""}`
- **Case-Insensitive JSON Keys**: Screen definition JSON uses case-insensitive key lookup
  - Property names like `varRef`, `VarRef`, or `varref` are all treated identically
  - Keys are normalized to lowercase (`promptHelp` → `prompttext`)
  - String values preserve their original casing
- **Variable References in JSON**: Use `$variable` (without quotes) to reference script variables in JSON
  - Example: `"default": $myVar` references the script variable `myVar`
  - With quotes (`"$myVar"`), it's treated as a literal string
  - See [EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md) for details

### Dynamic Screen Values with `$variable`

You can use `$variable` references (without quotes) to create dynamic screens:

```javascript
// Define script variables
var userName: string = "Alice";
var userAge: int = 30;
var windowTitle: string = "User Profile";

// Use $variable references in screen definition (no quotes)
screen profileScreen = {
    "title": $windowTitle,        // Dynamic title
    "width": 800,
    "height": 600,
    "vars": [{
        "name": "name",
        "type": "string",
        "default": $userName,     // References userName variable
        "display": {
            "type": "textfield",
            "labelText": "Name:"
        }
    }, {
        "name": "age",
        "type": "int",
        "default": $userAge,      // References userAge variable
        "display": {
            "type": "spinner",
            "labelText": "Age:"
        }
    }]
};

show screen profileScreen;
```

**Important:** 
- Use `$variable` **without quotes** to reference variables
- With quotes (`"$variable"`), it's a literal string, not a reference
- Variables must exist in scope when the screen is defined
- Works with any data type (string, int, bool, arrays, JSON objects, etc.)

### Console Commands

When using the interactive console:

- `/open <file>` - Load a script from file
- `/save <file>` - Save the current script
- `/help` - Display help information
- `/clear` - Clear console output
- `/echo on|off` - Toggle statement echo mode
- `/ai setup` - Configure AI integration
- `/safe-dirs` - Configure trusted directories

Press `Ctrl+Enter` to execute the script in the editor.
Press `Ctrl+Space` to trigger autocomplete for keywords, built-ins, and JSON properties.

### Autocomplete Features

The interactive console provides intelligent autocomplete support (triggered with `Ctrl+Space`):

#### EBS Code Autocomplete
- **Keywords**: Language keywords (`if`, `then`, `while`, `for`, `function`, `return`, etc.)
- **Built-in Functions**: Over 50 built-in functions like `print`, `substring`, `parseJson`, etc.
- **Context-Aware**: Shows only relevant suggestions based on context (e.g., built-ins after `call` or `#`)
- **Console Commands**: Autocomplete for `/` commands when typing in console

#### JSON Schema Autocomplete
When editing JSON content (screen definitions, area definitions, display metadata), autocomplete provides:

- **Property Names**: Suggests valid property names from JSON schemas
  - Type `{` then `Ctrl+Space` to see all available properties
  - Start typing a property name for filtered suggestions (e.g., type `"na` to see `name`)
  
- **Enum Values**: Suggests valid enum values for properties with constrained values
  - After `"type": "`, press `Ctrl+Space` to see all valid control types (textfield, button, label, etc.)
  - After `"alignment": "`, see valid alignment options (left, center, right, etc.)

**Example:**
```json
{
  "name": "LoginScreen",
  "type": "   ← Ctrl+Space here shows: textfield, button, label, etc.
  "title":    ← Ctrl+Space after opening quote shows: property suggestions
}
```

**Supported JSON Schemas:**
- `screen-definition.json` - Top-level screen properties (name, title, width, height, vars, area)
- `area-definition.json` - Container properties (type, layout, items, style)
- `display-metadata.json` - UI control metadata (type, mandatory, alignment, min, max, etc.)

The autocomplete automatically detects JSON content and provides schema-aware suggestions from all three schemas combined.

### Screen Debug Panel (Ctrl+D)

When working with EBS screens, you can toggle a debug panel by pressing `Ctrl+D` on any screen window. The debug panel appears on the right side and provides real-time information about the screen state:

#### Panel Layout
The debug panel is divided into two sections with an adjustable divider:

**Top Half: 📊 Variables**
- Lists all screen variables with their names, types, and current values
- Variables are sorted alphabetically for easy lookup
- Hover over a variable name to see: `varName : dataType` (e.g., `username : string`)
- Hover over a value to see the full content (useful for truncated strings, maps, lists, arrays)

**Bottom Half: 🖼️ Screen Items**
- Lists all screen area items from the screen definition
- Shows the item name with its actual current value from the JavaFX control
- Hover over an item name to see:
  - Full qualified name: `screenName.itemName`
  - `jfxType:` - The JavaFX control type (TEXTFIELD, CHECKBOX, etc.)
  - `varRef:` - The linked variable reference
  - `layout:` - The layout position

#### Copy to Clipboard
Click the 📋 button in the panel header to copy all variables and screen items to clipboard. The format includes:

```
Screen: myScreen
==================================================

📊 VARIABLES
----------------------------------------
username : string = "john_doe"
isActive : bool = true
count : int = 42

🖼️ SCREEN ITEMS
----------------------------------------
myScreen.usernameField [TEXTFIELD] varRef: username = "john_doe"
myScreen.activeCheck [CHECKBOX] varRef: isActive = true
```

#### Tooltip Styling
- Tooltips have a larger font (14px) for better readability
- Tooltips appear after 0.5 second delay to avoid interference with normal interaction

Press `Ctrl+D` again to hide the debug panel.

## Documentation

- **[EBS Script Syntax Reference](docs/EBS_SCRIPT_SYNTAX.md)** - Complete language syntax guide including type aliases (typeof)
- **[Python vs EBS String Functions](docs/PYTHON_VS_EBS_STRING_FUNCTIONS.md)** - Comparison guide for Python developers learning EBS Script
- **[EBS Script vs Oracle PL/SQL](docs/EBS_VS_ORACLE_PLSQL.md)** - Comprehensive comparison guide for PL/SQL developers learning EBS Script
- **[Architecture & Flow Documentation](ARCHITECTURE.md)** - Comprehensive documentation of the system architecture, data flow, and internal workings
- **[Class Tree Lister](CLASS_TREE_LISTER.md)** - Utility for analyzing the project's class hierarchy
- **[Syntax Reference](ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt)** - EBNF grammar specification

## Project Structure

```
maven-script-interpreter/
├── ScriptInterpreter/           # Main project directory
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/eb/
│   │       │       ├── script/          # Core interpreter
│   │       │       │   ├── token/       # Lexer and tokens
│   │       │       │   ├── parser/      # Parser
│   │       │       │   ├── interpreter/ # Interpreter and runtime
│   │       │       │   ├── arrays/      # Array implementations
│   │       │       │   ├── json/        # JSON support
│   │       │       │   └── file/        # File I/O
│   │       │       ├── ui/              # User interface
│   │       │       │   ├── cli/         # Console components
│   │       │       │   ├── ebs/         # Main application UI
│   │       │       │   └── tabs/        # Tab management
│   │       │       └── util/            # Utilities
│   │       └── resources/
│   │           └── css/                 # UI styling
│   └── pom.xml                          # Maven configuration
├── ARCHITECTURE.md                      # Architecture documentation
├── README.md                            # This file
└── CLASS_TREE_LISTER.md                # Class hierarchy tool docs
```

## Architecture Overview

The interpreter follows a classic three-phase architecture:

1. **Lexical Analysis** - Source code → Tokens (`EbsLexer`)
2. **Syntax Analysis** - Tokens → Abstract Syntax Tree (`Parser`)
3. **Interpretation** - AST → Execution (`Interpreter`)

The UI layer provides an interactive console built with JavaFX, featuring:
- Rich text editing with syntax highlighting
- Command history and autocomplete
- Tab-based script management
- Real-time output display

For detailed architecture information, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Built-in Functions

The interpreter includes numerous built-in functions:

### Type Conversion
- `toInt()`, `toFloat()`, `toDouble()`, `toString()`, `toBool()`

### String Operations
- `substring()`, `length()`, `indexOf()`, `replace()`, `split()`
- `toUpperCase()`, `toLowerCase()`, `trim()`
- `string.contains(str, sub)` - Checks if a string contains a substring, returns boolean
- `str.charArray()` - Returns an array of integer character codes (Unicode code points) for each character in a string

### Array Operations
- `push()`, `pop()`, `shift()`, `unshift()`
- `slice()`, `splice()`, `join()`

### Math Functions
- `abs()`, `min()`, `max()`, `round()`, `floor()`, `ceil()`
- `sqrt()`, `pow()`, `random()`

### File I/O
- `readFile()`, `writeFile()`, `appendFile()`
- `fileExists()`, `deleteFile()`

### JSON Operations
- `parseJson()`, `stringifyJson()`
- `validateJson()`, `deriveSchema()`

**Note**: Screen definitions use case-insensitive JSON key parsing, where property names are normalized to lowercase. Regular JSON operations (`parseJson()`) preserve the original key casing.

### Date/Time
- `now()`, `dateFormat()`, `parseDate()`

### CSS Operations
- `css.getValue(cssPath, selector, property)` - Retrieves a CSS property value from a stylesheet (e.g., `css.getValue("css/console.css", ".error", "-fx-fill")` returns `"#ee0000"`)
- `css.findCss(searchPath?)` - Searches for all available CSS stylesheet files and returns their paths as a string array

### Dialog Functions
- `system.inputDialog(title, headerText?, defaultValue?)` - Prompts user for text input, returns the entered string
- `system.confirmDialog(message, title?, headerText?)` - Shows YES/NO confirmation dialog, returns boolean
- `system.alertDialog(message, title?, alertType?)` - Shows message dialog with OK button (alertType: "info", "warning", "error")

## Database Support

The interpreter includes built-in database support with:
- Connection management
- SQL cursor operations
- Parameter binding
- Result set iteration
- Multiple simultaneous connections

Currently supports Oracle databases via `OracleDbAdapter`. Additional database adapters can be easily added by implementing the `DbAdapter` interface.

## AI Integration

The interpreter includes built-in AI integration for calling language models. The AI module supports both synchronous and asynchronous operations.

### AI Configuration

Configure AI settings using the `/ai setup` console command, which prompts for:
- API endpoint URL
- API key
- Model name

### Synchronous AI Calls

```javascript
// Simple AI completion (blocks until response)
var response = call ai.complete(systemPrompt, userPrompt, maxTokens, temperature);

// Example
var systemPrompt: string = "You are a helpful assistant.";
var userPrompt: string = "What is the capital of France?";
var result = call ai.complete(systemPrompt, userPrompt, 100, 0.7);
print result;
```

### Asynchronous AI Calls with Callbacks

For non-blocking AI calls that keep the UI responsive:

```javascript
// Async AI call returns a request ID
var requestId: int = call ai.completeAsync(systemPrompt, userPrompt, maxTokens, temperature, "callbackFunctionName");

// The callback function receives a JSON response
onAiComplete(aiResponse: json) {
    var isSuccess: bool = call json.getBool(aiResponse, "success", false);
    var result: string = call json.getString(aiResponse, "result", "");
    var wasCancelled: bool = call json.getBool(aiResponse, "cancelled", false);
    
    if isSuccess then {
        print "AI Response: " + result;
    } else if wasCancelled then {
        print "Request was cancelled";
    } else {
        print "Error: " + result;
    }
}
```

**Callback Response JSON Fields:**
- `success` (boolean) - Whether the AI call succeeded
- `result` (string) - The AI response text or error message
- `cancelled` (boolean) - Whether the request was cancelled

### Cancelling AI Requests

Cancel pending AI requests using the request ID:

```javascript
// Start an async request
var requestId: int = call ai.completeAsync(system, user, 200, 0.7, "myCallback");

// Cancel it if needed (e.g., user clicks Cancel button)
call ai.cancel(requestId);
```

**Cancellation Behavior:**
- Cancellation is immediate on the client side
- The callback receives `{"success": false, "cancelled": true, "result": "Request was cancelled"}`
- The underlying HTTP request may continue in the background until it naturally completes
- From the user's perspective, cancellation is instant

### AI Built-in Functions

| Function | Description |
|----------|-------------|
| `ai.complete(system, user, maxTokens, temp)` | Synchronous AI call, blocks until response |
| `ai.completeAsync(system, user, maxTokens, temp, callback)` | Async AI call, returns request ID |
| `ai.cancel(requestId)` | Cancels a pending async request |

### Screen Property Updates with `scr.setProperty`

Update UI controls programmatically (useful in AI callbacks):

```javascript
// Disable a button
call scr.setProperty("screenName.buttonName", "disabled", true);

// Update text content
call scr.setProperty("screenName.textAreaName", "value", "New text content");

// Enable button after operation
call scr.setProperty("screenName.buttonName", "disabled", false);
```

**Supported Properties:**
- `disabled` - Enable/disable controls (boolean)
- `value` / `text` - Update text content of TextField, TextArea, Label, or Button

## Extension & Customization

### Adding Built-in Functions

Add new functions to the `Builtins` class:

```java
public static Object myFunction(Object[] args) {
    // Implementation
    return result;
}
```

Register in the interpreter:

```java
environment.declare("myFunction", new BuiltinInfo(...));
```

### Adding Database Adapters

Implement the `DbAdapter` interface:

```java
public class MyDbAdapter implements DbAdapter {
    // Implement required methods
}
```

Set the adapter in the interpreter:

```java
interpreter.setDbAdapter(new MyDbAdapter());
```

### Running Scripts from Menu Items

You can add menu items that execute EBS scripts bundled as resources. This is useful for utility scripts like configuration editors.

**Step 1: Add your script to resources**

Place your `.ebs` script in `ScriptInterpreter/src/main/resources/scripts/`:

```
src/main/resources/
└── scripts/
    └── my_script.ebs
```

**Step 2: Add a menu item in `EbsMenu.java`**

```java
MenuItem myScriptItem = new MenuItem("My Script…");
myScriptItem.setOnAction(e -> {
    handler.runScriptFromResource("/scripts/my_script.ebs", "My Script");
});

// Add to the appropriate menu
toolsMenu.getItems().add(myScriptItem);
```

**The `runScriptFromResource` method:**
- Loads the script from the classpath resources
- Executes it in a background thread (doesn't block the UI)
- Updates the status bar during execution
- Displays errors in the console output if execution fails

**Parameters:**
- `resourcePath` - Path to the script resource (e.g., `/scripts/my_script.ebs`)
- `scriptName` - Friendly name shown in status bar and error messages

**Example: Color Editor Menu Item**

```java
MenuItem colorsItem = new MenuItem("Colors…");
colorsItem.setOnAction(e -> {
    handler.runScriptFromResource("/scripts/color_editor.ebs", "Color Editor");
});
```

## Contributing

Contributions are welcome! Areas for enhancement:
- Additional database adapters
- More built-in functions
- Enhanced error messages
- Performance optimizations
- Testing infrastructure
- Additional language features

## License

[Specify license here]

## Authors

Earl Bosch

## Acknowledgments

- JavaFX for the UI framework
- RichTextFX for advanced text editing capabilities
