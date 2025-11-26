package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;

import com.eb.script.json.Json;
import com.eb.util.Util;
import java.util.Map;

/**
 * Built-in functions for HTTP operations.
 * Handles all http.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsHttp {

    /**
     * Dispatch an HTTP builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "http.get")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "http.request" -> request(args);
            case "http.get" -> get(args);
            case "http.post" -> post(args);
            case "http.gettext" -> getText(args);
            case "http.posttext" -> postText(args);
            case "http.getjson" -> getJson(args);
            case "http.postjson" -> postJson(args);
            case "http.ensure2xx" -> ensure2xx(args);
            case "http.is2xx" -> is2xx(args);
            default -> throw new InterpreterError("Unknown HTTP builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is an HTTP builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("http.");
    }

    // --- Individual builtin implementations ---

    @SuppressWarnings("unchecked")
    private static Object request(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        String method = (String) (args.length > 1 ? args[1] : null);
        String body = (String) (args.length > 2 ? args[2] : null);
        Object h = (args.length > 3 ? args[3] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object get(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        Object h = (args.length > 1 ? args[1] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object post(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        String body = (String) (args.length > 1 ? args[1] : null);
        Object h = (args.length > 2 ? args[2] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object getText(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        Object h = (args.length > 1 ? args[1] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object postText(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        String body = (String) args[1];
        Object h = (args.length > 2 ? args[2] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object getJson(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        Object h = (args.length > 1 ? args[1] : null);
        Map<String, Object> headers = null;
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

    @SuppressWarnings("unchecked")
    private static Object postJson(Object[] args) throws InterpreterError {
        String url = (String) args[0];
        Object jsonBody = args[1];
        Object h = (args.length > 2 ? args[2] : null);
        Map<String, Object> headers = null;
        if (h instanceof Map m) {
            headers = m;
        }
        Number timeout = (Number) (args.length > 3 ? args[3] : null);
        
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

    private static Object ensure2xx(Object[] args) throws InterpreterError {
        Object in = args[0];
        String msg = (args.length > 1 ? (String) args[1] : null);

        int status = httpExtractStatus(in);
        boolean ok = status >= 200 && status <= 299;

        if (!ok) {
            String base = "HTTP request failed with status " + status;
            throw new InterpreterError((msg == null || msg.isBlank()) ? base : (base + " : " + msg));
        }
        return status;
    }

    private static Object is2xx(Object[] args) {
        Object in = args[0];
        int status = httpExtractStatus(in);
        return (status >= 200 && status <= 299);
    }

    // --- Helper methods ---

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
}
