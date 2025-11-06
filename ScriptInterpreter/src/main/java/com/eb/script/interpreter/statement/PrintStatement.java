package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class PrintStatement extends Statement {
    public final Expression expression;

    public PrintStatement(int line, Expression expression) {
        super(line);
        this.expression = expression;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitPrintStatement(this);
    }

    @Override
    public String toString() {
        return "print "+expression;
    }
    
    
}
