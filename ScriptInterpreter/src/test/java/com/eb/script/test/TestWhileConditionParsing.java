package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;

/**
 * Test for while loop condition parsing with complex expressions.
 * Tests the fix for issue where expressions like:
 *   while (currentX != toX || currentY != toY) && stepCount < maxSteps {...}
 * would fail to parse with error "Expected '{' after while condition".
 */
public class TestWhileConditionParsing {
    
    public static void main(String[] args) {
        System.out.println("=== Testing While Loop Condition Parsing ===\n");
        
        int passed = 0;
        int failed = 0;
        
        // Test 1: Original problematic condition with parentheses and &&
        System.out.println("Test 1: Parenthesized OR condition with AND operator");
        try {
            String script1 = """
                var currentX: int = 5;
                var toX: int = 10;
                var currentY: int = 3;
                var toY: int = 3;
                var stepCount: int = 0;
                var maxSteps: int = 100;
                
                while (currentX != toX || currentY != toY) && stepCount < maxSteps {
                    currentX = currentX + 1;
                    stepCount = stepCount + 1;
                }
                """;
            
            RuntimeContext ctx1 = Parser.parse("test1", script1);
            System.out.println("✓ Test 1 passed - complex condition with parentheses parses correctly");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 1 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 2: Multiple parenthesized groups with OR
        System.out.println("Test 2: Multiple parenthesized groups with OR");
        try {
            String script2 = """
                var x: int = 5;
                var y: int = 10;
                var count: int = 0;
                
                while (x < 10 && count < 5) || (y > 5 && count < 3) {
                    x = x + 1;
                    count = count + 1;
                }
                """;
            
            RuntimeContext ctx2 = Parser.parse("test2", script2);
            System.out.println("✓ Test 2 passed - multiple parenthesized groups parse correctly");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 2 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 3: Simple condition without parentheses (regression test)
        System.out.println("Test 3: Simple condition without parentheses");
        try {
            String script3 = """
                var count: int = 0;
                
                while count < 10 {
                    count = count + 1;
                }
                """;
            
            RuntimeContext ctx3 = Parser.parse("test3", script3);
            System.out.println("✓ Test 3 passed - simple condition without parentheses still works");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 3 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 4: Condition with optional 'then' keyword
        System.out.println("Test 4: Condition with optional 'then' keyword");
        try {
            String script4 = """
                var count: int = 0;
                
                while count < 5 then {
                    count = count + 1;
                }
                """;
            
            RuntimeContext ctx4 = Parser.parse("test4", script4);
            System.out.println("✓ Test 4 passed - 'then' keyword still works");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 4 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 5: Simple parenthesized condition
        System.out.println("Test 5: Simple parenthesized condition");
        try {
            String script5 = """
                var x: int = 5;
                
                while (x < 10) {
                    x = x + 1;
                }
                """;
            
            RuntimeContext ctx5 = Parser.parse("test5", script5);
            System.out.println("✓ Test 5 passed - simple parenthesized condition works");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 5 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 6: Complex nested parentheses
        System.out.println("Test 6: Complex nested parentheses");
        try {
            String script6 = """
                var a: int = 1;
                var b: int = 2;
                var c: int = 3;
                
                while ((a == 1 || b == 2) && c < 5) || (a != 0) {
                    c = c + 1;
                    if c > 4 then {
                        a = 0;
                    }
                }
                """;
            
            RuntimeContext ctx6 = Parser.parse("test6", script6);
            System.out.println("✓ Test 6 passed - complex nested parentheses work");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 6 failed: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Summary
        System.out.println("=== Test Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        
        if (failed > 0) {
            System.exit(1);
        }
    }
}
