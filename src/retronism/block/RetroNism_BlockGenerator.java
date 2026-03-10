package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

import java.util.Random;

public class Retronism_BlockGenerator extends BlockContainer {
	private Random generatorRand = new Random();

	public Retronism_BlockGenerator(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
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
		Retronism_TileGenerator tileEntity = (Retronism_TileGenerator) world.getBlockTileEntity(x, y, z);
		ModLoader.OpenGUI(player, new Retronism_GuiGenerator(player.inventory, tileEntity));
		return true;
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileGenerator();
	}

	public void onBlockRemoval(World world, int x, int y, int z) {
		Retronism_TileGenerator generator = (Retronism_TileGenerator) world.getBlockTileEntity(x, y, z);
		for (int i = 0; i < generator.getSizeInventory(); ++i) {
			ItemStack stack = generator.getStackInSlot(i);
			if (stack != null) {
				float rx = this.generatorRand.nextFloat() * 0.8F + 0.1F;
				float ry = this.generatorRand.nextFloat() * 0.8F + 0.1F;
				float rz = this.generatorRand.nextFloat() * 0.8F + 0.1F;
				while (stack.stackSize > 0) {
					int dropCount = this.generatorRand.nextInt(21) + 10;
					if (dropCount > stack.stackSize) dropCount = stack.stackSize;
					stack.stackSize -= dropCount;
					EntityItem entity = new EntityItem(world,
						(double)((float)x + rx), (double)((float)y + ry), (double)((float)z + rz),
						new ItemStack(stack.itemID, dropCount, stack.getItemDamage()));
					float spread = 0.05F;
					entity.motionX = (double)((float)this.generatorRand.nextGaussian() * spread);
					entity.motionY = (double)((float)this.generatorRand.nextGaussian() * spread + 0.2F);
					entity.motionZ = (double)((float)this.generatorRand.nextGaussian() * spread);
					world.entityJoinedWorld(entity);
				}
			}
		}
		super.onBlockRemoval(world, x, y, z);
	}
}
