package com.eb.ui.ebs;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for configuring safe directories that can be accessed by the script interpreter.
 * Safe directories are additional paths outside the sandbox root that are allowed for
 * file operations and class tree scanning.
 * 
 * @author Earl Bosch
 */
public class SafeDirectoriesDialog extends Stage {

    private static final String PREF_NODE = "com.eb.sandbox";
    private static final String PREF_KEY_DIR_PREFIX = "safeDir.";
    private static final String PREF_KEY_NAME_PREFIX = "safeDirName.";
    private static final int MAX_SAFE_DIRS = 20;

    private final TableView<SafeDirectoryEntry> dirTableView;
    private final List<SafeDirectoryEntry> safeDirectories;
    
    /**
     * Data model for a safe directory entry with an optional name.
     */
    public static class SafeDirectoryEntry {
        private final StringProperty directory;
        private final StringProperty name;
        
        public SafeDirectoryEntry(String directory, String name) {
            this.directory = new SimpleStringProperty(directory);
            this.name = new SimpleStringProperty(name != null ? name : "");
        }
        
        public String getDirectory() {
            return directory.get();
        }
        
        public void setDirectory(String value) {
            directory.set(value);
        }
        
        public StringProperty directoryProperty() {
            return directory;
        }
        
        public String getName() {
            return name.get();
        }
        
        public void setName(String value) {
            name.set(value != null ? value : "");
        }
        
        public StringProperty nameProperty() {
            return name;
        }
    }

    public SafeDirectoriesDialog() {
        setTitle("Safe Directories Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        safeDirectories = new ArrayList<>();
        loadSafeDirectories();

        // --- Create TableView for directories with name column ---
        dirTableView = new TableView<>();
        dirTableView.setPrefHeight(300);
        dirTableView.setPrefWidth(700);
        dirTableView.setEditable(true);
        dirTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Directory column (not editable)
        TableColumn<SafeDirectoryEntry, String> dirColumn = new TableColumn<>("Directory Path");
        dirColumn.setCellValueFactory(new PropertyValueFactory<>("directory"));
        dirColumn.setMinWidth(400);
        dirColumn.setEditable(false);
        
        // Name column (editable)
        TableColumn<SafeDirectoryEntry, String> nameColumn = new TableColumn<>("Name (Optional)");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setMinWidth(200);
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> {
            SafeDirectoryEntry entry = event.getRowValue();
            entry.setName(event.getNewValue());
        });
        
        dirTableView.getColumns().add(dirColumn);
        dirTableView.getColumns().add(nameColumn);
        
        refreshTableView();

        // --- Buttons ---
        Button btnAdd = new Button("Add Directory…");
        Button btnRemove = new Button("Remove");
        Button btnCopy = new Button("Copy to Clipboard");
        Button btnBrowse = new Button("Browse");
        Button btnSave = new Button("Save");
        Button btnClose = new Button("Close");
        
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);
        btnRemove.setDisable(true);
        btnCopy.setDisable(true);
        btnBrowse.setDisable(true);

        // Enable/disable buttons based on selection
        dirTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnRemove.setDisable(!hasSelection);
            btnCopy.setDisable(!hasSelection);
            btnBrowse.setDisable(!hasSelection);
        });

        // --- Actions ---
        btnAdd.setOnAction(e -> onAddDirectory());
        btnRemove.setOnAction(e -> onRemoveDirectory());
        btnCopy.setOnAction(e -> onCopyToClipboard());
        btnBrowse.setOnAction(e -> onBrowse());
        btnSave.setOnAction(e -> onSave());
        btnClose.setOnAction(e -> close());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Safe directories are additional paths that can be accessed by scripts.\n" +
            "By default, only the current working directory is accessible.\n" +
            "You can optionally provide a variable name for each directory."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnAdd, btnRemove, btnCopy, btnBrowse);

        HBox bottomButtons = new HBox(10);
        bottomButtons.getChildren().addAll(btnSave, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            new Label("Safe Directories:"),
            dirTableView,
            buttonBox,
            bottomButtons
        );

        VBox.setVgrow(dirTableView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(750);
        setMinHeight(450);
    }

    private void onAddDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Safe Directory");
        
        // Try to start at user home
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path homePath = Paths.get(userHome);
            if (Files.exists(homePath)) {
                chooser.setInitialDirectory(homePath.toFile());
            }
        }

        java.io.File selectedDir = chooser.showDialog(this);
        if (selectedDir != null) {
            String dirPath = selectedDir.getAbsolutePath();
            
            // Check if already in list
            boolean exists = safeDirectories.stream()
                .anyMatch(entry -> entry.getDirectory().equals(dirPath));
            if (exists) {
                showAlert(Alert.AlertType.WARNING, "Duplicate", 
                    "This directory is already in the safe directories list.");
                return;
            }

            // Check if we've reached the limit
            if (safeDirectories.size() >= MAX_SAFE_DIRS) {
                showAlert(Alert.AlertType.WARNING, "Limit Reached", 
                    "Maximum of " + MAX_SAFE_DIRS + " safe directories allowed.");
                return;
            }

            safeDirectories.add(new SafeDirectoryEntry(dirPath, ""));
            refreshTableView();
        }
    }

    private void onRemoveDirectory() {
        SafeDirectoryEntry selected = dirTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            safeDirectories.remove(selected);
            refreshTableView();
        }
    }

    private void onCopyToClipboard() {
        SafeDirectoryEntry selected = dirTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.getDirectory());
            Clipboard.getSystemClipboard().setContent(content);
