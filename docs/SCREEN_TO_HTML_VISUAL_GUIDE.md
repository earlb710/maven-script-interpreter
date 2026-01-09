# Screen JSON to HTML Conversion - Visual Guide

## Overview

The `scr.toHtml()` builtin converts EBS screen definitions (JavaFX UI) into HTML pages.

## Conversion Flow

```
┌─────────────────────────────────┐
│   EBS Screen Definition (JSON)  │
│                                 │
│  screen customerForm = {        │
│    "title": "Registration",     │
│    "width": 600,                │
│    "height": 500,               │
│    "vars": [                    │
│      {                          │
│        "name": "firstName",     │
│        "type": "string",        │
│        "display": {             │
│          "type": "textfield",   │
│          "labelText": "Name:",  │
│          "mandatory": true      │
│        }                        │
│      },                         │
│      ...                        │
│    ]                            │
│  }                              │
└─────────────────────────────────┘
              │
              │ call scr.toHtml("customerForm")
              ▼
┌─────────────────────────────────┐
│     HTML Page with CSS          │
│                                 │
│  <!DOCTYPE html>                │
│  <html>                         │
│    <head>                       │
│      <title>Registration</title>│
│      <style>                    │
│        /* Modern CSS */         │
│      </style>                   │
│    </head>                      │
│    <body>                       │
│      <div class="ebs-screen">  │
│        <div class="ebs-screen-title">│
│          Registration           │
│        </div>                   │
│        <div class="ebs-screen-content">│
│          <div class="ebs-var-container">│
│            <label>Name: *</label>│
│            <input type="text"   │
│                   name="firstName"/>│
│          </div>                 │
│          ...                    │
│        </div>                   │
│      </div>                     │
│    </body>                      │
│  </html>                        │
└─────────────────────────────────┘
```

## Control Mapping

| EBS Control Type | HTML Element | CSS Class |
|-----------------|--------------|-----------|
| textfield | `<input type="text">` | `.ebs-textfield` |
| textarea | `<textarea>` | `.ebs-textarea` |
| passwordfield | `<input type="password">` | `.ebs-passwordfield` |
| checkbox | `<input type="checkbox">` | `.ebs-checkbox` |
| combobox | `<select>` | `.ebs-combobox` |
| button | `<button>` | `.ebs-button` |
| label | `<span>` | `.ebs-label-value` |
| datepicker | `<input type="date">` | `.ebs-datepicker` |

## Layout Mapping

| EBS Area Type | HTML/CSS Implementation |
|--------------|-------------------------|
| vbox | `<div>` with `flex-direction: column` |
| hbox | `<div>` with `flex-direction: row` |
| gridpane | `<div>` with `display: grid` |
| borderpane | `<div>` with `grid-template-areas` |

## Example Output

### Input (EBS Screen Definition)
```javascript
screen contactForm = {
    "title": "Contact Us",
    "width": 500,
    "height": 400,
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
                "labelText": "Email:"
            }
        },
        {
            "name": "message",
            "type": "string",
            "display": {
                "type": "textarea",
                "labelText": "Message:"
            }
        }
    ]
};
```

### Output (HTML - Rendered Result)
```
╔══════════════════════════════════════════════════╗
║ Contact Us                                        ║ ← Purple gradient header
╠══════════════════════════════════════════════════╣
║                                                   ║
║  Your Name: *  [___________________________]     ║
║                                                   ║
║  Email:        [___________________________]     ║
║                                                   ║
║  Message:      [___________________________]     ║
║                [___________________________]     ║
║                [___________________________]     ║
║                                                   ║
╚══════════════════════════════════════════════════╝
```

## Use Cases

### 1. Documentation
Generate HTML documentation for screen layouts:
```javascript
var html = call scr.toHtml("myScreen");
call file.writeTextFile("docs/myScreen.html", html);
```

### 2. Web Prototyping
Export designs as HTML prototypes:
```javascript
var html = call scr.toHtml("dashboardScreen");
// Share with web developers
```

### 3. Static Forms
Create standalone HTML forms:
```javascript
var html = call scr.toHtml("surveyForm");
// Deploy to web server
```

### 4. Email Templates
Generate HTML for emails:
```javascript
var body = call scr.toHtml("emailForm", false);
// Embed in email template
```

## Features

✅ Complete HTML5 document structure
✅ Modern CSS styling with gradients
✅ Responsive flexbox layouts
✅ Form validation indicators
✅ Mobile-friendly design
✅ Customizable (with/without styles)
✅ JavaScript placeholder for events

## Limitations

❌ Event handlers not converted to JS
❌ Some advanced layouts simplified
❌ No runtime data binding
❌ Images/icons not processed

---

See [SCREEN_TO_HTML_FEATURE.md](SCREEN_TO_HTML_FEATURE.md) for complete documentation.
