# EBS2 Language Requirements and Design Goals

**Version:** 2.0.0-SPEC  
**Date:** December 2025  
**Status:** Specification Phase

## Executive Summary

EBS2 represents a redesign of the EBS (Earl Bosch Script) language, taking learnings from EBS1 and enhancing it to be:
1. A beginner-friendly language suitable for children
2. Cross-platform compatible (HTML5 and Java)
3. Progressively complex (easy features don't become harder to support advanced features)
4. Well-structured and easy to understand

## Core Design Principles

### 1. Simplicity First, Power When Needed

**Principle:** Make simple things simple, complex things possible.

Every feature in EBS2 should have:
- **Basic Form**: Simple, intuitive syntax for common use cases
- **Advanced Form**: Optional parameters and features for complex requirements

#### Language Efficiency: Dual Syntax Support

**EBS2 supports both long form (natural language) and short form (symbolic) syntax**, allowing users to choose based on their skill level and preference:

**Long Form (Natural Language):**
```javascript
// Natural, beginner-friendly
if age is greater than 12 then
    print "Teenager"
end if
```

**Short Form (Symbolic):**
```javascript
// Compact, developer-friendly
if age > 12 then
    print "Teenager"
end if
```

**Key Benefits:**
- **Flexibility**: Users choose what works best for them
- **Progressive Learning**: Start with long form, transition to short form
- **No Forced Simplification**: Experienced developers aren't slowed down
- **Value-Add Changes Only**: Changes from EBS1 only where they add value

### 2. Child-Friendly Language Design

**Target Audience:** Students aged 8-16 as primary users, adults as secondary.

**Design Guidelines:**
- Use natural English words instead of symbols when possible
- Avoid cryptic syntax and special characters
- Provide immediate visual feedback
- Make errors helpful and educational
- Include interactive tutorials in the runtime

**Language Characteristics:**
- **Forgiving Syntax**: Optional semicolons, flexible formatting
- **Case-Insensitive Keywords**: `if`, `IF`, `If` are all the same - reduces errors
- **Flexible Block Syntax**: Choose between `end if` or `{}` - familiar to different backgrounds
- **Clear Keywords**: Natural language like `print`, `repeat` alongside symbolic operators
- **Visual Programming Support**: Block-based editor mode for beginners
- **Helpful Errors**: "You forgot to close the quote" vs "Unexpected token at line 5"

### 3. Dual-Runtime Architecture

**Requirement:** Code must run identically in both HTML5 (browser) and Java (desktop) environments.

**Implications:**
- Core language features must be runtime-agnostic
- Platform-specific features clearly marked
- Automatic transpilation between runtimes
- Consistent behavior across platforms

**Architecture:**
```
    EBS2 Source Code
           ↓
    EBS2 Compiler/Parser
       ↙         ↘
HTML5 Runtime   Java Runtime
(JavaScript)     (JVM)
```

### 4. Well-Defined Structure

**Requirement:** Code structure should be immediately obvious, even to non-programmers.

**Features:**
- Clear program sections (variables, functions, screens, main)
- Visual indentation and block structure
- Mandatory code organization for larger programs
- Template-based project creation

## Detailed Requirements

### Requirement 1: Runtime Compatibility

#### 1.1 HTML5 Runtime
- **Browser Support**: Chrome, Firefox, Safari, Edge (latest versions)
- **Offline Capability**: Progressive Web App (PWA) support
- **File System**: Browser-based file system API for local storage
- **Graphics**: Canvas-based graphics and animations
- **Responsive**: Adapts to mobile, tablet, desktop screens

#### 1.2 Java Runtime
- **JVM Compatibility**: Java 21+
- **Desktop UI**: JavaFX for rich desktop applications
- **File System**: Full native file system access
- **Graphics**: JavaFX-based graphics
- **Performance**: Optimized for computational tasks

#### 1.3 Cross-Platform Features
- **Core Language**: Identical syntax and behavior
- **Standard Library**: Common built-in functions available on both
- **Screen API**: Unified UI component model
- **Data Types**: Consistent type system
- **Error Handling**: Same error types and handling

### Requirement 2: Beginner-Friendly Design

#### 2.1 Natural Language Syntax
```javascript
// Instead of: if (x > 10) { ... }
if x is greater than 10 then
    print "X is large"
end if

// Instead of: function add(a, b) { return a + b; }
to add numbers a and b
    return a + b
end function

// Instead of: for (i = 0; i < 10; i++)
repeat 10 times
    print "Hello"
end repeat
```

#### 2.2 Visual Block Mode
- Scratch-like block editor for beginners
- Converts blocks to text code
- Converts text code to blocks
- Smooth transition from blocks to text

#### 2.3 Interactive Learning
- Built-in tutorial system
- Step-by-step guided lessons
- Instant feedback on code
- Achievement system for motivation
- Example library with explanations

#### 2.4 Helpful Error Messages
```
❌ Bad:  "Syntax error at line 12"

✅ Good: "Line 12: Missing 'end' keyword to close the 'when' block that started on line 8"
```

### Requirement 3: Progressive Complexity

#### 3.1 Layered Feature Design

**Layer 1: Essential (Beginner)**
- Variables with simple types (number, text, yes/no)
- Basic math and text operations
- Simple output (print, show)
- Basic input (ask)
- Simple decisions (when/then)
- Simple loops (repeat)

**Layer 2: Intermediate**
- Functions with parameters
- Lists (arrays)
- Multiple screens/windows
- File reading/writing
- Basic graphics

**Layer 3: Advanced**
- Custom types and records
- Advanced data structures
- Database connectivity
- Network operations
- Advanced graphics and animation
- Multi-threading

#### 3.2 Two-Tier Function Design

Every complex function should have:
1. **Simple version**: Minimal parameters, sensible defaults
2. **Full version**: All parameters exposed

```javascript
// SIMPLE: Read entire file
var content = read file "data.txt"

// ADVANCED: Read with encoding and error handling
var content = read file "data.txt" 
    with encoding "UTF-8" 
    and handle errors gracefully

// SIMPLE: Show message box
print message "Hello!"

// ADVANCED: Show message with title, icon, buttons
print message "Save changes?" 
    titled "Confirm"
    with icon question
    and buttons yes_no_cancel
```

### Requirement 4: Well-Defined Structure

#### 4.1 Program Organization
```javascript
// This is a comment

program HelloWorld

// Section 1: Settings (optional)
settings
    title "My First Program"
    version "1.0"
end

// Section 2: Variables (optional)
variables
    var name as text = "World"
    var count as number = 0
end

// Section 3: Functions (optional)
functions
    to greet someone
        print "Hello " + someone
    end
end

// Section 4: Screens (optional)
screens
    screen MainWindow
        title "Hello App"
        width 400
        height 300
        
        button SayHello at x:10 y:10
            text "Click Me"
            when clicked
                call greet with name
            end screen
        end
    end
end

// Section 5: Main code (required)
main
    print screen MainWindow
end
```

#### 4.2 File Organization
- Single file for small programs (< 200 lines)
- Multiple files for larger projects
- Clear import/module system
- Project templates for different types of apps

## Language Feature Requirements

### Data Types

#### Basic Types (Simple)
- `number` - integers and decimals (auto-selects int/float/double)
- `text` - strings
- `yes/no` - boolean
- `date` - date and time

#### Advanced Types
- `list of <type>` - arrays
- `record` - structured data
- `map` - key-value pairs
- `json` - JSON objects

### Control Structures

#### Simple Forms
```javascript
// Decision
if condition then
    -- code
end if

// Alternative
if condition then
    -- code
else
    -- code
end if

// Repeat fixed times
repeat 10 times
    -- code
end repeat

// Repeat while condition
repeat while condition
    -- code
end repeat

// Repeat for each item
for each item in list
    -- code
end for
```

#### Advanced Forms
```javascript
// Multiple conditions
if condition1 then
    -- code
otherwise if condition2 then
    -- code
otherwise if condition3 then
    -- code
else
    -- code
end if

// Loop with counter
repeat with counter from 1 to 10
    -- counter available as variable
end repeat

// Loop with step
repeat with counter from 0 to 100 by 10
    -- increments by 10
end repeat
```

### Functions

#### Simple Functions
```javascript
to greet
    print "Hello World"
end function

// Call it
call greet
```

#### Functions with Parameters
```javascript
to greet person
    print "Hello " + person
end function

call greet with "Alice"
```

#### Functions with Multiple Parameters
```javascript
to add numbers a and b
    return a + b
end function

var result = call add with a:5 and b:3
```

#### Functions with Return Values
```javascript
to calculate sum of numbers
    var total = 0
    for each num in numbers
        total = total + num
    end for
    return total
end
```

### Screens and UI

#### Simple Window
```javascript
screen MyWindow
    title "Hello"
    
    button ClickMe
        text "Click Me"
        when clicked
            print "Clicked!"
        end screen
    end
end

// Show it
print screen MyWindow
```

#### Advanced Window with Layout
```javascript
screen AdvancedWindow
    title "User Form"
    width 600
    height 400
    
    layout vertical spacing 10
    
    label UserLabel
        text "Enter your name:"
    end screen
    
    textbox NameInput
        placeholder "Your name here"
        max length 50
    end
    
    button SubmitButton
        text "Submit"
        when clicked
            var name = get text from NameInput
            print message "Hello " + name
        end
    end
end
```

### Error Handling

#### Simple (for beginners)
```javascript
// Errors stop the program and print helpful message
var data = read file "missing.txt"
// Auto error: "Cannot find file 'missing.txt' in the current folder"
```

#### Advanced (for experienced users)
```javascript
try
    var data = read file "missing.txt"
catch when file_not_found as error
    print "Could not find file: " + error.message
    -- Use default data
    var data = "Default content"
end try
```

### Built-in Functions (Partial List)

#### Text Operations (Simple)
```javascript
// Combine text
var full = "Hello" + " " + "World"

// Get length
var len = length of "Hello"

// Convert case
var upper = uppercase "hello"
var lower = lowercase "HELLO"

// Find in text
var position = find "lo" in "Hello"
```

#### Text Operations (Advanced)
```javascript
// Replace with options
var result = replace "old" with "new" in text 
    case sensitive
    replace all occurrences

// Split text
var parts = split text by delimiter "," 
    trim whitespace
    skip empty parts

// Format text
var formatted = format "{name} scored {score} points" 
    with name:"Alice" and score:100
```

#### List Operations (Simple)
```javascript
// Create list
var numbers = list 1, 2, 3, 4, 5

// Add to list
add 6 to numbers

// Get item
var first = numbers at 0  -- 0-based indexing (consistent with mainstream languages)

// Get count
var count = count of numbers
```

#### List Operations (Advanced)
```javascript
// Filter list
var evens = filter numbers where item mod 2 equals 0

// Transform list
var doubled = transform numbers with item * 2

// Sort list
var sorted = sort numbers ascending

// Find in list
var found = find in numbers where item > 3
```

#### Screen Operations (Simple)
```javascript
// Show message
print "Hello World"

// Ask for input
var name = ask "What is your name?"

// Show yes/no question
var answer = ask yes or no "Do you want to continue?"
```

#### Screen Operations (Advanced)
```javascript
// Show custom dialog
print dialog "Custom Dialog"
    with message "Choose an option"
    and icon information
    and buttons ok_cancel
    and default button ok
    store result in choice

// Show notification
print notification "Task Complete"
    for duration 3 seconds
    with style success
    at position bottom_right
```

## Implementation Priorities

### Phase 1: Core Language (Months 1-3)
1. Design and document complete syntax
2. Implement lexer/parser for EBS2
3. Build AST representation
4. Create basic interpreter
5. Implement core data types
6. Build standard library (simple forms)

### Phase 2: HTML5 Runtime (Months 4-6)
1. Transpile EBS2 to JavaScript
2. Implement browser runtime
3. Build HTML5 UI components
4. Create visual block editor
5. Implement interactive tutorial system
6. Build online playground

### Phase 3: Java Runtime (Months 7-8)
1. Update Java interpreter for EBS2
2. Maintain JavaFX UI compatibility
3. Optimize for desktop performance
4. Implement native file operations
5. Build desktop IDE

### Phase 4: Advanced Features (Months 9-11)
1. Add advanced built-in functions
2. Implement database connectivity
3. Add network operations
4. Build graphics libraries
5. Add plugin system

### Phase 5: Tooling and Polish (Month 12)
1. Create comprehensive documentation
2. Build debugging tools
3. Implement code formatter
4. Create example library
5. Build community site

## Success Criteria

### For Children (Primary Users)
- [ ] 8-year-old can create a simple program after 30-minute tutorial
- [ ] 10-year-old can create interactive game within 2 hours
- [ ] 12-year-old can build multi-screen application in a weekend
- [ ] Error messages are understandable without adult help
- [ ] Visual block mode works smoothly for beginners

### For Runtime Compatibility
- [ ] 100% core feature parity between HTML5 and Java
- [ ] Same code produces identical results on both runtimes
- [ ] Performance acceptable on both platforms
- [ ] Easy deployment to both targets

### For Progressive Complexity
- [ ] Simple forms work without understanding advanced features
- [ ] Advanced features don't break simple code
- [ ] Clear upgrade path from simple to advanced
- [ ] Documentation separates beginner/intermediate/advanced

### For Structure
- [ ] Code structure is self-documenting
- [ ] Large programs are naturally organized
- [ ] Templates make starting new projects easy
- [ ] File organization is obvious

## Backward Compatibility

### EBS1 Compatibility Mode
- EBS2 runtime can execute EBS1 code with compatibility flag
- Migration tool converts EBS1 to EBS2 syntax
- Side-by-side execution during transition
- Clear migration guide

### Breaking Changes from EBS1
1. **Syntax**: More natural language keywords available
2. **Structure**: Recommended program organization (optional)
3. **Types**: Simplified type names (text vs string, number vs int/float)
4. **Comments**: Only `//` supported (single-line, block comments not supported)
5. **Operators**: Added `++` and `--` for increment/decrement
6. **Functions**: More natural definition syntax available

## Open Questions

1. **Block Editor**: Should we use existing framework (Blockly) or build custom?
2. **Type System**: Strong typing vs dynamic typing for beginners?
3. **Natural Language**: How natural is optimal? (e.g., "is greater than" vs ">")
4. **Keywords**: Use familiar keywords (if, else, return) or more natural ones?
5. **Performance**: Acceptable performance tradeoffs for simplicity?

## References

- EBS1 Language Reference: `EBS_LANGUAGE_REFERENCE.md`
- EBS1 Syntax: `docs/EBS_SCRIPT_SYNTAX.md`
- Architecture Documentation: `ARCHITECTURE.md`
- Scratch Programming Language: https://scratch.mit.edu/
- Python Turtle Graphics: https://docs.python.org/3/library/turtle.html
- Processing Language: https://processing.org/

## Approval and Sign-off

This requirements document should be reviewed and approved by:
- [ ] Project Sponsor
- [ ] Technical Lead
- [ ] Education Specialist (for child-friendly aspects)
- [ ] Community Representatives

---

**Document Status:** DRAFT - Awaiting Review  
**Next Steps:** Create detailed language specification (EBS2_LANGUAGE_SPEC.md)
