package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.expression.Expression;
import java.util.List;

public class ScreenShowStatement extends Statement {

    public final String name;          // required screen name
    public final List<Expression> parameters;  // optional parameters
    public final String callbackName;  // optional callback function name
    public final boolean isSubscreen;  // true if 'show subscreen', false if 'show screen'

    public ScreenShowStatement(int line, String name) {
        super(line);
        this.name = name;
        this.parameters = null;
        this.callbackName = null;
        this.isSubscreen = false;
    }

    public ScreenShowStatement(int line, String name, List<Expression> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
        this.callbackName = null;
        this.isSubscreen = false;
    }

    public ScreenShowStatement(int line, String name, List<Expression> parameters, String callbackName) {
        super(line);
        this.name = name;
        this.parameters = parameters;
        this.callbackName = callbackName;
        this.isSubscreen = false;
    }

    public ScreenShowStatement(int line, String name, List<Expression> parameters, String callbackName, boolean isSubscreen) {
        super(line);
        this.name = name;
        this.parameters = parameters;
        this.callbackName = callbackName;
        this.isSubscreen = isSubscreen;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitScreenShowStatement(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("show ");
        sb.append(isSubscreen ? "subscreen " : "screen ");
        sb.append(name);
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("(...)");
        }
        if (callbackName != null) {
            sb.append(" callback ").append(callbackName);
        }
        return sb.toString();
    }
}
