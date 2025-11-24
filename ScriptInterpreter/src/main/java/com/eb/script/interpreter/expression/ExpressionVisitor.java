package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

public interface ExpressionVisitor {

    Object visitLiteralExpression(LiteralExpression expr) throws InterpreterError;

    Object visitVariableExpression(VariableExpression expr) throws InterpreterError;

    Object visitBinaryExpression(BinaryExpression expr) throws InterpreterError;

    Object visitUnaryExpression(UnaryExpression expr) throws InterpreterError;

    Object visitCallExpression(CallExpression expr) throws InterpreterError;

    Object visitArrayInitExpression(ArrayExpression expr) throws InterpreterError;

    Object visitArrayLiteralExpression(ArrayLiteralExpression expr) throws InterpreterError;

    Object visitIndexExpression(IndexExpression expr) throws InterpreterError;

    Object visitLengthExpression(LengthExpression expr) throws InterpreterError;

    Object visitChainComparisonExpression(ChainComparisonExpression expr) throws InterpreterError;

    Object visitSqlSelectExpression(SqlSelectExpression expr) throws InterpreterError;

    Object visitCursorHasNextExpression(CursorHasNextExpression expr) throws InterpreterError;

    Object visitCursorNextExpression(CursorNextExpression expr) throws InterpreterError;

    Object visitPropertyExpression(PropertyExpression expr) throws InterpreterError;

    Object visitCastExpression(CastExpression expr) throws InterpreterError;

}
