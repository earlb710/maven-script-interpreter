package com.eb.script.interpreter.screen.data;

import com.eb.script.arrays.ArrayDef;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles complex variable reference resolution for screen data binding.
 * Supports simple variable names and complex expressions like "clients[0].clientName".
 * 
 * This class is part of the DATA layer and has no JavaFX dependencies.
 * 
 * @author Earl Bosch
 */
public class VarRefResolver {

    /**
     * Resolves a varRef value, handling both simple variable names and complex
     * expressions with array element access like "clients[0].clientName".
     * 
     * @param varRef The variable reference to resolve
     * @param screenVars The screen variables map
     * @return The resolved value, or null if not found
     */
    public static Object resolveVarRefValue(String varRef, 
            ConcurrentHashMap<String, Object> screenVars) {
        if (varRef == null || screenVars == null) {
            return null;
        }
        
        // Check if this is a simple variable name (no array access or property access)
        if (!varRef.contains("[") && !varRef.contains(".")) {
            return screenVars.get(varRef.toLowerCase());
        }
        
        // Handle complex expressions like "clients[0].clientName"
        // Extract the base variable name (everything before the first '[' or '.')
        int bracketPos = varRef.indexOf('[');
        int dotPos = varRef.indexOf('.');
        int splitPos;
        
        if (bracketPos >= 0 && dotPos >= 0) {
            splitPos = Math.min(bracketPos, dotPos);
        } else if (bracketPos >= 0) {
            splitPos = bracketPos;
        } else if (dotPos >= 0) {
            splitPos = dotPos;
        } else {
            // No complex access, just a simple variable
            return screenVars.get(varRef.toLowerCase());
        }
        
        String baseVarName = varRef.substring(0, splitPos).toLowerCase();
        String path = varRef.substring(splitPos);
        
        // Get the base variable from screenVars
        Object baseValue = screenVars.get(baseVarName);
        if (baseValue == null) {
            return null;
        }
        
        // Use case-insensitive navigation through the path
        return navigatePathCaseInsensitive(baseValue, path);
    }

