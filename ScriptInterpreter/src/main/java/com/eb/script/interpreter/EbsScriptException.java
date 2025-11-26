package com.eb.script.interpreter;

import java.util.Deque;

/**
 * A runtime exception that can be thrown during EBS script execution.
 * This exception carries an ErrorType that allows exception handlers
 * to catch specific types of errors.
 * 
 * @author Earl Bosch
 */
public class EbsScriptException extends InterpreterError {
    
    /** The type of error that occurred */
    private final ErrorType errorType;
    
    /** The original exception that caused this error, if any */
    private final Throwable cause;
    
    /** The line number where the error occurred */
    private final int line;
    
    /**
     * Create a new EbsScriptException with the specified error type and message.
     */
    public EbsScriptException(int line, ErrorType errorType, String message) {
        super(message);
        this.line = line;
        this.errorType = errorType != null ? errorType : ErrorType.ANY_ERROR;
        this.cause = null;
    }
    
    /**
     * Create a new EbsScriptException with the specified error type, message, and stack.
     */
    public EbsScriptException(int line, ErrorType errorType, String message, Deque<Environment.StackInfo> stack) {
        super(message, stack);
        this.line = line;
        this.errorType = errorType != null ? errorType : ErrorType.ANY_ERROR;
        this.cause = null;
    }
    
    /**
     * Create a new EbsScriptException wrapping another exception.
     */
    public EbsScriptException(int line, ErrorType errorType, String message, Throwable cause) {
        super(message);
        this.line = line;
        this.errorType = errorType != null ? errorType : ErrorType.ANY_ERROR;
        this.cause = cause;
    }
    
    /**
     * Create a new EbsScriptException with all details.
     */
    public EbsScriptException(int line, ErrorType errorType, String message, Throwable cause, Deque<Environment.StackInfo> stack) {
        super(message, stack);
        this.line = line;
        this.errorType = errorType != null ? errorType : ErrorType.ANY_ERROR;
        this.cause = cause;
    }
    
    /**
     * Get the error type for this exception.
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Get the line number where the error occurred.
     */
    public int getErrorLine() {
        return line;
    }
    
    /**
     * Get the original cause of this exception.
     */
    @Override
    public Throwable getCause() {
        return cause;
    }
    
    /**
     * Check if this exception matches the given error type.
     * ANY_ERROR matches all exceptions.
     */
    public boolean matches(ErrorType handlerType) {
        if (handlerType == ErrorType.ANY_ERROR) {
            return true;
        }
        return this.errorType == handlerType;
    }
    
    /**
     * Infer an ErrorType from an InterpreterError based on its message.
     */
    public static ErrorType inferErrorType(InterpreterError error) {
        if (error instanceof EbsScriptException) {
            return ((EbsScriptException) error).getErrorType();
        }
        
        String message = error.getMessage();
        if (message == null) {
            return ErrorType.ANY_ERROR;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Check for specific error patterns
        if (lowerMessage.contains("file") || lowerMessage.contains("read") || 
            lowerMessage.contains("write") || lowerMessage.contains("stream") ||
            lowerMessage.contains("path") || lowerMessage.contains("directory")) {
            return ErrorType.IO_ERROR;
        }
        
        if (lowerMessage.contains("database") || lowerMessage.contains("connection") ||
            lowerMessage.contains("cursor") || lowerMessage.contains("sql") ||
            lowerMessage.contains("query")) {
            return ErrorType.DB_ERROR;
        }
        
        if (lowerMessage.contains("type mismatch") || lowerMessage.contains("cannot cast") ||
            lowerMessage.contains("convert") || lowerMessage.contains("expected type")) {
            return ErrorType.TYPE_ERROR;
        }
        
        if (lowerMessage.contains("null") || lowerMessage.contains("undefined")) {
            return ErrorType.NULL_ERROR;
        }
        
        if (lowerMessage.contains("index") || lowerMessage.contains("out of bounds") ||
            lowerMessage.contains("array")) {
            return ErrorType.INDEX_ERROR;
        }
        
        if (lowerMessage.contains("division by zero") || lowerMessage.contains("arithmetic")) {
            return ErrorType.MATH_ERROR;
        }
        
        if (lowerMessage.contains("parse") || lowerMessage.contains("json") ||
            lowerMessage.contains("invalid format")) {
            return ErrorType.PARSE_ERROR;
        }
        
        if (lowerMessage.contains("network") || lowerMessage.contains("http") ||
            lowerMessage.contains("connection refused") || lowerMessage.contains("timeout")) {
            return ErrorType.NETWORK_ERROR;
        }
        
        if (lowerMessage.contains("not found") || lowerMessage.contains("undefined variable") ||
            lowerMessage.contains("unknown function")) {
            return ErrorType.NOT_FOUND_ERROR;
        }
        
        if (lowerMessage.contains("permission") || lowerMessage.contains("access denied") ||
            lowerMessage.contains("forbidden")) {
            return ErrorType.ACCESS_ERROR;
        }
        
        if (lowerMessage.contains("validation") || lowerMessage.contains("invalid")) {
            return ErrorType.VALIDATION_ERROR;
        }
        
        return ErrorType.ANY_ERROR;
    }
    
    /**
     * Create an EbsScriptException from an existing InterpreterError.
     */
    public static EbsScriptException fromInterpreterError(int line, InterpreterError error) {
        if (error instanceof EbsScriptException) {
            return (EbsScriptException) error;
        }
        
        ErrorType inferredType = inferErrorType(error);
        return new EbsScriptException(line, inferredType, error.getMessage(), error, error.errorStack);
    }
}
