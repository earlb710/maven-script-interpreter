package com.eb.script.interpreter;

import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for type aliases defined with the typeof keyword.
 * Type aliases are global and accessible throughout the script.
 * 
 * @author Earl Bosch
 */
public class TypeRegistry {
    
    /**
     * Represents a type alias definition
     */
    public static class TypeAlias {
        public final String name;
        public final DataType dataType;
        public final RecordType recordType;
        public final boolean isArray;
        public final Integer arraySize; // null for dynamic arrays
        
        public TypeAlias(String name, DataType dataType, RecordType recordType, boolean isArray, Integer arraySize) {
            this.name = name;
            this.dataType = dataType;
            this.recordType = recordType;
            this.isArray = isArray;
            this.arraySize = arraySize;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isArray) {
                sb.append("array.");
                if (recordType != null) {
                    sb.append(recordType.toString());
                } else {
                    sb.append(dataType);
                }
                if (arraySize != null) {
                    sb.append("[").append(arraySize).append("]");
                } else {
                    sb.append("[*]");
                }
            } else if (recordType != null) {
                sb.append(recordType.toString());
            } else {
                sb.append(dataType);
            }
            return sb.toString();
        }
    }
    
    private static final Map<String, TypeAlias> typeAliases = new HashMap<>();
    
    /**
     * Register a type alias
     * @param alias The type alias to register
     */
    public static void registerTypeAlias(TypeAlias alias) {
        typeAliases.put(alias.name.toLowerCase(), alias);
    }
    
    /**
     * Look up a type alias by name
     * @param name The alias name (case-insensitive)
     * @return The TypeAlias, or null if not found
     */
    public static TypeAlias getTypeAlias(String name) {
        return typeAliases.get(name.toLowerCase());
    }
    
    /**
     * Check if a type alias exists
     * @param name The alias name (case-insensitive)
     * @return true if the alias exists
     */
    public static boolean hasTypeAlias(String name) {
        return typeAliases.containsKey(name.toLowerCase());
    }
    
    /**
     * Clear all type aliases (useful for testing)
     */
    public static void clear() {
        typeAliases.clear();
    }
}
