"""Generate 16x16 machine port textures: iron block base + colored ring border."""
from PIL import Image, ImageDraw
import os

# Iron block base colors (approximate Beta 1.7.3 iron block)
IRON_LIGHT = (220, 220, 220, 255)
IRON_MID   = (200, 200, 200, 255)
IRON_DARK  = (167, 167, 167, 255)
IRON_SHADE = (140, 140, 140, 255)

# Port ring colors
COLORS = {
    "energy": (255, 220, 0, 255),    # yellow
    "fluid":  (40, 100, 220, 255),   # blue
    "gas":    (120, 200, 120, 255),  # green
}

def make_iron_base():
    """Create a simple 16x16 iron block texture."""
    img = Image.new("RGBA", (16, 16), IRON_MID)
    px = img.load()
    # Add some noise/detail like vanilla iron block
    import random
    random.seed(42)
    for y in range(16):
        for x in range(16):
            r = random.randint(-15, 15)
            base = IRON_MID
            c = tuple(max(0, min(255, base[i] + r)) for i in range(3)) + (255,)
            px[x, y] = c
    # Darken edges slightly
    for i in range(16):
        px[0, i] = IRON_SHADE
        px[i, 0] = IRON_SHADE
        px[15, i] = IRON_DARK
        px[i, 15] = IRON_DARK
    # Lighten top-left highlight
    for i in range(1, 15):
        px[1, i] = IRON_LIGHT
        px[i, 1] = IRON_LIGHT
    return img

def add_ring(img, color, ring_width=2):
    """Add a colored ring/border to the texture, with inner gap showing iron."""
    px = img.load()
    w, h = img.size

    # Darker shade for ring shadow
    dark = tuple(max(0, c - 60) for c in color[:3]) + (255,)

    # Draw outer ring (2px wide border, inset 2px from edge)
    inset = 2
    for i in range(inset, w - inset):
        for j in range(ring_width):
            # Top edge
            px[i, inset + j] = color
            # Bottom edge
            px[i, h - 1 - inset - j] = dark
            # Left edge
            px[inset + j, i] = color
            # Right edge
            px[w - 1 - inset - j, i] = dark

    # Corner reinforcement
    for di in range(ring_width):
        for dj in range(ring_width):
            px[inset + di, inset + dj] = color
            px[w - 1 - inset - di, inset + dj] = color
            px[inset + di, h - 1 - inset - dj] = dark
            px[w - 1 - inset - di, h - 1 - inset - dj] = dark

    return img

def main():
    out_dir = os.path.join(os.path.dirname(__file__), "..", "src", "retronism", "assets", "block")
    os.makedirs(out_dir, exist_ok=True)

    for name, color in COLORS.items():
        img = make_iron_base()
        img = add_ring(img, color)
        path = os.path.join(out_dir, f"retronism_port_{name}.png")
        img.save(path)
        print(f"Saved {path}")

if __name__ == "__main__":
    main()
