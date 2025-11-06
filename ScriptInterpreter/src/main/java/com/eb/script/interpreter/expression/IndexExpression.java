package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class IndexExpression extends Expression {

    public final int line;
    public final Expression target;      // The thing being indexed (e.g., variable, call result, another index)
    public final Expression[] indices;   // One or more index expressions (e.g., i, j, ...)

    public IndexExpression(int line, Expression target, Expression[] indices) {
        this.line = line;
        this.target = target;
        this.indices = indices;
    }

    @Override
    public Object accept(ExpressionVisitor visitor)throws InterpreterError{
        return visitor.visitIndexExpression(this);
    }

    @Override
    public String toString() {
        return target.toString();
    }

    @Override
    public int getLine() {
        return line;
    }
}
