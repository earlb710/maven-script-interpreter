package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class ScreenCloseStatement extends Statement {

    public final String name;          // optional screen name (null means close current screen)

    public ScreenCloseStatement(int line, String name) {
        super(line);
        this.name = name;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenCloseStatement(this);
    }

    @Override
    public String toString() {
        if (name != null) {
            return "close screen " + name;
        } else {
            return "close screen";
        }
    }
}
