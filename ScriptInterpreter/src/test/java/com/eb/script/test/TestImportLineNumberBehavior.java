package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Comprehensive test demonstrating the import line number fix.
 * 
 * PROBLEM: 
 * When using import statements, the lexer's line counter was not reset between files.
 * This caused error messages to report cumulative line numbers across all imported files
 * instead of the actual line number within each file.
 * 
 * SOLUTION:
 * Reset the lexer's line counter to 1 in tokenizeInit() method.
 * 
 * This test verifies the fix works correctly for:
 * 1. Simple imports
 * 2. Multiple sequential imports
 * 3. Nested imports (import chains)
 */
public class TestImportLineNumberBehavior {
    
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║  Testing Import Line Number Reset Behavior                  ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            
            testSimpleImport();
            System.out.println();
            
            testMultipleImports();
            System.out.println();
            
            testNestedImports();
            System.out.println();
            
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║  All Tests Passed ✓                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("Test suite failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void testSimpleImport() throws Exception {
        System.out.println("Test 1: Simple Import");
        System.out.println("─────────────────────");
        
        Path testDir = Files.createTempDirectory("test_simple_import");
        
        // Create imported file with error on line 3
        String importedContent = """
            // imported.ebs - line 1
            // line 2
            var x: int = 1 / 0;
            """;
        Path imported = testDir.resolve("imported.ebs");
        Files.writeString(imported, importedContent);
        
        // Create main file with 10 lines before import
        String mainContent = createPaddedMain(10, imported.toString());
        Path main = testDir.resolve("main.ebs");
        Files.writeString(main, mainContent);
        
        System.out.println("  Main file: 10 lines before import");
        System.out.println("  Imported file: error on line 3");
        System.out.println("  Expected error line: 3 (with fix)");
        System.out.println("  Without fix would be: 13 (10 + 3)");
        
        try {
            RuntimeContext ctx = Parser.parse(main);
            new Interpreter().interpret(ctx);
            throw new AssertionError("Expected error was not thrown");
        } catch (Exception e) {
            String lineNum = extractLineNumber(e.getMessage());
            if ("3".equals(lineNum)) {
                System.out.println("  ✓ PASSED: Error correctly reports line 3");
            } else {
                throw new AssertionError("Expected line 3, got line " + lineNum);
            }
        } finally {
            cleanup(testDir, imported, main);
        }
    }
    
    private static void testMultipleImports() throws Exception {
        System.out.println("Test 2: Multiple Sequential Imports");
        System.out.println("────────────────────────────────────");
        
        Path testDir = Files.createTempDirectory("test_multiple_imports");
        
        // Create first import with no errors (5 lines)
        String import1Content = """
            // import1.ebs
            var a: int = 1;
            var b: int = 2;
            var c: int = 3;
            // line 5
            """;
        Path import1 = testDir.resolve("import1.ebs");
        Files.writeString(import1, import1Content);
        
        // Create second import with error on line 4
        String import2Content = """
            // import2.ebs - line 1
            // line 2
            // line 3
            var error: int = 1 / 0;
            """;
        Path import2 = testDir.resolve("import2.ebs");
        Files.writeString(import2, import2Content);
        
        // Create main file (10 lines) importing both
        String mainContent = """
            // main.ebs
            // line 2
            // line 3
            // line 4
            // line 5
            // line 6
            // line 7
            // line 8
            // line 9
            // line 10
            import "%s";
            import "%s";
            """.formatted(import1.toString(), import2.toString());
        Path main = testDir.resolve("main.ebs");
        Files.writeString(main, mainContent);
        
        System.out.println("  Main file: 10 lines");
        System.out.println("  Import 1: 5 lines (no error)");
        System.out.println("  Import 2: error on line 4");
        System.out.println("  Expected error line: 4 (with fix)");
        System.out.println("  Without fix would be: 19 (10 + 5 + 4)");
        
        try {
            RuntimeContext ctx = Parser.parse(main);
            new Interpreter().interpret(ctx);
            throw new AssertionError("Expected error was not thrown");
        } catch (Exception e) {
            String lineNum = extractLineNumber(e.getMessage());
            if ("4".equals(lineNum)) {
                System.out.println("  ✓ PASSED: Error correctly reports line 4");
            } else {
                throw new AssertionError("Expected line 4, got line " + lineNum);
            }
        } finally {
            cleanup(testDir, import1, import2, main);
        }
    }
    
    private static void testNestedImports() throws Exception {
        System.out.println("Test 3: Nested Imports (Import Chain)");
        System.out.println("──────────────────────────────────────");
        
        Path testDir = Files.createTempDirectory("test_nested_imports");
        
        // Create deepest import with error on line 3
        String level3Content = """
            // level3.ebs
            // line 2
            var deep: int = 1 / 0;
            """;
        Path level3 = testDir.resolve("level3.ebs");
        Files.writeString(level3, level3Content);
        
        // Create mid-level import (7 lines) that imports level3
        String level2Content = createPaddedMain(7, level3.toString());
        Path level2 = testDir.resolve("level2.ebs");
        Files.writeString(level2, level2Content);
        
        // Create top-level import (12 lines) that imports level2
        String level1Content = createPaddedMain(12, level2.toString());
        Path level1 = testDir.resolve("level1.ebs");
        Files.writeString(level1, level1Content);
        
        // Create main file (20 lines) that imports level1
        String mainContent = createPaddedMain(20, level1.toString());
        Path main = testDir.resolve("main.ebs");
        Files.writeString(main, mainContent);
        
        System.out.println("  Chain: main(20) -> level1(12) -> level2(7) -> level3(error@3)");
        System.out.println("  Expected error line: 3 (with fix)");
        System.out.println("  Without fix would be: 42 (20 + 12 + 7 + 3)");
        
        try {
            RuntimeContext ctx = Parser.parse(main);
            new Interpreter().interpret(ctx);
            throw new AssertionError("Expected error was not thrown");
        } catch (Exception e) {
            String lineNum = extractLineNumber(e.getMessage());
            if ("3".equals(lineNum)) {
                System.out.println("  ✓ PASSED: Error correctly reports line 3");
            } else {
                throw new AssertionError("Expected line 3, got line " + lineNum);
            }
        } finally {
            cleanup(testDir, level3, level2, level1, main);
        }
    }
    
    private static String createPaddedMain(int lines, String importPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Main file\n");
        for (int i = 2; i <= lines; i++) {
            sb.append("// line ").append(i).append("\n");
        }
        sb.append("import \"").append(importPath).append("\";\n");
        return sb.toString();
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
    
    private static void cleanup(Path dir, Path... files) throws Exception {
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
        Files.deleteIfExists(dir);
    }
}
