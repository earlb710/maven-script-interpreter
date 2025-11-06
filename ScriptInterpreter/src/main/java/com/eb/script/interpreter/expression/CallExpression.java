package com.eb.script.interpreter.expression;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;
import com.eb.util.Util;
import com.eb.script.interpreter.statement.CallStatement;

public class CallExpression extends Expression {

    public final int line;
    public final CallStatement call;
    public DataType returnType;

    public CallExpression(CallStatement call) {
        this.call = call;
        this.line = call.getLine();
    }

    public void setReturnType(DataType returnType) {
        this.returnType = returnType;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitCallExpression(this);
    }

    @Override
    public String toString() {
        return call.name + " (" + Util.stringLineOfArray(call.parameters)+ ") : " + returnType;
    }

    @Override
    public int getLine() {
        return line;
    }
}
