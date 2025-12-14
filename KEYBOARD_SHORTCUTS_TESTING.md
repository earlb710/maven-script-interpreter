# Testing Guide: Global Alt+ Keyboard Shortcuts

## Feature Overview

This implementation adds global Alt+ keyboard shortcuts to access menus from anywhere in the console application, and Alt+E shortcut for screens to quickly focus the first editable field.

## Console Application Shortcuts

The following keyboard shortcuts are now available anywhere in the console application:

| Shortcut | Action |
|----------|--------|
| **Alt+F** | Opens the File menu |
| **Alt+E** | Opens the Edit menu |
| **Alt+C** | Opens the Config menu |
| **Alt+T** | Opens the Tools menu |
| **Alt+S** | Opens the Screens menu |
| **Alt+H** | Opens the Help menu |

## Screen Shortcuts

When a screen is open (created via EBS script), the following shortcut is available:

| Shortcut | Action |
|----------|--------|
| **Alt+E** | Focuses the first editable field (TextField, TextArea, ComboBox, etc.) |

## Testing Instructions

### 1. Build the Application
```bash
cd ScriptInterpreter
mvn clean compile
```

### 2. Run the Application
```bash
mvn javafx:run
```

### 3. Test Console Shortcuts

1. **Test Alt+F (File Menu)**:
   - Press Alt+F from anywhere in the console
   - Verify that the File menu opens
   - Menu should display options like "New Script File", "Open file", etc.

2. **Test Alt+E (Edit Menu)**:
   - Press Alt+E from anywhere in the console
   - Verify that the Edit menu opens
   - Menu should display options like "Cut", "Copy", "Paste", "Undo", "Redo", etc.

3. **Test Alt+C (Config Menu)**:
   - Press Alt+C from anywhere in the console
   - Verify that the Config menu opens
   - Menu should display configuration options like "AI Chat Model Setup", "Database Config", etc.

4. **Test Alt+T (Tools Menu)**:
   - Press Alt+T from anywhere in the console
   - Verify that the Tools menu opens
   - Menu should display tools like "Regex", "Thread Viewer", etc.

5. **Test Alt+S (Screens Menu)**:
   - Press Alt+S from anywhere in the console
   - Verify that the Screens menu opens
   - Menu should list any open screens or show "(No screens created)"

6. **Test Alt+H (Help Menu)**:
   - Press Alt+H from anywhere in the console
   - Verify that the Help menu opens
   - Menu should display help options like "Syntax Help"

### 4. Test Screen Shortcuts

1. **Create a test screen** with editable fields:
   ```ebs
   screen("testScreen", "Test Screen", 400, 300, [
       area("main", "vbox", "fill", [
           display("field1", "textfield", "First Field", "", 200),
           display("field2", "textfield", "Second Field", "", 200),
           display("field3", "textarea", "Text Area", "", 200, 100)
       ])
   ])
   screen.show("testScreen")
   ```

2. **Test Alt+E on Screen**:
   - With the screen open, press Alt+E
   - Verify that the first editable field (field1) receives focus
   - The cursor should be in the first TextField

3. **Test from different locations**:
   - Click on a button or non-editable area in the screen
   - Press Alt+E again
   - Verify that focus moves to the first editable field again

## Expected Behavior

### Console Application
- Keyboard shortcuts should work from any focused control within the console application
- When a menu is opened via keyboard shortcut, it should display as if clicked with the mouse
- Multiple shortcuts can be used in sequence to navigate different menus

### Screens
- Alt+E should find the first editable control in the screen's bound controls list
- If no editable controls exist, nothing should happen (no error)
- The shortcut should work regardless of which control currently has focus

## Implementation Details

### Console (EbsApp.java)
- Uses Scene-level event filter to capture Alt+ key combinations globally
- Checks for Alt key without Control or Shift modifiers
- Calls `menu.show()` on the appropriate menu based on key code
- Event is consumed to prevent further propagation

### Screens (ScreenFactory.java)
- Uses Scene-level event filter in screen creation
- Alt+E handler searches for first focusable and editable control
- Supports: TextField, TextArea, ComboBox, ChoiceBox, DatePicker, ColorPicker, Spinner, ScriptArea
- Uses `Platform.runLater()` to ensure focus happens on JavaFX Application Thread

## Troubleshooting

### Shortcuts Not Working
- Ensure you're pressing Alt (not Alt Gr or Option on Mac)
- Check that no other application is intercepting the key combinations
- Verify that the console window has focus

### Menu Doesn't Open
- Check console output for any error messages
- Verify that the menu bar is properly initialized
- Ensure the menu index matches the menu order in EbsMenu.java

### Alt+E Doesn't Focus Field on Screen
- Verify the screen has editable controls
- Check that controls are added to the bound controls list
- Ensure screen is fully initialized before pressing Alt+E
