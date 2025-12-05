package com.eb.script.test;

import com.eb.script.arrays.ArrayFixedByte;
import com.eb.script.image.EbsImage;
import com.eb.script.interpreter.builtins.BuiltinsImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Test class for BuiltinsImage and EbsImage functionality.
 * Tests basic image operations using programmatically created test images.
 */
public class TestImageBuiltins {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println("       TEST: BuiltinsImage and EbsImage Functions");
        System.out.println("====================================================================");
        System.out.println();

        try {
            testEbsImageCreation();
            testImageInfoAndDimensions();
            testImageManipulations();
            testBase64Operations();
            testGetBytesConversion();
            
            System.out.println();
            System.out.println("====================================================================");
            System.out.println("                    TEST SUMMARY");
            System.out.println("====================================================================");
            System.out.println("Passed: " + passCount);
            System.out.println("Failed: " + failCount);
            if (failCount == 0) {
                System.out.println("All tests PASSED");
            } else {
                System.out.println("Some tests FAILED");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Test error: " + e.getMessage());
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

    private static void testEbsImageCreation() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 1: EbsImage Creation and Basic Properties");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(100, 100, "png");
        
        EbsImage image = new EbsImage(testImageBytes, "test.png");
        
        assertEquals(100, image.getWidth(), "Image width");
        assertEquals(100, image.getHeight(), "Image height");
        assertEquals("test.png", image.getImageName(), "Image name");
        assertEquals("png", image.getImageType(), "Image type");

        ArrayFixedByte arrayBytes = new ArrayFixedByte(testImageBytes);
        EbsImage image2 = new EbsImage(arrayBytes);
        assertEquals(100, image2.getWidth(), "Image width from ArrayFixedByte");
        assertEquals(100, image2.getHeight(), "Image height from ArrayFixedByte");

        System.out.println("EbsImage creation tests PASSED");
        System.out.println();
    }

    private static void testImageInfoAndDimensions() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 2: Image Info and Dimensions");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(200, 150, "png");
        EbsImage image = new EbsImage(testImageBytes, "info_test.png");

        Object width = BuiltinsImage.dispatch(null, "image.getwidth", new Object[]{image});
        Object height = BuiltinsImage.dispatch(null, "image.getheight", new Object[]{image});
        
        assertEquals(200, width, "Get width via builtin");
        assertEquals(150, height, "Get height via builtin");

        Object info = BuiltinsImage.dispatch(null, "image.getinfo", new Object[]{image});
        assertTrue(info instanceof Map, "getInfo returns a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> infoMap = (Map<String, Object>) info;
        assertEquals(200, infoMap.get("width"), "Info width");
        assertEquals(150, infoMap.get("height"), "Info height");
        assertEquals("png", infoMap.get("type"), "Info type");
        assertEquals("info_test.png", infoMap.get("name"), "Info name");

        System.out.println("Image info and dimensions tests PASSED");
        System.out.println();
    }

    private static void testImageManipulations() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 3: Image Manipulations");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(100, 100, "png");
        EbsImage image = new EbsImage(testImageBytes);

        System.out.println("--- Testing resize ---");
        Object resized = BuiltinsImage.dispatch(null, "image.resize", 
            new Object[]{image, 50, 50, Boolean.FALSE});
        assertTrue(resized instanceof EbsImage, "Resize returns EbsImage");
        
        EbsImage resizedImg = (EbsImage) resized;
        assertEquals(50, resizedImg.getWidth(), "Resized width");
        assertEquals(50, resizedImg.getHeight(), "Resized height");

        System.out.println("--- Testing resize with keepAspect ---");
        byte[] rectImageBytes = createTestImage(200, 100, "png");
        EbsImage rectImage = new EbsImage(rectImageBytes);
        Object aspectResized = BuiltinsImage.dispatch(null, "image.resize", 
            new Object[]{rectImage, 50, 50, Boolean.TRUE});
        EbsImage aspectImg = (EbsImage) aspectResized;
        assertEquals(50, aspectImg.getWidth(), "Aspect resize width");
        assertEquals(25, aspectImg.getHeight(), "Aspect resize height");

        System.out.println("--- Testing crop ---");
        Object cropped = BuiltinsImage.dispatch(null, "image.crop", 
            new Object[]{image, 10, 10, 30, 40});
        assertTrue(cropped instanceof EbsImage, "Crop returns EbsImage");
        EbsImage croppedImg = (EbsImage) cropped;
        assertEquals(30, croppedImg.getWidth(), "Cropped width");
        assertEquals(40, croppedImg.getHeight(), "Cropped height");

        System.out.println("--- Testing rotate ---");
        Object rotated = BuiltinsImage.dispatch(null, "image.rotate", 
            new Object[]{image, 90.0});
        assertTrue(rotated instanceof EbsImage, "Rotate returns EbsImage");

        System.out.println("--- Testing flipHorizontal ---");
        Object flippedH = BuiltinsImage.dispatch(null, "image.fliphorizontal", 
            new Object[]{image});
        assertTrue(flippedH instanceof EbsImage, "FlipHorizontal returns EbsImage");

        System.out.println("--- Testing flipVertical ---");
        Object flippedV = BuiltinsImage.dispatch(null, "image.flipvertical", 
            new Object[]{image});
        assertTrue(flippedV instanceof EbsImage, "FlipVertical returns EbsImage");

        System.out.println("--- Testing toGrayscale ---");
        Object grayscale = BuiltinsImage.dispatch(null, "image.tograyscale", 
            new Object[]{image});
        assertTrue(grayscale instanceof EbsImage, "ToGrayscale returns EbsImage");

        System.out.println("--- Testing adjustBrightness ---");
        Object brighter = BuiltinsImage.dispatch(null, "image.adjustbrightness", 
            new Object[]{image, 1.5f});
        assertTrue(brighter instanceof EbsImage, "AdjustBrightness returns EbsImage");

        System.out.println("--- Testing adjustContrast ---");
        Object contrasted = BuiltinsImage.dispatch(null, "image.adjustcontrast", 
            new Object[]{image, 1.2f});
        assertTrue(contrasted instanceof EbsImage, "AdjustContrast returns EbsImage");

        System.out.println("Image manipulation tests PASSED");
        System.out.println();
    }

