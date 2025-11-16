package com.eb.script.interpreter;

import com.eb.script.token.DataType;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.interpreter.statement.StatementKind;
import com.eb.script.interpreter.statement.ScreenStatement;
import com.eb.script.interpreter.statement.ScreenShowStatement;
import com.eb.script.interpreter.statement.ScreenHideStatement;
import com.eb.script.interpreter.DisplayItem.ItemType;
import com.eb.script.interpreter.AreaDefinition.AreaType;
import com.eb.script.interpreter.AreaDefinition.AreaItem;
import com.eb.script.RuntimeContext;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Visit a screen statement to create a new screen
     */
    public void visitScreenStatement(ScreenStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1", stmt.name);
        try {
            if (context.getScreens().containsKey(stmt.name) || context.getScreensBeingCreated().contains(stmt.name)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + stmt.name + "' already exists.");
            }

            // Mark this screen as being created to prevent duplicate creation during async initialization
            context.getScreensBeingCreated().add(stmt.name);

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

            // Create thread-safe variable storage for this screen
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVarMap = new java.util.concurrent.ConcurrentHashMap<>();
            context.getScreenVars().put(stmt.name, screenVarMap);

            // Create thread-safe variable type storage for this screen
            java.util.concurrent.ConcurrentHashMap<String, DataType> screenVarTypeMap = new java.util.concurrent.ConcurrentHashMap<>();
            context.getScreenVarTypes().put(stmt.name, screenVarTypeMap);

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
                                throw interpreter.error(stmt.getLine(), "Variable definition in screen '" + stmt.name + "' must have a 'name' property.");
                            }

                            // Convert type string to DataType
                            DataType varType = null;
                            if (varTypeStr != null) {
                                varType = parseDataType(varTypeStr);
                                if (varType == null) {
                                    throw interpreter.error(stmt.getLine(), "Unknown type '" + varTypeStr + "' for variable '" + varName + "' in screen '" + stmt.name + "'.");
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
                                    storeDisplayItem(varName, displayDef, stmt.name);
                                }
                            }

                            // Store in screen's thread-safe variable map
                            screenVarMap.put(varName, value);

                            // Store the variable type if specified
                            if (varType != null) {
                                screenVarTypeMap.put(varName, varType);
                            }
                        }
                    }
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

                    context.getScreenAreas().put(stmt.name, areas);
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
                    // Create a default VBox area with label-control pairs in HBox rows
                    // This gives: Label : Control on same line, with rows stacked vertically
                    AreaDefinition defaultArea = new AreaDefinition();
                    defaultArea.areaType = AreaDefinition.AreaType.VBOX;
                    defaultArea.name = "default";
                    defaultArea.screenName = stmt.name;

                    // Instead of creating nested areas, we'll create a grid-like structure
                    // by creating HBox sub-areas for each variable
                    java.util.List<AreaDefinition> areas = new java.util.ArrayList<>();

                    for (String varName : varsWithDisplay) {
                        // Create an HBox area to hold label and control side-by-side
                        AreaDefinition hboxRow = new AreaDefinition();
                        hboxRow.areaType = AreaDefinition.AreaType.HBOX;
                        hboxRow.name = varName + "_row";
                        hboxRow.screenName = stmt.name;

                        // Create a label for the variable name
                        AreaDefinition.AreaItem labelItem = new AreaDefinition.AreaItem();
                        labelItem.name = varName + "_label";
                        labelItem.sequence = 0;
                        DisplayItem labelDisplay = new DisplayItem();
                        labelDisplay.itemType = DisplayItem.ItemType.LABEL;

                        // Get the display metadata for this variable to use its labelText or promptText as the label
                        DisplayItem varDisplayItem = context.getDisplayItem().get(stmt.name + "." + varName);
                        String labelText;
                        if (varDisplayItem != null && varDisplayItem.labelText != null && !varDisplayItem.labelText.isEmpty()) {
                            // Prefer labelText if specified - don't add colon if it already has one
                            labelText = varDisplayItem.labelText.endsWith(":") ? varDisplayItem.labelText : varDisplayItem.labelText + ":";
                        } else {
                            // Final fallback to capitalizing the variable name if neither labelText nor promptText is available
                            labelText = capitalizeWords(varName) + ":";
                        }
                        if (!labelText.isBlank() && labelText.charAt(labelText.length() - 1) != ':') {
                            labelText = labelText + ":";
                        }
                        if (varDisplayItem != null && varDisplayItem.promptHelp != null && !varDisplayItem.promptHelp.isEmpty()) {
                            // Fall back to promptText if no labelText is available
                            labelDisplay.promptHelp = varDisplayItem.promptHelp;
                        }
                        labelDisplay.labelText = labelText;
                        labelDisplay.style = "-fx-alignment: center-right;"; // Right-align the label text
                        labelItem.displayItem = labelDisplay;
                        labelItem.varRef = null; // Labels don't bind to variables
                        labelItem.maxWidth = "USE_PREF_SIZE";
                        labelItem.hgrow = "NEVER";
                        labelItem.minWidth = "150"; // Minimum width for label alignment
                        hboxRow.items.add(labelItem);

                        // Create the input control for the variable
                        AreaDefinition.AreaItem item = new AreaDefinition.AreaItem();
                        item.varRef = varName;
                        item.sequence = 1;
                        item.displayItem = context.getDisplayItem().get(stmt.name + "." + varName);

                        // Set sizing to fit content, not stretch to screen width
                        item.maxWidth = "USE_PREF_SIZE";
                        item.hgrow = "NEVER";

                        // Set reasonable preferred widths based on item type
                        if (item.displayItem != null) {
                            switch (item.displayItem.itemType) {
                                case TEXTFIELD:
                                case PASSWORDFIELD:
                                    item.prefWidth = "300";
                                    break;
                                case TEXTAREA:
                                    item.prefWidth = "400";
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
                                default:
                                    // Let other controls use default sizing
                                    break;
                            }
                        }

                        hboxRow.items.add(item);
                        areas.add(hboxRow);
                    }

                    context.getScreenAreas().put(stmt.name, areas);
                }
            }

            // Create the screen on JavaFX Application Thread and set up thread management
            final String screenName = stmt.name;
            final String screenTitle = title;
            final int screenWidth = width;
            final int screenHeight = height;
            final boolean screenMaximize = maximize;

            // Get the areas for this screen
            final java.util.List<AreaDefinition> areas = context.getScreenAreas().get(screenName);

            // Use CountDownLatch to wait for stage creation
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<Exception> creationError = new java.util.concurrent.atomic.AtomicReference<>();

            Platform.runLater(() -> {
                try {
                    Stage stage;

                    // Get screen variables map from context
                    java.util.concurrent.ConcurrentHashMap<String, Object> varsMap = context.getScreenVars().get(screenName);
                    java.util.concurrent.ConcurrentHashMap<String, DataType> varTypesMap = context.getScreenVarTypes().get(screenName);

                    // Use ScreenFactory if areas are defined, otherwise create simple stage
                    if (areas != null && !areas.isEmpty()) {
                        // Create screen with areas using ScreenFactory
                        // Create onClick handler that executes EBS code
                        ScreenFactory.OnClickHandler onClickHandler = (ebsCode) -> {
                            try {
                                // Parse and execute the EBS code
                                RuntimeContext clickContext = com.eb.script.parser.Parser.parse("onClick_" + screenName, ebsCode);
                                // Execute in the current interpreter context
                                for (com.eb.script.interpreter.statement.Statement s : clickContext.statements) {
                                    interpreter.acceptStatement(s);
                                }
                            } catch (com.eb.script.parser.ParseError e) {
                                throw new InterpreterError("Failed to parse onClick code: " + e.getMessage());
                            } catch (java.io.IOException e) {
                                throw new InterpreterError("IO error executing onClick code: " + e.getMessage());
                            }
                        };

                        stage = ScreenFactory.createScreen(
                                screenName,
                                screenTitle,
                                screenWidth,
                                screenHeight,
                                areas,
                                (scrName, varName) -> context.getDisplayItem().get(scrName + "." + varName),
                                varsMap, // Pass screenVars for binding
                                varTypesMap, // Pass variable types for proper conversion
                                onClickHandler, // Pass onClick handler for buttons
                                context // Pass context to store bound controls for refresh
                        );
                    } else {
                        // Create simple stage without areas
                        stage = new Stage();
                        stage.setTitle(screenTitle);

                        // Create a simple scene with a StackPane
                        StackPane root = new StackPane();
                        Scene scene = new Scene(root, screenWidth, screenHeight);
                        stage.setScene(scene);
                    }

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
                    context.getScreenThreads().put(screenName, screenThread);

                    // Set up cleanup when screen is closed
                    stage.setOnCloseRequest(event -> {
                        // Interrupt and stop the screen thread
                        Thread thread = context.getScreenThreads().get(screenName);
                        if (thread != null && thread.isAlive()) {
                            thread.interrupt();
                        }
                        // Clean up resources
                        context.getScreens().remove(screenName);
                        context.getScreenThreads().remove(screenName);
                        context.getScreenVars().remove(screenName);
                        context.getScreenAreas().remove(screenName);

                        // Clean up display metadata for this screen
                        context.getDisplayItem().entrySet().removeIf(entry
                                -> entry.getKey().startsWith(screenName + "."));
                    });

                    // Store the stage reference
                    context.getScreens().put(screenName, stage);
                    // Remove from being created set
                    context.getScreensBeingCreated().remove(screenName);

                    // Don't show the screen automatically - user must explicitly call "screen <name> show;"
                    if (context.getOutput() != null) {
                        context.getOutput().printlnOk("Screen '" + screenName + "' created with title: " + screenTitle);
                    }
                } catch (Exception e) {
                    creationError.set(e);
                    // Remove from being created set on error
                    context.getScreensBeingCreated().remove(screenName);
                    if (context.getOutput() != null) {
                        context.getOutput().printlnError("Failed to create screen '" + screenName + "': " + e.getMessage());
                    }
                } finally {
                    // Signal that creation is complete
                    latch.countDown();
                }
            });

            // Wait for stage creation to complete
            try {
                boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    // Timeout occurred
                    context.getScreensBeingCreated().remove(stmt.name);
                    throw interpreter.error(stmt.getLine(), "Screen creation timed out after 10 seconds");
                }
            } catch (InterruptedException e) {
                context.getScreensBeingCreated().remove(stmt.name);
                Thread.currentThread().interrupt();
                throw interpreter.error(stmt.getLine(), "Screen creation was interrupted");
            }

            // Check if there was an error during creation
            if (creationError.get() != null) {
                throw interpreter.error(stmt.getLine(), "Failed to create screen: " + creationError.get().getMessage());
            }

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Visit a screen show statement to display a screen
     */
    public void visitScreenShowStatement(ScreenShowStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 show", stmt.name);
        try {
            // Check if screen exists (may be null if still being created, but key should be present)
            if (!context.getScreens().containsKey(stmt.name)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + stmt.name + "' does not exist. Create it first with 'screen " + stmt.name + " = {...};'");
            }

            Stage stage = context.getScreens().get(stmt.name);
            if (stage == null) {
                // This shouldn't happen with the latch, but handle it gracefully
                throw interpreter.error(stmt.getLine(), "Screen '" + stmt.name + "' is still being initialized. Please try again.");
            }

            // Show the screen on JavaFX Application Thread
            Platform.runLater(() -> {
                if (!stage.isShowing()) {
                    stage.show();
                    if (context.getOutput() != null) {
                        context.getOutput().printlnOk("Screen '" + stmt.name + "' shown");
                    }
                } else {
                    if (context.getOutput() != null) {
                        context.getOutput().printlnInfo("Screen '" + stmt.name + "' is already showing");
                    }
                }
            });

        } catch (InterpreterError ex) {
            throw interpreter.error(stmt.getLine(), ex.getLocalizedMessage());
        } finally {
            interpreter.environment().popCallStack();
        }
    }

    /**
     * Visit a screen hide statement to hide a screen
     */
    public void visitScreenHideStatement(ScreenHideStatement stmt) throws InterpreterError {
        interpreter.environment().pushCallStack(stmt.getLine(), StatementKind.STATEMENT, "Screen %1 hide", stmt.name);
        try {
            // Check if screen exists (may be null if still being created, but key should be present)
            if (!context.getScreens().containsKey(stmt.name)) {
                throw interpreter.error(stmt.getLine(), "Screen '" + stmt.name + "' does not exist. Create it first with 'screen " + stmt.name + " = {...};'");
            }

            Stage stage = context.getScreens().get(stmt.name);
            if (stage == null) {
                // This shouldn't happen with the latch, but handle it gracefully
                throw interpreter.error(stmt.getLine(), "Screen '" + stmt.name + "' is still being initialized. Please try again.");
            }

            // Hide the screen on JavaFX Application Thread
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.hide();
                    if (context.getOutput() != null) {
                        context.getOutput().printlnOk("Screen '" + stmt.name + "' hidden");
                    }
                } else {
                    if (context.getOutput() != null) {
                        context.getOutput().printlnInfo("Screen '" + stmt.name + "' is already hidden");
                    }
                }
            });

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
    private void storeDisplayItem(String varName, Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = parseDisplayItem(displayDef, screenName);

        // Store the metadata using a composite key: screenName.varName
        String key = screenName + "." + varName;
        context.getDisplayItem().put(key, metadata);
    }

    private DisplayItem parseDisplayItem(Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = new DisplayItem();

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

        // Check for both camelCase and lowercase versions - promptText (placeholder hint)
        if (displayDef.containsKey("prompttext")) {
            metadata.promptHelp = String.valueOf(displayDef.get("prompttext"));
        }

        // Extract labelText (permanent label displayed before/above control)
        if (displayDef.containsKey("labeltext")) {
            metadata.labelText = String.valueOf(displayDef.get("labeltext"));
        }

        // Extract labelText alignment
        if (displayDef.containsKey("labeltextalignment")) {
            metadata.labelTextAlignment = String.valueOf(displayDef.get("labeltextalignment")).toLowerCase();
        }

        // Extract onClick event handler for buttons - check both camelCase and lowercase
        if (displayDef.containsKey("onclick")) {
            metadata.onClick = String.valueOf(displayDef.get("onclick"));
        }

        // Extract options for selection controls (ComboBox, ChoiceBox, ListView)
        if (displayDef.containsKey("options")) {
            Object optionsObj = displayDef.get("options");
            metadata.options = new ArrayList<>();
            if (optionsObj instanceof ArrayDynamic) {
                ArrayDynamic array = (ArrayDynamic) optionsObj;
                for (Object item : array.getAll()) {
                    metadata.options.add(String.valueOf(item));
                }
            } else if (optionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) optionsObj;
                for (Object item : list) {
                    metadata.options.add(String.valueOf(item));
                }
            }
        }

        // Extract promptText styling properties - check both camelCase and lowercase
        if (displayDef.containsKey("promptColor")) {
            metadata.labelColor = String.valueOf(displayDef.get("promptColor"));
        } else if (displayDef.containsKey("promptcolor")) {
            metadata.labelColor = String.valueOf(displayDef.get("promptcolor"));
        }

        if (displayDef.containsKey("promptBold")) {
            Object boldObj = displayDef.get("promptBold");
            if (boldObj instanceof Boolean) {
                metadata.labelBold = (Boolean) boldObj;
            }
        } else if (displayDef.containsKey("promptbold")) {
            Object boldObj = displayDef.get("promptbold");
            if (boldObj instanceof Boolean) {
                metadata.labelBold = (Boolean) boldObj;
            }
        }

        if (displayDef.containsKey("promptItalic")) {
            Object italicObj = displayDef.get("promptItalic");
            if (italicObj instanceof Boolean) {
                metadata.labelItalic = (Boolean) italicObj;
            }
        } else if (displayDef.containsKey("promptitalic")) {
            Object italicObj = displayDef.get("promptitalic");
            if (italicObj instanceof Boolean) {
                metadata.labelItalic = (Boolean) italicObj;
            }
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

                        // Check for both camelCase and lowercase versions of varRef
                        if (itemDef.containsKey("varref")) {
                            item.varRef = String.valueOf(itemDef.get("varref")).toLowerCase();
                        } else if (itemDef.containsKey("var_ref")) {
                            item.varRef = String.valueOf(itemDef.get("var_ref")).toLowerCase();
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
                        }
                        // If displayItem is not set here, it will remain null
                        // and the consuming code should fall back to using varRef's DisplayItem

                        // Parse additional UI properties for the item
                        // promptText now goes into displayItem
                        if (itemDef.containsKey("prompttext") || itemDef.containsKey("prompt_text")) {
                            String promptText = itemDef.containsKey("prompttext")
                                    ? String.valueOf(itemDef.get("prompttext"))
                                    : String.valueOf(itemDef.get("prompt_text"));

                            // If displayItem doesn't exist yet, create it
                            if (item.displayItem == null) {
                                item.displayItem = new DisplayItem();
                            }
                            item.displayItem.promptHelp = promptText;
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

        // Process nested child areas (areas within areas)
        if (areaDef.containsKey("areas")) {
            Object areasObj = areaDef.get("areas");
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
}
