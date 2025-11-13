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
    
    /**
     * Enum representing all supported JavaFX area/container types with their CSS classes and default styles.
     */
    public enum AreaType {
        // Layout Panes
        PANE("pane", "screen-area-pane", "-fx-background-color: transparent;"),
        STACKPANE("stackpane", "screen-area-stackpane", "-fx-background-color: transparent; -fx-alignment: center;"),
        ANCHORPANE("anchorpane", "screen-area-anchorpane", "-fx-background-color: transparent;"),
        BORDERPANE("borderpane", "screen-area-borderpane", "-fx-background-color: transparent;"),
        FLOWPANE("flowpane", "screen-area-flowpane", "-fx-background-color: transparent; -fx-hgap: 5; -fx-vgap: 5;"),
        GRIDPANE("gridpane", "screen-area-gridpane", "-fx-background-color: transparent; -fx-hgap: 10; -fx-vgap: 10;"),
        HBOX("hbox", "screen-area-hbox", "-fx-background-color: transparent; -fx-spacing: 10; -fx-alignment: center-left;"),
        VBOX("vbox", "screen-area-vbox", "-fx-background-color: transparent; -fx-spacing: 10; -fx-alignment: top-center;"),
        TILEPANE("tilepane", "screen-area-tilepane", "-fx-background-color: transparent; -fx-hgap: 5; -fx-vgap: 5;"),
        
        // Containers
        SCROLLPANE("scrollpane", "screen-area-scrollpane", "-fx-background-color: transparent; -fx-fit-to-width: true;"),
        SPLITPANE("splitpane", "screen-area-splitpane", "-fx-background-color: transparent;"),
        TABPANE("tabpane", "screen-area-tabpane", "-fx-background-color: transparent;"),
        TAB("tab", "screen-area-tab", ""),
        ACCORDION("accordion", "screen-area-accordion", "-fx-background-color: transparent;"),
        TITLEDPANE("titledpane", "screen-area-titledpane", "-fx-background-color: transparent;"),
        
        // Special
        GROUP("group", "screen-area-group", ""),
        REGION("region", "screen-area-region", "-fx-background-color: transparent;"),
        CANVAS("canvas", "screen-area-canvas", ""),
        
        // Default fallback
        CUSTOM("custom", "screen-area-custom", "");
        
        public static final String CSS_FILE = "/css/screen-areas.css";
        
        private final String typeName;
        private final String cssClass;
        private final String defaultStyle;
        
        AreaType(String typeName, String cssClass, String defaultStyle) {
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
         * Get AreaType from string, case-insensitive
         */
        public static AreaType fromString(String type) {
            if (type == null) return PANE;
            
            String lowerType = type.toLowerCase().trim();
            for (AreaType at : values()) {
                if (at.typeName.equals(lowerType)) {
                    return at;
                }
            }
            return CUSTOM; // Default to custom if not found
        }
        
        @Override
        public String toString() {
            return typeName;
        }
    }
    
    /**
     * Class to hold area definition with layout and items.
     */
    public static class AreaDefinition {
        // Area name/identifier
        String name;
        // Area type enum
        AreaType areaType;
        // Area type string (for compatibility)
        String type;
        // CSS class name from enum
        String cssClass;
        // Layout configuration
        String layout;
        // Style string (defaults to areaType's default)
        String style;
        // Associated screen name
        String screenName;
        // Items in this area
        List<AreaItem> items = new ArrayList<>();
        
        @Override
        public String toString() {
            return "AreaDefinition{" +
                   "name='" + name + '\'' +
                   ", areaType=" + areaType +
                   ", cssClass='" + cssClass + '\'' +
                   ", layout='" + layout + '\'' +
                   ", style='" + style + '\'' +
                   ", items=" + items.size() +
                   '}';
        }
    }
    
    /**
     * Class to hold area item definition with positioning and variable reference.
     */
    public static class AreaItem {
        // Item name/identifier
        String name;
        // Display sequence/order
        int sequence = 0;
        // Relative position (e.g., "top", "left", "center", coordinates)
        String relativePos;
        // Reference to a variable
        String varRef;
        
        @Override
        public String toString() {
            return "AreaItem{" +
                   "name='" + name + '\'' +
                   ", sequence=" + sequence +
                   ", relativePos='" + relativePos + '\'' +
                   ", varRef='" + varRef + '\'' +
                   '}';
        }
    }
}
