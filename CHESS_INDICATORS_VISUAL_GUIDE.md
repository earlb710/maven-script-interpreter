# Chess Move Indicators Visual Guide

## Overview
This document describes the visual indicators used in the chess game to show move validity and game states.

## Indicator Types

### 1. Green Circle - Normal Move
**Description**: Semi-transparent green circle (20px radius)
**Usage**: Indicates a valid move to an empty square
**Visual**: ● (green, transparent)
**Code**: `#00FF0080` fill, `#00AA00` 2px stroke

```
When a piece is selected, all empty squares it can legally move to 
will display a green circle indicator.
```

### 2. Red Circle - Capture Move
**Description**: Semi-transparent red circle (20px radius)
**Usage**: Indicates a valid capture move (enemy piece)
**Visual**: ● (red, transparent)
**Code**: `#FF000080` fill, `#AA0000` 2px stroke

```
When a piece is selected, all squares with enemy pieces it can 
legally capture will display a red circle indicator.
```

### 3. Yellow Circle - Checking Move
**Description**: Semi-transparent yellow circle (20px radius)
**Usage**: Indicates a move that would put opponent's king in check
**Visual**: ● (yellow, transparent)
**Code**: `#FFFF0080` fill, `#AAAA00` 2px stroke

```
When a piece is selected, moves that would check the opponent's 
king are marked with a yellow circle instead of green/red.
This helps players see checking opportunities.
```

### 4. Red Cross - Invalid Move / Game Over
**Description**: Red X drawn with 4px lines
**Usage**: 
- Indicates an invalid move (would expose own king to check)
- Marks the losing king in checkmate
- Marks both kings in stalemate
**Visual**: ✕ (red X)
**Code**: `#FF0000` 4px stroke, lines from (15,15)-(45,45) and (45,15)-(15,45)

```
When a piece is selected, moves that would illegally expose the 
player's own king to check are marked with a red cross.

After a move, if checkmate occurs, the losing king gets a red cross.
If stalemate occurs, both kings get red crosses.
```

## Move Classification Logic

When a player selects a piece, each valid move is classified as follows:

```
1. First, check if move would expose own king
   → YES: Mark with RED CROSS (invalid)
   → NO: Continue to step 2

2. Check if move captures enemy piece
   → YES: Continue to step 3 (might be checking capture)
   → NO: Continue to step 3

3. Check if move would put opponent's king in check
   → YES: Mark with YELLOW CIRCLE (checking move)
   → NO: Continue to step 4

4. Check if move captures enemy piece (from step 2)
   → YES: Mark with RED CIRCLE (capture)
   → NO: Mark with GREEN CIRCLE (normal move)
```

## Game State Indicators

### Checkmate
```
┌─────────────────────────┐
│  Status: Checkmate!     │
│  Winner: [Color] wins!  │
│                         │
│  Losing King: ✕ (red)   │
└─────────────────────────┘
```

### Stalemate
```
┌─────────────────────────┐
│  Status: Stalemate!     │
│  Game is a draw.        │
│                         │
│  Both Kings: ✕ (red)    │
└─────────────────────────┘
```

## Example Scenarios

### Scenario 1: Normal Piece Movement
```
When White selects a pawn:
- Forward 1 square: ● (green) - normal move
- Forward 2 squares (from start): ● (green) - normal move
- Diagonal capture: ● (red) - capture move
```

### Scenario 2: Pinned Piece (Cannot Move)
```
Setup:
  Black Rook ─── White Knight ─── White King
  
When White selects the Knight:
- All moves: ✕ (red cross) - would expose king to check
- Attempting any move: "Invalid move - would expose your king!"
```

### Scenario 3: Checking Opportunity
```
Setup:
  White Queen can move to check Black King
  
When White selects the Queen:
- Move to checking square: ● (yellow) - puts opponent in check
- Other moves: ● (green) or ● (red) - normal or capture
```

