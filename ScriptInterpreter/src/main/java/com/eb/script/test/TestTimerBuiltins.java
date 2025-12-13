package com.eb.script.test;

import com.eb.script.interpreter.builtins.BuiltinsTimer;
import com.eb.script.interpreter.InterpreterError;

/**
 * Comprehensive test suite for Timer builtins functionality.
 * Tests all timer operations including auto-initialization, bulk operations,
 * thread safety, and error handling.
 * 
 * @author Earl Bosch
 */
public class TestTimerBuiltins {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Timer Builtins Test Suite ===");
        System.out.println();
        
        try {
            testBasicStartStop();
            testAutoInitialization();
            testPeriodTracking();
            testFormattedStrings();
            testTimerState();
            testContinueFunctionality();
            testMultipleTimers();
            testBulkClear();
            testThreadSafety();
            testEdgeCases();
            testErrorHandling();
            
            System.out.println();
            System.out.println("=== Test Summary ===");
            System.out.println("Tests Passed: " + testsPassed);
            System.out.println("Tests Failed: " + testsFailed);
            
            if (testsFailed == 0) {
                System.out.println("✓ All tests passed!");
            } else {
                System.out.println("✗ Some tests failed");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("✗ Fatal error during testing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // ===== Test Methods =====
    
    private static void testBasicStartStop() throws Exception {
        System.out.println("Test 1: Basic Start/Stop Operations");
        
        // Test starting and stopping a timer
        String timerId = (String) BuiltinsTimer.dispatch("timer.start", new Object[]{"test1"});
        assertTrue("Start should return timer ID", timerId.equals("test1"));
        
        Thread.sleep(100);
        
        long elapsed = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{"test1"});
        assertTrue("Elapsed time should be >= 90ms", elapsed >= 90);
        assertTrue("Elapsed time should be <= 200ms", elapsed <= 200);
        
        // Clean up
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"test1"});
        
        System.out.println("  ✓ Basic start/stop works correctly");
        System.out.println();
    }
    
