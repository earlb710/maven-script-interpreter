package com.eb.script.test;

/**
 * Test runner for all timer-related tests.
 * Executes both Timed utility tests and Timer builtins tests.
 * 
 * @author Earl Bosch
 */
public class RunTimerTests {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        Timer Functionality - Complete Test Suite          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        boolean allPassed = true;
        
        try {
            // Run Timed utility tests
            System.out.println("┌─────────────────────────────────────────────────────────┐");
            System.out.println("│ Running Timed Utility Tests                            │");
            System.out.println("└─────────────────────────────────────────────────────────┘");
            System.out.println();
            
            TestTimedUtility.main(new String[0]);
            
            System.out.println();
            System.out.println("┌─────────────────────────────────────────────────────────┐");
            System.out.println("│ Running Timer Builtins Tests                           │");
            System.out.println("└─────────────────────────────────────────────────────────┘");
            System.out.println();
            
            // Run Timer builtins tests
            TestTimerBuiltins.main(new String[0]);
            
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              ALL TIMER TESTS PASSED! ✓                     ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            allPassed = false;
            System.err.println();
            System.err.println("╔════════════════════════════════════════════════════════════╗");
            System.err.println("║              SOME TESTS FAILED ✗                           ║");
            System.err.println("╚════════════════════════════════════════════════════════════╝");
            System.err.println();
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (!allPassed) {
            System.exit(1);
        }
    }
}
