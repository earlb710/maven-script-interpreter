# Python vs EBS Script: String Functions Comparison

This document compares Python's built-in string methods with EBS Script's string functions, helping developers familiar with Python transition to or work alongside EBS Script.

## Table of Contents
- [Quick Reference Table](#quick-reference-table)
- [Case Conversion](#case-conversion)
- [Search and Find](#search-and-find)
- [String Testing](#string-testing)
- [Modification and Replacement](#modification-and-replacement)
- [Splitting and Joining](#splitting-and-joining)
- [Padding and Formatting](#padding-and-formatting)
- [Character Operations](#character-operations)
- [Substring Extraction](#substring-extraction)
- [EBS-Specific Functions](#ebs-specific-functions)
- [Python Functions Not in EBS](#python-functions-not-in-ebs)
- [Usage Examples](#usage-examples)

---

## Quick Reference Table

| Python Method | EBS Function | Description |
|--------------|--------------|-------------|
| `str.upper()` | `str.toUpper(str)` | Convert to uppercase |
| `str.lower()` | `str.toLower(str)` | Convert to lowercase |
| `str.strip()` | `str.trim(str)` | Remove leading/trailing whitespace |
| `str.replace(old, new)` | `str.replace(str, old, new)` | Replace all occurrences of substring |
| `str.replace(old, new, 1)` | `str.replaceFirst(str, old, new)` | Replace first occurrence only |
| `str.split(sep)` | `str.split(str, regex)` | Split into array/list |
| `sep.join(list)` | `str.join(array, sep)` | Join array elements with separator |
| `sub in str` | `str.contains(str, sub)` | Check if contains substring |
| `str.startswith(prefix)` | `str.startsWith(str, prefix)` | Check prefix |
| `str.endswith(suffix)` | `str.endsWith(str, suffix)` | Check suffix |
| `str1 == str2` | `str.equals(str1, str2)` | Exact string comparison |
| `str1.lower() == str2.lower()` | `str.equalsIgnoreCase(str1, str2)` | Case-insensitive comparison |
| `len(str) == 0` | `str.isEmpty(str)` | Check if empty |
| `str.isspace()` or not trimmed | `str.isBlank(str)` | Check if blank (empty or whitespace) |
| `str[start:end]` | `str.substring(str, start, end)` | Extract substring |
| `str.find(sub)` | `str.indexOf(str, sub)` | Find first occurrence |
| `str.rfind(sub)` | `str.lastIndexOf(str, sub)` | Find last occurrence |
| `str[index]` | `str.charAt(str, index)` | Get character at index |
| `re.sub(pattern, repl, str)` | `str.replaceAll(str, regex, repl)` | Regex replacement (all occurrences) |
| `re.search(pattern, str).group()` | `str.findRegex(str, regex)` | Find first regex match |
| `re.findall(pattern, str)` | `str.findAllRegex(str, regex)` | Find all regex matches |
| `str.ljust(width, char)` | `str.rpad(str, length, char)` | Right-pad string |
| `str.rjust(width, char)` | `str.lpad(str, length, char)` | Left-pad string |
| `str(value)` | `str.toString(value)` | Convert to string |
| `[ord(c) for c in str]` | `str.charArray(str)` | Get character codes |

---

## Case Conversion

### Python
```python
text = "Hello World"
upper = text.upper()    # "HELLO WORLD"
lower = text.lower()    # "hello world"
```

### EBS Script
```javascript
var text: string = "Hello World";
var upper = call str.toUpper(text);    // "HELLO WORLD"
var lower = call str.toLower(text);    // "hello world"
```

**Key Difference**: In EBS, string functions are called with `call str.functionName(str)` syntax rather than method chaining.

---

## Search and Find

### Python
```python
text = "Hello World"
idx = text.find("World")       # 6
idx = text.find("xyz")         # -1 (not found)
idx = text.rfind("o")          # 7 (last occurrence)
idx = text.find("o", 5)        # 7 (starting from index 5)
```

### EBS Script
```javascript
var text: string = "Hello World";
var idx = call str.indexOf(text, "World");       // 6
var idx = call str.indexOf(text, "xyz");         // -1 (not found)
var idx = call str.lastIndexOf(text, "o");       // 7 (last occurrence)
var idx = call str.indexOf(text, "o", 5);        // 7 (starting from index 5)
```

**Key Difference**: Python uses `find()` and `rfind()`, while EBS uses `indexOf()` and `lastIndexOf()` (Java-style naming).

---

## String Testing

### Python
```python
text = "Hello World"

# Check if contains
has_world = "World" in text           # True

# Check prefix/suffix
starts_h = text.startswith("Hello")   # True
ends_d = text.endswith("World")       # True

# String comparison
is_equal = text == "Hello World"      # True

# Empty/blank checks
is_empty = len(text) == 0             # False
is_blank = text.strip() == ""         # False
```

### EBS Script
```javascript
var text: string = "Hello World";

// Check if contains
var hasWorld = call str.contains(text, "World");     // true

// Check prefix/suffix
var startsH = call str.startsWith(text, "Hello");    // true
var endsD = call str.endsWith(text, "World");        // true

// String comparison
var isEqual = call str.equals(text, "Hello World");              // true
var ignoreCase = call str.equalsIgnoreCase(text, "hello world"); // true

// Empty/blank checks
var isEmpty = call str.isEmpty(text);     // false
var isBlank = call str.isBlank(text);     // false
```

**Key Differences**: 
- Python uses the `in` operator for containment; EBS uses `str.contains()`
- EBS provides `str.equalsIgnoreCase()` as a built-in function

---

## Modification and Replacement

### Python
```python
text = "Hello World"

# Replace all occurrences
replaced = text.replace("o", "0")      # "Hell0 W0rld"

# Replace first occurrence only
first_only = text.replace("o", "0", 1)  # "Hell0 World"

# Regex replacement (requires import re)
import re
regex_replaced = re.sub(r"\s+", "-", text)  # "Hello-World"

# Find first regex match
import re
match = re.search(r"\w+", text)
first_match = match.group() if match else None  # "Hello"

# Find all regex matches
all_matches = re.findall(r"\w+", text)  # ["Hello", "World"]

# Trim whitespace
padded = "  Hello  "
trimmed = padded.strip()               # "Hello"
```

### EBS Script
```javascript
var text: string = "Hello World";

// Replace all occurrences
var replaced = call str.replace(text, "o", "0");      // "Hell0 W0rld"

// Replace first occurrence only
var firstOnly = call str.replaceFirst(text, "o", "0");  // "Hell0 World"

// Regex replacement (built-in, no import needed)
var regexReplaced = call str.replaceAll(text, "\\s+", "-");  // "Hello-World"

// Find first regex match
var firstMatch = call str.findRegex(text, "\\w+");    // "Hello"

// Find all regex matches
var allMatches = call str.findAllRegex(text, "\\w+"); // ["Hello", "World"]

// Trim whitespace
var padded: string = "  Hello  ";
var trimmed = call str.trim(padded);                  // "Hello"
```

**Key Differences**: 
- Python's `replace()` is a method; EBS uses `str.replace(str, old, new)`
- Python uses `replace(old, new, 1)` for single replace; EBS uses `str.replaceFirst()`
- EBS has built-in regex support via `str.replaceAll()`, `str.findRegex()`, and `str.findAllRegex()` without needing imports
- Note: In EBS regex patterns, backslashes need to be escaped (`\\s+` not `\s+`)

---

## Splitting and Joining

### Python
```python
text = "apple,banana,cherry"

# Split by delimiter
parts = text.split(",")               # ["apple", "banana", "cherry"]

# Split with limit
parts = text.split(",", 1)            # ["apple", "banana,cherry"]

# Join
separator = "-"
joined = separator.join(parts)        # "apple-banana-cherry"
```

### EBS Script
```javascript
var text: string = "apple,banana,cherry";

// Split by delimiter (uses regex)
var parts = call str.split(text, ",");     // ["apple", "banana", "cherry"]

// Split with limit
var parts = call str.split(text, ",", 2);  // ["apple", "banana,cherry"]

// Join
var separator: string = "-";
var joined = call str.join(parts, "-");    // "apple-banana-cherry"
```

**Key Differences**: 
- EBS `str.split()` uses regex patterns, so special regex characters need escaping
- Python join is `separator.join(list)`, EBS is `str.join(array, separator)` (reversed parameter order)

---

## Padding and Formatting

### Python
```python
text = "42"

# Left pad (right-justify)
left_padded = text.rjust(5, "0")      # "00042"

# Right pad (left-justify)
right_padded = text.ljust(5, "_")     # "42___"
```

### EBS Script
```javascript
var text: string = "42";

// Left pad
var leftPadded = call str.lpad(text, 5, "0");    // "00042"

// Right pad
var rightPadded = call str.rpad(text, 5, "_");   // "42___"
```

**Key Differences**: 
- Python uses `rjust()` for left-padding and `ljust()` for right-padding
- EBS uses more intuitive names: `lpad()` (add padding on left) and `rpad()` (add padding on right)

---

## Character Operations

### Python
```python
text = "Hello"

# Get character at index
char = text[0]                        # "H"

# Get character codes (Unicode code points)
codes = [ord(c) for c in text]        # [72, 101, 108, 108, 111]

# Get single char code
code = ord(text[0])                   # 72
```

### EBS Script
```javascript
var text: string = "Hello";

// Get character at index
var char = call str.charAt(text, 0);          // "H"

// Get character codes (Unicode code points)
var codes = call str.charArray(text);         // [72, 101, 108, 108, 111]

// Access individual code
print codes[0];                               // 72
```

**Key Differences**: 
- Python uses bracket notation `text[0]`; EBS uses `str.charAt(str, index)`
- EBS provides `str.charArray()` to get all character codes at once

---

## Substring Extraction

### Python
```python
text = "Hello World"

# Extract substring
sub1 = text[0:5]                      # "Hello"
sub2 = text[6:]                       # "World"
sub3 = text[:5]                       # "Hello"
sub4 = text[-5:]                      # "World" (negative index)
```

### EBS Script
```javascript
var text: string = "Hello World";

// Extract substring
var sub1 = call str.substring(text, 0, 5);    // "Hello"
var sub2 = call str.substring(text, 6);       // "World"

// Note: EBS doesn't support negative indexing
// For last 5 chars, calculate the position:
var length = text.length;
var sub4 = call str.substring(text, length - 5);  // "World"
```

**Key Differences**: 
- Python uses slice notation `[start:end]`; EBS uses `str.substring(str, start, end)`
- Python supports negative indexing; EBS requires calculating positions manually

---

## EBS-Specific Functions

These EBS functions don't have direct Python equivalents as simple methods:

### str.toString(value)
Converts any value to string representation.

```javascript
var num: int = 42;
var numStr = call str.toString(num);    // "42"

var jsonObj: json = {"key": "value"};
var jsonStr = call str.toString(jsonObj);  // '{"key":"value"}'
```

Python equivalent:
```python
str(42)           # "42"
str({"key": "value"})  # "{'key': 'value'}"
```

### string.contains(str, sub) - Alternative name
Same as `str.contains()` but with different naming convention.

```javascript
var result = call string.contains("Hello World", "World");  // true
```

---

## Python Functions Not in EBS

The following Python string methods don't have direct EBS equivalents:

| Python Method | Description | Workaround in EBS |
|--------------|-------------|-------------------|
| `str.title()` | Title case | Manual implementation |
| `str.capitalize()` | Capitalize first | Manual implementation |
| `str.swapcase()` | Swap case | Manual implementation |
| `str.center(width)` | Center with padding | Use `lpad` + `rpad` combination |
| `str.count(sub)` | Count occurrences | Loop with `indexOf` |
| `str.encode()` | Encode to bytes | Use `str.charArray()` |
| `str.format()` | String formatting | String concatenation |
| `str.isalpha()` | Check alphabetic | Use regex with `replaceAll` |
| `str.isdigit()` | Check digits | Use regex with `replaceAll` |
| `str.isalnum()` | Check alphanumeric | Use regex with `replaceAll` |
| `str.lstrip()` | Left strip only | Regex with `replaceAll` |
| `str.rstrip()` | Right strip only | Regex with `replaceAll` |
| `str.zfill(width)` | Zero-fill | Use `str.lpad(str, width, "0")` |
| `str.partition(sep)` | Partition by sep | Manual split logic |
| `str.maketrans()` / `str.translate()` | Character translation | Loop with `replace` |

### Workaround Examples

**Count occurrences:**
```javascript
// EBS workaround for Python's str.count()
countOccurrences(text: string, sub: string) return int {
    var count: int = 0;
    var pos: int = 0;
    while pos >= 0 {
        pos = call str.indexOf(text, sub, pos);
        if pos >= 0 then {
            count = count + 1;
            pos = pos + 1;
        }
    }
    return count;
}

var text: string = "banana";
var count = call countOccurrences(text, "a");  // 3
```

**Zero-fill (Python's zfill):**
```javascript
// EBS equivalent to Python's zfill
var num: string = "42";
var zeroPadded = call str.lpad(num, 5, "0");  // "00042"
```

---

## Usage Examples

### Example 1: Parse and Validate Email

**Python:**
```python
def validate_email(email):
    if "@" not in email:
        return False
    parts = email.split("@")
    return len(parts) == 2 and len(parts[0]) > 0 and "." in parts[1]

email = "user@example.com"
is_valid = validate_email(email)  # True
```

**EBS Script:**
```javascript
validateEmail(email: string) return bool {
    if !call str.contains(email, "@") then {
        return false;
    }
    var parts = call str.split(email, "@");
    if parts.length != 2 then {
        return false;
    }
    return parts[0].length > 0 and call str.contains(parts[1], ".");
}

var email: string = "user@example.com";
var isValid = call validateEmail(email);  // true
```

### Example 2: Format Name

**Python:**
```python
def format_name(first, last):
    first = first.strip().lower()
    last = last.strip().lower()
    return first[0].upper() + first[1:] + " " + last[0].upper() + last[1:]

name = format_name("  JOHN  ", "  DOE  ")  # "John Doe"
```

**EBS Script:**
```javascript
formatName(first: string, last: string) return string {
    first = call str.trim(first);
    first = call str.toLower(first);
    last = call str.trim(last);
    last = call str.toLower(last);
    
    var firstChar = call str.toUpper(call str.charAt(first, 0));
    var lastChar = call str.toUpper(call str.charAt(last, 0));
    
    var firstRest = call str.substring(first, 1);
    var lastRest = call str.substring(last, 1);
    
    return firstChar + firstRest + " " + lastChar + lastRest;
}

var name = call formatName("  JOHN  ", "  DOE  ");  // "John Doe"
```

### Example 3: Extract File Extension

**Python:**
```python
def get_extension(filename):
    if "." not in filename:
        return ""
    return filename.rsplit(".", 1)[-1].lower()

ext = get_extension("document.PDF")  # "pdf"
```

**EBS Script:**
```javascript
getExtension(filename: string) return string {
    var lastDot = call str.lastIndexOf(filename, ".");
    if lastDot < 0 then {
        return "";
    }
    var ext = call str.substring(filename, lastDot + 1);
    return call str.toLower(ext);
}

var ext = call getExtension("document.PDF");  // "pdf"
```

---

## Key Takeaways

1. **Function vs Method Syntax**: Python uses `"text".method()` while EBS uses `call str.method(text)`
2. **No Method Chaining**: EBS requires storing intermediate results or nesting calls
3. **Java-Style Naming**: EBS uses camelCase naming similar to Java (e.g., `indexOf` vs Python's `find`)
4. **Regex Built-in**: EBS has built-in regex support in `split()` and `replaceAll()`
5. **Parameter Order**: Some functions have different parameter orders (e.g., `join`)
6. **No Negative Indexing**: EBS doesn't support negative indices for substring operations

---

## Version Information

- **EBS Script**: Version 1.0-SNAPSHOT
- **Python Reference**: Python 3.x

For the complete EBS String function reference, see [EBS_SCRIPT_SYNTAX.md](EBS_SCRIPT_SYNTAX.md).
