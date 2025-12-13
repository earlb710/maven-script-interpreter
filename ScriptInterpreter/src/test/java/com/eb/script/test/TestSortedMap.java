package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.parser.ParseError;
import com.eb.script.RuntimeContext;
import java.io.IOException;

public class TestSortedMap {
    
    public static void main(String[] args) {
        try {
            // Test 1: Parse sorted map declaration
            System.out.println("Test 1: Parsing sorted map declaration");
            String script1 = "var sm: sorted map = {\"c\": 3, \"a\": 1, \"b\": 2};";
            RuntimeContext ctx1 = Parser.parse("test1", script1);
            System.out.println("✓ Parsed sorted map successfully");
            System.out.println();
            
            // Test 2: Parse normal map declaration
            System.out.println("Test 2: Parsing normal map declaration");
            String script2 = "var nm: map = {\"c\": 3, \"a\": 1, \"b\": 2};";
            RuntimeContext ctx2 = Parser.parse("test2", script2);
            System.out.println("✓ Parsed normal map successfully");
            System.out.println();
            
            // Test 3: Parse empty sorted map
            System.out.println("Test 3: Parsing empty sorted map");
            String script3 = "var em: sorted map = {};";
            RuntimeContext ctx3 = Parser.parse("test3", script3);
            System.out.println("✓ Parsed empty sorted map successfully");
            System.out.println();
            
            // Test 4: Parse const sorted map
            System.out.println("Test 4: Parsing const sorted map");
            String script4 = "const csm: sorted map = {\"x\": 10, \"y\": 20};";
            RuntimeContext ctx4 = Parser.parse("test4", script4);
            System.out.println("✓ Parsed const sorted map successfully");
            System.out.println();
            
            System.out.println("All parsing tests passed!");
            
        } catch (ParseError | IOException e) {
            System.err.println("Parse error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
