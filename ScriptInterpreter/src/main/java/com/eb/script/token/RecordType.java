package com.eb.script.token;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a record type definition with named fields and their types.
 * Used for structured data with type validation.
 * Supports nested records (records within records).
 * 
 * @author Earl Bosch
 */
public class RecordType implements Serializable {
    
    // Field definitions: field name -> field type (for primitive types)
    private final Map<String, DataType> fields;
    
    // Field metadata: field name -> RecordFieldMetadata (constraints, defaults, etc.)
    private final Map<String, RecordFieldMetadata> fieldMetadata;
    
    // Nested record definitions: field name -> nested RecordType (for record types)
    private final Map<String, RecordType> nestedRecords;
    
    /**
     * Create a new record type with no fields
     */
    public RecordType() {
        this.fields = new LinkedHashMap<>();
        this.fieldMetadata = new LinkedHashMap<>();
        this.nestedRecords = new LinkedHashMap<>();
    }
    
    /**
     * Create a new record type with specified fields
     * @param fields Map of field name to DataType
     */
    public RecordType(Map<String, DataType> fields) {
        this.fields = new LinkedHashMap<>(fields);
        this.fieldMetadata = new LinkedHashMap<>();
        this.nestedRecords = new LinkedHashMap<>();
    }
    
    /**
     * Add a field to this record type
     * @param name Field name
     * @param type Field data type
     */
    public void addField(String name, DataType type) {
        fields.put(name, type);
        // Create default metadata if not already set
        if (!fieldMetadata.containsKey(name)) {
            fieldMetadata.put(name, new RecordFieldMetadata(name, type));
        }
    }
    
    /**
     * Add a field to this record type with metadata
     * @param name Field name
     * @param type Field data type
     * @param mandatory Whether field is required (not null)
     * @param maxLength Maximum length for string fields (null for no limit)
     * @param defaultValue Default value for the field (null for no default)
     */
    public void addField(String name, DataType type, boolean mandatory, Integer maxLength, Object defaultValue) {
        fields.put(name, type);
        fieldMetadata.put(name, new RecordFieldMetadata(name, type, mandatory, maxLength, defaultValue));
    }
    
