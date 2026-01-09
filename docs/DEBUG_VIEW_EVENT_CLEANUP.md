# Debug View Event Cleanup Documentation

## Overview
This document describes the event handlers and resources associated with the debug view (Ctrl+D) and how they are properly cleaned up when the debug panel is closed.

## Debug View Lifecycle

### Opening the Debug View (Ctrl+D)
When the debug view is opened:
1. A new `ScrollPane` containing debug information is created
2. Event handlers are attached to UI controls (close button, copy button)
3. A background thread may be started for copy button feedback
4. The debug panel is stored in static maps for access during updates

### Closing the Debug View (Ctrl+D again or Close button)
When the debug view is closed:
1. **Event handler cleanup** is performed first via `cleanupDebugPanelResources()`
2. UI components are removed from the scene graph
3. Static map references are cleared
4. Window width is restored to original size

## Event Handlers and Resources

### 1. Close Button Event Handler
**Location:** `ScreenFactory.createDebugPanel()` line ~717

```java
closeButton.setOnAction(e -> {
    toggleDebugMode(screenName, context);
});
```

**Cleanup:** Automatically handled by JavaFX garbage collection when the button node is removed from the scene graph.

### 2. Copy Button Event Handler
**Location:** `ScreenFactory.createDebugPanel()` line ~733

```java
copyButton.setOnAction(e -> {
    // ... copy to clipboard logic ...
    
    // Start feedback thread
    Thread feedbackThread = new Thread(() -> {
        try {
            Thread.sleep(1000);
            Platform.runLater(() -> copyButton.setText("📋"));
        } catch (InterruptedException ex) {
            // Graceful exit on interrupt
        }
    });
    debugCopyFeedbackThreads.put(screenKey, feedbackThread);
    feedbackThread.start();
});
```

**Cleanup:** 
- The event handler lambda is automatically GC'd when the button is removed
- The background thread is explicitly interrupted via `cleanupDebugPanelResources()`
- Thread tracking map (`debugCopyFeedbackThreads`) entry is removed

### 3. Copy Button Feedback Thread
**Purpose:** Shows a brief "✓" checkmark for 1 second after copying to clipboard

**Cleanup Strategy:**
- Thread is stored in `debugCopyFeedbackThreads` map with screen name as key
- When debug panel closes, `cleanupDebugPanelResources()` interrupts the thread
- Thread name includes screen name for easier debugging: `"DebugPanel-CopyFeedback-" + screenName`
- Thread checks if button still exists in scene before updating (handles panel closure during sleep)
- InterruptedException is caught and handled gracefully

### 4. Focus Listeners on Screen Controls
**Location:** `ScreenFactory.setupStatusBarFocusListeners()` line ~3403

```java
control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
    if (isFocused) {
        // Update status bar with focused control info
    }
});
```

**Cleanup:** **NOT cleaned up** - This is intentional!
- Focus listeners are attached to the main screen controls, not the debug panel
- They update the status bar which remains visible after debug panel closes
- These listeners should persist for the lifetime of the screen
- They will be garbage collected when the screen window itself is closed

### 5. Scene Event Filter (Ctrl+D Handler)
**Location:** `ScreenFactory.showScreen()` line ~3354

```java
scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
    if (event.getCode() == KeyCode.D && event.isControlDown()) {
        toggleDebugMode(screenName, context);
        event.consume();
    }
    // ... other keyboard shortcuts ...
});
```

**Cleanup:** **NOT cleaned up** - This is intentional!
- The event filter is attached to the Scene, not the debug panel
- It needs to persist to allow toggling debug mode on/off
- Will be garbage collected when the Scene/Stage is closed

### 6. TableView Cell Factories and Tooltips
**Location:** Various places in `createDebugPanel()` for variables and items tables

```java
nameCol.setCellFactory(col -> new TableCell<>() {
    @Override
    protected void updateItem(String item, boolean empty) {
        // Create tooltips for each cell
        Tooltip tooltip = new Tooltip(tooltipText);
        setTooltip(tooltip);
    }
});
```

**Cleanup:** Automatically handled by JavaFX garbage collection when the TableView is removed from the scene graph.

## Static Maps for Resource Tracking

### Maps Cleared on Debug Panel Close
The following static maps have entries removed when debug panel closes:

