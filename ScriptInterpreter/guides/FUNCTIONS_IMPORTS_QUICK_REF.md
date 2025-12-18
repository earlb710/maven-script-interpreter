# Functions and Imports Quick Reference

A condensed reference guide for functions and imports in EBS. For detailed explanations, see [FUNCTIONS_AND_IMPORTS_GUIDE.md](FUNCTIONS_AND_IMPORTS_GUIDE.md).

---

## Function Syntax

### Basic Function

```javascript
// Without 'function' keyword
greet {
    print "Hello!";
}

// With 'function' keyword
function greet {
    print "Hello!";
}

call greet;
```

### Function with Parameters and Return

```javascript
add(a: int, b: int) return int {
    return a + b;
}

var sum = call add(5, 3);
```

### Default Parameters

```javascript
function greet(name: string = "World") return string {
    return "Hello, " + name + "!";
}

var msg1 = call greet();          // "Hello, World!"
var msg2 = call greet("Alice");   // "Hello, Alice!"
```

### Named Parameters

```javascript
divide(dividend: int, divisor: int) return double {
    return dividend / divisor;
}

var result = call divide(divisor = 4, dividend = 20);
```

---

## Import Syntax

### Basic Import

```javascript
// Import from same directory
import "helper.ebs";

// Import from subdirectory
import "util/stringUtil.ebs";

// Import with spaces (use quotes)
import "my utils/helper.ebs";

// Single or double quotes
import 'lib/database.ebs';
```

### Import Best Practices

```javascript
// ✓ Good: Imports at the top
import "util/math.ebs";
import "util/string.ebs";

// Type definitions
posType typeof record { x: int, y: int };

// Rest of code...
```

### Import After Dependencies (When Needed)

```javascript
// Define types first
ChessPiece typeof record { piece: string, color: string };
var WHITE: int = 0;

// Then import code that uses these types
import "chess-moves.ebs";
```

---

## Function Naming Conventions

```javascript
// ✓ Verb-noun pattern
function calculateTotal(items: array) return double { }
function getUserData(userId: int) return record { }
function validateInput(input: string) return bool { }

// ✓ Boolean functions: is/has/can/should
function isValidEmail(email: string) return bool { }
function hasPermission(user: record) return bool { }
function canMove(piece: record) return bool { }

// ✓ Get/Set pattern
function getUserName return string { }
function setUserName(name: string) { }
```

---

## Project Structure Templates

### Small Project (Single File)

```
my-project/
└── main.ebs          # All code in one file (< 300 lines)
```

### Medium Project (Type-Based)

```
my-project/
├── main.ebs          # Entry point
├── types.ebs         # Type definitions
├── constants.ebs     # Constants
├── util/
│   └── utils.ebs     # Utility functions
├── logic/
│   └── business.ebs  # Business logic
└── screens/
    └── main-ui.ebs   # UI screens
```

### Large Project (Feature-Based)

```
my-project/
├── main.ebs
├── shared/
│   ├── types.ebs
│   ├── constants.ebs
│   └── utils.ebs
├── features/
│   ├── user/
│   │   ├── user-types.ebs
│   │   ├── user-logic.ebs
│   │   └── user-screen.ebs
│   └── game/
│       ├── game-types.ebs
│       ├── game-logic.ebs
│       └── game-screen.ebs
└── test/
    ├── test-user.ebs
    └── test-game.ebs
```

---

## Common Patterns

### Pattern: Separate Types Module

```javascript
// types.ebs
posType typeof record { x: int, y: int };
UserType typeof record { id: int, name: string };

// main.ebs
import "types.ebs";
var pos: posType;
var user: UserType;
```

### Pattern: Constants Module

```javascript
// constants.ebs
var WHITE: int = 0;
var BLACK: int = 1;
var BOARD_SIZE: int = 8;

// main.ebs
import "constants.ebs";
var currentPlayer: int = WHITE;
```

### Pattern: Utility Functions

```javascript
// utils.ebs
function clamp(value: int, min: int, max: int) return int {
    if value < min then return min;
    if value > max then return max;
    return value;
}

// main.ebs
import "utils.ebs";
var clamped = call clamp(15, 0, 10);  // Returns 10
```

### Pattern: Return Multiple Values (Record)

```javascript
ResultType typeof record { success: bool, message: string, value: int };

function parseInteger(text: string) return ResultType {
    try {
        var value: int = int(text);
        return ResultType { success: true, message: "OK", value: value };
    } exceptions {
        when ANY_ERROR(msg) {
            return ResultType { success: false, message: msg, value: 0 };
        }
    }
}

var result = call parseInteger("42");
if result.success then {
    print result.value;
}
```

---

## Common Pitfalls

