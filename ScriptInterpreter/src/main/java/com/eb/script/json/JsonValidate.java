package com.eb.script.json;

import com.eb.script.arrays.ArrayDef;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Earl Bosch
 */
public class JsonValidate {

    private static final Map<String, Map<String, Object>> schemas = new HashMap();

    public static void registerSchema(String name, String jsonSchema) {
        var schema = (Map<String, Object>) Json.parse(jsonSchema);
        schemas.put(name, schema);
    }

    public static void registerSchema(String name, Map<String, Object> jsonSchema) {
        schemas.put(name, jsonSchema);
    }

    public static Map<String, Object> getSchema(String name) {
        return schemas.get(name);
    }

    public static Map<String, ArrayDef> validate(String schemaName, Object json) {
        Map<String, Object> schema = schemas.get(schemaName);
        if (schema != null) {
            Map<String, ArrayDef> errors = JsonSchema.validate(json, schema);
            return errors;
        }
        return null;
    }
}
