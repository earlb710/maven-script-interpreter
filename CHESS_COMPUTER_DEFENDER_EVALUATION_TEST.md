# Chess Computer AI - Defender Evaluation Test

## Feature Description
Enhanced the easy difficulty computer player to evaluate defenders before capturing pieces.

### Logic Implemented
When the computer considers capturing a piece:
1. **Check for recapturers**: Use `getAttackers()` to find opponent pieces that could recapture at the destination square
2. **If NO recapturers**: Capture is safe - consider it
3. **If recapturers exist**: 
   - Find the least valuable recapturer (opponent will use cheapest piece)
   - Calculate net material gain: `captureValue - attackerValue`
   - Only capture if net gain >= 0 (we gain material or break even)
4. **Select best safe capture**: Among all safe captures, pick the highest value
5. **If no safe captures**: Make a random legal move

### Material Evaluation
The logic calculates net material gain as:
- **Gain**: Value of captured piece
- **Loss**: Value of attacking piece (if opponent recaptures)
- **Net Gain** = Gain - Loss

Examples:
- Knight (3) captures defended pawn (1): Net = 1 - 3 = -2 (BAD - don't capture)
- Knight (3) captures defended knight (3): Net = 3 - 3 = 0 (NEUTRAL - OK to capture)
- Knight (3) captures defended rook (5): Net = 5 - 3 = +2 (GOOD - capture!)
- Knight (3) captures undefended pawn (1): Net = 1 - 0 = +1 (GOOD - capture!)

### Piece Values
- Pawn: 1
- Knight: 3
- Bishop: 3
- Rook: 5
- Queen: 9
- King: 1000 (invaluable)

## Test Scenarios

### Scenario 1: Undefended Piece
**Setup**: Computer can capture an undefended pawn (value 1) with its knight (value 3)
**Expected**: Computer should capture the pawn (no recapturers, net gain = +1)

### Scenario 2: Defended Low-Value Piece
**Setup**: Computer can capture a defended pawn (value 1) with its knight (value 3)
- The pawn is defended by another pawn
**Expected**: Computer should NOT capture (net gain = 1-3 = -2, would lose material)

### Scenario 3: Defended Equal-Value Piece
**Setup**: Computer can capture a defended knight (value 3) with its bishop (value 3)
- The knight is defended by a pawn
**Expected**: Computer SHOULD capture (net gain = 3-3 = 0, equal trade is OK)

### Scenario 4: Defended Higher-Value Piece
**Setup**: Computer can capture a defended rook (value 5) with its knight (value 3)
- The rook is defended by a pawn
**Expected**: Computer SHOULD capture (net gain = 5-3 = +2, gains material)

### Scenario 5: Multiple Capture Options
**Setup**: Computer can:
- Option A: Capture undefended pawn (value 1) with knight
- Option B: Capture defended queen (value 9) with knight (value 3), queen defended by pawn
**Expected**: Computer should choose Option B (higher value capture: 9 vs 1, net gain = +6)

### Scenario 6: Multiple Defenders
**Setup**: Computer can capture a rook (value 5) with knight (value 3)
- Rook is defended by both a pawn (value 1) and a bishop (value 3)
**Expected**: Computer SHOULD capture (uses cheapest defender=pawn, net gain = 5-3 = +2)

## Manual Testing Instructions

1. **Start the chess application**:
   ```bash
   cd ScriptInterpreter
   mvn javafx:run
   ```

2. **Setup game**:
   - Select "1 Player" mode
   - Select "Easy" difficulty
   - Click "Start Game"

3. **Create test positions**:
   - Play moves to set up the test scenarios above
   - Observe computer's capture decisions
   - Verify it avoids bad trades and takes good/equal trades

4. **Verify behavior**:
   - Computer should avoid capturing defended pieces when it loses material
   - Computer should capture when the trade is equal or favorable
   - Computer should prefer higher-value safe captures

## Code Changes
- **File**: `ScriptInterpreter/projects/Chess/chess-game.ebs`
- **Function**: `selectBestMoveEasy` (lines 2648-2744)
- **Key additions**:
  - Get attacking piece type and value from move
  - Determine opponent color
  - Call `getAttackers()` to find potential recapturers
  - Find least valuable recapturer (opponent uses cheapest piece)
  - Calculate net material gain/loss
  - Only consider captures with net gain >= 0 as "safe"
  - Select highest value safe capture

### Algorithm Flow
```
For each legal move:
  If move captures a piece:
    captureValue = value of captured piece
    attackerValue = value of attacking piece
    
    recapturers = getAttackers(destination, opponentColor)
    
    If no recapturers:
      isSafe = true  (gain = captureValue)
    Else:
      minRecapturerValue = find cheapest recapturer
      netGain = captureValue - attackerValue
      isSafe = (netGain >= 0)
    
    If isSafe and captureValue > bestCaptureValue:
      bestCaptureMove = this move

Return bestCaptureMove or random move if no safe captures
```
