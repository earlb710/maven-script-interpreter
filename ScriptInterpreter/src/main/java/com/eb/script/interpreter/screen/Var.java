package com.eb.script.interpreter.screen;

import com.eb.script.token.DataType;

/**
 * Represents a variable in a screen definition.
 * Each variable has a name, type, default value, and optional display metadata.
 * 
 * @author Earl
 */
public class Var {
    // Variable name
    private String name;
    
    // Variable data type
    private DataType type;
    
    // Default value
    private Object defaultValue;
    
    // Current value
    private Object value;
    
    // Optional display metadata
    private DisplayItem displayItem;
    
    // Reference to the set this variable belongs to
    private String setName;
    
    // Parameter direction: "in", "out", or "inout" (default)
    private String direction;
    
    /**
     * Default constructor
     */
    public Var() {
        this.direction = "inout"; // Default to inout
    }
    
    /**
     * Constructor with name
     * @param name The variable name
     */
    public Var(String name) {
        this.name = name;
    }
    
    /**
     * Constructor with name and type
     * @param name The variable name
     * @param type The variable data type
     */
    public Var(String name, DataType type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * Constructor with name, type, and default value
     * @param name The variable name
     * @param type The variable data type
     * @param defaultValue The default value
     */
    public Var(String name, DataType type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }
    
    // Getters and setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public DataType getType() {
        return type;
    }
    
    public void setType(DataType type) {
        this.type = type;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public DisplayItem getDisplayItem() {
        return displayItem;
    }
    
    public void setDisplayItem(DisplayItem displayItem) {
        this.displayItem = displayItem;
    }
    
    public String getSetName() {
        return setName;
    }
    
    public void setSetName(String setName) {
        this.setName = setName;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = normalizeDirection(direction);
    }
    
    /**
     * Normalize direction value to valid options
     * @param direction The direction value
     * @return Normalized direction ("in", "out", or "inout")
     */
    private String normalizeDirection(String direction) {
        if (direction == null) {
            return "inout";
        }
        String normalized = direction.toLowerCase();
        if ("in".equals(normalized) || "out".equals(normalized) || "inout".equals(normalized)) {
            return normalized;
        }
        // Default to inout for invalid values
        return "inout";
    }
    
    /**
     * Check if this variable is an input parameter
     * @return true if direction is "in" or "inout"
     */
    public boolean isInput() {
        return "in".equals(direction) || "inout".equals(direction);
    }
    
    /**
     * Check if this variable is an output parameter
     * @return true if direction is "out" or "inout"
     */
    public boolean isOutput() {
        return "out".equals(direction) || "inout".equals(direction);
    }
    
    /**
     * Get the fully qualified key for this variable (setname.varname in lowercase)
     * @return The key string
     */
    public String getKey() {
        if (setName == null || name == null) {
            return name != null ? name.toLowerCase() : null;
        }
        return setName.toLowerCase() + "." + name.toLowerCase();
    }
    
    @Override
    public String toString() {
        return "Var{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", defaultValue=" + defaultValue +
                ", value=" + value +
                ", setName='" + setName + '\'' +
                ", direction='" + direction + '\'' +
                ", hasDisplay=" + (displayItem != null) +
                '}';
    }
}
