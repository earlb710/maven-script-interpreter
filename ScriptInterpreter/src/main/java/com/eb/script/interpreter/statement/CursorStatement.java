package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.SqlSelectExpression;

public class CursorStatement extends Statement {

    public final String name;
    public final SqlSelectExpression select;

    public CursorStatement(int line, String name, SqlSelectExpression select) {
        super(line);
        this.name = name;
        this.select = select;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitCursorStatement(this);
    }

    @Override
    public String toString() {
        return "cursor " + name + " = " + select;
    }
}
