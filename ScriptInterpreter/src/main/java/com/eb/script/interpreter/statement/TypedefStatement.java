package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;

/**
 * Represents a type definition statement (typedef).
 * Syntax: typeName typeof type_definition
 * Example: atype typeof array.record{id: int, name: string}
 * 
 * @author Earl Bosch
 */
public class TypedefStatement extends Statement {
    
    public final String typeName;
    public final DataType dataType;
    public final RecordType recordType;
    public final boolean isArray;
    public final Integer arraySize; // null for dynamic arrays
    
    /**
     * Create a typedef statement for a simple type
     * @param line Line number
     * @param typeName Name of the type alias
     * @param dataType The data type being aliased
     */
    public TypedefStatement(int line, String typeName, DataType dataType) {
        super(line);
        this.typeName = typeName;
        this.dataType = dataType;
        this.recordType = null;
        this.isArray = false;
        this.arraySize = null;
    }
    
    /**
     * Create a typedef statement for a record type
     * @param line Line number
     * @param typeName Name of the type alias
     * @param recordType The record type definition
     */
    public TypedefStatement(int line, String typeName, RecordType recordType) {
        super(line);
        this.typeName = typeName;
        this.dataType = DataType.RECORD;
        this.recordType = recordType;
        this.isArray = false;
        this.arraySize = null;
    }
    
    /**
     * Create a typedef statement for an array type
     * @param line Line number
     * @param typeName Name of the type alias
     * @param dataType Element data type
     * @param recordType Element record type (if array of records)
     * @param arraySize Array size (null for dynamic)
     */
    public TypedefStatement(int line, String typeName, DataType dataType, RecordType recordType, Integer arraySize) {
        super(line);
        this.typeName = typeName;
        this.dataType = dataType;
        this.recordType = recordType;
        this.isArray = true;
        this.arraySize = arraySize;
    }
    
    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitTypedefStatement(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append(" typeof ");
        if (isArray) {
            sb.append("array.");
            if (recordType != null) {
                sb.append(recordType.toString());
            } else {
                sb.append(dataType);
            }
            if (arraySize != null) {
                sb.append("[").append(arraySize).append("]");
            } else {
                sb.append("[*]");
            }
        } else if (recordType != null) {
            sb.append(recordType.toString());
        } else {
            sb.append(dataType);
        }
        return sb.toString();
    }
}
