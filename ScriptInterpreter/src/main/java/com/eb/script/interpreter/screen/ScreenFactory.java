package com.eb.script.interpreter.screen;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.interpreter.builtins.BuiltinsFile;
import com.eb.script.file.FileData;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.screen.AreaDefinition.AreaType;
import com.eb.script.interpreter.screen.data.DataBindingManager;
import com.eb.script.interpreter.screen.data.VarRefResolver;
import com.eb.script.interpreter.screen.display.ControlListenerFactory;
import com.eb.script.interpreter.screen.display.ControlUpdater;
import com.eb.script.interpreter.screen.display.DisplayChangeHandler;
import com.eb.script.interpreter.screen.display.DisplayValidator;
import com.eb.script.json.Json;
import com.eb.script.json.JsonSchema;
import com.eb.script.json.JsonValidate;
import com.eb.script.token.DataType;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.util.*;

/**
 * Factory class for creating complete JavaFX screens/windows from
 * AreaDefinitions. This factory uses AreaContainerFactory and AreaItemFactory
 * to create a fully assembled screen. Includes JSON Schema validation for
 * screen definitions.
 */
public class ScreenFactory {
    
    // Debug panel layout constants (increased by 10% from original values: 250->275, 320->352, 400->440)
    private static final int DEBUG_PANEL_MIN_WIDTH = 275;
    private static final int DEBUG_PANEL_PREF_WIDTH = 352;
    private static final int DEBUG_PANEL_MAX_WIDTH = 440;
    private static final int DEBUG_AREA_INDENT_PIXELS = 15;
    private static final int DEBUG_MAX_CODE_DISPLAY_LENGTH = 50;
    private static final int DEBUG_TOOLTIP_MAX_WIDTH = 400;
    // Key/name field minimum width - no maximum to allow expansion for long names
    private static final int DEBUG_ITEM_NAME_MIN_WIDTH = 60;
    
    // Debug panel divider position constraints (min 50%, max 90% for main content)
    private static final double DEBUG_DIVIDER_MIN_POSITION = 0.5;
    private static final double DEBUG_DIVIDER_MAX_POSITION = 0.9;
    
    // Debug row styling constants
    private static final String DEBUG_ROW_HOVER_STYLE = "-fx-background-color: #d0e8ff; -fx-cursor: hand;";
    private static final String DEBUG_ROW_CLICK_STYLE = "-fx-background-color: #a0d0a0; -fx-cursor: hand;";
    
    // Change indicator for debug panel items
    // Using simple asterisk (*) for maximum font compatibility
    private static final String CHANGE_INDICATOR_EMOJI = "*";
    private static final String DEBUG_ITEM_NAME_BASE_STYLE = "-fx-alignment: CENTER-LEFT; -fx-font-weight: bold;";
    private static final String DEBUG_ITEM_NAME_CHANGED_STYLE = DEBUG_ITEM_NAME_BASE_STYLE + " -fx-text-fill: #cc5500;";

    /**
     * Functional interface for executing onClick EBS code
     */
    @FunctionalInterface
    public interface OnClickHandler {

        void execute(String ebsCode) throws InterpreterError;
        
        /**
         * Default method to execute EBS code and return the result.
         * Can be overridden for custom implementations.
         * 
         * @param ebsCode The EBS code to execute
         * @return The result of executing the code (null by default)
         * @throws InterpreterError If execution fails
         */
        default Object executeWithReturn(String ebsCode) throws InterpreterError {
            execute(ebsCode);
            return null;
        }
        
        /**
         * Execute EBS code directly on the calling thread (e.g., JavaFX thread).
         * This avoids deadlocks when the code needs to show dialogs.
         * Implementations must override this method if they use a separate thread
         * for code execution (e.g., screen thread via dispatchSync).
         * 
         * @param ebsCode The EBS code to execute
         * @throws InterpreterError If execution fails
         */
        default void executeDirect(String ebsCode) throws InterpreterError {
            // Default implementation calls execute() which may cause deadlock
            // if the implementation dispatches to a separate thread.
            // Implementations that use separate threads should override this method.
            execute(ebsCode);
        }
    }

    private static Map<String, Object> screenSchema;
    private static Map<String, Object> areaSchema;
    private static Map<String, Object> displayMetadataSchema;
    
    // Debug mode flag - per-thread (per EBS tab), toggleable with Ctrl+D
    // Using InheritableThreadLocal so child threads (like interpreter threads) inherit the debug state
    private static final InheritableThreadLocal<Boolean> debugMode = new InheritableThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    
    // Map to store debug panels for each screen (screenName -> debugPanel)
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.control.ScrollPane> screenDebugPanels = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store the root BorderPane for each screen (screenName -> BorderPane) for debug panel toggling
    private static final java.util.concurrent.ConcurrentHashMap<String, BorderPane> screenRootPanes = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store original window widths before debug panel expansion (screenName -> originalWidth)
    private static final java.util.concurrent.ConcurrentHashMap<String, Double> screenOriginalWidths = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store the original center content of each screen (screenName -> centerContent) for debug panel toggling
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.Node> screenCenterContents = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store the SplitPane for each screen when debug panel is shown (screenName -> SplitPane)
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.control.SplitPane> screenDebugSplitPanes = new java.util.concurrent.ConcurrentHashMap<>();
    // Map to store event counts per item.eventType (e.g., "fileTree.onChange" -> count)
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> eventCounts = new java.util.concurrent.ConcurrentHashMap<>();
    // Map to store screenshot sequence numbers per screen (screenName -> sequence number)
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> screenshotCounters = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store debug panel event count labels for dynamic refresh (key -> label)
    // Key format: "screenName.itemName.eventType"
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.control.Label> eventCountLabels = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store debug panel status labels for real-time status updates (screenName -> label)
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.control.Label> debugStatusLabels = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store changed item varNames per screen for real-time debug panel updates
    // Key format: "screenName" -> Set of changed varNames
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.Set<String>> changedItems = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map to store debug panel items TableView for each screen (screenName -> TableView)
    private static final java.util.concurrent.ConcurrentHashMap<String, javafx.scene.control.TableView<String[]>> debugItemsTables = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Increment and return the event count for a specific item.eventType combination.
     * Used for debugging to track how many times each event fires.
     * Also updates the corresponding label in the debug panel if it exists.
     * 
     * @param screenName The screen name
     * @param itemName The item name (e.g., "fileTree")
     * @param eventType The event type (e.g., "onChange", "onClick")
     * @return The new count after incrementing
     */
    public static int incrementEventCount(String screenName, String itemName, String eventType) {
        String key = (screenName + "." + itemName + "." + eventType).toLowerCase();
        int newCount = eventCounts.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicInteger(0)).incrementAndGet();
        
