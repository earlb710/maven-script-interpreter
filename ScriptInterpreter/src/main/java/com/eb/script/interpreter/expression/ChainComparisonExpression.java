package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.ebs.EbsToken;

/**
 * Represents a chain like: a < b <= c != d - operators.length ==
 * operands.length - 1 - Result is boolean.
 */
public class ChainComparisonExpression extends Expression {

    public final int line;
    public final Expression[] operands; // length >= 2
    public final EbsToken[] operators;     // comparators between operands

    public ChainComparisonExpression(int line, Expression[] operands, EbsToken[] operators) {
        if (operands == null || operands.length < 2) {
            throw new IllegalArgumentException("ChainComparisonExpression requires at least two operands.");
        }
        if (operators == null || operators.length != operands.length - 1) {
            throw new IllegalArgumentException("operators length must be operands length - 1.");
        }
        this.line = line;
        this.operands = operands;
        this.operators = operators;
    }

    // If you use a visitor pattern, uncomment and add to ExpressionVisitor:
    // @Override
    // public <R> R accept(ExpressionVisitor<R> visitor) {
    //     return visitor.visitChainComparison(this);
    // }
    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitChainComparisonExpression(this);
    }

    @Override
    public String toString() {
        return "size=" + operands.length;
    }

    @Override
    public int getLine() {
        return line;
    }
}
