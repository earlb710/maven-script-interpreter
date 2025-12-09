# EBS Script Interpreter - Running the Application

## Quick Start

### Using the provided launch scripts (Recommended):

**Linux/Mac:**
```bash
./run.sh
```

**Windows:**
```cmd
run.bat
```

These scripts automatically include the required JVM arguments for JavaFX WebView support.

## Alternative Launch Methods

### Using Maven:
```bash
mvn javafx:run
```

### Using IntelliJ IDEA or Other IDEs:

When running `com.eb.ui.cli.MainApp` from your IDE, you **must** add the following VM options:

```
--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED
--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED
--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
```

**In IntelliJ IDEA:**
1. Run → Edit Configurations...
2. Select your MainApp configuration
3. Add to "VM options" field: `--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED`
4. Apply and OK

**In Eclipse:**
1. Run → Run Configurations...
2. Select your MainApp configuration
3. Go to "Arguments" tab
4. Add to "VM arguments": `--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED`
5. Apply

### Running directly with java:
```bash
# First compile
mvn compile

# Then run with required JVM arguments
java --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
     -cp target/classes \
     com.eb.ui.cli.MainApp
```

## Why is this JVM argument required?

The JavaFX WebView component requires access to internal JavaFX graphics classes that are not exported by default in Java's module system:
- `com.sun.javafx.sg.prism` package - Required for WebView's rendering engine (NGWebView extends NGGroup)
- `com.sun.javafx.scene` package - Required for WebView's scene graph helpers (WebViewHelper extends ParentHelper)
- `com.sun.javafx.util` package - Required for WebView's utility classes (Utils internal class)

The `--add-exports` arguments explicitly export these internal packages to allow WebView to function properly.

Without these arguments, you'll encounter an `IllegalAccessError` when trying to use WebView controls in screens.

## Troubleshooting

**Error: IllegalAccessError with NGWebView**
```
java.lang.IllegalAccessError: superclass access check failed: 
class com.sun.javafx.sg.prism.web.NGWebView cannot access 
class com.sun.javafx.sg.prism.NGGroup
```

**Solution:** Ensure you're using one of the launch methods above that includes the required JVM argument.
