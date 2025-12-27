# EBS Script Syntax Reference

## Table of Contents
1. [Overview](#overview)
2. [Basic Syntax](#basic-syntax)
3. [Data Types](#data-types)
4. [Variables](#variables)
5. [Operators](#operators)
6. [Control Flow](#control-flow)
7. [Exception Handling](#exception-handling)
8. [Code Organization](#code-organization)
9. [Functions](#functions)
10. [Arrays](#arrays)
11. [JSON](#json)
12. [Database Operations](#database-operations)
13. [Screen/UI Windows](#screenui-windows)
14. [Built-in Functions](#built-in-functions)
15. [Comments](#comments)
16. [Console Commands](#console-commands)

---

## Overview

EBS (Earl Bosch Script) is a dynamically-typed scripting language with:
- Familiar C-like syntax
- Native JSON support
- Integrated database capabilities
- JavaFX-based UI screen support
- Rich set of built-in functions
- Interactive REPL environment

### Quick References
For a comprehensive overview of collection types (arrays, queues, maps, JSON), see **[EBS Collections Reference](EBS_COLLECTIONS_REFERENCE.md)**.

## Basic Syntax

### Program Structure
```javascript
// Comments start with //
var message: string = "Hello, World!";
print message;
```

### Statement Termination
- Most statements end with a semicolon `;`
- Blocks use curly braces `{ }`
- Comments start with `//` and extend to end of line

### Case Insensitivity
**Important**: EBS is case-insensitive for all identifiers (variables, functions, screens).

```javascript
// These all refer to the same variable
var userName = "Alice";
print USERNAME;      // Works: prints "Alice"
print UserName;      // Works: prints "Alice"
print username;      // Works: prints "Alice"

// Screen names are also case-insensitive
screen myScreen = { "title": "Example" };
show screen MyScreen;    // Works
show screen MYSCREEN;    // Works
```

All identifiers are normalized to lowercase internally, so `myVariable`, `MyVariable`, and `MYVARIABLE` all refer to the same entity. However, string values remain case-sensitive.

---

## Data Types

### Primitive Types
| Type | Description | Example |
|------|-------------|---------|
| `int` / `integer` | 32-bit integer | `42` |
| `long` | 64-bit integer | `9999999999` |
| `float` | Single-precision float | `3.14` |
| `double` | Double-precision float | `3.14159265359` |
| `byte` | 8-bit integer | `127` |
| `string` | Text string | `"Hello"` |
| `bool` / `boolean` | Boolean value | `true`, `false` |
| `date` | Date/time value | `now()` |
| `json` | JSON object/array | `{"key": "value"}` |
| `map` | Key-value map (JSON objects only) | `{"key": "value"}` |
| `record` | Structured type with named fields | `record {name: string, age: int}` |
| `bitmap` | Byte with named bit fields (8-bit, 0-7) | `bitmap { flag: 0, status: 1-3 }` |
| `intmap` | Integer with named bit fields (32-bit, 0-31) | `intmap { enabled: 0, mode: 1-7, id: 8-31 }` |
| `imagedata` | Binary image data for display and manipulation | Image data |
| `canvas` | Canvas graphics type | Canvas drawing surface |
| `array` | Generic array | `array[10]`, `array[*]` |
| `queue` | FIFO queue (use `queue.type`) | `queue.string`, `queue.int` |

### Type Inference
```javascript
var count = 42;           // Inferred as int
var name = "Alice";       // Inferred as string
var ratio = 3.14;         // Inferred as double
var flag = true;          // Inferred as bool
```

### Type Casting
Explicitly convert values between types using type casting syntax: `type(value)`

#### Basic Type Casting
```javascript
// String to numeric types
var num: int = int("42");                 // String to int
var bigNum: long = long("9999999999");    // String to long
var decimal: float = float("3.14");       // String to float
var precise: double = double("3.14159");  // String to double
var small: byte = byte("127");            // String to byte

// Numeric to string
var text: string = string(123);           // Int to string
var floatText: string = string(3.14);     // Float to string

// String to boolean
var flag: bool = boolean("true");         // String to boolean
var flag2: bool = bool("false");          // Alternative syntax

// Numeric conversions
var truncated: int = int(3.14);           // Double to int (truncates): 3
var asFloat: float = float(42);           // Int to float: 42.0
var asDouble: double = double(3.14f);     // Float to double
```

#### Type Casting in Expressions
```javascript
// Arithmetic with casting
var sum: int = int("10") + int("20");     // Result: 30

// Conditionals with casting
if double(x) / double(y) > 0.5 then {
    print "Ratio exceeds threshold";
}

// Chained conversions
var result: string = string(int("99"));   // "99" -> 99 -> "99"
```

#### Record Casting from JSON
Cast JSON objects to records with automatic type inference:

```javascript
// Auto-inferred record type
var jsonData: json = {"name": "Alice", "age": 30};
var person = record(jsonData);
print typeof person;  // Output: record {name:string, age:int}

// Explicit record type with validation
var userData: json = {"name": "Bob", "age": 25, "email": "bob@example.com"};
var user: record {name: string, age: int} = record(userData);
// Extra fields in JSON are allowed, only required fields are validated

// Nested records
var data: json = {"user": "Charlie", "settings": {"theme": "dark", "volume": 80}};
var config = record(data);
print typeof config;  // Output: record {user:string, settings:record}

// Error handling
var invalidData: json = {"name": "Diana"};  // Missing 'age' field
var employee: record {name: string, age: int} = record(invalidData);
// Error: Required field 'age' is missing from JSON object

// Arrays are rejected
var arrayData: json = [1, 2, 3];
var rec = record(arrayData);
// Error: Cannot cast JSON array to record. Only JSON objects (maps) can be cast to record type.
```

**Record Casting Features:**
- Automatically infers RecordType from JSON structure
- Validates required fields when explicit type is provided
- Extra fields in JSON are allowed
- Supports nested records
- Case-insensitive field matching
- Clear error messages for validation failures

#### Map Type and JSON to Map Casting
The `map` type represents a key-value store where keys are strings and values can be any type. Maps are backed by JSON objects and can only be created from JSON objects (not arrays).

EBS supports two types of maps:
- **Normal maps** (`map`): Maintain insertion order (LinkedHashMap)
- **Sorted maps** (`sorted map`): Automatically sort keys alphabetically (TreeMap)

```javascript
// Normal map (maintains insertion order)
var myMap: map = {"z": 3, "a": 1, "m": 2};
// When iterating: z=3, a=1, m=2 (insertion order)

// Sorted map (maintains alphabetical order)
var sortedMap: sorted map = {"z": 3, "a": 1, "m": 2};
// When iterating: a=1, m=2, z=3 (alphabetical order)

// Cast JSON object to map
var jsonData: json = {"name": "Alice", "age": 30, "city": "New York"};
var personMap = map(jsonData);

// Access and modify map values using json functions
var name = call json.get(personMap, "name");          // "Alice"
var age = call json.getint(personMap, "age");         // 30
call json.set(personMap, "city", "Los Angeles");      // Modify value
call json.set(personMap, "country", "USA");           // Add new key

// Convert between normal and sorted maps
var toSorted = call map.toSorted(myMap);              // Convert to sorted map
var toNormal = call map.toUnsorted(sortedMap);        // Convert to normal map

// Nested maps
var nestedJson: json = {
    "person": {
        "name": "Bob",
        "address": {"street": "123 Main St", "zip": "12345"}
    }
};
var nestedMap = map(nestedJson);

// Error: JSON arrays cannot be cast to map
var arrayData: json = [1, 2, 3];
var badCast = map(arrayData);
// Error: Cannot cast JSON array to map. Only JSON objects can be cast to map type.
```

**Map vs Record vs JSON:**
- **map**: A flexible key-value store. No predefined field structure. JSON objects only.
- **record**: A structured type with predefined field names and types. Provides type validation.
- **json**: Can hold any JSON value including objects, arrays, strings, numbers, booleans, or null.

**When to use each:**
- Use `map` when you need a flexible key-value store with string keys
- Use `record` when you need type-safe access to known fields
- Use `json` when you need to work with any JSON structure including arrays

#### Supported Type Aliases
- `int()` / `integer()` - Integer casting
- `long()` - Long integer casting
- `float()` - Float casting
- `double()` - Double casting
- `string()` - String casting
- `byte()` - Byte casting
- `boolean()` / `bool()` - Boolean casting
- `record()` - JSON to record casting
- `map()` - JSON object to map casting

#### Bitmap Type
The `bitmap` type defines named fields that map to bit ranges within a byte (0-7). This allows compact storage of multiple small values within a single byte, useful for flags, status bits, or packing multiple small integers.

```javascript
// Define a bitmap variable with named bit fields
// Each field specifies which bit(s) it occupies in the byte
var flags: bitmap { status: 0-1, enabled: 2, priority: 3-5, reserved: 6-7 };

// Field definitions:
// - status: 2 bits (0-1), can hold values 0-3
// - enabled: 1 bit (2), can hold values 0-1 (boolean-like)
// - priority: 3 bits (3-5), can hold values 0-7
// - reserved: 2 bits (6-7), can hold values 0-3

// Initially, the bitmap value is 0
print flags;  // Output: 0

// Set individual fields using property access
flags.status = 2;
flags.enabled = 1;
flags.priority = 5;
flags.reserved = 3;

// Read individual fields (values are automatically right-shifted)
print flags.status;    // Output: 2
print flags.enabled;   // Output: 1
print flags.priority;  // Output: 5
print flags.reserved;  // Output: 3

// Fields can be used in expressions
if (flags.enabled == 1) {
    print "The enabled flag is set!";
}

// Const bitmap for read-only bit masks
const PERMISSIONS: bitmap { read: 0, write: 1, execute: 2, admin: 3 } = 5;
// 5 = binary 0101 = read(1), write(0), execute(1), admin(0)
print PERMISSIONS.read;     // Output: 1
print PERMISSIONS.write;    // Output: 0
print PERMISSIONS.execute;  // Output: 1
print PERMISSIONS.admin;    // Output: 0
```

**Bitmap Field Syntax:**
- Single bit: `fieldName: bitPosition` (e.g., `enabled: 2`)
- Bit range: `fieldName: startBit-endBit` (e.g., `status: 0-1`)
- Bit positions must be 0-7 (within a byte)
- Fields cannot overlap

**Bitmap Features:**
- Automatic right-shift when reading field values
- Automatic left-shift and masking when writing field values
- Value validation against field bit width
- Case-insensitive field access (exact case matches are checked first)
- Fields are stored in a single byte

#### Bitmap Type Aliases and Casting

Define reusable bitmap types using the `typeof` keyword, then cast byte or int values to the bitmap type:

```javascript
// Define a reusable bitmap type alias
StatusFlags typeof bitmap { active: 0, error: 1, warning: 2, ready: 3, busy: 4-5 };

// Cast a byte value to the bitmap type
var byteValue: byte = 42;
var status = StatusFlags(byteValue);

// Access fields on the casted value
print status.active;    // Output: 0
print status.error;     // Output: 1
print status.warning;   // Output: 0
print status.ready;     // Output: 1
print status.busy;      // Output: 2

// Also works with integer values
var intValue: int = 255;
var allSet = StatusFlags(intValue);
print allSet.busy;      // Output: 3 (all bits set)
```

#### Bitmap Bit Position Verification

The bit positioning follows standard conventions:
- `flags.status=2` sets bits 0-1 to value 2, so `flags=2`
- `flags.enabled=1` sets bit 2 to value 1, so `flags=4` (1 << 2 = 4)
- `flags.priority=1` sets bits 3-5 to value 1, so `flags=8` (1 << 3 = 8)

```javascript
BitPos typeof bitmap { status: 0-1, enabled: 2, priority: 3-5 };

var test1 = BitPos(0);
test1.status = 2;
print test1;  // Output: 2

var test2 = BitPos(0);
test2.enabled = 1;
print test2;  // Output: 4

var test3 = BitPos(0);
test3.priority = 1;
print test3;  // Output: 8
```

#### typeof with Bitmap Types

The `typeof` operator works with both bitmap type aliases and bitmap variables:

```javascript
StatusFlags typeof bitmap { active: 0, error: 1, warning: 2 };
var status = StatusFlags(42);

// typeof on a type alias returns the full bitmap definition
print typeof StatusFlags;   // Output: bitmap {active: 0, error: 1, warning: 2}

// typeof on a bitmap variable returns "bitmap <aliasname>"
print typeof status;        // Output: bitmap statusflags
```

#### Intmap Type

The `intmap` type extends the bitmap concept to 32-bit integers, defining named fields that map to bit ranges within an integer (0-31). This allows storing larger values and more fields than bitmap, making it ideal for packing multiple values, IDs, flags, or configuration data into a single integer.

```javascript
// Define an intmap variable with named bit fields
// Each field specifies which bit(s) it occupies in the integer (0-31)
var config: intmap { enabled: 0, mode: 1-3, level: 4-11, id: 12-31 };

// Field definitions:
// - enabled: 1 bit (0), can hold values 0-1 (boolean-like)
// - mode: 3 bits (1-3), can hold values 0-7
// - level: 8 bits (4-11), can hold values 0-255
// - id: 20 bits (12-31), can hold values 0-1048575

// Initially, the intmap value is 0
print config;  // Output: 0

// Set individual fields using property access
config.enabled = 1;
config.mode = 5;
config.level = 128;
config.id = 65535;

// Read individual fields (values are automatically right-shifted)
print config.enabled;   // Output: 1
print config.mode;      // Output: 5
print config.level;     // Output: 128
print config.id;        // Output: 65535

// Fields can be used in expressions
if (config.enabled == 1) {
    print "Configuration is enabled";
}

if (config.level > 100) {
    print "High level: " + config.level;
}

// Const intmap for read-only configurations
const FLAGS: intmap { read: 0, write: 1, execute: 2, admin: 3-5 } = 15;
// 15 = binary ...01111 = read(1), write(1), execute(1), admin(1)
print FLAGS.read;     // Output: 1
print FLAGS.write;    // Output: 1
print FLAGS.execute;  // Output: 1
print FLAGS.admin;    // Output: 1
```

**Intmap Field Syntax:**
- Single bit: `fieldName: bitPosition` (e.g., `enabled: 0`)
- Bit range: `fieldName: startBit-endBit` (e.g., `id: 0-15`)
- Bit positions must be 0-31 (within a 32-bit integer)
- Fields cannot overlap

**Intmap vs Bitmap:**
- **bitmap**: 8-bit storage (byte), bit positions 0-7, maximum 255 as an unsigned byte value
- **intmap**: 32-bit storage (integer), bit positions 0-31, uses signed 32-bit integer (-2,147,483,648 to 2,147,483,647)
- Use bitmap for small flags and values (saves memory)
- Use intmap when you need more bits or larger field values

**Intmap Features:**
- Automatic right-shift when reading field values
- Automatic left-shift and masking when writing field values
- Value validation against field bit width
- Case-insensitive field access (exact case matches are checked first)
- Fields are stored in a single 32-bit integer

#### Intmap Type Aliases and Casting

Define reusable intmap types using the `typeof` keyword, then cast int values to the intmap type:

```javascript
// Define a reusable intmap type alias
PackedData typeof intmap { id: 0-15, count: 16-23, flags: 24-31 };

// Cast an integer value to the intmap type
var intValue: int = 16842752;
var data = PackedData(intValue);

// Access fields on the casted value
print data.id;      // Output: 0
print data.count;   // Output: 1
print data.flags;   // Output: 1

// Initialize with a specific value
var config = PackedData(0);
config.id = 65535;      // Max value for 16 bits
config.count = 255;     // Max value for 8 bits
config.flags = 170;     // Binary pattern
print config;           // Output: packed integer value
```

#### Intmap Bit Position Verification

The bit positioning follows standard conventions (same as bitmap but extended to 32 bits):
- `config.enabled=1` sets bit 0 to value 1, so the integer value includes 1
- `config.mode=5` sets bits 1-3 to value 5, so the integer value includes (5 << 1)
- `config.id=1000` sets bits 12-31 to value 1000, so the integer value includes (1000 << 12)

```javascript
PackedFlags typeof intmap { status: 0-1, enabled: 2, priority: 3-7 };

var test1 = PackedFlags(0);
test1.status = 2;
print test1;  // Output: 2

var test2 = PackedFlags(0);
test2.enabled = 1;
print test2;  // Output: 4

var test3 = PackedFlags(0);
test3.priority = 10;
print test3;  // Output: 80 (10 << 3)
```

#### typeof with Intmap Types

The `typeof` operator works with both intmap type aliases and intmap variables:

```javascript
ConfigData typeof intmap { enabled: 0, mode: 1-3, level: 4-11 };
var config = ConfigData(42);

// typeof on a type alias returns the full intmap definition
print typeof ConfigData;   // Output: intmap {enabled: 0, mode: 1-3, level: 4-11}

// typeof on an intmap variable returns "intmap <aliasname>"
print typeof config;       // Output: intmap configdata
```

### Null Values
```javascript
var empty = null;
if empty == null then {
    print "Value is null";
}
```

### Record Literal Syntax

EBS supports a clean, readable syntax for initializing records using type aliases. This syntax eliminates the need for quotes around field names, making code more concise and less ambiguous.

#### Basic Record Literal

```javascript
// Define a type alias
posType typeof record { x: int, y: int };

// Initialize using record literal syntax (no quotes on field names)
var position = posType { x: 10, y: 20 };

print position.x;  // Output: 10
print position.y;  // Output: 20
```

#### Nested Record Literals

Record literals support nesting recursively, allowing complex data structures to be initialized cleanly:

```javascript
// Define nested type aliases
addressType typeof record { street: string, city: string, zipCode: string };
contactType typeof record { email: string, phone: string, address: addressType };
personType typeof record { name: string, age: int, contact: contactType };

// Initialize with nested record literals
var person = personType {
    name: "Jane Smith",
    age: 30,
    contact: contactType {
        email: "jane@example.com",
        phone: "555-1234",
        address: addressType {
            street: "123 Main St",
            city: "Springfield",
            zipCode: "12345"
        }
    }
};

// Access nested fields
print person.contact.address.city;  // Output: Springfield
```

#### Record Literals in Arrays

Record literals work seamlessly with array assignments:

```javascript
// Define type aliases
posType typeof record { x: int, y: int };
ChessPiece typeof record { piece: string, color: string, pos: posType };

// Initialize array with record literals
var pieces: ChessPiece[3];
pieces[0] = ChessPiece { piece: "K", color: "W", pos: posType { x: 4, y: 0 } };
pieces[1] = ChessPiece { piece: "Q", color: "W", pos: posType { x: 3, y: 0 } };
pieces[2] = ChessPiece { piece: "R", color: "W", pos: posType { x: 0, y: 0 } };

print pieces[0].piece;  // Output: K
```

#### Comparison: Record Literal vs JSON Syntax

```javascript
// Traditional JSON object syntax (with quotes on keys)
var position1 = {"x": 10, "y": 20};

// New record literal syntax (no quotes on keys)
var position2 = posType { x: 10, y: 20 };

// Both create the same internal representation
// But record literal syntax provides:
// - Type validation at parse time
// - Cleaner, more readable code
// - Less ambiguity (clearly indicates a typed record)
```

**Record Literal Features:**
- No quotes required on field names
- Supports nested record literals recursively
- Type alias validation at parse time
- Proper JSON escaping for string values
- Null safety checks
- Compatible with all existing record operations

**Supported Value Types in Record Literals:**
- String literals (quoted): `"text"`
- Numbers: `42`, `3.14`, `999L`
- Booleans: `true`, `false`
- Null: `null`
- Nested record literals: `TypeName { ... }`

**Note:** Variables cannot be used directly in record literals. All values must be literal expressions or nested record literals.

---

## Variables

### Declaration
```javascript
// With explicit type using 'var'
var name: string;
var count: int = 0;
var price: double = 19.99;

// Using 'let' keyword (alias for 'var')
let message: string = "Hello";
let total: int = 100;

// Type inference
var autoType = "Hello";  // Inferred as string
let autoNum = 42;        // Inferred as int

// Multiple declarations
var x: int;
var y: int = 5;
let z = 10;
```

### Assignment
```javascript
name = "Alice";
count = count + 1;
price = price * 1.1;
```

### Compound Assignment Operators
```javascript
var x: int = 10;
x += 5;   // Equivalent to: x = x + 5
x -= 3;   // Equivalent to: x = x - 3
x *= 2;   // Equivalent to: x = x * 2
x /= 4;   // Equivalent to: x = x / 4
```

### Increment and Decrement Operators
```javascript
var count: int = 0;
count++;  // Equivalent to: count = count + 1
count--;  // Equivalent to: count = count - 1
```

### Scope
```javascript
var global = "I'm global";

if true then {
    var local = "I'm local";
    print global;  // OK
    print local;   // OK
}

// print local;  // ERROR: local not in scope
```

---

## Operators

### Arithmetic Operators
```javascript
var a = 10 + 5;      // Addition: 15
var b = 10 - 5;      // Subtraction: 5
var c = 10 * 5;      // Multiplication: 50
var d = 10 / 5;      // Division: 2
var e = 10 ^ 2;      // Exponentiation: 100
var f = -a;          // Unary negation: -15
```

### Assignment Operators
```javascript
var x = 10;          // Simple assignment
x += 5;              // Add and assign: x = x + 5
x -= 3;              // Subtract and assign: x = x - 3
x *= 2;              // Multiply and assign: x = x * 2
x /= 4;              // Divide and assign: x = x / 4
```

### Increment and Decrement Operators
```javascript
var count = 0;
count++;             // Post-increment: count = count + 1
count--;             // Post-decrement: count = count - 1
```

### Comparison Operators
```javascript
a == b    // Equal to
a != b    // Not equal to
a > b     // Greater than
a >= b    // Greater than or equal to
a < b     // Less than
a <= b    // Less than or equal to
```

### Logical Operators
```javascript
a and b   // Logical AND (also &&)
a or b    // Logical OR (also ||)
!a        // Logical NOT (also not)
not a     // Logical NOT (alternative to !)

// Example
if count > 0 and count < 100 then {
    print "In range";
}
```

### typeof Operator
Get the type of any variable or expression at runtime as a string.

#### Basic Usage
```javascript
// Simple types
var name: string = "Alice";
print typeof name;  // Output: string

var count: int = 42;
print typeof count;  // Output: int

var price: double = 19.99;
print typeof price;  // Output: double

var active: bool = true;
print typeof active;  // Output: bool
```

#### typeof with Records
```javascript
// Record with explicit type
var person: record {name: string, age: int};
person = {"name": "Bob", "age": 30};
print typeof person;  // Output: record {name:string, age:int}

// Record from JSON casting
var data: json = {"id": 123, "email": "user@example.com"};
var rec = record(data);
print typeof rec;  // Output: record {id:int, email:string}
```

#### typeof with Arrays
```javascript
// Fixed-size arrays
var numbers: int[5];
print typeof numbers;  // Output: array.int[5]

var bytes: byte[10];
print typeof bytes;  // Output: array.byte[10]

// Dynamic arrays
var items: string[];
print typeof items;  // Output: array.string[]

// Arrays of records
var people: array.record[3]{name: string, age: int};
print typeof people;  // Output: array.record[3] {name:string, age:int}

var users: array.record[]{id: int, email: string};
print typeof users;  // Output: array.record[] {id:int, email:string}
```

#### typeof in Conditionals
```javascript
// Type checking
if typeof value == "string" then {
    print "Value is a string";
}

// Dynamic type handling
if typeof data == "record" then {
    print "Processing record";
} else if typeof data == "array" then {
    print "Processing array";
}
```

#### typeof with Expressions
```javascript
// Get type of expression result
print typeof (10 + 20);           // Output: int
print typeof ("Hello" + " World"); // Output: string
print typeof (3.14 * 2);          // Output: double
print typeof int("42");            // Output: int
```

**typeof Output Formats:**
- Primitive types: `string`, `int`, `long`, `float`, `double`, `bool`, `byte`
- Complex types: `json`, `date`, `array`
- Records: `record {field:type, field:type, ...}`
- Arrays: `array.type[size]` or `array.type[]`
- Array of records: `array.record[size] {field:type, ...}` or `array.record[] {field:type, ...}`

### String Concatenation
```javascript
var greeting = "Hello, " + name + "!";
var fullName = firstName + " " + lastName;
```

### Operator Precedence
1. `()` - Parentheses
2. `^` - Exponentiation (right-associative)
3. `!`, unary `+`, unary `-` - Unary operators
4. `*`, `/` - Multiplication, Division
5. `+`, `-` - Addition, Subtraction
6. `>`, `>=`, `<`, `<=`, `==`, `!=` - Comparison
7. `and` - Logical AND
8. `or` - Logical OR

---

## Control Flow

### If Statements

#### Basic If
```javascript
if condition then {
    // statements
}

// Alternative syntax with parentheses
if (condition) {
    // statements
}
```

#### If-Else
```javascript
if score >= 60 then {
    print "Pass";
} else {
    print "Fail";
}
```

#### If-Else-If
```javascript
if score >= 90 then {
    print "A";
} else if score >= 80 then {
    print "B";
} else if score >= 70 then {
    print "C";
} else {
    print "F";
}
```

#### Single-Statement If
```javascript
if x > 0 then print "positive";
```

### While Loops

#### Basic While
```javascript
while count < 10 {
    print count;
    count++;
}

// Alternative with 'then'
while count < 10 then {
    print count;
    count++;
}
```

#### While with Condition in Parentheses
```javascript
while (hasMore) {
    var item = getNext();
    print item;
}
```

### For Loops

#### Traditional For Loop (C-style)
```javascript
for (var i: int = 0; i < 10; i++) {
    print i;
}

// With compound assignment
for (var i: int = 0; i < 20; i += 2) {
    print "Even number: " + i;
}

// Multiple statements in body
for (var i: int = 1; i <= 5; i++) {
    var square: int = i * i;
    print i + " squared = " + square;
}
```

#### For Loop Parts
- **Initializer**: Executed once before loop starts (can be `var` declaration or assignment)
- **Condition**: Evaluated before each iteration; loop continues while true
- **Increment**: Executed after each iteration

```javascript
// All parts are optional
for (;;) {
    // Infinite loop
    if count > 100 then break;
    count++;
}
```

### ForEach Loops

#### Iterate Over Arrays
```javascript
var numbers = [1, 2, 3, 4, 5];
foreach num in numbers {
    print num;
}

// With parentheses
foreach (item in items) {
    print item;
}
```

#### Iterate Over Strings
```javascript
var text = "Hello";
foreach char in text {
    print char;  // Prints each character
}
```

#### Reverse Iteration Over Arrays
```javascript
var numbers = [1, 2, 3, 4, 5];

// Forward iteration
foreach num in numbers {
    print num;  // Output: 1, 2, 3, 4, 5
}

// Reverse iteration using the array.reverse() builtin
foreach num in call array.reverse(numbers) {
    print num;  // Output: 5, 4, 3, 2, 1
}
```

> **Note:** `array.reverse()` is a builtin function that returns a reverse iteration wrapper. See the [Array Functions](#array-functions) section for details.

### Do-While Loops
```javascript
do {
    print count;
    count++;
} while (count < 10);
```

### Break and Continue
```javascript
// Break: exit loop
for (var i: int = 0; i < 100; i++) {
    if i > 10 then break;
    print i;
}

// Continue: skip to next iteration
for (var i: int = 0; i < 10; i++) {
    if i == 5 then continue;
    print i;  // Won't print 5
}

// Continue: skip to next iteration
var i = 0;
while i < 10 {
    i = i + 1;
    if i == 5 then continue;
    print i;  // Won't print 5
}

// 'exit' is synonym for 'break'
while true {
    if done then exit;
}
```

---

## Exception Handling

EBS provides robust exception handling with `try-exceptions-when` syntax for catching errors, and `raise exception` for explicitly throwing errors.

### Try-Exceptions Syntax

Use `try-exceptions` blocks to catch and handle errors:

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

### Available Error Types

| Error Type | Description |
|------------|-------------|
| `ANY_ERROR` | Catches any error (catch-all handler, should be last) |
| `IO_ERROR` | File I/O operations, streams, paths |
| `DB_ERROR` | Database connection and query errors |
| `TYPE_ERROR` | Type conversion and casting errors |
| `NULL_ERROR` | Null pointer or null value errors |
| `INDEX_ERROR` | Array index out of bounds errors |
| `MATH_ERROR` | Division by zero, arithmetic errors |
| `PARSE_ERROR` | JSON parsing, date parsing errors |
| `NETWORK_ERROR` | HTTP and network connection errors |
| `NOT_FOUND_ERROR` | Variable or function not found errors |
| `ACCESS_ERROR` | Permission or access denied errors |
| `VALIDATION_ERROR` | Validation errors |

### Raising Exceptions

Use the `raise exception` statement to explicitly throw errors from your code. This is useful for:
- **Input validation**: Rejecting invalid user input with descriptive error messages
- **Business rule violations**: Enforcing application-specific constraints
- **Precondition checks**: Validating function parameters before processing
- **Error propagation**: Converting low-level errors to domain-specific exceptions

#### Standard Exceptions

Standard exceptions (from the ErrorType list above) only accept a single message parameter (string). The message describes the error condition:

```javascript
// Raise with a message
raise exception IO_ERROR("File not found: config.txt");
raise exception VALIDATION_ERROR("Input must be a positive number");
raise exception MATH_ERROR("Cannot calculate square root of negative number");

// Raise without a message (uses default message: "ERROR_TYPE raised with no message")
raise exception NULL_ERROR();
```

**Note:** Standard exception names are case-insensitive. `IO_ERROR`, `io_error`, and `Io_Error` are all equivalent.

#### Custom Exceptions

Custom exceptions are identified by any name NOT in the standard ErrorType list. They can accept multiple parameters of any type:

```javascript
// Custom exceptions with multiple parameters
raise exception ValidationFailed("username", "must be at least 3 characters");
raise exception OutOfBoundsError(10, 0, 5);  // index, min, max
raise exception BusinessRuleViolation("order", 12345, "insufficient inventory");

// Single parameter custom exception
raise exception ConfigurationError("Missing required setting: API_KEY");
```

**Custom Exception Naming:**
- Names follow standard identifier rules (letters, digits, underscores; cannot start with a digit)
- Names are case-insensitive: `MyError`, `myerror`, and `MYERROR` all match the same exception
- Use descriptive names that indicate the error condition (e.g., `ValidationFailed`, `InsufficientFunds`)

#### Catching Raised Exceptions

Both standard and custom exceptions can be caught with exception handlers:

```javascript
// Catch standard exception
try {
    if value < 0 then {
        raise exception VALIDATION_ERROR("Value must be non-negative");
    }
} exceptions {
    when VALIDATION_ERROR(msg) {
        print "Validation failed: " + msg;
    }
}

// Catch custom exception by name
try {
    raise exception MyCustomError("something went wrong", 42);
} exceptions {
    when MyCustomError(msg) {
        print "Custom error caught: " + msg;
    }
}

// Custom exceptions can also be caught by ANY_ERROR
try {
    raise exception UnhandledScenario("edge case detected");
} exceptions {
    when ANY_ERROR(msg) {
        print "Caught by ANY_ERROR: " + msg;
    }
}
```

### Exception Handling Features

- **Handler Order**: Handlers are checked in order; the first matching handler is executed
- **Error Variable**: Use `when ERROR_TYPE(varName)` to capture the error message in a string variable
- **Custom Exceptions**: Any unrecognized exception name is treated as a custom exception
- **Case-Insensitive Matching**: Both standard and custom exception names are matched case-insensitively
- **Nested Try Blocks**: Try blocks can be nested for granular error handling
- **Standard Exception Parameters**: Standard exceptions accept zero or one parameter (the error message as a string)
- **Custom Exception Parameters**: Custom exceptions can accept any number of parameters of any type
- **ANY_ERROR Placement**: `ANY_ERROR` catches all exception types, so it should typically be last

### Exception Handling Examples

#### Input Validation with Custom Exception

```javascript
// Define a validation function that raises exceptions
validateAge(age: int) {
    if age < 0 then {
        raise exception ValidationFailed("age", "cannot be negative");
    }
    if age > 150 then {
        raise exception ValidationFailed("age", "unrealistic value");
    }
}

// Use the function with exception handling
try {
    call validateAge(-5);
} exceptions {
    when ValidationFailed(msg) {
        print "Validation error: " + msg;
    }
}
```

#### Chained Exception Handling

```javascript
// Process data with multiple potential error types
processOrder(orderId: int) {
    if orderId <= 0 then {
        raise exception VALIDATION_ERROR("Invalid order ID");
    }
    
    // Simulate database lookup that might fail
    var order = call db.getOrder(orderId);
    if order == null then {
        raise exception NOT_FOUND_ERROR("Order not found: " + orderId);
    }
    
    // Simulate business rule check
    if order.status == "cancelled" then {
        raise exception BusinessRuleViolation("order", orderId, "already cancelled");
    }
}

try {
    call processOrder(12345);
} exceptions {
    when VALIDATION_ERROR(msg) {
        print "Invalid input: " + msg;
    }
    when NOT_FOUND_ERROR(msg) {
        print "Not found: " + msg;
    }
    when BusinessRuleViolation(msg) {
        print "Business rule violated: " + msg;
    }
    when ANY_ERROR(msg) {
        print "Unexpected error: " + msg;
    }
}
```

---

## Code Organization

### Importing Other Scripts

Import other EBS script files to reuse functions and code:

```javascript
// Import from same directory
import "helper.ebs";

// Import from subdirectory
import "util/stringUtil.ebs";

// Import with spaces in path (use quotes)
import "my utils/helper functions.ebs";

// Single quotes also supported
import 'lib/database.ebs';
```

**Import Features:**
- Supports subdirectory paths using forward slashes (`/`)
- Handles spaces in directory and file names
- Both single (`'`) and double (`"`) quotes supported
- Circular import protection (files imported only once)
- Import paths are resolved relative to the importing script's directory

**Best Practices:**
- **Place imports at the top of your script file** whenever possible for better code organization and readability
- Import statements should appear before type definitions, constants, and other code
- Exception: If your imported code depends on type definitions or constants from the main file, place the import after those definitions

**Example:**
```javascript
// main.ebs
// Imports at the top (recommended)
import "util/math.ebs";
import "util/string.ebs";

var result: int = call add(5, 3);
var text: string = call toUpper("hello");
```

**Example with dependencies:**
```javascript
// chess.ebs
// Type definitions first
ChessCell typeof bitmap { cellColor: 0, pieceType: 1-6, pieceColor: 7 };
posType typeof record { x: int, y: int };

// Constants
var WHITE: int = 0;
var BLACK: int = 1;

// Helper functions that imported code depends on
isValidPosition(x: int, y: int) return bool {
    return x >= 0 && x < 8 && y >= 0 && y < 8;
}

// Import after dependencies are defined
import "chess-moves.ebs";

// Rest of the code...
```

### Type Aliases (typeof)

Define reusable type aliases for complex types using the `typeof` keyword. Type aliases are globally accessible throughout the script and can simplify variable declarations for complex types.

#### Basic Syntax
```javascript
// Generic syntax
typeName typeof type_definition;

// Concrete example
personType typeof record{name: string, age: int};
var person: personType;
```

#### Simple Type Aliases

Define aliases for basic data types:

```javascript
// Alias for a simple record type
personType typeof record{name: string, age: int};

// Use the type alias in variable declaration
var person: personType;
person = {"name": "John Doe", "age": 30};

// Print the whole record
print person;  // Displays: {"name": "John Doe", "age": 30}

// Modify fields using assignment
person.name = "Jane Smith";
person.age = 35;
print person;  // Displays updated values
```

#### Array Type Aliases

Define aliases for array types (fixed-size):

```javascript
// Fixed-size array alias
intArray typeof array.int[10];

var numbers: intArray;
numbers = [1, 2, 3, 4, 5];
print numbers[0];     // 1
print numbers.length; // 10 (array size, remaining elements are null)
```

#### Record Type Aliases

Define aliases for structured record types:

```javascript
// Simple record with multiple fields
employeeType typeof record{
    id: int,
    name: string,
    salary: double,
    active: bool
};

var employee: employeeType;
employee = {
    "id": 101,
    "name": "Alice Smith",
    "salary": 75000.00,
    "active": true
};

// Print the employee record
print employee;

// Update employee fields
employee.name = "Alice Johnson";
employee.salary = 80000.00;
```

#### Nested Record Type Aliases

Define aliases for records with nested structures:

```javascript
// Define a record type with nested record structure
customerType typeof record{
    id: int,
    name: string,
    address: record{
        street: string,
        city: string,
        zipCode: string
    }
};

var customer: customerType;
customer = {
    "id": 1001,
    "name": "Tech Corp",
    "address": {
        "street": "123 Main St",
        "city": "Springfield",
        "zipCode": "12345"
    }
};

// Print the customer record
print customer;

// Update fields (including nested fields)
customer.id = 1002;
customer.name = "New Corp";
customer.address.city = "Shelbyville";
customer.address.zipCode = "54321";
```

#### Array of Records Type Aliases

Define aliases for arrays of structured data:

```javascript
// Array of records (dynamic size)
employeeListType typeof array.record{
    id: int,
    name: string,
    department: string
};

var employees: employeeListType;
employees = [
    {"id": 1, "name": "Alice", "department": "Engineering"},
    {"id": 2, "name": "Bob", "department": "Sales"},
    {"id": 3, "name": "Charlie", "department": "Marketing"}
];

// Array operations
print employees.length;  // 3
print employees[0];      // First employee record
print employees[1];      // Second employee record
```

#### Array of Records with Nested Structures

Combine arrays and nested records in type aliases:

```javascript
// Complex nested structure
companyDataType typeof array.record{
    id: int,
    department: record{
        name: string,
        location: string,
        manager: string
    }
};

var companyData: companyDataType;
companyData = [
    {
        "id": 101,
        "department": {
            "name": "Engineering",
            "location": "Building A",
            "manager": "John Doe"
        }
    },
    {
        "id": 102,
        "department": {
            "name": "Sales",
            "location": "Building B",
            "manager": "Jane Smith"
        }
    }
];

// Access array elements
print companyData.length;  // 2
print companyData[0];      // First department
print companyData[1];      // Second department

// Update nested fields in array elements
companyData[0].department.name = "Research & Development";
companyData[0].department.location = "Building C";
companyData[1].department.manager = "New Manager";
```

#### Type Alias Features

**Key Characteristics:**
- Type aliases are **global** and accessible throughout the entire script
- Defined at parse time, allowing subsequent code to use them immediately
- Type aliases can be used for variable declarations just like built-in types
- Support all data types: primitives, arrays, records, and nested combinations
- **Array sizing**:
  - Fixed-size primitive arrays: Use `[size]` syntax (e.g., `array.int[10]`)
  - Dynamic arrays of records: Omit size specifier (e.g., `array.record{...}`)
- **Record field updates**: Fields can be updated using assignment, including nested fields
  - Top-level: `person.name = "value"`
  - Nested: `customer.address.city = "value"`
  - Array elements: `employees[0].address.city = "value"`
  - Case-insensitive property access (e.g., `zipCode` and `zipcode` both work)
- Access whole records/arrays for reading (e.g., `print person`)

**Benefits:**
- **Code Reusability**: Define complex types once, use them many times
- **Maintainability**: Update type definitions in one place
- **Readability**: Descriptive type names make code more understandable
- **Type Safety**: Enforce consistent structure across variables

**Example Use Case:**
```javascript
// Define a type for API response data
apiResponseType typeof record{
    status: int,
    message: string,
    data: record{
        userId: int,
        username: string,
        email: string
    }
};

// Use the type alias multiple times
var response1: apiResponseType;
var response2: apiResponseType;
var response3: apiResponseType;

response1 = {
    "status": 200,
    "message": "Success",
    "data": {
        "userId": 42,
        "username": "alice",
        "email": "alice@example.com"
    }
};

// Print the response
print response1;

// Update fields (including nested fields)
response1.status = 201;
response1.message = "Created";
response1.data.username = "bob";
response1.data.email = "bob@example.com";
```

#### Mixed Chain Access Patterns

The language supports complex **mixed chains** of array indexing `[index]` and property access `.property` for both reading and writing operations. This enables flexible navigation through nested data structures.

**Supported Patterns:**

```javascript
// Basic array[index].property
employees[0].name = "Alice Smith";
employees[1].email = "bob@example.com";

// Nested properties: array[index].property.nestedProperty
companies[0].address.city = "New Springfield";
companies[1].address.street = "789 Pine Blvd";

// Multiple array elements with nested properties
dataset[0].info.title = "Updated First";
dataset[2].info.description = "Updated Description";

// Case-insensitive property access in chains
people[0].firstname = "Jane";                        // Works with firstName
people[0].contactinfo.emailaddress = "jane@example.com";  // Works with contactInfo.emailAddress
```

**Examples in Loops:**

```javascript
// Update multiple elements sequentially
employeeType typeof array.record{
    id: int,
    name: string,
    email: string
};

var employees: employeeType;
employees = [
    {"id": 1, "name": "Alice", "email": "alice@example.com"},
    {"id": 2, "name": "Bob", "email": "bob@example.com"},
    {"id": 3, "name": "Charlie", "email": "charlie@example.com"}
];

// Update all employee IDs in a loop
for (var i: int = 0; i < employees.length; i++) {
    employees[i].id = employees[i].id + 1000;
}

// Result: IDs become 1001, 1002, 1003
```

**Key Points:**
- Mix `[index]` and `.property` access freely in assignment chains
- Works with typedef arrays (`array.record`) for structured data
- Case-insensitive property names throughout the chain
- Full support for nested records within array elements
- See `test_mixed_chain_access.ebs` for comprehensive examples

---

## Functions

### Function Declaration

EBS supports function declarations with or without the optional `function` keyword. Both styles are equivalent.

#### Basic Function
```javascript
// Without 'function' keyword (traditional)
greet {
    print "Hello!";
}

// With 'function' keyword (beginner-friendly)
function greet {
    print "Hello!";
}

call greet;
```

#### Function with Return Type
```javascript
// Without 'function' keyword
getValue return int {
    return 42;
}

// With 'function' keyword
function getValue return int {
    return 42;
}

var x = call getValue();
```

#### Function with Parameters
```javascript
// Without 'function' keyword
add(a: int, b: int) return int {
    return a + b;
}

// With 'function' keyword
function add(a: int, b: int) return int {
    return a + b;
}

var sum = call add(5, 3);
```

#### Function with Default Parameters
```javascript
function greet(name: string = "World") return string {
    return "Hello, " + name + "!";
}

var msg1 = call greet();           // "Hello, World!"
var msg2 = call greet("Alice");    // "Hello, Alice!"
```

### Function Calls

#### Statement Form
```javascript
call greet();
call processData(input);
```

#### Expression Form
```javascript
var result = call calculate(10, 20);
var message = call formatMessage("User", id);

// Using # shorthand
var result = #calculate(10, 20);
```

#### Named Parameters
```javascript
divide(dividend: int, divisor: int) return double {
    return dividend / divisor;
}

// Call with named parameters
var result = call divide(divisor = 4, dividend = 20);
```

### Return Statement
```javascript
findMax(a: int, b: int) return int {
    if a > b then {
        return a;
    }
    return b;
}
```

---

## Arrays

> **See Also**: For a comprehensive overview of all collection types including arrays, queues, maps, and JSON with comparison tables and usage guidance, see **[EBS Collections Reference](EBS_COLLECTIONS_REFERENCE.md)**.

### Array Declaration

#### Fixed-Size Arrays

**Using Typed Arrays (Traditional):**
```javascript
var numbers: int[5];           // Array of 5 integers
var matrix: int[3, 4];         // 2D array: 3 rows, 4 columns
var cube: int[2, 3, 4];        // 3D array
```

**Using Generic Array Type:**
```javascript
var items: array[10];          // Generic array of 10 elements
var grid: array[5, 5];         // 2D generic array (5x5 grid)
```

**Using array.type Syntax (Enhanced):**
```javascript
var strings: array.string[5];  // String array
var ints: array.int[10];       // Integer array
var nums: array.number[5];     // Number (double) array
var bytes: array.byte[10];     // Byte array (uses ArrayFixedByte)
var floats: array.float[5];    // Float array
var longs: array.long[3];      // Long array
```

The `array.type` syntax provides an alternative way to declare typed arrays, making the syntax more consistent and explicit.

#### Dynamic Arrays

**Using Typed Arrays:**
```javascript
var items: string[*];          // Dynamic string array
```

**Using Generic Array Type:**
```javascript
var collection: array[*];      // Dynamic generic array
var anyType: array.any[*];     // Explicit any type (same as array)
```

**Using array.type Syntax:**
```javascript
var dynamicStrings: array.string[*];  // Dynamic string array
var dynamicInts: array.int[*];        // Dynamic integer array
```

Dynamic arrays backed by `ArrayDynamic` can grow as needed using `array.expand()`.

#### Array Literals
```javascript
var numbers = [1, 2, 3, 4, 5];
var names = ["Alice", "Bob", "Charlie"];
var mixed = [1, "two", 3.0, true];

// Nested arrays
var matrix = [[1, 2], [3, 4], [5, 6]];
```

### Array Type Comparison

| Syntax | Type | Backed By | Usage |
|--------|------|-----------|-------|
| `int[10]` | Typed | ArrayFixed | Fixed-size integer array (traditional, uses Object[] with boxed Integer) |
| `array.int[10]` | Typed | ArrayFixedInt | Fixed-size integer array (enhanced, uses primitive int[]) |
| `array.byte[10]` | Typed | ArrayFixedByte | Byte array with optimized storage (primitive byte[]) |
| `array.bitmap[10]` | Typed | ArrayFixedByte | Bitmap array (same storage as byte, BITMAP type) |
| `array.intmap[10]` | Typed | ArrayFixedInt | Intmap array (same storage as int, INTMAP type) |
| `string[*]` | Typed | ArrayDynamic | Dynamic string array (traditional) |
| `array.string[*]` | Typed | ArrayDynamic | Dynamic string array (enhanced) |
| `array[10]` | Generic | ArrayFixed | Fixed-size generic array |
| `array.any[10]` | Generic | ArrayFixed | Fixed-size generic array (explicit) |
| `array[*]` | Generic | ArrayDynamic | Dynamic generic array |
| `json` | JSON | Java List/Map | JSON arrays and objects |

### Available array.type Variants

- `array` or `array.any` - Generic array (any type)
- `array.string` - String array
- `array.byte` - Byte array (uses ArrayFixedByte for fixed size, 8-bit signed integers)
- `array.bitmap` - Bitmap array (uses ArrayFixedByte for fixed size, 8-bit with named fields)
- `array.int` or `array.integer` - Integer array (uses ArrayFixedInt for fixed size, 32-bit signed integers)
- `array.intmap` - Intmap array (uses ArrayFixedInt for fixed size, 32-bit with named fields)
- `array.long` - Long integer array
- `array.float` - Float array
- `array.double` or `array.number` - Double/number array
- `array.bool` or `array.boolean` - Boolean array
- `array.date` - Date array

### Byte and Bitmap Array Interoperability

Both `array.byte` and `array.bitmap` are backed by the same `ArrayFixedByte` storage class, making them interchangeable. The difference is in the data type designation, which can be useful for type clarity in your code.

```javascript
// Create byte and bitmap arrays
var byteArray: array.byte[5];
var bitmapArray: array.bitmap[5];

// Cast between types using built-in functions
var castedToBitmap = call array.asBitmap(byteArray);
var castedToByte = call array.asByte(bitmapArray);

// Useful for interpreting byte data as bitmap fields
StatusByte typeof bitmap { active: 0, error: 1, warning: 2 };
var statusBytes: array.bitmap[10];
statusBytes[0] = 5;  // active=1, error=0, warning=1
var status = StatusByte(statusBytes[0]);
print status.active;  // 1
```

### Integer and Intmap Array Interoperability

Similarly, `array.int` and `array.intmap` are backed by the same `ArrayFixedInt` storage class, making them interchangeable. Use intmap arrays when you need to store bit-packed data structures with named fields.

```javascript
// Create integer and intmap arrays
var intArray: array.int[5];
var intmapArray: array.intmap[5];

// Cast between types using built-in functions
var castedToIntmap = call array.asIntmap(intArray);
var castedToInt = call array.asInt(intmapArray);

// Useful for interpreting integer data as intmap fields
ConfigData typeof intmap { enabled: 0, mode: 1-3, level: 4-11, id: 12-31 };
var configs: array.intmap[10];
configs[0] = 1048832;  // Some packed configuration value
var config = ConfigData(configs[0]);
print config.enabled;  // Extract enabled bit
print config.id;       // Extract id field
```

**When to use each syntax:**
- Use `int[10]` for concise traditional syntax (small arrays, < 100 elements)
- Use `array.int[10]` for explicit, consistent syntax across all types (better performance for large arrays ≥ 100 elements)
- Use `array[10]` or `array.any[10]` when you need mixed types
- Use `array.bitmap[10]` when storing data that should be interpreted as bitmap fields
- **Important**: `int[n]` and `array.int[n]` have different internal implementations affecting performance and memory usage
  - `int[n]` uses `Object[]` with boxed `Integer` objects (higher memory, boxing/unboxing overhead)
  - `array.int[n]` uses primitive `int[]` (lower memory, faster access, no boxing)
  - See [Array Syntax Guide](ARRAY_SYNTAX_GUIDE.md) for detailed comparison and usage recommendations

### Array Access
```javascript
var first = numbers[0];         // First element
var last = numbers[4];          // Last element
numbers[2] = 99;                // Set element

// Multi-dimensional access
var cell = matrix[1, 2];        // Row 1, Column 2
matrix[0, 0] = 100;
```

### Array Properties
```javascript
var count = numbers.length;     // Get array length
print "Array has " + count + " elements";
```

### Array Initialization
```javascript
// Using array.fill
var zeros: int[10];
call array.fill(zeros, 0);

// Using array.expand (dynamic arrays)
var dynamic: array[*];
call array.expand(dynamic, 10);  // Expand to 10 elements

// With literal assignment
var items: array[*] = [1, "two", 3.0, true];  // Mixed types
```

---

## JSON

### JSON Declaration
```javascript
var person: json = {
    "name": "Alice",
    "age": 30,
    "email": "alice@example.com"
};

var numbers: json = [1, 2, 3, 4, 5];
```

### Variable References in JSON (`$variable`)

You can reference EBS script variables directly in JSON using the `$` prefix **without quotes**:

```javascript
// Define script variables
var userName: string = "Alice";
var userAge: int = 30;
var isActive: bool = true;

// Use $variable references in JSON (no quotes)
var person: json = {
    "name": $userName,        // References the userName variable
    "age": $userAge,          // References the userAge variable
    "active": $isActive       // References the isActive variable
};

// Result: {"name": "Alice", "age": 30, "active": true}
```

**Important Notes:**
- `$variable` (without quotes) = variable reference - resolves to the variable's value
- `"$variable"` (with quotes) = literal string - the text "$variable"
- Variable references work with any data type (string, int, bool, arrays, JSON objects, etc.)
- Variables must exist in scope or an error will be thrown
- This feature works in screen definitions, JSON objects, and anywhere JSON is used

**Example in Screen Definition:**
```javascript
var defaultName: string = "Guest";
var defaultAge: int = 18;

screen myScreen = {
    "title": "User Form",
    "vars": [{
        "name": "userName",
        "type": "string",
        "default": $defaultName,    // No quotes - references defaultName
        "display": {
            "type": "textfield",
            "labelText": "Name:"
        }
    }, {
        "name": "userAge",
        "type": "int",
        "default": $defaultAge,     // No quotes - references defaultAge
        "display": {
            "type": "spinner",
            "labelText": "Age:"
        }
    }]
};
```

### JSON Access and Manipulation

#### Get Values
```javascript
var name = call json.get(person, "name");
var age = call json.getint(person, "age");
var email = call json.getstring(person, "email");

// Strict mode (throws error if key doesn't exist)
var name = call json.getstrict(person, "name");
```

#### Set Values
```javascript
call json.set(person, "age", 31);
call json.set(person, "city", "New York");  // Add new field
```

#### Remove Fields
```javascript
call json.remove(person, "email");
```

#### Array Operations
```javascript
var list: json = [1, 2, 3];
call json.add(list, 4);              // Append: [1, 2, 3, 4]
call json.insert(list, 0, 0);        // Insert at index: [0, 1, 2, 3, 4]
```

#### Parse and Stringify
```javascript
// Standard JSON with quoted keys
var jsonString = '{"name": "Bob", "age": 25}';
var obj = call json.jsonfromstring(jsonString);

// JavaScript-style JSON with unquoted keys (also supported)
var jsonString2 = '{name: "Charlie", age: 35}';
var obj2 = call json.jsonfromstring(jsonString2);

// Mixed quoted and unquoted keys
var jsonString3 = '{firstName: "David", "last-name": "Smith"}';
var obj3 = call json.jsonfromstring(jsonString3);

var person: json = {"name": "Alice", "age": 30};
var str = call string.tostring(person);  // Convert to string
```

**Note**: The JSON parser supports both quoted and unquoted keys:
- **Quoted keys**: `{"name": "value"}` - Standard JSON format, required for keys with special characters or spaces
- **Unquoted keys**: `{name: "value"}` - JavaScript-style syntax, only allowed for valid identifiers (letters, digits, underscore)
- Both styles can be mixed in the same JSON document

#### Validation and Schema
```javascript
// Derive schema from JSON object
var schema = call json.derivescheme(person);

// Register schema
call json.registerscheme("PersonSchema", schema);

// Validate against schema
var isValid = call json.validate(person, schema);
```

---

## Database Operations

### Connection Management

#### Connect to Database
```javascript
// JDBC connection string
connect db = "jdbc:oracle:thin:@localhost:1521:xe";

// With JSON configuration
connect myDb = {
    "url": "jdbc:oracle:thin:@localhost:1521:xe",
    "username": "scott",
    "password": "tiger"
};
```

#### Close Connection
```javascript
close connection db;
```

### SQL Cursors

#### Declare Cursor
```javascript
cursor userCursor = select * from users where age > 18;

cursor employeeCursor = select 
    emp_id, first_name, last_name, salary 
    from employees 
    where department = 'IT' 
    order by salary desc;
```

#### Open Cursor
```javascript
open userCursor();

// With parameters
cursor paramCursor = select * from users where age > :minAge;
open paramCursor(minAge = 18);
```

#### Fetch Rows
```javascript
while call userCursor.hasNext() {
    var row = call userCursor.next();
    print row.name;
    print row.age;
}
```

#### Close Cursor
```javascript
close userCursor;
```

### Using Connection Blocks
```javascript
use db {
    cursor myCursor = select * from products where price > 100;
    open myCursor();
    
    while call myCursor.hasNext() {
        var product = call myCursor.next();
        print product.name + ": $" + product.price;
    }
    
    close myCursor;
}
close connection db;
```

---

## Screen/UI Windows

### Screen Declaration

#### Basic Screen
```javascript
screen myWindow = {
    "title": "My Application",
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "userName",
            "type": "string",
            "default": "",
            "display": {
                "type": "textfield",
                "labelText": "User Name:",
                "promptHelp": "Enter your name"
            }
        }
    ]
};
```

**Note**: If a screen with the same name already exists, the declaration is skipped silently. Use `new screen` to replace an existing screen definition.

#### Replace Existing Screen Definition
```javascript
// Replace an existing screen definition with a new one
new screen myWindow = {
    "title": "Updated Application",
    "width": 1024,
    "height": 768,
    "vars": [ /* new variables */ ]
};
```

**Note**: 
- `new screen` will replace the existing screen definition
- Cannot replace a screen that is currently in use (open or hidden) - throws an exception
- Use `close screen` first if you need to replace an active screen

#### Check if Screen is Defined
```javascript
// Check if a screen has been defined before creating or showing it
var exists = call scr.findScreen("myWindow");
if (exists == false) then
    screen myWindow = { "title": "New Window", "width": 400, "height": 300 };
    show screen myWindow;
end if;
```

#### Dynamic Screen with Variable References

You can use `$variable` references (without quotes) to set default values dynamically:

```javascript
// Define script variables
var defaultUserName: string = "Guest";
var defaultAge: int = 25;
var appTitle: string = "Dynamic Form";

screen dynamicScreen = {
    "title": $appTitle,           // Dynamic title from variable
    "width": 800,
    "height": 600,
    "vars": [
        {
            "name": "userName",
            "type": "string",
            "default": $defaultUserName,    // No quotes - references variable
            "display": {
                "type": "textfield",
                "labelText": "User Name:"
            }
        },
        {
            "name": "age",
            "type": "int",
            "default": $defaultAge,         // No quotes - references variable
            "display": {
                "type": "spinner",
                "labelText": "Age:",
                "min": 0,
                "max": 120
            }
        }
    ]
};
```

**Note:** Use `$variable` without quotes to reference script variables. With quotes (`"$variable"`), it's treated as a literal string. See the [JSON Variable References](#variable-references-in-json-variable) section for more details.

### Variable Sets

**New in Version 1.0**: Screen variables can now be organized into named "sets" for better organization and visibility control. This is an enhancement to the traditional flat "vars" array structure.

#### What are Variable Sets?

Variable sets allow you to group related screen variables together with a meaningful name and control their visibility. Instead of defining all variables in a flat "vars" array, you can organize them into logical groups called "sets".

**Benefits:**
- **Better Organization**: Group related variables (e.g., "PersonalInfo", "ContactInfo", "Settings")
- **Visibility Control**: Mark entire sets as internal/hidden from UI using `hiddenind` or `scope`
- **Clear Structure**: Three-part notation (`screen.setName.varName`) makes variable ownership explicit
- **Backward Compatible**: Legacy "vars" format still works (automatically creates a "default" set)

#### Sets Format

Use the `"sets"` property instead of `"vars"` to define variable sets:

```javascript
screen myScreen = {
    "title": "User Profile",
    "width": 800,
    "height": 600,
    "sets": [
        {
            "setname": "PersonalInfo",
            "hiddenind": "N",
            "vars": [
                {
                    "name": "firstName",
                    "type": "string",
                    "default": "John",
                    "display": {"type": "textfield", "labelText": "First Name"}
                },
                {
                    "name": "lastName",
                    "type": "string",
                    "default": "Doe",
                    "display": {"type": "textfield", "labelText": "Last Name"}
                },
                {
                    "name": "age",
                    "type": "int",
                    "default": 30,
                    "display": {"type": "spinner", "min": 0, "max": 120, "labelText": "Age"}
                }
            ]
        },
        {
            "setname": "ContactInfo",
            "hiddenind": "N",
            "vars": [
                {
                    "name": "email",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield", "labelText": "Email"}
                },
                {
                    "name": "phone",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield", "labelText": "Phone"}
                }
            ]
        },
        {
            "setname": "Internal",
            "hiddenind": "Y",
            "vars": [
                {
                    "name": "userId",
                    "type": "int",
                    "default": 0
                },
                {
                    "name": "sessionToken",
                    "type": "string",
                    "default": ""
                }
            ]
        }
    ]
};
```

#### Set Properties

Each set has the following properties:

| Property | Required | Description | Valid Values |
|----------|----------|-------------|--------------|
| `setname` | Yes | Name of the variable set | Any string (stored as lowercase internally) |
| `hiddenind` | No | Visibility indicator | `"N"` = visible (default), `"Y"` = hidden from UI |
| `scope` | No | Alternative to hiddenind with more options | `"visible"`, `"internal"`, `"in"`, `"out"`, `"inout"` |
| `vars` | Yes | Array of variable definitions | Same format as legacy "vars" array |

**Note:** Both `hiddenind` and `scope` control visibility. Use `hiddenind` for simple visible/hidden control, or `scope` for more fine-grained parameter direction control. See [VARIABLE_SETS_VISUAL_GUIDE.md](VARIABLE_SETS_VISUAL_GUIDE.md) for details on `scope` values.

#### Accessing Variables in Sets

Variables in sets use **three-part notation**: `screen.setName.varName`

```javascript
// Access variables
print myScreen.PersonalInfo.firstName;  // "John"
print myScreen.PersonalInfo.age;        // 30
print myScreen.ContactInfo.email;       // ""

// Modify variables
myScreen.PersonalInfo.firstName = "Jane";
myScreen.PersonalInfo.age = 25;
myScreen.ContactInfo.email = "jane@example.com";

// Even internal (hidden) variables are accessible programmatically
myScreen.Internal.userId = 12345;
myScreen.Internal.sessionToken = "abc-xyz-123";
```

The three-part notation makes the variable organization explicit and avoids naming conflicts between sets.

#### Backward Compatibility with Legacy Format

The traditional flat "vars" format is still fully supported:

```javascript
screen legacyScreen = {
    "title": "Legacy Format",
    "vars": [
        {"name": "username", "type": "string", "default": ""},
        {"name": "age", "type": "int", "default": 18}
    ]
};
```

When using the legacy format, variables are automatically placed in a set named `"default"` with `hiddenind="N"`:

```javascript
// Access legacy format variables using either notation:
print legacyScreen.default.username;  // Three-part notation
print legacyScreen.username;          // Two-part notation (backward compatible)

// Both work the same way
legacyScreen.default.username = "alice";
legacyScreen.username = "bob";
```

#### When to Use Sets vs. Legacy Format

**Use Variable Sets (`"sets"`) when:**
- You have many variables and want to organize them logically
- You need internal/hidden variables for state management
- You want clear separation between different groups of data
- You're building complex forms with multiple sections

**Use Legacy Format (`"vars"`) when:**
- You have a simple screen with few variables
- All variables are visible and don't need grouping
- You prefer a flatter, simpler structure

#### Complete Example: User Registration Form

```javascript
screen registrationForm = {
    "title": "User Registration",
    "width": 900,
    "height": 700,
    "sets": [
        {
            "setname": "AccountInfo",
            "hiddenind": "N",
            "vars": [
                {
                    "name": "username",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "labelText": "Username:",
                        "maxLength": 20,
                        "mandatory": true
                    }
                },
                {
                    "name": "password",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "passwordfield",
                        "labelText": "Password:",
                        "mandatory": true
                    }
                }
            ]
        },
        {
            "setname": "PersonalDetails",
            "hiddenind": "N",
            "vars": [
                {
                    "name": "fullName",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield", "labelText": "Full Name:"}
                },
                {
                    "name": "email",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield", "labelText": "Email:"}
                },
                {
                    "name": "birthDate",
                    "type": "string",
                    "default": "",
                    "display": {"type": "datepicker", "labelText": "Birth Date:"}
                }
            ]
        },
        {
            "setname": "InternalData",
            "hiddenind": "Y",
            "vars": [
                {
                    "name": "registrationId",
                    "type": "int",
                    "default": 0
                },
                {
                    "name": "createdTimestamp",
                    "type": "string",
                    "default": ""
                },
                {
                    "name": "ipAddress",
                    "type": "string",
                    "default": ""
                }
            ]
        }
    ]
};

show screen registrationForm;

// Access account info
print "Username: " + registrationForm.AccountInfo.username;

// Access personal details
print "Name: " + registrationForm.PersonalDetails.fullName;
print "Email: " + registrationForm.PersonalDetails.email;

// Access internal data (hidden from UI but accessible in code)
registrationForm.InternalData.registrationId = 12345;
registrationForm.InternalData.createdTimestamp = call date.format(call date.now(), "yyyy-MM-dd HH:mm:ss");
```

#### See Also

For more detailed information about variable sets:
- [VARIABLE_SETS_MIGRATION.md](VARIABLE_SETS_MIGRATION.md) - Migration guide from legacy format
- [VARIABLE_SETS_VISUAL_GUIDE.md](VARIABLE_SETS_VISUAL_GUIDE.md) - Visual guide with data flow diagrams
- [VARSET_API.md](VARSET_API.md) - API reference for VarSet and Var classes
- [VARIABLE_SETS_IMPLEMENTATION_SUMMARY.md](VARIABLE_SETS_IMPLEMENTATION_SUMMARY.md) - Implementation details

#### Screen with Multiple Controls
```javascript
screen formScreen = {
    "title": "User Registration Form",
    "width": 900,
    "height": 700,
    "maximize": false,
    "vars": [
        {
            "name": "firstName",
            "type": "string",
            "default": "",
            "display": {
                "type": "textfield",
                "labelText": "First Name:",
                "labelTextAlignment": "left",
                "promptHelp": "Enter first name",
                "maxLength": 30
            }
        },
        {
            "name": "age",
            "type": "int",
            "default": 18,
            "display": {
                "type": "spinner",
                "labelText": "Age:",
                "min": 0,
                "max": 120
            }
        },
        {
            "name": "country",
            "type": "string",
            "default": "USA",
            "display": {
                "type": "combobox",
                "labelText": "Country:",
                "options": ["USA", "Canada", "UK", "Australia"]
            }
        },
        {
            "name": "birthDate",
            "type": "string",
            "default": "",
            "display": {
                "type": "datepicker",
                "labelText": "Birth Date:"
            }
        },
        {
            "name": "favoriteColor",
            "type": "string",
            "default": "#0000ff",
            "display": {
                "type": "colorpicker",
                "labelText": "Favorite Color:"
            }
        },
        {
            "name": "isActive",
            "type": "bool",
            "default": true,
            "display": {
                "type": "checkbox",
                "labelText": "Active"
            }
        },
        {
            "name": "bio",
            "type": "string",
            "default": "",
            "display": {
                "type": "textarea",
                "labelText": "Biography:",
                "promptHelp": "Tell us about yourself"
            }
        }
    ]
};
```

### Available Control Types

| Control Type | Description | Display Properties |
|--------------|-------------|-------------------|
| `textfield` | Single-line text input | `promptHelp`, `maxLength`, `alignment` |
| `textarea` | Multi-line text input | `promptHelp`, `maxLength`, `height` |
| `passwordfield` | Password input (masked) | `promptHelp`, `maxLength` |
| `checkbox` | Boolean checkbox | `labelText` |
| `radiobutton` | Radio button (exclusive selection) | `labelText` |
| `togglebutton` | Toggle button | `labelText` |
| `combobox` | Editable dropdown | `options`, `promptHelp`, `maxLength` |
| `choicebox` | Non-editable dropdown | `options`, `maxLength` |
| `listview` | List selection control | `options` |
| `spinner` | Numeric spinner | `min`, `max` |
| `slider` | Numeric slider | `min`, `max` |
| `datepicker` | Date picker | `promptHelp`, `maxLength` |
| `colorpicker` | Color picker | `maxLength` |
| `button` | Click button | `labelText`, `onClick` |
| `label` / `labeltext` | Display label | `labelText`, `alignment` |
| `hyperlink` | Clickable link | `labelText` |
| `separator` | Visual separator | - |
| `progressbar` | Progress bar | - |
| `progressindicator` | Progress indicator | - |

**Note:** For comprehensive documentation on alignment properties (item alignment, content alignment, and label text alignment), see the [Alignment Properties](#alignment-properties) section below.

### Display Properties

#### Common Properties
```javascript
"display": {
    "type": "textfield",
    "labelText": "Field Label:",           // Label shown before control
    "labelTextAlignment": "left",           // "left", "center", "right"
    "promptHelp": "Enter value",            // Placeholder text
    "maxLength": 50,                        // Maximum length/width
    "mandatory": true,                      // Field is required
    "alignment": "left",                    // Content alignment (see Alignment Properties section)
    "itemFontSize": "14px",                // Control font size
    "itemColor": "#000000",                // Control text color
    "textColor": "#000000",                // Control text color (alternative to itemColor for consistency with labelColor; takes precedence when both are specified)
    "itemBold": true,                      // Bold text
    "itemItalic": false,                   // Italic text
    "labelColor": "#0000ff",               // Label text color
    "labelBold": false,                    // Label bold
    "labelItalic": false,                  // Label italic
    "labelFontSize": "12px",               // Label font size
    "icon": "icons/save.png",              // Icon path (for buttons) - classpath resources or file system paths
    "style": "-fx-background-color: #fff;", // Custom CSS style
    "cssClass": "custom-class"             // CSS class name
}
```

#### Dropdown Options
```javascript
"display": {
    "type": "combobox",
    "options": ["Option 1", "Option 2", "Option 3"]
}
```

#### Numeric Range
```javascript
"display": {
    "type": "spinner",
    "min": 0,
    "max": 100
}
```

#### TextArea with Height
The `height` property specifies the number of lines to display in a textarea control.
```javascript
"display": {
    "type": "textarea",
    "maxLength": 80,
    "height": 3,
    "labelText": "Description:"
}
```

**Note:** For textarea items, if the label text would be too long, use `\n` to split it across multiple lines rather than having a single long label:
```javascript
"labelText": "Describe what\nyou want\nto match:"
```

#### Button with Action
```javascript
{
    "name": "submitBtn",
    "type": "string",
    "default": "Submit",
    "display": {
        "type": "button",
        "labelText": "Submit Form",
        "onClick": "call submitForm();"
    }
}
```

#### Button with Icon
Buttons can display icons alongside text. Icons are automatically scaled to 16x16 pixels.

```javascript
{
    "name": "saveBtn",
    "type": "string",
    "area": "toolbarArea",
    "display": {
        "type": "button",
        "labelText": "Save",
        "icon": "icons/save.png",          // Icon from classpath resources
        "onClick": "call saveFile();"
    }
}
```

#### Button with Icon and Keyboard Shortcut
Combine icons, shortcuts, and event handlers for a complete button experience:

```javascript
{
    "name": "openBtn",
    "type": "string",
    "area": "toolbarArea",
    "display": {
        "type": "button",
        "labelText": "Open",
        "icon": "icons/folder-open.png",   // Icon displayed alongside text
        "shortcut": "Ctrl+O",              // Keyboard shortcut (shown in tooltip)
        "onClick": "call openFile();"      // Click handler
    }
}
```

**Icon Path Resolution:**
- **Classpath resources** (recommended): Place icons in `src/main/resources/icons/`
  - Example: `"icon": "icons/save.png"`
- **File system paths**: Use absolute or relative paths
  - Example: `"icon": "/home/user/icons/save.png"`

See [BUTTON_ICONS.md](BUTTON_ICONS.md) for complete documentation.

#### Event Handlers

Event handlers allow you to execute EBS code in response to user interactions with screen controls.

##### onClick (Button only)
Executes EBS code when a button is clicked.

```javascript
"display": {
    "type": "button",
    "labelText": "Save",
    "onClick": "call saveData();"
}
```

##### shortcut (Button only)
Defines a keyboard shortcut for the button. The shortcut key is automatically underlined in the button label and shown in a tooltip.

```javascript
"display": {
    "type": "button",
    "labelText": "Save",
    "shortcut": "Alt+S",  // Alt+S triggers the button
    "onClick": "call saveData();"
}
```

Shortcuts work seamlessly with button icons:

```javascript
"display": {
    "type": "button",
    "labelText": "Save",
    "icon": "icons/save.png",    // Icon displayed alongside text
    "shortcut": "Ctrl+S",         // Shortcut shown in tooltip
    "onClick": "call saveData();"
}
```

Supported formats:
- `"Alt+S"` - Alt modifier with S key
- `"Ctrl+R"` - Ctrl modifier with R key
- `"Alt+Ctrl+X"` - Both modifiers with X key

See [BUTTON_SHORTCUT_PROPERTY.md](../BUTTON_SHORTCUT_PROPERTY.md) for complete documentation.

##### onValidate
Validates user input and shows visual error feedback. Must return a boolean (`true` for valid, `false` for invalid).

```javascript
"display": {
    "type": "textfield",
    "onValidate": "if (string.length(username) < 3) { return false; } return true;"
}
```

When validation fails, the control is marked with a red border.

##### onChange
Executes EBS code whenever the control's value changes. Does not require a return value.

```javascript
"display": {
    "type": "textfield",
    "onChange": "call updatePreview(inputText);"
}
```

**Example: Calculated field update**
```javascript
{
    "name": "quantity",
    "type": "int",
    "display": {
        "type": "spinner",
        "min": 1,
        "max": 100,
        "onChange": "total = quantity * unitPrice;"
    }
}
```

**Example: Cascading dropdown**
```javascript
{
    "name": "country",
    "type": "string",
    "display": {
        "type": "combobox",
        "options": ["USA", "Canada", "UK"],
        "onChange": "call loadCities(country);"
    }
}
```

**Example: Enable/disable controls**
```javascript
{
    "name": "enableOptions",
    "type": "bool",
    "display": {
        "type": "checkbox",
        "labelText": "Enable Advanced Options",
        "onChange": "call screen.setProperty('myScreen.advancedField', 'disabled', not enableOptions);"
    }
}
```

**Combining onValidate and onChange:**
Both handlers can be used on the same control. When both are present, `onValidate` runs first to apply validation styling, then `onChange` runs regardless of validation result.

```javascript
{
    "name": "email",
    "type": "string",
    "display": {
        "type": "textfield",
        "onValidate": "return string.contains(email, '@');",
        "onChange": "call checkEmailAvailability(email);"
    }
}
```

### Alignment Properties

EBS supports three types of alignment for screen controls, each serving a different purpose:

#### 1. Item Alignment (Item-Level Property)

Controls how the **control itself** is positioned within its parent container cell. This is set directly on the item object, **not** inside the `display` object.

```javascript
{
    "name": "submitBtn",
    "varRef": "submitValue",
    "alignment": "center",  // Item-level: positions the control in its container
    "display": {
        "type": "button",
        "labelText": "Submit"
    }
}
```

**Item Alignment Values:**
- `center` - Center alignment
- `top_left`, `top_center`, `top_right` - Top alignments
- `center_left`, `center_right` - Middle alignments  
- `bottom_left`, `bottom_center`, `bottom_right` - Bottom alignments
- `baseline_left`, `baseline_center`, `baseline_right` - Baseline alignments

**Usage:** Use item alignment with GridPane, HBox, VBox layouts to position controls within their layout cells.

**Example - Centering a button in a GridPane cell:**
```javascript
"items": [
    {
        "name": "cancelBtn",
        "varRef": "cancelValue",
        "sequence": 1,
        "layoutPos": "1,0",
        "alignment": "center",  // Centers button in grid cell
        "display": {
            "type": "button",
            "labelText": "Cancel"
        }
    }
]
```

#### 2. Content Alignment (Display Property)

Controls how **text/content is aligned** within the control itself. This is set inside the `display` object.

```javascript
{
    "name": "priceField",
    "varRef": "price",
    "display": {
        "type": "textfield",
        "labelText": "Price:",
        "alignment": "right"  // Display-level: right-aligns text within textfield
    }
}
```

**Content Alignment Values:**
- `left` - Left-align text within control
- `center` - Center text within control
- `right` - Right-align text within control
- `justify` - Justify text within control (for multi-line controls)

**Usage:** Use content alignment for textfield, textarea, label, and other text-based controls to control internal text positioning.

**Example - Right-aligned numeric field:**
```javascript
{
    "name": "amount",
    "type": "double",
    "default": 0.0,
    "display": {
        "type": "textfield",
        "labelText": "Amount:",
        "alignment": "right",  // Numbers right-aligned within field
        "maxLength": 15
    }
}
```

#### 3. Label Text Alignment (Display Property)

Controls how the **label text** is aligned. This is set inside the `display` object using the `labelTextAlignment` property.

```javascript
{
    "name": "titleField",
    "varRef": "title",
    "display": {
        "type": "textfield",
        "labelText": "Title:",
        "labelTextAlignment": "right",  // Right-aligns label text
        "alignment": "left"  // Left-aligns content within field
    }
}
```

**Label Text Alignment Values:**
- `left` or `l` - Left-align label
- `center` or `c` - Center label
- `right` or `r` - Right-align label

**Usage:** Use label text alignment to position labels relative to their controls, especially useful in forms with consistent label positioning.

#### Complete Alignment Example

```javascript
{
    "name": "formArea",
    "type": "gridpane",
    "style": "-fx-hgap: 10; -fx-vgap: 10; -fx-padding: 20;",
    "items": [
        {
            "name": "nameField",
            "varRef": "fullName",
            "sequence": 1,
            "layoutPos": "0,0",
            "alignment": "center_left",  // Item positioned center-left in cell
            "display": {
                "type": "textfield",
                "labelText": "Full Name:",
                "labelTextAlignment": "right",  // Label right-aligned
                "alignment": "left",  // Text left-aligned within field
                "maxLength": 40
            }
        },
        {
            "name": "amountField",
            "varRef": "totalAmount",
            "sequence": 2,
            "layoutPos": "1,0",
            "alignment": "center_left",  // Item positioned center-left in cell
            "display": {
                "type": "textfield",
                "labelText": "Amount:",
                "labelTextAlignment": "right",  // Label right-aligned
                "alignment": "right",  // Numbers right-aligned within field
                "maxLength": 15
            }
        }
    ]
}
```

#### Setting Alignment at Runtime

Use `scr.setProperty` to modify alignment dynamically:

```javascript
// Change item alignment (how control is positioned in container)
call scr.setProperty("myScreen.submitBtn", "alignment", "center");

// Note: Content alignment and label text alignment are set at design time
// in the display object and cannot be changed at runtime via scr.setProperty
```

**Important Notes:**
- **Item alignment** is set at the item level and can be changed at runtime
- **Content alignment** is set in the display object and defines how content is aligned within the control
- **Label text alignment** is set in the display object and defines how label text is aligned
- Not all alignment types apply to all controls (e.g., content alignment mainly applies to text-based controls)
- Item alignment is most useful with GridPane, HBox, and VBox layouts

### Automatic Width Calculation

The EBS interpreter automatically calculates control widths based on their data when no explicit `maxLength` is specified. This ensures controls are sized appropriately for their content.

#### Width Calculation Priority

1. **Explicit maxLength** (highest priority) - Always used when specified
2. **Data-driven width** - Automatic sizing based on control data
3. **Type-based default** - Fallback default width

#### Data-Driven Width Controls

##### ComboBox and ChoiceBox
Width is calculated based on the longest option in the `options` array:

```javascript
"display": {
    "type": "combobox",
    "options": ["XS", "S", "M", "L", "XL"]  // Narrow width
}

"display": {
    "type": "combobox",
    "options": ["Information Technology", "Human Resources"]  // Wide width
}
```

##### Spinner
Width is calculated based on the string length of `min` and `max` values:

```javascript
"display": {
    "type": "spinner",
    "min": 1,
    "max": 10  // Narrow width (2 characters)
}

"display": {
    "type": "spinner",
    "min": -10000,
    "max": 100000  // Wide width (6 characters for "100000")
}
```

##### DatePicker and ColorPicker
Use type-specific defaults:
- **DatePicker**: 15 characters (for date format)
- **ColorPicker**: 12 characters (compact)

#### Overriding Automatic Width

Use `maxLength` to override automatic calculation:

```javascript
"display": {
    "type": "spinner",
    "min": 0,
    "max": 1000000,
    "maxLength": 10  // Forces specific width regardless of min/max
}

"display": {
    "type": "combobox",
    "options": ["Very Long Option Name Here"],
    "maxLength": 15  // Limits width even with long options
}
```

#### Default Widths (when no data available)

| Control Type | Default Width |
|--------------|---------------|
| TextField | 30 characters |
| TextArea | 50 characters |
| PasswordField | 30 characters |
| Spinner | 10 characters |
| ComboBox | 20 characters |
| ChoiceBox | 20 characters |
| DatePicker | 15 characters |
| ColorPicker | 12 characters |

### Layout Configuration

#### Area Definition
```javascript
screen layoutScreen = {
    "title": "Layout Example",
    "width": 800,
    "height": 600,
    "vars": [
        // ... variables ...
    ],
    "area": [
        {
            "name": "formGrid",
            "type": "gridpane",
            "style": "-fx-hgap: 15; -fx-vgap: 10; -fx-padding: 20;",
            "items": [
                {
                    "name": "field1",
                    "varRef": "firstName",
                    "sequence": 1,
                    "layoutPos": "0,0",  // row,column
                    "prefWidth": "300"
                },
                {
                    "name": "field2",
                    "varRef": "lastName",
                    "sequence": 2,
                    "layoutPos": "0,1",
                    "prefWidth": "300"
                }
            ]
        }
    ]
};
```

#### Container Alignment

Containers (areas) support an `alignment` property to control how child elements are positioned within the container. This property works with HBox, VBox, StackPane, FlowPane, and TilePane containers.

**Available Alignment Values:**

```javascript
// Shorthand values (5)
"left"      // Left edge, vertically centered
"right"     // Right edge, vertically centered  
"top"       // Top edge, horizontally centered
"bottom"    // Bottom edge, horizontally centered
"center"    // Center both horizontally and vertically

// Full position values (9)
"top-left", "top-center", "top-right"
"center-left", "center", "center-right"
"bottom-left", "bottom-center", "bottom-right"

// Baseline values (3) - for text-based layouts
"baseline-left", "baseline-center", "baseline-right"
```

**Example: Centered Form Layout**
```javascript
screen centeredForm = {
    "title": "Centered Form",
    "width": 600,
    "height": 400,
    "vars": [
        {"name": "username", "type": "string", "default": ""},
        {"name": "password", "type": "string", "default": ""},
        {"name": "loginBtn", "type": "string", "default": ""}
    ],
    "area": [
        {
            "name": "loginArea",
            "type": "vbox",
            "alignment": "center",        // Center children horizontally
            "spacing": "15",
            "padding": "30",
            "items": [
                {"varRef": "username", "sequence": 1, 
                 "display": {"type": "textfield", "labelText": "Username:"}},
                {"varRef": "password", "sequence": 2,
                 "display": {"type": "passwordfield", "labelText": "Password:"}},
                {"varRef": "loginBtn", "sequence": 3,
                 "display": {"type": "button", "labelText": "Login"}}
            ]
        }
    ]
};
```

**Example: Right-Aligned Button Bar**
```javascript
screen buttonBarExample = {
    "title": "Button Bar",
    "width": 500,
    "height": 300,
    "vars": [
        {"name": "okBtn", "type": "string", "default": ""},
        {"name": "cancelBtn", "type": "string", "default": ""}
    ],
    "area": [
        {
            "name": "mainContent",
            "type": "vbox",
            "spacing": "10",
            "items": [],
            "areas": [
                {
                    "name": "buttonBar",
                    "type": "hbox",
                    "alignment": "right",     // Right-align buttons
                    "spacing": "10",
                    "padding": "10",
                    "items": [
                        {"varRef": "cancelBtn", "sequence": 1,
                         "display": {"type": "button", "labelText": "Cancel"}},
                        {"varRef": "okBtn", "sequence": 2,
                         "display": {"type": "button", "labelText": "OK"}}
                    ]
                }
            ]
        }
    ]
};
```

**Example: Complex Layout with Multiple Alignments**
```javascript
screen complexLayout = {
    "title": "Complex Alignment Demo",
    "width": 800,
    "height": 600,
    "vars": [
        {"name": "header", "type": "string", "default": "Application Header"},
        {"name": "content", "type": "string", "default": "Main content area"},
        {"name": "footer", "type": "string", "default": "Version 1.0"}
    ],
    "area": [
        {
            "name": "mainLayout",
            "type": "vbox",
            "spacing": "0",
            "items": [],
            "areas": [
                {
                    "name": "headerArea",
                    "type": "hbox",
                    "alignment": "center",           // Centered header
                    "padding": "20",
                    "areaBackground": "#2c3e50",
                    "items": [
                        {"varRef": "header", "sequence": 1,
                         "display": {"type": "label", 
                                   "style": "-fx-text-fill: white; -fx-font-size: 18px;"}}
                    ]
                },
                {
                    "name": "contentArea",
                    "type": "vbox",
                    "alignment": "top-left",         // Content at top-left
                    "padding": "30",
                    "items": [
                        {"varRef": "content", "sequence": 1,
                         "display": {"type": "textarea"}}
                    ]
                },
                {
                    "name": "footerArea",
                    "type": "hbox",
                    "alignment": "bottom-right",     // Footer at bottom-right
                    "padding": "10",
                    "areaBackground": "#ecf0f1",
                    "items": [
                        {"varRef": "footer", "sequence": 1,
                         "display": {"type": "label",
                                   "style": "-fx-font-size: 10px; -fx-text-fill: #7f8c8d;"}}
                    ]
                }
            ]
        }
    ]
};
```

**Container-Specific Behavior:**
- **HBox**: Alignment affects vertical positioning of children (children flow left-to-right)
- **VBox**: Alignment affects horizontal positioning of children (children flow top-to-bottom)
- **StackPane**: Alignment applies to all stacked children as default
- **FlowPane/TilePane**: Alignment determines starting position for wrapping/tiling

**Default Alignments:**
- HBox: `center-left`
- VBox: `top-center`
- StackPane: `center`
- FlowPane/TilePane: `top-left`

**Runtime Access:**
```javascript
// Get current alignment
var align = call scr.getAreaProperty("myScreen.myArea", "alignment");

// Change alignment dynamically
call scr.setAreaProperty("myScreen.myArea", "alignment", "center-right");
```

### Screen Control Commands

#### Show Screen
```javascript
// Show a specific screen by name
show screen myWindow;

// Show the current screen (from within screen context - e.g., in onClick handler)
show screen;
// New syntax (preferred)
show screen myWindow;
// With parameters
show screen myWindow("param1", 123);
// With callback
show screen myWindow callback handleEvent;

// Legacy syntax (still supported)
screen myWindow show;
```

**Note**: 
- Showing a screen will make it visible (unhide if previously hidden)
- If no screen name is provided, determines the screen from the execution thread context
- The no-name form must be called from within a screen context (e.g., onClick handlers)
- If called from console/main thread without a name, an error is thrown

#### Hide Screen
```javascript
// Hide a specific screen by name
hide screen myWindow;

// Hide the current screen (from within screen context - e.g., in onClick handler)
hide screen;
```

**Note**:
- Hiding a screen makes it invisible but keeps it in memory
- Screen can be shown again with `show screen <name>;`
- If no screen name is provided, determines the screen from the execution thread context
- The no-name form must be called from within a screen context (e.g., onClick handlers)
- If called from console/main thread without a name, an error is thrown

#### Close Screen
```javascript
// Close a specific screen by name
close screen myWindow;

// Close the current screen (from within screen context - e.g., in onClick handler)
close screen;
```

**Note**: Closing a screen will:
- Destroy the screen window (Stage)
- Clean up runtime resources (threads, status)
- Invoke any registered close callback
- **Preserve the screen configuration** - the screen can be shown again with `show screen`
- If no screen name is provided, determines the screen from the execution thread context
- The no-name form must be called from within a screen context (e.g., onClick handlers)
- If called from console/main thread without a name, an error is thrown

#### Screen State Transitions
| State | Configuration | Stage (Window) |
|-------|---------------|----------------|
| undefined | ✗ | ✗ |
| defined | ✓ | ✗ |
| open/hidden | ✓ | ✓ |

- `screen <name> = {...};` → defined
- `show screen <name>;` → open
- `hide screen <name>;` → hidden
- `close screen <name>;` → defined (configuration preserved)

### Child Screens (Parent-Child Relationship)

When showing a screen from within another screen's context, it becomes a child screen:

```javascript
screen parentScreen = {
    "title": "Parent",
    "width": 800,
    "height": 600,
    "areas": [
        {
            "name": "buttons",
            "items": [
                {
                    "type": "button",
                    "text": "Open Child",
                    "onClick": "show screen childScreen;"
                }
            ]
        }
    ]
};

screen childScreen = {
    "title": "Child Screen",
    "width": 400,
    "height": 300
};

show screen parentScreen;
// When button is clicked, childScreen is shown as "parentscreen.childscreen"
```

**Note**: 
- Child screen keys are automatically qualified as `parentscreen.childscreen` (lowercase)
- Supports nested hierarchy: `grandparent.parent.child`
- Child screens can only be created from within their parent's context
- From outside the parent screen, child screens cannot be called directly

### Accessing Screen Variables

#### Read Variable
```javascript
var userName = myWindow.userName;
var age = formScreen.age;
print "User: " + userName + ", Age: " + age;
```

#### Write Variable
```javascript
myWindow.userName = "Alice";
formScreen.age = 25;
formScreen.isActive = false;
```

### Screen Features
- **Thread-Safe**: Each screen runs in its own thread with thread-safe variables
- **Case-Insensitive**: Screen names and JSON keys are case-insensitive
- **Not Auto-Shown**: Screens must be explicitly shown with `show` command
- **Multiple Screens**: Can create and manage multiple independent screens
- **Data Binding**: Variable changes immediately reflected in UI
- **Reusable**: Closed screens can be shown again (configuration is preserved)

### Screen Window Properties

Screen windows support various configuration properties that control their appearance and behavior:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `title` | string | `""` | Window title displayed in title bar |
| `width` | int | `800` | Initial window width in pixels |
| `height` | int | `600` | Initial window height in pixels |
| `maximize` | boolean | `false` | Start window in maximized state |
| `resizable` | boolean | `true` | Allow window to be resized by user |
| `disableMaximize` | boolean | `false` | Prevent window from being maximized |
| `showMenu` | boolean | `true` | Show the Edit menu bar at top of window |

#### Window Size and State Properties

```javascript
// Basic window configuration
screen basicWindow = {
    "title": "My Application",
    "width": 800,
    "height": 600
};

// Start maximized
screen maximizedWindow = {
    "title": "Maximized Window",
    "width": 1024,
    "height": 768,
    "maximize": true  // Window opens in maximized state
};

// Fixed size window (cannot resize)
screen fixedWindow = {
    "title": "Fixed Size Dialog",
    "width": 400,
    "height": 300,
    "resizable": false  // User cannot resize window
};
```

#### Disable Maximize Property

The `disableMaximize` property prevents users from maximizing a window, useful for dialogs and utility windows that should maintain a specific size:

```javascript
// Window that can be resized but not maximized
screen utilityWindow = {
    "title": "Utility Panel",
    "width": 600,
    "height": 400,
    "resizable": true,        // Can resize by dragging edges
    "disableMaximize": true,  // Cannot maximize window
    "vars": [ /* ... */ ]
};

// Completely fixed dialog (no resize, no maximize)
screen fixedDialog = {
    "title": "Settings Dialog",
    "width": 500,
    "height": 350,
    "resizable": false,
    "disableMaximize": true,  // Extra protection against maximization
    "vars": [ /* ... */ ]
};

// Normal window (default behavior)
screen normalWindow = {
    "title": "Document Editor",
    "width": 900,
    "height": 700,
    // resizable defaults to true
    // disableMaximize defaults to false
    // User can resize and maximize freely
    "vars": [ /* ... */ ]
};
```

**How `disableMaximize` works:**
- When `true`, prevents all maximization attempts (button click, keyboard shortcuts, double-click title bar)
- Works independently of `resizable` - window can be resizable but not maximizable
- The maximize button remains visible but becomes non-functional (JavaFX limitation)
- Useful for maintaining consistent window layout and preventing UI issues

**Common Use Cases:**
- **Dialog Windows**: Settings, preferences, about dialogs
- **Utility Windows**: Calculators, color pickers, tool palettes
- **Fixed-Layout Forms**: Forms that look best at specific dimensions
- **Child Windows**: Secondary windows that shouldn't dominate screen

**Property Combinations:**

| `resizable` | `disableMaximize` | Result |
|-------------|-------------------|--------|
| `true` | `false` | Normal window - can resize and maximize (default) |
| `true` | `true` | Can resize but cannot maximize |
| `false` | `false` | Fixed size but can maximize (size doesn't change) |
| `false` | `true` | Completely fixed - no resize or maximize |

#### Menu Visibility Property

```javascript
// Hide the Edit menu for cleaner interface
screen cleanWindow = {
    "title": "Clean Interface",
    "width": 800,
    "height": 600,
    "showMenu": false,  // No menu bar at top
    "vars": [ /* ... */ ]
};
```

---

## Built-in Functions

### String Functions

**Note:** String functions can use either the `str.` or `string.` prefix. Both are equivalent and interchangeable (e.g., `str.trim()` and `string.trim()` work identically).

```javascript
// Conversion
call string.tostring(value);
call string.toupper(text);
call string.tolower(text);

// Manipulation
call string.trim(text);
call string.replace(text, old, new);
call string.replaceFirst(text, old, new);  // Replace first occurrence only
call str.replaceAll(text, regex, replacement);  // Regex-based replace all
call string.split(text, delimiter);
call string.join(array, separator);

// Substring and indexing
call str.substring(text, beginIndex, endIndex?);  // Extract substring
call str.indexOf(text, searchString, fromIndex?);  // Find first occurrence
call str.lastIndexOf(text, searchString, fromIndex?);  // Find last occurrence
call str.charAt(text, index);  // Get character at index

// Padding
call str.lpad(text, length, padChar);  // Left-pad to length
call str.rpad(text, length, padChar);  // Right-pad to length

// Queries
call string.contains(text, substring);
call string.startswith(text, prefix);
call string.endswith(text, suffix);
call string.equals(text1, text2);
call string.equalsignorecase(text1, text2);
call string.isempty(text);
call string.isblank(text);

// Character operations
call str.charArray(text);  // Returns int[] array of Unicode code points

// Regex operations
call str.findRegex(text, regex);      // Find first regex match
call str.findAllRegex(text, regex);   // Find all regex matches, returns array
```

#### str.charArray() - Character Code Array
Returns an integer array containing the Unicode code point for each character in a string.

```javascript
// Basic usage
var text: string = "Hello";
var codes = call str.charArray(text);
print codes;  // Output: [72, 101, 108, 108, 111]

// Access individual character codes
print codes[0];  // Output: 72 (character 'H')
print codes[1];  // Output: 101 (character 'e')

// Works with all character types
var special: string = "A@1";
var specialCodes = call str.charArray(special);
print specialCodes;  // Output: [65, 64, 49]

// Empty string returns empty array
var empty: string = "";
var emptyCodes = call str.charArray(empty);
print emptyCodes;  // Output: []

// Unicode characters
var unicode: string = "Ωμ";  
var unicodeCodes = call str.charArray(unicode);
// Returns Unicode code points for Greek characters

// Use in loops for character processing
var word: string = "Code";
var charCodes = call str.charArray(word);
foreach code in charCodes {
    print "Code: " + string(code);
}
```

**Features:**
- Returns `int[]` array of Unicode code points
- Works with ASCII, Unicode, and special characters
- Returns empty array for empty strings
- Useful for character-level string analysis
- Can be used for custom encoding/decoding operations

#### str.replaceFirst() - Replace First Occurrence
Replaces only the first occurrence of a target string with a replacement. Uses literal string matching (not regex).

```javascript
// Basic usage
var text: string = "Hello World Hello";
var result = call str.replaceFirst(text, "Hello", "Hi");
print result;  // Output: "Hi World Hello"

// Only replaces the first match
var text2: string = "banana";
var result2 = call str.replaceFirst(text2, "a", "o");
print result2;  // Output: "bonana"

// No match returns original string
var text3: string = "Hello World";
var result3 = call str.replaceFirst(text3, "xyz", "123");
print result3;  // Output: "Hello World"

// Compare with str.replace (replaces all)
var allReplaced = call str.replace(text2, "a", "o");
print allReplaced;  // Output: "bonono"
```

**Features:**
- Replaces only the first occurrence
- Uses literal string matching (not regex)
- Returns original string if target not found
- Use `str.replace()` for replacing all occurrences

#### str.findRegex() - Find First Regex Match
Finds the first occurrence of a regex pattern in the string. Returns the matched substring, or null if no match found.

```javascript
// Basic usage
var text: string = "Hello World 123";
var firstWord = call str.findRegex(text, "\\w+");
print firstWord;  // Output: "Hello"

// Find a number
var number = call str.findRegex(text, "\\d+");
print number;  // Output: "123"

// No match returns null
var noMatch = call str.findRegex(text, "xyz");
print noMatch;  // Output: null

// Find email pattern
var email: string = "Contact: john@example.com for info";
var foundEmail = call str.findRegex(email, "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
print foundEmail;  // Output: "john@example.com"

// Use in conditionals
if call str.findRegex(text, "\\d+") != null then {
    print "Text contains a number";
}
```

**Features:**
- Returns the matched substring (not position)
- Returns null if no match found
- Uses Java regex syntax
- Backslashes must be escaped (`\\d+` not `\d+`)

#### str.findAllRegex() - Find All Regex Matches
Finds all occurrences of a regex pattern in the string. Returns an ArrayFixed containing all matched substrings.

```javascript
// Basic usage
var text: string = "Hello World 123 Test 456";
var words = call str.findAllRegex(text, "\\w+");
print words;  // Output: ["Hello", "World", "123", "Test", "456"]
print words.length;  // Output: 5

// Find all numbers
var numbers = call str.findAllRegex(text, "\\d+");
print numbers;  // Output: ["123", "456"]

// Find all email addresses
var content: string = "Contact john@example.com or jane@test.org";
var emails = call str.findAllRegex(content, "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
print emails;  // Output: ["john@example.com", "jane@test.org"]

// No matches returns empty array
var noMatches = call str.findAllRegex(text, "xyz");
print noMatches;  // Output: []
print noMatches.length;  // Output: 0

// Process matches in a loop
var matches = call str.findAllRegex(text, "\\d+");
foreach match in matches {
    print "Found number: " + match;
}
```

**Features:**
- Returns ArrayFixed of all matched substrings
- Returns empty array if no matches found
- Uses Java regex syntax
- Useful for extracting multiple patterns from text
- Backslashes must be escaped (`\\d+` not `\d+`)

#### string.contains() - Check for Substring
Checks if a string contains a specified substring. Returns `true` if the substring is found, `false` otherwise.

```javascript
// Basic usage
var text: string = "Hello World";
var hasWorld = call string.contains(text, "World");
print hasWorld;  // Output: true

var hasXyz = call string.contains(text, "xyz");
print hasXyz;  // Output: false

// Case-sensitive matching
var hasHello = call string.contains(text, "Hello");
print hasHello;  // Output: true

var hasHELLO = call string.contains(text, "HELLO");
print hasHELLO;  // Output: false (case-sensitive)

// Use in conditionals
if call string.contains(filename, ".css") then {
    print "This is a CSS file";
}

// Also available as str.contains
var result = call str.contains("Hello", "ell");
print result;  // Output: true
```

**Features:**
- Case-sensitive substring matching
- Returns boolean (`true` or `false`)
- Available as both `string.contains` and `str.contains`
- Returns `false` if either argument is `null`

#### str.substring() - Extract Substring
Extracts a portion of a string from beginIndex to endIndex (exclusive).

```javascript
// Basic usage - from index to end
var text: string = "Hello World";
var result = call str.substring(text, 6);
print result;  // Output: "World"

// With end index
var result2 = call str.substring(text, 0, 5);
print result2;  // Output: "Hello"

// Extract middle portion
var result3 = call str.substring(text, 3, 8);
print result3;  // Output: "lo Wo"
```

**Features:**
- Uses Java String.substring() semantics
- beginIndex is inclusive, endIndex is exclusive
- If endIndex is omitted, extracts to end of string
- Throws error if indices are out of bounds

#### str.indexOf() - Find First Occurrence
Finds the first occurrence of a substring in the string.

```javascript
// Basic usage
var text: string = "Hello World Hello";
var index = call str.indexOf(text, "Hello");
print index;  // Output: 0

// Find second occurrence
var index2 = call str.indexOf(text, "Hello", 1);
print index2;  // Output: 12

// Not found returns -1
var notFound = call str.indexOf(text, "xyz");
print notFound;  // Output: -1
```

**Features:**
- Returns index of first occurrence, or -1 if not found
- Optional fromIndex parameter to start search from a position
- Case-sensitive matching

#### str.lastIndexOf() - Find Last Occurrence
Finds the last occurrence of a substring in the string.

```javascript
// Basic usage
var text: string = "Hello World Hello";
var index = call str.lastIndexOf(text, "Hello");
print index;  // Output: 12

// With fromIndex (search backwards from position)
var index2 = call str.lastIndexOf(text, "Hello", 10);
print index2;  // Output: 0

// Not found returns -1
var notFound = call str.lastIndexOf(text, "xyz");
print notFound;  // Output: -1
```

**Features:**
- Returns index of last occurrence, or -1 if not found
- Optional fromIndex parameter to limit search range
- Case-sensitive matching

#### str.charAt() - Get Character at Index
Returns the character at the specified index as a single-character string.

```javascript
// Basic usage
var text: string = "Hello";
var char1 = call str.charAt(text, 0);
print char1;  // Output: "H"

var char2 = call str.charAt(text, 4);
print char2;  // Output: "o"

// Use in loop
for (var i: int = 0; i < 5; i++) {
    print call str.charAt(text, i);
}
```

**Features:**
- Returns single-character string (not char type)
- Throws error if index is out of bounds
- Use with str.charArray() if you need character codes

#### str.replaceAll() - Regex Replace All
Replaces all occurrences of a regex pattern with a replacement string.

**Important:** Since EBS strings use backslash escaping, regex backslashes must be double-escaped (e.g., `\\d` instead of `\d`).

```javascript
// Basic usage - remove all digits (\\d+ matches one or more digits)
var text: string = "abc123def456";
var result = call str.replaceAll(text, "\\d+", "");
print result;  // Output: "abcdef"

// Replace all whitespace (\\s+ matches one or more whitespace chars)
var text2: string = "Hello   World  Test";
var result2 = call str.replaceAll(text2, "\\s+", " ");
print result2;  // Output: "Hello World Test"

// Use capture groups
var text3: string = "Hello World";
var result3 = call str.replaceAll(text3, "(\\w+)", "[$1]");
print result3;  // Output: "[Hello] [World]"
```

**Features:**
- Uses Java regex syntax
- Backslashes must be double-escaped (`\\d+` not `\d+`) in EBS strings
- Supports capture group references in replacement
- Use str.replace() for literal (non-regex) replacement

#### str.lpad() - Left Pad String
Left-pads a string with a character to reach a specified length.

```javascript
// Basic usage - pad with zeros
var num: string = "42";
var result = call str.lpad(num, 5, "0");
print result;  // Output: "00042"

// Pad with spaces
var text: string = "Hi";
var result2 = call str.lpad(text, 10, " ");
print result2;  // Output: "        Hi"

// Already at or exceeds length - no change
var text3: string = "Hello";
var result3 = call str.lpad(text3, 3, "0");
print result3;  // Output: "Hello"
```

**Features:**
- padChar must be exactly one character
- Returns original string if already >= length
- Useful for formatting numbers and alignment

#### str.rpad() - Right Pad String
Right-pads a string with a character to reach a specified length.

```javascript
// Basic usage - pad with spaces
var text: string = "Hi";
var result = call str.rpad(text, 10, " ");
print result;  // Output: "Hi        "

// Pad with dots
var text2: string = "Loading";
var result2 = call str.rpad(text2, 10, ".");
print result2;  // Output: "Loading..."

// Already at or exceeds length - no change
var text3: string = "Hello";
var result3 = call str.rpad(text3, 3, "0");
print result3;  // Output: "Hello"
```

**Features:**
- padChar must be exactly one character
- Returns original string if already >= length
- Useful for formatting and alignment


### JSON Functions
```javascript
// Parsing and conversion
call json.jsonfromstring(jsonString);
call string.tostring(jsonObject);

// Schema operations
call json.derivescheme(jsonObject);
call json.registerscheme(name, schema);
call json.validate(jsonObject, schema);

// Data access
call json.get(jsonObject, path);
call json.getstrict(jsonObject, path);
call json.getstring(jsonObject, path);
call json.getint(jsonObject, path);
call json.getinteger(jsonObject, path);  // Alias for json.getint
call json.getlong(jsonObject, path);
call json.getdouble(jsonObject, path);
call json.getbool(jsonObject, path);
call json.getarray(jsonObject, path);
call json.getobject(jsonObject, path);

// Data modification
call json.set(jsonObject, path, value);
call json.setstrict(jsonObject, path, value);
call json.remove(jsonObject, path);
call json.add(array, value);
call json.insert(array, index, value);

// Queries
call json.isempty(jsonObject);
```

### File Functions
```javascript
// File info
call file.exists(path);
call file.size(path);

// Text file operations
call file.readtextfile(path);
call file.writetextfile(path, content);
call file.appendtotextfile(path, content);

// Binary file operations
call file.readbinfile(path);
call file.writebinfile(path, byteArray);

// Streaming operations
var handle = call file.open(path, mode);  // mode: "r", "w", "a"
var line = call file.readln(handle);
call file.writeln(handle, text);
call file.write(handle, text);
var text = call file.read(handle, bytes);
var byteArray = call file.readbin(handle);
call file.writebin(handle, byteArray);
var isEof = call file.eof(handle);
call file.close(handle);

// File management
call file.listfiles(directory);
call file.rename(oldPath, newPath);
call file.move(source, destination);
call file.copy(source, destination);

// List open files
call file.listopenfiles();

// ZIP operations
var zipHandle = call file.openzip(zipPath, mode);  // mode: "r" (read) or "w" (write/create)
var files = call file.listzipfiles(zipHandle);
call file.unzip(zipHandle, entryName, targetPath);
call file.closezip(zipHandle);
```

### HTTP Functions
```javascript
// Basic requests
var response = call http.request(url, method, headers, body);
var response = call http.get(url, headers);
var response = call http.post(url, headers, body);

// Text requests
var text = call http.gettext(url, headers);
var text = call http.posttext(url, headers, body);

// JSON requests
var json = call http.getjson(url, headers);
var json = call http.postjson(url, headers, jsonBody);

// Response checking
call http.ensure2xx(response);  // Throws error if not 2xx
var isOk = call http.is2xx(response);  // Returns boolean
```

### Mail Functions

The mail functions allow you to connect to email servers (IMAP/POP3), list emails, and retrieve email content.

#### Mail URL Format

Mail connections can be configured using URL format:
```
mail://user:password@host:port?protocol=imaps
```

**Example URLs:**
- `mail://user%40gmail.com:apppassword@imap.gmail.com:993?protocol=imaps`
- `mail://user:password@outlook.office365.com:993?protocol=imaps`

**Note:** Use URL encoding for special characters (e.g., `%40` for `@` in email addresses).

```javascript
// Connect to mail server
var handle = call mail.open(host, port, user, password, protocol);

// List folders available in the mailbox
var folders = call mail.folders(handle);

// List emails in a folder (default INBOX)
var messages = call mail.list(handle, folder, start, count);

// Get full content of a specific email
var email = call mail.get(handle, messageId);

// Close the connection
call mail.close(handle);
```

#### mail.open(host, port, user, password, protocol?, timeout?)
Connects to a mail server and returns a connection handle for subsequent operations.

**Parameters:**
- `host` (string, required): Mail server hostname (e.g., "imap.gmail.com")
- `port` (integer, required): Server port (e.g., 993 for IMAPS, 143 for IMAP, 995 for POP3S, 110 for POP3)
- `user` (string, required): Username or email address
- `password` (string, required): Password or app-specific password (see note below)
- `protocol` (string, optional): Protocol to use. Default is "imaps". Supported: "imap", "imaps", "pop3", "pop3s"
- `timeout` (integer, optional): Connection timeout in seconds. Default is 30.

**Returns:** String - a connection handle to use with other mail functions

**Note on Gmail/Google Workspace:** Gmail requires an [App Password](https://support.google.com/accounts/answer/185833) when 2-Factor Authentication is enabled. Regular passwords will not work. **Important:** Gmail App Passwords are 16 characters with no spaces (displayed as `xxxx xxxx xxxx xxxx` but enter without spaces). Generate one from Google Account > Security > App Passwords.

**Note on SSL:** When using SSL protocols (imaps, pop3s), the implementation trusts all SSL certificates for compatibility. For production environments requiring strict certificate validation, additional configuration may be needed.

```javascript
// Connect to Gmail via IMAPS (SSL) - use an app password (16 chars, no spaces)
var handle = call mail.open("imap.gmail.com", 993, "myemail@gmail.com", "xxxxyyyyzzzzwwww", "imaps");

// Connect to a local server without SSL
var handle2 = call mail.open("localhost", 143, "testuser", "password", "imap");

// Connect with a custom timeout (60 seconds)
var handle3 = call mail.open("imap.gmail.com", 993, "user@gmail.com", "xxxxyyyyzzzzwwww", "imaps", 60);
```

#### mail.folders(handle)
Lists all available folders in the mail account.

**Parameters:**
- `handle` (string, required): Connection handle from mail.open

**Returns:** JSON array of folder information objects with properties:
- `name`: Full folder name/path
- `type`: Folder type ("messages", "folders", or "messages,folders")
- `messageCount`: Number of messages (if folder holds messages)
- `unreadCount`: Number of unread messages (if folder holds messages)

```javascript
var folders = call mail.folders(handle);
foreach folder in folders {
    print folder.name + " (" + folder.messageCount + " messages)";
}
```

#### mail.list(handle, folder?, start?, count?)
Lists emails in the specified folder.

**Parameters:**
- `handle` (string, required): Connection handle from mail.open
- `folder` (string, optional): Folder name. Default is "INBOX"
- `start` (integer, optional): Starting message number (1-based). Default is 1
- `count` (integer, optional): Maximum number of messages to return. Default is 50

**Returns:** JSON array of message summary objects with properties:
- `id`: Message number (use with mail.get)
- `subject`: Email subject
- `from`: Sender email address
- `fromName`: Sender display name
- `date`: Sent date as milliseconds since epoch
- `dateStr`: Sent date as formatted string
- `size`: Message size in bytes
- `read`: Boolean indicating if message has been read

```javascript
// List first 10 messages from INBOX
var messages = call mail.list(handle, "INBOX", 1, 10);
foreach msg in messages {
    var status = "";
    if msg.read == false then {
        status = "[NEW] ";
    }
    print status + msg.subject + " - from: " + msg.from;
}

// List messages from a different folder
var drafts = call mail.list(handle, "Drafts");
```

#### mail.get(handle, messageId)
Retrieves the full content of a specific email.

**Parameters:**
- `handle` (string, required): Connection handle from mail.open
- `messageId` (integer, required): Message ID from mail.list results

**Returns:** JSON object with full message content:
- `id`: Message number
- `subject`: Email subject
- `from`: Array of sender address objects (address, name)
- `to`: Array of recipient address objects
- `cc`: Array of CC address objects
- `bcc`: Array of BCC address objects
- `date`: Sent date as milliseconds since epoch
- `dateStr`: Sent date as formatted string
- `size`: Message size in bytes
- `read`: Boolean indicating if message has been read
- `contentType`: MIME content type
- `body`: Email body text (plain text or HTML)
- `attachments`: Array of attachment info objects (filename, contentType, size)

```javascript
// Get full email content
var email = call mail.get(handle, 1);
print "Subject: " + email.subject;
print "From: " + email.from[0].address;
print "Date: " + email.dateStr;
print "";
print email.body;

// Check for attachments
if email.attachments.length > 0 then {
    print "Attachments:";
    foreach att in email.attachments {
        print "  - " + att.filename + " (" + att.size + " bytes)";
    }
}
```

#### mail.close(handle)
Closes a mail server connection.

**Parameters:**
- `handle` (string, required): Connection handle from mail.open

**Returns:** Boolean - true if closed successfully

```javascript
call mail.close(handle);
```

#### Complete Mail Example
```javascript
// Connect to mail server
var handle = call mail.open("imap.gmail.com", 993, "user@gmail.com", "app-password");

// List available folders
var folders = call mail.folders(handle);
foreach f in folders {
    print "Folder: " + f.name;
}

// Get list of messages from inbox
var messages = call mail.list(handle, "INBOX", 1, 5);
print "Found " + messages.length + " messages";

// Display each message
foreach msg in messages {
    print "---";
    print "ID: " + msg.id;
    print "From: " + msg.from;
    print "Subject: " + msg.subject;
    print "Date: " + msg.dateStr;
    
    // Get full message content
    var full = call mail.get(handle, msg.id);
    print "Body preview: " + call str.substring(full.body, 0, 100) + "...";
}

// Clean up
call mail.close(handle);
```

### CSS Functions
```javascript
// Get property value from CSS stylesheet
var color = call css.getValue(cssPath, selector, property);
```

#### css.getValue(cssPath, selector, property)
Retrieves a CSS property value from a stylesheet file. This function can read CSS files from classpath resources or filesystem paths.

**Parameters:**
- `cssPath` (string, required): Path to the CSS file. Can be a classpath resource (e.g., "css/console.css") or a filesystem path
- `selector` (string, required): CSS selector to look up (e.g., ".error", "#main", ".console-frame .text-area")
- `property` (string, required): CSS property name to retrieve (e.g., "-fx-fill", "-fx-font-weight", "-fx-background-color")

**Returns:** String - the property value, or `null` if the selector or property is not found

```javascript
// Basic usage - get fill color for error class
var errorColor = call css.getValue("css/console.css", ".error", "-fx-fill");
print errorColor;  // Output: #ee0000

// Get font weight for keyword styling
var keywordWeight = call css.getValue("css/console.css", ".keyword", "-fx-font-weight");
print keywordWeight;  // Output: bold

// Multi-part selectors
var bgColor = call css.getValue("css/console.css", ".console-frame .text-area", "-fx-background-color");
print bgColor;  // Output: #000000

// Read from different CSS files
var headerBg = call css.getValue("css/screen-areas.css", ".screen-area-header", "-fx-background-color");
print headerBg;  // Output: #ffffff

// Handle non-existent selector (returns null)
var missing = call css.getValue("css/console.css", ".nonexistent", "-fx-fill");
if missing == null then {
    print "Selector not found";
}

// Handle non-existent property (returns null)
var noProperty = call css.getValue("css/console.css", ".error", "nonexistent-prop");
if noProperty == null then {
    print "Property not found";
}

// Extract theme colors for dynamic styling
var themeError = call css.getValue("css/console.css", ".error", "-fx-fill");
var themeWarn = call css.getValue("css/console.css", ".warn", "-fx-fill");
var themeOk = call css.getValue("css/console.css", ".ok", "-fx-fill");
print "Error: " + themeError + ", Warn: " + themeWarn + ", OK: " + themeOk;
```

**Features:**
- Reads CSS from classpath resources (e.g., "css/console.css") or filesystem paths
- Handles multi-selector rules (e.g., ".a, .b { ... }")
- Correctly parses CSS comments (single and multi-line)
- Handles values containing semicolons in quoted strings
- Caches parsed CSS files for improved performance
- Case-insensitive property name matching
- Skips @-rules (media queries, keyframes)

**Use Cases:**
- Extract theme colors for dynamic UI styling
- Read font settings from stylesheets
- Validate CSS property values programmatically
- Build style-aware applications that adapt to CSS changes

#### css.findCss(searchPath?)
Searches for all available CSS stylesheet files and returns their paths as a string array.

**Parameters:**
- `searchPath` (string, optional): Base path to search in. If not provided, searches in default locations (classpath css/ folder and sandbox)

**Returns:** String[] - an array of paths to all found CSS files

```javascript
// Find all available CSS files
var cssFiles = call css.findCss();
print "Found " + cssFiles.length + " CSS files:";
foreach file in cssFiles {
    print "  - " + file;
}

// Search in a specific directory
var customCss = call css.findCss("/path/to/styles");
foreach file in customCss {
    print file;
}

// Use with css.getValue to iterate over all stylesheets
var allCss = call css.findCss();
foreach cssFile in allCss {
    var errorColor = call css.getValue(cssFile, ".error", "-fx-fill");
    if errorColor != null then {
        print cssFile + " defines .error color: " + errorColor;
    }
}
```

**Features:**
- Searches classpath resources (css/ folder)
- Searches filesystem paths (sandbox directory)
- Returns absolute paths for filesystem files
- Returns relative paths for classpath resources (e.g., "css/console.css")
- Recursive directory search when searching filesystem paths

#### css.loadCss(screenName, cssPath)
Dynamically loads a CSS stylesheet and applies it to the specified screen at runtime. Enables custom styling and theme switching without restarting the application.

**Parameters:**
- `screenName` (string, required): Name of the screen to apply CSS to (case-insensitive)
- `cssPath` (string, required): Path to the CSS file. Supports multiple formats:
  - Classpath resources: `"css/custom.css"` or `"custom.css"` (auto-prefixes `/css/`)
  - Relative paths: `"my-theme.css"` (resolves in sandbox/current directory)
  - Absolute paths: `"/path/to/styles.css"`
  - URLs: `"file:///path/to/styles.css"`

**Returns:** Boolean - true if CSS loading was successfully scheduled

```javascript
// Define a screen with CSS classes
screen myScreen = {
    "title": "Styled Application",
    "width": 600,
    "height": 400,
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "items": [
                {
                    "name": "title",
                    "type": "label",
                    "text": "Welcome",
                    "cssClass": "custom-title"  // Reference CSS class
                },
                {
                    "name": "loadTheme",
                    "type": "button",
                    "text": "Load Dark Theme",
                    "onClick": "call css.loadCss('myscreen', 'dark-theme.css');"
                }
            ]
        }
    ]
};

show screen myScreen;

// Load CSS after showing screen
call css.loadCss("myscreen", "custom-styles.css");
```

**Important Notes:**
- Screen must be shown before loading CSS
- CSS applies immediately to the JavaFX scene
- Duplicate CSS files are automatically prevented
- Operation is asynchronous (returns immediately, CSS loads on JavaFX thread)
- **Best Practice**: Organize screen apps with CSS files in their own directory

**Recommended Directory Structure:**
```
my-screen-app/
├── my-app.ebs          # EBS script with screen definitions
├── custom-theme.css    # CSS file for styling
└── README.md           # Optional documentation
```

#### css.unloadCss(screenName, cssPath)
Removes a previously loaded CSS stylesheet from the specified screen, reverting to default or remaining styles.

**Parameters:**
- `screenName` (string, required): Name of the screen to remove CSS from (case-insensitive)
- `cssPath` (string, required): Path to the CSS file to remove (same format as loadCss)

**Returns:** Boolean - true if CSS removal was successfully scheduled

```javascript
// Remove custom CSS from screen
call css.unloadCss("myscreen", "dark-theme.css");

// Example: Theme switcher
switchTheme(themeName: string) {
    // Unload current theme
    call css.unloadCss("myscreen", currentTheme);
    // Load new theme
    call css.loadCss("myscreen", themeName + ".css");
    currentTheme = themeName;
}

// Usage in button click handlers
screen themeDemo = {
    "area": [
        {
            "name": "controls",
            "items": [
                {
                    "name": "darkBtn",
                    "type": "button",
                    "text": "Dark Theme",
                    "onClick": "call css.unloadCss('themedemo', 'light-theme.css'); call css.loadCss('themedemo', 'dark-theme.css');"
                },
                {
                    "name": "lightBtn",
                    "type": "button",
                    "text": "Light Theme",
                    "onClick": "call css.unloadCss('themedemo', 'dark-theme.css'); call css.loadCss('themedemo', 'light-theme.css');"
                }
            ]
        }
    ]
};
```

**Use Cases:**
- **Theme Switching**: Toggle between light/dark themes
- **Dynamic Styling**: Apply conditional styles based on application state
- **Screen-Specific Styles**: Load/unload CSS per screen
- **A/B Testing**: Switch styles for user testing

**Complete Example:**
See `ScriptInterpreter/scripts/examples/css-screen-demo/` for a full working example with organized CSS files.

### Array Functions
```javascript
// Array manipulation
call array.expand(array, newSize);
call array.sort(array);
call array.fill(array, value);

// Add and remove elements
call array.add(array, value);           // Add to end
call array.add(array, value, index);    // Insert at index
var removed = call array.remove(array, index);  // Remove at index, returns removed value

// Bidirectional iteration
var reverseWrapper = call array.reverse(array);  // Returns reverse iteration wrapper
foreach item in #array.reverse(array) {          // Iterate in reverse
    print item;
}

// Type casting for byte/bitmap arrays
var bitmapArray = call array.asBitmap(byteArray);
var byteArray = call array.asByte(bitmapArray);

// Type casting for int/intmap arrays
var intmapArray = call array.asIntmap(intArray);
var intArray = call array.asInt(intmapArray);

// Encoding
var encoded = call array.base64encode(byteArray);
var decoded = call array.base64decode(encodedString);
```

#### array.asBitmap(array)
Casts an `array.byte` to `array.bitmap`. Both types use the same ArrayFixedByte storage, so this is a zero-cost type conversion.

**Parameters:**
- `array` (array.byte, required): The byte array to cast

**Returns:** array.bitmap - The same array with BITMAP type designation

```javascript
var byteArray: array.byte[5];
byteArray[0] = 15;

var bitmapArray = call array.asBitmap(byteArray);
// Now can use bitmap type aliases for field access
StatusFlags typeof bitmap { read: 0, write: 1, execute: 2, admin: 3 };
var flags = StatusFlags(bitmapArray[0]);
print flags.read;  // 1
```

#### array.asByte(array)
Casts an `array.bitmap` to `array.byte`. Both types use the same ArrayFixedByte storage, so this is a zero-cost type conversion.

**Parameters:**
- `array` (array.bitmap, required): The bitmap array to cast

**Returns:** array.byte - The same array with BYTE type designation

```javascript
var bitmapArray: array.bitmap[5];
bitmapArray[0] = 15;

var byteArray = call array.asByte(bitmapArray);
print byteArray[0];  // 15 (raw byte value)
```

#### array.asIntmap(array)
Casts an `array.int` to `array.intmap`. Both types use the same ArrayFixedInt storage, so this is a zero-cost type conversion.

**Parameters:**
- `array` (array.int, required): The integer array to cast

**Returns:** array.intmap - The same array with INTMAP type designation

```javascript
var intArray: array.int[5];
intArray[0] = 1048832;

var intmapArray = call array.asIntmap(intArray);
// Now can use intmap type aliases for field access
ConfigData typeof intmap { enabled: 0, mode: 1-3, level: 4-11, id: 12-31 };
var config = ConfigData(intmapArray[0]);
print config.enabled;  // Extract enabled bit
print config.id;       // Extract id field
```

#### array.asInt(array)
Casts an `array.intmap` to `array.int`. Both types use the same ArrayFixedInt storage, so this is a zero-cost type conversion.

**Parameters:**
- `array` (array.intmap, required): The intmap array to cast

**Returns:** array.int - The same array with INTEGER type designation

```javascript
var intmapArray: array.intmap[5];
intmapArray[0] = 1048832;

var intArray = call array.asInt(intmapArray);
print intArray[0];  // 1048832 (raw integer value)
```

#### array.reverse(array)
Returns a wrapper that enables reverse iteration over the array using foreach. The array itself is not modified or copied - only a lightweight wrapper (~24 bytes) is created, providing O(1) memory overhead for bidirectional iteration.

**Parameters:**
- `array` (array, required): The array to iterate in reverse

**Returns:** Iterable wrapper - Can be used with foreach for reverse iteration

**Supported Array Types:** All array types (dynamic, fixed, byte, int, bitmap, intmap)

**Example - Basic Reverse Iteration:**
```javascript
var numbers = [1, 2, 3, 4, 5];

// Forward iteration (normal)
foreach num in numbers {
    print num;  // Output: 1, 2, 3, 4, 5
}

// Reverse iteration
foreach num in #array.reverse(numbers) {
    print num;  // Output: 5, 4, 3, 2, 1
}
```

**Example - Countdown:**
```javascript
var countdown = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1];

foreach num in #array.reverse(countdown) {
    print "T-minus " + num;
}
print "Liftoff!";
// Output: T-minus 1, T-minus 2, ..., T-minus 10, Liftoff!
```

**Example - Reverse Search:**
```javascript
var items = ["apple", "banana", "cherry", "date"];

// Find from the end
foreach item in #array.reverse(items) {
    if (item == "banana") {
        print "Found: " + item;
        break;
    }
}
```

**Example - Stack-like LIFO Processing:**
```javascript
var stack = [];
call array.add(stack, "First");
call array.add(stack, "Second");
call array.add(stack, "Third");

// Process in LIFO order (Last-In-First-Out)
foreach item in #array.reverse(stack) {
    print item;  // Output: Third, Second, First
}
```

**Example - Works with Fixed Arrays:**
```javascript
var fixed: int[4];
fixed[0] = 100;
fixed[1] = 200;
fixed[2] = 300;
fixed[3] = 400;

foreach val in #array.reverse(fixed) {
    print val;  // Output: 400, 300, 200, 100
}
```

**Performance:**
- **Time Complexity:** O(n) - same as forward iteration
- **Space Complexity:** O(1) - only a wrapper object (~24 bytes)
- **No Array Copying:** The original array is never duplicated
- **No Mutation:** The array remains unchanged

**Use Cases:**
- Processing items in reverse order
- Countdown operations
- Stack-like LIFO processing
- Reverse search operations
- Undo/history traversal

### Queue Functions

Queues provide FIFO (First-In-First-Out) data structures. Declare queues using the `queue.type` syntax:

```javascript
// Queue declaration
var stringQueue: queue.string;
var intQueue: queue.int;
var doubleQueue: queue.double;

// Also available: queue.byte, queue.long, queue.float, queue.bool, queue.date
```

#### Queue Operations

```javascript
// Add element to the back of the queue
call queue.enqueue(queue, value);

// Remove and return element from the front
var item = call queue.dequeue(queue);

// View front element without removing
var item = call queue.peek(queue);

// Get number of elements
var count = call queue.size(queue);

// Check if queue is empty
var isEmpty = call queue.isEmpty(queue);

// Remove all elements
call queue.clear(queue);

// Check if queue contains a value
var found = call queue.contains(queue, value);

// Convert queue to array
var arr = call queue.toArray(queue);
```

#### Queue Example: Task Processing

```javascript
// Create a task queue
var tasks: queue.string;

// Add tasks
call queue.enqueue(tasks, "Process file A");
call queue.enqueue(tasks, "Process file B");
call queue.enqueue(tasks, "Send notification");

// Process all tasks in order
while !call queue.isEmpty(tasks) {
    var task = call queue.dequeue(tasks);
    print "Executing: " + task;
}
```

### System Functions
```javascript
// Execute commands
var result = call system.command(command);
var result = call system.wincommand(command);  // Windows-specific

// System properties
var value = call system.getproperty(name);
call system.setproperty(name, value);

// Help system
call system.help();

// Dialog functions
var userInput = call system.inputDialog(title, headerText?, defaultValue?);
var confirmed = call system.confirmDialog(message, title?, headerText?);
call system.alertDialog(message, title?, alertType?);

// Thread/timing
call thread.sleep(milliseconds);  // Pause execution
var timerName = call thread.timerStart(name, period, callback);  // Start repeating timer
var stopped = call thread.timerStop(name);  // Stop repeating timer
```

#### thread.sleep(milliseconds)
Pauses the current thread execution for the specified number of milliseconds.

```javascript
// Pause for 1 second
call thread.sleep(1000);

// Pause for half a second
call thread.sleep(500);

// Use in a loop with delay
for (var i: int = 0; i < 5; i++) {
    print "Step " + i;
    call thread.sleep(500);  // Wait 500ms between steps
}
```

**Parameters:**
- `milliseconds` (long, required): Number of milliseconds to pause

**Returns:** String (usually empty)

**Use Cases:**
- Adding delays between operations
- Rate limiting API calls
- Animation timing
- Waiting for resources to become available

#### thread.timerStart(name, period, callback)
Starts a repeating timer that invokes a callback function at fixed intervals. The timer runs asynchronously in the background and continues until explicitly stopped with `thread.timerStop()`.

```javascript
// Define callback function (receives timer name as parameter)
timerCallback(timerName: string) {
    print "Timer fired: " + timerName;
}

// Start timer that fires every 1000ms (1 second)
var name: string = call thread.timerStart("myTimer", 1000, "timerCallback");

// Stop the timer when done
call thread.timerStop("myTimer");
```

**Parameters:**
- `name` (string, required): Unique identifier for the timer
- `period` (long, required): Time interval in milliseconds between callback invocations
- `callback` (string, required): Name of the function to call when the timer fires

**Returns:** String - The timer name for convenience

**Behavior:**
- Callbacks execute on the JavaFX Application Thread for UI safety
- Multiple timers can run concurrently with unique names
- Starting a timer with an existing name replaces the old timer
- The callback function receives the timer name as its only parameter

**Use Cases:**
- Periodic polling or checking
- Animation updates
- Auto-refresh functionality
- Background monitoring tasks
- Countdown timers

**Example - Self-stopping timer:**
```javascript
var count: int = 0;

countdownCallback(name: string) {
    count = count + 1;
    print "Count: " + call str.toString(count);
    
    // Stop after 5 iterations
    if count >= 5 then
        call thread.timerStop(name);
        print "Timer stopped!";
    end
}

call thread.timerStart("countdown", 1000, "countdownCallback");
```

**Example - Multiple concurrent timers:**
```javascript
fastCallback(name: string) {
    print "Fast: " + name;
}

slowCallback(name: string) {
    print "Slow: " + name;
}

// Start two timers with different periods
call thread.timerStart("fast", 500, "fastCallback");
call thread.timerStart("slow", 2000, "slowCallback");

// Stop both when done
call thread.timerStop("fast");
call thread.timerStop("slow");
```

#### thread.timerStop(name)
Stops a repeating timer that was started with `thread.timerStart()`. The timer is cancelled gracefully and removed from the registry.

```javascript
// Stop a running timer
var stopped: bool = call thread.timerStop("myTimer");

if stopped then
    print "Timer stopped successfully";
else
    print "Timer not found (already stopped or never started)";
end
```

**Parameters:**
- `name` (string, required): The name of the timer to stop

**Returns:** Boolean - `true` if timer was stopped, `false` if timer was not found

**Behavior:**
- Safe to call on non-existent timers (returns false)
- Does not interrupt currently executing callbacks
- Thread-safe operation

**Use Cases:**
- Stopping timers when conditions are met
- Cleanup on exit or navigation
- Manual stop controls
- Timer lifecycle management

#### Dialog Functions

EBS provides built-in dialog functions for user interaction:

##### system.inputDialog(title, headerText?, defaultValue?)
Shows an input dialog that prompts the user for text input.

**Parameters:**
- `title` (string, required): The dialog window title
- `headerText` (string, optional): Header text displayed above the input field
- `defaultValue` (string, optional): Default value pre-filled in the input field

**Returns:** String - the text entered by the user, or empty string if cancelled

```javascript
// Basic input dialog
var name = call system.inputDialog("Enter Name");

// With header text
var email = call system.inputDialog("Contact Info", "Please enter your email address");

// With default value
var username = call system.inputDialog("Username", "Enter your username", "guest");

// Example usage
var userInput = call system.inputDialog("Search", "Enter search term:", "");
if userInput != "" then {
    print "Searching for: " + userInput;
}
```

##### system.confirmDialog(message, title?, headerText?)
Shows a confirmation dialog with YES and NO buttons. Returns a boolean indicating the user's choice.

**Parameters:**
- `message` (string, required): The confirmation message to display
- `title` (string, optional): The dialog window title (default: "Confirm")
- `headerText` (string, optional): Header text displayed above the message

**Returns:** Boolean - `true` if user clicks YES, `false` if user clicks NO or closes the dialog

```javascript
// Basic confirmation
var confirmed = call system.confirmDialog("Are you sure?");
if confirmed then {
    print "User confirmed";
}

// With custom title
var deleteConfirmed = call system.confirmDialog("Delete this file?", "Confirm Delete");

// With header text
var saveConfirmed = call system.confirmDialog(
    "Do you want to save changes?",
    "Save Changes",
    "You have unsaved modifications"
);

// Example: Confirm before destructive action
var proceed = call system.confirmDialog("This action cannot be undone. Continue?", "Warning");
if proceed then {
    // Perform the action
    print "Action executed";
} else {
    print "Action cancelled";
}
```

##### system.alertDialog(message, title?, alertType?)
Shows a message dialog with only an OK button. Used to display information, warnings, or errors to the user.

**Parameters:**
- `message` (string, required): The message to display
- `title` (string, optional): The dialog window title (default: "Alert")
- `alertType` (string, optional): The type of alert - "info", "warning", or "error" (default: "info")

**Returns:** Nothing (void)

```javascript
// Basic information alert
call system.alertDialog("Operation completed successfully");

// With custom title
call system.alertDialog("File saved", "Success");

// Warning alert
call system.alertDialog("Disk space is running low", "Warning", "warning");

// Error alert
call system.alertDialog("Failed to connect to database", "Error", "error");

// Information alert (explicit)
call system.alertDialog("Welcome to the application!", "Welcome", "info");

// Example: Show completion message
call system.alertDialog("Export completed. 150 records exported.", "Export Complete", "info");
```

### Screen Functions
```javascript
// Check if a screen has been defined
var exists = call scr.findScreen("screenName");
// Returns true if screen exists (has configuration, Stage, or is declared)
// Case-insensitive lookup

// Get/set property value on a screen item
var value = call scr.getProperty("screenName.itemName", "propertyName");
call scr.setProperty("screenName.itemName", "propertyName", value);

// Available item properties:
// - "itemText": Text content for buttons, labels, and other text-based controls
// - "editable": Whether the control can be edited (boolean)
// - "disabled": Whether the control is disabled (boolean)
// - "visible": Whether the control is visible (boolean)
// - "tooltip": Tooltip text shown on hover (string)
// - "textColor": Text color (e.g., "#FF0000", "red")
// - "backgroundColor": Background color (e.g., "#FFFFFF", "white")
// - "prefWidth", "prefHeight": Preferred size (string)
// - "minWidth", "minHeight": Minimum size (string)
// - "maxWidth", "maxHeight": Maximum size (string)
// - "contentAlignment": Content alignment within control (left, center, right)
// - "itemAlignment": Item alignment in container (left, center, right, top-left, etc.)

// itemText property: Update text on buttons, labels, and text-based controls
// Supported controls: Button, Label, Text, Hyperlink, ToggleButton, CheckBox, RadioButton
call scr.setProperty("myScreen.statusButton", "itemText", "Processing...");
call scr.setProperty("myScreen.messageLabel", "itemText", "Task complete!");

// Example: Dynamic text updates with JSON
var config: map = {"button": "Start", "status": "Ready"};
call scr.setProperty("myScreen.actionButton", "itemText", call json.getstring(config, "button"));

// Get list of all item names in a screen
var itemList = call scr.getItemList("screenName");
// Alias: scr.getScreenItemList also available
var itemList = call scr.getScreenItemList("screenName");
// Returns an ArrayDynamic of strings containing all item names

// Show/hide/close screens programmatically
call scr.showScreen("screenName");
call scr.hideScreen("screenName");
call scr.closeScreen("screenName");

// Screen status management
call scr.setStatus("screenName", "clean");    // or "changed" or "error"
var status = call scr.getStatus("screenName");
call scr.setError("screenName", "Error message");
var error = call scr.getError("screenName");

// Check screen state
var hasChanges = call scr.checkChanged("screenName");
var hasErrors = call scr.checkError("screenName");

// Item source and status
var source = call scr.getItemSource("screenName", "itemName");  // "data" or "display"
call scr.setItemSource("screenName", "itemName", "data");
var itemStatus = call scr.getItemStatus("screenName", "itemName");
call scr.resetItemOriginalValue("screenName", "itemName");

// Revert and clear
call scr.revert("screenName");  // Revert to original values
call scr.clear("screenName");   // Clear all values

// Get variable reference
var varRef = call scr.getVarReference("screenName", "itemName");

// Area properties
var prop = call scr.getAreaProperty("screenName.areaName", "propertyName");
call scr.setAreaProperty("screenName.areaName", "propertyName", value);

// Available area properties:
// - "alignment": Container alignment (left, right, center, top-left, etc.)
// - "spacing": Spacing between children
// - "padding": Internal padding
// - "style": CSS style string
// - "areaBackground": Background color
// - "groupBorder": Border style
// - "groupLabelText": Label text for group borders
// - "groupLabelAlignment": Label alignment (left, center, right)
// - Other layout properties (see AREA_DEFINITION.md for complete list)

// Examples:
var currentAlign = call scr.getAreaProperty("myScreen.buttonBar", "alignment");
call scr.setAreaProperty("myScreen.buttonBar", "alignment", "center-right");
call scr.setAreaProperty("myScreen.formArea", "spacing", "20");
call scr.setAreaProperty("myScreen.header", "areaBackground", "#2c3e50");

// Choice/dropdown options
var options = call scr.getItemChoiceOptions("screenName", "itemName");
call scr.setItemChoiceOptions("screenName", "itemName", optionsMap);

// TreeView icon management (dynamically change icons at runtime)
// Set a static icon for a tree item
call scr.setTreeItemIcon("screenName", "itemPath", "icons/file.png");

// Set state-based icons (open/closed) for a tree item
call scr.setTreeItemIcons("screenName", "itemPath", "icons/folder.png", 
                          "icons/folder-open.png", "icons/folder.png");

// Get the current icon path for a tree item
var iconPath = call scr.getTreeItemIcon("screenName", "itemPath");

// TreeView styling (bold, italic, color per tree node)
// Set bold text
call scr.setTreeItemBold("screenName", "itemPath", true);

// Set italic text
call scr.setTreeItemItalic("screenName", "itemPath", true);

// Set text color (hex, rgb, or color name)
call scr.setTreeItemColor("screenName", "itemPath", "#ff0000");  // Red
call scr.setTreeItemColor("screenName", "itemPath", "rgb(255,0,0)");  // Red
call scr.setTreeItemColor("screenName", "itemPath", "red");  // Red

// Combine multiple styles on the same item
call scr.setTreeItemBold("screenName", "itemPath", true);
call scr.setTreeItemItalic("screenName", "itemPath", true);
call scr.setTreeItemColor("screenName", "itemPath", "#0066cc");

// itemPath uses dot notation to navigate tree hierarchy
// Examples: "Root", "Root.src", "Root.src.main.java"

// Examples:
var editable = call scr.getProperty("propTest.usernamefield", "editable");
call scr.setProperty("propTest.usernamefield", "visible", false);
var items = call scr.getItemList("propTest");
// items[0], items[1], ... contain item names like "usernamefield", "agefield", etc.

// TreeView icon example:
call scr.setTreeItemIcon("fileExplorer", "main.ebs", "icons/script-file-run.png");
call scr.setTreeItemIcons("fileExplorer", "src", "icons/folder.png",
                          "icons/folder-open.png", "icons/folder.png");

// TreeView styling example:
call scr.setTreeItemBold("fileExplorer", "src", true);
call scr.setTreeItemColor("fileExplorer", "main.ebs", "#0066cc");
```

**Note:** The canonical prefix for screen functions is `scr.` (e.g., `scr.getProperty`, `scr.setStatus`). This is the recommended prefix for all new code.

### Debug Functions
```javascript
// Debug mode
call debug.on();
call debug.off();

// Trace mode
call debug.traceon();
call debug.traceoff();

// Echo mode
call echo.on();
call echo.off();

// Debug logging
call debug.file(path);
call debug.newfile(path);
call debug.log(message);

// Assertions
call debug.assert(condition, message);
call debug.assertequals(expected, actual, message);

// Introspection
call debug.vars();         // Show variables
call debug.stack();        // Show call stack
call debug.memusage(unit); // "B", "KB", "MB"
```

### AI Functions

**Prerequisites:** Configure your OpenAI API key via Edit > AI Chat Model Setup before using AI functions.

#### ai.complete - Synchronous Text Generation
```javascript
// Basic usage with just user prompt
var answer = call ai.complete(null, "What is the capital of France?");
print answer;

// Full parameters: system prompt, user prompt, maxTokens, temperature
var response = call ai.complete(
    "You are a helpful coding assistant",  // system prompt (optional)
    "Write a function to calculate factorial",  // user prompt (required)
    500,  // maxTokens (optional, limits response length)
    0.7   // temperature (optional, 0.0-1.0, higher = more creative)
);
print response;
```

#### ai.completeAsync - Asynchronous Text Generation (Non-blocking)
```javascript
// Call AI without blocking the UI - result delivered via callback
call ai.completeAsync(
    "You are a regex expert",  // system prompt
    "Generate a pattern for email validation",  // user prompt
    200,  // maxTokens
    0.3,  // temperature
    "onAiComplete"  // callback function name
);

// Callback function receives JSON with: success (bool), result (string), error (string)
onAiComplete(response: json) {
    if call json.getBool(response, "success", false) then {
        var result: string = call json.getString(response, "result", "");
        print "AI Response: " + result;
    } else {
        var error: string = call json.getString(response, "error", "Unknown error");
        print "AI Error: " + error;
    }
}
```

#### ai.summarize - Text Summarization
```javascript
// Summarize a long text
var longText: string = "The quick brown fox jumps over the lazy dog. " +
    "This is a sample text that demonstrates the summarization capability. " +
    "It contains multiple sentences that can be condensed into a shorter form.";

var summary = call ai.summarize(longText, 50);  // maxTokens limits summary length
print "Summary: " + summary;

// Summarize file contents
var article = call file.readTextFile("article.txt");
var briefSummary = call ai.summarize(article);  // maxTokens is optional
print briefSummary;
```

#### ai.embed - Text Embeddings
```javascript
// Generate semantic embeddings for text
var embedding = call ai.embed("machine learning algorithms");
print "Embedding dimensions: " + call array.length(embedding);

// Embeddings are useful for semantic similarity comparisons
var embed1 = call ai.embed("happy");
var embed2 = call ai.embed("joyful");
// Similar meanings produce similar embedding vectors
```

#### ai.classify - Text Classification
```javascript
// Classify text into predefined categories
var categories = ["positive", "negative", "neutral"];
var sentiment = call ai.classify("This product is amazing!", categories);
print "Sentiment: " + sentiment;  // Output: positive

// Classify support tickets
var ticketTypes = ["billing", "technical", "general inquiry"];
var ticketText: string = "My payment failed and I was charged twice";
var ticketCategory = call ai.classify(ticketText, ticketTypes);
print "Ticket type: " + ticketCategory;  // Output: billing
```

### Date/Time Functions

EBS provides comprehensive date and time functions using the modern Java `java.time` API internally. Date values are stored as `java.time.LocalDateTime` for datetime values and `java.time.LocalDate` for date-only values, with easy conversions for display, calculations, and SQL operations.

**Key Types:**
- **LocalDate**: A date without time (e.g., "2024-01-15")
- **LocalDateTime**: A date with time (e.g., "2024-01-15T10:30:00")

**Creating Dates:**
```javascript
// Get current date/time
var now = call date.now();           // Current LocalDateTime
var today = call date.today();       // Current LocalDate (no time)

// Parse dates from strings
var parsed = call date.parse("2024-01-15", "yyyy-MM-dd");
var parsedDateTime = call date.parseDateTime("2024-01-15 10:30:00", "yyyy-MM-dd HH:mm:ss");
```

**Formatting for Display:**
```javascript
var formatted = call date.format(now, "yyyy-MM-dd HH:mm:ss");
var usFormat = call date.format(now, "MM/dd/yyyy");
var euFormat = call date.format(now, "dd.MM.yyyy");
```

**Date Calculations:**
```javascript
var nextWeek = call date.addDays(now, 7);
var yesterday = call date.addDays(now, -1);
var twoHoursAgo = call date.addHours(now, -2);
var inThirtyMinutes = call date.addMinutes(now, 30);
var inSixtySeconds = call date.addSeconds(now, 60);
var daysBetween = call date.daysBetween(date1, date2);
```

**Getting Date Components:**
```javascript
var year = call date.getYear(now);
var month = call date.getMonth(now);    // 1-12
var day = call date.getDay(now);        // 1-31
var hour = call date.getHour(now);      // 0-23
var minute = call date.getMinute(now);  // 0-59
var second = call date.getSecond(now);  // 0-59
```

**Epoch Milliseconds:**
```javascript
// Convert to/from epoch milliseconds (useful for calculations)
var epochMs = call date.toEpochMs(now);
var fromEpoch = call date.fromEpochMs(1699876543210);
```

**SQL Timestamp Conversion:**
```javascript
// Convert to SQL Timestamp (for database operations)
var sqlTimestamp = call date.toSqlTimestamp(now);
```

**Format Pattern Examples:**
| Pattern | Example Output |
|---------|---------------|
| `yyyy-MM-dd` | 2024-01-15 |
| `MM/dd/yyyy` | 01/15/2024 |
| `dd.MM.yyyy` | 15.01.2024 |
| `yyyy-MM-dd HH:mm:ss` | 2024-01-15 10:30:45 |
| `HH:mm:ss` | 10:30:45 |
| `EEEE, MMMM d, yyyy` | Monday, January 15, 2024 |

For implementation details and Java type recommendations, see [JAVA_DATE_TYPE_RECOMMENDATION.md](JAVA_DATE_TYPE_RECOMMENDATION.md).

### Random Number Generation Functions

EBS provides built-in random number generation with support for seeding (for reproducible sequences) and bounded ranges. All random functions use a shared `java.util.Random` instance.

**Generating Random Long Integers:**
```javascript
// Unbounded random long (any 64-bit integer value)
var anyLong = call random.nextLong();

// Random long in range [0, max) - exclusive upper bound
var percentage = call random.nextLong(100);        // 0 to 99

// Random long in range [min, max) - inclusive min, exclusive max
var diceRoll = call random.nextLong(1, 7);         // 1 to 6
var score = call random.nextLong(50, 100);         // 50 to 99
```

**Generating Random Double Values:**
```javascript
// Random double in range [0.0, 1.0) - standard probability
var probability = call random.nextDouble();

// Random double in range [0.0, max)
var percentage = call random.nextDouble(100.0);    // 0.0 to 99.999...

// Random double in range [min, max)
var temperature = call random.nextDouble(20.0, 30.0);  // 20.0 to 29.999...
var weight = call random.nextDouble(50.0, 100.0);      // 50.0 to 99.999...
```

**Reproducible Random Sequences (Seeding):**
```javascript
// Set seed for reproducible results
call random.setSeed(12345);
var val1 = call random.nextLong(100);      // Always produces 51 with seed 12345
var val2 = call random.nextDouble();

// Reset to same seed to repeat sequence
call random.setSeed(12345);
var val3 = call random.nextLong(100);      // Also produces 51
var val4 = call random.nextDouble();       // Same as val2

// Different seed produces different sequence
call random.setSeed(54321);
var val5 = call random.nextLong(100);      // Different value
```

**Practical Examples:**

*Dice Rolling:*
```javascript
// Roll a 6-sided die (1-6)
var roll = call random.nextLong(1, 7);

// Roll two dice
var die1 = call random.nextLong(1, 7);
var die2 = call random.nextLong(1, 7);
var total = die1 + die2;
```

*Random Selection from Array:*
```javascript
var items = ["apple", "banana", "cherry", "date"];
var randomIndex = call random.nextLong(items.length);
var randomItem = items[randomIndex];
print "Selected: " + randomItem;
```

*Monte Carlo Simulation:*
```javascript
// Estimate PI using random points
var inside = 0;
var total = 10000;
var i = 0;

while i < total {
    var x = call random.nextDouble(-1.0, 1.0);
    var y = call random.nextDouble(-1.0, 1.0);
    if (x * x + y * y) <= 1.0 then {
        inside = inside + 1;
    }
    i = i + 1;
}

var piEstimate = 4.0 * inside / total;
print "PI estimate: " + piEstimate;
```

*Shuffling (Fisher-Yates):*
```javascript
var deck = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
var i = deck.length - 1;

while i > 0 {
    var j = call random.nextLong(i + 1);
    var temp = deck[i];
    deck[i] = deck[j];
    deck[j] = temp;
    i = i - 1;
}
print "Shuffled deck: " + deck;
```

**Notes:**
- All random functions use a shared `Random` instance, so setting a seed affects all subsequent calls
- Upper bounds are exclusive: `random.nextLong(10)` returns 0-9, not 0-10
- For long ranges, uses rejection sampling to avoid modulo bias
- Seeds are useful for testing, debugging, and procedural generation

### Class Tree Functions
```javascript
// Generate class hierarchy
var tree = call classtree.generate(baseDirectory, packages);

// Scan packages
var scan = call classtree.scan(directory);
```

### Plugin Functions (External Java Functions)

The plugin system allows loading and calling external Java classes that implement the `EbsFunction` interface. This enables extending the interpreter with custom functionality without modifying the core codebase.

Custom functions are called using the `#custom.functionName(...)` syntax, which is consistent with other EBS builtins.

See [PLUGIN_SYSTEM.md](PLUGIN_SYSTEM.md) for complete documentation and examples.

#### plugin.load - Load a Java Plugin
```javascript
// Load a Java class as a plugin (must implement EbsFunction interface)
#plugin.load("com.example.MyFunction", "myFunc");

// Load with configuration
#plugin.load("com.example.MyFunction", "myFunc", {"option": "value"});
```

#### Calling Custom Functions with #custom.alias(...)
```javascript
// Call the loaded plugin using #custom.alias syntax
var result = #custom.myFunc("arg1", 42, true);
print result;
```

#### plugin.isLoaded - Check if Plugin is Loaded
```javascript
if #plugin.isLoaded("myFunc") then {
    print "Plugin is loaded";
}
```

#### plugin.unload - Unload a Plugin
```javascript
// Unload the plugin and cleanup resources
#plugin.unload("myFunc");
```

#### plugin.list - List Loaded Plugins
```javascript
var plugins = #plugin.list();
foreach p in plugins {
    print "Loaded: " + p;
}
```

#### plugin.info - Get Plugin Information
```javascript
var info = #plugin.info("myFunc");
print "Name: " + #json.getString(info, "name", "");
print "Description: " + #json.getString(info, "description", "");
```

#### Example: Using the Built-in Example Plugin
```javascript
// Load the built-in example plugin
#plugin.load("com.eb.script.interpreter.plugin.ExampleEbsFunction", "echo");

// Call it using #custom.echo(...)
var result = #custom.echo("Hello", "World");
print result;  // Output: [Echo] Hello World

// Unload when done
#plugin.unload("echo");
```

---

## Comments

### Single-Line Comments
```javascript
// This is a single-line comment
var x = 10;  // Comment at end of line

// Multiple single-line comments
// can be used for
// multi-line documentation
```

### Documentation Style
```javascript
// Function: calculateTotal
// Description: Calculates the total price including tax
// Parameters:
//   - price: base price (double)
//   - taxRate: tax rate as percentage (double)
// Returns: total price (double)
calculateTotal(price: double, taxRate: double) return double {
    return price * (1 + taxRate / 100);
}
```

---

## Console Commands

When using the interactive console, these commands are available:

### File Operations
```
/open <filename>       Load and open a script file
/save <filename>       Save current script to file
```

### Console Control
```
/clear                 Clear console output
/echo on               Enable statement echo mode
/echo off              Disable statement echo mode
/help                  Display help information
```

### AI Integration
```
/ai setup              Configure AI integration settings
```

### Security
```
/safe-dirs             Configure trusted directories for file operations
```

### Execution
```
Ctrl+Enter             Execute the script in the editor
```

---

## Language Features

### Case Sensitivity
- **Keywords**: Case-insensitive, normalized to lowercase by lexer (`var`, `VAR`, and `Var` all work)
- **Identifiers**: Case-insensitive, normalized to lowercase (variable, function, and screen names)
  - `myVariable`, `MyVariable`, and `MYVARIABLE` all refer to the same variable
  - Screen names: `clientScreen`, `ClientScreen`, and `CLIENTSCREEN` all refer to the same screen
  - Variable names: `userName`, `UserName`, and `USERNAME` all refer to the same variable
- **Screen JSON**: Keys are case-insensitive, normalized to lowercase
- **String values**: Case-sensitive (string content preserves original case)

### Reserved Keywords in Builtin Functions
Some words like `open` and `connect` are reserved keywords used for database operations (cursor opening, connection statements). However, these words can also be used as method names in builtin functions when prefixed with their module name.

**Examples of reserved keywords used as builtin method names:**
```javascript
// ftp.open - Opens an FTP connection
var ftpHandle = call ftp.open("ftp.example.com", 21, "user", "pass");

// mail.open - Opens a mail server connection
var mailHandle = call mail.open("imap.gmail.com", 993, "user", "pass", "imaps");

// file.open - Opens a file for reading/writing
var fileHandle = call file.open("data.txt", "r");
```

**Reserved keywords used alone retain their original meaning:**
```javascript
// 'open' as a cursor keyword
cursor myCursor = select * from users;
open myCursor();

// 'connect' as a database connection keyword
connect db = "jdbc:oracle:thin:@localhost:1521:xe";
```

This allows the language to support both database-style syntax (`open cursor`, `connect db`) and modern builtin functions (`ftp.open`, `mail.open`) without conflicts.

### Connection URL Formats

Mail and FTP connections can be configured using URL strings stored in variables.

#### Mail URL Format
```
******host:port?protocol=imaps
```

**Examples:**
- `******imap.gmail.com:993?protocol=imaps`
- `******outlook.office365.com:993?protocol=imaps`

#### FTP URL Format
```
******host:port     (standard FTP)
******host:port    (secure FTPS)
```

**Examples:**
- `******ftp.example.com:21`
- `******ftps.example.com:990`

**Note:** URL-encode special characters in usernames and passwords:
- Use `%40` for `@`
- Use `%3A` for `:`
- Use `%2F` for `/`

### Using Config Variables in Scripts

Variables defined in the Config dialogs (Config > Mail Server Config or Config > FTP Server Config) are automatically available as global variables in your scripts. The variable name you specify in the config becomes a script variable containing the connection URL.

**Note:** Config variable names are **case-insensitive**. If you define `MyFtp` in the config, you can access it as `myftp`, `MyFtp`, or `MYFTP` in your scripts.

**Example:**
If you create a mail config with variable name `MyEmail` and URL `******imap.gmail.com:993?protocol=imaps`, you can use it in a script:

```javascript
// Config variables are case-insensitive
print myemail;  // Outputs: ******imap.gmail.com:993?protocol=imaps
print MyEmail;  // Same result

// FTP config variable example
print myftp;    // Outputs: ******ftp.example.com:21
```

### Type Coercion
```javascript
var x = "10" + 5;      // String concatenation: "105"
var y = 10 + 5;        // Numeric addition: 15

// Explicit conversion via variable declaration
var num: int = "42";   // Automatic type conversion
var str = call string.tostring(42);
```

### Property Access
```javascript
// Array length
var count = myArray.length;

// JSON object navigation
var nested = call json.get(obj, "path.to.value");

// Screen variables
var value = myScreen.variableName;
```

### Error Handling
```javascript
// Assertions
call debug.assert(count > 0, "Count must be positive");
call debug.assertequals(expected, actual, "Values don't match");

// HTTP error checking
var response = call http.get(url);
call http.ensure2xx(response);  // Throws on non-2xx status
```

---

## Best Practices

### Naming Conventions
```javascript
// Variables: camelCase
var userName = "Alice";
var totalCount = 100;

// Functions: camelCase
processData(input: string) return string { }

// Constants: UPPER_SNAKE_CASE
var MAX_RETRIES = 3;
var DEFAULT_TIMEOUT = 30;

// Screens: camelCase
screen mainWindow = { };
```

### Code Organization
```javascript
// 1. Constants and configuration
var API_URL = "https://api.example.com";
var TIMEOUT = 30;

// 2. Function definitions
fetchData(endpoint: string) return json { }

// 3. Main program logic
var data = call fetchData("/users");
print data;
```

### Comments and Documentation
```javascript
// Clear, descriptive comments
// Explain WHY, not WHAT (code shows what)

// Good: Explains rationale
// Retry failed requests up to 3 times to handle transient network issues
var MAX_RETRIES = 3;

// Bad: States the obvious
// Set MAX_RETRIES to 3
var MAX_RETRIES = 3;
```

### Resource Management
```javascript
// Always close resources
var fileHandle = call file.open("data.txt", "r");
var content = call file.read(fileHandle, -1);
call file.close(fileHandle);

// Close database connections
connect db = "jdbc:...";
// ... use database ...
close connection db;

// Close cursors
cursor myCursor = select * from table;
open myCursor();
// ... use cursor ...
close myCursor;
```

---

## Examples

### Hello World
```javascript
var message: string = "Hello, World!";
print message;
```

### Fibonacci Sequence
```javascript
fibonacci(n: int) return int {
    if n <= 1 then {
        return n;
    }
    return call fibonacci(n - 1) + call fibonacci(n - 2);
}

var i = 0;
while i < 10 {
    print "fib(" + i + ") = " + call fibonacci(i);
    i = i + 1;
}
```

### Database Query and Processing
```javascript
connect db = "jdbc:oracle:thin:@localhost:1521:xe";

use db {
    cursor employees = select emp_id, name, salary from employees where salary > 50000;
    open employees();
    
    var totalSalary = 0;
    var count = 0;
    
    while call employees.hasNext() {
        var emp = call employees.next();
        print emp.name + ": $" + emp.salary;
        totalSalary = totalSalary + emp.salary;
        count = count + 1;
    }
    
    close employees;
    
    var average = totalSalary / count;
    print "Average salary: $" + average;
}

close connection db;
```

### REST API Integration
```javascript
// Fetch user data from API
var users = call http.getjson("https://api.example.com/users");

// Process each user
var i = 0;
while i < users.length {
    var user = call json.get(users, i);
    var name = call json.getstring(user, "name");
    var email = call json.getstring(user, "email");
    
    print name + " <" + email + ">";
    
    i = i + 1;
}
```

### Interactive Form with Validation
```javascript
screen userForm = {
    "title": "User Registration",
    "width": 600,
    "height": 500,
    "vars": [
        {
            "name": "username",
            "type": "string",
            "default": "",
            "display": {
                "type": "textfield",
                "labelText": "Username:",
                "mandatory": true,
                "maxLength": 20
            }
        },
        {
            "name": "email",
            "type": "string",
            "default": "",
            "display": {
                "type": "textfield",
                "labelText": "Email:",
                "mandatory": true
            }
        },
        {
            "name": "age",
            "type": "int",
            "default": 18,
            "display": {
                "type": "spinner",
                "labelText": "Age:",
                "min": 13,
                "max": 120
            }
        }
    ]
};

// Show form
show userForm;

// Wait for user input (in real application)
// Then validate and process
if userForm.age < 18 then {
    print "User must be 18 or older";
} else {
    print "Registration successful for " + userForm.username;
}
```

### File Processing
```javascript
// Read CSV file
var content = call file.readtextfile("data.csv");
var lines = call string.split(content, "\n");

var i = 0;
while i < lines.length {
    var line = lines[i];
    var fields = call string.split(line, ",");
    
    // Process each field
    var j = 0;
    while j < fields.length {
        print "Field " + j + ": " + fields[j];
        j = j + 1;
    }
    
    i = i + 1;
}
```

---

## Maintaining This Documentation

### When to Update This Document

This syntax reference must be kept synchronized with the EBS language implementation. Update this document whenever you make changes to:

#### Language Features
- **New Keywords**: Add any new keywords to the Keywords section and update relevant syntax sections
- **New Data Types**: Document in the Data Types section with examples
- **New Operators**: Add to the Operators section with precedence information
- **Control Flow Changes**: Update Control Flow section if syntax changes
- **Statement Types**: Document new statement types in appropriate sections

#### Built-in Functions
- **New Functions**: Add to the Built-in Functions section organized by category
- **Function Signature Changes**: Update parameter lists and return types
- **Deprecated Functions**: Mark as deprecated with alternatives
- **New Function Categories**: Create new subsections as needed

#### Screen/UI Features
- **New Control Types**: Add to the Available Control Types table with descriptions
- **New Display Properties**: Document in the Display Properties section
- **Layout Changes**: Update Area Definition section

#### Database Features
- **New Connection Types**: Document in Connection Management
- **SQL Syntax Changes**: Update SQL statements section
- **Cursor Operations**: Add new cursor methods/operations

### How to Update

1. **Make Language Changes First**: Implement and test your language changes in the codebase
2. **Update EBNF Grammar**: Modify `syntax_ebnf.txt` to reflect grammar changes
3. **Update This Document**: Add/modify sections to document the changes
4. **Add Examples**: Include practical examples demonstrating new features
5. **Update Version Info**: Update the Version Information section
6. **Test Examples**: Verify all code examples compile and run correctly
7. **Review Cross-References**: Check that all internal links and references are valid

### Documentation Standards

#### Code Examples
- Use consistent formatting and indentation
- Include comments explaining non-obvious behavior
- Test all examples before committing
- Use realistic variable and function names

#### Sections
- Keep sections focused and well-organized
- Use appropriate heading levels (##, ###, ####)
- Include tables for reference information
- Cross-reference related sections

#### Language
- Be clear and concise
- Use present tense for statements of fact
- Include "why" explanations where helpful
- Provide both simple and complex examples

### Validation Checklist

Before committing documentation changes, verify:

- [ ] All code examples use correct syntax according to `syntax_ebnf.txt`
- [ ] New features are documented in appropriate sections
- [ ] Examples have been tested and work correctly
- [ ] Internal links point to existing sections
- [ ] Grammar reference is updated if syntax changed
- [ ] Version information is current
- [ ] No contradictions with existing documentation
- [ ] Related sections are cross-referenced
- [ ] Built-in function signatures are accurate

### File Locations

Keep these files synchronized:
- **EBS_SCRIPT_SYNTAX.md** (this file) - User-facing syntax reference
- **syntax_ebnf.txt** - Formal EBNF grammar specification
- **README.md** - Language overview and quick start
- **ARCHITECTURE.md** - Implementation details
- **Test Scripts** - Practical examples in `ScriptInterpreter/scripts/`

### Questions or Issues?

If you're unsure how to document a feature:
1. Review existing documentation for similar features
2. Check `syntax_ebnf.txt` for the formal grammar
3. Look at test scripts for usage examples
4. Consult project maintainers if needed

---

## Grammar Reference (EBNF)

For the complete formal grammar specification, see:
[syntax_ebnf.txt](ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt)

---

## Additional Resources

- **README.md** - Project overview and quick start guide
- **ARCHITECTURE.md** - Detailed architecture documentation
- **syntax_ebnf.txt** - Formal EBNF grammar specification
- **Test Scripts** - Example scripts in `ScriptInterpreter/scripts/` directory

---

## Version Information

This documentation corresponds to the EBS Script Interpreter version 1.0-SNAPSHOT.

For the latest updates and information, visit the project repository.
