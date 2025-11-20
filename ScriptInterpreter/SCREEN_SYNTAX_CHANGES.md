# Screen Syntax Changes

## Overview

The screen show/hide syntax has been updated to be more intuitive, with the action (show/hide) coming before the screen identifier.

## Old Syntax (Deprecated)

```ebs
// Screen definition
screen myScreen = {
    title: "My Screen",
    width: 800,
    height: 600,
    // ...
};

// Show screen
screen myScreen show;

// Hide screen
screen myScreen hide;
```

## New Syntax

```ebs
// Screen definition (unchanged)
screen myScreen = {
    title: "My Screen",
    width: 800,
    height: 600,
    // ...
};

// Show screen (without parameters)
show screen myScreen;

// Show screen (with parameters)
show screen myScreen(param1, param2);

// Hide screen
hide screen myScreen;
```

## Changes Summary

### Show Screen

**Old:** `screen <name> show;`
**New:** `show screen <name>;` or `show screen <name>(parameters);`

The new syntax:
- Places the action keyword `show` first
- Allows optional parameters in parentheses
- More intuitive and consistent with command patterns

### Hide Screen

**Old:** `screen <name> hide;`
**New:** `hide screen <name>;`

The new syntax:
- Places the action keyword `hide` first
- More intuitive and consistent with command patterns

## Parameters Support

The show screen statement now supports optional parameters:

```ebs
show screen myScreen(x, y, width, height);
show screen myScreen("fullscreen");
show screen myScreen(options);
```

Parameters are passed as a list of expressions and can be evaluated at runtime.

## Parser Changes

- `SHOW` and `HIDE` are now recognized as statement starters
- New parsing methods: `showScreenStatement()` and `hideScreenStatement()`
- `screenStatement()` now only handles screen definition (with `=`)
- `ScreenShowStatement` now accepts optional `List<Expression> parameters`

## Migration Guide

To update existing code:

1. Replace `screen <name> show;` with `show screen <name>;`
2. Replace `screen <name> hide;` with `hide screen <name>;`
3. If needed, add parameters: `show screen <name>(param1, param2);`

## Benefits

- **More Intuitive**: Action (show/hide) comes first, making intent clear
- **Consistent Pattern**: Aligns with other command-style statements
- **Extensible**: Parameters support allows for future enhancements
- **Clearer Separation**: Screen definition vs. screen control are now more distinct

## Implementation Details

The changes maintain backward compatibility at the statement level:
- `ScreenShowStatement` has two constructors (with and without parameters)
- `ScreenHideStatement` remains simple (no parameters)
- Existing interpreter logic continues to work with minimal changes
