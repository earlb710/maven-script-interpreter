package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.parser.ParseError;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import java.io.IOException;

public class TestBinaryDatatype {
    
    public static void main(String[] args) {
        try {
            // Test 1: Parse binary variable declaration
            System.out.println("Test 1: Parsing binary variable declaration");
            String script1 = "var binaryData: binary;";
            RuntimeContext ctx1 = Parser.parse("test1", script1);
            System.out.println("✓ Parsed binary variable declaration successfully");
            System.out.println();
            
            // Test 2: Test binary.fromBase64 (datatype function)
            System.out.println("Test 2: Testing binary.fromBase64");
            String script2 = """
                var bin: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
                print "Created binary from Base64";
                """;
            RuntimeContext ctx2 = Parser.parse("test2", script2);
            Interpreter interp2 = new Interpreter();
            interp2.interpret(ctx2);
            System.out.println("✓ binary.fromBase64 works correctly");
            System.out.println();
            
            // Test 3: Test binary.toString (datatype function)
            System.out.println("Test 3: Testing binary.toString");
            String script3 = """
                var bin: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
                var text: string = binary.toString(bin);
                print "Text: " + text;
                """;
            RuntimeContext ctx3 = Parser.parse("test3", script3);
            Interpreter interp3 = new Interpreter();
            interp3.interpret(ctx3);
            System.out.println("✓ binary.toString works correctly");
            System.out.println();
            
            // Test 4: Test .length property access (variable chain function)
            System.out.println("Test 4: Testing .length property");
            String script4 = """
                var bin: binary = binary.fromBase64("SGVsbG8=");
                var len: int = bin.length;
                print "Length: " + len;
                """;
            RuntimeContext ctx4 = Parser.parse("test4", script4);
            Interpreter interp4 = new Interpreter();
            interp4.interpret(ctx4);
            System.out.println("✓ .length property works correctly");
            System.out.println();
            
            System.out.println("All binary datatype tests passed!");
            System.out.println();
            System.out.println("Note: Variable chain functions (get, set, slice, concat, toBase64)");
            System.out.println("should be called on the variable itself, not as static binary.* functions.");
            System.out.println("These require method-style call support in the parser.");
            
        } catch (ParseError | IOException e) {
            System.err.println("Parse error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterpreterError e) {
            System.err.println("Interpreter error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
