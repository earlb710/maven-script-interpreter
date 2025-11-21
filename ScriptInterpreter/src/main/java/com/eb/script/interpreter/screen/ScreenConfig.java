package com.eb.script.interpreter.screen;

import com.eb.script.token.DataType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScreenConfig stores the configuration for a screen before it is created.
 * This allows lazy initialization where screens are only created when first shown.
 */
public class ScreenConfig {
    
    private final String screenName;
    private final String title;
    private final int width;
    private final int height;
    private final boolean maximize;
    
    // Storage for all screen data that needs to be preserved
    private final ConcurrentHashMap<String, Object> screenVars;
    private final ConcurrentHashMap<String, DataType> screenVarTypes;
    private final Map<String, VarSet> varSets;
    private final Map<String, Var> varItems;
    private final Map<String, AreaItem> areaItems;
    private final List<AreaDefinition> areas;
    
    // Inline code handlers
    private final String startupCode;
    private final String cleanupCode;
    private final String gainFocusCode;
    private final String lostFocusCode;
    
    public ScreenConfig(String screenName, String title, int width, int height, boolean maximize,
                       ConcurrentHashMap<String, Object> screenVars,
                       ConcurrentHashMap<String, DataType> screenVarTypes,
                       Map<String, VarSet> varSets,
                       Map<String, Var> varItems,
                       Map<String, AreaItem> areaItems,
                       List<AreaDefinition> areas,
                       String startupCode, String cleanupCode,
                       String gainFocusCode, String lostFocusCode) {
        this.screenName = screenName;
        this.title = title;
        this.width = width;
        this.height = height;
        this.maximize = maximize;
        this.screenVars = screenVars;
        this.screenVarTypes = screenVarTypes;
        this.varSets = varSets;
        this.varItems = varItems;
        this.areaItems = areaItems;
        this.areas = areas;
        this.startupCode = startupCode;
        this.cleanupCode = cleanupCode;
        this.gainFocusCode = gainFocusCode;
        this.lostFocusCode = lostFocusCode;
    }
    
    // Getters
    public String getScreenName() { return screenName; }
    public String getTitle() { return title; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isMaximize() { return maximize; }
    public ConcurrentHashMap<String, Object> getScreenVars() { return screenVars; }
    public ConcurrentHashMap<String, DataType> getScreenVarTypes() { return screenVarTypes; }
    public Map<String, VarSet> getVarSets() { return varSets; }
    public Map<String, Var> getVarItems() { return varItems; }
    public Map<String, AreaItem> getAreaItems() { return areaItems; }
    public List<AreaDefinition> getAreas() { return areas; }
    public String getStartupCode() { return startupCode; }
    public String getCleanupCode() { return cleanupCode; }
    public String getGainFocusCode() { return gainFocusCode; }
    public String getLostFocusCode() { return lostFocusCode; }
}
