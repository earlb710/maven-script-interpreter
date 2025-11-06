package com.eb.script.json;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.token.DataType;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Earl Bosch
 *
 * Minimal, dependency-free JSON parser. Converts JSON strings to Java objects:
 * - Object -> LinkedHashMap<String, Object>
 * - Array -> ArrayList<Object>
 * - Number -> Integer/Long/Double (auto) - Boolean -> Boolean - String ->
 * String - null -> null
 *
 * Usage: Object v = Json.parse("{\"x\":1,\"y\":[true,null,\"ok\"]}"); if (v
 * instanceof Map) { ... }
 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
        this.i = 0;
    }

    /**
     * Parse a JSON string into Java objects.
     */
    public static Object parse(String input) {
        if (input == null) {
            return null;
        }
        Json p = new Json(input);
        Object value = p.readValue();
        p.skipWs();
        if (!p.eof()) {
            throw p.error("Trailing content after JSON value");
        }
        return value;
    }

    // === Core reading ===
    private Object readValue() {
        skipWs();
        if (eof()) {
            throw error("Unexpected end of input");
        }
        char c = peek();
        return switch (c) {
            case '{' ->
                readObject();
            case '[' ->
                readArray();
            case '"' ->
                readString();
            case 't', 'f' ->
                readBoolean();
            case 'n' -> {
                readNull();
                yield null;
            }
            default ->
                readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        skipWs();
        Map<String, Object> obj = new LinkedHashMap<>();
        if (peek() == '}') {
            next();
            return obj;
        }
        while (true) {
            skipWs();
            if (peek() != '"') {
                throw error("Expected object key string");
            }
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            Object val = readValue();
            obj.put(key, val);
            skipWs();
            char c = next();
            if (c == '}') {
                break;
            }
            if (c != ',') {
                throw error("Expected ',' or '}' in object");
            }
        }
        return obj;
    }

    private ArrayDef<Object, List<Object>> readArray() {
        expect('[');
        skipWs();
        ArrayDef arr = new ArrayDynamic(DataType.ANY);
        if (peek() == ']') {
            next();
            return arr;
        }
        while (true) {
            Object val = readValue();
            arr.add(val);
            skipWs();
            char c = next();
            if (c == ']') {
                break;
            }
            if (c != ',') {
                throw error("Expected ',' or ']' in array");
            }
        }
        return arr;
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (eof()) {
                throw error("Unterminated string");
            }
            char c = next();
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (eof()) {
                    throw error("Unterminated escape");
                }
                char e = next();
                switch (e) {
                    case '"' ->
                        sb.append('"');
                    case '\\' ->
                        sb.append('\\');
                    case '/' ->
                        sb.append('/');
                    case 'b' ->
                        sb.append('\b');
                    case 'f' ->
                        sb.append('\f');
                    case 'n' ->
                        sb.append('\n');
                    case 'r' ->
                        sb.append('\r');
                    case 't' ->
                        sb.append('\t');
                    case 'u' -> {
                        int code = readHex(4);
                        sb.append((char) code);
                    }
                    default ->
                        throw error("Invalid escape '\\" + e + "'");
                }
            } else {
                // Control chars are not allowed unescaped in JSON strings
                if (c >= 0 && c < 0x20) {
                    throw error("Unescaped control character in string");
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean readBoolean() {
        if (match("true")) {
            return Boolean.TRUE;
        }
        if (match("false")) {
            return Boolean.FALSE;
        }
        throw error("Invalid boolean");
    }

    private void readNull() {
        if (!match("null")) {
            throw error("Invalid null");
        }
    }

    private Number readNumber() {
        int start = i;

        // Optional sign
        if (peek() == '-') {
            next();
        }

        // Integer part
        if (!isDigit(peek())) {
            throw error("Invalid number");
        }
        if (peek() == '0') {
            next(); // leading zero
        } else {
            while (isDigit(peek())) {
                next();
            }
        }

        boolean isFloat = false;

        // Fraction
        if (peek() == '.') {
            isFloat = true;
            next();
            if (!isDigit(peek())) {
                throw error("Digits required after decimal point");
            }
            while (isDigit(peek())) {
                next();
            }
        }

        // Exponent
        if (peek() == 'e' || peek() == 'E') {
            isFloat = true;
            next();
            if (peek() == '+' || peek() == '-') {
                next();
            }
            if (!isDigit(peek())) {
                throw error("Digits required in exponent");
            }
            while (isDigit(peek())) {
                next();
            }
        }

        String num = s.substring(start, i);
        try {
            if (isFloat) {
                return Double.valueOf(num);
            } else {
                long v = Long.parseLong(num);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    return (int) v;
                }
                return v;
            }
        } catch (NumberFormatException ex) {
            throw error("Invalid number '" + num + "'");
        }
    }

    // === Helpers ===
    private void skipWs() {
        while (!eof() && Character.isWhitespace(peek())) {
            i++;
        }
    }

    private boolean match(String kw) {
        int len = kw.length();
        if (i + len > s.length()) {
            return false;
        }
        if (s.regionMatches(i, kw, 0, len)) {
            i += len;
            return true;
        }
        return false;
    }

    private char peek() {
        return eof() ? '\0' : s.charAt(i);
    }

    private char next() {
        return eof() ? '\0' : s.charAt(i++);
    }

    private boolean eof() {
        return i >= s.length();
    }

    private void expect(char ch) {
        char c = next();
        if (c != ch) {
            throw error("Expected '" + ch + "' but got '" + c + "'");
        }
    }

    private int readHex(int n) {
        int code = 0;
        for (int k = 0; k < n; k++) {
            if (eof()) {
                throw error("Bad unicode escape (EOF)");
            }
            char h = next();
            int d = Character.digit(h, 16);
            if (d < 0) {
                throw error("Bad hex digit '" + h + "'");
            }
            code = (code << 4) | d;
        }
        return code;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static String compactJson(Object value) {
        return prettyJson(value, 0);
    }

    public static String prettyJson(Object value) {
        return prettyJson(value, 2);
    }

    public static String prettyJson(Object value, int indentSize) {
        StringBuilder sb = new StringBuilder(256);
        // Use an identity set to prevent accidental cycles (not expected for parsed JSON, but defensive)
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        writeJson(value, sb, 0, Math.max(0, indentSize), seen);
        return sb.toString();
    }

    private static void writeJson(Object v,
            StringBuilder out,
            int level,
            int indent,
            IdentityHashMap<Object, Boolean> seen) {
        if (v == null) {
            out.append("null");
            return;
        }
        // Detect possible cycles (defensive only)
        if (v instanceof Map || v instanceof ArrayDef) {
            if (seen.put(v, Boolean.TRUE) != null) {
                throw new JsonParseException("JSON structure contains a cycle; cannot pretty-print.");
            }
        }

        if (v instanceof Map<?, ?> m) {
            out.append('{');
            if (!m.isEmpty()) {
                out.append('\n');
                int i = 0, size = m.size();
                for (var e : m.entrySet()) {
                    indent(out, level + 1, indent);
                    out.append('"').append(escapeJsonString(String.valueOf(e.getKey()))).append('"').append(": ");
                    writeJson(e.getValue(), out, level + 1, indent, seen);
                    if (++i < size) {
                        out.append(',');
                    }
                    out.append('\n');
                }
                indent(out, level, indent);
            }
            out.append('}');
            return;
        } else if (v instanceof ArrayDef list) {
            out.append('[');
            if (!list.isEmpty()) {
                out.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    indent(out, level + 1, indent);
                    writeJson(list.get(i), out, level + 1, indent, seen);
                    if (i + 1 < list.size()) {
                        out.append(',');
                    }
                    out.append('\n');
                }
                indent(out, level, indent);
            }
            out.append(']');
            return;
        } else if (v instanceof List list) {
            out.append('[');
            if (!list.isEmpty()) {
                out.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    indent(out, level + 1, indent);
                    writeJson(list.get(i), out, level + 1, indent, seen);
                    if (i + 1 < list.size()) {
                        out.append(',');
                    }
                    out.append('\n');
                }
                indent(out, level, indent);
            }
            out.append(']');
            return;
        }

        if (v instanceof String s) {
            out.append('"').append(escapeJsonString(s)).append('"');
            return;
        }

        if (v instanceof Number || v instanceof Boolean) {
            out.append(String.valueOf(v));
            return;
        }

        // Fallback: stringify any other object as a JSON string
        out.append('"').append(escapeJsonString(String.valueOf(v))).append('"');
    }

    private static void indent(StringBuilder out, int level, int indent) {
        if (indent <= 0) {
            return;
        }
        for (int i = 0; i < level * indent; i++) {
            out.append(' ');
        }
    }

    private static String escapeJsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' ->
                    sb.append("\\\"");
                case '\\' ->
                    sb.append("\\\\");
                case '\b' ->
                    sb.append("\\b");
                case '\f' ->
                    sb.append("\\f");
                case '\n' ->
                    sb.append("\\n");
                case '\r' ->
                    sb.append("\\r");
                case '\t' ->
                    sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private JsonParseException error(String msg) {
        // Optionally include a short context snippet
        int from = Math.max(0, i - 20);
        int to = Math.min(s.length(), i + 20);
        String ctx = s.substring(from, to).replace("\n", "\\n");
        return new JsonParseException(msg + " at pos " + i + " near: \"" + ctx + "\"");
    }

    /**
     * RuntimeException thrown for parse errors.
     */
    public static final class JsonParseException extends RuntimeException {

        public JsonParseException(String message) {
            super(message);
        }
    }

    /**
     * Minimal JSON path lookup for objects created by your Json.parse(...): -
     * Objects -> Map<String, Object>
     * - Arrays -> List<Object>
     * - Leaves -> String / Number / Boolean / null
     *
     * Usage: Object root = Json.parse(jsonString); Object v1 =
     * JsonPath.getValue(root, "state[3].city[2].street[40].house[15].owner");
     * String s = JsonPath.getAs(root, "user.name", String.class, "<unknown>");
     */
    
    // check empty json
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true; // null is considered empty
        }
        if (obj instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        if (obj instanceof List<?> l) {
            return l.isEmpty();
        }
        if (obj instanceof ArrayDef a) {
            return a.isEmpty();
        }
        if (obj instanceof String s) {
            return s.isEmpty();
        }
        return true;
    }

    /**
     * Non-strict lookup: returns null when anything is missing or mismatched.
     */
    public static Object getValue(Object root, String path) {
        return getValue(root, path, false);
    }

    /**
     * Strict lookup: throws JsonPathException on missing keys, bad types, or
     * OOB indices.
     */
    public static Object getValueStrict(Object root, String path) {
        return getValue(root, path, true);
    }

    /**
     * Typed helper: returns defaultValue when null or type-mismatch
     * (non-strict).
     */
    public static <T> T getAs(Object root, String path, Class<T> type, T defaultValue) {
        Object v = getValue(root, path, false);
        return type.isInstance(v) ? type.cast(v) : defaultValue;
    }

    // ---- Internal traversal ----
    private static Object getValue(Object root, String path, boolean strict) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        int i = 0, n = path.length();
        Object current = root;

        while (i < n) {
            char c = path.charAt(i);

            // Skip dots between segments
            if (c == '.') {
                i++;
                continue;
            }

            if (c == '[') {
                // [ index ]
                i++;
                i = skipWs(path, i, n);
                int start = i;
                boolean neg = false;
                if (i < n && path.charAt(i) == '-') {
                    neg = true;
                    i++;
                }
                if (i >= n || !Character.isDigit(path.charAt(i))) {
                    return failOrNull(strict, "Expected digit in array index at pos " + i, current);
                }
                long idx = 0;
                while (i < n && Character.isDigit(path.charAt(i))) {
                    idx = idx * 10 + (path.charAt(i) - '0');
                    i++;
                }
                if (neg) {
                    idx = -idx;
                }
                i = skipWs(path, i, n);
                if (i >= n || path.charAt(i) != ']') {
                    return failOrNull(strict, "Missing closing ']' for array index at pos " + i, current);
                }
                i++; // consume ']'
                current = stepIndex(current, idx, strict);
                continue;
            }

            // name segment until '.' or '['
            int start = i;
            while (i < n) {
                char ch = path.charAt(i);
                if (ch == '.' || ch == '[') {
                    break;
                }
                i++;
            }
            if (start == i) {
                // empty segment like ".." -> ignore
                continue;
            }
            String key = path.substring(start, i);
            current = stepKey(current, key, strict);
        }

        return current;
    }

    private static Object stepKey(Object current, String key, boolean strict) {
        if (current instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v == null && strict && !map.containsKey(key)) {
                throw new JsonPathException("Missing key '" + key + "' in object");
            }
            return v;
        }
        return failOrNull(strict, "Tried to get key '" + key + "' on non-object ("
                + typeName(current) + ")", current);
    }

    private static Object stepIndex(Object current, long idx, boolean strict) {
        if (!(current instanceof ArrayDef list)) {
            return failOrNull(strict, "Tried to index [" + idx + "] on non-array ("
                    + typeName(current) + ")", current);
        }
        if (idx < 0 || idx >= list.size()) {
            return failOrNull(strict, "Index [" + idx + "] out of bounds (size=" + list.size() + ")", current);
        }
        return list.get((int) idx);
    }

    private static Object failOrNull(boolean strict, String msg, Object at) {
        if (strict) {
            throw new JsonPathException(msg);
        }
        return null;
    }

    private static int skipWs(String s, int i, int n) {
        while (i < n && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static String typeName(Object o) {
        return (o == null) ? "null" : o.getClass().getSimpleName();
    }

// --- Public setter-like APIs ---
    /**
     * Non-strict set: creates missing parents, expands arrays by padding nulls.
     * Returns root.
     */
    public static Object setValue(Object root, String path, Object newValue) {
        return setValueInternal(root, path, newValue, false);
    }

    /**
     * Strict set: throws on missing parents, type mismatch, or OOB index.
     * Returns root.
     */
    public static Object setValueStrict(Object root, String path, Object newValue) {
        return setValueInternal(root, path, newValue, true);
    }

    /**
     * Remove value at path (object key or array index). Returns root (mutated).
     * Non-strict: no-op if path missing.
     */
    public static Object remove(Object root, String path) {
        setOrRemove(root, path, null, false, /*doRemove*/ true);
        return root;
    }

    /**
     * Append a value to the array at arrayPath (non-strict: creates parent if
     * missing). Returns root.
     */
    public static Object add(Object root, String arrayPath, Object value) {
        Object parent = getOrCreateContainer(root, arrayPath, false, /*toArray*/ true);
        if (!(parent instanceof ArrayDef list)) {
            throw new JsonPathException("Path '" + arrayPath + "' is not an array");
        }
        ((ArrayDef) list).add(value);
        return root;
    }

    /**
     * Insert a value into the array at arrayPath and index. Non-strict: pads
     * with nulls when index > size.
     */
    public static Object insert(Object root, String arrayPath, int index, Object value) {
        Object parent = getOrCreateContainer(root, arrayPath, false, /*toArray*/ true);
        if (!(parent instanceof ArrayDef list)) {
            throw new JsonPathException("Path '" + arrayPath + "' is not an array");
        }
        ArrayDef l = (ArrayDef) list;
        if (index < 0) {
            throw new JsonPathException("Negative index " + index);
        }
        if (index > l.size()) {
            // pad to index with nulls
            for (int i = l.size(); i < index; i++) {
                l.add(null);
            }
            l.add(value);
        } else {
            l.add(index, value);
        }
        return root;
    }

// --- Core internal helpers ---
    private static Object setValueInternal(Object root, String path, Object newValue, boolean strict) {
        return setOrRemove(root, path, newValue, strict, /*doRemove*/ false);
    }

    /**
     * Sets or removes the value at path. Returns root.
     */
    private static Object setOrRemove(Object root, String path, Object newValue, boolean strict, boolean doRemove) {
        if (path == null || path.isEmpty()) {
            // path refers to root itself -> for remove: clear? Instead, forbid for safety.
            if (doRemove) {
                throw new JsonPathException("Cannot remove the root value");
            }
            // For set, replacing root reference is a caller concern, not here.
            throw new JsonPathException("Setting the root value directly is not supported here.");
        }

        // We need to traverse to PARENT of the final segment, then apply the last step.
        int i = 0, n = path.length();
        Object current = root;
        Object parent = null;
        // How to put into parent at the current step:
        enum Kind {
            KEY, INDEX
        }
        Kind lastKind = null;
        String lastKey = null;
        long lastIndex = -1;

        while (i < n) {
            char c = path.charAt(i);
            if (c == '.') {
                i++;
                continue;
            }

            if (c == '[') {
                // array index segment
                i++;
                i = skipWs(path, i, n);
                boolean neg = false;
                if (i < n && path.charAt(i) == '-') {
                    if (strict) {
                        throw new JsonPathException("Negative index not allowed");
                    }
                    neg = true;
                    i++;
                }
                if (i >= n || !Character.isDigit(path.charAt(i))) {
                    if (strict) {
                        throw new JsonPathException("Expected digit in array index at pos " + i);
                    }
                    return root; // non-strict: treat as no-op
                }
                long idx = 0;
                while (i < n && Character.isDigit(path.charAt(i))) {
                    idx = idx * 10 + (path.charAt(i) - '0');
                    i++;
                }
                if (neg) {
                    idx = -idx;
                }
                i = skipWs(path, i, n);
                if (i >= n || path.charAt(i) != ']') {
                    if (strict) {
                        throw new JsonPathException("Missing ']' at pos " + i);
                    }
                    return root;
                }
                i++; // consume ']'

                // If there are more segments to go, we need to ensure child container exists
                boolean atLeaf = !hasMoreSegments(path, i);

                // Ensure current is an array (List)
                if (!(current instanceof ArrayDef)) {
                    if (strict) {
                        throw new JsonPathException("Expected array at segment '[" + idx + "]' but found " + typeName(current));
                    }
                    // Create array and attach to parent if possible
                    ArrayDef created = new ArrayDynamic(DataType.ANY);
                    attachIntoParent(parent, lastKind, lastKey, lastIndex, created, strict);
                    current = created;
                }

                // Ensure index exists (non-strict may pad)
                ArrayDef list = (ArrayDef) current;
                if (idx < 0) {
                    throw new JsonPathException("Negative index " + idx);
                }
                if (idx >= list.size()) {
                    if (strict) {
                        throw new JsonPathException("Index [" + idx + "] out of bounds (size=" + list.size() + ")");
                    }
                    // pad with nulls up to idx
                    for (int pad = list.size(); pad <= (int) idx; pad++) {
                        list.add(null);
                    }
                }

                parent = current;
                lastKind = Kind.INDEX;
                lastIndex = idx;

                // If leaf, perform set/remove; else step into child
                if (atLeaf) {
                    if (doRemove) {
                        list.remove((int) lastIndex); // shifts left
                    } else {
                        list.set((int) lastIndex, newValue);
                    }
                    return root;
                } else {
                    current = list.get((int) idx);
                    // If null on non-strict and more segments remain, create next container when needed:
                    if (current == null && !strict) {
                        // Peek next
                        int j = skipDots(path, i);
                        char next = (j < n) ? path.charAt(j) : '\0';
                        Object created = (next == '[') ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                        list.set((int) idx, created);
                        current = created;
                    }
                }
                continue;
            }

            // KEY segment (identifier until '.' or '[')
            int start = i;
            while (i < n) {
                char ch = path.charAt(i);
                if (ch == '.' || ch == '[') {
                    break;
                }
                i++;
            }
            if (start == i) {
                continue; // empty segment due to consecutive dots
            }
            String key = path.substring(start, i);
            boolean atLeaf = !hasMoreSegments(path, i);

            // Ensure current is an object (Map)
            if (!(current instanceof Map<?, ?>)) {
                if (strict) {
                    throw new JsonPathException("Expected object at segment '" + key + "' but found " + typeName(current));
                }
                Map<String, Object> created = new java.util.LinkedHashMap<>();
                attachIntoParent(parent, lastKind, lastKey, lastIndex, created, strict);
                current = created;
            }

            Map<String, Object> map = (Map<String, Object>) current;
            parent = current;
            lastKind = Kind.KEY;
            lastKey = key;

            if (atLeaf) {
                if (doRemove) {
                    map.remove(key);
                } else {
                    map.put(key, newValue);
                }
                return root;
            } else {
                Object child = map.get(key);
                if (child == null && !strict) {
                    // Peek next to decide container type
                    int j = skipDots(path, i);
                    char next = (j < n) ? path.charAt(j) : '\0';
                    child = (next == '[') ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                    map.put(key, child);
                } else if (child == null && strict) {
                    throw new JsonPathException("Missing object key '" + key + "'");
                }
                current = child;
            }
        }

        return root;
    }

    private static int skipDots(String s, int i) {
        while (i < s.length() && s.charAt(i) == '.') {
            i++;
        }
        return i;
    }

    private static boolean hasMoreSegments(String s, int i) {
        int n = s.length();
        while (i < n && s.charAt(i) == '.') {
            i++;
        }
        return i < n;
    }

    @SuppressWarnings("unchecked")
    private static void attachIntoParent(Object parent, Enum<?> kind, String key, long idx, Object child, boolean strict) {
        if (parent == null) {
            // Replacing the root container is a caller concern; practically, the variable holding root
            // must receive the returned root reference. Here we do nothing.
            return;
        }
        if (kind == null) {
            return;
        }
        switch (kind.name()) {
            case "KEY" -> {
                ((Map<String, Object>) parent).put(key, child);
            }
            case "INDEX" -> {
                ArrayDef list = (ArrayDef) parent;
                int i = (int) idx;
                if (i < 0) {
                    throw new JsonPathException("Negative index " + i);
                }
                if (i >= list.size()) {
                    if (strict) {
                        throw new JsonPathException("Index [" + i + "] out of bounds attaching child (size=" + list.size() + ")");
                    }
                    for (int pad = list.size(); pad <= i; pad++) {
                        list.add(null);
                    }
                }
                list.set(i, child);
            }
        }
    }

    /**
     * Traverse to the container designated by 'path' and return it. - If strict
     * == false: create missing parents on the way. - If strict == true : throw
     * when a required parent is missing, type mismatch, or index is OOB. - If
     * toArray == true: the target (final node) must be a List (array
     * container). - If toArray == false: the target (final node) must be a Map
     * (object container).
     *
     * Examples: getOrCreateContainer(root, "user.tags", false, true) -> returns
     * the List at user.tags, creating it if missing getOrCreateContainer(root,
     * "state[3].city", true, false) -> returns the Map at state[3].city
     * (strict: must exist)
     */
    @SuppressWarnings("unchecked")
    private static Object getOrCreateContainer(Object root, String path, boolean strict, boolean toArray) {
        if (path == null || path.isEmpty()) {
            // Path refers to the root container itself. Validate its type.
            if (toArray) {
                if (root instanceof ArrayDef) {
                    return root;
                }
                if (strict) {
                    throw new JsonPathException("Root is not an array (found " + typeName(root) + ")");
                }
                // Non-strict: if root is not an array, do not replace the caller's root reference here.
                // Caller must rebind the variable if they truly want a new root.
                ArrayDef created = new ArrayDynamic(DataType.ANY);
                return created;
            } else {
                if (root instanceof Map<?, ?>) {
                    return root;
                }
                if (strict) {
                    throw new JsonPathException("Root is not an object (found " + typeName(root) + ")");
                }
                Map<String, Object> created = new java.util.LinkedHashMap<>();
                return created;
            }
        }

        int i = 0, n = path.length();
        Object current = root;
        Object parent = null;

        // Track how 'current' is attached to 'parent'
        enum Kind {
            KEY, INDEX
        }
        Kind lastKind = null;
        String lastKey = null;
        long lastIndex = -1;

        while (i < n) {
            char c = path.charAt(i);

            // Skip dot separators
            if (c == '.') {
                i++;
                continue;
            }

            if (c == '[') {
                // ===== Array index segment =====
                i++;
                i = skipWs(path, i, n);

                // Read index
                boolean neg = false;
                if (i < n && path.charAt(i) == '-') {
                    if (strict) {
                        throw new JsonPathException("Negative index not allowed at pos " + i);
                    }
                    neg = true;
                    i++;
                }
                if (i >= n || !Character.isDigit(path.charAt(i))) {
                    if (strict) {
                        throw new JsonPathException("Expected digit in array index at pos " + i);
                    }
                    return current; // non-strict: abort gracefully
                }
                long idx = 0;
                while (i < n && Character.isDigit(path.charAt(i))) {
                    idx = idx * 10 + (path.charAt(i) - '0');
                    i++;
                }
                if (neg) {
                    idx = -idx;
                }

                i = skipWs(path, i, n);
                if (i >= n || path.charAt(i) != ']') {
                    if (strict) {
                        throw new JsonPathException("Missing closing ']' at pos " + i);
                    }
                    return current;
                }
                i++; // consume ']'

                // Ensure current is a List
                if (!(current instanceof ArrayDef)) {
                    if (strict) {
                        throw new JsonPathException("Expected array at '" + renderPath(parent, lastKind, lastKey, lastIndex)
                                + "' but found " + typeName(current));
                    }
                    // Create a List and attach it into parent (if any)
                    ArrayDef created = new ArrayDynamic(DataType.ANY);
                    attachIntoParent(parent, lastKind, lastKey, lastIndex, created, /*strict*/ false);
                    current = created;
                }

                ArrayDef list = (ArrayDef) current;
                if (idx < 0) {
                    throw new JsonPathException("Negative index " + idx);
                }
                if (idx >= list.size()) {
                    if (strict) {
                        throw new JsonPathException("Index [" + idx + "] out of bounds (size=" + list.size() + ")");
                    }
                    // Non-strict: pad with nulls up to idx
                    for (int pad = list.size(); pad <= (int) idx; pad++) {
                        list.add(null);
                    }
                }

                parent = current;
                lastKind = Kind.INDEX;
                lastIndex = idx;

                boolean atLeaf = !hasMoreSegments(path, i);
                if (atLeaf) {
                    // Endpoint must itself be a container with requested kind
                    Object leaf = list.get((int) idx);
                    if (leaf == null) {
                        Object created = toArray ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                        list.set((int) idx, created);
                        return created;
                    }
                    // Validate type
                    if (toArray) {
                        if (!(leaf instanceof ArrayDef)) {
                            if (strict) {
                                throw new JsonPathException("Target at " + renderPath(parent, lastKind, lastKey, lastIndex) + " is not an array");
                            }
                            // Non-strict: replace with array
                            ArrayDef created = new ArrayDynamic(DataType.ANY);
                            list.set((int) idx, created);
                            return created;
                        }
                    } else {
                        if (!(leaf instanceof Map<?, ?>)) {
                            if (strict) {
                                throw new JsonPathException("Target at " + renderPath(parent, lastKind, lastKey, lastIndex) + " is not an object");
                            }
                            Map<String, Object> created = new java.util.LinkedHashMap<>();
                            list.set((int) idx, created);
                            return created;
                        }
                    }
                    return leaf;
                } else {
                    // Step into child; create if null (non-strict) according to next segment
                    Object child = list.get((int) idx);
                    if (child == null && !strict) {
                        int j = skipDots(path, i);
                        char next = (j < n) ? path.charAt(j) : '\0';
                        Object created = (next == '[') ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                        list.set((int) idx, created);
                        current = created;
                    } else if (child == null && strict) {
                        throw new JsonPathException("Missing child container at index [" + idx + "]");
                    } else {
                        current = child;
                    }
                }
                continue;
            }

            // ===== Object key segment =====
            int start = i;
            while (i < n) {
                char ch = path.charAt(i);
                if (ch == '.' || ch == '[') {
                    break;
                }
                i++;
            }
            if (start == i) {
                continue; // empty segment due to consecutive dots
            }
            String key = path.substring(start, i);

            // Ensure current is a Map
            if (!(current instanceof Map<?, ?>)) {
                if (strict) {
                    throw new JsonPathException("Expected object at '" + renderPath(parent, lastKind, lastKey, lastIndex)
                            + "' but found " + typeName(current));
                }
                Map<String, Object> created = new java.util.LinkedHashMap<>();
                attachIntoParent(parent, lastKind, lastKey, lastIndex, created, /*strict*/ false);
                current = created;
            }

            Map<String, Object> map = (Map<String, Object>) current;
            parent = current;
            lastKind = Kind.KEY;
            lastKey = key;

            boolean atLeaf = !hasMoreSegments(path, i);
            if (atLeaf) {
                Object leaf = map.get(key);
                if (leaf == null) {
                    Object created = toArray ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                    map.put(key, created);
                    return created;
                }
                if (toArray) {
                    if (!(leaf instanceof ArrayDef)) {
                        if (strict) {
                            throw new JsonPathException("Target at '" + key + "' is not an array");
                        }
                        ArrayDef created = new ArrayDynamic(DataType.ANY);
                        map.put(key, created);
                        return created;
                    }
                } else {
                    if (!(leaf instanceof Map<?, ?>)) {
                        if (strict) {
                            throw new JsonPathException("Target at '" + key + "' is not an object");
                        }
                        Map<String, Object> created = new java.util.LinkedHashMap<>();
                        map.put(key, created);
                        return created;
                    }
                }
                return leaf;
            } else {
                Object child = map.get(key);
                if (child == null && !strict) {
                    int j = skipDots(path, i);
                    char next = (j < n) ? path.charAt(j) : '\0';
                    child = (next == '[') ? new ArrayDynamic(DataType.ANY) : new java.util.LinkedHashMap<String, Object>();
                    map.put(key, child);
                } else if (child == null && strict) {
                    throw new JsonPathException("Missing object key '" + key + "'");
                }
                current = child;
            }
        }

        // Should not reach here: loop returns at leaf
        return current;
    }

    /**
     * Renders a human-readable location for diagnostics.
     */
    private static String renderPath(Object parent, Enum<?> kind, String key, long index) {
        if (parent == null || kind == null) {
            return "$";
        }
        return switch (kind.name()) {
            case "KEY" ->
                "$." + key;
            case "INDEX" ->
                "$[" + index + "]";
            default ->
                "$";
        };
    }

    /**
     * Runtime exception for strict lookups.
     */
    public static final class JsonPathException extends RuntimeException {

        public JsonPathException(String message) {
            super(message);
        }
    }
}
