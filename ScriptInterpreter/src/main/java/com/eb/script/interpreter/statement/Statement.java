package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public abstract class Statement {

    protected final int line;

    public Statement(int line) {
        this.line = line;
    }
    
    public abstract void accept(StatementVisitor visitor) throws InterpreterError;

    public int getLine() {
        return line;
    }
}
