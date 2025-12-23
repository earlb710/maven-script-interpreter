# Color Configuration Guide

## Overview

This guide explains how to add colors to the EBS Script Interpreter in three different contexts:

1. **Syntax Highlighting Colors** - For code tokens in the editor/console (keywords, operators, strings, etc.)
2. **Screen JSON Color Properties** - For runtime UI control styling in screen definitions (labels, text fields, buttons, etc.)
3. **Color Editor Screen** - For console configuration colors managed through the color_editor.ebs script

These are three independent color systems with different purposes and implementations.

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

### Part 3: Color Editor Screen
12. [Color Editor System Architecture](#part-3-color-editor-screen)
13. [Adding a New Color to the Editor](#adding-a-new-color-to-the-editor)
14. [Complete Color Editor Example](#complete-color-editor-example)
15. [How Color Editor Affects Syntax Highlighting](#how-color-editor-affects-syntax-highlighting)
16. [Color Editor Best Practices](#color-editor-best-practices)
17. [Color Editor Troubleshooting](#color-editor-troubleshooting)

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

# Part 3: Color Editor Screen

## Overview

The Color Editor Screen is an EBS script (`color_editor.ebs`) that provides a graphical interface for managing console configuration colors. It reads colors from `console.cfg` and allows users to edit them through a JavaFX screen with color pickers.

**Scope Note:** This system is for **console configuration colors** that control the appearance of the editor interface. Colors are stored in `console.cfg` and loaded at startup. This is different from syntax highlighting (Part 1) and screen JSON properties (Part 2).

---

## Color Editor System Architecture

### Component Overview

The color editor system consists of:

```
console.cfg (JSON config file)
      ↓
color_editor.ebs (EBS script)
      ↓
EBS Screen with ColorPickers
      ↓
User Edits Colors
      ↓
Save to console.cfg
      ↓
ConsoleConfig.java reloads config
      ↓
Colors applied to console UI
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **console.cfg** | Repository root | JSON config file storing color profiles |
| **color_editor.ebs** | `src/main/resources/scripts/color_editor.ebs` | EBS script that creates the color editor screen |
| **ConsoleConfig.java** | `src/main/java/com/eb/ui/ebs/ConsoleConfig.java` | Java class that loads and applies colors from config |
| **console.css** | `src/main/resources/css/console.css` | CSS file with default color values |

### Console Configuration Colors

The `console.cfg` file contains color profiles with these properties:

**Syntax Highlighting Colors:**
- `info`, `comment`, `error`, `warn`, `ok`
- `code`, `datatype`, `data`, `keyword`, `builtin`, `literal`, `identifier`, `sql`, `custom`

**Editor Colors:**
- `background`, `text`, `caret`, `line-cursor`, `line-numbers`
- `console-background`

**Tab Colors:**
- `tab-background`, `tab-label-color`, `tab-label-changed-color`
- `tab-label-background`, `tab-select`, `tab-content`

**Find Colors:**
- `find-highlight-color`, `find-highlight-background`, `current-find-highlight-bg`

---

## Adding a New Color to the Editor

Follow these seven steps to add a new color to the color editor screen:

### Step 1: Add Default Value in color_editor.ebs

**File:** `src/main/resources/scripts/color_editor.ebs`

Add a default color constant (around line 370-400):

```javascript
// Existing defaults
var defaultInfoColor: string = "#e6e6e6";
var defaultCommentColor: string = "#ffffcc";

// NEW: Add your default color
var defaultMyNewColor: string = "#ff00ff";
```

### Step 2: Add Variable Declaration

Add a variable to hold the color value (around line 340-370):

```javascript
// Existing color variables
var infoColor: string = call json.getstring(colors, "info", "");
var commentColor: string = call json.getstring(colors, "comment", "");

// NEW: Add your color variable
var myNewColor: string = call json.getstring(colors, "myNewColor", "");
```

### Step 3: Add Default Fallback

Add fallback logic to use default if not present (around line 402-431):

```javascript
// Existing fallbacks
if infoColor == "" then { infoColor = defaultInfoColor; }
if commentColor == "" then { commentColor = defaultCommentColor; }

// NEW: Add fallback for your color
if myNewColor == "" then { myNewColor = defaultMyNewColor; }
```

### Step 4: Add to setColorsToDefaults Function

Update the function that resets colors (around line 438-468):

```javascript
function setColorsToDefaults {
    colorEditorScreen.info = defaultInfoColor;
    colorEditorScreen.comment = defaultCommentColor;
    // ... existing colors ...
    
    // NEW: Add your color
    colorEditorScreen.myNewColor = defaultMyNewColor;
}
```

### Step 5: Add to setColorsToOriginal Function

Update the function that restores loaded colors (around line 470-501):

```javascript
function setColorsToOriginal {
    colorEditorScreen.info = infoColor;
    colorEditorScreen.comment = commentColor;
    // ... existing colors ...
    
    // NEW: Add your color
    colorEditorScreen.myNewColor = myNewColor;
}
```

### Step 6: Add to buildProfileColorsJson Function

Update JSON building functions (around line 504-573):

```javascript
function buildProfileColorsJson(pName: string) return string {
    var colorsJson: string = "\"" + pName + "\": {\n";
    colorsJson = colorsJson + "      \"info\": \"" + colorEditorScreen.info + "\",\n";
    // ... existing colors ...
    
    // NEW: Add your color (note the comma on the previous line)
    colorsJson = colorsJson + "      \"myNewColor\": \"" + colorEditorScreen.myNewColor + "\"\n";
    colorsJson = colorsJson + "    }";
    return colorsJson;
}
```

**Important:** Remove the comma from what was the last line and add it to your new line if adding at the end.

### Step 7: Add ColorPicker to Screen Definition

Add a variable and UI control to the screen (around line 1568-1846):

```javascript
screen colorEditorScreen = {
    "title": "Console Configuration Color Editor",
    "vars": [
        {
            "name": "info",
            "type": "string",
            "default": $infoColor,
            "display": { "type": "colorpicker", "labelText": "Info Color:" }
        },
        // ... existing color variables ...
        
        // NEW: Add your color variable
        {
            "name": "myNewColor",
            "type": "string",
            "default": $myNewColor,
            "display": {
                "type": "colorpicker",
                "labelText": "My New Color:"
            }
        }
    ],
    "area": [{
        // ... screen layout ...
        "items": [
            {"name": "info_item", "varref": "info"},
            // ... existing items ...
            
            // NEW: Add your color item to appropriate group
            {"name": "myNewColor_item", "varref": "myNewColor"}
        ]
    }]
};
```

**Note:** Add the item to the appropriate group area (consoleColorsArea, syntaxColorsArea, tabColorsArea, or editorColorsArea) based on what the color controls.

---

## Complete Color Editor Example

Let's add a new `selection-background` color for text selection highlighting.

### Example: Adding Selection Background Color

#### 1. Add Default Value (line ~400)

```javascript
var defaultSelectionBackground: string = "#3366cc";
```

#### 2. Add Variable Declaration (line ~368)

```javascript
var selectionBackground: string = call json.getstring(colors, "selection-background", "");
```

#### 3. Add Default Fallback (line ~431)

```javascript
if selectionBackground == "" then { selectionBackground = defaultSelectionBackground; }
```

#### 4. Add to setColorsToDefaults (line ~468)

```javascript
function setColorsToDefaults {
    // ... existing assignments ...
    colorEditorScreen.selectionBackground = defaultSelectionBackground;
}
```

#### 5. Add to setColorsToOriginal (line ~501)

```javascript
function setColorsToOriginal {
    // ... existing assignments ...
    colorEditorScreen.selectionBackground = selectionBackground;
}
```

#### 6. Add to buildProfileColorsJson (line ~534)

```javascript
function buildProfileColorsJson(pName: string) return string {
    // ... existing JSON building ...
    colorsJson = colorsJson + "      \"tab-content\": \"" + colorEditorScreen.tabContent + "\",\n";
    colorsJson = colorsJson + "      \"selection-background\": \"" + colorEditorScreen.selectionBackground + "\"\n";  // NEW (last item, no comma)
    colorsJson = colorsJson + "    }";
    return colorsJson;
}
```

#### 7. Add to buildDefaultProfileColorsJson (line ~570)

```javascript
function buildDefaultProfileColorsJson(pName: string) return string {
    // ... existing JSON building ...
    colorsJson = colorsJson + "      \"tab-content\": \"" + defaultTabContentColor + "\",\n";
    colorsJson = colorsJson + "      \"selection-background\": \"" + defaultSelectionBackground + "\"\n";  // NEW
    colorsJson = colorsJson + "    }";
    return colorsJson;
}
```

#### 8. Add to saveConfigWithCurrentProfile (line ~794)

Find the section where colors are assigned from screen values (around line 764-794) and from config (around line 796-826):

```javascript
// When saving current profile (line ~794)
pSelectionBg = colorEditorScreen.selectionBackground;

// When reading from config (line ~826)
pSelectionBg = call json.getstring(pColors, "selection-background", "");
```

And in the JSON building section (around line ~857):

```javascript
profilesStr = profilesStr + "      \"tab-content\": \"" + pTabContent + "\",\n";
profilesStr = profilesStr + "      \"selection-background\": \"" + pSelectionBg + "\"\n";  // NEW
profilesStr = profilesStr + "    }";
```

#### 9. Add ColorPicker to Screen (line ~1846)

```javascript
{
    "name": "selectionBackground",
    "type": "string",
    "default": $selectionBackground,
    "display": {
        "type": "colorpicker",
        "labelText": "Selection Background:"
    }
}
```

And add the item to the appropriate area (e.g., editorColorsArea around line ~1999):

```javascript
"items": [
    {"name": "background_item", "varref": "background"},
    {"name": "text_item", "varref": "text"},
    {"name": "selectionBackground_item", "varref": "selectionBackground"}  // NEW
]
```

#### 10. Update ConsoleConfig.java

**File:** `src/main/java/com/eb/ui/ebs/ConsoleConfig.java`

Add code to read and apply the new color:

```java
// In loadConfig() method
String selectionBg = getColorValue(colors, "selection-background", "#3366cc");

// In applyColors() method
// Apply to relevant UI components
textArea.setStyle("-fx-selection-bar: " + selectionBg + ";");
```

#### 11. Test

```bash
cd ScriptInterpreter
mvn clean compile
mvn javafx:run

# In the console, run:
/open color_editor.ebs

# Verify your new color appears in the editor
# Change the color and click "Save and Apply"
# Verify the color is saved to console.cfg
```

---

## How Color Editor Affects Syntax Highlighting

**Important Connection:** The Color Editor Screen directly controls syntax highlighting colors through the "Apply" mechanism.

### The Apply Process

When you click **"Save and Apply"** in the color editor:

1. **Save**: Colors are saved to `console.cfg` JSON file
2. **Apply**: `system.reloadConfig()` is called, which triggers:
   - `EbsApp.reloadConfig()` - Java method that handles the reload
   - `ConsoleConfig` instance created - Reads colors from `console.cfg`
   - `generateCSS()` - Generates CSS rules from the color configuration
   - CSS written to file and applied to the UI

### Syntax Highlighting Integration

The generated CSS includes rules for **all syntax highlighting classes** defined in Part 1:

```css
/* Generated from console.cfg */

.keyword,
.text.keyword,
.styled-text-area .text.keyword,
.console-out .text.keyword,
.editor-ebs .text.keyword {
    -fx-fill: #00FFFF !important;
    -fx-background-color: transparent !important;
}

.builtin,
.text.builtin,
.styled-text-area .text.builtin {
    -fx-fill: #99e0e0 !important;
    -fx-background-color: transparent !important;
}
```

### Color Mapping

Colors from `console.cfg` map to syntax highlighting classes:

| console.cfg Property | Syntax Highlighting Class | Part 1 PrintStyle |
|---------------------|---------------------------|-------------------|
| `keyword` | `.keyword` | `KEYWORD` |
| `builtin` | `.builtin` | `BUILTIN` |
| `literal` | `.literal` | `LITERAL` |
| `datatype` | `.datatype` | `DATATYPE` |
| `identifier` | `.identifier` | `IDENTIFIER` |
| `comment` | `.comment` | `COMMENT` |
| `error` | `.error` | `ERROR` |
| `info` | `.info` | `INFO` |
| `sql` | `.sql` | `SQL` |
| `custom` | `.custom` | `CUSTOM` |

### Complete Flow Diagram

```text
User edits color in Color Editor
        ↓
Click "Save and Apply"
        ↓
color_editor.ebs saves to console.cfg
        ↓
system.reloadConfig() called
        ↓
EbsApp.reloadConfig()
        ↓
ConsoleConfig reads console.cfg
        ↓
generateCSS() creates CSS rules
        ↓
CSS written to file
        ↓
CSS applied to UI with !important
        ↓
Syntax highlighting colors updated immediately
```

### Why This Matters

1. **Runtime Updates**: Colors change immediately without restarting the application
2. **Persistence**: Color changes are saved and persist across sessions
3. **Profile Management**: Multiple color profiles can be managed and switched
4. **Unified System**: One config file controls both UI colors and syntax highlighting

### Adding New Syntax Highlight Colors

When adding a new syntax highlighting color (Part 1), you should also:

1. **Add to color_editor.ebs** (Part 3):
   - Add the color variable and ColorPicker
   - Add to all profile functions
   - Users can then customize the color through the GUI

2. **Update ConsoleConfig.java**:
   - The `generateCSS()` method automatically handles standard class names
   - Custom handling only needed for special cases (like `background`, `caret`)

3. **Default in console.css**:
   - Provide a default color in `console.css` for first-time users
   - The color editor will override this when users customize

### Example: Adding a New "operator" Color

**Part 1** (Syntax Highlighting):
```java
// PrintStyle.java
OPERATOR("operator")

// EbsTokenType.java
PLUS(PrintStyle.OPERATOR, Category.OPERATOR, "+")

// console.css (default)
.text.operator { -fx-fill: #b4b4b4; }
```

**Part 3** (Color Editor):
```javascript
// color_editor.ebs
var defaultOperatorColor: string = "#b4b4b4";
var operatorColor: string = call json.getstring(colors, "operator", "");

// Add to screen vars
{
    "name": "operator",
    "type": "string",
    "default": $operatorColor,
    "display": {"type": "colorpicker", "labelText": "Operator Color:"}
}
```

**ConsoleConfig.java** (automatic):
```java
// No changes needed! generateCSS() automatically creates:
// .operator, .text.operator, .styled-text-area .text.operator { -fx-fill: <color> !important; }
```

When users click "Save and Apply", the operator color from the editor overrides the default in console.css.

---

## Color Editor Best Practices

### 1. Follow Naming Conventions

Use kebab-case for color names in JSON:

✅ **Good:**
```
"selection-background"
"error-highlight"
"tab-hover-color"
```

❌ **Bad:**
```
"SelectionBackground"
"error_highlight"
"tabHoverColor"
```

### 2. Provide Sensible Defaults

Choose defaults that work well with the existing color scheme:

```javascript
// Consider contrast and readability
var defaultSelectionBg: string = "#3366cc";  // Blue, visible on dark backgrounds
var defaultErrorHighlight: string = "#ff4444";  // Red, but not too bright
```

### 3. Add Colors to Appropriate Groups

Place your color picker in the logical group:
- **Console Colors** - Message types (info, error, warn, ok)
- **Syntax Colors** - Code element types
- **Tab Colors** - Tab bar and label colors
- **Editor Colors** - Text area and cursor colors
- **Find Colors** - Search highlighting colors

### 4. Update All Profile Functions

Colors must be added to:
- `setColorsToDefaults()`
- `setColorsToOriginal()`
- `buildProfileColorsJson()`
- `buildDefaultProfileColorsJson()`
- `saveConfigWithCurrentProfile()` (multiple places)
- `onProfileChange()` (if you want profile-specific loading)

Missing any of these will cause issues when switching profiles or saving.

### 5. Handle Comma Placement in JSON

When building JSON strings, the last property should NOT have a trailing comma:

```javascript
// Correct
colorsJson = colorsJson + "      \"tab-content\": \"" + color1 + "\",\n";
colorsJson = colorsJson + "      \"new-color\": \"" + color2 + "\"\n";  // No comma

// Incorrect
colorsJson = colorsJson + "      \"new-color\": \"" + color2 + "\",\n";  // Trailing comma!
```

### 6. Test Profile Switching

After adding a color:
1. Open the editor and verify the color appears
2. Change the color and save
3. Switch to a different profile
4. Switch back and verify the color persisted
5. Create a new profile and verify default value is used

### 7. Document the Color

Add comments explaining what the color controls:

```javascript
// Selection background - color for selected text in the editor
var defaultSelectionBackground: string = "#3366cc";
```

---

## Color Editor Troubleshooting

### Problem: Color Not Appearing in Editor

**Symptoms:** New color doesn't show up in the color editor screen

**Solutions:**

1. **Check variable is in screen vars array:**
   ```javascript
   "vars": [
       {"name": "myNewColor", "type": "string", "default": $myNewColor, ...}
   ]
   ```

2. **Check item is in screen areas:**
   ```javascript
   "items": [
       {"name": "myNewColor_item", "varref": "myNewColor"}
   ]
   ```

3. **Verify $variable syntax:**
   - Use `$myNewColor` (with $) for default values
   - Variable name must match between var declaration and screen definition

### Problem: Color Not Saving

**Symptoms:** Color changes in editor but doesn't persist after closing

**Solutions:**

1. **Check buildProfileColorsJson includes your color:**
   ```javascript
   colorsJson = colorsJson + "      \"myNewColor\": \"" + colorEditorScreen.myNewColor + "\"\n";
   ```

2. **Check saveConfigWithCurrentProfile handles your color:**
   - Must be in the section that reads from screen (line ~764-794)
   - Must be in the JSON building section (line ~828-857)

3. **Verify JSON comma placement:**
   - Last property should not have a trailing comma
   - Check generated JSON is valid

### Problem: Profile Switching Loses Color

**Symptoms:** Color resets when switching between profiles

**Solutions:**

1. **Add to onProfileChange function:**
   ```javascript
   var pMyNewColor: string = call json.getstring(profileColors, "myNewColor", "");
   if pMyNewColor != "" then { colorEditorScreen.myNewColor = pMyNewColor; }
   else { colorEditorScreen.myNewColor = defaultMyNewColor; }
   ```

2. **Add to saveConfigWithCurrentProfile:**
   - Read from screen values for current profile
   - Read from config for other profiles

3. **Check addProfileToConfig function:**
   - Ensure new profiles get the default value

### Problem: Default Not Applied

**Symptoms:** New color shows as empty or black instead of default

**Solutions:**

1. **Check fallback logic:**
   ```javascript
   if myNewColor == "" then { myNewColor = defaultMyNewColor; }
   ```

2. **Check setColorsToDefaults:**
   ```javascript
   colorEditorScreen.myNewColor = defaultMyNewColor;
   ```

3. **Verify default constant is defined:**
   ```javascript
   var defaultMyNewColor: string = "#ff00ff";
   ```

### Problem: Color Not Applied to UI

**Symptoms:** Color saves correctly but doesn't affect the console appearance

**Solutions:**

1. **Add to ConsoleConfig.java:**
   ```java
   String myColor = getColorValue(colors, "myNewColor", "#ff00ff");
   ```

2. **Apply to relevant UI components:**
   ```java
   component.setStyle("-fx-property: " + myColor + ";");
   ```

3. **Reload config after saving:**
   - The `system.reloadConfig()` call should trigger a reload
   - If not, restart the application

### Debug Tips

1. **Add print statements:**
   ```javascript
   print "myNewColor loaded: " + myNewColor;
   print "myNewColor from screen: " + colorEditorScreen.myNewColor;
   ```

2. **Check console.cfg manually:**
   - Open `console.cfg` in a text editor
   - Verify your color property appears in the profiles object
   - Check JSON syntax is valid

3. **Test with default profile first:**
   - Always test with the "default" profile before testing other profiles
   - The default profile has special handling in many places

---

## Summary

This guide covers three independent color systems in the EBS Script Interpreter:

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

### Part 3: Color Editor Screen
- **Purpose:** Manage console configuration colors through GUI
- **Components:** console.cfg → color_editor.ebs → ColorPickers → Save → ConsoleConfig.java
- **Files:** console.cfg, color_editor.ebs, ConsoleConfig.java, console.css
- **Scope:** Console configuration and theme management

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

### Part 3: Color Editor Screen Files

```
Repository Root/
├── console.cfg                        # Console color configuration

ScriptInterpreter/
├── src/main/java/com/eb/ui/ebs/
│   └── ConsoleConfig.java            # Loads and applies console colors
├── src/main/resources/
│   ├── css/
│   │   └── console.css               # Default color values
│   └── scripts/
│       └── color_editor.ebs          # Color editor screen script
```

---

*Last updated: 2025-12-23*
