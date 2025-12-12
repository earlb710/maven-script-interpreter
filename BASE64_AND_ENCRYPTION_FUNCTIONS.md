# Base64 and Encryption Function Locations

This document provides a comprehensive guide to where Base64 encoding/decoding and encryption functions are located in the Maven Script Interpreter codebase.

## Base64 Functions

### 1. Array Base64 Functions (EBS Script Builtins)

**Location**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsSystem.java`

These are the primary base64 functions available in EBS scripts:

- **`array.base64encode`** - Encodes a byte array to a base64 string
  - Implementation method: `base64Encode(Object[] args)`
  - Lines: ~1570-1598
  - Uses: `java.util.Base64.getEncoder()`
  
- **`array.base64decode`** - Decodes a base64 string to a byte array
  - Implementation method: `base64Decode(Object[] args)`
  - Lines: ~1605-1616
  - Uses: `java.util.Base64.getDecoder()`

**EBS Script Usage Examples**:
```ebs
# Encode byte array to base64
var bytes = [72, 101, 108, 108, 111];  # "Hello" in bytes
var encoded = call array.base64encode(bytes);

# Decode base64 string to byte array
var decoded = call array.base64decode("SGVsbG8gV29ybGQ=");
```

### 2. Custom Base64 Encoder (Utility Class)

**Location**: `ScriptInterpreter/src/main/java/com/eb/util/encode/Base64Encode.java`

A custom implementation of Base64 encoding/decoding that doesn't rely on Java's built-in Base64 class.

**Key Methods**:
- `encodeBytes(byte[] pStrBytes)` - Encode bytes to base64
- `decodeBytes(byte[] pStrBytes)` - Decode base64 to bytes
- `encodeBytesUrl(byte[] pStrBytes)` - URL-safe encoding
- `decodeBytesUrl(byte[] pStrBytes)` - URL-safe decoding
- `encodeString(String pStr)` - Encode string to base64
- `decodeString(String pStr)` - Decode string from base64

**Features**:
- Custom lookup tables for encoding/decoding
- Support for standard and URL-safe base64 encoding
- Implements the `Encode` interface

### 3. Zip and Base64 Utility

**Location**: `ScriptInterpreter/src/main/java/com/eb/util/ZipBase64.java`

Combines ZIP compression with Base64 encoding for file/directory packaging.

**Key Methods**:
- `zipPathToBase64(Path input, OutputStream base64Out)` - Zip and encode to base64
- `base64ToZip(Path inBase64Text, Path outZip)` - Decode base64 and unzip

**Command Line Usage**:
```bash
# Encode: zip a file/dir and write Base64
java ZipBase64 encode <input-path> [output-base64.txt]

# Decode: convert Base64 text back into a .zip file
java ZipBase64 decode <input-base64.txt> <output-zip-file>
```

### 4. Image Base64 Functions

**Location**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsImage.java`

Base64 functions specifically for image data:

- **`image.fromBase64`** - Creates an EbsImage from a base64 string
- **`image.toBase64`** - Converts an EbsImage to a base64 string

**EBS Script Usage**:
```ebs
# Load image from base64
var base64Str = "iVBORw0KGgoAAAANS...";
var img = call image.fromBase64(base64Str);

# Convert image to base64
var img = call image.load(path="photo.jpg");
var base64 = call image.toBase64(img, format="png");
```

## Encryption Functions

### 1. Cryptographic Functions (EBS Script Builtins)

**Location**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsCrypto.java`

Complete suite of cryptographic functions available in EBS scripts:

#### Encryption/Decryption

- **`crypto.encrypt(plaintext, key)`** - AES-256-GCM encryption
  - Implementation method: `encrypt(Object[] args)`
  - Lines: ~65-105
  - Returns: Base64-encoded encrypted data (IV + ciphertext + auth tag)
  
- **`crypto.decrypt(ciphertext, key)`** - AES-256-GCM decryption
  - Implementation method: `decrypt(Object[] args)`
  - Lines: ~114-156
  - Returns: Decrypted plaintext string

**Features**:
- Uses AES-256-GCM mode (Authenticated Encryption)
- Automatic IV generation for each encryption
- Supports both Base64-encoded keys and password-based encryption
- Key derivation from passwords using SHA-256

**EBS Script Usage**:
```ebs
# Generate a secure key
var key = call crypto.generateKey();

