package com.eb.script.interpreter.screen.data;

import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.display.ControlListenerFactory;
import com.eb.script.interpreter.screen.display.ControlUpdater;
import com.eb.script.token.DataType;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages two-way data binding between screen variables and UI controls.
 * Coordinates binding setup, state tracking, and bulk refresh operations.
 * 
 * This class is the central coordinator for data binding operations,
 * delegating UI-specific operations to the display layer.
 * 
 * @author Earl Bosch
 */
public class DataBindingManager {

    /** Map of screen names to their bound controls */
    private static final ConcurrentHashMap<String, List<Node>> screenBoundControls = new ConcurrentHashMap<>();
    
    /** Map of screen names to their variable maps */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> screenVariables = new ConcurrentHashMap<>();
    
    /** Map of screen names to their variable type maps */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, DataType>> screenVarTypes = new ConcurrentHashMap<>();

    /**
     * Registers a screen's binding context.
     * 
     * @param screenName The screen name
     * @param screenVars The screen variables map
     * @param varTypes The variable types map
     */
    public static void registerScreen(String screenName, 
            ConcurrentHashMap<String, Object> screenVars,
            ConcurrentHashMap<String, DataType> varTypes) {
        if (screenName == null) {
            return;
        }
        if (screenVars != null) {
            screenVariables.put(screenName, screenVars);
        }
        if (varTypes != null) {
            screenVarTypes.put(screenName, varTypes);
        }
        // Initialize empty bound controls list
        screenBoundControls.putIfAbsent(screenName, new ArrayList<>());
    }
    
    /**
     * Unregisters a screen and cleans up its binding data.
     * 
     * @param screenName The screen name to unregister
     */
    public static void unregisterScreen(String screenName) {
        if (screenName == null) {
            return;
        }
        screenBoundControls.remove(screenName);
        screenVariables.remove(screenName);
        screenVarTypes.remove(screenName);
    }

    /**
     * Sets up two-way data binding between a control and a variable.
     * 
     * @param control The JavaFX control to bind
     * @param varName The variable name to bind to
     * @param screenVars The screen variables map
     * @param varTypes The variable types map
     * @param metadata Display metadata for the control
     */
    public static void setupBinding(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars,
            ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        if (control == null || varName == null || screenVars == null) {
            return;
        }
        
        // Delegate to ControlListenerFactory for the actual binding
        ControlListenerFactory.setupVariableBinding(control, varName, screenVars, varTypes, metadata);
    }
    
    /**
     * Sets up binding and tracks the control for a specific screen.
     * 
     * @param screenName The screen name
     * @param control The JavaFX control to bind
     * @param varName The variable name to bind to
     * @param metadata Display metadata for the control
     */
    public static void setupBindingForScreen(String screenName, Node control, String varName,
            DisplayItem metadata) {
        if (screenName == null || control == null || varName == null) {
            return;
        }
        
        ConcurrentHashMap<String, Object> screenVars = screenVariables.get(screenName);
        ConcurrentHashMap<String, DataType> varTypes = screenVarTypes.get(screenName);
        
        if (screenVars == null) {
            return;
        }
        
        // Set up the binding
        setupBinding(control, varName, screenVars, varTypes, metadata);
        
        // Track the bound control
        trackBoundControl(screenName, control);
    }
    
    /**
     * Tracks a bound control for a screen.
     * 
     * @param screenName The screen name
     * @param control The bound control to track
     */
    public static void trackBoundControl(String screenName, Node control) {
        if (screenName == null || control == null) {
            return;
        }
        
        List<Node> boundControls = screenBoundControls.computeIfAbsent(screenName, k -> new ArrayList<>());
        if (!boundControls.contains(control)) {
            boundControls.add(control);
        }
    }
    
    /**
     * Tracks multiple bound controls for a screen.
     * 
     * @param screenName The screen name
     * @param controls The bound controls to track
     */
    public static void trackBoundControls(String screenName, List<Node> controls) {
        if (screenName == null || controls == null) {
            return;
        }
        
        for (Node control : controls) {
            trackBoundControl(screenName, control);
        }
    }
    
