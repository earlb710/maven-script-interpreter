# Maven Script Interpreter - Architecture & Flow Documentation

## Overview

The Maven Script Interpreter is a custom scripting language interpreter with a JavaFX-based interactive development environment. It provides a complete execution environment for the EBS (Earl Bosch Script) language, featuring a rich console interface, syntax highlighting, autocomplete, and integrated database capabilities.

## System Architecture

### High-Level Components

```
┌─────────────────────────────────────────────────────────────┐
│                        User Interface Layer                  │
│  ┌───────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │  EbsApp       │  │  MainApp     │  │  EbsTab          │ │
│  │  (JavaFX UI)  │  │  (Entry)     │  │  (Tab Manager)   │ │
│  └───────┬───────┘  └──────┬───────┘  └────────┬─────────┘ │
│          │                  │                    │           │
│          └──────────────────┴────────────────────┘           │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                     Console & Interaction Layer              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Console (Handler → EbsConsoleHandler)                 │ │
│  │  - Command processing                                  │ │
│  │  - Script area management                              │ │
│  │  - Output handling                                     │ │
│  └────────────────────────────┬───────────────────────────┘ │
└────────────────────────────────┴─────────────────────────────┘
                                 │
┌────────────────────────────────┴─────────────────────────────┐
│                       Script Processing Layer                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Lexer      │─▶│   Parser     │─▶│  Interpreter     │  │
│  │  (Tokenize)  │  │  (AST Build) │  │  (Execute)       │  │
│  └──────────────┘  └──────────────┘  └──────┬───────────┘  │
│                                               │              │
└───────────────────────────────────────────────┼──────────────┘
                                                │
┌───────────────────────────────────────────────┴──────────────┐
│                    Runtime & Support Layer                    │
│  ┌────────────┐  ┌──────────┐  ┌─────────────────────────┐ │
│  │ Environment│  │ Builtins │  │   Database Support      │ │
│  │ (Variables)│  │(Functions)  │   - DbAdapter           │ │
│  └────────────┘  └──────────┘  │   - DbConnection        │ │
│                                 │   - DbCursor            │ │
│                                 └─────────────────────────┘ │
│  ┌────────────┐  ┌──────────┐  ┌─────────────────────────┐ │
│  │   Arrays   │  │   JSON   │  │   File I/O              │ │
│  │            │  │ Support  │  │                         │ │
│  └────────────┘  └──────────┘  └─────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

## Detailed Component Flow

### 1. Application Startup Flow

```
MainApp.main()
    │
    └─▶ MainApp.start(Stage)
            │
            └─▶ EbsApp.start(Stage)
                    │
                    ├─▶ Initialize sandbox directory
                    ├─▶ Create EbsConsoleHandler
                    ├─▶ Create Console with handler
                    ├─▶ Setup UI (BorderPane + MenuBar + TabPane)
                    ├─▶ Apply CSS styling
                    └─▶ Display window and wait for user input
```

**Key Classes:**
- `MainApp`: JavaFX Application entry point
- `EbsApp`: Main application controller, sets up UI and runtime context
- `Console`: Interactive console with script area and output
- `EbsConsoleHandler`: Handles command execution and script submission

### 2. Script Execution Flow

```
User Input (Console)
    │
    └─▶ Console.submit(text)
            │
            └─▶ Handler.execute(script)
                    │
                    └─▶ EbsConsoleHandler.execute(script)
                            │
                            ├─▶ Check for special commands (/open, /save, /help, etc.)
                            │   └─▶ Execute command and return
                            │
                            └─▶ Script execution path:
                                    │
                                    ├─▶ Parser.parse(name, script)
                                    │       │
                                    │       ├─▶ EbsLexer.tokenize(script)
                                    │       │       │
                                    │       │       └─▶ Returns List<EbsToken>
                                    │       │
                                    │       ├─▶ Parser.parse()
                                    │       │       │
                                    │       │       ├─▶ Build AST (statements & expressions)
                                    │       │       ├─▶ Resolve blocks/functions
                                    │       │       └─▶ Return parsed structure
                                    │       │
                                    │       └─▶ Create RuntimeContext
                                    │               - name: script identifier
                                    │               - blocks: function definitions
                                    │               - statements: executable statements
                                    │
                                    └─▶ Interpreter.interpret(RuntimeContext)
                                            │
                                            ├─▶ Initialize Environment
                                            ├─▶ Register built-in functions
                                            ├─▶ Execute statements sequentially
                                            │       │
                                            │       └─▶ Visit each statement:
                                            │           - VarStatement
                                            │           - AssignStatement
                                            │           - PrintStatement
                                            │           - IfStatement
                                            │           - WhileStatement
                                            │           - CallStatement
                                            │           - etc.
                                            │
                                            └─▶ Handle control flow:
                                                - ReturnSignal (function return)
                                                - BreakSignal (loop break)
                                                - ContinueSignal (loop continue)