### ✗ Wrong Import Order

```javascript
// ✗ Wrong: Import before types are defined
import "game-logic.ebs";  // Uses posType
posType typeof record { x: int, y: int };  // Too late!
```

```javascript
// ✓ Correct: Define types first
posType typeof record { x: int, y: int };
import "game-logic.ebs";  // Now posType is available
```

### ✗ Missing File Extension

```javascript
// ✗ Wrong: Missing .ebs extension
import "helper";

// ✓ Correct: Include extension
import "helper.ebs";
```

### ✗ Monolithic File

```javascript
// ✗ Wrong: Everything in one 2000-line file
main.ebs (2000 lines)

// ✓ Correct: Split into modules
main.ebs (200 lines)
types.ebs (100 lines)
logic.ebs (300 lines)
ui.ebs (400 lines)
```

---

## Troubleshooting

### Import Not Found

```
Error: Cannot find imported file: xyz.ebs
```

**Solutions:**
1. Check path is relative to importing file
2. Include `.ebs` extension
3. Check for typos in filename

### Function Not Defined

```
Error: Function 'xyz' is not defined
```

**Solutions:**
1. Import the file before calling the function
2. Verify function exists in imported file
3. Check for circular dependency issues

### Type Not Defined

```
Error: Type 'xyz' is not defined
```

**Solutions:**
1. Define type before importing code that uses it
2. Import types file before other modules
3. Check type name spelling

---

## File Organization Rules

### File Size Guidelines

- **< 200 lines**: Keep in single file
- **200-500 lines**: Consider splitting into 2-3 modules
- **> 500 lines**: Definitely split into multiple focused modules

### Module Responsibility

Each file should have ONE primary responsibility:

- **types.ebs**: Type definitions only
- **constants.ebs**: Constants only
- **validation.ebs**: Validation functions only
- **database.ebs**: Database operations only
- **ui-screens.ebs**: Screen definitions only

### File Naming

```javascript
// ✓ Good: Descriptive, clear purpose
user-validation.ebs
chess-move-calculator.ebs
database-connection.ebs

// ✗ Bad: Vague, unclear purpose
utils.ebs
helpers.ebs
misc.ebs
```

---

## Layered Architecture Example

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
// Import layers bottom-up
import "util/utils.ebs";
import "types.ebs";
import "constants.ebs";
import "data/database.ebs";
import "logic/game-logic.ebs";
import "screens/main-screen.ebs";

function main {
    call initializeDatabase();
    call startGame();
}

call main();
```

---

## Function Organization in File

```javascript
// Section 1: Type Definitions
posType typeof record { x: int, y: int };

// Section 2: Constants
var BOARD_SIZE: int = 8;

// Section 3: Helper/Utility Functions
function isValidPosition(x: int, y: int) return bool {
    return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
}

// Section 4: Main Logic Functions
function calculateMove(from: posType, to: posType) return bool {
    // Implementation
}

// Section 5: Screen Definitions (if applicable)
screen mainScreen = {
    // Screen definition
};

// Section 6: Initialization/Entry Point
call initialize();
show screen mainScreen;
```

---

## Real-World Example: Chess Project

```
Chess/
├── chess.ebs              # Entry point, startup screen
├── chess-game.ebs         # Main game logic, board state
├── chess-moves.ebs        # Move calculation functions
└── chess.css              # Styling

Import chain:
chess.ebs
    ↓ imports
chess-game.ebs
    ↓ imports
chess-moves.ebs
```

**Key points:**
- Types defined in chess-moves.ebs (used by all)
- Constants defined in chess-game.ebs
- Clear separation: UI → Game Logic → Move Calculation

---

## Quick Tips

1. **Start simple**: Single file → Split as needed
2. **Import at top**: Unless types need to be defined first
3. **One responsibility**: Each file does ONE thing well
4. **Clear names**: File and function names should be self-documenting
5. **Group imports**: Organize by category (core, utils, logic, UI)
6. **Document dependencies**: Comment what each file needs/provides
7. **Test independently**: Each module should be testable alone
8. **Keep it flat**: Max 2-3 directory levels

---

## Related Documentation

- **[FUNCTIONS_AND_IMPORTS_GUIDE.md](FUNCTIONS_AND_IMPORTS_GUIDE.md)** - Complete detailed guide
- **[EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md)** - Language syntax reference
- **[COMPLEX_DATA_TYPES_GUIDE.md](COMPLEX_DATA_TYPES_GUIDE.md)** - Arrays and records
- **[SCREEN_DEFINITION_BEST_PRACTICES.md](SCREEN_DEFINITION_BEST_PRACTICES.md)** - UI best practices

---

**Quick Reference Version:** 1.0  
**Last Updated:** 2025-12-18
