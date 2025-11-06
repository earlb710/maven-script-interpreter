package com.eb.script.token.ebs;

import com.eb.script.token.LexerType;
import com.eb.script.token.LexerToken;
import java.util.List;

public class EbsToken implements LexerToken {

    private static final List defaultStyle=List.of("info");
    private static final EbsToken emptyToken = new EbsToken(EbsTokenType.NULL, "", 0);
    
    public EbsTokenType type;
    public Object literal;
    public int line;
    public final String style;
    public final List<String> styleList;
    public int start, end;

    public EbsToken(EbsTokenType type, Object literal, int line) {
        this.type = type;
        this.literal = literal;
        this.line = line;
        this.style = this.type.getStyle();
        this.styleList = (style == null) ? defaultStyle : List.of(style);
    }

    public EbsToken(EbsTokenType type, Object literal, int line, int start, int end) {
        this.type = type;
        this.literal = literal;
        this.line = line;
        this.start = start;
        this.end = end;
        this.style = type.getStyle();
        this.styleList = (style == null) ? defaultStyle : List.of(style);
    }

    public EbsToken(EbsTokenType type, Object literal, int line, int start, int end, String style) {
        this.type = type;
        this.literal = literal;
        this.line = line;
        this.start = start;
        this.end = end;
        this.style = style;
        this.styleList = (style == null) ? defaultStyle : List.of(style);
    }

    @Override
    public String toString() {
        return type + " " + (literal != null ? literal.toString() : "null");
    }

    @Override
    public LexerType[] getLexerType() {
        return type.lexerTypes;
    }

}
