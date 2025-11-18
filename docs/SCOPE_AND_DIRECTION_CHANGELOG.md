# Scope and Direction Features - Changelog

## Overview

This document describes the changes made to improve the screen.set variable definitions by introducing better naming and parameter direction indicators.

## Changes Summary

### 1. Renamed `hiddenInd` to `scope` in VarSet

**Rationale:** The name `hiddenInd` was not descriptive enough. The new name `scope` better indicates that it controls the visibility scope of variable sets.

**Before:**
```java
private String hiddenInd;  // "Y" or "N"
```

**After:**
```java
private String scope;  // "internal" or "visible"
```

**New Values:**
- `"visible"` - Variables are visible in the UI (default, replaces "N")
- `"internal"` - Variables are for internal use only, hidden from UI (replaces "Y")

**Backward Compatibility:**
- Legacy values "Y" and "N" are automatically converted to "internal" and "visible"
- The JSON property name "hiddenind" is still supported
- The new JSON property name "scope" is preferred
- The `isHidden()` method is retained but deprecated

### 2. Added Parameter Direction to Var

**Rationale:** Need a way to indicate whether variables are input parameters, output parameters, or both.

**New Field:**
```java
private String direction;  // "in", "out", or "inout" (default)
```

**Direction Values:**
- `"in"` - Input parameter only (can be read from)
- `"out"` - Output parameter only (can be written to)  
- `"inout"` - Both input and output (default)

**Helper Methods:**
- `isInput()` - Returns true if direction is "in" or "inout"
- `isOutput()` - Returns true if direction is "out" or "inout"

## JSON Definition Examples

### Legacy Format (Still Supported)

```json
{
  "sets": [
    {
      "setname": "MySet",
      "hiddenind": "Y",
      "vars": [
        {"name": "myVar", "type": "string"}
      ]
    }
  ]
}
```

### New Format (Recommended)

```json
{
  "sets": [
    {
      "setname": "MySet",
      "scope": "internal",
      "vars": [
        {"name": "myVar", "type": "string", "direction": "inout"}
      ]
    }
  ]
}
```

### Mixed Format (Supported)

You can mix old and new property names:

```json
{
  "sets": [
    {
      "setname": "InputSet",
      "hiddenind": "N",  // Will be converted to "visible"
      "vars": [
        {"name": "inputVar", "type": "string", "direction": "in"}
      ]
    }
  ]
}
```

## API Changes

### VarSet Class

**New Methods:**
- `getScope()` - Returns the scope value
- `setScope(String)` - Sets the scope value (normalizes legacy values)
- `isInternal()` - Returns true if scope is "internal"

**Deprecated Methods:**
- `getHiddenInd()` - Use `getScope()` instead
- `setHiddenInd(String)` - Use `setScope()` instead
- `isHidden()` - Use `isInternal()` instead

**Changed Methods:**
- Constructor `VarSet(String setName, String scope)` - Second parameter renamed but accepts both old and new values

### Var Class

**New Methods:**
- `getDirection()` - Returns the direction value
- `setDirection(String)` - Sets the direction value (normalizes to valid options)
- `isInput()` - Returns true if direction is "in" or "inout"
- `isOutput()` - Returns true if direction is "out" or "inout"

**Changed Behavior:**
- All constructors now initialize `direction` to "inout" by default

### InterpreterScreen Class

**Changed Behavior:**
- Accepts both "scope" and "hiddenind" property names in JSON
- Default for scope is "visible"
- Accepts "direction" property in var definitions
- Default for direction is "inout"

## Migration Guide

### For Existing Code

1. **No action required** - Existing code using `hiddenInd` continues to work
2. **Optional** - Update to new terminology at your convenience:
   - Replace `getHiddenInd()` with `getScope()`
   - Replace `isHidden()` with `isInternal()`
   - Replace JSON "hiddenind" with "scope"
   - Replace "Y" with "internal", "N" with "visible"

### For New Code

1. Use `scope` instead of `hiddenInd`
2. Use "internal" and "visible" values
3. Add "direction" property to variables where appropriate
4. Use `isInternal()`, `isInput()`, `isOutput()` convenience methods

## Testing

### Test Scripts

1. **test_variable_sets.ebs** - Tests backward compatibility with legacy format
2. **test_variable_sets_with_direction.ebs** - Tests new scope and direction features

### Build Verification

```bash
cd ScriptInterpreter
mvn clean compile
```

Expected result: BUILD SUCCESS

## Benefits

1. **Clearer Intent**: "scope" is more descriptive than "hiddenInd"
2. **Better Documentation**: Direction indicators document parameter usage
3. **Backward Compatibility**: Existing scripts continue to work without changes
4. **Smooth Migration**: Deprecated methods provide gradual transition path
5. **Type Safety**: Normalization methods ensure valid values

## Future Enhancements

Potential future improvements:
1. Add runtime enforcement of direction constraints
2. Add UI indicators for parameter direction
3. Add validation warnings for incorrect direction usage
4. Add code completion hints based on direction
