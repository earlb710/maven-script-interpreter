package com.eb.ui.ebs;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashSet;

/**
 * Test class to validate custom function highlighting logic
 */
public class HighlightingTest {
    
    private static final String[] EBS_KEYWORDS = new String[]{
        "var", "print", "call", "return",
        "if", "then", "else", "while", "do", "foreach", "in", "break", "continue",
        "connect", "use", "cursor", "open", "close", "connection",
        "select", "from", "where", "order", "by", "group", "having"
    };

    private static final String[] EBS_TYPES = new String[]{
        "byte", "int", "integer", "long", "float", "double", "string", "date", "bool", "boolean", "json"
    };
    
    private static Set<String> extractCustomFunctions(String text) {
        Set<String> functions = new HashSet<>();
        
        Pattern funcDefPattern = Pattern.compile(
            "(?:^|\\s)(?:function\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*(?:\\([^)]*\\))?\\s*(?:return\\s+[A-Za-z_][A-Za-z0-9_]*)?\\s*\\{",
            Pattern.MULTILINE
        );
        
        Matcher m = funcDefPattern.matcher(text);
        while (m.find()) {
            String funcName = m.group(1);
            if (funcName != null && !isKeywordOrType(funcName)) {
                functions.add(funcName.toLowerCase());
            }
        }
        
        return functions;
    }
    
    private static boolean isKeywordOrType(String name) {
        String lowerName = name.toLowerCase();
        for (String kw : EBS_KEYWORDS) {
            if (kw.equals(lowerName)) {
                return true;
            }
        }
        for (String tp : EBS_TYPES) {
            if (tp.equals(lowerName)) {
                return true;
            }
        }
        return false;
    }
    
    public static void main(String[] args) {
        String testCode = """
            // Define custom functions
            myCustomFunction() {
                print "Hello"
            }
            
            calculateSum(a int, b int) return int {
                return a + b
            }
            
            function anotherFunction {
                print "test"
            }
            
            // Calls
            myCustomFunction()
            #myCustomFunction
            undefinedFunction()
            #undefinedFunction
            """;
        
        Set<String> customFunctions = extractCustomFunctions(testCode);
        
        System.out.println("Extracted custom functions:");
        for (String func : customFunctions) {
            System.out.println("  - " + func);
        }
        
        // Test function call detection
        String HASHCALL = "#\\s*([A-Za-z_][A-Za-z0-9_]*)";
        String FUNCNAME = "\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()";
        
        Pattern hashPattern = Pattern.compile(HASHCALL);
        Pattern funcPattern = Pattern.compile(FUNCNAME);
        
        System.out.println("\nHash calls detected:");
        Matcher hashMatcher = hashPattern.matcher(testCode);
        while (hashMatcher.find()) {
            String funcName = hashMatcher.group(1);
            System.out.println("  - #" + funcName + " at position " + hashMatcher.start());
        }
        
        System.out.println("\nFunction calls detected:");
        Matcher funcMatcher = funcPattern.matcher(testCode);
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            String lowerName = funcName.toLowerCase();
            if (customFunctions.contains(lowerName)) {
                System.out.println("  - " + funcName + "() [CUSTOM] at position " + funcMatcher.start());
            } else if (!isKeywordOrType(funcName)) {
                System.out.println("  - " + funcName + "() [UNDEFINED] at position " + funcMatcher.start());
            }
        }
    }
}
