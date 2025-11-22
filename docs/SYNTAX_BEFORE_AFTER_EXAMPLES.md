# Before and After: EBS Syntax Improvements

## Example 1: Simple Counter Loop

### Before (Original Syntax)
```javascript
var i: int = 0;
while i < 10 {
    print i;
    i = i + 1;
}
```

### After (New Syntax)
```javascript
for (var i: int = 0; i < 10; i++) {
    print i;
}
```

**Lines of Code:** 5 → 3 (40% reduction)  
**Clarity:** Loop structure is immediately clear

---

## Example 2: Data Processing with Accumulator

### Before (Original Syntax)
```javascript
var sum: int = 0;
var count: int = 0;

var i: int = 0;
while i < 100 {
    sum = sum + i;
    count = count + 1;
    i = i + 1;
}

var average: double = sum / count;
```

### After (New Syntax)
```javascript
let sum: int = 0;
let count: int = 0;

for (var i: int = 0; i < 100; i++) {
    sum += i;
    count++;
}

var average: double = sum / count;
```

**Lines of Code:** 11 → 8 (27% reduction)  
**Readability:** Modern, familiar syntax

---

## Example 3: Function Definition

### Before (Original Syntax)
```javascript
// Can be confusing - is this a function or something else?
calculateArea(width: double, height: double) return double {
    var area: double = width * height;
    return area;
}

calculatePerimeter(width: double, height: double) return double {
    var perimeter: double = 2 * width + 2 * height;
    return perimeter;
}
```

### After (New Syntax)
```javascript
// Clear intent - these are functions
function calculateArea(width: double, height: double) return double {
    let area: double = width * height;
    return area;
}

function calculatePerimeter(width: double, height: double) return double {
    let perimeter: double = 2 * width + 2 * height;
    return perimeter;
}
```

**Clarity:** Keyword makes function declarations obvious  
**Familiarity:** Matches JavaScript, TypeScript conventions

---

## Example 4: Array Processing

### Before (Original Syntax)
```javascript
var numbers = [1, 2, 3, 4, 5];

var i: int = 0;
while i < numbers.length {
    print numbers[i];
    i = i + 1;
}

var total: int = 0;
i = 0;
while i < numbers.length {
    total = total + numbers[i];
    i = i + 1;
}
```

### After (New Syntax)
```javascript
let numbers = [1, 2, 3, 4, 5];

// Using foreach
foreach num in numbers {
    print num;
}

// Using for loop
let total: int = 0;
for (var i: int = 0; i < numbers.length; i++) {
    total += numbers[i];
}
```

**Lines of Code:** 13 → 10 (23% reduction)  
**Clarity:** Intent is immediately obvious

---

## Example 5: Complex State Management

### Before (Original Syntax)
```javascript
var score: int = 0;
var multiplier: int = 1;
var bonus: int = 10;

// Update score
score = score + 100;
score = score * multiplier;
score = score + bonus;

// Update multiplier
multiplier = multiplier + 1;

// Apply penalties
if score > 1000 then {
    score = score - 50;
}
```

### After (New Syntax)
```javascript
let score: int = 0;
let multiplier: int = 1;
let bonus: int = 10;

// Update score
score += 100;
score *= multiplier;
score += bonus;

// Update multiplier
multiplier++;

// Apply penalties
if score > 1000 then {
    score -= 50;
}
```

**Lines Changed:** 5 statements simplified  
**Readability:** Dramatically improved with compound operators

---

## Example 6: Building Complex Expressions

### Before (Original Syntax)
```javascript
var x: int = 10;
var y: int = 20;
var z: int = 30;

x = x + y;
x = x - z;
x = x * 2;
x = x / 4;

y = y + 1;
y = y + 1;
y = y + 1;

z = z - 1;
```

### After (New Syntax)
```javascript
let x: int = 10;
let y: int = 20;
let z: int = 30;

x += y;
x -= z;
x *= 2;
x /= 4;

y++;
y++;
y++;

z--;
```

**Lines Changed:** 11 → 11 (same length)  
**Readability:** Much clearer intent, less repetition

---

## Summary of Improvements

| Feature | Before | After | Benefit |
|---------|--------|-------|---------|
| **For Loops** | 5 lines | 3 lines | -40% code, clearer structure |
| **Increment** | `x = x + 1` | `x++` | 60% less typing |
| **Compound Assignment** | `x = x + 5` | `x += 5` | 50% less typing, clearer |
| **Let Keyword** | `var` only | `var` or `let` | JS developer friendly |
| **Function Keyword** | Implicit | Explicit `function` | Obvious declarations |
| **ForEach** | Manual loop | `foreach` | Safer, clearer |

## Backward Compatibility

✅ **All old syntax still works!**

You can mix and match:
```javascript
// Old style
var count: int = 0;
while count < 10 {
    count = count + 1;
}

// New style
for (let i: int = 0; i < 10; i++) {
    print i;
}

// Mixed
var x: int = 0;
x++;  // New
x = x + 5;  // Old
x += 5;  // New
```

## Recommendation for Beginners

Start with the new syntax when learning:
1. Use `for` loops for counted iterations
2. Use `foreach` for array/collection iterations  
3. Use `++`/`--` for simple increments
4. Use compound operators (`+=`, `-=`, etc.) for accumulation
5. Use `function` keyword for clarity
6. Use `let` or `var` - your choice!

This makes EBS code look and feel like modern JavaScript/TypeScript, reducing the learning curve significantly.
