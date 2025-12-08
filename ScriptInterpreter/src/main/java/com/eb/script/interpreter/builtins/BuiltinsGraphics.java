package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayFixed;
import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsCanvas;
import com.eb.script.image.EbsImage;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Built-in functions for JavaFX Canvas drawing operations.
 * Handles canvas.*, draw.*, style.*, effect.*, and transform.* builtins
 * for creating and manipulating graphics using JavaFX Canvas.
 * 
 * @author Earl Bosch
 */
public class BuiltinsGraphics {

    /**
     * Dispatch a Graphics builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "canvas.create")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            // Canvas operations
            case "canvas.create" -> canvasCreate(args);
            case "canvas.save" -> canvasSave(env, args);
            case "canvas.toimage" -> canvasToImage(args);
            case "canvas.clear" -> canvasClear(args);
            case "canvas.getwidth" -> canvasGetWidth(args);
            case "canvas.getheight" -> canvasGetHeight(args);
            case "canvas.snapshot" -> canvasSnapshot(args);
            case "canvas.getname" -> canvasGetName(args);
            case "canvas.setname" -> canvasSetName(args);
            
            // Drawing operations
            case "draw.line" -> drawLine(args);
            case "draw.rect" -> drawRect(args);
            case "draw.circle" -> drawCircle(args);
            case "draw.ellipse" -> drawEllipse(args);
            case "draw.arc" -> drawArc(args);
            case "draw.polygon" -> drawPolygon(args);
            case "draw.text" -> drawText(args);
            case "draw.image" -> drawImage(args);
            
            // Style operations
            case "style.setstroke" -> styleSetStroke(args);
            case "style.setfill" -> styleSetFill(args);
            case "style.setfont" -> styleSetFont(args);
            case "style.setlinecap" -> styleSetLineCap(args);
            case "style.setlinejoin" -> styleSetLineJoin(args);
            
            // Effect operations
            case "effect.setshadow" -> effectSetShadow(args);
            case "effect.clearshadow" -> effectClearShadow(args);
            case "effect.setglobalalpha" -> effectSetGlobalAlpha(args);
            
            // Transform operations
            case "transform.save" -> transformSave(args);
            case "transform.restore" -> transformRestore(args);
            case "transform.translate" -> transformTranslate(args);
            case "transform.rotate" -> transformRotate(args);
            case "transform.scale" -> transformScale(args);
            
            default -> throw new InterpreterError("Unknown Graphics builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Graphics builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("canvas.") 
            || name.startsWith("draw.") 
            || name.startsWith("style.") 
            || name.startsWith("effect.") 
            || name.startsWith("transform.");
    }

    // ==================== Canvas Operations ====================

    /**
     * Create a new canvas with specified dimensions.
     * canvas.create(width, height, name?) -> CANVAS
     */
    private static Object canvasCreate(Object[] args) throws InterpreterError {
        int width = toPositiveInt(args[0], "canvas.create", "width");
        int height = toPositiveInt(args[1], "canvas.create", "height");
        String name = args.length > 2 && args[2] != null ? (String) args[2] : null;
        
        return new EbsCanvas(width, height, name);
    }

    /**
     * Save canvas to file as image.
     * canvas.save(canvas, path, format?) -> BOOL
     */
    private static Object canvasSave(Environment env, Object[] args) throws InterpreterError {
        ScriptArea output = env.getOutputArea();
        EbsCanvas canvas = getCanvas(args[0], "canvas.save");
        String path = (String) args[1];
        String format = args.length > 2 && args[2] != null ? (String) args[2] : "png";
        
        if (path == null || path.isBlank()) {
            throw new InterpreterError("canvas.save: path cannot be null or empty");
        }
        
        try {
            Path p = Util.resolveSandboxedPath(path);
            
            // Create parent directories if they don't exist
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            
            // Get bytes from canvas
            ArrayFixedByte bytes = canvas.getBytes(format);
            Files.write(p, bytes.elements);
            
            if (env.isEchoOn()) {
                sysOutput(output, "Saved canvas: " + path + " (format: " + format + ")");
            }
            
            return Boolean.TRUE;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("canvas.save: " + ex.getMessage());
        }
    }

