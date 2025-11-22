# Console Configuration Test File

This file (`console.cfg.test`) provides a high-contrast color scheme for testing the console configuration feature.

## How to Use

### Option 1: Copy to Test
```bash
cp console.cfg.test console.cfg
```
Then restart the console application to see the high-contrast theme.

### Option 2: Swap Configurations
```bash
# Save current config
mv console.cfg console.cfg.backup

# Use test config
cp console.cfg.test console.cfg

# Restart console to test

# Restore original
mv console.cfg.backup console.cfg
```

## Test Configuration Details

This test configuration uses a high-contrast color scheme with:
- **Background**: Dark gray (#1a1a1a) instead of black
- **Text**: Bright white (#f0f0f0)
- **Caret**: Bright yellow (#ffff00) for high visibility
- **Error**: Pure red (#ff0000)
- **Warning**: Orange (#ffaa00)
- **Success**: Bright green (#00ff00)
- **Keywords**: Cyan (#00ffff)
- **Builtins**: Light cyan (#66cccc)
- **Comments**: Bright green (#00ff00)

This makes it easy to verify that the configuration system is working correctly by providing visually distinct colors from the default theme.

## Validation

To verify the test configuration is valid JSON and loads correctly, run:
```bash
# Check JSON syntax
cat console.cfg.test | python -m json.tool > /dev/null && echo "Valid JSON"

# Or test with the application
cp console.cfg.test console.cfg
mvn javafx:run
```

## See Also

- **console.cfg**: Default configuration
- **CONSOLE_CONFIG_GUIDE.md**: Complete documentation with more examples
