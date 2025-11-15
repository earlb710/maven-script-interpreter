package com.eb.script.interpreter;

import com.eb.script.interpreter.AreaDefinition.AreaItem;
import com.eb.script.interpreter.AreaDefinition.AreaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Factory class for creating complete JavaFX screens/windows from AreaDefinitions.
 * This factory uses AreaContainerFactory and AreaItemFactory to create a fully assembled screen.
 * Includes JSON Schema validation for screen definitions.
 */
public class ScreenFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static JsonSchema screenSchema;
    private static JsonSchema areaSchema;
    private static JsonSchema displayMetadataSchema;

    static {
        try {
            // Load schemas from resources
            InputStream screenSchemaStream = ScreenFactory.class.getResourceAsStream("/json/screen-definition.json");
            InputStream areaSchemaStream = ScreenFactory.class.getResourceAsStream("/json/area-definition.json");
            InputStream displayMetadataSchemaStream = ScreenFactory.class.getResourceAsStream("/json/display-metadata.json");

            if (screenSchemaStream != null) {
                screenSchema = schemaFactory.getSchema(screenSchemaStream);
            }
            if (areaSchemaStream != null) {
                areaSchema = schemaFactory.getSchema(areaSchemaStream);
            }
            if (displayMetadataSchemaStream != null) {
                displayMetadataSchema = schemaFactory.getSchema(displayMetadataSchemaStream);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load JSON schemas: " + e.getMessage());
        }
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions.
     * This method creates containers, adds items, and applies layout properties.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param metadataProvider Function to retrieve DisplayItem for variables (screenName, varName) -> metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
                                      List<AreaDefinition> areas,
                                      BiFunction<String, String, DisplayItem> metadataProvider) {
        return createScreen(screenName, title, width, height, areas, metadataProvider, null);
    }

    /**
     * Creates a complete JavaFX window/screen from area definitions with variable binding.
     * This method creates containers, adds items, applies layout properties, and sets up two-way data binding.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param metadataProvider Function to retrieve DisplayItem for variables (screenName, varName) -> metadata
     * @param screenVars The ConcurrentHashMap containing screen variables for two-way binding (can be null)
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
                                      List<AreaDefinition> areas,
                                      BiFunction<String, String, DisplayItem> metadataProvider,
                                      java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        Stage stage = new Stage();
        stage.setTitle(title);

        // Create root container - use VBox as default if multiple areas
        Region rootContainer;
        
        if (areas == null || areas.isEmpty()) {
            // No areas defined, create empty pane
            rootContainer = new Pane();
        } else if (areas.size() == 1) {
            // Single area - use it as root
            rootContainer = createAreaWithItems(areas.get(0), screenName, metadataProvider, screenVars);
        } else {
            // Multiple areas - arrange in VBox
            VBox root = new VBox();
            root.setSpacing(10);
            
            for (AreaDefinition areaDef : areas) {
                Region areaContainer = createAreaWithItems(areaDef, screenName, metadataProvider, screenVars);
                root.getChildren().add(areaContainer);
            }
            
            rootContainer = root;
        }

        Scene scene = new Scene(rootContainer, width, height);
        stage.setScene(scene);

        return stage;
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
                                               BiFunction<String, String, DisplayItem> metadataProvider) {
        return createAreaWithItems(areaDef, screenName, metadataProvider, null);
    }

    /**
     * Creates a container from AreaDefinition and adds all items to it with variable binding.
     */
    private static Region createAreaWithItems(AreaDefinition areaDef, String screenName,
                                               BiFunction<String, String, DisplayItem> metadataProvider,
                                               java.util.concurrent.ConcurrentHashMap<String, Object> screenVars) {
        // Create the container using AreaContainerFactory
        Region container = AreaContainerFactory.createContainer(areaDef);

        // Sort items by sequence
        if (areaDef.items != null && !areaDef.items.isEmpty()) {
            List<AreaItem> sortedItems = areaDef.items.stream()
                .sorted(Comparator.comparingInt(item -> item.sequence))
                .toList();

            // Add items to container based on container type
            for (AreaItem item : sortedItems) {
                // Get metadata for the item
                DisplayItem metadata = item.displayMetadata;
                if (metadata == null && item.varRef != null && metadataProvider != null) {
                    metadata = metadataProvider.apply(screenName, item.varRef);
                }

                // Create the item using AreaItemFactory
                Node control = AreaItemFactory.createItem(item, metadata);

                // Set up two-way data binding if screenVars is provided and item has a varRef
                if (screenVars != null && item.varRef != null) {
                    setupVariableBinding(control, item.varRef, screenVars, metadata);
                }

                // Apply item layout properties
                applyItemLayoutProperties(control, item);

                // Add item to container based on container type
                addItemToContainer(container, control, item, areaDef.areaType);
            }
        }
        
        // Add nested child areas to the container
        if (areaDef.childAreas != null && !areaDef.childAreas.isEmpty()) {
            for (AreaDefinition childArea : areaDef.childAreas) {
                Region childContainer = createAreaWithItems(childArea, screenName, metadataProvider, screenVars);
                
                // Add the child container to the parent container
                // Treat child areas as regular nodes
                addChildAreaToContainer(container, childContainer, areaDef.areaType);
            }
        }

        return container;
    }

    /**
     * Applies layout properties from AreaItem to the control.
     * These are the properties NOT applied by AreaItemFactory (which only applies display properties).
     */
    private static void applyItemLayoutProperties(Node control, AreaItem item) {
        // Apply sizing properties
        if (control instanceof Region) {
            Region region = (Region) control;
            
            if (item.prefWidth != null && !item.prefWidth.isEmpty()) {
                try {
                    double width = parseSize(item.prefWidth);
                    if (width > 0) region.setPrefWidth(width);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
            
            if (item.prefHeight != null && !item.prefHeight.isEmpty()) {
                try {
                    double height = parseSize(item.prefHeight);
                    if (height > 0) region.setPrefHeight(height);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
            
            if (item.minWidth != null && !item.minWidth.isEmpty()) {
                try {
                    double width = parseSize(item.minWidth);
                    if (width > 0) region.setMinWidth(width);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
            
            if (item.minHeight != null && !item.minHeight.isEmpty()) {
                try {
                    double height = parseSize(item.minHeight);
                    if (height > 0) region.setMinHeight(height);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
            
            if (item.maxWidth != null && !item.maxWidth.isEmpty()) {
                try {
                    double width = parseSize(item.maxWidth);
                    if (width > 0) region.setMaxWidth(width);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
            
            if (item.maxHeight != null && !item.maxHeight.isEmpty()) {
                try {
                    double height = parseSize(item.maxHeight);
                    if (height > 0) region.setMaxHeight(height);
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
                        
                        if (top >= 0) AnchorPane.setTopAnchor(control, top);
                        if (right >= 0) AnchorPane.setRightAnchor(control, right);
                        if (bottom >= 0) AnchorPane.setBottomAnchor(control, bottom);
                        if (left >= 0) AnchorPane.setLeftAnchor(control, left);
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
     * Adds a child area (nested container) to a parent container.
     * Similar to addItemToContainer but for child areas.
     */
    private static void addChildAreaToContainer(Region container, Region childArea, AreaType areaType) {
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
     * Parse size string to double value.
     * Supports plain numbers and percentages (e.g., "300", "50%").
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
     * Parse insets string to Insets object.
     * Supports formats: "10" (all), "10 5" (vertical horizontal), "10 5 10 5" (top right bottom left).
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
     * Convenience method to create and show a screen on the JavaFX Application Thread.
     */
    public static void createAndShowScreen(String screenName, String title, double width, double height,
                                            List<AreaDefinition> areas,
                                            BiFunction<String, String, DisplayItem> metadataProvider,
                                            boolean maximize) {
        Platform.runLater(() -> {
            Stage stage = createScreen(screenName, title, width, height, areas, metadataProvider);
            
            if (maximize) {
                stage.setMaximized(true);
            }
            
            stage.show();
        });
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen definition (JSON/EBS format).
     * This method parses the screen definition and creates the window.
     * Validates the screen definition against the JSON schema if validation is enabled.
     *
     * @param screenDef Map containing screen definition with keys: name, title, width, height, vars, area
     * @return A Stage representing the complete window
     * @throws IllegalArgumentException if the screen definition is invalid
     */
    public static Stage createScreenFromDefinition(Map<String, Object> screenDef) {
        return createScreenFromDefinition(screenDef, true);
    }

    /**
     * Creates a complete JavaFX window/screen from a Map-based screen definition (JSON/EBS format).
     * This method parses the screen definition and creates the window.
     *
     * @param screenDef Map containing screen definition with keys: name, title, width, height, vars, area
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

        // Create metadata provider from map
        BiFunction<String, String, DisplayItem> metadataProvider = 
            (sName, varName) -> metadataMap.get(varName);

        return createScreen(screenName, title, width, height, areas, metadataProvider);
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
                item.displayMetadata = parseDisplayItem(displayDef, screenName);
            }
        }
        
        // UI properties
        // promptText now goes into displayMetadata
        String promptText = getStringValue(itemDef, "promptText", getStringValue(itemDef, "prompt_text", null));
        if (promptText != null) {
            // If displayMetadata doesn't exist yet, create it
            if (item.displayMetadata == null) {
                item.displayMetadata = new DisplayItem();
            }
            item.displayMetadata.promptText = promptText;
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
        
        return metadata;
    }

    // Helper methods for safe value extraction from Maps

    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            return value != null ? String.valueOf(value) : defaultValue;
        }
        return defaultValue;
    }

    private static double getNumberValue(Map<String, Object> map, String key, double defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
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
        if (map.containsKey(key)) {
            Object value = map.get(key);
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
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return defaultValue;
    }

    /**
     * Sets up two-way data binding between a UI control and a screen variable.
     * When the variable changes, the UI updates. When the UI changes, the variable updates.
     *
     * @param control The JavaFX control to bind
     * @param varName The variable name
     * @param screenVars The map containing screen variables
     * @param metadata The DisplayItem metadata for the control
     */
    private static void setupVariableBinding(Node control, String varName,
                                              java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
                                              DisplayItem metadata) {
        if (control == null || varName == null || screenVars == null) {
            return;
        }

        // Initialize control with current variable value
        Object currentValue = screenVars.get(varName);
        updateControlFromValue(control, currentValue, metadata);

        // Set up listener on the control to update the variable when control changes
        addControlListener(control, varName, screenVars, metadata);

        // Store references for potential future use
        control.getProperties().put("varName", varName);
        control.getProperties().put("screenVars", screenVars);
        control.getProperties().put("metadata", metadata);
    }

    /**
     * Updates a control's value based on the variable value.
     */
    private static void updateControlFromValue(Node control, Object value, DisplayItem metadata) {
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
                spinner.getValueFactory().setValue(((Number) value).intValue());
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
     * Adds a listener to a control to update the variable when the control changes.
     */
    private static void addControlListener(Node control, String varName,
                                            java.util.concurrent.ConcurrentHashMap<String, Object> screenVars,
                                            DisplayItem metadata) {
        if (control instanceof javafx.scene.control.TextField) {
            ((javafx.scene.control.TextField) control).textProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
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
            spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                screenVars.put(varName, newVal);
            });
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
            JsonNode jsonNode = objectMapper.valueToTree(screenDef);
            Set<ValidationMessage> errors = screenSchema.validate(jsonNode);
            
            if (!errors.isEmpty()) {
                String errorMessage = "Screen definition validation failed:\n" +
                    errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("\n"));
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
            JsonNode jsonNode = objectMapper.valueToTree(areaDef);
            Set<ValidationMessage> errors = areaSchema.validate(jsonNode);
            
            if (!errors.isEmpty()) {
                String errorMessage = "Area definition validation failed:\n" +
                    errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("\n"));
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
            JsonNode jsonNode = objectMapper.valueToTree(displayDef);
            Set<ValidationMessage> errors = displayMetadataSchema.validate(jsonNode);
            
            if (!errors.isEmpty()) {
                String errorMessage = "Display metadata validation failed:\n" +
                    errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("\n"));
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
