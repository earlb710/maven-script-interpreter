package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.token.DataType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Built-in functions for System operations.
 * Handles system.*, sleep, and array.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsSystem {

    /**
     * Dispatch a System builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "system.command")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "system.command" -> command(args);
            case "system.wincommand" -> winCommand(args);
            case "system.getproperty" -> getProperty(args);
            case "system.setproperty" -> setProperty(args);
            case "sleep" -> sleep(args);
            case "array.expand" -> arrayExpand(args);
            case "array.sort" -> arraySort(args);
            case "array.fill" -> arrayFill(args);
            case "array.base64encode" -> base64Encode(args);
            case "array.base64decode" -> base64Decode(args);
            default -> throw new InterpreterError("Unknown System builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a System/Array/Sleep builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("system.") || name.startsWith("array.") || name.equals("sleep");
    }

    // --- Individual builtin implementations ---

    private static Object command(Object[] args) {
        final ArrayDef cmdArgs = (args[1] instanceof ArrayDef) ? (ArrayDef) args[1] : null;
        final Long timeoutMs = (args.length > 2 && args[2] instanceof Long) ? (Long) args[2] : 60000L;
        final String cwd = (args.length > 3 && args[3] instanceof String) ? (String) args[3] : null;
        return runCmd((String) args[0], cmdArgs, timeoutMs, cwd);
    }

    private static Object winCommand(Object[] args) {
        final String osCms = (String) args[0];
        final ArrayDef cmdArgs = (args[1] instanceof ArrayDef) ? (ArrayDef) args[1] : null;
        ArrayDef newCmds = new ArrayDynamic(DataType.STRING);
        newCmds.add("/C");
        newCmds.add(osCms);
        newCmds.addAll(cmdArgs);
        final Long timeoutMs = (args.length > 2 && args[2] instanceof Long) ? (Long) args[2] : 60000L;
        final String cwd = (args.length > 3 && args[3] instanceof String) ? (String) args[3] : null;
        return runCmd("cmd", newCmds, timeoutMs, cwd);
    }

    private static Object getProperty(Object[] args) {
        final String key = (String) args[0];
        final String def = (args.length > 1 && args[1] instanceof String)
                ? (String) args[1] : null;
        return (def == null)
                ? java.lang.System.getProperty(key)
                : java.lang.System.getProperty(key, def);
    }

    private static Object setProperty(Object[] args) {
        final String key = (String) args[0];
        final String val = (args.length > 1 && args[1] instanceof String)
                ? (String) args[1] : null;
        return (val == null)
                ? java.lang.System.clearProperty(key)
                : java.lang.System.setProperty(key, val);
    }

    private static Object sleep(Object[] args) throws InterpreterError {
        final Number millisNum = (Number) args[0];
        final long millis = (millisNum != null) ? millisNum.longValue() : 0L;
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterpreterError("sleep interrupted: " + e.getMessage());
            }
        }
        return "";
    }

    private static Object arrayExpand(Object[] args) {
        if (args[0] instanceof ArrayDef array) {
            int len = (Integer) args[1];
            array.expandArray(len);
        }
        return null;
    }

    private static Object arraySort(Object[] args) {
        if (args[0] instanceof ArrayDef array) {
            boolean ascen = true;
            if (args[1] instanceof Boolean val) {
                ascen = val;
            }
            array.sortArray(ascen);
        }
        return null;
    }

    private static Object arrayFill(Object[] args) {
        if (args[0] instanceof ArrayDef array) {
            int len = (Integer) args[1];
            array.fillArray(len, args[2]);
        }
        return null;
    }

    private static Object base64Encode(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        if (a0 == null) {
            return null;
        }
        byte[] content;

        if (a0 instanceof ArrayFixedByte afb) {
            content = afb.elements;
        } else if (a0 instanceof byte[] ba) {
            content = ba;
        } else if (a0 instanceof ArrayDef ad) {
            int n = ad.size();
            content = new byte[n];
            for (int i = 0; i < n; i++) {
                Object el = ad.get(i);
                if (el == null) {
                    content[i] = 0;
                } else if (el instanceof Number num) {
                    int v = num.intValue();
                    if (v < 0 || v > 255) {
                        throw new InterpreterError("array.base64encode: element out of byte range: " + v);
                    }
                    content[i] = (byte) (v & 0xFF);
                } else {
                    throw new InterpreterError("array.base64encode: expected byte values (0..255) in array");
                }
            }
        } else {
            throw new InterpreterError("array.base64encode: expected byte array");
        }

        return java.util.Base64.getEncoder().encodeToString(content);
    }

    private static Object base64Decode(Object[] args) throws InterpreterError {
        String b64 = (String) args[0];
        if (b64 == null) {
            return null;
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(b64);
            return new ArrayFixedByte(bytes);
        } catch (IllegalArgumentException ex) {
            throw new InterpreterError("array.base64decode: invalid base64: " + ex.getMessage());
        }
    }

    // --- Helper methods ---

    private static Map<String, Object> runCmd(String command, ArrayDef args, Long timeoutMs, String cwd) {
        final List<String> cmd = new ArrayList<>();
        cmd.add(command);
        if (args != null && args.size() > 0) {
            for (Object o : args) {
                cmd.add(String.valueOf(o));
            }
        }

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null && !cwd.isBlank()) {
            pb.directory(new java.io.File(cwd));
        }
        pb.redirectErrorStream(false);

        try {
            final Process p = pb.start();

            final CompletableFuture<byte[]> outF = CompletableFuture.supplyAsync(() -> {
                try {
                    return p.getInputStream().readAllBytes();
                } catch (Exception e) {
                    return new byte[0];
                }
            });

            final CompletableFuture<byte[]> errF = CompletableFuture.supplyAsync(() -> {
                try {
                    return p.getErrorStream().readAllBytes();
                } catch (Exception e) {
                    return new byte[0];
                }
            });

            final boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
            }

            byte[] outBytes = outF.get(timeoutMs, TimeUnit.MILLISECONDS);
            byte[] errBytes = errF.get(timeoutMs, TimeUnit.MILLISECONDS);

            final int exit = finished ? p.exitValue() : -1;

            final String stdout = new String(outBytes, StandardCharsets.UTF_8);
            final String stderr = new String(errBytes, StandardCharsets.UTF_8);

            final Map<String, Object> result = new LinkedHashMap<>();
            result.put("exitCode", exit);
            result.put("stdout", stdout);
            result.put("stderr", stderr);
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("system.command failed: " + ex.getMessage(), ex);
        }
    }
}
