package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.parser.ParseError;
import com.eb.script.parser.Parser;

/**
 * Simple test for exception handling parsing.
 * This tests that the parser can correctly parse the try-exceptions syntax.
 */
public class TestExceptionHandling {
    
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
        
        System.out.println("\n=== All parsing tests completed ===");
    }
    
    private static void testParsing(String testName, String script) {
        System.out.println("Test: " + testName);
        try {
            RuntimeContext runtime = Parser.parse("test", script);
            System.out.println("  ✓ Parsing succeeded");
        } catch (Exception e) {
            System.out.println("  ✗ Parsing failed: " + e.getMessage());
        }
    }
    
    private static void testParsingFails(String testName, String script) {
        System.out.println("Test: " + testName + " (expected to fail)");
        try {
            RuntimeContext runtime = Parser.parse("test", script);
            System.out.println("  ✗ Parsing should have failed but succeeded");
        } catch (Exception e) {
            System.out.println("  ✓ Parsing failed as expected: " + e.getMessage());
        }
    }
}
