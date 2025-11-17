package com.eb.script.interpreter.screen;

import com.eb.script.interpreter.screen.DisplayItem.ItemType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

/**
 * Factory class for creating JavaFX UI controls from AreaItem definitions.
 * This factory creates JavaFX controls based on the DisplayItem and applies
 * display properties only (no layout properties).
 */
public class AreaItemFactory {

    /**
     * Creates a JavaFX control based on the provided AreaItem and DisplayItem.
     * Only display properties are applied (promptText, editable, disabled, visible, tooltip, colors, style).
     * Layout properties should be applied by the caller after creation.
     *
     * @param item The AreaItem containing display properties
     * @param metadata The DisplayItem containing the control type and styling
     * @return A JavaFX Node representing the control
     */
    public static Node createItem(AreaItem item, DisplayItem metadata) {
        if (metadata == null) {
            // If no metadata provided, create a simple Label as fallback
            Label label = new Label("No metadata");
            applyCommonProperties(label, item);
            return label;
        }

        ItemType itemType = metadata.itemType;
        if (itemType == null) {
            itemType = ItemType.TEXTFIELD; // Default fallback
        }

        Node control = createControlByType(itemType, metadata);
        
        // Apply common display properties
        applyCommonProperties(control, item);
        
        // Apply metadata-specific properties
        applyMetadataProperties(control, metadata);
        
        // Apply item-specific display properties (including promptText from metadata)
        applyItemSpecificProperties(control, item, metadata);
        
        // Apply control size and font styling
        applyControlSizeAndFont(control, metadata, item);
        
        return control;
    }

    /**
     * Creates the appropriate JavaFX control based on the ItemType.
     */
    private static Node createControlByType(ItemType itemType, DisplayItem metadata) {
        switch (itemType) {
            // Text Input Controls
            case TEXTFIELD:
                return new TextField();
            case TEXTAREA:
                return new TextArea();
            case PASSWORDFIELD:
                return new PasswordField();

            // Selection Controls
            case CHECKBOX:
                return new CheckBox();
            case RADIOBUTTON:
                return new RadioButton();
            case TOGGLEBUTTON:
                return new ToggleButton();
            case COMBOBOX:
                ComboBox<String> comboBox = new ComboBox<>();
                // Populate with options if available
                if (metadata != null && metadata.options != null && !metadata.options.isEmpty()) {
                    comboBox.getItems().addAll(metadata.options);
                }
                return comboBox;
            case CHOICEBOX:
                ChoiceBox<String> choiceBox = new ChoiceBox<>();
                // Populate with options if available
                if (metadata != null && metadata.options != null && !metadata.options.isEmpty()) {
                    choiceBox.getItems().addAll(metadata.options);
                }
                return choiceBox;
            case LISTVIEW:
                return new ListView<>();

            // Numeric Controls
            case SPINNER:
                // Create Spinner with proper ValueFactory
                Spinner<Integer> spinner = new Spinner<>();
                int min = metadata != null && metadata.min instanceof Number ? ((Number) metadata.min).intValue() : 0;
                int max = metadata != null && metadata.max instanceof Number ? ((Number) metadata.max).intValue() : 100;
                int initial = min; // Start at minimum value
                SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
                spinner.setValueFactory(valueFactory);
                spinner.setEditable(true);
                return spinner;
            case SLIDER:
                Slider slider = new Slider();
                // Set min/max if provided in metadata
                if (metadata != null) {
                    if (metadata.min instanceof Number) {
                        slider.setMin(((Number) metadata.min).doubleValue());
                    }
                    if (metadata.max instanceof Number) {
                        slider.setMax(((Number) metadata.max).doubleValue());
                    }
                }
                return slider;

            // Date/Time Controls
            case DATEPICKER:
                return new DatePicker();

            // Color Control
            case COLORPICKER:
                return new ColorPicker();

            // Button Controls
            case BUTTON:
                return new Button();

            // Display-Only Controls
            case LABEL:
            case LABELTEXT:
                return new Label();
            case TEXT:
                return new Text();
            case HYPERLINK:
                return new Hyperlink();
            case SEPARATOR:
                return new Separator();

            // Media/Display Controls
            case IMAGEVIEW:
                return new ImageView();
            case MEDIAVIEW:
                // MediaView requires javafx-media module which is not included
                // Return a label placeholder instead
                return new Label("[MediaView - javafx-media module required]");
            case WEBVIEW:
                // WebView requires javafx-web module which is not included
                // Return a label placeholder instead
                return new Label("[WebView - javafx-web module required]");
            case CHART:
                // Create a default bar chart as placeholder
                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                return new BarChart<>(xAxis, yAxis);

            // Progress/Status Controls
            case PROGRESSBAR:
                return new ProgressBar();
            case PROGRESSINDICATOR:
                return new ProgressIndicator();

            // Custom/fallback
            case CUSTOM:
            default:
                return new Label("Custom");
        }
    }

