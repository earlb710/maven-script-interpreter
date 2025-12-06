package com.eb.script.token;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an intmap type definition with named fields that map to bit ranges within a 32-bit integer.
 * Each field occupies a contiguous range of bits (0-31) and can have up to 32 fields total.
 * Used for compact storage of multiple small values within a single integer.
 * 
 * Example: intmap { status: 0-1, enabled: 2, priority: 3-7, flags: 8-15, reserved: 16-31 }
 * - status uses bits 0-1 (2 bits, values 0-3)
 * - enabled uses bit 2 (1 bit, values 0-1)
 * - priority uses bits 3-7 (5 bits, values 0-31)
 * - flags uses bits 8-15 (8 bits, values 0-255)
 * - reserved uses bits 16-31 (16 bits, values 0-65535)
 * 
 * @author Earl Bosch
 */
public class IntmapType {
    
    /**
     * Represents a field within an intmap, defined by its bit range.
     */
    public static class IntField {
        private final String name;
        private final int startBit;  // Inclusive, 0-31
        private final int endBit;    // Inclusive, 0-31
        
        public IntField(String name, int startBit, int endBit) {
            if (startBit < 0 || startBit > 31) {
                throw new IllegalArgumentException("Start bit must be 0-31, got: " + startBit);
            }
            if (endBit < 0 || endBit > 31) {
                throw new IllegalArgumentException("End bit must be 0-31, got: " + endBit);
            }
            if (startBit > endBit) {
                throw new IllegalArgumentException("Start bit (" + startBit + ") must be <= end bit (" + endBit + ")");
            }
            this.name = name;
            this.startBit = startBit;
            this.endBit = endBit;
        }
        
        /**
         * Create a single-bit field
         */
        public IntField(String name, int bit) {
            this(name, bit, bit);
        }
        
        public String getName() {
            return name;
        }
        
        public int getStartBit() {
            return startBit;
        }
        
        public int getEndBit() {
            return endBit;
        }
        
        /**
         * Get the number of bits in this field
         */
        public int getBitCount() {
            return endBit - startBit + 1;
        }
        
        /**
         * Get the maximum value this field can hold
         */
        public long getMaxValue() {
            return (1L << getBitCount()) - 1;
        }
        
        /**
         * Get the bitmask for this field (before shifting)
         */
        public int getMask() {
            int bitCount = getBitCount();
            if (bitCount == 32) {
                return 0xFFFFFFFF;
            }
            return ((1 << bitCount) - 1) << startBit;
        }
        
        /**
         * Extract this field's value from an integer
         */
        public int getValue(int data) {
            return (data >>> startBit) & (int)getMaxValue();
        }
        
        /**
         * Set this field's value in an integer, returning the new integer value
         */
        public int setValue(int data, int value) {
            long maxVal = getMaxValue();
            if (value < 0 || value > maxVal) {
                throw new IllegalArgumentException("Value " + value + " out of range for field '" + name + 
                    "' (max " + maxVal + ")");
            }
            // Clear the bits for this field
            data &= ~getMask();
            // Set the new value
            data |= (value << startBit);
            return data;
        }
        
        @Override
        public String toString() {
            if (startBit == endBit) {
                return name + ": " + startBit;
            } else {
                return name + ": " + startBit + "-" + endBit;
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IntField)) return false;
            IntField other = (IntField) obj;
            return name.equals(other.name) && startBit == other.startBit && endBit == other.endBit;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode() * 31 + startBit * 17 + endBit;
        }
    }
    
    // Field definitions: field name -> IntField
    private final Map<String, IntField> fields;
    
    /**
     * Create a new intmap type with no fields
     */
    public IntmapType() {
        this.fields = new LinkedHashMap<>();
    }
    
    /**
     * Add a field with a bit range to this intmap type
     * @param name Field name
     * @param startBit Starting bit position (0-31, inclusive)
     * @param endBit Ending bit position (0-31, inclusive)
     */
    public void addField(String name, int startBit, int endBit) {
        // Check for overlapping fields
        for (IntField existing : fields.values()) {
            if (rangesOverlap(startBit, endBit, existing.getStartBit(), existing.getEndBit())) {
                throw new IllegalArgumentException("Field '" + name + "' (" + startBit + "-" + endBit + 
                    ") overlaps with existing field '" + existing.getName() + "' (" + 
                    existing.getStartBit() + "-" + existing.getEndBit() + ")");
            }
        }
        fields.put(name, new IntField(name, startBit, endBit));
    }
    
    /**
     * Add a single-bit field to this intmap type
     * @param name Field name
     * @param bit Bit position (0-31)
     */
    public void addField(String name, int bit) {
        addField(name, bit, bit);
    }
    
    /**
     * Check if two bit ranges overlap
     */
    private boolean rangesOverlap(int start1, int end1, int start2, int end2) {
        return start1 <= end2 && start2 <= end1;
    }
    
    /**
     * Get a field definition by name
     * @param name Field name
     * @return IntField definition, or null if not found
     */
    public IntField getField(String name) {
        return fields.get(name);
    }
    
    /**
     * Get field with case-insensitive lookup
     */
    public IntField getFieldIgnoreCase(String name) {
        // Try exact match first
        if (fields.containsKey(name)) {
            return fields.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, IntField> entry : fields.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Check if this intmap has a field with the given name
     * @param name Field name
     * @return true if field exists
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    
    /**
     * Get all field definitions
     * @return Map of field names to IntField definitions
     */
    public Map<String, IntField> getFields() {
        return new LinkedHashMap<>(fields);
    }
    
    /**
     * Get the number of fields
     */
    public int getFieldCount() {
        return fields.size();
    }
    
    /**
     * Extract a field value from an integer
     * @param data The integer containing the intmap data
     * @param fieldName The name of the field to extract
     * @return The field value, right-shifted to start at bit 0
     */
    public int getValue(int data, String fieldName) {
        IntField field = getFieldIgnoreCase(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
        return field.getValue(data);
    }
    
    /**
     * Set a field value in an integer
     * @param data The integer containing the intmap data
     * @param fieldName The name of the field to set
     * @param value The value to set (must fit in the field's bit range)
     * @return The new integer value with the field set
     */
    public int setValue(int data, String fieldName, int value) {
        IntField field = getFieldIgnoreCase(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
        return field.setValue(data, value);
    }
    
    /**
     * Convert an Object value to an int for intmap operations.
     * Supports Integer, Number types, and other Objects by converting to int.
     * @param value The value to convert
     * @return The int value
     * @throws IllegalArgumentException if the value cannot be converted
     */
    public static int toIntValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value == null) {
            return 0;
        } else {
            throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to int for intmap");
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("intmap {");
        boolean first = true;
        for (IntField field : fields.values()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(field.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IntmapType)) return false;
        IntmapType other = (IntmapType) obj;
        return fields.equals(other.fields);
    }
    
    @Override
    public int hashCode() {
        return fields.hashCode();
    }
}
