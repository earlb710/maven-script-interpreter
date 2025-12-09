package com.eb.ui.ebs;

import com.eb.script.json.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the list of opened projects stored in console-projects.json.
 * Provides methods to load, save, add, and remove projects.
 * 
 * @author Earl Bosch
 */
public class ProjectListManager {
    
    private static final String PROJECTS_FILE = "console-projects.json";
    private final List<ProjectEntry> projects = new ArrayList<>();
    private Path projectsFilePath = null; // Cached resolved path for the projects file
    
    /**
     * Represents a project entry with name and path.
     */
    public static class ProjectEntry {
        private final String name;
        private final String path;
        
        public ProjectEntry(String name, String path) {
            this.name = name;
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ProjectEntry)) return false;
            ProjectEntry other = (ProjectEntry) obj;
            return path != null && path.equals(other.path);
        }
        
        @Override
        public int hashCode() {
            return path != null ? path.hashCode() : 0;
        }
    }
    
    /**
     * Resolve the path to the console-projects.json file.
     * Uses the same strategy as console.cfg: current directory, then parent directory.
     * Caches the result for consistent load/save location.
     * 
     * @return Path to console-projects.json file
     */
    private Path resolveProjectsFilePath() {
        if (projectsFilePath != null) {
            return projectsFilePath;
        }
        
        // Try current directory first
        Path path = Paths.get(PROJECTS_FILE);
        if (Files.exists(path)) {
            projectsFilePath = path.toAbsolutePath();
            System.out.println("Found " + PROJECTS_FILE + " at: " + projectsFilePath);
            return projectsFilePath;
        }
        
        // Try parent directory
        path = Paths.get("..", PROJECTS_FILE);
        if (Files.exists(path)) {
            projectsFilePath = path.toAbsolutePath();
            System.out.println("Found " + PROJECTS_FILE + " in parent dir at: " + projectsFilePath);
            return projectsFilePath;
        }
        
        // Not found - will create in current directory
        projectsFilePath = Paths.get(PROJECTS_FILE).toAbsolutePath();
        System.out.println(PROJECTS_FILE + " will be created at: " + projectsFilePath);
        return projectsFilePath;
    }
    
    /**
     * Load projects from console-projects.json file.
     * Reads project paths and loads project names from each project.json file.
     */
    public void loadProjects() {
        projects.clear();
        try {
            // Resolve the file path (checks current dir, parent dir, or sets default)
            Path projectsPath = resolveProjectsFilePath();
            
            // If file doesn't exist yet, create empty file and start with empty list
            if (!Files.exists(projectsPath)) {
                System.out.println("Projects file not found, creating empty file at: " + projectsPath);
                createEmptyProjectsFile();
                return;
            }
            
            System.out.println("Loading projects from: " + projectsPath);
            String jsonContent = Files.readString(projectsPath);
            System.out.println("File content: " + jsonContent.substring(0, Math.min(200, jsonContent.length())) + "...");
            
            Object parsed = Json.parse(jsonContent);
            
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> projectsMap = (Map<String, Object>) parsed;
                Object projectsArray = projectsMap.get("projects");
                
                // Handle ArrayDynamic (used by EBS JSON parser) or standard List
                List<Object> projectsList = null;
                
                if (projectsArray instanceof com.eb.script.arrays.ArrayDynamic) {
                    // EBS JSON parser returns ArrayDynamic
                    com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) projectsArray;
                    
                    // Convert ArrayDynamic to List for processing
                    projectsList = new ArrayList<>();
                    for (int i = 0; i < arrayDynamic.size(); i++) {
                        projectsList.add(arrayDynamic.get(i));
                    }
                } else if (projectsArray instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) projectsArray;
                    projectsList = list;
                }
                
                if (projectsList != null) {
                    if (projectsList.isEmpty()) {
                        System.out.println("No projects found in " + PROJECTS_FILE + ". Use File → New Project or File → Open Project to add projects.");
                    }
                    
                    for (Object projectObj : projectsList) {
                        try {
                            String path = null;
                            
                            // Support both old format (with name) and new format (path only)
                            if (projectObj instanceof String) {
                                // New format: just a path string
                                path = (String) projectObj;
                            } else if (projectObj instanceof Map) {
                                // Old format: object with name and path
                                @SuppressWarnings("unchecked")
                                Map<String, Object> projectMap = (Map<String, Object>) projectObj;
                                path = (String) projectMap.get("path");
                            }
                            
                            if (path != null) {
                                // Read project name from the project.json file
                                String name = readProjectNameFromFile(path);
                                if (name != null) {
                                    projects.add(new ProjectEntry(name, path));
                                } else {
                                    System.err.println("  Failed to read project name from: " + path);
                                }
                            } else {
                                System.err.println("  No path found in project entry");
                            }
                        } catch (Exception e) {
                            System.err.println("ERROR processing project entry: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    if (projects.size() > 0) {
                        System.out.println("Loaded " + projects.size() + " projects from " + PROJECTS_FILE);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + PROJECTS_FILE + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error parsing " + PROJECTS_FILE + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create an empty console-projects.json file at the resolved location.
     */
    private void createEmptyProjectsFile() {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("projects", new ArrayList<>());
            String jsonContent = Json.prettyJson(root);
            Path path = resolveProjectsFilePath(); // Use consistent path
            Files.writeString(path, jsonContent);
            System.out.println("Created empty " + PROJECTS_FILE + " at: " + path);
        } catch (Exception e) {
            System.err.println("Error creating " + PROJECTS_FILE + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Read project name from a project.json file.
     * 
     * @param projectJsonPath Path to the project.json file
     * @return Project name, or null if cannot be read
     */
    private String readProjectNameFromFile(String projectJsonPath) {
        try {
            Path path = Paths.get(projectJsonPath);
            System.out.println("Attempting to read project from: " + projectJsonPath);
            System.out.println("  Resolved path: " + path.toAbsolutePath());
            
            if (!Files.exists(path)) {
                System.err.println("  ERROR: Project file not found at: " + path.toAbsolutePath());
                return null;
            }
            
            String jsonContent = Files.readString(path);
            Object parsed = Json.parse(jsonContent);
            
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> projectMap = (Map<String, Object>) parsed;
                Object nameObj = projectMap.get("name");
                if (nameObj instanceof String) {
                    String name = (String) nameObj;
                    System.out.println("  Successfully read project name: " + name);
                    return name;
                } else {
                    System.err.println("  ERROR: 'name' field not found or not a string in " + projectJsonPath);
                }
            } else {
                System.err.println("  ERROR: Project file is not a valid JSON object: " + projectJsonPath);
            }
        } catch (Exception e) {
            System.err.println("  ERROR reading project name from " + projectJsonPath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Save projects to console-projects.json file at the resolved location.
     * Stores only the paths, not the names.
     */
    public void saveProjects() {
        try {
            // Build JSON structure - store only paths
            Map<String, Object> root = new LinkedHashMap<>();
            List<String> projectPaths = new ArrayList<>();
            
            for (ProjectEntry entry : projects) {
                projectPaths.add(entry.getPath());
            }
            
            root.put("projects", projectPaths);
            
            // Convert to JSON string
            String jsonContent = Json.prettyJson(root);
            
            // Write to file at the resolved path (consistent with load location)
            Path path = resolveProjectsFilePath();
            Files.writeString(path, jsonContent);
            
            System.out.println("Saved " + projects.size() + " project paths to " + path);
        } catch (IOException e) {
            System.err.println("Error writing " + PROJECTS_FILE + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error saving projects: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add a project to the list. If it already exists, it's moved to the end.
     * Automatically saves after adding.
     * 
     * @param name Project name
     * @param path Project path
     */
    public void addProject(String name, String path) {
        // Remove if already exists (to avoid duplicates)
        ProjectEntry newEntry = new ProjectEntry(name, path);
        projects.remove(newEntry);
        
        // Add to end of list (most recent)
        projects.add(newEntry);
        
        // Auto-save
        saveProjects();
    }
    
    /**
     * Remove a project from the list by path.
     * Automatically saves after removing.
     * 
     * @param path Project path
     * @return true if project was removed, false if not found
     */
    public boolean removeProject(String path) {
        boolean removed = projects.removeIf(entry -> entry.getPath().equals(path));
        if (removed) {
            saveProjects();
        }
        return removed;
    }
    
    /**
     * Get all projects.
     * 
     * @return List of project entries
     */
    public List<ProjectEntry> getProjects() {
        return new ArrayList<>(projects);
    }
    
    /**
     * Clear all projects.
     * Automatically saves after clearing.
     */
    public void clearProjects() {
        projects.clear();
        saveProjects();
    }
}
