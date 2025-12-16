package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;

import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.interpreter.screen.AreaDefinition;
import com.eb.script.interpreter.screen.AreaItem;
import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.Var;
import com.eb.script.token.DataType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in functions for screen control operations.
 * Provides methods for showing, hiding, closing, and managing screen properties.
 *
 * @author Earl Bosch
 */
public class BuiltinsScreen {

    // Constants for snapshot configuration
    private static final String DEFAULT_SNAPSHOT_FORMAT = "png";
    private static final String SNAPSHOT_NAME_SUFFIX = "_screenshot";
    private static final int SNAPSHOT_TIMEOUT_SECONDS = 10;
    
    // Storage for tree item icon paths (weak references to avoid memory leaks)
    private static final java.util.WeakHashMap<javafx.scene.control.TreeItem<String>, TreeItemIconData> treeItemIcons 
        = new java.util.WeakHashMap<>();
    
    // Storage for tree item style data (weak references to avoid memory leaks)
    private static final java.util.WeakHashMap<javafx.scene.control.TreeItem<String>, TreeItemStyleData> treeItemStyles 
        = new java.util.WeakHashMap<>();
    
    /**
     * Helper class to store icon path data for tree items
     */
    private static class TreeItemIconData {
        String iconPath;
        String iconOpenPath;
        String iconClosedPath;
        javafx.beans.value.ChangeListener<Boolean> expansionListener;
    }
    
    /**
     * Helper class to store style data for tree items
     */
    private static class TreeItemStyleData {
        Boolean bold;
        Boolean italic;
        String color;
    }

    /**
     * scr.showScreen(screenName?) -> BOOL Shows a screen. If screenName is null
     * or empty, uses the current screen from context. Returns true on success.
     */
    public static Object screenShow(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.showScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen configuration exists (might not be created yet)
        if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.showScreen: Screen '" + screenName + "' does not exist. Use 'show screen " + screenName + ";' statement instead.");
        }

