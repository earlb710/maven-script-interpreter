# EBS Script Syntax Reference

## Table of Contents
1. [Overview](#overview)
2. [Basic Syntax](#basic-syntax)
3. [Data Types](#data-types)
4. [Variables](#variables)
5. [Operators](#operators)
6. [Control Flow](#control-flow)
7. [Code Organization](#code-organization)
8. [Functions](#functions)
9. [Arrays](#arrays)
10. [JSON](#json)
11. [Database Operations](#database-operations)
12. [Screen/UI Windows](#screenui-windows)
13. [Built-in Functions](#built-in-functions)
14. [Comments](#comments)
15. [Console Commands](#console-commands)

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
| `array` | Generic array | `array[10]`, `array[*]` |

### Type Inference
```javascript
var count = 42;           // Inferred as int
var name = "Alice";       // Inferred as string
var ratio = 3.14;         // Inferred as double
var flag = true;          // Inferred as bool
```

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
| `textarea` | Multi-line text input | `promptHelp`, `maxLength` |
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
- Destroy the screen window
- Clean up all screen resources (variables, threads, etc.)
- Invoke any registered close callback
- If no screen name is provided, determines the screen from the execution thread context
- The no-name form must be called from within a screen context (e.g., onClick handlers)
- If called from console/main thread without a name, an error is thrown

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
- **Case-Insensitive**: JSON keys are case-insensitive
- **Not Auto-Shown**: Screens must be explicitly shown with `show` command
- **Multiple Screens**: Can create and manage multiple independent screens
- **Data Binding**: Variable changes immediately reflected in UI

---

## Built-in Functions

### String Functions
```javascript
// Conversion
call string.tostring(value);
call string.toupper(text);
call string.tolower(text);

// Manipulation
call string.trim(text);
call string.replace(text, old, new);
call string.split(text, delimiter);
call string.join(array, separator);

// Queries
call string.contains(text, substring);
call string.startswith(text, prefix);
call string.endswith(text, suffix);
call string.equals(text1, text2);
call string.equalsignorecase(text1, text2);
call string.isempty(text);
call string.isblank(text);
```

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

### Array Functions
```javascript
// Array manipulation
call array.expand(array, newSize);
call array.sort(array);
call array.fill(array, value);

// Encoding
var encoded = call array.base64encode(byteArray);
var decoded = call array.base64decode(encodedString);
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
```

### Screen Functions
```javascript
// Get property value from a screen item
var value = call screen.getProperty("screenName.itemName", "propertyName");

// Set property value on a screen item
call screen.setProperty("screenName.itemName", "propertyName", value);

// Get list of all item names in a screen
var itemList = call screen.getItemList("screenName");
// Alias: screen.getScreenItemList also available
var itemList = call screen.getScreenItemList("screenName");
// Returns an ArrayDynamic of strings containing all item names

// Examples:
var editable = call screen.getProperty("propTest.usernamefield", "editable");
call screen.setProperty("propTest.usernamefield", "visible", false);
var items = call screen.getItemList("propTest");
// items[0], items[1], ... contain item names like "usernamefield", "agefield", etc.
```

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
```javascript
// Text generation
var completion = call ai.complete(prompt, options);

// Summarization
var summary = call ai.summarize(text, options);

// Text embedding
var embedding = call ai.embed(text);

// Classification
var category = call ai.classify(text, categories);
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
- **Keywords**: Case-sensitive (`var`, `if`, `while`, etc.)
- **Identifiers**: Case-sensitive (variable and function names)
- **Screen JSON**: Keys are case-insensitive, normalized to lowercase
- **String values**: Case-sensitive

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
