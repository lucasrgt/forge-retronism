package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.gui.*;

public class Retronism_BlockMegaElectrolysisController extends BlockContainer {
	public Retronism_BlockMegaElectrolysisController(int id, int tex) {
		super(id, tex, Material.iron);
		setHardness(3.5F);
		setResistance(5.0F);
		setStepSound(Block.soundMetalFootstep);
		setBlockName("retroNismMegaElectrolysis");
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileMegaElectrolysis();
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if (player.isSneaking()) return false;
		if (world.multiplayerWorld) return true;
		Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) world.getBlockTileEntity(x, y, z);
		if (tile == null) return false;

		if (!tile.isFormed) {
			tile.checkStructure(world, x, y, z);
		}
		if (!tile.isFormed) return true;

		ModLoader.OpenGUI(player, new Retronism_GuiMegaElectrolysis(player.inventory, tile));
		return true;
	}

	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
		Retronism_TileMegaElectrolysis tile = (Retronism_TileMegaElectrolysis) world.getBlockTileEntity(x, y, z);
		if (tile != null) {
			tile.checkStructure(world, x, y, z);
		}
	}

	public int getRenderType() {
		return mod_Retronism.megaElectrolysisRenderID;
	}

	public boolean renderAsNormalBlock() { return false; }
	public boolean isOpaqueCube() { return false; }
}
