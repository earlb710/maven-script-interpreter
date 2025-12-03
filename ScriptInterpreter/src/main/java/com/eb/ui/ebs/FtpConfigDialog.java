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
 * Dialog for configuring FTP server connections that can be accessed by the script interpreter.
 * Allows users to define variable names and associate them with FTP server connection parameters.
 * 
 * @author Earl Bosch
 */
public class FtpConfigDialog extends Stage {

    private static final String PREF_NODE = "com.eb.ftp";
    private static final String PREF_KEY_NAME_PREFIX = "ftpVarName.";
    private static final String PREF_KEY_HOST_PREFIX = "ftpHost.";
    private static final String PREF_KEY_PORT_PREFIX = "ftpPort.";
    private static final String PREF_KEY_USER_PREFIX = "ftpUser.";
    private static final String PREF_KEY_PASS_PREFIX = "ftpPass.";
    private static final String PREF_KEY_SECURE_PREFIX = "ftpSecure.";
    private static final int MAX_FTP_CONFIGS = 20;

    private final TableView<FtpConfigEntry> ftpTableView;
    private final List<FtpConfigEntry> ftpConfigs;
    
    /**
     * Data model for an FTP configuration entry.
     */
    public static class FtpConfigEntry {
        private final StringProperty varName;
        private final StringProperty host;
        private final StringProperty port;
        private final StringProperty user;
        private final StringProperty password;
        private final StringProperty secure;
        
        public FtpConfigEntry(String varName, String host, String port, String user, String password, String secure) {
            this.varName = new SimpleStringProperty(varName != null ? varName : "");
            this.host = new SimpleStringProperty(host != null ? host : "");
            this.port = new SimpleStringProperty(port != null ? port : "21");
            this.user = new SimpleStringProperty(user != null ? user : "");
            this.password = new SimpleStringProperty(password != null ? password : "");
            this.secure = new SimpleStringProperty(secure != null ? secure : "no");
        }
        
        public String getVarName() { return varName.get(); }
        public void setVarName(String value) { varName.set(value != null ? value : ""); }
        public StringProperty varNameProperty() { return varName; }
        
        public String getHost() { return host.get(); }
        public void setHost(String value) { host.set(value != null ? value : ""); }
        public StringProperty hostProperty() { return host; }
        
        public String getPort() { return port.get(); }
        public void setPort(String value) { port.set(value != null ? value : "21"); }
        public StringProperty portProperty() { return port; }
        
        public String getUser() { return user.get(); }
        public void setUser(String value) { user.set(value != null ? value : ""); }
        public StringProperty userProperty() { return user; }
        
        public String getPassword() { return password.get(); }
        public void setPassword(String value) { password.set(value != null ? value : ""); }
        public StringProperty passwordProperty() { return password; }
        
        public String getSecure() { return secure.get(); }
        public void setSecure(String value) { secure.set(value != null ? value : "no"); }
        public StringProperty secureProperty() { return secure; }
        
        public boolean isSecure() {
            String s = secure.get().toLowerCase();
            return s.equals("yes") || s.equals("true") || s.equals("ftps");
        }
    }

