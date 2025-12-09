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

These scripts automatically run the application with the Java module system and include the required JVM arguments for JavaFX WebView support.

## Alternative Launch Methods

### Using Maven:
```bash
mvn javafx:run
```

### Using IntelliJ IDEA or Other IDEs:

When running `com.eb.ui.cli.MainApp` from your IDE, you **must** configure it to run with the module system and add the following VM options:

```
--module-path target/classes:${MAVEN_CLASSPATH}
--add-modules com.eb.scriptinterpreter
--add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED
--add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED
--add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED
--add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED
--module com.eb.scriptinterpreter/com.eb.ui.cli.MainApp
```

**In IntelliJ IDEA:**
1. Run → Edit Configurations...
2. Select your MainApp configuration
3. Set "Module" to use module path
4. Add to "VM options" field (replace `${MAVEN_CLASSPATH}` with your actual classpath):
   ```
   --module-path target/classes:${MAVEN_CLASSPATH}
   --add-modules com.eb.scriptinterpreter
   --add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED
   ```
5. Set Main class to: `com.eb.scriptinterpreter/com.eb.ui.cli.MainApp`
6. Apply and OK

**In NetBeans:**
The project includes a properly configured `nbactions.xml` file that automatically uses the module system with all required exports when you run the project from NetBeans (F6 or Run → Run Project). No additional configuration needed.

**In Eclipse:**
1. Run → Run Configurations...
2. Select your MainApp configuration
3. Go to "Arguments" tab
4. Add to "VM arguments" (adjust classpath as needed):
   ```
   --module-path target/classes:${MAVEN_CLASSPATH}
   --add-modules com.eb.scriptinterpreter
   --add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED
   --add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED
   ```
5. Set Main class to: `com.eb.scriptinterpreter/com.eb.ui.cli.MainApp`
6. Apply

### Running directly with java:
```bash
# First compile
mvn compile

# Get the module path (classpath of dependencies)
MODULE_PATH=$(mvn dependency:build-classpath -Dmdep.pathSeparator=: -q -DincludeScope=compile -Dsilent=true | tail -1)

# Then run with module system and required JVM arguments
java --module-path "target/classes:${MODULE_PATH}" \
     --add-modules com.eb.scriptinterpreter \
     --add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED \
     --module com.eb.scriptinterpreter/com.eb.ui.cli.MainApp
```

## Why are these JVM arguments required?

The application now uses the Java Platform Module System (JPMS). The JavaFX WebView component and some third-party libraries (like javafxsvg and Batik) require access to internal JavaFX packages that are not exported by default:

- `javafx.graphics/com.sun.javafx.sg.prism` - Required for WebView's rendering engine (NGWebView extends NGGroup)
- `javafx.graphics/com.sun.javafx.scene` - Required for WebView's scene graph helpers (WebViewHelper extends ParentHelper)
- `javafx.graphics/com.sun.javafx.util` - Required for WebView's utility classes (Utils internal class)
- `javafx.web/com.sun.webkit` - Required for WebView's WebKit engine internals

The exports are made to:
- `com.eb.scriptinterpreter` - Our named module
- `ALL-UNNAMED` - Automatic modules (third-party libraries without module-info)

Without these arguments, you'll encounter an `IllegalAccessError` when trying to use WebView controls in screens.

## Troubleshooting

**Error: IllegalAccessError with NGWebView**
```
java.lang.IllegalAccessError: superclass access check failed: 
class com.sun.javafx.sg.prism.web.NGWebView (in unnamed module) cannot access 
class com.sun.javafx.sg.prism.NGGroup (in module javafx.graphics)
```

**Solution:** Ensure you're using one of the launch methods above that includes:
1. The `--module-path` and `--module` arguments (to run with the module system)
2. The required `--add-exports` arguments with `ALL-UNNAMED` target

**Common Mistake:** Running with `-cp` (classpath) instead of `--module-path` will cause WebView to fail because the automatic modules won't have access to JavaFX internals.

