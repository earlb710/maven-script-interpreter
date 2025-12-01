package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ScreenStatement extends Statement {

    public final String name;          // required screen name
    public final Expression spec;      // json literal or identifier containing screen configuration
    public final boolean replaceExisting; // if true, use 'new screen' to replace existing screen definition

    public ScreenStatement(int line, String name, Expression spec) {
        this(line, name, spec, false);
    }

    public ScreenStatement(int line, String name, Expression spec, boolean replaceExisting) {
        super(line);
        this.name = name;
        this.spec = spec;
        this.replaceExisting = replaceExisting;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenStatement(this);
    }

    @Override
    public String toString() {
        if (replaceExisting) {
            return "new screen " + name + " = " + spec;
        }
        return "screen " + name + " = " + spec;
    }
}
