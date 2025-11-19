# UI Changes: Close Tab Menu Item

## Summary
Added "Close" menu item with Ctrl+W keyboard shortcut to close tabs and screen windows.

## Changes in Main Application Window

### Edit Menu (EbsMenu.java)
The Edit menu now includes a new "Close" menu item at the bottom:

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
└── Close (Ctrl+W)          ← NEW
```

### Behavior
- **Tabs**: Pressing Ctrl+W or selecting Edit > Close will close the currently active tab
- **Console Tab**: The main Console tab is protected and will NOT close (it's marked as non-closable)
- **File Tabs**: All file tabs (EbsTab instances) can be closed using this menu item

## Changes in Screen Windows

### Screen Windows Menu Bar (ScreenFactory.java)
Screen windows created via EBS scripts now include a menu bar with an Edit menu:

```
Edit
└── Close (Ctrl+W)
```

### Behavior
- Pressing Ctrl+W or selecting Edit > Close will close the screen window
- This works for all screen windows created via the `screen` statement in EBS

## Technical Implementation

1. **EbsMenu.java**: Added Close menu item with keyboard accelerator
   ```java
   MenuItem closeTabItem = new MenuItem("Close");
   closeTabItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
   ```

2. **EbsHandler.java**: Added `closeTab(Tab tab)` method to delegate to TabHandler

3. **TabHandler.java**: Implemented `closeTab(Tab tab)` to remove tab from TabPane

4. **ScreenFactory.java**: 
   - Added `createScreenMenuBar(Stage stage)` helper method
   - Modified screen creation to include menu bar at top of BorderPane
   - Menu bar includes Edit > Close with Ctrl+W accelerator

## Safety Features
- Console tab cannot be closed (protected by `tab.isClosable()` check)
- Only closable tabs respond to the Close command
- Screen windows close cleanly with proper cleanup via Stage.close()
