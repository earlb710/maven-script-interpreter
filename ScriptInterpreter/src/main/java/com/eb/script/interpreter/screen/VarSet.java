package com.eb.script.interpreter.screen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a set of variables in a screen definition.
 * Variables are grouped into sets with a name and visibility indicator.
 * Uses ConcurrentHashMap for thread-safe access from screen threads.
 * 
 * @author Earl
 */
public class VarSet {
    // Name of the variable set
    private String setName;
    
    // Scope: Indicates visibility and parameter direction
    // Values: "visible" (default), "internal", "in"/"parameterIn", "out"/"parameterOut", "inout"
    private String scope;
    
    // Map of variables in this set, keyed by lowercase varName
    // Uses ConcurrentHashMap for thread-safe access
    private Map<String, Var> variables;
    
    /**
     * Default constructor
     */
    public VarSet() {
        this.variables = new ConcurrentHashMap<>();
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
     * @param scope Scope indicator (e.g., "internal", "visible", "in", "out", "inout")
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
     * Normalize scope value to handle aliases
     * @param scope The scope value
     * @return Normalized scope value
     */
    private String normalizeScope(String scope) {
        if (scope == null) {
            return "visible";
        }
        String normalized = scope.toLowerCase();
        
        // Handle parameter direction aliases
        if ("parameterin".equals(normalized)) {
            return "in";
        } else if ("parameterout".equals(normalized)) {
            return "out";
        }
        
        // Valid values: visible, internal, in, out, inout
        return normalized;
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
     * Check if this set is for input parameters
     * @return true if scope is "in" or "inout"
     */
    public boolean isInput() {
        return "in".equalsIgnoreCase(scope) || "inout".equalsIgnoreCase(scope);
    }
    
    /**
     * Check if this set is for output parameters
     * @return true if scope is "out" or "inout"
     */
    public boolean isOutput() {
        return "out".equalsIgnoreCase(scope) || "inout".equalsIgnoreCase(scope);
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
