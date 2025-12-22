# Screen Inline Code Context Fix and Enhancements

## Summary
Fixed button onClick handlers to execute inline code with proper screen context, and added support for `startup` and `cleanup` inline code properties in screen definitions.

## Changes

### 1. Button onClick Context Fix
Fixed button onClick handlers to execute with proper screen context, allowing statements like `close screen;` and `hide screen;` to work without explicit screen names.

### 2. Startup and Cleanup Inline Code (NEW)
Added support for `startup` and `cleanup` properties in screen definitions that execute inline EBS code at screen lifecycle events.

## Problem
When button onClick code was executed, it ran on the JavaFX Application Thread rather than in the screen's thread context. The `getCurrentScreen()` method determined the current screen by checking the thread name for a "Screen-" prefix, which failed for onClick handlers executing on the JavaFX Application Thread.

## Solution
Implemented ThreadLocal-based screen context tracking that works independently of thread names:

1. **InterpreterContext.java**: Added `ThreadLocal<String> CURRENT_SCREEN_CONTEXT`
   - `getCurrentScreen()` now checks ThreadLocal first before falling back to thread name
   - `setCurrentScreen(String)` sets the ThreadLocal variable
   - `clearCurrentScreen()` removes the ThreadLocal to prevent memory leaks
   - Added storage for `startup` and `cleanup` inline code

2. **InterpreterScreen.java**: Modified inline code execution to set/clear context
   - Calls `context.setCurrentScreen(screenName)` before executing inline code
   - Executes code within try-finally block
   - Calls `context.clearCurrentScreen()` in finally to ensure cleanup
   - Added `executeScreenInlineCode()` helper method for consistent execution

## Technical Details

### onClick Context Fix

#### Before
```java
ScreenFactory.OnClickHandler onClickHandler = (ebsCode) -> {
    try {
        RuntimeContext clickContext = Parser.parse("onClick_" + screenName, ebsCode);
        for (Statement s : clickContext.statements) {
            interpreter.acceptStatement(s);  // No screen context!
        }
    } catch (ParseError e) { ... }
};
```

#### After
```java
ScreenFactory.OnClickHandler onClickHandler = (ebsCode) -> {
    try {
        context.setCurrentScreen(screenName);  // Set context
        try {
            RuntimeContext clickContext = Parser.parse("onClick_" + screenName, ebsCode);
            for (Statement s : clickContext.statements) {
                interpreter.acceptStatement(s);  // Now has screen context
            }
        } finally {
            context.clearCurrentScreen();  // Always clean up
        }
    } catch (ParseError e) { ... }
};
```

### Startup and Cleanup Inline Code (NEW)

Screen definitions can now include `startup` and `cleanup` properties with inline EBS code:

```javascript
screen myScreen = {
    "title": "My Screen",
    "startup": "counter = 0; message = 'Initialized'; print 'Screen ready';",
    "cleanup": "print 'Saving state...'; print 'Final counter: ' + counter;",
    "vars": [
        {
            "name": "counter",
            "type": "int",
            "default": 0
        },
        {
            "name": "message",
            "type": "string",
            "default": ""
        }
    ],
    "area": [ /* ... */ ]
};
```

**Execution Flow:**
1. **Screen Creation**: Properties are extracted and stored
2. **First Show**: `startup` code executes with screen context
3. **User Interaction**: Variables can be modified, buttons clicked
4. **Screen Close**: `cleanup` code executes before resources are destroyed

**Characteristics:**
- Both have full access to screen variables
- Both can use contextual screen statements (`close screen;`, `hide screen;`, etc.)
- `startup` runs ONCE when screen is first shown (not on re-show after hide)
- `cleanup` runs when screen is closed
- Errors are caught and logged but don't prevent screen show/close
- Both are optional - screens work normally without them

## Benefits
1. ✅ onClick handlers can now use `close screen;` without explicit names
2. ✅ onClick handlers can now use `hide screen;` without explicit names
3. ✅ onClick handlers can now use `show screen;` without explicit names
4. ✅ **NEW**: Screens can run initialization code on startup
5. ✅ **NEW**: Screens can run cleanup/save code on close
6. ✅ Proper cleanup prevents ThreadLocal memory leaks
7. ✅ No security vulnerabilities (verified by CodeQL)
8. ✅ Backward compatible - existing code continues to work

## Testing
Test scripts created to verify functionality:
- `test_button_onclick_context.ebs`: Multiple onClick scenarios
- `test_button_onclick_quick.ebs`: Quick onClick verification
- `test_screen_startup_cleanup.ebs`: Startup and cleanup code execution

## Example Usage

### onClick Context
```javascript
screen myScreen = {
    "title": "Example",
    "width": 300,
    "height": 200,
    "area": [{
        "name": "main",
        "type": "vbox",
        "items": [{
            "name": "closeBtn",
            "seq": 1,
            "display": {
                "type": "button",
                "labelText": "Close",
                "onClick": "close screen;"  // ✅ Works now!
            }
        }]
    }]
};
```

### Startup and Cleanup
```javascript
screen dataEntry = {
    "title": "Data Entry",
    "startup": "loadDefaults(); print 'Form ready';",
    "cleanup": "saveData(); print 'Data saved';",
    "vars": [ /* ... */ ],
    "area": [ /* ... */ ]
};

loadDefaults() {
    // Load default values into form
    dataEntry.companyName = "ACME Corp";
    dataEntry.status = "Active";
}

saveData() {
    // Save form data
    print "Saving: " + dataEntry.companyName;
}
```

## Files Modified
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/InterpreterContext.java`
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java`

## Impact
- **Low Risk**: Minimal code changes, focused fix
- **High Value**: Enables documented feature to work correctly
- **No Breaking Changes**: Backward compatible with existing code
