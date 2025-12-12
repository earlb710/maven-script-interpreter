package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsImage;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in functions for Image operations.
 * Handles image.* builtins for loading, saving, resizing, and manipulating images.
 * Images are represented as EbsImage which wraps JavaFX Image for manipulation
 * and can be converted to/from ArrayFixedByte (byte arrays) for interoperability.
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
            case "image.resize" -> resize(args);
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
            case "image.getbytes" -> getBytes(args);
            case "image.getname" -> getName(args);
            case "image.setname" -> setName(args);
            case "image.gettype" -> getType(args);
            case "image.settype" -> setType(args);
            case "image.create" -> create(args);
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
     * Load an image file and create an EbsImage.
     * image.load(path, width?, height?) -> IMAGE
     * If width and height are provided, the image is automatically resized.
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
            String fileName = p.getFileName().toString();
            
            EbsImage image = new EbsImage(bytes, fileName);
            
            // Auto-resize if width and height are provided
            if (args.length > 2 && args[1] != null && args[2] != null) {
                int targetWidth = toPositiveInt(args[1], "image.load", "width");
                int targetHeight = toPositiveInt(args[2], "image.load", "height");
                image = image.resize(targetWidth, targetHeight, false);
                
                if (env.isEchoOn()) {
                    sysOutput(output, "Loaded and resized image: " + path + " to " + targetWidth + "x" + targetHeight + " (" + image.getImageType() + ")");
                }
            } else {
                if (env.isEchoOn()) {
                    sysOutput(output, "Loaded image: " + path + " (" + image.getWidth() + "x" + image.getHeight() + ", " + image.getImageType() + ")");
                }
            }
            
            return image;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.load: " + ex.getMessage());
        }
    }

    /**
     * Create an EbsImage from ArrayFixedByte data.
     * image.create(bytes, name?, type?) -> IMAGE
     */
    private static Object create(Object[] args) throws InterpreterError {
        // Get bytes from first argument
        byte[] bytes;
        if (args[0] instanceof ArrayFixedByte afb) {
            bytes = afb.elements;
        } else if (args[0] instanceof byte[] ba) {
            bytes = ba;
        } else {
            throw new InterpreterError("image.create: expected byte array");
        }
        
        String name = args.length > 1 ? (String) args[1] : null;
        String type = args.length > 2 ? (String) args[2] : null;
        
        return new EbsImage(bytes, name, type);
    }

    /**
     * Save EbsImage to a file.
     * image.save(image, path, format?) -> BOOL
     */
    private static Object save(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        EbsImage image = getImage(args[0], "image.save");
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
            
            // Get bytes from image
            ArrayFixedByte bytes = image.getBytes(format);
            Files.write(p, bytes.elements);
            
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
     * image.resize(image, width, height, keepAspect?) -> IMAGE
     */
    private static Object resize(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.resize");
        int targetWidth = toPositiveInt(args[1], "image.resize", "width");
        int targetHeight = toPositiveInt(args[2], "image.resize", "height");
        boolean keepAspect = args.length > 3 && args[3] != null ? (Boolean) args[3] : false;
        
        return image.resize(targetWidth, targetHeight, keepAspect);
    }

    /**
     * Get image width.
     * image.getWidth(image) -> INTEGER
     */
    private static Object getWidth(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getWidth");
        return image.getWidth();
    }

    /**
     * Get image height.
     * image.getHeight(image) -> INTEGER
     */
    private static Object getHeight(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getHeight");
        return image.getHeight();
    }

    /**
     * Get image metadata as JSON.
     * image.getInfo(image) -> JSON { width, height, type, name, hasAlpha }
     */
    private static Object getInfo(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getInfo");
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("width", image.getWidth());
        info.put("height", image.getHeight());
        info.put("type", image.getImageType());
        info.put("name", image.getImageName());
        info.put("hasAlpha", image.hasAlpha());
        
        return info;
    }

    /**
     * Crop an image.
     * image.crop(image, x, y, width, height) -> IMAGE
     */
    private static Object crop(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.crop");
        int x = toNonNegativeInt(args[1], "image.crop", "x");
        int y = toNonNegativeInt(args[2], "image.crop", "y");
        int width = toPositiveInt(args[3], "image.crop", "width");
        int height = toPositiveInt(args[4], "image.crop", "height");
        
        return image.crop(x, y, width, height);
    }

    /**
     * Rotate an image.
     * image.rotate(image, degrees) -> IMAGE
     */
    private static Object rotate(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.rotate");
        double degrees = toDouble(args[1], "image.rotate", "degrees");
        
        return image.rotate(degrees);
    }

    /**
     * Flip an image horizontally.
     * image.flipHorizontal(image) -> IMAGE
     */
    private static Object flipHorizontal(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.flipHorizontal");
        return image.flipHorizontal();
    }

    /**
     * Flip an image vertically.
     * image.flipVertical(image) -> IMAGE
     */
    private static Object flipVertical(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.flipVertical");
        return image.flipVertical();
    }

    /**
     * Convert an image to grayscale.
     * image.toGrayscale(image) -> IMAGE
     */
    private static Object toGrayscale(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.toGrayscale");
        return image.toGrayscale();
    }

    /**
     * Adjust image brightness.
     * image.adjustBrightness(image, factor) -> IMAGE
     */
    private static Object adjustBrightness(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.adjustBrightness");
        float factor = toFloat(args[1], "image.adjustBrightness", "factor");
        
        if (factor < 0) {
            throw new InterpreterError("image.adjustBrightness: factor must be non-negative");
        }
        
        return image.adjustBrightness(factor);
    }

    /**
     * Adjust image contrast.
     * image.adjustContrast(image, factor) -> IMAGE
     */
    private static Object adjustContrast(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.adjustContrast");
        float factor = toFloat(args[1], "image.adjustContrast", "factor");
        
        if (factor < 0) {
            throw new InterpreterError("image.adjustContrast: factor must be non-negative");
        }
        
        return image.adjustContrast(factor);
    }

    /**
     * Load image from base64 encoded string.
     * image.fromBase64(b64String) -> IMAGE
     * Uses BuiltinsCrypto.decodeBase64() for base64 decoding.
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
            
            byte[] bytes = BuiltinsCrypto.decodeBase64(b64);
            return new EbsImage(bytes);
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
     * image.toBase64(image, format?) -> STRING
     * Uses BuiltinsCrypto.encodeBase64() for base64 encoding.
     */
    private static Object toBase64(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.toBase64");
        String format = args.length > 1 ? (String) args[1] : null;
        
        try {
            ArrayFixedByte bytes = image.getBytes(format);
            return BuiltinsCrypto.encodeBase64(bytes.elements);
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("image.toBase64: " + ex.getMessage());
        }
    }

    /**
     * Get image as ArrayFixedByte.
     * image.getBytes(image, format?) -> ARRAY
     */
    private static Object getBytes(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getBytes");
        String format = args.length > 1 ? (String) args[1] : null;
        
        return image.getBytes(format);
    }

    /**
     * Get image name.
     * image.getName(image) -> STRING
     */
    private static Object getName(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getName");
        return image.getImageName();
    }

    /**
     * Set image name.
     * image.setName(image, name) -> IMAGE
     */
    private static Object setName(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.setName");
        String name = (String) args[1];
        image.setImageName(name);
        return image;
    }

    /**
     * Get image type/format.
     * image.getType(image) -> STRING
     */
    private static Object getType(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.getType");
        return image.getImageType();
    }

    /**
     * Set image type/format.
     * image.setType(image, type) -> IMAGE
     */
    private static Object setType(Object[] args) throws InterpreterError {
        EbsImage image = getImage(args[0], "image.setType");
        String type = (String) args[1];
        image.setImageType(type);
        return image;
    }

    // --- Helper methods ---

    /**
     * Extract EbsImage from argument (supports EbsImage and ArrayFixedByte).
     */
    private static EbsImage getImage(Object arg, String funcName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(funcName + ": image cannot be null");
        }
        if (arg instanceof EbsImage img) {
            return img;
        }
        if (arg instanceof ArrayFixedByte afb) {
            return new EbsImage(afb);
        }
        if (arg instanceof byte[] ba) {
            return new EbsImage(ba);
        }
        throw new InterpreterError(funcName + ": expected EbsImage or ArrayFixedByte");
    }

    /**
     * Get format from file path extension.
     * Supports png, jpg, gif, bmp, and svg formats.
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
                case "svg" -> "svg";
                default -> "png";
            };
        }
        return "png";
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
