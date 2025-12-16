# Chess Yellow Piece Feature - Visual Guide

## Before and After

### Before Clicking
```
Original piece colors:
- White pieces: White fill (#fff) with black outline
- Black pieces: Black fill (#000) with black outline
```

### After Clicking
```
Selected piece becomes:
- Yellow/Gold fill (#FFD700) with black outline
- Same shape and outline as original
- Provides clear visual feedback
```

## Feature Flow

```
1. User clicks on a chess piece
          ↓
2. handleCellClick(x, y) is called
          ↓
3. Validates piece belongs to current player
          ↓
4. Gets yellow version: getYellowPiece(pieceType, pieceColor)
          ↓
5. Sets yellow piece: setYellowPiece(x, y, yellowPiece)
          ↓
6. Piece changes from original color to YELLOW on screen
          ↓
7. Valid moves shown with green/red indicators
```

## Color Scheme

### Original Colors
- **White Pieces**: RGB(255, 255, 255) - Pure White
- **Black Pieces**: RGB(0, 0, 0) - Pure Black
- **Piece Outline**: RGB(0, 0, 0) - Black stroke

### Yellow Highlight
- **Selected Piece**: RGB(255, 215, 0) - Gold/Yellow (#FFD700)
- **Piece Outline**: RGB(0, 0, 0) - Black stroke (unchanged)

## Example Piece Transformation

### White Knight - Before Click
```svg
<path d="..." fill="#fff" stroke="#000" stroke-width="1.5"/>
```

### White Knight - After Click (Yellow)
```svg
<path d="..." fill="#FFD700" stroke="#000" stroke-width="1.5"/>
```

### Black Queen - Before Click
```svg
<g fill="#000" stroke="#000" stroke-width="1.5">
  <circle cx="6" cy="12" r="2.75"/>
  ...
</g>
```

### Black Queen - After Click (Yellow)
```svg
<g fill="#FFD700" stroke="#000" stroke-width="1.5">
  <circle cx="6" cy="12" r="2.75"/>
  ...
</g>
```

## User Experience Flow

1. **Initial State**: All pieces in original colors (black/white)
2. **Click Action**: User clicks on their piece (e.g., white pawn)
3. **Visual Change**: Pawn changes from white to yellow
4. **Move Indicators**: Green circles appear on valid move squares, red circles on capture squares
5. **Piece Remains Yellow**: The piece stays yellow while selected
6. **New Selection**: Clicking another piece makes that piece yellow (previous selection returns to normal)

## Integration with Existing Features

The yellow piece feature works alongside:
- **Move Validation**: Only current player's pieces can be selected
- **Move Indicators**: Green circles for normal moves, red for captures
- **Three-Layer System**:
  - Layer 1 (bottom): Checkerboard squares
  - Layer 2 (middle): Move indicators (green/red circles)
  - Layer 3 (top): Chess pieces (including yellow highlighted piece)

## Technical Implementation

### Image Loading
```ebs
// Load yellow versions of all 12 piece types
loadYellowPieces() {
    yellowWhitePawn = call image.load(imgPath + "yellow_white_pawn.svg");
    yellowWhiteRook = call image.load(imgPath + "yellow_white_rook.svg");
    // ... etc for all 12 pieces
}
```

### Piece Selection
```ebs
// When piece is clicked
var yellowPiece: imagedata = call getYellowPiece(cell.pieceType, cell.pieceColor);
if yellowPiece != null then {
    call setYellowPiece(x, y, yellowPiece);
}
```

### Board Position Mapping
The chess board uses a coordinate system:
- X-axis: 0-7 (columns a-h)
- Y-axis: 0-7 (rows 8-1 from top to bottom)
- Each position maps to a screen variable: c00, c01, ..., c77

Example:
- Position (0, 0) = Row 8, Column a = chessScreen.c00
- Position (4, 7) = Row 1, Column e = chessScreen.c74

## Visual Example (Text Representation)

### Before Selection
```
  a  b  c  d  e  f  g  h
8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜  8
7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟  7
6 .  .  .  .  .  .  .  .  6
5 .  .  .  .  .  .  .  .  5
4 .  .  .  .  .  .  .  .  4
3 .  .  .  .  .  .  .  .  3
2 ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙  2
1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖  1
  a  b  c  d  e  f  g  h
```

### After Clicking White Pawn at e2
```
  a  b  c  d  e  f  g  h
8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜  8
7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟  7
6 .  .  .  .  .  .  .  .  6
5 .  .  .  . 🟢 .  .  .  5  ← Green indicator (valid move)
4 .  .  .  . 🟢 .  .  .  4  ← Green indicator (valid move)
3 .  .  .  .  .  .  .  .  3
2 ♙ ♙ ♙ ♙ 🟡 ♙ ♙ ♙  2  ← YELLOW pawn (selected)
1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖  1
  a  b  c  d  e  f  g  h

Legend: 🟡 = Yellow selected piece
        🟢 = Valid move indicator
```

## Benefits for Users

1. **Immediate Feedback**: Instant visual confirmation of piece selection
2. **Clear Selection State**: No ambiguity about which piece is active
3. **Reduced Errors**: Less chance of moving the wrong piece
4. **Professional Look**: Matches common chess application patterns
5. **Accessibility**: High contrast yellow color is easy to see

## Testing Checklist

- [ ] Can select white pieces
- [ ] Can select black pieces
- [ ] Selected piece turns yellow
- [ ] Yellow color is #FFD700 (gold)
- [ ] Piece outline remains black
- [ ] Valid moves still show green indicators
- [ ] Capture moves still show red indicators
- [ ] Only current player's pieces can be selected
- [ ] Selecting different piece changes yellow highlight
- [ ] All 6 piece types work (pawn, rook, knight, bishop, queen, king)
- [ ] Works for both white and black pieces
