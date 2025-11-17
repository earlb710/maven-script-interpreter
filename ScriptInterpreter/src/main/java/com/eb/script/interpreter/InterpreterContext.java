package com.eb.script.interpreter;

import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.AreaDefinition;
import com.eb.script.interpreter.screen.AreaItem;
import com.eb.script.interpreter.screen.VarSet;
import com.eb.script.interpreter.screen.Var;
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
    private final Map<String, Runnable> screenRefreshCallbacks = new ConcurrentHashMap<>();
    private final Map<String, List<javafx.scene.Node>> screenBoundControls = new ConcurrentHashMap<>();
    
    // New storage structures for the refactored variable sets
    private final Map<String, Map<String, VarSet>> screenVarSets = new ConcurrentHashMap<>(); // screenName -> (setName -> VarSet)
    private final Map<String, Map<String, Var>> screenVarItems = new ConcurrentHashMap<>(); // screenName -> (setName.varName -> Var)
    private final Map<String, Map<String, AreaItem>> screenAreaItems = new ConcurrentHashMap<>(); // screenName -> (setName.varName -> AreaItem)
    
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

    /**
     * Get the screen refresh callbacks map.
     * @return the screen refresh callbacks map
     */
    public Map<String, Runnable> getScreenRefreshCallbacks() {
        return screenRefreshCallbacks;
    }

    /**
     * Get the screen bound controls map.
     * @return the screen bound controls map
     */
    public Map<String, List<javafx.scene.Node>> getScreenBoundControls() {
        return screenBoundControls;
    }

    /**
     * Trigger a screen refresh for the given screen name.
     * @param screenName the name of the screen to refresh
     */
    public void triggerScreenRefresh(String screenName) {
        Runnable callback = screenRefreshCallbacks.get(screenName);
        if (callback != null) {
            callback.run();
        }
    }
    
    /**
     * Get the screen variable sets map.
     * @return the screen variable sets map
     */
    public Map<String, Map<String, VarSet>> getScreenVarSets() {
        return screenVarSets;
    }
    
    /**
     * Get the screen variable items map.
     * @return the screen variable items map
     */
    public Map<String, Map<String, Var>> getScreenVarItems() {
        return screenVarItems;
    }
    
    /**
     * Get the screen area items map.
     * @return the screen area items map
     */
    public Map<String, Map<String, AreaItem>> getScreenAreaItems() {
        return screenAreaItems;
    }
    
    /**
     * Get a variable from a screen by its fully qualified key (setName.varName).
     * @param screenName the name of the screen
     * @param varKey the variable key (setName.varName, case-insensitive)
     * @return the Var object, or null if not found
     */
    public Var getScreenVar(String screenName, String varKey) {
        Map<String, Var> screenItems = screenVarItems.get(screenName);
        if (screenItems == null) {
            return null;
        }
        return screenItems.get(varKey.toLowerCase());
    }
    
    /**
     * Get a variable set from a screen by its name.
     * @param screenName the name of the screen
     * @param setName the name of the variable set (case-insensitive)
     * @return the VarSet object, or null if not found
     */
    public VarSet getScreenVarSet(String screenName, String setName) {
        Map<String, VarSet> sets = screenVarSets.get(screenName);
        if (sets == null) {
            return null;
        }
        return sets.get(setName.toLowerCase());
    }
}
