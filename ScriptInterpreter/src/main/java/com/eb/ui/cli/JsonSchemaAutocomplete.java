package com.eb.ui.cli;

import com.eb.script.json.Json;
import com.eb.script.interpreter.Builtins;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides autocomplete suggestions based on JSON Schema definitions.
 * Supports schema-aware property names, enum values, and type suggestions
 * for JSON screen definitions, area definitions, and display metadata.
 *
 * @author Earl Bosch
 */
public class JsonSchemaAutocomplete {

    private static final Map<String, Object> SCREEN_SCHEMA = loadSchema("/json/screen-definition.json");
    private static final Map<String, Object> AREA_SCHEMA = loadSchema("/json/area-definition.json");
    private static final Map<String, Object> DISPLAY_SCHEMA = loadSchema("/json/display-metadata.json");

    /**
     * Load a JSON schema from the classpath.
     */
    private static Map<String, Object> loadSchema(String resourcePath) {
        try (InputStream is = JsonSchemaAutocomplete.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Warning: Could not load schema from " + resourcePath);
                return new HashMap<>();
            }
            String schemaJson = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            Object parsed = Json.parse(schemaJson);
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
        } catch (Exception e) {
            System.err.println("Error loading schema " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    /**
     * Get autocomplete suggestions for JSON content based on cursor position.
     * Analyzes the JSON structure and provides context-aware suggestions.
     *
     * @param jsonText The full JSON text being edited (may include assignment like "screen x = {")
     * @param caretPos The current caret position in the full text
     * @return List of autocomplete suggestions
     */
    public static List<String> getJsonSuggestions(String jsonText, int caretPos) {
        List<String> suggestions = new ArrayList<>();
        
        // Find where the JSON actually starts (after = if present)
        int jsonStartPos = findJsonStartPosition(jsonText);
        
        // If JSON hasn't started yet or caret is before JSON, return empty
        if (jsonStartPos < 0 || caretPos < jsonStartPos) {
            return suggestions;
        }
        
        // Extract just the JSON portion and adjust caret position
        String pureJson = jsonText.substring(jsonStartPos);
        int adjustedCaretPos = caretPos - jsonStartPos;

        // Determine the current context (what are we completing?)
        JsonContext context = analyzeContext(pureJson, adjustedCaretPos);
        
        // Extract the current partial word being typed
        String partialWord = extractPartialWord(pureJson, adjustedCaretPos);

        if (context.isInString) {
            // We're inside a string
            if (context.isKey || context.expectingKey) {
                // We're typing a property name
                suggestions.addAll(getPropertySuggestions(context));
            } else {
                // We're typing a value - check if it's an enum property
                suggestions.addAll(getEnumSuggestions(context));
            }
        } else if (context.expectingKey) {
            // We're not in a string but expecting a key (just after { or ,)
            suggestions.addAll(getPropertySuggestions(context));
        } else if (context.expectingValue) {
            // We're expecting a value after a colon
            // Check if user has typed '#' - if so, suggest builtins
            if (partialWord.startsWith("#")) {
                // User has typed #, suggest builtin functions
                suggestions.addAll(getBuiltinSuggestions());
            } else if (partialWord.isEmpty()) {
                // No partial word yet, suggest only '#' to trigger builtins
                suggestions.add("#");
            } else {
                // User is typing something else, only suggest enum values
                suggestions.addAll(getEnumSuggestions(context));
            }
        }
        
        // Filter suggestions by partial word
        if (!partialWord.isEmpty()) {
            String lowerPartial = partialWord.toLowerCase();
            suggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(lowerPartial))
                    .collect(Collectors.toList());
        }

        return suggestions;
    }
    
    /**
     * Find the position where JSON content starts in the text.
     * Handles cases like "screen x = {" where JSON starts after =.
     * Returns the position of { or [, or -1 if not found.
     */
    private static int findJsonStartPosition(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        
        // Check if text starts with JSON
        String trimmed = text.trim();
        int trimOffset = text.indexOf(trimmed.charAt(0));
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimOffset;
        }
        
        // Search backwards for the last occurrence of = followed by { or [
        // This handles cases like "var x = 10; screen s = {" where we want the second =
        for (int equalsIndex = text.lastIndexOf('='); equalsIndex >= 0; equalsIndex = text.lastIndexOf('=', equalsIndex - 1)) {
            // Find the { or [ after this =
            for (int i = equalsIndex + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{' || c == '[') {
                    return i;
                }
                if (!Character.isWhitespace(c) && c != '\n' && c != '\r') {
                    // Found a non-whitespace character that's not { or [
                    // This = is not followed by JSON, try the previous =
                    break;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Extract the partial word being typed at the caret position.
     */
    private static String extractPartialWord(String text, int caretPos) {
        int start = caretPos - 1;
        while (start >= 0) {
            char c = text.charAt(start);
            if (c == '"' || c == '{' || c == '}' || c == '[' || c == ']' || 
                c == ':' || c == ',' || Character.isWhitespace(c)) {
                break;
            }
            start--;
        }
        start++;
        return text.substring(start, caretPos);
    }

    /**
     * Analyze the JSON context at the given caret position.
     */
    private static JsonContext analyzeContext(String text, int caretPos) {
        JsonContext ctx = new JsonContext();
        
        // Find the most recent structural character before caret (outside of strings)
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        String lastKey = null;
        Character lastStructuralChar = null;
        int stringStart = -1;
        
        for (int i = 0; i < Math.min(caretPos, text.length()); i++) {
            char c = text.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                if (!inString) {
                    // Starting a string
                    stringStart = i;
                    inString = true;
                } else {
                    // Ending a string
                    inString = false;
                    // Check if this was a key (followed by :)
                    int j = i + 1;
                    while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                        j++;
                    }
                    if (j < text.length() && text.charAt(j) == ':') {
                        // It was a key
                        lastKey = text.substring(stringStart + 1, i);
                    }
                }
            } else if (!inString) {
                if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                    lastStructuralChar = c;
                    if (c == ',') {
                        lastKey = null; // Reset last key after comma
                    }
                }
            }
        }
        
        // Now determine the context
        ctx.isInString = inString;
        
        // If we're in a string, we need to determine if it's a key or value
        if (inString) {
            // Look backwards to see what came before this string
            int i = stringStart - 1;
            while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
                i--;
            }
            if (i >= 0) {
                char beforeString = text.charAt(i);
                // String after { or , is likely a key
                ctx.isKey = (beforeString == '{' || beforeString == ',');
                ctx.expectingKey = ctx.isKey;
            }
        } else {
            // Not in a string, check last structural character
            if (lastStructuralChar != null) {
                ctx.expectingKey = (lastStructuralChar == '{' || lastStructuralChar == ',');
                ctx.expectingValue = (lastStructuralChar == ':');
            }
        }
        
        ctx.currentKey = lastKey;
        
        return ctx;
    }

    /**
     * Get property name suggestions based on the current schema context.
     */
    private static List<String> getPropertySuggestions(JsonContext context) {
        List<String> suggestions = new ArrayList<>();
        
        // Add common properties from all schemas
        Set<String> allProps = new HashSet<>();
        
        // Screen definition properties
        addPropertiesFromSchema(SCREEN_SCHEMA, allProps);
        
        // Area definition properties
        addPropertiesFromSchema(AREA_SCHEMA, allProps);
        
        // Display metadata properties
        addPropertiesFromSchema(DISPLAY_SCHEMA, allProps);
        
        suggestions.addAll(allProps);
        Collections.sort(suggestions);
        
        return suggestions;
    }

    /**
     * Extract property names from a schema.
     */
    private static void addPropertiesFromSchema(Map<String, Object> schema, Set<String> props) {
        Object properties = schema.get("properties");
        if (properties instanceof Map) {
            Map<String, Object> propsMap = (Map<String, Object>) properties;
            props.addAll(propsMap.keySet());
        }
        
        // Also check definitions
        Object definitions = schema.get("definitions");
        if (definitions instanceof Map) {
            Map<String, Object> defsMap = (Map<String, Object>) definitions;
            for (Object def : defsMap.values()) {
                if (def instanceof Map) {
                    Map<String, Object> defMap = (Map<String, Object>) def;
                    Object defProps = defMap.get("properties");
                    if (defProps instanceof Map) {
                        Map<String, Object> defPropsMap = (Map<String, Object>) defProps;
                        props.addAll(defPropsMap.keySet());
                    }
                }
            }
        }
    }

    /**
     * Get builtin function suggestions with # prefix for use in JSON values.
     */
    private static List<String> getBuiltinSuggestions() {
        return Builtins.getBuiltins().stream()
                .map(name -> "#" + name)
                .collect(Collectors.toList());
    }

    /**
     * Get enum value suggestions based on the current property being edited.
     */
    private static List<String> getEnumSuggestions(JsonContext context) {
        List<String> suggestions = new ArrayList<>();
        
        if (context.currentKey != null) {
            // Check all schemas for enum values for this property
            suggestions.addAll(getEnumForProperty(SCREEN_SCHEMA, context.currentKey));
            suggestions.addAll(getEnumForProperty(AREA_SCHEMA, context.currentKey));
            suggestions.addAll(getEnumForProperty(DISPLAY_SCHEMA, context.currentKey));
        }
        
        return suggestions;
    }

    /**
     * Extract enum values for a specific property from a schema.
     */
    private static List<String> getEnumForProperty(Map<String, Object> schema, String propertyName) {
        List<String> enums = new ArrayList<>();
        
        // Check in main properties
        Object properties = schema.get("properties");
        if (properties instanceof Map) {
            Map<String, Object> propsMap = (Map<String, Object>) properties;
            Object propDef = propsMap.get(propertyName);
            if (propDef instanceof Map) {
                enums.addAll(extractEnumValues((Map<String, Object>) propDef));
            }
        }
        
        // Check in definitions
        Object definitions = schema.get("definitions");
        if (definitions instanceof Map) {
            Map<String, Object> defsMap = (Map<String, Object>) definitions;
            for (Object def : defsMap.values()) {
                if (def instanceof Map) {
                    Map<String, Object> defMap = (Map<String, Object>) def;
                    Object defProps = defMap.get("properties");
                    if (defProps instanceof Map) {
                        Map<String, Object> defPropsMap = (Map<String, Object>) defProps;
                        Object propDef = defPropsMap.get(propertyName);
                        if (propDef instanceof Map) {
                            enums.addAll(extractEnumValues((Map<String, Object>) propDef));
                        }
                    }
                }
            }
        }
        
        return enums;
    }

    /**
     * Extract enum values from a property definition.
     */
    private static List<String> extractEnumValues(Map<String, Object> propDef) {
        List<String> values = new ArrayList<>();
        Object enumObj = propDef.get("enum");
        if (enumObj instanceof List) {
            List<?> enumList = (List<?>) enumObj;
            for (Object val : enumList) {
                if (val != null) {
                    values.add(String.valueOf(val));
                }
            }
        } else if (enumObj instanceof com.eb.script.arrays.ArrayDef<?, ?>) {
            // Handle ArrayDef from JSON parser
            com.eb.script.arrays.ArrayDef<?, ?> arrayDef = (com.eb.script.arrays.ArrayDef<?, ?>) enumObj;
            for (int i = 0; i < arrayDef.size(); i++) {
                Object val = arrayDef.get(i);
                if (val != null) {
                    values.add(String.valueOf(val));
                }
            }
        }
        return values;
    }

    /**
     * Check if the text appears to be JSON content.
     * Looks for JSON opening braces/brackets anywhere in the text,
     * which handles cases like "screen testScreen = {" where the JSON
     * starts after an assignment.
     */
    /**
     * Check if the text looks like JSON content, considering the caret position.
     * @param text The full text
     * @param caretPosition The current caret position (optional, -1 to check entire text)
     * @return true if it looks like JSON
     */
    public static boolean looksLikeJson(String text, int caretPosition) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // If caret position is provided, extract context around it
        if (caretPosition >= 0) {
            // Look backwards from caret to find the start of the current JSON structure
            // or assignment statement
            int searchStart = Math.max(0, caretPosition - 500); // Look back up to 500 chars
            String localContext = text.substring(searchStart, Math.min(text.length(), caretPosition + 100));
            
            // Check if we're inside a JSON structure
            if (localContext.contains("{") || localContext.contains("[")) {
                // Count braces/brackets to see if we're inside JSON
                int braceDepth = 0;
                int bracketDepth = 0;
                boolean inString = false;
                char stringChar = 0;
                
                for (int i = 0; i < localContext.length() && i < caretPosition - searchStart; i++) {
                    char c = localContext.charAt(i);
                    if (inString) {
                        if (c == '\\') {
                            i++; // Skip escaped character
                        } else if (c == stringChar) {
                            inString = false;
                        }
                    } else {
                        if (c == '"' || c == '\'') {
                            inString = true;
                            stringChar = c;
                        } else if (c == '{') {
                            braceDepth++;
                        } else if (c == '}') {
                            braceDepth--;
                        } else if (c == '[') {
                            bracketDepth++;
                        } else if (c == ']') {
                            bracketDepth--;
                        }
                    }
                }
                
                // If we're inside braces/brackets, we're likely in JSON
                if (braceDepth > 0 || bracketDepth > 0) {
                    return true;
                }
            }
            
            // Check if there's a screen/var json assignment in the local context
            if (localContext.matches("(?s).*\\b(screen|var\\s+json)\\s+\\w+\\s*=\\s*\\{.*")) {
                return true;
            }
        }
        
        // Fall back to original logic for full-text check
        String trimmed = text.trim();
        
        // Check if text starts with JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return true;
        }
        
