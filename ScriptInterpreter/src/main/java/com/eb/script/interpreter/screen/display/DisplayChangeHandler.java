package com.eb.script.interpreter.screen.display;

import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.screen.ScreenFactory.OnClickHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

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
     */
    public static void setupChangeHandler(Node control, String changeCode,
            OnClickHandler onClickHandler, String screenName, InterpreterContext context) {
        if (control == null || changeCode == null || changeCode.isEmpty() || onClickHandler == null) {
            return;
        }
        
        // Create a change handler that executes the code
        Runnable changeHandler = () -> {
            try {
                // Execute the onChange code
                onClickHandler.execute(changeCode);
            } catch (Exception e) {
                System.err.println("Error executing onChange code: " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        // Attach change handler to appropriate control events
        attachChangeListener(control, changeHandler);
    }
    
    /**
     * Attaches a change listener to a control based on its type.
     * The handler is called whenever the control's value changes.
     * 
     * @param control The JavaFX control
     * @param handler The runnable to execute on change
     */
    public static void attachChangeListener(Node control, Runnable handler) {
        // Handle HBox containing slider (when showSliderValue is true)
        if (control instanceof HBox) {
            HBox hbox = (HBox) control;
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Slider) {
                Slider slider = (Slider) hbox.getChildren().get(0);
                slider.valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
                return;
            }
        }
        
        if (control instanceof TextField) {
            ((TextField) control).textProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof TextArea) {
            ((TextArea) control).textProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof PasswordField) {
            ((PasswordField) control).textProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof ComboBox) {
            ((ComboBox<?>) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof ChoiceBox) {
            ((ChoiceBox<?>) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof CheckBox) {
            ((CheckBox) control).selectedProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof RadioButton) {
            ((RadioButton) control).selectedProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof ToggleButton) {
            ((ToggleButton) control).selectedProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof Spinner) {
            ((Spinner<?>) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof Slider) {
            ((Slider) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof DatePicker) {
            ((DatePicker) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        } else if (control instanceof ColorPicker) {
            ((ColorPicker) control).valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
        }
    }
}
