package com.eb.ui.ebs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dialog for creating a new file in a project.
 * Allows user to specify file type, name, and path.
 * 
 * @author Earl Bosch
 */
public class NewFileDialog extends Dialog<NewFileDialog.FileInfo> {
    
    private final ComboBox<FileType> fileTypeCombo;
    private final TextField fileNameField;
    private final TextField filePathField;
    private final Button browseButton;
    
    /**
     * Supported file types for creation.
     */
    public enum FileType {
        EBS_SCRIPT("EBS Script", ".ebs"),
        JSON("JSON", ".json"),
        CSS("CSS", ".css"),
        MARKDOWN("Markdown", ".md");
        
        private final String displayName;
        private final String extension;
        
        FileType(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getExtension() {
            return extension;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    /**
     * Result data class containing file information.
     */
    public static class FileInfo {
        private final FileType type;
        private final String name;
        private final String path;
        
        public FileInfo(FileType type, String name, String path) {
            this.type = type;
            this.name = name;
            this.path = path;
        }
        
        public FileType getType() {
            return type;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
        
        /**
         * Get the full file path including the filename with extension.
         * @throws IllegalArgumentException if name or path is null or empty
         */
        public String getFullPath() {
            // Validate inputs
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("File name cannot be null or empty");
            }
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("File path cannot be null or empty");
            }
            
            Path dirPath = Paths.get(path);
            String fileName = name.trim();
            
            // Add extension if not already present
            // Check if the filename already has the correct extension
            if (!fileName.toLowerCase().endsWith(type.getExtension().toLowerCase())) {
                fileName += type.getExtension();
            }
            
            return dirPath.resolve(fileName).toString();
        }
    }
    
    /**
     * Create a new file dialog.
     * 
     * @param owner The owner window
     * @param defaultPath The default path for the file (typically the project directory)
     */
    public NewFileDialog(Window owner, String defaultPath) {
        setTitle("New File");
        setHeaderText("Create a new file in the project");
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        
        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // File type combo box
        fileTypeCombo = new ComboBox<>();
        fileTypeCombo.getItems().addAll(FileType.values());
        fileTypeCombo.setValue(FileType.EBS_SCRIPT); // Default to EBS script
        fileTypeCombo.setPrefWidth(300);
        
        // File name field
        fileNameField = new TextField();
        fileNameField.setPromptText("Enter file name");
        fileNameField.setPrefWidth(300);
        
        // Update extension hint when file type changes
        fileTypeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                fileNameField.setPromptText("Enter file name (extension " + newValue.getExtension() + " will be added)");
            }
        });
        
        // File path field (populated with project path)
        filePathField = new TextField();
        filePathField.setText(defaultPath != null ? defaultPath : System.getProperty("user.dir"));
        filePathField.setPrefWidth(300);
        
        // Browse button to select directory
        browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select File Location");
            
            // Set initial directory from the current path field value
            File currentPath = new File(filePathField.getText());
            if (currentPath.exists() && currentPath.isDirectory()) {
                dirChooser.setInitialDirectory(currentPath);
            } else if (currentPath.getParentFile() != null && currentPath.getParentFile().exists()) {
                dirChooser.setInitialDirectory(currentPath.getParentFile());
            } else {
                dirChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            
            File selectedDir = dirChooser.showDialog(getOwner());
            if (selectedDir != null) {
                filePathField.setText(selectedDir.getAbsolutePath());
            }
        });
        
        // Add components to grid
        grid.add(new Label("File Type:"), 0, 0);
        grid.add(fileTypeCombo, 1, 0);
        
        grid.add(new Label("File Name:"), 0, 1);
        grid.add(fileNameField, 1, 1);
        
        grid.add(new Label("Path:"), 0, 2);
        grid.add(filePathField, 1, 2);
        grid.add(browseButton, 2, 2);
        
        getDialogPane().setContent(grid);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        // Enable/Disable OK button based on file name
        Button okButton = (Button) getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);
        
        // Validation: OK button is enabled only when file name is not empty
        fileNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        // Convert the result when OK is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                FileType type = fileTypeCombo.getValue();
                String name = fileNameField.getText();
                String path = filePathField.getText();
                
                // Validate all inputs are non-null and non-empty (after trimming)
                if (type != null && 
                    name != null && !name.trim().isEmpty() && 
                    path != null && !path.trim().isEmpty()) {
                    return new FileInfo(type, name.trim(), path.trim());
                }
            }
            return null;
        });
        
        // Focus on file name field
        javafx.application.Platform.runLater(() -> fileNameField.requestFocus());
    }
}
