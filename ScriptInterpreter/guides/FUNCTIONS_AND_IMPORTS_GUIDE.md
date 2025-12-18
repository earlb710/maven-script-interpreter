# Functions and Imports Guide

## Overview

This guide provides comprehensive best practices for defining functions, organizing code with imports, and structuring large EBS applications. Whether you're building a simple script or a complex multi-file project, this guide will help you create maintainable, well-organized code.

**Related Documentation:**
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Complete language syntax reference
- [COMPLEX_DATA_TYPES_GUIDE.md](COMPLEX_DATA_TYPES_GUIDE.md) - Arrays and records guide
- [SCREEN_DEFINITION_BEST_PRACTICES.md](SCREEN_DEFINITION_BEST_PRACTICES.md) - Screen/UI best practices

---

## Table of Contents

1. [Functions](#functions)
   - [Function Definition](#function-definition)
   - [Function Organization](#function-organization)
   - [Parameter Best Practices](#parameter-best-practices)
   - [Return Values](#return-values)
   - [Function Naming Conventions](#function-naming-conventions)
2. [Import System](#import-system)
   - [Basic Imports](#basic-imports)
   - [Import Resolution](#import-resolution)
   - [Circular Import Protection](#circular-import-protection)
   - [Import Best Practices](#import-best-practices)
3. [Structuring Large Applications](#structuring-large-applications)
   - [Project Organization](#project-organization)
   - [Module Separation](#module-separation)
   - [Common Architectural Patterns](#common-architectural-patterns)
   - [File Organization](#file-organization)
4. [Real-World Examples](#real-world-examples)
   - [Chess Game Structure](#chess-game-structure)
   - [TicTacToe Structure](#tictactoe-structure)
5. [Best Practices](#best-practices)
6. [Common Pitfalls](#common-pitfalls)
7. [Troubleshooting](#troubleshooting)

---

## Functions

### Function Definition

EBS supports two styles of function definition - both are equivalent and you can choose based on your preference or team conventions.

#### Traditional Style (No `function` Keyword)

```javascript
// Basic function with no parameters or return value
greet {
    print "Hello, World!";
}

// Function with parameters
add(a: int, b: int) return int {
    return a + b;
}

// Function with return type
getCurrentYear return int {
    var now: date = now();
    return call date.getYear(now);
}
```

#### Beginner-Friendly Style (With `function` Keyword)

```javascript
// Basic function with 'function' keyword
function greet {
    print "Hello, World!";
}

// Function with parameters
function add(a: int, b: int) return int {
    return a + b;
}

// Function with return type
function getCurrentYear return int {
    var now: date = now();
    return call date.getYear(now);
}
```

**Best Practice:** Choose one style and use it consistently throughout your project. The traditional style (without `function`) is more concise, while the keyword style may be clearer for beginners.

### Function Organization

#### Logical Grouping

Organize functions logically within your files. Group related functions together and use comments to create clear sections.

```javascript
// ===========================
// Validation Functions
// ===========================

function isValidEmail(email: string) return bool {
    return call string.contains(email, "@");
}

function isValidAge(age: int) return bool {
    return age >= 0 && age <= 150;
}

// ===========================
// Data Processing Functions
// ===========================

function formatUserName(firstName: string, lastName: string) return string {
    return call string.toUpper(firstName) + " " + call string.toUpper(lastName);
}

function calculateTotal(items: array.double[*]) return double {
    var total: double = 0.0;
    for (var i: int = 0; i < call array.length(items); i++) {
        total = total + items[i];
    }
    return total;
}
```

#### Helper Functions First, Main Logic Later

Place helper/utility functions at the top of your file or in separate modules, followed by main application logic.

```javascript
// Helper functions
function isValidPosition(x: int, y: int) return bool {
    return x >= 0 && x < 8 && y >= 0 && y < 8;
}

function createPosition(x: int, y: int) return posType {
    var jsonStr: string = '{"x": ' + x + ', "y": ' + y + '}';
    var jsonData: json = call json.jsonfromstring(jsonStr);
    return record(jsonData);
}

// Main application logic
function startGame {
    call initializeBoard();
    call showGameScreen();
}
```

### Parameter Best Practices

#### Use Type Annotations

Always specify parameter types for clarity and type safety.

```javascript
// Good: Clear parameter types
function calculateArea(width: double, height: double) return double {
    return width * height;
}

// Bad: Missing type information (will cause parse error)
function calculateArea(width, height) return double {
    return width * height;
}
```

#### Default Parameters

Use default parameters for optional values.

```javascript
function createUser(name: string, age: int = 18, active: bool = true) return record {
    var jsonStr: string = '{"name": "' + name + '", "age": ' + age + ', "active": ' + active + '}';
    var jsonData: json = call json.jsonfromstring(jsonStr);
    return record(jsonData);
}

// Call with all parameters
var user1 = call createUser("Alice", 25, true);

// Call with defaults
var user2 = call createUser("Bob");  // Uses age=18, active=true
```

#### Named Parameters

For functions with many parameters, consider using named parameters for clarity.

```javascript
function createRect(x: int, y: int, width: int, height: int, color: string) return record {
    // Function implementation
}

// Named parameters make the call clearer
var rect = call createRect(x = 10, y = 20, width = 100, height = 50, color = "#FF0000");
```

#### Complex Parameters

For functions requiring many parameters, consider using a record type to group related values.

```javascript
// Define a configuration record type
ConfigType typeof record { host: string, port: int, timeout: int, retries: int };

// Function takes a single config parameter
function connectDatabase(config: ConfigType) return bool {
    print "Connecting to " + config.host + ":" + config.port;
    // Connection logic here
    return true;
}

// Usage
var dbConfig = ConfigType {
    host: "localhost",
    port: 5432,
    timeout: 30,
    retries: 3
};
var connected = call connectDatabase(dbConfig);
```

### Return Values

#### Single Return Value

Most functions should return a single value with a clear type.

```javascript
function divide(dividend: double, divisor: double) return double {
    if divisor == 0.0 then {
        throw "Division by zero error";
    }
    return dividend / divisor;
}
```

#### Returning Records for Multiple Values

When you need to return multiple values, use a record type.

```javascript
// Define return type
ResultType typeof record { success: bool, message: string, value: int };

function parseInteger(text: string) return ResultType {
    try {
        var value: int = int(text);
        return ResultType { success: true, message: "Success", value: value };
    } exceptions {
        when ANY_ERROR(msg) {
            return ResultType { success: false, message: msg, value: 0 };
        }
    }
}

// Usage
var result = call parseInteger("42");
if result.success then {
    print "Parsed value: " + result.value;
} else {
    print "Error: " + result.message;
}
```

#### Returning Arrays

Functions can return arrays for collections of values.

```javascript
function getEvenNumbers(max: int) return array.int[*] {
    var evens: array.int[*];
    var count: int = 0;
    
    for (var i: int = 0; i <= max; i++) {
        if i % 2 == 0 then {
            evens[count] = i;
            count++;
        }
    }
    
    return evens;
}

// Usage
var evens = call getEvenNumbers(10);
// Result: [0, 2, 4, 6, 8, 10]
```

### Function Naming Conventions

Follow consistent naming conventions for better code readability.

#### Verb-Noun Pattern

Use verbs for function names to indicate actions.

```javascript
// Good: Verb-noun pattern
function calculateTotal(items: array) return double { /* ... */ }
function getUserData(userId: int) return record { /* ... */ }
function validateInput(input: string) return bool { /* ... */ }
function formatDate(date: date) return string { /* ... */ }

// Less clear: Noun-only names
function total(items: array) return double { /* ... */ }
function userData(userId: int) return record { /* ... */ }
```

#### Boolean Functions

Functions returning boolean values should start with `is`, `has`, `can`, or `should`.

```javascript
function isValidEmail(email: string) return bool { /* ... */ }
function hasPermission(user: record, action: string) return bool { /* ... */ }
function canMove(piece: record, position: posType) return bool { /* ... */ }
function shouldRefresh(lastUpdate: date) return bool { /* ... */ }
```

#### Get/Set Functions

Use clear prefixes for accessor and mutator functions.

```javascript
function getUserName(user: record) return string { /* ... */ }
function setUserName(user: record, name: string) { /* ... */ }

function getScore return int { /* ... */ }
function setScore(newScore: int) { /* ... */ }
```

#### Case Sensitivity

**Important:** EBS is case-insensitive for identifiers. All function names are normalized to lowercase internally.

```javascript
// These all refer to the same function
function calculateTotal return int { return 100; }

var result1 = call calculateTotal();    // Works
var result2 = call CALCULATETOTAL();    // Works (same function)
var result3 = call CalculateTotal();    // Works (same function)
```

**Best Practice:** Use consistent camelCase naming for readability, even though the interpreter treats all variations the same.

---

## Import System

### Basic Imports

The `import` statement allows you to include code from other EBS files, enabling modular code organization.

#### Simple Import

```javascript
// Import from same directory
import "helper.ebs";

// Now you can call functions defined in helper.ebs
var result = call helperFunction();
```

#### Import from Subdirectory

```javascript
// Import from subdirectory
import "util/stringUtil.ebs";
import "lib/database.ebs";
import "helpers/validation.ebs";
```

#### Import with Spaces in Path

```javascript
// Use quotes for paths with spaces
import "my utils/helper functions.ebs";
import "game logic/move calculator.ebs";
```

#### Both Quote Styles Supported

```javascript
// Double quotes (recommended)
import "util/math.ebs";

// Single quotes (also valid)
import 'util/string.ebs';
```

### Import Resolution

Import paths are resolved relative to the importing script's directory.

#### Directory Structure Example

```
my-project/
├── main.ebs
├── util/
│   ├── math.ebs
│   └── string.ebs
└── lib/
    ├── database.ebs
    └── validation/
        └── validators.ebs
```

#### Import from main.ebs

```javascript
// In main.ebs
import "util/math.ebs";           // Resolves to: my-project/util/math.ebs
import "lib/database.ebs";        // Resolves to: my-project/lib/database.ebs
```

#### Import from util/math.ebs

```javascript
// In util/math.ebs (nested file)
import "string.ebs";              // Resolves to: my-project/util/string.ebs
import "../lib/database.ebs";    // Parent directory: my-project/lib/database.ebs
```

**Note:** While parent directory references (`../`) work, it's better to organize your files to minimize the need for complex relative paths.

### Circular Import Protection

EBS automatically protects against circular imports. Each file is imported only once, even if multiple files try to import it.

#### Example of Safe Circular Reference

```javascript
// file1.ebs
import "file2.ebs";

function functionInFile1 {
    print "Function 1";
}

// file2.ebs
import "file1.ebs";  // Safe: won't cause infinite loop

function functionInFile2 {
    print "Function 2";
}
```

The interpreter tracks imported files and skips files that have already been processed, preventing infinite import loops.

### Import Best Practices

#### 1. Place Imports at the Top

Import statements should appear at the beginning of your script for better organization and readability.

```javascript
// Good: Imports at the top
import "util/math.ebs";
import "util/string.ebs";
import "lib/database.ebs";

// Type definitions
posType typeof record { x: int, y: int };

// Constants
var MAX_USERS: int = 100;

// Functions
function main {
    // Main logic
}
```

#### 2. Import After Dependencies (When Necessary)

If imported code depends on types or constants from the main file, place the import after those definitions.

```javascript
// main.ebs

// Define types that imported code will use
ChessPiece typeof record { piece: string, color: string, pos: posType };
posType typeof record { x: int, y: int };

// Define constants
var WHITE: int = 0;
var BLACK: int = 1;

// Helper functions that imported code depends on
function isValidPosition(x: int, y: int) return bool {
    return x >= 0 && x < 8 && y >= 0 && y < 8;
}

// Import after dependencies are defined
import "chess-moves.ebs";

// Rest of the code...
```

#### 3. Group Related Imports

Group imports by category with blank lines for better organization.

```javascript
// Core utilities
import "util/math.ebs";
import "util/string.ebs";
import "util/date.ebs";

// Database modules
import "lib/database.ebs";
import "lib/query-builder.ebs";

// Business logic
import "game-logic.ebs";
import "scoring.ebs";
```

#### 4. Use Clear File Names

Choose descriptive file names that clearly indicate their purpose.

```javascript
// Good: Clear, descriptive names
import "user-validation.ebs";
import "database-connection.ebs";
import "chess-move-calculator.ebs";

// Less clear: Vague names
import "utils.ebs";
import "helpers.ebs";
import "stuff.ebs";
```

#### 5. Avoid Deep Nesting

Keep directory structures relatively flat to avoid complex import paths.

```javascript
// Good: Shallow structure
import "validation/email.ebs";
import "database/users.ebs";

// Less maintainable: Deep nesting
import "src/app/modules/user/validation/email/validators.ebs";
```

---

## Structuring Large Applications

### Project Organization

A well-organized project structure makes code easier to find, maintain, and test.

#### Recommended Directory Structure

```
my-application/
├── project.json              # Project metadata (if using project system)
├── README.md                 # Project documentation
├── main.ebs                  # Application entry point
├── config.ebs                # Configuration constants
├── util/                     # Utility functions
│   ├── math.ebs
│   ├── string.ebs
│   └── validation.ebs
├── lib/                      # Third-party or shared libraries
│   ├── database.ebs
│   └── http-client.ebs
├── models/                   # Data type definitions
│   ├── types.ebs
│   └── records.ebs
├── screens/                  # UI screen definitions
│   ├── main-screen.ebs
│   └── settings-screen.ebs
├── logic/                    # Business logic
│   ├── game-logic.ebs
│   └── scoring.ebs
└── test/                     # Test scripts
    ├── test-validation.ebs
    └── test-logic.ebs
```

#### Alternative Structure (Feature-Based)

For larger applications, you might organize by feature rather than by type:

```
my-application/
├── main.ebs
├── shared/                   # Shared utilities and types
│   ├── types.ebs
│   ├── constants.ebs
│   └── utils.ebs
├── features/
│   ├── user-management/
│   │   ├── user-types.ebs
│   │   ├── user-logic.ebs
│   │   └── user-screen.ebs
│   ├── game/
│   │   ├── game-types.ebs
│   │   ├── game-logic.ebs
│   │   ├── game-screen.ebs
│   │   └── move-calculator.ebs
│   └── settings/
│       ├── settings-logic.ebs
│       └── settings-screen.ebs
└── test/
    ├── test-user.ebs
    └── test-game.ebs
```

### Module Separation

#### 1. Separate Concerns

Divide code into modules based on responsibility:

- **Types Module**: Type aliases, record definitions, bitmap/intmap definitions
- **Constants Module**: Configuration values, enums, magic numbers
- **Utils Module**: Generic utility functions
- **Logic Module**: Business logic and algorithms
- **UI Module**: Screen definitions and UI-related code
- **Data Module**: Database operations, file I/O

#### 2. Types and Constants Module

Create a dedicated file for shared types and constants.

```javascript
// types.ebs - Shared type definitions

// Position type
posType typeof record { x: int, y: int };

// User type
UserType typeof record {
    id: int,
    name: string,
    email: string,
    active: bool
};

// Game state type
GameStateType typeof record {
    currentPlayer: int,
    moveCount: int,
    gameOver: bool
};

// Cell type with bit fields
CellType typeof bitmap {
    occupied: 0,
    pieceType: 1-3,
    color: 7
};
```

```javascript
// constants.ebs - Shared constants

// Player colors
var WHITE: int = 0;
var BLACK: int = 1;

// Piece types
var EMPTY: int = 0;
var PAWN: int = 1;
var KNIGHT: int = 2;
var BISHOP: int = 3;
var ROOK: int = 4;
var QUEEN: int = 5;
var KING: int = 6;

// Board dimensions
var BOARD_SIZE: int = 8;

// UI colors
var COLOR_PRIMARY: string = "#2c2c2c";
var COLOR_SECONDARY: string = "#3c3c3c";
var COLOR_ACCENT: string = "#ffcc00";
```

#### 3. Utility Functions Module

Group generic helper functions that can be reused across the application.

```javascript
// utils.ebs - Generic utility functions

function clamp(value: int, min: int, max: int) return int {
    if value < min then {
        return min;
    }
    if value > max then {
        return max;
    }
    return value;
}

function isInRange(value: int, min: int, max: int) return bool {
    return value >= min && value <= max;
}

function formatTime(seconds: int) return string {
    var mins: int = seconds / 60;
    var secs: int = seconds % 60;
    return mins + ":" + call string.lpad("" + secs, 2, "0");
}
```

#### 4. Business Logic Module

Separate business logic from UI and data access.

```javascript
// game-logic.ebs - Game-specific logic

// Import dependencies
import "types.ebs";
import "constants.ebs";
import "utils.ebs";

function isValidMove(from: posType, to: posType, board: array) return bool {
    // Validation logic
    if !call isInRange(from.x, 0, BOARD_SIZE - 1) then {
        return false;
    }
    // More validation...
    return true;
}

function calculateScore(board: array) return int {
    var score: int = 0;
    // Scoring logic...
    return score;
}
```

#### 5. Screen/UI Module

Keep screen definitions separate from logic.

```javascript
// main-screen.ebs - Main application screen

// Import required types and logic
import "types.ebs";
import "constants.ebs";
import "game-logic.ebs";

// Screen definition
screen mainScreen = {
    "title": "My Game",
    "width": 800,
    "height": 600,
    "vars": [
        // Variable definitions
    ],
    "area": [
        // Layout definition
    ]
};

// Screen event handlers
function handleStartGame {
    call initializeGame();
    show screen gameScreen;
}

function handleQuitGame {
    call scr.hidescreen("mainScreen");
}
```

### Common Architectural Patterns

#### Pattern 1: Layered Architecture

Organize code into distinct layers with clear dependencies.

```
Application Layer (main.ebs)
    ↓ imports
UI Layer (screens/*.ebs)
    ↓ imports
Business Logic Layer (logic/*.ebs)
    ↓ imports
Data Access Layer (data/*.ebs)
    ↓ imports
Utility Layer (util/*.ebs, types.ebs, constants.ebs)
```

**Example main.ebs:**

```javascript
// Import all layers
import "util/utils.ebs";
import "models/types.ebs";
import "models/constants.ebs";
import "data/database.ebs";
import "logic/game-logic.ebs";
import "screens/main-screen.ebs";

// Application entry point
function main {
    call initializeDatabase();
    call startGame();
}

call main();
```

#### Pattern 2: Module Pattern

Group related functionality into self-contained modules.

```
chess-application/
├── main.ebs
├── chess-types.ebs          # Type definitions
├── chess-constants.ebs      # Constants
├── chess-board.ebs          # Board management
├── chess-moves.ebs          # Move calculation
├── chess-rules.ebs          # Game rules
├── chess-ai.ebs             # AI logic
└── chess-ui.ebs             # UI screens
```

Each module is responsible for a specific aspect of the application.

#### Pattern 3: Main + Helpers Pattern

For simpler applications, use a main file with imported helper modules.

```javascript
// main.ebs - Main application file

// Import helpers
import "validation-helpers.ebs";
import "display-helpers.ebs";
import "calculation-helpers.ebs";

// Main application logic
function startApplication {
    var input = call getUserInput();
    var isValid = call validateInput(input);  // From validation-helpers
    
    if isValid then {
        var result = call performCalculation(input);  // From calculation-helpers
        call displayResult(result);  // From display-helpers
    }
}

call startApplication();
```

### File Organization

#### Single Responsibility Principle

Each file should have a clear, single purpose.

```javascript
// Good: Each file has a clear purpose
// user-validation.ebs - Only user validation functions
// user-database.ebs - Only user database operations
// user-screen.ebs - Only user-related screens

// Bad: Mixing concerns in one file
// user-everything.ebs - Validation, database, screens, logic all mixed
```

#### Naming Conventions

Use clear, descriptive file names:

```javascript
// Good: Descriptive names
chess-move-calculator.ebs
user-authentication.ebs
database-connection.ebs
main-screen.ebs

// Bad: Vague names
utils.ebs
helpers.ebs
misc.ebs
stuff.ebs
```

#### File Size Guidelines

- **Small files (< 200 lines)**: Single-purpose utilities, simple screens
- **Medium files (200-500 lines)**: Complex logic modules, detailed screens
- **Large files (500+ lines)**: Consider splitting into multiple modules

If a file grows beyond 500 lines, consider breaking it down:

```javascript
// Instead of one large file:
chess.ebs (800 lines)

// Split into multiple focused files:
chess-types.ebs (100 lines)
chess-board.ebs (150 lines)
chess-moves.ebs (200 lines)
chess-rules.ebs (150 lines)
chess-ui.ebs (200 lines)
```

---

## Real-World Examples

### Chess Game Structure

The Chess project is an excellent example of a well-structured multi-file application.

#### Directory Structure

```
Chess/
├── chess.ebs              # Main entry point and screen definitions
├── chess-game.ebs         # Core game logic and board management
├── chess-moves.ebs        # Move calculation functions
├── chess.css              # Custom styling
└── test/
    ├── test-chess-click.ebs
    └── test-knight.ebs
```

#### chess-moves.ebs (Module Example)

```javascript
// Chess Move Calculation Functions
// ==================================
// This file contains all the move calculation logic for chess pieces.
// It is imported by chess-game.ebs.

// Define types used in this module
ChessCell typeof bitmap { cellColor: 0, pieceType: 1-6, pieceColor: 7 };
posType typeof record { x: int, y: int };

// Helper function to create a posType record
function createPos(x: int, y: int) return posType {
    var jsonStr: string = '{"x": ' + x + ', "y": ' + y + '}';
    var jsonData: json = call json.jsonfromstring(jsonStr);
    return record(jsonData);
}

// Get all possible moves for a pawn
function getPawnMoves(x: int, y: int, color: int) return array {
    var moves: posType[] = [];
    // Move calculation logic...
    return moves;
}

// More move calculation functions...
```

#### chess-game.ebs (Main Logic)

```javascript
// Import move calculation functions
import "chess-moves.ebs";

// EBS Chess Game Application
// ===========================

// Define piece types (reused from chess-moves.ebs)
ChessPiece typeof record { piece: string, color: string, pos: posType };

// Game state variables
var pieces: ChessPiece[32];
var currentPlayer: int = WHITE;
var selectedPiece: int = -1;

// Constants
var EMPTY: int = 0;
var PAWN: int = 1;
// ... more constants

// Board initialization
function initializeBoard {
    // Initialize pieces array
    // Set up starting positions
}

// Main game logic functions
function handleCellClick(x: int, y: int) {
    // Use functions from chess-moves.ebs
    var moves = call getPawnMoves(x, y, currentPlayer);
    // More logic...
}
```

#### chess.ebs (Entry Point)

```javascript
// Import chess game logic and functions
import "chess-game.ebs";

// Startup dialog screen definition
screen startupDialog = {
    "title": "Chess Game - Start",
    // Screen configuration...
};

// Event handlers for UI
function handleStartGame {
    // Start the game
    call initializeBoard();
    show screen gameScreen;
}

// Show startup dialog
show screen startupDialog;
```

**Key Lessons from Chess:**

1. **Module Separation**: Move calculation logic is separate from game management
2. **Clear Dependencies**: chess-game imports chess-moves, chess.ebs imports chess-game
3. **Type Sharing**: Types defined in chess-moves are used across all files
4. **Focused Files**: Each file has a specific purpose (moves, game logic, UI)

### TicTacToe Structure

The TicTacToe project demonstrates a simpler, single-file approach suitable for smaller applications.

#### Directory Structure

```
TicTacToe/
├── tictactoe.ebs         # Complete game in one file
├── project.json          # Project metadata
└── README.md             # Documentation
```

#### tictactoe.ebs (Single File Example)

```javascript
// Tic Tac Toe Game - Human vs Computer
// ======================================

// Section 1: Constants
var CELL_SIZE: int = 100;
var DRAW_SIZE: int = 90;

// Section 2: Game state variables
var board: string[9];
var gameOver: bool = false;

// Section 3: Canvas cells
var canvas0: canvas = call canvas.create(CELL_SIZE, CELL_SIZE, "canvas0");
// ... more canvas cells

// Section 4: Helper functions
function initBoard {
    for (var i: int = 0; i < 9; i++) {
        board[i] = "";
    }
    gameOver = false;
}

function isEmpty(pos: int) return bool {
    return board[pos] == "";
}

// Section 5: Game logic functions
function checkWinner return string {
    // Winner checking logic
}

// Section 6: AI functions
function makeComputerMove {
    // AI move logic
}

// Section 7: Event handlers
function handleCellClick(position: int) {
    // Handle user click
}

// Section 8: Screen definition
screen gameScreen = {
    // Screen configuration
};

// Section 9: Initialization
call initBoard();
show screen gameScreen;
```

**Key Lessons from TicTacToe:**

1. **Single File Organization**: For simple games, one file with clear sections works well
2. **Section Comments**: Use comments to divide the file into logical sections
3. **Top-Down Structure**: Constants → Variables → Helpers → Logic → UI → Init
4. **When to Split**: When the file exceeds ~500 lines or has independent modules

---

## Best Practices

### 1. Start Simple, Refactor as Needed

Begin with a single file for simple scripts. Split into modules as complexity grows.

```javascript
// Phase 1: Single file (< 300 lines)
simple-game.ebs

// Phase 2: Split logic (300-800 lines)
main.ebs
game-logic.ebs

// Phase 3: Modular structure (> 800 lines)
main.ebs
types.ebs
constants.ebs
game-logic.ebs
move-calculator.ebs
ui-screens.ebs
```

### 2. Use Comments for Section Organization

Even in single files, use clear section comments.

```javascript
// ===========================
// Type Definitions
// ===========================

// ... type definitions here

// ===========================
// Constants
// ===========================

// ... constants here

// ===========================
// Utility Functions
// ===========================

// ... utility functions here
```

### 3. Keep Imports Clean

Avoid importing unnecessary files. Only import what you need.

```javascript
// Good: Only import what's needed
import "user-validation.ebs";

// Bad: Importing everything "just in case"
import "util/string.ebs";
import "util/math.ebs";
import "util/date.ebs";
import "lib/database.ebs";
// ... when you only need validation
```

### 4. Document Module Dependencies

Add a comment at the top of files documenting what they depend on.

```javascript
// chess-moves.ebs
// ===============
// Move calculation functions for chess pieces
//
// Dependencies:
//   - chess-types.ebs: ChessCell, posType definitions
//   - chess-constants.ebs: EMPTY, PAWN, etc.
//   - chess-board.ebs: getPieceAt(), isValidPosition()
//
// Imported by:
//   - chess-game.ebs
```

### 5. Use Type Aliases for Complex Types

Define type aliases in a shared module for consistency.

```javascript
// types.ebs
PlayerType typeof record {
    id: int,
    name: string,
    score: int,
    color: int
};

MoveType typeof record {
    from: posType,
    to: posType,
    piece: int,
    timestamp: date
};
```

### 6. Consistent Naming Across Modules

Use consistent naming conventions across all files.

```javascript
// All validation functions start with "validate"
function validateEmail(email: string) return bool { }
function validateAge(age: int) return bool { }

// All database functions start with "db"
function dbConnect(config: record) return bool { }
function dbQuery(sql: string) return array { }

// All screen functions start with "show" or "hide"
function showMainScreen { }
function hideSettingsScreen { }
```

### 7. Minimize Global State

Keep global variables to a minimum. Prefer passing parameters.

```javascript
// Good: Pass board as parameter
function checkWinner(board: array) return string {
    // Check winner using passed board
}

// Less flexible: Use global board
var globalBoard: array;
function checkWinner return string {
    // Check winner using globalBoard
}
```

### 8. Test Modules Independently

Create test files for each module.

```
project/
├── logic/
│   ├── game-logic.ebs
│   └── validation.ebs
└── test/
    ├── test-game-logic.ebs
    └── test-validation.ebs
```

---

## Common Pitfalls

### 1. Circular Dependencies with Shared State

**Problem:** Two modules that import each other and modify shared state.

```javascript
// file1.ebs
import "file2.ebs";
var sharedCounter: int = 0;

function incrementFromFile1 {
    sharedCounter++;
}

// file2.ebs
import "file1.ebs";

function incrementFromFile2 {
    sharedCounter++;  // Modifying state from file1
}
```

**Solution:** Extract shared state to a separate module.

```javascript
// state.ebs
var sharedCounter: int = 0;

// file1.ebs
import "state.ebs";
function incrementFromFile1 {
    sharedCounter++;
}

// file2.ebs
import "state.ebs";
function incrementFromFile2 {
    sharedCounter++;
}
```

### 2. Import Order Dependencies

**Problem:** Importing files in the wrong order causes missing definitions.

```javascript
// main.ebs
import "game-logic.ebs";  // Uses types from types.ebs
import "types.ebs";        // Too late! game-logic already tried to use these
```

**Solution:** Import dependencies before dependents, or define types first.

```javascript
// main.ebs
import "types.ebs";        // Import types first
import "game-logic.ebs";   // Then import code that uses those types
```

Or define types in main file before importing:

```javascript
// main.ebs
// Define types first
posType typeof record { x: int, y: int };

// Then import code that uses these types
import "game-logic.ebs";
```

### 3. Overly Deep Directory Structures

**Problem:** Complex nested directories make imports difficult.

```javascript
import "../../../shared/utils/string/formatting/helpers.ebs";
```

**Solution:** Keep directory structure shallow (2-3 levels max).

```javascript
import "util/string-format.ebs";
```

### 4. Monolithic Files

**Problem:** One file doing too many things becomes hard to maintain.

```javascript
// game.ebs - 2000 lines doing everything:
// - Type definitions
// - Constants
// - Board logic
// - AI logic
// - Move validation
// - UI screens
// - Database operations
// - Network code
```

**Solution:** Split into focused modules.

```javascript
game-types.ebs       (100 lines)
game-constants.ebs   (50 lines)
game-board.ebs       (200 lines)
game-ai.ebs          (300 lines)
game-moves.ebs       (250 lines)
game-ui.ebs          (400 lines)
game-database.ebs    (200 lines)
game-network.ebs     (300 lines)
main.ebs             (200 lines)
```

### 5. Inconsistent Naming

**Problem:** Different naming styles across modules.

```javascript
// file1.ebs
function validateUserInput { }
function PROCESS_DATA { }

// file2.ebs
function check_email { }
function CalculateScore { }
```

**Solution:** Choose one style and use it everywhere (recommend camelCase).

```javascript
// All files use camelCase
function validateUserInput { }
function processData { }
function checkEmail { }
function calculateScore { }
```

### 6. Not Using Type Aliases

**Problem:** Repeating complex type definitions everywhere.

```javascript
// In multiple files
var user1: record { name: string, age: int, email: string };
var user2: record { name: string, age: int, email: string };
```

**Solution:** Define type alias once, use everywhere.

```javascript
// types.ebs
UserType typeof record { name: string, age: int, email: string };

// Use in all files
var user1: UserType;
var user2: UserType;
```

---

## Troubleshooting

### Import Not Found

**Symptom:** Error message: "Cannot find imported file: xyz.ebs"

**Solutions:**

1. **Check the path**: Ensure the path is correct relative to the importing file.
   ```javascript
   // If main.ebs is in project/ and helper.ebs is in project/util/
   import "util/helper.ebs";  // Correct
   import "helper.ebs";        // Wrong - file not in same directory
   ```

2. **Check file extension**: Always include the `.ebs` extension.
   ```javascript
   import "helper.ebs";   // Correct
   import "helper";       // Wrong - missing extension
   ```

3. **Check for typos**: File names must match exactly (though case doesn't matter).
   ```javascript
   // File is named: stringUtils.ebs
   import "stringUtils.ebs";   // Correct
   import "stringUtils.ebs";   // Correct (case insensitive)
   import "stringUtil.ebs";    // Wrong - missing 's'
   ```

### Function Not Defined

**Symptom:** Error calling a function that should be imported.

**Solutions:**

1. **Check import order**: Import the file before using its functions.
   ```javascript
   // Wrong order
   var result = call helperFunction();  // Error: function not defined
   import "helper.ebs";
   
   // Correct order
   import "helper.ebs";
   var result = call helperFunction();  // Works
   ```

2. **Verify function is in imported file**: Make sure the function exists in the file you're importing.

3. **Check for circular dependencies**: If two files import each other, one might not see the other's functions.

### Type Not Defined

**Symptom:** Error using a type that should be imported.

**Solutions:**

1. **Define types before importing**: If imported code uses types, define them first.
   ```javascript
   // Define type
   posType typeof record { x: int, y: int };
   
   // Then import code that uses this type
   import "game-logic.ebs";
   ```

2. **Import type definitions file first**: If types are in a separate file, import it before other files.
   ```javascript
   import "types.ebs";        // Types defined here
   import "game-logic.ebs";   // Uses types from types.ebs
   ```

### Duplicate Definitions

**Symptom:** Error about a function or variable being defined multiple times.

**Solutions:**

1. **Check for duplicate imports**: Make sure you're not importing the same file multiple times (shouldn't cause issues due to circular import protection, but good to avoid).

2. **Check for duplicate definitions**: Ensure functions aren't defined in both the main file and imported files.

3. **Use unique names**: If you need similar functions from different modules, use unique names.
   ```javascript
   // In string-util.ebs
   function formatString(s: string) return string { }
   
   // In date-util.ebs
   function formatDate(d: date) return string { }
   
   // Not both named 'format'
   ```

### Performance Issues with Large Projects

**Symptom:** Application is slow to start or runs slowly.

**Solutions:**

1. **Lazy load screens**: Don't define all screens at startup. Show screens only when needed.
   ```javascript
   // Instead of defining all screens at startup
   // Define and show screens on demand
   function showSettings {
       screen settingsScreen = { /* definition */ };
       show screen settingsScreen;
   }
   ```

2. **Optimize imports**: Don't import modules you don't need.

3. **Profile performance**: Use timing to identify slow functions.
   ```javascript
   var startTime: date = now();
   call slowFunction();
   var endTime: date = now();
   var elapsed: long = call date.getElapsedMillis(startTime, endTime);
   print "Function took " + elapsed + "ms";
   ```

---

## Summary

### Key Takeaways

1. **Functions**: Use clear naming, type annotations, and organize logically
2. **Imports**: Place at the top, use relative paths, avoid circular dependencies
3. **Structure**: Start simple, split into modules as complexity grows
4. **Organization**: Use consistent naming, clear separation of concerns
5. **Best Practices**: Document dependencies, minimize global state, test independently

### Quick Reference Card

```javascript
// Function Definition
function functionName(param: type) return returnType {
    // implementation
}

// Import Statement
import "relative/path/to/file.ebs";

// Type Alias
TypeName typeof type_definition;

// Project Structure (Medium App)
main.ebs
types.ebs
constants.ebs
util/
    utils.ebs
logic/
    business-logic.ebs
screens/
    main-screen.ebs
```

### Next Steps

1. Review the [Chess project](../../projects/Chess/) for a real-world multi-file example
2. Check [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) for complete syntax reference
3. Read [COMPLEX_DATA_TYPES_GUIDE.md](COMPLEX_DATA_TYPES_GUIDE.md) for advanced data structure patterns
4. Explore [SCREEN_DEFINITION_BEST_PRACTICES.md](SCREEN_DEFINITION_BEST_PRACTICES.md) for UI best practices

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Maintained by:** EBS Script Interpreter Team
