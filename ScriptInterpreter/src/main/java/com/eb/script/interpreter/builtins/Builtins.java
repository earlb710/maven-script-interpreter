package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.json.Json;
import com.eb.script.token.DataType;
import com.eb.util.Util;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.interpreter.statement.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Built-in function registry: names, parameter definitions, and return types.
 * Return types are expressed as DataType.
 *
 * Used by the Parser to validate builtin calls and (for expressions) to assign
 * the CallExpression return type (convert DataType -> Token.Type if needed).
 *
 * @author Earl Bosch
 */
public final class Builtins {

    /**
     * Immutable registry of builtin name -> info (params + return DataType).
     */
    private static final Map<String, BuiltinInfo> BUILTINS = new TreeMap();

    /**
     * Unmodifiable set of builtin names.
     */
    public static final Set<String> NAMES;

// Optional suppliers so the interpreter can expose vars/stack to builtins
    @FunctionalInterface
    public interface VarsSupplier {

        Map<String, Object> get();
    }

    @FunctionalInterface
    public interface StackSupplier {

        List<Object> get();
    }

    private static volatile VarsSupplier VARS_SUPPLIER = null;
    private static volatile StackSupplier STACK_SUPPLIER = null;

    /**
     * Get builtin info (params + return DataType). Returns null if unknown.
     */
    public static BuiltinInfo getBuiltinInfo(String name) {
        return BUILTINS.get(name);
    }

    /**
     * Get builtin names
     */
    public static Set<String> getBuiltins() {
        return BUILTINS.keySet();
    }

    /**
     * Convenience: just the parameter signature (defensive copy).
     */
    public static Parameter[] getSignature(String name) {
        BuiltinInfo info = BUILTINS.get(name);
        return info.params;
    }

    /**
     * Allow the interpreter to inject a "variables snapshot" supplier.
     */
    public static void setVarsSupplier(VarsSupplier supplier) {
        VARS_SUPPLIER = supplier;
        // Also update BuiltinsDebug to keep in sync
        BuiltinsDebug.setVarsSupplier(supplier);
    }

    /**
     * Allow the interpreter to inject a "stack frames" supplier.
     */
    public static void setStackSupplier(StackSupplier supplier) {
        STACK_SUPPLIER = supplier;
        // Also update BuiltinsDebug to keep in sync
        BuiltinsDebug.setStackSupplier(supplier);
    }

    /**
     * Parameterized builtin info (definition + return data type).
     */
    public static final class BuiltinInfo {

        public final String name;
        public final Parameter[] params;
        public final DataType returnType;

