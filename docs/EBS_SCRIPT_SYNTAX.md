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

```javascript
// Declare a map variable
var myMap: map = {"key1": "value1", "key2": 123, "key3": true};

// Cast JSON object to map
var jsonData: json = {"name": "Alice", "age": 30, "city": "New York"};
var personMap = map(jsonData);

// Access and modify map values using json functions
var name = call json.get(personMap, "name");          // "Alice"
var age = call json.getint(personMap, "age");         // 30
call json.set(personMap, "city", "Los Angeles");      // Modify value
call json.set(personMap, "country", "USA");           // Add new key

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

### Null Values
```javascript
var empty = null;
if empty == null then {
    print "Value is null";
}
```

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
a and b   // Logical AND
a or b    // Logical OR
!a        // Logical NOT

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

**Example:**
```javascript
// main.ebs
import "util/math.ebs";
import "util/string.ebs";

var result: int = call add(5, 3);
var text: string = call toUpper("hello");
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
| `int[10]` | Typed | ArrayFixed | Fixed-size integer array (traditional) |
| `array.int[10]` | Typed | ArrayFixed | Fixed-size integer array (enhanced) |
| `array.byte[10]` | Typed | ArrayFixedByte | Byte array with optimized storage |
| `string[*]` | Typed | ArrayDynamic | Dynamic string array (traditional) |
| `array.string[*]` | Typed | ArrayDynamic | Dynamic string array (enhanced) |
| `array[10]` | Generic | ArrayFixed | Fixed-size generic array |
| `array.any[10]` | Generic | ArrayFixed | Fixed-size generic array (explicit) |
| `array[*]` | Generic | ArrayDynamic | Dynamic generic array |
| `json` | JSON | Java List/Map | JSON arrays and objects |

### Available array.type Variants

- `array` or `array.any` - Generic array (any type)
- `array.string` - String array
- `array.byte` - Byte array (uses ArrayFixedByte for fixed size)
- `array.int` or `array.integer` - Integer array
- `array.long` - Long integer array
- `array.float` - Float array
- `array.double` or `array.number` - Double/number array
- `array.bool` or `array.boolean` - Boolean array
- `array.date` - Date array

**When to use each syntax:**
- Use `int[10]` for concise traditional syntax
- Use `array.int[10]` for explicit, consistent syntax across all types
- Use `array[10]` or `array.any[10]` when you need mixed types
- All three syntaxes work identically with `ArrayFixed` and `ArrayDynamic` implementations

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
var jsonString = '{"name": "Bob", "age": 25}';
var obj = call json.jsonfromstring(jsonString);

var person: json = {"name": "Alice", "age": 30};
var str = call string.tostring(person);  // Convert to string
```

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
    "alignment": "left",                    // Content alignment
    "itemFontSize": "14px",                // Control font size
    "itemColor": "#000000",                // Control text color
    "itemBold": true,                      // Bold text
    "itemItalic": false,                   // Italic text
    "labelColor": "#0000ff",               // Label text color
    "labelBold": false,                    // Label bold
    "labelItalic": false,                  // Label italic
    "labelFontSize": "12px",               // Label font size
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

// Encoding
var encoded = call array.base64encode(byteArray);
var decoded = call array.base64decode(encodedString);
```

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

// Choice/dropdown options
var options = call scr.getItemChoiceOptions("screenName", "itemName");
call scr.setItemChoiceOptions("screenName", "itemName", optionsMap);

// Examples:
var editable = call scr.getProperty("propTest.usernamefield", "editable");
call scr.setProperty("propTest.usernamefield", "visible", false);
var items = call scr.getItemList("propTest");
// items[0], items[1], ... contain item names like "usernamefield", "agefield", etc.
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

### Class Tree Functions
```javascript
// Generate class hierarchy
var tree = call classtree.generate(baseDirectory, packages);

// Scan packages
var scan = call classtree.scan(directory);
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
