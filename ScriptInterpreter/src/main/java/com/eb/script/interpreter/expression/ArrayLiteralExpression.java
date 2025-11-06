package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;
import com.eb.util.Util;

/**
 *
 * @author Earl Bosch
 */
public class ArrayLiteralExpression extends Expression {

    public final int line;
    public Expression array;
    public final Expression[] elements;

    public ArrayLiteralExpression(int line, Expression array, Expression[] elements) {
        this.line = line;
        this.array = array;
        this.elements = elements;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitArrayLiteralExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (array != null) {
            ret.append(array.toString()).append(":");
        }
        ret.append(Util.stringify(elements));
        return ret.toString(); 
    }

}
