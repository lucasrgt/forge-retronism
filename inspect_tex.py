import sys
from PIL import Image

def analyze_texture(filename):
    try:
        img = Image.open(filename).convert("RGB")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    w, h = img.size
    print(f"Image Size: {w}x{h}")
    
    unique_colors = {}
    
    for y in range(0, h, 16):
        row_str = []
        for x in range(0, w, 16):
            # Sample center of 16x16 block
            r, g, b = img.getpixel((x+8, y+8))
            hex_col = f"#{r:02X}{g:02X}{b:02X}"
            
            if hex_col not in unique_colors:
                unique_colors[hex_col] = len(unique_colors) + 1
                print(f"Color {unique_colors[hex_col]:02d}: {hex_col}")
                
            row_str.append(f"{unique_colors[hex_col]:02d}")
        print(f"Row {y//16:02d} (Y={y:03d}): " + " ".join(row_str))

analyze_texture('megacrusher_hq.png')
