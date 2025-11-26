package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.parser.ParseError;
import com.eb.script.parser.Parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Test for exception handling parsing.
 * This tests that the parser can correctly parse the try-exceptions syntax.
 * Note: Runtime execution tests are not included as they require JavaFX environment.
 */
public class TestExceptionHandling {
    
    private static int passedTests = 0;
    private static int failedTests = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Testing Exception Handling Parsing ===\n");
        
        // Test 1: Basic try-exceptions parsing
        testParsing("Basic try-exceptions", """
            try {
                var x:int = 10;
            } exceptions {
                when ANY_ERROR {
                    print "error";
                }
            }
            """);
        
        // Test 2: Multiple handlers
        testParsing("Multiple handlers", """
            try {
                var x = 10 / 0;
            } exceptions {
                when MATH_ERROR {
                    print "math error";
                }
                when IO_ERROR {
                    print "io error";
                }
                when ANY_ERROR {
                    print "any error";
                }
            }
            """);
        
        // Test 3: Error variable capture
        testParsing("Error variable capture", """
            try {
                var x = 10 / 0;
            } exceptions {
                when MATH_ERROR(msg) {
                    print "Error: " + msg;
                }
            }
            """);
        
        // Test 4: Nested try blocks
        testParsing("Nested try blocks", """
            try {
                try {
                    var x = 1;
                } exceptions {
                    when ANY_ERROR {
                        print "inner error";
                    }
                }
            } exceptions {
                when ANY_ERROR {
                    print "outer error";
                }
            }
            """);
        
        // Test 5: Invalid error type (should fail)
        testParsingFails("Invalid error type", """
            try {
                var x = 10;
            } exceptions {
                when INVALID_ERROR_TYPE {
                    print "error";
                }
            }
            """);
        
        // Test 6: All error types are recognized
        testParsing("All error types", """
            try {
                var x = 1;
            } exceptions {
                when IO_ERROR { print "io"; }
                when DB_ERROR { print "db"; }
                when TYPE_ERROR { print "type"; }
                when NULL_ERROR { print "null"; }
                when INDEX_ERROR { print "index"; }
                when MATH_ERROR { print "math"; }
                when PARSE_ERROR { print "parse"; }
                when NETWORK_ERROR { print "network"; }
                when NOT_FOUND_ERROR { print "not found"; }
                when ACCESS_ERROR { print "access"; }
                when VALIDATION_ERROR { print "validation"; }
                when ANY_ERROR { print "any"; }
            }
            """);
        
        // Test 7: Try without exceptions should fail
        testParsingFails("Try without exceptions", """
            try {
                var x = 10;
            }
            """);
        
        // Test 8: Empty exceptions block should fail
        testParsingFails("Empty exceptions block", """
            try {
                var x = 10;
            } exceptions {
            }
            """);
        
        System.out.println("\n=== Parsing Test Summary ===");
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failedTests);
        
        if (failedTests > 0) {
            System.exit(1);
        }
    }
    
    private static void testParsing(String testName, String script) {
        System.out.println("Test: " + testName);
        try {
            RuntimeContext runtime = Parser.parse("test", script);
            System.out.println("  ✓ Parsing succeeded");
            passedTests++;
        } catch (Exception e) {
            System.out.println("  ✗ Parsing failed: " + e.getMessage());
            failedTests++;
        }
    }
    
    private static void testParsingFails(String testName, String script) {
        System.out.println("Test: " + testName + " (expected to fail)");
        try {
            RuntimeContext runtime = Parser.parse("test", script);
            System.out.println("  ✗ Parsing should have failed but succeeded");
            failedTests++;
        } catch (Exception e) {
            System.out.println("  ✓ Parsing failed as expected: " + e.getMessage());
            passedTests++;
        }
    }
}
