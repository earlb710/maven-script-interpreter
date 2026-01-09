# Chess Piece Deselection Behavior

## Overview
When a chess piece is deselected (by clicking on blank space or another piece), the yellow highlight is removed and the piece returns to its normal color.

## Implementation Details

### Key Changes Made

1. **Normal Piece Image Storage**
   - Added 12 global variables to store original (normal) piece images
   - These are used to restore pieces to their original appearance after deselection

2. **Enhanced Image Loading**
   - `loadPieceImages()` function now loads both normal and yellow variants
   - Normal pieces: white_*.svg and black_*.svg
   - Yellow pieces: yellow_white_*.svg and yellow_black_*.svg

3. **New Helper Functions**
   - `getNormalPiece(pieceType, color)`: Returns the normal piece image for a given type and color
   - Mirrors the existing `getYellowPiece()` function

4. **Renamed Generic Function**
   - `setYellowPiece()` → `setPieceImage()`
   - Now handles setting any piece image (both yellow and normal)

5. **Updated Click Handler**
   - `handleCellClick()` now:
     1. Restores the previously selected piece to normal color (if any exists)
     2. Clears move indicators
     3. Processes the new click
     4. If selecting a new piece, changes it to yellow

## Behavior Flow

### Selecting First Piece
1. User clicks on a white pawn
2. Pawn changes from white to yellow
3. Valid moves are shown with green/red indicators

### Selecting Second Piece (Different Piece)
1. User clicks on a white knight
2. **Previously selected pawn is restored to normal white color**
3. Knight changes from white to yellow
4. Valid moves for knight are shown with green/red indicators

### Clicking Empty Space
1. User clicks on an empty square
2. **Previously selected piece is restored to normal color**
3. No new piece is selected
4. All indicators are cleared

### Clicking Same Piece
1. User clicks the currently selected piece
2. **Piece is restored to normal color**
3. Piece is deselected
4. All indicators are cleared

## Technical Notes

- The restoration happens at the beginning of `handleCellClick()`, before processing the new click
- This ensures smooth visual transitions without flickering
- The previous selection coordinates (`selectedX`, `selectedY`) are used to identify which piece needs restoration
- Empty cells (pieceType == EMPTY) are properly handled to avoid errors
