package com.eb.script.interpreter.plugin;

import java.util.Map;

/**
 * Example implementation of the EbsFunction interface.
 * 
 * <p>This class demonstrates how to create a custom function that can be loaded
 * and called from EBS scripts using the plugin system.</p>
 * 
 * <h2>Usage Example in EBS</h2>
 * <pre>
 * // Load this example function
 * #plugin.load("com.eb.script.interpreter.plugin.ExampleEbsFunction", "echo");
 * 
 * // Call it using #custom.echo(...) syntax
 * var result = #custom.echo("Hello", "World");
 * print result;  // Outputs: [Echo] Hello World
 * 
 * // Get plugin info
 * var info = #plugin.info("echo");
 * print #json.getString(info, "description", "");
 * 
 * // Unload when done
 * #plugin.unload("echo");
 * </pre>
 * 
 * @author Earl Bosch
 * @since 1.0.4
 */
public class ExampleEbsFunction implements EbsFunction {
    
    private String prefix = "[Echo]";
    
    @Override
    public String getName() {
        return "Example Echo Function";
    }
    
    @Override
    public String getDescription() {
        return "An example EbsFunction implementation that echoes all arguments " +
               "back as a single concatenated string with a configurable prefix.";
    }
    
    @Override
    public Object execute(Object[] args) throws Exception {
        if (args == null || args.length == 0) {
            return prefix + " (no arguments)";
        }
        
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < args.length; i++) {
            sb.append(" ");
            if (args[i] == null) {
                sb.append("null");
            } else {
                sb.append(args[i].toString());
            }
        }
        return sb.toString();
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        // Check if a custom prefix was provided in the config
        if (config != null && config.containsKey("prefix")) {
            Object prefixValue = config.get("prefix");
            if (prefixValue != null) {
                this.prefix = prefixValue.toString();
            }
        }
    }
    
    @Override
    public void cleanup() {
        // Nothing to clean up for this simple example
    }
}
