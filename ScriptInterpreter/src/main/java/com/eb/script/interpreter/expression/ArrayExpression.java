package com.eb.script.interpreter.expression;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;

/**
 *
 * @author Earl Bosch
 */
public class ArrayExpression extends Expression {

    public final int line;
    public final DataType dataType;
    public final Expression[] dimensions; // dimension sizes, each an expression
    public Expression initializer;

    public ArrayExpression(int line, DataType dataType, Expression[] dimensions, Expression initializer) {
        this.line = line;
        this.dataType = dataType;
        this.dimensions = dimensions;
        this.initializer = initializer;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitArrayInitExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }
}
