package com.eb.script.interpreter;

import com.eb.script.interpreter.AreaDefinition.AreaItem;
import com.eb.script.interpreter.AreaDefinition.AreaType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Factory class for creating complete JavaFX screens/windows from AreaDefinitions.
 * This factory uses AreaContainerFactory and AreaItemFactory to create a fully assembled screen.
 */
public class ScreenFactory {

    /**
     * Creates a complete JavaFX window/screen from area definitions.
     * This method creates containers, adds items, and applies layout properties.
     *
     * @param screenName The name of the screen
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param areas List of AreaDefinitions containing containers and items
     * @param metadataProvider Function to retrieve DisplayMetadata for variables (screenName, varName) -> metadata
     * @return A Stage representing the complete window
     */
    public static Stage createScreen(String screenName, String title, double width, double height,
                                      List<AreaDefinition> areas,
                                      BiFunction<String, String, DisplayMetadata> metadataProvider) {
        Stage stage = new Stage();
        stage.setTitle(title);

        // Create root container - use VBox as default if multiple areas
        Region rootContainer;
        
        if (areas == null || areas.isEmpty()) {
            // No areas defined, create empty pane
            rootContainer = new Pane();
        } else if (areas.size() == 1) {
            // Single area - use it as root
            rootContainer = createAreaWithItems(areas.get(0), screenName, metadataProvider);
        } else {
            // Multiple areas - arrange in VBox
            VBox root = new VBox();
            root.setSpacing(10);
            
            for (AreaDefinition areaDef : areas) {
                Region areaContainer = createAreaWithItems(areaDef, screenName, metadataProvider);
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
                                               BiFunction<String, String, DisplayMetadata> metadataProvider) {
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
                DisplayMetadata metadata = item.displayMetadata;
                if (metadata == null && item.varRef != null && metadataProvider != null) {
                    metadata = metadataProvider.apply(screenName, item.varRef);
                }

                // Create the item using AreaItemFactory
                Node control = AreaItemFactory.createItem(item, metadata);

                // Apply item layout properties
                applyItemLayoutProperties(control, item);

                // Add item to container based on container type
                addItemToContainer(container, control, item, areaDef.areaType);
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
                                            BiFunction<String, String, DisplayMetadata> metadataProvider,
                                            boolean maximize) {
        Platform.runLater(() -> {
            Stage stage = createScreen(screenName, title, width, height, areas, metadataProvider);
            
            if (maximize) {
                stage.setMaximized(true);
            }
            
            stage.show();
        });
    }
}
