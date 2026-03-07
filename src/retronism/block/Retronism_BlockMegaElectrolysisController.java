package retronism.block;

import net.minecraft.src.*;

public class Retronism_BlockMegaElectrolysisController extends BlockContainer {
    public Retronism_BlockMegaElectrolysisController(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("megaelectrolysisController");
    }

    @Override
    protected TileEntity getBlockEntity() {
        return new Retronism_TileMegaElectrolysis();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (world.multiplayerWorld) return true;
        Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) world.getBlockTileEntity(x, y, z);
        if (tile == null) return false;

        if (!tile.checkStructure(world, x, y, z)) {
            return true;
        }

        ModLoader.OpenGUI(player, new Retronism_GuiMegaElectrolysis(player.inventory, tile));
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
        }
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            for (int i = 0; i < tile.getSizeInventory(); i++) {
                ItemStack stack = tile.getStackInSlot(i);
                if (stack != null) {
                    float rx = world.rand.nextFloat() * 0.6F + 0.1F;
                    float ry = world.rand.nextFloat() * 0.6F + 0.1F;
                    float rz = world.rand.nextFloat() * 0.6F + 0.1F;
                    EntityItem entity = new EntityItem(world, x + rx, y + ry, z + rz, stack);
                    world.entityJoinedWorld(entity);
                }
            }
        }
        super.onBlockRemoval(world, x, y, z);
    }
}
