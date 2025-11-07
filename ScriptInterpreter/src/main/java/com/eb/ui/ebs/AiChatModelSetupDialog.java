package com.eb.ui.ebs;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * "AI Chat Model Setup" popup that captures the AiFunctions settings and stores
 * them into System properties:
 *
 *  Chat:
 *   - ai.api.key
 *   - ai.chat.model
 *   - ai.chat.url
 *
 *  Embeddings:
 *   - ai.embed.model
 *   - ai.embed.url
 *
 *  Timeout:
 *   - ai.timeout.ms
 */
public class AiChatModelSetupDialog extends Stage {

    // ---- System property names (match AiFunctions) ----
    private static final String PROP_API_KEY     = "ai.api.key";
    private static final String PROP_CHAT_MODEL  = "ai.chat.model";
    private static final String PROP_CHAT_URL    = "ai.chat.url";
    private static final String PROP_EMB_MODEL   = "ai.embed.model";
    private static final String PROP_EMB_URL     = "ai.embed.url";
    private static final String PROP_TIMEOUT_MS  = "ai.timeout.ms";

    // ---- UI fields ----
    private final PasswordField txtApiKey;
    private final TextField     txtChatModel;
    private final TextField     txtChatUrl;

    private final TextField     txtEmbModel;
    private final TextField     txtEmbUrl;

    private final TextField     txtTimeoutMs;

    public AiChatModelSetupDialog() {
        setTitle("AI Chat Model Setup");
        initModality(Modality.APPLICATION_MODAL);

        // --- Create fields ---
        txtApiKey    = new PasswordField();
        txtChatModel = new TextField();
        txtChatUrl   = new TextField();

        txtEmbModel  = new TextField();
        txtEmbUrl    = new TextField();

        txtTimeoutMs = new TextField();

        // --- Prefill from current System properties (empty if none) ---
        txtApiKey.setText(System.getProperty(PROP_API_KEY, ""));
        txtChatModel.setText(System.getProperty(PROP_CHAT_MODEL, ""));
        txtChatUrl.setText(System.getProperty(PROP_CHAT_URL, ""));
        txtEmbModel.setText(System.getProperty(PROP_EMB_MODEL, ""));
        txtEmbUrl.setText(System.getProperty(PROP_EMB_URL, ""));
        txtTimeoutMs.setText(System.getProperty(PROP_TIMEOUT_MS, ""));

        // Prompts for clarity
        txtApiKey.setPromptText("sk-... (optional, depends on provider)");
        txtChatModel.setPromptText("e.g. gpt-4o-mini");
        txtChatUrl.setPromptText("https://api.openai.com/v1/chat/completions");

        txtEmbModel.setPromptText("e.g. text-embedding-3-small");
        txtEmbUrl.setPromptText("https://api.openai.com/v1/embeddings");

        txtTimeoutMs.setPromptText("30000");

        // Make URL/model fields stretch on resize
        setHgrow(txtChatModel, txtChatUrl, txtEmbModel, txtEmbUrl);

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;

        // CHAT section
        grid.add(new Label("API Key:"), 0, row);
        grid.add(txtApiKey, 1, row++);

        grid.add(new Label("Chat Model:"), 0, row);
        grid.add(txtChatModel, 1, row++);

        grid.add(new Label("Chat URL:"), 0, row);
        grid.add(txtChatUrl, 1, row++);

        // --- Divider (between chat and embedding) ---
        grid.add(new Separator(), 0, row++, 2, 1);

        // EMBEDDING section
        grid.add(new Label("Embedding Model:"), 0, row);
        grid.add(txtEmbModel, 1, row++);

        grid.add(new Label("Embedding URL:"), 0, row);
        grid.add(txtEmbUrl, 1, row++);

        // --- Divider (between embedding and timeout) ---
        grid.add(new Separator(), 0, row++, 2, 1);

        // TIMEOUT section
        grid.add(new Label("Timeout (ms):"), 0, row);
        grid.add(txtTimeoutMs, 1, row++);

        // Buttons
        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        GridPane buttons = new GridPane();
        buttons.setHgap(10);
        buttons.add(btnSave, 0, 0);
        buttons.add(btnCancel, 1, 0);
        grid.add(buttons, 1, row);

        // --- Actions ---
        btnSave.setOnAction(e -> onSave());
        btnCancel.setOnAction(e -> close());

        setScene(new Scene(grid));
        sizeToScene();
    }

    private void onSave() {
        String apiKey    = trimOrEmpty(txtApiKey.getText());
        String chatModel = trimOrEmpty(txtChatModel.getText());
        String chatUrl   = trimOrEmpty(txtChatUrl.getText());

        String embModel  = trimOrEmpty(txtEmbModel.getText());
        String embUrl    = trimOrEmpty(txtEmbUrl.getText());

        String timeout   = trimOrEmpty(txtTimeoutMs.getText());

        // Basic validation
        if (!chatUrl.isEmpty() && !isHttpUrl(chatUrl)) {
            showAlert(Alert.AlertType.ERROR, "Invalid URL",
                    "Chat URL must start with http:// or https://");
            return;
        }
        if (!embUrl.isEmpty() && !isHttpUrl(embUrl)) {
            showAlert(Alert.AlertType.ERROR, "Invalid URL",
                    "Embedding URL must start with http:// or https://");
            return;
        }
        if (!timeout.isEmpty()) {
            try {
                int t = Integer.parseInt(timeout);
                if (t <= 0) throw new NumberFormatException("non-positive");
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Timeout",
                        "TIMEOUT_MS must be a positive integer.");
                return;
            }
        }

        // Save into System properties (picked up by AiFunctions)
        System.setProperty(PROP_API_KEY,     apiKey);
        System.setProperty(PROP_CHAT_MODEL,  chatModel);
        System.setProperty(PROP_CHAT_URL,    chatUrl);
        System.setProperty(PROP_EMB_MODEL,   embModel);
        System.setProperty(PROP_EMB_URL,     embUrl);
        if (!timeout.isEmpty()) {
            System.setProperty(PROP_TIMEOUT_MS, timeout);
        } else {
            // Clear if left blank (optional behavior)
            System.clearProperty(PROP_TIMEOUT_MS);
        }

        showAlert(Alert.AlertType.INFORMATION, "Saved",
                "AI settings saved to System properties.");
        close();
    }

    private static boolean isHttpUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.initOwner(getOwner());
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }

    private static void setHgrow(TextField... fields) {
        for (TextField tf : fields) {
            GridPane.setHgrow(tf, Priority.ALWAYS);
            tf.setMaxWidth(Double.MAX_VALUE);
        }
    }
}
