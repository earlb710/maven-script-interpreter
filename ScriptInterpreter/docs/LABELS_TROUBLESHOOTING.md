# Labels Troubleshooting Guide

## Issue Report

User reported that after commit 8b1eb17, "buttons are working correctly, but all the other items lost their labels".

## Analysis

### How Labels Work

Labels for input controls (TextField, ComboBox, etc.) are created using the `labelText` field in display metadata:

1. **Variable Definition**: Variables define display metadata including `labelText`
   ```json
   {
       "name": "clientId",
       "type": "string",
       "display": {
           "type": "textfield",
           "labelText": "Client ID:",
           "labelTextAlignment": "left"
       }
   }
   ```

2. **Metadata Retrieval**: When rendering, metadata is retrieved via metadataProvider
   - For InterpreterScreen: `context.getDisplayItem().get(screenName + "." + varName)`
   - For createScreenFromDefinition: `metadataMap.get(varName)`

3. **Label Creation**: ScreenFactory checks for `labelText` and wraps controls
   ```java
   if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
       if (!(control instanceof Label) && !(control instanceof Button)) {
           nodeToAdd = createLabeledControl(metadata.labelText, ...);
       }
   }
   ```

### Parsing Paths

There are TWO separate paths for parsing display metadata:

#### Path 1: InterpreterScreen (for `screen` keyword in .ebs files)
- File: `InterpreterScreen.java`
- Method: Parses display metadata directly
- Line 658-659: Parses `"labeltext"` (lowercase key)
- Stores in context: `context.getDisplayItem().put(screenName + "." + varName, metadata)`

#### Path 2: ScreenFactory (for JSON-based screen definitions)
- File: `ScreenFactory.java`
- Method: `parseDisplayItem()`
- Line 1108: Parses `labelText` using `getStringValue(displayDef, "labelText", ...)`
- The `getStringValue` helper converts key to lowercase for lookup
- Stores in metadataMap

### Recent Changes (Commit 8b1eb17)

The commit added missing field parsing to `parseDisplayItem()`:
- Added `labelText` parsing
- Added `promptHelp` parsing
- Added `onClick` parsing
- Added `options` parsing
- Added styling properties parsing

**Expected Impact**: Should HELP, not hurt - fields that weren't being parsed are now being parsed.

**Actual Impact**: User reports labels are missing (except for buttons which work).

### Potential Causes

1. **Compilation Issue**: Code not recompiled after changes
   - Status: Unlikely - user confirms buttons work correctly

2. **Runtime Issue**: Exception during label creation
   - Check: Look for console errors when screen loads

3. **Metadata Not Retrieved**: metadataProvider returning null/empty metadata
   - Check: Verify metadata exists in context/metadataMap

4. **Label Display Issue**: Labels created but not visible
   - Check: Viewport transparency, z-index, overlapping elements

5. **Scope Issue**: parseDisplayItem being called where it shouldn't be
   - Check: Verify InterpreterScreen path doesn't accidentally use parseDisplayItem

### Code Verification

**parseDisplayItem parsing (ScreenFactory.java line 1108)**:
```java
metadata.labelText = getStringValue(displayDef, "labelText", 
    getStringValue(displayDef, "label_text", null));
```

**getStringValue helper (converts to lowercase)**:
```java
private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
    if (map.containsKey(key.toLowerCase())) {  // Converts "labelText" -> "labeltext"
        Object value = map.get(key.toLowerCase());
        return value != null ? String.valueOf(value) : defaultValue;
    }
    return defaultValue;
}
```

**InterpreterScreen parsing (line 658-659)**:
```java
if (displayDef.containsKey("labeltext")) {  // Already lowercase
    metadata.labelText = String.valueOf(displayDef.get("labeltext"));
}
```

Both approaches should work correctly with JSON containing `"labelText"` (camelCase).

### Debugging Steps

1. **Add Logging**: Add debug output to see if labelText is in metadata
   ```java
   System.out.println("DEBUG: metadata.labelText = " + 
       (metadata != null ? metadata.labelText : "null"));
   ```

2. **Check Metadata**: Verify metadata is not null and has labelText
   ```java
   DisplayItem metadata = metadataProvider.apply(screenName, item.varRef);
   if (metadata != null) {
       System.out.println("Metadata for " + item.varRef + ": labelText=" + metadata.labelText);
   }
   ```

3. **Check Control Creation**: Verify createLabeledControl is being called
   ```java
   if (metadata != null && metadata.labelText != null && !metadata.labelText.isEmpty()) {
       System.out.println("Creating labeled control for: " + metadata.labelText);
       nodeToAdd = createLabeledControl(...);
   }
   ```

4. **Check Visibility**: Verify labels are in the scene graph
   ```java
   if (nodeToAdd instanceof HBox) {
       HBox container = (HBox) nodeToAdd;
       System.out.println("Container has " + container.getChildren().size() + " children");
   }
   ```

### Next Steps

Awaiting user response to clarify:
1. Was code recompiled after commit 8b1eb17?
2. Are labels missing entirely or just not visible?
3. Are there any console errors?

Once clarified, can add targeted fixes or additional debugging.

## Workaround

If labels are missing, verify the display metadata in variable definitions includes `labelText`:

```json
{
    "name": "myField",
    "type": "string",
    "display": {
        "type": "textfield",
        "labelText": "My Field:",  // Must be present
        "labelTextAlignment": "left"
    }
}
```

## Status

- **Issue**: Reported by user
- **Reproduced**: Not yet confirmed
- **Root Cause**: Under investigation
- **Fix**: Pending user clarification
