package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;
import com.eb.script.interpreter.plugin.EbsFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in functions for loading and calling external Java plugins.
 * 
 * <p>This class provides the runtime support for the plugin system, allowing
 * EBS scripts to load external Java classes that implement the {@link EbsFunction}
 * interface and call them at runtime using the {@code #custom.functionName(...)} syntax.</p>
 * 
 * <h2>Plugin Management Functions</h2>
 * <ul>
 *   <li>{@code plugin.load(className, alias, config?)} - Load a Java class as a plugin</li>
 *   <li>{@code plugin.isLoaded(alias)} - Check if a plugin is loaded</li>
 *   <li>{@code plugin.unload(alias)} - Unload a plugin</li>
 *   <li>{@code plugin.list()} - List all loaded plugins</li>
 *   <li>{@code plugin.info(alias)} - Get information about a loaded plugin</li>
 * </ul>
 * 
 * <h2>Calling Custom Functions</h2>
 * <p>Once loaded, custom functions are called using {@code #custom.alias(...)} syntax:</p>
 * <pre>
 * var result = #custom.myFunc("arg1", 42);
 * </pre>
 * 
 * <h2>Example Usage in EBS</h2>
 * <pre>
 * // Load a custom function from the classpath
 * #plugin.load("com.example.MyFunction", "myFunc");
 * 
 * // Call the function using #custom.alias syntax
 * var result = #custom.myFunc("argument1", 42);
 * print result;
 * 
 * // List all loaded plugins
 * var plugins = #plugin.list();
 * foreach p in plugins {
 *     print p;
 * }
 * 
 * // Unload when done
 * #plugin.unload("myFunc");
 * </pre>
 * 
 * @author Earl Bosch
 * @since 1.0.4
 */
public class BuiltinsPlugin {
    
    /**
     * Registry of loaded plugins, keyed by alias (lowercase).
     */
    private static final ConcurrentHashMap<String, EbsFunction> LOADED_PLUGINS = new ConcurrentHashMap<>();
    
    /**
     * Registry of plugin class names, keyed by alias (lowercase).
     * Used for info/debugging.
     */
    private static final ConcurrentHashMap<String, String> PLUGIN_CLASS_NAMES = new ConcurrentHashMap<>();
    
    /**
     * Checks if the given builtin name is a Plugin builtin or a custom function call.
     * 
     * <p>Handles both:
     * <ul>
     *   <li>{@code plugin.*} - Plugin management builtins (load, unload, list, info)</li>
     *   <li>{@code custom.*} - Calls to loaded custom functions</li>
     * </ul>
     * 
     * @param name the builtin name (lowercase)
     * @return true if this is a plugin builtin or custom function call
     */
    public static boolean handles(String name) {
        return name.startsWith("plugin.") || name.startsWith("custom.");
    }
    
    /**
     * Dispatch a Plugin builtin or custom function call by name.
     * 
     * <p>Handles both:
     * <ul>
     *   <li>{@code plugin.*} - Plugin management builtins (load, unload, list, info)</li>
     *   <li>{@code custom.*} - Calls to loaded custom functions via #custom.functionName(...)</li>
     * </ul>
     * 
     * @param name Lowercase builtin name (e.g., "plugin.load" or "custom.myfunc")
     * @param args Arguments passed to the builtin or custom function
     * @return Result of the builtin or custom function call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        // Handle custom function calls: #custom.functionName(args...)
        if (name.startsWith("custom.")) {
            String alias = name.substring(7); // Remove "custom." prefix
            return invokeCustomFunction(alias, args);
        }
        
        // Handle plugin management builtins
        return switch (name) {
            case "plugin.load" -> pluginLoad(args);
            case "plugin.isloaded" -> pluginIsLoaded(args);
            case "plugin.unload" -> pluginUnload(args);
            case "plugin.list" -> pluginList();
            case "plugin.info" -> pluginInfo(args);
            default -> throw new InterpreterError("Unknown Plugin builtin: " + name);
        };
    }
    
    /**
     * Invoke a loaded custom function directly.
     * 
     * <p>This is called when using the #custom.functionName(...) syntax.</p>
     * 
     * @param alias the alias of the loaded plugin (function name after "custom.")
     * @param args the arguments to pass to the function
     * @return the result of the function execution
     * @throws InterpreterError if the function is not loaded or execution fails
     */
    private static Object invokeCustomFunction(String alias, Object[] args) throws InterpreterError {
        if (alias == null || alias.isBlank()) {
            throw new InterpreterError("custom: function name cannot be empty");
        }
        
        String normalizedAlias = alias.toLowerCase();
        EbsFunction function = LOADED_PLUGINS.get(normalizedAlias);
        
        if (function == null) {
            throw new InterpreterError("custom." + alias + ": no plugin loaded with this name. " +
                "Use plugin.load(className, \"" + alias + "\") first.");
        }
        
        try {
            return function.execute(args);
        } catch (InterpreterError e) {
            throw e;
        } catch (Exception e) {
            throw new InterpreterError("custom." + alias + ": execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Load a Java class as a plugin function.
     * 
     * <p>Syntax: {@code plugin.load(className, alias, config?)}</p>
     * 
     * @param args [0] className (String), [1] alias (String), [2] config (JSON Map, optional)
     * @return true if loaded successfully
     * @throws InterpreterError if loading fails
     */
    @SuppressWarnings("unchecked")
    private static Object pluginLoad(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("plugin.load: requires at least className and alias arguments");
        }
        
        String className = (String) args[0];
        String alias = (String) args[1];
        Map<String, Object> config = null;
        
        if (className == null || className.isBlank()) {
            throw new InterpreterError("plugin.load: className cannot be null or empty");
        }
        if (alias == null || alias.isBlank()) {
            throw new InterpreterError("plugin.load: alias cannot be null or empty");
        }
        
        // Optional config parameter
        if (args.length > 2 && args[2] != null) {
            if (!(args[2] instanceof Map)) {
                throw new InterpreterError("plugin.load: config must be a JSON object (map)");
            }
            config = (Map<String, Object>) args[2];
        }
        
        String normalizedAlias = alias.toLowerCase();
        
        // Check if already loaded
        if (LOADED_PLUGINS.containsKey(normalizedAlias)) {
            throw new InterpreterError("plugin.load: alias '" + alias + "' is already in use");
        }
        
        try {
            // Load the class
            Class<?> clazz = Class.forName(className);
            
            // Validate it implements EbsFunction
            if (!EbsFunction.class.isAssignableFrom(clazz)) {
                throw new InterpreterError("plugin.load: class '" + className + 
                    "' does not implement EbsFunction interface");
            }
            
            // Create an instance
            EbsFunction function = (EbsFunction) clazz.getDeclaredConstructor().newInstance();
            
            // Initialize the plugin
            function.initialize(config);
            
            // Register the plugin
            LOADED_PLUGINS.put(normalizedAlias, function);
            PLUGIN_CLASS_NAMES.put(normalizedAlias, className);
            
            return true;
        } catch (ClassNotFoundException e) {
            throw new InterpreterError("plugin.load: class not found: " + className + 
                ". Ensure the class is on the classpath.");
        } catch (NoSuchMethodException e) {
            throw new InterpreterError("plugin.load: class '" + className + 
                "' must have a public no-argument constructor");
        } catch (InstantiationException e) {
            throw new InterpreterError("plugin.load: cannot instantiate class '" + className + 
                "': " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new InterpreterError("plugin.load: cannot access class or constructor for '" + 
                className + "': " + e.getMessage());
        } catch (InterpreterError e) {
            throw e;
        } catch (Exception e) {
            throw new InterpreterError("plugin.load: failed to load class '" + className + 
                "': " + e.getMessage());
        }
    }
    
    /**
     * Check if a plugin is loaded.
     * 
     * <p>Syntax: {@code plugin.isLoaded(alias)}</p>
     * 
     * @param args [0] alias (String)
     * @return true if the plugin is loaded, false otherwise
     * @throws InterpreterError if parameters are invalid
     */
    private static Object pluginIsLoaded(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("plugin.isLoaded: requires alias argument");
        }
        
        String alias = (String) args[0];
        if (alias == null) {
            return false;
        }
        
        return LOADED_PLUGINS.containsKey(alias.toLowerCase());
    }
    
    /**
     * Unload a plugin.
     * 
     * <p>Syntax: {@code plugin.unload(alias)}</p>
     * 
     * @param args [0] alias (String)
     * @return true if the plugin was unloaded, false if it wasn't loaded
     * @throws InterpreterError if parameters are invalid
     */
    private static Object pluginUnload(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("plugin.unload: requires alias argument");
        }
        
        String alias = (String) args[0];
        if (alias == null || alias.isBlank()) {
            throw new InterpreterError("plugin.unload: alias cannot be null or empty");
        }
        
        String normalizedAlias = alias.toLowerCase();
        EbsFunction function = LOADED_PLUGINS.remove(normalizedAlias);
        PLUGIN_CLASS_NAMES.remove(normalizedAlias);
        
        if (function != null) {
            try {
                function.cleanup();
            } catch (Exception e) {
                // Log but don't fail - cleanup is best-effort
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * List all loaded plugins.
     * 
     * <p>Syntax: {@code plugin.list()}</p>
     * 
     * @return a list of loaded plugin aliases
     */
    private static Object pluginList() {
        return new ArrayList<>(LOADED_PLUGINS.keySet());
    }
    
    /**
     * Get information about a loaded plugin.
     * 
     * <p>Syntax: {@code plugin.info(alias)}</p>
     * 
     * @param args [0] alias (String)
     * @return a JSON object with plugin information, or null if not loaded
     * @throws InterpreterError if parameters are invalid
     */
    private static Object pluginInfo(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("plugin.info: requires alias argument");
        }
        
        String alias = (String) args[0];
        if (alias == null || alias.isBlank()) {
            throw new InterpreterError("plugin.info: alias cannot be null or empty");
        }
        
        String normalizedAlias = alias.toLowerCase();
        EbsFunction function = LOADED_PLUGINS.get(normalizedAlias);
        
        if (function == null) {
            return null;
        }
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("alias", normalizedAlias);
        info.put("className", PLUGIN_CLASS_NAMES.get(normalizedAlias));
        info.put("name", function.getName());
        info.put("description", function.getDescription());
        
        // Include signature if provided by the plugin
        Map<String, Object> signature = function.getSignature();
        if (signature != null) {
            info.put("signature", signature);
        }
        
        return info;
    }
    
    /**
     * Clear all loaded plugins. Called during interpreter cleanup.
     */
    public static void clearAllPlugins() {
        for (EbsFunction function : LOADED_PLUGINS.values()) {
            try {
                function.cleanup();
            } catch (Exception e) {
                // Ignore cleanup errors during shutdown
            }
        }
        LOADED_PLUGINS.clear();
        PLUGIN_CLASS_NAMES.clear();
    }
}
