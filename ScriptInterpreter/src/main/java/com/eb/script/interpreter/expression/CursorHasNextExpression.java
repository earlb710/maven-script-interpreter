package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class CursorHasNextExpression extends Expression {

    public final Expression target;
    public final int line;

    public CursorHasNextExpression(int line, Expression target){
        this.target = target;
        this.line = line;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitCursorHasNextExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }
}
