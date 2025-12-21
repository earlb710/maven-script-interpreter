package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test to verify that line numbers are correctly reset for nested imports.
 * This tests the scenario where:
 * - main.ebs imports file_a.ebs  
 * - file_a.ebs imports file_b.ebs
 * - file_b.ebs has an error
 * The error should report the correct line number in file_b.ebs, not a cumulative line count.
 */
public class TestNestedImportLineNumbers {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Nested Import Line Number Reset ===");
            System.out.println();
            
            // Create test directory
            Path testDir = Files.createTempDirectory("nested_import_test");
            
            // Create file_b.ebs with error on line 5
            String fileBContent = """
                // file_b.ebs
                // Line 2
                // Line 3
                // Line 4
                var y: int = 100 / 0;
                """;
            Path fileB = testDir.resolve("file_b.ebs");
            Files.writeString(fileB, fileBContent);
            
            // Create file_a.ebs that imports file_b (10 lines before import)
            String fileAContent = """
                // file_a.ebs
                // Line 2
                // Line 3
                // Line 4
                // Line 5
                // Line 6
                // Line 7
                // Line 8
                // Line 9
                // Line 10
                import "%s";
                """.formatted(fileB.toString());
            Path fileA = testDir.resolve("file_a.ebs");
            Files.writeString(fileA, fileAContent);
            
            // Create main.ebs that imports file_a (15 lines before import)
            String mainContent = """
                // main.ebs
                // Line 2
                // Line 3
                // Line 4
                // Line 5
                // Line 6
                // Line 7
                // Line 8
                // Line 9
                // Line 10
                // Line 11
                // Line 12
                // Line 13
                // Line 14
                // Line 15
                import "%s";
                """.formatted(fileA.toString());
            Path mainFile = testDir.resolve("main.ebs");
            Files.writeString(mainFile, mainContent);
            
            System.out.println("Test files created:");
            System.out.println("  main.ebs (15 lines) -> imports file_a.ebs");
            System.out.println("  file_a.ebs (10 lines) -> imports file_b.ebs");
            System.out.println("  file_b.ebs (error on line 5)");
            System.out.println();
            System.out.println("Without fix: error would report line 30 (15 + 10 + 5)");
            System.out.println("With fix: error should report line 5");
            System.out.println();
            
            // Test: Parse and run the main file
            try {
                System.out.println("Parsing and running main file...");
                RuntimeContext ctx = Parser.parse(mainFile);
                Interpreter interp = new Interpreter();
                interp.interpret(ctx);
                
                System.err.println("✗ TEST FAILED: Expected an error but none was thrown");
                
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                System.out.println("Error caught: " + errorMessage);
                System.out.println();
                
                // Extract line number
                String lineNum = extractLineNumber(errorMessage);
                if (lineNum != null) {
                    int line = Integer.parseInt(lineNum);
                    System.out.println("Reported line number: " + line);
                    
                    if (line == 5) {
                        System.out.println("✓ TEST PASSED: Error correctly reports line 5 (from file_b.ebs)");
                        System.out.println("  Line numbers are properly reset for nested imports");
                    } else if (line == 30) {
                        System.err.println("✗ TEST FAILED: Error reports cumulative line 30");
                        System.err.println("  This indicates line numbers are NOT being reset");
                        System.err.println("  The line counter continued across all three files");
                    } else {
                        System.err.println("? TEST UNEXPECTED: Error reports line " + line);
                        System.err.println("  Expected: 5 (with fix) or 30 (without fix)");
                    }
                } else {
                    System.err.println("? TEST INCONCLUSIVE: Could not extract line number");
                    System.err.println("  Full error: " + errorMessage);
                }
            }
            
            // Cleanup
            System.out.println();
            System.out.println("Cleaning up test files...");
            Files.deleteIfExists(fileB);
            Files.deleteIfExists(fileA);
            Files.deleteIfExists(mainFile);
            Files.deleteIfExists(testDir);
            
        } catch (Exception e) {
            System.err.println("Unexpected error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String extractLineNumber(String errorMessage) {
        if (errorMessage.contains("line ")) {
            int start = errorMessage.indexOf("line ") + 5;
            int end = start;
            while (end < errorMessage.length() && Character.isDigit(errorMessage.charAt(end))) {
                end++;
            }
            if (end > start) {
                return errorMessage.substring(start, end);
            }
        }
        return null;
    }
}
