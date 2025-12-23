package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;

import com.eb.script.token.Category;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Debugger;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Built-in functions for Debug and Echo operations.
 * Handles all debug.* and echo.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsDebug {

    /**
     * Dispatch a Debug builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "debug.on")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        Debugger debug = env.getDebugger();
        ScriptArea output = env.getOutputArea();

        return switch (name) {
            case "debug.on" -> debugOn(env, debug, output);
            case "debug.off" -> debugOff(env, debug, output);
            case "echo.on" -> echoOn(env, output);
            case "echo.off" -> echoOff(env, output);
            case "debug.traceon" -> traceOn(env, debug, output);
            case "debug.traceoff" -> traceOff(env, debug, output);
            case "debug.file" -> debugFile(env, debug, output, args);
            case "debug.newfile" -> debugNewFile(env, debug, output, args);
            case "debug.log" -> debugLog(debug, args);
            case "debug.assert" -> debugAssert(debug, args);
            case "debug.assertequals" -> debugAssertEquals(debug, args);
            case "debug.lineswritten" -> getLinesWritten(debug);
            case "debug.resetlinecount" -> resetLineCount(debug);
            case "debug.vars" -> debugVars();
            case "debug.stack" -> debugStack();
            case "debug.memusage" -> memUsage(env, debug, args);
            default -> throw new InterpreterError("Unknown Debug builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Debug/Echo builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("debug.") || name.startsWith("echo.");
    }

    // --- Individual builtin implementations ---

    private static Object debugOn(Environment env, Debugger debug, ScriptArea output) {
        if (env.isEchoOn()) {
            sysOutput(output, "Debug ON");
        }
        return debug.setDebugOn();
    }

    private static Object debugOff(Environment env, Debugger debug, ScriptArea output) {
        if (env.isEchoOn()) {
            sysOutput(output, "Debug OFF");
        }
        return debug.setDebugOff();
    }

    private static Object echoOn(Environment env, ScriptArea output) {
        env.setEcho(true);
        sysOutput(output, "Echo ON");
        return null;
    }

    private static Object echoOff(Environment env, ScriptArea output) {
        env.setEcho(false);
        sysOutput(output, "Echo OFF");
        return null;
    }

    private static Object traceOn(Environment env, Debugger debug, ScriptArea output) {
        if (env.isEchoOn()) {
            sysOutput(output, "Debug Trace ON");
        }
        return debug.setDebugTraceOn();
    }

    private static Object traceOff(Environment env, Debugger debug, ScriptArea output) {
        if (env.isEchoOn()) {
            sysOutput(output, "Debug Trace OFF");
        }
        return debug.setDebugTraceOff();
    }

    private static Object debugFile(Environment env, Debugger debug, ScriptArea output, Object[] args) {
        String fileName = (String) args[0];
        debug.setDebugFilePath(fileName);
        if (env.isEchoOn()) {
            sysOutput(output, "Debug file path : " + debug.getDebugFilePath());
        }
        return null;
    }

    private static Object debugNewFile(Environment env, Debugger debug, ScriptArea output, Object[] args) {
        String fileName = (String) args[0];
        debug.setDebugNewFilePath(fileName);
        if (env.isEchoOn()) {
            sysOutput(output, "Debug new file path : " + debug.getDebugFilePath());
        }
        return null;
    }

    private static Object debugLog(Debugger debug, Object[] args) {
        debug.debugWriteLine((String) args[0], (String) args[1]);
        return debug.isDebugOn();
    }

    private static Object debugAssert(Debugger debug, Object[] args) {
        Boolean condition = (Boolean) args[0];
        String message = (String) args[1];
        boolean ok = (condition != null && condition);
        String m = null;
        if (debug.isDebugOn()) {
            if (!ok) {
                m = (message == null ? "Assertion FAILED " : message);
            } else {
                m = "Assertion SUCCESS";
            }
            debug.debugWriteLine("ASSERT", m);
        }
        return ok;
    }

    private static Object debugAssertEquals(Debugger debug, Object[] args) {
        Object expected = args[0];
        Object actual = args[1];
        String message = (String) args[2];

        boolean equal = java.util.Objects.equals(expected, actual);
        if (debug.isDebugOn()) {
            String m = null;
            if (equal) {
                m = "Assertion SUCCESS: "
                        + " | expected=" + String.valueOf(expected)
                        + ", actual=" + String.valueOf(actual);
            } else {
                m = (message == null ? "Assertion FAILED: expected != actual" : message)
                        + " | expected=" + String.valueOf(expected)
                        + ", actual=" + String.valueOf(actual);
            }
            debug.debugWriteLine("ASSERT", m);
        }
        return equal;
    }

    private static Object debugVars() {
        Builtins.VarsSupplier supplier = getVarsSupplier();
        return (supplier != null) ? supplier.get() : java.util.Map.of();
    }

    private static Object debugStack() {
        Builtins.StackSupplier supplier = getStackSupplier();
        return (supplier != null) ? supplier.get() : java.util.List.of();
    }

    private static Object getLinesWritten(Debugger debug) {
        return debug.getLinesWritten();
    }

    private static Object resetLineCount(Debugger debug) {
        debug.resetLinesWritten();
        return null;
    }

    private static Object memUsage(Environment env, Debugger debug, Object[] args) {
        final String unit = (args.length > 0 && args[0] instanceof String u && !u.isBlank()) ? u : "MB";

        final long max = Runtime.getRuntime().maxMemory();
        final long total = Runtime.getRuntime().totalMemory();
        final long free = Runtime.getRuntime().freeMemory();
        final long used = total - free;

        long div;
        switch (unit.toUpperCase()) {
            case "KB" -> div = 1024L;
            case "B" -> div = 1L;
            default -> div = 1024L * 1024L;
        }

        final double maxU = max / (double) div;
        final double totalU = total / (double) div;
        final double freeU = free / (double) div;
        final double usedU = used / (double) div;

        final String msg = String.format(
                "mem: used=%.2f %s, free=%.2f %s, total=%.2f %s, max=%.2f %s",
                usedU, unit, freeU, unit, totalU, unit, maxU, unit
        );

        if (debug != null && debug.isDebugOn()) {
            debug.debugWriteLine("DEBUG", msg);
        } else {
            if (env.isEchoOn()) {
                System.out.println(msg);
            }
        }

        final java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("max", maxU);
        out.put("total", totalU);
        out.put("free", freeU);
        out.put("used", usedU);
        out.put("unit", unit.toUpperCase());
        return out;
    }

    // --- Helper methods ---

    private static void sysOutput(ScriptArea output, String message) {
        if (output != null) {
            output.println(message);
        } else {
            System.out.println(message);
        }
    }

    // Access the suppliers from Builtins class
    private static Builtins.VarsSupplier varsSupplier = null;
    private static Builtins.StackSupplier stackSupplier = null;

    public static void setVarsSupplier(Builtins.VarsSupplier supplier) {
        varsSupplier = supplier;
    }

    public static void setStackSupplier(Builtins.StackSupplier supplier) {
        stackSupplier = supplier;
    }

    private static Builtins.VarsSupplier getVarsSupplier() {
        return varsSupplier;
    }

    private static Builtins.StackSupplier getStackSupplier() {
        return stackSupplier;
    }
}
