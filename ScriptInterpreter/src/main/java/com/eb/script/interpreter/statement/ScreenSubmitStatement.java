package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class ScreenSubmitStatement extends Statement {

    public final String name;          // optional screen name (null means submit current screen)

    public ScreenSubmitStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenSubmitStatement(this);
    }

    @Override
    public String toString() {
        if (name != null) {
            return "submit screen " + name;
        } else {
            return "submit screen";
        }
    }
}
