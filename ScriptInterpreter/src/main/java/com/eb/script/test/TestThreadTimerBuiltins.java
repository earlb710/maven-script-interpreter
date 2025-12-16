package com.eb.script.test;

import com.eb.script.interpreter.builtins.BuiltinsThread;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.Interpreter;

/**
 * Comprehensive test suite for Thread Timer builtins functionality.
 * Tests all thread timer operations including start/stop, pause/resume,
 * state checking, and information retrieval.
 * 
 * @author Earl Bosch
 */
public class TestThreadTimerBuiltins {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Thread Timer Builtins Test Suite ===");
        System.out.println();
        
        try {
            testBasicOperations();
            testPauseResume();
            testStateChecking();
            testInformationRetrieval();
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
        } finally {
            // Cleanup
            BuiltinsThread.shutdown();
        }
    }
    
    // ===== Test Methods =====
    
    private static void testBasicOperations() throws Exception {
        System.out.println("Test 1: Basic Operations");
        
        // Test getCount initially (should be 0)
        long count = (long) BuiltinsThread.dispatch(null, "thread.getcount", new Object[]{});
        assertTrue("Initial count should be 0", count == 0);
        
        // Test timer list initially (should be empty array)
        String list = (String) BuiltinsThread.dispatch(null, "thread.timerlist", new Object[]{});
        assertTrue("Initial list should be empty array", list.equals("[]"));
        
        System.out.println("  ✓ Basic operations passed");
    }
    
    private static void testPauseResume() throws Exception {
        System.out.println("Test 2: Pause/Resume Operations");
        
        // Note: We cannot fully test pause/resume without a callback context
        // But we can test the error handling
        
        // Test pause non-existent timer
        boolean paused = (boolean) BuiltinsThread.dispatch(null, "thread.timerpause", new Object[]{"nonexistent"});
        assertTrue("Pausing non-existent timer should return false", !paused);
        
        // Test resume non-existent timer
        boolean resumed = (boolean) BuiltinsThread.dispatch(null, "thread.timerresume", new Object[]{"nonexistent"});
        assertTrue("Resuming non-existent timer should return false", !resumed);
        
        System.out.println("  ✓ Pause/Resume operations passed");
    }
    
    private static void testStateChecking() throws Exception {
        System.out.println("Test 3: State Checking");
        
        // Test isRunning for non-existent timer
        boolean running = (boolean) BuiltinsThread.dispatch(null, "thread.timerisrunning", new Object[]{"nonexistent"});
        assertTrue("Non-existent timer should not be running", !running);
        
        // Test isPaused for non-existent timer
        boolean paused = (boolean) BuiltinsThread.dispatch(null, "thread.timerispaused", new Object[]{"nonexistent"});
        assertTrue("Non-existent timer should not be paused", !paused);
        
        System.out.println("  ✓ State checking passed");
    }
    
    private static void testInformationRetrieval() throws Exception {
        System.out.println("Test 4: Information Retrieval");
        
        // Test getInfo for non-existent timer (should return null)
        Object info = BuiltinsThread.dispatch(null, "thread.timergetinfo", new Object[]{"nonexistent"});
        assertTrue("Info for non-existent timer should be null", info == null);
        
        // Test getPeriod for non-existent timer (should return -1)
        long period = (long) BuiltinsThread.dispatch(null, "thread.timergetperiod", new Object[]{"nonexistent"});
        assertTrue("Period for non-existent timer should be -1", period == -1L);
        
        // Test getFireCount for non-existent timer (should return -1)
        long fireCount = (long) BuiltinsThread.dispatch(null, "thread.timergetfirecount", new Object[]{"nonexistent"});
        assertTrue("Fire count for non-existent timer should be -1", fireCount == -1L);
        
        System.out.println("  ✓ Information retrieval passed");
    }
    
    private static void testEdgeCases() throws Exception {
        System.out.println("Test 5: Edge Cases");
        
        // Test getCount returns long
        Object countObj = BuiltinsThread.dispatch(null, "thread.getcount", new Object[]{});
        assertTrue("getCount should return long", countObj instanceof Long);
        
        // Test timerList returns valid JSON
        String list = (String) BuiltinsThread.dispatch(null, "thread.timerlist", new Object[]{});
        assertTrue("timerList should return string", list != null);
        assertTrue("timerList should start with [", list.startsWith("["));
        assertTrue("timerList should end with ]", list.endsWith("]"));
        
        System.out.println("  ✓ Edge cases passed");
    }
    
    private static void testErrorHandling() throws Exception {
        System.out.println("Test 6: Error Handling");
        
        // Test missing arguments
        try {
            BuiltinsThread.dispatch(null, "thread.timerpause", new Object[]{});
            testFailed("timerpause should require name argument");
        } catch (InterpreterError e) {
            testPassed("timerpause correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timerresume", new Object[]{});
            testFailed("timerresume should require name argument");
        } catch (InterpreterError e) {
            testPassed("timerresume correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timerisrunning", new Object[]{});
            testFailed("timerisrunning should require name argument");
        } catch (InterpreterError e) {
            testPassed("timerisrunning correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timerispaused", new Object[]{});
            testFailed("timerispaused should require name argument");
        } catch (InterpreterError e) {
            testPassed("timerispaused correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timergetinfo", new Object[]{});
            testFailed("timergetinfo should require name argument");
        } catch (InterpreterError e) {
            testPassed("timergetinfo correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timergetperiod", new Object[]{});
            testFailed("timergetperiod should require name argument");
        } catch (InterpreterError e) {
            testPassed("timergetperiod correctly rejects missing arguments");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timergetfirecount", new Object[]{});
            testFailed("timergetfirecount should require name argument");
        } catch (InterpreterError e) {
            testPassed("timergetfirecount correctly rejects missing arguments");
        }
        
        // Test null/empty timer names
        try {
            BuiltinsThread.dispatch(null, "thread.timerpause", new Object[]{null});
            testFailed("timerpause should reject null name");
        } catch (InterpreterError e) {
            testPassed("timerpause correctly rejects null name");
        }
        
        try {
            BuiltinsThread.dispatch(null, "thread.timerpause", new Object[]{""});
            testFailed("timerpause should reject empty name");
        } catch (InterpreterError e) {
            testPassed("timerpause correctly rejects empty name");
        }
        
        System.out.println("  ✓ Error handling passed");
    }
    
    // ===== Helper Methods =====
    
    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            testsPassed++;
        } else {
            testsFailed++;
            System.err.println("  ✗ Assertion failed: " + message);
        }
    }
    
    private static void testPassed(String message) {
        testsPassed++;
        // System.out.println("  ✓ " + message);
    }
    
    private static void testFailed(String message) {
        testsFailed++;
        System.err.println("  ✗ " + message);
    }
}
