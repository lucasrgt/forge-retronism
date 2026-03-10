package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.block.*;
import retronism.tile.*;


public class Retronism_RenderFluidPipe implements Retronism_IBlockRenderer {

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		float eps = 0.002F;
		Retronism_BlockFluidPipe pipe = (Retronism_BlockFluidPipe) block;
		int F = Aero_SideConfig.TYPE_FLUID;

		TileEntity te = world.getBlockTileEntity(x, y, z);
		float fillRatio = 0.0F;
		int fluidTex = -1;
		if (te instanceof Retronism_TileFluidPipe) {
			Retronism_TileFluidPipe tilePipe = (Retronism_TileFluidPipe) te;
			if (tilePipe.getFluidAmount() > 0) {
				float visualCapacity = 200.0F;
				fillRatio = Math.min(1.0F, (float) tilePipe.getFluidAmount() / visualCapacity);
				fluidTex = Block.waterMoving.blockIndexInTexture;
			}
		}
		if (fillRatio > 0.0F && fillRatio < 0.18F) fillRatio = 0.18F;

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Center fluid fill
		if (fillRatio > 0 && fluidTex >= 0) {
			float fillTop = iMin + (iMax - iMin) * fillRatio;
			block.setBlockBounds(iMin - eps, iMin, iMin - eps, iMax + eps, fillTop, iMax + eps);
			renderer.overrideBlockTexture = fluidTex;
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		// Arms with side config + connector plates
		for (int side = 0; side < 6; side++) {
			int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
			if (pipe.canConnectToSide(world, x, y, z, side)) {
				int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, side, F);
				Retronism_RenderUtils.renderPipeArm(renderer, block, x, y, z, side, mode, min, max, iMin, iMax, fillRatio, fluidTex, eps);
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
