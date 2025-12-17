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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
     * Pool size is set to 4 to handle typical workloads; could be made configurable via system property if needed.
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
        volatile ScheduledFuture<?> future;  // Volatile for thread-safe updates
        final long periodMs;
        final String callbackName;
        final InterpreterContext context;
        final long createdAt;
        final AtomicLong fireCount;  // Thread-safe counter
        volatile boolean paused;  // Volatile for thread-safe reads
        final String source;  // Source of the timer (screen name or "script")

        TimerInfo(ScheduledFuture<?> future, long periodMs, String callbackName, InterpreterContext context, String source) {
            this.future = future;
            this.periodMs = periodMs;
            this.callbackName = callbackName;
            this.context = context;
            this.createdAt = System.currentTimeMillis();
            this.fireCount = new AtomicLong(0);
            this.paused = false;
            this.source = source;
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
            case "thread.timerpause" -> timerPause(args);
            case "thread.timerresume" -> timerResume(context, args);
            case "thread.timerisrunning" -> timerIsRunning(args);
            case "thread.timerispaused" -> timerIsPaused(args);
            case "thread.timerlist" -> timerList();
            case "thread.timergetinfo" -> timerGetInfo(args);
            case "thread.timergetperiod" -> timerGetPeriod(args);
            case "thread.timergetfirecount" -> timerGetFireCount(args);
            case "thread.getcount" -> getTimerCount();
            default -> throw new InterpreterError("Unknown Thread builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Thread builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("thread.timer") || name.equals("thread.getcount");
    }

    // --- Helper methods ---

    /**
     * Creates a timer task that increments fire count and executes a callback.
     * This helper reduces code duplication between timerStart and timerResume.
     * 
     * @param timerName The name of the timer
     * @param callbackName The name of the callback function (lowercase)
     * @param currentScreen The current screen context (may be null)
     * @param context The interpreter context
     * @return A Runnable that can be scheduled
     */
    private static Runnable createTimerTask(String timerName, String callbackName, String currentScreen, InterpreterContext context) {
        return () -> {
            // Get the timer info to increment fire count (thread-safe)
            TimerInfo info = THREAD_TIMERS.get(timerName);
            if (info != null && !info.paused) {
                info.fireCount.incrementAndGet();
            }
            
            // Execute callback on JavaFX Application Thread for UI safety
            Platform.runLater(() -> {
                try {
                    // Set screen context if we were in a screen
                    if (currentScreen != null) {
                        context.setCurrentScreen(currentScreen);
                    }
                    
                    // Push screen variables to environment if this timer is associated with a screen
                    if (currentScreen != null) {
                        pushScreenVarsToEnvironment(currentScreen, context);
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

                        CallStatement callStmt = new CallStatement(0, callbackName, paramsList);

                        // Execute the call statement using the main interpreter
                        mainInterpreter.visitCallStatement(callStmt);
                    } finally {
                        // Pop screen variables from environment if they were pushed
                        if (currentScreen != null) {
                            popScreenVarsFromEnvironment(currentScreen, context);
                        }
                        if (currentScreen != null) {
                            context.clearCurrentScreen();
                        }
                    }
                } catch (Exception e) {
                    // Log error to output if available
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Error executing timer callback '" + callbackName + "' for timer '" + timerName + "': " + e.getMessage());
                    } else {
                        System.err.println("Error executing timer callback '" + callbackName + "' for timer '" + timerName + "': " + e.getMessage());
                    }
                }
            });
        };
    }
    
    /**
     * Helper method to push screen variables into the environment scope.
     * This allows timer callbacks to access screen variables directly by name.
     * Creates a new scope with screen variables defined in it.
     * 
     * @param screenName The name of the screen whose variables should be pushed
     * @param context The interpreter context
     */
    private static void pushScreenVarsToEnvironment(String screenName, InterpreterContext context) {
        // Get screen variables
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap = context.getScreenVars(screenName);
        if (screenVarMap == null) {
            return;
        }
        
        // Get the main interpreter's environment
        Interpreter mainInterpreter = context.getMainInterpreter();
        if (mainInterpreter == null) {
            return;
        }
        
        // Push a new environment scope
        mainInterpreter.environment().pushEnvironmentValues();
        
        // Add each screen variable to the new scope
        for (Map.Entry<String, Object> entry : screenVarMap.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();
            
            // Convert NULL_SENTINEL back to null for environment
            Object envValue = (value == com.eb.script.interpreter.InterpreterArray.NULL_SENTINEL) ? null : value;
            
            // Define variable in the new scope
            mainInterpreter.environment().getEnvironmentValues().define(varName, envValue);
        }
    }
    
    /**
     * Helper method to pop screen variables from the environment scope.
     * This removes the scope that was added by pushScreenVarsToEnvironment to prevent scope leakage.
     * 
     * @param screenName The name of the screen (not actually used since we just pop the scope)
     * @param context The interpreter context
     */
    private static void popScreenVarsFromEnvironment(String screenName, InterpreterContext context) {
        // Get the main interpreter's environment
        Interpreter mainInterpreter = context.getMainInterpreter();
        if (mainInterpreter == null) {
            return;
        }
        
        // Pop the environment scope that contains the screen variables
        mainInterpreter.environment().popEnvironmentValues();
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

        // Create the repeating task using the helper method
        Runnable timerTask = createTimerTask(timerName, finalCallbackName, currentScreen, context);

        // Schedule the task to run repeatedly
        ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(
            timerTask,
            periodMs,  // initial delay
            periodMs,  // period
            TimeUnit.MILLISECONDS
        );

        // Determine the source: screen name if in a screen context, otherwise "script"
        String source = (currentScreen != null && !currentScreen.isEmpty()) ? currentScreen : "script";

        // Store the timer info
        THREAD_TIMERS.put(timerName, new TimerInfo(future, periodMs, finalCallbackName, context, source));

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
     * thread.timerPause(name) - Pause a running timer
     * 
     * @param args Arguments: name (STRING)
     * @return true if timer was paused, false if timer not found or already paused
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerPause(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerPause requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerPause: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        if (timerInfo == null) {
            return false; // Timer not found
        }

        if (timerInfo.paused) {
            return false; // Already paused
        }

        // Cancel the current scheduled task
        timerInfo.future.cancel(false);
        timerInfo.paused = true;

        return true;
    }

    /**
     * thread.timerResume(name) - Resume a paused timer
     * 
     * @param context The interpreter context for executing callbacks
     * @param args Arguments: name (STRING)
     * @return true if timer was resumed, false if timer not found or not paused
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerResume(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerResume requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerResume: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        if (timerInfo == null) {
            return false; // Timer not found
        }

        if (!timerInfo.paused) {
            return false; // Not paused
        }

        // Recreate the timer task using the helper method
        final String finalCallbackName = timerInfo.callbackName.toLowerCase();
        final String currentScreen = context.getCurrentScreen();
        Runnable timerTask = createTimerTask(timerName, finalCallbackName, currentScreen, context);

        // Reschedule the task
        ScheduledFuture<?> newFuture = SCHEDULER.scheduleAtFixedRate(
            timerTask,
            timerInfo.periodMs,
            timerInfo.periodMs,
            TimeUnit.MILLISECONDS
        );

        // Update the timer info - replace the main future reference
        timerInfo.future = newFuture;
        timerInfo.paused = false;

        return true;
    }

    /**
     * thread.timerIsRunning(name) - Check if a timer is running
     * 
     * @param args Arguments: name (STRING)
     * @return true if timer exists and is running (not paused), false otherwise
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerIsRunning(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerIsRunning requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerIsRunning: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        return timerInfo != null && !timerInfo.paused;
    }

    /**
     * thread.timerIsPaused(name) - Check if a timer is paused
     * 
     * @param args Arguments: name (STRING)
     * @return true if timer exists and is paused, false otherwise
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerIsPaused(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerIsPaused requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerIsPaused: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        return timerInfo != null && timerInfo.paused;
    }

    /**
     * thread.timerList() - Get a list of all active timers
     * 
     * @return JSON string containing array of timer information
     */
    private static Object timerList() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Map.Entry<String, TimerInfo> entry : THREAD_TIMERS.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            TimerInfo info = entry.getValue();
            json.append("{");
            json.append("\"name\":\"").append(entry.getKey()).append("\",");
            json.append("\"period\":").append(info.periodMs).append(",");
            json.append("\"callback\":\"").append(info.callbackName).append("\",");
            json.append("\"source\":\"").append(info.source).append("\",");
            json.append("\"paused\":").append(info.paused).append(",");
            json.append("\"fireCount\":").append(info.fireCount.get()).append(",");
            json.append("\"createdAt\":").append(info.createdAt);
            json.append("}");
        }
        
        json.append("]");
        return json.toString();
    }

    /**
     * thread.timerGetInfo(name) - Get detailed information about a specific timer
     * 
     * @param args Arguments: name (STRING)
     * @return JSON string with timer details, or null if not found
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerGetInfo(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerGetInfo requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerGetInfo: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        if (timerInfo == null) {
            return null;
        }

        StringBuilder json = new StringBuilder("{");
        json.append("\"name\":\"").append(timerName).append("\",");
        json.append("\"period\":").append(timerInfo.periodMs).append(",");
        json.append("\"callback\":\"").append(timerInfo.callbackName).append("\",");
        json.append("\"source\":\"").append(timerInfo.source).append("\",");
        json.append("\"paused\":").append(timerInfo.paused).append(",");
        json.append("\"fireCount\":").append(timerInfo.fireCount.get()).append(",");
        json.append("\"createdAt\":").append(timerInfo.createdAt).append(",");
        json.append("\"uptime\":").append(System.currentTimeMillis() - timerInfo.createdAt);
        json.append("}");
        
        return json.toString();
    }

    /**
     * thread.timerGetPeriod(name) - Get the period of a timer in milliseconds
     * 
     * @param args Arguments: name (STRING)
     * @return Period in milliseconds, or -1 if timer not found
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerGetPeriod(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerGetPeriod requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerGetPeriod: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        return timerInfo != null ? timerInfo.periodMs : -1L;
    }

    /**
     * thread.timerGetFireCount(name) - Get the number of times a timer has fired
     * 
     * @param args Arguments: name (STRING)
     * @return Fire count, or -1 if timer not found
     * @throws InterpreterError if arguments are invalid
     */
    private static Object timerGetFireCount(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("thread.timerGetFireCount requires 1 argument: name");
        }

        String timerName = args[0] != null ? args[0].toString() : null;
        if (timerName == null || timerName.isBlank()) {
            throw new InterpreterError("thread.timerGetFireCount: timer name cannot be null or empty");
        }

        TimerInfo timerInfo = THREAD_TIMERS.get(timerName);
        return timerInfo != null ? timerInfo.fireCount.get() : -1L;
    }

    /**
     * thread.getCount() - Get the count of active timers
     * 
     * @return Count of active timers
     */
    private static Object getTimerCount() {
        return (long) THREAD_TIMERS.size();
    }

    /**
     * Stop all timers associated with a specific source (e.g., a screen name).
     * This is called when a screen is closed to clean up all timers created by that screen.
     * 
     * @param source The source to stop timers for (screen name or "script")
     * @return The number of timers stopped
     */
    public static int stopTimersForSource(String source) {
        if (source == null || source.isEmpty()) {
            return 0;
        }

        int stoppedCount = 0;
        // Iterate and remove timers with matching source
        Iterator<Map.Entry<String, TimerInfo>> iterator = THREAD_TIMERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TimerInfo> entry = iterator.next();
            TimerInfo timerInfo = entry.getValue();
            if (source.equals(timerInfo.source)) {
                // Cancel the scheduled task
                timerInfo.future.cancel(false);
                // Remove from registry
                iterator.remove();
                stoppedCount++;
            }
        }
        
        return stoppedCount;
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
