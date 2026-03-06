package retronism.item;

import net.minecraft.src.*;
import retronism.*;
import retronism.api.*;
import retronism.tile.*;
import retronism.gui.*;

public class RetroNism_ItemWrench extends Item {

	public RetroNism_ItemWrench(int id) {
		super(id);
		this.maxStackSize = 1;
	}

	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
		if (world.multiplayerWorld) return true;

		TileEntity te = world.getBlockTileEntity(x, y, z);

		// Only cycle config on pipes (Cable, FluidPipe, GasPipe)
		// MegaPipe is handled in its block's blockActivated
		// Machines use GUI tabs instead
		if (te instanceof RetroNism_TileCable || te instanceof RetroNism_TileFluidPipe || te instanceof RetroNism_TileGasPipe) {
			RetroNism_ISideConfigurable configurable = (RetroNism_ISideConfigurable) te;
			int[] config = configurable.getSideConfig();
			StringBuilder msg = new StringBuilder(RetroNism_SideConfig.getSideName(side) + ": ");
			boolean first = true;

			for (int type = 0; type < RetroNism_SideConfig.TYPE_COUNT; type++) {
				if (!configurable.supportsType(type)) continue;
				int oldMode = RetroNism_SideConfig.get(config, side, type);
				int newMode = RetroNism_SideConfig.cycleMode(oldMode);
				configurable.setSideMode(side, type, newMode);
				if (!first) msg.append(", ");
				msg.append(RetroNism_SideConfig.getTypeName(type));
				msg.append("=");
				msg.append(RetroNism_SideConfig.getModeName(newMode));
				first = false;
			}

			player.addChatMessage(msg.toString());
			world.markBlockNeedsUpdate(x, y, z);
			return true;
		}

		return false;
	}
}
