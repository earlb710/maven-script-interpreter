package com.eb.script.token;

/**
 *
 * @author Earl Bosch

 * Semantic styles for the console, mapped to CSS style-class names.
 * Add/rename entries here to keep UI style usage type-safe across the app.
 */
public enum PrintStyle {

    // Semantic lines
    INFO("info"),
    COMMENT("comment"),
    OK("ok"),
    WARN("warn"),
    ERR("err"),
    //formatting
    CODE("code"),
    DATA("data"),
    DATATYPE("datatype"),
    KEYWORD("keyword"),
    BUILTIN("builtin"),
    IDENTIFIER("identifier"),
    LITERAL("literal"),
    SQL("sql"),
    CUSTOM("custom"),

    // Prompt & input echoes
    PROMPT("prompt"),
    INPUT("input"),

    // Inline emphasis
    BOLD("b"),
    ITALIC("i"),
    UNDERLINE("u"),

    // Fallback (no class applied)
    DEFAULT(null);

    private final String styleClass;

    PrintStyle(String styleClass) {
        this.styleClass = styleClass;
    }

    /** The CSS style-class name (or null for DEFAULT). */
    public String styleClass() {
        return styleClass;
    }

    /** Convenience for RichTextFX append(...). */
    public String[] asArray() {
        return styleClass == null ? new String[0] : new String[]{ styleClass };
    }

    /** Maps a tag/short name to a style (useful if you parse markup). */
    public static PrintStyle fromTag(String tag) {
        if (tag == null) return DEFAULT;
        switch (tag.toLowerCase()) {
            case "info":   return INFO;
            case "ok":     return OK;
            case "warn":   return WARN;
            case "err":    return ERR;
            case "prompt": return PROMPT;
            case "input":  return INPUT;
            case "b":      return BOLD;
            case "i":      return ITALIC;
            case "u":      return UNDERLINE;
            case "code":   return CODE;
            default:       return DEFAULT;
        }
    }

    /** Maps an existing CSS class back to the enum (optional). */
    public static PrintStyle fromStyleClass(String styleClass) {
        if (styleClass == null) return DEFAULT;
        for (PrintStyle s : values()) {
            if (styleClass.equalsIgnoreCase(s.styleClass)) return s;
        }
        return DEFAULT;
    }
}
