package com.eb.script.token.ebs;

import com.eb.script.token.Category;
import com.eb.script.token.DataType;
import com.eb.script.token.LexerType;
import com.eb.script.token.PrintStyle;
import com.eb.script.token.LexerToken;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 *
 * @author Earl Bosch
 */
public enum EbsTokenType implements LexerToken {

    // Symbols
    SEMICOLON(";"), LBRACE("{"), RBRACE("}"), COLON(":"), COMMA(","), DOT("."), LPAREN("("), RPAREN(")"), LBRACKET("["), RBRACKET("]"),
    // Keywords
    VAR(PrintStyle.KEYWORD, Category.KEYWORD, "var", "let"), CONST(PrintStyle.KEYWORD, Category.KEYWORD, "const"), PRINT(PrintStyle.KEYWORD, Category.KEYWORD, "print"), CALL(PrintStyle.KEYWORD, Category.KEYWORD, "call", "#"), RETURN(PrintStyle.KEYWORD, Category.KEYWORD, "return"),
    IMPORT(PrintStyle.KEYWORD, Category.KEYWORD, "import"),
    FUNCTION(PrintStyle.KEYWORD, Category.KEYWORD, "function"),
    TYPEOF(PrintStyle.KEYWORD, Category.KEYWORD, "typeof"),
    // Conditional
    IF(PrintStyle.KEYWORD, Category.KEYWORD, "if"), THEN(PrintStyle.KEYWORD, Category.KEYWORD, "then"), ELSE(PrintStyle.KEYWORD, Category.KEYWORD, "else"),
    // Loop
    FOR(PrintStyle.KEYWORD, Category.KEYWORD, "for"),
    FOREACH(PrintStyle.KEYWORD, Category.KEYWORD, "foreach"), IN(PrintStyle.KEYWORD, Category.KEYWORD, "in"),
    WHILE(PrintStyle.KEYWORD, Category.KEYWORD, "while"), DO(PrintStyle.KEYWORD, Category.KEYWORD, "do"), BREAK(PrintStyle.KEYWORD, Category.KEYWORD, "break", "exit"), CONTINUE(PrintStyle.KEYWORD, Category.KEYWORD, "continue"),
    // Exception handling
    TRY(PrintStyle.KEYWORD, Category.KEYWORD, "try"),
    EXCEPTIONS(PrintStyle.KEYWORD, Category.KEYWORD, "exceptions"),
    WHEN(PrintStyle.KEYWORD, Category.KEYWORD, "when"),
    RAISE(PrintStyle.KEYWORD, Category.KEYWORD, "raise"),
    EXCEPTION(PrintStyle.KEYWORD, Category.KEYWORD, "exception"),
    // Comments
    COMMENT(PrintStyle.COMMENT, "//"),
    // data types
    QUOTE1(PrintStyle.DATATYPE, "\""), QUOTE2(PrintStyle.DATATYPE, "'"),
    BYTE(PrintStyle.DATA, DataType.BYTE, "byte"), INTEGER(PrintStyle.DATA, DataType.INTEGER, "int", "integer"), LONG(PrintStyle.DATA, DataType.LONG, "long"), FLOAT(PrintStyle.DATA, DataType.FLOAT, "float"), DOUBLE(PrintStyle.DATA, DataType.DOUBLE, "double"),
    STRING(PrintStyle.DATA, DataType.STRING, "string"), DATE(PrintStyle.DATA, DataType.DATE, "date"), BOOL(PrintStyle.DATA, DataType.BOOL, "bool", "boolean"),
    JSON(PrintStyle.DATA, DataType.JSON, "json"),
    ARRAY(PrintStyle.DATA, DataType.ARRAY, "array"),
    QUEUE(PrintStyle.DATA, DataType.QUEUE, "queue"),
    RECORD(PrintStyle.DATA, DataType.RECORD, "record"),
    MAP(PrintStyle.DATA, DataType.MAP, "map"),
    // Identifiers
    IDENTIFIER(PrintStyle.INFO, ""),
    BUILTIN(PrintStyle.BUILTIN, ""),
    DATATYPE(PrintStyle.DATATYPE, ""),
    NULL(Category.KEYWORD, "null"), //null
    // Boolean Operators
    BOOL_TRUE(PrintStyle.DATA, Category.KEYWORD, "true"), //true
    BOOL_FALSE(PrintStyle.DATA, Category.KEYWORD, "false"), //false
    BOOL_BANG(Category.OPERATOR, "!"), //!
    BOOL_GT(Category.OPERATOR, ">"), BOOL_LT(Category.OPERATOR, "<"), // > <
    BOOL_GT_EQ(Category.OPERATOR, ">=", "=>"), BOOL_LT_EQ(Category.OPERATOR, "<=", "=<"), // >= <=
    BOOL_EQ(Category.OPERATOR, "=="), //==
    BOOL_NEQ(Category.OPERATOR, "!="),
    BOOL_AND(Category.OPERATOR, "and", "&&"),
    BOOL_OR(Category.OPERATOR, "or", "||"),
    // Operators
    PLUS(Category.OPERATOR, "+"), MINUS(Category.OPERATOR, "-"), STAR(Category.OPERATOR, "*"), SLASH(Category.OPERATOR, "/"), EQUAL(Category.OPERATOR, "="), CARET(Category.OPERATOR, "^"),
    PLUS_PLUS(Category.OPERATOR, "++"), MINUS_MINUS(Category.OPERATOR, "--"),
    PLUS_EQUAL(Category.OPERATOR, "+="), MINUS_EQUAL(Category.OPERATOR, "-="), STAR_EQUAL(Category.OPERATOR, "*="), SLASH_EQUAL(Category.OPERATOR, "/="),
    // --- SQL keywords ---
    CONNECT(PrintStyle.SQL, Category.KEYWORD, "connect"),
    USE(PrintStyle.SQL, Category.KEYWORD, "use"),
    CURSOR(PrintStyle.SQL, Category.KEYWORD, "cursor"),
    OPEN(PrintStyle.SQL, Category.KEYWORD, "open"),
    CLOSE(PrintStyle.SQL, Category.KEYWORD, "close"),
    CONNECTION(PrintStyle.SQL, Category.KEYWORD, "connection"),
    SELECT(PrintStyle.SQL, Category.KEYWORD, "select"),
    FROM(PrintStyle.SQL, Category.KEYWORD, "from"),
    WHERE(PrintStyle.SQL, Category.KEYWORD, "where"),
    ORDER(PrintStyle.SQL, Category.KEYWORD, "order"),
    BY(PrintStyle.SQL, Category.KEYWORD, "by"),
    GROUP(PrintStyle.SQL, Category.KEYWORD, "group"),
    HAVING(PrintStyle.SQL, Category.KEYWORD, "having"),
    // --- UI keywords ---
    SCREEN(PrintStyle.KEYWORD, Category.KEYWORD, "screen"),
    SHOW(PrintStyle.KEYWORD, Category.KEYWORD, "show"),
    HIDE(PrintStyle.KEYWORD, Category.KEYWORD, "hide"),
    SUBMIT(PrintStyle.KEYWORD, Category.KEYWORD, "submit"),
    CALLBACK(PrintStyle.KEYWORD, Category.KEYWORD, "callback"),
    // --- Property keywords ---
    LENGTH(PrintStyle.KEYWORD, Category.KEYWORD, "length"),
    SIZE(PrintStyle.KEYWORD, Category.KEYWORD, "size"),
    // Others
    CUSTOM(PrintStyle.CUSTOM),
    EOF("\\0");