        private BuiltinInfo(String name, DataType returnType, Parameter[] params) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
        }
    }

    // ---------- helpers ----------
    private static Parameter newParam(String name, DataType type) {
        return new Parameter(name, type);
    }

    private static Parameter newParam(String name, DataType type, boolean mandatory) {
        return new Parameter(name, type, mandatory);
    }

    private static BuiltinInfo info(String name, DataType ret, Parameter... defs) {
        return new BuiltinInfo(name, ret, defs);
    }

    private static void addBuiltin(BuiltinInfo info) {
        BUILTINS.put(info.name.toLowerCase(), info);
    }

    static {
        // ==========================
        // JSON builtins
        // ==========================
        addBuiltin(info(
                "json.jsonFromString", // builtin name
                DataType.JSON, // return the parsed schema we stored
                newParam("json", DataType.STRING) // accepts String 
        ));
        addBuiltin(info(
                "json.registerScheme", // builtin name
                DataType.JSON, // return the parsed schema we stored
                newParam("name", DataType.STRING),
                newParam("schema", DataType.JSON, false) // accepts String or Map/List
        ));
        addBuiltin(info(
                "json.deriveScheme", // builtin name
                DataType.JSON, // return the schema generated
                newParam("json", DataType.JSON),
                newParam("calcLengths", DataType.BOOL, false)
        ));
        addBuiltin(info(
                "json.validate", // builtin name
                DataType.JSON, // returns List<ValidationError> as JSON array
                newParam("schemaName", DataType.STRING),
                newParam("json", DataType.JSON)
        ));
        addBuiltin(info(
                "json.isEmpty", // builtin name
                DataType.BOOL, // returns boolean
                newParam("value", DataType.JSON)));

        addBuiltin(info(
                "json.get", DataType.JSON,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING)
        ));
        addBuiltin(info(
                "json.getStrict", DataType.JSON,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING)
        ));
        addBuiltin(info(
                "json.getString", DataType.STRING,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.STRING, false)
        ));
        addBuiltin(info(
                "json.getInt", DataType.INTEGER,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.INTEGER, false)
        ));
        addBuiltin(info(
                "json.getInteger", DataType.INTEGER,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.INTEGER, false)
        ));
        addBuiltin(info(
                "json.getLong", DataType.LONG,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.LONG, false)
        ));
        addBuiltin(info(
                "json.getDouble", DataType.DOUBLE,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.DOUBLE, false)
        ));
        addBuiltin(info(
                "json.getBool", DataType.BOOL,
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("default", DataType.BOOL, false)
        ));
        addBuiltin(info(
                "json.getArray", DataType.JSON, // List in the JSON domain
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING)
        ));
        addBuiltin(info(
                "json.getObject", DataType.JSON, // Map in the JSON domain
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING)
        ));
        addBuiltin(info(
                "json.set", DataType.JSON, // returns mutated root
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("value", DataType.JSON)
        ));
        addBuiltin(info(
                "json.setStrict", DataType.JSON, // returns mutated root
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING),
                newParam("value", DataType.JSON)
        ));
        addBuiltin(info(
                "json.remove", DataType.JSON, // returns mutated root
                newParam("root", DataType.JSON),
                newParam("path", DataType.STRING)
        ));
        addBuiltin(info(
                "json.add", DataType.JSON, // returns mutated root
                newParam("root", DataType.JSON),
                newParam("arrayPath", DataType.STRING),
                newParam("value", DataType.JSON)
        ));
        addBuiltin(info(
                "json.insert", DataType.JSON, // returns mutated root
                newParam("root", DataType.JSON),
                newParam("arrayPath", DataType.STRING),
                newParam("index", DataType.INTEGER),
                newParam("value", DataType.JSON)
        ));

        // ==========================
        // STRING builtins
        // ==========================
        addBuiltin(info(
                "str.toString", DataType.STRING,
                newParam("str", DataType.STRING)
        ));
        // Alias: string.tostring (accepts any type, converts to string)
        addBuiltin(info(
                "string.tostring", DataType.STRING,
                newParam("value", DataType.STRING, true) // accepts any type
        ));
        addBuiltin(info(
                "str.toUpper", DataType.STRING,
                newParam("str", DataType.STRING)
        ));
        addBuiltin(info(
                "str.toLower", DataType.STRING,
                newParam("str", DataType.STRING)
        ));
        addBuiltin(info(
                "str.trim", DataType.STRING,
                newParam("str", DataType.STRING)
        ));
        addBuiltin(info(
                "str.replace", DataType.STRING,
                newParam("source", DataType.STRING),
                newParam("target", DataType.STRING),
                newParam("replacement", DataType.STRING)
        ));
        // str.split(text, regex) -> JSON (List<String>)
        // Uses Java regex; trailing empty strings are preserved (limit = -1).
        addBuiltin(info(
                "str.split", DataType.JSON,
                newParam("text", DataType.STRING),
                newParam("regex", DataType.STRING),
                newParam("limit", DataType.INTEGER, false)
        ));

        addBuiltin(info(
                "str.join", DataType.STRING,
                newParam("text", DataType.JSON, true),
                newParam("nl", DataType.STRING)
        ));

        addBuiltin(info(
                "str.contains", DataType.BOOL,
                newParam("str", DataType.STRING),
                newParam("sub", DataType.STRING)
        ));
        addBuiltin(info(
                "str.startsWith", DataType.BOOL,
                newParam("str", DataType.STRING),
                newParam("prefix", DataType.STRING)
        ));
        addBuiltin(info(
                "str.endsWith", DataType.BOOL,
                newParam("str", DataType.STRING),
                newParam("suffix", DataType.STRING)
        ));
        addBuiltin(info(
                "str.equalsIgnoreCase", DataType.BOOL,
                newParam("value1", DataType.STRING),
                newParam("value2", DataType.STRING)
        ));
        addBuiltin(info(
                "str.equals", DataType.BOOL,
                newParam("value1", DataType.STRING),
                newParam("value2", DataType.STRING)
        ));
        addBuiltin(info(
                "str.isEmpty", DataType.BOOL,
                newParam("str", DataType.STRING)
        ));
        addBuiltin(info(
                "str.isBlank", DataType.BOOL,
                newParam("str", DataType.STRING)
        ));
        
        // str.substring(str, beginIndex, endIndex?) -> STRING
        // Extract substring from string. If endIndex is not provided, extracts to end of string.
        // Uses Java String.substring() semantics.
        addBuiltin(info(
                "str.substring", DataType.STRING,
                newParam("str", DataType.STRING),
                newParam("beginIndex", DataType.INTEGER),
                newParam("endIndex", DataType.INTEGER, false) // optional
        ));
        
        // str.indexOf(str, searchString, fromIndex?) -> INTEGER
        // Find first occurrence of searchString in str, starting from fromIndex (default 0).
        // Returns -1 if not found. Uses Java String.indexOf() semantics.
        addBuiltin(info(
                "str.indexOf", DataType.INTEGER,
                newParam("str", DataType.STRING),
                newParam("searchString", DataType.STRING),
                newParam("fromIndex", DataType.INTEGER, false) // optional
        ));
        
        // str.lastIndexOf(str, searchString, fromIndex?) -> INTEGER
        // Find last occurrence of searchString in str, searching backwards from fromIndex (default end).
        // Returns -1 if not found. Uses Java String.lastIndexOf() semantics.
        addBuiltin(info(
                "str.lastIndexOf", DataType.INTEGER,
                newParam("str", DataType.STRING),
                newParam("searchString", DataType.STRING),
                newParam("fromIndex", DataType.INTEGER, false) // optional
        ));
        
        // str.charAt(str, index) -> STRING
        // Returns the character at the specified index as a single-character string.
        // Uses Java String.charAt() semantics.
        addBuiltin(info(
                "str.charAt", DataType.STRING,
                newParam("str", DataType.STRING),
                newParam("index", DataType.INTEGER)
        ));
        
        // str.replaceAll(str, regex, replacement) -> STRING
        // Replaces all occurrences of regex pattern with replacement string.
        // Uses Java String.replaceAll() semantics (regex-based).
        addBuiltin(info(
                "str.replaceAll", DataType.STRING,
                newParam("str", DataType.STRING),
                newParam("regex", DataType.STRING),
                newParam("replacement", DataType.STRING)
        ));
        
        // str.lpad(str, length, padChar) -> STRING
        // Left-pads the string with padChar to reach the specified length.
        // If string is already >= length, returns original string unchanged.
        addBuiltin(info(
                "str.lpad", DataType.STRING,
                newParam("str", DataType.STRING),
                newParam("length", DataType.INTEGER),
                newParam("padChar", DataType.STRING)
        ));
        
        // str.rpad(str, length, padChar) -> STRING
        // Right-pads the string with padChar to reach the specified length.
        // If string is already >= length, returns original string unchanged.
        addBuiltin(info(
                "str.rpad", DataType.STRING,
                newParam("str", DataType.STRING),
                newParam("length", DataType.INTEGER),
                newParam("padChar", DataType.STRING)
        ));
        
        // str.charArray(str) -> ARRAY (int[])
        // Returns an array of integer character codes for each character in the string.
        // Each character's Unicode code point is returned as an int.
        addBuiltin(info(
                "str.charArray", DataType.ARRAY,
                newParam("str", DataType.STRING)
        ));

        addBuiltin(info(
                "file.exists", DataType.BOOL,
                newParam("path", DataType.STRING)
        ));

        addBuiltin(info(
                "file.size", DataType.LONG,
                newParam("pathOrHandle", DataType.STRING)
        ));
        // file.open(path, mode?) -> STRING (handle)
        addBuiltin(info(
                "file.open", DataType.STRING,
                newParam("path", DataType.STRING),
                newParam("mode", DataType.STRING, false) // optional; null -> "r"
        ));

        // file.close(handleOrPath) -> BOOL
        addBuiltin(info(
                "file.close", DataType.BOOL,
                newParam("handle", DataType.STRING)
        ));

        // file.listOpenFiles() -> JSON (array of open file info)
        addBuiltin(info(
                "file.listOpenFiles", DataType.JSON
        ));

        // file.readln(handleOrPath, encoding?) -> STRING | null
        addBuiltin(info(
                "file.readln", DataType.STRING,
                newParam("handle", DataType.STRING),
                newParam("encoding", DataType.STRING, false) // optional; null -> "UTF-8"
        ));

        addBuiltin(info(
                "file.writeln", DataType.BOOL,
                newParam("handle", DataType.STRING),
                newParam("text", DataType.STRING),
                newParam("encoding", DataType.STRING, false) // optional; null -> "UTF-8"
        ));

        addBuiltin(info(
                "file.read", DataType.STRING,
                newParam("handle", DataType.STRING)
        ));

        addBuiltin(info(
                "file.write", DataType.BOOL,
                newParam("handle", DataType.STRING),
                newParam("text", DataType.STRING),
                newParam("encoding", DataType.STRING, false) // optional; null -> "UTF-8"
        ));

        addBuiltin(info(
                "file.readBin", DataType.ARRAY,
                newParam("handle", DataType.STRING)
        ));

        addBuiltin(info(
                "file.writeBin", DataType.BOOL,
                newParam("handle", DataType.STRING),
                newParam("content", DataType.ARRAY)
        ));

        // file.eof(handleOrPath) -> BOOL
        addBuiltin(info(
                "file.eof", DataType.BOOL,
                newParam("handle", DataType.STRING)
        ));

        addBuiltin(info(
                "file.readTextFile", DataType.STRING,
                newParam("path", DataType.STRING)
        ));

        addBuiltin(info(
                "file.writeTextFile", DataType.BOOL,
                newParam("path", DataType.STRING),
                newParam("content", DataType.STRING)
        ));
        addBuiltin(info(
                "file.readBinFile", DataType.ARRAY,
                newParam("path", DataType.STRING)
        ));

        addBuiltin(info(
                "file.writeBinFile", DataType.BOOL,
                newParam("path", DataType.STRING),
                newParam("content", DataType.STRING)
        ));

        addBuiltin(info(
                "file.appendToTextFile", DataType.BOOL,
                newParam("path", DataType.STRING),
                newParam("content", DataType.STRING)
        ));

