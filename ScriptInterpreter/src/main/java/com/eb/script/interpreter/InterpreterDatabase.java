package com.eb.script.interpreter;

import com.eb.script.interpreter.db.DbConnection;
import com.eb.script.interpreter.db.DbCursor;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.interpreter.statement.Statement;
import com.eb.script.interpreter.statement.ConnectStatement;
import com.eb.script.interpreter.statement.CloseConnectionStatement;
import com.eb.script.interpreter.statement.UseConnectionStatement;
import com.eb.script.interpreter.statement.CursorStatement;
import com.eb.script.interpreter.statement.OpenCursorStatement;
import com.eb.script.interpreter.statement.CloseCursorStatement;
import com.eb.script.interpreter.expression.SqlSelectExpression;
import com.eb.script.interpreter.expression.CursorHasNextExpression;
import com.eb.script.interpreter.expression.CursorNextExpression;

/**
 * InterpreterDatabase handles all database-related interpreter operations.
 * This includes connections, cursors, and SQL operations.
 */
public class InterpreterDatabase {
    
    private final InterpreterContext context;
    private final Interpreter interpreter;
    
    public InterpreterDatabase(InterpreterContext context, Interpreter interpreter) {
        this.context = context;
        this.interpreter = interpreter;
    }
    
    /**
     * Visit a connect statement to establish a database connection
     */
    public void visitConnectStatement(ConnectStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Connect %1", stmt.name);
        try {
            if (context.getConnections().containsKey(stmt.name)) {
                throw interpreter.error(stmt.getLine(), "Connection '" + stmt.name + "' already exists.");
            }
            Object spec = interpreter.evaluate(stmt.spec);        // string | json | identifier value
            DbConnection conn;
            try {
                conn = context.getDb().connect(spec);
            } catch (Exception e) {
                throw interpreter.error(stmt.getLine(), "Connect failed: " + e.getMessage());
            }
            context.getConnections().put(stmt.name, conn);
        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a close connection statement to close a database connection
     */
    public void visitCloseConnectionStatement(CloseConnectionStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Close connect %1", stmt.name);
        try {
            DbConnection conn = context.getConnections().remove(stmt.name);
            if (conn == null) {
                throw interpreter.error(stmt.getLine(), "Unknown connection '" + stmt.name + "'");
            }
            try {
                conn.close();
            } catch (Exception e) {
                throw interpreter.error(stmt.getLine(), "Close connection failed: " + e.getMessage());
            }
        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a use connection statement to execute statements within a connection context
     */
    public void visitUseConnectionStatement(UseConnectionStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Use connection %1", stmt.connectionName);
        try {
            DbConnection conn = context.getConnections().get(stmt.connectionName);
            if (conn == null) {
                throw interpreter.error(stmt.getLine(), "Unknown connection '" + stmt.connectionName + "'. Did you connect first?");
            }
            context.getConnectionStack().push(stmt.connectionName);
            try {
                if (stmt.statements != null) {
                    for (Statement s : stmt.statements) {
                        interpreter.environment().pushCallStack(s.getLine(), StatementKind.STATEMENT, "%1", s);
                        try {
                            interpreter.acceptStatement(s);
                        } finally {
                            interpreter.environment().popCallStack();
                        }
                    }
                }
            } finally {
                context.getConnectionStack().pop();
            }
        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a cursor statement to declare a cursor
     */
    public void visitCursorStatement(CursorStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Cursor %1", stmt.name);
        try {
            String connName = interpreter.currentConnection();
            if (connName == null) {
                throw interpreter.error(stmt.getLine(), "cursor declaration requires an active 'use <connection> { ... }' block");
            }
            // Parser ensures SELECT text is captured in stmt.select.sql
            context.getCursorSpecs().put(stmt.name, new Interpreter.CursorSpec(connName, stmt.select.sql, stmt.getLine()));
        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit an open cursor statement to open a declared cursor
     */
    public void visitOpenCursorStatement(OpenCursorStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Open cursor %1", stmt.name);
        try {
            Interpreter.CursorSpec spec = context.getCursorSpecs().get(stmt.name);
            if (spec == null) {
                throw interpreter.error(stmt.getLine(), "Unknown cursor '" + stmt.name + "'. Did you declare with 'cursor " + stmt.name + " = select ...;'?");
            }
            DbConnection conn = context.getConnections().get(spec.connectionName);
            if (conn == null) {
                throw interpreter.error(stmt.getLine(), "Connection '" + spec.connectionName + "' is not open");
            }
            // Collect parameters (named and/or positional)
            final java.util.Map<String, Object> named = new java.util.LinkedHashMap<>();
            final java.util.List<Object> positional = new java.util.ArrayList<>();
            if (stmt.parameters != null) {
                for (com.eb.script.interpreter.statement.Parameter p : stmt.parameters) {
                    Object val = interpreter.evaluate(p.value);
                    if (p.name != null) {
                        named.put(p.name, val);
                    } else {
                        positional.add(val);
                    }
                }
            }
            // Open the cursor via adapter and expose it as a variable with the cursor's name
            try {
                DbCursor cursor = conn.openCursor(spec.sql, named, positional);
                // make cursor variable visible (for myCursor.hasNext()/next())
                interpreter.environment().getEnvironmentValues().define(stmt.name, cursor);
            } catch (Exception e) {
                throw interpreter.error(stmt.getLine(), "Open cursor failed: " + e.getMessage());
            }
        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a close cursor statement to close an open cursor
     */
    public void visitCloseCursorStatement(CloseCursorStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.SQL, "Close cursor %1", stmt.name);
        try {
            Object v;
            try {
                v = interpreter.environment().get(stmt.name);
            } catch (RuntimeException undefined) {
                // not defined -> treat as not open
                v = null;
            }
            if (v instanceof DbCursor c) {
                try {
                    c.close();
                } catch (Exception e) {
                    throw interpreter.error(stmt.getLine(), "Close cursor failed: " + e.getMessage());
                }
                // Optionally clear variable so subsequent hasNext()/next() will fail fast
                interpreter.environment().getEnvironmentValues().assign(stmt.name, null);
            } // else: silently ignore closing a non-open cursor name
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a SQL SELECT expression to execute a query
     */
    public Object visitSqlSelectExpression(SqlSelectExpression expr) throws InterpreterError {
        interpreter.environment().pushCallStack(expr.line, StatementKind.SQL, "Expression %1", expr.sql);
        try {
            String connName = interpreter.currentConnection();
            if (connName == null) {
                throw interpreter.error(expr.line, "SELECT requires an active 'use <connection> { ... }' block");
            }
            DbConnection conn = context.getConnections().get(connName);
            if (conn == null) {
                throw interpreter.error(expr.line, "Connection '" + connName + "' is not open");
            }
            try {
                // No parameters here; add if you later extend SELECT expr to support them.
                return conn.executeSelect(expr.sql,
                        java.util.Collections.emptyMap(),
                        java.util.Collections.emptyList());
            } catch (Exception e) {
                throw interpreter.error(expr.line, "SELECT failed: " + e.getMessage());
            }
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a cursor hasNext expression to check if cursor has more rows
     */
    public Object visitCursorHasNextExpression(CursorHasNextExpression expr) throws InterpreterError {
        interpreter.environment().pushCallStack(expr.getLine(), StatementKind.SQL, "HasNext %1", expr.target.toString());
        try {
            Object v = interpreter.evaluate(expr.target);
            if (!(v instanceof DbCursor c)) {
                throw interpreter.error(expr.getLine(), "hasNext() target is not a cursor");
            }
            try {
                return c.hasNext();
            } catch (Exception e) {
                throw interpreter.error(expr.getLine(), "hasNext() failed: " + e.getMessage());
            }
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Visit a cursor next expression to fetch the next row
     */
    public Object visitCursorNextExpression(CursorNextExpression expr) throws InterpreterError {
        interpreter.environment().pushCallStack(expr.getLine(), StatementKind.SQL, "Next %1", expr.target.toString());
        try {
            Object v = interpreter.evaluate(expr.target);
            if (!(v instanceof DbCursor c)) {
                throw interpreter.error(expr.getLine(), "next() target is not a cursor");
            }
            try {
                return c.next(); // map of column -> value
            } catch (Exception e) {
                throw interpreter.error(expr.getLine(), "next() failed: " + e.getMessage());
            }
        } finally {
            interpreter.environment().popCallStack();
        }
    }
}
