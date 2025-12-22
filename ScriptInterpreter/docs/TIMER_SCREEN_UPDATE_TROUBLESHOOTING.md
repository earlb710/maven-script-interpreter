# Timer Screen Update Troubleshooting Guide

## Issue
Timer callbacks run and update variables, but screen labels don't refresh visually.

## Root Causes Fixed

### 1. Nested Platform.runLater() Calls
**Problem**: Timer callbacks run on JavaFX thread via `Platform.runLater()`. If screen update code unconditionally wraps operations in another `Platform.runLater()`, it creates nested queuing where updates are delayed instead of executing immediately.

**Solution**: Use `com.eb.util.Util.runOnFx()` instead of `Platform.runLater()`. This utility checks if already on JavaFX thread and executes immediately if so.

**Fixed in commits:**
- `69118f5`: Fixed `BuiltinsScreen.screenSetProperty()`
- `2aee009`: Fixed screen variable refresh callback in `ScreenFactory`
- `9cf7525`: Refactored to use `Util.runOnFx()` utility

### 2. Screen Variable Binding Pattern
**Problem**: Using `scr.setproperty()` to set "text" or "value" properties instead of using screen variable binding via `varRef`.

**Solution**: 
- Define screen variables in the `vars` array
- Bind controls to variables using `varRef` attribute
- Update variables directly: `screenName.varName = newValue`
- The system automatically refreshes bound controls

**Fixed in commits:**
- `01b9832`: Updated test script to use proper variable binding
- `44b164e`: Removed "text" and "value" from `scr.setproperty` (now throws error) and updated chess.ebs to use `varRef` binding for timer fields

## Current Implementation

### Timer Callback Flow
```
1. Timer fires → BuiltinsThread schedules callback via Platform.runLater()
2. Callback runs on JavaFX thread
3. Sets screen variable: chessScreen.whiteTimer = "10:00"
4. Triggers context.triggerScreenRefresh("chessscreen")
5. Refresh callback uses Util.runOnFx() → executes immediately (already on FX thread)
6. refreshBoundControls() updates all controls bound via varRef
7. Label text updates: label.setText(newValue)
```

### Key Components

**BuiltinsThread.java (line 127)**:
```java
Platform.runLater(() -> {
    // Timer callback executes here on JavaFX thread
    mainInterpreter.visitCallStatement(callStmt);
});
```

**Interpreter.java (line 842, 848)**:
```java
screenVarMap.put(secondPart, value);
context.triggerScreenRefresh(firstPart);  // Triggers UI update
```

**ScreenFactory.java (line 3157)**:
```java
context.getScreenRefreshCallbacks().put(lowerScreenName, () -> {
    com.eb.util.Util.runOnFx(() -> refreshBoundControls(allBoundControls, screenVars));
});
```

**com.eb.util.Util.java (line 729)**:
```java
public static void runOnFx(Runnable r) {
    if (Platform.isFxApplicationThread()) {
        r.run();  // Execute immediately
    } else {
        Platform.runLater(r);  // Queue for FX thread
    }
}
```

## Verification Checklist

If timer screen still not updating, verify:

### 1. Screen Variable Definition
```javascript
screen myScreen = {
    "vars": [
        {"name": "timerValue", "type": "string", "default": "00:00"}
    ],
    //...
}
```

### 2. Control Binding with varRef
```javascript
{
    "name": "timerLabel",
    "type": "label",
    "varRef": "timerValue"  // ← Must match variable name
}
```

### 3. Timer Callback Updates Variable
```javascript
timerCallback(timerName: string) {
    myScreen.timerValue = "10:00";  // ← Direct variable assignment
    // NOT: call scr.setproperty("myScreen.timerLabel", "text", "10:00");
}
```

### 4. Timer is Started
```javascript
call thread.timerStart("myTimer", 1000, "timerCallback");
```

### 5. Game/State Flag
```javascript
if !gameStarted then {
    return;  // ← Make sure this isn't blocking updates
}
```

## Debug Mode

To enable debug output, add temporary logging:

**In ScreenFactory.java (line ~3157)**:
```java
com.eb.util.Util.runOnFx(() -> {
    System.out.println("[DEBUG] Refreshing controls for: " + lowerScreenName);
    refreshBoundControls(allBoundControls, screenVars);
});
```

**In ControlUpdater.java (line ~391)**:
```java
Object currentValue = VarRefResolver.resolveVarRefValue(varName.toLowerCase(), screenVars);
if (varName.contains("timer")) {
    System.out.println("[DEBUG] Updating " + varName + " = " + currentValue);
}
updateControlFromValue(control, currentValue, metadata);
```

## Common Issues

### Issue: "Property 'text' not found" error
**Cause**: Trying to use `scr.setproperty()` with "text" or "value"
**Solution**: Use screen variable binding instead

### Issue: Timer callback not firing
**Cause**: Timer not started or stopped prematurely
**Solution**: Check `thread.timerStart()` is called and timer isn't stopped

### Issue: Variable updates but UI doesn't
**Cause**: Control not properly bound via `varRef`
**Solution**: Verify `varRef` attribute matches variable name (case-insensitive)

### Issue: Updates are delayed/batched
**Cause**: Nested `Platform.runLater()` calls
**Solution**: Verify using `Util.runOnFx()` not `Platform.runLater()`

## Testing

Manual test script: `test_timer_screen_update.ebs`
```bash
cd ScriptInterpreter
mvn javafx:run
# Load and run test_timer_screen_update.ebs
# Click "Start Timer" - label should update every second
```

## Related Files

- `BuiltinsThread.java`: Timer callback execution
- `Interpreter.java`: Variable assignment and refresh trigger
- `ScreenFactory.java`: Refresh callback registration
- `ControlUpdater.java`: Control update from variable values
- `Util.java`: Thread-safe JavaFX execution utility
- `chess.ebs`: Example implementation with timer fields
- `test_timer_screen_update.ebs`: Test script

## Additional Resources

- **ARCHITECTURE.md**: Section on "Thread-Safe JavaFX Updates with Util.runOnFx()"
- **SPACE_RULES.md**: EBS syntax conventions
- **docs/EBS_SCRIPT_SYNTAX.md**: Complete language reference
