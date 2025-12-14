# TreeView Expand and Collapse Events

## Overview

The EBS language now supports `onExpand` and `onCollapse` event handlers for TreeView components. These events are triggered when tree nodes are expanded or collapsed, allowing you to execute custom EBS code in response to user interactions with the tree structure.

## Event Handlers

### `onExpand` Event

The `onExpand` event is triggered when a tree node is expanded (opened to show its children).

**Syntax:**
```javascript
treeView {
    displayItem {
        type = treeview
        onExpand = "
            // Your EBS code here
            // Access the expanded node path via: varName.expandedItem
        "
    }
}
```

### `onCollapse` Event

The `onCollapse` event is triggered when a tree node is collapsed (closed to hide its children).

**Syntax:**
```javascript
treeView {
    displayItem {
        type = treeview
        onCollapse = "
            // Your EBS code here
            // Access the collapsed node path via: varName.collapsedItem
        "
    }
}
```

## Event Context

When an expand or collapse event is triggered, the system automatically provides context information through temporary variables:

- **`varName.expandedItem`**: Contains the dot-notation path of the node that was expanded
- **`varName.collapsedItem`**: Contains the dot-notation path of the node that was collapsed

The path follows dot notation format: `"Root.folder1.subfolder.item"`

## Complete Example

```javascript
screen fileExplorer {
    title = "File Explorer with Events"
    width = 800
    height = 600
    
    areas {
        mainArea {
            type = vbox
            items {
                // Status label to show event information
                statusLabel {
                    displayItem {
                        type = label
                        labelText = "Expand or collapse folders to see events"
                        itemFontSize = "14px"
                    }
                }
                
                // TreeView with expand/collapse events
                fileTree {
                    displayItem {
                        type = treeview
                        displayRecords = 15
                        showRoot = true
                        treeItems = [
                            {
                                value = "Root",
                                expanded = true,
                                children = [
                                    {
                                        value = "src",
                                        expanded = false,
                                        children = [
                                            { value = "main.ebs" },
                                            { value = "utils.ebs" }
                                        ]
                                    },
                                    {
                                        value = "docs",
                                        expanded = false,
                                        children = [
                                            { value = "README.md" },
                                            { value = "GUIDE.md" }
                                        ]
                                    }
                                ]
                            }
                        ]
                        
                        // Event handler for node expansion
                        onExpand = "
                            string expandedPath = fileTree.expandedItem;
                            call print('Expanded: ' + expandedPath);
                            call scr.setProperty('fileExplorer', 'statusLabel', 'labelText', 
                                'EXPANDED: ' + expandedPath);
                        "
                        
                        // Event handler for node collapse
                        onCollapse = "
                            string collapsedPath = fileTree.collapsedItem;
                            call print('Collapsed: ' + collapsedPath);
                            call scr.setProperty('fileExplorer', 'statusLabel', 'labelText', 
                                'COLLAPSED: ' + collapsedPath);
                        "
                    }
                }
                
                // Button to show event statistics
                statsButton {
                    displayItem {
                        type = button
                        labelText = "Show Event Statistics"
                        onClick = "
                            int expandCount = sys.getEventCount('fileExplorer', 'fileTree', 'onExpand');
                            int collapseCount = sys.getEventCount('fileExplorer', 'fileTree', 'onCollapse');
                            call print('Total Expand: ' + expandCount + ', Total Collapse: ' + collapseCount);
                        "
                    }
                }
            }
        }
    }
}

show screen fileExplorer;
```

## Use Cases

### 1. Lazy Loading of Tree Content

Load child nodes only when a parent is expanded:

```javascript
onExpand = "
    string path = fileTree.expandedItem;
    if (path == 'Root.Documents') {
        // Load documents folder contents dynamically
        call loadDocumentsFolder();
    }
"
```

### 2. Tracking User Navigation

Log which folders users are exploring:

```javascript
onExpand = "
    string path = fileTree.expandedItem;
    call log.info('User opened: ' + path);
    call analytics.trackEvent('folder_expanded', path);
"
```

