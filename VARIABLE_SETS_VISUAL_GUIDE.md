# Variable Sets Structure - Visual Guide

## Data Flow Diagram

```
Screen JSON Definition
│
├─▶ "sets": [...]  (New Format)
│   │
│   └─▶ Set 1: {setname, hiddenind, vars: [...]}
│       │
│       ├─▶ Var 1: {name, type, default, display}
│       ├─▶ Var 2: {name, type, default, display}
│       └─▶ Var 3: {name, type, default, display}
│
└─▶ "vars": [...]  (Legacy Format - Auto-creates "default" set)
    │
    ├─▶ Var 1: {name, type, default, display}
    ├─▶ Var 2: {name, type, default, display}
    └─▶ Var 3: {name, type, default, display}

                        ↓ InterpreterScreen.visitScreenStatement()

Storage in InterpreterContext
│
├─▶ screenVarSets: Map<String, Map<String, VarSet>>
│   │
│   └─▶ "myScreen" → Map
│       ├─▶ "personalinfo" → VarSet(setName="PersonalInfo", hiddenInd="N")
│       ├─▶ "contactinfo" → VarSet(setName="ContactInfo", hiddenInd="N")
│       └─▶ "internal" → VarSet(setName="Internal", hiddenInd="Y")
│
├─▶ screenVarItems: Map<String, Map<String, Var>>
│   │
│   └─▶ "myScreen" → Map
│       ├─▶ "personalinfo.firstname" → Var(name="firstName", type=STRING, ...)
│       ├─▶ "personalinfo.lastname" → Var(name="lastName", type=STRING, ...)
│       ├─▶ "contactinfo.email" → Var(name="email", type=STRING, ...)
│       ├─▶ "contactinfo.phone" → Var(name="phone", type=STRING, ...)
│       └─▶ "internal.userid" → Var(name="userId", type=INT, ...)
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
├── hiddenInd: "N"
└── variables: Map<String, Var>
    ├── "firstname" → Var
    ├── "lastname" → Var
    └── "age" → Var
```

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

### 4. Check if a set is hidden
```java
VarSet internalSet = context.getScreenVarSet("myScreen", "Internal");
boolean isHidden = internalSet.isHidden(); // Returns true if hiddenInd="Y"
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
    "default": VarSet(setName="default", hiddenInd="N")
  }
}

screenVarItems: {
  "myScreen": {
    "default.username": Var(name="username", setName="default")
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
      "hiddenind": "N",
      "vars": [
        {"name": "firstName", "type": "string", "default": "John"},
        {"name": "lastName", "type": "string", "default": "Doe"}
      ]
    },
    {
      "setname": "Internal",
      "hiddenind": "Y",
      "vars": [
        {"name": "userId", "type": "int", "default": 12345}
      ]
    }
  ]
}
```

### Storage Result
```
screenVarSets["userProfile"]:
  "personalinfo" → VarSet {
    setName: "PersonalInfo"
    hiddenInd: "N"
    variables: {
      "firstname" → Var{name="firstName", ...}
      "lastname" → Var{name="lastName", ...}
    }
  }
  "internal" → VarSet {
    setName: "Internal"
    hiddenInd: "Y"
    variables: {
      "userid" → Var{name="userId", ...}
    }
  }

screenVarItems["userProfile"]:
  "personalinfo.firstname" → Var{name="firstName", type=STRING, ...}
  "personalinfo.lastname" → Var{name="lastName", type=STRING, ...}
  "internal.userid" → Var{name="userId", type=INT, ...}
```

### EBS Access
```ebs
screen userProfile = { /* JSON above */ };

// All accessible with simple syntax
print userProfile.firstName;  // "John"
print userProfile.lastName;   // "Doe"
print userProfile.userId;     // 12345

// Hidden variables still accessible programmatically
userProfile.userId = 67890;
```

The "hiddenind" property controls UI visibility, not programmatic access.
