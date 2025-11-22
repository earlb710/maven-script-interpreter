package com.eb.script.interpreter;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.expression.ArrayExpression;
import com.eb.script.interpreter.expression.ArrayLiteralExpression;
import com.eb.script.interpreter.expression.Expression;
import com.eb.script.interpreter.expression.IndexExpression;
import com.eb.script.interpreter.expression.PropertyExpression;
import com.eb.script.interpreter.expression.VariableExpression;
import com.eb.script.interpreter.statement.IndexAssignStatement;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.token.DataType;

import java.util.List;

/**
 * InterpreterArray handles all array-related interpreter operations.
 * This includes array literals, initialization, indexing, and assignments.
 */
public class InterpreterArray {
    
    private final InterpreterContext context;
    private final Interpreter interpreter;
    
    public InterpreterArray(InterpreterContext context, Interpreter interpreter) {
        this.context = context;
        this.interpreter = interpreter;
    }
    
    /**
     * Visit an array literal expression to assign values to an array
     */
    public ArrayDef visitArrayLiteralExpression(ArrayLiteralExpression expr) throws InterpreterError {
        // Find predefined target array for assignment
        ArrayDef target = null;

        if (expr.array instanceof VariableExpression var) {
            Object v = interpreter.environment().get(var.name);
            if (v instanceof ArrayDef av) {
                target = av;
            } else {
                throw interpreter.error(expr.line, "'" + var.name + "' is not an array.");
            }

        } else if (expr.array instanceof IndexExpression idx) {
            // Evaluate container and indices to get to the leaf slot
            Object container = interpreter.evaluate(idx.target);

            // Evaluate indices
            int[] indices = new int[idx.indices.length];
            for (int k = 0; k < indices.length; k++) {
                indices[k] = toIndexInt(interpreter.evaluate(idx.indices[k]), expr.line, k + 1);
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
                    throw interpreter.error(expr.line, "Target at indexed position is null and not an array container.");
                }
            } else if (leaf instanceof ArrayDef av) {
                target = av;
            } else {
                throw interpreter.error(expr.line, "Target at indexed position is not an array.");
            }

        } else {
            // No target specified - infer type from literal elements
            DataType inferredType = inferArrayTypeFromLiteral(expr);
            target = new ArrayDynamic(inferredType);
        }

        // Recursively copy content, converting leaf strings and expanding as needed
        assignLiteralToArray(target, expr, expr.line);

