package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.statement.CallStatement;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.interpreter.expression.LiteralExpression;
import com.eb.script.token.DataType;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * @param context The interpreter context (needed for async callbacks)
     * @param name Lowercase builtin name (e.g., "ai.complete")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(InterpreterContext context, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "ai.complete" -> complete(args);
            case "ai.completeasync" -> completeAsync(context, args);
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

    /**
     * Async version of ai.complete that runs in a background thread and invokes
     * a callback function with the result.
     * 
     * The callback function receives a JSON object with the following structure:
     * - success: boolean indicating if the call succeeded
     * - result: the AI response text (if successful)
     * - error: the error message (if failed)
     * 
     * @param context The interpreter context for executing the callback
     * @param args Arguments: system, user, maxTokens, temperature, callbackName
     * @return null immediately (result is passed to callback)
     */
    private static Object completeAsync(InterpreterContext context, Object[] args) throws InterpreterError {
        String system = args.length > 0 && args[0] != null ? args[0].toString() : null;
        String user = args.length > 1 && args[1] != null ? args[1].toString() : "";
        Integer maxT = args.length > 2 && args[2] != null ? ((Number) args[2]).intValue() : null;
        Double temp = args.length > 3 && args[3] != null ? ((Number) args[3]).doubleValue() : null;
        String callbackName = args.length > 4 && args[4] != null ? args[4].toString() : null;
        
        if (callbackName == null || callbackName.isBlank()) {
            throw new InterpreterError("ai.completeAsync requires a callback function name");
        }
        
        // Lowercase the callback name to match how the lexer stores identifiers
        final String finalCallbackName = callbackName.toLowerCase();
        
        // Get the current screen context for the callback (if in a screen)
        String currentScreen = context.getCurrentScreen();
        
        // Start background thread for AI call
        Thread aiThread = new Thread(() -> {
            Map<String, Object> callbackData = new LinkedHashMap<>();
            try {
                // Execute the AI call
                String result = AiFunctions.chatComplete(system, user, maxT, temp);
                callbackData.put("success", true);
                callbackData.put("result", result);
                callbackData.put("error", null);
            } catch (Exception e) {
                callbackData.put("success", false);
                callbackData.put("result", null);
                callbackData.put("error", e.getMessage());
            }
            
            // Invoke callback on JavaFX Application Thread for UI safety
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
                        
                        // Create a CallStatement directly like screen callbacks do
                        // This properly resolves the function through the interpreter's currentRuntime.blocks
                        List<Parameter> paramsList = new ArrayList<>();
                        paramsList.add(new Parameter("response", DataType.JSON, 
                            new LiteralExpression(DataType.JSON, callbackData)));
                        
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
                        context.getOutput().printlnError("Error executing AI callback: " + e.getMessage());
                    } else {
                        System.err.println("Error executing AI callback: " + e.getMessage());
                    }
                }
            });
        }, "AI-CompleteAsync");
        
        aiThread.setDaemon(true);
        aiThread.start();
        
        // Return immediately - result will be passed to callback
        return null;
    }
}
