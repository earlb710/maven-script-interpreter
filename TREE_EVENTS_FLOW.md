# TreeView Expand/Collapse Events - Flow Diagram

## Event Flow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER INTERACTION                         │
│                                                                 │
│  User clicks tree node disclosure triangle (▶/▼)               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    JAVAFX EVENT SYSTEM                          │
│                                                                 │
│  TreeItem.expandedProperty fires change event                  │
│  (oldValue: boolean, newValue: boolean)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              EXPAND/COLLAPSE LISTENER (ScreenFactory)           │
│                                                                 │
│  1. Detect expand (newValue = true) or collapse (false)        │
│  2. Build tree item path: buildTreeItemPath(treeItem)          │
│     Example: "Root.src.main"                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EVENT CONTEXT SETUP                          │
│                                                                 │
│  Store path in temporary variable:                             │
│  • For expand:   varRef.expandedItem = "Root.src.main"        │
│  • For collapse: varRef.collapsedItem = "Root.src.main"       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EVENT TRACKING                               │
│                                                                 │
│  incrementEventCount(screenName, itemName, eventType)          │
│  • Tracks number of expand/collapse events                     │
│  • Available via sys.getEventCount()                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  EBS CODE EXECUTION                             │
│                                                                 │
│  onClickHandler.executeDirect(ebsCode)                         │
│  • Runs on JavaFX thread (prevents deadlocks)                  │
│  • Has access to all screen variables                          │
│  • Can access path via varRef.expandedItem/collapsedItem       │
└─────────────────────────────────────────────────────────────────┘
```

## Example Event Handler Execution

### When Node "Root.src" is Expanded:

```javascript
// TreeView definition
fileTree {
    displayItem {
        type = treeview
        onExpand = "
            call print('Expanded: ' + fileTree.expandedItem);
            call updateStatus(fileTree.expandedItem);
        "
    }
}
```

**Execution Flow:**

1. **User Action**: Clicks ▶ next to "src" folder
2. **JavaFX Event**: TreeItem("src").expandedProperty changes from false to true
3. **Path Building**: Walks up tree hierarchy → "Root.src"
4. **Variable Storage**: `fileTree.expandedItem = "Root.src"`
5. **Event Counter**: Increments expand counter for fileTree
6. **Code Execution**: Runs the onExpand EBS code:
   ```
   call print('Expanded: ' + fileTree.expandedItem);  // fileTree.expandedItem = "Root.src"
   call updateStatus(fileTree.expandedItem);
   ```

## Listener Attachment Strategy

### Initial Setup (Static Tree):

```
TreeView Created
    │
    ├─── Root (listener attached)
    │     │
    │     ├─── src (listener attached)
    │     │     │
    │     │     ├─── main.ebs (listener attached)
    │     │     └─── utils.ebs (listener attached)
    │     │
    │     └─── docs (listener attached)
    │           │
    │           ├─── README.md (listener attached)
    │           └─── GUIDE.md (listener attached)
```

All listeners are attached recursively during tree creation.

### Dynamic Tree (Runtime Addition):

```
TreeView with Children Listener
    │
    ├─── Root (listener + children listener attached)
    │     │
    │     ├─── [Children Listener Active]
    │     │
    │     └─── NEW: projects (automatically gets listener)
                   │
                   └─── [Children Listener Active]
                         │
                         └─── NEW: project1 (automatically gets listener)
```

The children collection listener ensures that dynamically added nodes get event listeners automatically.

## Path Building Algorithm

```
Function buildTreeItemPath(treeItem):
    pathParts = []
    current = treeItem
    
    while current is not null:
        if current.value is not null:
            pathParts.insert_at_beginning(current.value)
        current = current.parent
    
    return join(pathParts, ".")

Examples:
    TreeItem("main.ebs")
        └─ parent: TreeItem("src")
            └─ parent: TreeItem("Root")
                └─ parent: null
    
    Result: "Root.src.main.ebs"
```

## Integration with Debug Panel

```
┌────────────────────────────────────────────────────┐
│  Debug Panel - Event Handlers                      │
├────────────────────────────────────────────────────┤
│                                                    │
│  fileTree.onClick:     call selectFile();          │
│  fileTree.onChange:    call updateSelection();     │
│  fileTree.onExpand:    call loadChildren(); [3]    │  ← Shows event count
│  fileTree.onCollapse:  call saveState(); [2]       │
│                                                    │
└────────────────────────────────────────────────────┘
```

The debug panel shows all event handlers including the new onExpand and onCollapse handlers, with event execution counts displayed in brackets.

## Error Handling Flow

```
Event Handler Execution
    │
    ├─── Try Block
    │     │
    │     ├─── Set context variable (varRef.expandedItem)
    │     ├─── Increment event counter
    │     └─── Execute EBS code
    │
    └─── Catch Block (InterpreterError)
          │
          └─── Print error to System.err
               • "Error executing onExpand handler: ..."
               • Full stack trace printed
               • Execution continues (no crash)
```

All event handler errors are caught and logged, preventing them from crashing the application.

## Threading Model

```
┌──────────────────┐         ┌─────────────────────┐
│  JavaFX Thread   │         │  Interpreter Thread │
│                  │         │    (if separate)    │
├──────────────────┤         ├─────────────────────┤
│                  │         │                     │
│ User clicks node │         │                     │
│       │          │         │                     │
│       ▼          │         │                     │
│ Event fires      │         │                     │
│       │          │         │                     │
│       ▼          │         │                     │
│ Listener catches │         │                     │
│       │          │         │                     │
│       ▼          │         │                     │
│ executeDirect()  │────────▶│ Execute EBS code   │
│  (on JavaFX      │         │  (on JavaFX thread)│
│   thread)        │         │                     │
│                  │         │                     │
└──────────────────┘         └─────────────────────┘
```

Using `executeDirect()` ensures the EBS code runs on the JavaFX thread, preventing deadlocks when the code needs to show dialogs or update UI.

## Complete Data Flow Example

```
User Expands "docs" Node
         │
         ▼
┌────────────────────────┐
│ JavaFX Event           │
│ TreeItem("docs")       │
│ expanded = true        │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│ Build Path             │
│ Result: "Root.docs"    │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│ Store in Variable      │
│ fileTree.expandedItem  │
│ = "Root.docs"          │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│ Increment Counter      │
│ fileTree.onExpand += 1 │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│ Execute Handler Code   │
│ string path =          │
│   fileTree.expandedItem│
│ call loadDocs(path);   │
└────────────────────────┘
```

This flow ensures that event handlers have full context about which node was expanded and can perform appropriate actions.
