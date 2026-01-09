# 2D Array Fix for Specialized Types

## Issue
Previously, declaring multi-dimensional arrays with specialized types would fail:

```javascript
var bitmap2d: array.bitmap[10, 64];  // ❌ Failed with error
```

**Error Message:**
```
Cannot convert ArrayFixedByte to byte
```

## Root Cause
When creating multi-dimensional arrays:
1. Parent arrays stored the specialized type (e.g., `BITMAP`)
2. Parent arrays needed to store child `ArrayDef` objects
3. The `set()` method tried to convert child arrays to primitive type
4. Conversion failed: cannot convert `ArrayFixedByte` to `byte`

## Solution
Modified `ArrayFixed` and `ArrayDynamic` classes to skip type conversion when setting `ArrayDef` objects (nested arrays). These are now stored as-is in multi-dimensional arrays.

## Now Works ✅

### 2D Arrays with Specialized Types
```javascript
// Bitmap arrays - useful for bit flags and binary data
var bitmap2d: array.bitmap[10, 64];
bitmap2d[0, 0] = 1;
bitmap2d[9, 63] = 255;

// Intmap arrays - useful for packed integer fields
var intmap2d: array.intmap[5, 10];
intmap2d[0, 0] = 1000;
intmap2d[4, 9] = 2000;

// Integer arrays with primitive storage
var int2d: array.int[8, 8];  // Chess board size
int2d[0, 0] = 1;
int2d[7, 7] = 64;

// Byte arrays
var byte2d: array.byte[4, 16];
byte2d[0, 0] = 10;
byte2d[3, 15] = 100;
```

### 3D Arrays Also Work
```javascript
var bitmap3d: array.bitmap[3, 3, 3];
bitmap3d[0, 0, 0] = 1;
bitmap3d[2, 2, 2] = 27;
```

## Use Cases

### 1. Chess Board with Bitmaps
Instead of using a flat 1D array:
```javascript
// Old approach (still valid)
var board: array.bitmap[64];  // Flat representation

// New approach (more intuitive)
var board: array.bitmap[8, 8];  // Natural 2D representation
board[row, col] = pieceValue;
```

### 2. Image Processing
```javascript
// RGB pixel data
var pixels: array.byte[height, width, 3];  // 3 = R, G, B
pixels[y, x, 0] = redValue;
pixels[y, x, 1] = greenValue;
pixels[y, x, 2] = blueValue;
```

### 3. Game State Grids
```javascript
// Tile map for a game
var tileMap: array.int[32, 32];  // 32x32 grid
tileMap[row, col] = tileType;

// Collision flags
var collisionMap: array.bitmap[32, 32];
collisionMap[row, col] = isBlocked ? 1 : 0;
```

### 4. Matrix Operations
```javascript
// Mathematical matrices
var matrix: array.int[rows, cols];
var result: array.int[rows, cols];

// Perform operations
for (var i: int = 0; i < rows; i++) {
    for (var j: int = 0; j < cols; j++) {
        result[i, j] = matrix[i, j] * 2;
    }
}
```

## Testing
Run the comprehensive test suite:
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" \
  -Dexec.args="scripts/test/test_2d_specialized_arrays.ebs"
```

## Technical Details

### Files Modified
- `ScriptInterpreter/src/main/java/com/eb/script/arrays/ArrayFixed.java`
- `ScriptInterpreter/src/main/java/com/eb/script/arrays/ArrayDynamic.java`

### Key Change
```java
// Before: Always converted values
if (!dataType.isDataType(value)) {
    value = dataType.convertValue(value);
}
elements[index] = value;

// After: Skip conversion for nested arrays
if (!(value instanceof ArrayDef) && !dataType.isDataType(value)) {
    value = dataType.convertValue(value);
}
elements[index] = value;
```

## Backward Compatibility
✅ **All existing code continues to work**
- 1D arrays unaffected
- Array literals still work
- Dynamic arrays still work
- All existing tests pass

## Performance
No performance impact:
- Same storage as before
- Only adds a lightweight `instanceof` check
- Specialized types (ArrayFixedByte, ArrayFixedInt) still use primitive storage for leaf dimensions

---

**Version:** December 2024  
**EBS Version:** 1.0.6+
