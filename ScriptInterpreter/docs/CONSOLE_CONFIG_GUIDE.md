# Console Configuration Guide

## Overview

The `console.cfg` file is a JSON configuration file that allows you to customize the color scheme of the EBS Console application. It is located in the root directory of the project and is loaded automatically when the console starts.

## File Location

The `console.cfg` file must be placed in the root directory of the maven-script-interpreter project:
```
maven-script-interpreter/
├── console.cfg          <-- Configuration file location
├── ScriptInterpreter/
└── ...
```

## Configuration Format

The configuration file uses JSON format with the following structure:

```json
{
  "colors": {
    "colorName": "colorValue",
    ...
  }
}
```

## Available Color Properties

The following color properties can be configured:

### Text Style Colors
These colors apply to different types of text displayed in the console:

| Property | Default Value | Description |
|----------|---------------|-------------|
| `info` | `#e6e6e6` | Standard informational text |
| `comment` | `#ffffcc` | Comment text |
| `error` | `#ee0000` | Error messages |
| `warn` | `#eeee00` | Warning messages |
| `ok` | `#00ee00` | Success/OK messages |
| `code` | `white` | General code text |
| `datatype` | `#D070FF` | Data type keywords |
| `data` | `pink` | Data values |
| `keyword` | `#00FFFF` | Language keywords |
| `builtin` | `#99e0e0` | Built-in functions |
| `literal` | `blue` | Literal values |
| `identifier` | `white` | Variable/function identifiers |
| `sql` | `#00ee66` | SQL statements |
| `custom` | `#eeee90` | Custom styled text |

### Console Appearance
These properties control the overall appearance of the console:

| Property | Default Value | Description |
|----------|---------------|-------------|
| `background` | `#000000` | Console background color |
| `text` | `#e6e6e6` | Default text color |
| `caret` | `white` | Text cursor color |
| `line-cursor` | `#22292a` | Line cursor background (current line highlight) |
| `line-numbers` | `#808080` | Line number text color |

## Color Value Formats

Color values can be specified in the following formats:

1. **Hexadecimal RGB**: `#RRGGBB` (e.g., `#ff0000` for red)
2. **Named Colors**: CSS color names (e.g., `white`, `blue`, `pink`)
3. **Hexadecimal RGBA**: `#RRGGBBAA` (with alpha channel)

## Example Configurations

### Default Configuration (Dark Theme)
```json
{
  "colors": {
    "info": "#e6e6e6",
    "comment": "#ffffcc",
    "error": "#ee0000",
    "warn": "#eeee00",
    "ok": "#00ee00",
    "code": "white",
    "datatype": "#D070FF",
    "data": "pink",
    "keyword": "#00FFFF",
    "builtin": "#99e0e0",
    "literal": "blue",
    "identifier": "white",
    "sql": "#00ee66",
    "custom": "#eeee90",
    "background": "#000000",
    "text": "#e6e6e6",
    "caret": "white",
    "line-cursor": "#22292a",
    "line-numbers": "#808080"
  }
}
```

### Light Theme Example
```json
{
  "colors": {
    "info": "#333333",
    "comment": "#006600",
    "error": "#cc0000",
    "warn": "#cc6600",
    "ok": "#008800",
    "code": "#000000",
    "datatype": "#9933ff",
    "data": "#cc0066",
    "keyword": "#0066cc",
    "builtin": "#006699",
    "literal": "#0000cc",
    "identifier": "#000000",
    "sql": "#009966",
    "custom": "#996600",
    "background": "#ffffff",
    "text": "#000000",
    "caret": "#000000",
    "line-cursor": "#e0e0e0",
    "line-numbers": "#606060"
  }
}
```

### High Contrast Theme Example
```json
{
  "colors": {
    "info": "#ffffff",
    "comment": "#00ff00",
    "error": "#ff0000",
    "warn": "#ffaa00",
    "ok": "#00ff00",
    "code": "#ffffff",
    "datatype": "#ff00ff",
    "data": "#ff69b4",
    "keyword": "#00ffff",
    "builtin": "#66cccc",
    "literal": "#0088ff",
    "identifier": "#ffffff",
    "sql": "#00ff99",
    "custom": "#ffff00",
    "background": "#000000",
    "text": "#ffffff",
    "caret": "#ffff00",
    "line-cursor": "#333333",
    "line-numbers": "#cccccc"
  }
}
```

## How It Works

1. **Loading**: When the console application starts, it looks for `console.cfg` in the root directory
2. **Parsing**: The JSON configuration is parsed and validated
3. **CSS Generation**: Color properties are converted to JavaFX CSS rules
4. **Application**: The generated CSS is applied as an inline stylesheet, overriding default styles
5. **Fallback**: If the file is missing or invalid, default colors (from console.css) are used

## Troubleshooting

### Configuration Not Loading

If your configuration changes aren't appearing:

1. **Check File Location**: Ensure `console.cfg` is in the project root directory
2. **Verify JSON Syntax**: Use a JSON validator to check for syntax errors
3. **Check Console Output**: Look for error messages about config loading
4. **Restart Application**: Changes require restarting the console application

### Invalid JSON

If you see "Error parsing console.cfg", check for:
- Missing or extra commas
- Unquoted property names
- Missing closing braces
- Invalid escape sequences

### Colors Not Displaying

If colors aren't displaying as expected:
- Verify color values are in a valid format
- Some named colors may not be recognized (use hex values instead)
- Ensure the color name matches exactly (case-sensitive)

## Technical Details

### CSS Class Mapping

The configuration properties map to JavaFX CSS classes:
- Most properties map to `.className` CSS selectors
- `background` applies to console frame elements
- `text` applies to text fill properties
- `caret` applies to the cursor style

### Priority

The configuration CSS is loaded after the default `console.css`, so it will override default styles while preserving other CSS properties not related to colors.

## Related Files

- **EbsApp.java**: Loads and applies the configuration
- **ConsoleConfig.java**: Handles parsing and CSS generation
- **console.css**: Default stylesheet (can be overridden by console.cfg)
