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
     * Load projects from console-projects.json file.
     */
    public void loadProjects() {
        projects.clear();
        try {
            // Try current directory first
            Path projectsPath = Paths.get(PROJECTS_FILE);
            
            // If not found in current directory, try parent directory
            if (!Files.exists(projectsPath)) {
                projectsPath = Paths.get("..", PROJECTS_FILE);
            }
            
            // If still not found, start with empty list
            if (!Files.exists(projectsPath)) {
                System.out.println("Projects file not found: " + PROJECTS_FILE + ", starting with empty list.");
                return;
            }
            
            String jsonContent = Files.readString(projectsPath);
            Object parsed = Json.parse(jsonContent);
            
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> projectsMap = (Map<String, Object>) parsed;
                Object projectsArray = projectsMap.get("projects");
                
                if (projectsArray instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> projectsList = (List<Object>) projectsArray;
                    
                    for (Object projectObj : projectsList) {
                        if (projectObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> projectMap = (Map<String, Object>) projectObj;
                            String name = (String) projectMap.get("name");
                            String path = (String) projectMap.get("path");
                            
                            if (name != null && path != null) {
                                projects.add(new ProjectEntry(name, path));
                            }
                        }
                    }
                    System.out.println("Loaded " + projects.size() + " projects from " + PROJECTS_FILE);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + PROJECTS_FILE + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing " + PROJECTS_FILE + ": " + e.getMessage());
        }
    }
    
    /**
     * Save projects to console-projects.json file.
     */
    public void saveProjects() {
        try {
            // Build JSON structure
            Map<String, Object> root = new LinkedHashMap<>();
            List<Map<String, String>> projectsList = new ArrayList<>();
            
            for (ProjectEntry entry : projects) {
                Map<String, String> projectMap = new LinkedHashMap<>();
                projectMap.put("name", entry.getName());
                projectMap.put("path", entry.getPath());
                projectsList.add(projectMap);
            }
            
            root.put("projects", projectsList);
            
            // Convert to JSON string
            String jsonContent = Json.prettyJson(root);
            
            // Write to file in current directory
            Path projectsPath = Paths.get(PROJECTS_FILE);
            Files.writeString(projectsPath, jsonContent);
            
            System.out.println("Saved " + projects.size() + " projects to " + PROJECTS_FILE);
        } catch (IOException e) {
            System.err.println("Error writing " + PROJECTS_FILE + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error saving projects: " + e.getMessage());
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
