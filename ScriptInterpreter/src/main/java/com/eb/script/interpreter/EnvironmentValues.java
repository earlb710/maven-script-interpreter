package com.eb.script.interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Earl Bosch
 */
public class EnvironmentValues {

    final Map<String, Object> values = new HashMap<>();
    final EnvironmentValues enclosing;

    public EnvironmentValues() {
        this.enclosing = null;
    }

    public EnvironmentValues(EnvironmentValues enclosing) {
        this.enclosing = enclosing;
    }

    void clear() {
        values.clear();
    }

    void define(String name, Object value) {
        values.put(name, value);
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
