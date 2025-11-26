package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.token.DataType;
import com.eb.util.Util;
import java.util.List;

/**
 * Built-in functions for String operations.
 * Handles all str.* and string.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsString {

    /**
     * Dispatch a String builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "str.toupper")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "str.tostring", "string.tostring" -> toString(args);
            case "str.toupper" -> toUpper(args);
            case "str.tolower" -> toLower(args);
            case "str.trim" -> trim(args);
            case "str.replace" -> replace(args);
            case "str.split" -> split(args);
            case "str.join" -> join(args);
            case "str.contains" -> contains(args);
            case "str.startswith" -> startsWith(args);
            case "str.endswith" -> endsWith(args);
            case "str.equalsignorecase" -> equalsIgnoreCase(args);
            case "str.equals" -> equals(args);
            case "str.isempty" -> isEmpty(args);
            case "str.isblank" -> isBlank(args);
            case "str.substring" -> substring(args);
            case "str.indexof" -> indexOf(args);
            case "str.lastindexof" -> lastIndexOf(args);
            case "str.charat" -> charAt(args);
            case "str.replaceall" -> replaceAll(args);
            case "str.lpad" -> lpad(args);
            case "str.rpad" -> rpad(args);
            case "str.chararray" -> charArray(args);
            default -> throw new InterpreterError("Unknown String builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a String builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("str.") || name.startsWith("string.");
    }

    // --- Individual builtin implementations ---

    private static Object toString(Object[] args) {
        return (args[0] == null) ? null : String.valueOf(args[0]);
    }

    private static Object toUpper(Object[] args) {
        String s = (String) args[0];
        return (s == null) ? null : s.toUpperCase();
    }

    private static Object toLower(Object[] args) {
        String s = (String) args[0];
        return (s == null) ? null : s.toLowerCase();
    }

    private static Object trim(Object[] args) {
        String s = (String) args[0];
        return (s == null) ? null : s.trim();
    }

    private static Object replace(Object[] args) {
        String s = (String) args[0];
        String tgt = (String) args[1];
        String rep = (String) args[2];
        if (s == null || tgt == null || rep == null) {
            return s;
        }
        return Util.notNull(s).replace(tgt, rep);
    }

    private static Object split(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        String pattern = (String) args[1];
        Integer limit = (Integer) args[2];
        if (limit == null) {
            limit = -1;
        }
        if (s == null || pattern == null) {
            return null;
        }
        try {
            String[] parts = s.split(pattern, limit);
            return new ArrayFixed(DataType.STRING, parts);
        } catch (java.util.regex.PatternSyntaxException ex) {
            throw new InterpreterError("Invalid regex: " + ex.getDescription());
        }
    }

    private static Object join(Object[] args) throws InterpreterError {
        Object a0 = args[0];
        String nl = (String) args[1];

        if (a0 == null) {
            return null;
        }
        if (nl == null) {
            throw new InterpreterError("str.join: delimiter cannot be null");
        }

        String[] sarray;

        if (a0 instanceof String[] sa) {
            sarray = sa;
            int n = sarray.length;
            for (int i = 0; i < n; i++) {
                if (sarray[i] == null) {
                    sarray[i] = "";
                }
            }
        } else if (a0 instanceof ArrayDef ad) {
            int n = ad.size();
            sarray = new String[n];
            for (int i = 0; i < n; i++) {
                Object el = ad.get(i);
                sarray[i] = (el == null) ? "" : el.toString();
            }
        } else if (a0 instanceof List<?> list) {
            sarray = list.stream().map(e -> e == null ? "" : e.toString()).toArray(String[]::new);
        } else {
            throw new InterpreterError("str.join: first argument must be an array/list of strings");
        }

        return String.join(nl, sarray);
    }

    private static Object contains(Object[] args) {
        String s = (String) args[0], sub = (String) args[1];
        if (s == null || sub == null) {
            return false;
        }
        return s.contains(sub);
    }

    private static Object startsWith(Object[] args) {
        String s = (String) args[0], p = (String) args[1];
        if (s == null || p == null) {
            return false;
        }
        return s.startsWith(p);
    }

    private static Object endsWith(Object[] args) {
        String s = (String) args[0], suf = (String) args[1];
        if (s == null || suf == null) {
            return false;
        }
        return s.endsWith(suf);
    }

    private static Object equalsIgnoreCase(Object[] args) {
        String a = (String) args[0], b = (String) args[1];
        return Util.strEqIgnore(a, b);
    }

    private static Object equals(Object[] args) {
        String a = (String) args[0], b = (String) args[1];
        return Util.strEq(a, b);
    }

    private static Object isEmpty(Object[] args) {
        String s = (String) args[0];
        return s != null && s.isEmpty();
    }

    private static Object isBlank(Object[] args) {
        String s = (String) args[0];
        return Util.isBlank(s);
    }

    private static Object substring(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        Integer beginIndex = (Integer) args[1];
        Integer endIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;

        if (s == null) {
            return null;
        }
        if (beginIndex == null) {
            throw new InterpreterError("str.substring: beginIndex cannot be null");
        }

        try {
            if (endIndex == null) {
                return s.substring(beginIndex);
            } else {
                return s.substring(beginIndex, endIndex);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new InterpreterError("str.substring: " + ex.getMessage());
        }
    }

    private static Object indexOf(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        String searchString = (String) args[1];
        Integer fromIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;

        if (s == null || searchString == null) {
            return -1;
        }

        try {
            if (fromIndex == null) {
                return s.indexOf(searchString);
            } else {
                return s.indexOf(searchString, fromIndex);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new InterpreterError("str.indexOf: " + ex.getMessage());
        }
    }

    private static Object lastIndexOf(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        String searchString = (String) args[1];
        Integer fromIndex = (args.length > 2 && args[2] != null) ? (Integer) args[2] : null;

        if (s == null || searchString == null) {
            return -1;
        }

        try {
            if (fromIndex == null) {
                return s.lastIndexOf(searchString);
            } else {
                return s.lastIndexOf(searchString, fromIndex);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new InterpreterError("str.lastIndexOf: " + ex.getMessage());
        }
    }

    private static Object charAt(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        Integer index = (Integer) args[1];

        if (s == null) {
            return null;
        }
        if (index == null) {
            throw new InterpreterError("str.charAt: index cannot be null");
        }

        try {
            char ch = s.charAt(index);
            return String.valueOf(ch);
        } catch (IndexOutOfBoundsException ex) {
            throw new InterpreterError("str.charAt: " + ex.getMessage());
        }
    }

    private static Object replaceAll(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        String regex = (String) args[1];
        String replacement = (String) args[2];

        if (s == null) {
            return null;
        }
        if (regex == null || replacement == null) {
            throw new InterpreterError("str.replaceAll: regex and replacement cannot be null");
        }

        try {
            return s.replaceAll(regex, replacement);
        } catch (java.util.regex.PatternSyntaxException ex) {
            throw new InterpreterError("str.replaceAll: Invalid regex - " + ex.getMessage());
        }
    }

    private static Object lpad(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        Integer length = (Integer) args[1];
        String padChar = (String) args[2];

        if (s == null) {
            return null;
        }
        if (length == null) {
            throw new InterpreterError("str.lpad: length cannot be null");
        }
        if (padChar == null || padChar.isEmpty()) {
            throw new InterpreterError("str.lpad: padChar cannot be null or empty");
        }
        if (padChar.length() != 1) {
            throw new InterpreterError("str.lpad: padChar must be a single character");
        }

        if (s.length() >= length) {
            return s;
        }

        int padCount = length - s.length();
        StringBuilder sb = new StringBuilder(length);
        char ch = padChar.charAt(0);
        for (int i = 0; i < padCount; i++) {
            sb.append(ch);
        }
        sb.append(s);
        return sb.toString();
    }

    private static Object rpad(Object[] args) throws InterpreterError {
        String s = (String) args[0];
        Integer length = (Integer) args[1];
        String padChar = (String) args[2];

        if (s == null) {
            return null;
        }
        if (length == null) {
            throw new InterpreterError("str.rpad: length cannot be null");
        }
        if (padChar == null || padChar.isEmpty()) {
            throw new InterpreterError("str.rpad: padChar cannot be null or empty");
        }
        if (padChar.length() != 1) {
            throw new InterpreterError("str.rpad: padChar must be a single character");
        }

        if (s.length() >= length) {
            return s;
        }

        int padCount = length - s.length();
        StringBuilder sb = new StringBuilder(length);
        sb.append(s);
        char ch = padChar.charAt(0);
        for (int i = 0; i < padCount; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static Object charArray(Object[] args) {
        String s = (String) args[0];

        if (s == null) {
            return null;
        }

        int length = s.length();
        Integer[] charCodes = new Integer[length];

        for (int i = 0; i < length; i++) {
            charCodes[i] = (int) s.charAt(i);
        }

        ArrayFixed result = new ArrayFixed(DataType.INTEGER, charCodes);
        return result;
    }
}