```

### 3. Parsing Pipeline

#### 3.1 Lexical Analysis (Tokenization)

```
Source Code (String)
    │
    └─▶ EbsLexer.tokenize()
            │
            ├─▶ Scan character by character
            ├─▶ Recognize token patterns:
            │   - Keywords (var, if, while, print, etc.)
            │   - Identifiers (variable/function names)
            │   - Literals (numbers, strings, booleans)
            │   - Operators (+, -, *, /, ==, !=, etc.)
            │   - Separators (parentheses, brackets, semicolons)
            │   - Comments (// line comments)
            │
            └─▶ Returns List<EbsToken>
                Each token contains:
                - Type (EbsTokenType enum)
                - Value (lexeme/text)
                - Position (line, column)
```

#### 3.2 Syntax Analysis (Parsing)

```
List<EbsToken>
    │
    └─▶ Parser.parse()
            │
            ├─▶ Initialize parser state
            │   - Current token index
            │   - Block/function registry
            │
            ├─▶ Parse program structure:
            │   │
            │   ├─▶ Named blocks (functions):
            │   │   - Function name
            │   │   - Parameters (optional)
            │   │   - Return type (optional)
            │   │   - Body statements
            │   │
            │   └─▶ Top-level statements:
            │       - Variable declarations
            │       - Assignments
            │       - Control flow
            │       - Function calls
            │       - Print statements
            │
            ├─▶ Build Abstract Syntax Tree (AST):
            │   │
            │   ├─▶ Statements (inherit from Statement):
            │   │   - VarStatement
            │   │   - AssignStatement
            │   │   - IfStatement
            │   │   - WhileStatement
            │   │   - DoWhileStatement
            │   │   - ForEachStatement
            │   │   - BlockStatement
            │   │   - CallStatement
            │   │   - ReturnStatement
            │   │   - BreakStatement
            │   │   - ContinueStatement
            │   │   - PrintStatement
            │   │   - Database statements (Connect, Cursor, etc.)
            │   │
            │   └─▶ Expressions (inherit from Expression):
            │       - LiteralExpression (constants)
            │       - VariableExpression (variable access)
            │       - BinaryExpression (a + b, a == b, etc.)
            │       - UnaryExpression (!a, -a)
            │       - CallExpression (function calls)
            │       - IndexExpression (array[index])
            │       - ArrayExpression/ArrayLiteralExpression
            │       - LengthExpression (array.length)
            │       - SQL expressions
            │
            └─▶ Returns RuntimeContext
                - Parsed blocks (functions)
                - Parsed statements (program)
                - Environment placeholder
```

#### 3.3 Operator Precedence (in Parser)

```
Highest Precedence:
    │
    ├─▶ Postfix: array[index], property.length
    ├─▶ Unary: !, -, +
    ├─▶ Exponentiation: ^ (right-associative)
    ├─▶ Multiplication/Division: *, /
    ├─▶ Addition/Subtraction: +, -
    ├─▶ Comparison: ==, !=, <, >, <=, >=
    ├─▶ Logical AND: and
    └─▶ Logical OR: or
    │
Lowest Precedence
```

### 4. Interpretation & Execution Flow

```
RuntimeContext
    │
    └─▶ Interpreter.interpret(RuntimeContext)
            │
            ├─▶ Setup execution environment:
            │   │
            │   ├─▶ Create Environment
            │   │   - Variable storage (Map<String, Object>)
            │   │   - Scope management (stack of environments)
            │   │   - Echo mode (print executed statements)
            │   │
            │   ├─▶ Register built-in functions:
            │   │   - Type conversion (toInt, toFloat, toString, etc.)
            │   │   - String operations (substring, length, etc.)
            │   │   - Array operations
            │   │   - File I/O (read, write, etc.)
            │   │   - JSON operations (parse, stringify)
            │   │   - Math functions
            │   │   - Date/time functions
            │   │   - AI functions (if configured)
            │   │
            │   └─▶ Initialize database adapter (OracleDbAdapter by default)
            │
            ├─▶ Execute statements using Visitor pattern:
            │   │
            │   ├─▶ VarStatement:
            │   │   - Evaluate initializer expression
            │   │   - Declare variable in environment
            │   │   - Handle arrays with specified dimensions
            │   │
            │   ├─▶ AssignStatement:
            │   │   - Evaluate right-hand expression
            │   │   - Update variable in environment
            │   │
            │   ├─▶ PrintStatement:
            │   │   - Evaluate expression
            │   │   - Convert to string
            │   │   - Output to console
            │   │
            │   ├─▶ IfStatement:
            │   │   - Evaluate condition expression
            │   │   - Execute then-branch or else-branch
            │   │   - Handle nested blocks
            │   │
            │   ├─▶ WhileStatement/DoWhileStatement:
            │   │   - Loop while condition is true
            │   │   - Execute body statements
            │   │   - Handle break/continue signals
            │   │
            │   ├─▶ ForEachStatement:
            │   │   - Iterate over array/collection
            │   │   - Bind loop variable
            │   │   - Execute body for each element
            │   │
            │   ├─▶ CallStatement:
            │   │   - Resolve function (built-in or user-defined)
            │   │   - Evaluate arguments
            │   │   - Execute function:
            │   │       │
            │   │       ├─▶ Built-in function:
            │   │       │   - Direct Java method invocation
            │   │       │
            │   │       └─▶ User-defined block:
            │   │           - Create new scope
            │   │           - Bind parameters
            │   │           - Execute block statements
            │   │           - Capture return value
            │   │           - Restore scope
            │   │
            │   ├─▶ BlockStatement:
            │   │   - Execute statements in sequence
            │   │   - Manage scope
            │   │
            │   ├─▶ ReturnStatement:
            │   │   - Evaluate return expression
            │   │   - Throw ReturnSignal (exception-based control)
            │   │
            │   ├─▶ BreakStatement/ContinueStatement:
            │   │   - Throw BreakSignal/ContinueSignal
            │   │
            │   └─▶ Database statements:
            │       - ConnectStatement: establish DB connection
            │       - CursorStatement: declare cursor
            │       - OpenCursorStatement: open cursor with query
            │       - CloseCursorStatement: close cursor
            │       - UseConnectionStatement: switch active connection
            │       - CloseConnectionStatement: close connection
            │
            └─▶ Expression evaluation using Visitor pattern:
                │
                ├─▶ LiteralExpression:
                │   - Return constant value directly
                │
                ├─▶ VariableExpression:
                │   - Look up variable in environment
                │   - Throw error if undefined
                │
                ├─▶ BinaryExpression:
                │   - Evaluate left operand
                │   - Evaluate right operand
                │   - Apply operator:
                │       - Arithmetic: +, -, *, /, ^
                │       - Comparison: ==, !=, <, >, <=, >=
                │       - Logical: and, or
                │   - Return result
                │
                ├─▶ UnaryExpression:
                │   - Evaluate operand
                │   - Apply operator: !, -, +
                │   - Return result
                │
                ├─▶ CallExpression:
                │   - Same as CallStatement but returns value
                │
                ├─▶ IndexExpression:
                │   - Evaluate array expression
                │   - Evaluate index expressions
                │   - Access array element(s)
                │   - Return value
                │
                ├─▶ ArrayLiteralExpression:
                │   - Evaluate element expressions
                │   - Create array instance
                │   - Return array
                │
                └─▶ LengthExpression:
                    - Evaluate target expression
                    - Return array/string length
```

### 5. Environment & Scope Management

```
Environment Structure:
    │
    ├─▶ Global Scope (root)
    │   - Variables declared at top level
    │   - Built-in functions
    │   - User-defined blocks/functions
    │
    └─▶ Local Scopes (stack)
        - Function call parameters
        - Local variables
        - Block-scoped variables
        
Scope Resolution:
    │
    └─▶ Variable lookup:
        - Search current scope
        - If not found, search parent scope
        - Continue up to global scope
        - Throw error if not found
        
    └─▶ Variable assignment:
        - Search for existing variable
        - Update in the scope where found
        - If new variable, declare in current scope
```

### 6. Database Integration Flow

```
Database Operations:
    │
    ├─▶ Connection Management:
    │   │
    │   ├─▶ ConnectStatement:
    │   │   - Parse connection spec (URL/JSON)
    │   │   - Create DbConnection
    │   │   - Store in connections map
    │   │
    │   ├─▶ UseConnectionStatement:
    │   │   - Push connection to stack
    │   │   - Execute statements in block
    │   │   - Pop connection from stack
    │   │
    │   └─▶ CloseConnectionStatement:
    │       - Remove from connections map
    │       - Close database connection
    │
    └─▶ Cursor Operations:
        │
        ├─▶ CursorStatement:
        │   - Define cursor with SQL SELECT
        │   - Store cursor spec
        │
        ├─▶ OpenCursorStatement:
        │   - Get active connection
        │   - Prepare SQL statement
        │   - Bind parameters
        │   - Execute query
        │   - Create DbCursor
        │
        ├─▶ Cursor iteration:
        │   - CursorHasNextExpression: check for more rows
        │   - CursorNextExpression: fetch next row
        │   - Access columns via cursor object
        │
        └─▶ CloseCursorStatement:
            - Close cursor
            - Release resources
```

### 7. Array Support

```
Array Types:
    │
    ├─▶ ArrayFixed: Fixed-size, single type
    │   - Pre-allocated storage
    │   - Bounds checking
    │
    ├─▶ ArrayFixedByte: Optimized byte array
    │   - Memory-efficient storage
    │
    └─▶ ArrayDynamic: Variable-size, any type
        - Grows as needed
        - Mixed-type elements

Array Operations:
    │
    ├─▶ Declaration: var arr: int[10];
    ├─▶ Initialization: var arr = [1, 2, 3, 4, 5];
    ├─▶ Access: arr[index]
    ├─▶ Assignment: arr[index] = value;
    ├─▶ Multi-dimensional: arr[i][j] or arr[i,j]
    └─▶ Length: arr.length
```

### 8. JSON Support

```
JSON Operations:
    │
    ├─▶ Parsing:
    │   - Json.parse(string) → Map/List
    │   - Automatic type detection
    │
    ├─▶ Serialization:
    │   - Json.stringify(object) → String
    │   - Pretty printing support
    │
    ├─▶ Schema:
    │   - JsonSchema: define structure
    │   - JsonValidate: validate against schema
    │   - JsonSchemaDeriver: infer schema from data
    │
    └─▶ Variable declaration:
        - var data: json = {"key": "value"};
        - Direct JSON literal in source
```

### 9. UI Console Interaction

```
Console Components:
    │
    ├─▶ ScriptArea (extends StyleClassedTextArea):
    │   - Syntax highlighting
    │   - Line numbers
    │   - Code editing with rich text support
    │
    ├─▶ Console Output:
    │   - StyledTextAreaOutputStream
    │   - Colored output (info, error, success)
    │   - System.out/err redirection
    │
    ├─▶ Command History:
    │   - Up/Down arrow navigation
    │   - Previous commands recall
    │
    └─▶ Special Commands:
        - /open <file>: Load script from file
        - /save <file>: Save current script
        - /help: Show command reference
        - /clear: Clear console output
        - /echo on|off: Toggle statement echo
        - /ai setup: Configure AI integration
        - /safe-dirs: Configure safe directories

Tab Management:
    │
    ├─▶ Console Tab: Main interactive console
    ├─▶ Script Tabs: Open script files
    └─▶ TabHandler: Manage multiple tabs
```

### 10. Error Handling

```
Error Types:
    │
    ├─▶ Lexer Errors:
    │   - Invalid characters
    │   - Unclosed strings
    │   - Invalid number formats
    │
    ├─▶ Parser Errors (ParseError):
    │   - Syntax errors
    │   - Unexpected tokens
    │   - Missing semicolons/braces
    │
    ├─▶ Interpreter Errors (InterpreterError):
    │   - Undefined variables
    │   - Type mismatches
    │   - Division by zero
    │   - Array index out of bounds
    │
    └─▶ Control Flow Signals:
        - ReturnSignal: Function return
        - BreakSignal: Loop break
        - ContinueSignal: Loop continue
        
Error Reporting:
    - Line/column information from tokens
    - Stack trace for nested calls
    - Formatted error messages in console
```

## Module Relationships

### Core Packages

#### `com.eb.script`
- **Purpose**: Top-level script execution and runtime context
- **Key Classes**: `Run`, `RuntimeContext`

#### `com.eb.script.token`
- **Purpose**: Token definitions and lexical analysis
- **Key Classes**: `EbsLexer`, `EbsToken`, `EbsTokenType`, `DataType`

#### `com.eb.script.parser`
- **Purpose**: Syntax analysis and AST construction
- **Key Classes**: `Parser`, `ParseError`

#### `com.eb.script.interpreter`
- **Purpose**: Runtime execution and interpretation
- **Key Classes**: `Interpreter`, `Environment`, `Builtins`, `InterpreterError`

#### `com.eb.script.interpreter.expression`
- **Purpose**: Expression AST nodes
- **Key Classes**: `Expression`, various expression implementations

#### `com.eb.script.interpreter.statement`
- **Purpose**: Statement AST nodes
- **Key Classes**: `Statement`, various statement implementations

#### `com.eb.script.interpreter.db`
- **Purpose**: Database integration
- **Key Classes**: `DbAdapter`, `DbConnection`, `DbCursor`, `OracleDbAdapter`

#### `com.eb.script.arrays`
- **Purpose**: Array implementations
- **Key Classes**: `ArrayDef`, `ArrayFixed`, `ArrayDynamic`, `ArrayFixedByte`

#### `com.eb.script.json`
- **Purpose**: JSON parsing, validation, and schema
- **Key Classes**: `Json`, `JsonSchema`, `JsonValidate`, `JsonSchemaDeriver`

#### `com.eb.script.file`
- **Purpose**: File I/O operations
- **Key Classes**: `FileContext`, `FileData`, `BuiltinsFile`

#### `com.eb.ui.cli`
- **Purpose**: Console UI components
- **Key Classes**: `Console`, `Handler`, `ScriptArea`, `MainApp`

#### `com.eb.ui.ebs`
- **Purpose**: Main application UI
- **Key Classes**: `EbsApp`, `EbsConsoleHandler`, `EbsMenu`, `EbsTab`

#### `com.eb.ui.tabs`
- **Purpose**: Tab management
- **Key Classes**: `TabHandler`, `TabOpener`, `TabContext`, `EbsTab`

#### `com.eb.util`
- **Purpose**: Utility functions
- **Key Classes**: `Util`, `Debugger`, `ClassTreeLister`, `EmbeddingSearch`

## Data Flow Summary

```
┌──────────────┐
│  User Input  │
└──────┬───────┘
       │
       ▼
┌──────────────┐      ┌────────────┐      ┌──────────────┐
│   Console    │─────▶│  Handler   │─────▶│    Parser    │
└──────────────┘      └────────────┘      └──────┬───────┘
                                                  │
                                                  ▼
                                         ┌─────────────────┐
                                         │  RuntimeContext │
                                         │  (AST + Blocks) │
                                         └────────┬────────┘
                                                  │
                                                  ▼
                                         ┌─────────────────┐
                                         │  Interpreter    │
                                         │  (Execute)      │
                                         └────────┬────────┘
                                                  │
                      ┌───────────────────────────┼───────────────────────┐
                      │                           │                       │
                      ▼                           ▼                       ▼
              ┌───────────────┐         ┌─────────────────┐    ┌─────────────┐
              │  Environment  │         │  Built-ins      │    │  Database   │
              │  (Variables)  │         │  (Functions)    │    │  Adapter    │
              └───────────────┘         └─────────────────┘    └─────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  Console      │
              │  Output       │
              └───────────────┘
```

## Key Design Patterns

1. **Visitor Pattern**: Used for traversing and executing AST nodes (statements and expressions)
2. **Strategy Pattern**: DbAdapter allows different database implementations
3. **Command Pattern**: Console commands are processed through Handler interface
4. **Factory Pattern**: Token and expression/statement creation
5. **Singleton**: Global utilities and built-in function registry

## Extension Points

- **Custom Built-in Functions**: Add to `Builtins` class
- **Database Adapters**: Implement `DbAdapter` interface
- **Expression Types**: Extend `Expression` class and update parser
- **Statement Types**: Extend `Statement` class and update parser
- **UI Commands**: Add handlers in `EbsConsoleHandler`

## Performance Considerations

- **Token List**: Pre-tokenized for fast parsing
- **Array Types**: Specialized implementations for common cases
- **Scope Management**: Stack-based for efficient lookup
- **AST Caching**: RuntimeContext can be reused for repeated execution
- **Database Connections**: Pooled and reused across operations

## Security Features

- **Sandbox Root**: Scripts execute in designated safe directory
- **Safe Directories Dialog**: Configure trusted directories for file operations
- **Input Validation**: Type checking and bounds checking
- **Error Isolation**: Exceptions don't crash the application

## Future Enhancements

- Additional database adapters (MySQL, PostgreSQL, etc.)
- Debugger integration with breakpoints
- Performance profiling tools
- Module/import system
- Native library integration
- More built-in functions
- Enhanced error messages with suggestions
