package retronism.render;

import net.minecraft.src.*;

public interface Retronism_IBlockRenderer {
	boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block);
	void renderInventory(RenderBlocks renderer, Block block, int metadata);
}