    public final LexerType[] lexerTypes;

    private EbsTokenType(PrintStyle style, DataType dataType, String... str) {
        lexerTypes = new LexerType[str.length];
        int idx = 0;
        for (String s : str) {
            lexerTypes[idx] = new LexerType(Category.DATATYPE, dataType, style, s);
            idx++;
        }
    }

    private EbsTokenType(PrintStyle style, Category cat, String... str) {
        lexerTypes = new LexerType[str.length];
        int idx = 0;
        for (String s : str) {
            lexerTypes[idx] = new LexerType(cat, null, style, s);
            idx++;
        }
    }

    private EbsTokenType(PrintStyle style, String... str) {
        lexerTypes = new LexerType[str.length];
        int idx = 0;
        for (String s : str) {
            lexerTypes[idx] = new LexerType(null, null, style, s);
            idx++;
        }
    }

    private EbsTokenType(Category cat, String... str) {
        lexerTypes = new LexerType[str.length];
        int idx = 0;
        for (String s : str) {
            lexerTypes[idx] = new LexerType(cat, null, null, s);
            idx++;
        }
    }

    private EbsTokenType(String... str) {
        lexerTypes = new LexerType[str.length];
        int idx = 0;
        for (String s : str) {
            lexerTypes[idx] = new LexerType(null, null, null, s);
            idx++;
        }
    }

    @Override
    public LexerType[] getLexerType() {
        return lexerTypes;
    }

    public String[] getStrings() {
        String[] ret = new String[lexerTypes.length];
        int idx = 0;
        for (LexerType l : lexerTypes) {
            ret[idx] = l.getString();
            idx++;
        }
        return ret;
    }

    public String getStyle() {
        if (lexerTypes.length > 0) {
            return lexerTypes[0].getStyle();
        } else {
            return null;
        }
    }

    public Category getCategory() {
        if (lexerTypes.length > 0) {
            return lexerTypes[0].getCategory();
        } else {
            return null;
        }
    }

    public DataType getDataType() {
        if (lexerTypes.length > 0) {
            return lexerTypes[0].getDataType();
        } else {
            return null;
        }
    }

    public char getFirstChar() {
        if (lexerTypes.length > 0) {
            return lexerTypes[0].firstChar;
        } else {
            return 0;
        }
    }

    public boolean contains(String comp) {
        for (LexerType l : lexerTypes) {
            if (l.getString().equals(comp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkTokenType(EbsTokenType type, Object value) {
        if (null != type && value != null) {
            switch (type) {
                case INTEGER:
                    return (value instanceof Integer);
                case LONG:
                    return (value instanceof Long || value instanceof Integer);
                case STRING:
                    return (value instanceof String);
                case BOOL:
                    return (value instanceof Boolean);
                case FLOAT:
                    return (value instanceof Float);
                case DOUBLE:
                    return (value instanceof Double || value instanceof Float);
                case DATE:
                    return (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof Date);
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        if (lexerTypes != null) {
            return lexerTypes[0].getString();
        } else {
            return "";
        }
    }

}
