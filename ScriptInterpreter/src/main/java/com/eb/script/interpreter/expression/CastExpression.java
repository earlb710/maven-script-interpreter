package com.eb.script.interpreter.expression;

import com.eb.script.token.BitmapType;
import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;

/**
 * Represents a type casting expression: type(value)
 * Examples: int(x), string(y), float(z), bitmapTypeAlias(byteVar)
 * 
 * @author Earl Bosch
 */
public class CastExpression extends Expression {

    public final int line;
    public final DataType targetType;
    public final BitmapType bitmapType;  // For bitmap casting with field definitions
    public final String bitmapTypeAliasName;  // The name of the bitmap type alias
    public final Expression value;

    public CastExpression(int line, DataType targetType, Expression value) {
        this(line, targetType, null, null, value);
    }
    
    public CastExpression(int line, DataType targetType, BitmapType bitmapType, Expression value) {
        this(line, targetType, bitmapType, null, value);
    }
    
    public CastExpression(int line, DataType targetType, BitmapType bitmapType, String bitmapTypeAliasName, Expression value) {
        this.line = line;
        this.targetType = targetType;
        this.bitmapType = bitmapType;
        this.bitmapTypeAliasName = bitmapTypeAliasName;
        this.value = value;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitCastExpression(this);
    }

    @Override
    public String toString() {
        if (bitmapTypeAliasName != null) {
            return bitmapTypeAliasName + "(" + value + ")";
        }
        if (bitmapType != null) {
            return bitmapType + "(" + value + ")";
        }
        return targetType + "(" + value + ")";
    }

    @Override
    public int getLine() {
        return line;
    }
}
