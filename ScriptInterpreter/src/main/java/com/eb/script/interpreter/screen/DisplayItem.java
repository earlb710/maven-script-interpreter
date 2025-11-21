package com.eb.script.interpreter.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for screen display item metadata, input item types, area types, and related definitions.
 * This class holds all the metadata and enum types related to JavaFX UI rendering for the screen keyword.
 */
public class DisplayItem {
    // Item type enum (input and display items)
    ItemType itemType;
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
    // Placeholder text for text inputs (hint shown in empty field)
    String promptHelp;
    // Label text displayed before/above the control
    String labelText;
    // Label text alignment: "left", "center", "right"
    String labelTextAlignment;
    // Options/values for selection controls (ComboBox, ChoiceBox, ListView)
    List<String> options;
    // Label text color
    String labelColor;
    // Label text bold flag
    Boolean labelBold;
    // Label text italic flag
    Boolean labelItalic;
    // Label font size (e.g., "14px", "16px")
    String labelFontSize;
    // Item/control font size (e.g., "12px", "14px", "1em")
    String itemFontSize;
    // Maximum length/width for the control (number of characters for text fields)
    Integer maxLength;
    // Item/control text color
    String itemColor;
    // Item/control text bold flag
    Boolean itemBold;
    // Item/control text italic flag
    Boolean itemItalic;
    // onClick event handler - EBS code to execute when button is clicked
    String onClick;
    // onValidate event handler - EBS code to validate item value, expects return true/false
    String onValidate;
    // Whether to show the current value label for sliders
    Boolean showSliderValue;
    // Source of the value: "data" (original data value) or "display" (formatted display value)
    public String source = "data";
    // Status of the item: "clean" (unchanged) or "changed" (modified from original)
    public String status = "clean";
    // Column definitions for TableView - List of column metadata
    List<TableColumn> columns;
    
    /**
     * Inner class to define a table column
     */
    public static class TableColumn {
        public String name;        // Column name/header
        public String field;       // Field name in the record (JSON key)
        public String type;        // Data type: "string", "int", "double", "bool"
        public Integer width;      // Column width (optional)
        public String alignment;   // Text alignment: "left", "center", "right"
        
        public TableColumn() {}
        
        public TableColumn(String name, String field) {
            this.name = name;
            this.field = field;
            this.type = "string";
            this.alignment = "left";
        }
        
        public TableColumn(String name, String field, String type) {
            this.name = name;
            this.field = field;
            this.type = type;
            this.alignment = "left";
        }
    }
    
    @Override
    public String toString() {
        return "DisplayItem{" +
               "itemType=" + itemType +
               ", cssClass='" + cssClass + '\'' +
               ", mandatory=" + mandatory +
               ", case='" + caseFormat + '\'' +
               ", min=" + min +
               ", max=" + max +
               ", style='" + style + '\'' +
               ", alignment='" + alignment + '\'' +
               ", pattern='" + pattern + '\'' +
               ", promptHelp='" + promptHelp + '\'' +
               ", options=" + options +
               ", labelColor='" + labelColor + '\'' +
               ", labelBold=" + labelBold +
               ", labelItalic=" + labelItalic +
               '}';
    }
    
    /**
     * Enum representing all supported JavaFX item types (input and display) with their CSS classes and default styles.
     */
    public enum ItemType {
        // Text Input Controls
        TEXTFIELD("textfield", "screen-item-textfield", 
                  "-fx-padding: 5 10 5 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        TEXTAREA("textarea", "screen-item-textarea",
                 "-fx-padding: 2; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        PASSWORDFIELD("passwordfield", "screen-item-passwordfield",
                      "-fx-padding: 5 10 5 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;"),
        
        // Selection Controls
        CHECKBOX("checkbox", "screen-item-checkbox",
                 "-fx-padding: 5;"),
        RADIOBUTTON("radiobutton", "screen-item-radiobutton",
                    "-fx-padding: 5;"),
        TOGGLEBUTTON("togglebutton", "screen-item-togglebutton",
                     "-fx-padding: 8 15 8 15; -fx-background-color: #e0e0e0; -fx-border-radius: 3; -fx-background-radius: 3;"),
        COMBOBOX("combobox", "screen-item-combobox",
                 "-fx-padding: 2; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;"),
        CHOICEBOX("choicebox", "screen-item-choicebox",
                  "-fx-padding: 2; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;"),
        LISTVIEW("listview", "screen-item-listview",
                 "-fx-border-color: #cccccc; -fx-border-width: 1;"),
        TABLEVIEW("tableview", "screen-item-tableview",
                  "-fx-border-color: #cccccc; -fx-border-width: 1;"),
        
        // Numeric Controls
        SPINNER("spinner", "screen-item-spinner",
                "-fx-padding: 2;"),
        SLIDER("slider", "screen-item-slider",
               "-fx-padding: 5 10 5 10;"),
        
        // Date/Time Controls
        DATEPICKER("datepicker", "screen-item-datepicker",
                   "-fx-padding: 2; -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;"),
        
        // Color Control
        COLORPICKER("colorpicker", "screen-item-colorpicker",
                    "-fx-padding: 2;"),
        
        // Button Controls
        BUTTON("button", "screen-item-button",
               "-fx-padding: 8 15 8 15; -fx-background-color: #4a90e2; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;"),
        
        // Display-Only Controls
        LABEL("label", "screen-item-label",
              "-fx-padding: 2 5 2 5; -fx-font-size: 13px;"),
        LABELTEXT("labeltext", "screen-item-labeltext",
                  "-fx-padding: 2 5 2 5; -fx-font-size: 13px;"),
        TEXT("text", "screen-item-text",
             "-fx-padding: 2; -fx-font-size: 13px;"),
        HYPERLINK("hyperlink", "screen-item-hyperlink",
                  "-fx-padding: 2 5 2 5; -fx-text-fill: #4a90e2; -fx-underline: true;"),
        SEPARATOR("separator", "screen-item-separator",
                  "-fx-padding: 5 0 5 0;"),
        
        // Media/Display Controls
        IMAGEVIEW("imageview", "screen-item-imageview",
                  "-fx-fit-width: 100; -fx-fit-height: 100; -fx-preserve-ratio: true;"),
        MEDIAVIEW("mediaview", "screen-item-mediaview",
                  ""),
        WEBVIEW("webview", "screen-item-webview",
                "-fx-pref-width: 800; -fx-pref-height: 600;"),
        CHART("chart", "screen-item-chart",
              "-fx-padding: 10;"),
        
        // Progress/Status Controls
        PROGRESSBAR("progressbar", "screen-item-progressbar",
                    "-fx-pref-width: 200;"),
        PROGRESSINDICATOR("progressindicator", "screen-item-progressindicator",
                          ""),
        
        // Custom/Other
        CUSTOM("custom", "screen-item-custom", "");
        
        public static final String CSS_FILE = "/css/screen-items.css";
        
        private final String typeName;
        private final String cssClass;
        private final String defaultStyle;
        
        ItemType(String typeName, String cssClass, String defaultStyle) {
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
         * Get ItemType from string, case-insensitive
         */
        public static ItemType fromString(String type) {
            if (type == null) return TEXTFIELD;
            
            String lowerType = type.toLowerCase().trim();
            for (ItemType it : values()) {
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
