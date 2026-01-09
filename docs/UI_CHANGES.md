# UI Changes: Project Management Menu Items

## File Menu Updates

The File menu has been updated with new project management options. The menu structure now looks like this:

```
File
├── New Script File (Ctrl+N)
├── Open file… (Ctrl+O)
├── ─────────────────────────
├── New Project… (Ctrl+Shift+N)     ← NEW
├── Open Project… (Ctrl+Shift+O)    ← NEW
├── ─────────────────────────
├── Recent files
│   ├── (recent files list)
│   └── Clear list
├── ─────────────────────────
└── Exit (Ctrl+Q)
```

## New Menu Items

### 1. New Project… (Ctrl+Shift+N)

**Location:** File menu, after "Open file…" item

**Keyboard Shortcut:** Ctrl+Shift+N

**Functionality:**
- Opens a directory chooser dialog
- Prompts user to select a directory for the new project
- Creates a `project.json` file with default configuration
- Loads the project into the global `project` variable
- Displays success message in console

**Dialog Flow:**
1. User selects directory
2. If `project.json` already exists:
   - Shows confirmation dialog asking to overwrite
   - User can confirm or cancel
3. Creates `project.json` with default values
4. Loads project configuration globally
5. Console output:
   ```
   New project created: /path/to/project/project.json
   Project loaded into global variable 'project'
   ```

### 2. Open Project… (Ctrl+Shift+O)

**Location:** File menu, after "New Project…" item

**Keyboard Shortcut:** Ctrl+Shift+O

**Functionality:**
- Opens a file chooser dialog filtered to `project.json` files
- Prompts user to select a `project.json` file
- Parses and loads the project configuration
- Applies CSS files specified in the project
- Stores project data in the global `project` variable
- Displays success message in console

**Dialog Flow:**
1. User selects `project.json` file
2. Parses JSON configuration
3. Loads configuration into global environment
4. Applies CSS stylesheets (if specified)
5. Console output:
   ```
   Project opened: /path/to/project/project.json
   Project loaded into global variable 'project'
   CSS loaded: console.css
   ```

## Visual Elements

Both menu items are positioned in a logical group separated by menu separators:
- First separator after "Open file…"
- Second separator after "Open Project…"
- This creates a clear visual grouping of file operations vs. project operations

## Console Output Examples

### Creating a New Project:
```
New project created: /home/user/MyProject/project.json
Project loaded into global variable 'project'
```

### Opening an Existing Project:
```
Project opened: /home/user/MyProject/project.json
Project loaded into global variable 'project'
CSS loaded: console.css
```

### Error Cases:
```
Failed to create new project: Unable to write to directory
```

```
Failed to open project: Invalid project.json: root must be a JSON object
```

## Keyboard Shortcuts Summary

| Action | Shortcut |
|--------|----------|
| New Script File | Ctrl+N |
| Open File | Ctrl+O |
| **New Project** | **Ctrl+Shift+N** |
| **Open Project** | **Ctrl+Shift+O** |
| Save | Ctrl+S |
| Save As | Ctrl+Shift+S |
| Exit | Ctrl+Q |

Note: The shortcuts follow the pattern where Shift modifier converts file operations to project operations.
