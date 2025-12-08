# UI Layout with Projects TreeView

## New Console Layout

The console now features a **SplitPane** layout with the Projects TreeView on the right side:

```
┌────────────────────────────────────────────────────────────────────┐
│  File  Edit  Config  Tools  Screens  Help                         │
├─────────────────────────────────────────────┬──────────────────────┤
│                                             │  Projects            │
│  ┌──────────────────────────────────────┐  │  ├─ ProjectName1     │
│  │ Console Tab │ Script1.ebs │ Script2 │  │  ├─ ProjectName2     │
│  ├──────────────────────────────────────┤  │  └─ ProjectName3     │
│  │                                      │  │                      │
│  │  Console Output Area                 │  │  (TreeView)          │
│  │  > Loaded project: MyProject         │  │                      │
│  │  > Project configuration loaded      │  │  Right-click for:    │
│  │                                      │  │  • Open Project      │
│  │                                      │  │  • Remove from List  │
│  │                                      │  │  • Clear All         │
│  │  Input: █                            │  │                      │
│  └──────────────────────────────────────┘  │  Double-click to     │
│                                             │  open project        │
│  Main Tabs Area (75% default width)        │  (25% default width) │
└─────────────────────────────────────────────┴──────────────────────┘
│  Status Bar: Ready                                                 │
└────────────────────────────────────────────────────────────────────┘
```

## Layout Components

### Main Area (Left - 75% width)
- **Menu Bar:** File, Edit, Config, Tools, Screens, Help
- **Tab Pane:** Console tab and opened script file tabs
- **Console Output:** Script execution output
- **Console Input:** Command input area

### Projects TreeView (Right - 25% width)
- **Root Node:** "Projects" (always visible)
- **Project Entries:** Each opened project as a child node
  - Shows project name
  - Tooltip displays full path
- **Interactions:**
  - Double-click: Open project
  - Right-click: Context menu
  - Hover: View full path in tooltip

### Divider
- **Resizable:** Drag left/right to adjust panel sizes
- **Default Position:** 75% / 25% split
- **Range:** Can be adjusted from 50% to 90%

### Status Bar (Bottom)
- Shows current status and error messages
- Full width across both panels

## SplitPane Features

### Benefits
1. **Persistent Project Access:** Always visible list of projects
2. **Quick Navigation:** Double-click to switch between projects
3. **Space Efficiency:** Collapsible by dragging divider
4. **Context Preservation:** Main console area unchanged

### Interaction Patterns

**Opening Projects:**
1. Via Menu: File → New/Open Project → Appears in TreeView
2. Via TreeView: Double-click existing project → Opens immediately
3. Via Context Menu: Right-click → Open Project

**Managing Projects:**
1. Remove Individual: Right-click → Remove from List
2. Remove All: Right-click → Clear All Projects
3. Handle Missing: Prompted to remove if file not found

## File: console-projects.json

Located in the application root directory:

```json
{
  "projects": [
    {
      "name": "MyFirstProject",
      "path": "/home/user/projects/MyFirstProject/project.json"
    },
    {
      "name": "WebApp",
      "path": "/home/user/projects/WebApp/project.json"
    }
  ]
}
```

### Auto-Save Behavior
- **Created:** On first project add (if doesn't exist)
- **Updated:** Every time a project is added or removed
- **Loaded:** On application startup
- **Location:** Same directory as console.cfg

## Visual Indicators

### TreeView Styling
- **Root Node:** Bold text, always expanded
- **Project Nodes:** Regular text with folder icon (default)
- **Selected Node:** Highlighted background
- **Hover:** Slight background change + tooltip

### Tooltips
- **Project Name Node:** Shows full path to project.json
- **Content:** `"/full/path/to/project/project.json"`
- **Delay:** Standard JavaFX tooltip delay

### Context Menu
Shows when right-clicking on:
- **Project Node:** Open, Remove options
- **Empty Space / Root:** Clear All option
- **Styling:** Standard JavaFX menu style

## Integration Points

### With EbsConsoleHandler
- `setProjectTreeView(ProjectTreeView)`: Sets reference
- `openProjectByPath(Path)`: Opens project from tree
- Projects added automatically on create/open

### With ProjectListManager
- Loads from `console-projects.json` on startup
- Saves after each add/remove operation
- Maintains project order (most recent last)

### With EbsApp
- Created in `initUI()` method
- Added to SplitPane with main tabs
- Reference passed to handler

## Future Enhancements

Potential improvements:
- Drag and drop to reorder projects
- Project categories/folders
- Recent projects section
- Project search/filter
- Project icons based on type
- Quick project switching (Ctrl+Tab style)
