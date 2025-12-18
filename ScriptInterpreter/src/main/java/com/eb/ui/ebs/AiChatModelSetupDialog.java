package com.eb.ui.ebs;

import com.eb.ui.util.ButtonShortcutHelper;
import java.util.prefs.Preferences;
import com.eb.script.json.Json;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * "AI Chat Model Setup" popup that captures the AiFunctions settings and stores
 * them into System properties, with a Test button to validate values.
 *
 * Chat: - ai.api.key - ai.chat.model - ai.chat.url
 *
 * Embeddings: - ai.embed.model - ai.embed.url
 *
 * Timeout: - ai.timeout.ms
 */
public class AiChatModelSetupDialog extends Stage {

    private static final String PREF_NODE = "com.eb.ai";

    // ---- System property names (match AiFunctions) ----
    private static final String PROP_API_KEY = "ai.api.key";
    private static final String PROP_CHAT_MODEL = "ai.chat.model";
    private static final String PROP_CHAT_URL = "ai.chat.url";
    private static final String PROP_EMB_MODEL = "ai.embed.model";
    private static final String PROP_EMB_URL = "ai.embed.url";
    private static final String PROP_TIMEOUT_MS = "ai.timeout.ms";

    // ---- UI fields ----
    private final PasswordField txtApiKey;
    private final TextField txtChatModel;
    private final TextField txtChatUrl;

    private final TextField txtEmbModel;
    private final TextField txtEmbUrl;

    private final TextField txtTimeoutMs;

    public AiChatModelSetupDialog() {
        setTitle("AI Chat Model Setup");
        setAlwaysOnTop(true);
        loadPreferencesIntoSystemProps();
        initModality(Modality.WINDOW_MODAL);
//        initModality(Modality.APPLICATION_MODAL);

        // --- Create fields ---
        txtApiKey = new PasswordField();
        txtChatModel = new TextField();
        txtChatUrl = new TextField();

        txtEmbModel = new TextField();
        txtEmbUrl = new TextField();

        txtTimeoutMs = new TextField();

        // --- Prefill from System properties (empty if none) ---
        String apiKey = System.getProperty(PROP_API_KEY);
        txtApiKey.setText(System.getProperty(PROP_API_KEY, ""));
        txtChatModel.setText(System.getProperty(PROP_CHAT_MODEL, ""));
        txtChatUrl.setText(System.getProperty(PROP_CHAT_URL, ""));
        txtEmbModel.setText(System.getProperty(PROP_EMB_MODEL, ""));
        txtEmbUrl.setText(System.getProperty(PROP_EMB_URL, ""));
        txtTimeoutMs.setText(System.getProperty(PROP_TIMEOUT_MS, ""));

        txtApiKey.setPromptText("sk-... (optional, depends on provider)");
        txtChatModel.setPromptText("e.g. gpt-4o-mini");
        txtChatUrl.setPromptText("https://api.openai.com/v1/chat/completions");
        txtEmbModel.setPromptText("e.g. text-embedding-3-small");
        txtEmbUrl.setPromptText("https://api.openai.com/v1/embeddings");
        txtTimeoutMs.setPromptText("30000");

        setHgrow(txtChatModel, txtChatUrl, txtEmbModel, txtEmbUrl);

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;

        // Chat section
        grid.add(new Label("API Key:"), 0, row);
        grid.add(txtApiKey, 1, row++);

        grid.add(new Label("Chat Model:"), 0, row);
        grid.add(txtChatModel, 1, row++);

        grid.add(new Label("Chat URL:"), 0, row);
        grid.add(txtChatUrl, 1, row++);

        // Divider
        grid.add(new Separator(), 0, row++, 2, 1);

        // Embedding section
        grid.add(new Label("Embedding Model:"), 0, row);
        grid.add(txtEmbModel, 1, row++);

        grid.add(new Label("Embedding URL:"), 0, row);
        grid.add(txtEmbUrl, 1, row++);

        // Divider
        grid.add(new Separator(), 0, row++, 2, 1);

        // Timeout
        grid.add(new Label("Timeout (ms):"), 0, row);
        grid.add(txtTimeoutMs, 1, row++);

        // Buttons
        Button btnSave = new Button("Save");
        Button btnTest = new Button("Test…");
        Button btnClose = new Button("Close");
        
        // Add keyboard shortcuts to buttons
        ButtonShortcutHelper.addAltShortcut(btnSave, KeyCode.S);
        ButtonShortcutHelper.addAltShortcut(btnTest, KeyCode.T);
        ButtonShortcutHelper.addAltShortcut(btnClose, KeyCode.C);
        
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);

