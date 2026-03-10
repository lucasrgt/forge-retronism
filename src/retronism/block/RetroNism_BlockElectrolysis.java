package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

import java.util.Random;

public class Retronism_BlockElectrolysis extends BlockContainer {

	public Retronism_BlockElectrolysis(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileElectrolysis();
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	public int getBlockTextureFromSide(int side) {
		return side == 1 ? this.blockIndexInTexture + 17 : (side == 0 ? this.blockIndexInTexture + 17 : this.blockIndexInTexture);
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if (player.isSneaking()) return false;
		if (world.multiplayerWorld) return true;
		Retronism_TileElectrolysis tileEntity = (Retronism_TileElectrolysis) world.getBlockTileEntity(x, y, z);
		ModLoader.OpenGUI(player, new Retronism_GuiElectrolysis(player.inventory, tileEntity));
		return true;
	}
}
