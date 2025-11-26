package com.eb.script.interpreter;

import com.eb.script.json.Json;
import com.eb.script.json.JsonSchema;
import com.eb.script.json.JsonSchemaDeriver;
import com.eb.script.json.JsonValidate;
import com.eb.script.arrays.ArrayDef;
import java.util.Map;

/**
 * Built-in functions for JSON operations.
 * Handles all json.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsJson {

    /**
     * Dispatch a JSON builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "json.get")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "json.jsonfromstring" -> jsonFromString(args);
            case "json.derivescheme" -> deriveScheme(args);
            case "json.registerscheme" -> registerScheme(args);
            case "json.validate" -> validate(args);
            case "json.isempty" -> isEmpty(args);
            case "json.get" -> get(args);
            case "json.getstrict" -> getStrict(args);
            case "json.getstring" -> getString(args);
            case "json.getint", "json.getinteger" -> getInt(args);
            case "json.getlong" -> getLong(args);
            case "json.getdouble" -> getDouble(args);
            case "json.getbool" -> getBool(args);
            case "json.getarray" -> getArray(args);
            case "json.getobject" -> getObject(args);
            case "json.set" -> set(args);
            case "json.setstrict" -> setStrict(args);
            case "json.remove" -> remove(args);
            case "json.add" -> add(args);
            case "json.insert" -> insert(args);
            default -> throw new InterpreterError("Unknown JSON builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a JSON builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("json.");
    }

    // --- Individual builtin implementations ---

    private static Object jsonFromString(Object[] args) {
        Object jsonArg = args[0];
        if (jsonArg instanceof String val) {
            return Json.parse(val);
        } else {
            return null;
        }
    }

    private static Object deriveScheme(Object[] args) {
        Object jsonArg = args[0];
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

    private static Object registerScheme(Object[] args) throws InterpreterError {
        String schemaName = (String) args[0];
        Object schemaParam = args[1];

        final String schemaJson;
        if (schemaParam == null) {
            schemaJson = "{}";
        } else if (schemaParam instanceof String s) {
            schemaJson = s;
        } else {
            schemaJson = Json.compactJson(schemaParam);
        }

        JsonValidate.registerSchema(schemaName, schemaJson);
        return JsonValidate.getSchema(schemaName);
    }

    private static Object validate(Object[] args) throws InterpreterError {
        String schemaName = (String) args[0];
        Object jsonValue = args[1];

        Map<String, Object> schema = JsonValidate.getSchema(schemaName);
        if (schema == null) {
            throw new InterpreterError("json.validate: schema '" + schemaName + "' not registered");
        }

        Map<String, ArrayDef> errors = JsonSchema.validate(jsonValue, schema);
        return errors;
    }

    private static Object isEmpty(Object[] args) {
        return Json.isEmpty(args[0]);
    }

    private static Object get(Object[] args) {
        return Json.getValue(args[0], String.valueOf(args[1]));
    }

    private static Object getStrict(Object[] args) {
        return Json.getValueStrict(args[0], String.valueOf(args[1]));
    }

    private static Object getString(Object[] args) {
        return jsonGetAs(args[0], String.valueOf(args[1]), String.class, (String) args[2]);
    }

    private static Object getInt(Object[] args) {
        Number n = jsonGetAs(args[0], String.valueOf(args[1]), Number.class, null);
        Integer defaultValue = (Integer) args[2];
        return (n != null) ? (n instanceof Integer ? (Integer) n : n.intValue()) : defaultValue;
    }

    private static Object getLong(Object[] args) {
        Number n = jsonGetAs(args[0], String.valueOf(args[1]), Number.class, null);
        Long defaultValue = (Long) args[2];
        return (n != null) ? (n instanceof Long ? (Long) n : n.longValue()) : defaultValue;
    }

    private static Object getDouble(Object[] args) {
        Number n = jsonGetAs(args[0], String.valueOf(args[1]), Number.class, null);
        Double defaultValue = (Double) args[2];
        return (n != null) ? (n instanceof Double ? (Double) n : n.doubleValue()) : defaultValue;
    }

    private static Object getBool(Object[] args) {
        return jsonGetAs(args[0], String.valueOf(args[1]), Boolean.class, (Boolean) args[2]);
    }

    @SuppressWarnings("unchecked")
    private static Object getArray(Object[] args) {
        Object v = Json.getValue(args[0], String.valueOf(args[1]));
        return (v instanceof ArrayDef) ? (ArrayDef) v : null;
    }

    @SuppressWarnings("unchecked")
    private static Object getObject(Object[] args) {
        Object v = Json.getValue(args[0], String.valueOf(args[1]));
        return (v instanceof java.util.Map) ? (java.util.Map<String, Object>) v : null;
    }

    private static Object set(Object[] args) {
        Object root = args[0];
        String path = (String) args[1];
        Object val = args[2];
        Json.setValue(root, path, val);
        return root;
    }

    private static Object setStrict(Object[] args) {
        Object root = args[0];
        String path = (String) args[1];
        Object val = args[2];
        Json.setValueStrict(root, path, val);
        return root;
    }

    private static Object remove(Object[] args) {
        Object root = args[0];
        String path = (String) args[1];
        Json.remove(root, path);
        return root;
    }

    private static Object add(Object[] args) {
        Object root = args[0];
        String arrPath = (String) args[1];
        Object val = args[2];
        Json.add(root, arrPath, val);
        return root;
    }

    private static Object insert(Object[] args) {
        Object root = args[0];
        String arrPath = (String) args[1];
        Number index = (Number) args[2];
        Object val = args[3];
        Json.insert(root, arrPath, index.intValue(), val);
        return root;
    }

    // --- Helper methods ---

    /**
     * Typed lookup (non-strict): returns defaultValue if null or type mismatch.
     */
    private static <T> T jsonGetAs(Object jsonRoot, String path, Class<T> type, T defaultValue) {
        return Json.getAs(jsonRoot, path, type, defaultValue);
    }
}
