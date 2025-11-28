# EBS Script vs Oracle PL/SQL: Language Comparison

This document compares EBS (Earl Bosch Script) with Oracle PL/SQL, highlighting the key differences and similarities between these two languages. This guide is intended for developers familiar with PL/SQL who are learning EBS Script, or vice versa.

## Table of Contents
- [Overview](#overview)
- [Language Philosophy](#language-philosophy)
- [Quick Reference Table](#quick-reference-table)
- [Data Types](#data-types)
- [Variable Declarations](#variable-declarations)
- [Operators](#operators)
- [Control Flow](#control-flow)
- [Functions and Procedures](#functions-and-procedures)
- [Exception Handling](#exception-handling)
- [Database Operations](#database-operations)
- [Arrays and Collections](#arrays-and-collections)
- [JSON Support](#json-support)
- [String Operations](#string-operations)
- [EBS-Specific Features](#ebs-specific-features)
- [PL/SQL Features Not in EBS](#plsql-features-not-in-ebs)
- [Migration Examples](#migration-examples)

---

## Overview

| Aspect | EBS Script | Oracle PL/SQL |
|--------|-----------|---------------|
| **Type** | Interpreted scripting language | Compiled procedural language |
| **Environment** | JavaFX-based IDE with console | Oracle Database server |
| **Primary Use** | Scripting, UI applications, automation | Database programming, stored procedures |
| **Execution** | Interpreter runs on JVM | Runs within Oracle Database engine |
| **Data Storage** | In-memory, file-based | Database tables |

---

## Language Philosophy

### EBS Script
- **Scripting-first**: Designed for quick prototyping and automation
- **C/JavaScript-like syntax**: Familiar to web developers
- **Modern features**: Native JSON support, JavaFX UI integration
- **Flexible typing**: Supports both explicit and inferred types
- **Standalone**: Can run independently without a database

### Oracle PL/SQL
- **Database-centric**: Tightly integrated with Oracle Database
- **Ada-inspired syntax**: Block-structured with BEGIN/END keywords
- **Enterprise features**: Triggers, packages, bulk operations
- **Strong typing**: Requires explicit type declarations
- **Transaction support**: Built-in COMMIT/ROLLBACK

---

## Quick Reference Table

| Feature | EBS Script | Oracle PL/SQL |
|---------|-----------|---------------|
| Block delimiter | `{ }` | `BEGIN ... END;` |
| Statement terminator | `;` | `;` |
| Comments | `// single line` | `-- single line`, `/* multi */` |
| Variable declaration | `var x: int = 10;` | `x NUMBER := 10;` |
| Assignment | `x = 10;` | `x := 10;` |
| Equality test | `==` | `=` |
| Not equal | `!=` | `<>` or `!=` |
| Logical AND | `and` | `AND` |
| Logical OR | `or` | `OR` |
| Logical NOT | `!` | `NOT` |
| String concatenation | `+` | `||` |
| Function call | `call func()` or `#func()` | `func()` |
| Print output | `print "text";` | `DBMS_OUTPUT.PUT_LINE('text');` |
| If statement | `if cond then { }` | `IF cond THEN ... END IF;` |
| While loop | `while cond { }` | `WHILE cond LOOP ... END LOOP;` |
| For loop | `for (i=0; i<10; i++)` | `FOR i IN 1..10 LOOP ... END LOOP;` |

---

## Data Types

### EBS Script Data Types

```javascript
// Primitive types
var intVal: int = 42;
var longVal: long = 9999999999;
var floatVal: float = 3.14;
var doubleVal: double = 3.14159265359;
var byteVal: byte = 127;
var strVal: string = "Hello";
var boolVal: bool = true;
var dateVal: date = call now();

// Complex types
var jsonObj: json = {"key": "value"};
var mapObj: map = {"a": 1, "b": 2};
var arr: int[5];           // Fixed-size array
var dynArr: string[*];     // Dynamic array
```

### Oracle PL/SQL Data Types

```sql
DECLARE
  -- Numeric types
  int_val NUMBER := 42;
  int_val2 PLS_INTEGER := 42;
  float_val BINARY_FLOAT := 3.14;
  double_val BINARY_DOUBLE := 3.14159265359;
  
  -- Character types
  str_val VARCHAR2(100) := 'Hello';
  char_val CHAR(10) := 'World';
  clob_val CLOB;
  
  -- Boolean type
  bool_val BOOLEAN := TRUE;
  
  -- Date/Time types
  date_val DATE := SYSDATE;
  ts_val TIMESTAMP := SYSTIMESTAMP;
  
  -- Complex types
  TYPE num_array IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
  TYPE rec_type IS RECORD (name VARCHAR2(50), age NUMBER);
BEGIN
  NULL;
END;
```

### Type Comparison

| EBS Type | PL/SQL Equivalent | Notes |
|----------|------------------|-------|
| `int` | `NUMBER`, `PLS_INTEGER` | EBS is 32-bit, PL/SQL NUMBER is arbitrary precision |
| `long` | `NUMBER` | EBS is 64-bit |
| `float` | `BINARY_FLOAT` | Single precision |
| `double` | `BINARY_DOUBLE`, `NUMBER` | Double precision |
| `byte` | `RAW(1)` | Single byte |
| `string` | `VARCHAR2`, `CHAR` | EBS has no length limit |
| `bool` | `BOOLEAN` | Similar |
| `date` | `DATE`, `TIMESTAMP` | EBS includes time by default |
| `json` | `JSON` (12c+) | Native in both |
| `array` | `TABLE`, `VARRAY` | Different implementations |
| `record` | `RECORD`, `%ROWTYPE` | Similar concept |

---

## Variable Declarations

### EBS Script

```javascript
// Explicit type
var name: string = "Alice";
var age: int = 30;

// Type inference
var count = 42;           // Inferred as int
var message = "Hello";    // Inferred as string

// Uninitialized variables
var total: double;

// Using let keyword (alias for var)
let score: int = 100;

// Constants (by convention, use UPPER_CASE)
var MAX_SIZE: int = 100;
```

### Oracle PL/SQL

```sql
DECLARE
  -- Variable declaration (type before name)
  name VARCHAR2(100) := 'Alice';
  age NUMBER := 30;
  
  -- Uninitialized variables
  total NUMBER;
  
  -- Constants
  max_size CONSTANT NUMBER := 100;
  
  -- Anchored types
  emp_name employees.first_name%TYPE;
  emp_rec employees%ROWTYPE;
  
  -- NOT NULL constraint
  counter NUMBER NOT NULL := 0;
BEGIN
  NULL;
END;
```

### Key Differences

| Aspect | EBS Script | Oracle PL/SQL |
|--------|-----------|---------------|
| Keyword | `var` or `let` | (none, just type) |
| Type position | After colon: `var x: int` | Before name: `x NUMBER` |
| Assignment operator | `=` | `:=` |
| Type inference | Supported | Not supported |
| Anchored types | Not supported | `%TYPE`, `%ROWTYPE` |
| Constants | By convention | `CONSTANT` keyword |

---

## Operators

### Comparison Operators

| Operation | EBS Script | Oracle PL/SQL |
|-----------|-----------|---------------|
| Equal | `==` | `=` |
| Not equal | `!=` | `<>` or `!=` |
| Greater than | `>` | `>` |
| Less than | `<` | `<` |
| Greater or equal | `>=` | `>=` |
| Less or equal | `<=` | `<=` |

### Logical Operators

| Operation | EBS Script | Oracle PL/SQL |
|-----------|-----------|---------------|
| AND | `and` | `AND` |
| OR | `or` | `OR` |
| NOT | `!` | `NOT` |

### Arithmetic Operators

| Operation | EBS Script | Oracle PL/SQL |
|-----------|-----------|---------------|
| Addition | `+` | `+` |
| Subtraction | `-` | `-` |
| Multiplication | `*` | `*` |
| Division | `/` | `/` |
| Exponentiation | `^` | `**` |

### String Operators

| Operation | EBS Script | Oracle PL/SQL |
|-----------|-----------|---------------|
| Concatenation | `+` | `||` |

**Example:**

```javascript
// EBS Script
var fullName = firstName + " " + lastName;
```

```sql
-- PL/SQL
full_name := first_name || ' ' || last_name;
```

---

## Control Flow

### If Statements

**EBS Script:**
```javascript
// Basic if
if age >= 18 then {
    print "Adult";
}

// If-else
if score >= 60 then {
    print "Pass";
} else {
    print "Fail";
}

// If-else-if
if grade == "A" then {
    print "Excellent";
} else if grade == "B" then {
    print "Good";
} else {
    print "Needs improvement";
}

// Alternative syntax with parentheses
if (age >= 18) {
    print "Adult";
}
```

**Oracle PL/SQL:**
```sql
-- Basic if
IF age >= 18 THEN
    DBMS_OUTPUT.PUT_LINE('Adult');
END IF;

-- If-else
IF score >= 60 THEN
    DBMS_OUTPUT.PUT_LINE('Pass');
ELSE
    DBMS_OUTPUT.PUT_LINE('Fail');
END IF;

-- If-elsif-else
IF grade = 'A' THEN
    DBMS_OUTPUT.PUT_LINE('Excellent');
ELSIF grade = 'B' THEN
    DBMS_OUTPUT.PUT_LINE('Good');
ELSE
    DBMS_OUTPUT.PUT_LINE('Needs improvement');
END IF;
```

### While Loops

**EBS Script:**
```javascript
var i: int = 0;
while i < 10 {
    print i;
    i++;
}

// Alternative with 'then'
while i < 10 then {
    print i;
    i = i + 1;
}
```

**Oracle PL/SQL:**
```sql
DECLARE
    i NUMBER := 0;
BEGIN
    WHILE i < 10 LOOP
        DBMS_OUTPUT.PUT_LINE(i);
        i := i + 1;
    END LOOP;
END;
```

### For Loops

**EBS Script:**
```javascript
// C-style for loop
for (var i: int = 0; i < 10; i++) {
    print i;
}

// Foreach loop
var items = ["apple", "banana", "cherry"];
foreach item in items {
    print item;
}
```

**Oracle PL/SQL:**
```sql
-- Numeric for loop
FOR i IN 0..9 LOOP
    DBMS_OUTPUT.PUT_LINE(i);
END LOOP;

-- Reverse loop
FOR i IN REVERSE 1..10 LOOP
    DBMS_OUTPUT.PUT_LINE(i);
END LOOP;

-- Cursor for loop
FOR rec IN (SELECT * FROM employees) LOOP
    DBMS_OUTPUT.PUT_LINE(rec.first_name);
END LOOP;
```

### Do-While Loops

**EBS Script:**
```javascript
var count: int = 0;
do {
    print count;
    count++;
} while (count < 5);
```

**Oracle PL/SQL:**
```sql
-- PL/SQL uses simple LOOP with EXIT condition
DECLARE
    counter NUMBER := 0;
BEGIN
    LOOP
        DBMS_OUTPUT.PUT_LINE(counter);
        counter := counter + 1;
        EXIT WHEN counter >= 5;
    END LOOP;
END;
```

### Loop Control

| Control | EBS Script | Oracle PL/SQL |
|---------|-----------|---------------|
| Exit loop | `break;` or `exit;` | `EXIT;` |
| Exit with condition | `if cond then break;` | `EXIT WHEN condition;` |
| Skip iteration | `continue;` | `CONTINUE;` |

---

## Functions and Procedures

### Function Definition

**EBS Script:**
```javascript
// Basic function with return
add(a: int, b: int) return int {
    return a + b;
}

// With optional 'function' keyword
function multiply(x: int, y: int) return int {
    return x * y;
}

// Function with default parameters
greet(name: string = "World") return string {
    return "Hello, " + name + "!";
}

// Calling functions
var sum = call add(5, 3);           // Using 'call'
var product = #multiply(4, 6);       // Using '#' shorthand
var msg = call greet();              // Uses default
var msg2 = call greet("Alice");      // With argument
```

**Oracle PL/SQL:**
```sql
-- Function in PL/SQL block
CREATE OR REPLACE FUNCTION add_numbers(
    a IN NUMBER,
    b IN NUMBER
) RETURN NUMBER IS
BEGIN
    RETURN a + b;
END;
/

-- Procedure (no return value)
CREATE OR REPLACE PROCEDURE greet(
    p_name IN VARCHAR2 DEFAULT 'World'
) IS
BEGIN
    DBMS_OUTPUT.PUT_LINE('Hello, ' || p_name || '!');
END;
/

-- Calling functions
DECLARE
    result NUMBER;
BEGIN
    result := add_numbers(5, 3);
    greet('Alice');
END;
```

### Key Differences

| Aspect | EBS Script | Oracle PL/SQL |
|--------|-----------|---------------|
| Definition keyword | `function` (optional) | `FUNCTION`, `PROCEDURE` |
| Parameter syntax | `name: type` | `name IN/OUT type` |
| Return syntax | `return type` before body | `RETURN type IS` |
| Block delimiters | `{ }` | `IS/AS ... BEGIN ... END;` |
| Calling functions | `call func()` or `#func()` | `func()` |
| Default parameters | `param: type = value` | `param type DEFAULT value` |
| OUT parameters | Not supported | `OUT`, `IN OUT` keywords |

---

## Exception Handling

### EBS Script

```javascript
// Basic try-exceptions
try {
    var result = 10 / 0;
} exceptions {
    when MATH_ERROR {
        print "Cannot divide by zero!";
    }
    when ANY_ERROR {
        print "An unexpected error occurred";
    }
}

// Capture error message
try {
    var data = #file.read("missing.txt");
} exceptions {
    when IO_ERROR(msg) {
        print "File error: " + msg;
    }
}

// Multiple handlers
try {
    var json = #json.jsonfromstring(invalidJson);
} exceptions {
    when PARSE_ERROR {
        print "Invalid JSON format";
    }
    when TYPE_ERROR {
        print "Type conversion failed";
    }
    when ANY_ERROR(errorMsg) {
        print "Error: " + errorMsg;
    }
}
```

**EBS Error Types:**
- `ANY_ERROR` - Catches any error (catch-all)
- `IO_ERROR` - File I/O operations
- `DB_ERROR` - Database errors
- `TYPE_ERROR` - Type conversion errors
- `NULL_ERROR` - Null value errors
- `INDEX_ERROR` - Array index out of bounds
- `MATH_ERROR` - Division by zero, arithmetic errors
- `PARSE_ERROR` - JSON parsing, date parsing
- `NETWORK_ERROR` - HTTP and network errors
- `NOT_FOUND_ERROR` - Variable/function not found
- `ACCESS_ERROR` - Permission errors
- `VALIDATION_ERROR` - Validation errors

### Oracle PL/SQL

```sql
DECLARE
    v_result NUMBER;
BEGIN
    v_result := 10 / 0;
EXCEPTION
    WHEN ZERO_DIVIDE THEN
        DBMS_OUTPUT.PUT_LINE('Cannot divide by zero!');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
END;

-- User-defined exceptions
DECLARE
    e_invalid_data EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_invalid_data, -20001);
BEGIN
    IF some_condition THEN
        RAISE e_invalid_data;
    END IF;
EXCEPTION
    WHEN e_invalid_data THEN
        DBMS_OUTPUT.PUT_LINE('Invalid data!');
END;
```

**PL/SQL Predefined Exceptions:**
- `ZERO_DIVIDE` - Division by zero
- `NO_DATA_FOUND` - SELECT returned no rows
- `TOO_MANY_ROWS` - SELECT returned multiple rows
- `VALUE_ERROR` - Arithmetic or conversion error
- `INVALID_CURSOR` - Invalid cursor operation
- `DUP_VAL_ON_INDEX` - Unique constraint violation
- `OTHERS` - Catch-all exception

### Comparison

| Aspect | EBS Script | Oracle PL/SQL |
|--------|-----------|---------------|
| Keyword | `try { } exceptions { }` | `BEGIN ... EXCEPTION ... END;` |
| Handler syntax | `when ERROR_TYPE { }` | `WHEN exception_name THEN` |
| Catch-all | `when ANY_ERROR` | `WHEN OTHERS THEN` |
| Error message | `when ERROR(msg)` | `SQLERRM` function |
| Error code | Not available | `SQLCODE` function |
| Re-raise | Not supported | `RAISE;` |
| User-defined | Not supported | `EXCEPTION`, `RAISE` |

---

## Database Operations

### Connecting to Database

**EBS Script:**
```javascript
// Basic connection
connect db = "jdbc:oracle:thin:@localhost:1521:xe";

// With JSON configuration
connect myDb = {
    "url": "jdbc:oracle:thin:@localhost:1521:xe",
    "username": "scott",
    "password": "tiger"
};

// Use connection block
use db {
    // Database operations here
}

// Close connection
close connection db;
```

**Oracle PL/SQL:**
```sql
-- PL/SQL runs inside the database, so no connection needed
-- For remote connections, use database links:
CREATE DATABASE LINK remote_db
CONNECT TO username IDENTIFIED BY password
USING 'remote_tns_entry';

-- Access remote data
SELECT * FROM employees@remote_db;
```

### Cursor Operations

**EBS Script:**
```javascript
connect db = "jdbc:oracle:thin:@localhost:1521:xe";

use db {
    // Declare cursor
    cursor empCursor = select emp_id, name, salary 
                       from employees 
                       where department = 'IT';
    
    // Open cursor
    open empCursor();
    
    // Fetch rows
    while call empCursor.hasNext() {
        var row = call empCursor.next();
        print row.name + ": $" + row.salary;
    }
    
    // Close cursor
    close empCursor;
}

close connection db;
```

**Oracle PL/SQL:**
```sql
DECLARE
    CURSOR emp_cursor IS
        SELECT emp_id, name, salary
        FROM employees
        WHERE department = 'IT';
    
    emp_rec emp_cursor%ROWTYPE;
BEGIN
    OPEN emp_cursor;
    
    LOOP
        FETCH emp_cursor INTO emp_rec;
        EXIT WHEN emp_cursor%NOTFOUND;
        
        DBMS_OUTPUT.PUT_LINE(emp_rec.name || ': $' || emp_rec.salary);
    END LOOP;
    
    CLOSE emp_cursor;
END;

-- Simpler cursor FOR loop
BEGIN
    FOR emp_rec IN (
        SELECT emp_id, name, salary
        FROM employees
        WHERE department = 'IT'
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(emp_rec.name || ': $' || emp_rec.salary);
    END LOOP;
END;
```

### Parameterized Cursors

**EBS Script:**
```javascript
cursor deptCursor = select * from employees where department = :dept;
open deptCursor(dept = "Sales");

while call deptCursor.hasNext() {
    var row = call deptCursor.next();
    print row.name;
}

close deptCursor;
```

**Oracle PL/SQL:**
```sql
DECLARE
    CURSOR dept_cursor (p_dept VARCHAR2) IS
        SELECT * FROM employees WHERE department = p_dept;
BEGIN
    FOR rec IN dept_cursor('Sales') LOOP
        DBMS_OUTPUT.PUT_LINE(rec.name);
    END LOOP;
END;
```

### Key Differences

| Aspect | EBS Script | Oracle PL/SQL |
|--------|-----------|---------------|
| Connection | Explicit connection required | Implicit (runs in DB) |
| Cursor syntax | `cursor name = SQL` | `CURSOR name IS SQL` |
| Open cursor | `open cursorName()` | `OPEN cursor_name` |
| Fetch next | `call cursor.next()` | `FETCH cursor INTO vars` |
| Check more rows | `call cursor.hasNext()` | `cursor%NOTFOUND` |
| Cursor for loop | `foreach` with cursor | `FOR rec IN cursor LOOP` |
| Bind parameters | `:param` with open() | `(param type)` in cursor |

---

## Arrays and Collections

### EBS Script Arrays

```javascript
// Fixed-size array
var numbers: int[5];
numbers[0] = 10;
numbers[1] = 20;

// Dynamic array
var names: string[*];
call array.expand(names, 10);

// Array literal
var fruits = ["apple", "banana", "cherry"];

// Access elements
print fruits[0];      // "apple"
print fruits.length;  // 3

// Multi-dimensional array
var matrix: int[3, 3];
matrix[0, 0] = 1;

// Iterate over array
foreach fruit in fruits {
    print fruit;
}
```

### Oracle PL/SQL Collections

```sql
DECLARE
    -- Associative array (index by)
    TYPE num_array_t IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    numbers num_array_t;
    
    -- Nested table
    TYPE names_t IS TABLE OF VARCHAR2(50);
    names names_t := names_t('apple', 'banana', 'cherry');
    
    -- VARRAY (fixed size)
    TYPE fixed_array_t IS VARRAY(5) OF NUMBER;
    fixed_nums fixed_array_t := fixed_array_t(10, 20, 30);
BEGIN
    -- Assign values
    numbers(1) := 10;
    numbers(2) := 20;
    
    -- Access elements
    DBMS_OUTPUT.PUT_LINE(names(1));  -- 'apple'
    DBMS_OUTPUT.PUT_LINE(names.COUNT); -- 3
    
    -- Iterate
    FOR i IN 1..names.COUNT LOOP
        DBMS_OUTPUT.PUT_LINE(names(i));
    END LOOP;
    
    -- Nested table methods
    names.EXTEND;  -- Add element
    names(4) := 'date';
    names.DELETE(2);  -- Remove element
END;
```

### Comparison

| Feature | EBS Script | Oracle PL/SQL |
|---------|-----------|---------------|
| Fixed array | `type[size]` | `VARRAY(size)` |
| Dynamic array | `type[*]` | `TABLE OF`, `INDEX BY` |
| Index start | 0 | 1 |
| Length property | `.length` | `.COUNT` |
| Add element | `call array.add()` | `.EXTEND` |
| Remove element | `call array.remove()` | `.DELETE(index)` |
| Array literal | `[a, b, c]` | `type_name(a, b, c)` |
| Multi-dimensional | `type[x, y]` | Nested collections |

---

## JSON Support

### EBS Script

```javascript
// Declare JSON
var person: json = {
    "name": "Alice",
    "age": 30,
    "address": {
        "city": "New York",
        "zip": "10001"
    }
};

// Access values
var name = call json.get(person, "name");
var age = call json.getint(person, "age");
var city = call json.get(person, "address.city");

// Modify values
call json.set(person, "age", 31);
call json.set(person, "email", "alice@example.com");

// Parse JSON string
var jsonStr = '{"key": "value"}';
var obj = call json.jsonfromstring(jsonStr);

// JSON to string
var str = call string.tostring(person);
```

### Oracle PL/SQL

```sql
DECLARE
    v_json JSON;
    v_name VARCHAR2(100);
    v_age NUMBER;
    v_json_str CLOB := '{"name": "Alice", "age": 30}';
BEGIN
    -- Parse JSON
    v_json := JSON(v_json_str);
    
    -- Access values using JSON_VALUE
    SELECT JSON_VALUE(v_json_str, '$.name')
    INTO v_name
    FROM dual;
    
    -- Using JSON_OBJECT to create JSON
    v_json_str := JSON_OBJECT(
        'name' VALUE 'Alice',
        'age' VALUE 30
    );
    
    -- JSON_QUERY for nested objects
    SELECT JSON_QUERY(v_json_str, '$.address')
    INTO v_json_str
    FROM dual;
END;

-- JSON in tables (21c+)
CREATE TABLE employees (
    id NUMBER,
    data JSON
);

INSERT INTO employees VALUES (1, '{"name": "Alice", "dept": "IT"}');

SELECT e.data.name
FROM employees e
WHERE e.data.dept = 'IT';
```

### Comparison

| Feature | EBS Script | Oracle PL/SQL |
|---------|-----------|---------------|
| JSON type | `json` | `JSON` (21c), `CLOB` |
| Create JSON | `{"key": "value"}` | `JSON_OBJECT()` |
| Access value | `call json.get()` | `JSON_VALUE()` |
| Parse string | `call json.jsonfromstring()` | `JSON()` constructor |
| Modify JSON | `call json.set()` | `JSON_TRANSFORM()` (21c) |
| Native support | Yes | Partial (varies by version) |

---

## String Operations

### Comparison of Common Operations

| Operation | EBS Script | Oracle PL/SQL |
|-----------|-----------|---------------|
| Length | `text.length` | `LENGTH(text)` |
| Substring | `call str.substring(text, 0, 5)` | `SUBSTR(text, 1, 5)` |
| Upper case | `call str.toUpper(text)` | `UPPER(text)` |
| Lower case | `call str.toLower(text)` | `LOWER(text)` |
| Trim | `call str.trim(text)` | `TRIM(text)` |
| Find | `call str.indexOf(text, "sub")` | `INSTR(text, 'sub')` |
| Replace | `call str.replace(text, "a", "b")` | `REPLACE(text, 'a', 'b')` |
| Split | `call str.split(text, ",")` | `REGEXP_SUBSTR` in loop |
| Concatenate | `str1 + str2` | `str1 || str2` |

### Examples

**EBS Script:**
```javascript
var text: string = "Hello, World!";

// Common operations
var len = text.length;                              // 13
var upper = call str.toUpper(text);                 // "HELLO, WORLD!"
var sub = call str.substring(text, 0, 5);           // "Hello"
var replaced = call str.replace(text, "World", "EBS"); // "Hello, EBS!"
var pos = call str.indexOf(text, "World");          // 7

// Split and join
var parts = call str.split("a,b,c", ",");           // ["a", "b", "c"]
var joined = call str.join(parts, "-");             // "a-b-c"
```

**Oracle PL/SQL:**
```sql
DECLARE
    v_text VARCHAR2(100) := 'Hello, World!';
    v_len NUMBER;
    v_upper VARCHAR2(100);
    v_sub VARCHAR2(100);
BEGIN
    v_len := LENGTH(v_text);                        -- 13
    v_upper := UPPER(v_text);                       -- 'HELLO, WORLD!'
    v_sub := SUBSTR(v_text, 1, 5);                  -- 'Hello' (1-based index!)
    v_text := REPLACE(v_text, 'World', 'PL/SQL');   -- 'Hello, PL/SQL!'
    
    -- Finding position (returns 0 if not found)
    IF INSTR(v_text, 'World') > 0 THEN
        DBMS_OUTPUT.PUT_LINE('Found!');
    END IF;
END;
```

---

## EBS-Specific Features

These features are available in EBS but have no direct PL/SQL equivalent:

### 1. JavaFX UI Screens

```javascript
screen loginScreen = {
    "title": "Login",
    "width": 400,
    "height": 300,
    "vars": [
        {
            "name": "username",
            "type": "string",
            "display": {"type": "textfield", "labelText": "Username:"}
        },
        {
            "name": "password",
            "type": "string",
            "display": {"type": "passwordfield", "labelText": "Password:"}
        }
    ]
};

show screen loginScreen;
var user = loginScreen.username;
```

### 2. HTTP/REST Operations

```javascript
var response = call http.getjson("https://api.example.com/data");
var users = call http.postjson(url, headers, requestBody);
```

### 3. File I/O (Direct)

```javascript
var content = call file.readtextfile("data.txt");
call file.writetextfile("output.txt", content);
```

### 4. Interactive Console

```javascript
// Console commands
/open script.ebs
/save backup.ebs
/clear
/help
```

### 5. Type Inference

```javascript
var count = 42;           // Automatically inferred as int
var message = "Hello";    // Automatically inferred as string
```

### 6. Shorthand Function Calls

```javascript
var result = #math.sqrt(16);     // Using # instead of 'call'
```

---

## PL/SQL Features Not in EBS

These PL/SQL features are not available or work differently in EBS:

### 1. Stored Procedures in Database

```sql
-- PL/SQL: Stored in database
CREATE OR REPLACE PROCEDURE update_salary(
    p_emp_id IN NUMBER,
    p_amount IN NUMBER
) AS
BEGIN
    UPDATE employees SET salary = salary + p_amount WHERE emp_id = p_emp_id;
    COMMIT;
END;
```

### 2. Triggers

```sql
-- PL/SQL: Automatic execution on events
CREATE OR REPLACE TRIGGER audit_changes
    AFTER INSERT OR UPDATE ON employees
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log VALUES (SYSDATE, USER, :NEW.emp_id);
END;
```

### 3. Packages

```sql
-- PL/SQL: Modular code organization
CREATE OR REPLACE PACKAGE emp_pkg AS
    FUNCTION get_salary(p_emp_id NUMBER) RETURN NUMBER;
    PROCEDURE update_salary(p_emp_id NUMBER, p_amount NUMBER);
END;
/

CREATE OR REPLACE PACKAGE BODY emp_pkg AS
    -- Implementation
END;
```

### 4. Bulk Operations

```sql
-- PL/SQL: Efficient bulk processing
DECLARE
    TYPE emp_ids_t IS TABLE OF NUMBER;
    v_emp_ids emp_ids_t;
BEGIN
    SELECT emp_id BULK COLLECT INTO v_emp_ids FROM employees;
    
    FORALL i IN 1..v_emp_ids.COUNT
        UPDATE salaries SET amount = amount * 1.1 WHERE emp_id = v_emp_ids(i);
END;
```

### 5. Transaction Control

```sql
-- PL/SQL: Explicit transaction management
BEGIN
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
```

### 6. OUT Parameters

```sql
-- PL/SQL: Return multiple values
CREATE PROCEDURE get_emp_details(
    p_id IN NUMBER,
    p_name OUT VARCHAR2,
    p_salary OUT NUMBER
) AS
BEGIN
    SELECT name, salary INTO p_name, p_salary 
    FROM employees WHERE emp_id = p_id;
END;
```

### 7. Autonomous Transactions

```sql
-- PL/SQL: Independent transaction
CREATE PROCEDURE log_error(p_msg VARCHAR2) AS
    PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
    INSERT INTO error_log VALUES (SYSDATE, p_msg);
    COMMIT;  -- Commits independently
END;
```

---

## Migration Examples

### Example 1: Simple Calculation

**Oracle PL/SQL:**
```sql
DECLARE
    v_total NUMBER := 0;
    v_tax_rate NUMBER := 0.08;
    v_price NUMBER := 100;
BEGIN
    v_total := v_price * (1 + v_tax_rate);
    DBMS_OUTPUT.PUT_LINE('Total: $' || v_total);
END;
```

**EBS Script:**
```javascript
var total: double = 0;
var taxRate: double = 0.08;
var price: double = 100;

total = price * (1 + taxRate);
print "Total: $" + total;
```

### Example 2: Looping and Conditionals

**Oracle PL/SQL:**
```sql
DECLARE
    v_grade CHAR(1);
    v_score NUMBER := 85;
BEGIN
    IF v_score >= 90 THEN
        v_grade := 'A';
    ELSIF v_score >= 80 THEN
        v_grade := 'B';
    ELSIF v_score >= 70 THEN
        v_grade := 'C';
    ELSE
        v_grade := 'F';
    END IF;
    
    DBMS_OUTPUT.PUT_LINE('Grade: ' || v_grade);
END;
```

**EBS Script:**
```javascript
var grade: string;
var score: int = 85;

if score >= 90 then {
    grade = "A";
} else if score >= 80 then {
    grade = "B";
} else if score >= 70 then {
    grade = "C";
} else {
    grade = "F";
}

print "Grade: " + grade;
```

### Example 3: Working with Collections

**Oracle PL/SQL:**
```sql
DECLARE
    TYPE name_list_t IS TABLE OF VARCHAR2(50);
    v_names name_list_t := name_list_t('Alice', 'Bob', 'Charlie');
BEGIN
    FOR i IN 1..v_names.COUNT LOOP
        DBMS_OUTPUT.PUT_LINE('Hello, ' || v_names(i) || '!');
    END LOOP;
END;
```

**EBS Script:**
```javascript
var names = ["Alice", "Bob", "Charlie"];

foreach name in names {
    print "Hello, " + name + "!";
}

// Or using traditional for loop
for (var i: int = 0; i < names.length; i++) {
    print "Hello, " + names[i] + "!";
}
```

### Example 4: Database Query

**Oracle PL/SQL:**
```sql
DECLARE
    v_total_salary NUMBER := 0;
    v_emp_count NUMBER := 0;
BEGIN
    FOR emp IN (SELECT salary FROM employees WHERE department = 'IT') LOOP
        v_total_salary := v_total_salary + emp.salary;
        v_emp_count := v_emp_count + 1;
    END LOOP;
    
    IF v_emp_count > 0 THEN
        DBMS_OUTPUT.PUT_LINE('Average salary: $' || (v_total_salary / v_emp_count));
    END IF;
END;
```

**EBS Script:**
```javascript
connect db = "jdbc:oracle:thin:@localhost:1521:xe";

use db {
    cursor empCursor = select salary from employees where department = 'IT';
    open empCursor();
    
    var totalSalary: double = 0;
    var empCount: int = 0;
    
    while call empCursor.hasNext() {
        var row = call empCursor.next();
        totalSalary = totalSalary + row.salary;
        empCount++;
    }
    
    close empCursor;
    
    if empCount > 0 then {
        print "Average salary: $" + (totalSalary / empCount);
    }
}

close connection db;
```

---

## Summary of Key Differences

| Category | EBS Script | Oracle PL/SQL |
|----------|-----------|---------------|
| **Philosophy** | Scripting, modern syntax | Database programming |
| **Environment** | Standalone JVM | Inside Oracle Database |
| **Syntax style** | C/JavaScript-like | Ada-like |
| **Block delimiters** | `{ }` | `BEGIN...END` |
| **Assignment** | `=` | `:=` |
| **Equality** | `==` | `=` |
| **String concat** | `+` | `||` |
| **Type position** | After variable: `var x: int` | Before variable: `x NUMBER` |
| **Function calls** | `call func()` or `#func()` | `func()` |
| **Array index** | 0-based | 1-based |
| **UI support** | Built-in JavaFX | None |
| **File I/O** | Direct built-in | `UTL_FILE` package |
| **HTTP/REST** | Built-in | `UTL_HTTP` or APEX |
| **JSON** | Native | Version-dependent |
| **Transaction control** | Limited | Full support |

---

## Version Information

- **EBS Script**: Version 1.0-SNAPSHOT
- **Oracle PL/SQL Reference**: Oracle Database 19c and later

For the complete EBS syntax reference, see [EBS_SCRIPT_SYNTAX.md](EBS_SCRIPT_SYNTAX.md).
