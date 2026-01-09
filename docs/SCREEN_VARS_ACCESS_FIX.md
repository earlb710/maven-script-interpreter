# Screen Vars Access Fix

## Problem
Event code (onClick, onChange, thread.timer callbacks) could not access screen variables directly by name. They had to use qualified notation like `screenName.varName`, which was inconvenient and verbose.

## Root Cause
When event code was executed via `executeCodeDirectly()` or through the screen thread's `CodeExecutor`, the screen context was set but screen variables were not added to the environment scope. This meant the interpreter couldn't resolve simple variable names like `counter` - only fully qualified names like `myScreen.counter` would work.

## Solution
Modified the event code execution flow to push screen variables into a new environment scope before execution and pop the scope after execution.

### Key Changes

#### 1. InterpreterScreen.java
- **Added `pushScreenVarsToEnvironment(String screenName)`**
  - Creates a new environment scope using `environment.pushEnvironmentValues()`
  - Adds all screen variables from the screen's variable map to this scope
  - Converts `NULL_SENTINEL` values back to null for the environment
  
- **Added `popScreenVarsFromEnvironment(String screenName)`**
  - Removes the environment scope to prevent variable leakage
  - Uses `environment.popEnvironmentValues()`
  
- **Modified `createScreenThread()` CodeExecutor**
  - Calls `pushScreenVarsToEnvironment()` before executing EBS code
  - Calls `popScreenVarsFromEnvironment()` after execution (in finally block)
  
- **Modified `executeCodeDirectly()`**
  - Calls `pushScreenVarsToEnvironment()` before executing EBS code
  - Calls `popScreenVarsFromEnvironment()` after execution (in finally block)
  
- **Modified `executeScreenInlineCode()`**
  - Added push/pop for the fallback case when dispatcher is not available
  - Dispatcher case already handled by CodeExecutor

#### 2. BuiltinsThread.java
- **Added static `pushScreenVarsToEnvironment(String screenName, InterpreterContext context)`**
  - Similar to InterpreterScreen version but works with the main interpreter
  - Creates new scope and adds screen variables
  
- **Added static `popScreenVarsFromEnvironment(String screenName, InterpreterContext context)`**
  - Pops the scope from the main interpreter's environment
  
- **Modified `createTimerTask()`**
  - Checks if timer is associated with a screen (currentScreen != null)
  - Calls `pushScreenVarsToEnvironment()` before callback execution
  - Calls `popScreenVarsFromEnvironment()` after callback (in finally block)

## How It Works

### Before Fix
```javascript
screen myScreen = {
    "vars": [
        { "name": "counter", "type": "int", "default": 0 }
    ]
};

// onClick handler - had to use qualified notation
onClick: "myScreen.counter = myScreen.counter + 1;"
```

### After Fix
```javascript
screen myScreen = {
    "vars": [
        { "name": "counter", "type": "int", "default": 0 }
    ]
};

// onClick handler - can now use direct access
onClick: "counter = counter + 1;"
```

### Scope Management
1. Event code is about to execute
2. New environment scope is created
3. Screen variables are added to the new scope
4. Event code executes (can access vars directly)
5. Scope is popped (variables removed from environment)
6. No leakage to other code

## Benefits
- **More concise code**: Event handlers are shorter and more readable
- **Better developer experience**: Natural variable access pattern
- **No breaking changes**: Qualified notation still works
- **Proper isolation**: Scope management prevents variable leakage
- **Consistent behavior**: Works for all event types (onClick, onChange, startup, cleanup, timer callbacks)

## Testing
Two test scripts are provided:
- `test_screen_vars_access.ebs` - Tests button onClick handlers
- `test_screen_timer_vars_access.ebs` - Tests thread.timer callbacks

## Security
- CodeQL scan: 0 alerts
- No security vulnerabilities introduced
- Proper scope cleanup prevents memory leaks

## Compatibility
- Fully backward compatible
- Old code using qualified notation continues to work
- New code can use direct access
- No changes needed to existing scripts
