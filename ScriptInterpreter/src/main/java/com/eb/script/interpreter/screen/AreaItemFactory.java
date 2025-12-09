package com.eb.script.interpreter.screen;

import com.eb.script.interpreter.screen.DisplayItem.ItemType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.web.WebView;

/**
 * Factory class for creating JavaFX UI controls from AreaItem definitions.
 * This factory creates JavaFX controls based on the DisplayItem and applies
 * display properties only (no layout properties).
 */
public class AreaItemFactory {

    /**
     * Creates a JavaFX control based on the provided AreaItem and DisplayItem.
     * Only display properties are applied (promptHelp, editable, disabled, visible, tooltip, colors, style).
     * Layout properties should be applied by the caller after creation.
     *
     * @param item The AreaItem containing display properties
     * @param metadata The DisplayItem containing the control type and styling
     * @return A JavaFX Node representing the control
     */
    public static Node createItem(AreaItem item, DisplayItem metadata) {
        if (metadata == null) {
            // If no metadata provided, create a simple Label as fallback
            Label label = new Label("No metadata");
            applyCommonProperties(label, item);
            return label;
        }

        ItemType itemType = metadata.itemType;
        if (itemType == null) {
            itemType = ItemType.TEXTFIELD; // Default fallback
        }

        Node control = createControlByType(itemType, metadata);
        
        // Apply common display properties
        applyCommonProperties(control, item);
        
        // Apply metadata-specific properties
        applyMetadataProperties(control, metadata);
        
        // Apply item-specific display properties (including promptHelp from metadata)
        applyItemSpecificProperties(control, item, metadata);
        
        // Apply control size and font styling
        applyControlSizeAndFont(control, metadata, item);
        
        // If this is a slider and showSliderValue is true, wrap it with a value label
        if (itemType == ItemType.SLIDER && metadata.showSliderValue != null && metadata.showSliderValue) {
            return createSliderWithValueLabel(control, metadata);
        }
        
        return control;
    }

