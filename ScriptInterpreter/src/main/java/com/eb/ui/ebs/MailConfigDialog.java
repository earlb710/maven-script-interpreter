package com.eb.ui.ebs;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
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
 * Allows users to define variable names and associate them with mail server connection parameters.
 * 
 * @author Earl Bosch
 */
public class MailConfigDialog extends Stage {

    private static final String PREF_NODE = "com.eb.mail";
    private static final String PREF_KEY_NAME_PREFIX = "mailVarName.";
    private static final String PREF_KEY_HOST_PREFIX = "mailHost.";
    private static final String PREF_KEY_PORT_PREFIX = "mailPort.";
    private static final String PREF_KEY_USER_PREFIX = "mailUser.";
    private static final String PREF_KEY_PASS_PREFIX = "mailPass.";
    private static final String PREF_KEY_PROTO_PREFIX = "mailProto.";
    private static final int MAX_MAIL_CONFIGS = 20;

    private final TableView<MailConfigEntry> mailTableView;
    private final List<MailConfigEntry> mailConfigs;
    
    /**
     * Data model for a mail configuration entry.
     */
    public static class MailConfigEntry {
        private final StringProperty varName;
        private final StringProperty host;
        private final StringProperty port;
        private final StringProperty user;
        private final StringProperty password;
        private final StringProperty protocol;
        
        public MailConfigEntry(String varName, String host, String port, String user, String password, String protocol) {
            this.varName = new SimpleStringProperty(varName != null ? varName : "");
            this.host = new SimpleStringProperty(host != null ? host : "");
            this.port = new SimpleStringProperty(port != null ? port : "993");
            this.user = new SimpleStringProperty(user != null ? user : "");
            this.password = new SimpleStringProperty(password != null ? password : "");
            this.protocol = new SimpleStringProperty(protocol != null ? protocol : "imaps");
        }
        
        public String getVarName() { return varName.get(); }
        public void setVarName(String value) { varName.set(value != null ? value : ""); }
        public StringProperty varNameProperty() { return varName; }
        
        public String getHost() { return host.get(); }
        public void setHost(String value) { host.set(value != null ? value : ""); }
        public StringProperty hostProperty() { return host; }
        
        public String getPort() { return port.get(); }
        public void setPort(String value) { port.set(value != null ? value : "993"); }
        public StringProperty portProperty() { return port; }
        
        public String getUser() { return user.get(); }
        public void setUser(String value) { user.set(value != null ? value : ""); }
        public StringProperty userProperty() { return user; }
        
        public String getPassword() { return password.get(); }
        public void setPassword(String value) { password.set(value != null ? value : ""); }
        public StringProperty passwordProperty() { return password; }
        
        public String getProtocol() { return protocol.get(); }
        public void setProtocol(String value) { protocol.set(value != null ? value : "imaps"); }
        public StringProperty protocolProperty() { return protocol; }
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
        nameColumn.setMinWidth(80);
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> event.getRowValue().setVarName(event.getNewValue()));
        
        // Host column
        TableColumn<MailConfigEntry, String> hostColumn = new TableColumn<>("Host");
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        hostColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        hostColumn.setMinWidth(150);
        hostColumn.setEditable(true);
        hostColumn.setOnEditCommit(event -> event.getRowValue().setHost(event.getNewValue()));
        
        // Port column
        TableColumn<MailConfigEntry, String> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        portColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        portColumn.setMinWidth(60);
        portColumn.setEditable(true);
        portColumn.setOnEditCommit(event -> event.getRowValue().setPort(event.getNewValue()));
        
        // User column
        TableColumn<MailConfigEntry, String> userColumn = new TableColumn<>("User/Email");
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        userColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        userColumn.setMinWidth(180);
        userColumn.setEditable(true);
        userColumn.setOnEditCommit(event -> event.getRowValue().setUser(event.getNewValue()));
        
