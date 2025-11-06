package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

public class AssignStatement extends Statement {

    public final String name;
    public final Expression value;

    public AssignStatement(int line, String name, Expression value) {
        super(line);
        this.name = name;
        this.value = value;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitAssignStatement(this);
    }

    @Override
    public String toString() {
        return "Assign " + name + " = " + value;
    }

}
