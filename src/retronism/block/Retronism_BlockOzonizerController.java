package retronism.block;

import net.minecraft.src.*;
import retronism.api.Retronism_PortRegistry;

public class Retronism_BlockOzonizerController extends BlockContainer {
    public Retronism_BlockOzonizerController(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("ozonizerController");
    }

    @Override
    protected TileEntity getBlockEntity() {
        return new Retronism_TileOzonizer();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (world.multiplayerWorld) return true;
        Retronism_TileOzonizer tile = (Retronism_TileOzonizer) world.getBlockTileEntity(x, y, z);
        if (tile == null) return false;

        if (!tile.checkStructure(world, x, y, z)) {
            String debug = tile.getLastFailDebug();
            player.addChatMessage("Structure incomplete! " + (debug != null ? debug : ""));
            return true;
        }

        ModLoader.OpenGUI(player, new Retronism_GuiOzonizer(player.inventory, tile));
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        Retronism_TileOzonizer tile = (Retronism_TileOzonizer) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
        }
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        Retronism_PortRegistry.unregisterAllForController(x, y, z);
        Retronism_TileOzonizer tile = (Retronism_TileOzonizer) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.isFormed = false;
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
