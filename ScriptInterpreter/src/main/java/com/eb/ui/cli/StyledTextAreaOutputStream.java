package com.eb.ui.cli;

import java.io.OutputStream;
import org.fxmisc.richtext.StyleClassedTextArea;

public class StyledTextAreaOutputStream extends OutputStream {
    private final StyleClassedTextArea area;
    private final String[] styleClasses;
    private final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(256);

    StyledTextAreaOutputStream(StyleClassedTextArea area, String... styleClasses) {
        this.area = area;
        this.styleClasses = styleClasses;
    }

    @Override public void write(int b) {
        if (b == '\n') flush(); else buf.write(b);
    }

    @Override public void flush() {
        byte[] bytes = buf.toByteArray();
        buf.reset();
        if (bytes.length == 0) return;

        String s = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        javafx.application.Platform.runLater(() -> {
            int start = area.getLength();
            area.appendText(s + "\n");
            int end = start + s.length() + 1;

            if (styleClasses == null || styleClasses.length == 0) {
                area.setStyleClass(start, end, "info"); // or pick your default
            } else if (styleClasses.length == 1) {
                area.setStyleClass(start, end, styleClasses[0]); // single class
            } else {
                area.setStyle(start, end, java.util.Arrays.asList(styleClasses)); // multiple classes
            }

            area.moveTo(area.getLength());
            area.requestFollowCaret();
        });
    }
}
