# Thread Timer Tests

This directory contains test scripts for the `thread.timerStart` and `thread.timerStop` builtins.

## Test Files

### test_thread_timer.ebs

Comprehensive test suite for thread timer functionality.

**Tests Covered:**
1. **Basic Start/Stop**: Verifies timer starts and callback is invoked
2. **Non-existent Timer**: Verifies stopping a non-existent timer returns false
3. **Timer Replacement**: Verifies starting a timer with an existing name replaces the old timer
4. **Multiple Timers**: Verifies multiple timers can run concurrently
5. **Short Period**: Verifies timers work with very short periods (50ms)
6. **Immediate Stop**: Verifies stopping a timer immediately after starting prevents callback execution

**How to Run:**
```bash
# From the console
.run scripts/test/test_thread_timer.ebs

# Or from command line
java -cp target/classes com.eb.script.Run scripts/test/test_thread_timer.ebs
```

**Expected Output:**
All tests should pass (✓). If any test fails (✗), the output will indicate which test failed and why.

## Manual Testing

For interactive testing, you can use the examples in the `scripts/examples/` directory:

- `thread_timer_basic.ebs` - Simple countdown timer
- `thread_timer_multiple.ebs` - Multiple concurrent timers
- `thread_timer_animation.ebs` - Text-based animation

## Test Characteristics

### Timing Considerations
- Tests use `thread.sleep()` to wait for timer callbacks
- Sleep durations are set to allow sufficient time for callbacks to execute
- Tests may take a few seconds to complete due to timing requirements

### Thread Safety
- All timer operations are thread-safe
- Multiple timers can be started, stopped, and replaced without interference
- Callbacks execute on the JavaFX Application Thread for UI safety

### Error Handling
- Invalid timer names are handled gracefully
- Stopping non-existent timers returns false without error
- Timer replacement is automatic and safe

## Debugging Failed Tests

If a test fails:

1. **Check timing**: Increase sleep durations if callbacks aren't executing
2. **Check callback names**: Ensure callback function names match exactly (case-insensitive)
3. **Check for errors**: Look for error messages in console output
4. **Verify cleanup**: Ensure previous test runs didn't leave timers running

## Adding New Tests

When adding new timer tests:

1. Create descriptive test names
2. Use boolean flags or counters to verify callback execution
3. Always stop timers when done (in callback or after test)
4. Use appropriate sleep durations for timing verification
5. Print clear pass/fail messages

## Known Limitations

- Timer accuracy depends on system load and JVM scheduling
- Very short periods (< 50ms) may have variable execution timing
- Tests assume single-threaded script execution (except timer callbacks)
