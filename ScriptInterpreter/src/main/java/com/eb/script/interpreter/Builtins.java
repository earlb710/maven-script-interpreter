package com.eb.script.interpreter;

import com.eb.script.file.BuiltinsFile;
import com.eb.script.json.Json;
import com.eb.util.Debugger;
import com.eb.script.token.DataType;
import com.eb.util.Util;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.screen.AreaDefinition;
import com.eb.script.interpreter.screen.AreaItem;
import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.Var;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.json.JsonSchema;
import com.eb.script.json.JsonSchemaDeriver;
import com.eb.script.json.JsonValidate;
import com.eb.script.token.Category;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.ui.cli.ScriptArea;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    }

    /**
     * Allow the interpreter to inject a "stack frames" supplier.
     */
    public static void setStackSupplier(StackSupplier supplier) {
        STACK_SUPPLIER = supplier;
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
                "system.alertDialog", DataType.BOOL, // returns true when acknowledged
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
        Debugger debug = env.getDebugger();
        ScriptArea output = env.getOutputArea();
        if (args == null) {
            args = new String[0];
        }
        switch (name) {
            case "json.jsonfromstring" -> {
                Object jsonArg = (String) args[0];
                if (jsonArg instanceof String val) {
                    return Json.parse(val);
                } else {
                    return null;
                }
            }
            case "json.derivescheme" -> {
                Object jsonArg = (String) args[0];
                Object json;
                if (jsonArg instanceof String val) {
                    json = Json.parse(val);
                } else {
                    json = jsonArg;
                }
                boolean calcLength = false;
                if (args.length > 1) {
                    if (args[1] instanceof Boolean val) {
                        calcLength = val;
                    } else if (args[1] instanceof String val) {
                        calcLength = val.substring(0, 1).toLowerCase().equals("y");
                    }
                }
                JsonSchemaDeriver.Options options = new JsonSchemaDeriver.Options();
                options.computeStringLengths = calcLength;
                return JsonSchemaDeriver.derive(json, options);
            }
            case "json.registerscheme" -> {
                // Args: name (String), schema (String or JSON Map/List)
                String schemaName = (String) args[0];
                Object schemaParam = args[1];

                // If caller passed a String, use as-is; otherwise compact JSON to String
                final String schemaJson;
                if (schemaParam == null) {
                    // Decide your preferred behaviour: empty schema vs error.
                    // Here we register an empty object.
                    schemaJson = "{}";
                } else if (schemaParam instanceof String s) {
                    schemaJson = s;
                } else {
                    // Map/List/primitive -> compact JSON string
                    schemaJson = Json.compactJson(schemaParam);
                }

                // Register & return the stored (parsed) schema
                JsonValidate.registerSchema(schemaName, schemaJson);
                return JsonValidate.getSchema(schemaName);
            }
            case "json.validate" -> {
                // Args: schemaName (String), json (Map/List/String/Number/Boolean/null)
                String schemaName = (String) args[0];
                Object jsonValue = args[1];

                // Resolve schema from registry
                Map<String, Object> schema = JsonValidate.getSchema(schemaName);
                if (schema == null) {
                    throw new InterpreterError("json.validate: schema '" + schemaName + "' not registered");
                }

                // Validate and return the list of errors (JSON-domain list)
                Map<String, ArrayDef> errors = JsonSchema.validate(jsonValue, schema);
                return errors; // treated as JSON array by the interpreter
            }
            case "json.isempty" -> {
                return Json.isEmpty(args[0]);
            }
            case "json.get" -> {
                return Json.getValue(args[0], String.valueOf(args[1]));
            }
            case "json.getstrict" -> {
                return jsonGetStrict(args[0], String.valueOf(args[1]));
            }
            case "json.getstring" -> {
                return jsonGetString(args[0], String.valueOf(args[1]), (String) args[2]);
            }
            case "json.getint" -> {
                return jsonGetInt(args[0], String.valueOf(args[1]), (Integer) args[2]);
            }
            case "json.getinteger" -> {
                return jsonGetInt(args[0], String.valueOf(args[1]), (Integer) args[2]);
            }
            case "json.getlong" -> {
                return jsonGetLong(args[0], String.valueOf(args[1]), (Long) args[2]);
            }
            case "json.getdouble" -> {
                return jsonGetDouble(args[0], String.valueOf(args[1]), (Double) args[2]);
            }
            case "json.getbool" -> {
                return jsonGetBool(args[0], String.valueOf(args[1]), (Boolean) args[2]);
            }
            case "json.getarray" -> {
                return jsonGetArray(args[0], String.valueOf(args[1]));
            }
            case "json.getobject" -> {
                return jsonGetObject(args[0], String.valueOf(args[1]));
            }
            case "json.set" -> {
                Object root = args[0];
                String path = (String) args[1];
                Object val = args[2];
                Json.setValue(root, path, val);
                return root;
            }
            case "json.setstrict" -> {
                Object root = args[0];
                String path = (String) args[1];
                Object val = args[2];
                Json.setValueStrict(root, path, val);
                return root;
            }
            case "json.remove" -> {
                Object root = args[0];
                String path = (String) args[1];
                Json.remove(root, path);
                return root;
            }
            case "json.add" -> {
                Object root = args[0];
                String arrPath = (String) args[1];
                Object val = args[2];
                Json.add(root, arrPath, val);
                return root;
            }
            case "json.insert" -> {
                Object root = args[0];
                String arrPath = (String) args[1];
                Number index = (Number) args[2];
                Object val = args[3];
                Json.insert(root, arrPath, index.intValue(), val);
                return root;
            }

            case "str.tostring", "string.tostring" -> {
                return (args[0] == null) ? null : String.valueOf(args[0]);
            }

            case "str.toupper" -> {
                String s = (String) args[0];
                return (s == null) ? null : s.toUpperCase();
            }

            case "str.tolower" -> {
                String s = (String) args[0];
                return (s == null) ? null : s.toLowerCase();
            }

            case "str.trim" -> {
                String s = (String) args[0];
                return (s == null) ? null : s.trim();
            }

            //  str.replace(source, target, replacement)  -- literal replace (NOT regex)
            case "str.replace" -> {
                String s = (String) args[0];
                String tgt = (String) args[1];
                String rep = (String) args[2];
                if (s == null || tgt == null || rep == null) {
                    return s;  // null-safe; no change
                }
                return Util.notNull(s).replace(tgt, rep);
            }

            //  str.split(text, regex) -> JSON (List<String>)
            case "str.split" -> {
                String s = (String) args[0];
                String pattern = (String) args[1];
                Integer limit = (Integer) args[2];
                if (limit == null) {
                    limit = -1;
                }
                if (s == null || pattern == null) {
                    return null; // follow other string builtins' null semantics
                }
                try {
                    // keep trailing empties
                    String[] parts = s.split(pattern, limit);
                    return new ArrayFixed(DataType.STRING, parts);
                } catch (java.util.regex.PatternSyntaxException ex) {
                    throw new InterpreterError("Invalid regex: " + ex.getDescription());
                }
            }

            case "str.join" -> {
                Object a0 = args[0];
                String nl = (String) args[1];

                if (a0 == null) {
                    return null;
                }
                if (nl == null) {
                    throw new InterpreterError("str.join: delimiter cannot be null");
                }

                String[] sarray;

                if (a0 instanceof String[] sa) {
                    sarray = sa;
                    int n = sarray.length;
                    for (int i = 0; i < n; i++) {
                        if (sarray[i] == null) {
                            sarray[i] = "";
                        }
                    }
                } else if (a0 instanceof ArrayDef ad) {
                    // convert ArrayDef -> String[]
                    int n = ad.size();
                    sarray = new String[n];
                    for (int i = 0; i < n; i++) {
                        Object el = ad.get(i);
                        sarray[i] = (el == null) ? "" : el.toString();
                    }
                } else if (a0 instanceof List<?> list) {
                    sarray = list.stream().map(e -> e == null ? "" : e.toString()).toArray(String[]::new);
                } else {
                    throw new InterpreterError("str.join: first argument must be an array/list of strings");
                }

                return String.join(nl, sarray);
            }

            //  str.contains(s, sub)
            case "str.contains" -> {
                String s = (String) args[0], sub = (String) args[1];
                if (s == null || sub == null) {
                    return false;
                }
                return s.contains(sub);
            }

            //  str.startsWith(s, prefix)
            case "str.startswith" -> {
                String s = (String) args[0], p = (String) args[1];
                if (s == null || p == null) {
                    return false;
                }
                return s.startsWith(p);
            }

            //  str.endsWith(s, suffix)
            case "str.endswith" -> {
                String s = (String) args[0], suf = (String) args[1];
                if (s == null || suf == null) {
                    return false;
                }
                return s.endsWith(suf);
            }

            //  str.equalsIgnoreCase(a, b)
            case "str.equalsignorecase" -> {
                String a = (String) args[0], b = (String) args[1];
                return Util.strEqIgnore(a, b);
            }

            //  str.equals(a, b)
            case "str.equals" -> {
                String a = (String) args[0], b = (String) args[1];
                return Util.strEq(a, b);
            }

            //  str.isEmpty(s)
            case "str.isempty" -> {
                String s = (String) args[0];
                return s != null && s.isEmpty();
            }

            //  str.isBlank(s) : true for null or only-whitespace
            case "str.isblank" -> {
                String s = (String) args[0];
                return Util.isBlank(s);
            }
            
            //  str.substring(s, beginIndex, endIndex?) -> STRING
            //  Extract substring from string. If endIndex is not provided, extracts to end of string.
            //  Mimics Java String.substring() semantics.
            case "str.substring" -> {
                String s = (String) args[0];
                Integer beginIndex = (Integer) args[1];
                Integer endIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;
                
                if (s == null) {
                    return null;
                }
                if (beginIndex == null) {
                    throw new InterpreterError("str.substring: beginIndex cannot be null");
                }
                
                try {
                    if (endIndex == null) {
                        return s.substring(beginIndex);
                    } else {
                        return s.substring(beginIndex, endIndex);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    throw new InterpreterError("str.substring: " + ex.getMessage());
                }
            }
            
            //  str.indexOf(s, searchString, fromIndex?) -> INTEGER
            //  Find first occurrence of searchString in s, starting from fromIndex (default 0).
            //  Returns -1 if not found. Mimics Java String.indexOf() semantics.
            case "str.indexof" -> {
                String s = (String) args[0];
                String searchString = (String) args[1];
                Integer fromIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;
                
                if (s == null || searchString == null) {
                    return -1; // null-safe behavior: return -1 if either is null
                }
                
                try {
                    if (fromIndex == null) {
                        return s.indexOf(searchString);
                    } else {
                        return s.indexOf(searchString, fromIndex);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    throw new InterpreterError("str.indexOf: " + ex.getMessage());
                }
            }
            
            //  str.lastIndexOf(s, searchString, fromIndex?) -> INTEGER
            //  Find last occurrence of searchString in s, searching backwards from fromIndex.
            //  Returns -1 if not found. Mimics Java String.lastIndexOf() semantics.
            case "str.lastindexof" -> {
                String s = (String) args[0];
                String searchString = (String) args[1];
                Integer fromIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;
                
                if (s == null || searchString == null) {
                    return -1; // null-safe behavior: return -1 if either is null
                }
                
                try {
                    if (fromIndex == null) {
                        return s.lastIndexOf(searchString);
                    } else {
                        return s.lastIndexOf(searchString, fromIndex);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    throw new InterpreterError("str.lastIndexOf: " + ex.getMessage());
                }
            }
            
            //  str.charAt(s, index) -> STRING
            //  Returns the character at the specified index as a single-character string.
            //  Mimics Java String.charAt() semantics.
            case "str.charat" -> {
                String s = (String) args[0];
                Integer index = (Integer) args[1];
                
                if (s == null) {
                    return null;
                }
                if (index == null) {
                    throw new InterpreterError("str.charAt: index cannot be null");
                }
                
                try {
                    char ch = s.charAt(index);
                    return String.valueOf(ch);
                } catch (IndexOutOfBoundsException ex) {
                    throw new InterpreterError("str.charAt: " + ex.getMessage());
                }
            }
            
            //  str.replaceAll(s, regex, replacement) -> STRING
            //  Replaces all occurrences matching regex pattern with replacement string.
            //  Mimics Java String.replaceAll() semantics (regex-based).
            case "str.replaceall" -> {
                String s = (String) args[0];
                String regex = (String) args[1];
                String replacement = (String) args[2];
                
                if (s == null) {
                    return null;
                }
                if (regex == null || replacement == null) {
                    throw new InterpreterError("str.replaceAll: regex and replacement cannot be null");
                }
                
                try {
                    return s.replaceAll(regex, replacement);
                } catch (java.util.regex.PatternSyntaxException ex) {
                    throw new InterpreterError("str.replaceAll: Invalid regex - " + ex.getMessage());
                }
            }
            
            //  str.lpad(s, length, padChar) -> STRING
            //  Left-pads the string with padChar to reach the specified length.
            //  If string is already >= length, returns original string unchanged.
            case "str.lpad" -> {
                String s = (String) args[0];
                Integer length = (Integer) args[1];
                String padChar = (String) args[2];
                
                if (s == null) {
                    return null;
                }
                if (length == null) {
                    throw new InterpreterError("str.lpad: length cannot be null");
                }
                if (padChar == null || padChar.isEmpty()) {
                    throw new InterpreterError("str.lpad: padChar cannot be null or empty");
                }
                if (padChar.length() != 1) {
                    throw new InterpreterError("str.lpad: padChar must be a single character");
                }
                
                // If already at or exceeds desired length, return as-is
                if (s.length() >= length) {
                    return s;
                }
                
                // Calculate padding needed
                int padCount = length - s.length();
                StringBuilder sb = new StringBuilder(length);
                char ch = padChar.charAt(0);
                for (int i = 0; i < padCount; i++) {
                    sb.append(ch);
                }
                sb.append(s);
                return sb.toString();
            }
            
            //  str.rpad(s, length, padChar) -> STRING
            //  Right-pads the string with padChar to reach the specified length.
            //  If string is already >= length, returns original string unchanged.
            case "str.rpad" -> {
                String s = (String) args[0];
                Integer length = (Integer) args[1];
                String padChar = (String) args[2];
                
                if (s == null) {
                    return null;
                }
                if (length == null) {
                    throw new InterpreterError("str.rpad: length cannot be null");
                }
                if (padChar == null || padChar.isEmpty()) {
                    throw new InterpreterError("str.rpad: padChar cannot be null or empty");
                }
                if (padChar.length() != 1) {
                    throw new InterpreterError("str.rpad: padChar must be a single character");
                }
                
                // If already at or exceeds desired length, return as-is
                if (s.length() >= length) {
                    return s;
                }
                
                // Calculate padding needed
                int padCount = length - s.length();
                StringBuilder sb = new StringBuilder(length);
                sb.append(s);
                char ch = padChar.charAt(0);
                for (int i = 0; i < padCount; i++) {
                    sb.append(ch);
                }
                return sb.toString();
            }
            
            //  str.charArray(s) -> ARRAY (int[])
            //  Returns an array of integer character codes (Unicode code points) for each character in the string.
            //  Each character is converted to its integer Unicode value.
            case "str.chararray" -> {
                String s = (String) args[0];
                
                if (s == null) {
                    return null;
                }
                
                // Create an integer array with the character codes
                int length = s.length();
                Integer[] charCodes = new Integer[length];
                
                for (int i = 0; i < length; i++) {
                    charCodes[i] = (int) s.charAt(i);
                }
                
                // Wrap in an ArrayFixed to return as an EBS array
                ArrayFixed result = new ArrayFixed(DataType.INTEGER, charCodes);
                return result;
            }
            
            case "file.exists" -> {
                return BuiltinsFile.exists(env, args);
            }
            case "file.size" -> {
                return BuiltinsFile.size(env, args);
            }
            //  file.open(path, mode?) -> STRING handle
            //  mode: "r", "w", "a", "rw"   (default "r")
            case "file.open" -> {
                return BuiltinsFile.fileOpen(env, args);
            }
            //  file.close(handleOrPath) -> BOOL
            case "file.close" -> {
                return BuiltinsFile.fileClose(env, args);
            }

            //  file.openFiles() -> JSON
            //  Returns: List<Map<String,Object>> with fields:
            //  handle, path, mode, size, position, isOpen, openedMs
            case "file.listopenfiles" -> {
                return BuiltinsFile.listOpenFiles(env, args);
            }
            //  file.readln(handleOrPath, encoding?) -> STRING | null
            //  Returns the next text line (without EOL), or null if EOF is reached.
            //  Default encoding = UTF-8.
            case "file.readln" -> {
                return BuiltinsFile.readln(env, args);
            }
            //  file.writeln(handleOrPath, text, encoding?) -> BOOL
            //  Writes text + '\n' using the specified encoding (default UTF-8).
            //  Requires a write-capable handle ("w", "a", or "rw").
            case "file.writeln" -> {
                return BuiltinsFile.writeln(env, args);
            }
            //  file.read(handleOrPath, encoding?) -> STRING | null
            //  Returns all text (without EOL), or null if EOF is reached.
            //  Default encoding = UTF-8.
            case "file.read" -> {
                return BuiltinsFile.read(env, args);
            }
            //  file.write(handleOrPath, text, encoding?) -> BOOL
            //  Writes text using the specified encoding (default UTF-8).
            //  Requires a write-capable handle ("w", "a", or "rw").
            case "file.write" -> {
                return BuiltinsFile.write(env, args);
            }
            //  file.eof(handleOrPath) -> BOOL
            //  True if channel position >= channel size, else false.
            case "file.eof" -> {
                return BuiltinsFile.eof(env, args);
            }
            //  string.readTextFile(path) -> STRING (UTF-8)
            case "file.readtextfile" -> {
                return BuiltinsFile.readTextFile(env, args);
            }
            case "file.readbinfile" -> {
                return BuiltinsFile.readBinFile(env, args);
            }
            //  string.writeTextFile(path, content) -> BOOL (UTF-8)
            case "file.writetextfile" -> {
                return BuiltinsFile.writeTextFile(env, args);
            }
            case "file.writebinfile" -> {
                return BuiltinsFile.writeBinFile(env, args);
            }
            case "file.appendtotextfile" -> {
                return BuiltinsFile.appendToTextFile(env, args);
            }
            case "file.listfiles" -> {
                return BuiltinsFile.listFiles(env, args);
            }
            case "file.rename" -> {
                return BuiltinsFile.rename(env, args);
            }
            //  file.move(source, dest) -> BOOL
            case "file.move" -> {
                return BuiltinsFile.move(env, args);
            }
            //  file.copy(source, dest) -> BOOL
            case "file.copy" -> {
                return BuiltinsFile.copy(env, args);
            }
            // file.openZip(path, mode?) -> STRING handle
            case "file.openzip" -> {
                return BuiltinsFile.openZip(env, args);
            }
            case "file.createzip" -> {
                return BuiltinsFile.createZip(env, args);
            }
            case "file.listzipfiles" -> {
                return BuiltinsFile.listZipFiles(env, args);
            }
            case "file.unzip" -> {
                return BuiltinsFile.unzip(env, args);
            }
            // file.closeZip(handleOrPath) -> BOOL
            case "file.closezip" -> {
                return BuiltinsFile.closeZip(env, args);
            }
            //  http.request(url, method?, body?, headers?, timeoutMs?) -> JSON
            //  Returns: { status:int, headers:Map<String,String>, body:String, url:String, elapsedMs:long }
            case "http.request" -> {
                String url = (String) args[0];
                String method = (String) (args.length > 1 ? args[1] : null);
                String body = (String) (args.length > 2 ? args[2] : null);
                Object h = (args.length > 3 ? args[3] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 4 ? args[4] : null);
                try {
                    return Util.httpRequest(url, method, body, headers, timeout);
                } catch (Exception ex) {
                    throw new InterpreterError("request -> " + ex.getMessage());
                }
            }
            //  http.get(url, headers?, timeoutMs?) -> JSON envelope
            case "http.get" -> {
                String url = (String) args[0];
                Object h = (args.length > 1 ? args[1] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 2 ? args[2] : null);
                try {
                    return Util.httpRequest(url, "GET", null, headers, timeout);
                } catch (Exception ex) {
                    throw new InterpreterError("get -> " + ex.getMessage());
                }
            }

            //  http.post(url, body?, headers?, timeoutMs?) -> JSON envelope
            case "http.post" -> {
                String url = (String) args[0];
                String body = (String) (args.length > 1 ? args[1] : null);
                Object h = (args.length > 2 ? args[2] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 3 ? args[3] : null);
                try {
                    return Util.httpRequest(url, "POST", body, headers, timeout);
                } catch (Exception ex) {
                    throw new InterpreterError("post -> " + ex.getMessage());
                }
            }

            //  http.getText(url, headers?, timeoutMs?) -> STRING body
            case "http.gettext" -> {
                String url = (String) args[0];
                Object h = (args.length > 1 ? args[1] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 2 ? args[2] : null);
                try {
                    Map<String, Object> res = Util.httpRequest(url, "GET", null, headers, timeout);
                    return (String) res.get("body");
                } catch (Exception ex) {
                    throw new InterpreterError("getText -> " + ex.getMessage());
                }
            }

            //  http.postText(url, body, headers?, timeoutMs?) -> STRING body
            case "http.posttext" -> {
                String url = (String) args[0];
                String body = (String) args[1];
                Object h = (args.length > 2 ? args[2] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 3 ? args[3] : null);
                try {
                    Map<String, Object> res = Util.httpRequest(url, "POST", body, headers, timeout);
                    return (String) res.get("body");
                } catch (Exception ex) {
                    throw new InterpreterError("postText -> " + ex.getMessage());
                }
            }

            //  http.getJson(url, headers?, timeoutMs?) -> JSON parsed body
            case "http.getjson" -> {
                String url = (String) args[0];
                Object h = (args.length > 1 ? args[1] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 2 ? args[2] : null);
                String body = null;
                try {
                    Map<String, Object> res = Util.httpRequest(url, "GET", null, headers, timeout);
                    body = (String) res.get("body");
                } catch (Exception ex) {
                    throw new InterpreterError("getJson -> " + ex.getMessage());
                }
                if (body == null || body.isEmpty()) {
                    return null;
                }
                try {
                    return Json.parse(body);
                } catch (RuntimeException ex) {
                    throw new InterpreterError("http.getJson: response is not valid JSON (" + ex.getMessage() + ")");
                }
            }

            //  http.postJson(url, jsonBody, headers?, timeoutMs?) -> JSON parsed body
            case "http.postjson" -> {
                String url = (String) args[0];
                Object jsonBody = args[1];  // DataType.JSON: Map/List/String/Number/Boolean/null
                Object h = (args.length > 2 ? args[2] : null);
                Map headers = null;
                if (h instanceof Map m) {
                    headers = m;
                }
                Number timeout = (Number) (args.length > 3 ? args[3] : null);
                // Merge/ensure headers (Content-Type/Accept)
                java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
                if (headers instanceof java.util.Map<?, ?> hm) {
                    for (var e : hm.entrySet()) {
                        if (e.getKey() != null) {
                            merged.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }
                merged.putIfAbsent("Content-Type", "application/json; charset=utf-8");
                merged.putIfAbsent("Accept", "application/json");

                // Serialize JSON body: if it's already a String, pass as-is; else Compact JSON
                String body;
                if (jsonBody == null) {
                    body = "";
                } else if (jsonBody instanceof String s) {
                    body = s;
                } else {
                    body = Json.compactJson(jsonBody);
                }
                String resp = null;
                try {
                    Map<String, Object> res = Util.httpRequest(url, "POST", body, merged, timeout);
                    resp = (String) res.get("body");
                } catch (Exception ex) {
                    throw new InterpreterError("postJson -> " + ex.getMessage());
                }
                if (resp == null || resp.isEmpty()) {
                    return null;
                }
                try {
                    return Json.parse(resp);
                } catch (RuntimeException ex) {
                    throw new InterpreterError("http.postJson: response is not valid JSON (" + ex.getMessage() + ")");
                }
            }
            //  http.ensure2xx(statusOrEnvelope, message?) -> JSON
            //  - Accepts either a numeric status (e.g., 404) or an envelope map {status, headers, body, ...}.
            //  - Throws with the provided message (or a default) if not in [200..299].
            //  - Returns the original input (envelope map or the status number) if ok.
            case "http.ensure2xx" -> {
                Object in = args[0];
                String msg = (args.length > 1 ? (String) args[1] : null);

                int status = httpExtractStatus(in);
                boolean ok = status >= 200 && status <= 299;

                if (!ok) {
                    String base = "HTTP request failed with status " + status;
                    throw new InterpreterError((msg == null || msg.isBlank()) ? base : (base + " : " + msg));
                }
                // Return what we got (envelope map or status number)
                return status;
            }

            //  http.is2xx(statusOrEnvelope) -> BOOL  (optional helper)
            case "http.is2xx" -> {
                Object in = args[0];
                int status = httpExtractStatus(in);
                return (status >= 200 && status <= 299);
            }
// --------------------- DEBUG ---------------------

            case "debug.on" -> {
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug ON");
                }
                return debug.setDebugOn();
            }

            case "debug.off" -> {
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug OFF");
                }
                return debug.setDebugOff();
            }

            case "echo.on" -> {
                env.setEcho(true);
                sysOutput(output, "Echo ON");
                return null;
            }

            case "echo.off" -> {
                env.setEcho(false);
                sysOutput(output, "Echo OFF");
                return null;
            }

            case "debug.traceon" -> {
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug Trace ON");
                }
                return debug.setDebugTraceOn();
            }

            case "debug.traceoff" -> {
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug Trace OFF");
                }
                return debug.setDebugTraceOff();
            }

            case "debug.file" -> {
                // args[0] is file name; null/blank means "stdout"
                String fileName = (String) args[0];
                debug.setDebugFilePath(fileName);
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug file path : " + debug.getDebugFilePath());
                }
                return null; // declared as "void" (null) in registry
            }

            case "debug.newfile" -> {
                // args[0] is file name; null/blank means "stdout"
                String fileName = (String) args[0];
                debug.setDebugNewFilePath(fileName);
                if (env.isEchoOn()) {
                    sysOutput(output, "Debug new file path : " + debug.getDebugFilePath());
                }
                return null; // declared as "void" (null) in registry
            }

            case "debug.log" -> {
                debug.debugWriteLine((String) args[0], (String) args[1]);
                return debug.isDebugOn();
            }

            case "debug.assert" -> {
                Boolean condition = (Boolean) args[0];
                String message = (String) args[1];
                boolean ok = (condition != null && condition);
                String m = null;
                if (debug.isDebugOn()) {
                    if (!ok) {
                        m = (message == null ? "Assertion FAILED " : message);
//                    throw new RuntimeException(m);
                    } else {
                        m = "Assertion SUCCESS";
                    }
                    debug.debugWriteLine("ASSERT", m);
                }
                return ok;
            }

            case "debug.assertequals" -> {
                Object expected = args[0];
                Object actual = args[1];
                String message = (String) args[2];

                boolean equal = java.util.Objects.equals(expected, actual); // deep for Map/List
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
//                    throw new RuntimeException(m);
                }
                return equal;
            }

            case "debug.vars" -> {
                // Returns JSON (Map<String,Object>) of current variables if wired by the interpreter,
                // else an empty object.
                return (VARS_SUPPLIER != null) ? VARS_SUPPLIER.get()
                        : java.util.Map.of();
            }

            case "debug.stack" -> {
                // Returns JSON (List<Object>) with frames if wired by the interpreter,
                // else an empty array.
                return (STACK_SUPPLIER != null) ? STACK_SUPPLIER.get()
                        : java.util.List.of();
            }

            case "array.expand" -> {
                if (args[0] instanceof ArrayDef array) {
                    int len = (Integer) args[1];
                    array.expandArray(len);
                }
                return null;
            }

            case "array.sort" -> {
                if (args[0] instanceof ArrayDef array) {
                    boolean ascen = true;
                    if (args[1] instanceof Boolean val) {
                        ascen = val;
                    }
                    array.sortArray(ascen);
                }
                return null;
            }

            case "array.fill" -> {
                if (args[0] instanceof ArrayDef array) {
                    int len = (Integer) args[1];
                    array.fillArray(len, args[2]);
                }
                return null;
            }
            case "array.base64encode" -> {
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
                    // Convert a numeric array (0..255) to bytes
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

            case "array.base64decode" -> {
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
            case "system.command" -> {
                // 0: command (String) [required]
                // 1: args (List<?> or null) [optional]
                // 2: timeoutMs (Long or null) [optional]
                // 3: cwd (String or null) [optional]
                final ArrayDef cmdArgs = (args[1] instanceof ArrayDef) ? (ArrayDef) args[1] : null;
                final Long timeoutMs = (args.length > 2 && args[2] instanceof Long) ? (Long) args[2] : 60000L;
                final String cwd = (args.length > 3 && args[3] instanceof String) ? (String) args[3] : null;
                return runCmd((String) args[0], cmdArgs, timeoutMs, cwd);
            }

            case "system.wincommand" -> {
                // 0: command (String) [required]
                // 1: args (List<?> or null) [optional]
                // 2: timeoutMs (Long or null) [optional]
                // 3: cwd (String or null) [optional]
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
            case "system.getproperty" -> {
                final String key = (String) args[0];
                final String def = (args.length > 1 && args[1] instanceof String)
                        ? (String) args[1] : null;
                // def==null -> use 1-arg getter; else use 2-arg overload
                return (def == null)
                        ? java.lang.System.getProperty(key)
                        : java.lang.System.getProperty(key, def);
            }

            case "system.setproperty" -> {
                final String key = (String) args[0];
                final String val = (args.length > 1 && args[1] instanceof String)
                        ? (String) args[1] : null;
                // If value is null, clear the property (returns previous)
                return (val == null)
                        ? java.lang.System.clearProperty(key)
                        : java.lang.System.setProperty(key, val);
            }

            case "sleep" -> {
                // Sleep for specified milliseconds
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
                return ""; // Return empty string (DataType.STRING)
            }

            case "debug.memusage" -> {
                // Optional unit: "MB" (default), "KB", "B"
                final String unit = (args.length > 0 && args[0] instanceof String u && !u.isBlank()) ? u : "MB";

                final long max = Runtime.getRuntime().maxMemory();
                final long total = Runtime.getRuntime().totalMemory();
                final long free = Runtime.getRuntime().freeMemory();
                final long used = total - free;

                long div;
                switch (unit.toUpperCase()) {
                    case "KB" ->
                        div = 1024L;
                    case "B" ->
                        div = 1L;
                    default ->
                        div = 1024L * 1024L; // MB
                }

                final double maxU = max / (double) div;
                final double totalU = total / (double) div;
                final double freeU = free / (double) div;
                final double usedU = used / (double) div;

                final String msg = String.format(
                        "mem: used=%.2f %s, free=%.2f %s, total=%.2f %s, max=%.2f %s",
                        usedU, unit, freeU, unit, totalU, unit, maxU, unit
                );

                // Print via Debugger if debug is enabled, otherwise to stdout
                if (debug != null && debug.isDebugOn()) {
                    debug.debugWriteLine("DEBUG", msg);  // prints to stdout or debug file, per Debugger config
                } else {
                    if (env.isEchoOn()) {
                        System.out.println(msg);
                    }
                }

                // Also return a JSON-style envelope for programmatic use
                final java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
                out.put("max", maxU);
                out.put("total", totalU);
                out.put("free", freeU);
                out.put("used", usedU);
                out.put("unit", unit.toUpperCase());
                return out;
            }

            case "system.help" -> {
                // Check if a specific keyword/builtin was requested
                String keyword = args.length > 0 && args[0] != null ? args[0].toString() : null;

                if (keyword != null && !keyword.isEmpty()) {
                    // Return detailed help for the specific keyword/builtin
                    return getDetailedHelp(keyword);
                }

                // --- Collect keywords/operators/datatypes directly from Token.Type ---
                final java.util.Set<String> keywords = new java.util.TreeSet<>();
                final java.util.Set<String> operators = new java.util.TreeSet<>();
                final java.util.Set<String> datatypes = new java.util.TreeSet<>();
                for (EbsTokenType t : EbsTokenType.values()) {
                    // Keyword tokens (include synonyms, e.g. "break","exit")
                    if (t.getCategory() == Category.KEYWORD) {
                        for (String s : t.getStrings()) {
                            if (s != null && !s.isEmpty()) {
                                keywords.add(s);
                            }
                        }
                    }
                    // Operator tokens (include synonyms, e.g. "and","&&")
                    if (t.getCategory() == Category.OPERATOR) {
                        for (String s : t.getStrings()) {
                            if (s != null && !s.isEmpty()) {
                                operators.add(s);
                            }
                        }
                    }
                    // Datatype tokens (int, long, float, double, string, date, bool/boolean, json)
                    if (t.getDataType() != null) {
                        for (String s : t.getStrings()) {
                            if (s != null && !s.isEmpty()) {
                                datatypes.add(s);
                            }
                        }
                    }
                }

                // --- Collect builtins (name, returnType, parameters) ---
                final List<java.util.Map<String, Object>> builtins = new java.util.ArrayList<>();
                for (String bname : NAMES) { // registry is lowercase
                    BuiltinInfo info = getBuiltinInfo(bname);
                    java.util.Map<String, Object> bi = new java.util.LinkedHashMap<>();
                    bi.put("name", info.name);
                    bi.put("returnType", (info != null && info.returnType != null) ? info.returnType.name() : null);

                    List<java.util.Map<String, Object>> params = new java.util.ArrayList<>();
                    if (info != null && info.params != null) {
                        for (com.eb.script.interpreter.statement.Parameter p : info.params) {
                            java.util.Map<String, Object> pm = new java.util.LinkedHashMap<>();
                            pm.put("name", p.name);
                            pm.put("type", p.paramType == null ? null : p.paramType.name());
                            pm.put("mandatory", p.mandatory);
                            params.add(pm);
                        }
                    }
                    bi.put("params", params);
                    builtins.add(bi);
                }

                // --- Human-readable help text ---
                final StringBuilder sb = new StringBuilder();
                sb.append("Keywords:\n");
                for (String k : keywords) {
                    sb.append("  ").append(k).append('\n');
                }
                sb.append('\n').append("Datatypes:\n");
                for (String d : datatypes) {
                    sb.append("  ").append(d).append('\n');
                }
                sb.append('\n').append("Operators:\n");
                for (String o : operators) {
                    sb.append("  ").append(o).append('\n');
                }
                sb.append('\n').append("Builtins:\n");
                for (var bi : builtins) {
                    sb.append("  ").append(bi.get("name")).append("(");
                    @SuppressWarnings("unchecked")
                    List<java.util.Map<String, Object>> ps = (List<java.util.Map<String, Object>>) bi.get("params");
                    if (ps != null) {
                        for (int i = 0; i < ps.size(); i++) {
                            var pm = ps.get(i);
                            String pname = (String) pm.get("name");
                            String ptype = (String) pm.get("type");
                            boolean mand = Boolean.TRUE.equals(pm.get("mandatory"));
                            boolean arr = Boolean.TRUE.equals(pm.get("array"));
                            sb.append(pname).append(":").append(ptype == null ? "any" : ptype.toLowerCase());
                            if (arr) {
                                sb.append("[]");
                            }
                            if (!mand) {
                                sb.append("?");
                            }
                            if (i < ps.size() - 1) {
                                sb.append(", ");
                            }
                        }
                    }
                    String returnType = (String) bi.get("returnType");
                    sb.append(") : ").append(returnType == null ? "null" : returnType.toLowerCase()).append('\n');
                }
                final String text = sb.toString();

                // Print via Debugger (if enabled) or to stdout
                if (debug != null && debug.isDebugOn()) {
                    for (String line : text.split("\\r?\\n")) {
                        if (!line.isEmpty()) {
                            debug.debugWriteLine("INFO", line);
                        }
                    }
                } else {
                    if (env.isEchoOn()) {
                        System.out.print(text);
                    }
                }

                // --- JSON envelope to return to the script ---
                //                final java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                //                result.put("keywords", new java.util.ArrayList<>(keywords));
                //                result.put("datatypes", new java.util.ArrayList<>(datatypes));
                //                result.put("operators", new java.util.ArrayList<>(operators));
                //                result.put("builtins", builtins);
                return text;
            }
            
            // --- system.inputDialog(title, headerText?, defaultValue?) ---
            case "system.inputdialog" -> {
                String title = args.length > 0 && args[0] != null ? args[0].toString() : "Input";
                String headerText = args.length > 1 && args[1] != null ? args[1].toString() : null;
                String defaultValue = args.length > 2 && args[2] != null ? args[2].toString() : "";
                
                // Use JavaFX TextInputDialog
                final java.util.concurrent.atomic.AtomicReference<String> resultRef = new java.util.concurrent.atomic.AtomicReference<>("");
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(defaultValue);
                    dialog.setTitle(title);
                    if (headerText != null && !headerText.isEmpty()) {
                        dialog.setHeaderText(headerText);
                    }
                    dialog.setContentText("Enter value:");
                    
                    java.util.Optional<String> result = dialog.showAndWait();
                    resultRef.set(result.orElse(""));
                    latch.countDown();
                });
                
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterpreterError("system.inputDialog interrupted: " + e.getMessage());
                }
                return resultRef.get();
            }
            
            // --- system.confirmDialog(message, title?, headerText?) ---
            case "system.confirmdialog" -> {
                String message = args.length > 0 && args[0] != null ? args[0].toString() : "Are you sure?";
                String title = args.length > 1 && args[1] != null ? args[1].toString() : "Confirm";
                String headerText = args.length > 2 && args[2] != null ? args[2].toString() : null;
                
                final java.util.concurrent.atomic.AtomicBoolean resultRef = new java.util.concurrent.atomic.AtomicBoolean(false);
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        message,
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO
                    );
                    confirm.setTitle(title);
                    if (headerText != null && !headerText.isEmpty()) {
                        confirm.setHeaderText(headerText);
                    }
                    
                    java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
                    resultRef.set(result.isPresent() && result.get() == javafx.scene.control.ButtonType.YES);
                    latch.countDown();
                });
                
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterpreterError("system.confirmDialog interrupted: " + e.getMessage());
                }
                return resultRef.get();
            }
            
            // --- system.alertDialog(message, title?, alertType?) ---
            case "system.alertdialog" -> {
                String message = args.length > 0 && args[0] != null ? args[0].toString() : "";
                String title = args.length > 1 && args[1] != null ? args[1].toString() : "Alert";
                String alertTypeStr = args.length > 2 && args[2] != null ? args[2].toString().toLowerCase() : "info";
                
                javafx.scene.control.Alert.AlertType alertType;
                switch (alertTypeStr) {
                    case "warning" -> alertType = javafx.scene.control.Alert.AlertType.WARNING;
                    case "error" -> alertType = javafx.scene.control.Alert.AlertType.ERROR;
                    default -> alertType = javafx.scene.control.Alert.AlertType.INFORMATION;
                }
                
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(alertType, message);
                    alert.setTitle(title);
                    alert.showAndWait();
                    latch.countDown();
                });
                
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterpreterError("system.alertDialog interrupted: " + e.getMessage());
                }
                return true;
            }
            
            // --- AI: ai.complete(system?, user, maxTokens?, temperature?) ---
            case "ai.complete" -> {
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

            // --- AI: ai.summarize(text, maxTokens?) ---
            case "ai.summarize" -> {
                String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
                Integer maxT = args.length > 1 && args[1] != null ? ((Number) args[1]).intValue() : null;
                try {
                    return AiFunctions.summarize(text, maxT);
                } catch (Exception e) {
                    throw new InterpreterError("ai.summarize failed: " + e.getMessage());
                }
            }

            // --- AI: ai.embed(text) -> JSON array of numbers ---
            case "ai.embed" -> {
                String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
                try {
                    double[] vec = AiFunctions.embed(text);
                    ArrayDef out = new ArrayDynamic(DataType.DOUBLE);
                    for (double v : vec) {
                        out.add(v);
                    }
                    return out; // your runtime serializes this JSON-friendly structure
                } catch (Exception e) {
                    throw new InterpreterError("ai.embed failed: " + e.getMessage());
                }
            }

            // --- AI: ai.classify(text, labels[]) ---
            case "ai.classify" -> {
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
            // --- ClassTree functions ---
            case "classtree.generate" -> {
                return BuiltinsFile.generateClassTree(env, args);
            }
            case "classtree.scan" -> {
                return BuiltinsFile.scanClassTree(env, args);
            }

            // --- Screen property functions ---
            case "scr.setproperty" -> {
                return BuiltinsScreen.screenSetProperty(context, args);
            }
            case "scr.getproperty" -> {
                return BuiltinsScreen.screenGetProperty(context, args);
            }
            case "scr.getitemlist" -> {
                return BuiltinsScreen.screenGetItemList(context, args);
            }
            case "scr.getscreenitemlist" -> {
                return BuiltinsScreen.screenGetScreenItemList(context, args);
            }
            case "scr.showscreen" -> {
                return BuiltinsScreen.screenShow(context, args);
            }
            case "scr.hidescreen" -> {
                return BuiltinsScreen.screenHide(context, args);
            }
            case "scr.closescreen" -> {
                return BuiltinsScreen.screenClose(context, args);
            }
            case "scr.setstatus" -> {
                return BuiltinsScreen.screenSetStatus(context, args);
            }
            case "scr.getstatus" -> {
                return BuiltinsScreen.screenGetStatus(context, args);
            }
            case "scr.seterror" -> {
                return BuiltinsScreen.screenSetError(context, args);
            }
            case "scr.geterror" -> {
                return BuiltinsScreen.screenGetError(context, args);
            }
            case "scr.getitemsource" -> {
                return BuiltinsScreen.screenGetItemSource(context, args);
            }
            case "scr.setitemsource" -> {
                return BuiltinsScreen.screenSetItemSource(context, args);
            }
            case "scr.getitemstatus" -> {
                return BuiltinsScreen.screenGetItemStatus(context, args);
            }
            case "scr.resetitemoriginalvalue" -> {
                return BuiltinsScreen.screenResetItemOriginalValue(context, args);
            }
            case "scr.checkchanged" -> {
                return BuiltinsScreen.screenCheckChanged(context, args);
            }
            case "scr.checkerror" -> {
                return BuiltinsScreen.screenCheckError(context, args);
            }
            case "scr.revert" -> {
                return BuiltinsScreen.screenRevert(context, args);
            }
            case "scr.clear" -> {
                return BuiltinsScreen.screenClear(context, args);
            }
            case "scr.getvarreference" -> {
                return BuiltinsScreen.screenGetVarReference(context, args);
            }

            default ->
                throw new InterpreterError("Unknown builtin: " + name);
        }
    }

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

    @SuppressWarnings("unchecked")
    private static int httpExtractStatus(Object statusOrEnvelope) {
        if (statusOrEnvelope == null) {
            throw new RuntimeException("http status is null");
        }
        if (statusOrEnvelope instanceof Number n) {
            return n.intValue();
        }
        if (statusOrEnvelope instanceof java.util.Map<?, ?> m) {
            Object s = m.get("status");
            if (s instanceof Number n) {
                return n.intValue();
            }
            throw new RuntimeException("http.*: envelope missing numeric 'status'");
        }
        throw new RuntimeException("http.*: expected status number or response envelope");
    }
    // 0: command (String) [required]
    // 1: args (List<?> or null) [optional]
    // 2: timeoutMs (Long or null) [optional]
    // 3: cwd (String or null) [optional]

    private static Map<String, Object> runCmd(String command, ArrayDef args, Long timeoutMs, String cwd) {
        // Build full command line without using a shell
        final List<String> cmd = new ArrayList<>();
        cmd.add(command);
        if (args != null && args.size() > 1) {
            for (Object o : args) {
                cmd.add(String.valueOf(o));
            }
        }

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null && !cwd.isBlank()) {
            pb.directory(new java.io.File(cwd));
        }
        // Keep streams separate so we can report both:
        pb.redirectErrorStream(false);

        try {
            final Process p = pb.start();

            // Drain stdout/stderr concurrently to avoid buffer deadlocks:
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

            // Ensure both streams are consumed (bounded by timeout):
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

    private static void sysOutput(ScriptArea output, String message) {
        if (output != null) {
            output.println(message);
        } else {
            System.out.println(message);
        }
    }

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

    /**
     * Get detailed help for a specific keyword or builtin. Returns formatted
     * help text similar to /help <keyword> command.
     */
    private static String getDetailedHelp(String itemName) {
        try {
            // Load help-lookup.json from resources
            java.io.InputStream is = Builtins.class.getClassLoader().getResourceAsStream("help-lookup.json");
            if (is == null) {
                return "Help system not available (help-lookup.json not found)";
            }

            String jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            is.close();

            // Parse JSON
            Map<String, Object> lookup = (Map<String, Object>) Json.parse(jsonContent);

            // Search in keywords first
            ArrayDynamic keywords = (ArrayDynamic) lookup.get("keywords");
            if (keywords != null) {
                for (Object keywordObj : keywords) {
                    Map<String, Object> keyword = (Map<String, Object>) keywordObj;
                    String kwName = (String) keyword.get("keyword");
                    if (kwName != null && kwName.equalsIgnoreCase(itemName)) {
                        return formatHelpEntry(kwName, keyword, "Keyword");
                    }
                }
            }

            // Search in builtins
            ArrayDynamic builtins = (ArrayDynamic) lookup.get("builtins");
            if (builtins != null) {
                for (Object builtinObj : builtins) {
                    Map<String, Object> builtin = (Map<String, Object>) builtinObj;
                    String funcName = (String) builtin.get("function");
                    if (funcName != null && funcName.equalsIgnoreCase(itemName)) {
                        return formatHelpEntry(funcName, builtin, "Builtin Function");
                    }
                }
            }

            // Not found
            return "No help found for: " + itemName + "\nUse system.help() or /help keywords to see all available keywords and builtins.";

        } catch (Exception ex) {
            return "Error loading help: " + ex.getMessage();
        }
    }

    /**
     * Format a help entry as plain text (similar to displayHelpEntry but
     * returns string).
     */
    private static String formatHelpEntry(String name, Map<String, Object> entry, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(type).append(": ").append(name).append("\n");
        sb.append("═══════════════════════════════════════════════════════════\n");

        String shortDesc = (String) entry.get("short_description");
        if (shortDesc != null && !shortDesc.isEmpty()) {
            sb.append("Description: ").append(shortDesc).append("\n\n");
        }

        // For builtins, show parameters and return type
        if (entry.containsKey("parameters")) {
            ArrayDynamic params = (ArrayDynamic) entry.get("parameters");
            if (params != null && !params.isEmpty()) {
                StringBuilder paramStr = new StringBuilder();
                boolean first = true;
                for (Object param : params) {
                    if (!first) {
                        paramStr.append(", ");
                    }
                    if (param instanceof Map) {
                        Map<String, Object> paramMap = (Map<String, Object>) param;
                        String paramName = (String) paramMap.get("name");
                        String paramType = (String) paramMap.get("type");
                        Boolean optional = (Boolean) paramMap.get("optional");
                        if (paramName != null) {
                            paramStr.append(paramName);
                            if (paramType != null && !paramType.isEmpty()) {
                                paramStr.append(":").append(paramType);
                            }
                            if (Boolean.TRUE.equals(optional)) {
                                paramStr.append("?");
                            }
                        }
                    } else {
                        paramStr.append(param.toString());
                    }
                    first = false;
                }
                sb.append("Parameters: ").append(paramStr.toString()).append("\n");
            }
        }

        if (entry.containsKey("return_type")) {
            String returnType = (String) entry.get("return_type");
            if (returnType != null && !returnType.isEmpty()) {
                sb.append("Returns: ").append(returnType).append("\n");
            }
        }

        if (entry.containsKey("parameters") || entry.containsKey("return_type")) {
            sb.append("\n");
        }

        String longHelp = (String) entry.get("long_help");
        if (longHelp != null && !longHelp.isEmpty()) {
            sb.append("Details:\n");
            sb.append(longHelp).append("\n\n");
        }

        String example = (String) entry.get("example");
        if (example != null && !example.isEmpty()) {
            sb.append("Example:\n");
            sb.append(example).append("\n");
        }

        sb.append("═══════════════════════════════════════════════════════════\n");
        return sb.toString();
    }
}
