package com.eb.ui.ebs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for configuring mail server connections that can be accessed by the script interpreter.
 * Allows users to define variable names and associate them with mail server connection URLs.
 * 
 * URL Format: mail://user:password@host:port?protocol=imaps
 * 
 * @author Earl Bosch
 */
public class MailConfigDialog extends Stage {

    private static final String PREF_NODE = "com.eb.mail";
    private static final String PREF_KEY_NAME_PREFIX = "mailVarName.";
    private static final String PREF_KEY_URL_PREFIX = "mailUrl.";
    private static final int MAX_MAIL_CONFIGS = 20;
    
    /** Mail URL pattern: mail://user:password@host:port?protocol=xxx */
    private static final Pattern MAIL_URL_PATTERN = Pattern.compile(
        "mail://([^:]+):([^@]*)@([^:]+):(\\d+)(?:\\?protocol=([a-zA-Z0-9]+))?"
    );

    private final TableView<MailConfigEntry> mailTableView;
    private final List<MailConfigEntry> mailConfigs;
    
    /**
     * Data model for a mail configuration entry (simplified to varName + URL).
     */
    public static class MailConfigEntry {
        private final StringProperty varName;
        private final StringProperty url;
        
        public MailConfigEntry(String varName, String url) {
            this.varName = new SimpleStringProperty(varName != null ? varName : "");
            this.url = new SimpleStringProperty(url != null ? url : "");
        }
        
        public String getVarName() { return varName.get(); }
        public void setVarName(String value) { varName.set(value != null ? value : ""); }
        public StringProperty varNameProperty() { return varName; }
        
        public String getUrl() { return url.get(); }
        public void setUrl(String value) { url.set(value != null ? value : ""); }
        public StringProperty urlProperty() { return url; }
        
        /**
         * Creates a mail URL from individual components.
         */
        public static String buildUrl(String host, String port, String user, String password, String protocol) {
            try {
                String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.name());
                String encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8.name());
                String proto = protocol != null && !protocol.isEmpty() ? protocol : "imaps";
                return String.format("mail://%s:%s@%s:%s?protocol=%s", 
                    encodedUser, encodedPass, host, port, proto);
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
        
        /**
         * Parses a mail URL into components.
         * @return Map with keys: host, port, user, password, protocol
         */
        public static Map<String, String> parseUrl(String url) {
            Map<String, String> result = new HashMap<>();
            if (url == null || url.isEmpty()) return result;
            
            Matcher m = MAIL_URL_PATTERN.matcher(url);
            if (m.matches()) {
                try {
                    result.put("user", URLDecoder.decode(m.group(1), StandardCharsets.UTF_8.name()));
                    result.put("password", URLDecoder.decode(m.group(2), StandardCharsets.UTF_8.name()));
                    result.put("host", m.group(3));
                    result.put("port", m.group(4));
                    result.put("protocol", m.group(5) != null ? m.group(5) : "imaps");
                } catch (UnsupportedEncodingException e) {
                    // Return empty map on error
                }
            }
            return result;
        }
    }

