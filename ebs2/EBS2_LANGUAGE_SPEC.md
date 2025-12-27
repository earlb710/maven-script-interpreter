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

### Language Goals

- **For Children**: Natural syntax that reads like English stories
- **For Learners**: Progressive complexity with clear upgrade paths
- **For Developers**: Powerful features when needed, without compromising simplicity
- **For Deployment**: Write once, run anywhere (browser or desktop)

## Lexical Structure

### Comments

```javascript
-- Single line comment (two dashes)

--[[
  Multi-line comment
  Can span multiple lines
--]]

--- Documentation comment (three dashes)
--- Used for generating help text
```

### Keywords

#### Basic Keywords (Beginner Level)
```
program     end         var         as          is
when        then        otherwise   repeat      times
for         each        in          to          call
with        and         give        back        show
hide        ask         screen      button      label
textbox     list        text        number      yes
no          true        false
```

#### Advanced Keywords
```
function    return      try         catch       throw
import      from        export      record      map
json        while       until       break       continue
switch      case        default     const       let
async       await       class       extends     new
```

### Identifiers

- Start with letter or underscore
- Contain letters, digits, underscores
- Case-insensitive (normalized to lowercase internally)
- Cannot be reserved keywords

**Valid:** `myVariable`, `user_name`, `count2`, `_private`  
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
yes         -- Boolean true (beginner-friendly)
no          -- Boolean false
true        -- Also supported
false       -- Also supported
```

#### List Literals
```javascript
list 1, 2, 3, 4, 5                    -- Simple list
list of numbers 1, 2, 3               -- Typed list
list "apple", "banana", "cherry"      -- Text list
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
    show "Hello World"
end
```

### Complete Program Structure

```javascript
program ProgramName

-- Optional: Program metadata
settings
    title "My Application"
    version "1.0.0"
    author "Student Name"
    description "What this program does"
end

-- Optional: Global variables
variables
    var userName as text = "Guest"
    var score as number = 0
    var isPlaying as yes/no = no
end

-- Optional: Function definitions
functions
    to greet person
        show "Hello " + person
    end
    
    to calculate score
        give back score * 10
    end
end

-- Optional: Screen definitions
screens
    screen MainWindow
        title "Main Screen"
        -- screen content
    end
end

-- Required: Main entry point
main
    -- Program starts here
    show screen MainWindow
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

## Data Types

### Basic Types (Beginner)

#### number
```javascript
var count as number = 42
var price as number = 19.99
var big as number = 1000000

-- Automatic type selection (int, float, double)
-- Children don't need to understand the difference
```

#### text
```javascript
var name as text = "Alice"
var message as text = "Hello World"
var empty as text = ""
```

#### yes/no
```javascript
var isReady as yes/no = yes
var gameOver as yes/no = no

-- Can also use true/false
var isActive as yes/no = true
```

#### date
```javascript
var birthday as date = "2015-03-15"
var now as date = today
var deadline as date = "2025-12-31 23:59:59"
```

### Collection Types (Intermediate)

#### list
```javascript
-- Simple list
var numbers as list = list 1, 2, 3, 4, 5

-- Typed list
var names as list of text = list "Alice", "Bob", "Charlie"

-- Empty list
var items as list of number = empty list

-- Multi-type list (advanced)
var mixed as list = list 1, "two", yes, 3.14
```

#### record
```javascript
-- Define record type
type Person
    name as text
    age as number
    email as text
end

-- Create record
var student as Person = record {
    name: "Alice",
    age: 10,
    email: "alice@school.com"
}

-- Access fields
show student.name
show student.age
```

#### map
```javascript
-- Key-value storage
var settings as map = map {
    "theme": "dark",
    "fontSize": 14,
    "autoSave": yes
}

-- Access values
var theme = settings["theme"]
var size = settings["fontSize"]

-- Add/update
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
-- Function as value (advanced)
var calculator as function(number, number) giving number
calculator = to add
```

## Variables

### Variable Declaration (Simple)

```javascript
-- Declare with initial value
var name as text = "Alice"

-- Type inference (intermediate)
var count = 42           -- Inferred as number
var message = "Hello"    -- Inferred as text
```

