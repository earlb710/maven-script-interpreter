# Markdown View Button Test

This markdown file demonstrates the new **View** button feature for `.md` files in the tab editor.

## How to Use

1. Open this `.md` file in the EBS editor
2. Click the **View** button at the bottom of the editor
3. A WebView window will open showing the rendered HTML

## Features

The View button provides:

- **Real-time Preview**: Convert markdown to HTML and display it instantly
- **Auto-Refresh**: Toggle to automatically update preview when editing
- **Pin Window**: Keep the preview window always on top
- **Status Bar**: Shows URLs when hovering over links
- **Relative Path Support**: Images and links work correctly

## Example Content

### Code Block

```javascript
var markdown = "# Hello World";
var html = call file.markdownToHtml(markdown);
print html;
```

### Lists

- Item 1
- Item 2
- Item 3

### Tables

| Feature | Status |
|---------|--------|
| Headers | ✓ |
| Lists | ✓ |
| Code | ✓ |
| Tables | ✓ |

### Blockquote

> This is a blockquote with **bold** and *italic* text.

### Links

Check out the [Functions and Imports Guide](ScriptInterpreter/guides/FUNCTIONS_AND_IMPORTS_GUIDE.md) for more information.

---

**Note:** The View button uses the `file.markdownToHtml` builtin function to convert markdown to HTML with GitHub-style CSS.
