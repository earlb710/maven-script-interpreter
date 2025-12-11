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
