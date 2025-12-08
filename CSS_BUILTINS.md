# CSS Builtins - Dynamic CSS Loading for Screens

The EBS scripting language provides built-in functions for dynamically loading and managing CSS stylesheets for screens at runtime. This allows screens to have their own custom styling without requiring application restarts.

## Best Practice: Organizing Screen Applications

**When creating applications with screens and custom CSS**, organize your files in a dedicated directory structure. This keeps your EBS scripts and CSS files together:

```
my-screen-app/
├── my-app.ebs          # Main EBS script with screen definitions
├── custom-theme.css    # Custom CSS for your screens
├── dark-theme.css      # Alternative theme (optional)
└── README.md           # Optional documentation
```

**Benefits of this organization:**
- Related files stay together (easier to manage and deploy)
- CSS files can use relative paths from the EBS script location
- Easy to share or version control as a complete unit
- Clear separation of different screen applications
- Simplified path resolution when loading CSS

**Example:**
```javascript
// In my-screen-app/my-app.ebs
screen myScreen = { 
    title: "My Application",
    // ... screen definition
};

show screen myScreen;

// Load CSS from same directory using relative path
#css.loadCss("myscreen", "custom-theme.css");
```

See the complete example in: `ScriptInterpreter/scripts/examples/css-screen-demo/`

## Available Builtins

### css.loadCss(screenName, cssPath)

Dynamically loads a CSS stylesheet and applies it to the specified screen.

**Parameters:**
- `screenName` (string): Name of the screen to apply CSS to (case-insensitive)
- `cssPath` (string): Path to the CSS file (supports classpath resources, file system paths, or URLs)

**Returns:** `boolean` - true if successful

