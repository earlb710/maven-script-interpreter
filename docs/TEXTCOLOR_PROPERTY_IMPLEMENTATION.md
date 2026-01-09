# textColor Property Implementation

## Overview
This document describes the implementation of the new `textColor` property for screen items in the EBS scripting language. The property provides a consistent naming convention alongside `labelColor` for better code readability and user experience.

## Problem Statement
Previously, screen items had:
- `labelColor` - for the color of the descriptive label text
- `itemColor` - for the color of the actual item/control text

The naming inconsistency between `labelColor` and `itemColor` could be confusing. To improve consistency, we added `textColor` as an alternative property name for the actual item text color.

## Implementation Details

### 1. DisplayItem Class Changes
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/DisplayItem.java`

Added a new field:
```java
// Text color for the actual item (alternative to itemColor for consistency with labelColor)
String textColor;
```

Also updated the `toString()` method to include both `itemColor` and `textColor` fields for debugging purposes.

### 2. InterpreterScreen Parsing
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java`

Added parsing logic to handle the new property:
```java
// Extract textColor (alternative to itemColor for consistency with labelColor)
if (displayDef.containsKey("textColor")) {
    metadata.textColor = String.valueOf(displayDef.get("textColor"));
} else if (displayDef.containsKey("text_color")) {
    metadata.textColor = String.valueOf(displayDef.get("text_color"));
}
```

Supports both camelCase (`textColor`) and snake_case (`text_color`) variants for flexibility.

### 3. ScreenFactory Updates
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenFactory.java`

Updated three key methods:

#### parseDisplayMetadata
Added parsing for the textColor property:
```java
metadata.textColor = getStringValue(displayDef, "textColor", getStringValue(displayDef, "text_color", null));
```

#### mergeDisplayMetadata
Added textColor to the base copy:
```java
merged.textColor = base.textColor;
```

And to the overlay merge logic:
```java
if (overlay.textColor != null) merged.textColor = overlay.textColor;
```

#### cloneDisplayItem
Added textColor to the cloning logic:
```java
clone.textColor = source.textColor;
```

### 4. AreaItemFactory Styling
**File**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/AreaItemFactory.java`

Updated the styling application to use `textColor` with precedence over `itemColor`:

#### applyControlSizeAndFont method
```java
// Apply item text color (textColor takes precedence over itemColor)
String colorToApply = (metadata.textColor != null && !metadata.textColor.isEmpty()) 
                    ? metadata.textColor 
                    : metadata.itemColor;
if (colorToApply != null && !colorToApply.isEmpty()) {
    itemStyle.append("-fx-text-fill: ").append(colorToApply).append("; ");
}
```

#### Slider value label styling
Similarly updated to give precedence to `textColor` over `itemColor` for slider value labels.

### 5. JSON Schema Update
**File**: `ScriptInterpreter/src/main/resources/json/display-metadata.json`

Added the new property to the schema:
```json
"textColor": {
  "type": "string",
  "description": "Text color for the actual item (alternative to itemColor, for consistency with labelColor). Takes precedence over itemColor if both are specified."
}
```

## Usage Examples

### Basic Usage
```ebs
screen myScreen = {
    "vars": [
        {
            "name": "username",
            "type": "string",
            "display": {
                "type": "textfield",
                "labelText": "Username:",
                "labelColor": "#000000",    // Black label text
                "textColor": "#FF0000"      // Red input text
            }
        }
    ]
};
```

### Precedence Behavior
When both `textColor` and `itemColor` are specified, `textColor` takes precedence:
```ebs
{
    "name": "field",
    "type": "string",
    "display": {
        "type": "textfield",
        "textColor": "#AA00AA",    // Purple - this will be used
        "itemColor": "#00AAAA"     // Cyan - this will be ignored
    }
}
```

### Backward Compatibility
Existing code using `itemColor` continues to work without any changes:
```ebs
{
    "name": "field",
    "type": "string",
    "display": {
        "type": "textfield",
        "itemColor": "#00AA00"     // Still works as before
    }
}
```

## Benefits

1. **Consistent Naming**: `labelColor` and `textColor` provide a clear, consistent naming pattern
2. **Backward Compatibility**: Existing code using `itemColor` continues to work
3. **Clear Precedence**: When both are specified, `textColor` takes precedence, providing a clear upgrade path
4. **Better Readability**: More intuitive property names improve code maintainability

## Testing

Test scripts have been created to verify the implementation:
- `ScriptInterpreter/scripts/test/test_textcolor_property.ebs` - Comprehensive UI test
- `ScriptInterpreter/scripts/test/test_textcolor_parse.ebs` - Parsing verification test

## Compatibility

- **Backward Compatible**: Yes - all existing code using `itemColor` continues to work
- **Forward Compatible**: Yes - new code can use `textColor` for better clarity
- **Migration Path**: Users can gradually migrate from `itemColor` to `textColor` at their convenience

## Related Files

1. `DisplayItem.java` - Core data model
2. `InterpreterScreen.java` - Screen definition parsing
3. `ScreenFactory.java` - Screen factory and metadata processing
4. `AreaItemFactory.java` - Control creation and styling
5. `display-metadata.json` - JSON schema definition

## Security Summary

CodeQL analysis was performed on all changes. No security vulnerabilities were detected.
