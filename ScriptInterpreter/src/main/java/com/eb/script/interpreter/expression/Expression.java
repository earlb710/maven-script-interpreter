package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public abstract class Expression {

    public abstract Object accept(ExpressionVisitor visitor) throws InterpreterError;

    public abstract int getLine();
}
