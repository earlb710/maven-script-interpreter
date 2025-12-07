package com.eb.script.interpreter.statement;

import com.eb.script.token.BitmapType;
import com.eb.script.token.IntmapType;
import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;

public class VarStatement extends Statement {

    public final String name;
    public final DataType varType;
    public final RecordType recordType; // Record type definition (if varType is RECORD)
    public final BitmapType bitmapType; // Bitmap type definition (if varType is BITMAP)
    public final IntmapType intmapType; // Intmap type definition (if varType is INTMAP)
    public final Expression initializer;
    public final boolean isConst; // Whether this is a const declaration (cannot be reassigned)

    public VarStatement(int line, String name, DataType type, Expression initializer) {
        this(line, name, type, null, null, null, initializer, false);
    }
    
    public VarStatement(int line, String name, DataType type, Expression initializer, boolean isConst) {
        this(line, name, type, null, null, null, initializer, isConst);
    }
    
    public VarStatement(int line, String name, DataType type, RecordType recordType, Expression initializer) {
        this(line, name, type, recordType, null, null, initializer, false);
    }
    
    public VarStatement(int line, String name, DataType type, RecordType recordType, Expression initializer, boolean isConst) {
        this(line, name, type, recordType, null, null, initializer, isConst);
    }
    
    public VarStatement(int line, String name, DataType type, BitmapType bitmapType, Expression initializer, boolean isConst) {
        this(line, name, type, null, bitmapType, null, initializer, isConst);
    }
    
    public VarStatement(int line, String name, DataType type, IntmapType intmapType, Expression initializer, boolean isConst) {
        this(line, name, type, null, null, intmapType, initializer, isConst);
    }
    
    public VarStatement(int line, String name, DataType type, RecordType recordType, BitmapType bitmapType, IntmapType intmapType, Expression initializer, boolean isConst) {
        super(line);
        this.name = name;
        this.varType = type;
        this.recordType = recordType;
        this.bitmapType = bitmapType;
        this.intmapType = intmapType;
        this.initializer = initializer;
        this.isConst = isConst;
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
        this.recordType = null;
        this.bitmapType = null;
        this.intmapType = null;
        this.initializer = initializer;
        this.isConst = false;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitVarStatement(this);
    }

    @Override
    public String toString() {
        String prefix = isConst ? "Const " : "Variable ";
        if (bitmapType != null) {
            return prefix + name + " : " + bitmapType + " = " + initializer;
        }
        if (recordType != null) {
            return prefix + name + " : " + recordType + " = " + initializer;
        }
        return prefix + name + " : " + varType + " = " + initializer;
    }
}
