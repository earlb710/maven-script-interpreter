package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class CloseCursorStatement extends Statement {

    public final String name;

    public CloseCursorStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitCloseCursorStatement(this);
    }

    @Override
    public String toString() {
        return "close " + name;
    }
}