        // Password column (shows masked)
        TableColumn<MailConfigEntry, String> passColumn = new TableColumn<>("Password");
        passColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
        passColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        passColumn.setMinWidth(150);
        passColumn.setEditable(true);
        passColumn.setOnEditCommit(event -> event.getRowValue().setPassword(event.getNewValue()));
        
        // Protocol column
        TableColumn<MailConfigEntry, String> protoColumn = new TableColumn<>("Protocol");
        protoColumn.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        protoColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        protoColumn.setMinWidth(80);
        protoColumn.setEditable(true);
        protoColumn.setOnEditCommit(event -> event.getRowValue().setProtocol(event.getNewValue()));
        
        mailTableView.getColumns().addAll(nameColumn, hostColumn, portColumn, userColumn, passColumn, protoColumn);
        
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
            "Configure mail server connections with variable names.\n" +
            "These configurations provide quick access to mail servers in scripts.\n\n" +
            "Gmail App Password: Must be 16 characters with NO SPACES.\n" +
            "(Displayed as 'xxxx xxxx xxxx xxxx' but enter without spaces)\n" +
            "Generate from: Google Account > Security > App Passwords\n\n" +
            "Common Settings:\n" +
            "• Gmail IMAPS: imap.gmail.com:993 (protocol: imaps)\n" +
            "• Gmail POP3S: pop.gmail.com:995 (protocol: pop3s)\n" +
            "• Outlook IMAPS: outlook.office365.com:993 (protocol: imaps)"
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

        mailConfigs.add(new MailConfigEntry("", "", "993", "", "", "imaps"));
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

        mailConfigs.add(new MailConfigEntry("gmail", "imap.gmail.com", "993", "your-email@gmail.com", "", "imaps"));
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
                String host = entry.getHost().trim();
                
                if (!varName.isEmpty() && host.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' has no host");
                } else if (varName.isEmpty() && !host.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Host has no variable name");
                } else if (!varName.isEmpty() && !varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' is invalid");
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
            String host = prefs.get(PREF_KEY_HOST_PREFIX + i, null);
            if (varName != null || host != null) {
                mailConfigs.add(new MailConfigEntry(
                    varName != null ? varName : "",
                    host != null ? host : "",
                    prefs.get(PREF_KEY_PORT_PREFIX + i, "993"),
                    prefs.get(PREF_KEY_USER_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PASS_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PROTO_PREFIX + i, "imaps")
                ));
            }
        }
    }

    private void saveMailConfigs() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
            prefs.remove(PREF_KEY_HOST_PREFIX + i);
            prefs.remove(PREF_KEY_PORT_PREFIX + i);
            prefs.remove(PREF_KEY_USER_PREFIX + i);
            prefs.remove(PREF_KEY_PASS_PREFIX + i);
            prefs.remove(PREF_KEY_PROTO_PREFIX + i);
        }
        
        // Save current list (only entries with variable name and host)
        int index = 0;
        for (MailConfigEntry entry : mailConfigs) {
            String varName = entry.getVarName().trim();
            String host = entry.getHost().trim();
            
            if (!varName.isEmpty() && !host.isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + index, varName);
                prefs.put(PREF_KEY_HOST_PREFIX + index, host);
                prefs.put(PREF_KEY_PORT_PREFIX + index, entry.getPort().trim());
                prefs.put(PREF_KEY_USER_PREFIX + index, entry.getUser().trim());
                prefs.put(PREF_KEY_PASS_PREFIX + index, entry.getPassword());
                prefs.put(PREF_KEY_PROTO_PREFIX + index, entry.getProtocol().trim());
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
     */
    public static List<MailConfigEntry> getMailConfigEntries() {
        List<MailConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_MAIL_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String host = prefs.get(PREF_KEY_HOST_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && host != null && !host.isEmpty()) {
                entries.add(new MailConfigEntry(
                    varName,
                    host,
                    prefs.get(PREF_KEY_PORT_PREFIX + i, "993"),
                    prefs.get(PREF_KEY_USER_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PASS_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PROTO_PREFIX + i, "imaps")
                ));
            }
        }
        
        return entries;
    }
}
