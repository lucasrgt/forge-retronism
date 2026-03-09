import sys
from PIL import Image

def analyze_quadrants(filename):
    img = Image.open(filename).convert("RGB")
    w, h = img.size
    print(f"Size: {w}x{h}")
    # Zones: Top-Left (0,0, 64,64)
    # Top-Right (64,0, 128,64)
    # Bottom-Left (0,64, 64,128)
    # Bottom-Right (64,64, 128,128)
    
    quads = {
        "TL (0,0)": (0, 0, 64, 64),
        "TR (64,0)": (64, 0, 128, 64),
        "BL (0,64)": (0, 64, 64, 128),
        "BR (64,64)": (64, 64, 128, 128),
        "Center (32,32)": (32, 24, 96, 88)
    }
    
    for name, bounds in quads.items():
        box = img.crop(bounds)
        r, g, b = 0, 0, 0
        pixels = box.getdata()
        for p in pixels:
            r += p[0]
            g += p[1]
            b += p[2]
        size = len(pixels)
        r //= size
        g //= size
        b //= size
        print(f"{name}: Average #{r:02X}{g:02X}{b:02X}")

analyze_quadrants('megacrusher_hq.png')
