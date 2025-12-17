# Multi-Window Debug View Test

## Overview
This test script verifies that the debug view (Ctrl+D) works correctly when multiple screens are open simultaneously, ensuring proper thread management and event handler cleanup.

## Test Script
**File:** `test_multi_window_debug_view.ebs`

## Purpose
Verify that the debug view implementation correctly handles:
1. Multiple debug views open on different screens simultaneously
2. Independent operation of each debug panel
3. Copy button functionality with proper thread management
4. Cleanup of debug panel resources without affecting other screens
5. Rapid toggle operations without errors or memory leaks

## How to Run

### From Console
```bash
cd ScriptInterpreter
mvn javafx:run
```

Then in the console:
```
/run scripts/test/test_multi_window_debug_view.ebs
```

### From Command Line
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.Run scripts/test/test_multi_window_debug_view.ebs
```

## Test Scenarios

### Scenario 1: Multiple Independent Debug Views
1. Open all three test screens (done automatically by script)
2. Press Ctrl+D on Screen 1 to open debug view
3. Press Ctrl+D on Screen 2 to open debug view
4. Press Ctrl+D on Screen 3 to open debug view
5. **Expected:** All three debug panels are visible simultaneously
6. **Expected:** Each shows correct data for its respective screen

### Scenario 2: Copy Button Independence
1. With all three debug views open
2. Click the Copy button (📋) on Screen 1's debug panel
3. **Expected:** Only Screen 1's copy button shows checkmark (✓)
4. Wait 1 second for checkmark to clear
5. Click the Copy button on Screen 2's debug panel
6. **Expected:** Only Screen 2's copy button shows checkmark
7. **Expected:** No interference between screens

### Scenario 3: Independent Cleanup
1. Open debug views on all three screens
2. Close Screen 1's debug view (Ctrl+D or close button)
3. **Expected:** Screen 2 and 3 debug views remain open and functional
4. Click Copy button on Screen 2
5. **Expected:** Copy works normally on Screen 2
6. Click Copy button on Screen 3
7. **Expected:** Copy works normally on Screen 3
8. Reopen Screen 1's debug view
9. **Expected:** Screen 1 debug view opens normally
10. Click Copy button on Screen 1
11. **Expected:** Copy works normally

### Scenario 4: Rapid Toggle Test
1. Focus Screen 1
2. Rapidly press Ctrl+D multiple times (open/close/open/close)
3. **Expected:** Debug view toggles without errors
4. Open debug view, click Copy button, immediately close (within 1 second)
5. **Expected:** No errors, panel closes cleanly
6. Reopen debug view
7. **Expected:** Debug view opens normally, copy button works

### Scenario 5: Data Modification Test
1. Open debug view on Screen 1
2. Modify field values using the UI controls
3. Click "Increment Counter" button
4. **Expected:** Debug panel updates to show new values
5. Verify "Changed" indicator appears in debug panel
6. Repeat with Screen 2 and 3
7. **Expected:** Each debug panel independently tracks changes

## Expected Results

### ✅ Pass Criteria
- All three screens open successfully
- Debug view toggles correctly on each screen (Ctrl+D)
- Debug panels can be open simultaneously on multiple screens
- Copy button works independently on each debug panel
- Checkmark (✓) appears for 1 second only on the clicked panel
- Closing one debug view doesn't affect others
- Rapid toggle operations complete without errors
- Debug panel accurately displays each screen's variables
- Thread cleanup occurs correctly (no lingering threads)

### ❌ Fail Indicators
- Debug view doesn't open on some screens
- Copy button on one screen affects another screen
- Checkmark appears on wrong panel
- Closing one debug view closes others
- Errors occur during rapid toggle
- Debug panel shows wrong data
- Memory leaks or threading issues
- Copy button stays as checkmark permanently

## Implementation Details

### Thread Management
Each screen's debug panel has its own copy button feedback thread tracked in `debugCopyFeedbackThreads` map using screen name as key:
```java
debugCopyFeedbackThreads.put(screenName.toLowerCase(), feedbackThread);
```

### Cleanup Process
When a debug panel closes:
1. Thread for that specific screen is interrupted and removed
2. Other screens' threads continue running normally
3. Map entry is removed using screen-specific key
4. No cascading effects to other debug panels

### Thread Naming
Threads are named descriptively for debugging:
```java
"DebugPanel-CopyFeedback-" + screenName
```

## Troubleshooting

### Issue: Debug view doesn't open
- **Solution:** Ensure screen has focus, press Ctrl+D (not Ctrl+Shift+D)
- **Solution:** Check console for errors

### Issue: Copy button doesn't work
- **Solution:** Ensure debug panel is fully loaded
- **Solution:** Check clipboard permissions

### Issue: Checkmark appears on wrong screen
- **Solution:** This indicates a bug - report with reproduction steps
- **Expected:** Should never happen with correct implementation

### Issue: Rapid toggle causes errors
- **Solution:** This indicates thread cleanup issue - report with details
- **Expected:** Should handle rapid toggles gracefully

## Manual Verification Checklist

- [ ] Three test screens open successfully
- [ ] Debug view opens on Screen 1 (Ctrl+D)
- [ ] Debug view opens on Screen 2 (Ctrl+D)
- [ ] Debug view opens on Screen 3 (Ctrl+D)
- [ ] All three debug views visible simultaneously
- [ ] Copy button on Screen 1 works (shows ✓ for 1 second)
- [ ] Copy button on Screen 2 works (shows ✓ for 1 second)
- [ ] Copy button on Screen 3 works (shows ✓ for 1 second)
- [ ] Clicking copy on one screen doesn't affect others
- [ ] Closing Screen 1 debug view leaves others open
- [ ] Reopening Screen 1 debug view works
- [ ] Rapid toggle on Screen 1 works without errors
- [ ] Closing debug view during copy animation works
- [ ] Field modifications appear in debug panel
- [ ] "Increment Counter" button updates debug display
- [ ] No console errors during any operation
- [ ] All screens close cleanly

## Related Documentation
- `DEBUG_VIEW_EVENT_CLEANUP.md` - Technical details on event handler cleanup
- `DEBUG_VIEW_INVESTIGATION_SUMMARY.md` - Investigation summary and findings
- `TestMultipleDebugViews.java` - Unit tests for multi-screen scenarios

## Notes
- This is a manual test script that requires user interaction
- Automated unit tests are in `TestMultipleDebugViews.java`
- The script tests the actual JavaFX implementation, not just the logic
- All verification is visual/interactive
