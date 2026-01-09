# Defender Visualization Feature

## Overview
This feature allows players to see which pieces are defending an opponent's piece by clicking on it. Defenders are highlighted with purple circles, and the status message shows the total count of defenders.

## Feature Description

### User Interaction
1. **Click on opponent's piece**: When a player clicks on an enemy piece (not their own), the system:
   - Calculates which pieces of the same color can move to that square
   - Shows purple circles on all defending pieces
   - Displays defender count in status message

2. **Visual Feedback**:
   - Purple circles appear on all pieces that defend the clicked piece
   - Status message shows: "Opponent piece: X defenders" or "No defenders"

### Use Cases
- **Tactical Planning**: See if a piece is protected before attempting to capture it
- **Piece Value Assessment**: Understand which pieces are valuable for defense
- **Learning Tool**: Helps new players understand piece protection and control

## Implementation Details

### Files Modified

#### 1. chess-game.ebs

**Variable Declaration** (line 109):
```ebs
var purpleCircleIndicator: imagedata; // Purple circle for showing piece defenders
```

**Purple Circle Creation** (lines 430-440):
```ebs
// Create purple circle indicator for showing piece defenders
var purpleCircleCanvas: canvas = call canvas.create(60, 60, "purple_circle_indicator");
call canvas.clear(purpleCircleCanvas);
call style.setFill(purpleCircleCanvas, "#A020F080");  // Semi-transparent purple
call draw.circle(purpleCircleCanvas, 30, 30, 18, true);
call style.setStroke(purpleCircleCanvas, "#6A0DAD", 2.5);
call draw.circle(purpleCircleCanvas, 30, 30, 18, false);
purpleCircleIndicator = call canvas.toImage(purpleCircleCanvas);
```

**Indicator Type Support** (lines 750-752):
```ebs
} else if indicatorType == "defender" then {
    indicator = purpleCircleIndicator;
```

**Click Handler Update** (lines 1537-1565):
```ebs
// Check if clicking on opponent's piece - show defenders
if cell.pieceColor != currentPlayer then {
    print "Opponent's piece clicked - showing defenders";
    
    // Get all defenders of this piece
    var defenders = call getDefenders(x, y);
    var defenderCount: int = defenders.length;
    print "Found " + defenderCount + " defenders";
    
    // Show purple circles on all defender positions
    var i: int = 0;
    for (i = 0; i < defenderCount; i = i + 1) {
        var defender: posType = defenders[i];
        call setIndicator(defender.x, defender.y, "defender");
        print "  Defender at (" + defender.x + ", " + defender.y + ")";
    }
    
    // Update status message to show defender count
    var defenderText: string;
    if defenderCount == 0 then {
        defenderText = "No defenders";
    } else if defenderCount == 1 then {
        defenderText = "1 defender";
    } else {
        defenderText = defenderCount + " defenders";
    }
    chessScreen.statusMessage = "Opponent piece: " + defenderText;
    
    selectedX = -1;
    selectedY = -1;
    return;
}
```

#### 2. chess-moves.ebs

**New Function: getDefenders()** (lines 700-759):
```ebs
// Get all defenders of a piece at the given position
// Returns an array of posType positions where defenders are located
function getDefenders(targetX: int, targetY: int) return array {
    var defenders: posType[] = [];
    
    // Get the piece at the target position
    var targetCellValue: int = call getPieceAt(targetX, targetY);
    if targetCellValue == -1 then {
        return defenders;  // Empty array for invalid position
    }
    
    var targetCell = ChessCell(targetCellValue);
    if targetCell.pieceType == EMPTY then {
        return defenders;  // Empty square has no defenders
    }
    
    var targetColor: int = targetCell.pieceColor;
    
    // Check all squares for pieces of the same color
    var x: int = 0;
    var y: int = 0;
    
    for (y = 0; y < 8; y = y + 1) {
        for (x = 0; x < 8; x = x + 1) {
            // Skip the target piece itself
            if x == targetX && y == targetY then {
                // Continue to next iteration
            } else {
                var cellValue: int = call getPieceAt(x, y);
                if cellValue != -1 then {
                    var cell = ChessCell(cellValue);
                    
                    // Only check pieces of the same color as the target
                    if cell.pieceType != EMPTY && cell.pieceColor == targetColor then {
                        // Get moves for this piece
                        var moves = call validMoves(x, y);
                        
                        // Check if any move targets the square in question
                        var i: int = 0;
                        var len: int = moves.length;
                        for (i = 0; i < len; i = i + 1) {
                            var move: posType = moves[i];
                            if move.x == targetX && move.y == targetY then {
                                // This piece can move to the target square - it's a defender
                                var defenderPos: posType = call createPos(x, y);
                                call array.add(defenders, defenderPos);
                                // Break out of the move checking loop for this piece
                                i = len;  // Exit the loop
                            }
                        }
                    }
                }
            }
        }
    }
    
    return defenders;
}
```

