# En Passant Visual Guide

## Board Layout and Coordinates

```
   a   b   c   d   e   f   g   h
8  0   1   2   3   4   5   6   7   (y=0)  Rank 8 - Black pieces start
7  0   1   2   3   4   5   6   7   (y=1)  Rank 7 - Black pawns start
6  0   1   2   3   4   5   6   7   (y=2)  Rank 6
5  0   1   2   3   4   5   6   7   (y=3)  Rank 5 - White en passant rank ✓
4  0   1   2   3   4   5   6   7   (y=4)  Rank 4 - Black en passant rank ✓
3  0   1   2   3   4   5   6   7   (y=5)  Rank 3
2  0   1   2   3   4   5   6   7   (y=6)  Rank 2 - White pawns start
1  0   1   2   3   4   5   6   7   (y=7)  Rank 1 - White pieces start
```

## En Passant Scenario: White Captures Black

### Step 1: Initial Position
White pawn has advanced to e5 (x=4, y=3)
Black pawn is at d7 (x=3, y=1)

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ♟   ·   ·   ·   ·   Black pawn at d7
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ·   ♙   ·   ·   ·   White pawn at e5 (y=3) ✓
4  ·   ·   ·   ·   ·   ·   ·   ·
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

### Step 2: Black Pawn Double Move
Black moves pawn from d7 to d5 (x=3, y=1→3)
**En passant target set:** enPassantTargetX=3, enPassantTargetY=3

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·   (empty now)
6  ·   ·   ·   ^   ·   ·   ·   ·   (passed through d6)
5  ·   ·   ·   ♟   ♙   ·   ·   ·   Black at d5, White at e5 (adjacent!)
4  ·   ·   ·   ·   ·   ·   ·   ·
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**En passant now available!**
- White pawn at e5 (x=4, y=3) is on correct rank ✓
- Black pawn at d5 (x=3, y=3) is adjacent ✓
- Black pawn just moved two squares ✓
- En passant target exists (x=3, y=3) ✓

### Step 3: White Captures En Passant
White pawn moves from e5 to d6 (x=4,y=3 → x=3,y=2)
**Diagonal move to empty square detected** → En passant capture!

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ♙   ·   ·   ·   ·   White pawn now at d6
5  ·   ·   ·   ✗   ·   ·   ·   ·   Black pawn REMOVED (was at d5)
4  ·   ·   ·   ·   ·   ·   ·   ·
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**Result:**
- Black pawn at d5 removed from board ✓
- White pawn moved to d6 (the square black pawn passed through) ✓
- Captured piece added to display ✓
- enPassantTargetX and enPassantTargetY reset to -1 ✓

## En Passant Scenario: Black Captures White

### Step 1: Initial Position
Black pawn has advanced to e4 (x=4, y=4)
White pawn is at d2 (x=3, y=6)

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ·   ·   ·   ·   ·
4  ·   ·   ·   ·   ♟   ·   ·   ·   Black pawn at e4 (y=4) ✓
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ♙   ·   ·   ·   ·   White pawn at d2
1  ·   ·   ·   ·   ·   ·   ·   ·
```

### Step 2: White Pawn Double Move
White moves pawn from d2 to d4 (x=3, y=6→4)
**En passant target set:** enPassantTargetX=3, enPassantTargetY=4

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   v   ·   ·   ·   ·   (passed through d3)
4  ·   ·   ·   ♙   ♟   ·   ·   ·   White at d4, Black at e4 (adjacent!)
3  ·   ·   ·   ·   ·   ·   ·   ·
2  ·   ·   ·   ·   ·   ·   ·   ·   (empty now)
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**En passant now available!**
- Black pawn at e4 (x=4, y=4) is on correct rank ✓
- White pawn at d4 (x=3, y=4) is adjacent ✓
- White pawn just moved two squares ✓
- En passant target exists (x=3, y=4) ✓

### Step 3: Black Captures En Passant
Black pawn moves from e4 to d3 (x=4,y=4 → x=3,y=5)
**Diagonal move to empty square detected** → En passant capture!

```
   a   b   c   d   e   f   g   h
8  ·   ·   ·   ·   ·   ·   ·   ·
7  ·   ·   ·   ·   ·   ·   ·   ·
6  ·   ·   ·   ·   ·   ·   ·   ·
5  ·   ·   ·   ·   ·   ·   ·   ·
4  ·   ·   ·   ✗   ·   ·   ·   ·   White pawn REMOVED (was at d4)
3  ·   ·   ·   ♟   ·   ·   ·   ·   Black pawn now at d3
2  ·   ·   ·   ·   ·   ·   ·   ·
1  ·   ·   ·   ·   ·   ·   ·   ·
```

**Result:**
- White pawn at d4 removed from board ✓
- Black pawn moved to d3 (the square white pawn passed through) ✓
- Captured piece added to display ✓
- enPassantTargetX and enPassantTargetY reset to -1 ✓

## Code Flow Diagram

```
┌─────────────────────────────────────────────┐
│ Pawn moves two squares (e.g., d2 → d4)    │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ movePiece() detects double move            │
│ - yDiff == 2 or yDiff == -2                │
│ - Sets enPassantTargetX = toX              │
│ - Sets enPassantTargetY = toY              │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ Opponent's turn - selects adjacent pawn    │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ getPawnMoves() generates valid moves       │
│ - Checks if pawn is on correct rank       │
│ - Verifies enPassantTarget exists         │
│ - Confirms target is adjacent              │
│ - Adds diagonal capture move               │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ User sees green indicator on capture square│
│ (e.g., d3 for black capturing white at d4) │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ User clicks on en passant capture square   │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ movePiece() detects en passant capture     │
│ - Pawn moved diagonally to empty square   │
│ - Removes captured pawn at (toX, fromY)   │
│ - Updates screen and captured pieces      │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│ movePiece() resets en passant state        │
│ - enPassantTargetX = -1                    │
│ - enPassantTargetY = -1                    │
└─────────────────────────────────────────────┘
```

## Key Implementation Points

### Variable States

**No en passant available:**
```ebs
enPassantTargetX = -1
enPassantTargetY = -1
```

**En passant available (white pawn at d5):**
```ebs
enPassantTargetX = 3  // d-file
enPassantTargetY = 3  // rank 5 (y=3)
```

### Rank Calculation

```ebs
// White pawns on their 5th rank can capture en passant
if color == WHITE then {
    enPassantRank = 3;  // y=3 is rank 5
}

// Black pawns on their 4th rank can capture en passant
if color == BLACK then {
    enPassantRank = 4;  // y=4 is rank 4
}
```

### Move Direction

```ebs
// White moves up (decreasing y)
if color == WHITE then {
    direction = -1;  // y decreases
}

// Black moves down (increasing y)
if color == BLACK then {
    direction = 1;   // y increases
}
```

## Legend

- ♙ = White Pawn
- ♟ = Black Pawn
- · = Empty Square
- ✗ = Captured/Removed Piece
- ^ = Square passed through (upward)
- v = Square passed through (downward)
- ✓ = Condition satisfied
