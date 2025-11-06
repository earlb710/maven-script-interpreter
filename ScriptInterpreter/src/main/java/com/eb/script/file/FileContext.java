package com.eb.script.file;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 *
 * @author Earl Bosch
 */
public final class FileContext {

    public final String handle;
    public final Path path;
    public final String mode; // "r", "w", "a", "rw"
    public final FileSystem file;
    public final SeekableByteChannel chan;
    public final long openedMs;
    public long pos;
    public long size;
    public boolean closed;

    public FileContext(String handle, Path path, String mode) {
        this.handle = handle;
        this.path = path;
        this.mode = mode;
        this.chan = null;
        this.file = null;
        this.closed = (handle == null);
        this.openedMs = System.currentTimeMillis();
    }

    public FileContext(String handle, Path path, String mode, FileSystem fs) {
        this.handle = handle;
        this.path = path;
        this.mode = mode;
        this.chan = null;
        this.file = fs;
        this.closed = (handle == null);
        this.openedMs = System.currentTimeMillis();
    }

    public FileContext(String handle, Path path, String mode, SeekableByteChannel ch) {
        this.handle = handle;
        this.path = path;
        this.mode = mode;
        this.chan = ch;
        this.file = null;
        this.closed = (handle == null);
        this.openedMs = System.currentTimeMillis();
    }
}
