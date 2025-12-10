package com.eb.ui.ebs;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javafx.application.Platform.runLater;

/**
 * Dialog for editing project properties.
 * Allows user to modify project name, main script, CSS file, and directory paths.
 * 
 * @author Earl Bosch
 */
public class ProjectPropertiesDialog extends Dialog<ProjectPropertiesDialog.ProjectProperties> {
    
    private static final double MIN_TEXT_FIELD_WIDTH = 300.0;
    private static final String READ_ONLY_FIELD_STYLE = "-fx-background-color: #f0f0f0; -fx-opacity: 1.0;";
    
    private final TextField projectNameField;
    private final TextField projectDirField;
    private final TextField mainScriptField;
    private final TextField cssFileField;
    private final TextField resourceDirField;
    private final TextField testDirField;
    private final TextField tempDirField;
    private final TextField docDirField;
    private final Button createMainScriptButton;
    private final Button createCssButton;
    private final Button createResourceDirButton;
    private final Button createTestDirButton;
    private final Button createTempDirButton;
    private final Button createDocDirButton;
    private final Path projectDir;
    private final TableView<String> linkedDirTable;
    private final List<String> linkedDirectories;
    
    private boolean fieldsEdited = false;
    
    /**
     * Result data class containing project properties.
     */
    public static class ProjectProperties {
        private final String name;
        private final String mainScript;
        private final String cssFile;
        private final String resourceDir;
        private final String testDir;
        private final String tempDir;
        private final String docDir;
        private final List<String> linkedDirectories;
        
        public ProjectProperties(String name, String mainScript, String cssFile, 
                               String resourceDir, String testDir, String tempDir, String docDir,
                               List<String> linkedDirectories) {
            this.name = name;
            this.mainScript = mainScript;
            this.cssFile = cssFile;
            this.resourceDir = resourceDir;
            this.testDir = testDir;
            this.tempDir = tempDir;
            this.docDir = docDir;
            this.linkedDirectories = linkedDirectories;
        }
        
        public String getName() {
            return name;
        }
        
        public String getMainScript() {
            return mainScript;
        }
        
        public String getCssFile() {
            return cssFile;
        }
        
        public String getResourceDir() {
            return resourceDir;
        }
        
        public String getTestDir() {
            return testDir;
        }
        
        public String getTempDir() {
            return tempDir;
        }
        
        public String getDocDir() {
            return docDir;
        }
        
        public List<String> getLinkedDirectories() {
            return linkedDirectories;
        }
    }
    
