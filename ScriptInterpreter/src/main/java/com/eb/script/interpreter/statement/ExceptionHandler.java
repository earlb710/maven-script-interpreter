package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.ErrorType;

/**
 * Represents a single exception handler clause in a try-exceptions block.
 * Each handler catches a specific error type and executes a block of code.
 * 
 * Syntax: when ERROR_TYPE { statements }
 * 
 * @author Earl Bosch
 */
public class ExceptionHandler {
    
    /** The type of error this handler catches */
    public final ErrorType errorType;
    
    /** The block of statements to execute when this handler catches an error */
    public final BlockStatement handlerBlock;
    
    /** Optional variable name to hold the error message */
    public final String errorVarName;
    
    /**
     * Create an exception handler for the specified error type.
     */
    public ExceptionHandler(ErrorType errorType, BlockStatement handlerBlock) {
        this.errorType = errorType;
        this.handlerBlock = handlerBlock;
        this.errorVarName = null;
    }
    
    /**
     * Create an exception handler with an error variable name.
     * Syntax: when ERROR_TYPE(varName) { statements }
     */
    public ExceptionHandler(ErrorType errorType, String errorVarName, BlockStatement handlerBlock) {
        this.errorType = errorType;
        this.handlerBlock = handlerBlock;
        this.errorVarName = errorVarName;
    }
    
    /**
     * Check if this handler can catch the given error type.
     */
    public boolean canHandle(ErrorType thrownType) {
        // ANY_ERROR catches everything
        if (errorType == ErrorType.ANY_ERROR) {
            return true;
        }
        return errorType == thrownType;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("when ");
        sb.append(errorType.getName());
        if (errorVarName != null) {
            sb.append("(").append(errorVarName).append(")");
        }
        sb.append(" { ... }");
        return sb.toString();
    }
}
