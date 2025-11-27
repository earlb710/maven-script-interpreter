package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.interpreter.InterpreterError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Built-in functions for CSS operations.
 * Handles all css.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsCss {

    // Cache for parsed CSS files to avoid re-reading and re-parsing
    private static final Map<String, Map<String, Map<String, String>>> CSS_CACHE = new HashMap<>();

    /**
     * Dispatch a CSS builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "css.getvalue")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "css.getvalue" -> getValue(args);
            case "css.findcss" -> findCss(args);
            default -> throw new InterpreterError("Unknown CSS builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a CSS builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("css.");
    }

    /**
     * css.getValue(cssPath, selector, property) -> STRING
     * Retrieves a CSS property value from a stylesheet.
     * 
     * @param args args[0] = cssPath (String) - path to CSS file or resource
     *             args[1] = selector (String) - CSS selector (e.g., ".error", "#main", "body")
     *             args[2] = property (String) - CSS property name (e.g., "-fx-fill", "color")
     * @return The property value as a string, or null if not found
     */
    private static Object getValue(Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("css.getValue requires 3 arguments: cssPath, selector, property");
        }

        String cssPath = (String) args[0];
        String selector = (String) args[1];
        String property = (String) args[2];

        if (cssPath == null || cssPath.isBlank()) {
            throw new InterpreterError("css.getValue: cssPath cannot be null or empty");
        }
        if (selector == null || selector.isBlank()) {
            throw new InterpreterError("css.getValue: selector cannot be null or empty");
        }
        if (property == null || property.isBlank()) {
            throw new InterpreterError("css.getValue: property cannot be null or empty");
        }

        try {
            // Parse CSS and get the value
            Map<String, Map<String, String>> cssRules = parseCss(cssPath);
            
            // Look up the selector
            Map<String, String> properties = cssRules.get(selector.trim());
            if (properties == null) {
                return null;
            }

            // Look up the property (case-insensitive)
            String normalizedProperty = property.trim().toLowerCase();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getKey().toLowerCase().equals(normalizedProperty)) {
                    return entry.getValue();
                }
            }

            return null;
        } catch (IOException e) {
            throw new InterpreterError("css.getValue: Failed to read CSS file: " + e.getMessage());
        }
    }

    /**
     * css.findCss(searchPath?) -> STRING[]
     * Searches for all available CSS stylesheet files and returns their paths.
     * 
     * @param args args[0] = searchPath (String, optional) - base path to search in. 
     *             If null/empty, searches in default locations (classpath css/ folder and sandbox)
     * @return ArrayDynamic of strings containing all found CSS file paths
     */
    private static Object findCss(Object[] args) throws InterpreterError {
        String searchPath = null;
        if (args.length > 0 && args[0] != null) {
            searchPath = (String) args[0];
        }

        List<String> cssFiles = new ArrayList<>();

        try {
            // Search in classpath resources (css/ folder)
            findCssInClasspath(cssFiles);

            // Search in filesystem if a path is provided
            if (searchPath != null && !searchPath.isBlank()) {
                findCssInFilesystem(cssFiles, searchPath);
            } else {
                // Search in sandbox root if no specific path provided
                try {
                    Path sandboxRoot = com.eb.util.Util.SANDBOX_ROOT;
                    if (Files.exists(sandboxRoot)) {
                        findCssInFilesystem(cssFiles, sandboxRoot.toString());
                    }
                } catch (Exception e) {
                    // Ignore sandbox errors
                }
            }
        } catch (Exception e) {
            throw new InterpreterError("css.findCss: Error searching for CSS files: " + e.getMessage());
        }

        // Convert to ArrayDynamic
        ArrayDynamic result = new ArrayDynamic(com.eb.script.token.DataType.STRING);
        for (String cssFile : cssFiles) {
            result.add(cssFile);
        }

        return result;
    }

    /**
     * Search for CSS files in classpath resources.
     */
    private static void findCssInClasspath(List<String> cssFiles) {
        try {
            // Get the css resource folder URL
            URL cssUrl = BuiltinsCss.class.getResource("/css");
            if (cssUrl == null) {
                return;
            }

            URI uri = cssUrl.toURI();
            Path cssPath;
            
            if (uri.getScheme().equals("jar")) {
                // Running from JAR file
                FileSystem fileSystem = null;
                try {
                    try {
                        fileSystem = FileSystems.getFileSystem(uri);
                    } catch (Exception e) {
                        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    }
                    cssPath = fileSystem.getPath("/css");
                    if (Files.exists(cssPath)) {
                        try (Stream<Path> walk = Files.walk(cssPath, 1)) {
                            walk.filter(p -> p.toString().endsWith(".css"))
                                .forEach(p -> {
                                    String fileName = p.getFileName().toString();
                                    cssFiles.add("css/" + fileName);
                                });
                        }
                    }
                } catch (Exception e) {
                    // Ignore JAR access errors
                }
            } else {
                // Running from filesystem (IDE or exploded classes)
                cssPath = Paths.get(uri);
                if (Files.exists(cssPath) && Files.isDirectory(cssPath)) {
                    try (Stream<Path> walk = Files.walk(cssPath, 1)) {
                        walk.filter(p -> p.toString().endsWith(".css"))
                            .forEach(p -> {
                                String fileName = p.getFileName().toString();
                                cssFiles.add("css/" + fileName);
                            });
                    }
                }
            }
        } catch (Exception e) {
            // Ignore classpath search errors
        }
    }

    /**
     * Search for CSS files in the filesystem.
     */
    private static void findCssInFilesystem(List<String> cssFiles, String basePath) {
        try {
            Path initialPath = Path.of(basePath);
            Path searchPath;
            
            // Try resolving with sandbox if it's a relative path
            if (!initialPath.isAbsolute()) {
                try {
                    searchPath = com.eb.util.Util.resolveSandboxedPath(basePath);
                } catch (Exception e) {
                    // Use the original path if sandbox resolution fails
                    searchPath = initialPath;
                }
            } else {
                searchPath = initialPath;
            }
            
            if (!Files.exists(searchPath)) {
                return;
            }

            final Path finalSearchPath = searchPath;
            if (Files.isDirectory(searchPath)) {
                // Search recursively in directory
                try (Stream<Path> walk = Files.walk(searchPath)) {
                    walk.filter(p -> p.toString().endsWith(".css"))
                        .forEach(p -> {
                            String pathStr = p.toString();
                            if (!cssFiles.contains(pathStr)) {
                                cssFiles.add(pathStr);
                            }
                        });
                }
            } else if (searchPath.toString().endsWith(".css")) {
                // Single file
                String pathStr = searchPath.toString();
                if (!cssFiles.contains(pathStr)) {
                    cssFiles.add(pathStr);
                }
            }
        } catch (Exception e) {
            // Ignore filesystem errors for individual paths
        }
    }

    /**
     * Parse a CSS file and return a map of selectors to properties.
     * Uses caching to avoid re-parsing the same file multiple times.
     */
    private static Map<String, Map<String, String>> parseCss(String cssPath) throws IOException, InterpreterError {
        // Check cache first
        if (CSS_CACHE.containsKey(cssPath)) {
            return CSS_CACHE.get(cssPath);
        }

        String cssContent = readCssContent(cssPath);
        Map<String, Map<String, String>> rules = parseCssContent(cssContent);

        // Cache the result
        CSS_CACHE.put(cssPath, rules);

        return rules;
    }

    /**
     * Read CSS content from a file path or classpath resource.
     */
    private static String readCssContent(String cssPath) throws IOException, InterpreterError {
        // First try as a classpath resource (for resources in css/ folder)
        String resourcePath = cssPath;
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }
        if (!resourcePath.startsWith("/css/") && !resourcePath.contains("/")) {
            // Try adding css/ prefix for simple filenames
            resourcePath = "/css/" + cssPath;
        }

        InputStream is = BuiltinsCss.class.getResourceAsStream(resourcePath);
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }

        // Also try without the /css/ prefix
        if (!cssPath.startsWith("/")) {
            is = BuiltinsCss.class.getResourceAsStream("/" + cssPath);
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            }
        }

        // Try as a file path
        Path filePath = Path.of(cssPath);
        if (Files.exists(filePath)) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }

        // Try resolving with sandbox
        try {
            Path sandboxedPath = com.eb.util.Util.resolveSandboxedPath(cssPath);
            if (Files.exists(sandboxedPath)) {
                return Files.readString(sandboxedPath, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Ignore and throw file not found error below
        }

        throw new InterpreterError("css.getValue: CSS file not found: " + cssPath);
    }

    /**
     * Parse CSS content and extract selectors with their properties.
     * Handles basic CSS syntax including multi-line rules and comments.
     * Note: This parser is designed for JavaFX CSS files and may not fully support
     * advanced CSS features like nested @media queries or @keyframes.
     */
    private static Map<String, Map<String, String>> parseCssContent(String cssContent) {
        Map<String, Map<String, String>> rules = new HashMap<>();

        // Remove CSS comments
        cssContent = removeComments(cssContent);

        // Pattern to match CSS rules: selector(s) { properties }
        // This handles multi-selector rules like ".a, .b { ... }"
        // Note: Does not support nested rules (e.g., @media queries with nested selectors)
        Pattern rulePattern = Pattern.compile("([^{}]+)\\s*\\{([^{}]*)\\}", Pattern.MULTILINE);
        Matcher ruleMatcher = rulePattern.matcher(cssContent);

        while (ruleMatcher.find()) {
            String selectorGroup = ruleMatcher.group(1).trim();
            String propertiesBlock = ruleMatcher.group(2).trim();
            
            // Skip at-rules (e.g., @media, @keyframes) as they are not simple selectors
            if (selectorGroup.startsWith("@")) {
                continue;
            }

            // Parse properties
            Map<String, String> properties = parseProperties(propertiesBlock);

            // Handle multiple selectors separated by commas
            String[] selectors = selectorGroup.split(",");
            for (String selector : selectors) {
                selector = selector.trim();
                if (!selector.isEmpty()) {
                    // Merge with existing properties if selector already exists
                    if (rules.containsKey(selector)) {
                        rules.get(selector).putAll(properties);
                    } else {
                        rules.put(selector, new HashMap<>(properties));
                    }
                }
            }
        }

        return rules;
    }

    /**
     * Remove CSS comments from content.
     * Uses a simple regex that handles most comment formats including multi-line comments.
     */
    private static String removeComments(String cssContent) {
        // Remove /* ... */ comments using DOTALL flag for multi-line support
        // The .*? makes it non-greedy to handle multiple comments correctly
        return cssContent.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    /**
     * Parse CSS properties from a properties block.
     * Handles properties like: -fx-fill: red; color: blue;
     * Also handles values with semicolons inside strings.
     */
    private static Map<String, String> parseProperties(String propertiesBlock) {
        Map<String, String> properties = new HashMap<>();
        
        // Parse character by character to handle semicolons inside quoted strings
        StringBuilder currentDeclaration = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = 0; i < propertiesBlock.length(); i++) {
            char c = propertiesBlock.charAt(i);
            
            // Track quote state
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            
            // Semicolon ends a declaration only if not inside quotes
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                addDeclaration(properties, currentDeclaration.toString());
                currentDeclaration.setLength(0);
            } else {
                currentDeclaration.append(c);
            }
        }
        
        // Add the last declaration (may not end with semicolon)
        if (currentDeclaration.length() > 0) {
            addDeclaration(properties, currentDeclaration.toString());
        }

        return properties;
    }
    
    /**
     * Add a declaration (property: value) to the properties map.
     */
    private static void addDeclaration(Map<String, String> properties, String declaration) {
        declaration = declaration.trim();
        if (declaration.isEmpty()) {
            return;
        }
        
        // Find the first colon (property: value)
        int colonIndex = declaration.indexOf(':');
        if (colonIndex > 0) {
            String propertyName = declaration.substring(0, colonIndex).trim();
            String propertyValue = declaration.substring(colonIndex + 1).trim();

            if (!propertyName.isEmpty() && !propertyValue.isEmpty()) {
                properties.put(propertyName, propertyValue);
            }
        }
    }

    /**
     * Clear the CSS cache. Useful for reloading stylesheets.
     */
    public static void clearCache() {
        CSS_CACHE.clear();
    }
}
