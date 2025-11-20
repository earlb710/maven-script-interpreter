# Button onClick Screen Context Fix

## Summary
Fixed button onClick handlers to execute inline code with proper screen context, allowing statements like `close screen;` and `hide screen;` to work without explicit screen names.

## Problem
When button onClick code was executed, it ran on the JavaFX Application Thread rather than in the screen's thread context. The `getCurrentScreen()` method determined the current screen by checking the thread name for a "Screen-" prefix, which failed for onClick handlers executing on the JavaFX Application Thread.

## Solution
Implemented ThreadLocal-based screen context tracking that works independently of thread names:

1. **InterpreterContext.java**: Added `ThreadLocal<String> CURRENT_SCREEN_CONTEXT`
   - `getCurrentScreen()` now checks ThreadLocal first before falling back to thread name
   - `setCurrentScreen(String)` sets the ThreadLocal variable
   - `clearCurrentScreen()` removes the ThreadLocal to prevent memory leaks

2. **InterpreterScreen.java**: Modified onClick handler to set/clear context
   - Calls `context.setCurrentScreen(screenName)` before executing inline code
   - Executes code within try-finally block
   - Calls `context.clearCurrentScreen()` in finally to ensure cleanup

## Technical Details

### Before
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

### After
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

## Benefits
1. ✅ onClick handlers can now use `close screen;` without explicit names
2. ✅ onClick handlers can now use `hide screen;` without explicit names
3. ✅ onClick handlers can now use `show screen;` without explicit names
4. ✅ Proper cleanup prevents ThreadLocal memory leaks
5. ✅ No security vulnerabilities (verified by CodeQL)
6. ✅ Backward compatible - existing code continues to work

## Testing
Test scripts created to verify functionality:
- Basic: Button that increments counter and closes screen
- Comprehensive: Multiple scenarios testing close, hide, and variable operations

## Example Usage
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

## Files Modified
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/InterpreterContext.java`
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java`

## Impact
- **Low Risk**: Minimal code changes, focused fix
- **High Value**: Enables documented feature to work correctly
- **No Breaking Changes**: Backward compatible with existing code
