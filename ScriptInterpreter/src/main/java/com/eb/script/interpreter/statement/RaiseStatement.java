package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.ErrorType;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;
import java.util.List;

/**
 * Represents a raise exception statement for explicitly throwing errors in EBS scripts.
 * 
 * Syntax for standard exceptions:
 *   raise exception ERROR_TYPE("error message");
 * 
 * Syntax for custom exceptions:
 *   raise exception CUSTOM_EXCEPTION(param1, param2, ...);
 * 
 * Standard exceptions (like IO_ERROR, MATH_ERROR, etc.) take only a message parameter.
 * Custom exceptions can be declared with multiple parameters.
 * 
 * @author Earl Bosch
 */
public class RaiseStatement extends Statement {
    
    /** The name of the exception type to raise */
    public final String exceptionName;
    
    /** The error type if this is a standard exception, null for custom exceptions */
    public final ErrorType errorType;
    
    /** The parameters/arguments for the exception (message for standard, or custom params) */
    public final Expression[] parameters;
    
    /** Whether this is a custom exception (not a standard ErrorType) */
    public final boolean isCustomException;
    
    /**
     * Create a raise statement for a standard exception type.
     * 
     * @param line The source line number
     * @param errorType The standard error type (e.g., IO_ERROR, MATH_ERROR)
     * @param message The error message expression
     */
    public RaiseStatement(int line, ErrorType errorType, Expression message) {
        super(line);
        this.exceptionName = errorType.getName();
        this.errorType = errorType;
        this.parameters = message != null ? new Expression[]{message} : new Expression[0];
        this.isCustomException = false;
    }
    
    /**
     * Create a raise statement for a custom exception type.
     * 
     * @param line The source line number
     * @param exceptionName The name of the custom exception
     * @param parameters The parameters for the custom exception
     */
    public RaiseStatement(int line, String exceptionName, List<Expression> parameters) {
        super(line);
        this.exceptionName = exceptionName;
        this.errorType = null;
        this.parameters = parameters != null ? parameters.toArray(new Expression[0]) : new Expression[0];
        this.isCustomException = true;
    }
    
    /**
     * Create a raise statement for a custom exception type with an array of parameters.
     * 
     * @param line The source line number
     * @param exceptionName The name of the custom exception
     * @param parameters The parameters for the custom exception
     */
    public RaiseStatement(int line, String exceptionName, Expression[] parameters) {
        super(line);
        this.exceptionName = exceptionName;
        this.errorType = null;
        this.parameters = parameters != null ? parameters : new Expression[0];
        this.isCustomException = true;
    }
    
    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitRaiseStatement(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("raise exception ");
        sb.append(exceptionName);
        sb.append("(");
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters[i].toString());
        }
        sb.append(");");
        return sb.toString();
    }
}
