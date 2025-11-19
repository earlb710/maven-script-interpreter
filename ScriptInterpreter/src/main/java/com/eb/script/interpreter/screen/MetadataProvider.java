package com.eb.script.interpreter.screen;

/**
 * Provider interface for retrieving display metadata for screen variables.
 * Replaces BiFunction to avoid Java functional interface dependency.
 */
@FunctionalInterface
public interface MetadataProvider {
    
    /**
     * Retrieves display metadata for a variable.
     * 
     * @param screenName The name of the screen
     * @param varName The name of the variable
     * @return The DisplayItem metadata for the variable, or null if not found
     */
    DisplayItem getMetadata(String screenName, String varName);
}
