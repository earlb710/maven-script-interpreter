package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

/**
 *
 * @author Earl Bosch
 */
public class BreakStatement extends Statement {

    public BreakStatement(int line) {
        super(line);
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitBreakStatement(this);
    }

}
