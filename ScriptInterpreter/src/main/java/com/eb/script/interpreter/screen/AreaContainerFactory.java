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
            container.setStyle(appendStyle(container.getStyle(), areaDef.style));
        }
        
        // Apply areaBackground if specified
        if (areaDef.areaBackground != null && !areaDef.areaBackground.isEmpty()) {
            container.setStyle(appendStyle(container.getStyle(), "-fx-background-color: " + areaDef.areaBackground));
        }
        
        // Apply groupBorder styling if specified
        if (areaDef.groupBorder != null && !areaDef.groupBorder.isEmpty() && !areaDef.groupBorder.equalsIgnoreCase("none")) {
            String borderStyle = createBorderStyle(areaDef.groupBorder, areaDef.groupBorderColor, areaDef.groupBorderWidth, areaDef.groupBorderInsets, areaDef.groupBorderRadius);
            if (borderStyle != null && !borderStyle.isEmpty()) {
                container.setStyle(appendStyle(container.getStyle(), borderStyle));
            }
            
            // Add group label if specified
            if (areaDef.groupLabelText != null && !areaDef.groupLabelText.isEmpty()) {
                addGroupLabel(container, areaDef.groupLabelText, areaDef.groupLabelAlignment, areaDef.groupBorderColor, areaDef.groupLabelOffset, areaDef.groupLabelColor, areaDef.groupLabelBackground, areaDef.groupBorderWidth);
            }
        }

        // Apply layout configuration if provided
        if (areaDef.layout != null && !areaDef.layout.isEmpty()) {
            applyLayoutConfiguration(container, areaDef.layout);
        }
    }
    
    /**
     * Create a CSS border style string based on groupBorder type, color, width, insets, and radius.
     * @param borderType The type of border: none, raised, lowered, inset, outset, line
     * @param borderColor The color of the border in hex format (optional)
     * @param borderWidth The width of the border in pixels (optional, e.g., "2" or "2px")
     * @param borderInsets The insets for the border (optional, e.g., "5" for all sides, "5 10" for top/bottom and left/right, or "5 10 5 10" for top, right, bottom, left)
     * @param borderRadius The radius for the border corners (optional, e.g., "5" or "5px") - default is 5px
     * @return CSS style string for the border, or null if borderType is "none"
     */
    private static String createBorderStyle(String borderType, String borderColor, String borderWidth, String borderInsets, String borderRadius) {
        if (borderType == null || borderType.equalsIgnoreCase("none")) {
            return null;
        }
        
        // Default border color if not specified
        String color = (borderColor != null && !borderColor.isEmpty()) ? borderColor : "#808080";
        
        // Parse border width - handle various formats (e.g., "2", "2px", "2 px")
        String width;
        if (borderWidth != null && !borderWidth.isEmpty()) {
            // Extract numeric value and normalize to px
            String numericValue = borderWidth.replaceAll("[^0-9.]", "").trim();
            if (!numericValue.isEmpty()) {
                width = numericValue + "px";
            } else {
                // Invalid width, use default
                width = borderType.toLowerCase().equals("line") ? "1px" : "2px";
            }
        } else {
            // Default width: 1px for line, 2px for raised/lowered/inset
            width = borderType.toLowerCase().equals("line") ? "1px" : "2px";
        }
        
        // Parse border insets - handle various formats (e.g., "5", "5 10", "5 10 5 10")
        String insets = "";
        if (borderInsets != null && !borderInsets.isEmpty()) {
            // Split on whitespace and normalize each value to px
            String[] parts = borderInsets.trim().split("\\s+");
            StringBuilder insetsBuilder = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String numericValue = parts[i].replaceAll("[^0-9.]", "").trim();
                if (!numericValue.isEmpty()) {
                    if (insetsBuilder.length() > 0) {
                        insetsBuilder.append(" ");
                    }
                    insetsBuilder.append(numericValue).append("px");
                }
            }
            if (insetsBuilder.length() > 0) {
                insets = "; -fx-border-insets: " + insetsBuilder.toString();
            }
        }
        
        // Parse border radius - handle various formats (e.g., "5", "5px")
        String radius;
        if (borderRadius != null && !borderRadius.isEmpty()) {
            String numericValue = borderRadius.replaceAll("[^0-9.]", "").trim();
            if (!numericValue.isEmpty()) {
                radius = numericValue + "px";
            } else {
                radius = "5px"; // default radius
            }
        } else {
            radius = "5px"; // default radius
        }
        
        // Create border style based on type
        switch (borderType.toLowerCase()) {
            case "line":
                return "-fx-border-color: " + color + "; -fx-border-width: " + width + "; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
            case "raised":
                // Simulate raised effect: brighter border on top/left, darker shadow on bottom/right
                return "-fx-border-color: derive(" + color + ", 60%) derive(" + color + ", -40%) derive(" + color + ", -40%) derive(" + color + ", 60%); " +
                       "-fx-border-width: " + width + "; -fx-border-style: solid; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
            case "lowered":
                // Simulate lowered effect: darker shadow on top/left, brighter border on bottom/right
                return "-fx-border-color: derive(" + color + ", -40%) derive(" + color + ", 60%) derive(" + color + ", 60%) derive(" + color + ", -40%); " +
                       "-fx-border-width: " + width + "; -fx-border-style: solid; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
            case "inset":
                // Inset effect: use JavaFX built-in inset border style
                // Per user's example: -fx-border-color for sides, -fx-border-style: inset
                return "-fx-border-color: black " + color + " " + color + " black; " +
                       "-fx-border-width: " + width + "; -fx-border-style: solid; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
            case "outset":
                // Outset effect: use JavaFX built-in outset border style
                // Per user's example: -fx-border-color for sides, -fx-border-style: outset
                return "-fx-border-color: " + color + " black black " + color + "; " +
                       "-fx-border-width: " + width + "; -fx-border-style: solid; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
            default:
                // Default to simple line border
                return "-fx-border-color: " + color + "; -fx-border-width: " + width + "; -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";" + insets;
        }
    }
    
    /**
     * Adds a group label to a container. The label is positioned relative to the border
     * with the specified alignment and offset. Also adjusts container padding based on offset.
     * @param container The container to add the label to
     * @param labelText The text for the label
     * @param alignment The alignment: left, center, right (default: left)
     * @param borderColor The border color to use for label styling (optional, used as fallback for labelColor)
     * @param offset The vertical offset: top, on, bottom (default: on)
     * @param labelColor The text color for the label (optional, defaults to borderColor or #808080)
     * @param labelBackground The background color for the label (optional, defaults to white)
     * @param borderWidth The border width for offset calculations (optional, defaults to 2px)
     */
    private static void addGroupLabel(Region container, String labelText, String alignment, String borderColor, String offset, String labelColor, String labelBackground, String borderWidth) {
        // Create a label with the group text
        Label label = new Label(labelText);
        
        // Determine label text color: use labelColor if provided, else borderColor, else default gray
        String textColor;
        if (labelColor != null && !labelColor.isEmpty()) {
            textColor = labelColor;
        } else if (borderColor != null && !borderColor.isEmpty()) {
            textColor = borderColor;
        } else {
            textColor = "#808080";
        }
        
        // Determine label background color: use labelBackground if provided, else default white
        String bgColor = (labelBackground != null && !labelBackground.isEmpty()) ? labelBackground : "white";
        
        label.setStyle("-fx-text-fill: " + textColor + "; " +
                      "-fx-font-weight: bold; " +
                      "-fx-padding: 0 5 0 5; " +
                      "-fx-background-color: " + bgColor + ";");
        
        // Position label based on alignment
        String alignmentValue = (alignment != null) ? alignment.toLowerCase() : "left";
        
        // Determine vertical offset based on offset parameter
        String offsetValue = (offset != null) ? offset.toLowerCase() : "on";
        double translateY = getVerticalOffset(offsetValue, label, borderWidth);
        
        // Adjust container padding based on offset to prevent unnecessary space
        adjustPaddingForLabelOffset(container, offsetValue);
        
        // Add the label to the container
        // For VBox/HBox, insert at the beginning with proper alignment
        if (container instanceof VBox) {
            HBox labelWrapper = createAlignedLabelWrapper(label, alignmentValue, translateY);
            ((VBox) container).getChildren().add(0, labelWrapper);
        } else if (container instanceof HBox) {
            HBox labelWrapper = createAlignedLabelWrapper(label, alignmentValue, translateY);
            ((HBox) container).getChildren().add(0, labelWrapper);
        } else if (container instanceof Pane) {
            // For Pane, use translateX to position the label
            label.setTranslateY(translateY);
            switch (alignmentValue) {
                case "center":
                    label.setTranslateX(0);
                    label.setAlignment(Pos.CENTER);
                    break;
                case "right":
                    label.setTranslateX(-10);
                    label.setAlignment(Pos.CENTER_RIGHT);
                    break;
                case "left":
                default:
                    label.setTranslateX(10);
                    label.setAlignment(Pos.CENTER_LEFT);
                    break;
            }
            ((Pane) container).getChildren().add(label);
        } else if (container instanceof StackPane) {
            label.setTranslateY(translateY);
            ((StackPane) container).getChildren().add(label);
            StackPane.setAlignment(label, 
                alignmentValue.equals("center") ? Pos.TOP_CENTER :
                alignmentValue.equals("right") ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        }
    }
    
    /**
     * Adjusts container padding based on label offset to prevent unnecessary vertical space.
     * Modifies the container's style directly to ensure padding takes effect even if
     * style properties are set.
     * For 'top' offset: adds top padding for space between border and label above.
     * For 'on' offset: no extra padding since label sits on border.
     * For 'bottom' offset: adds bottom padding for space between border and label below.
     * @param container The container to adjust padding for
     * @param offset The label offset value: "top", "on", or "bottom"
     */
    private static void adjustPaddingForLabelOffset(Region container, String offset) {
        javafx.geometry.Insets currentPadding = container.getPadding();
        if (currentPadding == null) {
            currentPadding = new javafx.geometry.Insets(0);
        }
        
        double topPadding = currentPadding.getTop();
        double bottomPadding = currentPadding.getBottom();
        
        // Extract base offset type (ignore any +/- adjustments for padding calculation)
        String baseOffset = offset;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(top|bottom|on)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(offset);
        if (matcher.find()) {
            baseOffset = matcher.group(1).toLowerCase();
        }
        
        switch (baseOffset) {
            case "top":
                // For top offset, add extra top padding to create space between border and label above
                topPadding = Math.max(currentPadding.getTop(), 20);
                break;
            case "on":
                // For on offset, no extra padding - label sits on border, content can be close
                // Keep original padding or use minimal if none set
                topPadding = Math.max(currentPadding.getTop(), 2);
                break;
            case "bottom":
                // For bottom offset, add extra bottom padding to create space between content and label below
                bottomPadding = Math.max(currentPadding.getBottom(), 20);
                break;
            default:
                return; // No adjustment for unknown offsets
        }
        
        // Remove any existing -fx-padding declarations from the style string
        String currentStyle = container.getStyle();
        if (currentStyle == null) {
            currentStyle = "";
        }
        
        // Remove existing -fx-padding declarations (case-insensitive)
        currentStyle = currentStyle.replaceAll("(?i)-fx-padding\\s*:\\s*[^;]+;?", "");
        
        // Apply new padding via style to ensure it takes effect
        String paddingStyle = String.format("-fx-padding: %.0fpx %.0fpx %.0fpx %.0fpx",
            topPadding,
            currentPadding.getRight(),
            bottomPadding,
            currentPadding.getLeft());
        
        container.setStyle(appendStyle(currentStyle, paddingStyle));
    }
    
    /**
     * Determines the vertical offset (translateY) value based on the offset parameter.
     * More negative values move the label up, less negative values move it down.
     * All offsets now use dynamic font height calculation for consistent positioning.
     * Supports additional pixel adjustments with format: "top-3", "bottom+3", etc.
     * @param offset The offset value: "top", "on", "bottom" optionally followed by +/- adjustment
     * @param label The label to calculate font height from (for dynamic positioning)
     * @param borderWidth The border width for offset calculations (optional, defaults to 2px)
     * @return The translateY value in pixels
     */
    private static double getVerticalOffset(String offset, Label label, String borderWidth) {
        // Calculate font height from the label
        javafx.scene.text.Font font = label.getFont();
        double fontHeight = font.getSize(); // Approximate font height
        
        // Parse border width to get half border width
        double halfBorderWidth = 1.0; // Default half of 2px
        if (borderWidth != null && !borderWidth.isEmpty()) {
            String numericValue = borderWidth.replaceAll("[^0-9.]", "").trim();
            if (!numericValue.isEmpty()) {
                try {
                    halfBorderWidth = Double.parseDouble(numericValue) / 2.0;
                } catch (NumberFormatException e) {
                    halfBorderWidth = 1.0; // Default to half of 2px
                }
            }
        }
        
        // Parse offset to extract base type and optional pixel adjustment
        // Format: "top", "bottom", "on" or "top-3", "bottom+3", "on+5", etc.
        String baseOffset = offset;
        double extraAdjustment = 0.0;
        
        // Check for adjustment pattern: base type followed by + or - and a number
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(top|bottom|on)([+-]\\d+(?:\\.\\d+)?)?$", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(offset);
        if (matcher.matches()) {
            baseOffset = matcher.group(1).toLowerCase();
            String adjustmentStr = matcher.group(2);
            if (adjustmentStr != null && !adjustmentStr.isEmpty()) {
                try {
                    extraAdjustment = Double.parseDouble(adjustmentStr);
                } catch (NumberFormatException e) {
                    extraAdjustment = 0.0;
                }
            }
        }
        
        double baseY;
        switch (baseOffset) {
            case "top":
                // Position above the border - subtract half border width
                // Extra -9 to move up 9 more pixels from original top position
                baseY = -11 - fontHeight - halfBorderWidth - 9;
                break;
            case "bottom":
                // Position below the border - add half border width
                // Extra +6 to move down 6 more pixels from original bottom position
                baseY = -6 + halfBorderWidth + 6;
                break;
            case "on":
            default:
                // Default: border goes through label - uses half font height
                // Extra +3 to move down 3 more pixels from original on position
                baseY = -8 - (fontHeight / 2) + 3;
                break;
        }
        
        // Apply extra adjustment (negative moves up, positive moves down)
        return baseY + extraAdjustment;
    }
    
    /**
     * Creates an HBox wrapper for a label with the specified alignment and vertical offset.
     * This wrapper allows proper horizontal alignment of labels within VBox/HBox containers.
     * @param label The label to wrap
     * @param alignmentValue The alignment value: "left", "center", or "right"
     * @param translateY The vertical offset in pixels
     * @return An HBox containing the label with proper alignment
     */
    private static HBox createAlignedLabelWrapper(Label label, String alignmentValue, double translateY) {
        HBox labelWrapper = new HBox(label);
        labelWrapper.setTranslateY(translateY);
        
        // Set the alignment of the wrapper HBox based on alignment value
        Pos wrapperAlignment;
        switch (alignmentValue) {
            case "center":
                wrapperAlignment = Pos.CENTER;
                break;
            case "right":
                wrapperAlignment = Pos.CENTER_RIGHT;
                break;
            case "left":
            default:
                wrapperAlignment = Pos.CENTER_LEFT;
                break;
        }
        labelWrapper.setAlignment(wrapperAlignment);
        
        return labelWrapper;
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
    
    /**
     * Helper method to safely append CSS style strings.
     * Handles null values and avoids double semicolons.
     * @param currentStyle The current style string (may be null or empty)
     * @param newStyle The new style to append (may be null or empty)
     * @return The combined style string with proper semicolon separation
     */
    private static String appendStyle(String currentStyle, String newStyle) {
        if (newStyle == null || newStyle.trim().isEmpty()) {
            return currentStyle == null ? "" : currentStyle;
        }
        if (currentStyle == null || currentStyle.trim().isEmpty()) {
            return newStyle;
        }
        // Clean up both strings - remove trailing/leading semicolons and whitespace
        String cleanCurrent = currentStyle.trim();
        String cleanNew = newStyle.trim();
        
        // Remove trailing semicolons from current
        while (cleanCurrent.endsWith(";")) {
            cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - 1).trim();
        }
        
        // Remove leading semicolons from new
        while (cleanNew.startsWith(";")) {
            cleanNew = cleanNew.substring(1).trim();
        }
        
        if (cleanCurrent.isEmpty()) {
            return cleanNew;
        }
        if (cleanNew.isEmpty()) {
            return cleanCurrent;
        }
        
        return cleanCurrent + "; " + cleanNew;
    }
}
