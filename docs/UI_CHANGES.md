# UI Changes: Edit Menu Enhancements

## Summary
Enhanced Edit menu with Copy/Cut/Paste/Undo/Redo/Close menu items with keyboard shortcuts for both the main application and screen windows.

## Changes in Main Application Window

### Edit Menu (EbsMenu.java)
The Edit menu includes comprehensive editing operations:

```
Edit
├── Cut (Ctrl+X)
├── Copy (Ctrl+C)
├── Paste (Ctrl+V)
├── ───────────────
├── Undo (Ctrl+Z)
├── Redo (Ctrl+Y)
├── ───────────────
├── Find (Ctrl+F)
├── Replace (Ctrl+H)
├── ───────────────
├── Show/Hide Line Numbers (Ctrl+L)
├── ───────────────
└── Close (Ctrl+W)
```

### Behavior
- **Cut/Copy/Paste**: Standard clipboard operations on the active editor tab
- **Undo/Redo**: Undo and redo text editing operations in the active editor tab
- **Tabs**: Pressing Ctrl+W or selecting Edit > Close will close the currently active tab
- **Console Tab**: The main Console tab is protected and will NOT close (it's marked as non-closable)
- **File Tabs**: All file tabs (EbsTab instances) can be closed using this menu item

## Changes in Screen Windows

### Screen Windows Menu Bar (ScreenFactory.java)
Screen windows created via EBS scripts now include a menu bar with a fully-featured Edit menu:

```
Edit
├── Cut (Ctrl+X)          ← NEW
├── Copy (Ctrl+C)         ← NEW
├── Paste (Ctrl+V)        ← NEW
├── ───────────────
├── Undo (Ctrl+Z)         ← NEW
├── Redo (Ctrl+Y)         ← NEW
├── ───────────────
└── Close (Ctrl+W)
```

### Behavior
- **Cut/Copy/Paste**: Work with the currently focused text control (TextField, TextArea, PasswordField) in the screen
- **Undo/Redo**: Work with the currently focused text control's undo/redo history
- **Close**: Pressing Ctrl+W or selecting Edit > Close will close the screen window
- **Focus-based**: All operations automatically apply to whichever text control has keyboard focus
- This works for all screen windows created via the `screen` statement in EBS

## Technical Implementation

1. **EbsMenu.java**: Contains full Edit menu implementation for main application
   ```java
   MenuItem cutItem = new MenuItem("Cut");
   cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
   // Similar for Copy, Paste, Undo, Redo...
   ```

2. **EbsHandler.java**: Added `closeTab(Tab tab)` method to delegate to TabHandler

3. **TabHandler.java**: Implemented `closeTab(Tab tab)` to remove tab from TabPane

4. **ScreenFactory.java**: 
   - Enhanced `createScreenMenuBar(Stage stage)` helper method
   - Modified screen creation to include menu bar at top of BorderPane
   - Menu bar includes Edit menu with Cut/Copy/Paste/Undo/Redo/Close operations
   - All text editing operations use `stage.getScene().getFocusOwner()` to get the currently focused control
   - Operations are type-checked and applied to TextField and TextArea controls

## Code Example for Screen Windows
```java
// Cut operation on focused text control
cutItem.setOnAction(e -> {
    javafx.scene.Node focusOwner = stage.getScene().getFocusOwner();
    if (focusOwner instanceof javafx.scene.control.TextField) {
        ((javafx.scene.control.TextField) focusOwner).cut();
    } else if (focusOwner instanceof javafx.scene.control.TextArea) {
        ((javafx.scene.control.TextArea) focusOwner).cut();
    }
});
```

## Safety Features
- Console tab cannot be closed (protected by `tab.isClosable()` check)
- Only closable tabs respond to the Close command
- Screen windows close cleanly with proper cleanup via Stage.close()
- Edit operations (Cut/Copy/Paste/Undo/Redo) only work when a text control has focus
- No-op if focus is on non-text controls (buttons, labels, etc.)

