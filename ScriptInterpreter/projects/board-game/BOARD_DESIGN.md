# Board Game Design - Extended Monopoly-Style Board

## Overview

This board game features a 4-quadrant design where each quadrant is the size of a standard Monopoly board (40 spaces). **The quadrants share borders at the center** where they overlap, creating a unified game board. The START position is at the center intersection where all four quadrants meet, and players can choose one of 4 directions when they land on START.

## Quadrant Layout

```
   Q1    |    Q2
(NW)     |    (NE)
---------+--------
   Q3    |    Q4
(SW)     |    (SE)
```

**Shared Borders:**
- **Q1 shares border with Q2** (East side of Q1 = West side of Q2)
- **Q1 shares border with Q3** (South side of Q1 = North side of Q3)
- **Q2 shares border with Q4** (South side of Q2 = North side of Q4)
- **Q3 shares border with Q4** (East side of Q3 = West side of Q4)
- **All four quadrants meet at the CENTER START**

## Board Layout Diagram

```
┌──────────────────────────────────────────┬──────────────────────────────────────────┐
│         QUADRANT 1 (NW)                  │         QUADRANT 2 (NE)                  │
│         40 spaces                        │         40 spaces                        │
│                                          │                                          │
│  [11][12][13][14][15][16][17][18][19]   │   [21][22][23][24][25][26][27][28][29]  │
│   │                                 │    │    │                                 │   │
│  [10]       Q1 Properties          [20]  │  [20]       Q2 Properties          [30]  │
│   │         (10 per side)           │    │    │         (10 per side)           │   │
│  [09]                              [21]  │  [19]                              [31]  │
│   │                                 │    │    │                                 │   │
│  [08]                              [22]  │  [18]                              [32]  │
│   │                                 │    │    │                                 │   │
│  [07]                              [23]  │  [17]                              [33]  │
│   │                                 │    │    │                                 │   │
│  [06]                              [24]  │  [16]                              [34]  │
│   │                                 │    │    │                                 │   │
│  [05]                              [25]  │  [15]                              [35]  │
│   │                                 │    │    │                                 │   │
│  [04]                              [26]  │  [14]                              [36]  │
│   │                                 │    │    │                                 │   │
│  [03]                              [27]  │  [13]                              [37]  │
│   │                                 │    │    │                                 │   │
│  [02]                              [28]  │  [12]                              [38]  │
│   │                                 │    │    │                                 │   │
│  [01][40][39][38][37][36][35][34][33][32]══[11][10][09][08][07][06][05][04][03][02][01][40]
│                                     ▲    │    ▲                                     │
│                                     │    │    │                                     │
│                              Shared Border (Q1-Q2)                                  │
├──────────────────────────────────────────┼──────────────────────────────────────────┤
│                              Shared │    │    │ Shared                              │
│                              Border │    │    │ Border                              │
│                              (Q1-Q3)│    │    │ (Q2-Q4)                             │
│                                     ▼    │    ▼                                     │
│  [01][02][03][04][05][06][07][08][09][10]══[39][38][37][36][35][34][33][32][31][30]
│   │                                 │    │    │                                 │   │
│  [40]                              [11]  │  [40]                              [29]  │
│   │                                 │    │    │                                 │   │
│  [39]                              [12]  │  [39]                              [28]  │
│   │                                 │    │    │                                 │   │
│  [38]                              [13]  │  [38]                              [27]  │
│   │                                 │    │    │                                 │   │
│  [37]                              [14]  │  [37]                              [26]  │
│   │                                 │    │    │                                 │   │
│  [36]       Q3 Properties          [15]  │  [36]       Q4 Properties          [25]  │
│   │         (10 per side)           │    │    │         (10 per side)           │   │
│  [35]                              [16]  │  [35]                              [24]  │
│   │                                 │    │    │                                 │   │
│  [34]                              [17]  │  [34]                              [23]  │
│   │                                 │    │    │                                 │   │
│  [33]                              [18]  │  [33]                              [22]  │
│   │                                 │    │    │                                 │   │
│  [32]                              [19]  │  [32]                              [21]  │
│   │                                 │    │    │                                 │   │
│  [31][30][29][28][27][26][25][24][23][22]  [21][20][19][18][17][16][15][14][13][12][11]
│                                          │                                          │
│         QUADRANT 3 (SW)                  │         QUADRANT 4 (SE)                  │
│         40 spaces                        │         40 spaces                        │
└──────────────────────────────────────────┴──────────────────────────────────────────┘

                           ═══════════════════
                           ║  CENTER START   ║
                           ║                 ║
                           ║  Choose one of  ║
                           ║  4 directions:  ║
                           ║  → Q1 (NW)      ║
                           ║  → Q2 (NE)      ║
                           ║  → Q3 (SW)      ║
                           ║  → Q4 (SE)      ║
                           ═══════════════════
```

