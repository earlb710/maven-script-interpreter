# Scope Refactoring Documentation

## Overview

This document describes the refactoring of the `hiddenInd` field to `scope` and the consolidation of parameter direction into the scope field.

## What Changed

### Before

Variables were organized into sets with:
- **VarSet**: Had a `hiddenInd` field with values "Y" or "N"
- **Var**: Had a separate `direction` field with values "in", "out", or "inout"

### After

Variables are organized into sets with:
- **VarSet**: Has a `scope` field that combines both visibility and parameter direction
- **Var**: No longer has a `direction` field

## Scope Values

The `scope` field in VarSet now supports the following values:

| Scope Value | Alias | Description |
|-------------|-------|-------------|
| `"visible"` | - | Variables are visible in UI (default) |
| `"internal"` | - | Variables are hidden from UI (internal use only) |
| `"in"` | `"parameterIn"` | Input parameters (can be read from) |
| `"out"` | `"parameterOut"` | Output parameters (can be written to) |
| `"inout"` | - | Input/Output parameters (both read and write) |

### Legacy Support

For backward compatibility:
- `"Y"` is converted to `"internal"`
- `"N"` is converted to `"visible"`

## JSON Format

### New Format

```json
{
  "sets": [
    {
      "setname": "InputParameters",
      "scope": "in",
      "vars": [
        {"name": "sourceFile", "type": "string", "default": "data.csv"},
        {"name": "processMode", "type": "string", "default": "auto"}
      ]
    },
    {
      "setname": "OutputResults",
      "scope": "out",
      "vars": [
        {"name": "recordsProcessed", "type": "int", "default": 0},
        {"name": "status", "type": "string", "default": "pending"}
      ]
    },
    {
      "setname": "WorkingData",
      "scope": "inout",
      "vars": [
        {"name": "currentRecord", "type": "int", "default": 0}
      ]
    },
    {
      "setname": "InternalState",
      "scope": "internal",
      "vars": [
        {"name": "sessionId", "type": "int", "default": 0}
      ]
    }
  ]
}
```

### Using Aliases

You can also use the longer aliases for clarity:

```json
{
  "setname": "Inputs",
  "scope": "parameterIn",
  "vars": [...]
}
```

## API Changes

### VarSet Class

**New Methods:**
```java
public boolean isInput()   // Returns true if scope is "in" or "inout"
public boolean isOutput()  // Returns true if scope is "out" or "inout"
public boolean isInternal() // Returns true if scope is "internal"
```

**Updated Methods:**
```java
public String getScope()           // Returns normalized scope value
public void setScope(String scope) // Sets scope with normalization
```

**Deprecated Methods:**
```java
@Deprecated
public boolean isHidden() // Use isInternal() instead
```

### Var Class

**Removed:**
- `direction` field
- `getDirection()` method
- `setDirection()` method
- `isInput()` method
- `isOutput()` method

Parameter direction is now determined by the VarSet's scope, not individual variables.

## Migration Guide

### For Existing Code Using hiddenInd

No changes required - existing scripts using `"hiddenind": "Y"` or `"hiddenind": "N"` will continue to work:

```json
// This still works
{
  "setname": "MySet",
  "hiddenind": "Y",
  "vars": [...]
}
```

### For Code Using direction in Var

If you were specifying direction at the variable level:

**Before:**
```json
{
  "setname": "MySet",
  "scope": "visible",
  "vars": [
    {"name": "input1", "type": "string", "direction": "in"},
    {"name": "output1", "type": "string", "direction": "out"}
  ]
}
```

**After:**
Split into separate sets by direction:
```json
{
  "sets": [
    {
      "setname": "Inputs",
      "scope": "in",
      "vars": [
        {"name": "input1", "type": "string"}
      ]
    },
    {
      "setname": "Outputs",
      "scope": "out",
      "vars": [
        {"name": "output1", "type": "string"}
      ]
    }
  ]
}
```

### For Java Code

If you were checking variable direction:

**Before:**
```java
Var var = ...;
if (var.isInput()) {
    // Process input
}
```

**After:**
```java
VarSet varSet = context.getScreenVarSet(screenName, setName);
if (varSet.isInput()) {
    // Process all variables in this set as inputs
}
```

## Benefits

1. **Simpler Structure**: Parameter direction applies to entire sets, not individual variables
2. **Clearer Intent**: All variables in an "in" set are inputs, all in "out" set are outputs
3. **Better Organization**: Natural grouping of variables by their flow direction
4. **Reduced Complexity**: One less field to manage in Var class

## Examples

### Data Processing Screen

```json
{
  "title": "Data Processor",
  "sets": [
    {
      "setname": "InputFiles",
      "scope": "in",
      "vars": [
        {"name": "csvFile", "type": "string"},
        {"name": "configFile", "type": "string"}
      ]
    },
    {
      "setname": "ProcessingResults",
      "scope": "out",
      "vars": [
        {"name": "recordCount", "type": "int"},
        {"name": "errorCount", "type": "int"},
        {"name": "outputFile", "type": "string"}
      ]
    },
    {
      "setname": "Settings",
      "scope": "inout",
      "vars": [
        {"name": "batchSize", "type": "int"},
        {"name": "timeout", "type": "int"}
      ]
    }
  ]
}
```

### Checking Scope in Code

```java
VarSet inputSet = context.getScreenVarSet("dataProcessor", "InputFiles");
if (inputSet.isInput() && !inputSet.isOutput()) {
    // This is an input-only set
    System.out.println("Reading input parameters from " + inputSet.getSetName());
}

VarSet resultSet = context.getScreenVarSet("dataProcessor", "ProcessingResults");
if (resultSet.isOutput() && !resultSet.isInput()) {
    // This is an output-only set
    System.out.println("Writing results to " + resultSet.getSetName());
}

VarSet settingsSet = context.getScreenVarSet("dataProcessor", "Settings");
if (settingsSet.isInput() && settingsSet.isOutput()) {
    // This is an input/output set
    System.out.println("Settings can be read and modified");
}
```

## See Also

- [VARIABLE_SETS_VISUAL_GUIDE.md](../VARIABLE_SETS_VISUAL_GUIDE.md) - Visual guide to variable sets structure
- [SCOPE_AND_DIRECTION_CHANGELOG.md](../SCOPE_AND_DIRECTION_CHANGELOG.md) - Changelog for scope and direction features
