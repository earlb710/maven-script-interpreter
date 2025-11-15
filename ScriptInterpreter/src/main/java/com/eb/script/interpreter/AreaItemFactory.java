package com.eb.script.interpreter;

import com.eb.script.interpreter.AreaDefinition.AreaItem;
import com.eb.script.interpreter.DisplayMetadata.ItemType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

/**
 * Factory class for creating JavaFX UI controls from AreaItem definitions.
 * This factory creates JavaFX controls based on the DisplayMetadata and applies
 * display properties only (no layout properties).
 */
public class AreaItemFactory {

    /**
     * Creates a JavaFX control based on the provided AreaItem and DisplayMetadata.
     * Only display properties are applied (promptText, editable, disabled, visible, tooltip, colors, style).
     * Layout properties should be applied by the caller after creation.
     *
     * @param item The AreaItem containing display properties
     * @param metadata The DisplayMetadata containing the control type and styling
     * @return A JavaFX Node representing the control
     */
    public static Node createItem(AreaItem item, DisplayMetadata metadata) {
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
        
        // Apply item-specific display properties
        applyItemSpecificProperties(control, item);
        
        return control;
    }

    /**
     * Creates the appropriate JavaFX control based on the ItemType.
     */
    private static Node createControlByType(ItemType itemType, DisplayMetadata metadata) {
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
                return new ComboBox<>();
            case CHOICEBOX:
                return new ChoiceBox<>();
            case LISTVIEW:
                return new ListView<>();

            // Numeric Controls
            case SPINNER:
                return new Spinner<>();
            case SLIDER:
                return new Slider();

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
     * Applies properties from DisplayMetadata to the control.
     */
    private static void applyMetadataProperties(Node control, DisplayMetadata metadata) {
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
    private static void applyItemSpecificProperties(Node control, AreaItem item) {
        // Apply prompt text or text content based on control type
        if (item.promptText != null && !item.promptText.isEmpty()) {
            if (control instanceof TextField) {
                ((TextField) control).setPromptText(item.promptText);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPromptText(item.promptText);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPromptText(item.promptText);
            } else if (control instanceof Label) {
                // For labels, use promptText as the label's text content
                ((Label) control).setText(item.promptText);
            } else if (control instanceof Button) {
                // For buttons, use promptText as the button's text
                ((Button) control).setText(item.promptText);
            }
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
            String colorStyle = "-fx-text-fill: " + item.textColor + ";";
            control.setStyle(control.getStyle() + " " + colorStyle);
        }

        // Apply background color
        if (item.backgroundColor != null && !item.backgroundColor.isEmpty()) {
            String bgStyle = "-fx-background-color: " + item.backgroundColor + ";";
            control.setStyle(control.getStyle() + " " + bgStyle);
        }
    }
}
