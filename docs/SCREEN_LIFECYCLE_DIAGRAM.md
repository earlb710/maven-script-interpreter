# Screen Lifecycle Comparison

## BEFORE (Eager Initialization)

```
┌─────────────────────────────────────────────────────────────┐
│ User Code: screen xy = {...}                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ visitScreenStatement()                                       │
│ • Parse configuration                                        │
│ • Create variables, areas                                    │
│ • CREATE JAVAFX STAGE ← Happens immediately!                │
│ • ADD TO GLOBAL_SCREENS ← Added right away!                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
                ┌──────────────────────┐
                │ GLOBAL_SCREENS Map   │
                │ ["xy" → Stage]       │  ← Screen in list
                └──────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ User Code: show screen xy                                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ visitScreenShowStatement()                                   │
│ • Get existing Stage from GLOBAL_SCREENS                    │
│ • Call stage.show()                                          │
└─────────────────────────────────────────────────────────────┘
```

## AFTER (Lazy Initialization)

```
┌─────────────────────────────────────────────────────────────┐
│ User Code: screen xy = {...}                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ visitScreenStatement()                                       │
│ • Parse configuration                                        │
│ • Create variables, areas                                    │
│ • Create ScreenConfig object                                 │
│ • ADD TO screenConfigs ← Only config stored!                │
│ • NO STAGE CREATED ✓                                         │
│ • NOT IN GLOBAL_SCREENS ✓                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
                ┌──────────────────────┐
                │ screenConfigs Map    │
                │ ["xy" → Config]      │  ← Config stored
                └──────────────────────┘
                            
                ┌──────────────────────┐
                │ GLOBAL_SCREENS Map   │
                │ [empty]              │  ← No Stage yet!
                └──────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ User Code: show screen xy                                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ visitScreenShowStatement()                                   │
│ • Check if Stage exists in GLOBAL_SCREENS                   │
│ • Stage not found!                                           │
│ • Call createStageForScreen()                                │
│   ┌────────────────────────────────────────────────────┐   │
│   │ createStageForScreen()                              │   │
│   │ • Get ScreenConfig from screenConfigs               │   │
│   │ • CREATE JAVAFX STAGE ← First time!                │   │
│   │ • ADD TO GLOBAL_SCREENS ← Added now!               │   │
│   └────────────────────────────────────────────────────┘   │
│ • Call stage.show()                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
                ┌──────────────────────┐
                │ GLOBAL_SCREENS Map   │
                │ ["xy" → Stage]       │  ← Stage added!
                └──────────────────────┘
```

## Key Differences

| Aspect | Before | After |
|--------|--------|-------|
| **Stage Creation** | Immediate (on screen definition) | Deferred (on first show) |
| **GLOBAL_SCREENS Entry** | Added on screen definition | Added on first show |
| **Resource Usage** | All defined screens use JavaFX resources | Only shown screens use JavaFX resources |
| **Window Appearance** | Not visible but created | Not created until shown |
| **Memory Footprint** | Higher (all Stages created) | Lower (only shown Stages created) |

## State Diagram

### Before
```
screen xy = {...}  →  [Stage Created + In Global List]  →  show screen xy  →  [Stage Shown]
                      ↑ Happens here                                          ↑ Just shows
```

### After
```
screen xy = {...}  →  [Config Stored]  →  show screen xy  →  [Stage Created + In Global List + Shown]
                      ↑ Just config                         ↑ All happens here
```

## Code Location Reference

### Screen Definition
- **File**: `InterpreterScreen.java`
- **Method**: `visitScreenStatement()`
- **Line**: Creates and stores `ScreenConfig`

### Screen Show
- **File**: `InterpreterScreen.java`  
- **Method**: `visitScreenShowStatement()`
- **Calls**: `createStageForScreen()` if Stage doesn't exist
- **Result**: Stage created and added to `GLOBAL_SCREENS`

### Configuration Storage
- **File**: `InterpreterContext.java`
- **Map**: `screenConfigs` (Map<String, ScreenConfig>)
- **Methods**: `getScreenConfig()`, `setScreenConfig()`, `hasScreenConfig()`

### Stage Storage
- **File**: `InterpreterContext.java`
- **Map**: `GLOBAL_SCREENS` (ConcurrentHashMap<String, Stage>)
- **Access**: `getScreens()` method

## Example Timeline

### Scenario: Define 3 screens, show 1

```
Time  | Action                    | GLOBAL_SCREENS | screenConfigs | Memory Usage
------|---------------------------|----------------|---------------|-------------
t0    | screen menu = {...}       | []             | [menu]        | Low
t1    | screen settings = {...}   | []             | [menu,        | Low
      |                           |                |  settings]    |
t2    | screen help = {...}       | []             | [menu,        | Low
      |                           |                |  settings,    |
      |                           |                |  help]        |
t3    | show screen menu          | [menu]         | [menu,        | Medium
      |                           |                |  settings,    | (1 Stage)
      |                           |                |  help]        |
```

**Key Insight**: Only 1 Stage created despite 3 screens defined!

### Before This Change:
```
t0    | screen menu = {...}       | [menu]         | N/A           | High
t1    | screen settings = {...}   | [menu,         | N/A           | High
      |                           |  settings]     |               | (2 Stages)
t2    | screen help = {...}       | [menu,         | N/A           | High
      |                           |  settings,     |               | (3 Stages)
      |                           |  help]         |               |
```

**Problem**: All 3 Stages created, even if never shown!

## Conclusion

The lazy initialization ensures that JavaFX resources (Stages) are only allocated when actually needed, while still maintaining all screen data and variables accessible through the configuration layer.
