package com.eb.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Minimal inline-markup tokenizer for the console.
 *
 * Supports tags:
 *   <b>...</b>       -> style "bold"
 *   <i>...</i>       -> style "italic"
 *   <u>...</u>       -> style "underline"
 *   <ok>...</ok>     -> style "ok"
 *   <warn>...</warn> -> style "warn"
 *   <err>...</err>   -> style "err"
 *   <code>...</code> -> style "code"
 *
 * Escaping: use "\\<" to output a literal '<'.
 * Unmatched/unknown tags are treated as literal text.
 */
public final class MarkupTokenizer {

    /** A styled segment of text to append to the console. */
    public static final class Segment {
        public final String text;
        public final List<String> styles;
        public Segment(String text, List<String> styles) {
            this.text = text;
            this.styles = styles;
        }
        @Override public String toString() { return "Segment(" + styles + ",'" + text + "')"; }
    }

    private MarkupTokenizer() {}

    // Map tag name -> style class
    private static String styleForTag(String tagName) {
        return switch (tagName.toLowerCase()) {
            case "b"     -> "b";
            case "i"     -> "i";
            case "u"     -> "u";
            case "ok"    -> "ok";
            case "warn"  -> "warn";
            case "err"   -> "err";
            case "code"  -> "code";
            default      -> null; // unknown -> no style mapping
        };
    }

    /**
     * Tokenize a line of markup into styled segments.
     * The algorithm is a single left-to-right pass with a simple tag stack.
     */
    public static List<Segment> tokenize(String line) {
        ArrayList<Segment> out = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return out;
        }
        
        // Replace <br> tags with newlines before processing
        line = line.replace("<br>", "\n");
        line = line.replace("<BR>", "\n");

        StringBuilder buf = new StringBuilder();
        Deque<String> styleStack = new ArrayDeque<>();

        int i = 0, n = line.length();
        while (i < n) {
            char c = line.charAt(i);

            // Escaped '<' -> literal '<'
            if (c == '\\' && i + 1 < n && line.charAt(i + 1) == '<') {
                buf.append('<');
                i += 2;
                continue;
            }

            // Tag start?
            if (c == '<') {
                int tagEnd = line.indexOf('>', i + 1);
                if (tagEnd > i + 1) {
                    String raw = line.substring(i + 1, tagEnd).trim(); // e.g. "b", "/b"
                    boolean closing = raw.startsWith("/");
                    String name = closing ? raw.substring(1).trim() : raw;

                    // Only accept simple tag names [a-zA-Z0-9_-]+
                    if (!name.isEmpty() && name.matches("[A-Za-z][A-Za-z0-9_-]*")) {
                        // Flush current buffer as a segment with current styles
                        if (buf.length() > 0) {
                            out.add(new Segment(buf.toString(), new ArrayList<>(styleStack)));
                            buf.setLength(0);
                        }
                        if (closing) {
                            // Pop the matching style if present
                            String style = styleForTag(name);
                            if (style != null && styleStack.contains(style)) {
                                // pop until we find the matching style, preserving nested order
                                Deque<String> tmp = new ArrayDeque<>();
                                while (!styleStack.isEmpty()) {
                                    String top = styleStack.pop();
                                    if (top.equals(style)) break;
                                    tmp.push(top);
                                }
                                while (!tmp.isEmpty()) styleStack.push(tmp.pop());
                            } else {
                                // Unknown/unmatched closing tag -> treat as literal
                                buf.append('<').append(raw).append('>');
                            }
                        } else {
                            // Opening tag
                            String style = styleForTag(name);
                            if (style != null) {
                                styleStack.push(style);
                            } else {
                                // Unknown opening tag -> literal
                                buf.append('<').append(raw).append('>');
                            }
                        }

                        i = tagEnd + 1;
                        continue;
                    }
                }
                // Not a valid tag -> literal '<'
                buf.append('<');
                i++;
                continue;
            }

            // Regular char
            buf.append(c);
            i++;
        }

        // Flush trailing text
        if (buf.length() > 0) {
            out.add(new Segment(buf.toString(), new ArrayList<>(styleStack)));
        }
        return out;
    }
}
