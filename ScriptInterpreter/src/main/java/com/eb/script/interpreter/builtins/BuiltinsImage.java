package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in functions for Image operations.
 * Handles image.* builtins for loading, saving, resizing, and manipulating images.
 * Images are represented as ArrayFixedByte (byte arrays) for interoperability.
 *
 * @author Earl Bosch
 */
public class BuiltinsImage {

    /**
     * Dispatch an Image builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "image.load")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "image.load" -> load(env, args);
            case "image.save" -> save(env, args);
            case "image.resize" -> resize(env, args);
            case "image.getwidth" -> getWidth(args);
            case "image.getheight" -> getHeight(args);
            case "image.getinfo" -> getInfo(args);
            case "image.crop" -> crop(args);
            case "image.rotate" -> rotate(args);
            case "image.fliphorizontal" -> flipHorizontal(args);
            case "image.flipvertical" -> flipVertical(args);
            case "image.tograyscale" -> toGrayscale(args);
            case "image.adjustbrightness" -> adjustBrightness(args);
            case "image.adjustcontrast" -> adjustContrast(args);
            case "image.frombase64" -> fromBase64(args);
            case "image.tobase64" -> toBase64(args);
            default -> throw new InterpreterError("Unknown Image builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is an Image builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("image.");
    }

    // --- Individual builtin implementations ---

    /**
     * Load an image file into an ArrayFixedByte.
     * image.load(path) -> ARRAY (byte[])
     */
    private static Object load(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String path = (String) args[0];
        if (path == null || path.isBlank()) {
            throw new InterpreterError("image.load: path cannot be null or empty");
        }
        
        try {
            Path p = Util.resolveSandboxedPath(path);
            if (!Files.exists(p)) {
                throw new InterpreterError("image.load: file not found: " + path);
            }
            
            byte[] bytes = Files.readAllBytes(p);
            
            // Validate that it's actually an image
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                BufferedImage img = ImageIO.read(bais);
                if (img == null) {
                    throw new InterpreterError("image.load: file is not a valid image: " + path);
                }
            }
            
            if (env.isEchoOn()) {
                sysOutput(output, "Loaded image: " + path + " (" + bytes.length + " bytes)");
            }
            
            return new ArrayFixedByte(bytes);
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.load: " + ex.getMessage());
        }
    }

    /**
     * Save image bytes to a file.
     * image.save(bytes, path, format?) -> BOOL
     * format defaults to inferring from file extension (png, jpg, gif, bmp)
     */
    private static Object save(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        byte[] bytes = getBytes(args[0], "image.save");
        String path = (String) args[1];
        String format = args.length > 2 ? (String) args[2] : null;
        
        if (path == null || path.isBlank()) {
            throw new InterpreterError("image.save: path cannot be null or empty");
        }
        
        try {
            Path p = Util.resolveSandboxedPath(path);
            
            // Create parent directories if they don't exist
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            
            // Infer format from extension if not provided
            if (format == null || format.isBlank()) {
                format = getFormatFromPath(path);
            }
            
            // Read the image
            BufferedImage img = bytesToImage(bytes, "image.save");
            
            // Write with the specified format
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                boolean success = ImageIO.write(img, format, baos);
                if (!success) {
                    throw new InterpreterError("image.save: unsupported format: " + format);
                }
                Files.write(p, baos.toByteArray());
            }
            
            if (env.isEchoOn()) {
                sysOutput(output, "Saved image: " + path + " (format: " + format + ")");
            }
            
            return Boolean.TRUE;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.save: " + ex.getMessage());
        }
    }

    /**
     * Resize an image.
     * image.resize(bytes, width, height, keepAspect?) -> ARRAY (byte[])
     */
    private static Object resize(Environment env, Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.resize");
        int targetWidth = toPositiveInt(args[1], "image.resize", "width");
        int targetHeight = toPositiveInt(args[2], "image.resize", "height");
        boolean keepAspect = args.length > 3 && args[3] != null ? (Boolean) args[3] : false;
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.resize");
            
            int origWidth = original.getWidth();
            int origHeight = original.getHeight();
            
            int newWidth = targetWidth;
            int newHeight = targetHeight;
            
            if (keepAspect) {
                double widthRatio = (double) targetWidth / origWidth;
                double heightRatio = (double) targetHeight / origHeight;
                double ratio = Math.min(widthRatio, heightRatio);
                newWidth = (int) Math.round(origWidth * ratio);
                newHeight = (int) Math.round(origHeight * ratio);
            }
            
            BufferedImage resized = new BufferedImage(newWidth, newHeight, getImageType(original));
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            return imageToBytes(resized, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.resize: " + ex.getMessage());
        }
    }

    /**
     * Get image width.
     * image.getWidth(bytes) -> INTEGER
     */
    private static Object getWidth(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.getWidth");
        try {
            BufferedImage img = bytesToImage(bytes, "image.getWidth");
            return img.getWidth();
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.getWidth: " + ex.getMessage());
        }
    }

    /**
     * Get image height.
     * image.getHeight(bytes) -> INTEGER
     */
    private static Object getHeight(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.getHeight");
        try {
            BufferedImage img = bytesToImage(bytes, "image.getHeight");
            return img.getHeight();
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.getHeight: " + ex.getMessage());
        }
    }

    /**
     * Get image metadata as JSON.
     * image.getInfo(bytes) -> JSON { width, height, format, hasAlpha }
     */
    private static Object getInfo(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.getInfo");
        try {
            BufferedImage img = bytesToImage(bytes, "image.getInfo");
            String format = detectFormat(bytes);
            
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("width", img.getWidth());
            info.put("height", img.getHeight());
            info.put("format", format);
            info.put("hasAlpha", img.getColorModel().hasAlpha());
            info.put("sizeBytes", bytes.length);
            
            return info;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.getInfo: " + ex.getMessage());
        }
    }

    /**
     * Crop an image.
     * image.crop(bytes, x, y, width, height) -> ARRAY (byte[])
     */
    private static Object crop(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.crop");
        int x = toNonNegativeInt(args[1], "image.crop", "x");
        int y = toNonNegativeInt(args[2], "image.crop", "y");
        int width = toPositiveInt(args[3], "image.crop", "width");
        int height = toPositiveInt(args[4], "image.crop", "height");
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.crop");
            
            // Validate crop bounds
            if (x + width > original.getWidth()) {
                throw new InterpreterError("image.crop: x + width exceeds image width");
            }
            if (y + height > original.getHeight()) {
                throw new InterpreterError("image.crop: y + height exceeds image height");
            }
            
            BufferedImage cropped = original.getSubimage(x, y, width, height);
            
            // Create a new BufferedImage to prevent issues with getSubimage
            BufferedImage result = new BufferedImage(width, height, getImageType(original));
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(cropped, 0, 0, null);
            g2d.dispose();
            
            return imageToBytes(result, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.crop: " + ex.getMessage());
        }
    }

    /**
     * Rotate an image.
     * image.rotate(bytes, degrees) -> ARRAY (byte[])
     * degrees: 90, 180, 270 (or any angle)
     */
    private static Object rotate(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.rotate");
        double degrees = toDouble(args[1], "image.rotate", "degrees");
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.rotate");
            double radians = Math.toRadians(degrees);
            
            // Calculate new dimensions
            int origWidth = original.getWidth();
            int origHeight = original.getHeight();
            
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int newWidth = (int) Math.floor(origWidth * cos + origHeight * sin);
            int newHeight = (int) Math.floor(origHeight * cos + origWidth * sin);
            
            BufferedImage rotated = new BufferedImage(newWidth, newHeight, getImageType(original));
            Graphics2D g2d = rotated.createGraphics();
            
            // Make background transparent or white depending on alpha support
            if (original.getColorModel().hasAlpha()) {
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, newWidth, newHeight);
                g2d.setComposite(AlphaComposite.SrcOver);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, newWidth, newHeight);
            }
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.translate((newWidth - origWidth) / 2.0, (newHeight - origHeight) / 2.0);
            g2d.rotate(radians, origWidth / 2.0, origHeight / 2.0);
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            
            return imageToBytes(rotated, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.rotate: " + ex.getMessage());
        }
    }

    /**
     * Flip an image horizontally.
     * image.flipHorizontal(bytes) -> ARRAY (byte[])
     */
    private static Object flipHorizontal(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.flipHorizontal");
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.flipHorizontal");
            
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-original.getWidth(), 0);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage flipped = op.filter(original, null);
            
            return imageToBytes(flipped, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.flipHorizontal: " + ex.getMessage());
        }
    }

    /**
     * Flip an image vertically.
     * image.flipVertical(bytes) -> ARRAY (byte[])
     */
    private static Object flipVertical(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.flipVertical");
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.flipVertical");
            
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -original.getHeight());
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage flipped = op.filter(original, null);
            
            return imageToBytes(flipped, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.flipVertical: " + ex.getMessage());
        }
    }

    /**
     * Convert an image to grayscale.
     * image.toGrayscale(bytes) -> ARRAY (byte[])
     */
    private static Object toGrayscale(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.toGrayscale");
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.toGrayscale");
            
            BufferedImage grayscale = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = grayscale.createGraphics();
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            
            return imageToBytes(grayscale, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.toGrayscale: " + ex.getMessage());
        }
    }

    /**
     * Adjust image brightness.
     * image.adjustBrightness(bytes, factor) -> ARRAY (byte[])
     * factor: 1.0 = no change, &gt;1.0 = brighter, &lt;1.0 = darker
     */
    private static Object adjustBrightness(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.adjustBrightness");
        float factor = toFloat(args[1], "image.adjustBrightness", "factor");
        
        if (factor < 0) {
            throw new InterpreterError("image.adjustBrightness: factor must be non-negative");
        }
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.adjustBrightness");
            
            // Convert to compatible format for RescaleOp if needed
            BufferedImage compatible = ensureCompatibleImage(original);
            
            RescaleOp rescaleOp = new RescaleOp(factor, 0, null);
            BufferedImage brightened = rescaleOp.filter(compatible, null);
            
            return imageToBytes(brightened, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.adjustBrightness: " + ex.getMessage());
        }
    }

    /**
     * Adjust image contrast.
     * image.adjustContrast(bytes, factor) -> ARRAY (byte[])
     * factor: 1.0 = no change, &gt;1.0 = more contrast, &lt;1.0 = less contrast
     */
    private static Object adjustContrast(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.adjustContrast");
        float factor = toFloat(args[1], "image.adjustContrast", "factor");
        
        if (factor < 0) {
            throw new InterpreterError("image.adjustContrast: factor must be non-negative");
        }
        
        try {
            BufferedImage original = bytesToImage(bytes, "image.adjustContrast");
            
            // Contrast adjustment: offset = 128 * (1 - factor) shifts the middle gray
            float offset = 128.0f * (1.0f - factor);
            
            // Convert to compatible format for RescaleOp if needed
            BufferedImage compatible = ensureCompatibleImage(original);
            
            RescaleOp rescaleOp = new RescaleOp(factor, offset, null);
            BufferedImage adjusted = rescaleOp.filter(compatible, null);
            
            return imageToBytes(adjusted, detectFormat(bytes));
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.adjustContrast: " + ex.getMessage());
        }
    }

    /**
     * Load image from base64 encoded string.
     * image.fromBase64(b64String) -> ARRAY (byte[])
     */
    private static Object fromBase64(Object[] args) throws InterpreterError {
        String b64 = (String) args[0];
        if (b64 == null || b64.isBlank()) {
            throw new InterpreterError("image.fromBase64: base64 string cannot be null or empty");
        }
        
        try {
            // Handle data URI scheme (e.g., "data:image/png;base64,...")
            if (b64.contains(",")) {
                b64 = b64.substring(b64.indexOf(",") + 1);
            }
            
            byte[] bytes = Base64.getDecoder().decode(b64);
            
            // Validate that it's actually an image
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                BufferedImage img = ImageIO.read(bais);
                if (img == null) {
                    throw new InterpreterError("image.fromBase64: decoded data is not a valid image");
                }
            }
            
            return new ArrayFixedByte(bytes);
        } catch (InterpreterError ie) {
            throw ie;
        } catch (IllegalArgumentException ex) {
            throw new InterpreterError("image.fromBase64: invalid base64 encoding: " + ex.getMessage());
        } catch (Exception ex) {
            throw new InterpreterError("image.fromBase64: " + ex.getMessage());
        }
    }

    /**
     * Convert image to base64 encoded string.
     * image.toBase64(bytes, format?) -> STRING
     */
    private static Object toBase64(Object[] args) throws InterpreterError {
        byte[] bytes = getBytes(args[0], "image.toBase64");
        String format = args.length > 1 ? (String) args[1] : null;
        
        try {
            if (format == null || format.isBlank()) {
                format = detectFormat(bytes);
            }
            
            // Re-encode with specified format if different from original
            BufferedImage img = bytesToImage(bytes, "image.toBase64");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                boolean success = ImageIO.write(img, format, baos);
                if (!success) {
                    throw new InterpreterError("image.toBase64: unsupported format: " + format);
                }
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.toBase64: " + ex.getMessage());
        }
    }

    // --- Helper methods ---

    /**
     * Extract byte array from argument (supports ArrayFixedByte and byte[]).
     */
    private static byte[] getBytes(Object arg, String funcName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(funcName + ": image bytes cannot be null");
        }
        if (arg instanceof ArrayFixedByte afb) {
            return afb.elements;
        }
        if (arg instanceof byte[] ba) {
            return ba;
        }
        throw new InterpreterError(funcName + ": expected byte array (ArrayFixedByte)");
    }

    /**
     * Convert byte array to BufferedImage.
     */
    private static BufferedImage bytesToImage(byte[] bytes, String funcName) throws InterpreterError {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new InterpreterError(funcName + ": invalid image data");
            }
            return img;
        } catch (IOException ex) {
            throw new InterpreterError(funcName + ": error reading image: " + ex.getMessage());
        }
    }

    /**
     * Convert BufferedImage to byte array in specified format.
     */
    private static ArrayFixedByte imageToBytes(BufferedImage img, String format) throws InterpreterError {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean success = ImageIO.write(img, format, baos);
            if (!success) {
                // Fallback to PNG if format not supported
                success = ImageIO.write(img, "png", baos);
                if (!success) {
                    throw new InterpreterError("Failed to encode image");
                }
            }
            return new ArrayFixedByte(baos.toByteArray());
        } catch (IOException ex) {
            throw new InterpreterError("Error encoding image: " + ex.getMessage());
        }
    }

    /**
     * Detect image format from byte array.
     */
    private static String detectFormat(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
            
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName().toLowerCase();
            }
        } catch (IOException ignored) {
            // Fall through to default
        }
        return "png"; // Default format
    }

    /**
     * Get format from file path extension.
     */
    private static String getFormatFromPath(String path) {
        int dot = path.lastIndexOf('.');
        if (dot > 0 && dot < path.length() - 1) {
            String ext = path.substring(dot + 1).toLowerCase();
            return switch (ext) {
                case "jpg", "jpeg" -> "jpg";
                case "png" -> "png";
                case "gif" -> "gif";
                case "bmp" -> "bmp";
                default -> "png";
            };
        }
        return "png";
    }

    /**
     * Get appropriate BufferedImage type, preserving alpha if present.
     */
    private static int getImageType(BufferedImage img) {
        return img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    }

    /**
     * Ensure image is in a compatible format for operations like RescaleOp.
     */
    private static BufferedImage ensureCompatibleImage(BufferedImage img) {
        int type = img.getType();
        if (type == BufferedImage.TYPE_BYTE_INDEXED || type == BufferedImage.TYPE_BYTE_BINARY
            || type == BufferedImage.TYPE_CUSTOM) {
            // Convert to a compatible type
            BufferedImage compatible = new BufferedImage(
                img.getWidth(), img.getHeight(), 
                img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = compatible.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            return compatible;
        }
        return img;
    }

    /**
     * Convert argument to positive integer.
     */
    private static int toPositiveInt(Object arg, String funcName, String paramName) throws InterpreterError {
        if (!(arg instanceof Number)) {
            throw new InterpreterError(funcName + ": " + paramName + " must be a number");
        }
        int value = ((Number) arg).intValue();
        if (value <= 0) {
            throw new InterpreterError(funcName + ": " + paramName + " must be positive");
        }
        return value;
    }

    /**
     * Convert argument to non-negative integer.
     */
    private static int toNonNegativeInt(Object arg, String funcName, String paramName) throws InterpreterError {
        if (!(arg instanceof Number)) {
            throw new InterpreterError(funcName + ": " + paramName + " must be a number");
        }
        int value = ((Number) arg).intValue();
        if (value < 0) {
            throw new InterpreterError(funcName + ": " + paramName + " must be non-negative");
        }
        return value;
    }

    /**
     * Convert argument to double.
     */
    private static double toDouble(Object arg, String funcName, String paramName) throws InterpreterError {
        if (!(arg instanceof Number)) {
            throw new InterpreterError(funcName + ": " + paramName + " must be a number");
        }
        return ((Number) arg).doubleValue();
    }

    /**
     * Convert argument to float.
     */
    private static float toFloat(Object arg, String funcName, String paramName) throws InterpreterError {
        if (!(arg instanceof Number)) {
            throw new InterpreterError(funcName + ": " + paramName + " must be a number");
        }
        return ((Number) arg).floatValue();
    }

    /**
     * Output message to script area or stdout.
     */
    private static void sysOutput(ScriptArea output, String message) {
        if (output != null) {
            output.println(message);
        } else {
            System.out.println(message);
        }
    }
}
