package com.eb.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Generates test images of various sizes for testing UtilImage.
 * 
 * @author Earl Bosch
 */
public class ImageTestGenerator {
    
    /**
     * Generates test images with different dimensions.
     * 
     * @param outputDir the directory to save generated images
     * @throws IOException if image writing fails
     */
    public static void generateTestImages(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Define test image dimensions (width x height)
        int[][] dimensions = {
            {800, 600},    // 1
            {1024, 768},   // 2
            {1280, 960},   // 3
            {1600, 1200},  // 4
            {1920, 1440},  // 5
            {2048, 1536},  // 6
            {2560, 1920},  // 7
            {3000, 2250},  // 8
            {3500, 2625},  // 9
            {4000, 3000}   // 10
        };
        
        // Generate PNG images with different colors
        Color[] colors = {
            new Color(255, 100, 100),  // Red
            new Color(100, 255, 100),  // Green
            new Color(100, 100, 255),  // Blue
            new Color(255, 255, 100),  // Yellow
            new Color(255, 100, 255),  // Magenta
            new Color(100, 255, 255),  // Cyan
            new Color(200, 150, 100),  // Brown
            new Color(150, 100, 200),  // Purple
            new Color(100, 200, 150),  // Teal
            new Color(255, 150, 50)    // Orange
        };
        
        System.out.println("Generating test images...\n");
        
        for (int i = 0; i < dimensions.length; i++) {
            int width = dimensions[i][0];
            int height = dimensions[i][1];
            Color color = colors[i];
            
            // Create image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Fill background with color
            g2d.setColor(color);
            g2d.fillRect(0, 0, width, height);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.drawRect(10, 10, width - 20, height - 20);
            
            // Draw text with dimensions
            g2d.setColor(Color.WHITE);
            int fontSize = Math.min(width, height) / 20;
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            
            String text = width + " x " + height;
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            
            // Center the text
            int x = (width - textWidth) / 2;
            int y = (height - textHeight) / 2 + fm.getAscent();
            
            // Draw shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, x + 2, y + 2);
            
            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x, y);
            
            g2d.dispose();
            
            // Save as PNG
            String filename = String.format("test_image_%02d_%dx%d.png", i + 1, width, height);
            File outputFile = new File(dir, filename);
            ImageIO.write(image, "png", outputFile);
            
            System.out.printf("Generated: %s (%.2f KB)%n", 
                filename, outputFile.length() / 1024.0);
        }
        
        System.out.println("\nAll test images generated successfully!");
    }
    
    public static void main(String[] args) {
        try {
            String outputDir = args.length > 0 ? args[0] : "test-images";
            generateTestImages(outputDir);
        } catch (Exception e) {
            System.err.println("Error generating test images: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