        // Return the target for chaining if needed
        return target;
    }
    
    /**
     * Infer the element type from an array literal's elements
     */
    private DataType inferArrayTypeFromLiteral(ArrayLiteralExpression expr) throws InterpreterError {
        if (expr.elements == null || expr.elements.length == 0) {
            return DataType.ARRAY;  // Generic array for empty literals
        }
        
        // Evaluate all elements to check if they're all the same type
        DataType inferredType = null;
        
        for (Expression element : expr.elements) {
            if (element != null) {
                Object value = interpreter.evaluate(element);
                if (value != null) {
                    // Determine the type of this element
                    DataType elementType = null;
                    if (value instanceof Integer || value instanceof Long) {
                        elementType = DataType.INTEGER;
                    } else if (value instanceof Float || value instanceof Double) {
                        elementType = DataType.DOUBLE;
                    } else if (value instanceof Boolean) {
                        elementType = DataType.BOOL;
                    } else if (value instanceof String) {
                        elementType = DataType.STRING;
                    } else if (value instanceof Byte) {
                        elementType = DataType.BYTE;
                    } else if (value instanceof ArrayDef) {
                        elementType = DataType.ARRAY;
                    }
                    
                    // Check consistency with previous elements
                    if (inferredType == null) {
                        inferredType = elementType;
                    } else if (elementType != null && inferredType != elementType) {
                        // Mixed types detected - use generic array
                        return DataType.ARRAY;
                    }
                }
            }
        }
        
        // Return the inferred type, or generic if we couldn't determine
        return inferredType != null ? inferredType : DataType.ARRAY;
    }

    /**
     * Visit an array initialization expression to create a new array
     */
    public Object visitArrayInitExpression(ArrayExpression expr) throws InterpreterError {
        int dimCount = expr.dimensions.length;
        if (dimCount == 0) {
            throw interpreter.error(expr.line, "Array declaration requires at least one dimension.");
        }
        Integer[] dims = new Integer[dimCount];
        for (int i = 0; i < dimCount; i++) {
            Expression ed = expr.dimensions[i];
            dims[i] = (ed == null) ? null : toNonNegativeInt(expr.line, interpreter.evaluate(ed), i + 1);
        }
        ArrayDef array = createEmptyArray(expr.dataType, dims, 0);
        if (expr.initializer instanceof ArrayLiteralExpression lit) {
            int idx = 0;
            for (Expression e : lit.elements) {
                array.set(idx++, interpreter.evaluate(e));
            }
        }
        return array;
    }

    /**
     * Visit an index expression to access array elements
     */
    public Object visitIndexExpression(IndexExpression expr) throws InterpreterError {
        Object target = interpreter.evaluate(expr.target);

        interpreter.environment().pushCallStack(expr.line, StatementKind.EXPRESSION, "IndexExpression [%1]", target.getClass().getSimpleName());
        try {
            if (expr.indices == null || expr.indices.length == 0) {
                throw interpreter.error(expr.line, "Index expression requires at least one index.");
            }

            // Evaluate index values once
            int[] idx = new int[expr.indices.length];
            for (int k = 0; k < idx.length; k++) {
                Object v = interpreter.evaluate(expr.indices[k]);
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
                    throw interpreter.error(expr.line, "Cannot index into type " + kind + " for " + getTargetDescription(target));
                }
            }
            return current;
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Visit an index assignment statement to set array elements
     */
    public void visitIndexAssignStatement(IndexAssignStatement stmt) throws InterpreterError {
        // Handle PropertyExpression (e.g., record.field = value or array[0].field = value)
        if (stmt.target instanceof PropertyExpression propExpr) {
            // Evaluate the object being accessed
            Object obj = interpreter.evaluate(propExpr.object);
            
            if (obj == null) {
                throw interpreter.error(stmt.getLine(), "Cannot access property '" + propExpr.propertyName + "' of null");
            }
            
            // Check if the object is a Map (record or JSON object)
            if (obj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                
                // Check if the property exists
                if (!map.containsKey(propExpr.propertyName)) {
                    throw interpreter.error(stmt.getLine(), "Property '" + propExpr.propertyName + "' does not exist in record");
                }
                
                // Evaluate the value to assign
                Object value = interpreter.evaluate(stmt.value);
                
                // Assign the property value
                map.put(propExpr.propertyName, value);
                return;
            }
            
            throw interpreter.error(stmt.getLine(), "Cannot access property '" + propExpr.propertyName + "' on non-record type: " + obj.getClass().getSimpleName());
        }
        
        // Handle IndexExpression (original logic)
        if (!(stmt.target instanceof IndexExpression idxExpr)) {
            throw interpreter.error(stmt.getLine(), "Internal error: index assignment target is not an index or property expression.");
        }

        // Evaluate the container (up to the last dimension) and the final index
        Object container = interpreter.evaluate(idxExpr.target);

        // Evaluate indices
        int[] idx = new int[idxExpr.indices.length];
        for (int k = 0; k < idx.length; k++) {
            Object v = interpreter.evaluate(idxExpr.indices[k]);
            idx[k] = toIndexInt(v, stmt.getLine(), k + 1);
        }

        // Traverse to parent container (all but last index)
        for (int d = 0; d < idx.length - 1; d++) {
            container = getIndexed(container, idx[d], stmt.getLine());
        }

        // Set at last index
        int last = idx[idx.length - 1];
        Object rhs = interpreter.evaluate(stmt.value);

        setIndexed(container, last, rhs, stmt.getLine());
    }

    // ========== Helper Methods ==========

    /**
     * Create a child ArrayDef under a given parent slot
     */
    private ArrayDef newChildArrayForLiteral(ArrayDef parent, int childLen) {
        DataType elemType = parent.getDataType();

        if (parent.isFixed()) {
            // Create a fixed child sized to the nested literal length
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
     * Recursively assign a (possibly nested) literal array into a predefined ArrayDef
     */
    private void assignLiteralToArray(ArrayDef target, ArrayLiteralExpression literal, int line) throws InterpreterError {
        if (target == null) {
            throw interpreter.error(line, "Target array is null.");
        }

        final int literalLen = (literal.elements == null) ? 0 : literal.elements.length;

        // Enforce capacity rules at the parent level
        Integer parentLen = target.size();
        if (target.isFixed()) {
            // For fixed arrays: do NOT grow; throw on overflow
            if (parentLen != null && literalLen > parentLen) {
                throw interpreter.error(line, "Array literal length (" + literalLen + ") exceeds fixed array length (" + parentLen + ").");
            }
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
            Expression e = literal.elements[i];

            if (e instanceof ArrayLiteralExpression nested) {
                // Ensure child array exists at index i
                Object slot = target.get(i);
                ArrayDef child;

                if (slot == null) {
                    // Create a new child sized to nested length
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
                            throw interpreter.error(line, "Nested array literal length (" + childLen
                                    + ") exceeds fixed child length (" + cl + ") at index " + i + ".");
                        }
                    } else {
                        // Dynamic child: grow if required
                        Integer cl = child.size();
                        int current = (cl == null ? 0 : cl);
                        if (childLen > current) {
                            child.expandArray(childLen);
                        }
                    }
                } else {
                    throw interpreter.error(line, "Target at index " + i + " is not an array, but literal has a nested array.");
                }

                // Recurse into child
                assignLiteralToArray(child, nested, line);

            } else {
                // Leaf: evaluate to string, convert to DataType, assign
                Object raw = interpreter.evaluate(e);
                if (!(raw instanceof String)) {
                    raw = (raw == null ? null : raw.toString());
                }

                Object converted = (raw == null) ? null : elemType.convertValue(raw);
                target.set(i, converted);
            }
        }
    }

    /**
     * Allocate nested lists with the given shape
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

    /**
     * Convert value to array index integer
     */
    private int toIndexInt(Object v, int line, int position1Based) throws InterpreterError {
        if (!(v instanceof Number)) {
            throw interpreter.error(line, "Index " + position1Based + " must be a number.");
        }
        int i = ((Number) v).intValue();
        if (i < 0) {
            throw interpreter.error(line, "Index " + position1Based + " must be non-negative.");
        }
        return i;
    }

    /**
     * Check array bounds
     */
    private void checkBounds(int line, int i, int len) throws InterpreterError {
        if (i < 0 || i >= len) {
            throw interpreter.error(line, "Index out of bounds: " + i + " (size " + len + ").");
        }
    }

    /**
     * Coerce a value to a non-negative int
     */
    private int toNonNegativeInt(int line, Object v, int dimensionIndex1Based) throws InterpreterError {
        if (!(v instanceof Number)) {
            throw interpreter.error(line, "Array size at dimension " + dimensionIndex1Based + " must be a number.");
        }
        int n = ((Number) v).intValue();
        if (n < 0) {
            throw interpreter.error(line, "Array size at dimension " + dimensionIndex1Based + " must be non-negative.");
        }
        return n;
    }

    /**
     * Get element from indexed container
     */
    private Object getIndexed(Object container, int i, int line) throws InterpreterError {
        if (container instanceof Object[] array) {
            checkBounds(line, i, array.length);
            return array[i];
        } else if (container instanceof List<?>) {
            List<?> list = (List<?>) container;
            checkBounds(line, i, list.size());
            return list.get(i);
        } else if (container instanceof ArrayDef arr) {
            checkBounds(line, i, arr.size());
            return arr.get(i);
        } else {
            String kind = (container == null) ? "null" : container.getClass().getSimpleName();
            throw interpreter.error(line, "Cannot index into type " + kind + ".");
        }
    }

    /**
     * Set element in indexed container
     */
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
            // Convert value to the array's element type before setting
            DataType elemType = arr.getDataType();
            if (elemType != null && value != null) {
                value = elemType.convertValue(value);
            }
            arr.set(i, value);
        } else {
            String kind = (container == null) ? "null" : container.getClass().getSimpleName();
            throw interpreter.error(line, "Cannot index into type " + kind + ".");
        }
    }

    /**
     * Get description of target for error messages
     */
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
}
