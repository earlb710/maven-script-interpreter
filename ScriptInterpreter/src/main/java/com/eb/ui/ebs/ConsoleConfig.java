package com.eb.ui.ebs;

import com.eb.script.json.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration loader for console color properties.
 * Loads console.cfg JSON file from the root directory and provides access to color settings.
 * 
 * @author Earl Bosch
 */
public class ConsoleConfig {
    
    private static final String CONFIG_FILE = "console.cfg";
    private final Map<String, Object> config;
    private final Map<String, String> colors;
    private final boolean loadedFromFile;
    
    /**
     * Load console configuration from console.cfg file.
     * Falls back to default configuration if file is missing or invalid.
     */
    public ConsoleConfig() {
        this.colors = new LinkedHashMap<>();
        Map<String, Object> loadedConfig = loadConfigFile();
        
        if (loadedConfig != null) {
            this.config = loadedConfig;
            this.loadedFromFile = true;
            loadColors();
        } else {
            this.config = getDefaultConfig();
            this.loadedFromFile = false;
            loadColors();
        }
    }
    
    /**
     * Load the configuration file from the root directory.
     * Searches in current directory and parent directory.
     * @return Map containing configuration, or null if loading fails
     */
    private Map<String, Object> loadConfigFile() {
        try {
            // Try current directory first
            Path configPath = Paths.get(CONFIG_FILE);
            
            // If not found in current directory, try parent directory
            if (!Files.exists(configPath)) {
                configPath = Paths.get("..", CONFIG_FILE);
            }
            
            // If still not found, report and use defaults
            if (!Files.exists(configPath)) {
                System.out.println("Console config file not found: " + CONFIG_FILE + ", using defaults.");
                System.out.println("Searched in: " + Paths.get("").toAbsolutePath() + " and parent directory");
                return null;
            }
            
            String jsonContent = Files.readString(configPath);
            Object parsed = Json.parse(jsonContent);
            
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) parsed;
                System.out.println("Loaded console configuration from " + CONFIG_FILE);
                return configMap;
            } else {
                System.err.println("Invalid console.cfg format: expected JSON object");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error reading console.cfg: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing console.cfg: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract color properties from the configuration.
     * Supports both the new profile-based structure (with currentProfile, profiles) 
     * and the old flat structure (with colors).
     */
    private void loadColors() {
        if (config == null) {
            return;
        }
        
        // First, try the new profile-based structure
        Object profilesObj = config.get("profiles");
        if (profilesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> profilesMap = (Map<String, Object>) profilesObj;
            
            // Get the current profile name, default to "default" if not specified
            String currentProfile = "default";
            Object currentProfileObj = config.get("currentProfile");
            if (currentProfileObj != null && !currentProfileObj.toString().isEmpty()) {
                currentProfile = currentProfileObj.toString();
            }
            
            // Get the colors from the current profile
            Object profileColorsObj = profilesMap.get(currentProfile);
            if (profileColorsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> colorMap = (Map<String, Object>) profileColorsObj;
                
                for (Map.Entry<String, Object> entry : colorMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        colors.put(key, value.toString());
                    }
                }
                System.out.println("Loaded colors from profile: " + currentProfile);
                return;
            } else {
                // Profile not found, try "default"
                profileColorsObj = profilesMap.get("default");
                if (profileColorsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> colorMap = (Map<String, Object>) profileColorsObj;
                    
                    for (Map.Entry<String, Object> entry : colorMap.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value != null) {
                            colors.put(key, value.toString());
                        }
                    }
                    System.out.println("Profile '" + currentProfile + "' not found, loaded colors from 'default' profile");
                    return;
                }
            }
        }
        
