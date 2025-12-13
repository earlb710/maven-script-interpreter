package com.eb.script.test;

import com.eb.util.Timed;

/**
 * Test suite for Timed utility class.
 * Tests the core timer functionality including start, stop, reset, continue,
 * and string formatting operations.
 * 
 * @author Earl Bosch
 */
public class TestTimedUtility {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Timed Utility Test Suite ===");
        System.out.println();
        
        try {
            testBasicTimer();
            testTimerReset();
            testTimerContinue();
            testRunningState();
            testPeriodMethods();
            testStringFormatting();
            testDecimalPrecision();
            testEdgeCases();
            
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
    
    private static void testBasicTimer() throws Exception {
        System.out.println("Test 1: Basic Timer Start/Stop");
        
        Timed timer = new Timed();
        timer.timerStart();
        
        assertTrue("Timer should be running after start", timer.isRunning());
        
        Thread.sleep(100);
        
        long elapsed = timer.timerStop();
        assertTrue("Elapsed time should be >= 90ms", elapsed >= 90);
        assertTrue("Elapsed time should be <= 200ms", elapsed <= 200);
        assertTrue("Timer should not be running after stop", !timer.isRunning());
        
        System.out.println("  ✓ Basic timer start/stop works correctly");
        System.out.println();
    }
    
    private static void testTimerReset() throws Exception {
        System.out.println("Test 2: Timer Reset");
        
        Timed timer = new Timed();
        timer.timerStart();
        Thread.sleep(100);
        
        long periodBefore = timer.getTimerPeriod();
        assertTrue("Period should be > 0 before reset", periodBefore > 0);
        assertTrue("Timer should be running before reset", timer.isRunning());
        
        timer.timerReset();
        long periodAfter = timer.getTimerPeriod();
        
        assertTrue("Period should be approximately 0 after reset", periodAfter < 10);
        // Note: timerReset() does NOT change the running state, only time markers
        assertTrue("Timer should still be running after reset (reset doesn't change state)", timer.isRunning());
        
        System.out.println("  ✓ Timer reset works correctly");
        System.out.println();
    }
    
    private static void testTimerContinue() throws Exception {
        System.out.println("Test 3: Timer Continue");
        
        Timed timer = new Timed();
        timer.timerStart();
        Thread.sleep(100);
        timer.timerStop();
        
        long firstPeriod = timer.getTimerPeriod();
        
        timer.timerContinue();
        assertTrue("Timer should be running after continue", timer.isRunning());
        
        Thread.sleep(100);
        timer.timerStop();
        
        long totalPeriod = timer.getTimerPeriod();
        long continuePeriod = timer.getContinuePeriod();
        
        assertTrue("Total period should be greater than first period", totalPeriod > firstPeriod);
        assertTrue("Continue period should be approximately 100ms", 
                   continuePeriod >= 90 && continuePeriod <= 200);
        
        System.out.println("  ✓ Timer continue works correctly");
        System.out.println();
    }
    
    private static void testRunningState() throws Exception {
        System.out.println("Test 4: Running State Tracking");
        
        Timed timer = new Timed();
        assertTrue("New timer should not be running", !timer.isRunning());
        
        timer.timerStart();
        assertTrue("Timer should be running after start", timer.isRunning());
        
        timer.timerStop();
        assertTrue("Timer should not be running after stop", !timer.isRunning());
        
        timer.timerContinue();
        assertTrue("Timer should be running after continue", timer.isRunning());
        
        // Note: timerReset() does NOT change the running state
        timer.timerReset();
        assertTrue("Timer should still be running after reset (reset only resets time markers)", timer.isRunning());
        
        // Stop to verify stop changes state
        timer.timerStop();
        assertTrue("Timer should not be running after stop", !timer.isRunning());
        
        System.out.println("  ✓ Running state tracking works correctly");
        System.out.println();
    }
    
    private static void testPeriodMethods() throws Exception {
        System.out.println("Test 5: Period Tracking Methods");
        
        Timed timer = new Timed();
        timer.timerStart();
        
        Thread.sleep(50);
        long period1 = timer.getTimerPeriod();
        
        Thread.sleep(50);
        long period2 = timer.getTimerPeriod();
        
        assertTrue("Period should increase while running", period2 > period1);
        
        timer.timerStop();
        
        long stoppedPeriod1 = timer.getTimerPeriod();
        Thread.sleep(50);
        long stoppedPeriod2 = timer.getTimerPeriod();
        
        assertTrue("Period should remain constant when stopped", stoppedPeriod1 == stoppedPeriod2);
        
        System.out.println("  ✓ Period tracking methods work correctly");
        System.out.println();
    }
    
    private static void testStringFormatting() throws Exception {
        System.out.println("Test 6: String Formatting");
        
        Timed timer = new Timed();
        timer.timerStart();
        Thread.sleep(1234);
        timer.timerStop();
        
        String millisStr = timer.getTimerString_milliseconds();
        assertTrue("Milliseconds string should be numeric", millisStr.matches("\\d+"));
        
        String secondsStr = timer.getTimerString_Seconds();
        assertTrue("Seconds string should match format", secondsStr.matches("\\d+\\.\\d{3}"));
        
        // Verify zero-padding
        timer = new Timed();
        timer.timerStart();
        Thread.sleep(5);
        timer.timerStop();
        
        String shortTime = timer.getTimerString_Seconds();
        assertTrue("Short time should have zero-padding", shortTime.matches("\\d+\\.\\d{3}"));
        
        System.out.println("  ✓ String formatting works correctly");
        System.out.println();
    }
    
    private static void testDecimalPrecision() throws Exception {
        System.out.println("Test 7: Decimal Precision Control");
        
        Timed timer = new Timed();
        timer.timerStart();
        Thread.sleep(1234);
        timer.timerStop();
        
        String str0 = timer.getTimerString_Seconds(0);
        String str1 = timer.getTimerString_Seconds(1);
        String str2 = timer.getTimerString_Seconds(2);
        String str3 = timer.getTimerString_Seconds(3);
        
        assertTrue("0 decimals should have no decimal point", str0.matches("\\d+"));
        assertTrue("1 decimal should match pattern", str1.matches("\\d+\\.\\d{1}"));
        assertTrue("2 decimals should match pattern", str2.matches("\\d+\\.\\d{2}"));
        assertTrue("3 decimals should match pattern", str3.matches("\\d+\\.\\d{3}"));
        
        // Test continue period strings
        timer.timerContinue();
        Thread.sleep(500);
        timer.timerStop();
        
        String contStr0 = timer.getContinueString_Seconds(0);
        String contStr1 = timer.getContinueString_Seconds(1);
        String contStr2 = timer.getContinueString_Seconds(2);
        String contStr3 = timer.getContinueString_Seconds(3);
        
        assertTrue("Continue 0 decimals should have no decimal point", contStr0.matches("\\d+"));
        assertTrue("Continue 1 decimal should match pattern", contStr1.matches("\\d+\\.\\d{1}"));
        assertTrue("Continue 2 decimals should match pattern", contStr2.matches("\\d+\\.\\d{2}"));
        assertTrue("Continue 3 decimals should match pattern", contStr3.matches("\\d+\\.\\d{3}"));
        
        System.out.println("  ✓ Decimal precision control works correctly");
        System.out.println();
    }
    
    private static void testEdgeCases() throws Exception {
        System.out.println("Test 8: Edge Cases and Error Handling");
        
        Timed timer = new Timed();
        
        // Test invalid decimal precision
        try {
            timer.timerStart();
            Thread.sleep(50);
            timer.timerStop();
            timer.getTimerString_Seconds(-1);
            assertTrue("Should throw exception for negative decimals", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Should catch IllegalArgumentException for negative decimals", true);
        }
        
        try {
            timer.getTimerString_Seconds(4);
            assertTrue("Should throw exception for decimals > 3", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Should catch IllegalArgumentException for decimals > 3", true);
        }
        
        // Test very short duration
        timer = new Timed();
        timer.timerStart();
        timer.timerStop();
        long elapsed = timer.getTimerPeriod();
        assertTrue("Very short duration should be >= 0", elapsed >= 0);
        
        // Test multiple start calls (restart behavior)
        timer = new Timed();
        timer.timerStart();
        Thread.sleep(50);
        long period1 = timer.getTimerPeriod();
        
        timer.timerStart(); // Restart
        Thread.sleep(50);
        long period2 = timer.getTimerPeriod();
        
        assertTrue("Restart should reset the timer", period2 < period1 + 10);
        
        System.out.println("  ✓ Edge cases and error handling work correctly");
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
