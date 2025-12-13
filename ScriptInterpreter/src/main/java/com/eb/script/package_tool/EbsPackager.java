package com.eb.script.package_tool;

import com.eb.script.RuntimeContext;
import com.eb.script.parser.ParseError;
import com.eb.script.parser.Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line tool for packaging EBS scripts into .ebsp files.
 * This allows distribution of EBS applications without exposing source code.
 * 
 * Usage:
 *   java com.eb.script.package_tool.EbsPackager <input.ebs> [output.ebsp]
 *   java com.eb.script.package_tool.EbsPackager -o <output.ebsp> <input1.ebs> <input2.ebs> ...
 * 
 * @author Earl Bosch
 */
public class EbsPackager {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        try {
            // Parse command line arguments
            String outputFile = null;
            List<String> inputFiles = new ArrayList<>();
            
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-o") || args[i].equals("--output")) {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: -o requires an output filename");
                        System.exit(1);
                    }
                    outputFile = args[++i];
                } else if (args[i].equals("-h") || args[i].equals("--help")) {
                    printUsage();
                    System.exit(0);
                } else {
                    inputFiles.add(args[i]);
                }
            }
            
            if (inputFiles.isEmpty()) {
                System.err.println("Error: No input files specified");
                printUsage();
                System.exit(1);
            }
            
            // If no output file specified, use first input file with .ebsp extension
            if (outputFile == null) {
                String firstInput = inputFiles.get(0);
                outputFile = firstInput.replaceAll("(?i)\\.ebs$", "") + ".ebsp";
            }
            
            // Package the script(s)
            if (inputFiles.size() == 1) {
                packageSingleScript(inputFiles.get(0), outputFile);
            } else {
                packageMultipleScripts(inputFiles, outputFile);
            }
            
            System.out.println("Successfully packaged to: " + outputFile);
            
        } catch (Exception e) {
            System.err.println("Error packaging script: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Package a single EBS script into a .ebsp file.
     */
    private static void packageSingleScript(String inputFile, String outputFile) 
            throws IOException, ParseError {
        Path inputPath = Paths.get(inputFile);
        Path outputPath = Paths.get(outputFile);
        
        if (!Files.exists(inputPath)) {
            throw new IOException("Input file not found: " + inputFile);
        }
        
        System.out.println("Parsing: " + inputFile);
        RuntimeContext context = Parser.parse(inputPath);
        
        System.out.println("Packaging to: " + outputFile);
        RuntimeContextSerializer.serialize(context, outputPath);
        
        long originalSize = Files.size(inputPath);
        long packagedSize = Files.size(outputPath);
        System.out.println("Original size: " + originalSize + " bytes");
        System.out.println("Packaged size: " + packagedSize + " bytes");
        
        if (packagedSize < originalSize) {
            System.out.println("Size reduction: " + String.format("%.1f%%", 
                (1.0 - (double)packagedSize / originalSize) * 100));
        } else {
            System.out.println("Size increase: " + String.format("%.1f%%", 
                ((double)packagedSize / originalSize - 1.0) * 100));
        }
    }
    
    /**
     * Package multiple EBS scripts into a single .ebsp file.
     * The first script is the main entry point.
     */
    private static void packageMultipleScripts(List<String> inputFiles, String outputFile) 
            throws IOException, ParseError {
        // For now, we'll parse the main script which should import the others
        // In the future, we could create a composite RuntimeContext
        System.out.println("Packaging multiple scripts (main: " + inputFiles.get(0) + ")");
        packageSingleScript(inputFiles.get(0), outputFile);
        System.out.println("Note: Ensure the main script imports all dependencies");
    }
    
    private static void printUsage() {
        System.out.println("EBS Script Packager");
        System.out.println("==================");
        System.out.println();
        System.out.println("Package EBS scripts into binary .ebsp files to protect source code.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java com.eb.script.package_tool.EbsPackager <input.ebs> [output.ebsp]");
        System.out.println("  java com.eb.script.package_tool.EbsPackager -o <output.ebsp> <input1.ebs> <input2.ebs> ...");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output <file>  Specify output file (default: input.ebsp)");
        System.out.println("  -h, --help          Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Package a single script");
        System.out.println("  java com.eb.script.package_tool.EbsPackager myapp.ebs");
        System.out.println();
        System.out.println("  # Package with custom output name");
        System.out.println("  java com.eb.script.package_tool.EbsPackager myapp.ebs -o myapp-v1.ebsp");
        System.out.println();
        System.out.println("  # Package multiple scripts (main + dependencies)");
        System.out.println("  java com.eb.script.package_tool.EbsPackager main.ebs lib1.ebs lib2.ebs");
        System.out.println();
        System.out.println("Note: Packaged files can be run with:");
        System.out.println("  java -cp target/classes com.eb.script.Run myapp.ebsp");
    }
}