    /**
     * Creates the appropriate JavaFX control based on the ItemType.
     */
    private static Node createControlByType(ItemType itemType, DisplayItem metadata) {
        switch (itemType) {
            // Text Input Controls
            case TEXTFIELD:
                return new TextField();
            case TEXTAREA:
                return new TextArea();
            case PASSWORDFIELD:
                return new PasswordField();

            // Selection Controls
            case CHECKBOX:
                return new CheckBox();
            case RADIOBUTTON:
                return new RadioButton();
            case TOGGLEBUTTON:
                return new ToggleButton();
            case COMBOBOX:
                ComboBox<String> comboBox = new ComboBox<>();
                // Populate with options if available
                // If optionsMap is present, use the values (display text) for display
                if (metadata != null && metadata.optionsMap != null && !metadata.optionsMap.isEmpty()) {
                    comboBox.getItems().addAll(metadata.optionsMap.values());
                    // Store the optionsMap in the control's properties for value binding
                    comboBox.getProperties().put("optionsMap", metadata.optionsMap);
                } else if (metadata != null && metadata.options != null && !metadata.options.isEmpty()) {
                    comboBox.getItems().addAll(metadata.options);
                }
                return comboBox;
            case CHOICEBOX:
                ChoiceBox<String> choiceBox = new ChoiceBox<>();
                // Populate with options if available
                // If optionsMap is present, use the values (display text) for display
                if (metadata != null && metadata.optionsMap != null && !metadata.optionsMap.isEmpty()) {
                    choiceBox.getItems().addAll(metadata.optionsMap.values());
                    // Store the optionsMap in the control's properties for value binding
                    choiceBox.getProperties().put("optionsMap", metadata.optionsMap);
                } else if (metadata != null && metadata.options != null && !metadata.options.isEmpty()) {
                    choiceBox.getItems().addAll(metadata.options);
                }
                return choiceBox;
            case LISTVIEW:
                return new ListView<>();
             case TABLEVIEW:
                TableView<java.util.Map<String, Object>> tableView = new TableView<>();
                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                tableView.setPlaceholder(new javafx.scene.control.Label("No data available"));
                
                // Calculate height based on displayRecords if specified
                if (metadata != null && metadata.displayRecords != null && metadata.displayRecords > 0) {
                    // Row height is approximately 25-30 pixels, header is ~30 pixels
                    // Use 28 pixels per row + 32 for header + 2 for borders
                    double calculatedHeight = (metadata.displayRecords * 28.0) + 34.0;
                    // When displayRecords is specified, fix the height to show exactly that many records
                    tableView.setPrefHeight(calculatedHeight);
                    tableView.setMinHeight(calculatedHeight);
                    tableView.setMaxHeight(calculatedHeight);
                }
                
                // Create columns based on metadata
                if (metadata != null && metadata.columns != null && !metadata.columns.isEmpty()) {
                    for (DisplayItem.TableColumn colDef : metadata.columns) {
                        TableColumn<java.util.Map<String, Object>, String> column = 
                            new TableColumn<>(colDef.name != null ? colDef.name : colDef.field);
                        
                        // Set cell value factory to extract field from the Map
                        String fieldName = colDef.field;
                        column.setCellValueFactory(cellData -> {
                            java.util.Map<String, Object> row = cellData.getValue();
                            Object value = row.get(fieldName);
                            return new javafx.beans.property.SimpleStringProperty(
                                value != null ? String.valueOf(value) : ""
                            );
                        });
                        
                        // Set column width if specified, otherwise let it auto-calculate
                        if (colDef.width != null && colDef.width > 0) {
                            column.setPrefWidth(colDef.width);
                        } else {
                            // Calculate minimum width based on column name
                            // Approximate 8 pixels per character + 20 for padding
                            String headerText = colDef.name != null ? colDef.name : colDef.field;
                            if (headerText != null) {
                                double minWidth = (headerText.length() * 8.0) + 20.0;
                                column.setMinWidth(Math.max(minWidth, 60.0)); // At least 60 pixels
                            }
                        }
                        
                        // Set alignment if specified
                        if (colDef.alignment != null) {
                            // Convert alignment to JavaFX CSS format
                            String cssAlignment;
                            switch (colDef.alignment.toLowerCase()) {
                                case "left":
                                    cssAlignment = "CENTER_LEFT";
                                    break;
                                case "right":
                                    cssAlignment = "CENTER_RIGHT";
                                    break;
                                case "center":
                                    cssAlignment = "CENTER";
                                    break;
                                default:
                                    cssAlignment = "CENTER_LEFT";
                            }
                            column.setStyle("-fx-alignment: " + cssAlignment + ";");
                        }
                        
                        tableView.getColumns().add(column);
                    }
                }
                
                return tableView;
                
            case TREEVIEW:
                TreeView<String> treeView = new TreeView<>();
                
                // Set the root node if treeItems are specified
                if (metadata != null && metadata.treeItems != null && !metadata.treeItems.isEmpty()) {
                    // Determine if we should show the root node
                    // Default is true if not specified
                    boolean showRootValue = metadata.showRoot == null ? true : metadata.showRoot;
                    
                    TreeItem<String> rootItem;
                    
                    if (metadata.treeItems.size() == 1) {
                        // Single root item - use it as the actual root
                        DisplayItem.TreeItemDef rootDef = metadata.treeItems.get(0);
                        rootItem = createTreeItem(rootDef);
                        treeView.setShowRoot(showRootValue);
                    } else {
                        // Multiple root items - create a hidden container root
                        // In this case, we always hide the synthetic container root
                        rootItem = new TreeItem<>("Root");
                        for (DisplayItem.TreeItemDef itemDef : metadata.treeItems) {
                            rootItem.getChildren().add(createTreeItem(itemDef));
                        }
                        rootItem.setExpanded(true);
                        // For multiple roots, the container is always hidden
                        treeView.setShowRoot(false);
                    }
                    
                    treeView.setRoot(rootItem);
                    
                    // Apply expandAll if specified
                    if (metadata.expandAll != null && metadata.expandAll) {
                        expandAllNodes(rootItem);
                    }
                } else {
                    // Create empty root as placeholder
                    TreeItem<String> emptyRoot = new TreeItem<>("No items");
                    treeView.setRoot(emptyRoot);
                }
                
                // Calculate height based on displayRecords if specified
                if (metadata != null && metadata.displayRecords != null && metadata.displayRecords > 0) {
                    // Row height is approximately 24 pixels
                    double calculatedHeight = (metadata.displayRecords * 24.0) + 4.0;
                    treeView.setPrefHeight(calculatedHeight);
                    treeView.setMinHeight(calculatedHeight);
                    treeView.setMaxHeight(calculatedHeight);
                }
                
                return treeView;

            // Numeric Controls
            case SPINNER:
                // Create Spinner with proper ValueFactory
                Spinner<Integer> spinner = new Spinner<>();
                int min = metadata != null && metadata.min instanceof Number ? ((Number) metadata.min).intValue() : 0;
                int max = metadata != null && metadata.max instanceof Number ? ((Number) metadata.max).intValue() : 100;
                int initial = min; // Start at minimum value
                SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
                spinner.setValueFactory(valueFactory);
                spinner.setEditable(true);
                return spinner;
            case SLIDER:
                Slider slider = new Slider();
                // Set min/max if provided in metadata
                if (metadata != null) {
                    if (metadata.min instanceof Number) {
                        slider.setMin(((Number) metadata.min).doubleValue());
                    }
                    if (metadata.max instanceof Number) {
                        slider.setMax(((Number) metadata.max).doubleValue());
                    }
                }
                return slider;

            // Date/Time Controls
            case DATEPICKER:
                return new DatePicker();

            // Color Control
            case COLORPICKER:
                return new ColorPicker();

            // Button Controls
            case BUTTON:
                return new Button();

            // Display-Only Controls
            case LABEL:
            case LABELTEXT:
                return new Label();
            case TEXT:
                return new Text();
            case HYPERLINK:
                return new Hyperlink();
            case SEPARATOR:
                return new Separator();

            // Media/Display Controls
            case IMAGEVIEW:
                ImageView imageView = new ImageView();
                // Wrap ImageView in StackPane to support background colors
                // ImageView itself doesn't support -fx-background-color
                StackPane imageContainer = new StackPane(imageView);
                imageContainer.setAlignment(Pos.CENTER);
                
                // Apply image display properties from metadata
                if (metadata != null) {
                    // Set fit dimensions for ImageView only
                    if (metadata.fitWidth != null && metadata.fitWidth > 0) {
                        imageView.setFitWidth(metadata.fitWidth);
                    }
                    if (metadata.fitHeight != null && metadata.fitHeight > 0) {
                        imageView.setFitHeight(metadata.fitHeight);
                    }
                    // Set preserve ratio (default true)
                    imageView.setPreserveRatio(metadata.preserveRatio == null || metadata.preserveRatio);
                    // Set smooth scaling (default true)
                    imageView.setSmooth(metadata.smooth == null || metadata.smooth);
                }
                
                // Prevent StackPane from growing beyond its content
                // This fixes GridPane layout issues where StackPane takes extra horizontal space
                imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                
                return imageContainer;
            case CANVASVIEW:
                // Create a JavaFX Canvas for drawing
                Canvas canvas = new Canvas();
                // Wrap Canvas in StackPane for consistent layout handling
                StackPane canvasContainer = new StackPane(canvas);
                canvasContainer.setAlignment(Pos.CENTER);
                
                // Apply canvas display properties from metadata
                if (metadata != null) {
                    // Set canvas dimensions (default 400x400 if not specified)
                    double canvasWidth = (metadata.fitWidth != null && metadata.fitWidth > 0) ? metadata.fitWidth : 400;
                    double canvasHeight = (metadata.fitHeight != null && metadata.fitHeight > 0) ? metadata.fitHeight : 400;
                    canvas.setWidth(canvasWidth);
                    canvas.setHeight(canvasHeight);
                }
                
                // Prevent StackPane from growing beyond its content
                canvasContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                
                return canvasContainer;
            case MEDIAVIEW:
                // MediaView requires javafx-media module which is not included
                // Return a label placeholder instead
                return new Label("[MediaView - javafx-media module required]");
            case WEBVIEW:
                // Create WebView for displaying HTML content
                return new WebView();
            case CHART:
                // Create a default bar chart as placeholder
                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                return new BarChart<>(xAxis, yAxis);

            // Progress/Status Controls
            case PROGRESSBAR:
                return new ProgressBar();
            case PROGRESSINDICATOR:
                return new ProgressIndicator();

            // Custom/fallback
            case CUSTOM:
            default:
                return new Label("Custom");
        }
    }

