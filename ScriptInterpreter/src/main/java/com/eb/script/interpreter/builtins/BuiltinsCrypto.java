package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Built-in functions for Cryptographic operations.
 * Provides encryption, decryption, and hashing functionality.
 * Handles all crypto.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsCrypto {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits recommended for GCM)
    private static final int AES_KEY_SIZE = 256; // bits

    /**
     * Dispatch a Crypto builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "crypto.encrypt")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "crypto.encrypt" -> encrypt(args);
            case "crypto.decrypt" -> decrypt(args);
            case "crypto.generatekey" -> generateKey(args);
            case "crypto.hash" -> hash(args);
            case "crypto.sha256" -> sha256(args);
            case "crypto.sha512" -> sha512(args);
            case "crypto.md5" -> md5(args);
            case "crypto.base64encode" -> base64Encode(args);
            case "crypto.base64decode" -> base64Decode(args);
            default -> throw new InterpreterError("Unknown Crypto builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Crypto builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("crypto.");
    }

    // --- Individual builtin implementations ---

    /**
     * crypto.encrypt(plaintext, key) - Encrypts plaintext using AES-256-GCM
     * 
     * @param args [0] plaintext: String to encrypt
     *             [1] key: Base64-encoded AES key (or password string to derive key from)
     * @return Base64-encoded encrypted data (IV + ciphertext + auth tag)
     */
    private static Object encrypt(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("crypto.encrypt requires 2 arguments: plaintext and key");
        }

        String plaintext = (String) args[0];
        String keyInput = (String) args[1];

        if (plaintext == null || keyInput == null) {
            throw new InterpreterError("crypto.encrypt: plaintext and key cannot be null");
        }

        try {
            // Derive or decode the key
            SecretKey secretKey = deriveKey(keyInput);

            // Generate a random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext (the auth tag is included in ciphertext by GCM)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Return as Base64
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new InterpreterError("crypto.encrypt failed: " + e.getMessage());
        }
    }

    /**
     * crypto.decrypt(ciphertext, key) - Decrypts ciphertext using AES-256-GCM
     * 
     * @param args [0] ciphertext: Base64-encoded encrypted data (IV + ciphertext + auth tag)
     *             [1] key: Base64-encoded AES key (or password string to derive key from)
     * @return Decrypted plaintext string
     */
    private static Object decrypt(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("crypto.decrypt requires 2 arguments: ciphertext and key");
        }

        String ciphertext = (String) args[0];
        String keyInput = (String) args[1];

        if (ciphertext == null || keyInput == null) {
            throw new InterpreterError("crypto.decrypt: ciphertext and key cannot be null");
        }

        try {
            // Derive or decode the key
            SecretKey secretKey = deriveKey(keyInput);

            // Decode the Base64 ciphertext
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            if (combined.length < GCM_IV_LENGTH) {
                throw new InterpreterError("crypto.decrypt: invalid ciphertext format");
            }

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] actualCiphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, actualCiphertext, 0, actualCiphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(actualCiphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new InterpreterError("crypto.decrypt failed: " + e.getMessage());
        }
    }

    /**
     * crypto.generateKey() or crypto.generateKey(bits) - Generates a random AES key
     * 
     * @param args Optional [0] bits: Key size in bits (128, 192, or 256; default 256)
     * @return Base64-encoded AES key
     */
    private static Object generateKey(Object[] args) throws InterpreterError {
        int keySize = AES_KEY_SIZE;

        if (args.length > 0 && args[0] != null) {
            keySize = ((Number) args[0]).intValue();
            if (keySize != 128 && keySize != 192 && keySize != 256) {
                throw new InterpreterError("crypto.generateKey: key size must be 128, 192, or 256 bits");
            }
        }

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new InterpreterError("crypto.generateKey failed: " + e.getMessage());
        }
    }

    /**
     * crypto.hash(text, algorithm) - Computes hash of text using specified algorithm
     * 
     * @param args [0] text: String to hash
     *             [1] algorithm: Hash algorithm (SHA-256, SHA-512, MD5, SHA-1)
     * @return Hex-encoded hash
     */
    private static Object hash(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("crypto.hash requires 2 arguments: text and algorithm");
        }

        String text = (String) args[0];
        String algorithm = (String) args[1];

        if (text == null || algorithm == null) {
            throw new InterpreterError("crypto.hash: text and algorithm cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new InterpreterError("crypto.hash failed: " + e.getMessage());
        }
    }

    /**
     * crypto.sha256(text) - Computes SHA-256 hash
     * 
     * @param args [0] text: String to hash
     * @return Hex-encoded SHA-256 hash
     */
    private static Object sha256(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("crypto.sha256 requires 1 argument: text");
        }

        String text = (String) args[0];
        if (text == null) {
            throw new InterpreterError("crypto.sha256: text cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new InterpreterError("crypto.sha256 failed: " + e.getMessage());
        }
    }

    /**
     * crypto.sha512(text) - Computes SHA-512 hash
     * 
     * @param args [0] text: String to hash
     * @return Hex-encoded SHA-512 hash
     */
    private static Object sha512(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("crypto.sha512 requires 1 argument: text");
        }

        String text = (String) args[0];
        if (text == null) {
            throw new InterpreterError("crypto.sha512: text cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new InterpreterError("crypto.sha512 failed: " + e.getMessage());
        }
    }

    /**
     * crypto.md5(text) - Computes MD5 hash
     * 
     * @param args [0] text: String to hash
     * @return Hex-encoded MD5 hash
     */
    private static Object md5(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("crypto.md5 requires 1 argument: text");
        }

        String text = (String) args[0];
        if (text == null) {
            throw new InterpreterError("crypto.md5: text cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new InterpreterError("crypto.md5 failed: " + e.getMessage());
        }
    }

    /**
     * crypto.base64encode(text) - Encodes text to Base64
     * 
     * @param args [0] text: String to encode
     * @return Base64-encoded string
     */
    private static Object base64Encode(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("crypto.base64encode requires 1 argument: text");
        }

        String text = (String) args[0];
        if (text == null) {
            throw new InterpreterError("crypto.base64encode: text cannot be null");
        }

        try {
            return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InterpreterError("crypto.base64encode failed: " + e.getMessage());
        }
    }

    /**
     * crypto.base64decode(text) - Decodes Base64 text
     * 
     * @param args [0] text: Base64-encoded string to decode
     * @return Decoded string
     */
    private static Object base64Decode(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("crypto.base64decode requires 1 argument: text");
        }

        String text = (String) args[0];
        if (text == null) {
            throw new InterpreterError("crypto.base64decode: text cannot be null");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new InterpreterError("crypto.base64decode failed: " + e.getMessage());
        }
    }

    // --- Public utility methods for use by other builtin classes ---

    /**
     * Encodes byte array to Base64 string.
     * Public utility for use by image and array builtins.
     * 
     * @param bytes Byte array to encode
     * @return Base64-encoded string
     */
    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decodes Base64 string to byte array.
     * Public utility for use by image and array builtins.
     * 
     * @param base64String Base64-encoded string
     * @return Decoded byte array
     * @throws IllegalArgumentException if the input is not valid Base64
     */
    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    // --- Helper methods ---

    /**
     * Derives an AES key from a string input.
     * If the input is a valid Base64-encoded key of the right size, it's used directly.
     * Otherwise, it's hashed using SHA-256 to produce a 256-bit key.
     * 
     * Note: Password-based key derivation uses simple SHA-256 hashing without salt for simplicity.
     * This is suitable for casual encryption but not for high-security scenarios.
     * For production use with passwords, consider using PBKDF2 with random salt and iterations.
     * For best security, use crypto.generateKey() to create proper random keys.
     */
    private static SecretKey deriveKey(String keyInput) throws Exception {
        try {
            // Try to decode as Base64 first
            byte[] decoded = Base64.getDecoder().decode(keyInput);
            // Check if it's a valid AES key size (128, 192, or 256 bits)
            int bits = decoded.length * 8;
            if (bits == 128 || bits == 192 || bits == 256) {
                return new SecretKeySpec(decoded, "AES");
            }
        } catch (IllegalArgumentException ignored) {
            // Not valid Base64, fall through to hashing
        }

        // Hash the password to get a 256-bit key
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(keyInput.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Converts byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
