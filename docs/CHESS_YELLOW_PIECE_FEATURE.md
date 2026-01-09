# Chess Yellow Piece Feature Implementation

## Overview
This feature implements yellow highlighting for chess pieces when they are clicked. When a player clicks on a piece, it changes from its original color (black or white) to yellow, providing clear visual feedback that the piece has been selected.

## Implementation Details

### 1. Created Yellow SVG Files
Created 12 new SVG files in `ScriptInterpreter/src/main/resources/images/chess/`:
- `yellow_white_pawn.svg`
- `yellow_white_rook.svg`
- `yellow_white_knight.svg`
- `yellow_white_bishop.svg`
- `yellow_white_queen.svg`
- `yellow_white_king.svg`
- `yellow_black_pawn.svg`
- `yellow_black_rook.svg`
- `yellow_black_knight.svg`
- `yellow_black_bishop.svg`
- `yellow_black_queen.svg`
- `yellow_black_king.svg`

Each yellow piece is a copy of the original piece with the fill color changed from:
- White pieces: `fill="#fff"` → `fill="#FFD700"` (gold/yellow)
- Black pieces: `fill="#000"` → `fill="#FFD700"` (gold/yellow)

The stroke colors remain black (`stroke="#000"`) to maintain piece definition and visibility.

### 2. Modified chess.ebs Script

#### Added Global Variables (lines 114-125)
```ebs
var yellowWhitePawn: imagedata;
var yellowWhiteRook: imagedata;
var yellowWhiteKnight: imagedata;
var yellowWhiteBishop: imagedata;
var yellowWhiteQueen: imagedata;
var yellowWhiteKing: imagedata;
var yellowBlackPawn: imagedata;
var yellowBlackRook: imagedata;
var yellowBlackKnight: imagedata;
var yellowBlackBishop: imagedata;
var yellowBlackQueen: imagedata;
var yellowBlackKing: imagedata;
```

#### Added Helper Function: getYellowPiece (lines 164-181)
Returns the appropriate yellow piece image based on piece type and color:
```ebs
function getYellowPiece(pieceType: int, pieceColor: int) return imagedata {
    if pieceColor == WHITE then {
        if pieceType == PAWN then { return yellowWhitePawn; }
        if pieceType == ROOK then { return yellowWhiteRook; }
        // ... other white pieces
    } else {
        if pieceType == PAWN then { return yellowBlackPawn; }
        if pieceType == ROOK then { return yellowBlackRook; }
        // ... other black pieces
    }
    return null;
}
```

#### Added Function: loadYellowPieces (lines 222-234)
Loads all 12 yellow piece images from disk:
```ebs
loadYellowPieces() {
    yellowWhitePawn = call image.load(imgPath + "yellow_white_pawn.svg");
    yellowWhiteRook = call image.load(imgPath + "yellow_white_rook.svg");
    // ... load all other yellow pieces
}
```

#### Added Function: setYellowPiece (lines 392-463)
Sets a yellow piece at a specific board position (x, y):
```ebs
setYellowPiece(x: int, y: int, yellowPiece: imagedata) {
    // Set the yellow piece based on position
    if y == 0 then {
        if x == 0 then { chessScreen.c00 = yellowPiece; }
        if x == 1 then { chessScreen.c01 = yellowPiece; }
        // ... handle all 64 board positions
    }
}
```

#### Modified handleCellClick Function (lines 512-516)
Added code to change the clicked piece to yellow:
```ebs
// Change the selected piece to yellow
var yellowPiece: imagedata = call getYellowPiece(cell.pieceType, cell.pieceColor);
if yellowPiece != null then {
    call setYellowPiece(x, y, yellowPiece);
    print "Changed piece to yellow";
}
```

#### Updated Main Program (line 1338)
Added call to load yellow pieces during initialization:
```ebs
print "Loading yellow piece images...";
call loadYellowPieces();
```

## How It Works

1. **Initialization**: When the chess application starts, it loads all the yellow piece SVG images into memory.

2. **Piece Selection**: When a player clicks on a piece:
   - The `handleCellClick` function is called with the clicked position (x, y)
   - The function checks if the piece belongs to the current player
   - If valid, it gets the appropriate yellow version of the piece using `getYellowPiece`
   - It then sets the yellow piece at that position using `setYellowPiece`
   - The piece visually changes from its original color to yellow on the board

3. **Visual Feedback**: The yellow color provides clear, immediate visual feedback that:
   - A piece has been selected
   - This is the active piece
   - The player can now see valid moves (indicated by green/red circles)

## Benefits

- **Clear Visual Feedback**: Players can instantly see which piece they've selected
- **Improved UX**: The yellow highlight makes the selected piece stand out from the rest
- **Consistent with Chess UI Standards**: Many chess applications use highlighting to show selected pieces
- **Non-intrusive**: The yellow color is bright enough to be noticeable but doesn't obscure the piece shape

## Testing

To test this feature:
1. Run the chess application: `mvn javafx:run` from the ScriptInterpreter directory
2. Click on any piece belonging to the current player (white starts first)
3. The piece should change from its original color to yellow
4. Valid moves will be shown with green circles (normal moves) or red circles (capture moves)
5. Click on another piece to select it (the new piece becomes yellow)

## Technical Notes

- The yellow color used is `#FFD700` (gold), which provides good contrast against both light and dark squares
- The implementation maintains the three-layer rendering system:
  - Bottom layer: checkerboard squares (b## variables)
  - Middle layer: move indicators (i## variables)
  - Top layer: chess pieces (c## variables)
- The yellow piece images are separate from the original pieces, so the original pieces remain unchanged
- The feature works with the existing move validation and piece selection logic
