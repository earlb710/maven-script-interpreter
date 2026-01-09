# Chess Captured Pieces Display Feature

## Overview
This feature adds a visual display of captured chess pieces on both sides of the chess board. When a piece is captured during gameplay, it immediately appears in one of two vertical panels flanking the board.

## Visual Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Timer Display Area                              │
│             [White Timer]    |    [Black Timer]                        │
└─────────────────────────────────────────────────────────────────────────┘
┌────────────┬─────────────────────────────┬────────────┬────────────────┐
│    ⬜      │                             │    ⬛      │                │
│  Captured  │        Chess Board          │  Captured  │ Move History   │
│   White    │         (8x8)               │   Black    │                │
│  Pieces    │                             │  Pieces    │  - Notation    │
│            │  ┌─────────────────────┐   │            │  - Timestamps  │
│  [icon]    │  │   a  b  c  d  e  f  │   │  [icon]    │  - Players     │
│  [icon]    │  │ 8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜ │   │  [icon]    │                │
│  [icon]    │  │ 7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟ │   │  [icon]    │                │
│   ...      │  │ 6 · · · · · · · · │   │   ...      │                │
│            │  │ 5 · · · · · · · · │   │            │                │
│            │  │ 4 · · · · · · · · │   │            │                │
│  (50px)    │  │ 3 · · · · · · · · │   │  (50px)    │    (300px)     │
│  width     │  │ 2 ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙ │   │  width     │                │
│            │  │ 1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖ │   │            │                │
│            │  │   a  b  c  d  e  f  │   │            │                │
│            │  └─────────────────────┘   │            │                │
│            │       (624px)               │            │                │
└────────────┴─────────────────────────────┴────────────┴────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│                         Status Message Area                             │
└─────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│              [New Game]              [Close]                            │
└─────────────────────────────────────────────────────────────────────────┘
```

## How It Works

### Captured Piece Display Rules
1. **Left Panel (⬜)**: Displays WHITE pieces that have been captured by the Black player
2. **Right Panel (⬛)**: Displays BLACK pieces that have been captured by the White player

### Display Characteristics
- **Size**: Each captured piece is shown as a 40x40 pixel icon
- **Spacing**: 2px vertical spacing between pieces
- **Container**: 50px wide vertical container with dark gray background
- **Capacity**: Up to 16 pieces per side (all pawns: 8, and non-king pieces: 8)
- **Order**: Pieces appear in the order they were captured (top to bottom)

### Real-Time Updates
- Captured pieces appear immediately when a piece is taken
- Display updates automatically during both human and computer moves
- Display clears when starting a new game

## Technical Implementation

### Data Structures
```ebs
// Global arrays to track captured pieces (piece type stored as int)
var capturedWhitePieces: int[16];  // White pieces captured by black
var capturedWhiteCount: int = 0;    // Number of white pieces captured

var capturedBlackPieces: int[16];  // Black pieces captured by white
var capturedBlackCount: int = 0;    // Number of black pieces captured
```

### Key Functions

#### `addCapturedPiece(pieceType: int, pieceColor: int)`
Called when a piece is captured. Adds the piece to the appropriate tracking array and updates the display.

#### `updateCapturedPiecesDisplay()`
Updates all captured piece images on the screen by iterating through the tracking arrays and setting the appropriate screen variables.

#### `clearCapturedPieces()`
Resets the captured pieces display to empty. Called when starting a new game.

### Screen Variables
The feature uses 32 screen variables to display captured pieces:
- `capturedWhite0` through `capturedWhite15`: Left panel (white pieces)
- `capturedBlack0` through `capturedBlack15`: Right panel (black pieces)

### Integration Points
The feature is integrated into:
1. **Human player moves** (`handleCellClick()`) - Tracks captures during player moves
2. **Computer moves** (`makeComputerMove()`) - Tracks captures during AI moves
3. **Game initialization** (`handleStartGame()`) - Initializes empty display
4. **Game reset** (`resetGame()`) - Clears display for new games

## Example Usage

### Game Flow
1. **Start Game**: Captured pieces displays are empty
2. **First Capture**: White pawn captures black pawn
   - Black pawn icon appears in RIGHT panel (⬛)
3. **Second Capture**: Black knight captures white bishop
   - White bishop icon appears in LEFT panel (⬜)
4. **Continue Playing**: More captures accumulate in respective panels
5. **New Game**: Both panels clear and start fresh

### Visual Example (Mid-Game)
```
Left Panel (⬜)        Right Panel (⬛)
White Captured:       Black Captured:
- ♙ (pawn)           - ♟ (pawn)
- ♙ (pawn)           - ♟ (pawn)
- ♗ (bishop)         - ♞ (knight)
- ♘ (knight)         - ♝ (bishop)
```

## Testing Checklist

To verify the feature works correctly:

- [ ] Start a new 2-player game
- [ ] Make moves that result in captures
- [ ] Verify white pieces appear in the LEFT panel when captured
- [ ] Verify black pieces appear in the RIGHT panel when captured
- [ ] Verify pieces appear in the correct order (chronological)
- [ ] Click "New Game" and verify panels clear
- [ ] Start a 1-player game (vs computer)
- [ ] Verify captures work for both human and computer moves
- [ ] Test with different difficulty levels
- [ ] Verify display handles maximum captures (16 per side)

## Known Limitations

1. **Maximum Capacity**: Each side can display up to 16 captured pieces. In standard chess, this is sufficient (8 pawns + 8 major pieces, excluding kings).
2. **No Sorting**: Pieces appear in capture order, not grouped by type.
3. **Fixed Width**: The 50px width is fixed and cannot be adjusted via settings.

## Future Enhancements (Optional)

Possible improvements for future versions:
- Add piece counts or material advantage indicators
- Group captured pieces by type
- Add tooltips showing piece names
- Animate piece appearance when captured
- Make panel width configurable
- Add option to hide/show captured pieces panels
- Display material point value differences

## Files Modified

- `ScriptInterpreter/projects/Chess/chess-game.ebs`: Main implementation file
  - Added tracking arrays and functions
  - Modified screen definition
  - Integrated into game flow

## Dependencies

This feature uses existing chess game infrastructure:
- Normal piece images (already loaded)
- `scr.setvar()` builtin for updating display
- Transparent clear image for empty slots
- Existing color scheme variables

No new dependencies or external resources required.