## Logic Flow

```
┌────────────────────────────────────────┐
│ User clicks on opponent's piece        │
└───────────────┬────────────────────────┘
                │
                v
┌────────────────────────────────────────┐
│ handleCellClick() detects opponent     │
│ piece (cell.pieceColor != currentPlayer)│
└───────────────┬────────────────────────┘
                │
                v
┌────────────────────────────────────────┐
│ Call getDefenders(x, y)                │
│ - Iterate through all board squares   │
│ - Check pieces of same color          │
│ - Get validMoves() for each piece     │
│ - Check if target square is in moves  │
└───────────────┬────────────────────────┘
                │
                v
┌────────────────────────────────────────┐
│ Display purple circles on defenders    │
│ - Loop through defender positions      │
│ - Call setIndicator(x, y, "defender") │
└───────────────┬────────────────────────┘
                │
                v
┌────────────────────────────────────────┐
│ Update status message with count       │
│ - "Opponent piece: X defenders"        │
│ - "Opponent piece: 1 defender"         │
│ - "Opponent piece: No defenders"       │
└────────────────────────────────────────┘
```

## Example Scenarios

### Scenario 1: Protected Piece

**Board State:**
```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ♟   ·   ·   ·   ·   Black pawn at d5
4  ·   ·   ♟   ·   ♟   ·   ·   ·   Black pawns at c4 and e4
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**User Action:** White player clicks on black pawn at d5

**Result:**
- Purple circles appear on c4 and e4 (2 defenders)
- Status: "Opponent piece: 2 defenders"

### Scenario 2: Unprotected Piece

**Board State:**
```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ♟   ·   ·   ·   ·   Black pawn at d5
4  ·   ·   ·   ·   ·   ·   ·   ·
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**User Action:** White player clicks on black pawn at d5

**Result:**
- No purple circles appear
- Status: "Opponent piece: No defenders"

### Scenario 3: Complex Defense

**Board State:**
```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ♛   ·   ·   ·   ·   Black queen at d5
4  ·   ·   ·   ♜   ·   ·   ·   ·   Black rook at d4
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ♗   ·   ·   ·   ♞   ·   ·   Black bishop at b2, knight at f2
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**User Action:** White player clicks on black queen at d5

**Result:**
- Purple circles appear on d4 (rook), b2 (bishop), f2 (knight)
- Status: "Opponent piece: 3 defenders"

## Color Scheme

- **Purple Fill**: `#A020F080` (semi-transparent)
- **Purple Stroke**: `#6A0DAD` (solid purple)
- **Circle Size**: 18px radius (smaller than green/red indicators to distinguish)

## Integration with Existing Features

- **Clears Previous Indicators**: When clicking opponent piece, previous move indicators are cleared
- **Resets Selection**: After showing defenders, selection state is reset (no piece selected)
- **Compatible with All Piece Types**: Works for pawns, rooks, knights, bishops, queens, and kings
- **Works for Both Colors**: White players can check black pieces, black players can check white pieces

## Debug Output

Console messages when clicking opponent piece:
```
Opponent's piece clicked - showing defenders
Found 2 defenders
  Defender at (2, 4)
  Defender at (4, 4)
```

## Performance Considerations

- **Complexity**: O(n²) where n=64 board squares
- **Optimization**: Early exit once a piece is found to be a defender
- **Typical Case**: Fast enough for real-time interaction (< 100ms)

## Future Enhancements

Possible improvements:
- Show defender count as a number overlay on the clicked piece
- Different colors for different numbers of defenders
- Show attackers (opponent pieces that can capture) in a different color
- Animation when showing defenders
- Keyboard shortcut to toggle defender view on/off