**Key Features:**
- Each quadrant has exactly 40 spaces arranged in a square (like standard Monopoly)
- Quadrants share borders where they touch (marked with ══)
- The CENTER START is at the intersection where all 4 quadrants meet
- Players at CENTER can choose to enter any of the 4 quadrants

## Board Specifications

### Total Spaces
- **Quadrant 1 (NW)**: 40 spaces arranged in a square
- **Quadrant 2 (NE)**: 40 spaces arranged in a square
- **Quadrant 3 (SW)**: 40 spaces arranged in a square
- **Quadrant 4 (SE)**: 40 spaces arranged in a square
- **Center START**: 1 central intersection space where all quadrants meet
- **Total**: 160 unique board spaces + 1 center = 161 spaces total

**Note:** Spaces on shared borders are counted only once. For example, where Q1 and Q2 meet, those border spaces are shared between both quadrants.

### Quadrant Details

Each quadrant contains the equivalent of a full Monopoly board (40 spaces) with:
- **Properties**: Various property groups with different colors
- **Railroads**: 4 per quadrant
- **Utilities**: 2 per quadrant  
- **Special Spaces**: Go to Jail, Free Parking, Chance, Community Chest, Tax spaces
- **10 spaces per side** (standard Monopoly layout)

### Shared Borders

The quadrants physically connect and share borders:

- **Q1 ↔ Q2 Border**: East side of Q1 connects to West side of Q2
- **Q1 ↔ Q3 Border**: South side of Q1 connects to North side of Q3
- **Q2 ↔ Q4 Border**: South side of Q2 connects to North side of Q4
- **Q3 ↔ Q4 Border**: East side of Q3 connects to West side of Q4

Players can move between quadrants by crossing these shared borders during normal gameplay.

### Center START Mechanics

The CENTER START is at the central intersection where all four quadrants meet:

1. **Starting Position**: All players begin at the CENTER START
2. **Direction Choice**: When at START, players choose one of 4 directions:
   - **Q1 (Northwest)**: Enter the top-left quadrant
   - **Q2 (Northeast)**: Enter the top-right quadrant
   - **Q3 (Southwest)**: Enter the bottom-left quadrant
   - **Q4 (Southeast)**: Enter the bottom-right quadrant
3. **Passing START**: Players collect $200 (or game equivalent) when passing through START
4. **Changing Quadrants**: Players can switch quadrants by returning to START or crossing shared borders

### Navigation Rules

1. **First Turn**: All players start at CENTER START and roll dice to choose initial direction
2. **Movement**: Players move clockwise within their current quadrant following standard Monopoly rules
3. **Quadrant Completion**: When completing a circuit (40 spaces) within a quadrant, players can:
   - Continue in the same quadrant
   - Return to CENTER to choose a new direction
   - Cross a shared border to an adjacent quadrant
4. **Border Crossing**: Players landing on a shared border space can choose to:
   - Stay in their current quadrant
   - Cross into the adjacent quadrant on their next turn
5. **Strategic Gameplay**: Players can choose routes based on:
   - Available properties to purchase
   - Avoiding opponents' properties with high rent
   - Seeking specific property groups to complete sets
   - Strategic positioning for maximum income

### Visual Scale

```
Standard Monopoly Board: 40 spaces arranged in a square (10 per side)

This Extended Board (Shared Borders):
┌──────────┬──────────┐
│    Q1    │    Q2    │  Each quadrant = 40 spaces
│   (NW)   │   (NE)   │  10 spaces per side
│          │          │
├─── CENTER START ────┤  Quadrants share borders
│          │          │  Total unique spaces = 161
│    Q3    │    Q4    │
│   (SW)   │   (SE)   │
└──────────┴──────────┘
```

