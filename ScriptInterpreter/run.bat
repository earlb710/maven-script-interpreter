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

REM Get the module path from Maven (store in a temporary file)
call mvn dependency:build-classpath -Dmdep.pathSeparator=; -q -DincludeScope=compile -Dsilent=true > modulepath.tmp
set /p MODULE_PATH=<modulepath.tmp
del modulepath.tmp

REM Run with module system and required JVM arguments for JavaFX WebView
java --module-path "target\classes;%MODULE_PATH%" ^
     --add-modules com.eb.scriptinterpreter ^
     --add-exports javafx.graphics/com.sun.javafx.sg.prism=com.eb.scriptinterpreter,ALL-UNNAMED ^
     --add-exports javafx.graphics/com.sun.javafx.scene=com.eb.scriptinterpreter,ALL-UNNAMED ^
     --add-exports javafx.graphics/com.sun.javafx.util=com.eb.scriptinterpreter,ALL-UNNAMED ^
     --add-exports javafx.web/com.sun.webkit=com.eb.scriptinterpreter,ALL-UNNAMED ^
     --module com.eb.scriptinterpreter/com.eb.ui.cli.MainApp %*

