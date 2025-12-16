package com.eb.script.interpreter.screen;

import javafx.scene.Node;
import javafx.scene.layout.*;

/**
 * Represents a screen container type (e.g., Screen.container for HBox, VBox, GridPane).
 * This is used to track the JavaFX container type for named screen areas.
 * Also stores a reference to the actual JavaFX Region node.
 * 
 * @author Earl Bosch
 */
public class ScreenContainerType {
    
    private final String containerType;  // e.g., "hbox", "vbox", "gridpane"
    private Region javafxRegion;  // Reference to the actual JavaFX container
    
    /**
     * Create a new ScreenContainerType
     * @param containerType The container type string (e.g., "hbox", "vbox", "gridpane")
     */
    public ScreenContainerType(String containerType) {
        this.containerType = containerType != null ? containerType.toLowerCase() : null;
        this.javafxRegion = null;
    }
    
    /**
     * Create a new ScreenContainerType with JavaFX Region
     * @param containerType The container type string (e.g., "hbox", "vbox", "gridpane")
     * @param javafxRegion The JavaFX Region reference
     */
    public ScreenContainerType(String containerType, Region javafxRegion) {
        this.containerType = containerType != null ? containerType.toLowerCase() : null;
        this.javafxRegion = javafxRegion;
    }
    
    /**
     * Get the container type string
     * @return The container type (lowercase)
     */
    public String getContainerType() {
        return containerType;
    }
    
    /**
     * Get the JavaFX Region reference
     * @return The JavaFX Region, or null if not set
     */
    public Region getJavaFXRegion() {
        return javafxRegion;
    }
    
    /**
     * Set the JavaFX Region reference
     * @param javafxRegion The JavaFX Region to store
     */
    public void setJavaFXRegion(Region javafxRegion) {
        this.javafxRegion = javafxRegion;
    }
    
    /**
     * Get the full type name in "Screen.container" format
     * @return The full type name (always "Screen.container")
     */
    public String getFullTypeName() {
        return "Screen.container";
    }
    
