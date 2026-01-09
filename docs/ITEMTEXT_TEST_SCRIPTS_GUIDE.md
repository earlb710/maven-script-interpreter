# itemText Property Test Scripts Guide

This guide provides an overview of all test scripts for the `itemText` property feature.

## Test Scripts Location

All test scripts are located in: `ScriptInterpreter/scripts/test/`

## Test Scripts Overview

### Basic Tests

#### 1. test_itemtext_simple.ebs
**Purpose:** Simple validation test  
**Features:**
- Basic screen definition with button and label
- Demonstrates minimal itemText usage
- No GUI interaction required

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_simple.ebs"
```

---

#### 2. test_itemtext_property.ebs
**Purpose:** Interactive test with buttons and labels  
**Features:**
- Counter-based updates
- Multiple button and label controls
- Reset functionality
- Event handler demonstrations

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_property.ebs"
```

---

#### 3. test_itemtext_all_controls.ebs
**Purpose:** Comprehensive test for all 7 supported control types  
**Features:**
- Tests: Button, Label, Text, Hyperlink, ToggleButton, CheckBox, RadioButton
- Batch update all controls
- Reset functionality
- Visual verification of all control types

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_all_controls.ebs"
```

---

### JSON and Builtin Tests

#### 4. test_itemtext_json_simple.ebs
**Purpose:** JSON configuration management  
**Features:**
- Loading control text from JSON objects
- Using `json.getstring()` builtin
- Multiple configuration states
- Dynamic configuration switching

**Key Concepts:**
```ebs
var textConfig: map = {
    "button": "Click Me",
    "label": "Status: Ready"
};

var buttonText = call json.getstring(textConfig, "button");
call scr.setProperty("screen.button", "itemText", buttonText);
```

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_json_simple.ebs"
```

---

#### 5. test_itemtext_json_array.ebs
**Purpose:** JSON arrays and iteration  
**Features:**
- Storing control configurations in JSON arrays
- Using `json.arrayget()`, `json.arraysize()` builtins
- Loop-based batch updates
- Counter increment across multiple controls

**Key Concepts:**
```ebs
var controlsArray = call json.get(config, "controls");
var arraySize = call json.arraysize(config, "controls");

var i: int = 0;
while i < arraySize {
    var item = call json.arrayget(controlsArray, i);
    var id = call json.getstring(item, "id");
    var text = call json.getstring(item, "text");
    call scr.setProperty("screen." + id, "itemText", text);
    i = i + 1;
}
```

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_json_array.ebs"
```

---

#### 6. test_itemtext_json_state.ebs
**Purpose:** State management pattern with JSON  
**Features:**
- State machine implementation (idle → processing → completed)
- Using `json.get()`, `json.set()`, `json.getint()`, `json.getbool()`
- State-driven UI updates
- Task counter management
- Production-ready pattern demonstration

**Key Concepts:**
```ebs
var appState: map = {
    "mode": "idle",
    "taskCount": 0,
    "isProcessing": false
};

var stateTexts: map = {
    "idle": {"status": "Status: Idle", "button": "Start"},
    "processing": {"status": "Status: Processing", "button": "Stop"}
};

var mode = call json.getstring(appState, "mode");
var texts = call json.get(stateTexts, mode);
call scr.setProperty("screen.button", "itemText", call json.getstring(texts, "button"));
```

**Run:**
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="scripts/test/test_itemtext_json_state.ebs"
```

---

## Supported Controls

All test scripts work with these 7 control types:
1. **Button** - Standard clickable buttons
2. **Label** - Static text labels
3. **Text** - Text nodes (javafx.scene.text.Text)
4. **Hyperlink** - Clickable hyperlink controls
5. **ToggleButton** - Toggle-state buttons
6. **CheckBox** - Checkbox controls (updates text label)
7. **RadioButton** - Radio button controls (updates text label)

## JSON Builtins Used

### JSON Object Operations
- `json.getstring(map, key)` - Get string value from JSON
- `json.getint(map, key)` - Get integer value from JSON
- `json.getbool(map, key)` - Get boolean value from JSON
- `json.get(map, key)` - Get nested object from JSON
- `json.set(map, key, value)` - Set value in JSON

### JSON Array Operations
- `json.arraysize(map, arrayKey)` - Get array size
- `json.arrayget(array, index)` - Get item at index

## Usage Patterns

### Pattern 1: Simple Configuration
```ebs
var config: map = {"text": "Hello"};
call scr.setProperty("screen.label", "itemText", call json.getstring(config, "text"));
```

### Pattern 2: Batch Updates from Array
```ebs
var items = call json.get(config, "items");
var i: int = 0;
while i < call json.arraysize(config, "items") {
    var item = call json.arrayget(items, i);
    // Process item
    i = i + 1;
}
```

### Pattern 3: State-Driven UI
```ebs
var state: map = {"mode": "active"};
var texts: map = {"active": {"label": "Running"}};
var mode = call json.getstring(state, "mode");
var modeTexts = call json.get(texts, mode);
// Apply texts based on state
```

## Quick Start

**To run any test:**
1. Navigate to ScriptInterpreter directory: `cd ScriptInterpreter`
2. Run with Maven: `mvn javafx:run -Djavafx.args="../<test-file>.ebs"`
3. Interact with the UI to test features

**Recommended order:**
1. Start with `test_itemtext_simple.ebs` for basic understanding
2. Try `test_itemtext_all_controls.ebs` to see all supported controls
3. Explore `test_itemtext_json_simple.ebs` for JSON basics
4. Study `test_itemtext_json_array.ebs` for batch operations
5. Review `test_itemtext_json_state.ebs` for production patterns

## Troubleshooting

**Screen freezes:**
- Check for infinite loops in event handlers
- Ensure JSON operations don't reference missing keys
- Verify control paths are correct (screenName.itemName)

**Text not updating:**
- Verify control name matches exactly (case-sensitive)
- Ensure screen is shown before calling setProperty
- Check console for error messages

**JSON errors:**
- Validate JSON structure before using
- Use correct builtin for data type (getstring vs getint)
- Check array bounds before accessing

## Additional Resources

- **ITEMTEXT_PROPERTY_IMPLEMENTATION.md** - Detailed implementation documentation
- **ITEMTEXT_PROPERTY_SUMMARY.md** - Quick reference summary
- **docs/AREA_DEFINITION.md** - Screen definition syntax
- **docs/EBS_COLLECTIONS_REFERENCE.md** - JSON and collections documentation
