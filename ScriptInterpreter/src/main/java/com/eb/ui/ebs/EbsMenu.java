package com.eb.ui.ebs;

import com.eb.script.interpreter.builtins.AiFunctions;
import com.eb.ui.tabs.TabContext;
import java.nio.file.Path;
import java.util.Deque;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 *
 * @author Earl Bosch
 */
public class EbsMenu extends MenuBar {

    public final Menu recentMenu;
    public final Menu screensMenu;
    public final EbsConsoleHandler handler;

    public EbsMenu(EbsConsoleHandler handler) {
        this.handler = handler;
        recentMenu = new Menu("Recent files");
        screensMenu = new Menu("Screens");
        handler.loadRecentFiles();
        Menu fileMenu = new Menu("File");

        // --- New Script File ---
        MenuItem newItem = new MenuItem("New Script File");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newItem.setOnAction(event -> {
            handler.createNewScriptFile();
            refreshRecentMenu();
        });

        // --- Open file… ---
        MenuItem openItem = new MenuItem("Open file…");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        });
        openItem.setOnAction(event -> {
            handler.chooseOpenFile();
            refreshRecentMenu();
        });

        // --- Recent files submenu ---
        refreshRecentMenu();  // builds its items from 'recentFiles'

        // EbsApp.java (inside initUI(BorderPane root), after you've created fileMenu)
        // --- Save ---
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                // Get the original path from the TabContext kept in userData
                var ctx = (TabContext) et.getUserData();
                if (ctx != null && ctx.path != null) {
                    handler.saveHandle(et); // see helper below
                    refreshRecentMenu();
                } else {
                    // If no path (edge case), fall back to Save As…
                    handler.chooseSaveAs(et);
                    refreshRecentMenu();
                }
            }
        });

