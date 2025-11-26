// package com.eb.ui.tabs;  // adjust to your project
package com.eb.ui.tabs;

import com.eb.script.interpreter.builtins.Builtins;
import com.eb.script.interpreter.InterpreterError;
import com.eb.ui.ebs.EbsStyled;
import com.eb.ui.cli.ScriptArea;
import com.eb.ui.ebs.EbsConsoleHandler;
import com.eb.ui.ebs.EbsTab;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;

/**
 * Default implementation that centralizes the logic previously in
 * EbsHandler.showFileTab(...).
 */
public class TabHandler implements TabOpener {

    private Charset defaultCharset = StandardCharsets.UTF_8;
    private final TabPane tabPane;
    private final EbsConsoleHandler consoleHandler;
    private Tab tabBeingRemoved = null; // Track tab removal to prevent double-confirmation
    private boolean isHandlingClose = false; // Prevent recursive listener calls

    public TabHandler(EbsConsoleHandler consoleHandler, TabPane tabPane) {
        this.consoleHandler = consoleHandler;
        this.tabPane = Objects.requireNonNull(tabPane);
        setupTabCloseListener();
    }

    /**
     * Set up a listener to intercept tab removal and show confirmation for unsaved changes.
     */
    private void setupTabCloseListener() {
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            // Prevent recursive calls when we re-add or remove tabs
            if (isHandlingClose) {
                return;
            }
            
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab removed : change.getRemoved()) {
                        // Only handle if this is an EbsTab with unsaved changes
                        // and we haven't already confirmed this removal
                        if (removed instanceof EbsTab ebsTab && ebsTab.isDirty() && removed != tabBeingRemoved) {
                            // Set flag to prevent recursive calls
                            isHandlingClose = true;
                            
                            // Re-add the tab temporarily
                            int removeIndex = change.getFrom();
                            tabPane.getTabs().add(removeIndex, removed);
                            
                            // Select the tab so it's visible and active
                            tabPane.getSelectionModel().select(removed);
                            
                            // Reset flag after re-adding
                            isHandlingClose = false;
                            
                            // Show confirmation dialog
                            Platform.runLater(() -> {
                                boolean shouldClose = consoleHandler.confirmCloseTab(ebsTab);
                                if (shouldClose) {
                                    // User confirmed - remove the tab
                                    isHandlingClose = true;
                                    tabBeingRemoved = ebsTab;
                                    tabPane.getTabs().remove(ebsTab);
                                    tabBeingRemoved = null;
                                    isHandlingClose = false;
                                } else {
                                    // User cancelled - ensure tab is selected and focused
                                    Platform.runLater(() -> {
                                        tabPane.getSelectionModel().select(ebsTab);
                                        if (ebsTab.getContent() != null) {
                                            ebsTab.getContent().requestFocus();
                                        }
                                    });
                                }
                            });
                            
                            // Return early to avoid processing other changes
                            return;
                        }
                    }
                }
            }
        });
    }

    private Tab findTabByHandle(String handler) {
        if (handler == null) {
            return null;
        }
        for (Tab t : tabPane.getTabs()) {
            Object ud = t.getUserData();
            if (ud instanceof TabContext ctx) {
                if (handler.equals(ctx.fileContext.handle)) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Plain text viewer for non-ebs files.
     */
    private Region buildPlainViewer(TabContext ctx, Charset charset) {
        TextArea text = new TextArea();
        text.setEditable(false);
        text.setWrapText(false);
        text.setStyle("-fx-font-family: Consolas, 'Fira Code', monospace; -fx-font-size: 11px;");
        try {
            text.setText(Files.readString(ctx.path, charset));
        } catch (IOException ex) {
            text.setText("Error loading file:\n" + ex.getMessage());
        }
        return text;
    }

    /**
     * Build split view for .ebs files: top code, bottom output + Run button.
     */
    private Region buildEbsRunner(TabContext ctx, Tab ownerTab) {
        // Code area (editable)
        TextArea code = new TextArea();
        code.setWrapText(false);
        code.setStyle("-fx-font-family: Consolas, 'Fira Code', monospace; -fx-font-size: 11px;");
        try {
            code.setText(Files.readString(ctx.path, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            code.setText("// failed to load: " + ex.getMessage());
        }

        // Output area (run status & optional captured output)
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setStyle("-fx-font-family: Consolas, 'Fira Code', monospace; -fx-font-size: 11px;");
        output.setPromptText("Execution output will appear here...");

        // Buttons row (bottom)
        Button runBtn = new Button("Run");
        runBtn.setDefaultButton(true);
        runBtn.setPadding(new Insets(5, 10, 5, 10));

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> output.clear());

        HBox buttons = new HBox(8, runBtn, clearBtn);
        buttons.setStyle("-fx-padding: 0 0 0 0;");

        VBox bottom = new VBox(8, new Label("Output:"), output, buttons);
        bottom.setStyle("-fx-padding: 8;");

        // Split pane vertical
        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);

        VBox top = new VBox(6, new Label("Code:"), code);
        top.setStyle("-fx-padding: 6;");
        VBox.setVgrow(code, Priority.ALWAYS);

        split.getItems().addAll(top, bottom);
        split.setDividerPositions(0.6); // 60% code, 40% output by default
        // Keyboard shortcut: Ctrl+Enter to run
        code.setOnKeyPressed(ke -> {
            if (ke.isControlDown() && ke.getCode() == KeyCode.ENTER) {
                runBtn.fire();
                ke.consume();
            }
        });

        // Close button: ensure we free any resources (handles) if needed later
        ownerTab.setOnClosed(ev -> {
            /* optional: call a release if you add one */ });

        return split;
    }

    @Override
    public boolean showTab(TabContext context, boolean requestFocus) throws IOException, InterpreterError {
        // --- Begin: logic moved from EbsHandler.showFileTab(...) ---

        if (context.type == TabContext.TabType.FILE) {
            Path path = context.path;
            String filename = path.getFileName() != null ? path.getFileName().toString() : path.toString();
            String ext = filename.substring(filename.lastIndexOf('.'));

            // If already open, just focus; do not open a second handle
            Tab existing = findTabByHandle(context.fileContext.handle);
            if (existing != null) {
                if (requestFocus) {
                    tabPane.getSelectionModel().select(existing);
                }
                return true;
            }

            ScriptArea outputArea = new ScriptArea();
            ScriptArea dispArea = new ScriptArea();
            if (ext.equalsIgnoreCase(".ebs")) {
                dispArea.getStyleClass().addAll("console-out");
                outputArea.getStyleClass().addAll("console-out");
            }
            dispArea.setEditable(true);
            //area.getStyleClass().addAll("file-viewer", binary ? "binary-view" : "text-view");

            var dispAreaScroller = new VirtualizedScrollPane<>(dispArea);
            dispAreaScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            dispAreaScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            StackPane dispAreaFrame = new StackPane(dispAreaScroller);
            dispAreaFrame.getStyleClass().addAll("bevel-lowered");
            dispArea.setPadding(Insets.EMPTY);
            dispAreaScroller.setPadding(Insets.EMPTY);
            dispAreaFrame.setPadding(Insets.EMPTY);

            var outputAreaScroller = new VirtualizedScrollPane<>(outputArea);
            outputAreaScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            outputAreaScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            StackPane outputAreaFrame = new StackPane(outputAreaScroller);
            outputAreaFrame.getStyleClass().addAll("bevel-lowered");
            outputArea.setPadding(Insets.EMPTY);
            outputAreaScroller.setPadding(Insets.EMPTY);
            outputAreaFrame.setPadding(Insets.EMPTY);

            // Buttons row (bottom)
            Button runBtn = new Button("Run");
            runBtn.setDefaultButton(true);
            runBtn.setPadding(new Insets(5, 10, 5, 10));

            Button clearBtn = new Button("Clear");
            clearBtn.setOnAction(e -> outputArea.clear());

            HBox buttons = new HBox(8, runBtn, clearBtn);
            buttons.setStyle("-fx-padding: 6 4 0 0;");

            VBox bottom = new VBox(2, new Label("Output:"), outputAreaFrame, buttons);
            bottom.setStyle("-fx-padding: 2;");

            VBox top = new VBox(2, new Label("Code:"), dispAreaFrame);
            top.setStyle("-fx-padding: 2;");
            VBox.setVgrow(dispAreaFrame, Priority.ALWAYS);
            VBox.setVgrow(outputAreaFrame, Priority.ALWAYS);

            // Split pane vertical
            SplitPane split = new SplitPane();
            split.setOrientation(Orientation.VERTICAL);
            split.getItems().addAll(top, bottom);
            split.setDividerPositions(0.6); // 60% code, 40% output by default
            Tab tab = new EbsTab(context);
            
            // Set the status bar on the tab's handler so it can update the status bar
            if (tab instanceof EbsTab ebsTab) {
                if (ebsTab.getHandler() instanceof com.eb.ui.ebs.EbsHandler ebsHandler) {
                    // Get the status bar from the console handler and set it on this tab's handler
                    com.eb.ui.ebs.StatusBar statusBar = consoleHandler.getStatusBar();
                    if (statusBar != null) {
                        ebsHandler.setStatusBar(statusBar);
                    }
                }
            }

            tabPane.getTabs().add(tab);
            select(tab, requestFocus);

        }
        return false;
    }

    public Tab getSelectedTab() {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    public TabContext getSelectedTabContext() {
        Tab t = getSelectedTab();
        Object ud = t.getUserData();
        if (ud instanceof TabContext ctx) {
            return ctx;
        }
        return null;
    }
    
    /**
     * Close the given tab
     * @param tab The tab to close
     */
    public void closeTab(Tab tab) {
        if (tab != null && tabPane.getTabs().contains(tab)) {
            // Check if this is an EbsTab with unsaved changes
            if (tab instanceof EbsTab ebsTab) {
                boolean shouldClose = consoleHandler.confirmCloseTab(ebsTab);
                if (!shouldClose) {
                    return; // Don't close the tab
                }
            }
            tabPane.getTabs().remove(tab);
        }
    }

    /**
     * Create a new tab for a file (even if it doesn't exist yet).
     * Returns the created EbsTab so the caller can initialize it.
     * @param context The tab context
     * @param requestFocus Whether to focus the new tab
     * @return The created EbsTab, or null if creation failed
     */
    public EbsTab createNewTab(TabContext context, boolean requestFocus) {
        try {
            Tab tab = new EbsTab(context);
            
            // Set the status bar on the tab's handler
            if (tab instanceof EbsTab ebsTab) {
                if (ebsTab.getHandler() instanceof com.eb.ui.ebs.EbsHandler ebsHandler) {
                    com.eb.ui.ebs.StatusBar statusBar = consoleHandler.getStatusBar();
                    if (statusBar != null) {
                        ebsHandler.setStatusBar(statusBar);
                    }
                }
            }

            tabPane.getTabs().add(tab);
            select(tab, requestFocus);
            return (EbsTab) tab;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void select(Tab tab, boolean requestFocus) {
        runOnFx(() -> {
            tabPane.getSelectionModel().select(tab);
            if (requestFocus && tab.getContent() != null) {
                tab.getContent().requestFocus();
            }
        });
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static boolean looksBinary(byte[] sample) {
        if (sample == null) {
            return false;
        }
        int nonText = 0, checked = 0;
        for (byte b : sample) {
            int u = b & 0xFF;
            if (u == 0) {
                return true;
            }                // NUL strongly suggests binary
            if (u < 0x09) {
                nonText++;                    // control range
            }
            checked++;
            if (checked >= 512) {
                break;       // tiny sample
            }
        }
        return nonText > 4;
    }

}
