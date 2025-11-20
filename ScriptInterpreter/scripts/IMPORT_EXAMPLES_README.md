# Import Functionality Examples

This directory contains examples demonstrating the EBS import functionality.

## Files

### Subdirectory: `util/`
- **stringUtil.ebs** - String utility functions
  - `toUpper(text: string): string` - Converts text to uppercase

### Subdirectory: `test dir/subdir/`
- **helper.ebs** - Helper functions demonstrating imports from paths with spaces
  - `greet(name: string): string` - Returns a greeting message

### Test Scripts
- **test_subdir_import.ebs** - Simple test of subdirectory imports
- **test_import_with_spaces.ebs** - Test imports from paths with spaces
- **import_examples.ebs** - Comprehensive demonstration of all import features

## Import Features

### Subdirectory Imports
```javascript
import "util/stringUtil.ebs";
```

### Spaces in Paths
```javascript
import "test dir/subdir/helper.ebs";
```

### Quote Types
Both single and double quotes are supported:
```javascript
import "util/stringUtil.ebs";  // Double quotes
import 'util/stringUtil.ebs';  // Single quotes
```

### Path Normalization
Redundant path elements are automatically normalized:
```javascript
import "./util/../util/stringUtil.ebs";  // Normalized to: util/stringUtil.ebs
```

### Circular Import Prevention
Files are only imported once. Subsequent import statements for the same file are automatically skipped.

## Running the Examples

```bash
# From ScriptInterpreter directory
mvn javafx:run

# Then in the console:
/open scripts/import_examples.ebs
```

Or run individual test scripts:
```bash
/open scripts/test_subdir_import.ebs
/open scripts/test_import_with_spaces.ebs
```
