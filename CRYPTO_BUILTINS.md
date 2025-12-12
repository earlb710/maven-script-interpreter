# Crypto Builtins Documentation

## Overview

The EBS scripting language provides comprehensive cryptographic functions through the `crypto.*` namespace. These functions support encryption, hashing, and encoding operations.

## Hash Functions

### crypto.sha256(text)

Computes SHA-256 hash of the input text.

**Parameters:**
- `text` (string): Text to hash

**Returns:** 64-character hex-encoded SHA-256 hash

**Example:**
```ebs
var message: string = "Hello, World!";
var hash: string = call crypto.sha256(message);
print hash;
// Output: dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f
```

**Performance:** Uses hardware-accelerated native implementation (recommended for production)

---

### crypto.sha512(text) ✨ NEW

Computes SHA-512 hash of the input text.

**Parameters:**
- `text` (string): Text to hash

**Returns:** 128-character hex-encoded SHA-512 hash

**Example:**
```ebs
var message: string = "Hello, World!";
var hash: string = call crypto.sha512(message);
print hash;
// Output: 374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387
```

**Use Cases:**
- Stronger cryptographic hash than SHA-256 (512-bit vs 256-bit)
- File integrity verification
- Digital signatures
- Password hashing (consider adding salt)

**Performance:** Uses hardware-accelerated native implementation

---

### crypto.md5(text)

Computes MD5 hash of the input text.

**Parameters:**
- `text` (string): Text to hash

**Returns:** 32-character hex-encoded MD5 hash

**Example:**
```ebs
var message: string = "Hello, World!";
var hash: string = call crypto.md5(message);
print hash;
```

**⚠️ Warning:** MD5 is cryptographically broken and should NOT be used for security purposes. Use SHA-256 or SHA-512 instead.

---

### crypto.hash(text, algorithm)

Generic hash function supporting multiple algorithms.

**Parameters:**
- `text` (string): Text to hash
- `algorithm` (string): Hash algorithm name (e.g., "SHA-256", "SHA-512", "MD5", "SHA-1")

**Returns:** Hex-encoded hash

**Example:**
```ebs
var message: string = "Hello, World!";
var hash: string = call crypto.hash(message, "SHA-512");
print hash;
```

---

## Encoding Functions

### crypto.base64encode(text) ✨ NEW

Encodes text to Base64 format.

**Parameters:**
- `text` (string): Text to encode (supports UTF-8)

**Returns:** Base64-encoded string

**Example:**
```ebs
var message: string = "The quick brown fox";
var encoded: string = call crypto.base64encode(message);
print encoded;
// Output: VGhlIHF1aWNrIGJyb3duIGZveA==
```

**Use Cases:**
- Encoding binary data for text transmission
- API data encoding
- Email attachments
- URL-safe data encoding
- Storing binary data in JSON/XML

**Special Characters:**
```ebs
var special: string = "Special: !@#$%^&*()";
var encoded: string = call crypto.base64encode(special);
print encoded;
// Handles all UTF-8 characters correctly
```

---

### crypto.base64decode(text) ✨ NEW

Decodes Base64-encoded text back to original format.

**Parameters:**
- `text` (string): Base64-encoded string to decode

**Returns:** Decoded original string

**Example:**
```ebs
var encoded: string = "VGhlIHF1aWNrIGJyb3duIGZveA==";
var decoded: string = call crypto.base64decode(encoded);
print decoded;
// Output: The quick brown fox
```

**Round-trip Example:**
```ebs
var original: string = "Hello, World!";
var encoded: string = call crypto.base64encode(original);
var decoded: string = call crypto.base64decode(encoded);
print "Match: " + (original == decoded);
// Output: Match: true
```

**Error Handling:**
```ebs
// Invalid Base64 will throw an error
var invalid: string = "Not!Base64!";
var decoded: string = call crypto.base64decode(invalid);
// Error: crypto.base64decode failed: Illegal base64 character
```

---

## Obfuscation Functions

### crypto.obfuscate(text) ✨ NEW

Simple character substitution obfuscation for making text harder to read at a glance.

**Parameters:**
- `text` (string): Text to obfuscate

**Returns:** Obfuscated string

**Example:**
```ebs
var secret: string = "Password123";
var obfuscated: string = call crypto.obfuscate(secret);
print obfuscated;
// Output: Pqllvgkr829 (readable but different)
```

**How It Works:**
- Space characters map to `~`
- Uppercase letters (A-Z) map to different uppercase letters
- Lowercase letters (a-z) map to different lowercase letters
- Digits (0-9) map to different digits
- Special characters pass through unchanged
- Uses fixed character mappings (no key required)