### Variable Declaration (Advanced)

```javascript
-- Constant (cannot change)
constant PI as number = 3.14159

-- Multiple declarations
var x, y, z as number = 0

-- With scope
var local userName as text = "Guest"
var global appVersion as text = "1.0"
```

### Variable Assignment

```javascript
-- Simple assignment
userName = "Bob"

-- Compound assignment (advanced)
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
        show globalVar   -- Can access global
        show localVar    -- Can access local
    end
end

main
    show globalVar      -- Can access
    -- show localVar   -- ERROR: Cannot access
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

#### Natural Language (Beginner)
```javascript
when age is greater than 12 then
    show "Teenager"
end

when score is equal to 100 then
    show "Perfect!"
end

when lives is not equal to 0 then
    show "Still alive"
end
```

#### Symbolic Form (Advanced)
```javascript
if age > 12 then
    show "Teenager"
end

if score = 100 then
    show "Perfect!"
end

if lives <> 0 then
    show "Still alive"
end
```

### Logical Operations

#### Natural Language (Beginner)
```javascript
when age > 12 and age < 20 then
    show "Teenager"
end

when day is "Saturday" or day is "Sunday" then
    show "Weekend!"
end

when not isGameOver then
    continue playing
end
```

#### Symbolic Form (Advanced)
```javascript
if (age > 12) && (age < 20) then
    show "Teenager"
end

if (day = "Saturday") || (day = "Sunday") then
    show "Weekend!"
end

if !isGameOver then
    continue playing
end
```

### String Operations

```javascript
-- Concatenation
var fullName = firstName + " " + lastName

-- Repetition
var stars = "*" * 5      -- "*****"

-- Interpolation (advanced)
var greeting = "Hello {name}, you are {age} years old"
    with name: "Alice" and age: 10
```

## Control Flow

### Conditional Statements

#### Simple When (Beginner)
```javascript
when condition then
    -- code to run if true
end
```

#### When-Otherwise (Beginner)
```javascript
when age < 13 then
    show "Child"
otherwise
    show "Teen or Adult"
end
```

#### Multiple Conditions (Intermediate)
```javascript
when score >= 90 then
    show "Grade A"
otherwise when score >= 80 then
    show "Grade B"
otherwise when score >= 70 then
    show "Grade C"
otherwise
    show "Grade D or F"
end
```

#### If Statement (Advanced)
```javascript
if condition then
    -- code
else if otherCondition then
    -- code
else
    -- code
end
```

### Loops

#### Repeat Times (Beginner)
```javascript
-- Fixed repetition
repeat 10 times
    show "Hello"
end

-- With counter
repeat 5 times with counter
    show "Count: " + counter
end
```

#### Repeat While (Intermediate)
```javascript
-- Loop while condition is true
repeat while count < 100
    count = count + 1
    show count
end

-- Loop until condition is true
repeat until finished
    process next item
end
```

#### For Each (Intermediate)
```javascript
-- Iterate over list
var fruits = list "apple", "banana", "cherry"

for each fruit in fruits
    show "I like " + fruit
end

-- With index (advanced)
for each fruit in fruits with index
    show index + ": " + fruit
end
```

#### Numeric Range (Advanced)
```javascript
-- Count from 1 to 10
for counter from 1 to 10
    show counter
end

-- Count with step
for counter from 0 to 100 by 10
    show counter     -- 0, 10, 20, ..., 100
end

-- Count backwards
for counter from 10 down to 1
    show counter
end
```

#### While Loop (Advanced)
```javascript
while condition
    -- code
end

do
    -- code
while condition
```

### Loop Control

```javascript
-- Exit loop early
for counter from 1 to 100
    when counter = 50 then
        exit loop        -- or: break
    end
end

-- Skip to next iteration
for each item in items
    when item is empty then
        skip to next     -- or: continue
    end
    process item
end
```

## Functions

### Simple Functions (Beginner)

#### Function without Parameters
```javascript
to sayHello
    show "Hello World"
end