//            showAlert(Alert.AlertType.INFORMATION, "Copied", 
//                "Directory path copied to clipboard:\n" + selected.getDirectory());
        }
    }

    private void onBrowse() {
        SafeDirectoryEntry selected = dirTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Path path = Paths.get(selected.getDirectory());
            
            // Check if directory exists
            if (!Files.exists(path)) {
                showAlert(Alert.AlertType.WARNING, "Directory Not Found", 
                    "The directory does not exist:\n" + selected.getDirectory());
                return;
            }
            
            if (!Files.isDirectory(path)) {
                showAlert(Alert.AlertType.WARNING, "Not a Directory", 
                    "The path is not a directory:\n" + selected.getDirectory());
                return;
            }
            
            // Try to open the directory in the system file browser
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.open(path.toFile());
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Browse Failed", 
                        "Failed to open directory in file browser:\n" + e.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Not Supported", 
                    "Desktop browsing is not supported on this system.");
            }
        }
    }

    private void onSave() {
        try {
            saveSafeDirectories();
            showAlert(Alert.AlertType.INFORMATION, "Saved", 
                "Safe directories configuration saved successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", 
                "Failed to save configuration: " + e.getMessage());
        }
    }

    private void refreshTableView() {
        dirTableView.getItems().clear();
        dirTableView.getItems().addAll(safeDirectories);
    }

    private void loadSafeDirectories() {
        safeDirectories.clear();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            String dir = prefs.get(PREF_KEY_DIR_PREFIX + i, null);
            if (dir != null && !dir.isEmpty()) {
                String name = prefs.get(PREF_KEY_NAME_PREFIX + i, "");
                safeDirectories.add(new SafeDirectoryEntry(dir, name));
            }
        }
    }

    private void saveSafeDirectories() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            prefs.remove(PREF_KEY_DIR_PREFIX + i);
            prefs.remove(PREF_KEY_NAME_PREFIX + i);
        }
        
        // Save current list
        for (int i = 0; i < safeDirectories.size(); i++) {
            SafeDirectoryEntry entry = safeDirectories.get(i);
            prefs.put(PREF_KEY_DIR_PREFIX + i, entry.getDirectory());
            String name = entry.getName();
            if (name != null && !name.trim().isEmpty()) {
                prefs.put(PREF_KEY_NAME_PREFIX + i, name);
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
     * Static method to retrieve the list of safe directories from preferences.
     * Used by Util.resolveSandboxedPath() to check if a path is in a safe directory.
     */
    public static List<String> getSafeDirectories() {
        List<String> dirs = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            String dir = prefs.get(PREF_KEY_DIR_PREFIX + i, null);
            if (dir != null && !dir.isEmpty()) {
                dirs.add(dir);
            }
        }
        
        return dirs;
    }
    
    /**
     * Static method to retrieve the list of safe directory entries (with names) from preferences.
     * Used by the interpreter to define global variables for named safe directories.
     */
    public static List<SafeDirectoryEntry> getSafeDirectoryEntries() {
        List<SafeDirectoryEntry> entries = new ArrayList<>();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            String dir = prefs.get(PREF_KEY_DIR_PREFIX + i, null);
            if (dir != null && !dir.isEmpty()) {
                String name = prefs.get(PREF_KEY_NAME_PREFIX + i, "");
                entries.add(new SafeDirectoryEntry(dir, name));
            }
        }
        
        return entries;
    }
}
