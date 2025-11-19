package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import java.nio.file.Path;

public class TestImportBlocks {
    public static void main(String[] args) {
        try {
            System.out.println("Parsing import_examples.ebs...");
            Path scriptPath = Path.of("scripts/import_examples.ebs");
            RuntimeContext ctx = Parser.parse(scriptPath);
            
            System.out.println("Main script:");
            System.out.println("  Statements: " + ctx.statements.length);
            System.out.println("  Blocks/Functions: " + (ctx.blocks != null ? ctx.blocks.size() : 0));
            
            if (ctx.blocks != null && !ctx.blocks.isEmpty()) {
                System.out.println("  Function names in main:");
                for (String key : ctx.blocks.keySet()) {
                    System.out.println("    - " + key);
                }
            }
            
            // Now test with interpreter
            System.out.println("\nTesting with interpreter...");
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(ctx);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
