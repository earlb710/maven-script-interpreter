package com.eb.script.image;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.InterpreterError;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.effect.DropShadow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Canvas data type for EBS scripting language.
 * Wraps a JavaFX Canvas with GraphicsContext for drawing operations.
 * Provides methods for drawing shapes, text, images, and applying styles and effects.
 * Supports transformation state management (save/restore).
 * 
 * @author Earl Bosch
 */
public class EbsCanvas {

    /** The JavaFX canvas */
    private final Canvas canvas;
    
    /** The graphics context for drawing */
    private final GraphicsContext gc;
    
    /** Canvas name (optional) */
    private String canvasName;
    
    /** Stack for save/restore transformation states */
    private final Stack<GraphicsState> stateStack;
    
    /** Flag to track if JavaFX toolkit has been initialized */
    private static volatile boolean javafxInitialized = false;
    
    /**
     * Initialize JavaFX toolkit if not already initialized.
     * This is required for headless operation.
     */
    private static void ensureJavaFXInitialized() {
        if (!javafxInitialized) {
            synchronized (EbsCanvas.class) {
                if (!javafxInitialized) {
                    try {
                        // Initialize JavaFX toolkit
                        Platform.startup(() -> {});
                        javafxInitialized = true;
                    } catch (IllegalStateException e) {
                        // Toolkit already initialized
                        javafxInitialized = true;
                    }
                }
            }
        }
    }
    
    /**
     * Internal class to store graphics state for save/restore operations.
     */
    private static class GraphicsState {
        Color fill;
        Color stroke;
        double lineWidth;
        Font font;
        StrokeLineCap lineCap;
        StrokeLineJoin lineJoin;
        double globalAlpha;
        
        GraphicsState(GraphicsContext gc) {
            this.fill = (Color) gc.getFill();
            this.stroke = (Color) gc.getStroke();
            this.lineWidth = gc.getLineWidth();
            this.font = gc.getFont();
            this.lineCap = gc.getLineCap();
            this.lineJoin = gc.getLineJoin();
            this.globalAlpha = gc.getGlobalAlpha();
        }
        
        void restore(GraphicsContext gc) {
            gc.setFill(fill);
            gc.setStroke(stroke);
            gc.setLineWidth(lineWidth);
            gc.setFont(font);
            gc.setLineCap(lineCap);
            gc.setLineJoin(lineJoin);
            gc.setGlobalAlpha(globalAlpha);
        }
    }
    
    /**
     * Create a new EbsCanvas with specified dimensions.
     * 
     * @param width Canvas width in pixels
     * @param height Canvas height in pixels
     */
    public EbsCanvas(int width, int height) {
        this(width, height, null);
    }
    
