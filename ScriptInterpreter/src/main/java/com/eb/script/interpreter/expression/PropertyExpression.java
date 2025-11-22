package com.eb.script.interpreter.expression;

import com.eb.script.interpreter.InterpreterError;

/**
 * Expression for accessing a property/field of an object.
 * Used for record field access (e.g., employee.name, employee.age)
 * 
 * @author Earl Bosch
 */
public class PropertyExpression extends Expression {
    
    public final int line;
    public final Expression object;  // The object/record being accessed
    public final String propertyName; // The field/property name
    
    public PropertyExpression(int line, Expression object, String propertyName) {
        this.line = line;
        this.object = object;
        this.propertyName = propertyName;
    }
    
    @Override
    public Object accept(ExpressionVisitor visitor) throws InterpreterError {
        return visitor.visitPropertyExpression(this);
    }
    
    @Override
    public int getLine() {
        return line;
    }
    
    @Override
    public String toString() {
        return object.toString() + "." + propertyName;
    }
}
