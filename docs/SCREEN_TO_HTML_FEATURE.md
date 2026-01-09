# Screen JSON to HTML Conversion Feature

## Overview

The `scr.toHtml()` builtin function converts EBS screen definitions to HTML pages, making it easy to generate web-based representations of your JavaFX screen layouts.

## Function Signature

```javascript
scr.toHtml(screenName: string, includeStyles?: bool) -> string
```

### Parameters

- **screenName** (string, required): The name of the screen to convert
- **includeStyles** (bool, optional, default: true): Whether to include built-in CSS styling
  - `true`: Returns a complete HTML document with modern CSS styling
  - `false`: Returns just the HTML body content without styling

### Return Value

Returns a string containing the HTML output.

## Features

### Supported Screen Elements

The conversion process supports:

- **Variable Sets**: Organized groups of variables with scope control
- **Input Controls**:
  - Text fields (textfield)
  - Text areas (textarea)
  - Password fields (passwordfield)
  - Checkboxes (checkbox)
  - Combo boxes/dropdowns (combobox, choicebox)
  - Date pickers (datepicker)
- **Display Controls**:
  - Labels
  - Buttons
- **Layout Containers**:
  - VBox (vertical layout)
  - HBox (horizontal layout)
  - GridPane (grid layout)
  - BorderPane (border layout)
- **Layout Properties**:
  - Spacing
  - Padding
  - Alignment
  - Background colors
  - Custom styles

### Built-in Styling

When `includeStyles` is `true` (default), the function includes modern CSS that provides:

- Clean, responsive typography with system fonts
- Modern gradient-based UI with purple theme
- Form layout with proper spacing and alignment
- Hover effects and transitions on buttons
- Mandatory field indicators (red asterisk)
- Flexible layout system using CSS Flexbox
- Mobile-friendly responsive design

## Usage Examples

### Example 1: Basic Conversion

```javascript
// Define a simple screen
screen contactForm = {
    "title": "Contact Form",
    "width": 500,
    "height": 400,
    "sets": [{
        "setname": "contact",
        "scope": "visible",
        "vars": [
            {
                "name": "name",
                "type": "string",
                "display": {
                    "type": "textfield",
                    "labelText": "Your Name:",
                    "mandatory": true
                }
            },
            {
                "name": "email",
                "type": "string",
                "display": {
                    "type": "textfield",
                    "labelText": "Email Address:",
                    "mandatory": true
                }
            }
        ]
    }]
};

// Convert to HTML with default styling
var html: string = call scr.toHtml("contactForm");

// Save to file
call file.writeTextFile("contact_form.html", html);
```

### Example 2: Convert Without Styles

```javascript
// Convert screen to HTML body only (no styles or document structure)
var htmlBody: string = call scr.toHtml("contactForm", false);

// Use in a larger HTML template
var fullPage: string = 
    "<!DOCTYPE html><html><head><title>My App</title>" +
    "<link rel='stylesheet' href='my-styles.css'></head><body>" +
    htmlBody +
    "</body></html>";

call file.writeTextFile("custom_page.html", fullPage);
```

### Example 3: Multi-Section Form

```javascript
// Create a complex form with multiple sections
screen registrationForm = {
    "title": "User Registration",
    "width": 700,
    "height": 600,
    "sets": [
        {
            "setname": "personalInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "firstName",
                    "type": "string",
                    "display": {
                        "type": "textfield",
                        "labelText": "First Name:",
                        "mandatory": true
                    }
                },
                {
                    "name": "lastName",
                    "type": "string",
                    "display": {
                        "type": "textfield",
                        "labelText": "Last Name:",
                        "mandatory": true
                    }
                }
            ]
        },
        {
            "setname": "accountInfo",
            "scope": "visible",
            "vars": [
                {
                    "name": "email",
                    "type": "string",
                    "display": {
                        "type": "textfield",
                        "labelText": "Email:"
                    }
                },
                {
                    "name": "password",
                    "type": "string",
                    "display": {
                        "type": "passwordfield",
                        "labelText": "Password:"
                    }
                },
                {
                    "name": "subscribe",
                    "type": "bool",
                    "default": false,
                    "display": {
                        "type": "checkbox",
                        "labelText": "Subscribe to newsletter"
                    }
                }
            ]
        }
    ]
};

// Convert to HTML
var html: string = call scr.toHtml("registrationForm");
call file.writeTextFile("registration.html", html);
```

### Example 4: Display in WebView

```javascript
// Convert a screen to HTML and display in a WebView control
var formHtml: string = call scr.toHtml("contactForm");

// Create a screen with a WebView to show the HTML
screen htmlPreview = {
    "title": "HTML Preview",
    "width": 800,
    "height": 700,
    "vars": [{
        "name": "htmlContent",
        "type": "string",
        "default": $formHtml,
        "display": {
            "type": "webview",
            "labelText": "Generated HTML:"
        }
    }],
    "area": [{
        "name": "mainArea",
        "type": "vbox",
        "items": [{"varRef": "htmlContent"}]
    }]
};

show screen htmlPreview;
```

