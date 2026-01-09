# User Guide - Move Files and Folders

## Overview
The Project Tree now supports moving files and folders to different locations using a convenient "Move..." option in the context menu.

---

## 🎯 Quick Start

### Moving a File
1. **Right-click** on any file in the project tree
2. Select **"Move..."** from the menu
3. Choose the destination folder in the dialog
4. Click **"Select Folder"**
5. ✅ File is moved instantly!

### Moving a Folder
1. **Right-click** on any folder in the project tree
2. Select **"Move..."** from the menu
3. Choose the destination folder in the dialog
4. Click **"Select Folder"**
5. ✅ Folder and all its contents are moved instantly!

---

## 📋 Menu Location

### For Files
```
Right-click on file →
  Run Script (if .ebs file)
  ─────────────────────
  Rename File...
  Copy...
  Move...              ⬅️ NEW! Located right after Copy
  ─────────────────────
  Delete
```

### For Folders
```
Right-click on folder →
  New File...
  New Directory...
  ─────────────────────
  Rename Directory...
  Move...              ⬅️ NEW! Located right after Rename
  Delete Directory...  (or Remove from Project for linked folders)
  ─────────────────────
  Refresh
```

---

## 💡 Features

### Smart Validation
- ✅ Prevents overwriting existing files/folders
- ✅ Can't move a folder into itself
- ✅ Automatically refreshes the tree view
- ✅ Shows clear error messages if something goes wrong

### What Gets Moved?
- **Files**: The file is moved to the new location
- **Folders**: The folder and ALL its contents are moved (recursive)
- **Original location**: File/folder is removed from the tree automatically

### Tree View Updates
After a successful move:
- ✅ Item disappears from source location
- ✅ Item appears in destination (if visible in tree)
- ✅ No need to manually refresh!

---

## 🛡️ Safety Features

### Before Moving
The system checks:
1. ✅ Destination folder exists and is accessible
2. ✅ No file/folder with same name exists at destination
3. ✅ You're not trying to move a folder into itself

### Error Handling
If something goes wrong, you'll see a clear message:
- ❌ "A file with that name already exists in the destination directory"
- ❌ "Cannot move directory into itself or its subdirectories"
- ❌ "Failed to move file: [reason]"

---

## 🎬 Example Workflows

### Reorganizing Project Files
```
Before:
  MyProject/
    script1.ebs
    script2.ebs
    config.json

Action: Move script2.ebs to a new "scripts" folder

After:
  MyProject/
    script1.ebs
    config.json
    scripts/
      script2.ebs
```

### Moving a Library Folder
```
Before:
  MyProject/
    lib/
      utils.ebs
      helpers.ebs
    src/
      main.ebs

Action: Move lib/ folder into src/

After:
  MyProject/
    src/
      main.ebs
      lib/
        utils.ebs
        helpers.ebs
```

---

## 🔄 Move vs Copy vs Rename

### Move
- Changes **location** of file/folder
- Original is **removed**
- Can move to **any folder** in the system
- Use when: Reorganizing project structure

### Copy
- Creates **duplicate** in new location
- Original **remains** unchanged
- Destination is same parent folder (different name)
- Use when: Need multiple versions

### Rename
- Changes **name** only
- Location **stays the same**
- Use when: Just need to rename, not relocate

---

## ⚠️ Important Notes

1. **Atomic Operation**: The move is atomic (all-or-nothing)
2. **No Undo**: Moving can't be undone automatically (use manual move back if needed)
3. **Project Files**: Moving files doesn't update references in other files
4. **Linked Folders**: Linked folders can be moved just like regular folders

---

## 🐛 Troubleshooting

### "Failed to move file"
- Check file isn't open in editor
- Verify you have write permissions
- Ensure destination folder exists and is writable

### "Directory Exists"
- A folder with that name already exists at destination
- Choose a different destination or rename the folder first

### Move option is grayed out
- This shouldn't happen, but if it does:
  - Try refreshing the project tree
  - Restart the application

---

## 📝 Keyboard Shortcuts

Currently, there are no keyboard shortcuts for Move. Use the context menu by:
- Right-clicking with mouse
- Shift+F10 (context menu key on some keyboards)

---

## ✨ Tips & Tricks

1. **Preview destination**: The dialog shows current folder, making it easy to navigate
2. **Quick access**: Recently used folders appear at top of dialog
3. **Batch operations**: Move multiple files by moving their parent folder
4. **Undo workaround**: If you make a mistake, use Move again to put it back

---

## 📞 Need Help?

If you encounter issues with the Move feature:
1. Check this guide for common solutions
2. Review error messages carefully
3. Verify file permissions and disk space
4. Try the operation manually with file explorer to rule out system issues

---

**Version**: 1.0  
**Last Updated**: 2025-12-14  
**Feature Status**: ✅ Ready for Use