    public MailConfigDialog() {
        setTitle("Mail Server Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        mailConfigs = new ArrayList<>();
        loadMailConfigs();

        // --- Create TableView for mail configurations ---
        mailTableView = new TableView<>();
        mailTableView.setPrefHeight(300);
        mailTableView.setPrefWidth(900);
        mailTableView.setEditable(true);
        mailTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Variable Name column
        TableColumn<MailConfigEntry, String> nameColumn = new TableColumn<>("Variable");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("varName"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setMinWidth(100);
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> event.getRowValue().setVarName(event.getNewValue()));
        
        // URL column
        TableColumn<MailConfigEntry, String> urlColumn = new TableColumn<>("Mail URL (mail://user:password@host:port?protocol=imaps)");
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        urlColumn.setMinWidth(600);
        urlColumn.setEditable(true);
        urlColumn.setOnEditCommit(event -> event.getRowValue().setUrl(event.getNewValue()));
        
        mailTableView.getColumns().add(nameColumn);
        mailTableView.getColumns().add(urlColumn);
        
        refreshTableView();

        // --- Buttons ---
        Button btnAdd = new Button("Add Configuration");
        Button btnAddGmail = new Button("Add Gmail Template");
        Button btnRemove = new Button("Remove");
        Button btnSave = new Button("Save");
        Button btnClose = new Button("Close");
        
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);
        btnRemove.setDisable(true);

        // Enable/disable buttons based on selection
        mailTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnRemove.setDisable(!hasSelection);
        });

        // --- Actions ---
        btnAdd.setOnAction(e -> onAddConfig());
        btnAddGmail.setOnAction(e -> onAddGmailTemplate());
        btnRemove.setOnAction(e -> onRemoveConfig());
        btnSave.setOnAction(e -> onSave());
        btnClose.setOnAction(e -> close());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Configure mail server connections using URL format.\n\n" +
            "URL Format: mail://user:password@host:port?protocol=imaps\n\n" +
            "Examples:\n" +
            "• Gmail:   mail://user%40gmail.com:apppassword@imap.gmail.com:993?protocol=imaps\n" +
            "• Outlook: mail://user%40outlook.com:password@outlook.office365.com:993?protocol=imaps\n\n" +
            "Gmail App Password: Must be 16 characters with NO SPACES.\n" +
            "(Displayed as 'xxxx xxxx xxxx xxxx' but enter without spaces)\n\n" +
            "Note: Use %40 for @ in email addresses, URL-encode special characters in passwords."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnAdd, btnAddGmail, btnRemove);

        HBox bottomButtons = new HBox(10);
        bottomButtons.getChildren().addAll(btnSave, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            new Label("Mail Server Configurations:"),
            mailTableView,
            buttonBox,
            bottomButtons
        );

        VBox.setVgrow(mailTableView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(950);
        setMinHeight(550);
    }

    private void onAddConfig() {
        if (mailConfigs.size() >= MAX_MAIL_CONFIGS) {
            showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                "Maximum of " + MAX_MAIL_CONFIGS + " mail configurations allowed.");
            return;
        }

        mailConfigs.add(new MailConfigEntry("", "mail://user:password@host:993?protocol=imaps"));
        refreshTableView();
        mailTableView.getSelectionModel().selectLast();
        mailTableView.edit(mailConfigs.size() - 1, mailTableView.getColumns().get(0));
    }
    
    private void onAddGmailTemplate() {
        if (mailConfigs.size() >= MAX_MAIL_CONFIGS) {
            showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                "Maximum of " + MAX_MAIL_CONFIGS + " mail configurations allowed.");
            return;
        }

        mailConfigs.add(new MailConfigEntry("gmail", "mail://your-email%40gmail.com:your-16-char-app-password@imap.gmail.com:993?protocol=imaps"));
        refreshTableView();
        mailTableView.getSelectionModel().selectLast();
    }

    private void onRemoveConfig() {
        MailConfigEntry selected = mailTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            mailConfigs.remove(selected);
            refreshTableView();
        }
    }

    private void onSave() {
        try {
            // Validate entries before saving
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < mailConfigs.size(); i++) {
                MailConfigEntry entry = mailConfigs.get(i);
                String varName = entry.getVarName().trim();
                String url = entry.getUrl().trim();
                
                if (!varName.isEmpty() && url.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' has no URL");
                } else if (varName.isEmpty() && !url.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": URL has no variable name");
                } else if (!varName.isEmpty() && !varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' is invalid");
                } else if (!url.isEmpty() && !url.startsWith("mail://")) {
                    errors.add("Row " + (i + 1) + ": URL must start with 'mail://'");
                }
            }
            
            if (!errors.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", 
                    "Please fix the following errors:\n\n" + String.join("\n", errors));
                return;
            }
            
            // Check for duplicate variable names
            List<String> varNames = new ArrayList<>();
            for (MailConfigEntry entry : mailConfigs) {
                String varName = entry.getVarName().trim();
                if (!varName.isEmpty()) {
                    if (varNames.contains(varName)) {
                        showAlert(Alert.AlertType.ERROR, "Duplicate Variable Name", 
                            "Variable name '" + varName + "' is used more than once.");
                        return;
                    }
                    varNames.add(varName);
                }
            }
            
            saveMailConfigs();
            showAlert(Alert.AlertType.INFORMATION, "Saved", 
                "Mail configurations saved successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", 
                "Failed to save configuration: " + e.getMessage());
        }
    }

    private void refreshTableView() {
        mailTableView.getItems().clear();
        mailTableView.getItems().addAll(mailConfigs);
    }

    private void loadMailConfigs() {
        mailConfigs.clear();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String url = prefs.get(PREF_KEY_URL_PREFIX + i, null);
            if (varName != null || url != null) {
                mailConfigs.add(new MailConfigEntry(
                    varName != null ? varName : "",
                    url != null ? url : ""
                ));
            }
        }
    }

    private void saveMailConfigs() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
            prefs.remove(PREF_KEY_URL_PREFIX + i);
        }
        
        // Save current list (only entries with variable name and URL)
        int index = 0;
        for (MailConfigEntry entry : mailConfigs) {
            String varName = entry.getVarName().trim();
            String url = entry.getUrl().trim();
            
            if (!varName.isEmpty() && !url.isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + index, varName);
                prefs.put(PREF_KEY_URL_PREFIX + index, url);
                index++;
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.initOwner(this);
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }

    /**
     * Static method to retrieve the list of mail configuration entries from preferences.
     * Returns entries as (varName, URL string) pairs.
     */
    public static List<MailConfigEntry> getMailConfigEntries() {
        List<MailConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String url = prefs.get(PREF_KEY_URL_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && url != null && !url.isEmpty()) {
                entries.add(new MailConfigEntry(varName, url));
            }
        }
        
        return entries;
    }
    
    /**
     * Parses a mail URL into its components.
     * @param url Mail URL in format mail://user:password@host:port?protocol=xxx
     * @return Map with keys: host, port, user, password, protocol (or empty map if invalid)
     */
    public static Map<String, String> parseMailUrl(String url) {
        return MailConfigEntry.parseUrl(url);
    }
    
    /**
     * Builds a mail URL from components.
     * @return Mail URL string
     */
    public static String buildMailUrl(String host, String port, String user, String password, String protocol) {
        return MailConfigEntry.buildUrl(host, port, user, password, protocol);
    }
}
