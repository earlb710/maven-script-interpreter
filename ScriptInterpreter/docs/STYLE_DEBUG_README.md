# JavaFX Styling Debug Test

## Purpose
This standalone JavaFX application helps diagnose CSS styling issues by testing various combinations of:
- Font sizes (10px, 14px, 18px, 22px)
- Colors (red, blue, green, purple, custom hex codes)
- Bold text styling
- Italic text styling
- Combined styles
- Control widths

## How to Run

### Option 1: Using the shell script
```bash
cd ScriptInterpreter
./run-style-debug.sh
```

### Option 2: Using Maven
```bash
cd ScriptInterpreter
mvn clean compile
java -cp target/classes com.eb.ui.StyleDebugTest
```

### Option 3: Using JavaFX Maven plugin
```bash
cd ScriptInterpreter
mvn clean javafx:run -Djavafx.mainClass=com.eb.ui.StyleDebugTest
```

## What to Look For

If JavaFX styling is working correctly, you should see:

1. **Font Size Differences**: Text at 10px should be noticeably smaller than 22px
2. **Color Differences**: Red, blue, green, purple text should be clearly different colors
3. **Bold Text**: Bold text should appear thicker/heavier than regular text
4. **Italic Text**: Italic text should be slanted
5. **Control Width Differences**: TextFields with width 100, 200, 300 should be visibly different sizes

## If Styles Don't Work

If all the controls look the same (no size, color, or style differences), this indicates:

1. A JavaFX CSS application issue in the codebase
2. Styles being overwritten somewhere
3. Incorrect CSS syntax
4. Platform/version compatibility issues

## Sections Tested

1. Label Font Sizes
2. Label Colors
3. Label Bold/Italic
4. Label Combined Styles
5. TextField Font Sizes
6. TextField Colors
7. TextField Bold/Italic
8. TextField Combined Styles
9. TextField Width Tests
10. TextArea Styles
11. ComboBox Styles
12. Spinner Styles

## Files

- `StyleDebugTest.java`: The main test class
- `run-style-debug.sh`: Shell script to run the test easily
- `STYLE_DEBUG_README.md`: This file

## Expected Output

The console will print diagnostic messages indicating what to check visually.

## Troubleshooting

If the debug test works correctly but the EBS scripts don't:
- The issue is in how AreaItemFactory or ScreenFactory applies styles
- Check for style string concatenation issues
- Check for null/empty values not being handled
- Check for styles being applied before or after other operations

If the debug test also doesn't work:
- There may be a JavaFX platform/version issue
- CSS syntax may be incorrect
- Styles may need to be applied differently on your platform
