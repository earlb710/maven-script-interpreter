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
 * Dialog for configuring FTP server connections that can be accessed by the script interpreter.
 * Allows users to define variable names and associate them with FTP server connection URLs.
 * 
 * URL Formats:
 * - ftp://user:password@host:port   (standard FTP)
 * - ftps://user:password@host:port  (secure FTPS)
 * 
 * @author Earl Bosch
 */
public class FtpConfigDialog extends Stage {

    private static final String PREF_NODE = "com.eb.ftp";
    private static final String PREF_KEY_NAME_PREFIX = "ftpVarName.";
    private static final String PREF_KEY_URL_PREFIX = "ftpUrl.";
    private static final int MAX_FTP_CONFIGS = 20;
    
    /** FTP/FTPS URL pattern: (ftp|ftps)://user:password@host:port */
    private static final Pattern FTP_URL_PATTERN = Pattern.compile(
        "(ftps?)://([^:]+):([^@]*)@([^:]+):(\\d+)"
    );

    private final TableView<FtpConfigEntry> ftpTableView;
    private final List<FtpConfigEntry> ftpConfigs;
    
    /**
     * Data model for an FTP configuration entry (simplified to varName + URL).
     */
    public static class FtpConfigEntry {
        private final StringProperty varName;
        private final StringProperty url;
        
        public FtpConfigEntry(String varName, String url) {
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
         * Creates an FTP URL from individual components.
         */
        public static String buildUrl(String host, String port, String user, String password, boolean secure) {
            try {
                String scheme = secure ? "ftps" : "ftp";
                String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.name());
                String encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8.name());
                return String.format("%s://%s:%s@%s:%s", 
                    scheme, encodedUser, encodedPass, host, port);
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
        
        /**
         * Parses an FTP URL into components.
         * @return Map with keys: host, port, user, password, secure (true/false)
         */
        public static Map<String, String> parseUrl(String url) {
            Map<String, String> result = new HashMap<>();
            if (url == null || url.isEmpty()) return result;
            
            Matcher m = FTP_URL_PATTERN.matcher(url);
            if (m.matches()) {
                try {
                    result.put("secure", m.group(1).equals("ftps") ? "true" : "false");
                    result.put("user", URLDecoder.decode(m.group(2), StandardCharsets.UTF_8.name()));
                    result.put("password", URLDecoder.decode(m.group(3), StandardCharsets.UTF_8.name()));
                    result.put("host", m.group(4));
                    result.put("port", m.group(5));
                } catch (UnsupportedEncodingException e) {
                    // Return empty map on error
                }
            }
            return result;
        }
        
        public boolean isSecure() {
            return url.get().startsWith("ftps://");
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
        nameColumn.setMinWidth(100);
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> event.getRowValue().setVarName(event.getNewValue()));
        
        // URL column
        TableColumn<FtpConfigEntry, String> urlColumn = new TableColumn<>("FTP URL (ftp://user:password@host:port or ftps://...)");
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        urlColumn.setMinWidth(550);
        urlColumn.setEditable(true);
        urlColumn.setOnEditCommit(event -> event.getRowValue().setUrl(event.getNewValue()));
        
        ftpTableView.getColumns().add(nameColumn);
        ftpTableView.getColumns().add(urlColumn);
        
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
            "Configure FTP server connections using URL format.\n\n" +
            "URL Formats:\n" +
            "• Standard FTP: ftp://user:password@host:port\n" +
            "• Secure FTPS:  ftps://user:password@host:port\n\n" +
            "Examples:\n" +
            "• ftp://myuser:mypassword@ftp.example.com:21\n" +
            "• ftps://myuser:mypassword@ftps.example.com:990\n\n" +
            "Note: URL-encode special characters in passwords (use %40 for @, %3A for :, etc.)"
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

        ftpConfigs.add(new FtpConfigEntry("", "ftp://user:password@host:21"));
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
                String url = entry.getUrl().trim();
                
                if (!varName.isEmpty() && url.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' has no URL");
                } else if (varName.isEmpty() && !url.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": URL has no variable name");
                } else if (!varName.isEmpty() && !varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' is invalid");
                } else if (!url.isEmpty() && !url.startsWith("ftp://") && !url.startsWith("ftps://")) {
                    errors.add("Row " + (i + 1) + ": URL must start with 'ftp://' or 'ftps://'");
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
            String url = prefs.get(PREF_KEY_URL_PREFIX + i, null);
            if (varName != null || url != null) {
                ftpConfigs.add(new FtpConfigEntry(
                    varName != null ? varName : "",
                    url != null ? url : ""
                ));
            }
        }
    }

    private void saveFtpConfigs() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_FTP_CONFIGS; i++) {
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
            prefs.remove(PREF_KEY_URL_PREFIX + i);
        }
        
        // Save current list (only entries with variable name and URL)
        int index = 0;
        for (FtpConfigEntry entry : ftpConfigs) {
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
     * Static method to retrieve the list of FTP configuration entries from preferences.
     * Returns entries as (varName, URL string) pairs.
     */
    public static List<FtpConfigEntry> getFtpConfigEntries() {
        List<FtpConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_FTP_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String url = prefs.get(PREF_KEY_URL_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && url != null && !url.isEmpty()) {
                entries.add(new FtpConfigEntry(varName, url));
            }
        }
        
        return entries;
    }
    
    /**
     * Parses an FTP URL into its components.
     * @param url FTP URL in format ftp://user:password@host:port or ftps://user:password@host:port
     * @return Map with keys: host, port, user, password, secure (or empty map if invalid)
     */
    public static Map<String, String> parseFtpUrl(String url) {
        return FtpConfigEntry.parseUrl(url);
    }
    
    /**
     * Builds an FTP URL from components.
     * @return FTP URL string
     */
    public static String buildFtpUrl(String host, String port, String user, String password, boolean secure) {
        return FtpConfigEntry.buildUrl(host, port, user, password, secure);
    }
}