-- Call it
call sayHello
```

#### Function with One Parameter
```javascript
to greet person
    show "Hello " + person
end

-- Call it
call greet with "Alice"
```

#### Function with Multiple Parameters
```javascript
to introduce person and age
    show person + " is " + age + " years old"
end

-- Call it
call introduce with person: "Alice" and age: 10
```

### Functions with Return Values

#### Simple Return (Intermediate)
```javascript
to double number
    give back number * 2
end

-- Use it
var result = call double with 5
show result     -- Shows: 10
```

#### Multiple Returns (Advanced)
```javascript
to divideWithRemainder dividend and divisor
    var quotient = dividend / divisor
    var remainder = dividend mod divisor
    give back quotient and remainder
end

-- Use it
var quot, rem = call divideWithRemainder with dividend: 17 and divisor: 5
show "Quotient: " + quot + ", Remainder: " + rem
```

### Advanced Function Features

#### Optional Parameters
```javascript
to greet person and title: "Friend"
    show "Hello " + title + " " + person
end

-- Call with default
call greet with "Alice"           -- "Hello Friend Alice"

-- Call with custom title
call greet with "Alice" and title: "Dr."  -- "Hello Dr. Alice"
```

#### Variable Parameters
```javascript
to sum numbers...
    var total = 0
    for each num in numbers
        total = total + num
    end
    give back total
end

-- Call with any number of arguments
var result = call sum with 1, 2, 3, 4, 5
```

#### Recursive Functions
```javascript
to factorial n
    when n <= 1 then
        give back 1
    end
    give back n * call factorial with (n - 1)
end
```

#### Anonymous Functions (Advanced)
```javascript
var doubler = function(x) { give back x * 2 }

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
    end
    
    button OkButton
        text "OK"
        when clicked
            hide screen HelloScreen
        end
    end
end

-- Show the screen
main
    show screen HelloScreen
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
    end
    
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
            show "Hello " + name + ", you are " + age
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
        end
        
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

By default, errors stop the program and show a helpful message:

```javascript
-- This will auto-stop with helpful error
var data = read file "missing.txt"

-- Error shown:
-- "Cannot find file 'missing.txt'"
-- "Make sure the file exists in the same folder as your program"
```

### Try-Catch (Intermediate)

```javascript
try
    var data = read file "data.txt"
    show "File loaded successfully"
catch
    show "Could not load file, using defaults"
    var data = "default content"
end
```

### Specific Error Types (Advanced)

```javascript
try
    var data = read file "data.txt"
    var number = convert data to number
catch when file_not_found
    show "File not found"
    var data = "0"
catch when invalid_conversion
    show "File contents are not a valid number"
    var number = 0
catch otherwise as error
    show "Unexpected error: " + error.message
end
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
    when amount > account.balance then
        throw error "Insufficient funds"
    end
    account.balance = account.balance - amount
end
```

## Built-in Functions

### Console Output (Beginner)

```javascript
-- Simple print
show "Hello World"

-- Print with new line
show line "Hello World"

-- Print without new line
show "Hello " then show "World"

-- Clear screen
clear screen
```

### User Input (Beginner)

```javascript
-- Ask for text
var name = ask "What is your name?"

-- Ask for number
var age = ask number "How old are you?"

-- Ask yes/no question
var answer = ask yes or no "Do you want to continue?"

-- Choose from options
var choice = ask to choose from "Red", "Green", "Blue"
    with prompt "Pick a color:"
```

### Text Functions (Beginner)

```javascript
-- Length
var len = length of "Hello"          -- 5

-- Uppercase/Lowercase
var upper = uppercase "hello"         -- "HELLO"
var lower = lowercase "HELLO"         -- "hello"

-- Find text
var pos = find "lo" in "Hello"       -- 4
var has = "Hello" contains "lo"      -- yes

-- Get part of text
var part = take 3 from "Hello"       -- "Hel"
var part = take from 2 to 4 in "Hello"  -- "ell"

-- Replace text
var result = replace "World" with "Everyone" in "Hello World"
```

### Text Functions (Advanced)

