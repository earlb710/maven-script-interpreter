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
        
        // Test the regex patterns used in the actual highlighting code
        String HASHCALL = "#\\s*([A-Za-z_][A-Za-z0-9_]*)";
        String FUNCNAME = "\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()";
        
        // Build the master pattern similar to EbsTab
        String master = "(?<HASHCALL>" + HASHCALL + ")"
                + "|(?<FUNCTION>" + FUNCNAME + ")";
        
        Pattern combinedPattern = Pattern.compile(master, Pattern.MULTILINE);
        Matcher m = combinedPattern.matcher(testCode);
        
        System.out.println("\nMatches found:");
        while (m.find()) {
            if (m.group("HASHCALL") != null) {
                String matched = m.group("HASHCALL");
                String funcName = matched.replaceFirst("^#\\s*", "");
                String lowerName = funcName.toLowerCase();
                String type = customFunctions.contains(lowerName) ? "CUSTOM" : "UNDEFINED";
                System.out.println("  - Hash call: " + matched + " -> funcName: " + funcName + " [" + type + "]");
            } else if (m.group("FUNCTION") != null) {
                String matched = m.group("FUNCTION");
                String funcName = matched.replaceFirst("\\s*\\($", "").trim();
                String lowerName = funcName.toLowerCase();
                String type = customFunctions.contains(lowerName) ? "CUSTOM" : "UNDEFINED";
                System.out.println("  - Function call: " + matched + " -> funcName: " + funcName + " [" + type + "]");
            }
        }
        
        // Test case-insensitivity
        System.out.println("\n=== Testing Case Insensitivity ===");
        String caseTestCode = """
            MyFunc() {
                print "test"
            }
            
            MyFunc()
            myFunc()
            MYFUNC()
            #MyFunc
            #myfunc
            """;
        
        Set<String> caseFuncs = extractCustomFunctions(caseTestCode);
        System.out.println("\nExtracted functions (should all be lowercase):");
        for (String func : caseFuncs) {
            System.out.println("  - " + func);
        }
        
        Pattern casePattern = Pattern.compile(master, Pattern.MULTILINE);
        Matcher caseM = casePattern.matcher(caseTestCode);
        
        System.out.println("\nCase-insensitive matches (all should be CUSTOM):");
        while (caseM.find()) {
            if (caseM.group("HASHCALL") != null) {
                String matched = caseM.group("HASHCALL");
                String funcName = matched.replaceFirst("^#\\s*", "");
                String lowerName = funcName.toLowerCase();
                String type = caseFuncs.contains(lowerName) ? "CUSTOM" : "UNDEFINED";
                System.out.println("  - " + matched + " -> toLowerCase: " + lowerName + " [" + type + "]");
            } else if (caseM.group("FUNCTION") != null) {
                String matched = caseM.group("FUNCTION");
                String funcName = matched.replaceFirst("\\s*\\($", "").trim();
                String lowerName = funcName.toLowerCase();
                String type = caseFuncs.contains(lowerName) ? "CUSTOM" : "UNDEFINED";
                System.out.println("  - " + matched + " -> toLowerCase: " + lowerName + " [" + type + "]");
            }
        }
    }
}
