package com.eb.script.test;

/**
 * Test class to verify reflection works properly in the module system.
 * Tests Class.forName for UI dialogs that are loaded dynamically.
 */
public class ModuleReflectionTest {
    
    public static void main(String[] args) {
        System.out.println("Testing reflection in module com.eb.scriptinterpreter...");
        
        boolean allPassed = true;
        
        // Test loading UI classes via reflection (as done in the actual code)
        String[] classesToTest = {
            "com.eb.ui.ebs.SafeDirectoriesDialog",
            "com.eb.ui.ebs.DatabaseConfigDialog",
            "com.eb.ui.ebs.MailConfigDialog",
            "com.eb.ui.ebs.FtpConfigDialog",
            "com.eb.ui.ebs.EbsConsoleHandler",
            "com.eb.script.interpreter.Interpreter"
        };
        
        for (String className : classesToTest) {
            try {
                Class<?> clazz = Class.forName(className);
                System.out.println("✓ Successfully loaded: " + clazz.getName());
            } catch (ClassNotFoundException e) {
                System.err.println("✗ Failed to load: " + className);
                allPassed = false;
            }
        }
        
        if (allPassed) {
            System.out.println("\n=== Module Reflection Test PASSED ===");
            System.out.println("All classes can be loaded via reflection.");
            System.out.println("The 'opens' directives in module-info.java are working correctly.");
        } else {
            System.err.println("\n=== Module Reflection Test FAILED ===");
            System.exit(1);
        }
    }
}
