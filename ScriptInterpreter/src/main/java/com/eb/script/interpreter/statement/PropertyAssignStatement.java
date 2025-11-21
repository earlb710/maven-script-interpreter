package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;

/**
 * Statement for assigning to a property/field of an object.
 * Used for record field assignment (e.g., employee.name = "John")
 * 
 * @author Earl Bosch
 */
public class PropertyAssignStatement extends Statement {
    
    public final Expression object;  // The object/record being accessed
    public final String propertyName; // The field/property name
    public final Expression value;    // The value to assign
    
    public PropertyAssignStatement(int line, Expression object, String propertyName, Expression value) {
        super(line);
        this.object = object;
        this.propertyName = propertyName;
        this.value = value;
    }
    
    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitPropertyAssignStatement(this);
    }
    
    @Override
    public String toString() {
        return object.toString() + "." + propertyName + " = " + value.toString();
    }
}
