# Documentation Directory

This directory contains detailed documentation for the EBS (Earl Bosch Script) language, including language references, guides, and feature documentation.

## Core Language Documentation

### [EBS_SCRIPT_SYNTAX.md](EBS_SCRIPT_SYNTAX.md)
Complete EBS language syntax reference with all keywords, data types, control flow, and built-in functions.

### [EBS_COLLECTIONS_REFERENCE.md](EBS_COLLECTIONS_REFERENCE.md)
Comprehensive guide to all collection types in EBS:
- **Arrays**: Fixed-size and dynamic arrays with multiple syntax variants
- **Queues**: FIFO data structures for task processing
- **Maps**: Key-value stores for configuration and lookups
- **JSON**: Flexible structures for data interchange

Includes comparison tables, performance considerations, and detailed examples for choosing the right collection type.

### [ARRAY_SYNTAX_GUIDE.md](ARRAY_SYNTAX_GUIDE.md)
Detailed comparison of array syntax variants (`int[n]` vs `array.int[n]`) with performance benchmarks and memory usage analysis.

### [PYTHON_VS_EBS_STRING_FUNCTIONS.md](PYTHON_VS_EBS_STRING_FUNCTIONS.md)
Comparison guide for Python developers learning EBS Script.

### [EBS_VS_ORACLE_PLSQL.md](EBS_VS_ORACLE_PLSQL.md)
Comprehensive comparison guide for PL/SQL developers learning EBS Script.

## Feature-Specific Documentation

### Screen Variable Sets

#### [SCOPE_REFACTORING.md](SCOPE_REFACTORING.md)
Comprehensive guide to the scope refactoring that consolidated parameter direction into the VarSet scope field.

**Topics covered:**
- What changed (before/after comparison)
- Scope values and their meanings
- JSON format examples
- API changes
- Migration guide
- Benefits of the new approach

#### [VARSET_API.md](VARSET_API.md)
Complete API reference for the VarSet class.

**Topics covered:**
- Class overview and package
- Fields and their purposes
- All constructors
- All public methods with parameters and return values
- Usage examples
- Code snippets for common operations

#### [BUGFIX_GETITEMLIST.md](BUGFIX_GETITEMLIST.md)
Documentation of the screen.getItemList bug fix.

**Topics covered:**
- Issue description
- Root cause analysis
- Code comparison (before/after)
- Affected functions
- Testing approach
- Impact assessment
- Backward compatibility

## Additional Documents

### [SCOPE_REFACTORING.md](SCOPE_REFACTORING.md)
Comprehensive guide to the scope refactoring that consolidated parameter direction into the VarSet scope field.

**Topics covered:**
- What changed (before/after comparison)
- Scope values and their meanings
- JSON format examples
- API changes
- Migration guide
- Benefits of the new approach

### [VARSET_API.md](VARSET_API.md)
Complete API reference for the VarSet class.

**Topics covered:**
- Class overview and package
- Fields and their purposes
- All constructors
- All public methods with parameters and return values
- Usage examples
- Code snippets for common operations

### [BUGFIX_GETITEMLIST.md](BUGFIX_GETITEMLIST.md)
Documentation of the screen.getItemList bug fix.

**Topics covered:**
- Issue description
- Root cause analysis
- Code comparison (before/after)
- Affected functions
- Testing approach
- Impact assessment
- Backward compatibility

## Quick Reference

### Scope Values

| Value | Alias | Meaning |
|-------|-------|---------|
| `visible` | - | Visible in UI (default) |
| `internal` | - | Hidden from UI |
| `in` | `parameterIn` | Input parameters |
| `out` | `parameterOut` | Output parameters |
| `inout` | - | Input/output parameters |

### Key Methods

**VarSet:**
```java
boolean isInternal()  // Check if hidden
boolean isInput()     // Check if input parameter
boolean isOutput()    // Check if output parameter
```

## Related Documentation

Outside this directory:

- **[../VARIABLE_SETS_VISUAL_GUIDE.md](../VARIABLE_SETS_VISUAL_GUIDE.md)** - Visual guide with diagrams
- **[../SCOPE_AND_DIRECTION_CHANGELOG.md](../SCOPE_AND_DIRECTION_CHANGELOG.md)** - Full changelog
- **[../README.md](../README.md)** - Main project README
- **[../ARCHITECTURE.md](../ARCHITECTURE.md)** - System architecture

## Example Code

### JSON Screen Definition

```json
{
  "title": "My Screen",
  "sets": [
    {
      "setname": "Inputs",
      "scope": "in",
      "vars": [
        {"name": "sourceFile", "type": "string", "default": "data.csv"}
      ]
    },
    {
      "setname": "Outputs",
      "scope": "out",
      "vars": [
        {"name": "recordCount", "type": "int", "default": 0}
      ]
    },
    {
      "setname": "Settings",
      "scope": "inout",
      "vars": [
        {"name": "batchSize", "type": "int", "default": 100}
      ]
    },
    {
      "setname": "InternalState",
      "scope": "internal",
      "vars": [
        {"name": "processId", "type": "int", "default": 0}
      ]
    }
  ]
}
```

### Java Code

```java
// Get a var set
VarSet inputSet = context.getScreenVarSet("myScreen", "Inputs");

// Check scope
if (inputSet.isInput() && !inputSet.isOutput()) {
    System.out.println("Input-only parameters");
}

// Get variables from set
Var sourceFile = inputSet.getVariable("sourceFile");
String value = (String) sourceFile.getValue();
```

### EBS Script

```ebs
screen myScreen = {
    "title": "Data Processor",
    "sets": [
        {
            "setname": "Config",
            "scope": "in",
            "vars": [
                {"name": "inputFile", "type": "string", "default": "data.txt"}
            ]
        }
    ]
};

// Access variable
print myScreen.Config.inputFile;

// Get item list
var items = call screen.getItemList("myScreen");
print "Number of items: " + items.length();
```

## Test Scripts

Test scripts are located in `ScriptInterpreter/scripts/`:

- **test_scope_and_direction_comprehensive.ebs** - Comprehensive test suite
- **test_variable_sets_with_direction.ebs** - Example demonstrating features
- **test_screen_getitemlist.ebs** - Tests screen.getItemList function
- **test_getitemlist_sets.ebs** - Tests getItemList with sets structure

## Contributing

When adding new documentation:

1. Follow the existing format and style
2. Include code examples
3. Add cross-references to related documents
4. Update this README to list the new document
5. Keep examples practical and runnable

## Questions?

For questions or issues:
1. Check the existing documentation
2. Review the test scripts for working examples
3. Consult the main project README
4. See the ARCHITECTURE.md for system design