    private static void testAutoInitialization() throws Exception {
        System.out.println("Test 2: Auto-initialization (No Null Pointer Errors)");
        
        // Test getPeriod on non-existent timer
        long period = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"nonexistent1"});
        assertTrue("getPeriod should auto-init and return 0", period == 0);
        
        // Test getPeriodString on non-existent timer
        String periodStr = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"nonexistent2"});
        assertTrue("getPeriodString should auto-init and return 0.000", periodStr.equals("0.000"));
        
        // Test isRunning on non-existent timer
        boolean running = (Boolean) BuiltinsTimer.dispatch("timer.isrunning", new Object[]{"nonexistent3"});
        assertTrue("isRunning should auto-init and return false", !running);
        
        // Test stop on non-existent timer
        long elapsed = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{"nonexistent4"});
        assertTrue("stop should auto-init and return 0", elapsed == 0);
        
        // Test reset on non-existent timer
        boolean resetResult = (Boolean) BuiltinsTimer.dispatch("timer.reset", new Object[]{"nonexistent5"});
        assertTrue("reset should auto-init and return true", resetResult);
        
        // Test continue on non-existent timer
        boolean continueResult = (Boolean) BuiltinsTimer.dispatch("timer.continue", new Object[]{"nonexistent6"});
        assertTrue("continue should auto-init and return true", continueResult);
        
        // Clean up any auto-created timers
        BuiltinsTimer.dispatch("timer.clear", new Object[0]);
        
        System.out.println("  ✓ All operations auto-initialize correctly");
        System.out.println();
    }
    
    private static void testPeriodTracking() throws Exception {
        System.out.println("Test 3: Period Tracking While Running");
        
        BuiltinsTimer.dispatch("timer.start", new Object[]{"period_test"});
        
        Thread.sleep(50);
        long period1 = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"period_test"});
        
        Thread.sleep(50);
        long period2 = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"period_test"});
        
        assertTrue("Second period should be greater than first", period2 > period1);
        assertTrue("Period should be cumulative", period2 >= 90);
        
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"period_test"});
        
        // After stopping, period should remain constant
        long period3 = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"period_test"});
        long period4 = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"period_test"});
        assertTrue("Period should remain constant after stop", period3 == period4);
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"period_test"});
        
        System.out.println("  ✓ Period tracking works correctly");
        System.out.println();
    }
    
    private static void testFormattedStrings() throws Exception {
        System.out.println("Test 4: Formatted String Output");
        
        BuiltinsTimer.dispatch("timer.start", new Object[]{"format_test"});
        Thread.sleep(1234);
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"format_test"});
        
        // Test different decimal precisions
        String str0 = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"format_test", 0});
        String str1 = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"format_test", 1});
        String str2 = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"format_test", 2});
        String str3 = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"format_test", 3});
        String strDefault = (String) BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"format_test"});
        
        assertTrue("0 decimals should have no decimal point or 1 char", str0.matches("\\d+"));
        assertTrue("1 decimal should match pattern", str1.matches("\\d+\\.\\d{1}"));
        assertTrue("2 decimals should match pattern", str2.matches("\\d+\\.\\d{2}"));
        assertTrue("3 decimals should match pattern", str3.matches("\\d+\\.\\d{3}"));
        assertTrue("Default should have 3 decimals", strDefault.matches("\\d+\\.\\d{3}"));
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"format_test"});
        
        System.out.println("  ✓ Formatted string output works correctly");
        System.out.println();
    }
    
    private static void testTimerState() throws Exception {
        System.out.println("Test 5: Timer State Management");
        
        BuiltinsTimer.dispatch("timer.start", new Object[]{"state_test"});
        boolean running1 = (Boolean) BuiltinsTimer.dispatch("timer.isrunning", new Object[]{"state_test"});
        assertTrue("Timer should be running after start", running1);
        
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"state_test"});
        boolean running2 = (Boolean) BuiltinsTimer.dispatch("timer.isrunning", new Object[]{"state_test"});
        assertTrue("Timer should not be running after stop", !running2);
        
        BuiltinsTimer.dispatch("timer.reset", new Object[]{"state_test"});
        boolean running3 = (Boolean) BuiltinsTimer.dispatch("timer.isrunning", new Object[]{"state_test"});
        assertTrue("Timer should not be running after reset", !running3);
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"state_test"});
        
        System.out.println("  ✓ Timer state management works correctly");
        System.out.println();
    }
    
    private static void testContinueFunctionality() throws Exception {
        System.out.println("Test 6: Continue/Lap Functionality");
        
        BuiltinsTimer.dispatch("timer.start", new Object[]{"continue_test"});
        Thread.sleep(100);
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"continue_test"});
        
        long firstPeriod = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"continue_test"});
        long firstContinue = (Long) BuiltinsTimer.dispatch("timer.getcontinueperiod", new Object[]{"continue_test"});
        
        assertTrue("First period and continue period should be equal initially", 
                   Math.abs(firstPeriod - firstContinue) < 5);
        
        BuiltinsTimer.dispatch("timer.continue", new Object[]{"continue_test"});
        Thread.sleep(100);
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"continue_test"});
        
        long totalPeriod = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"continue_test"});
        long continuePeriod = (Long) BuiltinsTimer.dispatch("timer.getcontinueperiod", new Object[]{"continue_test"});
        
        assertTrue("Total period should be greater than first period", totalPeriod > firstPeriod);
        assertTrue("Continue period should be approximately 100ms", 
                   continuePeriod >= 90 && continuePeriod <= 200);
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"continue_test"});
        
        System.out.println("  ✓ Continue/lap functionality works correctly");
        System.out.println();
    }
    
    private static void testMultipleTimers() throws Exception {
        System.out.println("Test 7: Multiple Concurrent Timers");
        
        BuiltinsTimer.dispatch("timer.start", new Object[]{"timer_a"});
        Thread.sleep(50);
        BuiltinsTimer.dispatch("timer.start", new Object[]{"timer_b"});
        Thread.sleep(50);
        BuiltinsTimer.dispatch("timer.start", new Object[]{"timer_c"});
        Thread.sleep(50);
        
        long timeA = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{"timer_a"});
        long timeB = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{"timer_b"});
        long timeC = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{"timer_c"});
        
        assertTrue("Timer A should run longest", timeA > timeB);
        assertTrue("Timer B should run longer than C", timeB > timeC);
        assertTrue("Timer C should be approximately 50ms", timeC >= 40 && timeC <= 150);
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"timer_a"});
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"timer_b"});
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"timer_c"});
        
        System.out.println("  ✓ Multiple concurrent timers work correctly");
        System.out.println();
    }
    
    private static void testBulkClear() throws Exception {
        System.out.println("Test 8: Bulk Clear Operation");
        
        // Create several timers
        BuiltinsTimer.dispatch("timer.start", new Object[]{"bulk1"});
        BuiltinsTimer.dispatch("timer.start", new Object[]{"bulk2"});
        BuiltinsTimer.dispatch("timer.start", new Object[]{"bulk3"});
        BuiltinsTimer.dispatch("timer.start", new Object[]{"bulk4"});
        BuiltinsTimer.dispatch("timer.start", new Object[]{"bulk5"});
        
        // Clear all timers
        int cleared = (Integer) BuiltinsTimer.dispatch("timer.clear", new Object[0]);
        assertTrue("Should clear at least 5 timers", cleared >= 5);
        
        // Verify they're gone by checking auto-initialization
        long period = (Long) BuiltinsTimer.dispatch("timer.getperiod", new Object[]{"bulk1"});
        assertTrue("Timer should be auto-initialized with 0 after clear", period == 0);
        
        // Remove the auto-created timer
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"bulk1"});
        
        System.out.println("  ✓ Bulk clear operation works correctly");
        System.out.println();
    }
    
    private static void testThreadSafety() throws Exception {
        System.out.println("Test 9: Thread Safety");
        
        final int numThreads = 10;
        final int operationsPerThread = 100;
        Thread[] threads = new Thread[numThreads];
        final boolean[] errors = new boolean[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String timerId = "thread_" + threadId + "_" + j;
                        BuiltinsTimer.dispatch("timer.start", new Object[]{timerId});
                        Thread.sleep(1);
                        BuiltinsTimer.dispatch("timer.stop", new Object[]{timerId});
                        BuiltinsTimer.dispatch("timer.getperiod", new Object[]{timerId});
                        BuiltinsTimer.dispatch("timer.remove", new Object[]{timerId});
                    }
                } catch (Exception e) {
                    errors[threadId] = true;
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Check for errors
        for (boolean error : errors) {
            assertTrue("No thread should have errors", !error);
        }
        
        // Clean up any remaining timers
        BuiltinsTimer.dispatch("timer.clear", new Object[0]);
        
        System.out.println("  ✓ Thread safety verified (" + numThreads + " threads, " + operationsPerThread + " ops each)");
        System.out.println();
    }
    
    private static void testEdgeCases() throws Exception {
        System.out.println("Test 10: Edge Cases");
        
        // Test with empty string timer ID
        BuiltinsTimer.dispatch("timer.start", new Object[]{""});
        long elapsed = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{""});
        assertTrue("Empty string ID should work", elapsed >= 0);
        BuiltinsTimer.dispatch("timer.remove", new Object[]{""});
        
        // Test with very long timer ID
        String longId = "a".repeat(1000);
        BuiltinsTimer.dispatch("timer.start", new Object[]{longId});
        elapsed = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{longId});
        assertTrue("Long ID should work", elapsed >= 0);
        BuiltinsTimer.dispatch("timer.remove", new Object[]{longId});
        
        // Test with special characters in ID
        String specialId = "timer-with.special_chars@123!";
        BuiltinsTimer.dispatch("timer.start", new Object[]{specialId});
        elapsed = (Long) BuiltinsTimer.dispatch("timer.stop", new Object[]{specialId});
        assertTrue("Special chars ID should work", elapsed >= 0);
        BuiltinsTimer.dispatch("timer.remove", new Object[]{specialId});
        
        // Test removing non-existent timer
        boolean removed = (Boolean) BuiltinsTimer.dispatch("timer.remove", new Object[]{"does_not_exist"});
        assertTrue("Removing non-existent timer should return false", !removed);
        
        // Test clear when no timers exist
        BuiltinsTimer.dispatch("timer.clear", new Object[0]);
        int cleared = (Integer) BuiltinsTimer.dispatch("timer.clear", new Object[0]);
        assertTrue("Clearing empty registry should return 0", cleared == 0);
        
        System.out.println("  ✓ Edge cases handled correctly");
        System.out.println();
    }
    
    private static void testErrorHandling() throws Exception {
        System.out.println("Test 11: Error Handling");
        
        // Test invalid decimal precision
        BuiltinsTimer.dispatch("timer.start", new Object[]{"error_test"});
        Thread.sleep(50);
        BuiltinsTimer.dispatch("timer.stop", new Object[]{"error_test"});
        
        try {
            BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"error_test", -1});
            assertTrue("Should throw error for negative decimals", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Should catch IllegalArgumentException for negative decimals", true);
        }
        
        try {
            BuiltinsTimer.dispatch("timer.getperiodstring", new Object[]{"error_test", 4});
            assertTrue("Should throw error for decimals > 3", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Should catch IllegalArgumentException for decimals > 3", true);
        }
        
        // Test missing required arguments
        try {
            BuiltinsTimer.dispatch("timer.start", new Object[0]);
            assertTrue("Should throw error for missing timerId", false);
        } catch (InterpreterError e) {
            assertTrue("Should catch InterpreterError for missing argument", true);
        }
        
        // Test invalid builtin name
        try {
            BuiltinsTimer.dispatch("timer.invalid", new Object[]{"test"});
            assertTrue("Should throw error for unknown builtin", false);
        } catch (InterpreterError e) {
            assertTrue("Should catch InterpreterError for unknown builtin", true);
        }
        
        BuiltinsTimer.dispatch("timer.remove", new Object[]{"error_test"});
        
        System.out.println("  ✓ Error handling works correctly");
        System.out.println();
    }
    
    // ===== Helper Methods =====
    
    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            testsPassed++;
        } else {
            testsFailed++;
            System.err.println("  ✗ FAILED: " + message);
        }
    }
}
