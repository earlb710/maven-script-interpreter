package com.eb.script.package_tool;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.Environment;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes RuntimeContext objects for packaging EBS scripts.
 * Uses Java serialization with GZIP compression to create compact binary packages.
 * The Environment and source path are not serialized (transient fields) and are
 * reconstructed when loading the packaged script.
 * 
 * @author Earl Bosch
 */
public class RuntimeContextSerializer {
    
    /**
     * Serialize a RuntimeContext to a file with compression.
     * 
     * @param context The RuntimeContext to serialize
     * @param outputFile The file to write to
     * @throws IOException if serialization fails
     */
    public static void serialize(RuntimeContext context, Path outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            oos.writeObject(context);
        }
    }
    
    /**
     * Deserialize a RuntimeContext from a file.
     * The returned RuntimeContext will have a new Environment instance and
     * the sourcePath will be set to the package file path.
     * 
     * @param inputFile The file to read from
     * @return The deserialized RuntimeContext with reconstructed Environment
     * @throws IOException if deserialization fails
     * @throws ClassNotFoundException if the RuntimeContext class is not found
     */
    public static RuntimeContext deserialize(Path inputFile) throws IOException, ClassNotFoundException {
        RuntimeContext context;
        try (FileInputStream fis = new FileInputStream(inputFile.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
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
