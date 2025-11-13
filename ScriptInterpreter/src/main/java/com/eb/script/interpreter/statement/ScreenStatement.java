package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ScreenStatement extends Statement {

    public final String name;          // required screen name
    public final Expression spec;      // json literal or identifier containing screen configuration

    public ScreenStatement(int line, String name, Expression spec) {
        super(line);
        this.name = name;
        this.spec = spec;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenStatement(this);
    }

    @Override
    public String toString() {
        return "screen " + name + " = " + spec;
    }
}
