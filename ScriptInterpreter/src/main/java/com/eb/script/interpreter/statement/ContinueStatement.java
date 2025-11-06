package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

/**
 *
 * @author Earl Bosch
 */
public class ContinueStatement extends Statement {

    public ContinueStatement(int line) {
        super(line);
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitContinueStatement(this); 
    }

}
