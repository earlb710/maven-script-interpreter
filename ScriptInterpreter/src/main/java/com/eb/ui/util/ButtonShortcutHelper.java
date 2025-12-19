package com.eb.ui.util;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Utility class for adding keyboard shortcuts to buttons.
 * 
 * Features:
 * - Automatically finds a matching character in the button label
 * - Underlines the shortcut character in the label
 * - Adds shortcut information to the button's tooltip
 * - Supports both Alt+key and Ctrl+key combinations
 * 
 * Example usage:
 * <pre>
 * Button saveBtn = new Button("Save");
 * ButtonShortcutHelper.addAltShortcut(saveBtn, KeyCode.S); // Alt+S
 * 
 * Button closeBtn = new Button("Close");
 * ButtonShortcutHelper.addAltShortcut(closeBtn, KeyCode.C); // Alt+C
 * </pre>
 */
public class ButtonShortcutHelper {
    
    /**
     * Add a keyboard shortcut to a button with Alt modifier.
     * This method will underline the matching character in the button label
     * and add the shortcut information to the tooltip.
     * 
     * @param button The button to enhance
     * @param keyCode The key code for the shortcut (e.g., KeyCode.S)
     */
    public static void addAltShortcut(Button button, KeyCode keyCode) {
        addShortcut(button, keyCode, true, false);
    }
    
    /**
     * Add a keyboard shortcut to a button with Ctrl modifier.
     * This method will underline the matching character in the button label
     * and add the shortcut information to the tooltip.
     * 
     * @param button The button to enhance
     * @param keyCode The key code for the shortcut (e.g., KeyCode.S)
     */
    public static void addCtrlShortcut(Button button, KeyCode keyCode) {
        addShortcut(button, keyCode, false, true);
    }
    
    /**
     * Add a keyboard shortcut to a button.
     * This method will underline the matching character in the button label
     * and add the shortcut information to the tooltip.
     * 
     * @param button The button to enhance
     * @param keyCode The key code for the shortcut (e.g., KeyCode.S)
     * @param useAlt Whether to use Alt modifier
     * @param useCtrl Whether to use Ctrl modifier
     */
    public static void addShortcut(Button button, KeyCode keyCode, boolean useAlt, boolean useCtrl) {
        if (button == null || keyCode == null) {
            return;
        }
        
        String buttonText = button.getText();
        if (buttonText == null || buttonText.isEmpty()) {
            return;
        }
        
        // Get the character to match from the key code
        String keyChar = keyCode.getName();
        if (keyChar == null || keyChar.isEmpty()) {
            return;
        }
        
        // Find the character in the button text (case-insensitive)
        int charIndex = findCharacterIndex(buttonText, keyChar);
        
        // Use JavaFX mnemonic parsing to underline the character
        if (charIndex >= 0) {
            // Insert underscore before the character to create a mnemonic
            String mnemonicText = buttonText.substring(0, charIndex) + "_" + buttonText.substring(charIndex);
            button.setText(mnemonicText);
            button.setMnemonicParsing(true);
        }
        
        // Update or create tooltip with shortcut information
        updateTooltip(button, keyCode, useAlt, useCtrl);
        
        // Store reference to event handler so it can be removed when scene changes
        final javafx.event.EventHandler<KeyEvent>[] handlerRef = new javafx.event.EventHandler[1];
        
        // Create a helper method to add the event filter to a scene
        final Runnable addEventFilter = () -> {
            javafx.scene.Scene scene = button.getScene();
            if (scene != null) {
                // Create and store event filter to handle the keyboard shortcut
                handlerRef[0] = event -> {
                    boolean modifierMatch = (useAlt && event.isAltDown() && !event.isControlDown()) ||
                                          (useCtrl && event.isControlDown() && !event.isAltDown()) ||
                                          (useAlt && useCtrl && event.isAltDown() && event.isControlDown());
                    
                    if (modifierMatch && event.getCode() == keyCode) {
                        // Check if button is visible, managed, and not disabled
                        if (button.isVisible() && button.isManaged() && !button.isDisabled()) {
                            button.fire();
                            event.consume();
                        }
                    }
                };
                scene.addEventFilter(KeyEvent.KEY_PRESSED, handlerRef[0]);
            }
        };
        
        // If button already has a scene, add the event filter immediately
        if (button.getScene() != null) {
            addEventFilter.run();
        }
        
        // Add keyboard event handler to the button's scene
        button.sceneProperty().addListener((obs, oldScene, newScene) -> {
            // Remove old handler if it exists
            if (oldScene != null && handlerRef[0] != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, handlerRef[0]);
                handlerRef[0] = null;
            }
            
            if (newScene != null) {
                addEventFilter.run();
            }
        });
    }
    
    /**
     * Find the index of the first occurrence of the character in the text (case-insensitive).
     * Prefers uppercase letters, then lowercase, then any occurrence.
     * Ignores special characters and whitespace.
     * 
     * @param text The text to search in
     * @param keyChar The character to find (from KeyCode.getName())
     * @return The index of the character, or -1 if not found
     */
    private static int findCharacterIndex(String text, String keyChar) {
        if (text == null || keyChar == null || keyChar.isEmpty()) {
            return -1;
        }
        
        char searchChar = keyChar.charAt(0);
        
        // First, try to find uppercase version
        int index = text.indexOf(Character.toUpperCase(searchChar));
        if (index >= 0) {
            return index;
        }
        
        // Then try lowercase
        index = text.indexOf(Character.toLowerCase(searchChar));
        if (index >= 0) {
            return index;
        }
        
        return -1;
    }
    

    
    /**
     * Update or create the button's tooltip to include shortcut information.
     * 
     * @param button The button to update
     * @param keyCode The key code of the shortcut
     * @param useAlt Whether Alt modifier is used
     * @param useCtrl Whether Ctrl modifier is used
     */
    private static void updateTooltip(Button button, KeyCode keyCode, boolean useAlt, boolean useCtrl) {
        // Build shortcut text
        StringBuilder shortcutText = new StringBuilder("Shortcut: ");
        if (useAlt) {
            shortcutText.append("Alt+");
        }
        if (useCtrl) {
            shortcutText.append("Ctrl+");
        }
        shortcutText.append(keyCode.getName());
        
        // Get existing tooltip or create new one
        Tooltip tooltip = button.getTooltip();
        if (tooltip == null) {
            tooltip = new Tooltip(shortcutText.toString());
            button.setTooltip(tooltip);
        } else {
            String existingText = tooltip.getText();
            if (existingText == null || existingText.isEmpty()) {
                tooltip.setText(shortcutText.toString());
            } else {
                // Append shortcut info if not already present
                if (!existingText.toLowerCase().contains("shortcut")) {
                    tooltip.setText(existingText + "\n" + shortcutText);
                }
            }
        }
    }
    
    /**
     * Create a button with an Alt+key shortcut.
     * This is a convenience method that creates and configures a button in one call.
     * 
     * @param text The button text
     * @param keyCode The key code for the shortcut
     * @return A configured button with the shortcut
     */
    public static Button createButtonWithAltShortcut(String text, KeyCode keyCode) {
        Button button = new Button(text);
        addAltShortcut(button, keyCode);
        return button;
    }
    
    /**
     * Create a button with a Ctrl+key shortcut.
     * This is a convenience method that creates and configures a button in one call.
     * 
     * @param text The button text
     * @param keyCode The key code for the shortcut
     * @return A configured button with the shortcut
     */
    public static Button createButtonWithCtrlShortcut(String text, KeyCode keyCode) {
        Button button = new Button(text);
        addCtrlShortcut(button, keyCode);
        return button;
    }
}
