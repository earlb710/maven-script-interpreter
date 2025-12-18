# Chess Timer Cleanup Test

## Issue
The chess game timer (`chessTimer`) was not being stopped when the chess screen was closed because the timer was started from the main script context (in `handleStartGame()`) after showing the screen, not from within the screen context.

## Root Cause
When `thread.timerStart()` is called, it captures the current screen context via `context.getCurrentScreen()` and stores it as the timer's "source". The timer source is used for automatic cleanup when a screen closes.

In the original chess game:
- Timer was started in `handleStartGame()` AFTER `show screen chessScreen;`
- At that point, `getCurrentScreen()` returns `null` because we're not in a screen context
- Timer source was set to `"script"` instead of `"chessscreen"`
- When the chess screen closed, it only stopped timers with source `"chessscreen"`, not `"script"`

## Solution
Move the timer start to the screen's startup handler so it runs within the screen context:

### Changes Made

1. **Added `cleanupChessScreen()` function** (lines 896-901)
   - Explicitly stops the `chessTimer` when called
   - Provides clear logging for debugging
   - Now properly referenced by the cleanup handler

2. **Added `startup` handler to chess screen** (line 1295)
   - Starts the timer within the screen context
   - Timer source is correctly set to `"chessscreen"`
   - Timer will be automatically stopped when screen closes

3. **Removed timer start from `handleStartGame()`** (line 1264)
   - Timer no longer started from script context
   - Added comment explaining timer is started by screen's startup handler

## Testing

### Manual Test Steps
1. Open the chess game project
2. Run `chess-game.ebs`
3. Click "Start New Game" button
4. Observe the console output:
   - Should see "Chess timer started." message
   - Timer should update every second
5. Close the chess screen window (click X button)
6. Observe the console output:
   - Should see "Chess screen cleanup: Stopping timer..."
   - Should see "Stopped 1 timer(s) associated with screen 'chessscreen'"
   - Should see "Chess screen cleanup complete."
7. Verify timer stops (no more timer updates in console)

### Expected Console Output on Close
```
Chess screen cleanup: Stopping timer...
Chess screen cleanup complete.
Stopped 1 timer(s) associated with screen 'chessscreen'
Screen 'chessscreen' closed
```

## Technical Details

### Timer Source Tracking
From `BuiltinsThread.java:230-231`:
```java
// Determine the source: screen name if in a screen context, otherwise "script"
String source = (currentScreen != null && !currentScreen.isEmpty()) ? currentScreen : "script";
```

### Screen Cleanup
From `InterpreterScreen.java:1537-1542`:
```java
// Stop all timers associated with this screen
int stoppedTimers = BuiltinsThread.stopTimersForSource(screenName);
if (stoppedTimers > 0 && context.getOutput() != null) {
    String message = String.format("Stopped %d timer(s) associated with screen '%s'", stoppedTimers, screenName);
    context.getOutput().printlnInfo(message);
}
```

## Best Practice
**Always start screen-related timers in the screen's startup handler, not from external code.**

This ensures:
- Timer source is correctly set to the screen name
- Automatic cleanup works properly
- Timer lifecycle is tied to screen lifecycle
- Consistent behavior across all screens