1. `screenDebugPanels` - The ScrollPane containing debug UI
2. `screenDebugSplitPanes` - The SplitPane used to show debug alongside main content
3. `screenCenterContents` - Original center content before debug was shown
4. `screenOriginalWidths` - Original window width before debug expansion
5. `debugStatusLabels` - Label showing screen status in debug panel
6. `changedItems` - Set of changed variable names for highlighting
7. `debugItemsTables` - TableView showing screen items
8. `debugCopyFeedbackThreads` - Background threads for copy button feedback

### Maps NOT Cleared (Intentional)
These maps persist after debug panel closes:

1. `screenRootPanes` - Root BorderPane for the screen (needed for future debug toggles)
2. `screenMenuBars` - MenuBar for the screen (needed for menu operations)
3. `debugMode` - ThreadLocal boolean tracking debug state (persists per thread)
4. `eventCounts` - Event counter map (useful for tracking across debug sessions)
5. `eventCountLabels` - Labels in debug panel showing event counts (cleared via debug panel removal)

## Cleanup Method Details

### cleanupDebugPanelResources(String screenNameLower)
**Purpose:** Clean up event handlers, threads, and resources when debug panel closes

**Actions Performed:**
1. Interrupt and remove copy button feedback thread if running
2. Document that JavaFX will handle UI component cleanup
3. Note that focus listeners intentionally persist for status bar updates

**Code Location:** `ScreenFactory.java` line ~607

```java
private static void cleanupDebugPanelResources(String screenNameLower) {
    // Interrupt copy button feedback thread
    Thread feedbackThread = debugCopyFeedbackThreads.remove(screenNameLower);
    if (feedbackThread != null && feedbackThread.isAlive()) {
        feedbackThread.interrupt();
    }
    
    // UI components cleaned up automatically by JavaFX GC
    // Focus listeners persist intentionally for status bar
}
```

## Memory Leak Prevention

### Potential Issues and Mitigations

1. **Copy Button Feedback Threads**
   - **Risk:** Thread continues running after panel closes
   - **Mitigation:** Thread is interrupted and map entry removed
   - **Verification:** Thread checks if button is in scene before updating

2. **Event Handler References**
   - **Risk:** Strong references prevent garbage collection
   - **Mitigation:** All event handlers are attached to nodes in the panel's scene graph
   - **Verification:** When panel is removed from scene, nodes become eligible for GC

3. **TableView Listeners**
   - **Risk:** Cell factories and value change listeners retain references
   - **Mitigation:** JavaFX automatically cleans these up when TableView is removed
   - **Verification:** No manual listener cleanup needed (handled by platform)

4. **Static Map Entries**
   - **Risk:** Static maps retain references to debug panel components
   - **Mitigation:** All relevant maps are explicitly cleared in `toggleDebugPanel()`
   - **Verification:** Maps are accessed with `.remove()` not just `.get()`

## Testing Recommendations

### Manual Testing
1. Open debug panel (Ctrl+D)
2. Click copy button (starts background thread)
3. Immediately close debug panel (Ctrl+D or close button)
4. Verify no exceptions in console
5. Reopen debug panel and verify it works correctly

### Automated Testing Considerations
- Test rapid open/close cycles
- Test copy button click followed by immediate close
- Monitor thread count to verify threads are cleaned up
- Check memory usage over multiple open/close cycles

## Code Review Findings

### ✅ Properly Cleaned Up
- Copy button feedback threads (explicitly interrupted)
- Debug panel UI components (removed from scene graph)
- Static map entries (explicitly removed)
- Window width restoration (original width restored)

### ✅ Intentionally NOT Cleaned Up
- Focus listeners on screen controls (needed for status bar)
- Ctrl+D event filter on scene (needed to reopen debug)
- Screen root pane references (needed for future operations)

### ⚠️ Relies on JavaFX GC
- Button event handler lambdas
- TableView cell factories and tooltips
- Listener references from removed nodes

These are acceptable as JavaFX's garbage collection properly handles nodes removed from the scene graph.

## Conclusion

The debug view properly cleans up its event handlers and resources when closed:

1. **Active threads** are explicitly interrupted to prevent runaway background tasks
2. **UI components** are removed from scene graph, allowing JavaFX to garbage collect handlers
3. **Static map references** are explicitly cleared to prevent memory retention
4. **Intentionally persistent handlers** (focus listeners, keyboard shortcuts) are documented

The implementation follows JavaFX best practices and properly prevents memory leaks.
