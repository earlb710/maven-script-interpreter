# util.runlater Test

This test script validates the `util.runlater` builtin function that executes callbacks asynchronously on the JavaFX Application Thread.

## Test File

### test_util_runlater.ebs

Comprehensive test suite for `util.runlater` functionality with optional callback support.

**Tests Covered:**
1. **Simple Callback**: Verifies basic callback execution on JavaFX thread
2. **No Callback**: Verifies function returns immediately when no callback is provided (empty string)
3. **Multiple Callbacks**: Verifies multiple concurrent runlater calls execute independently

**How to Run:**
```bash
# From the console
/open scripts/test/test_util_runlater.ebs
# Press Ctrl+Enter to execute

# Or from command line
java -cp target/classes com.eb.script.Run scripts/test/test_util_runlater.ebs

# Parse-only mode (validate syntax without execution)
java -cp target/classes com.eb.script.Run --parse scripts/test/test_util_runlater.ebs
```

**Expected Output:**
The test script prints progress messages and test results. All callbacks should execute successfully with appropriate console output.

## Function Signature

```javascript
call util.runlater(callback);  // callback is optional EBS function name (string)
```

## Usage Examples

### Basic Callback
```javascript
myCallback() {
    print "This runs on JavaFX thread!";
}

call util.runlater("myCallback");
```

### Without Callback
```javascript
// Returns immediately, no callback scheduled
call util.runlater("");
```

### With Screen Context
```javascript
screen myScreen = {
    "vars": [{"name": "status", "type": "string", "default": "Ready"}]
};

updateUI() {
    myScreen.status = "Updated!";
    print "UI updated from callback";
}

show screen myScreen;
call util.runlater("updateUI");
```

## Test Characteristics

### Asynchronous Execution
- Callbacks execute asynchronously on the JavaFX Application Thread
- Main script continues immediately after `util.runlater` call
- Tests use `thread.sleep()` to wait for callbacks to complete

### Thread Safety
- All operations are thread-safe
- Multiple `util.runlater` calls can be made concurrently
- Callbacks are queued and executed in order on the JavaFX thread

### Screen Context
- Screen context is preserved for callbacks
- Callbacks can safely access screen variables
- Proper cleanup ensures no context leakage

## Features

1. **Optional Callback**: Callback parameter is optional - function returns immediately if empty/null
2. **No Parameters**: Callbacks receive no parameters (unlike timers which receive timer name)
3. **Error Handling**: Errors in callbacks are caught and logged without crashing the application
4. **Screen Integration**: Works seamlessly with screen-based UI operations

## Timing Considerations

- Tests use `thread.sleep(500)` to allow callbacks time to execute
- Sleep duration can be adjusted based on system performance
- Callbacks typically execute within milliseconds on modern systems

## Debugging Failed Tests

If tests fail:

1. **Check callback names**: Ensure function names match exactly (case-insensitive)
2. **Check timing**: Increase sleep duration if callbacks aren't completing
3. **Check for errors**: Look for error messages in console output indicating callback failures
4. **Verify JavaFX**: Ensure JavaFX Application Thread is running (only applicable in UI mode)

## Adding New Tests

When adding new `util.runlater` tests:

1. Create descriptive test names and clear comments
2. Use flags or counters to verify callback execution
3. Use appropriate sleep durations for timing verification
4. Test both with and without callbacks
5. Test error conditions (invalid callback names, etc.)
6. Print clear test result messages

## Comparison with thread.timerStart

| Feature | util.runlater | thread.timerStart |
|---------|---------------|-------------------|
| Execution | One-time | Repeating |
| Parameters to Callback | None | Timer name |
| Timing Control | Immediate (next FX cycle) | Periodic with specified interval |
| Stop Required | No | Yes (thread.timerStop) |
| Use Case | Single async UI update | Periodic updates/animations |

## Known Limitations

- Callback must be a function name (string), not a lambda or inline code
- Callbacks receive no parameters - use closure/global variables for data passing
- Execution timing depends on JavaFX event queue - not suitable for precise timing
- Only works when JavaFX Application Thread is running (console or UI mode)

## Implementation Notes

- Uses `Platform.runLater()` internally
- Callbacks execute via `CallStatement` similar to timer callbacks
- Screen context is captured at call time and restored during callback execution
- Errors are logged but don't interrupt the application
