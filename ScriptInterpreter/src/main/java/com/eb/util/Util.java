package com.eb.util;

import com.eb.script.token.DataType;
import com.eb.script.json.Json;
import static com.eb.script.token.DataType.BOOL;
import static com.eb.script.token.DataType.DATE;
import static com.eb.script.token.DataType.DOUBLE;
import static com.eb.script.token.DataType.FLOAT;
import static com.eb.script.token.DataType.INTEGER;
import static com.eb.script.token.DataType.JSON;
import static com.eb.script.token.DataType.LONG;
import static com.eb.script.token.DataType.STRING;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;

/**
 *
 * @author Earl Bosch
 */
public class Util {

    public static final Path SANDBOX_ROOT = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static String defaultFormat = "yyyy-MM-dd HH:mm:ss";
    
    // ThreadLocal to store the current RuntimeContext's source path directory
    private static final ThreadLocal<Path> currentContextSourceDir = new ThreadLocal<>();
    
    /**
     * Sets the source directory for the current executing context.
     * This directory will be considered safe for file operations.
     */
    public static void setCurrentContextSourceDir(Path sourceDir) {
        currentContextSourceDir.set(sourceDir);
    }
    
    /**
     * Clears the current context source directory.
     */
    public static void clearCurrentContextSourceDir() {
        currentContextSourceDir.remove();
    }
    
    /**
     * Gets the current context source directory, if set.
     */
    public static Path getCurrentContextSourceDir() {
        return currentContextSourceDir.get();
    }

