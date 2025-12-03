package com.eb.script.interpreter.plugin;

/**
 * Interface that external Java classes must implement to be callable from EBS scripts.
 * 
 * <h2>Overview</h2>
 * This interface provides the contract for creating custom Java functions that can be 
 * loaded and called from EBS scripts using the plugin system. External functions allow
 * extending the interpreter with custom functionality without modifying the core codebase.
 * 
 * <h2>Usage from EBS Scripts</h2>
 * <pre>
 * // Load a plugin class (must be on classpath)
 * var result = call plugin.load("com.example.MyFunction", "myFunc");
 * 
 * // Call the loaded function with arguments
 * var output = call plugin.call("myFunc", "arg1", 42, true);
 * 
 * // Check if a plugin is loaded
 * var loaded = call plugin.isLoaded("myFunc");
 * 
 * // Unload a plugin
 * call plugin.unload("myFunc");
 * </pre>
 * 
 * <h2>Implementation Example</h2>
 * <pre>
 * package com.example;
 * 
 * import com.eb.script.interpreter.plugin.EbsFunction;
 * import java.util.Map;
 * 
 * public class MyFunction implements EbsFunction {
 *     
 *     &#64;Override
 *     public String getName() {
 *         return "MyCustomFunction";
 *     }
 *     
 *     &#64;Override
 *     public String getDescription() {
 *         return "Example custom function that processes input";
 *     }
 *     
 *     &#64;Override
 *     public Object execute(Object[] args) throws Exception {
 *         if (args.length == 0) {
 *             return "No arguments provided";
 *         }
 *         // Process arguments and return result
 *         return "Processed: " + args[0].toString();
 *     }
 *     
 *     &#64;Override
 *     public void initialize(Map&lt;String, Object&gt; config) {
 *         // Optional: receive configuration when plugin is loaded
 *     }
 *     
 *     &#64;Override
 *     public void cleanup() {
 *         // Optional: cleanup resources when plugin is unloaded
 *     }
 * }
 * </pre>
 * 
 * <h2>Argument Handling</h2>
 * The {@code args} array passed to {@link #execute(Object[])} contains the evaluated
 * arguments from the EBS script call. Common types include:
 * <ul>
 *   <li>{@code String} - for string literals</li>
 *   <li>{@code Integer}, {@code Long}, {@code Double}, {@code Float} - for numeric values</li>
 *   <li>{@code Boolean} - for boolean values</li>
 *   <li>{@code Map<String, Object>} - for JSON objects</li>
 *   <li>{@code List<Object>} - for JSON arrays</li>
 *   <li>{@code ArrayDef} - for EBS arrays</li>
 * </ul>
 * 
 * <h2>Return Values</h2>
 * The {@link #execute(Object[])} method can return any of the above types, and they
 * will be properly handled by the EBS interpreter.
 * 
 * <h2>Thread Safety</h2>
 * Implementations should be thread-safe if the function may be called from multiple
 * screen threads simultaneously.
 * 
 * @author Earl Bosch
 * @since 1.0.4
 */
public interface EbsFunction {
    
    /**
     * Returns the name of this function for display and logging purposes.
     * This is a descriptive name, not the alias used to call the function.
     * 
     * @return the display name of this function
     */
    String getName();
    
    /**
     * Returns a description of what this function does.
     * Used for help text and documentation.
     * 
     * @return a human-readable description of this function
     */
    String getDescription();
    
    /**
     * Executes the function with the given arguments.
     * 
     * @param args the arguments passed from the EBS script (evaluated values)
     * @return the result of the function execution, or null if no result
     * @throws Exception if the function execution fails
     */
    Object execute(Object[] args) throws Exception;
    
    /**
     * Called when the plugin is loaded. Implementations can use this to
     * initialize resources or receive configuration.
     * 
     * <p>The default implementation does nothing.</p>
     * 
     * @param config optional configuration map (may be null or empty)
     */
    default void initialize(java.util.Map<String, Object> config) {
        // Default: do nothing
    }
    
    /**
     * Called when the plugin is unloaded. Implementations should clean up
     * any resources (close files, connections, etc.).
     * 
     * <p>The default implementation does nothing.</p>
     */
    default void cleanup() {
        // Default: do nothing
    }
}
