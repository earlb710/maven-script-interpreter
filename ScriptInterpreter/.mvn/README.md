# Maven Configuration

## jvm.config

This file contains JVM arguments that Maven will automatically apply when running the application.

Currently configured arguments:
- `--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED`: Required for JavaFX WebView to access internal prism classes from the javafx.graphics module.
