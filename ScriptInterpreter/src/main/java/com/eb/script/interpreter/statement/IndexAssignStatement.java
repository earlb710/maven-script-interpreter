// com/eb/script/interpreter/statement/IndexAssignStatement.java
package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class IndexAssignStatement extends Statement {

    public final Expression target;  // must be an IndexExpression
    public final Expression value;

    public IndexAssignStatement(int line, Expression target, Expression value) {
        super(line);
        this.target = target;
        this.value = value;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitIndexAssignStatement(this);
    }
}
