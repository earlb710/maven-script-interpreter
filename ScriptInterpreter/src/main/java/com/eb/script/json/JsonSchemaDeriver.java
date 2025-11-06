package com.eb.script.json;

import java.util.*;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.token.DataType;

/**
 * JsonSchemaDeriver - derive a (draft-2020-12 style) JSON Schema from a sample
 * JSON instance.
 *
 * The output schema is represented as a nested Map<String,Object>/List<Object>
 * tree that is compatible with the rest of this codebase and can be printed
 * with {@link Json#prettyJson}.
 *
 * Heuristics (configurable via Options): - objects: every observed property is
 * added to "properties"; if inferRequired=true, properties present in the
 * sample are listed under "required". (NB: with a single sample, required is a
 * best effort.) - arrays: * if items' schemas are identical -> single schema in
 * "items"; * if items are all objects -> properties are merged (union).
 * "required" is the intersection when inferRequired=true; otherwise omitted. *
 * otherwise -> "oneOf" with the distinct item schemas. Optionally sets minItems
 * and derives number/string bounds across the array when requested. - numbers:
 * integer vs number detection when detectInteger=true. - null values: if
 * allowNullType=true and a container sees nulls, the resulting type becomes a
 * union including "null".
 */
public final class JsonSchemaDeriver {

    private JsonSchemaDeriver() {
    }

    /**
     * Options controlling inference behaviour.
     */
    public static final class Options {

        /**
         * If true, add a "required" array for object properties observed in the
         * sample.
         */
        public boolean inferRequired = true;
        /**
         * If true, for arrays of objects merge properties by union; else keep
         * oneOf.
         */
        public boolean mergeArrayObjectsByUnion = true;
        /**
         * If true, treat integral numbers as "integer"; otherwise always
         * "number".
         */
        public boolean detectInteger = true;
        /**
         * If true, allow nulls by widening the type to include "null" when
         * encountered.
         */
        public boolean allowNullType = true;
        /**
         * If >0 and number of distinct scalar values <= this limit, emit an
         * "enum".
         */
        public int enumMaxCardinality = 0;
        /**
         * If true, compute min/max for numeric arrays (based on observed
         * values).
         */
        public boolean computeNumericBounds = true;
        /**
         * If true, compute minLength/maxLength for string arrays (based on
         * observed values).
         */
        public boolean computeStringLengths = true;
        /**
         * If true, emit additionalProperties=false for objects.
         */
        public boolean additionalPropertiesFalse = false;
    }

    /**
     * Derive a schema Map from a parsed JSON instance (as produced by
     * {@link Json#parse}).
     */
    public static Map<String, Object> derive(Object instance) {
        return derive(instance, new Options());
    }

    /**
     * Derive a schema Map from a parsed JSON instance with options.
     */
    public static Map<String, Object> derive(Object instance, Options opt) {
        return inferSchema(instance, opt);
    }

    /**
     * Convenience: parse input JSON and derive schema, returning pretty-printed
     * JSON.
     */
    public static String deriveFromJson(String json, Options opt) {
        Object instance = Json.parse(json);
        Map<String, Object> schema = derive(instance, opt);
        return Json.prettyJson(schema);
    }

