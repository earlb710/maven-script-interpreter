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
    
    // Scope: "internal" = internal access only, "visible" = visible (default)
    // Legacy values: "Y" = internal, "N" = visible (for backward compatibility)
    private String scope;
    
    // Map of variables in this set, keyed by lowercase varName
    private Map<String, Var> variables;
    
    /**
     * Default constructor
     */
    public VarSet() {
        this.variables = new HashMap<>();
        this.scope = "visible"; // Default to visible
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
     * @param scope Scope indicator ("internal", "visible", or legacy "Y"/"N")
     */
    public VarSet(String setName, String scope) {
        this();
        this.setName = setName;
        this.scope = normalizeScope(scope);
    }
    
    // Getters and setters
    
    public String getSetName() {
        return setName;
    }
    
    public void setSetName(String setName) {
        this.setName = setName;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = normalizeScope(scope);
    }
    
    /**
     * Normalize scope value to handle legacy "Y"/"N" values
     * @param scope The scope value (can be "internal", "visible", "Y", or "N")
     * @return Normalized scope ("internal" or "visible")
     */
    private String normalizeScope(String scope) {
        if (scope == null) {
            return "visible";
        }
        // Handle legacy values
        if ("Y".equalsIgnoreCase(scope)) {
            return "internal";
        } else if ("N".equalsIgnoreCase(scope)) {
            return "visible";
        }
        // Return as-is for new values
        return scope.toLowerCase();
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
     * Check if this set is internal (hidden from UI)
     * @return true if scope is "internal", false otherwise
     */
    public boolean isInternal() {
        return "internal".equalsIgnoreCase(scope);
    }
    
    /**
     * Check if this set is hidden (legacy method for backward compatibility)
     * @return true if scope is "internal", false otherwise
     * @deprecated Use isInternal() instead
     */
    @Deprecated
    public boolean isHidden() {
        return isInternal();
    }
    
    @Override
    public String toString() {
        return "VarSet{" +
                "setName='" + setName + '\'' +
                ", scope='" + scope + '\'' +
                ", variables=" + variables.size() +
                '}';
    }
}
