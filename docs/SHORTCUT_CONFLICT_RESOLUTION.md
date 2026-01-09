# Keyboard Shortcut Conflict Resolution

## Problem

The Alt+R shortcut was assigned to multiple buttons across different components, causing conflicts where the keyboard shortcut couldn't reliably activate the intended button.

## Conflicting Buttons

### Original Alt+R Assignments

1. **Remove** buttons (4 instances)
   - MailConfigDialog
   - DatabaseConfigDialog
   - FtpConfigDialog
   - SafeDirectoriesDialog

2. **Replace** button (1 instance)
   - EbsTab (script editor find/replace bar)

3. **Refresh** button (1 instance)
   - ThreadViewerDialog

4. **Reset** button (1 instance)
   - Console

## Resolution Strategy

### Changed: Remove Buttons → Alt+M

Changed all "Remove" buttons to use **Alt+M** (for reMove):
- **MailConfigDialog**: Remove now uses Alt+M
- **DatabaseConfigDialog**: Remove now uses Alt+M
- **FtpConfigDialog**: Remove now uses Alt+M
- **SafeDirectoriesDialog**: Remove now uses Alt+M

**Rationale:**
- "M" appears in "reMove"
- Alt+M is available (not used by other common actions)
- Intuitive and easy to remember
- Consistent across all configuration dialogs

### Unchanged: Replace, Refresh, Reset → Alt+R

These buttons retain Alt+R because they exist in different contexts:
- **Replace** (EbsTab): Only in find/replace bar within script editor
- **Refresh** (ThreadViewerDialog): Only in Thread Viewer dialog
- **Reset** (Console): Only in main console

**Rationale:**
- These are in separate windows/contexts, so no runtime conflicts
- Alt+R makes sense for these actions (Replace, Refresh, Reset)
- Users won't have multiple of these contexts open simultaneously

## Conflict Prevention

### Guidelines for Future Shortcuts

1. **Check for existing assignments** before adding new shortcuts
2. **Use different keys for same context** (e.g., within same dialog)
3. **Same key OK for different contexts** (e.g., different windows/dialogs)
4. **Prioritize memorable shortcuts** (first letter when possible)
5. **Document all assignments** to track usage

### Current Shortcut Allocation

| Key | Usage | Context |
|-----|-------|---------|
| A   | Add | Configuration dialogs |
| B   | Browse | File/directory selection dialogs |
| C   | Close/Cancel | Most dialogs |
| E   | Export | Export dialog |
| G   | Gmail | Mail configuration (Add Gmail) |
| L   | Clear | Console (cLear output) |
| M   | **Remove** | **Configuration dialogs** |
| N   | Next | Find bar navigation |
| O   | OK/Copy | Dialogs and copy operations |
| P   | Prev | Find bar navigation |
| R   | Replace/Refresh/Reset | Context-dependent |
| S   | Save | Most dialogs |
| T   | Test/Stop Thread | Context-dependent |
| U   | Run/Submit | Script execution (rUn, sUbmit) |
| V   | View | HTML/Markdown viewer |

### Reserved for Future Use

Available keys for new shortcuts:
- D, F, H, I, J, K, Q, W, X, Y, Z

## Testing Shortcuts

To test for conflicts:

1. Open multiple dialogs simultaneously
2. Try each shortcut in different contexts
3. Verify only intended button activates
4. Check that disabled buttons don't respond

## Related Issues

- **Issue #1**: Button height inconsistency (fixed - switched to mnemonic parsing)
- **Issue #2**: Alt+R conflict with Remove button (fixed - changed to Alt+M)

## Files Modified

### Code Changes
- MailConfigDialog.java
- DatabaseConfigDialog.java
- FtpConfigDialog.java
- SafeDirectoriesDialog.java

### Documentation Updates
- BUTTON_SHORTCUTS.md
- BUTTON_SHORTCUTS_IMPLEMENTATION_SUMMARY.md
- BUTTON_SHORTCUTS_VISUAL_GUIDE.md
- KEYBOARD_SHORTCUTS.md

## Verification

After changes, verified:
- ✅ All Remove buttons use Alt+M
- ✅ Replace, Refresh, Reset retain Alt+R
- ✅ No duplicate assignments within same context
- ✅ Documentation updated consistently
- ✅ Code compiles successfully