## Implementation Notes

### EBS Script Considerations

When implementing this board in EBS scripts:

1. **Board Representation**: Use a 2D array or map structure to represent spaces
2. **Player Position**: Track current quadrant (Q1-Q4) + space number (1-40) within that quadrant
3. **Quadrant State**: Store player's current quadrant location
4. **Space Properties**: JSON objects for each space with type, name, cost, rent, etc.
5. **Movement Logic**: Handle transitions between quadrants via shared borders
6. **Border Detection**: Identify when a player lands on a shared border space
7. **UI Rendering**: Consider using JavaFX screens to visualize the board

### Data Structures

```javascript
// Example space definition with shared borders
var boardSpaces: json = {
  "center": {
    "id": "START",
    "name": "CENTER START",
    "type": "start",
    "bonus": 200,
    "adjacentQuadrants": ["Q1", "Q2", "Q3", "Q4"]
  },
  "quadrant1": [
    {"id": "Q1-01", "name": "Mediterranean Avenue", "type": "property", "color": "brown", "cost": 60},
    {"id": "Q1-02", "name": "Community Chest", "type": "community_chest"},
    // ... spaces 3-40
    {"id": "Q1-10", "name": "Border Space", "type": "property", "sharedWith": "Q2", "border": "Q1-Q2"},
    {"id": "Q1-30", "name": "Border Space", "type": "property", "sharedWith": "Q3", "border": "Q1-Q3"}
  ],
  "quadrant2": [
    {"id": "Q2-01", "name": "Baltic Avenue", "type": "property", "color": "brown", "cost": 60},
    // ... spaces 2-40
    {"id": "Q2-01", "name": "Border Space", "type": "property", "sharedWith": "Q1", "border": "Q1-Q2"},
    {"id": "Q2-20", "name": "Border Space", "type": "property", "sharedWith": "Q4", "border": "Q2-Q4"}
  ],
  // Similar structures for Q3 and Q4
};

// Player position tracking
var player1Position: json = {
  "quadrant": "Q1",
  "space": 5,
  "atCenter": false,
  "onBorder": false,
  "adjacentQuadrant": null
};
```

### Screen Layout Recommendation

For the JavaFX UI implementation:
- Use a GridPane or Canvas for board visualization
- Each quadrant: 500x500 pixels
- Total board: 1000x1000 pixels (2x2 quadrant grid)
- Center intersection: Visually marked at the meeting point
- Shared borders: Highlighted or marked to show connectivity
- Scale for display and allow zooming/panning for detail

### Border Crossing Logic

When a player lands on a shared border space:

```javascript
// Pseudocode for border crossing
if call space.isOnBorder() then {
    var adjacentQ = call space.getAdjacentQuadrant();
    
    // Prompt player: "Cross into [quadrant name]? (Y/N)"
    var choice = call system.confirmDialog("Cross into " + adjacentQ + "?");
    
    if choice then {
        player.quadrant = adjacentQ;
        player.space = call getEquivalentSpaceInQuadrant(adjacentQ);
    }
}
```

## Property Distribution Suggestion

Each quadrant could have themed properties:

- **Q1 (NW) - Luxury District**: High-value properties (Park Place, Boardwalk equivalents)
  - Property values: $200-$400
  - Rent: High
  
- **Q2 (NE) - Commercial District**: Business/Office properties
  - Property values: $150-$300
  - Rent: Medium-High
  
- **Q3 (SW) - Residential District**: Medium-value properties (Atlantic Ave equivalents)
  - Property values: $100-$200
  - Rent: Medium
  
- **Q4 (SE) - Industrial District**: Low-value starter properties (Baltic, Mediterranean equivalents)
  - Property values: $60-$150
  - Rent: Low-Medium

This creates strategic value in choosing different quadrants! Players might start in cheaper Q4, build wealth, then move to more expensive quadrants.

## Next Steps

1. Define all 161 unique spaces with names, types, and properties
2. Map shared border spaces between adjacent quadrants
3. Create board visualization using EBS screens and JavaFX
4. Implement movement and quadrant-transition mechanics
5. Design player tokens and tracking system
6. Create property cards and game rules
7. Test gameplay balance across all four quadrants
8. Implement border-crossing decision logic
