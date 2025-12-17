package com.eb.script.interpreter.screen;

import javafx.scene.Node;
import javafx.scene.layout.*;

/**
 * Represents a screen container type (e.g., Screen.container for HBox, VBox, GridPane).
 * This is used to track the JavaFX container type for named screen areas.
 * Also stores a reference to the actual JavaFX Region node.
 * 
 * @author Earl Bosch
 */
public class ScreenContainerType {
    
    private final String containerType;  // e.g., "hbox", "vbox", "gridpane"
    private Region javafxRegion;  // Reference to the actual JavaFX container
    private String screenName;  // Screen this container belongs to
    private String areaName;  // This area's name
    private String parentAreaName;  // Parent area name (null if root)
    private java.util.List<String> childAreaNames;  // Child area names
    
    /**
     * Create a new ScreenContainerType
     * @param containerType The container type string (e.g., "hbox", "vbox", "gridpane")
     */
    public ScreenContainerType(String containerType) {
        this.containerType = containerType != null ? containerType.toLowerCase() : null;
        this.javafxRegion = null;
        this.childAreaNames = new java.util.ArrayList<>();
    }
    
    /**
     * Create a new ScreenContainerType with JavaFX Region
     * @param containerType The container type string (e.g., "hbox", "vbox", "gridpane")
     * @param javafxRegion The JavaFX Region reference
     */
    public ScreenContainerType(String containerType, Region javafxRegion) {
        this.containerType = containerType != null ? containerType.toLowerCase() : null;
        this.javafxRegion = javafxRegion;
        this.childAreaNames = new java.util.ArrayList<>();
    }
    
    /**
     * Get the container type string
     * @return The container type (lowercase)
     */
    public String getContainerType() {
        return containerType;
    }
    
    /**
     * Get the JavaFX Region reference
     * @return The JavaFX Region, or null if not set
     */
    public Region getJavaFXRegion() {
        return javafxRegion;
    }
    
    /**
     * Set the JavaFX Region reference
     * @param javafxRegion The JavaFX Region to store
     */
    public void setJavaFXRegion(Region javafxRegion) {
        this.javafxRegion = javafxRegion;
    }
    
    /**
     * Set the screen name this container belongs to
     */
    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
    
    /**
     * Get the screen name this container belongs to
     */
    public String getScreenName() {
        return screenName;
    }
    
    /**
     * Set this area's name
     */
    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
    
    /**
     * Get this area's name
     */
    public String getAreaName() {
        return areaName;
    }
    
    /**
     * Set the parent area name
     */
    public void setParentAreaName(String parentAreaName) {
        this.parentAreaName = parentAreaName;
    }
    
    /**
     * Get the parent area name
     */
    public String getParentAreaName() {
        return parentAreaName;
    }
    
    /**
     * Add a child area name
     */
    public void addChildAreaName(String childAreaName) {
        if (childAreaName != null && !childAreaNames.contains(childAreaName)) {
            childAreaNames.add(childAreaName);
        }
    }
    
    /**
     * Get the list of child area names
     */
    public java.util.List<String> getChildAreaNames() {
        return new java.util.ArrayList<>(childAreaNames);
    }
    
    /**
     * Get the full type name in "Screen.container" format
     * @return The full type name (always "Screen.container")
     */
    public String getFullTypeName() {
        return "Screen.container";
    }
    
    /**
     * Get a description of the JavaFX container including type, size, style, etc.
     * This now includes all properties from .properties as well.
     * @return String description of the JavaFX container
     */
    public String getJavaFXDescription() {
        if (javafxRegion == null) {
            return "JavaFX Container: null";
        }
        
        // Use getProperties() to include all properties
        return getProperties();
    }
    
