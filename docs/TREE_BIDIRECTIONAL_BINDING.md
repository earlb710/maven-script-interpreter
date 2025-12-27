# TreeView Bidirectional VarRef Binding

## Overview

As of this update, TreeView controls now support **bidirectional binding** with their varRef variables. This means:

1. **Forward Binding** (Tree → Variable): When a user selects a tree item, the bound variable is updated with the item's value
2. **Reverse Binding** (Variable → Tree): When the variable is set programmatically (e.g., via button click or script logic), the tree selection automatically updates to match

## Problem Solved

Previously, TreeView varRef binding was **one-way only**:
- ✅ Selecting a tree item updated the variable
- ❌ Setting the variable did NOT update the tree selection

Now it's **bidirectional**:
- ✅ Selecting a tree item updates the variable
- ✅ Setting the variable updates the tree selection

## Usage

### Basic Example

```javascript
screen FileExplorer {
    areas {
        mainArea {
            items {
                // TreeView with varRef binding
                fileTree {
                    varRef = selectedFile
                    displayItem {
                        type = treeview
                        showRoot = true
                        treeItems = [
                            {
                                value = "Root",
                                expanded = true,
                                children = [
                                    { value = "Documents" },
                                    { value = "Projects" },
                                    { value = "Images" }
                                ]
                            }
                        ]
                    }
                }
                
                // Button to programmatically select a tree item
                selectDocsBtn {
                    displayItem {
                        type = button
                        labelText = "Select Documents"
                        onClick = "
                            // Setting the variable will update the tree selection
                            selectedFile = 'Documents';
                        "
                    }
                }
            }
        }
    }
}
```

### How It Works

When you set the variable:
```javascript
selectedFile = 'Documents';
```

The tree automatically:
1. **Searches** for the tree item with value "Documents"
2. **Expands** all parent nodes to make it visible
3. **Selects** the item
4. **Scrolls** to make the selected item visible in the viewport

### Clearing Selection

To clear the tree selection, set the variable to an empty string:
```javascript
selectedFile = '';
```

## Features

### Automatic Parent Expansion

When you select a nested item programmatically, all parent nodes are automatically expanded:

```javascript
// Select a deeply nested item
selectedFile = 'README.md';  // Inside Documents folder
// The tree will expand "Root" and "Documents" to show "README.md"
```

### Automatic Scrolling

The tree automatically scrolls to make the selected item visible:

```javascript
selectedFile = 'LastItem';  // At the bottom of a long tree
// The tree scrolls down to show the selected item
```

### Loop Prevention

The implementation prevents infinite loops:
- Tree selection → variable update → tree selection → ...
- This is handled internally with update flags, no special code needed

## Complete Example

See `test_tree_bidirectional_binding.ebs` for a complete working example that demonstrates:
- Tree selection updating the variable
- Buttons setting the variable to update tree selection
- Clearing selection
- Nested item selection with automatic expansion

## Technical Details

### Implementation

The bidirectional binding is implemented in two key components:

**ControlUpdater.java**:
- Added `updateTreeView()` method
- Searches tree hierarchy for matching items
- Expands parents and scrolls to selection
- Prevents infinite loops with update flags

**ControlListenerFactory.java**:
- Modified TreeView binding setup
- Added `setupTreeViewBinding()` method
- Coordinates forward and reverse binding
- Uses the existing `refreshBoundControls` mechanism

### When Updates Occur

The tree selection is updated when:
1. Variable is set in an onClick handler (button clicks)
2. Variable is set in an onChange handler (other control changes)
3. Variable is set in any event handler that triggers `refreshBoundControls`

## Limitations

- The tree searches for items by **exact value match** (case-sensitive)
- Only the first matching item is selected if multiple items have the same value
- The tree must have a valid root item for selection to work
- Dynamic tree items added after initial creation are supported

## Related Features

- **TreeView Events**: See [TREEVIEW_EVENTS.md](TREEVIEW_EVENTS.md) for onExpand and onCollapse events
- **TreeView Icons**: See [TREEVIEW_ICONS.md](TREEVIEW_ICONS.md) for icon customization
- **TreeView Styling**: See [TREEVIEW_STYLING.md](TREEVIEW_STYLING.md) for text styling

## Version History

- **v1.1** (2025-12-26): Added bidirectional varRef binding for TreeView controls
- **v1.0** (2025-12-14): Initial TreeView implementation with one-way varRef binding
