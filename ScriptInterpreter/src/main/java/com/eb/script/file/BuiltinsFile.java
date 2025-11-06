package com.eb.script.file;

import com.eb.script.file.FileContext;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author Earl Bosch
 */
public class BuiltinsFile {

    public static final int BUFFER_SIZE = 256 * 1024;

    public static String fileOpen(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        String mode = (String) (args.length > 1 ? args[1] : null);
        if (path == null) {
            return null;
        }
        if (mode == null || mode.isBlank()) {
            mode = "r";
        }

        try {
            Path p = Util.resolveSandboxedPath(path);
            Set<OpenOption> opts = new HashSet<>();
            switch (mode) {
                case "r" ->
                    opts.add(StandardOpenOption.READ);
                case "w" -> {
                    opts.add(StandardOpenOption.WRITE);
                    opts.add(StandardOpenOption.CREATE);
                    opts.add(StandardOpenOption.TRUNCATE_EXISTING);
                }
                case "a" -> {
                    opts.add(StandardOpenOption.WRITE);
                    opts.add(StandardOpenOption.CREATE);
                    opts.add(StandardOpenOption.APPEND);
                }
                case "rw" -> {
                    opts.add(StandardOpenOption.READ);
                    opts.add(StandardOpenOption.WRITE);
                    opts.add(StandardOpenOption.CREATE);
                }
                default ->
                    throw new IllegalArgumentException("file.open: unsupported mode '" + mode + "'");
            }
            SeekableByteChannel ch = Files.newByteChannel(p, opts);
            String handle = "fd-" + java.util.UUID.randomUUID();
            FileContext of = new FileContext(handle, p, mode, ch);
            env.registerOpenedFile(of);
            return handle;
        } catch (Exception ex) {
            throw new InterpreterError("file.open: " + Util.formatExceptionWithOrigin(ex));
        }
    }

    public static Boolean fileClose(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];