        // Check if there's an assignment followed by JSON (e.g., "screen x = {")
        // Look for = followed by optional whitespace and then { or [
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex >= 0 && equalsIndex < trimmed.length() - 1) {
            String afterEquals = trimmed.substring(equalsIndex + 1).trim();
            if (afterEquals.startsWith("{") || afterEquals.startsWith("[")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if the text looks like JSON content (legacy method for backward compatibility).
     */
    public static boolean looksLikeJson(String text) {
        return looksLikeJson(text, -1);
    }
    
    /**
     * Check if we're in a context where JSON property keys should be suggested.
     * This helps the UI know whether to add quotes around the suggestion.
     * 
     * @param text The full text being edited
     * @param caretPos The current caret position
     * @return true if we're suggesting property keys (which need quotes)
     */
    public static boolean isSuggestingJsonKeys(String text, int caretPos) {
        if (!looksLikeJson(text)) {
            return false;
        }
        
        // Find where the JSON actually starts
        int jsonStartPos = findJsonStartPosition(text);
        if (jsonStartPos < 0 || caretPos < jsonStartPos) {
            return false;
        }
        
        // Extract just the JSON portion and adjust caret position
        String pureJson = text.substring(jsonStartPos);
        int adjustedCaretPos = caretPos - jsonStartPos;
        
        // Analyze context
        JsonContext context = analyzeContext(pureJson, adjustedCaretPos);
        
        // We're suggesting keys if we're in a string that's a key, or expecting a key
        return (context.isInString && context.isKey) || context.expectingKey;
    }

    /**
     * Context information about the current position in JSON.
     */
    private static class JsonContext {
        boolean expectingKey = false;
        boolean expectingValue = false;
        boolean isInString = false;
        boolean isKey = false;
        boolean afterColon = false;
        String currentKey = null;
    }
}