    /**
     * Get detailed help information about this container type.
     * Reads help content from help-lookup.json resource file.
     * @return Help text for this container type
     */
    @SuppressWarnings("unchecked")
    public String getHelp() {
        if (containerType == null) {
            return "Unknown container type";
        }
        
        try {
            // Load help-lookup.json from classpath
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("help-lookup.json");
            if (is == null) {
                return getFallbackHelp(); // Use fallback if file not found
            }
            
            String jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            is.close();
            
            // Parse JSON
            java.util.Map<String, Object> lookup = (java.util.Map<String, Object>) com.eb.script.json.Json.parse(jsonContent);
            java.util.Map<String, Object> containers = (java.util.Map<String, Object>) lookup.get("containers");
            
            if (containers == null || !containers.containsKey(containerType.toLowerCase())) {
                return getFallbackHelp(); // Use fallback if container not found
            }
            
            // Get container entry
            java.util.Map<String, Object> entry = (java.util.Map<String, Object>) containers.get(containerType.toLowerCase());
            
            // Format help text
            StringBuilder help = new StringBuilder();
            help.append("═══════════════════════════════════════════════════════════\n");
            help.append("Container Type: ").append(getFullTypeName()).append("\n");
            help.append("═══════════════════════════════════════════════════════════\n\n");
            
            // Add description
            String shortDesc = (String) entry.get("short_description");
            String longHelp = (String) entry.get("long_help");
            if (longHelp != null && !longHelp.isEmpty()) {
                help.append("Description:\n");
                help.append(longHelp).append("\n\n");
            } else if (shortDesc != null && !shortDesc.isEmpty()) {
                help.append("Description:\n");
                help.append(shortDesc).append("\n\n");
            }
            
            // Add supported properties
            String properties = (String) entry.get("properties");
            if (properties != null && !properties.isEmpty()) {
                help.append("Supported Properties:\n");
                String[] props = properties.split(",\\s*");
                for (String prop : props) {
                    help.append("  • ").append(prop).append("\n");
                }
                help.append("\n");
            }
            
            // Add usage example
            String example = (String) entry.get("example");
            if (example != null && !example.isEmpty()) {
                help.append("Example:\n");
                help.append(example).append("\n");
            }
            
            help.append("═══════════════════════════════════════════════════════════\n");
            return help.toString();
            
        } catch (Exception ex) {
            // If anything goes wrong, use fallback
            return getFallbackHelp();
        }
    }
    
    /**
     * Fallback help generation when help-lookup.json is not available.
     * @return Basic help text for this container type
     */
    private String getFallbackHelp() {
        StringBuilder help = new StringBuilder();
        help.append("═══════════════════════════════════════════════════════════\n");
        help.append("Container Type: ").append(getFullTypeName()).append("\n");
        help.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Add description based on container type
        String description = getContainerDescription(containerType);
        if (description != null && !description.isEmpty()) {
            help.append("Description:\n");
            help.append(description).append("\n\n");
        }
        
        // Add supported properties
        help.append("Supported Properties:\n");
        help.append(getSupportedProperties(containerType));
        help.append("\n");
        
        // Add usage example
        String example = getUsageExample(containerType);
        if (example != null && !example.isEmpty()) {
            help.append("Example:\n");
            help.append(example).append("\n");
        }
        
        help.append("═══════════════════════════════════════════════════════════\n");
        return help.toString();
    }
    
    /**
     * Get a description for a specific container type.
     */
    private String getContainerDescription(String type) {
        return switch (type.toLowerCase()) {
            case "hbox" -> "Horizontal box layout that arranges children in a single row from left to right.\n" +
                          "Ideal for buttons, toolbars, and horizontal layouts.";
            case "vbox" -> "Vertical box layout that arranges children in a single column from top to bottom.\n" +
                          "Ideal for forms, menus, and vertical layouts.";
            case "gridpane" -> "Grid layout that arranges children in rows and columns.\n" +
                              "Ideal for forms, tables, and structured layouts with precise positioning.";
            case "stackpane" -> "Stacked layout where children overlay each other.\n" +
                               "Ideal for layered content, overlays, and centered content.";
            case "borderpane" -> "Layout with five regions: top, bottom, left, right, and center.\n" +
                                "Ideal for application layouts with headers, footers, and sidebars.";
            case "flowpane" -> "Flow layout that wraps children horizontally or vertically.\n" +
                              "Ideal for tags, chips, and dynamic content that needs to wrap.";
            case "tilepane" -> "Tile layout that arranges children in uniform-sized tiles.\n" +
                              "Ideal for image galleries, icon grids, and uniform content.";
            case "anchorpane" -> "Flexible layout where children are positioned using anchors.\n" +
                                "Ideal for custom layouts with precise control over positioning.";
            case "pane" -> "Basic layout pane with no automatic positioning.\n" +
                          "Ideal for custom layouts where you control all positioning.";
            case "scrollpane" -> "Container that adds scrollbars for content larger than the view.\n" +
                                "Ideal for large forms, documents, and scrollable content.";
            case "splitpane" -> "Container with resizable dividers between children.\n" +
                               "Ideal for resizable panels, editors, and multi-view layouts.";
            case "tabpane" -> "Container with tabs for switching between different content panels.\n" +
                             "Ideal for multi-page forms, settings panels, and tabbed interfaces.";
            case "accordion" -> "Container with collapsible titled panels (only one open at a time).\n" +
                               "Ideal for settings, navigation menus, and grouped content.";
            case "titledpane" -> "Container with a collapsible title bar.\n" +
                                "Ideal for collapsible sections, groups, and expandable content.";
            case "group" -> "Logical grouping of controls, typically rendered as a VBox with spacing.\n" +
                           "Ideal for grouping related form fields and controls.";
            case "region" -> "Base layout class for custom containers.\n" +
                            "Ideal for extending with custom layout logic.";
            case "canvas" -> "Drawing surface for custom graphics and rendering.\n" +
                            "Ideal for charts, diagrams, and custom visualizations.";
            default -> "Container for organizing and laying out UI components.";
        };
    }
    
