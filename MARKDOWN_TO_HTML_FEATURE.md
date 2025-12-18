# Markdown to HTML Conversion Feature

## Overview

The `file.markdownToHtml` builtin function converts Markdown documents to HTML, making it easy to display documentation in WebView components within EBS applications.

## Function Signature

```javascript
file.markdownToHtml(markdownText: string, includeStyles?: bool) -> string
```

### Parameters

- **markdownText** (string, required): The markdown text to convert to HTML
- **includeStyles** (bool, optional, default: true): Whether to include built-in CSS styling
  - `true`: Returns a complete HTML document with GitHub-style CSS
  - `false`: Returns just the HTML body content without styling

### Return Value

Returns a string containing the HTML output.

## Features

### Markdown Support

The function uses the [commonmark-java](https://github.com/commonmark/commonmark-java) library, which supports:

- **Headings** (H1-H6)
- **Emphasis** (bold, italic)
- **Lists** (ordered and unordered)
- **Code blocks** and inline code
- **Links** and images
- **Blockquotes**
- **Tables** (via GFM extension)
- **Horizontal rules**
- **Line breaks** and paragraphs

### Built-in Styling

When `includeStyles` is `true` (default), the function includes GitHub-style CSS that provides:

- Clean, readable typography
- Syntax highlighting for code blocks
- Styled tables with alternating row colors
- Proper heading hierarchy with borders
- Blockquote styling
- Link colors and hover effects
- Responsive layout (max-width: 900px, centered)

## Usage Examples

### Example 1: Basic Conversion

```javascript
// Convert markdown with default styling
var markdown: string = "# Hello World\n\nThis is **bold** text.";
var html: string = call file.markdownToHtml(markdown);
print html;
```

### Example 2: HTML Body Only

```javascript
// Convert without styling (just HTML body)
var markdown: string = "## Section\n\n- Item 1\n- Item 2";
var htmlBody: string = call file.markdownToHtml(markdown, false);
print htmlBody;
```

### Example 3: Display in WebView

```javascript
// Read markdown file and display in WebView
var mdContent: string = call file.readTextFile("README.md");
var html: string = call file.markdownToHtml(mdContent);

screen docViewer = {
    "title": "Documentation Viewer",
    "width": 900,
    "height": 700,
    "vars": [{
        "name": "content",
        "type": "string",
        "default": $html,
        "display": {
            "type": "webview",
            "labelText": "Documentation:"
        }
    }],
    "area": [{
        "name": "mainArea",
        "type": "vbox",
        "items": [{"name": "viewer", "varRef": "content"}]
    }]
};

show screen docViewer;
```

### Example 4: Convert Guide Files

```javascript
// Convert one of the new guide files
var guidePath: string = "ScriptInterpreter/guides/FUNCTIONS_IMPORTS_QUICK_REF.md";
var mdContent: string = call file.readTextFile(guidePath);
var html: string = call file.markdownToHtml(mdContent);

// Save to HTML file
call file.writeTextFile("/tmp/guide.html", html);
print "Guide converted to HTML";
```

## Test Scripts

Two test scripts are provided:

1. **ScriptInterpreter/scripts/examples/file_markdowntohtml.ebs**
   - Basic examples of markdown conversion
   - Demonstrates both styled and unstyled output
   - Shows file reading and conversion

2. **test_markdown_viewer.ebs** (in repository root)
   - Complete WebView demonstration
   - Interactive buttons to load different guides
   - Shows how to build a markdown documentation viewer

## Use Cases

### Documentation Viewer

Create an in-app documentation viewer that displays markdown files:

```javascript
function showDocumentation(mdFilePath: string) {
    var mdContent: string = call file.readTextFile(mdFilePath);
    var html: string = call file.markdownToHtml(mdContent);
    
    // Update WebView with converted HTML
    docScreen.content = html;
}
```

### README Display

Display project README files within your application:

```javascript
var readme: string = call file.readTextFile("README.md");
var html: string = call file.markdownToHtml(readme);
// Display in WebView
```

### Help System

Build an interactive help system using markdown files:

```javascript
// Help topics stored as markdown files
function showHelp(topic: string) {
    var helpFile: string = "help/" + topic + ".md";
    var mdContent: string = call file.readTextFile(helpFile);
    var html: string = call file.markdownToHtml(mdContent);
    helpScreen.viewer = html;
}
```

### Blog or News Display

Render blog posts or news articles written in markdown:

```javascript
var posts: string[] = ["post1.md", "post2.md", "post3.md"];
for (var i: int = 0; i < call array.length(posts); i++) {
    var md: string = call file.readTextFile(posts[i]);
    var html: string = call file.markdownToHtml(md, false);
    // Add to blog display
}
```

## Implementation Details

### Dependencies

- **Library**: commonmark-java 0.23.0
- **Module**: org.commonmark (automatic module)

### CSS Styling

The built-in CSS provides:
- GitHub-inspired color scheme
- Responsive typography (system fonts)
- Code block styling with background color
- Table borders and alternating row colors
- Blockquote left border
- Proper link styling
- Heading borders for H1 and H2

### Performance

- Markdown parsing is efficient for documents up to several MB
- HTML rendering is done in-memory
- No external network calls required
- Suitable for interactive applications

## Limitations

- **Tables**: Basic table support via commonmark (not full GFM)
- **Syntax Highlighting**: Code blocks are styled but not syntax-highlighted
- **Custom Extensions**: Currently uses default commonmark without extensions
- **Relative Links**: Links in markdown are preserved as-is (may need adjustment for WebView)

## Future Enhancements

Possible future improvements:
- Support for GitHub Flavored Markdown (GFM) extensions
- Syntax highlighting for code blocks
- Custom CSS themes
- Image path resolution for local images
- TOC (Table of Contents) generation
- Markdown to PDF conversion

## Related Documentation

- [EBS Script Syntax Reference](docs/EBS_SCRIPT_SYNTAX.md)
- [Functions and Imports Guide](ScriptInterpreter/guides/FUNCTIONS_AND_IMPORTS_GUIDE.md)
- [Commonmark Spec](https://spec.commonmark.org/)

## Version Information

- **Added in**: EBS 1.0.8.12
- **Library**: commonmark-java 0.23.0
- **Module Support**: Java Platform Module System (JPMS)

---

**Last Updated**: 2025-12-18
