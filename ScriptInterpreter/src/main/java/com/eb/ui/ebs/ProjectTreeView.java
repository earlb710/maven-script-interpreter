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
    private ProjectFileWatcher fileWatcher;
    
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
        treeView.getStyleClass().add("project-tree");
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
        
        // Setup selection listener to show directory in status bar message
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != rootItem) {
                // Get the path from user data
                Object userData = newValue.getGraphic() != null ? newValue.getGraphic().getUserData() : null;
                if (userData instanceof String) {
                    String itemPath = (String) userData;
                    StatusBar statusBar = handler.getStatusBar();
                    if (statusBar != null) {
                        // Extract directory from the path
                        Path path = Paths.get(itemPath);
                        String directory;
                        
                        if (Files.isDirectory(path)) {
                            // If it's a directory (project or folder), show it directly
                            directory = path.toString();
                        } else {
                            // If it's a file, show its parent directory
                            Path parentPath = path.getParent();
                            directory = parentPath != null ? parentPath.toString() : path.toString();
                        }
                        
                        statusBar.setMessage(directory);
                    }
                }
            }
        });
        
        // Setup context menu
        setupContextMenu();
        
        // Add components
        Label titleLabel = new Label("Projects");
        titleLabel.getStyleClass().add("project-tree-title");
        
        getChildren().addAll(titleLabel, treeView);
        getStyleClass().add("project-tree-panel");
        setPadding(new Insets(5));
        setSpacing(5);
        
        // Initialize file watcher
        try {
            fileWatcher = new ProjectFileWatcher(this);
            fileWatcher.start();
        } catch (Exception e) {
            System.err.println("Error starting file watcher: " + e.getMessage());
            fileWatcher = null;
        }
        
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
                // Determine if this is a project, directory, or file
                TreeItem<String> parent = selectedItem.getParent();
                Object userData = selectedItem.getGraphic() != null ? selectedItem.getGraphic().getUserData() : null;
                String path = userData instanceof String ? (String) userData : null;
                
                boolean isProject = parent == rootItem;
                boolean isDirectory = path != null && Files.isDirectory(Path.of(path));
                boolean isFile = path != null && Files.isRegularFile(Path.of(path));
                
                if (isProject) {
                    // Context menu for project node
                    MenuItem newFileItem = new MenuItem("New File...");
                    newFileItem.setOnAction(e -> handler.createNewFile(path));
                    
                    MenuItem addDirItem = new MenuItem("Add Directory...");
                    addDirItem.setOnAction(e -> addDirectoryToProject(selectedItem, path));
                    
                    MenuItem removeDirItem = new MenuItem("Remove Directory...");
                    removeDirItem.setOnAction(e -> removeDirectoryFromProject(selectedItem, path));
                    
                    MenuItem renameProjectItem = new MenuItem("Rename Project...");
                    renameProjectItem.setOnAction(e -> renameProject(selectedItem, path));
                    
                    MenuItem removeItem = new MenuItem("Remove from List");
                    removeItem.setOnAction(e -> removeSelectedProject(selectedItem));
                    
                    contextMenu.getItems().addAll(
                        newFileItem, 
                        addDirItem, 
                        removeDirItem,
                        new SeparatorMenuItem(),
                        renameProjectItem,
                        new SeparatorMenuItem(),
                        removeItem
                    );
                } else if (isDirectory) {
                    // Context menu for directory node
                    MenuItem newFileItem = new MenuItem("New File...");
                    newFileItem.setOnAction(e -> {
                        // Find project root
                        TreeItem<String> projectNode = findProjectNode(selectedItem);
                        if (projectNode != null) {
                            Object projectData = projectNode.getGraphic() != null ? projectNode.getGraphic().getUserData() : null;
                            String projectPath = projectData instanceof String ? (String) projectData : null;
                            if (projectPath != null) {
                                handler.createNewFileInDirectory(projectPath, path);
                            }
                        }
                    });
                    
                    MenuItem newDirItem = new MenuItem("New Directory...");
                    newDirItem.setOnAction(e -> createNewDirectory(selectedItem, path));
                    
                    MenuItem renameDirItem = new MenuItem("Rename Directory...");
                    renameDirItem.setOnAction(e -> renameDirectory(selectedItem, path));
                    
                    MenuItem deleteDirItem = new MenuItem("Delete Directory...");
                    deleteDirItem.setOnAction(e -> deleteDirectory(selectedItem, path));
                    
                    MenuItem refreshItem = new MenuItem("Refresh");
                    refreshItem.setOnAction(e -> refreshDirectoryNode(selectedItem, path));
                    
                    contextMenu.getItems().addAll(
                        newFileItem,
                        newDirItem,
                        new SeparatorMenuItem(),
                        renameDirItem,
                        deleteDirItem,
                        new SeparatorMenuItem(),
                        refreshItem
                    );
                } else if (isFile) {
                    // Context menu for file node
                    MenuItem renameFileItem = new MenuItem("Rename File...");
                    renameFileItem.setOnAction(e -> renameFile(selectedItem, path));
                    
                    MenuItem copyFileItem = new MenuItem("Copy...");
                    copyFileItem.setOnAction(e -> copyFile(selectedItem, path));
                    
                    MenuItem deleteFileItem = new MenuItem("Delete");
                    deleteFileItem.setOnAction(e -> deleteFile(selectedItem, path));
                    
                    contextMenu.getItems().addAll(
                        renameFileItem,
                        copyFileItem,
                        new SeparatorMenuItem(),
                        deleteFileItem
                    );
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
            
            // Add project icon from resources (project.png for projects)
            Label iconLabel = new Label();
            try {
                Image projectImage = new Image(getClass().getResourceAsStream("/icons/project.png"));
                ImageView projectIcon = new ImageView(projectImage);
                projectIcon.setFitWidth(16);
                projectIcon.setFitHeight(16);
                projectIcon.setPreserveRatio(true);
                iconLabel.setGraphic(projectIcon);
            } catch (Exception e) {
                // Fallback to emoji if icon not found
                iconLabel.setText("\uD83D\uDCC2"); // 📂 open folder emoji for projects
            }
            iconLabel.setUserData(entry.getPath());
            projectItem.setGraphic(iconLabel);
            
            // Set tooltip with full path
            Tooltip tooltip = new Tooltip(entry.getPath());
            Tooltip.install(projectItem.getGraphic(), tooltip);
            
            // Load and add files from project.json
            loadProjectFiles(projectItem, entry.getPath());
            
            rootItem.getChildren().add(projectItem);
            
            // Register this project directory with the file watcher
            if (fileWatcher != null) {
                try {
                    Path jsonPath = Paths.get(entry.getPath());
                    if (Files.exists(jsonPath)) {
                        Path projectDir = jsonPath.getParent();
                        if (projectDir != null && Files.isDirectory(projectDir)) {
                            fileWatcher.registerProject(projectDir);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error registering project with file watcher: " + e.getMessage());
                }
            }
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
     * Load files from project directory and add them as children to the project tree item.
     * Scans the entire project directory recursively and also loads directories from project.json.
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
            
            Path projectDir = jsonPath.getParent();
            if (projectDir == null) {
                return;
            }
            
            // Get mainScript and directories from project.json
            String mainScript = null;
            java.util.List<String> extraDirectories = new java.util.ArrayList<>();
            try {
                String jsonContent = Files.readString(jsonPath);
                Object projectObj = com.eb.script.json.Json.parse(jsonContent);
                
                if (projectObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> project = (Map<String, Object>) projectObj;
                    
                    // Get mainScript
                    Object mainScriptObj = project.get("mainScript");
                    mainScript = mainScriptObj instanceof String ? (String) mainScriptObj : null;
                    
                    // Get directories array
                    Object dirsObj = project.get("directories");
                    if (dirsObj instanceof com.eb.script.arrays.ArrayDynamic) {
                        com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) dirsObj;
                        for (int i = 0; i < arrayDynamic.size(); i++) {
                            Object item = arrayDynamic.get(i);
                            if (item instanceof String) {
                                extraDirectories.add((String) item);
                            }
                        }
                    } else if (dirsObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) dirsObj;
                        for (Object item : list) {
                            if (item instanceof String) {
                                extraDirectories.add((String) item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading project.json: " + e.getMessage());
            }
            
            // Load all files and directories from project directory
            java.util.List<Path> paths = new java.util.ArrayList<>();
            java.util.Set<String> addedPaths = new java.util.HashSet<>();
            java.util.Set<String> linkedFolders = new java.util.HashSet<>();
            
            try (var stream = Files.list(projectDir)) {
                stream.forEach(p -> {
                    paths.add(p);
                    addedPaths.add(p.toString());
                });
            }
            
            // Add directories from project.json that aren't already in the list
            for (String dirPath : extraDirectories) {
                Path dir = projectDir.resolve(dirPath);
                String normalizedPath = dir.normalize().toString();
                
                if (!addedPaths.contains(normalizedPath)) {
                    paths.add(dir);
                    addedPaths.add(normalizedPath);
                    linkedFolders.add(normalizedPath); // Mark as linked folder
                } else {
                    // Even if the directory exists in project dir, mark it as linked if it's in extraDirectories
                    linkedFolders.add(normalizedPath);
                }
            }
            
            // Sort: mainScript first, then directories, then files, alphabetically
            final String finalMainScript = mainScript;
            paths.sort((p1, p2) -> {
                String name1 = p1.getFileName().toString();
                String name2 = p2.getFileName().toString();
                
                // mainScript always first
                if (finalMainScript != null) {
                    if (name1.equals(finalMainScript)) return -1;
                    if (name2.equals(finalMainScript)) return 1;
                }
                
                boolean p1IsDir = Files.isDirectory(p1);
                boolean p2IsDir = Files.isDirectory(p2);
                
                // Directories before files
                if (p1IsDir != p2IsDir) {
                    return p1IsDir ? -1 : 1;
                }
                
                // Alphabetical within same type
                return name1.compareToIgnoreCase(name2);
            });
            
            for (Path path : paths) {
                String fileName = path.getFileName().toString();
                
                // Skip hidden files and project.json
                if (fileName.startsWith(".") || fileName.equals("project.json")) {
                    continue;
                }
                
                boolean isDirectory = Files.isDirectory(path);
                boolean fileExists = Files.exists(path);
                boolean isLinkedFolder = isDirectory && linkedFolders.contains(path.normalize().toString());
                
                // Add "!" prefix if doesn't exist
                String displayName = fileExists ? fileName : "! " + fileName;
                TreeItem<String> item = new TreeItem<>(displayName);
                
                // Create label with icon
                Label label = createFileOrDirLabel(fileName, path.toString(), fileExists, isDirectory, isLinkedFolder);
                item.setGraphic(label);
                
                // Set tooltip
                Tooltip tooltip = new Tooltip(path.toString());
                Tooltip.install(item.getGraphic(), tooltip);
                
                projectItem.getChildren().add(item);
                
                // Load directory contents recursively
                if (isDirectory && fileExists) {
                    loadDirectoryContents(item, path);
                }
            }
            
            // Expand project to show files
            projectItem.setExpanded(true);
            
        } catch (Exception e) {
            System.err.println("Error loading project files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a label with icon for a file or directory.
     * 
     * @param fileName The name of the file/directory
     * @param path The full path
     * @param exists Whether the file/directory exists
     * @param isDirectory Whether this is a directory
     * @param isLinkedFolder Whether this is a linked folder from project.json directories array
     * @return Label with icon and appropriate styling
     */
    private Label createFileOrDirLabel(String fileName, String path, boolean exists, boolean isDirectory, boolean isLinkedFolder) {
        Label label = new Label();
        
        if (isDirectory) {
            // Directory icon - use folder_ref.png for linked folders, folder.png for regular folders
            try {
                String iconPath = isLinkedFolder ? "/icons/folder_ref.png" : "/icons/folder.png";
                Image folderImage = new Image(getClass().getResourceAsStream(iconPath));
                ImageView folderIcon = new ImageView(folderImage);
                folderIcon.setFitWidth(16);
                folderIcon.setFitHeight(16);
                folderIcon.setPreserveRatio(true);
                label.setGraphic(folderIcon);
            } catch (Exception e) {
                // Fallback to emoji
            }
        } else {
            // File icon
            ImageView icon = getIconForFileType(fileName);
            if (icon != null) {
                label.setGraphic(icon);
            }
        }
        
        // Store path in user data
        label.setUserData(path);
        
        // Set red text if doesn't exist - use CSS class
        if (!exists) {
            label.getStyleClass().add("project-tree-error");
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
    
    /**
     * Find the project node that contains the given tree item.
     * 
     * @param item The tree item to search from
     * @return The project node, or null if not found
     */
    private TreeItem<String> findProjectNode(TreeItem<String> item) {
        TreeItem<String> current = item;
        while (current != null && current.getParent() != rootItem) {
            current = current.getParent();
        }
        return current;
    }
    
    /**
     * Create a new subdirectory within the given directory.
     * 
     * @param dirItem The parent directory tree item
     * @param dirPath The parent directory path
     */
    private void createNewDirectory(TreeItem<String> dirItem, String dirPath) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Directory");
        dialog.setHeaderText("Create new directory in: " + Path.of(dirPath).getFileName());
        dialog.setContentText("Directory name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (newName == null || newName.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Directory name cannot be empty.");
                alert.setHeaderText("Invalid Name");
                alert.showAndWait();
                return;
            }
            
            try {
                Path parentPath = Path.of(dirPath);
                Path newDirPath = parentPath.resolve(newName.trim());
                
                if (Files.exists(newDirPath)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "A directory with that name already exists.");
                    alert.setHeaderText("Directory Exists");
                    alert.showAndWait();
                    return;
                }
                
                // Create the directory
                Files.createDirectory(newDirPath);
                
                // Refresh the parent directory in tree to show the new directory
                refreshDirectoryNode(dirItem, dirPath);
                
                System.out.println("Directory created: " + newDirPath);
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Failed to create directory: " + e.getMessage());
                alert.setHeaderText("Creation Error");
                alert.showAndWait();
            }
        });
    }
    
    /**
     * Rename a directory.
     * 
     * @param dirItem The directory tree item
     * @param dirPath The current directory path
     */
    private void renameDirectory(TreeItem<String> dirItem, String dirPath) {
        Path currentPath = Path.of(dirPath);
        TextInputDialog dialog = new TextInputDialog(currentPath.getFileName().toString());
        dialog.setTitle("Rename Directory");
        dialog.setHeaderText("Rename directory");
        dialog.setContentText("New name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (newName == null || newName.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Directory name cannot be empty.");
                alert.setHeaderText("Invalid Name");
                alert.showAndWait();
                return;
            }
            
            try {
                Path newPath = currentPath.getParent().resolve(newName.trim());
                
                if (Files.exists(newPath)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "A directory with that name already exists.");
                    alert.setHeaderText("Directory Exists");
                    alert.showAndWait();
                    return;
                }
                
                // Rename the directory
                Files.move(currentPath, newPath);
                
                // Update tree item
                dirItem.setValue(newName.trim());
                Label label = (Label) dirItem.getGraphic();
                if (label != null) {
                    label.setUserData(newPath.toString());
                }
                
                // Refresh tooltip
                Tooltip.install(dirItem.getGraphic(), new Tooltip(newPath.toString()));
                
                // Reload directory contents with new path
                dirItem.getChildren().clear();
                loadDirectoryContents(dirItem, newPath);
                
                System.out.println("Directory renamed: " + newPath);
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Failed to rename directory: " + e.getMessage());
                alert.setHeaderText("Rename Error");
                alert.showAndWait();
            }
        });
    }
    
    /**
     * Delete a directory after confirmation, showing file count.
     * 
     * @param dirItem The directory tree item
     * @param dirPath The directory path to delete
     */
    private void deleteDirectory(TreeItem<String> dirItem, String dirPath) {
        try {
            Path path = Path.of(dirPath);
            
            // Count files and subdirectories
            long[] counts = countFilesAndDirectories(path);
            long fileCount = counts[0];
            long dirCount = counts[1];
            
            // Build confirmation message
            StringBuilder message = new StringBuilder("Are you sure you want to delete this directory");
            if (fileCount > 0 || dirCount > 0) {
                message.append(" containing:");
                if (fileCount > 0) {
                    message.append("\n• ").append(fileCount).append(" file").append(fileCount > 1 ? "s" : "");
                }
                if (dirCount > 0) {
                    message.append("\n• ").append(dirCount).append(" subdirector").append(dirCount > 1 ? "ies" : "y");
                }
            }
            message.append("?\n\nThis action cannot be undone.");
            
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Directory");
            confirmDialog.setHeaderText("Delete directory: " + path.getFileName());
            confirmDialog.setContentText(message.toString());
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Delete directory and all its contents recursively
                        deleteDirectoryRecursively(path);
                        
                        // Remove from tree view
                        TreeItem<String> parent = dirItem.getParent();
                        if (parent != null) {
                            parent.getChildren().remove(dirItem);
                        }
                        
                        System.out.println("Directory deleted: " + dirPath);
                        
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, 
                            "Failed to delete directory: " + e.getMessage());
                        alert.setHeaderText("Delete Error");
                        alert.showAndWait();
                    }
                }
            });
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, 
                "Failed to analyze directory: " + e.getMessage());
            alert.setHeaderText("Error");
            alert.showAndWait();
        }
    }
    
    /**
     * Count files and subdirectories in a directory recursively.
     * 
     * @param directory The directory to count
     * @return Array with [fileCount, directoryCount]
     */
    private long[] countFilesAndDirectories(Path directory) throws Exception {
        long[] counts = {0, 0}; // [files, directories]
        
        Files.walk(directory)
            .forEach(path -> {
                if (!path.equals(directory)) {
                    if (Files.isDirectory(path)) {
                        counts[1]++;
                    } else if (Files.isRegularFile(path)) {
                        counts[0]++;
                    }
                }
            });
        
        return counts;
    }
    
    /**
     * Delete a directory and all its contents recursively.
     * 
     * @param directory The directory to delete
     */
    private void deleteDirectoryRecursively(Path directory) throws Exception {
        Files.walk(directory)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to delete: " + path, e);
                }
            });
    }
    
    /**
     * Rename a file.
     * 
     * @param fileItem The file tree item
     * @param filePath The current file path
     */
    private void renameFile(TreeItem<String> fileItem, String filePath) {
        TextInputDialog dialog = new TextInputDialog(Path.of(filePath).getFileName().toString());
        dialog.setTitle("Rename File");
        dialog.setHeaderText("Rename file");
        dialog.setContentText("New name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            try {
                Path oldPath = Path.of(filePath);
                Path newPath = oldPath.getParent().resolve(newName);
                
                if (Files.exists(newPath)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "A file with that name already exists.");
                    alert.showAndWait();
                    return;
                }
                
                Files.move(oldPath, newPath);
                
                // Update tree
                fileItem.setValue(newName);
                Label label = (Label) fileItem.getGraphic();
                if (label != null) {
                    label.setUserData(newPath.toString());
                    ImageView icon = getIconForFileType(newName);
                    if (icon != null) {
                        label.setGraphic(icon);
                    }
                }
                
                // Refresh tooltip
                Tooltip.install(fileItem.getGraphic(), new Tooltip(newPath.toString()));
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to rename file: " + e.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    /**
     * Delete a file after confirmation.
     * 
     * @param fileItem The file tree item
     * @param filePath The file path to delete
     */
    private void deleteFile(TreeItem<String> fileItem, String filePath) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete File");
        confirmDialog.setHeaderText("Delete file: " + Path.of(filePath).getFileName());
        confirmDialog.setContentText("Are you sure you want to delete this file?\nThis action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Path path = Path.of(filePath);
                    Files.delete(path);
                    
                    // Remove from tree view
                    TreeItem<String> parent = fileItem.getParent();
                    if (parent != null) {
                        parent.getChildren().remove(fileItem);
                    }
                    
                    System.out.println("File deleted: " + filePath);
                    
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "Failed to delete file: " + e.getMessage());
                    alert.setHeaderText("Delete Error");
                    alert.showAndWait();
                }
            }
        });
    }
    
    /**
     * Copy a file with a new name.
     * 
     * @param fileItem The file tree item
     * @param filePath The source file path
     */
    private void copyFile(TreeItem<String> fileItem, String filePath) {
        Path sourcePath = Path.of(filePath);
        String originalFileName = sourcePath.getFileName().toString();
        
        TextInputDialog dialog = new TextInputDialog(originalFileName);
        dialog.setTitle("Copy File");
        dialog.setHeaderText("Copy file: " + originalFileName);
        dialog.setContentText("New name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (newName == null || newName.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "File name cannot be empty.");
                alert.setHeaderText("Invalid Name");
                alert.showAndWait();
                return;
            }
            
            try {
                Path targetPath = sourcePath.getParent().resolve(newName.trim());
                
                if (Files.exists(targetPath)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, 
                        "A file with that name already exists.");
                    alert.setHeaderText("File Exists");
                    alert.showAndWait();
                    return;
                }
                
                // Copy the file
                Files.copy(sourcePath, targetPath);
                
                // Refresh the parent directory in tree to show the new file
                TreeItem<String> parent = fileItem.getParent();
                if (parent != null) {
                    Object parentData = parent.getGraphic() != null ? parent.getGraphic().getUserData() : null;
                    if (parentData instanceof String) {
                        String parentPath = (String) parentData;
                        refreshDirectoryNode(parent, parentPath);
                    }
                }
                
                System.out.println("File copied: " + targetPath);
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Failed to copy file: " + e.getMessage());
                alert.setHeaderText("Copy Error");
                alert.showAndWait();
            }
        });
    }
    
    /**
     * Rename a project.
     * 
     * @param projectItem The project tree item
     * @param projectJsonPath The project.json path
     */
    private void renameProject(TreeItem<String> projectItem, String projectJsonPath) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rename Project");
        dialog.setHeaderText("Rename project");
        dialog.setContentText("New name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            try {
                // Update project.json
                Path jsonPath = Path.of(projectJsonPath);
                String jsonContent = Files.readString(jsonPath);
                Object projectObj = com.eb.script.json.Json.parse(jsonContent);
                
                if (projectObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> project = (Map<String, Object>) projectObj;
                    project.put("name", newName);
                    
                    String updatedJson = com.eb.script.json.Json.prettyJson(project);
                    Files.writeString(jsonPath, updatedJson);
                    
                    // Update tree view
                    String displayName = getProjectDisplayName(projectJsonPath, newName);
                    projectItem.setValue(displayName);
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to rename project: " + e.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    /**
     * Add a directory to the project view and save it to project.json.
     * 
     * @param projectItem The project tree item
     * @param projectJsonPath The project.json path
     */
    private void addDirectoryToProject(TreeItem<String> projectItem, String projectJsonPath) {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Directory to Add");
        
        Path projectDir = Path.of(projectJsonPath).getParent();
        if (projectDir != null && Files.exists(projectDir)) {
            chooser.setInitialDirectory(projectDir.toFile());
        }
        
        java.io.File selectedDir = chooser.showDialog(treeView.getScene().getWindow());
        if (selectedDir != null) {
            try {
                // Read project.json
                Path jsonPath = Path.of(projectJsonPath);
                String jsonContent = Files.readString(jsonPath);
                Object projectObj = com.eb.script.json.Json.parse(jsonContent);
                
                if (projectObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> project = (Map<String, Object>) projectObj;
                    
                    // Get or create directories array
                    Object dirsObj = project.get("directories");
                    java.util.List<String> dirsList = null;
                    
                    // Handle ArrayDynamic or standard List
                    if (dirsObj instanceof com.eb.script.arrays.ArrayDynamic) {
                        com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) dirsObj;
                        dirsList = new java.util.ArrayList<>();
                        for (int i = 0; i < arrayDynamic.size(); i++) {
                            Object item = arrayDynamic.get(i);
                            if (item instanceof String) {
                                dirsList.add((String) item);
                            }
                        }
                    } else if (dirsObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> list = (java.util.List<String>) dirsObj;
                        dirsList = new java.util.ArrayList<>(list);
                    }
                    
                    if (dirsList == null) {
                        dirsList = new java.util.ArrayList<>();
                    }
                    
                    // Make directory path relative to project directory if possible
                    Path selectedPath = selectedDir.toPath();
                    String relativePath;
                    try {
                        relativePath = projectDir.relativize(selectedPath).toString();
                    } catch (IllegalArgumentException e) {
                        // If can't be relativized, use absolute path
                        relativePath = selectedPath.toString();
                    }
                    
                    // Add directory if not already in list
                    if (!dirsList.contains(relativePath)) {
                        dirsList.add(relativePath);
                        
                        // Update the project map with the modified directories list
                        project.put("directories", dirsList);
                        
                        // Write updated project.json
                        String updatedJson = com.eb.script.json.Json.prettyJson(project);
                        Files.writeString(jsonPath, updatedJson);
                        
                        System.out.println("Added directory to project.json: " + relativePath);
                    }
                }
                
                // Refresh the project to show the directory
                projectItem.getChildren().clear();
                loadProjectFiles(projectItem, projectJsonPath);
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add directory: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Remove a directory from the project view.
     * 
     * @param projectItem The project tree item
     * @param projectJsonPath The project.json path
     */
    private void removeDirectoryFromProject(TreeItem<String> projectItem, String projectJsonPath) {
        // Show list of directories to remove
        java.util.List<String> directories = new java.util.ArrayList<>();
        for (TreeItem<String> child : projectItem.getChildren()) {
            Object userData = child.getGraphic() != null ? child.getGraphic().getUserData() : null;
            if (userData instanceof String) {
                Path path = Path.of((String) userData);
                if (Files.isDirectory(path)) {
                    directories.add(child.getValue());
                }
            }
        }
        
        if (directories.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No directories to remove.");
            alert.showAndWait();
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(directories.get(0), directories);
        dialog.setTitle("Remove Directory");
        dialog.setHeaderText("Select directory to remove from view");
        dialog.setContentText("Directory:");
        
        dialog.showAndWait().ifPresent(dirName -> {
            // Just refresh - directories will be hidden if needed
            projectItem.getChildren().clear();
            loadProjectFiles(projectItem, projectJsonPath);
        });
    }
    
    /**
     * Refresh a directory node.
     * 
     * @param dirItem The directory tree item
     * @param dirPath The directory path
     */
    private void refreshDirectoryNode(TreeItem<String> dirItem, String dirPath) {
        dirItem.getChildren().clear();
        loadDirectoryContents(dirItem, Path.of(dirPath));
    }
    
    /**
     * Load directory contents recursively.
     * 
     * @param parentItem The parent tree item
     * @param dirPath The directory path
     */
    private void loadDirectoryContents(TreeItem<String> parentItem, Path dirPath) {
        try {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return;
            }
            
            java.util.List<Path> paths = new java.util.ArrayList<>();
            try (var stream = Files.list(dirPath)) {
                stream.forEach(paths::add);
            }
            
            // Sort: directories first, then files, alphabetically
            paths.sort((p1, p2) -> {
                boolean p1IsDir = Files.isDirectory(p1);
                boolean p2IsDir = Files.isDirectory(p2);
                if (p1IsDir != p2IsDir) {
                    return p1IsDir ? -1 : 1;
                }
                return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
            });
            
            for (Path path : paths) {
                String fileName = path.getFileName().toString();
                
                // Skip hidden files and project.json
                if (fileName.startsWith(".") || fileName.equals("project.json")) {
                    continue;
                }
                
                boolean isDirectory = Files.isDirectory(path);
                TreeItem<String> item = new TreeItem<>(fileName);
                
                // Create label with icon - subdirectories are not linked folders
                Label label = createFileOrDirLabel(fileName, path.toString(), true, isDirectory, false);
                item.setGraphic(label);
                
                // Set tooltip
                Tooltip tooltip = new Tooltip(path.toString());
                Tooltip.install(item.getGraphic(), tooltip);
                
                parentItem.getChildren().add(item);
                
                // Load directory contents recursively
                if (isDirectory) {
                    loadDirectoryContents(item, path);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading directory contents: " + e.getMessage());
        }
    }
    
    /**
     * Refreshes all projects in the tree view.
     * Called by the file watcher when changes are detected.
     */
    public void refreshProjects() {
        loadProjects();
    }
    
    /**
     * Expands all project nodes in the tree view.
     */
    public void expandAllProjects() {
        for (TreeItem<String> projectItem : rootItem.getChildren()) {
            projectItem.setExpanded(true);
        }
    }
    
    /**
     * Returns the file watcher for this tree view.
     * 
     * @return the file watcher, or null if it couldn't be created
     */
    public ProjectFileWatcher getFileWatcher() {
        return fileWatcher;
    }
    
    /**
     * Stops the file watcher when the tree view is no longer needed.
     */
    public void shutdown() {
        if (fileWatcher != null) {
            fileWatcher.stop();
        }
    }
}
