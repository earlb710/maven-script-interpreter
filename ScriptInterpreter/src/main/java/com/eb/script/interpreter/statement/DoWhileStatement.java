package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

/**
 *
 * @author Earl Bosch
 */
public class DoWhileStatement extends Statement {

    public final Expression condition;
    public final Statement statement;

    public DoWhileStatement(int line, Expression condition, Statement statements) {
        super(line);
        this.condition = condition;
        this.statement = statements;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitDoWhileStatement(this);
    }

}
