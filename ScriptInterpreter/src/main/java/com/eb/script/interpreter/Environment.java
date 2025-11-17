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
import java.util.concurrent.ConcurrentMap;

public class Environment {

    private EnvironmentValues envValues;

    private final Deque<StackInfo> callStack = new java.util.ArrayDeque<>();
    private Debugger debug = new Debugger(null);
    private boolean echo = true;
    private ScriptArea outputArea;
    private Interpreter currentInterpreter; // Reference to current Interpreter for cleanup

    private static final ConcurrentMap<String, FileContext> openedFiles = new ConcurrentHashMap<>();

    public Environment() {
        envValues = new EnvironmentValues();
    }

    public Environment registerOutputArea(ScriptArea outputArea) {
        this.outputArea = outputArea;
        return this;
    }

    public EnvironmentValues getEnvironmentValues() {
        return envValues;
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

    public void clear() {
        clearCallStack();
        envValues.clear();
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

    public StackInfo popCallStack() {
        return callStack.pop();
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
        return envValues.get(name);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        EnvironmentValues e = this.envValues;
        // NOTE: values/enclosing are in this class; add this method inside the class
        while (e != null) {
            out.putAll(e.values); // values is a field in Environment
            e = e.enclosing;
        }
        return out;
    }

    public void pushEnvironmentValues() {
        envValues = new EnvironmentValues(envValues);
    }

    public void popEnvironmentValues() {
        if (envValues != null) {
            envValues = envValues.enclosing;
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
