package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;
import com.eb.util.Timed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in functions for timing operations.
 * Provides stopwatch-like functionality for measuring execution time.
 * Handles all timer.* builtins.
 * <p>
 * <b>Auto-initialization Behavior:</b> All timer operations automatically initialize
 * timers if they don't exist, preventing null pointer errors. This design choice
 * prioritizes ease of use - operations return sensible defaults (0ms, false, "0.000")
 * rather than throwing exceptions for non-existent timers.
 * </p>
 *
 * @author Earl Bosch
 */
public class BuiltinsTimer {

    /**
     * Registry of active timers by ID.
     * Thread-safe to allow concurrent timer usage.
     */
    private static final Map<String, Timed> TIMERS = new ConcurrentHashMap<>();

    /**
     * Dispatch a Timer builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "timer.start")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "timer.start" -> start(args);
            case "timer.stop" -> stop(args);
            case "timer.reset" -> reset(args);
            case "timer.continue" -> continueTimer(args);
            case "timer.getperiod" -> getPeriod(args);
            case "timer.getperiodstring" -> getPeriodString(args);
            case "timer.getcontinueperiod" -> getContinuePeriod(args);
            case "timer.getcontinueperiodstring" -> getContinuePeriodString(args);
            case "timer.isrunning" -> isRunning(args);
            case "timer.remove" -> remove(args);
            case "timer.clear" -> clear(args);
            default -> throw new InterpreterError("Unknown Timer builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Timer builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("timer.");
    }

    // --- Individual builtin implementations ---

    /**
     * timer.start(timerId) - Start or restart a timer with the given ID
     * Returns the timer ID for convenience
     */
    private static Object start(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.start requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        timer.timerStart();
        
        return timerId;
    }

    /**
     * timer.stop(timerId) - Stop the timer and return elapsed time in milliseconds
     * Auto-initializes timer if it doesn't exist
     */
    private static Object stop(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.stop requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        return timer.timerStop();
    }

    /**
     * timer.reset(timerId) - Reset the timer to current time without starting it
     * Auto-initializes timer if it doesn't exist
     * Returns true on success
     */
    private static Object reset(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.reset requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        timer.timerReset();
        return true;
    }

    /**
     * timer.continue(timerId) - Continue a stopped timer, marking a continuation point
     * Auto-initializes timer if it doesn't exist
     * Returns true on success
     */
    private static Object continueTimer(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.continue requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        timer.timerContinue();
        return true;
    }

    /**
     * timer.getPeriod(timerId) - Get elapsed time in milliseconds since timer start
     * Auto-initializes timer if it doesn't exist (returns 0)
     */
    private static Object getPeriod(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.getPeriod requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        return timer.getTimerPeriod();
    }

    /**
     * timer.getPeriodString(timerId [, decimals]) - Get elapsed time as formatted string
     * Format: "seconds.milliseconds" (e.g., "5.123")
     * Optional decimals parameter (0-3) controls decimal precision. If omitted or null, uses default formatting.
     * Auto-initializes timer if it doesn't exist (returns "0.000")
     */
    private static Object getPeriodString(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.getPeriodString requires 1-2 arguments: timerId [, decimals]");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        
        if (args.length >= 2 && args[1] != null) {
            int decimals = convertToInt(args[1], "decimals");
            return timer.getTimerString_Seconds(decimals);
        } else {
            return timer.getTimerString_Seconds();
        }
    }

    /**
     * timer.getContinuePeriod(timerId) - Get elapsed time since last continuation point
     * Auto-initializes timer if it doesn't exist (returns 0)
     */
    private static Object getContinuePeriod(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.getContinuePeriod requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        return timer.getContinuePeriod();
    }

    /**
     * timer.getContinuePeriodString(timerId [, decimals]) - Get continue period as formatted string
     * Optional decimals parameter (0-3) controls decimal precision. If omitted or null, uses default formatting.
     * Auto-initializes timer if it doesn't exist (returns "0.000")
     */
    private static Object getContinuePeriodString(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.getContinuePeriodString requires 1-2 arguments: timerId [, decimals]");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        
        if (args.length >= 2 && args[1] != null) {
            int decimals = convertToInt(args[1], "decimals");
            return timer.getContinueString_Seconds(decimals);
        } else {
            return timer.getContinueString_Seconds();
        }
    }

    /**
     * timer.isRunning(timerId) - Check if the timer is currently running
     * Auto-initializes timer if it doesn't exist (returns false)
     */
    private static Object isRunning(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.isRunning requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        Timed timer = TIMERS.computeIfAbsent(timerId, k -> new Timed());
        return timer.isRunning();
    }

    /**
     * timer.remove(timerId) - Remove a timer from the registry
     * Returns true if removed, false if not found
     */
    private static Object remove(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("timer.remove requires 1 argument: timerId");
        }
        String timerId = String.valueOf(args[0]);
        
        return TIMERS.remove(timerId) != null;
    }

    /**
     * timer.clear() - Remove all timers from the registry
     * Returns the number of timers that were removed
     * Thread-safe: Uses atomic operation to ensure accurate count
     */
    private static Object clear(Object[] args) throws InterpreterError {
        // Use synchronized block to ensure atomicity of size check and clear
        synchronized (TIMERS) {
            int count = TIMERS.size();
            TIMERS.clear();
            return count;
        }
    }

    /**
     * Helper to convert an argument to int with bounds checking and special value handling
     */
    private static int convertToInt(Object value, String paramName) throws InterpreterError {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            long longValue = (Long) value;
            if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                throw new InterpreterError(paramName + " value out of integer range: " + longValue);
            }
            return (int) longValue;
        } else if (value instanceof Double) {
            double doubleValue = (Double) value;
            // Check for special floating-point values
            if (Double.isNaN(doubleValue)) {
                throw new InterpreterError(paramName + " cannot be NaN");
            }
            if (Double.isInfinite(doubleValue)) {
                throw new InterpreterError(paramName + " cannot be infinite");
            }
            // Check bounds before truncation
            if (doubleValue < Integer.MIN_VALUE || doubleValue > Integer.MAX_VALUE) {
                throw new InterpreterError(paramName + " value out of integer range: " + doubleValue);
            }
            // Round to nearest integer instead of truncating
            return (int) Math.round(doubleValue);
        } else if (value instanceof Float) {
            float floatValue = (Float) value;
            // Check for special floating-point values
            if (Float.isNaN(floatValue)) {
                throw new InterpreterError(paramName + " cannot be NaN");
            }
            if (Float.isInfinite(floatValue)) {
                throw new InterpreterError(paramName + " cannot be infinite");
            }
            // Check bounds before rounding
            if (floatValue < Integer.MIN_VALUE || floatValue > Integer.MAX_VALUE) {
                throw new InterpreterError(paramName + " value out of integer range: " + floatValue);
            }
            // Round to nearest integer
            return Math.round(floatValue);
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new InterpreterError("Invalid integer value for " + paramName + ": " + value);
            }
        }
        throw new InterpreterError("Cannot convert " + paramName + " to integer: " + value);
    }
}
