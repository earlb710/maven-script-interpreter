package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsImage;
import com.eb.script.image.EbsVectorImage;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in functions for Vector Image operations.
 * Handles vector.* builtins for loading, saving, and manipulating SVG images.
 * Vector images are represented as EbsVectorImage which wraps SVG DOM for manipulation
 * and can be converted to EbsImage (rasterized) for display.
 *
 * @author Earl Bosch
 */
public class BuiltinsVectorImage {

    /**
     * Dispatch a Vector Image builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "vector.load")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "vector.load" -> load(env, args);
            case "vector.save" -> save(env, args);
            case "vector.create" -> create(args);
            case "vector.getwidth" -> getWidth(args);
            case "vector.getheight" -> getHeight(args);
            case "vector.getinfo" -> getInfo(args);
            case "vector.scale" -> scale(args);
            case "vector.setfillcolor" -> setFillColor(args);
            case "vector.setstrokecolor" -> setStrokeColor(args);
            case "vector.setstrokewidth" -> setStrokeWidth(args);
            case "vector.rotate" -> rotate(args);
            case "vector.setdimensions" -> setDimensions(args);
            case "vector.toraster" -> toRaster(args);
            case "vector.toimage" -> toRaster(args); // alias
            case "vector.tobytes" -> toBytes(args);
            case "vector.tostring" -> toSvgString(args);
            case "vector.getname" -> getName(args);
            case "vector.setname" -> setName(args);
            case "vector.applyblur" -> applyBlur(args);
            case "vector.applydropshadow" -> applyDropShadow(args);
            case "vector.applygrayscale" -> applyGrayscale(args);
            case "vector.applysepia" -> applySepia(args);
            case "vector.applybrightness" -> applyBrightness(args);
            case "vector.applyhuerotate" -> applyHueRotate(args);
            default -> throw new InterpreterError("Unknown Vector Image builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Vector Image builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("vector.");
    }

    // --- Individual builtin implementations ---

    /**
     * Load an SVG file and create an EbsVectorImage.
     * vector.load(path) -> VECTOR_IMAGE
     */
    private static Object load(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        String path = (String) args[0];
        if (path == null || path.isBlank()) {
            throw new InterpreterError("vector.load: path cannot be null or empty");
        }
        
        try {
            Path p = Util.resolveSandboxedPath(path);
            if (!Files.exists(p)) {
                throw new InterpreterError("vector.load: file not found: " + path);
            }
            
            byte[] bytes = Files.readAllBytes(p);
            String fileName = p.getFileName().toString();
            
            EbsVectorImage image = new EbsVectorImage(bytes, fileName);
            
            if (env.isEchoOn()) {
                sysOutput(output, "Loaded vector image: " + path + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            }
            
            return image;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("vector.load: " + ex.getMessage());
        }
    }

    /**
     * Create an EbsVectorImage from ArrayFixedByte SVG data.
     * vector.create(bytes, name?) -> VECTOR_IMAGE
     */
    private static Object create(Object[] args) throws InterpreterError {
        // Get bytes from first argument
        byte[] bytes;
        if (args[0] instanceof ArrayFixedByte afb) {
            bytes = afb.elements;
        } else if (args[0] instanceof byte[] ba) {
            bytes = ba;
        } else {
            throw new InterpreterError("vector.create: expected byte array");
        }
        
        String name = args.length > 1 ? (String) args[1] : null;
        
        return new EbsVectorImage(bytes, name);
    }

    /**
     * Save EbsVectorImage to an SVG file.
     * vector.save(vectorImage, path) -> BOOL
     */
    private static Object save(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        EbsVectorImage image = getVectorImage(args[0], "vector.save");
        String path = (String) args[1];
        
        if (path == null || path.isBlank()) {
            throw new InterpreterError("vector.save: path cannot be null or empty");
        }
        
        try {
            Path p = Util.resolveSandboxedPath(path);
            
            // Create parent directories if they don't exist
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            
            // Get bytes from vector image
            ArrayFixedByte bytes = image.toBytes();
            Files.write(p, bytes.elements);
            
            if (env.isEchoOn()) {
                sysOutput(output, "Saved vector image: " + path);
            }
            
            return Boolean.TRUE;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("vector.save: " + ex.getMessage());
        }
    }

    /**
     * Get vector image width.
     * vector.getwidth(vectorImage) -> DOUBLE
     */
    private static Object getWidth(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.getWidth");
        return image.getWidth();
    }

    /**
     * Get vector image height.
     * vector.getheight(vectorImage) -> DOUBLE
     */
    private static Object getHeight(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.getHeight");
        return image.getHeight();
    }

    /**
     * Get vector image metadata as JSON.
     * vector.getinfo(vectorImage) -> JSON { width, height, name }
     */
    private static Object getInfo(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.getInfo");
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("width", image.getWidth());
        info.put("height", image.getHeight());
        info.put("name", image.getImageName());
        info.put("type", "svg");
        
        return info;
    }

    /**
     * Scale a vector image.
     * vector.scale(vectorImage, scaleX, scaleY) -> VECTOR_IMAGE
     */
    private static Object scale(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.scale");
        double scaleX = toDouble(args[1], "vector.scale", "scaleX");
        double scaleY = toDouble(args[2], "vector.scale", "scaleY");
        
        return image.scale(scaleX, scaleY);
    }

    /**
     * Set fill color of vector image.
     * vector.setFillColor(vectorImage, color) -> VECTOR_IMAGE
     */
    private static Object setFillColor(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.setFillColor");
        String color = (String) args[1];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("vector.setFillColor: color cannot be null or empty");
        }
        
        return image.setFillColor(color);
    }

