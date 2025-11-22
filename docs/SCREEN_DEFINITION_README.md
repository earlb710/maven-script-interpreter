# ScreenDefinition Class

## Overview

The `ScreenDefinition` class provides a convenient way to define and create JavaFX Stage (window) instances with optional singleton pattern support.

## Features

- **Encapsulates screen properties**: screenName, title, width, height
- **Singleton pattern support**: Control whether to reuse the same Stage or create new instances
- **Counter-based titles**: When not in singleton mode, automatically appends counter to window titles
- **Builder-style setters**: Configure screen properties using setter methods
- **Thread-safe**: Uses AtomicInteger for counter management

## Usage

### Basic Singleton Mode (Default)

```java
ScreenDefinition def = new ScreenDefinition(
    "MainScreen",     // screenName
    "My Application", // title
    800,              // width
    600               // height
);

// All calls return the same Stage instance
Stage stage1 = def.createScreen();
Stage stage2 = def.createScreen();
// stage1 == stage2 (true)

stage1.show();
```

### Non-Singleton Mode

```java
ScreenDefinition def = new ScreenDefinition(
    "DocWindow",
    "Document",
    600,
    400,
    false  // singleton = false
);

// Each call creates a new Stage with counter in title
Stage doc1 = def.createScreen(); // title: "Document #1"
Stage doc2 = def.createScreen(); // title: "Document #2"
Stage doc3 = def.createScreen(); // title: "Document #3"

doc1.show();
doc2.show();
doc3.show();
```

### Using Setters

```java
ScreenDefinition def = new ScreenDefinition();
def.setScreenName("CustomScreen");
def.setTitle("Custom Window");
def.setWidth(500);
def.setHeight(300);
def.setSingleton(false);

Stage stage = def.createScreen();
stage.show();
```

## API Reference

### Constructors

- `ScreenDefinition()` - Default constructor with singleton=true
- `ScreenDefinition(String screenName, String title, double width, double height)` - Constructor with required fields
- `ScreenDefinition(String screenName, String title, double width, double height, boolean singleton)` - Full constructor

### Methods

#### createScreen()
Returns a JavaFX Stage based on the definition.
- **Singleton mode**: Returns the same Stage instance on every call
- **Non-singleton mode**: Creates a new Stage with counter appended to title

#### Setters
- `setScreenName(String screenName)`
- `setTitle(String title)`
- `setWidth(double width)`
- `setHeight(double height)`
- `setSingleton(boolean singleton)`

#### Getters
- `String getScreenName()`
- `String getTitle()`
- `double getWidth()`
- `double getHeight()`
- `boolean isSingleton()`
- `int getInstanceCount()` - Returns the number of instances created (for non-singleton mode)

## Demo

Run `ScreenDefinitionDemo` to see examples of both singleton and non-singleton modes:

```bash
cd ScriptInterpreter
mvn compile
mvn exec:java -Dexec.mainClass="com.eb.script.interpreter.screen.ScreenDefinitionDemo"
```

## Location

- **Package**: `com.eb.script.interpreter.screen`
- **Source**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenDefinition.java`
- **Demo**: `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenDefinitionDemo.java`

## Implementation Notes

- The singleton Stage is automatically cleared when the window is closed, allowing recreation
- Thread-safe counter uses `AtomicInteger`
- All Stage instances are created with a simple StackPane scene
- Follows existing code patterns in the screen package (similar to AreaDefinition)
