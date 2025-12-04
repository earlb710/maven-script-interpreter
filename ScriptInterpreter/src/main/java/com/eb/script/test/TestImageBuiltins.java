package com.eb.script.test;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.builtins.BuiltinsImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Test class for BuiltinsImage functionality.
 * Tests basic image operations using programmatically created test images.
 */
public class TestImageBuiltins {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println("       TEST: BuiltinsImage Functions");
        System.out.println("====================================================================");
        System.out.println();

        try {
            testByteConversions();
            testImageInfoAndDimensions();
            testImageManipulations();
            testBase64Operations();
            
            System.out.println();
            System.out.println("====================================================================");
            System.out.println("                    TEST SUMMARY");
            System.out.println("====================================================================");
            System.out.println("Passed: " + passCount);
            System.out.println("Failed: " + failCount);
            if (failCount == 0) {
                System.out.println("✓ All tests PASSED");
            } else {
                System.out.println("✗ Some tests FAILED");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("✗ Test error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static byte[] createTestImage(int width, int height, String format) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, width / 2, height / 2);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(width / 2, 0, width / 2, height / 2);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, height / 2, width / 2, height / 2);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(width / 2, height / 2, width / 2, height / 2);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    private static void testByteConversions() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 1: Byte Array Handling");
        System.out.println("============================================================");

        // Create a 100x100 test image
        byte[] testImageBytes = createTestImage(100, 100, "png");
        ArrayFixedByte imageArray = new ArrayFixedByte(testImageBytes);

        // Test that we can get width and height
        Object width = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{imageArray});
        assertEquals(100, width, "Get width from ArrayFixedByte");

        Object height = BuiltinsImage.dispatch(null, "image.getheight", new Object[]{imageArray});
        assertEquals(100, height, "Get height from ArrayFixedByte");

        System.out.println("✓ Byte array handling tests PASSED");
        System.out.println();
    }

    private static void testImageInfoAndDimensions() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 2: Image Info and Dimensions");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(200, 150, "png");
        ArrayFixedByte imageArray = new ArrayFixedByte(testImageBytes);

        // Test getInfo
        Object info = BuiltinsImage.dispatch(null, "image.getinfo", new Object[]{imageArray});
        assertTrue(info instanceof Map, "getInfo returns a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> infoMap = (Map<String, Object>) info;
        assertEquals(200, infoMap.get("width"), "Info width");
        assertEquals(150, infoMap.get("height"), "Info height");
        assertEquals("png", infoMap.get("format"), "Info format");
        assertTrue(infoMap.containsKey("sizeBytes"), "Info contains sizeBytes");

        System.out.println("✓ Image info and dimensions tests PASSED");
        System.out.println();
    }

    private static void testImageManipulations() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 3: Image Manipulations");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(100, 100, "png");
        ArrayFixedByte imageArray = new ArrayFixedByte(testImageBytes);

        // Test resize
        System.out.println("--- Testing resize ---");
        Object resized = BuiltinsImage.dispatch(null, "image.resize", 
            new Object[]{imageArray, 50, 50, Boolean.FALSE});
        assertTrue(resized instanceof ArrayFixedByte, "Resize returns ArrayFixedByte");
        
