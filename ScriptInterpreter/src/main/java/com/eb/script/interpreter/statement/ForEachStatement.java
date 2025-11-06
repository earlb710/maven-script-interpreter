package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ForEachStatement extends Statement {

    public final String varName;
    public final Expression iterable;
    public final BlockStatement body;

    public ForEachStatement(int line, String varName, Expression iterable, BlockStatement body) {
        super(line);
        this.varName = varName;
        this.iterable = iterable;
        this.body = body;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitForEachStatement(this);
    }
}
