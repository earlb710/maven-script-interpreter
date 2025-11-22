package com.eb.script.interpreter;

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
     * scr.showWindow() -> BOOL Shows the current screen window.
     * This is a convenience method that always uses the current screen context.
     * Must be called from within a screen context (e.g., onClick handler).
     * Returns true on success.
     */
    public static Object screenShowWindow(InterpreterContext context, Object[] args) throws InterpreterError {
        // Always use current screen context - no screen name parameter
        return screenShow(context, new Object[0]);
    }

    /**
     * scr.hideWindow() -> BOOL Hides the current screen window.
     * This is a convenience method that always uses the current screen context.
     * Must be called from within a screen context (e.g., onClick handler).
     * Returns true on success.
     */
    public static Object screenHideWindow(InterpreterContext context, Object[] args) throws InterpreterError {
        // Always use current screen context - no screen name parameter
        return screenHide(context, new Object[0]);
    }

    /**
     * scr.closeWindow() -> BOOL Closes the current screen window.
     * This is a convenience method that always uses the current screen context.
     * Must be called from within a screen context (e.g., onClick handler).
     * Returns true on success.
     */
    public static Object screenCloseWindow(InterpreterContext context, Object[] args) throws InterpreterError {
        // Always use current screen context - no screen name parameter
        return screenClose(context, new Object[0]);
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

        String screenName = parts[0];
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
        final String screenNameFinal = screenName;
        final String itemNameFinal = itemName;
        final com.eb.script.interpreter.screen.AreaItem finalItem = targetItem;
        final String finalPropertyName = propertyName;
        final Object finalValue = value;

        javafx.application.Platform.runLater(() -> {
            try {
                // Get the list of bound controls for this screen
                java.util.List<javafx.scene.Node> controls = context.getScreenBoundControls().get(screenNameFinal);
                if (controls != null) {
                    // Find the control with matching user data
                    String targetUserData = screenNameFinal + "." + itemNameFinal;
                    for (javafx.scene.Node control : controls) {
                        Object userData = control.getUserData();
                        if (targetUserData.equals(userData)) {
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

        String screenName = parts[0];
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
     * scr.getItemSource(screenName, itemName) -> String Gets the source
     * property of a screen item: "data" or "display"
     */
    public static Object screenGetItemSource(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.getItemSource: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.getItemSource: itemName parameter cannot be null or empty");
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.getItemSource: screen '" + screenName + "' not found");
        }

        // Get the var item
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null) {
            throw new InterpreterError("scr.getItemSource: no variables defined for screen '" + screenName + "'");
        }

        // Find the variable - try with various key formats
        Var var = null;
        String lowerItemName = itemName.toLowerCase();

        // Try direct lookup
        for (Map.Entry<String, Var> entry : varItems.entrySet()) {
            String key = entry.getKey();
            Var v = entry.getValue();
            // Match by key or by variable name
            if (key.equals(lowerItemName) || key.endsWith("." + lowerItemName)
                    || (v.getName() != null && v.getName().equalsIgnoreCase(itemName))) {
                var = v;
                break;
            }
        }

        if (var == null) {
            throw new InterpreterError("scr.getItemSource: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get the display item
        DisplayItem displayItem = var.getDisplayItem();
        if (displayItem == null) {
            return "data"; // Default if no display item
        }

        return displayItem.source != null ? displayItem.source : "data";
    }

    /**
     * scr.setItemSource(screenName, itemName, source) -> Boolean Sets the
     * source property of a screen item: "data" or "display"
     */
    public static Object screenSetItemSource(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (String) args[0];
        String itemName = (String) args[1];
        String source = (String) args[2];

        if (screenName == null || screenName.isEmpty()) {
            throw new InterpreterError("scr.setItemSource: screenName parameter cannot be null or empty");
        }
        if (itemName == null || itemName.isEmpty()) {
            throw new InterpreterError("scr.setItemSource: itemName parameter cannot be null or empty");
        }
        if (source == null || source.isEmpty()) {
            throw new InterpreterError("scr.setItemSource: source parameter cannot be null or empty");
        }

        // Validate source value
        String lowerSource = source.toLowerCase();
        if (!lowerSource.equals("data") && !lowerSource.equals("display")) {
            throw new InterpreterError("scr.setItemSource: source must be 'data' or 'display', got: " + source);
        }

        // Verify screen exists
        if (!context.getScreens().containsKey(screenName.toLowerCase())) {
            throw new InterpreterError("scr.setItemSource: screen '" + screenName + "' not found");
        }

        // Get the var item
        Map<String, Var> varItems = context.getScreenVarItems(screenName);
        if (varItems == null) {
            throw new InterpreterError("scr.setItemSource: no variables defined for screen '" + screenName + "'");
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
            throw new InterpreterError("scr.setItemSource: item '" + itemName + "' not found in screen '" + screenName + "'");
        }

        // Get or create the display item
        DisplayItem displayItem = var.getDisplayItem();
        if (displayItem == null) {
            // Create a basic display item if it doesn't exist
            displayItem = new DisplayItem();
            var.setDisplayItem(displayItem);
        }

        // Set the source
        displayItem.source = lowerSource;

        return true;
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

        // Return the status based on comparison of current value to original value
        return var.getStatus();
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
            if (screenVars != null && var.getName() != null) {
                screenVars.put(var.getName().toLowerCase(), originalValue);
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
            if (screenVars != null && var.getName() != null) {
                screenVars.put(var.getName().toLowerCase(), defaultValue);
            }
        }

        return true;
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
}
