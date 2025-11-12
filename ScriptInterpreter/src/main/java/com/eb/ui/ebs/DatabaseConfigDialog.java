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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for configuring database connections that can be accessed by the script interpreter.
 * Allows users to define variable names and associate them with database connection strings.
 * 
 * @author Earl Bosch
 */
public class DatabaseConfigDialog extends Stage {

    private static final String PREF_NODE = "com.eb.database";
    private static final String PREF_KEY_NAME_PREFIX = "dbVarName.";
    private static final String PREF_KEY_CONN_PREFIX = "dbConnStr.";
    private static final int MAX_DB_CONFIGS = 20;

    private final TableView<DatabaseConfigEntry> dbTableView;
    private final List<DatabaseConfigEntry> databaseConfigs;
    
    /**
     * Data model for a database configuration entry with variable name and connection string.
     */
    public static class DatabaseConfigEntry {
        private final StringProperty varName;
        private final StringProperty connectionString;
        
        public DatabaseConfigEntry(String varName, String connectionString) {
            this.varName = new SimpleStringProperty(varName != null ? varName : "");
            this.connectionString = new SimpleStringProperty(connectionString != null ? connectionString : "");
        }
        
        public String getVarName() {
            return varName.get();
        }
        
        public void setVarName(String value) {
            varName.set(value != null ? value : "");
        }
        
        public StringProperty varNameProperty() {
            return varName;
        }
        
        public String getConnectionString() {
            return connectionString.get();
        }
        
        public void setConnectionString(String value) {
            connectionString.set(value != null ? value : "");
        }
        
        public StringProperty connectionStringProperty() {
            return connectionString;
        }
    }

    public DatabaseConfigDialog() {
        setTitle("Database Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        databaseConfigs = new ArrayList<>();
        loadDatabaseConfigs();

        // --- Create TableView for database configurations ---
        dbTableView = new TableView<>();
        dbTableView.setPrefHeight(300);
        dbTableView.setPrefWidth(800);
        dbTableView.setEditable(true);
        dbTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Variable Name column (editable)
        TableColumn<DatabaseConfigEntry, String> nameColumn = new TableColumn<>("Variable Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("varName"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setMinWidth(150);
        nameColumn.setEditable(true);
        
        // Connection String column (editable)
        TableColumn<DatabaseConfigEntry, String> connColumn = new TableColumn<>("Database Connection String");
        connColumn.setCellValueFactory(new PropertyValueFactory<>("connectionString"));
        connColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        connColumn.setMinWidth(500);
        connColumn.setEditable(true);
        
        // Set up edit commit handlers after both columns are declared
        nameColumn.setOnEditCommit(event -> {
            DatabaseConfigEntry entry = event.getRowValue();
            entry.setVarName(event.getNewValue());
            
            // Move to connection string column after editing variable name
            int row = event.getTablePosition().getRow();
            dbTableView.edit(row, connColumn);
        });
        
        connColumn.setOnEditCommit(event -> {
            DatabaseConfigEntry entry = event.getRowValue();
            entry.setConnectionString(event.getNewValue());
        });
        
        dbTableView.getColumns().add(nameColumn);
        dbTableView.getColumns().add(connColumn);
        
        refreshTableView();

        // --- Buttons ---
        Button btnAdd = new Button("Add Configuration");
        Button btnRemove = new Button("Remove");
        Button btnCopy = new Button("Copy Connection String");
        Button btnSave = new Button("Save");
        Button btnClose = new Button("Close");
        
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);
        btnRemove.setDisable(true);
        btnCopy.setDisable(true);

        // Enable/disable buttons based on selection
        dbTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnRemove.setDisable(!hasSelection);
            btnCopy.setDisable(!hasSelection);
        });

