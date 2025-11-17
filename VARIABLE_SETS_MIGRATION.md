# Variable Sets Structure - Migration Guide

## Overview

The screen JSON layout has been refactored to support grouping variables into named "sets" (varSets). This provides better organization and allows for internal-only variable sets.

## New Structure

### Sets Format

Variables are now organized into sets with the following structure:

```json
{
  "title": "My Screen",
  "width": 800,
  "height": 600,
  "sets": [
    {
      "setname": "PersonalInfo",
      "hiddenind": "N",
      "vars": [
        {
          "name": "firstName",
          "type": "string",
          "default": "John",
          "display": {"type": "textfield", "labeltext": "First Name"}
        },
        {
          "name": "lastName",
          "type": "string",
          "default": "Doe",
          "display": {"type": "textfield", "labeltext": "Last Name"}
        }
      ]
    },
    {
      "setname": "Internal",
      "hiddenind": "Y",
      "vars": [
        {
          "name": "userId",
          "type": "int",
          "default": 12345
        }
      ]
    }
  ]
}
```

### Set Properties

- **setname**: The name of the variable set (required)
- **hiddenind**: Visibility indicator
  - "N" = visible (default)
  - "Y" = internal access only (hidden from UI)
- **vars**: Array of variable definitions (same format as before)

## Storage Structure

Variables are stored in three new maps:

1. **screenVarSets**: `Map<String, Map<String, VarSet>>`
   - Key: screen name → lowercase set name → VarSet object
   - Contains metadata about each variable set

2. **screenVarItems**: `Map<String, Map<String, Var>>`
   - Key: screen name → "setname.varname" (lowercase) → Var object
   - Contains all variable definitions with their metadata

3. **screenAreaItems**: `Map<String, Map<String, AreaItem>>`
   - Key: screen name → "setname.varname" (lowercase) → AreaItem object
   - Contains area items that are linked to variables

## Backward Compatibility

The legacy "vars" format is still fully supported. When using the old format:

```json
{
  "title": "Legacy Screen",
  "vars": [
    {
      "name": "username",
      "type": "string",
      "default": "",
      "display": {"type": "textfield"}
    }
  ]
}
```

Variables are automatically placed in a default set called "default" with `hiddenind="N"`.

## Variable Access

Variable access remains the same for backward compatibility:

```ebs
screen myScreen = { ... };

// Access variables
print myScreen.firstName;
print myScreen.lastName;

// Modify variables
myScreen.firstName = "Jane";
myScreen.age = 25;
```

## Data Classes

### VarSet Class

```java
public class VarSet {
    private String setName;         // Name of the set
    private String hiddenInd;        // "Y" or "N"
    private Map<String, Var> variables;  // Variables in this set
    
    public boolean isHidden();       // Returns true if hiddenInd is "Y"
    public Var getVariable(String varName);
    public void addVariable(Var var);
}
```

### Var Class

```java
public class Var {
    private String name;            // Variable name
    private DataType type;          // Data type
    private Object defaultValue;    // Default value
    private Object value;           // Current value
    private DisplayItem displayItem; // Display metadata
    private String setName;         // Parent set name
    
    public String getKey();         // Returns "setname.varname" in lowercase
}
```

## Example: Mixed Structure

You can use both visible and hidden sets in the same screen:

```json
{
  "title": "User Profile",
  "sets": [
    {
      "setname": "UserInfo",
      "hiddenind": "N",
      "vars": [
        {"name": "name", "type": "string", "default": "", "display": {"type": "textfield"}},
        {"name": "email", "type": "string", "default": "", "display": {"type": "textfield"}}
      ]
    },
    {
      "setname": "Security",
      "hiddenind": "Y",
      "vars": [
        {"name": "sessionId", "type": "string", "default": ""},
        {"name": "lastLogin", "type": "date", "default": null}
      ]
    }
  ]
}
```

## Benefits

1. **Organization**: Group related variables together
2. **Visibility Control**: Mark internal variables as hidden
3. **Better Structure**: Clear separation between UI and internal state
4. **Backward Compatible**: Existing scripts continue to work
5. **Type Safety**: Full type information preserved in Var objects

## Migration

To migrate from the old format to the new format:

1. Wrap your existing "vars" array in a set:
   ```json
   "sets": [
     {
       "setname": "MainSet",
       "hiddenind": "N",
       "vars": [ /* your existing vars array */ ]
     }
   ]
   ```

2. Split variables into logical sets if desired
3. Mark internal-only variables with `"hiddenind": "Y"`

No code changes are required for migration as the old format is still fully supported.
