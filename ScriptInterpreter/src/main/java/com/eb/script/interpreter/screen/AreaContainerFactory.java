package com.eb.script.interpreter.screen;

import com.eb.script.interpreter.screen.AreaDefinition.AreaType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Group;

/**
 * Factory class for creating JavaFX container controls from AreaDefinition.
 * This factory creates JavaFX containers based on the AreaType and applies
 * layout properties only (no items added).
 */
public class AreaContainerFactory {

    /**
     * Creates a JavaFX container based on the provided AreaDefinition.
     * Only layout properties are applied (style, cssClass, layout configuration).
     * Items should be added by the caller after creation.
     *
     * @param areaDef The AreaDefinition containing container type and layout properties
     * @return A JavaFX Region/Pane representing the container
     */
    public static Region createContainer(AreaDefinition areaDef) {
        if (areaDef == null) {
            // If no definition provided, create a simple Pane as fallback
            return new Pane();
        }

        AreaType areaType = areaDef.areaType;
        if (areaType == null) {
            areaType = AreaType.PANE; // Default fallback
        }

        Region container = createContainerByType(areaType);
        
        // Apply layout properties
        applyLayoutProperties(container, areaDef);
        
        return container;
    }

    /**
     * Creates the appropriate JavaFX container based on the AreaType.
     */
    private static Region createContainerByType(AreaType areaType) {
        switch (areaType) {
            // Layout Panes
            case PANE:
                return new Pane();
            case STACKPANE:
                return new StackPane();
            case ANCHORPANE:
                return new AnchorPane();
            case BORDERPANE:
                return new BorderPane();
            case FLOWPANE:
                return new FlowPane();
            case GRIDPANE:
                return new GridPane();
            case HBOX:
                return new HBox();
            case VBOX:
                return new VBox();
            case TILEPANE:
                return new TilePane();

            // Containers
            case SCROLLPANE:
                return new ScrollPane();
            case SPLITPANE:
                return new SplitPane();
            case TABPANE:
                return new TabPane();
            case TAB:
                // Tab is not a Region, but we can wrap it in a Pane
                // The caller should handle Tab specially
                return new Pane(); // Placeholder - Tab needs special handling
            case ACCORDION:
                return new Accordion();
            case TITLEDPANE:
                return new TitledPane();

            // Special
            case GROUP:
                // Group is not a Region, wrap in Pane
                return new Pane(); // Placeholder - Group needs special handling
            case REGION:
                return new Region();
            case CANVAS:
                // Canvas is not a Region, wrap in Pane containing Canvas
                Pane canvasPane = new Pane();
                Canvas canvas = new Canvas();
                canvasPane.getChildren().add(canvas);
                return canvasPane;

            // Default fallback
            case CUSTOM:
            default:
                return new Pane();
        }
    }

    /**
     * Applies layout properties from AreaDefinition to the container.
     */
    private static void applyLayoutProperties(Region container, AreaDefinition areaDef) {
        // Apply CSS class from AreaDefinition
        if (areaDef.cssClass != null && !areaDef.cssClass.isEmpty()) {
            container.getStyleClass().add(areaDef.cssClass);
        }

        // Apply default style from AreaType
        if (areaDef.areaType != null && areaDef.areaType.getDefaultStyle() != null) {
            String defaultStyle = areaDef.areaType.getDefaultStyle();
            if (!defaultStyle.isEmpty()) {
                container.setStyle(defaultStyle);
            }
        }

        // Apply spacing property (for containers that support it)
        if (areaDef.spacing != null && !areaDef.spacing.isEmpty()) {
            applySpacing(container, areaDef.spacing);
        }
        
        // Apply padding property (for all Region types)
        if (areaDef.padding != null && !areaDef.padding.isEmpty()) {
            applyPadding(container, areaDef.padding);
        }

        // Apply title property (for TitledPane)
        if (areaDef.title != null && !areaDef.title.isEmpty() && container instanceof TitledPane) {
            ((TitledPane) container).setText(areaDef.title);
        }

        // Apply custom style from AreaDefinition (overrides default)
        if (areaDef.style != null && !areaDef.style.isEmpty()) {
            container.setStyle(container.getStyle() + "; " + areaDef.style);
        }
        
        // Apply groupBorder styling if specified
        if (areaDef.groupBorder != null && !areaDef.groupBorder.isEmpty() && !areaDef.groupBorder.equalsIgnoreCase("none")) {
            String borderStyle = createBorderStyle(areaDef.groupBorder, areaDef.groupBorderColor);
            if (borderStyle != null && !borderStyle.isEmpty()) {
                container.setStyle(container.getStyle() + "; " + borderStyle);
            }
        }

        // Apply layout configuration if provided
        if (areaDef.layout != null && !areaDef.layout.isEmpty()) {
            applyLayoutConfiguration(container, areaDef.layout);
        }
    }
    