    /**
     * Applies common properties that can be set on most controls.
     */
    private static void applyCommonProperties(Node control, AreaItem item) {
        // Apply visibility
        if (item.visible != null) {
            control.setVisible(item.visible);
        }

        // Apply disabled state
        if (item.disabled != null) {
            control.setDisable(item.disabled);
        }

        // Apply tooltip
        if (item.tooltip != null && !item.tooltip.isEmpty()) {
            if (control instanceof Control) {
                ((Control) control).setTooltip(new Tooltip(item.tooltip));
            }
        }

        // Apply CSS class and inline styles will be handled after
    }

    /**
     * Applies properties from DisplayItem to the control.
     */
    private static void applyMetadataProperties(Node control, DisplayItem metadata) {
        // Apply CSS class from metadata
        if (metadata.cssClass != null && !metadata.cssClass.isEmpty()) {
            control.getStyleClass().add(metadata.cssClass);
        }

        // Apply default style from ItemType
        if (metadata.itemType != null && metadata.itemType.getDefaultStyle() != null) {
            String defaultStyle = metadata.itemType.getDefaultStyle();
            if (!defaultStyle.isEmpty()) {
                control.setStyle(defaultStyle);
            }
        }

        // Apply custom style from metadata (overrides default)
        if (metadata.style != null && !metadata.style.isEmpty()) {
            control.setStyle(control.getStyle() + "; " + metadata.style);
        }
    }

