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
    show "Hello World!"
end
```

**What does this do?**
- `program HelloWorld` - Names your program
- `main` - Where your program starts
- `show "Hello World!"` - Displays text on screen
- `end` - Marks the end of main

**Try it yourself:**
1. Type the code above
2. Click "Run" (or press Ctrl+Enter)
3. You should see "Hello World!" appear!

## Understanding Programs

Every EBS2 program has the same basic structure:

```javascript
program MyProgram

-- This is a comment (notes for yourself)
-- The computer ignores comments

main
    -- Your code goes here
    show "This is where I write instructions"
end
```

**Parts of a program:**
- **program** - Gives your program a name
- **main** - The starting point (required)
- **--** - Comments (notes that the computer skips)
- **end** - Closes a section

## Variables - Storing Information

Variables are like labeled boxes where you store information:

### Storing Text

```javascript
program GreetFriend

main
    var name as text = "Sarah"
    show "Hello " + name
end
```

**Output:** `Hello Sarah`

### Storing Numbers

```javascript
program CountApples

main
    var apples as number = 5
    var oranges as number = 3
    var total = apples + oranges
    
    show "Total fruit: " + total
end
```

**Output:** `Total fruit: 8`

### Storing Yes/No

```javascript
program CheckWeather

main
    var isSunny as yes/no = yes
    var isRaining as yes/no = no
    
    show "Is it sunny? " + isSunny
    show "Is it raining? " + isRaining
end
```

**Remember:**
- Use `var` to create a variable
- Use `as text` for words
- Use `as number` for numbers
- Use `as yes/no` for true/false

## Making Decisions

Sometimes you want to do different things based on conditions:

### Simple Decision

```javascript
program CheckAge

main
    var age as number = 12
    
    when age is greater than 12 then
        show "You're a teenager!"
    end
end
```

### Decision with Otherwise

```javascript
program CheckAge2

main
    var age as number = 10
    
    when age is greater than 12 then
        show "You're a teenager!"
    otherwise
        show "You're a child!"
    end
end
```

### Multiple Choices

```javascript
program GradingSystem

main
    var score as number = 85
    
    when score >= 90 then
        show "Grade: A"
    otherwise when score >= 80 then
        show "Grade: B"
    otherwise when score >= 70 then
        show "Grade: C"
    otherwise
        show "Grade: D"
    end
end
```

**Tip:** You can also use shortcuts like `>` instead of `is greater than`:
- `>` means "greater than"
- `<` means "less than"
- `>=` means "greater than or equal to"
- `<=` means "less than or equal to"
- `=` means "equal to"

## Repeating Things

### Repeat a Fixed Number of Times

```javascript
program CountToTen

main
    repeat 10 times with counter
        show counter
    end
end
```

**Output:** Numbers 1 through 10

### Repeat with a List

```javascript
program FavoriteFruits

main
    var fruits = list "apple", "banana", "cherry"
    
    for each fruit in fruits
        show "I like " + fruit
    end
end
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
        show count
        count = count - 1
    end
    
    show "Blast off!"
end
```

**Output:** 5, 4, 3, 2, 1, Blast off!

## Creating Functions

Functions are reusable blocks of code:

### Simple Function

```javascript
program SayHello

to greet
    show "Hello there!"
end

main
    call greet
    call greet
    call greet
end
```

**Output:** "Hello there!" appears 3 times

### Function with Input

```javascript
program PersonalGreeting

to greet person
    show "Hello " + person + "!"
end

main
    call greet with "Alice"
    call greet with "Bob"
    call greet with "Charlie"
end
```

**Output:**
```
Hello Alice!
Hello Bob!
Hello Charlie!
```

### Function that Returns a Value

```javascript
program Calculator

to add numbers a and b
    give back a + b
end

main
    var result = call add with a:5 and b:3
    show "5 + 3 = " + result
end
```

**Output:** `5 + 3 = 8`

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
    end
    
    button OkButton
        text "OK"
        when clicked
            show "You clicked OK!"
        end
    end
end

main
    show screen HelloScreen
end
```

### Interactive Input

```javascript
program AskName

screen InputScreen
    title "What's Your Name?"
    
    label PromptLabel
        text "Please enter your name:"
    end
    
    textbox NameBox
        placeholder "Type your name here"
    end
    
    button SubmitButton
        text "Submit"
        when clicked
            var name = get text from NameBox
            show "Hello " + name + "!"
        end
    end
end

main
    show screen InputScreen
end
```

### Counter Button

```javascript
program CounterApp

variables
    var count as number = 0
end

screen CounterScreen
    title "Click Counter"
    
    label CountLabel
        text "Clicks: 0"
    end
    
    button ClickButton
        text "Click Me!"
        when clicked
            count = count + 1
            update CountLabel text "Clicks: " + count
        end
    end
    
    button ResetButton
        text "Reset"
        when clicked
            count = 0
            update CountLabel text "Clicks: 0"
        end
    end
end

main
    show screen CounterScreen
end
```

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
-- Wrong
when age > 12 then
    show "Teenager"

-- Right
when age > 12 then
    show "Teenager"
end
```

**Forgot quotes around text:**
```javascript
-- Wrong
show Hello World

-- Right
show "Hello World"
```

**Wrong variable type:**
```javascript
-- Wrong
var age as text = 12

-- Right
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
when condition then
    -- do something
otherwise
    -- do something else
end
```

#### Loops
```javascript
repeat 5 times
    -- do something
end

for each item in list
    -- do something with item
end
```

#### Functions
```javascript
to functionName parameter
    -- do something
    give back result
end
```

#### Screens
```javascript
screen MyScreen
    title "My Window"
    
    button MyButton
        text "Click"
        when clicked
            -- do something
        end
    end
end
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
