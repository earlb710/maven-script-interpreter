package com.eb.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * LogStructureProbe
 *
 * - Detects whether the log is JSON-line based or delimited text. - For
 * delimited text, tries multiple delimiter strategies and chooses the most
 * consistent one. - Estimates the typical number of columns. - Classifies
 * columns (or JSON keys) as date, status, location, or message.
 *
 * Pure Java 11+, no external dependencies.
 */
public class LogStructureProbe {

    // ========= Configuration defaults =========
    private static final int DEFAULT_MAX_LINES = 2_000;   // sample up to this many non-empty lines
    private static final int MIN_NONEMPTY_FOR_DECISION = 50;
    private static final int DEFAULT_MIN_LINE_LENGTH = 1;  // ignore very short lines
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    // ========= Delimiter strategies =========
    enum DelimStrategy {
        WHITESPACE, WHITESPACE_TAIL, TAB, COMMA, PIPE, SEMICOLON, JAVA_LOGBACK
    }

    // Central registry for all regex patterns
    private static final Map<String, Pattern> regexPatterns = new HashMap<>();

    // Convenience accessor with a clear failure if a key is missing
    private static Pattern re(String key) {
        Pattern p = regexPatterns.get(key);
        if (p == null) {
            throw new IllegalArgumentException("Unknown regex key: " + key);
        }
        return p;
    }