    /**
     * Create a new EbsCanvas with specified dimensions and name.
     * 
     * @param width Canvas width in pixels
     * @param height Canvas height in pixels
     * @param name Canvas name (optional)
     */
    public EbsCanvas(int width, int height, String name) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Canvas dimensions must be positive");
        }
        
        // Ensure JavaFX is initialized
        ensureJavaFXInitialized();
        
        this.canvas = new Canvas(width, height);
        this.gc = canvas.getGraphicsContext2D();
        this.canvasName = name;
        this.stateStack = new Stack<>();
        
        // Set default properties
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setFont(Font.font("System", 12));
    }
    
    /**
     * Get the underlying JavaFX Canvas.
     */
    public Canvas getCanvas() {
        return canvas;
    }
    
    /**
     * Get the GraphicsContext for direct manipulation.
     */
    public GraphicsContext getGraphicsContext() {
        return gc;
    }
    
    /**
     * Get canvas width.
     */
    public int getWidth() {
        return (int) canvas.getWidth();
    }
    
    /**
     * Get canvas height.
     */
    public int getHeight() {
        return (int) canvas.getHeight();
    }
    
    /**
     * Get canvas name.
     */
    public String getCanvasName() {
        return canvasName;
    }
    
    /**
     * Set canvas name.
     */
    public void setCanvasName(String name) {
        this.canvasName = name;
    }
    
    // ==================== Drawing Methods ====================
    
    /**
     * Clear the entire canvas.
     */
    public void clear() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    
    /**
     * Draw a line from (x1, y1) to (x2, y2).
     */
    public void drawLine(double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
    }
    
    /**
     * Draw a rectangle outline at (x, y) with given width and height.
     */
    public void drawRect(double x, double y, double width, double height) {
        gc.strokeRect(x, y, width, height);
    }
    
    /**
     * Fill a rectangle at (x, y) with given width and height.
     */
    public void fillRect(double x, double y, double width, double height) {
        gc.fillRect(x, y, width, height);
    }
    
    /**
     * Draw a circle outline centered at (x, y) with given radius.
     */
    public void drawCircle(double x, double y, double radius) {
        double diameter = radius * 2;
        gc.strokeOval(x - radius, y - radius, diameter, diameter);
    }
    
    /**
     * Fill a circle centered at (x, y) with given radius.
     */
    public void fillCircle(double x, double y, double radius) {
        double diameter = radius * 2;
        gc.fillOval(x - radius, y - radius, diameter, diameter);
    }
    
    /**
     * Draw an ellipse outline at (x, y) with given width and height.
     */
    public void drawEllipse(double x, double y, double width, double height) {
        gc.strokeOval(x, y, width, height);
    }
    
    /**
     * Fill an ellipse at (x, y) with given width and height.
     */
    public void fillEllipse(double x, double y, double width, double height) {
        gc.fillOval(x, y, width, height);
    }
    
    /**
     * Draw an arc outline centered at (x, y) with given radius, start angle, and arc length.
     * Angles are in degrees, with 0 degrees at 3 o'clock position.
     */
    public void drawArc(double x, double y, double radius, double startAngle, double arcLength) {
        double diameter = radius * 2;
        gc.strokeArc(x - radius, y - radius, diameter, diameter, startAngle, arcLength, ArcType.OPEN);
    }
    
    /**
     * Fill an arc centered at (x, y) with given radius, start angle, and arc length.
     * Angles are in degrees, with 0 degrees at 3 o'clock position.
     */
    public void fillArc(double x, double y, double radius, double startAngle, double arcLength) {
        double diameter = radius * 2;
        gc.fillArc(x - radius, y - radius, diameter, diameter, startAngle, arcLength, ArcType.ROUND);
    }
    
    /**
     * Draw a polygon outline with given points.
     * Points should be an array of alternating x,y coordinates: [x1, y1, x2, y2, ...]
     */
    public void drawPolygon(double[] xPoints, double[] yPoints) {
        if (xPoints.length != yPoints.length) {
            throw new IllegalArgumentException("xPoints and yPoints arrays must have same length");
        }
        gc.strokePolygon(xPoints, yPoints, xPoints.length);
    }
    
    /**
     * Fill a polygon with given points.
     * Points should be an array of alternating x,y coordinates: [x1, y1, x2, y2, ...]
     */
    public void fillPolygon(double[] xPoints, double[] yPoints) {
        if (xPoints.length != yPoints.length) {
            throw new IllegalArgumentException("xPoints and yPoints arrays must have same length");
        }
        gc.fillPolygon(xPoints, yPoints, xPoints.length);
    }
    
    /**
     * Draw text at (x, y).
     */
    public void drawText(String text, double x, double y) {
        if (text != null) {
            gc.fillText(text, x, y);
        }
    }
    
    /**
     * Draw an image at (x, y) with original size.
     */
    public void drawImage(EbsImage image, double x, double y) {
        if (image != null) {
            Image fxImage = image.getFxImage();
            gc.drawImage(fxImage, x, y);
        }
    }
    
    /**
     * Draw an image at (x, y) with specified width and height.
     */
    public void drawImage(EbsImage image, double x, double y, double width, double height) {
        if (image != null) {
            Image fxImage = image.getFxImage();
            gc.drawImage(fxImage, x, y, width, height);
        }
    }
    
    // ==================== Style Methods ====================
    
    /**
     * Set stroke color from hex string (e.g., "#FF0000" for red).
     */
    public void setStroke(String colorHex) {
        Color color = parseColor(colorHex);
        gc.setStroke(color);
    }
    
    /**
     * Set stroke color and line width.
     */
    public void setStroke(String colorHex, double lineWidth) {
        setStroke(colorHex);
        gc.setLineWidth(lineWidth);
    }
    
    /**
     * Set fill color from hex string (e.g., "#FF0000" for red).
     */
    public void setFill(String colorHex) {
        Color color = parseColor(colorHex);
        gc.setFill(color);
    }
    
    /**
     * Set font with name and size.
     */
    public void setFont(String fontName, double fontSize) {
        gc.setFont(Font.font(fontName, fontSize));
    }
    
    /**
     * Set line cap style: "BUTT", "ROUND", or "SQUARE".
     */
    public void setLineCap(String cap) {
        StrokeLineCap lineCap = switch (cap.toUpperCase()) {
            case "BUTT" -> StrokeLineCap.BUTT;
            case "ROUND" -> StrokeLineCap.ROUND;
            case "SQUARE" -> StrokeLineCap.SQUARE;
            default -> throw new IllegalArgumentException("Invalid line cap: " + cap);
        };
        gc.setLineCap(lineCap);
    }
    
    /**
     * Set line join style: "MITER", "BEVEL", or "ROUND".
     */
    public void setLineJoin(String join) {
        StrokeLineJoin lineJoin = switch (join.toUpperCase()) {
            case "MITER" -> StrokeLineJoin.MITER;
            case "BEVEL" -> StrokeLineJoin.BEVEL;
            case "ROUND" -> StrokeLineJoin.ROUND;
            default -> throw new IllegalArgumentException("Invalid line join: " + join);
        };
        gc.setLineJoin(lineJoin);
    }
    
    /**
     * Set global alpha (opacity) value between 0.0 (transparent) and 1.0 (opaque).
     */
    public void setGlobalAlpha(double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Alpha must be between 0.0 and 1.0");
        }
        gc.setGlobalAlpha(alpha);
    }
    
    // ==================== Effect Methods ====================
    
    /**
     * Set drop shadow effect with blur radius, offset, and color.
     */
    public void setShadow(double blur, double offsetX, double offsetY, String colorHex) {
        Color color = parseColor(colorHex);
        DropShadow shadow = new DropShadow();
        shadow.setRadius(blur);
        shadow.setOffsetX(offsetX);
        shadow.setOffsetY(offsetY);
        shadow.setColor(color);
        gc.setEffect(shadow);
    }
    
    /**
     * Clear any applied effects.
     */
    public void clearShadow() {
        gc.setEffect(null);
    }
    
    // ==================== Transformation Methods ====================
    
    /**
     * Save the current graphics state (transform, stroke, fill, etc.).
     */
    public void save() {
        gc.save();
        stateStack.push(new GraphicsState(gc));
    }
    
    /**
     * Restore the previously saved graphics state.
     */
    public void restore() {
        if (!stateStack.isEmpty()) {
            gc.restore();
            GraphicsState state = stateStack.pop();
            state.restore(gc);
        }
    }
    
    /**
     * Translate the coordinate system by (x, y).
     */
    public void translate(double x, double y) {
        gc.translate(x, y);
    }
    
    /**
     * Rotate the coordinate system by the given angle in degrees.
     */
    public void rotate(double degrees) {
        gc.rotate(degrees);
    }
    
    /**
     * Scale the coordinate system by (x, y) factors.
     */
    public void scale(double x, double y) {
        gc.scale(x, y);
    }
    
    // ==================== Export Methods ====================
    
    /**
     * Convert canvas to EbsImage.
     */
    public EbsImage toImage() throws InterpreterError {
        WritableImage snapshot;
        
        // Check if we're already on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            // We're on FX thread - run snapshot directly to avoid deadlock
            try {
                // Use SnapshotParameters with transparent fill to preserve alpha channel
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                snapshot = canvas.snapshot(params, null);
            } catch (Exception e) {
                throw new InterpreterError("Failed to take canvas snapshot: " + e.getMessage());
            }
        } else {
            // We're NOT on FX thread - use Platform.runLater with latch
            AtomicReference<WritableImage> snapshotRef = new AtomicReference<>();
            AtomicReference<Exception> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            // Run snapshot on FX thread
            Platform.runLater(() -> {
                try {
                    // Use SnapshotParameters with transparent fill to preserve alpha channel
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    WritableImage snap = canvas.snapshot(params, null);
                    snapshotRef.set(snap);
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
            
            // Wait for completion
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterpreterError("Interrupted while taking canvas snapshot");
            }
            
            if (errorRef.get() != null) {
                throw new InterpreterError("Failed to take canvas snapshot: " + errorRef.get().getMessage());
            }
            
            snapshot = snapshotRef.get();
        }
        try {
            // Create BufferedImage with alpha channel for transparency support
            BufferedImage buffered = new BufferedImage(
                (int) snapshot.getWidth(), 
                (int) snapshot.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            SwingFXUtils.fromFXImage(snapshot, buffered);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            byte[] bytes = baos.toByteArray();
            
            String name = canvasName != null ? canvasName + ".png" : "canvas.png";
            return new EbsImage(bytes, name, "png");
        } catch (IOException e) {
            throw new InterpreterError("Failed to convert canvas to image: " + e.getMessage());
        }
    }
    
    /**
     * Take a snapshot of a portion of the canvas as EbsImage.
     */
    public EbsImage snapshot(double x, double y, double width, double height) throws InterpreterError {
        WritableImage snapshot;
        
        // Check if we're already on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            // We're on FX thread - run snapshot directly to avoid deadlock
            try {
                // Use SnapshotParameters with transparent fill to preserve alpha channel
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                params.setViewport(new javafx.geometry.Rectangle2D(x, y, width, height));
                snapshot = canvas.snapshot(params, null);
            } catch (Exception e) {
                throw new InterpreterError("Failed to take canvas snapshot: " + e.getMessage());
            }
        } else {
            // We're NOT on FX thread - use Platform.runLater with latch
            AtomicReference<WritableImage> snapshotRef = new AtomicReference<>();
            AtomicReference<Exception> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            // Run snapshot on FX thread
            Platform.runLater(() -> {
                try {
                    // Use SnapshotParameters with transparent fill to preserve alpha channel
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    params.setViewport(new javafx.geometry.Rectangle2D(x, y, width, height));
                    WritableImage snap = canvas.snapshot(params, null);
                    snapshotRef.set(snap);
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
            
            // Wait for completion
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterpreterError("Interrupted while taking canvas snapshot");
            }
            
            if (errorRef.get() != null) {
                throw new InterpreterError("Failed to take canvas snapshot: " + errorRef.get().getMessage());
            }
            
            snapshot = snapshotRef.get();
        }
        try {
            // Create BufferedImage with alpha channel for transparency support
            BufferedImage buffered = new BufferedImage(
                (int) snapshot.getWidth(), 
                (int) snapshot.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            SwingFXUtils.fromFXImage(snapshot, buffered);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            byte[] bytes = baos.toByteArray();
            
            String name = canvasName != null ? canvasName + "_snapshot.png" : "canvas_snapshot.png";
            return new EbsImage(bytes, name, "png");
        } catch (IOException e) {
            throw new InterpreterError("Failed to take canvas snapshot: " + e.getMessage());
        }
    }
    
    /**
     * Save canvas to file as image.
     */
    public ArrayFixedByte getBytes(String format) throws InterpreterError {
        if (format == null || format.isBlank()) {
            format = "png";
        }
        
        final String finalFormat = format;
        WritableImage snapshot;
        
        // Check if we're already on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            // We're on FX thread - run snapshot directly to avoid deadlock
            try {
                // Use SnapshotParameters with transparent fill to preserve alpha channel
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                snapshot = canvas.snapshot(params, null);
            } catch (Exception e) {
                throw new InterpreterError("Failed to take canvas snapshot: " + e.getMessage());
            }
        } else {
            // We're NOT on FX thread - use Platform.runLater with latch
            AtomicReference<WritableImage> snapshotRef = new AtomicReference<>();
            AtomicReference<Exception> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            // Run snapshot on FX thread
            Platform.runLater(() -> {
                try {
                    // Use SnapshotParameters with transparent fill to preserve alpha channel
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    WritableImage snap = canvas.snapshot(params, null);
                    snapshotRef.set(snap);
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
            
            // Wait for completion
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterpreterError("Interrupted while taking canvas snapshot");
            }
            
            if (errorRef.get() != null) {
                throw new InterpreterError("Failed to take canvas snapshot: " + errorRef.get().getMessage());
            }
            
            snapshot = snapshotRef.get();
        }
        try {
            // Create BufferedImage with alpha channel for transparency support
            BufferedImage buffered = new BufferedImage(
                (int) snapshot.getWidth(), 
                (int) snapshot.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            SwingFXUtils.fromFXImage(snapshot, buffered);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, finalFormat.toLowerCase(), baos);
            return new ArrayFixedByte(baos.toByteArray());
        } catch (IOException e) {
            throw new InterpreterError("Failed to convert canvas to bytes: " + e.getMessage());
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Parse color from hex string or color name.
     * Supports formats: "#RGB", "#RRGGBB", "#RRGGBBAA", and named colors.
     */
    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isBlank()) {
            return Color.BLACK;
        }
        
        try {
            // Try web color parsing (handles #RGB, #RRGGBB, #RRGGBBAA, and named colors)
            return Color.web(colorStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color format: " + colorStr);
        }
    }
    
    @Override
    public String toString() {
        return "EbsCanvas[" + (int)canvas.getWidth() + "x" + (int)canvas.getHeight() + 
               (canvasName != null ? ", name=" + canvasName : "") + "]";
    }
}