    // =============================================================
    // Inference
    // =============================================================
    @SuppressWarnings("unchecked")
    private static Map<String, Object> inferSchema(Object v, Options opt) {
        if (v == null) {
            return typeOnly("null");
        }
        if (v instanceof Map<?, ?>) {
            Map<String, Object> props = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            Map<String, Object> obj = castToMap(v);
            for (Map.Entry<String, Object> e : obj.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                Map<String, Object> child = inferSchema(val, opt);
                props.put(key, child);
                if (opt.inferRequired && val != null) {
                    required.add(key);
                }
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("type", "object");
            out.put("properties", props);
            if (opt.inferRequired && !required.isEmpty()) {
                out.put("required", required);
            }
            if (opt.additionalPropertiesFalse) {
                out.put("additionalProperties", Boolean.FALSE);
            }
            return out;
        }
        if (v instanceof ArrayDef || v instanceof List<?>) {
            List<Object> list = toList(v);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("type", "array");
            if (list.isEmpty()) {
                out.put("items", new LinkedHashMap<String, Object>()); // unconstrained
                out.put("minItems", 0);
                return out;
            }
            // Infer item schemas
            List<Map<String, Object>> itemSchemas = new ArrayList<>();
            Set<String> scalarEnumSet = new LinkedHashSet<>();
            double minNum = Double.POSITIVE_INFINITY, maxNum = Double.NEGATIVE_INFINITY;
            int minLen = Integer.MAX_VALUE, maxLen = Integer.MIN_VALUE;
            boolean sawNumber = false, sawString = false;

            for (Object it : list) {
                Map<String, Object> s = inferSchema(it, opt);
                itemSchemas.add(s);
                // Collect basic stats for optional bounds
                if (it instanceof Number n) {
                    double d = n.doubleValue();
                    minNum = Math.min(minNum, d);
                    maxNum = Math.max(maxNum, d);
                    sawNumber = true;
                } else if (it instanceof String sstr) {
                    int L = sstr.length();
                    minLen = Math.min(minLen, L);
                    maxLen = Math.max(maxLen, L);
                    sawString = true;
                }
                // Scalar enum collection
                if (opt.enumMaxCardinality > 0 && isScalar(it)) {
                    scalarEnumSet.add(String.valueOf(it));
                }
            }

            Map<String, Object> itemsSchema = mergeItemSchemas(itemSchemas, opt);
            out.put("items", itemsSchema);
            out.put("minItems", list.size());
            if (opt.computeNumericBounds && sawNumber && itemsTypeIncludes(itemsSchema, "number", "integer")) {
                Map<String, Object> numConstraints = ensureFacetObject(itemsSchema);
                numConstraints.put("minimum", minNum);
                numConstraints.put("maximum", maxNum);
            }
            if (opt.computeStringLengths && sawString && itemsTypeIncludes(itemsSchema, "string")) {
                Map<String, Object> strConstraints = ensureFacetObject(itemsSchema);
                strConstraints.put("minLength", minLen);
                strConstraints.put("maxLength", maxLen);
            }
            if (opt.enumMaxCardinality > 0 && scalarEnumSet.size() > 0 && scalarEnumSet.size() <= opt.enumMaxCardinality) {
                // Note: enum of strings of scalars (stringified) is conservative; callers can adjust.
                List<String> enums = new ArrayList<>(scalarEnumSet);
                Map<String, Object> item = castToMap(out.get("items"));
                item.put("enum", enums);
            }
            return out;
        }
        if (v instanceof String) {
            Map<String, Object> out = typeOnly("string");
            return out;
        }
        if (v instanceof Boolean) {
            return typeOnly("boolean");
        }
        if (v instanceof Number n) {
            if (opt.detectInteger && isIntegral(n)) {
                return typeOnly("integer");
            }
            return typeOnly("number");
        }
        // Fallback: unknown -> leave as unconstrained
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> typeOnly(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        return m;
    }

    private static boolean isIntegral(Number n) {
        double d = n.doubleValue();
        return Math.rint(d) == d && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE;
    }

    private static boolean isScalar(Object v) {
        return v == null || v instanceof String || v instanceof Number || v instanceof Boolean;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object v) {
        if (v instanceof ArrayDef) {
            ArrayDef a = (ArrayDef) v;
            List<Object> list = new ArrayList<>(a.size());
            for (int i = 0; i < a.size(); i++) {
                list.add(a.get(i));
            }
            return list;
        }
        return (List<Object>) v;
    }

    // Merge a set of item schemas into a single items schema.
    //  - When items are homogeneous -> return that schema
    //  - For arrays of objects and mergeArrayObjectsByUnion=true: union properties, intersect required
    //  - Otherwise, return { oneOf: [distinct schemas] }
    private static Map<String, Object> mergeItemSchemas(List<Map<String, Object>> schemas, Options opt) {
        // deduplicate by JSON-ish identity (stringify map key ordering preserved)
        List<Map<String, Object>> distinct = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> s : schemas) {
            String key = s.toString();
            if (seen.add(key)) {
                distinct.add(s);
            }
        }
        if (distinct.size() == 1) {
            return cloneMap(distinct.get(0));
        }

        // Are all objects?
        boolean allObjects = true;
        for (Map<String, Object> s : distinct) {
            Object t = s.get("type");
            if (!"object".equals(t)) {
                allObjects = false;
                break;
            }
        }
        if (allObjects && opt.mergeArrayObjectsByUnion) {
            Map<String, Object> union = new LinkedHashMap<>();
            union.put("type", "object");

            // Merge properties (union) and required (intersection, if enabled)
            Map<String, Object> mergedProps = new LinkedHashMap<>();
            Set<String> requiredIntersection = null;
            for (Map<String, Object> s : distinct) {
                Map<String, Object> props = castToMap(s.getOrDefault("properties", new LinkedHashMap<String, Object>()));
                for (Map.Entry<String, Object> e : props.entrySet()) {
                    String k = e.getKey();
                    Map<String, Object> child = castToMap(e.getValue());
                    // If property repeats with different schema, use oneOf
                    if (!mergedProps.containsKey(k)) {
                        mergedProps.put(k, cloneMap(child));
                    } else {
                        Map<String, Object> prior = castToMap(mergedProps.get(k));
                        mergedProps.put(k, mergePropertySchemas(prior, child));
                    }
                }
                if (requiredIntersection == null) {
                    requiredIntersection = new LinkedHashSet<>(castToStringList(s.get("required")));
                } else {
                    requiredIntersection.retainAll(castToStringList(s.get("required")));
                }
            }
            union.put("properties", mergedProps);
            if (opt.inferRequired && requiredIntersection != null && !requiredIntersection.isEmpty()) {
                union.put("required", new ArrayList<>(requiredIntersection));
            }
            if (opt.additionalPropertiesFalse) {
                union.put("additionalProperties", Boolean.FALSE);
            }
            return union;
        }

        // Otherwise, return a union with oneOf
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("oneOf", distinct);
        return out;
    }

    private static Map<String, Object> mergePropertySchemas(Map<String, Object> a, Map<String, Object> b) {
        if (Objects.equals(a, b)) {
            return cloneMap(a);
        }
        // If types match and are object, merge recursively
        if ("object".equals(a.get("type")) && "object".equals(b.get("type"))) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("type", "object");
            Map<String, Object> propsA = castToMap(a.getOrDefault("properties", new LinkedHashMap<String, Object>()));
            Map<String, Object> propsB = castToMap(b.getOrDefault("properties", new LinkedHashMap<String, Object>()));
            Map<String, Object> props = new LinkedHashMap<>();
            Set<String> keys = new LinkedHashSet<>();
            keys.addAll(propsA.keySet());
            keys.addAll(propsB.keySet());
            for (String k : keys) {
                Map<String, Object> sa = castToMap(propsA.get(k));
                Map<String, Object> sb = castToMap(propsB.get(k));
                if (sa == null) {
                    props.put(k, cloneMap(sb));
                } else if (sb == null) {
                    props.put(k, cloneMap(sa));
                } else {
                    props.put(k, mergePropertySchemas(sa, sb));
                }
            }
            // required -> intersection if available
            List<String> ra = castToStringList(a.get("required"));
            List<String> rb = castToStringList(b.get("required"));
            if (!ra.isEmpty() && !rb.isEmpty()) {
                Set<String> inter = new LinkedHashSet<>(ra);
                inter.retainAll(rb);
                if (!inter.isEmpty()) {
                    out.put("required", new ArrayList<>(inter));
                }
            }
            return out;
        }
        // Otherwise, union with oneOf
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("oneOf", List.of(cloneMap(a), cloneMap(b)));
        return out;
    }

