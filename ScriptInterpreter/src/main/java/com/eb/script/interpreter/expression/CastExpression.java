package com.eb.script.interpreter.expression;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;

/**
 * Represents a type casting expression: type(value)
 * Examples: int(x), string(y), float(z)
 * 
 * @author Earl Bosch
 */
public class CastExpression extends Expression {

    public final int line;
    public final DataType targetType;
    public final Expression value;

    public CastExpression(int line, DataType targetType, Expression value) {
        this.line = line;
        this.targetType = targetType;
        this.value = value;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitCastExpression(this);
    }

    @Override
    public String toString() {
        return targetType + "(" + value + ")";
    }

    @Override
    public int getLine() {
        return line;
    }
}
