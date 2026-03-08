package retronism.block;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileMultiblockPort;

public class Retronism_BlockMultiblockPort extends BlockContainer {
    public Retronism_BlockMultiblockPort(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("multiblockPort");
    }

    @Override
    protected TileEntity getBlockEntity() {
        return new Retronism_TileMultiblockPort();
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return Block.blockSteel.getBlockTextureFromSide(side);
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        Retronism_TileMultiblockPort port = (Retronism_TileMultiblockPort) world.getBlockTileEntity(x, y, z);
        if (port != null) {
            port.notifyControllerRemoved();
        }
        super.onBlockRemoval(world, x, y, z);
    }
}
