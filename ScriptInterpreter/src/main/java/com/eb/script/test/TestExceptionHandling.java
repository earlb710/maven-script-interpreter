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
        
        // Test 5: Custom exception type (now allowed)
        testParsing("Custom exception type", """
            try {
                var x = 10;
            } exceptions {
                when MyCustomError {
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
        
        // Test 9: Raise standard exception parsing
        testParsing("Raise standard exception", """
            raise exception IO_ERROR("File not found");
            """);
        
        // Test 10: Raise standard exception without message
        testParsing("Raise standard exception without message", """
            raise exception MATH_ERROR();
            """);
        
        // Test 11: Raise custom exception with multiple parameters
        testParsing("Raise custom exception with parameters", """
            raise exception MyCustomError("message", 42, true);
            """);
        
        // Test 12: Raise custom exception in try block
        testParsing("Raise and catch custom exception", """
            try {
                raise exception ValidationFailed("field1", "must be numeric");
            } exceptions {
                when ValidationFailed(msg) {
                    print "Caught: " + msg;
                }
            }
            """);
        
        // Test 13: Standard exception should only take one parameter
        testParsingFails("Standard exception with multiple parameters", """
            raise exception IO_ERROR("message1", "message2");
            """);
        
        // Test 14: Custom exception with error variable in handler
        testParsing("Custom exception with error variable", """
            try {
                raise exception MyError("test");
            } exceptions {
                when MyError(errorMsg) {
                    print errorMsg;
                }
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
