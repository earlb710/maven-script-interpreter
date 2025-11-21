package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public interface StatementVisitor {

    void visitVarStatement(VarStatement stmt) throws InterpreterError;

    void visitAssignStatement(AssignStatement stmt) throws InterpreterError;

    void visitIndexAssignStatement(IndexAssignStatement stmt) throws InterpreterError;

    void visitPrintStatement(PrintStatement stmt) throws InterpreterError;

    void visitBlockStatement(BlockStatement stmt) throws InterpreterError;

    void visitCallStatement(CallStatement stmt) throws InterpreterError;

    void visitIfStatement(IfStatement stmt) throws InterpreterError;

    void visitWhileStatement(WhileStatement stmt) throws InterpreterError;

    void visitDoWhileStatement(DoWhileStatement stmt) throws InterpreterError;

    void visitForEachStatement(ForEachStatement stmt) throws InterpreterError;

    void visitForStatement(ForStatement stmt) throws InterpreterError;

    void visitBreakStatement(BreakStatement stmt) throws InterpreterError;

    void visitContinueStatement(ContinueStatement stmt) throws InterpreterError;

    Object visitReturnStatement(ReturnStatement stmt) throws InterpreterError;

    void visitCursorStatement(CursorStatement stmt) throws InterpreterError;

    void visitOpenCursorStatement(OpenCursorStatement stmt) throws InterpreterError;

    void visitCloseCursorStatement(CloseCursorStatement stmt) throws InterpreterError;

    void visitConnectStatement(ConnectStatement stmt) throws InterpreterError;

    void visitUseConnectionStatement(UseConnectionStatement stmt) throws InterpreterError;

    void visitCloseConnectionStatement(CloseConnectionStatement stmt) throws InterpreterError;

    void visitScreenStatement(ScreenStatement stmt) throws InterpreterError;

    void visitScreenShowStatement(ScreenShowStatement stmt) throws InterpreterError;

    void visitScreenHideStatement(ScreenHideStatement stmt) throws InterpreterError;

    void visitScreenCloseStatement(ScreenCloseStatement stmt) throws InterpreterError;

    void visitScreenSubmitStatement(ScreenSubmitStatement stmt) throws InterpreterError;

    void visitImportStatement(ImportStatement stmt) throws InterpreterError;

}
