package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import java.util.List;

/**
 * Represents a try-exceptions statement for error handling in EBS scripts.
 * 
 * Syntax:
 *   try {
 *       statements
 *   } exceptions {
 *       when ERROR_TYPE { handler statements }
 *       when ANY_ERROR { handler statements }
 *   }
 * 
 * The try block is executed first. If an error occurs, the exception handlers
 * are checked in order. The first handler that matches the error type will
 * be executed. If no handler matches, the error propagates up.
 * 
 * @author Earl Bosch
 */
public class TryStatement extends Statement {
    
    /** The block of statements to try executing */
    public final BlockStatement tryBlock;
    
    /** The list of exception handlers to check in order */
    public final ExceptionHandler[] handlers;
    
    /**
     * Create a try-exceptions statement with the given try block and handlers.
     */
    public TryStatement(int line, BlockStatement tryBlock, List<ExceptionHandler> handlers) {
        super(line);
        this.tryBlock = tryBlock;
        this.handlers = handlers.toArray(new ExceptionHandler[0]);
    }
    
    /**
     * Create a try-exceptions statement with an array of handlers.
     */
    public TryStatement(int line, BlockStatement tryBlock, ExceptionHandler[] handlers) {
        super(line);
        this.tryBlock = tryBlock;
        this.handlers = handlers;
    }
    
    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitTryStatement(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("try { ... } exceptions { ");
        for (int i = 0; i < handlers.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(handlers[i].toString());
        }
        sb.append(" }");
        return sb.toString();
    }
}
