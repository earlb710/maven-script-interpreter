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
    // Group border width in pixels (e.g., "2" or "2px") - default varies by border type
    public String groupBorderWidth;
    // Group border insets in pixels (e.g., "5" for all sides, or "5 10" for top/bottom and left/right, or "5 10 5 10" for top, right, bottom, left)
    public String groupBorderInsets;
    // Group border radius in pixels (e.g., "5" or "5px") - default is 5px
    public String groupBorderRadius;
    // Group label text (displayed inside/on the group border)
    public String groupLabelText;
    // Group label alignment: left, center, right (default: left)
    public String groupLabelAlignment;
    // Group label offset: top, on, bottom (default: on) - controls vertical position relative to border
    public String groupLabelOffset;
    // Group label text color in hex format (e.g., "#4a9eff") - defaults to groupBorderColor
    public String groupLabelColor;
    // Group label background color in hex format (e.g., "#ffffff") - defaults to "white"
    public String groupLabelBackground;
    // Area background color in hex format (e.g., "#f0f0f0") - optional background color for the area
    public String areaBackground;
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
    // Number of records - when set, all items in this area become templates that are duplicated for each record
    // Items' varRef will be expanded with index (e.g., with recordRef="clients" and varRef="age", becomes "clients[0].age")
    public Integer numberOfRecords;
    // Record reference - the array variable name to use when expanding items (e.g., "clients")
    // Combined with item's varRef to form full reference (e.g., recordRef="clients" + varRef="age" -> "clients[N].age")
    public String recordRef;
    // Disable label alignment - when true, controls without labels won't be wrapped in HBox for alignment
    // Useful for grid layouts (like chess boards) where label alignment creates unwanted spacing
    public Boolean disableLabelAlignment;
    // Alignment for child elements within the container (for HBox, VBox, StackPane, FlowPane, TilePane)
    // Valid values: top-left, top-center, top-right, center-left, center, center-right, 
    // bottom-left, bottom-center, bottom-right, baseline-left, baseline-center, baseline-right
    // Shorthand values: left (center-left), right (center-right), top (top-center), bottom (bottom-center)
    public String alignment;
    // Horizontal grow priority for this area when added to HBox or GridPane (ALWAYS, SOMETIMES, NEVER)
    public String hgrow;
    // Vertical grow priority for this area when added to VBox or GridPane (ALWAYS, SOMETIMES, NEVER)
    public String vgrow;
    // Width/height constraints for this area
    public String minWidth;
    public String prefWidth;
    public String maxWidth;
    public String minHeight;
    public String prefHeight;
    public String maxHeight;
    
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
               ", groupBorderWidth='" + groupBorderWidth + '\'' +
               ", groupBorderInsets='" + groupBorderInsets + '\'' +
               ", groupBorderRadius='" + groupBorderRadius + '\'' +
               ", groupLabelText='" + groupLabelText + '\'' +
               ", groupLabelAlignment='" + groupLabelAlignment + '\'' +
               ", groupLabelOffset='" + groupLabelOffset + '\'' +
               ", groupLabelColor='" + groupLabelColor + '\'' +
               ", groupLabelBackground='" + groupLabelBackground + '\'' +
               ", areaBackground='" + areaBackground + '\'' +
               ", alignment='" + alignment + '\'' +
               ", spacing='" + spacing + '\'' +
               ", padding='" + padding + '\'' +
               ", numberOfRecords=" + numberOfRecords +
               ", recordRef='" + recordRef + '\'' +
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
        GROUP("group", "screen-area-group", "-fx-background-color: transparent; -fx-spacing: 4; -fx-padding: 4;"),
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
