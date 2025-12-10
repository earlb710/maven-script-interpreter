package com.eb.script.interpreter.screen;

/**
 * Represents a screen component type (e.g., Screen.textArea, Screen.button).
 * This is used to track the JavaFX component type for screen variables.
 * 
 * @author Earl Bosch
 */
public class ScreenComponentType {
    
    private final String componentType;  // e.g., "textArea", "button", "textfield"
    
    /**
     * Create a new ScreenComponentType
     * @param componentType The component type string (e.g., "textArea", "button")
     */
    public ScreenComponentType(String componentType) {
        this.componentType = componentType != null ? componentType.toLowerCase() : null;
    }
    
    /**
     * Get the component type string
     * @return The component type (lowercase)
     */
    public String getComponentType() {
        return componentType;
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
