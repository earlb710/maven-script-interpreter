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
                ScreenFactory.incrementEventCount(screenName, finalItemName, "onChange");
                
                // Execute the onChange code
                onClickHandler.execute(changeCode);
                
                // After executing the onChange code, refresh all bound controls to update UI
                if (boundControls != null && screenVars != null) {
                    DataBindingManager.refreshBoundControls(boundControls, screenVars);
                }
            } catch (Exception e) {
                System.err.println("Error executing onChange code: " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        // Attach change handler to appropriate control events using shared utility
        ControlListenerFactory.attachValueChangeListener(control, changeHandler);
    }
}
