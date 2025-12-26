# Button Icon Feature - Visual Guide

## Before (No Icon Support)

```
┌────────────────────────┐
│  Toolbar Area          │
│  ┌──────┐ ┌──────┐   │
│  │ Save │ │ Open │   │  <-- Plain buttons with text only
│  └──────┘ └──────┘   │
└────────────────────────┘
```

## After (With Icon Support)

```
┌────────────────────────┐
│  Toolbar Area          │
│  ┌─────────┐ ┌──────────┐   │
│  │ 💾 Save │ │ 📂 Open  │   │  <-- Buttons with icons + text
│  └─────────┘ └──────────┘   │
└────────────────────────────┘
```

## Implementation

### 1. Display Metadata (JSON)

```javascript
{
    "name": "saveButton",
    "type": "string",
    "area": "toolbarArea",
    "display": {
        "type": "button",
        "labelText": "Save",
        "icon": "icons/save.png",     // ← New property!
        "shortcut": "Ctrl+S",
        "onClick": "call saveFile();"
    }
}
```

### 2. Icon Loading Flow

```
┌──────────────────────────────────────────────────────────┐
│ 1. Parse Display Metadata                                │
│    → Extract "icon": "icons/save.png"                   │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────────┐
│ 2. Create Button Control                                 │
│    → button = new Button()                              │
│    → button.setText("Save")                             │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────────┐
│ 3. Load Icon Image                                       │
│    → loadIcon("icons/save.png", 16, 16)                │
│    → Try classpath: src/main/resources/icons/save.png  │
│    → Fallback to file system if needed                  │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────────┐
│ 4. Create ImageView                                      │
│    → imageView = new ImageView(image)                   │
│    → imageView.setFitHeight(16)                         │
│    → imageView.setFitWidth(16)                          │
│    → imageView.setPreserveRatio(true)                   │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────────┐
│ 5. Apply Icon to Button                                  │
│    → button.setGraphic(imageView)                       │
│    → Result: Button displays icon + text                │
└──────────────────────────────────────────────────────────┘
```

### 3. Example Screen Layout

```
╔══════════════════════════════════════════════════════════╗
║  File Manager                                      [x]    ║
╠══════════════════════════════════════════════════════════╣
║  Toolbar Area                                             ║
║  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐    ║
║  │ 📄 New  │ │ 📂 Open  │ │ 💾 Save │ │ ⚙️ Config │    ║
║  │  Ctrl+N │ │  Ctrl+O  │ │ Ctrl+S  │ │          │    ║
║  └─────────┘ └──────────┘ └─────────┘ └──────────┘    ║
╠══════════════════════════════════════════════════════════╣
║  Content Area                                             ║
║                                                          ║
║  (File list or content goes here)                        ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
```

## Code Structure

### DisplayItem Class
```java
public class DisplayItem {
    // ... existing fields ...
    String icon;  // ← New field for icon path
    // ... other fields ...
}
```

### AreaItemFactory - Button Creation
```java
} else if (control instanceof Button) {
    Button button = (Button) control;
    button.setText(metadata.labelText);
    
    // NEW: Apply icon if specified
    if (metadata.icon != null && !metadata.icon.isEmpty()) {
        ImageView iconView = loadIcon(metadata.icon, 16, 16);
        if (iconView != null) {
            button.setGraphic(iconView);
        }
    }
    
    // Apply shortcut key if specified
    if (metadata.shortcut != null && !metadata.shortcut.isEmpty()) {
        applyButtonShortcut(button, metadata.shortcut);
    }
}
```

### Icon Loading Helper
```java
private static ImageView loadIcon(String iconPath, double width, double height) {
    // 1. Try classpath resources
    InputStream is = ClassLoader.getResourceAsStream(iconPath);
    
    // 2. Try file system
    if (is == null) {
        File file = new File(iconPath);
        if (file.exists()) {
            image = new Image(file.toURI().toString());
        }
    }
    
    // 3. Create and scale ImageView
    ImageView imageView = new ImageView(image);
    imageView.setFitHeight(height);
    imageView.setFitWidth(width);
    imageView.setPreserveRatio(true);
    return imageView;
}
```

## Available Icons

### File Types
```
📄 icons/script-file.png       - EBS script files
📋 icons/config-file.png       - Configuration files
📝 icons/markdown-file.png     - Markdown documentation
🖼️  icons/image-file.png        - Image files
📄 icons/text-file.png         - Text files
☕ icons/java-file.png         - Java source files
```

### Folders & Projects
```
📁 icons/folder.png            - Closed folder
📂 icons/folder-open.png       - Open folder
⭐ icons/folder_fav.png        - Favorite/bookmarked folder
🔗 icons/folder_ref.png        - Linked/reference folder
📦 icons/project.png           - Project root
```

## Usage Patterns

### Pattern 1: Toolbar Buttons
```javascript
// Common toolbar with file operations
{
    "area": "toolbarArea",
    "type": "hbox",
    "vars": [
        { "button": "New",  "icon": "icons/script-file.png" },
        { "button": "Open", "icon": "icons/folder-open.png" },
        { "button": "Save", "icon": "icons/save.png" }
    ]
}
```

### Pattern 2: Action Buttons
```javascript
// Standalone action buttons
{
    "name": "refreshButton",
    "icon": "icons/refresh.png",
    "shortcut": "F5"
}
```

### Pattern 3: Type Selection
```javascript
// File type selection buttons
{
    "vars": [
        { "name": "scriptBtn", "icon": "icons/script-file.png" },
        { "name": "configBtn", "icon": "icons/config-file.png" },
        { "name": "mdBtn",     "icon": "icons/markdown-file.png" }
    ]
}
```

## Key Features

✅ **Automatic Scaling**: All icons scaled to 16x16px  
✅ **Flexible Paths**: Classpath resources or file system  
✅ **Graceful Fallback**: Missing icons don't break buttons  
✅ **Transparent Support**: PNG alpha channel supported  
✅ **Combined Features**: Works with shortcuts & onClick  
✅ **Consistent API**: Same pattern as TreeView icons  

## Testing

Run the demo script:
```bash
cd ScriptInterpreter
mvn javafx:run -Djavafx.args="../test_button_icons.ebs"
```

Demo includes:
- File action buttons (script, config, markdown, image icons)
- Folder action buttons (folder, open folder, project, favorite)
- Status updates on button clicks
- Combination of icons, text, shortcuts, and event handlers

## Summary

**Problem**: Screen button items did not support icons  
**Solution**: Added `icon` property to display metadata  
**Result**: Buttons can now display icons alongside text  

**Benefits**:
- More visually appealing user interfaces
- Clearer button purpose at a glance
- Consistent with modern UI conventions
- Uses existing icon resources from TreeView feature
