package com.eb.script.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for screen display metadata, input item types, area types, and related definitions.
 * This class holds all the metadata and enum types related to JavaFX UI rendering for the screen keyword.
 */
public class DisplayMetadata {
    // Input item type enum
    InputItemType itemType;
    // JavaFX input item type string (for compatibility)
    String type;
    // CSS class name from enum
    String cssClass;
    // Whether the field is mandatory
    boolean mandatory = false;
    // Case handling: "upper", "lower", "title"
    String caseFormat;
    // Minimum value constraint
    Object min;
    // Maximum value constraint
    Object max;
    // CSS style string
    String style;
    // Associated screen name
    String screenName;
    // Text/content alignment (e.g., "left", "center", "right")
    String alignment;
    // Regex pattern for validation (useful for text inputs)
    String pattern;
    
    @Override
    public String toString() {
        return "DisplayMetadata{" +
               "itemType=" + itemType +
               ", cssClass='" + cssClass + '\'' +
               ", mandatory=" + mandatory +
               ", case='" + caseFormat + '\'' +
               ", min=" + min +
               ", max=" + max +
               ", style='" + style + '\'' +
               ", alignment='" + alignment + '\'' +
               ", pattern='" + pattern + '\'' +
               '}';
    }
    
    /**
     * Enum representing all supported JavaFX input item types with their CSS classes and default styles.
     */
    public enum InputItemType {
        // Text Input Controls
        TEXTFIELD("textfield", "screen-input-textfield", 
                  "-fx-padding: 5 10 5 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        TEXTAREA("textarea", "screen-input-textarea",
                 "-fx-padding: 5 10 5 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        PASSWORDFIELD("passwordfield", "screen-input-passwordfield",
                      "-fx-padding: 5 10 5 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        
        // Selection Controls
        CHECKBOX("checkbox", "screen-input-checkbox",
                 "-fx-padding: 5;"),
        RADIOBUTTON("radiobutton", "screen-input-radiobutton",
                    "-fx-padding: 5;"),
        TOGGLEBUTTON("togglebutton", "screen-input-togglebutton",
                     "-fx-padding: 8 15 8 15; -fx-background-color: #e0e0e0; -fx-border-radius: 3; -fx-background-radius: 3;"),
        COMBOBOX("combobox", "screen-input-combobox",
                 "-fx-padding: 5 10 5 10; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;"),
        CHOICEBOX("choicebox", "screen-input-choicebox",
                  "-fx-padding: 5 10 5 10; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;"),
        LISTVIEW("listview", "screen-input-listview",
                 "-fx-border-color: #cccccc; -fx-border-width: 1;"),
        
        // Numeric Controls
        SPINNER("spinner", "screen-input-spinner",
                "-fx-padding: 5 10 5 10;"),
        SLIDER("slider", "screen-input-slider",
               "-fx-padding: 5 10 5 10;"),
        
        // Date/Time Controls
        DATEPICKER("datepicker", "screen-input-datepicker",
                   "-fx-padding: 5 10 5 10; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;"),
        
        // Color Control
        COLORPICKER("colorpicker", "screen-input-colorpicker",
                    "-fx-padding: 5 10 5 10;"),
        
        // Button Controls
        BUTTON("button", "screen-input-button",
               "-fx-padding: 8 15 8 15; -fx-background-color: #4a90e2; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;"),
        
        // Display Controls
        LABEL("label", "screen-input-label",
              "-fx-padding: 2 5 2 5; -fx-font-size: 13px;"),
        HYPERLINK("hyperlink", "screen-input-hyperlink",
                  "-fx-padding: 2 5 2 5; -fx-text-fill: #4a90e2; -fx-underline: true;"),
        
        // Media Controls
        IMAGEVIEW("imageview", "screen-input-imageview",
                  "-fx-fit-width: 100; -fx-fit-height: 100; -fx-preserve-ratio: true;"),
        MEDIAVIEW("mediaview", "screen-input-mediaview",
                  ""),
        WEBVIEW("webview", "screen-input-webview",
                "-fx-pref-width: 800; -fx-pref-height: 600;"),
        
        // Progress Controls
        PROGRESSBAR("progressbar", "screen-input-progressbar",
                    "-fx-pref-width: 200;"),
        PROGRESSINDICATOR("progressindicator", "screen-input-progressindicator",
                          ""),
        
        // Custom/Other
        CUSTOM("custom", "screen-input-custom", "");
        
        public static final String CSS_FILE = "/css/screen-inputs.css";
        
        private final String typeName;
        private final String cssClass;
        private final String defaultStyle;
        
        InputItemType(String typeName, String cssClass, String defaultStyle) {
            this.typeName = typeName;
            this.cssClass = cssClass;
            this.defaultStyle = defaultStyle;
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        public String getCssClass() {
            return cssClass;
        }
        
        public String getDefaultStyle() {
            return defaultStyle;
        }
        
        /**
         * Get InputItemType from string, case-insensitive
         */
        public static InputItemType fromString(String type) {
            if (type == null) return TEXTFIELD;
            
            String lowerType = type.toLowerCase().trim();
            for (InputItemType it : values()) {
                if (it.typeName.equals(lowerType)) {
                    return it;
                }
            }
            return CUSTOM; // Default to custom if not found
        }
        
        @Override
        public String toString() {
            return typeName;
        }
    }
    
}
