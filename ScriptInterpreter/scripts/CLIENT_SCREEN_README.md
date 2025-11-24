# Client Screen Scripts

This directory contains example scripts demonstrating client information screen functionality.

## Scripts

### client_screen.ebs
A basic client information screen with the following updatable fields:
- **Client Name** (string): Text field for entering the client's first name
- **Surname** (string): Text field for entering the client's last name  
- **Age** (int): Spinner control for selecting age (0-150)
- **Client ID** (string): Text field for entering a unique client identifier

**Usage:**
```bash
cd ScriptInterpreter
mvn javafx:run
# Then load the script: /open scripts/client_screen.ebs
# Press Ctrl+Enter to run
```

**Features:**
- All fields are mandatory
- Age is controlled via a spinner with min/max validation
- Fields include helpful prompt text
- Screen size: 600x400 pixels

### client_screen_demo.ebs
An enhanced version of the client screen with interactive demonstrations of:
- Programmatic field updates
- Reading and writing field values
- Formatted output display
- Field validation examples
- Data manipulation

**Usage:**
```bash
cd ScriptInterpreter
mvn javafx:run
# Then load the script: /open scripts/client_screen_demo.ebs
# Press Ctrl+Enter to run
```

**Features:**
- All features from `client_screen.ebs`
- Uppercase formatting for Client ID field
- Detailed console output showing field interactions
- Examples of birthday updates and data validation
- Screen size: 650x450 pixels

## Field Details

| Field | Type | Control | Min | Max | Default | Validation |
|-------|------|---------|-----|-----|---------|------------|
| Client Name | string | textfield | - | - | "" | Mandatory |
| Surname | string | textfield | - | - | "" | Mandatory |
| Age | int | spinner | 0 | 150 | 18/25 | Mandatory |
| Client ID | string | textfield | - | - | "" | Mandatory, Uppercase |

## Running from Command Line

To execute these scripts:

```bash
cd ScriptInterpreter
mvn compile
mvn javafx:run
```

Then in the interactive console:
- Type `/open scripts/client_screen.ebs` or `/open scripts/client_screen_demo.ebs`
- Press `Ctrl+Enter` to execute

## Accessing Screen Variables

You can access and modify screen variables programmatically:

```javascript
// Read values
var name = clientInfoScreen.clientName;
var age = clientInfoScreen.age;

// Write values
clientInfoScreen.clientName = "Jane";
clientInfoScreen.age = 30;

// Build composite strings
var fullName = clientInfoScreen.clientName + " " + clientInfoScreen.surname;
```

## Screen Display

To show or hide the screen:

```javascript
show screen clientInfoScreen;  // Display the screen
hide screen clientInfoScreen;  // Hide the screen
```

## Notes

- The screen must be defined before it can be shown
- All fields support real-time updates
- Field validation is applied automatically
- The spinner control for age prevents invalid values
- Client ID field automatically converts input to uppercase (demo version)
