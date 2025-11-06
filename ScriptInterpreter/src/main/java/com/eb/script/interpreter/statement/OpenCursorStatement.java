package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import java.util.List;

public class OpenCursorStatement extends Statement {

    public final String name;
    public final List<Parameter> parameters; // may be empty

    public OpenCursorStatement(int line, String name, List<Parameter> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitOpenCursorStatement(this);
    }

    @Override
    public String toString() {
        return "open " + name + "(...)";
    }
}