    /**
     * Create a CSS border style string based on groupBorder type and color.
     * @param borderType The type of border: none, raised, inset, lowered, line
     * @param borderColor The color of the border in hex format (optional)
     * @return CSS style string for the border, or null if borderType is "none"
     */
    private static String createBorderStyle(String borderType, String borderColor) {
        if (borderType == null || borderType.equalsIgnoreCase("none")) {
            return null;
        }
        
        // Default border color if not specified
        String color = (borderColor != null && !borderColor.isEmpty()) ? borderColor : "#808080";
        
        // Create border style based on type
        switch (borderType.toLowerCase()) {
            case "line":
                return "-fx-border-color: " + color + "; -fx-border-width: 1px; -fx-border-radius: 5px";
            case "raised":
                // Simulate raised effect with gradient
                return "-fx-border-color: derive(" + color + ", 40%) " + color + " " + color + " derive(" + color + ", 40%); " +
                       "-fx-border-width: 2px; -fx-border-style: solid; -fx-border-radius: 5px";
            case "lowered":
            case "inset":
                // Simulate inset/lowered effect with gradient (opposite of raised)
                return "-fx-border-color: " + color + " derive(" + color + ", 40%) derive(" + color + ", 40%) " + color + "; " +
                       "-fx-border-width: 2px; -fx-border-style: solid; -fx-border-radius: 5px";
            default:
                // Default to simple line border
                return "-fx-border-color: " + color + "; -fx-border-width: 1px; -fx-border-radius: 5px";
        }
    }

    /**
     * Applies spacing property to containers that support it.
     * Spacing controls the gap between child elements.
     */
    private static void applySpacing(Region container, String spacingStr) {
        try {
            double spacing = Double.parseDouble(spacingStr.trim());
            
            if (container instanceof HBox) {
                ((HBox) container).setSpacing(spacing);
            } else if (container instanceof VBox) {
                ((VBox) container).setSpacing(spacing);
            } else if (container instanceof FlowPane) {
                ((FlowPane) container).setHgap(spacing);
                ((FlowPane) container).setVgap(spacing);
            } else if (container instanceof TilePane) {
                ((TilePane) container).setHgap(spacing);
                ((TilePane) container).setVgap(spacing);
            } else if (container instanceof GridPane) {
                ((GridPane) container).setHgap(spacing);
                ((GridPane) container).setVgap(spacing);
            }
            // Other container types don't have a direct spacing property
        } catch (NumberFormatException e) {
            // Invalid spacing value - ignore
            System.err.println("Warning: Invalid spacing value '" + spacingStr + "'");
        }
    }
    
    /**
     * Applies padding property to the container.
     * Padding creates internal space around the children within the container.
     * Supports formats: "10" (all sides), "10 5" (vertical horizontal), 
     * "10 5 10 5" (top right bottom left).
     */
    private static void applyPadding(Region container, String paddingStr) {
        javafx.geometry.Insets padding = parseInsets(paddingStr);
        if (padding != null) {
            container.setPadding(padding);
        }
    }
    
    /**
     * Parses insets string to Insets object.
     * Supports formats: "10" (all), "10 5" (vertical horizontal), 
     * "10 5 10 5" (top right bottom left).
     */
    private static javafx.geometry.Insets parseInsets(String insetsStr) {
        if (insetsStr == null || insetsStr.isEmpty()) {
            return null;
        }
        
        String[] parts = insetsStr.trim().split("\\s+");
        
        try {
            if (parts.length == 1) {
                double all = Double.parseDouble(parts[0]);
                return new javafx.geometry.Insets(all);
            } else if (parts.length == 2) {
                double vertical = Double.parseDouble(parts[0]);
                double horizontal = Double.parseDouble(parts[1]);
                return new javafx.geometry.Insets(vertical, horizontal, vertical, horizontal);
            } else if (parts.length == 4) {
                double top = Double.parseDouble(parts[0]);
                double right = Double.parseDouble(parts[1]);
                double bottom = Double.parseDouble(parts[2]);
                double left = Double.parseDouble(parts[3]);
                return new javafx.geometry.Insets(top, right, bottom, left);
            }
        } catch (NumberFormatException e) {
            // Invalid format - return null
            System.err.println("Warning: Invalid padding/insets value '" + insetsStr + "'");
        }
        
        return null;
    }

    /**
     * Applies layout configuration based on the layout string.
     * This method can be extended to handle various layout configurations.
     */
    private static void applyLayoutConfiguration(Region container, String layout) {
        // The layout string can contain various configuration options
        // For now, we'll handle basic cases like "fill"
        
        if ("fill".equalsIgnoreCase(layout)) {
            // Make the container fill available space
            if (container instanceof VBox) {
                VBox.setVgrow(container, Priority.ALWAYS);
            } else if (container instanceof HBox) {
                HBox.setHgrow(container, Priority.ALWAYS);
            }
            // Set preferred size to grow
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
        }
        
        // Additional layout configurations can be added here based on requirements
        // Examples: "center", "stretch", specific dimensions, etc.
    }

    /**
     * Helper method to parse alignment string to JavaFX Pos enum.
     * Used for containers that support alignment like StackPane, HBox, VBox.
     */
    private static Pos parseAlignment(String alignment) {
        if (alignment == null || alignment.isEmpty()) {
            return Pos.CENTER;
        }

        String normalized = alignment.toUpperCase().replace("-", "_").replace(" ", "_");
        
        try {
            return Pos.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // If not a valid Pos value, try to map common variations
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
}