        // Fall back to the old flat structure with "colors" key
        Object colorsObj = config.get("colors");
        if (colorsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> colorMap = (Map<String, Object>) colorsObj;
            
            for (Map.Entry<String, Object> entry : colorMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    colors.put(key, value.toString());
                }
            }
            System.out.println("Loaded colors from legacy 'colors' structure");
        }
    }
    
    /**
     * Get the default configuration.
     * These are the default color values from console.css
     */
    private Map<String, Object> getDefaultConfig() {
        Map<String, Object> defaultConfig = new LinkedHashMap<>();
        Map<String, String> defaultColors = new LinkedHashMap<>();
        
        // Default colors from console.css
        defaultColors.put("info", "#e6e6e6");
        defaultColors.put("comment", "#ffffcc");
        defaultColors.put("error", "#ee0000");
        defaultColors.put("warn", "#eeee00");
        defaultColors.put("ok", "#00ee00");
        defaultColors.put("code", "white");
        defaultColors.put("datatype", "#D070FF");
        defaultColors.put("data", "pink");
        defaultColors.put("keyword", "#569CD6");
        defaultColors.put("builtin", "#DCDCAA");
        defaultColors.put("literal", "blue");
        defaultColors.put("identifier", "white");
        defaultColors.put("sql", "#00ee66");
        defaultColors.put("custom", "#eeee90");
        defaultColors.put("function", "#FFB86C");
        defaultColors.put("jsonkey", "#4EC9B0");
        defaultColors.put("background", "#000000");
        defaultColors.put("text", "#e6e6e6");
        defaultColors.put("caret", "white");
        
        defaultConfig.put("colors", defaultColors);
        return defaultConfig;
    }
    
    /**
     * Get a color value by name.
     * @param name The color property name (e.g., "info", "error", "warn")
     * @return The color value as a string, or null if not found
     */
    public String getColor(String name) {
        return colors.get(name);
    }
    
    /**
     * Get all color properties as a map.
     * @return Map of color name to color value
     */
    public Map<String, String> getAllColors() {
        return new LinkedHashMap<>(colors);
    }
    
    /**
     * Generate CSS content from the loaded configuration.
     * This generates CSS rules that override the default styles.
     * @return CSS content as a string
     */
    public String generateCSS() {
        StringBuilder css = new StringBuilder();
        css.append("/* Generated from console.cfg */\n\n");
        
        for (Map.Entry<String, String> entry : colors.entrySet()) {
            String className = entry.getKey();
            String color = entry.getValue();
            
            // Handle special cases
            if ("background".equals(className)) {
                css.append(".console-frame .text-area,\n");
                css.append(".console-frame .virtualized-scroll-pane,\n");
                css.append(".console-frame .styled-text-area,\n");
                css.append(".console-frame .styled-text-area .content,\n");
                css.append(".console-out,\n");
                css.append(".console-in .text-area,\n");
                css.append(".console-in .styled-text-area,\n");
                css.append(".console-in .styled-text-area .content,\n");
                css.append(".console-in,\n");
                css.append(".editor-ebs,\n");
                css.append(".editor-ebs .styled-text-area .content,\n");
                css.append(".editor-text,\n");
                css.append(".editor-text .styled-text-area .content,\n");
                css.append(".styled-text-area .content,\n");
                css.append(".lineno {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("line-cursor".equals(className)) {
                css.append(".paragraph-box:has-caret {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("line-numbers".equals(className)) {
                css.append(".lineno,\n");
                css.append(".lineno .text,\n");
                css.append(".linenumbers,\n");
                css.append(".linenumbers .text {\n");
                css.append("    -fx-fill: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("text".equals(className)) {
                css.append(".console-frame .styled-text-area,\n");
                css.append(".console-out,\n");
                css.append(".console-in .text-area,\n");
                css.append(".console-in .styled-text-area,\n");
                css.append(".console-in,\n");
                css.append(".editor-ebs,\n");
                css.append(".editor-text {\n");
                css.append("    -fx-text-fill: ").append(color).append(" !important;\n");
                css.append("    -fx-fill: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("caret".equals(className)) {
                css.append(".console-in .caret,\n");
                css.append(".editor-ebs .caret,\n");
                css.append(".editor-text .caret {\n");
                css.append("    -fx-stroke: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("console-background".equals(className)) {
                css.append(".console-frame {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-background".equals(className)) {
                css.append(".tab-pane,\n");
                css.append(".tab-pane .tab-header-area,\n");
                css.append(".tab-pane .tab-header-background {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-label-color".equals(className)) {
                css.append(".tab-pane .tab .tab-label {\n");
                css.append("    -fx-text-fill: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-label-changed-color".equals(className)) {
                css.append(".tab-pane .tab.tab-changed .tab-label {\n");
                css.append("    -fx-text-fill: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-label-background".equals(className)) {
                css.append(".tab-pane .tab {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-select".equals(className)) {
                // Selected tab background
                css.append(".tab-pane .tab:selected {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else if ("tab-content".equals(className)) {
                // Tab content area background using multiple descendant selectors for broader coverage
                css.append(".tab-pane .tab-content-area,\n");
                css.append("#mainTabs .tab-content-area,\n");
                css.append(".viewer-tabs .tab-content-area {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
                // Also style the content elements within tabs to match
                // SplitPane uses .split-pane style class, VBox and HBox need type selectors (case-sensitive)
                css.append("#mainTabs .tab-content-area .split-pane,\n");
                css.append("#mainTabs .tab-content-area VBox,\n");
                css.append("#mainTabs .tab-content-area HBox,\n");
                css.append("#mainTabs .tab-content-area BorderPane,\n");
                css.append(".tab-pane .tab-content-area .split-pane,\n");
                css.append(".tab-pane .tab-content-area VBox,\n");
                css.append(".tab-pane .tab-content-area HBox,\n");
                css.append(".tab-pane .tab-content-area BorderPane {\n");
                css.append("    -fx-background-color: ").append(color).append(" !important;\n");
                css.append("}\n\n");
            } else {
                // Regular style class - need to target both regular and RichTextFX .text nodes
                // Use multiple selectors to cover all possible contexts
                css.append(".").append(className).append(",\n");
                css.append(".text.").append(className).append(",\n");
                css.append(".styled-text-area .text.").append(className).append(",\n");
                css.append(".console-out .text.").append(className).append(",\n");
                css.append(".console-in .text.").append(className).append(",\n");
                css.append(".editor-ebs .text.").append(className).append(",\n");
                css.append(".editor-text .text.").append(className).append(" {\n");
                css.append("    -fx-fill: ").append(color).append(" !important;\n");
                css.append("    -fx-background-color: transparent !important;\n");
                css.append("}\n\n");
            }
        }
        
        // Add syntax highlighting token mappings from console.cfg colors to tok-* classes
        // These map console.cfg color names to the CSS classes used by the syntax highlighter
        if (colors.containsKey("keyword")) {
            String keywordColor = colors.get("keyword");
            css.append("/* Syntax highlighting: keywords (from console.cfg 'keyword' color) */\n");
            css.append(".tok-keyword,\n");
            css.append(".text.tok-keyword,\n");
            css.append(".styled-text-area .text.tok-keyword,\n");
            css.append(".editor-ebs .text.tok-keyword,\n");
            css.append(".editor-text .text.tok-keyword {\n");
            css.append("    -fx-fill: ").append(keywordColor).append(" !important;\n");
            css.append("    -fx-font-weight: bold;\n");
            css.append("}\n\n");
        }
        
        if (colors.containsKey("builtin")) {
            String builtinColor = colors.get("builtin");
            css.append("/* Syntax highlighting: builtins (from console.cfg 'builtin' color) */\n");
            css.append(".tok-builtin,\n");
            css.append(".text.tok-builtin,\n");
            css.append(".styled-text-area .text.tok-builtin,\n");
            css.append(".editor-ebs .text.tok-builtin,\n");
            css.append(".editor-text .text.tok-builtin {\n");
            css.append("    -fx-fill: ").append(builtinColor).append(" !important;\n");
            css.append("}\n\n");
        }
        
        if (colors.containsKey("function")) {
            String functionColor = colors.get("function");
            css.append("/* Syntax highlighting: custom functions (from console.cfg 'function' color) */\n");
            css.append(".tok-custom-function,\n");
            css.append(".text.tok-custom-function,\n");
            css.append(".styled-text-area .text.tok-custom-function,\n");
            css.append(".editor-ebs .text.tok-custom-function,\n");
            css.append(".editor-text .text.tok-custom-function {\n");
            css.append("    -fx-fill: ").append(functionColor).append(" !important;\n");
            css.append("}\n\n");
        }
        
        if (colors.containsKey("jsonkey")) {
            String jsonkeyColor = colors.get("jsonkey");
            css.append("/* Syntax highlighting: JSON keys (from console.cfg 'jsonkey' color) */\n");
            css.append(".tok-jsonkey,\n");
            css.append(".text.tok-jsonkey,\n");
            css.append(".styled-text-area .text.tok-jsonkey,\n");
            css.append(".editor-ebs .text.tok-jsonkey,\n");
            css.append(".editor-text .text.tok-jsonkey {\n");
            css.append("    -fx-fill: ").append(jsonkeyColor).append(" !important;\n");
            css.append("}\n\n");
        }
        
        // Find highlighting styles - these override all other text styles
        // to ensure search matches are readable. Uses configurable colors.
        String findHighlightColor = colors.getOrDefault("find-highlight-color", "#000000");
        String findHighlightBackground = colors.getOrDefault("find-highlight-background", "#ddb107");
        // Current match uses a brighter background color for better visibility
        String findCurrentBackground = colors.getOrDefault("current-find-highlight-bg", "#ffff00");
        
        css.append("/* Find highlighting - overrides syntax highlighting for readability */\n");
        css.append(".find-hit,\n");
        css.append(".text.find-hit,\n");
        css.append(".styled-text-area .text.find-hit,\n");
        css.append(".console-out .text.find-hit,\n");
        css.append(".console-in .text.find-hit,\n");
        css.append(".editor-ebs .text.find-hit,\n");
        css.append(".editor-text .text.find-hit {\n");
        css.append("    -fx-fill: ").append(findHighlightColor).append(" !important;\n");
        css.append("    -rtfx-background-color: ").append(findHighlightBackground).append(" !important;\n");
        css.append("}\n\n");
        
        css.append(".find-current,\n");
        css.append(".text.find-current,\n");
        css.append(".styled-text-area .text.find-current,\n");
        css.append(".console-out .text.find-current,\n");
        css.append(".console-in .text.find-current,\n");
        css.append(".editor-ebs .text.find-current,\n");
        css.append(".editor-text .text.find-current {\n");
        css.append("    -fx-fill: ").append(findHighlightColor).append(" !important;\n");
        css.append("    -fx-font-weight: bold !important;\n");
        css.append("    -rtfx-background-color: ").append(findCurrentBackground).append(" !important;\n");
        css.append("}\n\n");
        
        return css.toString();
    }
    
    /**
     * Check if the configuration was loaded successfully from file.
     * @return true if loaded from file, false if using defaults
     */
    public boolean isLoadedFromFile() {
        return loadedFromFile;
    }
    
    /**
     * Get the raw configuration map.
     * @return Unmodifiable map containing the configuration data
     */
    public Map<String, Object> getConfig() {
        return Collections.unmodifiableMap(config);
    }
}
