# Debug View JavaFX Tooltip Enhancement

## Overview
This enhancement adds detailed JavaFX component information to the debug panel tooltips for screen items that are backed by JavaFX components.

## Feature Description

When you open the debug panel (Ctrl+D) on a screen and hover over items in the "Screen Items" section, the tooltip will now show additional JavaFX component details if the item is backed by a JavaFX component.

## What Information Is Displayed

The tooltip now includes a "JavaFX:" section that shows:

- **Type**: The JavaFX class name (e.g., TextField, Button, CheckBox)
- **Component Type**: The EBS screen component type (e.g., Screen.TextField)
- **Width/Height**: The component's layout bounds dimensions
- **X/Y Position**: The component's layout position
- **Style**: Any inline CSS style applied to the component
- **Style Classes**: CSS classes applied to the component
- **Visible**: Whether the component is visible
- **Managed**: Whether the component is managed by its parent layout
- **Disabled**: Whether the component is disabled
- **ID**: The component's ID (if set)

## Example Tooltip Content

Before this enhancement, a tooltip might show:
```
Item: username
Type: textfield
Var: username
Area: mainArea
State: CLEAN
---
Label: Username:
Prompt: Enter your username
```

After this enhancement, if the item is backed by a JavaFX component, it shows:
```
Item: username
Type: textfield
Var: username
Area: mainArea
State: CLEAN
---
Label: Username:
Prompt: Enter your username
JavaFX:
JavaFX Component Description:
  Type: TextField
  Component Type: Screen.TextField
  Width: 200.00
  Height: 31.00
  X: 10.00
  Y: 5.00
  Style: -fx-padding: 5 10 5 10;
  Style Classes: text-input, text-field, screen-item-textfield
  Visible: true
  Managed: true
  Disabled: false
```

## Implementation Details

### Files Changed
- `ScriptInterpreter/src/main/java/com/eb/script/interpreter/screen/ScreenFactory.java`

### Key Methods
1. **`buildDisplayItemInfo()`** - Enhanced to check for and include JavaFX component information
   - Accepts additional parameters: `context`, `screenName`, `varRef`
   - Looks up the `ScreenComponentType` from the interpreter context
   - Calls `getJavaFXDescription()` if a JavaFX node is present
   - Appends the description with "JavaFX:" label

2. **`ScreenComponentType.getJavaFXDescription()`** - Existing method that formats JavaFX component details
   - Already provides comprehensive component information
   - No changes needed - reused existing functionality

### Data Flow
1. User opens debug panel (Ctrl+D) on a screen
2. Debug panel retrieves all screen items from `InterpreterContext`
3. For each item, `buildDisplayItemInfo()` is called with context information
4. Method checks if item has a `ScreenComponentType` with a JavaFX node
5. If found, appends the JavaFX description to the tooltip content
6. Tooltip displays the enhanced information when user hovers over item

## Benefits

1. **Better Debugging**: Developers can see the actual JavaFX component state without inspecting code
2. **Layout Verification**: Easily verify component sizes and positions
3. **Style Inspection**: See applied CSS styles and classes
4. **State Visibility**: Check visibility, disabled, and managed states
5. **Non-Intrusive**: Only shows for items with JavaFX backing; others remain unchanged

## Usage

1. Open a screen with JavaFX-backed items
2. Press `Ctrl+D` to toggle the debug panel
3. In the "Screen Items" section (bottom half), hover over any item name
4. The tooltip will show JavaFX information if the item is backed by a component

## Technical Notes

- The enhancement is purely additive - no breaking changes
- Null-safe implementation handles cases where components may not be backed by JavaFX
- Uses existing `ScreenComponentType` infrastructure
- Minimal performance impact - only evaluates when tooltip is shown
- Compatible with all existing screen item types

## Related Classes

- `ScreenFactory` - Main factory for creating screens and debug panels
- `ScreenComponentType` - Stores component type and JavaFX node reference
- `InterpreterContext` - Holds screen state and component type mappings
- `AreaItem` - Represents a screen item definition

## Future Enhancements

Potential future improvements:
- Add click action to focus/highlight the actual JavaFX component in the UI
- Include event handlers information (onClick, onChange, etc.)
- Show parent-child component hierarchy
- Add component screenshot preview in tooltip
