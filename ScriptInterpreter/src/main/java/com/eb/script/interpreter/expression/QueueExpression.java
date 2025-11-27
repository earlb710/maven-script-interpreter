package com.eb.script.interpreter.expression;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;

/**
 * Expression representing a queue type variable declaration.
 * Similar to ArrayExpression but for FIFO queues.
 *
 * @author Earl Bosch
 */
public class QueueExpression extends Expression {

    public final int line;
    public final DataType dataType;  // Element type of the queue

    public QueueExpression(int line, DataType dataType) {
        this.line = line;
        this.dataType = dataType;
    }

    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitQueueInitExpression(this);
    }

    @Override
    public int getLine() {
        return line;
    }
}
