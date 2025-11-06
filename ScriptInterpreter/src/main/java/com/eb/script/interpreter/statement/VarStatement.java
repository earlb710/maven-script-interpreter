package com.eb.script.interpreter.statement;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;

public class VarStatement extends Statement {

    public final String name;
    public final DataType varType;
    public final Expression initializer;

    public VarStatement(int line, String name, DataType type, Expression initializer) {
        super(line);
        this.name = name;
        this.varType = type;
        this.initializer = initializer;
    }

    public VarStatement(int line, String name, EbsToken tokenType, Expression initializer) {
        super(line);
        if (tokenType.type == EbsTokenType.IDENTIFIER) {
            String tstr = ((String) tokenType.literal).toLowerCase();
            if (EbsTokenType.INTEGER.contains(tstr)) {
                tokenType.type = EbsTokenType.INTEGER;
            } else if (EbsTokenType.LONG.contains(tstr)) {
                tokenType.type = EbsTokenType.LONG;
            } else if (EbsTokenType.STRING.contains(tstr)) {
                tokenType.type = EbsTokenType.STRING;
            } else if (EbsTokenType.FLOAT.contains(tstr)) {
                tokenType.type = EbsTokenType.FLOAT;
            } else if (EbsTokenType.DOUBLE.contains(tstr)) {
                tokenType.type = EbsTokenType.DOUBLE;
            } else if (EbsTokenType.DATE.contains(tstr)) {
                tokenType.type = EbsTokenType.DATE;
            } else if (EbsTokenType.BOOL.contains(tstr)) {
                tokenType.type = EbsTokenType.BOOL;
            }
        }
        this.name = name;
        this.varType = tokenType.type.getDataType();
        this.initializer = initializer;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitVarStatement(this);
    }

    @Override
    public String toString() {
        return "Variable " + name + " : " + varType + " = " + initializer;
    }
}
