package com.eb.script.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for area-related definitions including area types, area definitions, and area items.
 * This class holds all the metadata and types related to JavaFX UI area/container rendering for the screen keyword.
 */
public class AreaDefinition {
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
     * Class to hold area item definition with positioning, variable reference, and optional display metadata.
     * If displayMetadata is not provided, the item will use the DisplayMetadata associated with its varRef.
     */
    public static class AreaItem {
        // Item name/identifier
        String name;
        // Display sequence/order
        int sequence = 0;
        // Layout position (e.g., "top", "left", "center", "0,0" for GridPane row,col)
        String layoutPos;
        // Reference to a variable
        String varRef;
        // Optional display metadata for this specific item
        // If null, will use the DisplayMetadata from varRef
        DisplayMetadata displayMetadata;
        
        // Additional UI properties for this item
        // Placeholder text for text inputs
        String promptText;
        // Whether the field can be edited
        Boolean editable;
        // Whether the control is disabled
        Boolean disabled;
        // Whether the control is visible
        Boolean visible;
        // Hover tooltip text
        String tooltip;
        // Text color (e.g., "#000000", "red")
        String textColor;
        // Background color (e.g., "#FFFFFF", "lightblue")
        String backgroundColor;
        
        // Layout-specific properties
        // Column span for GridPane layouts
        Integer colSpan;
        // Row span for GridPane layouts
        Integer rowSpan;
        // Horizontal grow priority (for HBox, VBox)
        String hgrow;
        // Vertical grow priority (for HBox, VBox)
        String vgrow;
        // Margin around the item (e.g., "10", "10 5", "10 5 10 5")
        String margin;
        // Padding inside the item (e.g., "10", "10 5", "10 5 10 5")
        String padding;
        // Preferred width
        String prefWidth;
        // Preferred height
        String prefHeight;
        // Minimum width
        String minWidth;
        // Minimum height
        String minHeight;
        // Maximum width
        String maxWidth;
        // Maximum height
        String maxHeight;
        // Alignment within parent (e.g., "center", "top-left")
        String alignment;
        
        @Override
        public String toString() {
            return "AreaItem{" +
                   "name='" + name + '\'' +
                   ", sequence=" + sequence +
                   ", layoutPos='" + layoutPos + '\'' +
                   ", varRef='" + varRef + '\'' +
                   ", displayMetadata=" + (displayMetadata != null ? "provided" : "from varRef") +
                   ", promptText='" + promptText + '\'' +
                   ", editable=" + editable +
                   ", disabled=" + disabled +
                   ", visible=" + visible +
                   ", tooltip='" + tooltip + '\'' +
                   ", textColor='" + textColor + '\'' +
                   ", backgroundColor='" + backgroundColor + '\'' +
                   ", colSpan=" + colSpan +
                   ", rowSpan=" + rowSpan +
                   ", hgrow='" + hgrow + '\'' +
                   ", vgrow='" + vgrow + '\'' +
                   ", margin='" + margin + '\'' +
                   ", padding='" + padding + '\'' +
                   ", prefWidth='" + prefWidth + '\'' +
                   ", prefHeight='" + prefHeight + '\'' +
                   ", alignment='" + alignment + '\'' +
                   '}';
        }
    }
}
