package retronism.render;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileMegaElectrolysis;

public class Retronism_RenderMegaElectrolysis implements Retronism_IBlockRenderer {

	private static final float[][] FORMED_PARTS = {
		// Frame — 4 corner pillars
		{0, 0, 0, 4, 48, 4},        // pillar_fl
		{44, 0, 0, 48, 48, 4},      // pillar_fr
		{0, 0, 44, 4, 48, 48},      // pillar_bl
		{44, 0, 44, 48, 48, 48},    // pillar_br
		// Frame — bottom beams
		{4, 0, 0, 44, 4, 4},        // beam_bottom_front
		{4, 0, 44, 44, 4, 48},      // beam_bottom_back
		{0, 0, 4, 4, 4, 44},        // beam_bottom_left
		{44, 0, 4, 48, 4, 44},      // beam_bottom_right
		// Frame — top beams
		{4, 44, 0, 44, 48, 4},      // beam_top_front
		{4, 44, 44, 44, 48, 48},    // beam_top_back
		{0, 44, 4, 4, 48, 44},      // beam_top_left
		{44, 44, 4, 48, 48, 44},    // beam_top_right
		// Body panels
		{5, 5, 1, 43, 43, 3},       // panel_front
		{5, 5, 45, 43, 43, 47},     // panel_back
		{1, 5, 5, 3, 43, 43},       // panel_left
		{45, 5, 5, 47, 43, 43},     // panel_right
		{5, 45, 5, 43, 47, 43},     // panel_top
		{4, 1, 4, 44, 3, 44},       // base_plate
		// Details — reaction chamber
		{14, 8, 14, 34, 38, 34},    // reaction_chamber
		{18, 12, 18, 30, 34, 30},   // chamber_core
		// Details — intake pipes (sides)
		{3, 18, 18, 14, 24, 28},    // intake_pipe_left
		{34, 18, 18, 45, 24, 28},   // intake_pipe_right
		// Details — exhaust stacks (top)
		{10, 44, 10, 16, 54, 16},   // exhaust_stack_1
		{32, 44, 10, 38, 54, 16},   // exhaust_stack_2
		// Details — output manifold (back)
		{16, 14, 34, 32, 28, 46},   // output_manifold
		// Details — control panel (front, protrudes slightly)
		{16, 26, -1, 32, 38, 1},    // control_panel
		// Details — top pipe run
		{20, 38, 8, 28, 42, 40},    // pipe_run_top
		// Details — support legs
		{6, -2, 6, 10, 1, 10},      // support_leg_fl
		{38, -2, 6, 42, 1, 10},     // support_leg_fr
		{6, -2, 38, 10, 1, 42},     // support_leg_bl
		{38, -2, 38, 42, 1, 42},    // support_leg_br
	};

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Retronism_TileMegaElectrolysis) {
			Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) te;
			if (tile.isFormed) {
				return renderFormedStructure(renderer, world, x, y, z, block, tile);
			}
		}
		// Not formed: render as normal block
		renderer.renderStandardBlock(block, x, y, z);
		return true;
	}

	private boolean renderFormedStructure(RenderBlocks renderer, IBlockAccess world,
			int x, int y, int z, Block block, Retronism_TileMegaElectrolysis tile) {
		float offX = -tile.structOffX;
		float offY = -tile.structOffY;
		float offZ = -tile.structOffZ;

		for (int i = 0; i < FORMED_PARTS.length; i++) {
			float[] p = FORMED_PARTS[i];
			block.setBlockBounds(
				p[0]/16F + offX, p[1]/16F + offY, p[2]/16F + offZ,
				p[3]/16F + offX, p[4]/16F + offY, p[5]/16F + offZ
			);
			renderer.renderStandardBlock(block, x, y, z);
		}
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		// Render as standard block in inventory — formed model is too large
		float[][] singleBlock = {{0, 0, 0, 16, 16, 16}};
		Retronism_RenderUtils.renderPartsInventory(renderer, block, singleBlock);
	}
}
