package com.eb.util;

import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 *
 * @author Earl Bosch
 */
public class Debugger {
// On/off flag

    public static volatile boolean DEBUG_ENABLED = false;
    public static volatile boolean DEBUG_TRACE = false;

// Optional log target; null => write to stdout
    private volatile java.nio.file.Path DEBUG_FILE_PATH;
    private volatile java.io.Writer DEBUG_WRITER = null;
    private final java.util.Deque<Long> debugStack = new java.util.ArrayDeque<>();
    private ScriptArea outputArea;
    private int currStackLevel = 0;

    public Debugger(String file, java.io.Writer debugWriter) {
        if (file != null) {
            DEBUG_FILE_PATH = Util.resolveSandboxedPath(file);
        }
        DEBUG_WRITER = debugWriter;
        ensureDebugWriter();
    }

    public Debugger(String file) {
        this(file, null);
    }

    public void setOutputArea(ScriptArea outputArea) {
        this.outputArea = outputArea;
    }

    public void setDebugFilePath(String debugFilePath) {
        java.nio.file.Path p = Util.resolveSandboxedPath(debugFilePath);
        this.DEBUG_FILE_PATH = p;
        closeDebugWriterQuietly();
        ensureDebugWriter();
    }

    public void setDebugNewFilePath(String debugFilePath) {
        java.nio.file.Path p = Util.resolveSandboxedPath(debugFilePath);
        this.DEBUG_FILE_PATH = p;
        closeDebugWriterQuietly();
        ensureDebugWriter(true);
    }

    public void setDebugWriter(Writer debugWriter) {
        this.DEBUG_WRITER = debugWriter;
        ensureDebugWriter();
    }

    public boolean setDebugOn() {
        DEBUG_ENABLED = true;
        return DEBUG_ENABLED;
    }

    public boolean setDebugOff() {
        DEBUG_ENABLED = false;
        return DEBUG_ENABLED;
    }

    public boolean isDebugOn() {
        return DEBUG_ENABLED;
    }

    public boolean setDebugTraceOn() {
        DEBUG_TRACE = true;
        return DEBUG_TRACE;
    }

    public boolean setDebugTraceOff() {
        DEBUG_TRACE = false;
        return DEBUG_TRACE;
    }

    public boolean isDebugTraceOn() {
        return DEBUG_TRACE;
    }

    public Path getDebugFilePath() {
        return DEBUG_FILE_PATH;
    }

    private synchronized void closeDebugWriterQuietly() {
        if (DEBUG_WRITER != null) {
            try {
                DEBUG_WRITER.close();
            } catch (Exception ignored) {
            }
            DEBUG_WRITER = null;
        }
    }

    private synchronized void ensureDebugWriter() {
        ensureDebugWriter(false);
    }

    /**
     * Lazily open the writer for DEBUG_FILE_PATH (create parent directories).
     */
    private synchronized void ensureDebugWriter(boolean newFile) {
        try {
            if (DEBUG_FILE_PATH == null) {  // stdout mode
                closeDebugWriterQuietly();
                return;
            }
            if (DEBUG_WRITER == null) {
                java.nio.file.Path parent = DEBUG_FILE_PATH.getParent();
                if (parent != null) {
                    java.nio.file.Files.createDirectories(parent);
                }
                if (newFile) {
                    DEBUG_WRITER = java.nio.file.Files.newBufferedWriter(
                            DEBUG_FILE_PATH,
                            java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                    );
                } else {
                    DEBUG_WRITER = java.nio.file.Files.newBufferedWriter(
                            DEBUG_FILE_PATH,
                            java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND
                    );
                }
            }
        } catch (IOException ex) {
            closeDebugWriterQuietly();
            DEBUG_WRITER = null;
            DEBUG_FILE_PATH = null;
        }
    }

    private static String indent(int level, int indent) {
        StringBuilder out = new StringBuilder();
        if (indent <= 0) {
            return "";
        }
        for (int i = 0; i < level * indent; i++) {
            out.append(' ');
        }
        return out.toString();
    }

    /**
     * Write one log line either to stdout or to the configured file.
     */
    public void debugWriteLine(String level, String message) {
        if (DEBUG_ENABLED) {
            String line = "";
            if (currStackLevel < debugStack.size()) {
                currStackLevel = debugStack.size();
                line = line + System.lineSeparator();
            }
            line = line + indent(currStackLevel, 2) + "[" + nowIso() + "]\t[" + levelOf(level) + "]\t" + messageOf(message) + System.lineSeparator();
            try {
                if (DEBUG_WRITER == null) {
                    if (outputArea != null) {
                        outputArea.print(line);
                    } else {
                        System.out.print(line);
                    }
                } else {
                    ensureDebugWriter();
                    DEBUG_WRITER.write(line);
                    DEBUG_WRITER.flush();
                }
            } catch (Exception ex) {
                throw new RuntimeException("debug.log: " + ex.getMessage());
            }
        }
    }

    public void debugWriteStart(String level, String message) {
        if (DEBUG_ENABLED) {
            StringBuilder line = new StringBuilder();
            line.append("[").append(nowIso()).append("]\t[").append(levelOf(level)).append("]\t").append(messageOf(message)).append(" [");
            try {
                if (DEBUG_WRITER == null) {
                    if (outputArea != null) {
                        outputArea.print(line.toString());
                    } else {
                        System.out.print(line.toString());
                    }
                } else {
                    ensureDebugWriter();
                    DEBUG_WRITER.write(line.toString());
                    DEBUG_WRITER.flush();
                }
                debugStack.push(System.nanoTime());
            } catch (Exception ex) {
                throw new RuntimeException("debug.log: " + ex.getMessage());
            }
        }
    }

    public void debugWriteEnd() {
        debugWriteEnd("");
    }

    public void debugWriteEnd(String message) {
        if (DEBUG_ENABLED) {
            long end = System.nanoTime();
            long start = debugStack.pop();
            currStackLevel--;
            String period = String.format("%.6f", (end - start) / 1000000000.0);
            String line = "] (" + period + "s) " + messageOf(message) + System.lineSeparator();
            try {
                if (DEBUG_WRITER == null) {
                    if (outputArea != null) {
                        outputArea.print(line);
                    } else {
                        System.out.print(line);
                    }                } else {
                    ensureDebugWriter();
                    DEBUG_WRITER.write(line);
                    DEBUG_WRITER.flush();
                }
            } catch (Exception ex) {
                throw new RuntimeException("debug.log: " + ex.getMessage());
            }
        }
    }

    private static String nowIso() {
        return java.time.LocalDateTime.now().toString();
    }

    private static String levelOf(Object lvl) {
        return (lvl == null) ? "INFO" : String.valueOf(lvl).toUpperCase();
    }

    private static String messageOf(Object msg) {
        return (msg == null) ? "" : String.valueOf(msg);
    }

}
