# EBS Array Syntax Guide: Understanding the Differences

## Overview

EBS Script supports two syntax forms for declaring typed arrays:

1. **Traditional Syntax**: `var x: int[6]`
2. **Enhanced Syntax**: `var x: array.int[6]`

While both syntaxes appear similar and work identically from a user perspective, they have important **internal implementation differences** that affect performance and memory usage.

---

## Key Differences

### Storage Implementation

#### Traditional Syntax: `int[6]`

```javascript
var numbers: int[6];
```

**Internal Implementation:**
- **Backing Class**: `ArrayFixed`
- **Storage Type**: `Object[]` (array of Objects)
- **Element Storage**: Boxed `Integer` objects
- **Memory Impact**: Higher memory overhead due to object wrapping

**How it works:**
- Each integer value is wrapped in an `Integer` object (boxing)
- The array stores references to these `Integer` objects
- Accessing values requires unboxing (converting `Integer` back to `int`)

#### Enhanced Syntax: `array.int[6]`

```javascript
var numbers: array.int[6];
```

**Internal Implementation:**
- **Backing Class**: `ArrayFixedInt`
- **Storage Type**: `int[]` (primitive array)
- **Element Storage**: Primitive `int` values
- **Memory Impact**: Lower memory overhead, more efficient

**How it works:**
- Values are stored directly as primitive `int` values
- No boxing/unboxing overhead
- More memory-efficient and faster access

---

## Performance Comparison

### Memory Usage

| Syntax | Storage | Memory per Element | Example: 1000 elements |
|--------|---------|-------------------|------------------------|
| `int[1000]` | `Object[]` with `Integer` objects | ~16-24 bytes | ~16-24 KB |
| `array.int[1000]` | `int[]` primitive | 4 bytes | 4 KB |

**Memory Savings**: The `array.int` syntax uses approximately **4-6x less memory** for integer arrays.

### CPU Performance

| Operation | `int[6]` | `array.int[6]` | Performance Difference |
|-----------|----------|----------------|------------------------|
| Array Access | Requires unboxing | Direct access | **array.int is faster** |
| Array Assignment | Requires boxing | Direct assignment | **array.int is faster** |
| Iteration | Repeated boxing/unboxing | Direct primitive access | **array.int is significantly faster** |

### Example: Performance Impact

```javascript
// Traditional syntax - boxing/unboxing overhead
var numbers: int[1000];
for (var i: int = 0; i < 1000; i++) {
    numbers[i] = i * 2;  // Boxing: int -> Integer
}
var sum: int = 0;
for (var i: int = 0; i < 1000; i++) {
    sum = sum + numbers[i];  // Unboxing: Integer -> int
}

// Enhanced syntax - no boxing/unboxing
var numbers: array.int[1000];
for (var i: int = 0; i < 1000; i++) {
    numbers[i] = i * 2;  // Direct primitive assignment
}
var sum: int = 0;
for (var i: int = 0; i < 1000; i++) {
    sum = sum + numbers[i];  // Direct primitive access
}
```

In the above example, the `array.int` version:
- Uses **~4x less memory**
- Runs **faster** due to no boxing/unboxing
- Generates less garbage collection pressure

---

## When to Use Each Syntax

### Use `array.int[size]` (Enhanced Syntax) When:

✅ **Performance matters**: Large arrays or frequently accessed arrays
✅ **Memory efficiency is important**: Limited memory environments
✅ **Numeric computation**: Math-heavy operations with many array accesses
✅ **Large datasets**: Processing thousands or millions of numeric values
✅ **Hot loops**: Arrays accessed repeatedly in tight loops

**Examples:**
```javascript
// Sensor data collection
var sensorReadings: array.int[10000];

// Matrix computations
var matrix: array.int[100, 100];

// Game scores or statistics
var scores: array.int[1000];

// Image pixel data
var pixels: array.int[1920, 1080];
```

### Use `int[size]` (Traditional Syntax) When:

✅ **Small arrays**: Arrays with fewer than ~100 elements where performance difference is negligible
✅ **Mixed-type needs**: You might need to store mixed types (though this defeats type safety)
✅ **Legacy code compatibility**: Maintaining existing code that uses this syntax
✅ **Brevity preferred**: Shorter syntax for quick scripts or prototypes

**Examples:**
```javascript
// Small configuration arrays
var settings: int[5];

// Short lists
var weekDays: int[7];

// Quick prototypes
var test: int[10];
```

---

## All Primitive Array Types

The enhanced `array.type` syntax supports all primitive types:

| Traditional | Enhanced | Backing Storage | Element Type |
|-------------|----------|-----------------|--------------|
| `int[n]` | `array.int[n]` | `int[]` | primitive int (32-bit) |
| `byte[n]` | `array.byte[n]` | `byte[]` | primitive byte (8-bit) |
| `long[n]` | `array.long[n]` | `Object[]` | Long object |
| `float[n]` | `array.float[n]` | `Object[]` | Float object |
| `double[n]` | `array.double[n]` | `Object[]` | Double object |
| `bool[n]` | `array.bool[n]` | `Object[]` | Boolean object |
| `string[n]` | `array.string[n]` | `Object[]` | String object |

