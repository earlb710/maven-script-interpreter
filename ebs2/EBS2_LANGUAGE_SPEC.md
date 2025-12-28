# EBS2 Language Specification

**Version:** 2.0.0-SPEC  
**Date:** December 2025  
**Status:** Specification Phase

## Table of Contents

1. [Introduction](#introduction)
2. [Lexical Structure](#lexical-structure)
3. [Program Structure](#program-structure)
4. [Data Types](#data-types)
5. [Variables](#variables)
6. [Operators](#operators)
7. [Control Flow](#control-flow)
8. [Functions](#functions)
9. [Screens and UI](#screens-and-ui)
10. [Error Handling](#error-handling)
11. [Built-in Functions](#built-in-functions)
12. [Modules and Imports](#modules-and-imports)
13. [Runtime Model](#runtime-model)

## Introduction

### Design Philosophy

EBS2 is designed with three core principles:
1. **Clarity over brevity**: Code should read like natural English
2. **Simplicity with depth**: Simple things stay simple; complex things become possible
3. **Universal access**: Runs identically in browsers (HTML5) and desktop (Java)

### Language Efficiency: Long and Short Forms

**EBS2 supports dual syntax** - both natural language (long form) and symbolic (short form) - allowing users to choose based on their needs:

**Long Form (Natural Language):**
- Best for beginners and children
- Self-documenting and readable
- Example: `if age is greater than 12 then`

**Short Form (Symbolic):**
- Best for experienced developers
- Compact and efficient
- Example: `if age > 12 then`

**Both forms are equivalent and can be mixed in the same program.** Users can start with long form and transition to short form as they gain experience.

### Language Goals

- **For Children**: Natural syntax that reads like English stories
- **For Learners**: Progressive complexity with clear upgrade paths
- **For Developers**: Powerful features when needed, without compromising simplicity
- **For Deployment**: Write once, run anywhere (browser or desktop)

## Lexical Structure

### Comments

**Single-line comments only:**

```javascript
// Single line comment
var x = 10  // Comment after code

// Multi-line explanations use multiple single lines:
// This is a longer explanation
// that spans multiple lines
// to describe complex logic
```

**Note:** Block comments (`/* */`) are NOT supported. Use multiple `//` lines instead for clarity and simplicity.

### Keywords

#### Basic Keywords (Beginner Level)
```
program     end         var         variable    as          is
if          then        else        repeat      times
for         each        in          to          call
with        and         return      print
log         hide        ask         screen      button
label       textbox     array       text        number
flag        true        false       const       constant    indicator
record      type        function    procedure
```

**Note:** All keywords are case-insensitive (`IF`, `if`, `If` all work the same).

#### Advanced Keywords
```
function    return      try         catch       throw
import      from        export      record      map
json        while       until       break       continue
switch      case        default     let
async       await       class       extends     new
```

### Identifiers

**User-defined names (variables, functions, procedures, types, record fields) are case-insensitive:**

- Start with letter or underscore
- Contain letters, digits, underscores
- Case-insensitive: `myVar`, `MYVAR`, `MyVar` are the same identifier
- Must be unique within the same scope (case-insensitive check)
- String literals remain case-sensitive: `"Hello"` ≠ `"HELLO"`

**Examples:**
```javascript
var userName = "Alice"
print USERNAME              // Same as userName (case-insensitive)
print UserName              // Same as userName (case-insensitive)

function calculateTotal() as number
    return 100
end function

var x = CALCULATETOTAL()    // Calls calculateTotal (case-insensitive)
```
- Case-sensitive for user-defined names
- Cannot be reserved keywords

**Valid:** `myVariable`, `user_name`, `count2`, `_private`

### Keyword Case Sensitivity

**All language keywords are case-insensitive** for ease of learning:

```javascript
// All of these are valid and equivalent
IF age > 12 THEN print "Teen"
if age > 12 then print "Teen"
If Age > 12 Then Print "Teen"
iF aGe > 12 tHeN pRiNt "Teen"
```

**Examples:**
- `if`, `IF`, `If` → all the same keyword
- `repeat`, `REPEAT`, `Repeat` → all the same keyword
- `function`, `FUNCTION`, `Function` → all the same keyword
- `end`, `END`, `End` → all the same keyword

**User-defined names remain case-sensitive:**
```javascript
var userName = "Alice"
var USERNAME = "BOB"      // Different variable
print userName            // "Alice"
print USERNAME            // "BOB"
```  
**Invalid:** `2count`, `my-var`, `class` (keyword)

### Literals

#### Number Literals
```javascript
42          -- Integer
3.14        -- Decimal
1_000_000   -- Underscore separators for readability
0xFF        -- Hexadecimal (advanced)
0b1010      -- Binary (advanced)
```

#### Text Literals
```javascript
"Hello World"           -- Double quotes
'Single quotes work'    -- Single quotes (same as double)
"Line 1\nLine 2"       -- Escape sequences
"She said \"Hi\""      -- Escaped quotes
```

#### Boolean Literals
```javascript
yes         // Boolean true (beginner-friendly)
no          // Boolean false
true        // Also supported
false       // Also supported
```

#### List Literals
```javascript
list 1, 2, 3, 4, 5                    // Simple list
list of numbers 1, 2, 3               // Typed list
list "apple", "banana", "cherry"      // Text list
```

#### Record Literals
```javascript
record {name: "Alice", age: 10}
```

### Operators

#### Arithmetic (Simple)
```javascript
+    -- Addition
-    -- Subtraction
*    -- Multiplication
/    -- Division
```

#### Arithmetic (Advanced)
```javascript
mod  -- Modulo (remainder)
^    -- Power
```

#### Comparison (Natural Language)
```javascript
is equal to              -- ==
is not equal to          -- !=
is greater than          -- >
is less than             -- <
is greater than or equal to  -- >=
is less than or equal to     -- <=
```

#### Comparison (Symbolic - Advanced)
```javascript
=    -- Equal
<>   -- Not equal (also !=)
>    -- Greater than
<    -- Less than
>=   -- Greater or equal
<=   -- Less or equal
```

#### Logical (Natural Language)
```javascript
and          -- Logical AND
or           -- Logical OR
not          -- Logical NOT
```

#### Logical (Symbolic - Advanced)
```javascript
&&   -- AND
||   -- OR
!    -- NOT
```

## Program Structure

### Minimal Program

```javascript
program HelloWorld

main
    print "Hello World"
end
```

### Complete Program Structure

```javascript
program ProgramName

// Optional: Program metadata
settings
    title "My Application"
    version "1.0.0"
    author "Student Name"
    description "What this program does"
end

// Optional: Global variables
variables
    var userName as text = "Guest"
    var score as number = 0
    variable isPlaying as yes/no = no    // 'variable' is same as 'var'
    const MAX_SCORE as number = 1000     // Constants cannot change
    constant APP_VERSION as text = "1.0" // 'constant' is same as 'const'
end

// Optional: Function definitions
functions
    to greet person
        print "Hello " + person
    end function
    
    to calculate score
        return score * 10
    end function
end

// Optional: Screen definitions
screens
    screen MainWindow
        title "Main Screen"
        -- screen content
    end screen
end

// Required: Main entry point
main
    -- Program starts here
    print screen MainWindow
end
```

### Section Rules

1. Sections must appear in this order (if present):
   - `settings`
   - `variables`
   - `functions`
   - `screens`
   - `main` (required)

2. Only `main` section is required

3. Empty sections can be omitted

4. Each section ends with `end` keyword

### Block Ending Conventions

**Single-line vs Multi-line blocks:**

EBS2 supports both compact single-line and explicit multi-line block syntax:

```javascript
// SINGLE-LINE: No end keyword needed
if x < 5 then print "Small"
repeat 3 times print "Hello"
for i from 1 to 5 print i

// MULTI-LINE: Explicit end keyword required
if x < 5 then
    print "Small"
    log "Value checked"
end if

repeat 3 times
    print "Hello"
    print "World"
end repeat

// MULTIPLE COMMANDS ON ONE LINE: Use semicolon separator
if x < 5 then print "Small"; log "Checked"; x = x + 1
```

**When to use each:**

- **Single-line**: Simple, one-command blocks - more compact
- **Multi-line**: Multiple commands or complex logic - more readable
- **Semicolon**: Multiple commands on same line - use sparingly

**Control structures with explicit multi-line endings:**

```javascript
// Explicit endings for control structures
if condition then
    // code
end if

repeat 10 times
    // code
end repeat

for each item in list
    // code
end for

while condition
    // code
end while

// Explicit endings for functions
to myFunction param
    // code
end function

function myFunction(param) {
    // code
end function

// Explicit endings for screens
screen MyScreen
    // content
end screen

// Program sections use simple 'end'
main
    // code
end
```

**Benefits:**
- Easier to match opening and closing blocks
- Reduces debugging time
- Makes code more readable
- Helps catch mismatched blocks early

### Curly Brace Block Syntax

**Alternative syntax using curly braces `{}`** for developers familiar with C-style languages:

EBS2 supports curly braces `{}` as an alternative to the explicit `end` keywords for defining blocks. **Blocks declared with curly braces create their own lexical scope** for variables.

#### Syntax Forms Comparison

| Feature | End Keyword Form | Curly Brace Form |
|---------|------------------|------------------|
| **If Statement** | `if x > 5 then ... end if` | `if x > 5 { ... }` |
| **Repeat Loop** | `repeat 10 times ... end repeat` | `repeat 10 times { ... }` |
| **For Loop** | `for each x in list ... end for` | `for each x in list { ... }` |
| **While Loop** | `while condition ... end while` | `while condition { ... }` |
| **Function** | `to func ... end function` | `function func() { ... }` |
| **Try-Catch** | `try ... catch ... end try` | `try { ... } catch { ... }` |

#### If Statements with Curly Braces

```javascript
// Simple if
if age > 12 {
    print "Teenager"
    log "Age verified"
}

// If-else
if age > 12 {
    print "Teenager"
} else {
    print "Child"
}

// If-else if-else
if score >= 90 {
    print "Grade A"
} else if score >= 80 {
    print "Grade B"
} else {
    print "Grade C or lower"
}
```

#### Loops with Curly Braces

```javascript
// Repeat times
repeat 10 times {
    print "Hello"
}

// Repeat with counter
repeat 5 times with i {
    print "Count: " + i
}

// For each
for each item in items {
    print item
}

// For range
for i from 1 to 10 {
    print i
}

// While loop
while x < 100 {
    x = x + 1
}
```

#### Functions with Curly Braces

```javascript
// Function definition
function greet(name) {
    print "Hello " + name
    return "Greeting sent"
}

// Function call
var result = greet("Alice")
```

#### Try-Catch with Curly Braces

```javascript
try {
    var data = read file "data.txt"
    print data
} catch {
    print "Error reading file"
}
```

#### Variable Scoping with Curly Braces

**Important:** Curly brace blocks create their own lexical scope. Variables declared inside `{}` are local to that block.

```javascript
var x = 10               // Outer scope

if x > 5 {
    var y = 20          // Block scope - only exists inside this block
    print x             // Can access outer x: prints 10
    print y             // Can access y: prints 20
}

print x                 // Still accessible: prints 10
print y                 // ERROR: y doesn't exist outside the block
```

**Scope rules:**
- Variables declared inside `{}` blocks are local to that block
- Inner blocks can access variables from outer blocks
- Outer blocks cannot access variables from inner blocks
- Variables declared with `end if/repeat/for/while/function` forms also create block scope

```javascript
// Nested scopes with curly braces
var outer = 1

if outer > 0 {
    var middle = 2
    
    if middle > 1 {
        var inner = 3
        print outer   // OK: 1
        print middle  // OK: 2
        print inner   // OK: 3
    }
    
    print outer       // OK: 1
    print middle      // OK: 2
    print inner       // ERROR: inner not in scope
}

print outer           // OK: 1
print middle          // ERROR: middle not in scope
```

#### Mixing Syntax Styles

You can mix both styles in the same program:

```javascript
// Using end keywords
if age > 12 then
    print "Teen"
end if

// Using curly braces
if age < 5 {
    print "Child"
}

// Single-line (no block needed)
if age == 12 then print "Exactly 12"
```

**Equivalence:**
When using `end if`, `end repeat`, `end for`, etc. in multi-line form, the content behaves **as if it had curly braces** - it creates a block scope:

```javascript
// These are equivalent in terms of scoping:

// Form 1: end if
if x > 5 then
    var temp = 10
end if

// Form 2: curly braces
if x > 5 {
    var temp = 10
}

// In both cases, temp is not accessible here
```

#### Style Recommendations

**Use `end` keywords when:**
- Writing for beginners or children
- Prioritizing readability over brevity
- Creating educational content

**Use curly braces when:**
- Writing for experienced developers
- Prefer C-style syntax (Java, JavaScript, C++, etc.)
- Need explicit visual grouping
- Working in teams familiar with brace syntax

**Use single-line when:**
- Block contains only one simple statement
- Prefer compact code
- Writing simple conditionals or loops

## Data Types

### Basic Types (Beginner)

#### number
```javascript
var count as number = 42
var price as number = 19.99
var big as number = 1000000

// Automatic type selection (int, float, double)
// Children don't need to understand the difference
```

#### number with range constraint
```javascript
// Integer-backed range (whole numbers only)
var percentage as number 0..100 = 50           // Backed by integer
var age as number 0..120 = 25                  // Backed by integer
var score as number 0..1000 = 875              // Backed by integer

// Double-backed range (decimal numbers allowed)
var temperature as number -273.15..1000.0 = 22.5   // Backed by double
var ratio as number -1.0..1.0 = 0.75               // Backed by double
var probability as number 0.0..1.0 = 0.85          // Backed by double

// Compiler automatically selects backing type based on range notation:
// - If range uses only integers (e.g., 0..100), backed by integer
// - If range includes decimals (e.g., -1.0..1.0), backed by double

// Values must be within the specified range
percentage = 75        // OK - within range 0..100
percentage = 150       // ERROR: Outside range 0..100

ratio = 0.5            // OK - within range -1.0..1.0
ratio = 1.5            // ERROR: Outside range -1.0..1.0
```

**Benefits:**
- ✅ **Type safety** - Values must be within specified range
- ✅ **Automatic backing** - Compiler selects int or double based on range notation
- ✅ **Self-documenting** - Range constraints visible in declaration
- ✅ **Performance** - Integer-backed ranges use less memory
- ✅ **Validation** - Runtime checks enforce range constraints

#### text
```javascript
var name as text = "Alice"
var message as text = "Hello World"
var empty as text = ""
```

#### flag
```javascript
var isReady as flag = true
var gameOver as flag = false

// Can use in conditions
if isReady then print "Ready to start!"
if not gameOver then print "Game continues"
```

#### date
```javascript
var birthday as date = "2015-03-15"
var now as date = today
var deadline as date = "2025-12-31 23:59:59"
```

### Collection Types (Intermediate)

#### array
```javascript
// Array with equals initialization (consistent with assignment)
var fruits as array = "apple", "banana", "cherry"
var numbers as array = 1, 2, 3, 4, 5

// Range syntax
var numbers as array = 1..100     // Creates array [1, 2, 3, ..., 100]
var letters as array = 'a'..'z'   // Creates array ['a', 'b', ..., 'z']
var countdown as array = 10..1    // Creates array [10, 9, 8, ..., 1]

// Typed arrays with equals
var names as array.text = "Alice", "Bob", "Charlie"
var scores as array.number = 95, 87, 92, 88
var flags as array.flag = true, false, true

// Typed indicator array (requires indicator definition)
var statuses as array.indicator("new", "active", "done") = "new", "active", "done"

// Empty array
var empty as array = 
var emptyNames as array.text = 

// Multi-type array (only with base array type)
var mixed as array = 1, "two", true, 3.14

// Arrays are 0-indexed (like most programming languages)
var first = numbers[0]      // Gets first element
var second = numbers[1]     // Gets second element
var last = numbers[99]      // Gets last element (for 100-item array)
```

**Typed Arrays:**
- `array.text` - Array of text values only
- `array.number` - Array of number values only
- `array.flag` - Array of boolean values only
- `array.indicator` - Array of indicator values only
- `array.date` - Array of date values only
- `array.record` - Array of record instances only

**Note:** Arrays are collections of multiple values that can be accessed by index. Use typed arrays for type safety.

#### indicator
```javascript
// Declare with equals initialization (consistent with assignment)
var status as indicator ("pending", "active", "complete") = "active"
var priority as indicator ("low", "medium", "high") = "medium"

// Without parentheses (alternative syntax)
var color as indicator "red", "yellow", "green" = "red"
var state as indicator "on", "off", "standby" = "off"

// Assignment after declaration - must be one of the allowed values
status = "pending"      // OK
status = "active"       // OK
status = "canceled"     // ERROR: Not in the allowed set

// Usage in comparisons
if status = "active" then print "Processing"
if color = "red" then print "Stop"
if priority = "high" then print "Urgent!"

// Common use cases with equals initialization
var direction as indicator ("north", "south", "east", "west") = "north"
var mood as indicator ("happy", "sad", "neutral") = "neutral"
var grade as indicator ("A", "B", "C", "D", "F") = "B"
```

**Benefits:**
- ✅ Type-safe enumeration - only allowed values accepted
- ✅ Self-documenting - valid values visible in declaration
- ✅ Prevents typos - compiler catches invalid values
- ✅ Required initialization - prevents uninitialized indicators
- ✅ Consistent syntax - equals sign like standard assignments
- ✅ Perfect for states, statuses, colors, priorities, modes

**Difference from array:**
- **`array`** - Collection of multiple values accessed by index: `myArray[0]`, `myArray[1]`, etc.
- **`indicator`** - Single value that must be one of a predefined set: `myStatus = "active"`

#### record

**Named Record Types** - Define reusable record structures:

```javascript
// Define record type with 'record type' keyword
record type Person
    name as text
    age as number
    email as text
end

// Alternative with curly braces
record type Student {
    name as text
    age as number
    grade as number
}

// Create record instances
var student as Person = record {
    name: "Alice",
    age: 10,
    email: "alice@school.com"
}

var teacher as Person = record {
    name: "Bob",
    age: 35,
    email: "bob@school.com"
}

// Access fields (case-insensitive field names)
print student.name      // "Alice"
print student.NAME      // "Alice" (same field)
print student.Age       // 10 (same field)
student.age = 11        // Update field

// Nested records
record type Address
    street as text
    city as text
    zipCode as text
end

record type Employee {
    name as text
    age as number
    address as Address
    salary as number
}

var emp as Employee = record {
    name: "Charlie",
    age: 28,
    address: record {
        street: "123 Main St",
        city: "Springfield",
        zipCode: "12345"
    },
    salary: 50000
}

// Access nested fields
print emp.address.city  // "Springfield"

// Records with arrays
record type Team
    name as text
    members as array.text
    scores as array.number
end

var team as Team = record {
    name: "Tigers",
    members: "Alice", "Bob", "Charlie",
    scores: 95, 87, 92
}

// Access array fields in records
print team.members[0]       // "Alice"
print team.scores[1]        // 87

// Loop through array in record
for each member in team.members {
    print member
}

// Complex nested structure with arrays and records
record type Course
    title as text
    instructor as text
    students as array.record(Student)
    grades as array.number
end

var course as Course = record {
    title: "Math 101",
    instructor: "Dr. Smith",
    students: 
        record { name: "Alice", age: 10, grade: 95 },
        record { name: "Bob", age: 11, grade: 87 },
    grades: 95, 87, 90
}

// Access nested arrays and records
print course.students[0].name       // "Alice"
print course.grades[1]              // 87

for each student in course.students {
    print student.name + ": " + student.grade
}
```

**Anonymous Record Types** - Inline record definitions without named types:

```javascript
// Anonymous record (no type definition needed)
var person as record = record {
    name: "Alice",
    age: 10,
    city: "Boston"
}

// Access fields the same way
print person.name       // "Alice"
print person.age        // 10
person.age = 11         // Update field

// Anonymous records in functions
function createUser(name as text, email as text) as record {
    return record {
        name: name,
        email: email,
        createdAt: now(),
        isActive: true
    }
}

var user = createUser("Bob", "bob@email.com")
print user.name         // "Bob"
print user.isActive     // true

// Array of anonymous records
var points as array.record = 
    record { x: 10, y: 20 },
    record { x: 30, y: 40 },
    record { x: 50, y: 60 }

for each point in points {
    print "X: " + point.x + ", Y: " + point.y
}

// Nested anonymous records
var config as record = record {
    server: record {
        host: "localhost",
        port: 8080
    },
    database: record {
        name: "mydb",
        user: "admin"
    }
}

print config.server.host    // "localhost"
print config.database.name  // "mydb"

// Anonymous records with arrays
var classroom as record = record {
    roomNumber: "101",
    students: "Alice", "Bob", "Charlie", "Diana",
    grades: 95, 87, 92, 88,
    equipment: "projector", "whiteboard", "computers"
}

// Access arrays within anonymous records
print classroom.students[0]     // "Alice"
print classroom.grades[2]       // 92

for each item in classroom.equipment {
    print "Equipment: " + item
}

// Complex anonymous record with nested records and arrays
var school as record = record {
    name: "Elementary School",
    year: 2024,
    classes: 
        record { 
            name: "Math 101", 
            students: "Alice", "Bob",
            scores: 95, 87
        },
        record { 
            name: "Science 101", 
            students: "Charlie", "Diana",
            scores: 92, 88
        }
}

// Access nested structures
print school.classes[0].name            // "Math 101"
print school.classes[0].students[0]     // "Alice"
print school.classes[1].scores[1]       // 88
```

**When to use each:**

| Named Records | Anonymous Records |
|---------------|-------------------|
| Reusable structure needed | One-time or ad-hoc structure |
| Complex data models | Simple temporary data |
| Type safety across codebase | Flexibility and rapid prototyping |
| Documentation important | Quick scripts and experiments |

**Benefits of anonymous records:**
- ✅ **Rapid prototyping** - No need to define types upfront
- ✅ **Flexibility** - Structure can vary per instance
- ✅ **Simplicity** - Less boilerplate for simple data
- ✅ **Inline definition** - See structure where it's used

**Array of Records:**

```javascript
// Array of named record types
record type Student
    name as text
    age as number
    grade as number
end

var students as array.record(Student) = 
    record { name: "Alice", age: 10, grade: 95 },
    record { name: "Bob", age: 11, grade: 87 },
    record { name: "Charlie", age: 10, grade: 92 }

// Access records in array (case-insensitive field names)
print students[0].name     // "Alice"
print students[0].NAME     // "Alice" (same field)
print students[1].grade    // 87
print students[1].GRADE    // 87 (same field)

// Loop through records
for each student in students {
    print student.name + ": " + student.grade
}

// Modify records in array
students[0].grade = 98

// Add new record to array
students.push(record { name: "Diana", age: 11, grade: 90 })

// Array of anonymous records (no type definition)
var points as array.record = 
    record { x: 10, y: 20 },
    record { x: 30, y: 40 },
    record { x: 50, y: 60 }

for each point in points {
    print "Point: (" + point.x + ", " + point.y + ")"
}
```

**Benefits:**
- ✅ **Structured data** - Organize related information together
- ✅ **Type safety** - Named records enforce structure, anonymous allow flexibility
- ✅ **Easy access** - Use dot notation: `student.name`
- ✅ **Array.record** - Type-safe arrays of structured data
- ✅ **Self-documenting** - Record type shows data structure
- ✅ **Flexible** - Anonymous records for ad-hoc structures

#### map
```javascript
// Key-value storage
var settings as map = map {
    "theme": "dark",
    "fontSize": 14,
    "autoSave": yes
}

// Access values
var theme = settings["theme"]
var size = settings["fontSize"]

// Add/update
settings["language"] = "English"
```

### Advanced Types

#### json
```javascript
var data as json = parse json from text
var config as json = load json from "config.json"
```

#### function type
```javascript
// Function as value (advanced)
var calculator as function(number, number) giving number
calculator = to add
```

## Variables

### Variable Declaration (Simple)

```javascript
// Declare with equals for literal initialization
var name as text = "Alice"
variable userName as text = "Bob"    // Same as 'var'

// Type inference with = (no type annotation)
var count = 42           // Inferred as number
var message = "Hello"    // Inferred as text

// Arrays with equals
var fruits as array = "apple", "banana", "cherry"
var numbers as array.number = 1, 2, 3, 4, 5

// Indicators with equals (required initialization)
var status as indicator ("new", "pending") = "new"
```

### Constant Declaration

Constants are declared with `const` or `constant` keywords and cannot be changed after initialization:

```javascript
// Constant declaration (both keywords work)
const PI as number = 3.14159
constant MAX_USERS as number = 100    // Same as 'const'

// Constants must be initialized
const APP_NAME as text = "MyApp"

// Attempting to change a constant causes an error
PI = 3.14                // ERROR: Cannot assign to constant
```

### Variable Declaration (Advanced)

```javascript
// Multiple declarations
var x, y, z as number = 0

// With scope
var local userName as text = "Guest"
var global appVersion as text = "1.0"
```

### Variable vs Constant Keywords

**All four keywords are supported:**

| Keyword | Alternative | Purpose | Mutable |
|---------|-------------|---------|---------|
| `var` | `variable` | Declare a variable | Yes |
| `const` | `constant` | Declare a constant | No |

**Examples:**
```javascript
// Variables (can be changed)
var count = 0
variable total = 0       // Same as 'var'
count = count + 1        // OK
total = total + 5        // OK

// Constants (cannot be changed)
const MAX = 100
constant LIMIT = 50      // Same as 'const'
MAX = 200                // ERROR: Cannot assign to constant
LIMIT = 75               // ERROR: Cannot assign to constant
```

**All keywords are case-insensitive:**
```javascript
VAR age = 10             // Same as 'var'
VARIABLE score = 0       // Same as 'variable'
CONST PI = 3.14          // Same as 'const'
CONSTANT MAX = 100       // Same as 'constant'
```

### Variable Assignment

```javascript
// Simple assignment
userName = "Bob"

// Compound assignment (advanced)
count = count + 1        -- Long form
count += 1               -- Short form (advanced)
```

### Variable Scope

```javascript
program ScopeExample

variables
    var globalVar as text = "Global"
end

functions
    to testScope
        var localVar as text = "Local"
        print globalVar   -- Can access global
        print localVar    -- Can access local
    end function
end

main
    print globalVar      -- Can access
    -- print localVar   -- ERROR: Cannot access
end
```

## Operators

### Arithmetic Operations

#### Simple Form
```javascript
var sum = 5 + 3          -- Addition: 8
var diff = 5 - 3         -- Subtraction: 2
var product = 5 * 3      -- Multiplication: 15
var quotient = 15 / 3    -- Division: 5
```

#### Advanced Form
```javascript
var remainder = 17 mod 5     -- Modulo: 2
var power = 2 ^ 8            -- Power: 256
var negated = -value         -- Negation
```

### Comparison Operations

EBS2 supports both natural language and symbolic comparison operators. Both forms are equivalent.

#### Comparison Operator Table

| Long Form (Natural) | Short Form | Description |
|---------------------|------------|-------------|
| `is equal to` | `=` or `==` | Equal |
| `is not equal to` | `<>` or `!=` | Not equal |
| `is greater than` | `>` | Greater than |
| `is less than` | `<` | Less than |
| `is greater than or equal to` | `>=` | Greater or equal |
| `is less than or equal to` | `<=` | Less or equal |

#### Natural Language (Beginner)
```javascript
if age is greater than 12 then
    print "Teenager"
end if

if score is equal to 100 then
    print "Perfect!"
end if

if lives is not equal to 0 then
    print "Still alive"
end if
```

#### Symbolic Form (Advanced)
```javascript
if age > 12 then
    print "Teenager"
end if

if score = 100 then
    print "Perfect!"
end if

if lives <> 0 then
    print "Still alive"
end if
```

### Logical Operations

EBS2 supports both natural language and symbolic logical operators. Both forms are equivalent.

#### Logical Operator Table

| Long Form (Natural) | Short Form | Description |
|---------------------|------------|-------------|
| `and` | `&&` | Logical AND |
| `or` | `\|\|` | Logical OR |
| `not` | `!` | Logical NOT |

#### Natural Language (Beginner)
```javascript
if age > 12 and age < 20 then
    print "Teenager"
end if

if day is "Saturday" or day is "Sunday" then
    print "Weekend!"
end if

if not isGameOver then
    continue playing
end if
```

#### Symbolic Form (Advanced)
```javascript
if (age > 12) && (age < 20) then
    print "Teenager"
end if

if (day = "Saturday") || (day = "Sunday") then
    print "Weekend!"
end if

if !isGameOver then
    continue playing
end if
```

### String Operations

```javascript
// Concatenation
var fullName = firstName + " " + lastName

// Repetition
var stars = "*" * 5      -- "*****"

// Interpolation (advanced)
var greeting = "Hello {name}, you are {age} years old"
    with name: "Alice" and age: 10
```

## Control Flow

### Conditional Statements

#### Simple If (Beginner)
```javascript
// Multi-line form (requires end if)
if condition then
    -- code to run if true
end if

// Single-line form (no end if needed)
if age < 5 then print "Preschooler"

// Multiple commands on one line (separated by semicolon)
if age < 5 then print "Young"; log "Age checked"
```

#### If-Else (Beginner)
```javascript
// Multi-line form
if age < 13 then
    print "Child"
else
    print "Teen or Adult"
end if

// Single-line form
if age < 13 then print "Child" else print "Teen or Adult"
```

#### Multiple Conditions (Intermediate)
```javascript
if score >= 90 then
    print "Grade A"
else if score >= 80 then
    print "Grade B"
else if score >= 70 then
    print "Grade C"
else
    print "Grade D or F"
end if
```

#### If Statement (Advanced)
```javascript
if condition then
    -- code
else if otherCondition then
    -- code
else
    -- code
end if
```

### Loops

#### Repeat Times (Beginner)
```javascript
// Multi-line form
repeat 10 times
    print "Hello"
end repeat

// Single-line form
repeat 10 times print "Hello"

// With counter (multi-line)
repeat 5 times with counter
    print "Count: " + counter
end repeat

// With counter (single-line)
repeat 5 times with counter print "Count: " + counter
```

#### Repeat While (Intermediate)
```javascript
// Loop while condition is true
repeat while count < 100
    count = count + 1
    print count
end repeat

// Loop until condition is true
repeat until finished
    process next item
end repeat
```

#### For Each (Intermediate)
```javascript
// Iterate over list
var fruits = list "apple", "banana", "cherry"

for each fruit in fruits
    print "I like " + fruit
end for

// With index (advanced)
for each fruit in fruits with index
    print index + ": " + fruit
end for
```

#### Numeric Range (Advanced)
```javascript
// Count from 1 to 10
for counter from 1 to 10
    print counter
end for

// Count with step
for counter from 0 to 100 by 10
    print counter     -- 0, 10, 20, ..., 100
end for

// Count backwards
for counter from 10 down to 1
    print counter
end for
```

#### While Loop (Advanced)
```javascript
while condition
    -- code
end while

do
    -- code
while condition
```

### Loop Control

```javascript
// Exit loop early
for counter from 1 to 100
    if counter = 50 then
        exit loop        // or: break
    end if
end for

// Skip to next iteration
for each item in items
    if item is empty then
        skip to next     // or: continue
    end if
    process item
end for
```

## Functions and Procedures

EBS2 distinguishes between **functions** (which return values) and **procedures** (which don't return values).

**IMPORTANT:** All function and procedure definitions and calls **MUST include parentheses `()`**, even when there are no parameters.

### Function vs Procedure

**Function** - Has parameters and returns a value:
```javascript
// Short form - function with return type (parentheses required)
function calculateScore(points as number) as number {
    return points * 10
}

// Function with no parameters (parentheses still required)
function getDefaultScore() as number {
    return 100
}

// Long form - function with returns clause
to calculateScore points as number returns number
    return points * 10
end function

// Usage - returns a value (parentheses required)
var score = calculateScore(50)  // score = 500
var default = getDefaultScore() // Parentheses required even with no params
```

**Procedure** - Has parameters but does NOT return a value:
```javascript
// Short form - procedure (no return type, parentheses required)
procedure displayMessage(message as text) {
    print message
    log "Message displayed"
}

// Procedure with no parameters (parentheses still required)
procedure showWelcome() {
    print "Welcome to EBS2!"
}

// Long form - procedure (no returns clause)
to displayMessage message as text
    print message
    log "Message displayed"
end procedure

// Usage - just executes, no return value (parentheses required)
displayMessage("Hello World")
showWelcome()  // Parentheses required even with no params
```

**Key Differences:**

| Feature | Function | Procedure |
|---------|----------|-----------|
| **Definition** | `function name(params) as returnType` or `function name() as returnType` | `procedure name(params)` or `procedure name()` |
| **Long Form** | `to name params returns type` | `to name params` (no returns) |
| **Returns Value** | Yes (must use `return`) | No (no `return` statement) |
| **Usage** | `var result = funcName(args)` or `funcName()` | `procName(args)` or `procName()` |
| **Purpose** | Calculate and return values | Perform actions/side effects |
| **Parentheses** | **Always required**, even with no parameters | **Always required**, even with no parameters |

**Benefits:**
- ✅ **Clear intent** - Function returns value, procedure performs action
- ✅ **Type safety** - Return type declared for functions
- ✅ **Better errors** - Compiler catches missing returns in functions
- ✅ **Self-documenting** - Code clearly shows what returns values
- ✅ **Consistency** - Same syntax whether parameters exist or not
- ✅ **Clarity** - Clear distinction between variable access and function calls
- ✅ **Familiar** - Matches JavaScript, Java, C++, Python, C# conventions

### Definition Forms

| Long Form (Natural) | Short Form (Traditional) |
|---------------------|--------------------------|
| `to functionName param returns type` | `function functionName(param) as type` |
| `to functionName returns type` (no params) | `function functionName() as type` |
| `to procedureName param` | `procedure procedureName(param)` |
| `to procedureName` (no params) | `procedure procedureName()` |
| `return value` | `return value` |
| `call functionName with value` | `functionName(value)` |
| `call functionName` (no params) | `functionName()` |

**Both forms are equivalent and can be mixed.** Choose based on your audience and preference.

**Note:** Parentheses `()` are **always required** in short form, even when there are no parameters.

### Simple Functions (Beginner)

#### Function without Parameters
```javascript
// Long form
to sayHello
    print "Hello World"
end function

// Short form (parentheses required)
procedure sayHello() {
    print "Hello World"
}

// Call it (parentheses required in short form)
call sayHello           // Long form
sayHello()              // Short form - parentheses required
```

#### Function with One Parameter
```javascript
to greet person
    print "Hello " + person
end function

// Call it
call greet with "Alice"
```

#### Function with Multiple Parameters
```javascript
to introduce person and age
    print person + " is " + age + " years old"
end function

// Call it
call introduce with person: "Alice" and age: 10
```

### Functions with Return Values

#### Simple Return (Intermediate)
```javascript
to double number
    return number * 2
end function

// Use it
var result = call double with 5
print result     -- Shows: 10
```

#### Multiple Returns (Advanced)
```javascript
to divideWithRemainder dividend and divisor
    var quotient = dividend / divisor
    var remainder = dividend mod divisor
    return quotient and remainder
end function

// Use it
var quot, rem = call divideWithRemainder with dividend: 17 and divisor: 5
print "Quotient: " + quot + ", Remainder: " + rem
```

### Advanced Function Features

#### Optional Parameters
```javascript
to greet person and title: "Friend"
    print "Hello " + title + " " + person
end function

// Call with default
call greet with "Alice"           -- "Hello Friend Alice"

// Call with custom title
call greet with "Alice" and title: "Dr."  -- "Hello Dr. Alice"
```

#### Variable Parameters
```javascript
to sum numbers...
    var total = 0
    for each num in numbers
        total = total + num
    end for
    return total
end

// Call with any number of arguments
var result = call sum with 1, 2, 3, 4, 5
```

#### Recursive Functions
```javascript
to factorial n
    if n <= 1 then
        return 1
    end function
    return n * call factorial with (n - 1)
end
```

#### Anonymous Functions (Advanced)
```javascript
var doubler = function(x) { return x * 2 }

var result = call doubler with 5
```

## Screens and UI

### Simple Screen (Beginner)

```javascript
screen HelloScreen
    title "My First Screen"
    
    label WelcomeLabel
        text "Hello World!"
        size large
    end screen
    
    button OkButton
        text "OK"
        when clicked
            hide screen HelloScreen
        end
    end
end

// Show the screen
main
    print screen HelloScreen
end
```

### Screen with Layout (Intermediate)

```javascript
screen UserForm
    title "User Information"
    width 400
    height 300
    
    layout vertical spacing 10 padding 20
    
    label NameLabel
        text "What is your name?"
    end screen
    
    textbox NameInput
        placeholder "Enter your name"
        max length 50
    end
    
    label AgeLabel
        text "How old are you?"
    end
    
    numberbox AgeInput
        minimum 1
        maximum 120
        default 10
    end
    
    button SubmitButton
        text "Submit"
        style primary
        when clicked
            var name = get text from NameInput
            var age = get value from AgeInput
            print "Hello " + name + ", you are " + age
        end
    end
end
```

### Screen with Multiple Areas (Advanced)

```javascript
screen GameScreen
    title "Space Game"
    width 800
    height 600
    
    area TopBar at top height 50
        layout horizontal spacing 10
        
        label ScoreLabel
            text "Score: 0"
        end screen
        
        label LivesLabel
            text "Lives: 3"
        end
        
        button PauseButton
            text "Pause"
            when clicked
                pause game
            end
        end
    end
    
    area GameCanvas at center
        canvas MainCanvas
            width fill
            height fill
            when draw
                draw game graphics
            end
        end
    end
    
    area BottomBar at bottom height 40
        label StatusLabel
            text "Ready to play!"
        end
    end
end
```

### UI Components

#### Labels
```javascript
label MyLabel
    text "This is a label"
    size small | medium | large
    style normal | bold | italic
    color red | blue | green | black | "##FF0000"
    align left | center | right
end
```

#### Buttons
```javascript
button MyButton
    text "Click Me"
    icon "star.png"
    style primary | secondary | success | danger
    enabled yes | no
    when clicked
        -- code to run
    end
end
```

#### Text Input
```javascript
textbox MyInput
    placeholder "Enter text..."
    default "Initial value"
    max length 100
    when changed
        var text = get text from MyInput
        -- react to changes
    end
end
```

#### Number Input
```javascript
numberbox MyNumber
    minimum 0
    maximum 100
    default 50
    step 1
    when changed
        var value = get value from MyNumber
        -- react to changes
    end
end
```

#### Checkboxes
```javascript
checkbox MyCheckbox
    text "I agree"
    checked no
    when toggled
        var isChecked = is checked MyCheckbox
        -- react to toggle
    end
end
```

#### Radio Buttons
```javascript
radiogroup Difficulty
    radio Easy
        text "Easy"
        checked yes
    end
    
    radio Medium
        text "Medium"
    end
    
    radio Hard
        text "Hard"
    end
    
    when changed
        var selected = get selected from Difficulty
        -- selected is "Easy", "Medium", or "Hard"
    end
end
```

#### Dropdowns
```javascript
dropdown ColorChoice
    option "Red"
    option "Green"
    option "Blue"
    selected "Red"
    when changed
        var color = get selected from ColorChoice
        -- react to selection
    end
end
```

#### Lists
```javascript
listbox FruitList
    items list "Apple", "Banana", "Cherry"
    multiple selection no
    when selected
        var fruit = get selected from FruitList
        -- react to selection
    end
end
```

#### Canvas (for graphics)
```javascript
canvas DrawingCanvas
    width 400
    height 300
    background white
    
    when draw
        -- Drawing commands
        draw rectangle at x:10 y:10 width:50 height:50 color:red
        draw circle at x:100 y:100 radius:30 color:blue
        draw line from x1:0 y1:0 to x2:100 y2:100 color:black width:2
    end
    
    when clicked at x and y
        -- React to clicks
        draw circle at x:x y:y radius:5 color:black
    end
end
```

## Error Handling

### Automatic Error Handling (Beginner)

By default, errors stop the program and print a helpful message:

```javascript
// This will auto-stop with helpful error
var data = read file "missing.txt"

// Error shown:
// "Cannot find file 'missing.txt'"
// "Make sure the file exists in the same folder as your program"
```

### Try-Catch (Intermediate)

```javascript
try
    var data = read file "data.txt"
    print "File loaded successfully"
catch
    print "Could not load file, using defaults"
    var data = "default content"
end try
```

### Specific Error Types (Advanced)

```javascript
try
    var data = read file "data.txt"
    var number = convert data to number
catch when file_not_found
    print "File not found"
    var data = "0"
catch when invalid_conversion
    print "File contents are not a valid number"
    var number = 0
catch otherwise as error
    print "Unexpected error: " + error.message
end try
```

### Error Types

```javascript
file_not_found          -- File doesn't exist
file_access_denied      -- No permission to read/write
invalid_conversion      -- Cannot convert type
division_by_zero        -- Math error
index_out_of_range      -- Array access error
null_value              -- Null reference
network_error           -- Internet connection issue
parse_error             -- Cannot parse data
```

### Throwing Errors (Advanced)

```javascript
to withdraw amount from account
    if amount > account.balance then
        throw error "Insufficient funds"
    end function
    account.balance = account.balance - amount
end
```

## Built-in Functions

### Console Output (Beginner)

```javascript
// Print to output (displays in HTML document or JavaFX text area)
print "Hello World"

// Print with new line
print line "Hello World"

// Print without new line
print "Hello " then print "World"

// Log to debug log (writes to log file, viewable in debug view)
log "Debug message"
log "Variable value:", myVariable

// Clear output screen
clear screen
```

**Note:** 
- `print` outputs to the main display (HTML document or JavaFX text area)
- `log` writes to a debug log file and debug view (available on both HTML5 and JavaFX)
- Both commands work identically across HTML5 and JavaFX platforms

### User Input (Beginner)

```javascript
// Ask for text
var name = ask "What is your name?"

// Ask for number
var age = ask number "How old are you?"

// Ask yes/no question
var answer = ask yes or no "Do you want to continue?"

// Choose from options
var choice = ask to choose from "Red", "Green", "Blue"
    with prompt "Pick a color:"
```

### Text Functions (Beginner)

```javascript
// Length
var len = length of "Hello"          -- 5

// Uppercase/Lowercase
var upper = uppercase "hello"         -- "HELLO"
var lower = lowercase "HELLO"         -- "hello"

// Find text
var pos = find "lo" in "Hello"       -- 4
var has = "Hello" contains "lo"      -- yes

// Get part of text
var part = take 3 from "Hello"       -- "Hel"
var part = take from 2 to 4 in "Hello"  -- "ell"

// Replace text
var result = replace "World" with "Everyone" in "Hello World"
```

### Text Functions (Advanced)

```javascript
// Split text
var parts = split "a,b,c" by ","     -- list "a", "b", "c"

// Join text
var joined = join list "a", "b", "c" with ","  -- "a,b,c"

// Trim whitespace
var trimmed = trim "  Hello  "       -- "Hello"

// Format text
var formatted = format "{name} is {age} years old"
    with name: "Alice" and age: 10

// Repeat text
var stars = repeat "*" times 5       -- "*****"

// Reverse text
var reversed = reverse "Hello"       -- "olleH"
```

### Number Functions (Beginner)

```javascript
// Round numbers
var rounded = round 3.7              -- 4
var down = round down 3.7            -- 3
var up = round up 3.2                -- 4

// Absolute value
var abs = absolute value of -5      -- 5

// Min/Max
var smallest = minimum of 5, 3, 8   -- 3
var largest = maximum of 5, 3, 8    -- 8

// Random number
var dice = random from 1 to 6
var chance = random                  -- 0.0 to 1.0
```

### Number Functions (Advanced)

```javascript
// Power and roots
var squared = 5 ^ 2                  -- 25
var cubed = power 2 to 3             -- 8
var root = square root of 16         -- 4

// Trigonometry
var s = sin of 45 degrees
var c = cos of 45 degrees
var t = tan of 45 degrees

// Convert angles
var rads = 180 degrees to radians
var degs = 3.14 radians to degrees
```

### List Functions (Beginner)

```javascript
// Create list
var numbers = list 1, 2, 3, 4, 5

// Count items
var count = count of numbers         -- 5

// Get item (0-based indexing like most programming languages)
var first = numbers[0]               -- 1
var second = numbers[1]              -- 2
var last = numbers[4]                -- 5

// Alternative syntax (long form)
var first = numbers at 0             -- 1
var last = numbers at end            -- 5

// Add item
add 6 to numbers
add 0 to start of numbers

// Remove item
remove 3 from numbers
remove first from numbers
remove last from numbers

// Check if contains
var has = numbers contains 3         -- yes/no
```

### List Functions (Advanced)

```javascript
// Filter list
var evens = filter numbers where item mod 2 = 0

// Transform list
var doubled = transform numbers with item * 2

// Sort list
var sorted = sort numbers ascending
var reversed = sort numbers descending

// Reverse list
var backwards = reverse numbers

// Join lists
var combined = join numbers with list 6, 7, 8

// Get subset
var subset = take from 2 to 4 in numbers

// Find in list
var position = find 5 in numbers
var found = find in numbers where item > 3

// Sum and average
var total = sum of numbers
var avg = average of numbers
```

### Date and Time (Beginner)

```javascript
// Get current date/time
var now = today
var time = current time

// Get parts
var year = year of now
var month = month of now
var day = day of now
var hour = hour of time
var minute = minute of time

// Format date
var formatted = format now as "YYYY-MM-DD"
var pretty = format now as "Month DD, YYYY"
```

### Date and Time (Advanced)

```javascript
// Create date
var birthday = date "2015-03-15"
var deadline = date "2025-12-31 23:59:59"

// Date arithmetic
var tomorrow = now + 1 day
var nextWeek = now + 7 days
var nextMonth = now + 1 month
var nextYear = now + 1 year

// Date difference
var age = years between birthday and now
var days = days between start and end

// Compare dates
if now is after deadline then
    print "Overdue!"
end if
```

### File Functions (Intermediate)

```javascript
// Read entire file
var content = read file "data.txt"

// Read as lines
var lines = read lines from "data.txt"

// Write to file
write "Hello World" to file "output.txt"

// Append to file
append "More text" to file "log.txt"

// Check if exists
var exists = file "data.txt" exists

// Delete file
delete file "temp.txt"

// List files in folder
var files = list files in "my_folder"
```

### File Functions (Advanced)

```javascript
// Read with encoding
var content = read file "data.txt" with encoding "UTF-8"

// Copy file
copy file "source.txt" to "backup.txt"

// Move/Rename file
move file "old.txt" to "new.txt"

// Create folder
create folder "my_folder"

// Delete folder
delete folder "temp_folder"

// Get file info
var size = size of file "data.txt"
var modified = last modified date of file "data.txt"
```

## Modules and Imports

### Simple Import (Intermediate)

```javascript
// Import all from another file
import "helpers.ebs"

// Now can use functions from helpers.ebs
call functionFromHelpers
```

### Named Imports (Advanced)

```javascript
// Import specific items
import greet, calculate from "helpers.ebs"

// Import with rename
import greet as sayHello from "helpers.ebs"

// Import all with namespace
import helpers from "helpers.ebs"
call helpers.greet with "Alice"
```

### Exports

```javascript
// helpers.ebs

// Mark functions for export
export function greet person
    print "Hello " + person
end

export function calculate sum of numbers
    var total = 0
    for each num in numbers
        total = total + num
    end for
    return total
end

// Private function (not exported)
to helperFunction
    -- only used internally
end function
```

### Module Organization

```
my_project/
├── main.ebs           -- Main program file
├── helpers.ebs        -- Helper functions
├── ui.ebs            -- Screen definitions
├── data.ebs          -- Data handling
└── modules/
    ├── math.ebs      -- Math utilities
    └── graphics.ebs  -- Graphics functions
```

## Runtime Model

### Dual Runtime Architecture

```
        EBS2 Source Code
               |
        [EBS2 Compiler]
            /    \
           /      \
    [HTML5 Runtime]  [Java Runtime]
    (JavaScript)      (JVM/JavaFX)
```

### HTML5 Runtime

**Target:** Modern web browsers  
**Transpilation:** EBS2 → JavaScript  
**UI Framework:** HTML5 + CSS3  
**Graphics:** Canvas API  
**Storage:** LocalStorage / IndexedDB

**Capabilities:**
- Runs in any modern browser
- Progressive Web App support
- Offline capability
- Mobile-friendly
- Shareable via URL

**Limitations:**
- Restricted file system access
- Browser security sandbox
- Performance limits for heavy computation

### Java Runtime

**Target:** Desktop applications  
**Execution:** EBS2 → Bytecode → JVM  
**UI Framework:** JavaFX with WebView for output  
**Graphics:** JavaFX Canvas  
**Storage:** Native file system

**Output Implementation:**
- Default screen uses JavaFX WebView component
- Renders HTML content just like a browser
- Ensures identical print behavior with HTML5 runtime

**Capabilities:**
- Full file system access
- Native performance
- Database connectivity
- Network operations
- Plugin support
- HTML rendering via WebView

**Limitations:**
- Requires JVM installation
- Desktop only (no mobile)

### Cross-Platform Features

#### Guaranteed Compatible
- Core language syntax
- Data types and variables
- Control flow
- Functions
- Basic UI components
- Text operations
- Number operations
- List operations
- Error handling
- Console output (print and log)

#### Platform-Specific
- Advanced file operations
- Database connections
- Native integrations
- Performance optimizations
- Platform UI conventions

### Output and Logging Implementation

#### print Command
**HTML5 Runtime:**
- Outputs to HTML document (appends text nodes or updates designated output area)
- Can target specific DOM elements
- Supports HTML formatting when needed

**Java Runtime:**
- Uses a default screen with a WebView component (HTML rendering)
- Outputs HTML content just like the browser version
- **Ensures identical behavior** between HTML5 and JavaFX
- Maintains consistent formatting and styling across platforms

**Benefit:** By using WebView in JavaFX, the print command produces identical output on both platforms. HTML formatting, CSS styling, and layout work the same way.

#### log Command
**HTML5 Runtime:**
- Writes to browser console (console.log)
- Stores in IndexedDB or LocalStorage for persistence
- Accessible via Debug View panel in the web IDE
- Can export log file

**Java Runtime:**
- Writes to application log file
- Accessible via Debug View window
- Supports log rotation and file management
- Can tail log in real-time

**Common Behavior:**
- Both commands are non-blocking
- Both support multiple arguments
- Both support semicolon-separated multi-command lines
- Both maintain timestamp and context information
- Debug view is available on both platforms

### Runtime Detection

```javascript
// Check current runtime
if running on browser then
    print "Running in web browser"
else if running on desktop then
    print "Running as desktop app"
end if

// Conditional features
if has feature "database" then
    -- Use database features
else
    -- Use alternative approach
end if
```

## Best Practices

### For Beginners

1. **Use natural language keywords**
   ```javascript
   if age is greater than 12 then
       -- instead of: if age > 12 then
   ```

2. **Use descriptive names**
   ```javascript
   var playerScore = 100
   -- instead of: var s = 100
   ```

3. **Add comments to explain**
   ```javascript
   -- Calculate the player's final score
   var finalScore = playerScore * difficulty
   ```

4. **Break down complex logic**
   ```javascript
   -- Not recommended
   if (age > 12 and age < 20) or isSpecial then
   
   -- Better
   var isTeenager = age > 12 and age < 20
   if isTeenager or isSpecial then
   ```

### For Advanced Users

1. **Use type annotations**
   ```javascript
   var scores as list of number = list 90, 85, 92
   ```

2. **Leverage advanced features when appropriate**
   ```javascript
   var doubled = transform numbers with item * 2
   -- instead of manual loop
   ```

3. **Use proper error handling**
   ```javascript
   try
       var data = read file "config.json"
   catch when file_not_found
       var data = default configuration
   end try
   ```

4. **Organize code into modules**
   ```javascript
   import math, graphics from "utilities.ebs"
   ```

## Migration from EBS1

### Key Differences

| Feature | EBS1 | EBS2 |
|---------|------|------|
| Keywords | `if`, `while`, `var` | `when`, `repeat`, `var` |
| Types | `string`, `int`, `float` | `text`, `number` |
| Functions | `greet(name: string)` | `to greet person` |
| Arrays | 0-based indexing | 1-based indexing (configurable) |
| Structure | Free-form | Organized sections |
| Comments | `//` and `/* */` | `--` and `--[[ ]]--` |

### Migration Tool

```bash
# Convert EBS1 code to EBS2
ebs2-migrate convert myapp.ebs --output myapp-v2.ebs

# Check compatibility
ebs2-migrate check myapp.ebs

# Interactive migration
ebs2-migrate interactive myapp.ebs
```

## Grammar Summary (EBNF)

```ebnf
program         = "program" identifier sections "end"
sections        = [settings] [variables] [functions] [screens] main
settings        = "settings" setting* "end"
variables       = "variables" varDecl* "end"
functions       = "functions" function* "end" "functions"
screens         = "screens" screen* "end" "screens"
main            = "main" statement* "end"

statement       = varDecl | assignment | ifStmt | loopStmt | 
                  functionCall | return | print | try-catch

varDecl         = ("var" | "variable") identifier ["as" type] ["=" expression]
                | ("const" | "constant") identifier ["as" type] "=" expression
assignment      = identifier "=" expression
functionCall    = "call" identifier ["with" arguments]

ifStmt          = "if" expression "then" 
                  (statement "end" "if" | singleStatement)
                  ["else" (statement* "end" "if" | singleStatement)]

loopStmt        = repeatLoop | forEachLoop | whileLoop

repeatLoop      = "repeat" (number | expression) "times" 
                  (statement* "end" "repeat" | singleStatement)

forEachLoop     = "for" "each" identifier "in" expression
                  (statement* "end" "for" | singleStatement)

whileLoop       = "while" expression 
                  (statement* "end" "while" | singleStatement)

function        = ("to" identifier params | "function" identifier "(" params ")")
                  statement* "end" "function"

tryStmt         = "try" statement* 
                  "catch" [errorCondition] statement* 
                  "end" "try"

screenDef       = "screen" identifier screenContent* "end" "screen"

statement       = varDecl | assignment | ifStmt | loopStmt | 
                  functionCall | return | print | try-catch

singleStatement = simpleStatement [";" simpleStatement]*
simpleStatement = varDecl | assignment | functionCall | return | print

expression      = primary (operator primary)*
primary         = literal | identifier | "(" expression ")" | functionCall
literal         = number | text | boolean | list | range

list            = "list" (elements | range)
range           = expression ".." expression

type            = "number" | "text" | "yes/no" | "date" | "list" | 
                  "list" "of" type | record | map
```

## Appendices

### Appendix A: Complete Keyword List

**Beginner Keywords:** program, end, var, as, is, if, then, else, repeat, times, for, each, in, to, call, with, and, give, back, print, log, hide, ask, screen, button, label, textbox, list, text, number, yes, no, true, false

**Advanced Keywords:** function, return, try, catch, throw, import, from, export, record, map, json, while, until, break, continue, switch, case, default, const, let, async, await, class, extends, new

### Appendix B: Long Form vs Short Form Reference

EBS2 supports both natural language (long form) and symbolic (short form) syntax. This table shows all equivalents:

#### Comparison Operators
| Long Form | Short Form | Example |
|-----------|------------|---------|
| `is equal to` | `=` or `==` | `if x is equal to 5` → `if x = 5` |
| `is not equal to` | `<>` or `!=` | `if x is not equal to 5` → `if x <> 5` |
| `is greater than` | `>` | `if x is greater than 5` → `if x > 5` |
| `is less than` | `<` | `if x is less than 5` → `if x < 5` |
| `is greater than or equal to` | `>=` | `if x is greater than or equal to 5` → `if x >= 5` |
| `is less than or equal to` | `<=` | `if x is less than or equal to 5` → `if x <= 5` |

#### Logical Operators
| Long Form | Short Form | Example |
|-----------|------------|---------|
| `and` | `&&` | `if a and b` → `if a && b` |
| `or` | `\|\|` | `if a or b` → `if a \|\| b` |
| `not` | `!` | `if not done` → `if !done` |

#### Function Syntax
| Long Form | Short Form | Example |
|-----------|------------|---------|
| `to name params` | `function name(params)` | `to greet person` → `function greet(person)` |
| `return value` | `return value` | `return x * 2` → `return x * 2` |
| `call func with args` | `func(args)` | `call greet with "Alice"` → `greet("Alice")` |

#### Loop Control
| Long Form | Short Form | Example |
|-----------|------------|---------|
| `exit loop` | `break` | `exit loop` → `break` |
| `skip to next` | `continue` | `skip to next` → `continue` |

**Note:** Both forms are completely equivalent and can be mixed within the same program. Choose based on your audience and preference.

### Appendix C: Complete Built-in Function List

See section [Built-in Functions](#built-in-functions) for detailed documentation.

**Categories:**
- Console Output (print, log, clear, ...)
- User Input (ask, choose, ...)
- Text Operations (length, uppercase, split, ...)
- Number Operations (round, absolute, random, ...)
- List Operations (count, filter, sort, ...)
- Date/Time (today, current time, format, ...)
- File Operations (read, write, exists, ...)
- Screen Operations (print screen, hide screen, ...)

### Appendix D: Migration Checklist

- [ ] Review breaking changes from EBS1
- [ ] Update variable declarations (string → text, int/float → number)
- [ ] Update array indexing (0-based → 1-based)
- [ ] Organize code into sections (optional but recommended)
- [ ] Test in both HTML5 and Java runtimes
- [ ] Consider using natural language syntax for beginner-facing code

**Note:** Comments (`//`), control structures (`if/else`), and most syntax remain compatible with EBS1.

### Appendix E: Standard Library Organization

```
ebs2/
├── core/           -- Core language features (always loaded)
├── text/           -- Extended text functions
├── math/           -- Advanced math functions
├── collections/    -- Advanced list, map operations
├── files/          -- File I/O operations
├── network/        -- HTTP, WebSocket functions
├── graphics/       -- Drawing and animation
├── ui/            -- UI components and helpers
└── database/      -- Database connectivity (Java only)
```

---

## Document Status

**Status:** SPECIFICATION DRAFT  
**Next Steps:**
1. Review and refine specification
2. Prototype implementation
3. Create example programs
4. Gather feedback from target users (children)
5. Iterate on design

**Reviewers:**
- [ ] Technical Lead
- [ ] Education Specialist
- [ ] Child User Testing
- [ ] Community Feedback

---

**Version History:**
- 2.0.0-SPEC (2025-12): Initial specification draft
