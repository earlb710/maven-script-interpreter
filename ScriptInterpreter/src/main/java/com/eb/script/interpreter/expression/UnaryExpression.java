package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.ebs.EbsToken;

/**
 *
 * @author 7041710
 */
public class UnaryExpression extends Expression {

    public final int line;
    public final EbsToken operator;
    public final Expression right;

    public UnaryExpression(int line, EbsToken operator, Expression right) {
        this.line = line;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitUnaryExpression(this);
    }

    @Override
    public String toString() {
        return operator.toString() + " " + right;
    }

    @Override
    public int getLine() {
        return line;
    }
}
