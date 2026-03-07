import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
from gui_builder import GuiBuilder

gui = GuiBuilder()
gui.panel(0, 0, 176, 166)
gui.energy_bar(30, 16, 16, 54)  # fluid tank
gui.progress_arrow(76, 34)
gui.energy_bar(116, 16, 16, 54)  # gas tank
gui.energy_bar(161, 16, 8, 54)
gui.player_inventory(7, 83)

gui.save(os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui", "retronism_megaelectrolysis.png"))
print("Generated GUI texture for MegaElectrolysis")