    /**
     * Get supported properties for a specific container type.
     */
    private String getSupportedProperties(String type) {
        StringBuilder props = new StringBuilder();
        
        // Common properties for all containers
        props.append("  • type (string) - Container type name\n");
        props.append("  • spacing (number) - Gap between children (HBox, VBox, FlowPane, GridPane, TilePane)\n");
        props.append("  • padding (string) - Internal spacing around children (all containers)\n");
        props.append("  • alignment (string) - Child alignment position (HBox, VBox, StackPane, FlowPane, TilePane)\n");
        props.append("  • style (string) - Custom CSS styling\n");
        props.append("  • areaBackground (string) - Background color in hex format\n");
        props.append("  • minWidth, prefWidth, maxWidth (string) - Width constraints\n");
        props.append("  • minHeight, prefHeight, maxHeight (string) - Height constraints\n");
        props.append("  • hgrow, vgrow (string) - Growth priority (ALWAYS, SOMETIMES, NEVER)\n");
        
        // Container-specific properties
        switch (type.toLowerCase()) {
            case "gridpane":
                props.append("  • hgap (number) - Horizontal gap between columns\n");
                props.append("  • vgap (number) - Vertical gap between rows\n");
                break;
            case "flowpane", "tilepane":
                props.append("  • hgap (number) - Horizontal gap between items\n");
                props.append("  • vgap (number) - Vertical gap between items\n");
                props.append("  • orientation (string) - HORIZONTAL or VERTICAL\n");
                break;
            case "titledpane":
                props.append("  • title (string) - Title text for the pane\n");
                break;
            case "group":
                props.append("  • groupBorder (string) - Border style (none, line, raised, lowered, inset, outset)\n");
                props.append("  • groupBorderColor (string) - Border color in hex format\n");
                props.append("  • groupBorderWidth (string) - Border width in pixels\n");
                props.append("  • groupBorderRadius (string) - Border corner radius in pixels\n");
                props.append("  • groupLabelText (string) - Label text on the border\n");
                props.append("  • groupLabelAlignment (string) - Label alignment (left, center, right)\n");
                break;
        }
        
        return props.toString();
    }
    
    /**
     * Get a usage example for a specific container type.
     */
    private String getUsageExample(String type) {
        return switch (type.toLowerCase()) {
            case "hbox" -> """
                {
                    "name": "buttonBar",
                    "type": "hbox",
                    "alignment": "right",
                    "spacing": "10",
                    "padding": "10 20",
                    "items": [...]
                }
                """;
            case "vbox" -> """
                {
                    "name": "formPanel",
                    "type": "vbox",
                    "alignment": "top-center",
                    "spacing": "15",
                    "padding": "20",
                    "items": [...]
                }
                """;
            case "gridpane" -> """
                {
                    "name": "dataGrid",
                    "type": "gridpane",
                    "spacing": "10",
                    "padding": "15",
                    "items": [...]
                }
                """;
            case "group" -> """
                {
                    "name": "settings",
                    "type": "group",
                    "groupBorder": "line",
                    "groupBorderColor": "#4a9eff",
                    "groupLabelText": "Settings",
                    "spacing": "8",
                    "items": [...]
                }
                """;
            default -> "See documentation for " + type + " container usage examples.";
        };
    }
    
