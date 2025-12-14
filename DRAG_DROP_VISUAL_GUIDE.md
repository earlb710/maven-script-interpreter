# Drag-and-Drop Visual User Guide

## How to Use Drag-and-Drop in Project Tree

This guide shows you how to use the new drag-and-drop feature to reorganize files and directories in your projects.

## Basic Operation

### Moving a File

```
Before:
Projects
└── MyProject
    ├── src/
    │   └── helper.ebs
    └── main.ebs

Steps:
1. Click and hold on "helper.ebs"
2. Drag over "src/" folder (folder highlights in blue)
3. Release mouse button

After:
Projects
└── MyProject
    ├── src/
    │   ├── helper.ebs    ← Moved here
    │   └── main.ebs
```

### Moving a Directory

```
Before:
Projects
└── MyProject
    ├── lib/
    │   └── utils.ebs
    ├── src/
    │   └── main.ebs
    └── config.json

Steps:
1. Click and hold on "lib/" folder
2. Drag over "src/" folder (folder highlights in blue)
3. Release mouse button

After:
Projects
└── MyProject
    ├── src/
    │   ├── lib/          ← Moved here
    │   │   └── utils.ebs
    │   └── main.ebs
    └── config.json
```

## Visual Feedback

### Valid Drop Target
When dragging over a directory (valid target):
```
┌─────────────────────────┐
│ 📁 src/                 │ ← Highlights with light blue background
└─────────────────────────┘
     ▲
     │ (dragging file over it)
```

### Invalid Drop Target
When dragging over a file (invalid target):
```
┌─────────────────────────┐
│ 📄 main.ebs             │ ← No highlight
└─────────────────────────┘
     ▲
     │ (dragging file over it)
     └── Cursor shows "not allowed" symbol ⊗
```

## What You Can Drag

✓ **Files** - Any file within a project  
✓ **Directories** - Any folder within a project  
✓ **Nested Items** - Files/folders in subdirectories  

✗ **Project Nodes** - Cannot drag the project itself  
✗ **Root Node** - Cannot drag "Projects" root  

## Where You Can Drop

✓ **Directories** - Any folder within a project  
✓ **Nested Directories** - Folders inside other folders  

✗ **Files** - Cannot drop onto files  
✗ **Same Parent** - Cannot drop into current parent (no change)  
✗ **Into Self** - Cannot drop folder into itself  
✗ **Into Subdirectory** - Cannot drop folder into its own child  

## Error Messages

### Duplicate Name
```
┌──────────────────────────────────────┐
│         Cannot Move                  │
├──────────────────────────────────────┤
│ A file or directory with that name   │
│ already exists in the destination.   │
│                                      │
│              [ OK ]                  │
└──────────────────────────────────────┘
```

**What it means**: The destination folder already has a file/folder with the same name.

**What to do**: 
- Rename the source file/folder first, or
- Choose a different destination, or
- Delete/move the existing file in the destination

### Move Failed
```
┌──────────────────────────────────────┐
│         Move Error                   │
├──────────────────────────────────────┤
│ Failed to move: [error details]     │
│                                      │
│              [ OK ]                  │
└──────────────────────────────────────┘
```

**What it means**: The file system operation failed (permissions, disk space, etc.)

**What to do**: 
- Check file permissions
- Verify disk space
- Ensure the file isn't open in another program
- Try the operation again

## Example Scenarios

### Scenario 1: Organizing Scripts into Folders

**Goal**: Move all utility scripts into a "utils" folder

```
Initial Structure:
Projects
└── MyProject
    ├── main.ebs
    ├── helper1.ebs
    ├── helper2.ebs
    └── config.json

Create "utils" folder:
(Right-click MyProject → New Directory → "utils")

Drag helper1.ebs to utils/:
1. Click helper1.ebs
2. Drag to utils/ (highlights blue)
3. Drop

Drag helper2.ebs to utils/:
1. Click helper2.ebs
2. Drag to utils/ (highlights blue)
3. Drop

Final Structure:
Projects
└── MyProject
    ├── utils/
    │   ├── helper1.ebs
    │   └── helper2.ebs
    ├── main.ebs
    └── config.json
```

### Scenario 2: Restructuring Project Directories

