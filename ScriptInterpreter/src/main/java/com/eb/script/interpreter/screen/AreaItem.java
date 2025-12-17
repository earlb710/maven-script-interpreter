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

    public String name;
    // Display sequence/order
    public int sequence = 0;
    // Layout position (e.g., "top", "left", "center", "0,0" for GridPane row,col)
    public String layoutPos;
    // Reference to a variable
    public String varRef;
    // Optional display metadata for this specific item
    // If null, will use the DisplayItem from varRef
    public DisplayItem displayItem;

    // Whether the field can be edited
    public Boolean editable;
    // Whether the control is disabled
    public Boolean disabled;
    // Whether the control is visible
    public Boolean visible;
    // Hover tooltip text
    public String tooltip;
    // Text color (e.g., "#000000", "red")
    public String textColor;
    // Background color (e.g., "#FFFFFF", "lightblue")
    public String backgroundColor;

    // Layout-specific properties
    // Column span for GridPane layouts
    public Integer colSpan;
    // Row span for GridPane layouts
    public Integer rowSpan;
    // Horizontal grow priority (for HBox, VBox)
    public String hgrow;
    // Vertical grow priority (for HBox, VBox)
    public String vgrow;
    // Margin around the item (e.g., "10", "10 5", "10 5 10 5")
    public String margin;
    // Padding inside the item (e.g., "10", "10 5", "10 5 10 5")
    public String padding;
    // Preferred width
    public String prefWidth;
    // Preferred height
    public String prefHeight;
    // Minimum width
    public String minWidth;
    // Minimum height
    public String minHeight;
    // Maximum width
    public String maxWidth;
    // Maximum height
    public String maxHeight;
    // Item alignment - controls alignment of the item (and its HBox/VBox wrapper) in the parent container
    // This affects how the item is positioned within its parent layout (e.g., "center", "top-left", "center-right")
    public String itemAlignment;
    // onValidate event handler - EBS code to validate item value, expects return true/false
    public String onValidate;
    // onChange event handler - EBS code to execute whenever the item value changes
    public String onChange;
    // onExpand event handler - EBS code to execute when a tree node is expanded
    public String onExpand;
    // onCollapse event handler - EBS code to execute when a tree node is collapsed
    public String onCollapse;

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
                + ", itemAlignment='" + itemAlignment + '\''
                + '}';
    }
}