    /**
     * Applies item-specific display properties to the control.
     */
    private static void applyItemSpecificProperties(Node control, AreaItem item, DisplayItem metadata) {
        // Apply prompt text for input controls (placeholder hint text)
        if (metadata != null && metadata.promptHelp != null && !metadata.promptHelp.isEmpty()) {
            if (control instanceof TextField) {
                ((TextField) control).setPromptText(metadata.promptHelp);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPromptText(metadata.promptHelp);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPromptText(metadata.promptHelp);
            }
        }
        
        // Apply label text for labels and buttons (actual displayed text)
        if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
            if (control instanceof Label) {
                Label label = (Label) control;
                label.setText(metadata.labelText);
                
                // Apply label text styling (color, bold, italic)
                applyPromptTextStyling(label, metadata);
            } else if (control instanceof Button) {
                ((Button) control).setText(metadata.labelText);
            }
        }

        // Apply control text alignment (for the content inside the control)
        if (metadata != null && metadata.alignment != null && !metadata.alignment.isEmpty()) {
            String alignment = metadata.alignment.toLowerCase();
            String alignmentStyle = "";
            
            switch (alignment) {
                case "l":
                case "left":
                    alignmentStyle = "-fx-alignment: center-left;";
                    break;
                case "c":
                case "center":
                    alignmentStyle = "-fx-alignment: center;";
                    break;
                case "r":
                case "right":
                    alignmentStyle = "-fx-alignment: center-right;";
                    break;
            }
            
            if (!alignmentStyle.isEmpty()) {
                if (control instanceof TextField || control instanceof TextArea || control instanceof ComboBox) {
                    control.setStyle(control.getStyle() + " " + alignmentStyle);
                } else if (control instanceof Spinner) {
                    // For Spinner, we need to access the internal TextField
                    Spinner<?> spinner = (Spinner<?>) control;
                    spinner.setStyle(control.getStyle() + " " + alignmentStyle);
                    // Also try to set on the editor if accessible
                    if (spinner.getEditor() != null) {
                        spinner.getEditor().setStyle(alignmentStyle);
                    }
                }
            }
        }

        // Apply label text alignment (this is for the wrapper label, not the control content)
        // This is handled separately and used by ScreenFactory when creating labeled controls
        if (item.displayItem != null && item.displayItem.labelTextAlignment != null && !item.displayItem.labelTextAlignment.isEmpty()) {
            // This alignment is used by ScreenFactory for the label wrapper
            // No need to apply it to the control itself here
        }

        // Apply editable property
        if (item.editable != null) {
            if (control instanceof TextField) {
                ((TextField) control).setEditable(item.editable);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setEditable(item.editable);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setEditable(item.editable);
            }
        }

