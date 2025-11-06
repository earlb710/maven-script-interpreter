package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class CursorNextExpression extends Expression {

    public final Expression target;
    public final int line;

    public CursorNextExpression(int line, Expression target) {
        this.target = target;
        this.line = line;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitCursorNextExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }
}
