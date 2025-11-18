package com.eb.script.token.ebs;

import com.eb.script.interpreter.Builtins;
import com.eb.script.token.Lexer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EbsLexer extends Lexer<EbsTokenType> {

    private List<EbsToken> tokens;

    public EbsLexer() {
        super(EbsTokenType.values());
        Set<String> builtins = Builtins.getBuiltins();
        super.addKeywords(EbsTokenType.BUILTIN, builtins);
        super.addKeywords(EbsTokenType.DATATYPE, super.getDatatypeStrings());
    }

    public List<EbsToken> tokenize(String str) {
        tokenizeInit(str);
        tokens = new ArrayList<>();
        try {
            while (!isAtEnd()) {
                scanToken();
            }
            tokens.add(new EbsToken(EbsTokenType.EOF, "\\0", line, current, current));
        } catch (Exception ex) {
            throw error(line, ex.getMessage());
        }
        return tokens;
    }

    private void scanToken() {

        ReturnToken<EbsTokenType> ret = super.scanNextToken();
        EbsTokenType t = ret.token;
        if (t != null) {
            switch (t) {
                case EbsTokenType.COMMENT -> {
                    skipLine();
                    addToken(t);
                }
                case EbsTokenType.CALL -> {
                    addToken(t, ret.tokenString);
                    advance(ret.tokenString.length());
                    start = current;
                    identifier();
                    advance();
                }
                case EbsTokenType.QUOTE1 -> {
                    string(EbsTokenType.QUOTE1);
                }
                case EbsTokenType.QUOTE2 -> {
                    string(EbsTokenType.QUOTE2);
                }
                default -> {
                    addToken(t, ret.tokenString);
                    advance(ret.tokenString.length());
                }
            }
        } else {
            start = current;
            char c = peekChar();
            switch (c) {
                default -> {
                    if (isDigit(c)) {
                        number();
                    } else if (isAlpha(c)) {
                        identifier();
                    }
                }
            }
            if (start == current && ret.style != null) {
                addCustomToken(EbsTokenType.CUSTOM, ret.tokenString, ret.style);
            }
            advance();
        }
    }

    private String getIdentifier() {
        char c = advance();
        while (isAlphaNumeric(c)) {
            c = advance();
        }
        if (current > source.length()) {
            current = source.length();
        }
        return source.substring(start, current).toLowerCase();
    }

    private void identifier() {
        if (!isAtEnd()) {
            int startIdent = start;
            String text = getIdentifier();
            char c = peek();
            while (c == '.') {
                // Save position before consuming the dot
                int beforeDot = current;
                advance();
                start = current;
                c = peek();
                if (isAlpha(c)) {
                    // Peek ahead to see what the next identifier is
                    int tempStart = current;
                    String nextIdent = getIdentifier();
                    
                    // Check if the next identifier is a reserved keyword (length, size, etc.)
                    EbsTokenType nextType = keywords.get(nextIdent);
                    if (nextType != null) {
                        // Don't combine with keywords - stop here
                        current = beforeDot;
                        break;
                    }
                    
                    // Not a keyword, combine it
                    text = text + "." + nextIdent;
                    c = peek();
                } else {
                    // No identifier after dot, rewind
                    current = beforeDot;
                    break;
                }
            }
            start = startIdent;
            current--;
            EbsTokenType t = keywords.get(text);
            if (t != null) {
                addToken(t, text);
            } else {
                String style = custom.get(text);
                if (style != null) {
                    addCustomToken(EbsTokenType.IDENTIFIER, text, style);
                } else if (lastToken != EbsTokenType.CALL) {
                    t = datatypes.get(text);
                    if (t != null) {
                        addToken(t, text);
                    } else {
                        addToken(EbsTokenType.IDENTIFIER, text);
                    }
                } else {
                    addToken(EbsTokenType.IDENTIFIER, text);
                }
            }
        }
    }

    private void number() {
        char c = peekChar();
        while (isDigit(c)) {
            c = advance();
        }
        current--;
        boolean isDecimal = false;
        boolean isLong = false;
        boolean isFloat = false;
        boolean isDouble = false;
        int end = current;
        // Check if next char is '.' and the char after that is a digit
        if (peekNext() == '.' && current + 2 < sourceLength && isDigit(source.charAt(current + 2))) {
            isDecimal = true;
            advance(); // move to '.'
            c = advance(); // consume '.' and get first digit after it
            while (isDigit(c)) {
                c = advance();
            }
            current--; // back up after going past last digit
            end = current;
            end = current;
            if (isAlpha(c)) {
                switch (c) {
                    case 'd', 'D' -> {
                        isDouble = true;
                        advance();
                    }
                    case 'f', 'F' -> {
                        isFloat = true;
                        advance();
                    }
                    case 'l', 'L' -> {
                        isLong = true;
                        advance();
                    }
                    default -> {
                    }
                }
            }
        }
        //current--;

        String num = source.substring(start, end + 1);
        if (isDecimal || isDouble) {
            addToken(EbsTokenType.DOUBLE, Double.valueOf(num));
        } else if (isFloat) {
            addToken(EbsTokenType.FLOAT, Float.valueOf(num));
        } else if (isLong) {
            addToken(EbsTokenType.LONG, Long.valueOf(num));
        } else {
            try {
                addToken(EbsTokenType.INTEGER, Integer.valueOf(num));
            } catch (NumberFormatException ex) {
                addToken(EbsTokenType.LONG, Long.valueOf(num));
            }
        }
    }

    private void string(EbsTokenType quoteToken) {
        addToken(quoteToken);
        advance();
        // Move off the opening quote and mark the start of content
        char c = peek();
        start = current;

        StringBuilder sb = new StringBuilder();
        char quote = quoteToken.getFirstChar();
        while (c != quote && !isAtEnd()) {
            if (c == '\n') {
                // Real newline inside the literal (NOT \n escape) — track line numbers
                line++;
                sb.append('\n');
                c = advance();
                continue;
            }

            if (c == '\\') {
                // Handle escape sequences
                if (isAtEnd()) {
                    errors.add("Unterminated string (after backslash) at line " + line);
                    return;
                }
                char e = advance();
                switch (e) {
                    case 'n' ->
                        sb.append('\n');
                    case 'r' ->
                        sb.append('\r');
                    case 't' ->
                        sb.append('\t');
                    case 'b' ->
                        sb.append('\b');
                    case 'f' ->
                        sb.append('\f');
                    case '\\' ->
                        sb.append('\\');
                    case '"' ->
                        sb.append('"');
                    case '\'' ->
                        sb.append('\'');
                    case 'u' -> { // \\uXXXX
                        int code = readHexDigits(4, "unicode");
                        sb.append((char) code);
                    }
                    case 'x' -> { // \xNN
                        int code = readHexDigits(2, "hex");
                        sb.append((char) code);
                    }
                    default -> {
                        // Unknown escape — keep the character as-is (or log a warning)
                        errors.add("Unknown escape '\\" + e + "' at line " + line);
                        sb.append(e);
                    }
                }
                // Advance to the next source character after the escape
                c = advance();
                continue;
            }

            // Regular character
            sb.append(c);
            c = advance();
        }
        if (isAtEnd()) {
            addToken(EbsTokenType.STRING, "");
            errors.add("Unterminated string at line " + line);
            return;
        }
        current--;

        // NOTE: At this point `current` points at the closing quote character.
        // We DON'T consume it here; scanToken() will do `current++` after returning.
        String value = sb.toString();

        // Preserve your date handling using the unescaped value
        if (isDateTime(value)) {
            addToken(EbsTokenType.DATE, toDateTime(value));
        } else {
            addToken(EbsTokenType.STRING, value);
        }
        advance();
        start = current;
        addToken(quoteToken);
        advance();
    }

    private int readHexDigits(int count, String kind) {
        int code = 0;
        for (int i = 0; i < count; i++) {
            if (isAtEnd()) {
                errors.add("Unterminated " + kind + " escape at line " + line);
                return code;
            }
            char h = advance();
            int d = Character.digit(h, 16);
            if (d == -1) {
                errors.add("Invalid " + kind + " escape digit '" + h + "' at line " + line);
                d = 0;
            }
            code = (code << 4) | d;
        }
        return code;
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private boolean isDateTime(String str) {
        if (str.matches("\\d{4}-\\d{2}-\\d{2}([T\\s]\\d{2}:\\d{2}(:\\d{2})?)?")) {
            return true;
        } else if (str.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return true;
        }
        return false;
    }

    private LocalDateTime toDateTime(String str) {
        if (str.length() > 10) {
            str = str.replaceAll("  ", " ");
            str = str.replace(' ', 'T');
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
    }

    private void addToken(EbsTokenType type) {
        addToken(type, null);
    }

    protected void addToken(EbsTokenType type, Object literal) {
        //String text = source.substring(start, current + 1);
        lastToken = new EbsToken(type, literal, line, start, current);
        tokens.add((EbsToken) lastToken);
    }

    protected void addCustomToken(EbsTokenType type, Object literal, String style) {
        //String text = source.substring(start, current + 1);
        lastToken = new EbsToken(type, literal, line, start, current, style);
        tokens.add((EbsToken) lastToken);
    }

    private LexerError error(int line, String message) {
        message = "Lexer error at line " + line + " : " + message;
        return new LexerError(message);
    }

    public static class LexerError extends RuntimeException {

        public LexerError(String message) {
            super(message);
        }
    }

}
