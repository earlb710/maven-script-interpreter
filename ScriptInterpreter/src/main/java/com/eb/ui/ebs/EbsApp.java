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

    public void start(Stage primaryStage) {

        this.stage = primaryStage;
        // Ensure sandbox directory exists
        try {
            Files.createDirectories(Util.SANDBOX_ROOT);
        } catch (IOException ignored) {
        }

        // Confirm on exit
        stage.setOnCloseRequest(evt -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Exit the application? Unsaved data will be lost.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Confirm Exit");
            var res = confirm.showAndWait();
            if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                evt.consume(); // cancel close
            } else {
                // User confirmed exit - cleanup all screens and threads
                cleanupScreens();
                // Exit JavaFX platform to ensure all windows close
                Platform.exit();
            }
        });
        handler = new EbsConsoleHandler(stage, ctx);
        console = new Console(handler);

        BorderPane root = new BorderPane();
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

        // --- Menu bar ---
        MenuBar menuBar = new EbsMenu(handler);
        root.setTop(menuBar);
        root.setCenter(mainTabs);

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

}
