# Parse Cache Clearing - Implementation and Testing Guide

## Problem Statement
When a file is edited and saved, it was not being removed from the parsed cache. This meant that if a script imported the edited file, it would use the old cached version instead of re-parsing the updated file.

## Solution Implemented

### 1. Added `removeFromParseCache()` method to Parser.java
A new public static method that removes a specific file from the parse cache:

```java
/**
 * Remove a specific file from the parse cache. This should be called when a file
 * is edited and saved to ensure it will be re-parsed on the next import or execution.
 * 
 * @param file The file path to remove from the cache
 */
public static void removeFromParseCache(Path file) {
    if (file != null) {
        Path normalizedPath = file.toAbsolutePath().normalize();
        String cacheKey = normalizedPath.toString();
        parseCache.remove(cacheKey);
    }
}
```

### 2. Updated EbsConsoleHandler.java - Three save locations

#### A. saveHandle() method - Regular save (Ctrl+S or File > Save)
```java
public void saveHandle(EbsTab tab) {
    // ... existing save logic ...
    callBuiltin("file.writeTextFile", contex.path.toString(), tab.getEditorText());
    tab.markCleanTitle();
    
    // Remove from parse cache since the file has been edited
    com.eb.script.parser.Parser.removeFromParseCache(contex.path);
}
```

#### B. chooseSaveAs() method - Save As dialog
```java
public void chooseSaveAs(EbsTab tab) {
    // ... existing save logic ...
    callBuiltin("file.writeTextFile", newPath.toString(), tab.getEditorText());
    // ... other cleanup ...
    
    // Remove from parse cache since the file has been edited
    com.eb.script.parser.Parser.removeFromParseCache(newPath);
}
```

#### C. Unsaved changes dialog - Save button
```java
if (result.get() == saveButton) {
    // ... existing save logic ...
    callBuiltin("file.writeTextFile", context.path.toString(), tab.getEditorText());
    tab.markCleanTitle();
    addRecentFile(context.path);
    
    // Remove from parse cache since the file has been edited
    com.eb.script.parser.Parser.removeFromParseCache(context.path);
}
```

## How to Test (Manual Verification in GUI)

### Test Case 1: Basic Import Cache Clearing

1. **Create test_import.ebs** with initial content:
```ebs
// Test script that will be imported
var message : string = "Version 1 - Initial";
print("Imported script says: " + message);
```

2. **Create test_main.ebs** that imports the first file:
```ebs
// Main test script
print("=== Main script starting ===");
import "test_import.ebs";
print("=== Main script finished ===");
```

3. **First run** - Open test_main.ebs in the console and run it (Ctrl+Enter or Run button)
   - Expected output:
     ```
     === Main script starting ===
     Imported script says: Version 1 - Initial
     === Main script finished ===
     ```
   - At this point, test_import.ebs is cached

4. **Edit and save** - Open test_import.ebs in a new tab and change it to:
```ebs
// Test script that will be imported - UPDATED!
var message : string = "Version 2 - Updated after edit!";
print("Imported script says: " + message);
```
   - Save the file (Ctrl+S or File > Save)
   - **KEY POINT**: This triggers `removeFromParseCache()`

5. **Second run** - Go back to test_main.ebs tab and run it again
   - Expected output:
     ```
     === Main script starting ===
     Imported script says: Version 2 - Updated after edit!
     === Main script finished ===
     ```
   - ✅ **SUCCESS**: The updated version is used (file was re-parsed because cache was cleared)
   - ❌ **FAILURE**: If "Version 1 - Initial" appears, the cache was not cleared (bug)

### Test Case 2: Save As Cache Clearing

1. Open test_import.ebs and modify the content
2. Use File > Save As to save it to a new location (e.g., test_import_copy.ebs)
3. Update test_main.ebs to import the new file name
4. Run test_main.ebs - should show the modified content

### Test Case 3: Close Tab with Save

1. Open test_import.ebs and make changes
2. Try to close the tab without saving - dialog appears
3. Click "Save" button in the unsaved changes dialog
4. Open test_main.ebs and run it - should show the modified content

## Code Changes Summary

**Files Modified:**
1. `ScriptInterpreter/src/main/java/com/eb/script/parser/Parser.java`
   - Added `removeFromParseCache(Path file)` method

2. `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsConsoleHandler.java`
   - Updated `saveHandle()` to call `removeFromParseCache()`
   - Updated `chooseSaveAs()` to call `removeFromParseCache()`
   - Updated unsaved changes dialog save handler to call `removeFromParseCache()`

**Build Status:** ✅ Compiled successfully with `mvn clean compile`

## Expected Behavior

- **Before fix**: Editing and saving a file would leave the old parsed version in cache. Subsequent imports would use stale data.
- **After fix**: Editing and saving a file removes it from the cache. The next import re-parses the file with the updated content.

## Technical Details

The parse cache uses normalized absolute paths as keys:
```java
Path normalizedPath = file.toAbsolutePath().normalize();
String cacheKey = normalizedPath.toString();
```

This ensures consistent cache key generation regardless of how the path is specified (relative vs absolute).

## Related Files

- `Parser.java` - Contains the parse cache and cache management methods
- `EbsConsoleHandler.java` - Handles file save operations in the UI
- `RuntimeContext.java` - The cached object that represents a parsed script
