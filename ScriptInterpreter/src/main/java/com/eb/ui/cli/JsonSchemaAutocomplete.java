package com.eb.ui.cli;

import com.eb.script.json.Json;
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
     * @param jsonText The full JSON text being edited
     * @param caretPos The current caret position
     * @return List of autocomplete suggestions
     */
    public static List<String> getJsonSuggestions(String jsonText, int caretPos) {
        List<String> suggestions = new ArrayList<>();

        // Determine the current context (what are we completing?)
        JsonContext context = analyzeContext(jsonText, caretPos);
        
        // Extract the current partial word being typed
        String partialWord = extractPartialWord(jsonText, caretPos);

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
            suggestions.addAll(getEnumSuggestions(context));
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
     */
    public static boolean looksLikeJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        return (trimmed.startsWith("{") || trimmed.startsWith("["));
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
