package com.eb.script.image;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.InterpreterError;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Image data type for EBS scripting language.
 * Wraps a JavaFX Image with metadata and provides manipulation methods.
 * All manipulations are performed on the JavaFX image.
 * Supports PNG, JPG, GIF, BMP, and SVG image formats.
 * 
 * @author Earl Bosch
 */
public class EbsImage {

    /** Flag to track if SVG loader has been installed */
    private static boolean svgLoaderInstalled = false;

    /** The JavaFX image */
    private WritableImage fxImage;
    
    /** Image format type (png, jpg, gif, bmp, svg) */
    private String imageType;
    
    /** Image name/path (optional) */
    private String imageName;
    
    /** Original bytes for format detection */
    private byte[] originalBytes;
    
    /**
     * Install the SVG image loader factory if not already installed.
     * This enables JavaFX to load SVG images natively.
     */
    public static synchronized void installSvgLoader() {
        if (!svgLoaderInstalled) {
            try {
                SvgImageLoaderFactory.install();
                svgLoaderInstalled = true;
            } catch (Exception e) {
                // Ignore if already installed or fails
            }
        }
    }

    /**
     * Create an EbsImage from raw byte array data.
     * 
     * @param bytes Raw image bytes
     * @throws InterpreterError if the bytes don't represent a valid image
     */
    public EbsImage(byte[] bytes) throws InterpreterError {
        this(bytes, null, null);
    }

    /**
     * Create an EbsImage from raw byte array data with name.
     * 
     * @param bytes Raw image bytes
     * @param name Optional image name/path
     * @throws InterpreterError if the bytes don't represent a valid image
     */
    public EbsImage(byte[] bytes, String name) throws InterpreterError {
        this(bytes, name, null);
    }