    private static void testBase64Operations() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 4: Base64 Operations");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(50, 50, "png");
        EbsImage image = new EbsImage(testImageBytes);

        System.out.println("--- Testing toBase64 ---");
        Object base64 = BuiltinsImage.dispatch(null, "image.tobase64", 
            new Object[]{image, "png"});
        assertTrue(base64 instanceof String, "ToBase64 returns String");
        String b64String = (String) base64;
        assertTrue(b64String.length() > 0, "Base64 string is not empty");

        System.out.println("--- Testing fromBase64 ---");
        Object decoded = BuiltinsImage.dispatch(null, "image.frombase64", 
            new Object[]{b64String});
        assertTrue(decoded instanceof EbsImage, "FromBase64 returns EbsImage");

        EbsImage decodedImg = (EbsImage) decoded;
        assertEquals(50, decodedImg.getWidth(), "Decoded image width");
        assertEquals(50, decodedImg.getHeight(), "Decoded image height");

        System.out.println("--- Testing fromBase64 with data URI ---");
        String dataUri = "data:image/png;base64," + b64String;
        Object decodedUri = BuiltinsImage.dispatch(null, "image.frombase64", 
            new Object[]{dataUri});
        assertTrue(decodedUri instanceof EbsImage, "FromBase64 with data URI returns EbsImage");

        System.out.println("Base64 operation tests PASSED");
        System.out.println();
    }

    private static void testGetBytesConversion() throws Exception {
        System.out.println("============================================================");
        System.out.println("TEST 5: GetBytes Conversion");
        System.out.println("============================================================");

        byte[] testImageBytes = createTestImage(80, 60, "png");
        EbsImage image = new EbsImage(testImageBytes, "conversion_test.png");

        System.out.println("--- Testing getBytes ---");
        Object bytesResult = BuiltinsImage.dispatch(null, "image.getbytes", 
            new Object[]{image, "png"});
        assertTrue(bytesResult instanceof ArrayFixedByte, "getBytes returns ArrayFixedByte");
        
        ArrayFixedByte afb = (ArrayFixedByte) bytesResult;
        assertTrue(afb.size() > 0, "ArrayFixedByte has data");

        EbsImage recreated = new EbsImage(afb);
        assertEquals(80, recreated.getWidth(), "Recreated image width");
        assertEquals(60, recreated.getHeight(), "Recreated image height");

        System.out.println("--- Testing getName/setName ---");
        Object name = BuiltinsImage.dispatch(null, "image.getname", new Object[]{image});
        assertEquals("conversion_test.png", name, "getName returns correct name");

        Object renamed = BuiltinsImage.dispatch(null, "image.setname", 
            new Object[]{image, "new_name.png"});
        assertTrue(renamed instanceof EbsImage, "setName returns EbsImage");
        assertEquals("new_name.png", ((EbsImage)renamed).getImageName(), "setName updates name");

        System.out.println("--- Testing getType/setType ---");
        Object type = BuiltinsImage.dispatch(null, "image.gettype", new Object[]{image});
        assertEquals("png", type, "getType returns correct type");

        Object retyped = BuiltinsImage.dispatch(null, "image.settype", 
            new Object[]{image, "jpg"});
        assertTrue(retyped instanceof EbsImage, "setType returns EbsImage");
        assertEquals("jpg", ((EbsImage)retyped).getImageType(), "setType updates type");

        System.out.println("GetBytes conversion tests PASSED");
        System.out.println();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            passCount++;
            System.out.println("  PASS: " + message);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            passCount++;
            System.out.println("  PASS: " + message);
        } else {
            failCount++;
            System.out.println("  FAIL: " + message + " - Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            passCount++;
            System.out.println("  PASS: " + message);
        } else {
            failCount++;
            System.out.println("  FAIL: " + message + " - Expected true");
        }
    }
}