### 3. Conditional Actions

Perform different actions based on which node was expanded:

```javascript
onExpand = "
    string path = fileTree.expandedItem;
    if (string.contains(path, 'admin')) {
        call security.checkAdminAccess();
    }
    if (string.contains(path, 'data')) {
        call loadDataPreview(path);
    }
"
```

### 4. State Management

Update application state when nodes are expanded or collapsed:

```javascript
onExpand = "
    string path = fileTree.expandedItem;
    expandedNodes[expandedNodes.length] = path;
    call saveState();
"

onCollapse = "
    string path = fileTree.collapsedItem;
    call array.remove(expandedNodes, path);
    call saveState();
"
```

## Event Tracking

The system automatically tracks expand and collapse events. You can retrieve event counts using:

```javascript
int expandCount = call sys.getEventCount(screenName, itemName, "onExpand");
int collapseCount = call sys.getEventCount(screenName, itemName, "onCollapse");
```

## Debug Panel Integration

When debug mode is enabled, expand and collapse event handlers are displayed in the debug panel along with other event handlers (onClick, onChange, onValidate).

The debug panel shows:
- Event handler code (truncated for display)
- Event execution count
- Full code in tooltip on hover

## Dynamic Tree Items

The expand/collapse event handlers work seamlessly with dynamically added tree items. When new nodes are added to the tree at runtime, the event listeners are automatically attached to them.

## Best Practices

1. **Keep handlers lightweight**: Expand/collapse events can fire frequently. Avoid heavy computations in the event handlers.

2. **Use the path information**: The `expandedItem` and `collapsedItem` variables provide context about which node was interacted with.

3. **Combine with other events**: Use expand/collapse events together with selection events (`onChange`) for comprehensive tree interaction handling.

4. **Error handling**: Wrap potentially failing operations in try-catch blocks within the event handler code.

5. **State consistency**: If tracking expanded state manually, ensure both expand and collapse handlers maintain consistency.

## Limitations

- Events are only triggered by user interaction with the tree nodes
- Programmatic expansion/collapse (e.g., via `expandAll`) does not trigger events
- The root node expansion state is managed by the `showRoot` property and does not trigger events

## Related Features

- **TreeView Icons**: See [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) for icon customization
- **TreeView Styling**: See [TREEVIEW_STYLING.md](TREEVIEW_STYLING.md) for text styling
- **Screen Events**: See general event handling documentation for onClick, onChange, onValidate events

## Technical Details

### Event Flow

1. User clicks on a tree node disclosure triangle to expand or collapse it
2. JavaFX fires the `expandedProperty` change event for that TreeItem
3. The event listener captures the change and determines if it's an expand or collapse
4. The tree item path is built using dot notation by walking up the tree hierarchy
5. The path is stored in a temporary variable (`varName.expandedItem` or `varName.collapsedItem`)
6. The event counter is incremented
7. The EBS event handler code is executed on the JavaFX thread using `executeDirect`

### Recursive Listener Attachment

The implementation recursively attaches listeners to all tree items and their children when the TreeView is created. It also listens for new children being added dynamically and automatically attaches listeners to them as well.

### Path Building

Tree item paths are built by walking up from the target node to the root, collecting the value of each node, and joining them with dots. For example:
- Root → "Root"
- Root.src → "Root.src"
- Root.src.main.ebs → "Root.src.main.ebs"

## Troubleshooting

**Events not firing:**
- Verify that the TreeView has a valid root node
- Check that the onExpand/onCollapse properties are set correctly
- Ensure the tree items have children to expand/collapse

**Path is empty or incorrect:**
- Ensure all tree items have non-null values
- Check that the tree structure is properly defined

**Handler code errors:**
- Check the console output for error messages
- Verify that all referenced variables exist in scope
- Use the debug panel to inspect the handler code

## Version History

- **v1.0** (2025-12-14): Initial implementation of onExpand and onCollapse events for TreeView