**Resolution Strategy:**
The builtin tries multiple resolution strategies in order:
1. Classpath resource (e.g., `/css/custom-screen.css`)
2. Classpath resource with `/css/` prefix added automatically for simple filenames
3. File system path (absolute or relative to sandbox)
4. Direct URL (file:// , http:// , https://)

**Example:**
```ebs
// Load CSS from classpath resources
#css.loadCss("myscreen", "custom-screen.css");

// Load CSS from file system
#css.loadCss("myscreen", "/path/to/my-styles.css");

// Load CSS from sandbox
#css.loadCss("myscreen", "my-custom.css");
```

**Notes:**
- The screen must exist and be shown before loading CSS
- CSS is applied immediately to the scene's stylesheets list
- Duplicate CSS files are automatically prevented
- Changes take effect immediately without reloading the screen

---

### css.unloadCss(screenName, cssPath)

Removes a previously loaded CSS stylesheet from the specified screen.

**Parameters:**
- `screenName` (string): Name of the screen to remove CSS from (case-insensitive)
- `cssPath` (string): Path to the CSS file to remove (same format as loadCss)

**Returns:** `boolean` - true if successful

**Example:**
```ebs
// Unload CSS stylesheet
#css.unloadCss("myscreen", "custom-screen.css");
```

**Notes:**
- The builtin attempts exact match first, then partial match if not found
- Safe to call even if the CSS was not loaded
- Styles revert to the remaining loaded stylesheets

---

### css.getValue(cssPath, selector, property)

Retrieves a CSS property value from a stylesheet file (existing builtin).

**Parameters:**
- `cssPath` (string): Path to CSS file or resource
- `selector` (string): CSS selector (e.g., ".error", "#main", "body")
- `property` (string): CSS property name (e.g., "-fx-fill", "color")

**Returns:** `string` - The property value, or null if not found

**Example:**
```ebs
var color = #css.getValue("css/console.css", ".error", "-fx-text-fill");
print "Error text color: " + color;
```

---

### css.findCss(searchPath?)

Searches for available CSS stylesheet files (existing builtin).

**Parameters:**
- `searchPath` (string, optional): Base path to search in. If not provided, searches in default locations (classpath css/ folder and sandbox)

**Returns:** `array` - Array of strings containing found CSS file paths

**Example:**
```ebs
var cssFiles = #css.findCss();
// Returns: ["css/console.css", "css/screen-areas.css", "css/screen-inputs.css", ...]

var customCss = #css.findCss("/path/to/custom");
// Returns CSS files found in the specified directory
```

---

## Complete Example

Here's a complete example showing how to create a screen with custom CSS loading functionality:

```ebs
// Define a screen
screen styledScreen = {
    title: "Custom Styled Screen",
    width: 600,
    height: 400,
    
    area mainArea = {
        type: "vbox",
        spacing: "10",
        padding: "20",
        
        items: [
            {
                name: "titleLabel",
                type: "label",
                text: "Custom Styled Screen",
                cssClass: "my-title"
            },
            {
                name: "contentField",
                type: "textfield",
                text: "Enter text here",
                cssClass: "my-input"
            },
            {
                name: "loadStyleButton",
                type: "button",
                text: "Load Custom Theme",
                onClick: {
                    #css.loadCss("styledscreen", "my-theme.css");
                    print "Custom theme loaded!";
                }
            },
            {
                name: "unloadStyleButton",
                type: "button",
                text: "Unload Custom Theme",
                onClick: {
                    #css.unloadCss("styledscreen", "my-theme.css");
                    print "Custom theme removed!";
                }
            }
        ]
    }
};

show screen styledScreen;
```

And the corresponding CSS file (`my-theme.css`):

```css
/* Custom theme for styledScreen */
.my-title {
    -fx-font-size: 24px;
    -fx-text-fill: #2196F3;
    -fx-font-weight: bold;
}

.my-input {
    -fx-background-color: #f0f0f0;
    -fx-border-color: #2196F3;
    -fx-border-width: 2px;
    -fx-padding: 10px;
}
```

---

## Use Cases

### Theme Switching
Allow users to switch between different visual themes at runtime:
```ebs
switchTheme(themeName: string) {
    #css.unloadCss("myscreen", currentTheme);
    #css.loadCss("myscreen", themeName + ".css");
    currentTheme = themeName;
}
```

### Screen-Specific Styling
Each screen can load its own CSS file for custom appearance:
```ebs
show screen loginScreen;
#css.loadCss("loginscreen", "login-styles.css");

show screen dashboardScreen;
#css.loadCss("dashboardscreen", "dashboard-styles.css");
```

### Dynamic Style Updates
Update styles based on application state:
```ebs
if errorOccurred then {
    #css.loadCss("myscreen", "error-styles.css");
} else {
    #css.unloadCss("myscreen", "error-styles.css");
}
```

---

## Technical Notes

### Thread Safety
- CSS loading/unloading operations are executed on the JavaFX Application Thread
- Operations are asynchronous and return immediately
- Changes are applied as soon as the JavaFX thread processes them

### Performance
- CSS files are cached by the underlying JavaFX SceneBuilder
- Loading the same CSS file multiple times is efficient (automatically deduplicated)
- Unloading CSS doesn't clear the cache, so re-loading is fast

### Error Handling
- If a CSS file cannot be found, an InterpreterError is thrown
- If the screen doesn't exist, an InterpreterError is thrown
- If the screen is not shown yet, an InterpreterError is thrown
- Parse errors in CSS files are logged but don't prevent loading

---

## Limitations

1. **Screen Must Exist**: The screen must be created and shown before loading CSS
2. **No Hot Reload**: Changes to CSS files require manual reload via `css.unloadCss` + `css.loadCss`
3. **JavaFX CSS Only**: Only JavaFX CSS properties are supported (e.g., `-fx-*` properties)
4. **No @import Support**: CSS @import rules may not work reliably; use multiple loadCss calls instead

---

## See Also

- `scr.*` builtins for screen management
- JavaFX CSS Reference Guide for available CSS properties
- Screen definition syntax in the EBS Language Reference
