package retronism.render;

import net.minecraft.src.*;
import retronism.Retronism_Registry;


public class Retronism_RenderMachinePort implements Retronism_IBlockRenderer {

    public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
        if (isPartOfFormedStructure(world, x, y, z)) {
            return true; // invisible when controller is rendering the full formed model
        }
        renderer.renderStandardBlock(block, x, y, z);
        return true;
    }

    private boolean isPartOfFormedStructure(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
        float[][] singleBlock = {{0, 0, 0, 16, 16, 16}};
        Retronism_RenderUtils.renderPartsInventory(renderer, block, singleBlock);
    }
}
