package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.block.*;


public class Retronism_RenderCable implements Retronism_IBlockRenderer {

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 6.0F / 16.0F;
		float max = 10.0F / 16.0F;
		float pMin = Retronism_RenderUtils.CABLE_PLATE_MIN;
		float pMax = Retronism_RenderUtils.CABLE_PLATE_MAX;
		float pT = Retronism_RenderUtils.PLATE_THICK;
		Retronism_BlockCable cable = (Retronism_BlockCable) block;
		int E = Aero_SideConfig.TYPE_ENERGY;

		// Center piece
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Down (-Y) = SIDE_BOTTOM(0)
		if(cable.canConnectToSide(world, x, y, z, 0)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 0, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? -Retronism_RenderUtils.INSET : 0.0F;
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x, y - 1, z)) {
					block.setBlockBounds(pMin, 0.0F, pMin, pMax, pT, pMax);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}
		// Up (+Y) = SIDE_TOP(1)
		if(cable.canConnectToSide(world, x, y, z, 1)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 1, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? 1.0F - Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? 1.0F + Retronism_RenderUtils.INSET : 1.0F;
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x, y + 1, z)) {
					block.setBlockBounds(pMin, 1.0F - pT, pMin, pMax, 1.0F, pMax);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}
		// North (-Z) = SIDE_NORTH(2)
		if(cable.canConnectToSide(world, x, y, z, 2)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 2, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? -Retronism_RenderUtils.INSET : 0.0F;
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x, y, z - 1)) {
					block.setBlockBounds(pMin, pMin, 0.0F, pMax, pMax, pT);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}
		// South (+Z) = SIDE_SOUTH(3)
		if(cable.canConnectToSide(world, x, y, z, 3)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 3, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? 1.0F - Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? 1.0F + Retronism_RenderUtils.INSET : 1.0F;
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x, y, z + 1)) {
					block.setBlockBounds(pMin, pMin, 1.0F - pT, pMax, pMax, 1.0F);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}
		// West (-X) = SIDE_WEST(4)
		if(cable.canConnectToSide(world, x, y, z, 4)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 4, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? -Retronism_RenderUtils.INSET : 0.0F;
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x - 1, y, z)) {
					block.setBlockBounds(0.0F, pMin, pMin, pT, pMax, pMax);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}
		// East (+X) = SIDE_EAST(5)
		if(cable.canConnectToSide(world, x, y, z, 5)) {
			int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, 5, E);
			if (mode != Aero_SideConfig.MODE_NONE) {
				float end = (mode == Aero_SideConfig.MODE_INPUT) ? 1.0F - Retronism_RenderUtils.INSET : (mode == Aero_SideConfig.MODE_OUTPUT) ? 1.0F + Retronism_RenderUtils.INSET : 1.0F;
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (cable.isNeighborMachine(world, x + 1, y, z)) {
					block.setBlockBounds(1.0F - pT, pMin, pMin, 1.0F, pMax, pMax);
					renderer.renderStandardBlock(block, x, y, z);
				}
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		Retronism_RenderUtils.renderSimplePipeInventory(renderer, block, 6.0F/16, 10.0F/16);
	}
}
