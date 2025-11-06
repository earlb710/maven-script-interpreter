package com.eb.script.token;

/**
 *
 * @author Earl Bsoch
 */
public class LexerType {

    public static final LexerType NONE = new LexerType("");

    protected final String str;
    protected final Category cat;
    protected final DataType dataType;
    protected final PrintStyle style; //css style
    public final char firstChar;

    public LexerType(Category cat, DataType dataType, PrintStyle style, String strings) {
        this.str = strings;
        this.cat = cat;
        this.dataType = dataType;
        this.style = style;
        this.firstChar = (strings == null || strings.isEmpty()) ? 0 : strings.charAt(0);
    }

    public LexerType(Category cat, PrintStyle style, String strings) {
        this.str = strings;
        this.cat = cat;
        this.dataType = null;
        this.style = style;
        this.firstChar = (strings == null || strings.isEmpty()) ? 0 : strings.charAt(0);
    }

    public LexerType(PrintStyle style, String strings) {
        this.str = strings;
        this.cat = null;
        this.dataType = null;
        this.style = style;
        this.firstChar = (strings == null || strings.isEmpty()) ? 0 : strings.charAt(0);
    }

    public LexerType(String strings) {
        this.str = strings;
        this.cat = null;
        this.dataType = null;
        this.style = null;
        this.firstChar = (strings == null || strings.isEmpty()) ? 0 : strings.charAt(0);
    }

    public String getString() {
        return str;
    }

    public Category getCategory() {
        return cat;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getStyle() {
        if (style != null) {
            return style.styleClass();
        } else {
            return null;
        }
    }

}
