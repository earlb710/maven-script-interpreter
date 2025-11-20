package com.eb.script.interpreter;

/**
 * Built-in functions for screen control operations.
 * Provides methods for showing, hiding, and closing screen windows.
 *
 * @author Earl Bosch
 */
public class BuiltinsScreen {

    /**
     * scr.showScreen(screenName?) -> BOOL Shows a screen. If screenName is null
     * or empty, uses the current screen from context. Returns true on success.
     */
    public static Object screenShow(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.showScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.showScreen: Screen '" + screenName + "' does not exist.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.showScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Show the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (!stage.isShowing()) {
                stage.show();
                if (context.getOutput() != null) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' shown");
                }
            } else {
                if (context.getOutput() != null) {
                    context.getOutput().printlnInfo("Screen '" + finalScreenName + "' is already showing");
                }
            }
        });
        return true;
    }

    /**
     * scr.hideScreen(screenName?) -> BOOL Hides a screen. If screenName is null
     * or empty, uses the current screen from context. Returns true on success.
     */
    public static Object screenHide(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.hideScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.hideScreen: Screen '" + screenName + "' does not exist.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.hideScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Hide the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            boolean wasShowing = stage.isShowing();
            stage.hide();
            if (context.getOutput() != null) {
                if (wasShowing) {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden");
                } else {
                    context.getOutput().printlnOk("Screen '" + finalScreenName + "' hidden (was already hidden)");
                }
            }
        });
        return true;
    }

    /**
     * scr.closeScreen(screenName?) -> BOOL Closes a screen. If screenName is
     * null or empty, uses the current screen from context. Returns true on
     * success.
     */
    public static Object screenClose(InterpreterContext context, Object[] args) throws InterpreterError {
        String screenName = (args.length > 0 && args[0] != null) ? (String) args[0] : null;

        // If no screen name provided, determine from thread context
        if (screenName == null || screenName.isEmpty()) {
            screenName = context.getCurrentScreen();
            if (screenName == null) {
                throw new InterpreterError(
                        "scr.closeScreen: No screen name specified and not executing in a screen context. "
                        + "Provide a screen name or call from within screen event handlers.");
            }
        }

        // Check if screen exists
        if (!context.getScreens().containsKey(screenName)) {
            throw new InterpreterError("scr.closeScreen: Screen '" + screenName + "' does not exist.");
        }

        javafx.stage.Stage stage = context.getScreens().get(screenName);
        if (stage == null) {
            throw new InterpreterError("scr.closeScreen: Screen '" + screenName + "' is still being initialized.");
        }

        final String finalScreenName = screenName;

        // Close the screen on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            // Close the stage
            if (stage.isShowing()) {
                stage.close();
            }

            // Interrupt and stop the screen thread
            Thread thread = context.getScreenThreads().get(finalScreenName);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }

            // Clean up resources
            context.remove(finalScreenName);

            if (context.getOutput() != null) {
                context.getOutput().printlnOk("Screen '" + finalScreenName + "' closed");
            }
        });

        return true;
    }
}
