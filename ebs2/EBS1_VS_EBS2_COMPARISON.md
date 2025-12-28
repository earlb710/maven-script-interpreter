# EBS1 vs EBS2 Comparison Guide

**Version:** 2.0.0-COMPARISON  
**Date:** December 2025  
**Status:** Planning Phase

## Table of Contents

1. [Overview](#overview)
2. [Philosophy and Design Goals](#philosophy-and-design-goals)
3. [Syntax Comparison](#syntax-comparison)
4. [Feature Comparison](#feature-comparison)
5. [Migration Guide](#migration-guide)
6. [Decision Matrix](#decision-matrix)

## Overview

### What is EBS1?

EBS1 (Earl Bosch Script) is the current scripting language featuring:
- Familiar C/JavaScript-like syntax
- Strong type system with explicit declarations
- JavaFX-based desktop IDE
- Database integration
- Rich UI capabilities

### What is EBS2?

EBS2 is a redesigned version of the language with focus on:
- Natural language syntax for beginners (especially children)
- Cross-platform execution (HTML5 + Java)
- Progressive complexity (simple → advanced)
- Well-defined program structure

### Key Differences Summary

| Aspect | EBS1 | EBS2 |
|--------|------|------|
| **Target Audience** | General developers | Children first, developers second |
| **Syntax Style** | C-like, symbolic | Dual: Natural + Symbolic |
| **Platforms** | Java/JavaFX desktop | HTML5 browser + Java desktop |
| **Learning Curve** | Moderate (familiar to programmers) | Gentle (intuitive for children) |
| **Structure** | Free-form | Organized sections |
| **Type Names** | `string`, `int`, `float` | `text`, `number` |
| **Indexing** | 0-based arrays | 0-based arrays (consistent) |
| **Comments** | `//` and `/* */` | `//` and `/* */` |
| **Block Syntax** | Multi-line only | Single-line or multi-line |
| **JavaFX Output** | TextFlow/TextArea | WebView (HTML rendering) |

## Philosophy and Design Goals

### EBS1 Philosophy

**"Familiar and Powerful"**

EBS1 aimed to provide:
- Syntax familiar to C/Java/JavaScript developers
- Strong typing with flexibility
- Rich desktop application capabilities
- Database integration for business applications
- Professional development experience

**Design Principles:**
1. Leverage existing programming knowledge
2. Type safety with explicit declarations
3. Desktop-first with JavaFX
4. Database-driven applications
5. Professional tooling

### EBS2 Philosophy

**"Simple First, Powerful When Needed"**

EBS2 aims to provide:
- Natural language that reads like English
- Intuitive for complete beginners (especially children)
- Cross-platform reach (web and desktop)
- Progressive complexity (grow with the user)
- Visual block programming for beginners

**Design Principles:**
1. Children can understand without prior knowledge
2. Natural language over symbols
3. Cross-platform from day one
4. Simple things stay simple
5. Learning-focused tooling

## Syntax Comparison

### Variables

#### EBS1
```javascript
// Explicit type declarations
var name: string = "Alice";
var age: int = 10;
var score: float = 95.5;
var active: bool = true;

// Type inference
var count = 42;  // inferred as int
```

#### EBS2
```javascript
// Simpler type names
var name as text = "Alice"
var age as number = 10
var score as number = 95.5
var active as yes/no = yes

// Type inference (same)
var count = 42
```

**Key Differences:**
- Type separator: `:` → `as`
- Type names: `string` → `text`, `int/float/double` → `number`, `bool` → `yes/no`
- Boolean values: `true/false` → `yes/no` (both supported)

### Control Flow

#### If/When Statements

**EBS1:**
```javascript
if age > 12 then {
    print "Teenager";
} else if age > 5 then {
    print "Child";
} else {
    print "Preschooler";
}
```

**EBS2 (Natural):**
```javascript
if age is greater than 12 then
    print "Teenager"
otherwise if age is greater than 5 then
    print "Child"
else
    print "Preschooler"
end if
```

**EBS2 (Symbolic - Advanced):**
```javascript
if age > 12 then
    print "Teenager"
else if age > 5 then
    print "Child"
else
    print "Preschooler"
end if
```

**Key Differences:**
- EBS2 offers natural language version
- EBS1 requires braces `{}`, EBS2 uses `end` keyword
- EBS2 `otherwise` vs EBS1 `else`

#### Loops

**EBS1:**
```javascript
// Repeat n times
var count = 0;
while count < 10 {
    print count;
    count = count + 1;
}

// For each
var fruits = ["apple", "banana", "cherry"];
for fruit in fruits {
    print fruit;
}
```

**EBS2 (Simple):**
```javascript
// Repeat n times
repeat 10 times with counter
    show counter
end repeat

// For each
var fruits = list "apple", "banana", "cherry"
for each fruit in fruits
    show fruit
end for
```

**Key Differences:**
- EBS2 has dedicated `repeat` for simple counting
- No explicit counter management in EBS2
- Natural language: `for each ... in`

### Functions

#### EBS1
```javascript
greet(name: string) return string {
    return "Hello, " + name + "!";
}

var message = call greet("Alice");
print message;
```

#### EBS2
```javascript
to greet person
    give back "Hello, " + person + "!"
end function

var message = call greet with "Alice"
show message
```

**Key Differences:**
- EBS2: Natural definition `to greet person` vs `greet(name: string)`
- EBS2: `give back` vs `return`
- EBS2: `call ... with` vs `call()`
- EBS2: `show` vs `print`

### Arrays/Lists

#### EBS1
```javascript
// Fixed-size array
var numbers: int[5];
numbers[0] = 1;
numbers[1] = 2;

// Dynamic array
var fruits = ["apple", "banana"];
print fruits[0];  // "apple"
print fruits.length;  // 2
```

#### EBS2
```javascript
// List with simple syntax
var numbers = list 1, 2, 3, 4, 5
show numbers at 1  -- "1" (1-based!)

// Dynamic list
var fruits = list "apple", "banana"
show fruits at 1  -- "apple"
show count of fruits  -- 2
```

**Key Differences:**
- EBS1: 0-based indexing, EBS2: 1-based indexing (more natural for children)
- EBS2: `list` keyword for creation
- EBS2: `at` keyword for access (also supports `[1]`)
- EBS2: `count of` vs `.length`

### Screens/UI

#### EBS1
```javascript
screen myWindow = {
    "title": "My App",
    "width": 800,
    "height": 600,
    "vars": [{
        "name": "counter",
        "type": "int",
        "default": 0,
        "display": {
            "type": "button",
            "labelText": "Click Me",
            "onClick": "incrementCounter"
        }
    }]
};

show screen myWindow;
```

#### EBS2
```javascript
screen MyWindow
    title "My App"
    width 800
    height 600
    
    button ClickMe
        text "Click Me"
        when clicked
            counter = counter + 1
        end screen
    end
end

show screen MyWindow
```

**Key Differences:**
- EBS2: Structured syntax vs JSON
- EBS2: Inline event handlers vs separate functions
- EBS2: More readable, less quoting
- EBS2: Clear hierarchy with indentation

## Feature Comparison

### Language Features

| Feature | EBS1 | EBS2 | Notes |
|---------|------|------|-------|
| **Variables** | ✓ | ✓ | EBS2 simpler type names |
| **Type Inference** | ✓ | ✓ | Both support |
| **Arrays** | ✓ | ✓ | EBS2 uses 1-based indexing |
| **Records** | ✓ | ✓ | Similar in both |
| **JSON** | ✓ | ✓ | Both support |
| **Functions** | ✓ | ✓ | EBS2 more natural syntax |
| **Classes** | ✗ | ✓ | EBS2 adds (optional, advanced) |
| **Async/Await** | ✗ | ✓ | EBS2 adds |
| **Pattern Matching** | ✗ | ✓ | EBS2 adds |
| **Exception Handling** | ✓ | ✓ | Both support |
| **Imports** | ✓ | ✓ | Similar in both |

### Built-in Functions

| Category | EBS1 | EBS2 | Notes |
|----------|------|------|-------|
| **String Functions** | 30+ | 40+ | EBS2 adds more + simple variants |
| **Math Functions** | 20+ | 25+ | EBS2 adds more |
| **Array Functions** | 15+ | 30+ | EBS2 more comprehensive |
| **File I/O** | ✓ | ✓ | Similar |
| **JSON** | ✓ | ✓ | Similar |
| **Date/Time** | ✓ | ✓ | Similar |
| **Database** | ✓ Oracle | ✓ SQLite built-in | EBS2 simpler DB access |
| **HTTP** | ✗ | ✓ | EBS2 adds |
| **Graphics** | Basic | Advanced | EBS2 full 2D graphics library |

### Runtime and Tooling

| Feature | EBS1 | EBS2 |
|---------|------|------|
| **Desktop Runtime** | ✓ JavaFX | ✓ JavaFX |
| **Web Runtime** | ✗ | ✓ HTML5 |
| **Mobile Support** | ✗ | ✓ (via web) |
| **IDE** | ✓ JavaFX IDE | ✓ Desktop + Web |
| **Block Editor** | ✗ | ✓ |
| **Debugger** | Basic | Enhanced |
| **Package Manager** | ✗ | ✓ |
| **Plugin System** | Limited | ✓ Full |
| **Testing Framework** | ✗ | ✓ |
| **Project Templates** | ✗ | ✓ |

### Platform Support

| Platform | EBS1 | EBS2 |
|----------|------|------|
| **Windows Desktop** | ✓ | ✓ |
| **Mac Desktop** | ✓ | ✓ |
| **Linux Desktop** | ✓ | ✓ |
| **Web Browser** | ✗ | ✓ |
| **Mobile (via web)** | ✗ | ✓ |
| **Offline Support** | ✓ | ✓ (both platforms) |

## Migration Guide

### Simple Migration Path

For basic programs, migration is straightforward:

1. **Update Types**
   - `string` → `text`
   - `int`, `float`, `double` → `number`
   - `bool` → `yes/no` (or keep `bool`)

2. **Update Syntax**
   - `var x: type` → `var x as type`
   - `{ }` → `end`

3. **Update Array Indexing**
   - Subtract 1 from all array indices (EBS2 uses 1-based)
   - Or use compatibility mode for 0-based

4. **Optional Changes**
   - `return` → `give back` (for beginner-friendly code)
   - `print` → `show` (for consistency)

### Automated Migration Tool

```bash
# Migrate EBS1 to EBS2
ebs2-migrate convert myapp.ebs --output myapp-v2.ebs

# Options
--zero-based       # Keep 0-based array indexing
--minimal          # Minimal changes only
--preserve-style   # Keep original code style where possible
```

### Migration Example

**EBS1 Code:**
```javascript
// Calculate average score
averageScore(scores: int[]) return float {
    var total: int = 0;
    var count: int = scores.length;
    
    for score in scores {
        total = total + score;
    }
    
    if count > 0 then {
        return total / count;
    } else {
        return 0.0;
    }
}

var results = [85, 90, 78, 92, 88];
var avg = call averageScore(results);
print "Average: " + avg;
```

**EBS2 Code (Natural):**
```javascript
// Calculate average score
to averageScore scores
    var total as number = 0
    var count as number = count of scores
    
    for each score in scores
        total = total + score
    end for
    
    if count is greater than 0 then
        give back total / count
    otherwise
        give back 0
    end if
end

var results = list 85, 90, 78, 92, 88
var avg = call averageScore with results
print "Average: " + avg
```

**EBS2 Code (Symbolic, Advanced):**
```javascript
// Calculate average score
function averageScore(scores)
    var total = 0
    var count = count of scores
    
    for each score in scores
        total = total + score
    end for
    
    if count > 0 then
        return total / count
    else
        return 0
    end if
end

var results = list 85, 90, 78, 92, 88
var avg = averageScore(results)
print "Average: " + avg
```

### Compatibility Mode

EBS2 includes a compatibility mode for running EBS1 code:

```bash
# Run EBS1 code in EBS2
ebs2 run --compat=ebs1 myapp.ebs

# Convert project incrementally
ebs2 run --compat=mixed myapp.ebs  # Allow both syntaxes
```

## Decision Matrix

### When to Use EBS1

Choose EBS1 if you:
- [x] Need immediate production stability
- [x] Have existing large EBS1 codebase
- [x] Team familiar with C-like syntax
- [x] Only need desktop deployment
- [x] Don't need web/mobile support
- [x] Prefer traditional programming style

### When to Use EBS2

Choose EBS2 if you:
- [x] Teaching children programming
- [x] Need web browser support
- [x] Want mobile accessibility
- [x] Starting new projects
- [x] Value natural language readability
- [x] Need modern features (async, graphics, plugins)
- [x] Want block-based programming option

### Hybrid Approach

You can use both:

1. **Maintain EBS1** for legacy applications
2. **Start new projects** in EBS2
3. **Migrate incrementally** using compatibility mode
4. **Use EBS2 web runtime** for distribution while keeping EBS1 for development

### Feature Comparison for Decision Making

| Need | EBS1 | EBS2 |
|------|------|------|
| **Teaching children** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Professional apps** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Web deployment** | ⭐ | ⭐⭐⭐⭐⭐ |
| **Desktop apps** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Database apps** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Games** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Learning curve** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Productivity** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Stability** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ (new) |

## Conclusion

### EBS1 Strengths
- Mature and stable
- Familiar to programmers
- Excellent desktop capabilities
- Strong database support
- Production-ready

### EBS2 Advantages
- Beginner-friendly
- Cross-platform (web + desktop)
- Modern language features
- Better for teaching
- Future-proof

### Recommendation

**For New Projects:**
- **Educational**: Use EBS2
- **Business Desktop**: Use EBS1 (until EBS2 stable)
- **Web Applications**: Use EBS2
- **Games**: Use EBS2
- **Data/Database**: Use EBS1 (for now)

**For Existing Projects:**
- Continue with EBS1
- Plan migration to EBS2
- Use migration tools
- Test compatibility mode

**Timeline:**
- **Now - Month 6**: EBS1 for production
- **Month 6-12**: Begin EBS2 adoption
- **Month 12+**: EBS2 recommended for all new projects
- **Long-term**: EBS1 maintenance mode, EBS2 active development

---

## FAQ

### Can I mix EBS1 and EBS2 in the same project?
Not directly, but you can use compatibility mode to run EBS1 code in EBS2, or call between them via APIs.

### Will EBS1 continue to be supported?
Yes, EBS1 will receive maintenance updates (bug fixes, security) for at least 2 years after EBS2 release.

### Can I gradually migrate my EBS1 project?
Yes! Use the migration tool to convert module-by-module, and use compatibility mode to run mixed codebases.

### Is EBS2 slower than EBS1?
On Java desktop, performance should be similar. In browser, there's some overhead from transpilation, but it's optimized for acceptable performance.

### Do I need to learn everything new for EBS2?
No! Core concepts are the same. Main differences are syntax style and some type names. Skills transfer easily.

### Can beginners use EBS1?
Yes, but EBS2 is specifically designed to be more intuitive for children and complete beginners.

### Which should I teach in my classroom?
If starting fresh, we recommend EBS2 for its beginner-friendly design. If you have existing EBS1 curriculum, you can continue with it.

---

**Document Version:** 2.0.0-COMPARISON  
**Last Updated:** December 2025  
**Status:** Planning Phase
