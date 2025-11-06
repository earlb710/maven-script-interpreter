package com.eb.util;

/**
 *
 * @author Earl Bosch
 */
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZipBase64
 *
 * Usage:
 *   # Encode: zip a file/dir and write Base64 to a .txt file (or stdout)
 *   java ZipBase64 encode <input-path> [output-base64-txt]
 *
 *   # Decode: convert Base64 text back into a .zip file
 *   java ZipBase64 decode <input-base64-txt> <output-zip-file>
 *
 * Notes:
 *  - Streams zip bytes directly through Base64; does NOT load everything in memory.
 *  - Preserves empty directories and relative paths.
 *  - Skips symbolic links by default (to avoid loops).
 */
public final class ZipBase64 {

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("""
                Usage:
                  Encode (zip -> base64):
                    java ZipBase64 encode <input-path> [output-base64.txt]
                    # If output is omitted, base64 goes to stdout.

                  Decode (base64 -> zip):
                    java ZipBase64 decode <input-base64.txt> <output.zip>
                """);
            System.exit(2);
        }

        String cmd = args[0].toLowerCase();
        try {
            switch (cmd) {
                case "encode" -> {
                    Path input = Paths.get(args[1]).toAbsolutePath().normalize();
                    if (!Files.exists(input)) {
                        throw new NoSuchFileException("Input does not exist: " + input);
                    }
                    if (args.length == 3) {
                        Path outTxt = Paths.get(args[2]).toAbsolutePath().normalize();
                        try (OutputStream fos = Files.newOutputStream(outTxt);
                             // Use MIME encoder to break lines every 76 chars (safer for some UIs).
                             OutputStream b64 = Base64.getMimeEncoder(76, new byte[]{'\n'}).wrap(fos)) {
                            zipPathToBase64(input, b64);
                        }
                        System.out.println("Wrote Base64 to: " + outTxt);
                    } else {
                        // Write to stdout if no output file provided.
                        try (OutputStream b64 = Base64.getMimeEncoder(76, new byte[]{'\n'}).wrap(System.out)) {
                            zipPathToBase64(input, b64);
                        }
                    }
                }
                case "decode" -> {
                    if (args.length != 3) {
                        throw new IllegalArgumentException("Decode requires <input-base64.txt> <output.zip>");
                    }
                    Path inTxt = Paths.get(args[1]).toAbsolutePath().normalize();
                    Path outZip = Paths.get(args[2]).toAbsolutePath().normalize();
                    base64ToZip(inTxt, outZip);
                    System.out.println("Wrote ZIP to: " + outZip);
                }
                default -> throw new IllegalArgumentException("Unknown command: " + cmd);
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Zips a file or directory and writes the ZIP stream into the given Base64-wrapped OutputStream.
     * The caller is responsible for wrapping the OutputStream with Base64 beforehand.
     */
    public static void zipPathToBase64(Path input, OutputStream base64Out) throws IOException {
        // Important: ZipOutputStream must be closed to finalize the ZIP before Base64 is done.
        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(base64Out))) {
            if (Files.isDirectory(input)) {
                // Use the directory's parent to derive relative names from the directory root.
                final Path base = input.toAbsolutePath().normalize();
                // Add the directory itself (as an entry) so an empty dir is preserved
                addDirectoryEntry(zipOut, base.getFileName().toString() + "/");

                // Walk the tree without following symlinks (prevents recursive loops).
                Files.walkFileTree(base, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (Files.isSymbolicLink(dir)) {
                            // Skip symlinked directories
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        if (!dir.equals(base)) {
                            String rel = base.relativize(dir).toString().replace('\\', '/') + "/";
                            addDirectoryEntry(zipOut, rel);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (Files.isSymbolicLink(file)) {
                            // Skip symlinked files
                            return FileVisitResult.CONTINUE;
                        }
                        String rel = base.relativize(file).toString().replace('\\', '/');
                        addFileEntry(zipOut, file, rel);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                // Single file
                String entryName = input.getFileName().toString();
                addFileEntry(zipOut, input, entryName);
            }
            // try-with-resources closes zipOut -> flushes Base64 chain upstream.
        }
    }

    private static void addDirectoryEntry(ZipOutputStream zipOut, String entryName) throws IOException {
        if (!entryName.endsWith("/")) entryName += "/";
        ZipEntry dirEntry = new ZipEntry(entryName);
        zipOut.putNextEntry(dirEntry);
        zipOut.closeEntry();
    }

    private static void addFileEntry(ZipOutputStream zipOut, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        try {
            entry.setTime(Files.getLastModifiedTime(file).toMillis());
        } catch (IOException ignored) { /* best effort */ }

        zipOut.putNextEntry(entry);
        // Stream the file into the ZIP entry
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                zipOut.write(buf, 0, r);
            }
        }
        zipOut.closeEntry();
    }

    /**
     * Decodes a Base64 text file back into a ZIP file (utility helper).
     */
    public static void base64ToZip(Path inBase64Text, Path outZip) throws IOException {
        // Ensure parent directory exists
        Path parent = outZip.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (InputStream in = new BufferedInputStream(Files.newInputStream(inBase64Text));
             InputStream b64 = Base64.getMimeDecoder().wrap(in);
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(outZip, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = b64.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        }
    }

}
