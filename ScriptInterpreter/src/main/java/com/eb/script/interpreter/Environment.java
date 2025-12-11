package com.eb.script.interpreter;

import com.eb.script.file.FileContext;
import com.eb.util.Debugger;
import com.eb.util.Util;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.ui.cli.ScriptArea;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Environment holds the execution state including variable scopes, call stack,
 * and debugging state. Thread-safe implementation allows screen threads to
 * access and modify variables defined by the main thread.
 * 
 * Thread safety is achieved through:
 * - ConcurrentLinkedDeque for call stack (thread-safe deque operations)
 * - ThreadLocal for envValues to allow per-thread scope chains while sharing base values
 * - ConcurrentHashMap in EnvironmentValues for variable storage
 */
public class Environment {

    // Base environment values that are shared across all threads
    private EnvironmentValues baseEnvValues;
    
    // Thread-local environment values for per-thread scope chains
    // Each thread can have its own scope chain while sharing the base values
    private final ThreadLocal<EnvironmentValues> threadLocalEnvValues = new ThreadLocal<>();

    // Thread-safe call stack using ConcurrentLinkedDeque
    private final Deque<StackInfo> callStack = new ConcurrentLinkedDeque<>();
    private Debugger debug = new Debugger(null);
    private volatile boolean echo = false; // Default to off to suppress debug output during script initialization and resource loading
    private volatile ScriptArea outputArea;
    private volatile Interpreter currentInterpreter; // Reference to current Interpreter for cleanup

    private static final ConcurrentMap<String, FileContext> openedFiles = new ConcurrentHashMap<>();

    public Environment() {
        baseEnvValues = new EnvironmentValues();
    }
    
    /**
     * Get the current environment values for this thread.
     * Returns the thread-local scope if one exists, otherwise returns the base environment.
     */
    private EnvironmentValues getCurrentEnvValues() {
        EnvironmentValues threadLocal = threadLocalEnvValues.get();
        return threadLocal != null ? threadLocal : baseEnvValues;
    }
    
    /**
     * Set the current environment values for this thread.
     * Passing null or the base environment will remove the thread-local scope,
     * causing subsequent access to fall back to the base environment.
     */
    private void setCurrentEnvValues(EnvironmentValues values) {
        // If values is null or is the base environment, remove thread-local scope
        // This ensures getCurrentEnvValues() will return baseEnvValues
        if (values == null || values == baseEnvValues) {
            threadLocalEnvValues.remove();
        } else {
            threadLocalEnvValues.set(values);
        }
    }

    public Environment registerOutputArea(ScriptArea outputArea) {
        this.outputArea = outputArea;
        return this;
    }

    public EnvironmentValues getEnvironmentValues() {
        return getCurrentEnvValues();
    }
    
    /**
     * Get the base environment values (shared across all threads).
     * Used for defining global variables and functions.
     */
    public EnvironmentValues getBaseEnvironmentValues() {
        return baseEnvValues;
    }

    public ScriptArea getOutputArea() {
        return outputArea;
    }

    public void setCurrentInterpreter(Interpreter interpreter) {
        this.currentInterpreter = interpreter;
    }

    public Interpreter getCurrentInterpreter() {
        return currentInterpreter;
    }

    public Map<String, FileContext> getOpenFiles() {
        return Collections.unmodifiableMap(openedFiles);
    }

    public void registerOpenedFile(FileContext of) {
        openedFiles.put(of.handle, of);
    }

    public FileContext findOpenedFileByHandleOrPath(String key) {
        FileContext of = openedFiles.get(key);
        if (of != null) {
            return of;
        }
        // also allow lookup by absolute normalized path string
        for (FileContext v : openedFiles.values()) {
            if (v.path.toString().equals(key)) {
                return v;
            }
            if (v.path.getFileName() != null && v.path.getFileName().toString().equals(key)) {
                return v;
            }
        }
        return null;
    }

    public boolean closeOpenedFile(String key) {
        FileContext of = findOpenedFileByHandleOrPath(key);
        if (of == null) {
            return false;
        }
        try {
            of.chan.close();
        } catch (Exception ignore) {
        }
        openedFiles.remove(of.handle);
        return true;
    }


    /**
     * Close all (e.g., at end of run)
     */
    public void closeAllOpenFiles() {
        for (FileContext of : openedFiles.values()) {
            try {
                of.chan.close();
            } catch (Exception ignore) {
            }
        }
        openedFiles.clear();
    }

    /**
     * Clear all environment state.
     * Note: This clears the base environment and the current thread's thread-local scope.
     * Other threads may still have their own thread-local scopes which will eventually 
     * fall back to the cleared base environment when accessed.
     */
    public void clear() {
        clearCallStack();
        baseEnvValues.clear();
        threadLocalEnvValues.remove();
    }

    public Deque<StackInfo> getCallStack() {
        return callStack;
    }

    public void clearCallStack() {
        callStack.clear();
    }

    public void pushCallStack(int line, StatementKind kind, String message, Object... values) {
        callStack.push(new StackInfo(line, kind, message, values));
    }

    /**
     * Pop the top entry from the call stack.
     * Returns null if the stack is empty (thread-safe behavior).
     */
    public StackInfo popCallStack() {
        return callStack.poll(); // poll() returns null if empty, unlike pop() which throws
    }

    public Debugger getDebugger() {
        return debug;
    }

    public boolean setDebugOn() {
        if (debug != null) {
            return debug.setDebugOn();
        }
        return false;
    }

    public boolean setDebugOff() {
        if (debug != null) {
            return debug.setDebugOff();
        }
        return false;
    }

    public boolean isEchoOn() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public Object get(String name) throws InterpreterError {
        return getCurrentEnvValues().get(name);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        EnvironmentValues e = getCurrentEnvValues();
        // NOTE: values/enclosing are in this class; add this method inside the class
        while (e != null) {
            out.putAll(e.values); // values is a field in Environment
            e = e.enclosing;
        }
        return out;
    }

    /**
     * Push a new scope onto the environment values stack for this thread.
     * The new scope will have access to variables from enclosing scopes.
     */
    public void pushEnvironmentValues() {
        EnvironmentValues current = getCurrentEnvValues();
        EnvironmentValues newScope = new EnvironmentValues(current);
        setCurrentEnvValues(newScope);
    }

    /**
     * Pop the current scope from the environment values stack for this thread.
     * Returns to the enclosing scope, falling back to baseEnvValues if enclosing is null.
     * Does nothing if already at the base environment.
     */
    public void popEnvironmentValues() {
        EnvironmentValues current = getCurrentEnvValues();
        if (current != null && current != baseEnvValues) {
            // If enclosing is null, we fall back to baseEnvValues
            // setCurrentEnvValues(null) will remove thread-local and fallback to baseEnvValues
            EnvironmentValues enclosing = current.enclosing;
            setCurrentEnvValues(enclosing != null ? enclosing : null);
        }
    }

    public static class StackInfo {

        public int line;
        public StatementKind kind;
        public String message;
        public Object[] values;

        public StackInfo(int line, StatementKind kind, String message, Object... values) {
            this.line = line;
            this.kind = kind;
            this.message = message;
            this.values = values;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();
            for (int idx = 1; idx <= values.length; idx++) {
                String str = Util.stringify(values[idx - 1]);
                message = message.replace("%" + idx, str);
            }
            ret.append("line ").append(line).append(" ").append(kind).append(" : ").append(message);
            return ret.toString();
        }

    }

}
