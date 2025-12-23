# Logging Setup Guide

## Overview

The EBS Script Interpreter provides a built-in logging system that allows you to capture debug information, trace script execution, and log custom messages to files or the console. This guide explains how to set up and use the logging features effectively.

## Table of Contents

- [Quick Start](#quick-start)
- [Logging Functions](#logging-functions)
- [Setting Up File Logging](#setting-up-file-logging)
- [Log Levels](#log-levels)
- [Log Format](#log-format)
- [Common Patterns](#common-patterns)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Quick Start

The simplest way to start logging is to enable debug mode and specify a log file:

```ebs
// Enable debug mode
debug.on();

// Set the log file (appends to existing file)
debug.file("logs/myapp.log");

// Log a message
debug.log("INFO", "Application started");

// Your code here...

// Log another message
debug.log("DEBUG", "Processing complete");

// Disable debug mode when done (writes final line count and resets counter)
debug.off();
```

**Note:** When you call `debug.off()`, it automatically writes a final message showing the total number of lines logged (e.g., "debug off: 3 lines written") and resets the line counter.

## Logging Functions

The EBS logging system provides several built-in functions:

### debug.on()

Enables debug logging. When enabled, calls to `debug.log()` will output messages.

```ebs
debug.on();
```

**Returns:** `BOOL` - true if debug is now enabled

### debug.off()

Disables debug logging. Messages logged via `debug.log()` will be ignored.

When debug is turned off, a final message is automatically written to the log file (if logging to a file) showing the total number of lines written, then the line counter is reset to zero.

```ebs
debug.off();
```

**Returns:** `BOOL` - false (debug is now disabled)

**Behavior:**
- Writes a final log entry: `"debug off: X lines written"` where X is the total line count
- Resets the line counter to zero
- Subsequent calls to `debug.log()` are ignored until `debug.on()` is called again

**Example output in log file:**
```
[2025-12-23T06:03:34.363283723]	[INFO]	debug off: 5 lines written
```

### debug.file(fileName)

Sets the log file path. Logs will be appended to this file. If the file doesn't exist, it will be created. Parent directories will be created automatically.

```ebs
debug.file("logs/debug.log");
debug.file("/tmp/myapp-debug.log");
```

**Parameters:**
- `fileName` (STRING) - Path to the log file (relative or absolute)

**Notes:**
- The path is sandboxed according to the safe directories configuration
- If fileName is null, logging switches back to stdout
- The file is opened in append mode, so previous logs are preserved

### debug.newFile(fileName)

Creates a new log file, truncating any existing content. Use this when you want to start with a fresh log file.

```ebs
debug.newFile("logs/session.log");
```

**Parameters:**
- `fileName` (STRING) - Path to the log file (relative or absolute)

**Notes:**
- Existing file content will be overwritten
- Use this at the start of a new session or test run
- Parent directories will be created automatically

### debug.log(level, message)

Writes a timestamped log entry with the specified level and message.

```ebs
debug.log("INFO", "User logged in");
debug.log("ERROR", "Failed to connect to database");
debug.log("DEBUG", "Variable value: " + string.tostr(myVar));
```

**Parameters:**
- `level` (STRING) - Log level (e.g., "INFO", "DEBUG", "ERROR", "WARN")
- `message` (STRING) - The message to log

**Returns:** `BOOL` - true if logging is enabled and the message was written

### debug.traceon() / debug.traceoff()

Enables or disables trace mode for detailed execution tracking. This is primarily for internal debugging.

```ebs
debug.traceon();
// ... code to trace
debug.traceoff();
```

### debug.linesWritten()

Returns the total number of log lines written since the debugger was created or since the last reset.

```ebs
var count: long = call debug.linesWritten();
print "Total lines logged: " + string.tostr(count);
```

**Returns:** `LONG` - The total number of lines written

**Notes:**
- Counter is maintained per script execution (per Environment/Debugger instance)
- Counter increments for each call to `debug.log()`, `debugWriteStart()`, and `debugWriteEnd()`
- Includes lines written to both files and console

### debug.resetLineCount()

Resets the line counter back to zero.

```ebs
call debug.resetLineCount();
var count: long = call debug.linesWritten();  // Will be 0
```

**Notes:**
- Useful for measuring log output for specific sections of code
- Does not affect the actual log file content, only the internal counter
- Note: `debug.off()` also automatically resets the counter after writing the final line count

### echo.on() / echo.off()

Controls whether console commands and script output are echoed to the console.

```ebs
echo.on();   // Enable console echo
echo.off();  // Disable console echo
```

## Setting Up File Logging

### Basic File Logging

To set up logging to a file, follow this pattern:

```ebs
// 1. Enable debug mode
debug.on();

// 2. Set the log file
debug.file("logs/application.log");

// 3. Log messages throughout your code
debug.log("INFO", "Application initialized");

// ... your application code ...

// 4. Optional: disable debug when done
debug.off();
```

### Starting a Fresh Log File

To start with a clean log file each time your script runs:

```ebs
debug.on();
debug.newFile("logs/session-" + date.now("yyyyMMdd-HHmmss") + ".log");
debug.log("INFO", "=== New session started ===");
```

### Multiple Log Files

You can switch between log files during execution:

```ebs
debug.on();

// Log startup to main log
debug.file("logs/main.log");
debug.log("INFO", "Application started");

// Switch to error log for error handling
debug.file("logs/errors.log");
debug.log("ERROR", "Error occurred: " + errorMsg);

// Switch back to main log
debug.file("logs/main.log");
debug.log("INFO", "Continuing execution");
```

### Switching to Console Output

To switch from file logging back to console output:

```ebs
// Start with file logging
debug.on();
debug.file("logs/debug.log");
debug.log("INFO", "Logging to file");

// Switch to console (stdout)
debug.file(null);
debug.log("INFO", "Now logging to console");
```

## Log Levels

While the EBS logging system doesn't enforce specific log levels, the following conventions are recommended:

| Level | Purpose | Example Use Cases |
|-------|---------|-------------------|
| **DEBUG** | Detailed debugging information | Variable values, function entry/exit, loop iterations |
| **INFO** | Informational messages | Application start/stop, major milestones, user actions |
| **WARN** | Warning messages | Deprecated features, non-critical issues, fallback behavior |
| **ERROR** | Error messages | Exceptions, failed operations, invalid data |
| **FATAL** | Critical errors | Unrecoverable errors, system failures |
| **TRACE** | Very detailed tracing | Low-level debugging, performance tracking |

Example:

```ebs
debug.on();
debug.file("logs/app.log");

debug.log("INFO", "Starting data processing");
debug.log("DEBUG", "Processing " + string.tostr(count) + " records");

if error then {
    debug.log("ERROR", "Failed to process record: " + errorMsg);
}

if useDeprecatedAPI then {
    debug.log("WARN", "Using deprecated API, please update code");
}
```

## Log Format

Logs are written with the following format:

```
[TIMESTAMP]    [LEVEL]    MESSAGE
```

Example output:

```
[2024-12-22T17:30:15.123]    [INFO]    Application started
[2024-12-22T17:30:15.456]    [DEBUG]    Loading configuration file
[2024-12-22T17:30:16.789]    [ERROR]    Failed to connect: Connection timeout
```

### Timestamp Format

Timestamps are in ISO 8601 format: `yyyy-MM-ddTHH:mm:ss.SSS`

### Indentation

The logging system supports automatic indentation for nested operations, which can help visualize the call stack and execution flow.

## Common Patterns

### Pattern 1: Application Lifecycle Logging

```ebs
debug.on();
debug.file("logs/app.log");

debug.log("INFO", "=== Application Starting ===");
debug.log("INFO", "Version: 1.0.0");
debug.log("INFO", "Environment: " + environment);

// Application code here

debug.log("INFO", "=== Application Shutting Down ===");
debug.off();
```

### Pattern 2: Error Logging with Context

```ebs
debug.on();
debug.file("logs/errors.log");

try {
    // Operation that might fail
    result = performOperation(data);
} catch ex {
    debug.log("ERROR", "Operation failed");
    debug.log("ERROR", "  Input: " + string.tostr(data));
    debug.log("ERROR", "  Exception: " + string.tostr(ex));
    debug.log("ERROR", "  Timestamp: " + date.now());
}
```

### Pattern 3: Performance Logging

```ebs
debug.on();
debug.file("logs/performance.log");

startTime = time.now();

// Operation to measure
processLargeDataset(dataset);

endTime = time.now();
elapsed = endTime - startTime;

debug.log("INFO", "Dataset processing completed in " + string.tostr(elapsed) + "ms");
debug.log("DEBUG", "Records processed: " + string.tostr(recordCount));
debug.log("DEBUG", "Average time per record: " + string.tostr(elapsed / recordCount) + "ms");
```

### Pattern 4: Conditional Logging

```ebs
// Set debug based on configuration
var debugMode: string = config.get("debugMode");

if debugMode == "enabled" then {
    debug.on();
    debug.file("logs/debug.log");
}

// Your code with logging
// Note: There's no debug.isDebugOn() builtin; check if logging is working by trying to log
debug.log("DEBUG", "Detailed debug information here");
```

### Pattern 5: Rotating Log Files by Date

```ebs
debug.on();

// Create log file with date in filename
var logFileName: string = "logs/app-" + date.now("yyyy-MM-dd") + ".log";
debug.file(logFileName);

debug.log("INFO", "Logging to: " + logFileName);
```

### Pattern 6: Tracking Log Volume

```ebs
call debug.on();
call debug.file("logs/app.log");

// Reset counter at start
call debug.resetLineCount();

// Your application code with logging
for var i: int = 0; i < recordCount; i++ {
    call debug.log("DEBUG", "Processing record " + string.tostr(i));
    call processRecord(i);
}

// Report how many lines were logged
var linesLogged: long = call debug.linesWritten();
call debug.log("INFO", "Total log lines written: " + string.tostr(linesLogged));
print "Logged " + string.tostr(linesLogged) + " lines to file";
```

### Pattern 7: Screen Event Logging

```ebs
// In a screen event handler
onClick = logButtonClick(event) return void {
    debug.on();
    debug.file("logs/screen-events.log");
    
    debug.log("INFO", "Button clicked: " + event.source);
    debug.log("DEBUG", "Screen: " + screenName);
    debug.log("DEBUG", "Timestamp: " + date.now());
    
    // Handle the click
    call processClick(event);
}
```

## Best Practices

### 1. Enable Logging Early

Enable logging at the start of your script so you capture all relevant information:

```ebs
// First thing in your script
debug.on();
debug.file("logs/app.log");
debug.log("INFO", "=== Script execution started ===");

// Rest of your script...
```

### 2. Use Appropriate Log Levels

Choose log levels consistently:
- Use **DEBUG** for verbose information you only need during development
- Use **INFO** for important milestones and state changes
- Use **WARN** for issues that don't prevent operation
- Use **ERROR** for failures that need attention

### 3. Include Context in Log Messages

Make log messages self-explanatory:

```ebs
// Good: Includes context
debug.log("ERROR", "Failed to load user profile for userId=" + userId + ": File not found");

// Poor: Lacks context
debug.log("ERROR", "File not found");
```

### 4. Log Exceptions and Errors

Always log exceptions with full details:

```ebs
} catch ex {
    debug.log("ERROR", "Exception in processData()");
    debug.log("ERROR", "  Type: " + string.tostr(ex));
    debug.log("ERROR", "  Message: " + string.tostr(ex));
}
```

### 5. Use Structured Log Messages

For easier parsing and analysis, use structured formats:

```ebs
// Key-value format
debug.log("INFO", "action=login, user=" + username + ", status=success, duration=" + duration + "ms");

// Delimiter-separated format
debug.log("INFO", "LOGIN | " + username + " | " + sessionId + " | " + timestamp);
```

### 6. Don't Log Sensitive Information

Avoid logging passwords, API keys, or personal information:

```ebs
// Bad: Logs password
debug.log("DEBUG", "Login attempt: username=" + username + ", password=" + password);

// Good: Doesn't log password
debug.log("DEBUG", "Login attempt: username=" + username);
```

### 7. Create Log Directories

Organize logs in directories by application, date, or purpose:

```ebs
debug.file("logs/2024/12/application.log");
debug.file("logs/errors/critical-errors.log");
debug.file("logs/" + appName + "/activity.log");
```

### 8. Use newFile() for Test Runs

When running tests, start with a fresh log file:

```ebs
// Test script
debug.on();
debug.newFile("logs/test-run-" + date.now("yyyyMMdd-HHmmss") + ".log");
debug.log("INFO", "=== Test execution started ===");

// Run tests...
```

### 9. Disable Debug in Production

For production scripts, control logging via configuration:

```ebs
if environment == "development" or environment == "testing" then {
    debug.on();
    debug.file("logs/dev-debug.log");
} else {
    debug.off();  // Disable in production
}
```

### 10. Close Logging Gracefully

At the end of your script, log completion and disable debug:

```ebs
debug.log("INFO", "=== Script execution completed successfully ===");
debug.off();
```

## Troubleshooting

### Logs Not Appearing in File

**Problem:** Calling `debug.log()` but nothing appears in the file.

**Solutions:**
1. Verify debug mode is enabled:
   ```ebs
   debug.on();  // Must be called before logging
   ```

2. Check the file path:
   ```ebs
   debug.file("logs/debug.log");  // Ensure path is correct
   ```

3. Verify the directory exists or can be created:
   ```ebs
   // The logging system creates parent directories automatically,
   // but ensure you have write permissions
   ```

4. Check safe directories configuration:
   ```ebs
   // The log path must be within a safe directory
   // Use console command: /safe-dirs
   // Or check: ScriptInterpreter/console.cfg
   ```

### Log File Path Issues

**Problem:** "File not found" or permission errors.

**Solutions:**
1. Use absolute paths to avoid ambiguity:
   ```ebs
   debug.file("/home/user/myapp/logs/debug.log");
   ```

2. Or use relative paths from the script location:
   ```ebs
   debug.file("logs/debug.log");
   ```

3. Check file permissions:
   ```bash
   # Ensure the directory is writable
   ls -la logs/
   ```

### Log File Not Being Created

**Problem:** The log file directory doesn't exist.

**Solution:** The logging system creates parent directories automatically. If it fails, check:
- Write permissions on the parent directory
- Disk space availability
- Safe directories configuration

### Logs Appearing in Console Instead of File

**Problem:** Logs go to stdout even though a file is specified.

**Solutions:**
1. Verify the file path is set correctly:
   ```ebs
   debug.on();
   debug.file("logs/debug.log");  // Not null
   ```

2. Check if file creation failed (permissions issue). The system falls back to stdout on errors.

### Performance Impact

**Problem:** Logging slows down the application.

**Solutions:**
1. Disable logging in performance-critical sections:
   ```ebs
   debug.off();
   // Performance-critical code
   debug.on();
   ```

2. Use conditional logging:
   ```ebs
   if detailedLoggingEnabled then {
       debug.log("DEBUG", call expensiveDebugInfo());
   }
   ```

3. Reduce log level (use fewer DEBUG messages):
   ```ebs
   // Only log INFO and above in production
   debug.log("INFO", "Important event");
   ```

### Mixing File and Console Output

**Problem:** Need logs in both file and console.

**Solution:** Write to file and also print to console:
```ebs
debug.on();
debug.file("logs/debug.log");

var message: string = "Important event occurred";
debug.log("INFO", message);
print message;  // Also print to console
```

## Advanced Topics

### Safe Directories

Log files must be written within configured safe directories. To view or configure safe directories:

1. **Console command:**
   ```
   /safe-dirs
   ```

2. **Configuration file:**
   Edit `ScriptInterpreter/console.cfg`:
   ```properties
   # Add your log directory to the sandbox
   sandbox.roots=/path/to/your/logs,/path/to/project
   ```

3. **In script:**
   Log files are automatically sandboxed via `Util.resolveSandboxedPath()`

### Integration with Screen Applications

When using screens, you can log screen events and variable changes:

```ebs
// Screen definition with logging
screen myScreen {
    vars {
        string debugEnabled = "true";
    }
    
    events {
        onShow = logScreenShow() return void {
            if myScreen.debugEnabled == "true" then {
                debug.on();
                debug.file("logs/screen-events.log");
                debug.log("INFO", "Screen 'myScreen' shown");
            }
        }
        
        onClick = logClick(event) return void {
            if myScreen.debugEnabled == "true" then {
                debug.log("DEBUG", "Button clicked: " + event.source);
            }
        }
    }
}
```

### Logging in Functions

Use logging to trace function execution:

```ebs
processData(data) return bool {
    debug.log("DEBUG", "Entering processData(), data size=" + string.tostr(array.length(data)));
    
    // Function logic...
    
    debug.log("DEBUG", "Exiting processData(), result=" + string.tostr(success));
    return success;
}
```

### Logging with Assertions

Combine logging with assertions for testing:

```ebs
debug.on();
debug.file("logs/test.log");

// Assert and log result
var result: bool = debug.assert(value > 0, "Value must be positive");
if not result then {
    debug.log("ERROR", "Assertion failed: value=" + string.tostr(value));
}

// Assert equality and log
var equal: bool = debug.assertEquals(expected, actual, "Values should match");
```

## Examples

### Complete Application Logging Example

```ebs
// Application with comprehensive logging

// Setup
debug.on();
debug.file("logs/myapp-" + date.now("yyyyMMdd") + ".log");
debug.log("INFO", "=== MyApp Starting ===");
debug.log("INFO", "Version: 1.2.3");

// Configuration
var configFile: string = "config.json";
debug.log("INFO", "Loading configuration from: " + configFile);

if file.exists(configFile) then {
    var config: json = parseJson(file.readTextFile(configFile));
    debug.log("INFO", "Configuration loaded successfully");
    debug.log("DEBUG", "Config keys: " + string.tostr(json.keys(config)));
} else {
    debug.log("ERROR", "Configuration file not found: " + configFile);
    debug.log("INFO", "Using default configuration");
    var config: json = call getDefaultConfig();
}

// Main processing
debug.log("INFO", "Starting main processing");
var processed: int = 0;
var errors: int = 0;

for var i: int = 0; i < array.length(items); i++ {
    var item = items[i];
    debug.log("DEBUG", "Processing item " + string.tostr(i + 1) + "/" + string.tostr(array.length(items)));
    
    try {
        call processItem(item);
        processed = processed + 1;
    } catch ex {
        errors = errors + 1;
        debug.log("ERROR", "Failed to process item " + string.tostr(i) + ": " + string.tostr(ex));
    }
}

// Summary
debug.log("INFO", "Processing complete");
debug.log("INFO", "  Total items: " + string.tostr(array.length(items)));
debug.log("INFO", "  Processed: " + string.tostr(processed));
debug.log("INFO", "  Errors: " + string.tostr(errors));

// Cleanup
debug.log("INFO", "=== MyApp Shutting Down ===");
debug.off();
```

### Error Logging Example

```ebs
// Dedicated error logging
debug.on();
debug.file("logs/errors.log");

handleError(operation: string, error: string) return void {
    debug.log("ERROR", "=== Error Report ===");
    debug.log("ERROR", "Operation: " + operation);
    debug.log("ERROR", "Error: " + error);
    debug.log("ERROR", "Timestamp: " + date.now());
    debug.log("ERROR", "===================");
}

// Usage
try {
    // Some operation
    var result = call riskyOperation();
} catch ex {
    call handleError("riskyOperation", string.tostr(ex));
}
```

## Summary

The EBS Script Interpreter's logging system provides:

- **Simple API:** Enable with `debug.on()`, write with `debug.log(level, message)`
- **File Output:** Direct logs to files with `debug.file(path)` or `debug.newFile(path)`
- **Console Output:** Log to stdout by setting file to null: `debug.file(null)`
- **Automatic Formatting:** ISO timestamps and structured log format
- **Safe by Default:** Sandboxed file paths for security
- **Flexible:** Switch between files, disable/enable dynamically

Start with the Quick Start example and adapt the patterns to your needs. Use appropriate log levels, include context in messages, and organize logs in directories for easy management.

## Related Documentation

- [README.md](https://github.com/earlb710/maven-script-interpreter/blob/master/README.md) - Project overview
- [EBS_SCRIPT_SYNTAX.md](https://github.com/earlb710/maven-script-interpreter/blob/master/EBS_SCRIPT_SYNTAX.md) - Language syntax reference
- [FUNCTIONS_AND_IMPORTS_GUIDE.md](./FUNCTIONS_AND_IMPORTS_GUIDE.md) - Function usage guide
- [SPACE_RULES.md](https://github.com/earlb710/maven-script-interpreter/blob/master/SPACE_RULES.md) - Project conventions

## Support

For issues or questions about logging:
- Check the [Troubleshooting](#troubleshooting) section
- Review the [Best Practices](#best-practices)
- Consult the source code: `ScriptInterpreter/src/main/java/com/eb/util/Debugger.java`
