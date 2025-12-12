package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.arrays.ArrayFixedInt;
import com.eb.script.token.DataType;
import com.eb.ui.ebs.EbsApp;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;

/**
 * Built-in functions for System operations.
 * Handles system.*, sleep, and array.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsSystem {
    
    /** Timeout in seconds for reloadConfig to wait for FX thread completion */
    private static final int RELOAD_CONFIG_TIMEOUT_SECONDS = 5;

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
            case "system.getebsver" -> getEBSver();
            case "system.testebsver" -> testEBSver(args);
            case "system.reloadconfig" -> reloadConfig();
            case "thread.sleep" -> sleep(args);
            case "array.expand" -> arrayExpand(args);
            case "array.sort" -> arraySort(args);
            case "array.fill" -> arrayFill(args);
            case "array.add" -> arrayAdd(args);
            case "array.remove" -> arrayRemove(args);
            case "array.base64encode" -> base64Encode(args);
            case "array.base64decode" -> base64Decode(args);
            case "array.asbitmap" -> arrayAsBitmap(args);
            case "array.asbyte" -> arrayAsByte(args);
            case "array.asintmap" -> arrayAsIntmap(args);
            case "array.asint" -> arrayAsInt(args);
            default -> throw new InterpreterError("Unknown System builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a System/Array/Sleep builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("system.") || name.startsWith("array.") || name.equals("thread.sleep");
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

    /**
     * EBS Version Components - Four-part versioning system.
     * Format: "language.keyword.builtin.build"
     * 
     * - LANGUAGE_VER: Major language changes that break compatibility with previous versions
     * - KEYWORD_VER: Incremented when keywords are added/modified/removed
     * - BUILTIN_VER: Incremented when builtin functions are added/modified/removed
     * - BUILD_VER: Build number, incremented with each release/build
     * 
     * Each component is incremented independently based on the type of change.
     */
    public static final int LANGUAGE_VER = 1;  // Major language compatibility version
    public static final int KEYWORD_VER = 1;   // Keyword version - Added IMAGE data type
    public static final int BUILTIN_VER = 6;   // Builtin version - Added EbsImage with JavaFX image support
    public static final int BUILD_VER = 1;     // Build number
    
    public static final String EBS_LANGUAGE_VERSION = LANGUAGE_VER + "." + KEYWORD_VER + "." + BUILTIN_VER + "." + BUILD_VER;

    private static Object getEBSver() {
        return EBS_LANGUAGE_VERSION;
    }

    /**
     * Compares a supplied version with the running EBS language version.
     * Returns true if the running version is greater than or equal to the supplied version.
     * Version format: "language.keyword.builtin.build" where each part is compared independently.
     *
     * @param args args[0] = version string to compare (e.g., "1.0.2")
     * @return true if running version >= supplied version, false otherwise
     */
    private static Object testEBSver(Object[] args) throws InterpreterError {
        String testVersion = (String) args[0];
        if (testVersion == null || testVersion.isBlank()) {
            throw new InterpreterError("system.testEBSver: version string cannot be null or empty");
        }
        return compareVersions(EBS_LANGUAGE_VERSION, testVersion) >= 0;
    }

    /**
     * Compares two version strings in "language.keyword.builtin" format.
     * @param v1 first version string (e.g., "1.0.2")
     * @param v2 second version string (e.g., "1.0.1")
     * @return negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = (i < parts1.length) ? parseVersionPart(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? parseVersionPart(parts2[i]) : 0;
            
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    /**
     * Parses a version part, handling non-numeric suffixes (e.g., "1-beta" -> 1).
     */
    private static int parseVersionPart(String part) {
        // Extract leading numeric portion
        StringBuilder numStr = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                numStr.append(c);
            } else {
                break;
            }
        }
        if (numStr.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(numStr.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
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
    
    /**
     * Reload console configuration from console.cfg and reapply CSS styles.
     * This allows users to apply config changes without restarting the application.
     * 
     * @return true if config was successfully reloaded, false otherwise
     */
    private static Object reloadConfig() throws InterpreterError {
        // If already on FX thread, execute directly
        if (Platform.isFxApplicationThread()) {
            return EbsApp.reloadConfig();
        }
        
        // Otherwise, run on FX thread and wait for result
        final AtomicBoolean result = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                result.set(EbsApp.reloadConfig());
            } finally {
                latch.countDown();
            }
        });
        
        try {
            // Wait for the reload to complete (with timeout)
            if (!latch.await(RELOAD_CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new InterpreterError("system.reloadConfig: timeout waiting for config reload");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("system.reloadConfig interrupted: " + e.getMessage());
        }
        
        return result.get();
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

    @SuppressWarnings("unchecked")
    private static Object arrayAdd(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("array.add: array cannot be null");
        }
        if (!(args[0] instanceof ArrayDef)) {
            throw new InterpreterError("array.add: first argument must be an array");
        }
        ArrayDef<Object, ?> array = (ArrayDef<Object, ?>) args[0];
        Object value = args[1];
        
        // Check if optional index parameter is provided
        if (args.length > 2 && args[2] != null) {
            if (!(args[2] instanceof Number)) {
                throw new InterpreterError("array.add: index must be a number");
            }
            int index = ((Number) args[2]).intValue();
            if (index < 0 || index > array.size()) {
                throw new InterpreterError("array.add: index " + index + " out of bounds (size: " + array.size() + ")");
            }
            array.add(index, value);
        } else {
            // Add to end of array
            array.add(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object arrayRemove(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("array.remove: array cannot be null");
        }
        if (!(args[0] instanceof ArrayDef)) {
            throw new InterpreterError("array.remove: first argument must be an array");
        }
        if (args[1] == null) {
            throw new InterpreterError("array.remove: index cannot be null");
        }
        if (!(args[1] instanceof Number)) {
            throw new InterpreterError("array.remove: index must be a number");
        }
        ArrayDef<Object, ?> array = (ArrayDef<Object, ?>) args[0];
        int index = ((Number) args[1]).intValue();
        
        if (index < 0 || index >= array.size()) {
            throw new InterpreterError("array.remove: index " + index + " out of bounds (size: " + array.size() + ")");
        }
        
        // Get the value before removing
        Object removedValue = array.get(index);
        array.remove(index);
        return removedValue;
    }

    /**
     * Encodes byte array to base64 string.
     * Uses BuiltinsCrypto.encodeBase64() for base64 encoding.
     */
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

        return BuiltinsCrypto.encodeBase64(content);
    }

    /**
     * Decodes base64 string to byte array.
     * Uses BuiltinsCrypto.decodeBase64() for base64 decoding.
     */
    private static Object base64Decode(Object[] args) throws InterpreterError {
        String b64 = (String) args[0];
        if (b64 == null) {
            return null;
        }
        try {
            byte[] bytes = BuiltinsCrypto.decodeBase64(b64);
            return new ArrayFixedByte(bytes);
        } catch (IllegalArgumentException ex) {
            throw new InterpreterError("array.base64decode: invalid base64: " + ex.getMessage());
        }
    }

    /**
     * Cast an array.byte to array.bitmap.
     * The underlying data remains the same, only the data type changes.
     * @param args The arguments (array to cast)
     * @return A new ArrayFixedByte with BITMAP data type
     */
    private static Object arrayAsBitmap(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        if (a0 == null) {
            return null;
        }
        if (a0 instanceof ArrayFixedByte afb) {
            // Use the castTo method to create a copy with BITMAP data type
            return afb.castTo(DataType.BITMAP);
        }
        throw new InterpreterError("array.asBitmap: expected byte array (array.byte), got " + a0.getClass().getSimpleName());
    }

    /**
     * Cast an array.bitmap to array.byte.
     * The underlying data remains the same, only the data type changes.
     * @param args The arguments (array to cast)
     * @return A new ArrayFixedByte with BYTE data type
     */
    private static Object arrayAsByte(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        if (a0 == null) {
            return null;
        }
        if (a0 instanceof ArrayFixedByte afb) {
            // Use the castTo method to create a copy with BYTE data type
            return afb.castTo(DataType.BYTE);
        }
        throw new InterpreterError("array.asByte: expected bitmap array (array.bitmap), got " + a0.getClass().getSimpleName());
    }

    /**
     * Cast an array.int to array.intmap.
     * The underlying data remains the same, only the data type changes.
     * @param args The arguments (array to cast)
     * @return A new ArrayFixedInt with INTMAP data type
     */
    private static Object arrayAsIntmap(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        if (a0 == null) {
            return null;
        }
        if (a0 instanceof ArrayFixedInt afi) {
            // Use the castTo method to create a copy with INTMAP data type
            return afi.castTo(DataType.INTMAP);
        }
        throw new InterpreterError("array.asIntmap: expected int array (array.int), got " + a0.getClass().getSimpleName());
    }

    /**
     * Cast an array.intmap to array.int.
     * The underlying data remains the same, only the data type changes.
     * @param args The arguments (array to cast)
     * @return A new ArrayFixedInt with INTEGER data type
     */
    private static Object arrayAsInt(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        if (a0 == null) {
            return null;
        }
        if (a0 instanceof ArrayFixedInt afi) {
            // Use the castTo method to create a copy with INTEGER data type
            return afi.castTo(DataType.INTEGER);
        }
        throw new InterpreterError("array.asInt: expected intmap array (array.intmap), got " + a0.getClass().getSimpleName());
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
