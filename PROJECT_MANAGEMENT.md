# Project Management Feature

## Overview

The EBS Console now supports project management through `project.json` files. Projects can be created and opened via the File menu, allowing you to organize your EBS scripts and configure project-wide settings like default CSS stylesheets. **A TreeView on the right side of the console displays all opened projects, with automatic persistence to `console-projects.json`.**

## Menu Items

### New Project (Ctrl+Shift+N)

Creates a new project in a selected directory:

1. Opens a directory chooser dialog
2. Creates a `project.json` file with default settings in the selected directory
3. Loads the project configuration into the global `project` variable
4. Applies any CSS files specified in the configuration
5. **Adds the project to the Projects TreeView automatically**

### Open Project (Ctrl+Shift+O)

Opens an existing project:

1. Opens a file chooser dialog to select a `project.json` file
2. Loads the project configuration into the global `project` variable
3. Applies any CSS files specified in the configuration
4. **Adds the project to the Projects TreeView automatically**

## Projects TreeView

A TreeView component is displayed on the right side of the console (25% width by default), showing all opened projects.

### Features

- **Persistent Storage:** Project list is automatically saved to `console-projects.json`
- **Double-Click to Open:** Double-click any project to open it
- **Tooltips:** Hover over projects to see full paths
- **Context Menu:** Right-click for options:
  - **Open Project:** Opens the selected project
  - **Remove from List:** Removes project from the list (doesn't delete files)
  - **Clear All Projects:** Removes all projects from the list
- **Missing Project Detection:** If a project file is missing, you're prompted to remove it from the list
- **Resizable:** Drag the divider to resize between console and project list

### console-projects.json Format

```json
{
  "projects": [
    {
      "name": "MyProject",
      "path": "/path/to/project/project.json"
    },
    {
      "name": "AnotherProject",
      "path": "/path/to/another/project.json"
    }
  ]
}
```

The file is automatically created and updated as you create or open projects.

## Project Configuration (project.json)

The `project.json` file defines project settings and attributes. Here's the default structure:

```json
{
  "name": "ProjectName",
  "directory": "/path/to/project",
  "description": "EBS Script Project",
  "version": "1.0.0",
  "css": [
    "console.css"
  ],
  "mainScript": "main.ebs",
  "settings": {
    "autoLoad": true
  }
}
```

### Configuration Fields

- **name** (string): The project name (displayed in TreeView)
- **directory** (string): The project's base directory path
- **description** (string): A brief description of the project
- **version** (string): Project version number
- **css** (array of strings): List of CSS files to load automatically. Files are resolved:
  - First as relative paths from the project directory
  - Then as classpath resources (e.g., from `/css/` folder)
- **mainScript** (string): The main entry script file (for future use)
- **settings** (object): Custom project settings
  - **autoLoad** (boolean): Whether to auto-load the project on startup (future feature)

## Accessing Project Configuration in Scripts

Once a project is loaded, you can access the configuration through the global `project` variable:

```ebs
// Access project name
string projectName = project.name

// Access project directory
string projectDir = project.directory

// Access CSS files list
array cssFiles = project.css

// Access custom settings
bool autoLoad = project.settings.autoLoad
```

## CSS Loading

CSS files specified in the `css` array are automatically loaded and applied to the main application scene when the project is opened. This allows you to customize the appearance of your project's UI.

CSS files are resolved in the following order:
1. Relative to the project directory
2. From classpath resources (e.g., `/css/console.css`)

## Example Workflow

1. **Create a new project:**
   - Click File → New Project (or press Ctrl+Shift+N)
   - Select a directory for your project
   - A `project.json` file is created with default settings
   - Project appears in the TreeView on the right

2. **Customize the project:**
   - Edit the `project.json` file to add custom CSS files, update the description, etc.

3. **Open the project (from TreeView or menu):**
   - Double-click the project in the TreeView, OR
   - Click File → Open Project (or press Ctrl+Shift+O)
   - Select the `project.json` file
   - The project configuration is loaded and CSS files are applied

4. **Access configuration in scripts:**
   - Use the global `project` variable to access project settings
   - Example: `println("Working on project: " + project.name)`

5. **Manage project list:**
   - Right-click in TreeView to remove individual projects
   - Use "Clear All Projects" to reset the list
   - Projects are automatically saved to `console-projects.json`

## Notes

- The global `project` variable is available in all scripts after a project is loaded
- CSS files are applied to the main application scene
- Project configuration can include custom fields beyond the default structure
- Multiple projects can be switched by opening different `project.json` files or double-clicking in the TreeView
- The project list persists across application sessions
- The SplitPane divider can be dragged to adjust the size of the TreeView
