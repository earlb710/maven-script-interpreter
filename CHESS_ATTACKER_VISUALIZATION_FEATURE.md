# Chess Attacker Visualization Feature

## Overview
When clicking on an opponent's piece in the chess game, the UI now shows both defenders and attackers with colored circle indicators.

## Visual Indicators

### Purple Circles (Defenders)
- **Color**: Purple (#A020F080)
- **Meaning**: Pieces of the SAME color as the clicked piece that can protect it
- **Example**: If you click a black rook, purple circles show other black pieces that defend the rook

### Orange Circles (Attackers)  
- **Color**: Orange (#FFA50080)
- **Meaning**: Pieces of the CURRENT PLAYER's color that can capture the clicked piece
- **Example**: If white is the current player and clicks a black rook, orange circles show white pieces that can attack the rook

## How It Works

1. **Click on opponent piece**: Click any piece that belongs to your opponent
2. **Visual feedback**: The piece is highlighted with a gray overlay
3. **Defenders shown**: Purple circles appear on squares with pieces that defend the clicked piece
4. **Attackers shown**: Orange circles appear on squares with your pieces that can attack it
5. **Status message**: Bottom of screen shows: "Opponent piece: X defenders, Y attackers"

## Example Scenarios

### Scenario 1: Well-Defended Piece
```
Click on: Black Queen at d8
Purple circles: Black Bishop at c7, Black Knight at f7 (defending the queen)
Orange circles: White Bishop at g5, White Knight at b6 (attacking the queen)
Status: "Opponent piece: 2 defenders, 2 attackers"
```

### Scenario 2: Undefended Piece
```
Click on: Black Pawn at h6
Purple circles: None (no defenders)
Orange circles: White Pawn at g5, White Rook at h1 (can attack it)
Status: "Opponent piece: No defenders, 2 attackers"
```

### Scenario 3: Safe Piece
```
Click on: Black Knight at b8
Purple circles: Black Pawn at a7, Black Pawn at c7 (defending)
Orange circles: None (no attackers can reach it)
Status: "Opponent piece: 2 defenders, no attackers"
```

## Implementation Details

### Code Changes

**File**: `chess-game.ebs`

1. **Added "attacker" indicator type** (line 878):
   - Updated `setIndicator()` function to handle "attacker" type
   - Uses existing orange circle indicator (shared with "capture_check")

2. **Enhanced opponent piece click handler** (lines 1739-1790):
   - Calls `getDefenders()` to find defending pieces (purple circles)
   - Calls `getAttackers()` to find attacking pieces (orange circles)
   - Updates status message to show both counts

### Functions Used

- **`getDefenders(x, y)`**: Returns array of positions for pieces that defend position (x,y)
- **`getAttackers(x, y, color)`**: Returns array of positions for pieces of given color that can attack position (x,y)
- **`setIndicator(x, y, "defender")`**: Shows purple circle at position
- **`setIndicator(x, y, "attacker")`**: Shows orange circle at position

## Benefits

1. **Better visualization**: See both threats and protection at a glance
2. **Strategic planning**: Identify vulnerable opponent pieces to attack
3. **Tactical awareness**: Understand which pieces are well-defended
4. **Learning tool**: Helps new players understand piece control and protection

## Color Coding Summary

| Indicator Color | Meaning | Use Case |
|----------------|---------|----------|
| 🟣 Purple | Defenders | Shows which pieces protect the clicked opponent piece |
| 🟠 Orange | Attackers | Shows which of your pieces can capture the clicked opponent piece |
| 🔴 Red | Capture move | Shows where you can capture when your piece is selected |
| 🟢 Green | Normal move | Shows where you can move when your piece is selected |
| 🟡 Yellow | Check | Shows pieces giving check to the king |
| 🔵 Blue | Last move | Shows the endpoints of the last move made |

## Technical Notes

- Both orange circles (attackers) and orange circles (capture_check) use the same `orangeCircleIndicator` image
- Indicators are overlays on top of the chess board and pieces
- Clicking elsewhere on the board clears all indicators
- The feature works for both human vs human and human vs computer games
