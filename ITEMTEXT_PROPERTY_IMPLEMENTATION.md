# itemText Property Implementation

## Overview
This document describes the implementation of the new `itemText` property for button and label items in the EBS scripting language. This property allows dynamic updating of button and label text at runtime using the `scr.setProperty()` builtin function.

## Problem Statement
Previously, the `text` and `value` properties were available but were intentionally removed with the guidance that text should be set via screen variables. However, for buttons and labels that don't have associated screen variables, there was no way to dynamically update their displayed text.

The `labelText` property in `DisplayItem` is used for initial text during screen creation, but there was no mechanism to update this text dynamically after the screen is shown. This implementation adds the `itemText` property to enable dynamic text updates for buttons and labels.

## Implementation Details

### 1. DisplayItem Class Changes
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/DisplayItem.java`

Added a new public field:
```java
// Item text - the actual displayed text for buttons and labels (can be updated dynamically via scr.setProperty)
public String itemText;
```

This field stores the text value that can be dynamically updated at runtime.

### 2. BuiltinsScreen Changes
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsScreen.java`

#### setAreaItemProperty Method
Added handling for the "itemtext" property to store the value in the DisplayItem:
```java
case "itemtext" -> {
    // ItemText is a display property for buttons and labels, so set it on DisplayItem
    if (item.displayItem == null) {
        item.displayItem = new DisplayItem();
    }
    item.displayItem.itemText = value != null ? String.valueOf(value) : null;
}
```

#### getAreaItemProperty Method
Added retrieval for the "itemtext" property:
```java
case "itemtext" ->
    item.displayItem != null ? item.displayItem.itemText : null;
```

#### applyPropertyToControl Method
Added application of the itemText property to JavaFX Button and Label controls:
```java
case "itemtext" -> {
    // ItemText property for buttons and labels - updates the displayed text
    String textValue = value != null ? String.valueOf(value) : "";
    if (control instanceof javafx.scene.control.Button) {
        ((javafx.scene.control.Button) control).setText(textValue);
    } else if (control instanceof javafx.scene.control.Label) {
        ((javafx.scene.control.Label) control).setText(textValue);
    }
}
```

Updated the error message to include "itemtext" in the list of valid properties:
```java
"Valid properties are: editable, disabled, visible, tooltip, textcolor, backgroundcolor, " +
"prefwidth, prefheight, minwidth, minheight, maxwidth, maxheight, itemtext. "
```

## Usage

### Setting Button Text
```ebs
// Define a button
screen myScreen = {
    "area": [{
        "name": "area1",
        "type": "vbox",
        "items": [{
            "name": "myButton",
            "display": {
                "type": "button",
                "labelText": "Click Me",
                "onClick": "print 'Clicked';"
            }
        }]
    }]
};

show screen myScreen;

// Update the button text dynamically
call scr.setProperty("myScreen.myButton", "itemText", "New Button Text");
```

### Setting Label Text
```ebs
// Define a label
screen myScreen = {
    "area": [{
        "name": "area1",
        "type": "vbox",
        "items": [{
            "name": "statusLabel",
            "display": {
                "type": "label",
                "labelText": "Status: Ready"
            }
        }]
    }]
};

show screen myScreen;

// Update the label text dynamically
call scr.setProperty("myScreen.statusLabel", "itemText", "Status: Processing...");
```

### Dynamic Updates in Event Handlers
```ebs
var counter: int = 0;

updateCounter() {
    counter = counter + 1;
    var text: string = "Count: " + string.fromInt(counter);
    call scr.setProperty("myScreen.counterLabel", "itemText", text);
}

screen myScreen = {
    "area": [{
        "name": "area1",
        "type": "vbox",
        "items": [
            {
                "name": "counterLabel",
                "display": {
                    "type": "label",
                    "labelText": "Count: 0"
                }
            },
            {
                "name": "incrementButton",
                "display": {
                    "type": "button",
                    "labelText": "Increment",
                    "onClick": "call updateCounter();"
                }
            }
        ]
    }]
};

show screen myScreen;
```

## Notes

### Relationship with labelText
- `labelText` in DisplayItem is used for the **initial** text when creating the button or label
- `itemText` is used for **dynamic updates** at runtime via `scr.setProperty()`
- Both properties can work together: use `labelText` for initial setup, then `itemText` for runtime updates

### Property Naming
The property name `itemText` was chosen to:
1. Distinguish it from the removed `text` property
2. Be consistent with other item-level properties like `itemColor`, `itemBold`, `itemItalic`
3. Clearly indicate it applies to the item itself (button or label text)

### Limitations
- The `itemText` property only applies to Button and Label controls
- For other controls with text (TextFields, TextAreas), use screen variables or the `editable` property with variable binding
- The property updates the JavaFX control's text property directly on the UI thread

## Testing
Test scripts are provided:
- `test_itemtext_property.ebs` - Comprehensive interactive test with buttons and labels
- `test_itemtext_simple.ebs` - Simple validation test

To test manually:
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="../test_itemtext_property.ebs"
```

## Related Properties
- `labelText` - Initial text for buttons and labels
- `textColor` / `itemColor` - Text color for items
- `labelColor` - Color for label text (when label is positioned with control)
- `promptHelp` - Placeholder text for input controls
