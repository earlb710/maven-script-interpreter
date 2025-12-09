@echo off
REM Launch script for EBS Script Interpreter (Windows)
REM This script ensures the required JVM arguments are passed

cd /d "%~dp0"

REM Check if compiled
if not exist "target\classes" (
    echo Project not compiled. Running 'mvn compile'...
    call mvn compile
    if errorlevel 1 (
        echo Compilation failed!
        exit /b 1
    )
)

REM Run with required JVM arguments for JavaFX WebView
java --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED ^
     -cp target\classes ^
     com.eb.ui.cli.MainApp %*
