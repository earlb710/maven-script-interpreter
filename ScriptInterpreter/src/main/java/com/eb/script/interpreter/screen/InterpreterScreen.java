package com.eb.script.interpreter.screen;

import com.eb.script.token.DataType;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.interpreter.statement.ScreenStatement;
import com.eb.script.interpreter.statement.ScreenShowStatement;
import com.eb.script.interpreter.statement.ScreenHideStatement;
import com.eb.script.interpreter.statement.ScreenCloseStatement;
import com.eb.script.interpreter.statement.ScreenSubmitStatement;
import com.eb.script.interpreter.screen.DisplayItem.ItemType;
import com.eb.script.interpreter.screen.AreaDefinition.AreaType;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InterpreterScreen handles all screen-related interpreter operations. This
 * includes creating screens, showing/hiding them, and managing screen state.
 */
public class InterpreterScreen {

    private final InterpreterContext context;
    private final Interpreter interpreter;

    public InterpreterScreen(InterpreterContext context, Interpreter interpreter) {
        this.context = context;
        this.interpreter = interpreter;
    }

    /**
     * Creates and starts a new daemon thread for a screen.
     * The thread runs an event loop that processes EBS code dispatched from UI event handlers.
     * This enables true thread-based screen isolation where stopping a screen thread
     * also stops its event handlers.
     * 
     * @param screenName The name of the screen for thread naming
     * @return The created and started thread
     */
    private Thread createScreenThread(String screenName) {
        // Create the code executor that will run EBS code on the screen thread
        ScreenEventDispatcher.CodeExecutor codeExecutor = (ebsCode) -> {
            try {
                // Set the screen context before executing code
                context.setCurrentScreen(screenName);
                try {
                    // Parse and execute the EBS code
                    RuntimeContext codeContext = com.eb.script.parser.Parser.parse("inline_" + screenName, ebsCode);
                    // Execute in the current interpreter context
                    for (com.eb.script.interpreter.statement.Statement s : codeContext.statements) {
                        interpreter.acceptStatement(s);
                    }
                    // If no return statement was executed, return null
                    return null;
                } catch (com.eb.script.interpreter.Interpreter.ReturnSignal rs) {
                    // Catch return statement and extract the value
                    return rs.value;
                } finally {
                    // Always clear the screen context after execution to prevent context leakage
                    context.clearCurrentScreen();
                }
            } catch (com.eb.script.parser.ParseError e) {
                throw new InterpreterError("Failed to parse inline code: " + e.getMessage());
            } catch (java.io.IOException e) {
                throw new InterpreterError("IO error executing inline code: " + e.getMessage());
            }
        };
        
        // Create the event dispatcher for this screen
        ScreenEventDispatcher dispatcher = new ScreenEventDispatcher(screenName, codeExecutor);
        context.setScreenEventDispatcher(screenName, dispatcher);
        
        Thread screenThread = new Thread(() -> {
            // Run the event loop - this blocks until shutdown is called
            dispatcher.runEventLoop();
            
            // When the event loop exits (thread stopping), close the screen
            // This ensures the screen closes when the thread is interrupted or shutdown
            Platform.runLater(() -> {
                Stage stage = context.getScreens().get(screenName);
                if (stage != null && stage.isShowing()) {
                    stage.close();
                }
                // Clean up screen state
                context.closeScreen(screenName);
            });
        }, "Screen-" + screenName);

        screenThread.setDaemon(true);
        screenThread.start();

        return screenThread;
    }

