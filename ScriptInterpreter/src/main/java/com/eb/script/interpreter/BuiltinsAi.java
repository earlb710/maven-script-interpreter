package com.eb.script.interpreter;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.token.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in functions for AI operations.
 * Handles all ai.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsAi {

    /**
     * Dispatch an AI builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "ai.complete")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "ai.complete" -> complete(args);
            case "ai.summarize" -> summarize(args);
            case "ai.embed" -> embed(args);
            case "ai.classify" -> classify(args);
            default -> throw new InterpreterError("Unknown AI builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is an AI builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("ai.");
    }

    // --- Individual builtin implementations ---

    private static Object complete(Object[] args) throws InterpreterError {
        String system = args.length > 0 && args[0] != null ? args[0].toString() : null;
        String user = args.length > 1 && args[1] != null ? args[1].toString() : "";
        Integer maxT = args.length > 2 && args[2] != null ? ((Number) args[2]).intValue() : null;
        Double temp = args.length > 3 && args[3] != null ? ((Number) args[3]).doubleValue() : null;
        try {
            return AiFunctions.chatComplete(system, user, maxT, temp);
        } catch (Exception e) {
            throw new InterpreterError("ai.complete failed: " + e.getMessage());
        }
    }

    private static Object summarize(Object[] args) throws InterpreterError {
        String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
        Integer maxT = args.length > 1 && args[1] != null ? ((Number) args[1]).intValue() : null;
        try {
            return AiFunctions.summarize(text, maxT);
        } catch (Exception e) {
            throw new InterpreterError("ai.summarize failed: " + e.getMessage());
        }
    }

    private static Object embed(Object[] args) throws InterpreterError {
        String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
        try {
            double[] vec = AiFunctions.embed(text);
            ArrayDef out = new ArrayDynamic(DataType.DOUBLE);
            for (double v : vec) {
                out.add(v);
            }
            return out;
        } catch (Exception e) {
            throw new InterpreterError("ai.embed failed: " + e.getMessage());
        }
    }

    private static Object classify(Object[] args) throws InterpreterError {
        String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
        Object lab = args.length > 1 ? args[1] : null;
        List<String> labels = new ArrayList<>();
        if (lab instanceof List<?> L) {
            for (Object o : L) {
                if (o != null) {
                    labels.add(o.toString());
                }
            }
        } else if (lab != null) {
            labels.add(lab.toString());
        }
        try {
            return AiFunctions.classify(text, labels);
        } catch (Exception e) {
            throw new InterpreterError("ai.classify failed: " + e.getMessage());
        }
    }
}
