package com.eb.ui.ebs;

import com.eb.util.Util;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.io.File;

/**
 * Dialog for creating a new project.
 * Allows user to specify project name and directory path.
 * 
 * @author Earl Bosch
 */
public class NewProjectDialog extends Dialog<NewProjectDialog.ProjectInfo> {
    
    private final TextField projectNameField;
    private final TextField projectPathField;
    private final Button browseButton;
    
    /**
     * Result data class containing project information.
     */
    public static class ProjectInfo {
        private final String name;
        private final String path;
        
        public ProjectInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
    }
    
    /**
     * Create a new project dialog.
     * 
     * @param owner The owner window
     */
    public NewProjectDialog(Window owner) {
        setTitle("New Project");
        setHeaderText("Create a new EBS project");
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        
        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Project name field (initially blank)
        projectNameField = new TextField();
        projectNameField.setPromptText("Enter project name");
        projectNameField.setPrefWidth(300);
        
        // Project path field (populated with current console path)
        projectPathField = new TextField();
        projectPathField.setText(Util.SANDBOX_ROOT.toString());
        projectPathField.setPrefWidth(300);
        
        // Browse button to select directory
        browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Project Directory");
            
            // Set initial directory from the current path field value
            File currentPath = new File(projectPathField.getText());
            if (currentPath.exists() && currentPath.isDirectory()) {
                dirChooser.setInitialDirectory(currentPath);
            } else {
                dirChooser.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
            }
            
            File selectedDir = dirChooser.showDialog(getOwner());
            if (selectedDir != null) {
                projectPathField.setText(selectedDir.getAbsolutePath());
            }
        });
        
        // Add components to grid
        grid.add(new Label("Project Name:"), 0, 0);
        grid.add(projectNameField, 1, 0);
        
        grid.add(new Label("Project Path:"), 0, 1);
        grid.add(projectPathField, 1, 1);
        grid.add(browseButton, 2, 1);
        
        getDialogPane().setContent(grid);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        // Enable/Disable OK button based on project name
        Button okButton = (Button) getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);
        
        // Validation: OK button is enabled only when project name is not empty
        projectNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        // Convert the result when OK is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                String name = projectNameField.getText().trim();
                String path = projectPathField.getText().trim();
                
                if (!name.isEmpty() && !path.isEmpty()) {
                    return new ProjectInfo(name, path);
                }
            }
            return null;
        });
        
        // Focus on project name field
        javafx.application.Platform.runLater(() -> projectNameField.requestFocus());
    }
}
