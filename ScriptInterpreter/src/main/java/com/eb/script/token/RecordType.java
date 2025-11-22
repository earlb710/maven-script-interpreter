package com.eb.script.token;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a record type definition with named fields and their types.
 * Used for structured data with type validation.
 * Supports nested records (records within records).
 * 
 * @author Earl Bosch
 */
public class RecordType {
    
    // Field definitions: field name -> field type (for primitive types)
    private final Map<String, DataType> fields;
    
    // Nested record definitions: field name -> nested RecordType (for record types)
    private final Map<String, RecordType> nestedRecords;
    
    /**
     * Create a new record type with no fields
     */
    public RecordType() {
        this.fields = new LinkedHashMap<>();
        this.nestedRecords = new LinkedHashMap<>();
    }
    
    /**
     * Create a new record type with specified fields
     * @param fields Map of field name to DataType
     */
    public RecordType(Map<String, DataType> fields) {
        this.fields = new LinkedHashMap<>(fields);
        this.nestedRecords = new LinkedHashMap<>();
    }
    
    /**
     * Add a field to this record type
     * @param name Field name
     * @param type Field data type
     */
    public void addField(String name, DataType type) {
        fields.put(name, type);
    }
    
    /**
     * Add a nested record field to this record type
     * @param name Field name
     * @param recordType Nested record type definition
     */
    public void addNestedRecord(String name, RecordType recordType) {
        nestedRecords.put(name, recordType);
        // Also mark in fields map that this is a RECORD type
        fields.put(name, DataType.RECORD);
    }
    
    /**
     * Get the type of a specific field
     * @param name Field name
     * @return DataType of the field, or null if field doesn't exist
     */
    public DataType getFieldType(String name) {
        return fields.get(name);
    }
    
    /**
     * Get the nested record type for a specific field
     * @param name Field name
     * @return RecordType of the nested record, or null if field doesn't exist or isn't a record
     */
    public RecordType getNestedRecordType(String name) {
        return nestedRecords.get(name);
    }
    
    /**
     * Check if this record has a field with the given name
     * @param name Field name
     * @return true if field exists
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    
    /**
     * Get all field definitions
     * @return Map of field names to types
     */
    public Map<String, DataType> getFields() {
        return new LinkedHashMap<>(fields);
    }
    
    /**
     * Validate that a value matches this record type structure
     * @param value The value to validate (should be a Map)
     * @return true if value is valid for this record type
     */
    public boolean validateValue(Object value) {
        if (value == null) {
            return true; // Allow null
        }
        
        if (!(value instanceof Map)) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) value;
        
        // Check that all fields in the record match the defined types
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            DataType expectedType = fields.get(fieldName);
            if (expectedType == null) {
                // Field not defined in record type - could allow or reject
                // For now, allow extra fields (flexible)
                continue;
            }
            
            // Check if this field is a nested record
            if (expectedType == DataType.RECORD && nestedRecords.containsKey(fieldName)) {
                RecordType nestedType = nestedRecords.get(fieldName);
                if (!nestedType.validateValue(fieldValue)) {
                    return false;
                }
            } else {
                if (!expectedType.isDataType(fieldValue)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Convert a value to match this record type's field types
     * @param value The value to convert (should be a Map)
     * @return Converted value with proper field types
     */
    public Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (!(value instanceof Map)) {
            return value;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) value;
        Map<String, Object> converted = new LinkedHashMap<>();
        
        // Convert each field to its declared type
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            DataType expectedType = fields.get(fieldName);
            if (expectedType != null) {
                // Check if this field is a nested record
                if (expectedType == DataType.RECORD && nestedRecords.containsKey(fieldName)) {
                    RecordType nestedType = nestedRecords.get(fieldName);
                    fieldValue = nestedType.convertValue(fieldValue);
                } else {
                    fieldValue = expectedType.convertValue(fieldValue);
                }
            }
            
            converted.put(fieldName, fieldValue);
        }
        
        return converted;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("record { ");
        boolean first = true;
        for (Map.Entry<String, DataType> entry : fields.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ");
            
            // If this field is a nested record, use its RecordType string representation
            if (entry.getValue() == DataType.RECORD && nestedRecords.containsKey(entry.getKey())) {
                sb.append(nestedRecords.get(entry.getKey()).toString());
            } else {
                sb.append(entry.getValue());
            }
            
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecordType)) return false;
        RecordType other = (RecordType) obj;
        return fields.equals(other.fields) && nestedRecords.equals(other.nestedRecords);
    }
    
    @Override
    public int hashCode() {
        return fields.hashCode() * 31 + nestedRecords.hashCode();
    }
}