// JSON listing of files (entries: name, isDir, size, modifiedMs)
        addBuiltin(info(
                "file.listFiles", DataType.JSON,
                newParam("path", DataType.STRING, false)
        ));

// Rename within the same directory (path + new name only)
        addBuiltin(info(
                "file.rename", DataType.BOOL,
                newParam("path", DataType.STRING),
                newParam("newName", DataType.STRING)
        ));

// Move (source -> dest)
        addBuiltin(info(
                "file.move", DataType.BOOL,
                newParam("source", DataType.STRING),
                newParam("dest", DataType.STRING)
        ));

        // Copy (source -> dest)
        addBuiltin(info(
                "file.copy", DataType.BOOL,
                newParam("source", DataType.STRING),
                newParam("dest", DataType.STRING)
        ));

        // Open/close ZIP archives
        addBuiltin(info(
                "file.openZip", DataType.STRING,
                newParam("path", DataType.STRING),
                newParam("mode", DataType.STRING) // optional; default "r" (read-only)
        ));
        addBuiltin(info(
                "file.closeZip", DataType.BOOL,
                newParam("handle", DataType.STRING)
        ));
        // file.listZipFiles(handleOrPath, path?) -> JSON (array of entry info)
        addBuiltin(info(
                "file.listZipFiles", DataType.JSON,
                newParam("handle", DataType.STRING),
                newParam("path", DataType.STRING, false) // optional; default "/"
        ));

        // file.unzip(handleOrPath, destDir, entries?, overwrite?) -> BOOL
        // entries: STRING (single) or ARRAY/LIST of STRINGs; omit/null -> all
        addBuiltin(info(
                "file.unzip", DataType.BOOL,
                newParam("handle", DataType.STRING),
                newParam("destDir", DataType.STRING),
                newParam("entries", DataType.JSON, false), // accept String or List<String>
                newParam("overwrite", DataType.BOOL, false)
        ));

        addBuiltin(info(
                "http.request", DataType.JSON,
                newParam("url", DataType.STRING),
                newParam("method", DataType.STRING), // optional (null => "GET")
                newParam("body", DataType.STRING), // optional for GET/HEAD
                newParam("headers", DataType.JSON), // Map<String,String> or null
                newParam("timeoutMs", DataType.LONG) // optional (null => default)
        ));

        // ==========================
        // HTTP convenience builtins
        // ==========================
        addBuiltin(info(
                "http.get", DataType.JSON,
                newParam("url", DataType.STRING),
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        addBuiltin(info(
                "http.post", DataType.JSON,
                newParam("url", DataType.STRING),
                newParam("body", DataType.STRING), // optional
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        addBuiltin(info(
                "http.getJson", DataType.JSON,
                newParam("url", DataType.STRING),
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        addBuiltin(info(
                "http.postJson", DataType.JSON,
                newParam("url", DataType.STRING),
                newParam("jsonBody", DataType.JSON), // Map/List/String/Number/Bool allowed by DataType.JSON
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        addBuiltin(info(
                "http.getText", DataType.STRING,
                newParam("url", DataType.STRING),
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        addBuiltin(info(
                "http.postText", DataType.STRING,
                newParam("url", DataType.STRING),
                newParam("body", DataType.STRING), // required content as STRING
                newParam("headers", DataType.JSON, false), // optional
                newParam("timeoutMs", DataType.LONG, false) // optional
        ));

        // ==========================
        // HTTP status helpers
        // ==========================
        addBuiltin(info(
                "http.ensure2xx", DataType.JSON,
                newParam("statusOrEnvelope", DataType.JSON), // Map | Number (DataType.JSON accepts both)
                newParam("message", DataType.STRING, false) // optional
        ));

        // (Optional) predicate variant:
        addBuiltin(info(
                "http.is2xx", DataType.BOOL,
                newParam("statusOrEnvelope", DataType.JSON)
        ));

// Debug/log/assert
        addBuiltin(info("debug.on", DataType.BOOL)); //switch debug on
        addBuiltin(info("debug.off", DataType.BOOL)); //switch debug off
        addBuiltin(info("debug.traceOn", DataType.BOOL)); //switch debug on
        addBuiltin(info("debug.traceOff", DataType.BOOL)); //switch debug off
        addBuiltin(info("debug.file", null, newParam("fileName", DataType.STRING))); // standard output if file name is null
        addBuiltin(info("debug.newFile", null, newParam("fileName", DataType.STRING))); // standard output if file name is null
        addBuiltin(info("debug.log", DataType.BOOL, newParam("level", DataType.STRING), newParam("message", DataType.STRING))); // log timestamped message
        addBuiltin(info("debug.assert", DataType.BOOL, newParam("condition", DataType.BOOL), newParam("message", DataType.STRING, false)));
        addBuiltin(info("debug.assertEquals", DataType.BOOL, newParam("expected", DataType.JSON), newParam("actual", DataType.JSON), newParam("message", DataType.STRING, false)));
        // Echo mode
        addBuiltin(info("echo.on", null)); // Enable echo mode
        addBuiltin(info("echo.off", null)); // Disable echo mode
        // Vars/stack
        addBuiltin(info("debug.vars", DataType.JSON));
        addBuiltin(info("debug.stack", DataType.JSON));

        // debug.memUsage([unit]) -> JSON { max, total, free, used, unit }
        addBuiltin(info(
                "debug.memUsage", DataType.JSON,
                // Optional unit: "MB" (default), "KB", or "B"
                newParam("unit", DataType.STRING, false)
        ));

        addBuiltin(info(
                "array.fill", null,
                newParam("array", DataType.ARRAY, true),
                newParam("length", DataType.INTEGER),
                newParam("value", DataType.ANY, false)
        ));

        addBuiltin(info(
                "array.sort", null,
                newParam("array", DataType.ARRAY, true),
                newParam("ascending", DataType.BOOL, false)
        ));

        addBuiltin(info(
                "array.expand", null,
                newParam("array", DataType.ARRAY, true),
                newParam("length", DataType.INTEGER)
        ));
        addBuiltin(info(
                "array.base64encode", DataType.STRING,
                newParam("bytes", DataType.ARRAY, true)
        ));
        addBuiltin(info(
                "array.base64decode", DataType.ARRAY,
                newParam("b64", DataType.STRING)
        ));

        addBuiltin(info(
                "system.command", DataType.JSON,
                newParam("command", DataType.STRING), // required
                newParam("args", DataType.JSON, false),// optional, array of JSON -> List
                newParam("timeoutMs", DataType.LONG, false),
                newParam("cwd", DataType.STRING, false)
        ));

        addBuiltin(info(
                "system.winCommand", DataType.JSON,
                newParam("command", DataType.STRING), // required
                newParam("args", DataType.JSON, false),// optional, array of JSON -> List
                newParam("timeoutMs", DataType.LONG, false),
                newParam("cwd", DataType.STRING, false)
        ));

        // ==========================
        // System properties builtins
        // ==========================
        addBuiltin(info(
                "system.getProperty", DataType.STRING,
                newParam("name", DataType.STRING), // required
                newParam("default", DataType.STRING, false) // optional
        ));

        addBuiltin(info(
                "system.setProperty", DataType.STRING,
                newParam("name", DataType.STRING), // required
                newParam("value", DataType.STRING, false) // optional (null => clear)
        ));

        addBuiltin(info(
                "system.help", DataType.STRING, // returns help text as string
                newParam("keyword", DataType.STRING, false) // optional keyword/builtin name
        ));

        addBuiltin(info(
                "system.inputDialog", DataType.STRING, // returns the input text or empty string if cancelled
                newParam("title", DataType.STRING, true),      // required: dialog title
                newParam("headerText", DataType.STRING, false), // optional: header text
                newParam("defaultValue", DataType.STRING, false) // optional: default input value
        ));

        addBuiltin(info(
                "system.confirmDialog", DataType.BOOL, // returns true if confirmed, false if cancelled
                newParam("message", DataType.STRING, true),     // required: confirmation message
                newParam("title", DataType.STRING, false),      // optional: dialog title
                newParam("headerText", DataType.STRING, false)  // optional: header text
        ));

        addBuiltin(info(
                "system.alertDialog", null, // returns nothing (void) when acknowledged
                newParam("message", DataType.STRING, true),     // required: alert message
                newParam("title", DataType.STRING, false),      // optional: dialog title (defaults to "Alert")
                newParam("alertType", DataType.STRING, false)   // optional: info, warning, error (defaults to info)
        ));

        addBuiltin(info(
                "sleep", DataType.STRING,
                newParam("millis", DataType.LONG) // required: milliseconds to sleep
        ));

// ==========================
// AI builtins
// ==========================
        addBuiltin(info(
                "ai.complete", DataType.STRING,
                newParam("system", DataType.STRING, false),
                newParam("user", DataType.STRING, true),
                newParam("maxTokens", DataType.INTEGER, false),
                newParam("temperature", DataType.DOUBLE, false)
        ));
        addBuiltin(info(
                "ai.summarize", DataType.STRING,
                newParam("text", DataType.STRING, true),
                newParam("maxTokens", DataType.INTEGER, false)
        ));
        addBuiltin(info(
                "ai.embed", DataType.JSON, // we’ll return array of numbers (JSON array)
                newParam("text", DataType.STRING, true)
        ));
        addBuiltin(info(
                "ai.classify", DataType.STRING,
                newParam("text", DataType.STRING, true),
                newParam("labels", DataType.JSON, true) // expect List<String>
        ));

        // ==========================
        // ClassTree builtins
        // ==========================
        addBuiltin(info(
                "classTree.generate", DataType.STRING,
                newParam("sourceDir", DataType.STRING, false) // optional; defaults to "src/main/java"
        ));
        addBuiltin(info(
                "classTree.scan", DataType.STRING,
                newParam("sourceDir", DataType.STRING, false) // optional; defaults to "src/main/java"
        ));

        // ==========================
        // Screen property builtins
        // ==========================
        addBuiltin(info(
                "scr.setProperty", DataType.BOOL,
                newParam("areaItem", DataType.STRING, true), // required; format "screenName.areaItemName"
                newParam("property", DataType.STRING, true), // required; property name
                newParam("value", DataType.ANY, true) // required; value to set
        ));
        addBuiltin(info(
                "scr.getProperty", DataType.ANY,
                newParam("areaItem", DataType.STRING, true), // required; format "screenName.areaItemName"
                newParam("property", DataType.STRING, true) // required; property name
        ));
        addBuiltin(info(
                "scr.getItemList", DataType.ANY,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.getScreenItemList", DataType.ANY,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.showScreen", DataType.BOOL,
                newParam("screenName", DataType.STRING, false) // optional; screen name (if null, uses current screen)
        ));
        addBuiltin(info(
                "scr.hideScreen", DataType.BOOL,
                newParam("screenName", DataType.STRING, false) // optional; screen name (if null, uses current screen)
        ));
        addBuiltin(info(
                "scr.closeScreen", DataType.BOOL,
                newParam("screenName", DataType.STRING, false) // optional; screen name (if null, uses current screen)
        ));
        addBuiltin(info(
                "scr.setStatus", DataType.BOOL,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("status", DataType.STRING, true) // required; status: "clean", "changed", or "error"
        ));
        addBuiltin(info(
                "scr.getStatus", DataType.STRING,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.setError", DataType.BOOL,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("errorMessage", DataType.STRING, true) // required; error message
        ));
        addBuiltin(info(
                "scr.getError", DataType.STRING,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.getItemSource", DataType.STRING,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true) // required; item name (varRef)
        ));
        addBuiltin(info(
                "scr.setItemSource", DataType.BOOL,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true), // required; item name (varRef)
                newParam("source", DataType.STRING, true) // required; source: "data" or "display"
        ));
        addBuiltin(info(
                "scr.getItemStatus", DataType.STRING,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true) // required; item name (varRef)
        ));
        addBuiltin(info(
                "scr.resetItemOriginalValue", DataType.BOOL,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true) // required; item name (varRef)
        ));
        addBuiltin(info(
                "scr.checkChanged", DataType.BOOL,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.checkError", DataType.BOOL,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.revert", DataType.BOOL,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.clear", DataType.BOOL,
                newParam("screenName", DataType.STRING, true) // required; screen name
        ));
        addBuiltin(info(
                "scr.getVarReference", DataType.STRING,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true) // required; item name
        ));
        addBuiltin(info(
                "scr.getAreaProperty", DataType.ANY,
                newParam("area", DataType.STRING, true), // required; format "screenName.areaName"
                newParam("property", DataType.STRING, true) // required; property name
        ));
        addBuiltin(info(
                "scr.setAreaProperty", DataType.BOOL,
                newParam("area", DataType.STRING, true), // required; format "screenName.areaName"
                newParam("property", DataType.STRING, true), // required; property name
                newParam("value", DataType.ANY, true) // required; value to set
        ));
        addBuiltin(info(
                "scr.setItemChoiceOptions", DataType.BOOL,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true), // required; item name
                newParam("optionsMap", DataType.ANY, true) // required; map of display text to data values
        ));
        addBuiltin(info(
                "scr.getItemChoiceOptions", DataType.ANY,
                newParam("screenName", DataType.STRING, true), // required; screen name
                newParam("itemName", DataType.STRING, true) // required; item name
        ));

        // ==========================
        // CSS builtins
        // ==========================
        addBuiltin(info(
                "css.getValue", DataType.STRING,
                newParam("cssPath", DataType.STRING, true),   // required; path to CSS file or resource
                newParam("selector", DataType.STRING, true),  // required; CSS selector (e.g., ".error")
                newParam("property", DataType.STRING, true)   // required; CSS property name (e.g., "-fx-fill")
        ));
        addBuiltin(info(
                "css.findCss", DataType.ARRAY,
                newParam("searchPath", DataType.STRING, false)  // optional; base path to search in
        ));

        NAMES = Collections.unmodifiableSet(BUILTINS.keySet());
    }

    public static boolean isBuiltin(String name) {
        return NAMES.contains(name);
    }

    // genral script functions that can be called with "call"
    public static Object callBuiltin(Environment env, String name, Object... args) throws InterpreterError {
        return callBuiltin(env, null, name, args);
    }

    public static Object callBuiltin(InterpreterContext context, String name, Object... args) throws InterpreterError {
        Environment env = context.getEnvironment();
        return callBuiltin(env, context, name, args);
    }

    public static Object callBuiltin(Environment env, InterpreterContext context, String name, Object... args) throws InterpreterError {
        if (args == null) {
            args = new String[0];
        }
        
        // Delegate to category-specific handler classes
        if (BuiltinsJson.handles(name)) {
            return BuiltinsJson.dispatch(name, args);
        }
        if (BuiltinsString.handles(name)) {
            return BuiltinsString.dispatch(name, args);
        }
        if (BuiltinsHttp.handles(name)) {
            return BuiltinsHttp.dispatch(name, args);
        }
        if (BuiltinsDebug.handles(name)) {
            return BuiltinsDebug.dispatch(env, name, args);
        }
        if (BuiltinsAi.handles(name)) {
            return BuiltinsAi.dispatch(name, args);
        }
        if (BuiltinsHelp.handles(name)) {
            return BuiltinsHelp.dispatch(env, name, args);
        }
        if (BuiltinsSystem.handles(name)) {
            return BuiltinsSystem.dispatch(env, name, args);
        }
        
        // CSS builtins
        if (BuiltinsCss.handles(name)) {
            return BuiltinsCss.dispatch(name, args);
        }
        
        // File builtins (already in BuiltinsFile)
        if (name.startsWith("file.") || name.startsWith("classtree.")) {
            return dispatchFileBuiltin(env, name, args);
        }
        
        // Screen builtins (already in BuiltinsScreen)
        if (name.startsWith("scr.")) {
            return dispatchScreenBuiltin(context, name, args);
        }
        
        throw new InterpreterError("Unknown builtin: " + name);
    }
    
    /**
     * Dispatch file-related builtins to BuiltinsFile.
     */
    private static Object dispatchFileBuiltin(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "file.exists" -> BuiltinsFile.exists(env, args);
            case "file.size" -> BuiltinsFile.size(env, args);
            case "file.open" -> BuiltinsFile.fileOpen(env, args);
            case "file.close" -> BuiltinsFile.fileClose(env, args);
            case "file.listopenfiles" -> BuiltinsFile.listOpenFiles(env, args);
            case "file.readln" -> BuiltinsFile.readln(env, args);
            case "file.writeln" -> BuiltinsFile.writeln(env, args);
            case "file.read" -> BuiltinsFile.read(env, args);
            case "file.write" -> BuiltinsFile.write(env, args);
            case "file.eof" -> BuiltinsFile.eof(env, args);
            case "file.readtextfile" -> BuiltinsFile.readTextFile(env, args);
            case "file.readbinfile" -> BuiltinsFile.readBinFile(env, args);
            case "file.writetextfile" -> BuiltinsFile.writeTextFile(env, args);
            case "file.writebinfile" -> BuiltinsFile.writeBinFile(env, args);
            case "file.appendtotextfile" -> BuiltinsFile.appendToTextFile(env, args);
            case "file.listfiles" -> BuiltinsFile.listFiles(env, args);
            case "file.rename" -> BuiltinsFile.rename(env, args);
            case "file.move" -> BuiltinsFile.move(env, args);
            case "file.copy" -> BuiltinsFile.copy(env, args);
            case "file.openzip" -> BuiltinsFile.openZip(env, args);
            case "file.createzip" -> BuiltinsFile.createZip(env, args);
            case "file.listzipfiles" -> BuiltinsFile.listZipFiles(env, args);
            case "file.unzip" -> BuiltinsFile.unzip(env, args);
            case "file.closezip" -> BuiltinsFile.closeZip(env, args);
            case "classtree.generate" -> BuiltinsFile.generateClassTree(env, args);
            case "classtree.scan" -> BuiltinsFile.scanClassTree(env, args);
            default -> throw new InterpreterError("Unknown file builtin: " + name);
        };
    }
    
    /**
     * Dispatch screen-related builtins to BuiltinsScreen.
     */
    private static Object dispatchScreenBuiltin(InterpreterContext context, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "scr.setproperty" -> BuiltinsScreen.screenSetProperty(context, args);
            case "scr.getproperty" -> BuiltinsScreen.screenGetProperty(context, args);
            case "scr.getitemlist" -> BuiltinsScreen.screenGetItemList(context, args);
            case "scr.getscreenitemlist" -> BuiltinsScreen.screenGetScreenItemList(context, args);
            case "scr.showscreen" -> BuiltinsScreen.screenShow(context, args);
            case "scr.hidescreen" -> BuiltinsScreen.screenHide(context, args);
            case "scr.closescreen" -> BuiltinsScreen.screenClose(context, args);
            case "scr.setstatus" -> BuiltinsScreen.screenSetStatus(context, args);
            case "scr.getstatus" -> BuiltinsScreen.screenGetStatus(context, args);
            case "scr.seterror" -> BuiltinsScreen.screenSetError(context, args);
            case "scr.geterror" -> BuiltinsScreen.screenGetError(context, args);
            case "scr.getitemsource" -> BuiltinsScreen.screenGetItemSource(context, args);
            case "scr.setitemsource" -> BuiltinsScreen.screenSetItemSource(context, args);
            case "scr.getitemstatus" -> BuiltinsScreen.screenGetItemStatus(context, args);
            case "scr.resetitemoriginalvalue" -> BuiltinsScreen.screenResetItemOriginalValue(context, args);
            case "scr.checkchanged" -> BuiltinsScreen.screenCheckChanged(context, args);
            case "scr.checkerror" -> BuiltinsScreen.screenCheckError(context, args);
            case "scr.revert" -> BuiltinsScreen.screenRevert(context, args);
            case "scr.clear" -> BuiltinsScreen.screenClear(context, args);
            case "scr.getvarreference" -> BuiltinsScreen.screenGetVarReference(context, args);
            case "scr.getareaproperty" -> BuiltinsScreen.screenGetAreaProperty(context, args);
            case "scr.setareaproperty" -> BuiltinsScreen.screenSetAreaProperty(context, args);
            case "scr.setitemchoiceoptions" -> BuiltinsScreen.screenSetItemChoiceOptions(context, args);
            case "scr.getitemchoiceoptions" -> BuiltinsScreen.screenGetItemChoiceOptions(context, args);
            default -> throw new InterpreterError("Unknown screen builtin: " + name);
        };
    }
    
    // The following switch cases have been moved to handler classes:
    // - JSON builtins -> BuiltinsJson
    // - String builtins -> BuiltinsString
    // - HTTP builtins -> BuiltinsHttp
    // - Debug/Echo builtins -> BuiltinsDebug
    // - AI builtins -> BuiltinsAi
    // - System/Array/Sleep builtins -> BuiltinsSystem
    // - Help/Dialog builtins -> BuiltinsHelp
    // - File builtins -> BuiltinsFile (existing)
    // - Screen builtins -> BuiltinsScreen (existing)

    // --- REMOVED: Original switch statement with 135+ cases ---
    // The original switch statement has been replaced by delegation to handler classes above.
    // Each handler class contains a dispatch method with its own switch statement,
    // making the code more maintainable and organized by category.
    
    /**
     * Non-strict lookup: returns the value or null if any step is
     * missing/mismatched.
     */
    public static Object jsonGet(Object jsonRoot, String path) {
        return Json.getValue(jsonRoot, path);
    }

    /**
     * Strict lookup: throws JsonPathException if any step is
     * missing/mismatched.
     */
    public static Object jsonGetStrict(Object jsonRoot, String path) {
        return Json.getValueStrict(jsonRoot, path);
    }

    /**
     * Typed lookup (non-strict): returns defaultValue if null or type mismatch.
     */
    public static <T> T jsonGetAs(Object jsonRoot, String path, Class<T> type, T defaultValue) {
        return Json.getAs(jsonRoot, path, type, defaultValue);
    }

// ---- Common typed convenience wrappers (non-strict) ----
    public static String jsonGetString(Object jsonRoot, String path, String defaultValue) {
        return jsonGetAs(jsonRoot, path, String.class, defaultValue);
    }

    public static Integer jsonGetInt(Object jsonRoot, String path, Integer defaultValue) {
        Number n = jsonGetAs(jsonRoot, path, Number.class, null);
        return (n != null) ? (n instanceof Integer ? (Integer) n : n.intValue()) : defaultValue;
    }

    public static Long jsonGetLong(Object jsonRoot, String path, Long defaultValue) {
        Number n = jsonGetAs(jsonRoot, path, Number.class, null);
        return (n != null) ? (n instanceof Long ? (Long) n : n.longValue()) : defaultValue;
    }

    public static Double jsonGetDouble(Object jsonRoot, String path, Double defaultValue) {
        Number n = jsonGetAs(jsonRoot, path, Number.class, null);
        return (n != null) ? (n instanceof Double ? (Double) n : n.doubleValue()) : defaultValue;
    }

    public static Boolean jsonGetBool(Object jsonRoot, String path, Boolean defaultValue) {
        return jsonGetAs(jsonRoot, path, Boolean.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonGetObject(Object jsonRoot, String path) {
        Object v = jsonGet(jsonRoot, path);
        return (v instanceof java.util.Map) ? (java.util.Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static ArrayDef jsonGetArray(Object jsonRoot, String path) {
        Object v = jsonGet(jsonRoot, path);
        return (v instanceof ArrayDef) ? (ArrayDef) v : null;
    }

    // NOTE: The following helper methods were previously used by the old massive switch statement
    // and have been moved to their respective handler classes:
    // - httpExtractStatus -> BuiltinsHttp
    // - runCmd -> BuiltinsSystem  
    // - sysOutput -> used in BuiltinsDebug
    // - getDetailedHelp, formatHelpEntry -> BuiltinsHelp


    public static String getBuiltinCallString(String name, Object retValue, Object[] args) {
        name = name.toLowerCase();
        StringBuilder ret = new StringBuilder(name);
        BuiltinInfo info = getBuiltinInfo(name);
        Parameter[] params = info.params;
        if (args.length > 0) {
            ret.append("(");
            int idx = 0;
            for (Object o : args) {
                if (idx > 0) {
                    ret.append(", ");
                }
                String v = "";
                DataType ptype = params[idx].paramType;
                v = Util.stringifyShort(o);
                ret.append(params[idx].name).append("=").append(v);
                idx++;
            }
            ret.append(")");
        }
        if (retValue != null) {
            ret.append(" : ").append(Util.stringifyShort(retValue));
        }
        return ret.toString();
    }
}