        GridPane buttons = new GridPane();
        buttons.setHgap(10);
        buttons.add(btnSave, 0, 0);
        buttons.add(btnTest, 1, 0);
        buttons.add(btnClose, 2, 0);
        grid.add(buttons, 1, row);

        // Actions
        btnSave.setOnAction(e -> onSave());
        btnClose.setOnAction(e -> close());
        btnTest.setOnAction(e -> onTest(btnSave, btnTest, btnClose)); // pass references to disable/enable

        setScene(new Scene(grid));
        sizeToScene();
    }

    private void onSave() {
        String apiKey = trimOrEmpty(txtApiKey.getText());
        String chatModel = trimOrEmpty(txtChatModel.getText());
        String chatUrl = trimOrEmpty(txtChatUrl.getText());
        String embModel = trimOrEmpty(txtEmbModel.getText());
        String embUrl = trimOrEmpty(txtEmbUrl.getText());
        String timeout = trimOrEmpty(txtTimeoutMs.getText());

        // Validation
        if (!chatUrl.isEmpty() && !isHttpUrl(chatUrl)) {
            showAlert(Alert.AlertType.ERROR, "Invalid URL", "Chat URL must start with http:// or https://");
            return;
        }
        if (!embUrl.isEmpty() && !isHttpUrl(embUrl)) {
            showAlert(Alert.AlertType.ERROR, "Invalid URL", "Embedding URL must start with http:// or https://");
            return;
        }
        if (!timeout.isEmpty()) {
            try {
                int t = Integer.parseInt(timeout);
                if (t <= 0) {
                    throw new NumberFormatException("non-positive");
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Timeout", "TIMEOUT_MS must be a positive integer.");
                return;
            }
        }

        // Save into System properties (used by AiFunctions)
//        System.setProperty(PROP_API_KEY, apiKey);
//        System.setProperty(PROP_CHAT_MODEL, chatModel);
//        System.setProperty(PROP_CHAT_URL, chatUrl);
//        System.setProperty(PROP_EMB_MODEL, embModel);
//        System.setProperty(PROP_EMB_URL, embUrl);
//        if (!timeout.isEmpty()) {
//            System.setProperty(PROP_TIMEOUT_MS, timeout);
//        } else {
//            System.clearProperty(PROP_TIMEOUT_MS);
//        }
        persistToPreferences(apiKey, chatModel, chatUrl, embModel, embUrl, timeout);

        showAlert(Alert.AlertType.INFORMATION, "Saved", "AI settings saved to System properties.");
    }

    // --- Test button handler ---
    private void onTest(Button... toToggle) {
        String apiKey = trimOrEmpty(txtApiKey.getText());
        String chatModel = trimOrEmpty(txtChatModel.getText());
        String chatUrl = trimOrEmpty(txtChatUrl.getText());
        String timeout = trimOrEmpty(txtTimeoutMs.getText());

        if (chatModel.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Model", "Please enter a Chat Model (e.g., gpt-4o-mini).");
            return;
        }
        if (chatUrl.isEmpty() || !isHttpUrl(chatUrl)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Chat URL", "Please enter a valid Chat URL (http/https).");
            return;
        }
        int tmo = 30_000;
        if (!timeout.isEmpty()) {
            try {
                tmo = Integer.parseInt(timeout);
                if (tmo <= 0) {
                    throw new NumberFormatException("non-positive");
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Timeout", "TIMEOUT_MS must be a positive integer.");
                return;
            }
        }

        setDisabled(true, toToggle);

        // Background thread for the HTTP call
        final int timeoutMs = tmo;
        new Thread(() -> {
            long t0 = System.nanoTime();
            try {
                String reply = testChat(chatUrl, chatModel, apiKey, timeoutMs);
                long ms = Math.round((System.nanoTime() - t0) / 1_000_000.0);
                String preview = truncate(reply, 220);
                Platform.runLater(() -> showAlert(
                        Alert.AlertType.INFORMATION,
                        "Connection OK",
                        "Model replied in " + ms + " ms:\n\n" + preview
                ));
            } catch (Exception ex) {
                long ms = Math.round((System.nanoTime() - t0) / 1_000_000.0);

                Platform.runLater(() -> showScrollableError(
                        "Test Failed",
                        "Error after " + ms + " ms:\n\n" + buildFullErrorText(ex)
                ));

            } finally {
                Platform.runLater(() -> setDisabled(false, toToggle));
            }
        }, "AI-Chat-Test").start();
    }

    // Make a small chat completion call with the current dialog values.
    private String testChat(String chatUrl, String model, String apiKey, int timeoutMs) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(5_000, timeoutMs / 2)))
                .build();

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("messages", List.of(Map.of("role", "user", "content", "Say OK")));

        String payload = Json.compactJson(req);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(chatUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json; charset=utf-8");

        if (!apiKey.isBlank()) {
            b.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest httpReq = b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();

        HttpResponse<String> res;
        try {
            res = http.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new IOException("Request timed out after " + timeoutMs + " ms", e);
        }

        if (res.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        }

        Object parsed = Json.parse(res.body());
        Object content = Json.getValue(parsed, "choices[0].message.content");
        if (content == null) {
            content = Json.getValue(parsed, "choices[0].text");
        }
        return Objects.toString(content, "");
    }

    private static boolean isHttpUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n) + " …";
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.initOwner(this);
//        a.initModality(Modality.APPLICATION_MODAL);
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }

    private static void setHgrow(TextField... fields) {
        for (TextField tf : fields) {
            GridPane.setHgrow(tf, Priority.ALWAYS);
            tf.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private static void setDisabled(boolean disabled, Button... buttons) {
        for (Button b : buttons) {
            b.setDisable(disabled);
        }
    }

    private static String safeString(Throwable ex) {
        String m = ex == null ? "" : (ex.getMessage() == null ? ex.toString() : ex.getMessage());
        // Optionally include cause line:
        if (ex != null && ex.getCause() != null) {
            m += "\n\nCause: " + ex.getCause();
        }
        return m;
    }

    private static String buildFullErrorText(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()); // class + message
        Throwable cause = ex.getCause();
        if (cause != null) {
            sb.append("\n\nCause: ").append(cause.toString());
        }
        // If you ever wrap Http status + body inside the message, it will all show.
        return sb.toString();
    }

    private void showScrollableError(String title, String longText) {
        // A TextArea gives us both scrollbars and easy copy/paste.
        TextArea ta = new TextArea(longText == null ? "" : longText);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(80);

        // Put the TextArea into a ScrollPane to ensure both axes scroll cleanly.
        ScrollPane sp = new ScrollPane(ta);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setPrefViewportHeight(320);
        sp.setPrefViewportWidth(720);

        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(this);
        a.initModality(Modality.WINDOW_MODAL);

        // Use the dialog pane's expandable area so it behaves like the standard "Show Details" UI,
        // but keep it expanded by default.
        a.getDialogPane().setContent(new Label("An error occurred. See details below:"));
        a.getDialogPane().setExpandableContent(sp);
        a.getDialogPane().setExpanded(true);

        // Make sure the dialog can grow large enough
        a.getDialogPane().setMinWidth(760);
        a.getDialogPane().setMinHeight(420);

        a.showAndWait();
    }

    private void persistToPreferences(String apiKey, String chatModel, String chatUrl, String embModel, String embUrl, String timeoutMs) {
        Preferences p = Preferences.userRoot().node(PREF_NODE);
        p.put("ai.api.key", apiKey);
        p.put("ai.chat.model", chatModel);
        p.put("ai.chat.url", chatUrl);
        p.put("ai.embed.model", embModel);
        p.put("ai.embed.url", embUrl);
        if (timeoutMs != null && !timeoutMs.isBlank()) {
            p.put("ai.timeout.ms", timeoutMs);
        } else {
            p.remove("ai.timeout.ms");
        }
    }

    private static void loadPreferencesIntoSystemProps() {
        Preferences p = Preferences.userRoot().node("com.eb.ai");
        copyIfPresent(p, "ai.api.key");
        copyIfPresent(p, "ai.chat.model");
        copyIfPresent(p, "ai.chat.url");
        copyIfPresent(p, "ai.embed.model");
        copyIfPresent(p, "ai.embed.url");
        copyIfPresent(p, "ai.timeout.ms");
    }

    private static void copyIfPresent(Preferences p, String key) {
        String v = p.get(key, null);
        if (v != null) {
            System.setProperty(key, v);
        }
    }

}
