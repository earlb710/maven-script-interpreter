# Chess Board 3-Layer Implementation

## Summary

The chess board has been restructured to use a proper 3-layer system for better visual organization and easier management of board elements.

## Layer Structure

### Layer 1: Bottom Layer - Board Squares (b00-b77)
- **Purpose**: Display the checkerboard pattern
- **Variables**: 64 imagedata variables named b00 through b77
- **Content**: Each square contains either a light or dark colored square image created from canvas
- **Characteristics**: 
  - These are the base layer and provide the background colors
  - Clickable for user interaction
  - Size: 60x60 pixels
  - Generated programmatically using canvas with the COLOR_CELL_LIGHT and COLOR_CELL_DARK colors

### Layer 2: Middle Layer - Indicators (i00-i77)
- **Purpose**: Show possible moves, captures, and other visual indicators
- **Variables**: 64 imagedata variables named i00 through i77
- **Content**: Semi-transparent colored circles (green for moves, red for captures)
- **Characteristics**:
  - Overlaid on top of the board squares
  - Mouse-transparent (pickOnBounds: false, style: "-fx-mouse-transparent: true")
  - Size: 50x50 pixels
  - Can be easily cleared by setting all i## variables to null
  - The `clearIndicators()` function provides easy management

### Layer 3: Top Layer - Chess Pieces (c00-c77)
- **Purpose**: Display the chess pieces at their current positions
- **Variables**: 64 imagedata variables named c00 through c77
- **Content**: SVG images of chess pieces loaded from resources
- **Characteristics**:
  - Overlaid on top of indicators
  - Mouse-transparent (pickOnBounds: false, style: "-fx-mouse-transparent: true")
  - Size: 50x50 pixels
  - Can be set to null for empty squares

## Implementation Details

### Screen Definition
Each cell in the chess board now has three UI elements stacked at the same layoutPos:
```
{"name": "board##", "layoutPos": "row,col", "varRef": "b##", ...}  // Bottom layer
{"name": "ind##", "layoutPos": "row,col", "varRef": "i##", ...}    // Middle layer
{"name": "cell##", "layoutPos": "row,col", "varRef": "c##", ...}   // Top layer
```

### Functions Added/Modified

1. **createBoardLayer()** - NEW
   - Creates the light and dark square images using canvas
   - Generates 60x60 pixel squares with the appropriate background colors
   - Stores images in `lightSquare` and `darkSquare` variables

2. **createIndicators()** - UNCHANGED
   - Creates the green and red circular indicators for showing valid moves

3. **clearIndicators()** - UNCHANGED
   - Clears all indicator layer variables by setting them to null
   - This is the key benefit of the middle layer - easy clearing without affecting other layers

### Initialization Sequence

The initialization now follows this order:
1. Initialize the chess board data structure (bitmap array)
2. Create board layer images (checkerboard squares)
3. Create indicator images (move markers)
4. Initialize all 64 board layer variables with the checkerboard pattern
5. Load chess piece SVG images for starting positions
6. Show the screen

### Benefits of 3-Layer Approach

1. **Clear Separation of Concerns**
   - Board appearance (bottom layer)
   - Temporary indicators (middle layer)
   - Game pieces (top layer)

2. **Easy Management**
   - Clear indicators without affecting pieces or board
   - Move pieces without affecting indicators or board
   - Change board appearance without affecting pieces or indicators

3. **Visual Clarity**
   - Proper layering ensures correct z-order
   - Semi-transparent indicators work correctly on colored backgrounds
   - Clean visual hierarchy

4. **Extensibility**
   - Easy to add new indicator types (highlights, threats, etc.)
   - Can modify board appearance dynamically
   - Supports future features like board themes

## Usage

### Clearing the Middle Layer (Indicators)
```ebs
call clearIndicators();  // Clears all move indicators
```

### Setting an Indicator
```ebs
call setIndicator(x, y, isCapture);  // Shows a move indicator at position (x,y)
```

### Moving a Piece
To move a piece from one square to another:
1. Clear the source square: `chessScreen.c[source] = null;`
2. Set the destination square: `chessScreen.c[dest] = pieceImage;`
3. The board layer remains unchanged throughout

### Updating Board Appearance
If needed, individual board squares can be changed:
```ebs
chessScreen.b00 = customSquareImage;  // Change a specific square
```

## File Changes

- **ScriptInterpreter/projects/Chess/chess.ebs**
  - Added 64 board layer variables (b00-b77) to screen vars
  - Added 32 piece layer variables for middle rows (c20-c57)
  - Added `createBoardLayer()` function
  - Updated screen definition for all 64 cells with 3-layer structure
  - Added board layer initialization code
  - Made all cells clickable (previously only piece cells were clickable)

## Testing

The implementation compiles successfully. To test the visual appearance and functionality:
1. Run the chess application: `mvn javafx:run`
2. Open the chess.ebs script
3. Click on pieces to see valid move indicators (middle layer)
4. Verify that board colors are displayed correctly (bottom layer)
5. Verify that chess pieces appear on top (top layer)
6. Test clicking on empty squares to verify clickability
