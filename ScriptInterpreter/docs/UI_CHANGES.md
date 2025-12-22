# UI Changes: "Start In" Directory Label

## Description
Added a label next to the Run button that displays the current "start in" directory. This makes the existing feature visible to users.

## Visual Layout

Before:
```
Output:
+--------------------------------------+
|  Output Area                         |
+--------------------------------------+
[Run] [Clear]
```

After:
```
Output:
+--------------------------------------+
|  Output Area                         |
+--------------------------------------+
[Run] [Clear]  Start in: /path/to/script/directory
```

## Features

1. **Label Text**: Shows "Start in: [directory path]"
   - The directory path is the parent directory of the current script file
   - For new unsaved files, it shows the working directory

2. **Tooltip**: Hovering over the label shows:
   "File operations use relative paths from this directory"

3. **Styling**: 
   - Font size: 10px
   - Color: Gray (#666)
   - Non-intrusive, informational appearance

## How It Works

When you open a script file (e.g., `/home/user/projects/my-script.ebs`):
- The "Start in" label shows: `/home/user/projects/`
- Any relative file paths in the script (e.g., `data.txt`) will be resolved as `/home/user/projects/data.txt`

This feature was already implemented in the backend - this change just makes it visible in the UI.

## Example

If you have this directory structure:
```
/home/user/projects/
  ├── my-script.ebs
  └── data.txt
```

And `my-script.ebs` contains:
```ebs
var handle: string = call file.open(path="data.txt", mode="r");
```

The file `data.txt` will be found because the "start in" directory is set to `/home/user/projects/`.

## Implementation Details

- File: `ScriptInterpreter/src/main/java/com/eb/ui/ebs/EbsTab.java`
- Lines modified: Around lines 488-495
- Added label to the buttons HBox
- No breaking changes to existing functionality
