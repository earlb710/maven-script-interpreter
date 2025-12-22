# Chess Indicators Testing Guide

## Test Cases

### 1. Test Invalid Move Indicators (Red Cross)
**Scenario**: Select a piece that if moved, would expose the king to check
**Expected**: The move should be marked with a red cross indicator
**Steps**:
1. Run chess application
2. Start a 2-player game
3. Set up a position where moving a piece would expose the king
4. Select the piece
5. Verify red cross appears on squares that would expose the king

### 2. Test Checking Move Indicators (Yellow Circle)
**Scenario**: Select a piece that can put the opponent's king in check
**Expected**: The move should be marked with a yellow circle indicator
**Steps**:
1. Run chess application
2. Start a 2-player game
3. Move pieces to create checking opportunities
4. Select a piece that can check the opponent
5. Verify yellow circle appears on squares that would check the opponent's king

### 3. Test Normal Move Indicators (Green Circle)
**Scenario**: Select a piece with normal moves
**Expected**: Normal moves show green circle, captures show red circle
**Steps**:
1. Run chess application
2. Start a 2-player game
3. Select any piece
4. Verify green circles on empty squares, red circles on enemy pieces

### 4. Test Invalid Move Prevention
**Scenario**: Try to make a move that would expose the king
**Expected**: Move should be rejected with error message
**Steps**:
1. Set up position with exposed king scenario
2. Try to execute an invalid move
3. Verify error message appears and move is not executed

### 5. Test Checkmate Detection
**Scenario**: Create a checkmate position
**Expected**: Losing king marked with red cross, status shows checkmate
**Steps**:
1. Play game until checkmate
2. Verify losing king has red cross indicator
3. Verify status message shows checkmate

### 6. Test Stalemate Detection
**Scenario**: Create a stalemate position
**Expected**: Both kings marked with red cross, status shows stalemate
**Steps**:
1. Create stalemate position
2. Verify both kings have red cross indicators
3. Verify status message shows stalemate/draw

## Running the Tests

```bash
cd ScriptInterpreter
mvn javafx:run
```

Then:
1. Open the chess application from the startup dialog
2. Start a 2-player game
3. Follow test scenarios above
