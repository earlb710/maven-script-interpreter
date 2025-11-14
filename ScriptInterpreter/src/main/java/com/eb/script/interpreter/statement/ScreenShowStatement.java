package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class ScreenShowStatement extends Statement {

    public final String name;          // required screen name

    public ScreenShowStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenShowStatement(this);
    }

    @Override
    public String toString() {
        return "screen " + name + " show";
    }
}
