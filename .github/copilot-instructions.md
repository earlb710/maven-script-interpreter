# Copilot Instructions for Maven Script Interpreter

## Project Overview

The Maven Script Interpreter is a custom scripting language interpreter for the EBS (Earl Bosch Script) language. It features a rich JavaFX-based interactive development environment with syntax highlighting, autocomplete, and integrated database support.

## Technology Stack

- **Language**: Java 21
- **Build Tool**: Maven 3.x
- **UI Framework**: JavaFX 21
- **Text Editor**: RichTextFX 0.11.6
- **Database Support**: JDBC with MySQL and PostgreSQL drivers
- **Architecture**: Three-phase interpreter (Lexer → Parser → Interpreter)

## Project Structure

```
maven-script-interpreter/
├── .github/                        # GitHub configuration
│   └── agents/                     # Custom agent definitions
├── ScriptInterpreter/              # Main project directory
│   ├── src/main/java/com/eb/
│   │   ├── script/                 # Core interpreter components
│   │   │   ├── token/              # Lexer and token definitions
│   │   │   ├── parser/             # Parser and AST
│   │   │   ├── interpreter/        # Interpreter and runtime
│   │   │   ├── arrays/             # Array implementations
│   │   │   ├── json/               # JSON support
│   │   │   └── file/               # File I/O operations
│   │   ├── ui/                     # User interface
│   │   │   ├── cli/                # Console components
│   │   │   ├── ebs/                # Main application UI
│   │   │   └── tabs/               # Tab management
│   │   └── util/                   # Utility classes
│   ├── src/main/resources/
│   │   └── css/                    # UI styling
│   └── pom.xml                     # Maven configuration
├── ARCHITECTURE.md                 # Detailed architecture documentation
├── README.md                       # User documentation
└── CLASS_TREE_LISTER.md           # Class hierarchy tool documentation
```

## Build and Run Instructions

### Prerequisites
- Java 21 or higher is **required** (the project uses Java 21 features and language level)
- Maven 3.x
- JavaFX 21

### Building
```bash
cd ScriptInterpreter
mvn clean compile
```

### Running the Application

**Interactive Console (JavaFX UI)**:
```bash
cd ScriptInterpreter
mvn javafx:run
```

**Command-Line Script Execution**:
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run <script-file.ebs>
```

## Testing

**Important**: This project currently does not have a formal test infrastructure or test suite. When making changes:

1. Manually test changes by running the application
2. Test with sample EBS scripts
3. Verify UI functionality through the interactive console
4. Test database features if modifying database-related code

## Code Organization and Conventions

### Core Interpreter Components

1. **Lexer** (`com.eb.script.token`): Tokenizes source code into tokens
   - `EbsLexer.java`: Main lexer implementation
   - `Token.java`: Token representation

2. **Parser** (`com.eb.script.parser`): Builds Abstract Syntax Tree (AST)
   - `Parser.java`: Recursive descent parser
   - AST node classes for various language constructs

3. **Interpreter** (`com.eb.script.interpreter`): Executes AST
   - `Interpreter.java`: Main interpreter with visitor pattern
   - `Environment.java`: Variable scoping and storage
   - `Builtins.java`: Built-in function implementations

### UI Components

- **EbsApp**: Main JavaFX application entry point
- **EbsConsoleHandler**: Console command processing and script execution
- **EbsTab**: Tab management for multiple script files
- **EbsStyled**: Syntax highlighting and text styling

### Database Support

The interpreter includes built-in database support:
- `DbAdapter` interface for database abstraction
- `OracleDbAdapter`: Oracle database implementation
- `DbConnection`: Connection management
- `DbCursor`: Result set iteration and cursor operations

Currently supports Oracle databases. MySQL and PostgreSQL drivers are included but adapters need to be implemented.

## Language Features

The EBS scripting language supports:
- Variables with type annotations (string, int, float, double, bool)
- Arrays (fixed-size and dynamic)
- JSON objects with parsing and validation
- Control flow (if/then/else, while, for loops)
- Functions with parameters and return values
- Database operations (connect, cursor, SQL queries)
- File I/O operations
- Rich set of built-in functions

See `ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt` for the complete grammar specification.

## Development Guidelines

### When Creating EBS Example Scripts

**IMPORTANT**: Always verify EBS syntax and available builtins before creating example scripts:

1. **Check Syntax**: Review `EBS_SCRIPT_SYNTAX.md` for correct language syntax
   - Function definitions: `functionName(params) return returnType { ... }` (no `function` keyword)
   - Variable declarations, control flow, and other language constructs
   
2. **Verify Builtins**: Check `Builtins.java` for available built-in functions
   - String functions: `string.toUpper`, `string.toLower`, `string.trim`, etc.
   - Array functions: `array.length`, `array.push`, `array.pop`, etc.
   - Math functions: `math.abs`, `math.max`, `math.min`, etc.
   - File I/O, JSON, date/time functions
   
3. **Test Before Committing**: Parse and validate example scripts to ensure they work correctly

### When Adding New Features

1. **Built-in Functions**: Add to `Builtins.java` and register in the interpreter's environment
2. **Language Constructs**: Update lexer tokens, parser rules, and interpreter execution logic
3. **Database Adapters**: Implement the `DbAdapter` interface
4. **UI Components**: Follow JavaFX and RichTextFX patterns used in existing code

### Code Style

- Use descriptive variable and method names
- Follow existing code organization patterns
- Add comments for complex logic, especially in the parser and interpreter
- Keep UI code separate from interpreter logic

### Common Patterns

- **Visitor Pattern**: Used in the interpreter for AST traversal
- **Environment Chaining**: For variable scoping (nested scopes)
- **Token-based Parsing**: Recursive descent parser with lookahead
- **Observer Pattern**: For console output and UI updates

## Known Limitations and Considerations

1. **Java Version**: Project requires Java 21 - do not downgrade the version requirement
2. **No Test Infrastructure**: Manual testing is required for all changes
3. **Database Support**: Only Oracle adapter is fully implemented
4. **Error Handling**: Error messages could be more descriptive in some areas
5. **Performance**: Not optimized for large scripts or datasets

## Documentation Resources

- **SPACE_RULES.md**: Copilot Space rules and interaction conventions for this project
- **ARCHITECTURE.md**: Comprehensive documentation of system architecture, component interactions, and data flow
- **README.md**: User-facing documentation with language syntax and examples
- **CLASS_TREE_LISTER.md**: Tool for analyzing class hierarchy

## When Working on Issues

1. Review ARCHITECTURE.md for understanding component interactions
2. Examine existing implementations before adding new features
3. Test with both the interactive console and command-line execution
4. Consider impact on both UI and core interpreter functionality
5. Update documentation if adding new language features or commands

## Common Tasks

### Adding a Built-in Function
1. Add method to `Builtins.java`
2. Register in `Interpreter.java` environment initialization
3. Update documentation if user-facing

### Adding Database Support
1. Implement `DbAdapter` interface
2. Add JDBC driver dependency to `pom.xml` if needed
3. Test with sample database operations

### Modifying Language Syntax
1. Update `EbsLexer.java` for new tokens
2. Update `Parser.java` for grammar rules
3. Update `Interpreter.java` for execution logic
4. Update `syntax_ebnf.txt` documentation

### UI Modifications
1. Modify relevant classes in `com.eb.ui.ebs`
2. Update CSS in `src/main/resources/css` if needed
3. Test with different screen sizes and themes