    /**
     * Navigates a path like "[0].clientName" through an object/array structure
     * with case-insensitive property name matching.
     * 
     * @param root The root object to navigate from
     * @param path The path to navigate
     * @return The value at the path, or null if not found
     */
    public static Object navigatePathCaseInsensitive(Object root, String path) {
        if (path == null || path.isEmpty() || root == null) {
            return root;
        }
        
        Object current = root;
        int i = 0;
        int n = path.length();
        
        while (i < n && current != null) {
            char c = path.charAt(i);
            
            if (c == '.') {
                i++;
                continue;
            }
            
            if (c == '[') {
                // Array index access
                i++; // skip '['
                int start = i;
                while (i < n && path.charAt(i) != ']') {
                    i++;
                }
                String indexStr = path.substring(start, i);
                i++; // skip ']'
                
                try {
                    int index = Integer.parseInt(indexStr);
                    if (current instanceof ArrayDef) {
                        ArrayDef<?, ?> arr = (ArrayDef<?, ?>) current;
                        if (index >= 0 && index < arr.size()) {
                            current = arr.get(index);
                        } else {
                            return null;
                        }
                    } else if (current instanceof List) {
                        List<?> list = (List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // Property name
                int start = i;
                while (i < n && path.charAt(i) != '.' && path.charAt(i) != '[') {
                    i++;
                }
                String propName = path.substring(start, i);
                
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    // Case-insensitive key lookup
                    current = getMapValueCaseInsensitive(map, propName);
                } else {
                    return null;
                }
            }
        }
        
        return current;
    }

    /**
     * Gets a value from a map using case-insensitive key matching.
     * 
     * @param map The map to search
     * @param key The key to find (case-insensitive)
     * @return The value, or null if not found
     */
    public static Object getMapValueCaseInsensitive(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        
        // First try exact match
        if (map.containsKey(key)) {
            return map.get(key);
        }
        
        // Try case-insensitive match
        String lowerKey = key.toLowerCase();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase().equals(lowerKey)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Sets a value for a varRef, handling both simple variable names and complex
     * expressions with array element access like "clients[0].clientName".
     * 
     * @param varRef The variable reference to set
     * @param value The value to set
     * @param screenVars The screen variables map
     */
    public static void setVarRefValue(String varRef, Object value,
            ConcurrentHashMap<String, Object> screenVars) {
        if (varRef == null || screenVars == null) {
            return;
        }
        
        // Check if this is a simple variable name (no array access or property access)
        if (!varRef.contains("[") && !varRef.contains(".")) {
            putOrRemoveIfNull(screenVars, varRef, value);
            return;
        }
        
        // Handle complex expressions like "clients[0].clientName"
        // Extract the base variable name (everything before the first '[' or '.')
        int bracketPos = varRef.indexOf('[');
        int dotPos = varRef.indexOf('.');
        int splitPos;
        
        if (bracketPos >= 0 && dotPos >= 0) {
            splitPos = Math.min(bracketPos, dotPos);
        } else if (bracketPos >= 0) {
            splitPos = bracketPos;
        } else if (dotPos >= 0) {
            splitPos = dotPos;
        } else {
            // No complex access, just a simple variable
            putOrRemoveIfNull(screenVars, varRef, value);
            return;
        }
        
        String baseVarName = varRef.substring(0, splitPos).toLowerCase();
        String path = varRef.substring(splitPos);
        
        // Get the base variable from screenVars
        Object baseValue = screenVars.get(baseVarName);
        if (baseValue == null) {
            return;
        }
        
        // Use case-insensitive navigation to set the value
        setPathValueCaseInsensitive(baseValue, path, value);
    }

    /**
     * Sets a value at a path like "[0].clientName" through an object/array structure
     * with case-insensitive property name matching.
     * 
     * @param root The root object to navigate from
     * @param path The path to the value
     * @param value The value to set
     */
    public static void setPathValueCaseInsensitive(Object root, String path, Object value) {
        if (path == null || path.isEmpty() || root == null) {
            return;
        }
        
        Object current = root;
        int i = 0;
        int n = path.length();
        String lastPropName = null;
        Object lastContainer = null;
        int lastArrayIndex = -1;
        
        while (i < n && current != null) {
            char c = path.charAt(i);
            
            if (c == '.') {
                i++;
                continue;
            }
            
            if (c == '[') {
                // Array index access
                i++; // skip '['
                int start = i;
                while (i < n && path.charAt(i) != ']') {
                    i++;
                }
                String indexStr = path.substring(start, i);
                i++; // skip ']'
                
                try {
                    int index = Integer.parseInt(indexStr);
                    
                    // Check if this is the last segment
                    if (i >= n || (path.charAt(i) == '.' && isLastPropertySegment(path, i + 1))) {
                        // This might be the parent - save for potential update
                        lastContainer = current;
                        lastArrayIndex = index;
                        lastPropName = null;
                    }
                    
                    if (current instanceof ArrayDef) {
                        ArrayDef<?, ?> arr = (ArrayDef<?, ?>) current;
                        if (index >= 0 && index < arr.size()) {
                            current = arr.get(index);
                        } else {
                            return;
                        }
                    } else if (current instanceof List) {
                        List<?> list = (List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                } catch (NumberFormatException e) {
                    return;
                }
            } else {
                // Property name
                int start = i;
                while (i < n && path.charAt(i) != '.' && path.charAt(i) != '[') {
                    i++;
                }
                String propName = path.substring(start, i);
                
                // Check if this is the last segment
                if (i >= n) {
                    // This is the final property to set
                    if (current instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) current;
                        setMapValueCaseInsensitive(map, propName, value);
                    }
                    return;
                }
                
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    lastContainer = map;
                    lastPropName = propName;
                    lastArrayIndex = -1;
                    current = getMapValueCaseInsensitive(map, propName);
                } else {
                    return;
                }
            }
        }
    }

    /**
     * Checks if the remaining path is just a single property name (the last segment).
     * 
     * @param path The path string
     * @param start The starting position to check from
     * @return true if this is the last property segment
     */
    public static boolean isLastPropertySegment(String path, int start) {
        int n = path.length();
        for (int i = start; i < n; i++) {
            char c = path.charAt(i);
            if (c == '.' || c == '[') {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets a value in a map using case-insensitive key matching.
     * If key exists (any case), updates that key. Otherwise, adds with the provided key.
     * 
     * @param map The map to update
     * @param key The key to set (case-insensitive matching)
     * @param value The value to set
     */
    public static void setMapValueCaseInsensitive(Map<String, Object> map, String key, Object value) {
        if (map == null || key == null) {
            return;
        }
        
        // First try exact match
        if (map.containsKey(key)) {
            map.put(key, value);
            return;
        }
        
        // Try case-insensitive match
        String lowerKey = key.toLowerCase();
        for (String existingKey : map.keySet()) {
            if (existingKey != null && existingKey.toLowerCase().equals(lowerKey)) {
                map.put(existingKey, value);
                return;
            }
        }
        
        // No existing key found, add with provided key
        map.put(key, value);
    }
    
    /**
     * Helper method to put a value in a ConcurrentHashMap or remove the key if value is null.
     * ConcurrentHashMap doesn't allow null values, so this handles the null case by removing the key.
     * 
     * @param map The ConcurrentHashMap
     * @param key The key (will be lowercased)
     * @param value The value to store, or null to remove the key
     */
    private static void putOrRemoveIfNull(ConcurrentHashMap<String, Object> map, String key, Object value) {
        String lowerKey = key.toLowerCase();
        if (value != null) {
            map.put(lowerKey, value);
        } else {
            map.remove(lowerKey);
        }
    }
}
