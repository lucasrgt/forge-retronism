package retronism.render;

import net.minecraft.src.*;
import retronism.Retronism_Registry;
import retronism.tile.Retronism_TileMegaCrusher;
import retronism.aero.Aero_Model;
import retronism.aero.Aero_ModelLoader;
import retronism.aero.Aero_ModelRenderer;

public class Retronism_RenderMegaCrusher implements Retronism_IBlockRenderer {

    public static final Aero_Model MODEL = Aero_ModelLoader.load("/models/MegaCrusher.aero.json");

    public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
        if (block == Retronism_Registry.megaCrusherCoreBlock) {
            TileEntity te = world.getBlockTileEntity(x, y, z);
            if (te instanceof Retronism_TileMegaCrusher) {
                Retronism_TileMegaCrusher core = (Retronism_TileMegaCrusher) te;
                if (core.validateStructure()) {
                    return true;
                }
            }
            renderer.renderStandardBlock(block, x, y, z);
            return true;
        }

        if (block == Retronism_Registry.testBlock) {
            Retronism_TileMegaCrusher core = findNearbyCore(world, x, y, z);
            if (core != null && core.validateStructure()) {
                return true; 
            }
            renderer.renderStandardBlock(block, x, y, z);
            return true;
        }

        return false;
    }

    private Retronism_TileMegaCrusher findNearbyCore(IBlockAccess world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int bx = x + dx, by = y + dy, bz = z + dz;
                    if (world.getBlockId(bx, by, bz) == Retronism_Registry.megaCrusherCoreBlock.blockID) {
                        TileEntity te = world.getBlockTileEntity(bx, by, bz);
                        if (te instanceof Retronism_TileMegaCrusher) {
                            Retronism_TileMegaCrusher core = (Retronism_TileMegaCrusher) te;
                            if (x >= core.originX && x <= core.originX + 2 &&
                                y >= core.originY && y <= core.originY + 2 &&
                                z >= core.originZ && z <= core.originZ + 2) {
                                return core;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
        // Forçar a textura HQ para o inventário
        int texID = ModLoader.getMinecraftInstance().renderEngine.getTexture("/block/retronism_megacrusher_hq.png");
        ModLoader.getMinecraftInstance().renderEngine.bindTexture(texID);
        
        Aero_ModelRenderer.renderInventory(renderer, MODEL, metadata);
    }
}
