package retronism.block;

import net.minecraft.src.*;
import net.minecraft.client.Minecraft;
import retronism.*;
import retronism.api.*;
import retronism.tile.*;
import retronism.gui.*;

public class Retronism_BlockMegaPipe extends Block {

	public Retronism_BlockMegaPipe(int id, int tex) {
		super(id, tex, Material.iron);
		setHardness(1.0F);
		setResistance(3.0F);
		setStepSound(soundMetalFootstep);
	}

	public boolean isOpaqueCube() { return false; }
	public boolean renderAsNormalBlock() { return false; }

	public int getRenderType() {
		return mod_Retronism.megaPipeRenderID;
	}

	public TileEntity getBlockEntity() {
		return new Retronism_TileMegaPipe();
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && held.itemID == mod_Retronism.wrench.shiftedIndex) {
			if (world.multiplayerWorld) return true;
			TileEntity te = world.getBlockTileEntity(x, y, z);
			if (te instanceof Retronism_TileMegaPipe) {
				ModLoader.getMinecraftInstance().displayGuiScreen(
					new Retronism_GuiMegaPipeConfig(player, (Retronism_TileMegaPipe) te));
			}
			return true;
		}
		return false;
	}

	public boolean canConnectTo(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Retronism_IEnergyReceiver) return true;
		if (te instanceof Retronism_TileGenerator) return true;
		if (te instanceof Retronism_IFluidHandler) return true;
		if (te instanceof Retronism_IGasHandler) return true;
		if (te instanceof IInventory) return true;
		int id = world.getBlockId(x, y, z);
		return id == mod_Retronism.cableBlock.blockID
			|| id == mod_Retronism.fluidPipeBlock.blockID
			|| id == mod_Retronism.gasPipeBlock.blockID
			|| id == this.blockID;
	}

	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		float min = 4.0F / 16.0F;
		float max = 12.0F / 16.0F;
		return AxisAlignedBB.getBoundingBoxFromPool(
			(double)(x + min), (double)(y + min), (double)(z + min),
			(double)(x + max), (double)(y + max), (double)(z + max));
	}
}
