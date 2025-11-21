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
    
    // Original value (captured when screen is created or explicitly set)
    private Object originalValue;
    
    // Optional display metadata
    private DisplayItem displayItem;
    
    // Reference to the set this variable belongs to
    private String setName;
    
    // Minimum character length for input controls
    private Integer minChar;
    
    // Maximum character length for input controls
    private Integer maxChar;
    
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
        this();
        this.name = name;
    }
    
    /**
     * Constructor with name and type
     * @param name The variable name
     * @param type The variable data type
     */
    public Var(String name, DataType type) {
        this();
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
        this();
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.originalValue = defaultValue;  // Set original value to default initially
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
    
    public Object getOriginalValue() {
        return originalValue;
    }
    
    public void setOriginalValue(Object originalValue) {
        this.originalValue = originalValue;
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
    
    public Integer getMinChar() {
        return minChar;
    }
    
    public void setMinChar(Integer minChar) {
        this.minChar = minChar;
    }
    
    public Integer getMaxChar() {
        return maxChar;
    }
    
    public void setMaxChar(Integer maxChar) {
        this.maxChar = maxChar;
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
    
    /**
     * Check if the current value has changed from the original value
     * @return true if value has changed, false otherwise
     */
    public boolean hasChanged() {
        if (originalValue == null && value == null) {
            return false;
        }
        if (originalValue == null || value == null) {
            return true;
        }
        return !originalValue.equals(value);
    }
    
    /**
     * Get the status of this variable based on whether it has changed
     * @return "changed" if value != original, "clean" otherwise
     */
    public String getStatus() {
        return hasChanged() ? "changed" : "clean";
    }
    
    /**
     * Reset the original value to the current value (marking as clean)
     */
    public void resetOriginalValue() {
        this.originalValue = this.value;
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
