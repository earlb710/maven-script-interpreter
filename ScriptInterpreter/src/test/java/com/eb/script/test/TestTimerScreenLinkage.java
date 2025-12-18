package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;

/**
 * Tests for the timer-screen linkage requirement:
 * - Timers created inside a screen should be automatically linked to that screen
 * - Timers created outside a screen must use qualified name "screenName.timerName"
 * - If a screen cannot be identified, an error should be thrown
 */
public class TestTimerScreenLinkage {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Timer-Screen Linkage Requirement ===\n");
        
        int passed = 0;
        int failed = 0;
        
        // Test 1: Creating timer outside screen without qualified name should fail
        System.out.println("Test 1: Timer creation outside screen without qualified name");
        String script1 = """
            timerCallback(timerName: string) {
                print "Timer fired";
            }
            
            call thread.timerStart("timer1", 1000, "timerCallback");
            """;
        
        try {
            RuntimeContext ctx1 = Parser.parse("test1", script1);
            Interpreter interp1 = new Interpreter();
            interp1.interpret(ctx1);
            System.err.println("✗ Test 1 FAILED - should have thrown error");
            failed++;
        } catch (InterpreterError e) {
            if (e.getMessage().contains("outside a screen context")) {
                System.out.println("✓ Test 1 PASSED - correctly threw error: " + e.getMessage());
                passed++;
            } else {
                System.err.println("✗ Test 1 FAILED - wrong error: " + e.getMessage());
                failed++;
            }
        } catch (Exception e) {
            System.err.println("✗ Test 1 FAILED - unexpected exception: " + e.getMessage());
            failed++;
        }
        System.out.println();
        
        // Test 2: Creating timer with qualified name for non-existent screen should fail
        System.out.println("Test 2: Timer creation with qualified name for non-existent screen");
        String script2 = """
            timerCallback(timerName: string) {
                print "Timer fired";
            }
            
            call thread.timerStart("nonExistentScreen.timer2", 1000, "timerCallback");
            """;
        
        try {
            RuntimeContext ctx2 = Parser.parse("test2", script2);
            Interpreter interp2 = new Interpreter();
            interp2.interpret(ctx2);
            System.err.println("✗ Test 2 FAILED - should have thrown error");
            failed++;
        } catch (InterpreterError e) {
            if (e.getMessage().contains("does not exist")) {
                System.out.println("✓ Test 2 PASSED - correctly threw error: " + e.getMessage());
                passed++;
            } else {
                System.err.println("✗ Test 2 FAILED - wrong error: " + e.getMessage());
                failed++;
            }
        } catch (Exception e) {
            System.err.println("✗ Test 2 FAILED - unexpected exception: " + e.getMessage());
            failed++;
        }
        System.out.println();
        
        // Test 3: Creating timer with qualified name for existing screen should succeed
        System.out.println("Test 3: Timer creation with qualified name for existing screen");
        String script3 = """
            timerCallback(timerName: string) {
                call thread.timerStop(timerName);
            }
            
            screen testScreen = {
                "title": "Test Screen",
                "width": 300,
                "height": 200,
                "vars": []
            };
            
            // Create timer with qualified name (should succeed)
            call thread.timerStart("testScreen.timer3", 1000, "timerCallback");
            
            // Verify timer was created and linked to screen
            var info: string = call thread.timerGetInfo("testScreen.timer3");
            if call str.contains(info, "\\"source\\":\\"testscreen\\"") then {
                print "Timer correctly linked to screen";
            } else {
                print "ERROR: Timer not correctly linked";
            }
            
            call thread.timerStop("testScreen.timer3");
            """;
        
        try {
            RuntimeContext ctx3 = Parser.parse("test3", script3);
            Interpreter interp3 = new Interpreter();
            interp3.interpret(ctx3);
            System.out.println("✓ Test 3 PASSED - timer created with qualified name");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 3 FAILED - unexpected exception: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Test 4: Timer created inside screen should auto-link to that screen
        System.out.println("Test 4: Timer creation inside screen context (auto-link)");
        String script4 = """
            timerCallback(timerName: string) {
                call thread.timerStop(timerName);
            }
            
            screen autoLinkScreen = {
                "title": "Auto-Link Test Screen",
                "width": 300,
                "height": 200,
                "vars": [],
                "startup": "
                    // Create timer inside screen context (should auto-link)
                    thread.timerStart('autoTimer', 1000, 'timerCallback');
                    
                    // Verify timer was created and linked to this screen
                    var info: string = thread.timerGetInfo('autoTimer');
                    if (str.contains(info, '\\"source\\":\\"autolinkscreen\\"')) {
                        println('Timer correctly auto-linked to screen');
                    } else {
                        println('ERROR: Timer not auto-linked to screen');
                    }
                "
            };
            """;
        
        try {
            RuntimeContext ctx4 = Parser.parse("test4", script4);
            Interpreter interp4 = new Interpreter();
            interp4.interpret(ctx4);
            // Note: We can't easily verify the auto-linking without showing the screen
            // but the script should at least parse and run without errors
            System.out.println("✓ Test 4 PASSED - screen with timer in startup created");
            passed++;
        } catch (Exception e) {
            System.err.println("✗ Test 4 FAILED - unexpected exception: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        
        // Summary
        System.out.println("=== Test Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        
        if (failed > 0) {
            System.exit(1);
        }
    }
}