    /**
     * Add a field to this record type with metadata object
     * @param metadata Field metadata containing all field properties
     */
    public void addField(RecordFieldMetadata metadata) {
        fields.put(metadata.getFieldName(), metadata.getFieldType());
        fieldMetadata.put(metadata.getFieldName(), metadata);
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
        // Create default metadata for nested record
        if (!fieldMetadata.containsKey(name)) {
            fieldMetadata.put(name, new RecordFieldMetadata(name, DataType.RECORD));
        }
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
     * Get field metadata for a specific field
     * @param name Field name
     * @return RecordFieldMetadata for the field, or null if field doesn't exist
     */
    public RecordFieldMetadata getFieldMetadata(String name) {
        return fieldMetadata.get(name);
    }
    
    /**
     * Get all field metadata
     * @return Map of field names to metadata
     */
    public Map<String, RecordFieldMetadata> getAllFieldMetadata() {
        return new LinkedHashMap<>(fieldMetadata);
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
        
        // Create a case-insensitive set of JSON field names for O(1) lookup
        Set<String> jsonFieldsLowerCase = new HashSet<>();
        for (String jsonField : record.keySet()) {
            jsonFieldsLowerCase.add(jsonField.toLowerCase());
        }
        
        // Check all defined fields against their metadata
        for (Map.Entry<String, RecordFieldMetadata> entry : fieldMetadata.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldMetadata metadata = entry.getValue();
            
            // Check if field exists in the record (case-insensitive)
            Object fieldValue = getFieldValueIgnoreCase(record, fieldName);
            boolean fieldExists = jsonFieldsLowerCase.contains(fieldName.toLowerCase());
            
            // If field doesn't exist and has no default, check if it's mandatory
            if (!fieldExists && !metadata.hasDefaultValue()) {
                if (metadata.isMandatory()) {
                    System.err.println("Error: Mandatory field '" + fieldName + "' is missing from record");
                    return false;
                }
                // Non-mandatory field without default - skip validation
                continue;
            }
            
            // If field exists, validate it using metadata
            if (fieldExists) {
                // Check if this field is a nested record
                if (metadata.getFieldType() == DataType.RECORD) {
                    RecordType nestedType = getNestedRecordTypeIgnoreCase(fieldName);
                    if (nestedType != null && !nestedType.validateValue(fieldValue)) {
                        return false;
                    }
                } else {
                    // Use metadata validation which checks type, mandatory, and max length
                    if (!metadata.validate(fieldValue)) {
                        return false;
                    }
                }
            }
        }
        
        // Check that all fields in the record are declared
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            
            // Use case-insensitive lookup for field types
            DataType expectedType = getFieldTypeIgnoreCase(fieldName);
            if (expectedType == null) {
                // Field not defined in record type - reject undeclared fields
                System.err.println("Error: Field '" + fieldName + "' is not declared in record type");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get field type with case-insensitive lookup
     */
    private DataType getFieldTypeIgnoreCase(String name) {
        // Try exact match first
        if (fields.containsKey(name)) {
            return fields.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, DataType> entry : fields.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Get field value with case-insensitive lookup
     */
    private Object getFieldValueIgnoreCase(Map<String, Object> record, String name) {
        // Try exact match first
        if (record.containsKey(name)) {
            return record.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Get field metadata with case-insensitive lookup
     */
    private RecordFieldMetadata getFieldMetadataIgnoreCase(String name) {
        // Try exact match first
        if (fieldMetadata.containsKey(name)) {
            return fieldMetadata.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, RecordFieldMetadata> entry : fieldMetadata.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Get nested record type with case-insensitive lookup
     */
    private RecordType getNestedRecordTypeIgnoreCase(String name) {
        // Try exact match first
        if (nestedRecords.containsKey(name)) {
            return nestedRecords.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, RecordType> entry : nestedRecords.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Convert a value to match this record type's field types and apply defaults
     * @param value The value to convert (should be a Map)
     * @return Converted value with proper field types and default values applied
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
        
        // First, apply default values for missing fields
        for (Map.Entry<String, RecordFieldMetadata> entry : fieldMetadata.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldMetadata metadata = entry.getValue();
            
            // Check if field is missing in the input
            Object fieldValue = getFieldValueIgnoreCase(record, fieldName);
            if (fieldValue == null && metadata.hasDefaultValue()) {
                // Apply default value
                converted.put(fieldName, metadata.getDefaultValue());
            }
        }
        
        // Convert each field to its declared type
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            // Use case-insensitive lookup for field types
            DataType expectedType = getFieldTypeIgnoreCase(fieldName);
            if (expectedType == null) {
                // Field not declared - skip undeclared fields
                // Validation will catch this error
                continue;
            }
            
            // Check if this field is a nested record
            if (expectedType == DataType.RECORD) {
                RecordType nestedType = getNestedRecordTypeIgnoreCase(fieldName);
                if (nestedType != null) {
                    fieldValue = nestedType.convertValue(fieldValue);
                }
            } else {
                fieldValue = expectedType.convertValue(fieldValue);
            }
            
            converted.put(fieldName, fieldValue);
        }
        
        return converted;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("record {");
        boolean first = true;
        for (Map.Entry<String, DataType> entry : fields.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(":");
            
            // If this field is a nested record, use its RecordType string representation
            if (entry.getValue() == DataType.RECORD && nestedRecords.containsKey(entry.getKey())) {
                sb.append(nestedRecords.get(entry.getKey()).toString());
            } else {
                sb.append(getTypeName(entry.getValue()));
            }
            
            first = false;
        }
        sb.append("}");
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
        if (!(obj instanceof RecordType)) return false;
        RecordType other = (RecordType) obj;
        return fields.equals(other.fields) && nestedRecords.equals(other.nestedRecords);
    }
    
    @Override
    public int hashCode() {
        return fields.hashCode() * 31 + nestedRecords.hashCode();
    }
}
