package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import java.io.Serializable;

public abstract class Statement implements Serializable {

    protected final int line;

    public Statement(int line) {
        this.line = line;
    }
    
    public abstract void accept(StatementVisitor visitor) throws InterpreterError;

    public int getLine() {
        return line;
    }
}
