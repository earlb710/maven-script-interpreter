# EBS2 Quick Reference Card

**Version:** 2.0.0  
**Last Updated:** December 28, 2025

---

## Program Structure

```javascript
program MyApp

variables
    var myVar as text = "hello"
    const MAX_SIZE as number = 100
end

functions
    function calculate(x as number) as number {
        return x * 2
    }
    
    procedure greet(name as text) {
        print "Hello " + name
    }
end

screens
    screen MyWindow
        title "My App"
        // screen content
    end
end

main
    // Program starts here
    print "Starting..."
end
```

---

## Data Types

| Type | Description | Example |
|------|-------------|---------|
| `text` | String of characters | `"Hello World"` |
| `number` | Integer or decimal | `42` or `3.14` |
| `number 0..100` | Ranged integer | `85` (0-100 only) |
| `number -1.0..1.0` | Ranged decimal | `0.5` (-1.0 to 1.0) |
| `flag` | Boolean true/false | `true` or `false` |
| `array` | Collection of values | `[1, 2, 3]` |
| `array.text` | Array of text | `["a", "b", "c"]` |
| `array.number` | Array of numbers | `[1, 2, 3]` |
| `indicator` | Enum/choice | `"red"`, `"yellow"`, `"green"` |
| `record` | Structured data | `{name: "Alice", age: 30}` |
| `map` | Key-value pairs | `{"key": "value"}` |
| `date` | Date/time value | `2025-12-28` |

---

## Variables and Constants

```javascript
// Variables (mutable)
var name as text = "Alice"
var age as number = 10
var scores as array = 85, 90, 95

// Type inference (type is inferred from value)
var city = "New York"          // inferred as text
var count = 42                  // inferred as number
var ready = true                // inferred as flag

// Constants (immutable)
const PI as number = 3.14159
const MAX_PLAYERS as number = 4
const GAME_NAME as text = "Chess"

// Ranged numbers
var percentage as number 0..100 = 75
var temperature as number -50.0..50.0 = 22.5

// Indicators (enums)
var status as indicator "pending", "active", "complete"
status = "active"
```

---

## Control Flow

### If-Then-Else

```javascript
// Multi-line form
if age < 13 then
    print "Child"
else if age < 20 then
    print "Teenager"
else
    print "Adult"
end if

// Single-line form
if age < 5 then print "Preschooler"

// With curly braces
if score >= 90 {
    print "Grade A"
} else if score >= 80 {
    print "Grade B"
} else {
    print "Grade C"
}
```

### Comparison Operators

| Long Form | Short Form | Meaning |
|-----------|------------|---------|
| `is equal to` | `=` or `==` | Equal |
| `is not equal to` | `!=` or `<>` | Not equal |
| `is greater than` | `>` | Greater than |
| `is less than` | `<` | Less than |
| `is greater than or equal to` | `>=` | Greater or equal |
| `is less than or equal to` | `<=` | Less or equal |

### Logical Operators

| Long Form | Short Form | Meaning |
|-----------|------------|---------|
| `and` | `&&` or `&` | Logical AND |
| `or` | `\|\|` or `\|` | Logical OR |
| `not` | `!` | Logical NOT |

### Increment/Decrement Operators

```javascript
var count = 5
count++        // count is now 6 (post-increment)
++count        // count is now 7 (pre-increment)
count--        // count is now 6 (post-decrement)
--count        // count is now 5 (pre-decrement)
```

---

## Loops

### Repeat Times

```javascript
// With counter
repeat 10 times with counter
    print counter
end repeat

// Without counter
repeat 5 times
    print "Hello"
end repeat
```

### For Each

```javascript
var fruits as array = "apple", "banana", "cherry"

for each fruit in fruits
    print fruit
end for
```

### While

```javascript
var count = 5

repeat while count > 0
    print count
    count = count - 1
end repeat
```

---

## Functions and Procedures

### Functions (Return Values)

```javascript
// Short form
function add(a as number, b as number) as number {
    return a + b
}

// Long form
to add a as number and b as number returns number
    return a + b
end function

// Usage
var sum = add(5, 3)
```

### Procedures (No Return Value)

```javascript
// Short form
procedure greet(name as text) {
    print "Hello " + name
}

// Long form
to greet person as text
    print "Hello " + person
end procedure

// Usage
greet("Alice")
```

---

## Arrays

```javascript
// Create array
var numbers as array = 1, 2, 3, 4, 5
var fruits as array = "apple", "banana", "cherry"

// Range syntax
var oneToTen = 1..10          // Creates [1, 2, 3, ..., 10]
var oneToHundred = 1..100     // Creates [1, 2, ..., 100]

// Access elements (0-based indexing)
var first = numbers[0]        // 1 (first element)
var second = numbers[1]       // 2 (second element)

// Array length
var size = numbers.length     // 5

// Typed arrays
var names as array.text = "Alice", "Bob", "Charlie"
var scores as array.number = 85, 90, 95
```

