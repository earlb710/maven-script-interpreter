# Syntax Highlighting Color Guide

## Overview

This guide explains how to add new syntax highlighting colors to the EBS Script Interpreter's interactive console and editor. The syntax highlighting system uses a three-layer architecture:

1. **PrintStyle** - Define semantic style names (Java enum)
2. **TokenType** - Map tokens to styles (token type definitions)
3. **CSS** - Define visual appearance (color, weight, style)

**Related Documentation:**
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - System architecture
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Language syntax

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Adding a New Highlighting Color](#adding-a-new-highlighting-color)
3. [Complete Example](#complete-example)
4. [CSS Style Properties](#css-style-properties)
5. [Best Practices](#best-practices)
6. [Troubleshooting](#troubleshooting)

---

## System Architecture

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
| **EbsStyled** | `src/main/java/com/eb/ui/ebs/EbsStyled.java` | Applies styles to text areas |
| **console.css** | `src/main/resources/css/console.css` | CSS definitions for colors and formatting |

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

## Complete Example

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

## Best Practices

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

## Troubleshooting

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

For more information, see:
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - Overall system design
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Language syntax
- [RichTextFX Documentation](https://github.com/FXMisc/RichTextFX) - Text area library

---

## Appendix: File Reference

Quick reference for all files involved in syntax highlighting:

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

---

*Last updated: 2025-12-22*