    /**
     * Get all runtime properties of this container as a formatted string.
     * This includes all settable properties with their current values.
     * @return String description of all properties
     */
    public String getProperties() {
        if (javafxRegion == null) {
            return "Container not initialized";
        }
        
        StringBuilder props = new StringBuilder();
        props.append("Container Properties:\n");
        props.append("  type: ").append(containerType != null ? containerType : "unknown").append("\n");
        props.append("  class: ").append(javafxRegion.getClass().getSimpleName()).append("\n");
        
        // Size properties
        props.append("  width: ").append(String.format("%.2f", javafxRegion.getWidth())).append("\n");
        props.append("  height: ").append(String.format("%.2f", javafxRegion.getHeight())).append("\n");
        props.append("  minWidth: ").append(String.format("%.2f", javafxRegion.getMinWidth())).append("\n");
        props.append("  minHeight: ").append(String.format("%.2f", javafxRegion.getMinHeight())).append("\n");
        props.append("  prefWidth: ").append(String.format("%.2f", javafxRegion.getPrefWidth())).append("\n");
        props.append("  prefHeight: ").append(String.format("%.2f", javafxRegion.getPrefHeight())).append("\n");
        props.append("  maxWidth: ").append(String.format("%.2f", javafxRegion.getMaxWidth())).append("\n");
        props.append("  maxHeight: ").append(String.format("%.2f", javafxRegion.getMaxHeight())).append("\n");
        
        // Position
        props.append("  x: ").append(String.format("%.2f", javafxRegion.getLayoutX())).append("\n");
        props.append("  y: ").append(String.format("%.2f", javafxRegion.getLayoutY())).append("\n");
        
        // Padding
        javafx.geometry.Insets padding = javafxRegion.getPadding();
        if (padding != null) {
            props.append("  padding: ").append(String.format("%.0f %.0f %.0f %.0f", 
                padding.getTop(), padding.getRight(), padding.getBottom(), padding.getLeft())).append("\n");
        }
        
        // Container-specific properties
        if (javafxRegion instanceof HBox hbox) {
            props.append("  spacing: ").append(String.format("%.2f", hbox.getSpacing())).append("\n");
            props.append("  alignment: ").append(hbox.getAlignment()).append("\n");
        } else if (javafxRegion instanceof VBox vbox) {
            props.append("  spacing: ").append(String.format("%.2f", vbox.getSpacing())).append("\n");
            props.append("  alignment: ").append(vbox.getAlignment()).append("\n");
        } else if (javafxRegion instanceof GridPane gridPane) {
            props.append("  hgap: ").append(String.format("%.2f", gridPane.getHgap())).append("\n");
            props.append("  vgap: ").append(String.format("%.2f", gridPane.getVgap())).append("\n");
            props.append("  alignment: ").append(gridPane.getAlignment()).append("\n");
        } else if (javafxRegion instanceof FlowPane flowPane) {
            props.append("  hgap: ").append(String.format("%.2f", flowPane.getHgap())).append("\n");
            props.append("  vgap: ").append(String.format("%.2f", flowPane.getVgap())).append("\n");
            props.append("  alignment: ").append(flowPane.getAlignment()).append("\n");
            props.append("  orientation: ").append(flowPane.getOrientation()).append("\n");
        } else if (javafxRegion instanceof TilePane tilePane) {
            props.append("  hgap: ").append(String.format("%.2f", tilePane.getHgap())).append("\n");
            props.append("  vgap: ").append(String.format("%.2f", tilePane.getVgap())).append("\n");
            props.append("  orientation: ").append(tilePane.getOrientation()).append("\n");
            props.append("  prefColumns: ").append(tilePane.getPrefColumns()).append("\n");
            props.append("  prefRows: ").append(tilePane.getPrefRows()).append("\n");
        }
        
        // Style
        String style = javafxRegion.getStyle();
        if (style != null && !style.isEmpty()) {
            props.append("  style: ").append(style).append("\n");
        }
        
        // Style classes
        if (!javafxRegion.getStyleClass().isEmpty()) {
            props.append("  styleClass: ").append(String.join(", ", javafxRegion.getStyleClass())).append("\n");
        }
        
        // State
        props.append("  visible: ").append(javafxRegion.isVisible()).append("\n");
        props.append("  managed: ").append(javafxRegion.isManaged()).append("\n");
        props.append("  disabled: ").append(javafxRegion.isDisabled()).append("\n");
        
        // ID
        String id = javafxRegion.getId();
        if (id != null && !id.isEmpty()) {
            props.append("  id: ").append(id).append("\n");
        }
        
        return props.toString();
    }
    
    /**
     * Get the parent area name for this container.
     * @return Parent area name, or null if this is a root container
     */
    public String getParent() {
        return parentAreaName;
    }
    
    /**
     * Get the list of child area names.
     * @return ArrayDynamic containing child area names
     */
    public com.eb.script.arrays.ArrayDynamic getChildren(com.eb.script.token.DataType dataType) {
        com.eb.script.arrays.ArrayDynamic children = new com.eb.script.arrays.ArrayDynamic(dataType);
        for (String childName : childAreaNames) {
            children.add(childName);
        }
        return children;
    }
    
