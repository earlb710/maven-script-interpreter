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

## String Builtins (str.* / string.*)
| File | Builtin | Description |
|------|---------|-------------|
| [str_tostring.ebs](str_tostring.ebs) | `str.tostring` | Convert value to string |
| [str_toupper.ebs](str_toupper.ebs) | `str.toupper` | Convert to uppercase |
| [str_tolower.ebs](str_tolower.ebs) | `str.tolower` | Convert to lowercase |
| [str_trim.ebs](str_trim.ebs) | `str.trim` | Remove leading/trailing whitespace |
| [str_replace.ebs](str_replace.ebs) | `str.replace` | Replace all occurrences |
| [str_replacefirst.ebs](str_replacefirst.ebs) | `str.replacefirst` | Replace first occurrence only |
| [str_split.ebs](str_split.ebs) | `str.split` | Split string into array |
| [str_join.ebs](str_join.ebs) | `str.join` | Join array elements into string |
| [str_contains.ebs](str_contains.ebs) | `str.contains` | Check if substring exists |
| [str_startswith.ebs](str_startswith.ebs) | `str.startswith` | Check if starts with prefix |
| [str_endswith.ebs](str_endswith.ebs) | `str.endswith` | Check if ends with suffix |
| [str_equals.ebs](str_equals.ebs) | `str.equals` | Case-sensitive equality |
| [str_equalsignorecase.ebs](str_equalsignorecase.ebs) | `str.equalsignorecase` | Case-insensitive equality |
| [str_isempty.ebs](str_isempty.ebs) | `str.isempty` | Check if empty (zero length) |
| [str_isblank.ebs](str_isblank.ebs) | `str.isblank` | Check if empty or whitespace only |
| [str_substring.ebs](str_substring.ebs) | `str.substring` | Extract portion of string |
| [str_indexof.ebs](str_indexof.ebs) | `str.indexof` | Find first occurrence index |
| [str_lastindexof.ebs](str_lastindexof.ebs) | `str.lastindexof` | Find last occurrence index |
| [str_charat.ebs](str_charat.ebs) | `str.charat` | Get character at index |
| [str_replaceall.ebs](str_replaceall.ebs) | `str.replaceall` | Regex-based replace all |
| [str_lpad.ebs](str_lpad.ebs) | `str.lpad` | Left pad to length |
| [str_rpad.ebs](str_rpad.ebs) | `str.rpad` | Right pad to length |
| [str_chararray.ebs](str_chararray.ebs) | `str.chararray` | Get Unicode code points array |
| [str_findregex.ebs](str_findregex.ebs) | `str.findregex` | Find first regex match |
| [str_findallregex.ebs](str_findallregex.ebs) | `str.findallregex` | Find all regex matches |

## Screen Builtins (scr.*)
| File | Builtin | Description |
|------|---------|-------------|
| [scr_findscreen.ebs](scr_findscreen.ebs) | `scr.findScreen` | Check if a screen has been defined |
| [scr_showscreen.ebs](scr_showscreen.ebs) | `scr.showScreen` | Show a screen programmatically |
| [scr_hidescreen.ebs](scr_hidescreen.ebs) | `scr.hideScreen` | Hide a screen programmatically |
| [scr_closescreen.ebs](scr_closescreen.ebs) | `scr.closeScreen` | Close a screen programmatically |
| [scr_getproperty.ebs](scr_getproperty.ebs) | `scr.getProperty` | Get property value from a screen item |
| [scr_setproperty.ebs](scr_setproperty.ebs) | `scr.setProperty` | Set property value on a screen item |
| [scr_getitemlist.ebs](scr_getitemlist.ebs) | `scr.getItemList` | Get list of all item names in a screen |
| [scr_setstatus.ebs](scr_setstatus.ebs) | `scr.setStatus` | Set the status of a screen |
| [scr_getstatus.ebs](scr_getstatus.ebs) | `scr.getStatus` | Get the current status of a screen |
| [scr_seterror.ebs](scr_seterror.ebs) | `scr.setError` | Set an error message on a screen |
| [scr_geterror.ebs](scr_geterror.ebs) | `scr.getError` | Get the current error message from a screen |
| [scr_checkchanged.ebs](scr_checkchanged.ebs) | `scr.checkChanged` | Check if a screen has unsaved changes |
| [scr_checkerror.ebs](scr_checkerror.ebs) | `scr.checkError` | Check if a screen has an error |
| [scr_revert.ebs](scr_revert.ebs) | `scr.revert` | Revert screen fields to original values |
| [scr_clear.ebs](scr_clear.ebs) | `scr.clear` | Clear all field values in a screen |
| [scr_getitemsource.ebs](scr_getitemsource.ebs) | `scr.getItemSource` | Get the source type of a screen item |
| [scr_setitemsource.ebs](scr_setitemsource.ebs) | `scr.setItemSource` | Set the source type of a screen item |
| [scr_getitemstatus.ebs](scr_getitemstatus.ebs) | `scr.getItemStatus` | Get the status of a screen item |
| [scr_resetitemoriginalvalue.ebs](scr_resetitemoriginalvalue.ebs) | `scr.resetItemOriginalValue` | Reset an item's original value |
| [scr_getvarreference.ebs](scr_getvarreference.ebs) | `scr.getVarReference` | Get the variable reference for an item |
| [scr_getareaproperty.ebs](scr_getareaproperty.ebs) | `scr.getAreaProperty` | Get property value from a screen area |
| [scr_setareaproperty.ebs](scr_setareaproperty.ebs) | `scr.setAreaProperty` | Set property value on a screen area |
| [scr_setitemchoiceoptions.ebs](scr_setitemchoiceoptions.ebs) | `scr.setItemChoiceOptions` | Set options for a dropdown/combobox |
| [scr_getitemchoiceoptions.ebs](scr_getitemchoiceoptions.ebs) | `scr.getItemChoiceOptions` | Get options from a dropdown/combobox |

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
// EBS Language Reference v1.0.2.1 - See EBS_LANGUAGE_REFERENCE.md
// Example: keyword_name keyword - Brief description
// Syntax: keyword syntax

// Working example code here
```

**Important:** Always include the version reference comment as the first line of every example script. Update the version number to match the current documentation version in `EBS_LANGUAGE_REFERENCE.md`.