### Scenario 4: Checkmate Position
```
Final Position:
  Black King in corner: ✕ (red cross) - checkmate
  Status: "Checkmate! White wins!"
  
Game is over, no more moves can be made.
```

### Scenario 5: Stalemate Position
```
Final Position:
  White King: ✕ (red cross) - stalemate
  Black King: ✕ (red cross) - stalemate
  Status: "Stalemate! Game is a draw."
  
Game is over, neither player can win.
```

## Technical Details

### Indicator Layer (i00-i77)
- The chess board uses a 3-layer system
- Layer 2 (indicators) overlays the board and pieces
- Each cell has a dedicated indicator variable (i00 through i77)
- Indicators are cleared when a piece is deselected

### Indicator Creation (createIndicators function)
```ebs
// Green circle for normal moves
var greenCanvas: canvas = call canvas.create(60, 60, "green_indicator");
call canvas.clear(greenCanvas);
call style.setFill(greenCanvas, "#00FF0080");
call draw.circle(greenCanvas, 30, 30, 20, true);
call style.setStroke(greenCanvas, "#00AA00", 2.0);
call draw.circle(greenCanvas, 30, 30, 20, false);
greenIndicator = call canvas.toImage(greenCanvas);

// Red circle for capture moves
var redCanvas: canvas = call canvas.create(60, 60, "red_indicator");
call canvas.clear(redCanvas);
call style.setFill(redCanvas, "#FF000080");
call draw.circle(redCanvas, 30, 30, 20, true);
call style.setStroke(redCanvas, "#AA0000", 2.0);
call draw.circle(redCanvas, 30, 30, 20, false);
redIndicator = call canvas.toImage(redCanvas);

// Yellow circle for checking moves
var yellowCanvas: canvas = call canvas.create(60, 60, "yellow_indicator");
call canvas.clear(yellowCanvas);
call style.setFill(yellowCanvas, "#FFFF0080");
call draw.circle(yellowCanvas, 30, 30, 20, true);
call style.setStroke(yellowCanvas, "#AAAA00", 2.0);
call draw.circle(yellowCanvas, 30, 30, 20, false);
yellowCircleIndicator = call canvas.toImage(yellowCanvas);

// Red cross for invalid moves
var crossCanvas: canvas = call canvas.create(60, 60, "red_cross_indicator");
call canvas.clear(crossCanvas);
call style.setStroke(crossCanvas, "#FF0000", 4.0);
call draw.line(crossCanvas, 15, 15, 45, 45);
call draw.line(crossCanvas, 45, 15, 15, 45);
redCrossIndicator = call canvas.toImage(crossCanvas);
```

### Indicator Display Function
```ebs
setIndicator(x: int, y: int, indicatorType: string) {
    var indicator: imagedata;
    if indicatorType == "capture" then {
        indicator = redIndicator;
    } else if indicatorType == "check" then {
        indicator = yellowCircleIndicator;
    } else if indicatorType == "invalid" then {
        indicator = redCrossIndicator;
    } else {
        indicator = greenIndicator;  // "normal" or default
    }
    
    // Set the appropriate indicator variable (i00-i77) based on x,y position
    // ... (implementation details in chess-game.ebs)
}
```

## Benefits

1. **Visual Clarity**: Players can immediately see which moves are legal
2. **Learning Aid**: Helps players understand chess rules (pinned pieces, checks)
3. **Strategic Planning**: Yellow circles show checking opportunities
4. **Error Prevention**: Red crosses prevent illegal moves that expose the king
5. **Game State Awareness**: Clear visual indication of checkmate/stalemate

## Testing Checklist

- [ ] Green circles appear on valid empty squares
- [ ] Red circles appear on capturable enemy pieces
- [ ] Yellow circles appear on checking moves
- [ ] Red crosses appear on moves that would expose king
- [ ] Red crosses prevent illegal move execution
- [ ] Checkmate marks losing king with red cross
- [ ] Stalemate marks both kings with red crosses
- [ ] Status messages update correctly for game states
