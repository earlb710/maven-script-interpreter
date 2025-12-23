# En Passant Implementation

## Overview
En passant ("in passing" in French) is a special pawn capture move in chess. This document describes how en passant has been implemented in the EBS Chess game.

## Chess Rules for En Passant

En passant is a pawn capture that can only occur immediately after an opponent moves a pawn two squares forward from its starting position, and that pawn lands beside the capturing pawn.

### Conditions for En Passant:
1. **Capturing pawn position**: The capturing pawn must be on its 5th rank (4th rank from the opponent's perspective)
   - For white: rank 5 (y=3 in the game's coordinate system where y=0 is top)
   - For black: rank 4 (y=4 in the game's coordinate system)

2. **Target pawn**: The opponent's pawn must have just moved two squares forward from its starting position

3. **Adjacent position**: The opponent's pawn must be horizontally adjacent to the capturing pawn

4. **Timing**: En passant can only be performed on the very next move; the opportunity expires if not taken immediately

5. **Capture motion**: The capturing pawn moves diagonally forward to the square the opponent's pawn passed over

## Implementation Details

### Tracking Variables
Two new global variables were added to `chess-game.ebs`:

```ebs
var enPassantTargetX: int = -1;  // X coordinate of pawn that can be captured en passant
var enPassantTargetY: int = -1;  // Y coordinate of pawn that can be captured en passant
```

A value of -1 indicates no en passant opportunity is currently available.

### Files Modified

#### 1. chess-game.ebs

**Variable declarations** (lines 144-147):
- Added `enPassantTargetX` and `enPassantTargetY` tracking variables

**resetGame() function** (lines 3319-3321):
- Reset en passant variables when starting a new game

**movePiece() function** (lines 1244-1301):
- **En passant capture detection** (lines 1244-1268): 
  - Detects when a pawn moves diagonally to an empty square (en passant capture)
  - Removes the captured pawn from the board and screen
  - Updates captured pieces display
  
- **En passant state reset** (lines 1270-1273):
  - Clears en passant opportunity after each move
  
- **Pawn double move detection** (lines 1275-1284):
  - Detects when a pawn moves two squares forward
  - Sets enPassantTargetX and enPassantTargetY for the next turn

#### 2. chess-moves.ebs

**getPawnMoves() function** (lines 95-121):
- **En passant move generation**:
  - Checks if pawn is on the correct rank for en passant
  - Verifies an en passant target exists
  - Confirms target pawn is horizontally adjacent
  - Adds the diagonal capture move to the valid moves list

### Logic Flow

1. **Pawn double move occurs**:
   - `movePiece()` detects pawn moved 2 squares
   - Sets `enPassantTargetX` and `enPassantTargetY`
   - Prints debug message: "Pawn double move detected - en passant available at (...)"

2. **Next player's turn**:
   - When showing valid moves for a pawn on the correct rank
   - `getPawnMoves()` checks for en passant opportunity
   - If conditions are met, adds diagonal capture to valid moves
   - Prints debug message: "En passant move available from (...) to (...)"

3. **En passant capture executed**:
   - User clicks on en passant destination square
   - `movePiece()` detects diagonal pawn move to empty square
   - Removes captured pawn from board and screen
   - Updates captured pieces display
   - Prints debug message: "En passant capture detected!"
   - Prints debug message: "Captured pawn at (...) via en passant"

4. **En passant state cleared**:
   - After any piece move, `enPassantTargetX` and `enPassantTargetY` reset to -1
   - En passant opportunity expires

### Coordinate System Notes

The chess board uses a coordinate system where:
- x: 0-7 (columns a-h)
- y: 0-7 (rows 8-1, with y=0 being rank 8)

For en passant:
- White pawns on rank 5 have y=3
- Black pawns on rank 4 have y=4

## Testing En Passant

### Manual Test Scenario 1: White Captures Black En Passant

1. White moves pawn from e2 to e3
2. Black moves pawn from d7 to d5 (double move) → en passant available at d5
3. White moves pawn from e3 to e4
4. Black moves pawn from f7 to f6
5. White moves pawn from e4 to e5
6. Black moves pawn from d5 to d4 (now adjacent to white pawn at e5)
7. Next black pawn double move...
8. Actually, easier: After step 2, white pawn at e3, black pawn just moved to d5
9. White pawn should be able to capture black pawn en passant if white pawn moves to e5 first

**Simplified scenario:**
1. Move pawns so white has a pawn on e5
2. Black moves pawn d7→d5 (double move, landing at d5)
3. White pawn at e5 can now capture en passant by moving to d6

### Manual Test Scenario 2: Black Captures White En Passant

1. Move pawns so black has a pawn on e4
2. White moves pawn d2→d4 (double move, landing at d4)
3. Black pawn at e4 can now capture en passant by moving to d3

### Verification Points

When testing, verify:
- ✓ Debug messages appear in console
- ✓ Green/red indicators show en passant as a valid move
- ✓ Captured pawn disappears from board
- ✓ Captured pawn appears in captured pieces display
- ✓ Move history shows the capture correctly
- ✓ En passant opportunity expires after one turn if not taken
- ✓ Game continues normally after en passant capture

## Debug Output

When en passant occurs, you should see console output like:
```
Pawn double move detected - en passant available at (3, 3)
En passant move available from (4, 3) to (3, 2)
En passant capture detected!
Captured pawn at (3, 3) via en passant
```

## Known Limitations

None currently known. The implementation follows standard chess rules for en passant.

## Future Enhancements

Possible future improvements:
- Visual indicator showing en passant opportunity (highlight the capturable pawn)
- Special notation in move history for en passant (e.p. suffix)
- Tutorial mode explaining en passant to users
