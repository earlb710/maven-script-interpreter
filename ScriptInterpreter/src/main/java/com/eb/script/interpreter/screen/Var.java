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
    
    /**
     * Default constructor
     */
    public Var() {
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
                ", hasDisplay=" + (displayItem != null) +
                '}';
    }
}
