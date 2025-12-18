package com.eb.script.package_tool;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.builtins.BuiltinsSystem;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes RuntimeContext objects for packaging EBS scripts.
 * Uses Java serialization with GZIP compression and Base64 encoding to create 
 * compact text-based packages. The file format includes a version header comment
 * followed by the base64-encoded compressed bytecode.
 * The Environment and source path are not serialized (transient fields) and are
 * reconstructed when loading the packaged script.
 * 
 * @author Earl Bosch
 */
public class RuntimeContextSerializer {
    
    /**
     * Serialize a RuntimeContext to a file with compression and base64 encoding.
     * The output file format is:
     * Line 1: // packaged esb language ver X.Y.Z.W
     * Line 2+: Base64-encoded GZIP-compressed serialized RuntimeContext
     * 
     * @param context The RuntimeContext to serialize
     * @param outputFile The file to write to
     * @throws IOException if serialization fails
     */
    public static void serialize(RuntimeContext context, Path outputFile) throws IOException {
        // Serialize and compress to byte array in memory
        byte[] compressedBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            oos.writeObject(context);
            oos.flush();
            gzos.finish();
            compressedBytes = baos.toByteArray();
        }
        
        // Encode to base64
        String base64Encoded = Base64.getEncoder().encodeToString(compressedBytes);
        
        // Write header comment and base64 content to file
        String version = BuiltinsSystem.EBS_LANGUAGE_VERSION;
        String header = "// packaged esb language ver " + version + "\n";
        String content = header + base64Encoded;
        
        Files.writeString(outputFile, content, StandardCharsets.UTF_8);
    }
    
    /**
     * Deserialize a RuntimeContext from a file.
     * The file format is expected to be:
     * Line 1: // packaged esb language ver X.Y.Z.W
     * Line 2+: Base64-encoded GZIP-compressed serialized RuntimeContext
     * 
     * The returned RuntimeContext will have a new Environment instance and
     * the sourcePath will be set to the package file path.
     * 
     * @param inputFile The file to read from
     * @return The deserialized RuntimeContext with reconstructed Environment
     * @throws IOException if deserialization fails
     * @throws ClassNotFoundException if the RuntimeContext class is not found
     */
    public static RuntimeContext deserialize(Path inputFile) throws IOException, ClassNotFoundException {
        // Read entire file content
        String fileContent = Files.readString(inputFile, StandardCharsets.UTF_8);
        
        // Split into lines
        String[] lines = fileContent.split("\n", 2);
        
        // Validate header line
        if (lines.length < 2 || !lines[0].startsWith("// packaged esb language ver ")) {
            throw new IOException("Invalid .ebsp file format: missing or invalid header");
        }
        
        // Extract base64 content (everything after the header line)
        String base64Content = lines[1].trim();
        
        // Decode base64
        byte[] compressedBytes;
        try {
            compressedBytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid .ebsp file format: corrupted base64 data", e);
        }
        
        // Decompress and deserialize
        RuntimeContext context;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            context = (RuntimeContext) ois.readObject();
        }
        
        // Reconstruct the RuntimeContext with a new Environment and the package path
        return new RuntimeContext(context.name, inputFile, context.blocks, context.statements);
    }
    
    /**
     * Check if a file is a packaged EBS file.
     * 
     * @param file The file to check
     * @return true if it's a .ebsp file
     */
    public static boolean isPackagedFile(Path file) {
        return file.toString().toLowerCase().endsWith(".ebsp");
    }
}
