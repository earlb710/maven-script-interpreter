package com.eb.script.json;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.token.DataType;
import java.util.*;
import java.util.regex.Pattern;
import java.util.Base64;

/**
 * Minimal, dependency-free JSON Schema validator for the shapes produced by
 * Json.parse(...): - Objects -> Map<String, Object>
 * - Arrays -> ArrayDef (ArrayDynamic) - Leaves -> String / Number / Boolean /
 * null
 *
 * Supports a practical subset of JSON Schema (2020-12 style keywords).
 */
public final class JsonSchema {

    private static final class Settings {

        public boolean validateFormats = true;
        public boolean requiredNonNull = false;
        public boolean uniqueCaseInsensitive = true;
        public boolean uniqueIgnoreNulls = false;
    }

    private static final class Context {

        public Settings settings;
        public Map<String, Object> rootSchema;
        public Map<String, Object> rootDefs;
        public Map<String, ArrayDef> errors;

        public Context(Map<String, Object> schema, Map<String, Object> rootDefs, Settings settings) {
            this.settings = settings;
            this.rootSchema = schema;
            this.rootDefs = rootDefs;
            this.errors = new HashMap();
        }

        public Map<String, Object> resolveRef(String ref) {
            if (ref.startsWith("#/$defs/") && rootDefs != null) {
                String name = ref.substring("#/$defs/".length());
                Object d = rootDefs.get(name);
                return getMap(d);
            } else if (ref.equals("#")) {
                return rootSchema;
            }
            return null;
        }

        public void addError(String path, String message) {
            ArrayDef e = errors.get(path);
            if (e == null) {
                e = new ArrayDynamic(DataType.STRING);
            }
            e.add(message);
            errors.put(path, e);
        }

    }

    public static Map<String, ArrayDef> validate(Object instance, Map<String, Object> schema) {
        return validate(instance, schema, new Settings());
    }

    public static Map<String, ArrayDef> validate(Object instance, Map<String, Object> schema, Settings settings) {
        Map<String, Object> rootDefs = getMap(schema.get("$defs"));
        Context ctx = new Context(schema, rootDefs, settings);
        validateAt(instance, schema, "$", null, null, ctx);
        return ctx.errors;
    }

