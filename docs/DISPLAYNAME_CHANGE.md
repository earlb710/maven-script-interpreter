# Display Name Field for Tab Labels

## Summary

This document describes the change from using `screenName` to `displayName` for tab labels in area definitions.

## Change Request

The user requested to:
1. Change the `screenName` JSON field in areas to `displayName`
2. Use this `displayName` to set the tab label

## Rationale

The `screenName` field was being used for two different purposes:
1. **Internal tracking**: To reference the overall screen name (passed as parameter)
2. **UI display**: To show tab labels in the user interface

This dual usage created confusion. The change separates these concerns:
- `screenName`: Internal identifier for the screen context
- `displayName`: User-facing label for UI elements (like tab labels)

## Changes Made

### 1. AreaDefinition.java

**Added new field** (line 25-26):
```java
// Associated screen name
public String screenName;
// Display name for UI elements (e.g., tab labels)
public String displayName;
```

**Impact**: AreaDefinition now has a dedicated field for display purposes.

### 2. ScreenFactory.java

**Added displayName parsing** (lines 962-964):
```java
area.screenName = screenName;

// Extract displayName for UI labels (e.g., tab labels)
area.displayName = getStringValue(areaDef, "displayName", null);
```

**Updated tab label assignment** (line 440):
```java
// Before:
tab.setText(childArea.screenName != null ? childArea.screenName : childArea.name);

// After:
tab.setText(childArea.displayName != null ? childArea.displayName : childArea.name);
```

**Impact**: Tab labels now use `displayName` with fallback to `name`.

### 3. test_screen_with_tabs.ebs

**Updated all three tab definitions**:

**Tab 1 - Contact Information** (line 253):
```json
{
    "name": "contactTab",
    "type": "tab",
    "displayName": "Contact Information",  // Changed from "screenName"
    "areas": [...]
}
```

**Tab 2 - Account Details** (line 337):
```json
{
    "name": "accountTab",
    "type": "tab",
    "displayName": "Account Details",  // Changed from "screenName"
    "areas": [...]
}
```

**Tab 3 - Summary** (line 423):
```json
{
    "name": "summaryTab",
    "type": "tab",
    "displayName": "Summary",  // Changed from "screenName"
    "areas": [...]
}
```

## Technical Details

### Field Separation

| Field | Purpose | Usage | Required |
|-------|---------|-------|----------|
| `name` | Internal identifier | Area identification, code references | Yes |
| `screenName` | Screen context | Tracking which screen the area belongs to | Auto-set |
| `displayName` | UI label | Tab labels, display text | Optional |

### Fallback Logic

The tab label assignment uses a fallback chain:
1. If `displayName` is provided → use it
2. Otherwise → use `name`

```java
tab.setText(childArea.displayName != null ? childArea.displayName : childArea.name);
```

This ensures tabs always have a label, even if `displayName` is not specified.

### JSON Schema

**New area definition structure**:
```json
{
    "name": "myTab",              // Required - Internal identifier
    "type": "tab",                // Required - Area type
    "displayName": "My Tab",      // Optional - User-facing label
    "areas": [...]                // Optional - Child areas
}
```

**Backward Compatibility**:
- Old scripts without `displayName` will still work (uses `name` as fallback)
- `screenName` field is no longer used for tab labels but remains for internal tracking

## Benefits

1. **Clear Separation**: Internal identifiers separate from display text
2. **Flexibility**: Display text can be different from internal name
3. **Localization Ready**: `displayName` can be easily localized without affecting code references
4. **Better Semantics**: Code is more self-documenting

## Example Usage

**Before** (using screenName):
```json
{
    "name": "tab1",
    "type": "tab",
    "screenName": "User Information"  // Used for both tracking and display
}
```

**After** (using displayName):
```json
{
    "name": "tab1",              // Internal identifier
    "type": "tab",
    "displayName": "User Information"  // Display label only
}
```

## Testing

To verify the changes:

1. **Run the test script**:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. **Open and execute**:
   - File → Open `scripts/test_screen_with_tabs.ebs`
   - Press Ctrl+Enter

3. **Verify**:
   - Tab 1 shows "Contact Information"
   - Tab 2 shows "Account Details"
   - Tab 3 shows "Summary"
   - All labels match the `displayName` values

## Migration Guide

For existing scripts using `screenName` for tab labels:

**Step 1**: Replace `screenName` with `displayName`
```json
// Before
{
    "name": "myTab",
    "type": "tab",
    "screenName": "My Tab Label"
}

// After
{
    "name": "myTab",
    "type": "tab",
    "displayName": "My Tab Label"
}
```

**Step 2**: Test and verify tab labels display correctly

**Note**: Scripts without `displayName` will use `name` as the tab label.

## Files Changed

1. **AreaDefinition.java**: Added `displayName` field (2 lines)
2. **ScreenFactory.java**: Updated parsing and tab label logic (5 lines)
3. **test_screen_with_tabs.ebs**: Changed 3 occurrences of `screenName` to `displayName`

**Total Impact**: 9 insertions, 4 deletions

## Commit Information

**Commit Hash**: d7bd473
**Commit Message**: Change screenName to displayName for tab labels in areas

## Conclusion

The change successfully separates internal identifiers from display labels:
- ✅ `displayName` field added to AreaDefinition
- ✅ JSON parsing updated to read `displayName`
- ✅ Tab labels use `displayName` instead of `screenName`
- ✅ Test script updated with new field
- ✅ Build successful (4.142s)
- ✅ Backward compatible with fallback to `name`

The API is now clearer and more maintainable.
