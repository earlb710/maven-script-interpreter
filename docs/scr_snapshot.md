# scr.snapshot Builtin Function

## Overview
The `scr.snapshot` builtin function captures a screenshot of a JavaFX screen and stores it as an `EbsImage` object.

## Keyboard Shortcut
You can also capture screenshots using **Ctrl+P** while a screen is active. This saves the screenshot to the system temp directory with format `{screenName}_{sequence}.png` and an auto-incrementing sequence number.

Example: If you press Ctrl+P three times on a screen named "myScreen", it will create:
- `{TEMP_DIR}/myScreen_001.png`
- `{TEMP_DIR}/myScreen_002.png`
- `{TEMP_DIR}/myScreen_003.png`

Where `{TEMP_DIR}` is the system temporary directory (e.g., `/tmp/` on Linux/Mac, `C:\Users\{user}\AppData\Local\Temp\` on Windows).

## Syntax
```ebs
image ebsImage = call scr.snapshot(screenName?)
```

## Parameters
- `screenName` (optional, string): The name of the screen to capture
  - If omitted, uses the current screen context (must be called from within screen event handlers)
  - If provided, captures the specified screen

## Return Value
Returns an `IMAGE` (EbsImage) object containing the captured screenshot in PNG format.

## Features
- Uses JavaFX's Scene.snapshot() method for high-quality screen capture
- Thread-safe implementation with proper synchronization
- Comprehensive error handling
- Default 10-second timeout for complex screens
- Automatic naming: `{screenName}_screenshot`

## Examples

### Example 1: Capture from within screen context
```ebs
screen myScreen = {
    "title": "My Screen",
    "width": 500,
    "height": 400,
    "vars": [
        {
            "name": "captureBtn",
            "type": "string",
            "default": "Capture",
            "display": {
                "type": "button",
                "onClick": "captureScreenshot"
            }
        }
    ]
};

show screen myScreen;

captureScreenshot() return void {
    // Capture current screen (myScreen)
    image screenshot = call scr.snapshot();
    
    // Save to file
    call image.save(screenshot, "my_screenshot.png");
    print "Screenshot saved!";
}
```

### Example 2: Capture by screen name
```ebs
show screen testScreen;

// Capture a specific screen by name
image screenshot = call scr.snapshot("testScreen");

// Get image information
int width = call image.getWidth(screenshot);
int height = call image.getHeight(screenshot);
print "Captured " + width + "x" + height + " screenshot";

// Save to file
call image.save(screenshot, "testScreen_capture.png");
```

## Working with Captured Screenshots

Once captured, you can use all standard image builtins:

```ebs
image screenshot = call scr.snapshot("myScreen");

// Get image properties
int width = call image.getWidth(screenshot);
int height = call image.getHeight(screenshot);
string name = call image.getName(screenshot);
string type = call image.getType(screenshot);

// Save to file
call image.save(screenshot, "output.png", "png");

// Resize
image resized = call image.resize(screenshot, 800, 600, true);

// Convert to grayscale
image gray = call image.toGrayscale(screenshot);

// Convert to base64
string base64 = call image.toBase64(screenshot);

// Get raw bytes
array bytes = call image.getBytes(screenshot);
```

## Error Handling

The function throws an `InterpreterError` in the following cases:
- Screen name not specified and not in screen context
- Screen does not exist or is not shown
- Screen has no scene to capture
- Screenshot capture fails
- Timeout waiting for capture (10 seconds)

## Keyboard Shortcut Details

The **Ctrl+P** keyboard shortcut provides quick screenshot capture:
- **Location**: Screenshots are saved to the system temporary directory (`java.io.tmpdir`)
- **Naming**: Format is `{screenName}_{sequence}.png` where sequence is a 3-digit number (001, 002, etc.)
- **Sequence**: Each screen maintains its own counter that increments with each capture
- **Format**: Always saves as PNG format
- **Feedback**: Success message is printed to console output with full file path
- **Cross-platform**: Uses system temp directory, works on Windows, Linux, and macOS

This is useful for quick debugging, documentation, or capturing screen states during development.

## Implementation Notes

- **Thread Safety**: Uses CountDownLatch for proper synchronization between the calling thread and JavaFX Application Thread
- **Format**: Screenshots are captured as PNG format by default
- **Timeout**: 10-second timeout for capturing (configurable via `SNAPSHOT_TIMEOUT_SECONDS` constant)
- **Naming**: Captured images are named `{screenName}_screenshot` by default

## See Also
- [Image Builtins](image_builtins.md)
- [Screen Builtins](screen_builtins.md)
- [EbsImage Documentation](ebsimage.md)
