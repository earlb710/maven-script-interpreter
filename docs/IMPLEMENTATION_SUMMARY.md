# Implementation Summary: Add Close Ctrl+W Menu Item

## Requirement
Add edit menu item "Close ctrl+W" to close tabs but not main console window; add to screen windows as well.

## Solution Implemented

### 1. Main Application Window - Edit Menu (EbsMenu.java)
Added a new "Close" menu item to the Edit menu with the following features:
- Keyboard shortcut: **Ctrl+W**
- Location: Bottom of Edit menu, after "Show/Hide Line Numbers"
- Behavior: Closes the currently selected tab if it's closable

**Code snippet:**
```java
// Close Tab
MenuItem closeTabItem = new MenuItem("Close");
closeTabItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
closeTabItem.setOnAction(e -> {
    Tab tab = handler.getSelectedTab();
    // Only close if it's not the console tab (which is not closable)
    if (tab != null && tab.isClosable()) {
        handler.closeTab(tab);
    }
});
```

### 2. Handler Methods (EbsHandler.java & TabHandler.java)
Added supporting methods to enable tab closing:

**EbsHandler.java:**
```java
public void closeTab(Tab tab) {
    if (tabHandler != null) {
        tabHandler.closeTab(tab);
    }
}
```

**TabHandler.java:**
```java
public void closeTab(Tab tab) {
    if (tab != null && tabPane.getTabs().contains(tab)) {
        tabPane.getTabs().remove(tab);
    }
}
```

### 3. Screen Windows Menu Bar (ScreenFactory.java)
Added menu bar support to screen windows created via EBS scripts:

**New method:**
```java
private static javafx.scene.control.MenuBar createScreenMenuBar(Stage stage) {
    javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
    
    // Create Edit menu
    javafx.scene.control.Menu editMenu = new javafx.scene.control.Menu("Edit");
    
    // Close menu item with Ctrl+W
    javafx.scene.control.MenuItem closeItem = new javafx.scene.control.MenuItem("Close");
    closeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
        javafx.scene.input.KeyCode.W, 
        javafx.scene.input.KeyCombination.CONTROL_DOWN));
    closeItem.setOnAction(e -> {
        // Close the screen window
        stage.close();
    });
    
    editMenu.getItems().add(closeItem);
    menuBar.getMenus().add(editMenu);
    
    return menuBar;
}
```

**Integration into createScreen():**
```java
// Create menu bar for the screen
javafx.scene.control.MenuBar menuBar = createScreenMenuBar(stage);

// Wrap in BorderPane to add menu bar at top and status bar at bottom
BorderPane screenRoot = new BorderPane();
screenRoot.setTop(menuBar);      // ← Menu bar added here
screenRoot.setCenter(scrollPane);
screenRoot.setBottom(statusBar);
```

## Safety Features

### Console Tab Protection
The console tab cannot be closed because:
1. In `Console.java` (line 146): `t.setClosable(false);`
2. The menu action checks: `if (tab != null && tab.isClosable())`

This ensures the main console window remains open at all times.

### Screen Window Cleanup
Screen windows close properly via `stage.close()` which:
- Triggers the existing `setOnCloseRequest` handler
- Interrupts the screen thread
- Cleans up resources via `context.remove(screenName)`

## Files Modified
1. `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsMenu.java` - Added Close menu item
2. `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsHandler.java` - Added closeTab method
3. `ScriptInterpreter/src/main/java/com/eb/ui/tabs/TabHandler.java` - Implemented tab removal
4. `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenFactory.java` - Added menu bar to screens

## Testing Results
✅ Build: `mvn clean compile` - **SUCCESS**
✅ No compilation errors
✅ No security vulnerabilities (UI-only changes)
✅ Follows existing code patterns and conventions

## User Experience

### Main Window
Users can now:
- Press **Ctrl+W** to close the active tab (except console)
- Click **Edit > Close** from the menu bar
- Console tab remains protected and unclosable

### Screen Windows
Users can now:
- Press **Ctrl+W** to close a screen window
- Click **Edit > Close** from the screen's menu bar
- Windows close cleanly with proper resource cleanup

## Compatibility
- Java 21 compatible
- JavaFX 21 compatible
- No breaking changes to existing functionality
- Backward compatible with existing EBS scripts
