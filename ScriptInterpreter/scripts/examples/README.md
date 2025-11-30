# EBS Keyword Examples

This directory contains short, precise example scripts for each EBS language keyword.

## Control Flow
| File | Keywords |
|------|----------|
| [if_then_else.ebs](if_then_else.ebs) | `if`, `then`, `else` |
| [while.ebs](while.ebs) | `while` |
| [do_while.ebs](do_while.ebs) | `do`, `while` |
| [for.ebs](for.ebs) | `for` loop |
| [foreach.ebs](foreach.ebs) | `foreach`, `in` |
| [break.ebs](break.ebs) | `break`, `exit` |
| [continue.ebs](continue.ebs) | `continue` |

## Variables & Types
| File | Keywords |
|------|----------|
| [var.ebs](var.ebs) | `var` |
| [const.ebs](const.ebs) | `const` |
| [datatypes.ebs](datatypes.ebs) | `byte`, `int`, `long`, `float`, `double`, `string`, `date`, `bool`, `json`, `record` |
| [array.ebs](array.ebs) | Array syntax |
| [json.ebs](json.ebs) | `json` type |
| [record.ebs](record.ebs) | `record` type |
| [casting.ebs](casting.ebs) | Type casting |
| [null.ebs](null.ebs) | `null` |
| [true_false.ebs](true_false.ebs) | `true`, `false` |

## Functions & Calls
| File | Keywords |
|------|----------|
| [function.ebs](function.ebs) | Function definition |
| [call.ebs](call.ebs) | `call` |
| [return.ebs](return.ebs) | `return` |
| [print.ebs](print.ebs) | `print` |

## Operators & Expressions
| File | Keywords |
|------|----------|
| [and_or.ebs](and_or.ebs) | `and`, `or` |
| [typeof.ebs](typeof.ebs) | `typeof` |
| [length.ebs](length.ebs) | `length` |

## Exception Handling
| File | Keywords |
|------|----------|
| [try_exceptions.ebs](try_exceptions.ebs) | `try`, `exceptions`, `when` |
| [raise.ebs](raise.ebs) | `raise`, `exception` |

## Modules
| File | Keywords |
|------|----------|
| [import.ebs](import.ebs) | `import` |

## UI/Screens
| File | Keywords |
|------|----------|
| [screen.ebs](screen.ebs) | `screen`, `show`, `hide` |

## Database
| File | Keywords |
|------|----------|
| [connect.ebs](connect.ebs) | `connect`, `use`, `close`, `connection` |
| [cursor.ebs](cursor.ebs) | `cursor`, `open`, `close` |
| [select.ebs](select.ebs) | `select`, `from`, `where`, `order`, `by`, `group`, `having` |

## System Builtins
| File | Builtin |
|------|---------|
| [getEBSver.ebs](getEBSver.ebs) | `system.getEBSver` - Get EBS language version |
| [getEBSver.ebs](getEBSver.ebs) | `system.testEBSver` - Test if running version >= supplied version |

## Running Examples

From the ScriptInterpreter directory:

```bash
# Run a specific example
java -cp target/classes com.eb.script.Run scripts/examples/var.ebs

# Or use Maven
mvn javafx:run
# Then load file from the console
```

## Adding New Examples

When adding new keywords to EBS, create a corresponding example file following this template:

```javascript
// EBS Language Reference v1.4.0 - See EBS_LANGUAGE_REFERENCE.md
// Example: keyword_name keyword - Brief description
// Syntax: keyword syntax

// Working example code here
```

**Important:** Always include the version reference comment as the first line of every example script. Update the version number to match the current documentation version in `EBS_LANGUAGE_REFERENCE.md`.
