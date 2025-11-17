#!/usr/bin/env python3
"""
Visual representation of the two-column test screen layout.
This script generates an ASCII art representation of the screen layout.
"""

def draw_screen():
    print("=" * 80)
    print("  Two Column Layout Test Screen - Navigation Demonstration")
    print("=" * 80)
    print()
    print("Window Size: 800 x 600 pixels")
    print("Layout: GridPane with 2 columns, 5 rows")
    print("Horizontal Gap: 20px  |  Vertical Gap: 15px  |  Padding: 30px")
    print()
    print("=" * 80)
    print()
    
    # Define fields
    left_column = [
        ("1", "First Name", "TextField", "Enter first name"),
        ("2", "Last Name", "TextField", "Enter last name"),
        ("3", "Email", "TextField", "Enter email address"),
        ("4", "Phone", "TextField", "Enter phone number"),
        ("5", "Age", "Spinner", "18-100")
    ]
    
    right_column = [
        ("6", "Address", "TextField", "Enter address"),
        ("7", "City", "TextField", "Enter city"),
        ("8", "State", "ComboBox", "Select state"),
        ("9", "Zip Code", "TextField", "Enter zip code"),
        ("10", "Country", "ChoiceBox", "USA, Canada, ...")
    ]
    
    print("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
    print("┃     LEFT COLUMN (Column 0)        ┃     RIGHT COLUMN (Column 1)       ┃")
    print("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
    
    for i in range(5):
        left = left_column[i]
        right = right_column[i]
        
        # Format row
        left_text = f"┃ {left[0]}. {left[1]:12} [{left[2]:10}]"
        right_text = f"┃ {right[0]}. {right[1]:12} [{right[2]:10}]"
        
        # Pad to align
        left_text = left_text + " " * (37 - len(left_text))
        right_text = right_text + " " * (37 - len(right_text))
        
        print(f"{left_text}{right_text}┃")
        
        # Show prompt text below
        left_prompt = f"┃   '{left[3]}'".ljust(37)
        right_prompt = f"┃   '{right[3]}'".ljust(37)
        print(f"{left_prompt}{right_prompt}┃")
        
        if i < 4:
            print("┃" + " " * 35 + "┃" + " " * 35 + "┃")
    
    print("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    print()
    
    # Navigation flow
    print("=" * 80)
    print("  TAB NAVIGATION ORDER (Top-to-Bottom, Left Column First)")
    print("=" * 80)
    print()
    print("Left Column (Items 1-5):")
    for seq, field, ctrl, _ in left_column:
        arrow = "   ↓" if seq != "5" else "   └→"
        print(f"  [{seq}] {field:12} ({ctrl}){arrow}")
    
    print()
    print("Right Column (Items 6-10):")
    for seq, field, ctrl, _ in right_column:
        arrow = "   ↓" if seq != "10" else ""
        print(f"  [{seq}] {field:12} ({ctrl}){arrow}")
    
    print()
    print("=" * 80)
    print("  KEY FEATURES")
    print("=" * 80)
    print()
    print("✓ Two-column layout with GridPane")
    print("✓ All items fit on screen (800x600 window)")
    print("✓ Navigation flows top-to-bottom in left column first")
    print("✓ Then continues top-to-bottom in right column")
    print("✓ Each field has proper labels and prompt text")
    print("✓ Mix of control types: TextField, Spinner, ComboBox, ChoiceBox")
    print("✓ Consistent sizing: all controls are 300px wide")
    print("✓ Proper spacing: 20px horizontal gap, 15px vertical gap")
    print()
    print("=" * 80)

if __name__ == "__main__":
    draw_screen()
