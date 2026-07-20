import os
import re

layout_dir = r"c:\Users\Sharmila\DentNova_Android\app\src\main\res\layout"

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # Replace hardcoded text colors that cause issues
    # Text colors: #FFFFFF, #000000, @android:color/white, @android:color/black, #1A2332, #1E293B, #1A1A2E, #7B8A9A
    # We will safely ignore #FFFFFF inside MaterialButton if it has app:backgroundTint="#00BCD4"
    # Actually, let's just do a blanket regex for TextViews and CardViews
    
    # Simple regex to replace all #FFFFFF, #000000, @android:color/white, @android:color/black
    # in android:textColor="..."
    
    color_patterns = [
        r'"#FFFFFF"', r'"#000000"', r'"@android:color/white"', r'"@android:color/black"',
        r'"#1A2332"', r'"#1E293B"', r'"#1A1A2E"', r'"#7B8A9A"', r'"#1A2B3C"', r'"#1A1C21"',
        r'"#6B7B8D"', r'"#64748B"', r'"#9EAAB8"', r'"#6B7280"'
    ]
    
    # 1. TextColors -> ?attr/colorOnSurface
    for cp in color_patterns:
        # Avoid replacing white text on explicitly colored elements if possible, but the user requested strict replacements.
        # Let's replace android:textColor="..."
        content = re.sub(r'android:textColor\s*=\s*' + cp, 'android:textColor="?attr/colorOnSurface"', content)
        content = re.sub(r'app:titleTextColor\s*=\s*' + cp, 'app:titleTextColor="?attr/colorOnSurface"', content)
    
    # 2. Card Backgrounds -> ?attr/colorSurfaceVariant
    for cp in color_patterns:
        content = re.sub(r'app:cardBackgroundColor\s*=\s*' + cp, 'app:cardBackgroundColor="?attr/colorSurfaceVariant"', content)
    
    # 3. Backgrounds -> ?attr/colorSurface
    # We should be careful. F5F9FA is already handled in previous scripts.
    for cp in [r'"#FFFFFF"', r'"#000000"', r'"@android:color/white"', r'"@android:color/black"']:
        # If it's a root layout or generic background
        content = re.sub(r'android:background\s*=\s*' + cp, 'android:background="?attr/colorSurface"', content)

    # 4. Icon Tints -> ?attr/colorOnSurface
    for cp in color_patterns:
        content = re.sub(r'android:tint\s*=\s*' + cp, 'android:tint="?attr/colorOnSurface"', content)
        content = re.sub(r'app:tint\s*=\s*' + cp, 'app:tint="?attr/colorOnSurface"', content)
        content = re.sub(r'app:iconTint\s*=\s*' + cp, 'app:iconTint="?attr/colorOnSurface"', content)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

modified = []
for filename in os.listdir(layout_dir):
    if filename.endswith('.xml'):
        filepath = os.path.join(layout_dir, filename)
        if process_file(filepath):
            modified.append(filename)

print("Modified files:", modified)
