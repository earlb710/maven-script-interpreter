# Four Cells Circle Drawing Example

## Overview
This example demonstrates a simple interactive JavaFX screen created with EBS scripting language. It features a 2x2 grid of canvas cells where clicking on any cell draws a blue circle in that cell.

## Features
- **4 Canvas Cells**: Arranged in a 2x2 grid layout using GridPane
- **Interactive**: Click on any cell to draw a circle
- **Visual Feedback**: Each cell has a light gray background and borders
- **Smooth Graphics**: Uses JavaFX Canvas with anti-aliasing for smooth circle rendering

## File Location
```
ScriptInterpreter/scripts/examples/four_cells_circle.ebs
```

## How to Run

### Option 1: From EBS Console (Recommended)
1. Start the EBS Console:
   ```bash
   cd ScriptInterpreter
   ./run.sh
   ```

2. In the console, type:
   ```
   /open scripts/examples/four_cells_circle.ebs
   ```

3. Press `Ctrl+Enter` to execute the script

4. A new window titled "Four Cells - Click to Draw Circle" will appear

### Option 2: From Command Line
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/examples/four_cells_circle.ebs
```

## Application Layout

```
┌─────────────────────────────────────────┐
│  Four Cells - Click to Draw Circle      │
├─────────────────────────────────────────┤
│                                         │
│  Click on any cell to draw a circle     │
│                                         │
│    ┌──────────┐  ┌──────────┐         │
│    │          │  │          │         │
│    │  Cell 1  │  │  Cell 2  │         │
│    │          │  │          │         │
│    └──────────┘  └──────────┘         │
│                                         │
│    ┌──────────┐  ┌──────────┐         │
│    │          │  │          │         │
│    │  Cell 3  │  │  Cell 4  │         │
│    │          │  │          │         │
│    └──────────┘  └──────────┘         │
│                                         │
└─────────────────────────────────────────┘
```

### After Clicking Cell 1:
```
┌─────────────────────────────────────────┐
│  Four Cells - Click to Draw Circle      │
├─────────────────────────────────────────┤
│                                         │
│  Click on any cell to draw a circle     │
│                                         │
│    ┌──────────┐  ┌──────────┐         │
│    │    ●●    │  │          │         │
│    │   ●  ●   │  │  Cell 2  │         │
│    │    ●●    │  │          │         │
│    └──────────┘  └──────────┘         │
│                                         │
│    ┌──────────┐  ┌──────────┐         │
│    │          │  │          │         │
│    │  Cell 3  │  │  Cell 4  │         │
│    │          │  │          │         │
│    └──────────┘  └──────────┘         │
│                                         │
└─────────────────────────────────────────┘
```

## Technical Details

### Canvas Size
- Each cell: 200x200 pixels
- Total window size: 440x480 pixels

### Colors
- **Background**: Dark gray (#2c2c2c)
- **Cell Background**: Light gray (#f0f0f0)
- **Circle Fill**: Blue (#3366ff)
- **Circle Stroke**: Dark blue (#0033cc, 3px width)
- **Cell Border**: Gray (#666666, 2px width)

### EBS Language Features Demonstrated

1. **Canvas Creation**: Using `canvas.create()` builtin
2. **Graphics Drawing**: Using `draw.rect()` and `draw.circle()` builtins
3. **Style Configuration**: Using `style.setFill()` and `style.setStroke()` builtins
4. **Screen Definition**: JSON-based screen layout with GridPane
5. **Event Handling**: onClick handlers for interactive behavior
6. **Functions**: Custom `drawCircleInCell()` function for reusable logic

### Code Structure

```ebs
// 1. Create canvas objects
var cell1: canvas = call canvas.create(200, 200, "cell1");

// 2. Initialize with background
call style.setFill(cell1, "#f0f0f0");
call draw.rect(cell1, 0, 0, 200, 200, true);

// 3. Define draw function
drawCircleInCell(cellCanvas: canvas) {
    // Clear background
    call style.setFill(cellCanvas, "#f0f0f0");
    call draw.rect(cellCanvas, 0, 0, 200, 200, true);
    
    // Draw circle
    call style.setFill(cellCanvas, "#3366ff");
    call draw.circle(cellCanvas, 100, 100, 60, true);
    
    // Draw stroke
    call style.setStroke(cellCanvas, "#0033cc", 3);
    call draw.circle(cellCanvas, 100, 100, 60, false);
}

// 4. Define screen with GridPane layout
screen fourCellsScreen = {
    "title": "Four Cells - Click to Draw Circle",
    "width": 440,
    "height": 480,
    "area": [/* ... */]
};

// 5. Assign canvases to screen variables
fourCellsScreen.canvas1 = cell1;

// 6. Show the screen
show screen fourCellsScreen;
```

## Learning Objectives

This example teaches:
- Basic JavaFX screen creation in EBS
- Canvas-based graphics programming
- Interactive event handling (onClick)
- Grid layout management (GridPane)
- Function definition and calling
- Style and drawing API usage

## Related Examples

- `scripts/examples/canvas_basics.ebs` - More canvas drawing examples
- `projects/Chess/chess.ebs` - Advanced GridPane usage with chess board

## Modifications

You can easily modify this example to:
- Change the number of cells (e.g., 3x3 grid)
- Draw different shapes (rectangles, triangles, etc.)
- Use different colors or sizes
- Add text labels to cells
- Implement a tic-tac-toe game
- Create a drawing pad

### Example: Change to 3x3 Grid

Simply create more canvas variables (cell5-cell9) and update the screen JSON to add more items with appropriate `layoutPos` values.

## Troubleshooting

**Issue**: Screen doesn't appear
- **Solution**: Make sure you executed the script with Ctrl+Enter, not just Enter

**Issue**: Circles don't appear when clicking
- **Solution**: Ensure the canvas variables are properly assigned to screen variables

**Issue**: Script fails to parse
- **Solution**: Check that JavaFX and all dependencies are properly installed

## Credits

Created as a demonstration of EBS scripting language capabilities for interactive GUI applications.
