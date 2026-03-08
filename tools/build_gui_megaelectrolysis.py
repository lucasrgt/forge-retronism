import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
from gui_builder import GuiBuilder

gui = GuiBuilder()
gui.panel(0, 0, 176, 166)
gui.energy_bar(161, 16, 8, 54)
gui.energy_bar(12, 16, 16, 54)  # fluid tank
gui.progress_arrow(52, 34)
gui.energy_bar(100, 16, 16, 24)  # gas tank
gui.energy_bar(100, 46, 16, 24)  # gas tank
gui.energy_bar(136, 16, 16, 54)  # fluid tank
gui.player_inventory(7, 83)

gui.save(os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui", "retronism_megaelectrolysis.png"))
print("Generated GUI texture for MegaElectrolysis")
