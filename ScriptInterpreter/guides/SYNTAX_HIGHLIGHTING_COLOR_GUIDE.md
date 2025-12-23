# Color Configuration Guide

## Overview

This guide explains how to add colors to the EBS Script Interpreter in two different contexts:

1. **Syntax Highlighting Colors** - For code tokens in the editor/console (keywords, operators, strings, etc.)
2. **Screen JSON Color Properties** - For runtime UI control styling in screen definitions (labels, text fields, buttons, etc.)

These are two independent color systems with different purposes and implementations.

**Related Documentation:**
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - System architecture
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Language syntax

---

## Table of Contents

### Part 1: Syntax Highlighting Colors
1. [Syntax Highlighting System Architecture](#part-1-syntax-highlighting-colors)
2. [Adding a New Highlighting Color](#adding-a-new-highlighting-color)
3. [Complete Highlighting Example](#complete-highlighting-example)
4. [CSS Style Properties](#css-style-properties)
5. [Highlighting Best Practices](#highlighting-best-practices)
6. [Highlighting Troubleshooting](#highlighting-troubleshooting)

### Part 2: Screen JSON Color Properties
7. [Screen JSON Color System Architecture](#part-2-screen-json-color-properties)
8. [Adding a New Screen Color Property](#adding-a-new-screen-color-property)
9. [Complete Screen Color Example](#complete-screen-color-example)
10. [Screen Color Best Practices](#screen-color-best-practices)
11. [Screen Color Troubleshooting](#screen-color-troubleshooting)

---

# Part 1: Syntax Highlighting Colors

## Overview

The syntax highlighting system adds colors to EBS code tokens in the interactive console and editor. The system uses a three-layer architecture:

1. **PrintStyle** - Define semantic style names (Java enum)
2. **TokenType** - Map tokens to styles (token type definitions)
3. **CSS** - Define visual appearance (color, weight, style)

**Scope Note:** Syntax highlighting is **lexer-based token coloring in the UI only**. It does NOT involve the Parser or Interpreter, which handle code execution and semantic analysis. Adding new highlighting colors requires no changes to Parser or Interpreter code.

---

## Syntax Highlighting System Architecture

### Component Overview

The syntax highlighting system consists of:

```
EBS Script Text
      ↓
EbsLexer (tokenizes text)
      ↓
EbsToken (contains type, literal, style)
      ↓
EbsStyled (applies styles to RichTextFX)
      ↓
console.css (visual rendering)
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **PrintStyle** | `src/main/java/com/eb/script/token/PrintStyle.java` | Enum defining semantic style names |
| **EbsTokenType** | `src/main/java/com/eb/script/token/ebs/EbsTokenType.java` | Enum mapping token types to PrintStyle |
| **EbsToken** | `src/main/java/com/eb/script/token/ebs/EbsToken.java` | Token class containing style information |
| **EbsLexer** | `src/main/java/com/eb/script/token/ebs/EbsLexer.java` | Tokenizes text and generates styled tokens |
| **EbsTab** | `src/main/java/com/eb/ui/ebs/EbsTab.java` | Applies lexer-generated styles to editor |
| **EbsStyled** | `src/main/java/com/eb/ui/ebs/EbsStyled.java` | Utility methods for style application |
| **console.css** | `src/main/resources/css/console.css` | CSS definitions for colors and formatting |

**Important Note:** Syntax highlighting is a **lexical/UI concern only**. The Parser and Interpreter are NOT involved in the highlighting process. They handle code execution, not visual styling. Adding new highlighting colors requires no changes to Parser or Interpreter code.

---

## Adding a New Highlighting Color

Follow these three steps to add a new syntax highlighting color:

### Step 1: Add to PrintStyle Enum

**File:** `src/main/java/com/eb/script/token/PrintStyle.java`

Add a new entry to the `PrintStyle` enum with a CSS class name:

```java
public enum PrintStyle {
    // ... existing styles ...
    
    // Your new style
    OPERATOR("operator"),  // CSS class name is "operator"
    
    // ... more styles ...
}
```

**Guidelines:**
- Use a descriptive semantic name (e.g., `OPERATOR`, `ANNOTATION`, `PRAGMA`)
- CSS class name should be lowercase, kebab-case optional (e.g., "operator", "macro-def")
- Group related styles together in the enum

### Step 2: Map Token Types to the New Style

**File:** `src/main/java/com/eb/script/token/ebs/EbsTokenType.java`

When defining token types, specify the PrintStyle in the constructor:

```java
public enum EbsTokenType implements LexerToken {
    // ... existing tokens ...
    
    // Arithmetic operators
    PLUS(Category.OPERATOR, "+"),
    MINUS(Category.OPERATOR, "-"),
    
    // Or with PrintStyle specified:
    PLUS(PrintStyle.OPERATOR, Category.OPERATOR, "+"),
    MINUS(PrintStyle.OPERATOR, Category.OPERATOR, "-"),
    
    // ... more tokens ...
}
```

**Token Type Constructor Options:**

```java
// 1. With style, category, and keywords
KEYWORD(PrintStyle.KEYWORD, Category.KEYWORD, "if", "else", "while")

// 2. With style and data type (for type keywords)
INTEGER(PrintStyle.DATA, DataType.INTEGER, "int", "integer")

// 3. With style only
CUSTOM(PrintStyle.CUSTOM)

// 4. With category only (no style)
SEMICOLON(Category.SYMBOL, ";")

// 5. Keywords only (no style or category)
EOF("\\0")
```

### Step 3: Define CSS Style

**File:** `src/main/resources/css/console.css`

Add CSS rules for your new style class. The style needs to be defined for multiple contexts:

```css
/* Basic style definition */
.operator {
    -fx-fill: #d4d4d4;
    -fx-font-weight: normal;
}

/* Style for all text contexts (required for proper rendering) */
.text.operator,
.styled-text-area .text.operator,
.editor-ebs .text.operator,
.editor-text .text.operator {
    -fx-fill: #d4d4d4;
    -fx-font-weight: normal;
}
```

**Important CSS Notes:**
- Always define both the basic class (`.operator`) and the context-specific classes (`.text.operator`, etc.)
- Use `-fx-fill` for text color (not `-fx-text-fill` in RichTextFX contexts)
- The `.text` selector is necessary for RichTextFX to properly apply styles

---

## Complete Highlighting Example

Let's add syntax highlighting for operators with a custom color.

### Example: Adding OPERATOR Style

#### 1. Add to PrintStyle.java

```java
public enum PrintStyle {
    // ... existing styles ...
    
    CODE("code"),
    DATA("data"),
    DATATYPE("datatype"),
    KEYWORD("keyword"),
    BUILTIN("builtin"),
    IDENTIFIER("identifier"),
    LITERAL("literal"),
    SQL("sql"),
    CUSTOM("custom"),
    OPERATOR("operator"),  // NEW: Add operator style
    
    // ... rest of enum ...
}
```

#### 2. Update EbsTokenType.java

```java
public enum EbsTokenType implements LexerToken {
    // ... existing tokens ...
    
    // Operators - now with PrintStyle.OPERATOR
    PLUS(PrintStyle.OPERATOR, Category.OPERATOR, "+"),
    MINUS(PrintStyle.OPERATOR, Category.OPERATOR, "-"),
    STAR(PrintStyle.OPERATOR, Category.OPERATOR, "*"),
    SLASH(PrintStyle.OPERATOR, Category.OPERATOR, "/"),
    PERCENT(PrintStyle.OPERATOR, Category.OPERATOR, "%", "mod"),
    EQUAL(PrintStyle.OPERATOR, Category.OPERATOR, "="),
    
    // Boolean operators
    BOOL_GT(PrintStyle.OPERATOR, Category.OPERATOR, ">"),
    BOOL_LT(PrintStyle.OPERATOR, Category.OPERATOR, "<"),
    BOOL_EQ(PrintStyle.OPERATOR, Category.OPERATOR, "=="),
    
    // ... more tokens ...
}
```

#### 3. Add CSS in console.css

```css
/* ===== Operator highlighting ===== */
/* Basic operator style */
.operator {
    -fx-fill: #b4b4b4;
    -fx-font-weight: normal;
}

/* Operator style for all editor contexts */
.text.operator,
.styled-text-area .text.operator,
.editor-ebs .text.operator,
.editor-text .text.operator {
    -fx-fill: #b4b4b4;
    -fx-font-weight: normal;
}
```

#### 4. Build and Test

```bash
cd ScriptInterpreter
mvn clean compile
mvn javafx:run
```

In the console, type code with operators to see the highlighting:

```
var x: int = 10 + 20 * 3;
```

The `+`, `*`, and `=` operators should now appear in your custom color (#b4b4b4).

---

## CSS Style Properties

### Available JavaFX CSS Properties for Text

| Property | Description | Example |
|----------|-------------|---------|
| `-fx-fill` | Text color | `-fx-fill: #00FFFF;` |
| `-fx-font-weight` | Font weight | `-fx-font-weight: bold;` |
| `-fx-font-style` | Font style | `-fx-font-style: italic;` |
| `-fx-underline` | Underline text | `-fx-underline: true;` |
| `-fx-strikethrough` | Strike through text | `-fx-strikethrough: true;` |
| `-fx-font-family` | Font family | `-fx-font-family: monospace;` |
| `-fx-font-size` | Font size | `-fx-font-size: 14px;` |

### RichTextFX-Specific Properties

| Property | Description | Example |
|----------|-------------|---------|
| `-rtfx-background-color` | Text background | `-rtfx-background-color: #ffff00;` |
| `-rtfx-underline-color` | Underline color | `-rtfx-underline-color: red;` |
| `-rtfx-underline-dash-array` | Dash pattern | `-rtfx-underline-dash-array: 2 2;` |
| `-rtfx-underline-width` | Underline thickness | `-rtfx-underline-width: 1;` |

### Color Format Options

```css
/* Hex colors */
-fx-fill: #00FFFF;        /* Cyan */
-fx-fill: #f00;           /* Red (short form) */

/* RGB colors */
-fx-fill: rgb(0, 255, 255);

/* RGBA colors (with transparency) */
-fx-fill: rgba(0, 255, 255, 0.8);

/* Named colors */
-fx-fill: cyan;
-fx-fill: red;
-fx-fill: white;
```

### Existing Color Scheme

Current highlighting colors in the console:

| Style | Color | Used For |
|-------|-------|----------|
| **info** | #e6e6e6 (light gray) | Default text, identifiers |
| **comment** | #ffffcc (pale yellow) | Comments |
| **keyword** | #00FFFF (cyan) | Keywords (if, while, var, etc.) |
| **data** | pink | Type keywords (int, string, bool) |
| **datatype** | #D070FF (purple) | Data types |
| **builtin** | #99e0e0 (light cyan) | Built-in functions |
| **sql** | #00ee66 (green) | SQL keywords |
| **custom** | #eeee90 (pale yellow) | Custom console commands |
| **error** | #ee0000 (red) | Error messages |
| **warn** | #eeee00 (yellow) | Warning messages |
| **ok** | #00ee00 (green) | Success messages |

---

## Highlighting Best Practices

### 1. Use Semantic Names

Choose PrintStyle names that describe **what** the token represents, not **how** it looks:

✅ **Good:**
```java
OPERATOR("operator")
ANNOTATION("annotation")
PRAGMA("pragma")
```

❌ **Bad:**
```java
LIGHT_GRAY("light-gray")
BOLD_CYAN("bold-cyan")
```

### 2. Maintain Consistency

- Group related styles together in PrintStyle enum
- Use similar colors for related concepts
- Keep font weights consistent within token categories

### 3. Consider Readability

- Ensure sufficient contrast with background (#000000 black)
- Test colors in actual usage scenarios
- Avoid overly bright or dark colors that strain eyes
- Consider colorblind-friendly color choices

### 4. Document Your Changes

When adding new styles:
- Add comments explaining the purpose
- Update this guide if adding a new category
- Consider adding example scripts demonstrating the highlighting

### 5. Test Thoroughly

```bash
# Build the project
cd ScriptInterpreter
mvn clean compile

# Run the interactive console
mvn javafx:run

# Test with various code samples
# - Simple expressions
# - Complex nested structures
# - Edge cases (strings, comments, etc.)
```

### 6. CSS Organization

Keep console.css organized:
- Group related styles together
- Use comments to mark sections
- Follow existing patterns for context-specific selectors

---

## Highlighting Troubleshooting

### Problem: Style Not Appearing

**Symptoms:** Text shows default color instead of new color

**Solutions:**

1. **Check all three layers are in place:**
   ```bash
   # Verify PrintStyle has your new style
   grep "YOUR_STYLE" src/main/java/com/eb/script/token/PrintStyle.java
   
   # Verify TokenType uses the style
   grep "YOUR_STYLE" src/main/java/com/eb/script/token/ebs/EbsTokenType.java
   
   # Verify CSS has the class
   grep "your-style" src/main/resources/css/console.css
   ```

2. **Rebuild the project:**
   ```bash
   cd ScriptInterpreter
   mvn clean compile
   ```

3. **Check CSS class name matches:**
   - PrintStyle: `OPERATOR("operator")`
   - CSS: `.operator` and `.text.operator`

### Problem: Color Shows in Console but Not Editor

**Symptoms:** Highlighting works in console output but not in the editor area

**Solution:** Add context-specific CSS selectors:

```css
/* Add these context selectors */
.editor-ebs .text.your-style,
.editor-text .text.your-style {
    -fx-fill: #your-color;
}
```

### Problem: Style Partially Applied

**Symptoms:** Some tokens get the style, others don't

**Solutions:**

1. Check token type definitions - ensure all relevant tokens map to the style
2. Look for token overlaps - one token might be styled differently
3. Verify lexer is generating correct token types

### Problem: Bold or Italic Not Working

**Symptoms:** Color works but font weight/style doesn't change

**Solution:** Check the CSS specificity. RichTextFX contexts may need explicit rules:

```css
.text.your-style,
.styled-text-area .text.your-style,
.editor-ebs .text.your-style {
    -fx-fill: #color;
    -fx-font-weight: bold;     /* Make sure this is included */
    -fx-font-style: italic;    /* And this if needed */
}
```

### Problem: Console Commands Not Highlighted

**Symptoms:** Custom console commands (like `/open`, `/help`) not styled

**Solution:** Console commands use a custom lexer. Check `EbsStyled.java`:

```java
static {
    // Custom keywords for console commands
    lexerConsole.addCustomKeywords("custom", "echo", "debug", "list", 
                                    "open", "close", "help", "clear", "reset");
    lexerConsole.addCustomChar("custom", '?');
}
```

Add your command to the `addCustomKeywords` call if needed.

### Debug Mode

Enable token debugging to see what styles are being applied:

```java
// In EbsStyled.java, temporarily add debug output
for (EbsToken tok : tokens) {
    System.out.println("Token: " + tok.type + ", Style: " + tok.style + 
                       ", Text: " + tok.literal);
}
```

---

## Advanced Topics

### Custom Token Styles

Sometimes you need a style that isn't determined by token type alone. Use the custom style constructor:

```java
// In EbsLexer.java or similar
EbsToken token = new EbsToken(
    EbsTokenType.IDENTIFIER,  // type
    "myVariable",              // literal
    lineNumber,                // line
    startPos,                  // start
    endPos,                    // end
    "custom-identifier"        // custom style override
);
```

### Dynamic Style Application

For highlighting that changes based on context (e.g., matching brackets):

```java
// Example from bracket matching in EbsTab.java
textArea.setStyleClass(start, end, "bracket-match");
textArea.setStyleClass(otherStart, otherEnd, "bracket-match");
```

### Console-Specific Styling

The console uses two lexer instances:
- `lexer` - for general EBS code
- `lexerConsole` - for console commands

Customize console command highlighting:

```java
lexerConsole.addCustomKeywords("custom", "newCommand");
```

---

## Summary

To add a new syntax highlighting color:

1. **PrintStyle.java** - Add semantic enum value
2. **EbsTokenType.java** - Map token types to the style
3. **console.css** - Define colors and formatting

**Remember:**
- Use semantic names
- Test thoroughly
- Maintain consistency
- Document your changes

---

# Part 2: Screen JSON Color Properties

## Overview

Screen JSON color properties control the visual styling of UI controls in EBS screen definitions at runtime. These properties allow you to customize the appearance of labels, text fields, buttons, and other JavaFX controls by specifying colors in the screen JSON files.

**Scope Note:** Screen JSON colors are **runtime UI styling properties** defined in screen definition JSON files. They are completely separate from syntax highlighting and involve the Interpreter, screen parsing, and UI rendering components.

---

## Screen JSON Color System Architecture

### Component Overview

The screen JSON color system consists of:

```
Screen JSON Definition
      ↓
InterpreterScreen (parses JSON)
      ↓
DisplayItem/AreaItem (data structures with color fields)
      ↓
ScreenFactory/AreaItemFactory (creates JavaFX controls)
      ↓
JavaFX Controls (visual rendering)
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **display-metadata.json** | `src/main/resources/json/display-metadata.json` | JSON schema defining available color properties |
| **DisplayItem** | `src/main/java/com/eb/script/interpreter/screen/DisplayItem.java` | Data structure holding display metadata including colors |
| **AreaItem** | `src/main/java/com/eb/script/interpreter/screen/AreaItem.java` | Data structure for area items with color properties |
| **InterpreterScreen** | `src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java` | Parses JSON and extracts color properties |
| **ScreenFactory** | `src/main/java/com/eb/script/interpreter/screen/ScreenFactory.java` | Creates screens and applies color properties to controls |
| **AreaItemFactory** | `src/main/java/com/eb/script/interpreter/screen/AreaItemFactory.java` | Creates UI controls and applies styling |

### Existing Color Properties

The system currently supports these color properties:

**Label Colors:**
- `labelColor` - Label text color
- `labelBackgroundColor` - Label background color

**Control/Item Colors:**
- `textColor` - Item text color (preferred, consistent with labelColor)
- `itemColor` - Item text color (alternative to textColor)
- `backgroundColor` - Item background color

**Area Colors:**
- `areaBackground` - Container area background color
- `groupBorderColor` - Group border color
- `groupLabelColor` - Group label text color
- `groupLabelBackground` - Group label background color

---

## Adding a New Screen Color Property

Follow these four steps to add a new screen color property:

### Step 1: Add to JSON Schema

**File:** `src/main/resources/json/display-metadata.json`

Add the new property to the schema with appropriate description:

```json
{
  "properties": {
    "existingProperty": { ... },
    
    "newColorProperty": {
      "type": "string",
      "description": "Description of what this color controls (e.g., '#FF0000', 'red')"
    }
  }
}
```

**Guidelines:**
- Use descriptive property names (camelCase convention)
- Include "Color" or "Background" in the name for clarity
- Provide clear description with example values
- Specify `"type": "string"` for color properties

### Step 2: Add Field to Data Structure

**File:** `src/main/java/com/eb/script/interpreter/screen/DisplayItem.java` (for display metadata)
or `src/main/java/com/eb/script/interpreter/screen/AreaItem.java` (for area items)

Add a field to hold the color property:

```java
public class DisplayItem {
    // Existing fields...
    String labelColor;
    String itemColor;
    
    // New field
    String newColorProperty;
    
    // Update toString() method to include new field
    @Override
    public String toString() {
        return "DisplayItem{" +
               // ... existing fields ...
               ", newColorProperty='" + newColorProperty + '\'' +
               '}';
    }
}
```

**Guidelines:**
- Use `String` type for color values
- Follow existing naming conventions
- Update `toString()` method for debugging
- Add JavaDoc comment describing the field

### Step 3: Parse Property from JSON

**File:** `src/main/java/com/eb/script/interpreter/screen/InterpreterScreen.java`

Add parsing logic to extract the property from JSON:

```java
// In parseDisplayMetadata() or similar method

// Extract newColorProperty (case-insensitive)
if (displayDef.containsKey("newColorProperty")) {
    metadata.newColorProperty = String.valueOf(displayDef.get("newColorProperty"));
    // Optional: substitute variables if needed
    try {
        metadata.newColorProperty = substituteVariablesInStyle(metadata.newColorProperty, line);
    } catch (Exception e) {
        System.err.println("Warning: Could not substitute variables in newColorProperty: " + e.getMessage());
    }
} else if (displayDef.containsKey("newcolorproperty")) {
    // Handle lowercase variant
    metadata.newColorProperty = String.valueOf(displayDef.get("newcolorproperty"));
} else if (displayDef.containsKey("new_color_property")) {
    // Handle snake_case variant
    metadata.newColorProperty = String.valueOf(displayDef.get("new_color_property"));
}
```

**Guidelines:**
- Support case-insensitive property names (camelCase, lowercase, snake_case)
- Use `String.valueOf()` to safely convert values
- Add to validation sets if property should be validated
- Handle variable substitution if the property supports EBS variables

### Step 4: Apply Property to UI Control

**File:** `src/main/java/com/eb/script/interpreter/screen/AreaItemFactory.java` or `ScreenFactory.java`

Apply the color to the JavaFX control:

```java
// In applyItemStyle() or similar method

if (metadata.newColorProperty != null && !metadata.newColorProperty.isEmpty()) {
    // Build CSS style string
    String colorStyle = "-fx-new-property: " + metadata.newColorProperty + ";";
    
    // Apply to control
    String currentStyle = control.getStyle();
    if (currentStyle == null || currentStyle.isEmpty()) {
        control.setStyle(colorStyle);
    } else {
        control.setStyle(currentStyle + " " + colorStyle);
    }
}
```

**JavaFX CSS Properties:**
- `-fx-text-fill` - Text color
- `-fx-background-color` - Background color
- `-fx-border-color` - Border color
- `-fx-prompt-text-fill` - Placeholder text color

**Guidelines:**
- Check for null/empty before applying
- Append to existing style rather than replacing
- Use appropriate JavaFX CSS property name
- Test with various control types

---

## Complete Screen Color Example

Let's add a new `placeholderColor` property for controlling prompt text color in text fields.

### Example: Adding placeholderColor Property

#### 1. Add to display-metadata.json

```json
{
  "properties": {
    "promptHelp": {
      "type": "string",
      "description": "Placeholder text for text inputs (hint shown in empty field)"
    },
    "placeholderColor": {
      "type": "string",
      "description": "Color for placeholder/prompt text (e.g., '#888888', 'gray')"
    }
  }
}
```

#### 2. Add Field to DisplayItem.java

```java
public class DisplayItem {
    // Existing color fields
    String labelColor;
    String labelBackgroundColor;
    String itemColor;
    String textColor;
    
    // NEW: Placeholder color field
    String placeholderColor;
    
    @Override
    public String toString() {
        return "DisplayItem{" +
               "type=" + type +
               ", labelColor='" + labelColor + '\'' +
               ", itemColor='" + itemColor + '\'' +
               ", textColor='" + textColor + '\'' +
               ", placeholderColor='" + placeholderColor + '\'' +  // NEW
               // ... other fields ...
               '}';
    }
}
```

#### 3. Parse in InterpreterScreen.java

```java
// In parseDisplayMetadata() method, after other color parsing

// Extract placeholderColor property
if (displayDef.containsKey("placeholderColor")) {
    metadata.placeholderColor = String.valueOf(displayDef.get("placeholderColor"));
    try {
        metadata.placeholderColor = substituteVariablesInStyle(metadata.placeholderColor, line);
    } catch (Exception e) {
        System.err.println("Warning: Could not substitute variables in placeholderColor: " + e.getMessage());
    }
} else if (displayDef.containsKey("placeholdercolor")) {
    metadata.placeholderColor = String.valueOf(displayDef.get("placeholdercolor"));
} else if (displayDef.containsKey("placeholder_color")) {
    metadata.placeholderColor = String.valueOf(displayDef.get("placeholder_color"));
}

// Add to property validation set (if using validation)
private static final Set<String> VALID_DISPLAY_PROPERTIES = Set.of(
    "labelcolor", "label_color",
    "itemcolor", "item_color",
    "textcolor", "text_color",
    "placeholdercolor", "placeholder_color",  // NEW
    // ... other properties
);
```

#### 4. Apply in AreaItemFactory.java

```java
// In applyDisplayProperties() or applyItemStyle() method

// Apply placeholder text color
if (metadata.placeholderColor != null && !metadata.placeholderColor.isEmpty()) {
    String promptStyle = "-fx-prompt-text-fill: " + metadata.placeholderColor + ";";
    
    String currentStyle = control.getStyle();
    if (currentStyle == null || currentStyle.isEmpty()) {
        control.setStyle(promptStyle);
    } else {
        control.setStyle(currentStyle + " " + promptStyle);
    }
}
```

#### 5. Use in Screen JSON

```json
{
  "name": "LoginScreen",
  "vars": [
    {
      "name": "username",
      "type": "string",
      "display": {
        "type": "textfield",
        "promptHelp": "Enter username",
        "placeholderColor": "#999999"
      }
    }
  ]
}
```

#### 6. Build and Test

```bash
cd ScriptInterpreter
mvn clean compile
mvn javafx:run
```

Load a screen that uses the new `placeholderColor` property and verify the placeholder text appears in the specified color.

---

## Screen Color Best Practices

### 1. Use Semantic Property Names

Choose property names that clearly describe what they control:

✅ **Good:**
```
placeholderColor
borderHighlightColor
disabledTextColor
```

❌ **Bad:**
```
color1
textColor2
customColor
```

### 2. Support Case Variations

Always handle multiple case variations in parsing:

```java
// Support camelCase, lowercase, and snake_case
if (displayDef.containsKey("myNewColor")) { ... }
else if (displayDef.containsKey("mynewcolor")) { ... }
else if (displayDef.containsKey("my_new_color")) { ... }
```

### 3. Provide Sensible Defaults

Don't force users to specify every color:

```java
// Use default if not specified
String borderColor = (metadata.borderColor != null && !metadata.borderColor.isEmpty()) 
                    ? metadata.borderColor 
                    : "#cccccc";  // Default gray border
```

### 4. Document Color Format

In JSON schema, always show example formats:

```json
{
  "description": "Border color (e.g., '#FF0000', 'red', 'rgb(255,0,0)')"
}
```

### 5. Test with Various Controls

Color properties may behave differently on different control types:
- Test with TextField, TextArea, Label, Button, etc.
- Verify color appears correctly in different themes
- Check disabled state rendering

### 6. Consider Accessibility

- Ensure sufficient contrast for readability
- Test with colorblind-friendly palettes
- Provide high-contrast alternatives

### 7. Update Schema Documentation

When adding properties, update:
- JSON schema file (`display-metadata.json`)
- Example screens in schema
- Any guide documents referencing screen properties

---

## Screen Color Troubleshooting

### Problem: Color Property Not Applied

**Symptoms:** Color specified in JSON doesn't appear on the control

**Solutions:**

1. **Verify JSON syntax:**
   ```bash
   # Check JSON is valid
   cat screen-definition.json | python -m json.tool
   ```

2. **Check parsing:**
   Add debug output in `InterpreterScreen.java`:
   ```java
   if (displayDef.containsKey("myColor")) {
       metadata.myColor = String.valueOf(displayDef.get("myColor"));
       System.out.println("DEBUG: Parsed myColor = " + metadata.myColor);
   }
   ```

3. **Verify field is set:**
   Check `toString()` output includes your property

4. **Check CSS property name:**
   Ensure you're using the correct JavaFX CSS property:
   - Text: `-fx-text-fill`
   - Background: `-fx-background-color`
   - Border: `-fx-border-color`

### Problem: Color Only Works in Lowercase

**Symptoms:** `myColor` doesn't work but `mycolor` does

**Solution:** Add case-insensitive parsing:

```java
if (displayDef.containsKey("myColor")) {
    metadata.myColor = String.valueOf(displayDef.get("myColor"));
} else if (displayDef.containsKey("mycolor")) {
    metadata.myColor = String.valueOf(displayDef.get("mycolor"));
}
```

### Problem: Color Not Applied to Specific Control Type

**Symptoms:** Color works on TextField but not on Label

**Solution:** Some controls need special handling:

```java
// Apply text color based on control type
if (control instanceof TextField || control instanceof TextArea) {
    control.setStyle("-fx-text-fill: " + color);
} else if (control instanceof Label) {
    ((Label) control).setTextFill(Color.web(color));
} else if (control instanceof Button) {
    control.setStyle("-fx-text-fill: " + color);
}
```

### Problem: Variable Substitution Not Working

**Symptoms:** `$COLOR_PRIMARY` appears literally instead of being replaced

**Solution:** Ensure variable substitution is called:

```java
try {
    metadata.myColor = substituteVariablesInStyle(metadata.myColor, line);
} catch (Exception e) {
    System.err.println("Warning: Could not substitute variables: " + e.getMessage());
}
```

### Problem: Color Property Appears in Validation Error

**Symptoms:** "Unknown property: myColor" warning in console

**Solution:** Add property to validation set:

```java
private static final Set<String> VALID_DISPLAY_PROPERTIES = Set.of(
    "labelcolor", "itemcolor", "textcolor",
    "mycolor", "my_color",  // Add new property
    // ... other properties
);
```

### Debug Mode

Enable detailed logging for screen parsing:

```java
// In InterpreterScreen.java
private static final boolean DEBUG = true;

if (DEBUG) {
    System.out.println("Parsing display metadata:");
    System.out.println("  Properties: " + displayDef.keySet());
    System.out.println("  labelColor: " + metadata.labelColor);
    System.out.println("  myColor: " + metadata.myColor);
}
```

---

## Summary

This guide covers two independent color systems in the EBS Script Interpreter:

### Part 1: Syntax Highlighting Colors
- **Purpose:** Color code tokens in the editor/console
- **Components:** PrintStyle enum → TokenType → CSS
- **Files:** PrintStyle.java, EbsTokenType.java, console.css
- **Scope:** Lexer and UI only (no Parser/Interpreter)

### Part 2: Screen JSON Color Properties
- **Purpose:** Style UI controls in screen definitions
- **Components:** JSON Schema → Parse → Data Structure → Apply to JavaFX
- **Files:** display-metadata.json, InterpreterScreen.java, DisplayItem.java, AreaItemFactory.java
- **Scope:** Interpreter screen parsing and UI rendering

For more information, see:
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - Overall system design
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Language syntax
- [RichTextFX Documentation](https://github.com/FXMisc/RichTextFX) - Text area library

---

## Appendix: File Reference

### Part 1: Syntax Highlighting Files

```
ScriptInterpreter/
├── src/main/java/com/eb/
│   ├── script/token/
│   │   ├── PrintStyle.java           # Step 1: Add style enum
│   │   └── ebs/
│   │       ├── EbsTokenType.java     # Step 2: Map tokens to style
│   │       ├── EbsToken.java         # Token with style field
│   │       └── EbsLexer.java         # Generates tokens
│   └── ui/ebs/
│       └── EbsStyled.java            # Applies styles to UI
└── src/main/resources/css/
    └── console.css                    # Step 3: Define CSS colors
```

### Part 2: Screen JSON Color Files

```
ScriptInterpreter/
├── src/main/java/com/eb/script/interpreter/screen/
│   ├── InterpreterScreen.java        # Step 3: Parse JSON properties
│   ├── DisplayItem.java              # Step 2: Add field to data structure
│   ├── AreaItem.java                 # Step 2: Add field for area items
│   ├── ScreenFactory.java            # Step 4: Apply to controls
│   └── AreaItemFactory.java          # Step 4: Apply styling
└── src/main/resources/json/
    └── display-metadata.json          # Step 1: Add to schema
```

---

*Last updated: 2025-12-23*
