package com.eb.ui.ebs;

import com.eb.script.json.Json;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for importing application configuration from a JSON file.
 * Shows confirmation before applying imported settings.
 * 
 * @author Earl Bosch
 */
public class ImportConfigDialog {

    private static final int MAX_CONFIG_ENTRIES = 20;
    private static final String EXCLUDED_PASSWORD_PREFIX = "[EXCLUDED";
    
    private final Stage parentStage;

    public ImportConfigDialog(Stage parentStage) {
        this.parentStage = parentStage;
    }

    /**
     * Show the file chooser and import configuration.
     */
    public void showAndImport() {
        // Show file open dialog
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Configuration");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("Configuration Files", "*.cfg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fc.showOpenDialog(parentStage);
        if (file != null) {
            try {
                // Read and parse the JSON file
                String jsonContent = Files.readString(file.toPath());
                Object parsed = Json.parse(jsonContent);
                
                if (!(parsed instanceof Map)) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Format", 
                        "The selected file does not contain valid configuration data.");
                    return;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> importData = (Map<String, Object>) parsed;
                
                // Build summary of what will be imported
                List<String> sections = new ArrayList<>();
                if (importData.containsKey("colors")) sections.add("Colors");
                if (importData.containsKey("aiConfig")) sections.add("AI Config");
                if (importData.containsKey("safeDirectories")) sections.add("Safe Directories");
                if (importData.containsKey("databaseConfigs")) sections.add("Database Config");
                if (importData.containsKey("emailConfigs")) sections.add("Email Config");
                if (importData.containsKey("ftpConfigs")) sections.add("FTP Config");
                
                if (sections.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "No Configuration Found", 
                        "The selected file does not contain any recognized configuration sections.");
                    return;
                }
                
                // Show confirmation dialog
                String sectionsStr = String.join(", ", sections);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Import");
                confirm.setHeaderText("Are you sure you want to apply this configuration?");
                confirm.setContentText("The following sections will be imported:\n" + sectionsStr + 
                    "\n\nThis will replace your current settings for these sections.");
                confirm.initOwner(parentStage);
                confirm.initModality(Modality.WINDOW_MODAL);
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            applyImportedConfig(importData);
                            showAlert(Alert.AlertType.INFORMATION, "Import Successful", 
                                "Configuration imported successfully.\n\nSections imported: " + sectionsStr +
                                "\n\nNote: Some changes may require restarting the application to take effect.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Import Failed", 
                                "Failed to apply configuration:\n" + ex.getMessage());
                        }
                    }
                });
                
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Import Failed", 
                    "Failed to read configuration file:\n" + ex.getMessage());
            }
        }
    }

    /**
     * Apply the imported configuration data to preferences.
     */
    @SuppressWarnings("unchecked")
    private void applyImportedConfig(Map<String, Object> importData) throws Exception {
        // Import Colors (console.cfg)
        if (importData.containsKey("colors")) {
            Object colorsObj = importData.get("colors");
            if (colorsObj instanceof Map) {
                Map<String, Object> colorsData = (Map<String, Object>) colorsObj;
                saveColorsConfig(colorsData);
            }
        }
        
        // Import AI Config
        if (importData.containsKey("aiConfig")) {
            Object aiObj = importData.get("aiConfig");
            if (aiObj instanceof Map) {
                Map<String, Object> aiConfig = (Map<String, Object>) aiObj;
                Preferences prefs = Preferences.userRoot().node("com.eb.ai");
                
                if (aiConfig.containsKey("chatModel")) {
                    prefs.put("chat.model", aiConfig.get("chatModel").toString());
                }
                if (aiConfig.containsKey("chatUrl")) {
                    prefs.put("chat.url", aiConfig.get("chatUrl").toString());
                }
                if (aiConfig.containsKey("embedModel")) {
                    prefs.put("embed.model", aiConfig.get("embedModel").toString());
                }
                if (aiConfig.containsKey("embedUrl")) {
                    prefs.put("embed.url", aiConfig.get("embedUrl").toString());
                }
                if (aiConfig.containsKey("timeoutMs")) {
                    prefs.put("timeout.ms", aiConfig.get("timeoutMs").toString());
                }
            }
        }
        
        // Import Safe Directories
        if (importData.containsKey("safeDirectories")) {
            Object safeDirsObj = importData.get("safeDirectories");
            if (safeDirsObj instanceof List) {
                List<Object> safeDirs = (List<Object>) safeDirsObj;
                Preferences prefs = Preferences.userRoot().node("com.eb.sandbox");
                
                // Clear existing entries
                for (int i = 0; i < MAX_CONFIG_ENTRIES; i++) {
                    prefs.remove("safeDir." + i);
                    prefs.remove("safeDirName." + i);
                }
                
                // Save new entries
                int index = 0;
                for (Object dirObj : safeDirs) {
                    if (dirObj instanceof Map && index < MAX_CONFIG_ENTRIES) {
                        Map<String, Object> dirEntry = (Map<String, Object>) dirObj;
                        if (dirEntry.containsKey("directory")) {
                            prefs.put("safeDir." + index, dirEntry.get("directory").toString());
                            if (dirEntry.containsKey("name")) {
                                prefs.put("safeDirName." + index, dirEntry.get("name").toString());
                            }
                            index++;
                        }
                    }
                }
            }
        }
        
        // Import Database Config
        if (importData.containsKey("databaseConfigs")) {
            Object dbsObj = importData.get("databaseConfigs");
            if (dbsObj instanceof List) {
                List<Object> dbs = (List<Object>) dbsObj;
                Preferences prefs = Preferences.userRoot().node("com.eb.database");
                
                // Clear existing entries
                for (int i = 0; i < MAX_CONFIG_ENTRIES; i++) {
                    prefs.remove("dbVarName." + i);
                    prefs.remove("dbConnStr." + i);
                }
                
                // Save new entries
                int index = 0;
                for (Object dbObj : dbs) {
                    if (dbObj instanceof Map && index < MAX_CONFIG_ENTRIES) {
                        Map<String, Object> dbEntry = (Map<String, Object>) dbObj;
                        if (dbEntry.containsKey("variableName")) {
                            prefs.put("dbVarName." + index, dbEntry.get("variableName").toString());
                            if (dbEntry.containsKey("connectionString")) {
                                String connStr = dbEntry.get("connectionString").toString();
                                // Skip excluded connection strings (passwords were not exported)
                                if (!connStr.startsWith(EXCLUDED_PASSWORD_PREFIX)) {
                                    prefs.put("dbConnStr." + index, connStr);
                                }
                            }
                            index++;
                        }
                    }
                }
            }
        }
        
        // Import Email Config
        if (importData.containsKey("emailConfigs")) {
            Object emailsObj = importData.get("emailConfigs");
            if (emailsObj instanceof List) {
                List<Object> emails = (List<Object>) emailsObj;
                Preferences prefs = Preferences.userRoot().node("com.eb.mail");
                
                // Clear existing entries
                for (int i = 0; i < MAX_CONFIG_ENTRIES; i++) {
                    prefs.remove("mailVarName." + i);
                    prefs.remove("mailUrl." + i);
                    // Note: Email passwords are not imported for security reasons.
                    // Users must reconfigure passwords manually after import.
                }
                
                // Save new entries
                int index = 0;
                for (Object emailObj : emails) {
                    if (emailObj instanceof Map && index < MAX_CONFIG_ENTRIES) {
                        Map<String, Object> emailEntry = (Map<String, Object>) emailObj;
                        if (emailEntry.containsKey("variableName")) {
                            prefs.put("mailVarName." + index, emailEntry.get("variableName").toString());
                            if (emailEntry.containsKey("url")) {
                                prefs.put("mailUrl." + index, emailEntry.get("url").toString());
                            }
                            index++;
                        }
                    }
                }
            }
        }
        
        // Import FTP Config
        if (importData.containsKey("ftpConfigs")) {
            Object ftpsObj = importData.get("ftpConfigs");
            if (ftpsObj instanceof List) {
                List<Object> ftps = (List<Object>) ftpsObj;
                Preferences prefs = Preferences.userRoot().node("com.eb.ftp");
                
                // Clear existing entries
                for (int i = 0; i < MAX_CONFIG_ENTRIES; i++) {
                    prefs.remove("ftpVarName." + i);
                    prefs.remove("ftpUrl." + i);
                    // Note: FTP passwords are not imported for security reasons.
                    // Users must reconfigure passwords manually after import.
                }
                
                // Save new entries
                int index = 0;
                for (Object ftpObj : ftps) {
                    if (ftpObj instanceof Map && index < MAX_CONFIG_ENTRIES) {
                        Map<String, Object> ftpEntry = (Map<String, Object>) ftpObj;
                        if (ftpEntry.containsKey("variableName")) {
                            prefs.put("ftpVarName." + index, ftpEntry.get("variableName").toString());
                            if (ftpEntry.containsKey("url")) {
                                prefs.put("ftpUrl." + index, ftpEntry.get("url").toString());
                            }
                            index++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Save colors configuration to console.cfg file.
     * Handles both formats:
     * - Full config structure with "profiles" key (from getConfig())
     * - Simple color map (legacy format)
     */
    @SuppressWarnings("unchecked")
    private void saveColorsConfig(Map<String, Object> colorsData) throws Exception {
        Path configPath = Path.of("console.cfg");
        
        // Check if colorsData already has the full profile structure
        if (colorsData.containsKey("profiles")) {
            // This is the full config structure - write it directly
            String jsonContent = Json.prettyJson(colorsData);
            Files.writeString(configPath, jsonContent);
            return;
        }
        
        // Otherwise, it's a simple color map - wrap it in the proper structure
        // Try to load existing config to preserve other settings
        Map<String, Object> existingConfig = null;
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                Object parsed = Json.parse(content);
                if (parsed instanceof Map) {
                    existingConfig = (Map<String, Object>) parsed;
                }
            } catch (Exception e) {
                // If we can't read existing config, we'll create new one
            }
        }
        
        // If we have existing config with profiles structure, update it
        if (existingConfig != null && existingConfig.containsKey("profiles")) {
            Map<String, Object> profiles = (Map<String, Object>) existingConfig.get("profiles");
            String currentProfile = existingConfig.containsKey("currentProfile") 
                ? existingConfig.get("currentProfile").toString() 
                : "default";
            
            // Update the current profile with imported colors
            profiles.put(currentProfile, colorsData);
            
            // Write back the entire config
            String jsonContent = Json.prettyJson(existingConfig);
            Files.writeString(configPath, jsonContent);
        } else {
            // Create new config with default profile structure
            Map<String, Object> newConfig = new java.util.LinkedHashMap<>();
            newConfig.put("currentProfile", "default");
            List<String> profileList = new ArrayList<>();
            profileList.add("default");
            newConfig.put("profileList", profileList);
            
            Map<String, Object> profiles = new java.util.LinkedHashMap<>();
            profiles.put("default", colorsData);
            newConfig.put("profiles", profiles);
            
            String jsonContent = Json.prettyJson(newConfig);
            Files.writeString(configPath, jsonContent);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.initOwner(parentStage);
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }
}
