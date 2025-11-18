package com.eb.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * Test script that reads test images and resizes them to max length 2000.
 * 
 * @author Earl Bosch
 */
public class ImageResizeTest {
    
    private static final int MAX_LENGTH = 2000;
    
    public static void main(String[] args) {
        try {
            String inputDir = args.length > 0 ? args[0] : "test-images";
            String outputDir = args.length > 1 ? args[1] : "test-images-resized";
            
            File inDir = new File(inputDir);
            if (!inDir.exists() || !inDir.isDirectory()) {
                System.err.println("Input directory does not exist: " + inputDir);
                System.exit(1);
            }
            
            File outDir = new File(outputDir);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            
            System.out.println("UtilImage Resize Test");
            System.out.println("=====================");
            System.out.println("Max Length: " + MAX_LENGTH);
            System.out.println("Input Directory: " + inputDir);
            System.out.println("Output Directory: " + outputDir);
            System.out.println();
            
            // Get all PNG files
            File[] imageFiles = inDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png"));
            
            if (imageFiles == null || imageFiles.length == 0) {
                System.err.println("No PNG images found in: " + inputDir);
                System.exit(1);
            }
            
            System.out.println("Found " + imageFiles.length + " test images\n");
            
            // Process each image
            int totalOriginalSize = 0;
            int totalResizedSize = 0;
            
            for (File imageFile : imageFiles) {
                processImage(imageFile, outDir);
                
                // Calculate totals
                byte[] originalBytes = Files.readAllBytes(imageFile.toPath());
                totalOriginalSize += originalBytes.length;
                
                String outputFileName = imageFile.getName().replace(".png", "_resized.png");
                File outputFile = new File(outDir, outputFileName);
                if (outputFile.exists()) {
                    byte[] resizedBytes = Files.readAllBytes(outputFile.toPath());
                    totalResizedSize += resizedBytes.length;
                }
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("SUMMARY");
            System.out.println("=".repeat(80));
            System.out.printf("Images Processed: %d%n", imageFiles.length);
            System.out.printf("Total Original Size: %.2f MB%n", totalOriginalSize / (1024.0 * 1024.0));
            System.out.printf("Total Resized Size: %.2f MB%n", totalResizedSize / (1024.0 * 1024.0));
            System.out.printf("Total Space Saved: %.2f MB (%.1f%%)%n", 
                (totalOriginalSize - totalResizedSize) / (1024.0 * 1024.0),
                100.0 * (totalOriginalSize - totalResizedSize) / totalOriginalSize);
            System.out.println("=".repeat(80));
            
            System.out.println("\n✓ All images processed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during image resize test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void processImage(File inputFile, File outputDir) throws IOException {
        System.out.println("Processing: " + inputFile.getName());
        
        // Read original image
        byte[] originalBytes = Files.readAllBytes(inputFile.toPath());
        BufferedImage originalImage = ImageIO.read(inputFile);
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        long originalSize = originalBytes.length;
        
        System.out.printf("  Original: %dx%d (%.2f KB)%n", 
            originalWidth, originalHeight, originalSize / 1024.0);
        
        // Resize using UtilImage
        long startTime = System.currentTimeMillis();
        byte[] resizedBytes = UtilImage.resizeImage(originalBytes, MAX_LENGTH);
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // Read resized image to get dimensions
        BufferedImage resizedImage = ImageIO.read(new ByteArrayInputStream(resizedBytes));
        int resizedWidth = resizedImage.getWidth();
        int resizedHeight = resizedImage.getHeight();
        long resizedSize = resizedBytes.length;
        
        System.out.printf("  Resized: %dx%d (%.2f KB)%n", 
            resizedWidth, resizedHeight, resizedSize / 1024.0);
        
        // Calculate statistics
        boolean dimensionsChanged = (originalWidth != resizedWidth) || (originalHeight != resizedHeight);
        double sizeReduction = 100.0 * (originalSize - resizedSize) / originalSize;
        double aspectRatio = (double) originalWidth / originalHeight;
        double newAspectRatio = (double) resizedWidth / resizedHeight;
        boolean aspectRatioMaintained = Math.abs(aspectRatio - newAspectRatio) < 0.01;
        
        System.out.printf("  Dimensions Changed: %s%n", dimensionsChanged ? "Yes" : "No");
        System.out.printf("  Size Reduction: %.1f%%%n", sizeReduction);
        System.out.printf("  Aspect Ratio Maintained: %s (%.3f -> %.3f)%n", 
            aspectRatioMaintained ? "Yes" : "No", aspectRatio, newAspectRatio);
        System.out.printf("  Processing Time: %d ms%n", elapsedTime);
        
        // Verify max dimension
        int maxDimension = Math.max(resizedWidth, resizedHeight);
        boolean withinMaxLength = maxDimension <= MAX_LENGTH;
        System.out.printf("  Max Dimension: %d (within %d: %s)%n", 
            maxDimension, MAX_LENGTH, withinMaxLength ? "Yes" : "No");
        
        // Save resized image
        String outputFileName = inputFile.getName().replace(".png", "_resized.png");
        File outputFile = new File(outputDir, outputFileName);
        Files.write(outputFile.toPath(), resizedBytes);
        
        System.out.println("  Output: " + outputFile.getName());
        System.out.println();
    }
}