        Object resizedWidth = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{resized});
        assertEquals(50, resizedWidth, "Resized width");

        // Test resize with keep aspect
        System.out.println("--- Testing resize with keepAspect ---");
        byte[] rectImageBytes = createTestImage(200, 100, "png");
        ArrayFixedByte rectImage = new ArrayFixedByte(rectImageBytes);
        Object aspectResized = BuiltinsImage.dispatch(null, "image.resize", 
            new Object[]{rectImage, 50, 50, Boolean.TRUE});
        Object aspectWidth = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{aspectResized});
        Object aspectHeight = BuiltinsImage.dispatch(null, "image.getheight", new Object[]{aspectResized});
        assertEquals(50, aspectWidth, "Aspect resize width");
        assertEquals(25, aspectHeight, "Aspect resize height (should maintain 2:1 ratio)");

        // Test crop
        System.out.println("--- Testing crop ---");
        Object cropped = BuiltinsImage.dispatch(null, "image.crop", 
            new Object[]{imageArray, 10, 10, 30, 40});
        Object croppedWidth = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{cropped});
        Object croppedHeight = BuiltinsImage.dispatch(null, "image.getheight", new Object[]{cropped});
        assertEquals(30, croppedWidth, "Cropped width");
        assertEquals(40, croppedHeight, "Cropped height");

        // Test rotate
        System.out.println("--- Testing rotate ---");
        Object rotated = BuiltinsImage.dispatch(null, "image.rotate", 
            new Object[]{imageArray, 90.0});
        assertTrue(rotated instanceof ArrayFixedByte, "Rotate returns ArrayFixedByte");

        // Test flip horizontal
        System.out.println("--- Testing flipHorizontal ---");
        Object flippedH = BuiltinsImage.dispatch(null, "image.fliphorizontal", 
            new Object[]{imageArray});
        assertTrue(flippedH instanceof ArrayFixedByte, "FlipHorizontal returns ArrayFixedByte");

        // Test flip vertical
        System.out.println("--- Testing flipVertical ---");
        Object flippedV = BuiltinsImage.dispatch(null, "image.flipvertical", 
            new Object[]{imageArray});
        assertTrue(flippedV instanceof ArrayFixedByte, "FlipVertical returns ArrayFixedByte");

        // Test grayscale
        System.out.println("--- Testing toGrayscale ---");
        Object grayscale = BuiltinsImage.dispatch(null, "image.tograyscale", 
            new Object[]{imageArray});
        assertTrue(grayscale instanceof ArrayFixedByte, "ToGrayscale returns ArrayFixedByte");

        // Test brightness
        System.out.println("--- Testing adjustBrightness ---");
        Object brighter = BuiltinsImage.dispatch(null, "image.adjustbrightness", 
            new Object[]{imageArray, 1.5f});
        assertTrue(brighter instanceof ArrayFixedByte, "AdjustBrightness returns ArrayFixedByte");

        // Test contrast
        System.out.println("--- Testing adjustContrast ---");
        Object contrasted = BuiltinsImage.dispatch(null, "image.adjustcontrast", 
            new Object[]{imageArray, 1.2f});
        assertTrue(contrasted instanceof ArrayFixedByte, "AdjustContrast returns ArrayFixedByte");

        System.out.println("✓ Image manipulation tests PASSED");
        System.out.println();
    }

    private static void testBase64Operations() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 4: Base64 Operations");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(50, 50, "png");
        ArrayFixedByte imageArray = new ArrayFixedByte(testImageBytes);

        // Test toBase64
        System.out.println("--- Testing toBase64 ---");
        Object base64 = BuiltinsImage.dispatch(null, "image.tobase64", 
            new Object[]{imageArray, "png"});
        assertTrue(base64 instanceof String, "ToBase64 returns String");
        String b64String = (String) base64;
        assertTrue(b64String.length() > 0, "Base64 string is not empty");

        // Test fromBase64
        System.out.println("--- Testing fromBase64 ---");
        Object decoded = BuiltinsImage.dispatch(null, "image.frombase64", 
            new Object[]{b64String});
        assertTrue(decoded instanceof ArrayFixedByte, "FromBase64 returns ArrayFixedByte");

        // Verify decoded image has same dimensions
        Object decodedWidth = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{decoded});
        Object decodedHeight = BuiltinsImage.dispatch(null, "image.getheight", new Object[]{decoded});
        assertEquals(50, decodedWidth, "Decoded image width");
        assertEquals(50, decodedHeight, "Decoded image height");

        // Test fromBase64 with data URI prefix
        System.out.println("--- Testing fromBase64 with data URI ---");
        String dataUri = "data:image/png;base64," + b64String;
        Object decodedUri = BuiltinsImage.dispatch(null, "image.frombase64", 
            new Object[]{dataUri});
        assertTrue(decodedUri instanceof ArrayFixedByte, "FromBase64 with data URI returns ArrayFixedByte");

        System.out.println("✓ Base64 operation tests PASSED");
        System.out.println();
    }

    // Assertion helpers
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            passCount++;
            System.out.println("  ✓ " + message);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            passCount++;
            System.out.println("  ✓ " + message);
        } else {
            failCount++;
            System.out.println("  ✗ " + message + " - Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            passCount++;
            System.out.println("  ✓ " + message);
        } else {
            failCount++;
            System.out.println("  ✗ " + message + " - Expected true");
        }
    }
}
