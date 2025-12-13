package com.eb.script.interpreter.statement;

import com.eb.script.token.DataType;
import com.eb.script.interpreter.expression.Expression;
import java.io.Serializable;

/**
 *
 * @author Earl Bosch
 */
public class Parameter implements Serializable {

    public final String name;
    public final DataType paramType;
    public final Expression value;
    public final boolean mandatory;

    public Parameter(String name, DataType paramType) {
        this.name = name.toLowerCase();
        this.paramType = paramType;
        this.value = null;
        this.mandatory = true;
    }

    public Parameter(String name, DataType paramType, Expression value) {
        this.name = name.toLowerCase();
        this.paramType = paramType;
        this.value = value;
        this.mandatory = true;
    }

    public Parameter(String name, Expression value) {
        this.name = name.toLowerCase();
        this.paramType = null;
        this.value = value;
        this.mandatory = true;
    }

    public Parameter(String name, DataType paramType, boolean mandatory) {
        this.name = name.toLowerCase();
        this.paramType = paramType;
        this.value = null;
        this.mandatory = mandatory;
    }

    public Parameter(String name, DataType paramType, Expression value, boolean mandatory) {
        this.name = name.toLowerCase();
        this.paramType = paramType;
        this.value = value;
        this.mandatory = mandatory;
    }

    public Parameter(String name, Expression value, boolean mandatory) {
        this.name = name.toLowerCase();
        this.paramType = null;
        this.value = value;
        this.mandatory = mandatory;
    }

    public Parameter(Expression value) {
        this.name = null;
        this.paramType = null;
        this.value = value;
        this.mandatory = true;
    }

    @Override
    public String toString() {
        return name + ":" + paramType + "=" + value;
    }

}
