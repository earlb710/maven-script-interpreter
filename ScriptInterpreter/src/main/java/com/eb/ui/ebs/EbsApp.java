package com.eb.ui.ebs;

import com.eb.script.RuntimeContext;
import com.eb.ui.cli.Console;
import com.eb.util.Util;
import java.io.IOException;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.MenuBar;

/**
 * A minimal JavaFX console with: - Output TextArea - Input TextField (press
 * Enter to submit) - Optional System.out/err redirection to output - Simple
 * history (UP/DOWN)
 */
public class EbsApp {

    RuntimeContext ctx = new RuntimeContext("console");

    private Stage stage; // store primary stage for dialogs
    private TabPane mainTabs;
    private Console console;
    private EbsConsoleHandler handler;
    private StatusBar statusBar; // Main window status bar
    
    // Static reference to root for config reloading
    private static BorderPane rootPane;
    private static final String CONFIG_CSS_FILE = "console-config.css";

    public void start(Stage primaryStage) {

        this.stage = primaryStage;
        // Ensure sandbox directory exists
        try {
            Files.createDirectories(Util.SANDBOX_ROOT);
        } catch (IOException ignored) {
        }

        // Confirm on exit only if there are dirty tabs or running screens
        stage.setOnCloseRequest(evt -> {
            boolean needsConfirmation = hasDirtyTabs() || hasRunningScreens();
            
            if (needsConfirmation) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Exit the application? Unsaved data will be lost.",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("Confirm Exit");
                var res = confirm.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                    evt.consume(); // cancel close
                    return;
                }
            }
            // User confirmed exit or no confirmation needed - cleanup all screens and threads
            cleanupScreens();
            // Exit JavaFX platform to ensure all windows close
            Platform.exit();
        });
        handler = new EbsConsoleHandler(stage, ctx);
        console = new Console(handler);

        BorderPane root = new BorderPane();
        rootPane = root; // Store static reference for config reloading
        
        // Load and apply console configuration to root BEFORE initUI
        ConsoleConfig consoleConfig = new ConsoleConfig();
        applyConsoleConfig(root, consoleConfig);
        
        initUI(root);
        
        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/css/console.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("EBS Console");
        primaryStage.show();
        console.requestFocus();

        console.printlnInfo("Console ready. Type \"/help\" and then [control+enter] for help on commands.");
    }

    /**
     * Run something off the FX thread and stream progress back to the console.
     */
    private void runDemoBackgroundTask(Thread t) {
//        Thread t = new Thread(() -> {
//            printlnInfo("[demo] Starting background work...");
//            for (int i = 1; i <= 5; i++) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException ignored) {
//                }
//                System.out.println("[demo] Tick " + i + " (via System.out)");
//            }
//            System.err.println("[demo] Finished (this line uses System.err).");
//        }, "demo-thread");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Cleanup all screens and threads when the application is closing
     */
    private void cleanupScreens() {
        try {
            Object interpreter = ctx.environment.getCurrentInterpreter();
            if (interpreter instanceof com.eb.script.interpreter.Interpreter) {
                ((com.eb.script.interpreter.Interpreter) interpreter).cleanup();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * If you hook streams, restore them on exit if you wish.
     * This is called when the JavaFX application is stopping.
     */
    public void stop() {
        cleanupScreens();
        Platform.exit();
    }

    public void submit(String... lines) throws Exception {
        console.submit(lines);
    }

    private void initUI(BorderPane root) {
        // Tabs & console (existing code)
        mainTabs = new TabPane();
        mainTabs.setId("mainTabs");
        mainTabs.getStyleClass().add("viewer-tabs");
        mainTabs.getTabs().add(console.getConsoleTab());
        mainTabs.getSelectionModel().select(console.getConsoleTab());

        // Focus input when Console tab is selected
        mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == console.getConsoleTab()) {
                Platform.runLater(() -> console.requestFocus());
            }
        });

        handler.setUI_outputArea(console.getOutputArea());
        handler.setUI_tabPane(handler, mainTabs);
        
        // Create project tree view on the right side
        ProjectTreeView projectTreeView = new ProjectTreeView(handler);
        handler.setProjectTreeView(projectTreeView);
        
        // Use SplitPane to put tree on the right
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.getItems().addAll(mainTabs, projectTreeView);
        splitPane.setDividerPositions(0.75); // 75% for main content, 25% for tree

        // --- Menu bar ---
        MenuBar menuBar = new EbsMenu(handler);
        root.setTop(menuBar);
        root.setCenter(splitPane); // Changed from mainTabs to splitPane
        
        // --- Status bar ---
        statusBar = new StatusBar();
        root.setBottom(statusBar);
        
        // Make status bar accessible to handler
        handler.setStatusBar(statusBar);

    }

    /**
     * Apply console configuration to the root parent.
     * Generates CSS from the configuration and adds it as a stylesheet to the parent.
     * Parent-level stylesheets have higher priority than Scene-level stylesheets.
     * 
     * @param parent The parent node to apply the configuration to
     * @param config The console configuration
     */
    private void applyConsoleConfig(javafx.scene.Parent parent, ConsoleConfig config) {
        if (config == null) {
            return;
        }
        
        // Generate CSS from configuration
        String css = config.generateCSS();
        
        // Add as stylesheet to parent (higher priority than scene-level)
        if (css != null && !css.isEmpty()) {
            try {
                // Write CSS to a fixed file in the working directory
                java.nio.file.Path cssFile = java.nio.file.Paths.get(CONFIG_CSS_FILE);
                java.nio.file.Files.writeString(cssFile, css);
                
                // Load the CSS file as a stylesheet (same approach as other stylesheets)
                String cssUri = cssFile.toUri().toString();
                parent.getStylesheets().add(cssUri);
                
            } catch (IOException e) {
                System.err.println("Warning: Failed to apply console configuration: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reload the console configuration from console.cfg and reapply CSS styles.
     * This method can be called from scripts to apply config changes without restarting.
     * Must be called on the JavaFX Application Thread.
     * 
     * @return true if config was successfully reloaded and applied, false otherwise
     */
    public static boolean reloadConfig() {
        if (rootPane == null) {
            System.err.println("Warning: Cannot reload config - root pane not initialized");
            return false;
        }
        
        try {
            // Reload config from file
            ConsoleConfig config = new ConsoleConfig();
            
            // Generate new CSS
            String css = config.generateCSS();
            if (css == null || css.isEmpty()) {
                System.err.println("Warning: Generated CSS is empty");
                return false;
            }
            
            // Write CSS to file
            java.nio.file.Path cssFile = java.nio.file.Paths.get(CONFIG_CSS_FILE);
            java.nio.file.Files.writeString(cssFile, css);
            
            // Get the stylesheet URI
            String cssUri = cssFile.toUri().toString();
            
            // Remove old config stylesheet if present (use endsWith for exact matching)
            rootPane.getStylesheets().removeIf(s -> s.endsWith(CONFIG_CSS_FILE));
            
            // Add the new stylesheet
            rootPane.getStylesheets().add(cssUri);
            
            // Force a layout pass to ensure styles are applied
            rootPane.applyCss();
            rootPane.layout();
            
            System.out.println("Console configuration reloaded successfully");
            return true;
            
        } catch (IOException e) {
            System.err.println("Warning: Failed to reload console configuration: " + e.getMessage());
            return false;
        }
    }

    // Escape content for a single EBS string literal
    private static String escapeForEbsString(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("\\", "\\\\") // backslashes
                .replace("\"", "\\\"") // quotes
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    /**
     * Check if any open tabs have unsaved changes (dirty state).
     * @return true if there are dirty tabs, false otherwise
     */
    private boolean hasDirtyTabs() {
        if (mainTabs == null) {
            return false;
        }
        for (Tab tab : mainTabs.getTabs()) {
            if (tab instanceof EbsTab ebsTab && ebsTab.isDirty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if any screens are currently running (open or hidden).
     * A screen is considered "running" if it exists in the global screens map,
     * regardless of whether it's currently showing or hidden.
     * @return true if there are running screens, false otherwise
     */
    private boolean hasRunningScreens() {
        try {
            var screens = com.eb.script.interpreter.InterpreterContext.getGlobalScreens();
            return screens != null && !screens.isEmpty();
        } catch (Exception e) {
            // Ignore errors checking screen state
        }
        return false;
    }

}