    // Populate once, at class load time
    static {
        // ===== Java/Logback line =====
        regexPatterns.put("JAVA_LOGBACK_LINE", Pattern.compile(
                "^"
                + "(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+" // 1: timestamp
                + "(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+" // 2: level
                + "\\[(.*?)\\]\\s+" // 3: logger/class (non-greedy)
                + "\\((.*)\\)\\s+" // 4: thread/context (greedy)
                + "(.*)$" // 5: message
        ));

        // ===== Dates/times =====
        regexPatterns.put("ISO_TS", Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+\\-]\\d{2}:?\\d{2})?\\b"));
        regexPatterns.put("ISO_DATE_ONLY", Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b"));
        regexPatterns.put("DMY_SLASH", Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b")); // 31/12/2024
        regexPatterns.put("APACHE_CLF", Pattern.compile("\\b\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+\\-]\\d{4}\\b")); // 10/Oct/2000:13:55:36 -0700
        regexPatterns.put("SYSLOG_TS", Pattern.compile("\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\b"));
        regexPatterns.put("EPOCH_10S", Pattern.compile("\\b\\d{10}(?:\\.\\d+)?\\b")); // seconds since epoch
        regexPatterns.put("EPOCH_13MS", Pattern.compile("\\b\\d{13}\\b"));            // milliseconds since epoch
        regexPatterns.put("LOG4J_TS", Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}\\b")); // 2025-10-22 00:03:35,241

        // ===== Status/levels =====
        regexPatterns.put("LOG_LEVEL", Pattern.compile("\\b(?:TRACE|DEBUG|INFO|NOTICE|WARN|WARNING|ERROR|ERR|SEVERE|FATAL|CRITICAL)\\b",
                Pattern.CASE_INSENSITIVE));
        regexPatterns.put("HTTP_STATUS", Pattern.compile("\\b(?:[1-5]\\d\\d)\\b"));
        regexPatterns.put("GENERIC_STATUS", Pattern.compile("\\b(?:SUCCESS|SUCCEEDED|OK|FAIL|FAILED|TIMEOUT|RETRY|DENIED|ALLOWED)\\b",
                Pattern.CASE_INSENSITIVE));

        // ===== Location =====
        regexPatterns.put("IPV4", Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"));
        regexPatterns.put("IPV6", Pattern.compile("\\b(?:(?:[0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4})\\b"));
        regexPatterns.put("URL", Pattern.compile("\\bhttps?://[^\\s]+\\b", Pattern.CASE_INSENSITIVE));
        regexPatterns.put("UNIX_PATH", Pattern.compile("(^/|\\s/)[^\\s]+"));
        regexPatterns.put("WINDOWS_PATH", Pattern.compile("\\b[A-Za-z]:\\\\[^\\s]+"));
        regexPatterns.put("JAVA_FQN", Pattern.compile("\\b(?:[a-zA-Z_]\\w*\\.)+[A-Z][A-Za-z0-9_]*(?::\\d+)?\\b"));

        // ===== Stack traces =====
        regexPatterns.put("EXCEPTION_HEADER", Pattern.compile(
                "\\b([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w+)*\\.(?:Exception|Error))\\b(?::\\s.*)?"
        ));
        regexPatterns.put("STACK_FRAME", Pattern.compile(
                "^\\s*at\\s+[a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*\\([^)]*\\)\\s*$"
        ));
        regexPatterns.put("CAUSED_BY", Pattern.compile(
                "^\\s*Caused by:\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w+)*\\.(?:Exception|Error))\\b(?::\\s.*)?\\s*$"
        ));
        regexPatterns.put("SUPPRESSED", Pattern.compile(
                "^\\s*Suppressed:\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w+)*\\.(?:Exception|Error))\\b(?::\\s.*)?\\s*$"
        ));

        // ===== Thread/context =====
        regexPatterns.put("THREAD_TOKEN", Pattern.compile("\\bthread\\b\\s*#?\\d+", Pattern.CASE_INSENSITIVE));
        regexPatterns.put("JAVA_THREAD_NAME", Pattern.compile(
                "\\b(?:main|ForkJoinPool-\\d+(?:-worker-\\d+)?|http[\\- ]?nio-\\d+-exec-\\d+|OkHttp|Finalizer|Reference\\s*Handler|Sign 77al\\s*Dispatcher|Timer-\\d+|Batch\\s*Thread\\s*-\\s*\\d+)\\b",
                Pattern.CASE_INSENSITIVE
        ));
        regexPatterns.put("CAMEL_THREAD", Pattern.compile("\\bCamel\\b\\s*\\(camel-\\d+\\)", Pattern.CASE_INSENSITIVE));
        regexPatterns.put("EXECUTOR_THREAD", Pattern.compile("\\b(?:pool-\\d+-thread-\\d+|executor-\\d+|_POOL|ForkJoinPool)\\b",
                Pattern.CASE_INSENSITIVE));
        regexPatterns.put("PAREN_CONTEXT", Pattern.compile("^\\(.*\\)$"));
        regexPatterns.put("GENERIC_URI", Pattern.compile("\\b[a-zA-Z][a-zA-Z0-9+\\-.]*://\\S+\\b"));
    }

    // Logback/Log4j regex (your file uses this form)
    private static final Pattern JAVA_LOGBACK_LINE = re("JAVA_LOGBACK_LINE");
    // ========= Regex patterns for classification =========
    // Dates/times
    private static final Pattern ISO_TS = re("ISO_TS");
    private static final Pattern ISO_DATE_ONLY = re("ISO_DATE_ONLY");
    private static final Pattern DMY_SLASH = re("DMY_SLASH");
    private static final Pattern APACHE_CLF = re("APACHE_CLF");
    private static final Pattern SYSLOG_TS = re("SYSLOG_TS");
    private static final Pattern EPOCH_10S = re("EPOCH_10S");
    private static final Pattern EPOCH_13MS = re("EPOCH_13MS");
    private static final Pattern LOG4J_TS = re("LOG4J_TS");

    // Status/levels
    private static final Pattern LOG_LEVEL = re("LOG_LEVEL");
    private static final Pattern HTTP_STATUS = re("HTTP_STATUS");
    private static final Pattern GENERIC_STATUS = re("GENERIC_STATUS");

    // Location
    private static final Pattern IPV4 = re("IPV4");
    private static final Pattern IPV6 = re("IPV6");
    private static final Pattern URL = re("URL");
    private static final Pattern UNIX_PATH = re("UNIX_PATH");
    private static final Pattern WINDOWS_PATH = re("WINDOWS_PATH");
    private static final Pattern JAVA_FQN = re("JAVA_FQN");

    // Message heuristic: longest/free text
    // (No explicit regex; computed statistically)
    // JSON detection heuristics
    private static final double JSON_SIGNAL_THRESHOLD = 0.80; // ≥80% of sampled lines look like JSON objects
    private static final Set<String> DATE_KEYS = setOf("timestamp", "time", "@timestamp", "ts", "datetime", "date", "log_ts", "eventTime", "event_time");
    private static final Set<String> STATUS_KEYS = setOf("level", "lvl", "severity", "status", "status_code", "http_status", "result", "outcome");
    private static final Set<String> LOCATION_KEYS = setOf("ip", "client_ip", "remote_addr", "host", "hostname", "path", "url", "uri", "location", "source", "source_ip");
    private static final Set<String> MESSAGE_KEYS = setOf("message", "msg", "log", "log_message", "event", "detail", "details", "description");
    private static final Set<String> THREAD_KEYS = setOf("thread", "thread_name", "thrd", "context", "log_thread");
    // ========= Java stack trace detection =========
    private static final Pattern EXCEPTION_HEADER = re("EXCEPTION_HEADER");

    private static final Pattern STACK_FRAME = re("STACK_FRAME");

    private static final Pattern CAUSED_BY = re("CAUSED_BY");

    private static final Pattern SUPPRESSED = re("SUPPRESSED");

    // ========= Thread/context detection =========
    // e.g., "(Camel (camel-4) thread #159344 - timer://processing)"
    private static final Pattern THREAD_TOKEN = re("THREAD_TOKEN");

    // Common thread naming styles
    private static final Pattern JAVA_THREAD_NAME = re("JAVA_THREAD_NAME");

    // Camel/Spring style
    private static final Pattern CAMEL_THREAD = re("CAMEL_THREAD");
    // Generic executor / pool patterns
    private static final Pattern EXECUTOR_THREAD = re("EXECUTOR_THREAD");

    // Lines with bracketed/parenthesized context
    private static final Pattern PAREN_CONTEXT = re("PAREN_CONTEXT");

    // Non-HTTP URI inside context (e.g., timer://..., jms://..., kafka://...)
    private static final Pattern GENERIC_URI = re("GENERIC_URI");

    // ========= Entry point =========
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[1];
            args[0] = "C:\\Temp\\short.log";
        }

        if (args.length == 0) {
            System.err.println("Usage: java LogStructureProbe <file.log> [--maxLines=N] [--minLineLen=N] [--charset=UTF-8]");
            System.exit(2);
        }

        Path file = Paths.get(args[0]);
        if (!Files.isReadable(file)) {
            System.err.println("File not readable: " + file);
            System.exit(2);
        }

        int maxLines = DEFAULT_MAX_LINES;
        int minLineLen = DEFAULT_MIN_LINE_LENGTH;
        Charset charset = DEFAULT_CHARSET;

        // Parse simple flags
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--maxLines=")) {
                maxLines = Integer.parseInt(a.substring("--maxLines=".length()));
            } else if (a.startsWith("--minLineLen=")) {
                minLineLen = Integer.parseInt(a.substring("--minLineLen=".length()));
            } else if (a.startsWith("--charset=")) {
                charset = Charset.forName(a.substring("--charset=".length()));
            }
        }

        List<String> lines = sampleLines(file, charset, maxLines, minLineLen);

        if (lines.size() < Math.min(MIN_NONEMPTY_FOR_DECISION, 5)) {
            System.out.println("Not enough non-empty lines to analyze (" + lines.size() + ").");
            return;
        }

        boolean looksJson = looksLikeJson(lines);
        if (looksJson) {
            analyzeJsonLogs(lines);
        } else {
            analyzeDelimitedLogs(lines);
        }

        List<StackTraceSpan> spans = detectJavaStackTraces(lines);
        printStackTraceSummary(lines, spans);

    }

    // ========= File sampling =========
    private static List<String> sampleLines(Path file, Charset charset, int maxLines, int minLen) throws IOException {
        List<String> sample = new ArrayList<>(Math.min(maxLines, 20000));
        try (BufferedReader br = Files.newBufferedReader(file, charset)) {
            String s;
            while ((s = br.readLine()) != null) {
                if (s.isEmpty()) {
                    continue;
                }
                if (s.startsWith("#")) {
                    continue; // skip comment lines
                }
                if (s.length() < minLen) {
                    continue;
                }
                sample.add(s);
                if (sample.size() >= maxLines) {
                    break;
                }
            }
        }
        return sample;
    }

    // ========= JSON detection =========
    private static boolean looksLikeJson(List<String> lines) {
        int jsonish = 0, total = 0;
        for (String l : lines) {
            String t = l.trim();
            if (t.isEmpty()) {
                continue;
            }
            total++;
            // Loose heuristic: starts with "{" (optionally after BOM/whitespace), contains ":" and quotes
            if (t.startsWith("{") && t.contains(":") && t.contains("\"")) {
                jsonish++;
            }
        }
        return total > 0 && (jsonish / (double) total) >= JSON_SIGNAL_THRESHOLD;
    }

    // ========= JSON analysis =========
    private static void analyzeJsonLogs(List<String> lines) {
        System.out.println("Detected format: JSON lines");
        List<Map<String, String>> parsed = new ArrayList<>();
        Map<String, Integer> keyFreq = new HashMap<>();

        int parsedCount = 0;
        for (String l : lines) {
            Map<String, String> kv = parseJsonTopLevel(l);
            if (!kv.isEmpty()) {
                parsed.add(kv);
                parsedCount++;
                for (String k : kv.keySet()) {
                    keyFreq.merge(k, 1, Integer::sum);
                }
            }
        }

        if (parsedCount == 0) {
            System.out.println("Could not parse JSON values reliably. Falling back to treating each line as a single message column.");
            System.out.println("Typical columns: 1");
            System.out.println("Detected message column: index 0 (entire line)");
            return;
        }

        // Determine typical key count (mode)
        Map<Integer, Integer> keyCountHistogram = new HashMap<>();
        for (Map<String, String> m : parsed) {
            keyCountHistogram.merge(m.size(), 1, Integer::sum);
        }
        int typicalKeyCount = mode(keyCountHistogram);

        // Classify keys
        Map<String, ColumnTypeScore> keyScores = new HashMap<>();
        Map<String, ColumnEvidence> keyEvidence = new HashMap<>();
        for (String key : keyFreq.keySet()) {
            keyScores.put(key, new ColumnTypeScore());
            keyEvidence.put(key, new ColumnEvidence());
        }

        // Accumulate scores from values
        for (Map<String, String> m : parsed) {
            for (Map.Entry<String, String> e : m.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();
                classifyValueIntoScores(val, keyScores.get(key), keyEvidence.get(key));
            }
        }

        // Add name-based priors
        for (String key : keyScores.keySet()) {
            ColumnTypeScore s = keyScores.get(key);
            if (inSetIgnoreCase(key, DATE_KEYS)) {
                s.date += 2.0;
            }
            if (inSetIgnoreCase(key, STATUS_KEYS)) {
                s.status += 2.0;
            }
            if (inSetIgnoreCase(key, LOCATION_KEYS)) {
                s.location += 2.0;
            }
            if (inSetIgnoreCase(key, MESSAGE_KEYS)) {
                s.message += 2.0;
            }
            if (inSetIgnoreCase(key, THREAD_KEYS)) {
                s.thread += 2.0;
            }
        }

        // Choose winners
        Map<String, String> bestByType = new LinkedHashMap<>(); // type -> key
        bestByType.put("date", bestKeyFor(keyScores, ScoreSelector.DATE));
        bestByType.put("status", bestKeyFor(keyScores, ScoreSelector.STATUS));
        bestByType.put("location", bestKeyFor(keyScores, ScoreSelector.LOCATION));
        bestByType.put("message", bestKeyFor(keyScores, ScoreSelector.MESSAGE));
        bestByType.put("thread", bestKeyFor(keyScores, ScoreSelector.THREAD));

        System.out.println("Typical keys (mode across lines): " + typicalKeyCount);
        System.out.println("Key presence counts (top 10):");
        keyFreq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        System.out.println("Detected columns:");
        printKeyDetection(keyScores, "date", bestByType.get("date"));
        printKeyDetection(keyScores, "status", bestByType.get("status"));
        printKeyDetection(keyScores, "location", bestByType.get("location"));
        printKeyDetection(keyScores, "message", bestByType.get("message"));
        printKeyDetection(keyScores, "thread", bestByType.get("thread"));

        printKeyEvidence("date", bestByType.get("date"), keyEvidence);
        printKeyEvidence("status", bestByType.get("status"), keyEvidence);
        printKeyEvidence("location", bestByType.get("location"), keyEvidence);
        printKeyEvidence("message", bestByType.get("message"), keyEvidence);
        printKeyEvidence("thread", bestByType.get("thread"), keyEvidence);

    }

    // Shallow JSON parser for top-level key/value pairs, robust to strings, numbers, booleans, null, nested objects/arrays (skipped correctly).
    private static Map<String, String> parseJsonTopLevel(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        char[] c = line.toCharArray();
        int i = 0, n = c.length;

        // Skip leading whitespace/BOM
        while (i < n && Character.isWhitespace(c[i])) {
            i++;
        }
        if (i >= n || c[i] != '{') {
            return map;
        }
        i++; // skip '{'

        while (i < n) {
            // Skip whitespace and commas
            while (i < n && (Character.isWhitespace(c[i]) || c[i] == ',')) {
                i++;
            }
            if (i < n && c[i] == '}') {
                break;
            }
            // Expect key string
            if (i >= n || c[i] != '"') {
                break;
            }
            String key = readJsonString(c, n, i);
            if (key == null) {
                break;
            }
            // readJsonString returns with closing quote index at global var; emulate by scanning:
            int[] res = readJsonStringAndEndIndex(c, n, i);
            key = res[0] == -1 ? null : new String(c, res[0], res[1] - res[0]);
            if (key == null) {
                break;
            }
            i = res[2]; // position after closing quote

            // Skip whitespace
            while (i < n && Character.isWhitespace(c[i])) {
                i++;
            }
            if (i >= n || c[i] != ':') {
                break;
            }
            i++; // skip ':'
            while (i < n && Character.isWhitespace(c[i])) {
                i++;
            }

            // Parse value
            int valStart = i;
            String value;
            if (i < n && c[i] == '"') {
                int[] vres = readJsonStringAndEndIndex(c, n, i);
                if (vres[0] == -1) {
                    break;
                }
                value = new String(c, vres[0], vres[1] - vres[0]);
                i = vres[2];
            } else if (i < n && (c[i] == '{' || c[i] == '[')) {
                int end = skipBalanced(c, n, i, c[i]);
                if (end == -1) {
                    break;
                }
                value = new String(c, i, end - i + 1);
                i = end + 1;
            } else {
                // number, boolean, null, or bare token until comma/}
                int start = i;
                while (i < n && c[i] != ',' && c[i] != '}') {
                    i++;
                }
                value = new String(c, start, i - start).trim();
            }
            map.put(unescapeJsonString(key), stripJsonQuotes(value));
            // continue loop; i is at next char/comma/}
        }

        return map;
    }

    // Utility to find end of a JSON string and return indexes: [contentStart, contentEndExclusive, posAfterString]
    private static int[] readJsonStringAndEndIndex(char[] c, int n, int i) {
        if (i >= n || c[i] != '"') {
            return new int[]{-1, -1, -1};
        }
        int start = i + 1;
        i++;
        boolean esc = false;
        while (i < n) {
            char ch = c[i];
            if (esc) {
                esc = false;
            } else if (ch == '\\') {
                esc = true;
            } else if (ch == '"') {
                int contentEnd = i;
                return new int[]{start, contentEnd, i + 1};
            }
            i++;
        }
        return new int[]{-1, -1, -1};
    }

    private static String readJsonString(char[] c, int n, int i) {
        int[] r = readJsonStringAndEndIndex(c, n, i);
        if (r[0] == -1) {
            return null;
        }
        return new String(c, r[0], r[1] - r[0]);
    }

    private static int skipBalanced(char[] c, int n, int i, char open) {
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inStr = false, esc = false;
        while (i < n) {
            char ch = c[i];
            if (inStr) {
                if (esc) {
                    esc = false;
                } else if (ch == '\\') {
                    esc = true;
                } else if (ch == '"') {
                    inStr = false;
                }
            } else {
                if (ch == '"') {
                    inStr = true;
                } else if (ch == open) {
                    depth++;
                } else if (ch == close) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            i++;
        }
        return -1;
    }

    private static String stripJsonQuotes(String v) {
        String t = v.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            return unescapeJsonString(t.substring(1, t.length() - 1));
        }
        return t;
    }

    private static String unescapeJsonString(String s) {
        // Minimal unescape for common escapes
        StringBuilder sb = new StringBuilder(s.length());
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (esc) {
                switch (ch) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (Exception e) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append(ch);
                }
                esc = false;
            } else if (ch == '\\') {
                esc = true;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean inSetIgnoreCase(String key, Set<String> set) {
        for (String k : set) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static void printKeyDetection(Map<String, ColumnTypeScore> scores, String label, String key) {
        if (key == null) {
            System.out.println("  " + label + ": not confidently identified");
            return;
        }
        ColumnTypeScore s = scores.get(key);
        System.out.printf("  %s: key \"%s\" (scores: date=%.2f, status=%.2f, location=%.2f, message=%.2f)%n",
                label, key, s.date, s.status, s.location, s.message);
    }

    // ========= Delimited text analysis =========
    private static void analyzeDelimitedLogs(List<String> lines) {
        System.out.println("Detected format: Delimited text");

        // Try each strategy; pick the one with most consistent column count (highest mode support, then lowest variance).
        StrategyResult best = null;

        for (DelimStrategy ds : new DelimStrategy[]{
            DelimStrategy.JAVA_LOGBACK,
            DelimStrategy.WHITESPACE_TAIL,
            DelimStrategy.WHITESPACE,
            DelimStrategy.TAB,
            DelimStrategy.COMMA,
            DelimStrategy.PIPE,
            DelimStrategy.SEMICOLON}) {
            StrategyResult r = evaluateStrategy(lines, ds);
            if (best == null || r.isBetterThan(best)) {
                best = r;
            }
        }

        if (best == null || best.rows.isEmpty()) {
            System.out.println("Could not tokenize lines reliably. Treating entire line as a single column.");
            System.out.println("Typical columns: 1");
            System.out.println("Detected message column: index 0 (entire line)");
            return;
        }

        System.out.println("Chosen delimiter strategy: " + best.strategy);
        System.out.println("Typical columns (mode across lines): " + best.typicalColumns);
        System.out.printf("Support for typical column count: %d of %d lines (%.1f%%)%n",
                best.supportForTypical, best.rows.size(), 100.0 * best.supportForTypical / Math.max(1, best.rows.size()));

        // Column classification
        int cols = best.typicalColumns;
        List<List<String>> colValues = new ArrayList<>();
        List<ColumnEvidence> colEvidence = new ArrayList<>(); // NEW
        for (int i = 0; i < cols; i++) {
            colValues.add(new ArrayList<>());
            colEvidence.add(new ColumnEvidence()); // NEW
        }
        for (List<String> row : best.rows) {
            if (row.size() == cols) {
                for (int i = 0; i < cols; i++) {
                    colValues.get(i).add(row.get(i));
                }
            }
        }

        // Score each column (instrumented)
        List<ColumnTypeScore> scores = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            ColumnTypeScore s = new ColumnTypeScore();
            ColumnEvidence ev = colEvidence.get(i); // NEW
            for (String v : colValues.get(i)) {
                classifyValueIntoScores(v, s, ev);   // pass evidence
            }
            if (i == cols - 1) {
                s.message += 1.0;
            }          // keep priors
            if (i >= Math.max(2, cols - 3)) {
                s.message += 0.5;
            }
            scores.add(s);
        }

        boolean anyStackTraces = !detectJavaStackTraces(lines).isEmpty();
        if (anyStackTraces) {
            for (int i = 0; i < scores.size(); i++) {
                // Small nudge: stack traces usually live in the message column
                scores.get(i).message += 0.2;
            }
        }

        if (best.strategy == DelimStrategy.JAVA_LOGBACK && cols == 5) {
            nudgeForJavaLogback(scores);
        } else if (best.strategy == DelimStrategy.WHITESPACE_TAIL) {
            nudgeForWhitespaceTail(scores, cols);
        }

        // Pick best indices per type
        Integer dateIdx = bestIndex(scores, ScoreSelector.DATE);
        Integer statusIdx = bestIndex(scores, ScoreSelector.STATUS);
        Integer locationIdx = bestIndex(scores, ScoreSelector.LOCATION);
        Integer threadIdx = bestIndex(scores, ScoreSelector.THREAD);
        // Message is special: prefer the longest average-length column if its message score is competitive
        Integer messageIdx = bestMessageIndex(colValues, scores);

        System.out.println("Detected columns (0-based indices):");
        printColDetection("date", dateIdx, scores);
        printColDetection("status", statusIdx, scores);
        printColDetection("location", locationIdx, scores);
        printColDetection("message", messageIdx, scores);
        printColDetection("thread", threadIdx, scores);

        printColEvidence("date", dateIdx, colEvidence);
        printColEvidence("status", statusIdx, colEvidence);
        printColEvidence("location", locationIdx, colEvidence);
        printColEvidence("message", messageIdx, colEvidence);
        printColEvidence("thread", threadIdx, colEvidence);

    }

    private static void printColDetection(String label, Integer idx, List<ColumnTypeScore> scores) {
        if (idx == null) {
            System.out.println("  " + label + ": not confidently identified");
            return;
        }
        ColumnTypeScore s = scores.get(idx);
        System.out.printf("  %s: index %d (scores: date=%.2f, status=%.2f, location=%.2f, message=%.2f)%n",
                label, idx, s.date, s.status, s.location, s.message);
    }

    private static void printColEvidence(String label, Integer idx, List<ColumnEvidence> colEvidence) {
        if (idx == null || idx < 0 || idx >= colEvidence.size()) {
            System.out.println("  " + label + " evidence: (none)");
            return;
        }
        ScoreSelector sel = selectorForLabel(label);
        ColumnEvidence ev = colEvidence.get(idx);
        List<Map.Entry<String, Integer>> top = topSignals(ev, sel, 8);
        if (top.isEmpty()) {
            System.out.println("  " + label + " evidence: (none)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Integer> e = top.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            Pattern p = regexPatterns.get(e.getKey());
            if (p != null) {
                sb.append(e.getKey()).append('(').append(e.getValue()).append(":\"").append(p.pattern()).append("\")");
            } else {
                sb.append(e.getKey()).append('(').append(e.getValue()).append(":\"unkown").append("\")");
            }
        }
        System.out.println("  " + label + " evidence: " + sb);
    }

    private static Integer bestIndex(List<ColumnTypeScore> scores, ScoreSelector selector) {
        double best = Double.NEGATIVE_INFINITY;
        Integer idx = null;
        for (int i = 0; i < scores.size(); i++) {
            double v = selector.value(scores.get(i));
            if (v > best) {
                best = v;
                idx = i;
            }
        }
        // Small sanity threshold: require > 0.5
        return (best > 0.5) ? idx : null;
    }

    private static Integer bestMessageIndex(List<List<String>> colValues, List<ColumnTypeScore> scores) {
        double bestLen = Double.NEGATIVE_INFINITY;
        int bestLenIdx = -1;
        for (int i = 0; i < colValues.size(); i++) {
            double avgLen = averageLength(colValues.get(i));
            if (avgLen > bestLen) {
                bestLen = avgLen;
                bestLenIdx = i;
            }
        }
        // If the longest column also has a reasonable message score, prefer it.
        if (bestLenIdx >= 0 && scores.get(bestLenIdx).message >= 0.5) {
            return bestLenIdx;
        }
        // fallback to message score winner
        return bestIndex(scores, ScoreSelector.MESSAGE);
    }

    private static double averageLength(List<String> vals) {
        if (vals.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (String s : vals) {
            sum += s.length();
        }
        return sum / (double) vals.size();
    }

    // ========= Strategy evaluation =========
    private static StrategyResult evaluateStrategy(List<String> lines, DelimStrategy ds) {
        List<List<String>> rows = new ArrayList<>();
        Map<Integer, Integer> histogram = new HashMap<>();

        for (String l : lines) {
            List<String> cols;
            if (ds == DelimStrategy.JAVA_LOGBACK) {
                cols = tokenizeLogback(l);
            } else if (ds == DelimStrategy.WHITESPACE_TAIL) {
                // Estimate how many structured tokens precede the message
                int leading = estimateLeadingStructuralTokens(l);
                cols = tokenizeWhitespaceTail(l, leading);

            } else {
                cols = tokenize(l, ds);
            }
            if (cols.isEmpty()) {
                continue;
            }
            rows.add(cols);
            histogram.merge(cols.size(), 1, Integer::sum);
        }

        int typical = mode(histogram);
        int support = histogram.getOrDefault(typical, 0);
        double variance = columnCountVariance(rows, typical);

        StrategyResult r = new StrategyResult();
        r.strategy = ds;
        r.rows = rows;
        r.typicalColumns = typical;
        r.supportForTypical = support;
        r.variance = variance;
        r.totalLines = lines.size();
        r.coverageRatio = r.totalLines == 0 ? 0.0 : (rows.size() / (double) r.totalLines);
        r.supportRatio = rows.isEmpty() ? 0.0 : (support / (double) rows.size());
        assessFormatQuality(r);
        return r;
    }

    private static double columnCountVariance(List<List<String>> rows, int typical) {
        if (rows.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double sumSq = 0;
        for (List<String> r : rows) {
            double d = r.size() - typical;
            sumSq += d * d;
        }
        return sumSq / rows.size();
    }

    private static int mode(Map<Integer, Integer> histogram) {
        int bestKey = -1;
        int bestCount = -1;
        for (Map.Entry<Integer, Integer> e : histogram.entrySet()) {
            if (e.getValue() > bestCount || (e.getValue() == bestCount && e.getKey() > bestKey)) {
                bestKey = e.getKey();
                bestCount = e.getValue();
            }
        }
        return bestKey == -1 ? 1 : bestKey;
    }

    // ========= Tokenization =========
    private static List<String> tokenize(String line, DelimStrategy ds) {
        List<String> out = new ArrayList<>();
        char[] a = line.toCharArray();
        int n = a.length;
        StringBuilder cur = new StringBuilder();
        boolean inPar = false;  // inside parentheses
        int parDepth = 0;
        boolean inDq = false, inSq = false, inBr = false; // double quotes, single quotes, square brackets
        for (int i = 0; i < n; i++) {
            char ch = a[i];
            // Track grouping
            if (ch == '"' && !inSq) {
                inDq = !inDq;
                cur.append(ch);
                continue;
            }
            if (ch == '\'' && !inDq) {
                inSq = !inSq;
                cur.append(ch);
                continue;
            }
            if (ch == '[' && !inDq && !inSq) {
                inBr = true;
                cur.append(ch);
                continue;
            }
            if (ch == ']' && inBr && !inDq && !inSq) {
                inBr = false;
                cur.append(ch);
                continue;
            }
            if (!inDq && !inSq) {
                if (ch == '(') {
                    inPar = true;
                    parDepth++;
                    cur.append(ch);
                    continue;
                }
                if (ch == ')' && inPar) {
                    parDepth--;
                    cur.append(ch);
                    if (parDepth == 0) {
                        inPar = false;
                    }
                    continue;
                }
            }

            boolean isDelim = false;
            switch (ds) {
                case WHITESPACE:
                    isDelim = !inDq && !inSq && !inBr && !inPar && Character.isWhitespace(ch);
                    break;
                case TAB:
                    isDelim = !inDq && !inSq && !inBr && ch == '\t';
                    break;
                case COMMA:
                    isDelim = !inDq && !inSq && !inBr && ch == ',';
                    break;
                case PIPE:
                    isDelim = !inDq && !inSq && !inBr && ch == '|';
                    break;
                case SEMICOLON:
                    isDelim = !inDq && !inSq && !inBr && ch == ';';
                    break;
            }

            if (isDelim) {
                if (ds == DelimStrategy.WHITESPACE) {
                    // consume contiguous whitespace
                    if (cur.length() > 0) {
                        out.add(cur.toString().trim());
                        cur.setLength(0);
                    }
                    // skip additional whitespace
                    while (i + 1 < n && Character.isWhitespace(a[i + 1])) {
                        i++;
                    }
                } else {
                    out.add(cur.toString().trim());
                    cur.setLength(0);
                }
            } else {
                cur.append(ch);
            }
        }

        if (cur.length() > 0) {
            out.add(cur.toString().trim());
        }
        // Remove leading/trailing quotes/brackets if they wrap the whole token
        for (int i = 0; i < out.size(); i++) {
            out.set(i, stripWrapping(out.get(i)));
        }
        // Drop empty tokens that might arise from consecutive delimiters (except whitespace mode which handles it)
        if (ds != DelimStrategy.WHITESPACE) {
            out.removeIf(String::isEmpty);
        }
        return out;
    }

    // Tokenize using the regex (returns 5 fixed columns)
    private static List<String> tokenizeLogback(String line) {
        Matcher m = JAVA_LOGBACK_LINE.matcher(line);
        if (!m.find()) {
            return Collections.emptyList();
        }
        return Arrays.asList(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5).trim());
    }

    private static String stripWrapping(String s) {
        if (s.length() >= 2) {
            if ((s.startsWith("\"") && s.endsWith("\""))
                    || (s.startsWith("'") && s.endsWith("'"))
                    || (s.startsWith("[") && s.endsWith("]"))) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    // ========= Classification scoring =========
    private static class ColumnTypeScore {

        double date = 0;
        double status = 0;
        double location = 0;
        double message = 0;
        double thread = 0;   // NEW: thread/context score
    }

    private enum ScoreSelector {
        DATE {
            double value(ColumnTypeScore s) {
                return s.date;
            }
        },
        STATUS {
            double value(ColumnTypeScore s) {
                return s.status;
            }
        },
        LOCATION {
            double value(ColumnTypeScore s) {
                return s.location;
            }
        },
        MESSAGE {
            double value(ColumnTypeScore s) {
                return s.message;
            }
        },
        THREAD {
            double value(ColumnTypeScore s) {
                return s.thread;
            }
        };

        abstract double value(ColumnTypeScore s);
    }

    private static void classifyValueIntoScores(String v, ColumnTypeScore s) {
        classifyValueIntoScores(v, s, null);
    }

    private static void classifyValueIntoScores(String v, ColumnTypeScore s, ColumnEvidence ev) {
        if (v == null) {
            return;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return;
        }

        // ---- Dates
        if (ISO_TS.matcher(t).find()) {
            s.date += 2.0;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "ISO_TS");
            }
        }
        if (APACHE_CLF.matcher(t).find()) {
            s.date += 2.0;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "APACHE_CLF");
            }
        }
        if (SYSLOG_TS.matcher(t).find()) {
            s.date += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "SYSLOG_TS");
            }
        }
        if (ISO_DATE_ONLY.matcher(t).find()) {
            s.date += 0.5;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "ISO_DATE_ONLY");
            }
        }
        if (DMY_SLASH.matcher(t).find()) {
            s.date += 0.5;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "DMY_SLASH");
            }
        }
        if (LOG4J_TS.matcher(t).find()) {
            s.date += 2.0;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, "LOG4J_TS");
            }
        }
        EpochKind ek = epochKind(t);
        if (ek != EpochKind.NONE) {
            s.date += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.DATE, ek == EpochKind.MS_13 ? "EPOCH_13MS" : "EPOCH_10S");
            }
        }

        // ---- Status
        if (LOG_LEVEL.matcher(t).find()) {
            s.status += 2.0;
            if (ev != null) {
                ev.hit(ScoreSelector.STATUS, "LOG_LEVEL");
            }
        }
        if (HTTP_STATUS.matcher(t).find()) {
            s.status += 1.5;
            if (ev != null) {
                ev.hit(ScoreSelector.STATUS, "HTTP_STATUS");
            }
        }
        if (GENERIC_STATUS.matcher(t).find()) {
            s.status += 0.5;
            if (ev != null) {
                ev.hit(ScoreSelector.STATUS, "GENERIC_STATUS");
            }
        }

        // ---- Location
        if (IPV4.matcher(t).find()) {
            s.location += 1.5;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "IPV4");
            }
        }
        if (IPV6.matcher(t).find()) {
            s.location += 1.5;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "IPV6");
            }
        }
        if (URL.matcher(t).find()) {
            s.location += 1.5;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "URL");
            }
        }
        if (UNIX_PATH.matcher(t).find()) {
            s.location += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "UNIX_PATH");
            }
        }
        if (WINDOWS_PATH.matcher(t).find()) {
            s.location += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "WINDOWS_PATH");
            }
        }
        if (JAVA_FQN.matcher(t).find()) {
            s.location += 0.7;
            if (ev != null) {
                ev.hit(ScoreSelector.LOCATION, "JAVA_FQN");
            }
        }

        // ---- Thread/context
        if (THREAD_TOKEN.matcher(t).find()) {
            s.thread += 1.8;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "THREAD_TOKEN");
            }
        }
        if (CAMEL_THREAD.matcher(t).find()) {
            s.thread += 1.2;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "CAMEL_THREAD");
            }
        }
        if (JAVA_THREAD_NAME.matcher(t).find()) {
            s.thread += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "JAVA_THREAD_NAME");
            }
        }
        if (EXECUTOR_THREAD.matcher(t).find()) {
            s.thread += 0.8;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "EXECUTOR_THREAD");
            }
        }
        if (PAREN_CONTEXT.matcher(t).find()) {
            s.thread += 0.6;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "PAREN_CONTEXT");
            }
        }
        if (GENERIC_URI.matcher(t).find()) {
            s.thread += 0.4;
            if (ev != null) {
                ev.hit(ScoreSelector.THREAD, "GENERIC_URI");
            }
        }

        // ---- Message heuristics
        int spaces = countSpaces(t);
        if (spaces >= 3) {
            s.message += 1.0;
            if (ev != null) {
                ev.hit(ScoreSelector.MESSAGE, "SPACES>=3");
            }
        }
        if (t.length() > 40) {
            s.message += 0.7;
            if (ev != null) {
                ev.hit(ScoreSelector.MESSAGE, "LEN>40");
            }
        }
        if (looksLikeStackTraceLine(t)) {
            s.message += 0.5;
            if (ev != null) {
                ev.hit(ScoreSelector.MESSAGE, "STACKTRACE_LINE");
            }
        }
    }

    private static EpochKind epochKind(String t) {
        try {
            Matcher m13 = EPOCH_13MS.matcher(t);
            if (m13.find()) {
                long ms = Long.parseLong(m13.group());
                if (ms >= 946684800000L && ms <= 4102444800000L) {
                    return EpochKind.MS_13;
                }
            }
            Matcher m10 = EPOCH_10S.matcher(t);
            if (m10.find()) {
                long sec;
                try {
                    sec = (long) Double.parseDouble(m10.group());
                } catch (NumberFormatException ex) {
                    sec = Long.MIN_VALUE;
                }
                long ms = sec * 1000L;
                if (ms >= 946684800000L && ms <= 4102444800000L) {
                    return EpochKind.SEC_10;
                }
            }
        } catch (Exception ignored) {
        }
        return EpochKind.NONE;
    }

    private static boolean isPlausibleEpoch(String t) {
        // 10-digit seconds or 13-digit millis in a sensible range (2000-01-01 .. 2100-01-01)
        try {
            Matcher m13 = EPOCH_13MS.matcher(t);
            if (m13.find()) {
                long ms = Long.parseLong(m13.group());
                return ms >= 946684800000L && ms <= 4102444800000L;
            }
            Matcher m10 = EPOCH_10S.matcher(t);
            if (m10.find()) {
                long sec = 0L;
                try {
                    sec = (long) Double.parseDouble(m10.group());
                } catch (NumberFormatException ignored) {
                }
                long ms = sec * 1000L;
                return ms >= 946684800000L && ms <= 4102444800000L;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static int countSpaces(String t) {
        int c = 0;
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                c++;
            }
        }
        return c;
    }

    private static boolean looksLikeStackTraceLine(String t) {
        return t.startsWith("at ") || t.contains("Exception:") || t.contains("Caused by:");
    }

    // ========= Strategy result =========
    private static class StrategyResult {

        DelimStrategy strategy;
        List<List<String>> rows;
        int typicalColumns;
        int supportForTypical;
        double variance;

        // totals & ratios
        int totalLines;           // total lines considered (non-empty sample)
        double coverageRatio;     // rows.size() / totalLines
        double supportRatio;      // supportForTypical / rows.size()

        double roleCoverage;    // 0..1 (#roles confidently detected / 5)
        double tailness;        // 0..1 avg( len(lastCol) / sum(len(allCols)) ) across typical rows

        // overall score for comparator
        double score() {
            // Information gain: reward multi-column over single-column
            double infoGain = (typicalColumns <= 1) ? 0.0 : Math.log1p(typicalColumns - 1); // ln(1 + (cols-1))

            // Stability: normalize variance against columns (smaller is better)
            double stability = 1.0 - Math.min(1.0, variance / Math.max(1, typicalColumns * typicalColumns));

            // Aggregate score (weights sum to ~1)
            double s = (0.28 * coverageRatio)
                    + (0.23 * supportRatio)
                    + (0.22 * roleCoverage)
                    + (0.14 * tailness)
                    + (0.07 * stability)
                    + (0.05 * infoGain);

            // Strong penalty for trivial single-column unless nothing else is good
            if (typicalColumns == 1) {
                s *= 0.50; // halve the score
            }

            if (typicalColumns >= 8 && tailness < 0.35) {
                s *= 0.75;
            }

            if (strategy == DelimStrategy.JAVA_LOGBACK
                    && typicalColumns == 5
                    && coverageRatio >= 0.50
                    && supportRatio >= 0.90) {
                s += 0.05;
            }

            return s;
        }

        // Reworked comparator: prefer higher score, then more columns, then lower variance
        boolean isBetterThan(StrategyResult other) {
            double s1 = this.score();
            double s2 = other.score();
            if (Double.compare(s1, s2) != 0) {
                return s1 > s2;
            }

            if (this.typicalColumns != other.typicalColumns) {
                return this.typicalColumns > other.typicalColumns;
            }
            return this.variance < other.variance;
        }
    }

    // ========= Small utilities =========
    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        LinkedHashSet<T> s = new LinkedHashSet<>();
        Collections.addAll(s, items);
        return s;
    }

    // Choose the key with the highest score for a given type (date/status/location/message).
    // Returns null if no key has a meaningful score.
    private static String bestKeyFor(Map<String, ColumnTypeScore> keyScores, ScoreSelector selector) {
        double best = Double.NEGATIVE_INFINITY;
        String bestKey = null;

        for (Map.Entry<String, ColumnTypeScore> e : keyScores.entrySet()) {
            double v = selector.value(e.getValue());
            if (v > best) {
                best = v;
                bestKey = e.getKey();
            }
        }

        // Require a minimal confidence to avoid spurious picks
        return (best > 0.5) ? bestKey : null;
    }

    // Represents one stack trace span in the log.
    private static class StackTraceSpan {

        String rootException;        // e.g., java.lang.NullPointerException
        int startLineIdx;            // index within sampled lines
        int endLineIdx;              // inclusive
        int frameCount;              // number of "at ..." frames under the root
        List<String> causes = new ArrayList<>();     // chained "Caused by" exception types
        List<String> suppressed = new ArrayList<>(); // "Suppressed" exception types
    }

    // Is this line likely to be an exception header?
    private static String matchExceptionHeader(String line) {
        Matcher m = EXCEPTION_HEADER.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        // Also accept "Caused by" lines as headers when encountered standalone
        Matcher c = CAUSED_BY.matcher(line);
        if (c.find()) {
            return c.group(1);
        }
        return null;
    }

    // Does this line look like a stack frame?
    private static boolean isStackFrameLine(String line) {
        return STACK_FRAME.matcher(line).find();
    }

    // Parse the sampled lines and group Java stack traces.
    // Returns a list of spans in chronological order.
    private static List<StackTraceSpan> detectJavaStackTraces(List<String> lines) {
        List<StackTraceSpan> spans = new ArrayList<>();
        StackTraceSpan cur = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Try matching a new exception header
            String exc = matchExceptionHeader(line);
            if (exc != null) {
                // If we were inside a previous trace, close it
                if (cur != null) {
                    cur.endLineIdx = i - 1;
                    spans.add(cur);
                }
                cur = new StackTraceSpan();
                cur.rootException = exc;
                cur.startLineIdx = i;
                cur.frameCount = 0;
                // Continue; we may have frames next
                continue;
            }

            if (cur != null) {
                // Handle frames and chained lines while in a trace
                if (isStackFrameLine(line)) {
                    cur.frameCount++;
                    continue;
                }
                Matcher caused = CAUSED_BY.matcher(line);
                if (caused.find()) {
                    cur.causes.add(caused.group(1));
                    continue;
                }
                Matcher sup = SUPPRESSED.matcher(line);
                if (sup.find()) {
                    cur.suppressed.add(sup.group(1));
                    continue;
                }

                // If the line is neither frame nor part of the exception chain,
                // we consider the current trace closed.
                cur.endLineIdx = i - 1;
                spans.add(cur);
                cur = null;
            }
        }

        // Close trailing trace if file ends while inside one
        if (cur != null) {
            cur.endLineIdx = lines.size() - 1;
            spans.add(cur);
        }

        return spans;
    }

    private static void printStackTraceSummary(List<String> lines, List<StackTraceSpan> spans) {
        if (spans.isEmpty()) {
            System.out.println("\nStack traces & exceptions: none detected");
            return;
        }

        System.out.println("\nStack traces & exceptions:");
        System.out.println("Total traces detected: " + spans.size());

        // Count by type (root exception)
        Map<String, Integer> byType = new HashMap<>();
        for (StackTraceSpan s : spans) {
            byType.merge(s.rootException, 1, Integer::sum);
        }

        System.out.println("Top exception types:");
        byType.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        // Show a few example spans with metadata
        System.out.println("\nExample occurrences:");
        int samples = Math.min(3, spans.size());
        for (int i = 0; i < samples; i++) {
            StackTraceSpan s = spans.get(i);
            int len = Math.max(1, s.endLineIdx - s.startLineIdx + 1);
            System.out.printf("  #%d: %s  lines[%d..%d], frames=%d, causes=%d, suppressed=%d%n",
                    i + 1, s.rootException, s.startLineIdx, s.endLineIdx, s.frameCount, s.causes.size(), s.suppressed.size());
            // Print the first header line for context
            String header = lines.get(s.startLineIdx);
            System.out.println("      " + truncate(header, 160));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    // Try to infer how many leading tokens are "structural" before the free-text message.
    // For Logback-like lines this is usually 4: date, level, [logger], (thread)
    // Returns a number in [2..6]. If nothing obvious is found, defaults to 3 (date + level + message).
    private static int estimateLeadingStructuralTokens(String line) {
        // Quick checks on the *trimmed* line
        String t = line.trim();
        if (t.isEmpty()) {
            return 2;
        }

        int count = 0;

        // 1) Timestamp at start? (support ISO or log4j/comma-millis)
        if (t.matches("^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?).*")) {
            count++;
            // Remove the first token (timestamp) for following checks
            // (We just approximate; we don't mutate the string here—count is what matters)
        }

        // 2) Level next?
        if (t.matches("(?i)^(?:\\S+\\s+){1,3}(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\b.*")) {
            count++;
        }
               
        // 3) Bracketed logger next?  [com.foo.Bar]
        if (t.contains("[") && t.contains("]")) {
            // simple signal; tokenizer already keeps [ ... ] intact
            count++;
        }

        // 4) Parenthesized thread next?  (Camel (camel-1) thread #5 - timer://...)
        if (t.contains("(") && t.contains(")")) {
            // simple signal; tokenizer groups parentheses if you applied that patch
            count++;
        }

        // Bound it reasonably so we don't over-split or under-split
        if (count < 2) {
            count = 2;        // at least "date + level"
        }
        if (count > 6) {
            count = 6;
        }

        return count;
    }

    // Split the line by whitespace, but keep the remainder as one tail/token (message).
    // 'leading' is how many leading tokens to extract before we treat the rest as message.
    private static List<String> tokenizeWhitespaceTail(String line, int leading) {
        List<String> out = new ArrayList<>(leading + 1);
        int n = line.length();
        int i = 0;

        // Skip leading spaces
        while (i < n && Character.isWhitespace(line.charAt(i))) {
            i++;
        }

        for (int k = 0; k < leading; k++) {
            if (i >= n) {
                out.add(""); // missing token; keep placeholder
                continue;
            }
            int start = i;
            // advance until next whitespace
            while (i < n && !Character.isWhitespace(line.charAt(i))) {
                i++;
            }
            out.add(line.substring(start, i));
            // skip spaces
            while (i < n && Character.isWhitespace(line.charAt(i))) {
                i++;
            }
        }

        // Tail = everything else (may contain spaces)
        if (i < n) {
            out.add(line.substring(i).trim());
        } else {
            out.add(""); // empty message
        }
        return out;
    }

    /**
     * Apply index-based priors when the best strategy is JAVA_LOGBACK. Expected
     * column roles for 5 columns: 0=date, 1=level, 2=logger (location),
     * 3=thread/context, 4=message
     */
    private static void nudgeForJavaLogback(List<ColumnTypeScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return;
        }
        for (int i = 0; i < scores.size(); i++) {
            switch (i) {
                case 0:
                    scores.get(i).date += 1.5;
                    break;
                case 1:
                    scores.get(i).status += 1.5;
                    break;
                case 2:
                    scores.get(i).location += 1.2;
                    break;
                case 3:
                    scores.get(i).thread += 1.8;
                    break; // thread emphasis
                case 4:
                    scores.get(i).message += 1.2;
                    break;
                default:
                    /* ignore extra columns */ break;
            }
        }
    }

    /**
     * Apply index-based priors when the best strategy is WHITESPACE_TAIL.
     * Heuristic assumption: the first ~4 tokens are structural (date, level,
     * [logger], (thread)), and the tail is message. If fewer columns, bias what
     * exists sensibly.
     *
     * @param scores per-column score objects (same order as columns)
     * @param typicalColumns best.typicalColumns from strategy selection
     */
    private static void nudgeForWhitespaceTail(List<ColumnTypeScore> scores, int typicalColumns) {
        if (scores == null || scores.isEmpty()) {
            return;
        }

        // If we have at least 5 columns, align with: 0=date, 1=level, 2=location, 3=thread, 4+=message
        if (typicalColumns >= 5) {
            for (int i = 0; i < scores.size(); i++) {
                if (i == 0) {
                    scores.get(i).date += 1.0;
                } else if (i == 1) {
                    scores.get(i).status += 0.8;
                } else if (i == 2) {
                    scores.get(i).location += 0.8;   // often [logger]
                } else if (i == 3) {
                    scores.get(i).thread += 0.8;   // often (thread)
                } else {
                    scores.get(i).message += 0.6;   // tail and beyond
                }
            }
            return;
        }

        // If we have 4 columns, likely: 0=date, 1=level, 2=location or thread, 3=message
        if (typicalColumns == 4) {
            ColumnTypeScore c2 = scores.get(2);
            scores.get(0).date += 1.0;
            if (scores.size() > 1) {
                scores.get(1).status += 0.8;
            }
            if (c2.thread < 0.1 && c2.location >= c2.thread) {
                c2.location += 0.6;
            } else {
                c2.thread += 0.6;
            }
            if (scores.size() > 3) {
                scores.get(3).message += 0.8;
            }
            return;
        }

        // If we only have 3 columns, bias to: 0=date, 1=status, 2=message
        if (typicalColumns == 3) {
            scores.get(0).date += 1.0;
            if (scores.size() > 1) {
                scores.get(1).status += 0.8;
            }
            if (scores.size() > 2) {
                scores.get(2).message += 0.8;
            }
            return;
        }

        // Fallback: if 2 columns, bias to: 0=date, 1=message
        if (typicalColumns == 2) {
            scores.get(0).date += 0.8;
            if (scores.size() > 1) {
                scores.get(1).message += 0.8;
            }
        }
    }

// Compute role coverage (date, status, location, thread, message) and tailness for a strategy.
    private static void assessFormatQuality(StrategyResult r) {
        if (r.rows == null || r.rows.isEmpty() || r.typicalColumns <= 0) {
            r.roleCoverage = 0.0;
            r.tailness = 0.0;
            return;
        }

        // Collect values for columns only from rows that match the typical column count
        int cols = r.typicalColumns;
        List<List<String>> colValues = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            colValues.add(new ArrayList<>());
        }

        long tailLenSum = 0, totalLenSum = 0;
        int matched = 0;

        for (List<String> row : r.rows) {
            if (row.size() != cols) {
                continue;
            }
            matched++;
            for (int i = 0; i < cols; i++) {
                String v = row.get(i);
                colValues.get(i).add(v);
                totalLenSum += (v != null ? v.length() : 0);
            }
            String last = row.get(cols - 1);
            tailLenSum += (last != null ? last.length() : 0);
        }

        // Per-column scoring using the same heuristics as analyzeDelimitedLogs(...)
        List<ColumnTypeScore> scores = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            ColumnTypeScore s = new ColumnTypeScore();
            for (String v : colValues.get(i)) {
                classifyValueIntoScores(v, s);
            }
            // Generic priors (last col tends to be message)
            if (i == cols - 1) {
                s.message += 1.0;
            }
            if (i >= Math.max(2, cols - 3)) {
                s.message += 0.5;
            }
            scores.add(s);
        }

        // If the strategy itself encodes structure, add nudges here too so roleCoverage reflects them:
        if (r.strategy == DelimStrategy.JAVA_LOGBACK && cols == 5) {
            nudgeForJavaLogback(scores);
        } else if (r.strategy == DelimStrategy.WHITESPACE_TAIL) {
            nudgeForWhitespaceTail(scores, cols);
        }

        // Pick best indices and count confident roles
        int roles = 0;
        Integer d = bestIndex(scores, ScoreSelector.DATE);
        roles += (d != null) ? 1 : 0;
        Integer l = bestIndex(scores, ScoreSelector.STATUS);
        roles += (l != null) ? 1 : 0;
        Integer c = bestIndex(scores, ScoreSelector.LOCATION);
        roles += (c != null) ? 1 : 0;
        Integer t = bestIndex(scores, ScoreSelector.THREAD);
        roles += (t != null) ? 1 : 0;
        Integer m = bestIndex(scores, ScoreSelector.MESSAGE);
        roles += (m != null) ? 1 : 0;

        r.roleCoverage = roles / 5.0;

        // Tailness: how much of the content lives in the last column
        r.tailness = (totalLenSum == 0) ? 0.0 : (tailLenSum / (double) totalLenSum);
    }

    private enum EpochKind {
        NONE, SEC_10, MS_13
    }

    // --- Evidence tracking -------------------------------------------------------
    private static class ColumnEvidence {

        private final EnumMap<ScoreSelector, Map<String, Integer>> hits
                = new EnumMap<>(ScoreSelector.class);

        void hit(ScoreSelector type, String signal) {
            hits.computeIfAbsent(type, k -> new LinkedHashMap<>())
                    .merge(signal, 1, Integer::sum);
        }

        Map<String, Integer> get(ScoreSelector type) {
            return hits.getOrDefault(type, Collections.emptyMap());
        }
    }

    private static List<Map.Entry<String, Integer>> topSignals(ColumnEvidence ev, ScoreSelector type, int k) {
        Map<String, Integer> m = ev.get(type);
        List<Map.Entry<String, Integer>> list = new ArrayList<>(m.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (list.size() > k) {
            return list.subList(0, k);
        }
        return list;
    }

    private static ScoreSelector selectorForLabel(String label) {
        switch (label) {
            case "date":
                return ScoreSelector.DATE;
            case "status":
                return ScoreSelector.STATUS;
            case "location":
                return ScoreSelector.LOCATION;
            case "message":
                return ScoreSelector.MESSAGE;
            case "thread":
                return ScoreSelector.THREAD;
            default:
                return ScoreSelector.MESSAGE;
        }
    }

    private static void printKeyEvidence(String label, String key, Map<String, ColumnEvidence> keyEvidence) {
        if (key == null) {
            System.out.println("  " + label + " evidence: (none)");
            return;
        }
        ScoreSelector sel = selectorForLabel(label);
        ColumnEvidence ev = keyEvidence.get(key);
        if (ev == null) {
            System.out.println("  " + label + " evidence: (none)");
            return;
        }
        List<Map.Entry<String, Integer>> top = topSignals(ev, sel, 8);
        if (top.isEmpty()) {
            System.out.println("  " + label + " evidence: (none)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Integer> e = top.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('(').append(e.getValue()).append(')');
        }
        System.out.println("  " + label + " evidence: " + sb);
    }

}
