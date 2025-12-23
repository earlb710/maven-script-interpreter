# Chess.ebs Bug Fix - Manual Test Instructions

## Bug Description
Previously, the chess game did not always start with white. If a previous game ended with black's turn (currentPlayer = 1), the next game would incorrectly start with black moving first.

## Fix Applied
Added game state reset code at the beginning of `handleStartGame()` function in `chess-game.ebs` to ensure:
- `currentPlayer` is always set to WHITE (0) at game start
- All other game state variables are properly reset (gameOver, selections, move tracking, castling flags, en passant, captured pieces, move history)

## Test Steps

### Test 1: First Game Start
1. Run the chess application: `cd ScriptInterpreter && mvn javafx:run`
2. Click "Start Game" button
3. **Expected Result**: White should start first (status shows "White to move")
4. **Expected Result**: Move history should show "Game started.\nWhite's turn.\n"

### Test 2: Multiple Games - White Always Starts
1. Start a new game (1 player mode)
2. Make several moves (at least 3-4 moves so the turn changes multiple times)
3. Note whose turn it is before ending the game
4. Click "New Game" button or close and restart
5. **Expected Result**: Regardless of whose turn it was in the previous game, white should always start
6. Repeat steps 2-5 several times to confirm consistency

### Test 3: Computer Player - White Always Starts
1. Start a new game in 1 player mode
2. Observe the randomly assigned color:
   - If human is WHITE: Human moves first (white starts)
   - If human is BLACK: Computer (white) moves first automatically
3. **Expected Result**: White (either human or computer) always starts
4. Play until black's turn, then restart
5. **Expected Result**: New game still starts with white, not continuing from black

### Test 4: Game State Reset
1. Start a game and make several moves
2. Capture some pieces
3. Make moves that involve castling rights
4. Note the current game state (captured pieces, move history, timer)
5. Click "New Game"
6. **Expected Results**:
   - Board is reset to starting position
   - Captured pieces displays are cleared
   - Move history shows only initial message
   - All indicators are cleared
   - Current player is white
   - Timer is reset

### Test 5: 2 Player Mode
1. Select "2 Player" mode
2. Start game
3. **Expected Result**: White starts first
4. Make several moves alternating between white and black
5. End game when it's black's turn
6. Start a new game
7. **Expected Result**: White starts first (not black)

## Success Criteria
All tests pass with the following verified:
- ✅ White always starts first in every new game
- ✅ Computer player (if playing white) moves first automatically
- ✅ Game state is fully reset between games
- ✅ No carryover of previous game state (currentPlayer, selections, move history, etc.)

## Additional Verification
Check console output for debug messages:
- "You are playing as WHITE. White starts first." (if human is white)
- "You are playing as BLACK. White (computer) starts first." (if human is black)
- Status message should always show "White to move" at game start
