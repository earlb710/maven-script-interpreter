# Timer-Screen Linkage Implementation

## Overview

This document describes the implementation of the requirement that timers must be linked to a screen. This change enforces better resource management and clearer ownership of timers.

## Requirement

> Change thread.timer creation: a timer now has to be linked to a screen; if the timer is created inside a screen and should be linked to that screen; if a timer is created outside a screen it should use a qualified name screenName.timerName so that the timer can be created against a screen; if a screen cannot be identified, timer is not created and an error should be thrown.

## Implementation Details

### Core Changes

**File:** `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsThread.java`

#### 1. Screen Detection and Validation

```java
// Get the current screen context for the callback (if in a screen)
final String currentScreen = context.getCurrentScreen();

// Determine the target screen for the timer
String targetScreen = null;

// Check if timer name contains a dot, indicating qualified name format (screenName.timerName)
if (timerName.contains(".")) {
    // Parse the qualified name
    int dotIndex = timerName.indexOf('.');
    String screenNamePart = timerName.substring(0, dotIndex);
    
    // Validate that screen exists
    if (!screenExists(context, screenNamePart)) {
        throw new InterpreterError("thread.timerStart: screen '" + screenNamePart + 
            "' does not exist. Cannot create timer with qualified name '" + timerName + "'");
    }
    
    targetScreen = screenNamePart.toLowerCase();
} else if (currentScreen != null && !currentScreen.isEmpty()) {
    // Timer is being created inside a screen - link to current screen
    targetScreen = currentScreen;
} else {
    // Timer is being created outside a screen without qualified name - error
    throw new InterpreterError("thread.timerStart: timer '" + timerName + 
        "' is being created outside a screen context. " +
        "Use qualified name format 'screenName.timerName' to link the timer to a specific screen.");
}
```

#### 2. Screen Existence Check

```java
/**
 * Helper method to check if a screen exists (either configured or already created).
 */
private static boolean screenExists(InterpreterContext context, String screenName) {
    String lowerScreenName = screenName.toLowerCase();
    return context.hasScreenConfig(lowerScreenName) || context.getScreens().containsKey(lowerScreenName);
}
```

### Behavior

1. **Timer Created Inside Screen Context:**
   - Automatically links to the current screen
   - No qualified name needed
   - Example: `thread.timerStart("myTimer", 1000, "callback")` inside a screen event handler

2. **Timer Created Outside Screen Context with Qualified Name:**
   - Must use format `screenName.timerName`
   - Screen must exist (configured or shown)
   - Example: `thread.timerStart("myScreen.myTimer", 1000, "callback")`

3. **Timer Created Outside Screen Context without Qualified Name:**
   - Throws error
   - Error message explains the requirement
   - Example: `thread.timerStart("myTimer", 1000, "callback")` → ERROR

### Error Messages

**No screen context and no qualified name:**
```
thread.timerStart: timer 'myTimer' is being created outside a screen context.
Use qualified name format 'screenName.timerName' to link the timer to a specific screen.
```

**Screen doesn't exist:**
```
thread.timerStart: screen 'nonExistentScreen' does not exist.
Cannot create timer with qualified name 'nonExistentScreen.myTimer'
```

## Testing

### Test Cases

**Test Class:** `ScriptInterpreter/src/test/java/com/eb/script/test/TestTimerScreenLinkage.java`

1. ✅ **Test 1:** Timer creation outside screen without qualified name → throws error
2. ✅ **Test 2:** Timer creation with qualified name for non-existent screen → throws error
3. ✅ **Test 3:** Timer creation with qualified name for existing screen → succeeds
4. ✅ **Test 4:** Timer creation inside screen context → auto-links correctly

All tests pass successfully.

## Updated Files

### Core Implementation
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsThread.java`

### Documentation
- `THREAD_TIMER_IMPLEMENTATION.md`
- `ScriptInterpreter/src/main/resources/help-lookup.json`

### Tests
- `ScriptInterpreter/src/test/java/com/eb/script/test/TestTimerScreenLinkage.java`
- `test_timer_screen_linkage.ebs`
- `test_timer_error_no_screen.ebs`
- `test_timer_error_invalid_screen.ebs`

### Example Scripts (Updated to Comply)
- `ScriptInterpreter/scripts/examples/thread_timer_basic.ebs`
- `ScriptInterpreter/scripts/examples/thread_timer_multiple.ebs`
- `ScriptInterpreter/scripts/examples/thread_timer_animation.ebs`
- `ScriptInterpreter/scripts/examples/thread_timer_advanced.ebs`
- `ScriptInterpreter/projects/Chess/chess-game.ebs`

## Backward Compatibility

**Breaking Change:** Existing scripts that create timers outside screen context without qualified names will fail with a clear error message explaining how to fix the issue.

**Migration Guide:**
1. If timer is created inside a screen event handler → no changes needed (auto-links)
2. If timer is created outside a screen → add qualified name: `screenName.timerName`
3. Ensure the screen exists before creating the timer

## Examples

### Inside Screen (Auto-link)

```ebs
screen myScreen = {
    "title": "My Screen",
    "startup": "
        // Timer auto-links to myScreen
        thread.timerStart('myTimer', 1000, 'callback');
    "
};
```

### Outside Screen (Qualified Name)

```ebs
timerCallback(timerName: string) {
    print 'Timer fired';
}

screen myScreen = {
    "title": "My Screen",
    "vars": []
};

// Use qualified name to link to myScreen
call thread.timerStart("myScreen.myTimer", 1000, "timerCallback");
```

## Security

- CodeQL security scan: **0 alerts**
- No vulnerabilities introduced
- Proper validation of screen existence prevents resource leaks

## Related Features

- Timer cleanup on screen close (PR #315)
- Thread timer management functions (pause, resume, info, etc.)

## Version

Implemented in EBS Language Version 1.0.8.12+

## Date

December 18, 2025
