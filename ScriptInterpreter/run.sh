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

# Run with required JVM arguments for JavaFX WebView
java --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
     -cp target/classes \
     com.eb.ui.cli.MainApp "$@"
