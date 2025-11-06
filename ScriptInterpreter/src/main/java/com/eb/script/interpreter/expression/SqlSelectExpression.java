package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public class SqlSelectExpression extends Expression {

    public final String sql;  // exact SELECT text, without trailing ';'
    public final int line;

    public SqlSelectExpression(int line, String sql) {
        this.sql = sql;
        this.line = line;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError{
        return visitor.visitSqlSelectExpression(this);
    }

    @Override
    public String toString() {
        return "SELECT<" + (sql == null ? "" : sql) + ">";
    }

    @Override
    public int getLine() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
