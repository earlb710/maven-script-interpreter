# API Reference: VarSet Class

## Overview

The `VarSet` class represents a set of variables in a screen definition. Variables are grouped into sets with a name and a scope that determines visibility and parameter direction.

## Package

```java
com.eb.script.interpreter.screen
```

## Class Declaration

```java
public class VarSet
```

## Fields

### setName
```java
private String setName
```
The name of the variable set.

### scope
```java
private String scope
```
Scope indicator that determines both visibility and parameter direction.

**Valid values:**
- `"visible"` - Variables are visible in UI (default)
- `"internal"` - Variables are hidden from UI
- `"in"` - Input parameters
- `"out"` - Output parameters
- `"inout"` - Input/output parameters

### variables
```java
private Map<String, Var> variables
```
Map of variables in this set, keyed by lowercase variable name.

## Constructors

### Default Constructor
```java
public VarSet()
```
Creates a VarSet with default scope of "visible".

### Constructor with setName
```java
public VarSet(String setName)
```
Creates a VarSet with the specified name and default scope.

**Parameters:**
- `setName` - The name of the variable set

### Constructor with setName and scope
```java
public VarSet(String setName, String scope)
```
Creates a VarSet with the specified name and scope.

**Parameters:**
- `setName` - The name of the variable set
- `scope` - Scope indicator (normalized automatically)

## Methods

### getSetName()
```java
public String getSetName()
```
Returns the name of this variable set.

**Returns:** The set name

### setSetName(String)
```java
public void setSetName(String setName)
```
Sets the name of this variable set.

**Parameters:**
- `setName` - The new set name

### getScope()
```java
public String getScope()
```
Returns the normalized scope value.

**Returns:** The scope ("visible", "internal", "in", "out", or "inout")

### setScope(String)
```java
public void setScope(String scope)
```
Sets the scope with automatic normalization.

**Parameters:**
- `scope` - The scope value (can include legacy values "Y"/"N" and aliases)

**Normalization:**
- `"Y"` → `"internal"`
- `"N"` → `"visible"`
- `"parameterIn"` → `"in"`
- `"parameterOut"` → `"out"`
- Other values are converted to lowercase

### getVariables()
```java
public Map<String, Var> getVariables()
```
Returns the map of variables in this set.

**Returns:** Map of variables keyed by lowercase variable name

### setVariables(Map)
```java
public void setVariables(Map<String, Var> variables)
```
Sets the variables map.

**Parameters:**
- `variables` - The new variables map

### addVariable(Var)
```java
public void addVariable(Var var)
```
Adds a variable to this set.

**Parameters:**
- `var` - The variable to add (added using lowercase name as key)

### getVariable(String)
```java
public Var getVariable(String varName)
```
Gets a variable from this set by name (case-insensitive).

**Parameters:**
- `varName` - The variable name

**Returns:** The variable, or null if not found

### isInternal()
```java
public boolean isInternal()
```
Checks if this set is internal (hidden from UI).

**Returns:** `true` if scope is "internal", `false` otherwise

### isInput()
```java
public boolean isInput()
```
Checks if this set contains input parameters.

**Returns:** `true` if scope is "in" or "inout", `false` otherwise

### isOutput()
```java
public boolean isOutput()
```
Checks if this set contains output parameters.

**Returns:** `true` if scope is "out" or "inout", `false` otherwise

### isHidden() (Deprecated)
```java
@Deprecated
public boolean isHidden()
```
Legacy method for checking if set is hidden.

**Returns:** `true` if scope is "internal", `false` otherwise

**Deprecated:** Use `isInternal()` instead

### toString()
```java
@Override
public String toString()
```
Returns a string representation of this VarSet.

**Returns:** String in format "VarSet{setName='...', scope='...', variables=N}"

## Usage Examples

### Creating a VarSet

```java
// With default scope (visible)
VarSet visibleSet = new VarSet("UserInputs");

// With specific scope
VarSet inputSet = new VarSet("Parameters", "in");
VarSet outputSet = new VarSet("Results", "out");
VarSet internalSet = new VarSet("WorkingData", "internal");

// Using aliases
VarSet paramSet = new VarSet("Params", "parameterIn");
```

### Adding Variables

```java
VarSet set = new VarSet("MySet");

Var var1 = new Var("username", DataType.STRING, "");
Var var2 = new Var("age", DataType.INT, 0);

set.addVariable(var1);
set.addVariable(var2);
```

### Checking Scope

```java
VarSet set = context.getScreenVarSet("myScreen", "Parameters");

if (set.isInput()) {
    System.out.println("This set contains input parameters");
}

if (set.isOutput()) {
    System.out.println("This set contains output parameters");
}

if (set.isInternal()) {
    System.out.println("This set is hidden from UI");
}

// Check for specific direction
if (set.isInput() && !set.isOutput()) {
    System.out.println("Input-only parameters");
} else if (set.isOutput() && !set.isInput()) {
    System.out.println("Output-only parameters");
} else if (set.isInput() && set.isOutput()) {
    System.out.println("Input/output parameters");
}
```

### Getting Variables

```java
VarSet set = context.getScreenVarSet("myScreen", "Inputs");

// Get specific variable (case-insensitive)
Var username = set.getVariable("username");
Var userName = set.getVariable("USERNAME"); // Same result

// Get all variables
Map<String, Var> allVars = set.getVariables();
for (Map.Entry<String, Var> entry : allVars.entrySet()) {
    System.out.println("Variable: " + entry.getKey());
}
```

## See Also

- [Var](VAR_API.md) - Individual variable documentation
- [SCOPE_REFACTORING.md](SCOPE_REFACTORING.md) - Details on scope refactoring
- [VARIABLE_SETS_VISUAL_GUIDE.md](../VARIABLE_SETS_VISUAL_GUIDE.md) - Visual guide
