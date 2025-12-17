package com.eb.script.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test to verify that multiple debug views can coexist on different screens simultaneously.
 * 
 * This test simulates the pattern used in ScreenFactory where each screen has its own
 * debug panel and copy button feedback thread, tracked by screen name.
 * 
 * Run with: java com.eb.script.test.TestMultipleDebugViews
 */
public class TestMultipleDebugViews {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("MULTIPLE DEBUG VIEWS TEST");
        System.out.println("=".repeat(80));
        System.out.println();
        
        try {
            testMultipleScreensWithSeparateThreads();
            testIndependentThreadCleanup();
            testSimultaneousCopyOperations();
            
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("TEST RESULTS");
            System.out.println("=".repeat(80));
            System.out.println("Tests Passed: " + testsPassed);
            System.out.println("Tests Failed: " + testsFailed);
            System.out.println("=".repeat(80));
            
            if (testsFailed > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("TEST SUITE FAILED WITH EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            System.err.println("ASSERTION FAILED: " + message);
            testsFailed++;
            throw new AssertionError(message);
        }
    }
    
    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            System.err.println("ASSERTION FAILED: " + message);
            testsFailed++;
            throw new AssertionError(message);
        }
    }
    
    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            System.err.println("ASSERTION FAILED: " + message + " (expected: " + expected + ", actual: " + actual + ")");
            testsFailed++;
            throw new AssertionError(message);
        }
    }
    
    /**
     * Test that multiple screens can have their own debug panels with separate threads.
     */
    private static void testMultipleScreensWithSeparateThreads() throws InterruptedException {
        System.out.println("Test 1: Multiple screens can have separate debug panels and threads");
        System.out.println("-".repeat(80));
        
        // Simulate the debugCopyFeedbackThreads map from ScreenFactory
        ConcurrentHashMap<String, Thread> feedbackThreads = new ConcurrentHashMap<>();
        
        // Simulate three different screens
        String screen1 = "screen1";
        String screen2 = "screen2";
        String screen3 = "screen3";
        
        AtomicInteger completedThreads = new AtomicInteger(0);
        
        // Create feedback threads for each screen
        for (String screenName : new String[]{screen1, screen2, screen3}) {
            Thread feedbackThread = new Thread(() -> {
                try {
                    Thread.sleep(500);
                    completedThreads.incrementAndGet();
                } catch (InterruptedException ex) {
                    // Interrupted
                }
            }, "DebugPanel-CopyFeedback-" + screenName);
            
            feedbackThreads.put(screenName, feedbackThread);
            feedbackThread.start();
        }
        
        // Verify all three threads are tracked separately
        assertEquals(3, feedbackThreads.size(), "Should have 3 separate threads");
        assertTrue(feedbackThreads.containsKey(screen1), "Should have thread for screen1");
        assertTrue(feedbackThreads.containsKey(screen2), "Should have thread for screen2");
        assertTrue(feedbackThreads.containsKey(screen3), "Should have thread for screen3");
        
        // Verify all threads are running
        assertTrue(feedbackThreads.get(screen1).isAlive(), "screen1 thread should be running");
        assertTrue(feedbackThreads.get(screen2).isAlive(), "screen2 thread should be running");
        assertTrue(feedbackThreads.get(screen3).isAlive(), "screen3 thread should be running");
        
        // Wait for threads to complete
        Thread.sleep(600);
        
        // Verify all threads completed
        assertEquals(3, completedThreads.get(), "All 3 threads should have completed");
        
        // Cleanup
        feedbackThreads.clear();
        
        testsPassed++;
        System.out.println("✓ Test passed - Multiple screens can have independent debug panels");
        System.out.println();
    }
    
    /**
     * Test that closing one screen's debug panel doesn't affect other screens.
     */
    private static void testIndependentThreadCleanup() throws InterruptedException {
        System.out.println("Test 2: Closing one debug panel doesn't affect others");
        System.out.println("-".repeat(80));
        
        ConcurrentHashMap<String, Thread> feedbackThreads = new ConcurrentHashMap<>();
        
        String screen1 = "screen1";
        String screen2 = "screen2";
        
        AtomicInteger screen1Completed = new AtomicInteger(0);
        AtomicInteger screen2Completed = new AtomicInteger(0);
        
        // Create threads for both screens
        Thread thread1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                screen1Completed.set(1);
            } catch (InterruptedException ex) {
                screen1Completed.set(-1); // Interrupted
            }
        }, "DebugPanel-CopyFeedback-" + screen1);
        
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                screen2Completed.set(1);
            } catch (InterruptedException ex) {
                screen2Completed.set(-1); // Interrupted
            }
        }, "DebugPanel-CopyFeedback-" + screen2);
        
        feedbackThreads.put(screen1, thread1);
        feedbackThreads.put(screen2, thread2);
        thread1.start();
        thread2.start();
        
        // Small delay to ensure threads are running
        Thread.sleep(100);
        
        // Simulate closing screen1's debug panel (like cleanupDebugPanelResources does)
        Thread removedThread = feedbackThreads.remove(screen1);
        if (removedThread != null && removedThread.isAlive()) {
            removedThread.interrupt();
        }
        
        // Verify screen1 thread was interrupted
        removedThread.join(2000);
        assertEquals(-1, screen1Completed.get(), "screen1 thread should have been interrupted");
        
        // Verify screen2 thread is still running
        assertTrue(feedbackThreads.containsKey(screen2), "screen2 thread should still be tracked");
        assertTrue(feedbackThreads.get(screen2).isAlive(), "screen2 thread should still be running");
        
        // Wait for screen2 to complete naturally
        Thread.sleep(1000);
        assertEquals(1, screen2Completed.get(), "screen2 thread should have completed normally");
        
        // Cleanup
        feedbackThreads.clear();
        
        testsPassed++;
        System.out.println("✓ Test passed - Independent cleanup doesn't affect other screens");
        System.out.println();
    }
    
    /**
     * Test that multiple screens can perform copy operations simultaneously.
     */
    private static void testSimultaneousCopyOperations() throws InterruptedException {
        System.out.println("Test 3: Multiple screens can perform copy operations simultaneously");
        System.out.println("-".repeat(80));
        
        ConcurrentHashMap<String, Thread> feedbackThreads = new ConcurrentHashMap<>();
        
        String[] screens = {"screen1", "screen2", "screen3", "screen4", "screen5"};
        AtomicInteger[] completionFlags = new AtomicInteger[screens.length];
        
        // Initialize completion flags
        for (int i = 0; i < completionFlags.length; i++) {
            completionFlags[i] = new AtomicInteger(0);
        }
        
        // Simulate simultaneous copy operations on all screens
        for (int i = 0; i < screens.length; i++) {
            final int index = i;
            String screenName = screens[i];
            
            // Cancel any existing thread (simulates copy button click)
            Thread existingThread = feedbackThreads.get(screenName);
            if (existingThread != null && existingThread.isAlive()) {
                existingThread.interrupt();
            }
            
            // Create new feedback thread
            Thread feedbackThread = new Thread(() -> {
                try {
                    Thread.sleep(100);
                    completionFlags[index].set(1);
                } catch (InterruptedException ex) {
                    completionFlags[index].set(-1);
                }
            }, "DebugPanel-CopyFeedback-" + screenName);
            
            feedbackThreads.put(screenName, feedbackThread);
            feedbackThread.start();
        }
        
        // Verify all threads are tracked
        assertEquals(screens.length, feedbackThreads.size(), "Should have thread for each screen");
        
        // Wait for all to complete
        Thread.sleep(200);
        
        // Verify all completed successfully
        for (int i = 0; i < completionFlags.length; i++) {
            assertEquals(1, completionFlags[i].get(), "Thread for " + screens[i] + " should have completed");
        }
        
        // Cleanup
        feedbackThreads.clear();
        
        testsPassed++;
        System.out.println("✓ Test passed - Simultaneous copy operations work correctly");
        System.out.println();
    }
}
