package com.eb.script.interpreter.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for area-related definitions including area types, area definitions, and area items.
 * This class holds all the metadata and types related to JavaFX UI area/container rendering for the screen keyword.
 */
public class AreaDefinition {
    // Area name/identifier
    public String name;
    // Area type enum
    public AreaType areaType;
    // Area type string (for compatibility)
    public String type;
    // CSS class name from enum
    public String cssClass;
    // Layout configuration
    public String layout;
    // Style string (defaults to areaType's default)
    public String style;
    // Associated screen name
    public String screenName;
    // Display name for UI elements (e.g., tab labels)
    public String displayName;
    // Title for titled containers (e.g., TitledPane, group headers)
    public String title;
    // Group border style: none, raised, inset, lowered, line (default: none)
    public String groupBorder;
    // Group border color in hex format (e.g., "#4a9eff")
    public String groupBorderColor;
    // Group label text (displayed inside/on the group border)
    public String groupLabelText;
    // Group label alignment: left, center, right (default: left)
    public String groupLabelAlignment;
    // Spacing between children (for HBox, VBox, FlowPane, GridPane, TilePane)
    public String spacing;
    // Padding inside the area (for all Region types)
    public String padding;
    // Items in this area
    public List<AreaItem> items = new ArrayList<>();
    // Nested child areas (areas within areas)
    public List<AreaDefinition> childAreas = new ArrayList<>();
    // Inline code to execute when focus enters this area
    public String gainFocus;
    // Inline code to execute when focus leaves this area
    public String lostFocus;
    
    @Override
    public String toString() {
        return "AreaDefinition{" +
               "name='" + name + '\'' +
               ", areaType=" + areaType +
               ", cssClass='" + cssClass + '\'' +
               ", layout='" + layout + '\'' +
               ", style='" + style + '\'' +
               ", title='" + title + '\'' +
               ", groupBorder='" + groupBorder + '\'' +
               ", groupBorderColor='" + groupBorderColor + '\'' +
               ", groupLabelText='" + groupLabelText + '\'' +
               ", groupLabelAlignment='" + groupLabelAlignment + '\'' +
               ", spacing='" + spacing + '\'' +
               ", padding='" + padding + '\'' +
               ", items=" + items.size() +
               ", childAreas=" + childAreas.size() +
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
    
}
