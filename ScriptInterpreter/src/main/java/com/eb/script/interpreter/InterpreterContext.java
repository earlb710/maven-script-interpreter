package com.eb.script.interpreter;

import com.eb.util.Debugger;
import com.eb.script.interpreter.db.DbAdapter;
import com.eb.script.interpreter.db.DbConnection;
import com.eb.script.interpreter.db.OracleDbAdapter;
import com.eb.script.token.DataType;
import com.eb.ui.cli.ScriptArea;
import javafx.stage.Stage;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InterpreterContext holds the state and variables that interpreter functions need.
 * This context can be passed to sub-interpreter classes to share state.
 */
public class InterpreterContext {

    private Environment environment;
    private Debugger debug;
    
    private final Map<String, DbConnection> connections = new java.util.HashMap<>();
    private final Map<String, Interpreter.CursorSpec> cursorSpecs = new java.util.HashMap<>();
    private final ConcurrentHashMap<String, Stage> screens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Thread> screenThreads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> screenVars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, DataType>> screenVarTypes = new ConcurrentHashMap<>();
    private final Set<String> screensBeingCreated = ConcurrentHashMap.newKeySet();
    private final Map<String, DisplayItem> displayMetadata = new java.util.HashMap<>();
    private final Map<String, List<AreaDefinition>> screenAreas = new java.util.HashMap<>();
    private final Deque<String> connectionStack = new java.util.ArrayDeque<>();
    private DbAdapter db = new OracleDbAdapter();
    private ScriptArea output;

    public InterpreterContext() {
    }

    // Getters and setters for all fields

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.debug = environment.getDebugger();
    }

    public Debugger getDebug() {
        return debug;
    }

    public Map<String, DbConnection> getConnections() {
        return connections;
    }

    public Map<String, Interpreter.CursorSpec> getCursorSpecs() {
        return cursorSpecs;
    }

    public ConcurrentHashMap<String, Stage> getScreens() {
        return screens;
    }

    public ConcurrentHashMap<String, Thread> getScreenThreads() {
        return screenThreads;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> getScreenVars() {
        return screenVars;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, DataType>> getScreenVarTypes() {
        return screenVarTypes;
    }

    public Set<String> getScreensBeingCreated() {
        return screensBeingCreated;
    }

    public Map<String, DisplayItem> getDisplayItem() {
        return displayMetadata;
    }

    public Map<String, List<AreaDefinition>> getScreenAreas() {
        return screenAreas;
    }

    public Deque<String> getConnectionStack() {
        return connectionStack;
    }

    public DbAdapter getDb() {
        return db;
    }

    public void setDb(DbAdapter db) {
        this.db = (db == null ? DbAdapter.NOOP : db);
    }

    public ScriptArea getOutput() {
        return output;
    }

    public void setOutput(ScriptArea output) {
        this.output = output;
    }

    /**
     * Get the current database connection name from the connection stack.
     * @return the current connection name, or null if no connection is active
     */
    public String getCurrentConnection() {
        return connectionStack.peek();
    }

    /**
     * Check if echo is enabled in the environment.
     * @return true if echo is on, false otherwise
     */
    public boolean isEchoOn() {
        return environment != null && environment.isEchoOn();
    }
}
