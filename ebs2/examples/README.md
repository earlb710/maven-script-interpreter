# EBS2 Example Scripts

This directory contains comprehensive example scripts demonstrating all features of the EBS2 language. Each example is self-contained and fully documented.

## Example Files

### 01_variables_and_types.ebs
**Topics Covered:**
- Variable declarations with type annotations
- Basic data types (number, text, flag)
- Type checking with `typeof` operator
- Arrays and array ranges (`1..100`, `-5..5`, `43..79`)
- Type conversions (`.toNumber()`, `.toText()`, `.toFlag()`, `.toInt()`)

**Key Concepts:**
- Type safety with annotations
- Postfix `typeof` operator
- Range syntax for array creation
- Chainable type conversion methods

---

### 02_operators.ebs
**Topics Covered:**
- Arithmetic operators (`+`, `-`, `*`, `/`, `mod`)
- Increment/decrement operators (`++`, `--`)
- Comparison operators (`=`, `<>`, `<`, `>`, `<=`, `>=`)
- Logical operators (`and`, `or`, `not`)

**Key Concepts:**
- Pre and post increment/decrement
- Boolean logic
- Operator precedence

---

### 03_control_flow.ebs
**Topics Covered:**
- `if/then/else` statements
- `case` statement for value matching
- `while` loops
- `for` loops with range and step
- Optional `end` for single-line statements

**Key Concepts:**
- Multi-level conditionals
- Case statement with primitive types (number, text, flag)
- Loop iteration patterns
- Array traversal

---

### 04_text_operations.ebs
**Topics Covered:**
- Text properties (`.length`, `.isEmpty`)
- Case conversion (`.toUpper()`, `.toLower()`)
- Substring operations (`.substring()`, `.head()`, `.tail()`, `.lead()`)
- Search methods (`.find()`, `.findFirst()`, `.findLast()`, `.contains()`)
- Replace methods (`.replaceFirst()`, `.replaceAll()`, `.replaceLast()`)
- Padding (`.padLeft()`, `.padRight()`, `.padCenter()`)
- Trimming (`.trim()`, `.trimLeft()`, `.trimRight()`, `.trimStart()`, `.trimEnd()`)
- Split and join operations
- Method chaining

**Key Concepts:**
- Fluent API design
- String manipulation
- Method chaining for complex transformations
- Optional parentheses for methods with no parameters

---

### 05_array_operations.ebs
**Topics Covered:**
- Array creation and initialization
- Array properties (`.length`, `.isEmpty`)
- Array access and indexing (0-based)
- Search methods (`.find()`, `.findFirst()`, `.findLast()`, `.contains()`)
- Manipulation methods (`.append()`, `.add()`, `.remove()`)
- Sorting (`.sort()`, `.reverseSort()`)
- Slicing (`.copy()`)
- Replace operations
- Method chaining

**Key Concepts:**
- 0-based indexing
- Overloaded methods (`.add(value)` vs `.add(index, value)`)
- Functional array operations
- Chaining for data transformation

---

### 06_functions.ebs
**Topics Covered:**
- Function definitions with `return` keyword
- Procedures (functions with no return value)
- Parameters with type annotations
- Return values with type annotations
- Recursive functions
- Functions with arrays
- Functions returning arrays
- Function composition

**Key Concepts:**
- Function vs procedure syntax
- Type-safe parameters and returns
- Recursion support
- Inline function calls

---

### 07_records.ebs
**Topics Covered:**
- Record type definitions
- Record literals
- Record field access
- Record extension with `extends` keyword
- Type checking with inheritance
- Anonymous records
- Nested records
- Records with arrays

**Key Concepts:**
- Object-oriented programming patterns
- Type suffix convention (`PersonType`, `EmployeeType`)
- Non-overwriting field inheritance
- Type hierarchy with `typeof`
- Complex data structures

---

### 08_dates.ebs
**Topics Covered:**
- Date creation from text
- Date arithmetic (addition/subtraction with numbers)
- Date subtraction returning fractional days
- Date comparisons
- Practical date calculations
- Time component handling

**Key Concepts:**
- Date operators (`date + number`, `date - date`)
- Fractional days (0.5 = 12 hours)
- Date range checking
- Duration calculations

---

### 09_comprehensive_example.ebs
**Complete Application Demo:**
A fully-featured task management system combining:
- Record types with extension
- Functions and procedures
- Array operations
- Date calculations
- Control flow (if/else, case, loops)
- Type checking
- Method chaining
- Complex business logic

**Key Concepts:**
- Real-world application structure
- Integration of multiple language features
- Data-driven programming
- Statistics and reporting

---

## Running the Examples

To run any example script:

```bash
ebs2 examples/01_variables_and_types.ebs
```

Or specify the full path:

```bash
ebs2 /path/to/ebs2/examples/01_variables_and_types.ebs
```

## Learning Path

Recommended order for learning:

1. **01_variables_and_types.ebs** - Start with basics
2. **02_operators.ebs** - Understand operators
3. **03_control_flow.ebs** - Control structures
4. **04_text_operations.ebs** - Text manipulation
5. **05_array_operations.ebs** - Array handling
6. **06_functions.ebs** - Function concepts
7. **07_records.ebs** - Data structures
8. **08_dates.ebs** - Date operations
9. **09_comprehensive_example.ebs** - Complete application

## Key Language Features Demonstrated

### Modern Syntax
- Method chaining: `"hello".toUpper().reverse()`
- Optional parentheses: `{3,1,2}.sort.reverse`
- Postfix operators: `x typeof number`

### Type System
- Type annotations: `var age as number`
- Type checking: `if value typeof text then`
- Type conversions: `.toNumber()`, `.toText()`, `.toFlag()`
- Type inheritance with `extends`

### Data Structures
- Arrays with 0-based indexing
- Array ranges: `1..100`
- Records with inheritance
- Nested data structures

### Operators
- Increment/decrement: `++`, `--`
- Date arithmetic: `date1 - date2`
- Logical: `and`, `or`, `not`

### Control Flow
- `if/then/else` with optional `end`
- `case` statement for value matching
- `while` and `for` loops

### Functions
- Type-safe parameters and returns
- Procedures for side effects
- Recursive functions
- Higher-order patterns

## Additional Resources

- **EBS2_LANGUAGE_SPEC.md** - Complete language specification
- **EBS2_QUICK_REFERENCE.md** - Quick syntax reference
- **EBS2_QUICK_START_GUIDE.md** - Getting started guide
- **EBS1_VS_EBS2_COMPARISON.md** - Migration guide from EBS1

## Notes

- All examples use `//` for comments (single-line only)
- All `end` statements specify what they end (e.g., `end if`, `end function`)
- All record types use `Type` suffix naming convention
- Optional `end` for single-command blocks
- 0-based array indexing throughout
- Method chaining is encouraged for readability

## Contributing

When adding new examples:
1. Follow the existing naming convention (`##_feature_name.ebs`)
2. Include comprehensive comments
3. Demonstrate one main concept per example
4. Show both basic and advanced usage
5. Update this README with the new example
