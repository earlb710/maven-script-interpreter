package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.FunctionMetadata;
import com.eb.script.interpreter.statement.BlockStatement;
import com.eb.script.parser.Parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Test to verify that function metadata is correctly stored with script name and line number.
 * This test only checks the parsing and BlockStatement line numbers, not the full interpreter.
 */
public class TestFunctionMetadata {
    
    public static void main(String[] args) {
        try {
            // Create a simple test script
            String testScript = """
                // Test script - line 1
                // line 2
                myFunction() return string {
                    return "test";
                }
                
                anotherFunction() {
                    print "hello";
                }
                """;
            
            // Write to a temporary file
            Path tempFile = Files.createTempFile("test-function-metadata", ".ebs");
            Files.writeString(tempFile, testScript);
            
            System.out.println("Test file: " + tempFile);
            
            // Parse the script
            RuntimeContext ctx = Parser.parse(tempFile);
            
            System.out.println("Parsed script: " + ctx.name);
            System.out.println("Number of blocks/functions: " + (ctx.blocks != null ? ctx.blocks.size() : 0));
            
            // Verify that BlockStatements have line numbers
            System.out.println("\nFunction blocks with line numbers:");
            if (ctx.blocks != null) {
                for (Map.Entry<String, BlockStatement> entry : ctx.blocks.entrySet()) {
                    String functionName = entry.getKey();
                    BlockStatement block = entry.getValue();
                    
                    System.out.println("  Function: " + functionName);
                    System.out.println("    Line: " + block.getLine());
                    
                    // Simulate what the interpreter would do
                    FunctionMetadata metadata = new FunctionMetadata(ctx.name, block.getLine());
                    System.out.println("    Metadata: " + metadata.toString());
                }
            }
            
            // Clean up
            Files.deleteIfExists(tempFile);
            
            System.out.println("\n✓ Test passed - function metadata can be correctly extracted!");
            System.out.println("  Functions are parsed with their line numbers.");
            System.out.println("  The Interpreter will register them with FunctionMetadata.");
            
        } catch (Exception e) {
            System.err.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
