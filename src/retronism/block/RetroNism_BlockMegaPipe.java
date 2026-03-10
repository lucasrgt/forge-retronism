package retronism.block;

import net.minecraft.src.*;
import net.minecraft.client.Minecraft;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

public class Retronism_BlockMegaPipe extends BlockContainer {

	public Retronism_BlockMegaPipe(int id, int tex) {
		super(id, tex, Material.iron);
		setHardness(1.0F);
		setResistance(3.0F);
		setStepSound(soundMetalFootstep);
	}

	public boolean isOpaqueCube() { return false; }
	public boolean renderAsNormalBlock() { return false; }

	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
		world.markBlockNeedsUpdate(x, y, z);
	}

	public int getRenderType() {
		return mod_Retronism.megaPipeRenderID;
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileMegaPipe();
	}

	private static final int[][] DIRS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && held.itemID == Retronism_Registry.wrench.shiftedIndex) {
			if (world.multiplayerWorld) return true;
			TileEntity te = world.getBlockTileEntity(x, y, z);
			if (te instanceof Aero_ISideConfigurable) {
				ModLoader.getMinecraftInstance().displayGuiScreen(
					new Retronism_GuiPipeConfig(player, (Aero_ISideConfigurable) te));
			}
			return true;
		}
		return false;
	}

	public boolean isNeighborMachine(IBlockAccess world, int nx, int ny, int nz) {
		int id = world.getBlockId(nx, ny, nz);
		if (id == this.blockID
			|| id == Retronism_Registry.cableBlock.blockID
			|| id == Retronism_Registry.fluidPipeBlock.blockID
			|| id == Retronism_Registry.gasPipeBlock.blockID
			|| id == Retronism_Registry.itemPipeBlock.blockID) return false;
		return canConnectTo(world, nx, ny, nz);
	}

	public boolean canConnectTo(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Aero_IEnergyReceiver) return true;
		if (te instanceof Retronism_TileGenerator) return true;
		if (te instanceof Aero_IFluidHandler) return true;
		if (te instanceof Aero_IGasHandler) return true;
		if (te instanceof IInventory) return true;
		if (Aero_PortRegistry.isPort(x, y, z)) return true;
		int id = world.getBlockId(x, y, z);
		return id == Retronism_Registry.cableBlock.blockID
			|| id == Retronism_Registry.fluidPipeBlock.blockID
			|| id == Retronism_Registry.gasPipeBlock.blockID
			|| id == Retronism_Registry.itemPipeBlock.blockID
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
