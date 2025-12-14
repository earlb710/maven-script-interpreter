# Tic Tac Toe Game

A simple implementation of the classic Tic Tac Toe game where you play against a computer player.

## Game Features

- **Human vs Computer**: Play as 'X' against a computer player that plays as 'O'
- **Strategic AI**: The computer uses a strategic approach to make moves:
  1. Tries to win if possible
  2. Blocks the player from winning
  3. Takes the center square when available
  4. Prefers corner squares
  5. Takes any available square as a fallback
- **Interactive UI**: 3x3 grid of clickable buttons in a JavaFX window
- **Game Status**: Clear visual feedback showing whose turn it is and game results
- **New Game Button**: Easily restart the game at any time

## How to Play

1. **Start the Game**: 
   - Open the EBS Console (run `mvn javafx:run` from the ScriptInterpreter directory)
   - Use the menu: File → Open Project and select `projects/TicTacToe/project.json`
   - Or use the console command: `/open projects/TicTacToe/tictactoe.ebs`
   - Press Ctrl+Enter to run the script

2. **Make Your Move**: 
   - You play as 'X' and go first
   - Click any empty cell to place your mark
   - The computer will automatically make its move as 'O'

3. **Win Conditions**:
   - Get three marks in a row (horizontal, vertical, or diagonal) to win
   - If all cells are filled with no winner, the game is a draw

4. **Start Over**: Click the "New Game" button to reset the board and play again

## Game Rules

- Players alternate turns (you always go first as X)
- Once a cell is marked, it cannot be changed
- The first player to get three of their marks in a row wins
- If all 9 cells are filled without a winner, the game is a draw

## Technical Details

- **Language**: EBS (Earl Bosch Script)
- **UI Framework**: JavaFX screens with GridPane layout
- **Board Representation**: 9-element string array (positions 0-8)
- **AI Logic**: Multi-level strategic decision making with win/block detection

## Files

- `tictactoe.ebs` - Main game script with all game logic and UI
- `project.json` - Project configuration file
- `README.md` - This file

## Game Strategy Tips

The computer AI is smart, so here are some tips:
- Try to take the center square if you can
- Look for opportunities to create two winning lines at once (fork)
- Watch for the computer trying to block your winning moves
- Corner squares are strategically valuable

Enjoy the game!
