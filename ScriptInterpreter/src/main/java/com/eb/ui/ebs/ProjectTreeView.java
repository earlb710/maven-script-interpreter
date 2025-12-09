package com.eb.ui.ebs;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.MouseButton;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * TreeView component for displaying opened projects.
 * Shows project names with paths as tooltips.
 * Supports double-click to open project and context menu for removal.
 * 
 * @author Earl Bosch
 */
public class ProjectTreeView extends VBox {
    
    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final ProjectListManager projectListManager;
    private final EbsConsoleHandler handler;
    private ContextMenu currentContextMenu;
    
    /**
     * Create a new ProjectTreeView.
     * 
     * @param handler The console handler for opening projects
     */
    public ProjectTreeView(EbsConsoleHandler handler) {
        this.handler = handler;
        this.projectListManager = new ProjectListManager();
        
        // Create root item
        rootItem = new TreeItem<>("Projects");
        rootItem.setExpanded(true);
        
        // Create tree view
        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        // Setup double-click to open project or file
        treeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem != rootItem) {
                    openSelectedItem(selectedItem);
                }
            }
        });
        
        // Setup selection listener to show path in status bar
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != rootItem) {
                // Get the project path from user data
                Object userData = newValue.getGraphic() != null ? newValue.getGraphic().getUserData() : null;
                if (userData instanceof String) {
                    String projectPath = (String) userData;
                    StatusBar statusBar = handler.getStatusBar();
                    if (statusBar != null) {
                        statusBar.setStatus(projectPath);
                    }
                }
            }
        });
        
        // Setup context menu
        setupContextMenu();
        
        // Add components
        Label titleLabel = new Label("Projects");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
        
        getChildren().addAll(titleLabel, treeView);
        setPadding(new Insets(5));
        setSpacing(5);
        
        // Load projects from file after UI is initialized
        javafx.application.Platform.runLater(() -> loadProjects());
    }
    
    /**
     * Setup context menu for tree items.
     */
    private void setupContextMenu() {
        // Use a dynamic context menu that changes based on what's selected
        treeView.setOnContextMenuRequested(event -> {
            // Hide any existing context menu
            if (currentContextMenu != null && currentContextMenu.isShowing()) {
                currentContextMenu.hide();
            }
            
            TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
            
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.setAutoHide(true); // Enable auto-hide when clicking outside
            currentContextMenu = contextMenu;
            
            if (selectedItem == rootItem || selectedItem == null) {
                // Context menu for root "Projects" node
                MenuItem newProjectItem = new MenuItem("New Project...");
                newProjectItem.setOnAction(e -> handler.createNewProject());
                
                MenuItem openProjectItem = new MenuItem("Open Project...");
                openProjectItem.setOnAction(e -> handler.openProject());
                
                contextMenu.getItems().addAll(newProjectItem, openProjectItem);
                
                // Add Clear All if there are projects
                if (!rootItem.getChildren().isEmpty()) {
                    MenuItem clearAllItem = new MenuItem("Clear All Projects");
                    clearAllItem.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                "Remove all projects from the list?",
                                ButtonType.OK, ButtonType.CANCEL);
                        confirm.setHeaderText("Clear Projects");
                        var result = confirm.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            clearAllProjects();
                        }
                    });
                    contextMenu.getItems().addAll(new SeparatorMenuItem(), clearAllItem);
                }
            } else {
                // Context menu for individual project node
                // Get the project.json path from user data
                Object userData = selectedItem.getGraphic() != null ? selectedItem.getGraphic().getUserData() : null;
                String projectJsonPath = userData instanceof String ? (String) userData : null;
                
                if (projectJsonPath != null) {
                    MenuItem newFileItem = new MenuItem("New File...");
                    newFileItem.setOnAction(e -> handler.createNewFile(projectJsonPath));
                    
                    MenuItem addFileItem = new MenuItem("Add File...");
                    addFileItem.setOnAction(e -> handler.addExistingFile(projectJsonPath));
                    
                    MenuItem removeItem = new MenuItem("Remove from List");
                    removeItem.setOnAction(e -> removeSelectedProject(selectedItem));
                    
                    contextMenu.getItems().addAll(newFileItem, addFileItem, new SeparatorMenuItem(), removeItem);
                }
            }
            
            contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
        });
        
        // Add mouse click handler to hide menu when clicking elsewhere
        treeView.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && currentContextMenu != null && currentContextMenu.isShowing()) {
                currentContextMenu.hide();
            }
        });
    }
    
    /**
     * Extract the project directory from a project.json path.
     * 
     * @param projectPath Path to project.json file
     * @return The directory containing the project.json, or the original path if not a project.json
     */
    private String getProjectDirectory(String projectPath) {
        if (projectPath != null && projectPath.endsWith("project.json")) {
            Path jsonPath = Path.of(projectPath);
            if (jsonPath.getParent() != null) {
                return jsonPath.getParent().toString();
            }
        }
        return projectPath;
    }
    
    /**
     * Open the selected item (project or file).
     */
    private void openSelectedItem(TreeItem<String> item) {
        // Check if this is a file (has a parent that's not root) or a project (parent is root)
        TreeItem<String> parent = item.getParent();
        if (parent != null && parent != rootItem) {
            // This is a file - open it
            openSelectedFile(item);
        } else {
            // This is a project - open project
            openSelectedProject(item);
        }
    }
    
    /**
     * Open the selected project.
     */
    private void openSelectedProject(TreeItem<String> item) {
        String projectPath = (String) item.getValue();
        if (projectPath != null) {
            // Extract actual path from the stored user data
            Object userData = item.getGraphic() != null ? item.getGraphic().getUserData() : null;
            if (userData instanceof String) {
                projectPath = (String) userData;
            }
            
            try {
                Path path = Paths.get(projectPath);
                if (Files.exists(path)) {
                    handler.openProjectByPath(path);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Project file not found:\n" + projectPath + "\n\nRemove it from the list?",
                            ButtonType.OK, ButtonType.CANCEL);
                    alert.setHeaderText("Project Not Found");
                    var result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        removeSelectedProject(item);
                    }
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Failed to open project: " + ex.getMessage());
                alert.setHeaderText("Error");
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Open the selected file.
     */
    private void openSelectedFile(TreeItem<String> item) {
        // Get file path from user data
        Object userData = item.getGraphic() != null ? item.getGraphic().getUserData() : null;
        if (userData instanceof String) {
            String filePath = (String) userData;
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    handler.openFileFromTreeView(path);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "File not found:\n" + filePath);
                    alert.setHeaderText("File Not Found");
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Failed to open file: " + ex.getMessage());
                alert.setHeaderText("Error");
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Remove the selected project from the list.
     */
    private void removeSelectedProject(TreeItem<String> item) {
        // Get path from user data
        Object userData = item.getGraphic() != null ? item.getGraphic().getUserData() : null;
        String projectPath = userData instanceof String ? (String) userData : item.getValue();
        
        if (projectPath != null) {
            projectListManager.removeProject(projectPath);
            rootItem.getChildren().remove(item);
        }
    }
    
    /**
     * Clear all projects from the list.
     */
    private void clearAllProjects() {
        projectListManager.clearProjects();
        rootItem.getChildren().clear();
    }
    
    /**
     * Load projects from the project list manager.
     */
    public void loadProjects() {
        projectListManager.loadProjects();
        refreshTreeView();
    }
    
    /**
     * Add a project to the tree view and save it.
     * 
     * @param name Project name
     * @param path Project path
     */
    public void addProject(String name, String path) {
        projectListManager.addProject(name, path);
        refreshTreeView();
    }
    
    /**
     * Refresh the tree view from the project list.
     */
    private void refreshTreeView() {
        rootItem.getChildren().clear();
        
        List<ProjectListManager.ProjectEntry> entries = projectListManager.getProjects();
        
        // Sort entries alphabetically by name (case-insensitive)
        entries.sort((e1, e2) -> {
            String name1 = getProjectDisplayName(e1.getPath(), e1.getName());
            String name2 = getProjectDisplayName(e2.getPath(), e2.getName());
            return name1.compareToIgnoreCase(name2);
        });
        
        for (ProjectListManager.ProjectEntry entry : entries) {
            // Get project name with version
            String displayName = getProjectDisplayName(entry.getPath(), entry.getName());
            TreeItem<String> projectItem = new TreeItem<>(displayName);
            
            // Add folder icon from resources
            Label iconLabel = new Label();
            try {
                Image folderImage = new Image(getClass().getResourceAsStream("/icons/folder.png"));
                ImageView folderIcon = new ImageView(folderImage);
                folderIcon.setFitWidth(16);
                folderIcon.setFitHeight(16);
                folderIcon.setPreserveRatio(true);
                iconLabel.setGraphic(folderIcon);
            } catch (Exception e) {
                // Fallback to emoji if icon not found
                iconLabel.setText("\uD83D\uDCC1"); // 📁 folder emoji
            }
            iconLabel.setUserData(entry.getPath());
            projectItem.setGraphic(iconLabel);
            
            // Set tooltip with full path
            Tooltip tooltip = new Tooltip(entry.getPath());
            Tooltip.install(projectItem.getGraphic(), tooltip);
            
            // Load and add files from project.json
            loadProjectFiles(projectItem, entry.getPath());
            
            rootItem.getChildren().add(projectItem);
        }
    }
    
    /**
     * Get the display name for a project (name - version).
     * 
     * @param projectJsonPath Path to the project.json file
     * @param defaultName Default name to use if version not found
     * @return Display name in format "ProjectName - vX.X.X"
     */
    private String getProjectDisplayName(String projectJsonPath, String defaultName) {
        try {
            Path jsonPath = Path.of(projectJsonPath);
            if (!Files.exists(jsonPath)) {
                return defaultName;
            }
            
            String jsonContent = Files.readString(jsonPath);
            Object projectObj = com.eb.script.json.Json.parse(jsonContent);
            
            if (!(projectObj instanceof Map)) {
                return defaultName;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) projectObj;
            
            // Get name and version
            Object nameObj = project.get("name");
            Object versionObj = project.get("version");
            
            String name = nameObj instanceof String ? (String) nameObj : defaultName;
            String version = versionObj instanceof String ? (String) versionObj : null;
            
            if (version != null && !version.isEmpty()) {
                return name + " - v" + version;
            } else {
                return name;
            }
            
        } catch (Exception e) {
            System.err.println("Error reading project display name: " + e.getMessage());
            return defaultName;
        }
    }
    
    /**
     * Load files from a project.json and add them as children to the project tree item.
     * 
     * @param projectItem The project tree item to add files to
     * @param projectJsonPath Path to the project.json file
     */
    private void loadProjectFiles(TreeItem<String> projectItem, String projectJsonPath) {
        try {
            Path jsonPath = Path.of(projectJsonPath);
            if (!Files.exists(jsonPath)) {
                return;
            }
            
            String jsonContent = Files.readString(jsonPath);
            Object projectObj = com.eb.script.json.Json.parse(jsonContent);
            
            if (!(projectObj instanceof Map)) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) projectObj;
            
            // Get mainScript
            Object mainScriptObj = project.get("mainScript");
            String mainScript = mainScriptObj instanceof String ? (String) mainScriptObj : null;
            
            // Get files array
            Object filesObj = project.get("files");
            if (filesObj == null) {
                return;
            }
            
            java.util.List<String> filesList = null;
            
            // Handle ArrayDynamic (used by EBS JSON parser) or standard List
            if (filesObj instanceof com.eb.script.arrays.ArrayDynamic) {
                com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) filesObj;
                filesList = new java.util.ArrayList<>();
                for (int i = 0; i < arrayDynamic.size(); i++) {
                    Object item = arrayDynamic.get(i);
                    if (item instanceof String) {
                        filesList.add((String) item);
                    }
                }
            } else if (filesObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> list = (java.util.List<String>) filesObj;
                filesList = new java.util.ArrayList<>(list); // Create mutable copy
            }
            
            if (filesList != null && !filesList.isEmpty()) {
                Path projectDir = jsonPath.getParent();
                
                // Sort files: mainScript first, then others
                java.util.List<String> sortedFiles = new java.util.ArrayList<>();
                if (mainScript != null && filesList.contains(mainScript)) {
                    sortedFiles.add(mainScript);
                }
                for (String file : filesList) {
                    if (!file.equals(mainScript)) {
                        sortedFiles.add(file);
                    }
                }
                
                for (String filePathStr : sortedFiles) {
                    // Resolve relative paths
                    Path filePath = projectDir.resolve(filePathStr);
                    String fileName = filePath.getFileName().toString();
                    boolean fileExists = Files.exists(filePath);
                    
                    // Add "!" prefix if file doesn't exist
                    String displayName = fileExists ? fileName : "! " + fileName;
                    TreeItem<String> fileItem = new TreeItem<>(displayName);
                    
                    // Create label with icon
                    Label fileLabel = createFileLabel(fileName, filePath.toString(), fileExists);
                    fileItem.setGraphic(fileLabel);
                    
                    // Set tooltip with full path
                    Tooltip fileTooltip = new Tooltip(filePath.toString());
                    Tooltip.install(fileItem.getGraphic(), fileTooltip);
                    
                    projectItem.getChildren().add(fileItem);
                }
                
                // Expand project to show files
                projectItem.setExpanded(true);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading project files: " + e.getMessage());
        }
    }
    
    /**
     * Create a label with icon for a file.
     * 
     * @param fileName The name of the file
     * @param filePath The full path to the file
     * @param fileExists Whether the file exists
     * @return Label with icon and appropriate styling
     */
    private Label createFileLabel(String fileName, String filePath, boolean fileExists) {
        Label label = new Label();
        
        // Get icon for file type
        ImageView icon = getIconForFileType(fileName);
        if (icon != null) {
            label.setGraphic(icon);
        }
        
        // Store file path in user data
        label.setUserData(filePath);
        
        // Set red text if file doesn't exist
        if (!fileExists) {
            label.setTextFill(Color.RED);
        }
        
        return label;
    }
    
    /**
     * Get the appropriate icon for a file based on its extension.
     * 
     * @param fileName The name of the file
     * @return ImageView with the appropriate icon, or null if icon not found
     */
    private ImageView getIconForFileType(String fileName) {
        String iconPath = null;
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".ebs")) {
            iconPath = "/icons/text-file.png"; // EBS script file
        } else if (lowerName.endsWith(".json")) {
            iconPath = "/icons/config-file.png"; // JSON config file
        } else if (lowerName.endsWith(".css")) {
            iconPath = "/icons/text-file.png"; // CSS file
        } else if (lowerName.endsWith(".md")) {
            iconPath = "/icons/markdown-file.png"; // Markdown file
        } else if (lowerName.endsWith(".xml")) {
            iconPath = "/icons/xml-file.png"; // XML file
        } else if (lowerName.endsWith(".java")) {
            iconPath = "/icons/java-file.png"; // Java file
        } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || 
                   lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
            iconPath = "/icons/image-file.png"; // Image file
        } else if (lowerName.endsWith(".git") || lowerName.endsWith(".gitignore")) {
            iconPath = "/icons/git-file.png"; // Git file
        } else {
            iconPath = "/icons/file.png"; // Generic file
        }
        
        try {
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(16);
            imageView.setFitHeight(16);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconPath);
            return null;
        }
    }
    
    /**
     * Refresh the files for a specific project in the tree view.
     * 
     * @param projectJsonPath Path to the project.json file
     */
    public void refreshProjectFiles(String projectJsonPath) {
        // Find the project item in the tree
        for (TreeItem<String> projectItem : rootItem.getChildren()) {
            Object userData = projectItem.getGraphic() != null ? projectItem.getGraphic().getUserData() : null;
            if (userData instanceof String && userData.equals(projectJsonPath)) {
                // Clear existing file children
                projectItem.getChildren().clear();
                
                // Reload files
                loadProjectFiles(projectItem, projectJsonPath);
                break;
            }
        }
    }
}
