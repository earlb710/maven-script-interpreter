package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.parser.Parser;

public class TestArrayTypeSyntax {
    public static void main(String[] args) {
        try {
            String code = """
                var genericArray: array[10];
                var anyArray: array.any[5];
                var stringArray: array.string[5];
                var intArray: array.int[5];
                var integerArray: array.integer[3];
                var longArray: array.long[3];
                var floatArray: array.float[3];
                var doubleArray: array.double[3];
                var numberArray: array.number[3];
                var byteArray: array.byte[5];
                var dynamicStrings: array.string[*];
                var matrix: array.int[3, 4];
                print "All array.type syntax variants parsed successfully!";
                """;
            
            RuntimeContext context = Parser.parse("test", code);
            System.out.println("✓ Parsing successful!");
            System.out.println("✓ array[10] - Generic array");
            System.out.println("✓ array.any[5] - Generic array (explicit)");
            System.out.println("✓ array.string[5] - String array");
            System.out.println("✓ array.int[5] - Integer array");
            System.out.println("✓ array.integer[3] - Integer array (alias)");
            System.out.println("✓ array.long[3] - Long array");
            System.out.println("✓ array.float[3] - Float array");
            System.out.println("✓ array.double[3] - Double array");
            System.out.println("✓ array.number[3] - Number array (double)");
            System.out.println("✓ array.byte[5] - Byte array");
            System.out.println("✓ array.string[*] - Dynamic string array");
            System.out.println("✓ array.int[3, 4] - Multi-dimensional integer array");
            
        } catch (Exception e) {
            System.err.println("✗ Parsing failed:");
            e.printStackTrace();
        }
    }
}
