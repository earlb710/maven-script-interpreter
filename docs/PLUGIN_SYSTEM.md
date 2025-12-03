# External Java Function Plugin System

This document describes how to create and use external Java functions (plugins) in EBS scripts.

## Overview

The EBS plugin system allows you to extend the interpreter with custom Java functionality without modifying the core codebase. External Java classes that implement the `EbsFunction` interface can be loaded at runtime and called from EBS scripts using the `#custom.functionName(...)` syntax.

## Quick Start

### 1. Create a Java class that implements EbsFunction

```java
package com.example;

import com.eb.script.interpreter.plugin.EbsFunction;
import java.util.Map;

public class MyCustomFunction implements EbsFunction {
    
    @Override
    public String getName() {
        return "My Custom Function";
    }
    
    @Override
    public String getDescription() {
        return "Processes input and returns a result";
    }
    
    @Override
    public Object execute(Object[] args) throws Exception {
        if (args.length == 0) {
            return "No arguments provided";
        }
        // Process arguments and return result
        return "Processed: " + args[0].toString();
    }
}
```

### 2. Compile and add to classpath

Compile your class and ensure it's on the classpath when running the EBS interpreter:

```bash
# Compile
javac -cp ScriptInterpreter/target/classes MyCustomFunction.java

# Run with the class on classpath
java -cp "ScriptInterpreter/target/classes:." com.eb.ui.cli.MainApp
```

### 3. Use in EBS script

```javascript
// Load the plugin with an alias
#plugin.load("com.example.MyCustomFunction", "myFunc");

// Call it using #custom.alias(...) syntax
var result = #custom.myFunc("Hello World");
print result;  // Outputs: Processed: Hello World

// Unload when done (optional - cleanup on exit)
#plugin.unload("myFunc");
```

## EbsFunction Interface

The `EbsFunction` interface defines the contract for plugin functions:

```java
public interface EbsFunction {
    // Required methods
    String getName();                           // Display name for logging
    String getDescription();                    // Description for help text
    Object execute(Object[] args) throws Exception;  // Main execution method
    
    // Optional methods (have default implementations)
    default void initialize(Map<String, Object> config) {}  // Called on load
    default void cleanup() {}                               // Called on unload
    default Map<String, Object> getSignature() { return null; }  // Parameter/return type metadata
}
```

## Providing Function Signature Metadata

Plugins can optionally provide JSON-style metadata about their parameters and return type via the `getSignature()` method. This information is included in `plugin.info()` results and can be used for documentation and validation.

```java
@Override
public Map<String, Object> getSignature() {
    Map<String, Object> sig = new LinkedHashMap<>();
    
    // Define parameters
    List<Map<String, Object>> params = new ArrayList<>();
    
    Map<String, Object> param1 = new LinkedHashMap<>();
    param1.put("name", "input");
    param1.put("type", "string");       // Type: string, int, long, float, double, bool, json, array, any
    param1.put("required", true);
    param1.put("description", "The input text to process");
    params.add(param1);
    
    Map<String, Object> param2 = new LinkedHashMap<>();
    param2.put("name", "options");
    param2.put("type", "json");
    param2.put("required", false);
    param2.put("description", "Optional configuration object");
    params.add(param2);
    
    sig.put("parameters", params);
    sig.put("returnType", "string");
    sig.put("returnDescription", "The processed result");
    
    return sig;
}
```

The signature information is returned when calling `plugin.info()`:

```javascript
#plugin.load("com.example.MyFunction", "myFunc");
var info = #plugin.info("myFunc");

// Access signature metadata
var sig = #json.get(info, "signature");
if sig != null then {
    var params = #json.get(sig, "parameters");
    var returnType = #json.getString(sig, "returnType", "any");
    print "Return type: " + returnType;
}
```

## Calling Custom Functions

Once a plugin is loaded with `plugin.load`, you can call it using the `#custom.alias(...)` syntax:

```javascript
// Load the plugin
#plugin.load("com.example.MyFunction", "myFunc");

// Call using #custom.alias syntax
var result = #custom.myFunc("arg1", 42, true);
print result;
```

This is consistent with how other EBS builtins are called (e.g., `#str.toUpper`, `#json.get`).

## Plugin Management Functions

### plugin.load(className, alias, config?)

Loads a Java class as a plugin function.

**Parameters:**
- `className` (string, required): Fully qualified Java class name
- `alias` (string, required): Name to reference via `#custom.alias(...)`
- `config` (json, optional): Configuration object passed to `initialize()`

**Returns:** `true` if loaded successfully

**Example:**
```javascript
// Basic load
#plugin.load("com.example.MyFunction", "myFunc");

// Load with configuration
#plugin.load("com.example.MyFunction", "myFunc", {"option": "value"});
```

### plugin.isLoaded(alias)

Checks if a plugin is currently loaded.

**Parameters:**
- `alias` (string, required): The alias to check

**Returns:** `true` if loaded, `false` otherwise

