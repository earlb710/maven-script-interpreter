package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class VariableExpression extends Expression {

    public final int line;
    public final String name;

    public VariableExpression(int line, String name) {
        this.line = line;
        this.name = name;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitVariableExpression(this);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int getLine() {
        return line;
    }
}
