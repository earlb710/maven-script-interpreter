#!/bin/bash
# Launch script for EBS Script Interpreter
# This script ensures the required JVM arguments are passed

cd "$(dirname "$0")"

# Check if compiled
if [ ! -d "target/classes" ]; then
    echo "Project not compiled. Running 'mvn compile'..."
    mvn compile
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
fi

# Get the module path from Maven
MODULE_PATH=$(mvn dependency:build-classpath -Dmdep.pathSeparator=: -q -DincludeScope=compile -Dsilent=true | tail -1)

# Run with module system and required JVM arguments for JavaFX WebView
java --module-path "target/classes:${MODULE_PATH}" \
     --add-modules com.eb.scriptinterpreter \
     --add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED \
     --add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED \
     --module com.eb.scriptinterpreter/com.eb.ui.cli.MainApp "$@"

