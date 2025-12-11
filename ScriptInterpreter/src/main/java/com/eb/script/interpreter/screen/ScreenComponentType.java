package com.eb.script.interpreter.screen;

import javafx.scene.Node;

/**
 * Represents a screen component type (e.g., Screen.textArea, Screen.button).
 * This is used to track the JavaFX component type for screen variables.
 * Also stores a reference to the actual JavaFX Node.
 * 
 * @author Earl Bosch
 */
public class ScreenComponentType {
    
    private final String componentType;  // e.g., "textArea", "button", "textfield"
    private Node javafxNode;  // Reference to the actual JavaFX component
    
    /**
     * Create a new ScreenComponentType
     * @param componentType The component type string (e.g., "textArea", "button")
     */
    public ScreenComponentType(String componentType) {
        this.componentType = componentType != null ? componentType.toLowerCase() : null;
        this.javafxNode = null;
    }
    
    /**
     * Create a new ScreenComponentType with JavaFX Node
     * @param componentType The component type string (e.g., "textArea", "button")
     * @param javafxNode The JavaFX Node reference
     */
    public ScreenComponentType(String componentType, Node javafxNode) {
        this.componentType = componentType != null ? componentType.toLowerCase() : null;
        this.javafxNode = javafxNode;
    }
    
    /**
     * Get the component type string
     * @return The component type (lowercase)
     */
    public String getComponentType() {
        return componentType;
    }
    
    /**
     * Get the JavaFX Node reference
     * @return The JavaFX Node, or null if not set
     */
    public Node getJavaFXNode() {
        return javafxNode;
    }
    
    /**
     * Set the JavaFX Node reference
     * @param javafxNode The JavaFX Node to store
     */
    public void setJavaFXNode(Node javafxNode) {
        this.javafxNode = javafxNode;
    }
    
    /**
     * Get the full type name in "Screen.xxx" format
     * @return The full type name with capitalized first letter (e.g., "Screen.textArea")
     */
    public String getFullTypeName() {
        if (componentType == null || componentType.isEmpty()) {
            return "Screen";
        }
        
        // Capitalize first letter of component type for display
        String capitalized = componentType.substring(0, 1).toUpperCase() + componentType.substring(1);
        return "Screen." + capitalized;
    }
    
    /**
     * Get a description of the JavaFX component including type, size, style, etc.
     * @return String description of the JavaFX component
     */
    public String getJavaFXDescription() {
        if (javafxNode == null) {
            return "JavaFX Node: null";
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append("JavaFX Component Description:\n");
        desc.append("  Type: ").append(javafxNode.getClass().getSimpleName()).append("\n");
        desc.append("  Component Type: ").append(getFullTypeName()).append("\n");
        
        // Size information
        desc.append("  Width: ").append(String.format("%.2f", javafxNode.getLayoutBounds().getWidth())).append("\n");
        desc.append("  Height: ").append(String.format("%.2f", javafxNode.getLayoutBounds().getHeight())).append("\n");
        
        // Position
        desc.append("  X: ").append(String.format("%.2f", javafxNode.getLayoutX())).append("\n");
        desc.append("  Y: ").append(String.format("%.2f", javafxNode.getLayoutY())).append("\n");
        
        // Style
        String style = javafxNode.getStyle();
        if (style != null && !style.isEmpty()) {
            desc.append("  Style: ").append(style).append("\n");
        }
        
        // Style classes
        if (!javafxNode.getStyleClass().isEmpty()) {
            desc.append("  Style Classes: ").append(String.join(", ", javafxNode.getStyleClass())).append("\n");
        }
        
        // Visibility
        desc.append("  Visible: ").append(javafxNode.isVisible()).append("\n");
        desc.append("  Managed: ").append(javafxNode.isManaged()).append("\n");
        desc.append("  Disabled: ").append(javafxNode.isDisabled()).append("\n");
        
        // ID
        String id = javafxNode.getId();
        if (id != null && !id.isEmpty()) {
            desc.append("  ID: ").append(id).append("\n");
        }
        
        return desc.toString();
    }
    
    @Override
    public String toString() {
        return getFullTypeName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ScreenComponentType that = (ScreenComponentType) obj;
        return componentType != null ? componentType.equals(that.componentType) : that.componentType == null;
    }
    
    @Override
    public int hashCode() {
        return componentType != null ? componentType.hashCode() : 0;
    }
}
