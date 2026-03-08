package retronism.render;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.Retronism_TileMegaElectrolysis;

public class Retronism_RenderMegaElectrolysisCasing implements Retronism_IBlockRenderer {

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		if (isPartOfFormedStructure(world, x, y, z)) {
			return true; // Render nothing — the controller renders the full model
		}
		renderer.renderStandardBlock(block, x, y, z);
		return true;
	}

	private boolean isPartOfFormedStructure(IBlockAccess world, int x, int y, int z) {
		int controllerId = Retronism_Registry.megaElectrolysisController.blockID;
		// Check 3x3x3 neighborhood for a formed controller
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (world.getBlockId(x + dx, y + dy, z + dz) == controllerId) {
						TileEntity te = world.getBlockTileEntity(x + dx, y + dy, z + dz);
						if (te instanceof Retronism_TileMegaElectrolysis) {
							if (((Retronism_TileMegaElectrolysis) te).isFormed) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		float[][] singleBlock = {{0, 0, 0, 16, 16, 16}};
		Retronism_RenderUtils.renderPartsInventory(renderer, block, singleBlock);
	}
}
