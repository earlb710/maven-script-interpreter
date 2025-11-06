package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class ConnectStatement extends Statement {

    public final String name;          // required connection name
    public final Expression spec;      // string | json literal | identifier

    public ConnectStatement(int line, String name, Expression spec) {
        super(line);
        this.name = name;
        this.spec = spec;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitConnectStatement(this);
    }

    @Override
    public String toString() {
        return "connect " + name + " = " + spec;
    }
}
