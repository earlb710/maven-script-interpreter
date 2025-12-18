package com.eb.ui.ebs;

import com.eb.ui.util.ButtonShortcutHelper;
import com.eb.script.json.Json;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for exporting application configuration.
 * Allows users to select which configuration sections to export.
 * Passwords are excluded from export for security.
 * 
 * @author Earl Bosch
 */
public class ExportConfigDialog extends Stage {

    private final CheckBox chkColors;
    private final CheckBox chkAiConfig;
    private final CheckBox chkSafeDirectories;
    private final CheckBox chkDatabaseConfig;
    private final CheckBox chkEmailConfig;
    private final CheckBox chkFtpConfig;
    private final Stage parentStage;

    public ExportConfigDialog(Stage parentStage) {
        this.parentStage = parentStage;
        setTitle("Export Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        // --- Create checkboxes for each config section ---
        chkColors = new CheckBox("Colors (console.cfg)");
        chkColors.setSelected(true);
        
        chkAiConfig = new CheckBox("AI Chat Model Setup (excludes API key)");
        chkAiConfig.setSelected(true);
        
        chkSafeDirectories = new CheckBox("Safe Directories");
        chkSafeDirectories.setSelected(true);
        
        chkDatabaseConfig = new CheckBox("Database Config (excludes connection strings with passwords)");
        chkDatabaseConfig.setSelected(true);
        
        chkEmailConfig = new CheckBox("Email Config (excludes passwords)");
        chkEmailConfig.setSelected(true);
        
        chkFtpConfig = new CheckBox("FTP Config (excludes passwords)");
        chkFtpConfig.setSelected(true);

        // --- Buttons ---
        Button btnExport = new Button("Export…");
        Button btnCancel = new Button("Cancel");
        
        // Add keyboard shortcuts to buttons
        ButtonShortcutHelper.addAltShortcut(btnExport, KeyCode.E);
        ButtonShortcutHelper.addAltShortcut(btnCancel, KeyCode.C);
        
        btnExport.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        btnExport.setOnAction(e -> onExport());
        btnCancel.setOnAction(e -> close());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Select the configuration sections you want to export.\n" +
            "Note: Passwords and sensitive information are excluded for security."
        );
        infoLabel.setWrapText(true);

        VBox checkboxBox = new VBox(8);
        checkboxBox.setPadding(new Insets(10, 0, 10, 20));
        checkboxBox.getChildren().addAll(chkColors, chkAiConfig, chkSafeDirectories, chkDatabaseConfig, chkEmailConfig, chkFtpConfig);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnExport, btnCancel);