    /**
     * Get a description of the JavaFX container including type, size, style, etc.
     * This is the .javafx property that lists all major attributes.
     * @return String description of the JavaFX container
     */
    public String getJavaFXDescription() {
        if (javafxRegion == null) {
            return "JavaFX Container: null";
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append("JavaFX Container Description:\n");
        desc.append("  Container Type: ").append(containerType != null ? containerType : "unknown").append("\n");
        desc.append("  JavaFX Class: ").append(javafxRegion.getClass().getSimpleName()).append("\n");
        
        // Size information
        desc.append("  Width: ").append(String.format("%.2f", javafxRegion.getWidth())).append("\n");
        desc.append("  Height: ").append(String.format("%.2f", javafxRegion.getHeight())).append("\n");
        desc.append("  Min Width: ").append(String.format("%.2f", javafxRegion.getMinWidth())).append("\n");
        desc.append("  Min Height: ").append(String.format("%.2f", javafxRegion.getMinHeight())).append("\n");
        desc.append("  Pref Width: ").append(String.format("%.2f", javafxRegion.getPrefWidth())).append("\n");
        desc.append("  Pref Height: ").append(String.format("%.2f", javafxRegion.getPrefHeight())).append("\n");
        desc.append("  Max Width: ").append(String.format("%.2f", javafxRegion.getMaxWidth())).append("\n");
        desc.append("  Max Height: ").append(String.format("%.2f", javafxRegion.getMaxHeight())).append("\n");
        
        // Position
        desc.append("  X: ").append(String.format("%.2f", javafxRegion.getLayoutX())).append("\n");
        desc.append("  Y: ").append(String.format("%.2f", javafxRegion.getLayoutY())).append("\n");
        
        // Padding
        javafx.geometry.Insets padding = javafxRegion.getPadding();
        if (padding != null) {
            desc.append("  Padding: ").append(String.format("%.0f %.0f %.0f %.0f", 
                padding.getTop(), padding.getRight(), padding.getBottom(), padding.getLeft())).append("\n");
        }
        
        // Container-specific properties
        if (javafxRegion instanceof HBox) {
            HBox hbox = (HBox) javafxRegion;
            desc.append("  Spacing: ").append(String.format("%.2f", hbox.getSpacing())).append("\n");
            desc.append("  Alignment: ").append(hbox.getAlignment()).append("\n");
        } else if (javafxRegion instanceof VBox) {
            VBox vbox = (VBox) javafxRegion;
            desc.append("  Spacing: ").append(String.format("%.2f", vbox.getSpacing())).append("\n");
            desc.append("  Alignment: ").append(vbox.getAlignment()).append("\n");
        } else if (javafxRegion instanceof GridPane) {
            GridPane gridPane = (GridPane) javafxRegion;
            desc.append("  HGap: ").append(String.format("%.2f", gridPane.getHgap())).append("\n");
            desc.append("  VGap: ").append(String.format("%.2f", gridPane.getVgap())).append("\n");
            desc.append("  Alignment: ").append(gridPane.getAlignment()).append("\n");
        } else if (javafxRegion instanceof FlowPane) {
            FlowPane flowPane = (FlowPane) javafxRegion;
            desc.append("  HGap: ").append(String.format("%.2f", flowPane.getHgap())).append("\n");
            desc.append("  VGap: ").append(String.format("%.2f", flowPane.getVgap())).append("\n");
            desc.append("  Alignment: ").append(flowPane.getAlignment()).append("\n");
            desc.append("  Orientation: ").append(flowPane.getOrientation()).append("\n");
        } else if (javafxRegion instanceof BorderPane) {
            BorderPane borderPane = (BorderPane) javafxRegion;
            desc.append("  Has Top: ").append(borderPane.getTop() != null).append("\n");
            desc.append("  Has Bottom: ").append(borderPane.getBottom() != null).append("\n");
            desc.append("  Has Left: ").append(borderPane.getLeft() != null).append("\n");
            desc.append("  Has Right: ").append(borderPane.getRight() != null).append("\n");
            desc.append("  Has Center: ").append(borderPane.getCenter() != null).append("\n");
        } else if (javafxRegion instanceof TilePane) {
            TilePane tilePane = (TilePane) javafxRegion;
            desc.append("  HGap: ").append(String.format("%.2f", tilePane.getHgap())).append("\n");
            desc.append("  VGap: ").append(String.format("%.2f", tilePane.getVgap())).append("\n");
            desc.append("  Orientation: ").append(tilePane.getOrientation()).append("\n");
            desc.append("  Pref Columns: ").append(tilePane.getPrefColumns()).append("\n");
            desc.append("  Pref Rows: ").append(tilePane.getPrefRows()).append("\n");
        }
        
        // Style
        String style = javafxRegion.getStyle();
        if (style != null && !style.isEmpty()) {
            desc.append("  Style: ").append(style).append("\n");
        }
        
        // Style classes
        if (!javafxRegion.getStyleClass().isEmpty()) {
            desc.append("  Style Classes: ").append(String.join(", ", javafxRegion.getStyleClass())).append("\n");
        }
        
        // Visibility
        desc.append("  Visible: ").append(javafxRegion.isVisible()).append("\n");
        desc.append("  Managed: ").append(javafxRegion.isManaged()).append("\n");
        desc.append("  Disabled: ").append(javafxRegion.isDisabled()).append("\n");
        
        // ID
        String id = javafxRegion.getId();
        if (id != null && !id.isEmpty()) {
            desc.append("  ID: ").append(id).append("\n");
        }
        
        // Child count
        if (javafxRegion instanceof Pane) {
            desc.append("  Children Count: ").append(((Pane) javafxRegion).getChildren().size()).append("\n");
        }
        
        return desc.toString();
    }
    
    /**
     * Get detailed help information about this container type.
     * Includes description, supported properties, and usage examples.
     * @return Help text for this container type
     */
    public String getHelp() {
        if (containerType == null) {
            return "Unknown container type";
        }
        
        StringBuilder help = new StringBuilder();
        help.append("═══════════════════════════════════════════════════════════\n");
        help.append("Container Type: ").append(getFullTypeName()).append("\n");
        help.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Add description based on container type
        String description = getContainerDescription(containerType);
        if (description != null && !description.isEmpty()) {
            help.append("Description:\n");
            help.append(description).append("\n\n");
        }
        
        // Add supported properties
        help.append("Supported Properties:\n");
        help.append(getSupportedProperties(containerType));
        help.append("\n");
        
        // Add usage example
        String example = getUsageExample(containerType);
        if (example != null && !example.isEmpty()) {
            help.append("Example:\n");
            help.append(example).append("\n");
        }
        
        help.append("═══════════════════════════════════════════════════════════\n");
        return help.toString();
    }
    
    /**
     * Get a description for a specific container type.
     */
    private String getContainerDescription(String type) {
        return switch (type.toLowerCase()) {
            case "hbox" -> "Horizontal box layout that arranges children in a single row from left to right.\n" +
                          "Ideal for buttons, toolbars, and horizontal layouts.";
            case "vbox" -> "Vertical box layout that arranges children in a single column from top to bottom.\n" +
                          "Ideal for forms, menus, and vertical layouts.";
            case "gridpane" -> "Grid layout that arranges children in rows and columns.\n" +
                              "Ideal for forms, tables, and structured layouts with precise positioning.";
            case "stackpane" -> "Stacked layout where children overlay each other.\n" +
                               "Ideal for layered content, overlays, and centered content.";
            case "borderpane" -> "Layout with five regions: top, bottom, left, right, and center.\n" +
                                "Ideal for application layouts with headers, footers, and sidebars.";
            case "flowpane" -> "Flow layout that wraps children horizontally or vertically.\n" +
                              "Ideal for tags, chips, and dynamic content that needs to wrap.";
            case "tilepane" -> "Tile layout that arranges children in uniform-sized tiles.\n" +
                              "Ideal for image galleries, icon grids, and uniform content.";
            case "anchorpane" -> "Flexible layout where children are positioned using anchors.\n" +
                                "Ideal for custom layouts with precise control over positioning.";
            case "pane" -> "Basic layout pane with no automatic positioning.\n" +
                          "Ideal for custom layouts where you control all positioning.";
            case "scrollpane" -> "Container that adds scrollbars for content larger than the view.\n" +
                                "Ideal for large forms, documents, and scrollable content.";
            case "splitpane" -> "Container with resizable dividers between children.\n" +
                               "Ideal for resizable panels, editors, and multi-view layouts.";
            case "tabpane" -> "Container with tabs for switching between different content panels.\n" +
                             "Ideal for multi-page forms, settings panels, and tabbed interfaces.";
            case "accordion" -> "Container with collapsible titled panels (only one open at a time).\n" +
                               "Ideal for settings, navigation menus, and grouped content.";
            case "titledpane" -> "Container with a collapsible title bar.\n" +
                                "Ideal for collapsible sections, groups, and expandable content.";
            case "group" -> "Logical grouping of controls, typically rendered as a VBox with spacing.\n" +
                           "Ideal for grouping related form fields and controls.";
            case "region" -> "Base layout class for custom containers.\n" +
                            "Ideal for extending with custom layout logic.";
            case "canvas" -> "Drawing surface for custom graphics and rendering.\n" +
                            "Ideal for charts, diagrams, and custom visualizations.";
            default -> "Container for organizing and laying out UI components.";
        };
    }
    
    /**
     * Get supported properties for a specific container type.
     */
    private String getSupportedProperties(String type) {
        StringBuilder props = new StringBuilder();
        
        // Common properties for all containers
        props.append("  • type (string) - Container type name\n");
        props.append("  • spacing (number) - Gap between children (HBox, VBox, FlowPane, GridPane, TilePane)\n");
        props.append("  • padding (string) - Internal spacing around children (all containers)\n");
        props.append("  • alignment (string) - Child alignment position (HBox, VBox, StackPane, FlowPane, TilePane)\n");
        props.append("  • style (string) - Custom CSS styling\n");
        props.append("  • areaBackground (string) - Background color in hex format\n");
        props.append("  • minWidth, prefWidth, maxWidth (string) - Width constraints\n");
        props.append("  • minHeight, prefHeight, maxHeight (string) - Height constraints\n");
        props.append("  • hgrow, vgrow (string) - Growth priority (ALWAYS, SOMETIMES, NEVER)\n");
        
        // Container-specific properties
        switch (type.toLowerCase()) {
            case "gridpane":
                props.append("  • hgap (number) - Horizontal gap between columns\n");
                props.append("  • vgap (number) - Vertical gap between rows\n");
                break;
            case "flowpane", "tilepane":
                props.append("  • hgap (number) - Horizontal gap between items\n");
                props.append("  • vgap (number) - Vertical gap between items\n");
                props.append("  • orientation (string) - HORIZONTAL or VERTICAL\n");
                break;
            case "titledpane":
                props.append("  • title (string) - Title text for the pane\n");
                break;
            case "group":
                props.append("  • groupBorder (string) - Border style (none, line, raised, lowered, inset, outset)\n");
                props.append("  • groupBorderColor (string) - Border color in hex format\n");
                props.append("  • groupBorderWidth (string) - Border width in pixels\n");
                props.append("  • groupBorderRadius (string) - Border corner radius in pixels\n");
                props.append("  • groupLabelText (string) - Label text on the border\n");
                props.append("  • groupLabelAlignment (string) - Label alignment (left, center, right)\n");
                break;
        }
        
        return props.toString();
    }
    
    /**
     * Get a usage example for a specific container type.
     */
    private String getUsageExample(String type) {
        return switch (type.toLowerCase()) {
            case "hbox" -> """
                {
                    "name": "buttonBar",
                    "type": "hbox",
                    "alignment": "right",
                    "spacing": "10",
                    "padding": "10 20",
                    "items": [...]
                }
                """;
            case "vbox" -> """
                {
                    "name": "formPanel",
                    "type": "vbox",
                    "alignment": "top-center",
                    "spacing": "15",
                    "padding": "20",
                    "items": [...]
                }
                """;
            case "gridpane" -> """
                {
                    "name": "dataGrid",
                    "type": "gridpane",
                    "spacing": "10",
                    "padding": "15",
                    "items": [...]
                }
                """;
            case "group" -> """
                {
                    "name": "settings",
                    "type": "group",
                    "groupBorder": "line",
                    "groupBorderColor": "#4a9eff",
                    "groupLabelText": "Settings",
                    "spacing": "8",
                    "items": [...]
                }
                """;
            default -> "See documentation for " + type + " container usage examples.";
        };
    }
    
    @Override
    public String toString() {
        return getFullTypeName() + " [" + (containerType != null ? containerType : "unknown") + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ScreenContainerType that = (ScreenContainerType) obj;
        return containerType != null ? containerType.equals(that.containerType) : that.containerType == null;
    }
    
    @Override
    public int hashCode() {
        return containerType != null ? containerType.hashCode() : 0;
    }
}
