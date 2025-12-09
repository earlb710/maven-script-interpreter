# Chess Piece Move Functions

## Overview

The Chess.ebs file has been enhanced with move calculation functions for each chess piece type. These functions calculate all theoretically possible moves for a piece at a given position, without considering the current board state.

## Function Signatures

All functions take the same three parameters:
- `x` (int): X-coordinate (0-7) of the piece position
- `y` (int): Y-coordinate (0-7) of the piece position  
- `direction` (string): Direction of movement - "up" or "down" (primarily used for pawns)

All functions return a JSON array containing position objects with `x` and `y` fields.

## Available Functions

### getPawnMoves(x: int, y: int, direction: string)
Calculates possible pawn moves including:
- Forward movement (1 square)
- Initial double move (2 squares from starting position)
- Diagonal capture moves

**Direction parameter:**
- "up": For white pawns moving up the board (decreasing y)
- "down": For black pawns moving down the board (increasing y)

### getRookMoves(x: int, y: int, direction: string)
Calculates all possible rook moves in horizontal and vertical directions.
Returns all positions in four directions: up, down, left, right.

### getKnightMoves(x: int, y: int, direction: string)
Calculates all possible knight L-shaped moves.
Returns up to 8 possible positions (2 squares in one direction, 1 in perpendicular).

### getBishopMoves(x: int, y: int, direction: string)
Calculates all possible bishop diagonal moves.
Returns all positions in four diagonal directions.

### getQueenMoves(x: int, y: int, direction: string)
Calculates all possible queen moves by combining rook and bishop moves.
Returns all positions in horizontal, vertical, and diagonal directions.

### getKingMoves(x: int, y: int, direction: string)
Calculates all possible king moves (one square in any direction).
Returns up to 8 possible positions around the current position.

## Usage Example

```javascript
// Get possible moves for a white pawn at position (3, 6)
var pawnMoves: json = call getPawnMoves(3, 6, "up");

// Get possible moves for a rook at position (0, 0)
var rookMoves: json = call getRookMoves(0, 0, "up");

// Get possible moves for a knight at position (4, 4)
var knightMoves: json = call getKnightMoves(4, 4, "up");

// Iterate through the moves
var len: int = call json.length(pawnMoves);
var i: int;
for (i = 0; i < len; i++) {
    var move: json = call json.get(pawnMoves, i);
    var xPos: int = call json.getint(move, "x");
    var yPos: int = call json.getint(move, "y");
    print "Move to: (" + xPos + ", " + yPos + ")";
}
```

## Notes

1. **No Board State Checking**: These functions calculate theoretical moves only. They do not check:
   - Whether the destination square is occupied
   - Whether the move would put the king in check
   - Whether pieces are blocking the path

2. **Return Type**: Functions return JSON arrays (not typed arrays) due to EBS language limitations with function return types.

3. **Position Format**: All positions use 0-based indexing:
   - x: 0-7 (columns a-h)
   - y: 0-7 (rows 8-1, where 0 is row 8 and 7 is row 1)

4. **Direction Parameter**: While the direction parameter is required for all functions, it is primarily meaningful for pawn moves. Other pieces use it for consistency but don't depend on it for move calculation.

## Integration with Chess Game

These functions can be used to:
- Display possible moves when a piece is selected
- Validate user moves
- Implement computer AI move generation
- Calculate move legality (combined with board state checking)

To integrate with board state, you would need to:
1. Call the appropriate move function for the piece type
2. Filter the returned moves based on:
   - Occupied squares
   - Piece colors
   - Check/checkmate rules
   - Special moves (castling, en passant)
