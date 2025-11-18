# Variable Sets Structure - Visual Guide

## Data Flow Diagram

```
Screen JSON Definition
│
├─▶ "sets": [...]  (New Format)
│   │
│   └─▶ Set 1: {setname, scope, vars: [...]}
│       │
│       ├─▶ Var 1: {name, type, default, direction, display}
│       ├─▶ Var 2: {name, type, default, direction, display}
│       └─▶ Var 3: {name, type, default, direction, display}
│
└─▶ "vars": [...]  (Legacy Format - Auto-creates "default" set)
    │
    ├─▶ Var 1: {name, type, default, direction, display}
    ├─▶ Var 2: {name, type, default, direction, display}
    └─▶ Var 3: {name, type, default, direction, display}

                        ↓ InterpreterScreen.visitScreenStatement()

Storage in InterpreterContext
│
├─▶ screenVarSets: Map<String, Map<String, VarSet>>
│   │
│   └─▶ "myScreen" → Map
│       ├─▶ "personalinfo" → VarSet(setName="PersonalInfo", scope="visible")
│       ├─▶ "contactinfo" → VarSet(setName="ContactInfo", scope="visible")
│       └─▶ "internal" → VarSet(setName="Internal", scope="internal")
│
├─▶ screenVarItems: Map<String, Map<String, Var>>
│   │
│   └─▶ "myScreen" → Map
│       ├─▶ "personalinfo.firstname" → Var(name="firstName", type=STRING, direction="inout", ...)
│       ├─▶ "personalinfo.lastname" → Var(name="lastName", type=STRING, direction="inout", ...)
│       ├─▶ "contactinfo.email" → Var(name="email", type=STRING, direction="inout", ...)
│       ├─▶ "contactinfo.phone" → Var(name="phone", type=STRING, direction="inout", ...)
│       └─▶ "internal.userid" → Var(name="userId", type=INT, direction="inout", ...)
│
└─▶ screenAreaItems: Map<String, Map<String, AreaItem>>
    │
    └─▶ "myScreen" → Map
        ├─▶ "personalinfo.firstname" → AreaItem(varRef="firstName", ...)
        ├─▶ "personalinfo.lastname" → AreaItem(varRef="lastName", ...)
        └─▶ "contactinfo.email" → AreaItem(varRef="email", ...)
```

## Key Structure

### VarSet Object
```
VarSet
├── setName: "PersonalInfo"
├── scope: "visible"  (visible, internal, in, out, inout)
└── variables: Map<String, Var>
    ├── "firstname" → Var
    ├── "lastname" → Var
    └── "age" → Var
```

**Scope Values:**
- `"visible"` - Variables are visible in UI (default)
- `"internal"` - Variables are for internal use only, hidden from UI
- `"in"` (or `"parameterIn"`) - Input parameters
- `"out"` (or `"parameterOut"`) - Output parameters
- `"inout"` - Input/Output parameters (both)
- Legacy: `"Y"` (internal) and `"N"` (visible) still supported for backward compatibility

### Var Object
```
Var
├── name: "firstName"
├── type: DataType.STRING
├── defaultValue: "John"
├── value: "John"
├── setName: "PersonalInfo"
├── displayItem: DisplayItem {...}
└── getKey() → "personalinfo.firstname"
```

## Access Patterns

### 1. Get a VarSet
```java
InterpreterContext context = ...;
VarSet personalInfo = context.getScreenVarSet("myScreen", "PersonalInfo");
// Returns VarSet with setName="PersonalInfo", hiddenInd="N"
```

### 2. Get a Var by qualified key
```java
Var firstName = context.getScreenVar("myScreen", "personalinfo.firstname");
// Returns Var with name="firstName", type=STRING
```

### 3. Get an AreaItem linked to a variable
```java
Map<String, AreaItem> areaItems = context.getScreenAreaItems().get("myScreen");
AreaItem item = areaItems.get("personalinfo.firstname");
// Returns AreaItem with varRef="firstName"
```

### 4. Check if a set is internal
```java
VarSet internalSet = context.getScreenVarSet("myScreen", "Internal");
boolean isInternal = internalSet.isInternal(); // Returns true if scope="internal"
// Legacy method still available: internalSet.isHidden()
```

### 5. Check variable parameter direction
```java
Var firstName = context.getScreenVar("myScreen", "personalinfo.firstname");
boolean canRead = firstName.isInput();   // true for "in" or "inout"
boolean canWrite = firstName.isOutput(); // true for "out" or "inout"
```

## Backward Compatibility

### Legacy Format
```json
{
  "vars": [
    {"name": "username", "type": "string"}
  ]
}
```

### Automatically Converted To
```
screenVarSets: {
  "myScreen": {
    "default": VarSet(setName="default", scope="visible")
  }
}

screenVarItems: {
  "myScreen": {
    "default.username": Var(name="username", setName="default", direction="inout")
  }
}
```

## Variable Access (EBS Script)

The new format uses three-part notation for variable access:

