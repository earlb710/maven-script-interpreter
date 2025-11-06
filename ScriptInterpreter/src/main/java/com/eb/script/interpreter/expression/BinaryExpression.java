package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.ebs.EbsToken;

public class BinaryExpression extends Expression {

    public final int line;
    public final Expression left;
    public final EbsToken operator;
    public final Expression right;

    public BinaryExpression(int line, Expression left, EbsToken operator, Expression right) {
        this.line = line;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitBinaryExpression(this);
    }

    @Override
    public String toString() {
        return left + " <" + operator + "> " + right;
    }

    @Override
    public int getLine() {
        return line;
    }
}
