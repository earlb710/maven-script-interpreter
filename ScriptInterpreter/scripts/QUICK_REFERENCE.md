# Quick Reference: Client Screen Scripts

## Quick Start

### Run Basic Version
```bash
cd ScriptInterpreter
mvn javafx:run
```
In console: `/open scripts/client_screen.ebs` → Press `Ctrl+Enter`

### Run Demo Version  
```bash
cd ScriptInterpreter
mvn javafx:run
```
In console: `/open scripts/client_screen_demo.ebs` → Press `Ctrl+Enter`

## Fields

| Field | Variable Name | Type | Control | Default |
|-------|--------------|------|---------|---------|
| Client Name | `clientName` | string | Text field | `""` |
| Surname | `surname` | string | Text field | `""` |
| Age | `age` | int | Spinner (0-150) | `18` |
| Client ID | `clientId` | string | Text field | `""` |

## Common Operations

### Read Field Values
```javascript
var name = clientScreen.clientName;
var surname = clientScreen.surname;
var age = clientScreen.age;
var id = clientScreen.clientId;

// Build full name
var fullName = clientScreen.clientName + " " + clientScreen.surname;
```

### Set Field Values
```javascript
clientScreen.clientName = "John";
clientScreen.surname = "Doe";
clientScreen.age = 30;
clientScreen.clientId = "CLT-12345";
```

### Show/Hide Screen
```javascript
show screen clientScreen;  // Display the screen
hide screen clientScreen;  // Hide the screen
```

### Update Age (Birthday)
```javascript
clientScreen.age = clientScreen.age + 1;
```

## Files Overview

- **client_screen.ebs** - Basic implementation
- **client_screen_demo.ebs** - With examples and output
- **CLIENT_SCREEN_README.md** - Full documentation
- **CLIENT_SCREEN_LAYOUT.md** - Visual layout guide
- **IMPLEMENTATION_SUMMARY.md** - Implementation details

## Validation

- ✅ All fields are mandatory
- ✅ Age: must be 0-150
- ✅ Real-time field synchronization
- ✅ Thread-safe operations

## Screen Size

- Basic: 600×400 pixels
- Demo: 650×450 pixels

## Tips

1. Use spinner up/down arrows or type directly for age
2. Tab key moves between fields
3. All changes are immediately synchronized
4. Fields can be updated while screen is shown
5. Client ID auto-converts to uppercase (demo version)
