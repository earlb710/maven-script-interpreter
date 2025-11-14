package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class ScreenHideStatement extends Statement {

    public final String name;          // required screen name

    public ScreenHideStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenHideStatement(this);
    }

    @Override
    public String toString() {
        return "screen " + name + " hide";
    }
}
