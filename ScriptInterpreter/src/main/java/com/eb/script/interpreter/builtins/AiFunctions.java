package com.eb.script.interpreter.builtins;

import com.eb.ui.ebs.AiChatModelSetupDialog;
import javafx.application.Platform;
import javafx.stage.Window;
import com.eb.script.json.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * General AI helpers that use an OpenAI-compatible REST API.
 *
 * Configuration (system property OR env var): - ai.chat.url / AI_CHAT_URL
 * (default: https://api.openai.com/v1/chat/completions) - ai.embed.url /
 * AI_EMBED_URL (default: https://api.openai.com/v1/embeddings) - ai.api.key /
 * AI_API_KEY (default: none -> no Authorization header) - ai.chat.model /
 * AI_CHAT_MODEL (default: gpt-4o-mini) - ai.embed.model / AI_EMBED_MODEL
 * (default: text-embedding-3-small) - ai.timeout.ms / AI_TIMEOUT_MS (default:
 * 30000)
 *
 * The body builder uses your Json class; responses are parsed via Json.parse.
 */
public final class AiFunctions {

    private AiFunctions() {
    }

    private static final String PREF_NODE = "com.eb.ai";

    // ---- Configuration helpers ----
    // First check Preferences (registry), then system property, then env var
    private static String cfg(String prop, String env, String def) {
        // First try to get from Preferences (registry)
        try {
            Preferences p = Preferences.userRoot().node(PREF_NODE);
            String prefVal = p.get(prop, null);
            if (prefVal != null && !prefVal.isBlank()) {
                return prefVal;
            }
        } catch (Exception ignored) {
            // Fall through to system property
        }
        
        // Then try system property
        String v = System.getProperty(prop);
        if (v != null && !v.isBlank()) {
            return v;
        }
        
        // Then try environment variable
        v = System.getenv(env);
        if (v != null && !v.isBlank()) {
            return v;
        }
        
        return def;
    }

    private static int cfgInt(String prop, String env, int def) {
        String v = cfg(prop, env, null);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    // These are read once at class load for backwards compatibility with env vars
    // The API key is read fresh on each call to support runtime configuration
    private static final String CHAT_URL = cfg("ai.chat.url", "AI_CHAT_URL", "https://api.openai.com/v1/chat/completions");
    private static final String EMBED_URL = cfg("ai.embed.url", "AI_EMBED_URL", "https://api.openai.com/v1/embeddings");
    private static final String CHAT_MODEL = cfg("ai.chat.model", "AI_CHAT_MODEL", "gpt-4o-mini");
    private static final String EMB_MODEL = cfg("ai.embed.model", "AI_EMBED_MODEL", "text-embedding-3-small");
    private static final int TIMEOUT_MS = cfgInt("ai.timeout.ms", "AI_TIMEOUT_MS", 30_000);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(5_000, TIMEOUT_MS / 2)))
            .build();

    // Read API key fresh on each request to pick up runtime configuration changes
    private static String getApiKey() {
        return cfg("ai.api.key", "AI_API_KEY", null);
    }

    private static HttpRequest.Builder base(URI uri) {
        String apiKey = getApiKey();
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(TIMEOUT_MS))
                .header("Content-Type", "application/json; charset=utf-8");
        if (apiKey != null && !apiKey.isBlank()) {
            b.header("Authorization", "Bearer " + apiKey);
        }
        return b;
    }

    private static String postJson(String url, Object body) throws IOException, InterruptedException {
        String payload = Json.compactJson(body);
        HttpRequest req = base(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res;
        try {
            res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new IOException("AI request timed out after " + TIMEOUT_MS + " ms", e);
        }
        if (res.statusCode() / 100 != 2) {
            throw new IOException("AI HTTP " + res.statusCode() + ": " + safeTrunc(res.body(), 600));
        }
        return res.body();
    }

    private static String safeTrunc(String s, int n) {
        if (s == null) {
            return null;
        }
        return s.length() <= n ? s : (s.substring(0, n) + " …");
    }

    // --------------------------------------------------------------------------------
    // Public API
    // --------------------------------------------------------------------------------
    /**
     * Chat completion (simple). Returns the assistant text.
     */
    public static String chatComplete(String system, String user, Integer maxTokens, Double temperature)
            throws IOException, InterruptedException {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", CHAT_MODEL);
        List<Map<String, String>> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", Objects.toString(user, "")));
        req.put("messages", messages);
        // Note: max_tokens is intentionally not sent as some models don't support it
        // and models will automatically limit response size appropriately
        if (temperature != null) {
            req.put("temperature", temperature);
        }

        String json = postJson(CHAT_URL, req);
        Object parsed = Json.parse(json);
        // Expect shape: { choices: [ { message: { content: "..." } } ] }
        Object content = Json.getValue(parsed, "choices[0].message.content");
        if (content == null) {
            // Some providers return 'choices[0].text'
            content = Json.getValue(parsed, "choices[0].text");
        }
        return Objects.toString(content, "");
    }

    /**
     * Simple summarizer using chatComplete (system prompt + short max tokens).
     */
    public static String summarize(String text, Integer maxTokens) throws IOException, InterruptedException {
        String sys = "You are a helpful assistant that writes concise, faithful summaries.";
        int mt = (maxTokens == null || maxTokens <= 0) ? 120 : maxTokens;
        String user = "Summarize the following text:\n\n" + Objects.toString(text, "");
        return chatComplete(sys, user, mt, 0.2);
    }

    /**
     * Embedding generator. Returns a raw double[] vector.
     */
    public static double[] embed(String input) throws IOException, InterruptedException {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", EMB_MODEL);
        req.put("input", Objects.toString(input, ""));

        String json = postJson(EMBED_URL, req);
        Object parsed = Json.parse(json);
        // Expect { data: [ { embedding: [ ... ] } ] }
        Object arr = Json.getValue(parsed, "data[0].embedding");
        if (!(arr instanceof List<?> list)) {
            return new double[0];
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object v = list.get(i);
            if (v instanceof Number n) {
                out[i] = n.doubleValue();
            }
        }
        return out;
    }

    /**
     * Lightweight classifier using a zero/few-shot prompt. Returns the
     * predicted label as plain text.
     */
    public static String classify(String text, List<String> labels) throws IOException, InterruptedException {
        String sys = "You are a classifier. Reply with EXACTLY one label from the given set.";
        String user = "Labels: " + String.join(", ", labels)
                + "\nChoose the single best label for the following text:\n\n"
                + Objects.toString(text, "");
        return chatComplete(sys, user, 20, 0.0);
    }

    /**
     * Opens the "AI Chat Model Setup" dialog (blocking) and returns when
     * closed. Safe to call from any thread; it marshals to the JavaFX
     * Application Thread.
     */
    public static void showModelSetupDialog() {
        showModelSetupDialog(null);
    }

    /**
     * Opens the "AI Chat Model Setup" dialog with an optional owner window. If
     * JavaFX is not initialized, this will require that your app has started
     * the JavaFX runtime (e.g., Application.launch or Platform.startup).
     */
    public static void showModelSetupDialog(Window owner) {
        Runnable uiTask = () -> {
            AiChatModelSetupDialog dlg = new AiChatModelSetupDialog();
            if (owner != null) {
                dlg.initOwner(owner);
            }
            // showAndWait blocks until user closes dialog
            dlg.showAndWait();
        };

        // If already on the FX thread, run now; else schedule on FX thread
        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            // If your app might call this very early (before JavaFX is started),
            // ensure Platform.startup(...) has been invoked somewhere in your app bootstrap.
            Platform.runLater(uiTask);
        }
    }
}
