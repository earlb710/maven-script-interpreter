package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class CloseConnectionStatement extends Statement {

    public final String name;

    public CloseConnectionStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitCloseConnectionStatement(this);
    }

    @Override
    public String toString() {
        return "close connection " + name;
    }
}
