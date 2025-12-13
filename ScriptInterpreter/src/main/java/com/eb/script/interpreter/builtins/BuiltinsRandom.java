package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;

import java.util.Random;

/**
 * Built-in functions for Random number generation.
 * Handles all random.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsRandom {

    /**
     * Single Random instance shared across all random.* calls.
     * Can be seeded with random.setSeed(seed).
     */
    private static Random random = new Random();

    /**
     * Dispatch a Random builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "random.nextlong")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "random.nextlong" -> nextLong(args);
            case "random.nextdouble" -> nextDouble(args);
            case "random.setseed" -> setSeed(args);
            default -> throw new InterpreterError("Unknown Random builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Random builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("random.");
    }

    // --- Individual builtin implementations ---

    /**
     * random.nextLong() - Returns a random long value
     * random.nextLong(max) - Returns a random long between 0 (inclusive) and max (exclusive)
     * random.nextLong(min, max) - Returns a random long between min (inclusive) and max (exclusive)
     */
    private static Object nextLong(Object[] args) throws InterpreterError {
        if (args.length == 0) {
            // No arguments: return any long value
            return random.nextLong();
        } else if (args.length == 1) {
            // One argument: range from 0 to max (exclusive)
            long max = requireLong(args[0], "random.nextLong", "max");
            if (max <= 0) {
                throw new InterpreterError("random.nextLong: max must be positive, got: " + max);
            }
            return random.nextLong(max);
        } else if (args.length >= 2) {
            // Two arguments: range from min (inclusive) to max (exclusive)
            long min = requireLong(args[0], "random.nextLong", "min");
            long max = requireLong(args[1], "random.nextLong", "max");
            if (min >= max) {
                throw new InterpreterError("random.nextLong: min must be less than max, got min=" + min + ", max=" + max);
            }
            // Generate random long in range [min, max)
            long range = max - min;
            return min + random.nextLong(range);
        }
        throw new InterpreterError("random.nextLong: unexpected number of arguments");
    }

    /**
     * random.nextDouble() - Returns a random double between 0.0 (inclusive) and 1.0 (exclusive)
     * random.nextDouble(max) - Returns a random double between 0.0 (inclusive) and max (exclusive)
     * random.nextDouble(min, max) - Returns a random double between min (inclusive) and max (exclusive)
     */
    private static Object nextDouble(Object[] args) throws InterpreterError {
        if (args.length == 0) {
            // No arguments: return [0.0, 1.0)
            return random.nextDouble();
        } else if (args.length == 1) {
            // One argument: range from 0.0 to max (exclusive)
            double max = requireDouble(args[0], "random.nextDouble", "max");
            if (max <= 0.0) {
                throw new InterpreterError("random.nextDouble: max must be positive, got: " + max);
            }
            return random.nextDouble() * max;
        } else if (args.length >= 2) {
            // Two arguments: range from min (inclusive) to max (exclusive)
            double min = requireDouble(args[0], "random.nextDouble", "min");
            double max = requireDouble(args[1], "random.nextDouble", "max");
            if (min >= max) {
                throw new InterpreterError("random.nextDouble: min must be less than max, got min=" + min + ", max=" + max);
            }
            // Generate random double in range [min, max)
            return min + (random.nextDouble() * (max - min));
        }
        throw new InterpreterError("random.nextDouble: unexpected number of arguments");
    }

    /**
     * random.setSeed(seed) - Sets the seed for the random number generator
     * This allows for reproducible sequences of random numbers.
     */
    private static Object setSeed(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("random.setSeed requires 1 argument: seed");
        }
        long seed = requireLong(args[0], "random.setSeed", "seed");
        random.setSeed(seed);
        return null; // setSeed returns void/null
    }

    // --- Helper methods ---

    /**
     * Extract a long argument with type validation.
     */
    private static long requireLong(Object arg, String functionName, String paramName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(functionName + ": " + paramName + " cannot be null");
        }
        if (!(arg instanceof Number)) {
            throw new InterpreterError(functionName + ": " + paramName + " must be a number, got: " + arg.getClass().getSimpleName());
        }
        return ((Number) arg).longValue();
    }

    /**
     * Extract a double argument with type validation.
     */
    private static double requireDouble(Object arg, String functionName, String paramName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(functionName + ": " + paramName + " cannot be null");
        }
        if (!(arg instanceof Number)) {
            throw new InterpreterError(functionName + ": " + paramName + " must be a number, got: " + arg.getClass().getSimpleName());
        }
        return ((Number) arg).doubleValue();
    }
}
