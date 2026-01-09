# Chess Arrow Indicator Test Plan

## Test Cases

### 1. Horizontal Moves
- **Test 1.1**: Rook from a1 (0,7) to h1 (7,7) - Right direction
  - Expected: Blue circle at a1, 6 right arrows, blue circle at h1
  
- **Test 1.2**: Rook from h1 (7,7) to a1 (0,7) - Left direction
  - Expected: Blue circle at h1, 6 left arrows, blue circle at a1

### 2. Vertical Moves
- **Test 2.1**: Rook from a1 (0,7) to a8 (0,0) - Up direction
  - Expected: Blue circle at a1, 6 up arrows, blue circle at a8
  
- **Test 2.2**: Rook from a8 (0,0) to a1 (0,7) - Down direction
  - Expected: Blue circle at a8, 6 down arrows, blue circle at a1

### 3. Diagonal Moves
- **Test 3.1**: Bishop from a1 (0,7) to h8 (7,0) - Up-Right diagonal
  - Expected: Blue circle at a1, 6 up-right arrows, blue circle at h8
  
- **Test 3.2**: Bishop from h8 (7,0) to a1 (0,7) - Down-Left diagonal
  - Expected: Blue circle at h8, 6 down-left arrows, blue circle at a1
  
- **Test 3.3**: Bishop from a8 (0,0) to h1 (7,7) - Down-Right diagonal
  - Expected: Blue circle at a8, 6 down-right arrows, blue circle at h1
  
- **Test 3.4**: Bishop from h1 (7,7) to a8 (0,0) - Up-Left diagonal
  - Expected: Blue circle at h1, 6 up-left arrows, blue circle at a8

### 4. Short Moves
- **Test 4.1**: Pawn from e2 (4,6) to e4 (4,4) - 2-square pawn move
  - Expected: Blue circle at e2, 1 up arrow at e3, blue circle at e4
  
- **Test 4.2**: King from e1 (4,7) to e2 (4,6) - Single square
  - Expected: Blue circle at e1, blue circle at e2 (no intermediate arrows)

### 5. Knight Moves
- **Test 5.1**: Knight from b1 (1,7) to c3 (2,5) - L-shape move
  - Expected: Blue circle at b1, up-right arrow at intermediate, blue circle at c3
  
Note: Knight moves use the directional arrow that best represents the overall direction since they don't move in straight lines.

### 6. Arrow Path Cleanup
- **Test 6.1**: Make move 1, then make move 2
  - Expected: Move 1 arrows should be cleared before move 2 arrows are drawn
  
- **Test 6.2**: Reset game
  - Expected: All arrow indicators should be cleared

## Code Review Checklist

### Logic Verification
- [x] Direction calculation (dx, dy) correctly determines movement direction
- [x] Arrow selection logic chooses correct arrow for each direction
- [x] Loop correctly iterates through intermediate positions
- [x] Path tracking correctly stores all arrow positions
- [x] Cleanup correctly removes all previous arrows

### Edge Cases
- [x] Single-cell moves (no intermediate cells)
- [x] Long moves (7 cells)
- [x] Diagonal moves with equal dx and dy
- [x] Knight moves (non-linear paths)
- [x] Game reset clears all arrows

### Integration Points
- [x] Compatible with existing 3-layer rendering system
- [x] Does not interfere with piece rendering
- [x] Does not interfere with valid move indicators (green/red circles)
- [x] Works with both player moves and computer moves

## Expected Visual Results

### Arrow Appearance
Each arrow indicator should:
- Be 60x60 pixels (matching cell size)
- Have semi-transparent blue color
- Have clear arrow head pointing in direction of movement
- Be centered in the cell
- Not overlap with pieces (due to layer separation)

### Circle Appearance
Endpoint circles should:
- Be semi-transparent blue
- Be smaller than the cell (diameter ~36 pixels)
- Be centered in the cell
- Clearly mark the start and end positions

## Implementation Details

### New Functions
1. `setCustomIndicator(x, y, indicator)` - Sets a custom indicator image at a position
2. Enhanced `showLastMove(fromX, fromY, toX, toY)` - Draws complete arrow path

### New Variables
- `blueCircleIndicator` - Circle for endpoints
- `blueArrowRight/Left/Up/Down` - Straight arrows
- `blueArrowUpRight/UpLeft/DownRight/DownLeft` - Diagonal arrows
- `arrowPathX[64]`, `arrowPathY[64]`, `arrowPathCount` - Path tracking

### Modified Functions
- `resetGame()` - Now resets arrow path tracking

## Manual Testing Instructions

Since automated UI testing is not available, manual testing should verify:

1. Start a new chess game
2. Make a horizontal move (e.g., rook from a1 to d1)
   - Verify blue circles appear at a1 and d1
   - Verify right arrows appear at b1 and c1
3. Make a vertical move (e.g., rook from d1 to d5)
   - Verify previous arrows are cleared
   - Verify new blue circles appear at d1 and d5
   - Verify up arrows appear at d2, d3, d4
4. Make a diagonal move (e.g., bishop from c1 to f4)
   - Verify diagonal arrows appear correctly
5. Make a knight move (e.g., knight from b1 to c3)
   - Verify directional arrow shows general direction
6. Click "New Game"
   - Verify all arrows are cleared
