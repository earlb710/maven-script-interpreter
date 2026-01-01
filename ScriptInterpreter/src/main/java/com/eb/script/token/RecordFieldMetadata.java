package com.eb.script.token;

import java.io.Serializable;

/**
 * Metadata for a record field, including constraints and default values.
 * Used to define field-level validation rules and initialization behavior.
 * 
 * @author Earl Bosch
 */
public class RecordFieldMetadata implements Serializable {
    
    private final String fieldName;
    private final DataType fieldType;
    private final boolean mandatory;      // Field cannot be null (not null constraint)
    private final Integer maxLength;      // Maximum length for string fields (null = no limit)
    private final Object defaultValue;    // Default value if field is not provided
    
    /**
     * Create field metadata with all properties
     * 
     * @param fieldName Name of the field
     * @param fieldType Data type of the field
     * @param mandatory Whether the field is required (not null)
     * @param maxLength Maximum length for string fields (null for no limit)
     * @param defaultValue Default value for the field (null for no default)
     */
    public RecordFieldMetadata(String fieldName, DataType fieldType, boolean mandatory, 
                               Integer maxLength, Object defaultValue) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.mandatory = mandatory;
        this.maxLength = maxLength;
        this.defaultValue = defaultValue;
    }
    
    /**
     * Create field metadata with only type (no constraints)
     * 
     * @param fieldName Name of the field
     * @param fieldType Data type of the field
     */
    public RecordFieldMetadata(String fieldName, DataType fieldType) {
        this(fieldName, fieldType, false, null, null);
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public DataType getFieldType() {
        return fieldType;
    }
    
    public boolean isMandatory() {
        return mandatory;
    }
    
    public Integer getMaxLength() {
        return maxLength;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
    
    public boolean hasMaxLength() {
        return maxLength != null;
    }
    
    /**
     * Validate a field value against this metadata
     * 
     * @param value The value to validate
     * @return true if valid, false otherwise
     */
    public boolean validate(Object value) {
        // Check mandatory constraint
        if (mandatory && value == null) {
            System.err.println("Error: Field '" + fieldName + "' is mandatory and cannot be null");
            return false;
        }
        
        // If value is null and not mandatory, it's valid
        if (value == null) {
            return true;
        }
        
        // Check type
        if (!fieldType.isDataType(value)) {
            System.err.println("Error: Field '" + fieldName + "' has invalid type. Expected " + 
                             fieldType + ", got " + value.getClass().getSimpleName());
            return false;
        }
        
        // Check max length for strings
        if (hasMaxLength() && fieldType == DataType.STRING && value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() > maxLength) {
                System.err.println("Error: Field '" + fieldName + "' exceeds maximum length of " + 
                                 maxLength + " (actual: " + strValue.length() + ")");
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(fieldName);
        sb.append(": ").append(getTypeName(fieldType));
        
        if (mandatory) {
            sb.append(" (mandatory)");
        }
        if (hasMaxLength()) {
            sb.append(" (max length: ").append(maxLength).append(")");
        }
        if (hasDefaultValue()) {
            sb.append(" (default: ").append(defaultValue).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Get a lowercase type name for display
     */
    private String getTypeName(DataType type) {
        switch (type) {
            case BYTE: return "byte";
            case INTEGER: return "int";
            case LONG: return "long";
            case FLOAT: return "float";
            case DOUBLE: return "double";
            case STRING: return "string";
            case DATE: return "date";
            case BOOL: return "bool";
            case JSON: return "json";
            case ARRAY: return "array";
            case RECORD: return "record";
            case MAP: return "map";
            default: return type.toString().toLowerCase();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecordFieldMetadata)) return false;
        RecordFieldMetadata other = (RecordFieldMetadata) obj;
        return fieldName.equals(other.fieldName) &&
               fieldType.equals(other.fieldType) &&
               mandatory == other.mandatory &&
               (maxLength == null ? other.maxLength == null : maxLength.equals(other.maxLength)) &&
               (defaultValue == null ? other.defaultValue == null : defaultValue.equals(other.defaultValue));
    }
    
    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + fieldType.hashCode();
        result = 31 * result + (mandatory ? 1 : 0);
        result = 31 * result + (maxLength != null ? maxLength.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        return result;
    }
}
