package com.eb.ui.ebs;

import javafx.application.Platform;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors project directories for file system changes using Java NIO.2 WatchService API.
 * Automatically refreshes the project tree view when files or directories are added, modified, or deleted.
 * 
 * Features:
 * - Monitors multiple project directories simultaneously
 * - Detects file creation, deletion, and modification
 * - Detects directory creation and deletion
 * - Runs on a background thread to avoid blocking UI
 * - Thread-safe project registration and unregistration
 * - Automatic tree refresh on JavaFX thread
 * 
 * @author Earl Bosch
 */
public class ProjectFileWatcher {
    
    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeyToPath;
    private final Map<Path, WatchKey> pathToWatchKey;
    private final ProjectTreeView treeView;
    private final Thread watcherThread;
    private final AtomicBoolean running;
    
    /**
     * Creates a new file watcher for monitoring project directories.
     * 
     * @param treeView The tree view to refresh when changes are detected
     * @throws IOException if the watch service cannot be created
     */
    public ProjectFileWatcher(ProjectTreeView treeView) throws IOException {
        this.treeView = treeView;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchKeyToPath = new ConcurrentHashMap<>();
        this.pathToWatchKey = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
        
        // Create background thread for watching
        this.watcherThread = new Thread(this::watchLoop, "ProjectFileWatcher");
        this.watcherThread.setDaemon(true);
    }
    
    /**
     * Starts the file watcher thread.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            watcherThread.start();
        }
    }
    
    /**
     * Stops the file watcher thread and releases resources.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                watchService.close();
            } catch (IOException e) {
                System.err.println("Error closing watch service: " + e.getMessage());
            }
        }
    }
    
    /**
     * Registers a project directory for monitoring.
     * Also registers all subdirectories recursively.
     * 
     * @param projectPath The path to the project directory
     */
    public void registerProject(Path projectPath) {
        if (!Files.isDirectory(projectPath)) {
            return;
        }
        
        try {
            registerDirectory(projectPath);
            
            // Register all subdirectories recursively
            Files.walk(projectPath)
                .filter(Files::isDirectory)
                .filter(path -> !path.equals(projectPath))
                .forEach(this::registerDirectory);
                
        } catch (IOException e) {
            System.err.println("Error registering project for watching: " + e.getMessage());
        }
    }
    
    /**
     * Unregisters a project directory from monitoring.
     * 
     * @param projectPath The path to the project directory
     */
    public void unregisterProject(Path projectPath) {
        // Find and cancel all watch keys for this project and its subdirectories
        List<Path> toRemove = new ArrayList<>();
        
        for (Map.Entry<Path, WatchKey> entry : pathToWatchKey.entrySet()) {
            Path watchedPath = entry.getKey();
            if (watchedPath.startsWith(projectPath)) {
                WatchKey key = entry.getValue();
                key.cancel();
                watchKeyToPath.remove(key);
                toRemove.add(watchedPath);
            }
        }
        
        toRemove.forEach(pathToWatchKey::remove);
    }
    
    /**
     * Registers a single directory for monitoring.
     * 
     * @param directory The directory to monitor
     */
    private void registerDirectory(Path directory) {
        try {
            // Register for all types of events
            WatchKey key = directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            );
            
            watchKeyToPath.put(key, directory);
            pathToWatchKey.put(directory, key);
            
        } catch (IOException e) {
            System.err.println("Error registering directory for watching: " + directory + " - " + e.getMessage());
        }
    }
    
    /**
     * Main watch loop that processes file system events.
     */
    private void watchLoop() {
        while (running.get()) {
            WatchKey key;
            try {
                // Wait for events (blocking)
                key = watchService.take();
            } catch (InterruptedException e) {
                // Thread interrupted, exit
                break;
            } catch (ClosedWatchServiceException e) {
                // Watch service closed, exit
                break;
            }
            
            Path directory = watchKeyToPath.get(key);
            if (directory == null) {
                key.reset();
                continue;
            }
            
            boolean shouldRefresh = false;
            boolean hasNewDirectory = false;
            
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                // Skip overflow events
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path filename = pathEvent.context();
                Path fullPath = directory.resolve(filename);
                
                // Mark that we should refresh the tree
                shouldRefresh = true;
                
                // If a new directory was created, register it for watching
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    registerDirectory(fullPath);
                    hasNewDirectory = true;
                }
                
                // If a directory was deleted, unregister it
                if (kind == StandardWatchEventKinds.ENTRY_DELETE && pathToWatchKey.containsKey(fullPath)) {
                    WatchKey deletedKey = pathToWatchKey.remove(fullPath);
                    if (deletedKey != null) {
                        deletedKey.cancel();
                        watchKeyToPath.remove(deletedKey);
                    }
                }
            }
            
            // Reset the key - important!
            boolean valid = key.reset();
            if (!valid) {
                // Key is no longer valid, remove it
                watchKeyToPath.remove(key);
                pathToWatchKey.remove(directory);
            }
            
            // Refresh the tree view on the JavaFX thread
            if (shouldRefresh) {
                final boolean newDir = hasNewDirectory;
                Platform.runLater(() -> {
                    try {
                        treeView.refreshProjects();
                        if (newDir) {
                            // If new directories were added, we might need to expand them
                            treeView.expandAllProjects();
                        }
                    } catch (Exception e) {
                        System.err.println("Error refreshing tree view: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * Returns whether the watcher is currently running.
     * 
     * @return true if the watcher is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
}