    // ===== Core recursive validation =====
    @SuppressWarnings("unchecked")
    private static void validateAt(Object instance, Map<String, Object> schema, String path, Object parent, Object parentKey, Context ctx) {

        // Handle $ref (internal only, #/$defs/Name)
        Object ref = schema.get("$ref");
        if (ref instanceof String sref) {
            Map<String, Object> target = ctx.resolveRef(sref);
            if (target == null) {
                ctx.addError(path, "Unresolvable $ref: " + sref);
                return;
            }
            validateAt(instance, target, path, parent, parentKey, ctx);
            return;
        }

        // type (string or array of strings)
        Set<String> typeSet = typeSet(schema.get("type"));
        if (!typeSet.isEmpty()) {
            Object newType = typeConvert(instance, typeSet);
            if (newType != instance) {
                setData(parent, parentKey, newType);
            } else if (!typeMatches(instance, typeSet)) {
                ctx.addError(path, "Type mismatch. Expected " + typeSet + " but found " + typeName(instance));
                // Early exit: further keyword checks generally assume correct type
                return;
            }
        }

        // enum
        List<Object> enumVals = getList(schema.get("enum"));
        if (enumVals != null) {
            boolean ok = false;
            for (Object v : enumVals) {
                if (Objects.equals(v, instance)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                ctx.addError(path, "Value not in enum value list: " + enumVals);
                return;
            }
        }

        // Dispatch per type
        if (instance == null) {
            // If null is allowed, nothing else to validate; else (already errored)
            return;
        } else if (instance instanceof Map<?, ?> map) {
            validateObject((Map<String, Object>) map, schema, path, parent, parentKey, ctx);
        } else if (instance instanceof ArrayDef list) {
            validateArray(list, schema, path, parent, parentKey, ctx);
        } else if (instance instanceof String s) {
            validateString(s, schema, path, parent, parentKey, ctx);
        } else if (instance instanceof Number num) {
            validateNumber(num, schema, path, parent, parentKey, ctx);
        } else if (instance instanceof Boolean) {
            // no specific constraints
        } else {
            // Fallback: treat other types as strings
            validateString(String.valueOf(instance), schema, path, parent, parentKey, ctx);
        }
    }

    private static void validateObject(Map<String, Object> obj, Map<String, Object> schema,
            String path, Object parent, Object parentKey, Context ctx) {
        // required
        List<String> required = toStringList(schema.get("required"));
        if (required != null) {
            for (String k : required) {
                if (!obj.containsKey(k)) {
                    ctx.addError(path, "Missing required property '" + k + "'");
                }
            }
        }
        // properties
        Map<String, Object> props = getMap(schema.get("properties"));
        // additionalProperties (default true)
        Object ap = schema.get("additionalProperties");
        boolean allowAdditional = !(ap instanceof Boolean b && !b);

        // min/maxProperties
        Integer minProps = toInt(schema.get("minProperties"));
        Integer maxProps = toInt(schema.get("maxProperties"));
        if (minProps != null && obj.size() < minProps) {
            ctx.addError(path, "Too few properties: " + obj.size() + " < " + minProps);
        }
        if (maxProps != null && obj.size() > maxProps) {
            ctx.addError(path, "Too many properties: " + obj.size() + " > " + maxProps);
        }

        // validate known properties
        if (props != null) {
            for (Map.Entry<String, Object> e : obj.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                Object propSchema = props.get(k);
                if (propSchema instanceof Map<?, ?> ps) {
                    validateAt(v, (Map<String, Object>) ps, path + "." + escapeKey(k), e, k, ctx);
                } else if (!allowAdditional) {
                    ctx.addError(path, "Unknown property '" + k + "' (additionalProperties=false)");
                }
            }
        } else {
            // no properties defined: only enforce additionalProperties=false
            if (!allowAdditional) {
                for (String k : obj.keySet()) {
                    ctx.addError(path, "Unknown property '" + k + "' (additionalProperties=false)");
                }
            }
        }
        List<String> distinctProps = toStringList(schema.get("distinctProperties"));
        if (distinctProps != null && distinctProps.size() >= 2) {
            java.util.Set<String> seenVals = new java.util.HashSet<>();
            for (String k : distinctProps) {
                if (!obj.containsKey(k)) {
                    continue; // absent keys are ignored for this check
                }
                Object v = obj.get(k);
                if (v == null && ctx.settings.uniqueIgnoreNulls) {
                    continue;
                }
                String key = stableKeyForUniqueness(v, ctx.settings);
                if (!seenVals.add(key)) {
                    ctx.addError(path, "Properties " + distinctProps + " must be distinct; duplicate value at '" + k + "'");
                    break;
                }
            }
        }
    }

    private static void validateArray(ArrayDef list, Map<String, Object> schema,
            String path, Object parent, Object parentKey, Context ctx) {

        Integer minItems = toInt(schema.get("minItems"));
        Integer maxItems = toInt(schema.get("maxItems"));
        Boolean unique = toBool(schema.get("uniqueItems"));

        if (minItems != null && list.size() < minItems) {
            ctx.addError(path, "Too few items: " + list.size() + " < " + minItems);
        }
        if (maxItems != null && list.size() > maxItems) {
            ctx.addError(path, "Too many items: " + list.size() + " > " + maxItems);
        }
        if (Boolean.TRUE.equals(unique)) {
            Set<Object> seen = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                Object it = list.get(i);
                if (!seen.add(stableKey(it))) {
                    ctx.addError(path, "Items must be unique (duplicate at index " + i + ")");
                    break;
                }
            }
        }

        // items (single schema only, for simplicity)
        Object items = schema.get("items");
        if (items instanceof Map<?, ?> itemSchema) {
            for (int i = 0; i < list.size(); i++) {
                Object it = list.get(i);
                validateAt(it, (Map<String, Object>) itemSchema, path + "[" + i + "]", list, i, ctx);
            }
        }

        Object uniqueByRaw = schema.get("uniqueBy");
        List<String> uniquePaths = toStringList(uniqueByRaw);
        if (uniqueByRaw instanceof String s && uniquePaths == null) {
            uniquePaths = List.of(s); // allow single string
        }

        if (uniquePaths != null && !uniquePaths.isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                List<Object> keyParts = new ArrayList<>(uniquePaths.size());
                for (String p : uniquePaths) {
                    Object kv;
                    // Special case: "$" or empty means "the item itself"
                    if ("$".equals(p) || p.isEmpty()) {
                        kv = item;
                    } else {
                        // Use your Json path helper to extract nested fields
                        kv = Json.getValue(item, p);
                    }
                    keyParts.add(kv);
                }
                String composite = compositeKey(keyParts, ctx.settings);
                if (composite == null && ctx.settings.uniqueIgnoreNulls) {
                    continue; // skip null-only keys when ignoring nulls
                }
                if (!seen.add(composite)) {
                    ctx.addError(path, "Duplicate item for uniqueBy " + uniquePaths + " at index " + i);
                    // You may choose to continue collecting all duplicates; here we report first hit.
                    break;
                }
            }
        }

    }

    private static String compositeKey(List<Object> parts, Settings settings) {
        // If all parts are null and uniqueIgnoreNulls is true -> return null to signal "skip"
        boolean allNull = true;
        for (Object p : parts) {
            if (p != null) {
                allNull = false;
                break;
            }
        }
        if (allNull && settings.uniqueIgnoreNulls) {
            return null;
        }

        List<String> normalized = new ArrayList<>(parts.size());
        for (Object p : parts) {
            normalized.add(stableKeyForUniqueness(p, settings));
        }
        // Use a separator unlikely to appear in normal values
        return String.join("\u0001", normalized);
    }

    private static String stableKeyForUniqueness(Object v, Settings settings) {
        if (v == null) {
            return "<NULL>";
        }
        if (v instanceof String s) {
            if (settings.uniqueCaseInsensitive) {
                s = s.toLowerCase(java.util.Locale.ROOT);
            }
            return s;
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        if (v instanceof Map<?, ?> || v instanceof ArrayDef) {
            // Reuse existing stableKey, but ensure case-insensitive for strings inside by stringifying
            return String.valueOf(stableKey(v));
        }
        return String.valueOf(v);
    }

    private static void validateString(String s, Map<String, Object> schema,
            String path, Object parent, Object parentKey, Context ctx) {
        Integer min = toInt(schema.get("minLength"));
        Integer max = toInt(schema.get("maxLength"));
        if (min != null && s.length() < min) {
            ctx.addError(path, "String too short: " + s.length() + " < " + min);
        }
        if (max != null && s.length() > max) {
            ctx.addError(path, "String too long: " + s.length() + " > " + max);
        }
        // pattern
        Object pat = schema.get("pattern");
        if (pat instanceof String re) {
            Pattern p = Pattern.compile(re);
            if (!p.matcher(s).matches()) {
                ctx.addError(path, "String does not match pattern: " + re);
            }
        }

        // contentEncoding: base64 (+ optional contentMediaType & contentSchema)
        Object enc = schema.get("contentEncoding");
        if ("base64".equals(enc)) {
            // Enforce strict RFC4648 base64 by regex + actual decode
            String base64Regex = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
            if (!Pattern.compile(base64Regex).matcher(s).matches()) {
                ctx.addError(path, "Invalid base64 encoding");
            } else {
                try {
                    byte[] bytes = Base64.getDecoder().decode(s);
                    // Optional contentMediaType + contentSchema for JSON payloads
                    Object cmt = schema.get("contentMediaType");
                    Object csch = schema.get("contentSchema");
                    if (cmt instanceof String media && csch instanceof Map<?, ?> inner) {
                        if (media.equals("application/json") || media.endsWith("+json")) {
                            // Best-effort: decode and validate if textual JSON
                            String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                            Object decoded = com.eb.script.json.Json.parse(json); // uses your parser
                            setData(parent, parentKey, decoded);
                            //noinspection unchecked
                            validateAt(decoded, (Map<String, Object>) inner, path + "(decoded)", parent, parentKey, ctx);
                        }
                    } else {
                        ArrayDef data = new ArrayFixedByte(bytes);
                        setData(parent, parentKey, data);
                    }
                } catch (IllegalArgumentException ex) {
                    ctx.addError(path, "Invalid base64 payload (decoder failed)");
                }
            }
        }
        Object fmt = schema.get("format");
        if (ctx.settings.validateFormats && fmt instanceof String f) {
            String err = checkFormat(s, f);
            if (err != null) {
                ctx.addError(path, "Invalid " + f + " format: " + err);
            }
        }
    }

    private static void validateNumber(Number num,
            Map<String, Object> schema,
            String path,
            Object parent, Object parentKey, Context ctx) {
        Double val = num.doubleValue();

        // integer type (when type includes "integer")
        Set<String> typeSet = typeSet(schema.get("type"));
        if (typeSet.contains("integer")) {
            if (val == Math.floor(val)) {
                // ok
            } else {
                ctx.addError(path, "Expected integer but found non-integer: " + val);
            }
        }

        Double min = toDouble(schema.get("minimum"));
        Double max = toDouble(schema.get("maximum"));
        Double exMin = toDouble(schema.get("exclusiveMinimum"));
        Double exMax = toDouble(schema.get("exclusiveMaximum"));
        Double mult = toDouble(schema.get("multipleOf"));

        if (min != null && val < min) {
            ctx.addError(path, "Value " + val + " < minimum " + min);
        }
        if (max != null && val > max) {
            ctx.addError(path, "Value " + val + " > maximum " + max);
        }
        if (exMin != null && !(val > exMin)) {
            ctx.addError(path, "Value " + val + " <= exclusiveMinimum " + exMin);
        }
        if (exMax != null && !(val < exMax)) {
            ctx.addError(path, "Value " + val + " >= exclusiveMaximum " + exMax);
        }
        if (mult != null) {
            double q = val / mult;
            // Allow small epsilon for floating-point
            if (Math.abs(Math.rint(q) - q) > 1e-12) {
                ctx.addError(path, "Value " + val + " is not a multiple of " + mult);
            }
        }
    }

    // ===== Helpers =====
    private static Map<String, Object> resolveRef(String ref, Map<String, Object> rootDefs, Map<String, Object> rootSchema) {
        if (ref.startsWith("#/$defs/") && rootDefs != null) {
            String name = ref.substring("#/$defs/".length());
            Object d = rootDefs.get(name);
            if (d instanceof Map<?, ?> m) {
                return getMap(d);
            }
        } else if (ref.equals("#")) {
            return rootSchema;
        }
        return null;
    }

    private static Set<String> typeSet(Object typeVal) {
        Set<String> s = new LinkedHashSet<>();
        if (typeVal instanceof String t) {
            s.add(t);
        } else if (typeVal instanceof List<?> lst) {
            for (Object o : lst) {
                if (o instanceof String st) {
                    s.add(st);
                }
            }
        }
        return s;
    }

    private static boolean typeMatches(Object instance, Set<String> typeSet) {
        if (typeSet != null && !typeSet.isEmpty()) {
            if (instance == null) {
                return typeSet.contains("null");
            }
            boolean obj = instance instanceof Map<?, ?>;
            boolean arr = instance instanceof ArrayDef;
            boolean str = instance instanceof String;
            boolean num = instance instanceof Number;
            boolean bool = instance instanceof Boolean;
            // integer vs number
            boolean integer = num && ((Number) instance).doubleValue() == Math.floor(((Number) instance).doubleValue());

            if (typeSet.contains("object") && obj) {
                return true;
            }
            if (typeSet.contains("array") && arr) {
                return true;
            }
            if (typeSet.contains("string") && str) {
                return true;
            }
            if (typeSet.contains("number") && num) {
                return true;
            }
            if (typeSet.contains("integer") && integer) {
                return true;
            }
            if (typeSet.contains("boolean") && bool) {
                return true;
            }
            if (typeSet.contains("null") && instance == null) {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    private static Object typeConvert(Object instance, Set<String> typeSet) {
        if (typeSet != null && !typeSet.isEmpty() && instance != null) {
            try {
                boolean str = instance instanceof String;
                boolean num = instance instanceof Number;
                boolean isInt = instance instanceof Integer || instance instanceof Long;
                boolean isDouble = instance instanceof Double || instance instanceof Float;
                boolean bool = instance instanceof Boolean;
                for (String type : typeSet) {
                    // integer vs number
                    if (type.equals("string")) {
                        if (str) {
                            return instance;
                        }
                        if (isInt) {
                            return String.valueOf(((Number) instance).longValue());
                        }
                        if (isDouble) {
                            return String.valueOf(((Number) instance).doubleValue());
                        }
                        if (bool) {
                            return ((Boolean) instance) ? "Y" : "N";
                        }
                    }
                    if (type.equals("integer")) {
                        if (isInt) {
                            return instance;
                        }
                        if (str) {
                            return Long.valueOf((String) instance);
                        }
                        if (num) {
                            return ((Number) instance).longValue();
                        }
                    }
                    if (type.equals("number")) {
                        if (isDouble) {
                            return instance;
                        }
                        if (str) {
                            return Double.valueOf((String) instance);
                        }
                        if (isInt) {
                            return ((Number) instance).doubleValue();
                        }
                    }
                    if (type.equals("boolean")) {
                        if (bool) {
                            return instance;
                        }
                        if (str) {
                            String b = ((String) instance);
                            if ((b.equalsIgnoreCase("Y")) || (b.equalsIgnoreCase("TRUE"))) {
                                return true;
                            } else if ((b.equalsIgnoreCase("N")) || (b.equalsIgnoreCase("FALSE"))) {
                                return false;
                            } else {
                                return instance;
                            }
                        }
                        if (num) {
                            return ((Number) instance).intValue() == 1 ? true : ((Number) instance).intValue() == 0 ? 0 : instance;
                        }
                    }
                }
            } catch (Exception ex) {
                //convert errors ingore
            }
        }
        return instance;

    }

    private static String typeName(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Map<?, ?>) {
            return "object";
        }
        if (o instanceof ArrayDef) {
            return "array";
        }
        if (o instanceof String) {
            return "string";
        }
        if (o instanceof Boolean) {
            return "boolean";
        }
        if (o instanceof Number n) {
            return (n.doubleValue() == Math.floor(n.doubleValue())) ? "integer/number" : "number";
        }
        return o.getClass().getSimpleName();
    }

    private static Map<String, Object> getMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getList(Object o) {
        return (o instanceof List<?>) ? (List<Object>) o : null;
    }

    private static Integer toInt(Object o) {
        return (o instanceof Number n) ? Integer.valueOf(n.intValue()) : null;
    }

    private static Double toDouble(Object o) {
        return (o instanceof Number n) ? Double.valueOf(n.doubleValue()) : null;
    }

    private static Boolean toBool(Object o) {
        return (o instanceof Boolean b) ? b : null;
    }

    private static List<String> toStringList(Object o) {
        if (o instanceof List raw) {
            List<String> out = new ArrayList<>(raw.size());
            for (Object v : raw) {
                out.add(String.valueOf(v));
            }
            return out;
        } else if (o instanceof ArrayDef raw) {
            List<String> out = new ArrayList<>(raw.size());
            for (Object v : raw) {
                out.add(String.valueOf(v));
            }
            return out;
        }
        return null;
    }

    private static String escapeKey(String k) {
        // Keep simple for dotted paths; quotes would complicate. Replace whitespace.
        return k.replaceAll("\\s+", "_");
    }

    private static Object stableKey(Object it) {
        // For simple uniqueness, stringify maps/arrays deterministically.
        if (it instanceof Map<?, ?> m) {
            List<String> parts = new ArrayList<>();
            for (String k : new TreeSet<>(m.keySet().stream().map(String::valueOf).toList())) {
                parts.add(k + ":" + String.valueOf(m.get(k)));
            }
            return String.join("|", parts);
        }
        if (it instanceof ArrayDef list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(String.valueOf(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return it;
    }

// inside JsonSchema class:
    private static String checkFormat(String s, String format) {
        try {
            switch (format) {
                case "email":
                    return isEmail(s) ? null : "not a valid email";
                case "uuid":
                    return isUuid(s) ? null : "not an RFC 4122 UUID";
                case "uri":
                    return isUri(s, /*requireScheme=*/ true) ? null : "not an absolute URI";
                case "uri-reference":
                    return isUri(s, /*requireScheme=*/ false) ? null : "not a valid URI reference";
                case "hostname":
                    return isHostname(s) ? null : "not a valid hostname";
                case "ipv4":
                    return isIpv4(s) ? null : "not an IPv4 address";
                case "ipv6":
                    return isIpv6(s) ? null : "not an IPv6 address";
                case "date":
                    java.time.LocalDate.parse(s);
                    return null; // RFC 3339 full-date
                case "time":
                    // RFC 3339 full-time requires offset; OffsetTime enforces it
                    java.time.OffsetTime.parse(s);
                    return null;
                case "date-time":
                    // RFC 3339 date-time (with offset)
                    java.time.OffsetDateTime.parse(s);
                    return null;
                case "duration":
                    java.time.Duration.parse(s);
                    return null; // ISO-8601 duration
                case "regex":
                    java.util.regex.Pattern.compile(s);
                    return null;
                default:
                    // Unknown formats are treated as annotation-only; no error.
                    return null;
            }
        } catch (Exception ex) {
            return ex.getMessage() == null ? "parse error" : ex.getMessage();
        }
    }

    private static boolean isEmail(String s) {
        // Pragmatic, not exhaustive. Ensures single @, non-empty local & domain, basic TLD dot.
        // For strict validation, plug your own policy.
        String re = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        return java.util.regex.Pattern.compile(re).matcher(s).matches();
    }

    private static boolean isUuid(String s) {
        // RFC 4122 (version 1-5, with proper variant)
        String re = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
        return java.util.regex.Pattern.compile(re).matcher(s).matches();
    }

    private static boolean isUri(String s, boolean requireScheme) {
        try {
            java.net.URI u = new java.net.URI(s);
            if (requireScheme) {
                return u.getScheme() != null && !u.getScheme().isEmpty();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isHostname(String s) {
        // Labels: 1..63 chars, alnum + hyphen (no leading/trailing hyphen)
        // Total length <= 253; allow single label or dot-separated.
        if (s.length() > 253) {
            return false;
        }
        String label = "(A-Za-z0-9?)";
        String re = "^" + label + "(?:\\." + label + ")*$";
        return java.util.regex.Pattern.compile(re).matcher(s).matches();
    }

    private static boolean isIpv4(String s) {
        String octet = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
        String re = "^(" + octet + "\\.){3}" + octet + "$";
        return java.util.regex.Pattern.compile(re).matcher(s).matches();
    }

    private static boolean isIpv6(String s) {
        // Let the JDK parse numeric IPv6 literals; this does not require network I/O.
        try {
            java.net.InetAddress addr = java.net.InetAddress.getByName(s);
            return addr instanceof java.net.Inet6Address && s.contains(":");
        } catch (Exception e) {
            return false;
        }
    }

    private static void setData(Object parent, Object parentKey, Object data) {
        if (parent instanceof Map pmap) {
            pmap.put(parentKey, data);
        }
        if (parent instanceof List plist) {
            plist.set((Integer) parentKey, data);
        }
    }
}
