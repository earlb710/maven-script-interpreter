# EBS Language Reference

**Documentation Version: 1.0.4.1**

This document serves as a pointer to the comprehensive EBS (Earl Bosch Script) language documentation.

## Main Language Reference

For the complete language syntax reference, built-in functions, and examples, see:

**[docs/EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md)**

This document includes:
- Basic syntax and program structure
- Data types and type casting
- Variables and operators
- Control flow (if, while, for, foreach, do-while)
- Exception handling (try-exceptions-when)
- Code organization (imports, type aliases)
- Functions and parameters
- Arrays and JSON
- Database operations
- Screen/UI Windows
- All built-in functions organized by category:
  - String functions
  - JSON functions
  - File I/O functions
  - HTTP functions
  - CSS functions
  - Array functions
  - Queue functions
  - System functions
  - Screen functions
  - Debug functions
  - AI functions
- Console commands
- Best practices and examples

## Keyword Examples

Short, precise examples for each EBS keyword are available in:

**[ScriptInterpreter/scripts/examples/](ScriptInterpreter/scripts/examples/)**

Each keyword has its own example file demonstrating correct usage.

## Quick Reference

### Grammar Specification
For the formal EBNF grammar, see:
- [ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt](ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt)

### Related Documentation
- [README.md](README.md) - Project overview and quick start
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - System architecture and internals
- [docs/PYTHON_VS_EBS_STRING_FUNCTIONS.md](docs/PYTHON_VS_EBS_STRING_FUNCTIONS.md) - Comparison for Python developers
- [docs/EBS_VS_ORACLE_PLSQL.md](docs/EBS_VS_ORACLE_PLSQL.md) - Comparison for PL/SQL developers

## Hello World Example

```javascript
var message: string = "Hello, World!";
print message;
```

## Version Information

This documentation corresponds to the EBS Script Interpreter version 1.0-SNAPSHOT.

### Version Format

EBS uses a four-part versioning system: **`language.keyword.builtin.build`**

| Part | Name | Description |
|------|------|-------------|
| 1st | Language Version | Major language changes that break compatibility with previous versions |
| 2nd | Keyword Version | Incremented when keywords are added, modified, or removed |
| 3rd | Builtin Version | Incremented when builtin functions are added, modified, or removed |
| 4th | Build Number | Incremented with each release/build |

Each version component is incremented independently based on the type of change made.

**Current Version: 1.0.4.1** (Language v1, Keyword v0, Builtin v4, Build 1)

### Getting the Language Version Programmatically

Use the `system.getEBSver` builtin function to retrieve the current EBS language version at runtime:

```javascript
var version = call system.getEBSver();
print "EBS Language Version: " + version;  // Output: EBS Language Version: 1.0.4.1
```

### Testing Version Compatibility

Use the `system.testEBSver` builtin function to check if the running version meets a minimum version requirement:

```javascript
// Returns true if running version >= supplied version
if call system.testEBSver("1.0.1") then {
    print "Version 1.0.1 or higher features are available";
}

// Check for specific builtin availability (v1.0.3 added ai.completeAsync)
if call system.testEBSver("1.0.3") then {
    print "ai.completeAsync function is available";
}
```

For the latest updates and comprehensive language reference, always refer to [docs/EBS_SCRIPT_SYNTAX.md](docs/EBS_SCRIPT_SYNTAX.md).

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.4.1 | 2025-12-03 | Added plugin system for loading external Java functions (EbsFunction interface) |
| 1.0.3.1 | 2025-12-01 | Added ai.completeAsync builtin for asynchronous AI calls with callback support |
| 1.0.2.1 | 2025-11-30 | Added 4th version component (build number); Format now language.keyword.builtin.build |
| 1.0.2 | 2025-11-30 | Added system.testEBSver builtin function; Changed to 3-part versioning (language.keyword.builtin) |
| 1.0.1 | 2025-11-30 | Added system.getEBSver builtin function to return language version |
| 1.0.0 | 2025-11-29 | Initial version - Created language reference with keyword examples |

---

## Maintaining This Documentation

When adding or removing **keywords**, **built-in functions**, or other language features, the following files must be updated to keep documentation in sync:

1. **Documentation Files:**
   - `docs/EBS_SCRIPT_SYNTAX.md` - Main language syntax reference
   - `EBS_LANGUAGE_REFERENCE.md` - This file (update version and changelog)
   - `README.md` - If the change affects the project overview

2. **Help System:**
   - `ScriptInterpreter/src/main/resources/help-lookup.json` - Built-in help lookup data

3. **Example Scripts:**
   - `ScriptInterpreter/scripts/examples/` - Individual example files for each keyword
   - **Important:** All example scripts must include this reference comment as the first line:
     ```
     // EBS Language Reference v1.0.2.1 - See EBS_LANGUAGE_REFERENCE.md
     ```
     Update the version number to match the current documentation version.

4. **Source Code (for builtin changes):**
   - `ScriptInterpreter/src/main/java/com/eb/script/interpreter/builtins/BuiltinsSystem.java` - Update version constants:
     - `LANGUAGE_VER` - Increment for major incompatible language changes
     - `KEYWORD_VER` - Increment for keyword additions/changes
     - `BUILTIN_VER` - Increment for builtin function additions/changes
     - `BUILD_VER` - Increment for each release/build

5. **Grammar Specification:**
   - `ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt` - EBNF grammar (if syntax changes)

**Version Update Rule:** Increment the documentation version in this file whenever significant changes are made to the language documentation.