**Note**: Currently, only `array.int` and `array.byte` have optimized primitive storage classes (`ArrayFixedInt` and `ArrayFixedByte`). Other types still use `Object[]` storage even with the enhanced syntax.

---

## Special Array Types

### Bitmap Arrays: `array.bitmap[n]`

```javascript
var flags: array.bitmap[10];
```

- **Backing Storage**: `byte[]` (same as `array.byte`)
- **Purpose**: Store bitmap data with named bit fields
- **Type Designation**: `BITMAP` (vs `BYTE`)
- **Use Case**: Bit-level field access within bytes

### Intmap Arrays: `array.intmap[n]`

```javascript
var configs: array.intmap[10];
```

- **Backing Storage**: `int[]` (same as `array.int`)
- **Purpose**: Store integer data with named bit fields (0-31)
- **Type Designation**: `INTMAP` (vs `INTEGER`)
- **Use Case**: Bit-level field access within integers

---

## Code Examples

### Example 1: Basic Usage Comparison

```javascript
// Both syntaxes work identically from user perspective
var traditional: int[5];
var enhanced: array.int[5];

traditional[0] = 10;
enhanced[0] = 10;

print traditional[0];  // Output: 10
print enhanced[0];     // Output: 10

// But internally, enhanced is more efficient
```

### Example 2: Large Array Performance

```javascript
// Inefficient for large arrays (boxing overhead)
var largeArray1: int[10000];

// Efficient for large arrays (primitive storage)
var largeArray2: array.int[10000];

// For 10,000 elements:
// - largeArray1: ~160-240 KB memory
// - largeArray2: ~40 KB memory
```

### Example 3: Numeric Computation

```javascript
// Matrix multiplication - use enhanced syntax
var matrixA: array.int[100, 100];
var matrixB: array.int[100, 100];
var result: array.int[100, 100];

// Fast primitive operations
for (var i: int = 0; i < 100; i++) {
    for (var j: int = 0; j < 100; j++) {
        var sum: int = 0;
        for (var k: int = 0; k < 100; k++) {
            sum = sum + (matrixA[i, k] * matrixB[k, j]);
        }
        result[i, j] = sum;
    }
}
```

### Example 4: Dynamic Arrays

Both syntaxes support dynamic arrays:

```javascript
// Traditional dynamic array
var dynamic1: int[*];

// Enhanced dynamic array
var dynamic2: array.int[*];

call array.expand(dynamic1, 10);
call array.expand(dynamic2, 10);
```

---

## Best Practices

### ✅ Recommended Practices

1. **Use `array.int` for numeric arrays** with more than 100 elements
2. **Use `array.byte` for byte arrays** regardless of size (always optimized)
3. **Use `array.bitmap` and `array.intmap`** for bit-field structures
4. **Be consistent** within a single project or module
5. **Consider performance** for arrays in hot loops or large datasets

### ❌ Anti-patterns

1. Don't mix syntaxes unnecessarily in the same codebase
2. Don't use traditional syntax for large numeric arrays
3. Don't optimize prematurely - use traditional syntax for small arrays if clarity is more important
4. Don't assume all `array.type` variants are optimized (currently only `int` and `byte`)

---

## Compatibility

Both syntaxes are fully compatible and work with all EBS features:

- ✅ Array literals: `var x: array.int[*] = [1, 2, 3];`
- ✅ Multi-dimensional: `var grid: array.int[10, 10];`
- ✅ Array functions: `array.fill()`, `array.expand()`, etc.
- ✅ Foreach loops: `foreach (val in myArray) { ... }`
- ✅ Type casting and conversion
- ✅ JSON serialization

---

## Summary

| Aspect | `int[6]` Traditional | `array.int[6]` Enhanced |
|--------|---------------------|------------------------|
| **Storage** | `Object[]` (boxed Integer) | `int[]` (primitive) |
| **Memory** | Higher (16-24 bytes/element) | Lower (4 bytes/element) |
| **Performance** | Slower (boxing/unboxing) | Faster (direct access) |
| **Use Case** | Small arrays, prototypes | Large arrays, performance-critical |
| **Best For** | < 100 elements | ≥ 100 elements or hot loops |

### Quick Decision Guide

```
Array size < 100 elements?
├─ Yes → Either syntax is fine (traditional is more concise)
└─ No  → Use array.int[size] for better performance

Array in a hot loop or frequently accessed?
├─ Yes → Use array.int[size]
└─ No  → Either syntax is fine

Memory constrained environment?
├─ Yes → Use array.int[size]
└─ No  → Either syntax is fine
```

---

## Related Documentation

- [EBS Script Syntax Reference](EBS_SCRIPT_SYNTAX.md) - Complete language syntax
- [README.md](../README.md) - Project overview and array features
- Example scripts:
  - `ScriptInterpreter/scripts/test/test_array_type_syntax.ebs`
  - `ScriptInterpreter/scripts/test/test_array_fixed_int.ebs`

---

**Last Updated**: December 2025  
**EBS Version**: 1.0.6+
