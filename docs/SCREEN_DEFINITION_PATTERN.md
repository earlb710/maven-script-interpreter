# ScreenDefinition Pattern

## Overview

The ScreenFactory has been refactored to use the ScreenDefinition pattern. Instead of directly returning Stage objects, ScreenFactory now creates ScreenDefinition objects that can be used to create Stage instances with singleton support.

## Pattern Flow

```
ScreenFactory.createScreenDefinition() 
    → Returns ScreenDefinition object
        → ScreenDefinition.createScreen() 
            → Returns Stage (singleton or new instance)
```

## Usage Example

### Before (Direct Stage Creation)
```java
Stage stage = ScreenFactory.createScreen(
    screenName, title, width, height, 
    areas, metadataProvider, screenVars, varTypes, 
    onClickHandler, context
);
```

### After (ScreenDefinition Pattern)
```java
// Create the definition
ScreenDefinition screenDef = ScreenFactory.createScreenDefinition(
    screenName, title, width, height,
    areas, metadataProvider, screenVars, varTypes,
    onClickHandler, context
);

// Create Stage from definition (supports singleton)
Stage stage = screenDef.createScreen();
```

## Benefits

1. **Separation of Concerns**: ScreenDefinition defines WHAT to create, ScreenFactory defines HOW to create it
2. **Singleton Support**: Can reuse ScreenDefinition to get same Stage or create multiple instances
3. **Flexibility**: Can modify definition properties before creating Stage
4. **Backward Compatibility**: Old `createScreen()` methods still work

## API Methods

### ScreenFactory.createScreenDefinition()

Multiple overloaded methods available:

```java
// Basic
ScreenDefinition createScreenDefinition(
    String screenName, String title, double width, double height,
    List<AreaDefinition> areas,
    BiFunction<String, String, DisplayItem> metadataProvider
)

// With variables
ScreenDefinition createScreenDefinition(
    String screenName, String title, double width, double height,
    List<AreaDefinition> areas,
    BiFunction<String, String, DisplayItem> metadataProvider,
    ConcurrentHashMap<String, Object> screenVars
)

// With onClick handlers
ScreenDefinition createScreenDefinition(
    String screenName, String title, double width, double height,
    List<AreaDefinition> areas,
    BiFunction<String, String, DisplayItem> metadataProvider,
    ConcurrentHashMap<String, Object> screenVars,
    ConcurrentHashMap<String, DataType> varTypes,
    OnClickHandler onClickHandler
)

// Full (with context)
ScreenDefinition createScreenDefinition(
    String screenName, String title, double width, double height,
    List<AreaDefinition> areas,
    BiFunction<String, String, DisplayItem> metadataProvider,
    ConcurrentHashMap<String, Object> screenVars,
    ConcurrentHashMap<String, DataType> varTypes,
    OnClickHandler onClickHandler,
    InterpreterContext context
)
```

### ScreenDefinition Methods

```java
// Create Stage (singleton or multi-instance based on flag)
Stage createScreen()

// Setters
void setAreas(List<AreaDefinition> areas)
void setMetadataProvider(BiFunction<String, String, DisplayItem> metadataProvider)
void setScreenVars(ConcurrentHashMap<String, Object> screenVars)
void setVarTypes(ConcurrentHashMap<String, DataType> varTypes)
void setOnClickHandler(ScreenFactory.OnClickHandler onClickHandler)
void setContext(InterpreterContext context)
void setSingleton(boolean singleton)

// Getters for all fields
```

## Singleton Behavior

```java
// Singleton mode (default)
ScreenDefinition def = ScreenFactory.createScreenDefinition(...);
Stage s1 = def.createScreen();
Stage s2 = def.createScreen();  // s1 == s2 (same instance)

// Multi-instance mode
def.setSingleton(false);
Stage s3 = def.createScreen();  // New instance with title "Title #1"
Stage s4 = def.createScreen();  // New instance with title "Title #2"
```

## Implementation Details

- `ScreenDefinition` stores all screen creation parameters
- `ScreenDefinition.createScreen()` delegates to `ScreenFactory.createScreen()` for complex screens with areas
- Simple screens (no areas) are created directly by ScreenDefinition
- Singleton Stage is nullified when window closes, allowing recreation

## Migration Guide

Existing code using `ScreenFactory.createScreen()` continues to work without changes. To adopt the new pattern:

1. Replace `ScreenFactory.createScreen()` with `ScreenFactory.createScreenDefinition()`
2. Store the returned `ScreenDefinition`
3. Call `screenDef.createScreen()` to get the Stage
4. Optionally configure singleton behavior with `setSingleton()`