// --- Save As… ---
        MenuItem saveAsItem = new MenuItem("Save As…");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                handler.chooseSaveAs(et); // see helper below
                refreshRecentMenu();
            }
        });

        // Add Save/Save As into File menu, ahead of Exit:
        fileMenu.getItems().addAll(saveItem, saveAsItem);

        // --- Exit (route via close-request so confirmation is shown) ---
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                handler.exit();
            }
        });

        fileMenu.getItems().addAll(newItem, openItem, recentMenu, new SeparatorMenuItem(), exitItem);
        getMenus().add(fileMenu);

        // --- Edit Menu ---
        Menu editMenu = new Menu("Edit");
        
        // Cut
        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.dispArea.cut();
            }
        });
        
        // Copy
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.dispArea.copy();
            }
        });
        
        // Paste
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.dispArea.paste();
            }
        });
        
        // Undo
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.dispArea.undo();
            }
        });
        
        // Redo
        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.dispArea.redo();
            }
        });
        
        // Find
        MenuItem findItem = new MenuItem("Find");
        findItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        findItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.showFindFromMenu(false);
            }
        });
        
        // Replace
        MenuItem replaceItem = new MenuItem("Replace");
        replaceItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        replaceItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.showFindFromMenu(true);
            }
        });
        
        // Show/Hide Line Numbers
        MenuItem toggleLineNumbersItem = new MenuItem("Show/Hide Line Numbers");
        toggleLineNumbersItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        toggleLineNumbersItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            if (tab instanceof EbsTab et) {
                et.toggleLineNumbers();
            }
        });
        
        // Close Tab
        MenuItem closeTabItem = new MenuItem("Close");
        closeTabItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
        closeTabItem.setOnAction(e -> {
            Tab tab = handler.getSelectedTab();
            // Only close if it's not the console tab (which is not closable)
            if (tab != null && tab.isClosable()) {
                handler.closeTab(tab);
            }
        });
        
        editMenu.getItems().addAll(cutItem, copyItem, pasteItem, new SeparatorMenuItem(), undoItem, redoItem, new SeparatorMenuItem(), findItem, replaceItem, new SeparatorMenuItem(), toggleLineNumbersItem, closeTabItem);
        getMenus().add(editMenu);

        Menu toolsMenu = new Menu("Config");
        MenuItem aiSetupItem = new MenuItem("AI Chat Model Setup…");
        aiSetupItem.setOnAction(e -> AiFunctions.showModelSetupDialog());

        MenuItem safeDirsItem = new MenuItem("Safe Directories…");
        safeDirsItem.setOnAction(e -> {
            SafeDirectoriesDialog dialog = new SafeDirectoriesDialog();
            dialog.show();
        });
        
        MenuItem dbConfigItem = new MenuItem("Database Config…");
        dbConfigItem.setOnAction(e -> {
            DatabaseConfigDialog dialog = new DatabaseConfigDialog();
            dialog.show();
        });
        
        MenuItem colorsItem = new MenuItem("Colors…");
        colorsItem.setOnAction(e -> {
            handler.runScriptFromResource("/scripts/color_editor.ebs", "Color Editor");
        });

        toolsMenu.getItems().addAll(aiSetupItem, safeDirsItem, dbConfigItem, colorsItem);
        getMenus().add(toolsMenu);

        // --- Tools Menu ---
        Menu devToolsMenu = new Menu("Tools");
        
        MenuItem threadViewerItem = new MenuItem("Thread Viewer…");
        threadViewerItem.setOnAction(e -> {
            ThreadViewerDialog dialog = new ThreadViewerDialog();
            dialog.show();
        });
        
        devToolsMenu.getItems().addAll(threadViewerItem);
        getMenus().add(devToolsMenu);

        // --- Screens Menu ---
        refreshScreensMenu();
        // Refresh screens menu when it's about to be shown
        screensMenu.setOnShowing(e -> refreshScreensMenu());
        getMenus().add(screensMenu);

    }

    public final void refreshRecentMenu() {
        recentMenu.getItems().clear();
        Deque<Path> recentFiles = handler.getRecentFiles();

        if (handler.recentFiles.isEmpty()) {
            MenuItem none = new MenuItem("(No recent files)");
            none.setDisable(true);
            recentMenu.getItems().add(none);
        } else {
            int index = 1;
            for (Path p : recentFiles) {
                // Label with index for quick recognition; avoid overly long labels
                String label = String.format("%d  %s", index, p.toString());
                MenuItem item = new MenuItem(label);

                // Accelerator 1..9 (optional)
                if (index >= 1 && index <= 9) {
                    KeyCode k = KeyCode.getKeyCode(String.valueOf(index));
                    item.setAccelerator(new KeyCodeCombination(k, KeyCombination.CONTROL_DOWN));
                }

                item.setOnAction(e -> handler.openRecent(p));
                recentMenu.getItems().add(item);

                index++;
            }
            recentMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clear = new MenuItem("Clear list");
            clear.setOnAction(e -> {
                recentFiles.clear();
                handler.saveRecentFiles();
                refreshRecentMenu();
            });
            recentMenu.getItems().add(clear);
        }
    }

    public final void refreshScreensMenu() {
        screensMenu.getItems().clear();
        
        // Access global static maps directly - no need to go through interpreter
        try {
            java.util.List<String> screenOrder = com.eb.script.interpreter.InterpreterContext.getGlobalScreenCreationOrder();
            java.util.concurrent.ConcurrentHashMap<String, javafx.stage.Stage> screens = com.eb.script.interpreter.InterpreterContext.getGlobalScreens();
            
            if (screenOrder.isEmpty()) {
                MenuItem none = new MenuItem("(No screens created)");
                none.setDisable(true);
                screensMenu.getItems().add(none);
            } else {
                int index = 1;
                for (String screenName : screenOrder) {
                    javafx.stage.Stage stage = screens.get(screenName);
                    if (stage != null) {
                        // Show screen name and whether it's minimized (● visible, ○ minimized)
                        // Use isIconified() to detect minimized state, and isShowing() to detect if window is open
                        String status;
                        if (!stage.isShowing()) {
                            status = "○"; // Not showing at all
                        } else if (stage.isIconified()) {
                            status = "○"; // Minimized/iconified
                        } else {
                            status = "●"; // Visible and not minimized
                        }
                        String label = String.format("%d  %s %s", index, status, screenName);
                        MenuItem item = new MenuItem(label);
                        
                        // When clicked, bring the screen to front or show it if minimized
                        item.setOnAction(e -> {
                            javafx.application.Platform.runLater(() -> {
                                if (!stage.isShowing()) {
                                    stage.show();
                                }
                                if (stage.isIconified()) {
                                    stage.setIconified(false);
                                }
                                stage.toFront();
                                stage.requestFocus();
                            });
                        });
                        
                        screensMenu.getItems().add(item);
                        index++;
                    }
                }
                
                // Add separator and "Close all screens" option at the bottom
                screensMenu.getItems().add(new SeparatorMenuItem());
                MenuItem closeAllItem = new MenuItem("Close all screens");
                closeAllItem.setOnAction(e -> {
                    javafx.application.Platform.runLater(() -> {
                        // Permanently close all screens (not just hide them)
                        // Create a copy of the list to avoid concurrent modification
                        java.util.List<String> screensToClose = new java.util.ArrayList<>(screenOrder);
                        for (String screenName : screensToClose) {
                            javafx.stage.Stage stage = screens.get(screenName);
                            if (stage != null) {
                                // Fire close request event to trigger cleanup
                                javafx.stage.WindowEvent closeEvent = new javafx.stage.WindowEvent(
                                    stage, 
                                    javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST
                                );
                                stage.fireEvent(closeEvent);
                                // If event wasn't consumed, close the stage
                                if (!closeEvent.isConsumed()) {
                                    stage.close();
                                }
                            }
                        }
                    });
                });
                screensMenu.getItems().add(closeAllItem);
            }
        } catch (Exception e) {
            MenuItem error = new MenuItem("(Error accessing screens: " + e.getMessage() + ")");
            error.setDisable(true);
            screensMenu.getItems().add(error);
            e.printStackTrace(); // Log the error for debugging
        }
    }

}
