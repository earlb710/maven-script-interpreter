# Bug Fix: screen.getItemList

## Issue

The `screen.getItemList()` builtin function was not returning items for screens that used the new "sets" structure with display metadata but no explicit "area" definitions.

## Root Cause

When screens have variables with display metadata but no explicit `"area"` property in the JSON, the interpreter automatically creates a default area (VBox) containing items for all displayed variables.

However, these automatically created items were being added to the area's item list but **not** being registered in the `areaItemsMap`. The `screen.getItemList()` function (and related functions like `screen.getProperty()` and `screen.setProperty()`) rely on `areaItemsMap` to find items.

## Affected Functions

This bug affected three builtin functions:

1. **screen.getItemList(screenName)** - Returns list of all item names
2. **screen.getProperty(screenName.itemName, propertyName)** - Gets item property
3. **screen.setProperty(screenName.itemName, propertyName, value)** - Sets item property

All three functions use `context.getScreenAreaItems()` which returns `areaItemsMap`.

## Code Location

The issue was in `InterpreterScreen.java`, in the section that creates default areas (around line 165-275).

### Before the Fix

```java
// Create a single item for the input control
AreaItem item = new AreaItem();
item.name = varName + "_field";
item.varRef = varName;
item.sequence = defaultArea.items.size();
item.displayItem = varDisplayItem;

// ... set sizing properties ...

defaultArea.items.add(item);
// BUG: Item added to area but NOT registered in areaItemsMap!
```

### After the Fix

```java
// Create a single item for the input control
AreaItem item = new AreaItem();
item.name = varName + "_field";
item.varRef = varName;
item.sequence = defaultArea.items.size();
item.displayItem = varDisplayItem;

// ... set sizing properties ...

defaultArea.items.add(item);

// FIX: Register item in areaItemsMap for screen.getItemList to find
// Store by item name (lowercase) for direct lookup
if (item.name != null && !item.name.isEmpty()) {
    areaItemsMap.put(item.name.toLowerCase(), item);
}

// Also store by varRef for variable-to-item linking
if (item.varRef != null && !item.varRef.isEmpty()) {
    // For default area with legacy vars, use "default.varname" format
    String areaItemKey = "default." + item.varRef.toLowerCase();
    areaItemsMap.put(areaItemKey, item);
}
```

## Testing

### Test Script

A test script `test_screen_getitemlist.ebs` was created to verify the fix:

```ebs
screen testScreen = {
    "title": "Test Screen",
    "width": 600,
    "height": 400,
    "sets": [
        {
            "setname": "TestSet",
            "scope": "visible",
            "vars": [
                {
                    "name": "field1",
                    "type": "string",
                    "default": "value1",
                    "display": {"type": "textfield", "labeltext": "Field 1"}
                },
                {
                    "name": "field2",
                    "type": "int",
                    "default": 42,
                    "display": {"type": "spinner", "min": 0, "max": 100, "labeltext": "Field 2"}
                }
            ]
        }
    ]
};

var itemList = call screen.getItemList("testScreen");
print "Number of items: " + itemList.length();
```

### Expected Results

After the fix:
- `screen.getItemList()` returns a list with items: `["field1_field", "field2_field"]`
- `screen.getProperty()` can successfully get properties from these items
- `screen.setProperty()` can successfully set properties on these items

## Impact

### Screens Affected

This bug affected screens that:
1. Use the new "sets" structure (not legacy "vars")
2. Have variables with display metadata
3. Do NOT have an explicit "area" definition

### Screens Not Affected

Screens with explicit "area" definitions were not affected because those items are registered in `areaItemsMap` during the `parseAreaDefinition()` process.

## Backward Compatibility

The fix maintains full backward compatibility:
- Screens with explicit "area" definitions continue to work as before
- Screens using legacy "vars" format (automatically converted to "default" set) work correctly
- All existing functionality is preserved

## Related Changes

This fix works in conjunction with:
- The VarSet scope refactoring (hiddenInd → scope)
- Support for parameter direction in scope values
- The variable sets structure

## Commit

Fixed in commit: `f0a3a98`
