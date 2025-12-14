# Board Game Design - Extended Monopoly-Style Board

## Overview

This board game features a 4-quadrant design, where each quadrant is the size of a standard Monopoly board. The START position is located in the center, and players can choose one of 4 directions when they land on or pass through START.

## Board Layout Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│ NORTH QUADRANT (40 spaces)                                               │
│                                                                           │
│  [40][39][38][37][36][35][34][33][32][31][30][29][28][27][26][25][24]  │
│   │                                                                  │    │
│  [41]                                                              [23]  │
│   │                                                                  │    │
│  [42]                                                              [22]  │
│   │                                                                  │    │
│  [43]                                                              [21]  │
│   │                  NORTH QUADRANT PROPERTIES                      │    │
│  [44]                  (Like Full Monopoly Board)                  [20]  │
│   │                                                                  │    │
│  [45]                                                              [19]  │
│   │                                                                  │    │
│  [46]                                                              [18]  │
│   │                                                                  │    │
│  [47]                                                              [17]  │
│   │                                                                  │    │
│  [48][49][50][51][52][53][54][55][56][57][58][59][60][61][62][63][64]  │
│                                                                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│ WEST [80]                                    [N1]                 [16] E │
│      [79]                                    [N2]                 [15] A │
│      [78]                                    [N3]                 [14] S │
│      [77]                                    [N4]                 [13] T │
│      [76]                                    [N5]                 [12]   │
│      [75]              ╔═══════════════╗     [N6]                 [11] Q │
│      [74]              ║               ║     [N7]                 [10] U │
│      [73]              ║    CENTER     ║     [N8]                 [9]  A │
│      [72]              ║               ║     [N9]                 [8]  D │
│ Q    [71]              ║  ** START **  ║    [N10]                 [7]  R │
│ U    [70]              ║               ║    [N11]                 [6]  A │
│ A    [69]              ║  Choose one   ║    [N12]                 [5]  N │
│ D    [68]              ║  of 4 dirs:   ║    [N13]                 [4]  T │
│ R    [67]              ║  N, E, S, W   ║    [N14]                 [3]     │
│ A    [66]              ║               ║    [N15]                 [2]     │
│ N    [65]              ╚═══════════════╝    [N16]                 [1]     │
│ T                                            [S1]                         │
│                                              [S2]                         │
│      [96]                                    [S3]                 [32]    │
│      [97]                                    [S4]                 [33]    │
│      [98]                                    [S5]                 [34]    │
│      [99]                                    [S6]                 [35] S  │
│     [100]                                    [S7]                 [36] O  │
│     [101]              ╔═══════════════╗     [S8]                 [37] U  │
│     [102]              ║               ║     [S9]                 [38] T  │
│     [103]              ║  Connectors   ║    [S10]                 [39] H  │
│     [104]              ║  to Center    ║    [S11]                 [40]    │
│ W    [105]              ║               ║    [S12]                [120] Q  │
│ E    [106]              ║  North: N1-16 ║    [S13]                [119] U  │
│ S    [107]              ║  East:  E1-16 ║    [S14]                [118] A  │
│ T    [108]              ║  South: S1-16 ║    [S15]                [117] D  │
│      [109]              ║  West:  W1-16 ║    [S16]                [116] R  │
│ Q    [110]              ║               ║                         [115] A  │
│ U    [111]              ╚═══════════════╝                         [114] N  │
│ A   [112][113][114][115][116][117][118][119][120][121]...[136][137][138] T  │
│ D                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│ SOUTH QUADRANT (40 spaces)                                               │
│                                                                           │
│ [112][113][114][115][116][117][118][119][120][121][122][123][124][125]  │
│   │                                                                  │    │
│ [111]                                                              [126]  │
│   │                                                                  │    │
│ [110]                                                              [127]  │
│   │                                                                  │    │
│ [109]                                                              [128]  │
│   │                  SOUTH QUADRANT PROPERTIES                      │    │
│ [108]                  (Like Full Monopoly Board)                  [129]  │
│   │                                                                  │    │
│ [107]                                                              [130]  │
│   │                                                                  │    │
│ [106]                                                              [131]  │
│   │                                                                  │    │
│ [105]                                                              [132]  │
│   │                                                                  │    │
│ [104][103][102][101][100][99][98][97][96][95][94][93][92][91][90][89]  │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

## Board Specifications

### Total Spaces
- **North Quadrant**: 40 spaces (numbered 1-40 or equivalent)
- **East Quadrant**: 40 spaces (numbered 1-40 or equivalent)  
- **South Quadrant**: 40 spaces (numbered 1-40 or equivalent)
- **West Quadrant**: 40 spaces (numbered 1-40 or equivalent)
- **Connector Spaces**: 16 spaces per direction (N1-N16, E1-E16, S1-S16, W1-W16)
- **Center START**: 1 central space
- **Total**: 160 board spaces + 64 connector spaces + 1 center = 225 spaces