        layout.getChildren().addAll(
            infoLabel,
            new Label("Configuration sections:"),
            checkboxBox,
            buttonBox
        );

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(450);
        setMinHeight(300);
    }

    private void onExport() {
        // Check if at least one option is selected
        if (!chkColors.isSelected() && !chkAiConfig.isSelected() && 
            !chkSafeDirectories.isSelected() && !chkDatabaseConfig.isSelected() &&
            !chkEmailConfig.isSelected() && !chkFtpConfig.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "No Selection", 
                "Please select at least one configuration section to export.");
            return;
        }

        // Show file save dialog
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Configuration");
        fc.setInitialFileName("config-export.json");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("Configuration Files", "*.cfg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Hide the options dialog while file browser is open
        hide();
        File file = fc.showSaveDialog(parentStage);
        if (file != null) {
            try {
                Map<String, Object> exportData = buildExportData();
                String jsonContent = Json.prettyJson(exportData);
                
                Path path = file.toPath();
                Files.writeString(path, jsonContent);
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                    "Configuration exported to:\n" + path.toString());
                close();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", 
                    "Failed to export configuration:\n" + ex.getMessage());
                // Show dialog again so user can retry
                show();
            }
        } else {
            // User cancelled the file chooser, show dialog again
            show();
        }
    }

    private Map<String, Object> buildExportData() {
        Map<String, Object> exportData = new LinkedHashMap<>();
        
        // Export Colors
        if (chkColors.isSelected()) {
            ConsoleConfig config = new ConsoleConfig();
            exportData.put("colors", config.getConfig());
        }
        
        // Export AI Config (excluding API key)
        if (chkAiConfig.isSelected()) {
            Map<String, Object> aiConfig = new LinkedHashMap<>();
            Preferences prefs = Preferences.userRoot().node("com.eb.ai");
            
            // Get values but exclude the API key
            String chatModel = prefs.get("chat.model", "");
            String chatUrl = prefs.get("chat.url", "");
            String embModel = prefs.get("embed.model", "");
            String embUrl = prefs.get("embed.url", "");
            String timeoutMs = prefs.get("timeout.ms", "");
            
            if (!chatModel.isEmpty()) aiConfig.put("chatModel", chatModel);
            if (!chatUrl.isEmpty()) aiConfig.put("chatUrl", chatUrl);
            if (!embModel.isEmpty()) aiConfig.put("embedModel", embModel);
            if (!embUrl.isEmpty()) aiConfig.put("embedUrl", embUrl);
            if (!timeoutMs.isEmpty()) {
                // Try to export as integer for consistency
                try {
                    aiConfig.put("timeoutMs", Integer.parseInt(timeoutMs));
                } catch (NumberFormatException e) {
                    aiConfig.put("timeoutMs", timeoutMs);
                }
            }
            
            exportData.put("aiConfig", aiConfig);
        }
        
        // Export Safe Directories
        if (chkSafeDirectories.isSelected()) {
            List<SafeDirectoriesDialog.SafeDirectoryEntry> entries = 
                SafeDirectoriesDialog.getSafeDirectoryEntries();
            
            List<Map<String, Object>> safeDirectories = new ArrayList<>();
            for (SafeDirectoriesDialog.SafeDirectoryEntry entry : entries) {
                Map<String, Object> dirEntry = new LinkedHashMap<>();
                dirEntry.put("directory", entry.getDirectory());
                if (entry.getName() != null && !entry.getName().isEmpty()) {
                    dirEntry.put("name", entry.getName());
                }
                safeDirectories.add(dirEntry);
            }
            exportData.put("safeDirectories", safeDirectories);
        }
        
        // Export Database Config (excluding connection strings that contain passwords)
        if (chkDatabaseConfig.isSelected()) {
            List<DatabaseConfigDialog.DatabaseConfigEntry> entries = 
                DatabaseConfigDialog.getDatabaseConfigEntries();
            
            List<Map<String, Object>> databaseConfigs = new ArrayList<>();
            for (DatabaseConfigDialog.DatabaseConfigEntry entry : entries) {
                Map<String, Object> dbEntry = new LinkedHashMap<>();
                dbEntry.put("variableName", entry.getVarName());
                
                // Exclude connection string if it appears to contain a password
                String connStr = entry.getConnectionString();
                if (!containsPassword(connStr)) {
                    dbEntry.put("connectionString", connStr);
                } else {
                    dbEntry.put("connectionString", "[EXCLUDED - contains password]");
                }
                databaseConfigs.add(dbEntry);
            }
            exportData.put("databaseConfigs", databaseConfigs);
        }
        
        // Export Email Config (excluding passwords)
        if (chkEmailConfig.isSelected()) {
            List<MailConfigDialog.MailConfigEntry> entries = 
                MailConfigDialog.getMailConfigEntries();
            
            List<Map<String, Object>> emailConfigs = new ArrayList<>();
            for (MailConfigDialog.MailConfigEntry entry : entries) {
                Map<String, Object> emailEntry = new LinkedHashMap<>();
                emailEntry.put("variableName", entry.getVarName());
                emailEntry.put("url", entry.getUrl());
                // Password is explicitly excluded for security
                emailConfigs.add(emailEntry);
            }
            exportData.put("emailConfigs", emailConfigs);
        }
        
        // Export FTP Config (excluding passwords)
        if (chkFtpConfig.isSelected()) {
            List<FtpConfigDialog.FtpConfigEntry> entries = 
                FtpConfigDialog.getFtpConfigEntries();
            
            List<Map<String, Object>> ftpConfigs = new ArrayList<>();
            for (FtpConfigDialog.FtpConfigEntry entry : entries) {
                Map<String, Object> ftpEntry = new LinkedHashMap<>();
                ftpEntry.put("variableName", entry.getVarName());
                ftpEntry.put("url", entry.getUrl());
                // Password is explicitly excluded for security
                ftpConfigs.add(ftpEntry);
            }
            exportData.put("ftpConfigs", ftpConfigs);
        }
        
        return exportData;
    }

    /**
     * Check if a connection string appears to contain a password.
     * Looks for common password patterns in JDBC URLs.
     */
    private boolean containsPassword(String connStr) {
        if (connStr == null || connStr.isEmpty()) {
            return false;
        }
        String lower = connStr.toLowerCase();
        // Check for common password patterns in JDBC connection strings
        // Pattern: password= or pwd= (common in most JDBC URLs)
        if (lower.contains("password=") || lower.contains("pwd=")) {
            return true;
        }
        // Pattern: user:password@ (credentials in URL format)
        if (lower.matches(".*://[^/]+:[^@]+@.*")) {
            return true;
        }
        return false;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.initOwner(this);
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }
}
