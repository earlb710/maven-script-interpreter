# En Passant Bug Fixes

## Summary
Fixed two critical bugs in the en passant implementation that prevented the feature from working correctly.

## Bug #1: Captured Pawn Not Removed

### Problem
When performing an en passant capture, the capturing pawn moved correctly to the diagonal square, but the opponent's pawn that should have been captured remained on the board.

### Root Cause
The en passant capture detection logic was executing AFTER the piece had already been moved:

```ebs
// OLD CODE (BUGGY)
// 1. Move piece from (fromX, fromY) to (toX, toY)
board[toIdx] = call encodeCellValue(...);  // Piece is now at destination

// 2. THEN check for en passant
var targetCellValue: int = call getPieceAt(toX, toY);  // Returns the piece we just moved!
var targetCell = ChessCell(targetCellValue);
if targetCell.pieceType == EMPTY then {  // This is NEVER true
    // En passant code never executes
}
```

At the time of the check, `getPieceAt(toX, toY)` returns the piece that was just moved there (not EMPTY), so the condition `targetCell.pieceType == EMPTY` is never true, and the en passant capture logic never executes.

### Fix
Check for en passant capture BEFORE moving the piece:

```ebs
// NEW CODE (FIXED)
// 1. Check for en passant BEFORE moving
var isEnPassantCapture: bool = false;
var capturedPawnX: int = -1;
var capturedPawnY: int = -1;

if fromCell.pieceType == PAWN && toX != fromX then {
    var targetCellValue: int = call getPieceAt(toX, toY);  // Still at original position
    if targetCellValue != -1 then {
        var targetCell = ChessCell(targetCellValue);
        if targetCell.pieceType == EMPTY then {  // Now correctly detects empty square
            isEnPassantCapture = true;
            capturedPawnX = toX;
            capturedPawnY = fromY;  // Store position of pawn to remove
        }
    }
}

// 2. Move the piece
board[toIdx] = call encodeCellValue(...);

// 3. If en passant, remove the captured pawn
if isEnPassantCapture then {
    var capturedPawnIdx: int = call boardIndex(capturedPawnY, capturedPawnX);
    board[capturedPawnIdx] = call encodeCellValue(..., EMPTY, ...);
    call setPieceImage(capturedPawnX, capturedPawnY, transparentClear);
    call addCapturedPiece(PAWN, capturedPawnCell.pieceColor);
}
```

### Changes Made
**File:** `chess-game.ebs`, function `movePiece()`

1. Moved en passant detection to execute BEFORE the piece is moved
2. Stored the captured pawn position in variables (`capturedPawnX`, `capturedPawnY`)
3. Performed the piece move
4. After the move, removed the captured pawn if en passant was detected

### Result
✅ En passant captures now correctly remove the opponent's pawn from the board
✅ Captured pawn appears in the captured pieces display
✅ Board state is correctly updated

## Bug #2: En Passant Shows Green Indicator Instead of Red

### Problem
When selecting a pawn that can perform en passant, two indicators appeared:
- Green circle on the forward empty square (correct)
- **Green circle** on the diagonal en passant capture square (should be red)

The en passant move was showing a green "normal move" indicator instead of a red "capture move" indicator.

### Root Cause
The indicator logic only checked if there was an enemy piece at the target square:

```ebs
// OLD CODE (BUGGY)
var targetCellValue: int = call getPieceAt(move.x, move.y);
var isCapture: bool = false;

if targetCellValue != -1 then {
    var targetCell = ChessCell(targetCellValue);
    if targetCell.pieceType != EMPTY then {  // En passant target is EMPTY
        isCapture = true;  // Never set for en passant
    }
}

if isCapture then {
    call setIndicator(move.x, move.y, "capture");  // Red
} else {
    call setIndicator(move.x, move.y, "normal");   // Green - WRONG for en passant
}
```

For en passant, the target square is empty (the captured pawn is at a different position), so `isCapture` remained `false` and the move showed a green indicator.

### Fix
Added special detection for en passant moves:

