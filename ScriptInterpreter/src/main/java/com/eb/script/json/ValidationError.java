package com.eb.script.json;

/**
 *
 * @author Earl Bosch
 *
 * Validation error with JSON Pointer-like path and message.
 * 
 */

public final class ValidationError {

    public final String path;
    public final String message;

    public ValidationError(String path, String message) {
        this.path = path;
        this.message = message;
    }

    @Override
    public String toString() {
        return path + " : " + message;
    }
}