        // Update the debug panel label if it exists
        javafx.scene.control.Label countLabel = eventCountLabels.get(key);
        if (countLabel != null) {
            String countText = " [" + newCount + "]";
            // Update on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                // Update just the count portion of the label text
                String currentText = countLabel.getText();
                // Format is ".eventType [count]: code..." - we need to update the [count] portion
                int bracketStart = currentText.indexOf('[');
                int bracketEnd = currentText.indexOf(']');
                if (bracketStart >= 0 && bracketEnd > bracketStart) {
                    String newText = currentText.substring(0, bracketStart) + "[" + newCount + "]" + currentText.substring(bracketEnd + 1);
                    countLabel.setText(newText);
                } else if (bracketStart < 0) {
                    // No bracket yet, find the colon and insert count before it
                    int colonPos = currentText.indexOf(':');
                    if (colonPos >= 0) {
                        String newText = currentText.substring(0, colonPos) + " [" + newCount + "]" + currentText.substring(colonPos);
                        countLabel.setText(newText);
                    }
                }
            });
        }
        
        return newCount;
    }
    
    /**
     * Get the current event count for a specific item.eventType combination.
     * 
     * @param screenName The screen name
     * @param itemName The item name
     * @param eventType The event type
     * @return The current count (0 if never fired)
     */
    public static int getEventCount(String screenName, String itemName, String eventType) {
        String key = (screenName + "." + itemName + "." + eventType).toLowerCase();
        java.util.concurrent.atomic.AtomicInteger count = eventCounts.get(key);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Reset all event counts for a screen (called when screen is closed).
     * 
     * @param screenName The screen name
     */
    public static void resetEventCounts(String screenName) {
        String prefix = (screenName + ".").toLowerCase();
        eventCounts.keySet().removeIf(key -> key.startsWith(prefix));
        // Also clean up event count labels
        eventCountLabels.keySet().removeIf(key -> key.startsWith(prefix));
        // Clean up debug status label and changed items tracking for this screen
        debugStatusLabels.remove(screenName.toLowerCase());
        changedItems.remove(screenName.toLowerCase());
        debugItemsTables.remove(screenName.toLowerCase());
    }
    
    /**
     * Updates the debug panel status label to reflect the current screen status.
     * This method is called when the screen status changes to provide real-time updates.
     * 
     * @param screenName The screen name
     * @param status The new screen status
     */
    public static void updateDebugStatusLabel(String screenName, ScreenStatus status) {
        if (screenName == null || status == null) {
            return;
        }
        
        javafx.scene.control.Label statusLabel = debugStatusLabels.get(screenName.toLowerCase());
        if (statusLabel != null) {
            String statusEmoji = status == ScreenStatus.ERROR ? "\u274C" : 
                                 status == ScreenStatus.CHANGED ? CHANGE_INDICATOR_EMOJI : "\u2713";
            String statusText = statusEmoji + " " + status.name();
            String color = status == ScreenStatus.ERROR ? "#cc0000" : 
                          status == ScreenStatus.CHANGED ? "#cc6600" : "#006600";
            
            // Update on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText(statusText);
                statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
            });
        }
    }
    
    /**
     * Marks an item as changed and updates the debug panel items table in real-time.
     * This is called when a control with varRef is modified.
     * 
     * @param screenName The screen name
     * @param varName The varRef/variable name of the changed item
     */
    public static void markItemChanged(String screenName, String varName) {
        if (screenName == null || varName == null) {
            return;
        }
        
        String key = screenName.toLowerCase();
        
        // Track the changed item
        changedItems.computeIfAbsent(key, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(varName);
        
        // Update the items table if visible - find the row and update its display text
        javafx.scene.control.TableView<String[]> itemsTable = debugItemsTables.get(key);
        if (itemsTable != null) {
            // Update on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                // Find the row with this varRef and update its display text
                for (String[] row : itemsTable.getItems()) {
                    if (row.length >= 5 && varName.equals(row[2])) {
                        // row[2] is varRef, row[3] is itemType, row[4] is rawName
                        String typeIcon = getItemTypeIcon(row[3]);
                        String rawName = row[4];
                        // Format: type icon, then change icon, then name (e.g., "🎨 ⚠️ background")
                        row[0] = typeIcon + " " + CHANGE_INDICATOR_EMOJI + " " + rawName;
                        break;
                    }
                }
                itemsTable.refresh();
            });
        }
    }
    
    /**
     * Checks if an item has been changed in the debug panel.
     * 
     * @param screenName The screen name
     * @param varName The varRef/variable name to check
     * @return true if the item has been changed
     */
    public static boolean isItemChanged(String screenName, String varName) {
        if (screenName == null || varName == null) {
            return false;
        }
        java.util.Set<String> items = changedItems.get(screenName.toLowerCase());
        return items != null && items.contains(varName);
    }
    
    /**
     * Clears all changed item markers for a screen.
     * 
     * @param screenName The screen name
     */
    public static void clearChangedItems(String screenName) {
        if (screenName != null) {
            String key = screenName.toLowerCase();
            changedItems.remove(key);
            
            // Also update the items table to remove ⚠️ prefixes
            javafx.scene.control.TableView<String[]> itemsTable = debugItemsTables.get(key);
            if (itemsTable != null) {
                // Reset all row display texts to remove ⚠️ prefix
                for (String[] row : itemsTable.getItems()) {
                    if (row.length >= 5) {
                        String typeIcon = getItemTypeIcon(row[3]);
                        String rawName = row[4];
                        row[0] = typeIcon + " " + rawName;
                    }
                }
                itemsTable.refresh();
            }
        }
    }
    
    /**
     * Refresh the debug panel items table for a screen if it's currently open.
     * This updates the items table to show the current state of items.
     * 
     * @param screenName The screen name
     * @param context The interpreter context
     */
    public static void refreshDebugPanelIfOpen(String screenName, InterpreterContext context) {
        if (screenName == null || context == null) {
            return;
        }
        String lowerScreenName = screenName.toLowerCase();
        
        // Check if this screen has a debug panel items table
        @SuppressWarnings("unchecked")
        javafx.scene.control.TableView<String[]> itemsTable = 
            (javafx.scene.control.TableView<String[]>) debugItemsTables.get(lowerScreenName);
        
        if (itemsTable != null) {
            // Force a complete refresh of the table
            Platform.runLater(() -> {
                itemsTable.refresh();
            });
        }
    }

    static {
        try {
            // Load schemas from resources
            FileData screenSchemaFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/screen-definition.json"));
            FileData areaSchemaFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/area-definition.json"));
            FileData displayMetadataFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/display-metadata.json"));

            if (screenSchemaFile != null) {
                screenSchema = (Map<String, Object>) Json.parse(screenSchemaFile.stringData);
                JsonValidate.registerSchema("sys.screenSchema", screenSchema);
            }
            if (areaSchemaFile != null) {
                areaSchema = (Map<String, Object>) Json.parse(areaSchemaFile.stringData);
                JsonValidate.registerSchema("sys.areaSchema", areaSchema);
            }
            if (displayMetadataFile != null) {
                displayMetadataSchema = (Map<String, Object>) Json.parse(displayMetadataFile.stringData);
                JsonValidate.registerSchema("sys.displayMetadataSchema", displayMetadataSchema);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load JSON schemas: " + e.getMessage());
        }
    }
    
    /**
     * Toggle debug mode on or off for the current thread.
     * Can be called from outside (e.g., from EBS tabs) to enable debug mode
     * even before running any scripts.
     * 
     * @param outputArea The output area to display the toggle message (optional)
     * @return The new debug mode state
     */
    public static boolean toggleDebugModeForThread(ScriptArea outputArea) {
        boolean newDebugMode = !debugMode.get();
        debugMode.set(newDebugMode);
        
        String message = "DEBUG MODE: " + (newDebugMode ? "ENABLED" : "DISABLED") + " (Thread: " + Thread.currentThread().getName() + ")";
        
        // Print to console
        System.out.println("=".repeat(80));
        System.out.println(message);
        System.out.println("=".repeat(80));
        
        // Show message in output area if available
        if (outputArea != null) {
            Platform.runLater(() -> {
                if (newDebugMode) {
                    outputArea.printlnInfo(message);
                } else {
                    outputArea.printlnWarn(message);
                }
            });
        }
        
        return newDebugMode;
    }
    
    /**
     * Toggle debug mode on or off for the current thread.
     * 
     * @param screenName The name of the screen (optional, for status bar message)
     * @param context The interpreter context (optional, for status bar access)
     */
    private static void toggleDebugMode(String screenName, InterpreterContext context) {
        boolean newDebugMode = !debugMode.get();
        debugMode.set(newDebugMode);
        
        String message = "DEBUG MODE: " + (newDebugMode ? "ENABLED" : "DISABLED") + " (Thread: " + Thread.currentThread().getName() + ")";
        
        // Print to console
        System.out.println("=".repeat(80));
        System.out.println(message);
        System.out.println("=".repeat(80));
        
        // Show message in status bar if available
        if (context != null && screenName != null) {
            com.eb.ui.ebs.StatusBar statusBar = context.getScreenStatusBars().get(screenName);
            if (statusBar != null) {
                Platform.runLater(() -> {
                    statusBar.setMessage(message);
                });
            }
            
            // Show or hide the debug panel
            Platform.runLater(() -> {
                toggleDebugPanel(screenName, context, newDebugMode);
            });
        }
    }
    
    /**
     * Toggle the debug panel visibility for a screen.
     * Shows a scrollable panel on the right side with all screen variables and values.
     * Uses a SplitPane with a draggable divider to allow resizing the debug panel.
     * Also expands the window width when debug is activated and restores it when deactivated.
     * 
     * @param screenName The name of the screen
     * @param context The interpreter context
     * @param show Whether to show or hide the debug panel
     */
    private static void toggleDebugPanel(String screenName, InterpreterContext context, boolean show) {
        BorderPane rootPane = screenRootPanes.get(screenName.toLowerCase());
        if (rootPane == null) {
            return;
        }
        
        // Get the Stage (window) for this screen to adjust its width
        javafx.stage.Stage stage = context.getScreens().get(screenName.toLowerCase());
        
        if (show) {
            // Store the original width before expanding
            if (stage != null && !screenOriginalWidths.containsKey(screenName.toLowerCase())) {
                screenOriginalWidths.put(screenName.toLowerCase(), stage.getWidth());
            }
            
            // Store the original center content before replacing with SplitPane
            javafx.scene.Node centerContent = rootPane.getCenter();
            if (centerContent != null && !screenCenterContents.containsKey(screenName.toLowerCase())) {
                screenCenterContents.put(screenName.toLowerCase(), centerContent);
            }
            
            // Create or update the debug panel
            javafx.scene.control.ScrollPane debugPanel = createDebugPanel(screenName, context);
            screenDebugPanels.put(screenName.toLowerCase(), debugPanel);
            
            // Create a horizontal SplitPane with main content on left and debug panel on right
            javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
            splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            
            // Add the original center content and the debug panel to the SplitPane
            javafx.scene.Node originalCenter = screenCenterContents.get(screenName.toLowerCase());
            if (originalCenter != null) {
                splitPane.getItems().addAll(originalCenter, debugPanel);
                
                // Store the SplitPane reference
                screenDebugSplitPanes.put(screenName.toLowerCase(), splitPane);
                
                // Expand the window width to accommodate the debug panel FIRST
                if (stage != null) {
                    double currentWidth = stage.getWidth();
                    double newWidth = currentWidth + DEBUG_PANEL_PREF_WIDTH;
                    stage.setWidth(newWidth);
                    
                    // Calculate and set the divider position immediately
                    // This prevents the initial "wide" appearance before shrinking
                    double dividerPos = 1.0 - ((double) DEBUG_PANEL_PREF_WIDTH / newWidth);
                    // Clamp the divider position to ensure main content has reasonable space
                    dividerPos = Math.max(DEBUG_DIVIDER_MIN_POSITION, Math.min(DEBUG_DIVIDER_MAX_POSITION, dividerPos));
                    splitPane.setDividerPositions(dividerPos);
                }
                
                // Replace the center content with the SplitPane AFTER setting width and divider
                rootPane.setCenter(splitPane);
            } else {
                // No original center content available - don't show split pane with only debug panel
                // Just show debug panel in the right side of the BorderPane as a fallback
                rootPane.setRight(debugPanel);
                
                // Expand the window width to accommodate the debug panel
                if (stage != null) {
                    double currentWidth = stage.getWidth();
                    double newWidth = currentWidth + DEBUG_PANEL_PREF_WIDTH;
                    stage.setWidth(newWidth);
                }
            }
        } else {
            // Restore the original center content if we used a SplitPane
            javafx.scene.Node originalCenter = screenCenterContents.remove(screenName.toLowerCase());
            if (originalCenter != null) {
                rootPane.setCenter(originalCenter);
            }
            
            // Clean up the SplitPane reference
            screenDebugSplitPanes.remove(screenName.toLowerCase());
            
            // Also clear BorderPane.setRight() in case we used the fallback approach
            rootPane.setRight(null);
            
            // Remove the debug panel reference, status label, and changed items tracking
            screenDebugPanels.remove(screenName.toLowerCase());
            debugStatusLabels.remove(screenName.toLowerCase());
            changedItems.remove(screenName.toLowerCase());
            debugItemsTables.remove(screenName.toLowerCase());
            
            // Restore the original window width
            if (stage != null) {
                Double originalWidth = screenOriginalWidths.remove(screenName.toLowerCase());
                if (originalWidth != null) {
                    stage.setWidth(originalWidth);
                }
            }
        }
    }
    
    /**
     * Create the debug panel showing all screen variables and their values.
     * 
     * @param screenName The name of the screen
     * @param context The interpreter context
     * @return A ScrollPane containing the debug information
     */
    private static javafx.scene.control.ScrollPane createDebugPanel(String screenName, InterpreterContext context) {
        VBox mainContent = new VBox(5);
        mainContent.setPadding(new Insets(10));
        mainContent.setStyle("-fx-background-color: #f5f5f5;");
        
        // Header using BorderPane for proper positioning - close button in absolute top right
        BorderPane headerPane = new BorderPane();
        headerPane.setPadding(new Insets(0, 0, 5, 0));
        
        // Left side: Title and copy button
        HBox leftHeader = new HBox(5);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        
        // Title
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Debug: " + screenName);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        
        // Copy button
        javafx.scene.control.Button copyButton = new javafx.scene.control.Button("📋");
        copyButton.setStyle("-fx-font-size: 12px; -fx-padding: 2 6 2 6; -fx-background-color: #e0e0e0; -fx-cursor: hand;");
        javafx.scene.control.Tooltip copyTooltip = new javafx.scene.control.Tooltip("Copy all to clipboard");
        copyTooltip.setStyle("-fx-font-size: 12px;");
        copyTooltip.setShowDelay(javafx.util.Duration.millis(500));
        copyButton.setTooltip(copyTooltip);
        
        leftHeader.getChildren().addAll(titleLabel, copyButton);
        headerPane.setLeft(leftHeader);
        
        // Close button - positioned in absolute top right corner
        javafx.scene.control.Button closeButton = new javafx.scene.control.Button("✕");
        closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 2 8 2 8; -fx-background-color: #ff6666; -fx-cursor: hand; -fx-text-fill: white; -fx-font-weight: bold;");
        javafx.scene.control.Tooltip closeTooltip = new javafx.scene.control.Tooltip("Close debug panel (Ctrl+D)");
        closeTooltip.setStyle("-fx-font-size: 12px;");
        closeTooltip.setShowDelay(javafx.util.Duration.millis(500));
        closeButton.setTooltip(closeTooltip);
        BorderPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        headerPane.setRight(closeButton);
        
        // Close button action - toggle debug mode off
        closeButton.setOnAction(e -> {
            toggleDebugMode(screenName, context);
        });
        
        // Get screen variables and types for copy action
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
        java.util.concurrent.ConcurrentHashMap<String, DataType> screenVarTypes = context.getScreenVarTypes(screenName);
        Map<String, AreaItem> screenAreaItems = context.getScreenAreaItems(screenName);
        
        copyButton.setOnAction(e -> {
            String clipboardText = formatAllForClipboard(screenName, screenVars, screenVarTypes, screenAreaItems, context);
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
            clipboardContent.putString(clipboardText);
            clipboard.setContent(clipboardContent);
            
            // Show brief feedback
            copyButton.setText("✓");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> copyButton.setText("📋"));
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();
        });
        
        mainContent.getChildren().add(headerPane);
        
        // Separator
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        mainContent.getChildren().add(separator);
        
        // === SCREEN STATUS & CONFIG SECTION ===
        VBox statusSection = createScreenStatusSection(screenName, context);
        mainContent.getChildren().add(statusSection);
        
        // === VARIABLES SECTION - Using TableView for proper alignment ===
        VBox varsSection = new VBox(3);
        varsSection.setPadding(new Insets(5));
        varsSection.setStyle("-fx-background-color: #f8f8f8;");
        
        javafx.scene.control.Label varsHeader = new javafx.scene.control.Label("📊 Variables");
        varsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #444;");
        varsSection.getChildren().add(varsHeader);
        
        if (screenVars != null && !screenVars.isEmpty()) {
            // Create TableView for variables
            javafx.scene.control.TableView<String[]> varsTable = new javafx.scene.control.TableView<>();
            varsTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            varsTable.setStyle("-fx-background-color: transparent;");
            
            // Name column (50%) - with tooltip showing variable name and type
            // Array format: [name, valueStr, typeStr]
            javafx.scene.control.TableColumn<String[], String> nameCol = new javafx.scene.control.TableColumn<>("Name");
            nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
            nameCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold;");
            nameCol.prefWidthProperty().bind(varsTable.widthProperty().multiply(0.5));
            nameCol.setCellFactory(col -> new javafx.scene.control.TableCell<String[], String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        // Get type string from row data
                        int rowIndex = getIndex();
                        String typeStr = "unknown";
                        if (rowIndex >= 0 && rowIndex < getTableView().getItems().size()) {
                            String[] rowData = getTableView().getItems().get(rowIndex);
                            if (rowData.length >= 3 && rowData[2] != null) {
                                typeStr = rowData[2];
                            }
                        }
                        StringBuilder tooltipText = new StringBuilder();
                        tooltipText.append("Variable: ").append(item);
                        tooltipText.append("\nType: ").append(typeStr);
                        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText.toString());
                        tooltip.setShowDelay(javafx.util.Duration.millis(500));
                        tooltip.setMaxWidth(DEBUG_TOOLTIP_MAX_WIDTH);
                        tooltip.setWrapText(true);
                        setTooltip(tooltip);
                    }
                }
            });
            
            // Value column (50%) - with tooltip showing full value
            javafx.scene.control.TableColumn<String[], String> valueCol = new javafx.scene.control.TableColumn<>("Value");
            valueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
            valueCol.setStyle("-fx-alignment: CENTER-LEFT;");
            valueCol.prefWidthProperty().bind(varsTable.widthProperty().multiply(0.5));
            valueCol.setCellFactory(col -> new javafx.scene.control.TableCell<String[], String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(item);
                        tooltip.setShowDelay(javafx.util.Duration.millis(500));
                        tooltip.setMaxWidth(DEBUG_TOOLTIP_MAX_WIDTH);
                        tooltip.setWrapText(true);
                        setTooltip(tooltip);
                    }
                }
            });
            
            varsTable.getColumns().add(nameCol);
            varsTable.getColumns().add(valueCol);
            
            // Populate data - Array format: [name, valueStr, typeStr]
            java.util.List<String> sortedKeys = new java.util.ArrayList<>(screenVars.keySet());
            java.util.Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);
            
            for (String key : sortedKeys) {
                Object value = screenVars.get(key);
                String valueStr = formatValue(value);
                DataType dataType = screenVarTypes != null ? screenVarTypes.get(key) : null;
                String typeStr = getDetailedTypeString(dataType, value);
                varsTable.getItems().add(new String[]{key, valueStr, typeStr});
            }
            
            // Allow table to expand to fill available space
            varsTable.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(varsTable, Priority.ALWAYS);
            
            // Add row click handler to copy variable to clipboard
            varsTable.setRowFactory(tv -> {
                javafx.scene.control.TableRow<String[]> row = new javafx.scene.control.TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty()) {
                        String[] rowData = row.getItem();
                        String name = rowData[0];
                        String value = rowData[1];
                        String clipboardText = name + " = " + value;
                        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(clipboardText);
                        clipboard.setContent(content);
                        // Show feedback in row
                        String originalStyle = row.getStyle();
                        row.setStyle("-fx-background-color: #90EE90;");
                        new Thread(() -> {
                            try {
                                Thread.sleep(300);
                                Platform.runLater(() -> row.setStyle(originalStyle));
                            } catch (InterruptedException ex) {
                                // Ignore
                            }
                        }).start();
                        // Also show feedback via status bar if available
                        com.eb.ui.ebs.StatusBar statusBar = context.getScreenStatusBars().get(screenName);
                        if (statusBar != null) {
                            statusBar.setMessage("Copied to clipboard: " + name);
                        }
                    }
                });
                return row;
            });
            
            varsSection.getChildren().add(varsTable);
        } else {
            javafx.scene.control.Label noVarsLabel = new javafx.scene.control.Label("No variables defined");
            noVarsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
            varsSection.getChildren().add(noVarsLabel);
        }
        
        javafx.scene.control.ScrollPane varsScrollPane = new javafx.scene.control.ScrollPane(varsSection);
        varsScrollPane.setFitToWidth(true);
        varsScrollPane.setFitToHeight(true);
        varsScrollPane.setStyle("-fx-background-color: transparent;");
        varsScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        varsScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(varsScrollPane, Priority.ALWAYS);
        
        // === SCREEN ITEMS SECTION - Using TableView for proper alignment ===
        VBox itemsSection = new VBox(3);
        itemsSection.setPadding(new Insets(5));
        itemsSection.setStyle("-fx-background-color: #f0f5f0;");
        
        javafx.scene.control.Label itemsHeader = new javafx.scene.control.Label("🖼️ Screen Items");
        itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #444;");
        itemsSection.getChildren().add(itemsHeader);
        
        if (screenAreaItems != null && !screenAreaItems.isEmpty()) {
            // Create TableView for screen items
            // Each row is [displayName, value, varRef] - varRef is used to check if item is changed
            javafx.scene.control.TableView<String[]> itemsTable = new javafx.scene.control.TableView<>();
            itemsTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            itemsTable.setStyle("-fx-background-color: transparent;");
            
            // Store reference for real-time updates
            debugItemsTables.put(screenName.toLowerCase(), itemsTable);
            
            // Name column (50%) - shows type icon and changed indicator if item was modified
            // The display text with icon is pre-computed and stored in data.getValue()[0]
            // Array format: [displayTextWithIcon, value, varRef, itemType, rawName]
            javafx.scene.control.TableColumn<String[], String> itemNameCol = new javafx.scene.control.TableColumn<>("Item");
            itemNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
            itemNameCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold;");
            itemNameCol.prefWidthProperty().bind(itemsTable.widthProperty().multiply(0.5));
            
            // Cell factory to apply dark orange color for changed items (contains ⚠️) and add tooltip
            // Note: Pre-computing display text avoids JavaFX cell factory timing issues
            // where cell factories may be called before data is fully initialized
            // Capture screenName and context for lambda
            final String itemsScreenName = screenName;
            final InterpreterContext itemsContext = context;
            itemNameCol.setCellFactory(col -> new javafx.scene.control.TableCell<String[], String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                        setStyle(DEBUG_ITEM_NAME_BASE_STYLE);
                    } else {
                        setText(item);
                        // Dark orange color for changed items (format: "typeIcon ⚠️ name")
                        boolean isChanged = item.contains(CHANGE_INDICATOR_EMOJI);
                        if (isChanged) {
                            setStyle(DEBUG_ITEM_NAME_CHANGED_STYLE);
                        } else {
                            setStyle(DEBUG_ITEM_NAME_BASE_STYLE);
                        }
                        // Add tooltip with item details from row data
                        // Array format: [displayTextWithIcon, value, varRef, itemType, rawName, parentArea, displayInfo, allKeys]
                        int rowIndex = getIndex();
                        if (rowIndex >= 0 && rowIndex < getTableView().getItems().size()) {
                            String[] rowData = getTableView().getItems().get(rowIndex);
                            String rawName = rowData.length >= 5 ? rowData[4] : item;
                            String itemType = rowData.length >= 4 ? rowData[3] : "unknown";
                            String varRef = rowData.length >= 3 ? rowData[2] : "";
                            String parentArea = rowData.length >= 6 ? rowData[5] : "";
                            String displayInfo = rowData.length >= 7 ? rowData[6] : "";
                            String allKeys = rowData.length >= 8 ? rowData[7] : "";
                            
                            // Determine state
                            String state = "CLEAN";
                            if (isChanged) {
                                state = "CHANGED";
                            }
                            // Check if screen has error status
                            ScreenStatus screenStatus = itemsContext.getScreenStatus(itemsScreenName);
                            if (screenStatus == ScreenStatus.ERROR) {
                                state = "ERROR";
                            }
                            
                            StringBuilder tooltipText = new StringBuilder();
                            tooltipText.append("Item: ").append(rawName);
                            tooltipText.append("\nType: ").append(itemType);
                            if (varRef != null && !varRef.isEmpty()) {
                                tooltipText.append("\nVar: ").append(varRef);
                            }
                            if (parentArea != null && !parentArea.isEmpty()) {
                                // Limit to last two direct parents only
                                String limitedAreaPath = limitAreaPathToTwoLevels(parentArea);
                                tooltipText.append("\nArea: ").append(limitedAreaPath);
                            }
                            tooltipText.append("\nState: ").append(state);
                            // Add all lookup keys if present
                            if (allKeys != null && !allKeys.isEmpty()) {
                                tooltipText.append("\nKeys: ").append(allKeys);
                            }
                            // Add display info if present
                            if (displayInfo != null && !displayInfo.isEmpty()) {
                                tooltipText.append("\n---\n").append(displayInfo);
                            }
                            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText.toString());
                            tooltip.setShowDelay(javafx.util.Duration.millis(500));
                            tooltip.setMaxWidth(DEBUG_TOOLTIP_MAX_WIDTH);
                            tooltip.setWrapText(true);
                            setTooltip(tooltip);
                        }
                    }
                }
            });
            
            // Value column (50%) - with tooltip showing full value
            javafx.scene.control.TableColumn<String[], String> itemValueCol = new javafx.scene.control.TableColumn<>("Value");
            itemValueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
            itemValueCol.setStyle("-fx-alignment: CENTER-LEFT;");
            itemValueCol.prefWidthProperty().bind(itemsTable.widthProperty().multiply(0.5));
            itemValueCol.setCellFactory(col -> new javafx.scene.control.TableCell<String[], String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(item);
                        tooltip.setShowDelay(javafx.util.Duration.millis(500));
                        tooltip.setMaxWidth(DEBUG_TOOLTIP_MAX_WIDTH);
                        tooltip.setWrapText(true);
                        setTooltip(tooltip);
                    }
                }
            });
            
            itemsTable.getColumns().add(itemNameCol);
            itemsTable.getColumns().add(itemValueCol);
            
            // Build a map from item name to area name by iterating through AreaDefinitions
            // This is needed because screenAreaItems doesn't always include area in the key
            Map<String, String> itemToAreaMap = new HashMap<>();
            List<AreaDefinition> areas = context.getScreenAreas(screenName);
            if (areas != null) {
                buildItemToAreaMap(areas, itemToAreaMap, "");
            }
            
            // Populate data - pre-compute display text with icon
            // Array format: [displayTextWithIcon, value, varRef, itemType, rawName, parentArea, displayInfo, allKeys]
            
            // Build a map of unique items to all their associated keys
            // Items can be stored with multiple keys (by name, by varRef, etc.)
            java.util.Map<AreaItem, java.util.List<String>> itemToKeysMap = new java.util.LinkedHashMap<>();
            java.util.List<String> sortedItemKeys = new java.util.ArrayList<>(screenAreaItems.keySet());
            java.util.Collections.sort(sortedItemKeys, String.CASE_INSENSITIVE_ORDER);
            
            for (String key : sortedItemKeys) {
                AreaItem item = screenAreaItems.get(key);
                itemToKeysMap.computeIfAbsent(item, k -> new java.util.ArrayList<>()).add(key);
            }
            
            // Now display each unique item once with all its associated keys
            for (java.util.Map.Entry<AreaItem, java.util.List<String>> entry : itemToKeysMap.entrySet()) {
                AreaItem item = entry.getKey();
                java.util.List<String> allKeys = entry.getValue();
                
                // Use the first key for lookups
                String primaryKey = allKeys.get(0);
                
                String displayName = item.name != null ? item.name : primaryKey;
                String valueStr = getScreenItemValue(primaryKey, item, context, screenName);
                String varRef = item.varRef != null ? item.varRef : "";
                // Get the item type from displayItem or fall back to displayMetadata lookup
                String itemType = getItemType(item, context, screenName);
                
                // Look up parent area from itemToAreaMap first, then try key format
                String parentArea = "";
                String itemNameLower = displayName.toLowerCase();
                if (itemToAreaMap.containsKey(itemNameLower)) {
                    parentArea = itemToAreaMap.get(itemNameLower);
                } else {
                    // Try extracting from key format (setName.itemName)
                    int dotIndex = primaryKey.indexOf('.');
                    if (dotIndex > 0) {
                        parentArea = primaryKey.substring(0, dotIndex);
                    }
                }
                
                // Build display info string with additional displayItem properties
                String displayInfo = buildDisplayItemInfo(item, context, screenName, varRef);
                
                // Format all keys for tooltip display
                String allKeysStr = String.join(", ", allKeys);
                
                // Pre-compute the display text with icon (no ⚠️ since not changed on initial display)
                String typeIcon = getItemTypeIcon(itemType);
                String displayText = typeIcon + " " + displayName;
                itemsTable.getItems().add(new String[]{displayText, valueStr, varRef, itemType, displayName, parentArea, displayInfo, allKeysStr});
            }
            
            // Allow table to expand to fill available space
            itemsTable.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(itemsTable, Priority.ALWAYS);
            
            // Add row click handler to copy item to clipboard
            // Capture screenName and context in final variables for lambda
            final String finalScreenName = screenName;
            final InterpreterContext finalContext = context;
            itemsTable.setRowFactory(tv -> {
                javafx.scene.control.TableRow<String[]> row = new javafx.scene.control.TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty()) {
                        String[] rowData = row.getItem();
                        // Array format: [displayTextWithIcon, value, varRef, itemType, rawName, parentArea, displayInfo, allKeys]
                        String displayText = rowData[0];
                        String value = rowData[1];
                        String varRef = rowData.length > 2 ? rowData[2] : "";
                        String itemType = rowData.length > 3 ? rowData[3] : "unknown";
                        String rawName = rowData.length > 4 ? rowData[4] : displayText;
                        String parentArea = rowData.length > 5 ? rowData[5] : "";
                        String displayInfo = rowData.length > 6 ? rowData[6] : "";
                        String allKeys = rowData.length > 7 ? rowData[7] : "";
                        
                        // Determine state (same logic as tooltip)
                        String state = "CLEAN";
                        boolean isChanged = displayText.contains(CHANGE_INDICATOR_EMOJI);
                        if (isChanged) {
                            state = "CHANGED";
                        }
                        ScreenStatus screenStatus = finalContext.getScreenStatus(finalScreenName);
                        if (screenStatus == ScreenStatus.ERROR) {
                            state = "ERROR";
                        }
                        
                        // Build clipboard content matching the tooltip format exactly
                        StringBuilder clipboardBuilder = new StringBuilder();
                        clipboardBuilder.append("Item: ").append(rawName);
                        clipboardBuilder.append("\nType: ").append(itemType);
                        if (varRef != null && !varRef.isEmpty()) {
                            clipboardBuilder.append("\nVar: ").append(varRef);
                        }
                        if (parentArea != null && !parentArea.isEmpty()) {
                            // Limit to last two direct parents only (same as tooltip)
                            String limitedAreaPath = limitAreaPathToTwoLevels(parentArea);
                            clipboardBuilder.append("\nArea: ").append(limitedAreaPath);
                        }
                        clipboardBuilder.append("\nState: ").append(state);
                        // Add all lookup keys if present
                        if (allKeys != null && !allKeys.isEmpty()) {
                            clipboardBuilder.append("\nKeys: ").append(allKeys);
                        }
                        clipboardBuilder.append("\nValue: ").append(value);
                        // Add display info if present (includes JavaFX info)
                        if (displayInfo != null && !displayInfo.isEmpty()) {
                            clipboardBuilder.append("\n---\n").append(displayInfo);
                        }
                        
                        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(clipboardBuilder.toString());
                        clipboard.setContent(content);
                        // Show feedback in row
                        String originalStyle = row.getStyle();
                        row.setStyle("-fx-background-color: #90EE90;");
                        new Thread(() -> {
                            try {
                                Thread.sleep(300);
                                Platform.runLater(() -> row.setStyle(originalStyle));
                            } catch (InterruptedException ex) {
                                // Ignore
                            }
                        }).start();
                        // Also show feedback via status bar if available
                        com.eb.ui.ebs.StatusBar statusBar = finalContext.getScreenStatusBars().get(finalScreenName);
                        if (statusBar != null) {
                            statusBar.setMessage("Copied to clipboard: " + rawName);
                        }
                    }
                });
                return row;
            });
            
            itemsSection.getChildren().add(itemsTable);
        } else {
            javafx.scene.control.Label noItemsLabel = new javafx.scene.control.Label("No screen items defined");
            noItemsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
            itemsSection.getChildren().add(noItemsLabel);
        }
        
        javafx.scene.control.ScrollPane itemsScrollPane = new javafx.scene.control.ScrollPane(itemsSection);
        itemsScrollPane.setFitToWidth(true);
        itemsScrollPane.setFitToHeight(true);
        itemsScrollPane.setStyle("-fx-background-color: transparent;");
        itemsScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        itemsScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(itemsScrollPane, Priority.ALWAYS);
        
        // === SCREEN AREAS SECTION ===
        VBox areasSection = createScreenAreasSection(screenName, context);
        javafx.scene.control.ScrollPane areasScrollPane = new javafx.scene.control.ScrollPane(areasSection);
        areasScrollPane.setFitToWidth(true);
        areasScrollPane.setFitToHeight(true);
        areasScrollPane.setStyle("-fx-background-color: transparent;");
        areasScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        areasScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(areasScrollPane, Priority.ALWAYS);
        
        // === EVENT HANDLERS SECTION ===
        VBox handlersSection = createEventHandlersSection(screenName, context, screenAreaItems);
        javafx.scene.control.ScrollPane handlersScrollPane = new javafx.scene.control.ScrollPane(handlersSection);
        handlersScrollPane.setFitToWidth(true);
        handlersScrollPane.setFitToHeight(true);
        handlersScrollPane.setStyle("-fx-background-color: transparent;");
        handlersScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        handlersScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(handlersScrollPane, Priority.ALWAYS);
        
        // Create TabPane for organizing sections
        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");
        
        javafx.scene.control.Tab varsTab = new javafx.scene.control.Tab("Vars", varsScrollPane);
        javafx.scene.control.Tab itemsTab = new javafx.scene.control.Tab("Items", itemsScrollPane);
        javafx.scene.control.Tab areasTab = new javafx.scene.control.Tab("Areas", areasScrollPane);
        javafx.scene.control.Tab handlersTab = new javafx.scene.control.Tab("Events", handlersScrollPane);
        
        tabPane.getTabs().addAll(varsTab, itemsTab, areasTab, handlersTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        mainContent.getChildren().add(tabPane);
        
        // Create scrollable container
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefWidth(DEBUG_PANEL_PREF_WIDTH);
        scrollPane.setMinWidth(DEBUG_PANEL_MIN_WIDTH);
        // No max width constraint - allow resizing via SplitPane divider
        scrollPane.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ccc; -fx-border-width: 0 0 0 1;");
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    /**
     * Create the Screen Status & Configuration section showing screen status, errors, 
     * dimensions, thread info, and parent/child relationships.
     * 
     * @param screenName The name of the screen
     * @param context The interpreter context
     * @return A VBox containing the status section
     */
    private static VBox createScreenStatusSection(String screenName, InterpreterContext context) {
        VBox statusSection = new VBox(2);
        statusSection.setPadding(new Insets(5));
        statusSection.setStyle("-fx-background-color: #e8f4f8;");
        
        javafx.scene.control.Label statusHeader = new javafx.scene.control.Label("⚙️ Status & Config");
        statusHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #444;");
        statusSection.getChildren().add(statusHeader);
        
        // Screen status - create row manually to store label reference for real-time updates
        ScreenStatus status = context.getScreenStatus(screenName);
        String statusEmoji = status == ScreenStatus.ERROR ? "\u274C" : 
                             status == ScreenStatus.CHANGED ? CHANGE_INDICATOR_EMOJI : "\u2713";
        String statusColor = status == ScreenStatus.ERROR ? "#cc0000" : 
                            status == ScreenStatus.CHANGED ? "#cc6600" : "#006600";
        javafx.scene.control.Label statusValueLabel = addDebugRowWithLabel(statusSection, "Status", 
                                                        statusEmoji + " " + status.name(), statusColor);
        // Store the label for real-time updates
        debugStatusLabels.put(screenName.toLowerCase(), statusValueLabel);
        
        // Error message if any
        String errorMsg = context.getScreenErrorMessage(screenName);
        if (errorMsg != null && !errorMsg.isEmpty()) {
            addDebugRow(statusSection, "Error", errorMsg, "#cc0000");
        }
        
        // Screen configuration
        ScreenConfig config = context.getScreenConfig(screenName);
        if (config != null) {
            addDebugRow(statusSection, "Title", config.getTitle(), "#333");
            addDebugRow(statusSection, "Size", config.getWidth() + " x " + config.getHeight(), "#333");
        }
        
        // Parent screen relationship
        String parentScreen = context.getScreenParent(screenName);
        if (parentScreen != null) {
            addDebugRow(statusSection, "Parent", parentScreen, "#0066cc");
        }
        
        // Thread info
        Thread screenThread = context.getScreenThreads().get(screenName.toLowerCase());
        if (screenThread != null) {
            String threadState = screenThread.isAlive() ? "🟢 " + screenThread.getName() : "🔴 stopped";
            addDebugRow(statusSection, "Thread", threadState, "#333");
        }
        
        // Event dispatcher info
        ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(screenName);
        if (dispatcher != null) {
            addDebugRow(statusSection, "Dispatcher", dispatcher.isRunning() ? "🟢 running" : "🔴 stopped", "#333");
        }
        
        return statusSection;
    }
    
    /**
     * Create the Screen Areas section showing the area hierarchy with types and item counts.
     * 
     * @param screenName The name of the screen
     * @param context The interpreter context
     * @return A VBox containing the areas section
     */
    private static VBox createScreenAreasSection(String screenName, InterpreterContext context) {
        VBox areasSection = new VBox(3);
        areasSection.setPadding(new Insets(5));
        areasSection.setStyle("-fx-background-color: #f5f0e8;");
        
        javafx.scene.control.Label areasHeader = new javafx.scene.control.Label("📐 Screen Areas");
        areasHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #444;");
        areasSection.getChildren().add(areasHeader);
        
        List<AreaDefinition> screenAreas = context.getScreenAreas(screenName);
        if (screenAreas != null && !screenAreas.isEmpty()) {
            for (AreaDefinition area : screenAreas) {
                addAreaDefinitionToSection(areasSection, area, 0);
            }
        } else {
            javafx.scene.control.Label noAreasLabel = new javafx.scene.control.Label("No areas defined");
            noAreasLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
            areasSection.getChildren().add(noAreasLabel);
        }
        
        return areasSection;
    }
    
    /**
     * Recursively add area definitions to the section with indentation for hierarchy.
     * Clicking on a row copies the area details to clipboard.
     * 
     * @param section The VBox to add the area to
     * @param area The area definition
     * @param depth The indentation depth
     */
    private static void addAreaDefinitionToSection(VBox section, AreaDefinition area, int depth) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 5, 2, 5 + (depth * DEBUG_AREA_INDENT_PIXELS)));
        
        // Area type icon
        String icon = getAreaTypeIcon(area.areaType);
        javafx.scene.control.Label iconLabel = new javafx.scene.control.Label(icon);
        iconLabel.setStyle("-fx-font-size: 11px;");
        
        // Area name and type
        String displayText = area.name + " [" + (area.type != null ? area.type : "pane") + "]";
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(displayText);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #663300;");
        
        // Item count
        int itemCount = area.items != null ? area.items.size() : 0;
        int childCount = area.childAreas != null ? area.childAreas.size() : 0;
        String countText = "";
        if (itemCount > 0) countText += itemCount + " items";
        if (childCount > 0) countText += (countText.isEmpty() ? "" : ", ") + childCount + " children";
        
        javafx.scene.control.Label countLabel = new javafx.scene.control.Label(countText);
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        row.getChildren().addAll(iconLabel, nameLabel, countLabel);
        
        // Build clipboard text with all area details
        StringBuilder clipboardBuilder = new StringBuilder();
        clipboardBuilder.append("Area: ").append(area.name);
        clipboardBuilder.append("\nType: ").append(area.type != null ? area.type : "pane");
        if (area.layout != null) clipboardBuilder.append("\nLayout: ").append(area.layout);
        if (area.spacing != null) clipboardBuilder.append("\nSpacing: ").append(area.spacing);
        if (area.padding != null) clipboardBuilder.append("\nPadding: ").append(area.padding);
        if (area.groupBorder != null) clipboardBuilder.append("\nBorder: ").append(area.groupBorder);
        if (area.style != null) clipboardBuilder.append("\nStyle: ").append(area.style);
        if (itemCount > 0) clipboardBuilder.append("\nItems: ").append(itemCount);
        if (childCount > 0) clipboardBuilder.append("\nChildren: ").append(childCount);
        if (area.gainFocus != null) clipboardBuilder.append("\ngainFocus: ").append(area.gainFocus);
        if (area.lostFocus != null) clipboardBuilder.append("\nlostFocus: ").append(area.lostFocus);
        
        String originalStyle = "-fx-background-color: " + (depth % 2 == 0 ? "#f8f3e8" : "#f0ebd8") + ";";
        row.setStyle(originalStyle);
        makeRowClickable(row, clipboardBuilder.toString(), originalStyle);
        
        section.getChildren().add(row);
        
        // Recursively add child areas
        if (area.childAreas != null) {
            for (AreaDefinition childArea : area.childAreas) {
                addAreaDefinitionToSection(section, childArea, depth + 1);
            }
        }
    }
    
    /**
     * Get an icon for the area type.
     */
    private static String getAreaTypeIcon(AreaDefinition.AreaType areaType) {
        if (areaType == null) return "📦";
        switch (areaType) {
            case VBOX: return "⬇️";
            case HBOX: return "➡️";
            case GRIDPANE: return "🔲";
            case BORDERPANE: return "🔳";
            case TABPANE: return "📑";
            case TAB: return "📄";
            case SCROLLPANE: return "📜";
            case SPLITPANE: return "➖";
            case ACCORDION: return "🎛️";
            case TITLEDPANE: return "📋";
            case FLOWPANE: return "〰️";
            case TILEPANE: return "🧱";
            case STACKPANE: return "📚";
            case ANCHORPANE: return "📌";
            case GROUP: return "📁";
            default: return "📦";
        }
    }
    
    /**
     * Create the Event Handlers section showing onClick, onValidate, onChange, 
     * and screen-level callbacks.
     * 
     * @param screenName The name of the screen
     * @param context The interpreter context
     * @param screenAreaItems The screen area items map
     * @return A VBox containing the handlers section
     */
    private static VBox createEventHandlersSection(String screenName, InterpreterContext context, 
            Map<String, AreaItem> screenAreaItems) {
        VBox handlersSection = new VBox(3);
        handlersSection.setPadding(new Insets(5));
        handlersSection.setStyle("-fx-background-color: #f0e8f5;");
        
        javafx.scene.control.Label handlersHeader = new javafx.scene.control.Label("⚡ Event Handlers");
        handlersHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #444;");
        handlersSection.getChildren().add(handlersHeader);
        
        boolean hasHandlers = false;
        
        // Screen-level callbacks
        String callback = context.getScreenCallback(screenName);
        if (callback != null) {
            addEventHandlerRow(handlersSection, screenName, "Screen", "callback", callback);
            hasHandlers = true;
        }
        
        String startupCode = context.getScreenStartupCode(screenName);
        if (startupCode != null) {
            addEventHandlerRow(handlersSection, screenName, "Screen", "onStartup", startupCode);
            hasHandlers = true;
        }
        
        String cleanupCode = context.getScreenCleanupCode(screenName);
        if (cleanupCode != null) {
            addEventHandlerRow(handlersSection, screenName, "Screen", "onCleanup", cleanupCode);
            hasHandlers = true;
        }
        
        String gainFocusCode = context.getScreenGainFocusCode(screenName);
        if (gainFocusCode != null) {
            addEventHandlerRow(handlersSection, screenName, "Screen", "onGainFocus", gainFocusCode);
            hasHandlers = true;
        }
        
        String lostFocusCode = context.getScreenLostFocusCode(screenName);
        if (lostFocusCode != null) {
            addEventHandlerRow(handlersSection, screenName, "Screen", "onLostFocus", lostFocusCode);
            hasHandlers = true;
        }
        
        // Item-level handlers
        // Use IdentityHashSet to avoid processing the same item twice
        // (items can be stored in the map under multiple keys: by name and by varRef)
        if (screenAreaItems != null) {
            java.util.Set<AreaItem> processedItems = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Map.Entry<String, AreaItem> entry : screenAreaItems.entrySet()) {
                AreaItem item = entry.getValue();
                
                // Skip if we've already processed this exact item instance
                if (processedItems.contains(item)) {
                    continue;
                }
                processedItems.add(item);
                
                String itemName = item.name != null ? item.name : entry.getKey();
                
                // Check item-level handlers
                if (item.onValidate != null) {
                    addEventHandlerRow(handlersSection, screenName, itemName, "onValidate", item.onValidate);
                    hasHandlers = true;
                }
                if (item.onChange != null) {
                    addEventHandlerRow(handlersSection, screenName, itemName, "onChange", item.onChange);
                    hasHandlers = true;
                }
                
                // Check displayItem handlers
                if (item.displayItem != null) {
                    if (item.displayItem.onClick != null) {
                        addEventHandlerRow(handlersSection, screenName, itemName, "onClick", item.displayItem.onClick);
                        hasHandlers = true;
                    }
                    if (item.displayItem.onValidate != null && item.onValidate == null) {
                        addEventHandlerRow(handlersSection, screenName, itemName, "onValidate", item.displayItem.onValidate);
                        hasHandlers = true;
                    }
                    if (item.displayItem.onChange != null && item.onChange == null) {
                        addEventHandlerRow(handlersSection, screenName, itemName, "onChange", item.displayItem.onChange);
                        hasHandlers = true;
                    }
                }
            }
        }
        
        if (!hasHandlers) {
            javafx.scene.control.Label noHandlersLabel = new javafx.scene.control.Label("No event handlers defined");
            noHandlersLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
            handlersSection.getChildren().add(noHandlersLabel);
        }
        
        return handlersSection;
    }
    
    /**
     * Add an event handler row to the handlers section.
     * Clicking on the row copies the handler details (including full code) to clipboard.
     * 
     * @param section The VBox to add the row to
     * @param screenName The screen name for event counting
     * @param itemName The name of the item (e.g., "Screen", "button1")
     * @param eventType The event type (e.g., "onClick", "onValidate")
     * @param fullCode The full handler code (for clipboard)
     */
    private static void addEventHandlerRow(VBox section, String screenName, String itemName, String eventType, String fullCode) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 5, 2, 5));
        
        // Event type icon
        String icon = getEventTypeIcon(eventType);
        javafx.scene.control.Label iconLabel = new javafx.scene.control.Label(icon);
        iconLabel.setStyle("-fx-font-size: 11px;");
        
        // Item name - left aligned, 50% width
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(itemName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #660066;");
        nameLabel.setMinWidth(DEBUG_ITEM_NAME_MIN_WIDTH);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Event type with truncated code preview - left aligned, 50% width
        // Get event count for debugging
        int eventCount = getEventCount(screenName, itemName, eventType);
        String countText = eventCount > 0 ? " [" + eventCount + "]" : "";
        
        // Event type with truncated code preview and count
        String truncatedCode = truncateCode(fullCode);
        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label("." + eventType + countText + ": " + truncatedCode);
        typeLabel.setStyle("-fx-text-fill: #006666;");
        typeLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(typeLabel, Priority.ALWAYS);
        typeLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Store label reference for dynamic count updates
        String key = (screenName + "." + itemName + "." + eventType).toLowerCase();
        eventCountLabels.put(key, typeLabel);
        
        row.getChildren().addAll(iconLabel, nameLabel, typeLabel);
        
        // Build clipboard text with handler details including full code
        String clipboardText = itemName + "." + eventType + ":\n" + fullCode;
        
        String originalStyle = "-fx-background-color: #ebe0f0;";
        row.setStyle(originalStyle);
        makeRowClickable(row, clipboardText, originalStyle);
        
        section.getChildren().add(row);
    }
    
    /**
     * Get an icon for the event type.
     */
    private static String getEventTypeIcon(String eventType) {
        switch (eventType.toLowerCase()) {
            case "onclick": return "🖱️";
            case "onvalidate": return "✅";
            case "onchange": return "🔄";
            case "onstartup": return "🚀";
            case "oncleanup": return "🧹";
            case "ongainfocus": return "👁️";
            case "onlostfocus": return "👁️‍🗨️";
            case "callback": return "📞";
            default: return "⚡";
        }
    }
    
    /**
     * Truncate code to a reasonable display length.
     */
    private static String truncateCode(String code) {
        if (code == null) return "";
        String trimmed = code.trim().replace("\n", " ").replace("\r", "");
        if (trimmed.length() > DEBUG_MAX_CODE_DISPLAY_LENGTH) {
            return trimmed.substring(0, DEBUG_MAX_CODE_DISPLAY_LENGTH - 3) + "...";
        }
        return trimmed;
    }
    
    /**
     * Makes a debug row clickable with copy-to-clipboard functionality.
     * Adds hover effect and click handler to copy the provided text to clipboard.
     * 
     * @param row The HBox row to make clickable
     * @param clipboardText The text to copy to clipboard when clicked
     * @param originalStyle The original background style to restore after hover
     */
    private static void makeRowClickable(HBox row, String clipboardText, String originalStyle) {
        // Set cursor to hand to indicate clickable
        row.setStyle(originalStyle + " -fx-cursor: hand;");
        
        // Add hover effect
        row.setOnMouseEntered(e -> {
            row.setStyle(DEBUG_ROW_HOVER_STYLE);
        });
        
        row.setOnMouseExited(e -> {
            row.setStyle(originalStyle + " -fx-cursor: hand;");
        });
        
        // Add click handler to copy to clipboard
        row.setOnMouseClicked(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(clipboardText);
            clipboard.setContent(content);
            
            // Show brief visual feedback
            String currentStyle = row.getStyle();
            row.setStyle(DEBUG_ROW_CLICK_STYLE);
            
            // Revert style after a short delay using JavaFX Timeline
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(200),
                    event -> row.setStyle(currentStyle)
                )
            );
            timeline.play();
        });
        
        // Add tooltip with click-to-copy hint
        String tooltipText = clipboardText + "\n\n📋 Click to copy";
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 12px;");
        tooltip.setShowDelay(javafx.util.Duration.millis(500));
        tooltip.setMaxWidth(DEBUG_TOOLTIP_MAX_WIDTH);
        tooltip.setWrapText(true);
        javafx.scene.control.Tooltip.install(row, tooltip);
    }
    
    /**
     * Helper method to add a simple debug row with label and value.
     * Clicking on the row copies the label and value to clipboard.
     */
    private static void addDebugRow(VBox section, String label, String value, String valueColor) {
        addDebugRowWithLabel(section, label, value, valueColor);
    }
    
    /**
     * Helper method to add a debug row and return the value label for dynamic updates.
     * Clicking on the row copies the label and value to clipboard.
     * 
     * @param section The VBox to add the row to
     * @param label The label text
     * @param value The value text
     * @param valueColor The color for the value text
     * @return The value Label node for dynamic updates
     */
    private static javafx.scene.control.Label addDebugRowWithLabel(VBox section, String label, String value, String valueColor) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(1, 5, 1, 5));
        
        // Label/key - left aligned, 50% width
        javafx.scene.control.Label labelNode = new javafx.scene.control.Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 11px;");
        labelNode.setMinWidth(DEBUG_ITEM_NAME_MIN_WIDTH);
        labelNode.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labelNode, Priority.ALWAYS);
        labelNode.setMaxWidth(Double.MAX_VALUE);
        
        // Value - left aligned, 50% width
        javafx.scene.control.Label valueNode = new javafx.scene.control.Label(value);
        valueNode.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 11px;");
        valueNode.setWrapText(true);
        valueNode.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueNode, Priority.ALWAYS);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        
        row.getChildren().addAll(labelNode, valueNode);
        
        // Format clipboard text with label and value
        String clipboardText = label + ": " + value;
        String originalStyle = "-fx-background-color: transparent;";
        makeRowClickable(row, clipboardText, originalStyle);
        
        section.getChildren().add(row);
        
        return valueNode;
    }
    
    /**
     * Get the value of a screen item for display in TableView.
     * 
     * @param key The item key
     * @param item The AreaItem
     * @param context The interpreter context
     * @param screenName The screen name
     * @return The value string
     */
    private static String getScreenItemValue(String key, AreaItem item, InterpreterContext context, String screenName) {
        // Try to find the bound control and get its actual value
        List<Node> boundControls = context.getScreenBoundControls().get(screenName);
        Node matchingControl = null;
        if (boundControls != null) {
            String itemKey = key.toLowerCase();
            for (Node node : boundControls) {
                Object userData = node.getUserData();
                if (userData != null && userData.toString().toLowerCase().equals(itemKey)) {
                    matchingControl = node;
                    break;
                }
            }
        }
        
        if (matchingControl != null) {
            // Extract value from the actual JavaFX control
            Object controlValue = getControlValue(matchingControl);
            if (controlValue != null) {
                return formatValue(controlValue);
            } else {
                return "(empty)";
            }
        } else if (item.varRef != null) {
            // Fallback to variable value if control not found
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
            if (screenVars != null) {
                Object value = screenVars.get(item.varRef.toLowerCase());
                if (value != null) {
                    return formatValue(value);
                } else {
                    return "(unset)";
                }
            }
        }
        return "(no varRef)";
    }
    
    /**
     * Get the display type for an AreaItem.
     * Returns the type from displayItem if available, otherwise tries to determine from varRef.
     * 
     * @param item The AreaItem
     * @param context The interpreter context
     * @return The item type string (e.g., "colorpicker", "textfield", "combobox")
     */
    /**
     * Get the display type for an AreaItem.
     * Returns the type from displayItem if available, otherwise tries to determine from varRef.
     * 
     * @param item The AreaItem
     * @param context The interpreter context
     * @param screenName The screen name (for displayMetadata lookup)
     * @return The item type string (e.g., "colorpicker", "textfield", "combobox")
     */
    private static String getItemType(AreaItem item, InterpreterContext context, String screenName) {
        // First check if displayItem has the type set directly
        if (item.displayItem != null && item.displayItem.type != null) {
            return item.displayItem.type.toLowerCase();
        }
        // Try to get the type from the displayMetadata registered with the varRef
        // The key format is "screenName.varName"
        if (item.varRef != null && screenName != null) {
            String key = screenName.toLowerCase() + "." + item.varRef.toLowerCase();
            DisplayItem displayItem = context.getDisplayItem().get(key);
            if (displayItem != null && displayItem.type != null) {
                return displayItem.type.toLowerCase();
            }
        }
        return "unknown";
    }
    
    /**
     * Get an emoji icon for the given item type.
     * Uses Unicode characters that represent the control type.
     * 
     * @param itemType The item type string
     * @return An emoji icon representing the item type
     */
    private static String getItemTypeIcon(String itemType) {
        if (itemType == null) {
            return "❓";
        }
        switch (itemType.toLowerCase()) {
            // Text input controls - □ (square, as requested for text items)
            case "textfield":
            case "textfieldpassword":
            case "text":
                return "□";
            case "textarea":
                return "▢";
            
            // Selection controls
            case "combobox":
            case "choicebox":
                return "▼";
            case "checkbox":
                return "☑";
            case "radiobutton":
                return "◉";
            
            // List/Tree controls
            case "listview":
                return "☰";
            case "tableview":
                return "▦";
            case "treeview":
                return "⊞";
            
            // Numeric controls
            case "spinner":
                return "↕";
            case "slider":
                return "─";
            
            // Date/Time/Color controls
            case "datepicker":
                return "📅";
            case "colorpicker":
                return "🎨";
            
            // Button controls
            case "button":
                return "🔘";
            
            // Display-only controls
            case "label":
            case "labeltext":
                return "🏷";
            case "hyperlink":
                return "🔗";
            case "separator":
                return "━";
            
            // Media controls
            case "image":
                return "🖼";
            case "canvas":
                return "🎨";
            case "webview":
                return "🌐";
            case "chart":
                return "📊";
            
            // Progress controls
            case "progressbar":
            case "progressindicator":
                return "⏳";
            
            default:
                return "◇";
        }
    }
    
    /**
     * Build a map from item name (lowercase) to area name by recursively iterating through areas.
     * This handles nested areas.
     * 
     * @param areas The list of AreaDefinitions to process
     * @param itemToAreaMap The map to populate (item name -> area name)
     * @param parentPath The parent path for nested areas (e.g., "parentArea.childArea")
     */
    private static void buildItemToAreaMap(List<AreaDefinition> areas, Map<String, String> itemToAreaMap, String parentPath) {
        if (areas == null) return;
        
        for (AreaDefinition area : areas) {
            String areaPath = area.name;
            if (parentPath != null && !parentPath.isEmpty()) {
                areaPath = parentPath + "." + area.name;
            }
            
            // Add all items in this area to the map
            if (area.items != null) {
                for (AreaItem item : area.items) {
                    if (item.name != null && !item.name.isEmpty()) {
                        itemToAreaMap.put(item.name.toLowerCase(), areaPath);
                    }
                }
            }
            
            // Recursively process child areas
            if (area.childAreas != null && !area.childAreas.isEmpty()) {
                buildItemToAreaMap(area.childAreas, itemToAreaMap, areaPath);
            }
        }
    }
    
    /**
     * Limits the area path to show only the last two direct parents.
     * For example:
     * - "mainArea.section1.subsection1.panel1.subpanel2" becomes "panel1.subpanel2"
     * - "area1.area2.area3" becomes "area2.area3"
     * - "area1.area2" stays as "area1.area2"
     * - "area1" stays as "area1"
     * 
     * @param fullAreaPath The full area path with all levels
     * @return The last two levels of the area path
     */
    private static String limitAreaPathToTwoLevels(String fullAreaPath) {
        if (fullAreaPath == null || fullAreaPath.isEmpty()) {
            return fullAreaPath;
        }
        
        // Find the last dot
        int lastDot = fullAreaPath.lastIndexOf('.');
        if (lastDot == -1) {
            // No dots means only one level, return as-is
            return fullAreaPath;
        }
        
        // Find the second-to-last dot
        int secondLastDot = fullAreaPath.lastIndexOf('.', lastDot - 1);
        if (secondLastDot == -1) {
            // Only one dot means two levels, return as-is
            return fullAreaPath;
        }
        
        // More than two levels, return substring from second-to-last dot onwards
        return fullAreaPath.substring(secondLastDot + 1);
    }
    
    /**
     * Build a display info string with additional displayItem properties.
     * 
     * @param item The AreaItem to get display info from
     * @return A formatted string with display properties, or empty string if none
     */
    private static String buildDisplayItemInfo(AreaItem item, InterpreterContext context, String screenName, String varRef) {
        if (item == null) return "";
        
        StringBuilder info = new StringBuilder();
        
        // Properties from AreaItem
        if (item.editable != null) {
            info.append("Editable: ").append(item.editable).append("\n");
        }
        if (item.disabled != null) {
            info.append("Disabled: ").append(item.disabled).append("\n");
        }
        if (item.visible != null) {
            info.append("Visible: ").append(item.visible).append("\n");
        }
        if (item.tooltip != null && !item.tooltip.isEmpty()) {
            info.append("Tooltip: ").append(item.tooltip).append("\n");
        }
        if (item.layoutPos != null && !item.layoutPos.isEmpty()) {
            info.append("Position: ").append(item.layoutPos).append("\n");
        }
        if (item.prefWidth != null && !item.prefWidth.isEmpty()) {
            info.append("Width: ").append(item.prefWidth).append("\n");
        }
        if (item.prefHeight != null && !item.prefHeight.isEmpty()) {
            info.append("Height: ").append(item.prefHeight).append("\n");
        }
        
        // Properties from DisplayItem
        DisplayItem displayItem = item.displayItem;
        if (displayItem != null) {
            if (displayItem.mandatory) {
                info.append("Mandatory: true\n");
            }
            if (displayItem.labelText != null && !displayItem.labelText.isEmpty()) {
                info.append("Label: ").append(displayItem.labelText).append("\n");
            }
            if (displayItem.promptHelp != null && !displayItem.promptHelp.isEmpty()) {
                info.append("Prompt: ").append(displayItem.promptHelp).append("\n");
            }
            if (displayItem.min != null) {
                info.append("Min: ").append(displayItem.min).append("\n");
            }
            if (displayItem.max != null) {
                info.append("Max: ").append(displayItem.max).append("\n");
            }
            if (displayItem.maxLength != null) {
                info.append("MaxLength: ").append(displayItem.maxLength).append("\n");
            }
            if (displayItem.pattern != null && !displayItem.pattern.isEmpty()) {
                info.append("Pattern: ").append(displayItem.pattern).append("\n");
            }
            if (displayItem.options != null && !displayItem.options.isEmpty()) {
                info.append("Options: ").append(displayItem.options.size()).append(" items\n");
            }
            if (displayItem.optionsMap != null && !displayItem.optionsMap.isEmpty()) {
                info.append("OptionsMap: ").append(displayItem.optionsMap.size()).append(" items\n");
            }
        }
        
        // Check if item is backed by a JavaFX component
        // Null checks are needed as this is called from multiple contexts where parameters may be null
        if (context != null && screenName != null && varRef != null && !varRef.isEmpty()) {
            java.util.concurrent.ConcurrentHashMap<String, ScreenComponentType> componentTypes = context.getScreenComponentTypes(screenName);
            if (componentTypes != null) {
                ScreenComponentType componentType = componentTypes.get(varRef);
                if (componentType != null && componentType.getJavaFXNode() != null) {
                    // Add JavaFX component description
                    if (info.length() > 0) {
                        info.append("\n");
                    }
                    info.append("JavaFX:\n");
                    String javafxDesc = componentType.getJavaFXDescription();
                    // Append the JavaFX description (already formatted by getJavaFXDescription)
                    String[] lines = javafxDesc.split("\n");
                    for (String line : lines) {
                        info.append(line).append("\n");
                    }
                }
            }
        }
        
        // Remove trailing newline
        if (info.length() > 0 && info.charAt(info.length() - 1) == '\n') {
            info.setLength(info.length() - 1);
        }
        
        return info.toString();
    }
    
    /**
     * Create a row displaying a screen item.
     * Clicking on the row copies the item details to clipboard.
     * 
     * @param key The item key
     * @param item The AreaItem
     * @param context The interpreter context
     * @param screenName The screen name
     * @return An HBox containing the item display
     */
    private static HBox createScreenItemRow(String key, AreaItem item, InterpreterContext context, String screenName) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 5, 2, 5));
        
        // Item name with tooltip showing full qualified name (screen.item)
        // Left aligned, 50% width
        String displayName = item.name != null ? item.name : key;
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #006600;");
        nameLabel.setMinWidth(DEBUG_ITEM_NAME_MIN_WIDTH);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Build tooltip with full qualified name (screen.item) and item details
        StringBuilder tooltipText = new StringBuilder();
        // Full qualified name: screenName.itemName
        tooltipText.append(screenName).append(".").append(displayName);
        // JavaFX item type
        if (item.displayItem != null && item.displayItem.itemType != null) {
            tooltipText.append("\njfxType: ").append(item.displayItem.itemType);
        }
        // Variable reference
        if (item.varRef != null) {
            tooltipText.append("\nvarRef: ").append(item.varRef);
        }
        // Layout position
        if (item.layoutPos != null) {
            tooltipText.append("\nlayout: ").append(item.layoutPos);
        }
        
        javafx.scene.control.Tooltip nameTooltip = new javafx.scene.control.Tooltip(tooltipText.toString());
        nameTooltip.setStyle("-fx-font-size: 14px;");
        nameTooltip.setShowDelay(javafx.util.Duration.millis(500));
        nameLabel.setTooltip(nameTooltip);
        
        // Get the actual value from the JavaFX control
        String valueStr = "";
        String fullValueStr = "";
        
        // Try to find the bound control and get its actual value
        List<Node> boundControls = context.getScreenBoundControls().get(screenName);
        Node matchingControl = null;
        if (boundControls != null) {
            String itemKey = key.toLowerCase();
            for (Node node : boundControls) {
                Object userData = node.getUserData();
                if (userData != null && userData.toString().toLowerCase().equals(itemKey)) {
                    matchingControl = node;
                    break;
                }
            }
        }
        
        if (matchingControl != null) {
            // Extract value from the actual JavaFX control
            Object controlValue = getControlValue(matchingControl);
            if (controlValue != null) {
                valueStr = formatValue(controlValue);
                fullValueStr = formatValueFull(controlValue);
            } else {
                valueStr = "(empty)";
                fullValueStr = "Control has no value";
            }
        } else if (item.varRef != null) {
            // Fallback to variable value if control not found
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
            if (screenVars != null) {
                Object value = screenVars.get(item.varRef.toLowerCase());
                if (value != null) {
                    valueStr = formatValue(value);
                    fullValueStr = formatValueFull(value);
                } else {
                    valueStr = "(unset)";
                    fullValueStr = "Variable not set";
                }
            }
        } else {
            valueStr = "(no varRef)";
            fullValueStr = "No variable reference";
        }
        
        // Value label - left aligned, 50% width
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label(valueStr);
        valueLabel.setStyle("-fx-text-fill: #333;");
        valueLabel.setWrapText(true);
        valueLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.control.Tooltip valueTooltip = new javafx.scene.control.Tooltip(fullValueStr);
        valueTooltip.setStyle("-fx-font-size: 14px;");
        valueTooltip.setShowDelay(javafx.util.Duration.millis(500));
        valueLabel.setTooltip(valueTooltip);
        
        row.getChildren().addAll(nameLabel, valueLabel);
        
        // Build comprehensive clipboard text with all item details
        StringBuilder clipboardBuilder = new StringBuilder();
        clipboardBuilder.append(screenName).append(".").append(displayName);
        if (item.displayItem != null && item.displayItem.itemType != null) {
            clipboardBuilder.append(" [").append(item.displayItem.itemType).append("]");
        }
        if (item.varRef != null) {
            clipboardBuilder.append("\nvarRef: ").append(item.varRef);
        }
        if (item.layoutPos != null) {
            clipboardBuilder.append("\nlayout: ").append(item.layoutPos);
        }
        clipboardBuilder.append("\nvalue: ").append(fullValueStr);
        
        String originalStyle = "-fx-background-color: #e8f0e8;";
        row.setStyle(originalStyle);
        makeRowClickable(row, clipboardBuilder.toString(), originalStyle);
        
        return row;
    }
    
    /**
     * Extract the current value from a JavaFX control.
     * 
     * @param control The JavaFX control node
     * @return The control's current value, or null if not extractable
     */
    private static Object getControlValue(Node control) {
        if (control instanceof javafx.scene.control.TextField) {
            return ((javafx.scene.control.TextField) control).getText();
        } else if (control instanceof javafx.scene.control.TextArea) {
            return ((javafx.scene.control.TextArea) control).getText();
        } else if (control instanceof javafx.scene.control.CheckBox) {
            return ((javafx.scene.control.CheckBox) control).isSelected();
        } else if (control instanceof javafx.scene.control.ComboBox) {
            return ((javafx.scene.control.ComboBox<?>) control).getValue();
        } else if (control instanceof javafx.scene.control.ChoiceBox) {
            return ((javafx.scene.control.ChoiceBox<?>) control).getValue();
        } else if (control instanceof javafx.scene.control.Slider) {
            return ((javafx.scene.control.Slider) control).getValue();
        } else if (control instanceof javafx.scene.control.Spinner) {
            return ((javafx.scene.control.Spinner<?>) control).getValue();
        } else if (control instanceof javafx.scene.control.DatePicker) {
            return ((javafx.scene.control.DatePicker) control).getValue();
        } else if (control instanceof javafx.scene.control.Label) {
            return ((javafx.scene.control.Label) control).getText();
        } else if (control instanceof javafx.scene.control.Labeled) {
            return ((javafx.scene.control.Labeled) control).getText();
        }
        return null;
    }
    
    /**
     * Format all variables and screen items for clipboard copy.
     * 
     * @param screenName The name of the screen
     * @param screenVars The screen variables map
     * @param screenVarTypes The screen variable types map
     * @param screenAreaItems The screen area items map
     * @param context The interpreter context for accessing bound controls
     * @return Formatted string for clipboard
     */
    private static String formatAllForClipboard(String screenName, 
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> screenVarTypes,
            Map<String, AreaItem> screenAreaItems,
            InterpreterContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Screen Debug: ").append(screenName).append("\n");
        sb.append("=".repeat(60)).append("\n\n");
        
        // Status & Config section
        sb.append("⚙️ STATUS & CONFIGURATION\n");
        sb.append("-".repeat(40)).append("\n");
        ScreenStatus status = context.getScreenStatus(screenName);
        sb.append("Status: ").append(status.name()).append("\n");
        String errorMsg = context.getScreenErrorMessage(screenName);
        if (errorMsg != null && !errorMsg.isEmpty()) {
            sb.append("Error: ").append(errorMsg).append("\n");
        }
        ScreenConfig config = context.getScreenConfig(screenName);
        if (config != null) {
            sb.append("Title: ").append(config.getTitle()).append("\n");
            sb.append("Size: ").append(config.getWidth()).append(" x ").append(config.getHeight()).append("\n");
        }
        String parentScreen = context.getScreenParent(screenName);
        if (parentScreen != null) {
            sb.append("Parent Screen: ").append(parentScreen).append("\n");
        }
        Thread screenThread = context.getScreenThreads().get(screenName.toLowerCase());
        if (screenThread != null) {
            sb.append("Thread: ").append(screenThread.getName()).append(" (").append(screenThread.isAlive() ? "alive" : "stopped").append(")\n");
        }
        ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(screenName);
        if (dispatcher != null) {
            sb.append("Dispatcher: ").append(dispatcher.isRunning() ? "running" : "stopped").append("\n");
        }
        
        // Variables section
        sb.append("\n📊 VARIABLES\n");
        sb.append("-".repeat(40)).append("\n");
        if (screenVars == null || screenVars.isEmpty()) {
            sb.append("No variables defined\n");
        } else {
            java.util.List<String> sortedKeys = new java.util.ArrayList<>(screenVars.keySet());
            java.util.Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);
            
            for (String key : sortedKeys) {
                Object value = screenVars.get(key);
                DataType dataType = screenVarTypes != null ? screenVarTypes.get(key) : null;
                String typeStr = getDataTypeString(dataType, value);
                sb.append(key).append(" : ").append(typeStr).append(" = ").append(formatValueFull(value)).append("\n");
            }
        }
        
        // Screen items section
        sb.append("\n🖼️ SCREEN ITEMS\n");
        sb.append("-".repeat(40)).append("\n");
        if (screenAreaItems == null || screenAreaItems.isEmpty()) {
            sb.append("No screen items defined\n");
        } else {
            java.util.List<String> sortedItemKeys = new java.util.ArrayList<>(screenAreaItems.keySet());
            java.util.Collections.sort(sortedItemKeys, String.CASE_INSENSITIVE_ORDER);
            
            // Get bound controls for value extraction
            List<Node> boundControls = context.getScreenBoundControls().get(screenName);
            
            for (String key : sortedItemKeys) {
                AreaItem item = screenAreaItems.get(key);
                String displayName = item.name != null ? item.name : key;
                
                // Full qualified name: screenName.itemName
                sb.append(screenName).append(".").append(displayName);
                
                // Add JavaFX item type
                if (item.displayItem != null && item.displayItem.itemType != null) {
                    sb.append(" [").append(item.displayItem.itemType).append("]");
                }
                
                // Add varRef
                if (item.varRef != null) {
                    sb.append(" varRef: ").append(item.varRef);
                }
                
                // Add actual control value
                String valueStr = getControlValueForClipboard(key, boundControls, item, screenVars);
                sb.append(" = ").append(valueStr);
                
                sb.append("\n");
            }
        }
        
        // Screen Areas section
        sb.append("\n📐 SCREEN AREAS\n");
        sb.append("-".repeat(40)).append("\n");
        List<AreaDefinition> screenAreas = context.getScreenAreas(screenName);
        if (screenAreas != null && !screenAreas.isEmpty()) {
            for (AreaDefinition area : screenAreas) {
                formatAreaDefinitionForClipboard(sb, area, 0);
            }
        } else {
            sb.append("No areas defined\n");
        }
        
        // Event Handlers section
        sb.append("\n⚡ EVENT HANDLERS\n");
        sb.append("-".repeat(40)).append("\n");
        boolean hasHandlers = false;
        
        String callback = context.getScreenCallback(screenName);
        if (callback != null) {
            sb.append("Screen.callback: ").append(callback).append("\n");
            hasHandlers = true;
        }
        String startupCode = context.getScreenStartupCode(screenName);
        if (startupCode != null) {
            sb.append("Screen.onStartup: ").append(truncateCode(startupCode)).append("\n");
            hasHandlers = true;
        }
        String cleanupCode = context.getScreenCleanupCode(screenName);
        if (cleanupCode != null) {
            sb.append("Screen.onCleanup: ").append(truncateCode(cleanupCode)).append("\n");
            hasHandlers = true;
        }
        String gainFocusCode = context.getScreenGainFocusCode(screenName);
        if (gainFocusCode != null) {
            sb.append("Screen.onGainFocus: ").append(truncateCode(gainFocusCode)).append("\n");
            hasHandlers = true;
        }
        String lostFocusCode = context.getScreenLostFocusCode(screenName);
        if (lostFocusCode != null) {
            sb.append("Screen.onLostFocus: ").append(truncateCode(lostFocusCode)).append("\n");
            hasHandlers = true;
        }
        
        // Item-level handlers
        // Use IdentityHashSet to avoid processing the same item twice
        // (items can be stored in the map under multiple keys: by name and by varRef)
        if (screenAreaItems != null) {
            java.util.Set<AreaItem> processedItems = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Map.Entry<String, AreaItem> entry : screenAreaItems.entrySet()) {
                AreaItem item = entry.getValue();
                
                // Skip if we've already processed this exact item instance
                if (processedItems.contains(item)) {
                    continue;
                }
                processedItems.add(item);
                
                String itemName = item.name != null ? item.name : entry.getKey();
                
                if (item.onValidate != null) {
                    sb.append(itemName).append(".onValidate: ").append(truncateCode(item.onValidate)).append("\n");
                    hasHandlers = true;
                }
                if (item.onChange != null) {
                    sb.append(itemName).append(".onChange: ").append(truncateCode(item.onChange)).append("\n");
                    hasHandlers = true;
                }
                if (item.displayItem != null) {
                    if (item.displayItem.onClick != null) {
                        sb.append(itemName).append(".onClick: ").append(truncateCode(item.displayItem.onClick)).append("\n");
                        hasHandlers = true;
                    }
                    if (item.displayItem.onValidate != null && item.onValidate == null) {
                        sb.append(itemName).append(".onValidate: ").append(truncateCode(item.displayItem.onValidate)).append("\n");
                        hasHandlers = true;
                    }
                    if (item.displayItem.onChange != null && item.onChange == null) {
                        sb.append(itemName).append(".onChange: ").append(truncateCode(item.displayItem.onChange)).append("\n");
                        hasHandlers = true;
                    }
                }
            }
        }
        
        if (!hasHandlers) {
            sb.append("No event handlers defined\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Format an area definition for clipboard output with indentation.
     */
    private static void formatAreaDefinitionForClipboard(StringBuilder sb, AreaDefinition area, int depth) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append(getAreaTypeIcon(area.areaType)).append(" ").append(area.name);
        sb.append(" [").append(area.type != null ? area.type : "pane").append("]");
        
        int itemCount = area.items != null ? area.items.size() : 0;
        int childCount = area.childAreas != null ? area.childAreas.size() : 0;
        if (itemCount > 0 || childCount > 0) {
            sb.append(" (");
            if (itemCount > 0) sb.append(itemCount).append(" items");
            if (itemCount > 0 && childCount > 0) sb.append(", ");
            if (childCount > 0) sb.append(childCount).append(" children");
            sb.append(")");
        }
        sb.append("\n");
        
        // Add additional area details
        if (area.layout != null) {
            sb.append(indent).append("  layout: ").append(area.layout).append("\n");
        }
        if (area.gainFocus != null) {
            sb.append(indent).append("  gainFocus: ").append(truncateCode(area.gainFocus)).append("\n");
        }
        if (area.lostFocus != null) {
            sb.append(indent).append("  lostFocus: ").append(truncateCode(area.lostFocus)).append("\n");
        }
        
        // Recursively add child areas
        if (area.childAreas != null) {
            for (AreaDefinition childArea : area.childAreas) {
                formatAreaDefinitionForClipboard(sb, childArea, depth + 1);
            }
        }
    }
    
    /**
     * Get the control value for clipboard formatting.
     * 
     * @param key The item key
     * @param boundControls List of bound controls
     * @param item The area item
     * @param screenVars Screen variables map
     * @return The formatted value string
     */
    private static String getControlValueForClipboard(String key, List<Node> boundControls, 
            AreaItem item, java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Try to find the bound control and get its actual value
        Node matchingControl = null;
        if (boundControls != null) {
            String itemKey = key.toLowerCase();
            for (Node node : boundControls) {
                Object userData = node.getUserData();
                if (userData != null && userData.toString().toLowerCase().equals(itemKey)) {
                    matchingControl = node;
                    break;
                }
            }
        }
        
        if (matchingControl != null) {
            Object controlValue = getControlValue(matchingControl);
            if (controlValue != null) {
                return formatValueFull(controlValue);
            }
            return "(empty)";
        } else if (item.varRef != null && screenVars != null) {
            // Fallback to variable value if control not found
            Object value = screenVars.get(item.varRef.toLowerCase());
            if (value != null) {
                return formatValueFull(value);
            }
            return "(unset)";
        }
        return "(no value)";
    }
    
    /**
     * Format all variables for clipboard copy.
     * 
     * @param screenName The name of the screen
     * @param screenVars The screen variables map
     * @param screenVarTypes The screen variable types map
     * @return Formatted string for clipboard
     */
    private static String formatVariablesForClipboard(String screenName, 
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> screenVarTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Screen: ").append(screenName).append("\n");
        sb.append("=".repeat(40)).append("\n\n");
        
        if (screenVars == null || screenVars.isEmpty()) {
            sb.append("No variables defined\n");
        } else {
            java.util.List<String> sortedKeys = new java.util.ArrayList<>(screenVars.keySet());
            java.util.Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);
            
            for (String key : sortedKeys) {
                Object value = screenVars.get(key);
                DataType dataType = screenVarTypes != null ? screenVarTypes.get(key) : null;
                String typeStr = getDataTypeString(dataType, value);
                sb.append(key).append(" : ").append(typeStr).append(" = ").append(formatValueFull(value)).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Create a row displaying a variable name and its value.
     * Clicking on the row copies the variable details to clipboard.
     * 
     * @param name The variable name
     * @param value The variable value
     * @param dataType The variable's data type (can be null)
     * @param rowIndex The row index for alternating background colors
     * @return An HBox containing the variable display
     */
    private static HBox createVariableRow(String name, Object value, DataType dataType, int rowIndex) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 5, 2, 5));
        
        // Variable name with tooltip showing full name and data type
        // Left aligned, 50% width
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(name + ":");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");
        nameLabel.setMinWidth(DEBUG_ITEM_NAME_MIN_WIDTH);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        String typeStr = getDataTypeString(dataType, value);
        javafx.scene.control.Tooltip nameTooltip = new javafx.scene.control.Tooltip(name + " : " + typeStr);
        nameTooltip.setStyle("-fx-font-size: 14px;");
        nameTooltip.setShowDelay(javafx.util.Duration.millis(500));
        nameLabel.setTooltip(nameTooltip);
        
        // Variable value with tooltip showing full value - left aligned, 50% width
        String valueStr = formatValue(value);
        String fullValueStr = formatValueFull(value);
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label(valueStr);
        valueLabel.setStyle("-fx-text-fill: #333;");
        valueLabel.setWrapText(true);
        valueLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.control.Tooltip valueTooltip = new javafx.scene.control.Tooltip(fullValueStr);
        valueTooltip.setStyle("-fx-font-size: 14px;");
        valueTooltip.setShowDelay(javafx.util.Duration.millis(500));
        valueLabel.setTooltip(valueTooltip);
        
        row.getChildren().addAll(nameLabel, valueLabel);
        
        // Build clipboard text with variable name, type, and value
        String clipboardText = name + " : " + typeStr + " = " + fullValueStr;
        // Use alternating row colors for better readability
        String originalStyle = "-fx-background-color: " + (rowIndex % 2 == 0 ? "#ffffff" : "#f0f0f0") + ";";
        row.setStyle(originalStyle);
        makeRowClickable(row, clipboardText, originalStyle);
        
        return row;
    }
    
    /**
     * Get the data type as a string for display.
     * 
     * @param dataType The DataType from the context (can be null)
     * @param value The actual value to infer type from if DataType is null
     * @return A string representation of the data type
     */
    private static String getDataTypeString(DataType dataType, Object value) {
        if (dataType != null) {
            return dataType.name().toLowerCase();
        }
        // Infer type from value
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Integer) {
            return "int";
        }
        if (value instanceof Long) {
            return "long";
        }
        if (value instanceof Double || value instanceof Float) {
            return "float";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        if (value instanceof java.util.Map) {
            return "map";
        }
        if (value instanceof java.util.List) {
            return "list";
        }
        if (value.getClass().isArray()) {
            return "array";
        }
        return value.getClass().getSimpleName().toLowerCase();
    }
    
    /**
     * Get a detailed type string including record definition for tooltips.
     * Similar to "print typeof" functionality.
     * 
     * @param dataType The DataType from the context (can be null)
     * @param value The actual value to infer type from if DataType is null
     * @return A detailed string representation of the data type with record definition
     */
    private static String getDetailedTypeString(DataType dataType, Object value) {
        if (value == null) {
            return "null";
        }
        
        // Check for arrays first
        if (value instanceof ArrayDef) {
            ArrayDef<?, ?> arrayDef = (ArrayDef<?, ?>) value;
            DataType elementType = arrayDef.getDataType();
            boolean isFixed = arrayDef.isFixed();
            int size = arrayDef.size();
            
            StringBuilder sb = new StringBuilder("array.");
            sb.append(getDataTypeName(elementType));
            
            if (isFixed) {
                sb.append("[").append(size).append("]");
            } else {
                sb.append("[]");
            }
            
            return sb.toString();
        }
        
        // Check for records (Map type)
        if (value instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            StringBuilder sb = new StringBuilder("record {");
            boolean first = true;
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(entry.getKey()).append(": ");
                // Get type of field value
                Object fieldValue = entry.getValue();
                if (fieldValue == null) {
                    sb.append("null");
                } else if (fieldValue instanceof String) {
                    sb.append("string");
                } else if (fieldValue instanceof Integer) {
                    sb.append("int");
                } else if (fieldValue instanceof Long) {
                    sb.append("long");
                } else if (fieldValue instanceof Double || fieldValue instanceof Float) {
                    sb.append("float");
                } else if (fieldValue instanceof Boolean) {
                    sb.append("bool");
                } else if (fieldValue instanceof java.util.Map) {
                    sb.append("record");
                } else {
                    sb.append(fieldValue.getClass().getSimpleName().toLowerCase());
                }
            }
            sb.append("}");
            return sb.toString();
        }
        
        // Fall back to simple type string
        return getDataTypeString(dataType, value);
    }
    
    /**
     * Get the data type name as a lowercase string.
     */
    private static String getDataTypeName(DataType dataType) {
        if (dataType == null) {
            return "unknown";
        }
        return dataType.name().toLowerCase();
    }
    
    /**
     * Format a value for display in the debug panel.
     * 
     * @param value The value to format
     * @return A string representation of the value
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 50) {
                return "\"" + str.substring(0, 47) + "...\"";
            }
            return "\"" + str + "\"";
        }
        if (value instanceof java.util.Map) {
            return "{Map: " + ((java.util.Map<?, ?>) value).size() + " entries}";
        }
        if (value instanceof java.util.List) {
            return "[List: " + ((java.util.List<?>) value).size() + " items]";
        }
        if (value.getClass().isArray()) {
            return "[Array: " + java.lang.reflect.Array.getLength(value) + " items]";
        }
        String str = String.valueOf(value);
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        return str;
    }
    
    /**
     * Format a value for tooltip display (full value without truncation).
     * 
     * @param value The value to format
     * @return A full string representation of the value for tooltip display
     */
    private static String formatValueFull(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) value;
            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (java.util.Map.Entry<Object, Object> entry : map.entrySet()) {
                if (count > 0) sb.append(", ");
                sb.append(String.valueOf(entry.getKey())).append(": ").append(String.valueOf(entry.getValue()));
                count++;
                if (count >= 20) {
                    sb.append(", ... (").append(map.size() - 20).append(" more)");
                    break;
                }
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (Object item : list) {
                if (count > 0) sb.append(", ");
                sb.append(String.valueOf(item));
                count++;
                if (count >= 20) {
                    sb.append(", ... (").append(list.size() - 20).append(" more)");
                    break;
                }
            }
            sb.append("]");
            return sb.toString();
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < Math.min(length, 20); i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.valueOf(java.lang.reflect.Array.get(value, i)));
            }
            if (length > 20) {
                sb.append(", ... (").append(length - 20).append(" more)");
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
    
    /**
     * Clean up debug panel resources for a specific screen.
     * Should be called when a screen is closed or removed.
     * 
     * @param screenName The name of the screen
     */
    public static void cleanupScreenDebugPanel(String screenName) {
        if (screenName != null) {
            String key = screenName.toLowerCase();
            screenDebugPanels.remove(key);
            screenRootPanes.remove(key);
            screenOriginalWidths.remove(key);
            debugStatusLabels.remove(key);
            changedItems.remove(key);
            debugItemsTables.remove(key);
        }
    }
    
    /**
     * Clean up all debug panel resources.
     * Should be called when the interpreter is reset or all screens are cleared.
     */
    public static void cleanupAllDebugPanels() {
        screenDebugPanels.clear();
        screenRootPanes.clear();
        screenOriginalWidths.clear();
        debugStatusLabels.clear();
        changedItems.clear();
        debugItemsTables.clear();
    }
    
    /**
     * Check if debug mode is enabled for the current thread.
     */
    private static boolean isDebugMode() {
        Boolean mode = debugMode.get();
        return mode != null && mode;
    }
    
    /**
     * Get debug mode state for the current thread (for inheritance to child threads).
     * Public method that can be called from other classes.
     */
    public static boolean getDebugModeForInheritance() {
        Boolean mode = debugMode.get();
        return mode != null && mode;
    }
    
    /**
     * Set debug mode for the current thread.
     * Public method that can be called from other classes to explicitly set debug state.
     */
    public static void setDebugModeForThread(boolean enabled) {
        debugMode.set(enabled);
    }
    
    /**
     * Capture a screenshot of the specified screen using Ctrl+P shortcut.
     * Saves the screenshot to system temp directory with screen name and sequence number.
     * 
     * @param screenName The name of the screen to capture
     * @param stage The JavaFX Stage containing the screen
     * @param context The interpreter context
     */
    public static void captureScreenshotToFile(String screenName, Stage stage, InterpreterContext context) {
        try {
            // Get the scene from the stage
            Scene scene = stage.getScene();
            if (scene == null) {
                System.err.println("Cannot capture screenshot: no scene available for screen '" + screenName + "'");
                return;
            }
            
            // Take the snapshot
            javafx.scene.image.WritableImage snapshot = scene.snapshot(null);
            if (snapshot == null) {
                System.err.println("Failed to capture screenshot for screen '" + screenName + "'");
                return;
            }
            
            // Get or initialize the screenshot counter for this screen
            java.util.concurrent.atomic.AtomicInteger counter = screenshotCounters.computeIfAbsent(
                screenName, 
                k -> new java.util.concurrent.atomic.AtomicInteger(0)
            );
            int sequence = counter.incrementAndGet();
            
            // Create filename using system temp directory for cross-platform compatibility
            String tempDir = System.getProperty("java.io.tmpdir");
            String filename = String.format("%s%s_%03d.png", tempDir, screenName, sequence);
            
            // Save to file
            java.io.File file = new java.io.File(filename);
            ImageIO.write(
                SwingFXUtils.fromFXImage(snapshot, null),
                "png",
                file
            );
            
            // Log success
            String message = String.format("Screenshot saved: %s (%dx%d pixels)", 
                filename, 
                (int)snapshot.getWidth(), 
                (int)snapshot.getHeight()
            );
            System.out.println(message);
            
            // Also print to console output if available
            if (context != null && context.getOutput() != null) {
                context.getOutput().printlnOk(message);
            }
            
        } catch (Exception e) {
            String errorMsg = "Error capturing screenshot for screen '" + screenName + "': " + e.getMessage();
            System.err.println(errorMsg);
            if (context != null && context.getOutput() != null) {
                context.getOutput().printlnError(errorMsg);
            }
            e.printStackTrace();
        }
    }
    
    /**
     * Log debug information about a JavaFX Node and its properties.
     * Only logs when debug mode is enabled.
     */
    private static void logNodeDebug(Node node, String context) {
        if (!isDebugMode()) {
            return;
        }
        
        StringBuilder log = new StringBuilder();
        log.append("\n").append("=".repeat(80)).append("\n");
        log.append("[DEBUG] ").append(context).append("\n");
        log.append("=".repeat(80)).append("\n");
        
        // JavaFX Node Type
        log.append("JavaFX Type:  ").append(node.getClass().getSimpleName()).append("\n");
        
        // ID (often used for CSS selection)
        if (node.getId() != null && !node.getId().isEmpty()) {
            log.append("ID:           ").append(node.getId()).append("\n");
        }
        
        // UserData (often contains screen name reference)
        if (node.getUserData() != null) {
            log.append("UserData:     ").append(node.getUserData()).append("\n");
        }
        
        // Style (inline CSS)
        if (node.getStyle() != null && !node.getStyle().isEmpty()) {
            log.append("Style:        ").append(node.getStyle()).append("\n");
        }
        
        // StyleClass (CSS class names)
        if (!node.getStyleClass().isEmpty()) {
            log.append("StyleClass:   ").append(node.getStyleClass()).append("\n");
        }
        
        // Control-specific properties (text content, prompt, etc.)
        if (node instanceof javafx.scene.control.Labeled) {
            javafx.scene.control.Labeled labeled = (javafx.scene.control.Labeled) node;
            if (labeled.getText() != null && !labeled.getText().isEmpty()) {
                log.append("Text:         ").append(labeled.getText()).append("\n");
            }
        }
        if (node instanceof javafx.scene.control.TextInputControl) {
            javafx.scene.control.TextInputControl textInput = (javafx.scene.control.TextInputControl) node;
            if (textInput.getPromptText() != null && !textInput.getPromptText().isEmpty()) {
                log.append("PromptText:   ").append(textInput.getPromptText()).append("\n");
            }
        }
        
        // Tooltip
        if (node instanceof javafx.scene.control.Control) {
            javafx.scene.control.Control control = (javafx.scene.control.Control) node;
            if (control.getTooltip() != null && control.getTooltip().getText() != null) {
                log.append("Tooltip:      ").append(control.getTooltip().getText()).append("\n");
            }
        }
        
        // Visibility state
        log.append("Visible:      ").append(node.isVisible() ? "YES" : "NO").append("\n");
        log.append("Managed:      ").append(node.isManaged() ? "YES" : "NO").append("\n");
        
        // Region-specific properties (padding, background, border)
        if (node instanceof Region) {
            Region region = (Region) node;
            if (region.getPadding() != null && !region.getPadding().equals(Insets.EMPTY)) {
                log.append("Padding:      ").append(region.getPadding()).append("\n");
            }
            if (region.getBackground() != null) {
                log.append("Background:   SET\n");
            }
            if (region.getBorder() != null) {
                log.append("Border:       SET\n");
            }
        }
        
        // Layout bounds (size and position)
        if (node.getLayoutBounds() != null) {
            log.append("LayoutBounds: ").append(String.format("width=%.1f, height=%.1f", 
                node.getLayoutBounds().getWidth(), 
                node.getLayoutBounds().getHeight())).append("\n");
        }
        
        // Custom properties map (if any)
        if (!node.getProperties().isEmpty()) {
            log.append("Properties:   ").append(node.getProperties()).append("\n");
        }
        
        log.append("=".repeat(80)).append("\n");
        System.out.println(log.toString());
    }
    
    /**
     * Recursively log all nodes in a scene graph.
     * Only logs when debug mode is enabled.
     */
    private static void logSceneGraph(Node node, int depth, String path) {
        if (!isDebugMode()) {
            return;
        }
        
        String indent = "  ".repeat(depth);
        String nodeInfo = String.format("%s[%s] %s", 
            indent,
            node.getClass().getSimpleName(),
            node.getId() != null ? "id=" + node.getId() : ""
        );
        
        if (node.getStyle() != null && !node.getStyle().isEmpty()) {
            nodeInfo += " style=\"" + node.getStyle() + "\"";
        }
        
        System.out.println(nodeInfo);
        
        // Recurse for containers
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            int childIndex = 0;
            for (Node child : parent.getChildrenUnmodifiable()) {
                logSceneGraph(child, depth + 1, path + "/" + childIndex);
                childIndex++;
            }
        }
    }

    /**
     * Creates a ScreenDefinition with basic parameters.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context) {
        return createScreenDefinition(screenName, title, width, height, areas, null, null, null, context);
    }
    
    /**
     * Creates a ScreenDefinition with variable binding support.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinition containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for two-way binding
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            InterpreterContext context) {
        return createScreenDefinition(screenName, title, width, height, areas, screenVars, null, null, context);
    }
    
    /**
     * Creates a ScreenDefinition with variable binding and onClick handlers.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for two-way binding
     * @param varTypes The ConcurrentHashMap containing screen variable types for proper type conversion
     * @param onClickHandler Handler for button onClick events
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            InterpreterContext context) {
        ScreenDefinition definition = new ScreenDefinition(screenName, title, width, height);
        definition.setAreas(areas);
        definition.setScreenVars(screenVars);
        definition.setVarTypes(varTypes);
        definition.setOnClickHandler(onClickHandler);
        definition.setContext(context);
        return definition;
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions. This
     * method creates containers, adds items, and applies layout properties.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context) {
        return createScreen(screenName, title, width, height, areas, null, null, null, context);
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions with
     * variable binding. This method creates containers, adds items, applies
     * layout properties, and sets up two-way data binding.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for
     * two-way binding (can be null)
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            InterpreterContext context) {
        return createScreen(screenName, title, width, height, areas, screenVars, null, null, context);
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions with
     * variable binding and onClick handlers. This method creates containers,
     * adds items, applies layout properties, sets up two-way data binding, and
     * configures button onClick handlers.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for
     * two-way binding (can be null)
     * @param varTypes The ConcurrentHashMap containing screen variable types
     * for proper type conversion (can be null)
     * @param onClickHandler Handler for button onClick events (can be null)
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            InterpreterContext context) {
        // Log debug mode state at the start of createScreen for troubleshooting
        if (isDebugMode()) {
            System.out.println("\n[DEBUG] createScreen() called with debug mode ENABLED");
            System.out.println("[DEBUG] Thread: " + Thread.currentThread().getName());
            System.out.println("[DEBUG] Screen: " + screenName);
            System.out.println("[DEBUG] Areas count: " + (areas != null ? areas.size() : 0));
            if (areas != null && !areas.isEmpty()) {
                for (AreaDefinition area : areas) {
                    System.out.println("[DEBUG]   - Area: " + area.name + 
                        " (childAreas: " + (area.childAreas != null ? area.childAreas.size() : 0) + 
                        ", items: " + (area.items != null ? area.items.size() : 0) + ")");
                }
            }
        }
        
        Stage stage = new Stage();
        stage.setTitle(title);

        // List to collect all bound controls for this screen
        List<Node> allBoundControls = new ArrayList<>();

        // Create root container - use VBox as default if multiple areas
        Region rootContainer;

        if (areas == null || areas.isEmpty()) {
            // No areas defined, create empty pane
            rootContainer = new Pane();
        } else if (areas.size() == 1) {
            // Single area - use it as root
            rootContainer = createAreaWithItems(areas.get(0), screenName, context, screenVars, varTypes, onClickHandler, allBoundControls);
        } else {
            // Multiple areas - arrange in VBox
            VBox root = new VBox();
            root.setSpacing(10);

            for (AreaDefinition areaDef : areas) {
                Region areaContainer = createAreaWithItems(areaDef, screenName, context, screenVars, varTypes, onClickHandler, allBoundControls);
                root.getChildren().add(areaContainer);
            }

            rootContainer = root;
        }

        // Store bound controls in context if provided
        // Use lowercase for consistent lookup across the application
        String lowerScreenName = screenName.toLowerCase();
        if (context != null && !allBoundControls.isEmpty()) {
            context.getScreenBoundControls().put(lowerScreenName, allBoundControls);

            // Register refresh callback that refreshes all bound controls
            context.getScreenRefreshCallbacks().put(lowerScreenName, () -> {
                // Use Platform.runLater to ensure UI updates happen on JavaFX Application Thread
                Platform.runLater(() -> {
                    refreshBoundControls(allBoundControls, screenVars);
                });
            });
        }

        // Wrap content in ScrollPane to handle overflow when content is larger than window
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);  // Make content fit to scroll pane width
        scrollPane.setFitToHeight(true); // Make content fit to scroll pane height to allow proper resizing
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Create status bar for the screen
        com.eb.ui.ebs.StatusBar statusBar = new com.eb.ui.ebs.StatusBar();
        
        // Add focus listeners to all bound controls to update status bar
        setupStatusBarUpdates(allBoundControls, statusBar, context, screenName);
        
        // Create menu bar for the screen
        javafx.scene.control.MenuBar menuBar = createScreenMenuBar(stage);
        
        // Wrap in BorderPane to add menu bar at top and status bar at bottom
        BorderPane screenRoot = new BorderPane();
        screenRoot.setTop(menuBar);
        screenRoot.setCenter(scrollPane);
        screenRoot.setBottom(statusBar);
        
        // Store the root pane reference for debug panel toggling
        screenRootPanes.put(screenName.toLowerCase(), screenRoot);
        
        // Store status bar in context for later access
        if (context != null) {
            context.getScreenStatusBars().put(screenName, statusBar);
        }

        Scene scene = new Scene(screenRoot, width, height);
        
        // Add Ctrl+D key handler to toggle debug mode (per-thread)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.D && event.isControlDown()) {
                toggleDebugMode(screenName, context);
                if (isDebugMode()) {
                    // Log the entire scene graph when debug mode is enabled
                    System.out.println("\n" + "=".repeat(80));
                    System.out.println("SCENE GRAPH for: " + screenName);
                    System.out.println("=".repeat(80));
                    logSceneGraph(screenRoot, 0, "/");
                    System.out.println("=".repeat(80));
                }
                event.consume(); // Prevent the event from propagating further
            }
            // Add Ctrl+P key handler to capture screenshot
            else if (event.getCode() == KeyCode.P && event.isControlDown()) {
                captureScreenshotToFile(screenName, stage, context);
                event.consume(); // Prevent the event from propagating further
            }
        });
        
        // Load CSS stylesheets for screen areas and input controls
        try {
            scene.getStylesheets().add(ScreenFactory.class.getResource("/css/screen-areas.css").toExternalForm());
            scene.getStylesheets().add(ScreenFactory.class.getResource("/css/screen-inputs.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Warning: Could not load screen CSS stylesheets: " + e.getMessage());
        }
        
        stage.setScene(scene);

        return stage;
    }
    
    /**
     * Setup focus listeners on all controls to update the status bar
     * with item tooltip and min/max information
     */
    private static void setupStatusBarUpdates(List<Node> controls, 
            com.eb.ui.ebs.StatusBar statusBar,
            InterpreterContext context,
            String screenName) {
        for (Node control : controls) {
            control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    // Get the item's user data which contains "screenName.itemName"
                    Object userData = control.getUserData();
                    if (userData != null && userData instanceof String) {
                        String fullRef = (String) userData;
                        // Extract item name from "screenName.itemName"
                        String itemName = fullRef.substring(screenName.length() + 1);
                        
                        // Get metadata for this item from context
                        DisplayItem metadata = context != null ? context.getDisplayItem().get(screenName + "." + itemName) : null;
                        // Update message with tooltip (prefer tooltip over promptHelp)
                        String tooltip = (String) control.getProperties().get("itemTooltip");
                        String message = tooltip != null ? tooltip : "";
                        statusBar.setMessage(message);
                        
                        // Get metadata for min/max info
                        if (metadata != null) {
                            // Update custom with min/max info
                            String minMaxInfo = "";
                            if (metadata.min != null && metadata.max != null) {
                                minMaxInfo = String.format("min:%s max:%s", metadata.min, metadata.max);
                            } else if (metadata.min != null) {
                                minMaxInfo = "min:" + metadata.min;
                            } else if (metadata.max != null) {
                                minMaxInfo = "max:" + metadata.max;
                            } else if (metadata.maxLength != null) {
                                minMaxInfo = "len:" + metadata.maxLength;
                            }
                            statusBar.setCustom(minMaxInfo);
                        }
                    }
                } else {
                    // Lost focus - clear status bar
                    statusBar.clearMessage();
                    statusBar.clearCustom();
                }
            });
        }
    }
    
    /**
     * Create a menu bar for screen windows with Edit menu containing Copy/Cut/Paste/Undo/Redo/Close items
     */
    private static javafx.scene.control.MenuBar createScreenMenuBar(Stage stage) {
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        
        // Create Edit menu
        javafx.scene.control.Menu editMenu = new javafx.scene.control.Menu("Edit");
        
        // Cut menu item with Ctrl+X
        javafx.scene.control.MenuItem cutItem = new javafx.scene.control.MenuItem("Cut");
        cutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.X, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> {
            // Get the focused control and perform cut if it's a text control
            javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
            if (focusOwner instanceof javafx.scene.control.TextField) {
                ((javafx.scene.control.TextField) focusOwner).cut();
            } else if (focusOwner instanceof javafx.scene.control.TextArea) {
                ((javafx.scene.control.TextArea) focusOwner).cut();
            }
        });
        
        // Copy menu item with Ctrl+C
        javafx.scene.control.MenuItem copyItem = new javafx.scene.control.MenuItem("Copy");
        copyItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.C, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> {
            // Get the focused control and perform copy if it's a text control
            javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
            if (focusOwner instanceof javafx.scene.control.TextField) {
                ((javafx.scene.control.TextField) focusOwner).copy();
            } else if (focusOwner instanceof javafx.scene.control.TextArea) {
                ((javafx.scene.control.TextArea) focusOwner).copy();
            }
        });
        
        // Paste menu item with Ctrl+V
        javafx.scene.control.MenuItem pasteItem = new javafx.scene.control.MenuItem("Paste");
        pasteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.V, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> {
            // Get the focused control and perform paste if it's a text control
            javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
            if (focusOwner instanceof javafx.scene.control.TextField) {
                ((javafx.scene.control.TextField) focusOwner).paste();
            } else if (focusOwner instanceof javafx.scene.control.TextArea) {
                ((javafx.scene.control.TextArea) focusOwner).paste();
            }
        });
        
        // Undo menu item with Ctrl+Z
        javafx.scene.control.MenuItem undoItem = new javafx.scene.control.MenuItem("Undo");
        undoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.Z, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> {
            // Get the focused control and perform undo if it's a text control
            javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
            if (focusOwner instanceof javafx.scene.control.TextField) {
                ((javafx.scene.control.TextField) focusOwner).undo();
            } else if (focusOwner instanceof javafx.scene.control.TextArea) {
                ((javafx.scene.control.TextArea) focusOwner).undo();
            }
        });
        
        // Redo menu item with Ctrl+Y
        javafx.scene.control.MenuItem redoItem = new javafx.scene.control.MenuItem("Redo");
        redoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.Y, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> {
            // Get the focused control and perform redo if it's a text control
            javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
            if (focusOwner instanceof javafx.scene.control.TextField) {
                ((javafx.scene.control.TextField) focusOwner).redo();
            } else if (focusOwner instanceof javafx.scene.control.TextArea) {
                ((javafx.scene.control.TextArea) focusOwner).redo();
            }
        });
        
        // Close menu item with Ctrl+W
        javafx.scene.control.MenuItem closeItem = new javafx.scene.control.MenuItem("Close");
        closeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.W, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        closeItem.setOnAction(e -> {
            // Close the screen window
            stage.close();
        });
        
        // Add all menu items to Edit menu with separators
        editMenu.getItems().addAll(
            cutItem, 
            copyItem, 
            pasteItem, 
            new javafx.scene.control.SeparatorMenuItem(),
            undoItem, 
            redoItem,
            new javafx.scene.control.SeparatorMenuItem(),
            closeItem
        );
        menuBar.getMenus().add(editMenu);
        
        return menuBar;
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context) {
        return createAreaWithItems(areaDef, screenName, context, null, null, null);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        return createAreaWithItems(areaDef, screenName, context, screenVars, null, null);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding and onClick handler.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler) {
        List<Node> boundControls = new ArrayList<>();
        return createAreaWithItems(areaDef, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding, onClick handler, and control tracking.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            List<Node> boundControls) {
        // Create the container using AreaContainerFactory
        Region container = AreaContainerFactory.createContainer(areaDef);
        
        // Apply width/height constraints if specified
        if (areaDef.minWidth != null && !areaDef.minWidth.isEmpty()) {
            try {
                double width = parseSize(areaDef.minWidth);
                container.setMinWidth(width);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        if (areaDef.prefWidth != null && !areaDef.prefWidth.isEmpty()) {
            try {
                double width = parseSize(areaDef.prefWidth);
                container.setPrefWidth(width);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        if (areaDef.maxWidth != null && !areaDef.maxWidth.isEmpty()) {
            try {
                double width = parseSize(areaDef.maxWidth);
                container.setMaxWidth(width);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        if (areaDef.minHeight != null && !areaDef.minHeight.isEmpty()) {
            try {
                double height = parseSize(areaDef.minHeight);
                container.setMinHeight(height);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        if (areaDef.prefHeight != null && !areaDef.prefHeight.isEmpty()) {
            try {
                double height = parseSize(areaDef.prefHeight);
                container.setPrefHeight(height);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        if (areaDef.maxHeight != null && !areaDef.maxHeight.isEmpty()) {
            try {
                double height = parseSize(areaDef.maxHeight);
                container.setMaxHeight(height);
            } catch (NumberFormatException e) {
                // Ignore invalid values
            }
        }
        
        // Register area container for runtime property updates
        if (context != null && areaDef.name != null && !areaDef.name.isEmpty()) {
            // Set userData on the container for identification
            container.setUserData("area:" + screenName + "." + areaDef.name);
            // Register the container in context for later lookup by scr.setAreaProperty
            context.registerAreaContainer(screenName, areaDef.name, container);
            
            // Register the container in screenContainerTypes as Screen.container
            java.util.concurrent.ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenContainerType> containerTypes = 
                context.getScreenContainerTypes(screenName);
            if (containerTypes != null && areaDef.areaType != null) {
                String areaNameLower = areaDef.name.toLowerCase(java.util.Locale.ROOT);
                String containerTypeName = areaDef.areaType.toString().toLowerCase();
                com.eb.script.interpreter.screen.ScreenContainerType containerType = 
                    new com.eb.script.interpreter.screen.ScreenContainerType(containerTypeName, container);
                containerTypes.put(areaNameLower, containerType);
            }
        }
        
        // Log debug information for this container if debug mode is enabled
        if (isDebugMode()) {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append(String.format("Created container: %s (type: %s)", 
                areaDef.name != null ? areaDef.name : "<unnamed>",
                areaDef.areaType != null ? areaDef.areaType.toString().toLowerCase() : "<none>"));
            
            // Add groupBorder info if present
            if (areaDef.groupBorder != null && !areaDef.groupBorder.isEmpty() && !areaDef.groupBorder.equalsIgnoreCase("none")) {
                contextBuilder.append(String.format(" [groupBorder: %s", areaDef.groupBorder));
                if (areaDef.groupBorderColor != null && !areaDef.groupBorderColor.isEmpty()) {
                    contextBuilder.append(String.format(", color: %s", areaDef.groupBorderColor));
                }
                if (areaDef.groupBorderWidth != null && !areaDef.groupBorderWidth.isEmpty()) {
                    contextBuilder.append(String.format(", width: %s", areaDef.groupBorderWidth));
                }
                if (areaDef.groupBorderInsets != null && !areaDef.groupBorderInsets.isEmpty()) {
                    contextBuilder.append(String.format(", insets: %s", areaDef.groupBorderInsets));
                }
                contextBuilder.append("]");
            }
            
            logNodeDebug(container, contextBuilder.toString());
        }

        // Sort items by sequence
        if (areaDef.items != null && !areaDef.items.isEmpty()) {
            // First, expand any items if area has numberOfRecords set
            List<AreaItem> expandedItems = expandMultiRecordItems(areaDef.items, areaDef.numberOfRecords, areaDef.recordRef);
            
            List<AreaItem> sortedItems = expandedItems.stream()
                    .sorted(Comparator.comparingInt(item -> item.sequence))
                    .toList();

            // First pass: Calculate maximum label width for alignment
            double maxLabelWidth = calculateMaxLabelWidth(sortedItems, screenName, context);

            // Add items to container based on container type
            for (AreaItem item : sortedItems) {
                // Get metadata for the item
                // Start with var-level metadata (from vars section), then merge item-level metadata (from area items display)
                DisplayItem metadata = null;
                if (item.varRef != null && context != null) {
                    metadata = lookupDisplayItem(context, screenName, item.varRef);
                }
                // If item has its own display metadata, merge it (item-level overwrites var-level)
                if (item.displayItem != null) {
                    metadata = mergeDisplayMetadata(metadata, item.displayItem);
                }

                // Create the item using AreaItemFactory
                Node control = AreaItemFactory.createItem(item, metadata);
                
                // Log debug information for this item if debug mode is enabled
                if (isDebugMode()) {
                    String itemType = metadata != null && metadata.itemType != null ? metadata.itemType.toString() : "UNKNOWN";
                    String labelText = metadata != null && metadata.labelText != null ? metadata.labelText : "<no label>";
                    String itemContext = String.format("Created item: %s (type: %s, varRef: %s, label: \"%s\")", 
                        item.name != null ? item.name : "<unnamed>",
                        itemType,
                        item.varRef != null ? item.varRef : "<none>",
                        labelText);
                    logNodeDebug(control, itemContext);
                }
                
                // Store item metadata in control's user data for later retrieval by screen.setProperty/getProperty
                // Format: "screenName.itemName"
                if (item.name != null && !item.name.isEmpty()) {
                    control.setUserData(screenName + "." + item.name);
                }
                
                // Store tooltip in control's properties for status bar display
                if (item.tooltip != null && !item.tooltip.isEmpty()) {
                    control.getProperties().put("itemTooltip", item.tooltip);
                }

                // If labelText is specified, wrap the control with a label
                // BUT: Don't wrap Label or Button controls - they display their own text
                Node nodeToAdd = control;
                if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
                    // Only wrap input controls, not Label or Button which display their own text
                    if (!(control instanceof javafx.scene.control.Label)
                            && !(control instanceof javafx.scene.control.Button)) {
                        // Determine layout based on labelPosition property, defaulting to "left"
                        // For TableView default to "top" unless explicitly specified
                        String labelPos = metadata.labelPosition;
                        if (labelPos == null || labelPos.isEmpty()) {
                            labelPos = (control instanceof javafx.scene.control.TableView) ? "top" : "left";
                        }
                        nodeToAdd = createLabeledControl(metadata.labelText, metadata.labelTextAlignment, control, maxLabelWidth, metadata, labelPos, item);
                    }
                } else {
                    // No label specified - wrap control in HBox with left padding to align with labeled controls
                    // This ensures controls without labels still align properly with labeled controls
                    // Skip this wrapping if disableLabelAlignment is true (useful for grid layouts like chess boards)
                    if (!Boolean.TRUE.equals(areaDef.disableLabelAlignment)
                            && !(control instanceof javafx.scene.control.Label)
                            && !(control instanceof javafx.scene.control.Button)
                            && !(control instanceof javafx.scene.control.TableView)) {
                        
                        // Check if item has hgrow or vgrow properties
                        boolean hasHgrow = item.hgrow != null && !item.hgrow.isEmpty();
                        boolean hasVgrow = item.vgrow != null && !item.vgrow.isEmpty();
                        
                        // Choose appropriate wrapper container type based on growth properties
                        // If vgrow is set, use VBox; otherwise use HBox (default for alignment)
                        if (hasVgrow && !hasHgrow) {
                            // Use VBox for vertical growth
                            javafx.scene.layout.VBox alignmentBox = new javafx.scene.layout.VBox();
                            alignmentBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                            // Add top padding equal to label height for alignment
                            alignmentBox.setPadding(new javafx.geometry.Insets(5, 0, 0, 0));
                            alignmentBox.setPickOnBounds(false);
                            alignmentBox.getChildren().add(control);
                            // Allow control to grow within the VBox
                            javafx.scene.layout.VBox.setVgrow(control, javafx.scene.layout.Priority.ALWAYS);
                            // Store vgrow priority on wrapper
                            try {
                                Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                                alignmentBox.getProperties().put("vgrowPriority", priority);
                            } catch (IllegalArgumentException e) {
                                // Ignore invalid values
                            }
                            nodeToAdd = alignmentBox;
                        } else {
                            // Use HBox for horizontal growth or default alignment
                            javafx.scene.layout.HBox alignmentBox = new javafx.scene.layout.HBox();
                            alignmentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            // Add left padding equal to label width plus spacing to align with labeled controls
                            alignmentBox.setPadding(new javafx.geometry.Insets(0, 0, 0, maxLabelWidth + 5));
                            alignmentBox.setPickOnBounds(false);
                            alignmentBox.getChildren().add(control);
                            
                            // If item has hgrow, configure the wrapper accordingly
                            if (hasHgrow) {
                                // Allow control to grow within the HBox
                                javafx.scene.layout.HBox.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);
                                alignmentBox.setMaxWidth(Double.MAX_VALUE);
                                // Store hgrow priority on wrapper
                                try {
                                    Priority priority = Priority.valueOf(item.hgrow.toUpperCase());
                                    alignmentBox.getProperties().put("hgrowPriority", priority);
                                } catch (IllegalArgumentException e) {
                                    // Ignore invalid values
                                }
                            }
                            
                            // If item has vgrow as well, store it on the wrapper
                            if (hasVgrow) {
                                try {
                                    Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                                    alignmentBox.getProperties().put("vgrowPriority", priority);
                                } catch (IllegalArgumentException e) {
                                    // Ignore invalid values
                                }
                            }
                            
                            nodeToAdd = alignmentBox;
                        }
                    }
                }

                // Set up onClick handler for buttons and other controls
                if (onClickHandler != null && metadata != null && metadata.onClick != null && !metadata.onClick.isEmpty()) {
                    String ebsCode = metadata.onClick;
                    if (control instanceof javafx.scene.control.Button) {
                        javafx.scene.control.Button button = (javafx.scene.control.Button) control;
                        button.setOnAction(event -> {
                            try {
                                // Use executeDirect to run code on JavaFX thread
                                // This avoids deadlocks when the code shows dialogs
                                onClickHandler.executeDirect(ebsCode);
                                // After executing the onClick code, refresh all bound controls
                                refreshBoundControls(boundControls, screenVars);
                            } catch (InterpreterError e) {
                                // Print error to console if available
                                System.err.println("Error executing button onClick: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    } else if (control instanceof javafx.scene.image.ImageView) {
                        // Set up onClick handler for ImageView
                        javafx.scene.image.ImageView imageView = (javafx.scene.image.ImageView) control;
                        imageView.setOnMouseClicked(event -> {
                            try {
                                // Use executeDirect to run code on JavaFX thread
                                onClickHandler.executeDirect(ebsCode);
                                // After executing the onClick code, refresh all bound controls
                                refreshBoundControls(boundControls, screenVars);
                            } catch (InterpreterError e) {
                                System.err.println("Error executing image onClick: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    } else {
                        // Generic onClick handler for other controls using mouse click event
                        control.setOnMouseClicked(event -> {
                            try {
                                onClickHandler.executeDirect(ebsCode);
                                refreshBoundControls(boundControls, screenVars);
                            } catch (InterpreterError e) {
                                System.err.println("Error executing onClick: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    }
                }

                // Set up two-way data binding if screenVars is provided and item has a varRef
                if (screenVars != null && item.varRef != null) {
                    setupVariableBinding(control, item.varRef, screenVars, varTypes, metadata);
                    // Store context and screenName for screen status updates when control values change
                    control.getProperties().put(ControlListenerFactory.PROP_INTERPRETER_CONTEXT, context);
                    control.getProperties().put(ControlListenerFactory.PROP_SCREEN_NAME, screenName);
                    // Track this control so we can refresh it when variables change
                    boundControls.add(control);
                    
                    // Store the JavaFX Node reference in the ScreenComponentType
                    if (context != null) {
                        java.util.concurrent.ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenComponentType> componentTypes = 
                            context.getScreenComponentTypes(screenName);
                        if (componentTypes != null) {
                            String varNameLower = item.varRef.toLowerCase(java.util.Locale.ROOT);
                            com.eb.script.interpreter.screen.ScreenComponentType componentType = componentTypes.get(varNameLower);
                            if (componentType != null) {
                                componentType.setJavaFXNode(control);
                            }
                        }
                    }
                }
                
                // Also add controls with names (like buttons) to boundControls even without varRef
                // This allows scr.setProperty to find and modify them (e.g., disable buttons)
                if (item.name != null && !item.name.isEmpty() && item.varRef == null) {
                    boundControls.add(control);
                }
                
                // Set up onValidate handler for input controls
                String validateCode = item.onValidate;
                if (validateCode == null && metadata != null) {
                    validateCode = metadata.onValidate;
                }
                if (onClickHandler != null && validateCode != null && !validateCode.isEmpty()) {
                    setupValidationHandler(control, validateCode, onClickHandler, screenName, context);
                }
                
                // Store item name on control for event debugging
                if (item.name != null) {
                    control.getProperties().put("itemName", item.name);
                }
                
                // Set up onChange handler for input controls
                String changeCode = item.onChange;
                if (changeCode == null && metadata != null) {
                    changeCode = metadata.onChange;
                }
                if (onClickHandler != null && changeCode != null && !changeCode.isEmpty()) {
                    setupChangeHandler(control, changeCode, onClickHandler, screenName, context, boundControls, screenVars);
                }

                // Apply item layout properties
                applyItemLayoutProperties(control, item);
                
                // If the control was wrapped (e.g., with a label), also apply width constraints to the wrapper
                // This ensures the wrapper container respects the same width limits as the control inside
                if (nodeToAdd != control && nodeToAdd instanceof Region) {
                    Region wrapper = (Region) nodeToAdd;
                    // Only apply width constraints to the wrapper, not height (height should be flexible for label+control)
                    if (item.minWidth != null && !item.minWidth.isEmpty()) {
                        try {
                            double width = parseSize(item.minWidth);
                            if (width > 0) {
                                wrapper.setMinWidth(width);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                    if (item.prefWidth != null && !item.prefWidth.isEmpty()) {
                        try {
                            double width = parseSize(item.prefWidth);
                            if (width > 0) {
                                wrapper.setPrefWidth(width);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                    if (item.maxWidth != null && !item.maxWidth.isEmpty()) {
                        try {
                            double width = parseSize(item.maxWidth);
                            if (width > 0) {
                                wrapper.setMaxWidth(width);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }

                // Add item to container based on container type
                addItemToContainer(container, nodeToAdd, item, areaDef.areaType);
                
                // Log debug information for this item if debug mode is enabled
                if (isDebugMode()) {
                    String itemContext = String.format("Created item: %s (varRef: %s) in area: %s", 
                        item.name != null ? item.name : "<unnamed>",
                        item.varRef != null ? item.varRef : "<none>",
                        areaDef.name != null ? areaDef.name : "<unnamed>");
                    logNodeDebug(nodeToAdd, itemContext);
                }
            }
        }

        // Add nested child areas to the container
        if (areaDef.childAreas != null && !areaDef.childAreas.isEmpty()) {
            for (AreaDefinition childArea : areaDef.childAreas) {
                // Special handling for Tab areas when parent is TabPane
                if (areaDef.areaType == AreaType.TABPANE && childArea.areaType == AreaType.TAB) {
                    // Tab should contain its child areas directly, not wrapped in an extra container
                    // Process the Tab's child areas to get the actual content
                    Region tabContent;
                    
                    if (childArea.childAreas != null && !childArea.childAreas.isEmpty()) {
                        // If Tab has multiple child areas, create a VBox to hold them
                        if (childArea.childAreas.size() == 1) {
                            // Single child area - use it directly
                            tabContent = createAreaWithItems(childArea.childAreas.get(0), screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                        } else {
                            // Multiple child areas - wrap in VBox
                            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
                            for (AreaDefinition tabChildArea : childArea.childAreas) {
                                Region childContent = createAreaWithItems(tabChildArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                                vbox.getChildren().add(childContent);
                            }
                            tabContent = vbox;
                        }
                    } else if (childArea.items != null && !childArea.items.isEmpty()) {
                        // Tab has items directly (no child areas)
                        tabContent = createAreaWithItems(childArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                    } else {
                        // Empty tab - create empty pane
                        tabContent = new javafx.scene.layout.Pane();
                    }
                    
                    // Ensure tab content has transparent background
                    if (tabContent.getStyle() == null || tabContent.getStyle().isEmpty()) {
                        tabContent.setStyle("-fx-background-color: transparent;");
                    } else if (!tabContent.getStyle().contains("-fx-background-color")) {
                        tabContent.setStyle(tabContent.getStyle() + " -fx-background-color: transparent;");
                    }
                    
                    // Wrap tab content in ScrollPane for automatic scrollbars when content is larger than tab
                    ScrollPane scrollPane = new ScrollPane(tabContent);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setFitToHeight(false); // Allow vertical scrolling when content exceeds viewport
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    
                    Tab tab = new Tab();
                    tab.setText(childArea.displayName != null ? childArea.displayName : childArea.name);
                    tab.setContent(scrollPane);
                    tab.setClosable(false); // Tabs not closable by default
                    
                    if (container instanceof TabPane) {
                        ((TabPane) container).getTabs().add(tab);
                    }
                } else {
                    // Normal nested area handling
                    Region childContainer = createAreaWithItems(childArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                    
                    // Add the child container to the parent container
                    // Treat child areas as regular nodes
                    addChildAreaToContainer(container, childContainer, areaDef.areaType, childArea);
                }
            }
        }

        // Set up area-level focus listeners if gainFocus or lostFocus code is defined
        if ((areaDef.gainFocus != null && !areaDef.gainFocus.trim().isEmpty()) 
            || (areaDef.lostFocus != null && !areaDef.lostFocus.trim().isEmpty())) {
            setupAreaFocusListeners(container, areaDef, screenName, context, onClickHandler);
        }
        
        // Configure SplitPane divider position if this is a SplitPane
        if (container instanceof javafx.scene.control.SplitPane) {
            javafx.scene.control.SplitPane splitPane = (javafx.scene.control.SplitPane) container;
            // Set initial divider position to 25% (0.25) for the first pane
            splitPane.setDividerPositions(0.25);
        }

        return container;
    }
    
    /**
     * Sets up focus listeners for an area container to execute gainFocus/lostFocus inline code.
     * Focus is tracked at the container level - gainFocus triggers when a node inside the area gains focus
     * and the previous focused node was outside the area (or null). lostFocus triggers when focus moves
     * from inside the area to outside the area.
     */
    private static void setupAreaFocusListeners(Region container, AreaDefinition areaDef, String screenName, InterpreterContext context, OnClickHandler onClickHandler) {
        // Track whether this area currently has focus
        final boolean[] areaHasFocus = new boolean[]{false};
        
        // Add a listener to the scene's focused property
        container.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.focusOwnerProperty().addListener((obs, oldNode, newNode) -> {
                    // Check if the new focused node is inside this area
                    boolean newNodeInArea = isNodeInContainer(newNode, container);
                    // Check if the old focused node was inside this area
                    boolean oldNodeInArea = isNodeInContainer(oldNode, container);
                    
                    // Trigger gainFocus if focus entered the area
                    if (newNodeInArea && !oldNodeInArea && !areaHasFocus[0]) {
                        areaHasFocus[0] = true;
                        if (areaDef.gainFocus != null && !areaDef.gainFocus.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                try {
                                    executeAreaInlineCode(screenName, areaDef.gainFocus, "area_gainFocus", context, onClickHandler);
                                } catch (InterpreterError e) {
                                    if (context.getOutput() != null) {
                                        context.getOutput().printlnError("Error executing area gainFocus code: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                    
                    // Trigger lostFocus if focus left the area
                    if (!newNodeInArea && oldNodeInArea && areaHasFocus[0]) {
                        areaHasFocus[0] = false;
                        if (areaDef.lostFocus != null && !areaDef.lostFocus.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                try {
                                    executeAreaInlineCode(screenName, areaDef.lostFocus, "area_lostFocus", context, onClickHandler);
                                } catch (InterpreterError e) {
                                    if (context.getOutput() != null) {
                                        context.getOutput().printlnError("Error executing area lostFocus code: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }
    
    /**
     * Checks if a node is a descendant of a container.
     */
    private static boolean isNodeInContainer(Node node, Region container) {
        if (node == null || container == null) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current == container) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
    
    /**
     * Executes area inline code with proper screen context using the onClick handler.
     * Uses executeDirect to run on the calling thread and avoid deadlocks with dialogs.
     */
    private static void executeAreaInlineCode(String screenName, String ebsCode, String eventType, InterpreterContext context, OnClickHandler onClickHandler) throws InterpreterError {
        if (onClickHandler != null) {
            onClickHandler.executeDirect(ebsCode);
        }
    }

    /**
     * Applies layout properties from AreaItem to the control. These are the
     * properties NOT applied by AreaItemFactory (which only applies display
     * properties).
     */
    private static void applyItemLayoutProperties(Node control, AreaItem item) {
        // Apply sizing properties
        if (control instanceof Region) {
            Region region = (Region) control;

            if (item.prefWidth != null && !item.prefWidth.isEmpty()) {
                try {
                    double width = parseSize(item.prefWidth);
                    if (width > 0) {
                        region.setPrefWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.prefHeight != null && !item.prefHeight.isEmpty()) {
                try {
                    double height = parseSize(item.prefHeight);
                    if (height > 0) {
                        region.setPrefHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.minWidth != null && !item.minWidth.isEmpty()) {
                try {
                    double width = parseSize(item.minWidth);
                    if (width > 0) {
                        region.setMinWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.minHeight != null && !item.minHeight.isEmpty()) {
                try {
                    double height = parseSize(item.minHeight);
                    if (height > 0) {
                        region.setMinHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.maxWidth != null && !item.maxWidth.isEmpty()) {
                try {
                    double width = parseSize(item.maxWidth);
                    if (width > 0) {
                        region.setMaxWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.maxHeight != null && !item.maxHeight.isEmpty()) {
                try {
                    double height = parseSize(item.maxHeight);
                    if (height > 0) {
                        region.setMaxHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
        }

        // Apply margin
        if (item.margin != null && !item.margin.isEmpty()) {
            Insets margin = parseInsets(item.margin);
            if (margin != null) {
                VBox.setMargin(control, margin);
                HBox.setMargin(control, margin);
                BorderPane.setMargin(control, margin);
                GridPane.setMargin(control, margin);
                StackPane.setMargin(control, margin);
                FlowPane.setMargin(control, margin);
            }
        }
    }

    /**
     * Adds an item to a container based on the container type.
     * If the control is a wrapper container (created by createLabeledControl) with growth
     * properties stored, those properties are applied to the parent container.
     */
    private static void addItemToContainer(Region container, Node control, AreaItem item, AreaType areaType) {
        if (container instanceof VBox) {
            VBox vbox = (VBox) container;
            vbox.getChildren().add(control);

            // Check if the control (wrapper) has a stored vgrowPriority
            Priority vgrowPriority = null;
            if (control.getProperties().containsKey("vgrowPriority")) {
                vgrowPriority = (Priority) control.getProperties().get("vgrowPriority");
            } else if (item.vgrow != null && !item.vgrow.isEmpty()) {
                // Fall back to item's vgrow property if no wrapper priority is set
                try {
                    vgrowPriority = Priority.valueOf(item.vgrow.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            // Apply vgrow priority to the control in the VBox
            if (vgrowPriority != null) {
                VBox.setVgrow(control, vgrowPriority);
            }

        } else if (container instanceof HBox) {
            HBox hbox = (HBox) container;
            hbox.getChildren().add(control);

            // Check if the control (wrapper) has a stored hgrowPriority
            Priority hgrowPriority = null;
            if (control.getProperties().containsKey("hgrowPriority")) {
                hgrowPriority = (Priority) control.getProperties().get("hgrowPriority");
            } else if (item.hgrow != null && !item.hgrow.isEmpty()) {
                // Fall back to item's hgrow property if no wrapper priority is set
                try {
                    hgrowPriority = Priority.valueOf(item.hgrow.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            // Apply hgrow priority to the control in the HBox
            if (hgrowPriority != null) {
                HBox.setHgrow(control, hgrowPriority);
            }

        } else if (container instanceof GridPane) {
            GridPane gridPane = (GridPane) container;

            // Parse layoutPos for row, col
            int row = 0;
            int col = 0;
            if (item.layoutPos != null && !item.layoutPos.isEmpty()) {
                String[] parts = item.layoutPos.split(",");
                if (parts.length >= 2) {
                    try {
                        row = Integer.parseInt(parts[0].trim());
                        col = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        // Use default 0,0
                    }
                }
            }

            gridPane.add(control, col, row);

            // Apply column and row span
            if (item.colSpan != null && item.colSpan > 1) {
                GridPane.setColumnSpan(control, item.colSpan);
            }
            if (item.rowSpan != null && item.rowSpan > 1) {
                GridPane.setRowSpan(control, item.rowSpan);
            }

            // Apply grid grow priorities
            // Check if the control (wrapper) has stored priorities first
            Priority hgrowPriority = null;
            Priority vgrowPriority = null;
            
            if (control.getProperties().containsKey("hgrowPriority")) {
                hgrowPriority = (Priority) control.getProperties().get("hgrowPriority");
            } else if (item.hgrow != null && !item.hgrow.isEmpty()) {
                try {
                    hgrowPriority = Priority.valueOf(item.hgrow.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            if (control.getProperties().containsKey("vgrowPriority")) {
                vgrowPriority = (Priority) control.getProperties().get("vgrowPriority");
            } else if (item.vgrow != null && !item.vgrow.isEmpty()) {
                try {
                    vgrowPriority = Priority.valueOf(item.vgrow.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            // Apply the priorities to the control in the GridPane
            if (hgrowPriority != null) {
                GridPane.setHgrow(control, hgrowPriority);
            }
            if (vgrowPriority != null) {
                GridPane.setVgrow(control, vgrowPriority);
            }

        } else if (container instanceof BorderPane) {
            BorderPane borderPane = (BorderPane) container;

            // Parse layoutPos for position (top, bottom, left, right, center)
            String position = item.layoutPos != null ? item.layoutPos.toLowerCase() : "center";

            switch (position) {
                case "top":
                    borderPane.setTop(control);
                    break;
                case "bottom":
                    borderPane.setBottom(control);
                    break;
                case "left":
                    borderPane.setLeft(control);
                    break;
                case "right":
                    borderPane.setRight(control);
                    break;
                case "center":
                default:
                    borderPane.setCenter(control);
                    break;
            }

        } else if (container instanceof StackPane) {
            StackPane stackPane = (StackPane) container;
            stackPane.getChildren().add(control);

            // Apply alignment if specified
            if (item.alignment != null && !item.alignment.isEmpty()) {
                try {
                    Pos pos = parseAlignment(item.alignment);
                    StackPane.setAlignment(control, pos);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

        } else if (container instanceof FlowPane) {
            FlowPane flowPane = (FlowPane) container;
            flowPane.getChildren().add(control);

        } else if (container instanceof TilePane) {
            TilePane tilePane = (TilePane) container;
            tilePane.getChildren().add(control);

        } else if (container instanceof AnchorPane) {
            AnchorPane anchorPane = (AnchorPane) container;
            anchorPane.getChildren().add(control);

            // Parse layoutPos for anchor constraints (e.g., "10,20,30,40" for top,right,bottom,left)
            if (item.layoutPos != null && !item.layoutPos.isEmpty()) {
                String[] parts = item.layoutPos.split(",");
                if (parts.length >= 4) {
                    try {
                        double top = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        double bottom = Double.parseDouble(parts[2].trim());
                        double left = Double.parseDouble(parts[3].trim());

                        if (top >= 0) {
                            AnchorPane.setTopAnchor(control, top);
                        }
                        if (right >= 0) {
                            AnchorPane.setRightAnchor(control, right);
                        }
                        if (bottom >= 0) {
                            AnchorPane.setBottomAnchor(control, bottom);
                        }
                        if (left >= 0) {
                            AnchorPane.setLeftAnchor(control, left);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }

        } else if (container instanceof Pane) {
            // Generic Pane - just add the control
            ((Pane) container).getChildren().add(control);
        }
    }

    /**
     * Adds a child area (nested container) to a parent container. Similar to
     * addItemToContainer but for child areas.
     */
    private static void addChildAreaToContainer(Region container, Region childArea, AreaType areaType, AreaDefinition childAreaDef) {
        if (container instanceof VBox) {
            ((VBox) container).getChildren().add(childArea);
            
            // Apply VBox-specific grow properties from child area definition
            if (childAreaDef != null && childAreaDef.vgrow != null && !childAreaDef.vgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(childAreaDef.vgrow.toUpperCase());
                    VBox.setVgrow(childArea, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
        } else if (container instanceof HBox) {
            ((HBox) container).getChildren().add(childArea);
            
            // Apply HBox-specific grow properties from child area definition
            if (childAreaDef != null && childAreaDef.hgrow != null && !childAreaDef.hgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(childAreaDef.hgrow.toUpperCase());
                    HBox.setHgrow(childArea, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
        } else if (container instanceof GridPane) {
            // For GridPane, just add to next available position
            ((GridPane) container).getChildren().add(childArea);
            
            // Apply GridPane grow properties from child area definition
            if (childAreaDef != null) {
                if (childAreaDef.hgrow != null && !childAreaDef.hgrow.isEmpty()) {
                    try {
                        Priority priority = Priority.valueOf(childAreaDef.hgrow.toUpperCase());
                        GridPane.setHgrow(childArea, priority);
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid values
                    }
                }
                if (childAreaDef.vgrow != null && !childAreaDef.vgrow.isEmpty()) {
                    try {
                        Priority priority = Priority.valueOf(childAreaDef.vgrow.toUpperCase());
                        GridPane.setVgrow(childArea, priority);
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid values
                    }
                }
            }
        } else if (container instanceof BorderPane) {
            // For BorderPane, default to center if not specified
            BorderPane borderPane = (BorderPane) container;
            if (borderPane.getCenter() == null) {
                borderPane.setCenter(childArea);
            }
        } else if (container instanceof javafx.scene.control.SplitPane) {
            javafx.scene.control.SplitPane splitPane = (javafx.scene.control.SplitPane) container;
            splitPane.getItems().add(childArea);
            // Note: SplitPane doesn't use hgrow/vgrow properties - divider position controls sizing
        } else if (container instanceof StackPane) {
            ((StackPane) container).getChildren().add(childArea);
        } else if (container instanceof FlowPane) {
            ((FlowPane) container).getChildren().add(childArea);
        } else if (container instanceof TilePane) {
            ((TilePane) container).getChildren().add(childArea);
        } else if (container instanceof AnchorPane) {
            ((AnchorPane) container).getChildren().add(childArea);
        } else if (container instanceof Pane) {
            ((Pane) container).getChildren().add(childArea);
        }
    }

    /**
     * Parse size string to double value. Supports plain numbers and percentages
     * (e.g., "300", "50%").
     */
    private static double parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return -1;
        }

        size = size.trim();

        if (size.endsWith("%")) {
            // Percentage - not directly supported, return -1
            return -1;
        } else if (size.equalsIgnoreCase("auto")) {
            return -1;
        } else if (size.equalsIgnoreCase("MAX")) {
            // "MAX" means use maximum possible size
            return Double.MAX_VALUE;
        } else {
            return Double.parseDouble(size);
        }
    }

    /**
     * Parse insets string to Insets object. Supports formats: "10" (all), "10
     * 5" (vertical horizontal), "10 5 10 5" (top right bottom left).
     */
    private static Insets parseInsets(String insetsStr) {
        if (insetsStr == null || insetsStr.isEmpty()) {
            return null;
        }

        String[] parts = insetsStr.trim().split("\\s+");

        try {
            if (parts.length == 1) {
                double all = Double.parseDouble(parts[0]);
                return new Insets(all);
            } else if (parts.length == 2) {
                double vertical = Double.parseDouble(parts[0]);
                double horizontal = Double.parseDouble(parts[1]);
                return new Insets(vertical, horizontal, vertical, horizontal);
            } else if (parts.length == 4) {
                double top = Double.parseDouble(parts[0]);
                double right = Double.parseDouble(parts[1]);
                double bottom = Double.parseDouble(parts[2]);
                double left = Double.parseDouble(parts[3]);
                return new Insets(top, right, bottom, left);
            }
        } catch (NumberFormatException e) {
            // Return null for invalid format
        }

        return null;
    }

    /**
     * Parse alignment string to JavaFX Pos enum.
     */
    private static Pos parseAlignment(String alignment) {
        if (alignment == null || alignment.isEmpty()) {
            return Pos.CENTER;
        }

        String normalized = alignment.toUpperCase().replace("-", "_").replace(" ", "_");

        try {
            return Pos.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try common variations
            switch (normalized) {
                case "LEFT":
                    return Pos.CENTER_LEFT;
                case "RIGHT":
                    return Pos.CENTER_RIGHT;
                case "TOP":
                    return Pos.TOP_CENTER;
                case "BOTTOM":
                    return Pos.BOTTOM_CENTER;
                case "CENTER":
                default:
                    return Pos.CENTER;
            }
        }
    }

    /**
     * Convenience method to create and show a screen on the JavaFX Application
     * Thread.
     */
    public static void createAndShowScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context,
            boolean maximize) {
        // Capture debug mode state from current thread before switching to JavaFX thread
        boolean debugModeEnabled = getDebugModeForInheritance();
        
        Platform.runLater(() -> {
            // Set debug mode on JavaFX Application Thread
            setDebugModeForThread(debugModeEnabled);
            
            Stage stage = createScreen(screenName, title, width, height, areas, context);

            if (maximize) {
                stage.setMaximized(true);
            }

            stage.show();
        });
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen
     * definition (JSON/EBS format). This method parses the screen definition
     * and creates the window. Validates the screen definition against the JSON
     * schema if validation is enabled.
     *
     * @param screenDef Map containing screen definition with keys: name, title,
     * width, height, vars, area
     * @return A Stage representing the complete window
     * @throws IllegalArgumentException if the screen definition is invalid
     */
    public static Stage createScreenFromDefinition(Map<String, Object> screenDef) {
        return createScreenFromDefinition(screenDef, true);
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen
     * definition (JSON/EBS format). This method parses the screen definition
     * and creates the window.
     *
     * @param screenDef Map containing screen definition with keys: name, title,
     * width, height, vars, area
     * @param validate Whether to validate against JSON schema
     * @return A Stage representing the complete window
     * @throws IllegalArgumentException if the screen definition is invalid
     */
    public static Stage createScreenFromDefinition(Map<String, Object> screenDef, boolean validate) {
        // Validate screen definition if requested and schema is available
        if (validate && screenSchema != null) {
            validateScreenDefinition(screenDef);
        }

        // Extract screen properties
        String screenName = getStringValue(screenDef, "name", "Screen");
        String title = getStringValue(screenDef, "title", screenName);
        double width = getNumberValue(screenDef, "width", 800.0);
        double height = getNumberValue(screenDef, "height", 600.0);

        // Parse variables and build metadata map
        Map<String, DisplayItem> metadataMap = new HashMap<>();
        if (screenDef.containsKey("vars")) {
            Object varsObj = screenDef.get("vars");
            if (varsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> varsList = (List<Object>) varsObj;
                for (Object varObj : varsList) {
                    if (varObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> varDef = (Map<String, Object>) varObj;
                        String varName = getStringValue(varDef, "name", null);
                        if (varName != null && varDef.containsKey("display")) {
                            Object displayObj = varDef.get("display");
                            if (displayObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> displayDef = (Map<String, Object>) displayObj;
                                DisplayItem metadata = parseDisplayItem(displayDef, screenName);
                                metadataMap.put(varName, metadata);
                            }
                        }
                    }
                }
            }
        }

        // Parse areas
        List<AreaDefinition> areas = new ArrayList<>();
        if (screenDef.containsKey("area")) {
            Object areaObj = screenDef.get("area");
            if (areaObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> areaList = (List<Object>) areaObj;
                for (Object areaDef : areaList) {
                    if (areaDef instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> areaDefMap = (Map<String, Object>) areaDef;
                        AreaDefinition area = parseAreaDefinition(areaDefMap, screenName);
                        areas.add(area);
                    }
                }
            }
        }

        // Create a temporary context and populate it with metadata
        InterpreterContext tempContext = new InterpreterContext();
        for (Map.Entry<String, DisplayItem> entry : metadataMap.entrySet()) {
            tempContext.getDisplayItem().put(screenName + "." + entry.getKey(), entry.getValue());
        }

        return createScreen(screenName, title, width, height, areas, tempContext);
    }

    /**
     * Parses an AreaDefinition from a Map.
     */
    private static AreaDefinition parseAreaDefinition(Map<String, Object> areaDef, String screenName) {
        AreaDefinition area = new AreaDefinition();

        // Extract area name (required)
        area.name = getStringValue(areaDef, "name", "area");

        // Extract area type and convert to enum
        String typeStr = getStringValue(areaDef, "type", "pane");
        area.type = typeStr.toLowerCase();
        area.areaType = AreaType.fromString(area.type);

        // Set CSS class from enum
        area.cssClass = area.areaType.getCssClass();

        // Extract layout configuration
        area.layout = getStringValue(areaDef, "layout", null);

        // Extract or set default style
        area.style = getStringValue(areaDef, "style", area.areaType.getDefaultStyle());

        area.screenName = screenName;
        
        // Extract displayName for UI labels (e.g., tab labels)
        area.displayName = getStringValue(areaDef, "displayName", null);
        
        // Extract spacing between children (for containers that support it)
        area.spacing = getStringValue(areaDef, "spacing", null);
        
        // Extract padding inside the area
        area.padding = getStringValue(areaDef, "padding", null);

        // Process items in the area
        if (areaDef.containsKey("items")) {
            Object itemsObj = areaDef.get("items");
            if (itemsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> itemsList = (List<Object>) itemsObj;

                for (Object itemObj : itemsList) {
                    if (itemObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemDef = (Map<String, Object>) itemObj;
                        AreaItem item = parseAreaItem(itemDef, screenName);
                        area.items.add(item);
                    }
                }
            }
        }

        // Process nested child areas (areas within areas)
        if (areaDef.containsKey("areas")) {
            Object areasObj = areaDef.get("areas");
            if (areasObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> areasList = (List<Object>) areasObj;

                for (Object childAreaObj : areasList) {
                    if (childAreaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> childAreaDef = (Map<String, Object>) childAreaObj;
                        AreaDefinition childArea = parseAreaDefinition(childAreaDef, screenName);
                        area.childAreas.add(childArea);
                    }
                }
            }
        }

        // Extract grow properties for layout management
        area.hgrow = getStringValue(areaDef, "hgrow", null);
        area.vgrow = getStringValue(areaDef, "vgrow", null);
        
        // Extract width/height constraints
        area.minWidth = getStringValue(areaDef, "minWidth", getStringValue(areaDef, "min_width", null));
        area.prefWidth = getStringValue(areaDef, "prefWidth", getStringValue(areaDef, "pref_width", null));
        area.maxWidth = getStringValue(areaDef, "maxWidth", getStringValue(areaDef, "max_width", null));
        area.minHeight = getStringValue(areaDef, "minHeight", getStringValue(areaDef, "min_height", null));
        area.prefHeight = getStringValue(areaDef, "prefHeight", getStringValue(areaDef, "pref_height", null));
        area.maxHeight = getStringValue(areaDef, "maxHeight", getStringValue(areaDef, "max_height", null));

        return area;
    }

    /**
     * Validates that only valid properties are present in a display definition.
     * Throws RuntimeException if invalid properties (like hgrow, vgrow) are found.
     */
    private static void validateDisplayProperties(Map<String, Object> displayDef, String screenName) {
        // Define valid display properties (including snake_case variants)
        java.util.Set<String> validProps = new java.util.HashSet<>(java.util.Arrays.asList(
            // Core display properties
            "type", "mandatory", "case", "caseformat", "alignment", "pattern",
            "min", "max", "style", "screenname",
            // Text and label properties
            "prompthelp", "prompt_help", "labeltext", "label_text",
            "labeltextalignment", "label_text_alignment", "labelposition", "label_position",
            // Event handlers
            "onclick", "on_click", "onvalidate", "on_validate", "onchange", "on_change",
            // Options and data
            "options", "columns", "displayrecords", "display_records",
            // Tree properties
            "treeitems", "tree_items", "expandall", "expand_all", "showroot", "show_root",
            // Styling properties
            "labelcolor", "label_color", "labelbold", "label_bold", 
            "labelitalic", "label_italic", "labelfontsize", "label_font_size",
            "itemfontsize", "item_font_size", "itemcolor", "item_color",
            "itembold", "item_bold", "itemitalic", "item_italic",
            "maxlength", "max_length", "height",
            // Image properties
            "fitwidth", "fit_width", "fitheight", "fit_height",
            "preserveratio", "preserve_ratio", "smooth", "scalemode", "scale_mode",
            // Slider properties
            "showslidervalue", "show_slider_value",
            // Sequence
            "seq", "sequence",
            // Data source
            "source", "status"
        ));
        
        // Properties that should NOT be in display (they belong to AreaItem)
        java.util.Set<String> itemOnlyProps = new java.util.HashSet<>(java.util.Arrays.asList(
            "hgrow", "vgrow", "margin", "padding",
            "prefwidth", "pref_width", "prefheight", "pref_height",
            "minwidth", "min_width", "minheight", "min_height",
            "maxwidth", "max_width", "maxheight", "max_height",
            "colspan", "col_span", "rowspan", "row_span",
            "layoutpos", "layout_pos", "relativepos", "relative_pos",
            "varref", "var_ref", "name", "editable", "disabled", "visible", "tooltip",
            "textcolor", "text_color", "backgroundcolor", "background_color"
        ));
        
        // Check for invalid properties
        for (String key : displayDef.keySet()) {
            String lowerKey = key.toLowerCase();
            if (itemOnlyProps.contains(lowerKey)) {
                throw new RuntimeException(
                    String.format("Invalid property '%s' in display definition for screen '%s'. " +
                                "This property belongs at the item level, not in the display object. " +
                                "Move '%s' outside of the 'display' object to the item level.",
                                key, screenName, key)
                );
            }
            if (!validProps.contains(lowerKey)) {
                // Give a warning for unknown properties (might be custom or future properties)
                System.err.println(
                    String.format("Warning: Unknown property '%s' in display definition for screen '%s'. " +
                                "This property may be ignored.",
                                key, screenName)
                );
            }
        }
    }

    /**
     * Validates that only valid properties are present in an area item definition.
     * Throws RuntimeException if invalid properties are found.
     */
    private static void validateAreaItemProperties(Map<String, Object> itemDef, String screenName) {
        // Define valid item properties (including snake_case variants)
        java.util.Set<String> validProps = new java.util.HashSet<>(java.util.Arrays.asList(
            // Core item properties
            "name", "sequence", "seq", "layoutpos", "layout_pos", "relativepos", "relative_pos",
            "varref", "var_ref", "display", "type",
            // UI behavior properties
            "editable", "disabled", "visible", "tooltip",
            "textcolor", "text_color", "backgroundcolor", "background_color",
            // Layout properties
            "colspan", "col_span", "rowspan", "row_span",
            "hgrow", "vgrow", "margin", "padding",
            "prefwidth", "pref_width", "prefheight", "pref_height",
            "minwidth", "min_width", "minheight", "min_height",
            "maxwidth", "max_width", "maxheight", "max_height",
            "alignment",
            // Event handlers (can be at item or display level)
            "onvalidate", "on_validate", "onchange", "on_change",
            // Data source
            "source",
            // promptHelp can be at item level (gets moved to displayItem)
            "prompthelp", "prompt_help",
            // Label properties (can be at item level for override/merge behavior)
            "labeltext", "label_text", "labeltextalignment", "label_text_alignment",
            "labelposition", "label_position",
            // Styling properties (can be at item level for override/merge behavior)
            "labelcolor", "label_color", "labelbold", "label_bold",
            "labelitalic", "label_italic", "labelfontsize", "label_font_size",
            "itemfontsize", "item_font_size", "itemcolor", "item_color",
            "itembold", "item_bold", "itemitalic", "item_italic"
        ));
        
        // Properties that should NOT be at item level (they belong in display object)
        java.util.Set<String> displayOnlyProps = new java.util.HashSet<>(java.util.Arrays.asList(
            "mandatory", "case", "caseformat", "pattern", "min", "max",
            "onclick", "on_click",
            "options", "columns", "displayrecords", "display_records",
            "treeitems", "tree_items", "expandall", "expand_all", "showroot", "show_root",
            "maxlength", "max_length", "height",
            "fitwidth", "fit_width", "fitheight", "fit_height",
            "preserveratio", "preserve_ratio", "smooth", "scalemode", "scale_mode",
            "showslidervalue", "show_slider_value"
        ));
        
        // Check for invalid properties
        for (String key : itemDef.keySet()) {
            String lowerKey = key.toLowerCase();
            // Skip the 'display' object itself
            if (lowerKey.equals("display")) {
                continue;
            }
            // Skip 'type' as it can be at item level for convenience
            if (lowerKey.equals("type")) {
                continue;
            }
            if (displayOnlyProps.contains(lowerKey)) {
                throw new RuntimeException(
                    String.format("Invalid property '%s' at item level for screen '%s'. " +
                                "This property belongs in the 'display' object, not at the item level. " +
                                "Move '%s' inside the 'display' object.",
                                key, screenName, key)
                );
            }
            if (!validProps.contains(lowerKey)) {
                // Give a warning for unknown properties (might be custom or future properties)
                System.err.println(
                    String.format("Warning: Unknown property '%s' at item level for screen '%s'. " +
                                "This property may be ignored.",
                                key, screenName)
                );
            }
        }
    }

    /**
     * Parses an AreaItem from a Map.
     */
    private static AreaItem parseAreaItem(Map<String, Object> itemDef, String screenName) {
        AreaItem item = new AreaItem();

        // Extract item properties
        item.name = getStringValue(itemDef, "name", null);
        // Support both "sequence" and "seq" for compactness
        item.sequence = getIntValue(itemDef, "sequence", getIntValue(itemDef, "seq", 0));
        item.varRef = getStringValue(itemDef, "varRef", getStringValue(itemDef, "var_ref", null));

        // Layout position (support multiple naming conventions)
        item.layoutPos = getStringValue(itemDef, "layoutPos",
                getStringValue(itemDef, "layout_pos",
                        getStringValue(itemDef, "relativePos",
                                getStringValue(itemDef, "relative_pos", null))));

        // Process optional display metadata for the item
        if (itemDef.containsKey("display")) {
            Object displayObj = itemDef.get("display");
            if (displayObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> displayDef = (Map<String, Object>) displayObj;
                item.displayItem = parseDisplayItem(displayDef, screenName);
            }
        } else if (itemDef.containsKey("type")) {
            // If item has a direct "type" property (e.g., colorpicker, button, label),
            // treat the item definition itself as the display definition
            // This applies whether or not the item has a varRef
            item.displayItem = parseDisplayItem(itemDef, screenName);
        }

        // UI properties
        // promptHelp (formerly promptText) now goes into displayItem
        String promptHelp = getStringValue(itemDef, "promptHelp", getStringValue(itemDef, "prompt_help", null));
        if (promptHelp != null) {
            // If displayItem doesn't exist yet, create it
            if (item.displayItem == null) {
                item.displayItem = new DisplayItem();
            }
            item.displayItem.promptHelp = promptHelp;
        }
        item.tooltip = getStringValue(itemDef, "tooltip", null);
        item.editable = getBooleanValue(itemDef, "editable", null);
        item.disabled = getBooleanValue(itemDef, "disabled", null);
        item.visible = getBooleanValue(itemDef, "visible", null);
        item.textColor = getStringValue(itemDef, "textColor", getStringValue(itemDef, "text_color", null));
        item.backgroundColor = getStringValue(itemDef, "backgroundColor", getStringValue(itemDef, "background_color", null));

        // Layout properties
        item.colSpan = getIntValue(itemDef, "colSpan", getIntValue(itemDef, "col_span", null));
        item.rowSpan = getIntValue(itemDef, "rowSpan", getIntValue(itemDef, "row_span", null));
        item.hgrow = getStringValue(itemDef, "hgrow", null);
        item.vgrow = getStringValue(itemDef, "vgrow", null);
        item.margin = getStringValue(itemDef, "margin", null);
        item.padding = getStringValue(itemDef, "padding", null);
        item.prefWidth = getStringValue(itemDef, "prefWidth", getStringValue(itemDef, "pref_width", null));
        item.prefHeight = getStringValue(itemDef, "prefHeight", getStringValue(itemDef, "pref_height", null));
        item.minWidth = getStringValue(itemDef, "minWidth", getStringValue(itemDef, "min_width", null));
        item.minHeight = getStringValue(itemDef, "minHeight", getStringValue(itemDef, "min_height", null));
        item.maxWidth = getStringValue(itemDef, "maxWidth", getStringValue(itemDef, "max_width", null));
        item.maxHeight = getStringValue(itemDef, "maxHeight", getStringValue(itemDef, "max_height", null));
        item.alignment = getStringValue(itemDef, "alignment", null);
        
        // Event handlers
        item.onValidate = getStringValue(itemDef, "onValidate", getStringValue(itemDef, "on_validate", null));
        item.onChange = getStringValue(itemDef, "onChange", getStringValue(itemDef, "on_change", null));

        // Validate that no invalid properties are present in item definition
        validateAreaItemProperties(itemDef, screenName);

        return item;
    }

    /**
     * Parses DisplayItem from a Map.
     */
    private static DisplayItem parseDisplayItem(Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = new DisplayItem();

        // Extract display type and convert to enum
        String typeStr = getStringValue(displayDef, "type", "textfield");
        metadata.type = typeStr.toLowerCase();
        metadata.itemType = DisplayItem.ItemType.fromString(metadata.type);

        // Set CSS class from enum
        metadata.cssClass = metadata.itemType.getCssClass();

        metadata.mandatory = getBooleanValue(displayDef, "mandatory", false);
        metadata.caseFormat = getStringValue(displayDef, "case", null);
        metadata.alignment = getStringValue(displayDef, "alignment", null);
        metadata.pattern = getStringValue(displayDef, "pattern", null);

        // Min and max can be various types
        if (displayDef.containsKey("min")) {
            metadata.min = displayDef.get("min");
        }
        if (displayDef.containsKey("max")) {
            metadata.max = displayDef.get("max");
        }

        // Extract or set default style
        metadata.style = getStringValue(displayDef, "style", metadata.itemType.getDefaultStyle());
        metadata.screenName = screenName;
        
        // Extract promptHelp (placeholder text for text inputs)
        metadata.promptHelp = getStringValue(displayDef, "promptHelp", getStringValue(displayDef, "prompt_help", null));
        
        // Extract labelText (permanent label displayed before/above control - used for buttons and labels)
        metadata.labelText = getStringValue(displayDef, "labelText", getStringValue(displayDef, "label_text", null));
        
        // Extract labelText alignment
        metadata.labelTextAlignment = getStringValue(displayDef, "labelTextAlignment", getStringValue(displayDef, "label_text_alignment", null));
        
        // Extract onClick event handler for buttons
        metadata.onClick = getStringValue(displayDef, "onClick", getStringValue(displayDef, "on_click", null));
        
        // Extract onValidate event handler for item validation
        metadata.onValidate = getStringValue(displayDef, "onValidate", getStringValue(displayDef, "on_validate", null));
        
        // Extract onChange event handler - fires whenever the item value changes
        metadata.onChange = getStringValue(displayDef, "onChange", getStringValue(displayDef, "on_change", null));
        
        // Extract options for selection controls
        if (displayDef.containsKey("options")) {
            Object optionsObj = displayDef.get("options");
            if (optionsObj instanceof java.util.Map) {
                // Handle Map type: keys are display text, values are data values
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) optionsObj;
                metadata.optionsMap = new java.util.LinkedHashMap<>();
                for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                    metadata.optionsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            } else if (optionsObj instanceof java.util.List) {
                metadata.options = new ArrayList<>();
                for (Object opt : (java.util.List<?>) optionsObj) {
                    metadata.options.add(String.valueOf(opt));
                }
            }
        }
        
        // Extract styling properties
        metadata.labelColor = getStringValue(displayDef, "labelColor", getStringValue(displayDef, "label_color", null));
        metadata.labelBold = getBooleanValue(displayDef, "labelBold", getBooleanValue(displayDef, "label_bold", null));
        metadata.labelItalic = getBooleanValue(displayDef, "labelItalic", getBooleanValue(displayDef, "label_italic", null));
        metadata.labelFontSize = getStringValue(displayDef, "labelFontSize", getStringValue(displayDef, "label_font_size", null));
        metadata.itemFontSize = getStringValue(displayDef, "itemFontSize", getStringValue(displayDef, "item_font_size", null));
        metadata.itemColor = getStringValue(displayDef, "itemColor", getStringValue(displayDef, "item_color", null));
        metadata.itemBold = getBooleanValue(displayDef, "itemBold", getBooleanValue(displayDef, "item_bold", null));
        metadata.itemItalic = getBooleanValue(displayDef, "itemItalic", getBooleanValue(displayDef, "item_italic", null));
        metadata.maxLength = getIntValue(displayDef, "maxLength", getIntValue(displayDef, "max_length", null));
        metadata.height = getIntValue(displayDef, "height", null);

        // Validate that no invalid properties are present in display definition
        validateDisplayProperties(displayDef, screenName);

        return metadata;
    }

    /**
     * Merges two DisplayItem metadata objects.
     * The overlay metadata takes precedence over base metadata for non-null fields.
     * 
     * @param base The base metadata (typically from var definition)
     * @param overlay The overlay metadata (typically from area item display)
     * @return Merged metadata with overlay values taking precedence
     */
    private static DisplayItem mergeDisplayMetadata(DisplayItem base, DisplayItem overlay) {
        // If no base, return overlay
        if (base == null) {
            return overlay;
        }
        // If no overlay, return base
        if (overlay == null) {
            return base;
        }
        
        // Create a new DisplayItem with base values
        DisplayItem merged = new DisplayItem();
        
        // Copy all fields from base first
        merged.itemType = base.itemType;
        merged.type = base.type;
        merged.cssClass = base.cssClass;
        merged.mandatory = base.mandatory;
        merged.caseFormat = base.caseFormat;
        merged.min = base.min;
        merged.max = base.max;
        merged.style = base.style;
        merged.screenName = base.screenName;
        merged.alignment = base.alignment;
        merged.pattern = base.pattern;
        merged.promptHelp = base.promptHelp;
        merged.labelText = base.labelText;
        merged.labelTextAlignment = base.labelTextAlignment;
        merged.labelPosition = base.labelPosition;
        merged.options = base.options;
        merged.optionsMap = base.optionsMap;
        merged.columns = base.columns;
        merged.displayRecords = base.displayRecords;
        merged.treeItems = base.treeItems;
        merged.expandAll = base.expandAll;
        merged.showRoot = base.showRoot;
        merged.labelColor = base.labelColor;
        merged.labelBold = base.labelBold;
        merged.labelItalic = base.labelItalic;
        merged.labelFontSize = base.labelFontSize;
        merged.itemFontSize = base.itemFontSize;
        merged.maxLength = base.maxLength;
        merged.height = base.height;
        merged.itemColor = base.itemColor;
        merged.itemBold = base.itemBold;
        merged.itemItalic = base.itemItalic;
        merged.onClick = base.onClick;
        merged.onValidate = base.onValidate;
        merged.onChange = base.onChange;
        
        // Override with non-null overlay values
        if (overlay.itemType != null) merged.itemType = overlay.itemType;
        if (overlay.type != null) merged.type = overlay.type;
        if (overlay.cssClass != null) merged.cssClass = overlay.cssClass;
        // Always use overlay's mandatory flag if it's been explicitly set (even if false)
        merged.mandatory = overlay.mandatory;
        if (overlay.caseFormat != null) merged.caseFormat = overlay.caseFormat;
        if (overlay.min != null) merged.min = overlay.min;
        if (overlay.max != null) merged.max = overlay.max;
        if (overlay.style != null) merged.style = overlay.style;
        if (overlay.screenName != null) merged.screenName = overlay.screenName;
        if (overlay.alignment != null) merged.alignment = overlay.alignment;
        if (overlay.pattern != null) merged.pattern = overlay.pattern;
        if (overlay.promptHelp != null) merged.promptHelp = overlay.promptHelp;
        if (overlay.labelText != null) merged.labelText = overlay.labelText;
        if (overlay.labelTextAlignment != null) merged.labelTextAlignment = overlay.labelTextAlignment;
        if (overlay.labelPosition != null) merged.labelPosition = overlay.labelPosition;
        if (overlay.options != null) merged.options = overlay.options;
        if (overlay.optionsMap != null) merged.optionsMap = overlay.optionsMap;
        if (overlay.columns != null) merged.columns = overlay.columns;
        if (overlay.displayRecords != null) merged.displayRecords = overlay.displayRecords;
        if (overlay.treeItems != null) merged.treeItems = overlay.treeItems;
        if (overlay.expandAll != null) merged.expandAll = overlay.expandAll;
        if (overlay.showRoot != null) merged.showRoot = overlay.showRoot;
        if (overlay.labelColor != null) merged.labelColor = overlay.labelColor;
        if (overlay.labelBold != null) merged.labelBold = overlay.labelBold;
        if (overlay.labelItalic != null) merged.labelItalic = overlay.labelItalic;
        if (overlay.labelFontSize != null) merged.labelFontSize = overlay.labelFontSize;
        if (overlay.itemFontSize != null) merged.itemFontSize = overlay.itemFontSize;
        if (overlay.maxLength != null) merged.maxLength = overlay.maxLength;
        if (overlay.height != null) merged.height = overlay.height;
        if (overlay.itemColor != null) merged.itemColor = overlay.itemColor;
        if (overlay.itemBold != null) merged.itemBold = overlay.itemBold;
        if (overlay.itemItalic != null) merged.itemItalic = overlay.itemItalic;
        if (overlay.onClick != null) merged.onClick = overlay.onClick;
        if (overlay.onValidate != null) merged.onValidate = overlay.onValidate;
        if (overlay.onChange != null) merged.onChange = overlay.onChange;
        
        return merged;
    }
    
    /**
     * Looks up a DisplayItem from the context, trying both the qualified screen name
     * and the base screen name (for child screens shown from parent screen context).
     * 
     * When a screen is defined, display metadata is stored with the base screen name.
     * However, when shown from within another screen's context, the qualified key
     * includes the parent prefix (e.g., "parentscreen.childscreen"). This method
     * handles both cases by first trying the qualified key, then falling back to
     * the base screen name.
     * 
     * @param context The interpreter context containing display items
     * @param screenName The screen name (may be qualified with parent prefix)
     * @param varRef The variable reference to look up
     * @return The DisplayItem if found, or null if not found
     */
    private static DisplayItem lookupDisplayItem(InterpreterContext context, String screenName, String varRef) {
        if (context == null || varRef == null) {
            return null;
        }
        
        // First try with the qualified screen name
        String qualifiedKey = screenName + "." + varRef;
        DisplayItem metadata = context.getDisplayItem().get(qualifiedKey);
        
        if (metadata != null) {
            return metadata;
        }
        
        // If not found and screenName contains a dot (indicating a parent.child pattern),
        // try with just the base screen name (last part after the last dot)
        if (screenName.contains(".")) {
            int lastDotIndex = screenName.lastIndexOf('.');
            String baseScreenName = screenName.substring(lastDotIndex + 1);
            String baseKey = baseScreenName + "." + varRef;
            metadata = context.getDisplayItem().get(baseKey);
        }
        
        return metadata;
    }

    // Helper methods for safe value extraction from Maps
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            return value != null ? String.valueOf(value) : defaultValue;
        }
        return defaultValue;
    }

    private static double getNumberValue(Map<String, Object> map, String key, double defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return defaultValue;
    }

    /**
     * Calculates the maximum label width needed for proper alignment. This
     * ensures all labels in a form have consistent width.
     *
     * @param items The list of items to check for labels
     * @param screenName The screen name
     * @param context InterpreterContext for accessing display metadata
     * @return The maximum label width in pixels
     */
    private static double calculateMaxLabelWidth(List<AreaItem> items, String screenName,
            InterpreterContext context) {
        double maxWidth = 100; // Minimum width
        javafx.scene.text.Text measuringText = new javafx.scene.text.Text();

        for (AreaItem item : items) {
            // Get metadata with same merge logic as createAreaWithItems
            DisplayItem metadata = null;
            if (item.varRef != null && context != null) {
                metadata = lookupDisplayItem(context, screenName, item.varRef);
            }
            if (item.displayItem != null) {
                metadata = mergeDisplayMetadata(metadata, item.displayItem);
            }

            // Only measure labels for controls that will be wrapped (not Label or Button controls)
            if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
                // Check if this is a control that will have a label wrapper
                if (metadata.itemType != DisplayItem.ItemType.LABEL
                        && metadata.itemType != DisplayItem.ItemType.LABELTEXT
                        && metadata.itemType != DisplayItem.ItemType.BUTTON) {

                    // Parse font size and create proper Font object for accurate measurement
                    double fontSize = 13.0; // Default font size
                    javafx.scene.text.FontWeight fontWeight = javafx.scene.text.FontWeight.NORMAL;
                    
                    if (metadata.labelFontSize != null && !metadata.labelFontSize.isEmpty()) {
                        fontSize = parseFontSize(metadata.labelFontSize);
                    }
                    
                    if (metadata.labelBold != null && metadata.labelBold) {
                        fontWeight = javafx.scene.text.FontWeight.BOLD;
                    }
                    
                    // Set font directly on Text node for accurate measurement
                    measuringText.setFont(javafx.scene.text.Font.font("System", fontWeight, fontSize));

                    // Handle multiline labels by splitting on \n and measuring the longest line
                    String labelText = metadata.labelText;
                    if (labelText.contains("\n")) {
                        String[] lines = labelText.split("\n");
                        double maxLineWidth = 0;
                        for (String line : lines) {
                            measuringText.setText(line);
                            double lineWidth = measuringText.getLayoutBounds().getWidth();
                            if (lineWidth > maxLineWidth) {
                                maxLineWidth = lineWidth;
                            }
                        }
                        double width = maxLineWidth + 20; // Add padding
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    } else {
                        measuringText.setText(labelText);
                        double width = measuringText.getLayoutBounds().getWidth() + 20; // Add padding
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    }
                }
            }
        }

        return maxWidth;
    }
    
    /**
     * Parses a font size string (e.g., "14px", "1.5em", "16") and returns the size in pixels.
     * 
     * @param fontSizeStr The font size string to parse
     * @return The font size in pixels
     */
    private static double parseFontSize(String fontSizeStr) {
        if (fontSizeStr == null || fontSizeStr.isEmpty()) {
            return 13.0; // Default font size
        }
        
        String trimmed = fontSizeStr.trim().toLowerCase();
        
        try {
            if (trimmed.endsWith("px")) {
                // Parse pixel values (e.g., "14px")
                return Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
            } else if (trimmed.endsWith("em")) {
                // Parse em values (e.g., "1.5em") - 1em = 13px (default)
                double emValue = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
                return emValue * 13.0;
            } else if (trimmed.endsWith("pt")) {
                // Parse point values (e.g., "12pt") - 1pt = 1.333px
                double ptValue = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
                return ptValue * 1.333;
            } else {
                // No unit specified, assume pixels
                return Double.parseDouble(trimmed);
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse font size '" + fontSizeStr + "', using default 13px");
            return 13.0;
        }
    }

    /**
     * Creates a labeled control by wrapping the control with a label. The label
     * is displayed based on the specified alignment.
     *
     * @param labelText The text for the label
     * @param alignment The alignment: "left", "center", "right" (default:
     * "left")
     * @param control The control to label
     * @param minWidth The minimum width for the label for alignment consistency
     * @param metadata The display metadata containing font size and styling
     * information
     * @return A container with the label and control
     */
    /**
     * Creates a labeled control by wrapping a control with its label.
     * For most controls, the label is placed horizontally (on the left).
     * For TableView and similar large controls, the label is placed vertically (on top).
     * 
     * If the item has hgrow or vgrow properties set, the container type will be chosen
     * to support those properties (HBox for hgrow, VBox for vgrow), and the properties
     * will be applied to the container so it can participate in parent layout growth.
     *
     * @param labelText The text for the label
     * @param alignment The alignment of the label ("left", "center", "right")
     * @param control The control to wrap
     * @param minWidth The minimum width for the label
     * @param metadata DisplayItem metadata containing styling information
     * @param labelPosition Label position: "left", "right", "top", or "bottom"
     * @param item The AreaItem containing layout properties like hgrow and vgrow
     * @return The wrapped control with label
     */
    private static Node createLabeledControl(String labelText, String alignment, Node control, double minWidth, DisplayItem metadata, String labelPosition, AreaItem item) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(labelText);

        // Check if item has hgrow or vgrow properties that require special container handling
        boolean hasHgrow = item != null && item.hgrow != null && !item.hgrow.isEmpty();
        boolean hasVgrow = item != null && item.vgrow != null && !item.vgrow.isEmpty();
        
        // Determine if vertical or horizontal layout based on position
        // If hgrow is set, prefer horizontal layout (HBox) unless explicitly set to vertical position
        // If vgrow is set, prefer vertical layout (VBox) unless explicitly set to horizontal position
        boolean isVertical = "top".equals(labelPosition) || "bottom".equals(labelPosition);
        
        // Override layout direction if growth properties require it
        if (hasHgrow && !hasVgrow && ("top".equals(labelPosition) || "bottom".equals(labelPosition))) {
            // Item has hgrow but position is top/bottom - we need HBox for hgrow to work
            // Change to horizontal layout and adjust label position
            isVertical = false;
            labelPosition = "left"; // Default to left for horizontal layout
        } else if (hasVgrow && !hasHgrow && ("left".equals(labelPosition) || "right".equals(labelPosition) || labelPosition == null || labelPosition.isEmpty())) {
            // Item has vgrow but position is left/right - we need VBox for vgrow to work
            // Change to vertical layout and adjust label position
            isVertical = true;
            labelPosition = "top"; // Default to top for vertical layout
        }
        
        // Build label style with default styling
        String defaultAlignment = isVertical ? "center-left" : "center-right";
        String defaultPadding;
        switch (labelPosition) {
            case "top":
                defaultPadding = "0 0 5 0";  // bottom padding
                break;
            case "bottom":
                defaultPadding = "5 0 0 0";  // top padding
                break;
            case "right":
                defaultPadding = "0 0 0 10"; // left padding
                break;
            case "left":
            default:
                defaultPadding = "0 10 0 0"; // right padding
                break;
        }
        StringBuilder styleBuilder = new StringBuilder("-fx-font-weight: normal; -fx-padding: " + defaultPadding + "; -fx-alignment: " + defaultAlignment + "; -fx-text-fill: #333333;");

        // Apply label styling from metadata
        if (metadata != null) {
            // Apply font size if specified
            if (metadata.labelFontSize != null && !metadata.labelFontSize.isEmpty()) {
                styleBuilder.append(" -fx-font-size: ").append(metadata.labelFontSize).append(";");
            }

            // Apply label color if specified (this will override the default)
            if (metadata.labelColor != null && !metadata.labelColor.isEmpty()) {
                // Remove default text-fill and apply custom color
                String currentStyle = styleBuilder.toString();
                currentStyle = currentStyle.replace("-fx-text-fill: #333333;", "");
                styleBuilder = new StringBuilder(currentStyle);
                styleBuilder.append(" -fx-text-fill: ").append(metadata.labelColor).append(";");
            }

            // Apply bold if specified
            if (metadata.labelBold != null && metadata.labelBold) {
                styleBuilder.append(" -fx-font-weight: bold;");
            }

            // Apply italic if specified
            if (metadata.labelItalic != null && metadata.labelItalic) {
                styleBuilder.append(" -fx-font-style: italic;");
            }
        }

        label.setStyle(styleBuilder.toString());
        
        if (isVertical) {
            // Vertical layout: label on top or bottom
            label.setMaxWidth(Double.MAX_VALUE);  // Allow label to stretch full width
            javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(0);
            container.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            container.setPickOnBounds(false);
            
            if ("bottom".equals(labelPosition)) {
                container.getChildren().addAll(control, label);
            } else {
                // "top" is default for vertical
                container.getChildren().addAll(label, control);
            }
            
            // Allow the control (e.g., TableView, TextArea) to grow both vertically and horizontally within the VBox
            javafx.scene.layout.VBox.setVgrow(control, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);
            
            // Apply vgrow to the container itself if the item has vgrow property
            // This allows the wrapper to participate in parent layout growth
            if (hasVgrow) {
                try {
                    Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                    // Store the priority on the container so it can be applied by the parent
                    container.getProperties().put("vgrowPriority", priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            // If item has hgrow, allow the container to grow horizontally as well
            if (hasHgrow) {
                container.setMaxWidth(Double.MAX_VALUE);
                try {
                    Priority priority = Priority.valueOf(item.hgrow.toUpperCase());
                    // Store the priority on the container so it can be applied by the parent
                    container.getProperties().put("hgrowPriority", priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            return container;
        } else {
            // Horizontal layout: label on left or right
            label.setMinWidth(minWidth); // Use calculated minimum width to align labels underneath each other
            label.setMaxWidth(Region.USE_PREF_SIZE);

            // Create container
            javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(5);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            // Make container pick on bounds so tooltips on child controls work properly
            container.setPickOnBounds(false);

            if ("right".equals(labelPosition)) {
                // Control first, then label on the right
                container.getChildren().addAll(control, label);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            } else {
                // "left" is default - Label first (on the left), then control
                container.getChildren().addAll(label, control);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            // Apply hgrow to the container itself if the item has hgrow property
            // This allows the wrapper to participate in parent layout growth
            if (hasHgrow) {
                // Allow the control to grow within the HBox
                javafx.scene.layout.HBox.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);
                // Set max width so container can expand
                container.setMaxWidth(Double.MAX_VALUE);
                try {
                    Priority priority = Priority.valueOf(item.hgrow.toUpperCase());
                    // Store the priority on the container so it can be applied by the parent
                    container.getProperties().put("hgrowPriority", priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            
            // If item has vgrow, allow the container to grow vertically as well
            if (hasVgrow) {
                container.setMaxHeight(Double.MAX_VALUE);
                try {
                    Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                    // Store the priority on the container so it can be applied by the parent
                    container.getProperties().put("vgrowPriority", priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

            return container;
        }
    }

    /**
     * Sets up two-way data binding between a UI control and a screen variable.
     * When the variable changes, the UI updates. When the UI changes, the
     * variable updates.
     *
     * @param control The JavaFX control to bind
     * @param varName The variable name
     * @param screenVars The map containing screen variables
     * @param metadata The DisplayItem metadata for the control
     * @deprecated Use DataBindingManager.setupBinding() instead
     */
    private static void setupVariableBinding(Node control, String varName,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        // Delegate to DataBindingManager in the data layer
        DataBindingManager.setupBinding(control, varName, screenVars, varTypes, metadata);
    }

    /**
     * Resolves a varRef value, handling both simple variable names and complex
     * expressions with array element access like "clients[0].clientName".
     * @deprecated Use VarRefResolver.resolveVarRefValue() instead
     */
    private static Object resolveVarRefValue(String varRef, 
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Delegate to VarRefResolver in the data layer
        return VarRefResolver.resolveVarRefValue(varRef, screenVars);
    }

    /**
     * Navigates a path like "[0].clientName" through an object/array structure
     * with case-insensitive property name matching.
     * @deprecated Use VarRefResolver.navigatePathCaseInsensitive() instead
     */
    private static Object navigatePathCaseInsensitive(Object root, String path) {
        // Delegate to VarRefResolver in the data layer
        return VarRefResolver.navigatePathCaseInsensitive(root, path);
    }

    /**
     * Gets a value from a map using case-insensitive key matching.
     * @deprecated Use VarRefResolver.getMapValueCaseInsensitive() instead
     */
    private static Object getMapValueCaseInsensitive(java.util.Map<String, Object> map, String key) {
        return VarRefResolver.getMapValueCaseInsensitive(map, key);
    }

    /**
     * Sets a value for a varRef, handling both simple variable names and complex
     * expressions with array element access like "clients[0].clientName".
     * @deprecated Use VarRefResolver.setVarRefValue() instead
     */
    private static void setVarRefValue(String varRef, Object value,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Delegate to VarRefResolver in the data layer
        VarRefResolver.setVarRefValue(varRef, value, screenVars);
    }

    /**
     * Sets a value at a path like "[0].clientName" through an object/array structure
     * with case-insensitive property name matching.
     * @deprecated Use VarRefResolver.setPathValueCaseInsensitive() instead
     */
    private static void setPathValueCaseInsensitive(Object root, String path, Object value) {
        // Delegate to VarRefResolver in the data layer
        VarRefResolver.setPathValueCaseInsensitive(root, path, value);
    }

    /**
     * Checks if the remaining path is just a single property name (the last segment).
     * @deprecated Use VarRefResolver.isLastPropertySegment() instead
     */
    private static boolean isLastPropertySegment(String path, int start) {
        return VarRefResolver.isLastPropertySegment(path, start);
    }

    /**
     * Sets a value in a map using case-insensitive key matching.
     * If key exists (any case), updates that key. Otherwise, adds with the provided key.
     * @deprecated Use VarRefResolver.setMapValueCaseInsensitive() instead
     */
    private static void setMapValueCaseInsensitive(java.util.Map<String, Object> map, String key, Object value) {
        VarRefResolver.setMapValueCaseInsensitive(map, key, value);
    }

    /**
     * Updates a control's value based on the variable value.
     * @deprecated Use ControlUpdater.updateControlFromValue() instead
     */
    private static void updateControlFromValue(Node control, Object value, DisplayItem metadata) {
        // Delegate to ControlUpdater in the display layer
        ControlUpdater.updateControlFromValue(control, value, metadata);
    }
    
    /**
     * Sets up an onValidate event handler for a control.
     * The validation code is executed when the control value changes.
     * If the code returns false, the control is marked with an error style.
     * 
     * @param control The JavaFX control to validate
     * @param validateCode The EBS code to execute for validation
     * @param onClickHandler Handler to execute the EBS code
     * @param screenName The screen name for context
     * @param context The interpreter context
     * @deprecated Use DisplayValidator.setupValidationHandler() instead
     */
    private static void setupValidationHandler(Node control, String validateCode,
            OnClickHandler onClickHandler, String screenName, InterpreterContext context) {
        // Delegate to DisplayValidator in the display layer
        DisplayValidator.setupValidationHandler(control, validateCode, onClickHandler, screenName, context);
    }
    
    /**
     * Attaches a validation listener to a control based on its type.
     * The validator is called whenever the control's value changes.
     * 
     * @param control The JavaFX control
     * @param validator The validation runnable to execute
     * @deprecated Use DisplayValidator.attachValidationListener() instead
     */
    private static void attachValidationListener(Node control, Runnable validator) {
        // Delegate to DisplayValidator in the display layer
        DisplayValidator.attachValidationListener(control, validator);
    }
    
    /**
     * Sets up an onChange event handler for a control.
     * The change code is executed whenever the control value changes.
     * 
     * @param control The JavaFX control to monitor
     * @param changeCode The EBS code to execute on change
     * @param onClickHandler Handler to execute the EBS code
     * @param screenName The screen name for context
     * @param context The interpreter context
     * @param boundControls List of bound controls to refresh after execution
     * @param screenVars The screen variables map
     */
    private static void setupChangeHandler(Node control, String changeCode,
            OnClickHandler onClickHandler, String screenName, InterpreterContext context,
            List<Node> boundControls, java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Delegate to DisplayChangeHandler in the display layer
        DisplayChangeHandler.setupChangeHandler(control, changeCode, onClickHandler, screenName, context, boundControls, screenVars);
    }

    /**
     * Adds a listener to a control to update the variable when the control
     * changes.
     * @deprecated Use ControlListenerFactory.addControlListener() instead
     */
    private static void addControlListener(Node control, String varName,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        // Delegate to ControlListenerFactory in the display layer
        ControlListenerFactory.addControlListener(control, varName, screenVars, varTypes, metadata);
    }
    
    /**
     * Refreshes all bound controls by updating their values from the screenVars
     * map. This is called after onClick handlers execute to reflect variable
     * changes in the UI.
     * @deprecated Use DataBindingManager.refreshBoundControls() instead
     */
    private static void refreshBoundControls(List<Node> boundControls,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Delegate to DataBindingManager in the data layer
        DataBindingManager.refreshBoundControls(boundControls, screenVars);
    }
    
    /**
     * Expands items when the area has numberOfRecords set.
     * For each item in the area, creates N copies where:
     * - The varRef is combined with recordRef and expanded with array index 
     *   (e.g., with recordRef="clients" and varRef="age", becomes "clients[0].age")
     * - The labelText is expanded with record number (e.g., "Name:" becomes "Client 1 - Name:")
     * - The name is made unique (e.g., "clientNameField" becomes "clientNameField_0")
     * - The sequence is calculated to keep items grouped by record (all fields for record 0, then record 1, etc.)
     * 
     * @param items The original list of items
     * @param numberOfRecords The number of records to expand to (from area level)
     * @param recordRef The record reference/array name (from area level)
     * @return A new list with expanded items
     */
    private static List<AreaItem> expandMultiRecordItems(List<AreaItem> items, Integer numberOfRecords, String recordRef) {
        List<AreaItem> expandedItems = new ArrayList<>();
        
        // If no expansion needed, return items as-is
        if (numberOfRecords == null || numberOfRecords <= 1 || recordRef == null || recordRef.isEmpty()) {
            return new ArrayList<>(items);
        }
        
        // Find the maximum sequence among items for proper grouping
        int maxSeq = 0;
        for (AreaItem item : items) {
            maxSeq = Math.max(maxSeq, item.sequence);
        }
        
        // Expand each item for each record
        for (int i = 0; i < numberOfRecords; i++) {
            for (AreaItem item : items) {
                AreaItem expandedItem = createExpandedItem(item, i, numberOfRecords, maxSeq, recordRef);
                expandedItems.add(expandedItem);
            }
        }
        
        return expandedItems;
    }
    
    /**
     * Creates an expanded copy of an item for a specific record index.
     * 
     * @param template The template item to copy from
     * @param index The record index (0-based)
     * @param totalRecords The total number of records
     * @param maxSequence The maximum sequence number among items
     * @param recordRef The record reference/array name to combine with varRef
     * @return A new AreaItem with expanded varRef, name, and labelText
     */
    private static AreaItem createExpandedItem(AreaItem template, int index, int totalRecords, int maxSequence, String recordRef) {
        AreaItem item = new AreaItem();
        
        // Copy all properties from template
        // Sequence calculation groups items by record index, then by original sequence within each record
        // Example: seq 1-4 for record 0, seq 5-8 for record 1, etc.
        // Formula: index * (maxSequence + 1) + template.sequence
        // This ensures all items for record 0 come before record 1, etc.
        item.sequence = index * (maxSequence + 1) + template.sequence;
        item.layoutPos = template.layoutPos;
        item.editable = template.editable;
        item.disabled = template.disabled;
        item.visible = template.visible;
        item.tooltip = template.tooltip;
        item.textColor = template.textColor;
        item.backgroundColor = template.backgroundColor;
        item.colSpan = template.colSpan;
        item.rowSpan = template.rowSpan;
        item.hgrow = template.hgrow;
        item.vgrow = template.vgrow;
        item.margin = template.margin;
        item.padding = template.padding;
        item.prefWidth = template.prefWidth;
        item.prefHeight = template.prefHeight;
        item.minWidth = template.minWidth;
        item.minHeight = template.minHeight;
        item.maxWidth = template.maxWidth;
        item.maxHeight = template.maxHeight;
        item.alignment = template.alignment;
        item.onValidate = template.onValidate;
        item.onChange = template.onChange;
        item.source = template.source;
        
        // Expand the name to be unique for each record
        if (template.name != null) {
            item.name = template.name + "_" + index;
        }
        
        // Expand the varRef by combining recordRef with the item's varRef and adding the index
        // e.g., recordRef="clients", varRef="age" becomes "clients[0].age"
        if (template.varRef != null) {
            item.varRef = expandVarRefWithRecordRef(template.varRef, recordRef, index);
        }
        
        // Clone and expand the displayItem
        if (template.displayItem != null) {
            item.displayItem = cloneDisplayItem(template.displayItem);
            // Expand the labelText with record number (1-based)
            if (item.displayItem.labelText != null && !item.displayItem.labelText.isEmpty()) {
                item.displayItem.labelText = expandLabelWithRecordNumber(item.displayItem.labelText, index + 1);
            }
        }
        
        return item;
    }
    
    /**
     * Expands a varRef by combining it with recordRef and adding an array index.
     * Examples:
     * - recordRef="clients", varRef="age", index=0 -> "clients[0].age"
     * - recordRef="clients", varRef="clientName", index=1 -> "clients[1].clientName"
     * 
     * @param varRef The field name (e.g., "age", "clientName")
     * @param recordRef The record/array reference (e.g., "clients")
     * @param index The array index (0-based)
     * @return The expanded variable reference
     */
    private static String expandVarRefWithRecordRef(String varRef, String recordRef, int index) {
        if (varRef == null || varRef.isEmpty()) {
            return varRef;
        }
        
        // Validate recordRef - if null or empty, just return varRef with index
        if (recordRef == null || recordRef.isEmpty()) {
            return varRef + "[" + index + "]";
        }
        
        // Combine recordRef with varRef and add the index
        // Result: "recordRef[index].varRef"
        return recordRef + "[" + index + "]." + varRef;
    }
    
    /**
     * Expands a label text with the record number.
     * Examples:
     * - "Name:" with recordNumber 1 -> "Record 1 - Name:"
     * - "Client - Name:" with recordNumber 2 -> "Client 2 - Name:"
     * - "Employee - ID:" with recordNumber 3 -> "Employee 3 - ID:"
     * 
     * The pattern handles labels in the format "EntityName - FieldLabel" by inserting
     * the record number after the entity name. If no such pattern is found, it prepends
     * "Record N - " to the label.
     * 
     * @param labelText The original label text
     * @param recordNumber The record number (1-based)
     * @return The expanded label text
     */
    private static String expandLabelWithRecordNumber(String labelText, int recordNumber) {
        if (labelText == null || labelText.isEmpty()) {
            return labelText;
        }
        
        // Check if the label follows the pattern "EntityName - FieldLabel" or "EntityName N - FieldLabel"
        // This pattern matches any word/entity name, followed by optional spaces, optional number,
        // a dash, and the rest of the label
        // Examples: "Client - Name:", "Employee - ID:", "Client 1 - Name:"
        java.util.regex.Pattern entityPattern = java.util.regex.Pattern.compile(
            "^(\\w+)\\s*(?:\\d+)?\\s*-\\s*(.+)$", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher entityMatcher = entityPattern.matcher(labelText);
        
        if (entityMatcher.matches()) {
            // Replace with the actual record number, keeping the entity name
            return entityMatcher.group(1) + " " + recordNumber + " - " + entityMatcher.group(2);
        }
        
        // If no "EntityName - Field" pattern, prepend "Record N - "
        return "Record " + recordNumber + " - " + labelText;
    }
    
    /**
     * Creates a clone of a DisplayItem.
     * 
     * @param source The source DisplayItem to clone
     * @return A new DisplayItem with copied properties
     */
    private static DisplayItem cloneDisplayItem(DisplayItem source) {
        DisplayItem clone = new DisplayItem();
        
        clone.itemType = source.itemType;
        clone.type = source.type;
        clone.cssClass = source.cssClass;
        clone.mandatory = source.mandatory;
        clone.caseFormat = source.caseFormat;
        clone.min = source.min;
        clone.max = source.max;
        clone.style = source.style;
        clone.screenName = source.screenName;
        clone.alignment = source.alignment;
        clone.pattern = source.pattern;
        clone.promptHelp = source.promptHelp;
        clone.labelText = source.labelText;
        clone.labelTextAlignment = source.labelTextAlignment;
        clone.labelPosition = source.labelPosition;
        clone.labelColor = source.labelColor;
        clone.labelBold = source.labelBold;
        clone.labelItalic = source.labelItalic;
        clone.labelFontSize = source.labelFontSize;
        clone.itemFontSize = source.itemFontSize;
        clone.maxLength = source.maxLength;
        clone.height = source.height;
        clone.itemColor = source.itemColor;
        clone.itemBold = source.itemBold;
        clone.itemItalic = source.itemItalic;
        clone.onClick = source.onClick;
        clone.onValidate = source.onValidate;
        clone.onChange = source.onChange;
        clone.showSliderValue = source.showSliderValue;
        clone.source = source.source;
        clone.status = source.status;
        clone.displayRecords = source.displayRecords;
        clone.seq = source.seq;
        
        // Clone options list if present
        // Note: This creates a shallow copy which is safe because options is a List<String>
        // and strings are immutable in Java
        if (source.options != null) {
            clone.options = new ArrayList<>(source.options);
        }
        
        // Clone optionsMap if present
        // Note: This creates a shallow copy which is safe because optionsMap is a Map<String, String>
        // and strings are immutable in Java
        if (source.optionsMap != null) {
            clone.optionsMap = new java.util.LinkedHashMap<>(source.optionsMap);
        }
        
        // Clone columns list if present
        if (source.columns != null) {
            clone.columns = new ArrayList<>();
            for (DisplayItem.TableColumn col : source.columns) {
                DisplayItem.TableColumn colClone = new DisplayItem.TableColumn();
                colClone.name = col.name;
                colClone.field = col.field;
                colClone.type = col.type;
                colClone.width = col.width;
                colClone.alignment = col.alignment;
                clone.columns.add(colClone);
            }
        }
        
        // Clone treeItems list if present
        if (source.treeItems != null) {
            clone.treeItems = cloneTreeItems(source.treeItems);
        }
        clone.expandAll = source.expandAll;
        clone.showRoot = source.showRoot;
        
        return clone;
    }
    
    /**
     * Deep clones a list of TreeItemDef objects.
     */
    private static List<DisplayItem.TreeItemDef> cloneTreeItems(List<DisplayItem.TreeItemDef> source) {
        if (source == null) {
            return null;
        }
        List<DisplayItem.TreeItemDef> clone = new ArrayList<>();
        for (DisplayItem.TreeItemDef item : source) {
            DisplayItem.TreeItemDef itemClone = new DisplayItem.TreeItemDef();
            itemClone.value = item.value;
            itemClone.icon = item.icon;
            itemClone.iconOpen = item.iconOpen;
            itemClone.iconClosed = item.iconClosed;
            itemClone.expanded = item.expanded;
            if (item.children != null) {
                itemClone.children = cloneTreeItems(item.children);
            }
            clone.add(itemClone);
        }
        return clone;
    }

    // Schema validation methods
    /**
     * Validates a screen definition against the JSON schema.
     *
     * @param screenDef The screen definition to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateScreenDefinition(Map<String, Object> screenDef) {
        if (screenSchema == null) {
            System.err.println("Warning: Screen schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(screenDef, screenSchema);
            if (!errors.isEmpty()) {
                String errorMessage = "Screen definition validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate screen definition: " + e.getMessage(), e);
        }
    }

    /**
     * Validates an area definition against the JSON schema.
     *
     * @param areaDef The area definition to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateAreaDefinition(Map<String, Object> areaDef) {
        if (areaSchema == null) {
            System.err.println("Warning: Area schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(areaSchema, areaSchema);

            if (!errors.isEmpty()) {
                String errorMessage = "Area definition validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate area definition: " + e.getMessage(), e);
        }
    }

    /**
     * Validates display metadata against the JSON schema.
     *
     * @param displayDef The display metadata to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateDisplayItem(Map<String, Object> displayDef) {
        if (displayMetadataSchema == null) {
            System.err.println("Warning: Display metadata schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(displayDef, displayMetadataSchema);

            if (!errors.isEmpty()) {
                String errorMessage = "Display metadata validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate display metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if schema validation is available.
     *
     * @return true if schemas are loaded and validation is available
     */
    public static boolean isValidationAvailable() {
        return screenSchema != null && areaSchema != null && displayMetadataSchema != null;
    }
}
