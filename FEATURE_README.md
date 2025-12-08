# ✨ Project Management Feature - Quick Start

## 🎯 What's New?

The EBS Script Interpreter now supports **project management** through `project.json` configuration files!

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

## 🚀 Quick Start

### Create a New Project

1. Press **Ctrl+Shift+N** (or File → New Project…)
2. Select a directory
3. A `project.json` file is created automatically
4. Project loaded into global `project` variable

### Open an Existing Project

1. Press **Ctrl+Shift+O** (or File → Open Project…)
2. Select a `project.json` file
3. Project configuration loaded
4. CSS files applied automatically

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

## 📚 Documentation

- **[PROJECT_MANAGEMENT.md](PROJECT_MANAGEMENT.md)** - Complete feature guide
- **[UI_CHANGES.md](UI_CHANGES.md)** - Visual UI documentation
- **[IMPLEMENTATION_SUMMARY_PROJECT.md](IMPLEMENTATION_SUMMARY_PROJECT.md)** - Technical details

## 🎨 Key Features

✅ **Easy Project Creation** - One-click creation with default configuration  
✅ **Automatic CSS Loading** - Styles applied from project config  
✅ **Global Access** - `project` variable available in all scripts  
✅ **Smart Path Resolution** - CSS loaded from project dir or classpath  
✅ **Error Handling** - Clear messages for all operations  

## 📝 Example Files

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

- CSS files are searched in order: project directory → classpath
- Project config supports custom fields beyond the defaults
- The `project` variable persists across script executions
- Multiple CSS files can be specified in the `css` array

---

**Ready to organize your EBS projects!** 🎉