        if (key == null) {
            return Boolean.FALSE;
        }
        try {
            boolean ok = env.closeOpenedFile(key);
            return ok ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception ex) {
            throw new InterpreterError("file.close: " + ex.getMessage());
        }
    }

    public static List<Map<String, Object>> listOpenFiles(Environment env, Object... args) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FileContext of : env.getOpenFiles().values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("handle", of.handle);
            m.put("path", of.path.toString());
            m.put("mode", of.mode);
            boolean isOpen = of.chan.isOpen();
            m.put("isOpen", isOpen);
            try {
                long pos = isOpen ? of.chan.position() : 0L;
                m.put("position", pos);
            } catch (Exception ignore) {
                m.put("position", 0L);
            }
            try {
                long size = isOpen ? of.chan.size() : 0L;
                m.put("size", size);
            } catch (Exception ignore) {
                m.put("size", 0L);
            }
            m.put("openedMs", of.openedMs);
            out.add(m);
        }
        return out; // DataType.JSON
    }
    //  file.readln(handleOrPath, encoding?) -> STRING | null
    //  Returns the next text line (without EOL), or null if EOF is reached.
    //  Default encoding = UTF-8.

    public static String readln(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        String enc = (String) (args.length > 1 ? args[1] : null);
        if (key == null) {
            return null;
        }

        FileContext of = env.findOpenedFileByHandleOrPath(key);
        if (of == null || of.chan == null || !of.chan.isOpen()) {
            throw new InterpreterError("file.readln: not open: " + key);
        }
        // Must be readable
        if (!of.mode.contains("r")) {
            throw new InterpreterError("file.readln: handle not opened for reading: " + key);
        }

        try {
            Charset cs = (enc == null || enc.isBlank()) ? StandardCharsets.UTF_8 : Charset.forName(enc);
            return readLine(of.chan, cs);  // may return null at EOF
        } catch (Exception ex) {
            throw new InterpreterError("file.readln: " + ex.getMessage());
        }
    }

    public static String read(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        String enc = (String) (args.length > 1 ? args[1] : null);
        if (key == null) {
            return null;
        }

        FileContext of = env.findOpenedFileByHandleOrPath(key);
        if (of == null || of.chan == null || !of.chan.isOpen()) {
            throw new InterpreterError("file.readln: not open: " + key);
        }
        // Must be readable
        if (!of.mode.contains("r")) {
            throw new InterpreterError("file.readln: handle not opened for reading: " + key);
        }

        try {
            Charset cs = (enc == null || enc.isBlank()) ? StandardCharsets.UTF_8 : Charset.forName(enc);
            return readAll(of.chan, cs);  // may return null at EOF
        } catch (Exception ex) {
            throw new InterpreterError("file.read: " + ex.getMessage());
        }
    }
    //  file.writeln(handleOrPath, text, encoding?) -> BOOL
    //  Writes text + '\n' using the specified encoding (default UTF-8).
    //  Requires a write-capable handle ("w", "a", or "rw").

    public static Boolean writeln(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        String text = (String) args[1];
        String enc = (String) (args.length > 2 ? args[2] : null);

        if (key == null) {
            return Boolean.FALSE;
        }

        FileContext of = env.findOpenedFileByHandleOrPath(key);
        if (of == null || of.chan == null || !of.chan.isOpen()) {
            throw new InterpreterError("file.writeln: not open: " + key);
        }
        // Write-capable?
        if (!(of.mode.contains("w") || of.mode.contains("a"))) {
            throw new InterpreterError("file.writeln: handle not opened for writing: " + key);
        }

        try {
            Charset cs = (enc == null || enc.isBlank()) ? StandardCharsets.UTF_8 : Charset.forName(enc);
            // If append mode, ensure we write at EOF
            if (of.mode.contains("a")) {
                long size = of.chan.size();
                of.chan.position(size);
            }
            // Write text then LF
            byte[] bytes = (text == null ? new byte[0] : text.getBytes(cs));
            writeBytes(of.chan, bytes);
            writeBytes(of.chan, new byte[]{(byte) '\n'});
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.writeln: " + ex.getMessage());
        }
    }

    public static Boolean write(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        String text = (String) args[1];
        String enc = (String) (args.length > 2 ? args[2] : null);

        if (key == null) {
            return Boolean.FALSE;
        }

        FileContext of = env.findOpenedFileByHandleOrPath(key);
        if (of == null || of.chan == null || !of.chan.isOpen()) {
            throw new InterpreterError("file.write: not open: " + key);
        }
        // Write-capable?
        if (!(of.mode.contains("w") || of.mode.contains("a"))) {
            throw new InterpreterError("file.write: handle not opened for writing: " + key);
        }

        try {
            Charset cs = (enc == null || enc.isBlank()) ? StandardCharsets.UTF_8 : Charset.forName(enc);
            // If append mode, ensure we write at EOF
            if (of.mode.contains("a")) {
                long size = of.chan.size();
                of.chan.position(size);
            }
            // Write text then LF
            byte[] bytes = (text == null ? new byte[0] : text.getBytes(cs));
            writeBytes(of.chan, bytes);
            of.chan.close();
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.write: " + ex.getMessage());
        }
    }

    //  file.eof(handleOrPath) -> BOOL
    //  True if channel position >= channel size, else false.
    public static Boolean eof(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        if (key == null) {
            return Boolean.TRUE; // null treated as EOF/invalid
        }
        FileContext of = env.findOpenedFileByHandleOrPath(key);
        if (of == null || of.chan == null || !of.chan.isOpen()) {
            return Boolean.TRUE; // not open -> treat as EOF
        }
        try {
            long pos = of.chan.position();
            long size = of.chan.size();
            return (pos >= size) ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception ex) {
            throw new InterpreterError("file.eof: " + ex.getMessage());
        }
    }

    //  string.readTextFile(path) -> STRING (UTF-8)
    public static String readTextFile(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        FileData data = readTextFile(env, path);
        return data.stringData;
    }

    public static FileData readTextFile(String filePath) throws InterpreterError, IOException {
        Path p = Util.resolveSandboxedPath(filePath);
        String ret = Files.readString(p, StandardCharsets.UTF_8);
        FileContext opened = new FileContext(null, p, "r");
        opened.size = ret.length();
        opened.pos = opened.size;
        opened.closed = true;
        return new FileData(opened, ret);
    }

    public static FileData readTextFile(Environment env, String filePath) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        if (filePath == null || filePath.isBlank()) {
            return null;  // null-safe like other string builtins
        }
        try {
            FileData ret = readTextFile(filePath);
            if (env.isEchoOn()) {
                sysOutput(output, "Read " + ret.stringData.length() + " characters from text file " + ret.fileContext.path.toString());
            }
            return ret;
        } catch (Exception ex) {
            throw new InterpreterError("file.readFile: " + ex.getMessage());
        }
    }

    public static FileData readBinFile(String filePath) throws InterpreterError, IOException {
        Path p = Util.resolveSandboxedPath(filePath);
        byte[] buf = Files.readAllBytes(p);
        FileContext opened = new FileContext(null, p, "r");
        opened.size = buf.length;
        opened.pos = opened.size;
        opened.closed = true;
        return new FileData(opened, buf);
    }

    public static ArrayFixedByte readBinFile(Environment env, Object... args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String path = (String) args[0];
        if (path == null) {
            return null;  // null-safe like other string builtins
        }
        try {
            FileData ret = readBinFile(path);
            if (env.isEchoOn()) {
                sysOutput(output, "Read " + ret.byteData.length + " bytes from binary file " + ret.getPathString());
            }
            return new ArrayFixedByte(ret.byteData);
        } catch (Exception ex) {
            throw new InterpreterError("file.readBinFile: " + ex.getMessage());
        }
    }
    //  string.writeFile(path, content) -> BOOL (UTF-8)

    public static Boolean writeTextFile(Environment env, Object... args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String path = (String) args[0];
        String content = (String) args[1];
        if (path == null) {
            return Boolean.FALSE; // path missing -> fail gracefully
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            // Create parent directory if needed (optional)
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (content == null) {
                content = "";
            }
            Files.writeString(
                    p,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            if (env.isEchoOn()) {
                sysOutput(output, "Write " + content.length() + " characters to text file " + p.toString());
            }
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.writeFile: " + ex.getMessage());
        }
    }

    public static Boolean writeBinFile(Environment env, Object... args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String path = (String) args[0];
        byte[] content = null;
        if (args[1] instanceof ArrayFixedByte array) {
            content = array.elements;
        } else if (args[1] instanceof byte[] array) {
            content = array;
        }
        if (args[1] instanceof ArrayDef array) {

        }
        if (path == null) {
            return Boolean.FALSE; // path missing -> fail gracefully
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            // Create parent directory if needed (optional)
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (content != null) {
                Files.write(
                        p,
                        content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
                if (env.isEchoOn()) {
                    sysOutput(output, "Write " + content.length + " bytes to binary file " + p.toString());
                }
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }

        } catch (Exception ex) {
            throw new InterpreterError("file.writeBinFile: " + ex.getMessage());
        }
    }

    public static Boolean appendToTextFile(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        String content = (String) args[1];
        if (path == null) {
            return Boolean.FALSE;
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(p, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.appendToFile: " + ex.getMessage());
        }
    }

    public static List<Map<String, Object>> listFiles(Environment env, Object... args) throws InterpreterError {
        String path = ".";
        if (args.length > 0) {
            path = (String) args[0];
        }
        if (path == null) {
            path = ".";
        }
        try {
            Path dir = Util.resolveSandboxedPath(path);
            List<Map<String, Object>> out = new ArrayList<>();

            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.forEach(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", p.getFileName().toString());
                        boolean isDir = Files.isDirectory(p);
                        m.put("isDir", isDir);
                        try {
                            m.put("size", isDir ? 0L : Files.size(p));
                            m.put("modified", Util.formatDate(Files.getLastModifiedTime(p)));
                        } catch (Exception ignore) {
                            m.put("size", 0L);
                            m.put("modified", 0L);
                        }
                        out.add(m);
                    });
                }
            } else if (Files.exists(dir)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", dir.getFileName().toString());
                boolean isDir = Files.isDirectory(dir);
                m.put("isDir", isDir);
                m.put("size", isDir ? 0L : Files.size(dir));
                m.put("modifiedMs", Files.getLastModifiedTime(dir).toMillis());
                out.add(m);
            }
            return out;  // DataType.JSON
        } catch (Exception ex) {
            throw new InterpreterError("file.listFiles: " + ex.getMessage());
        }
    }
    //  file.rename(path, newName) -> BOOL
    //  NOTE: newName is the filename only (renames within the same directory)

    public static Boolean rename(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        String newName = (String) args[1];
        if (path == null || newName == null || newName.isBlank()) {
            return Boolean.FALSE;
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            Path target = p.resolveSibling(newName);
            // No overwrite by default; change to REPLACE_EXISTING if desired
            Files.move(p, target);
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.rename: " + ex.getMessage());
        }
    }

    //  file.move(source, dest) -> BOOL
    //  Moves source to dest (creates parent directories of dest if needed).
    public static Boolean move(Environment env, Object... args) throws InterpreterError {
        String source = (String) args[0];
        String dest = (String) args[1];
        if (source == null || dest == null) {
            return Boolean.FALSE;
        }
        try {
            Path s = Util.resolveSandboxedPath(source);
            Path d = Util.resolveSandboxedPath(dest);
            Path parent = d.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Overwrite if dest exists; drop REPLACE_EXISTING if you want a strict move
            Files.move(s, d, StandardCopyOption.REPLACE_EXISTING);
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.move: " + ex.getMessage());
        }
    }

    //  file.copy(source, dest) -> BOOL
    //  Copies source to dest (creates parent directories; overwrites).
    public static Boolean copy(Environment env, Object... args) throws InterpreterError {
        String source = (String) args[0];
        String dest = (String) args[1];
        if (source == null || dest == null) {
            return Boolean.FALSE;
        }
        try {
            Path s = Util.resolveSandboxedPath(source);
            Path d = Util.resolveSandboxedPath(dest);
            Path parent = d.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(s, d, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return Boolean.TRUE;
        } catch (Exception ex) {
            throw new InterpreterError("file.copy: " + ex.getMessage());
        }
    }

    // file.openZip(path, mode?) -> STRING handle
    public static String openZip(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        String mode = (String) (args.length > 1 ? args[1] : null);
        if (path == null) {
            return null;
        }
        if (mode == null || mode.isBlank()) {
            mode = "r";
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            // Open a read-only ZIP filesystem using the JDK zipfs provider
            java.util.Map<String, String> fsenv = new LinkedHashMap<>();
            fsenv.put("create", "false");
            java.net.URI uri = java.net.URI.create("jar:" + p.toUri().toString());
            java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(uri, fsenv);

            String handle = "zd-" + java.util.UUID.randomUUID();
            FileContext oz = new FileContext(handle, p, mode, fs);
            env.registerOpenedFile(oz);
            return handle;
        } catch (Exception ex) {
            throw new InterpreterError("file.openZip: " + com.eb.util.Util.formatExceptionWithOrigin(ex));
        }
    }

    public static String createZip(Environment env, Object... args) throws InterpreterError {
        String path = (String) args[0];
        String mode = (String) (args.length > 1 ? args[1] : null);
        if (path == null) {
            return null;
        }
        if (mode == null || mode.isBlank()) {
            mode = "rw";
        }
        try {
            Path p = Util.resolveSandboxedPath(path);
            // Open a read-only ZIP filesystem using the JDK zipfs provider
            java.util.Map<String, String> fsenv = new java.util.LinkedHashMap<>();
            fsenv.put("create", "true");
            java.net.URI uri = java.net.URI.create("jar:" + p.toUri().toString());
            java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(uri, fsenv);

            String handle = "zd-" + java.util.UUID.randomUUID();
            FileContext oz = new FileContext(handle, p, mode, fs);
            env.registerOpenedFile(oz);
            return handle;
        } catch (Exception ex) {
            throw new InterpreterError("file.createZip: " + com.eb.util.Util.formatExceptionWithOrigin(ex));
        }
    }

    // file.listZipFiles(handleOrPath, path?) -> JSON
    // Returns List<Map<String,Object>> with fields: name, isDir, size, modifiedMs
    public static List<Map<String, Object>> listZipFiles(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        String inner = (args.length > 1) ? (String) args[1] : null;
        if (key == null) {
            return List.of(); // graceful empty
        }

        FileContext oz = env.findOpenedFileByHandleOrPath(key);
        if (oz == null || oz.file == null) {
            throw new InterpreterError("file.listZipFiles: not open: " + key);
        }
        try {
            java.nio.file.Path dir = (inner == null || inner.isBlank())
                    ? oz.file.getPath("/")
                    : oz.file.getPath(inner);

            List<Map<String, Object>> out = new ArrayList<>();
            if (Files.isDirectory(dir)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = Files.list(dir)) {
                    stream.forEach(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", p.getFileName() == null ? p.toString() : p.getFileName().toString());
                        boolean isDir = java.nio.file.Files.isDirectory(p);
                        m.put("isDir", isDir);
                        try {
                            m.put("size", isDir ? 0L : Files.size(p));
                            m.put("modifiedMs", Files.getLastModifiedTime(p).toMillis());
                        } catch (Exception ignore) {
                            m.put("size", 0L);
                            m.put("modifiedMs", 0L);
                        }
                        out.add(m);
                    });
                }
            } else if (Files.exists(dir)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", dir.getFileName() == null ? dir.toString() : dir.getFileName().toString());
                boolean isDir = Files.isDirectory(dir);
                m.put("isDir", isDir);
                m.put("size", isDir ? 0L : Files.size(dir));
                m.put("modifiedMs", Files.getLastModifiedTime(dir).toMillis());
                out.add(m);
            }
            return out; // DataType.JSON
        } catch (Exception ex) {
            throw new InterpreterError("file.listZipFiles: " + ex.getMessage());
        }
    }

    public static List<Map<String, Object>> unzip(Environment env, Object... args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String key = (String) args[0];
        String destDir = (String) args[1];
        Object entries = (args.length > 2 ? args[2] : null);
        Boolean ov = (args.length > 3 ? (Boolean) args[3] : null);
        boolean overwrite = (ov == null) ? true : ov.booleanValue();

        if (key == null) {
            throw new InterpreterError("file.unzip: handle cannot be null");
        }

        FileContext oz = env.findOpenedFileByHandleOrPath(key);
        if (oz == null || oz.file == null) {
            throw new InterpreterError("file.unzip: not open: " + key);
        }

        try {
            Path destRoot = null;
            if (destDir != null) {
                destRoot = Util.resolveSandboxedPath(destDir);
                Files.createDirectories(destRoot);
            }
            java.util.List<java.util.Map<String, Object>> result;
            if (entries == null) {
                // Extract ALL entries
                result = unzipAllWithReport(oz.file, destRoot, overwrite);
            } else if (entries instanceof String s) {
                // Single entry
                result = unzipSomeWithReport(oz.file, destRoot, java.util.List.of(s), overwrite);
            } else if (entries instanceof com.eb.script.arrays.ArrayDef ad) {
                java.util.List<String> list = new java.util.ArrayList<>();
                for (Object o : ad) {
                    if (o != null) {
                        list.add(String.valueOf(o));
                    }
                }
                result = unzipSomeWithReport(oz.file, destRoot, list, overwrite);
            } else if (entries instanceof java.util.List<?> listIn) {
                java.util.List<String> list = new java.util.ArrayList<>();
                for (Object o : listIn) {
                    if (o != null) {
                        list.add(String.valueOf(o));
                    }
                }
                result = unzipSomeWithReport(oz.file, destRoot, list, overwrite);
            } else {
                throw new InterpreterError("file.unzip: entries must be a String or an array/list of Strings");
            }

            if (env.isEchoOn()) {
                long filesCopied = result.stream()
                        .filter(m -> {
                            Object a = m.get("action");
                            return "copied".equals(a) || "overwritten".equals(a);
                        })
                        .count();
                if (env.isEchoOn()) {
                    sysOutput(output, "Unzipped " + filesCopied + " file(s) to " + destRoot.toString());
                }
            }

            return result; // DataType.JSON
        } catch (Exception ex) {
            throw new InterpreterError("file.unzip: " + com.eb.util.Util.formatExceptionWithOrigin(ex));
        }
    }

    // file.closeZip(handleOrPath) -> BOOL
    public static Boolean closeZip(Environment env, Object... args) throws InterpreterError {
        String key = (String) args[0];
        if (key == null) {
            return Boolean.FALSE;
        }
        return env.closeOpenedFile(key);
    }

    /**
     * Extract ALL entries from a zip FileSystem into destRoot. Returns a
     * JSON-style list of entry result maps.
     */
    private static List<java.util.Map<String, Object>> unzipAllWithReport(
            java.nio.file.FileSystem zipFs, Path destRoot, boolean overwrite) throws Exception {

        final Path zipRoot = zipFs.getPath("/");
        final Path destRootNorm = destRoot.toAbsolutePath().normalize();
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();

        try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(zipRoot)) {
            java.util.Iterator<Path> it = walk.iterator();
            while (it.hasNext()) {
                Path src = it.next();
                if (src == null || src.equals(zipRoot)) {
                    continue;
                }
                java.util.Map<String, Object> rec = copyZipPathReport(zipRoot, src, destRootNorm, overwrite);
                out.add(rec);
            }
        }
        return out;
    }

    /**
     * Extract selected entries from a zip FileSystem. Each entry may be a file
     * or a directory (directory extracts recursively). Returns a JSON-style
     * list of entry result maps.
     */
    private static List<Map<String, Object>> unzipSomeWithReport(
            java.nio.file.FileSystem zipFs, Path destRoot,
            List<String> entries, boolean overwrite) throws Exception {

        final Path zipRoot = zipFs.getPath("/");
        final Path destRootNorm = destRoot.toAbsolutePath().normalize();
        List<Map<String, Object>> out = new java.util.ArrayList<>();

        for (String e : entries) {
            if (e == null || e.isBlank()) {
                continue;
            }
            Path p = zipFs.getPath(e);
            // Normalize to absolute within the zip
            Path abs = p.isAbsolute() ? p.normalize() : zipRoot.resolve(p).normalize();
            if (!java.nio.file.Files.exists(abs)) {
                java.util.Map<String, Object> miss = new java.util.LinkedHashMap<>();
                miss.put("entry", e);
                miss.put("isDir", false);
                miss.put("action", "blocked");
                miss.put("reason", "notFound");
                out.add(miss);
                continue; // or throw if you prefer strict behavior
            }
            if (java.nio.file.Files.isDirectory(abs)) {
                try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(abs)) {
                    java.util.Iterator<Path> it = walk.iterator();
                    while (it.hasNext()) {
                        Path src = it.next();
                        if (src == null || src.equals(abs)) {
                            continue;
                        }
                        java.util.Map<String, Object> rec = copyZipPathReport(zipRoot, src, destRootNorm, overwrite);
                        out.add(rec);
                    }
                }
            } else {
                java.util.Map<String, Object> rec = copyZipPathReport(zipRoot, abs, destRootNorm, overwrite);
                out.add(rec);
            }
        }
        return out;
    }

    /**
     * Copy one path from ZIP FS to destination with reporting and zip-slip
     * guard. - If directory: create directories (returns action=createdDir,
     * size=0) - If file: copy file (returns action=copied|overwritten|skipped)
     * - If blocked by traversal: action=blocked, reason=pathTraversal
     */
    private static Map<String, Object> copyZipPathReport(
            Path zipRoot, Path src, Path destRootNorm, boolean overwrite) throws Exception {

        boolean isDir = Files.isDirectory(src);
        String relStr = zipRoot.relativize(src).toString(); // path inside the zip

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("entry", relStr);
        rec.put("isDir", isDir);

        // Resolve destination and guard against zip-slip
        Path dst = destRootNorm.resolve(relStr).normalize();
        if (!dst.startsWith(destRootNorm)) {
            rec.put("action", "blocked");
            rec.put("reason", "pathTraversal");
            return rec;
        }

        if (isDir) {
            Files.createDirectories(dst);
            rec.put("action", "createdDir");
            rec.put("dest", dst.toString());
            try {
                rec.put("size", 0L);
                rec.put("modifiedMs", Files.getLastModifiedTime(src).toMillis());
            } catch (Exception ignore) {
                rec.put("size", 0L);
                rec.put("modifiedMs", 0L);
            }
            return rec;
        }

        Path parent = dst.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean exists = Files.exists(dst);
        try {
            if (!exists || overwrite) {
                java.nio.file.Files.copy(
                        src, dst,
                        java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                rec.put("action", exists ? "overwritten" : "copied");
            } else {
                rec.put("action", "skipped");
            }
        } catch (Exception ex) {
            // Report as blocked with the error message
            rec.put("action", "blocked");
            rec.put("reason", ex.getMessage());
        }

        rec.put("dest", dst.toString());
        try {
            rec.put("size", Files.size(src));
            rec.put("modifiedMs", Files.getLastModifiedTime(src).toMillis());
        } catch (Exception ignore) {
            rec.put("size", 0L);
            rec.put("modifiedMs", 0L);
        }
        return rec;
    }

    /**
     * Reads a single line from the current channel position. Consumes through
     * the first '\n' (handles CRLF by trimming trailing '\r'). Returns null on
     * EOF with no data read.
     */
    private static String readLine(SeekableByteChannel ch, Charset cs) throws Exception {
        long startPos = ch.position();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        ByteBuffer buf = ByteBuffer.allocate(2048);
        long pos = startPos;

        while (true) {
            buf.clear();
            int n = ch.read(buf);
            if (n <= 0) {                         // EOF
                if (baos.size() == 0) {
                    return null;
                }
                break;                            // partial line (last line without newline)
            }
            byte[] a = buf.array();
            int newlineAt = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == (byte) '\n') {
                    newlineAt = i;
                    break;
                }
            }
            if (newlineAt >= 0) {
                // append bytes before '\n'
                if (newlineAt > 0) {
                    baos.write(a, 0, newlineAt);
                }
                // adjust channel position to just after newline
                long consumed = (pos - startPos) + newlineAt + 1;
                ch.position(startPos + consumed);
                break;
            } else {
                // no newline found: append all and continue
                baos.write(a, 0, n);
                pos += n;
            }
        }

        // Trim trailing '\r' (CRLF)
        byte[] raw = baos.toByteArray();
        int len = raw.length;
        if (len > 0 && raw[len - 1] == (byte) '\r') {
            len--;
        }
        return new String(raw, 0, len, cs);
    }

    /**
     * Reads a single line from the current channel position. Consumes through
     * the first '\n' (handles CRLF by trimming trailing '\r'). Returns null on
     * EOF with no data read.
     */
    private static String readAll(SeekableByteChannel ch, Charset cs) throws Exception {
        long startPos = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
        long pos = startPos;

        do {
            buf.clear();
            int n = ch.read(buf);
            if (n <= 0) {                         // EOF
                if (baos.size() == 0) {
                    return null;
                }
                break;                            // partial line (last line without newline)
            }
            byte[] a = buf.array();
            baos.write(a, 0, n);
            pos = pos + n;
        } while (true);
        ch.position(pos);

        // Trim trailing '\r' (CRLF)
        byte[] raw = baos.toByteArray();
        return new String(raw, 0, raw.length, cs);
    }

    private static void writeBytes(SeekableByteChannel ch, byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(data);
        while (buf.hasRemaining()) {
            ch.write(buf);
        }
    }

    private static void sysOutput(ScriptArea output, String message) {
        if (output != null) {
            output.println(message);
        } else {
            System.out.println(message);
        }
    }

    private Path getNewFileName(String pathStr) throws IOException {
        // Ensure sandbox exists
        Files.createDirectories(Util.SANDBOX_ROOT);

        // Find the next available Untitled-N.ebs
        if (pathStr == null) {
            pathStr = Util.SANDBOX_ROOT.toString();
        }
        final String base = "Untitled";
        final String ext = ".ebs";
        java.nio.file.Path p;
        int i = 1;
        while (true) {
            String name = pathStr + base + "-" + i + ext;

            p = Util.SANDBOX_ROOT.resolve(name);
            if (!Files.exists(p)) {
                break;
            }
            i++;
        }
        return p;
    }

    public static Boolean exists(Environment env, Object... args) throws InterpreterError {
        String path = (String) (args.length > 0 ? args[0] : null);
        if (path == null || path.isBlank()) {
            return Boolean.FALSE;
        }
        try {
            java.nio.file.Path p = com.eb.util.Util.resolveSandboxedPath(path);
            return java.nio.file.Files.exists(p);
        } catch (Exception ex) {
            throw new InterpreterError("file.exists: " + ex.getMessage());
        }
    }

    /**
     * file.size(handleOrPath) -> LONG (bytes) - If a string 'handle' refers to
     * an open file (e.g., from file.open), returns channel.size(). - Else
     * treats it as a PATH (sandbox-resolved) and returns Files.size(path) if it
     * is a regular file. - Returns null for directories or non-existent paths.
     */
    public static Long size(Environment env, Object... args) throws InterpreterError {
        String key = (String) (args.length > 0 ? args[0] : null);
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            // Try open-file handle first (if your OpenFile registry is used by file.open)
            FileContext of = env.findOpenedFileByHandleOrPath(key);
            if (of != null && of.chan != null) {
                try {
                    return of.chan.size();
                } catch (java.io.IOException io) {
                    throw new InterpreterError("file.size (handle): " + io.getMessage());
                }
            }

            // Fallback: treat as a sandbox path
            java.nio.file.Path p = com.eb.util.Util.resolveSandboxedPath(key);
            if (!java.nio.file.Files.exists(p)) {
                return null; // not found
            }
            if (!java.nio.file.Files.isRegularFile(p)) {
                return null; // for directories/special files return null (or choose a policy)
            }
            try {
                return java.nio.file.Files.size(p);
            } catch (java.io.IOException io) {
                throw new InterpreterError("file.size (path): " + io.getMessage());
            }
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("file.size: " + ex.getMessage());
        }
    }
}
