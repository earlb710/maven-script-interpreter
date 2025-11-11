package com.eb.ui.ebs;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    private static final String PREF_KEY_PREFIX = "safeDir.";
    private static final int MAX_SAFE_DIRS = 20;

    private final ListView<String> dirListView;
    private final List<String> safeDirectories;

    public SafeDirectoriesDialog() {
        setTitle("Safe Directories Configuration");
        setAlwaysOnTop(true);
        initModality(Modality.WINDOW_MODAL);

        safeDirectories = new ArrayList<>();
        loadSafeDirectories();

        // --- Create ListView for directories ---
        dirListView = new ListView<>();
        dirListView.setPrefHeight(300);
        dirListView.setPrefWidth(500);
        refreshListView();

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
        dirListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
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
            "By default, only the current working directory is accessible."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnAdd, btnRemove, btnCopy, btnBrowse);

        HBox bottomButtons = new HBox(10);
        bottomButtons.getChildren().addAll(btnSave, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            new Label("Safe Directories:"),
            dirListView,
            buttonBox,
            bottomButtons
        );

        VBox.setVgrow(dirListView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(550);
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
            if (safeDirectories.contains(dirPath)) {
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

            safeDirectories.add(dirPath);
            refreshListView();
        }
    }

    private void onRemoveDirectory() {
        String selected = dirListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            safeDirectories.remove(selected);
            refreshListView();
        }
    }

    private void onCopyToClipboard() {
        String selected = dirListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selected);
            Clipboard.getSystemClipboard().setContent(content);
            
            showAlert(Alert.AlertType.INFORMATION, "Copied", 
                "Directory path copied to clipboard:\n" + selected);
        }
    }

    private void onBrowse() {
        String selected = dirListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Path path = Paths.get(selected);
            
            // Check if directory exists
            if (!Files.exists(path)) {
                showAlert(Alert.AlertType.WARNING, "Directory Not Found", 
                    "The directory does not exist:\n" + selected);
                return;
            }
            
            if (!Files.isDirectory(path)) {
                showAlert(Alert.AlertType.WARNING, "Not a Directory", 
                    "The path is not a directory:\n" + selected);
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

    private void refreshListView() {
        dirListView.getItems().clear();
        dirListView.getItems().addAll(safeDirectories);
    }

    private void loadSafeDirectories() {
        safeDirectories.clear();
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            String dir = prefs.get(PREF_KEY_PREFIX + i, null);
            if (dir != null && !dir.isEmpty()) {
                safeDirectories.add(dir);
            }
        }
    }

    private void saveSafeDirectories() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        
        // Clear all existing entries
        for (int i = 0; i < MAX_SAFE_DIRS; i++) {
            prefs.remove(PREF_KEY_PREFIX + i);
        }
        
        // Save current list
        for (int i = 0; i < safeDirectories.size(); i++) {
            prefs.put(PREF_KEY_PREFIX + i, safeDirectories.get(i));
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
            String dir = prefs.get(PREF_KEY_PREFIX + i, null);
            if (dir != null && !dir.isEmpty()) {
                dirs.add(dir);
            }
        }
        
        return dirs;
    }
}