    /**
     * Gets the list of bound controls for a screen.
     * 
     * @param screenName The screen name
     * @return List of bound controls, or empty list if none
     */
    public static List<Node> getBoundControls(String screenName) {
        if (screenName == null) {
            return new ArrayList<>();
        }
        return screenBoundControls.getOrDefault(screenName, new ArrayList<>());
    }
    
    /**
     * Gets the screen variables map for a screen.
     * 
     * @param screenName The screen name
     * @return The screen variables map, or null if not found
     */
    public static ConcurrentHashMap<String, Object> getScreenVars(String screenName) {
        return screenVariables.get(screenName);
    }
    
    /**
     * Gets the variable types map for a screen.
     * 
     * @param screenName The screen name
     * @return The variable types map, or null if not found
     */
    public static ConcurrentHashMap<String, DataType> getVarTypes(String screenName) {
        return screenVarTypes.get(screenName);
    }

    /**
     * Refreshes all bound controls for a screen, updating their values
     * from the current variable values.
     * 
     * @param screenName The screen name
     */
    public static void refreshScreen(String screenName) {
        if (screenName == null) {
            return;
        }
        
        List<Node> boundControls = screenBoundControls.get(screenName);
        ConcurrentHashMap<String, Object> screenVars = screenVariables.get(screenName);
        
        if (boundControls != null && screenVars != null) {
            ControlUpdater.refreshBoundControls(boundControls, screenVars);
        }
    }
    
    /**
     * Refreshes a specific list of bound controls with their current variable values.
     * 
     * @param boundControls The controls to refresh
     * @param screenVars The screen variables map
     */
    public static void refreshBoundControls(List<Node> boundControls,
            ConcurrentHashMap<String, Object> screenVars) {
        if (boundControls == null || screenVars == null) {
            return;
        }
        ControlUpdater.refreshBoundControls(boundControls, screenVars);
    }
    
    /**
     * Gets a variable value from the screen variables.
     * 
     * @param screenName The screen name
     * @param varName The variable name
     * @return The variable value, or null if not found
     */
    public static Object getVariableValue(String screenName, String varName) {
        ConcurrentHashMap<String, Object> screenVars = screenVariables.get(screenName);
        if (screenVars == null || varName == null) {
            return null;
        }
        return VarRefResolver.resolveVarRefValue(varName, screenVars);
    }
    
    /**
     * Sets a variable value in the screen variables.
     * 
     * @param screenName The screen name
     * @param varName The variable name
     * @param value The value to set
     */
    public static void setVariableValue(String screenName, String varName, Object value) {
        ConcurrentHashMap<String, Object> screenVars = screenVariables.get(screenName);
        if (screenVars == null || varName == null) {
            return;
        }
        VarRefResolver.setVarRefValue(varName, value, screenVars);
    }
    
    /**
     * Sets a variable value and refreshes all bound controls for the screen.
     * 
     * @param screenName The screen name
     * @param varName The variable name
     * @param value The value to set
     */
    public static void setVariableValueAndRefresh(String screenName, String varName, Object value) {
        setVariableValue(screenName, varName, value);
        refreshScreen(screenName);
    }
    
    /**
     * Checks if a screen has any bound controls.
     * 
     * @param screenName The screen name
     * @return true if the screen has bound controls
     */
    public static boolean hasBindings(String screenName) {
        if (screenName == null) {
            return false;
        }
        List<Node> boundControls = screenBoundControls.get(screenName);
        return boundControls != null && !boundControls.isEmpty();
    }
    
    /**
     * Gets the count of bound controls for a screen.
     * 
     * @param screenName The screen name
     * @return The number of bound controls
     */
    public static int getBindingCount(String screenName) {
        if (screenName == null) {
            return 0;
        }
        List<Node> boundControls = screenBoundControls.get(screenName);
        return boundControls != null ? boundControls.size() : 0;
    }
    
    /**
     * Clears all binding data for all screens.
     * Use with caution - typically only for testing or cleanup.
     */
    public static void clearAll() {
        screenBoundControls.clear();
        screenVariables.clear();
        screenVarTypes.clear();
    }
}