    /**
     * Applies common properties that can be set on most controls.
     */
    private static void applyCommonProperties(Node control, AreaItem item) {
        // Apply visibility
        if (item.visible != null) {
            control.setVisible(item.visible);
        }

        // Apply disabled state
        if (item.disabled != null) {
            control.setDisable(item.disabled);
        }

        // Apply tooltip
        if (item.tooltip != null && !item.tooltip.isEmpty()) {
            if (control instanceof Control) {
                ((Control) control).setTooltip(new Tooltip(item.tooltip));
            }
        }

        // Apply CSS class and inline styles will be handled after
    }

    /**
     * Applies properties from DisplayItem to the control.
     */
    private static void applyMetadataProperties(Node control, DisplayItem metadata) {
        // Apply CSS class from metadata
        if (metadata.cssClass != null && !metadata.cssClass.isEmpty()) {
            control.getStyleClass().add(metadata.cssClass);
        }

        // Apply default style from ItemType
        if (metadata.itemType != null && metadata.itemType.getDefaultStyle() != null) {
            String defaultStyle = metadata.itemType.getDefaultStyle();
            if (!defaultStyle.isEmpty()) {
                control.setStyle(defaultStyle);
            }
        }

        // Apply custom style from metadata (overrides default)
        if (metadata.style != null && !metadata.style.isEmpty()) {
            control.setStyle(control.getStyle() + "; " + metadata.style);
        }
    }

    /**
     * Applies item-specific display properties to the control.
     */
    private static void applyItemSpecificProperties(Node control, AreaItem item, DisplayItem metadata) {
        // Apply prompt text for input controls (placeholder hint text)
        if (metadata != null && metadata.promptHelp != null && !metadata.promptHelp.isEmpty()) {
            if (control instanceof TextField) {
                ((TextField) control).setPromptText(metadata.promptHelp);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPromptText(metadata.promptHelp);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPromptText(metadata.promptHelp);
            }
        }
        
        // Apply label text for labels and buttons (actual displayed text)
        if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
            if (control instanceof Label) {
                Label label = (Label) control;
                label.setText(metadata.labelText);
                
                // Apply label text styling (color, bold, italic)
                applyPromptTextStyling(label, metadata);
            } else if (control instanceof Button) {
                ((Button) control).setText(metadata.labelText);
            }
        }

        // Apply control text alignment (for the content inside the control)
        if (metadata != null && metadata.alignment != null && !metadata.alignment.isEmpty()) {
            String alignment = metadata.alignment.toLowerCase();
            String alignmentStyle = "";
            
            switch (alignment) {
                case "l":
                case "left":
                    alignmentStyle = "-fx-alignment: center-left;";
                    break;
                case "c":
                case "center":
                    alignmentStyle = "-fx-alignment: center;";
                    break;
                case "r":
                case "right":
                    alignmentStyle = "-fx-alignment: center-right;";
                    break;
            }
            
            if (!alignmentStyle.isEmpty()) {
                if (control instanceof TextField || control instanceof TextArea || control instanceof ComboBox) {
                    control.setStyle(control.getStyle() + " " + alignmentStyle);
                } else if (control instanceof Spinner) {
                    // For Spinner, we need to access the internal TextField
                    Spinner<?> spinner = (Spinner<?>) control;
                    spinner.setStyle(control.getStyle() + " " + alignmentStyle);
                    // Also try to set on the editor if accessible
                    if (spinner.getEditor() != null) {
                        spinner.getEditor().setStyle(alignmentStyle);
                    }
                }
            }
        }

        // Apply label text alignment (this is for the wrapper label, not the control content)
        // This is handled separately and used by ScreenFactory when creating labeled controls
        if (item.displayItem != null && item.displayItem.labelTextAlignment != null && !item.displayItem.labelTextAlignment.isEmpty()) {
            // This alignment is used by ScreenFactory for the label wrapper
            // No need to apply it to the control itself here
        }

        // Apply editable property
        if (item.editable != null) {
            if (control instanceof TextField) {
                ((TextField) control).setEditable(item.editable);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setEditable(item.editable);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setEditable(item.editable);
            }
        }