```javascript
-- Split text
var parts = split "a,b,c" by ","     -- list "a", "b", "c"

-- Join text
var joined = join list "a", "b", "c" with ","  -- "a,b,c"

-- Trim whitespace
var trimmed = trim "  Hello  "       -- "Hello"

-- Format text
var formatted = format "{name} is {age} years old"
    with name: "Alice" and age: 10

-- Repeat text
var stars = repeat "*" times 5       -- "*****"

-- Reverse text
var reversed = reverse "Hello"       -- "olleH"
```

### Number Functions (Beginner)

```javascript
-- Round numbers
var rounded = round 3.7              -- 4
var down = round down 3.7            -- 3
var up = round up 3.2                -- 4

-- Absolute value
var abs = absolute value of -5      -- 5

-- Min/Max
var smallest = minimum of 5, 3, 8   -- 3
var largest = maximum of 5, 3, 8    -- 8

-- Random number
var dice = random from 1 to 6
var chance = random                  -- 0.0 to 1.0
```

### Number Functions (Advanced)

```javascript
-- Power and roots
var squared = 5 ^ 2                  -- 25
var cubed = power 2 to 3             -- 8
var root = square root of 16         -- 4

-- Trigonometry
var s = sin of 45 degrees
var c = cos of 45 degrees
var t = tan of 45 degrees

-- Convert angles
var rads = 180 degrees to radians
var degs = 3.14 radians to degrees
```

### List Functions (Beginner)

```javascript
-- Create list
var numbers = list 1, 2, 3, 4, 5

-- Count items
var count = count of numbers         -- 5

-- Get item
var first = numbers at 1             -- 1 (1-based indexing)
var last = numbers at end            -- 5

-- Add item
add 6 to numbers
add 0 to start of numbers

-- Remove item
remove 3 from numbers
remove first from numbers
remove last from numbers

-- Check if contains
var has = numbers contains 3         -- yes/no
```

### List Functions (Advanced)

```javascript
-- Filter list
var evens = filter numbers where item mod 2 = 0

-- Transform list
var doubled = transform numbers with item * 2

-- Sort list
var sorted = sort numbers ascending
var reversed = sort numbers descending

-- Reverse list
var backwards = reverse numbers

-- Join lists
var combined = join numbers with list 6, 7, 8

-- Get subset
var subset = take from 2 to 4 in numbers

-- Find in list
var position = find 5 in numbers
var found = find in numbers where item > 3

-- Sum and average
var total = sum of numbers
var avg = average of numbers
```

### Date and Time (Beginner)

```javascript
-- Get current date/time
var now = today
var time = current time

-- Get parts
var year = year of now
var month = month of now
var day = day of now
var hour = hour of time
var minute = minute of time

-- Format date
var formatted = format now as "YYYY-MM-DD"
var pretty = format now as "Month DD, YYYY"
```

### Date and Time (Advanced)

```javascript
-- Create date
var birthday = date "2015-03-15"
var deadline = date "2025-12-31 23:59:59"

-- Date arithmetic
var tomorrow = now + 1 day
var nextWeek = now + 7 days
var nextMonth = now + 1 month
var nextYear = now + 1 year

-- Date difference
var age = years between birthday and now
var days = days between start and end

-- Compare dates
when now is after deadline then
    show "Overdue!"
end
```

### File Functions (Intermediate)

```javascript
-- Read entire file
var content = read file "data.txt"

-- Read as lines
var lines = read lines from "data.txt"

-- Write to file
write "Hello World" to file "output.txt"

-- Append to file
append "More text" to file "log.txt"

-- Check if exists
var exists = file "data.txt" exists

-- Delete file
delete file "temp.txt"

-- List files in folder
var files = list files in "my_folder"
```

### File Functions (Advanced)

```javascript
-- Read with encoding
var content = read file "data.txt" with encoding "UTF-8"

-- Copy file
copy file "source.txt" to "backup.txt"

-- Move/Rename file
move file "old.txt" to "new.txt"

-- Create folder
create folder "my_folder"

-- Delete folder
delete folder "temp_folder"

-- Get file info
var size = size of file "data.txt"
var modified = last modified date of file "data.txt"
```

