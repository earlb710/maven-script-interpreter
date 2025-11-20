# Import Feature Examples

This document demonstrates the enhanced import functionality in EBS.

## Feature 1: Subdirectory Imports

### Before
Only files in the same directory could be imported:
```javascript
import "helper.ebs";  // Same directory only
```

### After
Files in subdirectories can now be imported:
```javascript
import "util/stringUtil.ebs";           // One level deep
import "lib/database/mysql.ebs";        // Multiple levels
import "utils/math/advanced.ebs";       // Any depth
```

## Feature 2: Spaces in Paths

### Support for Directory/File Names with Spaces
```javascript
// Spaces in directory names
import "my utils/helper.ebs";
import "test dir/subdir/helper.ebs";

// Spaces in file names
import "string functions.ebs";
import "database helpers.ebs";

// Combination of both
import "my utils/string functions.ebs";
```

## Feature 3: User-Friendly Import Tracking

### Before
Import paths were stored as absolute system paths:
```
Imported files list:
- /home/user/project/scripts/util/stringUtil.ebs
- /home/user/project/scripts/helper_math.ebs
- /home/user/project/scripts/test dir/subdir/helper.ebs
```

### After
Import paths are stored as written by the user (normalized):
```
Imported files list:
- util/stringUtil.ebs
- helper_math.ebs
- test dir/subdir/helper.ebs
```

## Additional Features

### Quote Types
Both single and double quotes are supported:
```javascript
import "util/stringUtil.ebs";   // Double quotes
import 'util/stringUtil.ebs';   // Single quotes
```

### Path Normalization
Redundant path elements are automatically normalized:
```javascript
import "./util/stringUtil.ebs";          // Normalized to: util/stringUtil.ebs
import "util/../util/stringUtil.ebs";    // Normalized to: util/stringUtil.ebs
import "./lib/./database.ebs";           // Normalized to: lib/database.ebs
```

### Circular Import Prevention
Files are only imported once:
```javascript
// main.ebs
import "helper.ebs";
import "helper.ebs";  // Automatically skipped

// Output: "Skipped (already imported): helper.ebs"
```

## Complete Example

### Directory Structure
```
scripts/
├── main.ebs
├── util/
│   ├── stringUtil.ebs
│   └── mathUtil.ebs
├── test dir/
│   └── subdir/
│       └── helper.ebs
└── my utils/
    └── database functions.ebs
```

### main.ebs
```javascript
print "=== Import Examples ===";

// Import from subdirectory
import "util/stringUtil.ebs";
import "util/mathUtil.ebs";

// Import from path with spaces
import "test dir/subdir/helper.ebs";
import "my utils/database functions.ebs";

// Use imported functions
var upper: string = call toUpper("hello");
var sum: int = call add(5, 3);
var greeting: string = call greet("User");

print "All imports successful!";
```

## Benefits

1. **Better Organization**: Keep related files in subdirectories
2. **Clearer Structure**: Organize code by functionality
3. **Flexible Naming**: Use descriptive names with spaces
4. **User-Friendly**: Import paths match what you write
5. **Platform Independent**: Works on all operating systems
6. **Backward Compatible**: Existing imports still work

## Migration Guide

### No Changes Required
All existing import statements continue to work:
```javascript
import "helper.ebs";  // ✓ Still works
```

### Optional Improvements
You can now organize your code better:
```javascript
// Instead of:
import "helper_string.ebs";
import "helper_math.ebs";
import "helper_database.ebs";

// You can do:
import "helpers/string.ebs";
import "helpers/math.ebs";
import "helpers/database.ebs";
```

## Testing

Run the comprehensive example:
```bash
cd ScriptInterpreter
mvn javafx:run
# Then in console:
/open scripts/import_examples.ebs
```

## Summary

The enhanced import functionality provides:
- ✅ Subdirectory support
- ✅ Spaces in paths
- ✅ Both quote types
- ✅ User-friendly tracking
- ✅ Path normalization
- ✅ Circular import prevention
- ✅ Backward compatibility

All features work seamlessly together for a better development experience.