    public FtpConfigDialog() {
        setTitle("FTP Server Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        ftpConfigs = new ArrayList<>();
        loadFtpConfigs();

        // --- Create TableView for FTP configurations ---
        ftpTableView = new TableView<>();
        ftpTableView.setPrefHeight(300);
        ftpTableView.setPrefWidth(850);
        ftpTableView.setEditable(true);
        ftpTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Variable Name column
        TableColumn<FtpConfigEntry, String> nameColumn = new TableColumn<>("Variable");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("varName"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setMinWidth(80);
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> event.getRowValue().setVarName(event.getNewValue()));
        
        // Host column
        TableColumn<FtpConfigEntry, String> hostColumn = new TableColumn<>("Host");
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        hostColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        hostColumn.setMinWidth(180);
        hostColumn.setEditable(true);
        hostColumn.setOnEditCommit(event -> event.getRowValue().setHost(event.getNewValue()));
        
        // Port column
        TableColumn<FtpConfigEntry, String> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        portColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        portColumn.setMinWidth(60);
        portColumn.setEditable(true);
        portColumn.setOnEditCommit(event -> event.getRowValue().setPort(event.getNewValue()));
        
        // User column
        TableColumn<FtpConfigEntry, String> userColumn = new TableColumn<>("Username");
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        userColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        userColumn.setMinWidth(120);
        userColumn.setEditable(true);
        userColumn.setOnEditCommit(event -> event.getRowValue().setUser(event.getNewValue()));
        
        // Password column
        TableColumn<FtpConfigEntry, String> passColumn = new TableColumn<>("Password");
        passColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
        passColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        passColumn.setMinWidth(120);
        passColumn.setEditable(true);
        passColumn.setOnEditCommit(event -> event.getRowValue().setPassword(event.getNewValue()));
        
        // Secure (FTPS) column
        TableColumn<FtpConfigEntry, String> secureColumn = new TableColumn<>("Secure (FTPS)");
        secureColumn.setCellValueFactory(new PropertyValueFactory<>("secure"));
        secureColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        secureColumn.setMinWidth(100);
        secureColumn.setEditable(true);
        secureColumn.setOnEditCommit(event -> event.getRowValue().setSecure(event.getNewValue()));
        
        ftpTableView.getColumns().addAll(nameColumn, hostColumn, portColumn, userColumn, passColumn, secureColumn);
        
        refreshTableView();

        // --- Buttons ---
        Button btnAdd = new Button("Add Configuration");
        Button btnRemove = new Button("Remove");
        Button btnSave = new Button("Save");
        Button btnClose = new Button("Close");
        
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);
        btnRemove.setDisable(true);

        // Enable/disable buttons based on selection
        ftpTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnRemove.setDisable(!hasSelection);
        });

        // --- Actions ---
        btnAdd.setOnAction(e -> onAddConfig());
        btnRemove.setOnAction(e -> onRemoveConfig());
        btnSave.setOnAction(e -> onSave());
        btnClose.setOnAction(e -> close());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Configure FTP server connections with variable names.\n" +
            "These configurations provide quick access to FTP servers in scripts.\n\n" +
            "Secure (FTPS) Column: Enter 'yes', 'true', or 'ftps' for secure connections.\n" +
            "Leave empty or 'no' for regular FTP.\n\n" +
            "Common Ports:\n" +
            "• FTP: 21 (default)\n" +
            "• FTPS (explicit TLS): 21\n" +
            "• FTPS (implicit SSL): 990"
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnAdd, btnRemove);

        HBox bottomButtons = new HBox(10);
        bottomButtons.getChildren().addAll(btnSave, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            new Label("FTP Server Configurations:"),
            ftpTableView,
            buttonBox,
            bottomButtons
        );

        VBox.setVgrow(ftpTableView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(900);
        setMinHeight(500);
    }

    private void onAddConfig() {
        if (ftpConfigs.size() >= MAX_FTP_CONFIGS) {
            showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                "Maximum of " + MAX_FTP_CONFIGS + " FTP configurations allowed.");
            return;
        }

        ftpConfigs.add(new FtpConfigEntry("", "", "21", "", "", "no"));
        refreshTableView();
        ftpTableView.getSelectionModel().selectLast();
        ftpTableView.edit(ftpConfigs.size() - 1, ftpTableView.getColumns().get(0));
    }

    private void onRemoveConfig() {
        FtpConfigEntry selected = ftpTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ftpConfigs.remove(selected);
            refreshTableView();
        }
    }

    private void onSave() {
        try {
            // Validate entries before saving
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < ftpConfigs.size(); i++) {
                FtpConfigEntry entry = ftpConfigs.get(i);
                String varName = entry.getVarName().trim();
                String host = entry.getHost().trim();
                
                if (!varName.isEmpty() && host.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' has no host");
                } else if (varName.isEmpty() && !host.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Host has no variable name");
                } else if (!varName.isEmpty() && !varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' is invalid");
                }
                
                // Validate port
                try {
                    int port = Integer.parseInt(entry.getPort().trim());
                    if (port < 1 || port > 65535) {
                        errors.add("Row " + (i + 1) + ": Port must be between 1 and 65535");
                    }
                } catch (NumberFormatException e) {
                    if (!entry.getHost().trim().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Invalid port number");
                    }
                }
            }
            
            if (!errors.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", 
                    "Please fix the following errors:\n\n" + String.join("\n", errors));
                return;
            }
            
            // Check for duplicate variable names
            List<String> varNames = new ArrayList<>();
            for (FtpConfigEntry entry : ftpConfigs) {
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
            
            saveFtpConfigs();
            showAlert(Alert.AlertType.INFORMATION, "Saved", 
                "FTP configurations saved successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", 
                "Failed to save configuration: " + e.getMessage());
        }
    }

    private void refreshTableView() {
        ftpTableView.getItems().clear();
        ftpTableView.getItems().addAll(ftpConfigs);
    }

    private void loadFtpConfigs() {
        ftpConfigs.clear();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_FTP_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String host = prefs.get(PREF_KEY_HOST_PREFIX + i, null);
            if (varName != null || host != null) {
                ftpConfigs.add(new FtpConfigEntry(
                    varName != null ? varName : "",
                    host != null ? host : "",
                    prefs.get(PREF_KEY_PORT_PREFIX + i, "21"),
                    prefs.get(PREF_KEY_USER_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PASS_PREFIX + i, ""),
                    prefs.get(PREF_KEY_SECURE_PREFIX + i, "no")
                ));
            }
        }
    }

    private void saveFtpConfigs() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_FTP_CONFIGS; i++) {
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
            prefs.remove(PREF_KEY_HOST_PREFIX + i);
            prefs.remove(PREF_KEY_PORT_PREFIX + i);
            prefs.remove(PREF_KEY_USER_PREFIX + i);
            prefs.remove(PREF_KEY_PASS_PREFIX + i);
            prefs.remove(PREF_KEY_SECURE_PREFIX + i);
        }
        
        // Save current list (only entries with variable name and host)
        int index = 0;
        for (FtpConfigEntry entry : ftpConfigs) {
            String varName = entry.getVarName().trim();
            String host = entry.getHost().trim();
            
            if (!varName.isEmpty() && !host.isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + index, varName);
                prefs.put(PREF_KEY_HOST_PREFIX + index, host);
                prefs.put(PREF_KEY_PORT_PREFIX + index, entry.getPort().trim());
                prefs.put(PREF_KEY_USER_PREFIX + index, entry.getUser().trim());
                prefs.put(PREF_KEY_PASS_PREFIX + index, entry.getPassword());
                prefs.put(PREF_KEY_SECURE_PREFIX + index, entry.getSecure().trim());
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
     * Static method to retrieve the list of FTP configuration entries from preferences.
     */
    public static List<FtpConfigEntry> getFtpConfigEntries() {
        List<FtpConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_FTP_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String host = prefs.get(PREF_KEY_HOST_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && host != null && !host.isEmpty()) {
                entries.add(new FtpConfigEntry(
                    varName,
                    host,
                    prefs.get(PREF_KEY_PORT_PREFIX + i, "21"),
                    prefs.get(PREF_KEY_USER_PREFIX + i, ""),
                    prefs.get(PREF_KEY_PASS_PREFIX + i, ""),
                    prefs.get(PREF_KEY_SECURE_PREFIX + i, "no")
                ));
            }
        }
        
        return entries;
    }
}
