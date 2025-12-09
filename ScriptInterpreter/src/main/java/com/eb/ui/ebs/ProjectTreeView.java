package com.eb.ui.ebs;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.MouseButton;
import javafx.geometry.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        
        // Setup double-click to open project
        treeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem != rootItem) {
                    openSelectedProject(selectedItem);
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
                // Get the project path from user data
                Object userData = selectedItem.getGraphic() != null ? selectedItem.getGraphic().getUserData() : null;
                String projectPath = userData instanceof String ? (String) userData : null;
                
                // Extract project directory from project.json path
                String projectDir = projectPath;
                if (projectPath != null && projectPath.endsWith("project.json")) {
                    Path jsonPath = Path.of(projectPath);
                    if (jsonPath.getParent() != null) {
                        projectDir = jsonPath.getParent().toString();
                    }
                }
                
                MenuItem newFileItem = new MenuItem("New File...");
                final String finalProjectDir = projectDir;
                newFileItem.setOnAction(e -> {
                    if (finalProjectDir != null) {
                        handler.createNewFile(finalProjectDir);
                    }
                });
                
                MenuItem addFileItem = new MenuItem("Add File...");
                addFileItem.setOnAction(e -> {
                    if (finalProjectDir != null) {
                        handler.addExistingFile(finalProjectDir);
                    }
                });
                
                MenuItem removeItem = new MenuItem("Remove from List");
                removeItem.setOnAction(e -> removeSelectedProject(selectedItem));
                
                contextMenu.getItems().addAll(newFileItem, addFileItem, new SeparatorMenuItem(), removeItem);
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
        
        for (ProjectListManager.ProjectEntry entry : entries) {
            TreeItem<String> projectItem = new TreeItem<>(entry.getName());
            
            // Add folder icon using Unicode emoji
            Label iconLabel = new Label("\uD83D\uDCC1"); // 📁 folder emoji
            iconLabel.setUserData(entry.getPath());
            projectItem.setGraphic(iconLabel);
            
            // Set tooltip with full path
            Tooltip tooltip = new Tooltip(entry.getPath());
            Tooltip.install(projectItem.getGraphic(), tooltip);
            
            rootItem.getChildren().add(projectItem);
        }
    }
}
