# Binary Datatype Implementation Summary

## Overview
Added a new `binary` datatype to the EBS scripting language for efficient handling of binary data such as images, files, and other byte-based content. The binary datatype provides a set of built-in functions similar to what you would expect from a byte array, but optimized for binary data operations.

## Datatype Declaration
```ebs
var binaryData: binary;
var imageBytes: binary = binary.fromBase64("...");
```

## Built-in Functions

### 1. `binary.length(binary) -> int`
Returns the length of the binary data in bytes.

```ebs
var data: binary = binary.fromBase64("SGVsbG8=");
var len: int = binary.length(data);  // Returns: 5
```

### 2. `binary.get(binary, index) -> byte`
Gets the byte value at the specified index (0-based).

```ebs
var data: binary = binary.fromBase64("SGVsbG8=");
var firstByte: byte = binary.get(data, 0);  // Returns: 72 ('H')
```

### 3. `binary.set(binary, index, value)`
Sets the byte value at the specified index. Returns null (use as call statement).

```ebs
var data: binary = binary.fromBase64("AAAA");
call binary.set(data, 0, 72);  // Modifies first byte to 72
```

### 4. `binary.slice(binary, start, end?) -> binary`
Extracts a portion of the binary data. If `end` is not provided, slices to the end.

```ebs
var data: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
var slice1: binary = binary.slice(data, 0, 5);  // First 5 bytes
var slice2: binary = binary.slice(data, 6);     // From byte 6 to end
```

### 5. `binary.concat(binary1, binary2) -> binary`
Concatenates two binary data arrays into a single binary array.

```ebs
var part1: binary = binary.fromBase64("SGVs");
var part2: binary = binary.fromBase64("bG8=");
var combined: binary = binary.concat(part1, part2);
```

### 6. `binary.toBase64(binary) -> string`
Converts binary data to a Base64-encoded string.

```ebs
var data: binary = binary.fromBase64("SGVsbG8=");
var encoded: string = binary.toBase64(data);  // Returns: "SGVsbG8="
```

### 7. `binary.fromBase64(string) -> binary`
Creates binary data from a Base64-encoded string.

```ebs
var data: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
// data now contains the decoded bytes: "Hello World"
```

### 8. `binary.toByteArray(binary) -> array.byte`
Converts binary data to an EBS byte array for compatibility with existing array operations.

```ebs
var data: binary = binary.fromBase64("AQIDBA==");
var arr = binary.toByteArray(data);
// arr is now an array.byte with length 4
```

### 9. `binary.fromByteArray(array.byte) -> binary`
Creates binary data from an EBS byte array.

```ebs
var arr: array.byte[3] = [1, 2, 3];
var data: binary = binary.fromByteArray(arr);
```

## Use Cases

### Working with Image Data
```ebs
// Load image data (e.g., from file.readBase64)
var imageBytes: binary = binary.fromBase64(base64ImageString);

// Get image size
var size: int = binary.length(imageBytes);
print "Image size: " + size + " bytes";

// Extract image header
var header: binary = binary.slice(imageBytes, 0, 100);
```

### Binary File Operations
```ebs
// Read binary file content
var fileContent: binary = binary.fromBase64(encodedContent);

// Split into chunks
var chunk1: binary = binary.slice(fileContent, 0, 1024);
var chunk2: binary = binary.slice(fileContent, 1024, 2048);

// Combine chunks
var combined: binary = binary.concat(chunk1, chunk2);
```

### Data Encoding/Decoding
```ebs
// Encode binary data for transmission
var data: binary = binary.fromBase64("SGVsbG8=");
var encoded: string = binary.toBase64(data);
// Send encoded string over network

// Decode received data
var received: binary = binary.fromBase64(encodedString);
```

## Technical Details

- **Internal Representation**: Uses Java's native `byte[]` for efficient memory usage
- **Type Safety**: Type-checked at compile time and runtime
- **Null Handling**: Binary functions handle null inputs gracefully
- **Bounds Checking**: Index operations validate bounds and throw descriptive errors
- **Immutability**: Most operations return new binary instances (except `binary.set`)

## Differences from array.byte

While `binary` and `array.byte` both work with byte data, they serve different purposes:

| Feature | binary | array.byte |
|---------|--------|------------|
| Purpose | Binary data (images, files) | General-purpose byte arrays |
| Type | `byte[]` | `ArrayFixedByte` |
| Base64 Support | Built-in (`binary.toBase64`) | Via `array.base64encode` |
| Slicing | Built-in (`binary.slice`) | Manual indexing |
| Concatenation | Built-in (`binary.concat`) | Via `array.add` |
| Best For | File I/O, images, serialization | Numeric computations, buffers |

## Testing

A comprehensive test suite is available:
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
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java` - Registered 9 binary functions
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsSystem.java` - Implemented binary functions
- `ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt` - Updated grammar documentation

## Future Enhancements

Possible additions for the binary datatype:
- `binary.fill(binary, value)` - Fill all bytes with a value
- `binary.equals(binary1, binary2)` - Compare two binary arrays
- `binary.fromHex(string)` / `binary.toHex(binary)` - Hex encoding/decoding
- `binary.indexOf(binary, byte)` - Find first occurrence of a byte
- `binary.reverse(binary)` - Reverse byte order
