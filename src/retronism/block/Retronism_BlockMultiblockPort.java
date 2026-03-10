package retronism.block;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.gui.*;

/**
 * AeroPort Block by lucasrgt - aerocoding.dev
 * Facilita a interação com máquinas gigantes por meio de "portos" de conexão.
 */
public class Retronism_BlockMultiblockPort extends BlockContainer {

    public Retronism_BlockMultiblockPort(int id, int tex) {
        super(id, tex, Material.iron);
    }

    @Override
    public TileEntity getBlockEntity() {
        return new Retronism_TileMultiblockPort();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking()) return false;
        
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te instanceof Retronism_TileMultiblockPort) {
            TileEntity core = ((Retronism_TileMultiblockPort) te).getCore();
            if (core instanceof Retronism_TileMegaCrusher) {
                // Abre a GUI da máquina principal!
                ModLoader.OpenGUI(player, new Retronism_GuiMegaCrusher(player.inventory, (Retronism_TileMegaCrusher)core));
                return true;
            }
        }
        return false;
    }
}
