# Maven Script Interpreter

A powerful script interpreter for the EBS (Earl Bosch Script) language, featuring a rich JavaFX-based interactive development environment with syntax highlighting, autocomplete, and integrated database support.

## Features

- **Custom Scripting Language**: Full-featured scripting language with familiar syntax
- **Type Casting**: Explicit type conversions with `int()`, `string()`, `float()`, `double()`, `byte()`, `long()`, `boolean()`
- **typeof Operator**: Get the type of any variable or expression at runtime (e.g., `typeof a` returns `"string"`)
- **Interactive Console**: JavaFX-based IDE with rich text editing
- **Syntax Highlighting**: Color-coded syntax for better readability
- **Autocomplete**: Intelligent code completion suggestions for keywords, built-ins, and JSON schemas
- **Configurable Colors**: Customize console colors via JSON configuration file
- **Database Integration**: Built-in SQL support with cursors and connections
- **Type Aliases**: Define reusable type aliases for complex types with the `typeof` keyword
- **Array Support**: Multi-dimensional arrays with type safety
- **JSON Support**: Native JSON parsing, validation, and schema support
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

## Language Overview

### Basic Syntax

```javascript
// Variable declaration
var name: string = "World";
var count: int = 42;
var items: int[5];  // Fixed-size array
var data: json = {"key": "value"};

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
person = {"name": "Alice", "age": 30};
print person;  // Print the record

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
- Complex types: `array`, `json`, `date`, `record`
- For records, displays the complete structure with field names and types

See `scripts/test/test_typeof_operator.ebs` for more examples.

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
        }
    ]
};

// Access screen variables using screen_name.var_name syntax
var currentCount = myWindow.counter;
print currentCount;  // Prints: 0

// Assign to screen variables
myWindow.counter = 42;
myWindow.message = "Updated!";

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

## Documentation

- **[EBS Script Syntax Reference](docs/EBS_SCRIPT_SYNTAX.md)** - Complete language syntax guide including type aliases (typeof)
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

## Database Support

The interpreter includes built-in database support with:
- Connection management
- SQL cursor operations
- Parameter binding
- Result set iteration
- Multiple simultaneous connections

Currently supports Oracle databases via `OracleDbAdapter`. Additional database adapters can be easily added by implementing the `DbAdapter` interface.

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
