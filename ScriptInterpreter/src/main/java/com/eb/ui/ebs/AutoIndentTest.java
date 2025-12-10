package com.eb.ui.ebs;

/**
 * Simple test to verify the auto-indent logic.
 * This doesn't run as a unit test but can be used to verify the logic works correctly.
 */
public class AutoIndentTest {
    
    public static void main(String[] args) {
        System.out.println("Testing auto-indent logic...");
        
        // Test 1: Simple indentation with spaces (caret at end of line before newline)
        testAutoIndent(
            "    var int x = 10\n    var int y = 20",
            18, // caret position at end of first line (after "10", before "\n")
            "    var int x = 10\n    \n    var int y = 20",
            "Test 1: Simple indentation with spaces"
        );
        
        // Test 2: Indentation with tabs (caret at end of line before newline)
        testAutoIndent(
            "\t\tvar int x = 10\n\t\tvar int y = 20",
            16, // caret position at end of first line (after "10", before "\n")
            "\t\tvar int x = 10\n\t\t\n\t\tvar int y = 20",
            "Test 2: Indentation with tabs"
        );
        
        // Test 3: No indentation
        testAutoIndent(
            "var int x = 10\nvar int y = 20",
            14, // caret position at end of first line
            "var int x = 10\n\nvar int y = 20",
            "Test 3: No indentation"
        );
        
        // Test 4: Mixed indentation
        testAutoIndent(
            "\t    var int x = 10\n\t    var int y = 20",
            19, // caret position at end of first line
            "\t    var int x = 10\n\t    \n\t    var int y = 20",
            "Test 4: Mixed indentation"
        );
        
        // Test 5: Mid-line insertion
        testAutoIndent(
            "    var int x = 10\n    var int y = 20",
            12, // caret position in middle of first line
            "    var int \n    x = 10\n    var int y = 20",
            "Test 5: Mid-line insertion"
        );
        
        System.out.println("\nAll tests completed!");
    }
    
    private static void testAutoIndent(String original, int caretPos, String expected, String testName) {
        String result = simulateAutoIndent(original, caretPos);
        boolean passed = result.equals(expected);
        
        System.out.println(testName + ": " + (passed ? "PASS" : "FAIL"));
        if (!passed) {
            System.out.println("  Original: " + escapeNewlines(original));
            System.out.println("  Expected: " + escapeNewlines(expected));
            System.out.println("  Got:      " + escapeNewlines(result));
        }
    }
    
    /**
     * Simulates the auto-indent logic from handleAutoIndent method
     */
    private static String simulateAutoIndent(String text, int caretPos) {
        // Find the start of the current line
        int lineStart = caretPos;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        // Determine the indentation of the current line
        int indentEnd = lineStart;
        while (indentEnd < text.length() && (text.charAt(indentEnd) == ' ' || text.charAt(indentEnd) == '\t')) {
            indentEnd++;
        }
        
        // Extract the indentation characters
        String indentation = text.substring(lineStart, indentEnd);
        
        // Insert newline + indentation at caret position
        return text.substring(0, caretPos) + "\n" + indentation + text.substring(caretPos);
    }
    
    private static String escapeNewlines(String s) {
        return s.replace("\n", "\\n").replace("\t", "\\t");
    }
}
