package com.eb.script.interpreter;

import com.eb.util.Debugger;
import com.eb.script.RuntimeContext;
import com.eb.script.token.DataType;
import com.eb.script.token.ebs.EbsToken;
import com.eb.util.Util;
import com.eb.script.interpreter.Builtins.BuiltinInfo;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.db.DbAdapter;
import com.eb.script.interpreter.db.DbConnection;
import com.eb.script.interpreter.db.DbCursor;
import com.eb.script.interpreter.db.OracleDbAdapter;
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
import com.eb.script.interpreter.statement.IfStatement;
import com.eb.script.interpreter.statement.IndexAssignStatement;
import com.eb.script.interpreter.statement.OpenCursorStatement;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.interpreter.statement.ReturnStatement;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.interpreter.statement.UseConnectionStatement;
import com.eb.script.interpreter.statement.WhileStatement;
import com.eb.script.interpreter.statement.ScreenStatement;
import com.eb.script.interpreter.statement.ScreenShowStatement;
import com.eb.script.interpreter.statement.ScreenHideStatement;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.script.interpreter.DisplayMetadata.ItemType;
import com.eb.script.interpreter.AreaDefinition.AreaType;
import com.eb.script.interpreter.AreaDefinition.AreaItem;
import com.eb.script.interpreter.statement.ConnectStatement;
import com.eb.ui.cli.ScriptArea;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class Interpreter implements StatementVisitor, ExpressionVisitor {

    private Environment environment;
    private Debugger debug;

    private final java.util.Map<String, DbConnection> connections = new java.util.HashMap<>();
    private final java.util.Map<String, CursorSpec> cursorSpecs = new java.util.HashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Stage> screens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Thread> screenThreads = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, Object>> screenVars = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, DisplayMetadata> displayMetadata = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.List<AreaDefinition>> screenAreas = new java.util.HashMap<>();
    private final java.util.Deque<String> connectionStack = new java.util.ArrayDeque<>();
    private DbAdapter db = new OracleDbAdapter();
    private ScriptArea output;

    public boolean isEchoOn() {
        return environment.isEchoOn();
    }

    public void setDbAdapter(DbAdapter adapter) {
        this.db = (adapter == null ? DbAdapter.NOOP : adapter);
    }

    private String currentConnection() {
        return connectionStack.peek();
    }

    // --- Call stack support ---
    // Each frame is a Map<String,Object> with keys like: name, kind, line.
    // We use fully-qualified types to avoid changing imports.
    public void interpret(RuntimeContext runtime) throws InterpreterError {
        environment = runtime.environment;
        output = environment.getOutputArea();
        debug = environment.getDebugger();

        // Set the source directory as safe for file operations during this execution
        if (runtime.sourcePath != null) {
            Path sourceDir = runtime.sourcePath.getParent();
            if (sourceDir != null) {
                com.eb.util.Util.setCurrentContextSourceDir(sourceDir);
            }
        }

        try {
            // Add runtime context name as a predefined variable accessible to scripts
            environment.getEnvironmentValues().define("__name__", runtime.name);

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
                        environment.getEnvironmentValues().define(name.trim(), directory);
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
                        environment.getEnvironmentValues().define(varName.trim(), connStr);
                    }
                }
            } catch (Exception e) {
                // If we can't load database configs (e.g., class not found in non-UI mode),
                // just continue without defining database variables
            }

            Builtins.setStackSupplier(() -> new java.util.ArrayList<>(environment.getCallStack()));
            environment.clearCallStack();
            environment.pushCallStack(0, StatementKind.SCRIPT, "Script : %1 ", runtime.name);

            for (Statement stmt : runtime.statements) {
                environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "%1", stmt);
                try {
                    if (debug.isDebugTraceOn()) {
                        debug.debugWriteStart("TRACE", "line " + stmt.getLine() + " : " + stmt.getClass().getSimpleName());
                        stmt.accept(this);
                        debug.debugWriteEnd();
                    } else {
                        stmt.accept(this);
                    }

                } finally {
                    environment.popCallStack();
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
        environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Var %1", stmt.name);  // name may be null
        try {
            Object value = evaluate(stmt.initializer);

            if (stmt.varType != null) {
                value = stmt.varType.convertValue(value);

                DataType expectedType = stmt.varType;
                if (stmt.initializer instanceof ArrayExpression array) {
                    if (!Util.checkDataType(array.dataType, value)) {
                        throw error(stmt.getLine(), "Array type mismatch: expected " + expectedType + " for variable '" + stmt.name + "'");
                    }
                } else if (!Util.checkDataType(expectedType, value)) {
                    throw error(stmt.getLine(), "Type mismatch: expected " + expectedType + " for variable '" + stmt.name + "'");
                }
            }
            environment.getEnvironmentValues().define(stmt.name, value);
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public ArrayDef visitArrayLiteralExpression(ArrayLiteralExpression expr) throws InterpreterError {
        // Find predefined target array for assignment
        ArrayDef target = null;

        if (expr.array instanceof VariableExpression var) {
            Object v = environment.get(var.name);
            if (v instanceof ArrayDef av) {
                target = av;
            } else {
                throw error(expr.line, "'" + var.name + "' is not an array.");
            }

        } else if (expr.array instanceof IndexExpression idx) {
            // Evaluate container and indices to get to the leaf slot
            Object container = evaluate(idx.target);

            // Evaluate indices
            int[] indices = new int[idx.indices.length];
            for (int k = 0; k < indices.length; k++) {
                indices[k] = toIndexInt(evaluate(idx.indices[k]), expr.line, k + 1);
            }

            // Traverse to final slot's parent
            Object current = container;
            for (int d = 0; d < indices.length - 1; d++) {
                current = getIndexed(current, indices[d], expr.line);
            }

            // Fetch/create the child array at the final index
            Object leaf = getIndexed(current, indices[indices.length - 1], expr.line);
            if (leaf == null) {
                if (current instanceof ArrayDef parent) {
                    int childLen = (expr.elements == null) ? 0 : expr.elements.length;
                    ArrayDef child = newChildArrayForLiteral(parent, childLen);
                    setIndexed(current, indices[indices.length - 1], child, expr.line);
                    target = child;
                } else {
                    throw error(expr.line, "Target at indexed position is null and not an array container.");
                }
            } else if (leaf instanceof ArrayDef av) {
                target = av;
            } else {
                throw error(expr.line, "Target at indexed position is not an array.");
            }

        } else {
            target = new ArrayDynamic(DataType.STRING);
            //throw error(expr.line, "Array literal must assign to a predefined array.");
        }

        // Recursively copy content, converting leaf strings and expanding as needed
        assignLiteralToArray(target, expr, expr.line);

        // Return the target for chaining if needed
        return target;
    }

    /**
     * Create a child ArrayDef under a given parent slot. If the parent is
     * fixed, create a fixed child sized exactly to childLen (no growth allowed
     * later). If the parent is dynamic, create a dynamic child and expand to
     * childLen.
     */
    private ArrayDef newChildArrayForLiteral(ArrayDef parent, int childLen) {
        DataType elemType = parent.getDataType();

        if (parent.isFixed()) {
            // Create a fixed child sized to the nested literal length.
            // No expand calls for fixed children; overflow will be handled by the assigner.
            if (elemType == DataType.BYTE) {
                return new ArrayFixedByte(Math.max(0, childLen));
            } else {
                return new ArrayFixed(elemType, Math.max(0, childLen));
            }
        } else {
            // Dynamic child can grow
            ArrayDynamic child = new ArrayDynamic(elemType);
            if (childLen > 0) {
                child.expandArray(childLen);
            }
            return child;
        }
    }

    /**
     * Recursively assign a (possibly nested) literal array into a predefined
     * ArrayDef, converting leaf string values to the array's DataType and
     * expanding only dynamic arrays. Growth of fixed arrays (parent or child)
     * is forbidden: overflow -> error.
     */
    private void assignLiteralToArray(ArrayDef target, ArrayLiteralExpression literal, int line) throws InterpreterError {
        if (target == null) {
            throw error(line, "Target array is null.");
        }

        final int literalLen = (literal.elements == null) ? 0 : literal.elements.length;

        // Enforce capacity rules at the parent level
        Integer parentLen = target.size();   // may be null for dynamic
        if (target.isFixed()) {
            // For fixed arrays: do NOT grow; throw on overflow
            if (parentLen != null && literalLen > parentLen) {
                throw error(line, "Array literal length (" + literalLen + ") exceeds fixed array length (" + parentLen + ").");
            }
            // No expandArray() for fixed parents
        } else {
            // Dynamic arrays may grow to fit
            int current = (parentLen == null ? 0 : parentLen);
            if (literalLen > current) {
                target.expandArray(literalLen);
                parentLen = literalLen;
            }
        }

        final DataType elemType = target.getDataType();

        for (int i = 0; i < literalLen; i++) {
            com.eb.script.interpreter.expression.Expression e = literal.elements[i];

            if (e instanceof ArrayLiteralExpression nested) {
                // Ensure child array exists at index i
                Object slot = target.get(i);
                ArrayDef child;

                if (slot == null) {
                    // Create a new child sized to nested length. If parent is fixed, child is fixed.
                    int childLen = (nested.elements == null) ? 0 : nested.elements.length;
                    child = newChildArrayForLiteral(target, childLen);
                    target.set(i, child);
                } else if (slot instanceof ArrayDef existingChild) {
                    child = existingChild;
                    int childLen = (nested.elements == null) ? 0 : nested.elements.length;

                    if (child.isFixed()) {
                        // FORBID growing fixed children; throw on overflow
                        Integer cl = child.size();
                        if (cl != null && childLen > cl) {
                            throw error(line, "Nested array literal length (" + childLen
                                    + ") exceeds fixed child length (" + cl + ") at index " + i + ".");
                        }
                        // do not expand fixed child
                    } else {
                        // Dynamic child: grow if required
                        Integer cl = child.size();
                        int current = (cl == null ? 0 : cl);
                        if (childLen > current) {
                            child.expandArray(childLen);
                        }
                    }
                } else {
                    throw error(line, "Target at index " + i + " is not an array, but literal has a nested array.");
                }

                // Recurse into child
                assignLiteralToArray(child, nested, line);

            } else {
                // Leaf: evaluate to string, convert to DataType, assign
                Object raw = evaluate(e);                 // grammar emits strings; be defensive anyway
                if (!(raw instanceof String)) {
                    raw = (raw == null ? null : raw.toString());
                }

                Object converted = (raw == null) ? null : elemType.convertValue(raw);
                // For fixed arrays we already ensured i < capacity; for dynamic arrays we expanded above
                target.set(i, converted);
            }
        }
    }

    @Override
    public Object visitArrayInitExpression(ArrayExpression expr) throws InterpreterError {
        int dimCount = expr.dimensions.length;
        if (dimCount == 0) {
            throw error(expr.line, "Array declaration requires at least one dimension.");
        }
        Integer[] dims = new Integer[dimCount];
        for (int i = 0; i < dimCount; i++) {
            Expression ed = expr.dimensions[i];
            dims[i] = (ed == null) ? null : toNonNegativeInt(expr.line, evaluate(ed), i + 1);
        }
        ArrayDef array = createEmptyArray(expr.dataType, dims, 0);
        if (expr.initializer instanceof ArrayLiteralExpression lit) {
            int idx = 0;
            for (com.eb.script.interpreter.expression.Expression e : lit.elements) {
                array.set(idx++, evaluate(e));
            }
        }
        return array;
    }

    private String getTargetDescription(Object target) {
        if (target instanceof ArrayDef array) {
            return "Array[" + array.size() + "]:" + array.getDataType();
        } else if (target instanceof List array) {
            return "List[" + array.size() + "]";
        } else if (target instanceof Object[] array) {
            return "List[" + array.length + "]";
        } else {
            return "(unknown)";
        }
    }

    @Override
    public Object visitIndexExpression(IndexExpression expr) throws InterpreterError {
        Object target = evaluate(expr.target);

        environment.pushCallStack(expr.line, StatementKind.EXPRESSION, "IndexExpression [%1]", target.getClass().getSimpleName());
        try {
            if (expr.indices == null || expr.indices.length == 0) {
                throw error(expr.line, "Index expression requires at least one index.");
            }

            // Evaluate index values once
            int[] idx = new int[expr.indices.length];
            for (int k = 0; k < idx.length; k++) {
                Object v = evaluate(expr.indices[k]);
                idx[k] = toIndexInt(v, expr.line, k + 1);
            }

            // Walk nested lists according to idx[]
            Object current = target;
            for (int d = 0; d < idx.length; d++) {
                int i = idx[d];

                if (current instanceof Object[] array) {
                    checkBounds(expr.line, i, array.length);
                    current = array[i];
                } else if (current instanceof List<?> list) {
                    checkBounds(expr.line, i, list.size());
                    current = list.get(i);
                } else if (current instanceof ArrayDef list) {
                    checkBounds(expr.line, i, list.size());
                    current = list.get(i);
                } else {
                    String kind = (current == null) ? "null" : current.getClass().getSimpleName();
                    throw error(expr.line, "Cannot index into type " + kind + " for " + getTargetDescription(target));
                }
            }
            return current;
        } finally {
            environment.popCallStack();
        }
    }

    /* ---------- helpers ---------- */
    private int toIndexInt(Object v, int line, int position1Based) throws InterpreterError {
        if (!(v instanceof Number)) {
            throw error(line, "Index " + position1Based + " must be a number.");
        }
        int i = ((Number) v).intValue();
        if (i < 0) {
            throw error(line, "Index " + position1Based + " must be non-negative.");
        }
        return i;
    }

    private void checkBounds(int line, int i, int len) throws InterpreterError {
        if (i < 0 || i >= len) {
            throw error(line, "Index out of bounds: " + i + " (size " + len + ").");
        }
    }

    /* --------------------------
   Helpers used by the visitors
   -------------------------- */
    /**
     * Coerce a value to a non-negative int; throw runtime error on invalid
     * input.
     */
    private int toNonNegativeInt(int line, Object v, int dimensionIndex1Based) throws InterpreterError {
        if (!(v instanceof Number)) {
            throw error(line, "Array size at dimension " + dimensionIndex1Based + " must be a number.");
        }
        int n = ((Number) v).intValue();
        if (n < 0) {
            throw error(line, "Array size at dimension " + dimensionIndex1Based + " must be non-negative.");
        }
        return n;
    }

    /**
     * Allocate nested lists with the given shape; leaf level is filled with
     * nulls.
     */
    private ArrayDef createEmptyArray(DataType dataType, Integer[] dims, int depth) {
        Integer len = dims[depth];
        ArrayDef ret = null;
        if (len == null) {
            ret = new ArrayDynamic(dataType);
            len = 0;
        } else {
            if (dataType == DataType.BYTE) {
                ret = new ArrayFixedByte(len);
            } else {
                ret = new ArrayFixed(dataType, len);
            }
        }
        if (depth == dims.length - 1) {
            for (int i = 0; i < len; i++) {
                ret.set(i, null);
            }
        } else {
            for (int i = 0; i < len; i++) {
                ret.set(i, createEmptyArray(dataType, dims, depth + 1));
            }
        }
        return ret;
    }

    private void expandArray(ArrayDef array, Integer[] dims, int depth) {
        Integer len = dims[depth];
        if (array.size() < len) {
            for (int idx = array.size(); idx < len; idx++) {
                if (depth < dims.length - 1) {
                    array.set(idx, createEmptyArray(array.getDataType(), dims, depth + 1));
                } else {
                    array.set(idx, null);
                }
            }
        }
    }

    /**
     * Compute shape of a nested list and validate it is rectangular. Returns an
     * int[] shape (e.g., {rows, cols, ...}) or {n} for 1D. Throws if rows have
     * different lengths or mixing scalar and list at same depth.
     */
    private int[] computeAndValidateShape(Object[] array, int line) throws InterpreterError {
        if (array == null || array.length == 0) {
            return new int[]{0};
        }

        // Determine shape of first element
        int[] subShape = null;
        boolean firstIsList = array[0] instanceof Object[];
        if (firstIsList) {
            subShape = computeAndValidateShape((Object[]) array[0], line);
        }

        // All elements must match "listness" and sub-shape
        for (int i = 1; i < array.length; i++) {
            Object el = array[i];
            boolean isList = el instanceof Object[];
            if (isList != firstIsList) {
                throw error(line, "Array literal must be rectangular: mixed scalar and sub-array at the same level.");
            }
            if (isList) {
                int[] shapeI = computeAndValidateShape((Object[]) el, line);
                if (!java.util.Arrays.equals(shapeI, subShape)) {
                    throw error(line, "Array literal is jagged: sub-arrays do not have consistent shape.");
                }
            }
        }

        if (firstIsList) {
            int[] shape = new int[subShape.length + 1];
            shape[0] = array.length;
            System.arraycopy(subShape, 0, shape, 1, subShape.length);
            return shape;
        } else {
            return new int[]{array.length};
        }
    }

    @Override
    public void visitIfStatement(IfStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.CONDITION, "If (%1)", stmt.condition);  // name may be null
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
            environment.popCallStack();
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
        environment.pushCallStack(stmt.getLine(), StatementKind.LOOP, "While (%1)", stmt.condition);  // name may be null
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
            environment.popCallStack();
        }
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.LOOP, "Do While (%1)", stmt.condition);  // name may be null
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
            environment.popCallStack();
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

        // Check if this is a screen variable assignment (screen_name.var_name)
        String name = stmt.name;
        int dotIndex = name.indexOf('.');

        if (dotIndex > 0) {
            String screenName = name.substring(0, dotIndex);
            String varName = name.substring(dotIndex + 1);

            // Check if this is a screen variable
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap = screenVars.get(screenName);
            if (screenVarMap != null) {
                if (screenVarMap.containsKey(varName)) {
                    screenVarMap.put(varName, value);
                    return;
                } else {
                    throw error(stmt.getLine(), "Screen '" + screenName + "' does not have a variable named '" + varName + "'.");
                }
            }
        }

        // Fall back to regular environment variable assignment
        environment.getEnvironmentValues().assign(stmt.name, value);
    }

    @Override
    public void visitIndexAssignStatement(IndexAssignStatement stmt) throws InterpreterError {
        if (!(stmt.target instanceof IndexExpression idxExpr)) {
            throw error(stmt.getLine(), "Internal error: index assignment target is not an index expression.");
        }

        // Evaluate the container (up to the last dimension) and the final index
        // Example: for m[i, j], we need m[i] first, then set element at [j].
        Object container = evaluate(idxExpr.target);

        // Evaluate indices
        int[] idx = new int[idxExpr.indices.length];
        for (int k = 0; k < idx.length; k++) {
            Object v = evaluate(idxExpr.indices[k]);
            idx[k] = toIndexInt(v, stmt.getLine(), k + 1);
        }

        // Traverse to parent container (all but last index)
        for (int d = 0; d < idx.length - 1; d++) {
            container = getIndexed(container, idx[d], stmt.getLine());
        }

        // Set at last index
        int last = idx[idx.length - 1];
        Object rhs = evaluate(stmt.value);

        setIndexed(container, last, rhs, stmt.getLine());

    }

    /* ---- helpers for assignment ---- */
    private Object getIndexed(Object container, int i, int line) throws InterpreterError {
        if (container instanceof Object[] array) {
            checkBounds(line, i, array.length);
            return array[i];
        } else if (container instanceof List<?>) {
            List<?> list = (java.util.List<?>) container;
            checkBounds(line, i, list.size());
            return list.get(i);
        } else if (container instanceof ArrayDef arr) {
            checkBounds(line, i, arr.size());
            return arr.get(i); // in getIndexed
        } else {
            String kind = (container == null) ? "null" : container.getClass().getSimpleName();
            throw error(line, "Cannot index into type " + kind + ".");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setIndexed(Object container, int i, Object value, int line) throws InterpreterError {
        if (container instanceof Object[] array) {
            checkBounds(line, i, array.length);
            array[i] = value;
        } else if (container instanceof java.util.List) {
            java.util.List list = (java.util.List) container;
            checkBounds(line, i, list.size());
            list.set(i, value);
        } else if (container instanceof ArrayDef arr) {
            checkBounds(line, i, arr.size() + 1);
            arr.set(i, value); // in setIndexed            
        } else {
            String kind = (container == null) ? "null" : container.getClass().getSimpleName();
            throw error(line, "Cannot index into type " + kind + ".");
        }
    }

    @Override
    public void visitPrintStatement(PrintStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Print");  // name may be null
        try {
            Object value = evaluate(stmt.expression);
            if (output == null) {
                System.out.println(Util.stringify(value));
            } else {
                output.println(Util.stringify(value));
            }
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement stmt) throws InterpreterError {
        visitBlockStatement(stmt, null);
    }

    public Object visitBlockStatement(BlockStatement stmt, Statement[] parameters) throws InterpreterError {
        environment.pushEnvironmentValues();
        environment.pushCallStack(stmt.getLine(), StatementKind.BLOCK, "Block %1", stmt.name);  // name may be null

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
            if (!Util.checkDataType(stmt.returnType, r.value)) {
                throw error(stmt.getLine(), "Return value '" + r.value + "' not correct type : " + stmt.returnType + " in " + stmt.name);
            }
            return r.value;
        } finally {
            // POP the frame even on errors/returns
            environment.popCallStack();
            environment.popEnvironmentValues();
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
        }
        throw error(stmt.getLine(), "Call cannot find '" + stmt.name + "'");
    }

    @Override
    public Object visitReturnStatement(ReturnStatement stmt) throws InterpreterError {
        throw new ReturnSignal(evaluate(stmt.value));
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
        // Check if this is a screen variable access (screen_name.var_name)
        String name = expr.name;
        int dotIndex = name.indexOf('.');

        if (dotIndex > 0) {
            String screenName = name.substring(0, dotIndex);
            String varName = name.substring(dotIndex + 1);

            // Check if this is a screen variable
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap = screenVars.get(screenName);
            if (screenVarMap != null) {
                if (screenVarMap.containsKey(varName)) {
                    return screenVarMap.get(varName);
                } else {
                    throw error(expr.line, "Screen '" + screenName + "' does not have a variable named '" + varName + "'.");
                }
            }
        }

        // Fall back to regular environment variable
        return environment.get(expr.name);
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression expr) throws InterpreterError {
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

        throw error( // choose appropriate line source if needed
                (expr.target instanceof VariableExpression v) ? v.line : 0,
                "'.length' can only be used on arrays."
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
        environment.pushCallStack(stmt.getLine(), StatementKind.LOOP, "foreach %1", stmt.varName);
        try {
            final Object it = evaluate(stmt.iterable);

            java.util.function.Consumer<Object> runBodyWith = (elem) -> {
                environment.pushEnvironmentValues();
                try {
                    environment.getEnvironmentValues().define(stmt.varName, elem);
                    try {
                        stmt.body.accept(this);
                    } catch (ContinueSignal c) {
                        // skip to next
                    } catch (InterpreterError ex) {
                        throw new RuntimeException(ex.getMessage());
                    }
                } finally {
                    environment.popEnvironmentValues();
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
            environment.popCallStack();
        }
    }

    @Override
    public void visitConnectStatement(ConnectStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Connect %1", stmt.name);
        try {
            if (connections.containsKey(stmt.name)) {
                throw error(stmt.getLine(), "Connection '" + stmt.name + "' already exists.");
            }
            Object spec = evaluate(stmt.spec);        // string | json | identifier value
            DbConnection conn;
            try {
                conn = db.connect(spec);
            } catch (Exception e) {
                throw error(stmt.getLine(), "Connect failed: " + e.getMessage());
            }
            connections.put(stmt.name, conn);
        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitCloseConnectionStatement(CloseConnectionStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Close connect %1", stmt.name);
        try {
            DbConnection conn = connections.remove(stmt.name);
            if (conn == null) {
                throw error(stmt.getLine(), "Unknown connection '" + stmt.name + "'");
            }
            try {
                conn.close();
            } catch (Exception e) {
                throw error(stmt.getLine(), "Close connection failed: " + e.getMessage());
            }
        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitScreenStatement(ScreenStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1", stmt.name);
        try {
            if (screens.containsKey(stmt.name)) {
                throw error(stmt.getLine(), "Screen '" + stmt.name + "' already exists.");
            }

            // Evaluate the spec (should be a JSON object)
            Object spec = evaluate(stmt.spec);

            if (!(spec instanceof Map)) {
                throw error(stmt.getLine(), "Screen configuration must be a JSON object (map).");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) spec;

            // Extract configuration properties with defaults
            String title = config.containsKey("title") ? String.valueOf(config.get("title")) : "Screen " + stmt.name;
            int width = config.containsKey("width") ? ((Number) config.get("width")).intValue() : 800;
            int height = config.containsKey("height") ? ((Number) config.get("height")).intValue() : 600;
            boolean maximize = config.containsKey("maximize") && Boolean.TRUE.equals(config.get("maximize"));

            // Create thread-safe variable storage for this screen
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap = new java.util.concurrent.ConcurrentHashMap<>();
            screenVars.put(stmt.name, screenVarMap);

            // Process variable definitions if present
            if (config.containsKey("vars")) {
                Object varsObj = config.get("vars");
                if (varsObj instanceof ArrayDynamic varsArray) {
                    @SuppressWarnings("unchecked")
                    List varsList = varsArray.getAll();
                    for (Object varObj : varsList) {
                        if (varObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> varDef = (Map<String, Object>) varObj;

                            // Extract variable properties
                            String varName = varDef.containsKey("name") ? String.valueOf(varDef.get("name")).toLowerCase() : null;
                            String varTypeStr = varDef.containsKey("type") ? String.valueOf(varDef.get("type")).toLowerCase() : null;
                            Object defaultValue = varDef.get("default");

                            if (varName == null || varName.isEmpty()) {
                                throw error(stmt.getLine(), "Variable definition in screen '" + stmt.name + "' must have a 'name' property.");
                            }

                            // Convert type string to DataType
                            DataType varType = null;
                            if (varTypeStr != null) {
                                varType = parseDataType(varTypeStr);
                                if (varType == null) {
                                    throw error(stmt.getLine(), "Unknown type '" + varTypeStr + "' for variable '" + varName + "' in screen '" + stmt.name + "'.");
                                }
                            }

                            // Convert and set the default value
                            Object value = defaultValue;
                            if (varType != null && value != null) {
                                value = varType.convertValue(value);
                            }

                            // Process optional display metadata
                            if (varDef.containsKey("display")) {
                                Object displayObj = varDef.get("display");
                                if (displayObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> displayDef = (Map<String, Object>) displayObj;

                                    // Store display metadata for the variable
                                    storeDisplayMetadata(varName, displayDef, stmt.name);
                                }
                            }

                            // Store in screen's thread-safe variable map
                            screenVarMap.put(varName, value);
                        }
                    }
                } else {
                    throw error(stmt.getLine(), "The 'vars' property in screen '" + stmt.name + "' must be an array.");
                }
            }

            // Process area definitions if present
            if (config.containsKey("area")) {
                Object areaObj = config.get("area");
                if (areaObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> areaList = (List<Object>) areaObj;
                    java.util.List<AreaDefinition> areas = new java.util.ArrayList<>();

                    for (Object aObj : areaList) {
                        if (aObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> areaDef = (Map<String, Object>) aObj;

                            AreaDefinition area = parseAreaDefinition(areaDef, stmt.name, stmt.getLine());
                            areas.add(area);
                        }
                    }

                    screenAreas.put(stmt.name, areas);
                } else {
                    throw error(stmt.getLine(), "The 'area' property in screen '" + stmt.name + "' must be an array.");
                }
            }

            // Create the screen on JavaFX Application Thread and set up thread management
            final String screenName = stmt.name;
            final String screenTitle = title;
            final int screenWidth = width;
            final int screenHeight = height;
            final boolean screenMaximize = maximize;

            Platform.runLater(() -> {
                try {
                    Stage stage = new Stage();
                    stage.setTitle(screenTitle);

                    // Create a simple scene with a StackPane
                    StackPane root = new StackPane();
                    Scene scene = new Scene(root, screenWidth, screenHeight);
                    stage.setScene(scene);

                    if (screenMaximize) {
                        stage.setMaximized(true);
                    }

                    // Create a thread for this screen
                    Thread screenThread = new Thread(() -> {
                        // Screen thread runs until interrupted or window closed
                        try {
                            while (!Thread.currentThread().isInterrupted()) {
                                Thread.sleep(100); // Keep thread alive
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }, "Screen-" + screenName);

                    screenThread.setDaemon(true);
                    screenThread.start();

                    // Store thread reference
                    screenThreads.put(screenName, screenThread);

                    // Set up cleanup when screen is closed
                    stage.setOnCloseRequest(event -> {
                        // Interrupt and stop the screen thread
                        Thread thread = screenThreads.get(screenName);
                        if (thread != null && thread.isAlive()) {
                            thread.interrupt();
                        }
                        // Clean up resources
                        screens.remove(screenName);
                        screenThreads.remove(screenName);
                        screenVars.remove(screenName);
                    });

                    // Store the stage reference
                    screens.put(screenName, stage);

                    // Show the screen
                    stage.show();

                    if (output != null) {
                        output.printlnOk("Screen '" + screenName + "' created with title: " + screenTitle);
                    }
                } catch (Exception e) {
                    if (output != null) {
                        output.printlnError("Failed to create screen '" + screenName + "': " + e.getMessage());
                    }
                }
            });

        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitScreenShowStatement(ScreenShowStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 show", stmt.name);
        try {
            Stage stage = screens.get(stmt.name);
            if (stage == null) {
                throw error(stmt.getLine(), "Screen '" + stmt.name + "' does not exist. Create it first with 'screen " + stmt.name + " = {...};'");
            }

            // Show the screen on JavaFX Application Thread
            Platform.runLater(() -> {
                if (!stage.isShowing()) {
                    stage.show();
                    if (output != null) {
                        output.printlnOk("Screen '" + stmt.name + "' shown");
                    }
                } else {
                    if (output != null) {
                        output.printlnInfo("Screen '" + stmt.name + "' is already showing");
                    }
                }
            });

        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitScreenHideStatement(ScreenHideStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 hide", stmt.name);
        try {
            Stage stage = screens.get(stmt.name);
            if (stage == null) {
                throw error(stmt.getLine(), "Screen '" + stmt.name + "' does not exist. Create it first with 'screen " + stmt.name + " = {...};'");
            }

            // Hide the screen on JavaFX Application Thread
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.hide();
                    if (output != null) {
                        output.printlnOk("Screen '" + stmt.name + "' hidden");
                    }
                } else {
                    if (output != null) {
                        output.printlnInfo("Screen '" + stmt.name + "' is already hidden");
                    }
                }
            });

        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    /**
     * Helper method to parse data type string to DataType enum
     */
    private DataType parseDataType(String typeStr) {
        if (typeStr == null) {
            return null;
        }

        switch (typeStr.toLowerCase()) {
            case "int":
            case "integer":
                return DataType.INTEGER;
            case "long":
                return DataType.LONG;
            case "float":
                return DataType.FLOAT;
            case "double":
                return DataType.DOUBLE;
            case "string":
                return DataType.STRING;
            case "bool":
            case "boolean":
                return DataType.BOOL;
            case "date":
                return DataType.DATE;
            case "byte":
                return DataType.BYTE;
            case "json":
                return DataType.JSON;
            default:
                return null;
        }
    }

    /**
     * Helper method to store display metadata for a variable
     */
    private void storeDisplayMetadata(String varName, Map<String, Object> displayDef, String screenName) {
        DisplayMetadata metadata = parseDisplayMetadata(displayDef, screenName);

        // Store the metadata using a composite key: screenName.varName
        String key = screenName + "." + varName;
        displayMetadata.put(key, metadata);
    }

    /**
     * Retrieve DisplayMetadata for a variable in a screen. This allows fallback
     * logic in AreaItem: if an AreaItem doesn't have its own DisplayMetadata,
     * it can retrieve the metadata from its varRef.
     *
     * @param screenName The name of the screen
     * @param varName The name of the variable
     * @return The DisplayMetadata for the variable, or null if not found
     */
    public DisplayMetadata getDisplayMetadata(String screenName, String varName) {
        String key = screenName + "." + varName;
        return displayMetadata.get(key);
    }

    /**
     * Helper method to parse display metadata from a definition map
     */
    private DisplayMetadata parseDisplayMetadata(Map<String, Object> displayDef, String screenName) {
        DisplayMetadata metadata = new DisplayMetadata();

        // Extract display type and convert to enum
        if (displayDef.containsKey("type")) {
            metadata.type = String.valueOf(displayDef.get("type")).toLowerCase();
            metadata.itemType = ItemType.fromString(metadata.type);
        } else {
            metadata.itemType = ItemType.TEXTFIELD; // Default to textfield
            metadata.type = "textfield";
        }

        // Set CSS class from enum
        metadata.cssClass = metadata.itemType.getCssClass();

        if (displayDef.containsKey("mandatory")) {
            Object mandatoryObj = displayDef.get("mandatory");
            if (mandatoryObj instanceof Boolean) {
                metadata.mandatory = (Boolean) mandatoryObj;
            }
        }

        if (displayDef.containsKey("case")) {
            metadata.caseFormat = String.valueOf(displayDef.get("case")).toLowerCase();
        }

        if (displayDef.containsKey("min")) {
            metadata.min = displayDef.get("min");
        }

        if (displayDef.containsKey("max")) {
            metadata.max = displayDef.get("max");
        }

        if (displayDef.containsKey("alignment")) {
            metadata.alignment = String.valueOf(displayDef.get("alignment")).toLowerCase();
        }

        if (displayDef.containsKey("pattern")) {
            metadata.pattern = String.valueOf(displayDef.get("pattern"));
        }

        // Extract or set default style
        if (displayDef.containsKey("style")) {
            metadata.style = String.valueOf(displayDef.get("style"));
        } else {
            // Use default style from the enum
            metadata.style = metadata.itemType.getDefaultStyle();
        }

        metadata.screenName = screenName;

        return metadata;
    }

    /**
     * Helper method to parse area definition from JSON
     */
    private AreaDefinition parseAreaDefinition(Map<String, Object> areaDef, String screenName, int line) throws InterpreterError {
        AreaDefinition area = new AreaDefinition();

        // Extract area name (required)
        if (areaDef.containsKey("name")) {
            area.name = String.valueOf(areaDef.get("name"));
        } else {
            throw error(line, "Area definition in screen '" + screenName + "' must have a 'name' property.");
        }

        // Extract area type and convert to enum
        if (areaDef.containsKey("type")) {
            area.type = String.valueOf(areaDef.get("type")).toLowerCase();
            area.areaType = AreaType.fromString(area.type);
        } else {
            area.areaType = AreaType.PANE; // Default to PANE
            area.type = "pane";
        }

        // Set CSS class from enum
        area.cssClass = area.areaType.getCssClass();

        // Extract layout configuration
        if (areaDef.containsKey("layout")) {
            area.layout = String.valueOf(areaDef.get("layout"));
        }

        // Extract or set default style
        if (areaDef.containsKey("style")) {
            area.style = String.valueOf(areaDef.get("style"));
        } else {
            // Use default style from the enum
            area.style = area.areaType.getDefaultStyle();
        }

        area.screenName = screenName;

        // Process items in the area
        if (areaDef.containsKey("items")) {
            Object itemsObj = areaDef.get("items");
            if (itemsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> itemsList = (List<Object>) itemsObj;

                for (Object itemObj : itemsList) {
                    if (itemObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemDef = (Map<String, Object>) itemObj;

                        AreaItem item = new AreaItem();

                        // Extract item properties
                        if (itemDef.containsKey("name")) {
                            item.name = String.valueOf(itemDef.get("name"));
                        }

                        if (itemDef.containsKey("sequence")) {
                            Object seqObj = itemDef.get("sequence");
                            if (seqObj instanceof Number) {
                                item.sequence = ((Number) seqObj).intValue();
                            }
                        }

                        if (itemDef.containsKey("layoutPos")) {
                            item.layoutPos = String.valueOf(itemDef.get("layoutPos"));
                        } else if (itemDef.containsKey("layout_pos")) {
                            // Support both camelCase and snake_case
                            item.layoutPos = String.valueOf(itemDef.get("layout_pos"));
                        } else if (itemDef.containsKey("relativePos")) {
                            // Backward compatibility with old name
                            item.layoutPos = String.valueOf(itemDef.get("relativePos"));
                        } else if (itemDef.containsKey("relative_pos")) {
                            // Backward compatibility with old name (snake_case)
                            item.layoutPos = String.valueOf(itemDef.get("relative_pos"));
                        }

                        if (itemDef.containsKey("varRef")) {
                            item.varRef = String.valueOf(itemDef.get("varRef"));
                        } else if (itemDef.containsKey("var_ref")) {
                            item.varRef = String.valueOf(itemDef.get("var_ref"));
                        }

                        // Process optional display metadata for the item
                        // If not provided, the item will use the DisplayMetadata from varRef
                        if (itemDef.containsKey("display")) {
                            Object displayObj = itemDef.get("display");
                            if (displayObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> displayDef = (Map<String, Object>) displayObj;

                                // Parse display metadata for this specific item
                                item.displayMetadata = parseDisplayMetadata(displayDef, screenName);
                            }
                        }
                        // If displayMetadata is not set here, it will remain null
                        // and the consuming code should fall back to using varRef's DisplayMetadata

                        // Parse additional UI properties for the item
                        if (itemDef.containsKey("promptText")) {
                            item.promptText = String.valueOf(itemDef.get("promptText"));
                        } else if (itemDef.containsKey("prompt_text")) {
                            item.promptText = String.valueOf(itemDef.get("prompt_text"));
                        }

                        if (itemDef.containsKey("editable")) {
                            Object editableObj = itemDef.get("editable");
                            if (editableObj instanceof Boolean) {
                                item.editable = (Boolean) editableObj;
                            }
                        }

                        if (itemDef.containsKey("disabled")) {
                            Object disabledObj = itemDef.get("disabled");
                            if (disabledObj instanceof Boolean) {
                                item.disabled = (Boolean) disabledObj;
                            }
                        }

                        if (itemDef.containsKey("visible")) {
                            Object visibleObj = itemDef.get("visible");
                            if (visibleObj instanceof Boolean) {
                                item.visible = (Boolean) visibleObj;
                            }
                        }

                        if (itemDef.containsKey("tooltip")) {
                            item.tooltip = String.valueOf(itemDef.get("tooltip"));
                        }

                        if (itemDef.containsKey("textColor")) {
                            item.textColor = String.valueOf(itemDef.get("textColor"));
                        } else if (itemDef.containsKey("text_color")) {
                            item.textColor = String.valueOf(itemDef.get("text_color"));
                        }

                        if (itemDef.containsKey("backgroundColor")) {
                            item.backgroundColor = String.valueOf(itemDef.get("backgroundColor"));
                        } else if (itemDef.containsKey("background_color")) {
                            item.backgroundColor = String.valueOf(itemDef.get("background_color"));
                        }

                        // Parse layout-specific properties
                        if (itemDef.containsKey("colSpan")) {
                            Object colSpanObj = itemDef.get("colSpan");
                            if (colSpanObj instanceof Number) {
                                item.colSpan = ((Number) colSpanObj).intValue();
                            }
                        } else if (itemDef.containsKey("col_span")) {
                            Object colSpanObj = itemDef.get("col_span");
                            if (colSpanObj instanceof Number) {
                                item.colSpan = ((Number) colSpanObj).intValue();
                            }
                        }

                        if (itemDef.containsKey("rowSpan")) {
                            Object rowSpanObj = itemDef.get("rowSpan");
                            if (rowSpanObj instanceof Number) {
                                item.rowSpan = ((Number) rowSpanObj).intValue();
                            }
                        } else if (itemDef.containsKey("row_span")) {
                            Object rowSpanObj = itemDef.get("row_span");
                            if (rowSpanObj instanceof Number) {
                                item.rowSpan = ((Number) rowSpanObj).intValue();
                            }
                        }

                        if (itemDef.containsKey("hgrow")) {
                            item.hgrow = String.valueOf(itemDef.get("hgrow")).toUpperCase();
                        }

                        if (itemDef.containsKey("vgrow")) {
                            item.vgrow = String.valueOf(itemDef.get("vgrow")).toUpperCase();
                        }

                        if (itemDef.containsKey("margin")) {
                            item.margin = String.valueOf(itemDef.get("margin"));
                        }

                        if (itemDef.containsKey("padding")) {
                            item.padding = String.valueOf(itemDef.get("padding"));
                        }

                        if (itemDef.containsKey("prefWidth")) {
                            item.prefWidth = String.valueOf(itemDef.get("prefWidth"));
                        } else if (itemDef.containsKey("pref_width")) {
                            item.prefWidth = String.valueOf(itemDef.get("pref_width"));
                        }

                        if (itemDef.containsKey("prefHeight")) {
                            item.prefHeight = String.valueOf(itemDef.get("prefHeight"));
                        } else if (itemDef.containsKey("pref_height")) {
                            item.prefHeight = String.valueOf(itemDef.get("pref_height"));
                        }

                        if (itemDef.containsKey("minWidth")) {
                            item.minWidth = String.valueOf(itemDef.get("minWidth"));
                        } else if (itemDef.containsKey("min_width")) {
                            item.minWidth = String.valueOf(itemDef.get("min_width"));
                        }

                        if (itemDef.containsKey("minHeight")) {
                            item.minHeight = String.valueOf(itemDef.get("minHeight"));
                        } else if (itemDef.containsKey("min_height")) {
                            item.minHeight = String.valueOf(itemDef.get("min_height"));
                        }

                        if (itemDef.containsKey("maxWidth")) {
                            item.maxWidth = String.valueOf(itemDef.get("maxWidth"));
                        } else if (itemDef.containsKey("max_width")) {
                            item.maxWidth = String.valueOf(itemDef.get("max_width"));
                        }

                        if (itemDef.containsKey("maxHeight")) {
                            item.maxHeight = String.valueOf(itemDef.get("maxHeight"));
                        } else if (itemDef.containsKey("max_height")) {
                            item.maxHeight = String.valueOf(itemDef.get("max_height"));
                        }

                        if (itemDef.containsKey("alignment")) {
                            item.alignment = String.valueOf(itemDef.get("alignment")).toLowerCase();
                        }

                        area.items.add(item);
                    }
                }

                // Sort items by sequence
                area.items.sort((a, b) -> Integer.compare(a.sequence, b.sequence));
            }
        }

        return area;
    }

    @Override
    public void visitUseConnectionStatement(UseConnectionStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Use connection %1", stmt.connectionName);
        try {
            DbConnection conn = connections.get(stmt.connectionName);
            if (conn == null) {
                throw error(stmt.getLine(), "Unknown connection '" + stmt.connectionName + "'. Did you connect first?");
            }
            connectionStack.push(stmt.connectionName);
            try {
                if (stmt.statements != null) {
                    for (Statement s : stmt.statements) {
                        environment.pushCallStack(s.getLine(), StatementKind.STATEMENT, "%1", s);
                        try {
                            s.accept(this);
                        } finally {
                            environment.popCallStack();
                        }
                    }
                }
            } finally {
                connectionStack.pop();
            }
        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitCursorStatement(CursorStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Cursor %1", stmt.name);
        try {
            String connName = currentConnection();
            if (connName == null) {
                throw error(stmt.getLine(), "cursor declaration requires an active 'use <connection> { ... }' block");
            }
            // Parser ensures SELECT text is captured in stmt.select.sql
            cursorSpecs.put(stmt.name, new CursorSpec(connName, stmt.select.sql, stmt.getLine())); // [1](https://za-prod.asyncgw.teams.microsoft.com/v1/objects/0-nza-d4-608facf2fafd223072e8197a8a6a9d5e/views/original/ScriptInterpreter_now.zip)
        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitOpenCursorStatement(OpenCursorStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Open cursor %1", stmt.name);
        try {
            CursorSpec spec = cursorSpecs.get(stmt.name);
            if (spec == null) {
                throw error(stmt.getLine(), "Unknown cursor '" + stmt.name + "'. Did you declare with 'cursor " + stmt.name + " = select ...;'?");
            }
            DbConnection conn = connections.get(spec.connectionName);
            if (conn == null) {
                throw error(stmt.getLine(), "Connection '" + spec.connectionName + "' is not open");
            }
            // Collect parameters (named and/or positional)
            final java.util.Map<String, Object> named = new java.util.LinkedHashMap<>();
            final java.util.List<Object> positional = new java.util.ArrayList<>();
            if (stmt.parameters != null) {
                for (com.eb.script.interpreter.statement.Parameter p : stmt.parameters) {
                    Object val = evaluate(p.value);
                    if (p.name != null) {
                        named.put(p.name, val);
                    } else {
                        positional.add(val);
                    }
                }
            }
            // Open the cursor via adapter and expose it as a variable with the cursor's name
            try {
                DbCursor cursor = conn.openCursor(spec.sql, named, positional);
                // make cursor variable visible (for myCursor.hasNext()/next())
                environment.getEnvironmentValues().define(stmt.name, cursor);
            } catch (Exception e) {
                throw error(stmt.getLine(), "Open cursor failed: " + e.getMessage());
            }
        } catch (InterpreterError ex) {
            throw error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public void visitCloseCursorStatement(com.eb.script.interpreter.statement.CloseCursorStatement stmt) throws InterpreterError {
        environment.pushCallStack(stmt.getLine(), StatementKind.SQL, "Close cursor %1", stmt.name);
        try {
            Object v;
            try {
                v = environment.get(stmt.name);
            } catch (RuntimeException undefined) {
                // not defined -> treat as not open
                v = null;
            }
            if (v instanceof DbCursor c) {
                try {
                    c.close();
                } catch (Exception e) {
                    throw error(stmt.getLine(), "Close cursor failed: " + e.getMessage());
                }
                // Optionally clear variable so subsequent hasNext()/next() will fail fast
                environment.getEnvironmentValues().assign(stmt.name, null);
            } // else: silently ignore closing a non-open cursor name
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public Object visitSqlSelectExpression(com.eb.script.interpreter.expression.SqlSelectExpression expr) throws InterpreterError {
        environment.pushCallStack(expr.line, StatementKind.SQL, "Expression %1", expr.sql);
        try {
            String connName = currentConnection();
            if (connName == null) {
                throw error(expr.line, "SELECT requires an active 'use <connection> { ... }' block");
            }
            DbConnection conn = connections.get(connName);
            if (conn == null) {
                throw error(expr.line, "Connection '" + connName + "' is not open");
            }
            try {
                // No parameters here; add if you later extend SELECT expr to support them.
                return conn.executeSelect(expr.sql,
                        java.util.Collections.emptyMap(),
                        java.util.Collections.emptyList());
            } catch (Exception e) {
                throw error(expr.line, "SELECT failed: " + e.getMessage());
            }
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public Object visitCursorHasNextExpression(com.eb.script.interpreter.expression.CursorHasNextExpression expr) throws InterpreterError {
        environment.pushCallStack(expr.getLine(), StatementKind.SQL, "HasNext %1", expr.target.toString());
        try {
            Object v = evaluate(expr.target);
            if (!(v instanceof DbCursor c)) {
                throw error(expr.getLine(), "hasNext() target is not a cursor");
            }
            try {
                return c.hasNext();
            } catch (Exception e) {
                throw error(expr.getLine(), "hasNext() failed: " + e.getMessage());
            }
        } finally {
            environment.popCallStack();
        }
    }

    @Override
    public Object visitCursorNextExpression(com.eb.script.interpreter.expression.CursorNextExpression expr) throws InterpreterError {
        environment.pushCallStack(expr.getLine(), StatementKind.SQL, "Next %1", expr.target.toString());
        try {
            Object v = evaluate(expr.target);
            if (!(v instanceof DbCursor c)) {
                throw error(expr.getLine(), "next() target is not a cursor");
            }
            try {
                return c.next(); // map of column -> value
            } catch (Exception e) {
                throw error(expr.getLine(), "next() failed: " + e.getMessage());
            }
        } finally {
            environment.popCallStack();
        }
    }

    // --- Helpers ---
    private Object evaluate(Expression expr) throws InterpreterError {
        if (expr == null) {
            return null;
        } else {
            return expr.accept(this);
        }
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
        environment.pushCallStack(c.getLine(), StatementKind.BUILTIN, "Call %1", name);
        try {
            return Builtins.callBuiltin(environment, name, args);
        } catch (Exception ex) {
            throw error(c.getLine(), "Call Builtin -> " + ex.getMessage());
        } finally {
            // POP even on failure
            environment.popCallStack();
        }
    }

    private static final class CursorSpec {

        final String connectionName;
        final String sql;
        final int line;

        CursorSpec(String connectionName, String sql, int line) {
            this.connectionName = connectionName;
            this.sql = sql;
            this.line = line;
        }
    }

    private InterpreterError error(int line, String message) {
        message = "Runtime error on line " + line + " : " + message;
        return new InterpreterError(message, environment.getCallStack());
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
    private void assignStringLiteralArray(ArrayDef target, ArrayLiteralExpression literal, int line) throws InterpreterError {
        if (target == null) {
            throw error(line, "Target array is null.");
        }

        // How many elements are in the literal
        final int literalLen = (literal.elements == null) ? 0 : literal.elements.length;

        // If dynamic, expand to fit; if fixed and too small, throw
        Integer dim = target.size();               // may be null for dynamic
        boolean isFixed = target.isFixed();
        if (isFixed) {
            if (dim != null && literalLen > dim) {
                throw error(line, "Array literal length (" + literalLen + ") exceeds fixed array length (" + dim + ").");
            }
            // no automatic shrink/expand on fixed arrays; we just write up to literalLen
        } else {
            // Dynamic array: ensure sufficient capacity
            int current = (dim == null ? 0 : dim);
            if (literalLen > current) {
                target.expandArray(literalLen);
            }
        }

        // Convert using the array's declared element DataType
        final DataType elemType = target.getDataType();

        // Copy & convert
        for (int i = 0; i < literalLen; i++) {
            Object raw = evaluate(literal.elements[i]); // expected to be String by your grammar
            if (!(raw instanceof String)) {
                // Be defensive: if someone slipped a non-string, normalize to string first.
                raw = (raw == null ? null : raw.toString());
            }

            // Convert "string" -> target element type, e.g., "123" -> Integer, "Y"/"true" -> Boolean, etc.
            Object converted = (raw == null) ? null : elemType.convertValue(raw);

            // Write to array slot i
            target.set(i, converted);
        }

        // If target is fixed and literal is shorter, we leave remaining elements as-is (nulls or prior values).
        // If you prefer explicit nulling of the tail, uncomment:
        // if (isFixed && dim != null) {
        //     for (int i = literalLen; i < dim; i++) target.setElement(i, null);
        // }
    }

}
