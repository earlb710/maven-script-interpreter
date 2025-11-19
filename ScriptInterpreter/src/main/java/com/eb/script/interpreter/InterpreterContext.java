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
 * InterpreterContext holds the state and variables that interpreter functions
 * need. This context can be passed to sub-interpreter classes to share state.
 */
public class InterpreterContext {

    // Global static maps shared across all interpreter instances for screen management
    private static final ConcurrentHashMap<String, Stage> GLOBAL_SCREENS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Thread> GLOBAL_SCREEN_THREADS = new ConcurrentHashMap<>();
    private static final List<String> GLOBAL_SCREEN_CREATION_ORDER = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Set<String> GLOBAL_SCREENS_BEING_CREATED = ConcurrentHashMap.newKeySet();

    private Environment environment;
    private Debugger debug;

    private final Map<String, DbConnection> connections = new java.util.HashMap<>();
    private final Map<String, Interpreter.CursorSpec> cursorSpecs = new java.util.HashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> screenVars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, DataType>> screenVarTypes = new ConcurrentHashMap<>();
    private final Map<String, DisplayItem> displayMetadata = new java.util.HashMap<>();
    private final Map<String, List<AreaDefinition>> screenAreas = new java.util.HashMap<>();
    private final Deque<String> connectionStack = new java.util.ArrayDeque<>();
    private final Map<String, Runnable> screenRefreshCallbacks = new ConcurrentHashMap<>();
    private final Map<String, List<javafx.scene.Node>> screenBoundControls = new ConcurrentHashMap<>();
    private final Map<String, com.eb.ui.ebs.StatusBar> screenStatusBars = new ConcurrentHashMap<>();

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
        return GLOBAL_SCREENS;
    }

    public ConcurrentHashMap<String, Thread> getScreenThreads() {
        return GLOBAL_SCREEN_THREADS;
    }

    public ConcurrentHashMap<String, Object> getScreenVars(String screen) {
        return screenVars.get(screen.toLowerCase());
    }

    public void setScreenVars(String screen, ConcurrentHashMap<String, Object> val) {
        screenVars.put(screen.toLowerCase(), val);
    }

    public ConcurrentHashMap<String, DataType> getScreenVarTypes(String screen) {
        return screenVarTypes.get(screen.toLowerCase());
    }

    public void setScreenVarTypes(String screen, ConcurrentHashMap<String, DataType> val) {
        screenVarTypes.put(screen.toLowerCase(), val);
    }

    public Set<String> getScreensBeingCreated() {
        return GLOBAL_SCREENS_BEING_CREATED;
    }

    public Map<String, DisplayItem> getDisplayItem() {
        return displayMetadata;
    }

    public List<AreaDefinition> getScreenAreas(String screen) {
        return screenAreas.get(screen.toLowerCase());
    }

    public void setScreenAreas(String screen, List<AreaDefinition> val) {
        screenAreas.put(screen.toLowerCase(), val);
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
     *
     * @return the current connection name, or null if no connection is active
     */
    public String getCurrentConnection() {
        return connectionStack.peek();
    }

    /**
     * Check if echo is enabled in the environment.
     *
     * @return true if echo is on, false otherwise
     */
    public boolean isEchoOn() {
        return environment != null && environment.isEchoOn();
    }

    /**
     * Get the screen refresh callbacks map.
     *
     * @return the screen refresh callbacks map
     */
    public Map<String, Runnable> getScreenRefreshCallbacks() {
        return screenRefreshCallbacks;
    }

    /**
     * Get the screen bound controls map.
     *
     * @return the screen bound controls map
     */
    public Map<String, List<javafx.scene.Node>> getScreenBoundControls() {
        return screenBoundControls;
    }

    /**
     * Get the screen status bars map.
     *
     * @return the screen status bars map
     */
    public Map<String, com.eb.ui.ebs.StatusBar> getScreenStatusBars() {
        return screenStatusBars;
    }

    /**
     * Trigger a screen refresh for the given screen name.
     *
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
     *
     * @return the screen variable sets map
     */
    public Map<String, VarSet> getScreenVarSets(String screen) {
        return screenVarSets.get(screen.toLowerCase());
    }

    public void setScreenVarSets(String screenName, Map<String, VarSet> val) {
        screenVarSets.put(screenName.toLowerCase(), val);
    }

    /**
     * Get the screen variable items map.
     *
     * @return the screen variable items map
     */
    public Map<String, Var> getScreenVarItems(String screen) {
        return screenVarItems.get(screen.toLowerCase());
    }

    public void setScreenVarItems(String screenName, Map<String, Var> val) {
        screenVarItems.put(screenName.toLowerCase(), val);
    }

    /**
     * Get the screen area items map.
     *
     * @return the screen area items map
     */
    public Map<String, AreaItem> getScreenAreaItems(String screen) {
        return screenAreaItems.get(screen.toLowerCase());
    }

    public void setScreenAreaItems(String screenName, Map<String, AreaItem> val) {
        screenAreaItems.put(screenName.toLowerCase(), val);
    }

    /**
     * Get a variable from a screen by its fully qualified key
     * (setName.varName).
     *
     * @param screenName the name of the screen
     * @param varKey the variable key (setName.varName, case-insensitive)
     * @return the Var object, or null if not found
     */
    public Var getScreenVar(String screenName, String varKey) {
        Map<String, Var> screenItems = screenVarItems.get(screenName.toLowerCase());
        if (screenItems == null) {
            return null;
        }
        return screenItems.get(varKey.toLowerCase());
    }

    /**
     * Get a variable set from a screen by its name.
     *
     * @param screenName the name of the screen
     * @param setName the name of the variable set (case-insensitive)
     * @return the VarSet object, or null if not found
     */
    public VarSet getScreenVarSet(String screenName, String setName) {
        Map<String, VarSet> sets = screenVarSets.get(screenName.toLowerCase());
        if (sets == null) {
            return null;
        }
        return sets.get(setName.toLowerCase());
    }

    /**
     * Get the screen creation order list.
     *
     * @return the list of screen names in the order they were created
     */
    public List<String> getScreenCreationOrder() {
        return GLOBAL_SCREEN_CREATION_ORDER;
    }

    /**
     * Static accessor for global screens map - can be called without instance.
     * This allows direct access to all screens from anywhere in the application.
     *
     * @return the global screens map
     */
    public static ConcurrentHashMap<String, Stage> getGlobalScreens() {
        return GLOBAL_SCREENS;
    }

    /**
     * Static accessor for global screen creation order - can be called without instance.
     * This allows direct access to screen order from anywhere in the application.
     *
     * @return the global screen creation order list
     */
    public static List<String> getGlobalScreenCreationOrder() {
        return GLOBAL_SCREEN_CREATION_ORDER;
    }

    public void clear() {
        GLOBAL_SCREENS.clear();
        GLOBAL_SCREEN_THREADS.clear();
        screenVars.clear();
        screenAreas.clear();
        displayMetadata.clear();
        GLOBAL_SCREENS_BEING_CREATED.clear();
        screenVarSets.clear();
        screenVarItems.clear();
        screenAreaItems.clear();
        GLOBAL_SCREEN_CREATION_ORDER.clear();
    }

    public void remove(String screenName) {
        GLOBAL_SCREENS.remove(screenName);
        GLOBAL_SCREEN_THREADS.remove(screenName);
        screenVars.remove(screenName);
        screenAreas.remove(screenName);
        screenVarSets.remove(screenName);
        screenVarItems.remove(screenName);
        screenAreaItems.remove(screenName);
        displayMetadata.entrySet().removeIf(entry -> entry.getKey().startsWith(screenName + "."));
        GLOBAL_SCREEN_CREATION_ORDER.remove(screenName);

    }

}
