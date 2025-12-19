package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test to verify that line numbers are correctly reported for errors in imported files.
 * This addresses the issue where line numbers continue from the importing file instead
 * of resetting to 1 for each imported file.
 */
public class TestImportLineNumbers {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Import Line Number Reset ===");
            System.out.println();
            
            // Create test directory and files
            Path testDir = Files.createTempDirectory("import_line_test");
            
            // Create imported file with runtime error on line 7
            String importedContent = """
                // This is imported_file.ebs
                // Line 2 - comment
                // Line 3 - comment
                // Line 4 - comment
                // Line 5 - comment
                // Line 6 - This line should cause an error on line 7
                var x: int = 10 / 0;
                """;
            Path importedFile = testDir.resolve("imported_file.ebs");
            Files.writeString(importedFile, importedContent);
            
            // Create main file that imports the file with 10 lines before the import
            String mainContent = """
                // This is main_file.ebs
                // Line 2
                // Line 3
                // Line 4
                // Line 5
                // Line 6
                // Line 7
                // Line 8
                // Line 9
                // Line 10 - Now importing the other file
                import "%s";
                
                // If error line numbers are wrong, it will show a line number > 7
                // If fixed, it should show line 7 from imported_file.ebs
                """.formatted(importedFile.toString());
            Path mainFile = testDir.resolve("main_file.ebs");
            Files.writeString(mainFile, mainContent);
            
            System.out.println("Test files created:");
            System.out.println("  Main file: " + mainFile);
            System.out.println("  Imported file: " + importedFile);
            System.out.println("  Error expected on line 7 of imported file");
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
                
                // Check if the error message contains the correct line number (7)
                if (errorMessage.contains("line 7")) {
                    System.out.println("✓ TEST PASSED: Error correctly reports line 7");
                    System.out.println("  Line numbers are properly reset for imported files");
                } else if (errorMessage.contains("line 17") || errorMessage.contains("line 1")) {
                    System.err.println("✗ TEST FAILED: Error reports wrong line number");
                    System.err.println("  Expected: line 7 (from imported file)");
                    System.err.println("  Got: " + extractLineNumber(errorMessage));
                    System.err.println("  This indicates line numbers are NOT being reset for imports");
                } else {
                    // Try to extract any line number mentioned
                    String lineNum = extractLineNumber(errorMessage);
                    if (lineNum != null) {
                        if (lineNum.equals("7")) {
                            System.out.println("✓ TEST PASSED: Error correctly reports line 7");
                        } else {
                            System.err.println("✗ TEST FAILED: Error reports wrong line number: " + lineNum);
                        }
                    } else {
                        System.err.println("? TEST INCONCLUSIVE: Could not extract line number from error");
                        System.err.println("  Full error: " + errorMessage);
                    }
                }
            }
            
            // Cleanup
            System.out.println();
            System.out.println("Cleaning up test files...");
            Files.deleteIfExists(importedFile);
            Files.deleteIfExists(mainFile);
            Files.deleteIfExists(testDir);
            
        } catch (Exception e) {
            System.err.println("Unexpected error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String extractLineNumber(String errorMessage) {
        // Try to extract line number from error message
        // Expected format: "Runtime error on line X : ..."
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