    /**
     * Create a new project properties dialog.
     * 
     * @param owner The owner window
     * @param projectJsonPath Path to the project.json file
     * @param currentProperties Current project properties from project.json
     */
    public ProjectPropertiesDialog(Window owner, Path projectJsonPath, Map<String, Object> currentProperties) {
        setTitle("Project Properties");
        setHeaderText("Edit project properties");
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        
        this.projectDir = projectJsonPath.getParent();
        
        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        
        // Configure column constraints to allow growth
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setHgrow(Priority.NEVER); // Label column doesn't grow
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS); // Text field column grows
        col1.setMinWidth(MIN_TEXT_FIELD_WIDTH); // Minimum width for text fields
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.NEVER); // Browse button column doesn't grow
        
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHgrow(Priority.NEVER); // Create button column doesn't grow
        
        grid.getColumnConstraints().addAll(col0, col1, col2, col3);
        
        // Project Name field
        projectNameField = new TextField();
        projectNameField.setPromptText("Project name");
        projectNameField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(projectNameField, Priority.ALWAYS);
        Object nameObj = currentProperties.get("name");
        if (nameObj != null) {
            projectNameField.setText(nameObj.toString());
        }
        
        // Project Directory field (display only)
        projectDirField = new TextField();
        projectDirField.setText(projectDir.toString());
        projectDirField.setEditable(false);
        projectDirField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(projectDirField, Priority.ALWAYS);
        // Set light gray background and position cursor at end to show last part of path
        projectDirField.setStyle(READ_ONLY_FIELD_STYLE);
        runLater(() -> {
            projectDirField.positionCaret(projectDirField.getText().length());
        });
        
        // Main Script field with browse and create buttons
        mainScriptField = new TextField();
        mainScriptField.setPromptText("main.ebs");
        mainScriptField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(mainScriptField, Priority.ALWAYS);
        Object mainScriptObj = currentProperties.get("mainScript");
        if (mainScriptObj != null) {
            mainScriptField.setText(mainScriptObj.toString());
        }
        
        Button browseMainScriptButton = new Button("...");
        browseMainScriptButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Main Script");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("EBS Scripts", "*.ebs")
            );
            fileChooser.setInitialDirectory(projectDir.toFile());
            File selectedFile = fileChooser.showOpenDialog(getOwner());
            if (selectedFile != null) {
                // Validate path is inside project directory
                if (!isPathInsideProject(selectedFile.toPath())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "Selected file must be inside the project directory.");
                    alert.setHeaderText("Invalid Path");
                    alert.showAndWait();
                    return;
                }
                // Store relative path
                Path relativePath = projectDir.relativize(selectedFile.toPath());
                mainScriptField.setText(relativePath.toString());
                updateCreateButtonStates();
            }
        });
        
        createMainScriptButton = new Button("Create");
        createMainScriptButton.setOnAction(e -> createFile(mainScriptField, ".ebs"));
        
        // Update create button state when field changes
        mainScriptField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // CSS File field with browse and create buttons
        cssFileField = new TextField();
        cssFileField.setPromptText("styles.css");
        cssFileField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cssFileField, Priority.ALWAYS);
        Object cssFileObj = currentProperties.get("cssFile");
        if (cssFileObj != null) {
            cssFileField.setText(cssFileObj.toString());
        }
        
        Button browseCssButton = new Button("...");
        browseCssButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSS File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSS Files", "*.css")
            );
            fileChooser.setInitialDirectory(projectDir.toFile());
            File selectedFile = fileChooser.showOpenDialog(getOwner());
            if (selectedFile != null) {
                // Validate path is inside project directory
                if (!isPathInsideProject(selectedFile.toPath())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "Selected file must be inside the project directory.");
                    alert.setHeaderText("Invalid Path");
                    alert.showAndWait();
                    return;
                }
                Path relativePath = projectDir.relativize(selectedFile.toPath());
                cssFileField.setText(relativePath.toString());
                updateCreateButtonStates();
            }
        });
        
        createCssButton = new Button("Create");
        createCssButton.setOnAction(e -> createFile(cssFileField, ".css"));
        
        // Update create button state when field changes
        cssFileField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // Resource Directory field with browse button
        resourceDirField = new TextField();
        resourceDirField.setPromptText("resources");
        resourceDirField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(resourceDirField, Priority.ALWAYS);
        Object resourceDirObj = currentProperties.get("resourceDir");
        if (resourceDirObj != null) {
            resourceDirField.setText(resourceDirObj.toString());
        }
        
        Button browseResourceDirButton = new Button("...");
        browseResourceDirButton.setOnAction(e -> browseDirectory(resourceDirField, "Select Resource Directory"));
        
        createResourceDirButton = new Button("Create");
        createResourceDirButton.setOnAction(e -> createDirectory(resourceDirField));
        
        // Update create button state when field changes
        resourceDirField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // Test Directory field with browse button
        testDirField = new TextField();
        testDirField.setPromptText("tests");
        testDirField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(testDirField, Priority.ALWAYS);
        Object testDirObj = currentProperties.get("testDir");
        if (testDirObj != null) {
            testDirField.setText(testDirObj.toString());
        }
        
        Button browseTestDirButton = new Button("...");
        browseTestDirButton.setOnAction(e -> browseDirectory(testDirField, "Select Test Directory"));
        
        createTestDirButton = new Button("Create");
        createTestDirButton.setOnAction(e -> createDirectory(testDirField));
        
        // Update create button state when field changes
        testDirField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // Temp Directory field with browse button
        tempDirField = new TextField();
        tempDirField.setPromptText("temp");
        tempDirField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(tempDirField, Priority.ALWAYS);
        Object tempDirObj = currentProperties.get("tempDir");
        if (tempDirObj != null) {
            tempDirField.setText(tempDirObj.toString());
        }
        
        Button browseTempDirButton = new Button("...");
        browseTempDirButton.setOnAction(e -> browseDirectory(tempDirField, "Select Temp Directory"));
        
        createTempDirButton = new Button("Create");
        createTempDirButton.setOnAction(e -> createDirectory(tempDirField));
        
        // Update create button state when field changes
        tempDirField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // Document Directory field with browse button
        docDirField = new TextField();
        docDirField.setPromptText("docs");
        docDirField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(docDirField, Priority.ALWAYS);
        Object docDirObj = currentProperties.get("docDir");
        if (docDirObj != null) {
            docDirField.setText(docDirObj.toString());
        }
        
        Button browseDocDirButton = new Button("...");
        browseDocDirButton.setOnAction(e -> browseDirectory(docDirField, "Select Document Directory"));
        
        createDocDirButton = new Button("Create");
        createDocDirButton.setOnAction(e -> createDirectory(docDirField));
        
        // Update create button state when field changes
        docDirField.textProperty().addListener((obs, old, newVal) -> updateCreateButtonStates());
        
        // Add components to grid
        int row = 0;
        grid.add(new Label("Project Name:"), 0, row);
        grid.add(projectNameField, 1, row++, 3, 1);
        
        grid.add(new Label("Project Directory:"), 0, row);
        grid.add(projectDirField, 1, row++, 3, 1);
        
        grid.add(new Label("Main Script:"), 0, row);
        grid.add(mainScriptField, 1, row);
        grid.add(browseMainScriptButton, 2, row);
        grid.add(createMainScriptButton, 3, row++);
        
        grid.add(new Label("Default CSS:"), 0, row);
        grid.add(cssFileField, 1, row);
        grid.add(browseCssButton, 2, row);
        grid.add(createCssButton, 3, row++);
        
        // Add separator before directories
        Separator separator1 = new Separator();
        grid.add(separator1, 0, row++, 4, 1);
        
        grid.add(new Label("Resource Directory:"), 0, row);
        grid.add(resourceDirField, 1, row);
        grid.add(browseResourceDirButton, 2, row);
        grid.add(createResourceDirButton, 3, row++);
        
        grid.add(new Label("Test Directory:"), 0, row);
        grid.add(testDirField, 1, row);
        grid.add(browseTestDirButton, 2, row);
        grid.add(createTestDirButton, 3, row++);
        
        grid.add(new Label("Temp Directory:"), 0, row);
        grid.add(tempDirField, 1, row);
        grid.add(browseTempDirButton, 2, row);
        grid.add(createTempDirButton, 3, row++);
        
        grid.add(new Label("Document Directory:"), 0, row);
        grid.add(docDirField, 1, row);
        grid.add(browseDocDirButton, 2, row);
        grid.add(createDocDirButton, 3, row++);
        
        // Extract linked directories from project.json
        linkedDirectories = new ArrayList<>();
        Object dirsObj = currentProperties.get("directories");
        if (dirsObj instanceof com.eb.script.arrays.ArrayDynamic) {
            com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) dirsObj;
            for (int i = 0; i < arrayDynamic.size(); i++) {
                Object item = arrayDynamic.get(i);
                if (item instanceof String) {
                    linkedDirectories.add((String) item);
                }
            }
        } else if (dirsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) dirsObj;
            for (Object item : list) {
                if (item instanceof String) {
                    linkedDirectories.add((String) item);
                }
            }
        }
        
        // Create VBox to hold grid and linked directories table
        VBox mainContainer = new VBox(10);
        mainContainer.getChildren().add(grid);
        
        // Always add separator and linked directories section
        Separator separator2 = new Separator();
        mainContainer.getChildren().add(separator2);
        
        // Create TableView for linked directories
        Label linkedDirLabel = new Label("Linked Directories:");
        linkedDirLabel.setStyle("-fx-font-weight: bold;");
        
        linkedDirTable = new TableView<>();
        linkedDirTable.setPrefHeight(150);
        linkedDirTable.setMaxHeight(200);
        
        TableColumn<String, String> pathColumn = new TableColumn<>("Directory Path");
        pathColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        pathColumn.setPrefWidth(500);
        
        linkedDirTable.getColumns().add(pathColumn);
        linkedDirTable.getItems().addAll(linkedDirectories);
        linkedDirTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        VBox linkedDirBox = new VBox(5);
        linkedDirBox.getChildren().addAll(linkedDirLabel, linkedDirTable);
        mainContainer.getChildren().add(linkedDirBox);
        
        getDialogPane().setContent(mainContainer);
        
        // Initialize create button states
        updateCreateButtonStates();
        
        // Add OK and Cancel buttons
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);
        
        // Add "Link Directory" and "Remove Link" buttons at bottom left
        Button linkDirButton = new Button("Link Directory");
        linkDirButton.setOnAction(e -> linkDirectory());
        
        Button removeLinkButton = new Button("Remove Link");
        removeLinkButton.setOnAction(e -> removeSelectedLink());
        removeLinkButton.setDisable(true);
        
        // Enable/disable Remove Link button based on selection
        linkedDirTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            removeLinkButton.setDisable(newSel == null);
        });
        
        HBox buttonBox = new HBox(10, linkDirButton, removeLinkButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        getDialogPane().setExpandableContent(buttonBox);
        getDialogPane().setExpanded(true);
        
        // Enable/Disable OK button based on field changes and valid project name
        Button okBtn = (Button) getDialogPane().lookupButton(okButton);
        okBtn.setDisable(true);
        
        // Add listeners to all editable fields to track changes
        projectNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            fieldsEdited = true;
            okBtn.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        mainScriptField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        cssFileField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        resourceDirField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        testDirField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        tempDirField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        docDirField.textProperty().addListener((obs, old, newVal) -> {
            fieldsEdited = true;
            okBtn.setDisable(projectNameField.getText().trim().isEmpty());
        });
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new ProjectProperties(
                    projectNameField.getText().trim(),
                    mainScriptField.getText().trim(),
                    cssFileField.getText().trim(),
                    resourceDirField.getText().trim(),
                    testDirField.getText().trim(),
                    tempDirField.getText().trim(),
                    docDirField.getText().trim(),
                    new ArrayList<>(linkedDirectories)
                );
            }
            return null;
        });
    }
    
    /**
     * Link a new directory to the project.
     */
    private void linkDirectory() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory to Link");
        File selectedDir = dirChooser.showDialog(getOwner());
        if (selectedDir != null) {
            String dirPath = selectedDir.getAbsolutePath();
            if (!linkedDirectories.contains(dirPath)) {
                linkedDirectories.add(dirPath);
                linkedDirTable.getItems().add(dirPath);
                fieldsEdited = true;
                
                // Enable OK button
                Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
                if (!projectNameField.getText().trim().isEmpty()) {
                    okBtn.setDisable(false);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "This directory is already linked to the project.");
                alert.setHeaderText("Already Linked");
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Remove the selected linked directory.
     */
    private void removeSelectedLink() {
        String selected = linkedDirTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            linkedDirectories.remove(selected);
            linkedDirTable.getItems().remove(selected);
            fieldsEdited = true;
            
            // Enable OK button
            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            if (!projectNameField.getText().trim().isEmpty()) {
                okBtn.setDisable(false);
            }
        }
    }
    
    /**
     * Browse for a directory and set the relative path in the field.
     * 
     * @param field The text field to update
     * @param title The dialog title
     */
    private void browseDirectory(TextField field, String title) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(title);
        dirChooser.setInitialDirectory(projectDir.toFile());
        File selectedDir = dirChooser.showDialog(getOwner());
        if (selectedDir != null) {
            // Validate path is inside project directory
            if (!isPathInsideProject(selectedDir.toPath())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Selected directory must be inside the project directory.");
                alert.setHeaderText("Invalid Path");
                alert.showAndWait();
                return;
            }
            // Store relative path
            Path relativePath = projectDir.relativize(selectedDir.toPath());
            field.setText(relativePath.toString());
        }
    }
    
    /**
     * Check if a path is inside the project directory.
     * 
     * @param path The path to check
     * @return true if the path is inside the project directory
     */
    private boolean isPathInsideProject(Path path) {
        try {
            Path normalizedPath = path.toAbsolutePath().normalize();
            Path normalizedProjectDir = projectDir.toAbsolutePath().normalize();
            return normalizedPath.startsWith(normalizedProjectDir);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Update the enabled state of create buttons based on whether files exist.
     */
    private void updateCreateButtonStates() {
        // Main script button - enabled if field not empty and file doesn't exist
        String mainScript = mainScriptField.getText().trim();
        boolean mainScriptExists = !mainScript.isEmpty() && 
                                   Files.exists(projectDir.resolve(mainScript));
        createMainScriptButton.setDisable(mainScript.isEmpty() || mainScriptExists);
        
        // CSS button - enabled if field not empty and file doesn't exist
        String cssFile = cssFileField.getText().trim();
        boolean cssExists = !cssFile.isEmpty() && 
                           Files.exists(projectDir.resolve(cssFile));
        createCssButton.setDisable(cssFile.isEmpty() || cssExists);
        
        // Resource directory button - enabled if field not empty and directory doesn't exist
        String resourceDir = resourceDirField.getText().trim();
        boolean resourceDirExists = !resourceDir.isEmpty() && 
                                   Files.exists(projectDir.resolve(resourceDir));
        createResourceDirButton.setDisable(resourceDir.isEmpty() || resourceDirExists);
        
        // Test directory button - enabled if field not empty and directory doesn't exist
        String testDir = testDirField.getText().trim();
        boolean testDirExists = !testDir.isEmpty() && 
                               Files.exists(projectDir.resolve(testDir));
        createTestDirButton.setDisable(testDir.isEmpty() || testDirExists);
        
        // Temp directory button - enabled if field not empty and directory doesn't exist
        String tempDir = tempDirField.getText().trim();
        boolean tempDirExists = !tempDir.isEmpty() && 
                               Files.exists(projectDir.resolve(tempDir));
        createTempDirButton.setDisable(tempDir.isEmpty() || tempDirExists);
        
        // Document directory button - enabled if field not empty and directory doesn't exist
        String docDir = docDirField.getText().trim();
        boolean docDirExists = !docDir.isEmpty() && 
                              Files.exists(projectDir.resolve(docDir));
        createDocDirButton.setDisable(docDir.isEmpty() || docDirExists);
    }
    
    /**
     * Create a file with the path from the text field.
     * 
     * @param field The text field containing the relative path
     * @param extension The default file extension
     */
    private void createFile(TextField field, String extension) {
        String relativePath = field.getText().trim();
        if (relativePath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a file path.");
            alert.setHeaderText("Empty Path");
            alert.showAndWait();
            return;
        }
        
        try {
            Path filePath = projectDir.resolve(relativePath);
            
            // Ensure the path is inside project directory
            if (!isPathInsideProject(filePath)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "File path must be inside the project directory.");
                alert.setHeaderText("Invalid Path");
                alert.showAndWait();
                return;
            }
            
            // Check if file already exists
            if (Files.exists(filePath)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File already exists.");
                alert.setHeaderText("File Exists");
                alert.showAndWait();
                updateCreateButtonStates();
                return;
            }
            
            // Create parent directories if needed
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Create the file
            Files.createFile(filePath);
            
            // Update button states
            updateCreateButtonStates();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "File created successfully:\n" + filePath);
            alert.setHeaderText("Success");
            alert.showAndWait();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, 
                "Failed to create file: " + e.getMessage());
            alert.setHeaderText("Error");
            alert.showAndWait();
        }
    }
    
    /**
     * Create a directory with the path from the text field.
     * 
     * @param field The text field containing the relative path
     */
    private void createDirectory(TextField field) {
        String relativePath = field.getText().trim();
        if (relativePath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a directory path.");
            alert.setHeaderText("Empty Path");
            alert.showAndWait();
            return;
        }
        
        try {
            Path dirPath = projectDir.resolve(relativePath);
            
            // Ensure the path is inside project directory
            if (!isPathInsideProject(dirPath)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Directory path must be inside the project directory.");
                alert.setHeaderText("Invalid Path");
                alert.showAndWait();
                return;
            }
            
            // Check if directory already exists
            if (Files.exists(dirPath)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Directory already exists.");
                alert.setHeaderText("Directory Exists");
                alert.showAndWait();
                updateCreateButtonStates();
                return;
            }
            
            // Create the directory and any parent directories
            Files.createDirectories(dirPath);
            
            // Update button states
            updateCreateButtonStates();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Directory created successfully:\n" + dirPath);
            alert.setHeaderText("Success");
            alert.showAndWait();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, 
                "Failed to create directory: " + e.getMessage());
            alert.setHeaderText("Error");
            alert.showAndWait();
        }
    }
}
