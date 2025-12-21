# Enhancement: Dynamic Builtin Pattern and Import Tracking

## User Request
The user requested two enhancements:
1. Check against all imported functions by parsing imported files and caching them
2. Use the builtin collection dynamically instead of hardcoding the prefixes

## Implementation

### 1. Dynamic Builtin Pattern

**Before:**
```java
String BUILTIN = "\\b(?:thread|string|array|json|file|http|ftp|...)\\.[A-Za-z_][A-Za-z0-9_]*\\b";
```
- Hardcoded list of 30+ prefixes
- Required manual updates when builtins are added/removed
- Risk of missing new builtins

**After:**
```java
private static String buildBuiltinPattern() {
    Set<String> builtins = Builtins.getBuiltins();
    Set<String> prefixes = new HashSet<>();
    
    // Extract unique prefixes from builtin names
    for (String builtin : builtins) {
        int dotIndex = builtin.indexOf('.');
        if (dotIndex > 0) {
            prefixes.add(builtin.substring(0, dotIndex));
        }
    }
    
    // Build regex from extracted prefixes
    String prefixAlternation = String.join("|", prefixes);
    return "\\b(?:" + prefixAlternation + ")\\.[A-Za-z_][A-Za-z0-9_]*\\b";
}
```

**Benefits:**
- Automatically includes all builtins from the registry
- No maintenance required when builtins are added
- Always in sync with actual builtin definitions
- Extracts prefixes like: `thread`, `string`, `array`, `json`, `canvas`, `draw`, etc.

### 2. Import Tracking

**Implementation Details:**

#### Parse Import Statements
```java
private List<String> extractImports(String text) {
    List<String> imports = new ArrayList<>();
    Pattern importPattern = Pattern.compile("^\\s*import\\s+[\"']([^\"']+)[\"']\\s*;", Pattern.MULTILINE);
    Matcher m = importPattern.matcher(text);
    while (m.find()) {
        imports.add(m.group(1));
    }
    return imports;
}
```

Matches patterns like:
- `import "chess-moves.ebs";`
- `import 'utils.ebs';`

#### Extract Functions from Imports
```java
private Set<String> extractImportedFunctions(String text) {
    List<String> imports = extractImports(text);
    
    // Hash-based caching to avoid re-parsing
    String importHash = String.join("|", imports);
    if (importHash.equals(lastImportHash)) {
        return cachedImportedFunctions;
    }
    
    lastImportHash = importHash;
    Set<String> importedFunctions = new HashSet<>();
    
    // Parse each imported file
    for (String importFile : imports) {
        Path importPath = currentDir.resolve(importFile).normalize();
        if (Files.exists(importPath)) {
            String importedText = Files.readString(importPath, defaultCharset);
            
            // Extract functions
            Set<String> functionsInImport = extractCustomFunctions(importedText);
            importedFunctions.addAll(functionsInImport);
            
            // Recursive: handle nested imports
            Set<String> nestedImports = extractImportedFunctions(importedText);
            importedFunctions.addAll(nestedImports);
        }
    }
    
    cachedImportedFunctions = importedFunctions;
    return importedFunctions;
}
```

**Features:**
- **Relative path resolution**: Resolves imports relative to current file's directory
- **Recursive parsing**: Handles nested imports (imports within imports)
- **Caching**: Uses hash of import statements to detect changes
  - Only re-parses when imports change, not on every keystroke
  - Efficient for real-time highlighting (100ms debounce)
- **Error handling**: Gracefully skips files that can't be read

#### Combine Functions for Highlighting
```java
private StyleSpans<Collection<String>> computeEbsHighlighting(String text) {
    // Extract local functions
    Set<String> customFunctions = extractCustomFunctions(text);
    
    // Extract imported functions (with caching)
    Set<String> importedFunctions = extractImportedFunctions(text);
    
    // Combine both sets
    Set<String> allCustomFunctions = new HashSet<>(customFunctions);
    allCustomFunctions.addAll(importedFunctions);
    
    // Use allCustomFunctions for highlighting checks
    if (allCustomFunctions.contains(lowerName)) {
        styleClass = "tok-custom-function";
    }
}
```

## Example Usage

### File: chess-game.ebs
```ebs
import "chess-moves.ebs";

// isCheckmate is defined in chess-moves.ebs
var result = call isCheckmate(player);  // Highlighted as ORANGE (custom)
```

### File: chess-moves.ebs
```ebs
function isCheckmate(playerColor: int) return bool {
    // Implementation
}
```

**Result**: The `isCheckmate` call in `chess-game.ebs` is now highlighted in orange because the function is found in the imported file.

## Performance Considerations

1. **Caching**: Import hash prevents re-parsing on every keystroke
2. **Debouncing**: Highlighting only updates after 100ms pause in typing
3. **Early exit**: If import statements haven't changed, returns cached functions immediately
4. **Error handling**: Failed file reads don't break highlighting, just skip that import

## Benefits

1. **Accurate highlighting**: Functions from imports correctly identified
2. **No false negatives**: Imported functions no longer show as unknown (white)
3. **Dynamic builtins**: Automatically synced with builtin registry
4. **Maintainability**: No hardcoded lists to update
5. **Robustness**: Handles complex import scenarios (nested, relative paths)

## Commit
Implemented in commit: **dc32f35**
