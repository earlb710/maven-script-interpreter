package com.eb.ui.ebs;

import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.token.ebs.EbsTokenType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 *
 * @author Earl Bosch
 */
public class EbsStyled {

    private static final EbsLexer lexerConsole = new EbsLexer();
    private static final EbsLexer lexer = new EbsLexer();

    static {
        lexerConsole.addCustomKeywords("custom", "echo", "debug", "list", "open", "close", "help", "clear", "reset");
        lexerConsole.addCustomChar("custom", '?');
    }

    public static List<EbsToken> tokenize(String text) {
        return lexer.tokenize(text);
    }

    public static List<EbsToken> tokenizeConsole(String text) {
        return lexerConsole.tokenize(text);
    }

    public static void appendStyledText(StyleClassedTextArea textArea, String text) {
        text = text.replace("\r\n", "\n");
        List<EbsToken> tokens = lexer.tokenize(text);
        appendStyledText(textArea, text, tokens);
    }

    public static void appendStyledText(StyleClassedTextArea textArea, String text, List<EbsToken> tokens) {
        int addStart = textArea.getLength();
        textArea.appendText(text);
        int totalLength = textArea.getLength();

        int last = 0;
        for (EbsToken tok : tokens) {
            if (tok.type == EbsTokenType.EOF) {
                break;
            }
            // Adjust to your getters:
            int start = tok.start;               // or tok.getStart(), tok.getOffset()
            int end = tok.end + 1;               // exclusive; or start + tok.length()
            //String part = text.substring(start, end);
            String style = tok.style;
            // Guardrails
            if (start < last) {
                continue;                  // skip overlaps
            }
            if (start > totalLength) {
                break;
            }
            if (end > totalLength) {
                end = totalLength;  // clip
            }
            if (style == null) {
                textArea.setStyleClass(start + addStart, end + addStart, "info");
            } else {
                textArea.setStyleClass(start + addStart, end + addStart, style);
            }

            last = end;
        }
    }

    /**
     * Convert tokens to RichTextFX StyleSpans: - Unstyled gaps are added with
     * empty style collection. - Each token contributes a single style: the CSS
     * class returned by Token.style. Adjust getters (start/end/style) to your
     * Token API.
     */
    public static StyleSpans<Collection<String>> toStyleSpans(List<EbsToken> tokens, int totalLength, int addStart) {
        int count = 0;
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        if (addStart > 0) {
            spans.add(Collections.emptyList(), addStart);
        }
        int last = 0;
        for (EbsToken tok : tokens) {
            // Adjust to your getters:
            int start = tok.start;          // or tok.getStart(), tok.getOffset()
            int end = tok.end + 1;            // exclusive; or start + tok.length()
            String style = tok.style;       // or tok.getStyle()

            // Guardrails
            if (start < last) {
                continue;                  // skip overlaps
            }
            if (start > totalLength) {
                break;
            }
            if (end > totalLength) {
                end = totalLength;  // clip
            }
            // Fill gap (unstyled) - use "info" style for default appearance
            if (start > last) {
                spans.add(Collections.singleton("info"), start - last);
                count++;
            }
            // Apply the token's CSS class
            if (style == null) {
                spans.add(Collections.singleton("info"), end - start);
                count++;
            } else {
                spans.add(Collections.singleton(style), end - start);
                count++;
            }

            last = end;
        }

        // Trailing gap - use "info" style for default appearance
        if (last < totalLength) {
            spans.add(Collections.singleton("info"), totalLength - last);
            count++;
        }
        if (count > 0) {
            StyleSpans<Collection<String>> ret = spans.create();
            return ret;
        } else {
            return null;
        }
    }

}
