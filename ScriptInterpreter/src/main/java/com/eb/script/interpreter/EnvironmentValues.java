package com.eb.script.interpreter;

import com.eb.script.token.RecordType;
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
        constants.clear();
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }
    
    public void defineConst(String name, Object value) {
        values.put(name, value);
        constants.add(name);
    }
    
    public void defineWithRecordType(String name, Object value, RecordType recordType) {
        values.put(name, value);
        if (recordType != null) {
            recordTypes.put(name, recordType);
        }
    }
    
    public void defineConstWithRecordType(String name, Object value, RecordType recordType) {
        values.put(name, value);
        constants.add(name);
        if (recordType != null) {
            recordTypes.put(name, recordType);
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
            values.put(name, value);
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
