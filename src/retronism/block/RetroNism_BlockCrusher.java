package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

import java.util.Random;

public class Retronism_BlockCrusher extends BlockContainer {
	private Random crusherRand = new Random();

	public Retronism_BlockCrusher(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	public int getBlockTextureFromSide(int side) {
		return mod_Retronism.texCrusher;
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public int getRenderType() {
		return mod_Retronism.crusherRenderID;
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if (player.isSneaking()) return false;
		if (world.multiplayerWorld) return true;
		Retronism_TileCrusher tileEntity = (Retronism_TileCrusher) world.getBlockTileEntity(x, y, z);
		ModLoader.OpenGUI(player, new Retronism_GuiCrusher(player.inventory, tileEntity));
		return true;
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileCrusher();
	}

	public void onBlockRemoval(World world, int x, int y, int z) {
		Retronism_TileCrusher crusher = (Retronism_TileCrusher) world.getBlockTileEntity(x, y, z);

		for (int i = 0; i < crusher.getSizeInventory(); ++i) {
			ItemStack stack = crusher.getStackInSlot(i);
			if (stack != null) {
				float rx = this.crusherRand.nextFloat() * 0.8F + 0.1F;
				float ry = this.crusherRand.nextFloat() * 0.8F + 0.1F;
				float rz = this.crusherRand.nextFloat() * 0.8F + 0.1F;

				while (stack.stackSize > 0) {
					int dropCount = this.crusherRand.nextInt(21) + 10;
					if (dropCount > stack.stackSize) {
						dropCount = stack.stackSize;
					}
					stack.stackSize -= dropCount;
					EntityItem entity = new EntityItem(world,
						(double) ((float) x + rx), (double) ((float) y + ry), (double) ((float) z + rz),
						new ItemStack(stack.itemID, dropCount, stack.getItemDamage()));
					float spread = 0.05F;
					entity.motionX = (double) ((float) this.crusherRand.nextGaussian() * spread);
					entity.motionY = (double) ((float) this.crusherRand.nextGaussian() * spread + 0.2F);
					entity.motionZ = (double) ((float) this.crusherRand.nextGaussian() * spread);
					world.entityJoinedWorld(entity);
				}
			}
		}

		super.onBlockRemoval(world, x, y, z);
	}
}
