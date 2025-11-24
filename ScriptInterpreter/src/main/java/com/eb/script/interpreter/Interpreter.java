package com.eb.script.interpreter;

import com.eb.script.interpreter.screen.InterpreterScreen;
import com.eb.util.Debugger;
import com.eb.script.RuntimeContext;
import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;
import com.eb.script.token.ebs.EbsToken;
import com.eb.util.Util;
import com.eb.script.interpreter.Builtins.BuiltinInfo;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.db.DbAdapter;
import com.eb.script.interpreter.expression.ArrayExpression;
import com.eb.script.interpreter.expression.ArrayLiteralExpression;
import com.eb.script.interpreter.expression.ExpressionVisitor;
import com.eb.script.interpreter.expression.Expression;
import com.eb.script.interpreter.statement.StatementVisitor;
import com.eb.script.interpreter.statement.Statement;
import com.eb.script.interpreter.expression.LiteralExpression;
import com.eb.script.interpreter.expression.BinaryExpression;
import com.eb.script.interpreter.expression.CallExpression;
import com.eb.script.interpreter.expression.ChainComparisonExpression;
import com.eb.script.interpreter.expression.IndexExpression;
import com.eb.script.interpreter.expression.VariableExpression;
import com.eb.script.interpreter.statement.PrintStatement;
import com.eb.script.interpreter.statement.BlockStatement;
import com.eb.script.interpreter.statement.VarStatement;
import com.eb.script.interpreter.expression.UnaryExpression;
import com.eb.script.interpreter.statement.AssignStatement;
import com.eb.script.interpreter.statement.BreakStatement;
import com.eb.script.interpreter.statement.CallStatement;
import com.eb.script.interpreter.statement.CloseConnectionStatement;
import com.eb.script.interpreter.statement.ContinueStatement;
import com.eb.script.interpreter.statement.CursorStatement;
import com.eb.script.interpreter.statement.DoWhileStatement;
import com.eb.script.interpreter.statement.ForEachStatement;
import com.eb.script.interpreter.statement.ForStatement;
import com.eb.script.interpreter.statement.IfStatement;
import com.eb.script.interpreter.statement.IndexAssignStatement;
import com.eb.script.interpreter.statement.PropertyAssignStatement;
import com.eb.script.interpreter.statement.OpenCursorStatement;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.interpreter.statement.ReturnStatement;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.interpreter.statement.UseConnectionStatement;
import com.eb.script.interpreter.statement.WhileStatement;
import com.eb.script.interpreter.statement.ScreenStatement;
import com.eb.script.interpreter.statement.ScreenShowStatement;
import com.eb.script.interpreter.statement.ScreenHideStatement;
import com.eb.script.interpreter.statement.ScreenCloseStatement;
import com.eb.script.interpreter.statement.ScreenSubmitStatement;
import com.eb.script.interpreter.statement.ImportStatement;
import com.eb.script.interpreter.statement.TypedefStatement;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.script.interpreter.statement.ConnectStatement;
import com.eb.script.parser.Parser;
import com.eb.script.parser.ParseError;
import com.eb.ui.cli.ScriptArea;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Interpreter implements StatementVisitor, ExpressionVisitor {

    private final InterpreterContext context;
    private InterpreterScreen screenInterpreter;
    private InterpreterDatabase databaseInterpreter;
    private InterpreterArray arrayInterpreter;
    private RuntimeContext currentRuntime;  // Store current runtime context for import resolution
    private String currentImportFile;  // Track which import file is currently being processed

    public Interpreter() {
        this.context = new InterpreterContext();
        this.screenInterpreter = new InterpreterScreen(context, this);
        this.databaseInterpreter = new InterpreterDatabase(context, this);
        this.arrayInterpreter = new InterpreterArray(context, this);
    }

    public InterpreterContext getContext() {
        return context;
    }

    public boolean isEchoOn() {
        return context.isEchoOn();
    }

    public void setDbAdapter(DbAdapter adapter) {
        context.setDb(adapter);
    }

    public String currentConnection() {
        return context.getCurrentConnection();
    }
    
    public RuntimeContext getCurrentRuntime() {
        return currentRuntime;
    }
    
    public String getCurrentImportFile() {
        return currentImportFile;
    }

    // Convenience accessors for frequently used context fields
    public Environment environment() {
        return context.getEnvironment();
    }

    private Debugger debug() {
        return context.getDebug();
    }

    private ScriptArea output() {
        return context.getOutput();
    }
    
    // Public method for sub-interpreters to call evaluate
    public Object evaluate(com.eb.script.interpreter.expression.Expression expr) throws InterpreterError {
        if (expr == null) {
            return null;
        }
        return expr.accept(this);
    }
    
    // Public method for sub-interpreters to accept statements
    public void acceptStatement(com.eb.script.interpreter.statement.Statement stmt) throws InterpreterError {
        stmt.accept(this);
    }
    
    // Public method for sub-interpreters to create errors
    public InterpreterError error(int line, String message) {
        message = "Runtime error on line " + line + " : " + message;
        return new InterpreterError(message, environment().getCallStack());
    }

    /**
     * Cleanup all screens and threads. Should be called when the application/console is closing.
     * This ensures all screen windows are closed and their threads are properly terminated.
     */
    public void cleanup() {
        // Close all screens on JavaFX thread
        Platform.runLater(() -> {
            for (Map.Entry<String, Stage> entry : context.getScreens().entrySet()) {
                try {
                    Stage stage = entry.getValue();
                    if (stage != null) {
                        stage.close();
                    }
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
            }
        });

        // Interrupt all screen threads
        for (Map.Entry<String, Thread> entry : context.getScreenThreads().entrySet()) {
            try {
                Thread thread = entry.getValue();
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
        }

        // Clear all maps
        
        context.clear();
    }

    // --- Call stack support ---
    // Each frame is a Map<String,Object> with keys like: name, kind, line.
    // We use fully-qualified types to avoid changing imports.
    public void interpret(RuntimeContext runtime) throws InterpreterError {
        this.currentRuntime = runtime;  // Store for import resolution
        context.setEnvironment(runtime.environment);
        context.setOutput(runtime.environment.getOutputArea());
        
        // Register this interpreter in the environment for cleanup
        environment().setCurrentInterpreter(this);
        
        // Register all functions from the current script BEFORE any imports are processed
        // This ensures we can detect conflicts when imports are executed
        // NOTE: This enforces strict no-overwrite semantics - if a function name exists in the current
        // script, no import can declare a function with the same name, and vice versa.
        // This is stricter than traditional import semantics but is the desired behavior per requirements.
        if (runtime.blocks != null) {
            for (String functionName : runtime.blocks.keySet()) {
                context.getDeclaredFunctions().put(functionName, runtime.name);
            }
        }

        // Set the source directory as safe for file operations during this execution
        if (runtime.sourcePath != null) {
            Path sourceDir = runtime.sourcePath.getParent();
            if (sourceDir != null) {
                com.eb.util.Util.setCurrentContextSourceDir(sourceDir);
            }
        }

        try {
            // Add runtime context name as a predefined variable accessible to scripts
            environment().getEnvironmentValues().define("__name__", runtime.name);

            // Load safe directories with names and define them as global variables
            try {
                Class<?> dialogClass = Class.forName("com.eb.ui.ebs.SafeDirectoriesDialog");
                java.lang.reflect.Method method = dialogClass.getMethod("getSafeDirectoryEntries");
                @SuppressWarnings("unchecked")
                java.util.List<?> entries = (java.util.List<?>) method.invoke(null);

                for (Object entryObj : entries) {
                    // Use reflection to get directory and name from SafeDirectoryEntry
                    java.lang.reflect.Method getDirMethod = entryObj.getClass().getMethod("getDirectory");
                    java.lang.reflect.Method getNameMethod = entryObj.getClass().getMethod("getName");

                    String directory = (String) getDirMethod.invoke(entryObj);
                    String name = (String) getNameMethod.invoke(entryObj);

                    // If the safe directory has a name, define it as a global variable
                    if (name != null && !name.trim().isEmpty()) {
                        environment().getEnvironmentValues().define(name.trim(), directory);
                    }
                }
            } catch (Exception e) {
                // If we can't load safe directories (e.g., class not found in non-UI mode),
                // just continue without defining safe directory variables
            }

            // Load database configurations and define them as global variables
            try {
                Class<?> dbDialogClass = Class.forName("com.eb.ui.ebs.DatabaseConfigDialog");
                java.lang.reflect.Method dbMethod = dbDialogClass.getMethod("getDatabaseConfigEntries");
                @SuppressWarnings("unchecked")
                java.util.List<?> dbEntries = (java.util.List<?>) dbMethod.invoke(null);

                for (Object entryObj : dbEntries) {
                    // Use reflection to get variable name and connection string from DatabaseConfigEntry
                    java.lang.reflect.Method getVarNameMethod = entryObj.getClass().getMethod("getVarName");
                    java.lang.reflect.Method getConnStrMethod = entryObj.getClass().getMethod("getConnectionString");

                    String varName = (String) getVarNameMethod.invoke(entryObj);
                    String connStr = (String) getConnStrMethod.invoke(entryObj);

                    // If both variable name and connection string are present, define as global variable
                    if (varName != null && !varName.trim().isEmpty()
                            && connStr != null && !connStr.trim().isEmpty()) {
                        environment().getEnvironmentValues().define(varName.trim(), connStr);
                    }
                }
            } catch (Exception e) {
                // If we can't load database configs (e.g., class not found in non-UI mode),
                // just continue without defining database variables
            }

            Builtins.setStackSupplier(() -> new java.util.ArrayList<>(environment().getCallStack()));
            environment().clearCallStack();
            environment().pushCallStack(0, StatementKind.SCRIPT, "Script : %1 ", runtime.name);

            for (Statement stmt : runtime.statements) {
                environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "%1", stmt);
                try {
                    if (debug().isDebugTraceOn()) {
                        debug().debugWriteStart("TRACE", "line " + stmt.getLine() + " : " + stmt.getClass().getSimpleName());
                        stmt.accept(this);
                        debug().debugWriteEnd();
                    } else {
                        stmt.accept(this);
                    }

                } finally {
                    environment().popCallStack();
                }
            }
        } finally {
            // Clear the context source directory after execution
            com.eb.util.Util.clearCurrentContextSourceDir();
        }
    }

    // --- Statement Visitors ---
    @Override
    public void visitVarStatement(VarStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Var %1", stmt.name);  // name may be null
        try {
            Object value = evaluate(stmt.initializer);

            if (stmt.varType != null) {
                DataType expectedType = stmt.varType;
                
                // Check for array declarations first (including arrays of records)
                if (stmt.initializer instanceof ArrayExpression array) {
                    // For array declarations, don't convert the array itself
                    // The array already has the correct element type from visitArrayInitExpression
                    if (!Util.checkDataType(array.dataType, value)) {
                        throw error(stmt.getLine(), "Array type mismatch: expected " + expectedType + " for variable '" + stmt.name + "'");
                    }
                } else if (expectedType == DataType.RECORD && stmt.recordType != null) {
                    // Special handling for standalone record types (not arrays)
                    // Convert and validate the value against the record type
                    value = stmt.recordType.convertValue(value);
                    if (!stmt.recordType.validateValue(value)) {
                        throw error(stmt.getLine(), "Record type mismatch for variable '" + stmt.name + "': value does not match record structure");
                    }
                } else {
                    // For non-array values, convert to the expected type
                    value = stmt.varType.convertValue(value);
                    if (!Util.checkDataType(expectedType, value)) {
                        throw error(stmt.getLine(), "Type mismatch: expected " + expectedType + " for variable '" + stmt.name + "'");
                    }
                }
            }
            
            // Store the record type metadata with the variable if it's a record
            if (stmt.recordType != null) {
                environment().getEnvironmentValues().defineWithRecordType(stmt.name, value, stmt.recordType);
            } else {
                environment().getEnvironmentValues().define(stmt.name, value);
            }
        } finally {
            environment().popCallStack();
        }
    }

        @Override
    public ArrayDef visitArrayLiteralExpression(ArrayLiteralExpression expr) throws InterpreterError {
        return arrayInterpreter.visitArrayLiteralExpression(expr);
    }


    

    

        @Override
    public Object visitArrayInitExpression(ArrayExpression expr) throws InterpreterError {
        return arrayInterpreter.visitArrayInitExpression(expr);
    }


    

        @Override
    public Object visitIndexExpression(IndexExpression expr) throws InterpreterError {
        return arrayInterpreter.visitIndexExpression(expr);
    }


    /* ---------- helpers ---------- */
    

    

    /* --------------------------
   Helpers used by the visitors
   -------------------------- */
    

    

    

    

    @Override
    public void visitIfStatement(IfStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.CONDITION, "If (%1)", stmt.condition);  // name may be null
        try {
            Object value = evaluate(stmt.condition);
            if (value instanceof Boolean cond) {
                if (cond) {
                    stmt.thenBranch.accept(this);
                } else {
                    if (stmt.elseBranch != null) {
                        stmt.elseBranch.accept(this);
                    }
                }
            } else {
                throw error(stmt.getLine(), "\"If\" condition expression must be boolean, but is = " + value);
            }
        } finally {
            environment().popCallStack();
        }
    }

    private Boolean evaluateBoolean(Expression condition) throws InterpreterError {
        Object value = evaluate(condition);

        if (value instanceof Boolean cond) {
            return cond;
        } else {
            throw error(condition.getLine(), "Loop condition must be boolean, but is = " + value);
        }
    }

    @Override
    public void visitWhileStatement(WhileStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.LOOP, "While (%1)", stmt.condition);  // name may be null
        try {
            int loopIdx = 0;
            while (evaluateBoolean(stmt.condition)) {
                try {
                    stmt.statement.accept(this);
                    loopIdx++;
                    if (loopIdx <= 0) {
                        throw error(stmt.getLine(), "Infinite loop detected!");
                    }
                } catch (BreakSignal signal) {
                    break;
                } catch (ContinueSignal signal) {
                }
            }
        } finally {
            environment().popCallStack();
        }
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.LOOP, "Do While (%1)", stmt.condition);  // name may be null
        try {
            int loopIdx = 0;
            do {
                try {
                    stmt.statement.accept(this);
                    loopIdx++;
                    if (loopIdx <= 0) {
                        throw error(stmt.getLine(), "Infinite loop detected!");
                    }
                } catch (BreakSignal signal) {
                    break;
                } catch (ContinueSignal signal) {
                }
            } while (evaluateBoolean(stmt.condition));
        } finally {
            environment().popCallStack();
        }
    }

    @Override
    public void visitBreakStatement(BreakStatement stmt) {
        throw BreakSignal.INSTANCE;
    }

    @Override
    public void visitContinueStatement(ContinueStatement stmt) {
        throw ContinueSignal.INSTANCE;
    }

    @Override
    public void visitAssignStatement(AssignStatement stmt) throws InterpreterError {
        Object value = evaluate(stmt.value);

        // Check if this is a record field assignment or screen variable assignment
        String name = stmt.name.toLowerCase();
        int firstDot = name.indexOf('.');
        int secondDot = firstDot > 0 ? name.indexOf('.', firstDot + 1) : -1;

        if (secondDot > 0) {
            // Three-part notation: screen.setName.varName
            String screenName = name.substring(0, firstDot);
            String setName = name.substring(firstDot + 1, secondDot);
            String varName = name.substring(secondDot + 1);
            String fullVarKey = setName + "." + varName;

            // Check if this is a screen variable
            ConcurrentHashMap<String, Object> screenVarMap = context.getScreenVars(screenName);
            if (screenVarMap != null) {
                if (screenVarMap.containsKey(varName)) {
                    // Variable exists with simple name (legacy format)
                    screenVarMap.put(varName, value);
                    // Trigger screen refresh to update UI controls
                    context.triggerScreenRefresh(screenName);
                    return;
                } else {
                    throw error(stmt.getLine(), "Screen '" + screenName + "' does not have a variable '" + setName + "." + varName + "'.");
                }
            }
        } else if (firstDot > 0) {
            // Two-part notation: could be screen.varName or record.field
            String firstPart = name.substring(0, firstDot);
            String secondPart = name.substring(firstDot + 1);

            // Check if this is a screen variable
            ConcurrentHashMap<String, Object> screenVarMap = context.getScreenVars(firstPart);
            if (screenVarMap != null) {
                if (screenVarMap.containsKey(secondPart)) {
                    screenVarMap.put(secondPart, value);
                    // Trigger screen refresh to update UI controls
                    context.triggerScreenRefresh(firstPart);
                    return;
                } else {
                    throw error(stmt.getLine(), "Screen '" + firstPart + "' does not have a variable named '" + secondPart + "'.");
                }
            }
            
            // Not a screen variable - check if it's a record field assignment
            try {
                Object recordObj = environment().getEnvironmentValues().get(firstPart);
                if (recordObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> record = (java.util.Map<String, Object>) recordObj;
                    
                    // Check if the record has type validation
                    RecordType recordType = environment().getEnvironmentValues().getRecordType(firstPart);
                    if (recordType != null) {
                        // Validate the field exists and type matches
                        if (!recordType.hasField(secondPart)) {
                            throw error(stmt.getLine(), "Record '" + firstPart + "' does not have a field named '" + secondPart + "'");
                        }
                        
                        DataType fieldType = recordType.getFieldType(secondPart);
                        value = fieldType.convertValue(value);
                        if (!fieldType.isDataType(value)) {
                            throw error(stmt.getLine(), "Type mismatch for field '" + secondPart + "': expected " + fieldType);
                        }
                    }
                    
                    // Assign the field value
                    record.put(secondPart, value);
                    return;
                } else if (recordObj != null) {
                    throw error(stmt.getLine(), "Cannot assign to field '" + secondPart + "' of non-record variable '" + firstPart + "'");
                }
            } catch (InterpreterError e) {
                // Variable doesn't exist - will fall through to regular assignment
            }
        }

        // Fall back to regular environment variable assignment
        // But first check if this variable has a recordType for validation
        RecordType recordType = environment().getEnvironmentValues().getRecordType(stmt.name);
        
        // Check if we just performed a record() cast and should store the inferred RecordType
        RecordType inferredRecordType = context.getLastInferredRecordType();
        if (inferredRecordType != null) {
            // Clear it immediately to prevent accidental reuse
            context.clearLastInferredRecordType();
            
            // Use the inferred RecordType for this assignment
            recordType = inferredRecordType;
            
            // Store the RecordType metadata with the variable
            environment().getEnvironmentValues().defineWithRecordType(stmt.name, value, recordType);
            return;
        }
        
        if (recordType != null) {
            // Variable has a record type - validate the value
            if (value instanceof ArrayDef) {
                // It's an array of records - validate each element
                ArrayDef<?, ?> arrayDef = (ArrayDef<?, ?>) value;
                
                // Iterate through the array and validate/convert each element
                for (int i = 0; i < arrayDef.size(); i++) {
                    Object element = arrayDef.get(i);
                    if (element != null) {
                        Object converted = recordType.convertValue(element);
                        if (!recordType.validateValue(converted)) {
                            throw error(stmt.getLine(), "Array element " + i + " does not match record structure for '" + stmt.name + "'");
                        }
                        // Update the element with the converted value
                        @SuppressWarnings("unchecked")
                        ArrayDef<Object, ?> typedArray = (ArrayDef<Object, ?>) arrayDef;
                        typedArray.set(i, converted);
                    }
                }
            } else {
                // It's a single record - validate it
                value = recordType.convertValue(value);
                if (!recordType.validateValue(value)) {
                    throw error(stmt.getLine(), "Value does not match record structure for '" + stmt.name + "'");
                }
            }
        }
        environment().getEnvironmentValues().assign(stmt.name, value);
    }

        @Override
    public void visitIndexAssignStatement(IndexAssignStatement stmt) throws InterpreterError {
        arrayInterpreter.visitIndexAssignStatement(stmt);
    }

    @Override
    public void visitPropertyAssignStatement(PropertyAssignStatement stmt) throws InterpreterError {
        // This would be used for more complex property assignments
        // For now, record field assignments are handled in visitAssignStatement
        throw error(stmt.getLine(), "Property assignment not yet fully implemented");
    }


    /* ---- helpers for assignment ---- */
    

    @SuppressWarnings({"unchecked", "rawtypes"})
    

    @Override
    public void visitPrintStatement(PrintStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Print");  // name may be null
        try {
            Object value = evaluate(stmt.expression);
            if (output() == null) {
                System.out.println(Util.stringify(value));
            } else {
                output().println(Util.stringify(value));
            }
        } finally {
            environment().popCallStack();
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement stmt) throws InterpreterError {
        visitBlockStatement(stmt, null);
    }

    public Object visitBlockStatement(BlockStatement stmt, Statement[] parameters) throws InterpreterError {
        environment().pushEnvironmentValues();
        environment().pushCallStack(stmt.getLine(), StatementKind.BLOCK, "Block %1", stmt.name);  // name may be null

        try {
            if (parameters != null) {
                for (Statement param : parameters) {
                    param.accept(this);
                }
            }
            for (Statement statement : stmt.statements) {
                if (statement != null) {
                    statement.accept(this);
                }
            }

        } catch (ReturnSignal r) {
            // Only handle return for named blocks (functions) with a declared return type
            // For anonymous blocks (e.g., if/then blocks), re-throw the signal to propagate it up
            if (stmt.returnType != null) {
                // This is a function block with a return type - validate and return the value
                if (!Util.checkDataType(stmt.returnType, r.value)) {
                    throw error(stmt.getLine(), "Return value '" + r.value + "' not correct type : " + stmt.returnType + " in " + stmt.name);
                }
                return r.value;
            } else {
                // This is an anonymous block - propagate the return signal up
                throw r;
            }
        } finally {
            // POP the frame even on errors/returns
            environment().popCallStack();
            environment().popEnvironmentValues();
        }
        return null;
    }

    // Expression form (e.g., used in "x = #json.get(j, \"path\");")
    @Override
    public Object visitCallExpression(CallExpression expr) throws InterpreterError {
        CallStatement call = expr.call;
        if (Builtins.isBuiltin(call.name)) {
            return evalBuiltin(call);
        } else if (call.block != null) {
            // user-defined block
            return visitBlockStatement(call.block, call.paramInit);
        } else if (currentRuntime != null && currentRuntime.blocks != null) {
            // Check if block was imported at runtime
            BlockStatement block = currentRuntime.blocks.get(call.name);
            if (block != null) {
                // Prepare parameter initialization statements
                Statement[] paramInit = prepareParamInit(call, block);
                return visitBlockStatement(block, paramInit);
            }
        }
        throw error(call.getLine(), "Call cannot find '" + call.name + "'");
    }

// Statement form (e.g., "#json.get(j, \"path\");")
    @Override
    public void visitCallStatement(CallStatement stmt) throws InterpreterError {
        if (Builtins.isBuiltin(stmt.name)) {
            evalBuiltin(stmt); // discard result
            return;
        } else if (stmt.block != null) {
            visitBlockStatement(stmt.block, stmt.paramInit);
            return;
        } else if (currentRuntime != null && currentRuntime.blocks != null) {
            // Check if block was imported at runtime
            BlockStatement block = currentRuntime.blocks.get(stmt.name);
            if (block != null) {
                // Prepare parameter initialization statements
                Statement[] paramInit = prepareParamInit(stmt, block);
                visitBlockStatement(block, paramInit);
                return;
            }
        }
        throw error(stmt.getLine(), "Call cannot find '" + stmt.name + "'");
    }
    
    /**
     * Prepare parameter initialization statements for a runtime-resolved block call
     */
    private Statement[] prepareParamInit(CallStatement call, BlockStatement block) throws InterpreterError {
        if (block.parameters == null || block.parameters.length == 0) {
            return null;
        }
        
        // Match the call parameters to the block's parameter definitions
        Parameter[] blockParams = block.parameters;
        Parameter[] callParams = call.parameters;
        
        Statement[] paramInit = new Statement[blockParams.length];
        for (int i = 0; i < blockParams.length; i++) {
            Parameter blockParam = blockParams[i];
            if (callParams != null && i < callParams.length && callParams[i] != null) {
                // Use the value from the call
                paramInit[i] = new VarStatement(call.getLine(), blockParam.name, blockParam.paramType, callParams[i].value);
            } else {
                // Use the default value from the block definition if available
                paramInit[i] = new VarStatement(call.getLine(), blockParam.name, blockParam.paramType, blockParam.value);
            }
        }
        return paramInit;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement stmt) throws InterpreterError {
        // If value is null (return;), return null
        Object returnValue = (stmt.value != null) ? evaluate(stmt.value) : null;
        throw new ReturnSignal(returnValue);
    }

    // --- Expression Visitors ---
    @Override
    public Object visitLiteralExpression(LiteralExpression expr) {
        Object value = expr.value;
        if (value instanceof String str) {
            // Try to parse as date or datetime
            try {
                if (str.contains(" ")) {
                    return LocalDateTime.parse(str);
                } else if (str.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(str);
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return value;
    }

    @Override
    public Object visitVariableExpression(VariableExpression expr) throws InterpreterError {
        // Check if this is a screen variable access (screen_name.set_name.var_name)
        String name = expr.name.toLowerCase();
        int firstDot = name.indexOf('.');
        int secondDot = firstDot > 0 ? name.indexOf('.', firstDot + 1) : -1;

        if (secondDot > 0) {
            // Three-part notation: screen.setName.varName
            String screenName = name.substring(0, firstDot);
            String setName = name.substring(firstDot + 1, secondDot);
            String varName = name.substring(secondDot + 1);
            String fullVarKey = setName + "." + varName;

            // Check if this is a screen variable
            ConcurrentHashMap<String, Object> screenVarMap = context.getScreenVars(screenName);
            if (screenVarMap != null) {
                if (screenVarMap.containsKey(varName)) {
                    // Variable exists with simple name (legacy format)
                    return screenVarMap.get(varName);
                } else {
                    throw error(expr.line, "Screen '" + screenName + "' does not have a variable '" + setName + "." + varName + "'.");
                }
            }
        } else if (firstDot > 0) {
            // Two-part notation for backward compatibility: screen.varName
            String screenName = name.substring(0, firstDot);
            String varName = name.substring(firstDot + 1);

            // Check if this is a screen variable
            ConcurrentHashMap<String, Object> screenVarMap = context.getScreenVars(screenName);
            if (screenVarMap != null && screenVarMap.containsKey(varName)) {
                // Found screen variable, return it
                return screenVarMap.get(varName);
            }
            // If not a screen variable, fall through to check regular environment variables
        }

        // Fall back to regular environment variable
        return environment().get(expr.name);
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression expr) throws InterpreterError {
        // Special handling for typeof operator
        if (expr.operator.type == EbsTokenType.TYPEOF) {
            // If the operand is a variable, we can look up its record type metadata
            if (expr.right instanceof VariableExpression) {
                VariableExpression varExpr = (VariableExpression) expr.right;
                String varName = varExpr.name;
                Object value = evaluate(expr.right);
                RecordType recordType = environment().getEnvironmentValues().getRecordType(varName);
                return getTypeString(value, recordType);
            } else {
                // For other expressions, just evaluate and get the runtime type
                Object value = evaluate(expr.right);
                return getTypeString(value, null);
            }
        }
        
        // For other unary operators, evaluate the right side first
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                if (right instanceof Integer v) {
                    return -v;
                } else if (right instanceof Long v) {
                    return -v;
                } else if (right instanceof Float v) {
                    return -v;
                } else if (right instanceof Double v) {
                    return -v;
                }
                throw error(expr.line, "Operand must be a number for unary '-'.");

            case PLUS:
                if (right instanceof Number v) {
                    return v;
                }
                throw error(expr.line, "Operand must be a number for unary '+'.");
            case BOOL_BANG: // if you add logical not
                Boolean b = Util.isTruthy(right);
                if (b == null) {
                    throw error(expr.line, "Operand must be a boolean for unary '!'.");
                }
                return !b;
        }
        return null;
    }

    private Object evalOperator(int line, Object left, EbsTokenType operator, Object right) throws InterpreterError {
        // Handle null equality/inequality checks BEFORE type coercion
        // This allows proper null validation (e.g., "if a == null")
        if (operator == EbsTokenType.BOOL_EQ || operator == EbsTokenType.BOOL_NEQ) {
            if (left == null || right == null) {
                boolean isEqual = (left == null && right == null);
                return operator == EbsTokenType.BOOL_EQ ? isEqual : !isEqual;
            }
        }
        
        if (left == null) {
            if (right != null) {
                switch (right) {
                    case String v2 ->
                        left = "";
                    case Long v2 ->
                        left = 0;
                    case Float v2 ->
                        left = 0;
                    case Double v2 ->
                        left = 0;
                    case Boolean v2 ->
                        left = false;
                    default -> {
                    }
                }
            }
        } else if (right == null) {
            switch (left) {
                case String v2 ->
                    left = "";
                case Long v2 ->
                    left = 0;
                case Float v2 ->
                    left = 0;
                case Double v2 ->
                    left = 0;
                case Boolean v2 ->
                    left = false;
                default -> {
                }
            }
        } else {
            switch (left) {
                case String v1 -> {
                    switch (right) {
                        case Long v2 ->
                            right = String.valueOf(v2);
                        case Float v2 ->
                            right = String.valueOf(v2);
                        case Double v2 ->
                            right = String.valueOf(v2);
                        case Boolean v2 ->
                            right = Util.stringBoolean(v2);
                        default -> {
                        }
                    }
                }
                case Integer v1 -> {
                    switch (right) {
                        case Long v2 ->
                            left = (Long) (long) v1;
                        case Float v2 ->
                            left = (Float) (float) v1;
                        case Double v2 ->
                            left = (Double) (double) v1;
                        case String v2 ->
                            left = String.valueOf(v1);
                        default -> {
                        }
                    }
                }
                case Long v1 -> {
                    switch (right) {
                        case Integer v2 ->
                            right = (Long) (long) v2;
                        case Float v2 -> {
                            left = (Double) (double) v1;
                            right = (Double) (double) v2;
                        }
                        case Double v2 ->
                            left = (Double) (double) v1;
                        case String v2 ->
                            left = String.valueOf(v1);
                        default -> {
                        }
                    }
                }
                case Float v1 -> {
                    switch (right) {
                        case Double v2 ->
                            left = (Double) (double) v1;
                        case Integer v2 ->
                            right = (Float) (float) v2;
                        case Long v2 -> {
                            left = (Double) (double) v1;
                            right = (Double) (double) v2;
                        }
                        case String v2 ->
                            left = String.valueOf(v1);
                        default -> {
                        }
                    }

                }
                case Double v1 -> {
                    switch (right) {
                        case Float v2 ->
                            right = (Double) (double) v2;
                        case Integer v2 ->
                            right = (Double) (double) v2;
                        case Long v2 ->
                            right = (Double) (double) v2;
                        case String v2 ->
                            left = String.valueOf(v1);
                        default -> {
                        }
                    }
                }
                case Boolean v1 -> {
                    switch (right) {
                        case String v2 ->
                            left = Util.stringBoolean(v1);
                        default -> {
                        }
                    }
                }
                default -> {
                }
            }
        }
        switch (operator) {
            case BOOL_AND -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return v1 && v2;
                }
                throw error(line, "Operands must be of type boolean for '" + operator + "'");
            }
            case BOOL_OR -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return v1 || v2;
                }
                throw error(line, "Operands must be of type boolean for '" + operator + "'");
            }
            case BOOL_NEQ -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return !v1.equals(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return !v1.equals(v2);
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return !v1.equals(v2);
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return !v1.equals(v2);
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return !v1.equals(v2);
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return !v1.equals(v2);
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }
            case BOOL_EQ -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return v1.equals(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1.equals(v2);
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1.equals(v2);
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1.equals(v2);
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1.equals(v2);
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return v1.equals(v2);
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }

            case BOOL_GT -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return Util.intBoolean(v1) > Util.intBoolean(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 > v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 > v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 > v2;
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 > v2;
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return v1.compareTo(v2) > 0;
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }

            case BOOL_GT_EQ -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return Util.intBoolean(v1) >= Util.intBoolean(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 >= v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 >= v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 >= v2;
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 >= v2;
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return v1.compareTo(v2) >= 0;
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }

            case BOOL_LT -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return Util.intBoolean(v1) < Util.intBoolean(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 < v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 < v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 < v2;
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 < v2;
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return v1.compareTo(v2) < 0;
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }

            case BOOL_LT_EQ -> {
                if (left instanceof Boolean v1 && right instanceof Boolean v2) {
                    return Util.intBoolean(v1) <= Util.intBoolean(v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 <= v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 <= v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 <= v2;
                } else if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 <= v2;
                } else if (left instanceof String v1 && right instanceof String v2) {
                    return v1.compareTo(v2) <= 0;
                }
                throw error(line, "Operands must be same type for '" + operator + "'");
            }

            case PLUS -> {
                if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 + v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 + v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 + v2;
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 + v2;
                }
                return Util.stringify(left) + Util.stringify(right); // string or date concatenation
            }

            case MINUS -> {
                if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 - v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 - v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 - v2;
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 - v2;
                }
                throw error(line, "Operands must be numbers for '" + operator + "'");
            }

            case STAR -> {
                if (left instanceof Double v1 && right instanceof Double v2) {
                    return v1 * v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return v1 * v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return v1 * v2;
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return v1 * v2;
                }
                throw error(line, "Operands must be numbers for '" + operator + "'");
            }

            case SLASH -> {
                if (left instanceof Double v1 && right instanceof Double v2) {
                    if (v2 == 0) {
                        throw error(line, "Division by zero");
                    }
                    return v1 / v2;
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    if (v2 == 0) {
                        throw error(line, "Division by zero");
                    }
                    return v1 / v2;
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    if (v2 == 0) {
                        throw error(line, "Division by zero");
                    }
                    return v1 / v2;
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    if (v2 == 0) {
                        throw error(line, "Division by zero");
                    }
                    return v1 / v2;
                }
                throw error(line, "Operands must be numbers for '" + operator + "'");
            }

            case CARET -> {
                if (left instanceof Double v1 && right instanceof Double v2) {
                    return Math.pow(v1, v2);
                } else if (left instanceof Float v1 && right instanceof Float v2) {
                    return Math.pow(v1, v2);
                } else if (left instanceof Long v1 && right instanceof Long v2) {
                    return Math.pow(v1, v2);
                } else if (left instanceof Integer v1 && right instanceof Integer v2) {
                    return Math.pow(v1, v2);
                }
                throw error(line, "Operands must be numbers for '" + operator + "'");
            }

            default ->
                throw error(line, "Unsupported operator: " + operator);
        }
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression expr) throws InterpreterError {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return evalOperator(expr.line, left, expr.operator.type, right);
    }

    @Override
    public Object visitLengthExpression(com.eb.script.interpreter.expression.LengthExpression expr) throws InterpreterError {
        Object target = evaluate(expr.target);
        if (target instanceof ArrayDef arr) {
            return arr.size();
        }

        if (target instanceof List<?> list) {
            return list.size();
        }

        if (target instanceof Map<?, ?> map) {
            return map.size();
        }

        if (target != null && target.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(target);
        }

        if (target instanceof String str) {
            return str.length();
        }

        throw error( // choose appropriate line source if needed
                (expr.target instanceof VariableExpression v) ? v.line : 0,
                "'.length' or '.size' can only be used on arrays and strings."
        );
    }

    @Override
    public Object visitChainComparisonExpression(ChainComparisonExpression expr) throws InterpreterError {
        Object leftVal = evaluate(expr.operands[0]);

        for (int i = 0; i < expr.operators.length; i++) {
            EbsToken op = expr.operators[i];
            Object rightVal = evaluate(expr.operands[i + 1]);
            Object eval = evalOperator(expr.line, leftVal, op.type, rightVal);
            if (eval instanceof Boolean ok) {
                if (!ok) {
                    return false; // short-circuit failure
                }
            } else {
                return false;
            }
            leftVal = rightVal; // next comparison's left is current right
        }
        return true;
    }

    @Override
    public void visitForEachStatement(ForEachStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.LOOP, "foreach %1", stmt.varName);
        try {
            final Object it = evaluate(stmt.iterable);

            java.util.function.Consumer<Object> runBodyWith = (elem) -> {
                environment().pushEnvironmentValues();
                try {
                    environment().getEnvironmentValues().define(stmt.varName, elem);
                    try {
                        stmt.body.accept(this);
                    } catch (ContinueSignal c) {
                        // skip to next
                    } catch (InterpreterError ex) {
                        throw new RuntimeException(ex.getMessage());
                    }
                } finally {
                    environment().popEnvironmentValues();
                }
            };

            try {
                if (it == null) {
                    throw error(stmt.getLine(), "foreach target is null");
                } else if (it instanceof ArrayDef<?, ?> arr) {
                    int n = arr.size();                        // ArrayDef API
                    for (int i = 0; i < n; i++) {
                        try {
                            runBodyWith.accept(arr.get(i));
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else if (it.getClass().isArray()) {
                    int n = java.lang.reflect.Array.getLength(it);
                    for (int i = 0; i < n; i++) {
                        Object elem = java.lang.reflect.Array.get(it, i);
                        try {
                            runBodyWith.accept(elem);
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else if (it instanceof Map<?, ?> m) {
                    for (Object k : m.keySet()) {
                        try {
                            runBodyWith.accept(k);
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else if (it instanceof List<?> list) {
                    for (Object elem : list) {
                        try {
                            runBodyWith.accept(elem);
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else if (it instanceof Iterable<?> each) {
                    for (Object elem : each) {
                        try {
                            runBodyWith.accept(elem);
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else if (it instanceof String s) {
                    for (int i = 0; i < s.length(); i++) {
                        try {
                            runBodyWith.accept(String.valueOf(s.charAt(i)));
                        } catch (BreakSignal b) {
                            break;
                        }
                    }
                } else {
                    throw error(stmt.getLine(), "Value is not iterable: " + Util.stringify(it));
                }
            } finally {
                // nothing
            }
        } finally {
            environment().popCallStack();
        }
    }

    @Override
    public void visitForStatement(ForStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.LOOP, "for");
        try {
            // Create a new scope for the loop
            environment().pushEnvironmentValues();
            try {
                // Execute initializer
                if (stmt.initializer != null) {
                    stmt.initializer.accept(this);
                }

                // Loop while condition is true
                while (true) {
                    // Evaluate condition (if null, treat as true for infinite loop)
                    if (stmt.condition != null) {
                        if (!evaluateBoolean(stmt.condition)) {
                            break;
                        }
                    }

                    // Execute body
                    try {
                        stmt.body.accept(this);
                    } catch (ContinueSignal c) {
                        // Continue to increment
                    } catch (BreakSignal b) {
                        // Break out of loop
                        break;
                    }

                    // Execute increment
                    if (stmt.increment != null) {
                        stmt.increment.accept(this);
                    }
                }
            } finally {
                environment().popEnvironmentValues();
            }
        } finally {
            environment().popCallStack();
        }
    }

    @Override
    public void visitConnectStatement(ConnectStatement stmt) throws InterpreterError {
        databaseInterpreter.visitConnectStatement(stmt);
    }

    @Override
    public void visitCloseConnectionStatement(CloseConnectionStatement stmt) throws InterpreterError {
        databaseInterpreter.visitCloseConnectionStatement(stmt);
    }

    @Override
    public void visitScreenStatement(ScreenStatement stmt) throws InterpreterError {
        screenInterpreter.visitScreenStatement(stmt);
    }

    @Override
    public void visitScreenShowStatement(ScreenShowStatement stmt) throws InterpreterError {
        screenInterpreter.visitScreenShowStatement(stmt);
    }

    @Override
    public void visitScreenHideStatement(ScreenHideStatement stmt) throws InterpreterError {
        screenInterpreter.visitScreenHideStatement(stmt);
    }

    @Override
    public void visitScreenCloseStatement(ScreenCloseStatement stmt) throws InterpreterError {
        screenInterpreter.visitScreenCloseStatement(stmt);
    }

    @Override
    public void visitScreenSubmitStatement(ScreenSubmitStatement stmt) throws InterpreterError {
        screenInterpreter.visitScreenSubmitStatement(stmt);
    }

    @Override
    public void visitImportStatement(ImportStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Import %1", stmt.filename);
        try {
            // Resolve the file path
            Path importPath = resolveImportPath(stmt.filename);
            
            if (!Files.exists(importPath)) {
                throw error(stmt.getLine(), "Import file not found: " + stmt.filename);
            }
            
            // Normalize the import path as written by the user, maintaining the relative structure
            // This provides a consistent representation while preserving the user's intent
            Path normalizedImportPath = Path.of(stmt.filename).normalize();
            String importKey = normalizedImportPath.toString();
            
            // Check if file has already been imported (prevents circular imports and duplicate imports)
            // We use the normalized form of the import path as written by the user
            if (context.getImportedFiles().contains(importKey)) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Skipped (already imported): " + stmt.filename);
                }
                return;  // Skip re-importing
            }
            
            // Add to imported files set before processing to prevent circular imports
            // Store the import path as normalized from what the user wrote
            context.getImportedFiles().add(importKey);
            
            // Read and parse the imported file
            String importedScript = Files.readString(importPath, StandardCharsets.UTF_8);
            RuntimeContext importContext = Parser.parse(importPath.getFileName().toString(), importedScript);
            
            // Set the current import file so that screen/function declarations know their source
            String previousImportFile = currentImportFile;
            currentImportFile = stmt.filename;
            
            try {
                // Execute the imported script in the current environment
                for (Statement s : importContext.statements) {
                    s.accept(this);
                }
            } finally {
                // Restore previous import file context
                currentImportFile = previousImportFile;
            }
            
            // Register imported blocks/functions in the current runtime context
            // Check for conflicts to enforce strict no-overwrite semantics
            if (importContext.blocks != null && currentRuntime != null) {
                for (Map.Entry<String, BlockStatement> entry : importContext.blocks.entrySet()) {
                    String functionName = entry.getKey();
                    
                    // Check if this function name was already declared (in current script or previous import)
                    // This enforces the requirement that no function name can be reused across the current
                    // script and any imports, preventing all forms of overwriting
                    if (context.getDeclaredFunctions().containsKey(functionName)) {
                        String existingSource = context.getDeclaredFunctions().get(functionName);
                        throw error(stmt.getLine(), 
                            "Function '" + functionName + "' is already declared in " + existingSource + 
                            " and cannot be overwritten by import from " + stmt.filename);
                    }
                    
                    // Register this function as declared from the imported file
                    context.getDeclaredFunctions().put(functionName, stmt.filename);
                    
                    // Store blocks so they can be called later
                    currentRuntime.blocks.put(functionName, entry.getValue());
                }
            }
            
            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Imported: " + stmt.filename);
            }
        } catch (IOException e) {
            throw error(stmt.getLine(), "Failed to read import file: " + e.getMessage());
        } catch (ParseError e) {
            throw error(stmt.getLine(), "Failed to parse import file: " + e.getMessage());
        } finally {
            environment().popCallStack();
        }
    }
    
    @Override
    public void visitTypedefStatement(TypedefStatement stmt) throws InterpreterError {
        environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Typedef %1", stmt.typeName);
        try {
            // Register the type alias in the global type registry
            TypeRegistry.TypeAlias alias = new TypeRegistry.TypeAlias(
                stmt.typeName,
                stmt.dataType,
                stmt.recordType,
                stmt.isArray,
                stmt.arraySize
            );
            TypeRegistry.registerTypeAlias(alias);
            
            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Type alias defined: " + stmt.typeName + " = " + alias.toString());
            }
        } finally {
            environment().popCallStack();
        }
    }
    
    /**
     * Resolve import file path relative to current script location or working directory
     */
    private Path resolveImportPath(String filename) {
        // First try relative to current script directory
        if (currentRuntime != null && currentRuntime.sourcePath != null) {
            Path scriptDir = currentRuntime.sourcePath.getParent();
            if (scriptDir != null) {
                Path resolvedPath = scriptDir.resolve(filename);
                if (Files.exists(resolvedPath)) {
                    return resolvedPath;
                }
            }
        }
        
        // Fall back to current working directory
        return Path.of(filename);
    }

    @Override
    public void visitUseConnectionStatement(UseConnectionStatement stmt) throws InterpreterError {
        databaseInterpreter.visitUseConnectionStatement(stmt);
    }

    @Override
    public void visitCursorStatement(CursorStatement stmt) throws InterpreterError {
        databaseInterpreter.visitCursorStatement(stmt);
    }

    @Override
    public void visitOpenCursorStatement(OpenCursorStatement stmt) throws InterpreterError {
        databaseInterpreter.visitOpenCursorStatement(stmt);
    }

    @Override
    public void visitCloseCursorStatement(com.eb.script.interpreter.statement.CloseCursorStatement stmt) throws InterpreterError {
        databaseInterpreter.visitCloseCursorStatement(stmt);
    }

    @Override
    public Object visitSqlSelectExpression(com.eb.script.interpreter.expression.SqlSelectExpression expr) throws InterpreterError {
        return databaseInterpreter.visitSqlSelectExpression(expr);
    }

    @Override
    public Object visitCursorHasNextExpression(com.eb.script.interpreter.expression.CursorHasNextExpression expr) throws InterpreterError {
        return databaseInterpreter.visitCursorHasNextExpression(expr);
    }

    @Override
    public Object visitCursorNextExpression(com.eb.script.interpreter.expression.CursorNextExpression expr) throws InterpreterError {
        return databaseInterpreter.visitCursorNextExpression(expr);
    }

    @Override
    public Object visitPropertyExpression(com.eb.script.interpreter.expression.PropertyExpression expr) throws InterpreterError {
        Object obj = evaluate(expr.object);
        
        if (obj == null) {
            throw error(expr.getLine(), "Cannot access property '" + expr.propertyName + "' of null");
        }
        
        // Check if the object is a Map (record or JSON object)
        if (obj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            
            // Check if the property exists
            if (!map.containsKey(expr.propertyName)) {
                throw error(expr.getLine(), "Property '" + expr.propertyName + "' does not exist in record");
            }
            
            return map.get(expr.propertyName);
        }
        
        throw error(expr.getLine(), "Cannot access property '" + expr.propertyName + "' on non-record type: " + obj.getClass().getSimpleName());
    }

    @Override
    public Object visitCastExpression(com.eb.script.interpreter.expression.CastExpression expr) throws InterpreterError {
        // Evaluate the value to be cast
        Object value = evaluate(expr.value);
        
        // Special handling for RECORD casting
        if (expr.targetType == DataType.RECORD) {
            // Validate that the value is a Map (not an array/List)
            if (value instanceof java.util.List || value instanceof ArrayDef) {
                throw error(expr.getLine(), "Cannot cast JSON array to record. Only JSON objects (maps) can be cast to record type.");
            }
            
            if (!(value instanceof java.util.Map)) {
                String typeName = (value == null) ? "null" : value.getClass().getSimpleName();
                throw error(expr.getLine(), "Cannot cast " + typeName + " to record. Only JSON objects (maps) can be cast to record type.");
            }
            
            // Infer RecordType from the Map structure
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            RecordType inferredType = inferRecordType(map);
            
            // Store the inferred RecordType in a thread-local or context for later retrieval
            // For now, we'll use the lastInferredRecordType field
            context.setLastInferredRecordType(inferredType);
            
            return value;
        }
        
        // Use DataType.convertValue() to perform the actual conversion for other types
        try {
            Object converted = expr.targetType.convertValue(value);
            return converted;
        } catch (Exception e) {
            throw error(expr.getLine(), "Cannot cast value '" + value + "' to type " + expr.targetType + ": " + e.getMessage());
        }
    }
    
    /**
     * Infer a RecordType from a Map's structure by examining its keys and values.
     */
    private RecordType inferRecordType(java.util.Map<String, Object> map) {
        RecordType recordType = new RecordType();
        
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            // Infer the DataType for this field
            DataType fieldType = inferDataType(fieldValue);
            
            // If the field is itself a record (nested Map), recurse
            if (fieldType == DataType.RECORD && fieldValue instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> nestedMap = (java.util.Map<String, Object>) fieldValue;
                RecordType nestedRecordType = inferRecordType(nestedMap);
                recordType.addNestedRecord(fieldName, nestedRecordType);
            } else {
                recordType.addField(fieldName, fieldType);
            }
        }
        
        return recordType;
    }
    
    /**
     * Infer DataType from a runtime value.
     */
    private DataType inferDataType(Object value) {
        if (value == null) {
            return DataType.STRING; // Default to STRING for null
        } else if (value instanceof String) {
            return DataType.STRING;
        } else if (value instanceof Integer) {
            return DataType.INTEGER;
        } else if (value instanceof Long) {
            return DataType.LONG;
        } else if (value instanceof Float) {
            return DataType.FLOAT;
        } else if (value instanceof Double) {
            return DataType.DOUBLE;
        } else if (value instanceof Boolean) {
            return DataType.BOOL;
        } else if (value instanceof Byte) {
            return DataType.BYTE;
        } else if (value instanceof java.util.Map) {
            return DataType.RECORD;
        } else if (value instanceof java.util.List || value instanceof ArrayDef) {
            return DataType.ARRAY;
        } else if (value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate || value instanceof Date) {
            return DataType.DATE;
        } else {
            return DataType.STRING; // Default to STRING for unknown types
        }
    }

    // --- Helpers ---

    /**
     * Get the type string representation for a value.
     * For records, if recordType metadata is available, return the full structure.
     * Otherwise, return the basic type name.
     */
    private String getTypeString(Object value, RecordType recordType) {
        if (value == null) {
            return "null";
        }
        
        // Check for arrays first, as they may contain records
        if (value instanceof ArrayDef) {
            // Get detailed array type information
            ArrayDef<?, ?> arrayDef = (ArrayDef<?, ?>) value;
            DataType elementType = arrayDef.getDataType();
            boolean isFixed = arrayDef.isFixed();
            int size = arrayDef.size();
            
            // Build the array type string
            StringBuilder sb = new StringBuilder("array.");
            
            // Add element type
            if (elementType == DataType.RECORD && recordType != null) {
                // For arrays of records, format as: array.record[size] {fields}
                sb.append("record");
                
                // Add array size information after "record"
                if (isFixed) {
                    // Always show size for fixed arrays, even if 0
                    sb.append("[").append(size).append("]");
                } else {
                    // Dynamic arrays show empty brackets
                    sb.append("[]");
                }
                
                // Add the record structure (just the fields part)
                // RecordType.toString() returns "record {fields}", so we strip "record " prefix
                String recordStr = recordType.toString();
                String recordPrefix = "record ";
                if (recordStr.startsWith(recordPrefix)) {
                    sb.append(" ").append(recordStr.substring(recordPrefix.length()));
                } else {
                    sb.append(" ").append(recordStr);
                }
            } else {
                // For primitive types, use the lowercase type name
                sb.append(getDataTypeName(elementType));
                
                // Add array size information
                if (isFixed) {
                    // Always show size for fixed arrays, even if 0
                    sb.append("[").append(size).append("]");
                } else {
                    // Dynamic arrays show empty brackets
                    sb.append("[]");
                }
            }
            
            return sb.toString();
        }
        
        // If we have record type metadata and it's not an array, use it to construct the full type string
        if (recordType != null) {
            return recordType.toString();
        }
        
        // Otherwise, determine type from the runtime value
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Long) {
            return "long";
        } else if (value instanceof Float) {
            return "float";
        } else if (value instanceof Double) {
            return "double";
        } else if (value instanceof Boolean) {
            return "bool";
        } else if (value instanceof Byte) {
            return "byte";
        } else if (value instanceof java.util.Map) {
            // It's a record/JSON but we don't have metadata
            // Try to infer the structure from the runtime value
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            StringBuilder sb = new StringBuilder("record {");
            boolean first = true;
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(entry.getKey()).append(":");
                sb.append(getSimpleTypeName(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof java.util.List) {
            // Plain List (not ArrayDef) - ArrayDef arrays are handled above
            return "array";
        } else if (value instanceof LocalDateTime || value instanceof LocalDate) {
            return "date";
        }
        
        return value.getClass().getSimpleName().toLowerCase();
    }
    
    /**
     * Get a simple type name for a value (used when inferring record field types)
     */
    private String getSimpleTypeName(Object value) {
        if (value == null) {
            return "any";
        } else if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Long) {
            return "long";
        } else if (value instanceof Float) {
            return "float";
        } else if (value instanceof Double) {
            return "double";
        } else if (value instanceof Boolean) {
            return "bool";
        } else if (value instanceof Byte) {
            return "byte";
        } else if (value instanceof java.util.Map) {
            return "record";
        } else if (value instanceof java.util.List || value instanceof ArrayDef) {
            return "array";
        } else if (value instanceof LocalDateTime || value instanceof LocalDate) {
            return "date";
        }
        return "any";
    }
    
    /**
     * Get a lowercase type name from a DataType enum value.
     */
    private String getDataTypeName(DataType type) {
        if (type == null) {
            return "any";
        }
        return switch (type) {
            case BYTE -> "byte";
            case INTEGER -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case STRING -> "string";
            case DATE -> "date";
            case BOOL -> "bool";
            case JSON -> "json";
            case ARRAY -> "array";
            case RECORD -> "record";
            default -> "any";
        };
    }

    private Object evalBuiltin(CallStatement c) throws InterpreterError {
        String name = c.name;
        int len = 0;
        if (c.parameters != null) {
            len = c.parameters.length;
        }
        BuiltinInfo info = Builtins.getBuiltinInfo(name);
        Parameter[] params = info.params;
        Object[] args = new Object[len];
        if (len > 0) {
            int idx = 0;
            for (Parameter p : c.parameters) {
                args[idx] = (p == null) ? null : evaluate(p.value);
                DataType ptype = params[idx].paramType;
                if (ptype != DataType.JSON && ptype != DataType.ANY && ptype != DataType.ANY && !Util.checkDataType(ptype, args[idx])) {
                    args[idx] = ptype.convertValue(args[idx]);
                    if (!Util.checkDataType(ptype, args[idx])) {
                        throw error(c.getLine(), "Call to [" + info.name + "] parameter [" + params[idx].name + ":" + params[idx].paramType + "] wrong type, expected " + params[idx].paramType + " but found " + args[idx].getClass().getName());
                    }
                }
                idx++;
            }
        }

        // PUSH a builtin frame
        environment().pushCallStack(c.getLine(), StatementKind.BUILTIN, "Call %1", name);
        try {
            return Builtins.callBuiltin(context, name, args);
        } catch (Exception ex) {
            throw error(c.getLine(), "Call Builtin -> " + ex.getMessage());
        } finally {
            // POP even on failure
            environment().popCallStack();
        }
    }

    public static final class CursorSpec {

        final String connectionName;
        final String sql;
        final int line;

        CursorSpec(String connectionName, String sql, int line) {
            this.connectionName = connectionName;
            this.sql = sql;
            this.line = line;
        }
    }

    public static class BreakSignal extends RuntimeException {

        public static final BreakSignal INSTANCE = new BreakSignal();

        private BreakSignal() {
            super(null, null, false, false); // no message, no cause, no suppression, no stack trace
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; // keep it cheap
        }
    }

    public static class ContinueSignal extends RuntimeException {

        public static final ContinueSignal INSTANCE = new ContinueSignal();

        private ContinueSignal() {
            super(null, null, false, false); // no message, no cause, no suppression, no stack trace
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; // keep it cheap
        }
    }

    public static final class ReturnSignal extends RuntimeException {

        public final Object value;

        public ReturnSignal(Object v) {
            super(null, null, false, false);
            this.value = v;
        }
    }

// Assign a literal array (whose elements evaluate to Strings) into a predefined ArrayDef,
// converting each element to the target DataType and expanding if the array is dynamic.
    

}