        // Apply text color
        if (item.textColor != null && !item.textColor.isEmpty()) {
            String currentStyle = control.getStyle();
            String colorStyle = "-fx-text-fill: " + item.textColor + ";";
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(colorStyle);
            } else {
                control.setStyle(currentStyle + " " + colorStyle);
            }
        }

        // Apply background color
        if (item.backgroundColor != null && !item.backgroundColor.isEmpty()) {
            String currentStyle = control.getStyle();
            String bgStyle = "-fx-background-color: " + item.backgroundColor + ";";
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(bgStyle);
            } else {
                control.setStyle(currentStyle + " " + bgStyle);
            }
        }
    }

    /**
     * Applies control size and font styling based on metadata.
     * Calculates preferred width based on maxLength or data type, considering font size.
     */
    private static void applyControlSizeAndFont(Node control, DisplayItem metadata, AreaItem item) {
        if (metadata == null) {
//            System.out.println("[DEBUG] applyControlSizeAndFont: metadata is null, returning");
            return;
        }
        
//        System.out.println("[DEBUG] applyControlSizeAndFont called for control: " + control.getClass().getSimpleName());
//        System.out.println("[DEBUG]   itemFontSize: " + metadata.itemFontSize);
//        System.out.println("[DEBUG]   itemColor: " + metadata.itemColor);
//        System.out.println("[DEBUG]   itemBold: " + metadata.itemBold);
//        System.out.println("[DEBUG]   itemItalic: " + metadata.itemItalic);
        
        // Build comprehensive style string for item
        StringBuilder itemStyle = new StringBuilder();
        
        // Apply item font size
        if (metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            itemStyle.append("-fx-font-size: ").append(metadata.itemFontSize).append("; ");
            //System.out.println("[DEBUG]   Adding font-size: " + metadata.itemFontSize);
        }
        
        // Apply item text color
        if (metadata.itemColor != null && !metadata.itemColor.isEmpty()) {
            itemStyle.append("-fx-text-fill: ").append(metadata.itemColor).append("; ");
            //System.out.println("[DEBUG]   Adding text-fill: " + metadata.itemColor);
        }
        
        // Apply item bold
        if (metadata.itemBold != null && metadata.itemBold) {
            itemStyle.append("-fx-font-weight: bold; ");
            //System.out.println("[DEBUG]   Adding font-weight: bold");
        }
        
        // Apply item italic
        if (metadata.itemItalic != null && metadata.itemItalic) {
            itemStyle.append("-fx-font-style: italic; ");
            //System.out.println("[DEBUG]   Adding font-style: italic");
        }
        
        //System.out.println("[DEBUG]   Built style string: '" + itemStyle.toString() + "'");
        
        // Apply the combined style to the control
        if (itemStyle.length() > 0) {
            String currentStyle = control.getStyle();
            //System.out.println("[DEBUG]   Current style before applying: '" + currentStyle + "'");
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(itemStyle.toString());
            } else {
                control.setStyle(currentStyle + " " + itemStyle.toString());
            }
            //System.out.println("[DEBUG]   Final style after applying: '" + control.getStyle() + "'");
        } else {
            //System.out.println("[DEBUG]   No styles to apply (itemStyle is empty)");
        }
        
        // Calculate and apply preferred width based on maxLength or data type
        double prefWidth = calculateControlWidth(metadata, item);
        if (prefWidth > 0) {
            if (control instanceof TextField) {
                ((TextField) control).setPrefWidth(prefWidth);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPrefWidth(prefWidth);
            } else if (control instanceof PasswordField) {
                ((PasswordField) control).setPrefWidth(prefWidth);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof ChoiceBox) {
                ((ChoiceBox<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof Spinner) {
                ((Spinner<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof DatePicker) {
                ((DatePicker) control).setPrefWidth(prefWidth);
            } else if (control instanceof ColorPicker) {
                ((ColorPicker) control).setPrefWidth(prefWidth);
            }
        }
    }
    
    /**
     * Calculates the preferred width for a control based on maxLength or data type.
     * Takes font size into consideration for accurate sizing.
     */
    private static double calculateControlWidth(DisplayItem metadata, AreaItem item) {
        if (metadata == null) {
            return -1; // Use default
        }
        
        // Create a measuring text node to calculate width
        javafx.scene.text.Text measuringText = new javafx.scene.text.Text();
        
        // Apply font size if specified
        String fontStyle = "-fx-font-weight: normal;";
        if (metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            fontStyle += " -fx-font-size: " + metadata.itemFontSize + ";";
        }
        measuringText.setStyle(fontStyle);
        
        // Determine the character width to use for calculation
        int charCount = 0;
        String sampleText = null;
        
        // Use maxLength if specified
        if (metadata.maxLength != null && metadata.maxLength > 0) {
            charCount = metadata.maxLength;
        } else {
            // For ChoiceBox and ComboBox, use options data to determine size if available
            if ((metadata.itemType == ItemType.CHOICEBOX || metadata.itemType == ItemType.COMBOBOX) 
                    && metadata.options != null && !metadata.options.isEmpty()) {
                // Find the longest option to use as the basis for width calculation
                String longestOption = "";
                for (String option : metadata.options) {
                    if (option != null && option.length() > longestOption.length()) {
                        longestOption = option;
                    }
                }
                if (!longestOption.isEmpty()) {
                    sampleText = longestOption;
                    measuringText.setText(sampleText);
                    double textWidth = measuringText.getLayoutBounds().getWidth();
                    double padding = 40; // Extra padding for dropdown arrow and borders
                    return textWidth + padding;
                }
            }
            
            // For Spinner, use min/max values to determine size if available
            if (metadata.itemType == ItemType.SPINNER && (metadata.min != null || metadata.max != null)) {
                // Find the longest value between min and max
                String longestValue = "";
                
                if (metadata.min != null) {
                    String minStr = String.valueOf(metadata.min);
                    if (minStr.length() > longestValue.length()) {
                        longestValue = minStr;
                    }
                }
                
                if (metadata.max != null) {
                    String maxStr = String.valueOf(metadata.max);
                    if (maxStr.length() > longestValue.length()) {
                        longestValue = maxStr;
                    }
                }
                
                if (!longestValue.isEmpty()) {
                    sampleText = longestValue;
                    measuringText.setText(sampleText);
                    double textWidth = measuringText.getLayoutBounds().getWidth();
                    double padding = 30; // Padding for spinner buttons and borders
                    return textWidth + padding;
                }
            }
            
            // Otherwise, guess length based on item type and data type
            charCount = guessLengthByType(metadata.itemType, item);
        }
        
        if (charCount <= 0) {
            return -1; // Use default
        }
        
        // Use 'M' as a representative character (one of the widest)
        sampleText = "M".repeat(charCount);
        measuringText.setText(sampleText);
        
        // Get the width and add padding for borders, scrollbars, etc.
        double textWidth = measuringText.getLayoutBounds().getWidth();
        double padding = 20; // Account for padding and borders
        
        return textWidth + padding;
    }
    
    /**
     * Guesses an appropriate character length based on control type and context.
     */
    private static int guessLengthByType(DisplayItem.ItemType itemType, AreaItem item) {
        if (itemType == null) {
            return 20; // Default
        }
        
        switch (itemType) {
            case TEXTFIELD:
            case PASSWORDFIELD:
                // Check if item has type info to guess better
                if (item != null && item.varRef != null) {
                    // For numeric types, shorter width
                    return 15;
                }
                return 30; // Default for text fields
                
            case TEXTAREA:
                return 50; // Wider for text areas
                
            case SPINNER:
                return 10; // Spinners are typically narrow
                
            case COMBOBOX:
            case CHOICEBOX:
                return 20; // Medium width for dropdowns
                
            case DATEPICKER:
                return 15; // Date format is typically short
                
            case COLORPICKER:
                return 12; // Color picker is relatively compact
                
            default:
                return 20; // Default fallback
        }
    }

    /**
     * Applies prompt text styling (color, bold, italic) to a Label.
     */
    private static void applyPromptTextStyling(Label label, DisplayItem metadata) {
        if (metadata == null) {
            return;
        }
        
        StringBuilder styleBuilder = new StringBuilder();
        String existingStyle = label.getStyle();
        if (existingStyle != null && !existingStyle.isEmpty()) {
            styleBuilder.append(existingStyle);
            if (!existingStyle.endsWith(";")) {
                styleBuilder.append(";");
            }
            styleBuilder.append(" ");
        }
        
        // Apply color
        if (metadata.labelColor != null && !metadata.labelColor.isEmpty()) {
            styleBuilder.append("-fx-text-fill: ").append(metadata.labelColor).append("; ");
        }
        
        // Apply bold
        if (Boolean.TRUE.equals(metadata.labelBold)) {
            styleBuilder.append("-fx-font-weight: bold; ");
        }
        
        // Apply italic
        if (Boolean.TRUE.equals(metadata.labelItalic)) {
            styleBuilder.append("-fx-font-style: italic; ");
        }
        
        if (styleBuilder.length() > 0) {
            label.setStyle(styleBuilder.toString());
        }
    }
}
