# Context Menu Changes - Project Tree

## Summary
This document illustrates the context menu changes made to the project tree view. A "Move..." option has been added below "Copy..." for both files and directories.

---

## 📄 File Context Menu

### Before:
```
┌─────────────────────────┐
│ Run Script              │  (only for .ebs files)
├─────────────────────────┤
│ Rename File...          │
│ Copy...                 │
├─────────────────────────┤
│ Delete                  │
└─────────────────────────┘
```

### After (NEW):
```
┌─────────────────────────┐
│ Run Script              │  (only for .ebs files)
├─────────────────────────┤
│ Rename File...          │
│ Copy...                 │
│ Move...                 │  ⭐ NEW
├─────────────────────────┤
│ Delete                  │
└─────────────────────────┘
```

---

## 📁 Directory Context Menu (Regular Folders)

### Before:
```
┌─────────────────────────┐
│ New File...             │
│ New Directory...        │
├─────────────────────────┤
│ Rename Directory...     │
│ Delete Directory...     │
├─────────────────────────┤
│ Refresh                 │
└─────────────────────────┘
```

### After (NEW):
```
┌─────────────────────────┐
│ New File...             │
│ New Directory...        │
├─────────────────────────┤
│ Rename Directory...     │
│ Move...                 │  ⭐ NEW
│ Delete Directory...     │
├─────────────────────────┤
│ Refresh                 │
└─────────────────────────┘
```

---

## 🔗 Linked Folder Context Menu

### Before:
```
┌─────────────────────────┐
│ New File...             │
│ New Directory...        │
├─────────────────────────┤
│ Rename Directory...     │
├─────────────────────────┤
│ Remove from Project     │
├─────────────────────────┤
│ Refresh                 │
└─────────────────────────┘
```

### After (NEW):
```
┌─────────────────────────┐
│ New File...             │
│ New Directory...        │
├─────────────────────────┤
│ Rename Directory...     │
│ Move...                 │  ⭐ NEW
├─────────────────────────┤
│ Remove from Project     │
├─────────────────────────┤
│ Refresh                 │
└─────────────────────────┘
```

---

## Functionality Details

### Move File
1. Right-click on any file
2. Select "Move..."
3. Choose destination directory in the dialog
4. File is moved to the new location
5. Tree view updates automatically

### Move Directory
1. Right-click on any directory (regular or linked)
2. Select "Move..."
3. Choose destination directory in the dialog
4. Directory and all its contents are moved
5. Tree view updates automatically

### Error Handling
- ✅ Checks if destination file/folder already exists
- ✅ Prevents moving directory into itself
- ✅ Handles null parent paths safely
- ✅ Shows user-friendly error dialogs
- ✅ Refreshes tree view automatically if destination is visible

---

## Implementation Files
- **Modified**: `ScriptInterpreter/src/main/java/com/eb/ui/ebs/ProjectTreeView.java`
  - Added menu items in context menu setup
  - Implemented `moveFile()` method
  - Implemented `moveDirectory()` method
  - Follows existing code patterns for consistency