    /**
     * Cleans up the screen thread for a given screen.
     * If this is a top-level screen (no parent), the thread is interrupted.
     * If this is a child screen (has a parent), the thread is not interrupted 
     * since it's shared with the parent.
     * 
     * @param screenName The name of the screen
     */
    private void cleanupScreenThread(String screenName) {
        // Shutdown the event dispatcher first
        ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(screenName);
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
        
        // Only interrupt the thread if this is NOT a child screen
        // Child screens share their parent's thread, so we should not interrupt it
        String parentScreenName = context.getScreenParent(screenName);
        if (parentScreenName == null) {
            // This is a top-level screen - it owns its thread, so we can interrupt it
            Thread thread = context.getScreenThreads().get(screenName);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        // For child screens, we just remove the reference but don't interrupt the parent's thread
    }

    /**
     * Generates the qualified screen key for storage in global maps.
     * If called from within a parent screen context, the key will be "parentScreen.childScreen" (lowercase).
     * Otherwise, the key will be just the screen name (lowercase).
     * 
     * Note: The parent screen key is used as-is (it may already be qualified like "grandparent.parent").
     * This creates a hierarchy: grandparent.parent.child
     * 
     * @param screenName The screen name (without parent prefix)
     * @param parentScreenKey The parent screen key (may already be qualified), or null if not in a parent context
     * @return The qualified screen key in lowercase
     */
    private String getQualifiedScreenKey(String screenName, String parentScreenKey) {
        String key;
        if (parentScreenKey != null && !parentScreenKey.isEmpty()) {
            // Child screen - append to parent's key (supports nested hierarchy)
            key = parentScreenKey + "." + screenName;
        } else {
            // Top-level screen - use just the screen name
            key = screenName;
        }
        return key.toLowerCase();
    }

    /**
     * Visit a screen statement to define a new screen (does not create Stage until shown)
     */
    public void visitScreenStatement(ScreenStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1", stmt.name);
        try {
            // Check if screen has been shown (Stage exists in GLOBAL_SCREENS)
            // A screen that is currently in use cannot be replaced
            if (context.getScreens().containsKey(stmt.name)) {
                if (stmt.replaceExisting) {
                    throw interpreter.error(stmt.getLine(), 
                        "Screen '" + stmt.name + "' is currently in use (open or hidden). " +
                        "Close the screen first before replacing it with 'new screen'.");
                }
                // For normal screen definition, skip silently if already shown
                if (context.getOutput() != null) {
                    context.getOutput().printlnInfo("Screen '" + stmt.name + "' already exists and is in use - definition skipped");
                }
                return;
            }
            
            // Check if this screen name was already declared (even if not shown yet)
            if (context.getDeclaredScreens().containsKey(stmt.name)) {
                if (stmt.replaceExisting) {
                    // For 'new screen', remove the old definition first
                    context.remove(stmt.name);
                    // Continue to create new definition
                } else {
                    // For normal screen definition, skip silently if already declared
                    if (context.getOutput() != null) {
                        String existingSource = context.getDeclaredScreens().get(stmt.name);
                        context.getOutput().printlnInfo("Screen '" + stmt.name + "' already defined in " + existingSource + " - definition skipped");
                    }
                    return;
                }
            }
            
            // Register this screen as declared
            // If currently processing an import, use the import file name; otherwise use the current runtime context name
            String sourceName;
            if (interpreter.getCurrentImportFile() != null) {
                sourceName = interpreter.getCurrentImportFile();
            } else if (interpreter.getCurrentRuntime() != null) {
                sourceName = interpreter.getCurrentRuntime().name;
            } else {
                sourceName = "unknown";
            }
            context.getDeclaredScreens().put(stmt.name, sourceName);

            // Evaluate the spec (should be a JSON object)
            Object spec = interpreter.evaluate(stmt.spec);

            if (!(spec instanceof Map)) {
                throw interpreter.error(stmt.getLine(), "Screen configuration must be a JSON object (map).");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) spec;

            // Extract configuration properties with defaults
            String title = config.containsKey("title") ? String.valueOf(config.get("title")) : "Screen " + stmt.name;
            int width = config.containsKey("width") ? ((Number) config.get("width")).intValue() : 800;
            int height = config.containsKey("height") ? ((Number) config.get("height")).intValue() : 600;
            boolean maximize = config.containsKey("maximize") && Boolean.TRUE.equals(config.get("maximize"));
            
            // Extract startup and cleanup inline code if present
            String startupCode = config.containsKey("startup") ? String.valueOf(config.get("startup")) : null;
            String cleanupCode = config.containsKey("cleanup") ? String.valueOf(config.get("cleanup")) : null;
            // Extract gainFocus and lostFocus inline code if present
            String gainFocusCode = config.containsKey("gainfocus") ? String.valueOf(config.get("gainfocus")) : null;
            String lostFocusCode = config.containsKey("lostfocus") ? String.valueOf(config.get("lostfocus")) : null;
            
            // Store startup and cleanup code in context
            if (startupCode != null && !startupCode.trim().isEmpty()) {
                context.setScreenStartupCode(stmt.name, startupCode);
            }
            if (cleanupCode != null && !cleanupCode.trim().isEmpty()) {
                context.setScreenCleanupCode(stmt.name, cleanupCode);
            }
            // Store gainFocus and lostFocus code in context
            if (gainFocusCode != null && !gainFocusCode.trim().isEmpty()) {
                context.setScreenGainFocusCode(stmt.name, gainFocusCode);
            }
            if (lostFocusCode != null && !lostFocusCode.trim().isEmpty()) {
                context.setScreenLostFocusCode(stmt.name, lostFocusCode);
            }

            // Create thread-safe variable storage for this screen
            ConcurrentHashMap<String, Object> screenVarMap = new java.util.concurrent.ConcurrentHashMap<>();
            context.setScreenVars(stmt.name, screenVarMap);

            // Create thread-safe variable type storage for this screen
            ConcurrentHashMap<String, DataType> screenVarTypeMap = new java.util.concurrent.ConcurrentHashMap<>();
            context.setScreenVarTypes(stmt.name, screenVarTypeMap);

            // Initialize new storage structures for this screen
            Map<String, VarSet> varSetsMap = new java.util.HashMap<>();
            Map<String, Var> varItemsMap = new java.util.HashMap<>();
            Map<String, AreaItem> areaItemsMap = new java.util.HashMap<>();
            
            context.setScreenVarSets(stmt.name, varSetsMap);
            context.setScreenVarItems(stmt.name, varItemsMap);
            context.setScreenAreaItems(stmt.name, areaItemsMap);
            
            // Process variable sets if present (new structure)
            if (config.containsKey("sets")) {
                Object setsObj = config.get("sets");
                if (setsObj instanceof ArrayDynamic setsArray) {
                    @SuppressWarnings("unchecked")
                    List setsList = setsArray.getAll();
                    for (Object setObj : setsList) {
                        if (setObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> setDef = (Map<String, Object>) setObj;
                            
                            // Extract set properties
                            String setName = setDef.containsKey("setname") ? String.valueOf(setDef.get("setname")) : null;
                            // Get scope property (default to "visible")
                            String scope = setDef.containsKey("scope") ? String.valueOf(setDef.get("scope")) : "visible";
                            
                            if (setName == null || setName.isEmpty()) {
                                throw interpreter.error(stmt.getLine(), "Variable set in screen '" + stmt.name + "' must have a 'setname' property.");
                            }
                            
                            // Create VarSet
                            VarSet varSet = new VarSet(setName, scope);
                            varSetsMap.put(setName.toLowerCase(), varSet);
                            
                            // Process vars within this set
                            if (setDef.containsKey("vars")) {
                                Object varsObj = setDef.get("vars");
                                if (varsObj instanceof ArrayDynamic varsArray) {
                                    @SuppressWarnings("unchecked")
                                    List varsList = varsArray.getAll();
                                    processVariableList(varsList, setName, stmt.name, stmt.getLine(), varSet, varItemsMap, screenVarMap, screenVarTypeMap);
                                } else {
                                    throw interpreter.error(stmt.getLine(), "The 'vars' property in set '" + setName + "' must be an array.");
                                }
                            }
                        }
                    }
                } else {
                    throw interpreter.error(stmt.getLine(), "The 'sets' property in screen '" + stmt.name + "' must be an array.");
                }
            }
            // Process variable definitions if present (legacy structure for backward compatibility)
            else if (config.containsKey("vars")) {
                // Create a default set for legacy format
                String defaultSetName = "default";
                VarSet defaultVarSet = new VarSet(defaultSetName, "N");
                varSetsMap.put(defaultSetName.toLowerCase(), defaultVarSet);
                
                Object varsObj = config.get("vars");
                if (varsObj instanceof ArrayDynamic varsArray) {
                    @SuppressWarnings("unchecked")
                    List varsList = varsArray.getAll();
                    processVariableList(varsList, defaultSetName, stmt.name, stmt.getLine(), defaultVarSet, varItemsMap, screenVarMap, screenVarTypeMap);
                } else {
                    throw interpreter.error(stmt.getLine(), "The 'vars' property in screen '" + stmt.name + "' must be an array.");
                }
            }

            // Process area definitions if present
            if (config.containsKey("area")) {
                Object areaObj = config.get("area");
                if (areaObj instanceof ArrayDynamic varsArray) {
                    @SuppressWarnings("unchecked")
                    List<Object> areaList = varsArray.getAll();
                    java.util.List<AreaDefinition> areas = new java.util.ArrayList<>();

                    for (Object aObj : areaList) {
                        if (aObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> areaDef = (Map<String, Object>) aObj;

                            AreaDefinition area = parseAreaDefinition(areaDef, stmt.name, stmt.getLine());
                            areas.add(area);
                        }
                    }

                    context.setScreenAreas(stmt.name, areas);
                } else {
                    throw interpreter.error(stmt.getLine(), "The 'area' property in screen '" + stmt.name + "' must be an array.");
                }
            } else {
                // If no explicit areas defined but variables with display metadata exist,
                // create a default area with items for all displayed variables
                java.util.List<String> varsWithDisplay = new java.util.ArrayList<>();
                for (String key : context.getDisplayItem().keySet()) {
                    if (key.startsWith(stmt.name + ".")) {
                        String varName = key.substring(stmt.name.length() + 1);
                        varsWithDisplay.add(varName);
                    }
                }

                if (!varsWithDisplay.isEmpty()) {
                    // Create a default VBox area with items for all displayed variables
                    AreaDefinition defaultArea = new AreaDefinition();
                    defaultArea.areaType = AreaDefinition.AreaType.VBOX;
                    defaultArea.name = "default";
                    defaultArea.screenName = stmt.name;
                    defaultArea.style = "-fx-spacing: 10; -fx-padding: 10;";

                    // Sort variables by their seq field if present
                    List<String> sortedVarsWithDisplay = new ArrayList<>(varsWithDisplay);
                    sortedVarsWithDisplay.sort((v1, v2) -> {
                        DisplayItem d1 = context.getDisplayItem().get(stmt.name + "." + v1);
                        DisplayItem d2 = context.getDisplayItem().get(stmt.name + "." + v2);
                        Integer seq1 = (d1 != null && d1.seq != null) ? d1.seq : Integer.MAX_VALUE;
                        Integer seq2 = (d2 != null && d2.seq != null) ? d2.seq : Integer.MAX_VALUE;
                        return seq1.compareTo(seq2);
                    });

                    for (String varName : sortedVarsWithDisplay) {
                        // Get the display metadata for this variable
                        DisplayItem varDisplayItem = context.getDisplayItem().get(stmt.name + "." + varName);
                        
                        // Get the variable type to determine numeric vs string
                        DataType varType = screenVarTypeMap.get(varName);
                        
                        // Find which set this variable belongs to by checking varItemsMap
                        String setName = null;
                        Var currentVar = null;
                        for (Map.Entry<String, Var> entry : varItemsMap.entrySet()) {
                            String key = entry.getKey(); // format: "setname.varname"
                            Var var = entry.getValue();
                            // Check if this is our variable by comparing the var name part
                            if (key.endsWith("." + varName) && var.getName().equalsIgnoreCase(varName)) {
                                // Extract setname from the key
                                setName = key.substring(0, key.lastIndexOf("."));
                                currentVar = var;
                                break;
                            }
                        }
                        
                        // If we couldn't find the set, default to "default"
                        if (setName == null) {
                            setName = "default";
                        }
                        
                        // Only add labelText if explicitly specified - don't generate from variable name
                        // Set default alignment based on control type and variable type
                        if (varDisplayItem != null) {
                            // Add colon to labelText if specified and doesn't have one
                            if (varDisplayItem.labelText != null && !varDisplayItem.labelText.isEmpty() 
                                && !varDisplayItem.labelText.endsWith(":")) {
                                varDisplayItem.labelText = varDisplayItem.labelText + ":";
                            }
                            
                            // Set default alignment based on control type and variable type if not specified
                            if (varDisplayItem.alignment == null || varDisplayItem.alignment.isEmpty()) {
                                // Numeric fields should be right-aligned by default
                                // String fields (including TextField with string type) should be left-aligned
                                if (isNumericControl(varDisplayItem.itemType, varType)) {
                                    varDisplayItem.alignment = "right";
                                } else {
                                    // Explicitly set left alignment for string/text fields
                                    varDisplayItem.alignment = "left";
                                }
                            }
                            
                            // Set default label text alignment if not specified
                            if (varDisplayItem.labelTextAlignment == null || varDisplayItem.labelTextAlignment.isEmpty()) {
                                varDisplayItem.labelTextAlignment = "left";
                            }
                        }

                        // Create a single item for the input control (label will be added by ScreenFactory)
                        AreaItem item = new AreaItem();
                        item.name = varName;  // Use just the variable name
                        item.varRef = varName;
                        // Use seq from displayItem if available, otherwise use current size
                        item.sequence = (varDisplayItem != null && varDisplayItem.seq != null) 
                            ? varDisplayItem.seq 
                            : defaultArea.items.size();
                        item.displayItem = varDisplayItem;

                        // Set sizing to fit content, not stretch to screen width
                        item.maxWidth = "USE_PREF_SIZE";
                        item.hgrow = "NEVER";

                        // Set reasonable preferred widths based on item type
                        if (item.displayItem != null) {
                            switch (item.displayItem.itemType) {
                                case TEXTFIELD:
                                case PASSWORDFIELD:
                                    // Calculate width based on minChar/maxChar if available
                                    if (currentVar != null && (currentVar.getMinChar() != null || currentVar.getMaxChar() != null)) {
                                        // Use maxChar if available, otherwise minChar
                                        Integer charCount = currentVar.getMaxChar() != null ? currentVar.getMaxChar() : currentVar.getMinChar();
                                        // Approximate 8 pixels per character + 20 for padding
                                        int calculatedWidth = (charCount * 8) + 20;
                                        item.prefWidth = String.valueOf(Math.max(calculatedWidth, 80)); // Minimum 80 pixels
                                    } else {
                                        item.prefWidth = "300";
                                    }
                                    break;
                                case TEXTAREA:
                                    // Calculate width based on maxChar if available
                                    if (currentVar != null && currentVar.getMaxChar() != null) {
                                        // Approximate 8 pixels per character + 20 for padding
                                        int calculatedWidth = (currentVar.getMaxChar() * 8) + 20;
                                        item.prefWidth = String.valueOf(Math.max(calculatedWidth, 200)); // Minimum 200 pixels
                                    } else {
                                        item.prefWidth = "400";
                                    }
                                    item.prefHeight = "100";
                                    break;
                                case SLIDER:
                                    item.prefWidth = "300";
                                    break;
                                case COMBOBOX:
                                case CHOICEBOX:
                                    item.prefWidth = "200";
                                    break;
                                case COLORPICKER:
                                    item.prefWidth = "200";
                                    break;
                                case DATEPICKER:
                                    item.prefWidth = "200";
                                    break;
                                case TABLEVIEW:
                                    item.prefWidth = "800";
                                    item.maxWidth = "USE_COMPUTED_SIZE";
                                    item.hgrow = "ALWAYS";
                                    
                                    // If displayRecords is specified, use fixed height; otherwise allow growth
                                    if (item.displayItem != null && item.displayItem.displayRecords != null && item.displayItem.displayRecords > 0) {
                                        // Fixed height mode - displayRecords specifies exact number of visible rows
                                        // Don't set prefHeight/maxHeight here - AreaItemFactory will handle it
                                        item.vgrow = "NEVER";
                                    } else {
                                        // Dynamic height mode - TableView can grow with available space
                                        item.prefHeight = "400";
                                        item.maxHeight = "USE_COMPUTED_SIZE";
                                        item.vgrow = "ALWAYS";
                                    }
                                    break;
                                default:
                                    // Let other controls use default sizing
                                    break;
                            }
                        }

                        defaultArea.items.add(item);
                        
                        // Register item in areaItemsMap with format "setname.itemname" for screen.getItemList
                        String itemKey = setName + "." + item.name;
                        areaItemsMap.put(itemKey.toLowerCase(), item);
                    }

                    java.util.List<AreaDefinition> areas = new java.util.ArrayList<>();
                    areas.add(defaultArea);
                    context.setScreenAreas(stmt.name, areas);
                }
            }

            // Get the areas that were stored in context
            java.util.List<AreaDefinition> areas = context.getScreenAreas(stmt.name);

            // Store the screen configuration for lazy initialization
            ScreenConfig screenConfig = new ScreenConfig(
                stmt.name, title, width, height, maximize,
                screenVarMap, screenVarTypeMap,
                varSetsMap, varItemsMap, areaItemsMap,
                areas,
                startupCode, cleanupCode,
                gainFocusCode, lostFocusCode
            );
            
            context.setScreenConfig(stmt.name, screenConfig);
            
            // Register the screen name as a JSON variable in the environment
            // This allows the screen JSON to be accessed as a variable (e.g., xyz.title, xyz.width, etc.)
            interpreter.environment().getEnvironmentValues().define(stmt.name, config);
            
            // Output confirmation that screen definition was stored
            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Screen '" + stmt.name + "' defined. Use 'show screen " + stmt.name + ";' to display it.");
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Create the actual JavaFX Stage for a screen from its stored configuration.
     * This implements lazy initialization - the Stage is only created when first shown.
     * 
     * @param qualifiedKey The qualified screen key (parent.child or just child) for storage
     * @param baseScreenName The base screen name (without parent prefix) for config lookup
     * @param line The line number for error reporting
     * @throws InterpreterError if screen creation fails
     */
    private void createStageForScreen(String qualifiedKey, String baseScreenName, int line) throws InterpreterError {
        // Get the stored configuration - try qualified key first, then base name
        ScreenConfig config = context.getScreenConfig(qualifiedKey);
        if (config == null) {
            config = context.getScreenConfig(baseScreenName.toLowerCase());
        }
        if (config == null) {
            throw interpreter.error(line, "Screen '" + baseScreenName + "' does not exist. Create it first with 'screen " + baseScreenName + " = {...};'");
        }

        // Mark this screen as being created to prevent duplicate creation
        context.getScreensBeingCreated().add(qualifiedKey);

        final ScreenConfig finalConfig = config;
        
        // Check if we're already on the JavaFX Application Thread
        // If so, run directly to avoid deadlock (Platform.runLater + latch.await would deadlock)
        if (Platform.isFxApplicationThread()) {
            // Already on FX thread - run directly and let exceptions propagate
            try {
                createStageForScreenOnFxThread(qualifiedKey, finalConfig);
            } catch (Exception e) {
                context.getScreensBeingCreated().remove(qualifiedKey);
                if (e instanceof InterpreterError) {
                    throw (InterpreterError) e;
                }
                throw interpreter.error(line, "Failed to create screen: " + e.getMessage());
            }
        } else {
            // Not on FX thread - use Platform.runLater and wait with latch
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<Exception> creationError = new java.util.concurrent.atomic.AtomicReference<>();

            Platform.runLater(() -> {
                try {
                    createStageForScreenOnFxThread(qualifiedKey, finalConfig);
                } catch (Exception e) {
                    creationError.set(e);
                    context.getScreensBeingCreated().remove(qualifiedKey);
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Failed to create screen '" + qualifiedKey + "': " + e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });

            // Wait for stage creation to complete
            try {
                boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    context.getScreensBeingCreated().remove(qualifiedKey);
                    throw interpreter.error(line, "Screen creation timed out after 10 seconds");
                }
            } catch (InterruptedException e) {
                context.getScreensBeingCreated().remove(qualifiedKey);
                Thread.currentThread().interrupt();
                throw interpreter.error(line, "Screen creation was interrupted");
            }

            // Check if there was an error during creation
            if (creationError.get() != null) {
                throw interpreter.error(line, "Failed to create screen: " + creationError.get().getMessage());
            }
        }
    }

    /**
     * Helper method that performs the actual stage creation on the JavaFX Application Thread.
     * This method should only be called from the FX thread.
     * 
     * @param qualifiedKey The qualified screen key (parent.child or just child) for storage
     * @param config The screen configuration
     */
    private void createStageForScreenOnFxThread(String qualifiedKey, ScreenConfig config) {
        // Ensure we're on the FX thread
        assert Platform.isFxApplicationThread() : "This method must be called from the JavaFX Application Thread";
        
        Stage stage;

        // Get screen variables and areas from config
        ConcurrentHashMap<String, Object> varsMap = config.getScreenVars();
        ConcurrentHashMap<String, DataType> varTypesMap = config.getScreenVarTypes();
        java.util.List<AreaDefinition> areas = config.getAreas();

        // Use ScreenFactory if areas are defined, otherwise create simple stage
        if (areas != null && !areas.isEmpty()) {
            // Create screen with areas using ScreenFactory
            // Create onClick handler that dispatches EBS code to the screen thread
            ScreenFactory.OnClickHandler onClickHandler = new ScreenFactory.OnClickHandler() {
                @Override
                public void execute(String ebsCode) throws InterpreterError {
                    executeCode(ebsCode, false);
                }
                
                @Override
                public Object executeWithReturn(String ebsCode) throws InterpreterError {
                    return executeCode(ebsCode, true);
                }
                
                @Override
                public void executeDirect(String ebsCode) throws InterpreterError {
                    // Execute code directly on the calling thread (e.g., JavaFX thread)
                    // This is useful for button onClick handlers that need to show dialogs
                    // to avoid deadlocks between JavaFX thread and screen thread
                    executeCodeDirectly(ebsCode, false);
                    // Trigger UI refresh after execution
                    context.triggerScreenRefresh(qualifiedKey);
                }
                
                private Object executeCode(String ebsCode, boolean returnValue) throws InterpreterError {
                    // Get the screen's event dispatcher
                    ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(qualifiedKey);
                    
                    if (dispatcher != null && dispatcher.isRunning()) {
                        // Dispatch the code to the screen thread for execution
                        // Use synchronous dispatch so we can return a result and propagate errors
                        Object result = dispatcher.dispatchSync(ebsCode);
                        
                        // After event execution completes, trigger UI refresh on the JavaFX thread
                        // This ensures any variable changes are reflected in the UI
                        Platform.runLater(() -> {
                            context.triggerScreenRefresh(qualifiedKey);
                        });
                        
                        return returnValue ? result : null;
                    } else {
                        // Fallback: execute directly if dispatcher is not available
                        // This can happen for child screens that share parent's thread
                        return executeCodeDirectly(ebsCode, returnValue);
                    }
                }
                
                private Object executeCodeDirectly(String ebsCode, boolean returnValue) throws InterpreterError {
                    try {
                        // Set the screen context before executing code
                        // This allows inline code to use screen statements like "close screen;" or "hide screen;"
                        context.setCurrentScreen(qualifiedKey);
                        try {
                            // Parse and execute the EBS code
                            RuntimeContext clickContext = com.eb.script.parser.Parser.parse("inline_" + qualifiedKey, ebsCode);
                            // Execute in the current interpreter context
                            for (com.eb.script.interpreter.statement.Statement s : clickContext.statements) {
                                interpreter.acceptStatement(s);
                            }
                            // If no return statement was executed, return null
                            return null;
                        } catch (com.eb.script.interpreter.Interpreter.ReturnSignal rs) {
                            // Catch return statement and extract the value
                            return returnValue ? rs.value : null;
                        } finally {
                            // Always clear the screen context after execution to prevent context leakage
                            context.clearCurrentScreen();
                        }
                    } catch (com.eb.script.parser.ParseError e) {
                        throw new InterpreterError("Failed to parse inline code: " + e.getMessage());
                    } catch (java.io.IOException e) {
                        throw new InterpreterError("IO error executing inline code: " + e.getMessage());
                    }
                }
            };

            // Create ScreenDefinition and use it to create the Stage
            ScreenDefinition screenDef = ScreenFactory.createScreenDefinition(
                    qualifiedKey,
                    config.getTitle(),
                    config.getWidth(),
                    config.getHeight(),
                    areas,
                    varsMap,
                    varTypesMap,
                    onClickHandler,
                    context
            );
            stage = screenDef.createScreen();
        } else {
            // Create simple ScreenDefinition without areas
            ScreenDefinition screenDef = new ScreenDefinition(qualifiedKey, config.getTitle(), config.getWidth(), config.getHeight());
            stage = screenDef.createScreen();
        }

        // If this screen has a parent screen, set the owner relationship
        // This makes the child screen always appear on top of the parent and 
        // associates them for window management (minimizing, closing, etc.)
        String parentScreenName = context.getScreenParent(qualifiedKey);
        if (parentScreenName != null) {
            Stage parentStage = context.getScreens().get(parentScreenName);
            if (parentStage != null) {
                stage.initOwner(parentStage);
            }
        }

        if (config.isMaximize()) {
            stage.setMaximized(true);
        }
        
        // Set up screen-level focus listeners
        String screenGainFocusCode = config.getGainFocusCode();
        String screenLostFocusCode = config.getLostFocusCode();
        
        if (screenGainFocusCode != null || screenLostFocusCode != null) {
            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && screenGainFocusCode != null && !screenGainFocusCode.trim().isEmpty()) {
                    try {
                        executeScreenInlineCode(qualifiedKey, screenGainFocusCode, "gainFocus");
                    } catch (InterpreterError e) {
                        if (context.getOutput() != null) {
                            context.getOutput().printlnError("Error executing screen gainFocus code: " + e.getMessage());
                        }
                    }
                } else if (!newValue && screenLostFocusCode != null && !screenLostFocusCode.trim().isEmpty()) {
                    try {
                        executeScreenInlineCode(qualifiedKey, screenLostFocusCode, "lostFocus");
                    } catch (InterpreterError e) {
                        if (context.getOutput() != null) {
                            context.getOutput().printlnError("Error executing screen lostFocus code: " + e.getMessage());
                        }
                    }
                }
            });
        }

        // Handle thread assignment for this screen
        // If this screen has a parent, reuse the parent's thread and dispatcher instead of creating new ones
        String parentForThread = context.getScreenParent(qualifiedKey);
        if (parentForThread != null) {
            // Child screen: try to reuse the parent's thread and dispatcher
            Thread parentThread = context.getScreenThreads().get(parentForThread);
            ScreenEventDispatcher parentDispatcher = context.getScreenEventDispatcher(parentForThread);
            
            if (parentThread != null && parentThread.isAlive() && parentDispatcher != null && parentDispatcher.isRunning()) {
                // Parent thread and dispatcher exist and are alive, register the child screen to use them
                context.getScreenThreads().put(qualifiedKey, parentThread);
                context.setScreenEventDispatcher(qualifiedKey, parentDispatcher);
            } else {
                // Parent's thread not found or is dead (edge case), create a new thread
                Thread screenThread = createScreenThread(qualifiedKey);
                context.getScreenThreads().put(qualifiedKey, screenThread);
            }
        } else {
            // Top-level screen: create a new thread
            Thread screenThread = createScreenThread(qualifiedKey);
            context.getScreenThreads().put(qualifiedKey, screenThread);
        }

        // Set up cleanup when screen is closed
        stage.setOnCloseRequest(event -> {
            ScreenStatus status = context.getScreenStatus(qualifiedKey);
            
            if (status == ScreenStatus.CHANGED || status == ScreenStatus.ERROR) {
                event.consume();
                
                final String dialogMessage;
                final String dialogTitle;
                if (status == ScreenStatus.ERROR) {
                    String errorMsg = context.getScreenErrorMessage(qualifiedKey);
                    String msg = "Screen has an error";
                    if (errorMsg != null && !errorMsg.isEmpty()) {
                        msg += ": " + errorMsg;
                    }
                    msg += "\n\nAre you sure?";
                    dialogMessage = msg;
                    dialogTitle = "Error - Confirm Close";
                } else {
                    dialogMessage = "Screen has unsaved changes.\n\nAre you sure?";
                    dialogTitle = "Warning - Confirm Close";
                }
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        dialogMessage,
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO
                    );
                    confirm.setTitle(dialogTitle);
                    confirm.setHeaderText("Confirm Close");
                    
                    java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.YES) {
                        performScreenClose(qualifiedKey);
                        Stage stageToClose = context.getScreens().get(qualifiedKey);
                        if (stageToClose != null) {
                            stageToClose.close();
                        }
                    }
                });
            } else {
                performScreenClose(qualifiedKey);
            }
        });

        // Store the stage reference in global map using qualified key
        context.getScreens().put(qualifiedKey, stage);
        context.getScreensBeingCreated().remove(qualifiedKey);
        context.getScreenCreationOrder().add(qualifiedKey);

                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screen '" + qualifiedKey + "' created with title: " + config.getTitle());
                }
    }

    /**
     * Visit a screen show statement to display a screen
     */
    public void visitScreenShowStatement(ScreenShowStatement stmt) throws InterpreterError {
        String screenName = stmt.name;
        
        // If no screen name provided, determine from thread context
        if (screenName == null) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw interpreter.error(stmt.getLine(), 
                    "No screen name specified and not executing in a screen context. " +
                    "Use 'show screen <name>;' to show a specific screen, or call 'show screen;' " +
                    "from within screen event handlers (e.g., onClick).");
            }
        }
        
        // Detect if we're showing a screen from within another screen's context
        // getCurrentScreen() returns the qualified key of the current screen (e.g., "parent" or "grandparent.parent")
        String parentScreenKey = context.getCurrentScreen();
        
        // Generate the qualified screen key (parent.child or just child)
        // If called from within a parent screen, automatically add parent prefix
        String qualifiedScreenKey = getQualifiedScreenKey(screenName, parentScreenKey);
        
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 show", qualifiedScreenKey);
        try {
            // Check if screen configuration exists (check both qualified and unqualified names)
            boolean configExists = context.hasScreenConfig(qualifiedScreenKey) || context.hasScreenConfig(screenName.toLowerCase());
            boolean stageExists = context.getScreens().containsKey(qualifiedScreenKey);
            
            if (!configExists && !stageExists) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' does not exist. Create it first with 'screen " + screenName + " = {...};'");
            }

            // If we have a parent, record the parent-child relationship using the qualified key
            if (parentScreenKey != null && !parentScreenKey.equalsIgnoreCase(screenName)) {
                context.setScreenParent(qualifiedScreenKey, parentScreenKey.toLowerCase());
            }

            // Create the Stage if it doesn't exist yet (lazy initialization)
            if (!context.getScreens().containsKey(qualifiedScreenKey)) {
                createStageForScreen(qualifiedScreenKey, screenName, stmt.getLine());
            }

            Stage stage = context.getScreens().get(qualifiedScreenKey);
            if (stage == null) {
                throw interpreter.error(stmt.getLine(), "Screen '" + qualifiedScreenKey + "' is still being initialized. Please try again.");
            }

            final String finalScreenKey = qualifiedScreenKey;
            
            // Store the callback if specified (will be invoked on screen close)
            if (stmt.callbackName != null) {
                context.setScreenCallback(finalScreenKey, stmt.callbackName);
            }

            // Get startup code from config if not already stored (check both qualified and base names)
            ScreenConfig config = context.getScreenConfig(finalScreenKey);
            if (config == null) {
                config = context.getScreenConfig(screenName.toLowerCase());
            }
            if (config != null) {
                if (config.getStartupCode() != null && !config.getStartupCode().trim().isEmpty()) {
                    context.setScreenStartupCode(finalScreenKey, config.getStartupCode());
                }
                if (config.getCleanupCode() != null && !config.getCleanupCode().trim().isEmpty()) {
                    context.setScreenCleanupCode(finalScreenKey, config.getCleanupCode());
                }
            }

            // Show the screen on JavaFX Application Thread
            // If already on FX thread, run directly to avoid potential issues
            Runnable showTask = () -> {
                if (!stage.isShowing()) {
                    stage.show();
                    if (context.getOutput() != null) {
                        context.getOutput().printlnOk("Screen '" + finalScreenKey + "' shown");
                    }
                    
                    // Execute startup code if present (only on first show)
                    String startupCode = context.getScreenStartupCode(finalScreenKey);
                    if (startupCode != null && !startupCode.trim().isEmpty()) {
                        try {
                            executeScreenInlineCode(finalScreenKey, startupCode, "startup");
                        } catch (InterpreterError e) {
                            if (context.getOutput() != null) {
                                context.getOutput().printlnError("Error executing startup code: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    if (context.getOutput() != null) {
                        context.getOutput().printlnInfo("Screen '" + finalScreenKey + "' is already showing");
                    }
                }
            };
            
            if (Platform.isFxApplicationThread()) {
                showTask.run();
            } else {
                Platform.runLater(showTask);
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Invoke a screen callback function with screen event data as JSON
     */
    private void invokeScreenCallback(String callbackName, String screenName, String event, int line) throws InterpreterError {
        // Create JSON event data
        Map<String, Object> eventData = new java.util.HashMap<>();
        eventData.put("screenName", screenName);
        eventData.put("event", event);
        eventData.put("timestamp", System.currentTimeMillis());
        
        // Create a call statement to invoke the callback with the JSON data
        // Build the call: "call callbackName(eventData);"
        List<com.eb.script.interpreter.statement.Parameter> paramsList = new ArrayList<>();
        paramsList.add(new com.eb.script.interpreter.statement.Parameter("eventData", DataType.JSON, 
            new com.eb.script.interpreter.expression.LiteralExpression(DataType.JSON, eventData)));
        
        com.eb.script.interpreter.statement.CallStatement callStmt = 
            new com.eb.script.interpreter.statement.CallStatement(line, callbackName, paramsList);
        
        // Execute the call statement
        interpreter.visitCallStatement(callStmt);
    }

    /**
     * Collect all output and inout fields from a screen and invoke callback on close
     */
    private void collectOutputFieldsAndInvokeCallback(String screenName, int line) throws InterpreterError {
        // Check if there's a callback set for this screen
        String callbackName = context.getScreenCallback(screenName);
        if (callbackName == null) {
            return; // No callback, nothing to do
        }

        // Collect output fields (those with "out" or "inout" scope)
        Map<String, VarSet> varSets = context.getScreenVarSets(screenName);
        List<Map<String, Object>> outputFields = new ArrayList<>();
        
        if (varSets != null) {
            for (VarSet varSet : varSets.values()) {
                // Check if this varSet has output scope (out or inout)
                if (varSet.isOutput()) {
                    // Iterate through variables in this set
                    for (Var var : varSet.getVariables().values()) {
                        // Get the current value from screenVars
                        ConcurrentHashMap<String, Object> screenVars = context.getScreenVars(screenName);
                        if (screenVars != null) {
                            Object value = screenVars.get(var.getName());
                            
                            // Create field object
                            Map<String, Object> field = new java.util.HashMap<>();
                            field.put("name", var.getName());
                            field.put("type", var.getType() != null ? var.getType().toString() : "string");
                            field.put("value", value);
                            field.put("scope", varSet.getScope());
                            
                            outputFields.add(field);
                        }
                    }
                }
            }
        }

        // Create the callback data with output fields
        Map<String, Object> callbackData = new java.util.HashMap<>();
        callbackData.put("screenName", screenName);
        callbackData.put("event", "closed");
        callbackData.put("timestamp", System.currentTimeMillis());
        callbackData.put("fields", outputFields);
        
        // Invoke the callback with the output fields
        List<com.eb.script.interpreter.statement.Parameter> paramsList = new ArrayList<>();
        paramsList.add(new com.eb.script.interpreter.statement.Parameter("eventData", DataType.JSON, 
            new com.eb.script.interpreter.expression.LiteralExpression(DataType.JSON, callbackData)));
        
        com.eb.script.interpreter.statement.CallStatement callStmt = 
            new com.eb.script.interpreter.statement.CallStatement(line, callbackName, paramsList);
        
        // Execute the call statement
        interpreter.visitCallStatement(callStmt);
    }

    /**
     * Visit a screen hide statement to hide a screen
     */
    public void visitScreenHideStatement(ScreenHideStatement stmt) throws InterpreterError {
        String screenName = stmt.name;
        
        // If no screen name provided, determine from thread context
        if (screenName == null) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw interpreter.error(stmt.getLine(), 
                    "No screen name specified and not executing in a screen context. " +
                    "Use 'hide screen <name>;' to hide a specific screen, or call 'hide screen;' " +
                    "from within screen event handlers (e.g., onClick).");
            }
        }
        
        // Store original name for error messages before normalization
        final String originalScreenName = screenName;
        
        // Normalize screen name to lowercase
        screenName = screenName.toLowerCase();
        
        // Try to resolve the screen name - first try as-is, then try with parent prefix
        String resolvedScreenName = resolveScreenName(screenName);
        
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 hide", resolvedScreenName);
        try {
            // Check if screen config or stage exists
            if (!context.hasScreenConfig(resolvedScreenName) && !context.getScreens().containsKey(resolvedScreenName)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + resolvedScreenName + "' does not exist. Create it first with 'screen " + originalScreenName + " = {...};'");
            }

            // If stage doesn't exist yet, nothing to hide
            if (!context.getScreens().containsKey(resolvedScreenName)) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnInfo("Screen '" + resolvedScreenName + "' is not shown (has not been created yet)");
                }
                return;
            }

            Stage stage = context.getScreens().get(resolvedScreenName);
            if (stage == null) {
                throw interpreter.error(stmt.getLine(), "Screen '" + resolvedScreenName + "' is still being initialized. Please try again.");
            }

            final String finalScreenName = resolvedScreenName;
            
            // Hide the screen on JavaFX Application Thread
            // If already on FX thread, run directly
            Runnable hideTask = () -> {
                boolean wasShowing = stage.isShowing();
                stage.hide();
                if (context.getOutput() != null) {
                    if (wasShowing) {
                        context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden");
                    } else {
                        context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden (was already hidden)");
                    }
                }
            };
            
            if (Platform.isFxApplicationThread()) {
                hideTask.run();
            } else {
                Platform.runLater(hideTask);
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }
    
    /**
     * Resolves a screen name by first trying the name as-is, then trying with parent prefix.
     * This is used for hide/close/show operations to handle child screens that are registered
     * with their qualified name (parent.child).
     * 
     * @param screenName The screen name to resolve (should be lowercase)
     * @return The resolved screen name (may include parent prefix)
     */
    private String resolveScreenName(String screenName) {
        // First try as-is - check if the Stage exists (runtime state)
        // We prioritize Stage over config because for hide/close we need the runtime state
        if (context.getScreens().containsKey(screenName)) {
            return screenName;
        }
        
        // Try with parent prefix from the screen parent map
        String parentScreen = context.getScreenParent(screenName);
        if (parentScreen != null) {
            String qualifiedName = parentScreen + "." + screenName;
            if (context.getScreens().containsKey(qualifiedName)) {
                return qualifiedName;
            }
        }
        
        // Try using current screen context - if we're inside a parent screen,
        // the target screen might be parent.screenName
        String currentScreen = context.getCurrentScreen();
        if (currentScreen != null && !currentScreen.isEmpty()) {
            // If current screen is "parent.child", get the parent part
            int lastDot = currentScreen.lastIndexOf('.');
            if (lastDot > 0) {
                String parentFromContext = currentScreen.substring(0, lastDot);
                
                // Try parent.screenName
                String qualifiedFromContext = parentFromContext + "." + screenName;
                if (context.getScreens().containsKey(qualifiedFromContext)) {
                    return qualifiedFromContext;
                }
            }
            
            // Also check if the current screen is exactly "parent.screenName" or just "screenName"
            // This handles the case where we're inside askAiScreen and trying to hide askAiScreen
            // Use precise matching: either exact match or preceded by a dot
            String suffix = "." + screenName;
            if (currentScreen.equals(screenName) || currentScreen.endsWith(suffix)) {
                if (context.getScreens().containsKey(currentScreen)) {
                    return currentScreen;
                }
            }
        }
        
        // Finally, check if only config exists (for operations that can work with just config)
        if (context.hasScreenConfig(screenName)) {
            return screenName;
        }
        
        // Return original (caller will handle not found error)
        return screenName;
    }

    /**
     * Visit a screen close statement to close a screen (with or without name)
     */
    public void visitScreenCloseStatement(ScreenCloseStatement stmt) throws InterpreterError {
        String screenName = stmt.name;
        
        // If no screen name provided, determine from thread context
        if (screenName == null) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw interpreter.error(stmt.getLine(), 
                    "No screen name specified and not executing in a screen context. " +
                    "Use 'close screen <name>;' to close a specific screen, or call 'close screen;' " +
                    "from within screen event handlers (e.g., onClick).");
            }
        }
        
        // Normalize screen name to lowercase and resolve with parent prefix if needed
        screenName = resolveScreenName(screenName.toLowerCase());
        
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 close", screenName);
        try {
            // Check if screen config or stage exists
            if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' does not exist.");
            }

            // If stage doesn't exist yet, the screen is already in 'defined' state
            // Nothing to close, just inform the user
            if (!context.getScreens().containsKey(screenName)) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnInfo("Screen '" + screenName + "' is already closed (in defined state, not shown)");
                }
                return;
            }

            Stage stage = context.getScreens().get(screenName);
            if (stage == null) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' is still being initialized. Please try again.");
            }

            final String finalScreenName = screenName;
            
            // Close the screen on JavaFX Application Thread
            // If already on FX thread, run directly
            Runnable closeTask = () -> {
                // Close the stage
                if (stage.isShowing()) {
                    stage.close();
                }
                
                // Clean up the screen thread (only top-level screens have their threads interrupted)
                cleanupScreenThread(finalScreenName);
                
                // Close screen (remove runtime state but preserve configuration for re-use)
                context.closeScreen(finalScreenName);
                
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' closed");
                }
            };
            
            if (Platform.isFxApplicationThread()) {
                closeTask.run();
            } else {
                Platform.runLater(closeTask);
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Visit a screen submit statement to submit (close with callback) a screen
     */
    public void visitScreenSubmitStatement(ScreenSubmitStatement stmt) throws InterpreterError {
        String screenName = stmt.name;
        
        // If no screen name provided, determine from thread context
        if (screenName == null) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw interpreter.error(stmt.getLine(), 
                    "No screen name specified and not executing in a screen context. " +
                    "Use 'submit screen <name>;' to submit a specific screen, or call 'submit screen;' " +
                    "from within screen event handlers (e.g., onClick).");
            }
        }
        
        // Normalize screen name to lowercase and resolve with parent prefix if needed
        screenName = resolveScreenName(screenName.toLowerCase());
        
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 submit", screenName);
        try {
            // Check if screen config or stage exists
            if (!context.hasScreenConfig(screenName) && !context.getScreens().containsKey(screenName)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' does not exist.");
            }

            // Check if there's a callback set for this screen
            String callbackName = context.getScreenCallback(screenName);
            if (callbackName == null) {
                throw interpreter.error(stmt.getLine(), 
                    "Cannot submit screen '" + screenName + "': No callback was specified when the screen was shown. " +
                    "Use 'show screen " + screenName + " callback <functionName>;' to set a callback.");
            }

            // If stage doesn't exist yet, cannot submit
            if (!context.getScreens().containsKey(screenName)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' has not been shown yet.");
            }

            Stage stage = context.getScreens().get(screenName);
            if (stage == null) {
                throw interpreter.error(stmt.getLine(), "Screen '" + screenName + "' is still being initialized. Please try again.");
            }

            final String finalScreenName = screenName;
            
            // Submit the screen on JavaFX Application Thread
            // If already on FX thread, run directly
            Runnable submitTask = () -> {
                try {
                    // Collect output fields and invoke callback
                    collectOutputFieldsAndInvokeCallback(finalScreenName, 0);
                } catch (InterpreterError e) {
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Error invoking submit callback: " + e.getMessage());
                    }
                }
                
                // Close the stage
                if (stage.isShowing()) {
                    stage.close();
                }
                
                // Clean up the screen thread (only top-level screens have their threads interrupted)
                cleanupScreenThread(finalScreenName);
                
                // Close screen (remove runtime state but preserve configuration for re-use)
                context.closeScreen(finalScreenName);
                
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' submitted");
                }
            };
            
            if (Platform.isFxApplicationThread()) {
                submitTask.run();
            } else {
                Platform.runLater(submitTask);
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Helper method to capitalize words in a string (e.g., "myVariable" -> "My
     * Variable")
     */
    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Split camelCase or lowercase string into words
        String withSpaces = input.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");

        // Capitalize first letter of each word
        String[] words = withSpaces.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    /**
     * Helper method to determine if a control type is numeric based on control type and variable type
     */
    private boolean isNumericControl(DisplayItem.ItemType itemType, DataType varType) {
        if (itemType == null) {
            return false;
        }
        
        // Spinner and Slider are always numeric
        if (itemType == DisplayItem.ItemType.SPINNER || itemType == DisplayItem.ItemType.SLIDER) {
            return true;
        }
        
        // TextField is numeric only if the variable type is numeric
        if (itemType == DisplayItem.ItemType.TEXTFIELD && varType != null) {
            return varType == DataType.INTEGER || 
                   varType == DataType.LONG || 
                   varType == DataType.FLOAT || 
                   varType == DataType.DOUBLE;
        }
        
        return false;
    }

    /**
     * Perform the actual screen close cleanup operations.
     * This includes collecting output fields, invoking callbacks, stopping threads, and cleaning up resources.
     * The screen configuration is preserved so the screen can be shown again.
     * @param screenName The name of the screen to close
     */
    private void performScreenClose(String screenName) {
        // Execute cleanup code if present
        String cleanupCode = context.getScreenCleanupCode(screenName);
        if (cleanupCode != null && !cleanupCode.trim().isEmpty()) {
            try {
                executeScreenInlineCode(screenName, cleanupCode, "cleanup");
            } catch (InterpreterError e) {
                if (context.getOutput() != null) {
                    context.getOutput().printlnError("Error executing cleanup code: " + e.getMessage());
                }
            }
        }
        
        // Collect output fields and invoke callback if set
        try {
            collectOutputFieldsAndInvokeCallback(screenName, 0);
        } catch (InterpreterError e) {
            if (context.getOutput() != null) {
                context.getOutput().printlnError("Error invoking close callback: " + e.getMessage());
            }
        }
        
        // Clean up the screen thread (only top-level screens have their threads interrupted)
        cleanupScreenThread(screenName);
        
        // Close screen (remove runtime state but preserve configuration for re-use)
        context.closeScreen(screenName);
    }

    /**
     * Helper method to parse data type string to DataType enum
     */
    private DataType parseDataType(String typeStr) {
        if (typeStr == null) {
            return null;
        }

        // Handle array.type syntax (e.g., "array.record")
        String lowerType = typeStr.toLowerCase();
        if (lowerType.startsWith("array.")) {
            // For array types, we return ARRAY as the base type
            // The element type will be handled separately
            return DataType.ARRAY;
        }

        switch (lowerType) {
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
            case "record":
                return DataType.RECORD;
            default:
                return null;
        }
    }

    /**
     * Helper method to parse the element type from array type strings (e.g., "array.record" -> RECORD)
     */
    private DataType parseElementType(String typeStr) {
        if (typeStr == null) {
            return null;
        }
        
        String lowerType = typeStr.toLowerCase();
        if (lowerType.startsWith("array.")) {
            // Extract the element type (e.g., "array.record" -> "record")
            String elementTypeStr = lowerType.substring(6); // Skip "array."
            return parseDataType(elementTypeStr);
        }
        
        return null;
    }

    /**
     * Helper method to store display metadata for a variable
     */
    private void storeDisplayItem(String varName, Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = parseDisplayItem(displayDef, screenName);

        // Store the metadata using a composite key: screenName.varName
        String key = screenName + "." + varName;
        context.getDisplayItem().put(key, metadata);
    }

    private DisplayItem parseDisplayItem(Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = new DisplayItem();

        // Extract display type and convert to enum (case-insensitive lookup)
        Object typeObj = getCaseInsensitive(displayDef, "type");
        if (typeObj != null) {
            metadata.type = String.valueOf(typeObj).toLowerCase();
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

        // Check for both camelCase and lowercase versions - promptHelp (placeholder hint)
        if (displayDef.containsKey("promptHelp")) {
            metadata.promptHelp = String.valueOf(displayDef.get("promptHelp"));
        } else if (displayDef.containsKey("prompthelp")) {
            metadata.promptHelp = String.valueOf(displayDef.get("prompthelp"));
        }

        // Extract labelText (permanent label displayed before/above control)
        if (displayDef.containsKey("labelText")) {
            metadata.labelText = String.valueOf(displayDef.get("labelText"));
        } else if (displayDef.containsKey("labeltext")) {
            metadata.labelText = String.valueOf(displayDef.get("labeltext"));
        }

        // Extract labelText alignment
        if (displayDef.containsKey("labelTextAlignment")) {
            metadata.labelTextAlignment = String.valueOf(displayDef.get("labelTextAlignment")).toLowerCase();
        } else if (displayDef.containsKey("labeltextalignment")) {
            metadata.labelTextAlignment = String.valueOf(displayDef.get("labeltextalignment")).toLowerCase();
        }

        // Extract onClick event handler for buttons - check both camelCase and lowercase
        if (displayDef.containsKey("onClick")) {
            metadata.onClick = String.valueOf(displayDef.get("onClick"));
        } else if (displayDef.containsKey("onclick")) {
            metadata.onClick = String.valueOf(displayDef.get("onclick"));
        }

        // Extract options for selection controls (ComboBox, ChoiceBox, ListView)
        if (displayDef.containsKey("options")) {
            Object optionsObj = displayDef.get("options");
            if (optionsObj instanceof Map) {
                // Handle Map type: keys are display text, values are data values
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) optionsObj;
                metadata.optionsMap = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    metadata.optionsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            } else if (optionsObj instanceof ArrayDynamic) {
                metadata.options = new ArrayList<>();
                ArrayDynamic array = (ArrayDynamic) optionsObj;
                for (Object item : array.getAll()) {
                    metadata.options.add(String.valueOf(item));
                }
            } else if (optionsObj instanceof List) {
                metadata.options = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) optionsObj;
                for (Object item : list) {
                    metadata.options.add(String.valueOf(item));
                }
            }
        }

        // Extract columns for TableView
        if (displayDef.containsKey("columns")) {
            Object columnsObj = displayDef.get("columns");
            metadata.columns = new ArrayList<>();
            
            List<Object> columnList = null;
            if (columnsObj instanceof ArrayDynamic) {
                columnList = ((ArrayDynamic) columnsObj).getAll();
            } else if (columnsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) columnsObj;
                columnList = list;
            }
            
            if (columnList != null) {
                for (Object item : columnList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> colDef = (Map<String, Object>) item;
                        DisplayItem.TableColumn column = parseTableColumn(colDef);
                        metadata.columns.add(column);
                    }
                }
            }
        }
        
        // Extract displayRecords for TableView height calculation
        if (displayDef.containsKey("displayRecords")) {
            Object displayRecordsObj = displayDef.get("displayRecords");
            if (displayRecordsObj instanceof Number) {
                metadata.displayRecords = ((Number) displayRecordsObj).intValue();
            }
        } else if (displayDef.containsKey("displayrecords")) {
            Object displayRecordsObj = displayDef.get("displayrecords");
            if (displayRecordsObj instanceof Number) {
                metadata.displayRecords = ((Number) displayRecordsObj).intValue();
            }
        }
        
        // Extract treeItems for TreeView
        if (displayDef.containsKey("treeItems")) {
            Object treeItemsObj = displayDef.get("treeItems");
            metadata.treeItems = parseTreeItems(treeItemsObj);
        } else if (displayDef.containsKey("treeitems")) {
            Object treeItemsObj = displayDef.get("treeitems");
            metadata.treeItems = parseTreeItems(treeItemsObj);
        }
        
        // Extract expandAll for TreeView
        if (displayDef.containsKey("expandAll")) {
            Object expandAllObj = displayDef.get("expandAll");
            if (expandAllObj instanceof Boolean) {
                metadata.expandAll = (Boolean) expandAllObj;
            }
        } else if (displayDef.containsKey("expandall")) {
            Object expandAllObj = displayDef.get("expandall");
            if (expandAllObj instanceof Boolean) {
                metadata.expandAll = (Boolean) expandAllObj;
            }
        }
        
        // Extract showRoot for TreeView
        if (displayDef.containsKey("showRoot")) {
            Object showRootObj = displayDef.get("showRoot");
            if (showRootObj instanceof Boolean) {
                metadata.showRoot = (Boolean) showRootObj;
            }
        } else if (displayDef.containsKey("showroot")) {
            Object showRootObj = displayDef.get("showroot");
            if (showRootObj instanceof Boolean) {
                metadata.showRoot = (Boolean) showRootObj;
            }
        }

        // Extract label styling properties (for the label wrapper text)
        if (displayDef.containsKey("labelColor")) {
            metadata.labelColor = String.valueOf(displayDef.get("labelColor"));
        } else if (displayDef.containsKey("labelcolor")) {
            metadata.labelColor = String.valueOf(displayDef.get("labelcolor"));
        }

        if (displayDef.containsKey("labelBold")) {
            Object boldObj = displayDef.get("labelBold");
            if (boldObj instanceof Boolean) {
                metadata.labelBold = (Boolean) boldObj;
            }
        } else if (displayDef.containsKey("labelbold")) {
            Object boldObj = displayDef.get("labelbold");
            if (boldObj instanceof Boolean) {
                metadata.labelBold = (Boolean) boldObj;
            }
        }

        if (displayDef.containsKey("labelItalic")) {
            Object italicObj = displayDef.get("labelItalic");
            if (italicObj instanceof Boolean) {
                metadata.labelItalic = (Boolean) italicObj;
            }
        } else if (displayDef.containsKey("labelitalic")) {
            Object italicObj = displayDef.get("labelitalic");
            if (italicObj instanceof Boolean) {
                metadata.labelItalic = (Boolean) italicObj;
            }
        }

        if (displayDef.containsKey("labelFontSize")) {
            metadata.labelFontSize = String.valueOf(displayDef.get("labelFontSize"));
        } else if (displayDef.containsKey("labelfontsize")) {
            metadata.labelFontSize = String.valueOf(displayDef.get("labelfontsize"));
        }
        
        // Extract sequence number for ordering
        if (displayDef.containsKey("seq")) {
            Object seqObj = displayDef.get("seq");
            if (seqObj instanceof Number) {
                metadata.seq = ((Number) seqObj).intValue();
            }
        } else if (displayDef.containsKey("sequence")) {
            Object seqObj = displayDef.get("sequence");
            if (seqObj instanceof Number) {
                metadata.seq = ((Number) seqObj).intValue();
            }
        }

        // Extract item/control styling properties (for the control itself)
        if (displayDef.containsKey("itemColor")) {
            metadata.itemColor = String.valueOf(displayDef.get("itemColor"));
        } else if (displayDef.containsKey("itemcolor")) {
            metadata.itemColor = String.valueOf(displayDef.get("itemcolor"));
        }

        if (displayDef.containsKey("itemBold")) {
            Object boldObj = displayDef.get("itemBold");
            if (boldObj instanceof Boolean) {
                metadata.itemBold = (Boolean) boldObj;
            }
        } else if (displayDef.containsKey("itembold")) {
            Object boldObj = displayDef.get("itembold");
            if (boldObj instanceof Boolean) {
                metadata.itemBold = (Boolean) boldObj;
            }
        }

        if (displayDef.containsKey("itemItalic")) {
            Object italicObj = displayDef.get("itemItalic");
            if (italicObj instanceof Boolean) {
                metadata.itemItalic = (Boolean) italicObj;
            }
        } else if (displayDef.containsKey("itemitalic")) {
            Object italicObj = displayDef.get("itemitalic");
            if (italicObj instanceof Boolean) {
                metadata.itemItalic = (Boolean) italicObj;
            }
        }

        if (displayDef.containsKey("itemFontSize")) {
            metadata.itemFontSize = String.valueOf(displayDef.get("itemFontSize"));
        } else if (displayDef.containsKey("itemfontsize")) {
            metadata.itemFontSize = String.valueOf(displayDef.get("itemfontsize"));
        }

        // Extract maxLength (for control width calculation)
        if (displayDef.containsKey("maxLength")) {
            Object maxLenObj = displayDef.get("maxLength");
            if (maxLenObj instanceof Number) {
                metadata.maxLength = ((Number) maxLenObj).intValue();
            }
        } else if (displayDef.containsKey("maxlength")) {
            Object maxLenObj = displayDef.get("maxlength");
            if (maxLenObj instanceof Number) {
                metadata.maxLength = ((Number) maxLenObj).intValue();
            }
        }

        // Extract height (for textarea control height, number of lines to display)
        if (displayDef.containsKey("height")) {
            Object heightObj = displayDef.get("height");
            if (heightObj instanceof Number) {
                metadata.height = ((Number) heightObj).intValue();
            }
        }

        // Extract showSliderValue (for displaying slider value label)
        if (displayDef.containsKey("showSliderValue")) {
            Object showSliderValueObj = displayDef.get("showSliderValue");
            if (showSliderValueObj instanceof Boolean) {
                metadata.showSliderValue = (Boolean) showSliderValueObj;
            }
        } else if (displayDef.containsKey("showslidervalue")) {
            Object showSliderValueObj = displayDef.get("showslidervalue");
            if (showSliderValueObj instanceof Boolean) {
                metadata.showSliderValue = (Boolean) showSliderValueObj;
            }
        }

        // Extract or set default style
        if (displayDef.containsKey("style")) {
            metadata.style = String.valueOf(displayDef.get("style"));
        } else {
            // Use default style from the enum
            metadata.style = metadata.itemType.getDefaultStyle();
        }

        // Extract onChange event handler - fires whenever the control value changes
        if (displayDef.containsKey("onChange")) {
            metadata.onChange = String.valueOf(displayDef.get("onChange"));
        } else if (displayDef.containsKey("on_change")) {
            metadata.onChange = String.valueOf(displayDef.get("on_change"));
        } else if (displayDef.containsKey("onchange")) {
            metadata.onChange = String.valueOf(displayDef.get("onchange"));
        }

        // Extract onValidate event handler - for inline validation
        if (displayDef.containsKey("onValidate")) {
            metadata.onValidate = String.valueOf(displayDef.get("onValidate"));
        } else if (displayDef.containsKey("on_validate")) {
            metadata.onValidate = String.valueOf(displayDef.get("on_validate"));
        } else if (displayDef.containsKey("onvalidate")) {
            metadata.onValidate = String.valueOf(displayDef.get("onvalidate"));
        }

        metadata.screenName = screenName;

        return metadata;
    }
    
    /**
     * Helper method to parse a table column definition from JSON
     */
    private DisplayItem.TableColumn parseTableColumn(Map<String, Object> colDef) {
        DisplayItem.TableColumn column = new DisplayItem.TableColumn();
        
        if (colDef.containsKey("name")) {
            column.name = String.valueOf(colDef.get("name"));
        }
        if (colDef.containsKey("field")) {
            column.field = String.valueOf(colDef.get("field"));
        }
        if (colDef.containsKey("type")) {
            column.type = String.valueOf(colDef.get("type"));
        }
        if (colDef.containsKey("width")) {
            Object widthObj = colDef.get("width");
            if (widthObj instanceof Number) {
                column.width = ((Number) widthObj).intValue();
            }
        }
        if (colDef.containsKey("alignment")) {
            column.alignment = String.valueOf(colDef.get("alignment"));
        }
        
        return column;
    }
    
    /**
     * Parses tree items from a JSON array for TreeView.
     * Handles both List and ArrayDynamic inputs.
     * 
     * @param treeItemsObj The tree items object (List or ArrayDynamic)
     * @return A list of TreeItemDef objects representing the tree structure
     */
    private List<DisplayItem.TreeItemDef> parseTreeItems(Object treeItemsObj) {
        List<DisplayItem.TreeItemDef> treeItems = new ArrayList<>();
        
        List<Object> itemList = null;
        if (treeItemsObj instanceof ArrayDynamic) {
            itemList = ((ArrayDynamic) treeItemsObj).getAll();
        } else if (treeItemsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) treeItemsObj;
            itemList = list;
        }
        
        if (itemList != null) {
            for (Object item : itemList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemDef = (Map<String, Object>) item;
                    DisplayItem.TreeItemDef treeItem = parseTreeItemDef(itemDef);
                    treeItems.add(treeItem);
                } else if (item instanceof String) {
                    // Simple string value - create a leaf node
                    DisplayItem.TreeItemDef treeItem = new DisplayItem.TreeItemDef((String) item);
                    treeItems.add(treeItem);
                }
            }
        }
        
        return treeItems;
    }
    
    /**
     * Parses a single tree item definition from a JSON map.
     * Recursively parses children if present.
     * 
     * @param itemDef The JSON map containing tree item properties
     * @return A TreeItemDef representing the tree node
     */
    private DisplayItem.TreeItemDef parseTreeItemDef(Map<String, Object> itemDef) {
        DisplayItem.TreeItemDef treeItem = new DisplayItem.TreeItemDef();
        
        // Extract value (display text)
        if (itemDef.containsKey("value")) {
            treeItem.value = String.valueOf(itemDef.get("value"));
        } else if (itemDef.containsKey("text")) {
            treeItem.value = String.valueOf(itemDef.get("text"));
        } else if (itemDef.containsKey("name")) {
            treeItem.value = String.valueOf(itemDef.get("name"));
        }
        
        // Extract icon path (optional - used when no iconOpen/iconClosed specified)
        if (itemDef.containsKey("icon")) {
            treeItem.icon = String.valueOf(itemDef.get("icon"));
        }
        
        // Extract iconOpen path (optional - for expanded state)
        if (itemDef.containsKey("iconOpen")) {
            treeItem.iconOpen = String.valueOf(itemDef.get("iconOpen"));
        } else if (itemDef.containsKey("iconopen")) {
            treeItem.iconOpen = String.valueOf(itemDef.get("iconopen"));
        }
        
        // Extract iconClosed path (optional - for collapsed state)
        if (itemDef.containsKey("iconClosed")) {
            treeItem.iconClosed = String.valueOf(itemDef.get("iconClosed"));
        } else if (itemDef.containsKey("iconclosed")) {
            treeItem.iconClosed = String.valueOf(itemDef.get("iconclosed"));
        }
        
        // Extract expanded state (optional)
        if (itemDef.containsKey("expanded")) {
            Object expandedObj = itemDef.get("expanded");
            if (expandedObj instanceof Boolean) {
                treeItem.expanded = (Boolean) expandedObj;
            }
        }
        
        // Recursively parse children
        if (itemDef.containsKey("children")) {
            Object childrenObj = itemDef.get("children");
            treeItem.children = parseTreeItems(childrenObj);
        }
        
        return treeItem;
    }

    /**
     * Helper method to get a value from a map with case-insensitive key lookup.
     * Returns null if key is not found.
     */
    private Object getCaseInsensitive(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        // First try exact match
        if (map.containsKey(key)) {
            return map.get(key);
        }
        // Try case-insensitive match
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Helper method to check if a map contains a key (case-insensitive).
     */
    private boolean containsKeyCaseInsensitive(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return false;
        }
        // First try exact match
        if (map.containsKey(key)) {
            return true;
        }
        // Try case-insensitive match
        for (String mapKey : map.keySet()) {
            if (mapKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
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
            throw interpreter.error(line, "Area definition in screen '" + screenName + "' must have a 'name' property.");
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
        
        // Extract title property (for titled containers)
        if (areaDef.containsKey("title")) {
            area.title = String.valueOf(areaDef.get("title"));
        }
        
        // Extract groupBorder property (for visual grouping) - case-insensitive
        Object groupBorderObj = getCaseInsensitive(areaDef, "groupBorder");
        if (groupBorderObj != null) {
            area.groupBorder = String.valueOf(groupBorderObj);
        }
        
        // Extract groupBorderColor property (for visual grouping) - case-insensitive
        Object groupBorderColorObj = getCaseInsensitive(areaDef, "groupBorderColor");
        if (groupBorderColorObj != null) {
            area.groupBorderColor = String.valueOf(groupBorderColorObj);
        }
        
        // Extract groupBorderWidth property (for visual grouping) - case-insensitive
        Object groupBorderWidthObj = getCaseInsensitive(areaDef, "groupBorderWidth");
        if (groupBorderWidthObj != null) {
            area.groupBorderWidth = String.valueOf(groupBorderWidthObj);
        }
        
        // Extract groupBorderInsets property (for visual grouping) - case-insensitive
        Object groupBorderInsetsObj = getCaseInsensitive(areaDef, "groupBorderInsets");
        if (groupBorderInsetsObj != null) {
            area.groupBorderInsets = String.valueOf(groupBorderInsetsObj);
        }
        
        // Extract groupBorderRadius property (for visual grouping) - case-insensitive
        Object groupBorderRadiusObj = getCaseInsensitive(areaDef, "groupBorderRadius");
        if (groupBorderRadiusObj != null) {
            area.groupBorderRadius = String.valueOf(groupBorderRadiusObj);
        }
        
        // Extract groupLabelText property (for visual grouping) - case-insensitive
        Object groupLabelTextObj = getCaseInsensitive(areaDef, "groupLabelText");
        if (groupLabelTextObj != null) {
            area.groupLabelText = String.valueOf(groupLabelTextObj);
        }
        
        // Extract groupLabelAlignment property (for visual grouping) - case-insensitive
        Object groupLabelAlignmentObj = getCaseInsensitive(areaDef, "groupLabelAlignment");
        if (groupLabelAlignmentObj != null) {
            area.groupLabelAlignment = String.valueOf(groupLabelAlignmentObj);
        }
        
        // Extract groupLabelOffset property (for visual grouping) - case-insensitive
        Object groupLabelOffsetObj = getCaseInsensitive(areaDef, "groupLabelOffset");
        if (groupLabelOffsetObj != null) {
            area.groupLabelOffset = String.valueOf(groupLabelOffsetObj);
        }
        
        // Extract groupLabelColor property (for visual grouping) - case-insensitive
        Object groupLabelColorObj = getCaseInsensitive(areaDef, "groupLabelColor");
        if (groupLabelColorObj != null) {
            area.groupLabelColor = String.valueOf(groupLabelColorObj);
        }
        
        // Extract groupLabelBackground property (for visual grouping) - case-insensitive
        Object groupLabelBackgroundObj = getCaseInsensitive(areaDef, "groupLabelBackground");
        if (groupLabelBackgroundObj != null) {
            area.groupLabelBackground = String.valueOf(groupLabelBackgroundObj);
        }
        
        // Extract areaBackground property - case-insensitive
        Object areaBackgroundObj = getCaseInsensitive(areaDef, "areaBackground");
        if (areaBackgroundObj != null) {
            area.areaBackground = String.valueOf(areaBackgroundObj);
        }
        
        // Extract spacing property
        if (areaDef.containsKey("spacing")) {
            area.spacing = String.valueOf(areaDef.get("spacing"));
        }
        
        // Extract padding property
        if (areaDef.containsKey("padding")) {
            area.padding = String.valueOf(areaDef.get("padding"));
        }
        
        // Extract gainFocus and lostFocus inline code if present
        if (areaDef.containsKey("gainfocus")) {
            area.gainFocus = String.valueOf(areaDef.get("gainfocus"));
        }
        if (areaDef.containsKey("lostfocus")) {
            area.lostFocus = String.valueOf(areaDef.get("lostfocus"));
        }
        
        // Extract numberOfRecords property for multi-record areas
        Object numberOfRecordsObj = getCaseInsensitive(areaDef, "numberOfRecords");
        if (numberOfRecordsObj != null && numberOfRecordsObj instanceof Number) {
            area.numberOfRecords = ((Number) numberOfRecordsObj).intValue();
        }
        
        // Extract recordRef property for multi-record areas
        Object recordRefObj = getCaseInsensitive(areaDef, "recordRef");
        if (recordRefObj != null) {
            area.recordRef = String.valueOf(recordRefObj);
        }

        area.screenName = screenName;

        // Process items in the area
        if (areaDef.containsKey("items")) {
            Object itemsObj = areaDef.get("items");
            List<Object> itemsList = null;
            if (itemsObj instanceof ArrayDynamic) {
                itemsList = ((ArrayDynamic) itemsObj).getAll();
            } else if (itemsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) itemsObj;
                itemsList = list;
            }

            if (itemsList != null) {
                for (Object itemObj : itemsList) {
                    if (itemObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemDef = (Map<String, Object>) itemObj;

                        AreaItem item = new AreaItem();

                        // Extract item properties
                        if (itemDef.containsKey("name")) {
                            item.name = String.valueOf(itemDef.get("name"));
                        }

                        // Support both "sequence" and "seq" for compactness
                        if (itemDef.containsKey("sequence")) {
                            Object seqObj = itemDef.get("sequence");
                            if (seqObj instanceof Number) {
                                item.sequence = ((Number) seqObj).intValue();
                            }
                        } else if (itemDef.containsKey("seq")) {
                            Object seqObj = itemDef.get("seq");
                            if (seqObj instanceof Number) {
                                item.sequence = ((Number) seqObj).intValue();
                            }
                        }

                        if (itemDef.containsKey("layoutpos")) {
                            item.layoutPos = String.valueOf(itemDef.get("layoutpos"));
                        } else if (itemDef.containsKey("layout_pos")) {
                            // Support both camelCase and snake_case
                            item.layoutPos = String.valueOf(itemDef.get("layout_pos"));
                        } else if (itemDef.containsKey("relativepos")) {
                            // Backward compatibility with old name
                            item.layoutPos = String.valueOf(itemDef.get("relativepos"));
                        } else if (itemDef.containsKey("relative_pos")) {
                            // Backward compatibility with old name (snake_case)
                            item.layoutPos = String.valueOf(itemDef.get("relative_pos"));
                        }

                        // Check for varRef with case-insensitive lookup
                        Object varRefValue = getCaseInsensitive(itemDef, "varref");
                        if (varRefValue != null) {
                            item.varRef = String.valueOf(varRefValue).toLowerCase();
                        }

                        // Process optional display metadata for the item
                        // If not provided, the item will use the DisplayItem from varRef
                        if (itemDef.containsKey("display")) {
                            Object displayObj = itemDef.get("display");
                            if (displayObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> displayDef = (Map<String, Object>) displayObj;

                                // Parse display metadata for this specific item
                                item.displayItem = parseDisplayItem(displayDef, screenName);
                            }
                        } else if (getCaseInsensitive(itemDef, "type") != null && 
                                   getCaseInsensitive(itemDef, "varref") == null) {
                            // If item has a direct "type" property (e.g., button, label) without a varRef,
                            // treat the item definition itself as the display definition
                            item.displayItem = parseDisplayItem(itemDef, screenName);
                        }
                        // If displayItem is not set here, it will remain null
                        // and the consuming code should fall back to using varRef's DisplayItem

                        // Parse additional UI properties for the item
                        // promptHelp (formerly promptText) now goes into displayItem
                        if (itemDef.containsKey("prompthelp") || itemDef.containsKey("prompt_help")) {
                            String promptHelp = itemDef.containsKey("prompthelp")
                                    ? String.valueOf(itemDef.get("prompthelp"))
                                    : String.valueOf(itemDef.get("prompt_help"));

                            // If displayItem doesn't exist yet, create it
                            if (item.displayItem == null) {
                                item.displayItem = new DisplayItem();
                            }
                            item.displayItem.promptHelp = promptHelp;
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
                        
                        // Event handlers
                        // onValidate handler
                        if (itemDef.containsKey("onValidate")) {
                            Object val = itemDef.get("onValidate");
                            if (val != null) item.onValidate = String.valueOf(val);
                        } else if (itemDef.containsKey("on_validate")) {
                            Object val = itemDef.get("on_validate");
                            if (val != null) item.onValidate = String.valueOf(val);
                        } else if (itemDef.containsKey("onvalidate")) {
                            Object val = itemDef.get("onvalidate");
                            if (val != null) item.onValidate = String.valueOf(val);
                        }
                        
                        // onChange handler
                        if (itemDef.containsKey("onChange")) {
                            Object val = itemDef.get("onChange");
                            if (val != null) item.onChange = String.valueOf(val);
                        } else if (itemDef.containsKey("on_change")) {
                            Object val = itemDef.get("on_change");
                            if (val != null) item.onChange = String.valueOf(val);
                        } else if (itemDef.containsKey("onchange")) {
                            Object val = itemDef.get("onchange");
                            if (val != null) item.onChange = String.valueOf(val);
                        }

                        // Store in screenAreaItems map by item name (for screen.getProperty/setProperty)
                        if (item.name != null && !item.name.isEmpty()) {
                            Map<String, AreaItem> areaItemsMap = context.getScreenAreaItems(screenName);
                            if (areaItemsMap != null) {
                                // Store by item name (lowercase) for direct lookup
                                areaItemsMap.put(item.name.toLowerCase(), item);
                            }
                        }
                        
                        // Also store by varRef if present (for variable-to-item linking)
                        if (item.varRef != null && !item.varRef.isEmpty()) {
                            Map<String, AreaItem> areaItemsMap = context.getScreenAreaItems(screenName);
                            if (areaItemsMap != null) {
                                // Store with the same key format as variables (setname.varname in lowercase)
                                // For backward compatibility, check if varRef contains a dot (already qualified)
                                String areaItemKey;
                                if (item.varRef.contains(".")) {
                                    areaItemKey = item.varRef.toLowerCase();
                                } else {
                                    // If no set prefix, use "default.varname" for legacy format
                                    areaItemKey = "default." + item.varRef.toLowerCase();
                                }
                                areaItemsMap.put(areaItemKey, item);
                            }
                        }

                        area.items.add(item);
                    }
                }

                // Sort items by sequence
                area.items.sort((a, b) -> Integer.compare(a.sequence, b.sequence));
            }
        }

        // Process nested child areas (areas within areas)
        // Check for both "areas" and "childAreas" (case-insensitive)
        Object areasObj = getCaseInsensitive(areaDef, "areas");
        if (areasObj == null) {
            areasObj = getCaseInsensitive(areaDef, "childAreas");
        }
        
        if (areasObj != null) {
            List<Object> areasList = null;

            // Handle both List and ArrayDynamic (JSON always uses ArrayDynamic)
            if (areasObj instanceof ArrayDynamic) {
                areasList = ((ArrayDynamic) areasObj).getAll();
            } else if (areasObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) areasObj;
                areasList = list;
            }

            if (areasList != null) {
                for (Object childAreaObj : areasList) {
                    if (childAreaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> childAreaDef = (Map<String, Object>) childAreaObj;

                        // Recursively parse child area
                        AreaDefinition childArea = parseAreaDefinition(childAreaDef, screenName, line);
                        area.childAreas.add(childArea);
                    }
                }
            }
        }

        return area;
    }
    
    /**
     * Resolve variable references in JSON values.
     * Handles both VariableReference objects (from unquoted $var in JSON)
     * and string values starting with '$' (for backward compatibility).
     * 
     * @param value The value to resolve (can be any type)
     * @param line The line number for error reporting
     * @return The resolved value (unchanged if not a $ reference, or the variable's value if it is)
     * @throws InterpreterError if the variable doesn't exist
     */
    private Object resolveVariableReference(Object value, int line) throws InterpreterError {
        // Handle VariableReference objects created by JSON parser (unquoted $variable syntax)
        if (value instanceof com.eb.script.json.Json.VariableReference) {
            com.eb.script.json.Json.VariableReference varRef = (com.eb.script.json.Json.VariableReference) value;
            String varName = varRef.variableName.toLowerCase(); // All variable names are case-insensitive and stored in lowercase
            
            // Try to get the variable value from the environment
            try {
                return interpreter.environment().get(varName);
            } catch (InterpreterError e) {
                // Re-throw with more context about the $ reference
                throw interpreter.error(line, "Variable reference '$" + varRef.variableName + "' not found in scope");
            }
        }
        
        // Quoted strings (e.g., "$userName") are treated as literal strings, not variable references
        // Only unquoted $variable syntax (handled above as VariableReference) resolves to variable values
        return value;
    }
    
    /**
     * Helper method to process a list of variable definitions
     */
    private void processVariableList(List varsList, String setName, String screenName, int line,
                                     VarSet varSet, Map<String, Var> varItemsMap,
                                     java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap,
                                     java.util.concurrent.ConcurrentHashMap<String, DataType> screenVarTypeMap) throws InterpreterError {
        for (Object varObj : varsList) {
            if (varObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> varDef = (Map<String, Object>) varObj;

                // Extract variable properties
                String varName = varDef.containsKey("name") ? String.valueOf(varDef.get("name")).toLowerCase() : null;
                String varTypeStr = varDef.containsKey("type") ? String.valueOf(varDef.get("type")).toLowerCase() : null;
                Object defaultValue = varDef.get("default");
                
                // Resolve variable references (e.g., "$myVar" -> value of myVar)
                defaultValue = resolveVariableReference(defaultValue, line);
                
                // Extract minChar and maxChar if present
                Integer minChar = null;
                Integer maxChar = null;
                if (varDef.containsKey("minChar")) {
                    Object minCharObj = varDef.get("minChar");
                    if (minCharObj instanceof Number) {
                        minChar = ((Number) minCharObj).intValue();
                    }
                }
                if (varDef.containsKey("maxChar")) {
                    Object maxCharObj = varDef.get("maxChar");
                    if (maxCharObj instanceof Number) {
                        maxChar = ((Number) maxCharObj).intValue();
                    }
                }
                
                // Extract case property if present
                String textCase = null;
                if (varDef.containsKey("case")) {
                    Object caseObj = varDef.get("case");
                    if (caseObj != null) {
                        String caseStr = String.valueOf(caseObj).toLowerCase();
                        // Validate case value
                        if (caseStr.equals("upper") || caseStr.equals("lower") || caseStr.equals("mixed")) {
                            textCase = caseStr;
                        }
                    }
                }

                if (varName == null || varName.isEmpty()) {
                    throw interpreter.error(line, "Variable definition in screen '" + screenName + "' must have a 'name' property.");
                }

                // Convert type string to DataType
                DataType varType = null;
                DataType elementType = null;
                boolean isArrayType = false;
                
                if (varTypeStr != null) {
                    // Check if it's an array type (e.g., "array.record")
                    if (varTypeStr.toLowerCase().startsWith("array.")) {
                        isArrayType = true;
                        elementType = parseElementType(varTypeStr);
                        if (elementType == null) {
                            throw interpreter.error(line, "Unknown element type in '" + varTypeStr + "' for variable '" + varName + "' in screen '" + screenName + "'.");
                        }
                    }
                    
                    varType = parseDataType(varTypeStr);
                    if (varType == null) {
                        throw interpreter.error(line, "Unknown type '" + varTypeStr + "' for variable '" + varName + "' in screen '" + screenName + "'.");
                    }
                }

                // Convert and set the default value
                Object value = defaultValue;
                Object recordTemplate = null;
                
                // Special handling for array.record: default is a template, not the array value
                if (isArrayType && elementType == DataType.RECORD && defaultValue != null) {
                    // For array.record, default should be a single record (template)
                    // Convert the template using elementType to ensure proper validation
                    recordTemplate = elementType.convertValue(defaultValue);
                    // Initialize as empty ArrayList for array types
                    value = new ArrayList<>();
                } else if (varType != null && value != null) {
                    // For other types, convert normally
                    value = varType.convertValue(value);
                }

                // Create Var object
                Var var = new Var(varName, varType, defaultValue);
                var.setValue(value);
                var.setSetName(setName);
                var.setMinChar(minChar);
                var.setMaxChar(maxChar);
                var.setTextCase(textCase);
                
                // Set array-specific properties
                if (isArrayType) {
                    var.setArrayType(true);
                    var.setElementType(elementType);
                    if (recordTemplate != null) {
                        var.setRecordTemplate(recordTemplate);
                    }
                }

                // Process optional display metadata
                if (varDef.containsKey("display")) {
                    Object displayObj = varDef.get("display");
                    if (displayObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> displayDef = (Map<String, Object>) displayObj;

                        // Parse and store display metadata
                        DisplayItem displayItem = parseDisplayItem(displayDef, screenName);
                        
                        // Copy textCase from Var to DisplayItem if not already set
                        if (displayItem.caseFormat == null && textCase != null) {
                            displayItem.caseFormat = textCase;
                        }
                        
                        var.setDisplayItem(displayItem);
                        
                        // Store display metadata with the old key format for backward compatibility
                        storeDisplayItem(varName, displayDef, screenName);
                    }
                }

                // Add to VarSet
                varSet.addVariable(var);

                // Add to varItemsMap with key "setname.varname" (both lowercase)
                String varKey = setName.toLowerCase() + "." + varName.toLowerCase();
                varItemsMap.put(varKey, var);

                // Store in screen's thread-safe variable map (legacy support)
                screenVarMap.put(varName, value);

                // Store the variable type if specified (legacy support)
                if (varType != null) {
                    screenVarTypeMap.put(varName, varType);
                }
            }
        }
    }
    
    /**
     * Execute inline EBS code in the context of a screen.
     * This is used for startup, cleanup, and other screen lifecycle events.
     * 
     * @param screenName The name of the screen
     * @param ebsCode The EBS code to execute
     * @param eventType The type of event (e.g., "startup", "cleanup") for error messages
     * @throws InterpreterError if the code fails to parse or execute
     */
    private void executeScreenInlineCode(String screenName, String ebsCode, String eventType) throws InterpreterError {
        // Try to use the screen's event dispatcher for consistent thread handling
        ScreenEventDispatcher dispatcher = context.getScreenEventDispatcher(screenName);
        
        if (dispatcher != null && dispatcher.isRunning()) {
            // Use the dispatcher to execute on the screen thread
            try {
                dispatcher.dispatchSync(ebsCode);
            } catch (InterpreterError e) {
                throw new InterpreterError("Failed to execute " + eventType + " code: " + e.getMessage());
            }
        } else {
            // Fallback: execute directly if dispatcher is not available
            try {
                // Set the screen context before executing inline code
                context.setCurrentScreen(screenName);
                try {
                    // Parse and execute the EBS code
                    RuntimeContext eventContext = com.eb.script.parser.Parser.parse(eventType + "_" + screenName, ebsCode);
                    // Execute in the current interpreter context
                    for (com.eb.script.interpreter.statement.Statement s : eventContext.statements) {
                        interpreter.acceptStatement(s);
                    }
                } finally {
                    // Always clear the screen context after execution to prevent context leakage
                    context.clearCurrentScreen();
                }
            } catch (com.eb.script.parser.ParseError e) {
                throw new InterpreterError("Failed to parse " + eventType + " code: " + e.getMessage());
            } catch (java.io.IOException e) {
                throw new InterpreterError("IO error executing " + eventType + " code: " + e.getMessage());
            }
        }
    }
}
