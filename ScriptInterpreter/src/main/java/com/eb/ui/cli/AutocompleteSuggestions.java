package com.eb.ui.cli;

import com.eb.script.interpreter.Builtins;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.token.Category;
import com.eb.script.token.DataType;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.ui.ebs.EbsStyled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides autocomplete suggestions for keywords, builtins, and console
 * commands. Filters suggestions based on context (e.g., only builtins after
 * 'call' or '#', console commands when input starts with '/').
 *
 * @author Earl Bosch
 */
public class AutocompleteSuggestions {

    private static final List<String> KEYWORDS = new ArrayList();

    static {
        for (EbsTokenType t : EbsTokenType.values()) {
            // Keyword tokens (include synonyms, e.g. "break","exit")
            if (t.getCategory() == Category.KEYWORD) {
                for (String s : t.getStrings()) {
                    if (s != null && !s.isEmpty()) {
                        KEYWORDS.add(s);
                    }
                }
            }
        }
    }

    private static final List<String> CONSOLE_COMMANDS = Arrays.asList(
            "/help", "/?",
            "/help keywords",
            "/clear",
            "/reset",
            "/time",
            "/open",
            "/close",
            "/list files",
            "/list open files",
            "/echo",
            "/echo on",
            "/echo off",
            "/debug",
            "/debug on",
            "/debug off",
            "/debug trace on",
            "/debug trace off",
            "/exit"
    );

    /**
     * Get all available suggestions (keywords + builtins).
     */
    public static List<String> getAllSuggestions() {
        List<String> all = new ArrayList<>(KEYWORDS);
        all.addAll(Builtins.getBuiltins());
        all.addAll(CONSOLE_COMMANDS);
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
     * Get only console command suggestions.
     */
    public static List<String> getConsoleCommandSuggestions() {
        return new ArrayList<>(CONSOLE_COMMANDS);
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
     * Returns console commands if the token starts with '/', builtins after
     * 'call' or '#', otherwise returns only keywords.
     */
    public static List<String> getSuggestionsForContext(String text, int caretPosition) {
        // Get the text before the caret
        String beforeCaret = text.substring(0, Math.min(caretPosition, text.length()));

        // Find the word at caret position
        String currentWord = getCurrentWord(beforeCaret);

        // Check if the current line starts with '/' (console command)
        if (currentWord.startsWith("/")) {
            currentWord = currentWord.substring(1);
            return getSuggestionsWithPrefix(getConsoleCommandSuggestions(), currentWord);
        }

        // Tokenize to find the context
        List<EbsToken> tokens = EbsStyled.tokenizeConsole(beforeCaret);

        // Check if we're after a 'call' keyword or '#' token
        boolean afterCallOrHash = false;
        if (!tokens.isEmpty()) {
            // Check last non-whitespace token
            for (int i = tokens.size() - 1; i >= 0; i--) {
                EbsToken token = tokens.get(i);
                String tokenStr = token.literal != null ? token.literal.toString().trim() : "";

                // Skip empty tokens
                if (tokenStr.isEmpty() || token.type == EbsTokenType.EOF) {
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

        // Move backwards while we have valid identifier characters or '/' for console commands
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '/' || c == '?') {
                start--;
            } else {
                break;
            }
        }

        return text.substring(start, end);
    }

    /**
     * Check if a given name is a builtin function.
     */
    public static boolean isBuiltin(String name) {
        return Builtins.getBuiltins().contains(name);
    }

    /**
     * Generate parameter signature for a builtin function.
     * Returns a string like "(param1="", param2=0)" with default values based on type.
     * Returns null if the name is not a builtin.
     */
    public static String getBuiltinParameterSignature(String builtinName) {
        Builtins.BuiltinInfo info = Builtins.getBuiltinInfo(builtinName);
        if (info == null || info.params == null || info.params.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < info.params.length; i++) {
            Parameter param = info.params[i];
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(param.name).append("=");
            
            // Add default value based on parameter type
            if (param.paramType != null) {
                switch (param.paramType) {
                    case STRING:
                        sb.append("\"\"");
                        break;
                    case INTEGER:
                    case LONG:
                    case BYTE:
                        sb.append("0");
                        break;
                    case FLOAT:
                    case DOUBLE:
                        sb.append("0.0");
                        break;
                    case BOOL:
                        sb.append("false");
                        break;
                    case JSON:
                        sb.append("{}");
                        break;
                    default:
                        // For other types (ARRAY, DATE, ANY), leave empty
                        break;
                }
            }
        }
        sb.append(")");

        return sb.toString();
    }
}
