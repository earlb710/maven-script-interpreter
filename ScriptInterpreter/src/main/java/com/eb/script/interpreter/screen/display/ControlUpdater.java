package com.eb.script.interpreter.screen.display;

import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsImage;
import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.data.VarRefResolver;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Handles updating JavaFX controls with values from the data layer.
 * 
 * This class is part of the DISPLAY layer and handles the UI-specific
 * logic for setting control values.
 * 
 * @author Earl Bosch
 */
public class ControlUpdater {

    /**
     * Updates a control's value based on the variable value.
     * 
     * @param control The JavaFX control to update
     * @param value The value to set
     * @param metadata Display metadata for the control
     */
    public static void updateControlFromValue(Node control, Object value, DisplayItem metadata) {
        // Handle HBox containing slider (when showSliderValue is true)
        if (control instanceof HBox) {
            HBox hbox = (HBox) control;
            // Check if this HBox contains a Slider as its first child
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Slider) {
                Slider slider = (Slider) hbox.getChildren().get(0);
                if (value instanceof Number) {
                    slider.setValue(((Number) value).doubleValue());
                    // The value label will be updated automatically via the listener in AreaItemFactory
                }
                return;
            }
        }
        
        if (control instanceof TextField) {
            ((TextField) control).setText(value != null ? String.valueOf(value) : "");
        } else if (control instanceof TextArea) {
            ((TextArea) control).setText(value != null ? String.valueOf(value) : "");
        } else if (control instanceof CheckBox) {
            ((CheckBox) control).setSelected(value instanceof Boolean && (Boolean) value);
        } else if (control instanceof Slider) {
            if (value instanceof Number) {
                ((Slider) control).setValue(((Number) value).doubleValue());
            }
        } else if (control instanceof Spinner) {
            if (value instanceof Number) {
                @SuppressWarnings("unchecked")
                Spinner<Integer> spinner = (Spinner<Integer>) control;
                // Check if ValueFactory exists before trying to set value
                if (spinner.getValueFactory() != null) {
                    spinner.getValueFactory().setValue(((Number) value).intValue());
                }
            }
        } else if (control instanceof ComboBox) {
            if (value != null) {
                @SuppressWarnings("unchecked")
                ComboBox<String> comboBox = (ComboBox<String>) control;
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> optionsMap = 
                    (java.util.Map<String, String>) comboBox.getProperties().get("optionsMap");
                String valueToDisplay = getDisplayTextForDataValue(String.valueOf(value), optionsMap);
                comboBox.setValue(valueToDisplay);
            }
        } else if (control instanceof ChoiceBox) {
            if (value != null) {
                @SuppressWarnings("unchecked")
                ChoiceBox<String> choiceBox = (ChoiceBox<String>) control;
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> optionsMap = 
                    (java.util.Map<String, String>) choiceBox.getProperties().get("optionsMap");
                String valueToDisplay = getDisplayTextForDataValue(String.valueOf(value), optionsMap);
                choiceBox.setValue(valueToDisplay);
            }
        } else if (control instanceof Label) {
            ((Label) control).setText(value != null ? String.valueOf(value) : "");
        } else if (control instanceof TableView) {
            updateTableView(control, value);
        } else if (control instanceof ColorPicker) {
            updateColorPicker(control, value);
        } else if (control instanceof DatePicker) {
            updateDatePicker(control, value);
        } else if (control instanceof ImageView) {
            updateImageView(control, value);
        } else if (control instanceof javafx.scene.layout.StackPane) {
            // Check if StackPane contains an ImageView (wrapped for background support)
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) control;
            if (!stackPane.getChildren().isEmpty() && stackPane.getChildren().get(0) instanceof ImageView) {
                updateImageView(stackPane.getChildren().get(0), value);
            }
        }
    }
    
    /**
     * Updates an ImageView control with an image value.
     * Supports EbsImage, ArrayFixedByte (raw image bytes), or file path strings.
     * 
     * @param control The ImageView control
     * @param value The image value (EbsImage, ArrayFixedByte, byte[], or String path)
     */
    private static void updateImageView(Node control, Object value) {
        ImageView imageView = (ImageView) control;
        
        if (value == null) {
            imageView.setImage(null);
            return;
        }
        
        try {
            if (value instanceof EbsImage) {
                // Use the JavaFX image directly from EbsImage
                EbsImage ebsImage = (EbsImage) value;
                imageView.setImage(ebsImage.getFxImage());
            } else if (value instanceof ArrayFixedByte) {
                // Convert byte array to JavaFX Image
                ArrayFixedByte afb = (ArrayFixedByte) value;
                ByteArrayInputStream bais = new ByteArrayInputStream(afb.elements);
                Image image = new Image(bais);
                imageView.setImage(image);
            } else if (value instanceof byte[]) {
                // Convert raw byte array to JavaFX Image
                byte[] bytes = (byte[]) value;
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                Image image = new Image(bais);
                imageView.setImage(image);
            } else if (value instanceof String) {
                // Try to load from file path or URL
                String path = (String) value;
                if (!path.isEmpty()) {
                    try {
                        // Try as URL first (including file: URLs)
                        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file:")) {
                            Image image = new Image(path);
                            imageView.setImage(image);
                        } else {
                            // Try as file path
                            java.io.File file = new java.io.File(path);
                            if (file.exists()) {
                                Image image = new Image(file.toURI().toString());
                                imageView.setImage(image);
                            } else {
                                System.err.println("Warning: Image file not found: " + path);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to load image from path '" + path + "': " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            String varName = (String) control.getProperties().get("varName");
            System.err.println("Warning: Failed to update ImageView" 
                + (varName != null ? " (variable: " + varName + ")" : "") 
                + ": " + e.getMessage());
        }
    }
    
    /**
     * Updates a TableView control with list/array data.
     * 
     * @param control The TableView control
     * @param value The value (expected to be a List or ArrayDynamic)
     */
    private static void updateTableView(Node control, Object value) {
        @SuppressWarnings("unchecked")
        TableView<Map<String, Object>> tableView = (TableView<Map<String, Object>>) control;
        
        // Clear existing items
        tableView.getItems().clear();
        
        // Add new items if value is a collection
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapItem = (Map<String, Object>) item;
                    tableView.getItems().add(mapItem);
                }
            }
        } else if (value instanceof ArrayDynamic) {
            // Handle EBS array type
            ArrayDynamic array = (ArrayDynamic) value;
            for (Object item : array.getAll()) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapItem = (Map<String, Object>) item;
                    tableView.getItems().add(mapItem);
                }
            }
        }
    }
    
    /**
     * Updates a ColorPicker control with a color value.
     * 
     * @param control The ColorPicker control
     * @param value The color value (expected to be a color string like "#ff0000")
     */
    private static void updateColorPicker(Node control, Object value) {
        if (value != null) {
            String colorString = String.valueOf(value);
            try {
                Color color = Color.web(colorString);
                ((ColorPicker) control).setValue(color);
            } catch (IllegalArgumentException e) {
                String varName = (String) control.getProperties().get("varName");
                System.err.println("Warning: Invalid color string '" + colorString + "' for ColorPicker" 
                    + (varName != null ? " (variable: " + varName + ")" : ""));
            }
        }
    }
    
    /**
     * Updates a DatePicker control with a date value.
     * 
     * @param control The DatePicker control
     * @param value The date value (LocalDate or date string)
     */
    private static void updateDatePicker(Node control, Object value) {
        if (value != null) {
            if (value instanceof LocalDate) {
                ((DatePicker) control).setValue((LocalDate) value);
            } else {
                // Try to parse as string
                try {
                    LocalDate date = LocalDate.parse(String.valueOf(value));
                    ((DatePicker) control).setValue(date);
                } catch (DateTimeParseException e) {
                    String varName = (String) control.getProperties().get("varName");
                    System.err.println("Warning: Invalid date string '" + value + "' for DatePicker"
                        + (varName != null ? " (variable: " + varName + ")" : ""));
                }
            }
        }
    }
    
    /**
     * Gets the display text (map value) for a given data value (map key) from an optionsMap.
     * If optionsMap is null or the value is not found, returns the original data value.
     * 
     * @param dataValue The data value (map key) to look up
     * @param optionsMap The map of data value (key) to display text (value), or null
     * @return The display text for the data value, or the original data value if not found
     */
    private static String getDisplayTextForDataValue(String dataValue, java.util.Map<String, String> optionsMap) {
        if (optionsMap == null || dataValue == null) {
            return dataValue;
        }
        
        // Find the display text (value) for this data value (key)
        if (optionsMap.containsKey(dataValue)) {
            return optionsMap.get(dataValue);
        }
        
        // Value not found in map, return original
        return dataValue;
    }
    
    /**
     * Refreshes all bound controls by updating their values from the screenVars map.
     * This is called after onClick handlers execute to reflect variable changes in the UI.
     * 
     * @param boundControls List of bound controls to refresh
     * @param screenVars The screen variables map
     */
    public static void refreshBoundControls(List<Node> boundControls,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        if (boundControls == null || screenVars == null) {
            return;
        }

        for (Node control : boundControls) {
            String varName = (String) control.getProperties().get("varName");
            DisplayItem metadata = (DisplayItem) control.getProperties().get("metadata");

            if (varName != null) {
                // Use VarRefResolver for case-insensitive lookup and complex expressions
                Object currentValue = VarRefResolver.resolveVarRefValue(varName.toLowerCase(), screenVars);
                updateControlFromValue(control, currentValue, metadata);
            }
        }
    }
}
