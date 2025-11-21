package com.eb.ui.ebs;

import com.eb.script.json.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     */
    private void loadColors() {
        if (config == null) {
            return;
        }
        
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
        defaultColors.put("keyword", "#00FFFF");
        defaultColors.put("builtin", "#99e0e0");
        defaultColors.put("literal", "blue");
        defaultColors.put("identifier", "white");
        defaultColors.put("sql", "#00ee66");
        defaultColors.put("custom", "#eeee90");
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
                css.append(".tab-pane .tab.changed .tab-label {\n");
                css.append("    -fx-text-fill: ").append(color).append(" !important;\n");
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
        
        return css.toString();
    }
    
    /**
     * Check if the configuration was loaded successfully from file.
     * @return true if loaded from file, false if using defaults
     */
    public boolean isLoadedFromFile() {
        return loadedFromFile;
    }
}
