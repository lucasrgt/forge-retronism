"""
RetroNism GUI Texture Builder
Deterministic, pixel-perfect GUI texture generator for Minecraft Beta 1.7.3.

Usage:
    from gui_builder import GuiBuilder
    gui = GuiBuilder()
    gui.panel(0, 0, 176, 166)
    gui.slot(79, 34)
    gui.player_inventory(7, 83)
    gui.energy_bar(161, 16, 8, 54)
    gui.save("output.png")
"""

from PIL import Image, ImageDraw
import os

# === PALETTE ===
T  = (0, 0, 0, 0)          # transparent
BK = (0, 0, 0, 255)         # black (outer border)
WH = (255, 255, 255, 255)   # white (highlight, inner borders)
BG = (198, 198, 198, 255)   # background fill
DK = (85, 85, 85, 255)      # dark gray (shadow)
SD = (55, 55, 55, 255)      # slot dark border
SL = (139, 139, 139, 255)   # slot inner fill
GY = (104, 104, 104, 255)   # gray (arrow shadow)
BAR_BG = (64, 64, 64, 255)  # energy bar empty (#404040)

# Furnace.png path for extracting sprites
FURNACE_PATH = os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui", "furnace.png")


class GuiBuilder:
    def __init__(self, width=256, height=256):
        self.image = Image.new("RGBA", (width, height), T)
        self.draw = ImageDraw.Draw(self.image)
        self._furnace = None

    def _get_furnace(self):
        if self._furnace is None:
            self._furnace = Image.open(FURNACE_PATH).convert("RGBA")
        return self._furnace

    # =========================================================================
    # PANEL - main GUI background with Minecraft 3D beveled border
    # =========================================================================
    def panel(self, x, y, w, h):
        """Standard Minecraft container panel. Typically (0, 0, 176, 166)."""
        img = self.image

        # Fill background
        self.draw.rectangle([x + 3, y + 3, x + w - 4, y + h - 4], fill=BG)

        # --- Black outer border (1px, with rounded corners) ---
        # Top edge
        for px in range(x + 2, x + w - 2):
            img.putpixel((px, y), BK)
        # Bottom edge
        for px in range(x + 2, x + w - 2):
            img.putpixel((px, y + h - 1), BK)
        # Left edge
        for py in range(y + 2, y + h - 2):
            img.putpixel((x, py), BK)
        # Right edge
        for py in range(y + 2, y + h - 2):
            img.putpixel((x + w - 1, py), BK)
        # Corner pixels (diagonal)
        img.putpixel((x + 1, y + 1), BK)
        img.putpixel((x + w - 2, y + 1), BK)
        img.putpixel((x + 1, y + h - 2), BK)
        img.putpixel((x + w - 2, y + h - 2), BK)

        # --- White highlight (top + left, 2px band) ---
        # Top rows (inside border)
        for px in range(x + 2, x + w - 2):
            img.putpixel((px, y + 1), WH)
            img.putpixel((px, y + 2), WH)
        # Left cols (inside border)
        for py in range(y + 2, y + h - 2):
            img.putpixel((x + 1, py), WH)
            img.putpixel((x + 2, py), WH)
        # Extra highlight pixel at inner corner (y=3, x=3)
        img.putpixel((x + 3, y + 3), WH)

        # --- Dark shadow (bottom + right, 2px band) ---
        # Bottom rows (inside border)
        for px in range(x + 3, x + w - 2):
            img.putpixel((px, y + h - 2), DK)
            img.putpixel((px, y + h - 3), DK)
        # Right cols (inside border)
        for py in range(y + 3, y + h - 2):
            img.putpixel((x + w - 2, py), DK)
            img.putpixel((x + w - 3, py), DK)

        # --- Fix corner transitions ---
        # Top-right: shadow starts at y=3 on the right
        img.putpixel((x + w - 3, y + 1), WH)
        img.putpixel((x + w - 3, y + 2), WH)
        img.putpixel((x + w - 2, y + 2), BG)
        # Bottom-left: highlight ends, shadow starts
        img.putpixel((x + 2, y + h - 3), BG)
        img.putpixel((x + 1, y + h - 2), BK)
        img.putpixel((x + 2, y + h - 2), BK)
        # Bottom-right corner
        img.putpixel((x + w - 2, y + h - 2), BK)

        return self

    # =========================================================================
    # SLOT - standard 18x18 inventory slot
    # =========================================================================
    def slot(self, x, y):
        """Single inventory slot at (x, y). Size: 18x18."""
        img = self.image
        # Top border (dark)
        for px in range(x, x + 17):
            img.putpixel((px, y), SD)
        # Left border (dark)
        for py in range(y, y + 17):
            img.putpixel((x, py), SD)
        # Bottom border (white)
        for px in range(x, x + 18):
            img.putpixel((px, y + 17), WH)
        # Right border (white)
        for py in range(y, y + 18):
            img.putpixel((x + 17, py), WH)
        # Corner overlap: bottom-right = WH, top-right = SL
        img.putpixel((x + 17, y), SL)
        # Inner fill
        self.draw.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SL)
        return self

    # =========================================================================
    # SLOT GRID - multiple slots in a row/grid
    # =========================================================================
    def slot_row(self, x, y, count):
        """Row of `count` slots starting at (x, y), spaced 18px apart."""
        for i in range(count):
            self.slot(x + i * 18, y)
        return self

    def slot_grid(self, x, y, cols, rows):
        """Grid of slots."""
        for r in range(rows):
            self.slot_row(x, y + r * 18, cols)
        return self

    # =========================================================================
    # PLAYER INVENTORY - standard 9x3 + hotbar (always at bottom of GUI)
    # =========================================================================
    def player_inventory(self, x, y):
        """Standard player inventory (9x3 grid + 9 hotbar).
        Typically at (7, 83) for 166-tall GUIs, (7, ySize-83) generically.
        Total height: 76px (3*18 + 4gap + 18)."""
        # Main 9x3 inventory
        self.slot_grid(x, y, 9, 3)
        # Hotbar (4px gap below inventory)
        self.slot_row(x, y + 58, 9)
        return self

    # =========================================================================
    # ENERGY BAR - vertical bar with slot-style border
    # =========================================================================
    def energy_bar(self, x, y, w, h):
        """Vertical energy bar outline. Inner area is dark (#404040).
        The Java code fills this with colored pixels at runtime.
        Standard generator bar: (161, 16, 8, 54)."""
        img = self.image
        # Top border (dark)
        for px in range(x, x + w - 1):
            img.putpixel((px, y), SD)
        # Left border (dark)
        for py in range(y, y + h - 1):
            img.putpixel((x, py), SD)
        # Bottom border (white)
        for px in range(x, x + w):
            img.putpixel((px, y + h - 1), WH)
        # Right border (white)
        for py in range(y, y + h):
            img.putpixel((x + w - 1, py), WH)
        # Corner pixels
        img.putpixel((x + w - 1, y), SL)
        # Inner fill (dark empty)
        self.draw.rectangle([x + 1, y + 1, x + w - 2, y + h - 2], fill=BAR_BG)
        return self

    # =========================================================================
    # PROGRESS ARROW - empty (gray) + filled (white) overlay in sprite area
    # =========================================================================
    def progress_arrow_empty(self, x, y):
        """Empty progress arrow (24x17) - gray SL silhouette.
        Drawn in the main texture area. Java overlays the filled version
        progressively at runtime using drawTexturedModalRect from sprite area."""
        furnace = self._get_furnace()
        empty = furnace.crop((79, 34, 103, 51))
        self.image.paste(empty, (x, y), empty)
        return self

    def progress_arrow_fill(self, sx=176, sy=14):
        """Filled progress arrow (24x17) - white WH with gray GY shadow.
        Placed in the SPRITE AREA of the texture (x>=176) for Java to use
        as overlay source via drawTexturedModalRect."""
        furnace = self._get_furnace()
        filled = furnace.crop((176, 14, 200, 31))
        self.image.paste(filled, (sx, sy), filled)
        return self

    def progress_arrow(self, x, y, sprite_x=176, sprite_y=14):
        """Complete progress arrow: empty in main area + filled in sprite area.
        Java: drawTexturedModalRect(guiX+x, guiY+y, sprite_x, sprite_y, progress+1, 17)"""
        self.progress_arrow_empty(x, y)
        self.progress_arrow_fill(sprite_x, sprite_y)
        return self

    # =========================================================================
    # FLAME - empty (gray) + filled (colorful) overlay in sprite area
    # =========================================================================
    def flame_empty(self, x, y):
        """Empty flame (14x14) - gray SL silhouette.
        Furnace draws the filled version from bottom-up at runtime."""
        furnace = self._get_furnace()
        empty = furnace.crop((56, 36, 70, 50))
        self.image.paste(empty, (x, y), empty)
        return self

    def flame_fill(self, sx=176, sy=0):
        """Filled flame (14x14) - colorful fire.
        Placed in SPRITE AREA for Java runtime overlay.
        Java: drawTexturedModalRect(x+X, y+Y+12-scale, sx, 12-scale, 14, scale+2)"""
        furnace = self._get_furnace()
        filled = furnace.crop((176, 0, 190, 14))
        self.image.paste(filled, (sx, sy), filled)
        return self

    def flame(self, x, y, sprite_x=176, sprite_y=0):
        """Complete flame: empty in main area + filled in sprite area."""
        self.flame_empty(x, y)
        self.flame_fill(sprite_x, sprite_y)
        return self

    # =========================================================================
    # BIG SLOT - 26x26 output slot (like furnace output)
    # =========================================================================
    def big_slot(self, x, y):
        """Large 26x26 output slot (like furnace result slot)."""
        img = self.image
        # Top border (dark)
        for px in range(x, x + 25):
            img.putpixel((px, y), SD)
        # Left border (dark)
        for py in range(y, y + 25):
            img.putpixel((x, py), SD)
        # Bottom border (white)
        for px in range(x, x + 26):
            img.putpixel((px, y + 25), WH)
        # Right border (white)
        for py in range(y, y + 26):
            img.putpixel((x + 25, py), WH)
        img.putpixel((x + 25, y), SL)
        # Inner fill
        self.draw.rectangle([x + 1, y + 1, x + 24, y + 24], fill=SL)
        return self

    # =========================================================================
    # LABEL AREA - flat colored rectangle (for custom overlays)
    # =========================================================================
    def rect(self, x, y, w, h, color):
        """Draw a filled rectangle. Color as (R,G,B) or (R,G,B,A) tuple."""
        if len(color) == 3:
            color = color + (255,)
        self.draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)
        return self

    # =========================================================================
    # HORIZONTAL SEPARATOR LINE
    # =========================================================================
    def separator(self, x, y, w):
        """Horizontal separator line (like between machine area and inventory).
        1px dark on top, 1px white below."""
        for px in range(x, x + w):
            self.image.putpixel((px, y), SD)
            self.image.putpixel((px, y + 1), WH)
        return self

    # =========================================================================
    # COPY REGION - paste from another image
    # =========================================================================
    def paste_from(self, src_path, src_rect, dest_x, dest_y):
        """Copy a region from another image.
        src_rect: (x, y, x2, y2) in source image."""
        src = Image.open(src_path).convert("RGBA")
        region = src.crop(src_rect)
        self.image.paste(region, (dest_x, dest_y), region)
        return self

    # =========================================================================
    # SAVE
    # =========================================================================
    def save(self, path):
        """Save the texture as PNG."""
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        self.image.save(path)
        print(f"Saved: {path} ({self.image.size[0]}x{self.image.size[1]})")
        return self

    def show(self):
        """Open the image for preview (dev only)."""
        self.image.show()
        return self