## Modules and Imports

### Simple Import (Intermediate)

```javascript
-- Import all from another file
import "helpers.ebs"

-- Now can use functions from helpers.ebs
call functionFromHelpers
```

### Named Imports (Advanced)

```javascript
-- Import specific items
import greet, calculate from "helpers.ebs"

-- Import with rename
import greet as sayHello from "helpers.ebs"

-- Import all with namespace
import helpers from "helpers.ebs"
call helpers.greet with "Alice"
```

### Exports

```javascript
-- helpers.ebs

-- Mark functions for export
export function greet person
    show "Hello " + person
end

export function calculate sum of numbers
    var total = 0
    for each num in numbers
        total = total + num
    end
    give back total
end

-- Private function (not exported)
to helperFunction
    -- only used internally
end
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
**UI Framework:** JavaFX  
**Graphics:** JavaFX Canvas  
**Storage:** Native file system

**Capabilities:**
- Full file system access
- Native performance
- Database connectivity
- Network operations
- Plugin support

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

#### Platform-Specific
- Advanced file operations
- Database connections
- Native integrations
- Performance optimizations
- Platform UI conventions

### Runtime Detection

```javascript
-- Check current runtime
when running on browser then
    show "Running in web browser"
otherwise when running on desktop then
    show "Running as desktop app"
end

-- Conditional features
when has feature "database" then
    -- Use database features
otherwise
    -- Use alternative approach
end
```

## Best Practices

### For Beginners

1. **Use natural language keywords**
   ```javascript
   when age is greater than 12 then
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
   when (age > 12 and age < 20) or isSpecial then
   
   -- Better
   var isTeenager = age > 12 and age < 20
   when isTeenager or isSpecial then
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
   end
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
functions       = "functions" function* "end"
screens         = "screens" screen* "end"
main            = "main" statement* "end"

statement       = varDecl | assignment | ifStmt | loopStmt | 
                  functionCall | return | show | try-catch

varDecl         = "var" identifier ["as" type] ["=" expression]
assignment      = identifier "=" expression
functionCall    = "call" identifier ["with" arguments]

ifStmt          = "when" expression "then" statement* 
                  ["otherwise" statement*] "end"

loopStmt        = repeatLoop | forEachLoop | whileLoop

repeatLoop      = "repeat" (number | expression) "times" 
                  statement* "end"

forEachLoop     = "for" "each" identifier "in" expression
                  statement* "end"

expression      = primary (operator primary)*
primary         = literal | identifier | "(" expression ")" | functionCall
literal         = number | text | boolean | list

type            = "number" | "text" | "yes/no" | "list" | 
                  "list" "of" type | record | map
```

## Appendices

### Appendix A: Complete Keyword List

**Beginner Keywords:** program, end, var, as, is, when, then, otherwise, repeat, times, for, each, in, to, call, with, and, give, back, show, hide, ask, screen, button, label, textbox, list, text, number, yes, no, true, false

**Advanced Keywords:** function, return, try, catch, throw, import, from, export, record, map, json, while, until, break, continue, switch, case, default, const, let, async, await, class, extends, new

### Appendix B: Complete Built-in Function List

See section [Built-in Functions](#built-in-functions) for detailed documentation.

**Categories:**
- Console Output (show, clear, ...)
- User Input (ask, choose, ...)
- Text Operations (length, uppercase, split, ...)
- Number Operations (round, absolute, random, ...)
- List Operations (count, filter, sort, ...)
- Date/Time (today, current time, format, ...)
- File Operations (read, write, exists, ...)
- Screen Operations (show screen, hide screen, ...)

### Appendix C: Migration Checklist

- [ ] Review breaking changes from EBS1
- [ ] Update variable declarations (string → text)
- [ ] Update control structures (if → when)
- [ ] Update function definitions
- [ ] Update array indexing (0-based → 1-based)
- [ ] Update comments (// → --)
- [ ] Organize code into sections
- [ ] Test in both HTML5 and Java runtimes

### Appendix D: Standard Library Organization

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
