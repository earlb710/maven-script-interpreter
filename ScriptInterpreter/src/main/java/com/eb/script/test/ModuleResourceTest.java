package com.eb.script.test;

import java.io.InputStream;

/**
 * Test class to verify resource loading works properly in the module system.
 * Tests getResourceAsStream for various resources used by the application.
 */
public class ModuleResourceTest {
    
    public static void main(String[] args) {
        System.out.println("Testing resource loading in module com.eb.scriptinterpreter...");
        
        boolean allPassed = true;
        
        // Test loading resources that are used in the actual code
        String[] resourcesToTest = {
            "/css/console.css",
            "/icons/folder.png",
            "/icons/file.png",
            "/scripts/help.ebs",
            "/images/chess/white_pawn.svg"
        };
        
        for (String resourcePath : resourcesToTest) {
            try (InputStream is = ModuleResourceTest.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    System.out.println("✓ Successfully loaded resource: " + resourcePath);
                } else {
                    System.err.println("✗ Resource not found: " + resourcePath);
                    allPassed = false;
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to load resource " + resourcePath + ": " + e.getMessage());
                allPassed = false;
            }
        }
        
        if (allPassed) {
            System.out.println("\n=== Module Resource Test PASSED ===");
            System.out.println("All resources can be loaded from the module.");
            System.out.println("The 'opens' directives for resource packages are working correctly.");
        } else {
            System.err.println("\n=== Module Resource Test FAILED ===");
            System.exit(1);
        }
    }
}
