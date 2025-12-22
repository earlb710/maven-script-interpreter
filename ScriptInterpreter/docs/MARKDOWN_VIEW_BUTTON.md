# Markdown View Button Feature

## Overview

The tab editor now includes a **View** button when editing `.md` (Markdown) files. This button allows you to instantly preview the markdown document as rendered HTML in a WebView window.

## Location

The View button appears in the button row at the bottom of the tab editor, next to the Run and Clear buttons, when editing markdown files.

## Features

### Instant Preview
- Click the **View** button to open a WebView window showing the rendered HTML
- Uses the `file.markdownToHtml` builtin function for conversion
- Includes GitHub-style CSS for professional appearance

### Auto-Refresh Toggle
- Enable **🔄 Auto Refresh** to automatically update the preview when you edit the markdown
- Updates are debounced (0.5 second delay) to avoid excessive refreshes
- Button turns blue when enabled

### Pin Window
- Click **📌 Pin** to keep the preview window always on top
- Useful when working with multiple windows

### Status Bar
- Shows URLs when hovering over links in the preview
- Displays error messages if content fails to load

### Relative Path Support
- Images and links with relative paths work correctly
- Base URL is automatically set to the markdown file's directory

## Usage

1. **Open a Markdown File**
   - Open any `.md` file in the tab editor
   - The View button will appear in the button row

2. **Click View Button**
   - A WebView window opens showing the rendered HTML
   - Window size: 800x600 (resizable)

3. **Enable Auto-Refresh (Optional)**
   - Click the 🔄 Auto Refresh toggle button
   - Edit the markdown - preview updates automatically after 0.5 seconds

4. **Pin Window (Optional)**
   - Click the 📌 Pin button to keep window on top
   - Useful for side-by-side editing and preview

## Technical Details

### Implementation

The View button for markdown files:
- Checks file extension (`.md`)
- Calls `BuiltinsFile.markdownToHtml()` to convert markdown to HTML
- Opens HTML in a WebView with toolbar and status bar
- Supports auto-refresh with debounced updates
- Uses base URL for relative path resolution

### Code Location

- **File**: `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsTab.java`
- **Methods**: 
  - Button creation: Lines ~574-590 (in `tabUI()` method)
  - Preview method: `openMarkdownInWebView()`

### Related Features

- **file.markdownToHtml** builtin function (see `MARKDOWN_TO_HTML_FEATURE.md`)
- HTML View button (for `.html` files)

## Differences from HTML View

| Feature | HTML View | Markdown View |
|---------|-----------|---------------|
| File Types | `.html` | `.md` |
| Conversion | None (direct) | Markdown → HTML |
| Styling | As provided | GitHub-style CSS |
| Auto-Refresh | ✓ | ✓ |
| Pin Window | ✓ | ✓ |
| Status Bar | ✓ | ✓ |

## Example Workflow

### Editing Documentation

```
1. Open FUNCTIONS_AND_IMPORTS_GUIDE.md
2. Click View button
3. Enable Auto-Refresh
4. Pin the preview window
5. Edit markdown on left, see preview on right
6. Preview updates automatically as you type
```

### Writing README Files

```
1. Open README.md in tab editor
2. Click View to see how it will look
3. Make formatting adjustments
4. Auto-refresh shows changes immediately
```

### Creating Help Documentation

```
1. Create help.md file
2. Write documentation with headers, lists, code blocks
3. Click View to preview
4. Verify links and formatting work correctly
```

## Keyboard Shortcuts

While editing markdown files:

- **Ctrl+S**: Save file
- **Ctrl+F**: Find/Replace
- **Ctrl+L**: Toggle line numbers
- **Ctrl+G**: Go to line
- **Ctrl+/**: Toggle line comments

No specific keyboard shortcut for View button - use mouse/click or add custom binding.

## Error Handling

If markdown conversion fails:
- Error message appears in the output area
- WebView window does not open
- Check markdown syntax for issues

Common issues:
- Malformed markdown syntax
- Invalid characters
- Very large files (may be slow)

## Tips

1. **Keep Preview Window Open**: Pin it and position beside editor
2. **Use Auto-Refresh**: Makes editing much faster
3. **Test Links**: Click links in preview to verify they work
4. **Check Images**: Verify relative image paths resolve correctly
5. **Export HTML**: Copy HTML from preview for use elsewhere

## Integration with Guides

The View button is particularly useful for viewing the newly created guides:

- `ScriptInterpreter/guides/FUNCTIONS_AND_IMPORTS_GUIDE.md`
- `ScriptInterpreter/guides/FUNCTIONS_IMPORTS_QUICK_REF.md`
- Other markdown documentation files

Simply open the guide in the tab editor and click View to see it with proper formatting, headers, code blocks, and tables.

## Future Enhancements

Possible improvements:
- Table of contents navigation
- Export to PDF
- Custom CSS themes
- Markdown syntax validation
- Live collaboration features

---

**Version**: 1.0  
**Added**: 2025-12-18  
**Requires**: file.markdownToHtml builtin function
