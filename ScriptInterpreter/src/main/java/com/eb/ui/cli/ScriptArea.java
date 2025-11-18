package com.eb.ui.cli;

import com.eb.util.MarkupTokenizer;
import com.eb.util.Util;
import java.util.List;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 *
 * @author Earl Bosch
 */
public class ScriptArea extends StyleClassedTextArea {

    private boolean showLineNumbers = true; // default: on

    public ScriptArea() {
        setParagraphGraphicFactory(LineNumberFactory.get(this)); // initial state ON
    }

    /**
     * Toggle line numbers on/off.
     */
    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
        if (show) {
            setParagraphGraphicFactory(LineNumberFactory.get(this));
        } else {
            setParagraphGraphicFactory(null);
        }
    }

    /**
     * Current state for line numbers.
     */
    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    /**
     * Convenience toggle.
     */
    public void toggleLineNumbers() {
        setShowLineNumbers(!showLineNumbers);
    }

    public void print(String line) {
        Util.runOnFx(() -> {
            var segments = MarkupTokenizer.tokenize(line);
            if (segments.isEmpty()) {
                return;
            }
            for (var seg : segments) {
                if (seg.styles == null || seg.styles.isEmpty()) {
                    printStyled(seg.text, "info");
                } else {
                    printStyled(seg.text, seg.styles);
                }
            }
        });
    }

    public void println(String line) {
        Util.runOnFx(() -> {
            var segments = MarkupTokenizer.tokenize(line);
            if (segments.isEmpty()) {
                this.appendText("\n");
                return;
            }
            for (var seg : segments) {
                if (seg.styles == null || seg.styles.isEmpty()) {
                    printStyled(seg.text, "info");
                } else {
                    printStyled(seg.text, seg.styles);
                }
            }
            this.appendText("\n");
        });
    }

    public void printlnInfo(String s) {
        printStyled(s + "\n", "info");
    }

    public void printlnWarn(String s) {
        printStyled(s + "\n", "warn");
    }

    public void printlnError(String s) {
        printStyled(s + "\n", "error");
    }

    public void printlnOk(String s) {
        printStyled(s + "\n", "ok");
    }

    public void printStyled(String text, String... styleClasses) {
        printStyled(text, java.util.Arrays.asList(styleClasses));
    }

    public void printStyled(String text, List<String> styleClasses) {
        Util.runOnFx(() -> {
            int start = this.getLength();
            this.appendText(text);
            int end = start + text.length() + 1; // +1 to include trailing newline added by callers

            if (styleClasses == null || styleClasses.isEmpty()) {
                this.setStyleClass(start, end, "info");
            } else if (styleClasses.size() == 1) {
                String s = styleClasses.get(0);
                if (s.length() == 1) {
                    styleClasses.add("info");
                    this.setStyle(start, end, styleClasses);
                } else {
                    this.setStyleClass(start, end, s);
                }
            } else {
                styleClasses.add(0, "info");
                this.setStyle(start, end, styleClasses); // multi-class API
            }

            this.moveTo(this.getLength());
            this.requestFollowCaret();
        });
    }

    public void addStyleToRange(int start, int endExclusive, String styleClass) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(getLength(), endExclusive);
        
        if (actualStart >= actualEnd) {
            return;
        }
        
        // Batch consecutive characters with same initial style to reduce setStyle calls
        int batchStart = actualStart;
        java.util.List<String> batchStyle = null;
        
        for (int i = actualStart; i < actualEnd; i++) {
            var curr = new java.util.ArrayList<>(getStyleOfChar(i));
            
            // Check if we need to start a new batch
            if (batchStyle != null && !curr.equals(batchStyle)) {
                // Apply previous batch
                if (!batchStyle.contains(styleClass)) {
                    batchStyle.add(styleClass);
                }
                setStyle(batchStart, i, batchStyle);
                batchStart = i;
                batchStyle = null;
            }
            
            // Initialize or continue batch
            if (batchStyle == null) {
                batchStyle = curr;
            }
        }
        
        // Apply final batch
        if (batchStyle != null) {
            if (!batchStyle.contains(styleClass)) {
                batchStyle.add(styleClass);
            }
            setStyle(batchStart, actualEnd, batchStyle);
        }
    }

    public void removeStyleFromRange(int start, int endExclusive, String styleClass) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(getLength(), endExclusive);
        
        if (actualStart >= actualEnd) {
            return;
        }
        
        // Batch consecutive characters with same initial style to reduce setStyle calls
        int batchStart = actualStart;
        java.util.List<String> batchStyle = null;
        
        for (int i = actualStart; i < actualEnd; i++) {
            var curr = new java.util.ArrayList<>(getStyleOfChar(i));
            
            // Check if we need to start a new batch
            if (batchStyle != null && !curr.equals(batchStyle)) {
                // Apply previous batch
                batchStyle.removeIf(c -> c.equals(styleClass));
                setStyle(batchStart, i, batchStyle);
                batchStart = i;
                batchStyle = null;
            }
            
            // Initialize or continue batch
            if (batchStyle == null) {
                batchStyle = curr;
            }
        }
        
        // Apply final batch
        if (batchStyle != null) {
            batchStyle.removeIf(c -> c.equals(styleClass));
            setStyle(batchStart, actualEnd, batchStyle);
        }
    }
}
