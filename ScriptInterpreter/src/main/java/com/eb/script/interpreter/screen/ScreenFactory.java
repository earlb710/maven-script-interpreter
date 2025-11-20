package com.eb.script.interpreter.screen;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.file.BuiltinsFile;
import com.eb.script.file.FileData;
import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.screen.AreaDefinition.AreaType;
import com.eb.script.json.Json;
import com.eb.script.json.JsonSchema;
import com.eb.script.json.JsonValidate;
import com.eb.script.token.DataType;
import com.eb.util.Util;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * Factory class for creating complete JavaFX screens/windows from
 * AreaDefinitions. This factory uses AreaContainerFactory and AreaItemFactory
 * to create a fully assembled screen. Includes JSON Schema validation for
 * screen definitions.
 */
public class ScreenFactory {

    /**
     * Functional interface for executing onClick EBS code
     */
    @FunctionalInterface
    public interface OnClickHandler {

        void execute(String ebsCode) throws InterpreterError;
    }

    private static Map<String, Object> screenSchema;
    private static Map<String, Object> areaSchema;
    private static Map<String, Object> displayMetadataSchema;

    static {
        try {
            // Load schemas from resources
            FileData screenSchemaFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/screen-definition.json"));
            FileData areaSchemaFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/area-definition.json"));
            FileData displayMetadataFile = BuiltinsFile.readTextFile(ScreenFactory.class.getResourceAsStream("/json/display-metadata.json"));

            if (screenSchemaFile != null) {
                screenSchema = (Map<String, Object>) Json.parse(screenSchemaFile.stringData);
                JsonValidate.registerSchema("sys.screenSchema", screenSchema);
            }
            if (areaSchemaFile != null) {
                areaSchema = (Map<String, Object>) Json.parse(areaSchemaFile.stringData);
                JsonValidate.registerSchema("sys.areaSchema", areaSchema);
            }
            if (displayMetadataFile != null) {
                displayMetadataSchema = (Map<String, Object>) Json.parse(displayMetadataFile.stringData);
                JsonValidate.registerSchema("sys.displayMetadataSchema", displayMetadataSchema);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load JSON schemas: " + e.getMessage());
        }
    }

    /**
     * Creates a ScreenDefinition with basic parameters.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context) {
        return createScreenDefinition(screenName, title, width, height, areas, null, null, null, context);
    }
    
    /**
     * Creates a ScreenDefinition with variable binding support.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinition containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for two-way binding
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            InterpreterContext context) {
        return createScreenDefinition(screenName, title, width, height, areas, screenVars, null, null, context);
    }
    
    /**
     * Creates a ScreenDefinition with variable binding and onClick handlers.
     * 
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for two-way binding
     * @param varTypes The ConcurrentHashMap containing screen variable types for proper type conversion
     * @param onClickHandler Handler for button onClick events
     * @param context InterpreterContext for accessing display metadata
     * @return A ScreenDefinition that can be used to create Stage instances
     */
    public static ScreenDefinition createScreenDefinition(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            InterpreterContext context) {
        ScreenDefinition definition = new ScreenDefinition(screenName, title, width, height);
        definition.setAreas(areas);
        definition.setScreenVars(screenVars);
        definition.setVarTypes(varTypes);
        definition.setOnClickHandler(onClickHandler);
        definition.setContext(context);
        return definition;
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions. This
     * method creates containers, adds items, and applies layout properties.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context) {
        return createScreen(screenName, title, width, height, areas, null, null, null, context);
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions with
     * variable binding. This method creates containers, adds items, applies
     * layout properties, and sets up two-way data binding.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for
     * two-way binding (can be null)
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            InterpreterContext context) {
        return createScreen(screenName, title, width, height, areas, screenVars, null, null, context);
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions with
     * variable binding and onClick handlers. This method creates containers,
     * adds items, applies layout properties, sets up two-way data binding, and
     * configures button onClick handlers.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param screenVars The ConcurrentHashMap containing screen variables for
     * two-way binding (can be null)
     * @param varTypes The ConcurrentHashMap containing screen variable types
     * for proper type conversion (can be null)
     * @param onClickHandler Handler for button onClick events (can be null)
     * @param context InterpreterContext for accessing display metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            InterpreterContext context) {
        Stage stage = new Stage();
        stage.setTitle(title);

        // List to collect all bound controls for this screen
        List<Node> allBoundControls = new ArrayList<>();

        // Create root container - use VBox as default if multiple areas
        Region rootContainer;

        if (areas == null || areas.isEmpty()) {
            // No areas defined, create empty pane
            rootContainer = new Pane();
        } else if (areas.size() == 1) {
            // Single area - use it as root
            rootContainer = createAreaWithItems(areas.get(0), screenName, context, screenVars, varTypes, onClickHandler, allBoundControls);
        } else {
            // Multiple areas - arrange in VBox
            VBox root = new VBox();
            root.setSpacing(10);

            for (AreaDefinition areaDef : areas) {
                Region areaContainer = createAreaWithItems(areaDef, screenName, context, screenVars, varTypes, onClickHandler, allBoundControls);
                root.getChildren().add(areaContainer);
            }

            rootContainer = root;
        }

        // Store bound controls in context if provided
        if (context != null && !allBoundControls.isEmpty()) {
            context.getScreenBoundControls().put(screenName, allBoundControls);

            // Register refresh callback that refreshes all bound controls
            context.getScreenRefreshCallbacks().put(screenName, () -> {
                // Use Platform.runLater to ensure UI updates happen on JavaFX Application Thread
                Platform.runLater(() -> {
                    refreshBoundControls(allBoundControls, screenVars);
                });
            });
        }

        // Wrap content in ScrollPane to handle overflow when content is larger than window
        ScrollPane scrollPane = new ScrollPane(rootContainer);
        scrollPane.setFitToWidth(true);  // Make content fit to scroll pane width
        scrollPane.setFitToHeight(false); // Allow vertical scrolling when needed
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Create status bar for the screen
        com.eb.ui.ebs.StatusBar statusBar = new com.eb.ui.ebs.StatusBar();
        
        // Add focus listeners to all bound controls to update status bar
        setupStatusBarUpdates(allBoundControls, statusBar, context, screenName);
        
        // Create menu bar for the screen
        javafx.scene.control.MenuBar menuBar = createScreenMenuBar(stage);
        
        // Wrap in BorderPane to add menu bar at top and status bar at bottom
        BorderPane screenRoot = new BorderPane();
        screenRoot.setTop(menuBar);
        screenRoot.setCenter(scrollPane);
        screenRoot.setBottom(statusBar);
        
        // Store status bar in context for later access
        if (context != null) {
            context.getScreenStatusBars().put(screenName, statusBar);
        }

        Scene scene = new Scene(screenRoot, width, height);
        
        // Load CSS stylesheets for screen areas and input controls
        try {
            scene.getStylesheets().add(ScreenFactory.class.getResource("/css/screen-areas.css").toExternalForm());
            scene.getStylesheets().add(ScreenFactory.class.getResource("/css/screen-inputs.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Warning: Could not load screen CSS stylesheets: " + e.getMessage());
        }
        
        stage.setScene(scene);

        return stage;
    }
    
    /**
     * Setup focus listeners on all controls to update the status bar
     * with item tooltip and min/max information
     */
    private static void setupStatusBarUpdates(List<Node> controls, 
            com.eb.ui.ebs.StatusBar statusBar,
            InterpreterContext context,
            String screenName) {
        for (Node control : controls) {
            control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    // Get the item's user data which contains "screenName.itemName"
                    Object userData = control.getUserData();
                    if (userData != null && userData instanceof String) {
                        String fullRef = (String) userData;
                        // Extract item name from "screenName.itemName"
                        String itemName = fullRef.substring(screenName.length() + 1);
                        
                        // Get metadata for this item from context
                        DisplayItem metadata = context != null ? context.getDisplayItem().get(screenName + "." + itemName) : null;
                        // Update message with tooltip (prefer tooltip over promptHelp)
                        String tooltip = (String) control.getProperties().get("itemTooltip");
                        String message = tooltip != null ? tooltip : "";
                        statusBar.setMessage(message);
                        
                        // Get metadata for min/max info
                        if (metadata != null) {
                            // Update custom with min/max info
                            String minMaxInfo = "";
                            if (metadata.min != null && metadata.max != null) {
                                minMaxInfo = String.format("min:%s max:%s", metadata.min, metadata.max);
                            } else if (metadata.min != null) {
                                minMaxInfo = "min:" + metadata.min;
                            } else if (metadata.max != null) {
                                minMaxInfo = "max:" + metadata.max;
                            } else if (metadata.maxLength != null) {
                                minMaxInfo = "len:" + metadata.maxLength;
                            }
                            statusBar.setCustom(minMaxInfo);
                        }
                    }
                } else {
                    // Lost focus - clear status bar
                    statusBar.clearMessage();
                    statusBar.clearCustom();
                }
            });
        }
    }
    
    /**
     * Create a menu bar for screen windows with Edit menu containing Close item
     */
    private static javafx.scene.control.MenuBar createScreenMenuBar(Stage stage) {
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        
        // Create Edit menu
        javafx.scene.control.Menu editMenu = new javafx.scene.control.Menu("Edit");
        
        // Close menu item with Ctrl+W
        javafx.scene.control.MenuItem closeItem = new javafx.scene.control.MenuItem("Close");
        closeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
            javafx.scene.input.KeyCode.W, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN));
        closeItem.setOnAction(e -> {
            // Close the screen window
            stage.close();
        });
        
        editMenu.getItems().add(closeItem);
        menuBar.getMenus().add(editMenu);
        
        return menuBar;
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context) {
        return createAreaWithItems(areaDef, screenName, context, null, null, null);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        return createAreaWithItems(areaDef, screenName, context, screenVars, null, null);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding and onClick handler.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler) {
        List<Node> boundControls = new ArrayList<>();
        return createAreaWithItems(areaDef, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with
     * variable binding, onClick handler, and control tracking.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
            InterpreterContext context,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            OnClickHandler onClickHandler,
            List<Node> boundControls) {
        // Create the container using AreaContainerFactory
        Region container = AreaContainerFactory.createContainer(areaDef);

        // Sort items by sequence
        if (areaDef.items != null && !areaDef.items.isEmpty()) {
            List<AreaItem> sortedItems = areaDef.items.stream()
                    .sorted(Comparator.comparingInt(item -> item.sequence))
                    .toList();

            // First pass: Calculate maximum label width for alignment
            double maxLabelWidth = calculateMaxLabelWidth(sortedItems, screenName, context);

            // Add items to container based on container type
            for (AreaItem item : sortedItems) {
                // Get metadata for the item
                // Start with var-level metadata (from vars section), then merge item-level metadata (from area items display)
                DisplayItem metadata = null;
                if (item.varRef != null && context != null) {
                    metadata = context.getDisplayItem().get(screenName + "." + item.varRef);
                }
                // If item has its own display metadata, merge it (item-level overwrites var-level)
                if (item.displayItem != null) {
                    metadata = mergeDisplayMetadata(metadata, item.displayItem);
                }

                // Create the item using AreaItemFactory
                Node control = AreaItemFactory.createItem(item, metadata);
                
                // Store item metadata in control's user data for later retrieval by screen.setProperty/getProperty
                // Format: "screenName.itemName"
                if (item.name != null && !item.name.isEmpty()) {
                    control.setUserData(screenName + "." + item.name);
                }
                
                // Store tooltip in control's properties for status bar display
                if (item.tooltip != null && !item.tooltip.isEmpty()) {
                    control.getProperties().put("itemTooltip", item.tooltip);
                }

                // If labelText is specified, wrap the control with a label
                // BUT: Don't wrap Label or Button controls - they display their own text
                Node nodeToAdd = control;
                if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
                    // Only wrap input controls, not Label or Button which display their own text
                    if (!(control instanceof javafx.scene.control.Label)
                            && !(control instanceof javafx.scene.control.Button)) {
                        nodeToAdd = createLabeledControl(metadata.labelText, metadata.labelTextAlignment, control, maxLabelWidth, metadata);
                    }
                } else {
                    // No label specified - wrap control in HBox with left padding to align with labeled controls
                    // This ensures controls without labels still align properly with labeled controls
                    if (!(control instanceof javafx.scene.control.Label)
                            && !(control instanceof javafx.scene.control.Button)) {
                        javafx.scene.layout.HBox alignmentBox = new javafx.scene.layout.HBox();
                        alignmentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        // Add left padding equal to label width plus spacing to align with labeled controls
                        alignmentBox.setPadding(new javafx.geometry.Insets(0, 0, 0, maxLabelWidth + 5));
                        // Make container pick on bounds so tooltips on child controls work properly
                        alignmentBox.setPickOnBounds(false);
                        alignmentBox.getChildren().add(control);
                        nodeToAdd = alignmentBox;
                    }
                }

                // Set up onClick handler for buttons
                if (onClickHandler != null && metadata != null && metadata.onClick != null && !metadata.onClick.isEmpty()) {
                    if (control instanceof javafx.scene.control.Button) {
                        javafx.scene.control.Button button = (javafx.scene.control.Button) control;
                        String ebsCode = metadata.onClick;
                        button.setOnAction(event -> {
                            try {
                                onClickHandler.execute(ebsCode);
                                // After executing the onClick code, refresh all bound controls
                                refreshBoundControls(boundControls, screenVars);
                            } catch (InterpreterError e) {
                                // Print error to console if available
                                System.err.println("Error executing button onClick: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    }
                }

                // Set up two-way data binding if screenVars is provided and item has a varRef
                if (screenVars != null && item.varRef != null) {
                    setupVariableBinding(control, item.varRef, screenVars, varTypes, metadata);
                    // Track this control so we can refresh it when variables change
                    boundControls.add(control);
                }

                // Apply item layout properties
                applyItemLayoutProperties(control, item);

                // Add item to container based on container type
                addItemToContainer(container, nodeToAdd, item, areaDef.areaType);
            }
        }

        // Add nested child areas to the container
        if (areaDef.childAreas != null && !areaDef.childAreas.isEmpty()) {
            for (AreaDefinition childArea : areaDef.childAreas) {
                // Special handling for Tab areas when parent is TabPane
                if (areaDef.areaType == AreaType.TABPANE && childArea.areaType == AreaType.TAB) {
                    // Tab should contain its child areas directly, not wrapped in an extra container
                    // Process the Tab's child areas to get the actual content
                    Region tabContent;
                    
                    if (childArea.childAreas != null && !childArea.childAreas.isEmpty()) {
                        // If Tab has multiple child areas, create a VBox to hold them
                        if (childArea.childAreas.size() == 1) {
                            // Single child area - use it directly
                            tabContent = createAreaWithItems(childArea.childAreas.get(0), screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                        } else {
                            // Multiple child areas - wrap in VBox
                            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
                            for (AreaDefinition tabChildArea : childArea.childAreas) {
                                Region childContent = createAreaWithItems(tabChildArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                                vbox.getChildren().add(childContent);
                            }
                            tabContent = vbox;
                        }
                    } else if (childArea.items != null && !childArea.items.isEmpty()) {
                        // Tab has items directly (no child areas)
                        tabContent = createAreaWithItems(childArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                    } else {
                        // Empty tab - create empty pane
                        tabContent = new javafx.scene.layout.Pane();
                    }
                    
                    // Ensure tab content has transparent background
                    if (tabContent.getStyle() == null || tabContent.getStyle().isEmpty()) {
                        tabContent.setStyle("-fx-background-color: transparent;");
                    } else if (!tabContent.getStyle().contains("-fx-background-color")) {
                        tabContent.setStyle(tabContent.getStyle() + " -fx-background-color: transparent;");
                    }
                    
                    // Wrap tab content in ScrollPane for automatic scrollbars when content is larger than tab
                    ScrollPane scrollPane = new ScrollPane(tabContent);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setFitToHeight(false); // Allow vertical scrolling when content exceeds viewport
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    
                    Tab tab = new Tab();
                    tab.setText(childArea.displayName != null ? childArea.displayName : childArea.name);
                    tab.setContent(scrollPane);
                    tab.setClosable(false); // Tabs not closable by default
                    
                    if (container instanceof TabPane) {
                        ((TabPane) container).getTabs().add(tab);
                    }
                } else {
                    // Normal nested area handling
                    Region childContainer = createAreaWithItems(childArea, screenName, context, screenVars, varTypes, onClickHandler, boundControls);
                    
                    // Add the child container to the parent container
                    // Treat child areas as regular nodes
                    addChildAreaToContainer(container, childContainer, areaDef.areaType, childArea);
                }
            }
        }

        return container;
    }

    /**
     * Applies layout properties from AreaItem to the control. These are the
     * properties NOT applied by AreaItemFactory (which only applies display
     * properties).
     */
    private static void applyItemLayoutProperties(Node control, AreaItem item) {
        // Apply sizing properties
        if (control instanceof Region) {
            Region region = (Region) control;

            if (item.prefWidth != null && !item.prefWidth.isEmpty()) {
                try {
                    double width = parseSize(item.prefWidth);
                    if (width > 0) {
                        region.setPrefWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.prefHeight != null && !item.prefHeight.isEmpty()) {
                try {
                    double height = parseSize(item.prefHeight);
                    if (height > 0) {
                        region.setPrefHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.minWidth != null && !item.minWidth.isEmpty()) {
                try {
                    double width = parseSize(item.minWidth);
                    if (width > 0) {
                        region.setMinWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.minHeight != null && !item.minHeight.isEmpty()) {
                try {
                    double height = parseSize(item.minHeight);
                    if (height > 0) {
                        region.setMinHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.maxWidth != null && !item.maxWidth.isEmpty()) {
                try {
                    double width = parseSize(item.maxWidth);
                    if (width > 0) {
                        region.setMaxWidth(width);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }

            if (item.maxHeight != null && !item.maxHeight.isEmpty()) {
                try {
                    double height = parseSize(item.maxHeight);
                    if (height > 0) {
                        region.setMaxHeight(height);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
        }

        // Apply margin
        if (item.margin != null && !item.margin.isEmpty()) {
            Insets margin = parseInsets(item.margin);
            if (margin != null) {
                VBox.setMargin(control, margin);
                HBox.setMargin(control, margin);
                BorderPane.setMargin(control, margin);
                GridPane.setMargin(control, margin);
                StackPane.setMargin(control, margin);
                FlowPane.setMargin(control, margin);
            }
        }
    }

    /**
     * Adds an item to a container based on the container type.
     */
    private static void addItemToContainer(Region container, Node control, AreaItem item, AreaType areaType) {
        if (container instanceof VBox) {
            VBox vbox = (VBox) container;
            vbox.getChildren().add(control);

            // Apply VBox-specific properties
            if (item.vgrow != null && !item.vgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                    VBox.setVgrow(control, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

        } else if (container instanceof HBox) {
            HBox hbox = (HBox) container;
            hbox.getChildren().add(control);

            // Apply HBox-specific properties
            if (item.hgrow != null && !item.hgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(item.hgrow.toUpperCase());
                    HBox.setHgrow(control, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

        } else if (container instanceof GridPane) {
            GridPane gridPane = (GridPane) container;

            // Parse layoutPos for row, col
            int row = 0;
            int col = 0;
            if (item.layoutPos != null && !item.layoutPos.isEmpty()) {
                String[] parts = item.layoutPos.split(",");
                if (parts.length >= 2) {
                    try {
                        row = Integer.parseInt(parts[0].trim());
                        col = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        // Use default 0,0
                    }
                }
            }

            gridPane.add(control, col, row);

            // Apply column and row span
            if (item.colSpan != null && item.colSpan > 1) {
                GridPane.setColumnSpan(control, item.colSpan);
            }
            if (item.rowSpan != null && item.rowSpan > 1) {
                GridPane.setRowSpan(control, item.rowSpan);
            }

            // Apply grid grow priorities
            if (item.hgrow != null && !item.hgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(item.hgrow.toUpperCase());
                    GridPane.setHgrow(control, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }
            if (item.vgrow != null && !item.vgrow.isEmpty()) {
                try {
                    Priority priority = Priority.valueOf(item.vgrow.toUpperCase());
                    GridPane.setVgrow(control, priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

        } else if (container instanceof BorderPane) {
            BorderPane borderPane = (BorderPane) container;

            // Parse layoutPos for position (top, bottom, left, right, center)
            String position = item.layoutPos != null ? item.layoutPos.toLowerCase() : "center";

            switch (position) {
                case "top":
                    borderPane.setTop(control);
                    break;
                case "bottom":
                    borderPane.setBottom(control);
                    break;
                case "left":
                    borderPane.setLeft(control);
                    break;
                case "right":
                    borderPane.setRight(control);
                    break;
                case "center":
                default:
                    borderPane.setCenter(control);
                    break;
            }

        } else if (container instanceof StackPane) {
            StackPane stackPane = (StackPane) container;
            stackPane.getChildren().add(control);

            // Apply alignment if specified
            if (item.alignment != null && !item.alignment.isEmpty()) {
                try {
                    Pos pos = parseAlignment(item.alignment);
                    StackPane.setAlignment(control, pos);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid values
                }
            }

        } else if (container instanceof FlowPane) {
            FlowPane flowPane = (FlowPane) container;
            flowPane.getChildren().add(control);

        } else if (container instanceof TilePane) {
            TilePane tilePane = (TilePane) container;
            tilePane.getChildren().add(control);

        } else if (container instanceof AnchorPane) {
            AnchorPane anchorPane = (AnchorPane) container;
            anchorPane.getChildren().add(control);

            // Parse layoutPos for anchor constraints (e.g., "10,20,30,40" for top,right,bottom,left)
            if (item.layoutPos != null && !item.layoutPos.isEmpty()) {
                String[] parts = item.layoutPos.split(",");
                if (parts.length >= 4) {
                    try {
                        double top = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        double bottom = Double.parseDouble(parts[2].trim());
                        double left = Double.parseDouble(parts[3].trim());

                        if (top >= 0) {
                            AnchorPane.setTopAnchor(control, top);
                        }
                        if (right >= 0) {
                            AnchorPane.setRightAnchor(control, right);
                        }
                        if (bottom >= 0) {
                            AnchorPane.setBottomAnchor(control, bottom);
                        }
                        if (left >= 0) {
                            AnchorPane.setLeftAnchor(control, left);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }

        } else if (container instanceof Pane) {
            // Generic Pane - just add the control
            ((Pane) container).getChildren().add(control);
        }
    }

    /**
     * Adds a child area (nested container) to a parent container. Similar to
     * addItemToContainer but for child areas.
     */
    private static void addChildAreaToContainer(Region container, Region childArea, AreaType areaType, AreaDefinition childAreaDef) {
        if (container instanceof VBox) {
            ((VBox) container).getChildren().add(childArea);
        } else if (container instanceof HBox) {
            ((HBox) container).getChildren().add(childArea);
        } else if (container instanceof GridPane) {
            // For GridPane, just add to next available position
            ((GridPane) container).getChildren().add(childArea);
        } else if (container instanceof BorderPane) {
            // For BorderPane, default to center if not specified
            BorderPane borderPane = (BorderPane) container;
            if (borderPane.getCenter() == null) {
                borderPane.setCenter(childArea);
            }
        } else if (container instanceof StackPane) {
            ((StackPane) container).getChildren().add(childArea);
        } else if (container instanceof FlowPane) {
            ((FlowPane) container).getChildren().add(childArea);
        } else if (container instanceof TilePane) {
            ((TilePane) container).getChildren().add(childArea);
        } else if (container instanceof AnchorPane) {
            ((AnchorPane) container).getChildren().add(childArea);
        } else if (container instanceof Pane) {
            ((Pane) container).getChildren().add(childArea);
        }
    }

    /**
     * Parse size string to double value. Supports plain numbers and percentages
     * (e.g., "300", "50%").
     */
    private static double parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return -1;
        }

        size = size.trim();

        if (size.endsWith("%")) {
            // Percentage - not directly supported, return -1
            return -1;
        } else if (size.equalsIgnoreCase("auto")) {
            return -1;
        } else {
            return Double.parseDouble(size);
        }
    }

    /**
     * Parse insets string to Insets object. Supports formats: "10" (all), "10
     * 5" (vertical horizontal), "10 5 10 5" (top right bottom left).
     */
    private static Insets parseInsets(String insetsStr) {
        if (insetsStr == null || insetsStr.isEmpty()) {
            return null;
        }

        String[] parts = insetsStr.trim().split("\\s+");

        try {
            if (parts.length == 1) {
                double all = Double.parseDouble(parts[0]);
                return new Insets(all);
            } else if (parts.length == 2) {
                double vertical = Double.parseDouble(parts[0]);
                double horizontal = Double.parseDouble(parts[1]);
                return new Insets(vertical, horizontal, vertical, horizontal);
            } else if (parts.length == 4) {
                double top = Double.parseDouble(parts[0]);
                double right = Double.parseDouble(parts[1]);
                double bottom = Double.parseDouble(parts[2]);
                double left = Double.parseDouble(parts[3]);
                return new Insets(top, right, bottom, left);
            }
        } catch (NumberFormatException e) {
            // Return null for invalid format
        }

        return null;
    }

    /**
     * Parse alignment string to JavaFX Pos enum.
     */
    private static Pos parseAlignment(String alignment) {
        if (alignment == null || alignment.isEmpty()) {
            return Pos.CENTER;
        }

        String normalized = alignment.toUpperCase().replace("-", "_").replace(" ", "_");

        try {
            return Pos.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try common variations
            switch (normalized) {
                case "LEFT":
                    return Pos.CENTER_LEFT;
                case "RIGHT":
                    return Pos.CENTER_RIGHT;
                case "TOP":
                    return Pos.TOP_CENTER;
                case "BOTTOM":
                    return Pos.BOTTOM_CENTER;
                case "CENTER":
                default:
                    return Pos.CENTER;
            }
        }
    }

    /**
     * Convenience method to create and show a screen on the JavaFX Application
     * Thread.
     */
    public static void createAndShowScreen(String screenName, String title, double width, double height,
            List<AreaDefinition> areas,
            InterpreterContext context,
            boolean maximize) {
        Platform.runLater(() -> {
            Stage stage = createScreen(screenName, title, width, height, areas, context);

            if (maximize) {
                stage.setMaximized(true);
            }

            stage.show();
        });
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen
     * definition (JSON/EBS format). This method parses the screen definition
     * and creates the window. Validates the screen definition against the JSON
     * schema if validation is enabled.
     *
     * @param screenDef Map containing screen definition with keys: name, title,
     * width, height, vars, area
     * @return A Stage representing the complete window
     * @throws IllegalArgumentException if the screen definition is invalid
     */
    public static Stage createScreenFromDefinition(Map<String, Object> screenDef) {
        return createScreenFromDefinition(screenDef, true);
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen
     * definition (JSON/EBS format). This method parses the screen definition
     * and creates the window.
     *
     * @param screenDef Map containing screen definition with keys: name, title,
     * width, height, vars, area
     * @param validate Whether to validate against JSON schema
     * @return A Stage representing the complete window
     * @throws IllegalArgumentException if the screen definition is invalid
     */
    public static Stage createScreenFromDefinition(Map<String, Object> screenDef, boolean validate) {
        // Validate screen definition if requested and schema is available
        if (validate && screenSchema != null) {
            validateScreenDefinition(screenDef);
        }

        // Extract screen properties
        String screenName = getStringValue(screenDef, "name", "Screen");
        String title = getStringValue(screenDef, "title", screenName);
        double width = getNumberValue(screenDef, "width", 800.0);
        double height = getNumberValue(screenDef, "height", 600.0);

        // Parse variables and build metadata map
        Map<String, DisplayItem> metadataMap = new HashMap<>();
        if (screenDef.containsKey("vars")) {
            Object varsObj = screenDef.get("vars");
            if (varsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> varsList = (List<Object>) varsObj;
                for (Object varObj : varsList) {
                    if (varObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> varDef = (Map<String, Object>) varObj;
                        String varName = getStringValue(varDef, "name", null);
                        if (varName != null && varDef.containsKey("display")) {
                            Object displayObj = varDef.get("display");
                            if (displayObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> displayDef = (Map<String, Object>) displayObj;
                                DisplayItem metadata = parseDisplayItem(displayDef, screenName);
                                metadataMap.put(varName, metadata);
                            }
                        }
                    }
                }
            }
        }

        // Parse areas
        List<AreaDefinition> areas = new ArrayList<>();
        if (screenDef.containsKey("area")) {
            Object areaObj = screenDef.get("area");
            if (areaObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> areaList = (List<Object>) areaObj;
                for (Object areaDef : areaList) {
                    if (areaDef instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> areaDefMap = (Map<String, Object>) areaDef;
                        AreaDefinition area = parseAreaDefinition(areaDefMap, screenName);
                        areas.add(area);
                    }
                }
            }
        }

        // Create a temporary context and populate it with metadata
        InterpreterContext tempContext = new InterpreterContext();
        for (Map.Entry<String, DisplayItem> entry : metadataMap.entrySet()) {
            tempContext.getDisplayItem().put(screenName + "." + entry.getKey(), entry.getValue());
        }

        return createScreen(screenName, title, width, height, areas, tempContext);
    }

    /**
     * Parses an AreaDefinition from a Map.
     */
    private static AreaDefinition parseAreaDefinition(Map<String, Object> areaDef, String screenName) {
        AreaDefinition area = new AreaDefinition();

        // Extract area name (required)
        area.name = getStringValue(areaDef, "name", "area");

        // Extract area type and convert to enum
        String typeStr = getStringValue(areaDef, "type", "pane");
        area.type = typeStr.toLowerCase();
        area.areaType = AreaType.fromString(area.type);

        // Set CSS class from enum
        area.cssClass = area.areaType.getCssClass();

        // Extract layout configuration
        area.layout = getStringValue(areaDef, "layout", null);

        // Extract or set default style
        area.style = getStringValue(areaDef, "style", area.areaType.getDefaultStyle());

        area.screenName = screenName;
        
        // Extract displayName for UI labels (e.g., tab labels)
        area.displayName = getStringValue(areaDef, "displayName", null);
        
        // Extract spacing between children (for containers that support it)
        area.spacing = getStringValue(areaDef, "spacing", null);
        
        // Extract padding inside the area
        area.padding = getStringValue(areaDef, "padding", null);

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
                        AreaItem item = parseAreaItem(itemDef, screenName);
                        area.items.add(item);
                    }
                }
            }
        }

        // Process nested child areas (areas within areas)
        if (areaDef.containsKey("areas")) {
            Object areasObj = areaDef.get("areas");
            if (areasObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> areasList = (List<Object>) areasObj;

                for (Object childAreaObj : areasList) {
                    if (childAreaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> childAreaDef = (Map<String, Object>) childAreaObj;
                        AreaDefinition childArea = parseAreaDefinition(childAreaDef, screenName);
                        area.childAreas.add(childArea);
                    }
                }
            }
        }

        return area;
    }

    /**
     * Parses an AreaItem from a Map.
     */
    private static AreaItem parseAreaItem(Map<String, Object> itemDef, String screenName) {
        AreaItem item = new AreaItem();

        // Extract item properties
        item.name = getStringValue(itemDef, "name", null);
        // Support both "sequence" and "seq" for compactness
        item.sequence = getIntValue(itemDef, "sequence", getIntValue(itemDef, "seq", 0));
        item.varRef = getStringValue(itemDef, "varRef", getStringValue(itemDef, "var_ref", null));

        // Layout position (support multiple naming conventions)
        item.layoutPos = getStringValue(itemDef, "layoutPos",
                getStringValue(itemDef, "layout_pos",
                        getStringValue(itemDef, "relativePos",
                                getStringValue(itemDef, "relative_pos", null))));

        // Process optional display metadata for the item
        if (itemDef.containsKey("display")) {
            Object displayObj = itemDef.get("display");
            if (displayObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> displayDef = (Map<String, Object>) displayObj;
                item.displayItem = parseDisplayItem(displayDef, screenName);
            }
        }

        // UI properties
        // promptHelp (formerly promptText) now goes into displayItem
        String promptHelp = getStringValue(itemDef, "promptHelp", getStringValue(itemDef, "prompt_help", null));
        if (promptHelp != null) {
            // If displayItem doesn't exist yet, create it
            if (item.displayItem == null) {
                item.displayItem = new DisplayItem();
            }
            item.displayItem.promptHelp = promptHelp;
        }
        item.tooltip = getStringValue(itemDef, "tooltip", null);
        item.editable = getBooleanValue(itemDef, "editable", null);
        item.disabled = getBooleanValue(itemDef, "disabled", null);
        item.visible = getBooleanValue(itemDef, "visible", null);
        item.textColor = getStringValue(itemDef, "textColor", getStringValue(itemDef, "text_color", null));
        item.backgroundColor = getStringValue(itemDef, "backgroundColor", getStringValue(itemDef, "background_color", null));

        // Layout properties
        item.colSpan = getIntValue(itemDef, "colSpan", getIntValue(itemDef, "col_span", null));
        item.rowSpan = getIntValue(itemDef, "rowSpan", getIntValue(itemDef, "row_span", null));
        item.hgrow = getStringValue(itemDef, "hgrow", null);
        item.vgrow = getStringValue(itemDef, "vgrow", null);
        item.margin = getStringValue(itemDef, "margin", null);
        item.padding = getStringValue(itemDef, "padding", null);
        item.prefWidth = getStringValue(itemDef, "prefWidth", getStringValue(itemDef, "pref_width", null));
        item.prefHeight = getStringValue(itemDef, "prefHeight", getStringValue(itemDef, "pref_height", null));
        item.minWidth = getStringValue(itemDef, "minWidth", getStringValue(itemDef, "min_width", null));
        item.minHeight = getStringValue(itemDef, "minHeight", getStringValue(itemDef, "min_height", null));
        item.maxWidth = getStringValue(itemDef, "maxWidth", getStringValue(itemDef, "max_width", null));
        item.maxHeight = getStringValue(itemDef, "maxHeight", getStringValue(itemDef, "max_height", null));
        item.alignment = getStringValue(itemDef, "alignment", null);

        return item;
    }

    /**
     * Parses DisplayItem from a Map.
     */
    private static DisplayItem parseDisplayItem(Map<String, Object> displayDef, String screenName) {
        DisplayItem metadata = new DisplayItem();

        // Extract display type and convert to enum
        String typeStr = getStringValue(displayDef, "type", "textfield");
        metadata.type = typeStr.toLowerCase();
        metadata.itemType = DisplayItem.ItemType.fromString(metadata.type);

        // Set CSS class from enum
        metadata.cssClass = metadata.itemType.getCssClass();

        metadata.mandatory = getBooleanValue(displayDef, "mandatory", false);
        metadata.caseFormat = getStringValue(displayDef, "case", null);
        metadata.alignment = getStringValue(displayDef, "alignment", null);
        metadata.pattern = getStringValue(displayDef, "pattern", null);

        // Min and max can be various types
        if (displayDef.containsKey("min")) {
            metadata.min = displayDef.get("min");
        }
        if (displayDef.containsKey("max")) {
            metadata.max = displayDef.get("max");
        }

        // Extract or set default style
        metadata.style = getStringValue(displayDef, "style", metadata.itemType.getDefaultStyle());
        metadata.screenName = screenName;
        
        // Extract promptHelp (placeholder text for text inputs)
        metadata.promptHelp = getStringValue(displayDef, "promptHelp", getStringValue(displayDef, "prompt_help", null));
        
        // Extract labelText (permanent label displayed before/above control - used for buttons and labels)
        metadata.labelText = getStringValue(displayDef, "labelText", getStringValue(displayDef, "label_text", null));
        
        // Extract labelText alignment
        metadata.labelTextAlignment = getStringValue(displayDef, "labelTextAlignment", getStringValue(displayDef, "label_text_alignment", null));
        
        // Extract onClick event handler for buttons
        metadata.onClick = getStringValue(displayDef, "onClick", getStringValue(displayDef, "on_click", null));
        
        // Extract options for selection controls
        if (displayDef.containsKey("options")) {
            Object optionsObj = displayDef.get("options");
            if (optionsObj instanceof java.util.List) {
                metadata.options = new ArrayList<>();
                for (Object opt : (java.util.List<?>) optionsObj) {
                    metadata.options.add(String.valueOf(opt));
                }
            }
        }
        
        // Extract styling properties
        metadata.labelColor = getStringValue(displayDef, "labelColor", getStringValue(displayDef, "label_color", null));
        metadata.labelBold = getBooleanValue(displayDef, "labelBold", getBooleanValue(displayDef, "label_bold", null));
        metadata.labelItalic = getBooleanValue(displayDef, "labelItalic", getBooleanValue(displayDef, "label_italic", null));
        metadata.labelFontSize = getStringValue(displayDef, "labelFontSize", getStringValue(displayDef, "label_font_size", null));
        metadata.itemFontSize = getStringValue(displayDef, "itemFontSize", getStringValue(displayDef, "item_font_size", null));
        metadata.itemColor = getStringValue(displayDef, "itemColor", getStringValue(displayDef, "item_color", null));
        metadata.itemBold = getBooleanValue(displayDef, "itemBold", getBooleanValue(displayDef, "item_bold", null));
        metadata.itemItalic = getBooleanValue(displayDef, "itemItalic", getBooleanValue(displayDef, "item_italic", null));
        metadata.maxLength = getIntValue(displayDef, "maxLength", getIntValue(displayDef, "max_length", null));

        return metadata;
    }

    /**
     * Merges two DisplayItem metadata objects.
     * The overlay metadata takes precedence over base metadata for non-null fields.
     * 
     * @param base The base metadata (typically from var definition)
     * @param overlay The overlay metadata (typically from area item display)
     * @return Merged metadata with overlay values taking precedence
     */
    private static DisplayItem mergeDisplayMetadata(DisplayItem base, DisplayItem overlay) {
        // If no base, return overlay
        if (base == null) {
            return overlay;
        }
        // If no overlay, return base
        if (overlay == null) {
            return base;
        }
        
        // Create a new DisplayItem with base values
        DisplayItem merged = new DisplayItem();
        
        // Copy all fields from base first
        merged.itemType = base.itemType;
        merged.type = base.type;
        merged.cssClass = base.cssClass;
        merged.mandatory = base.mandatory;
        merged.caseFormat = base.caseFormat;
        merged.min = base.min;
        merged.max = base.max;
        merged.style = base.style;
        merged.screenName = base.screenName;
        merged.alignment = base.alignment;
        merged.pattern = base.pattern;
        merged.promptHelp = base.promptHelp;
        merged.labelText = base.labelText;
        merged.labelTextAlignment = base.labelTextAlignment;
        merged.options = base.options;
        merged.labelColor = base.labelColor;
        merged.labelBold = base.labelBold;
        merged.labelItalic = base.labelItalic;
        merged.labelFontSize = base.labelFontSize;
        merged.itemFontSize = base.itemFontSize;
        merged.maxLength = base.maxLength;
        merged.itemColor = base.itemColor;
        merged.itemBold = base.itemBold;
        merged.itemItalic = base.itemItalic;
        merged.onClick = base.onClick;
        
        // Override with non-null overlay values
        if (overlay.itemType != null) merged.itemType = overlay.itemType;
        if (overlay.type != null) merged.type = overlay.type;
        if (overlay.cssClass != null) merged.cssClass = overlay.cssClass;
        // Always use overlay's mandatory flag if it's been explicitly set (even if false)
        merged.mandatory = overlay.mandatory;
        if (overlay.caseFormat != null) merged.caseFormat = overlay.caseFormat;
        if (overlay.min != null) merged.min = overlay.min;
        if (overlay.max != null) merged.max = overlay.max;
        if (overlay.style != null) merged.style = overlay.style;
        if (overlay.screenName != null) merged.screenName = overlay.screenName;
        if (overlay.alignment != null) merged.alignment = overlay.alignment;
        if (overlay.pattern != null) merged.pattern = overlay.pattern;
        if (overlay.promptHelp != null) merged.promptHelp = overlay.promptHelp;
        if (overlay.labelText != null) merged.labelText = overlay.labelText;
        if (overlay.labelTextAlignment != null) merged.labelTextAlignment = overlay.labelTextAlignment;
        if (overlay.options != null) merged.options = overlay.options;
        if (overlay.labelColor != null) merged.labelColor = overlay.labelColor;
        if (overlay.labelBold != null) merged.labelBold = overlay.labelBold;
        if (overlay.labelItalic != null) merged.labelItalic = overlay.labelItalic;
        if (overlay.labelFontSize != null) merged.labelFontSize = overlay.labelFontSize;
        if (overlay.itemFontSize != null) merged.itemFontSize = overlay.itemFontSize;
        if (overlay.maxLength != null) merged.maxLength = overlay.maxLength;
        if (overlay.itemColor != null) merged.itemColor = overlay.itemColor;
        if (overlay.itemBold != null) merged.itemBold = overlay.itemBold;
        if (overlay.itemItalic != null) merged.itemItalic = overlay.itemItalic;
        if (overlay.onClick != null) merged.onClick = overlay.onClick;
        
        return merged;
    }

    // Helper methods for safe value extraction from Maps
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            return value != null ? String.valueOf(value) : defaultValue;
        }
        return defaultValue;
    }

    private static double getNumberValue(Map<String, Object> map, String key, double defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map.containsKey(key.toLowerCase())) {
            Object value = map.get(key.toLowerCase());
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return defaultValue;
    }

    /**
     * Calculates the maximum label width needed for proper alignment. This
     * ensures all labels in a form have consistent width.
     *
     * @param items The list of items to check for labels
     * @param screenName The screen name
     * @param context InterpreterContext for accessing display metadata
     * @return The maximum label width in pixels
     */
    private static double calculateMaxLabelWidth(List<AreaItem> items, String screenName,
            InterpreterContext context) {
        double maxWidth = 100; // Minimum width
        javafx.scene.text.Text measuringText = new javafx.scene.text.Text();

        for (AreaItem item : items) {
            // Get metadata with same merge logic as createAreaWithItems
            DisplayItem metadata = null;
            if (item.varRef != null && context != null) {
                metadata = context.getDisplayItem().get(screenName + "." + item.varRef);
            }
            if (item.displayItem != null) {
                metadata = mergeDisplayMetadata(metadata, item.displayItem);
            }

            // Only measure labels for controls that will be wrapped (not Label or Button controls)
            if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
                // Check if this is a control that will have a label wrapper
                if (metadata.itemType != DisplayItem.ItemType.LABEL
                        && metadata.itemType != DisplayItem.ItemType.LABELTEXT
                        && metadata.itemType != DisplayItem.ItemType.BUTTON) {

                    // Parse font size and create proper Font object for accurate measurement
                    double fontSize = 13.0; // Default font size
                    javafx.scene.text.FontWeight fontWeight = javafx.scene.text.FontWeight.NORMAL;
                    
                    if (metadata.labelFontSize != null && !metadata.labelFontSize.isEmpty()) {
                        fontSize = parseFontSize(metadata.labelFontSize);
                    }
                    
                    if (metadata.labelBold != null && metadata.labelBold) {
                        fontWeight = javafx.scene.text.FontWeight.BOLD;
                    }
                    
                    // Set font directly on Text node for accurate measurement
                    measuringText.setFont(javafx.scene.text.Font.font("System", fontWeight, fontSize));

                    // Handle multiline labels by splitting on \n and measuring the longest line
                    String labelText = metadata.labelText;
                    if (labelText.contains("\n")) {
                        String[] lines = labelText.split("\n");
                        double maxLineWidth = 0;
                        for (String line : lines) {
                            measuringText.setText(line);
                            double lineWidth = measuringText.getLayoutBounds().getWidth();
                            if (lineWidth > maxLineWidth) {
                                maxLineWidth = lineWidth;
                            }
                        }
                        double width = maxLineWidth + 20; // Add padding
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    } else {
                        measuringText.setText(labelText);
                        double width = measuringText.getLayoutBounds().getWidth() + 20; // Add padding
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    }
                }
            }
        }

        return maxWidth;
    }
    
    /**
     * Parses a font size string (e.g., "14px", "1.5em", "16") and returns the size in pixels.
     * 
     * @param fontSizeStr The font size string to parse
     * @return The font size in pixels
     */
    private static double parseFontSize(String fontSizeStr) {
        if (fontSizeStr == null || fontSizeStr.isEmpty()) {
            return 13.0; // Default font size
        }
        
        String trimmed = fontSizeStr.trim().toLowerCase();
        
        try {
            if (trimmed.endsWith("px")) {
                // Parse pixel values (e.g., "14px")
                return Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
            } else if (trimmed.endsWith("em")) {
                // Parse em values (e.g., "1.5em") - 1em = 13px (default)
                double emValue = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
                return emValue * 13.0;
            } else if (trimmed.endsWith("pt")) {
                // Parse point values (e.g., "12pt") - 1pt = 1.333px
                double ptValue = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2));
                return ptValue * 1.333;
            } else {
                // No unit specified, assume pixels
                return Double.parseDouble(trimmed);
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse font size '" + fontSizeStr + "', using default 13px");
            return 13.0;
        }
    }

    /**
     * Creates a labeled control by wrapping the control with a label. The label
     * is displayed based on the specified alignment.
     *
     * @param labelText The text for the label
     * @param alignment The alignment: "left", "center", "right" (default:
     * "left")
     * @param control The control to label
     * @param minWidth The minimum width for the label for alignment consistency
     * @param metadata The display metadata containing font size and styling
     * information
     * @return A container with the label and control
     */
    private static Node createLabeledControl(String labelText, String alignment, Node control, double minWidth, DisplayItem metadata) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(labelText);

        // Build label style with right alignment, padding, and default text color
        StringBuilder styleBuilder = new StringBuilder("-fx-font-weight: normal; -fx-padding: 0 10 0 0; -fx-alignment: center-right; -fx-text-fill: #333333;");

        // Apply label styling from metadata
        if (metadata != null) {
            // Apply font size if specified
            if (metadata.labelFontSize != null && !metadata.labelFontSize.isEmpty()) {
                styleBuilder.append(" -fx-font-size: ").append(metadata.labelFontSize).append(";");
            }

            // Apply label color if specified (this will override the default)
            if (metadata.labelColor != null && !metadata.labelColor.isEmpty()) {
                // Remove default text-fill and apply custom color
                String currentStyle = styleBuilder.toString();
                currentStyle = currentStyle.replace("-fx-text-fill: #333333;", "");
                styleBuilder = new StringBuilder(currentStyle);
                styleBuilder.append(" -fx-text-fill: ").append(metadata.labelColor).append(";");
            }

            // Apply bold if specified
            if (metadata.labelBold != null && metadata.labelBold) {
                styleBuilder.append(" -fx-font-weight: bold;");
            }

            // Apply italic if specified
            if (metadata.labelItalic != null && metadata.labelItalic) {
                styleBuilder.append(" -fx-font-style: italic;");
            }
        }

        label.setStyle(styleBuilder.toString());
        label.setMinWidth(minWidth); // Use calculated minimum width to align labels underneath each other
        label.setMaxWidth(Region.USE_PREF_SIZE);

        // Determine alignment (default to left if not specified)
        String actualAlignment = (alignment != null) ? alignment.toLowerCase() : "left";

        // Create container based on alignment
        javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(5);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        // Make container pick on bounds so tooltips on child controls work properly
        container.setPickOnBounds(false);

        switch (actualAlignment) {
            case "right":
                // Control first, then label on the right
                container.getChildren().addAll(control, label);
                container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                break;
            case "center":
                // Center both
                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.getChildren().addAll(label, control);
                break;
            case "left":
            default:
                // Label first (on the left), then control
                container.getChildren().addAll(label, control);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                break;
        }

        return container;
    }

    /**
     * Sets up two-way data binding between a UI control and a screen variable.
     * When the variable changes, the UI updates. When the UI changes, the
     * variable updates.
     *
     * @param control The JavaFX control to bind
     * @param varName The variable name
     * @param screenVars The map containing screen variables
     * @param metadata The DisplayItem metadata for the control
     */
    private static void setupVariableBinding(Node control, String varName,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        if (control == null || varName == null || screenVars == null) {
            return;
        }

        // Initialize control with current variable value
        Object currentValue = screenVars.get(varName);
        updateControlFromValue(control, currentValue, metadata);

        // Set up listener on the control to update the variable when control changes
        addControlListener(control, varName, screenVars, varTypes, metadata);

        // Store references for potential future use
        control.getProperties().put("varName", varName);
        control.getProperties().put("screenVars", screenVars);
        control.getProperties().put("varTypes", varTypes);
        control.getProperties().put("metadata", metadata);
    }

    /**
     * Updates a control's value based on the variable value.
     */
    private static void updateControlFromValue(Node control, Object value, DisplayItem metadata) {
        // Handle HBox containing slider (when showSliderValue is true)
        if (control instanceof javafx.scene.layout.HBox) {
            javafx.scene.layout.HBox hbox = (javafx.scene.layout.HBox) control;
            // Check if this HBox contains a Slider as its first child
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof javafx.scene.control.Slider) {
                javafx.scene.control.Slider slider = (javafx.scene.control.Slider) hbox.getChildren().get(0);
                if (value instanceof Number) {
                    slider.setValue(((Number) value).doubleValue());
                    // The value label will be updated automatically via the listener in AreaItemFactory
                }
                return;
            }
        }
        
        if (control instanceof javafx.scene.control.TextField) {
            ((javafx.scene.control.TextField) control).setText(value != null ? String.valueOf(value) : "");
        } else if (control instanceof javafx.scene.control.TextArea) {
            ((javafx.scene.control.TextArea) control).setText(value != null ? String.valueOf(value) : "");
        } else if (control instanceof javafx.scene.control.CheckBox) {
            ((javafx.scene.control.CheckBox) control).setSelected(value instanceof Boolean && (Boolean) value);
        } else if (control instanceof javafx.scene.control.Slider) {
            if (value instanceof Number) {
                ((javafx.scene.control.Slider) control).setValue(((Number) value).doubleValue());
            }
        } else if (control instanceof javafx.scene.control.Spinner) {
            if (value instanceof Number) {
                @SuppressWarnings("unchecked")
                javafx.scene.control.Spinner<Integer> spinner = (javafx.scene.control.Spinner<Integer>) control;
                // Check if ValueFactory exists before trying to set value
                if (spinner.getValueFactory() != null) {
                    spinner.getValueFactory().setValue(((Number) value).intValue());
                }
            }
        } else if (control instanceof javafx.scene.control.ComboBox) {
            if (value != null) {
                @SuppressWarnings("unchecked")
                javafx.scene.control.ComboBox<String> comboBox = (javafx.scene.control.ComboBox<String>) control;
                comboBox.setValue(String.valueOf(value));
            }
        } else if (control instanceof javafx.scene.control.ChoiceBox) {
            if (value != null) {
                @SuppressWarnings("unchecked")
                javafx.scene.control.ChoiceBox<String> choiceBox = (javafx.scene.control.ChoiceBox<String>) control;
                choiceBox.setValue(String.valueOf(value));
            }
        } else if (control instanceof javafx.scene.control.Label) {
            ((javafx.scene.control.Label) control).setText(value != null ? String.valueOf(value) : "");
        }
    }

    /**
     * Adds a listener to a control to update the variable when the control
     * changes.
     */
    private static void addControlListener(Node control, String varName,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
            java.util.concurrent.ConcurrentHashMap<String, DataType> varTypes,
            DisplayItem metadata) {
        // Handle HBox containing slider (when showSliderValue is true)
        if (control instanceof javafx.scene.layout.HBox) {
            javafx.scene.layout.HBox hbox = (javafx.scene.layout.HBox) control;
            // Check if this HBox contains a Slider as its first child
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof javafx.scene.control.Slider) {
                javafx.scene.control.Slider slider = (javafx.scene.control.Slider) hbox.getChildren().get(0);
                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    screenVars.put(varName, newVal.intValue());
                });
                return;
            }
        }
        
        if (control instanceof javafx.scene.control.TextField) {
            ((javafx.scene.control.TextField) control).textProperty().addListener((obs, oldVal, newVal) -> {
                // Convert the string value to the appropriate type if type info is available
                Object convertedValue = newVal;
                if (varTypes != null && varTypes.containsKey(varName)) {
                    DataType type = varTypes.get(varName);
                    try {
                        convertedValue = type.convertValue(newVal);
                    } catch (Exception e) {
                        // If conversion fails, keep as string
                        System.err.println("Warning: Could not convert '" + newVal + "' to " + type + " for variable '" + varName + "'");
                    }
                }
                screenVars.put(varName, convertedValue);
            });
        } else if (control instanceof javafx.scene.control.TextArea) {
            ((javafx.scene.control.TextArea) control).textProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
            });
        } else if (control instanceof javafx.scene.control.CheckBox) {
            ((javafx.scene.control.CheckBox) control).selectedProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
            });
        } else if (control instanceof javafx.scene.control.Slider) {
            ((javafx.scene.control.Slider) control).valueProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal.intValue());
            });
        } else if (control instanceof javafx.scene.control.Spinner) {
            @SuppressWarnings("unchecked")
            javafx.scene.control.Spinner<Integer> spinner = (javafx.scene.control.Spinner<Integer>) control;
            // Check if ValueFactory exists before adding listener
            if (spinner.getValueFactory() != null) {
                spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        screenVars.put(varName, newVal);
                    }
                });
            }
        } else if (control instanceof javafx.scene.control.ComboBox) {
            @SuppressWarnings("unchecked")
            javafx.scene.control.ComboBox<String> comboBox = (javafx.scene.control.ComboBox<String>) control;
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
            });
        } else if (control instanceof javafx.scene.control.ChoiceBox) {
            @SuppressWarnings("unchecked")
            javafx.scene.control.ChoiceBox<String> choiceBox = (javafx.scene.control.ChoiceBox<String>) control;
            choiceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
            });
        }
    }

    /**
     * Refreshes all bound controls by updating their values from the screenVars
     * map. This is called after onClick handlers execute to reflect variable
     * changes in the UI.
     */
    private static void refreshBoundControls(List<Node> boundControls,
            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        if (boundControls == null || screenVars == null) {
            return;
        }

        for (Node control : boundControls) {
            String varName = (String) control.getProperties().get("varName");
            DisplayItem metadata = (DisplayItem) control.getProperties().get("metadata");

            if (varName != null) {
                Object currentValue = screenVars.get(varName);
                updateControlFromValue(control, currentValue, metadata);
            }
        }
    }

    // Schema validation methods
    /**
     * Validates a screen definition against the JSON schema.
     *
     * @param screenDef The screen definition to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateScreenDefinition(Map<String, Object> screenDef) {
        if (screenSchema == null) {
            System.err.println("Warning: Screen schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(screenDef, screenSchema);
            if (!errors.isEmpty()) {
                String errorMessage = "Screen definition validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate screen definition: " + e.getMessage(), e);
        }
    }

    /**
     * Validates an area definition against the JSON schema.
     *
     * @param areaDef The area definition to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateAreaDefinition(Map<String, Object> areaDef) {
        if (areaSchema == null) {
            System.err.println("Warning: Area schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(areaSchema, areaSchema);

            if (!errors.isEmpty()) {
                String errorMessage = "Area definition validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate area definition: " + e.getMessage(), e);
        }
    }

    /**
     * Validates display metadata against the JSON schema.
     *
     * @param displayDef The display metadata to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateDisplayItem(Map<String, Object> displayDef) {
        if (displayMetadataSchema == null) {
            System.err.println("Warning: Display metadata schema not loaded, skipping validation");
            return;
        }

        try {
            Map<String, ArrayDef> errors = JsonSchema.validate(displayDef, displayMetadataSchema);

            if (!errors.isEmpty()) {
                String errorMessage = "Display metadata validation failed:\n" + Util.stringify(errors);
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Failed to validate display metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if schema validation is available.
     *
     * @return true if schemas are loaded and validation is available
     */
    public static boolean isValidationAvailable() {
        return screenSchema != null && areaSchema != null && displayMetadataSchema != null;
    }
}
