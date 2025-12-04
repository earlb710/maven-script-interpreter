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
    
    /** Mail URL pattern: mail://user[:password]@host:port?protocol=xxx (password is optional) */
    private static final Pattern MAIL_URL_PATTERN = Pattern.compile(
        "mail://([^:@]+)(?::([^@]*))?@([^:]+):(\\d+)(?:\\?protocol=([a-zA-Z0-9]+))?"
    );

    private final TableView<MailConfigEntry> mailTableView;
    private final List<MailConfigEntry> mailConfigs;
    private static final String PREF_KEY_PASS_PREFIX = "mailPass.";
    
    /**
     * Data model for a mail configuration entry with separate password field.
     * URL format: mail://user@host:port?protocol=imaps (password stored separately)
     */
    public static class MailConfigEntry {
        private final StringProperty varName;
        private final StringProperty url;
        private final StringProperty password;
        
        public MailConfigEntry(String varName, String url, String password) {
            this.varName = new SimpleStringProperty(varName != null ? varName : "");
            this.url = new SimpleStringProperty(url != null ? url : "");
            this.password = new SimpleStringProperty(password != null ? password : "");
        }
        
        // Backward compatibility constructor
        public MailConfigEntry(String varName, String url) {
            this(varName, url, "");
        }
        
        public String getVarName() { return varName.get(); }
        public void setVarName(String value) { varName.set(value != null ? value : ""); }
        public StringProperty varNameProperty() { return varName; }
        
        public String getUrl() { return url.get(); }
        public void setUrl(String value) { url.set(value != null ? value : ""); }
        public StringProperty urlProperty() { return url; }
        
        public String getPassword() { return password.get(); }
        public void setPassword(String value) { password.set(value != null ? value : ""); }
        public StringProperty passwordProperty() { return password; }
        
        /**
         * Gets the full URL with password for connection purposes.
         */
        public String getFullUrlWithPassword() {
            String baseUrl = url.get();
            String pass = password.get();
            if (pass == null || pass.isEmpty()) {
                return baseUrl;
            }
            // Insert password into URL
            Map<String, String> parsed = parseUrl(baseUrl);
            if (parsed.isEmpty()) {
                return baseUrl;
            }
            return buildUrl(parsed.get("host"), parsed.get("port"), 
                          parsed.get("user"), pass, parsed.get("protocol"));
        }
        
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
         * @return Map with keys: host, port, user, password, protocol (password may be empty if not in URL)
         */
        public static Map<String, String> parseUrl(String url) {
            Map<String, String> result = new HashMap<>();
            if (url == null || url.isEmpty()) return result;
            
            Matcher m = MAIL_URL_PATTERN.matcher(url);
            if (m.matches()) {
                try {
                    result.put("user", URLDecoder.decode(m.group(1), StandardCharsets.UTF_8.name()));
                    // Password is now optional (group 2 may be null)
                    String password = m.group(2);
                    result.put("password", password != null ? URLDecoder.decode(password, StandardCharsets.UTF_8.name()) : "");
                    result.put("host", m.group(3));
                    result.put("port", m.group(4));
                    result.put("protocol", m.group(5) != null ? m.group(5) : "imaps");
                } catch (UnsupportedEncodingException e) {
                    // Return empty map on error
                }
            }
            return result;
        }
        
        /**
         * Creates a mail URL from individual components.
         * Password can be omitted if empty (URL will not contain the :password part).
         */
        public static String buildUrlNoPassword(String host, String port, String user, String protocol) {
            try {
                String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.name());
                String proto = protocol != null && !protocol.isEmpty() ? protocol : "imaps";
                return String.format("mail://%s@%s:%s?protocol=%s", 
                    encodedUser, host, port, proto);
            } catch (UnsupportedEncodingException e) {
                return "";
            }
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
        nameColumn.setOnEditCommit(event -> {
            event.getRowValue().setVarName(event.getNewValue());
        });
        // Commit edit when clicking elsewhere (fix for focus loss data loss bug)
        nameColumn.setCellFactory(column -> {
            TextFieldTableCell<MailConfigEntry, String> cell = new TextFieldTableCell<MailConfigEntry, String>() {
                private TextField textField;
                
                @Override
                public void startEdit() {
                    super.startEdit();
                    // Get the text field via reflection or by accessing the graphic
                    if (getGraphic() instanceof TextField) {
                        textField = (TextField) getGraphic();
                        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                            if (!isNowFocused && isEditing()) {
                                commitEdit(textField.getText());
                            }
                        });
                    }
                }
            };
            cell.setConverter(new javafx.util.converter.DefaultStringConverter());
            return cell;
        });
        
        // URL column (without password)
        TableColumn<MailConfigEntry, String> urlColumn = new TableColumn<>("Mail URL (mail://user@host:port?protocol=imaps)");
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        urlColumn.setMinWidth(450);
        urlColumn.setEditable(true);
        urlColumn.setOnEditCommit(event -> {
            event.getRowValue().setUrl(event.getNewValue());
        });
        // Commit edit when clicking elsewhere (fix for focus loss data loss bug)
        urlColumn.setCellFactory(column -> {
            TextFieldTableCell<MailConfigEntry, String> cell = new TextFieldTableCell<MailConfigEntry, String>() {
                private TextField textField;
                
                @Override
                public void startEdit() {
                    super.startEdit();
                    if (getGraphic() instanceof TextField) {
                        textField = (TextField) getGraphic();
                        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                            if (!isNowFocused && isEditing()) {
                                commitEdit(textField.getText());
                            }
                        });
                    }
                }
            };
            cell.setConverter(new javafx.util.converter.DefaultStringConverter());
            return cell;
        });
        
        // Password column with masked display
        TableColumn<MailConfigEntry, String> passwordColumn = new TableColumn<>("Password");
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
        passwordColumn.setCellFactory(column -> new PasswordTableCell());
        passwordColumn.setMinWidth(150);
        passwordColumn.setEditable(true);
        passwordColumn.setOnEditCommit(event -> event.getRowValue().setPassword(event.getNewValue()));
        
        mailTableView.getColumns().add(nameColumn);
        mailTableView.getColumns().add(urlColumn);
        mailTableView.getColumns().add(passwordColumn);
        
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
        // Also commit any pending edits when selection changes to prevent data loss
        mailTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Commit any pending edits before changing selection
            if (mailTableView.getEditingCell() != null) {
                // Force commit by requesting focus elsewhere temporarily
                mailTableView.requestFocus();
            }
            boolean hasSelection = newVal != null;
            btnRemove.setDisable(!hasSelection);
        });
        
        // Commit edits when focus is lost from the table
        mailTableView.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && mailTableView.getEditingCell() != null) {
                mailTableView.requestFocus();
            }
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
            "Configure mail server connections. Password is stored separately (not in URL).\n\n" +
            "URL Format: mail://user@host:port?protocol=imaps\n\n" +
            "Examples:\n" +
            "• Gmail:   mail://user%40gmail.com@imap.gmail.com:993?protocol=imaps\n" +
            "• Outlook: mail://user%40outlook.com@outlook.office365.com:993?protocol=imaps\n\n" +
            "Gmail App Password: Must be 16 characters with NO SPACES.\n" +
            "(Displayed as 'xxxx xxxx xxxx xxxx' but enter without spaces)\n\n" +
            "Note: Use %40 for @ in email addresses. Password is entered in separate column."
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
        setMinWidth(1000);
        setMinHeight(550);
    }

    private void onAddConfig() {
        if (mailConfigs.size() >= MAX_MAIL_CONFIGS) {
            showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                "Maximum of " + MAX_MAIL_CONFIGS + " mail configurations allowed.");
            return;
        }

        mailConfigs.add(new MailConfigEntry("", "mail://user@host:993?protocol=imaps", ""));
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

        mailConfigs.add(new MailConfigEntry("gmail", "mail://your-email%40gmail.com@imap.gmail.com:993?protocol=imaps", "your-16-char-app-password"));
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
            String password = prefs.get(PREF_KEY_PASS_PREFIX + i, null);
            if (varName != null || url != null) {
                mailConfigs.add(new MailConfigEntry(
                    varName != null ? varName : "",
                    url != null ? url : "",
                    password != null ? password : ""
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
            prefs.remove(PREF_KEY_PASS_PREFIX + i);
        }
        
        // Save current list (only entries with variable name and URL)
        int index = 0;
        for (MailConfigEntry entry : mailConfigs) {
            String varName = entry.getVarName().trim();
            String url = entry.getUrl().trim();
            String password = entry.getPassword();
            
            if (!varName.isEmpty() && !url.isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + index, varName);
                prefs.put(PREF_KEY_URL_PREFIX + index, url);
                if (password != null && !password.isEmpty()) {
                    prefs.put(PREF_KEY_PASS_PREFIX + index, password);
                }
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
     * Returns entries as (varName, URL string, password) tuples.
     */
    public static List<MailConfigEntry> getMailConfigEntries() {
        List<MailConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String url = prefs.get(PREF_KEY_URL_PREFIX + i, null);
            String password = prefs.get(PREF_KEY_PASS_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && url != null && !url.isEmpty()) {
                entries.add(new MailConfigEntry(varName, url, password != null ? password : ""));
            }
        }
        
        return entries;
    }
    
    /**
     * Parses a mail URL into its components.
     * @param url Mail URL in format mail://user[:password]@host:port?protocol=xxx
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
    
    /**
     * Custom table cell that displays password as masked characters but allows editing.
     */
    private class PasswordTableCell extends TableCell<MailConfigEntry, String> {
        private TextField textField;
        private boolean showPassword = false;
        
        public PasswordTableCell() {
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !isEmpty()) {
                    startEdit();
                }
            });
        }
        
        @Override
        public void startEdit() {
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            textField.setText(getItem() != null ? getItem() : "");
            setGraphic(textField);
            setText(null);
            textField.selectAll();
            textField.requestFocus();
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getMaskedPassword(getItem()));
            setGraphic(null);
        }
        
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item);
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getMaskedPassword(item));
                    setGraphic(null);
                }
            }
        }
        
        private void createTextField() {
            textField = new TextField();
            textField.setOnAction(event -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEdit(textField.getText());
                }
            });
            textField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }
        
        private String getMaskedPassword(String password) {
            if (password == null || password.isEmpty()) {
                return "";
            }
            // Show last 4 characters, mask the rest
            if (password.length() <= 4) {
                return "●".repeat(password.length());
            }
            return "●".repeat(password.length() - 4) + password.substring(password.length() - 4);
        }
    }
}
