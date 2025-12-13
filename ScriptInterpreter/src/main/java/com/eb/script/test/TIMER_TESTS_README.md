# Timer Functionality Test Suite

This directory contains comprehensive tests for the timer functionality in the EBS scripting language.

## Test Files

### 1. TestTimedUtility.java
Tests the core `Timed` utility class functionality:
- Basic timer start/stop operations
- Timer reset functionality
- Timer continue/lap functionality
- Running state tracking
- Period tracking methods
- String formatting (milliseconds and seconds)
- Decimal precision control (0-3 decimal places)
- Edge cases and error handling

**Total Tests:** 34 assertions across 8 test categories

### 2. TestTimerBuiltins.java
Tests the timer builtins available in EBS scripts:
- Basic start/stop operations
- Auto-initialization (no null pointer errors)
- Period tracking while running
- Formatted string output
- Timer state management
- Continue/lap functionality
- Multiple concurrent timers
- Bulk clear operation
- Thread safety (10 threads, 100 operations each)
- Edge cases (empty IDs, long IDs, special characters)
- Error handling (invalid parameters, missing arguments)

**Total Tests:** 47 assertions across 11 test categories

### 3. RunTimerTests.java
Master test runner that executes both test suites sequentially.

## Running the Tests

### Option 1: Run All Tests Together
```bash
cd ScriptInterpreter
mvn clean compile
java -cp target/classes com.eb.script.test.RunTimerTests
```

### Option 2: Run Individual Test Suites
```bash
cd ScriptInterpreter
mvn clean compile

# Run Timed utility tests only
java -cp target/classes com.eb.script.test.TestTimedUtility

# Run Timer builtins tests only
java -cp target/classes com.eb.script.test.TestTimerBuiltins
```

## Test Coverage

### Timed Utility Class
- ✅ Start/stop operations
- ✅ Reset functionality
- ✅ Continue/lap timing
- ✅ Running state management
- ✅ Period tracking (running and stopped)
- ✅ String formatting with zero-padding
- ✅ Decimal precision control (0-3 decimals)
- ✅ Error handling for invalid parameters
- ✅ Edge cases (very short durations, restart behavior)

### Timer Builtins
- ✅ `timer.start(timerId)` - Start/restart timer
- ✅ `timer.stop(timerId)` - Stop and get elapsed time
- ✅ `timer.getPeriod(timerId)` - Get elapsed time
- ✅ `timer.getPeriodString(timerId [, decimals])` - Get formatted time
- ✅ `timer.isRunning(timerId)` - Check running state
- ✅ `timer.reset(timerId)` - Reset timer
- ✅ `timer.continue(timerId)` - Continue/lap timing
- ✅ `timer.getContinuePeriod(timerId)` - Get continue period
- ✅ `timer.getContinuePeriodString(timerId [, decimals])` - Get formatted continue period
- ✅ `timer.remove(timerId)` - Remove timer
- ✅ `timer.clear()` - Remove all timers
- ✅ Auto-initialization behavior
- ✅ Thread-safe concurrent access
- ✅ Type conversion and validation

## Expected Output

When all tests pass, you should see:
```
╔════════════════════════════════════════════════════════════╗
║        Timer Functionality - Complete Test Suite          ║
╚════════════════════════════════════════════════════════════╝

[... test output ...]

╔════════════════════════════════════════════════════════════╗
║              ALL TIMER TESTS PASSED! ✓                     ║
╚════════════════════════════════════════════════════════════╝
```

## Test Assertions

Total: **81 assertions** across all test suites
- TestTimedUtility: 34 assertions
- TestTimerBuiltins: 47 assertions

## Notes

- Tests use `Thread.sleep()` for timing, so results may vary slightly based on system load
- Tests include tolerance ranges (e.g., ±10ms) to account for timing variations
- Thread safety tests run 10 concurrent threads with 100 operations each (1000 total operations)
- All tests clean up after themselves by removing created timers
