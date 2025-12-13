package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;
import java.io.Serializable;

public abstract class Expression implements Serializable {

    public abstract Object accept(ExpressionVisitor visitor) throws InterpreterError;

    public abstract int getLine();
}
