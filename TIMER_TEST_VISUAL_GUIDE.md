# Timer Test Screen Visual Guide

## Before and After Comparison

### Original Layout (Flat Vars)
```
┌────────────────────────────────────────┐
│   Timer Variable Access Test          │
├────────────────────────────────────────┤
│                                        │
│  Counter: [___________]                │
│  Message: [___________]                │
│  Interval (ms): [spinner]              │
│                                        │
│  [Start Timer]  [Stop Timer]           │
│  [Reset Counter]  [Check Status]       │
│  [Close]                               │
│                                        │
└────────────────────────────────────────┘
```
- Simple vertical list of controls
- No visual grouping
- Minimal styling

### New Layout (Area Items)
```
┌────────────────────────────────────────┐
│   Timer Variable Access Test          │
├════════════════════════════════════════┤
│  ╔══════════════════════════════════╗  │
│  ║     HEADER AREA (Blue)           ║  │
│  ║  Timer Variable Access Test      ║  │
│  ║  Tests direct screen variable    ║  │
│  ║  access in timer callbacks       ║  │
│  ╚══════════════════════════════════╝  │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │   DATA AREA (White)              │  │
│  │  Counter: [___________]          │  │
│  │  Message: [___________]          │  │
│  │  Interval (ms): [spinner]        │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │   CONTROL AREA                   │  │
│  │  ┌──────────────────────────┐    │  │
│  │  │ Timer Buttons Area       │    │  │
│  │  │ [Start Timer] [Stop]     │    │  │
│  │  └──────────────────────────┘    │  │
│  │  ┌──────────────────────────┐    │  │
│  │  │ Action Buttons Area      │    │  │
│  │  │ [Reset] [Check Status]   │    │  │
│  │  └──────────────────────────┘    │  │
│  │  ┌──────────────────────────┐    │  │
│  │  │ Close Button Area        │    │  │
│  │  │      [Close]             │    │  │
│  │  └──────────────────────────┘    │  │
│  └──────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```
- Clear visual hierarchy with colored sections
- Logical grouping of related controls
- Professional styling with borders and backgrounds
- Button color coding by function

## Area Hierarchy Diagram

```
screen timerTestScreen
└── area (root level)
    └── mainArea (vbox) - Root container, light gray
        ├── headerArea (vbox) - Blue background with border
        │   ├── titleLabel (label) - Bold title
        │   └── descLabel (label) - Description text
        │
        ├── dataArea (vbox) - White background with border
        │   ├── counterField (textfield) → counter var
        │   ├── messageField (textfield) → message var
        │   └── intervalSpinner (spinner) → timerInterval var
        │
        └── controlArea (vbox) - Button container
            ├── timerButtonsArea (hbox) - Horizontal layout
            │   ├── btnStart (button) - Green
            │   └── btnStop (button) - Red
            │
            ├── actionButtonsArea (hbox) - Horizontal layout
            │   ├── btnReset (button) - Orange
            │   └── btnCheckStatus (button) - Blue
            │
            └── closeButtonArea (hbox) - Horizontal layout
                └── btnClose (button) - Gray
```

## Color Scheme

### Container Backgrounds
- **Main Area**: `#f5f5f5` (Light Gray) - Neutral base
- **Header Area**: `#e3f2fd` (Light Blue) - Information section
- **Data Area**: `#ffffff` (White) - Data entry section

### Container Borders
- **Header Area**: `#1976d2` (Blue) - 2px solid
- **Data Area**: `#cccccc` (Light Gray) - 1px solid

### Button Colors
- **Start Timer**: `#4caf50` (Green) - Positive action
- **Stop Timer**: `#f44336` (Red) - Stop action
- **Reset Counter**: `#ff9800` (Orange) - Reset action
- **Check Status**: `#2196f3` (Blue) - Info action
- **Close**: `#607d8b` (Gray) - Exit action

## Data Flow

### Variable References
```
sets[0].vars                    area items
┌─────────────────┐            ┌──────────────────────┐
│ counter         │◄───varRef──│ counterField         │
│ message         │◄───varRef──│ messageField         │
│ timerInterval   │◄───varRef──│ intervalSpinner      │
└─────────────────┘            └──────────────────────┘
```

### Timer Callback Access
```
timerCallback function
       │
       ├──► counter (direct access, no prefix)
       └──► message (direct access, no prefix)
              │
              └──► Updates UI automatically via varRef binding
```

## Key Benefits of New Structure

### 1. Maintainability
- Variables separated from display logic
- Easy to modify layout without changing data definitions
- Clear container hierarchy

### 2. Reusability
- Can reuse same variables in multiple areas
- Can copy/paste area definitions between screens

### 3. Visual Organization
- Related controls grouped together
- Clear visual separation between sections
- Professional appearance

### 4. Flexibility
- Easy to add new controls to existing areas
- Can reorganize layout by moving area items
- Can add nested areas without limit

## Screen Properties Comparison

### Original
```javascript
"title": "Timer Variable Access Test",
"width": 500,
"height": 350,
"vars": [ /* 8 items with display properties */ ]
```

### New
```javascript
"title": "Timer Variable Access Test (Area Items)",
"width": 500,
"height": 450,  // Slightly taller for better layout
"sets": [ /* Variable definitions only */ ],
"area": [ /* Layout hierarchy */ ]
```

## Implementation Notes

### Variable Definitions (sets)
- Only contains: `name`, `type`, `default`
- No display properties
- Pure data definitions

### Area Items
- Contains: `name`, `varRef`, `sequence`, `display`
- Display properties specify UI control type
- `varRef` links to variable in sets

### Button Items
- No `varRef` needed (not data-bound)
- Only need: `name`, `display`
- Display includes `onClick` handler

### Layout Containers
- `vbox`: Vertical box (stacks children vertically)
- `hbox`: Horizontal box (arranges children horizontally)
- Both support: `spacing`, `padding`, `alignment`, `style`

## Testing the New Layout

### Visual Inspection
1. Header section should have blue background
2. Data fields should be in a white box
3. Buttons should be color-coded
4. Buttons should be arranged in horizontal rows

### Functional Testing
1. Timer callback should still access variables directly
2. All buttons should work as before
3. Screen should resize properly
4. Cleanup should run on close

### Console Output
Should be identical to original version, confirming functional equivalence.