---

## Records

```javascript
// Named record type
record type Person
    name as text
    age as number
    email as text
end

var person as Person = {
    name: "Alice",
    age: 30,
    email: "alice@example.com"
}

// Access fields
print person.name              // "Alice"
print person.age               // 30

// Anonymous record
var point as record = {x: 10, y: 20}
print point.x                  // 10
```

---

## Screens and UI

### Basic Screen

```javascript
screen MyWindow
    title "My Application"
    
    label WelcomeLabel
        text "Welcome!"
    end
    
    button ClickButton
        text "Click Me"
        when clicked
            print "Button clicked!"
        end
    end
end

main
    print screen MyWindow
end
```

### Screen Components

| Component | Purpose | Example |
|-----------|---------|---------|
| `label` | Display text | `text "Hello"` |
| `button` | Clickable button | `when clicked` |
| `textbox` | Text input | `placeholder "Enter name"` |
| `numberbox` | Number input | `minimum 0, maximum 100` |
| `checkbox` | Toggle option | `checked yes/no` |

---

## Comments

```javascript
// Single-line comment (C-style)
var x = 10  // Comment after code

// Multi-line comments use multiple lines
// This is line 1
// This is line 2
```

**Note:** Block comments (`/* */`) are NOT supported.

---

## Common Built-in Functions

### Output

```javascript
print "Hello"                  // Display to user
log "Debug message"            // Debug output only
```

### String Operations

```javascript
var text = "Hello World"
var upper = text.toUpper()     // "HELLO WORLD"
var lower = text.toLower()     // "hello world"
var len = text.length          // 11
var sub = text.substring(0, 5) // "Hello"
```

### Math Operations

```javascript
var num = -5
var abs = math.abs(num)        // 5
var max = math.max(10, 20)     // 20
var min = math.min(10, 20)     // 10
var rand = math.random()       // Random 0.0-1.0
var sqrt = math.sqrt(16)       // 4
```

### Array Operations

```javascript
var arr = [1, 2, 3]
arr.push(4)                    // [1, 2, 3, 4]
var last = arr.pop()           // [1, 2, 3], last=4
var first = arr.shift()        // [2, 3], first=1
arr.unshift(0)                 // [0, 2, 3]
```

---

## Error Handling

```javascript
try {
    var result = 10 / 0
} catch MATH_ERROR as error {
    print "Math error: " + error
} catch ANY_ERROR as error {
    print "Unexpected error: " + error
}
```

### Error Types

- `ANY_ERROR` - Catch all errors
- `MATH_ERROR` - Math operations
- `IO_ERROR` - File I/O
- `TYPE_ERROR` - Type conversion
- `INDEX_ERROR` - Array bounds
- `PARSE_ERROR` - JSON/parsing

---

## Type Conversion

```javascript
// To number
var num = number("42")         // String to number
var floatNum = number("3.14")  // String to decimal

// To text
var str = text(42)             // Number to string

// To flag
var bool = flag("true")        // String to boolean
```

---

## Imports

```javascript
// Import entire file
import "utils.ebs"

// Import specific items
import {calculate, format} from "helpers.ebs"

// Import with alias
import {longFunctionName as short} from "module.ebs"
```

---

## Quick Tips

### Case Insensitivity
All keywords and identifiers are case-insensitive:
- `IF`, `if`, `If` all work the same
- `myVariable`, `MyVariable`, `MYVARIABLE` refer to the same variable

### Semicolons Optional
Semicolons are optional but can separate multiple statements on one line:
```javascript
var x = 10; var y = 20; print x + y
```

### Block Styles
Choose between `end` keywords or `{}` braces (pick one style per project):
```javascript
// With end keywords
if x > 0 then
    print "Positive"
end if

// With curly braces
if x > 0 {
    print "Positive"
}
```

### Type Inference
Types can be inferred from values:
```javascript
var name = "Alice"             // Inferred as text
var age = 30                   // Inferred as number
var ready = true               // Inferred as flag
```

---

## See Also

- [EBS2 Language Specification](EBS2_LANGUAGE_SPEC.md) - Complete formal specification
- [Quick Start Guide](EBS2_QUICK_START_GUIDE.md) - Beginner-friendly tutorial
- [Comparison with EBS1](EBS1_VS_EBS2_COMPARISON.md) - Migration guide
- [Implementation Roadmap](EBS2_IMPLEMENTATION_ROADMAP.md) - Development plan

---

**Note:** This is a quick reference for EBS2 specification. For complete details and examples, see the full language specification.
