# Screen Definition Best Practices Guide

## Overview

This guide provides best practices and patterns for creating effective screen definitions in EBS (Earl Bosch Script). Screen definitions control the UI windows, layout, variable binding, and event handling in your applications.

**Related Documentation:**
- [AREA_DEFINITION.md](../../docs/AREA_DEFINITION.md) - Comprehensive area and item reference
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Language syntax including screen syntax
- [SCREEN_LIFECYCLE_DIAGRAM.md](../../docs/SCREEN_LIFECYCLE_DIAGRAM.md) - Understanding screen lifecycle

---

## Table of Contents

1. [Basic Structure](#basic-structure)
2. [Screen Properties](#screen-properties)
3. [Variable Definition](#variable-definition)
4. [Variable Sets](#variable-sets)
5. [Area Layout](#area-layout)
6. [Event Handlers](#event-handlers)
7. [Thread Safety](#thread-safety)
8. [Common Patterns](#common-patterns)
9. [Performance Tips](#performance-tips)
10. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Basic Structure

### Minimal Screen Definition

```
// Simplest screen - empty window
screen myScreen = {
    "title": "My Application",
    "width": 600,
    "height": 400
};

// Show the screen when needed
show screen myScreen;
```

### Complete Screen Definition

```
screen fullExample = {
    "name": "fullExample",           // Optional: defaults to variable name
    "title": "Complete Example",
    "width": 800,
    "height": 600,
    "maximize": false,               // Optional: start maximized
    "sets": [                        // Variable sets for organization
        {
            "setname": "userData",
            "scope": "visible",
            "vars": [/* variable definitions */]
        }
    ],
    "vars": [/* standalone variables */],
    "area": [/* layout areas */]
};
```

**Best Practice:** Start with a minimal definition and add complexity as needed. Don't over-engineer upfront.

### JSON Key Case-Insensitivity

**Important:** Screen definitions use a specialized JSON parser that normalizes all keys to lowercase for case-insensitive matching.

```
// These are all equivalent - keys are normalized to lowercase
screen example1 = {
    "title": "Example",      // lowercase
    "Width": 600,            // becomes "width"
    "HEIGHT": 400            // becomes "height"
};

// You can use camelCase or snake_case - both work
screen example2 = {
    "labelText": "Name:",    // becomes "labeltext"
    "label_text": "Name:",   // becomes "label_text" (but looked up as lowercase)
    "onClick": "doSomething();",  // becomes "onclick"
    "on_click": "doSomething();"  // becomes "on_click"
};
```

**Key Points:**
- All JSON keys in screen definitions are converted to lowercase at parse time
- Use consistent casing in your code for readability (recommend camelCase)
- Property lookups are case-insensitive: `labelText`, `labeltext`, and `LABELTEXT` all work
- This applies to all screen JSON: properties, display metadata, area definitions, and items
- Other JSON (non-screen) preserves original key case

**Best Practice:** Use camelCase consistently in screen definitions for better readability, knowing that the parser will handle case-insensitivity automatically.

---

## Screen Properties

### Window Sizing

```
// Fixed size window
screen fixedWindow = {
    "title": "Fixed Size",
    "width": 800,
    "height": 600
};

// Maximized window
screen maxWindow = {
    "title": "Maximized",
    "width": 1024,      // Initial size before maximize
    "height": 768,
    "maximize": true
};
```

**Best Practices:**
- Use standard sizes: 800x600 (small), 1024x768 (medium), 1280x960 (large)
- For data entry forms: 600-800 width is sufficient
- For dashboards/complex UIs: 1024+ width recommended
- Set `maximize: true` for data-heavy screens (tables, grids)

### Window Titles

```
// Clear, descriptive titles
screen goodTitles = {
    "title": "Customer Registration"     // ✓ Clear purpose
};

// Avoid
screen badTitles = {
    "title": "Screen 1"                  // ✗ Generic
};
```

**Best Practices:**
- Use descriptive titles that indicate the screen's purpose
- Keep titles concise (2-4 words)
- Use title case: "Customer Registration" not "customer registration"
- For dynamic content, update the title programmatically: `scr.setTitle(screenName, newTitle)`

---

## Variable Definition

### Basic Variable Definition

```
screen formExample = {
    "title": "User Form",
    "width": 500,
    "height": 400,
    "vars": [
        {
            "name": "firstName",
            "type": "string",
            "default": "",
            "display": {
                "type": "textfield",
                "mandatory": true,
                "labelText": "Enter first name"
            }
        },
        {
            "name": "age",
            "type": "int",
            "default": 0,
            "display": {
                "type": "textfield",
                "labelText": "Age"
            }
        },
        {
            "name": "isActive",
            "type": "bool",
            "default": true,
            "display": {
                "type": "checkbox"
            }
        }
    ]
};
```

### Variable with DisplayMetadata

```
{
    "name": "email",
    "type": "string",
    "default": "",
    "display": {
        "type": "textfield",
        "mandatory": true,
        "labelText": "Email Address:",
        "labelWidth": "120",
        "width": "250",
        "editable": true,
        "visible": true
    }
}
```

**Best Practices:**
- Always specify the `type` explicitly (string, int, bool, etc.)
- Provide meaningful `default` values
- Use `labelText` to guide users and label form fields
- Set `mandatory: true` for required fields
- Keep `labelWidth` consistent across related fields (e.g., all 100 or 120)

### Component Type Selection

| Data Type | Best Display Type | Use Case |
|-----------|------------------|----------|
| Short text | `textfield` | Names, IDs, single-line input |
| Long text | `textarea` | Comments, descriptions, multi-line |
| Password | `passwordfield` | Passwords, sensitive data |
| Boolean | `checkbox` | Yes/No, on/off flags |
| Selection | `combobox` | Choice from predefined options |
| Date | `datepicker` | Date selection |
| Number | `textfield` + validation | Numeric input |
| Read-only | `label` | Display-only data |

---

## Variable Sets

Variable sets organize related variables into logical groups with shared scope and lifecycle.

### Basic Variable Set

```
screen customerForm = {
    "title": "Customer Information",
    "width": 700,
    "height": 500,
    "sets": [
        {
            "setname": "personalInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "firstName",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield"}
                },
                {
                    "name": "lastName",
                    "type": "string",
                    "default": "",
                    "display": {"type": "textfield"}
                },
                {
                    "name": "dateOfBirth",
                    "type": "date",
                    "display": {"type": "datepicker"}
                }
            ]
        },
        {
            "setname": "contactInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "email",
                    "type": "string",
                    "display": {"type": "textfield"}
                },
                {
                    "name": "phone",
                    "type": "string",
                    "display": {"type": "textfield"}
                }
            ]
        }
    ]
};
```

**Best Practices:**
- Group related variables into sets (e.g., "address", "payment", "shipping")
- Use descriptive set names: `personalInfo` not `set1`
- Typical set scopes: `visible`, `hidden`, `internal`
- Keep sets focused - 3-8 variables per set is ideal
- Use sets to mirror your domain model or database structure

### Accessing Variable Set Values

```
// Access variables through screen reference
var fname = customerForm.firstName;
var lname = customerForm.lastName;

// Update values
customerForm.email = "new@example.com";
customerForm.phone = "555-1234";
```

---

## Area Layout

### Container Types

Choose the right container for your layout needs:

```
// Vertical layout (most common for forms)
{
    "name": "formArea",
    "type": "vbox",
    "spacing": "15",
    "padding": "20",
    "items": [/* form fields */]
}

// Horizontal layout (for button bars, toolbars)
{
    "name": "buttonBar",
    "type": "hbox",
    "spacing": "10",
    "alignment": "center-right",
    "items": [/* buttons */]
}

// Grid layout (for structured forms)
{
    "name": "gridForm",
    "type": "gridpane",
    "spacing": "10",
    "items": [
        {
            "varRef": "firstName",
            "layoutPos": "0,0"  // row,col
        },
        {
            "varRef": "lastName",
            "layoutPos": "0,1"
        }
    ]
}

// Border layout (for application frame)
{
    "name": "mainLayout",
    "type": "borderpane",
    "childAreas": [
        {
            "name": "topMenu",
            "type": "hbox",
            "layoutPos": "top"
        },
        {
            "name": "centerContent",
            "type": "vbox",
            "layoutPos": "center"
        },
        {
            "name": "statusBar",
            "type": "hbox",
            "layoutPos": "bottom"
        }
    ]
}
```

### Common Layout Patterns

#### Form Layout Pattern

```
screen formPattern = {
    "title": "Data Entry Form",
    "width": 600,
    "height": 400,
    "sets": [{
        "setname": "formData",
        "scope": "visible",
        "vars": [
            {"name": "field1", "type": "string", "display": {"type": "textfield"}},
            {"name": "field2", "type": "string", "display": {"type": "textfield"}},
            {"name": "field3", "type": "string", "display": {"type": "textarea"}}
        ]
    }],
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "spacing": "15",
            "padding": "20",
            "items": [
                {"varRef": "field1"},
                {"varRef": "field2"},
                {"varRef": "field3"},
                {
                    "name": "buttonArea",
                    "type": "hbox",
                    "spacing": "10",
                    "alignment": "center-right",
                    "items": [
                        {"type": "button", "text": "Save", "onClick": "saveData();"},
                        {"type": "button", "text": "Cancel", "onClick": "closeForm();"}
                    ]
                }
            ]
        }
    ]
};
```

#### Dashboard Pattern

```
screen dashboardPattern = {
    "title": "Dashboard",
    "width": 1200,
    "height": 800,
    "maximize": true,
    "area": [
        {
            "name": "main",
            "type": "borderpane",
            "childAreas": [
                {
                    "name": "header",
                    "type": "hbox",
                    "layoutPos": "top",
                    "padding": "10",
                    "spacing": "10",
                    "items": [
                        {"type": "label", "text": "Dashboard", "style": "-fx-font-size: 24px;"}
                    ]
                },
                {
                    "name": "content",
                    "type": "gridpane",
                    "layoutPos": "center",
                    "spacing": "20",
                    "padding": "20",
                    "childAreas": [
                        {
                            "name": "topLeft",
                            "type": "vbox",
                            "layoutPos": "0,0",
                            "items": [/* widget content */]
                        },
                        {
                            "name": "topRight",
                            "type": "vbox",
                            "layoutPos": "0,1",
                            "items": [/* widget content */]
                        }
                    ]
                }
            ]
        }
    ]
};
```

**Best Practices:**
- Use `vbox` for simple top-to-bottom forms
- Use `gridpane` for structured multi-column layouts
- Use `borderpane` for application-level layouts
- Use `hbox` for button bars and horizontal toolbars
- Always specify `spacing` and `padding` for visual breathing room
- Standard spacing: 10-15px, standard padding: 15-20px

---

## Event Handlers

EBS screens support multiple event types at different levels (item, area, and screen). Events allow you to respond to user interactions and state changes.

### Complete Event Reference

| Event | Level | Trigger | Return Value | Use Case |
|-------|-------|---------|--------------|----------|
| `startup` | Screen | Screen first shown/created | None | Initialize data, load resources |
| `cleanup` | Screen | Screen closed | None | Save state, release resources |
| `gainFocus` | Screen/Area | Screen/area gains focus | None | Refresh data, highlight UI |
| `lostFocus` | Screen/Area | Screen/area loses focus | None | Auto-save, validate changes |
| `onClick` | Item (buttons) | Button clicked | None | User actions (save, submit, navigate) |
| `onChange` | Item (input controls) | Value changes | None | Real-time calculations, updates, filtering |
| `onValidate` | Item (input controls) | Value changes (before onChange) | Boolean | Validate input, return false to reject |

### Event Execution Order

**Screen Lifecycle:**
1. `startup` executes when screen is first shown (once per screen creation)
2. Screen `gainFocus`/`lostFocus` fire when screen window gains/loses focus
3. `cleanup` executes when screen is closed (not on hide)

**Item Value Changes:**
1. `onValidate` executes first (if defined) - must return true/false
2. If validation passes, `onChange` executes (if defined)
3. Area `gainFocus`/`lostFocus` fire when focus moves between areas

### Screen-Level Events

Screen-level events are defined at the top level of the screen definition JSON and apply to the entire screen window.

#### startup Event

Executes when the screen is first created and shown. Use for initialization tasks.

```
screen myApp = {
    "title": "Application",
    "width": 800,
    "height": 600,
    "startup": "call initializeApp(); myApp.status = 'Ready';",
    "vars": [/* ... */],
    "area": [/* ... */]
};

show screen myApp;  // startup executes here on first show
```

**Use Cases:**
- Load initial data from database or files
- Initialize default values
- Set up timers or background processes
- Display welcome messages
- Connect to external resources

#### cleanup Event

Executes when the screen is closed (not on hide). Use for cleanup and resource release.

```
screen dataEntry = {
    "title": "Data Entry",
    "width": 600,
    "height": 400,
    "startup": "call openDatabaseConnection();",
    "cleanup": "call closeDatabaseConnection(); call saveUserPreferences();",
    "vars": [/* ... */]
};

close screen dataEntry;  // cleanup executes here
```

**Use Cases:**
- Close database connections
- Save user preferences or state
- Release file handles or network connections
- Stop timers or background threads
- Log user activity

#### Screen gainFocus Event

Executes when the screen window gains focus (becomes active).

```
screen dashboard = {
    "title": "Dashboard",
    "width": 1000,
    "height": 700,
    "gainFocus": "call refreshDashboardData(); print 'Dashboard is now active';",
    "vars": [/* ... */]
};
```

**Use Cases:**
- Refresh displayed data
- Resume timers or updates
- Highlight UI elements
- Log focus events

#### Screen lostFocus Event

Executes when the screen window loses focus (becomes inactive).

```
screen editor = {
    "title": "Document Editor",
    "width": 900,
    "height": 600,
    "lostFocus": "call autoSaveDocument(); print 'Editor lost focus';",
    "vars": [/* ... */]
};
```

**Use Cases:**
- Auto-save changes
- Pause timers or background updates
- Validate unsaved changes
- Log focus events

**Best Practices for Screen-Level Events:**
- Keep startup code fast - avoid long-running operations
- Always clean up resources in cleanup to prevent memory leaks
- Use gainFocus to refresh time-sensitive data
- Use lostFocus for auto-save functionality
- Remember: cleanup only fires on close, not on hide
- Screen-level gainFocus/lostFocus are different from area-level events

### onClick Events

Used for buttons and clickable items. Executes when the user clicks the control.

```
// Simple inline handler
{
    "type": "button",
    "text": "Save",
    "onClick": "call saveRecord();"
}

// Multi-line inline handler
{
    "type": "button",
    "text": "Calculate",
    "onClick": "var total = price * quantity; orderScreen.result = total; print 'Total: ' + total;"
}

// Calling multiple functions
{
    "type": "button",
    "text": "Submit",
    "onClick": "if call validateForm() then { call submitData(); call closeForm(); } end if;"
}
```

**Best Practices:**
- Keep onClick handlers short - call functions for complex logic
- Use onClick for discrete user actions (save, delete, calculate, navigate)
- Always validate before performing destructive actions
- Provide user feedback after actions complete

### onChange Events

Used for input controls (textfield, textarea, checkbox, combobox, etc.). Executes whenever the value changes.

```
// Real-time calculation
{
    "name": "quantity",
    "type": "int",
    "default": 1,
    "display": {
        "type": "textfield",
        "onChange": "call calculateTotal();"
    }
}

// Update dependent fields
{
    "name": "country",
    "type": "string",
    "display": {
        "type": "combobox",
        "onChange": "call loadStatesForCountry(orderScreen.country);"
    }
}

// Enable/disable based on checkbox
{
    "name": "enableAdvanced",
    "type": "bool",
    "default": false,
    "display": {
        "type": "checkbox",
        "onChange": "call scr.setProperty('myScreen.advancedPanel', 'disabled', not enableAdvanced);"
    }
}
```

**Best Practices:**
- Use onChange for real-time updates (calculations, dependent fields)
- Avoid expensive operations in onChange (use debouncing if needed)
- onChange fires after successful validation
- Can update other screen variables or UI properties

### onValidate Events

Used for input controls to validate user input. Must return a boolean (true = valid, false = invalid).

```
// Email validation
{
    "name": "email",
    "type": "string",
    "display": {
        "type": "textfield",
        "onValidate": "return call validateEmail(email);"
    }
}

// Inline validation
{
    "name": "age",
    "type": "int",
    "display": {
        "type": "textfield",
        "onValidate": "if age < 18 or age > 100 then { return false; } return true;"
    }
}

// Complex validation with multiple checks
{
    "name": "password",
    "type": "string",
    "display": {
        "type": "passwordfield",
        "onValidate": "var len = call string.length(password); if len < 8 then { return false; } if len > 50 then { return false; } return true;"
    }
}
```

**Best Practices:**
- Always return a boolean value (true or false)
- Runs before onChange - if validation fails, onChange doesn't execute
- Use for input validation, range checks, format verification
- Keep validation logic simple and fast
- Consider providing user feedback on validation failure
- Item-level onValidate takes precedence over variable-level onValidate

### Area-Level Events

Areas support focus events that fire when focus enters or leaves the area.

```
// Focus tracking
{
    "name": "formArea",
    "type": "vbox",
    "gainFocus": "print 'Form area gained focus'; formScreen.currentSection = 'form';",
    "lostFocus": "print 'Form area lost focus'; call autoSaveFormData();",
    "items": [/* ... */]
}

// Highlight active section
{
    "name": "detailsSection",
    "type": "vbox",
    "gainFocus": "call scr.setProperty('myScreen.detailsSection', 'style', '-fx-background-color: #e8f4f8;');",
    "lostFocus": "call scr.setProperty('myScreen.detailsSection', 'style', '-fx-background-color: transparent;');",
    "items": [/* ... */]
}
```

**Best Practices:**
- Use gainFocus to highlight active sections or load section-specific data
- Use lostFocus for auto-save functionality or section validation
- Keep focus handlers lightweight
- Focus events fire when keyboard or mouse focus changes

**Best Practices:**
- Keep event handlers short - call functions for complex logic
- Define validation functions separately and call them from `onValidate`
- Use `onChange` for real-time updates (calculations, filtering)
- Use `onClick` for user actions (save, delete, navigate)
- Test event handlers thoroughly - they execute in screen thread context

### Event Handler Pattern

```
// Define helper functions
calculateTotal() return double {
    var quantity: int = orderScreen.quantity;
    var price: double = orderScreen.unitPrice;
    return quantity * price;
}

validateEmail(email: string) return bool {
    // Simple email validation
    if email == "" then return false; end if;
    if call string.contains(email, "@") == false then return false; end if;
    return true;
}

// Use in screen definition
screen orderScreen = {
    "title": "Order Form",
    "width": 500,
    "height": 400,
    "vars": [
        {
            "name": "quantity",
            "type": "int",
            "default": 1,
            "display": {
                "type": "textfield",
                "onChange": "orderScreen.total = call calculateTotal();"
            }
        },
        {
            "name": "unitPrice",
            "type": "double",
            "default": 0.0,
            "display": {
                "type": "textfield",
                "onChange": "orderScreen.total = call calculateTotal();"
            }
        },
        {
            "name": "total",
            "type": "double",
            "default": 0.0,
            "display": {
                "type": "label"
            }
        },
        {
            "name": "email",
            "type": "string",
            "display": {
                "type": "textfield",
                "onValidate": "return call validateEmail(orderScreen.email);"
            }
        }
    ]
};
```

---

## Thread Safety

Each screen runs in its own thread. Follow these practices for thread-safe screen programming:

### Accessing Screen Variables

```
// CORRECT: Use screenName.varName syntax
var customerName = customerScreen.firstName;
customerScreen.lastName = "Smith";

// CORRECT: Access from any thread
mainScreen.status = "Processing...";
detailScreen.recordId = 123;
```

### Cross-Screen Communication

```
// Screen 1 updates a shared variable
screen1.sharedData = "Important information";

// Screen 2 reads the variable
var data = screen1.sharedData;
print "Received: " + data;
```

### Screen Lifecycle

```
// Define screen (stores configuration, doesn't create window)
screen myScreen = {
    "title": "My Screen",
    "width": 600,
    "height": 400
};

// Create and show screen (lazy initialization)
show screen myScreen;

// Hide screen (keeps window in memory)
hide screen myScreen;

// Show again (reuses existing window)
show screen myScreen;

// Close screen (destroys window)
close screen myScreen;

// Can show again (creates new window)
show screen myScreen;
```

**Best Practices:**
- Always access screen variables using `screenName.varName` syntax
- Screen variables are thread-safe by design
- Use `hide` instead of `close` if you'll show the screen again
- First `show` creates the window (lazy initialization)
- Closed screens can be shown again (creates new window)

---

## Common Patterns

### Master-Detail Pattern

```
// Master screen - list of records
screen masterScreen = {
    "title": "Customer List",
    "width": 400,
    "height": 600,
    "vars": [
        {
            "name": "selectedId",
            "type": "int",
            "default": 0
        }
    ],
    "area": [
        {
            "name": "list",
            "type": "vbox",
            "items": [
                {
                    "type": "button",
                    "text": "View Details",
                    "onClick": "call showDetail(masterScreen.selectedId);"
                }
            ]
        }
    ]
};

// Detail screen - individual record
screen detailScreen = {
    "title": "Customer Details",
    "width": 600,
    "height": 400,
    "vars": [
        {
            "name": "customerId",
            "type": "int",
            "default": 0
        },
        {
            "name": "name",
            "type": "string",
            "display": {"type": "textfield"}
        }
    ]
};

// Function to show detail
showDetail(id: int) {
    detailScreen.customerId = id;
    // Load data based on ID
    detailScreen.name = "Customer " + id;
    show screen detailScreen;
}
```

### Form Validation Pattern

```
validateForm() return bool {
    var isValid: bool = true;
    
    // Check required fields
    if formScreen.firstName == "" then {
        print "First name is required";
        isValid = false;
    }
    
    if formScreen.email == "" then {
        print "Email is required";
        isValid = false;
    } else {
        if call string.contains(formScreen.email, "@") == false then {
            print "Invalid email format";
            isValid = false;
        }
    }
    
    return isValid;
}

// Use in button onClick
{
    "type": "button",
    "text": "Submit",
    "onClick": "if call validateForm() then { call submitForm(); } end if;"
}
```

### Progress Indicator Pattern

```
screen progressScreen = {
    "title": "Processing",
    "width": 400,
    "height": 200,
    "vars": [
        {
            "name": "status",
            "type": "string",
            "default": "Starting...",
            "display": {"type": "label"}
        },
        {
            "name": "progress",
            "type": "double",
            "default": 0.0,
            "display": {"type": "progressbar"}
        }
    ]
};

// Update progress in long-running operation
processData() {
    show screen progressScreen;
    
    var total: int = 100;
    var i: int = 0;
    while i < total do {
        // Do work
        progressScreen.status = "Processing item " + i;
        progressScreen.progress = i / total;
        i = i + 1;
    }
    
    progressScreen.status = "Complete!";
    progressScreen.progress = 1.0;
}
```

---

## Performance Tips

### 1. Use Lazy Initialization

Screens are initialized lazily by default - the JavaFX Stage is only created on first `show`.

```
// Define multiple screens - no performance impact
screen screen1 = {...};
screen screen2 = {...};
screen screen3 = {...};

// Only create windows when needed
show screen screen1;  // Creates window here
```

### 2. Minimize Nested Areas

```
// AVOID: Deep nesting
{
    "type": "vbox",
    "childAreas": [
        {
            "type": "hbox",
            "childAreas": [
                {
                    "type": "vbox",
                    "childAreas": [/* ... */]
                }
            ]
        }
    ]
}

// PREFER: Flat structure when possible
{
    "type": "gridpane",
    "items": [/* items directly */]
}
```

### 3. Reuse Screens Instead of Creating New Ones

```
// GOOD: Reuse same screen
show screen detailScreen;
detailScreen.recordId = newId;
detailScreen.name = newName;

// AVOID: Don't recreate screens unnecessarily
close screen detailScreen;
screen detailScreen = {...};  // Avoid redefining
```

### 4. Use Hide Instead of Close

```
// PREFER: Hide preserves window (if screen will be used again)
hide screen tempScreen;    // Fast to show again
show screen tempScreen;    // Instant

// AVOID: Close destroys window
close screen tempScreen;   // Destroys resources
show screen tempScreen;    // Must recreate (slower)
```

**Important:** If a window will not be used again or is used infrequently, close it to free up resources. Use `hide` for screens that will be shown again soon or frequently. Use `close` for one-time dialogs or screens that are rarely accessed.

### 5. Optimize Event Handlers

```
// GOOD: Call external function
{
    "onClick": "call processData();"
}

// AVOID: Complex inline logic
{
    "onClick": "var x = 1; while x < 100 do { /* complex logic */ x++; }"
}
```

---

## Anti-Patterns to Avoid

### ❌ Don't: Recreate Screens in Loops

```
// BAD - Creates new screen definition each iteration
var i: int = 0;
while i < 10 do {
    screen temp = {"title": "Window " + i};  // Creates 10 screens!
    show screen temp;
    i = i + 1;
}
```

**Do Instead:**
```
// GOOD - Define once, reuse
screen temp = {"title": "Window"};
var i: int = 0;
while i < 10 do {
    temp.data = i;
    show screen temp;
    i = i + 1;
}
```

### ❌ Don't: Use Generic Names

```
// BAD
screen scr1 = {"title": "Screen 1"};
var var1: string;
var var2: string;
```

**Do Instead:**
```
// GOOD
screen customerForm = {"title": "Customer Registration"};
var firstName: string;
var lastName: string;
```

### ❌ Don't: Omit Type Declarations

```
// BAD - Type inference can be unclear
{
    "name": "count",
    "default": 0
}
```

**Do Instead:**
```
// GOOD - Explicit type
{
    "name": "count",
    "type": "int",
    "default": 0
}
```

### ❌ Don't: Put Business Logic in Event Handlers

```
// BAD - Complex logic inline
{
    "onClick": "var total = 0; var i = 0; while i < items.length do { total += items[i].price * items[i].qty; i++; } result = total * 1.08;"
}
```

**Do Instead:**
```
// GOOD - Separate function
{
    "onClick": "call calculateOrderTotal();"
}
```

### ❌ Don't: Forget to Handle Errors

```
// BAD - No validation
{
    "onClick": "call processOrder();"
}
```

**Do Instead:**
```
// GOOD - With validation
{
    "onClick": "if call validateOrder() then { call processOrder(); } else { print 'Validation failed'; } end if;"
}
```

### ❌ Don't: Mix Concerns in One Screen

```
// BAD - One screen doing too much
screen everythingScreen = {
    "title": "Application",
    "width": 1600,
    "height": 1200,
    // Customer entry, order processing, reporting, settings...
};
```

**Do Instead:**
```
// GOOD - Separate concerns
screen customerScreen = {"title": "Customer Management"};
screen orderScreen = {"title": "Order Processing"};
screen reportScreen = {"title": "Reports"};
screen settingsScreen = {"title": "Settings"};
```

---

## Quick Reference Checklist

Use this checklist when creating screens:

**Screen Definition:**
- [ ] Descriptive `title` that indicates purpose
- [ ] Appropriate `width` and `height` for content
- [ ] Consider `maximize: true` for data-heavy screens

**Variables:**
- [ ] Explicit `type` declarations
- [ ] Meaningful `default` values
- [ ] Appropriate display `type` for data type
- [ ] `mandatory: true` for required fields
- [ ] Helpful `labelText` for user guidance
- [ ] Consistent `labelWidth` across related fields

**Variable Sets:**
- [ ] Logical grouping of related variables
- [ ] Descriptive `setname` values
- [ ] Appropriate `scope` (visible/hidden)
- [ ] 3-8 variables per set

**Layout:**
- [ ] Appropriate container `type` (vbox, hbox, gridpane, borderpane)
- [ ] Consistent `spacing` (10-15px)
- [ ] Adequate `padding` (15-20px)
- [ ] Proper `alignment` for content
- [ ] Not deeply nested (2-3 levels max)

**Event Handlers:**
- [ ] Short inline handlers or function calls
- [ ] Validation functions for `onValidate`
- [ ] Error handling in click handlers
- [ ] Testing of all event handlers

**Thread Safety:**
- [ ] Use `screenName.varName` for all access
- [ ] Test cross-screen communication
- [ ] Proper screen lifecycle (show/hide/close)

**Performance:**
- [ ] Reuse screens instead of recreating
- [ ] Use `hide` instead of `close` when appropriate
- [ ] Minimize nested areas
- [ ] Complex logic in functions, not inline

---

## Example: Complete Best Practice Screen

```
// Helper functions
validateCustomerForm() return bool {
    if customerForm.firstName == "" then {
        print "First name is required";
        return false;
    }
    if customerForm.email == "" then {
        print "Email is required";
        return false;
    }
    if call string.contains(customerForm.email, "@") == false then {
        print "Invalid email format";
        return false;
    }
    return true;
}

saveCustomer() {
    if call validateCustomerForm() == false then return; end if;
    
    print "Saving customer: " + customerForm.firstName + " " + customerForm.lastName;
    // Database save logic here
    
    customerForm.status = "Saved successfully!";
}

// Screen definition following best practices
screen customerForm = {
    "name": "customerForm",
    "title": "Customer Registration",
    "width": 600,
    "height": 500,
    "sets": [
        {
            "setname": "personalInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "firstName",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "mandatory": true,
                        "labelText": "First Name:",
                        "labelWidth": "120",
                        "width": "250"
                    }
                },
                {
                    "name": "lastName",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "mandatory": true,
                        "labelText": "Last Name:",
                        "labelWidth": "120",
                        "width": "250"
                    }
                },
                {
                    "name": "dateOfBirth",
                    "type": "date",
                    "display": {
                        "type": "datepicker",
                        "labelText": "Date of Birth:",
                        "labelWidth": "120"
                    }
                }
            ]
        },
        {
            "setname": "contactInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "email",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "mandatory": true,
                        "labelText": "Email:",
                        "labelWidth": "120",
                        "width": "250",
                        "onValidate": "return call string.contains(customerForm.email, '@');"
                    }
                },
                {
                    "name": "phone",
                    "type": "string",
                    "default": "",
                    "display": {
                        "type": "textfield",
                        "labelText": "Phone:",
                        "labelWidth": "120",
                        "width": "250"
                    }
                }
            ]
        },
        {
            "setname": "status",
            "scope": "visible",
            "vars": [
                {
                    "name": "status",
                    "type": "string",
                    "default": "Ready",
                    "display": {
                        "type": "label",
                        "style": "-fx-font-weight: bold;"
                    }
                }
            ]
        }
    ],
    "area": [
        {
            "name": "mainArea",
            "type": "vbox",
            "spacing": "15",
            "padding": "20",
            "items": [
                {"varRef": "firstName"},
                {"varRef": "lastName"},
                {"varRef": "dateOfBirth"},
                {"varRef": "email"},
                {"varRef": "phone"},
                {
                    "name": "buttonBar",
                    "type": "hbox",
                    "spacing": "10",
                    "alignment": "center-right",
                    "padding": "10 0 0 0",
                    "items": [
                        {
                            "type": "button",
                            "text": "Save",
                            "onClick": "call saveCustomer();"
                        },
                        {
                            "type": "button",
                            "text": "Cancel",
                            "onClick": "hide screen customerForm;"
                        }
                    ]
                },
                {"varRef": "status"}
            ]
        }
    ]
};

// Show the screen
show screen customerForm;
```

---

## Additional Resources

- **[AREA_DEFINITION.md](../../docs/AREA_DEFINITION.md)** - Complete reference for areas, items, and properties
- **[EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md)** - Full EBS language syntax
- **[SCREEN_LIFECYCLE_DIAGRAM.md](../../docs/SCREEN_LIFECYCLE_DIAGRAM.md)** - Understanding screen creation and lifecycle
- **[Examples](../scripts/examples/)** - Real-world screen examples

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Maintained by:** EBS Development Team
