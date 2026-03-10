package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

import java.util.Random;

public class Retronism_BlockCable extends BlockContainer {

	public Retronism_BlockCable(int id, int textureIndex) {
		super(id, Material.circuits);
		this.blockIndexInTexture = textureIndex;
		this.setBlockBounds(6.0F/16, 6.0F/16, 6.0F/16, 10.0F/16, 10.0F/16, 10.0F/16);
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileCable();
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public int getRenderType() {
		return mod_Retronism.cableRenderID;
	}

	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
		world.markBlockNeedsUpdate(x, y, z);
	}

	public int quantityDropped(Random random) {
		return 1;
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	private static final int[][] DIRS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && held.itemID == Retronism_Registry.wrench.shiftedIndex) {
			if (!hasMachineNeighbor(world, x, y, z)) return false;
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

	private boolean hasMachineNeighbor(World world, int x, int y, int z) {
		for (int side = 0; side < 6; side++) {
			if (canConnectToSide(world, x, y, z, side) && isNeighborMachine(world, x + DIRS[side][0], y + DIRS[side][1], z + DIRS[side][2]))
				return true;
		}
		return false;
	}

	public boolean canConnectTo(IBlockAccess world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id == this.blockID || id == Retronism_Registry.megaPipeBlock.blockID) return true;
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Aero_IEnergyReceiver || te instanceof Retronism_TileGenerator) return true;
		return Aero_PortRegistry.isPortOfType(x, y, z, Aero_PortRegistry.PORT_TYPE_ENERGY);
	}

	public boolean isNeighborMachine(IBlockAccess world, int nx, int ny, int nz) {
		int id = world.getBlockId(nx, ny, nz);
		if (id == this.blockID || id == Retronism_Registry.megaPipeBlock.blockID) return false;
		return canConnectTo(world, nx, ny, nz);
	}

	public boolean canConnectToSide(IBlockAccess world, int myX, int myY, int myZ, int side) {
		int nx = myX + DIRS[side][0], ny = myY + DIRS[side][1], nz = myZ + DIRS[side][2];
		if (!canConnectTo(world, nx, ny, nz)) return false;
		// Check own side config
		TileEntity myTe = world.getBlockTileEntity(myX, myY, myZ);
		if (myTe instanceof Aero_ISideConfigurable) {
			int mode = Aero_SideConfig.get(((Aero_ISideConfigurable) myTe).getSideConfig(), side, Aero_SideConfig.TYPE_ENERGY);
			if (mode == Aero_SideConfig.MODE_NONE) return false;
		}
		// Check neighbor's opposite side config
		TileEntity nTe = world.getBlockTileEntity(nx, ny, nz);
		if (nTe instanceof Aero_ISideConfigurable) {
			int opp = Aero_SideConfig.oppositeSide(side);
			int mode = Aero_SideConfig.get(((Aero_ISideConfigurable) nTe).getSideConfig(), opp, Aero_SideConfig.TYPE_ENERGY);
			if (mode == Aero_SideConfig.MODE_NONE) return false;
		}
		return true;
	}

	private static final float PLATE_MIN = 4.0F / 16.0F;
	private static final float PLATE_MAX = 12.0F / 16.0F;

	private float[] calcBounds(IBlockAccess world, int i, int j, int k) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float minX = min, minY = min, minZ = min;
		float maxX = max, maxY = max, maxZ = max;

		for (int side = 0; side < 6; side++) {
			if (!canConnectToSide(world, i, j, k, side)) continue;
			int nx = i + DIRS[side][0], ny = j + DIRS[side][1], nz = k + DIRS[side][2];
			boolean machine = isNeighborMachine(world, nx, ny, nz);
			switch (side) {
				case 0: minY = 0.0F; if (machine) { minX = Math.min(minX, PLATE_MIN); maxX = Math.max(maxX, PLATE_MAX); minZ = Math.min(minZ, PLATE_MIN); maxZ = Math.max(maxZ, PLATE_MAX); } break;
				case 1: maxY = 1.0F; if (machine) { minX = Math.min(minX, PLATE_MIN); maxX = Math.max(maxX, PLATE_MAX); minZ = Math.min(minZ, PLATE_MIN); maxZ = Math.max(maxZ, PLATE_MAX); } break;
				case 2: minZ = 0.0F; if (machine) { minX = Math.min(minX, PLATE_MIN); maxX = Math.max(maxX, PLATE_MAX); minY = Math.min(minY, PLATE_MIN); maxY = Math.max(maxY, PLATE_MAX); } break;
				case 3: maxZ = 1.0F; if (machine) { minX = Math.min(minX, PLATE_MIN); maxX = Math.max(maxX, PLATE_MAX); minY = Math.min(minY, PLATE_MIN); maxY = Math.max(maxY, PLATE_MAX); } break;
				case 4: minX = 0.0F; if (machine) { minY = Math.min(minY, PLATE_MIN); maxY = Math.max(maxY, PLATE_MAX); minZ = Math.min(minZ, PLATE_MIN); maxZ = Math.max(maxZ, PLATE_MAX); } break;
				case 5: maxX = 1.0F; if (machine) { minY = Math.min(minY, PLATE_MIN); maxY = Math.max(maxY, PLATE_MAX); minZ = Math.min(minZ, PLATE_MIN); maxZ = Math.max(maxZ, PLATE_MAX); } break;
			}
		}
		return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int i, int j, int k) {
		float[] b = calcBounds(world, i, j, k);
		return AxisAlignedBB.getBoundingBoxFromPool(
			(double)i + b[0], (double)j + b[1], (double)k + b[2],
			(double)i + b[3], (double)j + b[4], (double)k + b[5]
		);
	}

	public void setBlockBoundsBasedOnState(IBlockAccess world, int i, int j, int k) {
		float[] b = calcBounds(world, i, j, k);
		this.setBlockBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
	}
}
