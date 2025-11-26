package com.eb.script.interpreter;

/**
 * Defines the types of errors that can be caught in EBS exception handlers.
 * These error types allow scripts to catch specific categories of errors
 * or use ANY_ERROR to catch all errors.
 * 
 * @author Earl Bosch
 */
public enum ErrorType {
    /** Catch any error - this is the catch-all handler */
    ANY_ERROR("ANY_ERROR"),
    
    /** I/O related errors (file operations, streams) */
    IO_ERROR("IO_ERROR"),
    
    /** Database connection and query errors */
    DB_ERROR("DB_ERROR"),
    
    /** Type conversion and casting errors */
    TYPE_ERROR("TYPE_ERROR"),
    
    /** Null pointer or null value errors */
    NULL_ERROR("NULL_ERROR"),
    
    /** Array index out of bounds errors */
    INDEX_ERROR("INDEX_ERROR"),
    
    /** Division by zero or other arithmetic errors */
    MATH_ERROR("MATH_ERROR"),
    
    /** Parse errors (JSON, dates, etc.) */
    PARSE_ERROR("PARSE_ERROR"),
    
    /** Network and HTTP errors */
    NETWORK_ERROR("NETWORK_ERROR"),
    
    /** Variable or function not found errors */
    NOT_FOUND_ERROR("NOT_FOUND_ERROR"),
    
    /** Permission or access denied errors */
    ACCESS_ERROR("ACCESS_ERROR"),
    
    /** Validation errors */
    VALIDATION_ERROR("VALIDATION_ERROR");
    
    private final String name;
    
    ErrorType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Get an ErrorType from its string name.
     * Returns null if the name doesn't match any known error type.
     */
    public static ErrorType fromName(String name) {
        if (name == null) {
            return null;
        }
        String upperName = name.toUpperCase();
        for (ErrorType type : values()) {
            if (type.name.equals(upperName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Check if a name is a valid error type name.
     */
    public static boolean isValidErrorType(String name) {
        return fromName(name) != null;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
