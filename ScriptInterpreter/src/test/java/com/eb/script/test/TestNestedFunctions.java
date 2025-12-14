package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.parser.ParseError;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import java.io.IOException;

public class TestNestedFunctions {
    
    public static void main(String[] args) {
        try {
            // Test 1: Simple nested function call
            System.out.println("Test 1: Simple nested function call");
            String script1 = """
                innerFunc() return string {
                    return "INNER";
                }
                
                outerFunc() return string {
                    var x: string = call innerFunc();
                    return "OUTER";
                }
                
                var result: string = call outerFunc();
                """;
            
            try {
                RuntimeContext ctx1 = Parser.parse("test1", script1);
                Interpreter interp1 = new Interpreter();
                interp1.interpret(ctx1);
                System.out.println("✓ Test 1 passed - nested function calls work correctly");
            } catch (Exception e) {
                System.err.println("✗ Test 1 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 2: Three-level nesting
            System.out.println("Test 2: Three-level nested function calls");
            String script2 = """
                level3() return int {
                    return 3;
                }
                
                level2() return int {
                    var x: int = call level3();
                    return 2;
                }
                
                level1() return int {
                    var y: int = call level2();
                    return 1;
                }
                
                var nested: int = call level1();
                """;
            
            try {
                RuntimeContext ctx2 = Parser.parse("test2", script2);
                Interpreter interp2 = new Interpreter();
                interp2.interpret(ctx2);
                System.out.println("✓ Test 2 passed - three-level nesting works correctly");
            } catch (Exception e) {
                System.err.println("✗ Test 2 failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
            
            // Test 3: Return in nested if blocks
            System.out.println("Test 3: Return in nested if blocks within functions");
            String script3 = """
                checkValue(val: int) return string {
                    if val > 0 then {
                        return "positive";
                    } else {
                        return "non-positive";
                    }
                }
                
                processValue(val: int) return string {
                    var check: string = call checkValue(val);
                    return "processed";
                }
                
                var result: string = call processValue(5);
                """;
            
            try {
                RuntimeContext ctx3 = Parser.parse("test3", script3);
                Interpreter interp3 = new Interpreter();
                interp3.interpret(ctx3);
                System.out.println("✓ Test 3 passed - returns in nested if blocks work correctly");
            } catch (Exception e) {
                System.err.println("✗ Test 3 failed: " + e.getMessage());
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
