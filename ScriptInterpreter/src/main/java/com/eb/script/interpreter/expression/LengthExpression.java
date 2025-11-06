package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class LengthExpression extends Expression {
    public int line;
    public final Expression target;

    public LengthExpression(int line, Expression target) {
        this.target = target;
    }

    @Override
    public Object accept(ExpressionVisitor visitor)throws InterpreterError {
        return visitor.visitLengthExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }
}
