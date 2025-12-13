# EBS Language Reference

**Documentation Version: 1.0.7.11**

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
  - Mail functions
  - CSS functions
  - Array functions
  - Queue functions
  - System functions
  - Screen functions
  - Debug functions
  - AI functions
  - Date/Time functions
  - Cryptographic functions
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
- [docs/EBS_COLLECTIONS_REFERENCE.md](docs/EBS_COLLECTIONS_REFERENCE.md) - Comprehensive collections guide (arrays, queues, maps, JSON)
- [docs/ARRAY_SYNTAX_GUIDE.md](docs/ARRAY_SYNTAX_GUIDE.md) - Detailed array syntax comparison and performance guide
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

**Current Version: 1.0.7.11** (Language v1, Keyword v0, Builtin v7, Build 11)

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
| 1.0.7.11 | 2025-12-12 | Added TreeView icon management functions: `scr.setTreeItemIcon`, `scr.setTreeItemIcons`, `scr.getTreeItemIcon` for dynamically setting icons on tree view items at runtime |
| 1.0.7.10 | 2025-12-12 | Added cryptographic functions: `crypto.encrypt`, `crypto.decrypt`, `crypto.generateKey`, `crypto.hash`, `crypto.sha256`, `crypto.md5`; AES-256-GCM encryption support; Password-based encryption with key derivation |
| 1.0.6.11 | 2025-12-06 | Added `intmap` type for 32-bit field mapping (0-31 bits) using integer storage; Added `array.intmap` backed by ArrayFixedInt; Added `array.asIntmap()` and `array.asInt()` for casting; Intmap type aliases with `typeof` keyword; Int to intmap casting |
| 1.0.6.10 | 2025-12-05 | Added `array.bitmap` type backed by ArrayFixedByte; Added `array.asBitmap()` and `array.asByte()` for casting between byte and bitmap arrays |
| 1.0.6.9 | 2025-12-05 | Added `bitmap` type for bit-level field access within a byte; Bitmap type aliases with `typeof` keyword; Byte/int to bitmap casting; `typeof` operator support for bitmap types and variables |
| 1.0.5.8 | 2025-12-03 | Added `mail.openUrl` and `ftp.openUrl` functions for URL-based connections; Password now optional in URL (stored separately in config); Added password column with masking in config dialogs |
| 1.0.5.7 | 2025-12-03 | Config variable names are now case-insensitive (e.g., `MyFtp` can be accessed as `myftp` in scripts) |
| 1.0.5.6 | 2025-12-03 | Mail and FTP config variables are now automatically available as global script variables (e.g., `print myemail;`) |
| 1.0.5.5 | 2025-12-03 | Added URL format for mail and FTP connections: `******host:port?protocol=imaps`, `******host:port`, `******host:port`; Config dialogs now store URLs |
| 1.0.5.4 | 2025-12-03 | Added Mail Server Config and FTP Server Config dialogs under Config menu; Updated help docs for Gmail app password format (16 chars, no spaces) |
| 1.0.5.3 | 2025-12-03 | Added optional timeout parameter for mail.open, ftp.open, and ftp.openSecure connections |
| 1.0.5.2 | 2025-12-03 | Allow reserved keywords (`open`, `connect`) to be used as builtin method names (e.g., `ftp.open`, `mail.open`) |
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
