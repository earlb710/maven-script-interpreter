package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;

public class TestModuloOperator {
    
    public static void main(String[] args) {
        try {
            // Test 1: Basic integer modulo
            System.out.println("Test 1: Basic integer modulo");
            String script1 = """
                var a: int = 10;
                var b: int = 3;
                var result: int = a % b;
                print("10 % 3 = " + result);
                """;
            
            try {
                RuntimeContext ctx1 = Parser.parse("test1", script1);
                Interpreter interp1 = new Interpreter();
                interp1.interpret(ctx1);
                System.out.println("✓ Test 1 passed - basic integer modulo works");
            } catch (Exception e) {
                System.err.println("✗ Test 1 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 2: Modulo with negative numbers
            System.out.println("Test 2: Modulo with negative numbers");
            String script2 = """
                var a: int = -10;
                var b: int = 3;
                var result: int = a % b;
                print("-10 % 3 = " + result);
                """;
            
            try {
                RuntimeContext ctx2 = Parser.parse("test2", script2);
                Interpreter interp2 = new Interpreter();
                interp2.interpret(ctx2);
                System.out.println("✓ Test 2 passed - modulo with negative numbers works");
            } catch (Exception e) {
                System.err.println("✗ Test 2 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 3: Modulo with long values
            System.out.println("Test 3: Modulo with long values");
            String script3 = """
                var a: long = 1000000000;
                var b: long = 7;
                var result: long = a % b;
                print("1000000000 % 7 = " + result);
                """;
            
            try {
                RuntimeContext ctx3 = Parser.parse("test3", script3);
                Interpreter interp3 = new Interpreter();
                interp3.interpret(ctx3);
                System.out.println("✓ Test 3 passed - modulo with long values works");
            } catch (Exception e) {
                System.err.println("✗ Test 3 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 4: Modulo with float values
            System.out.println("Test 4: Modulo with float values");
            String script4 = """
                var a: float = 10.5f;
                var b: float = 3.2f;
                var result: float = a % b;
                print("10.5 % 3.2 = " + result);
                """;
            
            try {
                RuntimeContext ctx4 = Parser.parse("test4", script4);
                Interpreter interp4 = new Interpreter();
                interp4.interpret(ctx4);
                System.out.println("✓ Test 4 passed - modulo with float values works");
            } catch (Exception e) {
                System.err.println("✗ Test 4 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 5: Modulo with double values
            System.out.println("Test 5: Modulo with double values");
            String script5 = """
                var a: double = 15.7;
                var b: double = 4.3;
                var result: double = a % b;
                print("15.7 % 4.3 = " + result);
                """;
            
            try {
                RuntimeContext ctx5 = Parser.parse("test5", script5);
                Interpreter interp5 = new Interpreter();
                interp5.interpret(ctx5);
                System.out.println("✓ Test 5 passed - modulo with double values works");
            } catch (Exception e) {
                System.err.println("✗ Test 5 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 6: Modulo in expressions
            System.out.println("Test 6: Modulo in expressions");
            String script6 = """
                var result: int = (100 % 7) + (50 % 6);
                print("(100 % 7) + (50 % 6) = " + result);
                """;
            
            try {
                RuntimeContext ctx6 = Parser.parse("test6", script6);
                Interpreter interp6 = new Interpreter();
                interp6.interpret(ctx6);
                System.out.println("✓ Test 6 passed - modulo in expressions works");
            } catch (Exception e) {
                System.err.println("✗ Test 6 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 7: Modulo by zero should throw error
            System.out.println("Test 7: Modulo by zero error handling");
            String script7 = """
                var a: int = 10;
                var b: int = 0;
                var result: int = a % b;
                """;
            
            try {
                RuntimeContext ctx7 = Parser.parse("test7", script7);
                Interpreter interp7 = new Interpreter();
                interp7.interpret(ctx7);
                System.err.println("✗ Test 7 failed - should have thrown error for modulo by zero");
            } catch (Exception e) {
                if (e.getMessage().contains("Modulo by zero")) {
                    System.out.println("✓ Test 7 passed - modulo by zero correctly throws error");
                } else {
                    System.err.println("✗ Test 7 failed with unexpected error: " + e.getMessage());
                }
            }
            System.out.println();
            
            // Test 8: Using "mod" keyword instead of %
            System.out.println("Test 8: Using 'mod' keyword");
            String script8 = """
                var a: int = 17;
                var b: int = 5;
                var result: int = a mod b;
                print("17 mod 5 = " + result);
                """;
            
            try {
                RuntimeContext ctx8 = Parser.parse("test8", script8);
                Interpreter interp8 = new Interpreter();
                interp8.interpret(ctx8);
                System.out.println("✓ Test 8 passed - 'mod' keyword works");
            } catch (Exception e) {
                System.err.println("✗ Test 8 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 9: Using %= compound assignment operator
            System.out.println("Test 9: Using %= compound assignment");
            String script9 = """
                var x: int = 17;
                x %= 5;
                print("17 %= 5: x = " + x);
                """;
            
            try {
                RuntimeContext ctx9 = Parser.parse("test9", script9);
                Interpreter interp9 = new Interpreter();
                interp9.interpret(ctx9);
                System.out.println("✓ Test 9 passed - %= compound assignment works");
            } catch (Exception e) {
                System.err.println("✗ Test 9 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            System.out.println("All tests completed!");
            
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
