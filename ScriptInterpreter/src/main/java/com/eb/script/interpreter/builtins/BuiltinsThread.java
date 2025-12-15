package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.statement.CallStatement;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.interpreter.expression.LiteralExpression;
import com.eb.script.token.DataType;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Built-in functions for thread-based timer operations.
 * Provides periodic callback functionality using scheduled executors.
 * Handles thread.timerStart and thread.timerStop builtins.
 * <p>
 * These timers differ from timer.* builtins in that they:
 * - Execute callbacks repeatedly at fixed intervals
 * - Run asynchronously in the background
 * - Continue until explicitly stopped
 * </p>
 *
 * @author Earl Bosch
 */
public class BuiltinsThread {

    /**
     * Single shared executor for all periodic timers.
     * Using a scheduled thread pool allows multiple timers to run concurrently.
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4);

    /**
     * Registry of active thread timers by name.
     * Thread-safe to allow concurrent timer management.
     * Values are ScheduledFuture objects that can be cancelled.
     */
    private static final Map<String, TimerInfo> THREAD_TIMERS = new ConcurrentHashMap<>();

    /**
     * Information about a running thread timer.
     */
    private static class TimerInfo {
        final ScheduledFuture<?> future;
        final long periodMs;
        final String callbackName;
        final InterpreterContext context;

        TimerInfo(ScheduledFuture<?> future, long periodMs, String callbackName, InterpreterContext context) {
            this.future = future;
            this.periodMs = periodMs;
            this.callbackName = callbackName;
            this.context = context;
        }
    }

    /**
     * Dispatch a Thread builtin by name.
     *
     * @param context The interpreter context (needed for callbacks)
     * @param name Lowercase builtin name (e.g., "thread.timerstart")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(InterpreterContext context, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "thread.timerstart" -> timerStart(context, args);
            case "thread.timerstop" -> timerStop(args);
            default -> throw new InterpreterError("Unknown Thread builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Thread builtin.
     */
    public static boolean handles(String name) {
        return name.equals("thread.timerstart") || name.equals("thread.timerstop");
    }

    // --- Individual builtin implementations ---

    /**
     * thread.timerStart(name, period, callback) - Start a repeating timer
     * 
     * @param context The interpreter context for executing callbacks
     * @param args Arguments: name (STRING), period (LONG milliseconds), callback (STRING function name)
     * @return The timer name for convenience
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerStart(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("thread.timerStart requires 3 arguments: name, period, callback");
        }

        // Extract arguments
        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerStart: timer name cannot be null or empty");
        }

        // Period in milliseconds
        long periodMs;
        if (args[1] instanceof Number) {
            periodMs = ((Number) args[1]).longValue();
        } else {
            throw new InterpreterError("thread.timerStart: period must be a number (milliseconds)");
        }
        if (periodMs <= 0) {
            throw new InterpreterError("thread.timerStart: period must be positive (got " + periodMs + ")");
        }

        // Callback function name
        String callbackName = args[2] != null ? args[2].toString() : null;
        if (callbackName == null || callbackName.isBlank()) {
            throw new InterpreterError("thread.timerStart: callback function name cannot be null or empty");
        }

        // Lowercase the callback name to match how the lexer stores identifiers
        final String finalCallbackName = callbackName.toLowerCase();

        // Get the current screen context for the callback (if in a screen)
        final String currentScreen = context.getCurrentScreen();

        // Stop existing timer with the same name if it exists
        TimerInfo existingTimer = THREAD_TIMERS.get(timerName);
        if (existingTimer != null) {
            existingTimer.future.cancel(false);
            THREAD_TIMERS.remove(timerName);
        }

        // Create the repeating task
        Runnable timerTask = () -> {
            // Execute callback on JavaFX Application Thread for UI safety
            Platform.runLater(() -> {
                try {
                    // Set screen context if we were in a screen
                    if (currentScreen != null) {
                        context.setCurrentScreen(currentScreen);
                    }

                    try {
                        // Get the main interpreter that has access to the script's functions
                        Interpreter mainInterpreter = context.getMainInterpreter();
                        if (mainInterpreter == null) {
                            throw new InterpreterError("No main interpreter available for callback execution");
                        }

                        // Create a CallStatement to invoke the callback function
                        // The callback receives the timer name as a parameter
                        List<Parameter> paramsList = new ArrayList<>();
                        paramsList.add(new Parameter("timerName", DataType.STRING, 
                            new LiteralExpression(DataType.STRING, timerName)));

                        CallStatement callStmt = new CallStatement(0, finalCallbackName, paramsList);

                        // Execute the call statement using the main interpreter
                        mainInterpreter.visitCallStatement(callStmt);
                    } finally {
                        if (currentScreen != null) {
                            context.clearCurrentScreen();
                        }
                    }
                } catch (Exception e) {
                    // Log error to output if available
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Error executing timer callback '" + finalCallbackName + "': " + e.getMessage());
                    } else {
                        System.err.println("Error executing timer callback '" + finalCallbackName + "': " + e.getMessage());
                    }
                }
            });
        };

        // Schedule the task to run repeatedly
        ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(
            timerTask,
            periodMs,  // initial delay
            periodMs,  // period
            TimeUnit.MILLISECONDS
        );

        // Store the timer info
        THREAD_TIMERS.put(timerName, new TimerInfo(future, periodMs, finalCallbackName, context));

        return timerName;
    }

    /**
     * thread.timerStop(name) - Stop a repeating timer
     * 
     * @param args Arguments: name (STRING)
     * @return true if timer was stopped, false if timer not found
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerStop(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerStop requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerStop: timer name cannot be null or empty");
        }

        // Get the timer info
        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        if (timerInfo == null) {
            return false; // Timer not found
        }

        // Cancel the scheduled task (don't interrupt if already running)
        timerInfo.future.cancel(false);

        // Remove from registry
        THREAD_TIMERS.remove(timerName);

        return true;
    }

    /**
     * Shutdown all thread timers and the executor service.
     * This should be called when the application exits.
     */
    public static void shutdown() {
        // Cancel all running timers
        for (TimerInfo timer : THREAD_TIMERS.values()) {
            timer.future.cancel(false);
        }
        THREAD_TIMERS.clear();

        // Shutdown the scheduler
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
