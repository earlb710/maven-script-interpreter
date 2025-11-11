package com.eb.ui.cli;

import com.eb.script.interpreter.Builtins;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.ui.ebs.EbsStyled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides autocomplete suggestions for keywords and builtins.
 * Filters suggestions based on context (e.g., only builtins after 'call' or '#').
 *
 * @author Earl Bosch
 */
public class AutocompleteSuggestions {

    private static final List<String> KEYWORDS = Arrays.asList(
        "var", "print", "call", "return",
        "if", "then", "else",
        "foreach", "in",
        "while", "do", "break", "exit", "continue",
        "null", "true", "false",
        "byte", "int", "integer", "long", "float", "double",
        "string", "date", "bool", "boolean", "json",
        "connect", "use", "cursor", "open", "close", "connection",
        "select", "from", "where", "order", "by", "group", "having"
    );

    /**
     * Get all available suggestions (keywords + builtins).
     */
    public static List<String> getAllSuggestions() {
        List<String> all = new ArrayList<>(KEYWORDS);
        all.addAll(Builtins.getBuiltins());
        return all.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get only builtin function suggestions.
     */
    public static List<String> getBuiltinSuggestions() {
        return Builtins.getBuiltins().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get only keyword suggestions.
     */
    public static List<String> getKeywordSuggestions() {
        return new ArrayList<>(KEYWORDS);
    }

    /**
     * Get suggestions filtered by the given prefix (case-insensitive).
     */
    public static List<String> getSuggestionsWithPrefix(List<String> suggestions, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return suggestions;
        }
        
        String lowerPrefix = prefix.toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    /**
     * Determine what suggestions to show based on the current input context.
     * Returns builtins only if the last token is 'call' or '#', otherwise returns only keywords.
     */
    public static List<String> getSuggestionsForContext(String text, int caretPosition) {
        // Get the text before the caret
        String beforeCaret = text.substring(0, Math.min(caretPosition, text.length()));
        
        // Tokenize to find the context
        List<EbsToken> tokens = EbsStyled.tokenizeConsole(beforeCaret);
        
        // Find the word at caret position
        String currentWord = getCurrentWord(beforeCaret);
        
        // Check if we're after a 'call' keyword or '#' token
        boolean afterCallOrHash = false;
        if (!tokens.isEmpty()) {
            // Check last non-whitespace token
            for (int i = tokens.size() - 1; i >= 0; i--) {
                EbsToken token = tokens.get(i);
                String tokenStr = token.literal != null ? token.literal.toString().trim() : "";
                
                // Skip empty tokens
                if (tokenStr.isEmpty()) {
                    continue;
                }
                
                // Check if it's a 'call' keyword or '#'
                if (token.type == EbsTokenType.CALL || "#".equals(tokenStr)) {
                    afterCallOrHash = true;
                    break;
                }
                
                // If we hit another significant token, stop looking
                if (token.type != EbsTokenType.IDENTIFIER && !tokenStr.trim().isEmpty()) {
                    break;
                }
            }
        }
        
        // Determine which suggestions to return
        List<String> suggestions;
        if (afterCallOrHash) {
            // Only show builtins after 'call' or '#'
            suggestions = getBuiltinSuggestions();
        } else {
            // Show only keywords (not builtins)
            suggestions = getKeywordSuggestions();
        }
        
        // Filter by current word prefix
        return getSuggestionsWithPrefix(suggestions, currentWord);
    }

    /**
     * Extract the current word being typed at the end of the text.
     */
    private static String getCurrentWord(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Find the last word boundary (space, newline, or special character)
        int end = text.length();
        int start = end;
        
        // Move backwards while we have valid identifier characters
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                start--;
            } else {
                break;
            }
        }
        
        return text.substring(start, end);
    }
}
