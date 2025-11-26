package com.eb.script.interpreter.statement;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;
import java.util.List;

public class BlockStatement extends Statement {

    public final String name;
    public final Statement[] statements;
    public final Parameter[] parameters;
    public DataType returnType;
    
    /** Optional exception handlers for this block (functions can have exceptions without try) */
    public ExceptionHandler[] exceptionHandlers;

    public BlockStatement(int line, List<Statement> statements) {
        super(line);
        this.name = null;
        this.statements = statementsToArray(statements);
        this.parameters = null;
        this.returnType = null;
        this.exceptionHandlers = null;
    }

    public BlockStatement(int line, String name, List<Statement> statements) {
        super(line);
        this.name = name;
        this.statements = statementsToArray(statements);
        this.parameters = null;
        this.returnType = null;
        this.exceptionHandlers = null;
    }

    public BlockStatement(int line, String name, List<Parameter> parameters, List<Statement> statements) {
        super(line);
        this.name = name;
        this.statements = statementsToArray(statements);
        this.parameters = parametersToArray(parameters);
        this.returnType = null;
        this.exceptionHandlers = null;
    }

    public BlockStatement(int line, String name, List<Parameter> parameters, List<Statement> statements, DataType returnType) {
        super(line);
        this.name = name;
        this.statements = statementsToArray(statements);
        this.parameters = parametersToArray(parameters);
        this.returnType = returnType;
        this.exceptionHandlers = null;
    }
    
    /**
     * Set exception handlers for this block.
     * This allows functions to have exception handlers without using try keyword.
     */
    public void setExceptionHandlers(List<ExceptionHandler> handlers) {
        if (handlers != null && !handlers.isEmpty()) {
            this.exceptionHandlers = handlers.toArray(new ExceptionHandler[0]);
        }
    }
    
    /**
     * Check if this block has exception handlers.
     */
    public boolean hasExceptionHandlers() {
        return exceptionHandlers != null && exceptionHandlers.length > 0;
    }

    private Statement[] statementsToArray(List<Statement> list) {
        if (list != null) {
            return list.toArray(Statement[]::new);
        }
        return null;
    }

    private Parameter[] parametersToArray(List<Parameter> list) {
        if (list != null) {
            return list.toArray(Parameter[]::new);
        }
        return null;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitBlockStatement(this);
    }
}
