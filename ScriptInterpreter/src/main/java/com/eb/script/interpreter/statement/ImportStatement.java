package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;

public class ImportStatement extends Statement {
    public final String filename;

    public ImportStatement(int line, String filename) {
        super(line);
        this.filename = filename;
    }

    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitImportStatement(this);
    }

    @Override
    public String toString() {
        return "import \"" + filename + "\"";
    }
}