    /**
     * Convert canvas to image.
     * canvas.toImage(canvas) -> IMAGE
     */
    private static Object canvasToImage(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.toImage");
        return canvas.toImage();
    }

    /**
     * Clear the canvas.
     * canvas.clear(canvas) -> NULL
     */
    private static Object canvasClear(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.clear");
        canvas.clear();
        return null;
    }

    /**
     * Get canvas width.
     * canvas.getWidth(canvas) -> INTEGER
     */
    private static Object canvasGetWidth(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.getWidth");
        return canvas.getWidth();
    }

    /**
     * Get canvas height.
     * canvas.getHeight(canvas) -> INTEGER
     */
    private static Object canvasGetHeight(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.getHeight");
        return canvas.getHeight();
    }

    /**
     * Take a snapshot of canvas region as image.
     * canvas.snapshot(canvas, x?, y?, width?, height?) -> IMAGE
     */
    private static Object canvasSnapshot(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.snapshot");
        
        if (args.length >= 5 && args[1] != null && args[2] != null && args[3] != null && args[4] != null) {
            double x = toDouble(args[1], "canvas.snapshot", "x");
            double y = toDouble(args[2], "canvas.snapshot", "y");
            double width = toPositiveDouble(args[3], "canvas.snapshot", "width");
            double height = toPositiveDouble(args[4], "canvas.snapshot", "height");
            return canvas.snapshot(x, y, width, height);
        } else {
            return canvas.toImage();
        }
    }

    /**
     * Get canvas name.
     * canvas.getName(canvas) -> STRING
     */
    private static Object canvasGetName(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.getName");
        return canvas.getCanvasName();
    }

