package com.eb.script.interpreter.screen;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of variables in a screen definition.
 * Variables are grouped into sets with a name and visibility indicator.
 * 
 * @author Earl
 */
public class VarSet {
    // Name of the variable set
    private String setName;
    
    // Hidden indicator: "Y" = internal access only, "N" = visible
    private String hiddenInd;
    
    // Map of variables in this set, keyed by lowercase varName
    private Map<String, Var> variables;
    
    /**
     * Default constructor
     */
    public VarSet() {
        this.variables = new HashMap<>();
        this.hiddenInd = "N"; // Default to visible
    }
    
    /**
     * Constructor with setName
     * @param setName The name of the variable set
     */
    public VarSet(String setName) {
        this();
        this.setName = setName;
    }
    
    /**
     * Constructor with all fields
     * @param setName The name of the variable set
     * @param hiddenInd Hidden indicator ("Y" or "N")
     */
    public VarSet(String setName, String hiddenInd) {
        this();
        this.setName = setName;
        this.hiddenInd = hiddenInd;
    }
    
    // Getters and setters
    
    public String getSetName() {
        return setName;
    }
    
    public void setSetName(String setName) {
        this.setName = setName;
    }
    
    public String getHiddenInd() {
        return hiddenInd;
    }
    
    public void setHiddenInd(String hiddenInd) {
        this.hiddenInd = hiddenInd;
    }
    
    public Map<String, Var> getVariables() {
        return variables;
    }
    
    public void setVariables(Map<String, Var> variables) {
        this.variables = variables;
    }
    
    /**
     * Add a variable to this set
     * @param var The variable to add
     */
    public void addVariable(Var var) {
        if (var != null && var.getName() != null) {
            this.variables.put(var.getName().toLowerCase(), var);
        }
    }
    
    /**
     * Get a variable from this set by name
     * @param varName The variable name (case-insensitive)
     * @return The variable, or null if not found
     */
    public Var getVariable(String varName) {
        if (varName == null) {
            return null;
        }
        return this.variables.get(varName.toLowerCase());
    }
    
    /**
     * Check if this set is hidden
     * @return true if hiddenInd is "Y", false otherwise
     */
    public boolean isHidden() {
        return "Y".equalsIgnoreCase(hiddenInd);
    }
    
    @Override
    public String toString() {
        return "VarSet{" +
                "setName='" + setName + '\'' +
                ", hiddenInd='" + hiddenInd + '\'' +
                ", variables=" + variables.size() +
                '}';
    }
}
