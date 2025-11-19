package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;
import java.util.List;

public class ScreenShowStatement extends Statement {

    public final String name;          // required screen name
    public final List<Expression> parameters;  // optional parameters

    public ScreenShowStatement(int line, String name) {
        super(line);
        this.name = name;
        this.parameters = null;
    }

    public ScreenShowStatement(int line, String name, List<Expression> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenShowStatement(this);
    }

    @Override
    public String toString() {
        if (parameters != null && !parameters.isEmpty()) {
            return "show screen " + name + "(...)";
        }
        return "show screen " + name;
    }
}
