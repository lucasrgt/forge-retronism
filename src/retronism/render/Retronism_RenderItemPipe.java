package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.block.*;


public class Retronism_RenderItemPipe implements Retronism_IBlockRenderer {

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		Retronism_BlockItemPipe pipe = (Retronism_BlockItemPipe) block;
		int I = Aero_SideConfig.TYPE_ITEM;

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Arms with side config + connector plates
		for (int side = 0; side < 6; side++) {
			int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
			if (pipe.canConnectToSide(world, x, y, z, side)) {
				int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, side, I);
				switch (side) {
					case 0: block.setBlockBounds(min, Retronism_RenderUtils.negBound(mode), min, max, min, max); break;
					case 1: block.setBlockBounds(min, max, min, max, Retronism_RenderUtils.posBound(mode), max); break;
					case 2: block.setBlockBounds(min, min, Retronism_RenderUtils.negBound(mode), max, max, min); break;
					case 3: block.setBlockBounds(min, min, max, max, max, Retronism_RenderUtils.posBound(mode)); break;
					case 4: block.setBlockBounds(Retronism_RenderUtils.negBound(mode), min, min, min, max, max); break;
					case 5: block.setBlockBounds(max, min, min, Retronism_RenderUtils.posBound(mode), max, max); break;
				}
				renderer.renderStandardBlock(block, x, y, z);
				if (pipe.isNeighborMachine(world, x+d[0], y+d[1], z+d[2])) {
					Retronism_RenderUtils.renderConnectorPlate(renderer, block, x, y, z, side, Retronism_RenderUtils.PIPE_PLATE_MIN, Retronism_RenderUtils.PIPE_PLATE_MAX, Retronism_RenderUtils.PLATE_THICK);
				}
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		Retronism_RenderUtils.renderSimplePipeInventory(renderer, block, 5.0F/16, 11.0F/16);
	}
}