    /**
     * Create an EbsImage from raw byte array data with name and type.
     * 
     * @param bytes Raw image bytes
     * @param name Optional image name/path
     * @param type Optional image type (png, jpg, gif, bmp, svg)
     * @throws InterpreterError if the bytes don't represent a valid image
     */
    public EbsImage(byte[] bytes, String name, String type) throws InterpreterError {
        if (bytes == null || bytes.length == 0) {
            throw new InterpreterError("EbsImage: image bytes cannot be null or empty");
        }
        
        // Ensure SVG loader is installed before loading any images
        installSvgLoader();
        
        this.originalBytes = bytes;
        this.imageName = name;
        
        // Detect format if not provided
        this.imageType = (type != null && !type.isBlank()) ? type.toLowerCase() : detectFormat(bytes, name);
        
        // Convert bytes to JavaFX Image
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            Image img = new Image(bais);
            
            if (img.isError()) {
                throw new InterpreterError("EbsImage: invalid image data - " + img.getException().getMessage());
            }
            
            // Create a writable copy
            int width = (int) img.getWidth();
            int height = (int) img.getHeight();
            this.fxImage = new WritableImage(width, height);
            
            PixelReader reader = img.getPixelReader();
            PixelWriter writer = this.fxImage.getPixelWriter();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setColor(x, y, reader.getColor(x, y));
                }
            }
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("EbsImage: failed to load image - " + ex.getMessage());
        }
    }

    /**
     * Create an EbsImage from ArrayFixedByte.
     * 
     * @param array ArrayFixedByte containing image data
     * @throws InterpreterError if the data is not a valid image
     */
    public EbsImage(ArrayFixedByte array) throws InterpreterError {
        this(array.elements, null, null);
    }

    /**
     * Create an EbsImage from ArrayFixedByte with name.
     * 
     * @param array ArrayFixedByte containing image data
     * @param name Optional image name/path
     * @throws InterpreterError if the data is not a valid image
     */
    public EbsImage(ArrayFixedByte array, String name) throws InterpreterError {
        this(array.elements, name, null);
    }

    /**
     * Create an EbsImage from an existing JavaFX WritableImage.
     * 
     * @param fxImage The JavaFX WritableImage
     * @param name Optional image name
     * @param type Image format type
     */
    public EbsImage(WritableImage fxImage, String name, String type) {
        this.fxImage = fxImage;
        this.imageName = name;
        this.imageType = (type != null && !type.isBlank()) ? type.toLowerCase() : "png";
        this.originalBytes = null;
    }

    // --- Getters ---

    /**
     * Get the JavaFX Image.
     */
    public Image getFxImage() {
        return fxImage;
    }

    /**
     * Get the writable JavaFX Image for modifications.
     */
    public WritableImage getWritableFxImage() {
        return fxImage;
    }

    /**
     * Get the image type/format.
     */
    public String getImageType() {
        return imageType;
    }

    /**
     * Set the image type/format.
     */
    public void setImageType(String type) {
        if (type != null && !type.isBlank()) {
            this.imageType = type.toLowerCase();
        }
    }

    /**
     * Get the image name.
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Set the image name.
     */
    public void setImageName(String name) {
        this.imageName = name;
    }

    /**
     * Get image width.
     */
    public int getWidth() {
        return (int) fxImage.getWidth();
    }

    /**
     * Get image height.
     */
    public int getHeight() {
        return (int) fxImage.getHeight();
    }

    /**
     * Check if the image has an alpha channel.
     * Checks the actual pixel format of the JavaFX image.
     */
    public boolean hasAlpha() {
        // Check the actual pixel format of the image
        PixelReader reader = fxImage.getPixelReader();
        if (reader != null) {
            javafx.scene.image.PixelFormat<?> format = reader.getPixelFormat();
            javafx.scene.image.PixelFormat.Type type = format.getType();
            return type == javafx.scene.image.PixelFormat.Type.BYTE_BGRA ||
                   type == javafx.scene.image.PixelFormat.Type.BYTE_BGRA_PRE ||
                   type == javafx.scene.image.PixelFormat.Type.INT_ARGB ||
                   type == javafx.scene.image.PixelFormat.Type.INT_ARGB_PRE;
        }
        // Fallback to format-based detection (svg also supports transparency)
        return "png".equals(imageType) || "gif".equals(imageType) || "svg".equals(imageType);
    }

    // --- Conversion methods ---

    /**
     * Convert the JavaFX image to byte array (ArrayFixedByte).
     * 
     * @return ArrayFixedByte containing the image data
     * @throws InterpreterError if conversion fails
     */
    public ArrayFixedByte getBytes() throws InterpreterError {
        return getBytes(this.imageType);
    }

    /**
     * Convert the JavaFX image to byte array with specified format.
     * 
     * @param format Output format (png, jpg, gif, bmp)
     * @return ArrayFixedByte containing the image data
     * @throws InterpreterError if conversion fails
     */
    public ArrayFixedByte getBytes(String format) throws InterpreterError {
        if (format == null || format.isBlank()) {
            format = this.imageType;
        }
        format = format.toLowerCase();
        
        try {
            // Convert JavaFX image to BufferedImage
            BufferedImage bimg = SwingFXUtils.fromFXImage(fxImage, null);
            
            // For JPG format, remove alpha channel
            if ("jpg".equals(format) || "jpeg".equals(format)) {
                BufferedImage rgbImage = new BufferedImage(
                    bimg.getWidth(), bimg.getHeight(), BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = rgbImage.createGraphics();
                g2d.setColor(java.awt.Color.WHITE);
                g2d.fillRect(0, 0, bimg.getWidth(), bimg.getHeight());
                g2d.drawImage(bimg, 0, 0, null);
                g2d.dispose();
                bimg = rgbImage;
            }
            
            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(bimg, format, baos);
            
            if (!success) {
                // Fallback to PNG
                baos.reset();
                success = ImageIO.write(bimg, "png", baos);
                if (!success) {
                    throw new InterpreterError("EbsImage.getBytes: failed to encode image");
                }
            }
            
            return new ArrayFixedByte(baos.toByteArray());
        } catch (InterpreterError ie) {
            throw ie;
        } catch (IOException ex) {
            throw new InterpreterError("EbsImage.getBytes: " + ex.getMessage());
        }
    }

    // --- Image manipulation methods ---

    /**
     * Resize the image.
     * 
     * @param newWidth Target width
     * @param newHeight Target height
     * @param keepAspect If true, maintain aspect ratio
     * @return A new EbsImage with the resized image
     */
    public EbsImage resize(int newWidth, int newHeight, boolean keepAspect) {
        int origWidth = getWidth();
        int origHeight = getHeight();
        
        int finalWidth = newWidth;
        int finalHeight = newHeight;
        
        if (keepAspect) {
            double widthRatio = (double) newWidth / origWidth;
            double heightRatio = (double) newHeight / origHeight;
            double ratio = Math.min(widthRatio, heightRatio);
            finalWidth = (int) Math.round(origWidth * ratio);
            finalHeight = (int) Math.round(origHeight * ratio);
        }
        
        WritableImage resized = new WritableImage(finalWidth, finalHeight);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = resized.getPixelWriter();
        
        for (int y = 0; y < finalHeight; y++) {
            for (int x = 0; x < finalWidth; x++) {
                int srcX = (int) ((double) x / finalWidth * origWidth);
                int srcY = (int) ((double) y / finalHeight * origHeight);
                srcX = Math.min(srcX, origWidth - 1);
                srcY = Math.min(srcY, origHeight - 1);
                writer.setColor(x, y, reader.getColor(srcX, srcY));
            }
        }
        
        return new EbsImage(resized, imageName, imageType);
    }

    /**
     * Crop the image.
     * 
     * @param x Start x coordinate
     * @param y Start y coordinate
     * @param width Crop width
     * @param height Crop height
     * @return A new EbsImage with the cropped region
     * @throws InterpreterError if crop region is out of bounds
     */
    public EbsImage crop(int x, int y, int width, int height) throws InterpreterError {
        if (x < 0 || y < 0 || x + width > getWidth() || y + height > getHeight()) {
            throw new InterpreterError("EbsImage.crop: region out of bounds");
        }
        
        WritableImage cropped = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = cropped.getPixelWriter();
        
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                writer.setColor(dx, dy, reader.getColor(x + dx, y + dy));
            }
        }
        
        return new EbsImage(cropped, imageName, imageType);
    }

    /**
     * Rotate the image.
     * 
     * @param degrees Rotation angle in degrees
     * @return A new EbsImage with the rotated image
     */
    public EbsImage rotate(double degrees) {
        double radians = Math.toRadians(degrees);
        int origWidth = getWidth();
        int origHeight = getHeight();
        
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth = (int) Math.ceil(origWidth * cos + origHeight * sin);
        int newHeight = (int) Math.ceil(origHeight * cos + origWidth * sin);
        
        WritableImage rotated = new WritableImage(newWidth, newHeight);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = rotated.getPixelWriter();
        
        double centerX = newWidth / 2.0;
        double centerY = newHeight / 2.0;
        double origCenterX = origWidth / 2.0;
        double origCenterY = origHeight / 2.0;
        
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                double srcX = dx * Math.cos(-radians) - dy * Math.sin(-radians) + origCenterX;
                double srcY = dx * Math.sin(-radians) + dy * Math.cos(-radians) + origCenterY;
                
                if (srcX >= 0 && srcX < origWidth && srcY >= 0 && srcY < origHeight) {
                    writer.setColor(x, y, reader.getColor((int) srcX, (int) srcY));
                } else {
                    writer.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        
        return new EbsImage(rotated, imageName, imageType);
    }

    /**
     * Flip the image horizontally.
     * 
     * @return A new EbsImage with the flipped image
     */
    public EbsImage flipHorizontal() {
        int width = getWidth();
        int height = getHeight();
        
        WritableImage flipped = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = flipped.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, reader.getColor(width - 1 - x, y));
            }
        }
        
        return new EbsImage(flipped, imageName, imageType);
    }

    /**
     * Flip the image vertically.
     * 
     * @return A new EbsImage with the flipped image
     */
    public EbsImage flipVertical() {
        int width = getWidth();
        int height = getHeight();
        
        WritableImage flipped = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = flipped.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, reader.getColor(x, height - 1 - y));
            }
        }
        
        return new EbsImage(flipped, imageName, imageType);
    }

    /**
     * Convert to grayscale.
     * 
     * @return A new EbsImage with the grayscale image
     */
    public EbsImage toGrayscale() {
        int width = getWidth();
        int height = getHeight();
        
        WritableImage gray = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = gray.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = reader.getColor(x, y);
                double luminance = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
                writer.setColor(x, y, new Color(luminance, luminance, luminance, c.getOpacity()));
            }
        }
        
        return new EbsImage(gray, imageName, imageType);
    }

    /**
     * Adjust brightness.
     * 
     * @param factor Brightness factor (1.0 = no change, greater = brighter, less = darker)
     * @return A new EbsImage with adjusted brightness
     */
    public EbsImage adjustBrightness(double factor) {
        int width = getWidth();
        int height = getHeight();
        
        WritableImage adjusted = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = adjusted.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = reader.getColor(x, y);
                double r = Math.min(1.0, Math.max(0.0, c.getRed() * factor));
                double g = Math.min(1.0, Math.max(0.0, c.getGreen() * factor));
                double b = Math.min(1.0, Math.max(0.0, c.getBlue() * factor));
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }
        
        return new EbsImage(adjusted, imageName, imageType);
    }

    /**
     * Adjust contrast.
     * 
     * @param factor Contrast factor (1.0 = no change, greater = more contrast, less = less contrast)
     * @return A new EbsImage with adjusted contrast
     */
    public EbsImage adjustContrast(double factor) {
        int width = getWidth();
        int height = getHeight();
        
        WritableImage adjusted = new WritableImage(width, height);
        PixelReader reader = fxImage.getPixelReader();
        PixelWriter writer = adjusted.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = reader.getColor(x, y);
                double r = Math.min(1.0, Math.max(0.0, (c.getRed() - 0.5) * factor + 0.5));
                double g = Math.min(1.0, Math.max(0.0, (c.getGreen() - 0.5) * factor + 0.5));
                double b = Math.min(1.0, Math.max(0.0, (c.getBlue() - 0.5) * factor + 0.5));
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }
        
        return new EbsImage(adjusted, imageName, imageType);
    }

    /**
     * Get a pixel color at specified coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return Color at the specified position
     */
    public Color getPixel(int x, int y) {
        return fxImage.getPixelReader().getColor(x, y);
    }

    /**
     * Set a pixel color at specified coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param color Color to set
     */
    public void setPixel(int x, int y, Color color) {
        fxImage.getPixelWriter().setColor(x, y, color);
    }

    // --- Helper methods ---

    /**
     * Detect image format from byte array and optional file name.
     * Supports PNG, JPG, GIF, BMP, and SVG formats.
     */
    private static String detectFormat(byte[] bytes, String fileName) {
        // First check if it's an SVG by looking at the content
        if (isSvgFormat(bytes)) {
            return "svg";
        }
        
        // Check file extension if name provided
        if (fileName != null && !fileName.isBlank()) {
            String ext = getExtension(fileName).toLowerCase();
            if ("svg".equals(ext)) {
                return "svg";
            }
        }
        
        // Use ImageIO for other formats
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
            
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                String format = reader.getFormatName().toLowerCase();
                if ("jpeg".equals(format)) {
                    return "jpg";
                }
                return format;
            }
        } catch (IOException ignored) {
            // Fall through to default
        }
        return "png"; // Default format
    }
    
    /**
     * Check if byte array contains SVG data.
     */
    private static boolean isSvgFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            return false;
        }
        
        // Skip BOM if present and find start
        int start = 0;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            start = 3; // UTF-8 BOM
        }
        
        // Convert first ~100 bytes to string and check for SVG markers
        int checkLen = Math.min(bytes.length - start, 100);
        String header = new String(bytes, start, checkLen, java.nio.charset.StandardCharsets.UTF_8).trim().toLowerCase();
        
        return header.startsWith("<?xml") && header.contains("svg") ||
               header.startsWith("<svg") ||
               header.contains("<!doctype svg");
    }
    
    /**
     * Get file extension from file name.
     */
    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot + 1);
        }
        return "";
    }

    @Override
    public String toString() {
        return "EbsImage{" +
               "name='" + (imageName != null ? imageName : "unnamed") + '\'' +
               ", type='" + imageType + '\'' +
               ", width=" + getWidth() +
               ", height=" + getHeight() +
               '}';
    }
}
