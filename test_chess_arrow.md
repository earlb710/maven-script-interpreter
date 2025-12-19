# Chess Arrow Feature - Test Plan

## Test Cases for Arrow Spanning

### Test Case 1: Vertical Move - Pawn Forward 2 Squares
**Move**: e2 to e4 (white pawn initial move)
- Start position: (4, 6)
- End position: (4, 4)
- Expected behavior:
  - Start marker at (4, 6)
  - Vertical line segment at (4, 5)
  - Arrow head pointing up at (4, 4)

### Test Case 2: Horizontal Move - Rook Moving Right
**Move**: a1 to h1 (white rook along first rank)
- Start position: (0, 7)
- End position: (7, 7)
- Expected behavior:
  - Start marker at (0, 7)
  - Horizontal line segments at (1,7), (2,7), (3,7), (4,7), (5,7), (6,7)
  - Arrow head pointing right at (7, 7)

### Test Case 3: Diagonal Move - Bishop Moving
**Move**: c1 to h6 (white bishop diagonal)
- Start position: (2, 7)
- End position: (7, 2)
- Expected behavior:
  - Start marker at (2, 7)
  - Diagonal line segments (NE-SW) at (3,6), (4,5), (5,4), (6,3)
  - Arrow head pointing NE at (7, 2)

### Test Case 4: Knight Move
**Move**: g1 to f3 (white knight L-shape)
- Start position: (6, 7)
- End position: (5, 5)
- Expected behavior:
  - Start marker at (6, 7)
  - No line segments (knight jumps)
  - Arrow head at (5, 5)

Note: For knight moves, the current implementation will try to draw intermediate segments, but since knights move in an L-shape (not straight line), we may need to enhance the logic to detect knight moves and skip intermediate segments.

### Test Case 5: Single Square Move
**Move**: e4 to e5 (pawn moving one square)
- Start position: (4, 4)
- End position: (4, 3)
- Expected behavior:
  - Start marker at (4, 4)
  - No intermediate cells
  - Arrow head pointing up at (4, 3)

### Test Case 6: Diagonal Move Southwest
**Move**: Queen from d4 to a1
- Start position: (3, 4)
- End position: (0, 7)
- Expected behavior:
  - Start marker at (3, 4)
  - Diagonal line segments (NW-SE) at (2,5), (1,6)
  - Arrow head pointing SW at (0, 7)

## How to Test

1. Start the chess game
2. Click "Start Game" in the startup dialog
3. Make a move by:
   - Clicking on a piece to select it
   - Clicking on a destination square
4. Observe the arrow drawn from start to end position
5. Verify:
   - Start position has a small circle marker
   - Intermediate cells show appropriate line segments
   - End position shows an arrow head pointing in the direction of movement
6. Make another move and verify the previous arrow is cleared

## Expected Visual Characteristics

### Start Marker
- Appearance: Small filled circle
- Size: 8px radius
- Color: Semi-transparent blue (#0080FFCC)
- Location: Center of the FROM cell

### Line Segments
- Appearance: Straight line through cell center
- Width: 5px
- Color: Semi-transparent blue (#0080FFCC)
- Direction: Matches move direction (horizontal, vertical, or diagonal)

### Arrow Head
- Appearance: Line with triangular point
- Width: 5px for line
- Color: Semi-transparent blue (#0080FFCC)
- Direction: Points toward destination (8 possible directions)
- Location: Center of the TO cell

## Known Limitations

### Knight Moves
The current implementation will attempt to draw line segments for knight moves, but knights don't move in straight lines. For an L-shaped knight move from (0,0) to (1,2), the algorithm would try to interpolate between these points, which doesn't accurately represent the knight's jump.

**Potential Fix**: Detect knight moves and only show start marker and arrow head, skipping intermediate line segments.

### Castling
Castling involves moving two pieces (king and rook). The current implementation shows the last piece moved. May need special handling to show both moves.

**Potential Fix**: Detect castling moves and draw two arrows (one for king, one for rook).

## Success Criteria

The feature is successful if:
1. ✓ All straight-line moves (horizontal, vertical, diagonal) show a connected arrow
2. ✓ Arrow clearly indicates move direction with arrow head
3. ✓ Arrow persists after the move until the next move
4. ✓ Previous arrow is properly cleared when a new move is made
5. ✓ Arrow components are visually smooth and aligned
6. ✓ Arrow color is distinct but doesn't obscure the board or pieces
7. ✓ Performance is not noticeably affected

## Manual Testing Procedure

### Setup
1. Navigate to ScriptInterpreter directory
2. Run: `mvn javafx:run`
3. Wait for the chess startup dialog to appear
4. Select "2 Player" mode (easier to control both sides for testing)
5. Click "Start Game"

### Test Execution
For each test case:
1. Identify the piece to move
2. Click on the piece (it should turn yellow to indicate selection)
3. Click on the destination square
4. Observe the arrow drawn from start to finish
5. Take a mental note or screenshot of the arrow appearance
6. Make the next move and verify the previous arrow is cleared

### Validation
After testing all cases:
- Verify arrows are visible and clear
- Verify direction is unambiguous
- Verify no visual artifacts or glitches
- Verify arrows don't interfere with piece visibility
- Verify performance is smooth

## Automated Testing (Future)

For future automated testing, consider:
1. Creating unit tests for arrow calculation functions
2. Snapshot testing for arrow visual rendering
3. Integration tests for move sequences
4. Performance benchmarks for arrow drawing