### Quadrant Details

Each quadrant contains the equivalent of a full Monopoly board (40 spaces) with:
- **Properties**: Various property groups with different colors
- **Railroads**: 4 per quadrant
- **Utilities**: 2 per quadrant  
- **Special Spaces**: Go to Jail, Free Parking, Chance, Community Chest, Tax spaces

### Center START Mechanics

When a player lands on or passes through the CENTER START:
1. Player collects $200 (or game equivalent)
2. Player must choose one of 4 directions:
   - **North (N)**: Enter North Quadrant via connector N1-N16
   - **East (E)**: Enter East Quadrant via connector E1-E16
   - **South (S)**: Enter South Quadrant via connector S1-S16
   - **West (W)**: Enter West Quadrant via connector W1-W16
3. Player moves along the chosen direction's connector spaces
4. Player continues into the selected quadrant

### Connector Pathways

Each direction has a 16-space connector path from CENTER to the quadrant:
- **North Connectors (N1-N16)**: Lead from center to North Quadrant entry
- **East Connectors (E1-E16)**: Lead from center to East Quadrant entry
- **South Connectors (S1-S16)**: Lead from center to South Quadrant entry
- **West Connectors (W1-W16)**: Lead from center to West Quadrant entry

These connectors can have their own properties, special spaces, or events.

### Navigation Rules

1. **First Turn**: All players start at CENTER START and choose a direction
2. **Completing a Quadrant**: When a player completes a full circuit of their quadrant (40 spaces), they return to their connector path
3. **Returning to Center**: Players walk back through the connector spaces (16 spaces) to reach CENTER START again
4. **Direction Choice**: Each time a player reaches CENTER, they may choose a different direction or continue in the same one
5. **Strategic Gameplay**: Players can choose directions based on:
   - Available properties to purchase
   - Avoiding opponents' properties
   - Seeking specific property groups
   - Strategic positioning

### Visual Scale

```
Standard Monopoly Board: 40 spaces arranged in a square

This Extended Board: 
┌─────────────┐
│   NORTH     │  40 spaces
│   QUADRANT  │
├─────┬───────┤
│WEST │CENTER │EAST
│  Q  │START  │ Q     Each quadrant = 40 spaces
│  U  │(+ 16) │ U     Center connectors = 16 each
│  A  │       │ A     Total = 225 spaces
│  D  │       │ D
├─────┴───────┤
│   SOUTH     │  40 spaces
│   QUADRANT  │
└─────────────┘
```

## Implementation Notes

### EBS Script Considerations

When implementing this board in EBS scripts:

1. **Board Representation**: Use a 2D array or map structure to represent spaces
2. **Player Position**: Track current quadrant + space number + connector position
3. **Direction State**: Store player's current direction choice
4. **Space Properties**: JSON objects for each space with type, name, cost, rent, etc.
5. **Movement Logic**: Handle transitions between quadrants and connectors
6. **UI Rendering**: Consider using JavaFX screens to visualize the board

### Data Structures

```javascript
// Example space definition
var boardSpaces: json = {
  "center": {
    "id": 0,
    "name": "CENTER START",
    "type": "start",
    "bonus": 200
  },
  "north_connector": [
    {"id": "N1", "name": "North Path 1", "type": "connector"},
    {"id": "N2", "name": "North Path 2", "type": "connector"},
    // ... N3-N16
  ],
  "north_quadrant": [
    {"id": 1, "name": "Mediterranean Avenue", "type": "property", "color": "brown", "cost": 60},
    {"id": 2, "name": "Community Chest", "type": "community_chest"},
    // ... spaces 3-40
  ],
  // Similar structures for East, South, West
};
```

### Screen Layout Recommendation

For the JavaFX UI implementation:
- Use a GridPane or Canvas for board visualization
- Center area: 300x300 pixels
- Each quadrant: 600x600 pixels
- Total board: Approximately 1500x1500 pixels
- Connector paths: 150-200 pixels each
- Scale down for display and allow zooming/panning

## Property Distribution Suggestion

Each quadrant could have themed properties:

- **North Quadrant**: Luxury/High-value properties (Park Place equivalent)
- **East Quadrant**: Commercial/Business properties (Boardwalk equivalent)
- **South Quadrant**: Residential/Medium properties (Atlantic Avenue equivalent)
- **West Quadrant**: Industrial/Low-value properties (Baltic Avenue equivalent)

This creates strategic value in choosing different directions!

## Next Steps

1. Define all 225 spaces with names, types, and properties
2. Create board visualization using EBS screens and JavaFX
3. Implement movement and direction-choice mechanics
4. Design player tokens and tracking system
5. Create property cards and game rules
6. Test gameplay balance across all four quadrants
