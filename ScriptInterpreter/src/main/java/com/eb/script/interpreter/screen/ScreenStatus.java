package com.eb.script.interpreter.screen;

/**
 * Enum representing the status of a screen.
 * A screen can be in one of three states:
 * - CLEAN: No changes made, no errors
 * - CHANGED: User has made changes that haven't been saved
 * - ERROR: Screen has encountered an error
 */
public enum ScreenStatus {
    /**
     * Screen is clean - no changes, no errors
     */
    CLEAN,
    
    /**
     * Screen has unsaved changes
     */
    CHANGED,
    
    /**
     * Screen has an error condition
     */
    ERROR;
    
    /**
     * Parse a string to ScreenStatus enum
     * @param status The status string (case-insensitive)
     * @return The ScreenStatus enum value, or CLEAN if invalid
     */
    public static ScreenStatus fromString(String status) {
        if (status == null || status.isEmpty()) {
            return CLEAN;
        }
        
        try {
            return ScreenStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CLEAN; // Default to CLEAN if invalid
        }
    }
    
    /**
     * Convert to lowercase string for EBS scripts
     * @return The lowercase status string
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
