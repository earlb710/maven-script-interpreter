package com.eb.script.interpreter.statement;

import com.eb.script.interpreter.InterpreterError;
import java.util.List;

public class CallStatement extends Statement {

    public final String name;
    public Parameter[] parameters;
    public BlockStatement block;
    public Statement[] paramInit;

    public CallStatement(int line, String name) {
        super(line);
        this.name = name;
        this.parameters = null;
    }

    public CallStatement(int line, String name, List<Parameter> parameters) {
        super(line);
        this.name = name;
        this.parameters = parametersToArray(parameters);
    }

//    public void setParameters(List<Parameter> parameters) {
//        this.parameters = parametersToArray(parameters);
//    }
//
    
    public void setBlockStatement(BlockStatement block) {
        this.block = block;
    }

    public void setParamStatments(List<Statement> paramStatements) {
        this.paramInit = statementsToArray(paramStatements);
    }

    private Statement[] statementsToArray(List<Statement> list) {
        if (list != null) {
            return list.toArray(Statement[]::new);
        }
        return null;
    }

    private Parameter[] parametersToArray(List<Parameter> list) {
        if (list != null) {
            return list.toArray(Parameter[]::new);
        }
        return null;
    }
    
    @Override
    public void accept(StatementVisitor visitor) throws InterpreterError {
        visitor.visitCallStatement(this);
    }
}
