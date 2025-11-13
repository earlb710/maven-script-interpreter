package com.eb.ui.ebs;

import com.eb.script.interpreter.AiFunctions;
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
    public final EbsConsoleHandler handler;

    public EbsMenu(EbsConsoleHandler handler) {
        this.handler = handler;
        recentMenu = new Menu("Recent files");
        handler.loadRecentFiles();
        Menu fileMenu = new Menu("File");

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

        fileMenu.getItems().addAll(openItem, recentMenu, new SeparatorMenuItem(), exitItem);
        getMenus().add(fileMenu);

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

        toolsMenu.getItems().addAll(aiSetupItem, safeDirsItem, dbConfigItem);
        getMenus().add(toolsMenu);

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

}
