package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.block.*;
import retronism.tile.*;


public class Retronism_RenderGasPipe implements Retronism_IBlockRenderer {

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		Retronism_BlockGasPipe pipe = (Retronism_BlockGasPipe) block;
		int G = Aero_SideConfig.TYPE_GAS;

		TileEntity te = world.getBlockTileEntity(x, y, z);
		boolean hasGas = false;
		int gasColor = 0xFFFFFFFF;
		if (te instanceof Retronism_TileGasPipe) {
			Retronism_TileGasPipe tilePipe = (Retronism_TileGasPipe) te;
			if (tilePipe.getGasAmount() > 0) {
				hasGas = true;
				gasColor = Aero_GasType.getColor(tilePipe.getGasType());
			}
		}

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		if (hasGas) {
			block.setBlockBounds(iMin, iMin, iMin, iMax, iMax, iMax);
			renderer.overrideBlockTexture = 64;
			Tessellator.instance.setColorOpaque_I(gasColor);
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		for (int side = 0; side < 6; side++) {
			int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
			if (pipe.canConnectToSide(world, x, y, z, side)) {
				int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, side, G);
				Retronism_RenderUtils.renderGasArm(renderer, block, x, y, z, side, mode, min, max, iMin, iMax, hasGas, gasColor);
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
