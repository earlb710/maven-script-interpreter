package com.eb.script.interpreter.screen;

/**
 *
 * @author Earl 
 * 
 * Class to hold area item definition with positioning, variable
 * reference, and optional display metadata. If displayMetadata is not provided,
 * the item will use the DisplayItem associated with its varRef.
 */
public class AreaItem {
    // Item name/identifier

    String name;
    // Display sequence/order
    int sequence = 0;
    // Layout position (e.g., "top", "left", "center", "0,0" for GridPane row,col)
    String layoutPos;
    // Reference to a variable
    String varRef;
    // Optional display metadata for this specific item
    // If null, will use the DisplayItem from varRef
    DisplayItem displayItem;

    // Whether the field can be edited
    Boolean editable;
    // Whether the control is disabled
    Boolean disabled;
    // Whether the control is visible
    Boolean visible;
    // Hover tooltip text
    String tooltip;
    // Text color (e.g., "#000000", "red")
    String textColor;
    // Background color (e.g., "#FFFFFF", "lightblue")
    String backgroundColor;

    // Layout-specific properties
    // Column span for GridPane layouts
    Integer colSpan;
    // Row span for GridPane layouts
    Integer rowSpan;
    // Horizontal grow priority (for HBox, VBox)
    String hgrow;
    // Vertical grow priority (for HBox, VBox)
    String vgrow;
    // Margin around the item (e.g., "10", "10 5", "10 5 10 5")
    String margin;
    // Padding inside the item (e.g., "10", "10 5", "10 5 10 5")
    String padding;
    // Preferred width
    String prefWidth;
    // Preferred height
    String prefHeight;
    // Minimum width
    String minWidth;
    // Minimum height
    String minHeight;
    // Maximum width
    String maxWidth;
    // Maximum height
    String maxHeight;
    // Alignment within parent (e.g., "center", "top-left")
    String alignment;

    @Override
    public String toString() {
        return "AreaItem{"
                + "name='" + name + '\''
                + ", sequence=" + sequence
                + ", layoutPos='" + layoutPos + '\''
                + ", varRef='" + varRef + '\''
                + ", displayMetadata=" + (displayItem != null ? "provided" : "from varRef")
                + ", editable=" + editable
                + ", disabled=" + disabled
                + ", visible=" + visible
                + ", tooltip='" + tooltip + '\''
                + ", textColor='" + textColor + '\''
                + ", backgroundColor='" + backgroundColor + '\''
                + ", colSpan=" + colSpan
                + ", rowSpan=" + rowSpan
                + ", hgrow='" + hgrow + '\''
                + ", vgrow='" + vgrow + '\''
                + ", margin='" + margin + '\''
                + ", padding='" + padding + '\''
                + ", prefWidth='" + prefWidth + '\''
                + ", prefHeight='" + prefHeight + '\''
                + ", alignment='" + alignment + '\''
                + '}';
    }
}

