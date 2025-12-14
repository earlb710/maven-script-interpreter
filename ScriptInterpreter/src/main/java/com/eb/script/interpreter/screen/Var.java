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
    
    // Text case transformation: "upper", "lower", or "mixed" (optional)
    private String textCase;
    
    // Flag to indicate if this is an array type (e.g., array.record)
    private boolean isArrayType;
    
    // For array types, this holds the element type (e.g., RECORD for array.record)
    private DataType elementType;
    
    // For array.record, this holds the template record used to initialize new elements
    private Object recordTemplate;
    
    // Whether changes to this var should mark the screen/item as changed (default: true)
    // If false, changes don't affect dirty tracking
    private Boolean stateful = true;
    
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
    
    public String getTextCase() {
        return textCase;
    }
    
    public void setTextCase(String textCase) {
        this.textCase = textCase;
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
    
    /**
     * Check if this is an array type variable
     * @return true if array type, false otherwise
     */
    public boolean isArrayType() {
        return isArrayType;
    }
    
    /**
     * Set whether this is an array type variable
     * @param isArrayType true if array type
     */
    public void setArrayType(boolean isArrayType) {
        this.isArrayType = isArrayType;
    }
    
    /**
     * Get the element type for array variables
     * @return the element DataType, or null if not an array
     */
    public DataType getElementType() {
        return elementType;
    }
    
    /**
     * Set the element type for array variables
     * @param elementType the element DataType
     */
    public void setElementType(DataType elementType) {
        this.elementType = elementType;
    }
    
    /**
     * Get whether this variable is stateful (affects dirty tracking)
     * @return true if stateful (default), false if not
     */
    public Boolean getStateful() {
        return stateful;
    }
    
    /**
     * Set whether this variable is stateful (affects dirty tracking)
     * @param stateful true to enable dirty tracking, false to disable
     */
    public void setStateful(Boolean stateful) {
        this.stateful = stateful;
    }
    
    /**
     * Get the record template for array.record variables
     * @return the template record, or null if not array.record
     */
    public Object getRecordTemplate() {
        return recordTemplate;
    }
    
    /**
     * Set the record template for array.record variables
     * @param recordTemplate the template record
     */
    public void setRecordTemplate(Object recordTemplate) {
        this.recordTemplate = recordTemplate;
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
                ", isArrayType=" + isArrayType +
                (isArrayType ? ", elementType=" + elementType : "") +
                '}';
    }
}