# Encrypt data
var encrypted = call crypto.encrypt("Secret message", key);

# Decrypt data
var decrypted = call crypto.decrypt(encrypted, key);

# Or use a password directly
var encrypted = call crypto.encrypt("Secret", "myPassword123");
var decrypted = call crypto.decrypt(encrypted, "myPassword123");
```

#### Key Generation

- **`crypto.generateKey()`** or **`crypto.generateKey(bits)`**
  - Implementation method: `generateKey(Object[] args)`
  - Lines: ~164-182
  - Default: 256-bit AES key
  - Supported sizes: 128, 192, or 256 bits
  - Returns: Base64-encoded random AES key

**EBS Script Usage**:
```ebs
# Generate 256-bit key (default)
var key256 = call crypto.generateKey();

# Generate 128-bit key
var key128 = call crypto.generateKey(128);
```

#### Hashing Functions

- **`crypto.hash(text, algorithm)`** - Generic hash function
  - Implementation method: `hash(Object[] args)`
  - Lines: ~191-210
  - Supports: SHA-256, SHA-512, MD5, SHA-1
  - Returns: Hex-encoded hash

- **`crypto.sha256(text)`** - SHA-256 hash
  - Implementation method: `sha256(Object[] args)`
  - Lines: ~218-235
  - Returns: Hex-encoded SHA-256 hash

- **`crypto.md5(text)`** - MD5 hash
  - Implementation method: `md5(Object[] args)`
  - Lines: ~243-260
  - Returns: Hex-encoded MD5 hash

**EBS Script Usage**:
```ebs
# SHA-256 hash
var hash = call crypto.sha256("Hello World");

# MD5 hash
var md5Hash = call crypto.md5("Hello World");

# Generic hash with algorithm
var sha512 = call crypto.hash("Hello World", "SHA-512");
```

## Function Registration

All builtin functions are registered in:

**Location**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java`

This file contains:
- Function signatures and parameter definitions
- Return type specifications
- Help documentation strings

## Help Documentation

User-facing help text for all builtins is stored in:

**Location**: `ScriptInterpreter/src/main/resources/help-lookup.json`

This JSON file includes:
- Short descriptions
- Long help text
- Usage examples
- Parameter specifications

## Summary Table

| Function Category | Primary Location | File |
|------------------|------------------|------|
| Array Base64 | BuiltinsSystem.java | `com.eb.script.interpreter.builtins` |
| Custom Base64 Encoder | Base64Encode.java | `com.eb.util.encode` |
| Zip + Base64 | ZipBase64.java | `com.eb.util` |
| Image Base64 | BuiltinsImage.java | `com.eb.script.interpreter.builtins` |
| Encryption (AES) | BuiltinsCrypto.java | `com.eb.script.interpreter.builtins` |
| Hashing | BuiltinsCrypto.java | `com.eb.script.interpreter.builtins` |

## Architecture Notes

1. **EBS Builtin Functions**: All user-facing EBS script functions follow the naming pattern `category.function` (e.g., `crypto.encrypt`, `array.base64encode`)

2. **Dispatch Pattern**: Each builtin category has its own handler class with a `dispatch()` method that routes calls to specific implementations

3. **Base64 Implementation**: The interpreter primarily uses Java's standard `java.util.Base64` class for array base64 operations, but also includes a custom implementation in `Base64Encode.java` for special use cases

4. **Encryption Standard**: All encryption uses AES-256-GCM mode, which provides both confidentiality and authenticity

5. **Key Derivation**: The crypto functions support both:
   - Proper random keys (generated via `crypto.generateKey()`)
   - Password-based encryption (automatically derives key using SHA-256)

## Version History

- **Version 1.0.7.10** (2025-12-12): Added cryptographic functions (`crypto.encrypt`, `crypto.decrypt`, `crypto.generateKey`, `crypto.hash`, `crypto.sha256`, `crypto.md5`)
- Earlier versions: Array base64 and image base64 functions available

## See Also

- `EBS_LANGUAGE_REFERENCE.md` - Complete EBS language specification
- `docs/EBS_SCRIPT_SYNTAX.md` - EBS script syntax guide
- `help-lookup.json` - Interactive help documentation