    public static boolean checkDataType(DataType expectedType, Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof ArrayDef array) {
            return switch (expectedType) {
                case BOOL ->
                    array.getDataType() == DataType.BOOL;
                case BYTE ->
                    array.getDataType() == DataType.BYTE;
                case BITMAP ->
                    array.getDataType() == DataType.BITMAP;
                case INTEGER, LONG ->
                    array.getDataType() == DataType.INTEGER || array.getDataType() == DataType.LONG;
                case FLOAT, DOUBLE ->
                    array.getDataType() == DataType.FLOAT || array.getDataType() == DataType.DOUBLE;
                case STRING ->
                    array.getDataType() == DataType.STRING;
                case DATE ->
                    array.getDataType() == DataType.DATE;
                case RECORD ->
                    array.getDataType() == DataType.RECORD;
                case MAP ->
                    array.getDataType() == DataType.MAP;
                case IMAGE ->
                    array.getDataType() == DataType.IMAGE;
                case JSON ->
                    true;
                case ANY ->
                    array.getDataType() != null;
                case ARRAY ->
                    true;
                default ->
                    false;
            };
        } else {
            return switch (expectedType) {
                case BOOL ->
                    value instanceof Boolean;
                case BYTE ->
                    value instanceof Byte;
                case INTEGER, LONG ->
                    value instanceof Integer || value instanceof Long;
                case FLOAT, DOUBLE ->
                    value instanceof Float || value instanceof Double;
                case STRING ->
                    value instanceof String;
                case DATE ->
                    value instanceof java.time.LocalDate || value instanceof java.time.LocalDateTime || value instanceof Date;
                case JSON ->
                    value instanceof Map || value instanceof List || value instanceof ArrayDef;
                case MAP ->
                    value instanceof Map;
                case QUEUE ->
                    value instanceof com.eb.script.arrays.QueueDef;
                case IMAGE ->
                    value instanceof EbsImage;
                case ANY ->
                    true;
                default ->
                    false;
            };
        }
    }

    public static String stringBoolean(Boolean b) {
        if (b) {
            return "Y";
        } else if (!b) {
            return "N";
        }
        return null;
    }

    public static int intBoolean(Boolean b) {
        if (b) {
            return 1;
        } else {
            return 0;
        }
    }

    public static Boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return null;
    }

    public static String asString(Object v) {
        return (v == null) ? null : String.valueOf(v);
    }

    public static String notNull(String s) {               // normalize to non-null
        return (s == null) ? "" : s;
    }

    public static boolean strEq(String a, String b) { // null-safe equals
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static boolean strEqIgnore(String a, String b) {
        return (a == null) ? (b == null) : a.equalsIgnoreCase(b);
    }

    public static boolean isBlank(String s) {
        if (s == null) {
            return true;             // treat null as blank
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Object> httpRequestCore(
            String url,
            String method,
            String body,
            Number timeout) {
        return httpRequest(url, method, body, null, timeout);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> httpRequest(
            String url,
            String method,
            String body,
            Map<?, ?> headers,
            Number timeout) {
        if (url == null) {
            throw new RuntimeException("http.request: url cannot be null");
        }
        try {
            URI initialUri = URI.create(url);
            String initialScheme = initialUri.getScheme() == null ? "" : initialUri.getScheme().toLowerCase();
            if (!initialScheme.equals("http") && !initialScheme.equals("https")) {
                throw new RuntimeException("http.request: only http/https schemes are allowed");
            }

            // If you ALSO want to allow HTTPS -> HTTP redirect downgrades, set to true:
            final boolean ALLOW_HTTPS_DOWNGRADE = false;

            // ---- Secure defaults for HTTPS connections
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            javax.net.ssl.SSLParameters sslParams = new javax.net.ssl.SSLParameters();
            sslParams.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            sslParams.setEndpointIdentificationAlgorithm("HTTPS"); // hostname verification

            HttpClient baseClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParams)
                    .followRedirects(HttpClient.Redirect.NEVER) // manual, so we can enforce policy
                    .build();

            // ---- Method handling
            String m = (method == null || method.isBlank()) ? "GET" : method.toUpperCase();
            switch (m) {
                case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD" -> {
                }
                default ->
                    throw new RuntimeException("http.request: unsupported method '" + m + "'");
            }

            final long timeoutMs = (timeout == null ? 30_000L : Math.max(0L, timeout.longValue()));
            final byte[] bytes = (body == null ? new byte[0] : body.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            java.util.function.Function<URI, HttpRequest.Builder> rbFactory = (URI u)
                    -> HttpRequest.newBuilder()
                            .uri(u)
                            .timeout(java.time.Duration.ofMillis(timeoutMs));

            // ---- Apply headers helper
            java.util.function.Consumer<HttpRequest.Builder> applyHeaders = (HttpRequest.Builder rb) -> {
                if (headers instanceof Map<?, ?> map) {
                    for (var e : map.entrySet()) {
                        Object hk = e.getKey();
                        Object hv = e.getValue();
                        if (hk != null && hv != null) {
                            rb.header(String.valueOf(hk), String.valueOf(hv));
                        }
                    }
                }
            };

            // ---- Build request per method
            java.util.function.BiFunction<HttpRequest.Builder, String, HttpRequest> buildReq
                    = (rb, methodName) -> switch (methodName) {
                case "GET" ->
                    rb.GET().build();
                case "HEAD" ->
                    rb.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
                case "DELETE" ->
                    (body == null)
                    ? rb.DELETE().build()
                    : rb.method("DELETE", HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                case "POST" ->
                    rb.POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                case "PUT" ->
                    rb.PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                case "PATCH" ->
                    rb.method("PATCH", HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                default ->
                    throw new IllegalStateException("unreachable");
            };

            // ---- Manual redirect loop
            URI current = initialUri;
            String currentMethod = m;
            boolean startedHttps = "https".equals(initialScheme);
            int redirects = 0;
            final int MAX_REDIRECTS = 5;
            long t0 = System.nanoTime();
            HttpResponse<byte[]> resp;

            while (true) {
                HttpRequest.Builder rb = rbFactory.apply(current);
                applyHeaders.accept(rb);
                HttpRequest req = buildReq.apply(rb, currentMethod);

                resp = baseClient.send(req, HttpResponse.BodyHandlers.ofByteArray());

                int sc = resp.statusCode();
                if (sc == 301 || sc == 302 || sc == 303 || sc == 307 || sc == 308) {
                    if (redirects++ >= MAX_REDIRECTS) {
                        throw new RuntimeException("http.request: too many redirects");
                    }
                    String loc = resp.headers().firstValue("Location").orElse(null);
                    if (loc == null || loc.isBlank()) {
                        throw new RuntimeException("http.request: redirect without Location header");
                    }

                    URI next = current.resolve(loc).normalize();
                    String nextScheme = next.getScheme() == null ? "" : next.getScheme().toLowerCase();

                    // Allow both http and https in general…
                    if (!nextScheme.equals("http") && !nextScheme.equals("https")) {
                        throw new RuntimeException("http.request: unsupported redirect scheme " + nextScheme);
                    }
                    // …but prevent HTTPS → HTTP downgrade unless explicitly allowed.
                    if (startedHttps && !"https".equals(nextScheme) && !ALLOW_HTTPS_DOWNGRADE) {
                        throw new RuntimeException("http.request: refusing HTTPS→HTTP redirect to " + next);
                    }

                    // RFC: 303 -> GET; 301/302 often become GET when original wasn't GET/HEAD
                    if (sc == 303 || ((sc == 301 || sc == 302) && !"GET".equals(currentMethod) && !"HEAD".equals(currentMethod))) {
                        currentMethod = "GET";
                    }
                    // 307/308 preserve method
                    current = next;
                    continue;
                }
                break; // final response
            }

            long elapsed = (System.nanoTime() - t0) / 1_000_000L;

            // ---- Build result map
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("url", current.toString());
            result.put("status", resp.statusCode());

            java.util.Map<String, String> outHeaders = new java.util.LinkedHashMap<>();
            resp.headers().map().forEach((k, vals) -> {
                if (vals != null && !vals.isEmpty()) {
                    outHeaders.put(k, vals.get(0));
                }
            });
            result.put("headers", outHeaders);

            String text = new String(resp.body(), java.nio.charset.StandardCharsets.UTF_8);
            result.put("body", text);
            result.put("elapsedMs", elapsed);

            return result;

        } catch (java.net.http.HttpTimeoutException ex) {
            throw new RuntimeException("http.request: timeout - " + ex.getMessage());
        } catch (Exception ex) {
            throw new RuntimeException("http.request: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
    }

    public static String stringListOfArray(Object... values) {
        StringBuilder ret = new StringBuilder();
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(",").append(System.lineSeparator());
            }
            ret.append(value.toString());
        }
        return ret.toString();
    }

    public static String stringLineOfArray(Iterable values) {
        StringBuilder ret = new StringBuilder();
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(", ");
            }
            ret.append(value.toString());
        }
        return ret.toString();
    }

    public static String stringLineOfArray(Object[] values) {
        StringBuilder ret = new StringBuilder();
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(", ");
            }
            if (value == null) {
                ret.append("(null)");
            } else {
                ret.append(value.toString());
            }
        }
        return ret.toString();
    }

    public static String stringListOfArray(Iterable values) {
        StringBuilder ret = new StringBuilder();
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(",").append(System.lineSeparator());
            }
            ret.append(value.toString());
        }
        return ret.toString();
    }

    public static <E extends Object> String stringify(E... values) {
        StringBuilder ret = new StringBuilder();
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(", ");
            }
            if (value instanceof Map || value instanceof List || value instanceof ArrayDef) {
                ret.append(Json.prettyJson(value)); // default indent
            } else if (value instanceof ArrayFixedByte array) {
                ret.append(new String(array.elements));
            } else if (value instanceof ArrayFixed array) {
                ret.append(stringify(array.elements));
            } else if (value instanceof ArrayDynamic array) {
                ret.append(stringify(array.elements));
            } else {
                ret.append(value == null ? "null" : value.toString());
            }
        }
        return ret.toString();
    }

    public static String stringifyShort(Object... values) {
        StringBuilder ret = new StringBuilder();
        String s = "";
        for (Object value : values) {
            if (!ret.isEmpty()) {
                ret.append(", ");
            }
            if (value instanceof Map || value instanceof List || value instanceof ArrayDef) {
                s = Json.prettyJson(value); // default indent
                int len = s.length();
                if (len > 45) {
                    s = s.substring(0, 45) + "...";
                    s = "\"" + s + "\"(" + len + ")";
                } else {
                    s = "\"" + s + "\"";
                }
            } else if (value instanceof ArrayFixedByte array) {
                s = new String(array.elements);
            } else if (value instanceof ArrayFixed array) {
                s = stringify(array.elements);
            } else if (value instanceof ArrayDynamic array) {
                s = stringify(array.elements);
            } else if (value instanceof String str) {
                s = str;
                int len = s.length();
                if (s.length() > 45) {
                    s = s.substring(0, 45) + "...";
                    s = "\"" + s + "\"(" + len + ")";
                } else {
                    s = "\"" + s + "\"";
                }
            } else {
                s = (value == null) ? "null" : value.toString();
            }

            ret.append(s);
        }
        return ret.toString();
    }

    public static Path resolveSandboxedPath(String path) {
        if (path != null) {
            Path p = SANDBOX_ROOT.resolve(path).normalize();
            if (!p.startsWith(SANDBOX_ROOT)) {
                // Check if path is in a safe directory
                if (!isInSafeDirectory(p)) {
                    throw new RuntimeException("Path escapes sandbox: " + path);
                }
            }
            return p;
        } else {
            return SANDBOX_ROOT;
        }
    }

    /**
     * Checks if a path is within one of the configured safe directories.
     * Safe directories are configured through the Safe Directories dialog in the Config menu.
     * Also checks if the path is within the directory of the currently executing script.
     */
    private static boolean isInSafeDirectory(Path path) {
        try {
            Path absolutePath = path.toAbsolutePath().normalize();
            
            // Check if path is within the current context's source directory
            Path contextSourceDir = getCurrentContextSourceDir();
            if (contextSourceDir != null) {
                Path contextDir = contextSourceDir.toAbsolutePath().normalize();
                if (absolutePath.startsWith(contextDir)) {
                    return true;
                }
            }
            
            // Use reflection to avoid hard dependency on UI class
            Class<?> dialogClass = Class.forName("com.eb.ui.ebs.SafeDirectoriesDialog");
            java.lang.reflect.Method method = dialogClass.getMethod("getSafeDirectories");
            @SuppressWarnings("unchecked")
            java.util.List<String> safeDirs = (java.util.List<String>) method.invoke(null);
            
            for (String safeDirStr : safeDirs) {
                Path safeDir = Path.of(safeDirStr).toAbsolutePath().normalize();
                if (absolutePath.startsWith(safeDir)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If we can't load safe directories (e.g., class not found in non-UI mode),
            // just return false and enforce sandbox
            return false;
        }
        return false;
    }

    public static String formatExceptionWith2Origin(Throwable ex) {
        // First non-JDK frame (skip java.*, jdk.*, etc.)
        StackTraceElement[] origin = ex.getStackTrace();
        String where = (origin == null)
                ? "unknown"
                : origin[0].getClassName() + "." + origin[0].getMethodName()
                + "(" + origin[0].getFileName() + ":" + origin[0].getLineNumber() + "),\n "
                + origin[1].getClassName() + "." + origin[1].getMethodName()
                + "(" + origin[1].getFileName() + ":" + origin[1].getLineNumber() + ")";
        return "[" + where + "]\n  " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    public static String formatExceptionWithOrigin(Throwable ex) {
        // First non-JDK frame (skip java.*, jdk.*, etc.)
        StackTraceElement origin = java.util.Arrays.stream(ex.getStackTrace())
                .filter(el -> {
                    String c = el.getClassName();
                    return !(c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("sun."));
                })
                .findFirst()
                .orElse(ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : null);

        String where = (origin == null)
                ? "unknown"
                : origin.getClassName() + "." + origin.getMethodName()
                + "(" + origin.getFileName() + ":" + origin.getLineNumber() + ")";

        return "[" + where + "] " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

// Rank for numeric promotion
    public static int numericRank(DataType t) {
        // smaller = narrower
        return switch (t) {
            case BYTE ->
                0;
            case INTEGER ->
                1;
            case LONG ->
                2;
            case FLOAT ->
                3;
            case DOUBLE ->
                4;
            default ->
                -1; // non-numeric
        };
    }

    public static boolean isNumeric(DataType t) {
        return numericRank(t) >= 0;
    }

    public static void setDefaultDateFormat(String defaultFormat) {
        Util.defaultFormat = defaultFormat;
    }

    public static String formatDate(FileTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(defaultFormat).withZone(ZoneId.systemDefault());
        return formatter.format(dateTime.toInstant());
    }

    /**
     * Splits an argument line into tokens, allowing double-quoted segments.
     * Examples: foo "bar baz" qux -> [foo, bar baz, qux] "C:\Program Files\X" a
     * -> [C:\Program Files\X, a] Supports escapes: \" becomes ", \\ becomes \
     */
    public static String[] splitArgsAllowingQuotes(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) {
            return new String[0];
        }

        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaping) {
                // Accept any escaped char literally
                cur.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {           // start escape
                escaping = true;
                continue;
            }

            if (c == '"') {            // toggle quote mode
                inQuotes = !inQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !inQuotes) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }

            cur.append(c);
        }

        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out.toArray(String[]::new);
    }

    public static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
