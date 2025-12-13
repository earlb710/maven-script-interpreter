# Timer Builtins Test Script

## Overview
`test_timer_builtins.ebs` is a comprehensive EBS test script that validates all timer builtin functions. This script serves both as a test suite and as a syntax reference for using timer functions in EBS scripts.

## Location
```
ScriptInterpreter/scripts/test/test_timer_builtins.ebs
```

## Purpose
- **Functional Testing**: Validates all 11 timer builtin functions work correctly
- **Syntax Reference**: Demonstrates proper EBS syntax for using timer functions
- **Integration Testing**: Tests timer functionality in a real EBS script environment
- **Documentation**: Shows practical examples of each timer function

## Running the Test

### From the Console
Load and run the script in the EBS interactive console:
```
> load scripts/test/test_timer_builtins.ebs
> run
```

### From Command Line (if supported)
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test/test_timer_builtins.ebs
```

## Test Coverage

The script includes **26 test assertions** covering:

### 1. Basic Operations (2 tests)
- `timer.start()` - Starting a timer and verifying return value
- `timer.stop()` - Stopping a timer and checking elapsed time

### 2. Auto-initialization (3 tests)
- `timer.getPeriod()` on non-existent timer (returns 0)
- `timer.isRunning()` on non-existent timer (returns false)
- `timer.stop()` on non-existent timer (returns 0)

### 3. Period Tracking (1 test)
- `timer.getPeriod()` increases while timer is running

### 4. String Formatting (1 test)
- `timer.getPeriodString()` with different decimal precisions (0-3)
- Default format includes decimal point

### 5. State Management (2 tests)
- `timer.isRunning()` returns true when running
- `timer.isRunning()` returns false when stopped

### 6. Reset Functionality (2 tests)
- `timer.reset()` returns true
- Period decreases after reset

### 7. Continue/Lap Timing (3 tests)
- `timer.continue()` returns true
- Total period increases after continue
- Continue period tracks time since last continuation

### 8. Continue Period String (1 test)
- `timer.getContinuePeriodString()` formats correctly

### 9. Multiple Timers (1 test)
- Multiple concurrent timers track independently
- Timers maintain correct relative durations

### 10. Remove Operation (2 tests)
- `timer.remove()` returns true for existing timer
- `timer.remove()` returns false for non-existent timer

### 11. Bulk Clear (2 tests)
- `timer.clear()` removes all timers
- `timer.clear()` on empty registry returns 0

### 12. Edge Cases - Empty ID (1 test)
- Empty string timer ID works correctly

### 13. Edge Cases - Special Characters (1 test)
- Special characters in timer ID work correctly

## Timer Builtins Tested

| Builtin | Description | Return Type |
|---------|-------------|-------------|
| `timer.start(timerId)` | Start or restart a timer | String |
| `timer.stop(timerId)` | Stop timer and get elapsed time | Long |
| `timer.getPeriod(timerId)` | Get elapsed time without stopping | Long |
| `timer.getPeriodString(timerId [, decimals])` | Get formatted elapsed time | String |
| `timer.isRunning(timerId)` | Check if timer is running | Boolean |
| `timer.reset(timerId)` | Reset timer to current time | Boolean |
| `timer.continue(timerId)` | Continue timer (lap timing) | Boolean |
| `timer.getContinuePeriod(timerId)` | Get time since last continue | Long |
| `timer.getContinuePeriodString(timerId [, decimals])` | Get formatted continue period | String |
| `timer.remove(timerId)` | Remove timer from registry | Boolean |
| `timer.clear()` | Remove all timers | Integer |

## Expected Output

When run successfully, the script outputs:
```
╔════════════════════════════════════════════════════════════╗
║          Timer Builtins - Comprehensive Test Suite        ║
╚════════════════════════════════════════════════════════════╝

TEST 1: Basic timer.start() and timer.stop()
  ✓ timer.start() returns correct timer ID
  ✓ timer.stop() returned elapsed time: 100ms

[... additional test output ...]

╔════════════════════════════════════════════════════════════╗
║                    Test Results Summary                   ║
╚════════════════════════════════════════════════════════════╝

Tests Passed: 26
Tests Failed: 0
Total Tests:  26

✓ All timer builtin tests passed successfully!

=== Timer Builtins Test Complete ===
```

## EBS Syntax Features Demonstrated

The test script demonstrates proper EBS syntax for:

1. **Variable Declarations**
   ```ebs
   var timerId : string = call timer.start("test");
   var elapsed : long = call timer.stop("test");
   var isRunning : bool = call timer.isRunning("test");
   var count : int = call timer.clear();
   ```

2. **Function Calls**
   ```ebs
   call timer.start("myTimer");
   var result : type = call timer.function("timerId");
   ```

3. **Conditional Statements**
   ```ebs
   if condition then {
       // statements
   } else {
       // statements
   }
   ```

4. **String Concatenation**
   ```ebs
   print "Result: " + variable;
   ```

5. **While Loops** (correct syntax)
   ```ebs
   while condition {
       // statements
   }
   ```
   Note: NOT `while condition do {` - the `do` keyword is only for do-while loops

6. **Logical Operators**
   ```ebs
   if value >= min and value <= max then {
       // in range
   }
   ```

## Validation

✅ Script parses successfully with no syntax errors
✅ All timer functions are tested
✅ Auto-initialization behavior is verified
✅ Edge cases are covered
✅ Thread-safe operation (multiple timers)
✅ Proper EBS syntax throughout

## Related Files

- **Java Tests**: `ScriptInterpreter/src/main/java/com/eb/script/test/TestTimerBuiltins.java`
- **Example Script**: `ScriptInterpreter/scripts/examples/timer.ebs`
- **Documentation**: `ScriptInterpreter/src/main/resources/help-lookup.json` (timer.* entries)
- **Test Documentation**: `ScriptInterpreter/src/main/java/com/eb/script/test/TIMER_TESTS_README.md`

## Notes

- Test timing may vary slightly based on system load (uses `sleep()` for delays)
- Tests include tolerance ranges (e.g., ±10ms) for timing checks
- All tests clean up timers after use to avoid interference
- Auto-initialization ensures no null pointer errors occur
