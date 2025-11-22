# UtilImage Test Scripts

This directory contains test utilities for the `UtilImage` class.

## Test Scripts

### ImageTestGenerator

Generates test images of various sizes for testing the resize functionality.

**Usage:**
```bash
cd ScriptInterpreter
mvn compile
java -cp target/classes com.eb.util.ImageTestGenerator [output-directory]
```

**Default output directory:** `test-images`

**Generated images:**
- 10 PNG images ranging from 800x600 to 4000x3000 pixels
- Each image has a unique color and displays its dimensions
- Total size: approximately 344 KB

### ImageResizeTest

Tests the `UtilImage.resizeImage()` method by reading test images and resizing them to a maximum dimension of 2000 pixels.

**Usage:**
```bash
cd ScriptInterpreter
mvn compile
java -cp target/classes com.eb.util.ImageResizeTest [input-directory] [output-directory]
```

**Default directories:**
- Input: `test-images`
- Output: `test-images-resized`

**Output:**
- Detailed statistics for each image (dimensions, file size, processing time)
- Aspect ratio verification
- Overall summary with total space saved

## Quick Start

To generate test images and run the resize test:

```bash
cd ScriptInterpreter

# Compile
mvn clean compile

# Generate test images
java -cp target/classes com.eb.util.ImageTestGenerator test-images

# Run resize test (max length = 2000)
java -cp target/classes com.eb.util.ImageResizeTest test-images test-images-resized
```

## Expected Results

When resizing with max length 2000:
- Images with dimensions ≤ 2000px: No resize needed (0% size reduction)
- Images with dimensions > 2000px: Resized to fit within 2000px (9-71% size reduction)
- All aspect ratios maintained
- Overall space savings: approximately 38%

## Notes

- Test images are excluded from git via `.gitignore`
- Both input and output directories are created automatically if they don't exist
- The resize test preserves the original image format (PNG)