**Use Cases:**
- Making text less obvious in logs or displays
- Simple obfuscation for casual privacy (not security)
- Hiding sensitive data from shoulder surfing
- Making text unreadable to automated scrapers

**⚠️ Important Notes:**
- This is NOT cryptographically secure encryption
- Does not provide security against determined attackers
- For true security, use `crypto.encrypt()` instead
- The mapping is fixed and reversible with `crypto.deobfuscate()`

---

### crypto.deobfuscate(text) ✨ NEW

Reverses obfuscation performed by `crypto.obfuscate()`.

**Parameters:**
- `text` (string): Obfuscated string to restore

**Returns:** Original string

**Example:**
```ebs
var obfuscated: string = "Pqllvgkr829";
var original: string = call crypto.deobfuscate(obfuscated);
print original;
// Output: Password123
```

**Round-trip Example:**
```ebs
var original: string = "Sensitive Data 2024";
var obfuscated: string = call crypto.obfuscate(original);
var restored: string = call crypto.deobfuscate(obfuscated);
print "Match: " + (original == restored);
// Output: Match: true
```

---

## Encryption Functions

### crypto.encrypt(plaintext, key)

Encrypts plaintext using AES-256-GCM.

**Parameters:**
- `plaintext` (string): Text to encrypt
- `key` (string): Base64-encoded AES key or password

**Returns:** Base64-encoded encrypted data (IV + ciphertext + auth tag)

**Example:**
```ebs
var key: string = call crypto.generatekey();
var encrypted: string = call crypto.encrypt("Secret message", key);
print encrypted;
```

---

### crypto.decrypt(ciphertext, key)

Decrypts ciphertext using AES-256-GCM.

**Parameters:**
- `ciphertext` (string): Base64-encoded encrypted data
- `key` (string): Base64-encoded AES key or password

**Returns:** Decrypted plaintext string

**Example:**
```ebs
var key: string = call crypto.generatekey();
var encrypted: string = call crypto.encrypt("Secret message", key);
var decrypted: string = call crypto.decrypt(encrypted, key);
print decrypted;
// Output: Secret message
```

---

### crypto.generatekey()

Generates a random AES encryption key.

**Parameters:**
- `bits` (optional int): Key size in bits (128, 192, or 256; default 256)

**Returns:** Base64-encoded AES key

**Example:**
```ebs
var key: string = call crypto.generatekey();
print key;
// Output: Random Base64-encoded 256-bit key

var key128: string = call crypto.generatekey(128);
print key128;
// Output: Random Base64-encoded 128-bit key
```

---

## Complete Example: Data Protection

```ebs
// Generate encryption key
var key: string = call crypto.generatekey(256);
print "Generated key: " + key;

// Encrypt sensitive data
var sensitiveData: string = "User password: secret123";
var encrypted: string = call crypto.encrypt(sensitiveData, key);
print "Encrypted: " + encrypted;

// Decrypt when needed
var decrypted: string = call crypto.decrypt(encrypted, key);
print "Decrypted: " + decrypted;

// Hash for integrity
var hash: string = call crypto.sha512(sensitiveData);
print "SHA-512 hash: " + hash;

// Encode for transmission
var encoded: string = call crypto.base64encode(encrypted);
print "Base64 encoded: " + encoded;

// Decode on receiving end
var decoded: string = call crypto.base64decode(encoded);
print "Decoded matches: " + (decoded == encrypted);
```

---

## Security Best Practices

1. **Use SHA-512 or SHA-256** for hashing, not MD5
2. **Generate random keys** with `crypto.generatekey()` instead of using passwords
3. **Store keys securely** - never hardcode keys in scripts
4. **Add salt** when hashing passwords
5. **Use Base64 encoding** for safe text transmission of binary data
6. **Validate input** before decoding/decrypting to prevent errors

---

## Error Handling

All crypto functions throw `InterpreterError` on failure:

```ebs
try {
    var hash: string = call crypto.sha512(null);
} catch(error) {
    print "Error: " + error;
    // Output: Error: crypto.sha512: text cannot be null
}

try {
    var decoded: string = call crypto.base64decode("Invalid!!!");
} catch(error) {
    print "Error: " + error;
    // Output: Error: crypto.base64decode failed: Illegal base64 character
}
```

---

## Performance Notes

- **SHA-256 and SHA-512**: Use hardware acceleration (SHA-NI) when available
- **Base64**: Fast encoding/decoding with Java's built-in `Base64` class
- **AES encryption**: Uses AES-GCM for authenticated encryption with hardware acceleration
