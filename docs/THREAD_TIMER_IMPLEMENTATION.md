# Thread Timer Implementation

## Overview

This document describes the implementation of `thread.timerStart` and `thread.timerStop` builtin functions for the EBS Script Interpreter.

## New Builtin Functions

### thread.timerStart(name, period, callback)

Starts a repeating timer that invokes a callback function at fixed intervals.

**Parameters:**
- `name` (STRING): Unique identifier for the timer. Must be linked to a screen:
  - If created inside a screen: Use simple name (e.g., "myTimer") - auto-links to current screen
  - If created outside a screen: Use qualified name (e.g., "screenName.myTimer") - links to specified screen
- `period` (LONG): Time interval in milliseconds between callback invocations
- `callback` (STRING): Name of the function to call when the timer fires

**Returns:** STRING - The timer name

**Behavior:**
- Creates a scheduled task using Java's `ScheduledExecutorService`
- Executes the callback on the JavaFX Application Thread for UI safety
- If a timer with the same name already exists, it is stopped and replaced
- The callback function receives the timer name as its parameter
- Multiple timers can run concurrently
- **Timer-Screen Linkage (NEW):**
  - Timers created inside a screen are automatically linked to that screen
  - Timers created outside a screen must use qualified name format "screenName.timerName"
  - If no screen can be identified, an error is thrown
  - The specified screen must exist (either configured or already shown)

**Examples:**

*Creating timer inside a screen (auto-links):*
```ebs
screen myScreen = {
    "title": "My Screen",
    "width": 400,
    "height": 300,
    "vars": [],
    "startup": "
        // Timer auto-links to myScreen
        thread.timerStart('myTimer', 1000, 'timerCallback');
    "
};
```

*Creating timer outside a screen with qualified name:*
```ebs
// Define callback function
timerCallback(timerName: string) {
    print "Timer fired: " + timerName;
}

// Define screen first
screen myScreen = {
    "title": "My Screen",
    "width": 400,
    "height": 300,
    "vars": []
};

// Create timer with qualified name (links to myScreen)
var name: string = call thread.timerStart("myScreen.myTimer", 1000, "timerCallback");
```

*Error case - creating timer outside screen without qualified name:*
```ebs
// This will throw an error!
call thread.timerStart("myTimer", 1000, "timerCallback");
// Error: timer 'myTimer' is being created outside a screen context.
// Use qualified name format 'screenName.timerName' to link the timer to a specific screen.
```

### thread.timerStop(name)

Stops a running repeating timer.

**Parameters:**
- `name` (STRING): The name of the timer to stop

**Returns:** BOOL - `true` if timer was stopped, `false` if timer not found

**Behavior:**
- Cancels the scheduled task gracefully (does not interrupt running callbacks)
- Removes the timer from the registry
- Thread-safe operation

**Example:**
```ebs
var stopped: bool = call thread.timerStop("myTimer");
if stopped then
    print "Timer stopped successfully";
else
    print "Timer not found";
end
```

## Implementation Details

### Class Structure

**BuiltinsThread.java** - New class handling thread-based timer operations
- Located: `com.eb.script.interpreter.builtins.BuiltinsThread`
- Uses `ScheduledExecutorService` with a pool of 4 threads
- Maintains thread-safe registry of active timers using `ConcurrentHashMap`
- Provides `shutdown()` method for cleanup during application exit

### Integration Points

1. **Builtins.java**
   - Registered in static initialization block
   - Added dispatch logic in `callBuiltin()` method
   - Appears in help system automatically

2. **Interpreter.java**
   - Integrated `BuiltinsThread.shutdown()` in `cleanup()` method
   - Ensures timers are properly terminated on application exit

3. **help-lookup.json**
   - Added comprehensive documentation for both functions
   - Includes examples and parameter descriptions

4. **BuiltinsSystem.java**
   - Updated version: BUILTIN_VER 7→8, BUILD_VER 11→12
   - EBS Language Version now 1.0.8.12

### Thread Safety

- Timer registry uses `ConcurrentHashMap` for thread-safe access
- Callbacks execute on JavaFX Application Thread via `Platform.runLater()`
- Timer cancellation is graceful (does not interrupt running tasks)
- Shutdown is coordinated with application lifecycle

### Error Handling

- Clear error messages include timer name and callback name
- Callback execution errors are logged but don't stop the timer
- Non-existent timer stop attempts return `false` rather than error

## Testing

### Manual Testing

Create a test script to verify functionality:

```ebs
var counter: int = 0;

// Callback that counts and stops after 5 iterations
myCallback(timerName: string) {
    counter = counter + 1;
    print "Timer fired #" + call str.toString(counter);
    
    if counter >= 5 then
        call thread.timerStop(timerName);
    end
}

// Start timer
print "Starting timer...";
call thread.timerStart("test", 1000, "myCallback");
print "Timer started. Will fire 5 times.";
```

### Test Scenarios

1. **Basic Start/Stop**: Verify timer starts and stops correctly
2. **Multiple Timers**: Run multiple timers with different names concurrently
3. **Replace Timer**: Start timer with existing name (should replace)
4. **Invalid Stop**: Stop non-existent timer (should return false)
5. **Callback Error**: Verify timer continues after callback error
6. **Application Exit**: Verify cleanup on exit

## Security

- CodeQL security scan passed with 0 vulnerabilities
- No resource leaks (integrated with application cleanup)
- Thread pool size is bounded (prevents unbounded thread creation)
- Callback execution is isolated (errors don't affect timer)

## Differences from timer.* Builtins

The existing `timer.*` builtins (`timer.start`, `timer.stop`, etc.) are stopwatch-style timers for measuring elapsed time. The new `thread.timerStart` and `thread.timerStop` are fundamentally different:

| Feature | timer.* (Stopwatch) | thread.timer* (Repeating) |
|---------|---------------------|---------------------------|
| Purpose | Measure elapsed time | Execute callbacks periodically |
| Execution | Synchronous | Asynchronous (background) |
| Callback | No callback | Requires callback function |
| Lifecycle | Manual start/stop/continue | Runs until stopped |
| Use Case | Performance measurement | Periodic tasks, polling, animations |

## Future Enhancements

Possible future improvements:
- Add `thread.timerPause(name)` and `thread.timerResume(name)`
- Add `thread.listTimers()` to list active timers
- Support one-shot timers (fire once and stop)
- Configurable thread pool size via system property
- Timer statistics (fire count, missed fires, etc.)

## Version History

- **v1.0.8.12** - Added `thread.timerStart` and `thread.timerStop`