## Use Cases

### 1. Documentation Generation

Generate HTML documentation for your EBS screens:

```javascript
generateScreenDocumentation(screenName: string) {
    var html: string = call scr.toHtml(screenName);
    var docFile: string = "docs/screens/" + screenName + ".html";
    call file.writeTextFile(docFile, html);
    print "Documentation generated: " + docFile;
}
```

### 2. Prototype Export

Export JavaFX screen designs as HTML prototypes for web developers:

```javascript
exportScreens() {
    var screens: string[] = ["loginScreen", "dashboardScreen", "settingsScreen"];
    var i: int = 0;
    while i < call array.length(screens) do {
        var screenName: string = screens[i];
        var html: string = call scr.toHtml(screenName);
        call file.writeTextFile("prototypes/" + screenName + ".html", html);
        i = i + 1;
    }
}
```

### 3. Static Form Generation

Generate static HTML forms from screen definitions:

```javascript
// Create a survey form
screen surveyForm = {
    "title": "Customer Satisfaction Survey",
    "width": 600,
    "height": 500,
    "vars": [
        {
            "name": "rating",
            "type": "string",
            "display": {
                "type": "combobox",
                "labelText": "How would you rate our service?"
            }
        },
        {
            "name": "comments",
            "type": "string",
            "display": {
                "type": "textarea",
                "labelText": "Additional Comments:"
            }
        }
    ]
};

// Export as standalone HTML
var html: string = call scr.toHtml("surveyForm");
call file.writeTextFile("survey.html", html);
```

### 4. Email Template Generation

Generate HTML for email templates based on screen layouts:

```javascript
createEmailTemplate(screenName: string) return string {
    // Get HTML without full document structure
    var htmlBody: string = call scr.toHtml(screenName, false);
    
    // Wrap in email-friendly HTML
    var emailHtml: string = 
        "<!DOCTYPE html><html><head>" +
        "<style>body { font-family: Arial, sans-serif; }</style>" +
        "</head><body>" + htmlBody + "</body></html>";
    
    return emailHtml;
}
```

## Implementation Details

### HTML Structure

The generated HTML follows this structure when `includeStyles` is true:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Screen Title</title>
    <style>
        /* Modern CSS with gradient theme */
    </style>
</head>
<body>
    <div class="ebs-screen">
        <div class="ebs-screen-title">Screen Title</div>
        <div class="ebs-screen-content">
            <!-- Variables and areas rendered here -->
        </div>
    </div>
    <script>
        /* JavaScript for button click handlers */
    </script>
</body>
</html>
```

### CSS Classes

The generated HTML uses these CSS classes:

- `.ebs-screen`: Main screen container
- `.ebs-screen-title`: Title bar with gradient
- `.ebs-screen-content`: Content area
- `.ebs-var-container`: Variable container (label + control)
- `.ebs-label`: Variable label
- `.ebs-mandatory`: Mandatory field indicator (red asterisk)
- `.ebs-textfield`, `.ebs-textarea`, `.ebs-passwordfield`: Input controls
- `.ebs-checkbox`: Checkbox control
- `.ebs-combobox`: Dropdown control
- `.ebs-button`: Button control
- `.ebs-label-value`: Read-only label display
- `.ebs-datepicker`: Date picker control
- `.ebs-area`: Layout area container
- `.ebs-area-vbox`, `.ebs-area-hbox`, etc.: Layout-specific area classes

### JavaScript

A basic JavaScript event handler is included that shows an alert when buttons are clicked. This serves as a placeholder for actual event handler implementation.

## Limitations

- **Event Handlers**: onClick, onChange, and onValidate handlers are not converted to JavaScript. Buttons show a placeholder alert.
- **Complex Layouts**: Some advanced layout features (like GridPane cell spanning) may not translate perfectly to HTML.
- **Dynamic Content**: The HTML is a static snapshot of the screen definition. Runtime state and data binding are not included.
- **Custom Styles**: JavaFX-specific styles may not convert directly to CSS.
- **Images and Icons**: Button icons and other image references are not processed.

## Future Enhancements

Possible future improvements:

- Convert EBS event handlers to JavaScript
- Support for more complex layout features
- Include data population from current screen state
- CSS theme customization options
- Server-side form submission handling
- Export entire screen hierarchies (linked screens)

## Related Documentation

- [Screen Definition Best Practices Guide](ScriptInterpreter/guides/SCREEN_DEFINITION_BEST_PRACTICES.md)
- [EBS Script Syntax Reference](docs/EBS_SCRIPT_SYNTAX.md)
- [Area Definition Documentation](docs/AREA_DEFINITION.md)
- [Markdown to HTML Feature](MARKDOWN_TO_HTML_FEATURE.md)

## Version Information

- **Added in**: EBS 1.0.9
- **Related Builtins**: `file.markdownToHtml`, `file.writeTextFile`

---

**Last Updated**: 2025-12-27
