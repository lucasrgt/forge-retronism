import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
from gui_builder import GuiBuilder

gui = GuiBuilder()
gui.panel(0, 0, 176, 166)
gui.energy_bar(7, 16, 8, 54)
gui.fluid_tank(56, 16, 14, 52)
gui.progress_arrow(79, 35)
gui.gas_tank(112, 16, 14, 52)
gui.player_inventory(7, 83)

gui.save(os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui", "retronism_ozonizer.png"))
print("Generated GUI texture for Ozonizer")
