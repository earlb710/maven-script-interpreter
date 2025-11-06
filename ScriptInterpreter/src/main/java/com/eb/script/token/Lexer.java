package com.eb.script.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Earl Bosch
 */
public abstract class Lexer<T extends LexerToken> {

    protected final Map<String, T> datatypes = new HashMap();
    protected final Map<String, T> keywords = new HashMap();
    protected final Map<String, T> operators = new HashMap();
    protected final Map<String, String> custom = new HashMap();
    protected final LexerToken[][] charTokens = new LexerToken[128][];
    protected final String[] charStyles = new String[128];

    protected String source;
    protected int sourceLength;
    protected int start = 0;
    protected int current = 0;
    protected char currentChar = 0;
    protected int line = 1;
    protected LexerToken lastToken;
    protected final List<String> errors = new ArrayList();

    public static class ReturnToken<T extends LexerToken> {

        public T token;
        public String style;
        public String tokenString;

        public ReturnToken(T token, String tokenString) {
            this.token = token;
            this.style = null;
            this.tokenString = tokenString;
        }

        public ReturnToken(String style, String tokenString) {
            this.token = null;
            this.style = style;
            this.tokenString = tokenString;
        }

    }

    public Lexer(T[] tokenDef) {
        charTokens[0] = new LexerToken[128];
        for (T t : tokenDef) {
            LexerType[] lta = t.getLexerType();
            for (LexerType lt : lta) {
                String s = lt.getString();
                if (s.length() > 0) {
                    char c = s.charAt(0);
                    if (c < 65 || (c > 90 && c < 97) || c > 122) {
                        if (s.length() == 1) {
                            charTokens[0][c] = t;
                        } else if (s.length() == 2) {
                            LexerToken[] t2 = charTokens[c];
                            if (t2 == null) {
                                t2 = new LexerToken[128];
                                charTokens[c] = t2;
                            }
                            t2[s.charAt(1)] = t;
                        }
                    }
                }
                if (null != lt.cat) {
                    switch (lt.cat) {
                        case DATATYPE -> {
                            datatypes.put(s, t);
                        }
                        case KEYWORD -> {
                            keywords.put(s, t);
                        }
                        case OPERATOR -> {
                            operators.put(s, t);
                        }
                    }
                }
            }
        }
    }

    protected void tokenizeInit(String str) {
        source = str;
        sourceLength = str.length();
        errors.clear();
        current = 0;
    }

    protected ReturnToken<T> scanNextToken() {
        char c = peekChar();
        boolean found = true;
        //skip white space chars
        do {
            switch (c) {
                case ' ', '\r', '\t' -> {
                    current++;
                    c = peekChar();
                }
                case '\n' -> {
                    current++;
                    line++;
                    c = peekChar();
                }
                default -> {
                    start = current;
                    found = false;
                }
            }
        } while (found && !isAtEnd());

        LexerToken t = null;
        String retString = "";
        if (!isAtEnd()) {
            LexerToken[] t1 = charTokens[c];
            if (t1 != null) {
                char c2 = peekNext();
                t = t1[c2];
                if (t == null) {
                    t = charTokens[0][c];
                    if (t != null) {
                        retString = new String(new char[]{c});
                    }
                } else {
                    retString = new String(new char[]{c, c2});
                    current++;
                }
            } else {
                t = charTokens[0][c];
                if (t != null) {
                    retString = new String(new char[]{c});
                }
            }
            if (t == null) {
                String s1 = charStyles[c];
                if (s1 != null) {
                    return new ReturnToken(s1, new String(new char[]{c}));
                }
            }
        }
        return new ReturnToken(t, retString);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addCustomKeywords(String style, String... words) {
        for (String w : words) {
            custom.put(w.toLowerCase(), style);
        }
    }

    public void addCustomChar(String style, char... characters) {
        for (char c : characters) {
            charStyles[c] = style;
        }

    }

    public void addCustomKeywords(String style, Collection<String> words) {
        for (String w : words) {
            custom.put(w.toLowerCase(), style);
        }
    }

    public void addKeywords(T token, String... words) {
        for (String w : words) {
            keywords.put(w.toLowerCase(), token);
        }
    }

    public void addKeywords(T token, Collection<String> words) {
        for (String w : words) {
            keywords.put(w.toLowerCase(), token);
        }
    }

    public Set<String> getDatatypeStrings() {
        return datatypes.keySet();
    }

    public Set<String> getKeywordStrings() {
        return keywords.keySet();
    }

    public Set<String> getOperatorStrings() {
        return operators.keySet();
    }

    public Map<String, T> getDatatypes() {
        return datatypes;
    }

    public Map<String, T> getKeywords() {
        return keywords;
    }

    public Map<String, T> getOperators() {
        return operators;
    }

    protected boolean isAtEnd() {
        return current >= sourceLength;
    }

    protected char advance() {
        current++;
        return peekChar();
    }

    protected char advance(int count) {
        current = current + count;
        return peekChar();
    }

    protected void skipLine() {
        while (current < sourceLength && source.charAt(current) != '\n') {
            current++;
        }
        //current--;
    }

    protected char peek() {
        return currentChar;
    }

    protected char peekChar() {
        if (isAtEnd()) {
            currentChar = '\0';
        } else {
            currentChar = source.charAt(current);
        }
        return currentChar;
    }

    protected char peekNext() {
        if (current + 1 >= sourceLength) {
            return '\0';
        }
        char ret = source.charAt(current + 1);
        return ret;
    }

    protected boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    protected boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_';
    }

    protected boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

}