```ebs
// NEW CODE (FIXED)
var targetCellValue: int = call getPieceAt(move.x, move.y);
var isCapture: bool = false;

// Check for regular capture
if targetCellValue != -1 then {
    var targetCell = ChessCell(targetCellValue);
    if targetCell.pieceType != EMPTY then {
        isCapture = true;
    }
}

// Check for en passant capture
if !isCapture && cell.pieceType == PAWN && move.x != x then {
    // Pawn moving diagonally
    if targetCellValue != -1 then {
        var targetCell2 = ChessCell(targetCellValue);
        if targetCell2.pieceType == EMPTY then {
            // Diagonal move to empty square - check if it's en passant
            if enPassantTargetX == move.x && enPassantTargetY == y then {
                isCapture = true;  // Now correctly detected as capture
            }
        }
    }
}

// Now correctly shows red for en passant
if isCapture then {
    call setIndicator(move.x, move.y, "capture");  // Red for en passant
}
```

### Changes Made
**File:** `chess-game.ebs`, function `handleCellClick()`

Added logic to detect en passant moves:
1. Check if piece is a pawn
2. Check if move is diagonal (`move.x != x`)
3. Check if target square is empty
4. Check if target matches the en passant opportunity (`enPassantTargetX` and `enPassantTargetY`)
5. If all conditions met, mark as capture

### Result
✅ En passant moves now show red "capture" indicator
✅ Forward pawn moves still show green "normal" indicator
✅ Visual feedback correctly represents the move type

## Testing Verification

### Test Scenario
1. White pawn at e2
2. Black pawn at d4
3. White moves e2 → e4 (double move)
4. En passant opportunity created at e4
5. Click black pawn at d4

**Expected Results:**
- Green circle at d3 (forward move)
- **Red circle** at e3 (en passant capture)

**Before Fix:**
- Green circle at d3 ✅
- Green circle at e3 ❌ (should be red)
- After capture, white pawn remained at e4 ❌

**After Fix:**
- Green circle at d3 ✅
- Red circle at e3 ✅
- After capture, white pawn removed from e4 ✅

## Code Changes Summary

### movePiece() Function
```diff
+ // Check for en passant capture BEFORE moving the piece
+ var isEnPassantCapture: bool = false;
+ var capturedPawnX: int = -1;
+ var capturedPawnY: int = -1;
+ 
+ if fromCell.pieceType == PAWN && toX != fromX then {
+     var targetCellValue: int = call getPieceAt(toX, toY);
+     if targetCellValue != -1 then {
+         var targetCell = ChessCell(targetCellValue);
+         if targetCell.pieceType == EMPTY then {
+             isEnPassantCapture = true;
+             capturedPawnX = toX;
+             capturedPawnY = fromY;
+         }
+     }
+ }
+ 
  // Update the board array - move the piece
  board[toIdx] = call encodeCellValue(...);
  board[fromIdx] = call encodeCellValue(..., EMPTY, ...);
  
- // Check for en passant capture after moving (BUG - too late!)
- var targetCellValue: int = call getPieceAt(toX, toY);  // Returns moved piece
  
+ // Handle en passant capture if detected
+ if isEnPassantCapture then {
+     var capturedPawnIdx: int = call boardIndex(capturedPawnY, capturedPawnX);
+     board[capturedPawnIdx] = call encodeCellValue(..., EMPTY, ...);
+     call setPieceImage(capturedPawnX, capturedPawnY, transparentClear);
+     call addCapturedPiece(PAWN, capturedPawnCell.pieceColor);
+ }
```

### handleCellClick() Function
```diff
  var isCapture: bool = false;
  
  if targetCellValue != -1 then {
      var targetCell = ChessCell(targetCellValue);
      if targetCell.pieceType != EMPTY then {
          isCapture = true;
      }
  }
  
+ // Check for en passant capture
+ if !isCapture && cell.pieceType == PAWN && move.x != x then {
+     if targetCellValue != -1 then {
+         var targetCell2 = ChessCell(targetCellValue);
+         if targetCell2.pieceType == EMPTY then {
+             if enPassantTargetX == move.x && enPassantTargetY == y then {
+                 isCapture = true;
+             }
+         }
+     }
+ }
```

## Commit Information
- **Commit Hash:** f89dd9f
- **Files Modified:** `chess-game.ebs`
- **Lines Changed:** +52, -29
- **Build Status:** ✅ Compiles successfully

## Related Documentation
- `EN_PASSANT_IMPLEMENTATION.md` - Technical implementation details
- `EN_PASSANT_VISUAL_GUIDE.md` - Visual scenarios and diagrams
- `IMPLEMENTATION_SUMMARY.md` - Overall feature summary
