package com.eb.util.encode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Performance comparison between Java's built-in MessageDigest SHA-256
 * and the custom SHA2.digest256 implementation.
 */
public class SHA2PerformanceTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    
    public static void main(String[] args) {
        System.out.println("SHA-256 Performance Comparison");
        System.out.println("==============================\n");
        
        // Test with different data sizes
        String[] testData = {
            "Hello, World!",  // Small: 13 bytes
            generateString(100),  // Medium: 100 bytes
            generateString(1000),  // Large: 1KB
            generateString(10000)  // Very Large: 10KB
        };
        
        String[] labels = {"Small (13 bytes)", "Medium (100 bytes)", "Large (1KB)", "Very Large (10KB)"};
        
        for (int i = 0; i < testData.length; i++) {
            System.out.println("Testing with " + labels[i] + ":");
            comparePerformance(testData[i]);
            System.out.println();
        }
    }
    
    private static void comparePerformance(String testData) {
        byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        
        // Warmup
        System.out.println("  Warming up...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            builtinSHA256(data);
            customDigest256(data);
        }
        
        // Test built-in MessageDigest
        long startBuiltin = System.nanoTime();
        byte[] builtinResult = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            builtinResult = builtinSHA256(data);
        }
        long endBuiltin = System.nanoTime();
        long builtinTime = endBuiltin - startBuiltin;
        
        // Test custom SHA2.digest256
        long startCustom = System.nanoTime();
        byte[] customResult = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            customResult = customDigest256(data);
        }
        long endCustom = System.nanoTime();
        long customTime = endCustom - startCustom;
        
        // Verify results match
        boolean resultsMatch = java.util.Arrays.equals(builtinResult, customResult);
        
        // Calculate statistics
        double builtinAvgMs = builtinTime / 1_000_000.0 / TEST_ITERATIONS;
        double customAvgMs = customTime / 1_000_000.0 / TEST_ITERATIONS;
        double speedRatio = (double) customTime / builtinTime;
        
        // Print results
        System.out.println("  Built-in SHA-256:");
        System.out.printf("    Total time: %.2f ms%n", builtinTime / 1_000_000.0);
        System.out.printf("    Average per hash: %.4f ms%n", builtinAvgMs);
        System.out.printf("    Throughput: %.0f hashes/sec%n", 1000.0 / builtinAvgMs);
        
        System.out.println("\n  Custom digest256:");
        System.out.printf("    Total time: %.2f ms%n", customTime / 1_000_000.0);
        System.out.printf("    Average per hash: %.4f ms%n", customAvgMs);
        System.out.printf("    Throughput: %.0f hashes/sec%n", 1000.0 / customAvgMs);
        
        System.out.println("\n  Comparison:");
        System.out.printf("    Custom is %.2fx %s than built-in%n", 
            Math.abs(speedRatio), 
            speedRatio > 1 ? "slower" : "faster");
        System.out.printf("    Results match: %s%n", resultsMatch);
        
        if (!resultsMatch) {
            System.out.println("    WARNING: Hash results do not match!");
            System.out.println("    Built-in: " + bytesToHex(builtinResult));
            System.out.println("    Custom:   " + bytesToHex(customResult));
        }
    }
    
    private static byte[] builtinSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static byte[] customDigest256(byte[] data) {
        return SHA2.digest256(data);
    }
    
    private static String generateString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char)('A' + (i % 26)));
        }
        return sb.toString();
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
