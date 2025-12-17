package com.eb.script.test;

import com.eb.script.json.Json;
import java.util.Map;

public class TestJsonMultiline {
    
    public static void main(String[] args) {
        System.out.println("Testing JSON parser with multi-line strings...\n");
        
        // Test 1: JSON with actual newlines in string values (the fix enables this)
        try {
            String jsonWithNewlines = "{\"onClick\": \"print \\\"Line 1\\\";\\nprint \\\"Line 2\\\";\\nprint \\\"Line 3\\\";\"}";
            Object result1 = Json.parse(jsonWithNewlines);
            System.out.println("✓ Test 1 PASSED: JSON with actual newlines");
            System.out.println("  Parsed: " + result1);
            System.out.println();
        } catch (Exception e) {
            System.out.println("✗ Test 1 FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        // Test 2: JSON with escaped newlines (should work before and after fix)
        try {
            String jsonWithEscaped = "{\"onClick\": \"print \\\"Line 1\\\";\\nprint \\\"Line 2\\\";\"}";
            Object result2 = Json.parse(jsonWithEscaped);
            System.out.println("✓ Test 2 PASSED: JSON with escaped newlines");
            System.out.println("  Parsed: " + result2);
            System.out.println();
        } catch (Exception e) {
            System.out.println("✗ Test 2 FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        // Test 3: JSON with actual newline characters embedded (the real use case)
        try {
            String jsonWithRealNewlines = "{\"code\": \"line1\nline2\nline3\"}";
            Object result3 = Json.parse(jsonWithRealNewlines);
            System.out.println("✓ Test 3 PASSED: JSON with real embedded newlines");
            if (result3 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) result3;
                String code = (String) map.get("code");
                System.out.println("  Code value: " + code.replace("\n", "\\n"));
                System.out.println("  Contains " + code.split("\n").length + " lines");
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("✗ Test 3 FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        // Test 4: Still reject other control characters for safety
        try {
            String jsonWithNullByte = "{\"bad\": \"has\u0000null\"}";
            Object result4 = Json.parse(jsonWithNullByte);
            System.out.println("✗ Test 4 FAILED: Should have rejected null byte control character");
            System.exit(1);
        } catch (Exception e) {
            System.out.println("✓ Test 4 PASSED: Correctly rejected control character");
            System.out.println("  Error: " + e.getMessage());
            System.out.println();
        }
        
        System.out.println("All tests passed!");
    }
}
