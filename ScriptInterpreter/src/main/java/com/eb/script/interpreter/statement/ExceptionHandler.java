package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.ErrorType;
import com.eb.script.interpreter.EbsScriptException;

/**
 * Represents a single exception handler clause in a try-exceptions block.
 * Each handler catches a specific error type (standard or custom) and executes a block of code.
 * 
 * Syntax: 
 *   when ERROR_TYPE { statements }
 *   when CUSTOM_EXCEPTION { statements }
 * 
 * @author Earl Bosch
 */
public class ExceptionHandler {
    
    /** The type of error this handler catches (for standard exceptions) */
    public final ErrorType errorType;
    
    /** The name of a custom exception this handler catches (null for standard exceptions) */
    public final String customExceptionName;
    
    /** The block of statements to execute when this handler catches an error */
    public final BlockStatement handlerBlock;
    
    /** Optional variable name to hold the error message */
    public final String errorVarName;
    
    /**
     * Create an exception handler for the specified standard error type.
     */
    public ExceptionHandler(ErrorType errorType, BlockStatement handlerBlock) {
        this.errorType = errorType;
        this.customExceptionName = null;
        this.handlerBlock = handlerBlock;
        this.errorVarName = null;
    }
    
    /**
     * Create an exception handler for a standard error type with an error variable name.
     * Syntax: when ERROR_TYPE(varName) { statements }
     */
    public ExceptionHandler(ErrorType errorType, String errorVarName, BlockStatement handlerBlock) {
        this.errorType = errorType;
        this.customExceptionName = null;
        this.handlerBlock = handlerBlock;
        this.errorVarName = errorVarName;
    }
    
    /**
     * Create an exception handler for a custom exception type.
     * Syntax: when CUSTOM_EXCEPTION { statements }
     */
    public ExceptionHandler(String customExceptionName, BlockStatement handlerBlock) {
        this.errorType = null;
        this.customExceptionName = customExceptionName;
        this.handlerBlock = handlerBlock;
        this.errorVarName = null;
    }
    
    /**
     * Create an exception handler for a custom exception type with an error variable name.
     * Syntax: when CUSTOM_EXCEPTION(varName) { statements }
     */
    public ExceptionHandler(String customExceptionName, String errorVarName, BlockStatement handlerBlock) {
        this.errorType = null;
        this.customExceptionName = customExceptionName;
        this.handlerBlock = handlerBlock;
        this.errorVarName = errorVarName;
    }
    
    /**
     * Check if this handler can catch the given error type (for standard exceptions).
     */
    public boolean canHandle(ErrorType thrownType) {
        // ANY_ERROR catches everything
        if (errorType == ErrorType.ANY_ERROR) {
            return true;
        }
        // If this handler is for a custom exception, it can't handle standard types
        if (customExceptionName != null) {
            return false;
        }
        return errorType == thrownType;
    }
    
    /**
     * Check if this handler can catch the given exception.
     * Handles both standard and custom exceptions.
     */
    public boolean canHandle(EbsScriptException exception) {
        // ANY_ERROR catches everything
        if (errorType == ErrorType.ANY_ERROR) {
            return true;
        }
        
        // Check for custom exception match
        if (customExceptionName != null && exception.isCustomException()) {
            return exception.matchesCustomException(customExceptionName);
        }
        
        // Check for standard exception match
        if (errorType != null && !exception.isCustomException()) {
            return errorType == exception.getErrorType();
        }
        
        return false;
    }
    
    /**
     * Check if this is a custom exception handler.
     */
    public boolean isCustomExceptionHandler() {
        return customExceptionName != null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("when ");
        if (customExceptionName != null) {
            sb.append(customExceptionName);
        } else {
            sb.append(errorType.getName());
        }
        if (errorVarName != null) {
            sb.append("(").append(errorVarName).append(")");
        }
        sb.append(" { ... }");
        return sb.toString();
    }
}
