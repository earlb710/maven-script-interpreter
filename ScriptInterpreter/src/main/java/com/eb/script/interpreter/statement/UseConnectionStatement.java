package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import java.util.List;

public class UseConnectionStatement extends Statement {

    public final String connectionName;
    public final Statement[] statements;

    public UseConnectionStatement(int line, String connectionName, List<Statement> body) {
        super(line);
        this.connectionName = connectionName;
        this.statements     = (body == null) ? null : body.toArray(Statement[]::new);
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitUseConnectionStatement(this);
    }

    @Override
    public String toString() {
        return "use " + connectionName + " { ... }";
    }
}
