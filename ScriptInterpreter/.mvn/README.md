# Maven Configuration

## jvm.config

This file contains JVM arguments that Maven will automatically apply when running the application.

Currently configured arguments:
- `--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED`: Required for JavaFX WebView rendering engine (NGWebView internal class).
- `--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED`: Required for JavaFX WebView scene graph helpers (WebViewHelper internal class).
