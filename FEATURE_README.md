# ✨ Project Management Feature - Quick Start

## 🎯 What's New?

The EBS Script Interpreter now supports **project management** through `project.json` configuration files with a **Projects TreeView** on the right side of the console!

### New Menu Items (File Menu)

```
📁 File
  └─ New Script File       (Ctrl+N)
  └─ Open file…            (Ctrl+O)
  ├─────────────────────────────────
  └─ ✨ New Project…       (Ctrl+Shift+N)  ⭐ NEW
  └─ ✨ Open Project…      (Ctrl+Shift+O)  ⭐ NEW
  ├─────────────────────────────────
  └─ Recent files
  └─ Exit                  (Ctrl+Q)
```

### New Projects TreeView 🌲

**A TreeView on the right side of the console** displays all your opened projects:

```
┌─────────────────────────┬────────────┐
│                         │ Projects   │
│   Console & Tabs        │ ├─ Proj1   │
│                         │ ├─ Proj2   │
│   (75% width)           │ └─ Proj3   │
│                         │            │
│                         │ (25% width)│
└─────────────────────────┴────────────┘
```

**Features:**
- 📋 **Automatic Tracking:** Every project you create or open is added
- 💾 **Auto-Save:** List saved to `console-projects.json` automatically
- 🖱️ **Double-Click to Open:** Quick access to your projects
- 📍 **Tooltips:** Hover to see full paths
- 🗑️ **Context Menu:** Right-click to remove or clear all

## 🚀 Quick Start

### Create a New Project

1. Press **Ctrl+Shift+N** (or File → New Project…)
2. Select a directory
3. A `project.json` file is created automatically
4. Project loaded into global `project` variable
5. **Project appears in TreeView automatically**

### Open an Existing Project

1. Press **Ctrl+Shift+O** (or File → Open Project…)
2. Select a `project.json` file
3. Project configuration loaded
4. CSS files applied automatically
5. **Project appears in TreeView automatically**

### Open from TreeView

1. **Double-click** any project in the TreeView
2. Project opens immediately
3. Configuration loaded and CSS applied

## 📦 Project Configuration

Example `project.json`:

```json
{
  "name": "MyProject",
  "directory": "/path/to/project",
  "description": "My EBS Script Project",
  "version": "1.0.0",
  "css": ["console.css", "custom.css"],
  "mainScript": "main.ebs",
  "settings": {
    "autoLoad": true
  }
}
```

## 💻 Using in Scripts

Access project configuration from any EBS script:

```ebs
// Print project info
println("Working on: " + project.name)
println("Version: " + project.version)

// Access CSS files
array cssFiles = project.css
println("CSS files: " + cssFiles)

// Access custom settings
println("Auto-load: " + project.settings.autoLoad)
```

## 🌲 Managing the Project List

### Via Context Menu (Right-Click in TreeView)

- **Open Project:** Opens the selected project
- **Remove from List:** Removes from list (doesn't delete files)
- **Clear All Projects:** Removes all projects from list

### Via console-projects.json

Projects are automatically saved to `console-projects.json`:

```json
{
  "projects": [
    {
      "name": "MyProject",
      "path": "/path/to/project/project.json"
    }
  ]
}
```

The file is automatically created and updated. You can also edit it manually.

## 📚 Documentation

- **[PROJECT_MANAGEMENT.md](PROJECT_MANAGEMENT.md)** - Complete feature guide with TreeView details
- **[UI_CHANGES.md](UI_CHANGES.md)** - Visual UI documentation
- **[IMPLEMENTATION_SUMMARY_PROJECT.md](IMPLEMENTATION_SUMMARY_PROJECT.md)** - Technical details

## 🎨 Key Features

✅ **Easy Project Creation** - One-click creation with default configuration  
✅ **Automatic CSS Loading** - Styles applied from project config  
✅ **Global Access** - `project` variable available in all scripts  
✅ **Smart Path Resolution** - CSS loaded from project dir or classpath  
✅ **Error Handling** - Clear messages for all operations  
✅ **Projects TreeView** - Visual list with double-click to open  
✅ **Persistent Storage** - Auto-save to console-projects.json  
✅ **Resizable Layout** - Drag divider to adjust tree width  

## 📝 Example Files

- `console-projects.json` - Project list storage (auto-generated)
- `ScriptInterpreter/example-project.json` - Sample configuration
- `ScriptInterpreter/example-show-project.ebs` - Demo script

## 🔑 Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| New Project | **Ctrl+Shift+N** |
| Open Project | **Ctrl+Shift+O** |
| New Script | Ctrl+N |
| Open File | Ctrl+O |

## 💡 Tips

- **Resize the TreeView:** Drag the divider between console and tree
- **Quick Open:** Double-click projects in the TreeView
- **Missing Projects:** Right-click to remove if project file is deleted
- **Persistent List:** Projects saved automatically and restored on restart
- CSS files are searched in order: project directory → classpath
- Project config supports custom fields beyond the defaults
- The `project` variable persists across script executions
- Multiple CSS files can be specified in the `css` array

---

**Ready to organize your EBS projects!** 🎉