        // Apply text color
        if (item.textColor != null && !item.textColor.isEmpty()) {
            String currentStyle = control.getStyle();
            String colorStyle = "-fx-text-fill: " + item.textColor + ";";
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(colorStyle);
            } else {
                control.setStyle(currentStyle + " " + colorStyle);
            }
        }

        // Apply background color
        if (item.backgroundColor != null && !item.backgroundColor.isEmpty()) {
            String currentStyle = control.getStyle();
            String bgStyle = "-fx-background-color: " + item.backgroundColor + ";";
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(bgStyle);
            } else {
                control.setStyle(currentStyle + " " + bgStyle);
            }
        }
    }

    /**
     * Applies control size and font styling based on metadata.
     * Calculates preferred width based on maxLength or data type, considering font size.
     */
    private static void applyControlSizeAndFont(Node control, DisplayItem metadata, AreaItem item) {
        if (metadata == null) {
//            System.out.println("[DEBUG] applyControlSizeAndFont: metadata is null, returning");
            return;
        }
        
//        System.out.println("[DEBUG] applyControlSizeAndFont called for control: " + control.getClass().getSimpleName());
//        System.out.println("[DEBUG]   itemFontSize: " + metadata.itemFontSize);
//        System.out.println("[DEBUG]   itemColor: " + metadata.itemColor);
//        System.out.println("[DEBUG]   itemBold: " + metadata.itemBold);
//        System.out.println("[DEBUG]   itemItalic: " + metadata.itemItalic);
        
        // Build comprehensive style string for item
        StringBuilder itemStyle = new StringBuilder();
        
        // Apply item font size
        if (metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            itemStyle.append("-fx-font-size: ").append(metadata.itemFontSize).append("; ");
            //System.out.println("[DEBUG]   Adding font-size: " + metadata.itemFontSize);
        }
        
        // Apply item text color
        if (metadata.itemColor != null && !metadata.itemColor.isEmpty()) {
            itemStyle.append("-fx-text-fill: ").append(metadata.itemColor).append("; ");
            //System.out.println("[DEBUG]   Adding text-fill: " + metadata.itemColor);
        }
        
        // Apply item bold
        if (metadata.itemBold != null && metadata.itemBold) {
            itemStyle.append("-fx-font-weight: bold; ");
            //System.out.println("[DEBUG]   Adding font-weight: bold");
        }
        
        // Apply item italic
        if (metadata.itemItalic != null && metadata.itemItalic) {
            itemStyle.append("-fx-font-style: italic; ");
            //System.out.println("[DEBUG]   Adding font-style: italic");
        }
        
        //System.out.println("[DEBUG]   Built style string: '" + itemStyle.toString() + "'");
        
        // Apply the combined style to the control
        if (itemStyle.length() > 0) {
            String currentStyle = control.getStyle();
            //System.out.println("[DEBUG]   Current style before applying: '" + currentStyle + "'");
            if (currentStyle == null || currentStyle.isEmpty()) {
                control.setStyle(itemStyle.toString());
            } else {
                control.setStyle(currentStyle + " " + itemStyle.toString());
            }
            //System.out.println("[DEBUG]   Final style after applying: '" + control.getStyle() + "'");
        } else {
            //System.out.println("[DEBUG]   No styles to apply (itemStyle is empty)");
        }
        
        // Calculate and apply preferred width based on maxLength or data type
        double prefWidth = calculateControlWidth(metadata, item);
        if (prefWidth > 0) {
            if (control instanceof TextField) {
                ((TextField) control).setPrefWidth(prefWidth);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPrefWidth(prefWidth);
            } else if (control instanceof PasswordField) {
                ((PasswordField) control).setPrefWidth(prefWidth);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof ChoiceBox) {
                ((ChoiceBox<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof Spinner) {
                ((Spinner<?>) control).setPrefWidth(prefWidth);
            } else if (control instanceof DatePicker) {
                ((DatePicker) control).setPrefWidth(prefWidth);
            } else if (control instanceof ColorPicker) {
                ((ColorPicker) control).setPrefWidth(prefWidth);
            }
        }
        
        // Calculate and apply preferred height for TextArea based on height property
        if (control instanceof TextArea && metadata.height != null && metadata.height > 0) {
            double prefHeight = calculateTextAreaHeight(metadata);
            if (prefHeight > 0) {
                ((TextArea) control).setPrefHeight(prefHeight);
            }
        }
    }
    
    /**
     * Calculates the preferred height for a TextArea based on the height property (number of lines)
     * and the font size. The pixel height is calculated as: fontSize * height (number of lines).
     * 
     * @param metadata The DisplayItem metadata containing height and font size
     * @return The calculated height in pixels, or -1 if height is not specified
     */
    private static double calculateTextAreaHeight(DisplayItem metadata) {
        if (metadata == null || metadata.height == null || metadata.height <= 0) {
            return -1; // Use default
        }
        
        // Default font size is 13px (as per ItemType.TEXTAREA default style)
        double fontSize = 13.0;
        
        // Parse font size if specified
        if (metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            String fontSizeStr = metadata.itemFontSize.trim().toLowerCase();
            try {
                if (fontSizeStr.endsWith("px")) {
                    fontSize = Double.parseDouble(fontSizeStr.substring(0, fontSizeStr.length() - 2).trim());
                } else if (fontSizeStr.endsWith("pt")) {
                    // Convert points to pixels (approximate: 1pt = 1.33px)
                    fontSize = Double.parseDouble(fontSizeStr.substring(0, fontSizeStr.length() - 2).trim()) * 1.33;
                } else if (fontSizeStr.endsWith("em")) {
                    // Convert em to pixels (approximate: 1em = 16px)
                    fontSize = Double.parseDouble(fontSizeStr.substring(0, fontSizeStr.length() - 2).trim()) * 16;
                } else {
                    // Try to parse as plain number (assume pixels)
                    fontSize = Double.parseDouble(fontSizeStr);
                }
            } catch (NumberFormatException e) {
                // Keep default font size if parsing fails
            }
        }
        
        // Calculate height: fontSize * number of lines + padding for borders and scrollbars
        // Add approximately 8 pixels padding per line for line spacing, plus 10 for borders
        double lineHeight = fontSize + 4; // Font size plus line spacing
        double padding = 10; // Padding for borders and internal spacing
        
        return (lineHeight * metadata.height) + padding;
    }
    
    /**
     * Calculates the preferred width for a control based on maxLength or data type.
     * Takes font size into consideration for accurate sizing.
     */
    private static double calculateControlWidth(DisplayItem metadata, AreaItem item) {
        if (metadata == null) {
            return -1; // Use default
        }
        
        // Create a measuring text node to calculate width
        javafx.scene.text.Text measuringText = new javafx.scene.text.Text();
        
        // Apply font size if specified
        String fontStyle = "-fx-font-weight: normal;";
        if (metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            fontStyle += " -fx-font-size: " + metadata.itemFontSize + ";";
        }
        measuringText.setStyle(fontStyle);
        
        // Determine the character width to use for calculation
        int charCount = 0;
        String sampleText = null;
        
        // Use maxLength if specified
        if (metadata.maxLength != null && metadata.maxLength > 0) {
            charCount = metadata.maxLength;
        } else {
            // For ChoiceBox and ComboBox, use options data to determine size if available
            if ((metadata.itemType == ItemType.CHOICEBOX || metadata.itemType == ItemType.COMBOBOX)) {
                String longestOption = getLongestDisplayOption(metadata);
                
                if (!longestOption.isEmpty()) {
                    sampleText = longestOption;
                    measuringText.setText(sampleText);
                    double textWidth = measuringText.getLayoutBounds().getWidth();
                    double padding = 50; // Extra padding for dropdown arrow, borders, and spacing (increased from 40)
                    return textWidth + padding;
                }
            }
            
            // For Spinner, use min/max values to determine size if available
            if (metadata.itemType == ItemType.SPINNER && (metadata.min != null || metadata.max != null)) {
                // Find the longest value between min and max
                String longestValue = "";
                
                if (metadata.min != null) {
                    String minStr = String.valueOf(metadata.min);
                    if (minStr.length() > longestValue.length()) {
                        longestValue = minStr;
                    }
                }
                
                if (metadata.max != null) {
                    String maxStr = String.valueOf(metadata.max);
                    if (maxStr.length() > longestValue.length()) {
                        longestValue = maxStr;
                    }
                }
                
                if (!longestValue.isEmpty()) {
                    // Add 2 extra characters for the spinner arrows
                    sampleText = longestValue + "MM";
                    measuringText.setText(sampleText);
                    double textWidth = measuringText.getLayoutBounds().getWidth();
                    double padding = 20; // Basic padding for borders
                    return textWidth + padding;
                }
            }
            
            // Otherwise, guess length based on item type and data type
            charCount = guessLengthByType(metadata.itemType, item);
        }
        
        if (charCount <= 0) {
            return -1; // Use default
        }
        
        // Use 'M' as a representative character (one of the widest)
        sampleText = "M".repeat(charCount);
        measuringText.setText(sampleText);
        
        // Get the width and add padding for borders, scrollbars, etc.
        double textWidth = measuringText.getLayoutBounds().getWidth();
        double padding = 20; // Account for padding and borders
        
        return textWidth + padding;
    }
    
    /**
     * Guesses an appropriate character length based on control type and context.
     */
    private static int guessLengthByType(DisplayItem.ItemType itemType, AreaItem item) {
        if (itemType == null) {
            return 20; // Default
        }
        
        switch (itemType) {
            case TEXTFIELD:
            case PASSWORDFIELD:
                // Check if item has type info to guess better
                if (item != null && item.varRef != null) {
                    // For numeric types, shorter width
                    return 15;
                }
                return 30; // Default for text fields
                
            case TEXTAREA:
                return 50; // Wider for text areas
                
            case SPINNER:
                return 10; // Spinners are typically narrow
                
            case COMBOBOX:
            case CHOICEBOX:
                return 20; // Medium width for dropdowns
                
            case DATEPICKER:
                return 15; // Date format is typically short
                
            case COLORPICKER:
                return 12; // Color picker is relatively compact
                
            default:
                return 20; // Default fallback
        }
    }
    
    /**
     * Gets the longest display option text from metadata.
     * Checks optionsMap first (using values as display text), then falls back to options list.
     * 
     * @param metadata The DisplayItem metadata containing options or optionsMap
     * @return The longest option text, or empty string if no options are available
     */
    private static String getLongestDisplayOption(DisplayItem metadata) {
        String longestOption = "";
        
        if (metadata == null) {
            return longestOption;
        }
        
        // Check optionsMap first (values are display text)
        if (metadata.optionsMap != null && !metadata.optionsMap.isEmpty()) {
            for (String option : metadata.optionsMap.values()) {
                if (option != null && option.length() > longestOption.length()) {
                    longestOption = option;
                }
            }
        } else if (metadata.options != null && !metadata.options.isEmpty()) {
            // Fall back to options list
            for (String option : metadata.options) {
                if (option != null && option.length() > longestOption.length()) {
                    longestOption = option;
                }
            }
        }
        
        return longestOption;
    }

    /**
     * Applies prompt text styling (color, bold, italic) to a Label.
     */
    private static void applyPromptTextStyling(Label label, DisplayItem metadata) {
        if (metadata == null) {
            return;
        }
        
        StringBuilder styleBuilder = new StringBuilder();
        String existingStyle = label.getStyle();
        if (existingStyle != null && !existingStyle.isEmpty()) {
            styleBuilder.append(existingStyle);
            if (!existingStyle.endsWith(";")) {
                styleBuilder.append(";");
            }
            styleBuilder.append(" ");
        }
        
        // Apply color
        if (metadata.labelColor != null && !metadata.labelColor.isEmpty()) {
            styleBuilder.append("-fx-text-fill: ").append(metadata.labelColor).append("; ");
        }
        
        // Apply bold
        if (Boolean.TRUE.equals(metadata.labelBold)) {
            styleBuilder.append("-fx-font-weight: bold; ");
        }
        
        // Apply italic
        if (Boolean.TRUE.equals(metadata.labelItalic)) {
            styleBuilder.append("-fx-font-style: italic; ");
        }
        
        if (styleBuilder.length() > 0) {
            label.setStyle(styleBuilder.toString());
        }
    }
    
    /**
     * Creates a slider wrapped in an HBox with a value label that displays the current slider value.
     * The label is positioned after the slider and updates dynamically as the slider changes.
     * 
     * @param sliderNode The slider control node
     * @param metadata The DisplayItem metadata for styling information
     * @return An HBox containing the slider and value label
     */
    private static Node createSliderWithValueLabel(Node sliderNode, DisplayItem metadata) {
        if (!(sliderNode instanceof javafx.scene.control.Slider)) {
            return sliderNode; // Safety check - return as-is if not a slider
        }
        
        javafx.scene.control.Slider slider = (javafx.scene.control.Slider) sliderNode;
        
        // Create the value label
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label();
        
        // Initialize with current slider value
        updateSliderValueLabel(valueLabel, slider.getValue());
        
        // Apply styling to the value label to match item styling
        StringBuilder valueLabelStyle = new StringBuilder();
        valueLabelStyle.append("-fx-padding: 0 0 0 10; "); // Left padding to separate from slider
        
        // Apply font size from metadata
        if (metadata != null && metadata.itemFontSize != null && !metadata.itemFontSize.isEmpty()) {
            valueLabelStyle.append("-fx-font-size: ").append(metadata.itemFontSize).append("; ");
        }
        
        // Apply text color from metadata
        if (metadata != null && metadata.itemColor != null && !metadata.itemColor.isEmpty()) {
            valueLabelStyle.append("-fx-text-fill: ").append(metadata.itemColor).append("; ");
        }
        
        // Apply bold from metadata
        if (metadata != null && metadata.itemBold != null && metadata.itemBold) {
            valueLabelStyle.append("-fx-font-weight: bold; ");
        }
        
        // Apply italic from metadata
        if (metadata != null && metadata.itemItalic != null && metadata.itemItalic) {
            valueLabelStyle.append("-fx-font-style: italic; ");
        }
        
        valueLabel.setStyle(valueLabelStyle.toString());
        valueLabel.setMinWidth(50); // Minimum width for the value label
        
        // Add listener to update the label when slider value changes
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSliderValueLabel(valueLabel, newValue.doubleValue());
        });
        
        // Store the value label as a property on the slider for later access
        slider.getProperties().put("valueLabel", valueLabel);
        
        // Create HBox container
        javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(5);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        container.getChildren().addAll(slider, valueLabel);
        
        // Make container transparent for picking so tooltips work properly
        container.setPickOnBounds(false);
        
        // Copy user data and properties from slider to container so they're accessible
        container.setUserData(slider.getUserData());
        container.getProperties().putAll(slider.getProperties());
        
        return container;
    }
    
    /**
     * Updates the value label with the current slider value.
     * Formats the value as an integer if it's a whole number, otherwise as a decimal.
     * 
     * @param label The label to update
     * @param value The slider value
     */
    private static void updateSliderValueLabel(javafx.scene.control.Label label, double value) {
        // Format as integer if it's a whole number, otherwise show one decimal place
        if (value == Math.floor(value)) {
            label.setText(String.format("%d", (int) value));
        } else {
            label.setText(String.format("%.1f", value));
        }
    }
    
    /**
     * Creates a TreeItem from a TreeItemDef, recursively creating children.
     * Supports icons that change based on expanded/collapsed state.
     * 
     * @param def The TreeItemDef definition
     * @return A TreeItem with value, icon, and children
     */
    private static TreeItem<String> createTreeItem(DisplayItem.TreeItemDef def) {
        TreeItem<String> item = new TreeItem<>(def.value != null ? def.value : "");
        
        // Set expanded state
        if (def.expanded != null) {
            item.setExpanded(def.expanded);
        }
        
        // Recursively add children first (so we know if this is a branch or leaf)
        boolean hasChildren = def.children != null && !def.children.isEmpty();
        if (hasChildren) {
            for (DisplayItem.TreeItemDef childDef : def.children) {
                item.getChildren().add(createTreeItem(childDef));
            }
            // Expand parent nodes that have children by default
            if (def.expanded == null) {
                item.setExpanded(true);
            }
        }
        
        // Set up icons
        boolean hasOpenClosedIcons = def.iconOpen != null || def.iconClosed != null;
        
        if (hasOpenClosedIcons && hasChildren) {
            // Dynamic icons that change based on expanded/collapsed state
            updateTreeItemIcon(item, def, item.isExpanded());
            
            // Add listener for expansion state changes
            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                updateTreeItemIcon(item, def, isExpanded);
            });
        } else if (def.icon != null && !def.icon.isEmpty()) {
            // Static icon (same for all states)
            setTreeItemIcon(item, def.icon);
        }
        
        return item;
    }
    
    /**
     * Updates the icon of a tree item based on its expanded state.
     * Uses iconOpen when expanded, iconClosed when collapsed.
     * Falls back to icon property if specific state icon is not defined.
     * 
     * @param item The TreeItem to update
     * @param def The TreeItemDef containing icon definitions
     * @param isExpanded Whether the item is currently expanded
     */
    private static void updateTreeItemIcon(TreeItem<String> item, DisplayItem.TreeItemDef def, boolean isExpanded) {
        String iconPath;
        if (isExpanded) {
            // Use iconOpen if available, otherwise fall back to icon
            iconPath = def.iconOpen != null ? def.iconOpen : def.icon;
        } else {
            // Use iconClosed if available, otherwise fall back to icon
            iconPath = def.iconClosed != null ? def.iconClosed : def.icon;
        }
        
        if (iconPath != null && !iconPath.isEmpty()) {
            setTreeItemIcon(item, iconPath);
        }
    }
    
    /**
     * Sets the icon graphic for a tree item.
     * Supports loading from classpath resources or file paths.
     * 
     * @param item The TreeItem to set the icon on
     * @param iconPath The path to the icon image
     */
    private static void setTreeItemIcon(TreeItem<String> item, String iconPath) {
        try {
            javafx.scene.image.Image image = null;
            
            // Try loading from classpath using ClassLoader (uses absolute paths from classpath root)
            String resourcePath = iconPath.startsWith("/") ? iconPath.substring(1) : iconPath;
            java.io.InputStream is = AreaItemFactory.class.getClassLoader().getResourceAsStream(resourcePath);
            
            // Also try with leading slash using Class.getResourceAsStream
            if (is == null) {
                is = AreaItemFactory.class.getResourceAsStream("/" + resourcePath);
            }
            
            if (is != null) {
                try {
                    image = new javafx.scene.image.Image(is);
                } finally {
                    is.close();
                }
            } else {
                // Try loading from file system
                java.io.File file = new java.io.File(iconPath);
                if (file.exists()) {
                    image = new javafx.scene.image.Image(file.toURI().toString());
                }
            }
            
            if (image != null && !image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(16);
                imageView.setFitWidth(16);
                imageView.setPreserveRatio(true);
                item.setGraphic(imageView);
            }
        } catch (Exception e) {
            // Silently ignore icon loading errors - icons are optional
        }
    }
    
    /**
     * Expands all nodes in a tree recursively.
     * 
     * @param item The root item to start expanding from
     */
    private static void expandAllNodes(TreeItem<String> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<String> child : item.getChildren()) {
            expandAllNodes(child);
        }
    }
}
