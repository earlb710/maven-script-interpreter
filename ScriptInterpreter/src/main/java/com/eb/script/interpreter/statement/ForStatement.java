package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ForStatement extends Statement {

    public final Statement initializer;
    public final Expression condition;
    public final Statement increment;
    public final BlockStatement body;

    public ForStatement(int line, Statement initializer, Expression condition, Statement increment, BlockStatement body) {
        super(line);
        this.initializer = initializer;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitForStatement(this);
    }
}