**Goal**: Move "resources" folder inside "src"

```
Initial Structure:
Projects
└── MyProject
    ├── src/
    │   └── main.ebs
    ├── resources/
    │   ├── images/
    │   └── data.json
    └── project.json

Drag resources/ to src/:
1. Click resources/ folder
2. Drag to src/ (highlights blue)
3. Drop

Final Structure:
Projects
└── MyProject
    ├── src/
    │   ├── resources/
    │   │   ├── images/
    │   │   └── data.json
    │   └── main.ebs
    └── project.json
```

### Scenario 3: Extracting Files from Subdirectory

**Goal**: Move file from nested folder to project root

```
Initial Structure:
Projects
└── MyProject
    ├── temp/
    │   └── important.ebs
    └── main.ebs

Drag important.ebs to MyProject:
1. Expand temp/ folder
2. Click important.ebs
3. Drag to MyProject node (root of project)
   ⚠️ Cannot drop on project node!

Instead, use context menu:
1. Right-click important.ebs
2. Select "Move..."
3. Choose MyProject directory
4. Click "Select"

Final Structure:
Projects
└── MyProject
    ├── temp/
    ├── important.ebs
    └── main.ebs
```

**Note**: To move to project root, use context menu "Move..." since project nodes themselves cannot be drop targets.

## Tips and Tricks

### Quick Reorganization
- Open all folders first (expand tree)
- Drag multiple items one by one
- Watch the blue highlight to confirm valid drop

### Undo Accidental Move
- Use context menu "Move..." to move back
- Or manually drag back to original location
- Tree updates immediately for both directions

### Complex Restructuring
For major project reorganization:
1. Plan the new structure first
2. Create new folders as needed
3. Move files in logical groups
4. Verify structure after each move

### Alternative: Context Menu
If drag-and-drop isn't working:
- Right-click file/folder
- Select "Move..."
- Choose destination with folder picker
- Click "Select"

Both methods use the same underlying operation!

## Keyboard Shortcuts

Currently, drag-and-drop uses mouse only:
- **Left Click + Hold**: Start drag
- **Move Mouse**: Drag to target
- **Release**: Drop

Future enhancements may include:
- **Ctrl + Drag**: Copy instead of move
- **Shift + Drag**: Move to specific subfolder
- **Esc**: Cancel drag operation

## Troubleshooting

### Drag Doesn't Start
**Possible causes**:
- Clicking on project node (not supported)
- Clicking on root "Projects" (not supported)
- File doesn't exist (shown in red)

**Solution**: Only drag files/folders within projects

### Drop Isn't Accepted
**Possible causes**:
- Dragging over a file (not a folder)
- Dragging into same parent folder
- Dragging folder into itself

**Solution**: Drag over a different folder (watch for blue highlight)

### Tree Doesn't Update
**Possible causes**:
- Move failed (check error message)
- Tree rendering issue

**Solution**: 
- Right-click parent folder → Refresh
- Or close and reopen project

### Blue Highlight Stays
**Rare issue**: Visual feedback not cleared

**Solution**: 
- Click elsewhere in tree
- Move mouse away from folder
- Restart application if persists

## Comparison: Drag-and-Drop vs Context Menu

| Feature | Drag-and-Drop | Context Menu "Move..." |
|---------|---------------|------------------------|
| Speed | Fast ✓ | Slower (dialog) |
| Visual | See target ✓ | Must type/select |
| Visible targets | Only visible folders | All folders (browser) |
| Precision | Depends on tree | Exact selection ✓ |
| Multi-move | One at a time | One at a time |
| Undo | Manual | Manual |

**Best practice**: Use drag-and-drop for quick moves within visible tree, use context menu for moving to distant or hidden folders.

## Summary

The drag-and-drop feature makes reorganizing your project structure quick and intuitive:

1. **Click and hold** on file/folder
2. **Drag** over destination folder (watch for blue highlight)
3. **Release** to complete move
4. Tree **updates automatically**

Remember:
- ✓ Only works within project structure
- ✓ Must drop on folders (not files)
- ✓ Shows blue highlight for valid targets
- ✓ Shows error if name conflicts
- ✓ Updates tree immediately

Happy organizing! 🎉