    /**
     * Set stroke color of vector image.
     * vector.setStrokeColor(vectorImage, color) -> VECTOR_IMAGE
     */
    private static Object setStrokeColor(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.setStrokeColor");
        String color = (String) args[1];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("vector.setStrokeColor: color cannot be null or empty");
        }
        
        return image.setStrokeColor(color);
    }

    /**
     * Set stroke width of vector image.
     * vector.setStrokeWidth(vectorImage, width) -> VECTOR_IMAGE
     */
    private static Object setStrokeWidth(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.setStrokeWidth");
        double width = toDouble(args[1], "vector.setStrokeWidth", "width");
        
        return image.setStrokeWidth(width);
    }

    /**
     * Rotate a vector image.
     * vector.rotate(vectorImage, degrees) -> VECTOR_IMAGE
     */
    private static Object rotate(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.rotate");
        double degrees = toDouble(args[1], "vector.rotate", "degrees");
        
        return image.rotate(degrees);
    }

    /**
     * Set dimensions of vector image.
     * vector.setDimensions(vectorImage, width, height) -> VECTOR_IMAGE
     */
    private static Object setDimensions(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.setDimensions");
        double width = toDouble(args[1], "vector.setDimensions", "width");
        double height = toDouble(args[2], "vector.setDimensions", "height");
        
        return image.setDimensions(width, height);
    }

    /**
     * Convert vector image to rasterized EbsImage.
     * vector.toRaster(vectorImage, width?, height?) -> IMAGE
     */
    private static Object toRaster(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.toRaster");
        
        if (args.length >= 3 && args[1] != null && args[2] != null) {
            int width = toPositiveInt(args[1], "vector.toRaster", "width");
            int height = toPositiveInt(args[2], "vector.toRaster", "height");
            return image.toRasterImage(width, height);
        } else {
            return image.toRasterImage();
        }
    }

    /**
     * Get vector image as byte array.
     * vector.toBytes(vectorImage) -> ARRAY
     */
    private static Object toBytes(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.toBytes");
        return image.toBytes();
    }

    /**
     * Get vector image as SVG string.
     * vector.toString(vectorImage) -> STRING
     */
    private static Object toSvgString(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.toString");
        return image.toSvgString();
    }

    /**
     * Get vector image name.
     * vector.getName(vectorImage) -> STRING
     */
    private static Object getName(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.getName");
        return image.getImageName();
    }

    /**
     * Set vector image name.
     * vector.setName(vectorImage, name) -> VECTOR_IMAGE
     */
    private static Object setName(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.setName");
        String name = (String) args[1];
        image.setImageName(name);
        return image;
    }

    // --- Filter Effect methods ---

    /**
     * Apply blur filter to vector image.
     * vector.applyBlur(vectorImage, radius) -> VECTOR_IMAGE
     */
    private static Object applyBlur(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applyBlur");
        double radius = toDouble(args[1], "vector.applyBlur", "radius");
        
        return image.applyBlur(radius);
    }

    /**
     * Apply drop shadow filter to vector image.
     * vector.applyDropShadow(vectorImage, dx, dy, blur, color) -> VECTOR_IMAGE
     */
    private static Object applyDropShadow(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applyDropShadow");
        double dx = toDouble(args[1], "vector.applyDropShadow", "dx");
        double dy = toDouble(args[2], "vector.applyDropShadow", "dy");
        double blur = toDouble(args[3], "vector.applyDropShadow", "blur");
        String color = (String) args[4];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("vector.applyDropShadow: color cannot be null or empty");
        }
        
        return image.applyDropShadow(dx, dy, blur, color);
    }

    /**
     * Apply grayscale filter to vector image.
     * vector.applyGrayscale(vectorImage) -> VECTOR_IMAGE
     */
    private static Object applyGrayscale(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applyGrayscale");
        return image.applyGrayscale();
    }

    /**
     * Apply sepia filter to vector image.
     * vector.applySepia(vectorImage) -> VECTOR_IMAGE
     */
    private static Object applySepia(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applySepia");
        return image.applySepia();
    }

    /**
     * Apply brightness adjustment to vector image.
     * vector.applyBrightness(vectorImage, factor) -> VECTOR_IMAGE
     */
    private static Object applyBrightness(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applyBrightness");
        double factor = toDouble(args[1], "vector.applyBrightness", "factor");
        
        return image.applyBrightness(factor);
    }

    /**
     * Apply hue rotation to vector image.
     * vector.applyHueRotate(vectorImage, degrees) -> VECTOR_IMAGE
     */
    private static Object applyHueRotate(Object[] args) throws InterpreterError {
        EbsVectorImage image = getVectorImage(args[0], "vector.applyHueRotate");
        double degrees = toDouble(args[1], "vector.applyHueRotate", "degrees");
        
        return image.applyHueRotate(degrees);
    }

    // --- Helper methods ---

    /**
     * Extract EbsVectorImage from argument.
     */
    private static EbsVectorImage getVectorImage(Object arg, String funcName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(funcName + ": vector image cannot be null");
        }
        if (arg instanceof EbsVectorImage img) {
            return img;
        }
        if (arg instanceof ArrayFixedByte afb) {
            return new EbsVectorImage(afb);
        }
        if (arg instanceof byte[] ba) {
            return new EbsVectorImage(ba);
        }
        throw new InterpreterError(funcName + ": expected EbsVectorImage or ArrayFixedByte");
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
     * Convert argument to double.
     */
    private static double toDouble(Object arg, String funcName, String paramName) throws InterpreterError {
        if (!(arg instanceof Number)) {
            throw new InterpreterError(funcName + ": " + paramName + " must be a number");
        }
        return ((Number) arg).doubleValue();
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
