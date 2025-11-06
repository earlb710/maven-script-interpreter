package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ReturnStatement extends Statement {

    public final Expression value;
    public Object returnValue;

    public ReturnStatement(int line, Expression value) {
        super(line);
        this.value = value;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        returnValue=visitor.visitReturnStatement(this);
    }
}
