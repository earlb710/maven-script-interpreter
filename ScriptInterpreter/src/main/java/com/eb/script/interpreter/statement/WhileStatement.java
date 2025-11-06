package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

/**
 *
 * @author Earl Bosch
 */
public class WhileStatement extends Statement {

    public final Expression condition;
    public final Statement statement;

    public WhileStatement(int line, Expression condition, Statement statement) {
        super(line);
        this.condition = condition;
        this.statement = statement;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitWhileStatement(this);
    }

}
