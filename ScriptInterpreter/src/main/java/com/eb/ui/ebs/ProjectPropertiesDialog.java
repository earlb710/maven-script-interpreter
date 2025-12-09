package com.eb.ui.ebs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Dialog for editing project properties.
 * Allows user to modify project name, main script, CSS file, and directory paths.
 * 
 * @author Earl Bosch
 */
public class ProjectPropertiesDialog extends Dialog<ProjectPropertiesDialog.ProjectProperties> {
    
    private final TextField projectNameField;
    private final TextField mainScriptField;
    private final TextField cssFileField;
    private final TextField resourceDirField;
    private final TextField testDirField;
    private final TextField tempDirField;
    private final Path projectDir;
    
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
        
        public ProjectProperties(String name, String mainScript, String cssFile, 
                               String resourceDir, String testDir, String tempDir) {
            this.name = name;
            this.mainScript = mainScript;
            this.cssFile = cssFile;
            this.resourceDir = resourceDir;
            this.testDir = testDir;
            this.tempDir = tempDir;
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
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Project Name field
        projectNameField = new TextField();
        projectNameField.setPromptText("Project name");
        projectNameField.setPrefWidth(300);
        Object nameObj = currentProperties.get("name");
        if (nameObj != null) {
            projectNameField.setText(nameObj.toString());
        }
        
        // Main Script field with browse button
        mainScriptField = new TextField();
        mainScriptField.setPromptText("main.ebs");
        mainScriptField.setPrefWidth(250);
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
                // Store relative path
                Path relativePath = projectDir.relativize(selectedFile.toPath());
                mainScriptField.setText(relativePath.toString());
            }
        });
        
        // CSS File field with browse button
        cssFileField = new TextField();
        cssFileField.setPromptText("styles.css");
        cssFileField.setPrefWidth(250);
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
                Path relativePath = projectDir.relativize(selectedFile.toPath());
                cssFileField.setText(relativePath.toString());
            }
        });
        
        // Resource Directory field with browse button
        resourceDirField = new TextField();
        resourceDirField.setPromptText("resources");
        resourceDirField.setPrefWidth(250);
        Object resourceDirObj = currentProperties.get("resourceDir");
        if (resourceDirObj != null) {
            resourceDirField.setText(resourceDirObj.toString());
        }
        
        Button browseResourceDirButton = new Button("...");
        browseResourceDirButton.setOnAction(e -> browseDirectory(resourceDirField, "Select Resource Directory"));
        
        // Test Directory field with browse button
        testDirField = new TextField();
        testDirField.setPromptText("tests");
        testDirField.setPrefWidth(250);
        Object testDirObj = currentProperties.get("testDir");
        if (testDirObj != null) {
            testDirField.setText(testDirObj.toString());
        }
        
        Button browseTestDirButton = new Button("...");
        browseTestDirButton.setOnAction(e -> browseDirectory(testDirField, "Select Test Directory"));
        
        // Temp Directory field with browse button
        tempDirField = new TextField();
        tempDirField.setPromptText("temp");
        tempDirField.setPrefWidth(250);
        Object tempDirObj = currentProperties.get("tempDir");
        if (tempDirObj != null) {
            tempDirField.setText(tempDirObj.toString());
        }
        
        Button browseTempDirButton = new Button("...");
        browseTempDirButton.setOnAction(e -> browseDirectory(tempDirField, "Select Temp Directory"));
        
        // Add components to grid
        int row = 0;
        grid.add(new Label("Project Name:"), 0, row);
        grid.add(projectNameField, 1, row++, 2, 1);
        
        grid.add(new Label("Main Script:"), 0, row);
        grid.add(mainScriptField, 1, row);
        grid.add(browseMainScriptButton, 2, row++);
        
        grid.add(new Label("Default CSS:"), 0, row);
        grid.add(cssFileField, 1, row);
        grid.add(browseCssButton, 2, row++);
        
        grid.add(new Label("Resource Directory:"), 0, row);
        grid.add(resourceDirField, 1, row);
        grid.add(browseResourceDirButton, 2, row++);
        
        grid.add(new Label("Test Directory:"), 0, row);
        grid.add(testDirField, 1, row);
        grid.add(browseTestDirButton, 2, row++);
        
        grid.add(new Label("Temp Directory:"), 0, row);
        grid.add(tempDirField, 1, row);
        grid.add(browseTempDirButton, 2, row++);
        
        getDialogPane().setContent(grid);
        
        // Add OK and Cancel buttons
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);
        
        // Enable/Disable OK button based on project name
        Button okBtn = (Button) getDialogPane().lookupButton(okButton);
        okBtn.setDisable(true);
        
        projectNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            okBtn.setDisable(newValue == null || newValue.trim().isEmpty());
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
                    tempDirField.getText().trim()
                );
            }
            return null;
        });
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
            // Store relative path
            Path relativePath = projectDir.relativize(selectedDir.toPath());
            field.setText(relativePath.toString());
        }
    }
}
