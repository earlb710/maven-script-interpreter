# Status Bar Implementation

## Overview

This document describes the implementation of status bars in the EBS Console application. Status bars have been added to both the main application window and individual file tabs to provide real-time feedback on execution status, error messages, and other information.

## Architecture

### Component Structure

```
StatusBar (JavaFX HBox)
├── Status Label (100px fixed width)
│   └── Shows execution state: "Running", blank, etc.
│
├── Separator (vertical line)
│
├── Message Label (flexible width)
│   └── Shows messages, errors with tooltip support
│
├── Separator (vertical line)
│
└── Custom Label (100px fixed width)
    └── Reserved for future custom content
```

### Integration Points

1. **Main Window** (`EbsApp.java`)
   - Status bar added to `BorderPane.setBottom()`
   - Accessible via `EbsConsoleHandler`

2. **File Tabs** (`EbsTab.java`)
   - Each tab has its own status bar instance
   - Wrapped in `BorderPane` with tab content
   - Updates during script execution

## Visual Layout

### Main Window

```
╔══════════════════════════════════════════════════════════════════╗
║                      EBS Console - Main Window                   ║
╠══════════════════════════════════════════════════════════════════╣
║  File   Edit   View   Help                                      ║
╠══════════════════════════════════════════════════════════════════╣
║ ┌─Console──┬─File1.ebs──┬─File2.ebs──┐                          ║
║ │                                     │                          ║
║ │  Console Input/Output Area          │                          ║
║ │                                     │                          ║
║ └─────────────────────────────────────┘                          ║
╠══════════════════════════════════════════════════════════════════╣
║ [Status] │ [Message/Error]              │ [Custom]              ║
╚══════════════════════════════════════════════════════════════════╝
          ↑ NEW STATUS BAR
```

### File Tab Layout

```
┌─File1.ebs──────────────────────────────────────────────────┐
│ Code:                                                       │
│ ┌─────────────────────────────────────────────────────┐   │
│ │ var string name = "test"                            │   │
│ │ print(name)                                         │   │
│ └─────────────────────────────────────────────────────┘   │
│                                                             │
│ Output:                                                     │
│ ┌─────────────────────────────────────────────────────┐   │
│ │ test                                                │   │
│ │ ✓ Done.                                             │   │
│ └─────────────────────────────────────────────────────┘   │
│ [Run] [Clear]                                               │
├─────────────────────────────────────────────────────────────┤
│ [Running] │ Execution completed        │                  │
└─────────────────────────────────────────────────────────────┘
             ↑ TAB-SPECIFIC STATUS BAR
```

## Status Bar States

### 1. Idle State
```
┌────────┬──────────────────────────┬────────┐
│        │                          │        │
└────────┴──────────────────────────┴────────┘
```
All sections are empty/blank when no activity is occurring.

### 2. Running State
```
┌────────┬──────────────────────────┬────────┐
│Running │                          │        │
└────────┴──────────────────────────┴────────┘
```
Shows "Running" in status section during script execution.

### 3. Success State
```
┌────────┬──────────────────────────┬────────┐
│        │ Execution completed      │        │
└────────┴──────────────────────────┴────────┘
```
Shows completion message in message section.

### 4. Error State
```
┌────────┬──────────────────────────┬────────┐
│        │ Error: variable not f... │        │
└────────┴──────────────────────────┴────────┘
         ↑ Hover to see full error message in tooltip
```
Shows error message (truncated if > 60 chars) with full text in tooltip.

## Implementation Details

### Files Created/Modified

#### New Files
- `StatusBar.java` - Main status bar component

#### Modified Files
- `EbsApp.java` - Added status bar to main window
- `EbsHandler.java` - Added status bar accessor methods
- `EbsConsoleHandler.java` - Added error message handling
- `EbsTab.java` - Added status bar to each tab, integrated with run button
- `console.css` - Added status bar styling

### Key Features

1. **Three-Part Design**
   - Fixed-width status section (left)
   - Flexible-width message section (center)
   - Fixed-width custom section (right)

2. **Automatic Error Display**
   - Errors from `submitErrors()` automatically appear in status bar
   - Long messages truncated with "..." and full text in tooltip

3. **Execution Feedback**
   - "Running" status during script execution
   - Success/error messages after completion
   - Status automatically clears after execution

4. **Visual Styling**
   - Light gray gradient background
   - Thin top border for separation
   - Small font (11px) for compact appearance
   - Vertical separators between sections

### Usage Examples

#### Setting Status
```java
statusBar.setStatus("Running");
statusBar.clearStatus();
```

#### Setting Messages
```java
// Simple message
statusBar.setMessage("Execution completed");

// Message with tooltip
statusBar.setMessage("Error: variabl...", "Error: variable not found in scope");

// Clear message
statusBar.clearMessage();
```

#### Setting Custom Content
```java
statusBar.setCustom("Line 42");
statusBar.clearCustom();
```

## Testing

### Manual Test Procedures

1. **Main Window Status Bar**
   - Launch application: `mvn javafx:run`
   - Verify status bar visible at bottom of main window
   - Verify three sections with separators

2. **Tab Status Bar**
   - Open a file tab
   - Verify status bar at bottom of tab
   - Click "Run" button
   - Verify "Running" appears during execution
   - Verify message appears after completion

3. **Error Display**
   - Execute invalid code
   - Verify error message in status bar
   - Hover over message to see tooltip with full error

### Expected Results

✅ Status bar visible at bottom of main window  
✅ Status bar visible at bottom of each file tab  
✅ Three sections with vertical separators  
✅ "Running" appears during script execution  
✅ Error messages appear in message section  
✅ Long messages truncated with tooltip showing full text  
✅ Status clears after execution completes  

## Future Enhancements

The custom section (right side) is reserved for future features such as:
- Line/column indicators
- File encoding information
- Cursor position
- Custom application status
- Any other contextual information

## CSS Classes

The status bar uses the following CSS classes:
- `.status-bar` - Main container styling
- `.status-bar .label` - Label text styling
- `.status-bar .separator` - Separator styling

Styling can be customized in `console.css`.
