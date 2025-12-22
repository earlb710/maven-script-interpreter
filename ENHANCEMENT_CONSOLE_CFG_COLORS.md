# Enhancement: console.cfg Color Profile Support for Syntax Highlighting

## User Request
Use console.cfg selected profile colors for syntax highlighting instead of hardcoded values in CSS.

## Implementation

### Changes Made

#### 1. Added New "function" Color to console.cfg

Added a new color property specifically for custom user-defined functions:

```json
{
  "currentProfile": "default",
  "profiles": {
    "default": {
      "keyword": "#569CD6",
      "builtin": "#DCDCAA",
      "function": "#FFB86C",
      ...
    }
  }
}
```

#### 2. Updated Color Values in console.cfg

Aligned console.cfg colors with the current CSS defaults:
- **keyword**: Changed from `#00FFFF` (cyan) to `#569CD6` (blue) - matches VS Code style
- **builtin**: Changed from `#99e0e0` (light cyan) to `#DCDCAA` (yellow) - matches VS Code style
- **function**: New property set to `#FFB86C` (orange) for custom functions

#### 3. Enhanced ConsoleConfig.java

**Updated Default Colors**:
```java
defaultColors.put("keyword", "#569CD6");
defaultColors.put("builtin", "#DCDCAA");
defaultColors.put("function", "#FFB86C");  // New
```

**Added Syntax Token Mappings in generateCSS()**:

The `generateCSS()` method now generates CSS rules that map console.cfg color properties to the syntax highlighting token classes:

```java
// Maps console.cfg colors to syntax token CSS classes
if (colors.containsKey("keyword")) {
    String keywordColor = colors.get("keyword");
    // Generates CSS for .tok-keyword class
}

if (colors.containsKey("builtin")) {
    String builtinColor = colors.get("builtin");
    // Generates CSS for .tok-builtin class
}

if (colors.containsKey("function")) {
    String functionColor = colors.get("function");
    // Generates CSS for .tok-custom-function class
}
```

This generates CSS like:
```css
/* Syntax highlighting: keywords (from console.cfg 'keyword' color) */
.tok-keyword,
.text.tok-keyword,
.styled-text-area .text.tok-keyword,
.editor-ebs .text.tok-keyword,
.editor-text .text.tok-keyword {
    -fx-fill: #569CD6 !important;
    -fx-font-weight: bold;
}

/* Syntax highlighting: builtins (from console.cfg 'builtin' color) */
.tok-builtin,
.text.tok-builtin,
.styled-text-area .text.tok-builtin,
.editor-ebs .text.tok-builtin,
.editor-text .text.tok-builtin {
    -fx-fill: #DCDCAA !important;
}

/* Syntax highlighting: custom functions (from console.cfg 'function' color) */
.tok-custom-function,
.text.tok-custom-function,
.styled-text-area .text.tok-custom-function,
.editor-ebs .text.tok-custom-function,
.editor-text .text.tok-custom-function {
    -fx-fill: #FFB86C !important;
}
```

## How It Works

1. **Application Startup**: `EbsApp` loads `console.cfg` via `ConsoleConfig`
2. **Profile Loading**: `ConsoleConfig` reads the current profile and extracts colors
3. **CSS Generation**: `generateCSS()` creates CSS rules from the profile colors
4. **CSS Application**: The generated CSS is applied to the application, overriding default styles
5. **Syntax Highlighting**: When highlighting code in `EbsTab`, the `.tok-*` classes use the colors from console.cfg

## Mapping

| console.cfg Property | CSS Class | Used For |
|---------------------|-----------|----------|
| `keyword` | `.tok-keyword` | Language keywords (var, print, call, return, if, then, else, etc.) |
| `builtin` | `.tok-builtin` | Built-in functions (thread.*, string.*, array.*, json.*, etc.) |
| `function` | `.tok-custom-function` | User-defined custom functions |

## Benefits

1. **User Customization**: Users can change syntax highlighting colors by editing console.cfg
2. **Profile Support**: Different color profiles can have different syntax highlighting schemes
3. **Consistency**: All colors come from one configuration file
4. **No CSS Editing**: Users don't need to edit CSS files directly
5. **Dynamic Updates**: Color changes in console.cfg are applied when the application restarts

## Example: Creating a Dark Theme Profile

Users can create custom profiles with their preferred colors:

```json
{
  "currentProfile": "dark-vibrant",
  "profileList": ["default", "dark-vibrant"],
  "profiles": {
    "default": {
      "keyword": "#569CD6",
      "builtin": "#DCDCAA",
      "function": "#FFB86C",
      ...
    },
    "dark-vibrant": {
      "keyword": "#FF79C6",
      "builtin": "#8BE9FD",
      "function": "#FFB86C",
      ...
    }
  }
}
```

To switch profiles, just change `"currentProfile"` to `"dark-vibrant"` and restart the application.

## Commit
Implemented in commit: **c80f802**
