package com.eb.ui.ebs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * A status bar component with three sections:
 * 1. Status section - shows current status (e.g., "Running", blank when idle)
 * 2. Message section - shows last error or informational message (with tooltip support)
 * 3. Custom section - reserved for future custom content
 */
public class StatusBar extends HBox {
    
    private final Label statusLabel;
    private final Label messageLabel;
    private final Label customLabel;
    
    public StatusBar() {
        super(5); // spacing between elements
        
        // Initialize labels
        statusLabel = new Label();
        messageLabel = new Label();
        customLabel = new Label();
        
        // Configure status label (first, left-aligned, fixed width)
        statusLabel.setMinWidth(100);
        statusLabel.setPrefWidth(100);
        statusLabel.setMaxWidth(100);
        statusLabel.setAlignment(Pos.CENTER_LEFT);
        
        // Configure message label (second, left-aligned, flexible width - uses 50%+ of space)
        // No min width constraint to allow natural stretching based on Priority.ALWAYS
        messageLabel.setAlignment(Pos.CENTER_LEFT);
        messageLabel.setMaxWidth(Double.MAX_VALUE); // Allow unlimited expansion
        HBox.setHgrow(messageLabel, Priority.ALWAYS); // Auto-stretch to fill available space
        
        // Configure custom label (third, right-aligned, fixed width)
        customLabel.setMinWidth(100);
        customLabel.setPrefWidth(100);
        customLabel.setMaxWidth(100);
        customLabel.setAlignment(Pos.CENTER_RIGHT);
        
        // Create separators
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // Add all components in order: Status | Message | Custom
        getChildren().addAll(
            statusLabel,
            sep1,
            messageLabel,
            sep2,
            customLabel
        );
        
        // Style the status bar
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(2, 5, 2, 5));
        getStyleClass().add("status-bar");
        
        // Set initial state
        clearStatus();
        clearMessage();
        clearCustom();
    }
    
    /**
     * Set the status text (e.g., "Running", "Ready", etc.)
     * @param status The status text to display
     */
    public void setStatus(String status) {
        statusLabel.setText(status != null ? status : "");
    }
    
    /**
     * Clear the status text (shows blank)
     */
    public void clearStatus() {
        statusLabel.setText("");
    }
    
    /**
     * Set the message text (e.g., error messages, info)
     * @param message The message text to display
     */
    public void setMessage(String message) {
        messageLabel.setText(message != null ? message : "");
    }
    
    /**
     * Set the message text with a tooltip for longer text
     * @param message The message text to display
     * @param tooltipText The full text to show in tooltip
     */
    public void setMessage(String message, String tooltipText) {
        setMessage(message);
        if (tooltipText != null && !tooltipText.isEmpty()) {
            messageLabel.setTooltip(new Tooltip(tooltipText));
        } else {
            messageLabel.setTooltip(null);
        }
    }
    
    /**
     * Clear the message text
     */
    public void clearMessage() {
        messageLabel.setText("");
        messageLabel.setTooltip(null);
    }
    
    /**
     * Set the custom text (for future use)
     * @param text The custom text to display
     */
    public void setCustom(String text) {
        customLabel.setText(text != null ? text : "");
    }
    
    /**
     * Clear the custom text
     */
    public void clearCustom() {
        customLabel.setText("");
    }
    
    /**
     * Get the status label for direct manipulation if needed
     * @return The status label
     */
    public Label getStatusLabel() {
        return statusLabel;
    }
    
    /**
     * Get the message label for direct manipulation if needed
     * @return The message label
     */
    public Label getMessageLabel() {
        return messageLabel;
    }
    
    /**
     * Get the custom label for direct manipulation if needed
     * @return The custom label
     */
    public Label getCustomLabel() {
        return customLabel;
    }
}
