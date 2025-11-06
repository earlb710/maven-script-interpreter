package com.eb.script.interpreter.expression;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;

public class LiteralExpression extends Expression {

    public final DataType dataType;
    public final Object value;

    public LiteralExpression(EbsTokenType type, Object value) {
        if (type == EbsTokenType.BOOL_TRUE) {
            type = EbsTokenType.BOOL;
            value = true;
        } else if (type == EbsTokenType.BOOL_FALSE) {
            type = EbsTokenType.BOOL;
            value = false;
        }
        this.dataType = type.getDataType();
        if (type == EbsTokenType.NULL) {
            this.value = null;
        } else if (!this.dataType.isDataType(value)) {
            this.value = dataType.convertValue(value);
        } else {
            this.value = value;
        }
    }

    public LiteralExpression(DataType dataType, Object value) {
        this.dataType = dataType;
        if (dataType == null) {
            this.value = null;
        } else if (!this.dataType.isDataType(value)) {
            this.value = dataType.convertValue(value);
        } else {
            this.value = value;
        }
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitLiteralExpression(this);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int getLine() {
        return -1;
    }
}
