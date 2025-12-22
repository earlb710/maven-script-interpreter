# Critical Bug Fix: Highlighting Not Applied

## Problem
Function highlighting was not being applied at all - everything remained white/default color.

## Root Cause
The custom function highlighting logic was fully implemented in `setupEbsSyntaxHighlighting()` and `computeEbsHighlighting()` methods, but **these methods were never being called**.

### The Flow Issue:
1. In `tabUI()` method (line 486), for .ebs files, the code was calling `setupLexerHighlighting()`
2. `setupLexerHighlighting()` → `applyLexerSpans()` → uses `EbsLexer.tokenize()`
3. The EbsLexer tokenizer only provides basic token-level styling (keywords, strings, numbers, etc.)
4. The custom function highlighting logic in `computeEbsHighlighting()` was completely bypassed

### What Was Happening:
```java
// In tabUI() - line 486
} else {
    setupLexerHighlighting();  // ❌ This was called for .ebs files
}

// setupEbsSyntaxHighlighting() was defined but NEVER called!
```

## Solution
Changed `tabUI()` to explicitly call `setupEbsSyntaxHighlighting()` for .ebs files:

```java
// In tabUI() - updated code
} else if (isEbs) {
    setupEbsSyntaxHighlighting();  // ✅ Now calls custom function highlighting
} else {
    setupLexerHighlighting();      // For other file types
}
```

### Additional Improvements:
1. **Scroll Position Preservation**: Updated `applyEbsHighlighting()` to preserve scroll position when applying styles (matching lexer behavior)
2. **Find Bar Compatibility**: Added check to skip styling when find bar is visible with active highlights
3. **Bracket Highlighting**: Ensured bracket matching is reapplied after syntax highlighting

## Result
The custom function highlighting now works correctly:
- **Custom functions**: Orange (#FFB86C)
- **Built-in functions**: Yellow (#DCDCAA)
- **Undefined functions**: Red (#FF5555) with underline

## Commit
Fixed in commit: **7cbe4fa**

## Lesson Learned
This is a classic "implementation vs integration" bug. The highlighting logic was correctly implemented and tested in isolation, but the integration point (where it gets called from the UI setup) was missed. Always verify that new features are actually wired up in the application flow!
