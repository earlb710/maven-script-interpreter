package com.eb.script.interpreter;

import com.eb.script.interpreter.screen.DisplayItem;
import com.eb.script.interpreter.screen.AreaDefinition;
import com.eb.script.interpreter.screen.AreaItem;
import com.eb.script.interpreter.screen.VarSet;
import com.eb.script.interpreter.screen.Var;
import com.eb.script.interpreter.screen.ScreenStatus;
import com.eb.script.interpreter.screen.ScreenConfig;
import com.eb.script.interpreter.screen.ScreenEventDispatcher;
import com.eb.util.Debugger;
import com.eb.script.interpreter.db.DbAdapter;
import com.eb.script.interpreter.db.DbConnection;
import com.eb.script.interpreter.db.OracleDbAdapter;
import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;
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
    private static final Set<String> GLOBAL_SCREENS_NOT_YET_SHOWN = ConcurrentHashMap.newKeySet();
    
    // ThreadLocal to track current screen context for onClick and other event handlers
    private static final ThreadLocal<String> CURRENT_SCREEN_CONTEXT = new ThreadLocal<>();

    private Environment environment;
    private Debugger debug;

    private final Map<String, DbConnection> connections = new java.util.HashMap<>();
    private final Map<String, Interpreter.CursorSpec> cursorSpecs = new java.util.HashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> screenVars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, DataType>> screenVarTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenComponentType>> screenComponentTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenContainerType>> screenContainerTypes = new ConcurrentHashMap<>();
    private final Map<String, DisplayItem> displayMetadata = new java.util.HashMap<>();
    private final Map<String, List<AreaDefinition>> screenAreas = new java.util.HashMap<>();
    private final Deque<String> connectionStack = new java.util.ArrayDeque<>();
    private final Map<String, Runnable> screenRefreshCallbacks = new ConcurrentHashMap<>();
    private final Map<String, List<javafx.scene.Node>> screenBoundControls = new ConcurrentHashMap<>();
    private final Map<String, com.eb.ui.ebs.StatusBar> screenStatusBars = new ConcurrentHashMap<>();
    private final Map<String, String> screenCallbacks = new ConcurrentHashMap<>(); // screenName -> callbackFunctionName
    private final Map<String, String> screenStartupCode = new ConcurrentHashMap<>(); // screenName -> startup EBS code
    private final Map<String, String> screenCleanupCode = new ConcurrentHashMap<>(); // screenName -> cleanup EBS code
    private final Map<String, String> screenGainFocusCode = new ConcurrentHashMap<>(); // screenName -> gainFocus EBS code
    private final Map<String, String> screenLostFocusCode = new ConcurrentHashMap<>(); // screenName -> lostFocus EBS code
    private final Set<String> importedFiles = ConcurrentHashMap.newKeySet();  // Global list of all imported files to prevent circular imports and duplicate imports
    private final Map<String, ScreenStatus> screenStatuses = new ConcurrentHashMap<>(); // screenName -> status
    private final Map<String, String> screenErrorMessages = new ConcurrentHashMap<>(); // screenName -> error message

    // New storage structures for the refactored variable sets
    private final Map<String, Map<String, VarSet>> screenVarSets = new ConcurrentHashMap<>(); // screenName -> (setName -> VarSet)
    private final Map<String, Map<String, Var>> screenVarItems = new ConcurrentHashMap<>(); // screenName -> (setName.varName -> Var)
    private final Map<String, Map<String, AreaItem>> screenAreaItems = new ConcurrentHashMap<>(); // screenName -> (setName.varName -> AreaItem)

    // Screen configurations stored before Stage creation (lazy initialization)
    private final Map<String, ScreenConfig> screenConfigs = new ConcurrentHashMap<>(); // screenName -> ScreenConfig

    // Track declared function names to prevent overwrites (functionName -> source file/script name)
    private final Map<String, String> declaredFunctions = new ConcurrentHashMap<>();
    
    // Track declared screen names to prevent overwrites (screenName -> source file/script name)
    private final Map<String, String> declaredScreens = new ConcurrentHashMap<>();

    // Store area container nodes for runtime property updates (screenName.areaName -> Region node)
    private final Map<String, javafx.scene.layout.Region> screenAreaContainers = new ConcurrentHashMap<>();

    // Track parent-child screen relationships (childScreenName -> parentScreenName)
    private final Map<String, String> screenParentMap = new ConcurrentHashMap<>();

    // Event dispatchers for screen threads (screenName -> ScreenEventDispatcher)
    private static final ConcurrentHashMap<String, ScreenEventDispatcher> GLOBAL_SCREEN_DISPATCHERS = new ConcurrentHashMap<>();

    private DbAdapter db = new OracleDbAdapter();
    private ScriptArea output;
    
    // Store the last inferred RecordType from a record() cast
    // This is used to associate RecordType metadata with cast expressions
    // Using ThreadLocal for thread-safety in case of concurrent script execution
    private final ThreadLocal<RecordType> lastInferredRecordType = new ThreadLocal<>();
    
    // Store the last inferred BitmapType from a bitmap type alias cast
    // This is used to associate BitmapType metadata with cast expressions
    // Using ThreadLocal for thread-safety in case of concurrent script execution
    private final ThreadLocal<com.eb.script.token.BitmapType> lastInferredBitmapType = new ThreadLocal<>();
    
    // Store the last inferred bitmap type alias name
    // This is used to track the name of the type alias used in a bitmap cast
    private final ThreadLocal<String> lastInferredBitmapTypeAliasName = new ThreadLocal<>();
    
    // Store the last inferred IntmapType from an intmap type alias cast
    // This is used to associate IntmapType metadata with cast expressions
    // Using ThreadLocal for thread-safety in case of concurrent script execution
    private final ThreadLocal<com.eb.script.token.IntmapType> lastInferredIntmapType = new ThreadLocal<>();
    
    // Store the last inferred intmap type alias name
    // This is used to track the name of the type alias used in an intmap cast
    private final ThreadLocal<String> lastInferredIntmapTypeAliasName = new ThreadLocal<>();
    
    // Store reference to the main interpreter for async callbacks
    // This allows callbacks to access functions defined in the script
    private volatile Interpreter mainInterpreter;

    public InterpreterContext() {
    }
    
    /**
     * Set the main interpreter reference for async callbacks.
     * This should be called after the interpreter processes the script.
     */
    public void setMainInterpreter(Interpreter interpreter) {
        this.mainInterpreter = interpreter;
    }
    
    /**
     * Get the main interpreter reference for async callbacks.
     */
    public Interpreter getMainInterpreter() {
        return mainInterpreter;
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

    /**
     * Get the event dispatchers map for all screens.
     * 
     * @return the event dispatchers map
     */
    public ConcurrentHashMap<String, ScreenEventDispatcher> getScreenEventDispatchers() {
        return GLOBAL_SCREEN_DISPATCHERS;
    }

    /**
     * Get the event dispatcher for a specific screen.
     * 
     * @param screenName the name of the screen
     * @return the event dispatcher, or null if not found
     */
    public ScreenEventDispatcher getScreenEventDispatcher(String screenName) {
        return GLOBAL_SCREEN_DISPATCHERS.get(screenName.toLowerCase());
    }

    /**
     * Set the event dispatcher for a screen.
     * 
     * @param screenName the name of the screen
     * @param dispatcher the event dispatcher
     */
    public void setScreenEventDispatcher(String screenName, ScreenEventDispatcher dispatcher) {
        if (dispatcher != null) {
            GLOBAL_SCREEN_DISPATCHERS.put(screenName.toLowerCase(), dispatcher);
        } else {
            GLOBAL_SCREEN_DISPATCHERS.remove(screenName.toLowerCase());
        }
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

    public ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenComponentType> getScreenComponentTypes(String screen) {
        return screenComponentTypes.get(screen.toLowerCase());
    }

    public void setScreenComponentTypes(String screen, ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenComponentType> val) {
        screenComponentTypes.put(screen.toLowerCase(), val);
    }

    public ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenContainerType> getScreenContainerTypes(String screen) {
        return screenContainerTypes.get(screen != null ? screen.toLowerCase() : null);
    }

    public void setScreenContainerTypes(String screen, ConcurrentHashMap<String, com.eb.script.interpreter.screen.ScreenContainerType> val) {
        screenContainerTypes.put(screen != null ? screen.toLowerCase() : null, val);
    }

    public Set<String> getScreensBeingCreated() {
        return GLOBAL_SCREENS_BEING_CREATED;
    }
    
    /**
     * Get the set of screens that have been created but not yet shown.
     * This is used to determine if item changes should be tracked as "changed".
     * During the initialization phase (after creation but before shown), changes
     * are part of normal setup and should not mark the screen as changed.
     * 
     * @return The set of screen names that have been created but not shown
     */
    public Set<String> getScreensNotYetShown() {
        return GLOBAL_SCREENS_NOT_YET_SHOWN;
    }

    /**
     * Get the current screen name based on the executing thread context.
     * First checks ThreadLocal for explicitly set screen context (e.g., from onClick handlers),
     * then falls back to checking if code is executing within a screen thread (thread name starts with "Screen-").
     * 
     * @return The current screen name, or null if not executing in a screen context
     */
    public String getCurrentScreen() {
        // First check ThreadLocal for explicitly set screen context
        String contextScreen = CURRENT_SCREEN_CONTEXT.get();
        if (contextScreen != null) {
            return contextScreen;
        }
        
        // Fall back to thread name check
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        
        // Check if we're executing in a screen thread
        if (threadName != null && threadName.startsWith("Screen-")) {
            // Extract screen name from thread name "Screen-<screenName>"
            return threadName.substring(7); // "Screen-".length() = 7
        }
        
        return null;
    }

    /**
     * Set the current screen name for the executing thread.
     * This is used by onClick handlers and other event handlers that execute on non-screen threads
     * (e.g., JavaFX Application Thread) to establish screen context.
     * 
     * @param screenName The screen name to set as current, or null to clear
     */
    public void setCurrentScreen(String screenName) {
        if (screenName != null) {
            CURRENT_SCREEN_CONTEXT.set(screenName);
        } else {
            CURRENT_SCREEN_CONTEXT.remove();
        }
    }
    
    /**
     * Clear the current screen context for the executing thread.
     * Should be called after onClick handlers or other event handlers complete
     * to prevent context leakage.
     */
    public void clearCurrentScreen() {
        CURRENT_SCREEN_CONTEXT.remove();
    }

    public Map<String, DisplayItem> getDisplayItem() {
        return displayMetadata;
    }

    public List<AreaDefinition> getScreenAreas(String screen) {
        return screenAreas.get(screen.toLowerCase());
    }
    
    public java.util.Set<String> getAllScreenAreaKeys() {
        return screenAreas.keySet();
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
     * Get the last inferred RecordType from a record() cast.
     * This is used to associate RecordType metadata with cast expressions.
     * Thread-safe using ThreadLocal.
     * 
     * @return the last inferred RecordType for the current thread, or null if none
     */
    public RecordType getLastInferredRecordType() {
        return lastInferredRecordType.get();
    }
    
    /**
     * Set the last inferred RecordType from a record() cast.
     * This is used to associate RecordType metadata with cast expressions.
     * Thread-safe using ThreadLocal.
     * 
     * @param recordType the inferred RecordType to store for the current thread
     */
    public void setLastInferredRecordType(RecordType recordType) {
        lastInferredRecordType.set(recordType);
    }
    
    /**
     * Clear the last inferred RecordType.
     * Should be called after the RecordType has been consumed.
     * Thread-safe using ThreadLocal.
     */
    public void clearLastInferredRecordType() {
        lastInferredRecordType.remove();
    }
    
    /**
     * Get the last inferred BitmapType from a bitmap type alias cast.
     * This is used to associate BitmapType metadata with cast expressions.
     * Thread-safe using ThreadLocal.
     * 
     * @return the last inferred BitmapType for the current thread, or null if none
     */
    public com.eb.script.token.BitmapType getLastInferredBitmapType() {
        return lastInferredBitmapType.get();
    }
    
    /**
     * Set the last inferred BitmapType from a bitmap type alias cast.
     * This is used to associate BitmapType metadata with cast expressions.
     * Thread-safe using ThreadLocal.
     * 
     * @param bitmapType the inferred BitmapType to store for the current thread
     */
    public void setLastInferredBitmapType(com.eb.script.token.BitmapType bitmapType) {
        lastInferredBitmapType.set(bitmapType);
    }
    
    /**
     * Set the last inferred BitmapType from a bitmap type alias cast along with the alias name.
     * This is used to associate BitmapType metadata with cast expressions.
     * Thread-safe using ThreadLocal.
     * 
     * @param bitmapType the inferred BitmapType to store for the current thread
     * @param aliasName the name of the type alias
     */
    public void setLastInferredBitmapType(com.eb.script.token.BitmapType bitmapType, String aliasName) {
        lastInferredBitmapType.set(bitmapType);
        lastInferredBitmapTypeAliasName.set(aliasName);
    }
    
    /**
     * Get the last inferred bitmap type alias name.
     * Thread-safe using ThreadLocal.
     * 
     * @return the last inferred bitmap type alias name for the current thread, or null if none
     */
    public String getLastInferredBitmapTypeAliasName() {
        return lastInferredBitmapTypeAliasName.get();
    }
    
    /**
     * Clear the last inferred BitmapType.
     * Should be called after the BitmapType has been consumed.
     * Thread-safe using ThreadLocal.
     */
    public void clearLastInferredBitmapType() {
        lastInferredBitmapType.remove();
        lastInferredBitmapTypeAliasName.remove();
    }
    
    /**
     * Get the last inferred IntmapType from an intmap type alias cast.
     * Thread-safe using ThreadLocal.
     * @return the last inferred IntmapType for the current thread, or null if none
     */
    public com.eb.script.token.IntmapType getLastInferredIntmapType() {
        return lastInferredIntmapType.get();
    }
    
    /**
     * Set the last inferred IntmapType from an intmap type alias cast.
     * Thread-safe using ThreadLocal.
     * @param intmapType the IntmapType to set for the current thread
     */
    public void setLastInferredIntmapType(com.eb.script.token.IntmapType intmapType) {
        setLastInferredIntmapType(intmapType, null);
    }
    
    /**
     * Set the last inferred IntmapType from an intmap type alias cast along with the alias name.
     * Thread-safe using ThreadLocal.
     * @param intmapType the IntmapType to set for the current thread
     * @param aliasName the type alias name to associate with this intmap
     */
    public void setLastInferredIntmapType(com.eb.script.token.IntmapType intmapType, String aliasName) {
        lastInferredIntmapType.set(intmapType);
        lastInferredIntmapTypeAliasName.set(aliasName);
    }
    
    /**
     * Get the last inferred intmap type alias name.
     * Thread-safe using ThreadLocal.
     * @return the last inferred intmap type alias name for the current thread, or null if none
     */
    public String getLastInferredIntmapTypeAliasName() {
        return lastInferredIntmapTypeAliasName.get();
    }
    
    /**
     * Clear the last inferred IntmapType.
     * Should be called after the IntmapType has been consumed.
     * Thread-safe using ThreadLocal.
     */
    public void clearLastInferredIntmapType() {
        lastInferredIntmapType.remove();
        lastInferredIntmapTypeAliasName.remove();
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
     * Get the set of imported files (global cache to prevent circular and duplicate imports).
     *
     * @return the set of imported files
     */
    public Set<String> getImportedFiles() {
        return importedFiles;
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
        // Use lowercase for consistent lookup since callbacks are stored with lowercase keys
        String lowerScreenName = screenName.toLowerCase();
        Runnable callback = screenRefreshCallbacks.get(lowerScreenName);
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
     * Get the screen configuration for a screen (before Stage creation).
     *
     * @param screenName the name of the screen
     * @return the ScreenConfig object, or null if not found
     */
    public ScreenConfig getScreenConfig(String screenName) {
        return screenConfigs.get(screenName.toLowerCase());
    }

    /**
     * Set the screen configuration for a screen (before Stage creation).
     *
     * @param screenName the name of the screen
     * @param config the ScreenConfig object
     */
    public void setScreenConfig(String screenName, ScreenConfig config) {
        screenConfigs.put(screenName.toLowerCase(), config);
    }

    /**
     * Check if a screen configuration exists.
     *
     * @param screenName the name of the screen
     * @return true if the screen configuration exists
     */
    public boolean hasScreenConfig(String screenName) {
        return screenConfigs.containsKey(screenName.toLowerCase());
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
        // Shutdown all event dispatchers before clearing
        for (ScreenEventDispatcher dispatcher : GLOBAL_SCREEN_DISPATCHERS.values()) {
            dispatcher.shutdown();
        }
        GLOBAL_SCREEN_DISPATCHERS.clear();
        GLOBAL_SCREENS.clear();
        GLOBAL_SCREEN_THREADS.clear();
        screenVars.clear();
        screenAreas.clear();
        displayMetadata.clear();
        GLOBAL_SCREENS_BEING_CREATED.clear();
        GLOBAL_SCREENS_NOT_YET_SHOWN.clear();
        screenVarSets.clear();
        screenVarItems.clear();
        screenAreaItems.clear();
        screenCallbacks.clear();
        GLOBAL_SCREEN_CREATION_ORDER.clear();
        screenStatuses.clear();
        screenErrorMessages.clear();
        screenConfigs.clear();
        declaredFunctions.clear();
        declaredScreens.clear();
        screenParentMap.clear();
    }

    public void remove(String screenName) {
        // Shutdown the event dispatcher for this screen
        ScreenEventDispatcher dispatcher = GLOBAL_SCREEN_DISPATCHERS.remove(screenName);
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
        GLOBAL_SCREENS.remove(screenName);
        GLOBAL_SCREEN_THREADS.remove(screenName);
        screenVars.remove(screenName);
        screenAreas.remove(screenName);
        screenVarSets.remove(screenName);
        screenVarItems.remove(screenName);
        screenAreaItems.remove(screenName);
        screenCallbacks.remove(screenName);
        screenStartupCode.remove(screenName);
        screenCleanupCode.remove(screenName);
        screenGainFocusCode.remove(screenName);
        screenLostFocusCode.remove(screenName);
        displayMetadata.entrySet().removeIf(entry -> entry.getKey().startsWith(screenName + "."));
        GLOBAL_SCREEN_CREATION_ORDER.remove(screenName);
        screenStatuses.remove(screenName);
        screenErrorMessages.remove(screenName);
        screenConfigs.remove(screenName);
        screenParentMap.remove(screenName);
    }

    /**
     * Close a screen by removing its runtime state (Stage, threads, status) but keeping
     * the screen configuration so the screen can be shown again with 'show screen'.
     * This transitions the screen from open/hidden state to defined state.
     * 
     * @param screenName the name of the screen to close
     */
    public void closeScreen(String screenName) {
        // Shutdown the event dispatcher for this screen
        ScreenEventDispatcher dispatcher = GLOBAL_SCREEN_DISPATCHERS.remove(screenName);
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
        GLOBAL_SCREENS.remove(screenName);
        GLOBAL_SCREEN_THREADS.remove(screenName);
        screenCallbacks.remove(screenName);
        GLOBAL_SCREEN_CREATION_ORDER.remove(screenName);
        screenStatuses.remove(screenName);
        screenErrorMessages.remove(screenName);
        screenParentMap.remove(screenName);
        // Note: screenConfigs, screenVars, screenAreas, screenVarSets, screenVarItems,
        // screenAreaItems, displayMetadata, screenStartupCode, screenCleanupCode,
        // screenGainFocusCode, and screenLostFocusCode are preserved so the screen
        // can be shown again.
    }

    /**
     * Get the callback function name for a screen
     * @param screenName The screen name
     * @return The callback function name, or null if no callback is set
     */
    public String getScreenCallback(String screenName) {
        return screenCallbacks.get(screenName);
    }

    /**
     * Set the callback function name for a screen
     * @param screenName The screen name
     * @param callbackName The callback function name
     */
    public void setScreenCallback(String screenName, String callbackName) {
        if (callbackName != null) {
            screenCallbacks.put(screenName, callbackName);
        } else {
            screenCallbacks.remove(screenName);
        }
    }

    /**
     * Get the startup code for a screen
     * @param screenName The screen name
     * @return The startup EBS code, or null if no startup code is set
     */
    public String getScreenStartupCode(String screenName) {
        return screenStartupCode.get(screenName);
    }

    /**
     * Set the startup code for a screen
     * @param screenName The screen name
     * @param code The startup EBS code
     */
    public void setScreenStartupCode(String screenName, String code) {
        if (code != null && !code.trim().isEmpty()) {
            screenStartupCode.put(screenName, code);
        } else {
            screenStartupCode.remove(screenName);
        }
    }

    /**
     * Get the cleanup code for a screen
     * @param screenName The screen name
     * @return The cleanup EBS code, or null if no cleanup code is set
     */
    public String getScreenCleanupCode(String screenName) {
        return screenCleanupCode.get(screenName);
    }

    /**
     * Set the cleanup code for a screen
     * @param screenName The screen name
     * @param code The cleanup EBS code
     */
    public void setScreenCleanupCode(String screenName, String code) {
        if (code != null && !code.trim().isEmpty()) {
            screenCleanupCode.put(screenName, code);
        } else {
            screenCleanupCode.remove(screenName);
        }
    }

    /**
     * Get the gainFocus code for a screen
     * @param screenName The screen name
     * @return The gainFocus EBS code, or null if no gainFocus code is set
     */
    public String getScreenGainFocusCode(String screenName) {
        return screenGainFocusCode.get(screenName);
    }

    /**
     * Set the gainFocus code for a screen
     * @param screenName The screen name
     * @param code The gainFocus EBS code
     */
    public void setScreenGainFocusCode(String screenName, String code) {
        if (code != null && !code.trim().isEmpty()) {
            screenGainFocusCode.put(screenName, code);
        } else {
            screenGainFocusCode.remove(screenName);
        }
    }

    /**
     * Get the lostFocus code for a screen
     * @param screenName The screen name
     * @return The lostFocus EBS code, or null if no lostFocus code is set
     */
    public String getScreenLostFocusCode(String screenName) {
        return screenLostFocusCode.get(screenName);
    }

    /**
     * Set the lostFocus code for a screen
     * @param screenName The screen name
     * @param code The lostFocus EBS code
     */
    public void setScreenLostFocusCode(String screenName, String code) {
        if (code != null && !code.trim().isEmpty()) {
            screenLostFocusCode.put(screenName, code);
        } else {
            screenLostFocusCode.remove(screenName);
        }
    }

    /**
     * Get the status of a screen
     * @param screenName The screen name
     * @return The screen status, or CLEAN if not set
     */
    public ScreenStatus getScreenStatus(String screenName) {
        return screenStatuses.getOrDefault(screenName.toLowerCase(), ScreenStatus.CLEAN);
    }

    /**
     * Set the status of a screen
     * @param screenName The screen name
     * @param status The new status
     */
    public void setScreenStatus(String screenName, ScreenStatus status) {
        if (status != null) {
            screenStatuses.put(screenName.toLowerCase(), status);
        } else {
            screenStatuses.put(screenName.toLowerCase(), ScreenStatus.CLEAN);
        }
    }

    /**
     * Get the error message for a screen
     * @param screenName The screen name
     * @return The error message, or null if no error
     */
    public String getScreenErrorMessage(String screenName) {
        return screenErrorMessages.get(screenName.toLowerCase());
    }

    /**
     * Set the error message for a screen
     * @param screenName The screen name
     * @param errorMessage The error message (can be null to clear)
     */
    public void setScreenErrorMessage(String screenName, String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            screenErrorMessages.put(screenName.toLowerCase(), errorMessage);
            // Automatically set status to ERROR when error message is set
            setScreenStatus(screenName, ScreenStatus.ERROR);
        } else {
            screenErrorMessages.remove(screenName.toLowerCase());
        }
    }

    /**
     * Get the map of declared functions (functionName -> source file/script name).
     * Used to track and prevent function name conflicts across imports.
     * 
     * @return the declared functions map
     */
    public Map<String, String> getDeclaredFunctions() {
        return declaredFunctions;
    }

    /**
     * Get the map of declared screens (screenName -> source file/script name).
     * Used to track and prevent screen name conflicts across imports.
     * 
     * @return the declared screens map
     */
    public Map<String, String> getDeclaredScreens() {
        return declaredScreens;
    }

    /**
     * Get the map of screen area containers.
     * The key is in the format "screenName.areaName" (lowercase).
     * Used for runtime property updates of area containers.
     *
     * @return the screen area containers map
     */
    public Map<String, javafx.scene.layout.Region> getScreenAreaContainers() {
        return screenAreaContainers;
    }

    /**
     * Get a specific area container by screen name and area name.
     *
     * @param screenName the screen name
     * @param areaName the area name
     * @return the Region container for the area, or null if not found
     */
    public javafx.scene.layout.Region getAreaContainer(String screenName, String areaName) {
        String key = screenName.toLowerCase() + "." + areaName.toLowerCase();
        return screenAreaContainers.get(key);
    }

    /**
     * Register an area container for later lookup.
     *
     * @param screenName the screen name
     * @param areaName the area name
     * @param container the Region container for the area
     */
    public void registerAreaContainer(String screenName, String areaName, javafx.scene.layout.Region container) {
        if (screenName != null && areaName != null && container != null) {
            String key = screenName.toLowerCase() + "." + areaName.toLowerCase();
            screenAreaContainers.put(key, container);
        }
    }

    /**
     * Set the parent screen for a child screen.
     * When a screen is shown from within another screen's context,
     * the child screen should be a child of the parent screen.
     *
     * @param childScreenName the child screen name
     * @param parentScreenName the parent screen name
     */
    public void setScreenParent(String childScreenName, String parentScreenName) {
        if (childScreenName != null && parentScreenName != null) {
            screenParentMap.put(childScreenName.toLowerCase(), parentScreenName.toLowerCase());
        }
    }

    /**
     * Get the parent screen name for a child screen.
     *
     * @param childScreenName the child screen name
     * @return the parent screen name, or null if no parent is set
     */
    public String getScreenParent(String childScreenName) {
        return childScreenName != null ? screenParentMap.get(childScreenName.toLowerCase()) : null;
    }

    /**
     * Check if a screen has a parent screen.
     *
     * @param screenName the screen name
     * @return true if the screen has a parent screen
     */
    public boolean hasScreenParent(String screenName) {
        return screenName != null && screenParentMap.containsKey(screenName.toLowerCase());
    }

    /**
     * Remove the parent relationship for a screen.
     *
     * @param screenName the screen name
     */
    public void removeScreenParent(String screenName) {
        if (screenName != null) {
            screenParentMap.remove(screenName.toLowerCase());
        }
    }

}