    /**
     * Set canvas name.
     * canvas.setName(canvas, name) -> CANVAS
     */
    private static Object canvasSetName(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "canvas.setName");
        String name = (String) args[1];
        canvas.setCanvasName(name);
        return canvas;
    }

    // ==================== Drawing Operations ====================

    /**
     * Draw a line.
     * draw.line(canvas, x1, y1, x2, y2) -> NULL
     */
    private static Object drawLine(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.line");
        double x1 = toDouble(args[1], "draw.line", "x1");
        double y1 = toDouble(args[2], "draw.line", "y1");
        double x2 = toDouble(args[3], "draw.line", "x2");
        double y2 = toDouble(args[4], "draw.line", "y2");
        
        canvas.drawLine(x1, y1, x2, y2);
        return null;
    }

    /**
     * Draw or fill a rectangle.
     * draw.rect(canvas, x, y, width, height, fill?) -> NULL
     */
    private static Object drawRect(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.rect");
        double x = toDouble(args[1], "draw.rect", "x");
        double y = toDouble(args[2], "draw.rect", "y");
        double width = toPositiveDouble(args[3], "draw.rect", "width");
        double height = toPositiveDouble(args[4], "draw.rect", "height");
        boolean fill = args.length > 5 && args[5] != null ? (Boolean) args[5] : false;
        
        if (fill) {
            canvas.fillRect(x, y, width, height);
        } else {
            canvas.drawRect(x, y, width, height);
        }
        return null;
    }

    /**
     * Draw or fill a circle.
     * draw.circle(canvas, x, y, radius, fill?) -> NULL
     */
    private static Object drawCircle(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.circle");
        double x = toDouble(args[1], "draw.circle", "x");
        double y = toDouble(args[2], "draw.circle", "y");
        double radius = toPositiveDouble(args[3], "draw.circle", "radius");
        boolean fill = args.length > 4 && args[4] != null ? (Boolean) args[4] : false;
        
        if (fill) {
            canvas.fillCircle(x, y, radius);
        } else {
            canvas.drawCircle(x, y, radius);
        }
        return null;
    }

    /**
     * Draw or fill an ellipse.
     * draw.ellipse(canvas, x, y, width, height, fill?) -> NULL
     */
    private static Object drawEllipse(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.ellipse");
        double x = toDouble(args[1], "draw.ellipse", "x");
        double y = toDouble(args[2], "draw.ellipse", "y");
        double width = toPositiveDouble(args[3], "draw.ellipse", "width");
        double height = toPositiveDouble(args[4], "draw.ellipse", "height");
        boolean fill = args.length > 5 && args[5] != null ? (Boolean) args[5] : false;
        
        if (fill) {
            canvas.fillEllipse(x, y, width, height);
        } else {
            canvas.drawEllipse(x, y, width, height);
        }
        return null;
    }

    /**
     * Draw or fill an arc.
     * draw.arc(canvas, x, y, radius, startAngle, length, fill?) -> NULL
     */
    private static Object drawArc(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.arc");
        double x = toDouble(args[1], "draw.arc", "x");
        double y = toDouble(args[2], "draw.arc", "y");
        double radius = toPositiveDouble(args[3], "draw.arc", "radius");
        double startAngle = toDouble(args[4], "draw.arc", "startAngle");
        double length = toDouble(args[5], "draw.arc", "length");
        boolean fill = args.length > 6 && args[6] != null ? (Boolean) args[6] : false;
        
        if (fill) {
            canvas.fillArc(x, y, radius, startAngle, length);
        } else {
            canvas.drawArc(x, y, radius, startAngle, length);
        }
        return null;
    }

    /**
     * Draw or fill a polygon.
     * draw.polygon(canvas, points, fill?) -> NULL
     * points should be array of [x1, y1, x2, y2, ...] coordinates
     */
    private static Object drawPolygon(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.polygon");
        
        // Get points array
        Object pointsObj = args[1];
        if (pointsObj == null) {
            throw new InterpreterError("draw.polygon: points cannot be null");
        }
        
        double[] coords = extractCoordinates(pointsObj, "draw.polygon");
        
        // Must have at least 3 points (6 coordinates)
        if (coords.length < 6 || coords.length % 2 != 0) {
            throw new InterpreterError("draw.polygon: points must contain at least 3 coordinate pairs");
        }
        
        // Split into x and y arrays
        int numPoints = coords.length / 2;
        double[] xPoints = new double[numPoints];
        double[] yPoints = new double[numPoints];
        for (int i = 0; i < numPoints; i++) {
            xPoints[i] = coords[i * 2];
            yPoints[i] = coords[i * 2 + 1];
        }
        
        boolean fill = args.length > 2 && args[2] != null ? (Boolean) args[2] : false;
        
        if (fill) {
            canvas.fillPolygon(xPoints, yPoints);
        } else {
            canvas.drawPolygon(xPoints, yPoints);
        }
        return null;
    }

    /**
     * Draw text on canvas.
     * draw.text(canvas, text, x, y) -> NULL
     */
    private static Object drawText(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.text");
        String text = (String) args[1];
        double x = toDouble(args[2], "draw.text", "x");
        double y = toDouble(args[3], "draw.text", "y");
        
        canvas.drawText(text, x, y);
        return null;
    }

    /**
     * Draw an image on canvas.
     * draw.image(canvas, image, x, y, width?, height?) -> NULL
     */
    private static Object drawImage(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "draw.image");
        
        if (!(args[1] instanceof EbsImage)) {
            throw new InterpreterError("draw.image: second argument must be an IMAGE");
        }
        EbsImage image = (EbsImage) args[1];
        
        double x = toDouble(args[2], "draw.image", "x");
        double y = toDouble(args[3], "draw.image", "y");
        
        if (args.length >= 6 && args[4] != null && args[5] != null) {
            double width = toPositiveDouble(args[4], "draw.image", "width");
            double height = toPositiveDouble(args[5], "draw.image", "height");
            canvas.drawImage(image, x, y, width, height);
        } else {
            canvas.drawImage(image, x, y);
        }
        return null;
    }

    // ==================== Style Operations ====================

    /**
     * Set stroke color and optionally line width.
     * style.setStroke(canvas, color, lineWidth?) -> NULL
     */
    private static Object styleSetStroke(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "style.setStroke");
        String color = (String) args[1];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("style.setStroke: color cannot be null or empty");
        }
        
        if (args.length > 2 && args[2] != null) {
            double lineWidth = toPositiveDouble(args[2], "style.setStroke", "lineWidth");
            canvas.setStroke(color, lineWidth);
        } else {
            canvas.setStroke(color);
        }
        return null;
    }

    /**
     * Set fill color.
     * style.setFill(canvas, color) -> NULL
     */
    private static Object styleSetFill(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "style.setFill");
        String color = (String) args[1];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("style.setFill: color cannot be null or empty");
        }
        
        canvas.setFill(color);
        return null;
    }

    /**
     * Set font.
     * style.setFont(canvas, fontName, fontSize) -> NULL
     */
    private static Object styleSetFont(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "style.setFont");
        String fontName = (String) args[1];
        double fontSize = toPositiveDouble(args[2], "style.setFont", "fontSize");
        
        if (fontName == null || fontName.isBlank()) {
            throw new InterpreterError("style.setFont: fontName cannot be null or empty");
        }
        
        canvas.setFont(fontName, fontSize);
        return null;
    }

    /**
     * Set line cap style.
     * style.setLineCap(canvas, cap) -> NULL
     * cap: "BUTT", "ROUND", or "SQUARE"
     */
    private static Object styleSetLineCap(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "style.setLineCap");
        String cap = (String) args[1];
        
        if (cap == null || cap.isBlank()) {
            throw new InterpreterError("style.setLineCap: cap cannot be null or empty");
        }
        
        canvas.setLineCap(cap);
        return null;
    }

    /**
     * Set line join style.
     * style.setLineJoin(canvas, join) -> NULL
     * join: "MITER", "BEVEL", or "ROUND"
     */
    private static Object styleSetLineJoin(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "style.setLineJoin");
        String join = (String) args[1];
        
        if (join == null || join.isBlank()) {
            throw new InterpreterError("style.setLineJoin: join cannot be null or empty");
        }
        
        canvas.setLineJoin(join);
        return null;
    }

    // ==================== Effect Operations ====================

    /**
     * Set drop shadow effect.
     * effect.setShadow(canvas, blur, offsetX, offsetY, color) -> NULL
     */
    private static Object effectSetShadow(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "effect.setShadow");
        double blur = toNonNegativeDouble(args[1], "effect.setShadow", "blur");
        double offsetX = toDouble(args[2], "effect.setShadow", "offsetX");
        double offsetY = toDouble(args[3], "effect.setShadow", "offsetY");
        String color = (String) args[4];
        
        if (color == null || color.isBlank()) {
            throw new InterpreterError("effect.setShadow: color cannot be null or empty");
        }
        
        canvas.setShadow(blur, offsetX, offsetY, color);
        return null;
    }

    /**
     * Clear shadow effect.
     * effect.clearShadow(canvas) -> NULL
     */
    private static Object effectClearShadow(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "effect.clearShadow");
        canvas.clearShadow();
        return null;
    }

    /**
     * Set global alpha (opacity).
     * effect.setGlobalAlpha(canvas, alpha) -> NULL
     * alpha: 0.0 (transparent) to 1.0 (opaque)
     */
    private static Object effectSetGlobalAlpha(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "effect.setGlobalAlpha");
        double alpha = toDouble(args[1], "effect.setGlobalAlpha", "alpha");
        
        if (alpha < 0.0 || alpha > 1.0) {
            throw new InterpreterError("effect.setGlobalAlpha: alpha must be between 0.0 and 1.0");
        }
        
        canvas.setGlobalAlpha(alpha);
        return null;
    }

    // ==================== Transform Operations ====================

    /**
     * Save graphics state.
     * transform.save(canvas) -> NULL
     */
    private static Object transformSave(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "transform.save");
        canvas.save();
        return null;
    }

    /**
     * Restore graphics state.
     * transform.restore(canvas) -> NULL
     */
    private static Object transformRestore(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "transform.restore");
        canvas.restore();
        return null;
    }

    /**
     * Translate coordinate system.
     * transform.translate(canvas, x, y) -> NULL
     */
    private static Object transformTranslate(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "transform.translate");
        double x = toDouble(args[1], "transform.translate", "x");
        double y = toDouble(args[2], "transform.translate", "y");
        
        canvas.translate(x, y);
        return null;
    }

    /**
     * Rotate coordinate system.
     * transform.rotate(canvas, degrees) -> NULL
     */
    private static Object transformRotate(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "transform.rotate");
        double degrees = toDouble(args[1], "transform.rotate", "degrees");
        
        canvas.rotate(degrees);
        return null;
    }

    /**
     * Scale coordinate system.
     * transform.scale(canvas, x, y) -> NULL
     */
    private static Object transformScale(Object[] args) throws InterpreterError {
        EbsCanvas canvas = getCanvas(args[0], "transform.scale");
        double x = toDouble(args[1], "transform.scale", "x");
        double y = toDouble(args[2], "transform.scale", "y");
        
        canvas.scale(x, y);
        return null;
    }

    // ==================== Helper Methods ====================

    /**
     * Extract EbsCanvas from argument.
     */
    private static EbsCanvas getCanvas(Object arg, String funcName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(funcName + ": canvas cannot be null");
        }
        if (!(arg instanceof EbsCanvas)) {
            throw new InterpreterError(funcName + ": expected CANVAS");
        }
        return (EbsCanvas) arg;
    }

    /**
     * Extract coordinates array from various array-like objects.
     */
    private static double[] extractCoordinates(Object obj, String funcName) throws InterpreterError {
        if (obj instanceof ArrayDef arrayDef) {
            // Handle EBS array types
            Object[] elements = (Object[]) arrayDef.getAll();
            double[] coords = new double[elements.length];
            for (int i = 0; i < elements.length; i++) {
                if (!(elements[i] instanceof Number)) {
                    throw new InterpreterError(funcName + ": all array elements must be numbers");
                }
                coords[i] = ((Number) elements[i]).doubleValue();
            }
            return coords;
        } else if (obj instanceof List<?> list) {
            // Handle List
            double[] coords = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object elem = list.get(i);
                if (!(elem instanceof Number)) {
                    throw new InterpreterError(funcName + ": all array elements must be numbers");
                }
                coords[i] = ((Number) elem).doubleValue();
            }
            return coords;
        } else if (obj.getClass().isArray()) {
            // Handle primitive or Object arrays
            if (obj instanceof double[] da) {
                return da;
            } else if (obj instanceof int[] ia) {
                double[] coords = new double[ia.length];
                for (int i = 0; i < ia.length; i++) {
                    coords[i] = ia[i];
                }
                return coords;
            } else if (obj instanceof float[] fa) {
                double[] coords = new double[fa.length];
                for (int i = 0; i < fa.length; i++) {
                    coords[i] = fa[i];
                }
                return coords;
            } else if (obj instanceof Object[] oa) {
                double[] coords = new double[oa.length];
                for (int i = 0; i < oa.length; i++) {
                    if (!(oa[i] instanceof Number)) {
                        throw new InterpreterError(funcName + ": all array elements must be numbers");
                    }
                    coords[i] = ((Number) oa[i]).doubleValue();
                }
                return coords;
            }
        }
        
        throw new InterpreterError(funcName + ": expected array of numbers");
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
     * Convert argument to positive double.
     */
    private static double toPositiveDouble(Object arg, String funcName, String paramName) throws InterpreterError {
        double value = toDouble(arg, funcName, paramName);
        if (value <= 0) {
            throw new InterpreterError(funcName + ": " + paramName + " must be positive");
        }
        return value;
    }

    /**
     * Convert argument to non-negative double.
     */
    private static double toNonNegativeDouble(Object arg, String funcName, String paramName) throws InterpreterError {
        double value = toDouble(arg, funcName, paramName);
        if (value < 0) {
            throw new InterpreterError(funcName + ": " + paramName + " must be non-negative");
        }
        return value;
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
