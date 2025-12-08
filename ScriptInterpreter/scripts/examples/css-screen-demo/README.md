# CSS Screen Demo Example

This example demonstrates how to organize screen applications with custom CSS files.

## Recommended Directory Structure

When creating applications with screens and custom CSS styling, organize your files in a dedicated directory:

```
my-screen-app/
├── my-app.ebs          # Main EBS script with screen definitions
├── custom-theme.css    # Custom CSS for your screens
└── README.md           # Optional documentation
```

This approach keeps related files together and makes it easier to:
- Manage CSS files specific to your screens
- Share or deploy your screen application as a unit
- Maintain consistent styling across screens
- Load custom CSS using relative paths

## Files in This Example

- **css-screen-demo.ebs** - Main script demonstrating CSS loading functionality
- **custom-screen.css** - Example CSS file with custom styles
- **test-css-builtins.ebs** - Test script for CSS builtins functionality

## Running the Example

From the repository root:

```bash
cd ScriptInterpreter
mvn javafx:run
```

Then in the console, load the example:

```javascript
import "scripts/examples/css-screen-demo/css-screen-demo.ebs";
```

Or run directly:

```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="scripts/examples/css-screen-demo/css-screen-demo.ebs"
```

## What This Example Demonstrates

1. **Creating a screen with CSS classes**: Items use `cssClass` property to reference CSS styles
2. **Loading custom CSS dynamically**: Use `css.loadCss(screenName, cssPath)` to apply custom styles
3. **Unloading CSS**: Use `css.unloadCss(screenName, cssPath)` to remove custom styles
4. **Finding available CSS files**: Use `css.findCss()` to discover CSS files
5. **Organizing screen apps**: Keep EBS and CSS files together in their own directory

## Key Features Demonstrated

### Dynamic CSS Loading

```javascript
// Load custom CSS for a screen
#css.loadCss("myscreen", "custom-screen.css");

// Unload custom CSS
#css.unloadCss("myscreen", "custom-screen.css");
```

### CSS Classes in Screen Definitions

```javascript
screen myScreen = {
    area mainArea = {
        items: [
            {
                name: "titleLabel",
                type: "label",
                cssClass: "custom-label"  // References CSS class
            }
        ]
    }
};
```

### Path Resolution

The CSS loading builtins support multiple path formats:
- **Relative paths**: `"custom-screen.css"` (resolves in sandbox/current directory)
- **Classpath resources**: `"css/console.css"` (from application resources)
- **Absolute paths**: `"/absolute/path/to/styles.css"`
- **URLs**: `"file:///path/to/styles.css"`

## Best Practices

1. **Organize by Feature**: Keep screen EBS files and their CSS in the same directory
2. **Use Descriptive Names**: Name CSS files to match their purpose (e.g., `login-screen.css`)
3. **Load CSS After Showing Screen**: Screens must be shown before loading CSS
4. **Use CSS Classes**: Define reusable styles with CSS classes rather than inline styles
5. **Theme Switching**: Load/unload CSS files to implement theme switching

## CSS File Structure

The `custom-screen.css` file demonstrates proper JavaFX CSS syntax:

```css
/* Custom button style */
.custom-button {
    -fx-background-color: #4CAF50;
    -fx-text-fill: white;
    -fx-font-size: 14px;
    -fx-padding: 10px 20px;
}

/* Custom label style */
.custom-label {
    -fx-text-fill: #2196F3;
    -fx-font-size: 16px;
    -fx-font-weight: bold;
}
```

Note: Use JavaFX CSS properties (prefixed with `-fx-`) for proper styling.
