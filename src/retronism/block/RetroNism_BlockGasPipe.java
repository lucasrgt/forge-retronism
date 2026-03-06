package retronism.block;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.tile.*;

import java.util.Random;

public class Retronism_BlockGasPipe extends BlockContainer {

	public Retronism_BlockGasPipe(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
		this.setBlockBounds(5.0F/16, 5.0F/16, 5.0F/16, 11.0F/16, 11.0F/16, 11.0F/16);
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileGasPipe();
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public int getRenderType() {
		return mod_Retronism.gasPipeRenderID;
	}

	public int quantityDropped(Random random) {
		return 1;
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	public boolean canConnectTo(IBlockAccess world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id == this.blockID || id == mod_Retronism.megaPipeBlock.blockID) return true;
		TileEntity te = world.getBlockTileEntity(x, y, z);
		return te instanceof Retronism_IGasHandler;
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int i, int j, int k) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float minX = min, minY = min, minZ = min;
		float maxX = max, maxY = max, maxZ = max;

		if(canConnectTo(world, i - 1, j, k)) minX = 0.0F;
		if(canConnectTo(world, i + 1, j, k)) maxX = 1.0F;
		if(canConnectTo(world, i, j - 1, k)) minY = 0.0F;
		if(canConnectTo(world, i, j + 1, k)) maxY = 1.0F;
		if(canConnectTo(world, i, j, k - 1)) minZ = 0.0F;
		if(canConnectTo(world, i, j, k + 1)) maxZ = 1.0F;

		return AxisAlignedBB.getBoundingBoxFromPool(
			(double)i + minX, (double)j + minY, (double)k + minZ,
			(double)i + maxX, (double)j + maxY, (double)k + maxZ
		);
	}

	public void setBlockBoundsBasedOnState(IBlockAccess world, int i, int j, int k) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float minX = min, minY = min, minZ = min;
		float maxX = max, maxY = max, maxZ = max;

		if(canConnectTo(world, i - 1, j, k)) minX = 0.0F;
		if(canConnectTo(world, i + 1, j, k)) maxX = 1.0F;
		if(canConnectTo(world, i, j - 1, k)) minY = 0.0F;
		if(canConnectTo(world, i, j + 1, k)) maxY = 1.0F;
		if(canConnectTo(world, i, j, k - 1)) minZ = 0.0F;
		if(canConnectTo(world, i, j, k + 1)) maxZ = 1.0F;

		this.setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