```ebs
screen myScreen = { ... };

// Access variables with three-part notation: screen.setName.varName
print myScreen.PersonalInfo.firstName;
print myScreen.ContactInfo.email;

// Modify variables
myScreen.PersonalInfo.firstName = "Jane";
myScreen.PersonalInfo.age = 25;
```

For legacy format with "vars", variables are in the "default" set:

```ebs
screen legacyScreen = {
    "vars": [{"name": "username", "type": "string"}]
};

// Access using three-part notation with "default" set
print legacyScreen.default.username;

// Or use two-part notation for backward compatibility
print legacyScreen.username;
```

The three-part notation (`screen.setName.varName`) makes the organization explicit and allows proper scoping of variables within their sets.

## Case Sensitivity

All storage keys are **lowercase** for case-insensitive access:

| Input | Storage Key |
|-------|-------------|
| setName: "PersonalInfo" | "personalinfo" |
| varName: "FirstName" | "personalinfo.firstname" |
| varRef: "Email" | "contactinfo.email" |

This ensures consistent lookup regardless of the original casing in the JSON definition.

## Complete Example

### JSON Input
```json
{
  "title": "User Profile",
  "sets": [
    {
      "setname": "PersonalInfo",
      "scope": "visible",
      "vars": [
        {"name": "firstName", "type": "string", "default": "John", "direction": "inout"},
        {"name": "lastName", "type": "string", "default": "Doe", "direction": "inout"}
      ]
    },
    {
      "setname": "Internal",
      "scope": "internal",
      "vars": [
        {"name": "userId", "type": "int", "default": 12345, "direction": "in"}
      ]
    }
  ]
}
```

**Note:** For backward compatibility, `"hiddenind": "Y"` or `"hiddenind": "N"` can still be used instead of `"scope"`.

### Storage Result
```
screenVarSets["userProfile"]:
  "personalinfo" → VarSet {
    setName: "PersonalInfo"
    scope: "visible"
    variables: {
      "firstname" → Var{name="firstName", direction="inout", ...}
      "lastname" → Var{name="lastName", direction="inout", ...}
    }
  }
  "internal" → VarSet {
    setName: "Internal"
    scope: "internal"
    variables: {
      "userid" → Var{name="userId", direction="in", ...}
    }
  }

screenVarItems["userProfile"]:
  "personalinfo.firstname" → Var{name="firstName", type=STRING, direction="inout", ...}
  "personalinfo.lastname" → Var{name="lastName", type=STRING, direction="inout", ...}
  "internal.userid" → Var{name="userId", type=INT, direction="in", ...}
```

### EBS Access
```ebs
screen userProfile = { /* JSON above */ };

// All accessible with simple syntax
print userProfile.firstName;  // "John"
print userProfile.lastName;   // "Doe"
print userProfile.userId;     // 12345

// Internal variables still accessible programmatically
userProfile.userId = 67890;
```

The "scope" property controls UI visibility, not programmatic access. Variables in sets with `scope="internal"` are not displayed in the UI but can still be accessed programmatically.

## Parameter Direction

Variables can specify their parameter direction to indicate how they should be used:

### Direction Values

- **`"in"`** - Input parameter: Can be read from, typically used for data passed into a screen
- **`"out"`** - Output parameter: Can be written to, typically used for results produced by a screen
- **`"inout"`** - Input/Output parameter: Can be both read and written (default)

### Example with Direction

```json
{
  "title": "Data Processing",
  "sets": [
    {
      "setname": "InputData",
      "scope": "visible",
      "vars": [
        {"name": "sourceFile", "type": "string", "default": "", "direction": "in"},
        {"name": "processMode", "type": "string", "default": "auto", "direction": "in"}
      ]
    },
    {
      "setname": "OutputData",
      "scope": "visible",
      "vars": [
        {"name": "recordsProcessed", "type": "int", "default": 0, "direction": "out"},
        {"name": "status", "type": "string", "default": "pending", "direction": "out"}
      ]
    },
    {
      "setname": "WorkingVars",
      "scope": "internal",
      "vars": [
        {"name": "currentRecord", "type": "int", "default": 0, "direction": "inout"}
      ]
    }
  ]
}
```

### Checking Direction in Code

```java
Var sourceFile = context.getScreenVar("dataProcessing", "inputdata.sourcefile");
if (sourceFile.isInput()) {
    // This variable can be read from
    String file = (String) sourceFile.getValue();
}

Var recordsProcessed = context.getScreenVar("dataProcessing", "outputdata.recordsprocessed");
if (recordsProcessed.isOutput()) {
    // This variable can be written to
    recordsProcessed.setValue(1000);
}
```

### Best Practices

1. **Input-only variables** (`"in"`): Use for configuration, parameters, or data that should not be modified
2. **Output-only variables** (`"out"`): Use for computed results, status indicators, or generated data
3. **Input/Output variables** (`"inout"`): Use for data that may be modified or accumulated during processing

The direction property is primarily documentation and validation - it helps clarify the intended use of each variable but does not enforce read/write restrictions at runtime.
