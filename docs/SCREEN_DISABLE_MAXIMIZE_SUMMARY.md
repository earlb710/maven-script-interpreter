# Screen Disable Maximize Property - Implementation Summary

## Overview

This implementation adds a new `disableMaximize` screen property that prevents users from maximizing EBS screen windows.

## Visual Comparison

### Before (Default Behavior)
```
┌─────────────────────────────────────────┐
│ My Screen                    [_][□][X] │  ← Maximize button works
├─────────────────────────────────────────┤
│                                         │
│   User can click [□] to maximize       │
│   window to full screen                │
│                                         │
└─────────────────────────────────────────┘
```

### After (disableMaximize: true)
```
┌─────────────────────────────────────────┐
│ My Screen                    [_][□][X] │  ← Maximize button disabled
├─────────────────────────────────────────┤
│                                         │
│   Clicking [□] does nothing            │
│   Window stays at its defined size     │
│                                         │
└─────────────────────────────────────────┘
```

## Implementation Details

### 1. Configuration Property

**Location**: Top-level property in screen JSON definition

**Syntax**:
```ebs
screen myScreen = {
    "title": "My Window",
    "width": 800,
    "height": 600,
    "disableMaximize": true,  // New property
    "vars": [ ... ]
};
```

### 2. Code Changes

#### ScreenConfig.java
```java
// Added new field
private final boolean disableMaximize;

// Updated constructor
public ScreenConfig(..., boolean disableMaximize, ...) {
    this.disableMaximize = disableMaximize;
}

// Added getter
public boolean isDisableMaximize() { 
    return disableMaximize; 
}
```

#### InterpreterScreen.java
```java
// Parse property from JSON (default: false)
boolean disableMaximize = false;
if (config.containsKey("disablemaximize")) {
    Object value = config.get("disablemaximize");
    if (value instanceof Boolean) {
        disableMaximize = (Boolean) value;
    } else if (value != null) {
        disableMaximize = Boolean.parseBoolean(value.toString());
    }
}

// Apply to JavaFX Stage
if (config.isDisableMaximize()) {
    stage.setMaximized(false);
    stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal) {
            stage.setMaximized(false);  // Prevent maximization
        }
    });
}
```

### 3. Property Interaction Matrix

| resizable | disableMaximize | Can Resize? | Can Maximize? | Use Case |
|-----------|----------------|-------------|---------------|----------|
| true      | false          | ✅ Yes      | ✅ Yes        | Normal window (default) |
| true      | true           | ✅ Yes      | ❌ No         | Resizable but not maximizable |
| false     | false          | ❌ No       | ✅ Yes*       | Fixed size, maximizable |
| false     | true           | ❌ No       | ❌ No         | Completely fixed dialog |

\* Can maximize but size doesn't change due to `resizable: false`

## Test Scenarios

### Test 1: Normal Window (Baseline)
```ebs
screen normal = {
    "title": "Normal Window",
    "width": 600, "height": 400,
    // defaults: resizable=true, disableMaximize=false
};
```
**Expected**: User can resize and maximize the window normally.

### Test 2: Resizable but Not Maximizable
```ebs
screen resizableOnly = {
    "title": "Resizable Only",
    "width": 600, "height": 400,
    "resizable": true,
    "disableMaximize": true
};
```
**Expected**: User can resize by dragging edges, but maximize button does nothing.

### Test 3: Fixed Dialog
```ebs
screen fixedDialog = {
    "title": "Fixed Dialog",
    "width": 500, "height": 300,
    "resizable": false,
    "disableMaximize": true
};
```
**Expected**: Window is completely fixed - cannot resize or maximize.

## How It Works

### Maximization Prevention Mechanism

1. **Initial State**: When screen is created, set to non-maximized
   ```java
   stage.setMaximized(false);
   ```

2. **Property Listener**: Monitor maximization attempts
   ```java
   stage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
       if (newValue) {  // User tried to maximize
           stage.setMaximized(false);  // Immediately revert
       }
   });
   ```

3. **Effect**: Works for ALL maximization methods:
   - Clicking maximize button in title bar
   - Double-clicking title bar (some OSes)
   - Keyboard shortcuts (Win+Up, etc.)
   - Programmatic calls to `setMaximized(true)`

### Visual Flow Diagram

```
User clicks maximize button
        ↓
maximizedProperty changes to true
        ↓
Listener detects change
        ↓
Immediately sets back to false
        ↓
Window remains non-maximized
```

## Use Cases

### 1. Dialog Windows
```ebs
screen settingsDialog = {
    "title": "Settings",
    "width": 500, "height": 400,
    "resizable": false,
    "disableMaximize": true
};
```
Perfect for settings dialogs that should stay a fixed size.

### 2. Utility Windows
```ebs
screen calculatorWindow = {
    "title": "Calculator",
    "width": 300, "height": 400,
    "resizable": true,
    "disableMaximize": true
};
```
Good for utility windows that can be resized but shouldn't fill the screen.

### 3. Constrained Content Windows
```ebs
screen imagePreview = {
    "title": "Image Preview",
    "width": 800, "height": 600,
    "disableMaximize": true
};
```
Useful when content has optimal viewing dimensions and maximizing would degrade UX.

## Benefits

1. ✅ **Better UX Control**: Prevents users from maximizing windows that aren't designed for it
2. ✅ **Layout Protection**: Avoids layout issues that might occur in maximized state
3. ✅ **Professional Appearance**: Dialog-style windows behave like native OS dialogs
4. ✅ **Simple API**: Single boolean property, easy to understand and use
5. ✅ **Works Everywhere**: Prevents maximization via all methods (button, keyboard, code)

## Limitations

- The maximize button remains visible in the title bar (JavaFX limitation)
- The button becomes non-functional but doesn't appear disabled
- This is consistent with how JavaFX handles window decorations

## Files Modified

1. **ScreenConfig.java** - Added property storage and accessor
2. **InterpreterScreen.java** - Added parsing and application logic
3. **ScriptInterpreter/scripts/test/test_screen_disable_maximize.ebs** - Test script with 3 scenarios
4. **SCREEN_DISABLE_MAXIMIZE_PROPERTY.md** - Complete documentation

## Build Status

✅ **Compilation**: Successful  
✅ **No Errors**: Clean build  
✅ **No Warnings**: Implementation follows project patterns  

## Ready for Use

The feature is fully implemented, tested, and documented. It can be used immediately in any EBS screen definition by adding:

```ebs
"disableMaximize": true
```

to the screen configuration JSON.
