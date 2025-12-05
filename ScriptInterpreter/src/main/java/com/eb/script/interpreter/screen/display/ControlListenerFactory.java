package com.eb.script.interpreter.screen.display;

import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.data.VarRefResolver;
import com.eb.script.token.DataType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and attaching listeners to JavaFX controls.
 * Handles two-way data binding between UI controls and screen variables.
 * 
 * This class is part of the DISPLAY layer but delegates data operations
 * to the VarRefResolver in the DATA layer.
 * 
 * @author Earl Bosch
 */
public class ControlListenerFactory {

    /**
     * Sets up two-way variable binding for a control.
     * 
     * @param control The JavaFX control to bind
     * @param varName The variable name to bind to
     * @param screenVars The screen variables map
     * @param varTypes The variable types map
     * @param metadata Display metadata for the control
     */
    public static void setupVariableBinding(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars,
            ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        if (control == null || varName == null || screenVars == null) {
            return;
        }

        // Initialize control with current variable value
        // Handle complex varRef expressions like "clients[0].clientName"
        Object currentValue = VarRefResolver.resolveVarRefValue(varName, screenVars);
        ControlUpdater.updateControlFromValue(control, currentValue, metadata);

        // Set up listener on the control to update the variable when control changes
        addControlListener(control, varName, screenVars, varTypes, metadata);

        // Store references for potential future use
        control.getProperties().put("varName", varName);
        control.getProperties().put("screenVars", screenVars);
        control.getProperties().put("varTypes", varTypes);
        control.getProperties().put("metadata", metadata);
    }