        // --- Actions ---
        btnAdd.setOnAction(e -> onAddConfig());
        btnRemove.setOnAction(e -> onRemoveConfig());
        btnCopy.setOnAction(e -> onCopyToClipboard());
        btnSave.setOnAction(e -> onSave());
        btnClose.setOnAction(e -> close());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Configure database connections with variable names.\n" +
            "These variables will be available as global string variables in scripts.\n" +
            "Example: If you name a connection 'myDB', you can use it like: connect db = myDB;\n\n" +
            "Supported Database Connection Formats:\n" +
            "• Oracle:     jdbc:oracle:thin:@//host:port/service  or  jdbc:oracle:thin:@host:port:sid\n" +
            "• MySQL:      jdbc:mysql://host:port/database\n" +
            "• PostgreSQL: jdbc:postgresql://host:port/database\n\n" +
            "All three database adapters are fully supported with JDBC drivers included."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnAdd, btnRemove, btnCopy);

        HBox bottomButtons = new HBox(10);
        bottomButtons.getChildren().addAll(btnSave, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            new Label("Database Configurations:"),
            dbTableView,
            buttonBox,
            bottomButtons
        );

        VBox.setVgrow(dbTableView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(850);
        setMinHeight(550);
    }

    private void onAddConfig() {
        // Check if we've reached the limit
        if (databaseConfigs.size() >= MAX_DB_CONFIGS) {
            showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                "Maximum of " + MAX_DB_CONFIGS + " database configurations allowed.");
            return;
        }

        databaseConfigs.add(new DatabaseConfigEntry("", ""));
        refreshTableView();
        
        // Select the newly added row and start editing
        dbTableView.getSelectionModel().selectLast();
        dbTableView.edit(databaseConfigs.size() - 1, dbTableView.getColumns().get(0));
    }

    private void onRemoveConfig() {
        DatabaseConfigEntry selected = dbTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            databaseConfigs.remove(selected);
            refreshTableView();
        }
    }

    private void onCopyToClipboard() {
        DatabaseConfigEntry selected = dbTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.getConnectionString());
            Clipboard.getSystemClipboard().setContent(content);
            
//            showAlert(Alert.AlertType.INFORMATION, "Copied", 
//                "Connection string copied to clipboard:\n" + selected.getConnectionString());
        }
    }

    private void onSave() {
        try {
            // Validate entries before saving
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < databaseConfigs.size(); i++) {
                DatabaseConfigEntry entry = databaseConfigs.get(i);
                String varName = entry.getVarName().trim();
                String connStr = entry.getConnectionString().trim();
                
                if (!varName.isEmpty() && connStr.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' has no connection string");
                } else if (varName.isEmpty() && !connStr.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Connection string has no variable name");
                } else if (!varName.isEmpty() && !varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    errors.add("Row " + (i + 1) + ": Variable name '" + varName + "' is invalid (must start with letter or underscore)");
                }
            }
            
            if (!errors.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", 
                    "Please fix the following errors:\n\n" + String.join("\n", errors));
                return;
            }
            
            // Check for duplicate variable names
            List<String> varNames = new ArrayList<>();
            for (DatabaseConfigEntry entry : databaseConfigs) {
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
            
            saveDatabaseConfigs();
            showAlert(Alert.AlertType.INFORMATION, "Saved", 
                "Database configurations saved successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", 
                "Failed to save configuration: " + e.getMessage());
        }
    }

    private void refreshTableView() {
        dbTableView.getItems().clear();
        dbTableView.getItems().addAll(databaseConfigs);
    }

    private void loadDatabaseConfigs() {
        databaseConfigs.clear();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_DB_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String connStr = prefs.get(PREF_KEY_CONN_PREFIX + i, null);
            if (varName != null || connStr != null) {
                databaseConfigs.add(new DatabaseConfigEntry(
                    varName != null ? varName : "",
                    connStr != null ? connStr : ""
                ));
            }
        }
    }

    private void saveDatabaseConfigs() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_DB_CONFIGS; i++) {
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
            prefs.remove(PREF_KEY_CONN_PREFIX + i);
        }
        
        // Save current list (only non-empty entries)
        int index = 0;
        for (DatabaseConfigEntry entry : databaseConfigs) {
            String varName = entry.getVarName().trim();
            String connStr = entry.getConnectionString().trim();
            
            // Only save if both fields have values
            if (!varName.isEmpty() && !connStr.isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + index, varName);
                prefs.put(PREF_KEY_CONN_PREFIX + index, connStr);
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
     * Static method to retrieve the list of database configuration entries from preferences.
     * Used by the interpreter to define global variables for database connections.
     */
    public static List<DatabaseConfigEntry> getDatabaseConfigEntries() {
        List<DatabaseConfigEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_DB_CONFIGS; i++) {
            String varName = prefs.get(PREF_KEY_NAME_PREFIX + i, null);
            String connStr = prefs.get(PREF_KEY_CONN_PREFIX + i, null);
            if (varName != null && !varName.isEmpty() && connStr != null && !connStr.isEmpty()) {
                entries.add(new DatabaseConfigEntry(varName, connStr));
            }
        }
        
        return entries;
    }
}
