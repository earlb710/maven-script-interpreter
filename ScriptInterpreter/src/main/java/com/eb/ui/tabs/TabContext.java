package com.eb.ui.tabs;

import com.eb.script.file.FileContext;
import java.nio.file.Path;

/**
 *
 * @author Earl Bosch
 */
public class TabContext {

    public enum TabType {
        CONSOLE, FILE, SCREEN
    }
    public final String name;
    public final TabType type;
    public final Path path;
    public FileContext fileContext;

    public TabContext(String name, Path path, FileContext fileContext) {
        if (name == null) {
            name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        }
        this.name =name;
        this.type = TabType.FILE;
        this.path = path;
        this.fileContext = fileContext;
    }
}
