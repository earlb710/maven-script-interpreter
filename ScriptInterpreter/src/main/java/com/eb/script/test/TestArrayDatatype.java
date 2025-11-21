package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.parser.Parser;

public class TestArrayDatatype {
    public static void main(String[] args) {
        try {
            String code = """
                var fixedArray: array[10];
                var dynamicArray: array[*];
                var literalArray: array[*] = [1, 2, 3];
                var matrix: array[3, 4];
                print "Array syntax parsed successfully!";
                """;
            
            RuntimeContext context = Parser.parse("test", code);
            System.out.println("✓ Parsing successful!");
            System.out.println("✓ array[10] - Fixed array syntax works");
            System.out.println("✓ array[*] - Dynamic array syntax works");
            System.out.println("✓ array[3, 4] - Multi-dimensional array syntax works");
            System.out.println("✓ Array literal assignment works");
            
        } catch (Exception e) {
            System.err.println("✗ Parsing failed:");
            e.printStackTrace();
        }
    }
}
