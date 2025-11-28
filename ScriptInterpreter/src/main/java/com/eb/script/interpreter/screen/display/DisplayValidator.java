package com.eb.script.interpreter.screen.display;

import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.screen.ScreenFactory.OnClickHandler;
import javafx.scene.Node;

/**
 * Handles UI-level validation for screen controls.
 * Manages onValidate event handlers and applies validation styling.
 * 
 * This class is part of the DISPLAY layer and handles JavaFX-specific
 * validation logic including error styling and validation listeners.
 * 
 * @author Earl Bosch
 */
public class DisplayValidator {

    /** Default error style for validation failures */
    public static final String ERROR_STYLE = "-fx-border-color: red; -fx-border-width: 2;";

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
     */
    public static void setupValidationHandler(Node control, String validateCode,
            OnClickHandler onClickHandler, String screenName, InterpreterContext context) {
        if (control == null || validateCode == null || validateCode.isEmpty() || onClickHandler == null) {
            return;
        }
        
        // Create a validation runner that executes the code and applies styling
        Runnable validator = () -> {
            try {
                // Execute the validation code
                Object result = onClickHandler.executeWithReturn(validateCode);
                
                // Check if result is a boolean and apply styling
                boolean isValid = true;
                if (result instanceof Boolean) {
                    isValid = (Boolean) result;
                }
                
                // Apply or remove error styling based on validation result
                applyValidationStyling(control, isValid);
            } catch (Exception e) {
                System.err.println("Error executing validation code: " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        // Attach validator to appropriate control events
        attachValidationListener(control, validator);
    }
    
    /**
     * Applies or removes validation error styling on a control.
     * 
     * @param control The JavaFX control
     * @param isValid Whether the control value is valid
     */
    public static void applyValidationStyling(Node control, boolean isValid) {
        if (!isValid) {
            // Mark control with error style
            String currentStyle = control.getStyle();
            if (currentStyle == null) {
                currentStyle = "";
            }
            if (!currentStyle.contains("-fx-border-color: red")) {
                control.setStyle(currentStyle + " " + ERROR_STYLE);
            }
        } else {
            // Remove error styling by removing red border properties
            String currentStyle = control.getStyle();
            if (currentStyle != null) {
                // Remove error border styles
                currentStyle = currentStyle.replaceAll("-fx-border-color:\\s*red;?", "");
                currentStyle = currentStyle.replaceAll("-fx-border-width:\\s*2;?", "");
                control.setStyle(currentStyle.trim());
            }
        }
    }
    
    /**
     * Clears all validation error styling from a control.
     * 
     * @param control The JavaFX control
     */
    public static void clearValidationStyling(Node control) {
        applyValidationStyling(control, true);
    }
    
    /**
     * Checks if a control has validation error styling.
     * 
     * @param control The JavaFX control
     * @return true if the control has error styling
     */
    public static boolean hasValidationError(Node control) {
        if (control == null) {
            return false;
        }
        String style = control.getStyle();
        return style != null && style.contains("-fx-border-color: red");
    }
    
    /**
     * Attaches a validation listener to a control based on its type.
     * The validator is called whenever the control's value changes.
     * 
     * @param control The JavaFX control
     * @param validator The validation runnable to execute
     */
    public static void attachValidationListener(Node control, Runnable validator) {
        // Delegate to shared utility method in ControlListenerFactory
        ControlListenerFactory.attachValueChangeListener(control, validator);
    }
}