**Example:**
```javascript
if #plugin.isLoaded("myFunc") then {
    print "Plugin is loaded";
}
```

### plugin.unload(alias)

Unloads a plugin and calls its `cleanup()` method.

**Parameters:**
- `alias` (string, required): The alias to unload

**Returns:** `true` if unloaded, `false` if wasn't loaded

**Example:**
```javascript
#plugin.unload("myFunc");
```

### plugin.list()

Lists all currently loaded plugins.

**Parameters:** None

**Returns:** Array of loaded plugin aliases

**Example:**
```javascript
var plugins = #plugin.list();
foreach p in plugins {
    print "Loaded plugin: " + p;
}
```

### plugin.info(alias)

Gets detailed information about a loaded plugin.

**Parameters:**
- `alias` (string, required): The alias to get info for

**Returns:** JSON object with plugin information, or null if not loaded

**Example:**
```javascript
var info = #plugin.info("myFunc");
if info != null then {
    print "Name: " + #json.getString(info, "name", "");
    print "Description: " + #json.getString(info, "description", "");
    print "Class: " + #json.getString(info, "className", "");
}
```

## Argument Types

When calling plugin functions, EBS values are converted to Java objects:

| EBS Type | Java Type |
|----------|-----------|
| string | `String` |
| int | `Integer` |
| long | `Long` |
| float | `Float` |
| double | `Double` |
| bool | `Boolean` |
| json (object) | `Map<String, Object>` |
| json (array) | `List<Object>` |
| array | `ArrayDef<?, ?>` |

Return values from `execute()` can be any of these types and will be properly handled by the interpreter.

## Error Handling

Plugin errors are handled through EBS's exception system:

```javascript
try {
    #plugin.load("com.example.NonExistent", "test");
} exceptions {
    when ANY_ERROR(msg) {
        print "Failed to load plugin: " + msg;
    }
}
```

Common errors:
- Class not found (not on classpath)
- Class doesn't implement `EbsFunction`
- No public no-arg constructor
- Alias already in use
- Plugin not loaded (when calling #custom.alias)

## Built-in Example

An example plugin is included in the interpreter for testing:

```javascript
// Load the built-in example
#plugin.load("com.eb.script.interpreter.plugin.ExampleEbsFunction", "echo");

// Call it using #custom.echo(...)
var result = #custom.echo("Hello", "World");
print result;  // Outputs: [Echo] Hello World

// With custom prefix
#plugin.unload("echo");
#plugin.load("com.eb.script.interpreter.plugin.ExampleEbsFunction", "echo", {"prefix": "[Custom]"});
var result2 = #custom.echo("Test");
print result2;  // Outputs: [Custom] Test
```

## Thread Safety

If your plugin may be called from multiple screen threads simultaneously, ensure thread-safety in your implementation:

```java
public class ThreadSafePlugin implements EbsFunction {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public Object execute(Object[] args) {
        return counter.incrementAndGet();
    }
    // ... other methods
}
```

## Best Practices

1. **Validate arguments**: Check argument count and types in `execute()`
2. **Handle nulls**: Arguments may be null - handle gracefully
3. **Clean up resources**: Use `cleanup()` to close files, connections, etc.
4. **Use configuration**: Accept options via `initialize(config)`
5. **Document your plugin**: Provide clear `getName()` and `getDescription()`
6. **Be thread-safe**: Use thread-safe data structures if needed
7. **Handle errors**: Throw meaningful exceptions with clear messages

## Example: Database Lookup Plugin

Here's a more complete example of a plugin that performs database lookups:

```java
package com.example;

import com.eb.script.interpreter.plugin.EbsFunction;
import java.sql.*;
import java.util.*;

public class DbLookupPlugin implements EbsFunction {
    
    private Connection conn;
    
    @Override
    public String getName() {
        return "Database Lookup";
    }
    
    @Override
    public String getDescription() {
        return "Performs quick database lookups";
    }
    
    @Override
    public void initialize(Map<String, Object> config) throws Exception {
        if (config == null) {
            throw new IllegalArgumentException("Config required with 'url', 'user', 'password'");
        }
        String url = (String) config.get("url");
        String user = (String) config.get("user");
        String password = (String) config.get("password");
        conn = DriverManager.getConnection(url, user, password);
    }
    
    @Override
    public Object execute(Object[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: table, keyValue");
        }
        String table = (String) args[0];
        Object keyValue = args[1];
        
        String sql = "SELECT * FROM " + table + " WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, keyValue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    return row;
                }
            }
        }
        return null;
    }
    
    @Override
    public void cleanup() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
}
```

Usage in EBS:
```javascript
#plugin.load("com.example.DbLookupPlugin", "dbLookup", {
    "url": "jdbc:oracle:thin:@localhost:1521:xe",
    "user": "myuser",
    "password": "mypass"
});

var user = #custom.dbLookup("users", 123);
if user != null then {
    print "Found: " + #json.getString(user, "name", "Unknown");
}

#plugin.unload("dbLookup");
```
