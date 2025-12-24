# Test Scripts Guide: disableMaximize Property

## Overview

Three test scripts are provided to validate and demonstrate the `disableMaximize` screen property.

## Test Script Comparison

| Script | Type | Best For | Features |
|--------|------|----------|----------|
| **test_disable_maximize_quick.ebs** | Quick Validation | CI/CD, quick checks | Side-by-side windows, color-coded, validation steps |
| **test_disable_maximize_interactive.ebs** | Interactive | Development, exploration | Dynamic configuration, preset scenarios, real-time testing |
| **test_disable_maximize.ebs** | Basic Demo | Documentation, simple demo | Static examples, straightforward |

---

## 1. Quick Validation Test (Recommended for Testing)

**File:** `test_disable_maximize_quick.ebs`

### Purpose
Quick side-by-side comparison of three window configurations for easy validation.

### Features
- ✅ Three pre-configured windows that open simultaneously
- ✅ Color-coded content (green, red, blue) for easy identification
- ✅ Clear expected behaviors listed in each window
- ✅ Step-by-step validation instructions in console output
- ✅ Fast execution - ideal for quick regression testing

### Windows Created
1. **CONTROL** (Green text) - Normal window, can maximize
2. **TEST** (Red text) - disableMaximize=true, cannot maximize
3. **FIXED** (Blue text) - Both resizable=false and disableMaximize=true

### How to Use
```bash
# From EBS console
/load test_disable_maximize_quick.ebs

# Or from command line
java -cp ... com.eb.script.Run test_disable_maximize_quick.ebs
```

### Validation Steps
1. Try to maximize the CONTROL window → Should work
2. Try to maximize the TEST window → Should NOT work
3. Try to resize the FIXED window → Should NOT work

---

## 2. Interactive Test Panel (Recommended for Development)

**File:** `test_disable_maximize_interactive.ebs`

### Purpose
Provides a comprehensive control panel for testing various configurations dynamically.

### Features
- ✅ Quick preset scenarios with keyboard shortcuts
- ✅ Manual configuration of all window properties
- ✅ Dynamic screen creation using screen.define()
- ✅ Real-time status updates
- ✅ Multiple test windows can be created without restarting

### Keyboard Shortcuts
- **Alt+1** - Preset: Normal window (can resize and maximize)
- **Alt+2** - Preset: No maximize window (can resize, cannot maximize)
- **Alt+3** - Preset: Fixed dialog (cannot resize or maximize)
- **Ctrl+T** - Create test window with current configuration

### Configuration Options
- Window title
- Window width and height
- Resizable checkbox
- Disable Maximize checkbox (the feature being tested)

### How to Use
```bash
# From EBS console
/load test_disable_maximize_interactive.ebs

# The control panel window will open
# Use preset buttons or configure manually
# Click "Create Test Window" to generate test windows
```

### Workflow
1. Open the interactive test panel
2. Choose a preset (Alt+1, Alt+2, Alt+3) OR configure manually
3. Press Ctrl+T or click "Create Test Window"
4. Test the created window's maximize behavior
5. Repeat with different configurations

---

## 3. Basic Demo (Simplest)

**File:** `test_disable_maximize.ebs`

### Purpose
Simple static demonstration with three pre-configured windows.

### Features
- ✅ Straightforward code, easy to understand
- ✅ Good for documentation examples
- ✅ Shows three common use cases

### Windows Created
1. Normal screen (can maximize)
2. No maximize screen (cannot maximize)
3. Fixed dialog (completely fixed)

### How to Use
```bash
# From EBS console
/load test_disable_maximize.ebs
```

---

## Which Test Script Should I Use?

### For Quick Validation
→ **Use test_disable_maximize_quick.ebs**
- Fast execution
- Clear pass/fail indicators
- Side-by-side comparison

### For Development & Experimentation
→ **Use test_disable_maximize_interactive.ebs**
- Test multiple configurations
- No need to edit script or restart
- Keyboard shortcuts for quick testing

### For Simple Demonstration
→ **Use test_disable_maximize.ebs**
- Minimal code
- Good for showing basic usage
- Easy to understand

---

## Expected Behaviors

### When disableMaximize = true
- ✅ Window opens at specified size
- ✅ Window can be resized (if resizable=true)
- ❌ Maximize button does nothing when clicked
- ❌ Keyboard shortcuts for maximize are blocked
- ❌ Double-clicking title bar doesn't maximize (on some OSes)

### When disableMaximize = false (default)
- ✅ Window opens at specified size
- ✅ Window can be resized (if resizable=true)
- ✅ Maximize button works normally
- ✅ Keyboard shortcuts work normally
- ✅ All maximize methods work as expected

---

## Test Matrix

| Test Case | resizable | disableMaximize | Can Resize? | Can Maximize? |
|-----------|-----------|-----------------|-------------|---------------|
| Normal    | true      | false           | ✅ Yes      | ✅ Yes        |
| NoMax     | true      | true            | ✅ Yes      | ❌ No         |
| Fixed     | false     | true            | ❌ No       | ❌ No         |

---

## Troubleshooting Test Scripts

### Interactive Test: screen.define() not found
The interactive test uses `screen.define()` which may be a newer feature. If this function doesn't exist, use the basic or quick test instead.

### Windows Don't Open
Check console output for errors. Ensure:
- Scripts are in the correct directory
- No syntax errors in the EBS code
- JavaFX runtime is available

### Maximize Button Still Visible
This is expected behavior. The maximize button remains visible but becomes non-functional when `disableMaximize=true`. This is a JavaFX platform limitation.

---

## Additional Notes

### Performance
All test scripts are lightweight and execute quickly. The interactive test maintains a single control panel window while generating test windows dynamically.

### Cleanup
Test windows can be closed individually. The quick test creates three windows that all close independently.

### Customization
You can modify the test scripts to add:
- Different window sizes
- Additional test scenarios
- Custom styling
- More complex content

---

## Examples from Test Scripts

### Quick Test Example
```ebs
screen testWindow = {
    "title": "TEST: Cannot Maximize",
    "width": 500,
    "height": 350,
    "resizable": true,
    "disableMaximize": true,  // ← Feature being tested
    "vars": [ ... ]
};
```

### Interactive Test Example
```ebs
// Dynamic screen creation
json:screenDef = {
    "title": testTitle,
    "width": testWidth,
    "height": testHeight,
    "resizable": testResizable,
    "disableMaximize": testDisableMaximize,  // ← User-controlled
    "vars": [ ... ]
};
screen.define(screenName, screenDef);
show screen screenName;
```

---

## Related Documentation

- **SCREEN_DISABLE_MAXIMIZE_PROPERTY.md** - Complete feature documentation
- **SCREEN_DISABLE_MAXIMIZE_SUMMARY.md** - Implementation details
- **README.md** - General EBS language documentation

---

**Last Updated:** December 2025  
**Feature Version:** 1.0