# =============================================================================
# PRESETS - common GUI layouts
# =============================================================================

def make_single_slot_gui(title_for_reference="machine"):
    """Preset: single centered slot + energy bar + player inventory.
    Like the Generator GUI."""
    gui = GuiBuilder()
    gui.panel(0, 0, 176, 166)
    gui.slot(79, 34)                    # centered input slot
    gui.energy_bar(161, 16, 8, 54)      # right-side energy bar
    gui.player_inventory(7, 83)
    return gui


def make_processor_gui(title_for_reference="processor"):
    """Preset: input slot -> arrow -> output slot + energy bar + player inv.
    Like the Crusher/Furnace pattern."""
    gui = GuiBuilder()
    gui.panel(0, 0, 176, 166)
    gui.slot(55, 34)                    # input slot
    gui.progress_arrow(79, 35)          # arrow between slots
    gui.slot(115, 34)                   # output slot
    gui.energy_bar(10, 16, 8, 54)       # left-side energy bar
    gui.player_inventory(7, 83)
    return gui


def make_dual_input_gui(title_for_reference="combiner"):
    """Preset: 2 input slots -> arrow -> output slot + energy bar."""
    gui = GuiBuilder()
    gui.panel(0, 0, 176, 166)
    gui.slot(45, 25)                    # input 1
    gui.slot(45, 47)                    # input 2
    gui.progress_arrow(73, 35)          # arrow
    gui.big_slot(107, 30)               # big output
    gui.energy_bar(10, 16, 8, 54)       # energy bar
    gui.player_inventory(7, 83)
    return gui


# =============================================================================
# CLI - run directly to test
# =============================================================================
if __name__ == "__main__":
    import sys
    out_dir = os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui")

    if len(sys.argv) > 1 and sys.argv[1] == "generator":
        gui = make_single_slot_gui()
        gui.save(os.path.join(out_dir, "retronism_generator.png"))
    elif len(sys.argv) > 1 and sys.argv[1] == "crusher":
        gui = make_processor_gui()
        gui.save(os.path.join(out_dir, "retronism_crusher.png"))
    else:
        # Generate all presets as test
        make_single_slot_gui().save(os.path.join(out_dir, "test_single.png"))
        make_processor_gui().save(os.path.join(out_dir, "test_processor.png"))
        make_dual_input_gui().save(os.path.join(out_dir, "test_dual.png"))
        print("Generated test textures in", out_dir)