    /**
     * Get the full hierarchy tree starting from this container.
     * @return String representation of the hierarchy tree
     */
    public String getTree() {
        return buildTree("", true);
    }
    
    /**
     * Recursively build the tree structure.
     * @param indent Current indentation level
     * @param isLast Whether this is the last child
     * @return Tree structure as a string
     */
    private String buildTree(String indent, boolean isLast) {
        StringBuilder tree = new StringBuilder();
        tree.append(indent);
        tree.append(isLast ? "└── " : "├── ");
        tree.append(areaName != null ? areaName : "unnamed");
        tree.append(" (").append(containerType != null ? containerType : "unknown").append(")\n");
        
        // Note: We can't recursively get children here without context
        // This will be handled in the Interpreter with context access
        for (int i = 0; i < childAreaNames.size(); i++) {
            String childName = childAreaNames.get(i);
            boolean childIsLast = (i == childAreaNames.size() - 1);
            tree.append(indent).append(isLast ? "    " : "│   ");
            tree.append(childIsLast ? "└── " : "├── ");
            tree.append(childName).append("\n");
        }
        
        return tree.toString();
    }
    
    /**
     * Get event handlers attached to this container.
     * @return String description of event handlers
     */
    public String getEvents() {
        if (javafxRegion == null) {
            return "Container not initialized";
        }
        
        StringBuilder events = new StringBuilder();
        events.append("Event Handlers:\n");
        
        // Check for mouse events
        if (javafxRegion.getOnMouseClicked() != null) {
            events.append("  onMouseClicked: registered\n");
        }
        if (javafxRegion.getOnMousePressed() != null) {
            events.append("  onMousePressed: registered\n");
        }
        if (javafxRegion.getOnMouseReleased() != null) {
            events.append("  onMouseReleased: registered\n");
        }
        if (javafxRegion.getOnMouseEntered() != null) {
            events.append("  onMouseEntered: registered\n");
        }
        if (javafxRegion.getOnMouseExited() != null) {
            events.append("  onMouseExited: registered\n");
        }
        
        // Check for key events
        if (javafxRegion.getOnKeyPressed() != null) {
            events.append("  onKeyPressed: registered\n");
        }
        if (javafxRegion.getOnKeyReleased() != null) {
            events.append("  onKeyReleased: registered\n");
        }
        if (javafxRegion.getOnKeyTyped() != null) {
            events.append("  onKeyTyped: registered\n");
        }
        
        // Check for focus events (these are on Node, not Region specifically)
        // We can access them via the focusedProperty
        if (javafxRegion.focusedProperty() != null) {
            events.append("  focus tracking: available\n");
        }
        
        if (events.toString().equals("Event Handlers:\n")) {
            events.append("  No event handlers registered\n");
        }
        
        return events.toString();
    }
    
    /**
     * Capture a snapshot of the container.
     * @return Base64-encoded PNG image data
     */
    public String getSnapshot() {
        if (javafxRegion == null) {
            return "Container not initialized";
        }
        
        try {
            // Must run on JavaFX Application Thread
            final java.util.concurrent.atomic.AtomicReference<String> result = 
                new java.util.concurrent.atomic.AtomicReference<>("");
            
            if (javafx.application.Platform.isFxApplicationThread()) {
                result.set(captureSnapshot());
            } else {
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                javafx.application.Platform.runLater(() -> {
                    result.set(captureSnapshot());
                    latch.countDown();
                });
                latch.await();
            }
            
            return result.get();
        } catch (Exception e) {
            return "Error capturing snapshot: " + e.getMessage();
        }
    }
    
    /**
     * Internal method to capture snapshot (must be called on JavaFX thread).
     */
    private String captureSnapshot() {
        try {
            javafx.scene.image.WritableImage image = javafxRegion.snapshot(null, null);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
            
            // Also copy to clipboard
            javafx.scene.image.Image fxImage = image;
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putImage(fxImage);
            clipboard.setContent(content);
            
            return "Snapshot captured (" + imageBytes.length + " bytes) and copied to clipboard.\nBase64: " + base64.substring(0, Math.min(100, base64.length())) + "...";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @Override
    public String toString() {
        return getFullTypeName() + " [" + (containerType != null ? containerType : "unknown") + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ScreenContainerType that = (ScreenContainerType) obj;
        return containerType != null ? containerType.equals(that.containerType) : that.containerType == null;
    }
    
    @Override
    public int hashCode() {
        return containerType != null ? containerType.hashCode() : 0;
    }
}