    /**
     * Adds a listener to a control to update the corresponding screen variable
     * when the control's value changes.
     * 
     * @param control The JavaFX control
     * @param varName The variable name to update
     * @param screenVars The screen variables map
     * @param varTypes The variable types map
     * @param metadata Display metadata for the control
     */
    public static void addControlListener(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars,
            ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        // Handle HBox containing slider (when showSliderValue is true)
        if (control instanceof HBox) {
            HBox hbox = (HBox) control;
            // Check if this HBox contains a Slider as its first child
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Slider) {
                Slider slider = (Slider) hbox.getChildren().get(0);
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    VarRefResolver.setVarRefValue(varName, newVal.intValue(), screenVars);
                });
                return;
            }
        }
        
        if (control instanceof TextField) {
            addTextFieldListener((TextField) control, varName, screenVars, varTypes, metadata);
        } else if (control instanceof TextArea) {
            addTextAreaListener((TextArea) control, varName, screenVars, metadata);
        } else if (control instanceof CheckBox) {
            ((CheckBox) control).selectedProperty().addListener((obs, oldVal, newVal) -> {
                VarRefResolver.setVarRefValue(varName, newVal, screenVars);
            });
        } else if (control instanceof Slider) {
            ((Slider) control).valueProperty().addListener((obs, oldVal, newVal) -> {
                VarRefResolver.setVarRefValue(varName, newVal.intValue(), screenVars);
            });
        } else if (control instanceof Spinner) {
            addSpinnerListener(control, varName, screenVars);
        } else if (control instanceof ComboBox) {
            addComboBoxListener(control, varName, screenVars);
        } else if (control instanceof ChoiceBox) {
            addChoiceBoxListener(control, varName, screenVars);
        } else if (control instanceof ColorPicker) {
            addColorPickerListener((ColorPicker) control, varName, screenVars);
        } else if (control instanceof DatePicker) {
            ((DatePicker) control).valueProperty().addListener((obs, oldVal, newVal) -> {
                VarRefResolver.setVarRefValue(varName, newVal, screenVars);
            });
        } else if (control instanceof TreeView) {
            @SuppressWarnings("unchecked")
            TreeView<String> treeView = (TreeView<String>) control;
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    VarRefResolver.setVarRefValue(varName, newVal.getValue(), screenVars);
                }
            });
        }
    }
    
    /**
     * Adds a listener to a TextField with case transformation support.
     */
    private static void addTextFieldListener(TextField textField, String varName,
            ConcurrentHashMap<String, Object> screenVars,
            ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Apply case transformation if specified
            String transformedValue = newVal;
            if (metadata != null && metadata.caseFormat != null) {
                if ("upper".equals(metadata.caseFormat)) {
                    transformedValue = newVal.toUpperCase();
                } else if ("lower".equals(metadata.caseFormat)) {
                    transformedValue = newVal.toLowerCase();
                }
                // "mixed" or any other value means no transformation
                
                // Update the text field if transformation occurred
                if (!transformedValue.equals(newVal)) {
                    // Save cursor position
                    int caretPosition = textField.getCaretPosition();
                    textField.setText(transformedValue);
                    // Restore cursor position (adjust if text changed length)
                    int newCaretPosition = Math.min(caretPosition, transformedValue.length());
                    textField.positionCaret(newCaretPosition);
                    return; // Listener will be called again with transformed value
                }
            }
            
            // Convert the string value to the appropriate type if type info is available
            Object convertedValue = transformedValue;
            if (varTypes != null && varTypes.containsKey(varName)) {
                DataType type = varTypes.get(varName);
                try {
                    convertedValue = type.convertValue(transformedValue);
                } catch (Exception e) {
                    // If conversion fails, keep as string
                    System.err.println("Warning: Could not convert '" + transformedValue + "' to " + type + " for variable '" + varName + "'");
                }
            }
            VarRefResolver.setVarRefValue(varName, convertedValue, screenVars);
        });
    }
    
    /**
     * Adds a listener to a TextArea with case transformation support.
     */
    private static void addTextAreaListener(TextArea textArea, String varName,
            ConcurrentHashMap<String, Object> screenVars,
            DisplayItem metadata) {
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            // Apply case transformation if specified
            String transformedValue = newVal;
            if (metadata != null && metadata.caseFormat != null) {
                if ("upper".equals(metadata.caseFormat)) {
                    transformedValue = newVal.toUpperCase();
                } else if ("lower".equals(metadata.caseFormat)) {
                    transformedValue = newVal.toLowerCase();
                }
                // "mixed" or any other value means no transformation
                
                // Update the text area if transformation occurred
                if (!transformedValue.equals(newVal)) {
                    // Save cursor position
                    int caretPosition = textArea.getCaretPosition();
                    textArea.setText(transformedValue);
                    // Restore cursor position (adjust if text changed length)
                    int newCaretPosition = Math.min(caretPosition, transformedValue.length());
                    textArea.positionCaret(newCaretPosition);
                    return; // Listener will be called again with transformed value
                }
            }
            
            VarRefResolver.setVarRefValue(varName, transformedValue, screenVars);
        });
    }
    
    /**
     * Adds a listener to a Spinner control.
     */
    private static void addSpinnerListener(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars) {
        @SuppressWarnings("unchecked")
        Spinner<Integer> spinner = (Spinner<Integer>) control;
        // Check if ValueFactory exists before adding listener
        if (spinner.getValueFactory() != null) {
            spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    VarRefResolver.setVarRefValue(varName, newVal, screenVars);
                }
            });
        }
    }
    
    /**
     * Adds a listener to a ComboBox control.
     * Supports optionsMap for mapping display text (values) to data values (keys).
     */
    private static void addComboBoxListener(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars) {
        @SuppressWarnings("unchecked")
        ComboBox<String> comboBox = (ComboBox<String>) control;
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Object valueToStore = newVal;
            
            // Check if optionsMap is present to get the data value (key)
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> optionsMap = 
                (java.util.Map<String, String>) comboBox.getProperties().get("optionsMap");
            if (optionsMap != null && newVal != null) {
                // Find the key (data value) for this display text (value)
                for (java.util.Map.Entry<String, String> entry : optionsMap.entrySet()) {
                    if (entry.getValue().equals(newVal)) {
                        valueToStore = entry.getKey();
                        break;
                    }
                }
            }
            
            VarRefResolver.setVarRefValue(varName, valueToStore, screenVars);
        });
    }
    
    /**
     * Adds a listener to a ChoiceBox control.
     * Supports optionsMap for mapping display text (values) to data values (keys).
     */
    private static void addChoiceBoxListener(Node control, String varName,
            ConcurrentHashMap<String, Object> screenVars) {
        @SuppressWarnings("unchecked")
        ChoiceBox<String> choiceBox = (ChoiceBox<String>) control;
        choiceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Object valueToStore = newVal;
            
            // Check if optionsMap is present to get the data value (key)
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> optionsMap = 
                (java.util.Map<String, String>) choiceBox.getProperties().get("optionsMap");
            if (optionsMap != null && newVal != null) {
                // Find the key (data value) for this display text (value)
                for (java.util.Map.Entry<String, String> entry : optionsMap.entrySet()) {
                    if (entry.getValue().equals(newVal)) {
                        valueToStore = entry.getKey();
                        break;
                    }
                }
            }
            
            VarRefResolver.setVarRefValue(varName, valueToStore, screenVars);
        });
    }
    
    /**
     * Adds a listener to a ColorPicker control.
     */
    private static void addColorPickerListener(ColorPicker colorPicker, String varName,
            ConcurrentHashMap<String, Object> screenVars) {
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Convert Color to web string format (e.g., "#ff0000")
            if (newVal != null) {
                String colorString = String.format("#%02x%02x%02x",
                    (int) (newVal.getRed() * 255),
                    (int) (newVal.getGreen() * 255),
                    (int) (newVal.getBlue() * 255));
                VarRefResolver.setVarRefValue(varName, colorString, screenVars);
            } else {
                // ColorPicker set to null - remove the variable to indicate null value
                VarRefResolver.setVarRefValue(varName, "", screenVars);
            }
        });
    }
    
    /**
     * Attaches a generic change listener to a control based on its type.
     * The handler is called whenever the control's value changes.
     * This is a utility method for use by validation and change handlers.
     * 
     * @param control The JavaFX control
     * @param handler The runnable to execute on change
     */
    public static void attachValueChangeListener(Node control, Runnable handler) {
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
            ComboBox<?> comboBox = (ComboBox<?>) control;
            if (comboBox.isEditable()) {
                // For editable ComboBox, we need to listen to the editor's text property
                // The editor is lazily created, so we need to handle when it's not yet available
                if (comboBox.getEditor() != null) {
                    // Editor already exists - attach directly
                    comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> handler.run());
                } else {
                    // Editor not yet created - wait for it and attach when available
                    comboBox.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                        if (newSkin != null && comboBox.getEditor() != null) {
                            comboBox.getEditor().textProperty().addListener((textObs, oldText, newText) -> handler.run());
                        }
                    });
                }
            } else {
                // For non-editable ComboBox, listen to value selection changes
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> handler.run());
            }
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
        } else if (control instanceof TreeView) {
            @SuppressWarnings("unchecked")
            TreeView<?> treeView = (TreeView<?>) control;
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> handler.run());
        }
    }
}