    private static List<String> castToStringList(Object v) {
        if (v == null) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> l = (List<String>) v;
        return l;
    }

    private static Map<String, Object> cloneMap(Map<String, Object> m) {
        Map<String, Object> n = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object val = e.getValue();
            if (val instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> child = (Map<String, Object>) val;
                n.put(e.getKey(), cloneMap(child));
            } else if (val instanceof List<?>) {
                List<?> L = (List<?>) val;
                List<Object> copy = new ArrayList<>(L.size());
                for (Object x : L) {
                    copy.add(x);
                }
                n.put(e.getKey(), copy);
            } else {
                n.put(e.getKey(), val);
            }
        }
        return n;
    }

    /**
     * Ensure we can add facets (min/max, lengths) on the same object as "type".
     */
    private static Map<String, Object> ensureFacetObject(Map<String, Object> schema) {
        // If schema is { oneOf: [...] } we cannot attach facets directly. In that case,
        // return the first object type child if present; otherwise, just return schema.
        if (schema.containsKey("oneOf")) {
            return schema; // keep simple
        }
        return schema;
    }

    private static boolean itemsTypeIncludes(Map<String, Object> itemsSchema, String... types) {
        Object t = itemsSchema.get("type");
        if (t == null) {
            return false;
        }
        if (t instanceof String s) {
            for (String x : types) {
                if (x.equals(s)) {
                    return true;
                }
            }
        } else if (t instanceof List<?> L) {
            for (Object o : L) {
                if (o instanceof String s && Arrays.asList(types).contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
