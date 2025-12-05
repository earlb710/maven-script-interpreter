package com.eb.script.interpreter.screen.display;

import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.screen.ScreenFactory;
import com.eb.script.interpreter.screen.ScreenFactory.OnClickHandler;
import com.eb.script.interpreter.screen.data.DataBindingManager;
import javafx.scene.Node;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles onChange event handlers for screen input controls.
 * Executes inline EBS code whenever the control's value changes.
 * 
 * This class is part of the DISPLAY layer and handles JavaFX-specific
 * change event logic.
 * 
 * @author Earl Bosch
 */
public class DisplayChangeHandler {

    /**
     * Sets up an onChange event handler for a control.
     * The change code is executed whenever the control value changes.
     * 
     * @param control The JavaFX control to monitor for changes
     * @param changeCode The EBS code to execute on change
     * @param onClickHandler Handler to execute the EBS code
     * @param screenName The screen name for context
     * @param context The interpreter context
     * @param boundControls List of bound controls to refresh after execution
     * @param screenVars The screen variables map
     */
    public static void setupChangeHandler(Node control, String changeCode,
            OnClickHandler onClickHandler, String screenName, InterpreterContext context,
            List<Node> boundControls, ConcurrentHashMap<String, Object> screenVars) {
        if (control == null || changeCode == null || changeCode.isEmpty() || onClickHandler == null) {
            return;
        }
        
        // Get item name from control properties (set during control creation)
        String itemName = (String) control.getProperties().get("itemName");
        if (itemName == null) {
            itemName = control.getId() != null ? control.getId() : "unknown";
        }
        
        final String finalItemName = itemName;
        
        // Create a change handler that executes the code
        Runnable changeHandler = () -> {
            try {
                // Increment event count for debugging
                int count = ScreenFactory.incrementEventCount(screenName, finalItemName, "onChange");
                System.out.println("[DEBUG] onChange fired: " + screenName + "." + finalItemName + " (count: " + count + ")");
                
                // Execute the onChange code
                System.out.println("[DEBUG onChange] Executing code: " + changeCode.substring(0, Math.min(100, changeCode.length())) + "...");
                onClickHandler.execute(changeCode);
                System.out.println("[DEBUG onChange] Code executed successfully");
                
                // After executing the onChange code, refresh all bound controls to update UI
                if (boundControls != null && screenVars != null) {
                    System.out.println("[DEBUG onChange] Refreshing " + boundControls.size() + " bound controls");
                    DataBindingManager.refreshBoundControls(boundControls, screenVars);
                }
            } catch (Exception e) {
                System.out.println("[DEBUG onChange] ERROR executing onChange code: " + e.getMessage());
                System.err.println("Error executing onChange code: " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        // Attach change handler to appropriate control events using shared utility
        ControlListenerFactory.attachValueChangeListener(control, changeHandler);
    }
}
