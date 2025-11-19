package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import java.nio.file.Path;

public class TestSimple {
    public static void main(String[] args) {
        try {
            System.out.println("Testing stringUtil.ebs parsing...");
            Path utilPath = Path.of("scripts/util/stringUtil.ebs");
            RuntimeContext utilCtx = Parser.parse(utilPath);
            System.out.println("✓ Parse successful");
            
            System.out.println("\nTesting import_examples.ebs parsing...");
            Path scriptPath = Path.of("scripts/import_examples.ebs");
            RuntimeContext ctx = Parser.parse(scriptPath);
            System.out.println("✓ Parse successful");
            
            System.out.println("\n=== All parsing tests passed ===");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
