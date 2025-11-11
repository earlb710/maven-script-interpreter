package com.eb.ui.cli;

import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import java.util.List;
import java.util.function.Consumer;

/**
 * Autocomplete popup for showing keyword and builtin suggestions.
 * Displays a scrollable list of suggestions that can be filtered and selected.
 *
 * @author Earl Bosch
 */
public class AutocompletePopup {

    private final Popup popup;
    private final ListView<String> listView;
    private Consumer<String> onSelect;
    private ScriptArea targetArea;

    public AutocompletePopup() {
        listView = new ListView<>();
        listView.setPrefHeight(200);
        listView.setPrefWidth(300);
        listView.getStyleClass().add("autocomplete-list");

        // Handle selection on Enter or double-click
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                selectCurrentItem();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                selectCurrentItem();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });

        popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(listView);
    }

    /**
     * Show the popup with the given suggestions just underneath the cursor.
     */
    public void show(ScriptArea area, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            hide();
            return;
        }

        this.targetArea = area;
        listView.getItems().setAll(suggestions);
        listView.getSelectionModel().selectFirst();

        if (popup.isShowing()) {
            // Already showing, just update position
            updatePosition(area);
        } else {
            // Show just underneath the cursor
            updatePosition(area);
            popup.show(area, 0, 0); // Initial show, position will be corrected
            updatePosition(area);
            
            // Focus the list view so arrow keys work
            listView.requestFocus();
        }
    }

    /**
     * Update popup position to be just underneath the cursor.
     */
    private void updatePosition(ScriptArea area) {
        Bounds caretBounds = area.getCaretBounds().orElse(null);
        if (caretBounds != null) {
            // getCaretBounds() already returns screen coordinates
            popup.setX(caretBounds.getMinX());
            popup.setY(caretBounds.getMaxY());
        }
    }

    /**
     * Hide the popup.
     */
    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
        targetArea = null;
    }

    /**
     * Check if popup is currently showing.
     */
    public boolean isShowing() {
        return popup.isShowing();
    }

    /**
     * Set the callback for when an item is selected.
     */
    public void setOnSelect(Consumer<String> callback) {
        this.onSelect = callback;
    }

    /**
     * Select the currently highlighted item and invoke callback.
     */
    private void selectCurrentItem() {
        String selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null && onSelect != null) {
            onSelect.accept(selected);
        }
        hide();
    }

    /**
     * Handle key events from the target area to navigate the list.
     * Returns true if the event was handled.
     */
    public boolean handleKeyEvent(KeyEvent event) {
        if (!popup.isShowing()) {
            return false;
        }

        if (event.getCode() == KeyCode.DOWN) {
            listView.getSelectionModel().selectNext();
            event.consume();
            return true;
        } else if (event.getCode() == KeyCode.UP) {
            listView.getSelectionModel().selectPrevious();
            event.consume();
            return true;
        } else if (event.getCode() == KeyCode.ENTER) {
            selectCurrentItem();
            event.consume();
            return true;
        } else if (event.getCode() == KeyCode.ESCAPE) {
            hide();
            event.consume();
            return true;
        }

        return false;
    }
}
