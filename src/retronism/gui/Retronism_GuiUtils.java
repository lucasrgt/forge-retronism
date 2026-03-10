package retronism.gui;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_GuiUtils extends Gui {

	private static final Retronism_GuiUtils INSTANCE = new Retronism_GuiUtils();

	/**
	 * Draw a striped green energy bar filling from bottom to top.
	 */
	public static void drawEnergyBar(int barX, int barY, int barW, int barH, int energyScaled) {
		if (energyScaled <= 0) return;
		int fillTop = barY + barH - energyScaled;
		for (int sy = fillTop; sy < barY + barH; sy++) {
			int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
			INSTANCE.drawRect(barX, sy, barX + barW, sy + 1, color);
		}
	}

	/**
	 * Compute scaled energy value from an IEnergyReceiver.
	 */
	public static int getEnergyScaled(Aero_IEnergyReceiver tile, int scale) {
		int max = tile.getMaxEnergy();
		return max > 0 ? tile.getStoredEnergy() * scale / max : 0;
	}

	/**
	 * Compute scaled energy value from raw values (for tiles that don't implement IEnergyReceiver).
	 */
	public static int getEnergyScaled(int storedEnergy, int maxEnergy, int scale) {
		return maxEnergy > 0 ? storedEnergy * scale / maxEnergy : 0;
	}
}
