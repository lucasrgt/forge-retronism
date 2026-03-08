package retronism.render;

import net.minecraft.src.*;

public class Retronism_RenderCrusher implements Retronism_IBlockRenderer {

	private static final float[][] CRUSHER_PARTS = {
		{0, 0, 0, 16, 3, 16},       // base_plate
		{2, 3, 2, 14, 10, 14},      // body_main
		{1, 10, 1, 15, 13, 15},     // upper_section
		{0, 13, 0, 16, 16, 2},      // hopper_front
		{0, 13, 14, 16, 16, 16},    // hopper_back
		{0, 13, 2, 2, 16, 14},      // hopper_left
		{14, 13, 2, 16, 16, 14},    // hopper_right
		{0, 4, 5, 2, 9, 11},        // piston_left
		{14, 4, 5, 16, 9, 11},      // piston_right
		{3, 4, 4, 5, 9, 12},        // jaw_left
		{11, 4, 4, 13, 9, 12},      // jaw_right
	};

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		for (int i = 0; i < CRUSHER_PARTS.length; i++) {
			float[] p = CRUSHER_PARTS[i];
			block.setBlockBounds(p[0]/16F, p[1]/16F, p[2]/16F, p[3]/16F, p[4]/16F, p[5]/16F);
			renderer.renderStandardBlock(block, x, y, z);
		}
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		Retronism_RenderUtils.renderPartsInventory(renderer, block, CRUSHER_PARTS);
	}
}
