# Visual Guide: "Start In" Directory Feature

## What Changed

### Before This PR
The run button area looked like this:
```
┌──────────────────────────────────────────┐
│ Output:                                  │
├──────────────────────────────────────────┤
│                                          │
│  [Console Output Area]                   │
│                                          │
├──────────────────────────────────────────┤
│ [Run]  [Clear]                           │
└──────────────────────────────────────────┘
```

**Problem**: Users couldn't see where their relative file paths would be resolved from.

### After This PR
The run button area now looks like this:
```
┌──────────────────────────────────────────┐
│ Output:                                  │
├──────────────────────────────────────────┤
│                                          │
│  [Console Output Area]                   │
│                                          │
├──────────────────────────────────────────┤
│ [Run]  [Clear]  Start in: /path/to/dir   │
└──────────────────────────────────────────┘
```

**Solution**: A label shows the current "start in" directory where file operations will look for relative paths.

## Interactive Features

### Label Appearance
- **Text**: "Start in: /path/to/script/directory"
- **Font**: 10px, gray color (#666)
- **Position**: Right of the Clear button
- **Max Width**: 400px (prevents layout issues)

### Hover Tooltip
When you hover over the label, you see:
```
┌─────────────────────────────────────────────────┐
│ File operations use relative paths from this    │
│ directory                                       │
│ /full/absolute/path/to/script/directory        │
└─────────────────────────────────────────────────┘
```

### Long Path Handling
For very long paths, the label truncates with ellipsis:
```
Short path:
Start in: /home/user/projects

Long path (truncated):
Start in: .../very/long/path/name/projects
```
(Hover shows full path in tooltip)

## Real-World Example

### Scenario
You're working on a project with this structure:
```
/home/user/my-projects/
├── data/
│   └── input.txt
└── scripts/
    ├── process.ebs
    └── utils.ebs
```

### Opening scripts/process.ebs
When you open `process.ebs` in a tab:

**Tab shows**:
```
Tab: process.ebs
Code area: [your script content]
Output area: [script output]
Buttons: [Run] [Clear] Start in: /home/user/my-projects/scripts
```

### What This Means
If your script contains:
```ebs
// This will look in /home/user/my-projects/scripts/data.txt
var handle1 = call file.open(path="data.txt", mode="r");

// To access ../data/input.txt, you need:
var handle2 = call file.open(path="../data/input.txt", mode="r");
```

The "Start in" label tells you that:
- Relative paths start from `/home/user/my-projects/scripts`
- `"data.txt"` → `/home/user/my-projects/scripts/data.txt`
- `"../data/input.txt"` → `/home/user/my-projects/data/input.txt`

## Benefits

### 1. **Clarity** 🎯
No more guessing where files will be loaded from.

### 2. **Debugging** 🐛
When `file.open()` fails, you immediately know what directory was searched.

### 3. **Consistency** ✅
Every tab shows its own "start in" directory, making multi-file editing clear.

### 4. **Learning** 📚
New users understand how relative paths work in EBS scripts.

## Technical Notes

### Feature Already Existed
The actual functionality (using script's directory for relative paths) was **already implemented**. This PR only adds **visual feedback**.

### Implementation
- **Backend**: No changes (already worked correctly)
- **Frontend**: Added label + CSS styling
- **Impact**: Purely informational, no behavior changes

### For New Files
When creating a new unsaved file:
```
[Run] [Clear] Start in: /current/working/directory
```
After saving the file, the label updates to show the file's parent directory.

## How to Use

1. **Open a script file** in the editor
2. **Look at the Run button area** - you'll see "Start in: [directory]"
3. **Write scripts with relative paths** knowing they'll resolve from that directory
4. **Hover over the label** to see the full path if it's truncated

## Example Workflows

### Workflow 1: Single Script
```
Open: /projects/myscript.ebs
Start in: /projects

Script uses: "data.txt"
Resolves to: /projects/data.txt ✅
```

### Workflow 2: Script in Subdirectory
```
Open: /projects/scripts/worker.ebs  
Start in: /projects/scripts

Script uses: "../config.json"
Resolves to: /projects/config.json ✅
```

### Workflow 3: New Unsaved File
```
Create: New1.ebs (not saved)
Start in: /current/working/directory

After saving to: /projects/New1.ebs
Start in: /projects ✅
```

## Styling Details

The label uses these CSS properties:
```css
.start-in-label {
    -fx-font-size: 10px;              /* Small, non-intrusive */
    -fx-text-fill: #666666;           /* Gray color */
    -fx-text-overrun: leading-ellipsis; /* ...path/truncated */
}
```

And in Java:
```java
startInLabel.setMaxWidth(400);  // Prevent layout stretch
startInLabel.setTooltip(...);    // Full path + explanation
```

## Frequently Asked Questions

**Q: Can I change the "start in" directory?**
A: The directory is determined by where your script file is saved. Save your script in a different directory to change the "start in" path.

**Q: What if I want to use absolute paths?**
A: Absolute paths (starting with `/` or `C:\`) work as expected and ignore the "start in" directory.

**Q: Does this affect existing scripts?**
A: No! This only adds visual feedback. All scripts continue to work exactly as before.

**Q: What about scripts in the console?**
A: Console commands use the working directory. File-based scripts use their file's directory.

## Summary

This feature makes the **implicit explicit** - you can now see where your relative file paths will be resolved from, making script development more transparent and easier to debug.
