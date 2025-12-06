package com.eb.script.interpreter;

import com.eb.script.token.BitmapType;
import com.eb.script.token.IntmapType;
import com.eb.script.token.RecordType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe storage for environment values (variables) and record type metadata.
 * Uses ConcurrentHashMap for thread-safe access from screen threads.
 *
 * @author Earl Bosch
 */
public class EnvironmentValues {

    final Map<String, Object> values = new ConcurrentHashMap<>();
    final Map<String, RecordType> recordTypes = new ConcurrentHashMap<>(); // Store record type metadata
    final Map<String, BitmapType> bitmapTypes = new ConcurrentHashMap<>(); // Store bitmap type metadata
    final Map<String, String> bitmapTypeAliasNames = new ConcurrentHashMap<>(); // Store bitmap type alias names
    final Map<String, IntmapType> intmapTypes = new ConcurrentHashMap<>(); // Store intmap type metadata
    final Map<String, String> intmapTypeAliasNames = new ConcurrentHashMap<>(); // Store intmap type alias names
    final Set<String> constants = ConcurrentHashMap.newKeySet(); // Track constant variables
    final EnvironmentValues enclosing;

    public EnvironmentValues() {
        this.enclosing = null;
    }

    public EnvironmentValues(EnvironmentValues enclosing) {
        this.enclosing = enclosing;
    }

    void clear() {
        values.clear();
        recordTypes.clear();
        bitmapTypes.clear();
        bitmapTypeAliasNames.clear();
        intmapTypes.clear();
        intmapTypeAliasNames.clear();
        constants.clear();
    }
    
    /**
     * Safely convert null values to empty JSON objects (LinkedHashMap).
     * ConcurrentHashMap doesn't accept null values, so we replace null with empty map.
     */
    private Object safeValue(Object value) {
        return value != null ? value : new LinkedHashMap<String, Object>();
    }

    public void define(String name, Object value) {
        values.put(name, safeValue(value));
    }
    
    public void defineConst(String name, Object value) {
        values.put(name, safeValue(value));
        constants.add(name);
    }
    
    public void defineWithRecordType(String name, Object value, RecordType recordType) {
        values.put(name, safeValue(value));
        if (recordType != null) {
            recordTypes.put(name, recordType);
        }
    }
    
    public void defineConstWithRecordType(String name, Object value, RecordType recordType) {
        values.put(name, safeValue(value));
        constants.add(name);
        if (recordType != null) {
            recordTypes.put(name, recordType);
        }
    }
    
    public void defineWithBitmapType(String name, Object value, BitmapType bitmapType) {
        defineWithBitmapType(name, value, bitmapType, null);
    }
    
    public void defineWithBitmapType(String name, Object value, BitmapType bitmapType, String typeAliasName) {
        values.put(name, safeValue(value));
        if (bitmapType != null) {
            bitmapTypes.put(name, bitmapType);
            if (typeAliasName != null) {
                bitmapTypeAliasNames.put(name, typeAliasName);
            }
        }
    }
    
    public void defineConstWithBitmapType(String name, Object value, BitmapType bitmapType) {
        defineConstWithBitmapType(name, value, bitmapType, null);
    }
    
    public void defineConstWithBitmapType(String name, Object value, BitmapType bitmapType, String typeAliasName) {
        values.put(name, safeValue(value));
        constants.add(name);
        if (bitmapType != null) {
            bitmapTypes.put(name, bitmapType);
            if (typeAliasName != null) {
                bitmapTypeAliasNames.put(name, typeAliasName);
            }
        }
    }
    
    public RecordType getRecordType(String name) {
        if (recordTypes.containsKey(name)) {
            return recordTypes.get(name);
        }
        if (enclosing != null) {
            return enclosing.getRecordType(name);
        }
        return null;
    }
    
    public BitmapType getBitmapType(String name) {
        if (bitmapTypes.containsKey(name)) {
            return bitmapTypes.get(name);
        }
        if (enclosing != null) {
            return enclosing.getBitmapType(name);
        }
        return null;
    }
    
    public String getBitmapTypeAliasName(String name) {
        if (bitmapTypeAliasNames.containsKey(name)) {
            return bitmapTypeAliasNames.get(name);
        }
        if (enclosing != null) {
            return enclosing.getBitmapTypeAliasName(name);
        }
        return null;
    }
    
    public void defineWithIntmapType(String name, Object value, IntmapType intmapType) {
        defineWithIntmapType(name, value, intmapType, null);
    }
    
    public void defineWithIntmapType(String name, Object value, IntmapType intmapType, String typeAliasName) {
        values.put(name, safeValue(value));
        if (intmapType != null) {
            intmapTypes.put(name, intmapType);
            if (typeAliasName != null) {
                intmapTypeAliasNames.put(name, typeAliasName);
            }
        }
    }
    
    public void defineConstWithIntmapType(String name, Object value, IntmapType intmapType) {
        defineConstWithIntmapType(name, value, intmapType, null);
    }
    
    public void defineConstWithIntmapType(String name, Object value, IntmapType intmapType, String typeAliasName) {
        values.put(name, safeValue(value));
        constants.add(name);
        if (intmapType != null) {
            intmapTypes.put(name, intmapType);
            if (typeAliasName != null) {
                intmapTypeAliasNames.put(name, typeAliasName);
            }
        }
    }
    
    public IntmapType getIntmapType(String name) {
        if (intmapTypes.containsKey(name)) {
            return intmapTypes.get(name);
        }
        if (enclosing != null) {
            return enclosing.getIntmapType(name);
        }
        return null;
    }
    
    public String getIntmapTypeAliasName(String name) {
        if (intmapTypeAliasNames.containsKey(name)) {
            return intmapTypeAliasNames.get(name);
        }
        if (enclosing != null) {
            return enclosing.getIntmapTypeAliasName(name);
        }
        return null;
    }
    
    /**
     * Check if a variable is declared as a constant.
     */
    public boolean isConst(String name) {
        if (constants.contains(name)) {
            return true;
        }
        if (enclosing != null) {
            return enclosing.isConst(name);
        }
        return false;
    }

    Object get(String name) throws InterpreterError {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new InterpreterError("Undefined variable '" + name + "'.");
    }

    public void assign(String name, Object value) throws InterpreterError {
        if (values.containsKey(name)) {
            // Check if the variable is a constant
            if (constants.contains(name)) {
                throw new InterpreterError("Cannot reassign constant variable '" + name + "'.");
            }
            values.put(name, safeValue(value));
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new InterpreterError("Undefined variable '" + name + "'.");
    }

    boolean containsKey(String name) {
        if (!values.containsKey(name)) {
            if (enclosing != null) {
                return enclosing.containsKey(name);
            } else {
                return false;
            }
        } else {
            return true;
        }

    }

}
