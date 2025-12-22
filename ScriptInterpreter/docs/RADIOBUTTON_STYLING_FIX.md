# RadioButton Text Styling Fix

## Problem
In the chess.ebs startup screen, the radio button option text for "1 Player" and "2 Player" appeared gray on a gray background, making it difficult to read.

## Root Cause
When creating a group of RadioButton controls in `AreaItemFactory.java`, the individual RadioButton controls were not receiving the item-specific styling properties (itemColor, itemBold, itemFontSize, itemItalic) from the DisplayItem metadata. These properties were being applied to the VBox container that holds the RadioButtons, but not to the RadioButton controls themselves.

## Solution

### 1. Code Changes in AreaItemFactory.java
Added a new helper method `applyRadioButtonItemStyling()` that applies the following styles to each RadioButton:
- **itemFontSize**: Sets the font size using `-fx-font-size`
- **itemColor/textColor**: Sets the text color using `-fx-text-fill` (textColor takes precedence over itemColor)
- **itemBold**: Sets bold font weight using `-fx-font-weight: bold`
- **itemItalic**: Sets italic font style using `-fx-font-style: italic`

The method is called for each RadioButton when it's created (lines 98 and 122), ensuring that all radio button option text is properly styled.

### 2. Configuration Changes in chess.ebs
Updated the gameMode variable display properties to include:
```json
"itemColor": "#ffffff",
"itemBold": true,
"itemFontSize": "14px"
```

This makes the radio button option text white, bold, and 14px in size, ensuring good visibility against the gray background.

## Expected Result
- The "1 Player" and "2 Player" radio button option text should now appear in **white bold 14px text**
- The text should be clearly visible against the gray background (#2c2c2c)
- The styling matches the other UI elements in the chess startup dialog

## Testing
A test script `test_radiobutton_styling.ebs` was created to verify the RadioButton styling functionality independently of the chess application.

## Technical Details
The JavaFX RadioButton control supports standard CSS styling through the `setStyle()` method. The fix ensures that:
1. Each RadioButton in a radio button group receives individual styling
2. The styling is applied immediately when the RadioButton is created
3. The styling properties follow the same pattern as other controls (textColor precedence over itemColor)
4. All item styling properties (size, color, bold, italic) are supported
