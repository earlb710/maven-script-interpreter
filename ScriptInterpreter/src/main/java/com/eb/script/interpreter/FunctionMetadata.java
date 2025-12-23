package com.eb.script.interpreter;

/**
 * Metadata about a declared function including its source, screen association, line number, and character positions.
 * Used to track functions across the interpreter context for validation and debugging.
 *
 * @author Earl Bosch
 */
public class FunctionMetadata {
    
    /** The script/file name where this function is declared */
    private final String scriptName;
    
    /** The screen name this function is associated with, or null if not screen-specific */
    private final String screenName;
    
    /** The line number in the source file where this function is declared */
    private final int lineNumber;
    
    /** The character position where the function name starts in the source file */
    private final int startPosition;
    
    /** The character position where the function name ends in the source file */
    private final int endPosition;
    
    /**
     * Create function metadata with script name only (for regular functions).
     * 
     * @param scriptName The name of the script/file where the function is declared
     * @param lineNumber The line number where the function is declared
     */
    public FunctionMetadata(String scriptName, int lineNumber) {
        this(scriptName, null, lineNumber, -1, -1);
    }
    
    /**
     * Create function metadata with script name and character positions.
     * 
     * @param scriptName The name of the script/file where the function is declared
     * @param lineNumber The line number where the function is declared
     * @param startPosition The character position where the function name starts
     * @param endPosition The character position where the function name ends
     */
    public FunctionMetadata(String scriptName, int lineNumber, int startPosition, int endPosition) {
        this(scriptName, null, lineNumber, startPosition, endPosition);
    }
    
    /**
     * Create function metadata with full information.
     * 
     * @param scriptName The name of the script/file where the function is declared
     * @param screenName The screen name this function is associated with (or null)
     * @param lineNumber The line number where the function is declared
     * @param startPosition The character position where the function name starts
     * @param endPosition The character position where the function name ends
     */
    public FunctionMetadata(String scriptName, String screenName, int lineNumber, int startPosition, int endPosition) {
        this.scriptName = scriptName;
        this.screenName = screenName;
        this.lineNumber = lineNumber;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    /**
     * Get the script/file name where this function is declared.
     * 
     * @return the script name
     */
    public String getScriptName() {
        return scriptName;
    }
    
    /**
     * Get the screen name this function is associated with.
     * 
     * @return the screen name, or null if not screen-specific
     */
    public String getScreenName() {
        return screenName;
    }
    
    /**
     * Get the line number where this function is declared.
     * 
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Get the character position where the function name starts.
     * 
     * @return the start position, or -1 if not available
     */
    public int getStartPosition() {
        return startPosition;
    }
    
    /**
     * Get the character position where the function name ends.
     * 
     * @return the end position, or -1 if not available
     */
    public int getEndPosition() {
        return endPosition;
    }
    
    /**
     * Check if this function is associated with a screen.
     * 
     * @return true if screenName is not null
     */
    public boolean hasScreenName() {
        return screenName != null && !screenName.isEmpty();
    }
    
    /**
     * Check if this function has character position information.
     * 
     * @return true if both start and end positions are available
     */
    public boolean hasPositionInfo() {
        return startPosition >= 0 && endPosition >= 0;
    }
    
    @Override
    public String toString() {
        if (hasScreenName()) {
            return scriptName + " (screen: " + screenName + ", line: " + lineNumber + ")";
        }
        return scriptName + " (line: " + lineNumber + ")";
    }
}
