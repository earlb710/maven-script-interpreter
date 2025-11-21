# Lazy Screen Initialization - Verification

## Overview
This change implements lazy screen initialization where screens are only created and added to the global list when `show screen` is called, not when `screen xy = {...}` is executed.

## Expected Behavior

### Before Changes
```ebs
screen myScreen = {
    "title": "Test",
    "width": 400,
    "height": 300
};
// At this point:
// - JavaFX Stage was created
// - Stage was added to GLOBAL_SCREENS map
// - Screen was visible in global list
```

### After Changes
```ebs
screen myScreen = {
    "title": "Test",
    "width": 400,
    "height": 300
};
// At this point:
// - NO JavaFX Stage created yet
// - Screen config stored in screenConfigs map
// - Screen NOT in GLOBAL_SCREENS map
// - Screen NOT visible in global list

show screen myScreen;
// Now:
// - JavaFX Stage is created
// - Stage is added to GLOBAL_SCREENS map
// - Screen is visible in global list
// - Screen window appears
```

## Code Flow

### 1. Screen Definition (`screen xy = {...}`)
- **File**: InterpreterScreen.java, method `visitScreenStatement()`
- **What happens**:
  1. Parse and validate configuration JSON
  2. Create storage structures (screenVars, screenVarTypes, varSets, etc.)
  3. Process variable definitions and area definitions
  4. Store everything in InterpreterContext
  5. Create ScreenConfig object with all the data
  6. Store ScreenConfig in `context.screenConfigs` map
  7. **NO Stage creation**
  8. **NO addition to GLOBAL_SCREENS**
  9. Output: "Screen 'name' defined. Use 'show screen name;' to display it."

### 2. Screen Show (`show screen xy`)
- **File**: InterpreterScreen.java, method `visitScreenShowStatement()`
- **What happens**:
  1. Check if screen config exists
  2. **If Stage doesn't exist**, call `createStageForScreen()`
     - Retrieves ScreenConfig from context
     - Creates JavaFX Stage on JavaFX Application Thread
     - Sets up event handlers, threads, etc.
     - **Adds Stage to GLOBAL_SCREENS map**
     - Adds to screen creation order
  3. If Stage exists, just show it
  4. Execute startup code if present

### 3. Screen Hide (`hide screen xy`)
- **File**: InterpreterScreen.java, method `visitScreenHideStatement()`
- **What happens**:
  1. Check if screen config or Stage exists
  2. **If Stage doesn't exist yet** (screen never shown):
     - Output: "Screen is not shown (has not been created yet)"
     - Return early
  3. If Stage exists, hide it

### 4. Screen Close (`close screen xy`)
- **File**: InterpreterScreen.java, method `visitScreenCloseStatement()`
- **What happens**:
  1. Check if screen config or Stage exists
  2. **If Stage doesn't exist yet** (screen never shown):
     - Remove config from context
     - Output: "Screen definition removed (was not shown)"
     - Return early
  3. If Stage exists, close it and clean up resources

## Key Classes Modified

### 1. ScreenConfig.java (NEW)
- Stores all screen configuration data
- Used for lazy initialization
- Contains: name, title, size, variables, areas, inline code, etc.

### 2. InterpreterContext.java
- Added `screenConfigs` map to store ScreenConfig objects
- Added `hasScreenConfig()`, `getScreenConfig()`, `setScreenConfig()` methods
- Updated `clear()` and `remove()` to handle screenConfigs

### 3. InterpreterScreen.java
- Refactored `visitScreenStatement()` to only store config
- Added `createStageForScreen()` method for lazy Stage creation
- Modified `visitScreenShowStatement()` to create Stage on first show
- Updated `visitScreenHideStatement()` to handle unshown screens
- Updated `visitScreenCloseStatement()` to handle unshown screens

### 4. BuiltinsScreen.java
- Updated `screenShow()` to handle unshown screens
- Updated `screenHide()` to handle unshown screens
- Updated `screenClose()` to handle unshown screens

## Benefits

1. **Reduced Resource Usage**: Screens that are defined but never shown don't consume JavaFX resources
2. **Cleaner API**: Clear separation between screen definition and screen display
3. **Better Control**: Developers can define multiple screens and only show the ones they need
4. **Consistent with Issue Request**: Screens are only in the global list when they're actually shown

## Testing Approach

Since this is a GUI application requiring JavaFX and this environment is headless, testing should be done by:

1. Running the application with JavaFX enabled
2. Creating a screen with `screen xy = {...}`
3. Verifying the screen window does NOT appear
4. Verifying screen variables are still accessible (e.g., `xy.varName`)
5. Calling `show screen xy`
6. Verifying the screen window NOW appears
7. Testing hide/close operations on both shown and unshown screens

## Example Test Script

```ebs
// Define screen - should not create window
print "Defining screen...";
screen testScreen = {
    "title": "Lazy Init Test",
    "width": 400,
    "height": 300,
    "vars": [{
        "name": "message",
        "type": "string",
        "default": "Hello"
    }]
};

print "Screen defined - NO window should appear yet";
print "Can still access variables: " + testScreen.message;

// Now show it - window should appear
print "Showing screen...";
show screen testScreen;
print "Window should NOW be visible!";
```
