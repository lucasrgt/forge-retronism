package retronism.item;

import net.minecraft.src.*;
import retronism.*;
import retronism.api.*;
import retronism.tile.*;
import retronism.gui.*;

public class Retronism_ItemWrench extends Item {

	public Retronism_ItemWrench(int id) {
		super(id);
		this.maxStackSize = 1;
	}

	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
		if (world.multiplayerWorld) return true;

		TileEntity te = world.getBlockTileEntity(x, y, z);

		// Only cycle config on pipes (Cable, FluidPipe, GasPipe)
		// MegaPipe is handled in its block's blockActivated
		// Machines use GUI tabs instead
		if (te instanceof Retronism_TileCable || te instanceof Retronism_TileFluidPipe || te instanceof Retronism_TileGasPipe) {
			Retronism_ISideConfigurable configurable = (Retronism_ISideConfigurable) te;
			int[] config = configurable.getSideConfig();
			StringBuilder msg = new StringBuilder(Retronism_SideConfig.getSideName(side) + ": ");
			boolean first = true;

			for (int type = 0; type < Retronism_SideConfig.TYPE_COUNT; type++) {
				if (!configurable.supportsType(type)) continue;
				int oldMode = Retronism_SideConfig.get(config, side, type);
				int newMode = Retronism_SideConfig.cycleMode(oldMode);
				configurable.setSideMode(side, type, newMode);
				if (!first) msg.append(", ");
				msg.append(Retronism_SideConfig.getTypeName(type));
				msg.append("=");
				msg.append(Retronism_SideConfig.getModeName(newMode));
				first = false;
			}

			player.addChatMessage(msg.toString());
			world.markBlockNeedsUpdate(x, y, z);
			return true;
		}

		return false;
	}
}
