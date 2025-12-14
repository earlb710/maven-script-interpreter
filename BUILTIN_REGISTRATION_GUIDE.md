# Builtin Function Registration Guide

## Important: Registration Requirements for Help Documentation

When adding new builtin functions to the EBS Script Interpreter, they must be registered in **TWO** places for full functionality and visibility in help:

### 1. Implementation (Required for Execution)

Implement the function in the appropriate builtins class and add it to the switch case in `Builtins.execute()`:

```java
// In Builtins.java execute() method
case "scr.setTreeItemIcon" -> BuiltinsScreen.screenSetTreeItemIcon(context, args);
```

### 2. Registration (Required for Help System)

**CRITICAL**: Functions must also be registered using `addBuiltin(info(...))` in the static initialization block of `Builtins.java`:

```java
// In Builtins.java static initialization block
addBuiltin(info(
    "scr.setTreeItemIcon", DataType.BOOL,
    newParam("screenName", DataType.STRING, true),  // required
    newParam("itemPath", DataType.STRING, true),    // required
    newParam("iconPath", DataType.STRING, false)    // optional
));
```

## Why Both Are Required

### Without Registration (`addBuiltin`):
- ❌ Function will NOT appear in `system.help()` output
- ❌ Function will NOT be visible in help.ebs tree view
- ❌ Users won't discover the function through help
- ✓ Function will still execute if called directly (if implemented)

### Impact on Help System

The help system (help.ebs) works as follows:
1. Calls `system.help()` to get list of all registered builtins
2. Parses the output to extract builtin names and signatures
3. Displays them in the tree view organized by category

If a function is not registered with `addBuiltin()`, it won't appear in `system.help()` output, and therefore won't be displayed in the help UI.

## Registration Parameters

```java
addBuiltin(info(
    "function.name",        // Function name (e.g., "scr.setTreeItemIcon")
    DataType.RETURN_TYPE,   // Return type (e.g., DataType.BOOL, DataType.STRING)
    newParam("paramName", DataType.TYPE, required)  // Parameters (true=required, false=optional)
));
```

### Common Return Types
- `DataType.BOOL` - Boolean return
- `DataType.STRING` - String return
- `DataType.INTEGER` or `DataType.INT` - Integer return
- `DataType.ANY` - Any type return
- `DataType.JSON` - JSON object return
- `DataType.ARRAY` - Array return
- `null` - No return value (void)

### Parameter Definition
- First argument: parameter name (string)
- Second argument: parameter type (DataType enum)
- Third argument: required flag (true = required, false = optional)

## Example: TreeItem Functions

The TreeItem functions were initially:
- ✓ Implemented in BuiltinsScreen.java
- ✓ Documented in help-lookup.json
- ✓ Handled in Builtins.execute() switch case
- ❌ NOT registered with addBuiltin()

**Result**: Functions worked when called, but didn't appear in help.

### Solution

Added registrations:
```java
addBuiltin(info(
    "scr.setTreeItemIcon", DataType.BOOL,
    newParam("screenName", DataType.STRING, true),
    newParam("itemPath", DataType.STRING, true),
    newParam("iconPath", DataType.STRING, false)
));
addBuiltin(info(
    "scr.getTreeItemIcon", DataType.STRING,
    newParam("screenName", DataType.STRING, true),
    newParam("itemPath", DataType.STRING, true)
));
// ... and 7 more TreeItem functions
```

## Checklist for Adding New Builtins

When adding a new builtin function:

- [ ] Implement the function in appropriate builtins class (e.g., BuiltinsScreen.java)
- [ ] Add switch case handler in Builtins.execute()
- [ ] **Register with addBuiltin(info(...)) in Builtins.java static block**
- [ ] Add help documentation to help-lookup.json (if user-facing)
- [ ] Test function execution
- [ ] Verify function appears in help.ebs tree
- [ ] Verify system.help() includes the function

## Location in Code

- **Registration Block**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/Builtins.java` (static initialization block)
- **Help Documentation**: `ScriptInterpreter/src/main/resources/help-lookup.json`
- **Help Script**: `ScriptInterpreter/src/main/resources/scripts/help.ebs`

## Testing Registration

To verify a function is properly registered:

1. Run the application
2. Open help (Syntax Help)
3. Navigate to the appropriate category (e.g., "scr" for screen functions)
4. Verify the function appears in the alphabetically sorted list
5. Click the function to view its signature and documentation

Or programmatically:
```ebs
var helpText: string = call system.help();
print helpText;  // Should include your function
```

## Common Mistakes

1. **Forgetting registration entirely** - Function works but isn't discoverable
2. **Wrong parameter types** - Causes runtime type checking errors
3. **Wrong required flags** - Optional parameters marked as required or vice versa
4. **Mismatched return types** - Documentation doesn't match actual behavior

## Related Issues

- Issue: TreeItem functions not appearing in help (PR #xxx)
  - Cause: Functions implemented but not registered
  - Solution: Added all 9 TreeItem function registrations
  - Lesson: Always register with addBuiltin() for visibility in help system
