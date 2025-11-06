package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

/**
 *
 * @author Earl Bosch
 */
public class IfStatement extends Statement {

    public final Expression condition;
    public final Statement thenBranch;
    public final Statement elseBranch; // may be null

    public IfStatement(int line, Expression condition, Statement thenBranch) {
        this(line, condition, thenBranch, null);
    }

    public IfStatement(int line, Expression condition, Statement thenBranch, Statement elseBranch) {
        super(line);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitIfStatement(this);
    }

}
