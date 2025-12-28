# EBS2 Quick Start Guide for Beginners

**Welcome to EBS2!** 🎉

This guide will help you write your first programs in just a few minutes.

## Table of Contents

1. [Your First Program](#your-first-program)
2. [Understanding Programs](#understanding-programs)
3. [Variables - Storing Information](#variables---storing-information)
4. [Making Decisions](#making-decisions)
5. [Repeating Things](#repeating-things)
6. [Creating Functions](#creating-functions)
7. [Making Windows](#making-windows)
8. [What's Next](#whats-next)

## Your First Program

Let's start with the simplest program possible:

```javascript
program HelloWorld

main
    print "Hello World!"
end main
```

**What does this do?**
- `program HelloWorld` - Names your program
- `main` - Where your program starts
- `print "Hello World!"` - Displays text on screen
- `end` - Marks the end of main

**Try it yourself:**
1. Type the code above
2. Click "Run" (or press Ctrl+Enter)
3. You should see "Hello World!" appear!

## Understanding Programs

Every EBS2 program has the same basic structure:

```javascript
program MyProgram

// This is a comment (notes for yourself)
// The computer ignores comments

main
    // Your code goes here
    print "This is where I write instructions"
end main
```

**Parts of a program:**
- **program** - Gives your program a name
- **main** - The starting point (required)
- **//** - Comments (notes that the computer skips)
- **end** - Closes a section

**Bonus Tip:** Keywords like `program`, `main`, `print`, `if`, `end` can be written in ANY CASE:
```javascript
PROGRAM MyProgram    // Same as 'program MyProgram'
Main                 // Same as 'main'
    PRINT "Hello"    // Same as 'print "Hello"'
End                  // Same as 'end'
```
All work the same! Write them however you prefer.

## Variables - Storing Information

Variables are like labeled boxes where you store information:

### Storing Text

```javascript
program GreetFriend

main
    var name as text = "Sarah"
    print "Hello " + name
end main
```

**Output:** `Hello Sarah`

### Storing Numbers

```javascript
program CountApples

main
    var apples as number = 5
    var oranges as number = 3
    var total = apples + oranges
    
    print "Total fruit: " + total
end main
```

**Output:** `Total fruit: 8`

### Numbers with Ranges (Advanced)

You can limit numbers to a specific range:

```javascript
program GameScore

main
    // Score must be between 0 and 100
    var score as number 0..100 = 85
    
    // Temperature can have decimals
    var temperature as number -50.0..50.0 = 22.5
    
    print "Score: " + score
    print "Temperature: " + temperature
end main
```

**Benefits:**
- Computer checks that numbers stay in range
- Whole number ranges (like `0..100`) use less memory
- Decimal ranges (like `-1.0..1.0`) allow decimals

### Storing True/False (Flags)

```javascript
program CheckWeather

main
    var isSunny as flag = true
    var isRaining as flag = false
    
    print "Is it sunny? " + isSunny
    print "Is it raining? " + isRaining
end main
```

**Remember:**
- Use `var` or `variable` to create a variable (both work the same)
- Use `const` or `constant` for values that never change
- Use `as text` for words
- Use `as number` for numbers
- Use `as flag` for true/false

### Constants - Values That Never Change

Sometimes you want to store a value that should NEVER change. Use `const` or `constant`:

```javascript
program GameSettings

main
    const MAX_PLAYERS as number = 4
    constant GAME_NAME as text = "Super Fun Game"
    
    print "Game: " + GAME_NAME
    print "Max players: " + MAX_PLAYERS
    
    // MAX_PLAYERS = 10  // ERROR! Cannot change a constant
end main
```

**When to use constants:**
- Game settings that don't change (like max score)
- Mathematical values (like PI = 3.14)
- Configuration values (like app name)
- Anything that should stay the same throughout your program

## Making Decisions

Sometimes you want to do different things based on conditions:

### Simple Decision

```javascript
program CheckAge

main
    var age as number = 12
    
    if age is greater than 12 then
        print "You're a teenager!"
    end if
end main
```

### Decision with Otherwise

```javascript
program CheckAge2

main
    var age as number = 10
    
    if age is greater than 12 then
        print "You're a teenager!"
    else
        print "You're a child!"
    end if
end main
```

### Multiple Choices

```javascript
program GradingSystem

main
    var score as number = 85
    
    if score >= 90 then
        print "Grade: A"
    else if score >= 80 then
        print "Grade: B"
    else if score >= 70 then
        print "Grade: C"
    else
        print "Grade: D"
    end if
end main
```

**Tip:** You can also use shortcuts like `>` instead of `is greater than`:
- `>` means "greater than"
- `<` means "less than"
- `>=` means "greater than or equal to"
- `<=` means "less than or equal to"
- `=` means "equal to"

### Short Form (One Line)

For simple checks, you can write everything on one line. The `end` keyword is **optional** for single-command blocks:

```javascript
program QuickCheck

main
    var age as number = 8
    
    // One line - no 'end if' needed (optional)!
    if age < 5 then print "Preschooler"
    if age >= 5 then print "School age"
    
    // Multiple commands on one line with semicolon - no 'end if' needed
    if age < 10 then print "Young"; log "Age checked"
end main
```

**When to use:**
- **One line (no end)**: Simple, single command
- **Multiple lines (with end)**: Complex logic or multiple commands

## Repeating Things

### Repeat a Fixed Number of Times

```javascript
program CountToTen

main
    repeat 10 times with counter
        print counter
    end repeat
end main
```

**Output:** Numbers 1 through 10

### Repeat with a List

```javascript
program FavoriteFruits

main
    var fruits as array = "apple", "banana", "cherry"
    
    for each fruit in fruits
        print "I like " + fruit
    end for
end main
```

**Output:**
```
I like apple
I like banana
I like cherry
```

### Repeat While Something is True

```javascript
program CountDown

main
    var count as number = 5
    
    repeat while count > 0
        print count
        count = count - 1
    end repeat
    
    print "Blast off!"
end main
```

**Output:** 5, 4, 3, 2, 1, Blast off!

## Working with Lists

Lists let you store multiple values together:

### Creating Lists

```javascript
program ArrayExample

main
    // Simple array with colon
    var numbers as array = 1, 2, 3, 4, 5
    
    // Explicit values
    var fruits as array = "apple", "banana", "cherry"
    
    // Range syntax (creates all numbers from 1 to 10)
    var oneToTen as array = 1..10
    
    // Large range (creates 1 to 100)
    var oneToHundred as array = 1..100
    
    // Arbitrary ranges
    var subset as array = 43..79      // Creates [43, 44, 45, ..., 79]
    
    // Negative ranges
    var negatives as array = -10..10  // Creates [-10, -9, ..., 0, 1, ..., 10]
    
    // Get items from array (starts at 0!)
    print numbers[0]  // Prints: 1 (first item)
    print numbers[1]  // Prints: 2 (second item)
    print fruits[0]   // Prints: apple (first fruit)
end main
```

**Remember:** Lists start counting from 0, just like most programming languages!
- `numbers[0]` = first item
- `numbers[1]` = second item
- `numbers[2]` = third item

### Indicators - Choosing from a Set

Sometimes you want a variable that can only be specific values (like traffic light colors). Use `indicator`:

```javascript
program TrafficLight

main
    // Indicator can only be "red", "yellow", or "green"
    var lightColor as indicator "red", "yellow", "green"
    
    // Set the value
    lightColor = "red"
    print "Light is: " + lightColor
    
    // Change the value (must be one of the allowed values)
    lightColor = "green"
    print "Light is now: " + lightColor
    
    // This would be an ERROR:
    // lightColor = "blue"  // ERROR! "blue" is not allowed
end main
```

**Indicator vs List:**
- **List**: Array of many values accessed by index like `myList[0]`, `myList[1]`
- **Indicator**: Single value that can only be one from a specific set

**Common uses for indicators:**
- Status: `var status as indicator "pending", "active", "complete"`
- Direction: `var direction as indicator "north", "south", "east", "west"`
- Grade: `var grade as indicator "A", "B", "C", "D", "F"`

## Creating Functions

Functions let you organize and reuse code. EBS2 has two types:

### Procedures (No Return Value)

**Procedures** perform actions but don't return a value:

```javascript
program SayHello

// Procedure - just does something
procedure greet() {
    print "Hello there!"
}

main
    greet()  // Call it
    greet()  // Call it again
    greet()  // Call it one more time
end main
```

**Output:** "Hello there!" appears 3 times

### Procedure with Input

```javascript
program PersonalGreeting

// Procedure with a parameter
procedure greet(person as text) {
    print "Hello " + person + "!"
}

main
    greet("Alice")
    greet("Bob")
    greet("Charlie")
end main
```

**Output:**
```
Hello Alice!
Hello Bob!
Hello Charlie!
```

### Functions (Return Value)

**Functions** calculate and return a value:

```javascript
program Calculator

// Function - returns a value
function add(a as number, b as number) as number {
    return a + b
}

main
    var result = add(5, 3)
    print "5 + 3 = " + result
end main
```

**Output:** `5 + 3 = 8`

**Key Point:**
- Use **procedure** when you just want to do something (like print or update)
- Use **function** when you need to calculate and return a value

## Making Windows

Create interactive windows with buttons and text boxes:

### Simple Window

```javascript
program SimpleWindow

screen HelloScreen
    title "My First Window"
    
    label WelcomeText
        text "Welcome to EBS2!"
        size large
    end label
    
    button OkButton
        text "OK"
        if clicked
            print "You clicked OK!"
        end when
    end button
end screen

main
    print screen HelloScreen
end main
```

### Interactive Input

```javascript
program AskName

screen InputScreen
    title "What's Your Name?"
    
    label PromptLabel
        text "Please enter your name:"
    end label
    
    textbox NameBox
        placeholder "Type your name here"
    end textbox
    
    button SubmitButton
        text "Submit"
        if clicked
            var name = get text from NameBox
            print "Hello " + name + "!"
        end when
    end button
end screen

main
    print screen InputScreen
end main
```

### Counter Button

```javascript
program CounterApp

variables
    var count as number = 0
end variables

screen CounterScreen
    title "Click Counter"
    
    label CountLabel
        text "Clicks: 0"
    end label
    
    button ClickButton
        text "Click Me!"
        if clicked
            count = count + 1
            update CountLabel text "Clicks: " + count
        end when
    end button
    
    button ResetButton
        text "Reset"
        if clicked
            count = 0
            update CountLabel text "Clicks: 0"
        end when
    end button
end screen

main
    print screen CounterScreen
end main
```

### 💡 Tip: Using print vs log

**print** - Shows output to the user (displays in your program's window)
```javascript
print "Hello, User!"  // Visible to everyone using your program
```

**log** - Writes debug messages (only visible in Debug View)
```javascript
log "Debug: count value is", count  // Only visible to you while debugging
```

Use `log` to help find problems in your code without cluttering the user's screen!

## Advanced Syntax: Curly Braces

As you get more comfortable with EBS2, you might want to use **curly braces** `{}` instead of `end if`, `end repeat`, etc. This style is popular in many programming languages like JavaScript, Java, and C++.

### Using Curly Braces for If Statements

```javascript
program CurlyBraceExample

main
    var age = 15
    
    // With curly braces (instead of end if)
    if age > 12 {
        print "You're a teenager!"
        log "Age verified"
    } else {
        print "You're a child!"
    }
end main
```

### Using Curly Braces for Loops

```javascript
program LoopWithBraces

main
    // Repeat with braces
    repeat 5 times {
        print "Hello"
    }
    
    // For loop with braces
    var fruits as array = "apple", "banana", "cherry"
    for each fruit in fruits {
        print fruit
    }
end main
```

### Using Curly Braces for Functions

```javascript
program FunctionWithBraces

functions
    function greet(name) {
        print "Hello " + name
        return "Greeting sent"
    }
end function

main
    var result = greet("Alice")
end main
```

### Variable Scope with Curly Braces

**Important:** When you use curly braces `{}`, variables created inside them **only exist inside those braces**:

```javascript
program ScopeExample

main
    var x = 10           // Exists everywhere in main
    
    if x > 5 {
        var y = 20       // Only exists inside these braces
        print x          // Works: prints 10
        print y          // Works: prints 20
    }
    
    print x              // Still works: prints 10
    print y              // ERROR: y doesn't exist here!
end main
```

### Which Style Should You Use?

**Use `end if`, `end repeat`, etc. when:**
- You're just learning programming
- You want code that's easy to read
- You're teaching others

**Use curly braces `{}` when:**
- You know other programming languages (JavaScript, Java, C++)
- You prefer compact code
- You're working with experienced developers

**Best practice:** Pick one style and stick with it in each project. Both work great!

## What's Next

### Easy Projects to Try

1. **Number Guessing Game**
   - Computer picks a random number
   - You guess until you get it right
   - Shows if guess is too high or low

2. **To-Do List**
   - Add tasks to a list
   - Mark tasks as complete
   - Delete completed tasks

3. **Simple Calculator**
   - Two number inputs
   - Buttons for +, -, ×, ÷
   - Shows the result

4. **Story Adventure**
   - Make choices that affect the story
   - Different endings based on choices
   - Use variables to track decisions

### Learning Resources

- **Tutorial Series**: Work through step-by-step lessons
- **Example Gallery**: See what others have made
- **Help System**: Press F1 for help on any keyword
- **Community**: Ask questions in the forum

### Tips for Success

1. **Start Small**: Begin with simple programs
2. **Experiment**: Try changing things to see what happens
3. **Read Errors**: Error messages help you fix problems
4. **Save Often**: Save your work regularly
5. **Ask for Help**: Don't be afraid to ask questions

### Common Mistakes (and how to fix them)

**Forgot `end` keyword:**
```javascript
// Wrong
if age > 12 then
    print "Teenager"

// Right
if age > 12 then
    print "Teenager"
end if
```

**Forgot quotes around text:**
```javascript
// Wrong
print Hello World

// Right
print "Hello World"
```

**Wrong variable type:**
```javascript
// Wrong
var age as text = 12

// Right
var age as number = 12
```

### Quick Reference Card

#### Variables
```javascript
var name as text = "Alice"
var age as number = 10
var ready as yes/no = yes
```

#### Decisions
```javascript
if condition then
    // do something
else
    // do something else
end if
```

#### Loops
```javascript
repeat 5 times
    // do something
end repeat

for each item in array
    // do something with item
end for
```

#### Functions
```javascript
to functionName parameter
    // do something
    return result
end function
```

#### Screens
```javascript
screen MyScreen
    title "My Window"
    
    button MyButton
        text "Click"
        if clicked
            // do something
        end when
    end button
end screen
```

### Practice Exercises

#### Exercise 1: Personal Introduction
Write a program that:
- Stores your name in a variable
- Stores your age in a variable
- Displays a sentence using both

#### Exercise 2: Odd or Even
Write a program that:
- Has a number variable
- Checks if it's odd or even
- Displays the result

#### Exercise 3: Times Table
Write a program that:
- Shows the 5 times table
- From 5×1 to 5×10
- Uses a loop

#### Exercise 4: Simple Quiz
Write a program that:
- Asks a question
- Checks the answer
- Says if correct or wrong

---

## Congratulations! 🎊

You've learned the basics of EBS2! You now know how to:
- ✓ Write simple programs
- ✓ Store information in variables
- ✓ Make decisions
- ✓ Repeat actions
- ✓ Create functions
- ✓ Build interactive windows

**Keep practicing and have fun coding!**

---

**Need More Help?**
- Press F1 for built-in help
- Visit the tutorial section
- Check the example gallery
- Ask in the community forum

**Remember:** Everyone starts as a beginner. The more you practice, the better you'll get! 🚀
