# Binary Datatype Implementation Summary

## Overview
Added a new `binary` datatype to the EBS scripting language for efficient handling of binary data such as images, files, and other byte-based content. The binary datatype provides factory functions for creation and supports variable chain methods for instance operations.

## Datatype Declaration
```ebs
var binaryData: binary;
var imageBytes: binary = binary.fromBase64("...");
```

## Function Types

The binary datatype distinguishes between two types of functions:

### Datatype Functions (Static)
Factory methods that create new binary instances. These are called on the `binary` type itself:

- `binary.fromBase64(string)` - Creates binary from Base64 string
- `binary.fromByteArray(array.byte)` - Creates binary from byte array

### Variable Chain Functions
Instance methods/properties called on binary variables. These operate on existing binary data:

- `.length` - Property access (returns int)
- `.get(index)` - Get byte at index
- `.set(index, value)` - Set byte at index  
- `.slice(start, end?)` - Extract portion
- `.concat(other)` - Concatenate with another binary
- `.toBase64()` - Convert to Base64 string

**Note:** Variable chain methods require method-style call support in the parser. Currently, only property access (`.length`) is fully supported.

## Available Built-in Functions

### 1. `binary.fromBase64(string) -> binary` (Datatype Function)
Creates binary data from a Base64-encoded string.

```ebs
var data: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
// data now contains the decoded bytes: "Hello World"
```

### 2. `binary.fromByteArray(array.byte) -> binary` (Datatype Function)
Creates binary data from an EBS byte array.

```ebs
var arr: array.byte[3] = [1, 2, 3];
var data: binary = binary.fromByteArray(arr);
```

### 3. `.length` (Property - Variable Chain)
Returns the length of the binary data in bytes.

```ebs
var data: binary = binary.fromBase64("SGVsbG8=");
var len: int = data.length;  // Returns: 5
```

### Variable Chain Methods (Require Parser Support)

The following methods should be called on the binary variable but require parser enhancements for method-style calls:

- `data.get(index)` - Get byte at specified index
- `data.set(index, value)` - Set byte at specified index
- `data.slice(start, end?)` - Extract portion of binary
- `data.concat(other)` - Concatenate two binary arrays
- `data.toBase64()` - Convert to Base64 string

## Use Cases

### Creating Binary Data
```ebs
// From Base64 string
var imageBytes: binary = binary.fromBase64(base64ImageString);

// From byte array
var arr: array.byte[10];
// ... populate array ...
var binData: binary = binary.fromByteArray(arr);
```

### Checking Binary Size
```ebs
var data: binary = binary.fromBase64("SGVsbG8=");
var size: int = data.length;
print "Size: " + size + " bytes";
```

## Technical Details

- **Internal Representation**: Uses Java's native `byte[]` for efficient memory usage
- **Type Safety**: Type-checked at compile time and runtime
- **Null Handling**: Factory functions handle null inputs gracefully
- **Immutability**: Factory functions return new binary instances

## Differences from array.byte

While `binary` and `array.byte` both work with byte data, they serve different purposes:

| Feature | binary | array.byte |
|---------|--------|------------|
| Purpose | Binary data (images, files) | General-purpose byte arrays |
| Type | `byte[]` | `ArrayFixedByte` |
| Factory Methods | `binary.fromBase64()`, `binary.fromByteArray()` | Array literals, `array.*` functions |
| Best For | File I/O, images, serialization | Numeric computations, buffers |

## Testing

Tests are available in:
- **Unit Tests**: `ScriptInterpreter/src/test/java/com/eb/script/test/TestBinaryDatatype.java`
- **Example Script**: `test_binary_datatype.ebs`

Run tests:
```bash
cd ScriptInterpreter
mvn test-compile
mvn exec:java -Dexec.mainClass="com.eb.script.test.TestBinaryDatatype" -Dexec.classpathScope="test"
```

Run example:
```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="../test_binary_datatype.ebs"
```

## Files Modified

- `ScriptInterpreter/src/main/java/com/eb/script/token/DataType.java` - Added BINARY enum and type checking
- `ScriptInterpreter/src/main/java/com/eb/script/token/ebs/EbsTokenType.java` - Added BINARY token
- `ScriptInterpreter/src/main/java/com/eb/script/parser/Parser.java` - Added BINARY type recognition
- `ScriptInterpreter/src/main/java/com/eb/util/Util.java` - Added BINARY type validation
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java` - Registered datatype functions
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsSystem.java` - Implemented factory methods
- `ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt` - Updated grammar documentation

## Design Rationale

The separation between datatype functions and variable chain functions eliminates ambiguity:

- **Datatype functions** (`binary.fromBase64`, `binary.fromByteArray`) can only be called as static methods since they create new instances
- **Variable chain functions** (`.length`, `.get()`, `.slice()`, etc.) operate on existing instances and should be called on the variable

This design makes the API clearer and aligns with object-oriented principles where factory methods are static and instance methods are called on objects.