        // If screen hasn't been created yet, suggest using the statement form
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.showScreen: Screen '" + screenName + "' has not been shown yet. Use 'show screen " + screenName + ";' statement first.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.showScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Show the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (!stage.isShowing()) {
                stage.show();
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' shown");
                }
            } else {
                if (context.getOutput() != null) {
                    context.getOutput().printlnInfo("Screen '" + finalScreenName + "' is already showing");
                }
            }
        });
        return true;
    }

    /**
     * scr.hideScreen(screenName?) -> BOOL Hides a screen. If screenName is null
     * or empty, uses the current screen from context. Returns true on success.
     */
    public static Object screenHide(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.hideScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen configuration exists
        if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.hideScreen: Screen '" + screenName + "' does not exist.");
        }

        // If screen hasn't been created yet, nothing to hide
        if (!context.getScreens().containsKey(screenName)) {
            if (context.getOutput() != null) {
                context.getOutput().printlnInfo("Screen '" + screenName + "' is not shown (has not been created yet)");
            }
            return true;
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.hideScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Hide the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            boolean wasShowing = stage.isShowing();
            stage.hide();
            if (context.getOutput() != null) {
                if (wasShowing) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden");
                } else {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden (was already hidden)");
                }
            }
        });
        return true;
    }

    /**
     * scr.closeScreen(screenName?) -> BOOL Closes a screen. If screenName is
     * null or empty, uses the current screen from context. Returns true on
     * success.
     */
    public static Object screenClose(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.closeScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen configuration exists
        if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.closeScreen: Screen '" + screenName + "' does not exist.");
        }

        // If screen hasn't been created yet, just remove the config
        if (!context.getScreens().containsKey(screenName)) {
            context.remove(screenName);
            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Screen '" + screenName + "' definition removed (was not shown)");
            }
            return true;
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.closeScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Close the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            // Close the stage
            if (stage.isShowing()) {
                stage.close();
            }

            // Interrupt and stop the screen thread
            Thread thread = context.getScreenThreads().get(finalScreenName);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }

            // Clean up resources
            context.remove(finalScreenName);

            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Screen '" + finalScreenName + "' closed");
            }
        });

        return true;
    }

    /**
     * scr.showMenu(screenName?) -> BOOL
     * Shows the menu bar at the top of a screen.
     * If screenName is null or empty, uses the current screen from context.
     * Returns true on success.
     */
    public static Object screenShowMenu(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.showMenu: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.showMenu: Screen '" + screenName + "' does not exist or has not been shown yet.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.showMenu: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Show the menu on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Get the root BorderPane from ScreenFactory
                javafx.scene.layout.BorderPane screenRoot = com.eb.script.interpreter.screen.ScreenFactory.getScreenRootPane(finalScreenName);
                
                if (screenRoot != null) {
                    // Check if menu bar already exists
                    if (screenRoot.getTop() == null) {
                        // Create and add menu bar
                        javafx.scene.control.MenuBar menuBar = com.eb.script.interpreter.screen.ScreenFactory.createScreenMenuBar(stage);
                        screenRoot.setTop(menuBar);
                        
                        if (context.getOutput() != null) {
                            context.getOutput().printlnOk("Menu bar shown for screen '" + finalScreenName + "'");
                        }
                    } else if (context.getOutput() != null) {
                        context.getOutput().printlnInfo("Menu bar is already visible for screen '" + finalScreenName + "'");
                    }
                } else if (context.getOutput() != null) {
                    context.getOutput().printlnError("Could not access screen root for '" + finalScreenName + "'");
                }
            } catch (Exception e) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnError("Error showing menu for screen '" + finalScreenName + "': " + e.getMessage());
                }
            }
        });

        return true;
    }

    /**
     * scr.hideMenu(screenName?) -> BOOL
     * Hides the menu bar at the top of a screen.
     * If screenName is null or empty, uses the current screen from context.
     * Returns true on success.
     */
    public static Object screenHideMenu(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.hideMenu: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.hideMenu: Screen '" + screenName + "' does not exist or has not been shown yet.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.hideMenu: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Hide the menu on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Get the root BorderPane from ScreenFactory
                javafx.scene.layout.BorderPane screenRoot = com.eb.script.interpreter.screen.ScreenFactory.getScreenRootPane(finalScreenName);
                
                if (screenRoot != null) {
                    // Remove menu bar
                    if (screenRoot.getTop() != null) {
                        screenRoot.setTop(null);
                        
                        if (context.getOutput() != null) {
                            context.getOutput().printlnOk("Menu bar hidden for screen '" + finalScreenName + "'");
                        }
                    } else if (context.getOutput() != null) {
                        context.getOutput().printlnInfo("Menu bar is already hidden for screen '" + finalScreenName + "'");
                    }
                } else if (context.getOutput() != null) {
                    context.getOutput().printlnError("Could not access screen root for '" + finalScreenName + "'");
                }
            } catch (Exception e) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnError("Error hiding menu for screen '" + finalScreenName + "': " + e.getMessage());
                }
            }
        });

        return true;
    }

    /**
     * scr.addMenu(screenName, parentPath, name, displayName, callback) -> BOOL
     * Adds a custom menu item to a screen's menu bar.
     * 
     * @param screenName The name of the screen
     * @param parentPath The parent menu path separated with dots (e.g., "Edit" or "Edit.Format")
     * @param name The internal name/identifier for the menu item
     * @param displayName The text displayed to the user
     * @param callback The EBS code to execute when the menu item is clicked
     * @return true on success
     */
    public static Object screenAddMenu(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String parentPath = (String) args[1];
        String name = (String) args[2];
        String displayName = (String) args[3];
        String callback = (String) args[4];

        // Validate parameters
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.addMenu: screenName parameter cannot be null or empty");
        }
        if (parentPath == null || parentPath.isEmpty()) {
            throw new InterpreterError("scr.addMenu: parentPath parameter cannot be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new InterpreterError("scr.addMenu: name parameter cannot be null or empty");
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new InterpreterError("scr.addMenu: displayName parameter cannot be null or empty");
        }
        if (callback == null || callback.isEmpty()) {
            throw new InterpreterError("scr.addMenu: callback parameter cannot be null or empty");
        }
        
        // Normalize screen name to lowercase
        screenName = screenName.toLowerCase();

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.addMenu: Screen '" + screenName + "' does not exist or has not been shown yet.");
        }

        final String finalScreenName = screenName;
        final String finalParentPath = parentPath;
        final String finalName = name;
        final String finalDisplayName = displayName;
        final String finalCallback = callback;

        // Add the menu item on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Get the MenuBar for this screen
                javafx.scene.control.MenuBar menuBar = com.eb.script.interpreter.screen.ScreenFactory.getScreenMenuBar(finalScreenName);
                
                if (menuBar == null) {
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Menu bar not found for screen '" + finalScreenName + "'. Use showMenu property or scr.showMenu() first.");
                    }
                    return;
                }

                // Parse the parent path (e.g., "Edit" or "Edit.Format")
                String[] pathParts = finalParentPath.split("\\.");
                
                // Find or create the parent menu
                javafx.scene.control.Menu parentMenu = null;
                
                // Start with top-level menus
                for (javafx.scene.control.Menu topMenu : menuBar.getMenus()) {
                    if (topMenu.getText().equals(pathParts[0])) {
                        parentMenu = topMenu;
                        break;
                    }
                }
                
                // If top-level menu doesn't exist, create it
                if (parentMenu == null) {
                    parentMenu = new javafx.scene.control.Menu(pathParts[0]);
                    menuBar.getMenus().add(parentMenu);
                }
                
                // Navigate through nested menus if path has multiple parts
                for (int i = 1; i < pathParts.length; i++) {
                    String menuName = pathParts[i];
                    javafx.scene.control.Menu foundSubMenu = null;
                    
                    // Look for existing submenu
                    for (javafx.scene.control.MenuItem item : parentMenu.getItems()) {
                        if (item instanceof javafx.scene.control.Menu && item.getText().equals(menuName)) {
                            foundSubMenu = (javafx.scene.control.Menu) item;
                            break;
                        }
                    }
                    
                    // Create submenu if it doesn't exist
                    if (foundSubMenu == null) {
                        foundSubMenu = new javafx.scene.control.Menu(menuName);
                        parentMenu.getItems().add(foundSubMenu);
                    }
                    
                    parentMenu = foundSubMenu;
                }
                
                // Create the menu item
                javafx.scene.control.MenuItem menuItem = new javafx.scene.control.MenuItem(finalDisplayName);
                
                // Get the event dispatcher for this screen
                com.eb.script.interpreter.screen.ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(finalScreenName);
                
                // Set up the onClick handler
                menuItem.setOnAction(event -> {
                    try {
                        if (dispatcher != null && dispatcher.isRunning()) {
                            // Dispatch event to screen thread (asynchronous)
                            dispatcher.dispatchAsync(finalCallback);
                        } else {
                            // Fallback: execute directly on JavaFX thread
                            // This is used if the screen doesn't have a dedicated thread
                            if (context.getOutput() != null) {
                                context.getOutput().printlnWarn("Screen '" + finalScreenName + "' has no running dispatcher, executing callback directly");
                            }
                            // We need an interpreter to execute the code
                            // This is a simplified approach - ideally we'd have access to the screen's interpreter
                            System.err.println("Warning: Cannot execute menu callback without dispatcher for screen: " + finalScreenName);
                        }
                    } catch (Exception e) {
                        if (context.getOutput() != null) {
                            context.getOutput().printlnError("Error executing menu callback for '" + finalName + "': " + e.getMessage());
                        }
                        e.printStackTrace();
                    }
                });
                
                // Add the menu item to the parent menu
                parentMenu.getItems().add(menuItem);
                
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Menu item '" + finalDisplayName + "' added to '" + finalParentPath + "' in screen '" + finalScreenName + "'");
                }
                
            } catch (Exception e) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnError("Error adding menu item to screen '" + finalScreenName + "': " + e.getMessage());
                }
                e.printStackTrace();
            }
        });

        return true;
    }

    /**
     * scr.findScreen(screenName) -> BOOL
     * Checks if a screen has been defined.
     * Returns true if the screen exists in any of these states:
     * - Has a configuration (defined with 'screen name = {...}' syntax)
     * - Has an active Stage (currently shown or hidden)
     * - Has been declared in the current or imported script
     * 
     * This is a case-insensitive lookup.
     */
    public static Object screenFindScreen(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.findScreen: screenName parameter cannot be null or empty");
        }

        // Normalize screen name to lowercase for case-insensitive lookup
        screenName = screenName.toLowerCase();

        // Check all three conditions for screen existence:
        // 1. Has a configuration (from 'screen name = {...}' syntax)
        // 2. Has an active Stage (currently shown or hidden)
        // 3. Has been declared in the current or imported script
        boolean configExists = context.hasScreenConfig(screenName);
        boolean stageExists = context.getScreens().containsKey(screenName);
        boolean declaredExists = context.getDeclaredScreens().containsKey(screenName);

        // Return true if screen is defined in any way
        return configExists || stageExists || declaredExists;
    }

    /**
     * scr.setProperty(screenName.areaItemName, propertyName, value) -> BOOL
     * Sets a property on an area item in a screen.
     */
    public static Object screenSetProperty(InterpreterContext context, Object[] args) throws InterpreterError {
        String areaItemPath = (String) args[0];
        String propertyName = (String) args[1];
        Object value = args[2];

        if (areaItemPath == null || areaItemPath.isEmpty()) {
            throw new InterpreterError("scr.setProperty: areaItem parameter cannot be null or empty");
        }
        if (propertyName == null || propertyName.isEmpty()) {
            throw new InterpreterError("scr.setProperty: property parameter cannot be null or empty");
        }

        // Parse the compound key: screenName.areaItemName
        String[] parts = areaItemPath.split("\\.", 2);
        if (parts.length != 2) {
            throw new InterpreterError("scr.setProperty: areaItem must be in format 'screenName.areaItemName', got: " + areaItemPath);
        }

        String screenName = resolveScreenNameWithParent(context, parts[0]);
        String itemName = parts[1];

        // Find the screen and area item
        List<AreaDefinition> areas = context.getScreenAreas(screenName);
        if (areas == null) {
            throw new InterpreterError("scr.setProperty: screen '" + screenName + "' not found");
        }

        // Search for the item in all areas
        AreaItem targetItem = null;
        for (AreaDefinition area : areas) {
            targetItem = findItemInArea(context, area, itemName.toLowerCase());
            if (targetItem != null) {
                break;
            }
        }

        if (targetItem == null) {
            throw new InterpreterError("scr.setProperty: area item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Set the property on the AreaItem
        setAreaItemProperty(targetItem, propertyName, value);

        // Apply changes to the actual JavaFX control on the UI thread
        // Find the control by its user data and apply the property change
        // Note: screenName is the simple name for area lookup, but bound controls are stored under qualified names
        final String screenNameFinal = screenName;
        final String itemNameFinal = itemName;
        final com.eb.script.interpreter.screen.AreaItem finalItem = targetItem;
        final String finalPropertyName = propertyName;
        final Object finalValue = value;
        
        // Get the current screen context to determine the qualified name for bound controls
        final String currentScreenContext = context.getCurrentScreen();

        javafx.application.Platform.runLater(() -> {
            try {
                // Bound controls are stored under qualified names (e.g., "regexscreen.askaiscreen")
                // Try to find controls using:
                // 1. Current screen context if it ends with our screen name
                // 2. Simple screen name as fallback
                String boundControlsKey = screenNameFinal.toLowerCase();
                
                // If we're in a qualified context like "regexscreen.askaiscreen" and looking for "askaiscreen",
                // use the qualified name to find bound controls
                if (currentScreenContext != null && !currentScreenContext.isEmpty()) {
                    String suffix = "." + screenNameFinal.toLowerCase();
                    if (currentScreenContext.toLowerCase().endsWith(suffix) || 
                        currentScreenContext.toLowerCase().equals(screenNameFinal.toLowerCase())) {
                        boundControlsKey = currentScreenContext.toLowerCase();
                    }
                }
                
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(boundControlsKey);
                if (controls != null) {
                    // Find the control with matching user data (use case-insensitive comparison)
                    // User data is stored as "screenName.itemName" using the qualified screen name
                    String targetUserData = boundControlsKey + "." + itemNameFinal.toLowerCase();
                    for (javafx.scene.Node control : controls) {
                        Object userData = control.getUserData();
                        if (userData != null && targetUserData.equalsIgnoreCase(userData.toString())) {
                            // Found the control - apply the property
                            applyPropertyToControl(control, finalPropertyName, finalValue);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error applying property to control: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return Boolean.TRUE;
    }

    /**
     * scr.getProperty(screenName.areaItemName, propertyName) -> ANY Gets a
     * property value from an area item in a screen.
     */
    public static Object screenGetProperty(InterpreterContext context, Object[] args) throws InterpreterError {
        String areaItemPath = (String) args[0];
        String propertyName = (String) args[1];

        if (areaItemPath == null || areaItemPath.isEmpty()) {
            throw new InterpreterError("scr.getProperty: areaItem parameter cannot be null or empty");
        }
        if (propertyName == null || propertyName.isEmpty()) {
            throw new InterpreterError("scr.getProperty: property parameter cannot be null or empty");
        }

        // Parse the compound key: screenName.areaItemName
        String[] parts = areaItemPath.split("\\.", 2);
        if (parts.length != 2) {
            throw new InterpreterError("scr.getProperty: areaItem must be in format 'screenName.areaItemName', got: " + areaItemPath);
        }

        String screenName = resolveScreenNameWithParent(context, parts[0]);
        String itemName = parts[1];

        // Find the screen and area item
        List<AreaDefinition> areas = context.getScreenAreas(screenName);
        if (areas == null) {
            throw new InterpreterError("scr.getProperty: screen '" + screenName + "' not found");
        }

        // Search for the item in all areas
        com.eb.script.interpreter.screen.AreaItem targetItem = null;
        for (com.eb.script.interpreter.screen.AreaDefinition area : areas) {
            targetItem = findItemInArea(context, area, itemName.toLowerCase());
            if (targetItem != null) {
                break;
            }
        }

        if (targetItem == null) {
            throw new InterpreterError("scr.getProperty: area item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get the property from the AreaItem
        return getAreaItemProperty(targetItem, propertyName);
    }

    /**
     * scr.getItemList(screenName) -> ArrayDynamic Returns a list of all item
     * names in the screen.
     */
    public static Object screenGetItemList(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getItemList: screenName parameter cannot be null or empty");
        }

        // Find the screen
        Map<String, AreaItem> areas = context.getScreenAreaItems(screenName);
        if (areas == null) {
            throw new InterpreterError("scr.getItemList: screen '" + screenName + "' not found");
        }

        // Create a dynamic array to hold the item names
        ArrayDynamic itemList = new ArrayDynamic(DataType.STRING);

        // Collect all item names from all areas
        for (String name : areas.keySet()) {
            itemList.add(name);
        }

        return itemList;
    }

    /**
     * scr.getScreenItemList(screenName) -> ArrayDynamic Returns a list of
     * all item names in the screen (same as getItemList). This is an alias for
     * getItemList for clarity.
     */
    public static Object screenGetScreenItemList(InterpreterContext context, Object[] args) throws InterpreterError {
        // This is simply an alias for getItemList
        return screenGetItemList(context, args);
    }

    /* 
     * scr.setStatus(screenName, status) -> Boolean
     * Sets the status of a screen to "clean", "changed", or "error"
     */
    public static Object screenSetStatus(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String statusStr = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setStatus: screenName parameter cannot be null or empty");
        }
        if (statusStr == null || statusStr.isEmpty()) {
            throw new InterpreterError("scr.setStatus: status parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.setStatus: screen '" + screenName + "' not found");
        }

        // Parse status
        com.eb.script.interpreter.screen.ScreenStatus status
                = com.eb.script.interpreter.screen.ScreenStatus.fromString(statusStr);

        // Set the status
        context.setScreenStatus(screenName, status);

        return true;
    }
    /* scr.getStatus(screenName) -> String
     * Gets the current status of a screen: "clean", "changed", or "error"
     */
    public static Object screenGetStatus(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getStatus: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getStatus: screen '" + screenName + "' not found");
        }

        // Get the status
        com.eb.script.interpreter.screen.ScreenStatus status = context.getScreenStatus(screenName);

        return status.toString();
    }

    /**
     * scr.setError(screenName, errorMessage) -> Boolean Sets an error
     * message for a screen and automatically sets status to "error"
     */
    public static Object screenSetError(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String errorMessage = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setError: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.setError: screen '" + screenName + "' not found");
        }

        // Set the error message (this automatically sets status to ERROR)
        context.setScreenErrorMessage(screenName, errorMessage);

        return true;
    }

    /* scr.getError(screenName) -> String
     * Gets the error message for a screen (returns   null { if no   { error }} )
     */
    public static Object screenGetError(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getError: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getError: screen '" + screenName + "' not found");
        }

        // Get the error message
        return context.getScreenErrorMessage(screenName);
    }

    /**
     * scr.setStatusBarMessage(screenName, message) -> Boolean
     * Sets a message in the screen's status bar.
     */
    public static Object screenSetStatusBarMessage(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String message = args.length > 1 && args[1] != null ? String.valueOf(args[1]) : "";

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setStatusBarMessage: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        String normalizedName = screenName.toLowerCase();
        if (!context.getScreens().containsKey(normalizedName)) {
            throw new InterpreterError("scr.setStatusBarMessage: screen '" + screenName + "' not found");
        }

        // Get the status bar and set the message
        com.eb.ui.ebs.StatusBar statusBar = context.getScreenStatusBars().get(normalizedName);
        if (statusBar != null) {
            javafx.application.Platform.runLater(() -> {
                statusBar.setMessage(message);
            });
            return true;
        }

        return false;
    }

    /**
     * scr.setVarStateful(screenName, varName, stateful) -> Boolean
     * Sets whether changes to a variable should mark the screen as changed
     */
    public static Object screenSetVarStateful(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];
        Boolean stateful = (Boolean) args[2];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setVarStateful: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.setVarStateful: varName parameter cannot be null or empty");
        }
        if (stateful == null) {
            throw new InterpreterError("scr.setVarStateful: stateful parameter cannot be null");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.setVarStateful: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        var.setStateful(stateful);
        return true;
    }

    /**
     * scr.getVarStateful(screenName, varName) -> Boolean
     * Gets whether changes to a variable mark the screen as changed
     */
    public static Object screenGetVarStateful(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getVarStateful: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.getVarStateful: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.getVarStateful: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        return var.getStateful();
    }

    /**
     * scr.getVarOriginalValue(screenName, varName) -> Any
     * Gets the original value of a variable (before any modifications)
     */
    public static Object screenGetVarOriginalValue(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getVarOriginalValue: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.getVarOriginalValue: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.getVarOriginalValue: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        return var.getOriginalValue();
    }

    /**
     * scr.submitVarItem(screenName, varName) -> Boolean
     * Marks the variable as submitted (sets original value to current value)
     */
    public static Object screenSubmitVarItem(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.submitVarItem: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.submitVarItem: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.submitVarItem: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        var.resetOriginalValue();
        return true;
    }

    /**
     * scr.resetVarItem(screenName, varName) -> Boolean
     * Resets the variable to its original value
     */
    public static Object screenResetVarItem(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.resetVarItem: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.resetVarItem: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.resetVarItem: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        // Reset to original value
        var.setValue(var.getOriginalValue());
        
        // Also update the screen variable map
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
        if (screenVars != null) {
            screenVars.put(varName.toLowerCase(), var.getOriginalValue());
        }
        
        return true;
    }

    /**
     * scr.clearVarItem(screenName, varName) -> Boolean
     * Clears the variable to its default/empty value
     */
    public static Object screenClearVarItem(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.clearVarItem: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.clearVarItem: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.clearVarItem: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        // Clear to default value
        var.setValue(var.getDefaultValue());
        
        // Also update the screen variable map
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
        if (screenVars != null) {
            screenVars.put(varName.toLowerCase(), var.getDefaultValue());
        }
        
        return true;
    }

    /**
     * scr.initVarItem(screenName, varName) -> Boolean
     * Initializes the variable to its default value and sets it as the original value
     */
    public static Object screenInitVarItem(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String varName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.initVarItem: screenName parameter cannot be null or empty");
        }
        if (varName == null || varName.isEmpty()) {
            throw new InterpreterError("scr.initVarItem: varName parameter cannot be null or empty");
        }

        // Find the variable
        Var var = findVar(context, screenName, varName);
        if (var == null) {
            throw new InterpreterError("scr.initVarItem: variable '" + varName + "' not found in screen '" + screenName + "'");
        }

        // Set to default value
        var.setValue(var.getDefaultValue());
        var.setOriginalValue(var.getDefaultValue());
        
        // Also update the screen variable map
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
        if (screenVars != null) {
            screenVars.put(varName.toLowerCase(), var.getDefaultValue());
        }
        
        return true;
    }
    
    /**
     * Helper method to find a Var by screen name and variable name
     */
    private static Var findVar(InterpreterContext context, String screenName, String varName) throws InterpreterError {
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("Screen '" + screenName + "' not found");
        }

        // Get the var items
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null) {
            throw new InterpreterError("No variables defined for screen '" + screenName + "'");
        }

        // Find the variable - try with various key formats
        String lowerVarName = varName.toLowerCase();

        for (Map.Entry<String, Var> entry : varItems.entrySet()) {
            String key = entry.getKey();
            Var v = entry.getValue();
            // Match by key or by variable name
            if (key.equals(lowerVarName) || key.endsWith("." + lowerVarName)
                    || (v.getName() != null && v.getName().equalsIgnoreCase(varName))) {
                return v;
            }
        }

        return null;
    }

    /**
     * scr.getItemStatus(screenName, itemName) -> String Gets the status of a
     * screen item: "clean" or "changed" Status is determined by comparing
     * current value to original value
     */
    public static Object screenGetItemStatus(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getItemStatus: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.getItemStatus: itemName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getItemStatus: screen '" + screenName + "' not found");
        }

        // Get the var item
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null) {
            throw new InterpreterError("scr.getItemStatus: no variables defined for screen '" + screenName + "'");
        }

        // Find the variable - try with various key formats
        Var var = null;
        String lowerItemName = itemName.toLowerCase();

        for (Map.Entry<String, Var> entry : varItems.entrySet()) {
            String key = entry.getKey();
            Var v = entry.getValue();
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (v.getName() != null && v.getName().equalsIgnoreCase(itemName))) {
                var = v;
                break;
            }
        }

        if (var == null) {
            throw new InterpreterError("scr.getItemStatus: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get the current value from screenVars (the actual live value)
        java.util.concurrent.ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
        Object currentValue = null;
        if (screenVars != null) {
            currentValue = screenVars.get(itemName.toLowerCase());
        }
        
        // Get the original value from the Var object
        Object originalValue = var.getOriginalValue();
        
        // Compare current value with original value
        if (originalValue == null && currentValue == null) {
            return "clean";
        }
        if (originalValue == null || currentValue == null) {
            return "changed";
        }
        
        return originalValue.equals(currentValue) ? "clean" : "changed";
    }

    /**
     * scr.resetItemOriginalValue(screenName, itemName) -> Boolean Resets the
     * original value to the current value, marking the item as "clean"
     */
    public static Object screenResetItemOriginalValue(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.resetItemOriginalValue: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.resetItemOriginalValue: itemName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.resetItemOriginalValue: screen '" + screenName + "' not found");
        }

        // Get the var item
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null) {
            throw new InterpreterError("scr.resetItemOriginalValue: no variables defined for screen '" + screenName + "'");
        }

        // Find the variable - try with various key formats
        Var var = null;
        String lowerItemName = itemName.toLowerCase();

        for (Map.Entry<String, Var> entry : varItems.entrySet()) {
            String key = entry.getKey();
            Var v = entry.getValue();
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (v.getName() != null && v.getName().equalsIgnoreCase(itemName))) {
                var = v;
                break;
            }
        }

        if (var == null) {
            throw new InterpreterError("scr.resetItemOriginalValue: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Reset the original value to the current value
        var.resetOriginalValue();

        return true;
    }

    /**
     * scr.checkChanged(screenName) -> Boolean Checks if any item in the
     * screen has changed from its original value
     */
    public static Object screenCheckChanged(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.checkChanged: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.checkChanged: screen '" + screenName + "' not found");
        }

        // Get the var items
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null || varItems.isEmpty()) {
            return false; // No variables means nothing changed
        }

        // Check if any variable has changed
        for (Var var : varItems.values()) {
            if (var.hasChanged()) {
                return true;
            }
        }

        return false;
    }

    /**
     * scr.checkError(screenName) -> Boolean Checks if the screen has an
     * error status
     */
    public static Object screenCheckError(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.checkError: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.checkError: screen '" + screenName + "' not found");
        }

        // Get the screen status
        com.eb.script.interpreter.screen.ScreenStatus status = context.getScreenStatus(screenName);

        return status == com.eb.script.interpreter.screen.ScreenStatus.ERROR;
    }

    /**
     * scr.revert(screenName) -> Boolean Reverts all items in the screen to
     * their original values
     */
    public static Object screenRevert(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.revert: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.revert: screen '" + screenName + "' not found");
        }

        // Get the var items and screen vars
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);

        if (varItems == null || varItems.isEmpty()) {
            return true; // No variables to revert
        }

        // Revert each variable to its original value
        for (Var var : varItems.values()) {
            Object originalValue = var.getOriginalValue();
            var.setValue(originalValue);

            // Update the screen vars map as well
            // Note: ConcurrentHashMap does not allow null values, so remove key if value is null
            if (screenVars != null && var.getName() != null) {
                if (originalValue != null) {
                    screenVars.put(var.getName().toLowerCase(), originalValue);
                } else {
                    screenVars.remove(var.getName().toLowerCase());
                }
            }
        }

        return true;
    }

    /**
     * scr.clear(screenName) -> Boolean Clears all items in the screen to
     * their default values (usually blank/null)
     */
    public static Object screenClear(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.clear: screenName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.clear: screen '" + screenName + "' not found");
        }

        // Get the var items and screen vars
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);

        if (varItems == null || varItems.isEmpty()) {
            return true; // No variables to clear
        }

        // Clear each variable to its default value
        for (Var var : varItems.values()) {
            Object defaultValue = var.getDefaultValue();

            // If no default value is set, use appropriate "blank" value based on type
            if (defaultValue == null) {
                DataType type = var.getType();
                if (type == DataType.STRING) {
                    defaultValue = "";
                } else if (type == DataType.INTEGER) {
                    defaultValue = 0;
                } else if (type == DataType.DOUBLE || type == DataType.FLOAT) {
                    defaultValue = 0.0;
                } else if (type == DataType.BOOL) {
                    defaultValue = false;
                }
                // For other types, leave as null
            }

            var.setValue(defaultValue);

            // Update the screen vars map as well
            // Note: ConcurrentHashMap does not allow null values, so remove key if value is null
            if (screenVars != null && var.getName() != null) {
                if (defaultValue != null) {
                    screenVars.put(var.getName().toLowerCase(), defaultValue);
                } else {
                    screenVars.remove(var.getName().toLowerCase());
                }
            }
        }

        return true;
    }

    /**
     * scr.getVarReference(screenName, itemName) -> String
     * Gets the varRef property of a screen area item.
     * Returns the variable reference string (e.g., "clients[0].clientName") or null if not set.
     */
    public static Object screenGetVarReference(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getVarReference: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.getVarReference: itemName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getVarReference: screen '" + screenName + "' not found");
        }

        // Get the area items for this screen
        Map<String, AreaItem> areaItems = context.getScreenAreaItems(screenName);
        if (areaItems == null || areaItems.isEmpty()) {
            throw new InterpreterError("scr.getVarReference: no area items defined for screen '" + screenName + "'");
        }

        // Find the area item - try with various key formats
        AreaItem item = null;
        String lowerItemName = itemName.toLowerCase();

        // Try direct lookup
        for (Map.Entry<String, AreaItem> entry : areaItems.entrySet()) {
            String key = entry.getKey();
            AreaItem ai = entry.getValue();
            // Match by key or by item name
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (ai.name != null && ai.name.equalsIgnoreCase(itemName))) {
                item = ai;
                break;
            }
        }

        if (item == null) {
            throw new InterpreterError("scr.getVarReference: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Return the varRef (may be null)
        return item.varRef;
    }

    /**
     * scr.getAreaProperty(screenName.areaName, propertyName) -> ANY
     * Gets a property value from an area definition in a screen.
     */
    public static Object screenGetAreaProperty(InterpreterContext context, Object[] args) throws InterpreterError {
        String areaPath = (String) args[0];
        String propertyName = (String) args[1];

        if (areaPath == null || areaPath.isEmpty()) {
            throw new InterpreterError("scr.getAreaProperty: area parameter cannot be null or empty");
        }
        if (propertyName == null || propertyName.isEmpty()) {
            throw new InterpreterError("scr.getAreaProperty: property parameter cannot be null or empty");
        }

        // Parse the compound key: screenName.areaName
        String[] parts = areaPath.split("\\.", 2);
        if (parts.length != 2) {
            throw new InterpreterError("scr.getAreaProperty: area must be in format 'screenName.areaName', got: " + areaPath);
        }

        String screenName = resolveScreenNameWithParent(context, parts[0]);
        String areaName = parts[1].toLowerCase();

        // Find the screen areas
        List<AreaDefinition> areas = context.getScreenAreas(screenName);
        if (areas == null) {
            throw new InterpreterError("scr.getAreaProperty: screen '" + screenName + "' not found");
        }

        // Search for the area by name
        AreaDefinition targetArea = findAreaByName(areas, areaName);
        if (targetArea == null) {
            throw new InterpreterError("scr.getAreaProperty: area '" + areaName + "' not found in screen '" + screenName + "'");
        }

        // Get the property from the AreaDefinition
        return getAreaDefinitionProperty(targetArea, propertyName);
    }

    /**
     * scr.setAreaProperty(screenName.areaName, propertyName, value) -> BOOL
     * Sets a property value on an area definition in a screen.
     */
    public static Object screenSetAreaProperty(InterpreterContext context, Object[] args) throws InterpreterError {
        String areaPath = (String) args[0];
        String propertyName = (String) args[1];
        Object value = args[2];

        if (areaPath == null || areaPath.isEmpty()) {
            throw new InterpreterError("scr.setAreaProperty: area parameter cannot be null or empty");
        }
        if (propertyName == null || propertyName.isEmpty()) {
            throw new InterpreterError("scr.setAreaProperty: property parameter cannot be null or empty");
        }

        // Parse the compound key: screenName.areaName
        String[] parts = areaPath.split("\\.", 2);
        if (parts.length != 2) {
            throw new InterpreterError("scr.setAreaProperty: area must be in format 'screenName.areaName', got: " + areaPath);
        }

        String screenName = resolveScreenNameWithParent(context, parts[0]);
        String areaName = parts[1].toLowerCase();

        // Find the screen areas
        List<AreaDefinition> areas = context.getScreenAreas(screenName);
        if (areas == null) {
            throw new InterpreterError("scr.setAreaProperty: screen '" + screenName + "' not found");
        }

        // Search for the area by name
        AreaDefinition targetArea = findAreaByName(areas, areaName);
        if (targetArea == null) {
            throw new InterpreterError("scr.setAreaProperty: area '" + areaName + "' not found in screen '" + screenName + "'");
        }

        // Set the property on the AreaDefinition
        setAreaDefinitionProperty(targetArea, propertyName, value);

        // Apply the property change to the actual JavaFX container on the UI thread
        final String finalScreenName = screenName;
        final String finalAreaName = areaName;
        final String finalPropertyName = propertyName;
        final Object finalValue = value;

        javafx.application.Platform.runLater(() -> {
            try {
                // Get the area container from context
                javafx.scene.layout.Region container = context.getAreaContainer(finalScreenName, finalAreaName);
                if (container != null) {
                    applyPropertyToAreaContainer(container, finalPropertyName, finalValue);
                }
            } catch (Exception e) {
                System.err.println("Error applying property to area container: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return Boolean.TRUE;
    }

    /**
     * scr.setItemChoiceOptions(screenName, itemName, optionsMap) -> Boolean
     * Sets the choice options for a ChoiceBox or ComboBox screen item using a map.
     * The map keys are data values (stored when selected), and values are display text (shown to users).
     */
    public static Object screenSetItemChoiceOptions(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];
        Object optionsMapArg = args[2];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setItemChoiceOptions: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.setItemChoiceOptions: itemName parameter cannot be null or empty");
        }
        if (optionsMapArg == null) {
            throw new InterpreterError("scr.setItemChoiceOptions: optionsMap parameter cannot be null");
        }
        if (!(optionsMapArg instanceof Map)) {
            throw new InterpreterError("scr.setItemChoiceOptions: optionsMap parameter must be a map, got: " + optionsMapArg.getClass().getSimpleName());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> optionsMap = (Map<String, Object>) optionsMapArg;

        // Verify screen exists (check both screen config and displayed screens)
        if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.setItemChoiceOptions: screen '" + screenName + "' not found");
        }

        // Get the area items for this screen
        Map<String, AreaItem> areaItems = context.getScreenAreaItems(screenName);
        if (areaItems == null || areaItems.isEmpty()) {
            throw new InterpreterError("scr.setItemChoiceOptions: no area items defined for screen '" + screenName + "'");
        }

        // Find the area item
        AreaItem item = null;
        String lowerItemName = itemName.toLowerCase();

        for (Map.Entry<String, AreaItem> entry : areaItems.entrySet()) {
            String key = entry.getKey();
            AreaItem ai = entry.getValue();
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (ai.name != null && ai.name.equalsIgnoreCase(itemName))) {
                item = ai;
                break;
            }
        }

        if (item == null) {
            throw new InterpreterError("scr.setItemChoiceOptions: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get or create the display item
        DisplayItem displayItem = item.displayItem;
        if (displayItem == null) {
            displayItem = new DisplayItem();
            item.displayItem = displayItem;
        }

        // Convert the map to String values and store in optionsMap
        java.util.LinkedHashMap<String, String> convertedOptionsMap = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : optionsMap.entrySet()) {
            convertedOptionsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        displayItem.setOptionsMap(convertedOptionsMap);

        // Also update the JavaFX control on the UI thread
        final String finalScreenName = screenName;
        final String finalItemName = itemName;
        final java.util.Map<String, String> finalOptionsMap = convertedOptionsMap;

        javafx.application.Platform.runLater(() -> {
            try {
                // Try to find controls with case-insensitive screen name matching
                java.util.List<javafx.scene.Node> controls = null;
                String actualScreenKey = null;
                for (String key : context.getScreenBoundControls().keySet()) {
                    if (key.equalsIgnoreCase(finalScreenName)) {
                        controls = context.getScreenBoundControls().get(key);
                        actualScreenKey = key;
                        break;
                    }
                }
                
                if (controls != null) {
                    // Build targetUserData using the actual screen key (as stored) for matching
                    String targetUserData = actualScreenKey + "." + finalItemName;
                    for (javafx.scene.Node control : controls) {
                        Object userData = control.getUserData();
                        // Try case-insensitive match for userData
                        boolean matches = targetUserData.equals(userData) || 
                                          (userData instanceof String && targetUserData.equalsIgnoreCase((String) userData));
                        if (matches) {
                            if (control instanceof javafx.scene.control.ChoiceBox) {
                                @SuppressWarnings("unchecked")
                                javafx.scene.control.ChoiceBox<String> choiceBox = (javafx.scene.control.ChoiceBox<String>) control;
                                // Store the current selection (display text)
                                String currentDisplayValue = choiceBox.getValue();
                                // Clear and add new items (values from the map are display text)
                                choiceBox.getItems().clear();
                                choiceBox.getItems().addAll(finalOptionsMap.values());
                                // Store the optionsMap in the control's properties
                                choiceBox.getProperties().put("optionsMap", finalOptionsMap);
                                // Restore selection if the display value is still in the new options
                                if (currentDisplayValue != null && finalOptionsMap.containsValue(currentDisplayValue)) {
                                    choiceBox.setValue(currentDisplayValue);
                                }
                            } else if (control instanceof javafx.scene.control.ComboBox) {
                                @SuppressWarnings("unchecked")
                                javafx.scene.control.ComboBox<String> comboBox = (javafx.scene.control.ComboBox<String>) control;
                                // Store the current selection (display text)
                                String currentDisplayValue = comboBox.getValue();
                                // Clear and add new items (values from the map are display text)
                                comboBox.getItems().clear();
                                comboBox.getItems().addAll(finalOptionsMap.values());
                                // Store the optionsMap in the control's properties
                                comboBox.getProperties().put("optionsMap", finalOptionsMap);
                                // Restore selection if the display value is still in the new options
                                if (currentDisplayValue != null && finalOptionsMap.containsValue(currentDisplayValue)) {
                                    comboBox.setValue(currentDisplayValue);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error setting choice options on control: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return Boolean.TRUE;
    }

    /**
     * scr.getItemChoiceOptions(screenName, itemName) -> Map
     * Gets the choice options for a ChoiceBox or ComboBox screen item.
     * Returns a map where keys are data values and values are display text.
     */
    public static Object screenGetItemChoiceOptions(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getItemChoiceOptions: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.getItemChoiceOptions: itemName parameter cannot be null or empty");
        }

        // Verify screen exists (check both screen config and displayed screens)
        if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getItemChoiceOptions: screen '" + screenName + "' not found");
        }

        // Get the area items for this screen
        Map<String, AreaItem> areaItems = context.getScreenAreaItems(screenName);
        if (areaItems == null || areaItems.isEmpty()) {
            throw new InterpreterError("scr.getItemChoiceOptions: no area items defined for screen '" + screenName + "'");
        }

        // Find the area item
        AreaItem item = null;
        String lowerItemName = itemName.toLowerCase();

        for (Map.Entry<String, AreaItem> entry : areaItems.entrySet()) {
            String key = entry.getKey();
            AreaItem ai = entry.getValue();
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (ai.name != null && ai.name.equalsIgnoreCase(itemName))) {
                item = ai;
                break;
            }
        }

        if (item == null) {
            throw new InterpreterError("scr.getItemChoiceOptions: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get the display item
        DisplayItem displayItem = item.displayItem;
        if (displayItem == null) {
            return new java.util.LinkedHashMap<String, String>();
        }

        // Return the optionsMap if present, or convert options to a map
        Map<String, String> optionsMap = displayItem.getOptionsMap();
        if (optionsMap != null && !optionsMap.isEmpty()) {
            return optionsMap;
        } else {
            List<String> options = displayItem.getOptions();
            if (options != null && !options.isEmpty()) {
                // Convert options list to map where key=value (display text is same as data value)
                java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
                for (String option : options) {
                    result.put(option, option);
                }
                return result;
            }
        }

        return new java.util.LinkedHashMap<String, String>();
    }

    /**
     * scr.snapshot(screenName?) -> IMAGE | null
     * Takes a screenshot of the specified screen (or current screen if not specified).
     * 
     * When called WITH a screen name: Returns an EbsImage containing the captured screenshot.
     * When called WITHOUT parameters: Saves screenshot to temp directory with auto-incrementing 
     *                                  sequence (same as Ctrl+P) and returns null.
     */
    public static Object screenSnapshot(InterpreterContext context, Object[] args) throws InterpreterError {
        // Determine mode based on whether parameters were provided
        // saveToFile = true when called without parameters (same as Ctrl+P)
        boolean saveToFile = (args.length == 0);
        
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isBlank()) {
            screenName = context.getCurrentScreen();
            if (screenName == null || screenName.isBlank()) {
                throw new InterpreterError(
                        "scr.snapshot: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }
        
        // Normalize screen name to lowercase to match how screens are stored
        screenName = screenName.toLowerCase();

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.snapshot: Screen '" + screenName + "' does not exist or is not shown.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.snapshot: Screen '" + screenName + "' is still being initialized.");
        }

        // If saveToFile mode (no parameters), use the same logic as Ctrl+P
        if (saveToFile) {
            final String finalScreenName = screenName;
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
                = new java.util.concurrent.atomic.AtomicReference<>();
            
            javafx.application.Platform.runLater(() -> {
                try {
                    com.eb.script.interpreter.screen.ScreenFactory.captureScreenshotToFile(
                        finalScreenName, stage, context);
                } catch (Exception e) {
                    errorRef.set(new InterpreterError("scr.snapshot: Error capturing screenshot: " + e.getMessage()));
                } finally {
                    latch.countDown();
                }
            });
            
            try {
                boolean completed = latch.await(SNAPSHOT_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    throw new InterpreterError("scr.snapshot: Timeout waiting for screenshot capture (waited " 
                        + SNAPSHOT_TIMEOUT_SECONDS + " seconds).");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterpreterError("scr.snapshot: Interrupted while waiting for screenshot capture.");
            }
            
            if (errorRef.get() != null) {
                throw errorRef.get();
            }
            
            return null; // Return null when saving to file
        }

        // Original behavior: return EbsImage when screen name is provided
        final String finalScreenName = screenName;
        
        // Capture the screenshot on JavaFX Application Thread using CountDownLatch for synchronization
        final java.util.concurrent.atomic.AtomicReference<com.eb.script.image.EbsImage> imageRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = stage.getScene();
                if (scene == null) {
                    errorRef.set(new InterpreterError("scr.snapshot: Screen '" + finalScreenName + "' has no scene to capture."));
                    latch.countDown();
                    return;
                }

                // Take the snapshot
                javafx.scene.image.WritableImage snapshot = scene.snapshot(null);
                
                if (snapshot == null) {
                    errorRef.set(new InterpreterError("scr.snapshot: Failed to capture screenshot of screen '" + finalScreenName + "'."));
                    latch.countDown();
                    return;
                }

                // Create EbsImage from the snapshot
                // Note: Format and naming are configured via class constants
                // DEFAULT_SNAPSHOT_FORMAT = "png", SNAPSHOT_NAME_SUFFIX = "_screenshot"
                com.eb.script.image.EbsImage ebsImage = new com.eb.script.image.EbsImage(
                    snapshot, 
                    finalScreenName + SNAPSHOT_NAME_SUFFIX, 
                    DEFAULT_SNAPSHOT_FORMAT
                );
                
                imageRef.set(ebsImage);
                
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screenshot captured from screen '" + finalScreenName + "' (" 
                        + (int)snapshot.getWidth() + "x" + (int)snapshot.getHeight() + " pixels)");
                }
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.snapshot: Error capturing screenshot: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });

        // Wait for the snapshot to complete (with configurable timeout)
        // SNAPSHOT_TIMEOUT_SECONDS can be adjusted for complex screens
        try {
            boolean completed = latch.await(SNAPSHOT_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                throw new InterpreterError("scr.snapshot: Timeout waiting for screenshot capture (waited " 
                    + SNAPSHOT_TIMEOUT_SECONDS + " seconds).");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.snapshot: Interrupted while waiting for screenshot capture.");
        }

        // Check if there was an error
        if (errorRef.get() != null) {
            throw errorRef.get();
        }

        // Return the captured image
        com.eb.script.image.EbsImage result = imageRef.get();
        if (result == null) {
            throw new InterpreterError("scr.snapshot: Failed to capture screenshot (result was null).");
        }
        
        return result;
    }

    /**
     * Helper method to recursively find an area by name.
     */
    private static AreaDefinition findAreaByName(List<AreaDefinition> areas, String areaName) {
        for (AreaDefinition area : areas) {
            if (area.name != null && area.name.toLowerCase().equals(areaName)) {
                return area;
            }
            // Recursively search in child areas
            if (!area.childAreas.isEmpty()) {
                AreaDefinition found = findAreaByName(area.childAreas, areaName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to get a property from an AreaDefinition.
     */
    private static Object getAreaDefinitionProperty(AreaDefinition area, String propertyName) throws InterpreterError {
        String propLower = propertyName.toLowerCase();
        
        return switch (propLower) {
            case "name" -> area.name;
            case "type" -> area.type;
            case "areatype" -> area.areaType != null ? area.areaType.getTypeName() : null;
            case "cssclass" -> area.cssClass;
            case "layout" -> area.layout;
            case "style" -> area.style;
            case "screenname" -> area.screenName;
            case "displayname" -> area.displayName;
            case "title" -> area.title;
            case "groupborder" -> area.groupBorder;
            case "groupbordercolor" -> area.groupBorderColor;
            case "grouplabeltext" -> area.groupLabelText;
            case "grouplabelalignment" -> area.groupLabelAlignment;
            case "grouplabeloffset" -> area.groupLabelOffset;
            case "spacing" -> area.spacing;
            case "padding" -> area.padding;
            case "alignment" -> area.alignment;
            case "gainfocus" -> area.gainFocus;
            case "lostfocus" -> area.lostFocus;
            case "numberofrecords" -> area.numberOfRecords;
            case "recordref" -> area.recordRef;
            case "hgrow" -> area.hgrow;
            case "vgrow" -> area.vgrow;
            default -> throw new InterpreterError("scr.getAreaProperty: unknown property '" + propertyName + "'");
        };
    }

    /**
     * Helper method to set a property on an AreaDefinition.
     */
    private static void setAreaDefinitionProperty(AreaDefinition area, String propertyName, Object value) throws InterpreterError {
        String propLower = propertyName.toLowerCase();
        
        switch (propLower) {
            case "name" -> area.name = value != null ? String.valueOf(value) : null;
            case "type" -> {
                area.type = value != null ? String.valueOf(value) : null;
                if (area.type != null) {
                    area.areaType = AreaDefinition.AreaType.fromString(area.type);
                    area.cssClass = area.areaType.getCssClass();
                }
            }
            case "cssclass" -> area.cssClass = value != null ? String.valueOf(value) : null;
            case "layout" -> area.layout = value != null ? String.valueOf(value) : null;
            case "style" -> area.style = value != null ? String.valueOf(value) : null;
            case "displayname" -> area.displayName = value != null ? String.valueOf(value) : null;
            case "title" -> area.title = value != null ? String.valueOf(value) : null;
            case "groupborder" -> area.groupBorder = value != null ? String.valueOf(value) : null;
            case "groupbordercolor" -> area.groupBorderColor = value != null ? String.valueOf(value) : null;
            case "grouplabeltext" -> area.groupLabelText = value != null ? String.valueOf(value) : null;
            case "grouplabelalignment" -> area.groupLabelAlignment = value != null ? String.valueOf(value) : null;
            case "grouplabeloffset" -> area.groupLabelOffset = value != null ? String.valueOf(value) : null;
            case "spacing" -> area.spacing = value != null ? String.valueOf(value) : null;
            case "padding" -> area.padding = value != null ? String.valueOf(value) : null;
            case "alignment" -> area.alignment = value != null ? String.valueOf(value) : null;
            case "hgrow" -> area.hgrow = value != null ? String.valueOf(value) : null;
            case "vgrow" -> area.vgrow = value != null ? String.valueOf(value) : null;
            case "gainfocus" -> area.gainFocus = value != null ? String.valueOf(value) : null;
            case "lostfocus" -> area.lostFocus = value != null ? String.valueOf(value) : null;
            case "numberofrecords" -> {
                if (value == null) {
                    area.numberOfRecords = null;
                } else if (value instanceof Number) {
                    area.numberOfRecords = ((Number) value).intValue();
                } else {
                    throw new InterpreterError("scr.setAreaProperty: 'numberOfRecords' property must be a number");
                }
            }
            case "recordref" -> area.recordRef = value != null ? String.valueOf(value) : null;
            case "visible", "managed" -> {
                // These are UI-only properties that are applied directly to the container
                // They don't need to be stored in AreaDefinition since they're handled by applyPropertyToAreaContainer
                if (!(value instanceof Boolean)) {
                    throw new InterpreterError("scr.setAreaProperty: '" + propertyName + "' property must be a boolean");
                }
            }
            case "screenname", "areatype" -> 
                throw new InterpreterError("scr.setAreaProperty: property '" + propertyName + "' is read-only");
            default -> throw new InterpreterError("scr.setAreaProperty: unknown property '" + propertyName + "'");
        }
    }

    /**
     * Helper method to recursively find an item by name in an area definition.
     */
    private static AreaItem findItemInArea(InterpreterContext context, AreaDefinition area, String itemName) {
        // Search in this area's items
        Map<String, AreaItem> items = context.getScreenAreaItems(area.screenName);
        AreaItem areaItem = items.get(itemName);
        if (areaItem != null) {
            return areaItem;
        }
        // Recursively search in child areas
        for (AreaDefinition childArea : area.childAreas) {
            AreaItem found = findItemInArea(context, childArea, itemName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Helper method to set a property on an AreaItem.
     */
    private static void setAreaItemProperty(com.eb.script.interpreter.screen.AreaItem item,
            String propertyName, Object value) throws InterpreterError {
        String propLower = propertyName.toLowerCase();

        switch (propLower) {
            case "value", "text" -> {
                // The "value" and "text" properties are handled by applyPropertyToControl
                // but we accept them here without error to allow the JavaFX control update
            }
            case "editable" -> {
                if (value instanceof Boolean) {
                    item.editable = (Boolean) value;
                } else {
                    throw new InterpreterError("scr.setProperty: 'editable' property must be a boolean");
                }
            }
            case "disabled" -> {
                if (value instanceof Boolean) {
                    item.disabled = (Boolean) value;
                } else {
                    throw new InterpreterError("scr.setProperty: 'disabled' property must be a boolean");
                }
            }
            case "visible" -> {
                if (value instanceof Boolean) {
                    item.visible = (Boolean) value;
                } else {
                    throw new InterpreterError("scr.setProperty: 'visible' property must be a boolean");
                }
            }
            case "tooltip" ->
                item.tooltip = value != null ? String.valueOf(value) : null;
            case "textcolor" ->
                item.textColor = value != null ? String.valueOf(value) : null;
            case "backgroundcolor" ->
                item.backgroundColor = value != null ? String.valueOf(value) : null;
            case "colspan" -> {
                if (value instanceof Number) {
                    item.colSpan = ((Number) value).intValue();
                } else if (value == null) {
                    item.colSpan = null;
                } else {
                    throw new InterpreterError("scr.setProperty: 'colSpan' property must be a number");
                }
            }
            case "rowspan" -> {
                if (value instanceof Number) {
                    item.rowSpan = ((Number) value).intValue();
                } else if (value == null) {
                    item.rowSpan = null;
                } else {
                    throw new InterpreterError("scr.setProperty: 'rowSpan' property must be a number");
                }
            }
            case "hgrow" ->
                item.hgrow = value != null ? String.valueOf(value).toUpperCase() : null;
            case "vgrow" ->
                item.vgrow = value != null ? String.valueOf(value).toUpperCase() : null;
            case "margin" ->
                item.margin = value != null ? String.valueOf(value) : null;
            case "padding" ->
                item.padding = value != null ? String.valueOf(value) : null;
            case "prefwidth" ->
                item.prefWidth = value != null ? String.valueOf(value) : null;
            case "prefheight" ->
                item.prefHeight = value != null ? String.valueOf(value) : null;
            case "minwidth" ->
                item.minWidth = value != null ? String.valueOf(value) : null;
            case "minheight" ->
                item.minHeight = value != null ? String.valueOf(value) : null;
            case "maxwidth" ->
                item.maxWidth = value != null ? String.valueOf(value) : null;
            case "maxheight" ->
                item.maxHeight = value != null ? String.valueOf(value) : null;
            case "alignment" ->
                item.alignment = value != null ? String.valueOf(value).toLowerCase() : null;
            default ->
                throw new InterpreterError("scr.setProperty: unknown property '" + propertyName + "'");
        }
    }

    /**
     * Helper method to get a property value from an AreaItem.
     */
    private static Object getAreaItemProperty(com.eb.script.interpreter.screen.AreaItem item,
            String propertyName) throws InterpreterError {
        String propLower = propertyName.toLowerCase();

        return switch (propLower) {
            case "editable" ->
                item.editable;
            case "disabled" ->
                item.disabled;
            case "visible" ->
                item.visible;
            case "tooltip" ->
                item.tooltip;
            case "textcolor" ->
                item.textColor;
            case "backgroundcolor" ->
                item.backgroundColor;
            case "colspan" ->
                item.colSpan;
            case "rowspan" ->
                item.rowSpan;
            case "hgrow" ->
                item.hgrow;
            case "vgrow" ->
                item.vgrow;
            case "margin" ->
                item.margin;
            case "padding" ->
                item.padding;
            case "prefwidth" ->
                item.prefWidth;
            case "prefheight" ->
                item.prefHeight;
            case "minwidth" ->
                item.minWidth;
            case "minheight" ->
                item.minHeight;
            case "maxwidth" ->
                item.maxWidth;
            case "maxheight" ->
                item.maxHeight;
            case "alignment" ->
                item.alignment;
            default ->
                throw new InterpreterError("scr.getProperty: unknown property '" + propertyName + "'");
        };
    }

    /**
     * Helper method to apply a property change to a JavaFX control. This method
     * is called on the JavaFX Application Thread.
     */
    private static void applyPropertyToControl(javafx.scene.Node control, String propertyName, Object value) {
        String propLower = propertyName.toLowerCase();

        switch (propLower) {
            case "value", "text" -> {
                // Set the text/value of the control
                String textValue = value != null ? String.valueOf(value) : "";
                if (control instanceof javafx.scene.control.TextField) {
                    ((javafx.scene.control.TextField) control).setText(textValue);
                } else if (control instanceof javafx.scene.control.TextArea) {
                    ((javafx.scene.control.TextArea) control).setText(textValue);
                } else if (control instanceof javafx.scene.web.WebView) {
                    // For WebView, load the content as HTML
                    ((javafx.scene.web.WebView) control).getEngine().loadContent(textValue);
                } else if (control instanceof javafx.scene.control.Label) {
                    ((javafx.scene.control.Label) control).setText(textValue);
                } else if (control instanceof javafx.scene.control.Button) {
                    ((javafx.scene.control.Button) control).setText(textValue);
                }
            }
            case "editable" -> {
                if (value instanceof Boolean) {
                    boolean boolVal = (Boolean) value;
                    if (control instanceof javafx.scene.control.TextField) {
                        ((javafx.scene.control.TextField) control).setEditable(boolVal);
                    } else if (control instanceof javafx.scene.control.TextArea) {
                        ((javafx.scene.control.TextArea) control).setEditable(boolVal);
                    } else if (control instanceof javafx.scene.control.ComboBox) {
                        ((javafx.scene.control.ComboBox<?>) control).setEditable(boolVal);
                    }
                }
            }
            case "disabled" -> {
                if (value instanceof Boolean) {
                    control.setDisable((Boolean) value);
                }
            }
            case "visible" -> {
                if (value instanceof Boolean) {
                    control.setVisible((Boolean) value);
                }
            }
            case "tooltip" -> {
                if (control instanceof javafx.scene.control.Control) {
                    String tooltipText = value != null ? String.valueOf(value) : null;
                    if (tooltipText != null && !tooltipText.isEmpty()) {
                        ((javafx.scene.control.Control) control).setTooltip(
                                new javafx.scene.control.Tooltip(tooltipText));
                    } else {
                        ((javafx.scene.control.Control) control).setTooltip(null);
                    }
                }
            }
            case "textcolor" -> {
                if (value != null) {
                    String color = String.valueOf(value);
                    String currentStyle = control.getStyle();
                    String newStyle = currentStyle == null ? "" : currentStyle;
                    // Remove old text color if present
                    newStyle = newStyle.replaceAll("-fx-text-fill:\\s*[^;]+;?", "");
                    newStyle += " -fx-text-fill: " + color + ";";
                    control.setStyle(newStyle.trim());
                }
            }
            case "backgroundcolor" -> {
                if (value != null) {
                    String color = String.valueOf(value);
                    String currentStyle = control.getStyle();
                    String newStyle = currentStyle == null ? "" : currentStyle;
                    // Remove old background color if present
                    newStyle = newStyle.replaceAll("-fx-background-color:\\s*[^;]+;?", "");
                    newStyle += " -fx-background-color: " + color + ";";
                    control.setStyle(newStyle.trim());
                }
            }
            case "prefwidth" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double width = Double.parseDouble(String.valueOf(value));
                            region.setPrefWidth(width);
                        } catch (NumberFormatException e) {
                            // Ignore invalid width values
                        }
                    }
                }
            }
            case "prefheight" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double height = Double.parseDouble(String.valueOf(value));
                            region.setPrefHeight(height);
                        } catch (NumberFormatException e) {
                            // Ignore invalid height values
                        }
                    }
                }
            }
            case "minwidth" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double width = Double.parseDouble(String.valueOf(value));
                            region.setMinWidth(width);
                        } catch (NumberFormatException e) {
                            // Ignore invalid width values
                        }
                    }
                }
            }
            case "minheight" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double height = Double.parseDouble(String.valueOf(value));
                            region.setMinHeight(height);
                        } catch (NumberFormatException e) {
                            // Ignore invalid height values
                        }
                    }
                }
            }
            case "maxwidth" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double width = Double.parseDouble(String.valueOf(value));
                            region.setMaxWidth(width);
                        } catch (NumberFormatException e) {
                            // Ignore invalid width values
                        }
                    }
                }
            }
            case "maxheight" -> {
                if (control instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) control;
                    if (value != null) {
                        try {
                            double height = Double.parseDouble(String.valueOf(value));
                            region.setMaxHeight(height);
                        } catch (NumberFormatException e) {
                            // Ignore invalid height values
                        }
                    }
                }
            }
            // Note: Other properties like colspan, rowspan, hgrow, vgrow, margin, padding, alignment
            // are layout-specific and would require re-layouting the parent container to apply.
            // These are stored in the AreaItem but not directly applied to the control at runtime.
        }
    }

    /**
     * Helper method to apply a property change to a JavaFX area container. This method
     * is called on the JavaFX Application Thread.
     */
    private static void applyPropertyToAreaContainer(javafx.scene.layout.Region container, String propertyName, Object value) {
        String propLower = propertyName.toLowerCase();

        switch (propLower) {
            case "style" -> {
                if (value != null) {
                    container.setStyle(String.valueOf(value));
                } else {
                    container.setStyle("");
                }
            }
            case "cssclass" -> {
                if (value != null) {
                    String newCssClass = String.valueOf(value);
                    // Get the previous user-set CSS class from properties (if any)
                    String previousCssClass = (String) container.getProperties().get("userCssClass");
                    
                    // Remove the previous user CSS class if it was set
                    if (previousCssClass != null && !previousCssClass.isEmpty()) {
                        container.getStyleClass().remove(previousCssClass);
                    }
                    
                    // Add the new CSS class if not empty
                    if (!newCssClass.isEmpty()) {
                        if (!container.getStyleClass().contains(newCssClass)) {
                            container.getStyleClass().add(newCssClass);
                        }
                        // Store the new class for future removal
                        container.getProperties().put("userCssClass", newCssClass);
                    } else {
                        container.getProperties().remove("userCssClass");
                    }
                }
            }
            case "spacing" -> {
                if (value != null) {
                    try {
                        double spacing = Double.parseDouble(String.valueOf(value));
                        if (container instanceof javafx.scene.layout.HBox) {
                            ((javafx.scene.layout.HBox) container).setSpacing(spacing);
                        } else if (container instanceof javafx.scene.layout.VBox) {
                            ((javafx.scene.layout.VBox) container).setSpacing(spacing);
                        } else if (container instanceof javafx.scene.layout.FlowPane) {
                            ((javafx.scene.layout.FlowPane) container).setHgap(spacing);
                            ((javafx.scene.layout.FlowPane) container).setVgap(spacing);
                        } else if (container instanceof javafx.scene.layout.TilePane) {
                            ((javafx.scene.layout.TilePane) container).setHgap(spacing);
                            ((javafx.scene.layout.TilePane) container).setVgap(spacing);
                        } else if (container instanceof javafx.scene.layout.GridPane) {
                            ((javafx.scene.layout.GridPane) container).setHgap(spacing);
                            ((javafx.scene.layout.GridPane) container).setVgap(spacing);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid spacing values
                    }
                }
            }
            case "padding" -> {
                if (value != null) {
                    javafx.geometry.Insets padding = parseInsets(String.valueOf(value));
                    if (padding != null) {
                        container.setPadding(padding);
                    }
                }
            }
            case "title" -> {
                if (container instanceof javafx.scene.control.TitledPane) {
                    ((javafx.scene.control.TitledPane) container).setText(value != null ? String.valueOf(value) : "");
                }
            }
            case "prefwidth" -> {
                if (value != null) {
                    try {
                        double width = Double.parseDouble(String.valueOf(value));
                        container.setPrefWidth(width);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "prefheight" -> {
                if (value != null) {
                    try {
                        double height = Double.parseDouble(String.valueOf(value));
                        container.setPrefHeight(height);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "minwidth" -> {
                if (value != null) {
                    try {
                        double width = Double.parseDouble(String.valueOf(value));
                        container.setMinWidth(width);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "minheight" -> {
                if (value != null) {
                    try {
                        double height = Double.parseDouble(String.valueOf(value));
                        container.setMinHeight(height);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "maxwidth" -> {
                if (value != null) {
                    try {
                        double width = Double.parseDouble(String.valueOf(value));
                        container.setMaxWidth(width);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "maxheight" -> {
                if (value != null) {
                    try {
                        double height = Double.parseDouble(String.valueOf(value));
                        container.setMaxHeight(height);
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            case "visible" -> {
                if (value instanceof Boolean) {
                    container.setVisible((Boolean) value);
                }
            }
            case "managed" -> {
                if (value instanceof Boolean) {
                    container.setManaged((Boolean) value);
                }
            }
            // Note: Some area properties like name, type, areaType, screenName are read-only
            // and cannot be changed at runtime. Others like groupBorder properties would require
            // re-creating the border styling which is complex.
        }
    }

    /**
     * Helper method to parse insets string to Insets object.
     * Supports formats: "10" (all), "10 5" (vertical horizontal),
     * "10 5 10 5" (top right bottom left).
     */
    private static javafx.geometry.Insets parseInsets(String insetsStr) {
        if (insetsStr == null || insetsStr.isEmpty()) {
            return null;
        }

        String[] parts = insetsStr.trim().split("\\s+");

        try {
            if (parts.length == 1) {
                double all = Double.parseDouble(parts[0]);
                return new javafx.geometry.Insets(all);
            } else if (parts.length == 2) {
                double vertical = Double.parseDouble(parts[0]);
                double horizontal = Double.parseDouble(parts[1]);
                return new javafx.geometry.Insets(vertical, horizontal, vertical, horizontal);
            } else if (parts.length == 4) {
                double top = Double.parseDouble(parts[0]);
                double right = Double.parseDouble(parts[1]);
                double bottom = Double.parseDouble(parts[2]);
                double left = Double.parseDouble(parts[3]);
                return new javafx.geometry.Insets(top, right, bottom, left);
            }
        } catch (NumberFormatException e) {
            // Return null for invalid format
        }

        return null;
    }
    
    /**
     * Resolves a screen.item path by trying the original screen name first,
     * then prepending the parent screen name if not found.
     * 
     * When a child screen is shown from within a parent screen context,
     * the child screen's areas are registered under its simple name.
     * However, the user might pass "screen.item" expecting it to resolve
     * to "parent.screen.item".
     * 
     * @param context The interpreter context
     * @param screenName The original screen name from the path
     * @return The resolved screen name, or the original if no parent fallback is needed
     */
    private static String resolveScreenNameWithParent(InterpreterContext context, String screenName) {
        // Screen areas are stored under simple names (e.g., "askaiscreen")
        // Bound controls are stored under qualified names (e.g., "regexscreen.askaiscreen")
        // We need to resolve both correctly
        
        String screenNameLower = screenName.toLowerCase();
        
        // First, check if screen areas exist under the simple name
        List<AreaDefinition> areas = context.getScreenAreas(screenNameLower);
        if (areas != null) {
            // Areas found under simple name - this is the correct name for area lookups
            return screenNameLower;
        }
        
        // If not found directly, check if the screen has a registered parent
        String parentScreen = context.getScreenParent(screenNameLower);
        if (parentScreen != null) {
            // Screen areas might still be under simple name even with a parent
            // (which we already checked above), so just return simple name
            return screenNameLower;
        }
        
        // Try extracting from current screen context
        String currentScreen = context.getCurrentScreen();
        if (currentScreen != null && !currentScreen.isEmpty()) {
            // If we're in "regexscreen.askaiscreen" and looking for "askaiscreen",
            // the areas are under "askaiscreen" not the qualified name
            String suffix = "." + screenNameLower;
            if (currentScreen.toLowerCase().endsWith(suffix)) {
                // Extract the child screen name and check if areas exist
                if (context.getScreenAreas(screenNameLower) != null) {
                    return screenNameLower;
                }
            }
            
            // Also check if currentScreen equals what we're looking for
            if (currentScreen.toLowerCase().equals(screenNameLower)) {
                return screenNameLower;
            }
        }
        
        // Return simple lowercase name - areas are stored under simple names
        return screenNameLower;
    }
    
    /**
     * scr.setTreeItemIcon(screenName, itemPath, iconPath) -> Boolean
     * Sets a static icon for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath, iconPath]
     * @return Boolean true on success
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenSetTreeItemIcon(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("scr.setTreeItemIcon: requires 3 parameters: screenName, itemPath, iconPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        String iconPath = (String) args[2];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemIcon: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemIcon: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.setTreeItemIcon: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        final String finalIconPath = iconPath;
        
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Update on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcon: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcon: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcon: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Set the icon
                setTreeItemIconGraphic(item, finalIconPath);
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.setTreeItemIcon: error setting icon: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.setTreeItemIcon: interrupted while setting icon");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return Boolean.TRUE;
    }
    
    /**
     * scr.setTreeItemIcons(screenName, itemPath, iconPath, iconOpenPath, iconClosedPath) -> Boolean
     * Sets state-based icons for a tree item (open/closed states for branches).
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath, iconPath, iconOpenPath, iconClosedPath]
     * @return Boolean true on success
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenSetTreeItemIcons(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 5) {
            throw new InterpreterError("scr.setTreeItemIcons: requires 5 parameters: screenName, itemPath, iconPath, iconOpenPath, iconClosedPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        String iconPath = (String) args[2];
        String iconOpenPath = (String) args[3];
        String iconClosedPath = (String) args[4];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemIcons: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemIcons: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.setTreeItemIcons: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        final String finalIconPath = iconPath;
        final String finalIconOpenPath = iconOpenPath;
        final String finalIconClosedPath = iconClosedPath;
        
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Update on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcons: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcons: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemIcons: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get or create icon data for this item
                TreeItemIconData iconData = treeItemIcons.get(item);
                if (iconData == null) {
                    iconData = new TreeItemIconData();
                    treeItemIcons.put(item, iconData);
                }
                
                // Store the icon paths
                iconData.iconPath = finalIconPath;
                iconData.iconOpenPath = finalIconOpenPath;
                iconData.iconClosedPath = finalIconClosedPath;
                
                // Remove any existing expansion listener (with error handling)
                if (iconData.expansionListener != null) {
                    try {
                        item.expandedProperty().removeListener(iconData.expansionListener);
                    } catch (Exception e) {
                        // Ignore errors if listener is already removed or item is invalid
                    }
                    iconData.expansionListener = null;
                }
                
                // Set initial icon based on current state
                boolean isExpanded = item.isExpanded();
                boolean hasChildren = !item.getChildren().isEmpty();
                
                if (hasChildren && (finalIconOpenPath != null || finalIconClosedPath != null)) {
                    // Use state-based icons for branches
                    String currentIconPath = isExpanded ? 
                        (finalIconOpenPath != null ? finalIconOpenPath : finalIconPath) : 
                        (finalIconClosedPath != null ? finalIconClosedPath : finalIconPath);
                    setTreeItemIconGraphic(item, currentIconPath);
                    
                    // Add listener for state changes
                    javafx.beans.value.ChangeListener<Boolean> listener = (obs, wasExpanded, nowExpanded) -> {
                        String newIconPath = nowExpanded ? 
                            (finalIconOpenPath != null ? finalIconOpenPath : finalIconPath) : 
                            (finalIconClosedPath != null ? finalIconClosedPath : finalIconPath);
                        setTreeItemIconGraphic(item, newIconPath);
                    };
                    item.expandedProperty().addListener(listener);
                    iconData.expansionListener = listener;
                } else {
                    // Use static icon for leaves
                    setTreeItemIconGraphic(item, finalIconPath);
                }
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.setTreeItemIcons: error setting icons: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.setTreeItemIcons: interrupted while setting icons");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return Boolean.TRUE;
    }
    
    /**
     * scr.getTreeItemIcon(screenName, itemPath) -> String
     * Gets the current icon path for a tree item.
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath]
     * @return String icon path or null if no icon set
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenGetTreeItemIcon(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("scr.getTreeItemIcon: requires 2 parameters: screenName, itemPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemIcon: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemIcon: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.getTreeItemIcon: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        
        final java.util.concurrent.atomic.AtomicReference<String> iconPathRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Query on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemIcon: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemIcon: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemIcon: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Try to get icon path from storage
                TreeItemIconData iconData = treeItemIcons.get(item);
                if (iconData != null && iconData.iconPath != null) {
                    iconPathRef.set(iconData.iconPath);
                } else {
                    iconPathRef.set(null);
                }
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.getTreeItemIcon: error getting icon: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.getTreeItemIcon: interrupted while getting icon");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return iconPathRef.get();
    }
    
    /**
     * scr.setTreeItemBold(screenName, itemPath, bold) -> Boolean
     * Sets bold styling for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath, bold]
     * @return Boolean true on success
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenSetTreeItemBold(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("scr.setTreeItemBold: requires 3 parameters: screenName, itemPath, bold");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        Boolean bold = (Boolean) args[2];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemBold: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemBold: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.setTreeItemBold: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        final Boolean finalBold = bold;
        
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Update on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemBold: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemBold: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemBold: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get or create style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData == null) {
                    styleData = new TreeItemStyleData();
                    treeItemStyles.put(item, styleData);
                }
                
                // Set bold property
                styleData.bold = finalBold;
                
                // Force refresh of the tree view to apply styles
                treeView.refresh();
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.setTreeItemBold: error setting bold: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.setTreeItemBold: interrupted while setting bold");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return Boolean.TRUE;
    }
    
    /**
     * scr.setTreeItemItalic(screenName, itemPath, italic) -> Boolean
     * Sets italic styling for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath, italic]
     * @return Boolean true on success
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenSetTreeItemItalic(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("scr.setTreeItemItalic: requires 3 parameters: screenName, itemPath, italic");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        Boolean italic = (Boolean) args[2];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemItalic: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemItalic: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.setTreeItemItalic: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        final Boolean finalItalic = italic;
        
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Update on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemItalic: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemItalic: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemItalic: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get or create style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData == null) {
                    styleData = new TreeItemStyleData();
                    treeItemStyles.put(item, styleData);
                }
                
                // Set italic property
                styleData.italic = finalItalic;
                
                // Force refresh of the tree view to apply styles
                treeView.refresh();
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.setTreeItemItalic: error setting italic: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.setTreeItemItalic: interrupted while setting italic");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return Boolean.TRUE;
    }
    
    /**
     * scr.setTreeItemColor(screenName, itemPath, color) -> Boolean
     * Sets text color for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * Color can be specified as: hex string (#RRGGBB), rgb string (rgb(r,g,b)), or color name (red, blue, etc.)
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath, color]
     * @return Boolean true on success
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenSetTreeItemColor(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("scr.setTreeItemColor: requires 3 parameters: screenName, itemPath, color");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        String color = (String) args[2];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemColor: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.setTreeItemColor: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.setTreeItemColor: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        final String finalColor = color;
        
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Update on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemColor: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemColor: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.setTreeItemColor: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get or create style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData == null) {
                    styleData = new TreeItemStyleData();
                    treeItemStyles.put(item, styleData);
                }
                
                // Set color property
                styleData.color = finalColor;
                
                // Force refresh of the tree view to apply styles
                treeView.refresh();
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.setTreeItemColor: error setting color: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.setTreeItemColor: interrupted while setting color");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return Boolean.TRUE;
    }
    
    /**
     * scr.getTreeItemBold(screenName, itemPath) -> Boolean
     * Gets the bold styling state for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath]
     * @return Boolean true if bold is set, false if not set or null
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenGetTreeItemBold(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("scr.getTreeItemBold: requires 2 parameters: screenName, itemPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemBold: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemBold: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.getTreeItemBold: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        
        final java.util.concurrent.atomic.AtomicReference<Boolean> boldRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Query on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemBold: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemBold: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemBold: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData != null && styleData.bold != null) {
                    boldRef.set(styleData.bold);
                } else {
                    boldRef.set(false);
                }
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.getTreeItemBold: error getting bold: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.getTreeItemBold: interrupted while getting bold");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return boldRef.get();
    }
    
    /**
     * scr.getTreeItemItalic(screenName, itemPath) -> Boolean
     * Gets the italic styling state for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath]
     * @return Boolean true if italic is set, false if not set or null
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenGetTreeItemItalic(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("scr.getTreeItemItalic: requires 2 parameters: screenName, itemPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemItalic: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemItalic: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.getTreeItemItalic: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        
        final java.util.concurrent.atomic.AtomicReference<Boolean> italicRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Query on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemItalic: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemItalic: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemItalic: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData != null && styleData.italic != null) {
                    italicRef.set(styleData.italic);
                } else {
                    italicRef.set(false);
                }
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.getTreeItemItalic: error getting italic: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.getTreeItemItalic: interrupted while getting italic");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return italicRef.get();
    }
    
    /**
     * scr.getTreeItemColor(screenName, itemPath) -> String
     * Gets the text color for a tree item at the specified path.
     * The itemPath uses dot notation to specify the path through the tree (e.g., "Root.src.main").
     * 
     * @param context The interpreter context
     * @param args [screenName, itemPath]
     * @return String color value or null if no color set
     * @throws InterpreterError if parameters are invalid or tree item not found
     */
    public static Object screenGetTreeItemColor(InterpreterContext context, Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("scr.getTreeItemColor: requires 2 parameters: screenName, itemPath");
        }
        
        String screenName = (String) args[0];
        String itemPath = (String) args[1];
        
        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemColor: screenName parameter cannot be null or empty");
        }
        if (itemPath == null || itemPath.isEmpty()) {
            throw new InterpreterError("scr.getTreeItemColor: itemPath parameter cannot be null or empty");
        }
        
        // Normalize screen name
        screenName = screenName.toLowerCase();
        
        // Verify screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.getTreeItemColor: screen '" + screenName + "' not found");
        }
        
        final String finalScreenName = screenName;
        final String finalItemPath = itemPath;
        
        final java.util.concurrent.atomic.AtomicReference<String> colorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<InterpreterError> errorRef 
            = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        // Query on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Find the TreeView control for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(finalScreenName);
                if (controls == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemColor: no controls found for screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                javafx.scene.control.TreeView<String> treeView = null;
                for (javafx.scene.Node control : controls) {
                    if (control instanceof javafx.scene.control.TreeView) {
                        @SuppressWarnings("unchecked")
                        javafx.scene.control.TreeView<String> tv = (javafx.scene.control.TreeView<String>) control;
                        treeView = tv;
                        break;
                    }
                }
                
                if (treeView == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemColor: no TreeView found in screen '" + finalScreenName + "'"));
                    latch.countDown();
                    return;
                }
                
                // Find the tree item using the path
                javafx.scene.control.TreeItem<String> item = findTreeItemByPath(treeView.getRoot(), finalItemPath);
                if (item == null) {
                    errorRef.set(new InterpreterError("scr.getTreeItemColor: tree item '" + finalItemPath + "' not found"));
                    latch.countDown();
                    return;
                }
                
                // Get style data for this item
                TreeItemStyleData styleData = treeItemStyles.get(item);
                if (styleData != null && styleData.color != null) {
                    colorRef.set(styleData.color);
                } else {
                    colorRef.set(null);
                }
                
            } catch (Exception e) {
                errorRef.set(new InterpreterError("scr.getTreeItemColor: error getting color: " + e.getMessage()));
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for completion
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("scr.getTreeItemColor: interrupted while getting color");
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return colorRef.get();
    }
    
    /**
     * Helper method to find a tree item by its path (dot-separated).
     * Supports both simple paths (matching value) and hierarchical paths.
     * 
     * @param root The root tree item to search from
     * @param path The path to the item (e.g., "Root.src.main.java")
     * @return The found TreeItem or null if not found
     */
    private static javafx.scene.control.TreeItem<String> findTreeItemByPath(
            javafx.scene.control.TreeItem<String> root, String path) {
        if (root == null || path == null || path.isEmpty()) {
            return null;
        }
        
        // Split path into components
        String[] pathParts = path.split("\\.");
        
        // Start from root
        javafx.scene.control.TreeItem<String> current = root;
        
        // Traverse the path step by step
        int partIndex = 0;
        
        // Check if root matches the first part of the path (handle null root value)
        String rootValue = current.getValue();
        if (rootValue != null && rootValue.equals(pathParts[0])) {
            partIndex = 1; // Start searching from the next part
        } else if (rootValue == null && pathParts.length == 1) {
            // If root value is null and we're looking for a single-part path, it can't match
            return null;
        }
        
        // For each remaining part of the path, find the matching child
        while (partIndex < pathParts.length) {
            String part = pathParts[partIndex];
            boolean found = false;
            
            // Search children for matching value
            for (javafx.scene.control.TreeItem<String> child : current.getChildren()) {
                if (child.getValue() != null && child.getValue().equals(part)) {
                    current = child;
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return null; // Path component not found
            }
            
            partIndex++;
        }
        
        return current;
    }
    
    /**
     * Helper method to set an icon on a tree item.
     * Loads the icon from resources and sets it as the graphic.
     * 
     * @param item The tree item to set the icon on
     * @param iconPath The path to the icon image (can be null)
     */
    private static void setTreeItemIconGraphic(javafx.scene.control.TreeItem<String> item, String iconPath) {
        if (iconPath == null || iconPath.isEmpty()) {
            item.setGraphic(null);
            return;
        }
        
        try {
            javafx.scene.image.Image image = null;
            
            // Try loading from classpath using ClassLoader
            String resourcePath = iconPath.startsWith("/") ? iconPath.substring(1) : iconPath;
            
            // Try first method
            try (java.io.InputStream is = BuiltinsScreen.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    image = new javafx.scene.image.Image(is);
                }
            }
            
            // If not found, try with leading slash
            if (image == null) {
                try (java.io.InputStream is = BuiltinsScreen.class.getResourceAsStream("/" + resourcePath)) {
                    if (is != null) {
                        image = new javafx.scene.image.Image(is);
                    }
                }
            }
            
            // If still not found, try as file path
            if (image == null) {
                java.io.File file = new java.io.File(iconPath);
                if (file.exists() && file.isFile()) {
                    image = new javafx.scene.image.Image(file.toURI().toString());
                }
            }
            
            if (image != null) {
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                imageView.setFitWidth(16);
                imageView.setFitHeight(16);
                imageView.setPreserveRatio(true);
                item.setGraphic(imageView);
            } else {
                item.setGraphic(null);
            }
        } catch (Exception e) {
            System.err.println("Error loading tree icon '" + iconPath + "': " + e.getMessage());
            item.setGraphic(null);
        }
    }
    
    /**
     * Helper method to get the style CSS string for a tree item.
     * This is used by the TreeView cell factory to apply styles.
     * 
     * @param item The tree item to get style for
     * @return CSS style string or null if no style set
     */
    public static String getTreeItemStyle(javafx.scene.control.TreeItem<String> item) {
        TreeItemStyleData styleData = treeItemStyles.get(item);
        if (styleData == null) {
            return null;
        }
        
        StringBuilder style = new StringBuilder();
        
        if (styleData.bold != null && styleData.bold) {
            style.append("-fx-font-weight: bold; ");
        }
        
        if (styleData.italic != null && styleData.italic) {
            style.append("-fx-font-style: italic; ");
        }
        
        if (styleData.color != null && !styleData.color.isEmpty()) {
            // Accept any color format: hex (#RRGGBB), rgb/rgba, or color names
            style.append("-fx-text-fill: ").append(styleData.color).append("; ");
        }
        
        return style.length() > 0 ? style.toString() : null;
    }
}
