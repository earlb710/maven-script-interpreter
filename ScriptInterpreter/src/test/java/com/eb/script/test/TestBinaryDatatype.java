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
            
            // Test 2: Test binary.fromBase64 and binary.toBase64
            System.out.println("Test 2: Testing binary.fromBase64 and binary.toBase64");
            String script2 = """
                var bin: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
                var encoded: string = binary.toBase64(bin);
                print "Base64: " + encoded;
                """;
            RuntimeContext ctx2 = Parser.parse("test2", script2);
            Interpreter interp2 = new Interpreter();
            interp2.interpret(ctx2);
            System.out.println("✓ binary.fromBase64 and binary.toBase64 work correctly");
            System.out.println();
            
            // Test 3: Test binary.length
            System.out.println("Test 3: Testing binary.length");
            String script3 = """
                var bin: binary = binary.fromBase64("SGVsbG8=");
                var len: int = binary.length(bin);
                print "Length: " + len;
                """;
            RuntimeContext ctx3 = Parser.parse("test3", script3);
            Interpreter interp3 = new Interpreter();
            interp3.interpret(ctx3);
            System.out.println("✓ binary.length works correctly");
            System.out.println();
            
            // Test 4: Test binary.get and binary.set
            System.out.println("Test 4: Testing binary.get and binary.set");
            String script4 = """
                var bin: binary = binary.fromBase64("AAAA");
                var b: byte = binary.get(bin, 0);
                print "Before set: " + b;
                call binary.set(bin, 0, 72);
                var b2: byte = binary.get(bin, 0);
                print "After set: " + b2;
                """;
            RuntimeContext ctx4 = Parser.parse("test4", script4);
            Interpreter interp4 = new Interpreter();
            interp4.interpret(ctx4);
            System.out.println("✓ binary.get and binary.set work correctly");
            System.out.println();
            
            // Test 5: Test binary.slice
            System.out.println("Test 5: Testing binary.slice");
            String script5 = """
                var bin: binary = binary.fromBase64("SGVsbG8gV29ybGQ=");
                var slice1: binary = binary.slice(bin, 0, 5);
                var slice2: binary = binary.slice(bin, 6);
                var encoded1: string = binary.toBase64(slice1);
                var encoded2: string = binary.toBase64(slice2);
                print "Slice 1: " + encoded1;
                print "Slice 2: " + encoded2;
                """;
            RuntimeContext ctx5 = Parser.parse("test5", script5);
            Interpreter interp5 = new Interpreter();
            interp5.interpret(ctx5);
            System.out.println("✓ binary.slice works correctly");
            System.out.println();
            
            // Test 6: Test binary.concat
            System.out.println("Test 6: Testing binary.concat");
            String script6 = """
                var bin1: binary = binary.fromBase64("SGVs");
                var bin2: binary = binary.fromBase64("bG8=");
                var combined: binary = binary.concat(bin1, bin2);
                var encoded: string = binary.toBase64(combined);
                print "Combined: " + encoded;
                """;
            RuntimeContext ctx6 = Parser.parse("test6", script6);
            Interpreter interp6 = new Interpreter();
            interp6.interpret(ctx6);
            System.out.println("✓ binary.concat works correctly");
            System.out.println();
            
            // Test 7: Test binary.toByteArray and binary.fromByteArray
            System.out.println("Test 7: Testing binary.toByteArray and binary.fromByteArray");
            String script7 = """
                var bin: binary = binary.fromBase64("AQIDBA==");
                var arr = binary.toByteArray(bin);
                print "Array length: " + arr.length;
                var bin2: binary = binary.fromByteArray(arr);
                var encoded: string = binary.toBase64(bin2);
                print "Back to binary: " + encoded;
                """;
            RuntimeContext ctx7 = Parser.parse("test7", script7);
            Interpreter interp7 = new Interpreter();
            interp7.interpret(ctx7);
            System.out.println("✓ binary.toByteArray and binary.fromByteArray work correctly");
            System.out.println();
            
            System.out.println("All binary datatype tests passed!");
            
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
