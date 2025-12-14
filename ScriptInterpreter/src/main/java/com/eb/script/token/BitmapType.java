package com.eb.script.token;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a bitmap type definition with named fields that map to bit ranges within a byte.
 * Each field occupies a contiguous range of bits (0-7) and can have up to 8 fields total.
 * Used for compact storage of multiple small values within a single byte.
 * 
 * Example: bitmap { status: 0-1, enabled: 2, priority: 3-5, reserved: 6-7 }
 * - status uses bits 0-1 (2 bits, values 0-3)
 * - enabled uses bit 2 (1 bit, values 0-1)
 * - priority uses bits 3-5 (3 bits, values 0-7)
 * - reserved uses bits 6-7 (2 bits, values 0-3)
 * 
 * @author Earl Bosch
 */
public class BitmapType implements Serializable {
    
    /**
     * Represents a field within a bitmap, defined by its bit range.
     */
    public static class BitField implements Serializable {
        private final String name;
        private final int startBit;  // Inclusive, 0-7
        private final int endBit;    // Inclusive, 0-7
        
        public BitField(String name, int startBit, int endBit) {
            if (startBit < 0 || startBit > 7) {
                throw new IllegalArgumentException("Start bit must be 0-7, got: " + startBit);
            }
            if (endBit < 0 || endBit > 7) {
                throw new IllegalArgumentException("End bit must be 0-7, got: " + endBit);
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
        public BitField(String name, int bit) {
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
        public int getMaxValue() {
            return (1 << getBitCount()) - 1;
        }
        
        /**
         * Get the bitmask for this field (before shifting)
         */
        public int getMask() {
            return getMaxValue() << startBit;
        }
        
        /**
         * Extract this field's value from a byte
         */
        public int getValue(byte data) {
            int unsignedByte = data & 0xFF;
            return (unsignedByte >> startBit) & getMaxValue();
        }
        
        /**
         * Set this field's value in a byte, returning the new byte value
         */
        public byte setValue(byte data, int value) {
            if (value < 0 || value > getMaxValue()) {
                throw new IllegalArgumentException("Value " + value + " out of range for field '" + name + 
                    "' (max " + getMaxValue() + ")");
            }
            int unsignedByte = data & 0xFF;
            // Clear the bits for this field
            unsignedByte &= ~getMask();
            // Set the new value
            unsignedByte |= (value << startBit);
            return (byte) unsignedByte;
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
            if (!(obj instanceof BitField)) return false;
            BitField other = (BitField) obj;
            return name.equals(other.name) && startBit == other.startBit && endBit == other.endBit;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode() * 31 + startBit * 17 + endBit;
        }
    }
    
    // Field definitions: field name -> BitField
    private final Map<String, BitField> fields;
    
    /**
     * Create a new bitmap type with no fields
     */
    public BitmapType() {
        this.fields = new LinkedHashMap<>();
    }
    
    /**
     * Add a field with a bit range to this bitmap type
     * @param name Field name
     * @param startBit Starting bit position (0-7, inclusive)
     * @param endBit Ending bit position (0-7, inclusive)
     */
    public void addField(String name, int startBit, int endBit) {
        // Check for overlapping fields
        for (BitField existing : fields.values()) {
            if (rangesOverlap(startBit, endBit, existing.getStartBit(), existing.getEndBit())) {
                throw new IllegalArgumentException("Field '" + name + "' (" + startBit + "-" + endBit + 
                    ") overlaps with existing field '" + existing.getName() + "' (" + 
                    existing.getStartBit() + "-" + existing.getEndBit() + ")");
            }
        }
        fields.put(name, new BitField(name, startBit, endBit));
    }
    
    /**
     * Add a single-bit field to this bitmap type
     * @param name Field name
     * @param bit Bit position (0-7)
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
     * @return BitField definition, or null if not found
     */
    public BitField getField(String name) {
        return fields.get(name);
    }
    
    /**
     * Get field with case-insensitive lookup
     */
    public BitField getFieldIgnoreCase(String name) {
        // Try exact match first
        if (fields.containsKey(name)) {
            return fields.get(name);
        }
        // Try case-insensitive match
        for (Map.Entry<String, BitField> entry : fields.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Check if this bitmap has a field with the given name
     * @param name Field name
     * @return true if field exists
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    
    /**
     * Get all field definitions
     * @return Map of field names to BitField definitions
     */
    public Map<String, BitField> getFields() {
        return new LinkedHashMap<>(fields);
    }
    
    /**
     * Get the number of fields
     */
    public int getFieldCount() {
        return fields.size();
    }
    
    /**
     * Extract a field value from a byte
     * @param data The byte containing the bitmap data
     * @param fieldName The name of the field to extract
     * @return The field value, right-shifted to start at bit 0
     */
    public int getValue(byte data, String fieldName) {
        BitField field = getFieldIgnoreCase(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
        return field.getValue(data);
    }
    
    /**
     * Set a field value in a byte
     * @param data The byte containing the bitmap data
     * @param fieldName The name of the field to set
     * @param value The value to set (must fit in the field's bit range)
     * @return The new byte value with the field set
     */
    public byte setValue(byte data, String fieldName, int value) {
        BitField field = getFieldIgnoreCase(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
        return field.setValue(data, value);
    }
    
    /**
     * Convert an Object value to a byte for bitmap operations.
     * Supports Byte, Number types, and other Objects by converting to byte.
     * @param value The value to convert
     * @return The byte value
     * @throws IllegalArgumentException if the value cannot be converted
     */
    public static byte toByteValue(Object value) {
        if (value instanceof Byte) {
            return (Byte) value;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else if (value == null) {
            return 0;
        } else {
            throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to byte for bitmap");
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("bitmap {");
        boolean first = true;
        for (BitField field : fields.values()) {
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
        if (!(obj instanceof BitmapType)) return false;
        BitmapType other = (BitmapType) obj;
        return fields.equals(other.fields);
    }
    
    @Override
    public int hashCode() {
        return fields.hashCode();
    }
}
